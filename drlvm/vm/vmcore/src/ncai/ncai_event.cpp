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
 * @author Intel, Petr Ivanov
 */

/**
 * NCAI event management
 *
 */

#define LOG_DOMAIN "ncai.events"
#include "cxxlog.h"

#include "suspend_checker.h"
#include "thread_generic.h"
#include "environment.h"
#include "jvmti_internal.h"
#include "jvmti_break_intf.h"
#include "open/ncai_thread.h"
#include "ncai_direct.h"
#include "ncai_internal.h"
#include "ncai_utils.h"


static ncaiError ncai_add_event_to_thread(ncaiEnv *, ncaiEventKind, ncaiThread);
static void ncai_remove_event_from_thread(ncaiEnv *, ncaiEventKind, ncaiThread);
static void ncai_add_event_to_global(ncaiEnv *, ncaiEventKind);
static void ncai_remove_event_from_global(ncaiEnv *, ncaiEventKind);
static ncaiError ncai_check_start_single_step(NCAIEnv* env);
static ncaiError ncai_check_stop_single_step(NCAIEnv* env);

/*
 * Set Event Callbacks
 *
 * Set the functions to be called for each event.
 *
 */

ncaiError JNICALL
ncaiSetEventCallbacks(ncaiEnv* env,
                       ncaiEventCallbacks* callbacks,
                       size_t size)
{
    TRACE2("ncai.events", "SetEventCallbacks called");
    SuspendEnabledChecker sec;

    if (size <= 0)
        return NCAI_ERROR_ILLEGAL_ARGUMENT;

    if (callbacks != NULL)
        memcpy(&(((NCAIEnv*)env)->event_table), callbacks, sizeof(ncaiEventCallbacks));
    else
        memset(&(((NCAIEnv*)env)->event_table), 0, sizeof(ncaiEventCallbacks));

    return NCAI_ERROR_NONE;
}

/*
 * Set Event Callbacks
 *
 * Get the list od functions to be called for each event.
 *
 */
ncaiError JNICALL
ncaiGetEventCallbacks(ncaiEnv* env,
                       ncaiEventCallbacks* callbacks,
                       size_t size)
{
    TRACE2("ncai.events", "GetEventCallbacks called");
    SuspendEnabledChecker sec;

    if (size <= 0)
        return NCAI_ERROR_ILLEGAL_ARGUMENT;

    if (callbacks == NULL)
        return NCAI_ERROR_NULL_POINTER;

    memcpy(callbacks, &(((NCAIEnv*)env)->event_table), sizeof(ncaiEventCallbacks));

    return NCAI_ERROR_NONE;
}

/*
 * Set Event Notification Mode
 *
 * Control the generation of events.
 */
ncaiError JNICALL
ncaiSetEventNotificationMode(ncaiEnv* env,
                              ncaiEventMode mode,
                              ncaiEventKind event_type,
                              ncaiThread event_thread)
{
    TRACE2("ncai.event", "SetEventNotificationMode called," <<
        "event type = " << event_type << " mode = " << mode);
    SuspendEnabledChecker sec;

    if (mode == NCAI_ENABLE)
    {
        if (event_thread == NULL)
            ncai_add_event_to_global(env, event_type);
        else
        {
            ncaiError result = ncai_add_event_to_thread(env, event_type, event_thread);
            if (result != NCAI_ERROR_NONE)
                return result;
        }

        if (event_type == NCAI_EVENT_STEP)
        {
            ncaiError result = ncai_check_start_single_step((NCAIEnv*)env);

            if (result != NCAI_ERROR_NONE)
                return result;
        }
    }
    else if (mode == NCAI_DISABLE)
    {
        if (event_thread == NULL)
            ncai_remove_event_from_global(env, event_type);
        else
            ncai_remove_event_from_thread(env, event_type, event_thread);

        if (event_type == NCAI_EVENT_STEP)
        {
            ncaiError result = ncai_check_stop_single_step((NCAIEnv*)env);

            if (result != NCAI_ERROR_NONE)
                return result;
        }
    }
    else
    {
        return NCAI_ERROR_ILLEGAL_ARGUMENT;
    }

    return NCAI_ERROR_NONE;
}

/*
 * Set Step Mode for given thread
 * If thread is NULL, set mode for all known threads
 */
ncaiError JNICALL
ncaiSetStepMode(ncaiEnv* env,
                ncaiThread thread,
                ncaiStepMode mode)
{
    TRACE2("ncai.event", "SetStepMode called," <<
        "thread = " << thread << " mode = " << mode);
    SuspendEnabledChecker sec;

    if (mode < NCAI_STEP_OFF || mode > NCAI_STEP_OUT)
        return NCAI_ERROR_ILLEGAL_ARGUMENT;

    GlobalNCAI* ncai = VM_Global_State::loader_env->NCAI;
    hythread_t hythread = reinterpret_cast<hythread_t>(thread);
    VMBreakPoints* vm_brpt = ((NCAIEnv*)env)->ti_env->vm->vm_env->TI->vm_brpt;
    vm_thread_t vm_thread = jthread_get_vm_thread(hythread);

    assert(vm_thread);
    if (vm_thread == NULL)
        return NCAI_ERROR_INTERNAL;

    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;

    LMAutoUnlock lock(vm_brpt->get_lock());

    if (thread)
    {
        ncai_check_alloc_ss_data(jvmti_thread);
        jvmti_thread->ncai_ss->use_local_mode = true;
        jvmti_thread->ncai_ss->step_mode = mode;
        jvmti_thread->ncai_ss->flag_out = false;
        return NCAI_ERROR_NONE;
    }

    ncaiThread* threads;
    jint thread_count;

    ncaiError err = ncaiGetAllThreads((ncaiEnv*)env, &thread_count, &threads);

    if (err != NCAI_ERROR_NONE)
        return err;

    assert(thread_count > 0);

    for (jint i = 0; i < thread_count; i++)
    {
        hythread = (hythread_t)threads[i];
        vm_thread = jthread_get_vm_thread(hythread);

        if (!vm_thread)
            continue;

        ncai_check_alloc_ss_data(&vm_thread->jvmti_thread);
        vm_thread->jvmti_thread.ncai_ss->use_local_mode = false;
        vm_thread->jvmti_thread.ncai_ss->flag_out = false;
    }

    ncai->step_mode = mode;

    return NCAI_ERROR_NONE;
}


//////////////////////
// Helper Functions
//////////////////////

// Must be called under breakpoint lock
ncaiStepMode ncai_get_thread_ss_mode(jvmti_thread_t jvmti_thread)
{
    assert(jvmti_thread->ncai_ss);

    if (jvmti_thread->ncai_ss->use_local_mode)
        return jvmti_thread->ncai_ss->step_mode;

    return VM_Global_State::loader_env->NCAI->step_mode;
}

ncaiError ncai_add_event_to_thread(ncaiEnv *env, ncaiEventKind event_type,
                                   ncaiThread event_thread)
{
    NCAIEnv *p_env = (NCAIEnv *)env;
    ncaiEventThread *et =p_env->event_threads[event_type - NCAI_MIN_EVENT_TYPE_VAL];

    // Find out if this environment is already registered
    // on this thread on this event type
    while (et != NULL)
    {
        if (et->thread == event_thread)
            return NCAI_ERROR_NONE;

        et = et->next;
    }

    ncaiEventThread *newet;
    newet = (ncaiEventThread *) ncai_alloc(sizeof(ncaiEventThread));

    if (newet == NULL)
        return NCAI_ERROR_OUT_OF_MEMORY;

    newet->thread = event_thread;

    LMAutoUnlock aulock(p_env->env_lock); //ncai env general lock

    newet->next = p_env->event_threads[event_type - NCAI_MIN_EVENT_TYPE_VAL];
    p_env->event_threads[event_type - NCAI_MIN_EVENT_TYPE_VAL] = newet;
    return NCAI_ERROR_NONE;
}

void ncai_remove_event_from_thread(ncaiEnv *env, ncaiEventKind event_type,
                                   ncaiThread event_thread)
{
    NCAIEnv *p_env = (NCAIEnv *)env;
    ncaiEventThread *et =
        p_env->event_threads[event_type - NCAI_MIN_EVENT_TYPE_VAL];

    LMAutoUnlock aulock(p_env->env_lock); //ncai env general lock

    // Address of 'next' in previous list item or address of list root
    // Is a place where current->next must be placed when removing item
    ncaiEventThread **plast_next = &(p_env->event_threads[event_type - NCAI_MIN_EVENT_TYPE_VAL]);

    // Find out if this environment is already registered
    // on this thread on this event type
    for (ncaiEventThread* current = et; current != NULL; current = current->next)
    {
        if (current->thread == event_thread)
        {
            *plast_next = current->next;
            ncai_free(current);
            return;
        }
        plast_next = &current->next;
    }
}

void ncai_add_event_to_global(ncaiEnv *env, ncaiEventKind event_type)
{
    NCAIEnv *p_env = (NCAIEnv *)env;
    p_env->global_events[event_type - NCAI_MIN_EVENT_TYPE_VAL] = true;
}

void ncai_remove_event_from_global(ncaiEnv *env, ncaiEventKind event_type)
{
    NCAIEnv *p_env = (NCAIEnv *)env;
    p_env->global_events[event_type - NCAI_MIN_EVENT_TYPE_VAL] = false;
}

static ncaiError ncai_check_start_single_step(NCAIEnv* env)
{
    if (env->ti_env->vm->vm_env->NCAI->step_enabled)
        return NCAI_ERROR_NONE;

    return ncai_start_single_step(env);
}

static ncaiError ncai_check_stop_single_step(NCAIEnv* env)
{
    DebugUtilsTI* ti = env->ti_env->vm->vm_env->TI;
    GlobalNCAI* ncai = env->ti_env->vm->vm_env->NCAI;

    if (!ncai->step_enabled)
        return NCAI_ERROR_NONE;

    // Check that no environment has SingleStep enabled
    LMAutoUnlock lock(&ti->TIenvs_lock);
    bool disable = true;

    for (TIEnv *ti_env = ti->getEnvironments(); ti_env;
         ti_env = ti_env->next)
    {
        if (!ti_env->ncai_env)
            continue;

        if (ti_env->ncai_env->global_events[NCAI_EVENT_STEP - NCAI_MIN_EVENT_TYPE_VAL] ||
            ti_env->ncai_env->event_threads[NCAI_EVENT_STEP - NCAI_MIN_EVENT_TYPE_VAL])
        {
            disable = false;
            break;
        }
    }

    return disable ? ncai_stop_single_step(env) : NCAI_ERROR_NONE;
}

void ncai_report_method_entry(jmethodID method)
{
    GlobalNCAI* ncai = VM_Global_State::loader_env->NCAI;

    if (!GlobalNCAI::isEnabled())
        return;

    assert(method);

    Method* m = reinterpret_cast<Method*>(method);
    bool is_native = m->is_native();

    if (is_native && ncai->step_enabled)
        ncai_step_native_method_entry(m);
}

void ncai_report_method_exit(jmethodID method, jboolean exc_popped, jvalue ret_val)
{
    GlobalNCAI* ncai = VM_Global_State::loader_env->NCAI;

    if (!GlobalNCAI::isEnabled())
        return;

    assert(method);

    Method* m = reinterpret_cast<Method*>(method);
    bool is_native = m->is_native();

    if (is_native && ncai->step_enabled)
        ncai_step_native_method_exit(m);
}
