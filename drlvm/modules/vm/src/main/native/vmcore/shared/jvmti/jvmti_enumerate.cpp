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
 * @file
 * This file implements enumeration
 * of global references controlled by the TI
 */

#define LOG_DOMAIN "ti.enum"
#include "cxxlog.h"

#include "jvmti.h"
#include "jvmti_internal.h"
#include "jvmti_utils.h"
#include "jvmti_tags.h"


// enumerates tags for one TI environment
static void enumerate_env(TIEnv* env)
{
    if (env->tags != NULL) {
        env->tags->enumerate();
    }
}


// traverses the list of TI environments
// and enumerates the global refs for each 
void DebugUtilsTI::enumerate()
{
    TIEnv* env = p_TIenvs;
    while (env != NULL) {
        enumerate_env(env);
        env = env->next;
    }
}
