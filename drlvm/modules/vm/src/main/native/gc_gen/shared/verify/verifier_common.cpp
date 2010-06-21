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
#include "verify_mutator_effect.h"

Boolean verifier_compare_objs_pools(Pool* objs_pool_before_gc, Pool* objs_pool_after_gc, Pool* free_pool ,Object_Comparator object_comparator)
{
  Vector_Block* objs_set_before_gc = pool_get_entry(objs_pool_before_gc);
  Vector_Block* objs_set_after_gc = pool_get_entry(objs_pool_after_gc);
  while(objs_set_before_gc && objs_set_after_gc){
    POINTER_SIZE_INT* iter_1 = vector_block_iterator_init(objs_set_before_gc);
    POINTER_SIZE_INT* iter_2 = vector_block_iterator_init(objs_set_after_gc);
    while(!vector_block_iterator_end(objs_set_before_gc, iter_1) 
                && !vector_block_iterator_end(objs_set_after_gc, iter_2) ){
      if(!(*object_comparator)(iter_1, iter_2)){
        assert(0);
        printf("\nERROR:    objs pools compare error!!!\n");
        return FALSE;
      }
      iter_1 = vector_block_iterator_advance(objs_set_before_gc, iter_1);
      iter_2 = vector_block_iterator_advance(objs_set_after_gc, iter_2);
    }
    if(!vector_block_iterator_end(objs_set_before_gc, iter_1) 
                || !vector_block_iterator_end(objs_set_after_gc, iter_2) )    
      return FALSE;
 
    vector_block_clear(objs_set_before_gc);
    vector_block_clear(objs_set_after_gc);
    pool_put_entry(free_pool, objs_set_before_gc);
    pool_put_entry(free_pool, objs_set_after_gc);
    objs_set_before_gc = pool_get_entry(objs_pool_before_gc);
    objs_set_after_gc = pool_get_entry(objs_pool_after_gc);
  }
  if(pool_is_empty(objs_pool_before_gc)&&pool_is_empty(objs_pool_before_gc)){
    return TRUE;
  }else{ 
    assert(0);
    return FALSE;
  }
}

Boolean verifier_copy_rootsets(GC* gc, Heap_Verifier* heap_verifier)
{
  Heap_Verifier_Metadata* verifier_metadata = heap_verifier->heap_verifier_metadata;
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;
  gc_verifier->root_set = verifier_free_set_pool_get_entry(verifier_metadata->free_set_pool);
  
  GC_Metadata* gc_metadata = gc->metadata;
  pool_iterator_init(gc_metadata->gc_rootset_pool);
  Vector_Block* root_set = pool_iterator_next(gc_metadata->gc_rootset_pool);
  
  while(root_set){    
    POINTER_SIZE_INT* iter = vector_block_iterator_init(root_set);
    while(!vector_block_iterator_end(root_set,iter)){
      REF* p_ref = (REF* )*iter;
      iter = vector_block_iterator_advance(root_set,iter);
      if( read_slot(p_ref) == NULL) continue;
      verifier_set_push(p_ref,gc_verifier->root_set,verifier_metadata->root_set_pool);
    } 
    root_set = pool_iterator_next(gc_metadata->gc_rootset_pool);
  }  
  pool_put_entry(verifier_metadata->root_set_pool, gc_verifier->root_set);
  
  gc_verifier->root_set = NULL;
  return TRUE;
}

Boolean verify_rootset_slot(REF* p_ref, Heap_Verifier* heap_verifier)
{
  Partial_Reveal_Object* p_obj = read_slot(p_ref);
  assert(address_belongs_to_gc_heap(p_obj,heap_verifier->gc));
#if !defined(USE_UNIQUE_MARK_SWEEP_GC) && !defined(USE_UNIQUE_MOVE_COMPACT_GC)
  GC_Gen* gc    = (GC_Gen*)heap_verifier->gc;
  Space* mspace = gc_get_mos(gc);
  Space* lspace  = gc_get_los(gc);
  Space* nos = gc_get_nos(gc);

  if(p_obj == NULL){
    if(collect_is_major() ||(!heap_verifier->gc_is_gen_mode && !NOS_PARTIAL_FORWARD)){
      assert(0);
      return FALSE;
    }else{
      return TRUE;
    }
  }
  if(!heap_verifier->gc_is_gen_mode){
    assert(!address_belongs_to_gc_heap(p_ref, heap_verifier->gc));
    if(address_belongs_to_gc_heap(p_ref, heap_verifier->gc)){
      printf("\nERROR: rootset address is inside gc heap\n");
      assert(0);
      return FALSE;
    }
  }
  assert(address_belongs_to_gc_heap(p_obj,heap_verifier->gc));
  if(heap_verifier->is_before_gc){
#endif
    //if(!address_belongs_to_gc_heap(p_ref) && address_belongs_to_gc_heap(p_obj)){
    if(!address_belongs_to_gc_heap(p_obj, heap_verifier->gc)){
      printf("\nERROR: obj referenced by rootset is outside the heap error!\n");
      assert(0);
      return FALSE;
    }
#if !defined(USE_UNIQUE_MARK_SWEEP_GC) && !defined(USE_UNIQUE_MOVE_COMPACT_GC)
  }else{
    if(heap_verifier->gc_verifier->is_before_fallback_collection){
      if(!address_belongs_to_gc_heap(p_obj, heap_verifier->gc)){
        printf("\nERROR: obj referenced by rootset is outside the heap error!\n");
        assert(0);
        return FALSE;
      }
      return TRUE;
    }

    if(!address_belongs_to_space(p_obj, mspace) && !address_belongs_to_space(p_obj, lspace) && !NOS_PARTIAL_FORWARD){
      if( collect_is_minor()){
        if( minor_is_semispace()){
          if( obj_belongs_to_survivor_area((Sspace*)nos, p_obj)) 
            return TRUE;            
        }
      }
      
      printf("\nERROR: obj referenced by rootset is in NOS after GC!\n");
      assert(0);
      return FALSE;
    }
  }
#endif
  return TRUE;
}


Boolean verifier_parse_options(Heap_Verifier* heap_verifier, char* options)
{
  char* verifier_options = options;
  char* option = NULL;
  for (option = strtok(verifier_options,","); option; option = strtok(NULL,",")) {
    string_to_upper(option);
    if(!strcmp(option, "ROOTSET")) heap_verifier->need_verify_rootset = TRUE;
    else if (!strcmp(option, "WRITEBARRIER")) heap_verifier->need_verify_writebarrier = TRUE;
    else if (!strcmp(option, "ALLOCATION")) heap_verifier->need_verify_allocation= TRUE;
    else if (!strcmp(option, "GC")) heap_verifier->need_verify_gc= TRUE;
    else if(!strcmp(option, "DEFAULT")){
      heap_verifier->need_verify_rootset = TRUE;
      heap_verifier->need_verify_writebarrier = TRUE;
      heap_verifier->need_verify_gc= TRUE;
    }else if(!strcmp(option, "ALL")){
      heap_verifier->need_verify_rootset = TRUE;
      heap_verifier->need_verify_writebarrier = TRUE;
      heap_verifier->need_verify_allocation= TRUE;
      heap_verifier->need_verify_gc= TRUE;
    }else{
      printf("Parse verify option error.\n");
      printf("Usage: -XX:gc.verify=rooset,writebarrier,allocation,gc \n");
      printf("Usage: -XX:gc.verify=default \n");
      printf("Usage: -XX:gc.verify=all \n");
      return FALSE;
    }
  }
  return TRUE;
}


void verifier_log_before_gc(Heap_Verifier* heap_verifier)
{
  Allocation_Verifier* alloc_verifier = heap_verifier->allocation_verifier;
  WriteBarrier_Verifier* wb_verifier = heap_verifier->writebarrier_verifier;
  RootSet_Verifier* rootset_verifier = heap_verifier->rootset_verifier;
  printf("\n\n");
  verifier_log_start("   Begin of GC ");
  printf(" collection number: %4d \n", heap_verifier->gc->num_collections);
  
  if(heap_verifier->need_verify_allocation){
    printf(" .......................................................................... \n");
    printf(" Allocation  Verify: %s , ", alloc_verifier->is_verification_passed?"passed":"failed");
    printf("new nos: %" FMT64 "u : %" FMT64 "u , ", alloc_verifier->num_nos_newobjs, alloc_verifier->num_nos_objs);
    printf("new los: %" FMT64 "u : %lld \n", alloc_verifier->num_los_newobjs, 
          alloc_verifier->num_los_objs-alloc_verifier->last_num_los_objs);
  }

  if(heap_verifier->need_verify_rootset){
    printf(" .......................................................................... \n");
    printf(" RootSet      Verify: %s, ", rootset_verifier->is_verification_passed?"passed":"failed");
    printf("num: %" FMT64 "u, ", rootset_verifier->num_slots_in_rootset);
    printf("error num: %" FMT64 "u \n", rootset_verifier->num_error_slots);

  }

  if(heap_verifier->need_verify_writebarrier){
    printf(" .......................................................................... \n");
    printf(" WriteBarrier Verify: %s, ", wb_verifier->is_verification_passed?"passed":"failed");
    printf("cached: %" FMT64 "u, ", wb_verifier->num_ref_wb_in_remset);
    printf("real : %" FMT64 "u \n", wb_verifier->num_ref_wb_after_scanning);
  }
}

void verifier_log_start(const char* message)
{
  printf("------------------------------%-16s------------------------------\n", message);
}

void verifier_collect_kind_log(Heap_Verifier* heap_verifier)
{
  GC* gc = heap_verifier->gc;
  const char* gc_kind;
  if(collect_is_minor()){ 
    gc_kind = " minor collection.";
  }else if(collect_is_fallback()){ 
    gc_kind = " fallback collection.";
  }else if(collect_is_major_normal()){ 
    if(gc->tuner->kind == TRANS_NOTHING)  gc_kind = "major collection (normal)";
    else if(gc->tuner->kind == TRANS_FROM_LOS_TO_MOS) gc_kind = "major collection (LOS shrink)";
    else if(gc->tuner->kind == TRANS_FROM_MOS_TO_LOS) gc_kind = "major collection (LOS extend)";
  }else if(major_is_marksweep()){
    gc_kind = " mark sweep collection.";
  }
  printf(" GC_kind: %s\n", gc_kind);
}

void verifier_hashcode_log(GC_Verifier* gc_verifier);
void verifier_log_after_gc(Heap_Verifier* heap_verifier)
{
  GC_Verifier* gc_verifier = heap_verifier->gc_verifier;
  if(heap_verifier->need_verify_gc){
    printf(" .......................................................................... \n");
    verifier_collect_kind_log(heap_verifier);
    printf(" GC Verify Result: %s  \n", gc_verifier->is_verification_passed?"Passed":"Failed*");
    printf(" .......................................................................... \n");
    printf(" %-14s:    %-7s |   Before %10" FMT64 "u   |   After %10" FMT64 "u   |\n", "live obj", "NUM" ,gc_verifier->num_live_objects_before_gc, gc_verifier->num_live_objects_after_gc);
    printf(" %-14s:    %-7s |   Before %7" FMT64 "u MB   |   After %7" FMT64 "u MB   |\n","live obj", "SIZE", gc_verifier->size_live_objects_before_gc>>20, gc_verifier->size_live_objects_after_gc>>20);
    printf(" %-14s:    %-7s |   Before %10" FMT64 "u   |   After %10" FMT64 "u   |\n", "resurrect obj", "NUM",gc_verifier->num_resurrect_objects_before_gc, gc_verifier->num_resurrect_objects_after_gc);
    if(gc_verifier->size_resurrect_objects_before_gc>>20 == 0 &&  gc_verifier->size_resurrect_objects_before_gc != 0){
      if(gc_verifier->size_resurrect_objects_before_gc>>10 == 0 ){
        printf(" %-14s:    %-7s |   Before %7" FMT64 "u  B   |   After %7" FMT64 "u  B   |\n", "resurrect obj", "SIZE", gc_verifier->size_resurrect_objects_before_gc, gc_verifier->size_resurrect_objects_after_gc);
      }else{  
        printf(" %-14s:    %-7s |   Before %7" FMT64 "u KB   |   After %7" FMT64 "u KB   |\n", "resurrect obj", "SIZE", gc_verifier->size_resurrect_objects_before_gc>>10, gc_verifier->size_resurrect_objects_after_gc>>10);
      }
    }else{
      printf(" %-14s:    %-7s |   Before %7" FMT64 "u MB   |   After %7" FMT64 "u MB   |\n", "resurrect obj", "SIZE", gc_verifier->size_resurrect_objects_before_gc>>20, gc_verifier->size_resurrect_objects_after_gc>>20);
    }
    verifier_hashcode_log(gc_verifier);
  }
  if(!heap_verifier->gc_verifier->is_before_fallback_collection)
    verifier_log_start("    End of GC   ");
  else
    verifier_log_start(" failed GC end ");

}

void verifier_hashcode_log(GC_Verifier* gc_verifier)
{
    printf(" %-14s:    %-7s |   Before %10" FMT64 "u   |   After %10" FMT64 "u   |\n", "hashcode", "NUM", gc_verifier->num_hash_before_gc, gc_verifier->num_hash_after_gc);
}





