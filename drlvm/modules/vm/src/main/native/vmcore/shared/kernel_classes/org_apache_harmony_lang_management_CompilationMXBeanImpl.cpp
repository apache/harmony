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
 * @file org_apache_harmony_lang_management_CompilationMXBeanImpl.cpp
 *
 * This file is a part of kernel class natives VM core component.
 * It contains implementation for native methods of
 * org.apache.harmony.lang.management.CompilationMXBeanImpl class.
 */

#include <cxxlog.h>
#include "interpreter.h"
#include "environment.h"
#include "org_apache_harmony_lang_management_CompilationMXBeanImpl.h"
/*
 * Class:     org_apache_harmony_lang_management_CompilationMXBeanImpl
 * Method:    isJITEnabled
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_apache_harmony_lang_management_CompilationMXBeanImpl_isJITEnabled
(JNIEnv *, jclass)
{
    TRACE2("management","CompilationMXBeanImpl_isJITEnabled called");
    return interpreter_enabled() ? JNI_FALSE : JNI_TRUE;
};

/*
 * Class:     org_apache_harmony_lang_management_CompilationMXBeanImpl
 * Method:    getTotalCompilationTimeImpl
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_lang_management_CompilationMXBeanImpl_getTotalCompilationTimeImpl
(JNIEnv * env, jobject)
{
    TRACE2("management","CompilationMXBeanImpl_getTotalCompilationTimeImpl called");
    JavaVM * vm = NULL;
    env->GetJavaVM(&vm);
    return ((JavaVM_Internal*)vm)->vm_env->total_compilation_time;
};

/*
 * Class:     org_apache_harmony_lang_management_CompilationMXBeanImpl
 * Method:    isCompilationTimeMonitoringSupportedImpl
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_apache_harmony_lang_management_CompilationMXBeanImpl_isCompilationTimeMonitoringSupportedImpl
(JNIEnv *, jobject)
{
    TRACE2("management","CompilationMXBeanImpl_isCompilationTimeMonitoringSupportedImpl called");
    return JNI_TRUE;
};

