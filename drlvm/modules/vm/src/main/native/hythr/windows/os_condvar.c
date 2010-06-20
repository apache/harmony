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
 * @file os_condvar.c
 * @brief Custom queue-based condition variable implementation.
 * @detailed Custom implementation is needed on Windows, because it lacks
 *      the suitable condition variable synchronization object.
 */

#include <open/hythread_ext.h>
#include "port_mutex.h"
#include "thread_private.h"

static void _enqueue (hycond_t *cond, struct waiting_node *node)
{
     node->next = &cond->dummy_node;
     node->prev = cond->dummy_node.prev;
     node->prev->next = node;
     cond->dummy_node.prev = node;
}

static int _remove_from_queue (hycond_t *cond, struct waiting_node *node)
{
    if (node->next == NULL || node->prev == NULL) {
        // already dequeued (by signal)
        return -1;
    }
    node->prev->next = node->next;
    node->next->prev = node->prev;
    node->next = NULL;
    node->prev = NULL;
    return 0;
}

// returns NULL if queue is empty
static struct waiting_node * _dequeue (hycond_t *cond)
{
    struct waiting_node *node;
    if (cond->dummy_node.next == &cond->dummy_node) {
        // the queue is empty
        return NULL;
    }
    node = cond->dummy_node.next;
    _remove_from_queue(cond,node);
    return node;
}

/**
 * waits on a condition variable, directly using OS interfaces.
 *
 * This function does not implement interruptability and thread state
 * functionality, thus the caller of this function have to handle it.
 */
int os_cond_timedwait(hycond_t *cond, osmutex_t *mutex, I_64 ms, IDATA nano)
{
    int r = 0;
    struct waiting_node node;
    DWORD res;
    DWORD timeout;
    if (!ms && !nano) {
        timeout = INFINITE;
    } else {
        timeout = (DWORD)ms + (nano ? 1:0);
    }

    // NULL attributes, manual reset, initially unsignalled, NULL name
    node.event = CreateEvent(NULL, TRUE, FALSE, NULL);
    port_mutex_lock(&cond->queue_mutex);
    _enqueue(cond, &node);
    port_mutex_unlock(&cond->queue_mutex);

    // release mutex and wait for signal
    port_mutex_unlock(mutex);

    res = WaitForSingleObject(node.event, timeout);
    if (res != WAIT_OBJECT_0) {
        if (res == WAIT_TIMEOUT)
            r = TM_ERROR_TIMEOUT;
        else
            r = (int)GetLastError();
    }

    // re-acquire mutex associated with condition variable
    port_mutex_lock(mutex);

    port_mutex_lock(&cond->queue_mutex);
    _remove_from_queue(cond, &node);
    CloseHandle(node.event);
    port_mutex_unlock(&cond->queue_mutex);

    return r;
}

/** @name Conditional variable
 */
//@{

/**
 * Creates and initializes condition variable.
 *
 * @param[in] cond the address of the condition variable.
 * @return 0 on success, non-zero otherwise.
 */
IDATA VMCALL hycond_create (hycond_t *cond) {
    cond->dummy_node.next = cond->dummy_node.prev = &cond->dummy_node;
    port_mutex_create(&cond->queue_mutex, APR_THREAD_MUTEX_NESTED);
    return 0;
}

/**
 * Signals a single thread that is blocking on the given condition variable to
 * wake up.
 *
 * @param[in] cond the condition variable on which to produce the signal.
 * @sa apr_thread_cond_signal()
 */
IDATA VMCALL hycond_notify (hycond_t *cond) {
    int r = 0;
    DWORD res;
    struct waiting_node *node;

    port_mutex_lock(&cond->queue_mutex);
    node = _dequeue(cond);
    if (node != NULL) {
        res = SetEvent(node->event);
        if (res == 0) {
             r = (int)GetLastError();
        }
    }
    port_mutex_unlock(&cond->queue_mutex);
    return r;
}

/**
 * Signals all threads blocking on the given condition variable.
 *
 * @param[in] cond the condition variable on which to produce the broadcast.
 * @sa apr_thread_cond_broadcast()
 */
IDATA VMCALL hycond_notify_all (hycond_t *cond) {
    int r = 0;
    DWORD res;
    struct waiting_node *node;

    port_mutex_lock(&cond->queue_mutex);
    for (node = _dequeue(cond); node != NULL; node = _dequeue(cond)) {
        res = SetEvent(node->event);
        if (res == 0) {
            r = GetLastError();
        }
    }
    port_mutex_unlock(&cond->queue_mutex);
    return r;
}

/**
 * Destroys the condition variable and releases the associated memory.
 *
 * @param[in] cond the condition variable to destroy
 * @sa apr_thread_cond_destroy()
 */
IDATA VMCALL hycond_destroy (hycond_t *cond) {
    assert(cond->dummy_node.next == &cond->dummy_node
            && "destroying condition variable with active waiters");
    return port_mutex_destroy(&cond->queue_mutex);
}

//@}
