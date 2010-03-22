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

static bool error = false;

static void
set_test_success( jvmtiEnv* jvmti_env,
                  JNIEnv* jni_env)
{
    CHECK_ERROR();

    DEBUG("Setting test success...");
    jclass klass = jni_env->FindClass("PopFrame1/PopFrame1");
    if(!klass) {
        ERR_REPORT("get class");
        return;
    }
    jfieldID field = jni_env->GetStaticFieldID(klass, "status", "Z");
    if( !field ) {
        ERR_REPORT("get 'status' field");
        return;
    }
    jni_env->SetStaticBooleanField(klass, field, JNI_TRUE);
    return;
}

static void JNICALL
agent_function(jvmtiEnv * jvmti_env,
               JNIEnv * jni_env,
               void *args)
{
    CHECK_ERROR();

    DEBUG("agent: Getting java.lang.Thread ...");
    jclass klass = jni_env->FindClass("java/lang/Thread");
    if(!klass) {
        ERR_REPORT("get java.lang.Thread");
        return;
    }

    DEBUG("agent: Getting java.lang.Thread.yield() method...");
    jmethodID yield_method = jni_env->GetStaticMethodID(klass, "yield", "()V");
    if(!yield_method) {
        ERR_REPORT("get java.lang.Thread.yield() method");
        return;
    }

    jvmtiError result;
    int count = 0;
    bool test = false;
    jthread test_thread = (jthread)args;
    while( !test ) {
        DEBUG("agent: Checking test thread suspend state...");
        int index = 0;
        while(true) {
            jvmtiThreadInfo thread_info;
            result = jvmti_env->GetThreadInfo(test_thread, &thread_info);
            if(JVMTI_ERROR_NONE != result) {
                ERR_REPORT("GetThreadInfo");
                return;
            }

            jint state;
            result = jvmti_env->GetThreadState(test_thread, &state);
            if(JVMTI_ERROR_NONE != result) {
                ERR_REPORT("GetThreadInfo");
                return;
            }
            if(state & JVMTI_THREAD_STATE_SUSPENDED) {
                DEBUG("agent: Test thread is suspended!");
                break;
            }
            jni_env->CallStaticVoidMethod(klass, yield_method, NULL);
            if( !((++index) % 1000) ) {
                DEBUG("Stage = " << std::hex << state );
                DEBUG("agent: Waiting suspension of test thread...");
            }
        }

        DEBUG("agent: ----- " << ++count << " PopFrame call -----");
        result = jvmti_env->PopFrame(test_thread);
        if(JVMTI_ERROR_NONE != result) {
            if(JVMTI_ERROR_NO_MORE_FRAMES == result) {
                DEBUG("agent: PopFrame result is JVMTI_ERROR_NO_MORE_FRAMES!");
                test = true;
                break;
            } else {
                ERR_REPORT("PopFrame result is " << result);
                jvmti_env->ResumeThread(test_thread);
                return;
            }
        } else {
            DEBUG("agent: ...success!");
        }

        DEBUG("agent: Resume test thread");
        result = jvmti_env->ResumeThread(test_thread);
        if (result != JVMTI_ERROR_NONE) {
            ERR_REPORT("ResumeThread");
            return;
        }
    }

    if(!test || count != 3) {
        ERR_REPORT("Test failed! - unknown interruption");
        return;
    }

    DEBUG("agent: Removing SingleStep event...");
    result = jvmti_env->SetEventNotificationMode(JVMTI_DISABLE,
        JVMTI_EVENT_SINGLE_STEP, test_thread);
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("SingleStep: SetEventNotificationMode result is " << result );
        return;
    }

    set_test_success( jvmti_env, jni_env );
   
    DEBUG("agent: Resume test thread");
    result = jvmti_env->ResumeThread(test_thread);
    if (result != JVMTI_ERROR_NONE) {
        ERR_REPORT("ResumeThread");
        return;
    }

    DEBUG("agent: thread is done!");
    return;
}

static void
do_agent_notify(jvmtiEnv* jvmti_env)
{
    DEBUG("test: Suspending test thread...");
    jvmtiError result = jvmti_env->SuspendThread(NULL);
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("SuspendThread result is " << result );
        return;
    }

    DEBUG("test: Breakpoint callback is done!");
    return;
}

void JNICALL
agent_callback_Breakpoint( jvmtiEnv* jvmti_env,
                           JNIEnv* jni_env,
                           jthread thread,
                           jmethodID method,
                           jlocation location)
{
    CHECK_ERROR();

    char *name;
    char *descr;
    jvmtiError result = jvmti_env->GetMethodName(method, &name, &descr, NULL);
    if( result != JVMTI_ERROR_NONE
        || strcmp(name, "third_step") || strcmp(descr, "()V" ) )
    {
        return;
    }
    jclass klass;
    result = jvmti_env->GetMethodDeclaringClass(method, &klass);
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("get method class");
        return;
    }
    char *class_name;
    result = jvmti_env->GetClassSignature(klass, &class_name, NULL);
    if(result != JVMTI_ERROR_NONE
        || strcmp(class_name, "LPopFrame1/PopFrame1$1;"))
    {
        ERR_REPORT("wrong Breakpoint callback");
        return;
    }
    DEBUG("test: Breakpoint occupied in function PopFrame1.PopFrame1.third_step()");

    DEBUG("test: Clearing breakpoint...");
    result = jvmti_env->ClearBreakpoint(method, location);
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("clear breakpoint");
        return;
    }

    DEBUG("test: Setting SingleStep event...");
    result = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_SINGLE_STEP, thread);
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("SingleStep: SetEventNotificationMode result is " << result );
        return;
    }

    do_agent_notify(jvmti_env);

    DEBUG("test: Breakpoint callback is done!");
    return;
}

void JNICALL 
agent_callback_SingleStep( jvmtiEnv * jvmti_env,
                           JNIEnv * jni_env,
                           jthread thread,
                           jmethodID method,
                           jlocation location)
{
    CHECK_ERROR();

    char *name;
    char *descr;
    jvmtiError result = jvmti_env->GetMethodName(method, &name, &descr, NULL);
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("get method name");
        return;
    }
    jclass klass;
    result = jvmti_env->GetMethodDeclaringClass(method, &klass);
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("get method class");
        return;
    }
    char *class_name;
    result = jvmti_env->GetClassSignature(klass, &class_name, NULL);
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("get class name");
        return;
    }
    DEBUG("test: SingleStep occupied in function: " << name << descr
        << ", class: " << class_name);

    do_agent_notify(jvmti_env);

    DEBUG("test: SingleStep callback is done!");
    return;
}

void JNICALL
agent_callback_ThreadStart(jvmtiEnv* jvmti_env,
                           JNIEnv* jni_env,
                           jthread start_thread)
{
    jvmtiPhase phase;
    jvmtiError result = jvmti_env->GetPhase(&phase);
    if(phase != JVMTI_PHASE_LIVE) {
        return;
    }

    jvmtiThreadInfo thread_info;
    result = jvmti_env->GetThreadInfo(start_thread, &thread_info);
    if(JVMTI_ERROR_NONE != result) {
        ERR_REPORT("GetThreadInfo");
        return;
    }
    if (strcmp(thread_info.name, "Test thread")) {
        return;
    }
    DEBUG("Tested thread is started!");

    DEBUG("Creating AgentThread...");
    DEBUG("Getting java.lang.Thread ...");
    jclass klass = jni_env->FindClass("java/lang/Thread");
    if(!klass) {
        ERR_REPORT("get java.lang.Thread");
        return;
    }

    DEBUG("Getting java.lang.Thread contructor...");
    jmethodID method = jni_env->GetMethodID(klass, "<init>", "(Ljava/lang/String;)V");
    if(!method) {
        ERR_REPORT("get java.lang.Thread contructor");
        return;
    }

    DEBUG("Getting name string...");
    jstring name = jni_env->NewStringUTF("AgentThread");
    if(!name) {
        ERR_REPORT("get name string");
        return;
    }

    DEBUG("Creating agent thread...");
    jthread thread = jni_env->NewObject(klass, method, name);
    if(!thread) {
        ERR_REPORT("create thread");
        return;
    }

    DEBUG("Creating global reference for test thread...");
    jobject test_thread = jni_env->NewGlobalRef(start_thread);
    if(!test_thread) {
        ERR_REPORT("create thread");
        return;
    }

    DEBUG("Getting PopFrame1.PopFrame1$1 ...");
    klass = jni_env->FindClass("PopFrame1/PopFrame1$1");
    if(!klass) {
        ERR_REPORT("get PopFrame1.PopFrame1$1");
        return;
    }

    DEBUG("Getting PopFrame1.PopFrame1$1.third_step() method...");
    method = jni_env->GetMethodID(klass, "third_step", "()V");
    if(!method) {
        ERR_REPORT("get PopFrame1.PopFrame1$1.third_step() method");
        return;
    }

    DEBUG("Getting line number table...");
    jvmtiLineNumberEntry *table;
    jint number;
    result = jvmti_env->GetLineNumberTable( method, &number, &table );
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("get line number table, error = " << result);
        return;
    }

    jlocation location = 0; 
    for(jint index = 0; index < number - 1; index++) {
        if( table[index].line_number == 67) {
            location = table[index].start_location;
            break;
        }
    }

    DEBUG("Setting breakpint...")
    result = jvmti_env->SetBreakpoint( method, location );
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("set breakpoint");
        return;
    }

    DEBUG("Run agent thread...");
    result = jvmti_env->RunAgentThread(thread, agent_function,
        (void*)test_thread, JVMTI_THREAD_NORM_PRIORITY);
    if(result != JVMTI_ERROR_NONE) {
        ERR_REPORT("agent thread start");
        return;
    }

    return;
}
