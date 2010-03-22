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
#define LOG_DOMAIN "vmcore.thread"
#include "cxxlog.h"
#include "platform_lowlevel.h"

#ifndef PLATFORM_POSIX
#include "vm_process.h"
#endif

#include "object_layout.h"
#include "object_handles.h"
//FIXME remove this code
#include "exceptions.h"
#include "Class.h"
#include "environment.h"
#include "vtable.h"
#include "nogc.h"
#include "sync_bits.h"

#include "lock_manager.h"
#include "thread_manager.h"
#include "thread_generic.h"
#include "thread_helpers.h"
#include "jthread.h"

#include "vm_threads.h"
#include "tl/memory_pool.h"
#include "suspend_checker.h"
#include "jni_utils.h"
#include "heap.h"
#include "vm_strings.h"
#include "interpreter.h"
#include "exceptions_int.h"

#ifdef _IPF_
#include "java_lang_thread_ipf.h"
#elif defined _EM64T_
//#include "java_lang_thread_em64t.h"
#else
#include "java_lang_thread_ia32.h"
#endif



jint jthread_allocate_vm_thread_pool(JavaVM *java_vm,
                                     vm_thread_t vm_thread)
{
    assert(java_vm);
    assert(vm_thread);

    apr_pool_t *thread_pool;
    if (apr_pool_create(&thread_pool,
            ((JavaVM_Internal*)java_vm)->vm_env->mem_pool) != APR_SUCCESS)
    {
        return JNI_ENOMEM;
    }
    vm_thread->pool = thread_pool;

    return JNI_OK;
}

void jthread_deallocate_vm_thread_pool(vm_thread_t vm_thread)
{
    assert(vm_thread);
    
    // Destroy current VM_thread pool.
    apr_pool_destroy(vm_thread->pool);

    // resume thread if it was suspended
    // dead thread resume has no affect
    Lock_Manager *suspend_lock = VM_Global_State::loader_env->p_suspend_lock;
    suspend_lock->_lock();
    if (vm_thread->suspend_flag) {
        hythread_resume((hythread_t)vm_thread);
    }
    suspend_lock->_unlock();

    // zero VM_thread structure
    memset(&vm_thread->java_thread, 0,
        sizeof(VM_thread) - offsetof(VM_thread, java_thread));
}

vm_thread_t jthread_get_vm_thread_ptr_safe(jobject thread_obj)
{
    hythread_t native = jthread_get_native_thread(thread_obj);
    return jthread_get_vm_thread(native);
}

vm_thread_t jthread_get_vm_thread_ptr_stub()
{
    return jthread_self_vm_thread();
}

vm_thread_accessor get_thread_ptr = jthread_get_vm_thread_ptr_stub;

IDATA jthread_throw_exception(const char *name, const char *message)
{
    assert(hythread_is_suspend_enabled());
    jobject jthe = exn_create(name);
    return jthread_throw_exception_object(jthe);
}

IDATA jthread_throw_exception_object(jobject object)
{
    if (interpreter_enabled()) {
        // FIXME - Function set_current_thread_exception does the same
        // actions as exn_raise_object, and it should be replaced.
        hythread_suspend_disable();
        set_current_thread_exception(object->object);
        hythread_suspend_enable();
    } else {
        if (is_unwindable()) {
            exn_throw_object(object);
        } else {
            ASSERT_RAISE_AREA;
            exn_raise_object(object);
        }
    }

    return 0;
}

/**
 * Allocates VM_thread structure
 */
vm_thread_t jthread_allocate_thread()
{
    vm_thread_t vm_thread =
            (vm_thread_t)STD_CALLOC(1, sizeof(struct VM_thread));
    assert(vm_thread);
    ((hythread_t)vm_thread)->java_status = TM_STATUS_ALLOCATED;
    return vm_thread;
} // jthread_allocate_thread

/**
 * Sets resisters to JVMTI thread
 */
void vm_set_jvmti_saved_exception_registers(vm_thread_t vm_thread,
                                            Registers* regs)
{
    assert(vm_thread);
    jvmti_thread_t jvmti_thread = &vm_thread->jvmti_thread;
    if (!jvmti_thread->jvmti_saved_exception_registers) {
        jvmti_thread->jvmti_saved_exception_registers =
            (Registers*)STD_MALLOC(sizeof(Registers));
        assert(jvmti_thread->jvmti_saved_exception_registers);
    }
    *(jvmti_thread->jvmti_saved_exception_registers) = *regs;
} // vm_set_jvmti_saved_exception_registers

/**
 * Sets exception registers
 */
void vm_set_exception_registers(vm_thread_t vm_thread, Registers & regs)
{
    assert(vm_thread);
    if (!vm_thread->regs) {
        vm_thread->regs = (Registers*)malloc(sizeof(Registers));
        assert(vm_thread->regs);
    }
    *(vm_thread->regs) = regs;
} // vm_set_exception_registers

/**
 * Gets IP from exception registers for current thread
 */
void *vm_get_ip_from_regs(vm_thread_t vm_thread)
{
    assert(vm_thread);
    assert(vm_thread->regs);
    return vm_thread->regs->get_ip();
} // vm_get_ip_from_regs

/**
 * Resets IP in exception registers
 */
void vm_reset_ip_from_regs(vm_thread_t vm_thread)
{
    assert(vm_thread);
    vm_thread->regs->reset_ip();
} // vm_reset_ip_from_regs

/**
 * This file contains the functions which eventually should become part of vmcore.
 * This localizes the dependencies of Thread Manager on vmcore component.
 */

hythread_thin_monitor_t *vm_object_get_lockword_addr(jobject monitor)
{
    assert(monitor);
    return (hythread_thin_monitor_t *) (*(ManagedObject **) monitor)->
        get_obj_info_addr();
}

extern "C" char *vm_get_object_class_name(void *ptr)
{
    return (char *) (((ManagedObject *) ptr)->vt()->clss->get_name()->bytes);
}

void* jthread_get_tm_data(jobject thread)
{
    static int offset = -1;

    hythread_suspend_disable();

    ManagedObject *thread_obj = ((ObjectHandle) thread)->object;
    if (offset == -1) {
        Class *clazz = thread_obj->vt()->clss;
        Field *field = class_lookup_field_recursive(clazz, "vm_thread", "J");
        offset = field->get_offset();
    }
    U_8* java_ref = (U_8*)thread_obj;
    void** val = (void**)(java_ref + offset);

    hythread_suspend_enable();

    return *val;
} // jthread_get_tm_data

void jthread_set_tm_data(jobject thread, void *val)
{
    static unsigned offset = (unsigned)-1;

    hythread_suspend_disable();

    ManagedObject *thread_obj = ((ObjectHandle) thread)->object;
    if (offset == -1) {
        Class *clazz = thread_obj->vt()->clss;
        Field *field = class_lookup_field_recursive(clazz, "vm_thread", "J");
        offset = field->get_offset();
    }
    U_8* java_ref = (U_8*)thread_obj;
    *(jlong*)(java_ref + offset) = (jlong) (POINTER_SIZE_INT) val;

    hythread_suspend_enable();
} // jthread_set_tm_data

int vm_objects_are_equal(jobject obj1, jobject obj2)
{
    //ObjectHandle h1 = (ObjectHandle)obj1;
    //ObjectHandle h2 = (ObjectHandle)obj2;
    if (obj1 == NULL && obj2 == NULL) {
        return 1;
    }
    if (obj1 == NULL || obj2 == NULL) {
        return 0;
    }
    return obj1->object == obj2->object;
}

int ti_is_enabled()
{
    return VM_Global_State::loader_env->TI->isEnabled();
}

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/*
 * Class:     org_apache_harmony_drlvm_thread_ThreadHelper
 * Method:    getThreadIdOffset
 * Signature: ()I
 */
VMEXPORT jint JNICALL
Java_org_apache_harmony_drlvm_thread_ThreadHelper_getThreadIdOffset(JNIEnv *env, jclass klass)
{
    return (jint)hythread_get_thread_id_offset();
}


/*
 * Class:     org_apache_harmony_drlvm_thread_ThreadHelper
 * Method:    getLockWordOffset
 * Signature: ()I
 */
VMEXPORT jint JNICALL
Java_org_apache_harmony_drlvm_thread_ThreadHelper_getLockWordOffset(JNIEnv *env, jclass klass)
{
	unsigned offset = ManagedObject::header_offset();
	return (jint)offset;
}

/*
 * Class:     org_apache_harmony_drlvm_thread_ThreadHelper
 * Method:    getThreadJavaObjectOffset
 * Signature: ()I
 */
VMEXPORT jint JNICALL
Java_org_apache_harmony_drlvm_thread_ThreadHelper_getThreadJavaObjectOffset(JNIEnv *env, jclass klass)
{
    vm_thread_t vm_thread = NULL;
    return (jint)(POINTER_SIZE_INT)&vm_thread->java_thread;
}

#ifdef __cplusplus
}
#endif /* __cplusplus */
