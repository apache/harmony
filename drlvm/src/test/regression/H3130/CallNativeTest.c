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

JNIEXPORT void JNICALL 
Java_org_apache_harmony_drlvm_tests_regression_h3130_CallNativeTest_testCallNative(JNIEnv *, jobject); 

JNIEXPORT jobject JNICALL 
Java_org_apache_harmony_drlvm_tests_regression_h3130_CallNativeTest_getNull(JNIEnv *, jclass);


JNIEXPORT void JNICALL 
Java_org_apache_harmony_drlvm_tests_regression_h3130_CallNativeTest_testCallNative(JNIEnv *jenv, jobject obj) 
{
    jclass clazz = (*jenv)->GetObjectClass(jenv, obj);
    jmethodID mid = (*jenv)->GetMethodID(jenv, clazz, "getNull", "()Ljava/lang/Object;");
    jobject res = (*jenv)->CallObjectMethod(jenv, obj, mid);
    if (res) {
        (*jenv)->ThrowNew(jenv, (*jenv)->FindClass(jenv, "junit/framework/AssertionFailedError"), "Non-null returned");
    }
}

JNIEXPORT jobject JNICALL 
Java_org_apache_harmony_drlvm_tests_regression_h3130_CallNativeTest_getNull(JNIEnv *jenv, jclass jcl) {
    return NULL;
}
