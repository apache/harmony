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

#include "agent.h"

#define CHECK_CAP(obtained, needed, jvmti_cap)              \
    if(!obtained.jvmti_cap) {                               \
    REPORT( "Cannot set capability: "#jvmti_cap );          \
        return JNI_ERR;                                     \
    }                                                       \
    needed.jvmti_cap = 1;

static inline jint
set_agent_capabilities(jvmtiEnv * jvmti_env)
{
    // get VM capabilities
    jvmtiCapabilities vm_cap;
    jvmtiError result = jvmti_env->GetPotentialCapabilities(&vm_cap);
    if (result != JVMTI_ERROR_NONE) {
        REPORT( "get potential capabilities" );
        return JNI_ERR;
    }

    jvmtiCapabilities need_cap = {0};
    CHECK_CAP(vm_cap, need_cap, can_generate_breakpoint_events);
    CHECK_CAP(vm_cap, need_cap, can_generate_single_step_events);
    CHECK_CAP(vm_cap, need_cap, can_suspend);
    CHECK_CAP(vm_cap, need_cap, can_pop_frame);
    CHECK_CAP(vm_cap, need_cap, can_get_line_numbers);

    result = jvmti_env->AddCapabilities(&need_cap);
    if (result != JVMTI_ERROR_NONE) {
        REPORT( "set needed capabilities" );
        return JNI_ERR;
    }
    return JNI_OK;
}

static inline jint
set_agent_events(jvmtiEnv * jvmti_env)
{
    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(callbacks));

    // set event callbacks
    callbacks.ThreadStart = &agent_callback_ThreadStart;
    callbacks.Breakpoint = &agent_callback_Breakpoint;
    callbacks.SingleStep = &agent_callback_SingleStep;
    jvmtiError result = jvmti_env->SetEventCallbacks(&callbacks, (jint)sizeof(callbacks));
    if( result != JVMTI_ERROR_NONE ) {
        REPORT("set events callbacks");
        return JNI_ERR;
    }

    // set ThreadStart event
    result = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_START, NULL);
    if( result != JVMTI_ERROR_NONE ) {
        REPORT("enable ThreadStart event");
        return JNI_ERR;
    }
    // set Breakpoint event
    result = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_BREAKPOINT, NULL);
    if( result != JVMTI_ERROR_NONE ) {
        REPORT("enable Breakpoint event");
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *vm, char *options, void *reserved)
{
    // get JVMTI enviroment
    jvmtiEnv * jvmti_env;
    DEBUG("Getting JVMTI enviroment... ");
    if( vm->GetEnv( (void**)&jvmti_env, JVMTI_VERSION_1_0) != JNI_OK ) {
        REPORT( "get JVMTI enviroment" );
        return JNI_OK;
    }
    
    // set capabilities
    DEBUG("Setting capabilities... ");
    if( set_agent_capabilities(jvmti_env) != JNI_OK ) {
        return JNI_OK;
    }

    // set events
    DEBUG("Setting events... ");
    if( set_agent_events(jvmti_env) != JNI_OK ) {
        return JNI_OK;
    }

    return JNI_OK;
}
