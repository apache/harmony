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
 * @author Intel, Pavel Afremov
 */

/**
 * @file java_lang_FinalizerThread.cpp
 *
 * This file is a part of kernel class natives VM core component.
 * It contains implementation for native methods of java_lang_FinalizerThread kernel
 * class. Not all of the methods are implemented now.
 */

#include "java_lang_FinalizerThread.h"
#include "open/gc.h"
#include "open/hythread_ext.h"
#include "finalize.h"
#include "port_sysinfo.h"
#include "vm_threads.h"

/* added for NATIVE FINALIZER THREAD */
#include "finalizer_thread.h"
#include "ref_enqueue_thread.h"

/**
 * Implements getObject(..) method.
 * For details see kernel classes component documentation.
 *
 * Class:     java_lang_FinalizerThread
 * Method:    getObject
 * Signature: ()I;
 */
JNIEXPORT jint JNICALL Java_java_lang_FinalizerThread_getProcessorsQuantity
  (JNIEnv *, jclass)
{
    return (jint) port_CPUs_number();
}

/**
 * Implements doFinalization(..) method.
 * For details see kernel classes component documentation.
 *
 * Class:     java_lang_FinalizerThread
 * Method:    doFinalization
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_java_lang_FinalizerThread_doFinalization
  (JNIEnv *, jclass, jint quantity)
{
    vm_enqueue_references();
    return (jint) vm_do_finalization(quantity);
}
/**
 * Implements getFinalizersQuantity() method.
 * For details see kernel classes component documentation.
 *
 * Class:     java_lang_FinalizerThread
 * Method:    getFinalizersQuantity
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_java_lang_FinalizerThread_getFinalizersQuantity
  (JNIEnv *, jclass)
{
    return (jint) vm_get_finalizable_objects_quantity()
            + vm_get_references_quantity();
}

/**
 * Implements fillFinalizationQueueOnExit() method.
 * For details see kernel classes component documentation.
 *
 * Class:     java_lang_FinalizerThread
 * Method:    fillFinalizationQueueOnExit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_lang_FinalizerThread_fillFinalizationQueueOnExit
  (JNIEnv *, jclass)
{
    tmn_suspend_disable();
    gc_finalize_on_exit();
    tmn_suspend_enable();
}

/* BEGIN: These three methods are added for NATIVE FINALIZER THREAD */
/*
 * Class:     java_lang_FinalizerThread
 * Method:    getNativeFinalizerThreadFlagFromVM
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_java_lang_FinalizerThread_getNativeFinalizerThreadFlagFromVM
  (JNIEnv *, jclass)
{
    return (jboolean)get_native_finalizer_thread_flag();
}

/*
 * Class:     java_lang_FinalizerThread
 * Method:    runFinalizationInNativeFinalizerThreads
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_lang_FinalizerThread_runFinalizationInNativeFinalizerThreads
  (JNIEnv *, jclass)
{
    native_sync_enqueue_references();
    
    // Do finalization in dedicated native finalizer threads.
    native_sync_run_finalization();
}

/*
 * Class:     java_lang_FinalizerThread
 * Method:    finalizerShutDown
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_lang_FinalizerThread_finalizerShutDown
  (JNIEnv *, jclass, jboolean value)
{
    finalizer_shutdown(value);
}
/*
 * Class:     java_lang_FinalizerThread
 * Method:    isNativePartEnabled
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_java_lang_FinalizerThread_isNativePartEnabled
  (JNIEnv *, jclass)
{
    return (jboolean) vm_finalization_is_enabled();
}
/* END: These three methods are added for NATIVE FINALIZER THREAD */
