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
 * Test case for AttachCurrentThread() Invocation API function.
 * Checkss that:
 * 1. Native thread can attach to vm with MethodEntry/Exit events turned on.
 * 2. Native thread can attach again after it was detached.
 * Test scenario:
 * 1. From exception callback creates native thread.
 * 2. Native thread attaches to vm and detaches 2 times.
 */

#include <iostream>
#include <string.h>
#include <jvmti.h>

using namespace std;

#define PACKAGE "org/apache/harmony/drlvm/tests/regression/h4979/"

static const char* EXCEPTION_CLASS = "L" PACKAGE "InvokeAgentException;";

static char* ATTACHED_THREAD_NAME = "attached_thread";
static const jint MAX_FRAME_COUNT = 100;
static JavaVM* java_vm = NULL;

static bool passed = false;
static bool finished = false;

static unsigned long method_entry_count = 0;
static unsigned long method_exit_count = 0;

static bool start_native_thread();

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

static bool thread_attach_detach(const char* msg)
{
    cerr << "[native thread] attaching " << msg << "..." << endl;

    JavaVMAttachArgs attach_args = {JNI_VERSION_1_2, ATTACHED_THREAD_NAME,
        NULL};

    JNIEnv* jni;
    if (0 != java_vm->AttachCurrentThread((void**) &jni,
            (void*) &attach_args)) {
        cerr << "[native thread] ERROR: unable to attach the thread" << endl;
        return false;
    }

    cerr << "[native thread] detaching " << msg << "..." << endl;

    if (0 != java_vm->DetachCurrentThread()) {
        cerr << "[native thread] ERROR: unable to detach the thread" << endl;
        return false;
    }

    return true;
}

static void thread_func(void* arg)
{
    cerr << endl << "[native thread] thread stated" << endl << endl;

    bool status =
            thread_attach_detach("first time") &&
            thread_attach_detach("second time");

    cerr << endl << "[native thread] thread finishing" << endl << endl;

    passed = status;
    finished = true;
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

    TURN_EVENT(JVMTI_EVENT_EXCEPTION, false);

    cerr << "starting native thread..." << endl << flush;

    if (! start_native_thread()) {
        cerr << "[JvmtiAgent] ERROR: unable to run native thread" << endl;
        return;
    }

    cerr << "wait for native thraad to finish..." << endl << flush;

    while (! finished);

    cerr << "method_entry_count = " << method_entry_count << endl;
    cerr << "method_exit_count = " << method_exit_count << endl;

    if (passed)
        set_passed_state(jni);
}

static void JNICALL
MethodEntry(jvmtiEnv *jvmti,
            JNIEnv* jni,
            jthread thread,
            jmethodID method)
{
    method_entry_count ++;
}

static void JNICALL
MethodExit(jvmtiEnv *jvmti,
            JNIEnv* jni,
            jthread thread,
            jmethodID method,
            jboolean was_popped_by_exception,
            jvalue return_value)
{
    method_exit_count ++;
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved)
{
    java_vm = vm;
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

    callbacks.Exception = Exception;
    callbacks.MethodEntry = MethodEntry;
    callbacks.MethodExit = MethodExit;

    err = jvmti->SetEventCallbacks(&callbacks, sizeof(jvmtiEventCallbacks));
    if (JVMTI_ERROR_NONE != err) {
        cerr << "[JvmtiAgent] ERROR: unable to register event callbacks" << endl;
        return -1;
    }

    // Set capabilities
    jvmtiCapabilities capabilities;
    memset(&capabilities, 0, sizeof(jvmtiCapabilities));
    capabilities.can_generate_exception_events = 1;
    capabilities.can_generate_method_entry_events = 1;
    capabilities.can_generate_method_exit_events = 1;

    err = jvmti->AddCapabilities(&capabilities);
    if (JVMTI_ERROR_NONE != err) {
        cerr << "[JvmtiAgent] ERROR: unable to possess capabilities" << endl;
        return -1;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_EXCEPTION, NULL);
    if (JVMTI_ERROR_NONE != err) {
        cerr << "[JvmtiAgent] ERROR: unable to enable Exception event"
                << endl;
        return -1;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_METHOD_ENTRY, NULL);
    if (JVMTI_ERROR_NONE != err) {
        cerr << "[JvmtiAgent] ERROR: unable to enable MethodEntry event"
                << endl;
        return -1;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_METHOD_EXIT, NULL);
    if (JVMTI_ERROR_NONE != err) {
        cerr << "[JvmtiAgent] ERROR: unable to enable MethodExit event"
                << endl;
        return -1;
    }

    // Agent initialized successfully
    return 0;
}

#if defined WIN32 || defined _WIN32

#include <process.h>

static bool start_native_thread()
{
    uintptr_t handle = _beginthread(&thread_func, 0, NULL);

    if (handle == 0 || handle == 1 || handle == -1)
        return false;

    return true;
}

#else

static bool start_native_thread()
{
    pthread_t thread;
    int r;

    r = pthread_create(&thread, NULL, (void*(*)(void*))thread_func, NULL);

    if (0 != r)
        return false;

    return true;
}

#endif
//
