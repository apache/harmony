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
 * @file thread_java_attrs.c
 * @brief Java thread priority related functions
 */

#include <open/hythread_ext.h>
#include "jthread.h"
#include "vm_threads.h"

/**
 * Returns the priority for the <code>thread</code>.
 *
 * @param[in] java_thread thread those attribute is read
 * @sa java.lang.Thread.getPriority()
 */
jint jthread_get_priority(jthread java_thread)
{
    hythread_t tm_native_thread = jthread_get_native_thread(java_thread);
    return (jint)hythread_get_priority(tm_native_thread);
}

/**
 * Sets the priority for the <code>thread</code>.
 *
 * @param[in] java_thread thread those attribute is set
 * @param[in] priority thread priority
 * @sa java.lang.Thread.setPriority()
 */
IDATA VMCALL jthread_set_priority(jthread java_thread, jint priority)
{
    hythread_t tm_native_thread = jthread_get_native_thread(java_thread);
    return hythread_set_priority(tm_native_thread, priority);
}

/**
 * Returns daemon status for the specified thread.
 *
 * @param[in] thread thread those attribute is read
 */
