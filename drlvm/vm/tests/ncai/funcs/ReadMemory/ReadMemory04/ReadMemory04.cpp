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
 * @author Petr Ivanov
 *
 */

/* *********************************************************************** */

#include "events.h"
#include "utils.h"
#include "ncai.h"

static bool test = false;
static bool util = false;
static bool flag = false;

const char test_case_name[] = "ReadMemory04";
typedef void (*functionPtr)(void);
static void Test();

/* *********************************************************************** */

extern "C" JNIEXPORT void JNICALL Java_ncai_funcs_ReadMemory04_TestFunction
  (JNIEnv *env, jclass clazz)
{
    fprintf(stderr, "JNI TestFunction: run\n");
    return;
}


JNIEXPORT jint JNICALL Agent_OnLoad(prms_AGENT_ONLOAD)
{
    Callbacks CB;
    check_AGENT_ONLOAD;
    jvmtiEvent events[] = { JVMTI_EVENT_EXCEPTION, JVMTI_EVENT_VM_DEATH };
    cb_exc;
    cb_death;
    return func_for_Agent_OnLoad(vm, options, reserved, &CB,
        events, sizeof(events)/sizeof(jvmtiEvent), test_case_name, DEBUG_OUT);
}

/* *********************************************************************** */

void JNICALL callbackException(prms_EXCPT)
{
    check_EXCPT;
    if (flag) return;

    /*
     * Function separate all other exceptions in all other method
     */
    if (!check_phase_and_method_debug(jvmti_env, method, SPP_LIVE_ONLY,
                "special_method", DEBUG_OUT)) return;

    flag = true;
    util = true;

    jvmtiError err;
    ncaiError ncai_err;

    jvmtiExtensionFunctionInfo* ext_info = NULL;
    jint ext_count = 0;

    err = jvmti_env->GetExtensionFunctions(&ext_count, &ext_info);

    if (err != JVMTI_ERROR_NONE)
    {
        fprintf(stderr, "test_function: GetExtensionFunctions() returned error: %d, '%s'\n",
            err, get_jvmti_eror_text(err));
        test = false;
        return;
    }

    if (ext_count == 0 || ext_info == NULL)
    {
        fprintf(stderr, "test_function: GetExtensionFunctions() returned no extensions\n");
        test = false;
        return;
    }

    jvmtiExtensionFunction get_ncai_func = NULL;

    for (int i = 0; i < ext_count; i++)
    {
        if (strcmp(ext_info[i].id, "org.apache.harmony.vm.GetExtensionEnv") == 0)
        {
            get_ncai_func = ext_info[i].func;
            break;
        }
    }

    if (get_ncai_func == NULL)
    {
        fprintf(stderr, "test_function: GetNCAIEnvironment() nas not been found among JVMTI extensions\n");
        test = false;
        return;
    }

    ncaiEnv* ncai_env = NULL;

    err = get_ncai_func(jvmti_env, &ncai_env, NCAI_VERSION_1_0);

    if (err != JVMTI_ERROR_NONE)
    {
        fprintf(stderr, "test_function: get_ncai_func() returned error: %d, '%s'\n",
            err, get_jvmti_eror_text(err));
        test = false;
        return;
    }

    if (ncai_env == NULL)
    {
        fprintf(stderr, "test_function: get_ncai_func() returned NULL environment\n");
        test = false;
        return;
    }

    char buf[200];
    functionPtr func = Test;

    ncai_err = ncai_env->ReadMemory((void *)func, 1, &buf);
    if(ncai_err != NCAI_ERROR_NONE)
    {
        fprintf(stderr,
            "test_function: ncai_env->ReadMemory(%p, 1, (void*)%p) has returned %d\n",
            (void*)func, &buf, ncai_err);
        test = false;
        return;
    }

    test = true;
}

void JNICALL callbackVMDeath(prms_VMDEATH)
{
    check_VMDEATH;
    func_for_callback_VMDeath(jni_env, jvmti_env, test_case_name, test, util);
}

/* *********************************************************************** */

void Test()
{
    int s = 3;
    int a = s++;
    int d = ++a;
    int f = d--;
    int g = --f;
    int h = g-f;
    int j = s+a;
    while(s < 30)
    {
        s--;
        if(s <= 0)
            s = h+j;
    }
    return;
}
