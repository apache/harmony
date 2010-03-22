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
#ifndef _VM_CLASS_LOADING_H
#define _VM_CLASS_LOADING_H

#include "open/types.h"
#include "open/common.h"

/**
 * @file
 * Class loading functionality of the class support interface. 
 * These functions are responsible for loading classes
 * from the virtual machine and interested components.
 */

#ifdef __cplusplus
extern "C" {
#endif

DECLARE_OPEN(Class_Loader_Handle, class_get_class_loader, (Class_Handle ch));

/**
* @return A class corresponding to a primitive type. For all primitive types t
*         t == <code>class_get_primitive_type_of_class(class_get_class_of_primitive_type(t))</code>
*/
DECLARE_OPEN(Class_Handle, class_get_class_of_primitive_type, (VM_Data_Type typ));


/**
* @return An <code>VM_Data_Type</code> value for a given class.
*/
DECLARE_OPEN(VM_Data_Type, class_get_primitive_type_of_class, (Class_Handle ch));

/**
* Given a class handle <code>cl</code> construct a class handle of the type
* representing array of <code>cl</code>. If class cl is value type, assume
* that the element is a reference to a boxed instance of that type.
*/
DECLARE_OPEN(Class_Handle, class_get_array_of_class, (Class_Handle ch));

/** 
 * @defgroup Extended VM Class Loading Extended Interface
 * The extended functionality is implemented on the basis of basic interfaces
 * and enables greater efficiency on the corresponding component side. 
 */

/**
 * Looks up the class only among the classes loaded by the given class loader. 
 * 
 * This class loader does not delegate the lookup operation to 
 * the parent loader or try to load any class.
 *
 * @param classloader - the handle of the C++ class loader. If <code>NULL</code>
 *                      bootstrap class loader is assumed
 * @param name           - the name of the class to look up. Name is accepted
 *  both in internal and external forms (with '/'s and '.'s).
 *
 * @return The handle for C++ class representation, if found. Otherwise, <code>NULL</code>.
 */
DECLARE_OPEN(Class_Handle, class_loader_lookup_class,
    (Class_Loader_Handle classloader, const char* name));


/**
 * Tries to load the class given its name and using the specified class loader.
 *
 * @param classloader    - the handle of the C++ class loader. If <code>NULL</code>
 *                      bootstrap class loader is assumed
 * @param name           - the name of the class to load
 *
 * @return The handle for the C++ class representation, if loaded successfully; otherwise, <code>NULL</code>.
 */
DECLARE_OPEN(Class_Handle, class_loader_load_class,
    (Class_Loader_Handle classloader, const char* name));

/** @ingroup Extended 
 *
 * Finds already loaded class given its name and using the bootstrap class loader.
 *
 * @param name  - the name of the class to load
 * @param exc   - the exception code for a class loading failure
 *
 * @result The handle for the C++ class representation, if found;
 * otherwise, <code>NULL</code>.
 */
DECLARE_OPEN(Class_Handle, vm_lookup_class_with_bootstrap, (const char* name));

/** @ingroup Extended 
 *
 * Tries to load the class given its name and using the bootstrap class loader.
 *
 * @param name  - the name of the class to load
 * @param exc   - the exception code for a class loading failure
 *
 * @result The handle for the C++ class representation, if loaded successfully; otherwise, <code>NULL</code>.
 */
DECLARE_OPEN(Class_Handle, vm_load_class_with_bootstrap, (const char* name));

/**
 * Returns the C++ class structure representing the system 
 * <code>java.lang.Object</code> class.
 *
 * This function is the fast equivalent of the
 * <code>vm_load_class_with_bootstrap("java/lang/Object")</code> function.
 *
 * @return the handle for the <code>java.lang.Object</code> C++ class representation.
 */
DECLARE_OPEN(Class_Handle, vm_get_system_object_class, ());

/**
 * Returns the C++ class structure representing the system 
 * <code>java.lang.Class</code> class.
 *
 * This function is the fast equivalent of the
 * <code>vm_load_class_with_bootstrap("java/lang/Class")</code> function.
 *
 * @return the handle for the <code>java.lang.Class</code> C++ class representation.
 */
DECLARE_OPEN(Class_Handle, vm_get_system_class_class, ());

/**
 * Returns the C++ class structure representing the system class
 * <code>java.lang.String</code>.
 * 
 * This function is the fast equivalent of the
 * <code>vm_load_class_with_bootstrap("java/lang/String")</code> function.
 *
 * @return The handle of <code>java.lang.String</code> C++ class representation.
 */
DECLARE_OPEN(Class_Handle, vm_get_system_string_class, ());

/**
 * Stores the pointer to verifier-specific data into the class loader C++ structure.
 *
 * @param classloader      - the handle to the class loader to set the verifier data in
 * @param data             - the pointer to the verifier data
 */
DECLARE_OPEN(void, class_loader_set_verifier_data_ptr,
    (Class_Loader_Handle classloader, void* data));

/**
 * Returns the pointer to verifier-specific data associated with the given class loader.
 *
 * @param classloader - the handle to class loader to retrieve verifier pointer from
 * 
 * @return The pointer to the verifier data
 */
DECLARE_OPEN(void*, class_loader_get_verifier_data_ptr, (Class_Loader_Handle classloader));

/**
 * Acquires the lock on a given class loader. 
 *
 * @param classloader - the handle to the C++ class loader structure to acquire lock on.
 */
DECLARE_OPEN(void, class_loader_lock, (Class_Loader_Handle classloader));

/**
 * Releases the lock on a given class loader. 
 *
 * @param classloader - the handle to the C++ class loader structure to release lock on. 
 */
DECLARE_OPEN(void, class_loader_unlock, (Class_Loader_Handle classloader));

#ifdef __cplusplus
}
#endif

#endif // _VM_CLASS_LOADING_H
