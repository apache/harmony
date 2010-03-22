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

#include <iostream>
#include <jvmti.h>

using namespace std;

#define PACKAGE "org/apache/harmony/drlvm/tests/regression/h4512/"

static const char* EXCEPTION_CLASS = "L" PACKAGE "InvokeAgentException;";

#define TURN_EVENT(event, state) { \
    jvmtiError err = turn_event(jvmti, event, state, #event); \
    if (JVMTI_ERROR_NONE != err) return; \
}

#define CHECK_RESULT(func) \
    if (JVMTI_ERROR_NONE != err) { \
        cerr << "[JvmtiAgent] ERROR: " << #func << " failed with error: " << err << endl;  \
        return; \
    }

#define CHECK_JNI3(result, func, error_code) { \
    if (jni->ExceptionCheck()) { \
        cerr << "[JvmtiAgent] ERROR: unexpected exception in " << #func << endl;  \
        jni->ExceptionDescribe(); \
        return error_code; \
    } \
    if (! (result)) { \
        cerr << "[JvmtiAgent] ERROR: get NULL in " << #func << endl;  \
        return error_code; \
    } \
}

#define CHECK_JNI(result, func) CHECK_JNI3(result, func, )

static void set_passed_state(JNIEnv* jni)
{
    cerr << endl << "TEST PASSED" << endl << endl;

    jclass cl = jni->FindClass(PACKAGE "Status");
    CHECK_JNI(cl, FindClass);

    jfieldID testGroup_field = jni->GetStaticFieldID(cl, "status", "Z");
    CHECK_JNI(testGroup_field, GetStaticFieldID);

    jni->SetStaticBooleanField(cl, testGroup_field, JNI_TRUE);
    CHECK_JNI(true, SetStaticBooleanField);
}

static jvmtiError turn_event(jvmtiEnv* jvmti, jvmtiEvent event, bool state,
        const char* event_name)
{
    jvmtiError err;
    err = jvmti->SetEventNotificationMode(state ? JVMTI_ENABLE : JVMTI_DISABLE,
            event, NULL);
    if (JVMTI_ERROR_NONE != err) {
        cerr << "[JvmtiAgent] ERROR: unable to " << (state ? "en" : "dis")
                << "able " << event_name
                << endl;
    }

    return err;
}

static void JNICALL VMInit(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread)
{
    cerr << endl << "==> VM Init callback" << endl;

    TURN_EVENT(JVMTI_EVENT_EXCEPTION, true);
}

static void JNICALL
Exception(jvmtiEnv *jvmti,
            JNIEnv* jni,
            jthread thread,
            jmethodID method,
            jlocation location,
            jobject exception,
            jmethodID catch_method,
            jlocation catch_location)
{
    jvmtiError err;

    jclass exn_class = jni->GetObjectClass(exception);
    CHECK_JNI(exn_class, GetObjectClass);

    char* class_name = NULL;
    err = jvmti->GetClassSignature(exn_class, &class_name, NULL);
    CHECK_RESULT(GetClassSignature);

    if (0 != strcmp(EXCEPTION_CLASS, class_name))
        return;

    cerr << endl << "==> Exception callback" << endl;
    cerr << "    for class: " << class_name << endl;

    TURN_EVENT(JVMTI_EVENT_EXCEPTION, false);

    jlong nanos = 0;
    cerr << "    calling GetCurrentThreadCpuTime()..." << endl;

    err = jvmti->GetCurrentThreadCpuTime(&nanos);
    CHECK_RESULT(GetCurrentThreadCpuTime);

    cerr << "    ok." << endl;

    set_passed_state(jni);
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved)
{
    jvmtiEnv *jvmti = NULL;
    jvmtiError err;

    // Get JVMTI interface pointer
    jint iRes = vm->GetEnv((void**)&jvmti, JVMTI_VERSION);
    if (JNI_OK != iRes) {
        cerr << "[JvmtiAgent] ERROR: unable to get JVMTI environment" << endl;
        return -1;
    }

    // Set events callbacks
    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(jvmtiEventCallbacks));

    callbacks.VMInit = VMInit;
    callbacks.Exception = Exception;

    err = jvmti->SetEventCallbacks(&callbacks, sizeof(jvmtiEventCallbacks));
    if (JVMTI_ERROR_NONE != err) {
        cerr << "[JvmtiAgent] ERROR: unable to register event callbacks" << endl;
        return -1;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_VM_INIT, NULL);
    if (JVMTI_ERROR_NONE != err) {
        cerr << "[JvmtiAgent] ERROR: unable to enable VMInit event"
                << endl;
        return -1;
    }

    // Set capabilities
    jvmtiCapabilities capabilities;
    memset(&capabilities, 0, sizeof(jvmtiCapabilities));
    capabilities.can_generate_exception_events = 1;
    capabilities.can_get_current_thread_cpu_time = 1;

    err = jvmti->AddCapabilities(&capabilities);
    if (JVMTI_ERROR_NONE != err) {
        cerr << "[JvmtiAgent] ERROR: unable to possess capabilities" << endl;
        return -1;
    }

    // Agent initialized successfully
    return 0;
}
