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
 * Memory management functions in JVMTI
 */

#include "jvmti_direct.h"
#include "jvmti_utils.h"
#include "cxxlog.h"
#include "suspend_checker.h"

/*
 * @see official specification for details.
 */
jvmtiError JNICALL
jvmtiAllocate(jvmtiEnv* env, jlong size, unsigned char** res)
{
    TRACE2("jvmti.mem", "Allocate called, size = " << size);
    SuspendEnabledChecker sec;

    /*
     * Check given env & current phase.
     */
    jvmtiPhase* phases = NULL;

    CHECK_EVERYTHING();

    if (size < 0)
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;

    if (size == 0)
    {
        *res = NULL;
        return JVMTI_ERROR_NONE;
    }

    if (res == NULL)
        return JVMTI_ERROR_NULL_POINTER;

    jvmtiError err = _allocate(size, res);
    return err;
}

/*
 * Deallocate
 *
 * Deallocate mem using the JVMTI allocator. This function should
 * be used to deallocate any memory allocated and returned by a
 * JVMTI function (including memory allocated with Allocate). All
 * allocated memory must be deallocated or the memory cannot be
 * reclaimed.
 *
 * REQUIRED Functionality
 */
jvmtiError JNICALL
jvmtiDeallocate(jvmtiEnv* env,
                unsigned char* mem)
{
    TRACE2("jvmti.mem", "Deallocate called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase* phases = NULL;

    CHECK_EVERYTHING();

    if (mem == NULL) {
        return JVMTI_ERROR_NONE;
    }
    else {
        _deallocate(mem);
        return JVMTI_ERROR_NONE;
    }
}

