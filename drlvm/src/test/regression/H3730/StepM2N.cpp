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
 * Test case for Single Step through the M2N frame.
 * Test scenario:
 *   Test application:
 *     OUT_CLASS.OUT_METHOD () calls some native method X()
 *     X() calls BP_CLASS.BP_METHOD()
 *
 *     These calls could be indirect, i.e. method a() calls something that
 *     calls method b().
 *     OUT_METHOD() may call BP_METHOD() via reflection Mehtod.invoke().
 *     Because invoke() uses natie code in it's implementation.
 *
 *   Test agent:
 *     Agent tries to 'step out' from BP_METHOD() to OUT_METHOD() using the
 *     following steps.
 *
 *     1. Sets breakpoint onthe beginning of BP_METHOD()
 *     2. Recieves breakpoint event and requests Frame Pop event for the
 *      topmost frame.
 *     3. Recieves Frame Pop event and turns on Single Stepping.
 *     4. Recieves Single Step event and repeats steps 3..4 until Single Step
 *      occures in OUT_METHOD() (than test is considered passed) or until
 *      SS_LIMIT interations is made.
 */

#include <iostream>
#include <jvmti.h>

using namespace std;

#define PACKAGE "org/apache/harmony/drlvm/tests/regression/h3730/"

static const char* BP_CLASS = "L" PACKAGE "StepM2N;";
static const char* BP_METHOD_NAME = "inner";
static const char* BP_METHOD_SIG = "()V";

static const char* OUT_CLASS = "L" PACKAGE "StepM2N;";
static const char* OUT_METHOD_NAME = "outer";
static const char* OUT_METHOD_SIG = "()V";

static const int SS_LIMIT = 100;

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

#define JNI_CHECK(result, func) \
    if (NULL == (result)) { \
        cerr << "[JvmtiAgent] ERROR: " << #func << " failed." << endl;  \
        return; \
    }


static void JNICALL VMInit(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread)
{
    cerr << endl << "==> VM Init callback" << endl;

    TURN_EVENT(JVMTI_EVENT_CLASS_PREPARE, true);
}

static void JNICALL
ClassPrepare(jvmtiEnv *jvmti,
            JNIEnv* jni,
            jthread thread,
            jclass klass)
{
    jvmtiError err;

    char* class_name = NULL;
    err = jvmti->GetClassSignature(klass, &class_name, NULL);
    CHECK_RESULT(GetClassSignature);

    if (0 != strcmp(BP_CLASS, class_name))
        return;

    cerr << endl << "==> Class Prepare callback" << endl;
    cerr << "    for class: " << class_name << endl;

    TURN_EVENT(JVMTI_EVENT_CLASS_PREPARE, false);

    jint method_count = 0;
    jmethodID* methods = NULL;
    err = jvmti->GetClassMethods(klass, &method_count, &methods);
    CHECK_RESULT(GetClassMethods);

    jmethodID method = NULL;
    for (int i = 0; i < method_count; i++) {
//        cerr << "       method[" << i << "]: ";

        char* method_name = NULL;
        char* method_sig = NULL;
        err = jvmti->GetMethodName(methods[i], &method_name, &method_sig, NULL);
        CHECK_RESULT(GetMethodName);

//        cerr << method_name << " : " <<  method_sig << endl;
        if (0 == strcmp(BP_METHOD_NAME, method_name) &&
            0 == strcmp(BP_METHOD_SIG, method_sig)) {
            method = methods[i];
        }
    }

    err = jvmti->Deallocate((unsigned char*) methods);
    CHECK_RESULT(Deallocate);

    JNI_CHECK(method, find method);

//    jmethodID method = jni->GetMethodID(klass, "nop", "()V");
//    JNI_CHECK(method, GetMethodID);

    err = jvmti->SetBreakpoint(method, 0);
    CHECK_RESULT(SetBreakpoint);

    TURN_EVENT(JVMTI_EVENT_BREAKPOINT, true);

    cout << "    Breakpoint is set" << endl;
}

static void JNICALL
Breakpoint(jvmtiEnv *jvmti,
            JNIEnv* jni,
            jthread thread,
            jmethodID method,
            jlocation location)
{
    cerr << endl << "==> Breakpoint callback" << endl;
    jvmtiError err;

    TURN_EVENT(JVMTI_EVENT_BREAKPOINT, false);

    char* method_name = NULL;
    char* method_sig = NULL;
    err = jvmti->GetMethodName(method, &method_name, &method_sig, NULL);
    CHECK_RESULT(GetMethodName);

    cerr << "    at: " << method_name << method_sig << " :" << location << endl;
    if (! (0 == strcmp(BP_METHOD_NAME, method_name) &&
        0 == strcmp(BP_METHOD_SIG, method_sig))) {
        cerr << "[JvmtiAgent] ERROR: breakpoint in wrong method" << endl;
        return;
    }

    err = jvmti->NotifyFramePop(NULL, 0);
    CHECK_RESULT(NotifyFramePop);

    TURN_EVENT(JVMTI_EVENT_FRAME_POP, true);

    cout << "    Frame Pop is requested" << endl;
}

static void JNICALL
FramePop(jvmtiEnv *jvmti,
            JNIEnv* jni,
            jthread thread,
            jmethodID method,
            jboolean was_popped_by_exception)
{
    cerr << endl << "==> Frame Pop callback" << endl;
    jvmtiError err;

    TURN_EVENT(JVMTI_EVENT_FRAME_POP, false);

    char* method_name = NULL;
    char* method_sig = NULL;
    err = jvmti->GetMethodName(method, &method_name, &method_sig, NULL);
    CHECK_RESULT(GetMethodName);

    cerr << "    at: " << method_name << method_sig << endl;

//    if (! (0 == strcmp(BP_METHOD_NAME, method_name) &&
//        0 == strcmp(BP_METHOD_SIG, method_sig))) {
//        cerr << "[JvmtiAgent] ERROR: frame pop in wrong method" << endl;
//        return;
//    }

    TURN_EVENT(JVMTI_EVENT_SINGLE_STEP, true);

    cout << "    Single Step is requested" << endl;
}

static void JNICALL
SingleStep(jvmtiEnv *jvmti,
            JNIEnv* jni,
            jthread thread,
            jmethodID method,
            jlocation location)
{
    static int hit_count = 0;
    hit_count ++;

    cerr << endl << "==> Single Step callback" << endl;
    jvmtiError err;

    TURN_EVENT(JVMTI_EVENT_SINGLE_STEP, false);

    char* method_name = NULL;
    char* method_sig = NULL;
    err = jvmti->GetMethodName(method, &method_name, &method_sig, NULL);
    CHECK_RESULT(GetMethodName);

    jclass klass = NULL;
    err = jvmti->GetMethodDeclaringClass(method, &klass);
    CHECK_RESULT(GetMethodDeclaringClass);

    char* class_name = NULL;
    err = jvmti->GetClassSignature(klass, &class_name, NULL);
    CHECK_RESULT(GetClassSignature);

    cerr << "    at: " << class_name << "." << method_name
            << method_sig << " :" << location << endl;

    if (0 != strcmp(OUT_CLASS, class_name) ||
        0 != strcmp(OUT_METHOD_NAME, method_name) ||
        0 != strcmp(OUT_METHOD_SIG, method_sig) ) {

        if (hit_count > SS_LIMIT) {
            TURN_EVENT(JVMTI_EVENT_SINGLE_STEP, false);


            cout << "    " << SS_LIMIT
                    << " attempts failed to step out to desired locatioon:"
                    << endl << "    " <<OUT_CLASS << "." << OUT_METHOD_NAME
                    << OUT_METHOD_SIG << endl;
            return;
        }

        err = jvmti->NotifyFramePop(NULL, 0);
        CHECK_RESULT(NotifyFramePop);

        TURN_EVENT(JVMTI_EVENT_FRAME_POP, true);
        cout << "    Frame Pop is requested" << endl;
        cerr << "    continue popping frames..." << endl;
        return;
    } else {
        cerr << "    expected location reached" << endl;
        set_passed_state(jni);
        return;
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
    callbacks.ClassPrepare = ClassPrepare;
    callbacks.Breakpoint = Breakpoint;
    callbacks.FramePop = FramePop;
    callbacks.SingleStep = SingleStep;

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
    capabilities.can_generate_frame_pop_events = 1;
    capabilities.can_generate_breakpoint_events = 1;
    capabilities.can_generate_single_step_events = 1;

    err = jvmti->AddCapabilities(&capabilities);
    if (JVMTI_ERROR_NONE != err) {
        cerr << "[JvmtiAgent] ERROR: unable to possess capabilities" << endl;
        return -1;
    }

    // Agent initialized successfully
    return 0;
}
