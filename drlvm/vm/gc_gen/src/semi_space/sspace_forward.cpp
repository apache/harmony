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

static Boolean semispace_alloc_block(Sspace* sspace, Allocator* allocator)
{
  alloc_context_reset(allocator);

  /* now try to get a new block */
  unsigned int old_free_idx = sspace->free_block_idx;
  unsigned int new_free_idx = old_free_idx+1;
  while( old_free_idx <= sspace->ceiling_block_idx ){   
    unsigned int allocated_idx = atomic_cas32(&sspace->free_block_idx, new_free_idx, old_free_idx);
    if(allocated_idx != old_free_idx){
      old_free_idx = sspace->free_block_idx;
      new_free_idx = old_free_idx+1;
      continue;
    }
    /* ok, got one */
    Block_Header* alloc_block = (Block_Header*)&(sspace->blocks[allocated_idx - sspace->first_block_idx]);

    allocator_init_free_block(allocator, alloc_block);

    return TRUE;
  }

  /* semispace is out, a fallback should be triggered */
  return FALSE;
  
}

void* semispace_alloc(unsigned int size, Allocator* allocator)
{
  void *p_return = NULL;
   
  /* All chunks of data requested need to be multiples of GC_OBJECT_ALIGNMENT */
  assert((size % GC_OBJECT_ALIGNMENT) == 0);
  assert( size <= GC_LOS_OBJ_SIZE_THRESHOLD );

  /* check if collector local alloc block is ok. If not, grab a new block */
  p_return = thread_local_alloc(size, allocator);
  if(p_return) return p_return;
  
  /* grab a new block */
  Sspace* sspace = (Sspace*)allocator->alloc_space;
  Boolean ok = semispace_alloc_block(sspace, allocator);
  if(!ok) return NULL; 
  
  p_return = thread_local_alloc(size, allocator);
  assert(p_return);
    
  return p_return;
}
