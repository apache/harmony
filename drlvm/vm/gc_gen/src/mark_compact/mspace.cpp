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

#include "mspace.h"

struct GC_Gen;
extern void gc_set_mos(GC_Gen* gc, Space* space);
extern Space* gc_get_nos(GC_Gen* gc);

Mspace *mspace_initialize(GC* gc, void* start, POINTER_SIZE_INT mspace_size, POINTER_SIZE_INT commit_size)
{
  Mspace* mspace = (Mspace*)STD_MALLOC( sizeof(Mspace));
  assert(mspace);
  memset(mspace, 0, sizeof(Mspace));
  
  mspace->reserved_heap_size = mspace_size;
  mspace->num_total_blocks = (unsigned int)(mspace_size >> GC_BLOCK_SHIFT_COUNT);

  void* reserved_base = start;
  /* commit mspace mem */
  if(!large_page_hint)
    vm_commit_mem(reserved_base, commit_size);
  memset(reserved_base, 0, commit_size);
  
  mspace->committed_heap_size = commit_size;
  mspace->heap_start = reserved_base;
  
#ifdef STATIC_NOS_MAPPING
  mspace->heap_end = (void *)((POINTER_SIZE_INT)reserved_base + mspace_size);
#else
  mspace->heap_end = (void *)((POINTER_SIZE_INT)reserved_base + commit_size);
#endif

  mspace->num_managed_blocks = (unsigned int)(commit_size >> GC_BLOCK_SHIFT_COUNT);
  
  mspace->first_block_idx = GC_BLOCK_INDEX_FROM(gc->heap_start, reserved_base);
  mspace->ceiling_block_idx = mspace->first_block_idx + mspace->num_managed_blocks - 1;
  
  mspace->num_used_blocks = 0;
  mspace->free_block_idx = mspace->first_block_idx;
  
  space_init_blocks((Blocked_Space*)mspace);

  mspace->space_statistic = (Space_Statistics*)STD_MALLOC(sizeof(Space_Statistics));
  assert(mspace->space_statistic);
  memset(mspace->space_statistic, 0, sizeof(Space_Statistics));


  mspace->num_collections = 0;
  mspace->time_collections = 0;
  mspace->survive_ratio = 0.2f;
  mspace->last_alloced_size = 0;
  mspace->accumu_alloced_size = 0;  
  mspace->total_alloced_size = 0;
  mspace->last_surviving_size = 0;
  mspace->period_surviving_size = 0;
  

  mspace->move_object = TRUE;
  mspace->gc = gc;

  mspace->expected_threshold_ratio = 0.5f;

  return mspace;
}


void mspace_destruct(Mspace* mspace)
{
    //FIXME:: when map the to-half, the decommission start address should change
#ifdef USE_32BITS_HASHCODE
  space_desturct_blocks((Blocked_Space*)mspace);
#endif

  /* we don't free the real space here, the heap will be freed altogether */
  STD_FREE(mspace->space_statistic);
  STD_FREE(mspace);  
  mspace = NULL;
}

void mspace_reset_after_collection(Mspace* mspace)
{
  unsigned int old_num_used = mspace->num_used_blocks;
  unsigned int new_num_used = mspace->free_block_idx - mspace->first_block_idx;
  unsigned int num_used = old_num_used>new_num_used? old_num_used:new_num_used;
  
  Block* blocks = mspace->blocks;
  unsigned int i;
  for(i=0; i < num_used; i++){
    Block_Header* block = (Block_Header*)&(blocks[i]);
    assert(!((POINTER_SIZE_INT)block % GC_BLOCK_SIZE_BYTES));
    block->status = BLOCK_USED;
    block->free = block->new_free;
    block->new_free = block->base;
    block->src = NULL;
    block->next_src = NULL;
    assert(!block->dest_counter);
    if(i >= new_num_used){
      block->status = BLOCK_FREE;
      block->free = GC_BLOCK_BODY(block);
    }
  }
  mspace->num_used_blocks = new_num_used;
  /*For_statistic mos infomation*/
  mspace->period_surviving_size = new_num_used * GC_BLOCK_SIZE_BYTES;
 
  /* we should clear the remaining blocks which are set to be BLOCK_COMPACTED or BLOCK_TARGET */
  for(; i < mspace->num_managed_blocks; i++){
    Block_Header* block = (Block_Header*)&(blocks[i]);
    assert(block->status& (BLOCK_COMPACTED|BLOCK_TARGET|BLOCK_DEST));
    block->status = BLOCK_FREE;
    block->src = NULL;
    block->next_src = NULL;
    block->free = GC_BLOCK_BODY(block);
    assert(!block->dest_counter);
  }
}


#include "../common/fix_repointed_refs.h"

void mspace_fix_after_copy_nursery(Collector* collector, Mspace* mspace)
{
  //the first block is not set yet
  Block_Header* curr_block = blocked_space_block_iterator_next((Blocked_Space*)mspace);
  unsigned int first_block_idx = mspace->first_block_idx;
  unsigned int old_num_used = mspace->num_used_blocks;
  unsigned int old_free_idx = first_block_idx + old_num_used;
  unsigned int new_free_idx = mspace->free_block_idx;
  
  /* for NOS copy, we are sure about the last block for fixing */
  Block_Header* space_end = (Block_Header*)&mspace->blocks[new_free_idx-first_block_idx];  
  
  while( curr_block < space_end){
    assert(curr_block->status == BLOCK_USED);
    if( curr_block->block_idx < old_free_idx)
      /* for blocks used before nos copy */
      block_fix_ref_after_marking(curr_block); 
  
    else  /* for blocks used for nos copy */
      block_fix_ref_after_copying(curr_block); 
         
    curr_block = blocked_space_block_iterator_next((Blocked_Space*)mspace);
  }
   
  return;  
}

/*For_LOS adaptive.*/
void mspace_set_expected_threshold_ratio(Mspace* mspace, float threshold_ratio)
{
    mspace->expected_threshold_ratio = threshold_ratio;
    return;
}

float mspace_get_expected_threshold_ratio(Mspace* mspace)
{
    return mspace->expected_threshold_ratio;
}

