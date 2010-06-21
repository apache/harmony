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

#include "../common/gc_metadata.h"
#include "../thread/collector.h"
#include "../gen/gen.h"
#include "../finalizer_weakref/finalizer_weakref.h"

#ifdef GC_GEN_STATS
#include "../gen/gen_stats.h"
#endif

static FORCE_INLINE void scan_slot(Collector* collector, REF *p_ref)
{
  Partial_Reveal_Object *p_obj = read_slot(p_ref);
  if(p_obj == NULL) return;

  if(obj_mark_in_vt(p_obj)){
#ifdef GC_GEN_STATS
    GC_Gen_Collector_Stats* stats = (GC_Gen_Collector_Stats*)collector->stats;
    gc_gen_collector_update_marked_obj_stats_major(stats);
#endif
    collector_tracestack_push(collector, p_obj);
    unsigned int obj_size = vm_object_size(p_obj);
#ifdef USE_32BITS_HASHCODE
    obj_size += (hashcode_is_set(p_obj))?GC_OBJECT_ALIGNMENT:0;
#endif
    if(!obj_belongs_to_space(p_obj, gc_get_los((GC_Gen*)collector->gc))){
      collector->non_los_live_obj_size += obj_size;
      collector->segment_live_size[SIZE_TO_SEGMENT_INDEX(obj_size)] += obj_size;
    } else {
      collector->los_live_obj_size += round_up_to_size(obj_size, KB);
    }
  }
  
  return;
}

static FORCE_INLINE void scan_object(Collector* collector, Partial_Reveal_Object *p_obj)
{
  vm_notify_obj_alive( (void *)p_obj);
  assert((((POINTER_SIZE_INT)p_obj) % GC_OBJECT_ALIGNMENT) == 0);

  Partial_Reveal_VTable *vtable = decode_vt(obj_get_vt(p_obj));
  if(TRACE_JLC_VIA_VTABLE)
    if(vtable->vtmark == VT_UNMARKED) {
      vtable->vtmark = VT_MARKED;
      if(obj_mark_in_vt(vtable->jlC))
        collector_tracestack_push(collector, vtable->jlC);
    }
  
  if( !object_has_ref_field(p_obj) ) return;
  
  REF *p_ref;

  if (object_is_array(p_obj)) {   /* scan array object */
  
    Partial_Reveal_Array* array = (Partial_Reveal_Array*)p_obj;
    unsigned int array_length = array->array_len;
  
    p_ref = (REF *)((POINTER_SIZE_INT)array + (int)array_first_element_offset(array));

    for (unsigned int i = 0; i < array_length; i++) {
      scan_slot(collector, p_ref+i);
    }   

  }else{ /* scan non-array object */
    
    unsigned int num_refs = object_ref_field_num(p_obj);
    
    int* ref_iterator = object_ref_iterator_init(p_obj);
    
    for(unsigned int i=0; i<num_refs; i++){  
      p_ref = object_ref_iterator_get(ref_iterator+i, p_obj);  
      scan_slot(collector, p_ref);
    }    

#ifndef BUILD_IN_REFERENT
    scan_weak_reference(collector, p_obj, scan_slot);
#endif
  }
  
  return;
}


static void trace_object(Collector* collector, Partial_Reveal_Object *p_obj)
{ 
  scan_object(collector, p_obj);
  
  Vector_Block* trace_stack = collector->trace_stack;
  while( !vector_stack_is_empty(trace_stack)){
    p_obj = (Partial_Reveal_Object *)vector_stack_pop(trace_stack); 
    scan_object(collector, p_obj);
    trace_stack = collector->trace_stack;
  }
    
  return; 
}

/* for marking phase termination detection */
static volatile unsigned int num_finished_collectors = 0;

/* NOTE:: Only marking in object header is idempotent.
   Originally, we have to mark the object before put it into markstack, to 
   guarantee there is only one occurrance of an object in markstack. This is to
   guarantee there is only one occurrance of a repointed ref slot in repset (they
   are put to the set when the object is scanned). If the same object is put to 
   markstack twice, they will be scanned twice and their ref slots will be recorded twice. 
   Problem occurs when the ref slot is updated first time with new position,
   the second time the value in the ref slot is not the old position as expected.
   It needs to read the original obj header for forwarding pointer. With the new value,
   it will read something nonsense since the obj is not moved yet.
   This can be worked around if we want. 
   To do this we have to use atomic instruction for marking, which is undesirable. 
   So we abondoned this design. We no longer use the repset to remember repointed slots 
*/
  
void mark_scan_heap_for_space_tune(Collector *collector)
{
  GC* gc = collector->gc;
  GC_Metadata* metadata = gc->metadata;
#ifdef GC_GEN_STATS
  GC_Gen_Collector_Stats* stats = (GC_Gen_Collector_Stats*)collector->stats;
#endif

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

      Partial_Reveal_Object *p_obj = read_slot(p_ref);
      /* root ref can't be NULL, (remset may have NULL ref entry, but this function is only for ALGO_MAJOR */
      assert(p_obj!=NULL);
      /* we have to mark the object before put it into marktask, because
         it is possible to have two slots containing a same object. They will
         be scanned twice and their ref slots will be recorded twice. Problem
         occurs after the ref slot is updated first time with new position
         and the second time the value is the ref slot is the old position as expected.
         This can be worked around if we want. 
      */
      if(obj_mark_in_vt(p_obj)){
        collector_tracestack_push(collector, p_obj);

#ifdef GC_GEN_STATS 
        gc_gen_collector_update_rootset_ref_num(stats);
#endif

        unsigned int obj_size = vm_object_size(p_obj);
#ifdef USE_32BITS_HASHCODE
        obj_size += (hashcode_is_set(p_obj))?GC_OBJECT_ALIGNMENT:0;
#endif
        if(!obj_belongs_to_space(p_obj, gc_get_los((GC_Gen*)gc))){
          collector->non_los_live_obj_size += obj_size;
          collector->segment_live_size[SIZE_TO_SEGMENT_INDEX(obj_size)] += obj_size;
        } else {
          collector->los_live_obj_size += round_up_to_size(obj_size, KB);
        }
      }

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
      Partial_Reveal_Object* p_obj = (Partial_Reveal_Object *)*iter;
      iter = vector_block_iterator_advance(mark_task,iter);

      /* FIXME:: we should not let mark_task empty during working, , other may want to steal it. 
         degenerate my stack into mark_task, and grab another mark_task */
      trace_object(collector, p_obj);
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

void trace_obj_in_space_tune_marking(Collector *collector, void *p_obj)
{
  obj_mark_in_vt((Partial_Reveal_Object*)p_obj);
  trace_object(collector, (Partial_Reveal_Object *)p_obj);
}
