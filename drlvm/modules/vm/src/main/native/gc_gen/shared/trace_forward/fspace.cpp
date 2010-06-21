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

#include "fspace.h"

#ifdef PREFETCH_SUPPORTED
Boolean mark_prefetch = FALSE;
#endif

Boolean NOS_PARTIAL_FORWARD = FALSE;

Boolean forward_first_half;
void* object_forwarding_boundary=NULL;

Fspace *fspace_initialize(GC* gc, void* start, POINTER_SIZE_INT fspace_size, POINTER_SIZE_INT commit_size) 
{    
  assert( (fspace_size%GC_BLOCK_SIZE_BYTES) == 0 );
  Fspace* fspace = (Fspace *)STD_MALLOC(sizeof(Fspace));
  assert(fspace);
  memset(fspace, 0, sizeof(Fspace));
    
  fspace->reserved_heap_size = fspace_size;
  fspace->num_total_blocks = (unsigned int)(fspace_size >> GC_BLOCK_SHIFT_COUNT);

  void* reserved_base = start;
  /* commit fspace mem */    
  if(!large_page_hint)    
    vm_commit_mem(reserved_base, commit_size);
  memset(reserved_base, 0, commit_size);
  
  fspace->committed_heap_size = commit_size;
  fspace->heap_start = reserved_base;

#ifdef STATIC_NOS_MAPPING
  fspace->heap_end = (void *)((POINTER_SIZE_INT)reserved_base + fspace->reserved_heap_size);
#else /* for dynamic mapping, fspace->heap_end is gc->heap_end */
  fspace->heap_end = (void *)((POINTER_SIZE_INT)reserved_base + fspace->committed_heap_size);
#endif

  fspace->num_managed_blocks = (unsigned int)(commit_size >> GC_BLOCK_SHIFT_COUNT);
  
  fspace->first_block_idx = GC_BLOCK_INDEX_FROM(gc->heap_start, reserved_base);
  fspace->ceiling_block_idx = fspace->first_block_idx + fspace->num_managed_blocks - 1;
  
  fspace->num_used_blocks = 0;
  fspace->free_block_idx = fspace->first_block_idx;
  
  space_init_blocks((Blocked_Space*)fspace);
  
  fspace->move_object = TRUE;
  fspace->num_collections = 0;
  fspace->time_collections = 0;
  fspace->survive_ratio = 0.2f;
  fspace->last_alloced_size = 0;
  fspace->accumu_alloced_size = 0;  
  fspace->total_alloced_size = 0;
  fspace->last_surviving_size = 0;
  fspace->period_surviving_size = 0;
  
  fspace->gc = gc;
  /* above is same as Mspace init --> */
  
  forward_first_half = TRUE;
  /* we always disable partial forwarding in non-gen mode. */
  if( !gc_is_gen_mode() )
    NOS_PARTIAL_FORWARD = FALSE;

  if( NOS_PARTIAL_FORWARD )
    object_forwarding_boundary = (void*)&fspace->blocks[fspace->num_managed_blocks >>1 ];
  else
    object_forwarding_boundary = (void*)&fspace->blocks[fspace->num_managed_blocks];
     
  return fspace;
}

void fspace_destruct(Fspace *fspace) 
{
#ifdef USE_32BITS_HASHCODE
  space_desturct_blocks((Blocked_Space*)fspace);
#endif
  /* we don't free the real space here, the heap will be freed altogether */
  STD_FREE(fspace);   
  fspace = NULL;
}

void fspace_reset_after_collection(Fspace* fspace)
{ 
  unsigned int first_idx = fspace->first_block_idx;
  unsigned int marked_start_idx = 0; //was for oi markbit reset, now useless
  unsigned int marked_last_idx = 0;
  Boolean is_major_collection = collect_is_major();
  
  if(  is_major_collection || 
         NOS_PARTIAL_FORWARD == FALSE || !gc_is_gen_mode())            
  {
    fspace->free_block_idx = first_idx;
    fspace->ceiling_block_idx = first_idx + fspace->num_managed_blocks - 1;  
    forward_first_half = TRUE; /* only useful for not-FORWARD_ALL*/
    fspace->num_used_blocks = 0;
  
  }else{    
    if(forward_first_half){
      fspace->free_block_idx = first_idx;
      fspace->ceiling_block_idx = ((Block_Header*)object_forwarding_boundary)->block_idx - 1;
      marked_start_idx = ((Block_Header*)object_forwarding_boundary)->block_idx - first_idx;
      marked_last_idx = fspace->num_managed_blocks - 1;
    }else{
      fspace->free_block_idx = ((Block_Header*)object_forwarding_boundary)->block_idx;
      fspace->ceiling_block_idx = first_idx + fspace->num_managed_blocks - 1;
      marked_start_idx = 0;
      marked_last_idx = ((Block_Header*)object_forwarding_boundary)->block_idx - 1 - first_idx;
    }
    fspace->num_used_blocks = marked_last_idx - marked_start_idx + 1;
    forward_first_half = forward_first_half^1;
  }
  
  Block* blocks = fspace->blocks;
  unsigned int num_freed = 0;
  int new_start_idx = (int)(fspace->free_block_idx) - (int)first_idx;
  int new_last_idx = (int)fspace->ceiling_block_idx - (int)first_idx;
  for(int i = new_start_idx; i <= new_last_idx; i++){
    Block_Header* block = (Block_Header*)&(blocks[i]);
    block->src = NULL;
    block->next_src = NULL;
    assert(!block->dest_counter);
    if(block->status == BLOCK_FREE) continue;
    block->status = BLOCK_FREE; 
    block->free = block->base;

  }
  
  /* For los extension
   * num_managed_blocks of fspace might be 0.
   * In this case, the last block we found is mos' last block.
   * And this implementation depends on the fact that mos and fspace are continuous.
   */
  int last_block_index = fspace->num_managed_blocks - 1;
  Block_Header *fspace_last_block = (Block_Header*)&fspace->blocks[last_block_index];
  fspace_last_block->next = NULL;
  
  return;
}

#ifndef STATIC_NOS_MAPPING
void* fspace_heap_start_adjust(Fspace* fspace, void* new_space_start, POINTER_SIZE_INT new_space_size)
{

  blocked_space_adjust((Blocked_Space*)fspace, new_space_start, new_space_size);
  
  fspace->free_block_idx = fspace->first_block_idx;

  if( NOS_PARTIAL_FORWARD )
    object_forwarding_boundary = (void*)&fspace->blocks[fspace->num_managed_blocks >>1];
  else
    object_forwarding_boundary = (void*)&fspace->blocks[fspace->num_managed_blocks];
 
  return new_space_start; 
}
#endif

void collector_execute_task(GC* gc, TaskType task_func, Space* space);

#ifdef GC_GEN_STATS
#include "../gen/gen_stats.h"
#endif

/* world is stopped when starting fspace_collection */      
void fspace_collection(Fspace *fspace)
{
  fspace->num_collections++;  

  GC* gc = fspace->gc;
  
  /* we should not destruct rootset structure in case we need fall back */
  pool_iterator_init(gc->metadata->gc_rootset_pool);

  if( !gc_is_gen_mode() ){
#ifdef MARK_BIT_FLIPPING

    TRACE2("gc.process", "GC: nongenerational forward algo start ... \n");
    collector_execute_task(gc, (TaskType)nongen_forward_pool, (Space*)fspace);
    TRACE2("gc.process", "\nGC: end of nongen forward algo ... \n");
#else
    assert(0);  
#endif /*#ifdef MARK_BIT_FLIPPING #else */
  }else{
    TRACE2("gc.process", "generational forward algo start ... \n");
    collector_execute_task(gc, (TaskType)gen_forward_pool, (Space*)fspace);
    TRACE2("gc.process", "\nGC: end of gen forward algo ... \n");
  }    
  
  return; 
}
