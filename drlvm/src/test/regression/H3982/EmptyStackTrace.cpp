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
 * Test case for GetStackTrace() function on threads with empty stack.
 * 1. Creates native thread.
 * 2. Attaches it to vm.
 * 3. From exception callback retrievs all threads and finds the attached one.
 * 4. Calls GetStackTrace() for the attached thread. It should return success
 * code as RI does.
 */

#include <iostream>
#include <string.h>
#include <jvmti.h>

using namespace std;

#define PACKAGE "org/apache/harmony/drlvm/tests/regression/h3982/"

static const char* EXCEPTION_CLASS = "L" PACKAGE "InvokeAgentException;";

static char* ATTACHED_THREAD_NAME = "attached_thread";
static const jint MAX_FRAME_COUNT = 100;
static JavaVM* java_vm = NULL;

static bool attached = false;
static bool finish = false;

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

static void thread_func(void* arg)
{
    cerr << endl << "native thread stated" << endl << endl;

    JavaVMAttachArgs attach_args = {JNI_VERSION_1_2, ATTACHED_THREAD_NAME, NULL};

    JNIEnv* jni;
    if (0 != java_vm->AttachCurrentThread((void**) &jni,
            (void*) &attach_args)) {
        cerr << "[native thread] ERROR: unable to attach the thread" << endl;
        return;
    }

    attached = true;

    int a = 9;

    while (! finish);

    if (0 != java_vm->DetachCurrentThread()) {
        cerr << "[native thread] ERROR: unable to detach the thread" << endl;
        return;
    }


    cerr << endl << "native thread finished" << endl << endl;
}


static void JNICALL VMInit(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread)
{
    cerr << endl << "==> VM Init callback" << endl;

    cerr << "starting native thread..." << endl << flush;

    if (! start_native_thread()) {
        cerr << "[JvmtiAgent] ERROR: unable to run native thread" << endl;
        return;
    }

    TURN_EVENT(JVMTI_EVENT_EXCEPTION, true);

    cerr << endl << "==> VM Init finished" << endl << flush;
}

static void check_stack_trace(jvmtiEnv *jvmti, JNIEnv* jni)
{
    jvmtiError err;

    if (! attached) {
        cerr << "wait for native thraad to attach..." << endl << flush;
        while (! attached);
    }

    jint threads_count;
    jthread* threads;

    err = jvmti->GetAllThreads(&threads_count, &threads);
    CHECK_RESULT(GetAllThreads);

    int attached_theads_count = 0;
    jthread attached_thread;

    cerr << endl << "All threads:" << endl;
    for (int i = 0; i < threads_count; i++) {
        jvmtiThreadInfo thread_info;

        err = jvmti->GetThreadInfo(threads[i], &thread_info);
        CHECK_RESULT(GetThreadInfo);


        jvmtiThreadGroupInfo group_info;

        err = jvmti->GetThreadGroupInfo(thread_info.thread_group, &group_info);
        CHECK_RESULT(GetThreadDroupInfo);

        cerr << i << ":\t" << thread_info.name << "\tin group: "
                << group_info.name << endl;

        if (0 == strcmp(ATTACHED_THREAD_NAME, thread_info.name)) {
            attached_theads_count ++;
            attached_thread = threads[i];
        }
    }

    if (attached_theads_count != 1) {
        cerr << "FAILED: found " << attached_theads_count
                << " attached threads; expected 1" << endl;
        return;
    }

    jvmtiFrameInfo frames[MAX_FRAME_COUNT];
    jint frame_count;

    err = jvmti->GetStackTrace(attached_thread, 0, MAX_FRAME_COUNT, frames,
            &frame_count);
    CHECK_RESULT(GetStackTrace);

    cerr << endl << "Atached thread  stack [" << frame_count << "]" << endl;
    for (int i = 0; i < frame_count; i++) {
        char* method_name;
        char* method_sig;

        err = jvmti->GetMethodName(frames[i].method, &method_name, &method_sig,
                NULL);
        CHECK_RESULT(GetMethodName);

        cerr << i << ":\t" << method_name << method_sig << endl;

    }

    set_passed_state(jni);

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

    check_stack_trace(jvmti, jni);

    finish = true;
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
