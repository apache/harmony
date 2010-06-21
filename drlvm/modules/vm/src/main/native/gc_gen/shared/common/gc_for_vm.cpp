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
 * @author Xiao-Feng Li, 2006/10/05
 */

#include <cxxlog.h>
#include "open/vm_properties.h"
#include "open/vm_properties.h"
#include "port_sysinfo.h"
#include "vm_threads.h"
#include "jit_runtime_support.h"
#include "compressed_ref.h"
#include "../gen/gen.h"
#include "../mark_sweep/gc_ms.h"
#include "../move_compact/gc_mc.h"
#include "interior_pointer.h"
#include "../thread/conclctor.h"
#include "../thread/collector.h"
#include "../verify/verify_live_heap.h"
#include "../finalizer_weakref/finalizer_weakref.h"
#include "collection_scheduler.h"
#include "gc_concurrent.h"
#ifdef USE_32BITS_HASHCODE
#include "hashcode.h"
#endif

static GC* p_global_gc = NULL;
Boolean mutator_need_block;

void gc_tls_init();

Boolean gc_requires_barriers() 
{   return p_global_gc->generate_barrier; }

static void gc_get_system_info(GC *gc)
{
  gc->_machine_page_size_bytes = (unsigned int)port_vmem_page_sizes()[0];
  gc->_num_processors = port_CPUs_number();
  gc->_system_alloc_unit = vm_get_system_alloc_unit();
  SPACE_ALLOC_UNIT = max(gc->_system_alloc_unit, GC_BLOCK_SIZE_BYTES);
}

static void init_gc_helpers()
{
    vm_properties_set_value("vm.component.classpath.gc_gen", "gc_gen.jar", VM_PROPERTIES);
    vm_helper_register_magic_helper(VM_RT_NEW_RESOLVED_USING_VTABLE_AND_SIZE, "org/apache/harmony/drlvm/gc_gen/GCHelper", "alloc");
    vm_helper_register_magic_helper(VM_RT_NEW_VECTOR_USING_VTABLE,  "org/apache/harmony/drlvm/gc_gen/GCHelper", "allocArray");
    vm_helper_register_magic_helper(VM_RT_GC_HEAP_WRITE_REF,  "org/apache/harmony/drlvm/gc_gen/GCHelper", "write_barrier_slot_rem");
    vm_helper_register_magic_helper(VM_RT_GET_IDENTITY_HASHCODE,  "org/apache/harmony/drlvm/gc_gen/GCHelper", "get_hashcode");
}

int gc_init() 
{
  INFO2("gc.process", "GC: call GC init...\n");
  if (p_global_gc != NULL) {
      return JNI_ERR;
  }

  vm_gc_lock_init();

  GC* gc = gc_parse_options();
  assert(gc);

  p_global_gc = gc;

#ifdef BUILD_IN_REFERENT
   if( ! IGNORE_FINREF){
     INFO2(" gc.init" , "finref must be ignored, since BUILD_IN_REFERENT is defined." );
     IGNORE_FINREF = TRUE;
   }
#endif 

/* VT pointer compression is a compile-time option, reference compression and vtable compression are orthogonal */
#ifdef COMPRESS_VTABLE
  assert(vm_is_vtable_compressed());
  vtable_base = (POINTER_SIZE_INT)vm_get_vtable_base_address();
#endif

  gc_tls_init();
  
  gc_get_system_info(gc);
  
  gc_metadata_initialize(gc); /* root set and mark stack */

#if defined(USE_UNIQUE_MARK_SWEEP_GC)
  gc_ms_initialize((GC_MS*)gc, min_heap_size_bytes, max_heap_size_bytes);
#elif defined(USE_UNIQUE_MOVE_COMPACT_GC)
  gc_mc_initialize((GC_MC*)gc, min_heap_size_bytes, max_heap_size_bytes);
#else
  gc_gen_initialize((GC_Gen*)gc, min_heap_size_bytes, max_heap_size_bytes);
#endif
  
  set_native_finalizer_thread_flag(!IGNORE_FINREF);
  set_native_ref_enqueue_thread_flag(!IGNORE_FINREF);

#ifndef BUILD_IN_REFERENT
  gc_finref_metadata_initialize(gc);
#endif

  collection_scheduler_initialize(gc);

  if(gc_is_specify_con_gc()){
     gc->gc_concurrent_status = GC_CON_NIL;
    conclctor_initialize(gc);
  } else {
     gc->gc_concurrent_status = GC_CON_DISABLE;
  }
  
  collector_initialize(gc);
  
  gc_init_heap_verification(gc);

  init_gc_helpers();
  
  mutator_need_block = FALSE;

  INFO2("gc.process", "GC: end of GC init\n");
  return JNI_OK;
}

void gc_wrapup() 
{ 
  INFO2("gc.process", "GC: call GC wrapup ....");
  GC* gc =  p_global_gc;
  // destruct threads first, and then destruct data structures
  conclctor_destruct(gc);
  collector_destruct(gc);

#if defined(USE_UNIQUE_MARK_SWEEP_GC)
 gc_ms_destruct((GC_MS*)gc);
#elif defined(USE_UNIQUE_MOVE_COMPACT_GC)
 gc_mc_destruct((GC_MC*)gc);
#else
  gc_gen_wrapup_verbose((GC_Gen*)gc);
  gc_gen_destruct((GC_Gen*)gc);
#endif

  gc_metadata_destruct(gc); /* root set and mark stack */
#ifndef BUILD_IN_REFERENT
  gc_finref_metadata_destruct(gc);
#endif

  if( verify_live_heap ){
    gc_terminate_heap_verification(gc);
  }

  STD_FREE(gc->tuner);
  
  STD_FREE(p_global_gc);

  p_global_gc = NULL;
  INFO2("gc.process", "GC: end of GC wrapup\n");
}

Boolean gc_supports_compressed_references()
{
#ifdef COMPRESS_REFERENCE
  return TRUE;
#else
  return FALSE;
#endif
}

/* this interface need reconsidering. is_pinned is unused. */
void gc_add_root_set_entry(Managed_Object_Handle *ref, Boolean is_pinned) 
{
  Partial_Reveal_Object** p_ref = (Partial_Reveal_Object**)ref;
  Partial_Reveal_Object* p_obj = *p_ref;
  /* we don't enumerate NULL reference and nos_boundary
     FIXME:: nos_boundary is a static field in GCHelper.java for fast write barrier, not a real object reference 
     this should be fixed that magic Address field should not be enumerated. */
#ifdef COMPRESS_REFERENCE
  if (p_obj == (Partial_Reveal_Object*)HEAP_BASE || p_obj == NULL || p_obj == nos_boundary ) return;
#else
  if (p_obj == NULL || p_obj == nos_boundary ) return;
#endif  
  assert( !obj_is_marked_in_vt(p_obj));
  /* for Minor_collection, it's possible for p_obj be forwarded in non-gen mark-forward GC. 
     The forward bit is actually last cycle's mark bit.
     For Major collection, it's possible for p_obj be marked in last cycle. Since we don't
     flip the bit for major collection, we may find it's marked there.
     So we can't do assert about oi except we really want. */
  assert( address_belongs_to_gc_heap(p_obj, p_global_gc));
  gc_rootset_add_entry(p_global_gc, p_ref);
} 

void gc_add_root_set_entry_interior_pointer (void **slot, int offset, Boolean is_pinned) 
{  
  add_root_set_entry_interior_pointer(slot, offset, is_pinned); 
}

void gc_add_compressed_root_set_entry(REF* ref, Boolean is_pinned)
{
  REF *p_ref = (REF *)ref;
  if(read_slot(p_ref) == NULL) return;
  Partial_Reveal_Object* p_obj = read_slot(p_ref);
  assert(!obj_is_marked_in_vt(p_obj));
  assert( address_belongs_to_gc_heap(p_obj, p_global_gc));
  gc_compressed_rootset_add_entry(p_global_gc, p_ref);
}

Boolean gc_supports_class_unloading()
{
  return TRACE_JLC_VIA_VTABLE;
}

void gc_add_weak_root_set_entry(Managed_Object_Handle *ref, Boolean is_pinned, Boolean is_short_weak)
{
  //assert(is_short_weak == FALSE); //Currently no need for short_weak_roots
  Partial_Reveal_Object** p_ref = (Partial_Reveal_Object**)ref;
  Partial_Reveal_Object* p_obj = *p_ref;
  /* we don't enumerate NULL reference and nos_boundary
     FIXME:: nos_boundary is a static field in GCHelper.java for fast write barrier, not a real object reference
     this should be fixed that magic Address field should not be enumerated. */
#ifdef COMPRESS_REFERENCE
  if (p_obj == (Partial_Reveal_Object*)HEAP_BASE || p_obj == NULL || p_obj == nos_boundary ) return;
#else
  if (p_obj == NULL || p_obj == nos_boundary ) return;
#endif
  assert( !obj_is_marked_in_vt(p_obj));
  assert( address_belongs_to_gc_heap(p_obj, p_global_gc));
  gc_weak_rootset_add_entry(p_global_gc, p_ref, is_short_weak);
}

extern Boolean IGNORE_FORCE_GC;

/* VM to force GC */
void gc_force_gc() 
{
  vm_gc_lock_enum();
  
  if(!IGNORE_FORCE_GC)
    gc_reclaim_heap(p_global_gc, GC_CAUSE_RUNTIME_FORCE_GC);  

  vm_gc_unlock_enum();
}

void* gc_heap_base_address() 
{  return gc_heap_base(p_global_gc); }

void* gc_heap_ceiling_address() 
{  return gc_heap_ceiling(p_global_gc); }

/* this is a contract between vm and gc */
void mutator_initialize(GC* gc, void* tls_gc_info);
void mutator_destruct(GC* gc, void* tls_gc_info); 
void gc_thread_init(void* gc_info)
{  mutator_initialize(p_global_gc, gc_info);  }

void gc_thread_kill(void* gc_info)
{  mutator_destruct(p_global_gc, gc_info);  }

int64 gc_free_memory()
{
#if defined(USE_UNIQUE_MARK_SWEEP_GC)
  return (int64)gc_ms_free_memory_size((GC_MS*)p_global_gc);
#elif defined(USE_UNIQUE_MOVE_COMPACT_GC)
  return (int64)gc_mc_free_memory_size((GC_MC*)p_global_gc);
#else
  return (int64)gc_gen_free_memory_size((GC_Gen*)p_global_gc);
#endif
}

/* java heap size.*/
int64 gc_total_memory() 
{
#if defined(USE_UNIQUE_MARK_SWEEP_GC)
  return (int64)((POINTER_SIZE_INT)gc_ms_total_memory_size((GC_MS*)p_global_gc));
#elif defined(USE_UNIQUE_MOVE_COMPACT_GC)
  return (int64)((POINTER_SIZE_INT)gc_mc_total_memory_size((GC_MC*)p_global_gc));
#else
  return (int64)((POINTER_SIZE_INT)gc_gen_total_memory_size((GC_Gen*)p_global_gc));
#endif
}

int64 gc_max_memory() 
{
#if defined(USE_UNIQUE_MARK_SWEEP_GC)
    return (int64)((POINTER_SIZE_INT)gc_ms_total_memory_size((GC_MS*)p_global_gc));
#elif defined(USE_UNIQUE_MOVE_COMPACT_GC)
    return (int64)((POINTER_SIZE_INT)gc_mc_total_memory_size((GC_MC*)p_global_gc));
#else
    return (int64)((POINTER_SIZE_INT)gc_gen_total_memory_size((GC_Gen*)p_global_gc));
#endif
}

int64 gc_get_collection_count()
{
  GC* gc =  p_global_gc;
  if (gc != NULL) {
    return (int64) gc->num_collections;
  } else {
    return -1;
  }
}

int64 gc_get_collection_time()
{
  GC* gc =  p_global_gc;
  if (gc != NULL) {
    return (int64) gc->time_collections;
  } else {
    return -1;
  }
}

void gc_vm_initialized()
{ return; }

Boolean gc_is_object_pinned (Managed_Object_Handle obj)
{  return 0; }

void gc_pin_object (Managed_Object_Handle* p_object) 
{  return; }

void gc_unpin_object (Managed_Object_Handle* p_object) 
{  return; }

Managed_Object_Handle gc_get_next_live_object(void *iterator) 
{  assert(0); return NULL; }

unsigned int gc_time_since_last_gc()
{  assert(0); return 0; }

#ifndef USE_32BITS_HASHCODE
#define GCGEN_HASH_MASK 0x1fc
I_32 gc_get_hashcode(Managed_Object_Handle p_object) 
{  
   Partial_Reveal_Object *obj = (Partial_Reveal_Object *)p_object;
   if(!obj) return 0;
   assert(address_belongs_to_gc_heap(obj, p_global_gc));
   Obj_Info_Type info = get_obj_info_raw(obj);
   int hash = (int)(info & GCGEN_HASH_MASK);
   if (!hash) {
       hash = (int)((((POINTER_SIZE_INT)obj) >> 3) & GCGEN_HASH_MASK);
       if(!hash)  hash = (0x173 & GCGEN_HASH_MASK);
       POINTER_SIZE_INT new_info = info | hash;
       while (true) {
         Obj_Info_Type temp =
           atomic_casptrsz((volatile POINTER_SIZE_INT*)(&obj->obj_info), new_info, info);
         if (temp == info) break;
         info = get_obj_info_raw(obj);
         new_info = info | hash;
       }
   }
   return hash;
}
#else //USE_32BITS_HASHCODE
I_32 gc_get_hashcode(Managed_Object_Handle p_object)
{
#if defined(USE_UNIQUE_MARK_SWEEP_GC) || defined(USE_UNIQUE_MOVE_COMPACT_GC)
  return (I_32)0;//p_object;
#endif

  Partial_Reveal_Object* p_obj = (Partial_Reveal_Object*)p_object;
  assert(address_belongs_to_gc_heap(p_obj, p_global_gc));
  Obj_Info_Type info = get_obj_info_raw(p_obj);
  int hash;
  unsigned int infoMask = (unsigned int)(info & HASHCODE_MASK);
  if (infoMask == HASHCODE_SET_BUFFERED)          hash = obj_lookup_hashcode_in_buf(p_obj);
  else if (infoMask == HASHCODE_SET_UNALLOCATED)  hash = hashcode_gen((void*)p_obj);
  else if (infoMask == HASHCODE_SET_ATTACHED)     hash = *(int*) ((unsigned char *)p_obj + vm_object_size(p_obj));
  else if  (infoMask == HASHCODE_UNSET) {
      Obj_Info_Type new_info = info | HASHCODE_SET_BIT;
      while (true) {
        Obj_Info_Type temp =
          atomic_casptrsz((volatile POINTER_SIZE_INT*)(&p_obj->obj_info), new_info, info);
        if (temp == info) break;
        info = get_obj_info_raw(p_obj);
        new_info = info | HASHCODE_SET_BIT;
      }
      hash = hashcode_gen((void*)p_obj);
  }    else                                       assert(0);

  return hash;
}
#endif //USE_32BITS_HASHCODE

void gc_finalize_on_exit()
{
  if(!IGNORE_FINREF )
    put_all_fin_on_exit(p_global_gc);
}

/* for future use
 * void gc_phantom_ref_enqueue_hook(void *p_reference)
 * {
 *   if(special_reference_type((Partial_Reveal_Object *)p_reference) == PHANTOM_REFERENCE){
 *     Partial_Reveal_Object **p_referent_field = obj_get_referent_field(p_reference);
 *     *p_referent_field = (Partial_Reveal_Object *)((unsigned int)*p_referent_field | PHANTOM_REF_ENQUEUED_MASK | ~PHANTOM_REF_PENDING_MASK);
 *   }
 * }
 */

extern Boolean JVMTI_HEAP_ITERATION;
void gc_iterate_heap() {
    // data structures in not consistent for heap iteration
    if (!JVMTI_HEAP_ITERATION) return;

#if defined(USE_UNIQUE_MARK_SWEEP_GC)
  gc_ms_iterate_heap((GC_MS*)p_global_gc);
#elif defined(USE_UNIQUE_MOVE_COMPACT_GC)
  gc_mc_iterate_heap((GC_MC*)p_global_gc);
#else
  gc_gen_iterate_heap((GC_Gen *)p_global_gc);
#endif
}

void gc_set_mutator_block_flag()
{  mutator_need_block = TRUE; }

Boolean gc_clear_mutator_block_flag()
{
  Boolean old_flag = mutator_need_block;
  mutator_need_block = FALSE;
  return old_flag;
}

Boolean obj_belongs_to_gc_heap(Partial_Reveal_Object* p_obj)
{
  return address_belongs_to_gc_heap(p_obj, p_global_gc);  
}




