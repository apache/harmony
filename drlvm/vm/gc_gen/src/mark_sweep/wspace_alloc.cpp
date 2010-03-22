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

#include "wspace.h"
#include "wspace_chunk.h"
#include "wspace_alloc.h"
#include "gc_ms.h"
#include "../gen/gen.h"
#include "../common/gc_concurrent.h"


/* Only used in pfc_set_slot_index() */
inline unsigned int first_free_index_in_color_word(POINTER_SIZE_INT word, POINTER_SIZE_INT alloc_color)
{
  for(unsigned int index = 0; index < BITS_PER_WORD; index += COLOR_BITS_PER_OBJ)
    if(!(word & (alloc_color << index)))
      return index;
  
  assert(0);  /* There must be a free obj in this table word */
  return MAX_SLOT_INDEX;
}

/* Given an index word in table, set pfc's slot_index
 * The value of argument alloc_color can be cur_alloc_color or cur_mark_color.
 * It depends on in which phase this func is called.
 * In sweeping phase, wspace has been marked but alloc and mark colors have not been flipped,
 * so we have to use cur_mark_color as alloc_color.
 * In compaction phase, two colors have been flipped, so we use cur_alloc_color.
 */
void pfc_set_slot_index(Chunk_Header *chunk, unsigned int first_free_word_index, POINTER_SIZE_INT alloc_color)
{
  unsigned int index_in_word = first_free_index_in_color_word(chunk->table[first_free_word_index], alloc_color);
  assert(index_in_word != MAX_SLOT_INDEX);
  chunk->slot_index = composed_slot_index(first_free_word_index, index_in_word);
}

/* From the table's beginning search the first free slot, and set it to pfc's slot_index */
void pfc_reset_slot_index(Chunk_Header *chunk)
{
  POINTER_SIZE_INT *table = chunk->table;
  
  unsigned int index_word_num = (chunk->slot_num + SLOT_NUM_PER_WORD_IN_TABLE - 1) / SLOT_NUM_PER_WORD_IN_TABLE;
  for(unsigned int i=0; i<index_word_num; ++i){
    if(table[i] != cur_alloc_mask){
      pfc_set_slot_index(chunk, i, cur_alloc_color);
      return;
    }
  }
}

/* Alloc small without-fin object in wspace without getting new free chunk */
void *wspace_thread_local_alloc(unsigned size, Allocator *allocator)
{
  if(size > LARGE_OBJ_THRESHOLD) return NULL;
  
  Wspace *wspace = gc_get_wspace(allocator->gc);
  
  /* Flexible alloc mechanism:
  Size_Segment *size_seg = wspace_get_size_seg(wspace, size);
  unsigned int seg_index = size_seg->seg_index;
  */
  unsigned int seg_index = (size-GC_OBJECT_ALIGNMENT) / MEDIUM_OBJ_THRESHOLD;
  assert(seg_index <= 2);
  Size_Segment *size_seg = wspace->size_segments[seg_index];
  assert(size_seg->local_alloc);
  
  size = (unsigned int)NORMAL_SIZE_ROUNDUP(size, size_seg);
  unsigned int index = NORMAL_SIZE_TO_INDEX(size, size_seg);
  
  Chunk_Header **chunks = allocator->local_chunks[seg_index];
  Chunk_Header *chunk = chunks[index];  
  if(!chunk){
    mutator_post_signal((Mutator*) allocator,HSIG_DISABLE_SWEEP_LOCAL_CHUNKS);
    
    chunk = wspace_get_pfc(wspace, seg_index, index);
    //if(!chunk) chunk = wspace_steal_pfc(wspace, seg_index, index);
    if(!chunk){
      mutator_post_signal((Mutator*) allocator,HSIG_MUTATOR_SAFE);
      return NULL;
    }
    chunk->status |= CHUNK_IN_USE;
    chunks[index] = chunk;
    
    mutator_post_signal((Mutator*) allocator,HSIG_MUTATOR_SAFE);
  }
  
  mutator_post_signal((Mutator*) allocator,HSIG_MUTATOR_ENTER_ALLOC_MARK);
  void *p_obj = alloc_in_chunk(chunks[index]);
  mutator_post_signal((Mutator*) allocator,HSIG_MUTATOR_SAFE);

  if(chunk->slot_index == MAX_SLOT_INDEX){
    chunk->status = CHUNK_USED | CHUNK_NORMAL;
    /*register to used chunk list.*/
    wspace_reg_used_chunk(wspace,chunk);
    chunks[index] = NULL;
    chunk = NULL;
  }
  
  assert(!chunk || chunk->slot_index <= chunk->alloc_num);
  assert(!chunk || chunk->slot_index < chunk->slot_num);
  assert(p_obj);

#ifdef SSPACE_ALLOC_INFO
  wspace_alloc_info(size);
#endif
#ifdef SSPACE_VERIFY
  wspace_verify_alloc(p_obj, size);
#endif
 if(p_obj) {
   ((Mutator*)allocator)->new_obj_occupied_size+=size;
 }
  return p_obj;
}

static void *wspace_alloc_normal_obj(Wspace *wspace, unsigned size, Allocator *allocator)
{
  Size_Segment *size_seg = wspace_get_size_seg(wspace, size);
  unsigned int seg_index = size_seg->seg_index;
  
  size = (unsigned int)NORMAL_SIZE_ROUNDUP(size, size_seg);
  unsigned int index = NORMAL_SIZE_TO_INDEX(size, size_seg);
  
  Chunk_Header *chunk = NULL;
  void *p_obj = NULL;
  
  if(size_seg->local_alloc){
    Chunk_Header **chunks = allocator->local_chunks[seg_index];
    chunk = chunks[index];
    if(!chunk){
      mutator_post_signal((Mutator*) allocator,HSIG_DISABLE_SWEEP_LOCAL_CHUNKS);
      chunk = wspace_get_pfc(wspace, seg_index, index);
      if(!chunk){
        chunk = (Chunk_Header*)wspace_get_normal_free_chunk(wspace);
        if(chunk) normal_chunk_init(chunk, size);
      }
      //if(!chunk) chunk = wspace_steal_pfc(wspace, seg_index, index);
      if(!chunk){
        mutator_post_signal((Mutator*) allocator,HSIG_MUTATOR_SAFE);
	 //INFO2("gc.wspace", "[Local Alloc Failed] alloc obj with size" << size << " bytes" );
        return NULL;
      }
      chunk->status |= CHUNK_IN_USE;
      chunks[index] = chunk;
      mutator_post_signal((Mutator*) allocator,HSIG_MUTATOR_SAFE);
    }    
    
    mutator_post_signal((Mutator*) allocator,HSIG_MUTATOR_ENTER_ALLOC_MARK);
    p_obj = alloc_in_chunk(chunks[index]);
    mutator_post_signal((Mutator*) allocator,HSIG_MUTATOR_SAFE);
    
    if(chunk->slot_index == MAX_SLOT_INDEX){
      chunk->status = CHUNK_USED | CHUNK_NORMAL;
      /*register to used chunk list.*/
      wspace_reg_used_chunk(wspace,chunk);
      chunks[index] = NULL;
    }
    
  } else {  
    mutator_post_signal((Mutator*) allocator,HSIG_DISABLE_SWEEP_GLOBAL_CHUNKS);

    if(gc_is_specify_con_sweep()){
      while(gc_is_sweep_global_normal_chunk()){
        mutator_post_signal((Mutator*) allocator,HSIG_MUTATOR_SAFE);
      }  
    }

    chunk = wspace_get_pfc(wspace, seg_index, index);
    if(!chunk){
      chunk = (Chunk_Header*)wspace_get_normal_free_chunk(wspace);
      if(chunk) normal_chunk_init(chunk, size);
    }
    //if(!chunk) chunk = wspace_steal_pfc(wspace, seg_index, index);
    if(!chunk) {
      mutator_post_signal((Mutator*) allocator,HSIG_MUTATOR_SAFE);
      //INFO2("gc.wspace", "[Non-Local Alloc Failed] alloc obj with size" << size << " bytes" );
      return NULL;
    }
    p_obj = alloc_in_chunk(chunk);

    if(chunk->slot_index == MAX_SLOT_INDEX){
      chunk->status = CHUNK_USED | CHUNK_NORMAL;
      /*register to used chunk list.*/
      wspace_reg_used_chunk(wspace,chunk);
      chunk = NULL;
    }
    
    if(chunk){
      wspace_put_pfc(wspace, chunk);
    }
    
    mutator_post_signal((Mutator*) allocator,HSIG_MUTATOR_SAFE);
  }
  if(p_obj) {
  	((Mutator*)allocator)->new_obj_occupied_size+=size;
  }
  return p_obj;
}

static void *wspace_alloc_super_obj(Wspace *wspace, unsigned size, Allocator *allocator)
{
  assert(size > SUPER_OBJ_THRESHOLD);

  unsigned int chunk_size = SUPER_SIZE_ROUNDUP(size);
  assert(chunk_size > SUPER_OBJ_THRESHOLD);
  assert(!(chunk_size & CHUNK_GRANULARITY_LOW_MASK));
  
  Chunk_Header *chunk;
  if(chunk_size <= HYPER_OBJ_THRESHOLD)
    chunk = (Chunk_Header*)wspace_get_abnormal_free_chunk(wspace, chunk_size);
  else
    chunk = (Chunk_Header*)wspace_get_hyper_free_chunk(wspace, chunk_size, FALSE);
  
  if(!chunk) return NULL;
  abnormal_chunk_init(chunk, chunk_size, size);
  
  mutator_post_signal((Mutator*) allocator,HSIG_MUTATOR_ENTER_ALLOC_MARK);  
  if(is_obj_alloced_live()){
    chunk->table[0] |= cur_mark_black_color; // just for debugging, mark new object
  } 
  mutator_post_signal((Mutator*) allocator,HSIG_MUTATOR_SAFE);
  
  chunk->table[0] |= cur_alloc_color;
  set_super_obj_mask(chunk->base);
  assert(chunk->status == CHUNK_ABNORMAL);
  chunk->status = CHUNK_ABNORMAL| CHUNK_USED;
  wspace_reg_used_chunk(wspace, chunk);
  assert(get_obj_info_raw((Partial_Reveal_Object*)chunk->base) & SUPER_OBJ_MASK);
  
  ((Mutator*)allocator)->new_obj_occupied_size+=chunk_size;
  return chunk->base;
}

static void *wspace_try_alloc(unsigned size, Allocator *allocator)
{
  Wspace *wspace = gc_get_wspace(allocator->gc);
  void *p_obj = NULL;
  
  if(size <= SUPER_OBJ_THRESHOLD)
    p_obj = wspace_alloc_normal_obj(wspace, size, allocator);
  else
    p_obj = wspace_alloc_super_obj(wspace, size, allocator);

#ifdef SSPACE_ALLOC_INFO
  if(p_obj) wspace_alloc_info(size);
#endif
#ifdef SSPACE_VERIFY
  if(p_obj) wspace_verify_alloc(p_obj, size);
#endif

#ifdef WSPACE_CONCURRENT_GC_STATS
  if(p_obj && gc_con_is_in_marking()) ((Partial_Reveal_Object*)p_obj)->obj_info |= NEW_OBJ_MASK;
#endif

 
  return p_obj;
}

Free_Chunk_List *get_hyper_free_chunk_list();

/* FIXME:: the collection should be seperated from the alloation */
void *wspace_alloc(unsigned size, Allocator *allocator)
{
  void *p_obj = NULL;
  /*
  if( get_hyper_free_chunk_list()->head == NULL )
  	INFO2("gc.wspace", "[BEFORE ALLOC]hyper free chunk is EMPTY!!");
  */
  
  if(gc_is_specify_con_gc())
    gc_sched_collection(allocator->gc, GC_CAUSE_CONCURRENT_GC);
  
  /* First, try to allocate object from TLB (thread local chunk) */
  p_obj = wspace_try_alloc(size, allocator);
  if(p_obj){
    ((Mutator*)allocator)->new_obj_size += size;
    /*
    if( get_hyper_free_chunk_list()->head == NULL )
  	INFO2("gc.wspace", "[AFTER FIRST ALLOC]hyper free chunk is EMPTY!!");
    */
    return p_obj;
  }
  
  if(allocator->gc->in_collection) return NULL;
  
  vm_gc_lock_enum();
  /* after holding lock, try if other thread collected already */
  p_obj = wspace_try_alloc(size, allocator);
  if(p_obj){
    vm_gc_unlock_enum();
    ((Mutator*)allocator)->new_obj_size += size;
    /*
    if( get_hyper_free_chunk_list()->head == NULL )
  	INFO2("gc.wspace", "[AFTER SECOND ALLOC]hyper free chunk is EMPTY!!");
    */
    return p_obj;
  }
  
  INFO2("gc.con.info", "[Exhausted Cause] Allocation size is :" << size << " bytes");
  GC *gc = allocator->gc;
  /*
  gc->cause = GC_CAUSE_MOS_IS_FULL;
  if(gc_is_specify_con_gc())
    gc_relaim_heap_con_mode(gc);
  else*/ 
  gc_reclaim_heap(gc, GC_CAUSE_MOS_IS_FULL);
  vm_gc_unlock_enum();

#ifdef SSPACE_CHUNK_INFO
  printf("Failure size: %x\n", size);
#endif

  p_obj = wspace_try_alloc(size, allocator);
  /*
  if( get_hyper_free_chunk_list()->head == NULL )
  	INFO2("gc.wspace", "[AFTER COLLECTION ALLOC]hyper free chunk is EMPTY!!");
  */
  if(p_obj) ((Mutator*)allocator)->new_obj_size += size;
  
  return p_obj;
}
