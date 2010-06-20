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

#include "open/vm_class_manipulation.h"
#include "vtable.h"
#include "Class.h"
#include "classloader.h"
#include "exceptions.h"
#include "exceptions_impl.h"
#include "exceptions_jit.h"
#include "exceptions_type.h"
#include "environment.h"
#include "heap.h"
#include "ini.h"
#include "vm_strings.h"

Class *get_exc_class(const char *exception_name)
{
    ASSERT_RAISE_AREA;
    assert(hythread_is_suspend_enabled());
    Global_Env *env = VM_Global_State::loader_env;
    String *exc_str = env->string_pool.lookup(exception_name);
    Class *exc_class =
        env->bootstrap_class_loader->LoadVerifyAndPrepareClass(env, exc_str);

    if (exc_class == NULL) {
        return NULL;
    }

    tmn_suspend_disable();
    class_initialize(exc_class);
    tmn_suspend_enable();

    if (exn_raised()) {
        return NULL;
    }

    return exc_class;
}

Method* lookup_exc_constructor(Class * exc_class, const char *signature)
{
    ASSERT_RAISE_AREA;
    Global_Env *env = VM_Global_State::loader_env;
    // Get the method for the constructor
    String *init_name = env->Init_String;
    String *init_descr = env->string_pool.lookup(signature);
    Method *exc_init = exc_class->lookup_method(init_name, init_descr);
    return exc_init;
}

//FIXME LAZY EXCEPTION (2006.05.13)
// internal declaration of functions should be moved to exception_impl.h
static Method* prepare_exc_creating(Class* exc_class, jvalue* args,
    const char* exc_message);
static Method* prepare_exc_creating(Class* exc_class, jvalue* args,
    const char* exc_message, jthrowable exc_cause);

//FIXME LAZY EXCEPTION (2006.05.13)
// cause can be null
static Method* prepare_exc_creating(Class* exc_class, jvalue* args) {
    ASSERT_RAISE_AREA;
    assert(hythread_is_suspend_enabled());

    // Finds corresponding constructor
    Method* exc_init = lookup_exc_constructor(exc_class, "()V");

    // Check that constructor is found
    if (NULL == exc_init) {
        return prepare_exc_creating(exc_class, args, "");
    }

    // Returns found constructor
    return exc_init;
}

static Method* prepare_exc_creating(Class* exc_class, jvalue* args,
    jthrowable exc_cause) {
    ASSERT_RAISE_AREA;
    assert(hythread_is_suspend_enabled());

    // Checks that it's corresponding method
    if (NULL == exc_cause) {
        return prepare_exc_creating(exc_class, args);
    }

    // Finds corresponding constructor
    Method* exc_init = lookup_exc_constructor(exc_class, "(Ljava/lang/Throwable;)V");

    // Check that constructor is found
    if (exc_init == NULL){
        return prepare_exc_creating(exc_class, args, "", exc_cause);
    }

    // Fills arguments for constructor
    args[1].l = exc_cause;

    // Returns found constructor
    return exc_init;
}

static Method* prepare_exc_creating(Class* exc_class, jvalue* args,
    const char* exc_message) {
    ASSERT_RAISE_AREA;
    assert(hythread_is_suspend_enabled());

    // Checks that it's corresponding method
    if (NULL == exc_message) {
        return prepare_exc_creating(exc_class, args);
    }

    // Finds corresponding constructor
    Method* exc_init = lookup_exc_constructor(exc_class, "(Ljava/lang/String;)V");

    // Check that constructor is found
    if (NULL == exc_init){
        return NULL;
    }

    // Creates string object
    tmn_suspend_disable();

    ManagedObject *arg_obj =
        string_create_from_utf8(exc_message, (unsigned) strlen(exc_message));

    if (!arg_obj) {
        exn_raise_object(VM_Global_State::loader_env->java_lang_OutOfMemoryError);
        tmn_suspend_enable();
        return NULL;
    }
    jobject arg = oh_allocate_local_handle();
    arg->object = arg_obj;

    tmn_suspend_enable();

    // Fills arguments for constructor
    args[1].l = arg;

    // Returns found constructor
    return exc_init;
}

static Method* prepare_exc_creating(Class* exc_class, jvalue* args,
    const char* exc_message, jthrowable exc_cause) {
    ASSERT_RAISE_AREA;
    assert(hythread_is_suspend_enabled());

    // Checks that it's corresponding method
    if (NULL == exc_message) {
        return prepare_exc_creating(exc_class, args, exc_cause);
    }

    // Checks that it's corresponding method
    if (NULL == exc_cause) {
        return prepare_exc_creating(exc_class, args, exc_message);
    }

    // Finds corresponding constructor
    Method* exc_init = lookup_exc_constructor(exc_class, "(Ljava/lang/String;Ljava/lang/Throwable;)V");

    // Check that constructor is found
    if (NULL == exc_init){
        return NULL;
    }

    // Creates string object
    tmn_suspend_disable_recursive();

    ManagedObject *arg_obj =
        string_create_from_utf8(exc_message, (unsigned) strlen(exc_message));

    if (!arg_obj) {
        exn_raise_object(VM_Global_State::loader_env->java_lang_OutOfMemoryError);
        return NULL;
    }
    jobject arg = oh_allocate_local_handle();
    arg->object = arg_obj;

    tmn_suspend_enable_recursive();

    // Fills arguments for constructor
    args[1].l = arg;
    args[2].l = exc_cause;

    // Returns found constructor
    return exc_init;
}

void init_cause(jthrowable exc_object, jthrowable exc_cause) {
    ASSERT_RAISE_AREA;
    assert(exc_cause);
    assert(hythread_is_suspend_enabled());

    tmn_suspend_disable();

    Class* exc_class = exc_object->object->vt()->clss;
    Method *init_cause_method = class_lookup_method_recursive(exc_class,
        "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;");
    assert(init_cause_method);
    jvalue args[2];
    args[0].l = exc_object;
    args[1].l = exc_cause;
    jvalue ret_val;
    vm_execute_java_method_array((jmethodID) init_cause_method, &ret_val,
        args);
    tmn_suspend_enable();
}

jthrowable create_exception(Class* exc_class, Method* exc_init, jvalue* args) {
    ASSERT_RAISE_AREA;
    assert(hythread_is_suspend_enabled());

    bool suspended_enabled = hythread_is_suspend_enabled();

    if (suspended_enabled) {
        tmn_suspend_disable();
    }

    ManagedObject *man_obj = class_alloc_new_object(exc_class);

    if (!man_obj) {
        exn_raise_object(VM_Global_State::loader_env->java_lang_OutOfMemoryError);
        if (suspended_enabled) {
            tmn_suspend_enable();
        }
        return NULL;
    }

    jthrowable exc_object = oh_allocate_local_handle();
    exc_object->object = man_obj;
    args[0].l = exc_object;

    if (exn_raised()) { //if RuntimeException or Error
        if (suspended_enabled) {
            tmn_suspend_enable();
        }
        return NULL;
    }

    vm_execute_java_method_array((jmethodID) exc_init, 0, args);

    if (suspended_enabled) {
        tmn_suspend_enable();
    }

    return exc_object;
}

jthrowable create_exception(Class* exc_class,
    const char* exc_message, jthrowable exc_cause)
{
    ASSERT_RAISE_AREA;
    assert(hythread_is_suspend_enabled());
    jvalue args[3];
    Method *exc_init =
        prepare_exc_creating(exc_class, args, exc_message, exc_cause);

    if (exc_init == NULL){
        return NULL;
    }

    return create_exception(exc_class, exc_init, args);
} // create_exception(Class *exc_class, const char *exc_message, jthrowable exc_cause)

jthrowable create_exception(Exception* exception)
{
    ASSERT_RAISE_AREA;
    assert(hythread_is_suspend_enabled());

    if ( NULL != exception->exc_class) {
        jthrowable exc_cause = NULL;
        Class* exc_class = exception->exc_class;
        const char* exc_message = exception->exc_message;

        if (NULL != exception->exc_cause) {
            tmn_suspend_disable_recursive();
            exc_cause = oh_allocate_local_handle();
            exc_cause->object = exception->exc_cause;
            tmn_suspend_enable_recursive();
        }
        exn_clear();
        
        jthrowable exc_exception = NULL;
        exc_exception = exn_create(exc_class, exc_message, exc_cause);
        return exc_exception;
    } else {
        return NULL;
    }
}

void exn_throw_object_internal(jthrowable exc_object)
{
    BEGIN_RAISE_AREA;
    // functions can be invoked in suspend disabled and enabled state
    if (hythread_is_suspend_enabled()) {
        tmn_suspend_disable();
    }
    assert(!hythread_is_suspend_enabled());
    CTRACE(("%s", "exn_throw_object(), delegating to exn_throw_for_JIT()"));
    exn_throw_for_JIT(exc_object->object, NULL, NULL, NULL, NULL);
    END_RAISE_AREA;
}

void exn_throw_by_class_internal(Class* exc_class, const char* exc_message,
    jthrowable exc_cause)
{
    BEGIN_RAISE_AREA;
    // functions can be invoked in suspend disabled and enabled state
    if (!hythread_is_suspend_enabled()) {
        // exception is throwing, so suspend can be enabled safely
        tmn_suspend_enable();
    }
    assert(hythread_is_suspend_enabled());
#ifdef VM_LAZY_EXCEPTION
    //set_unwindable(false);

    jvalue args[3];
    Method* exc_init = prepare_exc_creating(
            exc_class, args, exc_message, exc_cause);

    if (NULL == exc_init) {
        CTRACE(("%s",
            "exn_throw_by_class(),create exception and delegating to exn_throw_for_JIT()"));
        jthrowable exc_object = exn_create(exc_class, exc_message, exc_cause);
        exn_rethrow_if_pending();
        //set_unwindable(true);
        exn_throw_object_internal(exc_object);
    } else {
        CTRACE(("%s", "exn_throw_by_class(), lazy delegating to exn_throw_for_JIT()"));
        //set_unwindable(true);

        // no return, so enable isn't required
        tmn_suspend_disable();
        exn_throw_for_JIT(NULL, exc_class, exc_init, NULL, args);
        //tmn_suspend_enable();
    }
#else
    jthrowable exc_object = exn_create(exc_class, exc_message, exc_cause);
    exn_rethrow_if_pending();
    exn_throw_object_internal(exc_object);
#endif
    END_RAISE_AREA;
}

void exn_throw_by_name_internal(const char* exc_name, const char* exc_message,
    jthrowable exc_cause)
{
    BEGIN_RAISE_AREA;
    // functions can be invoked in suspend disabled and enabled state
    if (!hythread_is_suspend_enabled()) {
        // exception is throwing, so suspend can be enabled safely
        tmn_suspend_enable();
    }
    assert(hythread_is_suspend_enabled());
    Class *exc_class = get_exc_class(exc_name);

    if (exc_class == NULL) {
        assert(exn_raised());
        exn_rethrow();
        return; // unreachable code
    }
    exn_throw_by_class_internal(exc_class, exc_message, exc_cause);
    END_RAISE_AREA;
}

void exn_raise_object_internal(jthrowable exc_object)
{
    CTRACE(("%s", "exn_raise_object(), propagating non-destructively"));

    tmn_suspend_disable_recursive();
    p_TLS_vmthread->thread_exception.exc_object = exc_object->object;
    tmn_suspend_enable_recursive();
}

void exn_raise_by_class_internal(Class* exc_class, const char* exc_message,
    jthrowable exc_cause)
{
#ifdef VM_LAZY_EXCEPTION
    CTRACE(("%s", "exn_raise_object(), propagating lazy & non-destructively"));

    tmn_suspend_disable_recursive();
    p_TLS_vmthread->thread_exception.exc_class = exc_class;
    p_TLS_vmthread->thread_exception.exc_message = exc_message;

    if (exc_cause != NULL) {
        p_TLS_vmthread->thread_exception.exc_cause = exc_cause->object;
    } else {
        p_TLS_vmthread->thread_exception.exc_cause = NULL;
    }
    tmn_suspend_enable_recursive();
#else
    assert(hythread_is_suspend_enabled());
    jthrowable exc_object = exn_create(exc_class, exc_message, exc_cause);

    if (exn_raised()){
        return;
    }
    exn_raise_object_internal(exc_object);
#endif
}

void exn_raise_by_name_internal(const char* exc_name, const char* exc_message,
    jthrowable exc_cause)
{
    assert(hythread_is_suspend_enabled());
    Class *exc_class = get_exc_class(exc_name);

    if (exc_class == NULL) {
        assert(exn_raised());
        return;
    }
    exn_raise_by_class_internal(exc_class, exc_message, exc_cause);
}

// function should be called in disable mode
void __stdcall clear_exception_internal()
{
    assert(!hythread_is_suspend_enabled());
    p_TLS_vmthread->thread_exception.exc_object = NULL;
    p_TLS_vmthread->thread_exception.exc_class = NULL;
    p_TLS_vmthread->thread_exception.exc_cause = NULL;
    p_TLS_vmthread->thread_exception.exc_message = NULL;
} // clear_exception_internal

// function should be called in disable mode
void __stdcall set_exception_object_internal(ManagedObject * exc)
{
    assert(!hythread_is_suspend_enabled());
    p_TLS_vmthread->thread_exception.exc_object = exc;
} // set_exc_object_internal

// function is safe point & should be called in disable mode in safe enviroment
ManagedObject* __stdcall get_exception_object_internal()
{
    assert(!hythread_is_suspend_enabled());
    if (NULL != p_TLS_vmthread->thread_exception.exc_object) {
        return p_TLS_vmthread->thread_exception.exc_object;
    } else if (NULL != p_TLS_vmthread->thread_exception.exc_class) {
        Exception* exception = (Exception*)&(p_TLS_vmthread->thread_exception);

        // suspend can be enabeled in safe enviroment
        tmn_suspend_enable();
        jthrowable exc_object = create_exception(exception);
        tmn_suspend_disable();

        return exc_object->object;
    } else {
        return NULL;
    }
} // get_exc_object_internal

