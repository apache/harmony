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
 * @author Mikhail Fursov
 */  

/**
 * @file java_lang_String.cpp
 *
 * This file is a part of kernel class natives VM core component.
 * It contains implementation for native methods of java.lang.EMThread kernel 
 * class.
 */


#include "java_lang_EMThreadSupport.h"

#include "environment.h"
#include "compile.h"


JNIEXPORT jboolean JNICALL Java_java_lang_EMThreadSupport_needProfilerThreadSupport
    (JNIEnv *jenv, jclass cls) 
{
    return (jboolean) strcmp("false", 
        VM_Global_State::loader_env->em_component->
        GetProperty(OPEN_EM_VM_PROFILER_NEEDS_THREAD_SUPPORT));
}


JNIEXPORT void JNICALL 
Java_java_lang_EMThreadSupport_onTimeout(JNIEnv *jenv, jclass cls) 
{
    return VM_Global_State::loader_env->em_interface->ProfilerThreadTimeout();
}


JNIEXPORT jint JNICALL 
Java_java_lang_EMThreadSupport_getTimeout(JNIEnv *jenv, jclass cls) 
{
    const char* timeout_string = VM_Global_State::loader_env->em_component->
        GetProperty(OPEN_EM_VM_PROFILER_THREAD_TIMEOUT);
    return atoi(timeout_string);
}



