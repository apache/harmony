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
 * @author Gregory Shimansky
 */  
/*
 * JVMTI thread API
 */

#define LOG_DOMAIN "jvmti.thread"
#include "cxxlog.h"

#include "jvmti_utils.h"
#include "jni_utils.h"
#include "vm_threads.h"
#include "thread_generic.h"

#include "ti_thread.h"
#include "jthread.h"
#include "thread_manager.h"
#include "object_handles.h"
#include "platform_lowlevel.h"
#include "mon_enter_exit.h"
#include "interpreter_exports.h"
#include "environment.h"
#include "suspend_checker.h"
#include "stack_iterator.h"

//#include "Class.h" // FIXME: this is for Class::heap_base and Class::heap_end

#define MAX_JVMTI_ENV_NUMBER 10
#define jvmti_test_jenv (p_TLS_vmthread->jni_env)

/*
 * Get Thread State
 *
 * Get the state of a thread.
 *
 * REQUIRED Functionality
 */
jvmtiError JNICALL
jvmtiGetThreadState(jvmtiEnv* env,
                    jthread thread,
                    jint* thread_state_ptr)
{
    TRACE2("jvmti.thread", "GetThreadState called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (NULL != thread)
    {
        if (!is_valid_thread_object(thread)) {
            return JVMTI_ERROR_INVALID_THREAD;
        }
    }
    else 
        thread = jthread_self();

    if (thread_state_ptr == NULL){
        return JVMTI_ERROR_NULL_POINTER;
    }
    IDATA UNUSED status = jthread_get_jvmti_state(thread, thread_state_ptr);
    assert(status == TM_ERROR_NONE);

    return JVMTI_ERROR_NONE;
}

/*
 * Get All Threads
 *
 * Get all live threads. The threads are Java programming language
 * threads; that is, threads that are attached to the VM. A thread
 * is live if java.lang.Thread.isAlive() would return true, that
 * is, the thread has been started and has not yet died. The
 * universe of threads is determined by the context of the JVMTI
 * environment, which typically is all threads attached to the VM.
 *
 * REQUIRED Functionality
 */
jvmtiError JNICALL
jvmtiGetAllThreads(jvmtiEnv* env,
                   jint* threads_count_ptr,
                   jthread** threads_ptr)
{
    jthread_iterator_t iterator;
    int i,java_thread_count; 
    jthread* java_threads;
    jvmtiError err;
    TRACE2("jvmti.thread", "GetAllThreads called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (threads_count_ptr == NULL || threads_ptr == NULL){
        return JVMTI_ERROR_NULL_POINTER;
    }

     //jthread_get_all_threads(threads_ptr, threads_count_ptr);

    iterator=jthread_iterator_create();
    java_thread_count = (jint)jthread_iterator_size(iterator);
    //allocate memory
    err=jvmtiAllocate(env,java_thread_count*sizeof(jthread),(unsigned char**)&java_threads);
    if (err != JVMTI_ERROR_NONE){
        jthread_iterator_release(&iterator);
        return err; 
    } 
    for (i=0;i<java_thread_count;i++) {
        java_threads[i]= oh_copy_to_local_handle(jthread_iterator_next(&iterator));
        assert(java_threads[i]);
    }
    *threads_count_ptr = java_thread_count;
    *threads_ptr = java_threads;
    jthread_iterator_release(&iterator);
    return JVMTI_ERROR_NONE;
}

/*
 * Suspend Thread
 *
 * Suspend the specified thread. If the calling thread is specified,
 * this function will not return until some other thread calls
 * ResumeThread. If the thread is currently suspended, this
 * function does nothing and returns an error.
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiSuspendThread(jvmtiEnv* env,
                   jthread thread)
{
    TRACE2("jvmti.thread", "SuspendThread called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    jvmtiCapabilities capa;

    jvmtiError err = env -> GetCapabilities(&capa);

    if (err != JVMTI_ERROR_NONE){
       return err; 
    } 
    if (capa.can_suspend == 0){
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    }

    if (NULL != thread)
    {
        if (!is_valid_thread_object(thread))
            return JVMTI_ERROR_INVALID_THREAD;
    }
    else
        thread = jthread_self();

    jint state;
    err = jvmtiGetThreadState(env, thread, &state);

     if (err != JVMTI_ERROR_NONE){
        return err;
    } 

    // check error condition: JVMTI_ERROR_THREAD_NOT_ALIVE
    if ((state & JVMTI_THREAD_STATE_ALIVE) == 0)
        return JVMTI_ERROR_THREAD_NOT_ALIVE;

    if (state & JVMTI_THREAD_STATE_SUSPENDED)
        return JVMTI_ERROR_THREAD_SUSPENDED;

    return (jvmtiError)jthread_suspend(thread);
}

/*
 * Suspend Thread List
 *
 * Suspend the request_count threads specified in the request_list
 * array. Threads may be resumed with ResumeThreadList or
 * ResumeThread. If the calling thread is specified in the
 * request_list array, this function will not return until some
 * other thread resumes it. Errors encountered in the suspension
 * of a thread are returned in the results array, not in the
 * return value of this function. Threads that are currently
 * suspended are not suspended.
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiSuspendThreadList(jvmtiEnv* env,
                       jint request_count,
                       const jthread* request_list,
                       jvmtiError* results)
{
    TRACE2("jvmti.thread", "SuspendThreadList called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    jvmtiCapabilities capa;

    jvmtiError err = env -> GetCapabilities(&capa);

    if (err != JVMTI_ERROR_NONE){
       return err; 
    } 
    if (capa.can_suspend == 0){
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    }
    if (request_count < 0){
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }
    if (request_list == NULL || results == NULL){
        return JVMTI_ERROR_NULL_POINTER;
    }

    for (int i = 0; i < request_count; i++){
        results[i] = jvmtiSuspendThread(env, request_list[i]);
    }

    return JVMTI_ERROR_NONE;
}

/*
 * Resume Thread
 *
 * Resume a suspended thread. Any threads currently suspended
 * through a JVMTI suspend function (eg. SuspendThread) or
 * java.lang.Thread.suspend() will resume execution; all other
 * threads are unaffected.
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiResumeThread(jvmtiEnv* env,
                  jthread thread)
{
    TRACE2("jvmti.thread", "ResumeThread called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    jvmtiCapabilities capa;

    jvmtiError err = jvmtiGetCapabilities(env, &capa);

    if (err != JVMTI_ERROR_NONE){
       return err; 
    } 
    if (capa.can_suspend == 0){
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    }
    if (!is_valid_thread_object(thread)){
        return JVMTI_ERROR_INVALID_THREAD;
    }
    if (false){ // TBD
        return JVMTI_ERROR_INVALID_TYPESTATE;
    }

    if (NULL == thread)
        return JVMTI_ERROR_INVALID_THREAD;

    jint state;

    err = jvmtiGetThreadState(env, thread, &state);

    if (err != JVMTI_ERROR_NONE)
        return err;

    if ((state & JVMTI_THREAD_STATE_ALIVE) == 0)
        return JVMTI_ERROR_THREAD_NOT_ALIVE;

    if ((state & JVMTI_THREAD_STATE_SUSPENDED) == 0)
        return JVMTI_ERROR_THREAD_NOT_SUSPENDED;

    jthread_resume(thread);

    return JVMTI_ERROR_NONE;
}

/*
 * Resume Thread List
 *
 * Resume the request_count threads specified in the request_list
 * array. Any thread suspended through a JVMTI suspend function
 * (eg. SuspendThreadList) or java.lang.Thread.suspend() will
 * resume execution.
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiResumeThreadList(jvmtiEnv* env,
                      jint request_count,
                      const jthread* request_list,
                      jvmtiError* results)
{
    TRACE2("jvmti.thread", "ResumeThreadList called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    jvmtiCapabilities capa;

    jvmtiError err = env -> GetCapabilities(&capa);

    if (err != JVMTI_ERROR_NONE){
       return err; 
    } 
    if (capa.can_suspend == 0){
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    }
    if (request_count < 0){
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }
    if (request_list == NULL || results == NULL){
        return JVMTI_ERROR_NULL_POINTER;
    }

    for (int i = 0; i < request_count; i++){
        results[i] = jvmtiResumeThread(env, request_list[i]);
    }

    return JVMTI_ERROR_NONE;
}

/*
 * Stop Thread
 *
 * Send the specified asynchronous exception to the specified
 * thread (similar to java.lang.Thread.stop). Normally, this
 * function is used to kill the specified thread with an instance
 * of the exception ThreadDeath.
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiStopThread(jvmtiEnv* env,
                jthread thread,
                jobject exception)
{
    TRACE2("jvmti.thread", "StopThread called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    jint state;
    
    jvmtiCapabilities capa;

    jvmtiError err = env -> GetCapabilities(&capa);

    if (err != JVMTI_ERROR_NONE){
       return err; 
    } 
    if (capa.can_signal_thread == 0){
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    }
    if (!is_valid_thread_object(thread)){
        return JVMTI_ERROR_INVALID_THREAD;
    }

    err = jvmtiGetThreadState(env, thread, &state);

    if (err != JVMTI_ERROR_NONE){
        return err;
    }
    if ((state & JVMTI_THREAD_STATE_ALIVE) == 0){
        return JVMTI_ERROR_THREAD_NOT_ALIVE;
    }

    if (! is_valid_throwable_object(exception))
        return JVMTI_ERROR_INVALID_OBJECT;


    if (TM_ERROR_NONE != jthread_exception_stop(thread, exception))
        return JVMTI_ERROR_INTERNAL;

    // force exit from wait
    jthread_interrupt(thread);

    return JVMTI_ERROR_NONE;
}

/*
 * Interrupt Thread
 *
 * Interrupt the specified thread (similar to
 * java.lang.Thread.interrupt).
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiInterruptThread(jvmtiEnv* env,
                     jthread thread)
{
    TRACE2("jvmti.thread", "InterruptThread called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    jvmtiCapabilities capa;

    jvmtiError err = env -> GetCapabilities(&capa);

    if (err != JVMTI_ERROR_NONE){
       return err; 
    } 

    if (capa.can_signal_thread == 0){
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    }

    if (!is_valid_thread_object(thread)){
        return JVMTI_ERROR_INVALID_THREAD;
    }

    jint thread_state;
    IDATA UNUSED status = jthread_get_jvmti_state(thread, &thread_state);
    assert(status == TM_ERROR_NONE);

    if (! (JVMTI_THREAD_STATE_ALIVE & thread_state))
        return JVMTI_ERROR_THREAD_NOT_ALIVE;

    return (jvmtiError)jthread_interrupt(thread);
}

/*
 * Get Thread Info
 *
 * Get thread information. The fields of the jvmtiThreadInfo
 * structure are filled in with details of the specified thread.
 *
 * REQUIRED Functionality
 */
jvmtiError JNICALL
jvmtiGetThreadInfo(jvmtiEnv* env,
                   jthread thread,
                   jvmtiThreadInfo* info_ptr)
{
    TRACE2("jvmti.thread", "GetThreadInfo called");
    DebugUtilsTI *ti = VM_Global_State::loader_env->TI;
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (info_ptr == NULL)
        return JVMTI_ERROR_NULL_POINTER;

    if (NULL != thread) {
        if (!is_valid_thread_object(thread)) {
            return JVMTI_ERROR_INVALID_THREAD;
        }
    } else {
        thread = jthread_self();
        assert(thread != NULL);
    }

    jclass cl = GetObjectClass(jvmti_test_jenv, thread);

    jfieldID id = jvmti_test_jenv->GetFieldID(cl, "name", "Ljava/lang/String;");
    assert(id != NULL); // field must exist in kernel class
    jstring name = jvmti_test_jenv->GetObjectField(thread, id);
    info_ptr->name = (char*)jvmti_test_jenv->GetStringUTFChars(name, false);

    id = jvmti_test_jenv->GetFieldID(cl, "priority", "I");
    assert(id != NULL); // field must exist in kernel class
    info_ptr->priority = jvmti_test_jenv->GetIntField(thread, id);

    id = jvmti_test_jenv->GetFieldID(cl, "daemon","Z");
    assert(id != NULL); // field must exist in kernel class
    info_ptr->is_daemon = jvmti_test_jenv->GetBooleanField(thread, id);

    id = jvmti_test_jenv->GetFieldID(cl, "group","Ljava/lang/ThreadGroup;");
    assert(id != NULL); // field must exist in kernel class
    info_ptr->thread_group = jvmti_test_jenv->GetObjectField(thread, id);

    id = jvmti_test_jenv->GetFieldID(cl, "contextClassLoader","Ljava/lang/ClassLoader;");
    assert(id != NULL); // field must exist in kernel class
    info_ptr->context_class_loader = jvmti_test_jenv->GetObjectField(thread, id);

    return JVMTI_ERROR_NONE;
}

/*
 * Get Owned Monitor Info
 *
 * Get information about the monitors owned by the specified thread.
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiGetOwnedMonitorInfo(jvmtiEnv* env,
                         jthread thread,
                         jint* owned_monitor_count_ptr,
                         jobject** owned_monitors_ptr)
{
    TRACE2("jvmti.thread", "GetOwnedMonitorInfo called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    jvmtiCapabilities capa;

    jvmtiError err = env -> GetCapabilities(&capa);

    if (err != JVMTI_ERROR_NONE){
       return err; 
    } 
    if (capa.can_get_owned_monitor_info == 0){
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    }
    if (NULL != thread)
    {
        if (!is_valid_thread_object(thread))
            return JVMTI_ERROR_INVALID_THREAD;
    }
    else
        thread = jthread_self();
    if (owned_monitor_count_ptr == NULL || owned_monitors_ptr == NULL){
        return JVMTI_ERROR_NULL_POINTER;
    }

    jint state;

    err = jvmtiGetThreadState(env, thread, &state);

    if (err != JVMTI_ERROR_NONE){
        return err;
    }
    if ((state & JVMTI_THREAD_STATE_ALIVE) == 0){
        return JVMTI_ERROR_THREAD_NOT_ALIVE;
    }

    bool thread_suspended = false;
    // Gregory -
    // There is a race condition. If target thread is not suspended,
    // it may exit some of its owned monitors which leads to incorrect
    // references in the array.
    vm_thread_t vm_thread = jthread_get_vm_thread_ptr_safe(thread);
    // Check that this thread is not current
    if (vm_thread != p_TLS_vmthread)
    {
        IDATA UNREF status = hythread_suspend_other((hythread_t)vm_thread);
        assert(TM_ERROR_NONE == status);
        thread_suspended = true;
    }

    IDATA UNUSED status = jthread_get_owned_monitors(thread, owned_monitor_count_ptr, owned_monitors_ptr);
    assert(status == TM_ERROR_NONE);

    if (thread_suspended)
        hythread_resume((hythread_t)vm_thread);

    return JVMTI_ERROR_NONE;
}

/*
 * Get Current Contended Monitor
 *
 * Get the object, if any, whose monitor the specified thread is
 * waiting to enter or waiting to regain through java.lang.Object.wait.
 *
 * REQUIRED Functionality
 */
jvmtiError JNICALL
jvmtiGetCurrentContendedMonitor(jvmtiEnv* env,
                                jthread thread,
                                jobject* monitor_ptr)
{
    TRACE2("jvmti.thread", "GetCurrentContendedMonitor called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    jvmtiCapabilities capa;

    jvmtiError err = env -> GetCapabilities(&capa);

    if (err != JVMTI_ERROR_NONE){
       return err; 
    } 
    if (capa.can_get_current_contended_monitor == 0){
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    }
    if (monitor_ptr == NULL){
        return JVMTI_ERROR_NULL_POINTER;
    }
    if (NULL == thread)
        thread = jthread_self();

    jint state;

    err = jvmtiGetThreadState(env, thread, &state);

    if (err != JVMTI_ERROR_NONE){
        return err;
    }
    if ((state & JVMTI_THREAD_STATE_ALIVE) == 0){
        return JVMTI_ERROR_THREAD_NOT_ALIVE;
    }

    IDATA status = jthread_get_contended_monitor(thread, monitor_ptr);

    return (jvmtiError)status;
}

/*
 * Run Agent Thread
 *
 * Starts the execution of an agent thread. with the specified
 * native function.
 *
 * REQUIRED Functionality
 */
jvmtiError JNICALL
jvmtiRunAgentThread(jvmtiEnv* env,
                    jthread thread,
                    jvmtiStartFunction proc,
                    const void* arg,
                    jint priority)
{
    JNIEnv * jni_env;

    TRACE2("jvmti.thread", "RunAgentThread called");
    DebugUtilsTI *ti = VM_Global_State::loader_env->TI;
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (priority < JVMTI_THREAD_MIN_PRIORITY || priority > JVMTI_THREAD_MAX_PRIORITY){
        return JVMTI_ERROR_INVALID_PRIORITY;
    }
    if (!is_valid_thread_object(thread)){
        return JVMTI_ERROR_INVALID_THREAD;
    }
    if (proc == NULL){
        return JVMTI_ERROR_NULL_POINTER;
    }

    // Set daemon flag for the thread
    jclass thread_class = GetObjectClass(jvmti_test_jenv, thread);
    assert(thread_class);
    jfieldID is_daemon = jvmti_test_jenv->GetFieldID(thread_class, "daemon", "Z");
    assert(is_daemon);
    jvmti_test_jenv->SetBooleanField(thread, is_daemon, JNI_TRUE);

    jni_env = jthread_get_JNI_env(jthread_self());

    // Run new thread
    jthread_start_proc_data attrs = {0};
    attrs.priority = priority; 
    attrs.daemon = JNI_TRUE;
    attrs.jvmti_env = env;
    attrs.proc = proc;
    attrs.arg = arg;

    jthread_create_with_function(jni_env, thread, &attrs);

    return JVMTI_ERROR_NONE;
}

/*
 * Set Thread Local Storage
 *
 * The VM stores a pointer value associated with each
 * environment-thread pair. This pointer value is called
 * thread-local storage. This value is NULL unless set with this
 * function. Agents can allocate memory in which they store thread
 * specific information. By setting thread-local storage it can
 * then be accessed with GetThreadLocalStorage.
 *
 * REQUIRED Functionality
 */
jvmtiError JNICALL
jvmtiSetThreadLocalStorage(jvmtiEnv* env,
                           jthread thread,
                           const void* data)
{
    TRACE2("jvmti.thread", "SetThreadLocalStorage called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (NULL != thread)
    {
        if (!is_valid_thread_object(thread)){
            return JVMTI_ERROR_INVALID_THREAD;
    }
    }
    else
        thread = jthread_self();

    jint state;

    jvmtiError err = jvmtiGetThreadState(env, thread, &state);

    if (err != JVMTI_ERROR_NONE){
        return err;
    }
    if ((state & JVMTI_THREAD_STATE_ALIVE) == 0){
        return JVMTI_ERROR_THREAD_NOT_ALIVE;
    }

    JVMTILocalStorage* aa = NULL;
    JVMTILocalStorage* lstg = jthread_get_jvmti_local_storage(thread);
    if (lstg -> env == NULL) {
        if (lstg -> data == NULL) {
            // we have no records stored;
            // so, we put our first record into vm_thread -> jvmti_local_storage
            lstg -> env = (data == NULL) ? NULL : env;
            lstg -> data = (void *)data;
            return JVMTI_ERROR_NONE;
        } else {
            // we have more than one record stored;
            // so, they are stored in array which is pointed at by 
            // vm_thread -> jvmti_local_storage -> data  
            aa = (JVMTILocalStorage*)lstg -> data;
        }
    } else {
        // we have just one record stored;
        // so, it's stored in vm_thread -> jvmti_local_storage 
        if (lstg -> env == env) {
            // override data in this record
            lstg -> data = (void *)data;
            return JVMTI_ERROR_NONE;
        } else if (data != NULL){
            // we have just one record stored and we have to add another one; 
            // so, array is created and record is copied there 
            aa = (JVMTILocalStorage*)STD_MALLOC(sizeof(JVMTILocalStorage)*
                                                           MAX_JVMTI_ENV_NUMBER);
            for (int i = 0; i < MAX_JVMTI_ENV_NUMBER; i++){
                aa[0].env = NULL;
                aa[0].data = NULL;
            }
            aa[0].env = lstg -> env;
            aa[0].data = lstg -> data;
            lstg -> env = NULL;
            lstg -> data = (void *)aa;
        }
    }
    // array look up for existing env or for free record
    int ii = -1;
    for (int i = 0; i < MAX_JVMTI_ENV_NUMBER; i++){
        if (aa[i].env == env){
            ii = i;
            break;
        } else if (aa[i].env == NULL && ii < 0){
            ii = i;
        }
    }
    assert(ii > -1); // ii == -1 => array is full
    aa[ii].env = (data == NULL) ? NULL : env;
    aa[ii].data = (void *)data;

    return JVMTI_ERROR_NONE;
}

/*
 * Get Thread Local Storage
 *
 * Called by the agent to get the value of the JVMTI thread-local
 * storage.
 *
 * REQUIRED Functionality
 */
jvmtiError JNICALL
jvmtiGetThreadLocalStorage(jvmtiEnv* env,
                           jthread thread,
                           void** data_ptr)
{
    TRACE2("jvmti.thread", "GetThreadLocalStorage called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (data_ptr == NULL){
        return JVMTI_ERROR_NULL_POINTER;
    }

    if (NULL != thread)
    {
        if (!is_valid_thread_object(thread)){
            return JVMTI_ERROR_INVALID_THREAD;
    }
    }
    else
        thread = jthread_self();

    jint state;
    jvmtiError err = jvmtiGetThreadState(env, thread, &state);

    if (err != JVMTI_ERROR_NONE){
        return err;
    }
    if ((state & JVMTI_THREAD_STATE_ALIVE) == 0){
        return JVMTI_ERROR_THREAD_NOT_ALIVE;
    }

    *data_ptr = NULL;

    //if (!vm_thread)
    //    return JVMTI_ERROR_THREAD_NOT_ALIVE; // non-existent thread

    JVMTILocalStorage* lstg = jthread_get_jvmti_local_storage(thread);
    if (lstg -> env == NULL) {
        if (lstg -> data != NULL) {
            // we have more than one record stored;
            // so, they are stored in array which is pointed at by 
            // vm_thread -> jvmti_local_storage -> data  
            JVMTILocalStorage* aa = (JVMTILocalStorage* )lstg -> data;
            for (int i = 0; i < MAX_JVMTI_ENV_NUMBER; i++){
                if (aa[i].env == env) {
                    *data_ptr = aa[i].data;
                    break;
                }
            }
        }
    } else {
        // we have just one record stored;
        // so, it's stored in vm_thread -> jvmti_local_storage 
        if (lstg -> env == env) {
            *data_ptr = lstg -> data;
        }
    }

    return JVMTI_ERROR_NONE;
}

