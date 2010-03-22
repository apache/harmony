/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#ifndef _VM_COMPONENT_MANAGER_H
#define _VM_COMPONENT_MANAGER_H

#include "open/compmgr.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * If no component manager exist, initializes a component manager.
 * Otherwise, increases a component manager reference count.
 *
 * This function is safe to call from multiple threads.
 * 
 * @param[out] p_cm - on return, points to a component manager
 *                    interface handle
 *
 * @return <code>APR_SUCCESS</code> if successful, or a non-zero error code.
 */
int CmAcquire(OpenComponentManagerHandle* p_cm);

/**
 * Decrement a reference counter and destroy a component manager if it
 * becomes zero. The caller should ensure no cached handles will be used.
 *
 * This function is safe to call from multiple threads.
 *
 * @return <code>APR_SUCCESS</code> if successful, or a non-zero error code.
 */
int CmRelease();

/**
 * Register a buitin component in a component manager.
 *
 * @param init_func - initializer function which provides a default
 *                    and private interfaces for the component
 *
 * This function is safe to call from multiple threads.
 *
 * @return <code>APR_SUCCESS</code> if successful, or a non-zero error code.
 */
int CmAddComponent(OpenComponentInitializer init_func);

/**
 * Load a dynamic library if not loaded, and register
 * the component with a given initializer function
 * in a component manager.
 *
 * This function is safe to call from multiple threads.
 *
 * @param path                       - path to DLL which contains a component
 * @param initializer_function_name  - a name of a function of <code>OpenComponentInitializer</code> 
 *                                     type that registers a component in a component manager
 *
 * @return <code>APR_SUCCESS</code> if successful, or a non-zero error code.
 */
int CmLoadComponent(const char* path,
                    const char* initializer_function_name);

/**
 * Does nothing if there was more successful attempts to add the component than
 * component releases, otherwise deallocates all instances of a given component, unregisters a
 * component in the component manager, and frees all component resources using
 * <code>Free</code> function. If the component is loaded from a dynamic 
 * library and no other components are using the library, then unloads
 * the dynamic library.
 *
 * This function is safe to call from multiple threads.
 *
 * @return <code>APR_SUCCESS</code> if successful, or a non-zero error code.
 */
int CmReleaseComponent(const char* component_name);


#ifdef __cplusplus
}
#endif

#endif /* _VM_COMPONENT_MANAGER_H */
