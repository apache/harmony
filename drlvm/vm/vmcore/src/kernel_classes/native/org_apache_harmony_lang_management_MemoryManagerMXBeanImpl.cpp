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
 * @author Andrey Yakushev
 */

/**
 * @file org_apache_harmony_lang_management_MemoryManagerMXBeanImpl.cpp
 *
 * This file is a part of kernel class natives VM core component.
 * It contains implementation for native methods of
 * org.apache.harmony.lang.management.MemoryManagerMXBeanImpl class.
 */

#include "org_apache_harmony_lang_management_MemoryManagerMXBeanImpl.h"
#include <cxxlog.h>
#include "environment.h"
#include "port_malloc.h"

/* Native methods */

/*
 * Method: org.apache.harmony.lang.management.MemoryManagerMXBeanImpl.createMemoryPools(ILorg/apache/harmony/lang/management/MemoryMXBeanImpl;)V
 */
JNIEXPORT void JNICALL
Java_org_apache_harmony_lang_management_MemoryManagerMXBeanImpl_createMemoryPools(
    JNIEnv *jenv_ext, jobject obj, jint, jobject memBean)
{
    // TODO implement this method stub correctly
    TRACE2("management","createMemoryPools stub invocation");

    JNIEnv_Internal *jenv = (JNIEnv_Internal *)jenv_ext;

    jclass memoryManagerMXBeanImplClazz =jenv->FindClass(
        "org/apache/harmony/lang/management/MemoryManagerMXBeanImpl");
    if (jenv->ExceptionCheck()) {return;};

    jmethodID createMemoryPoolHelperMethod = jenv->GetMethodID(
        memoryManagerMXBeanImplClazz,
        "createMemoryPoolHelper",
        "(Ljava/lang/String;ZILorg/apache/harmony/lang/management/MemoryMXBeanImpl;)V");
    if (jenv->ExceptionCheck()) {return;};

    jobject nameMP = jenv->NewStringUTF(NATIVE_POOL_NAME);
    if (jenv->ExceptionCheck()) {return;};

    jenv->CallVoidMethod(obj, createMemoryPoolHelperMethod, nameMP, JNI_TRUE, 1, memBean);
    if (jenv->ExceptionCheck()) {return;};
};

/*
 * Method: org.apache.harmony.lang.management.MemoryManagerMXBeanImpl.isValidImpl()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_lang_management_MemoryManagerMXBeanImpl_isValidImpl(JNIEnv *, jobject)
{
    // TODO implement this method stub correctly
    TRACE2("management","isValidImpl stub invocation");
    return JNI_TRUE;
};

