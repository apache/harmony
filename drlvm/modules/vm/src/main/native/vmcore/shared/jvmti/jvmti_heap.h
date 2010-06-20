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

#ifndef _JVMTI_HEAP_H_
#define _JVMTI_HEAP_H_

#include <stack>

// DRLVM exported headers
#include <open/types.h>
#include <open/vm.h>
#include <jvmti_types.h>

// DRLVM internal headers
#include "jvmti_direct.h"
#include "vm_arrays.h"

// private module headers
#include "jvmti_tags.h"

/**
 * @file
 * Internal header file for heap iteration functions.
 */

struct TIIterationState {

    // iterate over heap
    jvmtiHeapObjectCallback heap_object_callback;
    jvmtiHeapObjectFilter object_filter;

    // iterate over instance of class
    Class* class_filter;

    // iterate over reachable objects
    jvmtiHeapRootCallback heap_root_callback;
    jvmtiStackReferenceCallback stack_ref_callback;
    jvmtiObjectReferenceCallback object_ref_callback;

    // true means that user requested to terminate iteration,
    // by returning JVMTI_ITERATION_ABORT,  thus no subsequent
    // data is passed to user callbacks
    bool abort;
    // non-zero value means the error occured during heap iteration
    int error;

    // used to trace the heap
    unsigned char *markbits;
    UDATA markbits_size;
    std::stack<ManagedObject*> *markstack;

    // some data is set up in enclosing scope and saved here,
    // the actual callback function uses them
    // deeper in the stack to pass to user callback
    jvmtiHeapRootKind root_kind;    
    jlong thread_tag;
    jint depth;
    jmethodID method;
    void* frame_base; // used in calculation of slot index
    void* user_data;

    // debug data
    int objects;
    int bytes;
};

extern TIEnv* global_ti_env; // FIXME: store it in TLS

/**
 * returns object tag.
 */
inline jlong ti_get_object_tag(TIEnv *ti_env, Managed_Object_Handle obj)
{
    return ti_env->tags->get(obj);
}

/**
 * returns tag of object class.
 */
inline jlong ti_get_object_class_tag(TIEnv *ti_env, Managed_Object_Handle obj)
{
    Class* clss = ((ManagedObject*)obj)->vt()->clss;
    return ti_env->tags->get(*(clss->get_class_handle()));
}

/**
 * returns object size.
 */
inline jint ti_get_object_size(TIEnv *ti_env, Managed_Object_Handle obj)
{
    Class* clss = ((ManagedObject*)obj)->vt()->clss;
    if (clss->is_array()) {
        return vm_vector_size(clss, get_vector_length(obj));
    } else {
        return class_get_object_size(clss);
    }
}

/**
 * returns true if the object pointer looks like a valid object.
 */
inline bool is_object_valid(Managed_Object_Handle obj)
{
    return ((obj != NULL) && (obj > VM_Global_State::loader_env->heap_base)
            && (obj < VM_Global_State::loader_env->heap_end));
}

/**
 * returns true if the jobject looks like a valid object.
 */
inline bool is_jobject_valid(jobject jobj)
{
    if (jobj == NULL) return false;
    hythread_suspend_disable();
    bool r = is_object_valid(jobj->object);
    hythread_suspend_enable();
    return r;
}

/**
 * returns true if the jclass looks like a valid java.lang.Class instance.
 */
inline bool is_jclass_valid(jclass jobj)
{
    if (jobj == NULL) return false;
    hythread_suspend_disable();
    bool r = false;
    if (is_object_valid(jobj->object)) {
        Class* cls = ((ManagedObject*)jobj->object)->vt()->clss;
        r = cls->is_instanceof(VM_Global_State::loader_env->JavaLangClass_Class);
    }
    hythread_suspend_enable();
    return r;
}

#endif // _JVMTI_HEAP_H_
