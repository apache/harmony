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
 * @author Artem Aliev
 */  
#include "java_util_concurrent_locks_LockSupport.h"
#include "thread_generic.h"
#include "jthread.h"
#include "vm_threads.h"
#include "jni.h"

/* Inaccessible static: parked */
/*
 * Method: java.util.concurrent.locks.LockSupport.unpark(Ljava/lang/Thread;)V
 */
JNIEXPORT void JNICALL Java_java_util_concurrent_locks_LockSupport_unpark (JNIEnv *jenv, jclass, jobject thread) {
    if (!thread) return;
    jthread_unpark(thread);
}

/*
 * Method: java.util.concurrent.locks.LockSupport.park()V
 */
JNIEXPORT void JNICALL Java_java_util_concurrent_locks_LockSupport_park (JNIEnv * UNREF jenv, jclass) {
    jthread_park();
}

/*
 * Method: java.util.concurrent.locks.LockSupport.parkNanos(J)V
 */

#if defined (__INTEL_COMPILER) 
#pragma warning( push )
#pragma warning (disable:1682)
#endif

JNIEXPORT void JNICALL Java_java_util_concurrent_locks_LockSupport_parkNanos(JNIEnv * UNREF jenv, jclass, jlong nanos) {
    jthread_timed_park(0,(jint)nanos);
}

#if defined (__INTEL_COMPILER)
#pragma warning( pop )
#endif

/*
 * Method: java.util.concurrent.locks.LockSupport.parkUntil(J)V
 */
JNIEXPORT void JNICALL Java_java_util_concurrent_locks_LockSupport_parkUntil(JNIEnv * UNREF jenv, jclass UNREF thread, jlong millis) {
    jthread_park_until((jlong)millis);
}

