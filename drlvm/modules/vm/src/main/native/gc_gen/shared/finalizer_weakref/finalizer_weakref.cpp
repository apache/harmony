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
 * @author Li-Gang Wang, 2006/11/29
 */

#include "open/types.h"
#include "open/vm_gc.h"
#include "finalizer_weakref.h"
#include "../thread/mutator.h"
#include "../common/gc_metadata.h"
#include "../trace_forward/fspace.h"
#include "../los/lspace.h"
#include "../gen/gen.h"
#include "../mark_sweep/gc_ms.h"
#include "../common/space_tuner.h"
#include "../common/compressed_ref.h"
#include "../common/object_status.h"
#include "../common/gc_concurrent.h"

Boolean IGNORE_FINREF = FALSE;
Boolean DURING_RESURRECTION = FALSE;

static void finref_add_repset_from_pool(GC *gc, Pool *pool)
{
  finref_reset_repset(gc);
  pool_iterator_init(pool);
  Vector_Block *block = pool_iterator_next(pool);
  while(block){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(block);
    for(; !vector_block_iterator_end(block, iter); iter = vector_block_iterator_advance(block, iter)){
      REF *p_ref = (REF*)iter;
      Partial_Reveal_Object *p_obj = read_slot(p_ref);
      if(*p_ref && obj_need_move(gc, p_obj))
        finref_repset_add_entry(gc, p_ref);
    }
    block = pool_iterator_next(pool);
  }
  finref_put_repset(gc);
}

static inline void fallback_update_fw_ref(REF *p_ref)
{
  assert(collect_is_fallback());
  
  Partial_Reveal_Object *p_obj = read_slot(p_ref);
  if(obj_belongs_to_nos(p_obj) && obj_is_fw_in_oi(p_obj)){
    assert(!obj_is_marked_in_vt(p_obj));
    assert(obj_get_vt(p_obj) == obj_get_vt(obj_get_fw_in_oi(p_obj)));
    p_obj = obj_get_fw_in_oi(p_obj);
    assert(p_obj);
    write_slot(p_ref, p_obj);
  }
}

static void identify_finalizable_objects(Collector *collector)
{
  GC *gc = collector->gc;
  Finref_Metadata *metadata = gc->finref_metadata;
  Pool *obj_with_fin_pool = metadata->obj_with_fin_pool;
  
  gc_reset_finalizable_objects(gc);
  pool_iterator_init(obj_with_fin_pool);
  Vector_Block *block = pool_iterator_next(obj_with_fin_pool);
  while(block){
    unsigned int block_has_ref = 0;
    POINTER_SIZE_INT *iter = vector_block_iterator_init(block);
    for(; !vector_block_iterator_end(block, iter); iter = vector_block_iterator_advance(block, iter)){
      REF *p_ref = (REF*)iter;
      if(collect_is_fallback())
        fallback_update_fw_ref(p_ref);  // in case that this collection is ALGO_MAJOR_FALLBACK
      Partial_Reveal_Object *p_obj = read_slot(p_ref);
      if(!p_obj)
        continue;
      if(gc_obj_is_dead(gc, p_obj)){
        gc_add_finalizable_obj(gc, p_obj);
        *p_ref = (REF)NULL;
      } else {
        if(collect_is_minor() && obj_need_move(gc, p_obj)){
          assert(obj_is_fw_in_oi(p_obj));
          write_slot(p_ref, obj_get_fw_in_oi(p_obj));
        }
        ++block_has_ref;
      }
    }
    if(!block_has_ref)
      vector_block_clear(block);
    
    block = pool_iterator_next(obj_with_fin_pool);
  }
  gc_put_finalizable_objects(gc);
  
  if(collect_need_update_repset())
    finref_add_repset_from_pool(gc, obj_with_fin_pool);
}

extern void trace_obj_in_gen_fw(Collector *collector, void *p_ref);
extern void trace_obj_in_nongen_fw(Collector *collector, void *p_ref);
extern void trace_obj_in_gen_ss(Collector *collector, void *p_ref);
extern void trace_obj_in_nongen_ss(Collector *collector, void *p_ref);
extern void trace_obj_in_normal_marking(Collector *collector, void *p_obj);
extern void trace_obj_in_fallback_marking(Collector *collector, void *p_ref);
extern void trace_obj_in_ms_fallback_marking(Collector *collector, void *p_ref);
extern void trace_obj_in_space_tune_marking(Collector *collector, void *p_obj);
extern void trace_obj_in_ms_marking(Collector *collector, void *p_obj);
extern void trace_obj_in_ms_concurrent_mark(Collector *collector, void *p_obj);


typedef void (* Trace_Object_Func)(Collector *collector, void *p_ref_or_obj);

// Resurrect the obj tree whose root is the obj which p_ref points to
static inline void resurrect_obj_tree(Collector *collector, REF *p_ref)
{
  GC *gc = collector->gc;
  GC_Metadata *metadata = gc->metadata;
  Partial_Reveal_Object *p_obj = read_slot(p_ref);
  assert(p_obj && gc_obj_is_dead(gc, p_obj));
  
  void *p_ref_or_obj = p_ref;
  Trace_Object_Func trace_object;
  
  /* set trace_object() function */
  if(collect_is_minor()){
    if(gc_is_gen_mode()){
      if(minor_is_forward())
        trace_object = trace_obj_in_gen_fw;
      else if(minor_is_semispace())
        trace_object = trace_obj_in_gen_ss;
      else 
        assert(0);
    }else{
      if(minor_is_forward())
        trace_object = trace_obj_in_nongen_fw;
      else if(minor_is_semispace())
        trace_object = trace_obj_in_nongen_ss;
      else 
        assert(0);
    }
  } else if(collect_is_major_normal() || !gc_has_nos()){
    p_ref_or_obj = p_obj;
    if(gc_has_space_tuner(gc) && (gc->tuner->kind != TRANS_NOTHING)){
      trace_object = trace_obj_in_space_tune_marking;
      unsigned int obj_size = vm_object_size(p_obj);
#ifdef USE_32BITS_HASHCODE
      obj_size += hashcode_is_set(p_obj) ? GC_OBJECT_ALIGNMENT : 0;
#endif
      if(!obj_belongs_to_space(p_obj, gc_get_los((GC_Gen*)gc))){
        collector->non_los_live_obj_size += obj_size;
        collector->segment_live_size[SIZE_TO_SEGMENT_INDEX(obj_size)] += obj_size;
      } else {
        collector->los_live_obj_size += round_up_to_size(obj_size, KB); 
      }
    } else if(!gc_has_nos()){
      trace_object = trace_obj_in_ms_marking;
    } else {
      trace_object = trace_obj_in_normal_marking;
    }
  } else if(collect_is_fallback()){
    if(major_is_marksweep())
      trace_object = trace_obj_in_ms_fallback_marking;
    else
      trace_object = trace_obj_in_fallback_marking;
  } else {
    assert(major_is_marksweep());
    p_ref_or_obj = p_obj;
   if( gc->gc_concurrent_status == GC_CON_NIL ) 
      trace_object = trace_obj_in_ms_marking;
    else
      trace_object = trace_obj_in_ms_concurrent_mark;
  }
  
  collector->trace_stack = free_task_pool_get_entry(metadata);
  collector_tracestack_push(collector, p_ref_or_obj);
  pool_put_entry(metadata->mark_task_pool, collector->trace_stack);
  
  collector->trace_stack = free_task_pool_get_entry(metadata);
  Vector_Block *task_block = pool_get_entry(metadata->mark_task_pool);
  while(task_block){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(task_block);
    while(!vector_block_iterator_end(task_block, iter)){
      void *p_ref_or_obj = (void*)*iter;
      assert(((collect_is_minor()||collect_is_fallback()) && *(Partial_Reveal_Object **)p_ref_or_obj)
              || ((collect_is_major_normal()||major_is_marksweep()||!gc_has_nos()) && p_ref_or_obj));
      trace_object(collector, p_ref_or_obj);
      if(collector->result == FALSE)  break; /* Resurrection fallback happens; force return */
      
      iter = vector_block_iterator_advance(task_block, iter);
    }
    vector_stack_clear(task_block);
    pool_put_entry(metadata->free_task_pool, task_block);
    
    if(collector->result == FALSE){
      gc_task_pool_clear(metadata->mark_task_pool);
      break; /* force return */
    }
    
    task_block = pool_get_entry(metadata->mark_task_pool);
  }
  
  task_block = (Vector_Block*)collector->trace_stack;
  vector_stack_clear(task_block);
  pool_put_entry(metadata->free_task_pool, task_block);
  collector->trace_stack = NULL;
}

static void resurrect_finalizable_objects(Collector *collector)
{
  GC *gc = collector->gc;
  Finref_Metadata *metadata = gc->finref_metadata;
  Pool *finalizable_obj_pool = metadata->finalizable_obj_pool;
  
  if(finalizable_obj_pool_is_empty(gc))
    return;
  
  DURING_RESURRECTION = TRUE;
  
  pool_iterator_init(finalizable_obj_pool);
  Vector_Block *block = pool_iterator_next(finalizable_obj_pool);
  while(block){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(block);
    for(; !vector_block_iterator_end(block, iter); iter = vector_block_iterator_advance(block, iter)){
      REF *p_ref = (REF*)iter;
      Partial_Reveal_Object *p_obj = read_slot(p_ref);
      assert(p_obj);
      
      /* Perhaps obj has been resurrected by previous resurrections */
      if(!gc_obj_is_dead(gc, p_obj)){
        if(collect_is_minor() && obj_need_move(gc, p_obj))
          write_slot(p_ref, obj_get_fw_in_oi(p_obj));
        continue;
      }
      
      resurrect_obj_tree(collector, p_ref);
      if(collector->result == FALSE){
        /* Resurrection fallback happens */
        assert(collect_is_minor());
        return; /* force return */
      }
    }
    
    block = pool_iterator_next(finalizable_obj_pool);
  }
  
  /* In major & fallback & sweep-compact collection we need record p_ref of the root dead obj to update it later.
   * Because it is outside heap, we can't update it in ref fixing.
   * In minor collection p_ref of the root dead obj is automatically updated while tracing.
   */
  if(collect_need_update_repset())
    finref_add_repset_from_pool(gc, finalizable_obj_pool);
  metadata->pending_finalizers = TRUE;
  
  DURING_RESURRECTION = FALSE;
  
  /* fianlizable objs have been added to finref repset pool or updated by tracing */
}

static void identify_dead_refs(GC *gc, Pool *pool)
{
  if(collect_need_update_repset())
    finref_reset_repset(gc);

  pool_iterator_init(pool);
  Vector_Block *block = pool_iterator_next(pool);
  while(block){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(block);
    for(; !vector_block_iterator_end(block, iter); iter = vector_block_iterator_advance(block, iter)){
      REF *p_ref = (REF*)iter;
      Partial_Reveal_Object *p_obj = read_slot(p_ref);
      assert(p_obj);
      REF *p_referent_field = obj_get_referent_field(p_obj);
      if(collect_is_fallback())
        fallback_update_fw_ref(p_referent_field);
        
      Partial_Reveal_Object *p_referent = read_slot(p_referent_field);
      
      if(!p_referent){  
        /* referent field has been cleared. I forgot why we set p_ref with NULL here. 
           I guess it's because this ref_obj was processed in abother p_ref already, so
           there is no need to keep same ref_obj in this p_ref. */
        *p_ref = (REF)NULL;
        continue;
      }
      if(!gc_obj_is_dead(gc, p_referent)){  // referent is alive
        if(obj_need_move(gc, p_referent)){
          if(collect_is_minor()){
            assert(obj_is_fw_in_oi(p_referent));
            Partial_Reveal_Object* p_new_referent = obj_get_fw_in_oi(p_referent);
            write_slot(p_referent_field, p_new_referent);
            /* if it's gen mode, and referent stays in NOS, we need keep p_referent_field in collector remset.
               This leads to the ref obj live even it is actually only weakly-reachable in next gen-mode collection. 
               This simplifies the design. Otherwise, we need remember the refobj in MOS seperately and process them seperately. */
            if(gc_is_gen_mode())
              if(addr_belongs_to_nos(p_new_referent) && !addr_belongs_to_nos(p_obj))
                collector_remset_add_entry(gc->collectors[0], ( Partial_Reveal_Object**)p_referent_field); 

          } else{ // if(collect_move_object()){ the condition is redundant because obj_need_move already checks 
            finref_repset_add_entry(gc, p_referent_field);
          }
        }
        *p_ref = (REF)NULL;
      }else{
	      /* else, the referent is dead (weakly reachable), clear the referent field */
	      *p_referent_field = (REF)NULL; 
	      /* for dead referent, p_ref is not set NULL. p_ref keeps the ref object, which
	         will be moved to VM for enqueueing. */
      }
    }/* for each ref object */
    
    block = pool_iterator_next(pool);
  }
  
  if(collect_need_update_repset()){
    finref_put_repset(gc);
    finref_add_repset_from_pool(gc, pool);
  }
}

static void identify_dead_softrefs(Collector *collector)
{
  GC *gc = collector->gc;
  if(collect_is_minor()){
    assert(softref_pool_is_empty(gc));
    return;
  }
  
  Pool *softref_pool = gc->finref_metadata->softref_pool;
  identify_dead_refs(gc, softref_pool);
}

static void identify_dead_weakrefs(Collector *collector)
{
  GC *gc = collector->gc;
  Pool *weakref_pool = gc->finref_metadata->weakref_pool;
  
  identify_dead_refs(gc, weakref_pool);
}

/*
 * The reason why we don't use identify_dead_refs() to implement this function is
 * that we will differentiate phanref from weakref in the future.
 */
static void identify_dead_phanrefs(Collector *collector)
{
  GC *gc = collector->gc;
  Finref_Metadata *metadata = gc->finref_metadata;
  Pool *phanref_pool = metadata->phanref_pool;
  
  if(collect_need_update_repset())
    finref_reset_repset(gc);
//  collector_reset_repset(collector);
  pool_iterator_init(phanref_pool);
  Vector_Block *block = pool_iterator_next(phanref_pool);
  while(block){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(block);
    for(; !vector_block_iterator_end(block, iter); iter = vector_block_iterator_advance(block, iter)){
      Partial_Reveal_Object **p_ref = (Partial_Reveal_Object **)iter;
      Partial_Reveal_Object *p_obj = read_slot((REF*)p_ref);
      assert(p_obj);
      REF *p_referent_field = obj_get_referent_field(p_obj);
      if(collect_is_fallback())
        fallback_update_fw_ref(p_referent_field);

      Partial_Reveal_Object *p_referent = read_slot(p_referent_field);      
      if(!p_referent){  // referent field has been cleared
        *p_ref = NULL;
        continue;
      }
      if(!gc_obj_is_dead(gc, p_referent)){  // referent is alive
        if(obj_need_move(gc, p_referent)){
          if(collect_is_minor()){
            assert(obj_is_fw_in_oi(p_referent));
            Partial_Reveal_Object* p_new_referent = obj_get_fw_in_oi(p_referent);
            write_slot(p_referent_field, p_new_referent);
            if(gc_is_gen_mode())
              if(addr_belongs_to_nos(p_new_referent) && !addr_belongs_to_nos(p_obj))
                collector_remset_add_entry(gc->collectors[0], ( Partial_Reveal_Object**)p_referent_field); 

          } else{ // if(collect_move_object()){ this check is redundant because obj_need_move checks
            finref_repset_add_entry(gc, p_referent_field);
          }
        }
        *p_ref = (REF)NULL;
        continue;
      }
      *p_referent_field = (REF)NULL;
      /* Phantom status: for future use
       * if((unsigned int)p_referent & PHANTOM_REF_ENQUEUE_STATUS_MASK){
       *   // enqueued but not explicitly cleared OR pending for enqueueing
       *   *iter = NULL;
       * }
       * resurrect_obj_tree(collector, p_referent_field);
       */
    }
    block = pool_iterator_next(phanref_pool);
  }
//  collector_put_repset(collector);
  if(collect_need_update_repset()){
    finref_put_repset(gc);
    finref_add_repset_from_pool(gc, phanref_pool);
  }
}

static void put_finalizable_obj_to_vm(GC *gc)
{
  Pool *finalizable_obj_pool = gc->finref_metadata->finalizable_obj_pool;
  Pool *free_pool = gc->finref_metadata->free_pool;
  
  Vector_Block *block = pool_get_entry(finalizable_obj_pool);
  while(block){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(block);
    while(!vector_block_iterator_end(block, iter)){
      assert(*iter);
      Managed_Object_Handle p_obj = (Managed_Object_Handle)read_slot((REF*)iter);
      vm_finalize_object(p_obj);
      iter = vector_block_iterator_advance(block, iter);
    }
    vector_block_clear(block);
    pool_put_entry(free_pool, block);
    block = pool_get_entry(finalizable_obj_pool);
  }
}

static inline void put_dead_weak_refs_to_vm(GC *gc, Pool *ref_pool)
{
  Pool *free_pool = gc->finref_metadata->free_pool;
  
  Vector_Block *block = pool_get_entry(ref_pool);
  while(block){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(block);
    while(!vector_block_iterator_end(block, iter)){
      Managed_Object_Handle p_obj = (Managed_Object_Handle)read_slot((REF*)iter);
      if(p_obj)
        vm_enqueue_reference(p_obj);
      iter = vector_block_iterator_advance(block, iter);
    }
    vector_block_clear(block);
    pool_put_entry(free_pool, block);
    block = pool_get_entry(ref_pool);
  }
}

static void put_dead_refs_to_vm(GC *gc)
{
  Finref_Metadata *metadata = gc->finref_metadata;
  
  if(softref_pool_is_empty(gc)
      && weakref_pool_is_empty(gc)
      && phanref_pool_is_empty(gc)
      && pool_is_empty(metadata->fallback_ref_pool)){
    gc_clear_weakref_pools(gc);
    return;
  }
  
  put_dead_weak_refs_to_vm(gc, metadata->softref_pool);
  put_dead_weak_refs_to_vm(gc, metadata->weakref_pool);
  put_dead_weak_refs_to_vm(gc, metadata->phanref_pool);
  
  /* This is a major collection after resurrection fallback */
  if(!pool_is_empty(metadata->fallback_ref_pool)){
    put_dead_weak_refs_to_vm(gc, metadata->fallback_ref_pool);
  }
  
  metadata->pending_weakrefs = TRUE;
}

/* Finalizable objs falls back to objs with fin when resurrection fallback happens */
static void finalizable_objs_fallback(GC *gc)
{
  Finref_Metadata *metadata = gc->finref_metadata;
  Pool *finalizable_obj_pool = metadata->finalizable_obj_pool;
  Pool *obj_with_fin_pool = metadata->obj_with_fin_pool;
  Vector_Block *obj_with_fin_block = finref_get_free_block(gc);
    
  Vector_Block *block = pool_get_entry(finalizable_obj_pool);
  while(block){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(block);
    for(; !vector_block_iterator_end(block, iter); iter = vector_block_iterator_advance(block, iter)){
      REF *p_ref = (REF*)iter;
      Partial_Reveal_Object *p_obj = read_slot(p_ref);
      assert(p_obj);
      /* Perhaps obj has been resurrected by previous resurrections. If the fin-obj was resurrected, we need put it back to obj_with_fin pool.
         For minor collection, the resurrected obj was forwarded, so we need use the new copy.*/
      if(!gc_obj_is_dead(gc, p_obj) && obj_belongs_to_nos(p_obj)){
        /* Even in NOS, not all live objects are forwarded due to the partial-forward algortihm */ 
        if(!NOS_PARTIAL_FORWARD || fspace_obj_to_be_forwarded(p_obj)){
          write_slot(p_ref , obj_get_fw_in_oi(p_obj));
          p_obj = read_slot(p_ref);
        }
      }
      /* Perhaps obj_with_fin_block has been allocated with a new free block if it is full */
      obj_with_fin_block = gc_add_finalizer(gc, obj_with_fin_block, p_obj);
    }
    block = pool_get_entry(finalizable_obj_pool);
  }
  
  pool_put_entry(obj_with_fin_pool, obj_with_fin_block);
  metadata->pending_finalizers = FALSE;
}

static void dead_weak_refs_fallback(GC *gc, Pool *ref_pool)
{
  Finref_Metadata *metadata = gc->finref_metadata;
  Pool *free_pool = metadata->free_pool;
  Pool *fallback_ref_pool = metadata->fallback_ref_pool;
  
  Vector_Block *fallback_ref_block = finref_get_free_block(gc);
  Vector_Block *block = pool_get_entry(ref_pool);
  while(block){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(block);
    while(!vector_block_iterator_end(block, iter)){
      Partial_Reveal_Object *p_obj = read_slot((REF*)iter);
      /* Perhaps fallback_ref_block has been allocated with a new free block if it is full */
      if(p_obj)
        fallback_ref_block = finref_add_fallback_ref(gc, fallback_ref_block, p_obj);
      iter = vector_block_iterator_advance(block, iter);
    }
    vector_block_clear(block);
    pool_put_entry(free_pool, block);
    block = pool_get_entry(ref_pool);
  }
  
  pool_put_entry(fallback_ref_pool, fallback_ref_block);
}

/* Record softrefs and weakrefs whose referents are dead
 * so that we can update their addr and put them to VM.
 * In fallback collection these refs will not be considered for enqueueing again,
 * since their referent fields have been cleared by identify_dead_refs().
 */
static void dead_refs_fallback(GC *gc)
{
  Finref_Metadata *metadata = gc->finref_metadata;
  
  if(!softref_pool_is_empty(gc) || !weakref_pool_is_empty(gc))
    metadata->pending_weakrefs = TRUE;
  
  /* We only use fallback_ref_pool in resurrection fallback so it must be empty */
  assert(pool_is_empty(metadata->fallback_ref_pool));
  
  dead_weak_refs_fallback(gc, metadata->softref_pool);
  dead_weak_refs_fallback(gc, metadata->weakref_pool);
  
  gc_clear_weakref_pools(gc);
}

/* Deal with resurrection fallback */
static void resurrection_fallback_handler(GC *gc)
{
  Finref_Metadata *metadata = gc->finref_metadata;
  
  /* Repset pool should be empty, because we don't add anything to this pool in Minor Collection. */
  assert(pool_is_empty(metadata->repset_pool));
  
  finalizable_objs_fallback(gc);
  dead_refs_fallback(gc);
  
  assert(pool_is_empty(metadata->finalizable_obj_pool));
  assert(pool_is_empty(metadata->softref_pool));
  assert(pool_is_empty(metadata->weakref_pool));
  assert(pool_is_empty(metadata->phanref_pool));
  
  assert(metadata->finalizable_obj_set == NULL);
  assert(metadata->repset == NULL);
}

void collector_identify_finref(Collector *collector)
{
  GC *gc = collector->gc;
  
  gc_set_weakref_sets(gc);
  identify_dead_softrefs(collector);
  identify_dead_weakrefs(collector);
  identify_finalizable_objects(collector);
  resurrect_finalizable_objects(collector);
  gc->collect_result = gc_collection_result(gc);
  if(!gc->collect_result){
    assert(collect_is_minor());
    resurrection_fallback_handler(gc);
    return;
  }
  identify_dead_phanrefs(collector);
}

void fallback_finref_cleanup(GC *gc)
{
  gc_set_weakref_sets(gc);
  gc_clear_weakref_pools(gc);
}

void gc_put_finref_to_vm(GC *gc)
{
  put_dead_refs_to_vm(gc);
  put_finalizable_obj_to_vm(gc);
}

void put_all_fin_on_exit(GC *gc)
{
  Pool *obj_with_fin_pool = gc->finref_metadata->obj_with_fin_pool;
  Pool *free_pool = gc->finref_metadata->free_pool;
  
  /* Because we are manipulating obj_with_fin_pool, GC lock must be hold in case that GC happens */
  vm_gc_lock_enum();
  /* FIXME: holding gc lock is not enough, perhaps there are mutators that are allocating objects with finalizer
   * could be fixed as this:
   * in fspace_alloc() and lspace_alloc() hold gc lock through
   * allocating mem and adding the objects with finalizer to the pool
   */
  lock(gc->mutator_list_lock);
  gc_set_obj_with_fin(gc);
  unlock(gc->mutator_list_lock);
  
  Vector_Block *block = pool_get_entry(obj_with_fin_pool);
  while(block){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(block);
    while(!vector_block_iterator_end(block, iter)){
      Managed_Object_Handle p_obj = (Managed_Object_Handle)read_slot((REF*)iter);
      if(p_obj)
        vm_finalize_object(p_obj);
      iter = vector_block_iterator_advance(block, iter);
    }
    vector_block_clear(block);
    pool_put_entry(free_pool, block);
    block = pool_get_entry(obj_with_fin_pool);
  }
  
  vm_gc_unlock_enum();
}

static void update_referent_field_ignore_finref(GC *gc, Pool *pool)
{
  Vector_Block *block = pool_get_entry(pool);
  while(block){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(block);
    for(; !vector_block_iterator_end(block, iter); iter = vector_block_iterator_advance(block, iter)){
      REF *p_ref = (REF*)iter;
      Partial_Reveal_Object *p_obj = read_slot(p_ref);
      assert(p_obj);
      REF *p_referent_field = obj_get_referent_field(p_obj);
      if(collect_is_fallback())
        fallback_update_fw_ref(p_referent_field);
        
      Partial_Reveal_Object *p_referent = read_slot(p_referent_field);      
      if(!p_referent){  // referent field has been cleared
        *p_ref = (REF)NULL;
        continue;
      }
      if(!gc_obj_is_dead(gc, p_referent)){  // referent is alive
        if(obj_need_move(gc, p_referent))
          if(collect_is_minor()){
            assert(obj_is_fw_in_oi(p_referent));
            Partial_Reveal_Object* p_new_referent = obj_get_fw_in_oi(p_referent);
            write_slot(p_referent_field, p_new_referent);
            if(gc_is_gen_mode())
              if(addr_belongs_to_nos(p_new_referent) && !addr_belongs_to_nos(p_obj))
                collector_remset_add_entry(gc->collectors[0], ( Partial_Reveal_Object**)p_referent_field); 

          } else {
            finref_repset_add_entry(gc, p_referent_field);
          }
        *p_ref = (REF)NULL;
        continue;
      }
      *p_referent_field = (REF)NULL; /* referent is weakly reachable: clear the referent field */
    }
    block = pool_get_entry(pool);
  }
}

void gc_update_weakref_ignore_finref(GC *gc)
{
  Finref_Metadata *metadata = gc->finref_metadata;
  
  if(collect_need_update_repset())
    finref_reset_repset(gc);
  if(collect_move_object()){
    update_referent_field_ignore_finref(gc, metadata->softref_pool);
    update_referent_field_ignore_finref(gc, metadata->weakref_pool);
    update_referent_field_ignore_finref(gc, metadata->phanref_pool);
  }
  if(collect_need_update_repset())
    finref_put_repset(gc);
}

extern void *los_boundary;
/* Move compaction needs special treament when updating referent field */
static inline void move_compaction_update_ref(GC *gc, REF *p_ref)
{
  /* There are only two kinds of p_ref being added into finref_repset_pool:
   * 1. p_ref is in a vector block from one finref pool;
   * 2. p_ref is a referent field.
   * So if p_ref belongs to heap, it must be a referent field pointer.
   * Objects except a tree root which are resurrected need not be recorded in finref_repset_pool.
   */
  if(address_belongs_to_gc_heap(p_ref, gc) && (p_ref >= los_boundary)){
    unsigned int offset = get_gc_referent_offset();
    Partial_Reveal_Object *p_old_ref = (Partial_Reveal_Object*)((POINTER_SIZE_INT)p_ref - offset);
    Partial_Reveal_Object *p_new_ref = obj_get_fw_in_table(p_old_ref);
    p_ref = (REF*)((POINTER_SIZE_INT)p_new_ref + offset);
  }
  Partial_Reveal_Object *p_obj = read_slot(p_ref);
  assert(space_of_addr(gc, p_obj)->move_object);
  
  if(p_obj < los_boundary)
    p_obj = obj_get_fw_in_oi(p_obj);
  else
    p_obj = obj_get_fw_in_table(p_obj);

  write_slot(p_ref, p_obj);
}

/* In two cases mark-sweep needs fixing repointed refs:
 * 1. ms with compaction
 * 2. ms as a mos collection algorithm
 */
static inline void moving_mark_sweep_update_ref(GC *gc, REF *p_ref, Boolean double_fix)
{
  /* There are only two kinds of p_ref being added into finref_repset_pool:
   * 1. p_ref is in a vector block from one finref pool;
   * 2. p_ref is a referent field.
   * So if p_ref belongs to heap, it must be a referent field pointer.
   * Objects except a tree root which are resurrected need not be recorded in finref_repset_pool.
   */
  if(address_belongs_to_gc_heap((void*)p_ref, gc)){
    unsigned int offset = get_gc_referent_offset();
    Partial_Reveal_Object *p_old_ref = (Partial_Reveal_Object*)((POINTER_SIZE_INT)p_ref - offset);
    if(obj_is_fw_in_oi(p_old_ref)){
      Partial_Reveal_Object *p_new_ref = obj_get_fw_in_oi(p_old_ref);
      /* Only major collection in MS Gen GC might need double_fix.
       * Double fixing happens when both forwarding and compaction happen.
       */
      if(double_fix && obj_is_fw_in_oi(p_new_ref)){
        assert(major_is_marksweep());
        p_new_ref = obj_get_fw_in_oi(p_new_ref);
        assert(address_belongs_to_gc_heap(p_new_ref, gc));
      }
      p_ref = (REF*)((POINTER_SIZE_INT)p_new_ref + offset);
    }
  }
  Partial_Reveal_Object *p_obj = read_slot(p_ref);
  /* assert(obj_need_move(gc, p_obj));
   * This assertion is commented out because it assert(!obj_is_dead(gc, p_obj)).
   * When gc_fix_rootset is invoked, mark bit and alloc bit have been flipped in Mark-Sweep,
   * so this assertion will fail.
   * But for sure p_obj here must be an one needing moving.
   */
  p_obj = obj_get_fw_in_oi(p_obj);
  /* Only major collection in MS Gen GC might need double_fix.
   * Double fixing happens when both forwarding and compaction happen.
   */
  if(double_fix && obj_is_fw_in_oi(p_obj)){
    assert(major_is_marksweep());
    p_obj = obj_get_fw_in_oi(p_obj);
    assert(address_belongs_to_gc_heap(p_obj, gc));
  }
  write_slot(p_ref, p_obj);
}

/* only called in non-minor collection. parameter pointer_addr_in_pool means it is p_ref or p_obj in pool*/
static void nondestructively_fix_finref_pool(GC *gc, Pool *pool, Boolean pointer_addr_in_pool, Boolean double_fix)
{
  Finref_Metadata *metadata = gc->finref_metadata;
  REF *p_ref;
  Partial_Reveal_Object *p_obj;
  
  /* NOTE:: this is nondestructive to the root sets. */
  pool_iterator_init(pool);
  Vector_Block *repset = pool_iterator_next(pool);
  while(repset){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(repset);
    for(; !vector_block_iterator_end(repset,iter); iter = vector_block_iterator_advance(repset,iter)){
      if(pointer_addr_in_pool)
        p_ref = (REF*)*iter;
      else
        p_ref = (REF*)iter;
      p_obj = read_slot(p_ref);
      
      if(collect_is_compact_move()){ /* include both unique move-compact and major move-compact */
        move_compaction_update_ref(gc, p_ref);
      } else if(collect_is_ms_compact()){
        if(obj_is_fw_in_oi(p_obj))
          moving_mark_sweep_update_ref(gc, p_ref, double_fix);
      } else { /* major slide compact */
        assert((obj_is_marked_in_vt(p_obj) && obj_is_fw_in_oi(p_obj)));
        write_slot(p_ref , obj_get_fw_in_oi(p_obj));
      }
    }
    repset = pool_iterator_next(pool);
  }
}

void gc_update_finref_repointed_refs(GC *gc, Boolean double_fix)
{
  assert(!collect_is_minor());
  
  Finref_Metadata *metadata = gc->finref_metadata;
  Pool *repset_pool = metadata->repset_pool;
  Pool *fallback_ref_pool = metadata->fallback_ref_pool;
  
  nondestructively_fix_finref_pool(gc, repset_pool, TRUE, double_fix);
  if(!pool_is_empty(fallback_ref_pool)){
    assert(collect_is_fallback());
    nondestructively_fix_finref_pool(gc, fallback_ref_pool, FALSE, double_fix);
  }
}

void gc_activate_finref_threads(GC *gc)
{
  Finref_Metadata* metadata = gc->finref_metadata;
  
  if(metadata->pending_finalizers || metadata->pending_weakrefs){
    metadata->pending_finalizers = FALSE;
    metadata->pending_weakrefs = FALSE;
    vm_hint_finalize();
  }
}

static void finref_copy_pool_to_rootset(GC *gc, Pool *src_pool)
{
  pool_iterator_init(src_pool);
  while(Vector_Block *root_set = pool_iterator_next(src_pool)){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(root_set);
    while(!vector_block_iterator_end(root_set, iter)){
      gc_compressed_rootset_add_entry(gc, (REF*)iter);
      iter = vector_block_iterator_advance(root_set, iter);
    }
  }
}

void gc_copy_finaliable_obj_to_rootset(GC *gc)
{
  Pool *finalizable_obj_pool = gc->finref_metadata->finalizable_obj_pool;
  Pool *finalizable_obj_pool_copy = gc->finref_metadata->finalizable_obj_pool_copy;
  Pool *free_pool = gc->metadata->gc_rootset_pool;
  finref_metadata_clear_pool(finalizable_obj_pool_copy);
  finref_copy_pool(finalizable_obj_pool, finalizable_obj_pool_copy, gc);
  finref_copy_pool_to_rootset(gc, finalizable_obj_pool_copy);
}






