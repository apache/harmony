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
 * @author Valentin Al. Sitnick
 *
 */


#ifndef _UTILS_H_
#define _UTILS_H_

#ifdef LINUX
#include <unistd.h>
#else
#include <windows.h>
#endif

#include "jvmti.h"
#include "events.h"
#include <string.h>
#include <math.h>
#include <stdlib.h>



#define FREE_TINFO  jvmti_env->Deallocate((unsigned char*)(tinfo.name));

#define FREE_TLIST  jvmti_env->Deallocate((unsigned char*)(threads));

#define FREE_CLIST  jvmti_env->Deallocate((unsigned char*)(classes));

#define FREE_MLIST  jvmti_env->Deallocate((unsigned char*)(methods));

#define FREE_FLIST  jvmti_env->Deallocate((unsigned char*)(fids));

#define FREE_VARIABLE_TABLE_EL                                             \
    jvmti_env->Deallocate((unsigned char*)table[i].name);                  \
    jvmti_env->Deallocate((unsigned char*)table[i].signature);             \
    jvmti_env->Deallocate((unsigned char*)table[i].generic_signature);

#define FREE_VARIABLE_TABLE                                                \
    jvmti_env->Deallocate((unsigned char*)table);

#define FREE_METHOD_NAME_OUT                                               \
    jvmti_env->Deallocate((unsigned char*)name);                           \
    jvmti_env->Deallocate((unsigned char*)sign);                           \
    jvmti_env->Deallocate((unsigned char*)generic);

/***************************************************************************/

#define AGENT_JVMTI_ENV_INIT {                                             \
    /* initialization of JVMTI environment */                              \
    {                                                                      \
        jint res = JNI_OK;                                                 \
                                                                           \
        res = vm->GetEnv((void**)&jvmti, JVMTI_VERSION_1_0);               \
                                                                           \
        if (res != JNI_OK) {                                               \
           printf ("Failure of initialization of JVMTI environment: %d\n", \
                   (int)res);                                              \
           return res;                                                     \
        }                                                                  \
    }                                                                      \
}

/***************************************************************************/

#define AGENT_JVMTI_SET_JAVA_LIB_PATH_WINDOWS {                            \
    /* adding .dll & .so directories to "java.library.path" */             \
    {                                                                      \
        results[0] = jvmti->SetSystemProperty(                             \
                "java.library.path", ".\\dll\\.");                         \
        if  (results[0] != JVMTI_ERROR_NONE) {                             \
            printf("Failure of adding library path : %d \n", results[0]);  \
            return JNI_ERR;                                                \
        }                                                                  \
    }                                                                      \
}

/***************************************************************************/

#define AGENT_JVMTI_SET_JAVA_LIB_PATH_LINUX {                              \
    /* adding .dll & .so directories to "java.library.path" */             \
    {                                                                      \
        results[0] = jvmti->SetSystemProperty(                             \
                "java.library.path", "./so");                              \
        if  (results[0] != JVMTI_ERROR_NONE) {                             \
            printf("Failure of adding library path : %d \n", results[0]);  \
            return JNI_ERR;                                                \
        }                                                                  \
    }                                                                      \
}

/***************************************************************************/

#define AGENT_JVMTI_CAPABILITIES_INIT {                                    \
    /* initialization of needed capabilities and calbacks */               \
    {                                                                      \
        results[1] = jvmti->GetPotentialCapabilities(&possible_caps);      \
        results[2] = jvmti->AddCapabilities(&possible_caps);               \
                                                                           \
        if  ((results[1] != JVMTI_ERROR_NONE) ||                           \
             (results[2] != JVMTI_ERROR_NONE)) {                           \
            printf("Failure of adding capabilities : pot=%d add=%d \n",    \
                    results[1],                                            \
                    results[2]);                                           \
            return JNI_ERR;                                                \
        }                                                                  \
    }                                                                      \
}

/***************************************************************************/

#define AGENT_JVMTI_SET_CALLBACK_EXCEPTION_INIT {                          \
    /* events enabling */                                                  \
    {                                                                      \
        jvmtiEvent events[] = { JVMTI_EVENT_EXCEPTION };                   \
                                                                           \
        memset(&callbacks, 0, sizeof(callbacks));                          \
        callbacks.Exception = callbackException;                           \
                                                                           \
        results[3] =                                                       \
            jvmti->SetEventNotificationMode(                               \
                    JVMTI_ENABLE, events[0], NULL);                        \
                                                                           \
        if (results[3] != JVMTI_ERROR_NONE) {                              \
            printf("Failure of enabling event: %d\n", results[3]);         \
            return JNI_ERR;                                                \
        }                                                                  \
    }                                                                      \
}

/***************************************************************************/

#define AGENT_JVMTI_SET_CALLBACK_VMINIT_INIT {                             \
    /* events enabling */                                                  \
    {                                                                      \
        jvmtiEvent events[] = { JVMTI_EVENT_VM_INIT };                     \
                                                                           \
        memset(&callbacks, 0, sizeof(callbacks));                          \
        callbacks.VMInit = callbackVMInit;                                 \
                                                                           \
        results[3] =                                                       \
            jvmti->SetEventNotificationMode(                               \
                    JVMTI_ENABLE, events[0], NULL);                        \
                                                                           \
        if (results[3] != JVMTI_ERROR_NONE) {                              \
            printf("Failure of enabling event: %d\n", results[3]);         \
            return JNI_ERR;                                                \
        }                                                                  \
    }                                                                      \
}

/***************************************************************************/

#define AGENT_JVMTI_SET_CALLBACK_THREAD_START_INIT {                       \
    /* events enabling */                                                  \
    {                                                                      \
        jvmtiEvent events[] = { JVMTI_EVENT_THREAD_START };                \
                                                                           \
        memset(&callbacks, 0, sizeof(callbacks));                          \
        callbacks.ThreadStart = callbackThreadStart;                       \
                                                                           \
        results[3] =                                                       \
            jvmti->SetEventNotificationMode(                               \
                    JVMTI_ENABLE, events[0], NULL);                        \
                                                                           \
        if (results[3] != JVMTI_ERROR_NONE) {                              \
            printf("Failure of enabling event: %d\n", results[3]);         \
            return JNI_ERR;                                                \
        }                                                                  \
    }                                                                      \
}

/***************************************************************************/

#define AGENT_JVMTI_SET_CALLBACK_THRD_START_INIT {                         \
    /* events enabling */                                                  \
    {                                                                      \
        jvmtiEvent events[] = { JVMTI_EVENT_THREAD_START };                \
                                                                           \
        memset(&callbacks, 0, sizeof(callbacks));                          \
        callbacks.ThreadStart = callbackThreadStart;                       \
                                                                           \
        results[3] =                                                       \
            jvmti->SetEventNotificationMode(                               \
                    JVMTI_ENABLE, events[0], NULL);                        \
                                                                           \
        if (results[3] != JVMTI_ERROR_NONE) {                              \
            printf("Failure of enabling event: %d\n", results[3]);         \
            return JNI_ERR;                                                \
        }                                                                  \
    }                                                                      \
}

/***************************************************************************/

#define AGENT_JVMTI_CALLBACKS_INIT {                                       \
    /* set event callbacks */                                              \
    {                                                                      \
        results[7] =                                                       \
                jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));   \
        if (results[7] != JVMTI_ERROR_NONE) {                              \
            printf("Failure of setting event callbacks: %d\n",             \
                    results[7]);                                           \
            return JNI_ERR;                                                \
        }                                                                  \
    }                                                                      \
}

/***************************************************************************/

#define START_TEST_CHECK                                                   \
    jint test_start = 0;                                                   \
    jvmtiPhase phase;                                                      \
    jvmtiThreadInfo tinfo;                                                 \
                                                                           \
    if (jvmti_env->GetPhase(&phase) == JVMTI_ERROR_NONE)                   \
    {                                                                      \
        if (phase == JVMTI_PHASE_LIVE)                                     \
        {                                                                  \
            test_start = 1;                                                \
        }                                                                  \
    }                                                                      \
                                                                           \
    if (test_start)                                                        \
    {                                                                      \
        test_start = 0;                                                    \
                                                                           \
        if (jvmti_env->GetThreadInfo(thread, &tinfo)                       \
                == JVMTI_ERROR_NONE)                                       \
        {                                                                  \
            if (!strcmp(tinfo.name, "agent"))                              \
            {                                                              \
                test_start = 1;                                            \
            }                                                              \
        }                                                                  \
    }

/***************************************************************************/

#define CHECK_BEFORE_START()                                               \
    jvmtiPhase phase;                                                      \
    bool test_can_be_started = false;                                      \
                                                                           \
    if (jvmti_env->GetPhase(&phase) == JVMTI_ERROR_NONE)                   \
    {                                                                      \
        if (phase == JVMTI_PHASE_LIVE)                                     \
        {                                                                  \
            jvmtiThreadInfo tinfo ;                                        \
                                                                           \
            if (jvmti_env->GetThreadInfo(thread, &tinfo)                   \
                    == JVMTI_ERROR_NONE)                                   \
            {                                                              \
                if (!strcmp(tinfo.name, "agent"))                          \
                {                                                          \
                    test_can_be_started = true;                            \
                }                                                          \
            }                                                              \
                                                                           \
            FREE_TINFO;                                                    \
        }                                                                  \
    }

/***************************************************************************/

#define GET_CURRENT_JCLASS_AND_SPECIAL_METHOD_ID()                         \
{                                                                          \
    jint ccount;                                                           \
    jclass cc = NULL;                                                      \
    jclass* classes;                                                       \
    jvmtiError r_get_classes =                                             \
        jvmti_env->GetLoadedClasses(&ccount, &classes);                    \
                                                                           \
    if (r_get_classes == JVMTI_ERROR_NONE)                                 \
    {                                                                      \
        for (int i = 0; i < ccount; i++)                                   \
        {                                                                  \
            char* s;                                                       \
            char* g;                                                       \
            jvmtiError r_get_class_sign =                                  \
                  jvmti_env->GetClassSignature(classes[i], &s, &g);        \
                                                                           \
            if (r_get_class_sign == JVMTI_ERROR_NONE)                      \
            {                                                              \
                if (!strcmp(s, filename))                                  \
                {                                                          \
                    cc = classes[i];                                       \
                    break;                                                 \
                }                                                          \
            }                                                              \
        }                                                                  \
    }                                                                      \
    else                                                                   \
    {                                                                      \
        printf(" FAILED \n\terror during GetLoadedClasses");               \
        return;                                                            \
    }                                                                      \
    if (cc)                                                                \
    {                                                                      \
        jint mcount;                                                       \
        jmethodID* methods;                                                \
        jvmtiError r = jvmti_env->GetClassMethods(cc, &mcount, &methods);  \
        if (r == JVMTI_ERROR_NONE)                                         \
        {                                                                  \
            for (int i = 0; i < mcount; i++)                               \
            {                                                              \
                char* name;                                                \
                char* signature;                                           \
                char* generic;                                             \
                if (jvmti_env->GetMethodName(methods[i], &name, &signature,\
                            &generic) == JVMTI_ERROR_NONE)                 \
                {                                                          \
                    if (!strcmp(name, "special_method"))                   \
                    {                                                      \
                        method = methods[i];                               \
                        break;                                             \
                    }                                                      \
                }                                                          \
            }                                                              \
        }                                                                  \
    }                                                                      \
    if (!cc || !method)                                                    \
    {                                                                      \
        printf(" FAILED\n\tError during class or method definition\n class = %x, method = %x\n ",\
                cc, method);                                               \
        return;                                                            \
    }                                                                      \
}

/***************************************************************************/

#define GET_CURRENT_JCLASS()                                               \
{                                                                          \
    jint ccount;                                                           \
    jclass cc = NULL;                                                      \
    jclass* classes;                                                       \
    jvmtiError r_get_classes =                                             \
        jvmti_env->GetLoadedClasses(&ccount, &classes);                    \
                                                                           \
    if (r_get_classes == JVMTI_ERROR_NONE)                                 \
    {                                                                      \
        for (int i = 0; i < ccount; i++)                                   \
        {                                                                  \
            char* s;                                                       \
            char* g;                                                       \
            jvmtiError r_get_class_sign =                                  \
                  jvmti_env->GetClassSignature(classes[i], &s, &g);        \
                                                                           \
            if (r_get_class_sign == JVMTI_ERROR_NONE)                      \
            {                                                              \
                if (!strcmp(s, filename))                                  \
                {                                                          \
                    cc = classes[i];                                       \
                    break;                                                 \
                }                                                          \
            }                                                              \
        }                                                                  \
    }                                                                      \
    else                                                                   \
    {                                                                      \
        printf(" FAILED \n\terror during GetLoadedClasses");               \
        return;                                                            \
    }                                                                      \
                                                                           \
    if (!cc)                                                               \
    {                                                                      \
        printf(" FAILED\n\tError during class definition\n class = %x",    \
                cc);                                                       \
        return;                                                            \
    }                                                                      \
                                                                           \
    clazz = cc;                                                            \
}

/***************************************************************************/

#define GET_NEEDED_FID()                                                   \
                                                                           \
if (jvmti_env->GetClassFields(clazz, &fcount, &fids) == JVMTI_ERROR_NONE)  \
{                                                                          \
    for (int i = 0; i < fcount; i++)                                       \
    {                                                                      \
        char* field_name;                                                  \
        char* field_signature;                                             \
        char* field_generic;                                               \
                                                                           \
        if (jvmti_env->GetFieldName(clazz, fids[i], &field_name,           \
                  &field_signature, &field_generic) == JVMTI_ERROR_NONE)   \
        {                                                                  \
            if (!strcmp(field_name, "special_field"))                      \
            {                                                              \
                if (!strcmp(field_signature, "Z"))                         \
                {                                                          \
                    fid = fids[i];                                         \
                    break;                                                 \
                }                                                          \
            }                                                              \
        }                                                                  \
    }                                                                      \
}

/***************************************************************************/

void thread_state_output_as_string(long state);

void class_status_output_as_string(long status);

void print_capabilities(jvmtiCapabilities c, const char* name);

int cmp_caps_and_output_results(jvmtiCapabilities c1,
        jvmtiCapabilities c2, const char* name1, const char* name2);

void make_caps_zero(jvmtiCapabilities* c1);

void make_caps_set_all(jvmtiCapabilities* c1);

/***************************************************************************/

#define CAPABILITY_TURN_OFF(cap)                                           \
{                                                                          \
    jvmtiCapabilities zero_caps;                                           \
    jvmtiError result;                                                     \
    make_caps_zero(&zero_caps);                                            \
    zero_caps.cap = 1;                                                     \
    result = jvmti->RelinquishCapabilities(&zero_caps);                    \
    if (result != JVMTI_ERROR_NONE)                                        \
    {                                                                      \
        fprintf(stderr,                                                    \
                "Error during RelinquishCapabilities. Test Stopped\n");    \
        fflush(stderr);                                                    \
        return JNI_ERR;                                                    \
    }                                                                      \
}                                                                          \

/***************************************************************************/

#define CAPABILITY_TURN_OFF_VOID(cap)                                      \
{                                                                          \
    jvmtiCapabilities zero_caps;                                           \
    jvmtiError result;                                                     \
    make_caps_zero(&zero_caps);                                            \
    zero_caps.cap = 1;                                                     \
    result = jvmti_env->RelinquishCapabilities(&zero_caps);                \
    if (result != JVMTI_ERROR_NONE)                                        \
    {                                                                      \
        fprintf(stderr,                                                    \
                "Error during RelinquishCapabilities. Test Stopped\n");    \
        fflush(stderr);                                                    \
        return;                                                            \
    }                                                                      \
}                                                                          \

/***************************************************************************/

#define CAPABILITY_CHECK()                                                 \
{                                                                          \
    jvmtiCapabilities full_caps;                                           \
    jvmtiCapabilities caps;                                                \
    make_caps_set_all(&full_caps);                                         \
    result = jvmti_env->GetCapabilities(&caps);                            \
    if (result != JVMTI_ERROR_NONE)                                        \
    {                                                                      \
        fprintf(stderr,                                                    \
                "Error during GetCapabilities. Test Stopped\n");           \
        fflush(stderr);                                                    \
        return;                                                            \
    }                                                                      \
    int diff = cmp_caps_and_output_results(full_caps, caps,                \
            "potential", "current");                                       \
}                                                                          \

/***************************************************************************/

#define SPP_NONE              0
#define SPP_ONLOAD_ONLY       1
#define SPP_ONLOAD_AND_LIVE   2
#define SPP_START_AND_LIVE    3
#define SPP_LIVE_ONLY         4

#define DEBUG_OUT             0

bool check_phase_debug(jvmtiEnv* jvmti_env, int phase_param, bool is_debug);

bool check_phase_and_method_debug(jvmtiEnv* jvmti_env, jmethodID method, int phase_param,
        const char* exp_name, bool is_debug);

bool check_phase_and_thread_debug(jvmtiEnv* jvmti_env, jthread thread, int phase_param,
        const char* exp_name, bool is_debug);

jthread create_not_alive_thread(JNIEnv* jni_env, jvmtiEnv* jvmti_env);

bool is_needed_field_found(jvmtiEnv* jvmti_env, const char* filename, const char* fieldname,
        jclass* myclass, jfieldID* myfield, bool is_debug);

void func_for_callback_VMDeath(JNIEnv* jni_env, jvmtiEnv* jvmti_env,
        const char* test_case_name, bool test, bool util);

jint func_for_Agent_OnLoad(JavaVM *vm, char *options, void *reserved, Callbacks* callbacks,
        jvmtiEvent* events, jint size, const char* test_case_name, bool is_debug);

jint func_for_Agent_OnLoad_JVMTI(JavaVM *vm, char *options, void *reserved, Callbacks* callbacks,
        jvmtiEvent* events, jint size, const char* test_case_name, bool is_debug, jvmtiEnv** jvmti_env);

//////////////////////////////////////////////////////////////////////////

const char* get_jvmti_eror_text(jvmtiError);

#endif /* _UTILS_H_ */
