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

#include <sys/mman.h>
#include <limits.h>
#include <unistd.h>
#undef __USE_XOPEN
#include <signal.h>

#include "open/platform_types.h"
#include "port_crash_handler.h"
#include "port_malloc.h"
#include "stack_dump.h"
#include "../linux/include/gdb_crash_handler.h"
#include "signals_internal.h"
#include "port_thread_internal.h"


#define FLAG_CORE ((port_crash_handler_get_flags() & PORT_CRASH_DUMP_PROCESS_CORE) != 0)
#define FLAG_DBG  ((port_crash_handler_get_flags() & PORT_CRASH_CALL_DEBUGGER) != 0)


bool is_stack_overflow(port_tls_data_t* tlsdata, void* fault_addr)
{
    if (!tlsdata || !fault_addr)
        return false;

    if (tlsdata->guard_page_addr)
    {
        return (fault_addr >= tlsdata->guard_page_addr &&
                (size_t)fault_addr < (size_t)tlsdata->guard_page_addr
                                                + tlsdata->guard_page_size);
    }

    size_t stack_top =
        (size_t)tlsdata->stack_addr - tlsdata->stack_size + tlsdata->guard_page_size;

    // Determine that fault is beyond stack top
    return ((size_t)fault_addr < stack_top &&
            (size_t)fault_addr > stack_top - tlsdata->stack_size);
}

static void c_handler(Registers* pregs, size_t signum, void* fault_addr)
{ // this exception handler is executed *after* OS signal handler returned
    int result;
    port_tls_data_t* tlsdata = get_private_tls_data();

    switch ((int)signum)
    {
    case SIGSEGV:
        if (tlsdata->restore_guard_page)
        {
            // Now it's safe to disable alternative stack
            set_alt_stack(tlsdata, FALSE);
            result = port_process_signal(PORT_SIGNAL_STACK_OVERFLOW, pregs, fault_addr, FALSE);
        }
        else
            result = port_process_signal(PORT_SIGNAL_GPF, pregs, fault_addr, FALSE);
        break;
    case SIGFPE:
        result = port_process_signal(PORT_SIGNAL_ARITHMETIC, pregs, fault_addr, FALSE);
        break;
    case SIGTRAP:
        // Correct return address
        pregs->set_ip((void*)((POINTER_SIZE_INT)pregs->get_ip() - 1));
        result = port_process_signal(PORT_SIGNAL_BREAKPOINT, pregs, fault_addr, FALSE);
        break;
    case SIGINT:
        result = port_process_signal(PORT_SIGNAL_CTRL_C, pregs, fault_addr, FALSE);
        break;
    case SIGQUIT:
        result = port_process_signal(PORT_SIGNAL_QUIT, pregs, fault_addr, FALSE);
        break;
    case SIGABRT:
        result = port_process_signal(PORT_SIGNAL_ABORT, NULL, fault_addr, FALSE);
        break;
    default:
        result = port_process_signal(PORT_SIGNAL_UNKNOWN, pregs, fault_addr, TRUE);
    }

    if (result == 0)
    {
        // Restore guard page if needed
        if (tlsdata->restore_guard_page)
        {
            port_thread_restore_guard_page();
            tlsdata->restore_guard_page = FALSE;

            if (port_thread_detach_temporary() == 0)
                STD_FREE(tlsdata);
        }
        return;
    }

    // We've got a crash
    if (signum == SIGSEGV)
    {
        port_thread_restore_guard_page(); // To catch SO again
        tlsdata->restore_guard_page = FALSE;
    }

    if (result > 0) // invoke debugger
    { // Prepare second catch of signal to attach GDB from signal handler
        //assert(tlsdata); // Should be attached - provided by general_signal_handler
        tlsdata->debugger = TRUE;
        return; // To produce signal again
    }

    // result < 0 - exit process
    if (FLAG_CORE)
    { // Return to the same place to produce the same crash and generate core
        signal(signum, SIG_DFL); // setup default handler
        return;
    }

    // No core needed - simply terminate
    _exit(-1);
}

static void general_signal_handler(int signum, siginfo_t* info, void* context)
{
    Registers regs;

    if (!context)
        return;

    // Convert OS context to Registers
    port_thread_context_to_regs(&regs, (ucontext_t*)context);
    void* fault_addr = info ? info->si_addr : NULL;

    // Check if SIGSEGV is produced by port_read/write_memory
    port_tls_data_t* tlsdata = get_private_tls_data();
    if (tlsdata && tlsdata->violation_flag)
    {
        tlsdata->violation_flag = 0;
        regs.set_ip(tlsdata->restart_address);
        return;
    }

    if (!tlsdata) // Tread is not attached - attach thread temporarily
    {
        int res;
        tlsdata = (port_tls_data_t*)STD_MALLOC(sizeof(port_tls_data_t));

        if (tlsdata) // Try to attach the thread
            res = port_thread_attach_local(tlsdata, TRUE, TRUE, 0);

        if (!tlsdata || res != 0)
        { // Can't process correctly; perform default actions
            if (FLAG_DBG)
            {
                bool result = gdb_crash_handler(&regs);
                _exit(-1); // Exit process if not sucessful...
            }

            if (FLAG_CORE &&
                signum != SIGABRT) // SIGABRT can't be rethrown
            {
                signal(signum, SIG_DFL); // setup default handler
                return;
            }

            _exit(-1);
        }

        // SIGSEGV can represent SO which can't be processed out of signal handler
        if (signum == SIGSEGV && // This can occur only when a user set an alternative stack
            is_stack_overflow(tlsdata, fault_addr))
        {
            int result = port_process_signal(PORT_SIGNAL_STACK_OVERFLOW, &regs, fault_addr, FALSE);

            if (result == 0)
            {
                if (port_thread_detach_temporary() == 0)
                    STD_FREE(tlsdata);
                return;
            }

            if (result > 0)
                tlsdata->debugger = TRUE;
            else
            {
                if (FLAG_CORE)
                { // Rethrow crash to generate core
                    signal(signum, SIG_DFL); // setup default handler
                    return;
                }
                _exit(-1);
            }
        }
    }

    if (tlsdata->debugger)
    {
        bool result = gdb_crash_handler(&regs);
        _exit(-1); // Exit process if not sucessful...
    }

    if (signum == SIGABRT && // SIGABRT can't be trown again from c_handler
        FLAG_DBG)
    { // So attaching GDB right here
        bool result = gdb_crash_handler(&regs);
        _exit(-1); // Exit process if not sucessful...
    }

    if (signum == SIGSEGV &&
        is_stack_overflow(tlsdata, fault_addr))
    {
        // Second SO while previous SO is not processed yet - is GPF
        if (tlsdata->restore_guard_page)
            tlsdata->restore_guard_page = FALSE;
        else
        { // To process signal on protected stack area
            port_thread_clear_guard_page();
            // Note: the call above does not disable alternative stack
            // It can't be made while we are on alternative stack
            // Alt stack will be disabled explicitly in c_handler()
            tlsdata->restore_guard_page = TRUE;
        }
    }

    // Prepare registers for transfering control out of signal handler
    void* callback = (void*)&c_handler;

    port_set_longjump_regs(callback, &regs, 3,
                            &regs, (void*)(size_t)signum, fault_addr);

    // Convert prepared Registers back to OS context
    port_thread_regs_to_context((ucontext_t*)context, &regs);
    // Return from signal handler to go to C handler
}


struct sig_reg
{
    int signal;
    port_sigtype port_sig;
    int flags;
    bool set_up;
};

static sig_reg signals_used[] =
{
    { SIGTRAP, PORT_SIGNAL_BREAKPOINT, SA_SIGINFO, false },
    { SIGSEGV, PORT_SIGNAL_GPF,        SA_SIGINFO | SA_ONSTACK, false },
    { SIGFPE,  PORT_SIGNAL_ARITHMETIC, SA_SIGINFO, false },
    { SIGINT,  PORT_SIGNAL_CTRL_C,     SA_SIGINFO, false },
    { SIGQUIT, PORT_SIGNAL_QUIT,       SA_SIGINFO, false },
    { SIGABRT, PORT_SIGNAL_ABORT,      SA_SIGINFO, false }
};

static struct sigaction old_actions[sizeof(signals_used)/sizeof(signals_used[0])];

// For signals that must change their default behavior
struct sig_redef
{
    int     signal;
    sig_t   handler;
    bool    set_up;
};

static sig_redef signals_other[] =
{
    { SIGPIPE, SIG_IGN, false }
};

static sig_t old_handlers[sizeof(signals_other)/sizeof(signals_other[0])];


static void restore_signals()
{
    for (size_t i = 0; i < sizeof(signals_used)/sizeof(signals_used[0]); i++)
    {
        if (!signals_used[i].set_up)
            continue;

        signals_used[i].set_up = false;
        sigaction(signals_used[i].signal, &old_actions[i], NULL);
    }
    for (size_t j = 0; j < sizeof(signals_other)/sizeof(signals_other[0]); j++)
    {
        if (!signals_other[j].set_up)
            continue;

        signals_other[j].set_up = false;
        signal(signals_other[j].signal, old_handlers[j]);
    }
}

int initialize_signals()
{
    struct sigaction sa;

    for (size_t i = 0; i < sizeof(signals_used)/sizeof(signals_used[0]); i++)
    {
        if (!sd_is_handler_registered(signals_used[i].port_sig) &&
            signals_used[i].signal != SIGSEGV) // Sigsegv is needed for port_memaccess
            continue;

        sigemptyset(&sa.sa_mask);
        sa.sa_flags = signals_used[i].flags;
        sa.sa_sigaction = &general_signal_handler;

        if (0 != sigaction(signals_used[i].signal, &sa, &old_actions[i]))
        {
            restore_signals();
            return -1;
        }

        signals_used[i].set_up = true;
    }

    for (size_t j = 0; j < sizeof(signals_other)/sizeof(signals_other[0]); j++)
    {
        old_handlers[j] = signal(signals_other[j].signal, signals_other[j].handler);
        if (old_handlers[j] == SIG_ERR)
        {
            restore_signals();
            return -1;
        }
        signals_other[j].set_up = true;
    }

    // Prepare gdb crash handler
    if (!init_gdb_crash_handler())
    {
        restore_signals();
        return -1;
    }

    return 0;

} //initialize_signals

int shutdown_signals()
{
    cleanup_gdb_crash_handler();
    restore_signals();
    return 0;
} //shutdown_signals

void sig_process_crash_flags_change(unsigned added, unsigned removed)
{
// Still empty on Linux
}
