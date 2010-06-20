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
 * @file thread_ti_timing.c
 * @brief JVMTI timing related functions
 */

#include <open/hythread_ext.h>
#include "jthread.h"
#include "ti_thread.h"
#include "vm_threads.h"

#define THREAD_CPU_TIME_SUPPORTED 1

/*
 *  Thread CPU time enabled flag.
 */
int thread_cpu_time_enabled = 0;

/**
 * Returns time spent by the specific thread while contending for monitors.
 *
 * @param[in] java_thread
 * @param[out] nanos_ptr CPU time in nanoseconds
 */
IDATA VMCALL
jthread_get_thread_blocked_time(jthread java_thread, jlong * nanos_ptr)
{
    assert(java_thread);
    assert(nanos_ptr);
    vm_thread_t vm_thread = jthread_get_vm_thread_from_java(java_thread);
    assert(vm_thread);
    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;
    if (jvmti_thread)
        *nanos_ptr = jvmti_thread->blocked_time;
	else
		*nanos_ptr = 0;
    return TM_ERROR_NONE;
} // jthread_get_thread_blocked_time

/**
 * Returns time utilized by given thread.
 *
 * @param[in] java_thread
 * @param[out] nanos_ptr CPU time in nanoseconds
 */
IDATA VMCALL
jthread_get_thread_cpu_time(jthread java_thread, jlong * nanos_ptr)
{
    int64 kernel_time;

    assert(nanos_ptr);
    hythread_t native_thread = (NULL == java_thread)
        ? hythread_self() : jthread_get_native_thread(java_thread);
    assert(native_thread);
    return hythread_get_thread_times(native_thread, &kernel_time,
                                     nanos_ptr);
} // jthread_get_thread_cpu_time

/**
 * Returns information about the system timer.
 *
 * @param[out] info_ptr timer info
 */
IDATA VMCALL jthread_get_thread_cpu_timer_info(jvmtiTimerInfo * info_ptr)
{
    return TM_ERROR_NONE;
} // jthread_get_thread_cpu_timer_info

/**
 * Returns time utilized by the given thread in user mode.
 *
 * @param[in] java_thread 
 * @param[out] nanos_ptr CPU time in nanoseconds
 */
IDATA VMCALL
jthread_get_thread_user_cpu_time(jthread java_thread, jlong * nanos_ptr)
{
    assert(nanos_ptr);
    assert(java_thread);
    hythread_t native_thread = jthread_get_native_thread(java_thread);
    assert(native_thread);

    int64 kernel_time;
    int64 user_time;
    IDATA status = hythread_get_thread_times(native_thread,
                        &kernel_time, &user_time);
    assert(status == TM_ERROR_NONE);
    *nanos_ptr = user_time;
    return TM_ERROR_NONE;
} // jthread_get_thread_user_cpu_time

/**
 * Returns time spent by the specific thread while waiting for monitors.
 *
 * @param[in] java_thread 
 * @param[out] nanos_ptr CPU time in nanoseconds
 */
IDATA VMCALL
jthread_get_thread_waited_time(jthread java_thread, jlong * nanos_ptr)
{
    assert(java_thread);
    assert(nanos_ptr);
    vm_thread_t vm_thread = jthread_get_vm_thread_from_java(java_thread);
    assert(vm_thread);
    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;
    if (jvmti_thread)
        *nanos_ptr = jvmti_thread->waited_time;
	else
		*nanos_ptr = 0;

    return TM_ERROR_NONE;
} // jthread_get_thread_waited_time

/**
 * Returns number of times the specific thread contending for monitors.
 *
 * @param[in] java_thread
 * @return number of times the specific thread contending for monitors
 */
jlong VMCALL jthread_get_thread_blocked_times_count(jthread java_thread)
{
    assert(java_thread);
    hythread_t native_thread = jthread_get_native_thread(java_thread);
    assert(native_thread);
    jvmti_thread_t jvmti_thread = jthread_get_jvmti_thread(native_thread);
    if (jvmti_thread)
        return jvmti_thread->blocked_count;
	else
		return 0;
} // jthread_get_thread_blocked_times_count

/**
 * Returns number of times the specific thread waiting on monitors for notification.
 *
 * @param[in] java_thread
 * @return number of times the specific thread waiting on monitors for notification
 */
jlong VMCALL jthread_get_thread_waited_times_count(jthread java_thread)
{

    hythread_t native_thread = jthread_get_native_thread(java_thread);
    assert(native_thread);
    jvmti_thread_t jvmti_thread = jthread_get_jvmti_thread(native_thread);
    if (jvmti_thread)
        return jvmti_thread->waited_count;
	else
		return 0;
} // jthread_get_thread_waited_times_count

/**
 * Returns true if VM supports current thread CPU and USER time requests
 *
 * @return true if current thread CPU and USER time requests are supported, false otherwise;
 */
jboolean jthread_is_current_thread_cpu_time_supported()
{
    return THREAD_CPU_TIME_SUPPORTED;
} // jthread_is_current_thread_cpu_time_supported

/**
 * Returns true if VM supports thread CPU and USER time requests
 *
 * @return true if thread CPU and USER time requests are supported, false otherwise;
 */
jboolean jthread_is_thread_cpu_time_supported()
{
    return THREAD_CPU_TIME_SUPPORTED;
} // jthread_is_thread_cpu_time_supported

/**
 * Returns true if VM supports (current) thread CPU and USER time requests and 
 * this feature is enabled 
 *
 * @return true if thread CPU and USER time requests are enabled, false otherwise;
 */
jboolean jthread_is_thread_cpu_time_enabled()
{
    return thread_cpu_time_enabled;
} // jthread_is_thread_cpu_time_enabled

/**
 * Enabled or diabled thread CPU and USER time requests
 *
 * @param[in] true or false to enable or disable the feature
 */
void jthread_set_thread_cpu_time_enabled(jboolean flag)
{
    thread_cpu_time_enabled = THREAD_CPU_TIME_SUPPORTED ? flag : 0;
} // jthread_set_thread_cpu_time_enabled
