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

#ifndef _SEMI_SPACE_H_
#define _SEMI_SPACE_H_

#include "../thread/gc_thread.h"
extern void* tospace_start;
extern void* tospace_end;

typedef struct Sspace{
  /* <-- first couple of fields are overloadded as Space */
  void* heap_start;
  void* heap_end;
  POINTER_SIZE_INT reserved_heap_size;
  POINTER_SIZE_INT committed_heap_size;
  unsigned int num_collections;
  int64 time_collections;
  float survive_ratio;
  unsigned int collect_algorithm;
  GC* gc;
  Boolean move_object;

  Space_Statistics* space_statistic;

  /* Size allocted since last minor collection. */
  volatile uint64 last_alloced_size;
  /* Size allocted since last major collection. */
  uint64 accumu_alloced_size;
  /* Total size allocated since VM starts. */
  uint64 total_alloced_size;

  /* Size survived from last collection. */
  uint64 last_surviving_size;
  /* Size survived after a certain period. */
  uint64 period_surviving_size;  

  /* END of Space --> */

  Block* blocks; /* short-cut for mpsace blockheader access, not mandatory */
  
  /* FIXME:: the block indices should be replaced with block header addresses */
  unsigned int first_block_idx; /* always pointing to sspace bottom */
  unsigned int ceiling_block_idx; /* tospace ceiling */
  volatile unsigned int free_block_idx; /* tospace cur free block */
  
  unsigned int num_used_blocks;
  unsigned int num_managed_blocks;
  unsigned int num_total_blocks;
  
  volatile Block_Header* block_iterator;
  /* END of Blocked_Space --> */
  
  Block_Header* cur_free_block;
  unsigned int tospace_first_idx;
  void* survivor_area_end;
  void* survivor_area_start;

}Sspace;

Sspace *sspace_initialize(GC* gc, void* start, POINTER_SIZE_INT sspace_size, POINTER_SIZE_INT commit_size);
void sspace_destruct(Sspace *sspace);

void* sspace_alloc(unsigned size, Allocator *allocator);
Boolean sspace_alloc_block(Sspace* sspace, Allocator* allocator);

void sspace_collection(Sspace* sspace);
void sspace_prepare_for_collection(Sspace* sspace);
void sspace_reset_after_collection(Sspace* sspace);

void* semispace_alloc(unsigned int size, Allocator* allocator);

void nongen_ss_pool(Collector* collector);
void gen_ss_pool(Collector* collector);

POINTER_SIZE_INT sspace_free_space_size(Sspace* nos);
POINTER_SIZE_INT sspace_used_space_size(Sspace* nos);

FORCE_INLINE Boolean sspace_has_free_block(Sspace* sspace)
{
  return (sspace->cur_free_block != NULL);
}

FORCE_INLINE Boolean obj_belongs_to_tospace(Partial_Reveal_Object* p_obj)
{
  return ( p_obj >= tospace_start && p_obj < tospace_end );
}

FORCE_INLINE Boolean obj_belongs_to_survivor_area(Sspace* sspace, Partial_Reveal_Object* p_obj)
{
  return (p_obj >= sspace->survivor_area_start && 
                          p_obj < sspace->survivor_area_end);
}

/* to be forwarded to MOS or was forwarded by other thread */
FORCE_INLINE Boolean obj_be_forwarded(Sspace* sspace, Partial_Reveal_Object* p_obj)
{
  /* NOTE:: Tricky! When the thread checks, the oi could be set a forward address by another thread. */
  Obj_Info_Type oi = get_obj_info_raw(p_obj);
  Boolean be_forwarded = (Boolean)(oi & (FORWARD_BIT|OBJ_AGE_BIT));
  assert( obj_belongs_to_survivor_area(sspace, p_obj)? be_forwarded:1);
    
  return be_forwarded;
}

/* treat semispace alloc as thread local alloc. If it fails or p_obj is old, forward it to MOS */
FORCE_INLINE void* semispace_copy_object(Partial_Reveal_Object* p_obj, unsigned int size, Allocator* allocator)
{
  void* p_targ_obj = NULL;
  Sspace* sspace = (Sspace*)allocator->alloc_space;
  
  /* this object should be forward to MOS, or is already forwarded by other thread (to either MOS or Tospace. 
     In any case, we don't need to allocate it in tospace now. */
  if( obj_be_forwarded(sspace, p_obj) ) 
    return NULL;
    
  p_targ_obj = thread_local_alloc(size, allocator);
  if(!p_targ_obj)
    p_targ_obj = semispace_alloc(size, allocator);           
  
  return p_targ_obj;
}

#ifndef STATIC_NOS_MAPPING
void* sspace_heap_start_adjust(Sspace* sspace, void* new_heap_start, POINTER_SIZE_INT new_heap_size);
#endif /* #ifndef STATIC_NOS_MAPPING  */

inline POINTER_SIZE_INT sspace_tospace_size(Sspace* space)
{
  return blocked_space_free_mem_size((Blocked_Space*)space);
}

inline POINTER_SIZE_INT sspace_survivor_area_size(Sspace* space)
{
  return (POINTER_SIZE_INT)space->survivor_area_end - (POINTER_SIZE_INT)space->survivor_area_start;
}

#endif // _FROM_SPACE_H_
