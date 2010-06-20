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
 * JVMTI extensions API
 */

#include "jvmti_internal.h"
#include "jvmti_utils.h"
#include "open/vm_util.h"
#include "cxxlog.h"
#include "suspend_checker.h"

struct JvmtiExtension
{
    JvmtiExtension *next;
    jvmtiExtensionFunctionInfo info;
};

static jvmtiParamInfo jvmtiGetNCAIEnvironmentParams[] =
{
    {
        const_cast<char*>("ncai_env_ptr"),
        JVMTI_KIND_OUT,
        JVMTI_TYPE_CVOID,
        JNI_FALSE
    },
    {
        const_cast<char*>("version"),
        JVMTI_KIND_IN,
        JVMTI_TYPE_JINT,
        JNI_FALSE
    }
};

static jvmtiError jvmtiGetNCAIEnvironmentErrors[] =
{ // Universal errors are excluded according to specification
    JVMTI_ERROR_NONE,
};

static JvmtiExtension jvmti_extension_list[] =
{
    {
        NULL,
        {
            jvmtiGetNCAIEnvironment,
            const_cast<char*>("org.apache.harmony.vm.GetExtensionEnv"),
            const_cast<char*>("Returns the reference to the NCAI function table"),
            sizeof(jvmtiGetNCAIEnvironmentParams) / sizeof(jvmtiParamInfo),
            jvmtiGetNCAIEnvironmentParams,
            sizeof(jvmtiGetNCAIEnvironmentErrors) / sizeof(jvmtiError),
            jvmtiGetNCAIEnvironmentErrors
        }
    }

};

static const jint extensions_number = sizeof(jvmti_extension_list) / sizeof(JvmtiExtension);

static void free_allocated_extension_array(jvmtiExtensionFunctionInfo *array,
                                           jint number)
{
    for (int iii = 0; iii <= number; iii++)
    {
        if (array[iii].id)
            _deallocate((unsigned char *)array[iii].id);
        if (array[iii].short_description)
            _deallocate((unsigned char *)array[iii].short_description);
        if (array[iii].errors)
            _deallocate((unsigned char *)array[iii].errors);

        if (array[iii].params)
        {
            for (int jjj = 0; jjj < array[iii].param_count; jjj++)
            {
                if (array[iii].params[jjj].name)
                    _deallocate((unsigned char *)array[iii].params[jjj].name);
            }

            _deallocate((unsigned char *)array[iii].params);
        }
    }
    _deallocate((unsigned char *)array);
}

/*
 * Get Extension Functions
 *
 * Returns the set of extension functions.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetExtensionFunctions(jvmtiEnv* env,
                           jint* extension_count_ptr,
                           jvmtiExtensionFunctionInfo** extensions)
{
    TRACE2("jvmti.extension", "GetExtensionFunctions called");
    SuspendEnabledChecker sec;
    // check environment, phase, pointers
    jvmtiError errorCode;

    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_ONLOAD, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (NULL == extension_count_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    if (NULL == extensions)
        return JVMTI_ERROR_NULL_POINTER;

    *extension_count_ptr = extensions_number;

    if (0 == extensions_number)
    {
        *extensions = NULL;
        return JVMTI_ERROR_NONE;
    }

    jvmtiExtensionFunctionInfo *array;
    size_t arr_size = sizeof(jvmtiExtensionFunctionInfo)*extensions_number;
    errorCode = _allocate(arr_size, (unsigned char **)&array);

    if (JVMTI_ERROR_NONE != errorCode)
        return errorCode;

    memset(array, 0, arr_size);

    JvmtiExtension *ex = jvmti_extension_list;
    for (int iii = 0; iii < extensions_number; iii++)
    {
        jvmtiExtensionFunctionInfo *info = &ex->info;

        array[iii].func = info->func;
        array[iii].param_count = info->param_count;
        array[iii].error_count = info->error_count;

        errorCode = _allocate(strlen(info->id) + 1,
            (unsigned char **)&(array[iii].id));
        if (JVMTI_ERROR_NONE != errorCode)
        {
            free_allocated_extension_array(array, iii);
            return errorCode;
        }

        errorCode = _allocate(strlen(info->short_description) + 1,
            (unsigned char **)&(array[iii].short_description));
        if (JVMTI_ERROR_NONE != errorCode)
        {
            free_allocated_extension_array(array, iii);
            return errorCode;
        }

        errorCode = _allocate(info->param_count * sizeof(jvmtiParamInfo),
            (unsigned char **)&(array[iii].params));
        if (JVMTI_ERROR_NONE != errorCode)
        {
            free_allocated_extension_array(array, iii);
            return errorCode;
        }

        errorCode = _allocate(info->error_count * sizeof(jvmtiError),
            (unsigned char **)&(array[iii].errors));
        if (JVMTI_ERROR_NONE != errorCode)
        {
            free_allocated_extension_array(array, iii);
            return errorCode;
        }

        memset(array[iii].params, 0,
            info->param_count * sizeof(jvmtiParamInfo));

        for (int jjj = 0; jjj < info->param_count; jjj++)
        {
            array[iii].params[jjj] = info->params[jjj];

            errorCode = _allocate(strlen(info->params[jjj].name) + 1,
                (unsigned char **)&(array[iii].params[jjj].name));
            if (JVMTI_ERROR_NONE != errorCode)
            {
                array[iii].params[jjj].name = NULL;
                free_allocated_extension_array(array, iii);
                return errorCode;
            }

            strcpy(array[iii].params[jjj].name, info->params[jjj].name);
        }

        strcpy(array[iii].id, info->id);
        strcpy(array[iii].short_description, info->short_description);
        memcpy(array[iii].errors, info->errors,
            info->error_count * sizeof(jvmtiError));

        ex = ex->next;
    }

    *extensions = array;
    return JVMTI_ERROR_NONE;
}

struct JvmtiExtensionEvent
{
    JvmtiExtensionEvent *next;
    jvmtiExtensionEventInfo info;
};

static JvmtiExtensionEvent *jvmti_exntension_event_list = NULL;

static const jint extensions_events_number = 0;

static void free_allocated_extension_event_array(jvmtiExtensionEventInfo *array,
                                                 jint number)
{
    for (jint iii = 0; iii <= number; iii++)
    {
        if (array[iii].id)
            _deallocate((unsigned char *)array[iii].id);
        if (array[iii].short_description)
            _deallocate((unsigned char *)array[iii].short_description);

        if (array[iii].params)
        {
            for (jint jjj = 0; jjj < array[iii].param_count; jjj++)
            {
                if (array[iii].params[jjj].name)
                    _deallocate((unsigned char *)array[iii].params[jjj].name);
            }

            _deallocate((unsigned char *)array[iii].params);
        }
    }
    _deallocate((unsigned char *)array);
}

/*
 * Get Extension Events
 *
 * Returns the set of extension events.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetExtensionEvents(jvmtiEnv* env,
                        jint* extension_count_ptr,
                        jvmtiExtensionEventInfo** extensions)
{
    TRACE2("jvmti.extension", "GetExtensionEvents called");
    SuspendEnabledChecker sec;
    // check environment, phase, pointers
    jvmtiError errorCode;

    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_ONLOAD, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (NULL == extension_count_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    if (NULL == extensions)
        return JVMTI_ERROR_NULL_POINTER;

    *extension_count_ptr = extensions_events_number;

    if (0 == extensions_events_number)
    {
        *extensions = NULL;
        return JVMTI_ERROR_NONE;
    }

    jvmtiExtensionEventInfo *array;
    size_t arr_size = sizeof(jvmtiExtensionEventInfo)*extensions_events_number;
    errorCode = _allocate(arr_size, (unsigned char **)&array);

    if (JVMTI_ERROR_NONE != errorCode)
        return errorCode;

    memset(array, 0, arr_size);

    JvmtiExtensionEvent *ex = jvmti_exntension_event_list;
    for (int iii = 0; iii < extensions_events_number; iii++)
    {
        jvmtiExtensionEventInfo *info = &ex->info;

        array[iii].extension_event_index = info->extension_event_index;
        array[iii].param_count = info->param_count;

        errorCode = _allocate(strlen(info->id) + 1,
            (unsigned char **)&(array[iii].id));
        if (JVMTI_ERROR_NONE != errorCode)
        {
            free_allocated_extension_event_array(array, iii);
            return errorCode;
        }

        errorCode = _allocate(strlen(info->short_description) + 1,
            (unsigned char **)&(array[iii].short_description));
        if (JVMTI_ERROR_NONE != errorCode)
        {
            free_allocated_extension_event_array(array, iii);
            return errorCode;
        }

        errorCode = _allocate(info->param_count * sizeof(jvmtiParamInfo),
            (unsigned char **)&(array[iii].params));
        if (JVMTI_ERROR_NONE != errorCode)
        {
            free_allocated_extension_event_array(array, iii);
            return errorCode;
        }

        memset(array[iii].params, 0,
            info->param_count * sizeof(jvmtiParamInfo));

        for (int jjj = 0; jjj < info->param_count; jjj++)
        {
            array[iii].params[jjj] = info->params[jjj];

            errorCode = _allocate(strlen(info->params[jjj].name) + 1,
                (unsigned char **)&(array[iii].params[jjj].name));
            if (JVMTI_ERROR_NONE != errorCode)
            {
                array[iii].params[jjj].name = NULL;
                free_allocated_extension_event_array(array, iii);
                return errorCode;
            }

            strcpy(array[iii].params[jjj].name, info->params[jjj].name);
        }

        strcpy(array[iii].id, info->id);
        strcpy(array[iii].short_description, info->short_description);

        ex = ex->next;
    }

    *extensions = array;
    return JVMTI_ERROR_NONE;
}

jvmtiError TIEnv::allocate_extension_event_callbacks_table()
{
    return _allocate(extensions_events_number * sizeof(jvmtiExtensionEvent),
        (unsigned char **)&extension_event_table);
}

/*
 * Set Extension Event Callback
 *
 * Sets the callback function for an extension event and enables
 * the event. Or, if the callback is NULL, disables the event.
 * Note that unlike standard events, setting the callback and
 * enabling the event are a single operation.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiSetExtensionEventCallback(jvmtiEnv* env,
                               jint extension_event_index,
                               jvmtiExtensionEvent callback)
{
    TRACE2("jvmti.extension", "SetExtensionEventCallback called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_ONLOAD, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (extension_event_index > extensions_events_number)
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;

    TIEnv *ti_env = (TIEnv *)env;
    ti_env->extension_event_table[extension_event_index] = callback;

    return JVMTI_ERROR_NONE;
}

