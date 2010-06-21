
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

#include "fspace.h"
#include "../thread/collector.h"
#include "../common/gc_metadata.h"
#include "../finalizer_weakref/finalizer_weakref.h"
#include "../common/compressed_ref.h"

#ifdef GC_GEN_STATS
#include "../gen/gen_stats.h"
#endif
static FORCE_INLINE Boolean fspace_object_to_be_forwarded(Partial_Reveal_Object *p_obj, Fspace *fspace)
{
  assert(obj_belongs_to_nos(p_obj));  
  return forward_first_half? (p_obj < object_forwarding_boundary):(p_obj>=object_forwarding_boundary);
}

static FORCE_INLINE void scan_slot(Collector *collector, REF *p_ref) 
{
  Partial_Reveal_Object *p_obj = read_slot(p_ref);
  if( p_obj == NULL) return;
    
  /* the slot can be in tspace or fspace, we don't care.
     we care only if the reference in the slot is pointing to fspace */
  if (obj_belongs_to_nos(p_obj))
    collector_tracestack_push(collector, p_ref); 

  return;
}

void trace_forwarded_gen_jlC_from_vtable(Collector* collector, Partial_Reveal_Object *p_obj); 
static FORCE_INLINE void scan_object(Collector* collector, Partial_Reveal_Object *p_obj) 
{
  assert((((POINTER_SIZE_INT)p_obj) % GC_OBJECT_ALIGNMENT) == 0);
  Partial_Reveal_VTable *vtable = decode_vt(obj_get_vt(p_obj));
  if(TRACE_JLC_VIA_VTABLE)
    if(vtable->vtmark == VT_UNMARKED) {
      vtable->vtmark = VT_MARKED;
      trace_forwarded_gen_jlC_from_vtable(collector, vtable->jlC);
    }
  if (!object_has_ref_field(p_obj)) return;
    
  REF *p_ref;

  /* scan array object */
  if (object_is_array(p_obj)) {
    Partial_Reveal_Object* array = p_obj;
    assert(!obj_is_primitive_array(array));

    I_32 array_length = vector_get_length((Vector_Handle) array);        
    for (int i = 0; i < array_length; i++) {
      p_ref= (REF *)vector_get_element_address_ref((Vector_Handle) array, i);
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

/* NOTE:: At this point, p_ref can be in anywhere like root, and other spaces, but *p_ref must be in fspace, 
   since only slot which points to object in fspace could be added into TraceStack.
   The problem is the *p_ref may be forwarded already so that, when we come here we find it's pointing to tospace.
   We will simply return for that case. It might be forwarded due to:
    1. two difference slots containing same reference; 
    2. duplicate slots in remset ( we use SSB for remset, no duplication filtering.)
   The same object can be traced by the thread itself, or by other thread.
*/

static FORCE_INLINE void forward_object(Collector *collector, REF *p_ref) 
{
  Space* space = collector->collect_space; 
  GC* gc = collector->gc;
  Partial_Reveal_Object *p_obj = read_slot(p_ref);

  if(!obj_belongs_to_nos(p_obj)) return; 

  /* Fastpath: object has already been forwarded, update the ref slot */
  if(obj_is_fw_in_oi(p_obj)) {
    Partial_Reveal_Object* p_target_obj = obj_get_fw_in_oi(p_obj);
    write_slot(p_ref, p_target_obj);
    return;
  }

  /* only mark the objects that will remain in fspace */
  if(NOS_PARTIAL_FORWARD && !fspace_object_to_be_forwarded(p_obj, (Fspace*)space)) {
    assert(!obj_is_fw_in_oi(p_obj));
    /* this obj remains in fspace, remember its ref slot for next GC if p_ref is not root. 
       we don't need remember root ref. Actually it's wrong to rem root ref since they change in next GC */
    if( !addr_belongs_to_nos(p_ref) && address_belongs_to_gc_heap(p_ref, gc))
      collector_remset_add_entry(collector, ( Partial_Reveal_Object**) p_ref); 
    
    if(obj_mark_in_oi(p_obj)){
      scan_object(collector, p_obj);
#ifdef GC_GEN_STATS
      GC_Gen_Collector_Stats* stats = (GC_Gen_Collector_Stats*)collector->stats;
      gc_gen_collector_update_marked_nos_obj_stats_minor(stats);
#endif

    }
    return;
  }
    
  /* following is the logic for forwarding */  
  Partial_Reveal_Object* p_target_obj = collector_forward_object(collector, p_obj);
  
  /* if p_target_obj is NULL, it is forwarded by other thread. 
      Note: a race condition here, it might be forwarded by other, but not set the 
      forwarding pointer yet. We need spin here to get the forwarding pointer. 
      We can implement the collector_forward_object() so that the forwarding pointer 
      is set in the atomic instruction, which requires to roll back the mos_alloced
      space. That is easy for thread local block allocation cancellation. */
  if( p_target_obj == NULL ){
    if(collector->result == FALSE ){
      /* failed to forward, let's get back to controller. */
      vector_stack_clear(collector->trace_stack);
      return;
    }

    Partial_Reveal_Object *p_new_obj = obj_get_fw_in_oi(p_obj);
    assert(p_new_obj);
    write_slot(p_ref, p_new_obj);
    return;
  }  
  /* otherwise, we successfully forwarded */

#ifdef GC_GEN_STATS
  GC_Gen_Collector_Stats* stats = (GC_Gen_Collector_Stats*)collector->stats;
  gc_gen_collector_update_marked_nos_obj_stats_minor(stats);
  gc_gen_collector_update_moved_nos_obj_stats_minor(stats, vm_object_size(p_obj));
#endif

  write_slot(p_ref, p_target_obj);


  scan_object(collector, p_target_obj); 
  return;
}

void trace_forwarded_gen_jlC_from_vtable(Collector* collector, Partial_Reveal_Object *p_obj) 
//Forward the vtable->jlc and trace the forwarded object. But do not update the vtable->jlc but leave them for weakroots updating
//We probably do not need this function if we do not perform class unloading in copy-collections. That means all vtable->jlc would be strong roots in this algorithm
{
  Space* space = collector->collect_space; 
  GC* gc = collector->gc;

  if(!obj_belongs_to_nos(p_obj)) return; 

  /* Fastpath: object has already been forwarded*/
  if(obj_is_fw_in_oi(p_obj))          return;

  if(NOS_PARTIAL_FORWARD && !fspace_object_to_be_forwarded(p_obj, (Fspace*)space)) {
    assert(!obj_is_fw_in_oi(p_obj));
    if(obj_mark_in_oi(p_obj)) 
      scan_object(collector, p_obj);
    return;
  }
    
  /* following is the logic for forwarding */  
  Partial_Reveal_Object* p_target_obj = collector_forward_object(collector, p_obj);
  if( p_target_obj == NULL ){
    if(collector->result == FALSE ){
      /* failed to forward, let's get back to controller. */
      vector_stack_clear(collector->trace_stack);
      return;
    }
    assert(obj_get_fw_in_oi(p_obj));
    return;
  }  
  scan_object(collector, p_target_obj); 
  return;
}

static void trace_object(Collector *collector, REF *p_ref)
{ 
  forward_object(collector, p_ref);
  
  Vector_Block* trace_stack = (Vector_Block*)collector->trace_stack;
  while( !vector_stack_is_empty(trace_stack)){
    p_ref = (REF *)vector_stack_pop(trace_stack); 
#ifdef PREFETCH_SUPPORTED    
    /* DO PREFETCH */
   if(mark_prefetch) {
     if(!vector_stack_is_empty(trace_stack)) {
        REF *pref = (REF*)vector_stack_read(trace_stack, 0);
        PREFETCH( read_slot(pref) );
     }
   }
#endif   
    forward_object(collector, p_ref);
    trace_stack = (Vector_Block*)collector->trace_stack;
  }
    
  return; 
}
 
/* for tracing phase termination detection */
static volatile unsigned int num_finished_collectors = 0;

static void collector_trace_rootsets(Collector* collector)
{
  GC* gc = collector->gc;
  GC_Metadata* metadata = gc->metadata;
#ifdef GC_GEN_STATS
  GC_Gen_Collector_Stats* stats = (GC_Gen_Collector_Stats*)collector->stats;
#endif
  
  unsigned int num_active_collectors = gc->num_active_collectors;
  atomic_cas32( &num_finished_collectors, 0, num_active_collectors);

  Space* space = collector->collect_space;
  collector->trace_stack = free_task_pool_get_entry(metadata);

  /* find root slots saved by 1. active mutators, 2. exited mutators, 3. last cycle collectors */  
  Vector_Block* root_set = pool_iterator_next(metadata->gc_rootset_pool);

  /* first step: copy all root objects to trace tasks. */ 

  TRACE2("gc.process", "GC: collector["<<((POINTER_SIZE_INT)collector->thread_handle)<<"]: copy root objects to trace stack ......");
  while(root_set){
    POINTER_SIZE_INT* iter = vector_block_iterator_init(root_set);
    while(!vector_block_iterator_end(root_set,iter)){
      REF *p_ref = (REF *)*iter;
      iter = vector_block_iterator_advance(root_set,iter);
      
      if(!*p_ref) continue;  /* root ref cann't be NULL, but remset can be */
      Partial_Reveal_Object *p_obj = read_slot(p_ref);

#ifdef GC_GEN_STATS
      gc_gen_collector_update_rootset_ref_num(stats);
#endif

      if(obj_belongs_to_nos(p_obj)){
        collector_tracestack_push(collector, p_ref);
      }
    } 
    root_set = pool_iterator_next(metadata->gc_rootset_pool);
  }
  /* put back the last trace_stack task */    
  pool_put_entry(metadata->mark_task_pool, collector->trace_stack);
  
  /* second step: iterate over the trace tasks and forward objects */
  collector->trace_stack = free_task_pool_get_entry(metadata);

  TRACE2("gc.process", "GC: collector["<<((POINTER_SIZE_INT)collector->thread_handle)<<"]: finish copying root objects to trace stack.");

  TRACE2("gc.process", "GC: collector["<<((POINTER_SIZE_INT)collector->thread_handle)<<"]: trace and forward objects ......");

retry:
  Vector_Block* trace_task = pool_get_entry(metadata->mark_task_pool);

  while(trace_task){    
    POINTER_SIZE_INT* iter = vector_block_iterator_init(trace_task);
    while(!vector_block_iterator_end(trace_task,iter)){
      REF *p_ref = (REF *)*iter;
      iter = vector_block_iterator_advance(trace_task,iter);
#ifdef PREFETCH_SUPPORTED      
      /* DO PREFETCH */  
      if( mark_prefetch ) {    
        if(!vector_block_iterator_end(trace_task, iter)) {
      	  REF *pref= (REF*) *iter;
      	  PREFETCH( read_slot(pref));
        }	
      }
#endif           
      assert(*p_ref); /* a task can't be NULL, it was checked before put into the task stack */
      /* in sequential version, we only trace same object once, but we were using a local hashset for that,
         which couldn't catch the repetition between multiple collectors. This is subject to more study. */
   
      /* FIXME:: we should not let root_set empty during working, other may want to steal it. 
         degenerate my stack into root_set, and grab another stack */
   
      /* a task has to belong to collected space, it was checked before put into the stack */
      trace_object(collector, p_ref);
      if(collector->result == FALSE)  break; /* force return */
    }
    vector_stack_clear(trace_task);
    pool_put_entry(metadata->free_task_pool, trace_task);
    if(collector->result == FALSE){
      gc_task_pool_clear(metadata->mark_task_pool);
      break; /* force return */
    }

    trace_task = pool_get_entry(metadata->mark_task_pool);
  }
  
  atomic_inc32(&num_finished_collectors);
  while(num_finished_collectors != num_active_collectors){
    if( pool_is_empty(metadata->mark_task_pool)) continue;
    /* we can't grab the task here, because of a race condition. If we grab the task, 
       and the pool is empty, other threads may fall to this barrier and then pass. */
    atomic_dec32(&num_finished_collectors);
    goto retry;      
  }
  TRACE2("gc.process", "GC: collector["<<((POINTER_SIZE_INT)collector->thread_handle)<<"]: finish tracing and forwarding objects.");

  /* now we are done, but each collector has a private stack that is empty */  
  trace_task = (Vector_Block*)collector->trace_stack;
  vector_stack_clear(trace_task);
  pool_put_entry(metadata->free_task_pool, trace_task);   
  collector->trace_stack = NULL;
  
  return;
}

void gen_forward_pool(Collector* collector) 
{  
  GC* gc = collector->gc;
  Fspace* space = (Fspace*)collector->collect_space;
 
  collector_trace_rootsets(collector);
  
  /* the rest work is not enough for parallelization, so let only one thread go */
  if( (POINTER_SIZE_INT)collector->thread_handle != 0 ) {
    TRACE2("gc.process", "GC: collector["<<(POINTER_SIZE_INT)collector->thread_handle<<"] finished");
    return;
  }

  gc->collect_result = gc_collection_result(gc);
  if(!gc->collect_result){
#ifndef BUILD_IN_REFERENT
    fallback_finref_cleanup(gc);
#endif
    return;
  }

  if(!IGNORE_FINREF ){
    collector_identify_finref(collector);
    if(!gc->collect_result) return;
  }
#ifndef BUILD_IN_REFERENT
  else {
      gc_set_weakref_sets(gc);
      gc_update_weakref_ignore_finref(gc);
    }
#endif
  gc_identify_dead_weak_roots(gc);
  
  gc_fix_rootset(collector, FALSE);
  
  TRACE2("gc.process", "GC: collector[0] finished");

  return;
  
}

void trace_obj_in_gen_fw(Collector *collector, void *p_ref)
{
  trace_object(collector, (REF *)p_ref);
}
