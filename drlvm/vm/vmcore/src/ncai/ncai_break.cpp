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
 * @author Ilya Berezhniuk
 */

//#include <memory.h>
//#include <string.h>
//#include <stdio.h>

#define LOG_DOMAIN "ncai.break"
#include "cxxlog.h"
#include "suspend_checker.h"
#include "jvmti_break_intf.h"

#include "ncai_utils.h"
#include "ncai_direct.h"
#include "ncai_internal.h"


// Callback function for NCAI breakpoint processing
bool ncai_process_breakpoint_event(TIEnv *env, const VMBreakPoint* bp,
                                    const POINTER_SIZE_INT data)
{
    TRACE2("ncai.break", "BREAKPOINT occured, location = " << bp->addr);

    VM_thread* vm_thread = p_TLS_vmthread;
    if (!vm_thread)
        return false;

    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;

    // This check works for current thread only
    if (jvmti_thread->flag_ncai_handler) // Recursion
        return true;

    jvmti_thread->flag_ncai_handler = true;

    NCAIEnv* ncai_env = env->ncai_env;
    void* addr = (void*)bp->addr;

    bool suspend_enabled = hythread_is_suspend_enabled();

    if (!suspend_enabled)
        hythread_suspend_enable();

    hythread_t hythread = hythread_self();
    ncaiThread thread = reinterpret_cast<ncaiThread>(hythread);

    ncaiBreakpoint func =
        (ncaiBreakpoint)ncai_env->get_event_callback(NCAI_EVENT_BREAKPOINT);

    if (NULL != func)
    {
        if (ncai_env->global_events[NCAI_EVENT_BREAKPOINT - NCAI_MIN_EVENT_TYPE_VAL])
        {
            TRACE2("ncai.break", "Calling global breakpoint callback, address = " << addr);

            func((ncaiEnv*)ncai_env, thread, addr);

            TRACE2("ncai.break", "Finished global breakpoint callback, address = " << addr);
        }
        else
        {

            ncaiEventThread* next_et;
            ncaiEventThread* first_et =
                ncai_env->event_threads[NCAI_EVENT_BREAKPOINT - NCAI_MIN_EVENT_TYPE_VAL];

            for (ncaiEventThread* et = first_et; NULL != et; et = next_et)
            {
                next_et = et->next;

                if (et->thread == thread)
                {
                    TRACE2("ncai.break", "Calling local breakpoint callback, address = " << addr);

                    func((ncaiEnv*)ncai_env, thread, addr);

                    TRACE2("ncai.break", "Finished local breakpoint callback, address = " << addr);
                }

                et = next_et;
            }
        }
    }

    if (!suspend_enabled)
        hythread_suspend_disable();

    jvmti_thread->flag_ncai_handler = false;
    return true;
}


ncaiError JNICALL
ncaiSetBreakpoint(ncaiEnv *env, void* code_addr)
{
    TRACE2("ncai.break", "SetBreakpoint called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (code_addr == NULL)
        return NCAI_ERROR_INVALID_ADDRESS;

    NCAIEnv *p_env = (NCAIEnv*)env;
    VMBreakInterface* brpt_intf = p_env->brpt_intf;
    VMBreakPoints *vm_breaks = VM_Global_State::loader_env->TI->vm_brpt;
    LMAutoUnlock lock(vm_breaks->get_lock());

    VMBreakPointRef* bp_ref = brpt_intf->find_reference(code_addr);

    if (NULL != bp_ref)
        return NCAI_ERROR_DUPLICATE;

    if (!brpt_intf->add_reference(code_addr, 0))
        return NCAI_ERROR_INTERNAL;

    TRACE2("ncai.break", "SetBreakpoint is successfull");
    return NCAI_ERROR_NONE;
}


ncaiError JNICALL
ncaiClearBreakpoint(ncaiEnv *env, void* code_addr)
{
    TRACE2("ncai.break", "ClearBreakpoint called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (code_addr == NULL)
        return NCAI_ERROR_INVALID_ADDRESS;

    NCAIEnv *p_env = (NCAIEnv*)env;
    VMBreakInterface* brpt_intf = p_env->brpt_intf;
    VMBreakPoints *vm_breaks = VM_Global_State::loader_env->TI->vm_brpt;
    LMAutoUnlock lock(vm_breaks->get_lock());

    VMBreakPointRef* bp_ref = brpt_intf->find_reference(code_addr);

    if (NULL == bp_ref)
        return NCAI_ERROR_NOT_FOUND;

    if (!brpt_intf->remove_reference(bp_ref))
        return NCAI_ERROR_INTERNAL;

    TRACE2("ncai.break", "ClearBreakpoint is successfull");
    return NCAI_ERROR_NONE;
}
