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

#include "gc_space.h"

void space_init_blocks(Blocked_Space* space)
{ 
  Block* blocks = (Block*)space->heap_start; 
  Block_Header* last_block = (Block_Header*)blocks;
  unsigned int start_idx = space->first_block_idx;
  for(unsigned int i=0; i < space->num_managed_blocks; i++){
    Block_Header* block = (Block_Header*)&(blocks[i]);
    block_init(block);
    block->block_idx = i + start_idx;
    last_block->next = block;
    last_block = block;
  }
  last_block->next = NULL;
  space->blocks = blocks;
   
  return;
}

void space_desturct_blocks(Blocked_Space* space)
{
  Block* blocks = (Block*)space->heap_start; 
  unsigned int i=0;
  for(; i < space->num_managed_blocks; i++){
    Block_Header* block = (Block_Header*)&(blocks[i]);
    block_destruct(block);
  }
}

#ifndef STATIC_NOS_MAPPING
void blocked_space_adjust(Blocked_Space* space, void* new_space_start, POINTER_SIZE_INT new_space_size)
{    
  space->heap_start = new_space_start;
  space->blocks = (Block*)new_space_start;
  space->committed_heap_size = new_space_size;
  space->reserved_heap_size = new_space_size;
  space->num_managed_blocks = (unsigned int)(new_space_size >> GC_BLOCK_SHIFT_COUNT);
  space->num_total_blocks = space->num_managed_blocks;
  space->first_block_idx = ((Block_Header*)new_space_start)->block_idx;
  space->ceiling_block_idx = space->first_block_idx + space->num_managed_blocks - 1;
 	space->num_used_blocks = 0;

  void* new_space_end = (void*)((POINTER_SIZE_INT)new_space_start + new_space_size);
  space->heap_end = new_space_end;

  /* we can't set free_block_idx here! because the adjusted space might have some used block inside. 
     e.g., when it's used to adjust MOS after a collection.
  space->free_block_idx = space->first_block_idx;
  */
  
  return;
}
#endif /* #ifndef STATIC_NOS_MAPPING */

void blocked_space_shrink(Blocked_Space* space, unsigned int changed_size)
{
  unsigned int block_dec_count = changed_size >> GC_BLOCK_SHIFT_COUNT;
  void* new_base = (void*)&(space->blocks[space->num_managed_blocks - block_dec_count]);
 
  void* decommit_base = (void*)round_down_to_size((POINTER_SIZE_INT)new_base, SPACE_ALLOC_UNIT);
  assert( ((Block_Header*)decommit_base)->block_idx >= space->free_block_idx);
  
  void* old_end = (void*)&space->blocks[space->num_managed_blocks];
  POINTER_SIZE_INT decommit_size = (POINTER_SIZE_INT)old_end - (POINTER_SIZE_INT)decommit_base;
  assert(decommit_size && !(decommit_size%GC_BLOCK_SIZE_BYTES));
  
  Boolean result = vm_decommit_mem(decommit_base, decommit_size);
  assert(result == TRUE);

  space->heap_end = decommit_base;
  space->committed_heap_size = (POINTER_SIZE_INT)decommit_base - (POINTER_SIZE_INT)space->heap_start;
  space->num_managed_blocks = (unsigned int)(space->committed_heap_size >> GC_BLOCK_SHIFT_COUNT);
  
  Block_Header* new_last_block = (Block_Header*)&space->blocks[space->num_managed_blocks - 1];
  space->ceiling_block_idx = new_last_block->block_idx;
  new_last_block->next = NULL;
}

void blocked_space_extend(Blocked_Space* space, unsigned int changed_size)
{
  unsigned int block_inc_count = changed_size >> GC_BLOCK_SHIFT_COUNT;
  
  void* old_base = (void*)&space->blocks[space->num_managed_blocks];
  void* commit_base = (void*)round_down_to_size((POINTER_SIZE_INT)old_base, SPACE_ALLOC_UNIT);
  unsigned int block_diff_count = (unsigned int)(((POINTER_SIZE_INT)old_base - (POINTER_SIZE_INT)commit_base) >> GC_BLOCK_SHIFT_COUNT);
  block_inc_count += block_diff_count;
  
  POINTER_SIZE_INT commit_size = block_inc_count << GC_BLOCK_SHIFT_COUNT;
  void* result = vm_commit_mem(commit_base, commit_size);
  assert(result == commit_base);

  void* new_end = (void*)((POINTER_SIZE_INT)commit_base + commit_size);
  space->committed_heap_size = (POINTER_SIZE_INT)new_end - (POINTER_SIZE_INT)space->heap_start;
  /*Fixme: For_Heap_Adjust, but need fix if static mapping.*/
  space->heap_end = new_end;
  /* init the grown blocks */
  Block_Header* block = (Block_Header*)commit_base;
  Block_Header* last_block = (Block_Header*)((Block*)block -1);
  unsigned int start_idx = last_block->block_idx + 1;
  unsigned int i;
  for(i=0; block < new_end; i++){
    block_init(block);
    block->block_idx = start_idx + i;
    last_block->next = block;
    last_block = block;
    block = (Block_Header*)((Block*)block + 1);  
  }
  last_block->next = NULL;
  space->ceiling_block_idx = last_block->block_idx;
  space->num_managed_blocks = (unsigned int)(space->committed_heap_size >> GC_BLOCK_SHIFT_COUNT);
}

void blocked_space_block_iterator_init(Blocked_Space *space)
{ space->block_iterator = (Block_Header*)space->blocks; }

void blocked_space_block_iterator_init_free(Blocked_Space *space)
{ space->block_iterator = (Block_Header*)&space->blocks[space->free_block_idx - space->first_block_idx]; }

Block_Header *blocked_space_block_iterator_get(Blocked_Space *space)
{ return (Block_Header*)space->block_iterator; }

Block_Header *blocked_space_block_iterator_next(Blocked_Space *space)
{
  Block_Header *cur_block = (Block_Header*)space->block_iterator;
  
  while(cur_block != NULL){
    Block_Header *next_block = cur_block->next;
    
    Block_Header *temp = (Block_Header*)atomic_casptr((volatile void **)&space->block_iterator, next_block, cur_block);
    if(temp != cur_block){
      cur_block = (Block_Header*)space->block_iterator;
      continue;
    }
    return cur_block;
  }
  /* run out space blocks */
  return NULL;
}

/* ================================================ */

#ifdef USE_32BITS_HASHCODE
Partial_Reveal_Object* block_get_first_marked_object_extend(Block_Header* block, void** start_pos)
{
  Partial_Reveal_Object* cur_obj = (Partial_Reveal_Object*)block->base;
  Partial_Reveal_Object* block_end = (Partial_Reveal_Object*)block->free;

  Partial_Reveal_Object* first_marked_obj = next_marked_obj_in_block(cur_obj, block_end);
  if(!first_marked_obj)
    return NULL;

  *start_pos = obj_end_extend(first_marked_obj);
  
  return first_marked_obj;
}
#endif /* #ifdef USE_32BITS_HASHCODE */

Partial_Reveal_Object* block_get_first_marked_object(Block_Header* block, void** start_pos)
{
  Partial_Reveal_Object* cur_obj = (Partial_Reveal_Object*)block->base;
  Partial_Reveal_Object* block_end = (Partial_Reveal_Object*)block->free;

  Partial_Reveal_Object* first_marked_obj = next_marked_obj_in_block(cur_obj, block_end);
  if(!first_marked_obj)
    return NULL;
  
  *start_pos = obj_end(first_marked_obj);
  
  return first_marked_obj;
}

Partial_Reveal_Object* block_get_next_marked_object(Block_Header* block, void** start_pos)
{
  Partial_Reveal_Object* cur_obj = *(Partial_Reveal_Object**)start_pos;
  Partial_Reveal_Object* block_end = (Partial_Reveal_Object*)block->free;

  Partial_Reveal_Object* next_marked_obj = next_marked_obj_in_block(cur_obj, block_end);
  if(!next_marked_obj)
    return NULL;
  
  *start_pos = obj_end(next_marked_obj);
  
  return next_marked_obj;
}

Partial_Reveal_Object *block_get_first_marked_obj_prefetch_next(Block_Header *block, void **start_pos)
{
  Partial_Reveal_Object *cur_obj = (Partial_Reveal_Object *)block->base;
  Partial_Reveal_Object *block_end = (Partial_Reveal_Object *)block->free;
  
  Partial_Reveal_Object *first_marked_obj = next_marked_obj_in_block(cur_obj, block_end);
  if(!first_marked_obj)
    return NULL;
  
  Partial_Reveal_Object *next_obj = obj_end(first_marked_obj);
  *start_pos = next_obj;
  
  if(next_obj >= block_end)
    return first_marked_obj;
  
  Partial_Reveal_Object *next_marked_obj = next_marked_obj_in_block(next_obj, block_end);
  
  if(next_marked_obj){
    if(next_marked_obj != next_obj)
      obj_set_prefetched_next_pointer(next_obj, next_marked_obj);
  } else {
      obj_set_prefetched_next_pointer(next_obj, 0);
  }
  return first_marked_obj;
}

Partial_Reveal_Object *block_get_first_marked_obj_after_prefetch(Block_Header *block, void **start_pos)
{
#ifdef USE_32BITS_HASHCODE
  return block_get_first_marked_object_extend(block, start_pos);
#else
  return block_get_first_marked_object(block, start_pos);
#endif
}

Partial_Reveal_Object *block_get_next_marked_obj_prefetch_next(Block_Header *block, void **start_pos)
{
  Partial_Reveal_Object *cur_obj = *(Partial_Reveal_Object **)start_pos;
  Partial_Reveal_Object *block_end = (Partial_Reveal_Object *)block->free;

  if(cur_obj >= block_end)
    return NULL;
  
  Partial_Reveal_Object *cur_marked_obj;
  
  if(obj_is_marked_in_vt(cur_obj))
    cur_marked_obj = cur_obj;
  else
    cur_marked_obj = (Partial_Reveal_Object *)obj_get_prefetched_next_pointer(cur_obj);

  if(!cur_marked_obj)
    return NULL;
  
  Partial_Reveal_Object *next_obj = obj_end(cur_marked_obj);
  *start_pos = next_obj;
  
  if(next_obj >= block_end)
    return cur_marked_obj;
  
  Partial_Reveal_Object *next_marked_obj = next_marked_obj_in_block(next_obj, block_end);
  
  if(next_marked_obj){
    if(next_marked_obj != next_obj)
      obj_set_prefetched_next_pointer(next_obj, next_marked_obj);
  } else {
      obj_set_prefetched_next_pointer(next_obj, 0);
  }
  
  return cur_marked_obj;  
}

Partial_Reveal_Object *block_get_next_marked_obj_after_prefetch(Block_Header *block, void **start_pos)
{
  Partial_Reveal_Object *cur_obj = (Partial_Reveal_Object *)(*start_pos);
  Partial_Reveal_Object *block_end = (Partial_Reveal_Object *)block->free;

  if(cur_obj >= block_end)
    return NULL;
  
  Partial_Reveal_Object *cur_marked_obj;
  
  if(obj_is_marked_in_vt(cur_obj))
    cur_marked_obj = cur_obj;
  else if (obj_is_fw_in_oi(cur_obj))
    /* why we need this obj_is_fw_in_oi(cur_obj) check. It's because of one source block with two dest blocks.
       In that case, the second half of it might have been copied by another thread, so the live objects's 
       markbit is cleared. When the thread for the first half reaches the first object of the second half, 
       it finds there is no markbit in vt. But it still want to get the forward pointer of this object, 
       and figure out that, the target address (forward pointer) is in another dest block than current one; 
       so it knows it finishes its part of the source block, and will get next source source block. 
       Without this check, it will get prefetch pointer from oi, and finds the forward pointer (and with fwd_bit).
       It's not the next live object, so it's wrong.
       
       Now I simply let it to return NULL when it finds ! obj_is_marked_in_vt(cur_obj) but obj_is_fw_in_oi(cur_obj).
       This is the same in logic but clearer.
       Change from original code: 
          if(obj_is_marked_in_vt(cur_obj) || obj_is_fw_in_oi(cur_obj) )
                  cur_marked_obj = cur_obj;
          else
                  cur_marked_obj = obj_get_prefetched_next_pointer(cur_obj); 
          
       To current code:
          if(obj_is_marked_in_vt(cur_obj) )
                  cur_marked_obj = cur_obj;
          else if (obj_is_fw_in_oi(cur_obj) )
                  return NULL;
          else
                  cur_marked_obj = obj_get_prefetched_next_pointer(cur_obj); 
    */
    return NULL;
  else
    cur_marked_obj = obj_get_prefetched_next_pointer(cur_obj);
  
  if(!cur_marked_obj)
    return NULL;
  
#ifdef USE_32BITS_HASHCODE  
  Partial_Reveal_Object *next_obj = obj_end_extend(cur_marked_obj);  
#else
  Partial_Reveal_Object *next_obj = obj_end(cur_marked_obj);
#endif

  *start_pos = next_obj;
  
  return cur_marked_obj;
}
