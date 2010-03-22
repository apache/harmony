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
#ifndef _VM_TYPE_ACCESS_H
#define _VM_TYPE_ACCESS_H

#include "common.h"
#include "types.h"
/**
 * @file
 * Part of Class Support interface related to accessing generalized
 * types' properties.
 */

#ifdef __cplusplus
extern "C" {
#endif

/**
* If type info is unboxed, <code>type_info_get_class</code> 
* will return the class of the unboxed type and <code>class_is_primitive<code>
* will return its <code>VM_Data_Type</code>.
* FIXME not used
*/ 
DECLARE_OPEN(BOOLEAN, type_info_is_unboxed, (Type_Info_Handle tih));

/**
* If <code>TRUE</code>, then <code>type_info_get_type_info</code>
* returns the type info that the pointer points to.
* FIXME not used
*/
DECLARE_OPEN(BOOLEAN, type_info_is_unmanaged_pointer, (Type_Info_Handle tih));

/**
* If <code>TRUE</code>, then <code>type_info_get_type_info</code>
* returns the type info that the pointer points to.
* FIXME???
*/
DECLARE_OPEN(BOOLEAN, type_info_is_managed_pointer, (Type_Info_Handle tih));

/**
* If <code>TRUE</code>, use <code>type_info_get_method_sig</code> to 
* retrieve the method signature.
* FIXME not used
*/
DECLARE_OPEN(BOOLEAN, type_info_is_method_pointer, (Type_Info_Handle tih));

/**
* Is it a general array, i.e., either multidimensional or non zero-based.
*
* @return <code>FALSE</code> for vectors. Always <code>FALSE</code> for Java.
* FIXME not used
*/ 
DECLARE_OPEN(BOOLEAN, type_info_is_general_array, (Type_Info_Handle tih));;

/**
* Get the method signature if <code>type_info_is_method_pointer</code> 
* returned <code>TRUE</code>.
* FIXME not used
*/
DECLARE_OPEN(Method_Signature_Handle, type_info_get_method_sig, (Type_Info_Handle tih));


/**
* Return an <code>VM_Data_Type</code> corresponding to a type info.
* This function is provided for convenience as it can be implemented in terms
* of other functions provided in this interface.
*/
DECLARE_OPEN(VM_Data_Type, type_info_get_type, (Type_Info_Handle tih));



/**
* Gets Type_Info_Handle from the given type name.
* Does'n resolve type if not resolved
*/
DECLARE_OPEN(Type_Info_Handle, type_info_create_from_java_descriptor, (Class_Loader_Handle cl, const char* typeName));

/**
* Get the name of the class referenced by this type info handle
*/
DECLARE_OPEN(const char* , type_info_get_type_name, (Type_Info_Handle tih));

/**
* Get the class if <code>type_info_is_reference</code> or 
* <code>type_info_is_unboxed</code> returned <code>TRUE</code>. 
* If the type info is a vector or a general array, return the
* class handle for the array type (not the element type).
*/
DECLARE_OPEN(Class_Handle, type_info_get_class, (Type_Info_Handle tih));

/**
* Get the class if <code>type_info_is_reference</code> or 
* <code>type_info_is_unboxed</code> returned <code>TRUE</code>. 
* If the type info is a vector or a general array, return the
* class handle for the array type (not the element type).
* Invokes class loader with no exception but preserves previously
* raised exception.
*/
DECLARE_OPEN(Class_Handle, type_info_get_class_no_exn, (Type_Info_Handle tih));

/**
* Returns number of array dimension of a type referenced by the 
* given type info handle.
*/
DECLARE_OPEN(U_32, type_info_get_num_array_dimensions, (Type_Info_Handle tih));

/**
* Get recursively type info if <code>type_info_is_unmanaged_pointer</code>,
* <code>type_info_is_vector</code> or <code>type_info_is_general_array</code>
* returned <code>TRUE</code>.
*/
DECLARE_OPEN(Type_Info_Handle, type_info_get_type_info, (Type_Info_Handle tih));

/**
* For a return value a type can be void when it is not an unmanaged pointer.
* In all other contexts, if <code>type_info_is_void</code> is <code>TRUE</code> then
* <code>type_info_is_unmanaged_pointer</code> is <code>TRUE</code> too.
* FIXME???
*/ 
DECLARE_OPEN(BOOLEAN, type_info_is_void, (Type_Info_Handle tih));

/**
* Array shapes and custom modifiers are not implemented yet.
*
* If type info is a reference, <code>type_info_get_class</code> 
* will return the class of the reference.
*/
DECLARE_OPEN(BOOLEAN, type_info_is_reference, (Type_Info_Handle tih));

/**
* Checks if a type referenced by the given type info handle is resolved
*/
DECLARE_OPEN(BOOLEAN, type_info_is_resolved, (Type_Info_Handle tih));

/**
* @return <code>TRUE</code> if the type is a primitive type. 
*         <code>type_info_is_primitive</code> does
*         not cause resolution in contrast to the otherwise equivalentcall sequence
*         suggested in the description of <code>type_info_is_unboxed</code> 
*        (i.e. <code>type_info_is_unboxed-->type_info_get_class-->class_is_primitive</code>).
*/
DECLARE_OPEN(BOOLEAN, type_info_is_primitive, (Type_Info_Handle tih));

/**
* Is it a vector, i.e., a one-dimensional, zero-based array.
* FIXME? In Java all arrays are vectors
*/ 
DECLARE_OPEN(BOOLEAN, type_info_is_vector, (Type_Info_Handle tih));

#ifdef __cplusplus
}
#endif

#endif // _VM_TYPE_ACCESS_H
