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
#include "../mark_sweep/wspace_mark_sweep.h"
void analyze_bad_obj(Partial_Reveal_Object *p_obj)
{
  Chunk_Header *chunk;
  unsigned int slot_index;
  unsigned int obj_size = 0;
  
  if(is_super_obj(p_obj)){
         chunk = ABNORMAL_CHUNK_HEADER(p_obj);
         slot_index = 0;
         obj_size = CHUNK_SIZE(chunk);
	  INFO2("gc.verifier", "[super bad obj]=" << p_obj << " size=" << obj_size << ", chunk" << chunk);
   } else {
         chunk = NORMAL_CHUNK_HEADER(p_obj);
         slot_index = slot_addr_to_index(chunk, p_obj);
         obj_size = chunk->slot_size;
	  INFO2("gc.verifier", "[normal bad obj]=" << p_obj << ", size=" << obj_size << ", chunk[" << chunk <<"] slot index[" <<slot_index <<"]" );
   }

   if(obj_is_mark_gray_in_table(p_obj))
   	INFO2("gc.verifier", "Bad Gray object!!!");

    if(obj_is_mark_black_in_table(p_obj))
	 INFO2("gc.verifier", "It is not a Bad object!!!");
   	
    Partial_Reveal_VTable *vt = decode_vt(obj_get_vt(p_obj));
    INFO2( "gc.verifier", "bad object is class " << vtable_get_gcvt(vt)->gc_class_name << " jlC=" << vt->jlC);
    INFO2( "gc.verifier", "^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
    
}


 /*<--------live objects scanner begin-------->*/
static FORCE_INLINE void scan_slot(Heap_Verifier* heap_verifier, REF*p_ref) 
{
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;
  Partial_Reveal_Object *p_obj = read_slot(p_ref);
  if(p_obj == NULL) return;  
  assert(address_belongs_to_gc_heap(p_obj, heap_verifier->gc)); 
  verifier_tracestack_push(p_obj, gc_verifier->trace_stack); 
  return;
}
 
static void scan_object(Heap_Verifier* heap_verifier, Partial_Reveal_Object *p_obj) 
{
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;
  if(!obj_mark_in_vt(p_obj)) return;
  assert(obj_is_mark_black_in_table(p_obj));
  if(!obj_is_mark_black_in_table(p_obj)) {
     analyze_bad_obj(p_obj);
  }
  verify_object_header(p_obj, heap_verifier); 
  verifier_update_verify_info(p_obj, heap_verifier);
  
  if (!object_has_ref_field(p_obj)) return;
  REF* p_ref;
  if (object_is_array(p_obj)) {
    Partial_Reveal_Array* array = (Partial_Reveal_Array*)p_obj;
    unsigned int array_length = array->array_len;
    //INFO2("gc.verifier","\tscan array "<< p_obj <<"(" << array_length << ")");
    p_ref = (REF*)((POINTER_SIZE_INT)array + (int)array_first_element_offset(array));
    for (unsigned int i = 0; i < array_length; i++) {
      scan_slot(heap_verifier, p_ref+i);
    }   
  }else{ 
    unsigned int num_refs = object_ref_field_num(p_obj);
    int* ref_iterator = object_ref_iterator_init(p_obj);
    
    //INFO2("gc.verifier","\tscan object "<< p_obj <<"(" << num_refs << ")");	
    for(unsigned int i=0; i<num_refs; i++){  
      p_ref = object_ref_iterator_get(ref_iterator+i, p_obj);  
      scan_slot(heap_verifier, p_ref);
    }
    
   #ifndef BUILD_IN_REFERENT
   WeakReferenceType type = special_reference_type(p_obj);
   if(type != NOT_REFERENCE) {
     REF *p_referent_field = obj_get_referent_field(p_obj);
     scan_slot(heap_verifier, p_referent_field);
   }
   #endif  
  }
  return;
}

 
static void trace_object(Heap_Verifier* heap_verifier, Partial_Reveal_Object* p_obj)
{ 
  //INFO2("gc.verifier","trace root ["<< p_obj <<"] => ");
  scan_object(heap_verifier, p_obj);
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;
  Vector_Block* trace_stack = (Vector_Block*)gc_verifier->trace_stack;
  Partial_Reveal_Object *sub_obj = NULL;
  while( !vector_stack_is_empty(trace_stack)){
    sub_obj = (Partial_Reveal_Object *)vector_stack_pop(trace_stack); 
    scan_object(heap_verifier, sub_obj);
    trace_stack = (Vector_Block*)gc_verifier->trace_stack;
  }
  return; 
}
 
void con_verifier_trace_from_rootsets(Heap_Verifier* heap_verifier, Pool* root_set_pool)
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
      Partial_Reveal_Object* p_obj = read_slot(p_ref);
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

  pool_put_entry(verifier_metadata->objects_pool_before_gc, gc_verifier->objects_set);
  
  vector_stack_clear(gc_verifier->trace_stack);
  pool_put_entry(verifier_metadata->free_task_pool, gc_verifier->trace_stack);
  gc_verifier->trace_stack = NULL;

}

unsigned int clear_objs_mark_bit(Heap_Verifier* heap_verifier)
{
  Heap_Verifier_Metadata* verifier_metadata = heap_verifier->heap_verifier_metadata; 
  Pool* marked_objs_pool = verifier_metadata->objects_pool_before_gc;

  pool_iterator_init(marked_objs_pool);
  Vector_Block* objs_set = pool_iterator_next(marked_objs_pool);
  unsigned int clear_counter = 0;
  while(objs_set){
    POINTER_SIZE_INT* iter = vector_block_iterator_init(objs_set);
    while(!vector_block_iterator_end(objs_set,iter)){
      Partial_Reveal_Object* p_obj = (Partial_Reveal_Object*)*iter;
      iter = vector_block_iterator_advance(objs_set,iter);
      assert(p_obj != NULL);
      assert(obj_is_marked_in_vt(p_obj));
      clear_counter++;
      obj_unmark_in_vt(p_obj);
    } 
    objs_set = pool_iterator_next(marked_objs_pool);
  }
  return clear_counter;
}

void verifier_rescan_after_con(Heap_Verifier* heap_verifier)
{
  INFO2("gc.con.verify", "start scan live object %%%%%%%%%%%%%%%%%%%%%%");
  Heap_Verifier_Metadata* verifier_metadata = heap_verifier->heap_verifier_metadata;
  con_verifier_trace_from_rootsets(heap_verifier, verifier_metadata->root_set_pool);
  clear_objs_mark_bit(heap_verifier);
  INFO2("gc.con.verify", "end of scan live object %%%%%%%%%%%%%%%%%%%%%%");
}

void verify_gc_reset(Heap_Verifier* heap_verifier);
void verify_heap_after_con_gc(GC *gc)
{
     Heap_Verifier *heap_verifier = get_heap_verifier();
     int64 verify_start_time = time_now();
     verifier_copy_rootsets(gc,  heap_verifier);
     verifier_rescan_after_con(heap_verifier);
     INFO2("gc.verifier", "[Verifier] verifier marked num=" << heap_verifier->gc_verifier->num_live_objects_before_gc );
     verify_gc_reset(heap_verifier);
     unsigned int verify_time = trans_time_unit(time_now() - verify_start_time);
     INFO2("gc.verifier", "[Verifier] verify time = [" << verify_time << "] ms")
}

