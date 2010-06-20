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
 * Trace the java heap
 */

#include "cxxlog.h"

#include "open/gc.h"
#include "open/vm_field_access.h"
#include "open/vm_class_manipulation.h"
#include "slot.h"

// VM internal headers
#include "vm_arrays.h"

// private module headers
#include "jvmti_heap.h"
#include "jvmti_roots.h"
#include "jvmti_tags.h"
#include "jvmti_trace.h"

static void ti_trace_reference(TIEnv *ti_env,
        ManagedObject* referrer,
        ManagedObject* obj,
        jvmtiObjectReferenceKind reference_kind,
        jint referrer_index)
{
    TIIterationState *state = ti_env->iteration_state;

    // do nothing if the user requested abort termination
    if (state->abort) return;

    jlong class_tag = ti_get_object_class_tag(ti_env, obj);
    jlong size = ti_get_object_size(ti_env, obj);

    tag_pair** tp = ti_get_object_tptr(obj);
    jlong tag = (*tp != NULL ? (*tp)->tag : 0);

    jlong referrer_tag = ti_get_object_tag(ti_env, referrer);

    void *user_data = state->user_data;

    assert(state->object_ref_callback);
    jvmtiIterationControl r = state->object_ref_callback(reference_kind,
            class_tag, size, &tag, referrer_tag,
            referrer_index, user_data);

    ti_env->tags->update(obj, tag, tp);
    
    if (JVMTI_ITERATION_ABORT == r) {
        state->abort = true;
    }
    if (r != JVMTI_ITERATION_CONTINUE) return;

    if (ti_mark_object(obj, state)) {
        TRACE2("ti.trace", "marked " << (void*)obj
                << " from " << (void*)referrer);
        state->markstack->push(obj);
    }
}

static void ti_trace_object(TIEnv *ti_env, ManagedObject* referrer)
{
    TIIterationState* state = ti_env->iteration_state;
    state->objects += 1;
    state->bytes += ti_get_object_size(ti_env, referrer);
    TRACE2("ti.trace", "tracing object " << (void*)referrer);
    Class* ch = ((ManagedObject*)referrer)->vt()->clss;
    if (ch->is_array()) { 
        if (class_is_non_ref_array(ch)) return;

        // trace reference array
        int len = get_vector_length(referrer);
        int i;
        for (i = 0; i < len; i++) {
            Slot slot(get_vector_element_address_ref(referrer, i));
            if (slot.is_null()) continue;
            ti_trace_reference(
                    ti_env, referrer, 
                    (ManagedObject*) slot.dereference(),
                    JVMTI_REFERENCE_ARRAY_ELEMENT, i);
        }
    } else {
        // trace object fields
        unsigned num_fields = class_num_instance_fields_recursive(ch);
        unsigned i;
        for (i = 0; i < num_fields; i++) {
            Field_Handle fh = class_get_instance_field_recursive(ch, i);
			if(field_is_enumerable_reference(fh)){
                int offset = field_get_offset(fh);
                Slot slot((void*)((UDATA)referrer + offset));
                if (slot.is_null()) continue;
                ti_trace_reference(
                        ti_env, referrer, 
                        (ManagedObject*) slot.dereference(),
                        JVMTI_REFERENCE_FIELD, i);
            }
        }
    }
}

void ti_trace_heap(TIEnv* ti_env)
{
    TIIterationState *state = ti_env->iteration_state;
    state->objects = 0;
    state->bytes = 0;
    while (!state->markstack->empty() && !state->abort) {
        ManagedObject *obj = state->markstack->top();
        state->markstack->pop();
        ti_trace_object(ti_env, obj);
    }
    INFO2("ti.trace", "traced " << state->objects << " live objects, "
            << state->bytes << " live bytes");
}

bool ti_mark_object(Managed_Object_Handle obj, TIIterationState *state)
{
    assert((UDATA)obj < (UDATA)VM_Global_State::loader_env->heap_end);
    assert((UDATA)obj >= (UDATA)VM_Global_State::loader_env->heap_base);
    UDATA offset = (UDATA)obj - (UDATA)VM_Global_State::loader_env->heap_base;
    UDATA bitnum = offset / GC_OBJECT_ALIGNMENT;
    UDATA index = bitnum / 8;
    unsigned mask = 1 << (bitnum % 8);
    assert(index < state->markbits_size);
    bool unmarked = ((state->markbits[index] & mask) == 0);
    state->markbits[index] = (state->markbits[index] | mask);
    return unmarked;
}
