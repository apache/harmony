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
#ifndef _EXCEPTIONS_JIT_H_
#define _EXCEPTIONS_JIT_H_

#include "exceptions_type.h"
#include "jni.h"
#include "open/types.h"


//**** Main exception propogation entry points
void exn_throw_for_JIT(ManagedObject* exn_obj, Class_Handle exn_class,
    Method_Handle exn_constr, U_8* jit_exn_constr_args, jvalue* vm_exn_constr_args);

// Throw an exception in the current thread.
// Must be called with an M2nFrame on the top of the stack, and throws to the previous managed
// frames or the previous M2nFrame.
// If exn_obj is nonnull then it is the exception, otherwise the exception is an instance of
// exn_class created using the given constructor and arguments (a null exn_constr indicates the default constructor).
// Does not return.
void exn_athrow(ManagedObject* exn_obj, Class_Handle exn_class, Method_Handle exn_constr=NULL, U_8* exn_constr_args=NULL);

// Throw an exception in the current thread.
// Must be called with the current thread "suspended" in managed code and regs holds the suspended values.
// Exception defined as in previous two functions.
// Mutates the regs value, which should be used to "resume" the managed code.
void exn_athrow_regs(Registers* regs, Class_Handle exn_class, bool java_code, bool transfer_control=false);

// exception catch callback to restore stack after Stack Overflow Error
void exception_catch_callback();

// exception catch support for JVMTI
void jvmti_exception_catch_callback();

//**** Runtime exception support

// rth_throw_illegal_monitor_state throws an java.lang.IllegalMonitorStateException (lazily)
NativeCodePtr exn_get_rth_throw_illegal_monitor_state();

// rth_throw takes an exception and throws it
NativeCodePtr exn_get_rth_throw();

// rth_throw_lazy takes a constructor, the class for that constructor, and arguments for that constructor.
// it throws a (lazily created) instance of that class using that constructor and arguments.
NativeCodePtr exn_get_rth_throw_lazy();

// rth_throw_lazy_trampoline takes an exception class in the first standard place
// and throws a (lazily created) instance of that class using the default constructor
NativeCodePtr exn_get_rth_throw_lazy_trampoline();

// rth_throw_null_pointer throws a null pointer exception (lazily)
NativeCodePtr exn_get_rth_throw_null_pointer();

// rth_throw_array_index_out_of_bounds throws an array index out of bounds exception (lazily)
NativeCodePtr exn_get_rth_throw_array_index_out_of_bounds();

// rth_throw_negative_array_size throws a negative array size exception (lazily)
NativeCodePtr exn_get_rth_throw_negative_array_size();

// rth_throw_array_store throws an array store exception (lazily)
NativeCodePtr exn_get_rth_throw_array_store();

// rth_throw_arithmetic throws an arithmetic exception (lazily)
//NativeCodePtr exn_get_rth_throw_arithmetic();

// rth_throw_class_cast_exception throws a class cast exception (lazily)
NativeCodePtr exn_get_rth_throw_class_cast_exception();

// rth_throw_incompatible_class_change_exception throws an incompatible class change exception (lazily)
NativeCodePtr exn_get_rth_throw_incompatible_class_change_exception();

NativeCodePtr exn_get_rth_throw_illegal_state_exception();

//**** Various standard exception types

Class_Handle exn_get_class_cast_exception_type();

#endif // _EXCEPTIONS_JIT_H_
