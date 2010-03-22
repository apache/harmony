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
 * @file thread_ti_instr.c
 * @brief JVMTI basic related functions
 */

#include "open/hythread_ext.h"
#include "jthread.h"
#include "ti_thread.h"
#include "vm_threads.h"
#include "object_handles.h"

/**
 * Returns the list of all Java threads.
 *
 * @param[out] threads resulting threads list
 * @param[out] count_ptr number of threads in the resulting list
 */
IDATA VMCALL jthread_get_all_threads(jthread ** threads, jint * count_ptr)
{
    assert(threads);
    assert(count_ptr);

    hythread_group_t java_thread_group = get_java_thread_group();
    assert(java_thread_group);
    hythread_iterator_t iterator = hythread_iterator_create(java_thread_group);
    IDATA count = hythread_iterator_size(iterator);

    IDATA java_thread_count = 0;
    for (IDATA i = 0; i < count; i++) {
        hythread_t native_thread = hythread_iterator_next(&iterator);
        vm_thread_t vm_thread = jthread_get_vm_thread(native_thread);
        if (vm_thread && vm_thread->java_thread) {
            java_thread_count++;
        }
    }

    jthread *java_threads = (jthread*)malloc(sizeof(jthread) * java_thread_count);
    if (!java_threads) {
        hythread_iterator_release(&iterator);
        return TM_ERROR_OUT_OF_MEMORY;
    }

    hythread_iterator_reset(&iterator);
    java_thread_count = 0;
    for (IDATA i = 0; i < count; i++) {
        hythread_t native_thread = hythread_iterator_next(&iterator);
        vm_thread_t vm_thread = jthread_get_vm_thread(native_thread);
        if (vm_thread && vm_thread->java_thread) {
            hythread_suspend_disable();
            ObjectHandle thr = oh_allocate_local_handle();
            assert(thr);
            thr->object = vm_thread->java_thread->object;
            assert(thr->object);
            hythread_suspend_enable();
            java_threads[java_thread_count++] = thr;
        }
    }
    *threads = java_threads;
    *count_ptr = (jint)java_thread_count;
    IDATA status = hythread_iterator_release(&iterator);

    return status;
} // jthread_get_all_threads

/*
 */
int
jthread_find_deads(jobject thread, jobject *deads, int base, int top)
{
    for (int i = 0; i < top; i++) {
        if (vm_objects_are_equal(thread, deads[i])) {
            return 1;
        }
    }
    return 0;
}

// FIXME: synchronization and maybe thread suspension needed
/**
 * Checks for the deadlock conditions within the specified thread list.
 *
 * @param[in] thread_list thread list where to search for deadlock conditions
 * @param[in] thread_count number of threads in the thread list
 * @param[out] dead_list deadlocked threads
 * @param[out] dead_count number of deadlocked threads
 */
IDATA VMCALL
jthread_get_deadlocked_threads(jthread * thread_list,
                               jint thread_count,
                               jthread ** dead_list,
                               jint * dead_count)
{
    int deads_size;
    int deads_base;
    int deads_top;
    int output_top;

    IDATA status = TM_ERROR_NONE;
    jthread *deads = (jthread *) malloc(sizeof(jthread) * thread_count);
    jthread *output = (jthread *) malloc(sizeof(jthread) * thread_count);
    if ((deads == NULL) || (output == NULL)) {
        status = TM_ERROR_OUT_OF_MEMORY;
        goto free_allocated_memory;
    }

    deads_size = 1;
    deads_base = 0;
    deads_top = 0;
    output_top = 0;
    for (jint i = 0; i < thread_count; i++) {
        jthread thread = thread_list[i];
        output[output_top] = thread;
        while (true) {
            jobject monitor;
            IDATA status = jthread_get_contended_monitor(thread, &monitor);
            if (status != TM_ERROR_NONE) {
                goto free_allocated_memory;
            }
            if (!monitor) {
                deads_top = deads_base; // remove frame
                break;
            }
            if (jthread_find_deads(thread, deads, deads_base, deads_top)) {
                output_top++;
                deads_base = deads_top; // add frame
                break;
            }
            if (deads_top == deads_size) {
               // status = deads_expand(&deads, deads_size);
                if (status != TM_ERROR_NONE)
                    return status;
            }
            deads[deads_top] = thread;
            deads_top++;
            status = jthread_get_lock_owner(monitor, &thread);
            if (status != TM_ERROR_NONE) {
                goto free_allocated_memory;
            }
        }
    }

    if (output_top > 0) {
        output = (jthread*)realloc(output, sizeof(jthread) * output_top);
        if (!output) {
            status = TM_ERROR_OUT_OF_MEMORY;
            goto free_allocated_memory;
        }
        *dead_list = output;
    } else {
        *dead_list = NULL;
    }
    *dead_count = output_top;

    if (!deads) {
        free(deads);
    }
    return TM_ERROR_NONE;

free_allocated_memory:
    if (!deads) {
        free(deads);
    }
    if (!output) {
        free(output);
    }
    return status;
} // jthread_get_deadlocked_threads

/**
 * Returns the number of all Java threads.
 *
 * @param[out] count_ptr number of threads.
 */
IDATA VMCALL jthread_get_thread_count(jint * count_ptr)
{
    assert(count_ptr);
    hythread_group_t java_thread_group = get_java_thread_group();
    assert(java_thread_group);
    hythread_iterator_t iterator = hythread_iterator_create(java_thread_group);
    IDATA count = hythread_iterator_size(iterator);

    IDATA java_thread_count = 0;
    for (IDATA i = 0; i < count; i++) {
        hythread_t native_thread = hythread_iterator_next(&iterator);
        vm_thread_t vm_thread = jthread_get_vm_thread(native_thread);
        if (vm_thread) {
            java_thread_count++;
        }
    }
    *count_ptr = (jint)java_thread_count;
    IDATA status = hythread_iterator_release(&iterator);

    return status;
} // jthread_get_thread_count

/**
 * Returns the number of blocked threads.
 *
 * @param[out] count_ptr number of threads.
 */
IDATA VMCALL jthread_get_blocked_count(jint * count_ptr)
{
    assert(count_ptr);

    hythread_group_t java_thread_group = get_java_thread_group();
    assert(java_thread_group);
    hythread_iterator_t iterator = hythread_iterator_create(java_thread_group);
    IDATA count = hythread_iterator_size(iterator);

    IDATA thread_count = 0;
    for (IDATA i = 0; i < count; i++) {
        hythread_t native_thread = hythread_iterator_next(&iterator);
        if (native_thread
            && hythread_is_blocked_on_monitor_enter(native_thread))
        {
            thread_count++;
        }
    }
    *count_ptr = (jint)thread_count;
    IDATA status = hythread_iterator_release(&iterator);

    return status;
} // jthread_get_blocked_count

/**
 * Returns the number of waiting threads.
 *
 * @param[out] count number of threads.
 */
IDATA VMCALL jthread_get_waited_count(jint * count_ptr)
{
    assert(count_ptr);

    hythread_group_t java_thread_group = get_java_thread_group();
    assert(java_thread_group);
    hythread_iterator_t iterator = hythread_iterator_create(java_thread_group);
    IDATA count = hythread_iterator_size(iterator);

    IDATA thread_count = 0;
    for (IDATA i = 0; i < count; i++) {
        hythread_t native_thread = hythread_iterator_next(&iterator);
        if (native_thread
            && hythread_is_waiting(native_thread))
        {
            thread_count++;
        }
    }
    *count_ptr = (jint)thread_count;
    IDATA status = hythread_iterator_release(&iterator);

    return status;
} // jthread_get_waited_count

/**
 * Returns the <code>thread</code>'s state according
 * to JVMTI specification. See <a href=http://java.sun.com/j2se/1.5.0/docs/guide/jvmti/jvmti.html#GetThreadState> 
 * JVMTI Specification </a> for more details.
 *
 * @param[in] java_thread thread those state is to be queried
 * @param[out] state resulting thread state
 * 
 */
IDATA VMCALL jthread_get_jvmti_state(jthread java_thread, jint * state)
{
    assert(state);
    assert(java_thread);

    hythread_t native_thread = jthread_get_native_thread(java_thread);
    vm_thread_t vm_thread = jthread_get_vm_thread(native_thread);
    *state = 0;
    if (!native_thread) {
        // Not started yet
        return TM_ERROR_NONE;
    }

    if (hythread_is_alive(native_thread)) {
        *state |= JVMTI_THREAD_STATE_ALIVE;
    }
    if (hythread_is_runnable(native_thread)) {
        *state |= JVMTI_THREAD_STATE_RUNNABLE;
    }
    if (hythread_is_blocked_on_monitor_enter(native_thread)) {
        *state |= JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER;
    }
    if (hythread_is_waiting(native_thread)) {
        *state |= JVMTI_THREAD_STATE_WAITING;
    }
    if (hythread_is_waiting_indefinitely(native_thread)) {
        *state |= JVMTI_THREAD_STATE_WAITING_INDEFINITELY;
    }
    if (hythread_is_waiting_with_timeout(native_thread)) {
        *state |= JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT;
    }
    if (hythread_is_sleeping(native_thread)) {
        *state |= JVMTI_THREAD_STATE_SLEEPING;
    }
    if (hythread_is_in_monitor_wait(native_thread)) {
        *state |= JVMTI_THREAD_STATE_IN_OBJECT_WAIT;
    }
    if (hythread_is_parked(native_thread)) {
        *state |= JVMTI_THREAD_STATE_PARKED;
    }
    if (hythread_interrupted(native_thread)) {
        *state |= JVMTI_THREAD_STATE_INTERRUPTED;
    }
    if (hythread_is_in_native(native_thread)) {
        *state |= JVMTI_THREAD_STATE_IN_NATIVE;
    }
    if (hythread_is_terminated(native_thread)) {
        *state |= JVMTI_THREAD_STATE_TERMINATED;
    }
    if (vm_thread && vm_thread->suspend_flag) {
        *state |= JVMTI_THREAD_STATE_SUSPENDED;
    }

    return TM_ERROR_NONE;
} // jthread_get_jvmti_state

/**
 * Returns true if specified thread holds the lock associated with the given monitor.
 *
 * @param[in] thread thread which may hold the lock
 * @param[in] monitor object those monitor is possibly locked
 * @return true if thread holds the lock, false otherwise;
 */
jboolean VMCALL jthread_holds_lock(jthread thread, jobject monitor)
{
    jthread lock_owner;
    IDATA status = jthread_get_lock_owner(monitor, &lock_owner);
    assert(status == TM_ERROR_NONE);

    hythread_suspend_disable();
    jboolean result = vm_objects_are_equal(thread, lock_owner);
    hythread_suspend_enable();

    return result;
} // jthread_holds_lock

/**
 * Returns the monitor the specific thread is currently contending for.
 *
 * @param[in] java_thread thread to be explored for contention
 * @param[out] monitor monitor the thread <code>thread</code> is currently contending for, 
 * or NULL if thread doesn't contend for any monitor.
 */
IDATA VMCALL
jthread_get_contended_monitor(jthread java_thread, jobject * monitor)
{
    assert(java_thread);
    assert(monitor);
    *monitor = NULL;
    vm_thread_t vm_thread = jthread_get_vm_thread_from_java(java_thread);
    if (!vm_thread) {
        return TM_ERROR_NONE;
    }
    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;
    if (jvmti_thread) {
        *monitor = jvmti_thread->contended_monitor;
    }
    return TM_ERROR_NONE;
} // jthread_get_contended_monitor

/**
 * Returns the monitor the specific thread is currently waiting on.
 *
 * @param[in] java_thread thread to be explored for contention
 * @param[out] monitor monitor the thread <code>thread</code> is currently contending for, 
 * or NULL if thread doesn't contend for any monitor.
 */
IDATA VMCALL jthread_get_wait_monitor(jthread java_thread, jobject * monitor)
{
    assert(java_thread);
    assert(monitor);
    *monitor = NULL;
    vm_thread_t vm_thread = jthread_get_vm_thread_from_java(java_thread);
    if (!vm_thread) {
        return TM_ERROR_NONE;
    }
    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;
    if (jvmti_thread) {
        *monitor = jvmti_thread->wait_monitor;
    }
    return TM_ERROR_NONE;
} // jthread_get_wait_monitor

/**
 * Returns the owner of the lock associated with the given monitor.
 *
 * If the given monitor is not owned by any thread, NULL is returned.
 *
 * @param[in] monitor monitor those owner needs to be determined
 * @param[out] lock_owner thread which owns the monitor
 */
IDATA VMCALL jthread_get_lock_owner(jobject monitor, jthread * lock_owner)
{
    assert(monitor);
    assert(lock_owner);

    *lock_owner = NULL;
    IDATA status = TM_ERROR_NONE;

    hythread_suspend_disable();
    hythread_thin_monitor_t *lockword = vm_object_get_lockword_addr(monitor);
    hythread_t native_thread = hythread_thin_monitor_get_owner(lockword);
    if (native_thread) {
        vm_thread_t vm_thread = jthread_get_vm_thread(native_thread);
        if (vm_thread) {
            *lock_owner = vm_thread->java_thread;
        } else {
            status = TM_ERROR_ILLEGAL_STATE;
        }
    }
    hythread_suspend_enable();

    return status;
} // jthread_get_lock_owner

/**
 * Returns the number of times given thread have entered given monitor;
 *
 * If the given monitor is not owned by this thread, 0 is returned.
 *
 * @param[in] monitor monitor those owner needs to be determined
 * @param[in] owner thread which owns the monitor
 */
IDATA VMCALL jthread_get_lock_recursion(jobject monitor, jthread owner)
{
    assert(monitor);

    hythread_t given_thread = owner ? jthread_get_native_thread(owner) : NULL;

    hythread_suspend_disable();
    hythread_thin_monitor_t *lockword = vm_object_get_lockword_addr(monitor);
    hythread_t lock_owner = hythread_thin_monitor_get_owner(lockword);

    IDATA recursion = 0;
    if (lock_owner
        && (!given_thread
            || hythread_get_id(lock_owner) == hythread_get_id(given_thread)))
    {
        recursion = hythread_thin_monitor_get_recursion(lockword);
    }
    hythread_suspend_enable();

    return recursion;
} // jthread_get_lock_recursion

/**
 * Returns all monitors owned by the specific thread.
 *
 * @param[in] java_thread thread which owns monitors
 * @param[out] monitor_count_ptr number of owned monitors 
 * @param[out] monitors_ptr array of owned monitors
 */
IDATA VMCALL
jthread_get_owned_monitors(jthread java_thread,
                           jint *monitor_count_ptr,
                           jobject **monitors_ptr)
{
    assert(java_thread);
    assert(monitors_ptr);
    assert(monitor_count_ptr);

    IDATA status = hythread_global_lock();
    if (status != TM_ERROR_NONE) {
        return status;
    }
    vm_thread_t vm_thread = jthread_get_vm_thread_from_java(java_thread);
    if (!vm_thread) {
        status = hythread_global_unlock();
        return status;
    }
    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;
    if (!jvmti_thread)
	{
        status = hythread_global_unlock();
        return status;
	}

    jobject *monitors =
        (jobject *) malloc(sizeof(jobject *) *
                           jvmti_thread->owned_monitors_nmb);
    if (!monitors) {
        hythread_global_unlock();
        return TM_ERROR_OUT_OF_MEMORY;
    }

    tmn_suspend_disable();
    for (int i = 0; i < jvmti_thread->owned_monitors_nmb; i++) {
        jobject new_ref = oh_allocate_local_handle_from_jni();

        if (NULL != new_ref)
            new_ref->object = jvmti_thread->owned_monitors[i]->object;
        else
        {
            tmn_suspend_enable();   
            hythread_global_unlock();
            return TM_ERROR_OUT_OF_MEMORY;
        }
        // change the order of reported monitors to be compliant with RI
        monitors[jvmti_thread->owned_monitors_nmb - 1 - i] = new_ref;
    }
    tmn_suspend_enable();   

    *monitors_ptr = monitors;
    *monitor_count_ptr = jvmti_thread->owned_monitors_nmb;

    status = hythread_global_unlock();
    return status;
} // jthread_get_owned_monitors
