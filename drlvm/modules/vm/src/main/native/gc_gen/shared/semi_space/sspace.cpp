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


#include "sspace.h"

POINTER_SIZE_INT TOSPACE_SIZE = 0;
void* tospace_start;
void* tospace_end;

static unsigned int sspace_compute_num_tospace_blocks(Sspace* sspace)
{
  unsigned int num_tospace_blocks = 0;

  if(! TOSPACE_SIZE) /* if no specified size, use default one */
    return sspace->num_managed_blocks >> 3;  


  if( (TOSPACE_SIZE << 1) >= sspace->committed_heap_size ){

    TOSPACE_SIZE = sspace->committed_heap_size >> 1;

    /* tospace should always smaller than fromspace, and at least 1 block. */
    TOSPACE_SIZE = round_down_to_size(TOSPACE_SIZE, GC_BLOCK_SIZE_BYTES);
    if( TOSPACE_SIZE != 0 ) TOSPACE_SIZE -= 1; 

    if( TOSPACE_SIZE >= MB ){
        LWARN(66, "GC Init: TOSPACE_SIZE is too big, set it to be {0}MB" << TOSPACE_SIZE/MB);
    }else{
        LWARN(66, "GC Init: TOSPACE_SIZE is too big, set it to be {0}KB" << TOSPACE_SIZE/KB);
    }
  }

  num_tospace_blocks = (unsigned int)(TOSPACE_SIZE >> GC_BLOCK_SHIFT_COUNT);

  return num_tospace_blocks;
}

static void sspace_config_after_clean(Sspace* sspace)
{
  unsigned int num_tospace_blocks = sspace_compute_num_tospace_blocks(sspace);
  unsigned int num_fromspace_blocks = sspace->num_managed_blocks - num_tospace_blocks;
  unsigned int sspace_first_idx = sspace->first_block_idx;
 
  /* prepare for from-space, first half */
  Block_Header* fromspace_last_block = (Block_Header*)&(sspace->blocks[num_fromspace_blocks - 1]);
  fromspace_last_block->next = NULL;

  /* prepare for to-space */
  sspace->tospace_first_idx = sspace_first_idx + num_fromspace_blocks;
  sspace->ceiling_block_idx = sspace->tospace_first_idx +  num_tospace_blocks - 1;
  assert( sspace->ceiling_block_idx == sspace_first_idx + sspace->num_managed_blocks - 1 );
  sspace->free_block_idx = sspace->tospace_first_idx;
          
  /* no survivor area at the beginning */
  sspace->survivor_area_start = (Block_Header*)&(sspace->blocks[num_fromspace_blocks]);
  sspace->survivor_area_end = sspace->survivor_area_start;

  sspace->cur_free_block = (Block_Header*)sspace->heap_start;
  if(num_fromspace_blocks == 0) sspace->cur_free_block = NULL; 

  sspace->num_used_blocks = 0;
  
}

Sspace *sspace_initialize(GC* gc, void* start, POINTER_SIZE_INT sspace_size, POINTER_SIZE_INT commit_size) 
{    
  assert( (sspace_size%GC_BLOCK_SIZE_BYTES) == 0 );
  Sspace* sspace = (Sspace *)STD_MALLOC(sizeof(Sspace));
  assert(sspace);
  memset(sspace, 0, sizeof(Sspace));
    
  sspace->reserved_heap_size = sspace_size;
  sspace->num_total_blocks = (unsigned int)(sspace_size >> GC_BLOCK_SHIFT_COUNT);

  void* reserved_base = start;
  /* commit sspace mem */    
  if(!large_page_hint)    
    vm_commit_mem(reserved_base, commit_size);
  memset(reserved_base, 0, commit_size);
  
  sspace->committed_heap_size = commit_size;
  sspace->heap_start = reserved_base;
  sspace->blocks = (Block*)reserved_base;

#ifdef STATIC_NOS_MAPPING
  sspace->heap_end = (void *)((POINTER_SIZE_INT)reserved_base + sspace->reserved_heap_size);
#else /* for dynamic mapping, nos->heap_end is gc->heap_end */
  sspace->heap_end = (void *)((POINTER_SIZE_INT)reserved_base + sspace->committed_heap_size);
#endif

  sspace->num_managed_blocks = (unsigned int)(commit_size >> GC_BLOCK_SHIFT_COUNT);
 
  unsigned int sspace_first_idx = GC_BLOCK_INDEX_FROM(gc->heap_start, sspace->heap_start);
  sspace->first_block_idx = sspace_first_idx;

  space_init_blocks((Blocked_Space*)sspace); /* it uses first_block_idx and num_managed_blocks */

  sspace_config_after_clean(sspace);  /* it uses first_block_idx and num_managed_blocks */
 	  
  sspace->move_object = TRUE;
  sspace->num_collections = 0;
  sspace->time_collections = 0;
  sspace->survive_ratio = 0.2f;
  sspace->last_alloced_size = 0;
  sspace->accumu_alloced_size = 0;  
  sspace->total_alloced_size = 0;
  sspace->last_surviving_size = 0;
  sspace->period_surviving_size = 0;
  
  sspace->gc = gc;

  return sspace;
}

void sspace_destruct(Sspace *sspace) 
{
#ifdef USE_32BITS_HASHCODE
  space_desturct_blocks((Blocked_Space*)sspace);
#endif
  /* we don't free the real space here, the heap will be freed altogether */
  STD_FREE(sspace);   
  sspace = NULL;
}

POINTER_SIZE_INT sspace_free_space_size(Sspace* nos)
{
  POINTER_SIZE_INT tospace_free_size = blocked_space_free_mem_size((Blocked_Space*)nos);
  
  POINTER_SIZE_INT fromspace_free_size = 0;
  Block_Header* cur_free_block = nos->cur_free_block;
  POINTER_SIZE_INT num_free_blocks = 0;
  while(cur_free_block){
    num_free_blocks++;
    cur_free_block = cur_free_block->next;  
  }
  fromspace_free_size = num_free_blocks << GC_BLOCK_SHIFT_COUNT;
 
  return tospace_free_size + fromspace_free_size;
}

POINTER_SIZE_INT sspace_used_space_size(Sspace* nos)
{
  return nos->committed_heap_size - sspace_free_space_size(nos);
}

/* adjust the next pointer of block to point correctly to its adjacent next */
void sspace_prepare_for_collection(Sspace* sspace)
{  
  if(sspace->num_managed_blocks == 0)
    return;
  
  unsigned int tospace_first_idx = sspace->tospace_first_idx;
  unsigned int sspace_last_idx = sspace->first_block_idx + sspace->num_managed_blocks - 1;

  /* if to-space is in second half, need to do two connections:
     1. from-space last block to the first block of to-space, 2. connect from-space two parts cross survivor area */
  if(sspace->ceiling_block_idx == sspace_last_idx){
    Block_Header* tospace_first_block = (Block_Header*)&sspace->blocks[tospace_first_idx - sspace->first_block_idx];
    /* For 1. Fromspace has two parts only when tospace is not adjacent to survivor area. */
    if(tospace_first_block != sspace->survivor_area_end){
      Block_Header* block_before_tospace =  (Block_Header*)((Block*)tospace_first_block - 1);
      assert( block_before_tospace->next == NULL);
      block_before_tospace->next = tospace_first_block;
    }
    /* For 2. It doesn't matter if survivor_area size is 0. */
    Block_Header* block_before_survivor_area = (Block_Header*)((Block*)(sspace->survivor_area_start) - 1);
    block_before_survivor_area->next = (Block_Header*)(sspace->survivor_area_start);
      
  }else{  /* otherwise, tospace is in first half */
    
    /* Connect last block of from-space to first block of tospacde */
    Block_Header* block_before_tospace = (Block_Header*)&sspace->blocks[tospace_first_idx - sspace->first_block_idx - 1];
    block_before_tospace->next = (Block_Header*)((Block*)block_before_tospace + 1);
        
  }
  /* in any case, sspace last block's next should point to NULL. This is the case:
     1. when tospace is in second half and has 0 in size. 
     2. when tospace is in first half, both tospace and survivor area have 0 in size. 
     But the above code sets it to point to the first block of tospace, which is sspace->heap_end and the block is not mapped.
     */
  ((Block_Header*)&(sspace->blocks[sspace->num_managed_blocks - 1]))->next = NULL;  

  return;
}

void sspace_reset_after_collection(Sspace* sspace)
{ 
  /* the space is zero size after major (or variant) collection. No need
     to reset its tospace and fromspace, but still we still set its allocation
     pointer to NULL so that nothing can be allocated. And we need set 
     num_used_blocks to 0 in case GC need the data for space tuning whatever. */
  if(sspace->num_managed_blocks == 0) {
    sspace->cur_free_block = NULL;
    sspace->num_used_blocks = 0;
    return;
  }
  
  Block* blocks = (Block*)sspace->blocks;
   /* During LOS extension, NOS last block points back to MOS first block, so that,
     the first couple blocks of MOS are compacted to NOS end. We need reset the NOS last block next pointer.
     This is needed for next collection. And it's needed for from-space setup, 
     so that the last block of from-space points to NULL. */
  Block_Header *sspace_last_block = (Block_Header*)((Block*)sspace->heap_end - 1);

  assert(sspace_last_block);

  sspace_last_block->next = NULL;

  unsigned int sspace_first_idx = sspace->first_block_idx;

  Boolean is_major_collection = collect_is_major();

  if( is_major_collection ){ 
    /* prepare for from-space, first half */
    sspace_config_after_clean(sspace);

    /* clean up the collected blocks */
    for(unsigned int i = sspace_first_idx; i <= sspace->ceiling_block_idx; i++){
      Block_Header* block = (Block_Header*)&(blocks[i - sspace_first_idx]);
      block_reset(block);
    }

    return;          
  }

  /* for minor collection */
  unsigned int num_tospace_blocks = sspace_compute_num_tospace_blocks(sspace);
  unsigned int num_fromspace_blocks = sspace->num_managed_blocks - num_tospace_blocks;
  unsigned int sspace_last_idx = sspace_first_idx + sspace->num_managed_blocks - 1;

    /* clean up the collected blocks */
  unsigned int start_idx = 0;
  unsigned int end_idx = sspace->tospace_first_idx - sspace_first_idx - 1;
  for(unsigned int i = start_idx; i <= end_idx; i++){
    Block_Header* block = (Block_Header*)&(blocks[i]);
    block_reset(block);
  }
  
  /* when tospace is in first half. clean the part of fromspace after tospace. */
  if(sspace->ceiling_block_idx != sspace_last_idx){
    start_idx = sspace->ceiling_block_idx - sspace_first_idx + 1;
    end_idx = sspace_last_idx - sspace_first_idx;
    for(unsigned int i = start_idx; i <= end_idx; i++){
      Block_Header* block = (Block_Header*)&(blocks[i]);
      block_reset(block);
    }    
  }

  sspace->cur_free_block = (Block_Header*)sspace->heap_start;
  
  /* minor collection always has a survivor area.
    (The size of survivor area can zero if tospace size is zero, e.g., sspace size is to small. 
     This is no correctness issue, because it means all sspace objects are forwarded to MOS. )
  */
  sspace->survivor_area_start = (void *)&blocks[sspace->tospace_first_idx - sspace_first_idx];
  sspace->survivor_area_end = (void *)&blocks[sspace->free_block_idx - sspace_first_idx];
  sspace->num_used_blocks = sspace->free_block_idx - sspace->tospace_first_idx;        

  /* if the survivor_area of this collection is at first half of sspace */
  if(  sspace->ceiling_block_idx != sspace_last_idx){      
      
    /* Check if suvivor area is overlapped with new tospace. In that case, we should ensure fromspace covers survivor area. */                   
    if( sspace->survivor_area_end > &(sspace->blocks[num_fromspace_blocks]) ){
     /* this case might happen after nos_boundary adjusted. Since this function is only called before nos_boundary adjustment, that means, 
        if execution comes here, it's because of last time minor collection boundary adjustment, and because we don't change the sspace config in minor collection.) */
      num_fromspace_blocks = ((POINTER_SIZE_INT)sspace->survivor_area_end - (POINTER_SIZE_INT)sspace->heap_start) >> GC_BLOCK_SHIFT_COUNT;
      num_tospace_blocks = sspace->num_managed_blocks - num_fromspace_blocks;
    }
    
    /* prepare for to-space in second half */
    sspace->tospace_first_idx = sspace_first_idx + num_fromspace_blocks;
    sspace->ceiling_block_idx = sspace->tospace_first_idx +  num_tospace_blocks - 1;
    sspace->free_block_idx = sspace->tospace_first_idx;
    
    /* prepare from-space */
    /* connect the free areas cross survivor area */
    Block_Header* block_before_survivor_area = (Block_Header*)((Block*)sspace->survivor_area_start - 1);
    /* set last block of fromspace next to NULL, if the size of survivor_area is zero or its adjacent to tospace.
       ensure we don't connect fromspace to tospace. */
    /* we intentionally don't use survivor_area_end->block_idx dereference, because survivor_area_end 
       might point at the heap_end, which has no mem mapped. */
    if( sspace->survivor_area_end == &(sspace->blocks[num_fromspace_blocks])) 
      block_before_survivor_area->next = NULL;
    else{
      block_before_survivor_area->next = (Block_Header*)sspace->survivor_area_end;
      ((Block_Header*)&(sspace->blocks[num_fromspace_blocks-1]))->next = NULL;
    }
    
  }else{ /* after minor collection, if the survivor_area is at second half of sspace */

    /* Check if sspace has no enough blocks to hold tospace. */                   
    if( sspace->tospace_first_idx - sspace->first_block_idx <= num_tospace_blocks ){
      /* this case shold never happen right after minor collection, might happen after nos_boundary adjusted */        
      assert(0); 
      num_tospace_blocks = (sspace->tospace_first_idx - sspace->first_block_idx) >> 3;
      num_fromspace_blocks = sspace->num_managed_blocks - num_tospace_blocks;
    }
          
    /* prepare for to-space */
    sspace->ceiling_block_idx = sspace->tospace_first_idx - 1;
    sspace->tospace_first_idx = sspace->tospace_first_idx - num_tospace_blocks;
    sspace->free_block_idx = sspace->tospace_first_idx;

    /* prepare for from-space */
    /* connect the free areas cross tospace and survivor_area */
    Block_Header* block_before_tospace = (Block_Header*)&blocks[sspace->tospace_first_idx - 1 - sspace_first_idx];
    /* in case survivor_area_end is heap_end, then block_after_survivor_area should be NULL */
    Block_Header* survivor_area_last_blk = (Block_Header*)((Block*)(sspace->survivor_area_end) - 1);
    block_before_tospace->next = survivor_area_last_blk->next;
    
  }
         
  return;
}

#ifndef STATIC_NOS_MAPPING 
void* sspace_heap_start_adjust(Sspace* sspace, void* new_space_start, POINTER_SIZE_INT new_space_size)
{ 
  GC* gc = sspace->gc;
  
  /* we can simply change certain fields of sspace and keep the original semispace config.
     The issue is, the tospace was computed according to original space size. 
     So we just do another round of config after major collection. 
     It's troublesome to do config again after minor collection. */

  /* major collection leaves no survivor area in nos */
  if( collect_is_major()){
    /* retore the fromspace last block next pointer. It was set a moment ago in sspace_reset_after_collection. */
    Block_Header* block_before_survivor_area = (Block_Header*)((Block*)(sspace->survivor_area_start) - 1);
    block_before_survivor_area->next = (Block_Header*)(sspace->survivor_area_start);

    blocked_space_adjust((Blocked_Space*)sspace, new_space_start, new_space_size);
    
    sspace_config_after_clean(sspace);
    
    return new_space_start;   
  }
  /* for minor collection */
  /* always leave at least one free block at the beginning of sspace for fromspace, 
     so that sspace->cur_free_block always points to sspace start. */
  void* old_space_start = new_space_start;
  
  if( new_space_start >= sspace->survivor_area_start)
    new_space_start = (Block*)(sspace->survivor_area_start) - 1; 
  
  void* tospace_start = &(sspace->blocks[sspace->free_block_idx - sspace->first_block_idx]); 
  if( new_space_start >= tospace_start)
    new_space_start = (Block*)tospace_start - 1;
  
  new_space_size += (POINTER_SIZE_INT)old_space_start - (POINTER_SIZE_INT)new_space_start;

  /* change the fields that are changed */  
  sspace->heap_start = new_space_start;
  sspace->blocks = (Block*)new_space_start;
  sspace->committed_heap_size = new_space_size;
  sspace->reserved_heap_size = new_space_size;
  sspace->num_managed_blocks = (unsigned int)(new_space_size >> GC_BLOCK_SHIFT_COUNT);
  sspace->num_total_blocks = sspace->num_managed_blocks;
  sspace->first_block_idx = ((Block_Header*)new_space_start)->block_idx;
  
  sspace->cur_free_block = (Block_Header*)sspace->heap_start;

  return new_space_start;
}
#endif /* #ifndef STATIC_NOS_MAPPING  */

void collector_execute_task(GC* gc, TaskType task_func, Space* space);

/* world is stopped when starting sspace_collection */      
#include "../gen/gen.h"  /* for gc_is_gen_mode() */
void sspace_collection(Sspace *sspace)
{
  sspace->num_collections++;  

  GC* gc = sspace->gc;
  
  /* we should not destruct rootset structure in case we need fall back */
  pool_iterator_init(gc->metadata->gc_rootset_pool);

  if(!gc_is_gen_mode()){
#ifdef MARK_BIT_FLIPPING
    TRACE2("gc.process", "GC: nongenerational semispace algo start ... \n");
    collector_execute_task(gc, (TaskType)nongen_ss_pool, (Space*)sspace);
    TRACE2("gc.process", "\nGC: end of nongen semispace pool algo ... \n");
#else
    assert(0);
#endif /*#ifdef MARK_BIT_FLIPPING #else */

  }else{
    TRACE2("gc.process", "generational semispace algo start ... \n");
    collector_execute_task(gc, (TaskType)gen_ss_pool, (Space*)sspace);
    TRACE2("gc.process", "\nGC: end of gen semispace pool algo ... \n");
  }    
  
  return; 
}
