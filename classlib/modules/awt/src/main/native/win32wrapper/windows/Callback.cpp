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
 * @author Pavel Dolgov
 */
#include <windows.h>

#include "Callback.h"

static jmethodID javaCallback;
static jmethodID javaCallbackOFN;
static jmethodID javaCallbackDataTransfer;
static jclass javaClass = NULL;
static JavaVM* jvm;

LRESULT __stdcall CallBackWNDPROC(HWND p1, UINT p2, WPARAM p3, LPARAM p4) {
    JNIEnv* env;
    jvm->GetEnv((void**)&env, JNI_VERSION_1_2);
    return (LRESULT)env->CallStaticLongMethod(javaClass, javaCallback, (jlong)p1, (jint)p2, (jlong)p3, (jlong)p4);
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_nativebridge_windows_Callback_initCallBackWNDPROC (JNIEnv * env, jclass cls) {
    javaClass = (jclass)env->NewGlobalRef(cls);
    javaCallback = env->GetStaticMethodID(cls, "runCallbackWNDPROC", "(JIJJ)J");
    env->GetJavaVM(&jvm);
    return (jlong)(void *)CallBackWNDPROC;
}

LRESULT __stdcall CallBackOFNHOOKPROC(HWND p1, UINT p2, WPARAM p3, LPARAM p4) {
    JNIEnv* env;
    jvm->GetEnv((void**)&env, JNI_VERSION_1_2);
    return (LRESULT)env->CallStaticLongMethod(javaClass, javaCallbackOFN, (jlong)p1, (jint)p2, (jlong)p3, (jlong)p4);
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_nativebridge_windows_Callback_initCallBackOFNHOOKPROC (JNIEnv * env, jclass cls) {
    javaClass = (jclass)env->NewGlobalRef(cls);
    javaCallbackOFN = env->GetStaticMethodID(cls, "runCallbackOFNHOOKPROC", "(JIJJ)J");
    env->GetJavaVM(&jvm);
    return (jlong)(void *)CallBackOFNHOOKPROC;
}

LRESULT __stdcall DataTransferProc(HWND p1, UINT p2, WPARAM p3, LPARAM p4) {
    JNIEnv* env;
    jvm->GetEnv((void**)&env, JNI_VERSION_1_2);
    return (LRESULT)env->CallStaticLongMethod(javaClass, javaCallbackDataTransfer, (jlong)p1, (jint)p2, (jlong)p3, (jlong)p4);
}

JNIEXPORT jlong JNICALL Java_org_apache_harmony_awt_nativebridge_windows_Callback_initCallBackDataTransferProc (JNIEnv * env, jclass cls) {
    javaClass = (jclass)env->NewGlobalRef(cls);
    javaCallbackDataTransfer = env->GetStaticMethodID(cls, "runCallbackDataTransferProc", "(JIJJ)J");
    env->GetJavaVM(&jvm);
    return (jlong)(void *)DataTransferProc;
}
