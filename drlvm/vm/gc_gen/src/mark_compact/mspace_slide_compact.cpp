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

#include "mspace_collect_compact.h"
#include "../los/lspace.h"
#include "../finalizer_weakref/finalizer_weakref.h"

#ifdef GC_GEN_STATS
#include "../gen/gen_stats.h"
#endif

struct GC_Gen;
Space* gc_get_nos(GC_Gen* gc);
Space* gc_get_mos(GC_Gen* gc);
Space* gc_get_los(GC_Gen* gc);

static volatile Block_Header *last_block_for_dest;

static void mspace_compute_object_target(Collector* collector, Mspace* mspace)
{  
  Block_Header *curr_block = collector->cur_compact_block;
  Block_Header *dest_block = collector->cur_target_block;
  Block_Header *local_last_dest = dest_block;
  void *dest_addr = dest_block->base;
  Block_Header *last_src;

#ifdef USE_32BITS_HASHCODE
  Hashcode_Buf* old_hashcode_buf = NULL;
  Hashcode_Buf* new_hashcode_buf = hashcode_buf_create();
  hashcode_buf_init(new_hashcode_buf);
#endif
  
  assert(!collector->rem_set);
  collector->rem_set = free_set_pool_get_entry(collector->gc->metadata);
#ifdef USE_32BITS_HASHCODE  
  collector->hashcode_set = free_set_pool_get_entry(collector->gc->metadata);
#endif

#ifdef GC_GEN_STATS
  GC_Gen_Collector_Stats* stats = (GC_Gen_Collector_Stats*)collector->stats;
#endif

  while( curr_block ){
    void* start_pos;
    Partial_Reveal_Object *first_obj = block_get_first_marked_obj_prefetch_next(curr_block, &start_pos);
    if(first_obj){
      ++curr_block->dest_counter;
      if(!dest_block->src)
        dest_block->src = first_obj;
      else
        last_src->next_src = first_obj;
      last_src = curr_block;
    }
    Partial_Reveal_Object* p_obj = first_obj;
 
    while( p_obj ){
      assert( obj_is_marked_in_vt(p_obj));

      unsigned int obj_size = (unsigned int)((POINTER_SIZE_INT)start_pos - (POINTER_SIZE_INT)p_obj);
      

#ifdef GC_GEN_STATS
      gc_gen_collector_update_moved_nos_mos_obj_stats_major(stats, obj_size);
#endif

      Obj_Info_Type obj_info = get_obj_info(p_obj);
      
      unsigned int obj_size_precompute = obj_size;
      
#ifdef USE_32BITS_HASHCODE
      precompute_hashcode_extend_size(p_obj, dest_addr, &obj_size_precompute);
#endif
      if( ((POINTER_SIZE_INT)dest_addr + obj_size_precompute) > (POINTER_SIZE_INT)GC_BLOCK_END(dest_block)){
#ifdef USE_32BITS_HASHCODE      
        block_swap_hashcode_buf(dest_block, &new_hashcode_buf, &old_hashcode_buf);
#endif        
        dest_block->new_free = dest_addr;
        dest_block = mspace_get_next_target_block(collector, mspace);
        if(dest_block == NULL){ 
          collector->result = FALSE; 
          return; 
        }
        if((!local_last_dest) || (dest_block->block_idx > local_last_dest->block_idx))
          local_last_dest = dest_block;
        dest_addr = dest_block->base;
        dest_block->src = p_obj;
        last_src = curr_block;
        if(p_obj != first_obj)
          ++curr_block->dest_counter;
      }
      assert(((POINTER_SIZE_INT)dest_addr + obj_size) <= (POINTER_SIZE_INT)GC_BLOCK_END(dest_block));
      
#ifdef USE_32BITS_HASHCODE      
       obj_info = slide_compact_process_hashcode(p_obj, dest_addr, &obj_size, collector,curr_block->hashcode_buf, new_hashcode_buf);
#endif      

      if( obj_info != 0 ) {
        collector_remset_add_entry(collector, (Partial_Reveal_Object **)dest_addr);
        collector_remset_add_entry(collector, (Partial_Reveal_Object **)(POINTER_SIZE_INT)obj_info);
      }
      
      obj_set_fw_in_oi(p_obj, dest_addr);
      
      /* FIXME: should use alloc to handle alignment requirement */
      dest_addr = (void *)((POINTER_SIZE_INT) dest_addr + obj_size);
      p_obj = block_get_next_marked_obj_prefetch_next(curr_block, &start_pos);
    }
 #ifdef USE_32BITS_HASHCODE      
    hashcode_buf_clear(curr_block->hashcode_buf);
 #endif    
    curr_block = mspace_get_next_compact_block(collector, mspace);
  
  }
  
#ifdef USE_32BITS_HASHCODE 
  pool_put_entry(collector->gc->metadata->collector_hashcode_pool, collector->hashcode_set);
  collector->hashcode_set = NULL;
#endif
  pool_put_entry(collector->gc->metadata->collector_remset_pool, collector->rem_set);
  collector->rem_set = NULL;
  dest_block->new_free = dest_addr;
  
  Block_Header *cur_last_dest = (Block_Header *)last_block_for_dest;
  collector->cur_target_block = local_last_dest;
  while((local_last_dest)&&((!cur_last_dest) || (local_last_dest->block_idx > cur_last_dest->block_idx))){
    atomic_casptr((volatile void **)&last_block_for_dest, local_last_dest, cur_last_dest);
    cur_last_dest = (Block_Header *)last_block_for_dest;
  }
  
#ifdef USE_32BITS_HASHCODE
  old_hashcode_buf = block_set_hashcode_buf(dest_block, new_hashcode_buf);
  hashcode_buf_destory(old_hashcode_buf);
#endif
  return;
}   

#include "../common/fix_repointed_refs.h"

static void mspace_fix_repointed_refs(Collector *collector, Mspace *mspace)
{
  Block_Header *curr_block = blocked_space_block_iterator_next((Blocked_Space*)mspace);
  
  /* for ALGO_MAJOR, we must iterate over all compact blocks */
  while( curr_block){
    block_fix_ref_after_repointing(curr_block); 
    curr_block = blocked_space_block_iterator_next((Blocked_Space*)mspace);
  }

  return;
}

typedef struct{
  volatile Block_Header *block;
  SpinLock lock;
} Cur_Dest_Block;

static Cur_Dest_Block current_dest_block;
static volatile Block_Header *next_block_for_dest;

static inline Block_Header *set_next_block_for_dest(Mspace *mspace)
{
  assert(!next_block_for_dest);
  
  Block_Header *block = blocked_space_block_iterator_get((Blocked_Space*)mspace);
  
  if(block->status != BLOCK_DEST)
    return block;
  
  while(block->status == BLOCK_DEST) {
    block = block->next;
    if(!block) break;
  }
  next_block_for_dest = block;
  return block;
}

#define DEST_NOT_EMPTY ((Block_Header *)0xFF)

static Block_Header *get_next_dest_block(Mspace *mspace)
{
  Block_Header *cur_dest_block;
  
  if(next_block_for_dest){
    cur_dest_block = (Block_Header*)next_block_for_dest;
    while(cur_dest_block->status == BLOCK_DEST){
      cur_dest_block = cur_dest_block->next;
      if(!cur_dest_block) break;
    }
    next_block_for_dest = cur_dest_block;
  } else {
    cur_dest_block = set_next_block_for_dest(mspace);
  }
  
  unsigned int total_dest_counter = 0;
  /*For LOS_Shrink: last_dest_block might point to a fake block*/
  Block_Header *last_dest_block = 
        (Block_Header *)round_down_to_size((POINTER_SIZE_INT)(last_block_for_dest->base), GC_BLOCK_SIZE_BYTES);
  for(; cur_dest_block <= last_dest_block; cur_dest_block = cur_dest_block->next){
    if(!cur_dest_block)  return NULL;
    if(cur_dest_block->status == BLOCK_DEST){
      continue;
    }
    if(cur_dest_block->dest_counter == 0 && cur_dest_block->src){
      cur_dest_block->status = BLOCK_DEST;
      return cur_dest_block;
    } else if(cur_dest_block->dest_counter == 1 && GC_BLOCK_HEADER(cur_dest_block->src) == cur_dest_block){
      return cur_dest_block;
    } else if(cur_dest_block->dest_counter == 0 && !cur_dest_block->src){
      cur_dest_block->status = BLOCK_DEST;
    } else {
      total_dest_counter += cur_dest_block->dest_counter;
    }
  }
  
  if(total_dest_counter)
    return DEST_NOT_EMPTY;
  
  return NULL;
}

static Block_Header *check_dest_block(Mspace *mspace)
{
  Block_Header *cur_dest_block;
  
  if(next_block_for_dest){
    cur_dest_block = (Block_Header*)next_block_for_dest;
    while(cur_dest_block->status == BLOCK_DEST){
      cur_dest_block = cur_dest_block->next;
    }
  } else {
    cur_dest_block = blocked_space_block_iterator_get((Blocked_Space*)mspace);
  }

  unsigned int total_dest_counter = 0;
  Block_Header *last_dest_block = (Block_Header *)last_block_for_dest;
  for(; cur_dest_block < last_dest_block; cur_dest_block = cur_dest_block->next){
    if(cur_dest_block->status == BLOCK_DEST)
      continue;
    if(cur_dest_block->dest_counter == 0 && cur_dest_block->src){
      return cur_dest_block;
    } else if(cur_dest_block->dest_counter == 1 && GC_BLOCK_HEADER(cur_dest_block->src) == cur_dest_block){
      return cur_dest_block;
    } else if(cur_dest_block->dest_counter == 0 && !cur_dest_block->src){
      cur_dest_block->status = BLOCK_DEST;
    } else {
      total_dest_counter += cur_dest_block->dest_counter;
    }
  }
  
  if(total_dest_counter) return DEST_NOT_EMPTY;
  return NULL;
}

static inline Partial_Reveal_Object *get_next_first_src_obj(Mspace *mspace)
{
  Partial_Reveal_Object *first_src_obj;
  
  while(TRUE){
    lock(current_dest_block.lock);
    Block_Header *next_dest_block = (Block_Header *)current_dest_block.block;
    
    if (!next_dest_block || !(first_src_obj = next_dest_block->src)){
      next_dest_block = get_next_dest_block(mspace);
      if(!next_dest_block){
        unlock(current_dest_block.lock);
        return NULL;
      } else if(next_dest_block == DEST_NOT_EMPTY){
        unlock(current_dest_block.lock);
        while(check_dest_block(mspace)==DEST_NOT_EMPTY);
        continue;
      }
      first_src_obj = next_dest_block->src;
      if(next_dest_block->status == BLOCK_DEST){
        assert(!next_dest_block->dest_counter);
        current_dest_block.block = next_dest_block;
      }
    }
    
    Partial_Reveal_Object *next_src_obj = GC_BLOCK_HEADER(first_src_obj)->next_src;
    if(next_src_obj && GC_BLOCK_HEADER(ref_to_obj_ptr((REF)get_obj_info_raw(next_src_obj))) != next_dest_block){
      next_src_obj = NULL;
    }
    next_dest_block->src = next_src_obj;
    unlock(current_dest_block.lock);
    return first_src_obj;
  }
}

static inline void gc_init_block_for_fix_repointed_refs(GC* gc, Mspace* mspace)
{
  Space_Tuner* tuner = gc->tuner;
  POINTER_SIZE_INT tuning_size = tuner->tuning_size;
  /*If LOS_Shrink, we just fix the repointed refs from the start of old mspace.*/
  if((tuner->kind == TRANS_NOTHING) || (tuner->kind == TRANS_FROM_LOS_TO_MOS)){
    blocked_space_block_iterator_init((Blocked_Space*)mspace);
    return;
  }else{
    /*If LOS_Extend, we fix from the new start of mspace, because the block list is start from there.*/
    mspace->block_iterator = (Block_Header*)((POINTER_SIZE_INT)mspace->blocks + tuning_size);
  }  
  return;
}

static inline void gc_init_block_for_sliding_compact(GC *gc, Mspace *mspace)
{
  /* initialize related static variables */
  next_block_for_dest = NULL;
  current_dest_block.block = NULL;
  current_dest_block.lock = FREE_LOCK;

  Space_Tuner* tuner = gc->tuner;
  POINTER_SIZE_INT tuning_size = tuner->tuning_size;

  if( tuner->kind == TRANS_NOTHING ){
    /*If space is not tuned, we just start from mspace->heap_start.*/
    blocked_space_block_iterator_init((Blocked_Space*)mspace);
    return;
  }else if (tuner->kind == TRANS_FROM_MOS_TO_LOS){
    /*If LOS_Extend, we compact from the new start of mspace, because the block list is start from there.*/
    mspace->block_iterator = (Block_Header*)((POINTER_SIZE_INT)mspace->blocks + tuning_size);
  }else{
    /*If LOS_Shrink, we compact from the new start of mspace too. 
      *This is different from the operations in function gc_init_block_for_fix_repointed_refs, 
      *because we want to compact mspace to the new start.*/
    mspace->block_iterator = (Block_Header*)((POINTER_SIZE_INT)mspace->blocks - tuning_size);
  }

  return;
}

extern unsigned int mspace_free_block_idx;

static void mspace_sliding_compact(Collector* collector, Mspace* mspace)
{
  void *start_pos;
    
  while(Partial_Reveal_Object *p_obj = get_next_first_src_obj(mspace)){
    Block_Header *src_block = GC_BLOCK_HEADER(p_obj);
    assert(src_block->dest_counter);
    
    Partial_Reveal_Object *p_target_obj = obj_get_fw_in_oi(p_obj);
    Block_Header *dest_block = GC_BLOCK_HEADER(p_target_obj);
    
    /* We don't set start_pos as p_obj in case that memmove of this obj may overlap itself.
     * In that case we can't get the correct vt and obj_info.
     */
#ifdef USE_32BITS_HASHCODE
    start_pos = obj_end_extend(p_obj);
#else
    start_pos = obj_end(p_obj);
#endif
    
    do {
      assert(obj_is_marked_in_vt(p_obj));
#ifdef USE_32BITS_HASHCODE
      obj_clear_dual_bits_in_vt(p_obj);
#else
      obj_unmark_in_vt(p_obj);
#endif

       unsigned int obj_size = (unsigned int)((POINTER_SIZE_INT)start_pos - (POINTER_SIZE_INT)p_obj);
      if(p_obj != p_target_obj){
        assert((((POINTER_SIZE_INT)p_target_obj) % GC_OBJECT_ALIGNMENT) == 0);
        memmove(p_target_obj, p_obj, obj_size);
      }
      set_obj_info(p_target_obj, 0);
      
      p_obj = block_get_next_marked_obj_after_prefetch(src_block, &start_pos);
      if(!p_obj)
        break;
      p_target_obj = obj_get_fw_in_oi(p_obj);
    
    } while(GC_BLOCK_HEADER(p_target_obj) == dest_block);
    
    atomic_dec32(&src_block->dest_counter);
  }

}


static volatile unsigned int num_marking_collectors = 0;
static volatile unsigned int num_repointing_collectors = 0;
static volatile unsigned int num_fixing_collectors = 0;
static volatile unsigned int num_moving_collectors = 0;
static volatile unsigned int num_restoring_collectors = 0;
static volatile unsigned int num_extending_collectors = 0;

void slide_compact_mspace(Collector* collector) 
{
  GC* gc = collector->gc;
  Mspace* mspace = (Mspace*)gc_get_mos((GC_Gen*)gc);
  Lspace* lspace = (Lspace*)gc_get_los((GC_Gen*)gc);
  
  unsigned int num_active_collectors = gc->num_active_collectors;

  /* Pass 1: **************************************************
    *mark all live objects in heap, and save all the slots that 
    *have references  that are going to be repointed.
    */

  TRACE2("gc.process", "GC: collector["<<((POINTER_SIZE_INT)collector->thread_handle)<<"]: pass1: marking...");

  unsigned int old_num = atomic_cas32( &num_marking_collectors, 0, num_active_collectors+1);

  if(collect_is_fallback())
    mark_scan_heap_for_fallback(collector);
  else if(gc->tuner->kind != TRANS_NOTHING)
    mark_scan_heap_for_space_tune(collector);
  else
    mark_scan_heap(collector);
  old_num = atomic_inc32(&num_marking_collectors);

   /* last collector's world here */
  if( ++old_num == num_active_collectors ){

    if(!IGNORE_FINREF )
      collector_identify_finref(collector);
#ifndef BUILD_IN_REFERENT
    else {
      gc_set_weakref_sets(gc);
      gc_update_weakref_ignore_finref(gc);
    }
#endif
    gc_identify_dead_weak_roots(gc);

    if( gc->tuner->kind != TRANS_NOTHING ) gc_compute_space_tune_size_after_marking(gc);
    //assert(!(gc->tuner->tuning_size % GC_BLOCK_SIZE_BYTES));
    /* prepare for next phase */
    gc_init_block_for_collectors(gc, mspace);
    
#ifdef USE_32BITS_HASHCODE
    if(collect_is_fallback())
      fallback_clear_fwd_obj_oi_init(collector);
#endif

    last_block_for_dest = NULL;
    /* let other collectors go */
    num_marking_collectors++; 
  }
  while(num_marking_collectors != num_active_collectors + 1);

  TRACE2("gc.process", "GC: collector["<<((POINTER_SIZE_INT)collector->thread_handle)<<"]:  finish pass1 and start pass2: relocating mos&nos...");

  /* Pass 2: **************************************************
     assign target addresses for all to-be-moved objects */

  atomic_cas32( &num_repointing_collectors, 0, num_active_collectors+1);

#ifdef USE_32BITS_HASHCODE
  if(collect_is_fallback())
    fallback_clear_fwd_obj_oi(collector);
#endif
  mspace_compute_object_target(collector, mspace);
  
  old_num = atomic_inc32(&num_repointing_collectors);
  /*last collector's world here*/
  if( ++old_num == num_active_collectors ){
    if(lspace->move_object) {
      TRACE2("gc.process", "GC: collector["<<((POINTER_SIZE_INT)collector->thread_handle)<<"]: pass2: relocating los ...");
      lspace_compute_object_target(collector, lspace);
    }
    gc->collect_result = gc_collection_result(gc);
    if(!gc->collect_result){
      num_repointing_collectors++;
      return;
    }
    gc_reset_block_for_collectors(gc, mspace);
    gc_init_block_for_fix_repointed_refs(gc, mspace);
    num_repointing_collectors++; 
  }
  while(num_repointing_collectors != num_active_collectors + 1);
  if(!gc->collect_result) return;
  TRACE2("gc.process", "GC: collector["<<((POINTER_SIZE_INT)collector->thread_handle)<<"]: finish pass2 and start pass3: repointing...");

  /* Pass 3: **************************************************
    *update all references whose objects are to be moved
    */
  old_num = atomic_cas32( &num_fixing_collectors, 0, num_active_collectors+1);
  mspace_fix_repointed_refs(collector, mspace);
  old_num = atomic_inc32(&num_fixing_collectors);
  /*last collector's world here */
  if( ++old_num == num_active_collectors ){
    lspace_fix_repointed_refs(collector, lspace);
    gc_fix_rootset(collector, FALSE);
    gc_init_block_for_sliding_compact(gc, mspace);
    /*LOS_Shrink: This operation moves objects in LOS, and should be part of Pass 4
      *lspace_sliding_compact is not binded with los shrink, we could slide compact los individually.
      *So we use a flag lspace->move_object here, not tuner->kind == TRANS_FROM_LOS_TO_MOS.
      */
    if(lspace->move_object)  lspace_sliding_compact(collector, lspace);
    /*The temp blocks for storing interim infomation is copied to the real place they should be.
      *And the space of the blocks are freed, which is alloced in gc_space_tuner_init_fake_blocks_for_los_shrink.
      */
    last_block_for_dest = (Block_Header *)round_down_to_size((POINTER_SIZE_INT)last_block_for_dest->base, GC_BLOCK_SIZE_BYTES);
    if(gc->tuner->kind == TRANS_FROM_LOS_TO_MOS) gc_space_tuner_release_fake_blocks_for_los_shrink(gc);
    num_fixing_collectors++;
  }
  while(num_fixing_collectors != num_active_collectors + 1);

  TRACE2("gc.process", "GC: collector["<<((POINTER_SIZE_INT)collector->thread_handle)<<"]: finish pass3 and start pass4: moving...");

  /* Pass 4: **************************************************
     move objects                                             */

  atomic_cas32( &num_moving_collectors, 0, num_active_collectors);
  
  mspace_sliding_compact(collector, mspace); 
  
  atomic_inc32(&num_moving_collectors);
  while(num_moving_collectors != num_active_collectors);

  TRACE2("gc.process", "GC: collector["<<((POINTER_SIZE_INT)collector->thread_handle)<<"]: finish pass4 and start pass 5: restoring obj_info...");

  /* Pass 5: **************************************************
     restore obj_info                                         */

  atomic_cas32( &num_restoring_collectors, 0, num_active_collectors+1);
  
  collector_restore_obj_info(collector);
#ifdef USE_32BITS_HASHCODE
  collector_attach_hashcode(collector);
#endif
  
  old_num = atomic_inc32(&num_restoring_collectors);

  if( ++old_num == num_active_collectors ){
    if(gc->tuner->kind != TRANS_NOTHING)
      mspace_update_info_after_space_tuning(mspace);
    num_restoring_collectors++;
  }
  while(num_restoring_collectors != num_active_collectors + 1);

  /* Dealing with out of memory in mspace */
  void* mspace_border = &mspace->blocks[mspace->free_block_idx - mspace->first_block_idx];
  if( mspace_border > nos_boundary){
    atomic_cas32( &num_extending_collectors, 0, num_active_collectors);
    
    mspace_extend_compact(collector);
    
    atomic_inc32(&num_extending_collectors);
    while(num_extending_collectors != num_active_collectors);
  }

  TRACE2("gc.process", "GC: collector["<<((POINTER_SIZE_INT)collector->thread_handle)<<"]: finish pass5 and done.");

  return;
}
