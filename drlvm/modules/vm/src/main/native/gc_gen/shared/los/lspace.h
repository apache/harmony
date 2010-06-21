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
 * @author Ji Qi, 2006/10/05
 */

#ifndef _LSPACE_H_
#define _LSPACE_H_

#include "../common/gc_common.h"
#include "../thread/gc_thread.h"
#include "free_area_pool.h"
#ifdef USE_32BITS_HASHCODE
#include "../common/hashcode.h"
#endif

/*Fixme: This macro is for handling HEAP_BASE issues caused by JIT OPT*/
#ifdef COMPRESS_REFERENCE
  #define LOS_HEAD_RESERVE_FOR_HEAP_BASE ( SPACE_ALLOC_UNIT )
#else
  #define LOS_HEAD_RESERVE_FOR_HEAP_BASE ( 0*KB )
#endif

typedef struct Lspace{
  /* <-- first couple of fields are overloadded as Space */
  void* heap_start;
  void* heap_end;
  POINTER_SIZE_INT reserved_heap_size;
  POINTER_SIZE_INT committed_heap_size;
  unsigned int num_collections;
  int64 time_collections;
  float survive_ratio;
  unsigned int collect_algorithm;  
  GC* gc;
  /*LOS_Shrink:This field stands for sliding compact to lspace */
  Boolean move_object;

  Space_Statistics* space_statistic; 

  /* Size allocted since last collection. */
  volatile uint64 last_alloced_size;
  /* Size allocted since last major collection. */
  uint64 accumu_alloced_size;
  /* Total size allocated since VM starts. */
  uint64 total_alloced_size;

  /* Size survived from last collection. */
  uint64 last_surviving_size;
  /* Size survived after a certain period. */
  uint64 period_surviving_size;  
  /* END of Space --> */

  Free_Area_Pool* free_pool;
  /*Size of allocation which caused lspace alloc failure.
   *This one is used to assign area to failed collection inside gc.
   *Resetted in every gc_assign_free_area_to_mutators
   */
  POINTER_SIZE_INT failure_size;
  void* success_ptr;
  
  void* scompact_fa_start;
  void* scompact_fa_end;
}Lspace;

Lspace *lspace_initialize(GC* gc, void* reserved_base, POINTER_SIZE_INT lspace_size);
void lspace_destruct(Lspace* lspace);
Managed_Object_Handle lspace_alloc(unsigned size, Allocator* allocator);
void* lspace_try_alloc(Lspace* lspace, POINTER_SIZE_INT alloc_size);
void lspace_sliding_compact(Collector* collector, Lspace* lspace);
void lspace_compute_object_target(Collector* collector, Lspace* lspace);
void lspace_reset_for_slide(Lspace* lspace);
void lspace_collection(Lspace* lspace);

inline POINTER_SIZE_INT lspace_free_memory_size(Lspace* lspace)
{
  if(!lspace) return 0;
  /* FIXME:: */
  assert(lspace->committed_heap_size >= (POINTER_SIZE_INT)lspace->last_surviving_size + (POINTER_SIZE_INT)lspace->last_alloced_size);
  return (lspace->committed_heap_size - (POINTER_SIZE_INT)lspace->last_surviving_size - (POINTER_SIZE_INT)lspace->last_alloced_size);
}

inline POINTER_SIZE_INT lspace_committed_size(Lspace* lspace)
{
  if(lspace)
    return lspace->committed_heap_size;
  else
    return 0;
}

inline Partial_Reveal_Object* lspace_get_next_marked_object( Lspace* lspace, unsigned int* iterate_index)
{
    POINTER_SIZE_INT next_area_start = (POINTER_SIZE_INT)lspace->heap_start + (*iterate_index) * KB;
    BOOLEAN reach_heap_end = 0;
    unsigned int hash_extend_size = 0;

    while(!reach_heap_end){
        //FIXME: This while shoudl be if, try it!
        while((next_area_start< (POINTER_SIZE_INT)lspace->heap_end)&&!*((POINTER_SIZE_INT*)next_area_start)){
            assert(((Free_Area*)next_area_start)->size);
            next_area_start += ((Free_Area*)next_area_start)->size;            
        }
        if(next_area_start < (POINTER_SIZE_INT)lspace->heap_end){
            //If there is a living object at this addr, return it, and update iterate_index

#ifdef USE_32BITS_HASHCODE
            hash_extend_size  = (hashcode_is_attached((Partial_Reveal_Object*)next_area_start))?GC_OBJECT_ALIGNMENT:0;
#endif

            if(obj_is_marked_in_vt((Partial_Reveal_Object*)next_area_start)){
                POINTER_SIZE_INT obj_size = ALIGN_UP_TO_KILO(vm_object_size((Partial_Reveal_Object*)next_area_start) + hash_extend_size);
                *iterate_index = (unsigned int)((next_area_start + obj_size - (POINTER_SIZE_INT)lspace->heap_start) >> BIT_SHIFT_TO_KILO);
                return (Partial_Reveal_Object*)next_area_start;
            //If this is a dead object, go on to find  a living one.
            }else{
                POINTER_SIZE_INT obj_size = ALIGN_UP_TO_KILO(vm_object_size((Partial_Reveal_Object*)next_area_start)+ hash_extend_size);
                next_area_start += obj_size;
            }
        }else{
            reach_heap_end = 1;
        } 
    }
    return NULL;

}

inline Partial_Reveal_Object* lspace_get_first_marked_object(Lspace* lspace, unsigned int* mark_bit_idx)
{
    return lspace_get_next_marked_object(lspace, mark_bit_idx);
}

void lspace_fix_after_copy_nursery(Collector* collector, Lspace* lspace);

void lspace_fix_repointed_refs(Collector* collector, Lspace* lspace);

POINTER_SIZE_INT lspace_get_failure_size(Lspace* lspace);

inline Partial_Reveal_Object* lspace_get_next_marked_object_by_oi( Lspace* lspace, unsigned int* iterate_index)
{
    POINTER_SIZE_INT next_area_start = (POINTER_SIZE_INT)lspace->heap_start + (*iterate_index) * KB;
    BOOLEAN reach_heap_end = 0;
    unsigned int hash_extend_size = 0;
   
while(!reach_heap_end){        
   //FIXME: This while shoudl be if, try it!
        while((next_area_start<(POINTER_SIZE_INT)lspace->heap_end) && !*((POINTER_SIZE_INT*)next_area_start)){
            assert(((Free_Area*)next_area_start)->size);
            next_area_start += ((Free_Area*)next_area_start)->size;    
        }
        if(next_area_start < (POINTER_SIZE_INT)lspace->heap_end){
            //If there is a living object at this addr, return it, and update iterate_index

#ifdef USE_32BITS_HASHCODE
            hash_extend_size  = (hashcode_is_attached((Partial_Reveal_Object*)next_area_start))?GC_OBJECT_ALIGNMENT:0;
#endif

            if(obj_is_marked_in_oi((Partial_Reveal_Object*)next_area_start)){
                POINTER_SIZE_INT obj_size = ALIGN_UP_TO_KILO(vm_object_size((Partial_Reveal_Object*)next_area_start) + hash_extend_size);
                *iterate_index = (unsigned int)((next_area_start + obj_size - (POINTER_SIZE_INT)lspace->heap_start) >> BIT_SHIFT_TO_KILO);
                return (Partial_Reveal_Object*)next_area_start;
            //If this is a dead object, go on to find  a living one.
            }else{
                POINTER_SIZE_INT obj_size = ALIGN_UP_TO_KILO(vm_object_size((Partial_Reveal_Object*)next_area_start)+ hash_extend_size);
                next_area_start += obj_size;
            }
        }else{
            reach_heap_end = 1;
        } 
    }
    return NULL;

}

inline static Partial_Reveal_Object* lspace_get_first_marked_object_by_oi(Lspace* lspace, unsigned int* mark_bit_idx)
{
    return lspace_get_next_marked_object_by_oi(lspace, mark_bit_idx);
}

void lspace_reset_for_sweep(Lspace* lspace);
void lspace_sweep(Lspace* lspace);


#endif /*_LSPACE_H_ */
