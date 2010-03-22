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
 * @file org_apache_harmony_lang_management_ClassLoadingMXBeanImpl.cpp
 *
 * This file is a part of kernel class natives VM core component.
 * It contains implementation for native methods of
 * org.apache.harmony.lang.management.ClassLoadingMXBeanImpl class.
 */
#define LOG_DOMAIN "management"
#include <cxxlog.h>
#include "environment.h"
#include "org_apache_harmony_lang_management_ClassLoadingMXBeanImpl.h"
/*
 * Class:     org_apache_harmony_lang_management_ClassLoadingMXBeanImpl
 * Method:    getLoadedClassCountImpl
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_lang_management_ClassLoadingMXBeanImpl_getLoadedClassCountImpl
(JNIEnv * env, jobject this_bean)
{
    TRACE("ClassLoadingMXBeanImpl_getLoadedClassCountImpl invocation");
    return (jint)
        (Java_org_apache_harmony_lang_management_ClassLoadingMXBeanImpl_getTotalLoadedClassCountImpl(env, this_bean)
        - Java_org_apache_harmony_lang_management_ClassLoadingMXBeanImpl_getUnloadedClassCountImpl(env, this_bean));
}

/*
 * Class:     org_apache_harmony_lang_management_ClassLoadingMXBeanImpl
 * Method:    getTotalLoadedClassCountImpl
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_lang_management_ClassLoadingMXBeanImpl_getTotalLoadedClassCountImpl
(JNIEnv * env, jobject)
{
    TRACE("ClassLoadingMXBeanImpl_getTotalLoadedClassCountImpl invocation");
    JavaVM * vm = NULL;
    env->GetJavaVM(&vm);
    return ((JavaVM_Internal*)vm)->vm_env->total_loaded_class_count;
}

/*
 * Class:     org_apache_harmony_lang_management_ClassLoadingMXBeanImpl
 * Method:    getUnloadedClassCountImpl
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_lang_management_ClassLoadingMXBeanImpl_getUnloadedClassCountImpl
(JNIEnv * env, jobject)
{
    TRACE("ClassLoadingMXBeanImpl_getUnloadedClassCountImpl invocation");
    JavaVM * vm = NULL;
    env->GetJavaVM(&vm);
    return ((JavaVM_Internal*)vm)->vm_env->unloaded_class_count;
}

/*
 * Class:     org_apache_harmony_lang_management_ClassLoadingMXBeanImpl
 * Method:    isVerboseImpl
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_apache_harmony_lang_management_ClassLoadingMXBeanImpl_isVerboseImpl
(JNIEnv * env, jobject)
{
    TRACE("ClassLoadingMXBeanImpl_isVerboseImpl invocation");
    return log_is_info_enabled(LOG_CLASS_INFO);
}

/*
 * Class:     org_apache_harmony_lang_management_ClassLoadingMXBeanImpl
 * Method:    setVerboseImpl
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_lang_management_ClassLoadingMXBeanImpl_setVerboseImpl
(JNIEnv * env, jobject, jboolean value)
{
    TRACE("ClassLoadingMXBeanImpl_setVerboseImpl invocation");
    if (value) {
        log_enable_info_category(LOG_CLASS_INFO, 0);
    } else {
        log_disable_info_category(LOG_CLASS_INFO, 0);
    }
}

