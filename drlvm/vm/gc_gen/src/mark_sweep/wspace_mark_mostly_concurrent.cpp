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
#include "../thread/conclctor.h"
#include "gc_ms.h"

volatile Boolean need_terminate_mostly_con_mark;
extern unsigned int mostly_con_final_marker_num;
extern unsigned int mostly_con_long_marker_num;

Boolean obj_is_marked_in_table(Partial_Reveal_Object *obj);

static FORCE_INLINE void scan_slot(Collector* marker, REF *p_ref)
{
  Partial_Reveal_Object *p_obj = read_slot(p_ref);
  if( p_obj == NULL) return;
  
  assert(address_belongs_to_gc_heap(p_obj, marker->gc));
  if(obj_mark_gray_in_table(p_obj)){
    assert(p_obj);
    collector_tracestack_push((Collector*)marker, p_obj);
  }  
}

static FORCE_INLINE void scan_object(Conclctor* marker, Partial_Reveal_Object *p_obj)
{
  assert((((POINTER_SIZE_INT)p_obj) % GC_OBJECT_ALIGNMENT) == 0);
  if(obj_is_dirty_in_table(p_obj)){ 
    return;
  }

  if(!object_has_ref_field(p_obj)) return;

  REF *p_ref;
  
  if(object_is_array(p_obj)){   /* scan array object */
    Partial_Reveal_Array *array = (Partial_Reveal_Array*)p_obj;
    unsigned int array_length = array->array_len;
    
    p_ref = (REF *)((POINTER_SIZE_INT)array + (int)array_first_element_offset(array));
    for (unsigned int i = 0; i < array_length; i++)
      scan_slot((Collector*)marker, p_ref+i);
    
    return;
  }
  
  /* scan non-array object */
  unsigned int num_refs = object_ref_field_num(p_obj);
  int *ref_iterator = object_ref_iterator_init(p_obj);
  
  for(unsigned int i=0; i<num_refs; i++){
    p_ref = object_ref_iterator_get(ref_iterator+i, p_obj);
    scan_slot((Collector*)marker, p_ref);
  }

#ifndef BUILD_IN_REFERENT
  //scan_weak_reference((Collector*)marker, p_obj, scan_slot);
  scan_weak_reference_direct((Collector*)marker, p_obj, scan_slot);
#endif

}

static void trace_object(Conclctor* marker, Partial_Reveal_Object *p_obj)
{
  scan_object(marker, p_obj);
  obj_mark_black_in_table(p_obj, marker);
  
  Vector_Block *trace_stack = marker->trace_stack;
  while(!vector_stack_is_empty(trace_stack)){
    p_obj = (Partial_Reveal_Object*)vector_stack_pop(trace_stack);
    scan_object(marker, p_obj);
    obj_mark_black_in_table(p_obj, marker);    
    trace_stack = marker->trace_stack;
  }
}

/* for marking phase termination detection */
void mostly_con_mark_terminate_reset()
{ need_terminate_mostly_con_mark = FALSE; }

void terminate_mostly_con_mark()
{   need_terminate_mostly_con_mark = TRUE; }

static Boolean concurrent_mark_need_terminating_mc(GC* gc)
{
  return need_terminate_mostly_con_mark;
  /*
  GC_Metadata *metadata = gc->metadata;
  return pool_is_empty(metadata->gc_dirty_set_pool);
  */
}

static volatile unsigned int num_active_markers = 0;
static SpinLock info_lock;
void wspace_mark_scan_mostly_concurrent(Conclctor* marker)
{
  GC *gc = marker->gc;
  GC_Metadata *metadata = gc->metadata;
  
  unsigned int num_dirtyset_slot = 0;

  marker->trace_stack = free_task_pool_get_entry(metadata);
  
  /* first step: copy all root objects to mark tasks.*/
  Vector_Block *root_set = pool_iterator_next(metadata->gc_rootset_pool);

  while(root_set){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(root_set);
    while(!vector_block_iterator_end(root_set,iter)){
      Partial_Reveal_Object *p_obj = (Partial_Reveal_Object *)*iter;
      iter = vector_block_iterator_advance(root_set,iter);
      
      assert(p_obj!=NULL);
      assert(address_belongs_to_gc_heap(p_obj, gc));
      if(obj_mark_gray_in_table(p_obj))
        collector_tracestack_push((Collector*)marker, p_obj);
    }
    root_set = pool_iterator_next(metadata->gc_rootset_pool);
  }

  /* put back the last trace_stack task */
  pool_put_entry(metadata->mark_task_pool, marker->trace_stack);
  marker->trace_stack = free_task_pool_get_entry(metadata);

  /* following code has such concerns:
      1, current_thread_id should be unique
      2, mostly concurrent do not need adding new marker dynamically
      3, when the heap is exhausted, final marking will enumeration rootset, it should be after above actions
  */
  unsigned int current_thread_id = atomic_inc32(&num_active_markers);

  if((current_thread_id+1) == gc->num_active_markers )
    state_transformation( gc, GC_CON_START_MARKERS, GC_CON_TRACING);
  
  while( gc->gc_concurrent_status == GC_CON_START_MARKERS );

retry:

  
  
  /*second step: mark dirty pool*/
  Vector_Block* dirty_set = pool_get_entry(metadata->gc_dirty_set_pool);

  while(dirty_set){
    POINTER_SIZE_INT* iter = vector_block_iterator_init(dirty_set);
    while(!vector_block_iterator_end(dirty_set,iter)){
      Partial_Reveal_Object *p_obj = (Partial_Reveal_Object *)*iter;
      iter = vector_block_iterator_advance(dirty_set,iter);

      assert(p_obj!=NULL); //FIXME: restrict condition?
      
      obj_clear_dirty_in_table(p_obj);
      obj_clear_mark_in_table(p_obj, marker);

      if(obj_mark_gray_in_table(p_obj))
        collector_tracestack_push((Collector*)marker, p_obj);

      num_dirtyset_slot ++;
    } 
    vector_block_clear(dirty_set);
    pool_put_entry(metadata->free_set_pool, dirty_set);
    dirty_set = pool_get_entry(metadata->gc_dirty_set_pool);
  }

   /* put back the last trace_stack task */    
  pool_put_entry(metadata->mark_task_pool, marker->trace_stack);  

  /* third step: iterate over the mark tasks and scan objects */
   marker->trace_stack = free_task_pool_get_entry(metadata);

  
  Vector_Block *mark_task = pool_get_entry(metadata->mark_task_pool);
  
  while(mark_task){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(mark_task);
    while(!vector_block_iterator_end(mark_task,iter)){
      Partial_Reveal_Object *p_obj = (Partial_Reveal_Object*)*iter;
      iter = vector_block_iterator_advance(mark_task,iter);
      trace_object(marker, p_obj);      
    }
    /* run out one task, put back to the pool and grab another task */
    vector_stack_clear(mark_task);
    pool_put_entry(metadata->free_task_pool, mark_task);
    mark_task = pool_get_entry(metadata->mark_task_pool);
  }

  /*
  if(current_thread_id == 0){
    gc_prepare_dirty_set(marker->gc);
  }*/

  gc_copy_local_dirty_set_to_global(gc);
  
  /* conditions to terminate mark: 
           1.All thread finished current job.
           2.Flag is set to terminate concurrent mark.
    */
  atomic_dec32(&num_active_markers);
  while(num_active_markers != 0 || !concurrent_mark_need_terminating_mc(gc) ) {
      if(!pool_is_empty(metadata->mark_task_pool) || !pool_is_empty(metadata->gc_dirty_set_pool)) {
	   atomic_inc32(&num_active_markers);
          goto retry;
      } else if( current_thread_id >= mostly_con_long_marker_num ) {
         break;
      }
      apr_sleep(15000);
  }

  /*
  while(num_active_markers != 0 || !concurrent_mark_need_terminating_mc(gc)){
    if(!pool_is_empty(metadata->mark_task_pool) || !pool_is_empty(metadata->gc_dirty_set_pool)){
      atomic_inc32(&num_active_markers);
      goto retry;
    }
  }*/
  
  /* put back the last mark stack to the free pool */
  mark_task = (Vector_Block*)marker->trace_stack;
  vector_stack_clear(mark_task);
  pool_put_entry(metadata->free_task_pool, mark_task);
  marker->trace_stack = NULL;
  marker->num_dirty_slots_traced = num_dirtyset_slot;

  /*
  if(num_dirtyset_slot!=0) {
  	lock(info_lock);
  	INFO2("gc.marker", "marker ["<< current_thread_id <<"] processed dirty slot="<<num_dirtyset_slot);
	unlock(info_lock);
  }*/
  return;
}


void wspace_final_mark_scan_mostly_concurrent(Conclctor* marker)
{
  
  GC *gc = marker->gc;
  GC_Metadata *metadata = gc->metadata;

  unsigned int num_dirtyset_slot = 0;
  
  marker->trace_stack = free_task_pool_get_entry(metadata);
  Vector_Block *root_set = pool_iterator_next(metadata->gc_rootset_pool);
  
  /* first step: copy all root objects to mark tasks.*/
  while(root_set){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(root_set);
    while(!vector_block_iterator_end(root_set,iter)){
      Partial_Reveal_Object *p_obj = (Partial_Reveal_Object *)*iter;
      iter = vector_block_iterator_advance(root_set,iter);
      
      assert(p_obj!=NULL);
      assert(address_belongs_to_gc_heap(p_obj, gc));
      if(obj_mark_gray_in_table(p_obj))
        collector_tracestack_push((Collector*)marker, p_obj);
    }
    root_set = pool_iterator_next(metadata->gc_rootset_pool);
  }
  /* put back the last trace_stack task */
  pool_put_entry(metadata->mark_task_pool, marker->trace_stack);
  marker->trace_stack = free_task_pool_get_entry(metadata);


  /*second step: mark dirty pool*/
  Vector_Block* dirty_set = pool_get_entry(metadata->gc_dirty_set_pool);

  while(dirty_set){
    POINTER_SIZE_INT* iter = vector_block_iterator_init(dirty_set);
    while(!vector_block_iterator_end(dirty_set,iter)){
      Partial_Reveal_Object *p_obj = (Partial_Reveal_Object *)*iter;
      iter = vector_block_iterator_advance(dirty_set,iter);

      assert(p_obj!=NULL); //FIXME: restrict condition?
      
      obj_clear_dirty_in_table(p_obj);
      obj_clear_mark_in_table(p_obj, marker);

      if(obj_mark_gray_in_table(p_obj))
        collector_tracestack_push((Collector*)marker, p_obj);

      num_dirtyset_slot ++;
    } 
    vector_block_clear(dirty_set);
    pool_put_entry(metadata->free_set_pool, dirty_set);
    dirty_set = pool_get_entry(metadata->gc_dirty_set_pool);
  }
   /* put back the last trace_stack task */    
  pool_put_entry(metadata->mark_task_pool, marker->trace_stack);  

  /* third step: iterate over the mark tasks and scan objects */
  marker->trace_stack = free_task_pool_get_entry(metadata);

  Vector_Block *mark_task = pool_get_entry(metadata->mark_task_pool);
  
  while(mark_task){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(mark_task);
    while(!vector_block_iterator_end(mark_task,iter)){
      Partial_Reveal_Object *p_obj = (Partial_Reveal_Object*)*iter;
      iter = vector_block_iterator_advance(mark_task,iter);
      trace_object(marker, p_obj);      
    }
    /* run out one task, put back to the pool and grab another task */
    vector_stack_clear(mark_task);
    pool_put_entry(metadata->free_task_pool, mark_task);
    mark_task = pool_get_entry(metadata->mark_task_pool);
  }

  /* put back the last mark stack to the free pool */
  mark_task = (Vector_Block*)marker->trace_stack;
  vector_stack_clear(mark_task);
  pool_put_entry(metadata->free_task_pool, mark_task);
  marker->trace_stack = NULL;

  //marker->time_mark += time_mark;
  marker->num_dirty_slots_traced = num_dirtyset_slot;
  //INFO2("gc.marker", "[final marker] processed dirty slot="<<num_dirtyset_slot);
  
  return;
}




void wspace_last_mc_marker_work( Conclctor *last_marker ) {
   
   GC *gc = last_marker->gc;
   if( gc->gc_concurrent_status != GC_CON_TRACING )
   	return;
   
   gc_con_update_stat_after_marking(gc); //calculate marked size
    //just debugging
   Con_Collection_Statistics *con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
   con_collection_stat->marking_end_time = time_now();
   int64 con_marking_time = con_collection_stat->marking_end_time - con_collection_stat->marking_start_time;
   INFO2("gc.scheduler", "[MOSTLY_CON] con marking time=" << con_marking_time << " us");

   state_transformation( gc, GC_CON_TRACING, GC_CON_TRACE_DONE );
   //INFO2("gc.con.info", "<new state 3> first marking thread finished its job, GC is waiting for all the marking threads finish, current marker num is [" << gc->num_active_markers << "]" );
}

void gc_mostly_con_update_stat_after_final_marking(GC *gc);
void wspace_mostly_con_final_mark( GC *gc ) {

    /*init the root set pool*/
    pool_iterator_init(gc->metadata->gc_rootset_pool);
    /*prepare dirty object*/
    gc_prepare_dirty_set(gc);
    /*new asssign thread may reuse the one just finished in the same phase*/ 
    conclctor_set_weakref_sets(gc);

    /*start final mostly concurrent mark */
   gc_ms_start_mostly_con_final_mark((GC_MS*)gc, mostly_con_final_marker_num);

   mostly_con_mark_terminate_reset();
   gc_mostly_con_update_stat_after_final_marking(gc);
   
   gc_reset_dirty_set(gc);
   gc_clear_rootset(gc);
   gc_prepare_sweeping(gc);
   state_transformation( gc, GC_CON_TRACE_DONE, GC_CON_BEFORE_SWEEP );
}

void trace_obj_in_ms_mostly_concurrent_mark(Collector *collector, void *p_obj)
{
  obj_mark_gray_in_table((Partial_Reveal_Object*)p_obj);
  trace_object((Conclctor*)collector, (Partial_Reveal_Object *)p_obj);
}



