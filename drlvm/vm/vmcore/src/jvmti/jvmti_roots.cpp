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

/*
 * JVMTI heap API
 */

#define LOG_DOMAIN "ti.roots"
#include "cxxlog.h"

// global headers
#include "open/gc.h"
#include "open/vm_gc.h"
#include "open/hythread_ext.h"
#include "jit_import_rt.h"
// VM headers
#include "jvmti_support.h"
#include "jthread.h"
#include "Class.h"
#include "cci.h"
#include "classloader.h"
#include "finalize.h"
#include "interpreter.h"
#include "jit_intf_cpp.h"
#include "m2n.h"
#include "object_handles.h"
#include "object_layout.h"
#include "root_set_enum_internal.h"
#include "stack_iterator.h"
#include "suspend_checker.h"
#include "thread_manager.h"
#include "vm_arrays.h"

// JVMTI headers
#include "jvmti_direct.h"
#include "jvmti_utils.h"

// private headers
#include "jvmti_heap.h"
#include "jvmti_tags.h"
#include "jvmti_trace.h"

/**
 * calls stack root object callback.
 *
 * @param root is location of the root pointer (or interior pointer, or heap
 * offset).
 * @param obj is value of the root (differs from *root in case of interior
 * pointers).
 */
void vm_ti_enumerate_stack_root(
        jvmtiEnv* env,
        void* root, Managed_Object_Handle obj,
        jvmtiHeapRootKind root_kind,
        int depth,
        jmethodID method,
        int slot)
{
    // JVMTI does not care about NULL roots
    if (NULL == obj) return;

    TIEnv* ti_env = (TIEnv*)env;
    TIIterationState *state = ti_env->iteration_state;
    assert(state);
    if (state->abort) {
        // user requested iteration abort, so ignore root
        return;
    }

    jvmtiIterationControl r = JVMTI_ITERATION_CONTINUE;
    if (state->stack_ref_callback) {
        tag_pair **tp = ti_get_object_tptr(obj);
        jlong tag = (*tp != NULL ? (*tp)->tag : 0);
        jlong class_tag = ti_get_object_class_tag(ti_env, obj);
        jlong size = ti_get_object_size(ti_env, obj);

        void* user_data = state->user_data;
        jlong thread_tag = state->thread_tag;

        r = state->stack_ref_callback(root_kind, class_tag, size, &tag,
                thread_tag, depth, method, slot, user_data);
        ti_env->tags->update(obj, tag, tp);
    }

    if (JVMTI_ITERATION_ABORT == r) {
        state->abort = true;
    } else if (JVMTI_ITERATION_CONTINUE == r) {
        // push the reference to the mark stack for later tracing
        assert(state->markstack);
        if (ti_mark_object(obj, state)) {
            state->markstack->push((ManagedObject*)obj);
        }
    }
}

/**
 * calls heap root object callback.
 *
 * @param root is location of the root pointer (or interior pointer, or heap offset).
 * @param obj is value of the root (differs from *root in case of interior pointers).
 */
void vm_ti_enumerate_heap_root(
        jvmtiEnv* env,
        void* root,
        Managed_Object_Handle obj,
        jvmtiHeapRootKind root_kind)
{
    // JVMTI does not care about NULL roots
    if (NULL == obj) return;

    TIEnv* ti_env = (TIEnv*)env;
    TIIterationState *state = ti_env->iteration_state;
    assert(state);
    if (state->abort) {
        // user requested iteration abort, so ignore root
        return;
    }

    jvmtiIterationControl r = JVMTI_ITERATION_CONTINUE;
    if (state->heap_root_callback) {
        tag_pair** tp = ti_get_object_tptr(obj);
        jlong tag = ((*tp) != NULL ? (*tp)->tag : 0);
        jlong class_tag = ti_get_object_class_tag(ti_env, obj);
        jlong size = ti_get_object_size(ti_env, obj);

        void* user_data = state->user_data;

        r = state->heap_root_callback(root_kind, class_tag, size, &tag, user_data);
        ti_env->tags->update(obj, tag, tp);
    }

    if (JVMTI_ITERATION_ABORT == r) {
        state->abort = true;
    } else if (JVMTI_ITERATION_CONTINUE == r) {
        // push the reference to the mark stack for later tracing
        assert(state->markstack);
        if (ti_mark_object(obj, state)) {
            state->markstack->push((ManagedObject*)obj);
        }
    }
}

/**
 * calls root object callback, taking some information
 * from TIIterationState.
 *
 * @param root is location of the root pointer (or interior pointer, or heap offset).
 * @param obj is value of the root (differs from *root in case of interior pointers).
 */
static void ti_enumerate_root(void* root, Managed_Object_Handle obj)
{
    TIEnv* ti_env = global_ti_env; // FIXME: load ti_env from TLS
    TIIterationState *state = ti_env->iteration_state;
    assert(state);
    if (JVMTI_HEAP_ROOT_STACK_LOCAL == state->root_kind
            || JVMTI_HEAP_ROOT_JNI_LOCAL == state->root_kind) {
        jint depth = (jint)state->depth;
        jmethodID method = state->method;
        jint slot = (jint)(((UDATA)state->frame_base - (UDATA)root)/sizeof(void*));

        vm_ti_enumerate_stack_root((jvmtiEnv*)ti_env, 
                root, obj, state->root_kind, 
                depth, method, slot);
    } else {
        vm_ti_enumerate_heap_root((jvmtiEnv*)ti_env, 
                root, obj, 
                state->root_kind);
    }
}

////////////////////////////////////////
// hijacked enumeration functions
//

static void ti_add_root_set_entry(
        Managed_Object_Handle *root, 
        Boolean UNREF pinned)
{
    TRACE2("ti.root", "ti root " << root << " -> " << *root);
    ti_enumerate_root(root, *root);
}

static void ti_add_weak_root_set_entry(
        Managed_Object_Handle *root,
        Boolean UNREF pinned,
        Boolean UNREF short_weak)
{
    TRACE2("ti.root", "ti root " << root << " -> " << *root);
    // XXX: should weak roots be enumerated?
    ti_enumerate_root(root, *root);
}

static void ti_add_root_set_entry_interior_pointer(
        void **slot, 
        int offset, 
        Boolean UNREF pinned)
{
    Managed_Object_Handle obj = (Managed_Object_Handle)
        ((UDATA)*slot - offset);
    ti_enumerate_root(slot, obj);
}

static void ti_add_compressed_root_set_entry(
        U_32 *ref, 
        Boolean UNREF pinned)
{
    assert(REFS_IS_COMPRESSED_MODE);
#ifndef REFS_USE_UNCOMPRESSED
    Managed_Object_Handle obj = (Managed_Object_Handle)
        uncompress_compressed_reference(*ref);
    ti_enumerate_root(ref, obj);
#endif // REFS_USE_UNCOMPRESSED
}

//
// hijacked enumeration functions
////////////////////////////////////////

static void ti_enumerate_globals(TIEnv* ti_env)
{
    // this function reimplements the function
    // vm_enumerate_root_set_global_refs()

    TIIterationState *state = ti_env->iteration_state;
    state->root_kind = JVMTI_HEAP_ROOT_OTHER;

    // Static fields of all classes
    vm_enumerate_static_fields();
    vm_enumerate_objects_to_be_finalized();
    vm_enumerate_references_to_enqueue();

    state->root_kind = JVMTI_HEAP_ROOT_JNI_GLOBAL;
    oh_enumerate_global_handles();


    state->root_kind = JVMTI_HEAP_ROOT_OTHER;
    vm_enumerate_interned_strings();


    state->root_kind = JVMTI_HEAP_ROOT_MONITOR;
    extern void vm_enumerate_root_set_mon_arrays();
    vm_enumerate_root_set_mon_arrays();

    state->root_kind = JVMTI_HEAP_ROOT_SYSTEM_CLASS;
    ClassLoader::gc_enumerate();
}

static void ti_enumerate_thread_not_on_stack(TIEnv* ti_env, VM_thread* thread)
{
    TIIterationState *state = ti_env->iteration_state;
    state->root_kind = JVMTI_HEAP_ROOT_THREAD;

    assert(thread);
    if (thread->thread_exception.exc_object != NULL) {
        vm_enumerate_root_reference((void **)&(thread->thread_exception.exc_object), FALSE);
    }
    if (thread->thread_exception.exc_cause != NULL) {
        vm_enumerate_root_reference((void **)&(thread->thread_exception.exc_cause), FALSE);
    }
    if (thread->jvmti_thread.p_exception_object_ti != NULL) {
        vm_enumerate_root_reference((void **)&(thread->jvmti_thread.p_exception_object_ti), FALSE);
    }

    if (thread->native_handles)
        ((NativeObjectHandles*)(thread->native_handles))->enumerate();
    if (thread->gc_frames) {
        ((GcFrame*)(thread->gc_frames))->enumerate();
    }
}



static void ti_enumerate_thread_stack(TIEnv* ti_env, StackIterator* si)
{
    ASSERT_NO_INTERPRETER

    TIIterationState *state = ti_env->iteration_state;
    state->depth = 0;

    while (!si_is_past_end(si)) {

        CodeChunkInfo* cci = si_get_code_chunk_info(si);
        if (cci) {
            state->method = (jmethodID)cci->get_method();
            state->root_kind = JVMTI_HEAP_ROOT_STACK_LOCAL;
            // FIXME: set up frame base (platform dependent!)
            cci->get_jit()->get_root_set_from_stack_frame(cci->get_method(), 0, si_get_jit_context(si));
        } else {
            state->method = (jmethodID)m2n_get_method(si_get_m2n(si));
            state->root_kind = JVMTI_HEAP_ROOT_JNI_LOCAL;
            oh_enumerate_handles(m2n_get_local_handles(si_get_m2n(si)));
        }
        state->depth += 1;
        si_goto_previous(si);
    }
    si_free(si);
}

void jitted_ti_enumerate_thread(jvmtiEnv *env, VM_thread *thread)
{
    StackIterator* si;
    si = si_create_from_native(thread);
    ti_enumerate_thread_stack((TIEnv*)env, si);

    // Enumerate references associated with a thread that are not stored on the thread's stack.
    ti_enumerate_thread_not_on_stack((TIEnv*)env, thread);
}

static void ti_enumerate_thread(TIEnv *ti_env, VM_thread* thread)
{
    TIIterationState *state = ti_env->iteration_state;
    state->root_kind = JVMTI_HEAP_ROOT_THREAD;
    state->thread_tag = ti_env->tags->get(
            (Managed_Object_Handle)
            jthread_self()->object);

    if (interpreter_enabled()) {
        interpreter.interpreter_ti_enumerate_thread((jvmtiEnv*)ti_env, thread);
    } else {
        jitted_ti_enumerate_thread((jvmtiEnv*)ti_env, thread);
    }
}

void ti_enumerate_roots(TIEnv *ti_env, hythread_iterator_t iterator)
{
    TRACE2("ti.trace", "enumerating roots");

    // FIXME: weird function table manipulations
    void (*save_gc_add_root_set_entry)
        (Managed_Object_Handle *ref, Boolean pinned);
    void (*save_gc_add_weak_root_set_entry)
        (Managed_Object_Handle *ref1, Boolean pinned, Boolean short_weak);
    void (*save_gc_add_root_set_entry_interior_pointer)
        (void **slot, int offset, Boolean pinned);
    void (*save_gc_add_compressed_root_set_entry)
        (U_32 *ref, Boolean pinned);

    // save away old values
    save_gc_add_root_set_entry =
        gc_add_root_set_entry;
    save_gc_add_weak_root_set_entry =
        gc_add_weak_root_set_entry;
    save_gc_add_root_set_entry_interior_pointer =
        gc_add_root_set_entry_interior_pointer;
    save_gc_add_compressed_root_set_entry =
        gc_add_compressed_root_set_entry;

    // hijack ti enumeration functions
    gc_add_root_set_entry =
        ti_add_root_set_entry;
    gc_add_weak_root_set_entry =
        ti_add_weak_root_set_entry;
    gc_add_root_set_entry_interior_pointer =
        ti_add_root_set_entry_interior_pointer;
    gc_add_compressed_root_set_entry =
        ti_add_compressed_root_set_entry;

    // Run through list of active threads and enumerate each one of them.
    hythread_t tm_thread = hythread_iterator_next(&iterator);
    while (tm_thread && !ti_env->iteration_state->abort) {
        vm_thread_t thread = jthread_get_vm_thread(tm_thread);
        if (thread)
            ti_enumerate_thread(ti_env, thread);
        tm_thread = hythread_iterator_next(&iterator);
    }

    // finally, process all the global refs
    ti_enumerate_globals(ti_env);

    // restore original enumeration functions
    gc_add_root_set_entry =
        save_gc_add_root_set_entry;
    gc_add_weak_root_set_entry =
        save_gc_add_weak_root_set_entry;
    gc_add_root_set_entry_interior_pointer =
        save_gc_add_root_set_entry_interior_pointer;
    gc_add_compressed_root_set_entry =
        save_gc_add_compressed_root_set_entry;

    TRACE2("ti.trace", "completed root enumeration");
}
