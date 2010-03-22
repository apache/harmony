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

/* Out-of-signal-handler signal processing is not implemented for IPF

static void c_handler(Registers* pregs, size_t signum, void* fault_addr)
{ // this exception handler is executed *after* VEH handler returned
}
*/

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

static void general_signal_handler(int signum, siginfo_t* info, void* context)
{
    Registers regs;
    port_tls_data_t* tlsdata = get_private_tls_data();

    if (!context)
        return;

/* Not implemented
    // Check if SIGSEGV is produced by port_read/write_memory
    if (tlsdata && tlsdata->violation_flag)
    {
        tlsdata->violation_flag = 0;
        pregs->set_ip(tlsdata->restart_address);
        return;
    }
*/
    // Convert OS context to Registers
    port_thread_context_to_regs(&regs, (ucontext_t*)context);

    void* fault_addr = info ? info->si_addr : NULL;

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

    int result;

    switch ((int)signum)
    {
    case SIGSEGV:
        result = port_process_signal(PORT_SIGNAL_GPF, &regs, fault_addr, FALSE);
        break;
    case SIGFPE:
        result = port_process_signal(PORT_SIGNAL_ARITHMETIC, &regs, fault_addr, FALSE);
        break;
    case SIGTRAP:
        // Correct return address - FIXME ??? probably not needed on IPF
        regs.set_ip((void*)((POINTER_SIZE_INT)regs.get_ip() - 1));
        result = port_process_signal(PORT_SIGNAL_BREAKPOINT, &regs, fault_addr, FALSE);
        break;
    case SIGINT:
        result = port_process_signal(PORT_SIGNAL_CTRL_C, &regs, fault_addr, FALSE);
        break;
    case SIGQUIT:
        result = port_process_signal(PORT_SIGNAL_QUIT, &regs, fault_addr, FALSE);
        break;
    case SIGABRT:
        result = port_process_signal(PORT_SIGNAL_ABORT, NULL, fault_addr, FALSE);
        break;
    default:
        result = port_process_signal(PORT_SIGNAL_UNKNOWN, &regs, fault_addr, TRUE);
    }

    // Convert Registers back to OS context
    port_thread_regs_to_context((ucontext_t*)context, &regs);

    if (result == 0) // Signal was processed - continue execution
    {
        if (port_thread_detach_temporary() == 0)
            STD_FREE(tlsdata);
        return;
    }

    // We've got a crash
    if (result > 0)
    { // result > 0 - invoke debugger
        bool result = gdb_crash_handler(&regs);
        // Continue with making core or exiting process if not successful...
    }

    // result < 0 - exit process
    if (FLAG_CORE &&
        signum != SIGABRT) // SIGABRT can't be rethrown
    { // Return to the same place to produce the same crash and generate core
        signal(signum, SIG_DFL); // setup default handler
        return;
    }
    // No core needed - simply terminate
    _exit(-1);
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


static void restore_signals()
{
    for (size_t i = 0; i < sizeof(signals_used)/sizeof(signals_used[0]); i++)
    {
        if (!signals_used[i].set_up)
            continue;

        signals_used[i].set_up = false;
        sigaction(signals_used[i].signal, &old_actions[i], NULL);
    }
}

int initialize_signals()
{
    struct sigaction sa;

    for (size_t i = 0; i < sizeof(signals_used)/sizeof(signals_used[0]); i++)
    {
        if (!sd_is_handler_registered(signals_used[i].port_sig))
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

    // Prepare gdb crash handler
    if (!init_gdb_crash_handler())
    {
        restore_signals();
        return -1;
    }

    return 0;

} //initialize_signals

int shutdown_signals() {
    cleanup_gdb_crash_handler();
    restore_signals();
    return 0;
} //shutdown_signals

void sig_process_crash_flags_change(unsigned added, unsigned removed)
{
// Still empty on Linux
}

#if 0

// Variables used to locate the context from the signal handler
static int sc_nest = -1;
static bool use_ucontext = false;
static U_32 exam_point;

/*
 * We find the true signal stack frame set-up by kernel,which is located
 * by locate_sigcontext() below; then change its content according to
 * exception handling semantics, so that when the signal handler is
 * returned, application can continue its execution in Java exception handler.
 */

/*
See function initialize_signals() below first please.

Two kinds of signal contexts (and stack frames) are tried in
locate_sigcontext(), one is sigcontext, which is the way of
Linux kernel implements( see Linux kernel source
arch/i386/kernel/signal.c for detail); the other is ucontext,
used in some other other platforms.

The sigcontext locating in Linux is not so simple as expected,
because it involves not only the kernel, but also glibc/linuxthreads,
which VM is linked against. In glibc/linuxthreads, user-provided
signal handler is wrapped by linuxthreads signal handler, i.e.
the signal handler really registered in system is not the one
provided by user. So when Linux kernel finishes setting up signal
stack frame and returns to user mode for singal handler execution,
locate_sigcontext() is not the one being invoked immediately. It's
called by linuxthreads function. That means the user stack viewed by
locate_sigcontext() is NOT NECESSARILY the signal frame set-up by
kernel, we need find the true one according to glibc/linuxthreads
specific signal implementation in different versions.

Because locate_sigcontext() uses IA32 physical register epb for
call stack frame, compilation option `-fomit-frame-pointer' MUST
not be used when gcc compiles it; and as gcc info, `-O2' will do
`-fomit-frame-pointer' by default, although we haven't seen that
in our experiments.
*/

volatile void locate_sigcontext(int signum)
{
    sigcontext *sc, *found_sc;
    U_32 *ebp = NULL;
    int i;

//TODO: ADD correct stack handling here!!

#define SC_SEARCH_WIDTH 3
    for (i = 0; i < SC_SEARCH_WIDTH; i++) {
        sc = (sigcontext *)(ebp + 3 );
        if (sc->sc_ip == ((U_32)exam_point)) {    // found
            sc_nest = i;
            use_ucontext = false;
            found_sc = sc;
        // we will try to find the real sigcontext setup by Linux kernel,
        // because if we want to change the execution path after the signal
        // handling, we must modify the sigcontext used by kernel.
        // LinuxThreads in glibc 2.1.2 setups a sigcontext for our singal
        // handler, but we should not modify it, because kernel doesn't use
        // it when resumes the application. Then we must find the sigcontext
        // setup by kernel, and modify it in singal handler.
        // but, with glibc 2.2.3, it's useless to modify only the sigcontext
        // setup by Linux kernel, because LinuxThreads does a interesting
        // copy after our signal handler returns, which destroys the
        // modification we have just done in the handler. So with glibc 2.2.3,
        // what we need do is simply to modify the sigcontext setup by
        // LinuxThreads, which will be copied to overwrite the one setup by
        // kernel. Really complicated..., not really. We use a simple trick
        // to overcome the changes in glibc from version to version, that is,
        // we modify both sigcontexts setup by kernel and LinuxThreads. Then
        // it will always work.

        } else {                    // not found
            struct ucontext *uc;
            uc = (struct ucontext *)((uint64)ebp[4]);
            if ((ebp < (U_32 *)uc) && ((U_32 *)uc < ebp + 0x100)) {
                sc = (sigcontext *)&uc->uc_mcontext;
                if (sc->sc_ip == ((U_32)exam_point)) {    // found
                    sc_nest = i;
                    use_ucontext = true;
                    found_sc = sc;
                    break;
                }
            }
        }

        ebp = (U_32 *)((uint64)ebp[0]);
    }

    if (sc_nest < 0) {
        printf("cannot locate sigcontext.\n");
        printf("Please add or remove any irrelevant statement(e.g. add a null printf) in VM source code, then rebuild it. If problem remains, please submit a bug report. Thank you very much\n");
        exit(1);
    }
}



#endif
