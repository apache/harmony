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
 * @author Ilya Berezhniuk
 */

//#include <memory.h>
//#include <string.h>
//#include <stdio.h>

#define LOG_DOMAIN "ncai.methods"
#include "cxxlog.h"
#include "suspend_checker.h"
#include "interpreter_exports.h"
#include "jit_intf_cpp.h"
#include "cci.h"

#include "ncai_utils.h"
#include "ncai_direct.h"
#include "ncai_internal.h"


ncaiError JNICALL
ncaiGetMethodLocation(ncaiEnv *env, jmethodID method,
    void** address_ptr, size_t* size_ptr)
{
    TRACE2("ncai.methods", "GetMethodLocation called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (address_ptr == NULL || size_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    if (method == NULL)
        return NCAI_ERROR_INVALID_METHOD;

    if (interpreter_enabled())
        return NCAI_ERROR_INTERPRETER_USED;

    Method* m = (Method*)method;

    if (m->get_state() != Method::ST_Compiled)
        return NCAI_ERROR_NOT_COMPILED;

    *address_ptr = m->get_code_addr();

    CodeChunkInfo* cci = m->get_first_JIT_specific_info();
    *size_ptr = cci->get_code_block_size();

    return NCAI_ERROR_NONE;
}


ncaiError JNICALL
ncaiGetNativeLocation(ncaiEnv *env,
    jmethodID method, jlocation location, void** address_ptr)
{
    TRACE2("ncai.methods", "GetNativeLocation called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (address_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    if (method == NULL)
        return NCAI_ERROR_INVALID_METHOD;

    if (interpreter_enabled())
        return NCAI_ERROR_INTERPRETER_USED;

    Method* m = (Method*)method;

    if (m->get_state() != Method::ST_Compiled)
        return NCAI_ERROR_NOT_COMPILED;

#if defined (__INTEL_COMPILER)
#pragma warning( push )
#pragma warning (disable:1683) // to get rid of remark #1683: explicit conversion of a 64-bit integral type to a smaller integral type
#endif

    if (location < 0 || unsigned(location) >= m->get_byte_code_size())
        return NCAI_ERROR_INVALID_LOCATION;

#if defined (__INTEL_COMPILER)
#pragma warning( pop )
#endif

    // Find native location
    NativeCodePtr np = NULL;

    for (CodeChunkInfo* cci = m->get_first_JIT_specific_info();
         cci; cci = cci->_next)
    {
        JIT *jit = cci->get_jit();
        OpenExeJpdaError res = jit->get_native_location_for_bc(m,
            (uint16)location, &np);
        if (res == EXE_ERROR_NONE)
            break;
    }

    if (NULL == np)
        return NCAI_ERROR_INVALID_LOCATION;

    *address_ptr = (void*)np;
    return NCAI_ERROR_NONE;
}


ncaiError JNICALL
ncaiIsMethodCompiled(ncaiEnv *env,
    jmethodID method, jboolean* is_compiled_ptr)
{
    TRACE2("ncai.methods", "IsMethodCompiled called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (is_compiled_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    if (method == NULL)
        return NCAI_ERROR_INVALID_METHOD;

    if (interpreter_enabled())
        return NCAI_ERROR_INTERPRETER_USED;

    Method* m = (Method*)method;

    *is_compiled_ptr =
        (jboolean)(m->get_state() == Method::ST_Compiled);

    return NCAI_ERROR_NONE;
}
