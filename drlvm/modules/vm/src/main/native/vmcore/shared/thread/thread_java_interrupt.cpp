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
 * @file thread_java_interrupt.c
 * @brief Java thread interruption related functions
 */

#include <open/hythread_ext.h>
#include "jthread.h"
#include "vm_threads.h"

/**
 * Interrupt a <code>thread</code>.
 *
 * If the <code>thread</code>is currently blocked (i.e. waiting on a monitor_wait or sleeping)
 * resume the <code>thread</code>and cause it to return from the blocking function with
 * TM_THREAD_INTERRUPTED.
 *
 * @param[in] java_thread a thread to be interrupted
 * @sa java.lang.Thread.interrupt()
 */
IDATA VMCALL jthread_interrupt(jthread java_thread)
{
    hythread_t tm_native_thread = jthread_get_native_thread(java_thread);
    hythread_interrupt(tm_native_thread);
    return TM_ERROR_NONE;
} // jthread_interrupt

/**
 * Returns <code>true</code> if the thread <code>thread</code> is interrupted.
 *
 * Otherwise returns <code>false</code>.
 *
 * @param[in] java_thread thread to be checked
 * @return <code>true</code> if the thread <code>thread</code>
 * is interrupted; <code>false</code> otherwise.
 * @sa java.lang.Thread.isInterrupted()
 */
jboolean jthread_is_interrupted(jthread java_thread)
{
    hythread_t tm_native_thread = jthread_get_native_thread(java_thread);
    return hythread_interrupted(tm_native_thread) > 0;
} // jthread_is_interrupted

/**
 * Clears the interruption flag for the specific <code>thread</code>.
 *
 * @param[in] java_thread where to clear interrupt flag
 * @sa java.lang.Thread.interrupted()
 */
IDATA VMCALL jthread_clear_interrupted(jthread java_thread)
{
    hythread_t tm_native_thread = jthread_get_native_thread(java_thread);
    return hythread_clear_interrupted_other(tm_native_thread);
} // jthread_clear_interrupted
