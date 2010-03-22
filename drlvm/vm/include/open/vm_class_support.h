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
 * @author Intel, Pavel Pervov
 */  

#ifndef _VM_CLASS_SUPPORT_H
#define _VM_CLASS_SUPPORT_H

/**
 * @file
 * Class Support interface exposed by VM.
 *
 * This file combines separate groups of interfaces commonly reffered to as
 * Class Support.
 *
 * If you do not know which part of Class Support interfaces you need,
 * include this file.
 */

#include "vm_class_loading.h"
#include "vm_class_manipulation.h"
#include "vm_method_access.h"
#include "vm_field_access.h"
#include "vm_type_access.h"


/* Inclusion of all these functions into class support interface is yet to decide. */

/* "through VM" method */

/**
 * Called by a EE to have the VM replace a section of executable code in a thread-safe fashion. 
 */
void vm_patch_code_block(U_8* code_block, U_8* new_code, size_t size);

/**
 * Called by a JIT to have the VM recompile a method using the specified JIT. After 
 * recompilation, the corresponding vtable entries will be updated, and the necessary 
 * callbacks to JIT_recompiled_method_callback will be made. It is a requirement that 
 * the method has not already been compiled by the given JIT; this means that multiple 
 * instances of a JIT may need to be active at the same time. (See vm_clone_jit.)
 */
void vm_recompile_method(JIT_Handle jit, Method_Handle method);

/**
 * Returns the class handle corresponding to a given allocation handle.
 */
Class_Handle allocation_handle_get_class(Allocation_Handle ah);

/**
 * Returns the offset to the vtable pointer in an object.
 */
size_t object_get_vtable_offset();

/**
 * Returns the offset from the start of the vtable at which the
 * superclass hierarchy is stored.  This is for use with fast type
 * checking.
 */
int vtable_get_super_array_offset();

/**
 * Returns TRUE if vtable pointers within objects are to be treated
 * as offsets rather than raw pointers.
 */
BOOLEAN vm_is_vtable_compressed();

/**
 * @return the number of bytes allocated by VM in VTable
 *         for use by GC.
 */
size_t vm_number_of_gc_bytes_in_vtable();

/**
 * Returns the base address of the vtable memory area.  This value will
 * never change and can be cached at startup.
 */
void* vm_get_vtable_base_address();

/**
 * Returns the width in bytes (e.g. 4 or 8) of the vtable type
 * information in each object's header.  This is typically used
 * by the JIT for generating type-checking code, e.g. for inlined
 * type checks or for inlining of virtual methods.
 */
unsigned vm_get_runtime_type_handle_width();

/**
 * Returns the number of superclass hierarchy elements that are
 * stored within the vtable.  This is for use with fast type checking.
 */
int vm_max_fast_instanceof_depth();

/* array object access */

/**
 * allocate new vector.
 *
 * @param array_class Class structure of array to be allocated.
 * @param length length of array to be allocated.
 * @return pointer to the allocated array,
 *         NULL if exception occured, and context is not unwindable,
 *         no-return if exception occured in unwindable context.
 *           
 * @throws OutOfMemoryError
 * @note in case that this method is called from jitted code,
 *       unwinding is performed, and this method never returns.
 *       In all other cases, current thread exception is set,
 *       and control is returned.
 * @note implemented in vm_arrays.cpp
 *           
 * NOTE: reference to internal type Class
 */
Vector_Handle vm_new_vector(Class* array_class, int length);

/** 
 * allocate new primitive vector.
 *
 * @copydoc vm_new_vector() 
 *
 * @note attempt to pass non-primitive array class to this function
 *       will cause undefined results.
 *
 * NOTE: reference to internal type Class
 */
Vector_Handle vm_new_vector_primitive(Class* vector_class, int length);

/**
 * Return the offset to the length field of the array.  That field has
 * the same offset for vectors of all types.
 */
int vector_length_offset();

/**
 * Return the offset to the first element of the vector of the given type.
 * Assume that the elements are boxed. Byte offset.
 */
int vector_first_element_offset_class_handle(Class_Handle element_type);

/**
 * Return the offset to the first element of the vector of the given type.
 * This function is provided for the cases when the class handle of the
 * element is not available.
 */
int vector_first_element_offset(VM_Data_Type element_type);

/**
 * Return the offset to the first element of the vector of the given type.
 * If the class is a value type, assume that elements are unboxed.
 * If the class is not a value type, assume that elements are references.
 */
int vector_first_element_offset_unboxed(Class_Handle element_type);

/* object allocation */


/**
 * allocate new object using its Class structure.
 *
 * @param clss Class structure of the object.
 * @note XXX exception and out of memory semantics are not specified -salikh
 * NOTE: reference to internal types Java_java_lang_Object, Class
 */
Java_java_lang_Object* class_alloc_new_object(Class* clss);

/**
 * allocates new object using its VTable structure.
 *
 * @param vtable VTable structure of the object.
 * @note XXX exception and out of memory semantics are not specified -salikh
 * NOTE: reference to internal types Java_java_lang_Object, VTable
 */
Java_java_lang_Object* class_alloc_new_object_using_vtable(VTable *vtable);

/* Kernel */

/**
 * Find Class With Class Loader.
 *
 * NOTE: reference to internal type ClassLoader
 */
jclass FindClassWithClassLoader(const char* name, ClassLoader* loader);

/**
 * Find Class With Class Loader.
 *
 * NOTE: reference to internal type ClassLoader
 */
jclass SignatureToClass (JNIEnv* env_ext, const char* sig, ClassLoader* class_loader);

/**
 * Find field.
 *
 * NOTE: reference to internal types Field, Class
 */
Field* LookupField(Class* clss, const char* name);

/**
 * Find method.
 *
 * NOTE: reference to internal types Method, Class
 */
Method* LookupMethod (Class* clss, const char* mname, const char* msig);
/* NOTE: reference to internal type ClassLoader */

/**
 * Get Method Parameter Types.
 */
jclass* GetMethodParameterTypes (JNIEnv* env, const char* sig, unsigned short* nparams, ClassLoader* class_loader);

#endif /* _VM_CLASS_SUPPORT_H */
