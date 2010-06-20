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
#define LOG_DOMAIN "vm.core"
#include "cxxlog.h"

#include <stdlib.h>

#include "jit_import_rt.h"
#include "environment.h"
#include "exceptions.h"
#include "open/gc.h"
#include "lil.h"
#include "lock_manager.h"
#include "m2n.h"
#include "nogc.h"
#include "object_handles.h"
#include "vm_stats.h"
#include "vm_threads.h"

#include "thread_manager.h"
#include "open/types.h"
#include "open/vm_util.h"
#include "vtable.h"
#include "port_threadunsafe.h"

#ifndef NDEBUG
// this is for managed object sanity checks
// (used in monitor enter/exit functions)
#include "Class.h"
#endif

static ObjectHandlesOld* oh_allocate_object_handle();

//////////////////////////////////////////////////////////////////////////
// GC native interface

GcFrame::GcFrame()
{
    assert(!hythread_is_suspend_enabled());

    nodes = &firstSetOfNodes;

    nodes->capacity = GC_FRAME_DEFAULT_SIZE;
    nodes->obj_size = 0;
    nodes->mp_size = 0;
    nodes->next = NULL;

    next = (GcFrame*)p_TLS_vmthread->gc_frames;
    p_TLS_vmthread->gc_frames = this;
}

GcFrame::~GcFrame()
{
    assert(!hythread_is_suspend_enabled());

    assert(((GcFrame*)p_TLS_vmthread->gc_frames) == this);
    p_TLS_vmthread->gc_frames = next;
    next = NULL;
    GcFrameNode* c;
    GcFrameNode* n;
    for(c=nodes; c->next; c=n) {
        n = c->next;
        STD_FREE(c);
    }
    nodes = NULL;
}

void GcFrame::add_object(ManagedObject** p)
{
    assert(p);
    assert(NULL == *p || (*p >= vm_get_heap_base_address()
        && *p < vm_get_heap_ceiling_address()));
    assert(!hythread_is_suspend_enabled());

    ensure_capacity();
    nodes->elements[nodes->obj_size+nodes->mp_size] = nodes->elements[nodes->obj_size];
    nodes->elements[nodes->obj_size] = (void**)p;
    nodes->obj_size++;
}

void GcFrame::add_managed_pointer(ManagedPointer* p)
{
    assert(!hythread_is_suspend_enabled());

    ensure_capacity();
    nodes->elements[nodes->obj_size+nodes->mp_size] = (void**)p;
    nodes->mp_size++;
}

void GcFrame::enumerate()
{
    TRACE2("enumeration", "nodes = " << (void*) nodes);

    GcFrameNode* c;
    for(c=nodes; c; c=c->next) {
        unsigned i;
        for(i=0; i<c->obj_size; i++) 
            if (*c->elements[i])
                vm_enumerate_root_reference(c->elements[i], FALSE);
        for(i=0; i<c->mp_size; i++)
      // ? 20030710: GC V4 does not support this, and a static build fails on Linux
#ifdef PLATFORM_POSIX
      LDIE(45, "Not supported on this platform");
#else
            gc_add_root_set_entry_managed_pointer(c->elements[c->obj_size+i], FALSE);
#endif
    }
    if (next)
        next->enumerate();
}

void GcFrame::ensure_capacity()
{
    if (!nodes || nodes->obj_size+nodes->mp_size >= nodes->capacity) {
        GcFrameNode* n = (GcFrameNode*)STD_MALLOC(sizeof(GcFrameNode));
        assert(n);
        n->capacity = GC_FRAME_DEFAULT_SIZE;
        n->obj_size = 0;
        n->mp_size = 0;
        n->next = nodes;
        nodes = n;
    }
}

/*
 * Group of handle sanity checks.
 */
#ifndef NDEBUG
static bool
managed_object_is_java_lang_class_unsafe(ManagedObject* p_obj) {
    return p_obj->vt()->clss == VM_Global_State::loader_env->JavaLangClass_Class;
}

bool
managed_object_is_java_lang_class(ManagedObject* p_obj) {
    assert(!hythread_is_suspend_enabled());
    return managed_object_is_java_lang_class_unsafe(p_obj);
}

bool
object_is_java_lang_class(ObjectHandle oh) {
    assert(hythread_is_suspend_enabled());
    tmn_suspend_disable();
    bool res = managed_object_is_java_lang_class_unsafe(oh->object);
    tmn_suspend_enable();
    return res;
}

static bool
managed_object_object_is_valid_unsafe(ManagedObject* p_obj) {
    Class* clss = p_obj->vt()->clss;
    assert(clss->get_vtable()->clss == clss);
    return managed_object_is_java_lang_class_unsafe(*(clss->get_class_handle()));
}

bool
managed_object_is_valid(ManagedObject* p_obj) {
    assert(!hythread_is_suspend_enabled());
    return managed_object_object_is_valid_unsafe(p_obj);
}

bool
object_is_valid(ObjectHandle oh) {
    assert(hythread_is_suspend_enabled());
    tmn_suspend_disable();
    bool res = managed_object_object_is_valid_unsafe(oh->object);
    tmn_suspend_enable();
    return res;
}
#endif /* NDEBUG */

//////////////////////////////////////////////////////////////////////////
// Global Handles

static ObjectHandlesOld* global_object_handles = NULL;

static ObjectHandle oh_allocate_global_handle_internal()
{
    Global_Env * vm_env = VM_Global_State::loader_env;

    // Allocate and init handle
    ObjectHandlesOld* h = oh_allocate_object_handle(); //(ObjectHandlesOld*)m_malloc(sizeof(ObjectHandlesOld));

    if (h == NULL)  {
        return NULL;
    }

    h->handle.object = NULL;
    h->allocated_on_the_stack = false;
    
    hythread_suspend_disable(); // ----------------vvv
    vm_env->p_handle_lock->_lock(); 
    // Insert at beginning of globals list
    h->prev = NULL;
    h->next = global_object_handles;
    global_object_handles = h;
    if(h->next)
        h->next->prev = h;
    vm_env->p_handle_lock->_unlock();
    hythread_suspend_enable(); //--------------------------------------------^^^
    return &h->handle;
} //vm_create_global_object_handle

ObjectHandle oh_allocate_global_handle()
{
    ObjectHandle res = oh_allocate_global_handle_internal();
    assert(res);
    return res;
}

ObjectHandle oh_allocate_global_handle_from_jni()
{
    ObjectHandle res = oh_allocate_global_handle_internal();

    if (res == NULL) {
        exn_raise_object(VM_Global_State::loader_env->java_lang_OutOfMemoryError);
    }
    return res;
}

static bool UNUSED is_global_handle(ObjectHandle handle)
{
    for(ObjectHandlesOld* g = global_object_handles; g; g=g->next)
        if (g==(ObjectHandlesOld*)handle) return true;
    return false;
}

void oh_deallocate_global_handle(ObjectHandle handle)
{
    Global_Env * vm_env = VM_Global_State::loader_env;

    tmn_suspend_disable(); // ----------vvv
    vm_env->p_handle_lock->_lock();
    assert(is_global_handle(handle));

    handle->object = NULL;
    ObjectHandlesOld* h = (ObjectHandlesOld*)handle;
    if (h->next) h->next->prev = h->prev;
    if (h->prev) h->prev->next = h->next;
    if (h==global_object_handles) global_object_handles = h->next;

    vm_env->p_handle_lock->_unlock();
    tmn_suspend_enable(); // -------------------------------------^^^
    STD_FREE(h);
} //vm_delete_global_object_handle

static void oh_enumerate_weak_global_handles();

void oh_enumerate_global_handles()
{
    TRACE2("enumeration", "enumerating global handles");
    for(ObjectHandlesOld* g = global_object_handles; g; g=g->next)
        if (g->handle.object)
            vm_enumerate_root_reference((void**)&g->handle.object, FALSE);
    oh_enumerate_weak_global_handles();
}

//////////////////////////////////////////////////////////////////////////
// Weak Global Handles
static ObjectHandlesOld* weak_global_object_handles = NULL;

static ObjectHandle oh_allocate_weak_global_handle_internal()
{
    Global_Env * vm_env = VM_Global_State::loader_env;

    // Allocate and init handle
    ObjectHandlesOld* h = oh_allocate_object_handle(); //(ObjectHandlesOld*)m_malloc(sizeof(ObjectHandlesOld));

    if (h == NULL)  {
        return NULL;
    }

    h->handle.object = NULL;
    h->allocated_on_the_stack = false;
    
    hythread_suspend_disable(); // ----------------vvv
    vm_env->p_handle_lock->_lock(); 
    // Insert at beginning of globals list
    h->prev = NULL;
    h->next = weak_global_object_handles;
    weak_global_object_handles = h;
    if(h->next)
        h->next->prev = h;
    vm_env->p_handle_lock->_unlock();
    hythread_suspend_enable(); //--------------------------------------------^^^
    return &h->handle;
} //vm_create_weak_global_object_handle

ObjectHandle oh_allocate_weak_global_handle_from_jni()
{
    ObjectHandle res = oh_allocate_weak_global_handle_internal();

    if (res == NULL) {
        exn_raise_object(VM_Global_State::loader_env->java_lang_OutOfMemoryError);
    }
    return res;
}

static bool UNUSED is_weak_global_handle(ObjectHandle handle)
{
    for(ObjectHandlesOld* g = weak_global_object_handles; g; g=g->next)
        if (g==(ObjectHandlesOld*)handle) return true;
    return false;
}

void oh_deallocate_weak_global_handle(ObjectHandle handle)
{
    Global_Env * vm_env = VM_Global_State::loader_env;

    tmn_suspend_disable(); // ----------vvv
    vm_env->p_handle_lock->_lock();
    assert(is_weak_global_handle(handle));

    handle->object = NULL;
    ObjectHandlesOld* h = (ObjectHandlesOld*)handle;
    if (h->next) h->next->prev = h->prev;
    if (h->prev) h->prev->next = h->next;
    if (h==weak_global_object_handles) weak_global_object_handles = h->next;

    vm_env->p_handle_lock->_unlock();
    tmn_suspend_enable(); // -------------------------------------^^^
    STD_FREE(h);
} //vm_delete_global_object_handle

static void oh_enumerate_weak_global_handles()
{
    TRACE2("enumeration", "enumerating weak global handles");
    for(ObjectHandlesOld* g = weak_global_object_handles; g; g=g->next)
        if (g->handle.object)
            vm_enumerate_weak_root_reference((void**)&g->handle.object, FALSE);
}

//////////////////////////////////////////////////////////////////////////
// ObjectHandles

static ObjectHandlesOld* oh_allocate_object_handle()
{
    ObjectHandlesOld* h = (ObjectHandlesOld*)STD_MALLOC(sizeof(ObjectHandlesOld));

    if (h == NULL)  {
        return NULL;
    }

    assert(h);
    memset(h, 0, sizeof(ObjectHandlesOld));
    return h;
}

static ObjectHandlesNew* oh_add_new_handles(ObjectHandlesNew** hs)
{
    unsigned capacity = 10;
    unsigned size = sizeof(ObjectHandlesNew)+sizeof(ManagedObject*)*(capacity-1);
    ObjectHandlesNew* n = (ObjectHandlesNew*)STD_MALLOC(size);

    if (n == NULL) {
        return NULL;
    }

    assert(n);
    memset(n, 0, size);
#ifdef _IPF_
    n->capacity = (U_32)capacity;
#else //IA32
    n->capacity = (uint16)capacity;
#endif //IA32
    n->size = 0;
    n->next = *hs;
    *hs = n;
    return n;
}

static ObjectHandle oh_allocate_handle(ObjectHandles** hs)
{
    // the function should be called only from suspend disabled mode
    // as it is not gc safe.
    assert(!hythread_is_suspend_enabled());
    ObjectHandlesNew* cur = (ObjectHandlesNew*)*hs;

    if (!cur || cur->size>=cur->capacity) {
        ObjectHandlesNew* new_handle_block = oh_add_new_handles((ObjectHandlesNew**)hs);

        if (new_handle_block == NULL) {
            return NULL;
        }
        assert(new_handle_block);
        cur = new_handle_block;
    }
    ObjectHandle h = (ObjectHandle)&cur->refs[cur->size];
    cur->size++;
    h->object = NULL;
    return h;
}

VMEXPORT // temporary solution for interpreter unplug
void oh_enumerate_handles(ObjectHandles* hs)
{
    for(ObjectHandlesNew* cur = (ObjectHandlesNew*)hs; cur; cur=cur->next)
        for(unsigned i=0; i<cur->size; i++)
            if (cur->refs[i])
                vm_enumerate_root_reference((void**)&cur->refs[i], FALSE);
}

void oh_free_handles(ObjectHandles* hs)
{
    ObjectHandlesNew* h = (ObjectHandlesNew*)hs;
    while(h) {
        ObjectHandlesNew* next = h->next;
        STD_FREE(h);
        h = next;
    }
}

//////////////////////////////////////////////////////////////////////////
// Native Object Handles

VMEXPORT // temporary solution for interpreter unplug
NativeObjectHandles::NativeObjectHandles()
: handles(NULL)
{
    next = (NativeObjectHandles*)p_TLS_vmthread->native_handles;
    p_TLS_vmthread->native_handles = this;
}

VMEXPORT // temporary solution for interpreter unplug
NativeObjectHandles::~NativeObjectHandles()
{
    assert(((NativeObjectHandles*)p_TLS_vmthread->native_handles) == this);
    p_TLS_vmthread->native_handles = next;
    next = NULL;
    oh_free_handles(handles);
}

ObjectHandle NativeObjectHandles::allocate()
{
    ObjectHandle res = oh_allocate_handle(&handles);
    if (res == NULL) {
        printf("GBPLTW");
    }
    assert(res);
    return res;
}

void NativeObjectHandles::enumerate()
{
    oh_enumerate_handles(handles);
    if (next)
        next->enumerate();
}

//////////////////////////////////////////////////////////////////////////
// Local Handles

static ObjectHandle oh_allocate_local_handle_internal()
{
    assert(!hythread_is_suspend_enabled());

    // There are really 3 cases to check:
    //   1) JNI transition: both LJF and LJF->local_object_handles are
    //      non-NULL. Any local handles will be cleaned up on return.
    //   2) RNI/stub transition: LJF is non-NULL but LJF->local_object_handles
    //      is NULL. Although an LJF frame was pushed, local handles will NOT
    //      be cleaned up, so to avoid a storage leak we use the same code as
    //      case (3) below.
    //   3) LJF is NULL. Native code uses Native_Local_Object_Handles, whose
    //      destructor cleans up any local handles.

    // RNI is not used at all, and we need local handles, so (2) is handled by
    // creating new local handles.
    // XXX: need to check against leaks. -salikh

    M2nFrame* lm2nf = m2n_get_last_frame();
    ObjectHandles* hs = (lm2nf ? m2n_get_local_handles(lm2nf) : NULL);
    ObjectHandle res;
    if (lm2nf /* && hs */) { // -salikh
        res = oh_allocate_handle(&hs);
        m2n_set_local_handles(lm2nf, hs);
    } else {
        assert(p_TLS_vmthread->native_handles);
        res = ((NativeObjectHandles*)p_TLS_vmthread->native_handles)->allocate();
    }
    return res;
}

VMEXPORT // temporary solution for interpreter unplug
ObjectHandle oh_allocate_local_handle() {
    ObjectHandle res = oh_allocate_local_handle_internal();
    assert(res);
    return res;
}

ObjectHandle oh_allocate_local_handle_from_jni() {
    ObjectHandle res = oh_allocate_local_handle_internal();

    if (res == NULL) {
        exn_raise_object(VM_Global_State::loader_env->java_lang_OutOfMemoryError);
    }
    return res;
}


ObjectHandle oh_convert_to_local_handle(ManagedObject* pointer) {
    assert(!hythread_is_suspend_enabled());
    assert(pointer);
    ObjectHandle jobj = oh_allocate_local_handle();
    TRACE2("oh", "oh_convert_to_local_handle() pointer = " << pointer << ", handle = " << jobj);
    jobj->object = pointer;
    return jobj;
}


ObjectHandle oh_copy_to_local_handle(ObjectHandle oh) {
    tmn_suspend_disable();
    assert(oh);
    assert(oh->object);
    ObjectHandle jobj = oh_allocate_local_handle();
    TRACE2("oh", "oh_copy_to_local_handle() oh = " << oh << ", handle = " << jobj);
    jobj->object = oh->object;
    tmn_suspend_enable();
    return jobj;
}

void oh_discard_local_handle(ObjectHandle handle)
{
   tmn_suspend_disable_recursive(); // --------------vvv

    // This ensures that the object will not be kept live by this handle
    // Actual freeing can come latter
    handle->object = NULL;

    tmn_suspend_enable_recursive(); // -----------------------------------------^^^
}

void free_local_object_handles(ObjectHandle head, ObjectHandle tail)
{
    assert(head && tail);
    ObjectHandlesOld* h = (ObjectHandlesOld*)head;
    while(h!=(ObjectHandlesOld*)tail) {
#ifdef VM_STATS
        VM_Statistics::get_vm_stats().num_local_jni_handles++;
#endif //VM_STATS
        ObjectHandlesOld* next = h->next;
        STD_FREE(h);
        h = next;
    }
}

void free_local_object_handles3(ObjectHandles* head)
{
    ObjectHandlesNew* h = (ObjectHandlesNew*)head;
#ifdef VM_STATS
    UNSAFE_REGION_START
    VM_Statistics::get_vm_stats().num_free_local_called++;
    if(h != NULL)
        VM_Statistics::get_vm_stats().num_free_local_called_free++;
        UNSAFE_REGION_END
#endif //VM_STATS
    while(h) {
#ifdef VM_STATS
        unsigned size = h->size;
        UNSAFE_REGION_START
        VM_Statistics::get_vm_stats().num_local_jni_handles += size;
        VM_Statistics::get_vm_stats().num_jni_handles_freed++;
        VM_Statistics::get_vm_stats().num_jni_handles_wasted_refs += (h->capacity - size);
        UNSAFE_REGION_END
#endif //VM_STATS
        ObjectHandlesNew* next = h->next;
        STD_FREE(h);
        h = next;
    }
}

VMEXPORT // temporary solution for interpreter unplug
void free_local_object_handles2(ObjectHandles* head)
{
    ObjectHandlesNew* h = (ObjectHandlesNew*)head;
    assert(h);
#ifdef VM_STATS
    UNSAFE_REGION_START
    VM_Statistics::get_vm_stats().num_free_local_called++;
    if(h->next != NULL)
        VM_Statistics::get_vm_stats().num_free_local_called_free++;
        UNSAFE_REGION_END
#endif //VM_STATS
    while(h->next) {
#ifdef VM_STATS
        unsigned size = h->size;
        UNSAFE_REGION_START
        VM_Statistics::get_vm_stats().num_local_jni_handles += size;
        VM_Statistics::get_vm_stats().num_jni_handles_freed++;
        VM_Statistics::get_vm_stats().num_jni_handles_wasted_refs += (h->capacity - size);
        UNSAFE_REGION_END
#endif //VM_STATS
        ObjectHandlesNew* next = h->next;
        STD_FREE(h);
        h = next;
    }
#ifdef VM_STATS
    UNSAFE_REGION_START
    VM_Statistics::get_vm_stats().num_jni_handles_wasted_refs += (h->capacity - h->size);
    UNSAFE_REGION_END
#endif //VM_STATS
}

// Fill bjectHandles sructure as empty.
void oh_null_init_handles(ObjectHandles* handles) {
    ((ObjectHandlesNew*) handles)->capacity = 0;
    ((ObjectHandlesNew*) handles)->size = 0;
    ((ObjectHandlesNew*) handles)->next = 0;

}

LilCodeStub* oh_gen_allocate_handles(LilCodeStub* cs, unsigned number_handles, const char* base_var, const char* UNREF helper_var)
{
    char buf[200];

    const unsigned cap_off = (unsigned)(POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->capacity;
#ifdef _IPF_
    const unsigned size_off = (unsigned)(POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->size;
#endif //!_IPF_
    const unsigned next_off = (unsigned)(POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->next;

    unsigned extra_handles = 8;
    unsigned handles_capacity = number_handles+extra_handles;
    unsigned size_of_object_handles = sizeof(ObjectHandlesNew)+sizeof(ManagedObject*)*(handles_capacity-1);

    sprintf(buf, "alloc %s,%d;", base_var, size_of_object_handles);
    cs = lil_parse_onto_end(cs, buf);
    if (!cs) return NULL;

#ifdef _IPF_
    sprintf(buf,
            "st [%s+%d:g4],%d; st [%s+%d:g4],%d; st [%s+%d:pint],0;",
            base_var, cap_off, handles_capacity, base_var, size_off, number_handles, base_var, next_off);
#else //!_IPF_
    assert(handles_capacity < 0x10000);
    unsigned int capsize = (number_handles<<16) | handles_capacity;
    sprintf(buf, "st [%s+%d:g4],%d; st [%s+%d:pint],0;", base_var, cap_off, capsize, base_var, next_off);
#endif //!_IPF_
    cs = lil_parse_onto_end(cs, buf);
    if (!cs) return NULL;

    cs = lil_parse_onto_end(cs, "handles=l0;");
    if (!cs) return NULL;

    return cs;
}

POINTER_SIZE_INT oh_get_handle_offset(unsigned handle_indx)
{
    const POINTER_SIZE_INT refs_off = (POINTER_SIZE_INT)&((ObjectHandlesNew*)0)->refs;
    return refs_off+handle_indx*sizeof(ManagedObject*);
}

LilCodeStub* oh_gen_init_handle(LilCodeStub* cs, const char* base_var, unsigned handle_indx, const char* val, bool null_check)
{
    char buf[200];
    POINTER_SIZE_INT offset = oh_get_handle_offset(handle_indx);
    if (null_check && REFS_IS_COMPRESSED_MODE) {
        sprintf(buf,
                "jc %s=0x%"PI_FMT"X:ref,%%n; st [%s+%"PI_FMT"d:ref],%s; j %%o; :%%g; st [%s+%"PI_FMT"d:ref],0; :%%g;",
                val, (POINTER_SIZE_INT)VM_Global_State::loader_env->heap_base,
                base_var, offset,
                val, base_var, offset);
    } else {
        sprintf(buf, "st [%s+%"PI_FMT"d:ref],%s;", base_var, offset, val);
    }
    cs = lil_parse_onto_end(cs, buf);
    return cs;
}
