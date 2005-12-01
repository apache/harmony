/* Copyright 1991, 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @file
 * @ingroup VMLS
 * @brief VM Local Storage Header
 */

#if !defined(HY_VMLS_H)
#define HY_VMLS_H

#if defined(__cplusplus)
extern "C" {
#endif

#include "hycomp.h"
#include "jni.h"

#define HY_VMLS_MAX_KEYS 256

/**
 * @struct HyVMLSFunctionTable
 * The VM local storage function table.
 */
typedef struct HyVMLSFunctionTable {
    UDATA  (JNICALL *HYVMLSAllocKeys)(JNIEnv * env, UDATA * pInitCount, ...) ;
    void  (JNICALL *HYVMLSFreeKeys)(JNIEnv * env, UDATA * pInitCount, ...) ;
    void*  (JNICALL *HyVMLSGet)(JNIEnv * env, void * key) ;
    void*  (JNICALL *HyVMLSSet)(JNIEnv * env, void ** pKey, void * value) ;
} HyVMLSFunctionTable;
#define HYSIZEOF_HyVMLSFunctionTable 16

/**
 * @fn HyVMLSFunctionTable::HYVMLSAllocKeys
 * Allocate one or more slots of VM local storage. 
 *
 * @code UDATA  JNICALL HYVMLSAllocKeys(JNIEnv * env, UDATA * pInitCount, ...); @endcode
 *
 * @param[in] env  A JNIEnv pointer
 * @param[in] pInitCount  Pointer to the reference count for these slots
 * @param[out] ...  Locations to store the allocated keys
 *
 * @return 0 on success, 1 on failure.
 *
 * @note Newly allocated VMLS slots contain NULL in all VMs.
 */
/**
 * @fn HyVMLSFunctionTable::HYVMLSFreeKeys
 * Destroy one or more slots of VM local storage. 
 *
 * @code void  JNICALL HYVMLSFreeKeys(JNIEnv * env, UDATA * pInitCount, ...); @endcode
 *
 * @param[in] env  A JNIEnv pointer
 * @param[in] pInitCount  Pointer to the reference count for these slots
 * @param[out] ...  Pointers to the allocated keys
 */
/**
 * @fn HyVMLSFunctionTable::HyVMLSGet
 * Retrieve the value in a VM local storage slot. 
 *
 * @code void*  JNICALL HyVMLSGet(JNIEnv * env, void * key); @endcode
 *
 * @param[in] env  JNIEnv pointer
 * @param[in] key  The VMLS key
 *
 * @return The contents of the VM local storage slot in the VM that contains the specified env
 */
/**
 * @fn HyVMLSFunctionTable::HyVMLSSet
 * Store a value into a VM local storage slot.
 *
 * @code void*  JNICALL HyVMLSSet(JNIEnv * env, void ** pKey, void * value); @endcode
 *
 * @param[in] env  JNIEnv pointer
 * @param[in] pKey  Pointer to the VM local storage key
 * @param[in] value  Value to store
 *
 * @return The value stored
 */

#if defined(USING_VMI)
#define HY_VMLS_FNTBL(env) (*VMI_GetVMIFromJNIEnv(env))->GetVMLSFunctions(VMI_GetVMIFromJNIEnv(env))
#else
#define HY_VMLS_FNTBL(env) ((HyVMLSFunctionTable *) ((((void ***) (env))[offsetof(HyVMThread,javaVM)/sizeof(UDATA)])[offsetof(HyJavaVM,vmLocalStorageFunctions)/sizeof(UDATA)]))
#endif

#define HY_VMLS_GET(env, key) (HY_VMLS_FNTBL(env)->HyVMLSGet(env, (key)))
#define HY_VMLS_SET(env, key, value) (HY_VMLS_FNTBL(env)->HyVMLSSet(env, &(key), (void *) (value)))

#if defined(__cplusplus)
}
#endif

#endif /* HY_VMLS_H */
