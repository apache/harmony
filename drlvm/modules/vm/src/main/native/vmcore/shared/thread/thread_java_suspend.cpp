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
 * @file thread_java_suspend.c
 * @brief Java thread suspend/resume functions
 */

#define LOG_DOMAIN "tm.suspend"
#include "clog.h"

#include "open/hythread_ext.h"

#include "jthread.h"
#include "vm_threads.h"
#include "environment.h"

/**
 * Resumes the suspended thread <code>thread</code> execution.
 *
 * This function cancels the effect of all previous
 * <code>thread_suspend(thread)</code> calls such that
 * the thread <code>thread</code> may proceed with execution.
 *
 * @param[in] java_thread thread to be resumed
 * @sa java.lang.Thread.resume(), JVMTI::ResumeThread()
 */
IDATA VMCALL jthread_resume(jobject java_thread)
{
    assert(java_thread);

    vm_thread_t vm_thread = jthread_get_vm_thread_from_java(java_thread);
    if (!vm_thread || vm_thread->suspend_flag == 0) {
        // it is a fast path
        // thread is not alive or is not suspended, nothing to do

        CTRACE(("java resume exit, self: %p, thread: %p",
            hythread_self(), vm_thread));

        return TM_ERROR_NONE;
    }

    CTRACE(("java resume enter, self: %p, thread: %p, "
        "suspend_flag: %d", hythread_self(), vm_thread,
        vm_thread->suspend_flag));


    Lock_Manager *suspend_lock = VM_Global_State::loader_env->p_suspend_lock;
    suspend_lock->_lock();

    CTRACE(("java resume lock enter, self: %p, thread: %p, "
        "suspend_flag: %d", hythread_self(), vm_thread,
        vm_thread->suspend_flag));

    vm_thread = jthread_get_vm_thread_from_java(java_thread);
    if (!vm_thread || vm_thread->suspend_flag == 0) {
        // thread is not alive or is already suspended,
        // release the lock and exit

        CTRACE(("java resume exit, self: %p, thread: %p",
            hythread_self(), vm_thread));

        suspend_lock->_unlock();
        return TM_ERROR_NONE;
    }

    hythread_t hy_thread = (hythread_t)vm_thread;
    if (vm_thread->suspend_flag != 0) {
        hythread_resume(hy_thread);
        vm_thread->suspend_flag = 0;
    }

    CTRACE(("java resume lock exit, self: %p, thread: %p, "
        "suspend_flag: %d", hythread_self(), vm_thread,
        vm_thread->suspend_flag));

    suspend_lock->_unlock();

    CTRACE(("java resume exit, self: %p, thread: %p, "
        "suspend_flag: %d", hythread_self(), vm_thread,
        vm_thread->suspend_flag));

    return TM_ERROR_NONE;
} // jthread_resume

/**
 * Resumes the suspended threads from the list.
 *
 * @param[out] results list of error codes for resume result
 * @param[in] count number of threads in the list
 * @param[in] thread_list list of threads to be resumed
 * @sa JVMTI::ResumeThreadList()
 */
IDATA VMCALL
jthread_resume_all(jvmtiError *results,
                   jint count,
                   const jobject *thread_list)
{
    assert(results);
    assert(thread_list);

    if (!count) {
        return TM_ERROR_NONE;
    }

    CTRACE(("java resume all"));

    Lock_Manager *suspend_lock = VM_Global_State::loader_env->p_suspend_lock;
    suspend_lock->_lock();

    for (jint i = 0; i < count; i++) {
        results[i] = (jvmtiError)jthread_resume(thread_list[i]);
    }

    suspend_lock->_unlock();
    return TM_ERROR_NONE;
} // jthread_resume_all

/**
 * Suspends the <code>thread</code> execution.
 *
 * The execution of <code>java_thread</code> is suspended till
 * <code>jthread_resume(java_thread)</code> is called from another thread.
 *
 * Note: this function implements safe suspend based on the safe points and
 * regions. This means that:
 * <ul>
 *   <li>If the thread is in Java code,
 *        it runs till the safe point and puts itself into a wait state.
 *   <li>If the thread is running native code, it runs till
 *        the end of safe region and then puts itself into a wait state,
 * </ul>
 *
 * @param[in] java_thread thread to be suspended
 * @sa java.lang.Thread.suspend(), JVMTI::SuspendThread()
 */
IDATA VMCALL jthread_suspend(jobject java_thread)
{
    IDATA status;
    assert(java_thread);

    vm_thread_t vm_thread = jthread_get_vm_thread_from_java(java_thread);
    if (!vm_thread || vm_thread->suspend_flag) {
        // it is a fast path
        // thread is not alive or is already suspended, nothing to do

        CTRACE(("java suspend enter, self: %p, thread: %p",
            hythread_self(), vm_thread));

        return TM_ERROR_NONE;
    }

    CTRACE(("java suspend enter, self: %p, thread: %p, "
           "suspend_flag: %d", hythread_self(), vm_thread,
           vm_thread->suspend_flag));

    Lock_Manager *suspend_lock = VM_Global_State::loader_env->p_suspend_lock;
    suspend_lock->_lock();

    CTRACE(("java suspend lock enter, self: %p, thread: %p, "
           "suspend_flag: %d", hythread_self(), vm_thread,
           vm_thread->suspend_flag));

    vm_thread = jthread_get_vm_thread_from_java(java_thread);
    if (!vm_thread || vm_thread->suspend_flag) {
        // thread is not alive or is already suspended,
        // release the lock and exit

        CTRACE(("java suspend exit, self: %p, thread: %p, ",
            hythread_self(), vm_thread));

        suspend_lock->_unlock();
        return TM_ERROR_NONE;
    }

    hythread_t hy_thread = (hythread_t)vm_thread;
    if (hy_thread == hythread_self()) {
        // suspend self
        hythread_send_suspend_request(hy_thread);

        // set SUSPEND state
        vm_thread->suspend_flag = 1;
        status = hythread_thread_lock(hy_thread);
        assert(status == TM_ERROR_NONE);
        IDATA state = hythread_get_state(hy_thread) | TM_THREAD_STATE_SUSPENDED;
        status = hythread_set_state(hy_thread, state);
        assert(status == TM_ERROR_NONE);
        status = hythread_thread_unlock(hy_thread);
        assert(status == TM_ERROR_NONE);

        CTRACE(("java suspend lock exit, self: %p, thread: %p, "
            "suspend_flag: %d", hythread_self(), vm_thread,
            vm_thread->suspend_flag));

        suspend_lock->_unlock();

        hythread_safe_point();

        suspend_lock->_lock();

        CTRACE(("java suspend lock enter, self: %p, thread: %p, "
            "suspend_flag: %d", hythread_self(), vm_thread,
            vm_thread->suspend_flag));
    } else {
        while ((status = hythread_suspend_other(hy_thread)) != TM_ERROR_NONE) {
            hythread_safe_point();
            hythread_exception_safe_point();
        }
        vm_thread->suspend_flag = 1;

    }
    CTRACE(("java suspend lock exit, self: %p, thread: %p, "
        "suspend_flag: %d", hythread_self(), vm_thread,
        vm_thread->suspend_flag));

    suspend_lock->_unlock();

    CTRACE(("java suspend exit, self: %p, thread: %p, "
        "suspend_flag: %d", hythread_self(), vm_thread,
        vm_thread->suspend_flag));

    return TM_ERROR_NONE;
} // jthread_suspend

/**
 * Suspends the threads from the list.
 *
 * The <code>thread</code> thread's execution is suspended till
 * <code>thread_resume(thread)</code> is called from another thread.
 *
 * Note: this function implements safe suspend based on the safe points and
 * regions. This means that:
 * <ul>
 *   <li>If the thread is in Java code,
 *        it runs till the safe point and puts itself into wait state.
 *   <li>If the thread is running native code, it runs till
 *        the end of safe region and then puts itself into a wait state,
 * </ul>
 *
 * @param[out] results list of error codes for suspension result
 * @param[in] count number of threads in the list
 * @param[in] thread_list list of threads to be suspended
 * @sa JVMTI::SuspendThreadList()
 */
IDATA VMCALL
jthread_suspend_all(jvmtiError *results,
                    jint count,
                    const jobject *thread_list)
{
    assert(results);
    assert(thread_list);

    if (!count) {
        return TM_ERROR_NONE;
    }

    CTRACE(("java suspend all"));

    Lock_Manager *suspend_lock = VM_Global_State::loader_env->p_suspend_lock;
    suspend_lock->_lock();

    for (jint i = 0; i < count; i++) {
        results[i] = (jvmtiError)jthread_suspend(thread_list[i]);
    }

    suspend_lock->_unlock();
    return TM_ERROR_NONE;
} // jthread_suspend_all
