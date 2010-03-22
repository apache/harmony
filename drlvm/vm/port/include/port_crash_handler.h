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

#ifndef _PORT_CRASH_HANDLER_
#define _PORT_CRASH_HANDLER_


/**
 * @file Interface to signal handling and crash handling functions.
 */

#include "open/platform_types.h"
#include "port_frame_info.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Signals types handled by the crash handler.
 */
typedef enum
{
    /**
     * General protection fault, e.g. SIGSEGV on unix.
     */
    PORT_SIGNAL_GPF = 0,
    /**
     * Special type of GPF when crash happens because of stack
     * overflow. It can be handled on the same stack because guard
     * page for the handler code is temporarily disabled.
     */
    PORT_SIGNAL_STACK_OVERFLOW,
    /**
     * Process used function abort().
     */
    PORT_SIGNAL_ABORT,
    /**
     * Ctrl-\ combination was pressed.
     */
    PORT_SIGNAL_QUIT,
    /**
     * Ctrl-Break combination was pressed.
     */
    PORT_SIGNAL_CTRL_BREAK,
    /**
     * Ctrl-C combination was pressed.
     */
    PORT_SIGNAL_CTRL_C,
    /**
     * Process met a breakpoint set with port_set_breakpoint().
     */
    PORT_SIGNAL_BREAKPOINT,
    /**
     * Process encountered an arithmeric error, e.g. division by
     * zero.
     */
    PORT_SIGNAL_ARITHMETIC,
    /**
     * Unknown signal - does not have a handler.
     */
    PORT_SIGNAL_UNKNOWN,
    /**
     * Maximum identifier for signal type
     */
    PORT_SIGNAL_MIN = PORT_SIGNAL_GPF,
    /**
     * Maximum identifier for signal type
     */
    PORT_SIGNAL_MAX = PORT_SIGNAL_ARITHMETIC
} port_sigtype;

/**
 * Function callback type for process signal handlers. These
 * callbacks are called by signal handler when some signal occures in
 * the process thread.
 *
 * Callback functions have to be thread safe.
 *
 * @param signum - signal that in the thread.
 * @param regs - platform dependent register context of the process at
 * signal point.
 * @param fault_addr - fault address.
 * @return <code>TRUE</code> if signal is handled by the process and
 * execution should continue. <code>FALSE</code> if signal is not handled
 * by the process and crash handler should execute crash sequence.
 */
typedef Boolean (*port_signal_handler)(
    port_sigtype signum,
    Registers *regs,
    void* fault_addr);

/**
 * Signal callback registration information.
 */
typedef struct
{
    /**
     * Signal type for callback registration.
     */
    port_sigtype signum;
    /**
     * Callback function to call when <code>signum</code> signal
     * occurres.
     */
    port_signal_handler callback;
} port_signal_handler_registration;

/**
 * Initialize signals and crash handler and register process signal
 * callbacks.
 *
 * This function should not be called from different threads at the
 * same time because it is not thread safe.
 *
 * @param registrations - Pointer to an array of
 * <code>port_signal_handler_registration structures.
 * @param count - number of signal registrations. Zero is allowed
 * @param unwind_callback - VM function for unwinding compiled
 * stack frames.
 * @return <code>TRUE</code> if initalization is successful.
 * <code>FALSE</code> if initialization failed.
 */
VMEXPORT Boolean port_init_crash_handler(
    port_signal_handler_registration *registrations,
    unsigned count,
    port_unwind_compiled_frame unwind_callback);

/**
 * Crash handler output flags.
 */
typedef enum
{
    /**
     * Crash handler should call debugger instead of exiting the
     * process.
     */
    PORT_CRASH_CALL_DEBUGGER = 0x0001,
    /**
     * Crash handler should output all information to stderr.
     */
    PORT_CRASH_DUMP_TO_STDERR = 0x0002,
    /**
     * Crash handler should output all information to a file.
     */
    PORT_CRASH_DUMP_TO_FILE = 0x0004,
    /**
     * Crash handler should show stack dump when crash happens.
     */
    PORT_CRASH_STACK_DUMP = 0x0008,
    /**
     * Crash handler should dump stacks for all threads, not just the
     * one that crashed.
     */
    PORT_CRASH_DUMP_ALL_THREADS = 0x0010,
    /**
     * Crash handler should print process command line and working
     * directory.
     */
    PORT_CRASH_PRINT_COMMAND_LINE = 0x0020,
    /**
     * Crash handler should print process environment.
     */
    PORT_CRASH_PRINT_ENVIRONMENT = 0x0040,
    /**
     * Crash handler should print modules list.
     */
    PORT_CRASH_PRINT_MODULES = 0x0080,
    /**
     * Crash handler should print thread registers context.
     */
    PORT_CRASH_PRINT_REGISTERS = 0x0100,
    /**
     * Crash handler should print threads list.
     */
    PORT_CRASH_PRINT_THREADS_LIST = 0x0200,
    /**
     * Crash handler should create a core/mini dump of program state.
     */
    PORT_CRASH_DUMP_PROCESS_CORE = 0x0400
} port_crash_handler_flags;

/**
 * Returns supported features from the list of crash handler flags.
 * When unsupported feature is requested, port_crash_handler_set_flags
 * does not fail.
 *
 * @returns  supported crash handler features.
 */
VMEXPORT unsigned port_crash_handler_get_capabilities();

/**
 * Set crash handler output flags. Default mode is <code>
 * PORT_CRASH_DUMP_TO_STDERR |
 * PORT_CRASH_STACK_DUMP | PORT_CRASH_PRINT_COMMAND_LINE |
 * PORT_CRASH_PRINT_ENVIRONMENT | PORT_CRASH_PRINT_MODULES |
 * PORT_CRASH_PRINT_REGISTERS</code>.
 *
 * @param flags - crash handler output flags.
 */
VMEXPORT void port_crash_handler_set_flags(unsigned flags);

/**
 * Callback function that is called at the end of shutdown sequence.
 *
 * Crash action functions don't have to be thread safe because crash
 * sequence is guaranteed to be executed on one thread at a time.
 *
 * @param signum - signal that in the thread.
 * @param regs - platform dependent register context of the process at
 * signal point.
 * @param fault_addr - fault address.
 */
typedef void (*port_crash_handler_action)(
    port_sigtype signum,
    Registers *regs,
    void* fault_addr);

/**
 * Add an action to be done at the end of crash sequence. Actions may
 * dump additional information about the state of process specific
 * data structures.
 *
 * This function should not be called from different threads at the
 * same time because it is not thread safe.
 *
 * @param action - action callback pointer
 * @return <code>TRUE</code> if action was added successfully,
 * <code>FALSE</code> if action was not added because no memory could
 * be allocated.
 */
VMEXPORT Boolean port_crash_handler_add_action(port_crash_handler_action action);

/**
 * Shutdown signals and crash handler. All signals are assigned their
 * default handlers.
 *
 * This function should not be called from different threads at the
 * same time because it is not thread safe.
 *
 * @return <code>TRUE</code> if shutdown is
 * successful. <code>FALSE</code> if shutdown failed.
 */
VMEXPORT Boolean port_shutdown_crash_handler();


/**
* Instruments specified location to produce PORT_SIGNAL_BREAKPOINT event.
* @param [in] addr  - memory location to instrument.
* @param [out] prev - address to store previous value of the instrumented byte.
* @return <code>0</code> if OK; nonzero if the location is already
* instrumented or if an error occured.
* @note Caller should keep store previous byte to restore
* the location in future.
*/
VMEXPORT int port_set_breakpoint(void* addr, unsigned char* prev);

/**
* Restores original byte in the location previously instrumented
* with port_set_breakpoint.
* @param [in] addr  - memory location to deinstrument.
* @param [in] prev  - previous byte to restore.
* @return <code>0</code> if OK; nonzero if the location was not
* instrumented yet or if an error occured.
*/
VMEXPORT int port_clear_breakpoint(void* addr, unsigned char prev);

/**
* Checks if the location is instrumented.
* @param [in] addr  - memory location to deinstrument.
* @return <code>TRUE</code> if instrumented; FALSE otherwise.
*/
VMEXPORT Boolean port_is_breakpoint_set(void* addr);



#ifdef __cplusplus
}
#endif

#endif /* _PORT_CRASH_HANDLER_ */
