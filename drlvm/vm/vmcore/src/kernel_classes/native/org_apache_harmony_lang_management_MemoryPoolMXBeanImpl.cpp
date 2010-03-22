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
 * @file org_apache_harmony_lang_management_MemoryPoolMXBeanImpl.cpp
 *
 * This file is a part of kernel class natives VM core component.
 * It contains implementation for native methods of
 * org.apache.harmony.lang.management.MemoryPoolMXBeanImpl class.
 */

#include <cxxlog.h>
#include <jni.h>
#include "org_apache_harmony_lang_management_MemoryPoolMXBeanImpl.h"
#include "environment.h"
#include "port_malloc.h"

/* Native methods */

/*
 * Method: org.apache.harmony.lang.management.MemoryPoolMXBeanImpl.getCollectionUsageImpl()Ljava/lang/management/MemoryUsage;
 */
JNIEXPORT jobject JNICALL
Java_org_apache_harmony_lang_management_MemoryPoolMXBeanImpl_getCollectionUsageImpl(JNIEnv *jenv_ext, jobject this_been)
{
    // TODO implement this method stub correctly
    TRACE2("management","getCollectionUsageImpl stub invocation");

    JNIEnv_Internal *jenv = (JNIEnv_Internal *)jenv_ext;

    jlong init = 1L<<21;
    jlong used = 1L<<20;
    jlong committed = 1L<<20;
    jlong max = 1L<<22;

    jclass memoryPoolMXBeanClazz =jenv->FindClass(
        "java/lang/management/MemoryPoolMXBean");
    if (jenv->ExceptionCheck()) {return NULL;};

    jmethodID getNameMethod = jenv->GetMethodID(
        memoryPoolMXBeanClazz,
        "getName",
        "()Ljava/lang/String;");
    if (jenv->ExceptionCheck()) {return NULL;};

    jobject jname = jenv->CallObjectMethod(this_been, getNameMethod);
    if (jenv->ExceptionCheck()) {return NULL;};

    jobject npname = jenv->NewStringUTF(NATIVE_POOL_NAME);
    if (jenv->ExceptionCheck()) {return NULL;};

    jclass javaLangStringClazz =jenv->FindClass(
        "java/lang/String");
    if (jenv->ExceptionCheck()) {return NULL;};

    jmethodID compareToMethod = jenv->GetMethodID(
        javaLangStringClazz,
        "compareTo",
        "(Ljava/lang/String;)I");
    if (jenv->ExceptionCheck()) {return NULL;};

    jint is_native = jenv->CallIntMethod(jname, compareToMethod, npname);
    if (jenv->ExceptionCheck()) {return NULL;};

    if (is_native==0) {
#ifdef _MEMMGR
        init = port_mem_used_size();
        used = port_mem_committed_size() - port_mem_reserved_size();
        committed = port_mem_committed_size();
        max = port_mem_max_size();
#endif
    }

    jclass memoryUsageClazz =jenv->FindClass("java/lang/management/MemoryUsage");
    if (jenv->ExceptionCheck()) return NULL;
    jmethodID memoryUsageClazzConstructor = jenv->GetMethodID(memoryUsageClazz, "<init>", "(JJJJ)V");
    if (jenv->ExceptionCheck()) return NULL;

    jobject memoryUsage = jenv->NewObject(memoryUsageClazz, memoryUsageClazzConstructor, init, used,
        committed, max);

    return memoryUsage;
};

jlong collection_usage_threshold = 5L;

/*
 * Method: org.apache.harmony.lang.management.MemoryPoolMXBeanImpl.getCollectionUsageThresholdImpl()J
 */
JNIEXPORT jlong JNICALL
Java_org_apache_harmony_lang_management_MemoryPoolMXBeanImpl_getCollectionUsageThresholdImpl(JNIEnv *, jobject){
    // TODO implement this method stub correctly
    TRACE2("management","getCollectionUsageThresholdImpl stub invocation");
    return collection_usage_threshold;
};

/*
 * Method: org.apache.harmony.lang.management.MemoryPoolMXBeanImpl.getCollectionUsageThresholdCountImpl()J
 */
JNIEXPORT jlong JNICALL
Java_org_apache_harmony_lang_management_MemoryPoolMXBeanImpl_getCollectionUsageThresholdCountImpl(JNIEnv *, jobject)
{
    // TODO implement this method stub correctly
    TRACE2("management","getCollectionUsageThresholdCountImpl stub invocation");
    return 7L;
};

/*
 * Method: org.apache.harmony.lang.management.MemoryPoolMXBeanImpl.getPeakUsageImpl()Ljava/lang/management/MemoryUsage;
 */
JNIEXPORT jobject JNICALL
Java_org_apache_harmony_lang_management_MemoryPoolMXBeanImpl_getPeakUsageImpl(JNIEnv * jenv_ext, jobject)
{
    // TODO implement this method stub correctly
    TRACE2("management","getPeakUsageImpl stub invocation");

    JNIEnv_Internal *jenv = (JNIEnv_Internal *)jenv_ext;

    jlong init = 1L<<21;
    jlong used = 1L<<20;
    jlong committed = 1L<<20;
    jlong max = 1L<<22;

    jclass memoryUsageClazz =jenv->FindClass("java/lang/management/MemoryUsage");
    if (jenv->ExceptionCheck()) return NULL;
    jmethodID memoryUsageClazzConstructor = jenv->GetMethodID(memoryUsageClazz, "<init>", "(JJJJ)V");
    if (jenv->ExceptionCheck()) return NULL;

    jobject memoryUsage = jenv->NewObject(memoryUsageClazz, memoryUsageClazzConstructor, init, used,
        committed, max);

    return memoryUsage;
};

/*
 * Method: org.apache.harmony.lang.management.MemoryPoolMXBeanImpl.getUsageImpl()Ljava/lang/management/MemoryUsage;
 */
JNIEXPORT jobject JNICALL
Java_org_apache_harmony_lang_management_MemoryPoolMXBeanImpl_getUsageImpl(JNIEnv * jenv_ext, jobject)
{
    // TODO implement this method stub correctly
    TRACE2("management","MemoryPoolMXBeanImpl_getUsageImpl stub invocation");

    JNIEnv_Internal *jenv = (JNIEnv_Internal *)jenv_ext;

    jlong init = 1L<<21;
    jlong used = 1L<<20;
    jlong committed = 1L<<20;
    jlong max = 1L<<22;

    jclass memoryUsageClazz =jenv->FindClass("java/lang/management/MemoryUsage");
    if (jenv->ExceptionCheck()) return NULL;
    jmethodID memoryUsageClazzConstructor = jenv->GetMethodID(memoryUsageClazz, "<init>", "(JJJJ)V");
    if (jenv->ExceptionCheck()) return NULL;

    jobject memoryUsage = jenv->NewObject(memoryUsageClazz, memoryUsageClazzConstructor, init, used,
        committed, max);

    return memoryUsage;
};

jlong usage_threshold = 5L;

/*
 * Method: org.apache.harmony.lang.management.MemoryPoolMXBeanImpl.getUsageThresholdImpl()J
 */
JNIEXPORT jlong JNICALL
Java_org_apache_harmony_lang_management_MemoryPoolMXBeanImpl_getUsageThresholdImpl(JNIEnv *, jobject)
{
    // TODO implement this method stub correctly
    TRACE2("management","MemoryPoolMXBeanImpl_getUsageThresholdImpl stub invocation");
    return usage_threshold;
};

/*
 * Method: org.apache.harmony.lang.management.MemoryPoolMXBeanImpl.getUsageThresholdCountImpl()J
 */
JNIEXPORT jlong JNICALL
Java_org_apache_harmony_lang_management_MemoryPoolMXBeanImpl_getUsageThresholdCountImpl(JNIEnv *, jobject)
{
    // TODO implement this method stub correctly
    TRACE2("management","MemoryPoolMXBeanImpl_getUsageThresholdCountImpl stub invocation");
    return 8L;
};

/*
 * Method: org.apache.harmony.lang.management.MemoryPoolMXBeanImpl.isCollectionUsageThresholdExceededImpl()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_lang_management_MemoryPoolMXBeanImpl_isCollectionUsageThresholdExceededImpl(JNIEnv *, jobject)
{
    // TODO implement this method stub correctly
    TRACE2("management","isCollectionUsageThresholdExceededImpl stub invocation");
    return JNI_FALSE;
};

/*
 * Method: org.apache.harmony.lang.management.MemoryPoolMXBeanImpl.isCollectionUsageThresholdSupportedImpl()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_lang_management_MemoryPoolMXBeanImpl_isCollectionUsageThresholdSupportedImpl(JNIEnv *, jobject)
{
    // TODO implement this method stub correctly
    TRACE2("management","isCollectionUsageThresholdSupportedImpl stub invocation");
    return JNI_TRUE;
};

/*
 * Method: org.apache.harmony.lang.management.MemoryPoolMXBeanImpl.isUsageThresholdExceededImpl()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_lang_management_MemoryPoolMXBeanImpl_isUsageThresholdExceededImpl(JNIEnv *, jobject)
{
    // TODO implement this method stub correctly
    TRACE2("management","isUsageThresholdExceededImpl stub invocation");
    return JNI_FALSE;
};

/*
 * Method: org.apache.harmony.lang.management.MemoryPoolMXBeanImpl.isUsageThresholdSupportedImpl()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_lang_management_MemoryPoolMXBeanImpl_isUsageThresholdSupportedImpl(JNIEnv *, jobject)
{
    // TODO implement this method stub correctly
    TRACE2("management","isUsageThresholdSupportedImpl stub invocation");
    return JNI_TRUE;
};

/*
 * Method: org.apache.harmony.lang.management.MemoryPoolMXBeanImpl.isValidImpl()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_lang_management_MemoryPoolMXBeanImpl_isValidImpl(JNIEnv *, jobject)
{
    // TODO implement this method stub correctly
    TRACE2("management","MemoryPoolMXBeanImpl_isValidImpl stub invocation");
    return JNI_TRUE;
};

/*
 * Method: org.apache.harmony.lang.management.MemoryPoolMXBeanImpl.resetPeakUsageImpl()V
 */
JNIEXPORT void JNICALL
Java_org_apache_harmony_lang_management_MemoryPoolMXBeanImpl_resetPeakUsageImpl(JNIEnv *, jobject)
{
    // TODO implement this method stub correctly
    TRACE2("management","MemoryPoolMXBeanImpl_resetPeakUsageImpl stub invocation");
};

/*
 * Method: org.apache.harmony.lang.management.MemoryPoolMXBeanImpl.setCollectionUsageThresholdImpl(J)V
 */
JNIEXPORT void JNICALL
Java_org_apache_harmony_lang_management_MemoryPoolMXBeanImpl_setCollectionUsageThresholdImpl(
    JNIEnv *, jobject, jlong newValue)
{
    // TODO implement this method stub correctly
    TRACE2("management","MemoryPoolMXBeanImpl_setCollectionUsageThresholdImpl stub invocation");
        collection_usage_threshold = newValue;
};

/*
 * Method: org.apache.harmony.lang.management.MemoryPoolMXBeanImpl.setUsageThresholdImpl(J)V
 */
JNIEXPORT void JNICALL
Java_org_apache_harmony_lang_management_MemoryPoolMXBeanImpl_setUsageThresholdImpl(
    JNIEnv *, jobject, jlong newValue)
{
    // TODO implement this method stub correctly
    TRACE2("management","MemoryPoolMXBeanImpl_setUsageThresholdImpl stub invocation");
        usage_threshold = newValue;
};
