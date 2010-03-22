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
 * @author Gregory Shimansky
 */  
/*
 * JVMTI local variables API
 */
#define LOG_DOMAIN "jvmti.locals"
#include "cxxlog.h"

#include "jvmti_direct.h"
#include "jvmti_utils.h"
#include "vm_threads.h"
#include "interpreter_exports.h"
#include "object_handles.h"
#include "environment.h"
#include "thread_generic.h"
#include "jthread.h"
#include "suspend_checker.h"
#include "stack_iterator.h"
#include "stack_trace.h"
#include "jit_intf_cpp.h"
#include "cci.h"
#include "Class.h"
#include "vtable.h"

/*
 * Local Variable functions:
 *
 *     Get Local Variable - Object
 *     Get Local Variable - Int
 *     Get Local Variable - Long
 *     Get Local Variable - Float
 *     Get Local Variable - Double
 *     Set Local Variable - Object
 *     Set Local Variable - Int
 *     Set Local Variable - Long
 *     Set Local Variable - Float
 *     Set Local Variable - Double
 *
 * These functions are used to retrieve or set the value of a
 * local variable. The variable is identified by the depth of the
 * frame containing its value and the variable's slot number within
 * that frame. The mapping of variables to slot numbers can be
 * obtained with the function GetLocalVariableTable.
 *
 * OPTIONAL Functionality
 */

static jvmtiError
GetLocal_checkArgs(jvmtiEnv* env,
                        jthread *thread,
                        jint depth,
                        jint UNREF slot,
                        void* value_ptr)
{
    jint state;
    jvmtiError err;

    // TODO: check error condition: JVMTI_ERROR_MUST_POSSESS_CAPABILITY

    if (*thread == 0) {
        *thread = getCurrentThread();
    }

    // check error condition: JVMTI_ERROR_INVALID_THREAD
    err = jvmtiGetThreadState(env, *thread, &state);

    if (err != JVMTI_ERROR_NONE) {
        return err;
    }

    // check error condition: JVMTI_ERROR_THREAD_NOT_ALIVE
    if ((state & JVMTI_THREAD_STATE_ALIVE) == 0) {
        return JVMTI_ERROR_THREAD_NOT_ALIVE;
    }

    // check error condition: JVMTI_ERROR_ILLEGAL_ARGUMENT
    if (depth < 0) {
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }

    // check error condition: JVMTI_ERROR_NULL_POINTER
    if (value_ptr == 0) {
        return JVMTI_ERROR_NULL_POINTER;
    }

    return JVMTI_ERROR_NONE;
}

#define GET_JIT_FRAME_CONTEXT                               \
    StackIterator *si = si_create_from_native(vm_thread);   \
                                                            \
    if (!si_get_method(si)) /* Skip native VM frame */      \
        si_goto_previous(si);                               \
                                                            \
    while (depth > 0 && !si_is_past_end(si))                \
    {                                                       \
        if (si_get_method(si))                              \
            depth -= 1 + si_get_inline_depth(si);           \
        si_goto_previous(si);                               \
    }                                                       \
                                                            \
    if (si_is_past_end(si))                                 \
    {                                                       \
        if (thread_suspended)                               \
            hythread_resume((hythread_t)vm_thread);         \
        si_free(si);                                        \
        return JVMTI_ERROR_NO_MORE_FRAMES;                  \
    }                                                       \
                                                            \
    if (si_is_native(si))                                   \
    {                                                       \
        if (thread_suspended)                               \
            hythread_resume((hythread_t)vm_thread);         \
        si_free(si);                                        \
        return JVMTI_ERROR_OPAQUE_FRAME;                    \
    }                                                       \
                                                            \
    JitFrameContext *jfc = si_get_jit_context(si);          \
    CodeChunkInfo *cci = si_get_code_chunk_info(si);        \
    JIT *jit = cci->get_jit();                              \
    Method *method = cci->get_method();

static bool is_valid_object(jvmtiEnv* env, jobject handle)
{
    SuspendEnabledChecker sec;

    if (NULL == handle)
        return true;

    tmn_suspend_disable();

    ManagedObject *obj = ((ObjectHandle) handle)->object;

    if (obj < (ManagedObject *)VM_Global_State::loader_env->heap_base ||
        obj > (ManagedObject *)VM_Global_State::loader_env->heap_end)
    {
        tmn_suspend_enable();
        return false;
    }

    Class *clss = obj->vt()->clss;
    ManagedObject *clsObj = struct_Class_to_java_lang_Class(clss);
    // ppervov: FIXME: there is an assertion in the above function which
    // is exactly the same as in the following if. So, this code will only
    // work in release and just assert in debug.
    if (clsObj->vt()->clss != VM_Global_State::loader_env->JavaLangClass_Class) {
        tmn_suspend_enable();
        return false;
    }

    tmn_suspend_enable();
    return true;
}

/**
 * General function to set value of local variable.
 * @param var_type  type of the local variable
 * @param p_value   pointer to the new variable value
 */
static jvmtiError set_local(jvmtiEnv* env,
                            jthread thread,
                            jint depth,
                            jint slot,
                            VM_Data_Type var_type,
                            void* p_value)
{
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    CHECK_CAPABILITY(can_access_local_variables);

    // check error condition: JVMTI_ERROR_INVALID_THREAD
    // check error condition: JVMTI_ERROR_THREAD_NOT_ALIVE
    // check error condition: JVMTI_ERROR_ILLEGAL_ARGUMENT
    jvmtiError err = GetLocal_checkArgs(env, &thread, depth, slot, p_value);
    if (err != JVMTI_ERROR_NONE)
        return err;

    // check error condition: JVMTI_ERROR_INVALID_OBJECT
    if (VM_DATA_TYPE_CLASS == var_type && ! is_valid_object(env, *(jobject*) p_value))
        return JVMTI_ERROR_INVALID_OBJECT;

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

    if (interpreter_enabled())
    {
        // TODO: check error condition: JVMTI_ERROR_INVALID_SLOT
        // TODO: check error condition: JVMTI_ERROR_TYPE_MISMATCH
        // TODO: check error condition: JVMTI_ERROR_OPAQUE_FRAME
        // TODO: check error condition: JVMTI_ERROR_NO_MORE_FRAMES

        switch (var_type) {
            case VM_DATA_TYPE_CLASS:
                err = interpreter.interpreter_ti_setObject(env, vm_thread,
                        depth, slot, *(jobject*) p_value);
                break;
            case VM_DATA_TYPE_INT32:
            case VM_DATA_TYPE_F4:
                err = interpreter.interpreter_ti_setLocal32(env, vm_thread,
                        depth, slot, *(int*) p_value);
                break;
            case VM_DATA_TYPE_INT64:
            case VM_DATA_TYPE_F8:
                err = interpreter.interpreter_ti_setLocal64(env, vm_thread,
                        depth, slot, *(int64*) p_value);
                break;
            default:
                DIE(("Error: unrecognized local variable type"));
        }
    }
    else
    {
        GET_JIT_FRAME_CONTEXT;

        tmn_suspend_disable();
        OpenExeJpdaError result;

        switch (var_type) {
            case VM_DATA_TYPE_CLASS:
                if (NULL != *(jobject*) p_value)
                    result = jit->set_local_var(method, jfc, slot,
                        VM_DATA_TYPE_CLASS, &(*(ObjectHandle*) p_value)->object);
                else
                {
                    ManagedObject *n = (ManagedObject *)VM_Global_State::loader_env->managed_null;
                    result = jit->set_local_var(method, jfc, slot,
                        VM_DATA_TYPE_CLASS, &n);
                }

                break;
            case VM_DATA_TYPE_INT32:
            case VM_DATA_TYPE_F4:
            case VM_DATA_TYPE_INT64:
            case VM_DATA_TYPE_F8:
                result = jit->set_local_var(method, jfc, slot, var_type,
                        p_value);
                break;
            default:
                DIE(("Error: unrecognized local variable type"));
        }

        si_free(si);
        tmn_suspend_enable();

        err = jvmti_translate_jit_error(result);
    }

    if (thread_suspended)
        hythread_resume((hythread_t)vm_thread);

    return err;
}

jvmtiError JNICALL
jvmtiGetLocalObject(jvmtiEnv* env,
                    jthread thread,
                    jint depth,
                    jint slot,
                    jobject* value_ptr)
{
    TRACE("GetLocalObject called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    CHECK_CAPABILITY(can_access_local_variables);

    // check error condition: JVMTI_ERROR_INVALID_THREAD
    // check error condition: JVMTI_ERROR_THREAD_NOT_ALIVE
    // check error condition: JVMTI_ERROR_ILLEGAL_ARGUMENT
    // check error condition: JVMTI_ERROR_NULL_POINTER
    jvmtiError err = GetLocal_checkArgs(env, &thread, depth, slot, value_ptr);
    if (err != JVMTI_ERROR_NONE)
        return err;

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

    if (interpreter_enabled())
        // check error condition: JVMTI_ERROR_INVALID_SLOT
        // check error condition: JVMTI_ERROR_OPAQUE_FRAME
        // check error condition: JVMTI_ERROR_NO_MORE_FRAMES
        // TODO: check error condition: JVMTI_ERROR_TYPE_MISMATCH
        err = interpreter.interpreter_ti_getObject(env,
            vm_thread, depth, slot, value_ptr);
    else
    {
        GET_JIT_FRAME_CONTEXT;
        ManagedObject *obj;

        tmn_suspend_disable();
        OpenExeJpdaError result = jit->get_local_var(method, jfc, slot,
            VM_DATA_TYPE_CLASS, &obj);
        si_free(si);

        if (result == EXE_ERROR_NONE)
        {
            if (obj != (ManagedObject *)VM_Global_State::loader_env->managed_null)
            {
                ObjectHandle oh = oh_allocate_local_handle();
                oh->object = obj;
                *value_ptr = oh;
            }
            else
                *value_ptr = NULL;
        }
        tmn_suspend_enable();

        err = jvmti_translate_jit_error(result);
    }

    if (thread_suspended)
        hythread_resume((hythread_t)vm_thread);

    return err;
}

jvmtiError JNICALL
jvmtiGetLocalInt(jvmtiEnv* env,
                 jthread thread,
                 jint depth,
                 jint slot,
                 jint* value_ptr)
{
    TRACE("GetLocalInt called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    CHECK_CAPABILITY(can_access_local_variables);

    // check error condition: JVMTI_ERROR_INVALID_THREAD
    // check error condition: JVMTI_ERROR_THREAD_NOT_ALIVE
    // check error condition: JVMTI_ERROR_ILLEGAL_ARGUMENT
    // check error condition: JVMTI_ERROR_NULL_POINTER
    jvmtiError err = GetLocal_checkArgs(env, &thread, depth, slot, value_ptr);
    if (err != JVMTI_ERROR_NONE)
        return err;

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

    if (interpreter_enabled())
        // check error condition: JVMTI_ERROR_INVALID_SLOT
        // check error condition: JVMTI_ERROR_OPAQUE_FRAME
        // check error condition: JVMTI_ERROR_NO_MORE_FRAMES
        // TODO: check error condition: JVMTI_ERROR_TYPE_MISMATCH
        err = interpreter.interpreter_ti_getLocal32(env,
            vm_thread, depth, slot, value_ptr);
    else
    {
        GET_JIT_FRAME_CONTEXT;

        tmn_suspend_disable();
        OpenExeJpdaError result = jit->get_local_var(method, jfc, slot,
            VM_DATA_TYPE_INT32, value_ptr);
        si_free(si);
        tmn_suspend_enable();

        err = jvmti_translate_jit_error(result);
    }

    if (thread_suspended)
        hythread_resume((hythread_t)vm_thread);

    return err;
}

jvmtiError JNICALL
jvmtiGetLocalLong(jvmtiEnv* env,
                  jthread thread,
                  jint depth,
                  jint slot,
                  jlong* value_ptr)
{
    TRACE("GetLocalLong called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    CHECK_CAPABILITY(can_access_local_variables);

    // check error condition: JVMTI_ERROR_INVALID_THREAD
    // check error condition: JVMTI_ERROR_THREAD_NOT_ALIVE
    // check error condition: JVMTI_ERROR_ILLEGAL_ARGUMENT
    // check error condition: JVMTI_ERROR_NULL_POINTER
    jvmtiError err = GetLocal_checkArgs(env, &thread, depth, slot, value_ptr);
    if (err != JVMTI_ERROR_NONE)
        return err;

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

    if (interpreter_enabled())
        // check error condition: JVMTI_ERROR_INVALID_SLOT
        // check error condition: JVMTI_ERROR_OPAQUE_FRAME
        // check error condition: JVMTI_ERROR_NO_MORE_FRAMES
        // TODO: check error condition: JVMTI_ERROR_TYPE_MISMATCH
        err = interpreter.interpreter_ti_getLocal64(env,
            vm_thread, depth, slot, value_ptr);
    else
    {
        GET_JIT_FRAME_CONTEXT;

        tmn_suspend_disable();
        OpenExeJpdaError result = jit->get_local_var(method, jfc, slot,
            VM_DATA_TYPE_INT64, value_ptr);
        si_free(si);
        tmn_suspend_enable();

        err = jvmti_translate_jit_error(result);
    }

    if (thread_suspended)
        hythread_resume((hythread_t)vm_thread);

    return err;
}

jvmtiError JNICALL
jvmtiGetLocalFloat(jvmtiEnv* env,
                   jthread thread,
                   jint depth,
                   jint slot,
                   jfloat* value_ptr)
{
    TRACE("GetLocalFloat called");
    SuspendEnabledChecker sec;
    return jvmtiGetLocalInt(env, thread, depth, slot, (jint*)value_ptr);
}

jvmtiError JNICALL
jvmtiGetLocalDouble(jvmtiEnv* env,
                    jthread thread,
                    jint depth,
                    jint slot,
                    jdouble* value_ptr)
{
    TRACE("GetLocalDouble called");
    SuspendEnabledChecker sec;
    return jvmtiGetLocalLong(env, thread, depth, slot, (jlong*)value_ptr);
}



jvmtiError JNICALL
jvmtiSetLocalObject(jvmtiEnv* env,
                    jthread thread,
                    jint depth,
                    jint slot,
                    jobject value)
{
    TRACE("SetLocalObject called");

    return set_local(env, thread, depth, slot, VM_DATA_TYPE_CLASS, (void*) &value);
}

jvmtiError JNICALL
jvmtiSetLocalInt(jvmtiEnv* env,
                 jthread thread,
                 jint depth,
                 jint slot,
                 jint value)
{
    TRACE("SetLocalInt called");

    return set_local(env, thread, depth, slot, VM_DATA_TYPE_INT32, (void*) &value);
}

jvmtiError JNICALL
jvmtiSetLocalLong(jvmtiEnv* env,
                  jthread thread,
                  jint depth,
                  jint slot,
                  jlong value)
{
    TRACE("SetLocalLong called");

    return set_local(env, thread, depth, slot, VM_DATA_TYPE_INT64, (void*) &value);
}

jvmtiError JNICALL
jvmtiSetLocalFloat(jvmtiEnv* env,
                   jthread thread,
                   jint depth,
                   jint slot,
                   jfloat value)
{
    TRACE("SetLocalFloat called");

    return set_local(env, thread, depth, slot, VM_DATA_TYPE_F4, (void*) &value);
}

jvmtiError JNICALL
jvmtiSetLocalDouble(jvmtiEnv* env,
                    jthread thread,
                    jint depth,
                    jint slot,
                    jdouble value)
{
    TRACE("SetLocalDouble called");

    return set_local(env, thread, depth, slot, VM_DATA_TYPE_F8, (void*) &value);
}


