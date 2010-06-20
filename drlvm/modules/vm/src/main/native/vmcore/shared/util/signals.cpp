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

#define LOG_DOMAIN "signals"
#include "cxxlog.h"

#include "open/platform_types.h"
#include "port_crash_handler.h"
#include "init.h"
#include "vm_threads.h"
#include "environment.h"
#include "exceptions.h"
#include "signals.h"


#ifdef PLATFORM_POSIX

#if defined(_EM64T_)
#define RESTORE_STACK_SIZE 0x0400
#elif defined (_IA32_)
#define RESTORE_STACK_SIZE 0x0100
#else // IPF
#define RESTORE_STACK_SIZE 0x0200
#endif

#else // WINDOWS
#define RESTORE_STACK_SIZE 0x0100
#endif


size_t get_available_stack_size()
{
    size_t stack_addr = (size_t)port_thread_get_stack_address();
    size_t stack_size = port_thread_get_effective_stack_size();
    size_t used_stack_size = stack_addr - (size_t)&stack_size;
    size_t available_stack_size = stack_size - used_stack_size;

    return (available_stack_size > 0) ? available_stack_size : 0;
}

bool check_available_stack_size(size_t required_size)
{
    size_t available_stack_size = get_available_stack_size();

    if (available_stack_size < required_size)
    {
        port_thread_clear_guard_page();
        p_TLS_vmthread->restore_guard_page = true;
        Global_Env *env = VM_Global_State::loader_env;
        exn_raise_by_class(env->java_lang_StackOverflowError_Class);
        return false;
    }

    return true;
}

static inline size_t get_available_stack_size(void* sp) {
    size_t stack_addr = (size_t)port_thread_get_stack_address();
    size_t stack_size = port_thread_get_effective_stack_size();
    size_t used_stack_size = stack_addr - (size_t)sp;
    size_t available_stack_size = stack_size - used_stack_size;

    return (available_stack_size > 0) ? available_stack_size : 0;
}

bool check_stack_size_enough_for_exception_catch(void* sp)
{
    size_t stack_addr = (size_t)port_thread_get_stack_address();
    size_t stack_size = port_thread_get_effective_stack_size();
    size_t used_stack_size = stack_addr - (size_t)sp;
    size_t available_stack_size = stack_size - used_stack_size;

    return RESTORE_STACK_SIZE < available_stack_size;
}

Boolean stack_overflow_handler(port_sigtype UNREF signum, Registers* regs, void* fault_addr)
{
    TRACE2("signals", ("SOE detected at ip=%p, sp=%p",
                            regs->get_ip(), regs->get_sp()));

    vm_thread_t vmthread = get_thread_ptr();
    Global_Env* env = VM_Global_State::loader_env;
    void* saved_ip = regs->get_ip();
    void* new_ip = NULL;

    if (is_in_ti_handler(vmthread, saved_ip))
    {
        new_ip = vm_get_ip_from_regs(vmthread);
        regs->set_ip(new_ip);
    }

    if (!vmthread || env == NULL)
        return FALSE; // Crash

    port_thread_postpone_guard_page();
    vmthread->restore_guard_page = true;

    // Pass exception to NCAI exception handler
    bool is_handled = 0;
    ncai_process_signal_event((NativeCodePtr)regs->get_ip(),
                                (jint)signum, false, &is_handled);
    if (is_handled)
    {
        if (new_ip)
            regs->set_ip(saved_ip);
        return TRUE;
    }

    Class* exn_class = env->java_lang_StackOverflowError_Class;

    if (is_in_java(regs))
    {
        signal_throw_java_exception(regs, exn_class);
    }
    else if (is_unwindable())
    {
        if (hythread_is_suspend_enabled())
            hythread_suspend_disable();
        signal_throw_exception(regs, exn_class);
    } else {
        exn_raise_by_class(exn_class);
    }

    if (new_ip && regs->get_ip() == new_ip)
        regs->set_ip(saved_ip);

    return TRUE;
}

Boolean null_reference_handler(port_sigtype UNREF signum, Registers* regs, void* fault_addr)
{
    TRACE2("signals", "NPE detected at " << regs->get_ip());

    vm_thread_t vmthread = get_thread_ptr();
    Global_Env* env = VM_Global_State::loader_env;
    void* saved_ip = regs->get_ip();
    void* new_ip = NULL;

    if (is_in_ti_handler(vmthread, saved_ip))
    {
        new_ip = vm_get_ip_from_regs(vmthread);
        regs->set_ip(new_ip);
    }

    if (!vmthread || env == NULL)
        return FALSE; // Crash

    if (!is_in_java(regs) || interpreter_enabled())
        return FALSE; // Crash

    // Pass exception to NCAI exception handler
    bool is_handled = 0;
    ncai_process_signal_event((NativeCodePtr)regs->get_ip(),
                                (jint)signum, false, &is_handled);
    if (is_handled)
    {
        if (new_ip)
            regs->set_ip(saved_ip);
        return TRUE;
    }

    signal_throw_java_exception(regs, env->java_lang_NullPointerException_Class);

    if (new_ip && regs->get_ip() == new_ip)
        regs->set_ip(saved_ip);

    return TRUE;
}

Boolean abort_handler(port_sigtype UNREF signum, Registers* UNREF regs, void* fault_addr)
{
    TRACE2("signals", "Abort detected at " << regs->get_ip());
    // Crash always
    return FALSE;
}

Boolean ctrl_backslash_handler(port_sigtype UNREF signum, Registers* UNREF regs, void* UNREF fault_addr)
{
    TRACE2("signals", "Ctrl+\\ pressed");
    vm_dump_handler();
    return TRUE;
}

Boolean ctrl_break_handler(port_sigtype UNREF signum, Registers* UNREF regs, void* UNREF fault_addr)
{
    TRACE2("signals", "Ctrl+Break pressed");
    vm_dump_handler();
    return TRUE;
}

Boolean ctrl_c_handler(port_sigtype UNREF signum, Registers* UNREF regs, void* UNREF fault_addr)
{
    TRACE2("signals", "Ctrl+C pressed");
    vm_interrupt_handler();
    // Continue execution - the process will be terminated from
    // the separate thread started in vm_interrupt_handler()
    return TRUE;
}

Boolean native_breakpoint_handler(port_sigtype UNREF signum, Registers* regs, void* fault_addr)
{
    TRACE2("signals", "Native breakpoint detected at " << regs->get_ip());
#ifdef _IPF_
    LDIE(51, "Not implemented");
#endif

    if (VM_Global_State::loader_env == NULL || interpreter_enabled())
        return FALSE; // Crash

    vm_thread_t vmthread = get_thread_ptr();

    if (is_in_ti_handler(vmthread, regs->get_ip()))
        // Breakpoints should not occur in breakpoint buffer
        return FALSE; // Crash

    // Pass exception to NCAI exception handler
    bool is_handled = 0;
    ncai_process_signal_event((NativeCodePtr)regs->get_ip(),
                                (jint)signum, true, &is_handled);
    if (is_handled)
        return TRUE;

    return (Boolean)jvmti_jit_breakpoint_handler(regs);
}

Boolean arithmetic_handler(port_sigtype UNREF signum, Registers* regs, void* fault_addr)
{
    TRACE2("signals", "ArithmeticException detected at " << regs->get_ip());
#ifdef _IPF_
    LDIE(51, "Not implemented");
#endif

    vm_thread_t vmthread = get_thread_ptr();
    void* saved_ip = regs->get_ip();
    void* new_ip = NULL;

    if (is_in_ti_handler(vmthread, saved_ip))
    {
        new_ip = vm_get_ip_from_regs(vmthread);
        regs->set_ip(new_ip);
    }

    // Pass exception to NCAI exception handler
    bool is_handled = 0;
    ncai_process_signal_event((NativeCodePtr)regs->get_ip(),
                                (jint)signum, false, &is_handled);
    if (is_handled)
    {
        if (new_ip)
            regs->set_ip(saved_ip);
        return TRUE;
    }

    if (!vmthread || VM_Global_State::loader_env == NULL ||
        !is_in_java(regs) || interpreter_enabled())
        return FALSE; // Crash

    signal_throw_java_exception(regs, VM_Global_State::loader_env->java_lang_ArithmeticException_Class);

    if (new_ip && regs->get_ip() == new_ip)
        regs->set_ip(saved_ip);

    return TRUE;
}
