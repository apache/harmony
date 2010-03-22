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
#include <assert.h>

#ifndef _CHECK3784_
#define _CHECK3784_

#ifdef __cplusplus
extern "C" {
#endif


static jlong lorig = (jlong)-1;
static jint  iorig = (jint)-1;


JNIEXPORT jint JNICALL 
Java_org_apache_harmony_drlvm_tests_regression_h3784_Test_getPointerSize(JNIEnv *, jclass)  {
    return (jint)sizeof(void*);
}

JNIEXPORT jlong JNICALL 
Java_org_apache_harmony_drlvm_tests_regression_h3784_Test_getAddress(JNIEnv *, jclass) 
{
    if (sizeof(void*)==4) {
        return (jlong)iorig;
    } 
    assert(sizeof(void*)==8);
    return lorig;
}

JNIEXPORT jboolean JNICALL 
Java_org_apache_harmony_drlvm_tests_regression_h3784_Test_check(JNIEnv *, jclass, jlong val) 
{
    if (sizeof(void*)==4) {
        return iorig == (jint)val;
    }
    assert(sizeof(void*)==8);
    return lorig == val;
}


#ifdef __cplusplus
}
#endif
#endif
