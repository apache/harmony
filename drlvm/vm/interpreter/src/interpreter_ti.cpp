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
#include "open/vm_field_access.h"
#include "open/vm_method_access.h"
#include "open/vm_class_manipulation.h"
#include "interpreter.h"
#include "interpreter_exports.h"
#include "interpreter_imports.h"
#include "interp_defs.h"
#include "interp_native.h"
#include "port_malloc.h"
#include "thread_generic.h"

static jint skip_old_frames(VM_thread *thread)
{
    if (NULL == getLastStackFrame(thread))
        return 0;

    StackFrame* first_frame = (StackFrame*)(thread->firstFrame);

    if (first_frame)
    {
        Class *clss = method_get_class(first_frame->method);
        assert(clss);

        if (strcmp(method_get_name(first_frame->method), "runImpl") == 0 &&
            strcmp(class_get_name(clss), "java/lang/Thread") == 0)
        {
            return 1;
        }
    }

    return 0;
}

jvmtiError
interpreter_ti_get_frame_count(jvmtiEnv*, VM_thread *thread, jint* count_ptr) {
    StackFrame *frame = getLastStackFrame(thread);
    *count_ptr = 0;

    while(frame != 0) {
        (*count_ptr)++;
        frame = frame->prev;
    }

    (*count_ptr) -= skip_old_frames(thread);

    return JVMTI_ERROR_NONE;
}

jvmtiError
interpreter_ti_getLocalCommon(
        jvmtiEnv*,
        VM_thread *thread,
        jint depth,
        jint slot,
        StackFrame **framePtr) {
    StackFrame *frame = getLastStackFrame(thread);
    while(depth && frame != 0) {
        depth--;
        frame = frame->prev;
    }

    // check error condition: JVMTI_ERROR_NO_MORE_FRAMES
    if (depth != 0) {
        return JVMTI_ERROR_NO_MORE_FRAMES;
    }

    // check error condition: JVMTI_ERROR_OPAQUE_FRAME
    if (frame->ip == 0) {
        return JVMTI_ERROR_OPAQUE_FRAME;
    }

    unsigned uslot = slot;

    // check error condition: JVMTI_ERROR_INVALID_SLOT
    if (uslot >= frame->locals.getLocalsNumber()) {
        return JVMTI_ERROR_INVALID_SLOT;
    }

    *framePtr = frame;
    return JVMTI_ERROR_NONE;
}

jvmtiError
interpreter_ti_getLocal64(
        jvmtiEnv* env,
        VM_thread *thread,
        jint depth,
        jint slot,
        jlong* value_ptr) {
    StackFrame *frame;

    // check error condition: JVMTI_ERROR_NO_MORE_FRAMES
    // check error condition: JVMTI_ERROR_OPAQUE_FRAME
    // check error condition: JVMTI_ERROR_INVALID_SLOT
    jvmtiError err = interpreter_ti_getLocalCommon(env, thread, depth, slot, &frame);
    if (err != JVMTI_ERROR_NONE) return err;

    // TODO: check error condition: JVMTI_ERROR_TYPE_MISMATCH
    // partial check error condition: JVMTI_ERROR_TYPE_MISMATCH
    if (frame->locals.ref(slot) != 0 && frame->locals.ref(slot+1) != 0) {
        return JVMTI_ERROR_TYPE_MISMATCH;
    }

    *value_ptr = frame->locals.getLong(slot).i64;
    return JVMTI_ERROR_NONE;
}

jvmtiError
interpreter_ti_getLocal32(
        jvmtiEnv* env,
        VM_thread *thread,
        jint depth,
        jint slot,
        jint* value_ptr) {
    StackFrame *frame;

    // check error condition: JVMTI_ERROR_NO_MORE_FRAMES
    // check error condition: JVMTI_ERROR_OPAQUE_FRAME
    // check error condition: JVMTI_ERROR_INVALID_SLOT
    jvmtiError err = interpreter_ti_getLocalCommon(env, thread, depth, slot, &frame);
    if (err != JVMTI_ERROR_NONE) return err;

    // TODO: check error condition: JVMTI_ERROR_TYPE_MISMATCH
    // partial check error condition: JVMTI_ERROR_TYPE_MISMATCH
    if (frame->locals.ref(slot) != 0) {
        return JVMTI_ERROR_TYPE_MISMATCH;
    }

    *value_ptr = frame->locals(slot).i;
    return JVMTI_ERROR_NONE;
}

jvmtiError
interpreter_ti_getObject(
        jvmtiEnv* env,
        VM_thread *thread,
        jint depth,
        jint slot,
        jobject* value_ptr)
{
    StackFrame *frame;

    // check error condition: JVMTI_ERROR_NULL_POINTER
    if( value_ptr == NULL )
        return JVMTI_ERROR_NULL_POINTER;

    // check error condition: JVMTI_ERROR_NO_MORE_FRAMES
    // check error condition: JVMTI_ERROR_OPAQUE_FRAME
    // check error condition: JVMTI_ERROR_INVALID_SLOT
    jvmtiError err = interpreter_ti_getLocalCommon(env, thread, depth, slot, &frame);
    if (err != JVMTI_ERROR_NONE)
        return err;

    // TODO: check error condition: JVMTI_ERROR_TYPE_MISMATCH
    // partial check error condition: JVMTI_ERROR_TYPE_MISMATCH
    if (frame->locals.ref(slot) == 0) {
        return JVMTI_ERROR_TYPE_MISMATCH;
    }

    assert(hythread_is_suspend_enabled());
    hythread_suspend_disable();
    ManagedObject *obj = UNCOMPRESS_INTERP(frame->locals(slot).ref);
    if (NULL == obj) {
        *value_ptr = NULL;
    } else {
        ObjectHandle handle = oh_allocate_local_handle();
        handle->object = obj;
        *value_ptr = (jobject) handle;
    }
    hythread_suspend_enable();
    return JVMTI_ERROR_NONE;
}

jvmtiError
interpreter_ti_setLocal64(
        jvmtiEnv* env,
        VM_thread *thread,
        jint depth,
        jint slot,
        jlong value) {
    StackFrame *frame;

    // check error condition: JVMTI_ERROR_NO_MORE_FRAMES
    // check error condition: JVMTI_ERROR_OPAQUE_FRAME
    // check error condition: JVMTI_ERROR_INVALID_SLOT
    jvmtiError err = interpreter_ti_getLocalCommon(env, thread, depth, slot, &frame);
    if (err != JVMTI_ERROR_NONE) return err;

    // TODO: check error condition: JVMTI_ERROR_TYPE_MISMATCH
    // partial check error condition: JVMTI_ERROR_TYPE_MISMATCH
    if (frame->locals.ref(slot) != 0 && frame->locals.ref(slot+1) != 0) {
        return JVMTI_ERROR_TYPE_MISMATCH;
    }
 
    Value2 v;
    v.i64 = value;
    frame->locals.setLong(slot, v);
    return JVMTI_ERROR_NONE;
}

jvmtiError
interpreter_ti_setLocal32(
        jvmtiEnv* env,
        VM_thread *thread,
        jint depth,
        jint slot,
        jint value) {
    StackFrame *frame;

    // check error condition: JVMTI_ERROR_NO_MORE_FRAMES
    // check error condition: JVMTI_ERROR_OPAQUE_FRAME
    // check error condition: JVMTI_ERROR_INVALID_SLOT
    jvmtiError err = interpreter_ti_getLocalCommon(env, thread, depth, slot, &frame);
    if (err != JVMTI_ERROR_NONE) return err;

    // TODO: check error condition: JVMTI_ERROR_TYPE_MISMATCH
    // partial check error condition: JVMTI_ERROR_TYPE_MISMATCH
    if (frame->locals.ref(slot) != 0) {
        return JVMTI_ERROR_TYPE_MISMATCH;
    }

    frame->locals(slot).i = value;
    return JVMTI_ERROR_NONE;
}

jvmtiError
interpreter_ti_setObject(
        jvmtiEnv* env,
        VM_thread *thread,
        jint depth,
        jint slot,
        jobject value) {
    StackFrame *frame;

    // check error condition: JVMTI_ERROR_NO_MORE_FRAMES
    // check error condition: JVMTI_ERROR_OPAQUE_FRAME
    // check error condition: JVMTI_ERROR_INVALID_SLOT
    jvmtiError err = interpreter_ti_getLocalCommon(env, thread, depth, slot, &frame);
    if (err != JVMTI_ERROR_NONE) return err;

    // TODO: check error condition: JVMTI_ERROR_TYPE_MISMATCH
    // partial check error condition: JVMTI_ERROR_TYPE_MISMATCH
    if (frame->locals.ref(slot) == 0) {
        return JVMTI_ERROR_TYPE_MISMATCH;
    }

    ObjectHandle handle = (ObjectHandle) value;
    frame->locals(slot).ref = COMPRESS_INTERP(handle->object);
    return JVMTI_ERROR_NONE;
}

jvmtiError
interpreter_ti_getStackTrace(
        jvmtiEnv * UNREF env,
        VM_thread *thread,
        jint start_depth,
        jint max_frame_count,
        jvmtiFrameInfo *frame_buffer,
        jint *count_ptr) {
    jint start;
    StackFrame *frame = getLastStackFrame(thread);

    jint depth;
    interpreter_ti_get_frame_count(env, thread, &depth);

    if (start_depth < 0)
    {
        start = depth + start_depth;
        if (start < 0)
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }
    else
        start = start_depth;

    // skip start_depth of frames
    while (start) {
        if (frame == NULL)
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;

        frame = frame->prev;
        start--;
    }

    jint count = 0;
    while (frame != NULL && count < max_frame_count &&
           count < depth - start)
    {
        Method *m = frame->method;
        frame_buffer[count].method = (jmethodID)m;

        if (m->is_native())
            frame_buffer[count].location = -1;
        else
            frame_buffer[count].location = (jlocation)((U_8*)frame->ip -
                    (U_8*)m->get_byte_code_addr());

        frame = frame->prev;
        count++;
    }

    *count_ptr = count;
    return JVMTI_ERROR_NONE;
}

jvmtiError
interpreter_ti_getFrameLocation(
        jvmtiEnv * UNREF env, VM_thread *thread, jint depth,
        jmethodID *method_ptr, jlocation *location_ptr) {
    StackFrame *frame = getLastStackFrame(thread);

    // skip depth of frames
    while (depth) {
        if (frame == NULL) {
            return JVMTI_ERROR_NO_MORE_FRAMES;
        }
        frame = frame->prev;
        depth--;
    }

    Method *m = frame->method;
    *method_ptr = (jmethodID) m;
    if (m->is_native()) {
        *location_ptr = -1;
    } else {
        *location_ptr = (jlocation)((U_8*)frame->ip -
                (U_8*)frame->method->get_byte_code_addr());
    }
    return JVMTI_ERROR_NONE;
}

U_8
Opcode_BREAKPOINT(StackFrame& frame) {
    Method *m = frame.method;
    jlocation l = frame.ip - (U_8*)m->get_byte_code_addr();
    M2N_ALLOC_MACRO;
    U_8 b = (U_8) (POINTER_SIZE_INT) jvmti_process_interpreter_breakpoint_event((jmethodID)m, l);
    if(b == OPCODE_COUNT) {
        // breakpoint was remove by another thread, get original opcode
        b = *(frame.ip);
    }
    M2N_FREE_MACRO;
    return b;
}

jbyte interpreter_ti_set_breakpoint(jmethodID method, jlocation location) {
    Method *m = (Method*) method;
    U_8 *bytecodes = (U_8*) m->get_byte_code_addr();
    U_8 b = bytecodes[location];
    bytecodes[location] = OPCODE_BREAKPOINT;
    return b;
}

void interpreter_ti_clear_breakpoint(jmethodID method, jlocation location, jbyte saved) {
    Method *m = (Method*) method;
    U_8 *bytecodes = (U_8*) m->get_byte_code_addr();
    bytecodes[location] = saved;
}

int interpreter_ti_notification_mode = 0;

void interpreter_ti_set_notification_mode(jvmtiEvent event_type, bool UNREF enable) {
    int new_mask = 0;

    switch (event_type) {
        case JVMTI_EVENT_METHOD_ENTRY: new_mask = INTERPRETER_TI_METHOD_ENTRY_EVENT; break;
        case JVMTI_EVENT_METHOD_EXIT: new_mask = INTERPRETER_TI_METHOD_EXIT_EVENT; break;
        case JVMTI_EVENT_SINGLE_STEP: new_mask = INTERPRETER_TI_SINGLE_STEP_EVENT; break;
        case JVMTI_EVENT_FIELD_ACCESS: new_mask = INTERPRETER_TI_FIELD_ACCESS; break;
        case JVMTI_EVENT_FIELD_MODIFICATION: new_mask = INTERPRETER_TI_FIELD_MODIFICATION; break;
        case JVMTI_EVENT_EXCEPTION_CATCH: new_mask = INTERPRETER_TI_OTHER; break;
        case JVMTI_EVENT_EXCEPTION: new_mask = INTERPRETER_TI_OTHER; break;
        default: break;
    }

    if (enable) interpreter_ti_notification_mode |= new_mask;
    else interpreter_ti_notification_mode &= ~new_mask;

    if (!interpreter_ti_notification_mode)
        get_thread_ptr()->jvmti_thread.p_exception_object_ti = NULL;
}

void method_entry_callback(Method *method) {
#if 0
    fprintf(stderr, "enter%s: %s %s%s\n",
            method->is_native() ? " <native>": "",
            method->get_class()->name->bytes,
            method->get_name()->bytes,
            method->get_descriptor()->bytes);
#endif

    assert(!hythread_is_suspend_enabled());
    assert(interpreter_ti_notification_mode & INTERPRETER_TI_METHOD_ENTRY_EVENT);

    jvmti_process_method_entry_event((jmethodID) method);

    assert(!hythread_is_suspend_enabled());
}
void method_exit_callback(Method *method, bool was_popped_by_exception, jvalue ret_val) {
    assert(!hythread_is_suspend_enabled());
    assert(interpreter_ti_notification_mode & INTERPRETER_TI_METHOD_EXIT_EVENT);

    jvmti_process_method_exit_event((jmethodID) method,
            was_popped_by_exception, ret_val);

    assert(!hythread_is_suspend_enabled());
}

void
frame_pop_callback(FramePopListener *l, Method *method, jboolean was_popped_by_exception) {
    assert(!hythread_is_suspend_enabled());
    hythread_suspend_enable();

    while (l) {
        jvmtiEnv *env = (jvmtiEnv*) l->listener;
        jvmti_process_frame_pop_event(env, (jmethodID) method, was_popped_by_exception);
        FramePopListener *prev = l;
        l = l->next;
        STD_FREE((void*)prev);
    }
    hythread_suspend_disable();
}

jvmtiError
interpreter_ti_pop_frame(jvmtiEnv * UNREF env, VM_thread *thread) {
    assert(hythread_is_suspend_enabled());
    StackFrame *frame = getLastStackFrame(thread);
    if (frame->jvmti_pop_frame == POP_FRAME_AVAILABLE) {
        frame->jvmti_pop_frame = POP_FRAME_NOW;
        return JVMTI_ERROR_NONE;
    } else {
        return JVMTI_ERROR_OPAQUE_FRAME;
    }

}

jvmtiError
interpreter_ti_notify_frame_pop(jvmtiEnv* env,
                                VM_thread *thread,
                                int depth)
{
    assert(hythread_is_suspend_enabled());
    StackFrame *frame = getLastStackFrame(thread);

    // skip depth of frames
    while (depth) {
        if (frame == NULL) {
            return JVMTI_ERROR_NO_MORE_FRAMES;
        }
        frame = frame->prev;
        depth--;
    }

    Method *m = frame->method;
    if (m->is_native()) {
        return JVMTI_ERROR_OPAQUE_FRAME;
    }

    FramePopListener *l = frame->framePopListener;
    while (l) {
        // already set frame pop listener.
        if (l->listener == (void*)env) {
            return JVMTI_ERROR_NONE;
        }
        l = l->next;
    }

    // allocate new frame pop listener
    l = (FramePopListener*) STD_MALLOC(sizeof(FramePopListener));

    l->listener = (jvmtiEnv*) env;
    l->next = frame->framePopListener;
    frame->framePopListener = l;

    return JVMTI_ERROR_NONE;
}


void single_step_callback(StackFrame &frame) {
    U_8 ip0 = *frame.ip;
    hythread_suspend_enable();
    Method *method = frame.method;
    
    jvmti_process_single_step_event((jmethodID) method,
            frame.ip - (U_8*)method->get_byte_code_addr());

    hythread_suspend_disable();
}

////////////////////////////////////////
//  Interpreter frames iteration
FrameHandle* interpreter_get_last_frame(struct VM_thread *thread)
{
    return (FrameHandle*)getLastStackFrame(thread);
}

FrameHandle* interpreter_get_prev_frame(FrameHandle* frame)
{
    if (frame == NULL)
        return NULL;

    return (FrameHandle*)(((StackFrame*)frame)->prev);
}

Method_Handle interpreter_get_frame_method(FrameHandle* frame)
{
    return (Method_Handle)((StackFrame*)frame)->method;
}

U_8* interpreter_get_frame_bytecode_ptr(FrameHandle* frame)
{
    return ((StackFrame*)frame)->ip;
}

bool is_frame_in_native_frame(FrameHandle* frame, void* begin, void* end)
{
    return (frame >= begin && frame < end);
}

/////////////////////////////////
/// Field Watch functionality
//
static inline bool field_event_mask(Field *field, bool modify) {
    char *flag, mask;
    if (modify) {
        field_get_track_modification_flag(field, &flag, &mask);
    } else {
        field_get_track_access_flag(field, &flag, &mask);
    }
    return ((*flag & mask) != 0);
}


static inline void field_access_callback(Field *field, StackFrame& frame, ManagedObject *obj) {
    Method *method = frame.method;
    jlocation pc = frame.ip - (U_8*)method->get_code_addr();

    M2N_ALLOC_MACRO;

    jvmti_process_field_access_event(field, (jmethodID) method, pc, obj);

    M2N_FREE_MACRO;
}

static inline void field_modification_callback(Field *field, StackFrame& frame, ManagedObject * obj, jvalue val) {
    Method *method = frame.method;
    jlocation pc = frame.ip - (U_8*)method->get_code_addr();
    jvmti_process_field_modification_event(field, (jmethodID) method, pc, obj, val);
}

void getfield_callback(Field *field, StackFrame& frame) {
    if (!field_event_mask(field, false)) return;

    REF ref = frame.stack.pick(0).ref;
    ManagedObject *obj = UNCOMPRESS_INTERP(ref);
    field_access_callback(field, frame, obj);
}

void getstatic_callback(Field *field, StackFrame& frame) {
    if (!field_event_mask(field, false)) return;

    field_access_callback(field, frame, NULL);
}

jvalue new_field_value(Field *field, StackFrame& frame) {
    jvalue val;
    val.l = 0;
    switch (field->get_java_type()) {
        case VM_DATA_TYPE_BOOLEAN: val.z = (U_8) frame.stack.pick().u; break;
        case VM_DATA_TYPE_CHAR: val.c = (uint16) frame.stack.pick().u; break;
        case VM_DATA_TYPE_INT8: val.b = (I_8) frame.stack.pick().i; break;
        case VM_DATA_TYPE_INT16: val.s = (int16) frame.stack.pick().i; break;
        case VM_DATA_TYPE_INT32: val.i = frame.stack.pick().i; break;
        case VM_DATA_TYPE_INT64: val.j = frame.stack.getLong(0).i64; break;
        case VM_DATA_TYPE_F4: val.f = frame.stack.pick().f; break;
        case VM_DATA_TYPE_F8: val.d = frame.stack.getLong(0).d; break;
        case VM_DATA_TYPE_ARRAY:
        case VM_DATA_TYPE_CLASS:
        {
            ObjectHandle h = oh_allocate_local_handle();
            h->object = UNCOMPRESS_INTERP(frame.stack.pick().ref);
            val.l = h;
        }
            break;
        default:
            LDIE(52, "Unexpected data type");
    }
    return val;
}

void putstatic_callback(Field *field, StackFrame& frame) {
    if (!field_event_mask(field, true)) return;

    M2N_ALLOC_MACRO;
    jvalue val = new_field_value(field, frame);
    field_modification_callback(field, frame, NULL, val);
    M2N_FREE_MACRO;
}

void putfield_callback(Field *field, StackFrame& frame) {
    if (!field_event_mask(field, true)) return;

    Java_Type type = field->get_java_type();
    REF ref;

    if (type == VM_DATA_TYPE_INT64 || type == VM_DATA_TYPE_F8) {
        ref = frame.stack.pick(2).ref;
    } else {
        ref = frame.stack.pick(1).ref;
    }

    M2N_ALLOC_MACRO;
    jvalue val = new_field_value(field, frame);
    field_modification_callback(field, frame, UNCOMPRESS_INTERP(ref), val);
    M2N_FREE_MACRO;
}

