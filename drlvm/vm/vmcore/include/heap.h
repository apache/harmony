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
#ifndef _HEAP_H
#define _HEAP_H
/**
 * @file
 *
 * This include file specifies the precise interface to heap
 * to be used by all VM components. This is needed in order
 * to provide interface more convenient than raw GC interface.
 * 
 * Using these methods instead of direct GC interface will
 * improve readability, make code more concise, and automatically
 * ensure that all needed checks (like out of memory) are done 
 * properly.
 *
 * @note VM Component internal interface
 * @see \link interface_classification VM interfaces classification \endlink
 */

#include "open/types.h"
#include "object_layout.h"
#include "Class.h"

/**
 * calls gc_alloc and checks for OutOfMemoryError
 * by comparing allocation result with NULL and throwing exception
 * as appropriate.
 * 
 * @note Implemented in jit_runtime_support.cpp
 */
void *vm_malloc_with_thread_pointer(unsigned size, Allocation_Handle ah, void *tp);

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
 */
VMEXPORT // temporary solution for interpreter unplug
Vector_Handle vm_new_vector(Class *array_class, int length);

/** 
 * allocate new primitive vector.
 *
 * @copydoc vm_new_vector() 
 *
 * @note attempt to pass non-primitive array class to this function
 *       will cause undefined results.
 */
VMEXPORT // temporary solution for interpreter unplug
Vector_Handle vm_new_vector_primitive(Class *array_class, int length);

/** 
 * allocates new vector.
 * @copydoc vm_new_vector() 
 * 
 * @note XXX The purpose of this function is not clear. -salikh
 */
Vector_Handle vm_new_vector_or_null(Class *array_class, int length);

/**
 * allocates new vector.
 *
 * @param vector_handle VTable structure of array.
 * @param length length of array to be allocated.
 *         NULL if exception occured, and context is not unwindable,
 *         no-return if exception occured in unwindable context.
 * @param tp pointer to thread-local data.
 *
 * @return pointer to the allocated array,
 *
 * @throws OutOfMemoryError
 *
 * @note implemented in vm_arrays.cpp
 *
 * @note in case that this method is called from jitted code,
 *       unwinding is performed, and this method never returns.
 *       In all other cases, current thread exception is set,
 *       and control is returned.
 */
Vector_Handle vm_new_vector_using_vtable_and_thread_pointer(
    int length, Allocation_Handle vector_handle, void *tp);

/**
 * allocates new vector.
 * @param vector_handle pointer to array vtable.
 * @copydoc vm_new_vector_using_vtable_and_thread_pointer()
 * @param tp pointer to thread-local data.
 *
 * @note XXX The purpose of this function is not clear. -salikh
 */
Vector_Handle vm_new_vector_or_null_using_vtable_and_thread_pointer(
    int length, Allocation_Handle vector_handle, void *tp);

/**
 * alocates new multidimensional array recursively.
 *
 * Recursive allocation means that all subarray are also
 * allocated, and references are stored to top-level multidimensional array,
 * downto simple reference arrays (arrays of dimension 1).
 *
 * @param clss Class structure of the multidimensional array.
 * @param length_array dimensions of the array specified as int array
 * @param dimensions a number of array dimensions
 */
Vector_Handle vm_multianewarray_recursive(
    Class* clss, int* length_array, unsigned dimensions);

/**
 * allocates new multidimensional array.
 *
 * @param clss Class structure of the multidimensional array.
 * @param dimensions a number of array dimensions
 * @note XXX The full specification of this function is not clear. -salikh
 */
Vector_Handle vm_multianewarray_resolved(
    Class *clss, unsigned dimensions, ...);

/**
 * updates statistics of vertor allocation.
 *
 * @note XXX is it used in debug version only? -salikh
 */
void vm_new_vector_update_stats(
    int length, Allocation_Handle vector_handle, void *tp);

/**
 * allocate new object using its Class structure.
 *
 * @param clss Class structure of the object.
 * @note XXX exception and out of memory semantics are not specified -salikh
 */
VMEXPORT ManagedObject *class_alloc_new_object(Class *clss);

/**
 * allocates new object using its VTable structure.
 *
 * @param vtable VTable structure of the object.
 * @note XXX exception and out of memory semantics are not specified -salikh
 */
VMEXPORT ManagedObject *class_alloc_new_object_using_vtable(
    VTable *vtable);

/**
 * allocates new object and runs its default () constructor.
 * 
 * @copydoc class_alloc_new_object()
 * @note XXX exception and out of memory semantics are not specified -salikh
 */
ManagedObject *class_alloc_new_object_and_run_default_constructor(
    Class *clss);

/**
 * allocates new object and runs specified constructor.
 *
 * @copydoc class_alloc_new_object()
 * @param constructor Method structure of constructor
 * @param constructor_args arguments to be passed to constructor
 * @note XXX exception and out of memory semantics are not specified -salikh
 */
ManagedObject *class_alloc_new_object_and_run_constructor(
    Class * clss, Method * constructor, U_8 * constructor_args);

#endif /* #ifndef _HEAP_H */
