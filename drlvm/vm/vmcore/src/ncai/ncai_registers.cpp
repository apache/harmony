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

#define LOG_DOMAIN "ncai.registers"
#include "cxxlog.h"

#include "suspend_checker.h"
#include "open/ncai_thread.h"
#include "ncai_utils.h"
#include "ncai_internal.h"
#include "ncai_direct.h"


ncaiError JNICALL
ncaiGetRegisterCount(ncaiEnv* env, jint* count_ptr)
{
    TRACE2("ncai.registers", "GetRegisterCount called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (count_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    *count_ptr = ncai_get_reg_table_size();
    return NCAI_ERROR_NONE;
}

ncaiError JNICALL
ncaiGetRegisterInfo(ncaiEnv *env, jint reg_number, ncaiRegisterInfo* info_ptr)
{
    TRACE2("ncai.registers", "GetRegisterInfo called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (reg_number < 0 || (size_t)reg_number >= ncai_get_reg_table_size())
        return NCAI_ERROR_ACCESS_DENIED; // FIXME Select proper error code

    if (info_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    size_t namesize = strlen(g_ncai_reg_table[reg_number].name) + 1;
    char* name = (char*)ncai_alloc(namesize);

    if (name)
        memcpy(name, g_ncai_reg_table[reg_number].name, namesize);

    info_ptr->name = name;
    info_ptr->size = g_ncai_reg_table[reg_number].size;

    return NCAI_ERROR_NONE;
}

ncaiError JNICALL
ncaiGetRegisterValue(ncaiEnv *env, ncaiThread thread,
    jint reg_number, void *buf)
{
    TRACE2("ncai.registers", "GetRegisterValue called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (reg_number < 0 || (size_t)reg_number >= ncai_get_reg_table_size())
        return NCAI_ERROR_ACCESS_DENIED;

    if (buf == NULL)
        return NCAI_ERROR_NULL_POINTER;

    hythread_t hythread = reinterpret_cast<hythread_t>(thread);

    if (hythread_get_suspend_count_native(hythread) <= 0)
        return NCAI_ERROR_THREAD_NOT_SUSPENDED;

    ncai_get_register_value(hythread, reg_number, buf);

    return NCAI_ERROR_NONE;
}

ncaiError JNICALL
ncaiSetRegisterValue(ncaiEnv *env, ncaiThread thread,
    jint reg_number, void *buf)
{
    TRACE2("ncai.registers", "SetRegisterValue called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (reg_number < 0 || (size_t)reg_number >= ncai_get_reg_table_size())
        return NCAI_ERROR_ACCESS_DENIED;

    if (buf == NULL)
        return NCAI_ERROR_NULL_POINTER;

    hythread_t hythread = reinterpret_cast<hythread_t>(thread);

    if (hythread_get_suspend_count_native(hythread) <= 0)
        return NCAI_ERROR_THREAD_NOT_SUSPENDED;

    ncai_set_register_value(hythread, reg_number, buf);

    return NCAI_ERROR_NONE;
}
