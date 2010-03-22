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

#ifndef _GC_SPACE_H_
#define _GC_SPACE_H_

#include "gc_block.h"

extern unsigned int SPACE_ALLOC_UNIT;

typedef struct Space_Statistics{
  POINTER_SIZE_INT num_live_obj;
  POINTER_SIZE_INT size_live_obj;
  POINTER_SIZE_INT size_free_space;
  POINTER_SIZE_INT last_size_free_space;
  POINTER_SIZE_INT size_new_obj;
  float space_utilization_ratio;
}Space_Statistics;

struct GC;
/* all Spaces inherit this Space structure */
typedef struct Space{
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
}Space;

struct Allocator;
typedef void *(*Space_Alloc_Func)(unsigned, Allocator *);

inline POINTER_SIZE_INT space_committed_size(Space* space){ return space ? space->committed_heap_size : 0; }
inline void* space_heap_start(Space* space){ return space->heap_start; }
inline void* space_heap_end(Space* space){ return space->heap_end; }

inline Boolean address_belongs_to_space(void* addr, Space* space) 
{
  return (addr >= space_heap_start(space) && addr < space_heap_end(space));
}

inline Boolean obj_belongs_to_space(Partial_Reveal_Object *p_obj, Space* space)
{
  return address_belongs_to_space((Partial_Reveal_Object*)p_obj, space);
}


typedef struct Blocked_Space {
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
  unsigned int first_block_idx;
  unsigned int ceiling_block_idx;
  volatile unsigned int free_block_idx;
  
  unsigned int num_used_blocks;
  unsigned int num_managed_blocks;
  unsigned int num_total_blocks;
  
  volatile Block_Header* block_iterator;
  /* END of Blocked_Space --> */
}Blocked_Space;

inline Boolean blocked_space_has_free_block(Blocked_Space *space){ return space->free_block_idx <= space->ceiling_block_idx; }
inline unsigned int blocked_space_free_mem_size(Blocked_Space *space){ return (space->ceiling_block_idx - space->free_block_idx + 1) << GC_BLOCK_SHIFT_COUNT;  }
inline Boolean blocked_space_used_mem_size(Blocked_Space *space){ return (space->free_block_idx - space->first_block_idx) << GC_BLOCK_SHIFT_COUNT; }

void space_init_blocks(Blocked_Space* space);
void space_desturct_blocks(Blocked_Space* space);

void blocked_space_shrink(Blocked_Space* space, unsigned int changed_size);
void blocked_space_extend(Blocked_Space* space, unsigned int changed_size);

void blocked_space_block_iterator_init(Blocked_Space *space);
void blocked_space_block_iterator_init_free(Blocked_Space *space);
Block_Header *blocked_space_block_iterator_get(Blocked_Space *space);
Block_Header *blocked_space_block_iterator_next(Blocked_Space *space);

#ifndef STATIC_NOS_MAPPING
void blocked_space_adjust(Blocked_Space* space, void* new_space_start, POINTER_SIZE_INT new_space_size);
#endif

#endif //#ifndef _GC_SPACE_H_
