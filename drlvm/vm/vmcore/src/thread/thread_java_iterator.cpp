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
 * @file thread_java_iterator.c
 * @brief Java thread iterator related functions
 */

#include "open/hythread_ext.h"
#include "jthread.h"
#include "ti_thread.h"
#include "vm_threads.h"

/**
 * Creates the iterator that can be used to walk over java threads.
 */
jthread_iterator_t VMCALL jthread_iterator_create(void)
{
    hythread_group_t java_thread_group = get_java_thread_group();
    return (jthread_iterator_t)
        hythread_iterator_create(java_thread_group);
} // jthread_iterator_create

/**
 * Releases the iterator.
 * 
 * @param[in] it iterator
 */
IDATA VMCALL jthread_iterator_release(jthread_iterator_t * it)
{
    return hythread_iterator_release((hythread_iterator_t *) it);
} // jthread_iterator_release

/**
 * Resets the iterator such that it will start from the beginning.
 * 
 * @param[in] it iterator
 */
IDATA VMCALL jthread_iterator_reset(jthread_iterator_t * it)
{
    return hythread_iterator_reset((hythread_iterator_t *) it);
} // jthread_iterator_reset

/**
 * Returns the next jthread using the given iterator.
 * 
 * @param[in] it iterator
 */
jobject VMCALL jthread_iterator_next(jthread_iterator_t * it)
{
    hythread_t native_thread = hythread_iterator_next((hythread_iterator_t *) it);
    while (native_thread != NULL) {
        if (hythread_is_alive(native_thread)) {
            vm_thread_t vm_thread = jthread_get_vm_thread(native_thread);
            if (vm_thread) {
                return vm_thread->java_thread;
            }
        }
        native_thread = hythread_iterator_next((hythread_iterator_t *) it);
    }
    return NULL;
} // jthread_iterator_next

/**
 * Returns the the number of Java threads.
 * 
 * @param[in] iterator
 */
IDATA VMCALL jthread_iterator_size(jthread_iterator_t iterator)
{
    IDATA status = jthread_iterator_reset(&iterator);
    assert(status == TM_ERROR_NONE);
    jthread thread = jthread_iterator_next(&iterator);
    int count = 0;
    while (thread != NULL) {
        count++;
        thread = jthread_iterator_next(&iterator);
    }
    status = jthread_iterator_reset(&iterator);
    assert(status == TM_ERROR_NONE);
    return count;
} // jthread_iterator_size
