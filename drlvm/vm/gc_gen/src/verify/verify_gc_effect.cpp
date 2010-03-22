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

#include "verifier_common.h"
#include "verify_gc_effect.h"
#ifdef USE_UNIQUE_MARK_SWEEP_GC
#include "../mark_sweep/wspace_mark_sweep.h"
#endif

static POINTER_SIZE_INT hash_obj_distance = 0;

void verifier_init_GC_verifier(Heap_Verifier* heap_verifier)
{
  GC_Verifier* gc_verifier = (GC_Verifier*)STD_MALLOC(sizeof(GC_Verifier));
  assert(gc_verifier);
  memset(gc_verifier, 0, sizeof(GC_Verifier));
  
  gc_verifier->trace_stack = gc_verifier->objects_set = gc_verifier->root_set = NULL;
  gc_verifier->is_tracing_resurrect_obj = FALSE;
  heap_verifier->gc_verifier = gc_verifier;
}
void verifier_destruct_GC_verifier(Heap_Verifier* heap_verifier)
{
  assert(!heap_verifier->gc_verifier ->trace_stack);
  assert(!heap_verifier->gc_verifier ->objects_set );
  assert(!heap_verifier->gc_verifier ->root_set);
  STD_FREE(heap_verifier->gc_verifier );
  heap_verifier->gc_verifier  = NULL;
}


void verifier_clear_objsets(Heap_Verifier* heap_verifier)
{
  Heap_Verifier_Metadata* verifier_metadata = heap_verifier->heap_verifier_metadata;
  verifier_clear_pool(verifier_metadata->objects_pool_before_gc, verifier_metadata->free_set_pool, FALSE);
  verifier_clear_pool(verifier_metadata->objects_pool_after_gc, verifier_metadata->free_set_pool, FALSE);
#ifndef BUILD_IN_REFERENT
  verifier_clear_pool(verifier_metadata->resurrect_objects_pool_before_gc, verifier_metadata->free_set_pool, FALSE);
  verifier_clear_pool(verifier_metadata->resurrect_objects_pool_after_gc, verifier_metadata->free_set_pool, FALSE);
#endif
}

void verify_gc_reset(Heap_Verifier* heap_verifier)
{
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;
  
  gc_verifier->trace_stack = gc_verifier->objects_set = gc_verifier->root_set = NULL;
  gc_verifier->is_tracing_resurrect_obj = FALSE;
  gc_verifier->num_live_objects_after_gc = gc_verifier->num_live_objects_before_gc = 0;
  gc_verifier->size_live_objects_after_gc = gc_verifier->size_live_objects_before_gc = 0;
  gc_verifier->num_hash_after_gc = gc_verifier->num_hash_before_gc = 0;
  gc_verifier->num_hash_attached_after_gc = gc_verifier->num_hash_attached_before_gc = 0;
  gc_verifier->num_hash_buffered_after_gc = gc_verifier->num_hash_buffered_before_gc = 0;
  gc_verifier->num_hash_set_unalloc_after_gc = gc_verifier->num_hash_set_unalloc_before_gc = 0;
#ifndef BUILD_IN_REFERENT
  gc_verifier->num_resurrect_objects_after_gc = gc_verifier->num_resurrect_objects_before_gc = 0;
  gc_verifier->size_resurrect_objects_after_gc = gc_verifier->size_resurrect_objects_before_gc = 0;
#endif

  verifier_clear_rootsets(heap_verifier);
  verifier_clear_objsets(heap_verifier);
}

void verify_live_finalizable_obj(Heap_Verifier* heap_verifier, Pool* live_finalizable_objs_pool)
{
  if(heap_verifier->gc_is_gen_mode) return;
  pool_iterator_init(live_finalizable_objs_pool);
  Vector_Block* live_fin_objs = pool_iterator_next(live_finalizable_objs_pool);
  while(live_fin_objs){
    POINTER_SIZE_INT * iter = vector_block_iterator_init(live_fin_objs);
    while(!vector_block_iterator_end(live_fin_objs, iter)){
      Partial_Reveal_Object* p_fin_obj = read_slot((REF*)iter);
      iter = vector_block_iterator_advance(live_fin_objs, iter);
      if(p_fin_obj==NULL) continue;
      assert(obj_is_marked_in_vt(p_fin_obj));
      if(!obj_is_marked_in_vt(p_fin_obj)){
        printf("\nERROR: live finalizable obj is not marked.\n");
        assert(0);
      }
    }
    live_fin_objs = pool_iterator_next(live_finalizable_objs_pool);
  }
}

void* verifier_copy_obj_information(Partial_Reveal_Object* p_obj)
{
  if(!object_has_ref_field(p_obj)){
    Live_Object_Inform* p_obj_information = (Live_Object_Inform* )STD_MALLOC(sizeof(Live_Object_Inform));
    assert(p_obj_information);
    p_obj_information->vt_raw = obj_get_vt_raw(p_obj);
    p_obj_information->address = p_obj;
    p_obj_information->obj_info=get_obj_info_raw(p_obj)& OBJ_INFO_MASK;
    return (void*) p_obj_information;
  }else{
    REF *p_ref;
    if (object_is_array(p_obj)) {  
      Partial_Reveal_Array* array = (Partial_Reveal_Array*)p_obj;
      unsigned int array_length = array->array_len;
      Live_Object_Ref_Slot_Inform* p_obj_information = (Live_Object_Ref_Slot_Inform* )STD_MALLOC(sizeof(Live_Object_Inform) + sizeof(VT)*array_length);

      p_obj_information->vt_raw = obj_get_vt_raw(p_obj);
      p_obj_information->address = p_obj;
      p_obj_information->obj_info=get_obj_info_raw(p_obj)& OBJ_INFO_MASK;
      
      p_ref = (REF *)((POINTER_SIZE_INT)array + (int)array_first_element_offset(array));

      unsigned int i = 0;
      for(; i<array_length;i++){
        Partial_Reveal_Object* p_obj = read_slot(p_ref+i);
        p_obj_information->ref_slot[i] = p_obj==NULL? (VT)NULL: obj_get_vt(p_obj);
      }
      return p_obj_information;
    }else{
      unsigned int num_refs = object_ref_field_num(p_obj);
      Live_Object_Ref_Slot_Inform* p_obj_information = (Live_Object_Ref_Slot_Inform* )STD_MALLOC(sizeof(Live_Object_Inform) + sizeof(VT)*num_refs);
      
      p_obj_information->vt_raw = obj_get_vt_raw(p_obj);
      p_obj_information->address = p_obj;
      p_obj_information->obj_info=get_obj_info_raw(p_obj)& OBJ_INFO_MASK;

      int* ref_iterator = object_ref_iterator_init(p_obj);
      
      unsigned int i = 0;
      for(; i<num_refs; i++){  
        p_ref = object_ref_iterator_get(ref_iterator+i, p_obj);
        Partial_Reveal_Object* p_obj = read_slot(p_ref);
        p_obj_information->ref_slot[i] = p_obj == NULL? (VT)NULL: obj_get_vt(p_obj);
      }
      return p_obj_information;
    }
  }
}

static Boolean fspace_object_was_forwarded(Partial_Reveal_Object *p_obj, Fspace *fspace, Heap_Verifier* heap_verifier)
{
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;
  assert(obj_belongs_to_space(p_obj, (Space*)fspace));
  unsigned int forwarded_first_part;
  if(!verifier_collect_is_minor(gc_verifier) || !NOS_PARTIAL_FORWARD || heap_verifier->gc_is_gen_mode)
    forwarded_first_part = true;
  else
    forwarded_first_part = forward_first_half^1;
  /* forward_first_half is flipped after the collection, so the condition is reversed as well */
  return forwarded_first_part? (p_obj < object_forwarding_boundary):(p_obj >= object_forwarding_boundary);
}

void verifier_update_info_before_resurrect(Heap_Verifier* heap_verifier)
{
  if(!heap_verifier->need_verify_gc) return;
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;
  Heap_Verifier_Metadata* verifier_metadata = heap_verifier->heap_verifier_metadata;

  if(heap_verifier->is_before_gc){
    pool_put_entry(verifier_metadata->objects_pool_before_gc, gc_verifier->objects_set);
    gc_verifier->objects_set = verifier_free_set_pool_get_entry(verifier_metadata->free_set_pool);
    assert(gc_verifier->objects_set);
    
    pool_put_entry(verifier_metadata->hashcode_pool_before_gc, gc_verifier->hashcode_set);
    gc_verifier->hashcode_set = verifier_free_set_pool_get_entry(verifier_metadata->free_set_pool);
    assert(gc_verifier->hashcode_set);
    return;
  }else{
    pool_put_entry(verifier_metadata->objects_pool_after_gc, gc_verifier->objects_set);
    gc_verifier->objects_set = verifier_free_set_pool_get_entry(verifier_metadata->free_set_pool);
    assert(gc_verifier->objects_set);
    
    pool_put_entry(verifier_metadata->hashcode_pool_after_gc, gc_verifier->hashcode_set);
    gc_verifier->hashcode_set = verifier_free_set_pool_get_entry(verifier_metadata->free_set_pool);
    assert(gc_verifier->hashcode_set);
    return;
  }

}

void verifier_update_info_after_resurrect(Heap_Verifier* heap_verifier)
{
  if(!heap_verifier->need_verify_gc) return;
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;
  Heap_Verifier_Metadata* verifier_metadata = heap_verifier->heap_verifier_metadata;

  hash_obj_distance = 0;

  if(heap_verifier->is_before_gc){
    pool_put_entry(verifier_metadata->resurrect_objects_pool_before_gc, gc_verifier->objects_set);
    gc_verifier->objects_set = NULL;
    assert(!gc_verifier->objects_set);
    
    pool_put_entry(verifier_metadata->hashcode_pool_before_gc, gc_verifier->hashcode_set);
    gc_verifier->hashcode_set = verifier_free_set_pool_get_entry(verifier_metadata->free_set_pool);
    assert(gc_verifier->hashcode_set);
    return;
  }else{
    pool_put_entry(verifier_metadata->resurrect_objects_pool_after_gc, gc_verifier->objects_set);
    gc_verifier->objects_set = NULL;
    assert(!gc_verifier->objects_set);
    
    pool_put_entry(verifier_metadata->hashcode_pool_after_gc, gc_verifier->hashcode_set);
    gc_verifier->hashcode_set = verifier_free_set_pool_get_entry(verifier_metadata->free_set_pool);
    assert(gc_verifier->hashcode_set);
    return;
  }

}

#ifdef USE_32BITS_HASHCODE
inline Object_Hashcode_Inform* verifier_copy_hashcode(Partial_Reveal_Object* p_obj, Heap_Verifier* heap_verifier, Boolean is_before_gc)  
{
  hash_obj_distance ++;
  
  if(!hashcode_is_set(p_obj))  return NULL;

  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;  
  if(is_before_gc) gc_verifier->num_hash_before_gc++;
  else gc_verifier->num_hash_after_gc++;

  Obj_Info_Type info = get_obj_info_raw(p_obj);
  int hash = 0;
  switch(info & HASHCODE_MASK){
    case HASHCODE_SET_UNALLOCATED:
      if(is_before_gc) gc_verifier->num_hash_set_unalloc_before_gc++;
      else gc_verifier->num_hash_set_unalloc_after_gc++;
      hash = hashcode_gen((void*)p_obj);
      break;
    case HASHCODE_SET_ATTACHED:
      if(is_before_gc) gc_verifier->num_hash_attached_before_gc++;
      else gc_verifier->num_hash_attached_after_gc++;
      hash = hashcode_lookup(p_obj,info);
      break;
    case HASHCODE_SET_BUFFERED:
      if(is_before_gc) gc_verifier->num_hash_buffered_before_gc++;
      else gc_verifier->num_hash_buffered_after_gc++;
      hash = hashcode_lookup(p_obj,info);
      break;
    default:
      assert(0);
  }

  unsigned int size = sizeof(Object_Hashcode_Inform);
  Object_Hashcode_Inform* obj_hash_info = (Object_Hashcode_Inform*) STD_MALLOC(size);
  assert(obj_hash_info);
  memset(obj_hash_info, 0, size);

  obj_hash_info->address = p_obj;
  obj_hash_info->hashcode = hash;
  obj_hash_info->hash_obj_distance = hash_obj_distance - 1;

  hash_obj_distance = 0;

  return obj_hash_info;
}
#else 
#define GCGEN_HASH_MASK 0x1fc
inline Object_Hashcode_Inform* verifier_copy_hashcode(Partial_Reveal_Object* p_obj, Heap_Verifier* heap_verifier, Boolean is_before_gc)  
{
  hash_obj_distance ++;
  
  Obj_Info_Type info = get_obj_info_raw(p_obj);

  int hash = info & GCGEN_HASH_MASK;

  if(!hash)  return NULL;

  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;  
  if(is_before_gc) gc_verifier->num_hash_before_gc++;
  else gc_verifier->num_hash_after_gc++;

  unsigned int size = sizeof(Object_Hashcode_Inform);
  Object_Hashcode_Inform* obj_hash_info = (Object_Hashcode_Inform*) STD_MALLOC(size);
  assert(obj_hash_info);
  memset(obj_hash_info, 0, size);

  obj_hash_info->address = p_obj;
  obj_hash_info->hashcode = hash;
  obj_hash_info->hash_obj_distance = hash_obj_distance - 1;

  hash_obj_distance = 0;

  return obj_hash_info;
}
#endif //USE_32BIT_HASHCODE

void verifier_update_verify_info(Partial_Reveal_Object* p_obj, Heap_Verifier* heap_verifier)
{
  if(!heap_verifier->need_verify_gc) return;
  Heap_Verifier_Metadata* verifier_metadata = heap_verifier->heap_verifier_metadata;
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;

#ifndef USE_UNIQUE_MARK_SWEEP_GC
  GC_Gen* gc = (GC_Gen*)heap_verifier->gc;
  Space* mspace = gc_get_mos(gc);
  Space* nspace = gc_get_nos(gc);
  Space* lspace  = gc_get_los(gc);

  if(!gc_verifier->is_before_fallback_collection && verifier_collect_is_minor(gc_verifier)){
    if(!heap_verifier->is_before_gc){
      assert(!obj_belongs_to_space(p_obj, nspace) || !fspace_object_was_forwarded(p_obj, (Fspace*)nspace, heap_verifier) || obj_belongs_to_survivor_area((Sspace*)nspace, p_obj));
      if(obj_belongs_to_space(p_obj, nspace) && fspace_object_was_forwarded(p_obj, (Fspace*)nspace, heap_verifier) && !obj_belongs_to_survivor_area((Sspace*)nspace, p_obj) ){
        gc_verifier->is_verification_passed = FALSE;
      }
    }
  }else if(!gc_verifier->is_before_fallback_collection){
    if(!heap_verifier->is_before_gc){
      assert(!obj_belongs_to_space(p_obj, nspace));
      if(obj_belongs_to_space(p_obj, nspace)){
        gc_verifier->is_verification_passed = FALSE;
      }
    }
  }
#else
  GC_MS* gc = (GC_MS*)heap_verifier->gc;
  if(!heap_verifier->is_before_gc){
    /*in GC_MS mark sweep algorithm, all live objects should be set their mark bit*/
    assert(obj_is_alloc_in_color_table(p_obj));
    if(!obj_is_alloc_in_color_table(p_obj))
      printf("\nERROR: obj after GC should be set its alloc color!\n");
  }else{
    if( !in_con_idle(heap_verifier->gc) )
      assert(obj_is_mark_black_in_table(p_obj));
  }
#endif

   /*store the object information*/
  void* p_obj_information =  verifier_copy_obj_information(p_obj);
  void* obj_hash_info = verifier_copy_hashcode(p_obj, heap_verifier, heap_verifier->is_before_gc);
   
#ifndef BUILD_IN_REFERENT
  if(!gc_verifier->is_tracing_resurrect_obj){
#endif    
    /*size and number*/
    if(heap_verifier->is_before_gc){
      verifier_set_push(p_obj_information, gc_verifier->objects_set, verifier_metadata->objects_pool_before_gc);
      if(obj_hash_info != NULL) verifier_set_push(obj_hash_info, gc_verifier->hashcode_set, verifier_metadata->hashcode_pool_before_gc);
      gc_verifier->num_live_objects_before_gc ++;
      gc_verifier->size_live_objects_before_gc += vm_object_size(p_obj);
    }else{
      verifier_set_push(p_obj_information, gc_verifier->objects_set, verifier_metadata->objects_pool_after_gc);
      if(obj_hash_info != NULL) verifier_set_push(obj_hash_info, gc_verifier->hashcode_set, verifier_metadata->hashcode_pool_after_gc);
      gc_verifier->num_live_objects_after_gc ++;
      gc_verifier->size_live_objects_after_gc += vm_object_size(p_obj);
    }
    return;
    
#ifndef BUILD_IN_REFERENT    
  }else{
    
    if(heap_verifier->is_before_gc){
      verifier_set_push(p_obj_information, gc_verifier->objects_set, verifier_metadata->resurrect_objects_pool_before_gc);
      if(obj_hash_info != NULL) verifier_set_push(obj_hash_info, gc_verifier->hashcode_set, verifier_metadata->hashcode_pool_before_gc);
      gc_verifier->num_resurrect_objects_before_gc ++;
      gc_verifier->size_resurrect_objects_before_gc += vm_object_size(p_obj);
    }else{
      verifier_set_push(p_obj_information, gc_verifier->objects_set, verifier_metadata->resurrect_objects_pool_after_gc);
      if(obj_hash_info != NULL) verifier_set_push(obj_hash_info, gc_verifier->hashcode_set, verifier_metadata->hashcode_pool_after_gc);
      gc_verifier->num_resurrect_objects_after_gc ++;
      gc_verifier->size_resurrect_objects_after_gc += vm_object_size(p_obj);
    }
    return;
    
  }
#endif

}

Boolean compare_live_obj_inform(POINTER_SIZE_INT* obj_container1,POINTER_SIZE_INT* obj_container2)
{
  Live_Object_Inform* obj_inform_1 = (Live_Object_Inform*)*obj_container1;
  Live_Object_Inform* obj_inform_2 = (Live_Object_Inform*)*obj_container2;
  Boolean ret=TRUE;
  
  if(((POINTER_SIZE_INT)obj_inform_1->vt_raw) == ((POINTER_SIZE_INT)obj_inform_2->vt_raw)){
    if(obj_inform_1->obj_info != obj_inform_2->obj_info) {
    	assert(0);
    	ret = FALSE;
        goto free_ref;
    }
    /*FIXME: erase live object information in compare_function. */
    if( object_has_ref_field((Partial_Reveal_Object*)obj_inform_1) ){
      Live_Object_Ref_Slot_Inform* obj_ref_inform_1 = (Live_Object_Ref_Slot_Inform*)obj_inform_1;
      Live_Object_Ref_Slot_Inform* obj_ref_inform_2 = (Live_Object_Ref_Slot_Inform*)obj_inform_2;
     
     if(obj_ref_inform_1->obj_info != obj_ref_inform_2->obj_info) {
    	assert(0);
    	ret = FALSE;
    	goto free_ref;
    } 
      if (object_is_array((Partial_Reveal_Object*)obj_ref_inform_1)){
        Partial_Reveal_Array* array = (Partial_Reveal_Array*)obj_ref_inform_2->address;
        unsigned int array_length = array->array_len;

        unsigned int i = 0;
        for(; i<array_length;i++){
          if((POINTER_SIZE_INT)obj_ref_inform_1->ref_slot[i] != (POINTER_SIZE_INT)obj_ref_inform_2->ref_slot[i]){
	    assert(0);
	    ret = FALSE;
	    goto free_ref;
          }
        }
      }else{

        unsigned int num_refs = object_ref_field_num((Partial_Reveal_Object*)(obj_ref_inform_2->address));
        
        unsigned int i = 0;
        for(; i<num_refs; i++){  
          if((POINTER_SIZE_INT)obj_ref_inform_1->ref_slot[i] != (POINTER_SIZE_INT)obj_ref_inform_2->ref_slot[i]){
            assert(0);
            ret = FALSE;
            goto free_ref;
          }
        }

      }
    }    
  }else{ 
    assert(0); 
    ret = FALSE;
  }
free_ref:
  STD_FREE(obj_inform_1);
  STD_FREE(obj_inform_2);
  return ret;
}

Boolean compare_obj_hash_inform(POINTER_SIZE_INT* container1,POINTER_SIZE_INT* container2)
{
  Object_Hashcode_Inform* obj_hash_1 = (Object_Hashcode_Inform*) *container1;
  Object_Hashcode_Inform* obj_hash_2 = (Object_Hashcode_Inform*) *container2;
  if(obj_hash_1->hashcode == obj_hash_2->hashcode && obj_hash_1->hash_obj_distance== obj_hash_2->hash_obj_distance){
    STD_FREE(obj_hash_1);
    STD_FREE(obj_hash_2);
    return TRUE;
  }else{ 
    assert(0);
    STD_FREE(obj_hash_1);
    STD_FREE(obj_hash_2);
    return FALSE;
  }
}

void verify_gc_effect(Heap_Verifier* heap_verifier)
{
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;
  
  Heap_Verifier_Metadata* verifier_metadata = heap_verifier->heap_verifier_metadata;
  Pool* free_pool = verifier_metadata->free_set_pool;

  Boolean passed = verifier_compare_objs_pools(verifier_metadata->objects_pool_before_gc, 
                    verifier_metadata->objects_pool_after_gc , free_pool, compare_live_obj_inform);
  if(!passed)     gc_verifier->is_verification_passed = FALSE;
#ifndef BUILD_IN_REFERENT
  passed = verifier_compare_objs_pools(verifier_metadata->resurrect_objects_pool_before_gc, 
                    verifier_metadata->resurrect_objects_pool_after_gc , free_pool, compare_live_obj_inform);
  if(!passed)     gc_verifier->is_verification_passed = FALSE;
#endif
  passed = verifier_compare_objs_pools(verifier_metadata->hashcode_pool_before_gc, 
                    verifier_metadata->hashcode_pool_after_gc , free_pool, compare_obj_hash_inform);
  if(!passed)     gc_verifier->is_verification_passed = FALSE;

  if(gc_verifier->num_live_objects_before_gc != gc_verifier->num_live_objects_after_gc){
    gc_verifier->is_verification_passed = FALSE;
    printf("\nERROR: live objects number error!\n");
    assert(0);
  }
  
  if(gc_verifier->size_live_objects_before_gc != gc_verifier->size_live_objects_after_gc){
    printf("\nERROR: live objects size error!\n");
    assert(0);
    gc_verifier->is_verification_passed = FALSE;
  }
  
#ifndef BUILD_IN_REFERENT  
  if(gc_verifier->num_resurrect_objects_before_gc != gc_verifier->num_resurrect_objects_after_gc){
    printf("\nERROR: resurrect objects number error!\n");
    assert(0);
    gc_verifier->is_verification_passed = FALSE;
  }
  
  if(gc_verifier->size_resurrect_objects_before_gc != gc_verifier->size_resurrect_objects_after_gc){
    printf("\nERROR: resurrect objects size error!\n"); 
    assert(0);
    gc_verifier->is_verification_passed = FALSE;
  }
#endif

  if(gc_verifier->num_hash_before_gc != gc_verifier->num_hash_after_gc){
    printf("\nERROR: hashcode number error\n");
    assert(0);
    gc_verifier->is_verification_passed = FALSE;
  }

}


void verifier_pool_clear_objs_mark_bit(Pool* marked_objs_pool)
{
  pool_iterator_init(marked_objs_pool);
  Vector_Block* objs_set = pool_iterator_next(marked_objs_pool);
  
  while(objs_set){
    POINTER_SIZE_INT* iter = vector_block_iterator_init(objs_set);
    while(!vector_block_iterator_end(objs_set,iter)){
      Live_Object_Inform* p_verify_obj = (Live_Object_Inform* )*iter;
      iter = vector_block_iterator_advance(objs_set,iter);

      Partial_Reveal_Object* p_obj = p_verify_obj->address;
      assert(p_obj != NULL); 
      assert(obj_is_marked_in_vt(p_obj));
      obj_unmark_in_vt(p_obj);
    } 
    objs_set = pool_iterator_next(marked_objs_pool);
  }
}

void verifier_clear_objs_mark_bit(Heap_Verifier* heap_verifier)
{
  Pool* marked_objs_pool = NULL;
  
  Heap_Verifier_Metadata* verifier_metadata = heap_verifier->heap_verifier_metadata;
  if(heap_verifier->is_before_gc) {
    verifier_pool_clear_objs_mark_bit(verifier_metadata->objects_pool_before_gc);
  #ifndef BUILD_IN_REFERENT
    verifier_pool_clear_objs_mark_bit(verifier_metadata->resurrect_objects_pool_before_gc);
  #endif
  }else{
    verifier_pool_clear_objs_mark_bit(verifier_metadata->objects_pool_after_gc);
  #ifndef BUILD_IN_REFERENT
    verifier_pool_clear_objs_mark_bit(verifier_metadata->resurrect_objects_pool_after_gc);
  #endif
  }
  return;
}


void verifier_reset_gc_verification(Heap_Verifier* heap_verifier)
{
  if(!heap_verifier->need_verify_gc) return;
  heap_verifier->gc_verifier->is_verification_passed = TRUE;
  verifier_copy_rootsets(heap_verifier->gc, heap_verifier);
}
void verifier_clear_gc_verification(Heap_Verifier* heap_verifier)
{
  verify_gc_reset(heap_verifier);  
  verifier_set_fallback_collection(heap_verifier->gc_verifier, FALSE);  
}

void verifier_reset_hash_distance()
{ hash_obj_distance = 0;}






