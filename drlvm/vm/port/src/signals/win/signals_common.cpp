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


#include <crtdbg.h>
#include <process.h>
#include <signal.h>
#include "open/platform_types.h"
#include "open/hythread_ext.h"
#include "port_malloc.h"
#include "port_mutex.h"

#include "port_crash_handler.h"
#include "stack_dump.h"
#include "signals_internal.h"
#include "port_thread_internal.h"


#if INSTRUMENTATION_BYTE == INSTRUMENTATION_BYTE_INT3
#define JVMTI_EXCEPTION_STATUS STATUS_BREAKPOINT
#elif INSTRUMENTATION_BYTE == INSTRUMENTATION_BYTE_HLT || INSTRUMENTATION_BYTE == INSTRUMENTATION_BYTE_CLI
#define JVMTI_EXCEPTION_STATUS STATUS_PRIVILEGED_INSTRUCTION
#else
#error Unknown value of INSTRUMENTATION_BYTE
#endif

#define FLAG_CORE ((port_crash_handler_get_flags() & PORT_CRASH_DUMP_PROCESS_CORE) != 0)
#define FLAG_DBG  ((port_crash_handler_get_flags() & PORT_CRASH_CALL_DEBUGGER) != 0)

#define ALT_PAGES_COUNT 16


typedef void (PORT_CDECL *sigh_t)(int); // Signal handler type

static PVOID veh = NULL;
static sigh_t prev_sig = (sigh_t)SIG_ERR;
// Mutex to protect access to the global data
static osmutex_t g_mutex;
// The global data protected by the mutex
static int report_modes[4];
static _HFILE report_files[3];
//--------
static bool asserts_disabled = false;


static void show_debugger_dialog()
{
    int result = MessageBox(NULL,
                    "Crash handler has been requested to call the debugger\n\n"
                    "Press Retry to attach to the debugger\n"
                    "Press Cancel to terminate the application",
                    "Crash Handler",
                    MB_RETRYCANCEL | MB_ICONHAND | MB_SETFOREGROUND | MB_TASKMODAL);

    if (result == IDCANCEL)
    {
        _exit(3);
        return;
    }

    port_win_dbg_break(); // Call the debugger
}


static void c_handler(Registers* pregs,
                        void* fault_addr, size_t code, size_t flags)
{ // this exception handler is executed *after* VEH handler returned
    int result;
    Boolean iscrash = (DWORD)flags == EXCEPTION_NONCONTINUABLE;

    switch ((DWORD)code)
    {
    case STATUS_STACK_OVERFLOW:
        result = port_process_signal(PORT_SIGNAL_STACK_OVERFLOW, pregs, fault_addr, iscrash);
        break;
    case STATUS_ACCESS_VIOLATION:
        result = port_process_signal(PORT_SIGNAL_GPF, pregs, fault_addr, iscrash);
        break;
    case STATUS_INTEGER_DIVIDE_BY_ZERO:
    case EXCEPTION_FLT_DIVIDE_BY_ZERO:
    case EXCEPTION_FLT_OVERFLOW:
    case EXCEPTION_FLT_UNDERFLOW:
    case EXCEPTION_INT_OVERFLOW:
        result = port_process_signal(PORT_SIGNAL_ARITHMETIC, pregs, fault_addr, iscrash);
        break;
    case JVMTI_EXCEPTION_STATUS:
        result = port_process_signal(PORT_SIGNAL_BREAKPOINT, pregs, fault_addr, iscrash);
        break;
    default:
        result = port_process_signal(PORT_SIGNAL_UNKNOWN, pregs, fault_addr, TRUE);
    }

    port_tls_data_t* tlsdata = get_private_tls_data();

    if (result == 0)
    {
        // Restore guard page if needed
        if (tlsdata->restore_guard_page)
        {
            port_thread_restore_guard_page();
            tlsdata->restore_guard_page = FALSE;
        }

        if (port_thread_detach_temporary() == 0)
            STD_FREE(tlsdata);
        return;
    }

    if (result > 0 /*Assert dialog*/|| FLAG_CORE)
    {
        // Prepare second catch of this exception to produce minidump (because
        // we've lost LPEXCEPTION_POINTERS structure) and/or show assert dialog
        if (FLAG_CORE)
            tlsdata->produce_core = TRUE;
        if (result > 0)
            tlsdata->debugger = TRUE;
        // To catch STACK_OVERFLOW
        port_thread_restore_guard_page();

        return; // To produce exception again
    }

    _exit(-1); // We need neither dump nor assert dialog
}

void prepare_assert_dialog(Registers* regs)
{
    // To catch STACK_OVERFLOW
    port_thread_restore_guard_page();
    shutdown_signals();
}

static void* map_alt_stack(size_t size)
{
    LPVOID res = VirtualAlloc(0, (SIZE_T)size, MEM_RESERVE, PAGE_READWRITE);
    if (!res)
        return NULL;

    return VirtualAlloc(res, (SIZE_T)size, MEM_COMMIT, PAGE_READWRITE);
}

static void unmap_alt_stack(void* addr, size_t size)
{
    VirtualFree(addr, (SIZE_T)size, MEM_RELEASE);
}

LONG NTAPI vectored_exception_handler_internal(LPEXCEPTION_POINTERS nt_exception)
{
    DWORD code = nt_exception->ExceptionRecord->ExceptionCode;
    void* fault_addr = nt_exception->ExceptionRecord->ExceptionAddress;

    Registers regs;
    // Convert NT context to Registers
    port_thread_context_to_regs(&regs, nt_exception->ContextRecord);

    // Check if TLS structure is set - probably we should produce minidump
    port_tls_data_t* tlsdata = get_private_tls_data();

    if (!tlsdata) // Tread is not attached - attach thread temporarily
    {
        int res;
        tlsdata = (port_tls_data_t*)STD_MALLOC(sizeof(port_tls_data_t));

        if (tlsdata) // Try to attach the thread
            res = port_thread_attach_local(tlsdata, TRUE, TRUE, 0);

        if (!tlsdata || res != 0)
        { // Can't process correctly; perform default actions
            if (FLAG_CORE)
                create_minidump(nt_exception);

            if (FLAG_DBG)
            {
                show_debugger_dialog(); // Workaround; EXCEPTION_CONTINUE_SEARCH does not work
                _exit(-1);
                return EXCEPTION_CONTINUE_SEARCH; // Assert dialog
            }

            _exit(-1);
        }

        // SO for alien thread can't be processed out of VEH
        if (code == STATUS_STACK_OVERFLOW &&
            sd_is_handler_registered(PORT_SIGNAL_STACK_OVERFLOW))
        {
            int result;
            size_t alt_stack_size = ALT_PAGES_COUNT*tlsdata->guard_page_size;
            void* alt_stack = map_alt_stack(alt_stack_size);
            void* stack_bottom = (void*)((POINTER_SIZE_INT)alt_stack + alt_stack_size);

            if (alt_stack)
                result = (int)(POINTER_SIZE_INT)port_call_alt_stack(
                                            port_process_signal, stack_bottom, 4,
                                            PORT_SIGNAL_STACK_OVERFLOW, &regs, fault_addr, FALSE);
            else
                result = port_process_signal(PORT_SIGNAL_STACK_OVERFLOW, &regs, fault_addr, FALSE);

            if (result == 0)
            {
                if (port_thread_detach_temporary() == 0)
                    STD_FREE(tlsdata);
                if (alt_stack)
                    unmap_alt_stack(alt_stack, alt_stack_size);
                return EXCEPTION_CONTINUE_EXECUTION;
            }

            if (FLAG_CORE)
            {
                if (alt_stack)
                    port_call_alt_stack(create_minidump, stack_bottom, 1, nt_exception);
                else
                    create_minidump(nt_exception);
            }

            if (alt_stack)
                unmap_alt_stack(alt_stack, alt_stack_size);

            if (result > 0)
            {
                show_debugger_dialog(); // Workaround; EXCEPTION_CONTINUE_SEARCH does not work
                _exit(-1);
                shutdown_signals();
                return EXCEPTION_CONTINUE_SEARCH; // Assert dialog
            }

            _exit(-1);
        }
    }

    if (tlsdata->produce_core)
    {
        create_minidump(nt_exception);
        if (!tlsdata->debugger)
            _exit(-1);
    }

    if (tlsdata->debugger)
    {
        show_debugger_dialog(); // Workaround
        _exit(-1);
        // Go to handler to restore CRT/VEH settings and crash once again
//        port_set_longjump_regs(&prepare_assert_dialog, &regs, 1, &regs);
//        port_thread_regs_to_context(nt_exception->ContextRecord, &regs);
//        return EXCEPTION_CONTINUE_EXECUTION;
    }

    switch (code)
    {
    case STATUS_STACK_OVERFLOW:
        if (!sd_is_handler_registered(PORT_SIGNAL_STACK_OVERFLOW))
            return EXCEPTION_CONTINUE_SEARCH;
        break;
    case STATUS_ACCESS_VIOLATION:
        if (!sd_is_handler_registered(PORT_SIGNAL_GPF))
            return EXCEPTION_CONTINUE_SEARCH;
        break;
    case JVMTI_EXCEPTION_STATUS:
        if (!sd_is_handler_registered(PORT_SIGNAL_BREAKPOINT))
            return EXCEPTION_CONTINUE_SEARCH;
        break;
    case STATUS_INTEGER_DIVIDE_BY_ZERO:
    case EXCEPTION_FLT_DIVIDE_BY_ZERO:
    case EXCEPTION_FLT_OVERFLOW:
    case EXCEPTION_FLT_UNDERFLOW:
    case EXCEPTION_INT_OVERFLOW:
        if (!sd_is_handler_registered(PORT_SIGNAL_ARITHMETIC))
            return EXCEPTION_CONTINUE_SEARCH;
        break;
    default:
        return EXCEPTION_CONTINUE_SEARCH;
    }

    if (code == STATUS_STACK_OVERFLOW)
    {
        tlsdata->guard_page_set = FALSE; // GUARD_PAGE was cleared by OS

        if (!tlsdata->restore_guard_page)
            tlsdata->restore_guard_page = TRUE;
    }

    // Prepare to transfering control out of VEH handler
    port_set_longjump_regs(&c_handler, &regs, 4, &regs,
        nt_exception->ExceptionRecord->ExceptionAddress,
        (void*)(size_t)nt_exception->ExceptionRecord->ExceptionCode,
        (void*)(size_t)nt_exception->ExceptionRecord->ExceptionFlags);
    // Convert prepared Registers back to NT context
    port_thread_regs_to_context(nt_exception->ContextRecord, &regs);
    // Return from VEH - presumably continue execution
    return EXCEPTION_CONTINUE_EXECUTION;
}

BOOL ctrl_handler(DWORD ctrlType)
{
    int result = 0;

    switch (ctrlType)
    {
    case CTRL_BREAK_EVENT:
        if (!sd_is_handler_registered(PORT_SIGNAL_CTRL_BREAK))
            return FALSE;

        result = port_process_signal(PORT_SIGNAL_CTRL_BREAK, NULL, NULL, FALSE);
        if (result == 0)
            return TRUE;
        else
            return FALSE;

    case CTRL_C_EVENT:
    case CTRL_CLOSE_EVENT:
    case CTRL_LOGOFF_EVENT:
    case CTRL_SHUTDOWN_EVENT:
        if (!sd_is_handler_registered(PORT_SIGNAL_CTRL_C))
            return FALSE;

        result = port_process_signal(PORT_SIGNAL_CTRL_C, NULL, NULL, FALSE);
        if (result == 0)
            return TRUE;
        else
            return FALSE;
    }

    return FALSE;
}


static void disable_assert_dialogs()
{
#ifdef _DEBUG
    report_modes[0] = _CrtSetReportMode(_CRT_ASSERT, _CRTDBG_MODE_FILE);
    report_files[0] = _CrtSetReportFile(_CRT_ASSERT, _CRTDBG_FILE_STDERR);
    report_modes[1] = _CrtSetReportMode(_CRT_ERROR,  _CRTDBG_MODE_FILE);
    report_files[1] = _CrtSetReportFile(_CRT_ERROR,  _CRTDBG_FILE_STDERR);
    report_modes[2] = _CrtSetReportMode(_CRT_WARN,   _CRTDBG_MODE_FILE);
    report_files[2] = _CrtSetReportFile(_CRT_WARN,   _CRTDBG_FILE_STDERR);
    report_modes[3] = _set_error_mode(_OUT_TO_STDERR);
#endif // _DEBUG
}

static void restore_assert_dialogs()
{
#ifdef _DEBUG
    _CrtSetReportMode(_CRT_ASSERT, report_modes[0]);
    _CrtSetReportFile(_CRT_ASSERT, report_files[0]);
    _CrtSetReportMode(_CRT_ERROR,  report_modes[1]);
    _CrtSetReportFile(_CRT_ERROR,  report_files[1]);
    _CrtSetReportMode(_CRT_WARN,   report_modes[2]);
    _CrtSetReportFile(_CRT_WARN,   report_files[2]);
    _set_error_mode(report_modes[3]);
#endif // _DEBUG
}

static void PORT_CDECL sigabrt_handler(int signum)
{
    int result = port_process_signal(PORT_SIGNAL_ABORT, NULL, NULL, FALSE);
    // There no reason for checking for 0 - abort() will do _exit(3) anyway
//    if (result == 0)
//        return;

    shutdown_signals(); // Remove handlers

    if (result > 0) // Assert dialog
        show_debugger_dialog();

    _exit(3);
}

static void PORT_CDECL final_sigabrt_handler(int signum)
{
    _exit(3);
}

void sig_process_crash_flags_change(unsigned added, unsigned removed)
{
    apr_status_t aprarr = port_mutex_lock(&g_mutex);
    if (aprarr != APR_SUCCESS)
        return;

    if ((added & PORT_CRASH_CALL_DEBUGGER) != 0 && asserts_disabled)
    {
        restore_assert_dialogs();
        asserts_disabled = false;
        signal(SIGABRT, (sigh_t)final_sigabrt_handler);
    }

    if ((removed & PORT_CRASH_CALL_DEBUGGER) != 0 && !asserts_disabled)
    {
        disable_assert_dialogs();
        asserts_disabled = true;
        signal(SIGABRT, (sigh_t)sigabrt_handler);
    }

    port_mutex_unlock(&g_mutex);
}

int initialize_signals()
{
    apr_status_t aprerr = port_mutex_create(&g_mutex, APR_THREAD_MUTEX_NESTED);

    if (aprerr != APR_SUCCESS)
        return -1;

    BOOL ok = SetConsoleCtrlHandler((PHANDLER_ROUTINE)ctrl_handler, TRUE);

    if (!ok)
        return -1;

    // Adding vectored exception handler
    veh = AddVectoredExceptionHandler(0, vectored_exception_handler);

    if (!veh)
        return -1;

    prev_sig = signal(SIGABRT, (sigh_t)sigabrt_handler);

    if (prev_sig == SIG_ERR)
        return -1;

    disable_assert_dialogs();
    asserts_disabled = true;

    return 0;
}

int shutdown_signals()
{
    if (asserts_disabled)
    {
        restore_assert_dialogs();
        asserts_disabled = false;
    }

    signal(SIGABRT, prev_sig);

    ULONG res = RemoveVectoredExceptionHandler(veh);

    if (!res)
        return -1;

    BOOL ok = SetConsoleCtrlHandler((PHANDLER_ROUTINE)ctrl_handler, FALSE);

    if (!ok)
        return -1;

    port_mutex_destroy(&g_mutex);
    return 0;
} //shutdown_signals
