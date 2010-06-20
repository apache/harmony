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

#ifndef USE_GC_STATIC

#include <apr_dso.h>
#include <apr_errno.h>
#include "dll_gc.h"
#include "open/gc.h"
#include "open/vm_util.h"
#include "environment.h"
#include "properties.h"
/* $$$ GMJ #include "object_generic.h" */
#include "object.h"

static void default_gc_write_barrier(Managed_Object_Handle);
static void default_gc_pin_object(Managed_Object_Handle*);
static void default_gc_unpin_object(Managed_Object_Handle*);
static I_32 default_gc_get_hashcode(Managed_Object_Handle);
static Managed_Object_Handle default_gc_get_next_live_object(void*);
static void default_gc_iterate_heap();
static void default_gc_finalize_on_exit();
static void default_gc_set_mutator_block_flag();
static Boolean default_gc_clear_mutator_block_flag();
static int64 default_gc_max_memory();
static void default_gc_wrapup();
static Boolean default_gc_requires_barriers();
static Boolean default_gc_supports_compressed_references();
static void default_gc_heap_slot_write_ref(Managed_Object_Handle p_base_of_object_with_slot,
                                           Managed_Object_Handle *p_slot,
                                           Managed_Object_Handle value);
static void default_gc_heap_slot_write_ref_compressed(Managed_Object_Handle p_base_of_object_with_slot,
                                                      U_32 *p_slot,
                                                      Managed_Object_Handle value);
static void default_gc_heap_write_global_slot(Managed_Object_Handle *p_slot,
                                              Managed_Object_Handle value);
static void default_gc_heap_write_global_slot_compressed(U_32 *p_slot,
                                                         Managed_Object_Handle value);
static void default_gc_heap_wrote_object(Managed_Object_Handle p_base_of_object_just_written);
static void default_gc_add_compressed_root_set_entry(U_32 *ref);
static void default_gc_add_root_set_entry_managed_pointer(void **slot,
                                                          Boolean is_pinned);
static void *default_gc_heap_base_address();
static void *default_gc_heap_ceiling_address();
static void default_gc_test_safepoint();
/* $$$ GMJ static I_32 default_gc_get_hashcode(Managed_Object_Handle); */

static Boolean default_gc_supports_frontier_allocation(unsigned *offset_of_current, unsigned *offset_of_limit);
static void default_gc_add_weak_root_set_entry(
        Managed_Object_Handle*, Boolean, Boolean);

static Boolean default_gc_supports_class_unloading();

Boolean (*gc_supports_compressed_references)() = 0;
void (*gc_add_root_set_entry)(Managed_Object_Handle *ref, Boolean is_pinned) = 0;
void (*gc_add_weak_root_set_entry)(Managed_Object_Handle *ref, Boolean is_pinned,Boolean is_short_weak) = 0;
void (*gc_add_compressed_root_set_entry)(U_32 *ref, Boolean is_pinned) = 0;
void (*gc_add_root_set_entry_interior_pointer)(void **slot, int offset, Boolean is_pinned) = 0;
void (*gc_add_root_set_entry_managed_pointer)(void **slot, Boolean is_pinned) = 0;
void (*gc_class_prepared)(Class_Handle ch, VTable_Handle vth) = 0;
int64 (*gc_get_collection_count)() = 0;
int64 (*gc_get_collection_time)() = 0;
void (*gc_force_gc)() = 0;
int64 (*gc_free_memory)() = 0;
void (*gc_heap_slot_write_ref)(Managed_Object_Handle p_base_of_object_with_slot,
                               Managed_Object_Handle *p_slot,
                               Managed_Object_Handle value) = 0;
void (*gc_heap_slot_write_ref_compressed)(Managed_Object_Handle p_base_of_object_with_slot,
                                          U_32 *p_slot,
                                          Managed_Object_Handle value) = 0;
void (*gc_heap_write_global_slot)(Managed_Object_Handle *p_slot,
                                  Managed_Object_Handle value) = 0;
void (*gc_heap_write_global_slot_compressed)(U_32 *p_slot,
                                             Managed_Object_Handle value) = 0;
void (*gc_heap_write_ref)(Managed_Object_Handle p_base_of_object_with_slot,
                                 unsigned offset,
                                 Managed_Object_Handle value) = 0;
Boolean (*gc_heap_copy_object_array)(Managed_Object_Handle src_array, unsigned int src_start, Managed_Object_Handle dst_array, unsigned int dst_start, unsigned int length)=0;
void (*gc_heap_wrote_object)(Managed_Object_Handle p_base_of_object_just_written) = 0;
int (*gc_init)() = 0;
Boolean (*gc_is_object_pinned)(Managed_Object_Handle obj) = 0;

Managed_Object_Handle (*gc_alloc)(unsigned size, 
                                  Allocation_Handle p_vtable,
                                  void *thread_pointer) = 0;
Managed_Object_Handle (*gc_alloc_fast)(unsigned size, 
                                       Allocation_Handle p_vtable,
                                       void *thread_pointer) = 0;

void (*gc_vm_initialized)() = 0;
Boolean (*gc_requires_barriers)() = default_gc_requires_barriers;
void (*gc_thread_init)(void *gc_information) = 0;
void (*gc_thread_kill)(void *gc_information) = 0;
unsigned int (*gc_time_since_last_gc)()      = 0;
int64  (*gc_total_memory)()                  = 0;
int64  (*gc_max_memory)()                    = 0;
void * (*gc_heap_base_address)()             = 0;
void * (*gc_heap_ceiling_address)()          = 0;
void   (*gc_test_safepoint)()                = 0;

void (*gc_wrapup)() = default_gc_wrapup;
void (*gc_write_barrier)(Managed_Object_Handle p_base_of_obj_with_slot) = 0;
Boolean (*gc_supports_frontier_allocation)(unsigned *offset_of_current, unsigned *offset_of_limit) = 0;

void (*gc_pin_object)(Managed_Object_Handle* p_object) = 0;
void (*gc_unpin_object)(Managed_Object_Handle* p_object) = 0;
I_32 (*gc_get_hashcode)(Managed_Object_Handle obj) = 0;
Managed_Object_Handle (*gc_get_next_live_object)(void *iterator) = 0;
I_32 (*gc_get_hashcode0) (Managed_Object_Handle p_object) = 0;
void (*gc_iterate_heap)() = 0;

void (*gc_finalize_on_exit)() = 0;
void (*gc_set_mutator_block_flag)() = 0;
Boolean (*gc_clear_mutator_block_flag)() = 0;

Boolean (*gc_supports_class_unloading)() = 0;

static apr_dso_handle_sym_t getFunction(apr_dso_handle_t *handle, const char *name, const char *dllName)
{
    apr_dso_handle_sym_t fn; 
    if (apr_dso_sym(&fn, handle, name) != APR_SUCCESS) {
        LWARN(6, "Couldn't load GC dll {0}: missing entry point {1}" << dllName << name);
        return 0;
    }
    return fn;
}



static apr_dso_handle_sym_t getFunctionOptional(apr_dso_handle_t *handle,
                                   const char *name,
                                   const char * UNREF dllName,
                                   apr_dso_handle_sym_t default_func)
{
    apr_dso_handle_sym_t fn; 
    if (apr_dso_sym(&fn, handle, name) != APR_SUCCESS) {
        return default_func;
    } else {
        return fn;
    }
} //getFunctionOptional

static apr_pool_t *pool;

void vm_add_gc(const char *dllName)
{
    if(!pool) {
        apr_pool_create(&pool, 0);
    }
    
    apr_dso_handle_t *handle;
    if (apr_dso_load(&handle, dllName, pool) != APR_SUCCESS)
    {
        LWARN(7, "Failure to open GC dll {0}" << dllName);
        return;
    }


    gc_supports_compressed_references = (Boolean (*)())
        getFunctionOptional(handle, 
                            "gc_supports_compressed_references", 
                            dllName,
                            (apr_dso_handle_sym_t)default_gc_supports_compressed_references); 
    
    gc_add_root_set_entry = (void (*)(Managed_Object_Handle *ref, Boolean is_pinned)) 
        getFunction(handle, "gc_add_root_set_entry", dllName); 

    gc_add_weak_root_set_entry = (void (*)(Managed_Object_Handle *, Boolean, Boolean)) 
        getFunctionOptional(handle, "gc_add_weak_root_set_entry", dllName,
                (apr_dso_handle_sym_t) default_gc_add_weak_root_set_entry); 

    gc_add_compressed_root_set_entry = (void (*)(U_32 *ref, Boolean is_pinned)) 
        getFunctionOptional(handle, 
                            "gc_add_compressed_root_set_entry", 
                            dllName,
                            (apr_dso_handle_sym_t)default_gc_add_compressed_root_set_entry); 
    gc_add_root_set_entry_interior_pointer = (void (*)(void **slot, int offset, Boolean is_pinned)) getFunction(handle, "gc_add_root_set_entry_interior_pointer", dllName);
    gc_add_root_set_entry_managed_pointer = (void (*)(void **slot, Boolean is_pinned))
        getFunctionOptional(handle,
                            "gc_add_root_set_entry_managed_pointer",
                            dllName,
                            (apr_dso_handle_sym_t)default_gc_add_root_set_entry_managed_pointer);
    gc_class_prepared = (void (*)(Class_Handle ch, VTable_Handle vth)) getFunction(handle, "gc_class_prepared", dllName);
    gc_get_collection_count = (int64 (*)()) getFunction(handle, "gc_get_collection_count", dllName);
    gc_get_collection_time = (int64 (*)()) getFunction(handle, "gc_get_collection_time", dllName);
    gc_force_gc = (void (*)()) getFunction(handle, "gc_force_gc", dllName);
    gc_free_memory = (int64 (*)()) getFunction(handle, "gc_free_memory", dllName);
    gc_heap_slot_write_ref = (void (*)(Managed_Object_Handle p_base_of_object_with_slot,
                                       Managed_Object_Handle *p_slot,
                                       Managed_Object_Handle value))
        getFunctionOptional(handle,
                            "gc_heap_slot_write_ref",
                            dllName,
                            (apr_dso_handle_sym_t)default_gc_heap_slot_write_ref);
    gc_heap_slot_write_ref_compressed = (void (*)(Managed_Object_Handle p_base_of_object_with_slot,
                                                  U_32 *p_slot,
                                                  Managed_Object_Handle value))
        getFunctionOptional(handle,
                            "gc_heap_slot_write_ref_compressed",
                            dllName,
                            (apr_dso_handle_sym_t)default_gc_heap_slot_write_ref_compressed);    ;
    gc_heap_write_global_slot = (void (*)(Managed_Object_Handle *p_slot,
                                          Managed_Object_Handle value))        
        getFunctionOptional(handle,
                            "gc_heap_write_global_slot",
                            dllName,
                            (apr_dso_handle_sym_t)default_gc_heap_write_global_slot);
    gc_heap_write_global_slot_compressed = (void (*)(U_32 *p_slot,
                                                     Managed_Object_Handle value))        
        getFunctionOptional(handle,
                            "gc_heap_write_global_slot_compressed",
                            dllName,
                            (apr_dso_handle_sym_t)default_gc_heap_write_global_slot_compressed);
    gc_heap_copy_object_array = (Boolean (*)(Managed_Object_Handle src_array, unsigned int src_start, Managed_Object_Handle dst_array, unsigned int dst_start, unsigned int length))
	getFunctionOptional(handle,
                            "gc_heap_copy_object_array",
                            dllName,
                            (apr_dso_handle_sym_t)default_gc_heap_wrote_object);	
    gc_heap_wrote_object = (void (*)(Managed_Object_Handle p_base_of_object_just_written))
        getFunctionOptional(handle,
                            "gc_heap_wrote_object",
                            dllName,
                            (apr_dso_handle_sym_t)default_gc_heap_wrote_object);
    gc_init = (int (*)()) getFunction(handle, "gc_init", dllName);
    gc_is_object_pinned = (Boolean (*)(Managed_Object_Handle obj)) getFunction(handle, "gc_is_object_pinned", dllName);


    gc_alloc = (Managed_Object_Handle (*)(unsigned size, 
                                           Allocation_Handle type,
                                           void *thread_pointer)) 
        getFunctionOptional(handle, "gc_alloc", dllName, NULL);

    gc_alloc_fast = (Managed_Object_Handle (*)(unsigned size, 
                                           Allocation_Handle type,
                                           void *thread_pointer)) 
        getFunctionOptional(handle, "gc_alloc_fast", dllName, NULL);

    gc_pin_object = (void (*)(Managed_Object_Handle*))
        getFunctionOptional(handle, "gc_pin_object", dllName, 
            (apr_dso_handle_sym_t)default_gc_pin_object);

    gc_unpin_object = (void (*)(Managed_Object_Handle*))
        getFunctionOptional(handle, "gc_unpin_object", dllName,
            (apr_dso_handle_sym_t)default_gc_unpin_object);

    gc_get_hashcode = (I_32 (*)(Managed_Object_Handle))
        getFunctionOptional(handle, "gc_get_hashcode", dllName,
            (apr_dso_handle_sym_t)default_gc_get_hashcode);

    gc_get_next_live_object = (Managed_Object_Handle (*)(void*))
        getFunctionOptional(handle, "gc_get_next_live_object", dllName,
            (apr_dso_handle_sym_t)default_gc_get_next_live_object);

    gc_iterate_heap = (void (*)())
        getFunctionOptional(handle, "gc_iterate_heap", dllName,
            (apr_dso_handle_sym_t)default_gc_iterate_heap);
    
    gc_finalize_on_exit = (void (*)())
        getFunctionOptional(handle, "gc_finalize_on_exit", dllName,
            (apr_dso_handle_sym_t)default_gc_finalize_on_exit);

    gc_set_mutator_block_flag = (void (*)())
        getFunctionOptional(handle, "gc_set_mutator_block_flag", dllName,
            (apr_dso_handle_sym_t)default_gc_set_mutator_block_flag);

    gc_clear_mutator_block_flag = (Boolean (*)())
        getFunctionOptional(handle, "gc_clear_mutator_block_flag", dllName,
            (apr_dso_handle_sym_t)default_gc_clear_mutator_block_flag);

    gc_get_hashcode0 = (I_32 (*)(Managed_Object_Handle))
        getFunctionOptional(handle, "gc_get_hashcode", dllName, (apr_dso_handle_sym_t) default_gc_get_hashcode);

    gc_vm_initialized = (void (*)()) getFunction(handle, "gc_vm_initialized", dllName);
    gc_requires_barriers = (Boolean (*)()) 
        getFunctionOptional(handle, "gc_requires_barriers", dllName,
            (apr_dso_handle_sym_t)default_gc_requires_barriers);
    gc_thread_init = (void (*)(void *gc_information)) getFunction(handle, "gc_thread_init", dllName);
    gc_thread_kill = (void (*)(void *gc_information)) getFunction(handle, "gc_thread_kill", dllName);
    gc_time_since_last_gc = (unsigned int (*)()) getFunction(handle, "gc_time_since_last_gc", dllName);
    gc_total_memory = (int64 (*)()) getFunction(handle, "gc_total_memory", dllName);
    gc_max_memory = (int64 (*)()) 
        getFunctionOptional(handle, "gc_max_memory", dllName,
            (apr_dso_handle_sym_t)default_gc_max_memory);
    gc_heap_base_address = (void * (*)()) 
        getFunctionOptional(handle, 
                            "gc_heap_base_address", 
                            dllName, 
                            (apr_dso_handle_sym_t)default_gc_heap_base_address);
    gc_heap_ceiling_address = (void * (*)()) 
        getFunctionOptional(handle, 
                            "gc_heap_ceiling_address", 
                            dllName, 
                            (apr_dso_handle_sym_t)default_gc_heap_ceiling_address);
    gc_supports_frontier_allocation = (Boolean (*)(unsigned *offset_of_current, unsigned *offset_of_limit)) 
        getFunctionOptional(handle, 
                            "gc_supports_frontier_allocation", 
                            dllName, 
                            (apr_dso_handle_sym_t)default_gc_supports_frontier_allocation);

    gc_wrapup = (void (*)()) getFunction(handle, "gc_wrapup", dllName);
    gc_write_barrier = (void (*)(Managed_Object_Handle p_base_of_obj_with_slot)) 
        getFunctionOptional(handle, "gc_write_barrier", dllName,
            (apr_dso_handle_sym_t)default_gc_write_barrier);
    gc_test_safepoint = (void (*)()) getFunctionOptional(handle, "gc_test_safepoint", dllName, (apr_dso_handle_sym_t)default_gc_test_safepoint);

    gc_supports_class_unloading = (Boolean (*)())
        getFunctionOptional(handle, 
                            "gc_supports_class_unloading", 
                            dllName,
                            (apr_dso_handle_sym_t)default_gc_supports_class_unloading);     
} //vm_add_gc


bool vm_is_a_gc_dll(const char *dll_filename)
{
    if (!pool) {
        apr_pool_create(&pool, 0);
    }
    
    apr_dso_handle_t *handle;
    bool result = false;
    apr_status_t stat;
    if ((stat = apr_dso_load(&handle, dll_filename, pool)) == APR_SUCCESS)
    {
        apr_dso_handle_sym_t tmp;
        if (apr_dso_sym(&tmp, handle, "gc_init") == APR_SUCCESS) {
            result = true;
        }
        apr_dso_unload(handle);
    } else {
        char buf[1024];
        apr_dso_error(handle, buf, 1024);
        LWARN(8, "Loading error {0}" << buf);
        //apr_strerror(stat, buf, 1024);
        //printf("error %s, is %d, expected %d\n", buf, stat, APR_SUCCESS);
    }
    return result;
} //vm_is_a_gc_dll

static Boolean default_gc_requires_barriers()
{
    return FALSE;
} //default_gc_requires_barriers


static void default_gc_wrapup()
{
} //default_gc_wrapup


static Boolean default_gc_supports_compressed_references()
{
    return FALSE;
} //default_gc_supports_compressed_references


// This is the default implementation of the GC interface gc_heap_slot_write_ref() function for use by the VM
// when no other implementation is provided by a GC.
static void default_gc_heap_slot_write_ref(Managed_Object_Handle UNREF p_base_of_object_with_slot,
                                           Managed_Object_Handle *p_slot,
                                           Managed_Object_Handle value)
{
    assert(!REFS_IS_COMPRESSED_MODE);
    assert(p_slot != NULL);
    *p_slot = value;
} //default_gc_heap_slot_write_ref


static void default_gc_heap_slot_write_ref_compressed(Managed_Object_Handle UNREF p_base_of_object_with_slot,
                                                      U_32 *p_slot,
                                                      Managed_Object_Handle value)
{
    // p_slot is the address of a 32 bit slot holding the offset of a referenced object in the heap.
    // That slot is being updated, so store the heap offset of value's object. If value is NULL, store a 0 offset.
    assert(REFS_IS_COMPRESSED_MODE);
#ifndef REFS_USE_UNCOMPRESSED
    assert(p_slot != NULL);
    if (value != NULL) {
        COMPRESSED_REFERENCE value_offset = compress_reference((ManagedObject *)value);
        *p_slot = value_offset;
    } else {
        *p_slot = 0;
    }
#endif // REFS_USE_UNCOMPRESSED
} //default_gc_heap_slot_write_ref_compressed



static void default_gc_heap_write_global_slot(Managed_Object_Handle *p_slot,
                                              Managed_Object_Handle value)
{
    assert(p_slot != NULL);
    *p_slot = value;
} //default_gc_heap_write_global_slot



static void default_gc_heap_write_global_slot_compressed(U_32 *p_slot,
                                                         Managed_Object_Handle value)
{
    // p_slot is the address of a 32 bit global variable holding the offset of a referenced object in the heap.
    // That slot is being updated, so store the heap offset of value's object. If value is NULL, store a 0 offset.
    assert(REFS_IS_COMPRESSED_MODE);
#ifndef REFS_USE_UNCOMPRESSED
    assert(p_slot != NULL);
    if (value != NULL) {
        COMPRESSED_REFERENCE value_offset = compress_reference((ManagedObject *)value);
        *p_slot = value_offset;
    } else {
        *p_slot = 0;
    }
#endif // REFS_USE_UNCOMPRESSED
} //default_gc_heap_write_global_slot_compressed



static void default_gc_heap_wrote_object(Managed_Object_Handle UNREF p_base_of_object_just_written)
{
} //default_gc_heap_wrote_object



static void default_gc_add_compressed_root_set_entry(U_32 * UNREF ref)
{
    LDIE(7, "Fatal GC error: compressed references are not supported.");
} //default_gc_add_compressed_root_set_entry



static void default_gc_add_root_set_entry_managed_pointer(void ** UNREF slot,
                                                          Boolean UNREF is_pinned)
{
    LDIE(8,"Fatal GC error: managed pointers are not supported.");
} //default_gc_add_root_set_entry_managed_pointer



static void *default_gc_heap_base_address()
{
    return (void *)0;
} //default_gc_heap_base_address


static Boolean default_gc_supports_frontier_allocation(unsigned * UNREF offset_of_current, unsigned * UNREF offset_of_limit)
{
    return FALSE;
}

static I_32 default_gc_get_hashcode(Managed_Object_Handle obj) {
    return default_hashcode(obj);
}

static void *default_gc_heap_ceiling_address()
{
#ifdef POINTER64
    return (void *)0xffffFFFFffffFFFF;
#else  // !POINTER64
    return (void *)0xffffFFFF;
#endif // !POINTER64
} //default_gc_heap_ceiling_address


static void default_gc_test_safepoint()
{
    // Do nothing.
} // default_gc_test_safepoint

#define WARN_ONCE(message_number, messagedef_and_params) \
    {                               \
        static bool warning = true; \
        if (warning) {              \
            LWARN(message_number, messagedef_and_params);\
            warning = false;        \
        }                           \
    }


static void default_gc_pin_object(Managed_Object_Handle*)
{
    WARN_ONCE(9, "The GC did not provide {0}" << "gc_pin_object()");
}

static void default_gc_unpin_object(Managed_Object_Handle*)
{
    WARN_ONCE(9, "The GC did not provide {0}" << "gc_unpin_object()");
}

/* $$$ GMJ
static I_32 default_gc_get_hashcode(Managed_Object_Handle obj)
{
    return default_hashcode((ManagedObject*) obj);
}
*/

static Managed_Object_Handle default_gc_get_next_live_object(void*)
{
    WARN_ONCE(10, "The GC did not provide live object iterator");
    return NULL;
}

static void default_gc_iterate_heap()
{
    WARN_ONCE(11, "The GC did not provide heap iteration");
}

static void default_gc_finalize_on_exit()
{
    WARN_ONCE(12, "The GC did not provide finalization on exit");
}

static void default_gc_set_mutator_block_flag()
{
    WARN_ONCE(43, "The GC did not provide set mutator block flag");
}


static Boolean default_gc_clear_mutator_block_flag()
{
    WARN_ONCE(44, "The GC did not provide clear mutator block flag");
    return FALSE;
}

static int64 default_gc_max_memory()
{
    WARN_ONCE(9, "The GC did not provide {0}" << "gc_max_memory()");
    return 0x7fffFFFFl;
}

static void default_gc_write_barrier(Managed_Object_Handle)
{
}

static void default_gc_add_weak_root_set_entry(
        Managed_Object_Handle* root, Boolean pinned, Boolean is_short)
{
    WARN_ONCE(9, "The GC did not provide {0}" << "gc_add_weak_root_set_entry()");
    // default to strong reference semantics
    gc_add_root_set_entry(root, pinned);
}

static Boolean default_gc_supports_class_unloading()
{
    return TRUE;
} //default_gc_supports_class_unloading
#endif // !USE_GC_STATIC
