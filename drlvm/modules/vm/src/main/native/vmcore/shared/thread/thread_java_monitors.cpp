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
 * @file thread_java_monitors.c
 * @brief Java thread monitors related functions
 */
#define LOG_DOMAIN "tm.monitor"
#include "cxxlog.h"

#include <open/hythread_ext.h>
#include "jthread.h"
#include "vm_threads.h"
#include "jni.h"

static void jthread_add_owned_monitor(jobject monitor);
static void jthread_remove_owned_monitor(jobject monitor);
static void jthread_set_owned_monitor(jobject monitor);
static void jthread_set_wait_monitor(jobject monitor);


/**
 *  Initializes Java monitor.
 *
 *  Monitor is a recursive lock with one conditional variable associated with it.
 *  Implementation may use the knowledge of internal object layout in order to allocate lock
 *  and conditional variable in the most efficient manner.
 *
 *  @param[in] monitor object where monitor needs to be initialized.
 */
IDATA VMCALL jthread_monitor_init(jobject monitor)
{
    assert(monitor);

    hythread_suspend_disable();
    hythread_thin_monitor_t *lockword = vm_object_get_lockword_addr(monitor);
    IDATA status = hythread_thin_monitor_create(lockword);
    hythread_suspend_enable();
    return status;
} // jthread_monitor_init



/**
 * Gains the ownership over monitor.
 *
 * Current thread blocks if the specified monitor is owned by other thread.
 *
 * @param[in] monitor object where monitor is located
 * @sa JNI::MonitorEnter()
 */
IDATA VMCALL jthread_monitor_enter(jobject monitor)
{
    IDATA state;
    hythread_t native_thread;
    apr_time_t enter_begin;

    assert(monitor);
    hythread_suspend_disable();
    hythread_thin_monitor_t *lockword = vm_object_get_lockword_addr(monitor);
    IDATA status = hythread_thin_monitor_try_enter(lockword);
    if (status != TM_ERROR_EBUSY) {
        goto entered;
    }

#ifdef LOCK_RESERVATION
    // busy unreserve lock before blocking and inflating
    while (TM_ERROR_NONE != hythread_unreserve_lock(lockword)) {
        hythread_yield();
        hythread_safe_point();
        hythread_exception_safe_point();
        lockword = vm_object_get_lockword_addr(monitor);
    }
    status = hythread_thin_monitor_try_enter(lockword);
    if (status != TM_ERROR_EBUSY) {
        goto entered;
    }
#endif //LOCK_RESERVATION

    native_thread = hythread_self();
    hythread_thread_lock(native_thread);
    state = hythread_get_state(native_thread);
    state &= ~TM_THREAD_STATE_RUNNABLE;
    state |= TM_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER;
    status = hythread_set_state(native_thread, state);
    assert(status == TM_ERROR_NONE);
    hythread_thread_unlock(native_thread);

    // should be moved to event handler
    if (ti_is_enabled()) {
        enter_begin = apr_time_now();
        int disable_count = hythread_reset_suspend_disable();
        jthread_set_owned_monitor(monitor);
        if(jvmti_should_report_event(JVMTI_EVENT_MONITOR_CONTENDED_ENTER)) {
            jvmti_send_contended_enter_or_entered_monitor_event(monitor, 1);
        }
        hythread_set_suspend_disable(disable_count);
    }

    // busy wait and inflate
    // reload pointer after safepoints
    lockword = vm_object_get_lockword_addr(monitor);
    while ((status =
            hythread_thin_monitor_try_enter(lockword)) == TM_ERROR_EBUSY)
    {
        hythread_safe_point();
        hythread_exception_safe_point();
        lockword = vm_object_get_lockword_addr(monitor);

        if (hythread_is_fat_lock(*lockword)) {
            status = hythread_thin_monitor_enter(lockword);
            if (status != TM_ERROR_NONE) {
                hythread_suspend_enable();
                assert(0);
                return status;
            }
            goto contended_entered;
        }
        hythread_yield();
    }
    assert(status == TM_ERROR_NONE);
    if (!hythread_is_fat_lock(*lockword)) {
        hythread_inflate_lock(lockword);
    }

// do all ti staff here
contended_entered:
    if (ti_is_enabled()) {
        int disable_count = hythread_reset_suspend_disable();
        if(jvmti_should_report_event(JVMTI_EVENT_MONITOR_CONTENDED_ENTERED)) {
            jvmti_send_contended_enter_or_entered_monitor_event(monitor, 0);
        }
        hythread_set_suspend_disable(disable_count);
        // should be moved to event handler
        jvmti_thread_t jvmti_thread =
            jthread_get_jvmti_thread(hythread_self());
        jvmti_thread->blocked_time += apr_time_now() - enter_begin;
    }

    hythread_thread_lock(native_thread);
    state = hythread_get_state(native_thread);
    state &= ~TM_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER;
    state |= TM_THREAD_STATE_RUNNABLE;
    status = hythread_set_state(native_thread, state);
    assert(status == TM_ERROR_NONE);
    hythread_thread_unlock(native_thread);

entered:
    if (ti_is_enabled()) {
        jthread_add_owned_monitor(monitor);
    }
    hythread_suspend_enable();
    return TM_ERROR_NONE;
} // jthread_monitor_enter

/**
 * Attempt to gain the ownership over monitor without blocking.
 *
 * @param[in] monitor object where monitor is located
 */
IDATA VMCALL jthread_monitor_try_enter(jobject monitor)
{
    assert(monitor);

    hythread_suspend_disable();
    hythread_thin_monitor_t *lockword = vm_object_get_lockword_addr(monitor);
    IDATA status = hythread_thin_monitor_try_enter(lockword);
    hythread_suspend_enable();

    if (status == TM_ERROR_NONE && ti_is_enabled()) {
        jthread_add_owned_monitor(monitor);
    }
    return status;
} // jthread_monitor_try_enter

/**
 * Releases the ownership over monitor.
 *
 * @param[in] monitor monitor
 * @sa JNI::MonitorExit()
 */
IDATA VMCALL jthread_monitor_exit(jobject monitor)
{
    assert(monitor);

    hythread_suspend_disable();
    hythread_thin_monitor_t *lockword = vm_object_get_lockword_addr(monitor);
    IDATA status = hythread_thin_monitor_exit(lockword);
    hythread_suspend_enable();

    if (status == TM_ERROR_NONE && ti_is_enabled()) {
        jthread_remove_owned_monitor(monitor);
    }
    if (status == TM_ERROR_ILLEGAL_STATE) {
        jthread_throw_exception("java/lang/IllegalMonitorStateException",
                                "Illegal monitor state");
    }
    return status;
} // jthread_monitor_exit

/**
 * Completely releases the ownership over monitor.
 *
 * @param[in] monitor monitor
 */
IDATA VMCALL jthread_monitor_release(jobject monitor)
{
    assert(monitor);

    hythread_suspend_disable();
    hythread_thin_monitor_t *lockword = vm_object_get_lockword_addr(monitor);
    IDATA status = hythread_thin_monitor_release(lockword);
    assert(status == TM_ERROR_NONE);
    hythread_suspend_enable();

    return TM_ERROR_NONE;
} // jthread_monitor_release

/**
 * Notifies one thread waiting on the monitor.
 *
 * Only single thread waiting on the
 * object's monitor is waked up.
 * Nothing happens if no threads are waiting on the monitor.
 *
 * @param[in] monitor object where monitor is located
 * @sa java.lang.Object.notify() 
 */
IDATA VMCALL jthread_monitor_notify(jobject monitor)
{
    assert(monitor);

    hythread_suspend_disable();
    hythread_thin_monitor_t *lockword = vm_object_get_lockword_addr(monitor);
    IDATA status = hythread_thin_monitor_notify(lockword);
    hythread_suspend_enable();

    return status;
} // jthread_monitor_notify

/**
 * Notifies all threads which are waiting on the monitor.
 *
 * Each thread from the set of threads waiting on the
 * object's monitor is waked up.
 *
 * @param[in] monitor object where monitor is located
 * @sa java.lang.Object.notifyAll() 
 */
IDATA VMCALL jthread_monitor_notify_all(jobject monitor)
{
    assert(monitor);

    hythread_suspend_disable();
    hythread_thin_monitor_t *lockword = vm_object_get_lockword_addr(monitor);
    IDATA status = hythread_thin_monitor_notify_all(lockword);
    hythread_suspend_enable();

    return status;
} // jthread_monitor_notify_all

/**
 * Wait on the <code>object</code>'s monitor.
 *
 * This function instructs the current thread to be scheduled off 
 * the processor and wait on the monitor until the following occurs: 
 * <UL>
 * <LI>another thread invokes <code>thread_notify(object)</code>
 * and VM chooses this thread to wake up;
 * <LI>another thread invokes <code>thread_notifyAll(object);</code>
 * <LI>another thread invokes <code>thread_interrupt(thread);</code>
 * </UL>
 *
 * @param[in] monitor object where monitor is located
 * @sa java.lang.Object.wait()
 * @return 
 */
IDATA VMCALL jthread_monitor_wait(jobject monitor)
{
    return jthread_monitor_timed_wait(monitor, 0, 0);
} // jthread_monitor_wait

/**
 * Wait on the <code>object</code>'s monitor with the specified timeout.
 *
 * This function instructs the current thread to be scheduled off 
 * the processor and wait on the monitor until the following occurs: 
 * <UL>
 * <LI>another thread invokes <code>thread_notify(object)</code>
 * and VM chooses this thread to wake up;
 * <LI>another thread invokes <code>thread_notifyAll(object);</code>
 * <LI>another thread invokes <code>thread_interrupt(thread);</code>
 * <LI>real time elapsed from the waiting begin is
 * greater or equal the timeout specified.
 * </UL>
 *
 * @param[in] monitor object where monitor is located
 * @param[in] millis time to wait (in milliseconds)
 * @param[in] nanos time to wait (in nanoseconds)
 * @sa java.lang.Object.wait()
 */
IDATA VMCALL
jthread_monitor_timed_wait(jobject monitor, jlong millis, jint nanos)
{
    assert(monitor);

    hythread_suspend_disable();
    hythread_t native_thread = hythread_self();
    hythread_thin_monitor_t *lockword = vm_object_get_lockword_addr(monitor);
    if (!hythread_is_fat_lock(*lockword)) {
        if (!hythread_owns_thin_lock(native_thread, *lockword)) {
            CTRACE(("ILLEGAL_STATE wait %x\n", lockword));
            hythread_suspend_enable();
            return TM_ERROR_ILLEGAL_STATE;
        }
        hythread_inflate_lock(lockword);
    }

    apr_time_t wait_begin;
    if (ti_is_enabled()) {
        int disable_count = hythread_reset_suspend_disable();
        jthread_set_wait_monitor(monitor);
        jthread_set_owned_monitor(monitor);
        if(jvmti_should_report_event(JVMTI_EVENT_MONITOR_WAIT)) {
            jvmti_send_wait_monitor_event(monitor, (jlong) millis);
        }
        if(jvmti_should_report_event(JVMTI_EVENT_MONITOR_CONTENDED_ENTER)) {
            jvmti_send_contended_enter_or_entered_monitor_event(monitor, 1);
        }
        hythread_set_suspend_disable(disable_count);

        // should be moved to event handler
        wait_begin = apr_time_now();
        jthread_remove_owned_monitor(monitor);
    }

    hythread_thread_lock(native_thread);
    IDATA state = hythread_get_state(native_thread);
    state &= ~TM_THREAD_STATE_RUNNABLE;
    state |= TM_THREAD_STATE_WAITING | TM_THREAD_STATE_IN_MONITOR_WAIT;
    if ((millis > 0) || (nanos > 0)) {
        state |= TM_THREAD_STATE_WAITING_WITH_TIMEOUT;
    }
    else {
        state |= TM_THREAD_STATE_WAITING_INDEFINITELY;
    }
    IDATA status = hythread_set_state(native_thread, state);
    assert(status == TM_ERROR_NONE);
    hythread_thread_unlock(native_thread);

    status =
        hythread_thin_monitor_wait_interruptable(lockword, millis, nanos);

    hythread_thread_lock(native_thread);
    state = hythread_get_state(native_thread);
    if ((millis > 0) || (nanos > 0)) {
        state &= ~TM_THREAD_STATE_WAITING_WITH_TIMEOUT;
    }
    else {
        state &= ~TM_THREAD_STATE_WAITING_INDEFINITELY;
    }
    state &= ~(TM_THREAD_STATE_WAITING | TM_THREAD_STATE_IN_MONITOR_WAIT);
    state |= TM_THREAD_STATE_RUNNABLE;
    hythread_set_state(native_thread, state);
    hythread_thread_unlock(native_thread);

    hythread_suspend_enable();
    if (ti_is_enabled()) {
        jthread_add_owned_monitor(monitor);
        int disable_count = hythread_reset_suspend_disable();
        if(jvmti_should_report_event(JVMTI_EVENT_MONITOR_CONTENDED_ENTERED)) {
            jvmti_send_contended_enter_or_entered_monitor_event(monitor, 0);
        }
        if(jvmti_should_report_event(JVMTI_EVENT_MONITOR_WAITED)) {
            jvmti_send_waited_monitor_event(monitor,
                ((status == APR_TIMEUP) ? (jboolean) 1 : (jboolean) 0));
        }
        hythread_set_suspend_disable(disable_count);
        // should be moved to event handler
        jvmti_thread_t jvmti_thread =
            jthread_get_jvmti_thread(hythread_self());
        jvmti_thread->waited_time += apr_time_now() - wait_begin;
    }
    return status;
} // jthread_monitor_timed_wait

static void jthread_add_owned_monitor(jobject monitor)
{
    vm_thread_t vm_thread = jthread_self_vm_thread();
    assert(vm_thread);
    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;

    if (!jvmti_thread) {
        // nothing to do
        return;
    }
    CTRACE(("TM: add owned monitor: %x", monitor));

    int disable_status = hythread_reset_suspend_disable();

    if (jvmti_thread->contended_monitor) {
        vm_thread->jni_env->
            DeleteGlobalRef(jvmti_thread->contended_monitor);
        jvmti_thread->contended_monitor = NULL;
    }

    if (jvmti_thread->wait_monitor) {
        vm_thread->jni_env->DeleteGlobalRef(jvmti_thread->wait_monitor);
        jvmti_thread->wait_monitor = NULL;
    }

    if (jvmti_thread->owned_monitors_nmb >= jvmti_thread->owned_monitors_size) {
        int new_size = jvmti_thread->owned_monitors_size * 2;

        CTRACE(("Increasing owned_monitors_size to: %d", new_size));
        jobject* new_monitors = (jobject*)apr_palloc(vm_thread->pool,
                new_size * sizeof(jobject));
        assert(new_monitors);
        memcpy(new_monitors, jvmti_thread->owned_monitors,
                jvmti_thread->owned_monitors_size * sizeof(jobject));
        jvmti_thread->owned_monitors = new_monitors;
        jvmti_thread->owned_monitors_size = new_size;
    }

    jvmti_thread->owned_monitors[jvmti_thread->owned_monitors_nmb]
        = vm_thread->jni_env->NewGlobalRef(monitor);
    jvmti_thread->owned_monitors_nmb++;

    hythread_set_suspend_disable(disable_status);
} // jthread_add_owned_monitor

static void jthread_remove_owned_monitor(jobject monitor)
{
    vm_thread_t vm_thread = jthread_self_vm_thread();
    assert(vm_thread);
    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;

    if (!jvmti_thread) {
        // nothing to do
        return;
    }
    CTRACE(("TM: remove owned monitor: %x", monitor));

    for (int i = jvmti_thread->owned_monitors_nmb - 1; i >= 0; i--) {
        if (vm_objects_are_equal(jvmti_thread->owned_monitors[i], monitor)) {
            int disable_status = hythread_reset_suspend_disable();
            vm_thread->jni_env->DeleteGlobalRef(jvmti_thread->owned_monitors[i]);
            hythread_set_suspend_disable(disable_status);
            int j;
            for (j = i; j < jvmti_thread->owned_monitors_nmb - 1; j++) {
                jvmti_thread->owned_monitors[j] =
                    jvmti_thread->owned_monitors[j + 1];
            }
            jvmti_thread->owned_monitors[j] = NULL;
            jvmti_thread->owned_monitors_nmb--;
            return;
        }
    }
} // jthread_remove_owned_monitor

static void jthread_set_owned_monitor(jobject monitor)
{
    vm_thread_t vm_thread = jthread_self_vm_thread();
    assert(vm_thread);
    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;

    if (!jvmti_thread) {
        // nothing to do
        return;
    }
    CTRACE(("TM: set contended monitor: %x", monitor));

    int disable_count = hythread_reset_suspend_disable();
    jvmti_thread->contended_monitor = vm_thread->jni_env->NewGlobalRef(monitor);
    hythread_set_suspend_disable(disable_count);
} // jthread_set_owned_monitor

static void jthread_set_wait_monitor(jobject monitor)
{
    vm_thread_t vm_thread = jthread_self_vm_thread();
    assert(vm_thread);
    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;

    if (!jvmti_thread) {
        // nothing to do
        return;
    }
    CTRACE(("TM: set wait monitor: %x", monitor));

    int disable_count = hythread_reset_suspend_disable();
    jvmti_thread->wait_monitor = vm_thread->jni_env->NewGlobalRef(monitor);
    hythread_set_suspend_disable(disable_count);
} // jthread_set_wait_monitor
