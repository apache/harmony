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
#ifndef _EXCEPTIONS_H_
#define _EXCEPTIONS_H_

#include "open/types.h"
#include "exceptions_type.h"
#include "jni.h"

/**
@file
\ref exceptions
*/

/**
@page exceptions Exceptions subsystem

\section exn_introduction Introduction
The functions to work with exceptions are described in exceptions.h.

\section exn_issues Issues

\li Interaction with JIT and runtime helpers? -salikh
\li Interaction with JIT is implemented via rth_wrap_exn_throw stubs. -pavel.n.afremov
\li Existing interface is currently included.

*/

/**
 * Returns the thread-local exception object
 * or NULL if no exception occured.
 */
VMEXPORT jthrowable exn_get();

VMEXPORT Class* exn_get_class();
VMEXPORT const char* exn_get_name();


/**
 * Returns true if the thread-local exception object is set.
 */
VMEXPORT bool exn_raised();

/**
 * Clears the thread-local exception object.
 *
 * @note rename of clear_current_thread_exception(). It may be eliminated if
 * exn_catch() will be used and will clean thread exception.
 */
VMEXPORT void exn_clear();

/**
 * Creates exception object.
 */
//FIXME LAZY EXCEPTION (2006.05.06)
// Maybe change to exn_object_create (whole 7)
jthrowable exn_create(Exception* exception);

jthrowable exn_create(Class* exc_class);
jthrowable exn_create(Class* exc_class, jthrowable cause);
jthrowable exn_create(Class* exc_class, const char* message);
jthrowable exn_create(Class* exc_class, const char* message, jthrowable cause);

/**
 * Creates exception object.
 */
VMEXPORT jthrowable exn_create(const char* exc_name);

/**
 * Creates exception object.
 */
VMEXPORT jthrowable exn_create(const char* exc_name, jthrowable cause);

/**
 * Creates exception object.
 */
VMEXPORT jthrowable exn_create(const char* exc_name, const char* message);

/**
 * Creates exception object.
 */
VMEXPORT jthrowable exn_create(const char* exc_name, const char* message, jthrowable cause);

/**
 * Returns true if frame is unwindable and false if isn't. 
 */
VMEXPORT bool is_unwindable();

/**
 * Sets unwindable property of frame. If frame is unwindable property
 * should be true and should be false if frame isn't unwindable.
 * Returns previous value of unwindable property.
 */
bool set_unwindable(bool unwindable);

/**
 * Throws an exception object
 *
 * @note internal convenience function, may not be exposed to VMI interface.
 */
void exn_throw_object(jthrowable exc_object);

/**
 * Throws an exceptionas lazy.
 * Does not return in case of destructive exception propagation.
 *
 * @note internal convenience function, may not be exposed to VMI interface.
 */
void exn_throw_by_class(Class* exc_class);
void exn_throw_by_class(Class* exc_class, jthrowable exc_cause);
void exn_throw_by_class(Class* exc_class, const char* exc_message);
void exn_throw_by_class(Class* exc_class, const char* exc_message,
    jthrowable exc_cause);

/**
 * Throws an exceptionas lazy.
 * Does not return in case of destructive exception propagation.
 */
VMEXPORT void exn_throw_by_name(const char* exception_name);

/**
 * Throws an exceptionas lazy.
 * Does not return in case of destructive exception propagation.
 */
VMEXPORT void exn_throw_by_name(const char* exception_name, jthrowable cause);

/**
 * Throws an exceptionas lazy.
 * Does not return in case of destructive exception propagation.
 */
VMEXPORT void exn_throw_by_name(const char* exception_name, const char* message);

/**
 * Throws an exceptionas lazy.
 * Does not return in case of destructive exception propagation.
 */
VMEXPORT void exn_throw_by_name(const char* exception_name, const char* message, jthrowable cause);

/**
 * Sets exceptions as a thread local exception.
 *
 * @note explicit non-destructive semantics should be deduced from context.
 */
VMEXPORT void exn_raise_object(jthrowable exc_object);

/**
 * Sets exception lazy as a thread local exception.
 *
 * @note internal convenience function, may not be exposed to VMI interface.
 * @note explicit non-destructive semantics should be deduced from context.
 */
void exn_raise_by_class(Class* exc_class);
void exn_raise_by_class(Class* exc_class, jthrowable exc_cause);
void exn_raise_by_class(Class* exc_class, const char* exc_message);
void exn_raise_by_class(Class* exc_class, const char* exc_message,
    jthrowable exc_cause);

/**
 * Sets exception lazy as a thread local exception.
 *
 * @note explicit non-destructive semantics should be deduced from context.
 */
VMEXPORT void exn_raise_by_name(const char* exception_name);

/**
 * Sets exception lazy as a thread local exception.
 *
 * @note explicit non-destructive semantics should be deduced from context.
 */
VMEXPORT void exn_raise_by_name(const char* exception_name, jthrowable exc_cause);

/**
 * Sets exception lazy as a thread local exception.
 *
 * @note explicit non-destructive semantics should be deduced from context.
 */
VMEXPORT void exn_raise_by_name(const char* exception_name, const char* message);

/**
 * Sets exception lazy as a thread local exception.
 *
 * @note explicit non-destructive semantics should be deduced from context.
 */
VMEXPORT void exn_raise_by_name(const char* exception_name, const char* message,
    jthrowable exc_cause);

/**
 * Pushes dummy non-unwindable stack frame in order to prevent stack unwinding.
 * After this returns true. If unwinding is happnened control coming back into
 * this function, and after this it returns false.
 *
 * @note experimental
 */
bool exn_function_try();

/**
 * pops dummy non-unwindable stack frame
 *
 * returns the current thread exception object
 * or NULL if no exception occured.
 *
 * @note experimental
 */
jthrowable exn_function_catch();

/**
 * Wrapper for exn_function_try.
 */
#define exn_try (if (exn_function_try()))

/**
 * Wrapper for exn_function_catch.
 */
#define exn_catch (th) (if ( th = exn_function_catch()))

#define ASSERT_THROW_AREA \
assert(is_unwindable());

#define ASSERT_RAISE_AREA \
assert(!is_unwindable());

#define BEGIN_RAISE_AREA \
{ \
bool unwindable = set_unwindable(false);\
if (unwindable) exn_rethrow_if_pending();

#define END_RAISE_AREA \
if (unwindable) exn_rethrow_if_pending();\
set_unwindable(unwindable);\
}


////////////////////////////////////////////////////////////////////////
// FUNCTIONS below are from old VM implementation //////////////////////
////////////////////////////////////////////////////////////////////////

#include "open/vm_util.h"

struct ManagedObject;

//**** Stack Trace support

// Print the stack trace stored in the exception object to the given file.
void exn_print_stack_trace(FILE* f, jthrowable exc);

void print_uncaught_exception_message(FILE *f, const char* context_message, jthrowable exc);


//**** Native code exception support

void exn_rethrow();
void exn_rethrow_if_pending();

typedef struct VM_thread * vm_thread_t;
void* get_exception_catch_stack_addr(void* curr_ip);
VMEXPORT size_t get_available_stack_size();
VMEXPORT bool check_available_stack_size(size_t required_size);
bool check_stack_size_enough_for_exception_catch(void* sp);

#endif // _EXCEPTIONS_H_
