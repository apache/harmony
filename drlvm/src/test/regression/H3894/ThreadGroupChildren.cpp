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
 * Test case for GetThreadGroupChildren() jvmti function.
 * Checks that the function may report more than 10 child theads.
 * Checks that the function reports the same thread group children that were
 * added by java application.
 */

#include <iostream>
#include <jvmti.h>

using namespace std;

#define PACKAGE "org/apache/harmony/drlvm/tests/regression/h3894/"

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

static bool check_array(jvmtiEnv* jvmti,
                              JNIEnv* jni,
                              jobject* array,
                              int array_length,
                              jobject object,
                              jclass clazz,
                              const char* field_name,
                              const char* field_sig)
{
    jfieldID field = jni->GetFieldID(clazz, field_name, field_sig);
    CHECK_JNI3(field, GetFieldID, false);

    jobjectArray expected = jni->GetObjectField(object, field);
    CHECK_JNI3(expected, GetObjectField, false);

    jint expected_length = jni->GetArrayLength(expected);
    CHECK_JNI3(expected_length, GetArrayLength, false);

    cerr << "Array length:   \t" << array_length << endl
            << "Expected length:\t" << expected_length << endl;

    if (array_length != expected_length) {
        cerr << "Array length doesn't match expected" << endl;
        return false;
    }

    for (jint j = 0; j < expected_length; j++) {
        jobject entry = jni->GetObjectArrayElement(expected, j);
        CHECK_JNI3(entry, GetObjectArrayElement, false);

        bool found = false;
        for (int i = 0; i < array_length; i++) {
            jboolean is_same = jni->IsSameObject(entry, array[i]);
            CHECK_JNI3(true, IsSameObject, false);

            if (is_same)
                found = ! found;
        }

        if (! found) {
            cerr << "Expected element not found" << endl;
            return false;
        }

    }

    return true;

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

    jfieldID testGroup_field = jni->GetFieldID(exn_class, "testGroup",
            "Ljava/lang/ThreadGroup;");
    CHECK_JNI(testGroup_field, GetFieldID);

    jthreadGroup group = jni->GetObjectField(exception, testGroup_field);
    CHECK_JNI(group, GetObjectField);

    jint thread_count;
    jthread* threads;
    jint group_count;
    jthreadGroup* groups;

    err = jvmti->GetThreadGroupChildren(group, &thread_count, &threads,
            &group_count, &groups);
    CHECK_RESULT(GetThreadGroupChildren);

    cerr << endl << "Child threads:" << endl;
    for (int i = 0; i < thread_count; i++) {
        jvmtiThreadInfo info;

        err = jvmti->GetThreadInfo(threads[i], &info);
        CHECK_RESULT(GetThreadInfo);

        cerr << i << ":\t" << info.name << endl;
    }

    cerr << endl << "Child groups:" << endl;
    for (int i = 0; i < group_count; i++) {
        jvmtiThreadGroupInfo info;

        err = jvmti->GetThreadGroupInfo(groups[i], &info);
        CHECK_RESULT(GetThreadDroupInfo);

        cerr << i << ":\t" << info.name << endl;
    }

    cerr << endl << "Checking threads..." << endl;
    if (! check_array(jvmti, jni, threads, thread_count, exception, exn_class,
            "childThreads", "[Ljava/lang/Thread;")) {
        return;
    }

    cerr << endl << "Checking groups..." << endl;
    if (! check_array(jvmti, jni, groups, group_count, exception, exn_class,
            "childGroups", "[Ljava/lang/ThreadGroup;")) {
        return;
    }


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

    err = jvmti->AddCapabilities(&capabilities);
    if (JVMTI_ERROR_NONE != err) {
        cerr << "[JvmtiAgent] ERROR: unable to possess capabilities" << endl;
        return -1;
    }

    // Agent initialized successfully
    return 0;
}
