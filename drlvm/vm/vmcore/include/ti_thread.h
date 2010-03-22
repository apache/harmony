/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


#ifndef __TI_THREAD_H__
#define __TI_THREAD_H__

/**
 * @file ti_thread.h
 * @brief JVMTI support 
 * @details
 * TI part of the Java threading interface.
 * The TI part is mostly targeted to address the needs of JVMTI and 
 * <code>java.lang.management</code> classes needs.
 * All functions start with <code>jthread_*</code> prefix.
 */

#include "open/hythread.h"
#include "jvmti_types.h"

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/**
 * JVM TI local storage structure.
 *
 * @param[in] thread 
 */
typedef struct {
    jvmtiEnv * env;
    void * data;
} JVMTILocalStorage;

typedef struct HyThread *jthread_iterator_t;
/** @name State query
 */
//@{

VMEXPORT IDATA jthread_get_jvmti_state(jthread thread, jint *thread_state);

//@}
/** @name Instrumentation
 */
//@{

VMEXPORT IDATA jthread_get_all_threads(jthread** threads, jint *count);
VMEXPORT IDATA jthread_get_deadlocked_threads(jthread* thread_list, jint thread_count, jthread** dead_list, jint *dead_count);
VMEXPORT IDATA jthread_get_thread_count(jint *count);
VMEXPORT IDATA jthread_get_blocked_count(jint* count);
VMEXPORT IDATA jthread_get_waited_count(jint* count);
VMEXPORT IDATA jthread_get_total_started_thread_count(jint* count);

//@}
/** @name Monitor info
 */
//@{

VMEXPORT IDATA jthread_get_contended_monitor(jthread thread, jobject* monitor);
VMEXPORT IDATA jthread_get_wait_monitor(jthread thread, jobject* monitor);
VMEXPORT jboolean jthread_holds_lock(jthread thread, jobject monitor);
VMEXPORT IDATA jthread_get_lock_owner(jobject monitor, jthread* lock_owner);
VMEXPORT IDATA jthread_get_lock_recursion(jobject monitor, jthread lock_owner);
VMEXPORT IDATA jthread_get_owned_monitors(jthread thread, jint* mon_count_ptr, jobject** monitors);

VMEXPORT jboolean jthread_is_thread_contention_monitoring_enabled();
VMEXPORT jboolean jthread_is_thread_contention_monitoring_supported();
VMEXPORT void jthread_set_thread_contention_monitoring_enabled(jboolean flag);

//@}
/** @name CPU timing
 */
//@{

VMEXPORT IDATA jthread_get_thread_cpu_time(jthread thread, jlong *nanos_ptr);
VMEXPORT IDATA jthread_get_thread_user_cpu_time(jthread thread, jlong *nanos_ptr);
VMEXPORT IDATA jthread_get_thread_blocked_time(jthread thread, jlong *nanos_ptr);
VMEXPORT IDATA jthread_get_thread_waited_time(jthread thread, jlong *nanos_ptr);
VMEXPORT IDATA jthread_get_thread_cpu_timer_info(jvmtiTimerInfo* info_ptr);

VMEXPORT jlong jthread_get_thread_blocked_times_count(jthread java_thread);
VMEXPORT jlong jthread_get_thread_waited_times_count(jthread java_thread);

VMEXPORT jboolean jthread_is_current_thread_cpu_time_supported();
VMEXPORT jboolean jthread_is_thread_cpu_time_enabled();
VMEXPORT jboolean jthread_is_thread_cpu_time_supported();

VMEXPORT void jthread_set_thread_cpu_time_enabled(jboolean flag);

//@}
/** @name Peak count
 */
//@{

VMEXPORT IDATA jthread_reset_peak_thread_count();
VMEXPORT IDATA jthread_get_peak_thread_count(jint *threads_count_ptr);

//@}
/** @name Raw monitors
 */
//@{

VMEXPORT IDATA jthread_raw_monitor_create(jrawMonitorID *mon_ptr);
VMEXPORT IDATA jthread_raw_monitor_destroy(jrawMonitorID mon_ptr);
VMEXPORT IDATA jthread_raw_monitor_enter(jrawMonitorID mon_ptr);
VMEXPORT IDATA jthread_raw_monitor_try_enter(jrawMonitorID mon_ptr);
VMEXPORT IDATA jthread_raw_monitor_exit(jrawMonitorID mon_ptr);
VMEXPORT IDATA jthread_raw_monitor_notify(jrawMonitorID mon_ptr);
VMEXPORT IDATA jthread_raw_monitor_notify_all(jrawMonitorID mon_ptr);
VMEXPORT IDATA jthread_raw_monitor_wait(jrawMonitorID mon_ptr, I_64 millis);

//@}

VMEXPORT JVMTILocalStorage* jthread_get_jvmti_local_storage(jthread java_thread);

/** @name jthread iterators
 */
//@{

VMEXPORT jthread_iterator_t jthread_iterator_create(void);
VMEXPORT jthread jthread_iterator_next(jthread_iterator_t *it);
VMEXPORT IDATA jthread_iterator_reset(jthread_iterator_t *it);
VMEXPORT IDATA jthread_iterator_size(jthread_iterator_t iterator);
VMEXPORT IDATA jthread_iterator_release(jthread_iterator_t *it);

//@}

#ifdef __cplusplus
}
#endif

#endif  /* __TI_THREAD_H__ */
