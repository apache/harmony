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
 * @author Salikh Zakirov
 */  
/*
 * JVMTI heap API
 */

#include "cxxlog.h"
#include "port_mutex.h"

#include "jvmti_direct.h"
#include "jvmti_utils.h"

#include "open/gc.h"
#include "open/vm_gc.h"
#include "open/hythread_ext.h"

#include "object_layout.h"
#include "thread_manager.h"
#include "suspend_checker.h"
#include "vm_arrays.h"
#include "object_handles.h"
#include "vm_log.h"

// private module headers
#include "jvmti_heap.h"
#include "jvmti_roots.h"
#include "jvmti_tags.h"
#include "jvmti_trace.h"

// FIXME: use TLS to store ti_env
TIEnv* global_ti_env;

/*
 * Get Tag
 *
 * Retrieve the tag associated with an object. The tag is a long
 * value typically used to store a unique identifier or pointer to
 * object information. The tag is set with SetTag. Objects for
 * which no tags have been set return a tag value of zero.
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiGetTag(jvmtiEnv* env,
            jobject object,
            jlong* tag_ptr)
{
    TRACE2("jvmti.heap", "GetTag called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    TIEnv* ti_env = reinterpret_cast<TIEnv *>(env);

    if (!ti_env->posessed_capabilities.can_tag_objects)
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    if (!tag_ptr)
        return JVMTI_ERROR_NULL_POINTER;
    if (!is_jobject_valid(object))
        return JVMTI_ERROR_INVALID_OBJECT;

    if (ti_env->tags == NULL) {
        *tag_ptr = 0;
        return JVMTI_ERROR_NONE;
    }

    tmn_suspend_disable();  // ----vv
    Managed_Object_Handle obj = object->object;
    *tag_ptr = ti_env->tags->get(obj);
    tmn_suspend_enable();   // ----^^

    return JVMTI_ERROR_NONE;
}

/*
 * Set Tag
 *
 * Set the tag associated with an object. The tag is a long value
 * typically used to store a unique identifier or pointer to object
 * information. The tag is visible with GetTag.
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiSetTag(jvmtiEnv* env,
            jobject object,
            jlong tag)
{
    TRACE2("jvmti.heap", "SetTag called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_START, JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    TIEnv* ti_env = reinterpret_cast<TIEnv *>(env);

    if (!ti_env->posessed_capabilities.can_tag_objects)
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    if (!is_jobject_valid(object))
        return JVMTI_ERROR_INVALID_OBJECT;

    if (ti_env->tags == NULL) {
        port_mutex_lock(&ti_env->environment_data_lock);
        if (ti_env->tags == NULL) {
            ti_env->tags = new TITags;
        }
        port_mutex_unlock(&ti_env->environment_data_lock);
    }

    if (ti_env->tags == NULL) {
        return JVMTI_ERROR_OUT_OF_MEMORY;
    }

    tmn_suspend_disable();  // ----vv
    Managed_Object_Handle obj = object->object;
    ti_env->tags->set(obj, tag);
    tmn_suspend_enable();   // ----^^

    return JVMTI_ERROR_NONE;
}

/*
 * Force Garbage Collection
 *
 * Force the VM to perform a garbage collection. The garbage
 * collection is as complete as possible. This function does
 * not cause finalizers to be run. This function does not return
 * until the garbage collection is finished.
 *
 * REQUIRED Functionality
 */
jvmtiError JNICALL
jvmtiForceGarbageCollection(jvmtiEnv* env)
{
    TRACE2("jvmti.heap", "ForceGarbageCollection called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    gc_force_gc();

    return JVMTI_ERROR_NONE;
}

static jvmtiError allocate_iteration_state(TIEnv* ti_env)
{
    assert(NULL == ti_env->iteration_state);
    ti_env->iteration_state = new TIIterationState;
    TIIterationState* state = ti_env->iteration_state;
    if (NULL == state) {
        return JVMTI_ERROR_OUT_OF_MEMORY;
    }

    memset(state, 0, sizeof(TIIterationState));

    // we trust in correct values of global values
    // Global_Env::heap_base and Global_Env::heap_end
    assert((UDATA)ti_env->vm->vm_env->heap_base == (UDATA)gc_heap_base_address());
    assert((UDATA)ti_env->vm->vm_env->heap_end == (UDATA)gc_heap_ceiling_address());

    state->markbits_size = ((UDATA)ti_env->vm->vm_env->heap_end
        - (UDATA)ti_env->vm->vm_env->heap_base) / GC_OBJECT_ALIGNMENT / 8;
    state->markbits = new unsigned char[state->markbits_size];
    if (state->markbits == NULL) {
        delete state;
        ti_env->iteration_state = NULL;
        return JVMTI_ERROR_OUT_OF_MEMORY;
    }
    state->markstack = new std::stack<ManagedObject*>;
    if (state->markstack == NULL) {
        delete[] state->markbits;
        delete state;
        ti_env->iteration_state = NULL;
        return JVMTI_ERROR_OUT_OF_MEMORY;
    }
    memset(state->markbits, 0, state->markbits_size);
    return JVMTI_ERROR_NONE;
}

static void free_iteration_state(TIEnv* ti_env)
{
    assert(ti_env->iteration_state);
    assert(ti_env->iteration_state->markstack);
    assert(ti_env->iteration_state->markbits);
    delete ti_env->iteration_state->markstack;
    delete[] ti_env->iteration_state->markbits;
    delete ti_env->iteration_state;
    ti_env->iteration_state = NULL;
}


/*
 * Iterate Over Objects Reachable From Object
 *
 * This function iterates over all objects that are directly and
 * indirectly reachable from the specified object.
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiIterateOverObjectsReachableFromObject(jvmtiEnv* env,
                                           jobject object,
                                           jvmtiObjectReferenceCallback object_ref_callback,
                                           void* user_data)
{
    TRACE2("jvmti.heap", "IterateOverObjectsReachableFromObject called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    TIEnv* ti_env = reinterpret_cast<TIEnv *>(env);

    if (!ti_env->posessed_capabilities.can_tag_objects)
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    if (!object_ref_callback)
        return JVMTI_ERROR_NULL_POINTER;
    if (!is_jobject_valid(object))
        return JVMTI_ERROR_INVALID_OBJECT;

    hythread_global_lock();

    jvmtiError r;
    r = allocate_iteration_state(ti_env);
    if (r != JVMTI_ERROR_NONE) {
        hythread_global_unlock();
        return r;
    }

    hythread_suspend_disable(); // to keep assertions happy
    hythread_iterator_t iterator;
    hythread_suspend_all(&iterator, NULL);

    global_ti_env = ti_env; // FIXME: use TLS to store TIEnv pointer

    ti_env->iteration_state->user_data = user_data;
    ti_env->iteration_state->object_ref_callback = object_ref_callback;

    ti_env->iteration_state->markstack->push((ManagedObject*)object->object);
    ti_trace_heap(ti_env);

    free_iteration_state(ti_env);

    TRACE2("ti.iterate", "iteration complete");
    hythread_resume_all(NULL);
    hythread_suspend_enable();
    hythread_global_unlock();

    return JVMTI_ERROR_NONE;
}

static void ti_iterate_reachable(TIEnv* ti_env, 
        hythread_iterator_t iterator)
{
    // enumerate roots
    ti_enumerate_roots(ti_env, iterator);

    // check if we don't need to trace heap
    if (ti_env->iteration_state->abort
            || ti_env->iteration_state->object_ref_callback == NULL) return;

    ti_trace_heap(ti_env);
}

/*
 * Iterate Over Reachable Objects
 *
 * This function iterates over the root objects and all objects
 * that are directly and indirectly reachable from the root
 * objects. The root objects comprise the set of system classes,
 * JNI globals, references from thread stacks, and other objects
 * used as roots for the purposes of garbage collection.
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiIterateOverReachableObjects(jvmtiEnv* env,
                                 jvmtiHeapRootCallback heap_root_callback,
                                 jvmtiStackReferenceCallback stack_ref_callback,
                                 jvmtiObjectReferenceCallback object_ref_callback,
                                 void* user_data)
{
    TRACE2("jvmti.heap", "IterateOverReachableObjects called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    TIEnv* ti_env = reinterpret_cast<TIEnv *>(env);

    if (!ti_env->posessed_capabilities.can_tag_objects)
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;

    hythread_global_lock();

    jvmtiError r;
    r = allocate_iteration_state(ti_env);
    if (r != JVMTI_ERROR_NONE) {
        hythread_global_unlock();
        return r;
    }

    hythread_suspend_disable(); // to keep assertions happy
    hythread_iterator_t iterator;
    hythread_suspend_all(&iterator, NULL);

    global_ti_env = ti_env; // FIXME: use TLS to store TIEnv pointer

    ti_env->iteration_state->user_data = user_data;
    ti_env->iteration_state->heap_root_callback = heap_root_callback;
    ti_env->iteration_state->stack_ref_callback = stack_ref_callback;
    ti_env->iteration_state->object_ref_callback = object_ref_callback;

    ti_iterate_reachable(ti_env, iterator);

    free_iteration_state(ti_env);

    TRACE2("ti.iterate", "iteration complete");
    hythread_resume_all(NULL);
    hythread_suspend_enable();
    hythread_global_unlock();

    return JVMTI_ERROR_NONE;
}

bool vm_iterate_object(Managed_Object_Handle obj)
{
    TIEnv* ti_env = global_ti_env;  // FIXME: use TLS to store ti_env

    TRACE2("vm.iterate", "vm_iterate_object " << (ManagedObject*)obj);

    // Note: It is ok for ti_env->tags to be NULL at this point.

    jlong class_tag = ti_get_object_class_tag(ti_env, obj);
    tag_pair** tp = ti_get_object_tptr(obj);
    jlong tag = (*tp != NULL ? (*tp)->tag : 0);
    jlong size = ti_get_object_size(ti_env, obj);

    TIIterationState *state = ti_env->iteration_state;
    assert(state);
    void* user_data = state->user_data;
    jvmtiHeapObjectCallback heap_object_callback
        = state->heap_object_callback;

    if (state->class_filter != NULL &&
            ((ManagedObject*)obj)->vt()->clss != state->class_filter) {
        // do not run user callback if we were
        // asked to iterate instances of specified class
        return true;
    }

    if (state->object_filter == JVMTI_HEAP_OBJECT_UNTAGGED && tag != 0) {
        // do not run user callback for tagged objects
        // if we were asked for untagged objects only
        return true;
    }

    // in JVMTI_HEAP_OBJECT_TAGGED case, we should only get tagged objects
    assert(!(state->object_filter == JVMTI_HEAP_OBJECT_TAGGED && tag == 0));

    jvmtiIterationControl r =
        heap_object_callback(class_tag, size, &tag, user_data);

    // update tag value or remove tag
    ti_env->tags->update(obj, tag, tp);

    // return true to continue iteration, false to terminate it
    return (JVMTI_ITERATION_CONTINUE == r);
}

/*
 * Iterate Over Heap
 *
 * Iterate over all objects in the heap. This includes both
 * reachable and unreachable objects.
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiIterateOverHeap(jvmtiEnv* env,
                     jvmtiHeapObjectFilter object_filter,
                     jvmtiHeapObjectCallback heap_object_callback,
                     void* user_data)
{
    TRACE2("jvmti.heap", "IterateOverHeap called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    TIEnv* ti_env = reinterpret_cast<TIEnv *>(env);

    if (!ti_env->posessed_capabilities.can_tag_objects)
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    if (object_filter != JVMTI_HEAP_OBJECT_TAGGED
            && object_filter != JVMTI_HEAP_OBJECT_UNTAGGED
            && object_filter != JVMTI_HEAP_OBJECT_EITHER)
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;

    // According to JVMTI specification, if callback is NULL,
    // we must not process untagged objects.
    // Since the iteration is visible to user only through callback,
    // doing nothing will satisfy specification.
    if (!heap_object_callback)
        return JVMTI_ERROR_NONE;

    // heap iteration requires stop-the-world
    hythread_global_lock();

    assert(NULL == ti_env->iteration_state);
    ti_env->iteration_state = new TIIterationState;
    TIIterationState* state = ti_env->iteration_state;
    if (NULL == state) {
        hythread_global_unlock();
        return JVMTI_ERROR_OUT_OF_MEMORY;
    }

    memset(state, 0, sizeof(TIIterationState));

    hythread_suspend_disable(); // to keep assertions happy
    hythread_iterator_t  iterator;
    hythread_suspend_all(&iterator, NULL);
    TRACE2("ti.iterate", "suspended all threads");
    
    global_ti_env = ti_env; // FIXME: use TLS to store TIEnv pointer
    state->user_data = user_data;
    state->heap_object_callback = heap_object_callback;
    state->object_filter = object_filter;

    if (JVMTI_HEAP_OBJECT_TAGGED == object_filter) {
        // we iterate tagged objects directly from tags structure
        if (ti_env->tags)
            ti_env->tags->iterate();
    } else {
        // iterating untagged objects requires full heap iteration
        gc_iterate_heap();
    }

    delete ti_env->iteration_state;
    ti_env->iteration_state = NULL;

    TRACE2("ti.iterate", "iteration complete");
    hythread_resume_all(NULL);
    hythread_suspend_enable();
    hythread_global_unlock();

    return JVMTI_ERROR_NONE;
}

/*
 * Iterate Over Instances Of Class
 *
 * Iterate over all objects in the heap that are instances of
 * the specified class. This includes both reachable and
 * unreachable objects.
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiIterateOverInstancesOfClass(jvmtiEnv* env,
                                 jclass klass,
                                 jvmtiHeapObjectFilter object_filter,
                                 jvmtiHeapObjectCallback heap_object_callback,
                                 void* user_data)
{
    TRACE2("jvmti.heap", "IterateOverInstancesOfClass called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    TIEnv* ti_env = reinterpret_cast<TIEnv *>(env);

    if (!ti_env->posessed_capabilities.can_tag_objects)
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    if (!is_jclass_valid(klass))
        return JVMTI_ERROR_INVALID_CLASS;
    if (object_filter != JVMTI_HEAP_OBJECT_TAGGED
            && object_filter != JVMTI_HEAP_OBJECT_UNTAGGED
            && object_filter != JVMTI_HEAP_OBJECT_EITHER)
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;

    // According to JVMTI specification, if callback is NULL,
    // we must not process untagged objects.
    // Since the iteration is visible to user only through callback,
    // doing nothing will satisfy specification.
    if (!heap_object_callback)
        return JVMTI_ERROR_NONE;

    // heap iteration requires stop-the-world
    TRACE2("ti.iterate", "acquire tm lock");
    hythread_global_lock();
    TRACE2("ti.iterate", "got tm lock");

    assert(NULL == ti_env->iteration_state);
    ti_env->iteration_state = new TIIterationState;
    TIIterationState* state = ti_env->iteration_state;
    if (NULL == state) {
        hythread_global_unlock();
        return JVMTI_ERROR_OUT_OF_MEMORY;
    }

    memset(state, 0, sizeof(TIIterationState));

    hythread_suspend_disable(); // to keep assertions happy
    hythread_iterator_t  iterator;
    hythread_suspend_all(&iterator, NULL);
    TRACE2("ti.iterate", "suspended all threads");
    
    global_ti_env = ti_env; // FIXME: use TLS to store TIEnv pointer
    state->user_data = user_data;
    state->heap_object_callback = heap_object_callback;
    state->object_filter = object_filter;
    state->class_filter = jclass_to_struct_Class(klass);

    if (JVMTI_HEAP_OBJECT_TAGGED == object_filter) {
        // we iterate tagged objects directly from tags structure
        ti_env->tags->iterate();
    } else {
        // iterating untagged objects requires full heap iteration
        gc_iterate_heap();
    }

    delete ti_env->iteration_state;
    ti_env->iteration_state = NULL;

    TRACE2("ti.iterate", "iteration complete");
    hythread_resume_all(NULL);
    hythread_suspend_enable();
    hythread_global_unlock();

    return JVMTI_ERROR_NONE;
}

/*
 * Get Objects With Tags
 *
 * Return objects in the heap with the specified tags. The format
 * is parallel arrays of objects and tags.
 *
 * OPTIONAL Functionality
 */
jvmtiError JNICALL
jvmtiGetObjectsWithTags(jvmtiEnv* env,
                        jint tag_count,
                        const jlong* tags,
                        jint* count_ptr,
                        jobject** object_result_ptr,
                        jlong** tag_result_ptr)
{
    TRACE2("jvmti.heap", "GetObjectsWithTags called");
    SuspendEnabledChecker sec;
    /*
     * Check given env & current phase.
     */
    jvmtiPhase phases[] = {JVMTI_PHASE_LIVE};

    CHECK_EVERYTHING();

    TIEnv* ti_env = reinterpret_cast<TIEnv *>(env);

    if (!ti_env->posessed_capabilities.can_tag_objects)
        return JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    if (count_ptr == NULL || tags == NULL ||
            (object_result_ptr == NULL && tag_result_ptr == NULL))
        return JVMTI_ERROR_NULL_POINTER;
    if (tag_count <= 0)
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;

    std::set<jlong> tag_set;
    std::list<tag_pair> objects;
    jvmtiError error = JVMTI_ERROR_NONE;

    int i;
    for (i = 0; i < tag_count; i++) {
        if (tags[i] == 0)
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        tag_set.insert(tags[i]);
    }

    hythread_suspend_disable(); // ---------------vv

    ti_env->tags->get_objects_with_tags(tag_set, objects);
    int count = (int)objects.size();
    *count_ptr = count;
    if (count == 0) {
        // set output args to NULL
        // for compatibility
        if (object_result_ptr)
            *object_result_ptr = NULL;
        if (tag_result_ptr)
            *tag_result_ptr = NULL;
    } else {
        assert(count > 0);
        if (object_result_ptr != NULL) {
            jvmtiError r = _allocate(count * sizeof(jobject),
                    (unsigned char **)object_result_ptr);
            if (r == JVMTI_ERROR_NONE) {
                memset(*object_result_ptr, 0, count * sizeof(jobject));
                std::list<tag_pair>::iterator o;
                for (i = 0, o = objects.begin(); o != objects.end(); o++, i++) {
                    jobject jobj = oh_allocate_local_handle();
                    if (jobj) jobj->object = (ManagedObject*)o->obj;
                    else error = JVMTI_ERROR_OUT_OF_MEMORY;
                    (*object_result_ptr)[i] = jobj;
                }
            } else {
                error = r;
            }
            if (r != JVMTI_ERROR_NONE && (*object_result_ptr)) {
                _deallocate((unsigned char *)*object_result_ptr);
                *object_result_ptr = NULL;
            }
        }

        if (tag_result_ptr != NULL && error == JVMTI_ERROR_NONE) {
            jvmtiError r = _allocate(count * sizeof(jlong),
                    (unsigned char **)tag_result_ptr);
            if (JVMTI_ERROR_NONE == r) {
                memset(*tag_result_ptr, 0, count * sizeof(jlong));
                std::list<tag_pair>::iterator o;
                for (i = 0, o = objects.begin(); o != objects.end(); o++, i++) {
                    (*tag_result_ptr)[i] = o->tag;
                }
            } else {
                error = r;
            }
        }
    }
    
    hythread_suspend_enable();  // ---------------^^
    
    return error;
}
