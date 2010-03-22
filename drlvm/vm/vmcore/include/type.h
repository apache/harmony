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
// This file describes structures used to keep track of types, class, methods, fields, etc.

#ifndef _TYPE_H_
#define _TYPE_H_

#include "open/types.h"
#include "open/vm.h"
#include "vm_core_types.h"
#include "String_Pool.h"

enum Kind {
    K_S1, K_S2, K_S4, K_S8, K_Sp, K_U1, K_U2, K_U4, K_U8, K_Up, K_F4, K_F8,
    K_Boolean, K_Char, K_Void, 
    K_LAST_PRIMITIVE = K_Void,/*service field to control primitive types bounds*/ 
    K_TypedRef, K_UnboxedValue, K_Object, K_Vector, K_Array, K_MethodPointer, K_UnmanagedPointer, K_ManagedPointer
};

struct TypeDesc {
public:
    //*** Queries

    // Return the most specific kind describing the type
    Kind get_kind();

    // For each type, either all its members are values, or all its members are locations.
    // Return whether this type's members are all values
    bool is_unboxed_value();

    // Is this type one of the primitive ones: S1, S2, S4, S8, Sp, U1, U2, U4, U8, Up, F4, F8, Booean, Char, or Void
    bool is_primitive();

    //*** Arrays

    // For each type, either all its members are arrays (or vectors), or none of its members are arrays.
    // Return whether this type's members are all arrays/vectors.
    bool is_array();

    // For each type, either all its members are vectors, or none of its members are vectors.
    // Return whether this type's members are all vectors.
    bool is_vector();

    // For array/vector types only
    // Return the element type of the array/vector type.
    TypeDesc* get_element_type();

    //*** Pointers

    // For each type, either all its members are managed pointers, or none or its members are managed pointers.
    // Return whether this type's members are all managed pointers.
    bool is_managed_pointer();

    // For each type, either all its members are unmanaged pointers, or none or its members are unmanaged pointers.
    // Return whether this type's members are all unmanaged pointers.
    bool is_unmanaged_pointer();

    // For managed and unmanaged pointer types, return the type of the value pointed to.
    TypeDesc* get_pointed_to_type();

    // For each type, either all its members are method pointers, or none or its members are method pointers.
    // Return whether this type's members are all method pointers.
    bool is_method_pointer();

    TypeDesc* type_desc_create_vector();

///////////////////////////////////////
//  Loader Interface

    // For types that are loaded from class definitions, return the identifier of the type.
    // For other types return NULL.
    Class* load_type_desc();

    bool is_loaded();

    const String* get_type_name(){
        return name;
    }

    ClassLoader* get_classloader(){
        return loader;
    }

private:
    TypeDesc* get_vector_type(){
        return vector_type;
    }

    void set_vector_type(TypeDesc *td){
        vector_type = td;
    }

///////////////////////////////////////
//  Implementation

public:
    TypeDesc(Kind, TypeDesc* component_type, TypeDesc* vector_type, const String* name, ClassLoader* cl, Class* c);
    virtual ~TypeDesc();

private:
    Kind k;
    TypeDesc* component_type; // refers to component_type of vector
    TypeDesc* vector_type; // refers to corresponding vector type
    const String* name;
    ClassLoader* loader;
    Class* clss;
};

// Loader Interface
TypeDesc* type_desc_create_from_java_class(Class* c);
TypeDesc* type_desc_create_from_java_descriptor(const char*, ClassLoader*);
TypeDesc* type_desc_create_from_class(Class* c);

#endif //! _TYPE_H_
