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

#ifndef _VM_THREADS_H_
#define _VM_THREADS_H_

#ifdef PLATFORM_POSIX
#include <semaphore.h>
#include "platform_lowlevel.h"
#else
#include "vm_process.h"
#endif

#include <apr_pools.h>

#include "open/types.h"
#include "open/hythread.h"
#include "open/hythread_ext.h"
#include "open/vm_gc.h"

#include "ti_thread.h"
#include "jthread.h"
#include "thread_manager.h"
#include "jvmti.h"
#include "jvmti_direct.h"
#include "jni_direct.h"
#include "vm_core_types.h"
#include "object_layout.h"

#define tmn_suspend_disable assert(hythread_is_suspend_enabled());hythread_suspend_disable
#define tmn_suspend_enable assert(!hythread_is_suspend_enabled());hythread_suspend_enable
#define tmn_suspend_disable_recursive hythread_suspend_disable
#define tmn_suspend_enable_recursive hythread_suspend_enable

typedef vm_thread_t (*vm_thread_accessor)();
extern VMEXPORT vm_thread_accessor get_thread_ptr;

#define p_TLS_vmthread (jthread_self_vm_thread())

/**
 * Gets jvmti_thread pointer from native thread
 */
inline jvmti_thread_t jthread_self_jvmti()
{
    register vm_thread_t vm_thread = jthread_self_vm_thread();
    return vm_thread ? &(vm_thread->jvmti_thread) : NULL;
} // jthread_self_jvmti

/**
 * Gets jvmti_thread pointer from a given native thread
 */
inline jvmti_thread_t jthread_get_jvmti_thread(hythread_t native)
{
    assert(native);
    register vm_thread_t vm_thread = jthread_get_vm_thread(native);
    return vm_thread ? &(vm_thread->jvmti_thread) : NULL;
} // jthread_get_jvmti_thread

/**
 *  Auxiliary function to update thread count
 */
void jthread_start_count();
void jthread_end_count();

jint jthread_allocate_vm_thread_pool(JavaVM * java_vm, vm_thread_t vm_thread);
void jthread_deallocate_vm_thread_pool(vm_thread_t vm_thread);
vm_thread_t jthread_allocate_thread();
void vm_set_jvmti_saved_exception_registers(vm_thread_t vm_thread, Registers* regs);
void vm_set_exception_registers(vm_thread_t vm_thread, Registers & regs);
void *vm_get_ip_from_regs(vm_thread_t vm_thread);
void vm_reset_ip_from_regs(vm_thread_t vm_thread);

/**
 * @param[in] obj - jobject those address needs to be given
 *
 * @return The address of the memory chunk in the object which can be used by the
 *         Thread Manager for synchronization purposes.
 */
hythread_thin_monitor_t * vm_object_get_lockword_addr(jobject obj);

/**
 * @return The size of the memory chunk in the object that can be used by
 *         Thread Manager for synchronization purposes.
 *
 * The returned size must be equal for all Java objets and is constant over time.
 * It should be possible to call this method during initialization time.
 */
size_t vm_object_get_lockword_size();

/**
 * Creates exception object using given class name and message and throws it
 * using <code>jthread_throw_exception</code> method.
 *
 * @param[in] name     - char* name
 * @param[in] message  - char* message
 *
 * @return <code>int</code>
 */
IDATA jthread_throw_exception(const char* name, const char* message);

/**
 * Throws given exception object. Desides whether current thread is unwindable
 * and throws it, raises exception otherwise.
 */
IDATA jthread_throw_exception_object(jobject object);

/**
 * <code>ti</code> is enabled
 *
 * @return <code>int</code>
 */
int ti_is_enabled();

#endif //!_VM_THREADS_H_
