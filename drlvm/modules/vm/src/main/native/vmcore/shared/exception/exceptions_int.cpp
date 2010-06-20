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

#define LOG_DOMAIN "exn"
#include "clog.h"

#include "open/hythread_ext.h"
#include "exceptions.h"
#include "exceptions_impl.h"

//////////////////////////////////////////////////////////////////////////
// Interpreter support
ManagedObject * get_exception_for_interpreter()
{
    return get_exception_object_internal();
}//get_exception_for_interpreter

void set_exception_for_interpreter(ManagedObject * exn)
{
    assert(!is_unwindable());
    set_exception_object_internal(exn);
}   //set_exception_for_interpreter

void clear_exception_for_interpreter()
{
    // function should be only called from suspend disabled mode
    // it changes enumeratable reference to zero which is not
    // gc safe operation.
    assert(!hythread_is_suspend_enabled());
    clear_exception_internal();
}   //clear_current_thread_exception




VMEXPORT
ManagedObject* get_current_thread_exception() {
    return get_exception_for_interpreter();
}

VMEXPORT
void set_current_thread_exception(ManagedObject* obj) {
    set_exception_for_interpreter(obj);
}

VMEXPORT
void clear_current_thread_exception(){
    clear_exception_for_interpreter();
}

VMEXPORT
bool check_current_thread_exception(){
    return exn_raised();
}

