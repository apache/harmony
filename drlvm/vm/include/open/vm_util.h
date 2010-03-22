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

#ifndef _VM_UTILS_H_
#define _VM_UTILS_H_

#include <apr_general.h>
#include "port_malloc.h"
#include "open/types.h"

struct Global_Env;

class VM_Global_State {
public:
    VMEXPORT static Global_Env *loader_env;
}; //VM_Global_State


extern VTable *cached_object_array_vtable_ptr;

/**
 * Runtime support functions exported directly, because they 
 * may be called from native code.
 */

Boolean class_is_subtype(Class *sub, Class *super);

/**
 * Like <code>class_is_subtype</code>, but <code>sub</code> must not be an 
 * interface class.
 */
Boolean class_is_subtype_fast(VTable *sub, Class *super);


#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Generates an VM's helper to invoke the provided function.
 *
 * The helper takes the <code>void*</code> parameter, which is passed 
 * to the function after some preparation made (namely GC and stack 
 * info are prepared to allow GC to work properly).
 *
 * The function must follow stdcall convention, which takes <code>void*</code> and
 * returns <code>void*</code>, so does the helper.
 * On a return from the function, the helper checks whether an exception
 * was raised for the current thread, and rethrows it if necessary.
 */
VMEXPORT void * vm_create_helper_for_function(void* (*fptr)(void*));

#ifdef __cplusplus
}
#endif

#endif /* #ifndef _VM_UTILS_H_ */



