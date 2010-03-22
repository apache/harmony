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
 * @file thread_ti_monitors.c
 * @brief JVMTI raw monitors related functions
 */

#include <open/hythread_ext.h>
#include "port_mutex.h"
#include "vm_threads.h"

typedef struct ResizableArrayEntry *array_entry_t;
typedef struct ResizableArrayType *array_t;

struct ResizableArrayEntry {
    void *entry;
    UDATA next_free;
};

struct ResizableArrayType {
    UDATA size;
    UDATA capacity;
    UDATA next_index;
    array_entry_t entries;
};

/**
 * Resizable array implementation
 */
static IDATA array_create(array_t * arr)
{
    array_t ptr = (array_t) malloc(sizeof(ResizableArrayType));
    if (!ptr) {
        return -1;
    }
    ptr->capacity = 1024;
    ptr->size = 0;
    ptr->next_index = 0;
    ptr->entries =
        (array_entry_t) malloc(sizeof(ResizableArrayEntry) * ptr->capacity);
    if (!ptr->entries) {
        free(ptr);
        return -1;
    }
    *arr = ptr;
    return 0;
} // array_create

static IDATA array_destroy(array_t arr)
{
    if (!arr) {
        return -1;
    }
    free(arr->entries);
    free(arr);
    return 0;
} // array_destroy

static UDATA array_add(array_t arr, void *value)
{
    UDATA index;
    if (!arr) {
        return 0;
    }
    if (arr->next_index) {
        index = arr->next_index;
    } else {
        index = arr->size + 1;
        if (index >= arr->capacity) {
            arr->entries = (array_entry_t)realloc(arr->entries,
                sizeof(void *) * arr->capacity * 2);
            if (!arr->entries)
                return 0;
            arr->capacity *= 2;
        }
        arr->entries[index].next_free = 0;
    }
    arr->next_index = arr->entries[index].next_free;
    arr->entries[index].entry = value;
    arr->size++;

    return index;
} // array_add

static void * array_delete(array_t arr, UDATA index)
{
    void *return_value;
    if (!arr || index > arr->size || index == 0) {
        return NULL;
    }
    return_value = arr->entries[index].entry;

    arr->entries[index].entry = NULL;
    arr->entries[index].next_free = arr->next_index;
    arr->next_index = index;

    return return_value;
} // array_delete

static void * array_get(array_t arr, UDATA index)
{
    if (!arr || index > arr->size || index == 0) {
        return NULL;
    }
    return arr->entries[index].entry;
} // array_get

static array_t jvmti_monitor_table = 0;
static osmutex_t jvmti_monitor_table_lock;

static IDATA jthread_init_jvmti_monitor_table()
{
    IDATA status = hythread_global_lock();
    if (status != TM_ERROR_NONE) {
        return status;
    }
    if (!jvmti_monitor_table) {
        if (array_create(&jvmti_monitor_table)) {
            hythread_global_unlock();
            return TM_ERROR_OUT_OF_MEMORY;
        }
        status = port_mutex_create(&jvmti_monitor_table_lock, APR_THREAD_MUTEX_NESTED);
        if (status != TM_ERROR_NONE) {
            hythread_global_unlock();
            return status;
        }
    }
    status = hythread_global_unlock();
    return status;
} // jthread_init_jvmti_monitor_table

/**
 * Initializes raw monitor. 
 *
 * Raw monitors are a simple combination of mutex and conditional variable which is 
 * not associated with any Java object. This function creates the raw monitor at the 
 * address specified as mon_ptr. 
 * User needs to allocate space equal to sizeof(jrawMonitorID) before doing this call.
 *
 * @param[in] mon_ptr address where monitor needs to be created and initialized.
 */
IDATA VMCALL jthread_raw_monitor_create(jrawMonitorID * mon_ptr)
{
    assert(mon_ptr);

    hythread_monitor_t monitor;
    IDATA status = hythread_monitor_init(&monitor, 0);
    if (status != TM_ERROR_NONE) {
        return status;
    }

    // possibly should be moved to jvmti(environment?) init section
    if (!jvmti_monitor_table) {
        status = jthread_init_jvmti_monitor_table();
        if (status != TM_ERROR_NONE) {
            return status;
        }
    }

    status = port_mutex_lock(&jvmti_monitor_table_lock);
    if (status != TM_ERROR_NONE) {
        return status;
    }
    *mon_ptr = (jrawMonitorID)array_add(jvmti_monitor_table, monitor);
    if (!(*mon_ptr)) {
        port_mutex_unlock(&jvmti_monitor_table_lock);
        return TM_ERROR_OUT_OF_MEMORY;
    }

    status = port_mutex_unlock(&jvmti_monitor_table_lock);
    return status;
} // jthread_raw_monitor_create

/**
 * Destroys raw monitor. 
 *
 * @param[in] mon_ptr address where monitor needs to be destroyed.
 */
IDATA VMCALL jthread_raw_monitor_destroy(jrawMonitorID mon_ptr)
{
    hythread_monitor_t monitor =
         (hythread_monitor_t)array_get(jvmti_monitor_table, (UDATA)mon_ptr);
    if (!monitor) {
        return TM_ERROR_INVALID_MONITOR;
    }

    while (hythread_monitor_destroy((hythread_monitor_t)monitor) != TM_ERROR_NONE)
    {
        IDATA status = hythread_monitor_exit((hythread_monitor_t) monitor);
        if (status != TM_ERROR_NONE) {
            return status;
        }
    }

    IDATA status = port_mutex_lock(&jvmti_monitor_table_lock);
    if (status != TM_ERROR_NONE) {
        return status;
    }
    array_delete(jvmti_monitor_table, (UDATA) mon_ptr);
    status = port_mutex_unlock(&jvmti_monitor_table_lock);
    return status;
} // jthread_raw_monitor_destroy

/**
 * Gains the ownership over monitor.
 *
 * Current thread blocks if the specified monitor is owned by other thread.
 *
 * @param[in] mon_ptr monitor
 */
IDATA VMCALL jthread_raw_monitor_enter(jrawMonitorID mon_ptr)
{
    hythread_monitor_t monitor =
         (hythread_monitor_t)array_get(jvmti_monitor_table, (UDATA)mon_ptr);
    if (!monitor) {
        return TM_ERROR_INVALID_MONITOR;
    }
    IDATA status = hythread_monitor_enter(monitor);
    hythread_safe_point();
    hythread_exception_safe_point();
    return status;
} // jthread_raw_monitor_enter

/**
 * Attempt to gain the ownership over monitor without blocking.
 *
 * @param[in] mon_ptr monitor
 * @return 0 in case of successful attempt.
 */
IDATA VMCALL jthread_raw_monitor_try_enter(jrawMonitorID mon_ptr)
{
    hythread_monitor_t monitor =
         (hythread_monitor_t)array_get(jvmti_monitor_table, (UDATA)mon_ptr);
    if (!monitor) {
        return TM_ERROR_INVALID_MONITOR;
    }
    return hythread_monitor_try_enter((hythread_monitor_t) monitor);
} // jthread_raw_monitor_try_enter

/**
 * Releases the ownership over monitor.
 *
 * @param[in] mon_ptr monitor
 */
IDATA VMCALL jthread_raw_monitor_exit(jrawMonitorID mon_ptr)
{
    hythread_monitor_t monitor =
         (hythread_monitor_t)array_get(jvmti_monitor_table, (UDATA)mon_ptr);
    if (!monitor) {
        return TM_ERROR_INVALID_MONITOR;
    }
    IDATA status = hythread_monitor_exit(monitor);
    hythread_safe_point();
    hythread_exception_safe_point();
    return status;
} // jthread_raw_monitor_exit

/**
 * Wait on the monitor.
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
 * @param[in] mon_ptr monitor
 * @param[in] millis timeout in milliseconds. Zero timeout is not taken into consideration.
 * @return 
 *      TM_ERROR_NONE               success
 *      TM_ERROR_INTERRUPT          wait was interrupted
 *      TM_ERROR_INVALID_MONITOR    current thread isn't the owner
 */
IDATA VMCALL jthread_raw_monitor_wait(jrawMonitorID mon_ptr, I_64 millis)
{
    hythread_monitor_t monitor =
         (hythread_monitor_t)array_get(jvmti_monitor_table, (UDATA)mon_ptr);
    if (!monitor) {
        return TM_ERROR_INVALID_MONITOR;
    }
    // JDWP agent expects RawMonitor waiting thread has RUNNABLE state.
    // That why no Java thread state change is done here.
    // RI behaviour confirms this assumption.
    return hythread_monitor_wait_interruptable(monitor, millis, 0);
} // jthread_raw_monitor_wait

/**
 * Notifies one thread waiting on the monitor.
 *
 * Only single thread waiting on the
 * object's monitor is waked up.
 * Nothing happens if no threads are waiting on the monitor.
 *
 * @param[in] mon_ptr monitor
 */
IDATA VMCALL jthread_raw_monitor_notify(jrawMonitorID mon_ptr)
{
    hythread_monitor_t monitor =
         (hythread_monitor_t)array_get(jvmti_monitor_table, (UDATA)mon_ptr);
    if (!monitor) {
        return TM_ERROR_INVALID_MONITOR;
    }
    return hythread_monitor_notify(monitor);
} // jthread_raw_monitor_notify

/**
 * Notifies all threads which are waiting on the monitor.
 *
 * Each thread from the set of threads waiting on the
 * object's monitor is waked up.
 *
 * @param[in] mon_ptr monitor
 */
IDATA VMCALL jthread_raw_monitor_notify_all(jrawMonitorID mon_ptr)
{
    hythread_monitor_t monitor =
         (hythread_monitor_t)array_get(jvmti_monitor_table, (UDATA)mon_ptr);
    if (!monitor) {
        return TM_ERROR_INVALID_MONITOR;
    }
    return hythread_monitor_notify_all(monitor);
} // jthread_raw_monitor_notify_all
