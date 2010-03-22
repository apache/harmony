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

#include <jni.h>

#ifdef __cplusplus
extern "C" jint JNIEXPORT JNICALL JNI_OnLoad(JavaVM *vm, void *reserved);
#endif

jint JNIEXPORT JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv *env;
    jint status = vm->GetEnv((void **)&env, JNI_VERSION_1_4);
    if (status != JNI_OK)
    {
        printf("Could not get JNIenv\n");
        return 0;
    }

    jclass cl = env->FindClass("org/apache/harmony/drlvm/tests/regression/h3074/AnotherClass");
    if (NULL == cl)
    {
        printf("Failed to load class:\n");
        env->ExceptionDescribe();
        return 0;
    }

    jmethodID mid = env->GetStaticMethodID(cl, "method", "()V");
    if (NULL == mid)
    {
        printf("Failed to get method ID:\n");
        env->ExceptionDescribe();
        return 0;
    }

    env->CallStaticVoidMethod(cl, mid);
    return JNI_VERSION_1_4;
}
