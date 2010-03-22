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

#include "../common/gc_metadata.h"
#include "../thread/collector.h"
#include "../gen/gen.h"
#include "../finalizer_weakref/finalizer_weakref.h"

#ifdef GC_GEN_STATS
#include "../gen/gen_stats.h"
#endif
static void scan_slot(Collector* collector, REF *p_ref)
{
  if( read_slot(p_ref) == NULL) return;

  collector_tracestack_push(collector, p_ref);
  return;
}

static void scan_object(Collector* collector, REF *p_ref)
{
  Partial_Reveal_Object *p_obj = read_slot(p_ref);
  assert(p_obj);
  assert((((POINTER_SIZE_INT)p_obj) % GC_OBJECT_ALIGNMENT) == 0);

  Partial_Reveal_VTable *vtable = decode_vt(obj_get_vt(p_obj));
  if(TRACE_JLC_VIA_VTABLE)
    if(!(vtable->vtmark & VT_FALLBACK_MARKED)) {
      vtable->vtmark |= VT_FALLBACK_MARKED;  //we need different marking for fallback compaction
      collector_tracestack_push(collector, &(vtable->jlC));
      //64bits consideration is needed, vtable->jlC is an uncompressed reference
    }
  
  if(obj_belongs_to_nos(p_obj) && obj_is_fw_in_oi(p_obj)){
    assert(obj_get_vt(p_obj) == obj_get_vt(obj_get_fw_in_oi(p_obj)));
    p_obj = obj_get_fw_in_oi(p_obj);
    assert(p_obj);
    write_slot(p_ref, p_obj);
  }
  
  if(!obj_mark_in_vt(p_obj))
    return;
  
#ifdef GC_GEN_STATS
  GC_Gen_Collector_Stats* stats = (GC_Gen_Collector_Stats*)collector->stats;
  gc_gen_collector_update_marked_obj_stats_major(stats);
#endif
  if( !object_has_ref_field(p_obj) ) return;
  
    /* scan array object */
  if (object_is_array(p_obj)) {
    Partial_Reveal_Object* array = p_obj;
    assert(!obj_is_primitive_array(array));
    
    I_32 array_length = vector_get_length((Vector_Handle) array);
    for (int i = 0; i < array_length; i++) {
      REF *p_ref = (REF*)vector_get_element_address_ref((Vector_Handle) array, i);
      scan_slot(collector, p_ref);
    }   
    return;
  }

  /* scan non-array object */
  unsigned int num_refs = object_ref_field_num(p_obj);
  int *ref_iterator = object_ref_iterator_init(p_obj);
            
  for(unsigned int i=0; i<num_refs; i++){
    REF* p_ref = object_ref_iterator_get(ref_iterator+i, p_obj);        
    scan_slot(collector, p_ref);
  }

#ifndef BUILD_IN_REFERENT
  scan_weak_reference(collector, p_obj, scan_slot);
#endif
  
  return;
}


static void trace_object(Collector* collector, REF *p_ref)
{ 
  scan_object(collector, p_ref);
  
  Vector_Block* trace_stack = collector->trace_stack;
  while( !vector_stack_is_empty(trace_stack)){
    p_ref = (REF *)vector_stack_pop(trace_stack); 
    scan_object(collector, p_ref);
    trace_stack = collector->trace_stack;
  }
    
  return; 
}

/* for marking phase termination detection */
static volatile unsigned int num_finished_collectors = 0;

void mark_scan_heap_for_fallback(Collector* collector)
{ 
  GC* gc = collector->gc;
  GC_Metadata* metadata = gc->metadata;
#ifdef GC_GEN_STATS
  GC_Gen_Collector_Stats* stats = (GC_Gen_Collector_Stats*)collector->stats;
#endif
  
  assert(collect_is_fallback());

  /* reset the num_finished_collectors to be 0 by one collector. This is necessary for the barrier later. */
  unsigned int num_active_collectors = gc->num_active_collectors;
  atomic_cas32( &num_finished_collectors, 0, num_active_collectors);
   
  collector->trace_stack = free_task_pool_get_entry(metadata);

  Vector_Block* root_set = pool_iterator_next(metadata->gc_rootset_pool);

  /* first step: copy all root objects to mark tasks. 
      FIXME:: can be done sequentially before coming here to eliminate atomic ops */ 
  while(root_set){
    POINTER_SIZE_INT* iter = vector_block_iterator_init(root_set);
    while(!vector_block_iterator_end(root_set,iter)){
      REF *p_ref = (REF *)*iter;
      iter = vector_block_iterator_advance(root_set,iter);

      /* root ref can't be NULL, (remset may have NULL ref entry, but this function is only for ALGO_MAJOR */
      assert(*p_ref);
      
      collector_tracestack_push(collector, p_ref);

#ifdef GC_GEN_STATS
      gc_gen_collector_update_rootset_ref_num(stats);   
#endif

    } 
    root_set = pool_iterator_next(metadata->gc_rootset_pool);
  }
  /* put back the last trace_stack task */    
  pool_put_entry(metadata->mark_task_pool, collector->trace_stack);
  
  /* second step: iterate over the mark tasks and scan objects */
  /* get a task buf for the mark stack */
  collector->trace_stack = free_task_pool_get_entry(metadata);

retry:
  Vector_Block* mark_task = pool_get_entry(metadata->mark_task_pool);
  
  while(mark_task){
    POINTER_SIZE_INT* iter = vector_block_iterator_init(mark_task);
    while(!vector_block_iterator_end(mark_task,iter)){
      REF* p_ref = (REF *)*iter;
      iter = vector_block_iterator_advance(mark_task,iter);

      /* FIXME:: we should not let mark_task empty during working, , other may want to steal it. 
         degenerate my stack into mark_task, and grab another mark_task */
      trace_object(collector, p_ref);
    } 
    /* run out one task, put back to the pool and grab another task */
   vector_stack_clear(mark_task);
   pool_put_entry(metadata->free_task_pool, mark_task);
   mark_task = pool_get_entry(metadata->mark_task_pool);      
  }
  
  /* termination detection. This is also a barrier.
     NOTE:: We can simply spin waiting for num_finished_collectors, because each 
     generated new task would surely be processed by its generating collector eventually. 
     So code below is only for load balance optimization. */
  atomic_inc32(&num_finished_collectors);
  while(num_finished_collectors != num_active_collectors){
    if( !pool_is_empty(metadata->mark_task_pool)){
      atomic_dec32(&num_finished_collectors);
      goto retry;  
    }
  }
     
  /* put back the last mark stack to the free pool */
  mark_task = (Vector_Block*)collector->trace_stack;
  vector_stack_clear(mark_task);
  pool_put_entry(metadata->free_task_pool, mark_task);   
  collector->trace_stack = NULL;
  
  return;
}

void trace_obj_in_fallback_marking(Collector *collector, void *p_ref)
{
  trace_object(collector, (REF *)p_ref);
}

#ifdef USE_32BITS_HASHCODE

/* for semispace NOS, actually only the fromspace needs cleaning of oi. */
void fallback_clear_fwd_obj_oi(Collector* collector)
{
  GC* gc = collector->gc;
  Blocked_Space* space = (Blocked_Space*)((GC_Gen*)gc)->nos;

  assert(collect_is_fallback());

  unsigned int num_active_collectors = gc->num_active_collectors;
  atomic_cas32( &num_finished_collectors, 0, num_active_collectors);
  
  Block_Header* curr_block = blocked_space_block_iterator_next(space);
  while(curr_block){
    Partial_Reveal_Object* curr_obj = (Partial_Reveal_Object*) curr_block->base;
    while(curr_obj < curr_block->free){
      unsigned int obj_size = vm_object_size(curr_obj);
      /* forwarded object is dead object (after fallback marking),but we need know its size to iterate live object */
      if(obj_is_fw_in_oi(curr_obj)){
        if(obj_is_sethash_in_vt(curr_obj)){ 
          /* this only happens in semispace GC, where an object with attached hashcode is forwarded.
             This object should be in survivor_area, forwarded from fromspace in last minor collection. 
             We restore its hashbits correctly in oi. */
          set_obj_info(curr_obj, (Obj_Info_Type)HASHCODE_SET_ATTACHED);
        }else{
          set_obj_info(curr_obj, (Obj_Info_Type)0);
        }
      }
      /* if it's not forwared, it may still have hashcode attached if its in survivor_area. 
         It's not forwarded because fallback happens before it's forwarded. */
      if(hashcode_is_attached(curr_obj))
        obj_size += GC_OBJECT_ALIGNMENT;
      
      curr_obj = (Partial_Reveal_Object*)((POINTER_SIZE_INT)curr_obj + obj_size);
    }
    curr_block = blocked_space_block_iterator_next(space);
  }
  atomic_inc32(&num_finished_collectors);
  while(num_finished_collectors < num_active_collectors) ;
}

void fallback_clear_fwd_obj_oi_init(Collector* collector)
{
  Blocked_Space* space = (Blocked_Space*)((GC_Gen*)collector->gc)->nos;
  blocked_space_block_iterator_init(space); 
    
}
#endif









