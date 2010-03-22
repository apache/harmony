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
 * @author Intel, Pavel Afremov
 */


#define LOG_DOMAIN "exn"
#include "clog.h"

#include "open/vm_class_info.h"
#include "Class.h"
#include "open/types.h"
#include "vtable.h"
#include "classloader.h"
#include "exceptions.h"
#include "exceptions_impl.h"
#include "exceptions_jit.h"
#include "environment.h"
#include "dump.h"
#include "heap.h"
#include "interpreter.h"
#include "jit_intf_cpp.h"
#include "lil.h"
#include "lil_code_generator.h"
#include "m2n.h"
#include "mon_enter_exit.h"
#include "stack_iterator.h"
#include "vm_stats.h"
#include "jvmti_break_intf.h"
#include "cci.h"
#include "port_threadunsafe.h"


#ifdef _IPF_
#include "../m2n_ipf_internal.h"
#elif defined _EM64T_
#include "../m2n_em64t_internal.h"
#else
#include "../m2n_ia32_internal.h"
#endif


////////////////////////////////////////////////////////////////////////////
// Target_Exception_Handler

Target_Exception_Handler::Target_Exception_Handler(NativeCodePtr start_ip,
    NativeCodePtr end_ip,
    NativeCodePtr handler_ip, Class_Handle exn_class, bool exn_is_dead)
{
    _start_ip = start_ip;
    _end_ip = end_ip;
    _handler_ip = handler_ip;
    _exc = exn_class;
    _exc_obj_is_dead = exn_is_dead;
}

NativeCodePtr Target_Exception_Handler::get_start_ip()
{
    return _start_ip;
}

NativeCodePtr Target_Exception_Handler::get_end_ip()
{
    return _end_ip;
}

NativeCodePtr Target_Exception_Handler::get_handler_ip()
{
    return _handler_ip;
}

Class_Handle Target_Exception_Handler::get_exc()
{
    return _exc;
}

bool Target_Exception_Handler::is_exc_obj_dead()
{
    return _exc_obj_is_dead;
}

#ifdef POINTER64
typedef uint64 NumericNativeCodePtr;
#else
typedef U_32 NumericNativeCodePtr;
#endif

bool Target_Exception_Handler::is_in_range(NativeCodePtr ip, bool is_ip_past)
{
    NumericNativeCodePtr nip = (NumericNativeCodePtr) ip;
    NumericNativeCodePtr sip = (NumericNativeCodePtr) _start_ip;
    NumericNativeCodePtr eip = (NumericNativeCodePtr) _end_ip;

    return (is_ip_past ? sip < nip && nip <= eip : sip <= nip && nip < eip);
}   //Target_Exception_Handler::is_in_range

bool Target_Exception_Handler::is_assignable(Class_Handle exn_class)
{
    if (!_exc)
        return true;
    Class_Handle e = exn_class;
    while (e)
        if (e == _exc)
            return true;
        else
            e = class_get_super_class(e);
    return false;
}   //Target_Exception_Handler::is_assignable

void Target_Exception_Handler::update_catch_range(NativeCodePtr new_start_ip,
    NativeCodePtr new_end_ip)
{
    _start_ip = new_start_ip;
    _end_ip = new_end_ip;
}   //Target_Exception_Handler::update_catch_range

void Target_Exception_Handler::
update_handler_address(NativeCodePtr new_handler_ip)
{
    _handler_ip = new_handler_ip;
}   //Target_Exception_Handler::update_handler_address

//////////////////////////////////////////////////////////////////////////
// Lazy Exception Utilities

// Note: Function runs from unwindable area before exception throwing
// function can be safe point & should be called with disable recursion = 1
static ManagedObject *create_lazy_exception(
    Class_Handle exn_class,
    Method_Handle exn_constr,
    U_8 * exn_constr_args,
    jvalue* vm_exn_constr_args)
{
    assert(!hythread_is_suspend_enabled());

    bool unwindable = set_unwindable(false);
    ManagedObject* result;
    if (NULL == vm_exn_constr_args) {
        result = class_alloc_new_object_and_run_constructor(
            (Class*) exn_class, (Method*) exn_constr, exn_constr_args);
    } else {
        // exception is throwing, so suspend can be enabled safely
        tmn_suspend_enable();
        jthrowable exc_object = create_exception(
            (Class*) exn_class, (Method*) exn_constr, vm_exn_constr_args);

        if (exc_object) {
            result = exc_object->object;
        } else {
            result = NULL;
        }
        tmn_suspend_disable();
    }
    set_unwindable(unwindable);
    exn_rethrow_if_pending();
    return result;
}   //create_object_lazily


//////////////////////////////////////////////////////////////////////////
// Main Exception Propagation Function

// This function propagates an exception to its handler.
// It can only be called for the current thread.
// The input stack iterator provides the starting point for propagation.  If the top frame is an M2nFrame, it is ignored.
// Let A be the current frame, let B be the most recent M2nFrame prior to A.
// The exception is propagated to the first managed frame between A and B that has a handler for the exception,
// or to the native code that managed frame immediately after B if no such managed frame exists.
// If exn_obj is nonnull then it is the exception, otherwise the exception is an instance of
// exn_class created using the given constructor and arguments (a null exn_constr indicates the default constructor).
// The stack iterator is mutated to represent the context that should be resumed.
// The client should either use si_transfer_control to resume it, or use an OS context mechanism
// copied from the final stack iterator.

// function can be safe point & should be called with disable recursion = 1
static ManagedObject * exn_propagate_exception(
    StackIterator * si,
    ManagedObject ** exn_obj,
    Class_Handle exn_class,
    Method_Handle exn_constr,
    U_8 * jit_exn_constr_args,
    jvalue* vm_exn_constr_args)
{
    assert(!hythread_is_suspend_enabled());
    ASSERT_RAISE_AREA;
    ASSERT_NO_INTERPRETER;

    assert(*exn_obj || exn_class);

    // Save the throw context
    StackIterator *throw_si = (StackIterator*) STD_ALLOCA(si_size());
    memcpy(throw_si, si, si_size());

    // Skip first frame if it is an M2nFrame (which is always a transition from managed to the throw code).
    // The M2nFrame will be removed from the thread's M2nFrame list but transfer control or copy to registers.
    if (si_is_native(si)) {
        si_goto_previous(si);
    }

    Method *interrupted_method;
    NativeCodePtr interrupted_method_location;
    JIT *interrupted_method_jit;
    bool restore_guard_page = p_TLS_vmthread->restore_guard_page;

    if (!si_is_native(si))
    {
        CodeChunkInfo *interrupted_cci = si_get_code_chunk_info(si);
        assert(interrupted_cci);
        interrupted_method = interrupted_cci->get_method();
        interrupted_method_location = si_get_ip(si);
        interrupted_method_jit = interrupted_cci->get_jit();
    }
    else
    {
        interrupted_method = m2n_get_method(si_get_m2n(si));
        interrupted_method_location = 0;
        interrupted_method_jit = 0;
    }

    if (NULL != *exn_obj)
    {
        // Gregory - When *exn_obj is NULL it means we're called from exn_athrow_regs
        // which means that IP points exactly to the right location. But
        // when *exn_obj is not NULL, it means that we're called from exn_throw_for_JIT
        // where *exn_obj is already constructed and is thrown by code via athrow.
        // So in this case IP reported by stack iterator is past the athrow bytecode
        // and should be moved back to be inside of bytecode location for interrupted
        // method.

        interrupted_method_location = (NativeCodePtr)((POINTER_SIZE_INT)interrupted_method_location - 1);

        // Determine the type of the exception for the type tests below.
        exn_class = (*exn_obj)->vt()->clss;
    }

#ifdef VM_STATS
    assert(exn_class);
    exn_class->class_thrown();
    UNSAFE_REGION_START
    VM_Statistics::get_vm_stats().num_exceptions++;
    UNSAFE_REGION_END
#endif // VM_STATS

    // Remove single step breakpoints which could have been set on the
    // exception bytecode
    DebugUtilsTI *ti = VM_Global_State::loader_env->TI;
    if (ti->isEnabled() && ti->is_single_step_enabled())
    {
        jvmti_thread_t jvmti_thread = jthread_self_jvmti();
        ti->vm_brpt->lock();
        if (NULL != jvmti_thread->ss_state)
        {
            jvmti_remove_single_step_breakpoints(ti, jvmti_thread);
        }
        ti->vm_brpt->unlock();
    }

    // When VM is in shutdown stage we need to execute "finally" clause to
    // release monitors and propagate an exception to the upper frames.
    Class_Handle search_exn_class = !VM_Global_State::loader_env->IsVmShutdowning()
        ? exn_class : VM_Global_State::loader_env->JavaLangObject_Class;

    if (!si_is_native(si))
    {
        bool same_frame = true;
        while (!si_is_past_end(si) && !si_is_native(si)) {
            CodeChunkInfo *cci = si_get_code_chunk_info(si);
            assert(cci);
            Method *method = cci->get_method();
            JIT *jit = cci->get_jit();
            assert(method && jit);
            NativeCodePtr ip = si_get_ip(si);
            bool is_ip_past = !!si_get_jit_context(si)->is_ip_past;

#ifdef VM_STATS
            cci->num_throws++;
#endif // VM_STATS

            // Examine this frame's exception handlers looking for a match
            unsigned num_handlers = cci->get_num_target_exception_handlers();
            for (unsigned i = 0; i < num_handlers; i++) {
                Target_Exception_Handler_Ptr handler =
                    cci->get_target_exception_handler_info(i);
                if (!handler)
                    continue;
                if (handler->is_in_range(ip, is_ip_past)
                    && handler->is_assignable(search_exn_class)) {
                    // Found a handler that catches the exception.
#ifdef VM_STATS
                    cci->num_catches++;
                    if (same_frame) {
                        VM_Statistics::get_vm_stats().num_exceptions_caught_same_frame++;
                    }
                    if (handler->is_exc_obj_dead()) {
                        VM_Statistics::get_vm_stats().num_exceptions_dead_object++;
                        if (!*exn_obj) {
                            VM_Statistics::get_vm_stats().num_exceptions_object_not_created++;
                        }
                    }
#endif // VM_STATS

                    if (restore_guard_page) {
                        bool res = check_stack_size_enough_for_exception_catch(si_get_sp(si));
                        //must always be enough. otherwise program behavior is unspecified: finally blocks, monitor exits are not executed
                        assert(res); 
                        if (!res) {
                            break;
                        }

                    }

                    // Setup handler context
                    jit->fix_handler_context(method, si_get_jit_context(si));
                    si_set_ip(si, handler->get_handler_ip(), false);

                    // Start single step in exception handler
                    if (ti->isEnabled() && ti->is_single_step_enabled())
                    {
                        jvmti_thread_t jvmti_thread = jthread_self_jvmti();
                        ti->vm_brpt->lock();
                        if (NULL != jvmti_thread->ss_state)
                        {
                            uint16 bc;
                            NativeCodePtr ip = handler->get_handler_ip();
                            OpenExeJpdaError UNREF result =
                                jit->get_bc_location_for_native(method, ip, &bc);
                            assert(EXE_ERROR_NONE == result);

                            jvmti_StepLocation method_start = {(Method *)method, ip, bc, false};

                            jvmti_set_single_step_breakpoints(ti, jvmti_thread,
                                &method_start, 1);
                        }
                        ti->vm_brpt->unlock();
                    }

                    // Create exception if necessary
                    if (!*exn_obj && !handler->is_exc_obj_dead()) {
                        assert(!exn_raised());

                        *exn_obj = create_lazy_exception(exn_class, exn_constr,
                            jit_exn_constr_args, vm_exn_constr_args);
                    }

                    if (jvmti_is_exception_event_requested()) {
                        // Create exception if necessary
                        if (NULL == *exn_obj) {
                            *exn_obj = create_lazy_exception(exn_class, exn_constr,
                                jit_exn_constr_args, vm_exn_constr_args);
                        }

                        // Reload exception object pointer because it could have
                        // moved while calling JVMTI callback

                        *exn_obj = jvmti_jit_exception_event_callback_call(*exn_obj,
                            interrupted_method_jit, interrupted_method,
                            interrupted_method_location,
                            jit, method, handler->get_handler_ip());
                    }

                    CTRACE(("setting return pointer to %d", exn_obj));

                    si_set_return_pointer(si, (void **) exn_obj);
                    //si_free(throw_si);
                    return NULL;
                }
            }

            // No appropriate handler found, undo synchronization
            vm_monitor_exit_synchronized_method(si);

            jvalue ret_val = {(jlong)0};
            jvmti_process_method_exception_exit_event(
                reinterpret_cast<jmethodID>(method), JNI_TRUE, ret_val, si);

            // Goto previous frame
            si_goto_previous(si);
            same_frame = false;
        }
    }
    // Exception propagates to the native code
    assert(si_is_native(si));

    // The current thread exception is set to the exception and we return 0/NULL to the native code
    if (*exn_obj == NULL) {
        *exn_obj = create_lazy_exception(exn_class, exn_constr,
            jit_exn_constr_args, vm_exn_constr_args);
    }
    assert(!hythread_is_suspend_enabled());

    CodeChunkInfo *catch_cci = si_get_code_chunk_info(si);
    Method *catch_method = NULL;
    if (catch_cci)
        catch_method = catch_cci->get_method();

    // Reload exception object pointer because it could have
    // moved while calling JVMTI callback
    if (exn_raised()) {
        //si_free(throw_si);
        return NULL;
    }

    *exn_obj = jvmti_jit_exception_event_callback_call(*exn_obj,
        interrupted_method_jit, interrupted_method, interrupted_method_location,
        NULL, NULL, NULL);

    //si_free(throw_si);
    return *exn_obj;
}   //exn_propagate_exception

#ifndef _IPF_
// Alexei
// Check if we could proceed with destructive stack unwinding,
// i. e. the last GC frame is created before the last m2n frame.
// We use here a knowledge that the newer stack objects
// have smaller addresses for ia32 and em64t architectures.
static bool UNUSED is_gc_frame_before_m2n_frame()
{
    if (p_TLS_vmthread->gc_frames) {
        POINTER_SIZE_INT m2n_address =
            (POINTER_SIZE_INT) m2n_get_last_frame();
        POINTER_SIZE_INT gc_frame_address =
            (POINTER_SIZE_INT) p_TLS_vmthread->gc_frames;
        // gc frame is created before the last m2n frame
        return m2n_address < gc_frame_address;
    }
    else {
        return true;    // no gc frames - nothing to be broken
    }
}
#endif // _IPF_

// function can be safe point & should be called with disable reqursion = 1
void exn_throw_for_JIT(ManagedObject* exn_obj, Class_Handle exn_class,
    Method_Handle exn_constr, U_8* jit_exn_constr_args, jvalue* vm_exn_constr_args)
{
/*
 * !!!! NO LOGGER IS ALLOWED IN THIS FUNCTION !!!
 * !!!! RELEASE BUILD WILL BE BROKEN          !!!
 * !!!! NO TRACE2, INFO, WARN, ECHO, ASSERT, ...
 */
    assert(!hythread_is_suspend_enabled());

    if(exn_raised()) {
        return;
    }

    ASSERT_NO_INTERPRETER
    ASSERT_RAISE_AREA;

    if ((exn_obj == NULL) && (exn_class == NULL)) {
        exn_class = VM_Global_State::loader_env->java_lang_NullPointerException_Class;
    }
    ManagedObject* local_exn_obj = exn_obj;
    StackIterator* si = (StackIterator*) STD_ALLOCA(si_size());
    si_fill_from_native(si);

    if (exn_raised()) {
        return;
    }

#ifndef _IPF_
    assert(is_gc_frame_before_m2n_frame());
#endif // _IPF_

    assert(!exn_raised());

    if (si_is_past_end(si)) {
        //FIXME LAZY EXCEPTION (2006.05.12)
        // should be replaced by lazy version
        set_exception_object_internal(local_exn_obj);
        return;
    }

    si_transfer_all_preserved_registers(si);
    assert(!exn_raised());

    DebugUtilsTI* ti = VM_Global_State::loader_env->TI;
    exn_obj = exn_propagate_exception(si, &local_exn_obj, exn_class, exn_constr,
        jit_exn_constr_args, vm_exn_constr_args);

    if (exn_raised()) {
        //si_free(si);
        return;
    }

    M2nFrame* m2nFrame = m2n_get_last_frame();
    ObjectHandles* last_m2n_frame_handles = m2n_get_local_handles(m2nFrame);

    if (last_m2n_frame_handles) {
        free_local_object_handles2(last_m2n_frame_handles);
    }
    set_exception_object_internal(exn_obj);

    if (ti->get_global_capability(DebugUtilsTI::TI_GC_ENABLE_EXCEPTION_EVENT)) {
        Registers regs = {0};
        VM_thread *thread = p_TLS_vmthread;
        NativeCodePtr callback = (NativeCodePtr)
                jvmti_exception_catch_callback;

        si_copy_to_registers(si, &regs);
        vm_set_exception_registers(thread, regs);
        si_set_callback(si, &callback);
    } else if (p_TLS_vmthread->restore_guard_page) {
        Registers regs = {0};
        VM_thread *thread = p_TLS_vmthread;
        NativeCodePtr callback = (NativeCodePtr)
                exception_catch_callback;
        si_copy_to_registers(si, &regs);
        vm_set_exception_registers(thread, regs);
        si_set_callback(si, &callback);
    }

    // don't put any call here
    si_transfer_control(si);
}   //exn_throw_for_JIT

// Throw an exception in the current thread.
// Must be called with an M2nFrame on the top of the stack, and throws to the previous managed
// frames or the previous M2nFrame.
// Exception defined as in previous function.
// Does not return.

// function can be safe point & should be called with disable reqursion = 1
void exn_athrow(ManagedObject* exn_obj, Class_Handle exn_class,
    Method_Handle exn_constr, U_8* exn_constr_args)
{
    assert(!hythread_is_suspend_enabled());
    BEGIN_RAISE_AREA;
    exn_throw_for_JIT(exn_obj, exn_class, exn_constr, exn_constr_args, NULL);
    END_RAISE_AREA;
}


// Throw an exception in the current thread.
// Must be called with the current thread "suspended" in managed code and regs holds the suspended values.
// Exception defined as in previous two functions.
// Mutates the regs value, which should be used to "resume" the managed code.

// function can be safe point & should be called with disable reqursion = 1
void exn_athrow_regs(Registers * regs, Class_Handle exn_class, bool java_code, bool transfer_control)
{
    assert(!hythread_is_suspend_enabled());
    assert(exn_class);

#ifndef _IPF_
    M2nFrame *cur_m2nf = (M2nFrame *) STD_ALLOCA(m2n_get_size());
    M2nFrame *unw_m2nf;
    ManagedObject *exn_obj = NULL;
    StackIterator *si;
    DebugUtilsTI* ti = VM_Global_State::loader_env->TI;
    VM_thread* vmthread = p_TLS_vmthread;

    if (java_code)
        m2n_push_suspended_frame(vmthread, cur_m2nf, regs);
    else
        // Gregory -
        // Initialize cur_m2nf pointer in case we've crashed in native code that is unwindable,
	// e.g. in the code that sets non-unwindable state for the native code area
        cur_m2nf = m2n_get_last_frame();

    BEGIN_RAISE_AREA;

    si = (StackIterator*) STD_ALLOCA(si_size());
    si_fill_from_native(si);
    ManagedObject *local_exn_obj = NULL;
    exn_obj = exn_propagate_exception(si, &local_exn_obj, exn_class, NULL, NULL, NULL);

    //free local handles
    ObjectHandles* last_m2n_frame_handles = m2n_get_local_handles(cur_m2nf);

    if (last_m2n_frame_handles) {
        free_local_object_handles2(last_m2n_frame_handles);
    }

    if (ti->get_global_capability(DebugUtilsTI::TI_GC_ENABLE_EXCEPTION_EVENT)) {
        VM_thread *thread = p_TLS_vmthread;
        NativeCodePtr callback = (NativeCodePtr)
                jvmti_exception_catch_callback;

        si_copy_to_registers(si, regs);
        vm_set_exception_registers(thread, *regs);
        si_set_callback(si, &callback);
    } else if (p_TLS_vmthread->restore_guard_page) {
        VM_thread *thread = p_TLS_vmthread;
        NativeCodePtr callback = (NativeCodePtr)
                exception_catch_callback;
        si_copy_to_registers(si, regs);
        vm_set_exception_registers(thread, *regs);
        si_set_callback(si, &callback);
    }

    si_copy_to_registers(si, regs);

    if (transfer_control) {
        // Let NCAI to continue single stepping in exception handler
        ncai_setup_signal_step(&vmthread->jvmti_thread, (NativeCodePtr)regs->get_ip());

        set_exception_object_internal(exn_obj);
        si_transfer_control(si);
        assert(!"si_transfer_control should not return");
    } 

    unw_m2nf = si_get_m2n(si);
    //si_free(si);

    END_RAISE_AREA;

    set_exception_object_internal(exn_obj);
    m2n_set_last_frame(unw_m2nf);
#endif
}   //exn_athrow_regs

//////////////////////////////////////////////////////////////////////////
// Exception Catch support

// exception catch callback to restore stack after Stack Overflow Error
void exception_catch_callback() {
    Registers regs = {0};
    VM_thread *thread = p_TLS_vmthread;
    assert(thread);

    if (thread->regs) {
        regs = *(Registers*)thread->regs;
    }

    M2nFrame* m2n = (M2nFrame *) STD_ALLOCA(m2n_get_size());
    m2n_push_suspended_frame(thread, m2n, &regs);
    M2nFrame* prev_m2n = m2n_get_previous_frame(m2n);

    StackIterator* si = (StackIterator*) STD_ALLOCA(si_size());
    si_fill_from_registers(si, &regs, false, prev_m2n);

    // si_create_from_registers uses large stack space,
    // so guard page restored after its invoke.
    if (p_TLS_vmthread->restore_guard_page) {
        int res = port_thread_restore_guard_page();

        if (res != 0) {
            Global_Env *env = VM_Global_State::loader_env;

            if (si_is_native(si)) {
                m2n_set_last_frame(prev_m2n);

                if ((interpreter_enabled() || (!prev_m2n) ||
                        (m2n_get_frame_type(prev_m2n) & FRAME_NON_UNWINDABLE))) {
                    exn_raise_by_class(env->java_lang_StackOverflowError_Class);
                } else {
                    //si_free(si);
                    exn_throw_by_class(env->java_lang_StackOverflowError_Class);
                }
            } else {
                //si_free(si);
                exn_throw_by_class(env->java_lang_StackOverflowError_Class);
            }
        }

        p_TLS_vmthread->restore_guard_page = false;
    }

    si_transfer_control(si);
}

// exception catch support for JVMTI, also restore stack after Stack Overflow Error
void jvmti_exception_catch_callback() {
    Registers regs = {0};
    VM_thread *thread = p_TLS_vmthread;
    assert(thread);

    if (thread->regs) {
        regs = *(Registers*)thread->regs;
    }

    M2nFrame* m2n = (M2nFrame *) STD_ALLOCA(m2n_get_size());
    m2n_push_suspended_frame(thread, m2n, &regs);
    M2nFrame* prev_m2n = m2n_get_previous_frame(m2n);

    StackIterator* si = (StackIterator*) STD_ALLOCA(si_size());
    si_fill_from_registers(si, &regs, false, prev_m2n);

    // si_create_from_registers uses large stack space,
    // so guard page restored after its invoke, 
    // but befor ti agent callback invokation, 
    // because it should work on protected page.
    if (p_TLS_vmthread->restore_guard_page) {
        int res = port_thread_restore_guard_page();

        if (res != 0) {
            Global_Env *env = VM_Global_State::loader_env;

            if (si_is_native(si)) {
                m2n_set_last_frame(prev_m2n);

                if ((interpreter_enabled() || (!prev_m2n) ||
                        (m2n_get_frame_type(prev_m2n) & FRAME_NON_UNWINDABLE))) {
                    exn_raise_by_class(env->java_lang_StackOverflowError_Class);
                } else {
                    //si_free(si);
                    exn_throw_by_class(env->java_lang_StackOverflowError_Class);
                }
            } else {
                //si_free(si);
                exn_throw_by_class(env->java_lang_StackOverflowError_Class);
            }
        }

        p_TLS_vmthread->restore_guard_page = false;
    }

    if (!si_is_native(si))
    {
        CodeChunkInfo* catch_cci = si_get_code_chunk_info(si);
        assert(catch_cci);
        Method* catch_method = catch_cci->get_method();
        NativeCodePtr catch_method_location = si_get_ip(si);
        JIT* catch_method_jit = catch_cci->get_jit();
        ManagedObject** exn_obj = (ManagedObject**) si_get_return_pointer(si);
        *exn_obj = jvmti_jit_exception_catch_event_callback_call( *exn_obj,
                catch_method_jit, catch_method, catch_method_location);
    }

    si_transfer_control(si);
}

//////////////////////////////////////////////////////////////////////////
// Runtime Exception Support

// rt_throw takes an exception and throws it
NativeCodePtr exn_get_rth_throw()
{
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
    }

    LilCodeStub *cs = lil_parse_code_stub("entry 0:stdcall:ref:void;"
        "push_m2n 0, 0;"
        "m2n_save_all;" "out platform:ref,pint,pint,pint:void;");
    assert(cs);

    REFS_RUNTIME_SWITCH_IF
#ifdef REFS_RUNTIME_OR_COMPRESSED
        cs = lil_parse_onto_end(cs,
            "jc i0=%0i:ref,%n;"
            "o0=i0;" "j %o;" ":%g;" "o0=0:ref;" ":%g;",
            VM_Global_State::loader_env->heap_base);
#endif // REFS_RUNTIME_OR_COMPRESSED
    REFS_RUNTIME_SWITCH_ELSE
#ifdef REFS_RUNTIME_OR_UNCOMPRESSED
        cs = lil_parse_onto_end(cs, "o0=i0;");
#endif // REFS_RUNTIME_OR_UNCOMPRESSED
    REFS_RUNTIME_SWITCH_ENDIF
    assert(cs);

    lil_parse_onto_end(cs,
        "o1=0;" "o2=0;" "o3=0;" "call.noret %0i;", exn_athrow);
    assert(cs);

    assert(lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_throw", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);
    return addr;
}   //exn_get_rth_throw


static void rth_throw_lazy(Method * exn_constr)
{
#if defined(_IPF_) || defined(_EM64T_)
    LDIE(61, "Lazy exceptions are not supported on this platform");
#else
    U_8 *args = (U_8 *) (m2n_get_args(m2n_get_last_frame()) + 1);   // +1 to skip constructor
    if (NULL != exn_constr) {
        args += exn_constr->get_num_arg_slots() * 4 - 4;
    } else {
        args += 1*4 /*default constructor*/ - 4;
    }
    exn_athrow(NULL, *(Class_Handle *) args, exn_constr, args);
#endif
}   //rth_throw_lazy


// rt_throw_lazy takes a constructor, the class for that constructor, and arguments for that constructor.
// it throws a (lazily created) instance of that class using that constructor and arguments.
NativeCodePtr exn_get_rth_throw_lazy()
{
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
    }

    const unsigned cap_off = (unsigned)(POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->capacity;
    const POINTER_SIZE_INT next_off = (POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->next;
    const unsigned handles_size = (unsigned)(sizeof(ObjectHandlesNew)+sizeof(ManagedObject*)*16);
    const unsigned cap_and_size = (unsigned)((0<<16) | 16);

#ifdef _IPF_
    LilCodeStub *cs = lil_parse_code_stub(
        "entry 0:managed:pint:void;"
        "push_m2n 0, 0, handles;"
        "m2n_save_all;"
        "in2out platform:void;"
        "call.noret %0i;",
        rth_throw_lazy);
#else
    LilCodeStub *cs = lil_parse_code_stub(
        "entry 0:managed:pint:void;"
        "push_m2n 0, 0, handles;"
        "m2n_save_all;"
        "locals 1;"
        "alloc l0, %0i;"
        "st[l0+%1i:g4], %2i;"
        "st[l0+%3i:pint], 0;"
        "handles=l0;"
        "in2out platform:void;"
        "call.noret %4i;",
        handles_size,
        cap_off, cap_and_size,
        next_off,
        rth_throw_lazy);
#endif
    assert(lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_throw_lazy", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);
    return addr;
}   //exn_get_rth_throw_lazy


// rt_throw_lazy_trampoline takes an exception class as first standard place
// and throws a (lazily created) instance of that class using the default constructor
NativeCodePtr exn_get_rth_throw_lazy_trampoline()
{
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
    }

    const unsigned cap_off = (unsigned)(POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->capacity;
    const POINTER_SIZE_INT next_off = (POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->next;
    const unsigned handles_size = (unsigned)(sizeof(ObjectHandlesNew)+sizeof(ManagedObject*)*16);
    const unsigned cap_and_size = (unsigned)((0<<16) | 16);

#ifdef _IPF_
    LilCodeStub *cs = lil_parse_code_stub("entry 1:stdcall::void;"
        "push_m2n 0, 0, handles;"
        "m2n_save_all;"
        "out platform:ref,pint,pint,pint:void;"
        "o0=0:ref;" "o1=sp0;" "o2=0;" "o3=0;" "call.noret %0i;",
        exn_athrow);
#else
    LilCodeStub *cs = lil_parse_code_stub("entry 1:stdcall::void;"
        "push_m2n 0, 0, handles;"
        "m2n_save_all;"
        "locals 1;"
        "alloc l0, %0i;"
        "st[l0+%1i:g4], %2i;"
        "st[l0+%3i:pint], 0;"
        "handles=l0;"
        "out platform:ref,pint,pint,pint:void;"
        "o0=0:ref;" "o1=sp0;" "o2=0;" "o3=0;" "call.noret %4i;",
        handles_size,
        cap_off, cap_and_size,
        next_off,
        exn_athrow);
#endif
    assert(lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_throw_lazy_trampoline", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);
    return addr;
}   //exn_get_rth_throw_lazy_trampoline


// rth_throw_null_pointer throws a null pointer exception (lazily)
NativeCodePtr exn_get_rth_throw_null_pointer()
{
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
    }

    Class *exn_clss =
        VM_Global_State::loader_env->java_lang_NullPointerException_Class;
    LilCodeStub *cs =
        lil_parse_code_stub("entry 0:stdcall::void;" "std_places 1;"
        "sp0=%0i;" "tailcall %1i;",
        exn_clss,
        lil_npc_to_fp(exn_get_rth_throw_lazy_trampoline()));
    assert(lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_throw_null_pointer", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);

    return addr;
}   //exn_get_rth_throw_null_pointer

// Return the type of illegal monitor state exception
Class_Handle exn_get_illegal_monitor_state_exception_type()
{
    static Class *exn_clss = NULL;

    if (exn_clss != NULL) {
        return exn_clss;
    }

    Global_Env *env = VM_Global_State::loader_env;
    String *exc_str =
        env->string_pool.lookup("java/lang/IllegalMonitorStateException");
    exn_clss =
        env->bootstrap_class_loader->LoadVerifyAndPrepareClass(env, exc_str);
    assert(exn_clss);

    return exn_clss;
}

// rth_throw_illegal_monitor_state throws an java.lang.IllegalMonitorStateException (lazily)
NativeCodePtr exn_get_rth_throw_illegal_monitor_state() {
    static NativeCodePtr addr = NULL;

    if (addr) {
        return addr;
    }

    LilCodeStub *cs = lil_parse_code_stub("entry 0:stdcall::void;"
        "std_places 1;" "sp0=%0i;" "tailcall %1i;",
        exn_get_illegal_monitor_state_exception_type(),
        lil_npc_to_fp(exn_get_rth_throw_lazy_trampoline())
    );
    assert(lil_is_valid(cs));

    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_throw_illegal_monitor_state", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);

    return addr;
}   //exn_get_rth_throw_null_pointer


// rth_throw_array_index_out_of_bounds throws an array index out of bounds exception (lazily)
NativeCodePtr exn_get_rth_throw_array_index_out_of_bounds()
{
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
    }

    Global_Env *env = VM_Global_State::loader_env;
    Class *exn_clss = env->java_lang_ArrayIndexOutOfBoundsException_Class;
    LilCodeStub *cs = lil_parse_code_stub("entry 0:stdcall::void;"
        "std_places 1;" "sp0=%0i;" "tailcall %1i;",
        exn_clss,
        lil_npc_to_fp(exn_get_rth_throw_lazy_trampoline()));
    assert(lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_throw_array_index_out_of_bounds", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);
    return addr;
}   //exn_get_rth_throw_array_index_out_of_bounds


// Return the type of negative array size exception
Class_Handle exn_get_negative_array_size_exception_type()
{
    assert(hythread_is_suspend_enabled());
    Class *exn_clss;


    Global_Env *env = VM_Global_State::loader_env;
    String *exc_str =
        env->string_pool.lookup("java/lang/NegativeArraySizeException");
    exn_clss =
        env->bootstrap_class_loader->LoadVerifyAndPrepareClass(env, exc_str);
    assert(exn_clss);


    return exn_clss;
}

// rth_throw_negative_array_size throws a negative array size exception (lazily)
NativeCodePtr exn_get_rth_throw_negative_array_size()
{
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
    }

    LilCodeStub *cs = lil_parse_code_stub("entry 0:stdcall::void;"
        "std_places 1;" "sp0=%0i;" "tailcall %1i;",
        exn_get_negative_array_size_exception_type(),
        lil_npc_to_fp(exn_get_rth_throw_lazy_trampoline()));
    assert(lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_throw_negative_array_size", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);

    return addr;
}   //exn_get_rth_throw_negative_array_size


// Return the type of illegal state exception
Class_Handle exn_get_illegal_state_exception_type()
{
    assert(hythread_is_suspend_enabled());
    Class *exn_clss;


    Global_Env *env = VM_Global_State::loader_env;
    String *exc_str =
        env->string_pool.lookup("java/lang/IllegalMonitorStateException");
    exn_clss =
        env->bootstrap_class_loader->LoadVerifyAndPrepareClass(env, exc_str);
    assert(exn_clss);

    return exn_clss;
}

// rth_throw_negative_array_size throws a negative array size exception (lazily)
NativeCodePtr exn_get_rth_throw_illegal_state_exception()
{
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
    }

    LilCodeStub *cs = lil_parse_code_stub("entry 0:stdcall::void;"
        "std_places 1;" "sp0=%0i;" "tailcall %1i;",
        exn_get_illegal_state_exception_type(),
        lil_npc_to_fp(exn_get_rth_throw_lazy_trampoline()));
    assert(lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_throw_illegal_state_exception", lil_cs_get_code_size(cs));
    lil_free_code_stub(cs);

    return addr;
}   //exn_get_rth_throw_illegal_state_exception


// rth_throw_array_store throws an array store exception (lazily)
NativeCodePtr exn_get_rth_throw_array_store()
{
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
    }

    Global_Env *env = VM_Global_State::loader_env;
    LilCodeStub *cs = lil_parse_code_stub("entry 0:stdcall::void;"
        "std_places 1;" "sp0=%0i;" "tailcall %1i;",
        env->java_lang_ArrayStoreException_Class,
        lil_npc_to_fp(exn_get_rth_throw_lazy_trampoline()));
    assert(lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_throw_array_store", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);

    return addr;
}   //exn_get_rth_throw_array_store


// rth_throw_arithmetic throws an arithmetic exception (lazily)
//NativeCodePtr exn_get_rth_throw_arithmetic()
//{
//    static NativeCodePtr addr = NULL;
//    if (addr) {
//        return addr;
//    }
//
//    Global_Env *env = VM_Global_State::loader_env;
//    LilCodeStub *cs = lil_parse_code_stub("entry 0:stdcall::void;"
//        "std_places 1;" "sp0=%0i;" "tailcall %1i;",
//        env->java_lang_ArithmeticException_Class,
//        lil_npc_to_fp(exn_get_rth_throw_lazy_trampoline()));
//    assert(lil_is_valid(cs));
//    addr = LilCodeGenerator::get_platform()->compile(cs);
//
//    DUMP_STUB(addr, "rth_throw_arithmetic", lil_cs_get_code_size(cs));
//
//    lil_free_code_stub(cs);
//
//    return addr;
//}   //exn_get_rth_throw_arithmetic


// Return the type of class cast exception
Class_Handle exn_get_class_cast_exception_type()
{
    assert(hythread_is_suspend_enabled());
    return VM_Global_State::loader_env->java_lang_ClassCastException_Class;
}

// rth_throw_class_cast_exception throws a class cast exception (lazily)
NativeCodePtr exn_get_rth_throw_class_cast_exception()
{
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
    }

    LilCodeStub *cs = lil_parse_code_stub("entry 0:stdcall::void;"
        "std_places 1;" "sp0=%0i;" "tailcall %1i;",
        exn_get_class_cast_exception_type(),
        lil_npc_to_fp(exn_get_rth_throw_lazy_trampoline()));
    assert(cs && lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_throw_class_cast_exception", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);
    return addr;
}   //exn_get_rth_throw_class_cast_exception


// Return the type of incompatible class change exception
Class_Handle exn_get_incompatible_class_change_exception_type()
{
    assert(hythread_is_suspend_enabled());
    Class *exn_clss;

    Global_Env *env = VM_Global_State::loader_env;
    String *exc_str =
        env->string_pool.lookup("java/lang/IncompatibleClassChangeError");
    exn_clss =
        env->bootstrap_class_loader->LoadVerifyAndPrepareClass(env, exc_str);
    assert(exn_clss);


    return exn_clss;
}

// rth_throw_incompatible_class_change_exception throws an incompatible class change exception (lazily)
NativeCodePtr exn_get_rth_throw_incompatible_class_change_exception()
{
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
    }

    LilCodeStub *cs = lil_parse_code_stub("entry 0:stdcall::void;"
        "std_places 1;" "sp0=%0i;" "tailcall %1i;",
        exn_get_incompatible_class_change_exception_type(),
        lil_npc_to_fp(exn_get_rth_throw_lazy_trampoline()));
    assert(cs && lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "rth_throw_incompatible_class_change_exception", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);
    return addr;
}   //exn_get_rth_throw_incompatible_class_change_exception


