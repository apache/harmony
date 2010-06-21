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

#include "wspace_mark_sweep.h"
#include "../finalizer_weakref/finalizer_weakref.h"

static Wspace *wspace_in_marking;
static FORCE_INLINE Boolean obj_mark_gray(Partial_Reveal_Object *obj)
{
  if(obj_belongs_to_space(obj, (Space*)wspace_in_marking))
    return obj_mark_gray_in_table(obj);
  else
    return obj_mark_in_vt(obj);
}

static FORCE_INLINE Boolean obj_mark_black(Partial_Reveal_Object *obj)
{
  if(obj_belongs_to_space(obj, (Space*)wspace_in_marking)){
    Boolean marked_by_self = obj_mark_black_in_table(obj);

#ifndef USE_UNIQUE_MARK_SWEEP_GC
    /* When fallback happens, some objects in MOS have their fw bit set, which is actually their mark bit in the last minor gc.
     * If we don't clear it, some objects that didn't be moved will be mistaken for being moved in the coming fixing phase.
     */
    if(marked_by_self){
      Obj_Info_Type oi = obj->obj_info;
      Obj_Info_Type new_oi = oi & DUAL_MARKBITS_MASK;
      while(new_oi != oi){
        Obj_Info_Type temp = atomic_casptrsz((volatile Obj_Info_Type*)get_obj_info_addr(obj), new_oi, oi);
        if(temp == oi) break;
        oi = obj->obj_info;
        new_oi = oi & DUAL_MARKBITS_MASK;
      }
    }
#endif
    return marked_by_self;
  } else {
    return obj_mark_in_vt(obj);
  }
}


/* The caller must be in places where alloc color and mark color haven't been flipped */
Boolean obj_is_marked_in_table(Partial_Reveal_Object *obj)
{
  unsigned int index_in_word;
  volatile POINTER_SIZE_INT *p_color_word = get_color_word_in_table(obj, index_in_word);
  assert(p_color_word);
  
  POINTER_SIZE_INT color_word = *p_color_word;
  POINTER_SIZE_INT mark_color = cur_mark_gray_color << index_in_word;
  
  return (Boolean)(color_word & mark_color);
}

static FORCE_INLINE void scan_slot(Collector *collector, REF *p_ref)
{
  Partial_Reveal_Object *p_obj = read_slot(p_ref);
  if( p_obj == NULL) return;
  
  assert(address_belongs_to_gc_heap(p_obj, collector->gc));
  if(obj_mark_gray(p_obj)){
    assert(p_obj);
    collector_tracestack_push(collector, p_obj);
  }
}

static FORCE_INLINE void scan_object(Collector *collector, Partial_Reveal_Object *p_obj)
{
  assert((((POINTER_SIZE_INT)p_obj) % GC_OBJECT_ALIGNMENT) == 0);
  
  Partial_Reveal_VTable *vtable = decode_vt(obj_get_vt(p_obj));
  if(TRACE_JLC_VIA_VTABLE)
    if(vtable->vtmark == VT_UNMARKED) {
      vtable->vtmark = VT_MARKED;
      if(obj_mark_black(vtable->jlC))
        collector_tracestack_push(collector, vtable->jlC);
    }
  
  if(!object_has_ref_field(p_obj)) return;
  
  REF *p_ref;
  
  if(object_is_array(p_obj)){   /* scan array object */
    Partial_Reveal_Array *array = (Partial_Reveal_Array*)p_obj;
    unsigned int array_length = array->array_len;
    
    p_ref = (REF *)((POINTER_SIZE_INT)array + (int)array_first_element_offset(array));
    for (unsigned int i = 0; i < array_length; i++)
      scan_slot(collector, p_ref+i);
    
    return;
  }
  
  /* scan non-array object */
  unsigned int num_refs = object_ref_field_num(p_obj);
  int *ref_iterator = object_ref_iterator_init(p_obj);
  
  for(unsigned int i=0; i<num_refs; i++){
    p_ref = object_ref_iterator_get(ref_iterator+i, p_obj);
    scan_slot(collector, p_ref);
  }

    if(!IGNORE_FINREF )
      scan_weak_reference(collector, p_obj, scan_slot);
#ifndef BUILD_IN_REFERENT
    else {
      scan_weak_reference_direct(collector, p_obj, scan_slot);
    }
#endif

}

static void trace_object(Collector *collector, Partial_Reveal_Object *p_obj)
{
  scan_object(collector, p_obj);
  obj_mark_black(p_obj);
  
  Vector_Block *trace_stack = collector->trace_stack;
  while(!vector_stack_is_empty(trace_stack)){
    p_obj = (Partial_Reveal_Object*)vector_stack_pop(trace_stack);
    scan_object(collector, p_obj);
    obj_mark_black(p_obj);
    trace_stack = collector->trace_stack;
  }
}

/* NOTE:: This is another marking version: marking in color bitmap table.
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
   So we abondoned this design. We no longer use the repset to remember repointed slots.
*/

/* for marking phase termination detection */
static volatile unsigned int num_finished_collectors = 0;

void wspace_mark_scan(Collector *collector, Wspace *wspace)
{
  GC *gc = collector->gc;
  GC_Metadata *metadata = gc->metadata;
  wspace_in_marking = wspace;
  
  /* reset the num_finished_collectors to be 0 by one collector. This is necessary for the barrier later. */
  unsigned int num_active_collectors = gc->num_active_collectors;
  atomic_cas32(&num_finished_collectors, 0, num_active_collectors);
  
  collector->trace_stack = free_task_pool_get_entry(metadata);
  
  Vector_Block *root_set = pool_iterator_next(metadata->gc_rootset_pool);
  
  /* first step: copy all root objects to mark tasks.
     FIXME:: can be done sequentially before coming here to eliminate atomic ops */
  while(root_set){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(root_set);
    while(!vector_block_iterator_end(root_set,iter)){
      REF *p_ref = (REF*)*iter;
      iter = vector_block_iterator_advance(root_set,iter);
      
      Partial_Reveal_Object *p_obj = read_slot(p_ref);
      /* root ref can't be NULL, (remset may have NULL ref entry, but this function is only for ALGO_MAJOR */
      assert(p_obj != NULL);
      /* we have to mark the object before putting it into marktask, because
         it is possible to have two slots containing a same object. They will
         be scanned twice and their ref slots will be recorded twice. Problem
         occurs after the ref slot is updated first time with new position
         and the second time the value is the ref slot is the old position as expected.
         This can be worked around if we want.
      */
      assert(address_belongs_to_gc_heap(p_obj, gc));
      if(obj_mark_gray(p_obj))
        collector_tracestack_push(collector, p_obj);
    }
    root_set = pool_iterator_next(metadata->gc_rootset_pool);
  }
  /* put back the last trace_stack task */
  pool_put_entry(metadata->mark_task_pool, collector->trace_stack);
  
  /* second step: iterate over the mark tasks and scan objects */
  /* get a task buf for the mark stack */
  collector->trace_stack = free_task_pool_get_entry(metadata);

retry:
  Vector_Block *mark_task = pool_get_entry(metadata->mark_task_pool);
  
  while(mark_task){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(mark_task);
    while(!vector_block_iterator_end(mark_task, iter)){
      Partial_Reveal_Object *p_obj = (Partial_Reveal_Object*)*iter;
      iter = vector_block_iterator_advance(mark_task, iter);
      
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
    if(!pool_is_empty(metadata->mark_task_pool)){
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

void trace_obj_in_ms_marking(Collector *collector, void *p_obj)
{
  obj_mark_gray((Partial_Reveal_Object*)p_obj);
  trace_object(collector, (Partial_Reveal_Object*)p_obj);
}
