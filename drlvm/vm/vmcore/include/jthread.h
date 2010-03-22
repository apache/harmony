/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


#ifndef __JTHREAD_H__
#define __JTHREAD_H__

/**
 * @file
 * @brief Java threading interface
 * @details
 * Java threading interface - contains functions to work with Java threads. 
 * The generic part od Java thrading interface is mostly targeted to address 
 * the needs of <code>java.lang.Object</code> and <code>java.lang.Thread</code> 
 * classes implementations.
 * All functions in this interface start with <code><>jthread_*</code> prefix.
 * The implemnentation of this layer provides the mapping of Java thrads onto 
 * native/OS threads.
 * 
 * For more detailes, see thread manager component documentation located at 
 * <code>vm/thread/doc/ThreadManager.htm</code>
 */

#include <jni.h>
#include <jvmti.h>
#include "open/types.h"
#include "open/hythread_ext.h"
#include "ti_thread.h"

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/** 
 * @name Basic manipulation
 */
//@{

typedef struct JVMTIThread *jvmti_thread_t;
typedef struct jthread_start_proc_data * jthread_start_proc_data_t;
typedef struct VM_thread * vm_thread_t;

VMEXPORT jlong jthread_thread_init(JNIEnv *jni_env, jthread java_thread, jobject weak_ref, hythread_t dead_thread);
VMEXPORT IDATA jthread_create(JNIEnv * jni_env, jthread thread, jthread_start_proc_data_t attrs);
VMEXPORT IDATA jthread_create_with_function(JNIEnv * jni_env, jthread thread, jthread_start_proc_data_t attrs);
VMEXPORT IDATA jthread_attach(JNIEnv * jni_env, jthread thread, jboolean daemon);
VMEXPORT IDATA jthread_detach(jthread thread);
VMEXPORT IDATA jthread_java_detach(jthread thread);
VMEXPORT IDATA jthread_vm_detach(vm_thread_t vm_thread);
VMEXPORT IDATA jthread_yield();
VMEXPORT IDATA jthread_stop(jthread thread);
VMEXPORT IDATA jthread_exception_stop(jthread thread, jobject throwable);
VMEXPORT IDATA jthread_sleep(jlong millis, jint nanos);
VMEXPORT JNIEnv *jthread_get_JNI_env(jthread thread);
VMEXPORT IDATA jthread_wait_for_all_nondaemon_threads();



//@}
/** @name Identification
 */
//@{

VMEXPORT jthread jthread_self(void);
VMEXPORT jlong jthread_get_id(jthread thread);
VMEXPORT jthread jthread_get_thread(jlong thread_id);



//@}
/** @name Top&lt;-&gt;middle pointer conversion
 */
//@{

VMEXPORT jthread jthread_get_java_thread(hythread_t thread);


//@}
/** @name Attributes access
 */
//@{


VMEXPORT IDATA jthread_set_priority(jthread thread, jint priority);
VMEXPORT jint jthread_get_priority(jthread thread);

/**
 * Sets the name for the <code>thread</code>.
 *
 * @param[in] thread those attribute is set
 * @param[in] name thread name
 *
 * @sa <code>java.lang.Thread.setName()</code>
 */
VMEXPORT IDATA jthread_set_name(jthread thread, jstring name);

/**
 * Returns the name for the <code>thread</code>.
 *
 * @param[in] - thread those attribute is read
 *
 * @sa <code>java.lang.Thread.getName()</code>
 */
VMEXPORT jstring jthread_get_name(jthread thread);


//@}
/** @name Interruption
 */
//@{

VMEXPORT IDATA jthread_interrupt(jthread thread);
VMEXPORT jboolean jthread_is_interrupted(jthread thread);
VMEXPORT IDATA jthread_clear_interrupted(jthread thread);


//@}
/** @name Monitors
 */
//@{

VMEXPORT IDATA jthread_monitor_init(jobject mon);
VMEXPORT IDATA jthread_monitor_enter(jobject mon);
VMEXPORT IDATA jthread_monitor_try_enter(jobject mon);
VMEXPORT IDATA jthread_monitor_exit(jobject mon);
VMEXPORT IDATA jthread_monitor_release(jobject mon);
VMEXPORT IDATA jthread_monitor_notify(jobject mon);
VMEXPORT IDATA jthread_monitor_notify_all(jobject mon);
VMEXPORT IDATA jthread_monitor_wait(jobject mon);
VMEXPORT IDATA jthread_monitor_timed_wait(jobject mon, jlong millis, jint nanos);

//@}
/** @name Parking
 */
//@{

VMEXPORT IDATA jthread_park();
VMEXPORT IDATA jthread_timed_park(jlong millis, jint nanos);
VMEXPORT IDATA jthread_unpark(jthread thread);
VMEXPORT IDATA jthread_park_until(jlong milis);

//@}
/** @name Suspension
 */
//@{

VMEXPORT IDATA jthread_suspend(jobject thread);
VMEXPORT IDATA jthread_suspend_all(jvmtiError* results, jint count, const jobject* thread_list);
VMEXPORT IDATA jthread_resume(jobject thread);
VMEXPORT IDATA jthread_resume_all(jvmtiError* results, jint count, const jobject* thread_list);
VMEXPORT IDATA jthread_cancel_all();

#ifdef __cplusplus
}
#endif

#endif  /* __JTHREAD_H__ */
