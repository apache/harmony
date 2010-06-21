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
#include "../common/gc_concurrent.h"
#include "../common/collection_scheduler.h"

Boolean fspace_alloc_block(Fspace* fspace, Allocator* allocator)
{    
  alloc_context_reset(allocator);

  /* now try to get a new block */
  unsigned int old_free_idx = fspace->free_block_idx;
  unsigned int new_free_idx = old_free_idx+1;
  while(old_free_idx <= fspace->ceiling_block_idx){   
    unsigned int allocated_idx = atomic_cas32(&fspace->free_block_idx, new_free_idx, old_free_idx);
    if(allocated_idx != old_free_idx){     /* if failed */  
      old_free_idx = fspace->free_block_idx;
      new_free_idx = old_free_idx+1;
      continue;
    }
    /* ok, got one */
    Block_Header* alloc_block = (Block_Header*)&(fspace->blocks[allocated_idx - fspace->first_block_idx]);
    
    allocator_init_free_block(allocator, alloc_block);

    return TRUE;
  }

  return FALSE;
  
}

/* FIXME:: the collection should be separated from the allocation */
#ifdef GC_GEN_STATS
#include "../gen/gen_stats.h"
#endif
void* fspace_alloc(unsigned size, Allocator *allocator) 
{
  void*  p_return = NULL;

  /* First, try to allocate object from TLB (thread local block) */
  p_return = thread_local_alloc(size, allocator);
  if (p_return)  return p_return;

  /* ran out local block, grab a new one*/  
  Fspace* fspace = (Fspace*)allocator->alloc_space;
  int attempts = 0;
  while( !fspace_alloc_block(fspace, allocator)){
    vm_gc_lock_enum();
    /* after holding lock, try if other thread collected already */
    if ( !blocked_space_has_free_block((Blocked_Space*)fspace) ) {  
        if(attempts < 2) {
#ifdef GC_GEN_STATS
        GC_Gen* gc = (GC_Gen*)allocator->gc;
        GC_Gen_Stats* stats = gc->stats;
        gc_gen_update_nos_alloc_obj_stats(stats, fspace->committed_heap_size);
#endif
          gc_reclaim_heap(allocator->gc, GC_CAUSE_NOS_IS_FULL); 
          if(allocator->alloc_block){
            vm_gc_unlock_enum();  
            break;
          }
          
          attempts++;
          
        }else{
          vm_gc_unlock_enum();  
          return NULL;
        }
    }
    vm_gc_unlock_enum();  
  }
  
  p_return = thread_local_alloc(size, allocator);
  
  return p_return;
  
}


