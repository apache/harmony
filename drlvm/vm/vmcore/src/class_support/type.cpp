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

#include <assert.h>
//#include <stdlib.h>

#define LOG_DOMAIN "vm.core"
#include "cxxlog.h"

#include "open/types.h"
#include "open/vm_class_manipulation.h"

#include "Class.h"
#include "environment.h"
#include "lock_manager.h"
#include "type.h"
#include "vm_core_types.h"

Kind TypeDesc::get_kind()
{
    return k;
}

bool TypeDesc::is_unboxed_value()
{
    switch (get_kind()) {
    case K_S1:
    case K_S2:
    case K_S4:
    case K_S8:
    case K_Sp:
    case K_U1:
    case K_U2:
    case K_U4:
    case K_U8:
    case K_Up:
    case K_F4:
    case K_F8:
    case K_Boolean:
    case K_Char:
    case K_UnboxedValue:
        return true;
    default:
        return false;
    }
}

bool TypeDesc::is_primitive()
{
    switch (get_kind()) {
    case K_S1:
    case K_S2:
    case K_S4:
    case K_S8:
    case K_Sp:
    case K_U1:
    case K_U2:
    case K_U4:
    case K_U8:
    case K_Up:
    case K_F4:
    case K_F8:
    case K_Boolean:
    case K_Char:
    case K_Void:
        return true;
    default:
        return false;
    }
}

bool TypeDesc::is_array()
{
    Kind k = get_kind();
    return k==K_Vector || k==K_Array;
}

bool TypeDesc::is_vector()
{
    return get_kind()==K_Vector;
}

TypeDesc* TypeDesc::get_element_type()
{
    return component_type;
}

bool TypeDesc::is_managed_pointer()
{
    return get_kind()==K_ManagedPointer;
}

bool TypeDesc::is_unmanaged_pointer()
{
    return get_kind()==K_UnmanagedPointer;
}

TypeDesc* TypeDesc::get_pointed_to_type()
{
    return component_type;
}

bool TypeDesc::is_method_pointer()
{
    return get_kind()==K_MethodPointer;
}

TypeDesc::TypeDesc(Kind _k, TypeDesc* ctd, TypeDesc* vtd, const String* _name, ClassLoader* _cl, Class* c)
: k(_k), component_type(ctd), vector_type(vtd), name(_name), loader(_cl), clss(c)
{
}

TypeDesc::~TypeDesc()
{
    if (vector_type)
    {
        delete vector_type;
        vector_type = NULL;
    }
}

TypeDesc* type_desc_create_from_class(Class* c)
{
    TypeDesc* td;

    if (c->is_primitive()) {
        Kind k = K_UnmanagedPointer;
        switch (class_get_primitive_type_of_class(c)) {
        case VM_DATA_TYPE_INT8:    k = K_S1;      break;
        case VM_DATA_TYPE_UINT8:   k = K_U1;      break;
        case VM_DATA_TYPE_INT16:   k = K_S2;      break;
        case VM_DATA_TYPE_UINT16:  k = K_U2;      break;
        case VM_DATA_TYPE_INT32:   k = K_S4;      break;
        case VM_DATA_TYPE_UINT32:  k = K_U4;      break;
        case VM_DATA_TYPE_INT64:   k = K_S8;      break;
        case VM_DATA_TYPE_UINT64:  k = K_U8;      break;
        case VM_DATA_TYPE_INTPTR:  k = K_Sp;      break;
        case VM_DATA_TYPE_UINTPTR: k = K_Up;      break;
        case VM_DATA_TYPE_F4:      k = K_F4;      break;
        case VM_DATA_TYPE_F8:      k = K_F8;      break;
        case VM_DATA_TYPE_BOOLEAN: k = K_Boolean; break;
        case VM_DATA_TYPE_CHAR:    k = K_Char;    break;
        case VM_DATA_TYPE_VOID:    k = K_Void;    break;
        default:
            LDIE(52, "Unexpected data type");
        }
        td = new TypeDesc(k, NULL, NULL, NULL, c->get_class_loader(), c);
    } else if (c->is_array()) {
        td = new TypeDesc(K_Vector, NULL, NULL, NULL, c->get_class_loader(), c);
    } else {
        Kind k = K_Object;
        td = new TypeDesc(k, NULL, NULL, NULL, c->get_class_loader(), c);
    }
    assert(td);
    return td;
}


