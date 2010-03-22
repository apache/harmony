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

#define LOG_DOMAIN "vm.core.types"
#include "cxxlog.h"

#include "type.h"
#include "Class.h"
#include "exceptions.h"
#include "open/vm_util.h"
#include "open/vm_type_access.h"

Type_Info_Handle type_info_create_from_java_descriptor(
    Class_Loader_Handle cl, const char* typeName) {
    return type_desc_create_from_java_descriptor(typeName, cl);
}

VM_Data_Type type_info_get_type(Type_Info_Handle tih)
{
    assert(tih);
    TypeDesc* td = (TypeDesc*)tih;
    switch (td->get_kind()) {
    case K_S1:               return VM_DATA_TYPE_INT8;
    case K_S2:               return VM_DATA_TYPE_INT16;
    case K_S4:               return VM_DATA_TYPE_INT32;
    case K_S8:               return VM_DATA_TYPE_INT64;
    case K_Sp:               return VM_DATA_TYPE_INTPTR;
    case K_U1:               return VM_DATA_TYPE_UINT8;
    case K_U2:               return VM_DATA_TYPE_UINT16;
    case K_U4:               return VM_DATA_TYPE_UINT32;
    case K_U8:               return VM_DATA_TYPE_UINT64;
    case K_Up:               return VM_DATA_TYPE_UINTPTR;
    case K_F4:               return VM_DATA_TYPE_F4;
    case K_F8:               return VM_DATA_TYPE_F8;
    case K_Char:             return VM_DATA_TYPE_CHAR;
    case K_Boolean:          return VM_DATA_TYPE_BOOLEAN;
    case K_Void:             return VM_DATA_TYPE_VOID;
    case K_Object:           return VM_DATA_TYPE_CLASS;
    case K_Vector:           return VM_DATA_TYPE_ARRAY;
    case K_UnboxedValue:     return VM_DATA_TYPE_VALUE;
    case K_UnmanagedPointer: return VM_DATA_TYPE_UP;
    case K_ManagedPointer:   return VM_DATA_TYPE_MP;
    // The rest are not implemented in the VM_Data_Type scheme
    case K_Array:
    case K_MethodPointer:
    case K_TypedRef:
    default:
        DIE(("Invalid vm data type"));
        return VM_DATA_TYPE_INVALID;
    }
} //type_info_get_type


BOOLEAN type_info_is_reference(Type_Info_Handle tih)
{
    TypeDesc* td = (TypeDesc*)tih;
    assert(td);
    return td->get_kind()==K_Object;
} //type_info_is_reference


BOOLEAN type_info_is_unboxed(Type_Info_Handle tih)
{
    TypeDesc* td = (TypeDesc*)tih;
    assert(td);
    return td->is_unboxed_value();
} //type_info_is_unboxed


BOOLEAN type_info_is_unmanaged_pointer(Type_Info_Handle tih)
{
    TypeDesc* td = (TypeDesc*)tih;
    assert(td);
    return td->is_unmanaged_pointer();
} //type_info_is_unmanaged_pointer


BOOLEAN type_info_is_void(Type_Info_Handle tih)
{
    TypeDesc* td = (TypeDesc*)tih;
    assert(td);
    return td->get_kind()==K_Void;
} //type_info_is_void


BOOLEAN type_info_is_method_pointer(Type_Info_Handle tih)
{
    TypeDesc* td = (TypeDesc*)tih;
    assert(td);
    return td->is_method_pointer();
} //type_info_is_method_pointer


BOOLEAN type_info_is_vector(Type_Info_Handle tih)
{
    TypeDesc* td = (TypeDesc*)tih;
    assert(td);
    return td->is_vector();
} //type_info_is_vector


BOOLEAN type_info_is_general_array(Type_Info_Handle tih)
{
    TypeDesc* td = (TypeDesc*)tih;
    assert(td);
    return td->is_array() && !td->is_vector();
} //type_info_is_general_array


BOOLEAN type_info_is_primitive(Type_Info_Handle tih)
{
    TypeDesc* td = (TypeDesc*)tih;
    assert(td);
    return td->is_primitive();
} //type_info_is_primitive


const char* type_info_get_type_name(Type_Info_Handle tih) {
    TypeDesc* td = (TypeDesc*)tih;
    assert(td);
    return td->get_type_name()->bytes;
}

Class_Handle type_info_get_class(Type_Info_Handle tih)
{
    TypeDesc* td = (TypeDesc*)tih;
    assert(td);
    Class* c = td->load_type_desc();
    if(!c) return NULL;
    if(!c->verify(VM_Global_State::loader_env)) return NULL;
    if(!c->prepare(VM_Global_State::loader_env)) return NULL;
    return c;
} //type_info_get_class

Class_Handle type_info_get_class_no_exn(Type_Info_Handle tih)
{
    // Store raised exception
    jthrowable exc_object = exn_get();
    // Workaround to let JIT invoke class loader even if exception is pending
    exn_clear();
    Class_Handle ch = type_info_get_class(tih);
    // To clear exn_class if set
    exn_clear();
    // Restore saved exception
    if (exc_object)
        exn_raise_object(exc_object);

    return ch;
} // type_info_get_class_no_exn

Method_Signature_Handle type_info_get_method_sig(Type_Info_Handle UNREF tih)
{
    LDIE(51, "Not implemented");
    return 0;
} //type_info_get_method_sig



Type_Info_Handle type_info_get_type_info(Type_Info_Handle tih)
{
    TypeDesc* td = (TypeDesc*)tih;
    assert(td);
    switch (td->get_kind()) {
    case K_Vector:
    case K_Array:
        return td->get_element_type();
    case K_ManagedPointer:
    case K_UnmanagedPointer:
        return td->get_pointed_to_type();
    default:
        LDIE(73, "Unexpected kind");
        return 0;
    }
} //type_info_get_type_info

static U_32 countLeadingChars(const char* str, char c) {
    U_32 n=0;
    while (str[n]==c) {
        n++;                
    }
    return n;
}


U_32 type_info_get_num_array_dimensions(Type_Info_Handle tih) {
    TypeDesc* td = (TypeDesc*)tih;
    if (td->get_kind() == K_Vector) {
        const String* name = td->get_type_name();
        U_32 res = 0;
        if (name == NULL) {
            res = 1 + type_info_get_num_array_dimensions(td->get_element_type());
        } else {
            res = countLeadingChars(name->bytes, '[');
        }
        assert(res<=255);
        return res;
    }
    return 0;
}

BOOLEAN type_info_is_resolved(Type_Info_Handle tih) {
    TypeDesc* td = (TypeDesc*)tih;
    switch (td->get_kind()) {
    case K_Vector:
        if (td->get_element_type()->is_primitive()) {
            return true;
        }
        return type_info_is_resolved(td->get_element_type());
    case K_Object:
        return td->is_loaded();
    default:
        LDIE(73, "Unexpected kind");
        return 0;
    }
}

BOOLEAN type_info_is_managed_pointer(Type_Info_Handle tih)
{
    assert(tih);
    TypeDesc* td = (TypeDesc*)tih;
    assert(td);
    return td->is_managed_pointer();
} //type_info_is_managed_pointer

