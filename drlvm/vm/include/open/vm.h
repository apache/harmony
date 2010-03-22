/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
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
 * These are the functions that a VM built as a DLL must export.
 * Some functions may be optional and are marked as such.
 */

#ifndef _VM_EXPORT_H
#define _VM_EXPORT_H

#define OPEN_VM "vm"
#define OPEN_VM_VERSION "1.0"
#define OPEN_INTF_VM "open.interface.vm." OPEN_VM_VERSION

#define O_A_H_VM_VMDIR  "org.apache.harmony.vm.vmdir"

#ifdef WIN32
#include <stddef.h>
#else
#include <unistd.h>
#endif
#include "open/types.h"


#ifdef __cplusplus
extern "C" {
#endif

/**
 * Dynamic interface adapter, returns specific API by its name.
 */
DECLARE_OPEN(void *, vm_get_interface, (const char*));


/**
 * Number of instance fields defined in a class. That doesn't include
 * inherited fields.
 */
 VMEXPORT unsigned class_num_instance_fields(Class_Handle ch);

/**
 * Get the handle for a field. If <code>idx</code> is greater than or equal to
 * <code>class_num_instance_fields</code>. 
 *
 * @return <code>NULL</code>
 *
 * The value of idx indexes into the fields defined in this class and
 * doesn't include inherited fields.
 */
 VMEXPORT Field_Handle class_get_instance_field(Class_Handle ch, unsigned idx);

/**
 * Number of instance fields defined in a class. This number includes
 * inherited fields.
 */

VMEXPORT unsigned class_num_instance_fields_recursive(Class_Handle ch);
/**
 * Get the handle for a field.  
 *
 * @return  <code>NULL</code> if idx is greater than or equal to
 *          <code>class_num_instance_fields_recursive</code>.
 *
 * The value of idx indexes into the set of fields that includes both fields
 * defined in this class and inherited fields.
 */
VMEXPORT Field_Handle class_get_instance_field_recursive(Class_Handle ch, unsigned idx);

/**
 * Number of methods declared in the class.
 */
 VMEXPORT unsigned class_get_number_methods(Class_Handle ch);

/**
 * @return <code>TRUE</code> if all instances of this class are pinned.
 */
 VMEXPORT void* class_alloc_via_classloader(Class_Handle ch, I_32 size);

/**
 * This exactly what I want.
 * Get the alignment of the class.
 */
 VMEXPORT unsigned class_get_alignment(Class_Handle ch);

 /**
 * Returns the size of an instance in the heap, in bytes.
 * 
 * @param klass - the class handle
 *
 * @return The size of an instance in the heap.
 */
 DECLARE_OPEN(size_t, class_get_object_size, (Class_Handle ch));

/**
 * @return The offset to the start of user data form the start of a boxed
 *         instance.
 */
 VMEXPORT unsigned class_get_unboxed_data_offset(Class_Handle ch);

/**
 * @return Class handle given object's <code>VTable_Handle</code>.
 */ 
 DECLARE_OPEN(Class_Handle, vtable_get_class, (VTable_Handle vh));


////
// begin inner-class related functions.
///

/**
 * @return <code>TRUE</code> the number of inner classes.
 */ 
 VMEXPORT unsigned class_number_inner_classes(Class_Handle ch);

/**
 * @return <code>TRUE</code> if an inner class is public.
 */ 
 VMEXPORT Boolean class_is_inner_class_public(Class_Handle ch, unsigned idx);

/**
 * @return an inner class
 */
 VMEXPORT Class_Handle class_get_inner_class(Class_Handle ch, unsigned idx);

/**
 * @return the class that declared this one, or <code>NULL</code> if top-level class
 */
 VMEXPORT Class_Handle class_get_declaring_class(Class_Handle ch);


///
// end class-related functions.
///

////
// begin field-related functions.
////

/**
 * @return <code>TRUE</code> if the field must be enumerated by GC
 *
 * This function doesn't cause resolution of the class of the field.
 *
 * FIXME: move to internal headers
 */
 VMEXPORT Boolean field_is_enumerable_reference(Field_Handle fh);

////
// end field-related functions.
////

////
// begin vector layout functions.
////

/**
 * Vectors are one-dimensional, zero-based arrays. All Java 
 * <code>arrays</code> are vectors.
 * Functions provided in this section do not work on multidimensional or
 * non-zero based arrays (i.e. arrays with a lower bound that is non-zero
 * for at least one of the dimensions.
 */

/**
 * Return the offset to the length field of the array. That field has
 * the same offset for vectors of all types.
 */
VMEXPORT int vector_length_offset();


/**
 * Return the offset to the first element of the vector of the given type.
 * This function is provided for the cases when the class handle of the
 * element is not available.
 */
VMEXPORT int vector_first_element_offset(VM_Data_Type element_type);


/**
 * Return the offset to the first element of the vector of the given type.
 * Assume that the elements are boxed. Byte offset.
 */ 
VMEXPORT int vector_first_element_offset_class_handle(Class_Handle element_type);

/**
 * Return the offset to the first element of the vector of the given type.
 * If the class is a value type, assume that elements are unboxed.
 * If the class is not a value type, assume that elements are references.
 */ 
VMEXPORT int vector_first_element_offset_unboxed(Class_Handle element_type);

/**
 * Return the length of a vector. The caller must ensure that GC will not
 * move or deallocate the vector while vector_get_length() is active.
 */
VMEXPORT I_32 vector_get_length(Vector_Handle vector);

/**
 * Return the address to an element of a vector of references.
 * The caller must ensure that GC will not move or deallocate the vector
 * while vector_get_element_address_ref() is active.
 */
VMEXPORT Managed_Object_Handle *
vector_get_element_address_ref(Vector_Handle vector, I_32 idx);

/**
 * Return the size of a vector of a given number of elements.
 * The size is rounded up to take alignment into account.
 */
VMEXPORT unsigned vm_vector_size(Class_Handle vector_class, int length);

////
// end vector layout functions.
////


/**
 * @return <code>TRUE</code> if references within objects and vector elements are
 *          to be treated as offsets rather than raw pointers.
 */
DECLARE_OPEN(BOOLEAN, vm_is_heap_compressed, ());

/**
 * @return The starting address of the GC heap.
 */
DECLARE_OPEN(void *, vm_get_heap_base_address, ());

/**
 * @return The ending address of the GC heap.
 */
DECLARE_OPEN(void *, vm_get_heap_ceiling_address, ());

/**
 * @return <code>TRUE</code> if vtable pointers within objects are to be treated
 *         as offsets rather than raw pointers.
 */
DECLARE_OPEN(BOOLEAN, vm_is_vtable_compressed, ());

/**
 * @return The base address of the vtable memory area. This value will
 *         never change and can be cached at startup.
 */
DECLARE_OPEN(void *, vm_get_vtable_base_address, ());

#ifdef __cplusplus
}
#endif


#endif // _VM_EXPORT_H
