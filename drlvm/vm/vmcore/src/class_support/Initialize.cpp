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

#define LOG_DOMAIN "classloader"
#include "cxxlog.h"

#include "open/vm_class_manipulation.h"
#include "vtable.h"
#include "Class.h"
#include "jthread.h"
#include "exceptions.h"
#include "thread_manager.h"
#include "vm_strings.h"
#include "classloader.h"
#include "ini.h"
#include "vm_threads.h"

// Initializes a class.

void Class::initialize()
{
    ASSERT_RAISE_AREA;
    assert(!exn_raised());
    assert(!hythread_is_suspend_enabled());

    // the following code implements the 11-step class initialization program
    // described in page 226, section 12.4.2 of Java Language Spec, 1996
    // ISBN 0-201-63451-1

    TRACE2("class.init", "initializing class " << m_name->bytes);

    // ---  step 1   ----------------------------------------------------------

    assert(!hythread_is_suspend_enabled());
    jobject jlc = struct_Class_to_java_lang_Class_Handle(this);
    jthread_monitor_enter(jlc);

    // ---  step 2   ----------------------------------------------------------
    TRACE2("class.init", "initializing class " << m_name->bytes << " STEP 2" );

    while(m_initializing_thread != p_TLS_vmthread && is_initializing()) {
        jthread_monitor_wait(jlc);
        if(exn_raised()) {
            jthread_monitor_exit(jlc);
            return;
        }
    }

    // ---  step 3   ----------------------------------------------------------
    if(m_initializing_thread == p_TLS_vmthread) {
        jthread_monitor_exit(jlc);
        return;
    }

    // ---  step 4   ----------------------------------------------------------
    if(is_initialized()) {
        jthread_monitor_exit(jlc);
        return;
    }

    // ---  step 5   ----------------------------------------------------------
    if(in_error()) {
        jthread_monitor_exit(jlc);
        tmn_suspend_enable();
        exn_raise_by_name("java/lang/NoClassDefFoundError", m_name->bytes);
        tmn_suspend_disable();
        return;
    }

    // ---  step 6   ----------------------------------------------------------
    TRACE2("class.init", "initializing class " << m_name->bytes << "STEP 6" );

    assert(m_state == ST_ConstraintsVerified);
    assert(m_initializing_thread == 0);
    lock();
    m_state = ST_Initializing;
    unlock();
    m_initializing_thread = p_TLS_vmthread;
    jthread_monitor_exit(jlc);

    // ---  step 7 ------------------------------------------------------------

    if(has_super_class()) {
        class_initialize(get_super_class());

        if(exn_raised()) { 
            jthread_monitor_enter(jlc);
            m_initializing_thread = NULL;
            lock();
            m_state = ST_Error;
            unlock();
            assert(!hythread_is_suspend_enabled());
            jthread_monitor_notify_all(jlc);
            jthread_monitor_exit(jlc);
            return;
        }
    }

    // ---  step 8   ----------------------------------------------------------

    Method* meth = m_static_initializer;
    if(meth == NULL) {
        jthread_monitor_enter(jlc);
        lock();
        m_state = ST_Initialized;
        unlock();
        TRACE2("classloader", "class " << m_name->bytes << " initialized");
        m_initializing_thread = NULL;
        assert(!hythread_is_suspend_enabled());
        jthread_monitor_notify_all(jlc);
        jthread_monitor_exit(jlc);
        return;
    }

    TRACE2("class.init", "initializing class " << m_name->bytes << " STEP 8" );
    jthrowable p_error_object;

    assert(!hythread_is_suspend_enabled());
    // it's a safe point so environment should be protected
    vm_execute_java_method_array((jmethodID) meth, 0, 0);

    // suspend can be enabled in safe environment
    tmn_suspend_enable();
    p_error_object = exn_get();
    tmn_suspend_disable();

    // ---  step 9   ----------------------------------------------------------
    TRACE2("class.init", "initializing class " << m_name->bytes << " STEP 9" ); 

    if(!p_error_object) {
        jthread_monitor_enter(jlc);
        lock();
        m_state = ST_Initialized;
        unlock();
        TRACE2("classloader", "class " << m_name->bytes << " initialized");
        m_initializing_thread = NULL;
        assert(!hythread_is_suspend_enabled());
        jthread_monitor_notify_all(jlc);
        jthread_monitor_exit(jlc);
        return;
    }

    // ---  step 10  ----------------------------------------------------------
    assert(p_error_object != NULL);
    assert(!hythread_is_suspend_enabled());
    exn_clear();
    Class* p_error_class = p_error_object->object->vt()->clss;
    Class* jle = VM_Global_State::loader_env->java_lang_Error_Class;
    while(p_error_class && p_error_class != jle) {
        p_error_class = p_error_class->get_super_class();
    }
    assert(!hythread_is_suspend_enabled());
    if(p_error_class == NULL) {
        // class of p_error_object is not a descendant of java/lang/Error
#ifdef _DEBUG_REMOVED
        Class* eiie = VM_Global_State::loader_env->
            java_lang_ExceptionInInitializerError_Class;
        assert(eiie);
#endif
        tmn_suspend_enable();
        p_error_object = exn_create("java/lang/ExceptionInInitializerError",
            p_error_object);
        tmn_suspend_disable();
    }

    // ---  step 11  ----------------------------------------------------------
    assert(!hythread_is_suspend_enabled());
    jthread_monitor_enter(jlc);
    lock();
    m_state = ST_Error;
    unlock();
    m_initializing_thread = NULL;
    assert(!hythread_is_suspend_enabled());
    jthread_monitor_notify_all(jlc);
    jthread_monitor_exit(jlc);
    exn_raise_object(p_error_object);
    // end of 11 step class initialization program
} //class_initialize1


void class_initialize_from_jni(Class *clss)
{
    ASSERT_RAISE_AREA;
    assert(hythread_is_suspend_enabled());

    // check verifier constraints
    if(!clss->verify_constraints(VM_Global_State::loader_env)) {
        assert(exn_raised());
        return;
    }

    tmn_suspend_disable();
    if (!clss->is_initialized()) {
        clss->initialize();
    }
    tmn_suspend_enable();
} // class_initialize_from_jni


void class_initialize(Class *clss)
{
    ASSERT_RAISE_AREA;
    assert(!hythread_is_suspend_enabled());

    if(!clss->is_initialized()) {
        // check verifier constraints
        tmn_suspend_enable();
        if(!clss->verify_constraints(VM_Global_State::loader_env)) {
            assert(exn_raised());
            tmn_suspend_disable();
            return;
        }
        tmn_suspend_disable();

        clss->initialize();
    }
} // class_initialize
