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
 * @author Euguene Ostrovsky
 */

/**
 * @file java_lang_VMMemoryManager.cpp
 *
 * This file is a part of kernel class natives VM core component.
 * It contains implementation for native methods of 
 * java.lang.VMMemoryManager class.
 */


#include "open/gc.h"
#include "jni_utils.h"
#include "object.h"
#include "finalize.h"
#include "cxxlog.h"
#include "vm_threads.h"

#include "java_lang_VMMemoryManager.h"

/*
 * Class:     java_lang_VMMemoryManager
 * Method:    arrayCopy
 * Signature: (Ljava/lang/Object;ILjava/lang/Object;II)V
 */
JNIEXPORT void JNICALL Java_java_lang_VMMemoryManager_arrayCopy
  (JNIEnv *jenv, jclass, jobject src, jint srcPos, jobject dest, jint destPos, jint len)
{
    array_copy_jni(jenv, src, srcPos, dest, destPos, len);
}

/*
 * Class:     java_lang_VMMemoryManager
 * Method:    clone
 * Signature: (Ljava/lang/Object;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_java_lang_VMMemoryManager_clone
  (JNIEnv *jenv, jclass, jobject obj)
{
    return object_clone(jenv, obj);
}

/*
 * Class:     java_lang_VMMemoryManager
 * Method:    getFreeMemory
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_java_lang_VMMemoryManager_getFreeMemory
  (JNIEnv *, jclass)
{
    return gc_free_memory();
}

/*
 * Class:     java_lang_VMMemoryManager
 * Method:    getIdentityHashCode
 * Signature: (Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMMemoryManager_getIdentityHashCode
  (JNIEnv *jenv, jclass, jobject obj)
{
    return object_get_generic_hashcode(jenv, obj);
}

/*
 * Class:     java_lang_VMMemoryManager
 * Method:    getMaxMemory
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_java_lang_VMMemoryManager_getMaxMemory
  (JNIEnv *, jclass)
{
    return gc_max_memory();
}

/*
 * Class:     java_lang_VMMemoryManager
 * Method:    getTotalMemory
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_java_lang_VMMemoryManager_getTotalMemory
  (JNIEnv *, jclass)
{
    return gc_total_memory();
}

/*
 * Class:     java_lang_VMMemoryManager
 * Method:    runFinalzation
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_lang_VMMemoryManager_runFinalization
  (JNIEnv *, jclass)
{
    TRACE2("ref", "Enqueueing references");
    vm_enqueue_references();
    
    // For now we run the finalizers immediately in the context of the thread which requested GC.
    // Eventually we may have a different scheme, e.g., a dedicated finalize thread.
    TRACE2("finalize", "Running pending finalizers");
    vm_run_pending_finalizers();
}

/*
 * Class:     java_lang_VMMemoryManager
 * Method:    runGC
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_lang_VMMemoryManager_runGC
  (JNIEnv *, jclass)
{
    assert(hythread_is_suspend_enabled());
    gc_force_gc();      
    return;
}
