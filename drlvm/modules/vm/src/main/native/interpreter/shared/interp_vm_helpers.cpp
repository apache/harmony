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
#include "open/vm_class_manipulation.h"
#include "open/vm_ee.h"

#include "interp_vm_helpers.h"
#include "interpreter_imports.h"
#include "interp_native.h"
#include "exceptions.h"
#include "compile.h"
#include "interp_defs.h"

void interp_throw_exception(const char* exc_name) {
    M2N_ALLOC_MACRO;
    assert(!hythread_is_suspend_enabled());
    hythread_suspend_enable();
    assert(hythread_is_suspend_enabled());
    jthrowable exc_object = exn_create(exc_name);
    exn_raise_object(exc_object);
    hythread_suspend_disable();
    M2N_FREE_MACRO;
}

void interp_throw_exception(const char* exc_name, const char* exc_message) {
    M2N_ALLOC_MACRO;
    assert(!hythread_is_suspend_enabled());
    hythread_suspend_enable();
    assert(hythread_is_suspend_enabled());
    jthrowable exc_object = exn_create(exc_name, exc_message);
    exn_raise_object(exc_object);
    hythread_suspend_disable();
    M2N_FREE_MACRO;
}


GenericFunctionPointer interp_find_native(Method_Handle method)
{
    hythread_suspend_enable();
    GenericFunctionPointer f = classloader_find_native((Method_Handle) method);
    hythread_suspend_disable();
    return f;
}


Class* interp_resolve_class(Class *clazz, int classId) {
    assert(!hythread_is_suspend_enabled());
    Compilation_Handle handle;
    handle.env = VM_Global_State::loader_env;
    handle.jit = 0;

    hythread_suspend_enable();
    Class *objClass = resolve_class((Compile_Handle*)&handle, clazz, classId); 
    hythread_suspend_disable();
    if (!objClass) {
        if (!exn_raised())
            class_throw_linking_error_for_interpreter(clazz, classId, 0);
    }

    return objClass;
}

Class* interp_resolve_class_new(Class *clazz, int classId) {
    assert(!hythread_is_suspend_enabled());
    Compilation_Handle handle;
    handle.env = VM_Global_State::loader_env;
    handle.jit = 0;

    hythread_suspend_enable();
    Class *objClass = resolve_class_new((Compile_Handle*)&handle, clazz, classId); 
    hythread_suspend_disable();
    if (!objClass) {
        if (!exn_raised())
            class_throw_linking_error_for_interpreter(clazz, classId, OPCODE_NEW);
    }

    return objClass;
}


Field* interp_resolve_static_field(Class *clazz, int fieldId, bool putfield) {
    assert(!hythread_is_suspend_enabled());
    Compilation_Handle handle;
    handle.env = VM_Global_State::loader_env;
    handle.jit = 0;

    hythread_suspend_enable();
    Field *field =
        resolve_static_field((Compile_Handle*)&handle, clazz, fieldId, putfield);
    hythread_suspend_disable();
    if (!field) {
        if (!exn_raised())
            class_throw_linking_error_for_interpreter(clazz, fieldId,
                    putfield?OPCODE_PUTSTATIC:OPCODE_GETSTATIC);
    }
    return field;
}

Field* interp_resolve_nonstatic_field(Class *clazz, int fieldId, bool putfield) {
    assert(!hythread_is_suspend_enabled());
    Compilation_Handle handle;
    handle.env = VM_Global_State::loader_env;
    handle.jit = 0;

    hythread_suspend_enable();
    Field *field = resolve_nonstatic_field(
            (Compile_Handle*)&handle, clazz, fieldId, putfield);
    hythread_suspend_disable();
    if (!field) {
        if (!exn_raised())
            class_throw_linking_error_for_interpreter(clazz, fieldId,
                    putfield?OPCODE_PUTFIELD:OPCODE_GETFIELD);
    }
    return field;
}


Method* interp_resolve_virtual_method(Class* clazz, int methodId) {
    assert(!hythread_is_suspend_enabled());
    Compilation_Handle handle;
    handle.env = VM_Global_State::loader_env;
    handle.jit = 0;

    hythread_suspend_enable();
    assert(hythread_is_suspend_enabled());
    Method *method = resolve_virtual_method(
            (Compile_Handle*)&handle, clazz, methodId);
    hythread_suspend_disable();
    if (!method) {
        if (!exn_raised())
            class_throw_linking_error_for_interpreter(clazz, methodId, OPCODE_INVOKEVIRTUAL);
    }
    return method;
}

Method* interp_resolve_interface_method(Class* clazz, int methodId) {
    assert(!hythread_is_suspend_enabled());
    Compilation_Handle handle;
    handle.env = VM_Global_State::loader_env;
    handle.jit = 0;

    hythread_suspend_enable();
    assert(hythread_is_suspend_enabled());
    Method *method = resolve_interface_method(
            (Compile_Handle*)&handle, clazz, methodId);
    hythread_suspend_disable();
    if (!method) {
        if (!exn_raised())
            class_throw_linking_error_for_interpreter(clazz, methodId, OPCODE_INVOKEINTERFACE);
    }
    return method;
}

Method *interp_resolve_static_method(Class *clazz, int methodId) {
    assert(!hythread_is_suspend_enabled());
    Compilation_Handle handle;
    handle.env = VM_Global_State::loader_env;
    handle.jit = 0;

    hythread_suspend_enable();
    Method *method = resolve_static_method(
            (Compile_Handle*)&handle, clazz, methodId);
    hythread_suspend_disable();
    if (!method) {
        if (!exn_raised())
            class_throw_linking_error_for_interpreter(clazz, methodId, OPCODE_INVOKESTATIC);
    }
    return method;
}

Method *interp_resolve_special_method(Class *clazz, int methodId) {
    assert(!hythread_is_suspend_enabled());
    Compilation_Handle handle;
    handle.env = VM_Global_State::loader_env;
    handle.jit = 0;

    hythread_suspend_enable();
    Method *method = resolve_special_method(
            (Compile_Handle*)&handle, clazz, methodId);
    hythread_suspend_disable();
    if (!method) {
        if (!exn_raised())
            class_throw_linking_error_for_interpreter(clazz, methodId, OPCODE_INVOKESPECIAL);
    }
    return method;
}

Class* interp_class_get_array_of_class(Class *objClass) {
    hythread_suspend_enable();
    Class *clazz = class_get_array_of_class(objClass);
    hythread_suspend_disable();
    return clazz;
}
