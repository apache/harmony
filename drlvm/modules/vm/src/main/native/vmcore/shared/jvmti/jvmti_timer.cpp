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
 * @author Gregory Shimansky
 */
/*
 * JVMTI timer API
 */

#include <apr_time.h>

#include "jvmti_direct.h"
#include "jvmti_utils.h"
#include "time.h"
#include "cxxlog.h"
#include "port_sysinfo.h"
#include "suspend_checker.h"
#include "jvmti_internal.h"
#include "jthread.h"
#include "jvmti.h"

/**
* Sets field values to provided jvmtiTimerInfo structure.
*/
static inline void fill_timer_info(jvmtiTimerInfo* info_ptr)
{
    info_ptr->max_value = 0xffffffffffffffffULL;    // max unsigned long long
    info_ptr->may_skip_forward = JNI_FALSE;
    info_ptr->may_skip_backward = JNI_FALSE;

#ifdef PLATFORM_POSIX
    // linux api provides only total cpu time measurement :(
    info_ptr->kind = JVMTI_TIMER_TOTAL_CPU;
#elif PLATFORM_NT
    info_ptr->kind = JVMTI_TIMER_USER_CPU;
#endif
}

/*
 * Get Current Thread CPU Timer Information
 *
 * Get information about the GetCurrentThreadCpuTime timer.
 * The fields of the jvmtiTimerInfo structure are filled in
 * with details about the timer. This information is specific
 * to the platform and the implementation of GetCurrentThreadCpuTime
 * and thus does not vary by thread nor does it vary during a
 * particular invocation of the VM.
 *
 * OPTIONAL Functionality.
 */
jvmtiError JNICALL
jvmtiGetCurrentThreadCpuTimerInfo(jvmtiEnv* env,
                                  jvmtiTimerInfo* info_ptr)
{
    TRACE2("jvmti.timer", "GetCurrentThreadCpuTimerInfo called");
  //  TRACE("GetCurrentThreadCpuTimerInfo called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    CHECK_CAPABILITY(can_get_current_thread_cpu_time);

    if (NULL == info_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    fill_timer_info(info_ptr);

    return JVMTI_ERROR_NONE;
}

/*
 * Get Current Thread CPU Time
 *
 * Return the CPU time utilized by the current thread.
 *
 * OPTIONAL Functionality.
 */
jvmtiError JNICALL
jvmtiGetCurrentThreadCpuTime(jvmtiEnv* env,
                             jlong* nanos_ptr)
{
    TRACE2("jvmti.timer", "GetCurrentThreadCpuTime called");
    IDATA status;
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};
    CHECK_EVERYTHING();

    CHECK_CAPABILITY(can_get_current_thread_cpu_time);

    if (NULL == nanos_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    status = jthread_get_thread_cpu_time(NULL, nanos_ptr);

    if (status != TM_ERROR_NONE)
        return JVMTI_ERROR_INTERNAL;

    return JVMTI_ERROR_NONE;
}

/*
 * Get Thread CPU Timer Information
 *
 * Get information about the GetThreadCpuTime timer. The fields
 * of the jvmtiTimerInfo structure are filled in with details
 * about the timer.
 *
 * OPTIONAL Functionality.
 */
jvmtiError JNICALL
jvmtiGetThreadCpuTimerInfo(jvmtiEnv* env,
                           jvmtiTimerInfo* info_ptr)
{
    TRACE2("jvmti.timer", "GetThreadCpuTimerInfo called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    CHECK_CAPABILITY(can_get_thread_cpu_time);

    if (NULL == info_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    fill_timer_info(info_ptr);

    return JVMTI_ERROR_NONE;
}

/*
 * Get Thread CPU Time
 *
 * Return the CPU time utilized by the specified thread.
 *
 * OPTIONAL Functionality.
 */
jvmtiError JNICALL
jvmtiGetThreadCpuTime(jvmtiEnv* env,
                      jthread thread,
                      jlong* nanos_ptr)
{
    TRACE2("jvmti.timer", "GetThreadCpuTime called");
    IDATA status;
    SuspendEnabledChecker sec;
   /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    CHECK_CAPABILITY(can_get_thread_cpu_time);

    if (NULL == nanos_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    if (NULL == thread) {
        status = jthread_get_thread_cpu_time(NULL, nanos_ptr);

    } else {
        if (! is_valid_thread_object(thread))
            return JVMTI_ERROR_INVALID_THREAD;

        // lock thread manager to avoid occasional change of thread state
        hythread_global_lock();

        int state;// =thread_get_thread_state(thread);
        jvmtiError err = jvmtiGetThreadState(env, thread, &state);
        if (err != JVMTI_ERROR_NONE){
   	    return err;
	    }

        switch (state)
        {
        case JVMTI_THREAD_STATE_TERMINATED:     // thread is terminated
        case JVMTI_JAVA_LANG_THREAD_STATE_NEW:  // thread is new
            hythread_global_unlock();
            return JVMTI_ERROR_THREAD_NOT_ALIVE;
        default:    // thread is alive
            status = jthread_get_thread_cpu_time(thread, nanos_ptr);

            break;
        }

        hythread_global_unlock();
    }

    if (status != TM_ERROR_NONE)
        return JVMTI_ERROR_INTERNAL;

    return JVMTI_ERROR_NONE;
}

/*
 * Get Timer Information
 *
 * Get information about the GetTime timer. The fields of the
 * jvmtiTimerInfo structure are filled in with details about the
 * timer. This information will not change during a particular
 * invocation of the VM.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetTimerInfo(jvmtiEnv* env,
                  jvmtiTimerInfo* info_ptr)
{
    TRACE2("jvmti.timer", "GetTimerInfo called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase* phases = NULL;

    CHECK_EVERYTHING();

    if (NULL == info_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    fill_timer_info(info_ptr);

    return JVMTI_ERROR_NONE;
}

/*
 * Get Time
 *
 * Return the current value of the system timer, in nanoseconds.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetTime(jvmtiEnv* env,
             jlong* nanos_ptr)
{
    TRACE2("jvmti.timer", "GetTime called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase* phases = NULL;

    CHECK_EVERYTHING();

    if (nanos_ptr == NULL)
    {
        return JVMTI_ERROR_NULL_POINTER;
    }
    // get time in milliseconds
    apr_time_t apr_time = apr_time_now();

    *nanos_ptr = ((jlong) apr_time) * 1000ULL; // convert to nanos

    return JVMTI_ERROR_NONE;
}

/*
 * Get Available Processors
 *
 * Returns the number of processors available to the Java virtual
 * machine.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetAvailableProcessors(jvmtiEnv* env,
                            jint* processor_count_ptr)
{
    TRACE2("jvmti.timer", "GetAvailableProcessors called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase* phases = NULL;

    CHECK_EVERYTHING();

    if (processor_count_ptr == NULL)
    {
        return JVMTI_ERROR_NULL_POINTER;
    }

    *processor_count_ptr = port_CPUs_number();

    return JVMTI_ERROR_NONE;
}
