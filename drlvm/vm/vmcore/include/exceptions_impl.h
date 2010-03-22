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
 * @author Intel, Pavel Afremov
 */
#ifndef _EXCEPTIONS_IMPL_H_
#define _EXCEPTIONS_IMPL_H_

#include "exceptions_type.h"
#include "jni.h"
#include "open/types.h"
#include "platform_lowlevel.h"

#define VM_LAZY_EXCEPTION

Class *get_exc_class(const char *exception_name);
Method* lookup_exc_constructor(Class * exc_class, const char *signature);
void init_cause(jthrowable exc_object, jthrowable exc_cause);

jthrowable create_exception(Class* exc_class, Method* exc_init, jvalue* args);
jthrowable create_exception(Class* exc_class,
    const char* exc_message, jthrowable exc_cause);
jthrowable create_exception(Exception* exception);

void exn_throw_object_internal(jthrowable exc_object);
void exn_throw_by_class_internal(Class* exc_class, const char* exc_message,
    jthrowable exc_cause);
void exn_throw_by_name_internal(const char* exc_name, const char* exc_message,
    jthrowable exc_cause);

void exn_raise_object_internal(jthrowable exc_object);
void exn_raise_by_class_internal(Class* exc_class, const char* exc_message,
    jthrowable exc_cause);
void exn_raise_by_name_internal(const char* exc_name, const char* exc_message,
    jthrowable exc_cause);

ManagedObject* __stdcall get_exception_object_internal();
void __stdcall set_exception_object_internal(ManagedObject * exn);
void __stdcall clear_exception_internal();

#endif // _EXCEPTIONS_IMPL_H_
