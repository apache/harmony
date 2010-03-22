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
 * @author Andrey Chernyshev
 */  

#define LOG_DOMAIN "kernel"
#include "cxxlog.h"

#include "java_lang_VMThreadManager.h"
#include "open/hythread_ext.h"
#include "jthread.h"
#include "ti_thread.h"
#include "jni_utils.h"
#include "thread_manager.h"
#include "vm_threads.h"

/*
 * Class:     java_lang_VMThreadManager
 * Method:    currentThreadNative
 * Signature: ()Ljava/lang/Thread;
 */
JNIEXPORT jobject JNICALL Java_java_lang_VMThreadManager_currentThreadNative
  (JNIEnv * UNREF jenv, jclass clazz)
{
    return jthread_self();
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    holdsLock
 * Signature: (Ljava/lang/Object;)Z
 */
JNIEXPORT jboolean JNICALL Java_java_lang_VMThreadManager_holdsLock
  (JNIEnv * UNREF jenv, jclass clazz, jobject monitor)
{
	return jthread_holds_lock(jthread_self(), monitor);
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    interrupt
 * Signature: (Ljava/lang/Thread;)I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMThreadManager_interrupt
  (JNIEnv * UNREF jenv, jclass clazz, jobject jthread)
{
    return (jint)jthread_interrupt(jthread);
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    isInterrupted
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_java_lang_VMThreadManager_isInterrupted__
  (JNIEnv * UNREF jenv, jclass clazz)
{
    return (jthread_clear_interrupted(jthread_self()) == TM_ERROR_INTERRUPT)?JNI_TRUE:JNI_FALSE;
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    isInterrupted
 * Signature: (Ljava/lang/Thread;)Z
 */
JNIEXPORT jboolean JNICALL Java_java_lang_VMThreadManager_isInterrupted__Ljava_lang_Thread_2
  (JNIEnv * UNREF jenv, jclass clazz, jobject thread)
{
    return jthread_is_interrupted(thread);
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    notify
 * Signature: (Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMThreadManager_notify
  (JNIEnv * UNREF jenv, jclass clazz, jobject monitor)
{
    return (jint)jthread_monitor_notify(monitor);
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    notifyAll
 * Signature: (Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMThreadManager_notifyAll
  (JNIEnv * UNREF jenv, jclass clazz, jobject monitor)
{
    return (jint)jthread_monitor_notify_all(monitor);
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    resume
 * Signature: (Ljava/lang/Thread;)I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMThreadManager_resume
  (JNIEnv * UNREF jenv, jclass clazz, jobject thread)
{
    return (jint)jthread_resume(thread);
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    setPriority
 * Signature: (Ljava/lang/Thread;I)I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMThreadManager_setPriority
  (JNIEnv * UNREF jenv, jclass clazz, jobject UNREF thread, jint UNREF priority)
{
    return (jint)jthread_set_priority(thread, priority);
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    sleep
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMThreadManager_sleep
  (JNIEnv * UNREF jenv, jclass clazz, jlong millis, jint nanos)
{
    return (jint)jthread_sleep(millis, nanos); 
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    init
 * Signature: (JI)V
 */
JNIEXPORT jlong JNICALL Java_java_lang_VMThreadManager_init
  (JNIEnv *jenv, jclass clazz, jobject thread, jobject ref, jlong oldThread)
{
    return jthread_thread_init(jenv, thread, ref, (hythread_t)(POINTER_SIZE_INT)oldThread);
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    start
 * Signature: (Ljava/lang/Thread;JZI)I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMThreadManager_start
  (JNIEnv *jenv, jclass clazz, jobject thread, jlong stackSize, jboolean daemon, jint priority)
{
    jthread_start_proc_data attrs = {0};

    attrs.daemon = daemon;
    attrs.priority = priority;
    // FIXME - may be define value 40000000
    attrs.stacksize = stackSize > 40000000 ? 0:(jint)stackSize;

    return (jint)jthread_create(jenv, thread, &attrs);
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    stop
 * Signature: (Ljava/lang/Thread;Ljava/lang/Throwable;)I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMThreadManager_stop
  (JNIEnv *env, jclass clazz, jobject UNREF thread, jthrowable UNREF threadDeathException)
{
    return (jint)jthread_exception_stop(thread, threadDeathException);
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    suspend
 * Signature: (Ljava/lang/Thread;)I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMThreadManager_suspend
  (JNIEnv * UNREF jenv, jclass clazz, jobject jthread)
{
    return (jint)jthread_suspend(jthread);
}


/*
 * Class:     java_lang_VMThreadManager
 * Method:    wait
 * Signature: (Ljava/lang/Object;JI)I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMThreadManager_wait
  (JNIEnv *env, jclass clazz, jobject monitor, jlong millis, jint UNREF nanos)
{
    // TODO: need to evaluate return code properly
    return (jint)jthread_monitor_timed_wait(monitor, millis, nanos);
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    yield
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMThreadManager_yield
  (JNIEnv * UNREF jenv, jclass clazz)
{
    return (jint)jthread_yield();
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    isAlive
 * Signature: (Ljava/lang/Thread;)Z
 */
JNIEXPORT jboolean JNICALL Java_java_lang_VMThreadManager_isAlive
  (JNIEnv *jenv, jclass clazz, jobject thread)
{
    hythread_t tm_native_thread;

    tm_native_thread = jthread_get_native_thread(thread);
    assert(tm_native_thread);
    return hythread_is_alive(tm_native_thread) ? 1 : 0;
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    join
 * Signature: (Ljava/lang/Thread;JI)I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMThreadManager_join
  (JNIEnv * UNREF jenv, jclass clazz, jobject thread, jlong millis, jint nanos)
{
    assert(0);  //wjw  this API is no longer used, remove it as soon as it is convenient
    return (jint) 0;
}

/*
 * Class:     java_lang_VMThreadManager
 * Method:    getState
 * Signature: (Ljava/lang/Thread;)I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMThreadManager_getState
  (JNIEnv * UNREF jenv, jclass clazz, jobject java_thread)
{
    jint state;
    assert(java_thread);
    jthread_get_jvmti_state(java_thread, &state);

    // FIXME - need to set WAITING state instead
    hythread_t native_thread = jthread_get_native_thread(java_thread);
    assert(native_thread);
    if (hythread_is_parked(native_thread)) {
        state |= TM_THREAD_STATE_PARKED;
    }
    return state;
}


/*
 * Class:     java_lang_VMThreadManager
 * Method:    yield
 * Signature: ()I
 */

/*
 * ????
JNIEXPORT jint JNICALL Java_java_lang_VMThreadManager_initVMThreadManager
  (JNIEnv * UNREF jenv, jclass clazz)
{
    return hythread_init();
}
*/
