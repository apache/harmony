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

static Boolean mspace_alloc_block(Mspace* mspace, Allocator* allocator)
{
  alloc_context_reset(allocator);

  /* now try to get a new block */
  unsigned int old_free_idx = mspace->free_block_idx;
  unsigned int new_free_idx = old_free_idx+1;
  while( old_free_idx <= mspace->ceiling_block_idx ){   
    unsigned int allocated_idx = atomic_cas32(&mspace->free_block_idx, new_free_idx, old_free_idx);
    if(allocated_idx != old_free_idx){
      old_free_idx = mspace->free_block_idx;
      new_free_idx = old_free_idx+1;
      continue;
    }
    /* ok, got one */
    Block_Header* alloc_block = (Block_Header*)&(mspace->blocks[allocated_idx - mspace->first_block_idx]);

    allocator_init_free_block(allocator, alloc_block);

    return TRUE;
  }

  /* Mspace is out. If it's caused by mutator, a collection should be triggered. 
     If it's caused by collector, a fallback should be triggered. */
  return FALSE;
  
}

void* mspace_alloc(unsigned int size, Allocator* allocator)
{
  void *p_return = NULL;
   
  /* All chunks of data requested need to be multiples of GC_OBJECT_ALIGNMENT */
  assert((size % GC_OBJECT_ALIGNMENT) == 0);
  assert( size <= GC_LOS_OBJ_SIZE_THRESHOLD );

  /* check if collector local alloc block is ok. If not, grab a new block */
  p_return = thread_local_alloc(size, allocator);
  if(p_return) return p_return;
  
  /* grab a new block */
   Mspace* mspace = (Mspace*)allocator->alloc_space;;
   Boolean ok = mspace_alloc_block(mspace, allocator);
  if(!ok) return NULL; 
  
  p_return = thread_local_alloc(size, allocator);
  assert(p_return);
    
  return p_return;
}




