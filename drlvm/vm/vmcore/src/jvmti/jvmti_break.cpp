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
/*
 * JVMTI API for working with breakpoints
 */
#define LOG_DOMAIN "jvmti.break"
#include "cxxlog.h"

#include "open/hythread_ext.h"
#include "open/vm_method_access.h"

#include "jvmti_direct.h"
#include "jvmti_utils.h"
#include "jvmti_internal.h"
#include "environment.h"
#include "Class.h"
#include "vm_log.h"
#include "cci.h"

#include "suspend_checker.h"
#include "interpreter_exports.h"
#include "jvmti_break_intf.h"

#include "jthread.h"


// Callback function for JVMTI breakpoint processing
bool jvmti_process_breakpoint_event(TIEnv *env, const VMBreakPoint* bp, const POINTER_SIZE_INT UNREF data)
{
    assert(bp);

    TRACE("Process breakpoint: "
        << bp->method << " :" << bp->location << " :" << bp->addr );

    DebugUtilsTI *ti = VM_Global_State::loader_env->TI;
    if (ti->getPhase() != JVMTI_PHASE_LIVE)
        return false;

    jlocation location = bp->location;
    jmethodID method = bp->method;
    NativeCodePtr addr = bp->addr;
    
    hythread_t h_thread = hythread_self();
    jthread j_thread = jthread_get_java_thread(h_thread);
    ObjectHandle hThread = oh_allocate_local_handle();
    hThread->object = (Java_java_lang_Thread *)j_thread->object;
    tmn_suspend_enable();

    JNIEnv *jni_env = p_TLS_vmthread->jni_env;

    jvmtiEventBreakpoint func =
        (jvmtiEventBreakpoint)env->get_event_callback(JVMTI_EVENT_BREAKPOINT);

    if (NULL != func)
    {
        if (env->global_events[JVMTI_EVENT_BREAKPOINT - JVMTI_MIN_EVENT_TYPE_VAL])
        {
            TRACE("Calling global breakpoint callback: "
                << method << " :" << location << " :" << addr);

            func((jvmtiEnv*)env, jni_env, (jthread)hThread, method, location);

            TRACE("Finished global breakpoint callback: "
                << method << " :" << location << " :" << addr);
        }
        else
        {
            TIEventThread *next_et;
            TIEventThread *first_et =
                env->event_threads[JVMTI_EVENT_BREAKPOINT - JVMTI_MIN_EVENT_TYPE_VAL];

            for (TIEventThread *et = first_et; NULL != et; et = next_et)
            {
                next_et = et->next;

                if (et->thread == hythread_self())
                {
                    TRACE("Calling local breakpoint callback: "
                        << method << " :" << location << " :" << addr);

                    func((jvmtiEnv*)env, jni_env, (jthread)hThread, method, location);

                    TRACE("Finished local breakpoint callback: "
                        << method << " :" << location << " :" << addr);
                }
            }
        }
    }

    oh_discard_local_handle(hThread);
    hythread_exception_safe_point();
    tmn_suspend_disable();

    return true;
}

/*
* Set Breakpoint
*
* Set a breakpoint at the instruction indicated by method and
* location. An instruction can only have one breakpoint.
*
* OPTIONAL Functionality
*/
jvmtiError JNICALL
jvmtiSetBreakpoint(jvmtiEnv* env,
                   jmethodID method,
                   jlocation location)
{
    TRACE("SetBreakpoint is called for method: " << method
        << " :" << location);
    SuspendEnabledChecker sec;

    jvmtiError errorCode;

    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (method == NULL)
        return JVMTI_ERROR_INVALID_METHODID;

    Method *m = (Method*) method;
    TRACE("SetBreakpoint: " << method << " :" << location);

#if defined (__INTEL_COMPILER) 
#pragma warning( push )
#pragma warning (disable:1683) // to get rid of remark #1683: explicit conversion of a 64-bit integral type to a smaller integral type
#endif

    if (location < 0 || unsigned(location) >= m->get_byte_code_size())
        return JVMTI_ERROR_INVALID_LOCATION;

#if defined (__INTEL_COMPILER)
#pragma warning( pop )
#endif
    /*
    * JVMTI_ERROR_MUST_POSSESS_CAPABILITY
    */
    jvmtiCapabilities capptr;
    errorCode = jvmtiGetCapabilities(env, &capptr);

    if (errorCode != JVMTI_ERROR_NONE)
        return errorCode;

    if (capptr.can_generate_breakpoint_events == 0)
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;

    TIEnv *p_env = (TIEnv *)env;
    VMBreakInterface* brpt_intf = p_env->brpt_intf;
    VMBreakPoints *vm_breaks = VM_Global_State::loader_env->TI->vm_brpt;
    LMAutoUnlock lock(vm_breaks->get_lock());

    VMBreakPointRef* bp = brpt_intf->find_reference(method, location);

    if (NULL != bp)
        return JVMTI_ERROR_DUPLICATE;

    if (!brpt_intf->add_reference(method, location, (POINTER_SIZE_INT)false))
        return JVMTI_ERROR_INTERNAL;

    TRACE("SetBreakpoint is successful");
    return JVMTI_ERROR_NONE;
}

/*
* Clear Breakpoint
*
* Clear the breakpoint at the bytecode indicated by method and
* location.
*
* OPTIONAL Functionality
*/
jvmtiError JNICALL
jvmtiClearBreakpoint(jvmtiEnv* env,
                     jmethodID method,
                     jlocation location)
{
    TRACE("ClearBreakpoint is called for method: " << method
        << " :" << location);
    SuspendEnabledChecker sec;
    jvmtiError errorCode;

    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (method == NULL)
        return JVMTI_ERROR_INVALID_METHODID;

    Method *m = (Method*) method;
    TRACE("ClearBreakpoint: " << method << " :" << location);

#if defined (__INTEL_COMPILER) 
#pragma warning( push )
#pragma warning (disable:1683) // to get rid of remark #1683: explicit conversion of a 64-bit integral type to a smaller integral type
#endif

    if (location < 0 || unsigned(location) >= m->get_byte_code_size())
        return JVMTI_ERROR_INVALID_LOCATION;

#if defined (__INTEL_COMPILER)
#pragma warning( pop )
#endif

    /*
    * JVMTI_ERROR_MUST_POSSESS_CAPABILITY
    */
    jvmtiCapabilities capptr;
    errorCode = jvmtiGetCapabilities(env, &capptr);

    if (errorCode != JVMTI_ERROR_NONE)
        return errorCode;

    if (capptr.can_generate_breakpoint_events == 0)
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;

    TIEnv *p_env = (TIEnv *)env;
    VMBreakInterface* brpt_intf = p_env->brpt_intf;
    VMBreakPoints *vm_breaks = VM_Global_State::loader_env->TI->vm_brpt;
    LMAutoUnlock lock(vm_breaks->get_lock());

    VMBreakPointRef* bp_ref = brpt_intf->find_reference(method, location);

    if (NULL == bp_ref)
        return JVMTI_ERROR_NOT_FOUND;

    if (!brpt_intf->remove_reference(bp_ref))
        return JVMTI_ERROR_INTERNAL;

    TRACE("ClearBreakpoint is successful");
    return JVMTI_ERROR_NONE;
}
