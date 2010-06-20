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
 * @author Gregory Shimansky
 */  
/*
 * JVMTI general API
 */
#define LOG_DOMAIN "jvmti.general"
#include "cxxlog.h"

#include "jvmti_direct.h"
#include "jvmti_utils.h"
#include "jvmti_internal.h"
#include "environment.h"
#include "suspend_checker.h"
#include "jvmti_break_intf.h"

/*
 * Get Phase
 *
 * Return the current phase of VM execution.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetPhase(jvmtiEnv* env,
              jvmtiPhase* phase_ptr)
{
    TRACE2("jvmti.general", "GetPhase called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase* phases = NULL;

    CHECK_EVERYTHING();

    if (NULL == phase_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    *phase_ptr = ((TIEnv *)env)->vm->vm_env->TI->getPhase();
    TRACE2("jvmti", "Phase = " << *phase_ptr);
    return JVMTI_ERROR_NONE;
}

/*
 * Dispose Environment
 *
 * Shutdown a JVMTI connection created with JNI GetEnv (see JVMTI
 * Environments). Dispose of any resources held by the environment.
 * Suspended threads are not resumed, this must be done explicitly
 * by the agent. Allocated memory is not released, this must be
 * done explicitly by the agent. This environment may not be used
 * after this call. This call returns to the caller.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiDisposeEnvironment(jvmtiEnv* env)
{
    TRACE2("jvmti.general", "DisposeEnvironment called, env = " << env);
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase* phases = NULL;

    CHECK_EVERYTHING();

    TIEnv *p_env = (TIEnv *)env;
    DebugUtilsTI *ti = p_env->vm->vm_env->TI;
    LMAutoUnlock lock(&ti->TIenvs_lock);

    // Disable all global events for this environment
    int iii;
    for (iii = JVMTI_MIN_EVENT_TYPE_VAL; iii <= JVMTI_MAX_EVENT_TYPE_VAL; iii++)
        remove_event_from_global(env, (jvmtiEvent)iii);

    // Disable all thread local events for this environment, do it only in live phase
    // otherwise environment couldn't register for any thread local events
    // because it couldn't get any live thread references
    jvmtiPhase ph;
    jvmtiGetPhase(env, &ph);
    if (JVMTI_PHASE_LIVE == ph)
    {
        // FIXME: this loop is very ugly, could we improve it?
        jint threads_number;
        jthread *threads;
        jvmtiGetAllThreads(env, &threads_number, &threads);
        for (int jjj = 0; jjj < threads_number; jjj++)
            for (iii = JVMTI_MIN_EVENT_TYPE_VAL; iii <= JVMTI_MAX_EVENT_TYPE_VAL; iii++)
                remove_event_from_thread(env, (jvmtiEvent)iii, threads[jjj]);
        _deallocate((unsigned char *)threads);
    }

    // Remove all breakpoints set by this environment and release interface
    ti->vm_brpt->release_intf(p_env->brpt_intf);

    // Remove all capabilities for this environment
    jvmtiRelinquishCapabilities(env, &p_env->posessed_capabilities);

    // Remove environment from the global list
    p_env->vm->vm_env->TI->removeEnvironment(p_env);

    _deallocate((unsigned char *)env);

    return JVMTI_ERROR_NONE;
}

/*
 * Set Environment Local Storage
 *
 * The VM stores a pointer value associated with each environment.
 * This pointer value is called environment-local storage. This
 * value is NULL unless set with this function.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiSetEnvironmentLocalStorage(jvmtiEnv* env,
                                const void* data)
{
    TRACE("SetEnvironmentLocalStorage called, data = " << data);
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase* phases = NULL;

    CHECK_EVERYTHING();

    ((TIEnv *)env)->user_storage = const_cast<void*>(data);

    return JVMTI_ERROR_NONE;
}

/*
 * Get Environment Local Storage
 *
 * Called by the agent to get the value of the JVMTI
 * environment-local storage
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetEnvironmentLocalStorage(jvmtiEnv* env,
                                void** data_ptr)
{
    TRACE2("jvmti.general", "GetEnvironmentLocalStorage called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase* phases = NULL;

    CHECK_EVERYTHING();

    if (NULL == data_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    *data_ptr = ((TIEnv *)env)->user_storage;

    return JVMTI_ERROR_NONE;
}

/*
 * Get Version Number
 *
 * Return the JVMTI version via version_ptr. The return value is
 * the version identifier. The version identifier includes major,
 * minor and micro version as well as the interface type.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetVersionNumber(jvmtiEnv* env,
                      jint* version_ptr)
{
    TRACE2("jvmti.general", "GetVersionNumber called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase* phases = NULL;

    CHECK_EVERYTHING();

    if (NULL == version_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    *version_ptr = JVMTI_VERSION;

    return JVMTI_ERROR_NONE;
}

/*
 * Get Error Name
 *
 * Return the symbolic name for an error code.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetErrorName(jvmtiEnv* UNREF env,
                  jvmtiError error,
                  char** name_ptr)
{
    TRACE2("jvmti.general", "GetErrorName called, error = " << error);
    SuspendEnabledChecker sec;
    if (NULL == name_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    const char *error_name;

    switch (error)
    {
        case JVMTI_ERROR_NONE:
            error_name = "JVMTI_ERROR_NONE";
            break;
        case JVMTI_ERROR_NULL_POINTER:
            error_name = "JVMTI_ERROR_NULL_POINTER";
            break;
        case JVMTI_ERROR_OUT_OF_MEMORY:
            error_name = "JVMTI_ERROR_OUT_OF_MEMORY";
            break;
        case JVMTI_ERROR_ACCESS_DENIED:
            error_name = "JVMTI_ERROR_ACCESS_DENIED";
            break;
        case JVMTI_ERROR_UNATTACHED_THREAD:
            error_name = "JVMTI_ERROR_UNATTACHED_THREAD";
            break;
        case JVMTI_ERROR_INVALID_ENVIRONMENT:
            error_name = "JVMTI_ERROR_INVALID_ENVIRONMENT";
            break;
        case JVMTI_ERROR_WRONG_PHASE:
            error_name = "JVMTI_ERROR_WRONG_PHASE";
            break;
        case JVMTI_ERROR_INTERNAL:
            error_name = "JVMTI_ERROR_INTERNAL";
            break;
        case JVMTI_ERROR_INVALID_PRIORITY:
            error_name = "JVMTI_ERROR_INVALID_PRIORITY";
            break;
        case JVMTI_ERROR_THREAD_NOT_SUSPENDED:
            error_name = "JVMTI_ERROR_THREAD_NOT_SUSPENDED";
            break;
        case JVMTI_ERROR_THREAD_SUSPENDED:
            error_name = "JVMTI_ERROR_THREAD_SUSPENDED";
            break;
        case JVMTI_ERROR_THREAD_NOT_ALIVE:
            error_name = "JVMTI_ERROR_THREAD_NOT_ALIVE";
            break;
        case JVMTI_ERROR_CLASS_NOT_PREPARED:
            error_name = "JVMTI_ERROR_CLASS_NOT_PREPARED";
            break;
        case JVMTI_ERROR_NO_MORE_FRAMES:
            error_name = "JVMTI_ERROR_NO_MORE_FRAMES";
            break;
        case JVMTI_ERROR_OPAQUE_FRAME:
            error_name = "JVMTI_ERROR_OPAQUE_FRAME";
            break;
        case JVMTI_ERROR_DUPLICATE:
            error_name = "JVMTI_ERROR_DUPLICATE";
            break;
        case JVMTI_ERROR_NOT_FOUND:
            error_name = "JVMTI_ERROR_NOT_FOUND";
            break;
        case JVMTI_ERROR_NOT_MONITOR_OWNER:
            error_name = "JVMTI_ERROR_NOT_MONITOR_OWNER";
            break;
        case JVMTI_ERROR_INTERRUPT:
            error_name = "JVMTI_ERROR_INTERRUPT";
            break;
        case JVMTI_ERROR_UNMODIFIABLE_CLASS:
            error_name = "JVMTI_ERROR_UNMODIFIABLE_CLASS";
            break;
        case JVMTI_ERROR_NOT_AVAILABLE:
            error_name = "JVMTI_ERROR_NOT_AVAILABLE";
            break;
        case JVMTI_ERROR_ABSENT_INFORMATION:
            error_name = "JVMTI_ERROR_ABSENT_INFORMATION";
            break;
        case JVMTI_ERROR_INVALID_EVENT_TYPE:
            error_name = "JVMTI_ERROR_INVALID_EVENT_TYPE";
            break;
        case JVMTI_ERROR_NATIVE_METHOD:
            error_name = "JVMTI_ERROR_NATIVE_METHOD";
            break;
        case JVMTI_ERROR_INVALID_THREAD:
            error_name = "JVMTI_ERROR_INVALID_THREAD";
            break;
        case JVMTI_ERROR_INVALID_FIELDID:
            error_name = "JVMTI_ERROR_INVALID_FIELDID";
            break;
        case JVMTI_ERROR_INVALID_METHODID:
            error_name = "JVMTI_ERROR_INVALID_METHODID";
            break;
        case JVMTI_ERROR_INVALID_LOCATION:
            error_name = "JVMTI_ERROR_INVALID_LOCATION";
            break;
        case JVMTI_ERROR_INVALID_OBJECT:
            error_name = "JVMTI_ERROR_INVALID_OBJECT";
            break;
        case JVMTI_ERROR_INVALID_CLASS:
            error_name = "JVMTI_ERROR_INVALID_CLASS";
            break;
        case JVMTI_ERROR_TYPE_MISMATCH:
            error_name = "JVMTI_ERROR_TYPE_MISMATCH";
            break;
        case JVMTI_ERROR_INVALID_SLOT:
            error_name = "JVMTI_ERROR_INVALID_SLOT";
            break;
        case JVMTI_ERROR_MUST_POSSESS_CAPABILITY:
            error_name = "JVMTI_ERROR_MUST_POSSESS_CAPABILITY";
            break;
        case JVMTI_ERROR_INVALID_THREAD_GROUP:
            error_name = "JVMTI_ERROR_INVALID_THREAD_GROUP";
            break;
        case JVMTI_ERROR_INVALID_MONITOR:
            error_name = "JVMTI_ERROR_INVALID_MONITOR";
            break;
        case JVMTI_ERROR_ILLEGAL_ARGUMENT:
            error_name = "JVMTI_ERROR_ILLEGAL_ARGUMENT";
            break;
        case JVMTI_ERROR_INVALID_TYPESTATE:
            error_name = "JVMTI_ERROR_INVALID_TYPESTATE";
            break;
        case JVMTI_ERROR_UNSUPPORTED_VERSION:
            error_name = "JVMTI_ERROR_UNSUPPORTED_VERSION";
            break;
        case JVMTI_ERROR_INVALID_CLASS_FORMAT:
            error_name = "JVMTI_ERROR_INVALID_CLASS_FORMAT";
            break;
        case JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION:
            error_name = "JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION";
            break;
        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED:
            error_name = "JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED";
            break;
        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED:
            error_name = "JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED";
            break;
        case JVMTI_ERROR_FAILS_VERIFICATION:
            error_name = "JVMTI_ERROR_FAILS_VERIFICATION";
            break;
        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED:
            error_name = "JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED";
            break;
        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED:
            error_name = "JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED";
            break;
        case JVMTI_ERROR_NAMES_DONT_MATCH:
            error_name = "JVMTI_ERROR_NAMES_DONT_MATCH";
            break;
        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED:
            error_name = "JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED";
            break;
        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED:
            error_name = "JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED";
            break;
        default:
            *name_ptr = NULL;
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }

    jvmtiError res;
    res = _allocate(strlen(error_name) + 1, (unsigned char **)name_ptr);

    if (JVMTI_ERROR_NONE != res)
        return res;

    strcpy(*name_ptr, error_name);
    return JVMTI_ERROR_NONE;
}

/*
 * Set Verbose Flag
 *
 * Control verbose output. This is the output which typically is sent
 * to stderr.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiSetVerboseFlag(jvmtiEnv* env,
                    jvmtiVerboseFlag flag,
                    jboolean value)
{
    TRACE2("jvmti.general", "SetVerboseFlag called, flag = " << flag << " , value = " << value);
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase* phases = NULL;

    CHECK_EVERYTHING();

    const char* category = "";
    switch (flag)
    {
    case JVMTI_VERBOSE_OTHER:
        break;
    case JVMTI_VERBOSE_GC:
        category = LOG_GC_INFO;
    case JVMTI_VERBOSE_CLASS:
        category = LOG_CLASS_INFO;
        break;
    case JVMTI_VERBOSE_JNI:
        category = LOG_JNI_INFO;
        break;
    default:
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }

    if (value) {
        log_enable_info_category(category, 0);
    } else {
        log_disable_info_category(category, 0);
    }

    return JVMTI_ERROR_NONE;
}

/*
 * Get JLocation Format
 *
 * Although the greatest functionality is achieved with location
 * information referencing the virtual machine bytecode index,
 * the definition of jlocation has intentionally been left
 * unconstrained to allow VM implementations that do not have
 * this information.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetJLocationFormat(jvmtiEnv* env,
                        jvmtiJlocationFormat* format_ptr)
{
    TRACE2("jvmti.general", "GetJLocationFormat called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase* phases = NULL;

    CHECK_EVERYTHING();

    if (NULL == format_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    *format_ptr = JVMTI_JLOCATION_JVMBCI;

    return JVMTI_ERROR_NONE;
}
