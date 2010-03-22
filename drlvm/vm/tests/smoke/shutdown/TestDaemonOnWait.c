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


#include "TestDaemonOnWait.h"

/*
 * Class:     shutdown_TestDaemonOnWait_WorkerThread
 * Method:    callJNI
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_shutdown_TestDaemonOnWait_00024WorkerThread_callJNI
  (JNIEnv * jni_env, jobject thread)
{
    static int allocated = 0;
    jclass thread_class;
    jmethodID methID;

    ++allocated;
    thread_class = (*jni_env)->GetObjectClass(jni_env, thread);
    methID = (*jni_env)->GetMethodID(jni_env, thread_class, "calledFromJNI", "()V");
    if (methID == NULL) {
        printf("FAILED\n");
        --allocated;
        return;
    }

    (*jni_env)->CallVoidMethod(jni_env, thread, methID);
    if ((*jni_env)->ExceptionOccurred(jni_env)) {
        --allocated;
        return;
    }
    printf("FAILED\n");
}
