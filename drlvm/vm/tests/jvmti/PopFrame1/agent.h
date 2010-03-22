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
 * @author Pavel Rebriy
 */

#ifndef _JVMTI_AGENT_H_
#define _JVMTI_AGENT_H_

#include <stdlib.h>
#include <string.h>
#include <iostream>
#include "jvmti.h"

#ifdef NDEBUG
#define DEBUG(str)
#else // NDEBUG
#define DEBUG(str)      std::cout << "DEBUG: " << str << std::endl << std::flush;
#endif // NDEBUG

#define REPORT(str)     std::cerr << "ERROR: " << str << std::endl << std::flush;
#define ERR_REPORT(str)                                                 \
    {                                                                   \
        std::cerr << "ERROR: " << str << std::endl << std::flush;       \
        error = true;                                                   \
    }

#define CHECK_ERROR()   \
    if(error) {         \
        return;         \
    }

void JNICALL
agent_callback_ThreadStart(jvmtiEnv* jvmti_env,
                           JNIEnv* jni_env,
                           jthread thread);

void JNICALL
agent_callback_Breakpoint( jvmtiEnv* jvmti_env,
                           JNIEnv* jni_env,
                           jthread thread,
                           jmethodID method,
                           jlocation location);

void JNICALL 
agent_callback_SingleStep( jvmtiEnv * jvmti_env,
                           JNIEnv * jni_env,
                           jthread thread,
                           jmethodID method,
                           jlocation location);

#endif // _JVMTI_AGENT_H_
