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
 * JVMTI property API
 */

#include <apr_file_info.h>

#include "open/vm_properties.h"
#include "jvmti_direct.h"
#include "jvmti_utils.h"
#include "environment.h"
#include "properties.h"
#include "cxxlog.h"
#include "port_filepath.h"
#include "suspend_checker.h"
#include "classpath_const.h"

/*
 * Add To Bootstrap Class Loader Search
 *
 * After the bootstrap class loader unsuccessfully searches for
 * a class, the specified platform-dependent search path segment
 * will be searched as well. This function can be used to cause
 * instrumentation classes to be defined by the bootstrap class
 * loader.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiAddToBootstrapClassLoaderSearch(jvmtiEnv* env,
                                     const char* segment)
{
    TRACE2("jvmti.property", "AddToBootstrapClassLoaderSearch called, segment = " << segment);
    SuspendEnabledChecker sec;

    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_ONLOAD};

    CHECK_EVERYTHING();

    if (NULL == segment)
        return JVMTI_ERROR_NULL_POINTER;

    // create temp pool for apr functions
    apr_pool_t *tmp_pool;
    apr_pool_create(&tmp_pool, NULL);

    // check existence of a given path
    apr_finfo_t finfo;
    if(apr_stat(&finfo, segment, APR_FINFO_SIZE, tmp_pool) != APR_SUCCESS) {
        // broken path to the file
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }
    // destroy temp pool
    apr_pool_destroy(tmp_pool);

    // we'll append segment to -Xbootclasspath/a:
    const char* bcp_property = XBOOTCLASSPATH_A;

    // get bootclasspath property
    char *bcp_prop = vm_properties_get_value(bcp_property, VM_PROPERTIES);

    size_t len_bcp = 0;

    if (bcp_prop != NULL) {
        len_bcp = strlen(bcp_prop);
    }

    if (len_bcp != 0) {
        // create new bootclasspath
        char* new_bcp = (char*) STD_ALLOCA(len_bcp + 1 + strlen(segment) + 1);
        strcpy(new_bcp, bcp_prop);
        new_bcp[len_bcp] = PORT_PATH_SEPARATOR;
        strcpy(new_bcp + len_bcp + 1, segment);

        // update bootclasspath property
        vm_properties_set_value(bcp_property, new_bcp, VM_PROPERTIES);
        vm_properties_set_value(bcp_property, new_bcp, JAVA_PROPERTIES);

        STD_FREE(new_bcp);
    } else {
        // update bootclasspath property
        vm_properties_set_value(bcp_property, segment, VM_PROPERTIES);
        vm_properties_set_value(bcp_property, segment, JAVA_PROPERTIES);
    }
    
    vm_properties_destroy_value(bcp_prop);

    return JVMTI_ERROR_NONE;
}

/*
 * Get System Properties
 *
 * The list of VM system property keys which may be used with
 * GetSystemProperty is returned. It is strongly recommended
 * that virtual machines provide the following property keys:
 *    java.vm.vendor
 *    java.vm.version
 *    java.vm.name
 *    java.vm.info
 *    java.library.path
 *    java.class.path
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetSystemProperties(jvmtiEnv* env,
                         jint* count_ptr,
                         char*** property_ptr)
{
    TRACE2("jvmti.property", "GetSystemProperties called");
    SuspendEnabledChecker sec;
    jvmtiError errorCode;

    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_ONLOAD, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (NULL == count_ptr || NULL == property_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    jint properties_count = 0;

    char** keys = vm_properties_get_keys(JAVA_PROPERTIES);
    while(keys[properties_count] != NULL)
        properties_count++;

    char **prop_names_array;
    errorCode = _allocate(sizeof(char*) * properties_count, (unsigned char **)&prop_names_array);
    if (JVMTI_ERROR_NONE != errorCode)
        return errorCode;

    // Copy properties defined in properties list
    for (int iii = 0; iii < properties_count; iii++)
    {
        errorCode = _allocate(strlen(keys[iii]) + 1, (unsigned char **)&prop_names_array[iii]);
        if (JVMTI_ERROR_NONE != errorCode)
        {
            // Free everything that was allocated already
            for (int jjj = 0; jjj < iii; jjj++)
                _deallocate((unsigned char *)prop_names_array[iii]);
            _deallocate((unsigned char *)prop_names_array);
            vm_properties_destroy_keys(keys);
            return errorCode;
        }
        strcpy(prop_names_array[iii], keys[iii]);
    }

    *count_ptr = properties_count;
    *property_ptr = prop_names_array;
    vm_properties_destroy_keys(keys);

    return JVMTI_ERROR_NONE;
}

/*
 * Get System Property
 *
 * Return a VM system property value given the property key.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiGetSystemProperty(jvmtiEnv* env,
                       const char* property,
                       char** value_ptr)
{
    TRACE2("jvmti.property", "GetSystemProperty called, property = " << property);
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_ONLOAD, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    if (NULL == property || NULL == value_ptr)
        return JVMTI_ERROR_NULL_POINTER;

    char *value = vm_properties_get_value(property, JAVA_PROPERTIES);
    if (NULL == value)
        return JVMTI_ERROR_NOT_AVAILABLE;

    char *ret;
    jvmtiError errorCode = _allocate(strlen(value) + 1, (unsigned char **)&ret);
    if (errorCode == JVMTI_ERROR_NONE) {
        strcpy(ret, value);
        *value_ptr = ret;
    }
    vm_properties_destroy_value(value);

    return errorCode;
}

/*
 * Set System Property
 *
 * Set a VM system property value.
 *
 * REQUIRED Functionality.
 */
jvmtiError JNICALL
jvmtiSetSystemProperty(jvmtiEnv* env,
                       const char* property,
                       const char* value)
{
    TRACE2("jvmti.property", "SetSystemProperty called, property = " << property << " value = " << value);
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_ONLOAD};

    CHECK_EVERYTHING();

    if (NULL == property)
        return JVMTI_ERROR_NULL_POINTER;

    if (NULL == value)
        return JVMTI_ERROR_NOT_AVAILABLE;

    Global_Env *vm_env = ((TIEnv*)env)->vm->vm_env;
    vm_properties_set_value(property, value, JAVA_PROPERTIES);

    return JVMTI_ERROR_NONE;
}
