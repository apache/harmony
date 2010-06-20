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
 * @author Intel, Evgueni Brevnov
 */

#define LOG_DOMAIN "vm.core.shutdown"
#include "cxxlog.h"

#include <stdlib.h>
#include <apr_thread_mutex.h>

#include "open/hythread.h"
#include "open/gc.h"

#include "jthread.h"
#include "jni.h"
#include "jni_direct.h"
#include "environment.h"
#include "classloader.h"
#include "compile.h"
#include "nogc.h"
#include "jni_utils.h"
#include "vm_stats.h"
#include "thread_dump.h"
#include "interpreter.h"
#include "finalize.h"
#include "signals.h"

#define PROCESS_EXCEPTION(messageId, message) \
{ \
    LECHO(messageId, message << "Internal error: "); \
\
    if (jni_env->ExceptionCheck()== JNI_TRUE) \
    { \
        jni_env->ExceptionDescribe(); \
        jni_env->ExceptionClear(); \
    } \
\
    return JNI_ERR; \
} \

static jobject java_lang_ThreadGroup_lock_field;

/**
 * Calls java.lang.System.execShutdownSequence() method.
 *
 * @param jni_env JNI environment of the current thread
 */
static jint exec_shutdown_sequence(JNIEnv * jni_env) {
    jclass system_class;
    jmethodID shutdown_method;

    assert(hythread_is_suspend_enabled());
    BEGIN_RAISE_AREA;

    system_class = jni_env->FindClass("java/lang/System");
    if (jni_env->ExceptionCheck() == JNI_TRUE || system_class == NULL) {
        // This is debug message only. May appear when VM is already in shutdown stage.
        PROCESS_EXCEPTION(38, "{0}can't find java.lang.System class.");
    }

    shutdown_method = jni_env->GetStaticMethodID(system_class, "execShutdownSequence", "()V");
    if (jni_env->ExceptionCheck() == JNI_TRUE || shutdown_method == NULL) {
        PROCESS_EXCEPTION(39, "{0}can't find java.lang.System.execShutdownSequence() method.");
    }

    jni_env->CallStaticVoidMethod(system_class, shutdown_method);

    if (jni_env->ExceptionCheck() == JNI_TRUE) {
        PROCESS_EXCEPTION(40, "{0}java.lang.System.execShutdownSequence() method completed with an exception.");
    }

    END_RAISE_AREA;
    return JNI_OK;
}

static void vm_shutdown_callback() {
    hythread_suspend_enable();
    set_unwindable(false);

    vm_thread_t vm_thread = jthread_self_vm_thread();
    assert(vm_thread);

    jobject java_thread = vm_thread->java_thread;
    assert(java_thread);

    IDATA UNUSED status = jthread_detach(java_thread);
    assert(status == TM_ERROR_NONE);

    status = jthread_monitor_release(java_lang_ThreadGroup_lock_field);
    assert(status == TM_ERROR_NONE);

    hythread_exit(NULL);
}

static void vm_thread_cancel(vm_thread_t thread) {
    assert(thread);

    // grab hythread global lock
    hythread_global_lock();

    IDATA UNUSED status = jthread_vm_detach(thread);
    assert(status == TM_ERROR_NONE);

    hythread_cancel((hythread_t)thread);

    // release hythread global lock
    hythread_global_unlock();
}

/**
 * Stops running java threads by throwing an exception
 * up to the first native frame.
 *
 * @param[in] vm_env a global VM environment
 */
static void vm_shutdown_stop_java_threads(Global_Env * vm_env) {
    hythread_t self;
    hythread_t * running_threads;
    hythread_t native_thread;
    hythread_iterator_t it;
    VM_thread *vm_thread;

    self = hythread_self();

    // Get java.lang.ThreadGroup.lock field
    JNIEnv *jni_env = jthread_self_vm_thread()->jni_env;
    Class* klass = vm_env->java_lang_ThreadGroup_Class;
    jobject class_handle = struct_Class_to_jclass(klass);
    jfieldID lock_field_id = jni_env->GetStaticFieldID(class_handle, "lock",
        "Ljava/lang/ThreadGroup$ThreadGroupLock;");
    assert(lock_field_id);
    java_lang_ThreadGroup_lock_field =
        jni_env->GetStaticObjectField(class_handle, lock_field_id);
    assert(java_lang_ThreadGroup_lock_field);

    // Collect running java threads.
    // Set callbacks to let threads exit
    TRACE2("shutdown", "stopping threads, self " << self);
    it = hythread_iterator_create(NULL);
    running_threads = (hythread_t *)apr_palloc(vm_env->mem_pool,
            hythread_iterator_size(it) * sizeof(hythread_t));
    int size = 0;
    while(native_thread = hythread_iterator_next(&it)) {
        vm_thread = jthread_get_vm_thread(native_thread);
        if (native_thread != self && vm_thread != NULL) {
            hythread_set_safepoint_callback(native_thread,
                    vm_shutdown_callback);
            running_threads[size] = native_thread;
            ++size;
        }
    }
    hythread_iterator_release(&it);

    TRACE2("shutdown", "waiting threads");
    for (int i = 0; i < size; i++) {
        hythread_sleep(100);
    }

    TRACE2("shutdown", "cancelling threads");

    // Grab java.lang.ThreadGroup.lock
    IDATA status = jthread_monitor_enter(java_lang_ThreadGroup_lock_field);
    assert(status == TM_ERROR_NONE);

    // forcedly kill remaining threads
    // There is a small chance that some of these threads are not in the point
    // safe for killing, e.g. in malloc()
    it = hythread_iterator_create(NULL);
    while(native_thread = hythread_iterator_next(&it)) {
        vm_thread = jthread_get_vm_thread(native_thread);
        // we should not cancel self and
        // non-java threads (i.e. vm_thread == NULL)
        if (native_thread != self && vm_thread != NULL) {
            vm_thread_cancel(vm_thread);
            TRACE2("shutdown", "cancelling " << native_thread);
            STD_FREE(vm_thread);
        }
    }
    hythread_iterator_release(&it);

    // release java.lang.ThreadGroup.lock
    status = jthread_monitor_exit(java_lang_ThreadGroup_lock_field);
    assert(status == TM_ERROR_NONE);

    TRACE2("shutdown", "shutting down threads complete");
}

/**
 * A native analogue of <code>java.lang.System.execShutdownSequence</code>.
 */
void exec_native_shutdown_sequence() {
    // print out gathered data
#ifdef VM_STATS
    VM_Statistics::get_vm_stats().print();
#endif
}

/**
 * Waits until all non-daemon threads finish their execution,
 * initiates VM shutdown sequence and stops running threads if any.
 *
 * @param[in] java_vm JVM that should be destroyed
 * @param[in] java_thread current java thread
 */
jint vm_destroy(JavaVM_Internal * java_vm, jthread java_thread)
{
    IDATA status;
    JNIEnv * jni_env;
    jobject uncaught_exception;

    assert(hythread_is_suspend_enabled());

    jni_env = jthread_get_JNI_env(java_thread);

    // Wait until all non-daemon threads finish their execution.
    status = jthread_wait_for_all_nondaemon_threads();
    if (status != TM_ERROR_NONE) {
        TRACE("Failed to wait for all non-daemon threads completion.");
        return JNI_ERR;
    }

    // Remember thread's uncaught exception if any.
    uncaught_exception = jni_env->ExceptionOccurred();
    jni_env->ExceptionClear();

    // Execute pending shutdown hooks & finalizers
    status = exec_shutdown_sequence(jni_env);
    exec_native_shutdown_sequence();
    if (status != JNI_OK) return (jint)status;
    
    if(get_native_finalizer_thread_flag())
        wait_native_fin_threads_detached();
    if(get_native_ref_enqueue_thread_flag())
        wait_native_ref_thread_detached();

    // Raise uncaught exception to current thread.
    // It will be properly processed in jthread_java_detach().
    if (uncaught_exception) {
        exn_raise_object(uncaught_exception);
    }

    // Send VM_Death event and switch to DEAD phase.
    // This should be the last event sent by VM.
    // This event should be sent before Agent_OnUnload called.
    jvmti_send_vm_death_event();

    // prepare thread manager to shutdown
    hythread_shutdowning();

    // Call Agent_OnUnload() for agents and unload agents.
    // Gregory -
    // We cannot call this function after vm_shutdown_stop_java_threads!!!
    // In this case agent's thread won't be shutdown properly, and the
    // code of agent's thread will run in unmapped address space
    // of unloaded agent's library. This is bad and will almost certainly crash.
    java_vm->vm_env->TI->Shutdown(java_vm);

    // Stop all (except current) java threads
    // before destroying VM-wide data.
    vm_shutdown_stop_java_threads(java_vm->vm_env);

    // TODO: ups we don't stop native threads as well :-((
    // We are lucky! Currently, there are no such threads.

    // Detach current main thread.
    status = jthread_detach(java_thread);

    // check detach status
    if (status != TM_ERROR_NONE)
        return JNI_ERR;

    // Shutdown signals
    vm_shutdown_signals();

    // Block thread creation.
    // TODO: investigate how to achieve that with ThreadManager

    // Starting this moment any exception occurred in java thread will cause
    // entire java stack unwinding to the most recent native frame.
    // JNI is not available as well.
    assert(java_vm->vm_env->vm_state == Global_Env::VM_RUNNING);
    java_vm->vm_env->vm_state = Global_Env::VM_SHUTDOWNING;

    TRACE2("shutdown", "vm_destroy complete");
    return JNI_OK;
}

static hylatch_t shutdown_end;

static IDATA vm_interrupt_process(void * data) {
    IDATA status = hylatch_wait(shutdown_end);
    assert(status == TM_ERROR_NONE);

    status = hylatch_destroy(shutdown_end);
    assert(status == TM_ERROR_NONE);

    STD_FREE(data);

    // Return 130 to be compatible with RI.
    exit(130);
}

/**
 * Initiates VM shutdown sequence.
 */
static IDATA vm_interrupt_entry_point(void * data) {
    JavaVM * java_vm = (JavaVM *)data;
    JavaVMAttachArgs vm_args = {JNI_VERSION_1_2, const_cast<char*>("InterruptionHandler"), NULL};

    JNIEnv * jni_env;
    jint status = AttachCurrentThread(java_vm, (void **)&jni_env, &vm_args);
    if (status == JNI_OK) {
        exec_shutdown_sequence(jni_env);
        exec_native_shutdown_sequence();
        DetachCurrentThread(java_vm);
    }

    IDATA hy_status = hylatch_count_down(shutdown_end);
    assert(hy_status == TM_ERROR_NONE);

    return status;
}

/**
 * Release allocated resourses.
 */
static IDATA vm_dump_process(void * data) {
    IDATA status = hylatch_wait(shutdown_end);
    assert(status == TM_ERROR_NONE);

    status = hylatch_destroy(shutdown_end);
    assert(status == TM_ERROR_NONE);

    STD_FREE(data);

    return TM_ERROR_NONE;
}

/**
 * Dumps all java stacks.
 */
static IDATA vm_dump_entry_point(void * data) {
    JavaVM * java_vm = (JavaVM *)data;
    JavaVMAttachArgs vm_args = {JNI_VERSION_1_2, const_cast<char*>("DumpHandler"), NULL};

    JNIEnv * jni_env;
    jint status = AttachCurrentThread(java_vm, (void **)&jni_env, &vm_args);
    if (status == JNI_OK) {
        // TODO: specify particular VM to notify.
        jvmti_notify_data_dump_request();
        st_print_all(stdout);
        DetachCurrentThread(java_vm);
    }

    IDATA hy_status = hylatch_count_down(shutdown_end);
    assert(hy_status == TM_ERROR_NONE);

    return status;
}

/**
 * Current process received an interruption signal (Ctrl+C pressed).
 * Shutdown all running VMs and terminate the process.
 */
void vm_interrupt_handler() {
    int nVMs;
    IDATA status = JNI_GetCreatedJavaVMs(NULL, 0, &nVMs);
    assert(nVMs <= 1);
    if (status != JNI_OK) {
        return;
    }

    JavaVM ** vmBuf = (JavaVM **) STD_MALLOC(nVMs * sizeof(JavaVM *)
        + (nVMs + 1) * sizeof(vm_thread_t));
    assert(vmBuf);
    status = JNI_GetCreatedJavaVMs(vmBuf, nVMs, &nVMs);
    assert(nVMs <= 1);
    if (status != JNI_OK) {
        STD_FREE(vmBuf);
        return;
    }

    vm_thread_t *threadBuf = (vm_thread_t*)((char*)vmBuf + (nVMs * sizeof(JavaVM *)));

    status = hylatch_create(&shutdown_end, nVMs);
    assert(status == TM_ERROR_NONE);

    // Create a new thread for each VM to avoid scalability and deadlock problems.
    for (int i = 0; i < nVMs; i++) {
        threadBuf[i] = jthread_allocate_thread();
        assert(threadBuf[i]);
        status = hythread_create_ex((hythread_t)threadBuf[i], NULL, 0, 0, NULL,
            vm_interrupt_entry_point, (void *)vmBuf[i]);
        assert(status == TM_ERROR_NONE);
    }

    // spawn a new thread which will terminate the process.
    status = hythread_create(NULL, 0, 0, 0,
        vm_interrupt_process, (void *)vmBuf);
    assert(status == TM_ERROR_NONE);

    // set a NULL terminator
    threadBuf[nVMs] = NULL;
}

/**
 * Current process received an SIGQUIT signal (Linux) or Ctrl+Break (Windows).
 * Prints java stack traces for each VM running in the current process.
 */
void vm_dump_handler() {
    int nVMs;
    jint status = JNI_GetCreatedJavaVMs(NULL, 0, &nVMs);
    assert(nVMs <= 1);
    if (status != JNI_OK)
        return;

    JavaVM** vmBuf = (JavaVM **) STD_MALLOC(nVMs * sizeof(JavaVM *)
        + (nVMs + 1) * sizeof(vm_thread_t));
    assert(vmBuf);
    status = JNI_GetCreatedJavaVMs(vmBuf, nVMs, &nVMs);
    assert(nVMs <= 1);
    if (status != JNI_OK) {
        STD_FREE(vmBuf);
        return;
    }

    vm_thread_t *threadBuf = (vm_thread_t*)((char*)vmBuf + (nVMs * sizeof(JavaVM *)));

    status = hylatch_create(&shutdown_end, nVMs);
    assert(status == TM_ERROR_NONE);

    // Create a new thread for each VM to avoid scalability and deadlock problems.
    IDATA UNUSED hy_status;
    for (int i = 0; i < nVMs; i++) {
        threadBuf[i] = jthread_allocate_thread();
        assert(threadBuf[i]);
        hy_status = hythread_create_ex((hythread_t)threadBuf[i],
            NULL, 0, 0, NULL, vm_dump_entry_point, (void *)vmBuf[i]);
        assert(hy_status == TM_ERROR_NONE);
    }
    // set buf end marker
    threadBuf[nVMs] = NULL;

    // spawn a new thread which will release resources.
    hy_status = hythread_create(NULL, 0, 0, 0,
        vm_dump_process, (void *)vmBuf);
    assert(hy_status == TM_ERROR_NONE);
}
