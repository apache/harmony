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
 * @file thread_java_basic.c
 * @brief Key threading operations like thread creation and pointer conversion.
 */
#define LOG_DOMAIN "tm.java"
#include "cxxlog.h"

#include "open/hythread_ext.h"
#include "open/vm_properties.h"
#include "jthread.h"
#include "vm_threads.h"
#include "port_thread.h"
#include "jni.h"

static jmethodID jthread_get_run_method(JNIEnv * env, jthread java_thread);
static jfieldID jthread_get_alive_field(JNIEnv * env, jthread java_thread);
static IDATA jthread_associate_native_and_java_thread(JNIEnv *jni_env,
    jthread java_thread, vm_thread_t vm_thread, jobject weak_ref);

/**
 * Creates new Java thread.
 *
 * The newly created thread will immediately start to execute the <code>run()</code>
 * method of the appropriate <code>thread</code> object.
 *
 * @param[in] jni_env jni environment for the current thread.
 * @param[in] java_thread Java thread object with which new thread must be associated.
 * @param[in] attrs thread attributes.
 * @sa java.lang.Thread.run()
 */
IDATA jthread_create(JNIEnv *jni_env,
                     jthread java_thread,
                     jthread_start_proc_data_t attrs)
{
    return jthread_create_with_function(jni_env, java_thread, attrs);
} // jthread_create

/**
 * Wrapper around user thread start proc.
 * Used to perform some duty jobs right after thread is started
 * and before thread is finished.
 */
int HYTHREAD_PROC jthread_wrapper_start_proc(void *arg)
{
    // store start procedure argument to local variable
    jthread_start_proc_data start_proc_data = *(jthread_start_proc_data_t)arg;
    STD_FREE(arg);

    // get hythread global lock
    IDATA status = hythread_global_lock();
    assert(status == TM_ERROR_NONE);

    // get native thread
    hythread_t native_thread = start_proc_data.native_thread;

    // check hythread library state
    if (hythread_lib_state() != TM_LIBRARY_STATUS_INITIALIZED) {
        // set TERMINATED state
        status = hythread_set_state(native_thread, TM_THREAD_STATE_TERMINATED);
        assert(status == TM_ERROR_NONE);

        // set hythread_self()
        hythread_set_self(native_thread);
        assert(native_thread == hythread_self());

        // release thread structure data
        hythread_detach_ex(native_thread);

        // zero hythread_self() because we don't do it in hythread_detach_ex()
        hythread_set_self(NULL);

        CTRACE(("TM: native thread terminated due to shutdown: native: %p tm: %p",
            port_thread_current(), native_thread));

        // FIXME - uncomment after TM state transition complete
        //STD_FREE(native_thread);

        // release hythread global lock
        status = hythread_global_unlock();
        assert(status == TM_ERROR_NONE);

        return 0;
    }

    // register to group and set ALIVE & RUNNABLE states
    status = hythread_set_to_group(native_thread, get_java_thread_group());
    assert(status == TM_ERROR_NONE);

    // set hythread_self()
    hythread_set_self(native_thread);
    assert(native_thread == hythread_self());

    // set priority
    status = hythread_set_priority(native_thread,
                                   hythread_get_priority(native_thread));
    // FIXME - cannot set priority
    //assert(status == TM_ERROR_NONE);

    // get java thread
    vm_thread_t vm_thread = jthread_get_vm_thread_unsafe(native_thread);
    assert(vm_thread);
    jobject java_thread = vm_thread->java_thread;
    assert(java_thread);

    // attach java thread to VM
    JNIEnv *jni_env;
    status = vm_attach(start_proc_data.java_vm, &jni_env);
    assert(status == JNI_OK);

    vm_thread->jni_env = jni_env;
    vm_thread->daemon = start_proc_data.daemon;

    CTRACE(("TM: Java thread started: id=%d OS_handle=%p",
           hythread_get_id(native_thread), port_thread_current()));

    if (!vm_thread->daemon) {
        status = hythread_increase_nondaemon_threads_count(native_thread);
        assert(status == TM_ERROR_NONE);
    }

    // increase started thread count
    jthread_start_count();

    // set j.l.Thread.isAlive field to true
    assert(hythread_is_alive(native_thread));
    jni_env->SetBooleanField(java_thread,
        jthread_get_alive_field(jni_env, java_thread), true);

    // release hythread global lock
    status = hythread_global_unlock();
    assert(status == TM_ERROR_NONE);

    // send JVMTI Thread Start event
    if(jvmti_should_report_event(JVMTI_EVENT_THREAD_START)) {
        jvmti_send_thread_start_end_event(vm_thread, 1);
    }

    jvmtiStartFunction start_jvmti_proc = start_proc_data.proc;
    if (start_jvmti_proc != NULL) {
        // start JVMTI thread
        start_jvmti_proc(start_proc_data.jvmti_env, jni_env,
            (void*)start_proc_data.arg);
    } else {
        // start Java thread
        jni_env->CallVoidMethodA(java_thread,
            jthread_get_run_method(jni_env, java_thread), NULL);
    }

    // run j.l.Thread.detach(), don't get hythread global lock here
    status = jthread_java_detach(java_thread);
    assert(status == TM_ERROR_NONE);

    // get hythread global lock
    status = hythread_global_lock();
    assert(status == TM_ERROR_NONE);

    // detach vm thread
    status = jthread_vm_detach(vm_thread);
    assert(status == TM_ERROR_NONE);

    // set TERMINATED thread
    status = hythread_set_state(native_thread, TM_THREAD_STATE_TERMINATED);
    assert(status == TM_ERROR_NONE);

    CTRACE(("TM: Java thread finished: id=%d OS_handle=%p",
        hythread_get_id(native_thread), port_thread_current()));

    hythread_detach_ex(native_thread);

    // FIXME - uncomment after TM state transition complete
    //STD_FREE(vm_thread);

    // release hythread global lock
    status = hythread_global_unlock();
    assert(status == TM_ERROR_NONE);

    return 0;
} // jthread_wrapper_start_proc

/**
 * Creates new Java thread with specific execution function.
 *
 * This function differs from <code>jthread_create</code> such that
 * the newly created thread, instead of invoking Java execution engine,
 * would start to directly execute the specific native function pointed by the <code>proc</code>.
 * This method of thread creation would be useful for creating TI agent threads (i.e. Java threads
 * which always execute only native code).
 *
 * @param[in] jni_env jni environment for the current thread.
 * @param[in] java_thread Java thread object with which new thread must be associated.
 * @param[in] attrs thread attributes.
 * @param[in] proc the start function to be executed in this thread.
 * @param[in] arg The argument to the start function. Is passed as an array.
 * @sa JVMTI::RunAgentThread()
 */
IDATA jthread_create_with_function(JNIEnv *jni_env,
                                   jthread java_thread,
                                   jthread_start_proc_data_t given_attrs)
{
    if (jni_env == NULL || java_thread == NULL || given_attrs == NULL) {
        return TM_ERROR_NULL_POINTER;
    }
    hythread_t native_thread = jthread_get_native_thread(java_thread);
    assert(native_thread);

    vm_thread_t vm_thread = jthread_get_vm_thread_unsafe(native_thread);
    assert(vm_thread);
    vm_thread->java_thread = jni_env->NewGlobalRef(java_thread);

    // prepare args for wrapper_proc
    jthread_start_proc_data_t attrs =
        (jthread_start_proc_data_t)STD_MALLOC(sizeof(jthread_start_proc_data));
    if (attrs == NULL) {
        return TM_ERROR_OUT_OF_MEMORY;
    }
    *attrs = *given_attrs;
    attrs->native_thread = native_thread;

    // Get JavaVM 
    IDATA status = jni_env->GetJavaVM(&attrs->java_vm);
    if (status != JNI_OK) {
        return TM_ERROR_INTERNAL;
    }

    static size_t default_stacksize;
    if (0 == default_stacksize) {
        size_t stack_size = vm_property_get_size("thread.stacksize", 0, VM_PROPERTIES);
        default_stacksize = stack_size ? stack_size : TM_DEFAULT_STACKSIZE;
    }

    if (!attrs->stacksize) {
        attrs->stacksize = default_stacksize;
    }

    status = hythread_create_ex(native_thread, NULL, attrs->stacksize,
                        attrs->priority, jthread_wrapper_start_proc, NULL, attrs);

    CTRACE(("TM: Created thread: id=%d", hythread_get_id(native_thread)));

    return status;
} // jthread_create_with_function

/**
 * Attaches the current native thread to Java VM.
 *
 * This function will create a control structure for Java thread
 * and associate it with the current native thread. Nothing happens
 * if this thread is already attached.
 *
 * @param[in] jni_env JNI environment for current thread
 * @param[in] java_thread j.l.Thread instance to associate with current thread
 * @param[in] daemon JNI_TRUE if attaching thread is a daemon thread, JNI_FALSE otherwise
 * @sa JNI::AttachCurrentThread ()
 */
IDATA jthread_attach(JNIEnv *jni_env, jthread java_thread, jboolean daemon)
{
    if (jthread_self() != NULL) {
        // Do nothing if thread already attached.
        return TM_ERROR_NONE;
    }

    hythread_t native_thread = hythread_self();
    assert(native_thread);
    vm_thread_t vm_thread = jthread_get_vm_thread(native_thread);
    assert(vm_thread);
    IDATA status = jthread_associate_native_and_java_thread(jni_env,
        java_thread, vm_thread, NULL);
    if (status != TM_ERROR_NONE) {
        return status;
    }

    vm_thread->java_thread = jni_env->NewGlobalRef(java_thread);
    vm_thread->jni_env = jni_env;
    vm_thread->daemon = daemon;
    if (!daemon) {
        status = hythread_increase_nondaemon_threads_count(native_thread);
        assert(status == TM_ERROR_NONE);
    }

    // Send Thread Start event.
    assert(hythread_is_alive(native_thread));
    if(jvmti_should_report_event(JVMTI_EVENT_THREAD_START)) {
        jvmti_send_thread_start_end_event(vm_thread, 1);
    }
    jthread_start_count();

    CTRACE(("TM: Current thread attached to jthread=%p", java_thread));
    return TM_ERROR_NONE;
} // jthread_attach

/**
 * Associates the Java thread with the native thread.
 *
 * @param[in] env JNI environment that will be associated with the created Java thread
 * @param[in] java_thread the Java thread for the association
 * @return the native thread
 */
jlong jthread_thread_init(JNIEnv *jni_env,
                          jthread java_thread,
                          jobject weak_ref,
                          hythread_t dead_thread)
{
    vm_thread_t vm_thread = NULL;
    if (dead_thread) {
        vm_thread = jthread_get_vm_thread_unsafe(dead_thread);
        assert(vm_thread);
        if (vm_thread->weak_ref) {
            // delete used weak reference
            jni_env->DeleteGlobalRef(vm_thread->weak_ref);
        }
    } else {
        vm_thread = jthread_allocate_thread();
    }

    IDATA status = hythread_struct_init((hythread_t)vm_thread);
    if (status != TM_ERROR_NONE) {
        return 0;
    }

    status = jthread_associate_native_and_java_thread(jni_env,
        java_thread, vm_thread, weak_ref);
    if (status != TM_ERROR_NONE) {
        return 0;
    }
    return (jlong)((IDATA)vm_thread);
} // jthread_thread_init


/**
 * Detaches the selected thread from java VM.
 *
 * This function will release any resources associated with the given thread.
 *
 * @param[in] java_thread Java thread to be detached
 */
IDATA jthread_detach(jthread java_thread)
{
    assert(java_thread);
    assert(hythread_is_suspend_enabled());

    // detach java thread
    IDATA status = jthread_java_detach(java_thread);

    // get hythread global lock
    IDATA lock_status = hythread_global_lock();
    assert(lock_status == TM_ERROR_NONE);

    // get vm_thread
    hythread_t native_thread = jthread_get_native_thread(java_thread);
    assert(native_thread);
    vm_thread_t vm_thread = jthread_get_vm_thread(native_thread);
    assert(vm_thread);

    // detach vm thread
    status |= jthread_vm_detach(vm_thread);

    // release hythread global lock
    lock_status = hythread_global_unlock();
    assert(lock_status == TM_ERROR_NONE);

    return status;
} // jthread_detach

/**
 * Detaches the selected vm_thread.
 *
 * This function will release any resources associated with the given vm_thread.
 *
 * @param[in] java_thread Java thread to be detached
 */
IDATA jthread_vm_detach(vm_thread_t vm_thread)
{
    assert(vm_thread);
    assert(hythread_is_suspend_enabled());

    CTRACE(("TM: jthread_vm_detach(%p)", vm_thread));
    if (!vm_thread->daemon) {
        hythread_t native_thread = (hythread_t)vm_thread;
        IDATA status = hythread_decrease_nondaemon_threads_count(native_thread, 1);
        assert(status == TM_ERROR_NONE);
    }

    // Detach from VM.
    jobject java_thread = vm_thread->java_thread;
    jint status = vm_detach(java_thread);
    if (status != JNI_OK) {
        return TM_ERROR_INTERNAL;
    }

    // FIXME - uncomment after TM state transition complete
    //status = jthread_associate_native_and_java_thread(java_thread, NULL);
    //if (status != TM_ERROR_NONE) {
    //    return status;
    //}

    // Delete global reference to current thread object.
    // jni_env is already deallocated in vm_detach.
    DeleteGlobalRef(/*jni_env*/NULL, java_thread);

    // Decrease alive thread counter
    jthread_end_count();
    assert(hythread_is_suspend_enabled());

    return TM_ERROR_NONE;
} // jthread_vm_detach

static IDATA
jthread_associate_native_and_java_thread(JNIEnv * jni_env,
                                         jthread java_thread,
                                         vm_thread_t vm_thread,
                                         jobject weak_ref)
{
    if ((jni_env == NULL) || (java_thread == NULL))
    {
        return TM_ERROR_NULL_POINTER;
    }
    vm_thread->weak_ref =
        (weak_ref) ? jni_env->NewGlobalRef(weak_ref) : NULL;

    // Associate java thread with native thread      
    jthread_set_tm_data(java_thread, vm_thread);

    return TM_ERROR_NONE;
} // jthread_associate_native_and_java_thread

/**
 * Lets an another thread to pass.
 * @sa java.lang.Thread.yield()
 */
IDATA jthread_yield()
{
    hythread_yield();
    return TM_ERROR_NONE;
} // jthread_yield

/*
 * Callback which is executed in the target thread at safe point 
 * whenever Thread.stop() method is called.
 */
static void stop_callback(void)
{
    hythread_t native_thread = hythread_self();
    assert(native_thread);
    vm_thread_t vm_thread = jthread_get_vm_thread(native_thread);
    assert(vm_thread);
    jobject excn = vm_thread->stop_exception;

    // Does not return if the exception could be thrown straight away
    jthread_throw_exception_object(excn);
} // stop_callback

/**
 * Stops the execution of the given <code>thread</code> and forces
 * ThreadDeath exception to be thrown in it.
 *
 * @param[in] java_thread thread to be stopped
 * @sa java.lang.Thread.stop()
 */
IDATA jthread_stop(jthread java_thread)
{
    assert(java_thread);
    vm_thread_t vm_thread = (vm_thread_t)jthread_get_tm_data(java_thread);
    assert(vm_thread);
    JNIEnv *env = vm_thread->jni_env;
    assert(env);
    jclass clazz = env->FindClass("java/lang/ThreadDeath");
    jmethodID excn_constr = env->GetMethodID(clazz, "<init>", "()V");
    jobject excen_obj = env->NewObject(clazz, excn_constr);

    return jthread_exception_stop(java_thread, excen_obj);
}

/**
 * Stops the execution of the given <code>thread</code> and forces
 * the <code>throwable</code> exception to be thrown in it.
 *
 * @param[in] java_thread thread to be stopped
 * @param[in] excn exception to be thrown
 * @sa java.lang.Thread.stop()
 */
IDATA jthread_exception_stop(jthread java_thread, jobject excn)
{
    assert(java_thread);
    vm_thread_t vm_thread = jthread_get_vm_thread_from_java(java_thread);
    assert(vm_thread);

    // Install safepoint callback that would throw exception
    JNIEnv *env = vm_thread->jni_env;
    assert(env);
    vm_thread->stop_exception = env->NewGlobalRef(excn);

    return hythread_set_thread_stop_callback((hythread_t)vm_thread, stop_callback);
} // jthread_exception_stop

/**
 * Causes the current <code>thread</code> to sleep for at least the specified time.
 * This call doesn't clear the interrupted flag.
 *
 * @param[in] millis timeout in milliseconds
 * @param[in] nanos timeout in nanoseconds
 *
 * @return  returns 0 on success or negative value on failure,
 * or TM_THREAD_INTERRUPTED in case thread was interrupted during sleep.
 * @sa java.lang.Thread.sleep()
 */
IDATA jthread_sleep(jlong millis, jint nanos)
{
    hythread_t native_thread = hythread_self();
    hythread_thread_lock(native_thread);
    IDATA state = hythread_get_state(native_thread);
    state &= ~TM_THREAD_STATE_RUNNABLE;
    state |= TM_THREAD_STATE_WAITING | TM_THREAD_STATE_SLEEPING
                | TM_THREAD_STATE_WAITING_WITH_TIMEOUT;
    IDATA status = hythread_set_state(native_thread, state);
    assert(status == TM_ERROR_NONE);
    hythread_thread_unlock(native_thread);

    status = hythread_sleep_interruptable(millis, nanos);
#ifndef NDEBUG
    if (status == TM_ERROR_INTERRUPT) {
        CTRACE(("TM: sleep interrupted status received, thread: %p",
               hythread_self()));
    }
#endif

    hythread_thread_lock(native_thread);
    state = hythread_get_state(native_thread);
    state &= ~(TM_THREAD_STATE_WAITING | TM_THREAD_STATE_SLEEPING
                    | TM_THREAD_STATE_WAITING_WITH_TIMEOUT);
    state |= TM_THREAD_STATE_RUNNABLE;
    hythread_set_state(native_thread, state);
    hythread_thread_unlock(native_thread);

    return status;
} // jthread_sleep

/**
 * Returns JNI environment associated with the given jthread, or NULL if there is none.
 *
 * The NULL value means the jthread object is not yet associated with native thread,
 * or appropriate native thread has already died and deattached.
 * 
 * @param[in] java_thread java.lang.Thread object
 */
JNIEnv * jthread_get_JNI_env(jthread java_thread)
{
    if (java_thread == NULL) {
        return NULL;
    }
    hythread_t native_thread = jthread_get_native_thread(java_thread);
    if (native_thread == NULL) {
        return NULL;
    }
    vm_thread_t vm_thread = jthread_get_vm_thread(native_thread);
    if (vm_thread == NULL) {
        return NULL;
    }
    return vm_thread->jni_env;
} // jthread_get_JNI_env

/**
 * Returns thread ID for the given <code>thread</code>.
 *
 * Thread ID must be unique for all Java threads.
 * Can be reused after thread is finished.
 *
 * @return thread ID
 * @sa java.lang.Thread.getId()
 */
jlong jthread_get_id(jthread java_thread)
{
    hythread_t native_thread = jthread_get_native_thread(java_thread);
    assert(native_thread);
    return hythread_get_id(native_thread);
} // jthread_get_id

/**
 * Returns jthread given the thread ID.
 *
 * @param[in] thread_id thread ID
 * @return jthread for the given ID, or NULL if there are no such.
 */
jthread jthread_get_thread(jlong thread_id)
{
    hythread_t native_thread = hythread_get_thread((IDATA)thread_id);
    if (native_thread == NULL) {
        return NULL;
    }
    vm_thread_t vm_thread = jthread_get_vm_thread(native_thread);
    assert(vm_thread);
    jobject java_thread = vm_thread->java_thread;
    assert(java_thread);
    return java_thread;
} // jthread_get_thread

/**
 * Returns Java thread associated with the given native <code>thread</code>.
 *
 * @return Java thread
 */
jthread jthread_get_java_thread(hythread_t native_thread)
{
    if (native_thread == NULL) {
        CTRACE(("TM: native thread is NULL"));
        return NULL;
    }
    vm_thread_t vm_thread = jthread_get_vm_thread(native_thread);
    if (vm_thread == NULL) {
        CTRACE(("TM: vm_thread_t thread is NULL"));
        return NULL;
    }
    return vm_thread->java_thread;
} // jthread_get_java_thread

/**
 * Returns jthread associated with the current thread.
 *
 * @return jthread associated with the current thread, 
 * or NULL if the current native thread is not attached to JVM.
 */
jthread jthread_self(void)
{
    return jthread_get_java_thread(hythread_self());
} // jthread_self

/**
 * Cancels all java threads. This method being used at VM shutdown
 * to terminate all java threads.
 */
IDATA jthread_cancel_all()
{
    return hythread_cancel_all(NULL);
} // jthread_cancel_all

/**
 * waiting all nondaemon thread's
 * 
 */
IDATA VMCALL jthread_wait_for_all_nondaemon_threads()
{
    hythread_t native_thread = hythread_self();
    assert(native_thread);
    vm_thread_t vm_thread = jthread_get_vm_thread(native_thread);
    return hythread_wait_for_nondaemon_threads(native_thread, 
                                               (vm_thread->daemon ? 0 : 1));
} // jthread_wait_for_all_nondaemon_threads

/*
 *  Auxiliary function to throw java.lang.InterruptedException
 */
void throw_interrupted_exception(void)
{
    CTRACE(("interrupted_exception thrown"));
    vm_thread_t vm_thread = p_TLS_vmthread;
    assert(vm_thread);
    JNIEnv *env = vm_thread->jni_env;
    assert(env);
    jclass clazz = env->FindClass("java/lang/InterruptedException");
    env->ThrowNew(clazz, "Park() is interrupted");
} // throw_interrupted_exception

static jmethodID jthread_get_run_method(JNIEnv * env, jthread java_thread)
{
    static jmethodID runImpl_method = NULL;

    TRACE("run method find enter");
    if (!runImpl_method) {
        jclass clazz = env->GetObjectClass(java_thread);
        runImpl_method = env->GetMethodID(clazz, "runImpl", "()V");
    }
    TRACE("run method find exit");
    return runImpl_method;
} // jthread_get_run_method

static jfieldID jthread_get_alive_field(JNIEnv * env, jthread java_thread)
{
    static jfieldID isAlive_field = NULL;

    TRACE("run method find enter");
    if (!isAlive_field) {
        jclass clazz = env->GetObjectClass(java_thread);
        isAlive_field = env->GetFieldID(clazz, "isAlive", "Z");
    }
    TRACE("run method find exit");
    return isAlive_field;
} // jthread_get_run_method

