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
#ifndef _JVMTI_UTILS_H_
#define _JVMTI_UTILS_H_

/*
 * Utility functions used internally by jvmti implementation.
 */
#include "jvmti_direct.h"
#include "open/vm_util.h"

/* ************************************************************* */

/*
 * Checks if the TI environment is valid.
 *
 * @return JVMTI_ERROR_INVALID_ENVIRONMENT if environment is invalid,
 *         JVMTI_ERROR_NULL_POINTER if environment is null.
 */
static inline jvmtiError check_environment_is_valid(jvmtiEnv* env)
{
    if (env == NULL)
        return JVMTI_ERROR_NULL_POINTER;

    //TBD: The serious check for env will added here.

    return JVMTI_ERROR_NONE;
}

/* ************************************************************* */

/*
 * The fast version of Allocate function that does not check
 * if the arguments are valid - used by set of TI functions.
 */
static inline jvmtiError _allocate(jlong size, unsigned char** res)
{
#if defined (__INTEL_COMPILER) 
#pragma warning( push )
#pragma warning (disable:1683) // to get rid of remark #1683: explicit conversion of a 64-bit integral type to a smaller integral type
#endif

    *res = (unsigned char*) STD_MALLOC((size_t) size);

#if defined (__INTEL_COMPILER)
#pragma warning( pop )
#endif

    if (*res == NULL)
        return JVMTI_ERROR_OUT_OF_MEMORY;
    return JVMTI_ERROR_NONE;
}

/* ************************************************************* */

/*
 * The fast version of Deallocate function that does not check
 * if the arguments are valid - used by set of TI functions.
 */
static inline void _deallocate(unsigned char* mem)
{
    STD_FREE(mem);
}

/* ************************************************************* *
 *
 * This function check environment and current phase.
 * Input:
 *     - env
 *           environment with functions and data;
 *     - num
 *           number of valid phases, if num = 0, function can be
 *           started on any phase. In this case pointer to array
 *           of valid phases can be NULL;
 *     - phases
 *           pointer to array of valid phases;
 *
 * Output:
 *     - JVMTI_ERROR_NONE
 *           if all checks are "Ok";
 *     - JVMTI_ERROR_WRONG_PHASE
 *           if phase function started in wrong phase or if pointer
 *           to possible phases is NULL;
 *     - JVMTI_ERROR_INVALID_ENVIRONMENT
 *           if env is invalid;
 *
 * Examples:
 *     1. ---------------------------
 *     jvmtiPhase phases[] = {JVMTI_PHASE_ONLOAD, JVMTI_PHASE_ONLOAD};
 *     CHECK_EVERYTHING();
 *
 *     2. ---------------------------
 *     jvmtiPhase phases[] = {}; //for all phases
 *     CHECK_EVERYTHING();
 *
 * ************************************************************* */

static inline jvmtiError check_everything(jvmtiEnv* env,
                                          jint num,
                                          jvmtiPhase* phases);
static inline jvmtiError check_env(jvmtiEnv* env);

static inline jvmtiError check_phase(jvmtiEnv* env,
        jint num,
        jvmtiPhase* phases);

/* ************************************************************* */

static inline jvmtiError check_everything(jvmtiEnv* env,
                                          jint num,
                                          jvmtiPhase* phases)
{
    jvmtiError res_env = check_env(env);

    if (res_env != JVMTI_ERROR_NONE)
    {
        return res_env;
    }

    jvmtiError res_phs = check_phase(env, num, phases);

    if (res_phs != JVMTI_ERROR_NONE)
    {
        return res_phs;
    }

    return JVMTI_ERROR_NONE;
}

/* ************************************************************* */

static inline jvmtiError check_env(jvmtiEnv* env)
{
    jvmtiError errorCode = check_environment_is_valid(env);

    if (errorCode != JVMTI_ERROR_NONE)
    {
        return errorCode;
    }

    return JVMTI_ERROR_NONE;
}

/* ************************************************************* */

static inline jvmtiError check_phase(jvmtiEnv* env, jint num, jvmtiPhase* phases)
{
    if (num == 0) // function can works on all phases
    {
        return JVMTI_ERROR_NONE;
    }
    else
    {
        bool is_valid = false;
        jvmtiPhase phase;

        jvmtiError result = env->GetPhase(&phase);

        if (phases == NULL)
        {
            return JVMTI_ERROR_WRONG_PHASE;
        }

        if (result == JVMTI_ERROR_NONE)
        {
            for (int i = 0; i < num; i++)
            {
                if (phases[i] == phase)
                {
                    is_valid = true;
                }
            }

            if (is_valid)
            {
                return JVMTI_ERROR_NONE;
            }
            else
            {
                return JVMTI_ERROR_WRONG_PHASE;
            }
        }
        else
        {
            return result;
        }
    }
}

/* ************************************************************* */

#define CHECK_EVERYTHING()                                        \
{                                                                 \
    jvmtiError error_code_ = (phases != NULL) ?                   \
            check_everything( env,                                \
            sizeof(phases)/sizeof(jvmtiPhase),                    \
            phases) : check_everything( env, 0,                   \
            phases);                                              \
                                                                  \
    if( error_code_ != JVMTI_ERROR_NONE )                         \
            return error_code_;                                   \
}

/* ************************************************************* */

#define CHECK_CAPABILITY(capability)                              \
{                                                                 \
    jvmtiCapabilities capa;                                       \
    jvmtiError err = env -> GetCapabilities(&capa);               \
    if (err != JVMTI_ERROR_NONE) return err;                      \
    if (!capa.capability)                                         \
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;               \
}                                                                 \

/* ************************************************************* */

#endif  /* _JVMTI_UTILS_H_ */

