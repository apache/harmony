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
 * @file 
 * Verify stack enumeration code by conservative scanning the stack
 */
#ifdef _DEBUG

/* 
 * BEWARE! This code is used in _DEBUG configuration only 
 */

#include <set>

#define LOG_DOMAIN "verify.rse"
#include "cxxlog.h"
#include "open/gc.h"
#include "open/vm_properties.h"

#include "root_set_enum_internal.h"
#include "verify_stack_enumeration.h"

bool verify_stack_enumeration_flag = false; // true if verification is enabled
int verify_stack_enumeration_period = 1024; // verify each Nth safepoint
int verify_stack_enumeration_counter = 0;   // safepoint counter

static int counter = 0; // verification scans counter

static void *heap_base = NULL;
static void *heap_ceiling = NULL;

static void** stack_top = NULL;
static void** stack_end = NULL;

static std::set<void**> roots;
static int size = 0;

static void register_root(void** root)
{
    // register only stack roots
    if (stack_top < root && root < stack_end) {
        TRACE2("verify.rse.root", "registered stack root " << *root << " at " << root);
        ++size;
        roots.insert(root);
    }
}

typedef void (*add_root_set_entry_func)(Managed_Object_Handle *, Boolean);
static void add_root_set_entry(Managed_Object_Handle *ref, Boolean is_pinned)
{
    TRACE2("verify.rse.root", "root " << *ref << " at " << ref);
    register_root(ref);
}


typedef void (*add_weak_root_set_entry_func)(Managed_Object_Handle *ref, Boolean is_pinned,Boolean is_short_weak);
static void add_weak_root_set_entry(Managed_Object_Handle *ref, Boolean is_pinned,Boolean is_short_weak)
{
    TRACE2("verify.rse.root", "weak root " << *ref << " at " << ref);
    register_root(ref);
}


typedef void (*add_compressed_root_set_entry_func)(U_32 *ref, Boolean is_pinned);
static void add_compressed_root_set_entry(U_32 *ref, Boolean is_pinned)
{
    TRACE("compressed root " << *ref << " at " << ref << ", not verified");
}

typedef void (*add_root_set_entry_interior_pointer_func)(void **ref, int offset, Boolean is_pinned);
static void add_root_set_entry_interior_pointer (void **ref, int offset, Boolean is_pinned)
{
    TRACE("interior root " << *ref << "(-" << offset << ") at " << ref);
    register_root(ref);
}

static bool pointer_looks_like_root(void* pointer)
{
    // check just the pointer bounds for now
    return ((pointer > heap_base) && (pointer < heap_ceiling));
}

void verify_stack_enumeration() {

    // XXX: workaround to avoid infinite recursion
    // due to suspend_enable() in vm_gc_lock_enum()
    verify_stack_enumeration_flag = false;

    // grab gc lock or block in suspend-enabled mode
    vm_gc_lock_enum();

    // we are guaranteed that noone will start
    // enumeration while we are holding gc lock

    TRACE("--- verify_stack_enumeration started [" << counter << "]---");

    // cache heap boundaries
    if (NULL == heap_base) { heap_base = gc_heap_base_address(); }
    if (NULL == heap_ceiling) { heap_ceiling = gc_heap_ceiling_address(); }

    // GC must be prevented from happening while we hijack the GC functions
    assert(!hythread_is_suspend_enabled());

    // save away the GC function pointer
    add_root_set_entry_func gc_add_root_set_entry_saved 
        = gc_add_root_set_entry;  
    add_weak_root_set_entry_func gc_add_weak_root_set_entry_saved
        = gc_add_weak_root_set_entry;
    add_compressed_root_set_entry_func gc_add_compressed_root_set_entry_saved
        = gc_add_compressed_root_set_entry;
    add_root_set_entry_interior_pointer_func gc_add_root_set_entry_interior_pointer_saved
        = gc_add_root_set_entry_interior_pointer;

    // hijack to debug callback
    gc_add_root_set_entry = &add_root_set_entry;            
    gc_add_weak_root_set_entry = &add_weak_root_set_entry;
    gc_add_compressed_root_set_entry = &add_compressed_root_set_entry;
    gc_add_root_set_entry_interior_pointer = &add_root_set_entry_interior_pointer;

    assert(roots.empty());

    // find out stack boundaries
    VM_thread* thread = p_TLS_vmthread;
    void* local;
    stack_top = &local;
    stack_end = thread->stack_end;
    assert((((POINTER_SIZE_INT)stack_top) & (sizeof(void*)-1)) == 0 
            && "pointer must be aligned");
    assert((((POINTER_SIZE_INT)stack_end) & (sizeof(void*)-1)) == 0 
            && "pointer must be aligned");
    assert(stack_top < stack_end);

    // NOTE: we assume thread enumeration happens on *this* thread
    vm_enumerate_thread(p_TLS_vmthread);

    // scan the stack
    TRACE("  stack is from " << stack_top << " to " << stack_end);
    void** word = stack_top;
    std::set<void**>::iterator i;
    while (word < stack_end) {
        if (pointer_looks_like_root(*word)) {
            i = roots.find(word);
            if (i != roots.end()) {
                TRACE2("verify.rse.root", 
                    "pointer " << *word << " at " << word << " was enumerated");
                // drop the root
                roots.erase(i);
            } else {
                TRACE2("verify.rse.bug", 
                    "pointer " << *word << " at " << word << " was not enumerated");
            }
        }
        ++word;
    }

    i = roots.begin();
    while (i != roots.end()) {
        if (*i) {
            void** root = *i;
            TRACE2("verify.rse.bug", "the root " << *root << " at " << root
                << " was not found during conservative stack scan");
        }
        ++i;
    }

    // restore the GC function pointers
    gc_add_root_set_entry = gc_add_root_set_entry_saved;    
    gc_add_weak_root_set_entry = gc_add_weak_root_set_entry_saved;
    gc_add_compressed_root_set_entry = gc_add_compressed_root_set_entry_saved;
    gc_add_root_set_entry_interior_pointer = gc_add_root_set_entry_interior_pointer_saved;

    // free memory
    roots.clear();
    size = 0;

    TRACE("--- completed verify_stack_enumeration [" << counter++ << "]---");
    vm_gc_unlock_enum();

    // XXX: workaround to avoid infinite recursion
    // switch back to verification enabled
    // mode after releasing gc lock.
    verify_stack_enumeration_flag = true;
}

// Let it be literate :)
static const char* num_suffix(int n) {
    if ((n/10)%10 == 1) return "th"; // 10-19
    if (n%10 == 1) return "st"; // 1, 21, 31, ... 91
    if (n%10 == 2) return "nd"; // 2, 22, 32, ... 92
    if (n%10 == 3) return "rd"; // 3, 23, 33, ... 93
    return "th";
}

void initialize_verify_stack_enumeration()
{
    verify_stack_enumeration_flag = vm_property_get_boolean("verify.rse", false, VM_PROPERTIES);
    if (verify_stack_enumeration_flag) {
        INFO("verify stack enumeration mode");

        int n = vm_property_get_integer("verify.rse.after", 0, VM_PROPERTIES);
        if (n > 0) verify_stack_enumeration_counter = n;
        INFO(">verify after " << verify_stack_enumeration_counter);

        n = vm_property_get_integer("verify.rse.period", 0, VM_PROPERTIES);
        if (n > 0) verify_stack_enumeration_period = n;
        INFO(">verify each " << verify_stack_enumeration_period 
                << num_suffix(verify_stack_enumeration_period) << " iteration");
    }
}
#endif // _DEBUG
