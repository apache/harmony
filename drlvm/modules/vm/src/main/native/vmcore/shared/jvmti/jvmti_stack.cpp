/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Gregory Shimansky, Pavel Afremov
 */
/*
 * JVMTI stack frame API
 */

#define LOG_DOMAIN "jvmti.stack"

#include "open/vm_method_access.h"

#include "jvmti_direct.h"
#include "jvmti_utils.h"
#include "interpreter_exports.h"
#include "jthread.h"
#include "vm_threads.h"
#include "environment.h"
#include "Class.h"
#include "cxxlog.h"
#include "thread_generic.h"
#include "suspend_checker.h"
#include "stack_trace.h"
#include "stack_iterator.h"
#include "jit_intf_cpp.h"
#include "thread_manager.h"
#include "cci.h"
#include "open/vm_class_info.h"

class JavaStackIterator
{
    StackIterator* si;

    /** index of current java stack frame */
    jint depth;

    /** number of inlines in current si frame */
    U_32 inlined_num;

    /** index of inlined frame in current si frame */
    int inlined_depth;

    bool past_end;

    void update() {
        Method_Handle method = NULL;

        // skip non java frames
        while (! si_is_past_end(si) && NULL == (method = si_get_method(si)))
            si_goto_previous(si);

        // end of stack
        if (NULL == method) {
            past_end = true;
            return;
        }

        // hide vm specific stack tail
        if (0 == strcmp("runImpl", method->get_name()->bytes)) {
            const char* class_name = method->get_class()->get_name()->bytes;

            if (0 == strcmp("java/lang/VMStart$MainThread", class_name)
                || 0 == strcmp("java/lang/Thread", class_name))
            {
                past_end = true;
                return;
            }
        }

        inlined_num = si_get_inline_depth(si);
        inlined_depth = inlined_num;
    }

public:
    ~JavaStackIterator() {
        si_free(si);
    }

    JavaStackIterator(VM_thread *thread) {
        si = si_create_from_native(thread);
        depth = 0;
        past_end = false;
        update();
    }

    StackIterator* get_si() {
        return si;
    }

    jint get_depth() {
        return depth;
    }

    bool is_inlined() {
        return 0 != inlined_depth;
    }

    bool is_jni() {
        return si_is_native(si);
    }

    operator bool () {
        return ! past_end;
    }

    void operator ++ (int) {
        assert(! past_end);

        depth++;

        if (inlined_depth > 0) {
            inlined_depth--;
        } else {
            si_goto_previous(si);
            update();
        }
    }

    void operator += (int frames_num) {
        for (; frames_num > 0 && *this; frames_num--, (*this)++);
    }

    void get_location(jmethodID* p_method, jlocation* p_location) {
        Method* method = si_get_method(si);

        // jni method frame
        if (si_is_native(si)) {
            *p_method = (jmethodID) method;
            *p_location = -1;
            return;
        }

        // java frame
        NativeCodePtr ip = si_get_ip(si);

        // if ip points to next instruction
        if (si_get_jit_context(si)->is_ip_past) {
            ip = (NativeCodePtr) ((char*) ip - 1);
        }

        CodeChunkInfo *cci = si_get_code_chunk_info(si);
        JIT *jit = cci->get_jit();

        uint16 bc;
        // inlined method frame
        if (0 != inlined_depth) {
            U_32 offset = (U_32) ((char*) ip -
                (char*) cci->get_code_block_addr());
            method = jit->get_inlined_method(
                cci->get_inline_info(), offset, inlined_depth);
            bc = jit->get_inlined_bc(
                cci->get_inline_info(), offset, inlined_depth);
        } else {
            OpenExeJpdaError UNREF result = jit->get_bc_location_for_native(
                    method, ip, &bc);
            assert(result == EXE_ERROR_NONE);
        }

        *p_method = (jmethodID) method;
        *p_location = (jlocation) bc;
    }
};

jthread getCurrentThread() {
    jthread current_thread = jthread_self();
    assert(current_thread);

    return oh_copy_to_local_handle(current_thread);
}


jint get_thread_stack_depth(VM_thread *thread)
{
    JavaStackIterator jsi(thread);

    while (jsi)
        jsi++;

    return jsi.get_depth();
} // get_thread_stack_depth

static jvmtiError get_stack_trace_jit(
        VM_thread* vm_thread,
        jint start_depth,
        jint max_frame_count,
        jvmtiFrameInfo* frame_buffer,
        jint* count_ptr)
{
    jint start = start_depth;

    if (start < 0) {
        start = start + get_thread_stack_depth(vm_thread);

        if (start < 0)
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }

    JavaStackIterator jsi(vm_thread);

    // to be complient with RI we must break the spec.
    // if stack is empty and start_depth is 0 RI doesn't return
    // JVMTI_ERROR_ILLEGAL_ARGUMENT as it is required by spec.
    if (start == 0 && ! jsi) {
        *count_ptr = 0;
        return JVMTI_ERROR_NONE;
    }

    jsi += start;

    if (! jsi)
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;

    jint i = 0;
    for (; i < max_frame_count && jsi; i++, jsi++) {
        jsi.get_location(& frame_buffer[i].method, & frame_buffer[i].location);
    }

    *count_ptr = i;

    return JVMTI_ERROR_NONE;
} // get_stack_trace_jit

/*
 * Get Stack Trace
 *
 * Get information about the stack of a thread. If max_frame_count
 * is less than the depth of the stack, the max_frame_count
 * deepest frames are returned, otherwise the entire stack is
 * returned. Deepest frames are at the beginning of the returned
 * buffer.
 *
 * REQUIRED Functionality
 */

jvmtiError JNICALL
jvmtiGetStackTrace(jvmtiEnv* env,
                   jthread thread,
                   jint start_depth,
                   jint max_frame_count,
                   jvmtiFrameInfo* frame_buffer,
                   jint* count_ptr)
{
    TRACE("GetStackTrace called");
    SuspendEnabledChecker sec;
    jvmtiError err;

    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    // check error condition: JVMTI_ERROR_INVALID_THREAD
    jint state;
    err = jvmtiGetThreadState(env, thread, &state);

    if (err != JVMTI_ERROR_NONE) {
        return err;
    }

    // check error condition: JVMTI_ERROR_THREAD_NOT_ALIVE
    if ((state & JVMTI_THREAD_STATE_ALIVE) == 0) {
        return JVMTI_ERROR_THREAD_NOT_ALIVE;
    }

    // check error condition: JVMTI_ERROR_NULL_POINTER
    if (frame_buffer == 0) {
        return JVMTI_ERROR_NULL_POINTER;
    }

    // check error condition: JVMTI_ERROR_NULL_POINTER
    if (count_ptr == 0) {
        return JVMTI_ERROR_NULL_POINTER;
    }

    // check error condition: JVMTI_ERROR_ILLEGAL_ARGUMENT
    if (max_frame_count < 0) {
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }

    bool thread_suspended = false;
    // Suspend thread before getting stacks
    vm_thread_t vm_thread;
    if (NULL != thread)
    {
        // Check that this thread is not current
        vm_thread = jthread_get_vm_thread_ptr_safe(thread);
        if (vm_thread != p_TLS_vmthread)
        {
            // to avoid suspension of each other due to race condition
            // get global thread lock as it's done in hythread_suspend_all().
            IDATA UNREF status = hythread_global_lock();
            assert(TM_ERROR_NONE == status);

            status = hythread_suspend_other((hythread_t)vm_thread);
            assert(TM_ERROR_NONE == status);

            thread_suspended = true;

            status = hythread_global_unlock();
            assert(TM_ERROR_NONE == status);
        }
    }
    else
        vm_thread = p_TLS_vmthread;

    if (interpreter_enabled())
        err = interpreter.interpreter_ti_getStackTrace(env,
            vm_thread, start_depth, max_frame_count, frame_buffer, count_ptr);
    else
        err = get_stack_trace_jit(vm_thread, start_depth, max_frame_count, frame_buffer, count_ptr);

    if (thread_suspended)
        hythread_resume((hythread_t)vm_thread);

    return err;
} // jvmtiGetStackTrace

/*
 * Get All Stack Traces
 *
 * Get information about the stacks of all live threads.
 * If max_frame_count is less than the depth of a stack,
 * the max_frame_count deepest frames are returned for that
 * thread, otherwise the entire stack is returned. Deepest
 * frames are at the beginning of the returned buffer.
 *
 * REQUIRED Functionality
 */
jvmtiError JNICALL
jvmtiGetAllStackTraces(jvmtiEnv* env,
                       jint max_frame_count,
                       jvmtiStackInfo** stack_info_ptr,
                       jint* thread_count_ptr)
{
    TRACE("GetAllStackTraces called");
    SuspendEnabledChecker sec;
    jint count;
    jthread *threads;
    jvmtiError res;

    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (NULL == stack_info_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    if (NULL == thread_count_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    if (max_frame_count < 0)
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;

    // FIXME: new threads can be created before all threads are suspended
    // event handler for thread_creating should block creation new threads
    // at this moment
    assert(hythread_is_suspend_enabled());
    hythread_suspend_all(NULL, NULL);
    res = jvmtiGetAllThreads(env, &count, &threads);

    if (res != JVMTI_ERROR_NONE) {
       hythread_resume_all(NULL);
       return res;
    }

    jvmtiStackInfo *info;
    // FIXME: memory leak in case of error
    res = _allocate(sizeof(jvmtiStackInfo) * count +
        sizeof(jvmtiFrameInfo) * max_frame_count * count,
        (unsigned char **)&info);

    if (JVMTI_ERROR_NONE != res) {
       hythread_resume_all(NULL);
       return res;
    }

    // getting thread states
    for (int i = 0; i < count; i++) {
        info[i].thread = threads[i];
        res = jvmtiGetThreadState(env, threads[i], &info[i].state);

        if (JVMTI_ERROR_NONE != res) {
            _deallocate((unsigned char *) info);
            hythread_resume_all(NULL);
            return res;
        }

        // Frame_buffer pointer should pointer to the memory right
        // after the jvmtiStackInfo structures
        info[i].frame_buffer = ((jvmtiFrameInfo *)&info[count]) +
            max_frame_count * i;
        res = jvmtiGetStackTrace(env, info[i].thread, 0, max_frame_count,
            info[i].frame_buffer, &info[i].frame_count);

        if (JVMTI_ERROR_NONE != res) {
            _deallocate((unsigned char *) info);
            hythread_resume_all(NULL);
            return res;
        }
    }

    hythread_resume_all(NULL);

    *thread_count_ptr = count;
    *stack_info_ptr = info;
    return JVMTI_ERROR_NONE;
} // jvmtiGetAllStackTraces

/*
 * Get Thread List Stack Traces
 *
 * Get information about the stacks of the supplied threads.
 * If max_frame_count is less than the depth of a stack, the
 * max_frame_count deepest frames are returned for that thread,
 * otherwise the entire stack is returned. Deepest frames are
 * at the beginning of the returned buffer.
 *
 * REQUIRED Functionality
 */
jvmtiError JNICALL
jvmtiGetThreadListStackTraces(jvmtiEnv* env,
                              jint thread_count,
                              const jthread* thread_list,
                              jint max_frame_count,
                              jvmtiStackInfo** stack_info_ptr)
{
    TRACE("GetThreadListStackTraces called");
    SuspendEnabledChecker sec;
    jint count = thread_count;
    const jthread *threads = thread_list;
    jvmtiError res;

    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (NULL == stack_info_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    if (NULL == thread_list)
        return JVMTI_ERROR_NULL_POINTER;

    if (thread_count < 0)
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;

    if (max_frame_count < 0)
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;

    jvmtiStackInfo *info;
    res = _allocate(sizeof(jvmtiStackInfo) * count +
        sizeof(jvmtiFrameInfo) * max_frame_count * count,
        (unsigned char **)&info);

    if (JVMTI_ERROR_NONE != res)
       return res;

    int i;
    vm_thread_t self = jthread_self_vm_thread();
    // stopping all threads
    for(i = 0; i < count; i++) {
        // FIXME: thread can be dead at this time
        // event handler for thread death should block thread death
        // until the function end.
        info[i].thread = threads[i];
        vm_thread_t vm_thread = jthread_get_vm_thread_ptr_safe(threads[i]);
        if (self != vm_thread) {
            IDATA UNREF status = hythread_suspend_other((hythread_t)vm_thread);
            assert(TM_ERROR_NONE == status);
        }
    }

    // getting thread states
    for(i = 0; i < count; i++) {
        // Frame_buffer pointer should pointer to the memory right
        // after the jvmtiStackInfo structures
        info[i].frame_buffer = ((jvmtiFrameInfo *)&info[count]) +
            max_frame_count * i;
        res = jvmtiGetStackTrace(env, info[i].thread, 0, max_frame_count,
                info[i].frame_buffer, &info[i].frame_count);

        // if thread was terminated before we got it's stack - return null frame_count
        if (JVMTI_ERROR_THREAD_NOT_ALIVE == res)
            info[i].frame_count = 0;
        else if (JVMTI_ERROR_NONE != res)
            break;
    }

    // unsuspend suspended threads.
    for(i = 0; i < count; i++) {
        vm_thread_t vm_thread = jthread_get_vm_thread_ptr_safe(threads[i]);
        if (self != vm_thread) {
            hythread_resume((hythread_t)vm_thread);
        }
    }

    if (JVMTI_ERROR_NONE != res)
        return res;

    *stack_info_ptr = info;
    return JVMTI_ERROR_NONE;
} // jvmtiGetThreadListStackTraces

/*
 * Get Frame Count
 *
 * Get the number of frames currently in the specified thread's
 * call stack.
 *
 * REQUIRED Functionality
 */
jvmtiError JNICALL
jvmtiGetFrameCount(jvmtiEnv* env,
                   jthread thread,
                   jint* count_ptr)
{
    TRACE("GetFrameCount called");
    SuspendEnabledChecker sec;
    jint state;
    jvmtiError err;

    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (thread == 0) {
        thread = getCurrentThread();
    }

    // check error condition: JVMTI_ERROR_INVALID_THREAD
    err = env->GetThreadState(thread, &state);

    if (err != JVMTI_ERROR_NONE) {
        return err;
    }

    // check error condition: JVMTI_ERROR_THREAD_NOT_ALIVE
    if ((state & JVMTI_THREAD_STATE_ALIVE) == 0) {
        return JVMTI_ERROR_THREAD_NOT_ALIVE;
    }

    // check error condition: JVMTI_ERROR_NULL_POINTER
    if (count_ptr == 0) {
        return JVMTI_ERROR_NULL_POINTER;
    }

    bool thread_suspended = false;
    // Suspend thread before getting stacks
    vm_thread_t vm_thread;
    if (NULL != thread)
    {
        // Check that this thread is not current
        vm_thread = jthread_get_vm_thread_ptr_safe(thread);
        if (vm_thread != p_TLS_vmthread)
        {
            IDATA UNREF status = hythread_suspend_other((hythread_t)vm_thread);
            assert(TM_ERROR_NONE == status);
            thread_suspended = true;
        }
    }
    else
        vm_thread = p_TLS_vmthread;

    jvmtiError errStack;
    if (interpreter_enabled())
        errStack = interpreter.interpreter_ti_get_frame_count(env,
            vm_thread, count_ptr);
    else
    {
        jint depth = get_thread_stack_depth(vm_thread);
        *count_ptr = depth;
        errStack = JVMTI_ERROR_NONE;
    }

    if (thread_suspended)
        hythread_resume((hythread_t)vm_thread);

    return errStack;
} // jvmtiGetFrameCount

/*
 * Pop Frame
 *
 * Pop the topmost stack frame of thread's stack. Popping a frame
 * takes you to the preceding frame.
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiPopFrame(jvmtiEnv* env,
              jthread thread)
{
    TRACE("PopFrame called");
    SuspendEnabledChecker sec;
    jint state;
    jvmtiError err;

    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();
    CHECK_CAPABILITY(can_pop_frame);

    if (NULL == thread) {
        return JVMTI_ERROR_INVALID_THREAD;
    }

    JNIEnv *jni_env = p_TLS_vmthread->jni_env;

    jthread curr_thread = getCurrentThread();
    if (jni_env->IsSameObject(thread, curr_thread) ) {
        // cannot pop frame yourself
        return JVMTI_ERROR_THREAD_NOT_SUSPENDED;
    }

    // get thread state
    err = env->GetThreadState(thread, &state);
    if (err != JVMTI_ERROR_NONE) {
        return err;
    }

    // check thread state
    if ((state & JVMTI_THREAD_STATE_ALIVE) == 0) {
        return JVMTI_ERROR_THREAD_NOT_ALIVE;
    }
    if( (state & JVMTI_THREAD_STATE_SUSPENDED) == 0) {
        return JVMTI_ERROR_THREAD_NOT_SUSPENDED;
    }

    // check stack depth
    hythread_t hy_thread = jthread_get_native_thread(thread);
    vm_thread_t vm_thread = jthread_get_vm_thread(hy_thread);

    jint depth;
    if (interpreter_enabled()) {
        err = interpreter.interpreter_ti_get_frame_count(env,
            vm_thread, &depth);
    } else {
        depth = get_thread_stack_depth(vm_thread);
        err = JVMTI_ERROR_NONE;
    }
    if (err != JVMTI_ERROR_NONE) {
        return err;
    }

    if(depth <= 1) {
        return JVMTI_ERROR_NO_MORE_FRAMES;
    }

    if (interpreter_enabled()) {
        return interpreter.interpreter_ti_pop_frame(env, vm_thread);
    } else {
        return jvmti_jit_pop_frame(thread);
    }
} // jvmtiPopFrame

/*
 * Get Frame Location
 *
 * For a Java programming language frame, return the location of
 * the instruction currently executing.
 *
 * REQUIRED Functionality
 */
jvmtiError JNICALL
jvmtiGetFrameLocation(jvmtiEnv* env,
                      jthread thread,
                      jint depth,
                      jmethodID* method_ptr,
                      jlocation* location_ptr)
{
    TRACE("GetFrameLocation called");
    SuspendEnabledChecker sec;
    jint state;
    jvmtiError err;

    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (thread == 0) {
        thread = getCurrentThread();
    }

    // check error condition: JVMTI_ERROR_INVALID_THREAD
    err = env->GetThreadState(thread, &state);

    if (err != JVMTI_ERROR_NONE) {
        return err;
    }

    // check error condition: JVMTI_ERROR_THREAD_NOT_ALIVE
    if ((state & JVMTI_THREAD_STATE_ALIVE) == 0) {
        return JVMTI_ERROR_THREAD_NOT_ALIVE;
    }

    // check error condition: JVMTI_ERROR_NULL_POINTER
    if (location_ptr == NULL) {
        return JVMTI_ERROR_NULL_POINTER;
    }

    // check error condition: JVMTI_ERROR_NULL_POINTER
    if (method_ptr == NULL) {
        return JVMTI_ERROR_NULL_POINTER;
    }

    // check error condition: JVMTI_ERROR_ILLEGAL_ARGUMENT
    if (depth < 0) {
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }

    bool thread_suspended = false;
    // Suspend thread before getting stacks
    vm_thread_t vm_thread;
    if (NULL != thread)
    {
        // Check that this thread is not current
        vm_thread = jthread_get_vm_thread_ptr_safe(thread);
        if (vm_thread != p_TLS_vmthread)
        {
            IDATA UNREF status = hythread_suspend_other((hythread_t)vm_thread);
            assert(TM_ERROR_NONE == status);
            thread_suspended = true;
        }
    }
    else
        vm_thread = p_TLS_vmthread;

    jvmtiError errStack;
    if (interpreter_enabled())
        // check error condition: JVMTI_ERROR_ILLEGAL_ARGUMENT
        // check error condition: JVMTI_ERROR_NO_MORE_FRAMES
        errStack = interpreter.interpreter_ti_getFrameLocation(env,
            jthread_get_vm_thread_ptr_safe(thread),
            depth, method_ptr, location_ptr);
    else
    {
        JavaStackIterator jsi(vm_thread);
        jsi += depth;

        if (jsi) {
            jsi.get_location(method_ptr, location_ptr);
            errStack = JVMTI_ERROR_NONE;
        } else {
            errStack = JVMTI_ERROR_NO_MORE_FRAMES;
        }
    }

    if (thread_suspended)
        hythread_resume((hythread_t)vm_thread);

    return errStack;
} // jvmtiGetFrameLocation

/*
 * Notify Frame Pop
 *
 * When the frame that is currently at depth is popped from the
 * stack, generate a FramePop event. See the FramePop event for
 * details. Only frames corresponding to non-native Java
 * programming language methods can receive notification.
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiNotifyFramePop(jvmtiEnv* env,
                    jthread thread,
                    jint depth)
{
    TRACE("NotifyFramePop called: thread: "
        << thread << ", depth: " << depth);
    SuspendEnabledChecker sec;
    jint state;
    jthread curr_thread = getCurrentThread();
    jvmtiError err;

    // Check given env & current phase.
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};
    CHECK_EVERYTHING();
    CHECK_CAPABILITY(can_generate_frame_pop_events);

    if (NULL == thread)
        thread = curr_thread;

    // check error condition: JVMTI_ERROR_INVALID_THREAD
    err = env->GetThreadState(thread, &state);

    if (err != JVMTI_ERROR_NONE) {
        return err;
    }

    // check error condition: JVMTI_ERROR_THREAD_NOT_ALIVE
    if ((state & JVMTI_THREAD_STATE_ALIVE) == 0) {
        return JVMTI_ERROR_THREAD_NOT_ALIVE;
    }

    // check error condition: JVMTI_ERROR_THREAD_NOT_SUSPENDED
    JNIEnv *jni_env = p_TLS_vmthread->jni_env;
    if (!jni_env->IsSameObject(thread,curr_thread)
            && ((state & JVMTI_THREAD_STATE_SUSPENDED) == 0)) {
        return JVMTI_ERROR_THREAD_NOT_SUSPENDED;
    }

    // check error condition: JVMTI_ERROR_ILLEGAL_ARGUMENT
    if (depth < 0) {
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }

    vm_thread_t vm_thread;
    if (NULL != thread)
        vm_thread = jthread_get_vm_thread_ptr_safe(thread);
    else
        vm_thread = p_TLS_vmthread;

    if (interpreter_enabled())
        // check error condition: JVMTI_ERROR_OPAQUE_FRAME
        // check error condition: JVMTI_ERROR_NO_MORE_FRAMES
        return interpreter.interpreter_ti_notify_frame_pop(env,
            vm_thread, depth);
    else
    {
        JavaStackIterator jsi(vm_thread);

        jsi += depth;

        if (! jsi)
            return JVMTI_ERROR_NO_MORE_FRAMES;

        if (jsi.is_jni() || jsi.is_inlined())
            return JVMTI_ERROR_OPAQUE_FRAME;


        // get method for TRACE message
        Method* UNREF method = si_get_method(jsi.get_si());

        // fast forward to teh end of stack
        for (; jsi; jsi++);

        jvmti_frame_pop_listener *new_listener =
            (jvmti_frame_pop_listener *)STD_MALLOC(
                sizeof(jvmti_frame_pop_listener));
        assert(new_listener);

        new_listener->depth = jsi.get_depth() - depth;
        new_listener->env = reinterpret_cast<TIEnv *>(env);
        new_listener->next = vm_thread->jvmti_thread.frame_pop_listener;
        vm_thread->jvmti_thread.frame_pop_listener = new_listener;

        TRACE("Pop listener is created: thread: "
            << vm_thread << ", listener: " << new_listener
            << ", env: " << new_listener->env << ", depth: " << new_listener->depth
            << " -> " << method);

        return JVMTI_ERROR_NONE;
    }
} // jvmtiNotifyFramePop
