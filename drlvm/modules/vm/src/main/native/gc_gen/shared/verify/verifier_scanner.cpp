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
 
#include "verify_live_heap.h"
#include "verifier_common.h"
#include "verify_gc_effect.h"
#include "verify_mutator_effect.h"
#include "../finalizer_weakref/finalizer_weakref.h"

 /*<--------live objects scanner begin-------->*/
static FORCE_INLINE void scan_slot(Heap_Verifier* heap_verifier, REF*p_ref) 
{
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;
  Partial_Reveal_Object *p_obj = read_slot(p_ref);
  if(p_obj == NULL) return;  
   
  verify_live_object_slot(p_ref, heap_verifier);  
  verifier_tracestack_push(p_obj, gc_verifier->trace_stack); 
  return;
}
 
static FORCE_INLINE void scan_object(Heap_Verifier* heap_verifier, Partial_Reveal_Object *p_obj) 
{
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;

#if !defined(USE_UNIQUE_MARK_SWEEP_GC) && !defined(USE_UNIQUE_MOVE_COMPACT_GC)
  if(gc_verifier->is_before_fallback_collection) {
    if(obj_belongs_to_nos(p_obj) && obj_is_fw_in_oi(p_obj)){
      assert(obj_get_vt(p_obj) == obj_get_vt(obj_get_fw_in_oi(p_obj)));
      p_obj = obj_get_fw_in_oi(p_obj);
      assert(p_obj);
    }
  }
#endif
  
  if(!obj_mark_in_vt(p_obj)) return;

  if( !major_is_marksweep() && p_obj >= los_boundary ){
    Block_Header* block = GC_BLOCK_HEADER(p_obj);
    if( heap_verifier->is_before_gc)  block->num_live_objs++;
    /* we can't set block->num_live_objs = 0 if !is_before_gc, because the some blocks may be freed hence not
        visited after GC. So we should reset it in GC space reset functions. */
  }

  verify_object_header(p_obj, heap_verifier); 
  verifier_update_verify_info(p_obj, heap_verifier);

   /*FIXME: */
  if (!object_has_ref_field(p_obj)) return;
    
  REF* p_ref;

  if (object_is_array(p_obj)) {  
  
    Partial_Reveal_Array* array = (Partial_Reveal_Array*)p_obj;
    unsigned int array_length = array->array_len; 
    p_ref = (REF*)((POINTER_SIZE_INT)array + (int)array_first_element_offset(array));

    for (unsigned int i = 0; i < array_length; i++) {
      scan_slot(heap_verifier, p_ref+i);
    }   

  }else{ 
    
    unsigned int num_refs = object_ref_field_num(p_obj);
    int* ref_iterator = object_ref_iterator_init(p_obj);
 
    for(unsigned int i=0; i<num_refs; i++){  
      p_ref = object_ref_iterator_get(ref_iterator+i, p_obj);  
      scan_slot(heap_verifier, p_ref);
    }

#ifndef BUILD_IN_REFERENT
     WeakReferenceType type = special_reference_type(p_obj);
    if(type == SOFT_REFERENCE && verifier_collect_is_minor(gc_verifier)){
      p_ref = obj_get_referent_field(p_obj);
      scan_slot(heap_verifier, p_ref);
    } 
#endif  
  }
  return;
}

 
static void trace_object(Heap_Verifier* heap_verifier, Partial_Reveal_Object* p_obj)
{ 
  scan_object(heap_verifier, p_obj);
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;
  Vector_Block* trace_stack = (Vector_Block*)gc_verifier->trace_stack;
  while( !vector_stack_is_empty(trace_stack)){
    p_obj = (Partial_Reveal_Object *)vector_stack_pop(trace_stack); 
    scan_object(heap_verifier, p_obj);
    trace_stack = (Vector_Block*)gc_verifier->trace_stack;
  }
  return; 
}
 
void verifier_trace_rootsets(Heap_Verifier* heap_verifier, Pool* root_set_pool)
{
  Heap_Verifier_Metadata* verifier_metadata = heap_verifier->heap_verifier_metadata;
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;
  gc_verifier->objects_set = verifier_free_set_pool_get_entry(verifier_metadata->free_set_pool);
  gc_verifier->trace_stack = verifier_free_task_pool_get_entry(verifier_metadata->free_task_pool);
  gc_verifier->hashcode_set = verifier_free_set_pool_get_entry(verifier_metadata->free_set_pool);
  pool_iterator_init(root_set_pool);
  Vector_Block* root_set = pool_iterator_next(root_set_pool);
  
  /* first step: copy all root objects to trace tasks. */ 
  while(root_set){
    POINTER_SIZE_INT* iter = vector_block_iterator_init(root_set);
    while(!vector_block_iterator_end(root_set,iter)){
      REF* p_ref = (REF* )*iter;
      iter = vector_block_iterator_advance(root_set,iter);

      if(!heap_verifier->need_verify_rootset || !heap_verifier->is_before_gc){
        if(!verify_rootset_slot(p_ref, heap_verifier)){
          gc_verifier->is_verification_passed = FALSE;
          assert(0);
          continue;
        }
      }

      Partial_Reveal_Object* p_obj = read_slot(p_ref);
      assert(p_obj != NULL);  

      verifier_tracestack_push(p_obj, gc_verifier->trace_stack);
    } 
    root_set = pool_iterator_next(root_set_pool);
  }
  /* put back the last trace_stack task */    
  pool_put_entry(verifier_metadata->mark_task_pool, gc_verifier->trace_stack);
  
  /* second step: iterate over the trace tasks and forward objects */
  gc_verifier->trace_stack = verifier_free_task_pool_get_entry(verifier_metadata->free_task_pool);

  Vector_Block* trace_task = pool_get_entry(verifier_metadata->mark_task_pool);

  while(trace_task){    
    POINTER_SIZE_INT* iter = vector_block_iterator_init(trace_task);
    while(!vector_block_iterator_end(trace_task,iter)){
      Partial_Reveal_Object* p_obj = (Partial_Reveal_Object* )*iter;
      iter = vector_block_iterator_advance(trace_task,iter);
      trace_object(heap_verifier, p_obj); 
    }
    vector_stack_clear(trace_task);
    pool_put_entry(verifier_metadata->free_task_pool, trace_task);
    trace_task = pool_get_entry(verifier_metadata->mark_task_pool);
  }
  vector_stack_clear(gc_verifier->trace_stack);
  pool_put_entry(verifier_metadata->free_task_pool, gc_verifier->trace_stack);
  gc_verifier->trace_stack = NULL;

}


void verifier_trace_objsets(Heap_Verifier* heap_verifier, Pool* obj_set_pool)
{
  Heap_Verifier_Metadata* verifier_metadata = heap_verifier->heap_verifier_metadata;
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;
  gc_verifier->trace_stack = verifier_free_task_pool_get_entry(verifier_metadata->free_task_pool);
  pool_iterator_init(obj_set_pool);
  Vector_Block* obj_set = pool_iterator_next(obj_set_pool);
  /* first step: copy all root objects to trace tasks. */ 
  while(obj_set){
    POINTER_SIZE_INT* iter = vector_block_iterator_init(obj_set);
    while(!vector_block_iterator_end(obj_set,iter)){
      Partial_Reveal_Object* p_obj = read_slot((REF*)iter);
      iter = vector_block_iterator_advance(obj_set,iter);
      /*p_obj can be NULL , When GC happened, the obj in Finalize objs list will be clear.*/
      //assert(p_obj != NULL);  
      if(p_obj == NULL) continue;
      if(heap_verifier->gc_is_gen_mode && heap_verifier->is_before_gc && !obj_belongs_to_nos(p_obj)) continue;
      verifier_tracestack_push(p_obj, gc_verifier->trace_stack);
    } 
    obj_set = pool_iterator_next(obj_set_pool);
  }
  /* put back the last trace_stack task */    
  pool_put_entry(verifier_metadata->mark_task_pool, gc_verifier->trace_stack);
  
  /* second step: iterate over the trace tasks and forward objects */
  gc_verifier->trace_stack = verifier_free_task_pool_get_entry(verifier_metadata->free_task_pool);

  Vector_Block* trace_task = pool_get_entry(verifier_metadata->mark_task_pool);

  while(trace_task){    
    POINTER_SIZE_INT* iter = vector_block_iterator_init(trace_task);
    while(!vector_block_iterator_end(trace_task,iter)){
      Partial_Reveal_Object* p_obj = (Partial_Reveal_Object* )*iter;
      iter = vector_block_iterator_advance(trace_task,iter);
      trace_object(heap_verifier, p_obj); 
    }
    vector_stack_clear(trace_task);
    pool_put_entry(verifier_metadata->free_task_pool, trace_task);
    trace_task = pool_get_entry(verifier_metadata->mark_task_pool);
  }
  vector_stack_clear(gc_verifier->trace_stack);
  pool_put_entry(verifier_metadata->free_task_pool, gc_verifier->trace_stack);
  gc_verifier->trace_stack = NULL;

}

void verifier_scan_resurrect_objects(Heap_Verifier* heap_verifier)
{
  GC* gc    =  heap_verifier->gc;
  Heap_Verifier_Metadata* verifier_metadata = heap_verifier->heap_verifier_metadata;
  verifier_update_info_before_resurrect(heap_verifier);
#ifndef BUILD_IN_REFERENT
  heap_verifier->gc_verifier->is_tracing_resurrect_obj = TRUE;
  if(heap_verifier->is_before_gc){
    verifier_copy_pool(verifier_metadata->obj_with_fin_pool, gc->finref_metadata->obj_with_fin_pool);
    verifier_trace_objsets(heap_verifier, verifier_metadata->obj_with_fin_pool);
  }else{
    if(!heap_verifier->gc_verifier->is_before_fallback_collection){
      verify_live_finalizable_obj(heap_verifier, gc->finref_metadata->obj_with_fin_pool);
      verifier_copy_pool_reverse_order(verifier_metadata->finalizable_obj_pool, gc->finref_metadata->finalizable_obj_pool);
      verifier_trace_objsets(heap_verifier, verifier_metadata->finalizable_obj_pool);
      verifier_clear_pool(verifier_metadata->finalizable_obj_pool, heap_verifier->heap_verifier_metadata->free_set_pool, FALSE);
    }else{
      verifier_trace_objsets(heap_verifier, verifier_metadata->obj_with_fin_pool );  
    }
    verifier_clear_pool(verifier_metadata->obj_with_fin_pool, heap_verifier->heap_verifier_metadata->free_set_pool, FALSE);
  }
  heap_verifier->gc_verifier->is_tracing_resurrect_obj = FALSE;
  verifier_update_info_after_resurrect(heap_verifier);
#endif
}

void verifier_scan_unreachable_objects(Heap_Verifier* heap_verifier);
void verifier_scan_prepare()
{ 
  verifier_reset_hash_distance(); 
}
void verifier_scan_live_objects(Heap_Verifier* heap_verifier)
{
  Heap_Verifier_Metadata* verifier_metadata = heap_verifier->heap_verifier_metadata;
  verifier_scan_prepare();
  verifier_trace_rootsets(heap_verifier, verifier_metadata->root_set_pool);
  verifier_scan_resurrect_objects(heap_verifier);
  verifier_scan_unreachable_objects(heap_verifier);
  verifier_clear_objs_mark_bit(heap_verifier);
}
/*<--------live objects scanner end--------->*/

/*<--------all (live and dead) objects scanner begin-------->*/
static FORCE_INLINE void verifier_scan_object_slots(Partial_Reveal_Object *p_obj, Heap_Verifier* heap_verifier) 
{
  verifier_allocation_update_info(p_obj, heap_verifier);
  verify_object_header(p_obj, heap_verifier); 
  if (!object_has_ref_field(p_obj)) return; 
  REF* p_ref;

  if (object_is_array(p_obj)){    
    Partial_Reveal_Array* array = (Partial_Reveal_Array*)p_obj;
    unsigned int array_length = array->array_len; 
    p_ref = (REF*)((POINTER_SIZE_INT)array + (int)array_first_element_offset(array));

    for (unsigned int i = 0; i < array_length; i++) {
     verify_write_barrier(p_ref+i, heap_verifier); 
     if( read_slot(p_ref+i) != NULL) verify_all_object_slot(p_ref+i, heap_verifier);
     // if(!is_unreachable_obj(p_obj)){ 
     //   verify_write_barrier(p_ref+i, heap_verifier);
     //   if( read_slot(p_ref+i) != NULL) verify_live_object_slot(p_ref+i, heap_verifier);
     // }else{
     //   if( read_slot(p_ref+i) != NULL) verify_all_object_slot(p_ref+i, heap_verifier);
     // }
    }   
  }else{   
    unsigned int num_refs = object_ref_field_num(p_obj);
    int* ref_iterator = object_ref_iterator_init(p_obj);
 
    for(unsigned int i=0; i<num_refs; i++){
      p_ref = object_ref_iterator_get(ref_iterator+i, p_obj);  
      verify_write_barrier(p_ref, heap_verifier);
      if( read_slot(p_ref) != NULL) verify_all_object_slot(p_ref, heap_verifier);
      //if(!is_unreachable_obj(p_obj)){
      //  verify_write_barrier(p_ref, heap_verifier);
      //  if( read_slot(p_ref) != NULL) verify_live_object_slot(p_ref, heap_verifier);
      //}else{
      //  if( read_slot(p_ref) != NULL) verify_all_object_slot(p_ref, heap_verifier);
      //}
    }
    
#ifndef BUILD_IN_REFERENT
    WeakReferenceType type = special_reference_type(p_obj);
    if(type == NOT_REFERENCE) return;
    
    //if(type != SOFT_REFERENCE && verifier_collect_is_minor(heap_verifier->gc_verifier)){
    {
      p_ref = obj_get_referent_field(p_obj);
      verify_write_barrier(p_ref, heap_verifier);
      if( read_slot(p_ref) != NULL)   verify_all_object_slot(p_ref, heap_verifier);
      //if(!is_unreachable_obj(p_obj)){ 
      //  verify_write_barrier(p_ref, heap_verifier);
      //  if( read_slot(p_ref) != NULL)   verify_live_object_slot(p_ref, heap_verifier);
      //}else{
      //  if( read_slot(p_ref) != NULL)   verify_all_object_slot(p_ref, heap_verifier);
      //}
    } 
#endif
  
  }
  return;
}

void verifier_scan_nos_mos_objects(Space* space, Heap_Verifier* heap_verifier)
{
  Block* blocks = (Block*)space->heap_start; 
  unsigned int start_idx = ((Blocked_Space*)space)->first_block_idx; 
  unsigned int i;
  unsigned int num_block = ((Blocked_Space*)space)->free_block_idx - ((Blocked_Space*)space)->first_block_idx;
  for( i=0; i < num_block; i++ ){
    Block_Header* block = (Block_Header*)&(blocks[i]);
    Partial_Reveal_Object* cur_obj = (Partial_Reveal_Object*) block->base;
    while( cur_obj < block->free ){
      verify_object_header(cur_obj, heap_verifier);
      verifier_scan_object_slots(cur_obj, heap_verifier);
      cur_obj = obj_end(cur_obj);
    }
  }
}

inline Partial_Reveal_Object* lspace_get_next_object( Space* lspace, POINTER_SIZE_INT* & next_area_start){
  POINTER_SIZE_INT* ret_obj = NULL;
  
  while(((POINTER_SIZE_INT)next_area_start < (POINTER_SIZE_INT)lspace->heap_end)&&!*next_area_start ){
    next_area_start =(POINTER_SIZE_INT*)((POINTER_SIZE_INT)next_area_start + ((Free_Area*)next_area_start)->size);
  }
  if((POINTER_SIZE_INT)next_area_start < (POINTER_SIZE_INT)lspace->heap_end){
    ret_obj = next_area_start;
    unsigned int hash_extend_size = 0;
#ifdef USE_32BITS_HASHCODE
    hash_extend_size  = (hashcode_is_attached((Partial_Reveal_Object*)next_area_start))?GC_OBJECT_ALIGNMENT:0;
#endif
    POINTER_SIZE_INT obj_size = ALIGN_UP_TO_KILO(vm_object_size((Partial_Reveal_Object*)next_area_start) + hash_extend_size);
    assert(obj_size);
    next_area_start = (POINTER_SIZE_INT*)((POINTER_SIZE_INT)next_area_start + obj_size);
    return (Partial_Reveal_Object*)ret_obj;
  }else{
    return NULL;
  } 
}

inline Partial_Reveal_Object* lspace_get_first_object( Space* lspace, POINTER_SIZE_INT* & next_area_start){
  return lspace_get_next_object(lspace, next_area_start);
}

void verifier_scan_los_objects(Space* lspace, Heap_Verifier* heap_verifier)
{
  POINTER_SIZE_INT* interator = (POINTER_SIZE_INT*)lspace->heap_start;
  Partial_Reveal_Object* p_los_obj = lspace_get_first_object(lspace, interator);

  while(p_los_obj){
    verify_object_header(p_los_obj, heap_verifier);
    verifier_scan_object_slots(p_los_obj, heap_verifier);
    p_los_obj = lspace_get_next_object(lspace, interator);
  }
}

void verifier_scan_all_objects(Heap_Verifier* heap_verifier)
{
#if !defined(USE_UNIQUE_MARK_SWEEP_GC) && !defined(USE_UNIQUE_MOVE_COMPACT_GC)
  GC_Gen* gc       = (GC_Gen*)heap_verifier->gc;
  Space* fspace     = gc_get_nos(gc);
  Space* mspace   = gc_get_mos(gc);
  Space* lspace     = gc_get_los(gc);
  
  verifier_scan_nos_mos_objects(fspace, heap_verifier);
  verifier_scan_nos_mos_objects(mspace, heap_verifier);
  verifier_scan_los_objects(lspace, heap_verifier);
#else
  assert(0);
#endif
}
/*<--------all objects scanner end--------->*/

/*<--------unreachable objects scanner begin------>*/

void verifier_scan_mos_unreachable_objects(Space* space, Heap_Verifier* heap_verifier)
{
  Block* blocks = (Block*)space->heap_start; 
  unsigned int start_idx = ((Blocked_Space*)space)->first_block_idx; 
  unsigned int i;
  unsigned int num_block = ((Blocked_Space*)space)->free_block_idx - ((Blocked_Space*)space)->first_block_idx;
  for( i=0; i < num_block; i++ ){
    Block_Header* block = (Block_Header*)&(blocks[i]);
    Partial_Reveal_Object* cur_obj = (Partial_Reveal_Object*) block->base;
    while( cur_obj < block->free ){
      verify_object_header(cur_obj, heap_verifier);
     // if(!obj_is_marked_in_vt(cur_obj)) tag_unreachable_obj(cur_obj);
      cur_obj = obj_end(cur_obj);
    }
  }
}

void verifier_scan_los_unreachable_objects(Space* lspace, Heap_Verifier* heap_verifier)
{
  POINTER_SIZE_INT* interator = (POINTER_SIZE_INT*)lspace->heap_start;
  Partial_Reveal_Object* p_los_obj = lspace_get_first_object(lspace, interator);

  while(p_los_obj){
    verify_object_header(p_los_obj, heap_verifier);
    //if(!obj_is_marked_in_vt(p_los_obj)) tag_unreachable_obj(p_los_obj);
    p_los_obj = lspace_get_next_object(lspace, interator);
  }
}

void verifier_scan_unreachable_objects(Heap_Verifier* heap_verifier)
{
#if !defined(USE_UNIQUE_MARK_SWEEP_GC) && !defined(USE_UNIQUE_MOVE_COMPACT_GC)
  if(heap_verifier->is_before_gc) return;
  GC_Gen* gc       = (GC_Gen*)heap_verifier->gc;  
  Space* mspace   = gc_get_mos(gc);
  Space* lspace     = gc_get_los(gc);
  
  verifier_scan_mos_unreachable_objects(mspace, heap_verifier);
  verifier_scan_los_unreachable_objects(lspace, heap_verifier);
#else
  return;
#endif
}
/*<--------unreachable objects scanner end------>*/

void verifier_init_object_scanner(Heap_Verifier* heap_verifier)
{
  heap_verifier->live_obj_scanner = verifier_scan_live_objects;
  heap_verifier->all_obj_scanner   = verifier_scan_all_objects;
}






