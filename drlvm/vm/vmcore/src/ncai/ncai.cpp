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
 * @author Intel, Petr Ivanov
 */

#define LOG_DOMAIN "ncai"
#include "cxxlog.h"
#include "jvmti_internal.h"
#include "suspend_checker.h"
#include "environment.h"
#include "jvmti_break_intf.h"
#include "ncai_direct.h"
#include "ncai_internal.h"

#include "ncai.h"

//////////////////////////////////////////////////////////////////////////////
// Stubs for functions which are not implemented yet
// FIXME: Possibly will be not needed after Capabilities implementation
static ncaiError JNICALL ncaiGetModuleClassLoaderStub(ncaiEnv*,ncaiModule,jobject*)
{ return NCAI_ERROR_NOT_AVAILABLE; }
static ncaiError JNICALL ncaiFindJavaMethodStub(ncaiEnv*,void*,jmethodID*)
{ return NCAI_ERROR_NOT_AVAILABLE; }
static ncaiError JNICALL ncaiGetBytcodeLocationStub(ncaiEnv* env, void* address, jmethodID* method, jlocation* location_ptr)
{ return NCAI_ERROR_NOT_AVAILABLE; }
static ncaiError JNICALL ncaiGetThreadStateStub(ncaiEnv*,ncaiThread,jint*)
{ return NCAI_ERROR_NOT_AVAILABLE; }
static ncaiError JNICALL ncaiGetVersionStub(ncaiEnv*,jint*)
{ return NCAI_ERROR_NOT_AVAILABLE; }
static ncaiError JNICALL ncaiGetErrorNameStub(ncaiEnv*,ncaiError,const char**)
{ return NCAI_ERROR_NOT_AVAILABLE; }
static ncaiError JNICALL ncaiGetPotentialCapabilitiesStub(ncaiEnv*,ncaiCapabilities*)
{ return NCAI_ERROR_NOT_AVAILABLE; }
static ncaiError JNICALL ncaiGetCapabilitiesStub(ncaiEnv*,ncaiCapabilities*)
{ return NCAI_ERROR_NOT_AVAILABLE; }
static ncaiError JNICALL ncaiAddCapabilitiesStub(ncaiEnv*,ncaiCapabilities*)
{ return NCAI_ERROR_NOT_AVAILABLE; }
static ncaiError JNICALL ncaiRelinquishCapabilitiesStub(ncaiEnv*,ncaiCapabilities*)
{ return NCAI_ERROR_NOT_AVAILABLE; }
static ncaiError JNICALL ncaiSetWatchpointStub(ncaiEnv*,void*,size_t,ncaiWatchpointMode)
{ return NCAI_ERROR_NOT_AVAILABLE; }
static ncaiError JNICALL ncaiClearWatchpointStub(ncaiEnv*,void*)
{ return NCAI_ERROR_NOT_AVAILABLE; }
static ncaiError JNICALL ncaiNotifyFramePopStub(ncaiEnv*,ncaiThread,void*)
{ return NCAI_ERROR_NOT_AVAILABLE; }

// Function table
const struct _ncai ncai_table =
{
    ncaiGetAllLoadedModules,
    ncaiGetModuleInfo,
    ncaiGetModuleClassLoaderStub,       //ncaiGetModuleClassLoader,
    ncaiIsMethodCompiled,
    ncaiGetMethodLocation,
    ncaiFindJavaMethodStub,             //ncaiFindJavaMethod,
    ncaiGetBytcodeLocationStub,         //ncaiGetBytcodeLocation,
    ncaiGetNativeLocation,
    ncaiGetAllThreads,
    ncaiGetThreadInfo,
    ncaiGetThreadHandle,
    ncaiGetThreadObject,
    ncaiSuspendThread,
    ncaiResumeThread,
    ncaiTerminateThread,
    ncaiGetThreadStateStub,             //ncaiGetThreadState,
    ncaiGetFrameCount,
    ncaiGetStackTrace,
    ncaiGetRegisterCount,
    ncaiGetRegisterInfo,
    ncaiGetRegisterValue,
    ncaiSetRegisterValue,
    ncaiReadMemory,
    ncaiWriteMemory,
    ncaiGetSignalCount,
    ncaiGetSignalInfo,
    ncaiGetJvmtiEnv,
    ncaiGetVersionStub,                 //ncaiGetVersion,
    ncaiGetErrorNameStub,               //ncaiGetErrorName,
    ncaiGetPotentialCapabilitiesStub,   //ncaiGetPotentialCapabilities,
    ncaiGetCapabilitiesStub,            //ncaiGetCapabilities,
    ncaiAddCapabilitiesStub,            //ncaiAddCapabilities,
    ncaiRelinquishCapabilitiesStub,     //ncaiRelinquishCapabilities,
    ncaiGetEventCallbacks,
    ncaiSetEventCallbacks,
    ncaiSetEventNotificationMode,
    ncaiSetBreakpoint,
    ncaiClearBreakpoint,
    ncaiSetWatchpointStub,              //ncaiSetWatchpoint,
    ncaiClearWatchpointStub,            //ncaiClearWatchpoint,
    ncaiSetStepMode,
    ncaiNotifyFramePopStub,             //ncaiNotifyFramePop,
};

jvmtiError create_ncai_environment(jvmtiEnv* jvmti_env, NCAIEnv** penv)
{
    TRACE2("jvmti.ncai", "create_ncai_environment called");

    TIEnv *ti_env = (TIEnv *)jvmti_env;
    NCAIEnv *newenv;
    jvmtiError error_code;

    error_code = _allocate(sizeof(NCAIEnv), (unsigned char**)&newenv);
    if (error_code != JVMTI_ERROR_NONE)
    {
        *penv = NULL;
        return error_code;
    }

    newenv->functions = &ncai_table;
    newenv->ti_env = ti_env;
    newenv->modules = NULL;
    ti_env->ncai_env = newenv;
    memset(&newenv->event_table, 0, sizeof(ncaiEventCallbacks));
    memset(&newenv->global_events, 0, sizeof(newenv->global_events));
    memset(&newenv->event_threads, 0, sizeof(newenv->event_threads));

    newenv->env_lock = new Lock_Manager();
    assert(newenv->env_lock);

    // Acquire interface for breakpoint handling
    newenv->brpt_intf =
        VM_Global_State::loader_env->TI->vm_brpt->new_intf(ti_env,
            ncai_process_breakpoint_event, PRIORITY_NCAI_BREAKPOINT, false);
    assert(newenv->brpt_intf);

    *penv = newenv;
    TRACE2("jvmti.ncai", "New NCAI environment created: " << newenv);
    return JVMTI_ERROR_NONE;
}


jvmtiError JNICALL jvmtiGetNCAIEnvironment(jvmtiEnv* jvmti_env, ...)
{
    TRACE2("jvmti.ncai", "GetNCAIEnvironment called");
    SuspendEnabledChecker sec;

    va_list args;
    va_start(args, jvmti_env);
    // GetExtensionEnv function has following prototype:
    // GetExtensionEnv(jvmtiEnv* jvmti_env, void** ncai_env_ptr, jint version);
    NCAIEnv** penv = (NCAIEnv**)va_arg(args, void**);
    jint version = (jint)va_arg(args, jint);
    va_end(args);

    if (penv == NULL)
        return JVMTI_ERROR_NULL_POINTER;

    jint vmajor = (version & NCAI_VERSION_MASK_MAJOR) >> NCAI_VERSION_SHIFT_MAJOR;
    jint vminor = (version & NCAI_VERSION_MASK_MINOR) >> NCAI_VERSION_SHIFT_MINOR;
    jint vmicro = (version & NCAI_VERSION_MASK_MICRO) >> NCAI_VERSION_SHIFT_MICRO;
    if (vmajor > NCAI_VERSION_MAJOR ||
        vminor > NCAI_VERSION_MINOR ||
        vmicro > NCAI_VERSION_MICRO)
    {
        *penv = NULL;
        return JVMTI_ERROR_NOT_AVAILABLE;
    }

    GlobalNCAI* ncai = VM_Global_State::loader_env->NCAI;

    TIEnv* ti_env = (TIEnv*)jvmti_env;
    if (ti_env->ncai_env != NULL)
    {
        *penv = ti_env->ncai_env;
        ncai->enabled = true;
        return JVMTI_ERROR_NONE;
    }

    jvmtiError err = create_ncai_environment(jvmti_env, penv);

    if (err == JVMTI_ERROR_NONE)
        ncai->enabled = true;

    return err;
}

ncaiError JNICALL ncaiGetJvmtiEnv(ncaiEnv *env, jvmtiEnv** jvmti_env_ptr)
{
    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (jvmti_env_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    *jvmti_env_ptr = (jvmtiEnv*)((NCAIEnv*)env)->ti_env;

    return NCAI_ERROR_NONE;
}

GlobalNCAI::GlobalNCAI():
    enabled(false),
    modules(NULL),
    step_enabled(false),
    step_mode(NCAI_STEP_INTO)
{
}

GlobalNCAI::~GlobalNCAI()
{
    // Clean modules list
    clean_all_modules(&modules);
}

bool GlobalNCAI::isEnabled()
{
    return VM_Global_State::loader_env->NCAI &&
           VM_Global_State::loader_env->NCAI->enabled;
}
