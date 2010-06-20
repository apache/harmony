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
#include "interpreter.h"

#include "open/vm_method_access.h"
#include "interp_native.h"
#include "interp_defs.h"
#include "stack_trace.h"
#include "exceptions.h"
#include "jvmti_support.h"
#include "vtable.h"
#include "jit_import_rt.h"

typedef struct StackFrame StackIterator_interp;

static inline StackIterator_interp*
interp_si_create_from_native() {
    return getLastStackFrame();
}

static inline StackIterator_interp*
interp_si_create_from_native(VM_thread *thread) {
    return getLastStackFrame(thread);
}

static inline bool
interp_si_is_past_end(StackIterator_interp* si) {
    return si == 0;
}

static inline Method_Handle
interp_si_get_method(StackIterator_interp* si) {
    return (Method_Handle) si->method;
}

static inline NativeCodePtr
interp_si_get_ip(StackIterator_interp* si) {
    return si->ip;
}

static inline Boolean
interp_si_method_is_native(Method_Handle m) {
    assert(m);
    Method *meth = (Method *)m;
    return meth->is_native();
}

static inline Class_Handle
interp_si_method_get_class(Method_Handle m) {
    assert(m);
    return ((Method *)m)->get_class();
}

static inline void
interp_si_free(StackIterator_interp* UNREF si) {
    // nothing to be done!
}

/** Note: Tricky construction, to preserve visual compatibility */
static inline void
interp_si_goto_previous(StackIterator_interp* &si) {
    si = si->prev;
}


/*extern*/ bool
interpreter_st_get_frame(unsigned target_depth, StackTraceFrame* stf) {
    // st_get_frame
    
    StackIterator_interp* si = interp_si_create_from_native();
    unsigned depth = 0;
    while (!interp_si_is_past_end(si)) {
        stf->method = interp_si_get_method(si);
        if (stf->method) {
            if (depth==target_depth) {
                stf->ip = interp_si_get_ip(si);
                interp_si_free(si);
                return true;
            }
            depth++;
        }
        interp_si_goto_previous(si);
    }
    interp_si_free(si);
    return false;
}

static inline unsigned
interp_st_get_depth(VM_thread *p_thread) {
    StackIterator_interp* si = interp_si_create_from_native(p_thread);
    unsigned depth = 0;
    while (!interp_si_is_past_end(si)) {
        if (interp_si_get_method(si))
            depth++;
        interp_si_goto_previous(si);
    }
    interp_si_free(si);
    return depth;
}

unsigned  
interpreter_st_get_interrupted_method_native_bit(VM_thread *thread) {
    StackIterator_interp* si = interp_si_create_from_native(thread);
    if (interp_si_is_past_end(si)) {
        return 0;
    }
    Method * method = (Method *)interp_si_get_method(si);
    return method -> is_native();
 }

void
interpreter_st_get_trace(VM_thread *p_vmthread, unsigned* res_depth, StackTraceFrame** stfs) {
    unsigned depth = interp_st_get_depth(p_vmthread);
    StackTraceFrame* stf = st_alloc_frames(depth);
    assert(stf);
    *res_depth = depth;
    *stfs = stf;
    StackIterator_interp* si = interp_si_create_from_native(p_vmthread);
    depth = 0;
    while (!interp_si_is_past_end(si)) {
        Method_Handle method = interp_si_get_method(si);
        if (method) {
            stf->method = method;
            stf->ip = interp_si_get_ip(si);
            stf->depth = -1;
            stf->outdated_this = si->This;
            assert(stf->outdated_this || method->is_static());
            stf++;
            depth++;
        }
        interp_si_goto_previous(si);
    }
    assert(depth==*res_depth);
    interp_si_free(si);
}


#if defined(REF32) // Compressed references; cref is COMPRESSED_REFERENCE
#define vm_enumerate(cref,f) vm_enumerate_compressed_root_reference(cref,f)
#else // Uncompressed or runtime switchable; ref us uncompressed
static inline void** m2v(REF* obj) {
    return (void**)obj;
}
#define vm_enumerate(ref,f) vm_enumerate_root_reference(m2v(ref),f)
#endif

void
interp_enumerate_root_set_single_thread_on_stack(VM_thread *thread) {
    TRACE2("enumeration", "interp_enumerate_root_set_single_thread_on_stack()");
    StackIterator_interp* si;
    si = interp_si_create_from_native(thread);
    
    int i;
    DEBUG_GC("\n\nGC enumeration in interpreter stack:\n");
    while(!interp_si_is_past_end(si)) {
        Method* method = (Method*)interp_si_get_method(si);
        method = method;

        if (si->This) {
            vm_enumerate_root_reference((void**)&si->This, FALSE);
            DEBUG_GC("  [THIS]: " << si->This);
        }

        if (si->exc) {
            vm_enumerate_root_reference((void**)&si->exc, FALSE);
            DEBUG_GC("  [EXCEPTION]: " << si->exc);
        }

        if (method->is_native()) {
            DEBUG_GC("[METHOD <native>]: " << method);
            interp_si_goto_previous(si);
            continue;
        }

        DEBUG_GC("[METHOD "<< si->stack.size << " " << (int)si->locals.varNum << "]: "
                << method);

        if (si->stack.size)
            for(i = 0; i <= si->stack.index; i++) {
                if (si->stack.refs[i] == FLAG_OBJECT) {
                    DEBUG_GC("  Stack[" << i << "] ");
                    REF* ref = &si->stack.data[i].ref;
                    ManagedObject *obj = UNCOMPRESS_INTERP(*ref);
                    if (obj == 0) {
                        DEBUG_GC("NULL");
                    } else {
                        DEBUG_GC(obj);
                        vm_enumerate(ref, FALSE); // CHECK!!! can we enumerate uncompressed ref in compressed mode
                    }
                }
            }

                unsigned j;
        if (si->locals.varNum)
            for(j = 0; j < si->locals.varNum; j++) {
                if (si->locals.refs[j] == FLAG_OBJECT) {
                    DEBUG_GC("  Locals[" << j << "] ");
                    REF* ref = &si->locals.vars[j].ref;
                    ManagedObject *obj = UNCOMPRESS_INTERP(*ref);
                    if (obj == 0) {
                        DEBUG_GC("NULL\n");
                    } else {
                        DEBUG_GC(obj);
                        vm_enumerate(ref, FALSE); // CHECK!!! can we enumerate uncompressed ref in compressed mode
                    }
                }
            }
        MonitorList *ml = si->locked_monitors;
        while(ml) {
            vm_enumerate_root_reference((void**)&ml->monitor, FALSE);
            ml = ml->next;
        }
        interp_si_goto_previous(si);
    }

    // enumerate m2n frames
    M2nFrame *m2n = m2n_get_last_frame(thread);
    while(m2n) {
        oh_enumerate_handles(m2n_get_local_handles(m2n));
        m2n = m2n_get_previous_frame(m2n);
    }
}

/* extern*/void
interpreter_enumerate_thread(VM_thread *thread)
{
    interp_enumerate_root_set_single_thread_on_stack(thread);
    
    // Enumerate references associated with a thread that are not stored on the thread's stack.
    vm_enumerate_root_set_single_thread_not_on_stack(thread);
} //vm_enumerate_thread

void
interp_ti_enumerate_root_set_single_thread_on_stack(jvmtiEnv* ti_env, VM_thread *thread) {
    TRACE2("enumeration", "interp_enumerate_root_set_single_thread_on_stack()");
    StackIterator_interp* si;
    si = interp_si_create_from_native(thread);
    
    int i;
    int depth;
    DEBUG_GC("\n\nGC enumeration in interpreter stack:\n");
    for (depth = 0; !interp_si_is_past_end(si); depth++) {
        Method* method = (Method*)interp_si_get_method(si);
        jmethodID method_id = (jmethodID)method;
        int slot = 0;

        if (si->This) {
            vm_ti_enumerate_stack_root(ti_env,
                    (void**)&si->This, si->This,
                    JVMTI_HEAP_ROOT_STACK_LOCAL,
                    depth, method_id, slot++);
            DEBUG_GC("  [THIS]: " << si->This);
        }

        if (si->exc) {
            vm_ti_enumerate_stack_root(ti_env,
                (void**)&si->exc, si->exc,
                JVMTI_HEAP_ROOT_STACK_LOCAL,
                depth, method_id, slot++);
            DEBUG_GC("  [EXCEPTION]: " << si->exc);
        }

        if (method->is_native()) {
            DEBUG_GC("[METHOD <native>]: " << method);
            interp_si_goto_previous(si);
            continue;
        }

        DEBUG_GC("[METHOD "<< si->stack.size << " " << (int)si->locals.varNum << "]: "
                << method);

        if (si->stack.size)
            for(i = 0; i <= si->stack.index; i++) {
                if (si->stack.refs[i] == FLAG_OBJECT) {
                    DEBUG_GC("  Stack[" << i << "] ");
                    REF* ref = &si->stack.data[i].ref;
                    ManagedObject *obj = UNCOMPRESS_INTERP(*ref);
                    if (obj == 0) {
                        DEBUG_GC("NULL");
                    } else {
                        DEBUG_GC(obj);
                        vm_ti_enumerate_stack_root(ti_env,
                            ref, (Managed_Object_Handle)obj, 
                            JVMTI_HEAP_ROOT_STACK_LOCAL,
                            depth, method_id, slot++);
                    }
                }
            }

                unsigned j;
        if (si->locals.varNum)
            for(j = 0; j < si->locals.varNum; j++) {
                if (si->locals.refs[j] == FLAG_OBJECT) {
                    DEBUG_GC("  Locals[" << j << "] ");
                    REF* ref = &si->locals.vars[j].ref;
                    ManagedObject *obj = UNCOMPRESS_INTERP(*ref);
                    if (obj == 0) {
                        DEBUG_GC("NULL\n");
                    } else {
                        DEBUG_GC(obj);
                        vm_ti_enumerate_stack_root(ti_env,
                            ref, (Managed_Object_Handle)obj, 
                            JVMTI_HEAP_ROOT_STACK_LOCAL,
                            depth, method_id, slot++);
                    }
                }
            }
        MonitorList *ml = si->locked_monitors;
        while(ml) {
            vm_ti_enumerate_stack_root(ti_env,
                    &ml->monitor, ml->monitor,
                    JVMTI_HEAP_ROOT_MONITOR,
                    depth, method_id, slot++);
            ml = ml->next;
        }
        interp_si_goto_previous(si);
    }

    // enumerate m2n frames
    M2nFrame *m2n = m2n_get_last_frame(thread);
    while(m2n) {
        oh_enumerate_handles(m2n_get_local_handles(m2n));
        m2n = m2n_get_previous_frame(m2n);
    }
}

void interpreter_ti_enumerate_thread(jvmtiEnv *ti_env, VM_thread *thread)
{
    interp_ti_enumerate_root_set_single_thread_on_stack(ti_env, thread);

    // Enumerate references associated with a thread that are not stored on the thread's stack.
    vm_enumerate_root_set_single_thread_not_on_stack(thread);
}
