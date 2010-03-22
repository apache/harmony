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
 * @file org_apache_harmony_lang_management_RuntimeMXBeanImpl.cpp
 *
 * This file is a part of kernel class natives VM core component.
 * It contains implementation for native methods of
 * org.apache.harmony.lang.management.RuntimeMXBeanImpl class.
 */

#include <apr_time.h>
#include <apr_network_io.h>
#include <cxxlog.h>
#include "environment.h"
#include "org_apache_harmony_lang_management_RuntimeMXBeanImpl.h"

/**
 * The number of digits in decimal print for max unsigned long
 */
#define MAX_LONG_LENGTH_AS_DECIMAL 20

/*
 * Method: org.apache.harmony.lang.management.RuntimeMXBeanImpl.getNameImpl()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_org_apache_harmony_lang_management_RuntimeMXBeanImpl_getNameImpl(JNIEnv *env, jobject)
{
    TRACE2("management", "RuntimeMXBeanImpl_getNameImpl called");
    JavaVM * vm = NULL;
    env->GetJavaVM(&vm);

    char host_name[APRMAXHOSTLEN + 1] = {0};
    apr_pool_t *pool;
    apr_pool_create(&pool, 0);
    apr_gethostname(host_name, APRMAXHOSTLEN + 1, pool);
    char result[MAX_LONG_LENGTH_AS_DECIMAL + 1 + APRMAXHOSTLEN + 1] = {0};
    sprintf(result, "%d@%s", getpid(), host_name);
    apr_pool_destroy(pool);
    return env->NewStringUTF(result);
};

/*
 * Method: org.apache.harmony.lang.management.RuntimeMXBeanImpl.getStartTimeImpl()J
 */
JNIEXPORT jlong JNICALL
Java_org_apache_harmony_lang_management_RuntimeMXBeanImpl_getStartTimeImpl(JNIEnv *env, jobject )
{
    TRACE2("management","RuntimeMXBeanImpl_getStartTimeImpl called");
    JavaVM * vm = NULL;
    env->GetJavaVM(&vm);
    return ((JavaVM_Internal*)vm)->vm_env->start_time;
};

/*
 * Method: org.apache.harmony.lang.management.RuntimeMXBeanImpl.getUptimeImpl()J
 */
JNIEXPORT jlong JNICALL
Java_org_apache_harmony_lang_management_RuntimeMXBeanImpl_getUptimeImpl(JNIEnv *env, jobject obj)
{
    TRACE2("management","RuntimeMXBeanImpl_getUptimeImpl called");
    return apr_time_now()/1000 -
        Java_org_apache_harmony_lang_management_RuntimeMXBeanImpl_getStartTimeImpl(env, obj);
};

/*
 * Method: org.apache.harmony.lang.management.RuntimeMXBeanImpl.isBootClassPathSupportedImpl()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_lang_management_RuntimeMXBeanImpl_isBootClassPathSupportedImpl(JNIEnv *, jobject)
{
    TRACE2("management","RuntimeMXBeanImpl_isBootClassPathSupportedImpl called");
    return JNI_TRUE;
};


