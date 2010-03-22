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

#include <assert.h>
#include <stdlib.h>
#include "port_malloc.h"
#include "port_mutex.h"
#include "stack_dump.h"
#include "signals_internal.h"
#include "port_crash_handler.h"

port_signal_handler signal_callbacks[] =
{
    NULL, // PORT_SIGNAL_GPF
    NULL, // PORT_SIGNAL_STACK_OVERFLOW
    NULL, // PORT_SIGNAL_ABORT
    NULL, // PORT_SIGNAL_QUIT
    NULL, // PORT_SIGNAL_CTRL_BREAK
    NULL, // PORT_SIGNAL_CTRL_C
    NULL, // PORT_SIGNAL_BREAKPOINT
    NULL  // PORT_SIGNAL_ARITHMETIC
};

struct crash_additional_actions
{
    port_crash_handler_action action;
    crash_additional_actions *next;
};

static crash_additional_actions *crash_actions = NULL;

static unsigned crash_output_flags = 0;

static osmutex_t g_mutex;

static port_unwind_compiled_frame g_unwind_callback = NULL;


Boolean port_init_crash_handler(
    port_signal_handler_registration *registrations,
    unsigned count,
    port_unwind_compiled_frame unwind_callback)
{
    if (port_mutex_create(&g_mutex, APR_THREAD_MUTEX_NESTED) != APR_SUCCESS)
        return FALSE;

    for (unsigned iii = 0; iii < count; iii++)
    {
        assert(registrations[iii].signum >= PORT_SIGNAL_MIN);
        assert(registrations[iii].signum <= PORT_SIGNAL_MAX);
        signal_callbacks[registrations[iii].signum] = registrations[iii].callback;
    }

    // initialize_signals needs to know what handlers are registered
    if (initialize_signals() != 0)
        return FALSE;

    sd_init_crash_handler();

    g_unwind_callback = unwind_callback;

    // Set default flags
    port_crash_handler_set_flags(
        PORT_CRASH_DUMP_TO_STDERR |
        PORT_CRASH_STACK_DUMP |
        PORT_CRASH_PRINT_COMMAND_LINE |
        PORT_CRASH_PRINT_ENVIRONMENT |
        PORT_CRASH_PRINT_MODULES |
        PORT_CRASH_PRINT_REGISTERS);

    return TRUE;
}

unsigned port_crash_handler_get_capabilities()
{
    // Return the features we currently support
    return (port_crash_handler_flags)
           (PORT_CRASH_CALL_DEBUGGER |
            PORT_CRASH_DUMP_TO_STDERR |
            PORT_CRASH_STACK_DUMP |
            PORT_CRASH_DUMP_ALL_THREADS |
            PORT_CRASH_PRINT_COMMAND_LINE |
            PORT_CRASH_PRINT_ENVIRONMENT |
            PORT_CRASH_PRINT_MODULES |
            PORT_CRASH_PRINT_REGISTERS |
            PORT_CRASH_DUMP_PROCESS_CORE);
}

void port_crash_handler_set_flags(unsigned flags)
{
    unsigned new_flags = flags & port_crash_handler_get_capabilities();

    sig_process_crash_flags_change(new_flags & ~crash_output_flags,
                                   crash_output_flags & ~new_flags);

    crash_output_flags = new_flags;
}

unsigned port_crash_handler_get_flags()
{
    return crash_output_flags;
}

Boolean port_crash_handler_add_action(port_crash_handler_action action)
{
    crash_additional_actions *a =
        (crash_additional_actions*)STD_MALLOC(sizeof(crash_additional_actions));
    if (NULL == a)
        return FALSE;

    a->action = action;
    a->next = crash_actions;
    crash_actions = a;
    return TRUE;
}

Boolean port_shutdown_crash_handler()
{
    if (shutdown_signals() != 0)
        return FALSE;

    for (crash_additional_actions *a = crash_actions; NULL != a;)
    {
        crash_additional_actions *next = a->next;
        STD_FREE(a);
        a = next;
    }

    sd_cleanup_crash_handler();

    port_mutex_destroy(&g_mutex);

    return TRUE;
}

/* Returns 0  when execution should be continued with (updated) Registers
   Returns 1  when crash occured and process should invoke a debugger
   Returns -1 when crash occured and process should be terminated */
int port_process_signal(port_sigtype signum, Registers *regs, void* fault_addr, Boolean iscrash)
{
    if (!iscrash)
    {
        if (signum < PORT_SIGNAL_MIN ||
            signum > PORT_SIGNAL_MAX)
            return 0;

        if (signal_callbacks[signum] != NULL)
        {
            Boolean cres =
                signal_callbacks[signum](signum, regs, fault_addr);

            if (cres) // signal was processed
                return 0;
        }
    }

    // CRASH
    port_mutex_lock(&g_mutex);

    sd_print_crash_info(signum, regs, g_unwind_callback);

    for (crash_additional_actions* action = crash_actions;
         action; action = action->next)
    {
        action->action(signum, regs, fault_addr);
    }

    if ((crash_output_flags & PORT_CRASH_CALL_DEBUGGER) != 0)
    {
        port_mutex_unlock(&g_mutex);
        return 1;
    }

    return -1;
}

Boolean sd_is_handler_registered(port_sigtype signum)
{
    return signal_callbacks[signum] != NULL;
}
