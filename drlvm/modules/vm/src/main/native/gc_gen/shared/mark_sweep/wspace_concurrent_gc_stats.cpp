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

#ifdef WSPACE_CONCURRENT_GC_STATS

#include "wspace_alloc.h"
#include "wspace_mark_sweep.h"
#include "wspace_verify.h"
#include "gc_ms.h"
#include "../gen/gen.h"
#include "../thread/collector.h"
#include "../finalizer_weakref/finalizer_weakref.h"
#include "../common/fix_repointed_refs.h"
#include "../common/gc_concurrent.h"



static unsigned int total_object_num = 0;
static unsigned int total_live_obj_num = 0;
static unsigned int total_float_obj_num = 0;
static unsigned int total_live_new_obj_num = 0;
static unsigned int total_float_new_obj_num = 0;

static Chunk_Header_Basic *volatile next_chunk_for_scanning;


static void wspace_init_chunk_for_scanning(Wspace *wspace)
{
  next_chunk_for_scanning = (Chunk_Header_Basic*)space_heap_start((Space*)wspace);
}


static void normal_chunk_scanning(Chunk_Header *chunk)
{
  chunk->slot_index = 0;
  //chunk_depad_last_index_word(chunk);
  
  unsigned int alloc_num = chunk->alloc_num;
  assert(alloc_num);
  
  if(alloc_num == chunk->slot_num){  /* Filled with objects */
    unsigned int slot_size = chunk->slot_size;
    Partial_Reveal_Object *p_obj = (Partial_Reveal_Object*)slot_index_to_addr(chunk, 0);
    for(unsigned int i = alloc_num; i--;){
      total_object_num ++;
      if(obj_is_marked_in_vt(p_obj)){
        total_live_obj_num ++;
        if(p_obj->obj_info & NEW_OBJ_MASK){
          p_obj->obj_info &= ~NEW_OBJ_MASK;
          total_live_new_obj_num ++;
        }
      }else{
        total_float_obj_num ++;        
        if(p_obj->obj_info & NEW_OBJ_MASK){
          p_obj->obj_info &= ~NEW_OBJ_MASK;
          total_float_new_obj_num ++;
        }
      }
      p_obj = (Partial_Reveal_Object*)((POINTER_SIZE_INT)p_obj + slot_size);
    }
  } else {  /* Chunk is not full */
    while(alloc_num){
      Partial_Reveal_Object *p_obj = next_alloc_slot_in_chunk(chunk);

      total_object_num ++;
      if(obj_is_marked_in_vt(p_obj)){
        total_live_obj_num ++;
        if(p_obj->obj_info & NEW_OBJ_MASK){
          p_obj->obj_info &= ~NEW_OBJ_MASK;
          total_live_new_obj_num ++;
        }
      }else{
        total_float_obj_num ++;        
        if(p_obj->obj_info & NEW_OBJ_MASK){
          p_obj->obj_info &= ~NEW_OBJ_MASK;
          total_float_new_obj_num ++;
        }
      }
      
      assert(p_obj);
      --alloc_num;
    }
  }
  
  if(chunk->alloc_num != chunk->slot_num){
    //chunk_pad_last_index_word(chunk, cur_alloc_mask);
    pfc_reset_slot_index(chunk);
  }

}

static void abnormal_chunk_scanning(Chunk_Header *chunk)
{
  Partial_Reveal_Object* p_obj = (Partial_Reveal_Object*)chunk->base;
  total_object_num ++;
  if(obj_is_marked_in_vt(p_obj)){
    total_live_obj_num ++;
    if(p_obj->obj_info & NEW_OBJ_MASK){
      p_obj->obj_info &= ~NEW_OBJ_MASK;
      total_live_new_obj_num ++;
    }
  }else{
    total_float_obj_num ++;        
    if(p_obj->obj_info & NEW_OBJ_MASK){
      p_obj->obj_info &= ~NEW_OBJ_MASK;
      total_float_new_obj_num ++;
    }
  }
}

void wspace_scan_heap(GC* gc)
{
  Wspace *wspace = gc_get_wspace(gc);
  total_object_num = 0;
  total_live_obj_num = 0;
  total_float_obj_num = 0;
  total_live_new_obj_num = 0;
  total_float_new_obj_num = 0;

  wspace_init_chunk_for_scanning(wspace);

  Chunk_Header_Basic *chunk = wspace_grab_next_chunk(wspace, &next_chunk_for_scanning, FALSE);
  
  while(chunk){
    if(chunk->status & CHUNK_NORMAL)
      normal_chunk_scanning((Chunk_Header*)chunk);
    else if(chunk->status & CHUNK_ABNORMAL)
      abnormal_chunk_scanning((Chunk_Header*)chunk);
    
    chunk = wspace_grab_next_chunk(wspace, &next_chunk_for_scanning, FALSE);
  }

  printf("total_object_num: %d \n", total_object_num);
  printf("total_live_obj_num: %d \n", total_live_obj_num);
  printf("total_float_obj_num: %d \n", total_float_obj_num);
  printf("total_live_new_obj_num: %d \n", total_live_new_obj_num);
  printf("total_float_new_obj_num: %d \n", total_float_new_obj_num);
}



static void normal_chunk_clear(Chunk_Header *chunk)
{
  chunk->slot_index = 0;
  //chunk_depad_last_index_word(chunk);
  
  unsigned int alloc_num = chunk->alloc_num;
  assert(alloc_num);
  
  if(alloc_num == chunk->slot_num){  /* Filled with objects */
    unsigned int slot_size = chunk->slot_size;
    Partial_Reveal_Object *p_obj = (Partial_Reveal_Object*)slot_index_to_addr(chunk, 0);
    for(unsigned int i = alloc_num; i--;){
        if(p_obj->obj_info & NEW_OBJ_MASK){
          p_obj->obj_info &= ~NEW_OBJ_MASK;
        }
    }
  } else {  /* Chunk is not full */
    while(alloc_num){
      Partial_Reveal_Object *p_obj = next_alloc_slot_in_chunk(chunk);
      if(p_obj->obj_info & NEW_OBJ_MASK){
        p_obj->obj_info &= ~NEW_OBJ_MASK;
        total_live_new_obj_num ++;
      }      
      assert(p_obj);
      --alloc_num;
    }
  }
  
  if(chunk->alloc_num != chunk->slot_num){
    //chunk_pad_last_index_word(chunk, cur_alloc_mask);
    pfc_reset_slot_index(chunk);
  }

}

static void abnormal_chunk_clear(Chunk_Header *chunk)
{
  Partial_Reveal_Object* p_obj = (Partial_Reveal_Object*)chunk->base;
    if(p_obj->obj_info & NEW_OBJ_MASK){
      p_obj->obj_info &= ~NEW_OBJ_MASK;
    }
}

void wspace_clear_heap(GC* gc)
{
  Wspace *wspace = gc_get_wspace(gc);

  wspace_init_chunk_for_scanning(wspace);

  Chunk_Header_Basic *chunk = wspace_grab_next_chunk(wspace, &next_chunk_for_scanning, FALSE);
  
  while(chunk){
    if(chunk->status & CHUNK_NORMAL)
      normal_chunk_clear((Chunk_Header*)chunk);
    else if(chunk->status & CHUNK_ABNORMAL)
      abnormal_chunk_clear((Chunk_Header*)chunk);
    
    chunk = wspace_grab_next_chunk(wspace, &next_chunk_for_scanning, FALSE);
  }

}
#endif



