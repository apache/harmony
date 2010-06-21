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

Boolean sspace_alloc_block(Sspace* sspace, Allocator* allocator)
{    
  alloc_context_reset(allocator);
  
  /* now try to get a new block */
  Block_Header* old_free_blk = sspace->cur_free_block;
    
  while(old_free_blk != NULL){   
    Block_Header* new_free_blk = old_free_blk->next;
    Block_Header* allocated_blk = (Block_Header*)atomic_casptr((volatile void**)&sspace->cur_free_block, new_free_blk, old_free_blk);
    if(allocated_blk != old_free_blk){     /* if failed */  
      old_free_blk = sspace->cur_free_block;
      continue;
    }
    /* ok, got one */    
    allocator_init_free_block(allocator, allocated_blk);

    return TRUE;
  }

  return FALSE;
  
}

void* sspace_alloc(unsigned size, Allocator *allocator) 
{
  void*  p_return = NULL;

  /* First, try to allocate object from TLB (thread local block) */
  p_return = thread_local_alloc(size, allocator);
  if (p_return)  return p_return;

  /* ran out local block, grab a new one*/  
  Sspace* sspace = (Sspace*)allocator->alloc_space;
  int attempts = 0;
  while( !sspace_alloc_block(sspace, allocator)){
    vm_gc_lock_enum();
    /* after holding lock, try if other thread collected already */
    if ( !sspace_has_free_block(sspace) ) {  
        if(attempts < 2) {
          gc_reclaim_heap(allocator->gc, GC_CAUSE_NOS_IS_FULL); 
          if(allocator->alloc_block){
            vm_gc_unlock_enum();  
            break;
          }
          
          attempts++;
          
        }else{  /* no free block after "attempts" collections */
          vm_gc_unlock_enum();  
          return NULL;
        }
    }
    vm_gc_unlock_enum();  
  }
  
  p_return = thread_local_alloc(size, allocator);
  
  return p_return;
  
}

