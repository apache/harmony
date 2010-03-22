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
 * Test case for Exception event location parameter.
 * Checks that location parameter for Exception is the same as location in
 * topmost stack frame.
*/

#include <iostream>
#include <jvmti.h>

using namespace std;

#define PACKAGE "org/apache/harmony/drlvm/tests/regression/h3698/"

static const char* EXCEPTION_CLASS = "L" PACKAGE "InvokeAgentException;";

static void set_passed_state(JNIEnv* jni)
{
    cerr << endl << "TEST PASSED" << endl << endl;

    jclass cl = jni->FindClass(PACKAGE "Status");
    if (NULL == cl) {
        cerr << "unable to find 'Status' class" << endl;
        return;
    }

    jfieldID fid = jni->GetStaticFieldID(cl, "status", "Z");
    if (NULL == fid) {
        cerr << "unable to find 'status' field" << endl;
        return;
    }

    jni->SetStaticBooleanField(cl, fid, JNI_TRUE);
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

#define TURN_EVENT(event, state) { \
    jvmtiError err = turn_event(jvmti, event, state, #event); \
    if (JVMTI_ERROR_NONE != err) return; \
}

#define CHECK_RESULT(func) \
    if (JVMTI_ERROR_NONE != err) { \
        cerr << "[JvmtiAgent] ERROR: " << #func << " failed with error: " << err << endl;  \
        return; \
    }

#define CHECK_JNI(result, func) \
    if (NULL == (result)) { \
        cerr << "[JvmtiAgent] ERROR: " << #func << " failed." << endl;  \
        return; \
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

    char* method_name = NULL;
    char* method_sig = NULL;
    err = jvmti->GetMethodName(method, &method_name, &method_sig, NULL);
    CHECK_RESULT(GetMethodName);

    cerr << "Exception location:\t" << method_name << method_sig
            << "\t: " << location << endl;;

    char* catch_method_name = NULL;
    char* catch_method_sig = NULL;
    err = jvmti->GetMethodName(catch_method, &catch_method_name, &catch_method_sig, NULL);
    CHECK_RESULT(GetMethodName);

    cerr << "Catch clause location:\t" << catch_method_name << catch_method_sig
            << "\t: " << catch_location << endl;;


    jint frame_count = 0;
    const jint max_frame_count = 1;
    jvmtiFrameInfo frames[max_frame_count];

    err = jvmti->GetStackTrace(NULL, 0, max_frame_count, frames, &frame_count);
    CHECK_RESULT(GetStackTrace);

    char *frame_method_name = NULL;
    char *frame_method_sig = NULL;
    err = jvmti->GetMethodName(frames[0].method, &frame_method_name, &frame_method_sig, NULL);
    CHECK_RESULT(GetMethodName);
    cerr << "Top frame location:\t" << frame_method_name << frame_method_sig
            << "\t: " << frames[0].location << endl;

    if (0 == strcmp(method_name, frame_method_name) &&
        0 == strcmp(method_sig, frame_method_sig) &&
        location == frames[0].location) {
        set_passed_state(jni);
    } else {
        cerr << endl << "Exception location doesn't match top frame location" << endl;
    }
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

    err = jvmti->AddCapabilities(&capabilities);
    if (JVMTI_ERROR_NONE != err) {
        cerr << "[JvmtiAgent] ERROR: unable to possess capabilities" << endl;
        return -1;
    }

    // Agent initialized successfully
    return 0;
}
