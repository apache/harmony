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
#include "open/vm_method_access.h"
#include "open/vm_field_access.h"
#include "open/vm_class_manipulation.h"
#include "open/vm_class_loading.h"
#include "open/vm_util.h" // for VM_Global_State ???!!!
#include "Class.h"
#include "class_member.h"
#include "classloader.h"

unsigned short class_cp_get_size(Class_Handle klass)
{
    assert(klass);
    return klass->get_constant_pool().get_size();
} // class_cp_get_size

unsigned char class_cp_get_tag(Class_Handle klass, unsigned short index)
{
    assert(klass);
    return klass->get_constant_pool().get_tag(index);
} // class_cp_get_tag

const char* class_cp_get_utf8_bytes(Class_Handle klass, unsigned short index)
{
    assert(klass);
    return klass->get_constant_pool().get_utf8_chars(index);
} // class_cp_get_utf8_bytes

unsigned short class_cp_get_class_name_index(Class_Handle klass, unsigned short index)
{
    assert(klass);
    return klass->get_constant_pool().get_class_name_index(index);
} // class_cp_get_class_name_index

unsigned short class_cp_get_descriptor_index(Class_Handle klass, unsigned short index)
{
    assert(klass);
    return klass->get_constant_pool().get_name_and_type_descriptor_index(index);
} // class_cp_get_descriptor_index

unsigned short class_cp_get_ref_name_and_type_index(Class_Handle klass, unsigned short index)
{
    assert(klass);
    return klass->get_constant_pool().get_ref_name_and_type_index(index);
} // class_cp_get_ref_name_and_type_index

unsigned short class_cp_get_ref_class_index(Class_Handle klass, unsigned short index)
{
    assert(klass);
    return klass->get_constant_pool().get_ref_class_index(index);
} // class_cp_get_ref_class_index

unsigned short class_cp_get_name_index(Class_Handle klass, unsigned short index)
{
    assert(klass);
    return klass->get_constant_pool().get_name_and_type_name_index(index);
} // class_cp_get_name_index

Method_Handle class_resolve_method(Class_Handle klass, unsigned short index)
{
    assert(klass);
    assert(index);
    return klass->_resolve_method(VM_Global_State::loader_env, index);
} // class_resolve_method

BOOLEAN class_is_same_package(Class_Handle klass1, Class_Handle klass2)
{
    assert(klass1);
    assert(klass2);
    return (klass1->get_package() == klass2->get_package()) ? TRUE : FALSE;
} // class_is_same_package

unsigned short class_get_method_number(Class_Handle klass)
{
    assert(klass);
    return klass->get_number_of_methods();
} // class_get_method_number

Method_Handle class_get_method(Class_Handle klass, unsigned short index)
{
    assert(klass);
    if(index >= klass->get_number_of_methods())
    {
        assert(index < klass->get_number_of_methods());
        return NULL;
    }
    return klass->get_method(index);
} // class_get_method_number

unsigned char* method_get_stackmaptable(Method_Handle hmethod)
{
    assert(hmethod);
    return hmethod->get_stackmap();
} // method_get_stackmaptable

BOOLEAN method_is_protected(Method_Handle hmethod)
{
    assert(hmethod);
    Method *method = (Method*)hmethod;
    return hmethod->is_protected();
} // method_is_protected

BOOLEAN field_is_protected(Field_Handle hfield)
{
    assert(hfield);
    return hfield->is_protected();
} // field_is_protected

void class_loader_set_verifier_data_ptr(Class_Loader_Handle classloader, void* data)
{
    assert(classloader);
    classloader->SetVerifyData(data);
} // class_loader_set_verifier_data_ptr

void* class_loader_get_verifier_data_ptr(Class_Loader_Handle classloader)
{
    assert(classloader);
    return classloader->GetVerifyData();
} // class_loader_get_verifier_data_ptr

void class_loader_lock(Class_Loader_Handle classloader)
{
    assert(classloader);
    classloader->Lock();
} // class_loader_lock

void class_loader_unlock(Class_Loader_Handle classloader)
{
    assert(classloader);
    classloader->Unlock();
} // class_loader_unlock

Class_Handle class_loader_lookup_class(Class_Loader_Handle classloader, const char* name)
{
    assert(classloader);
    assert(name);
    Global_Env *env = VM_Global_State::loader_env;
    String* class_name = env->string_pool.lookup( name );
    return classloader->LookupClass(class_name);
} // class_loader_lookup_class

Class_Handle class_loader_load_class(Class_Loader_Handle classloader, const char* name)
{
    assert(classloader);
    assert(name);
    Global_Env *env = VM_Global_State::loader_env;
    String* class_name = env->string_pool.lookup(name);
    return classloader->LoadClass(env, class_name);
} // class_loader_load_class

