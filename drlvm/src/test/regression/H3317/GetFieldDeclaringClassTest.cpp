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
 * This test checks that jvmti function GetFieldDeclaringClass() works for
 * inherited fields.
 */
#include <iostream>
#include <jvmti.h>

using namespace std;

#define PACKAGE "org/apache/harmony/drlvm/tests/regression/h3317/"

static void set_passed_state(JNIEnv* jni)
{
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

static void JNICALL VMInit(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread)
{
    jvmtiError err;

    // get child class
    jclass child = jni->FindClass(PACKAGE "Child");
    if (NULL == child) {
        cerr << "unable to find 'Child' class" << endl;
        return;
    }

    // get parent class
    jclass parent = jni->FindClass(PACKAGE "Parent");
    if (NULL == parent) {
        cerr << "unable to find 'Parent' class" << endl;
        return;
    }

    // get inherited field from child class
    jfieldID fid = jni->GetFieldID(child, "intField", "I");
    if (NULL == fid) {
        cerr << "unable to find the field" << endl;
        return;
    }

    // get field declaring class
    jclass declaring = NULL;

    err = jvmti->GetFieldDeclaringClass(child, fid, &declaring);
    if (JVMTI_ERROR_NONE != err) {
        cerr << "unable to get declaring class; ERROR: " << err << endl;
        return;
    }

    if (jni->IsSameObject(declaring, parent))
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

    // Agent initialized successfully
    return 0;
}
