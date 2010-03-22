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

#include <jvmti.h>
#include <string.h>

static const struct ExceptionEvents
{
    const char *throw_method_name;
    const char *catch_method_name;
    const char *exception_class_name;
    const jlocation throw_location;
    const jlocation catch_location;
} exception_locations[] =
{
    {"f1", "f1", "Lorg/apache/harmony/drlvm/tests/regression/h2926/MyException;", 9, 10},
    {"f2", "f2", "Ljava/lang/NullPointerException;", 3, 10},
    {"f5", "f3", "Lorg/apache/harmony/drlvm/tests/regression/h2926/MyException;", 9, 7},
    {"f6", "f4", "Ljava/lang/NullPointerException;", 3, 7}
};

static void JNICALL exception_callback(jvmtiEnv *jvmti_env,
    JNIEnv* jni_env,
    jthread thread,
    jmethodID throw_method,
    jlocation throw_location,
    jobject exception,
    jmethodID catch_method,
    jlocation catch_location)
{
    static bool testing_started = false;
    static int count = 0;
    jvmtiError status;

    char *throw_method_name;
    char *catch_method_name;
    char *exception_class_name;

    status = jvmti_env->GetMethodName(throw_method, &throw_method_name, NULL, NULL);
    if (JVMTI_ERROR_NONE != status)
        return;

    status = jvmti_env->GetMethodName(catch_method, &catch_method_name, NULL, NULL);
    if (JVMTI_ERROR_NONE != status)
        return;

    jclass exception_class = jni_env->GetObjectClass(exception);
    status = jvmti_env->GetClassSignature(exception_class,
        &exception_class_name, NULL);
    if (JVMTI_ERROR_NONE != status)
        return;

    if ((count < sizeof(exception_locations) / sizeof(ExceptionEvents)) &&
        (0 ==
            strcmp(throw_method_name, exception_locations[count].throw_method_name)) &&
        (0 ==
            strcmp(catch_method_name, exception_locations[count].catch_method_name)) &&
        (0 ==
            strcmp(exception_class_name, exception_locations[count].exception_class_name)) &&
        (throw_location == exception_locations[count].throw_location) &&
        (catch_location == exception_locations[count].catch_location)
)
    {
        printf("Exception event received: %s(%d)->%s->%s(%d)\n",
            throw_method_name,
            (int)throw_location,
            exception_class_name,
            catch_method_name,
            (int)catch_location);

        testing_started = true;

        if (count < sizeof(exception_locations) / sizeof(ExceptionEvents))
            count++;

        if (count == sizeof(exception_locations) / sizeof(ExceptionEvents))
        {
            printf("Success\n");
            testing_started = false;
            jclass cl = jni_env->FindClass("org/apache/harmony/drlvm/tests/regression/h2926/Status");
            if (NULL == cl)
                return;

            jfieldID fid = jni_env->GetStaticFieldID(cl, "status", "Z");
            if (NULL == fid)
                return;

            jni_env->SetStaticBooleanField(cl, fid, JNI_TRUE);

            jvmti_env->SetEventNotificationMode(JVMTI_DISABLE,
                JVMTI_EVENT_EXCEPTION, NULL);
        }
    }
    else if (testing_started)
    {
        printf("Exception event number %d failed test\n", count);
        printf("Exception event received: %s(%d)->%s->%s(%d)\n",
            throw_method_name,
            (int)throw_location,
            exception_class_name,
            catch_method_name,
            (int)catch_location);
        printf("Should be: %s(%d)->%s->%s(%d)\n",
            exception_locations[count].throw_method_name,
            (int)exception_locations[count].throw_location,
            exception_locations[count].exception_class_name,
            exception_locations[count].catch_method_name,
            (int)exception_locations[count].catch_location);
        jvmti_env->SetEventNotificationMode(JVMTI_DISABLE,
            JVMTI_EVENT_EXCEPTION, NULL);
    }

    jvmti_env->Deallocate((unsigned char *)throw_method_name);
    jvmti_env->Deallocate((unsigned char *)catch_method_name);
    jvmti_env->Deallocate((unsigned char *)exception_class_name);
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved)
{
    jvmtiEnv *jvmti_env;
    if(vm->GetEnv((void**)&jvmti_env, JVMTI_VERSION_1_0) != JNI_OK)
        return JNI_ERR;

    // Set exception event capability
    jvmtiCapabilities capabilities;
    memset(&capabilities, 0, sizeof(jvmtiCapabilities));
    capabilities.can_generate_exception_events = 1;

    jvmtiError result = jvmti_env->AddCapabilities(&capabilities);
    if (JVMTI_ERROR_NONE != result)
        return JNI_ERR;

    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(jvmtiEventCallbacks));
    callbacks.Exception = exception_callback;

    result = jvmti_env->SetEventCallbacks(&callbacks, (jint)sizeof(callbacks));
    if (JVMTI_ERROR_NONE != result)
        return JNI_ERR;

    // Set event mode to true
    result = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_EXCEPTION, NULL);
    if (JVMTI_ERROR_NONE != result)
        return JNI_ERR;

    return JNI_OK;
}
