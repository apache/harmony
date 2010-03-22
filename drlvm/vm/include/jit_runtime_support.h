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
* @file
* @brief Declaration of access interfaces to runtime helpers provided by VM.
*/

#ifndef _JIT_RUNTIME_SUPPORT_H_
#define _JIT_RUNTIME_SUPPORT_H_

#include "open/types.h"
#include "open/rt_helpers.h"
#include "jni_types.h"

#define _DEBUG_CHECK_NULL_

#ifdef __cplusplus
extern "C" {
#endif


DECLARE_OPEN(void *, vm_helper_get_addr, (VM_RT_SUPPORT f));


/**
 * Temporary interface addition
 * same as <code>vm_helper_get_addr</code>, but tries to optimize the stubs it creates,
 * by specializing them.
 */
DECLARE_OPEN(void *, vm_helper_get_addr_optimized, (VM_RT_SUPPORT f, Class_Handle c));

/**
 *  Checks if helper is a suspension point
 */
DECLARE_OPEN(HELPER_INTERRUPTIBILITY_KIND, vm_helper_get_interruptibility_kind, (VM_RT_SUPPORT f));

DECLARE_OPEN(HELPER_CALLING_CONVENTION, vm_helper_get_calling_convention, (VM_RT_SUPPORT f));
DECLARE_OPEN(const char*, vm_helper_get_name, (VM_RT_SUPPORT id));

/**
 * Returns number of helper arguments.
 * Intended primarily for debugging.
 */
DECLARE_OPEN(U_32, vm_helper_get_numargs, (VM_RT_SUPPORT id));

/**
* Returns Id of runtime helper by its string representation. 
* Name comparison is case-insensitive.
* If the helperName is unknown, then VM_RT_UNKNOWN is returned.
*/
DECLARE_OPEN(VM_RT_SUPPORT, vm_helper_get_by_name, (const char* name));


DECLARE_OPEN(jint, vm_helper_register_magic_helper, (VM_RT_SUPPORT id, 
                                   const char* class_name, 
                                   const char* method_name));

DECLARE_OPEN(Method_Handle, vm_helper_get_magic_helper, (VM_RT_SUPPORT id));

#ifdef __cplusplus
}
#endif // __cplusplus

#endif // !_JIT_RUNTIME_SUPPORT_H_
