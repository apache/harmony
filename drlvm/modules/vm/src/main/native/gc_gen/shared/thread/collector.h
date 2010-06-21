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

#ifndef _COLLECTOR_H_
#define _COLLECTOR_H_

#include "../common/gc_space.h"

struct Block_Header;
struct Stealable_Stack;
struct Chunk_Header;
struct Free_Chunk_List;

#define NORMAL_SIZE_SEGMENT_GRANULARITY_BITS  8
#define NORMAL_SIZE_SEGMENT_GRANULARITY (1 << NORMAL_SIZE_SEGMENT_GRANULARITY_BITS)
#define NORMAL_SIZE_SEGMENT_NUM (GC_LOS_OBJ_SIZE_THRESHOLD / NORMAL_SIZE_SEGMENT_GRANULARITY)
#define SIZE_TO_SEGMENT_INDEX(size) ((((size) + NORMAL_SIZE_SEGMENT_GRANULARITY-1) >> NORMAL_SIZE_SEGMENT_GRANULARITY_BITS) - 1)
#define SEGMENT_INDEX_TO_SIZE(index)  (((index)+1) << NORMAL_SIZE_SEGMENT_GRANULARITY_BITS)

typedef struct Collector{
  /* <-- first couple of fields are overloaded as Allocator */
  void *free;
  void *ceiling;
  void *end;
  void *alloc_block;
  Chunk_Header ***local_chunks; /* this is for MARK-SWEEP GC */
  Space* alloc_space;
  GC* gc;
  VmThreadHandle thread_handle;   /* This thread; */
  unsigned int handshake_signal; /*Handshake is used in concurrent GC.*/
  unsigned int num_alloc_blocks; /* the number of allocated blocks in this collection. */
  int64 time_measurement_start;
  int64 time_measurement_end;
  /* End of Allocator --> */

  /* FIXME:: for testing */
  Space* collect_space;
  
  /* backup allocator in case there are two target copy spaces, such as semispace GC */
  Allocator* backup_allocator;

  Vector_Block *trace_stack;
  
  Vector_Block* rep_set; /* repointed set */
  Vector_Block* rem_set;
#ifdef USE_32BITS_HASHCODE
  Vector_Block* hashcode_set;
#endif
  
  Vector_Block *softref_set;
  Vector_Block *weakref_set;
  Vector_Block *phanref_set;
  
  VmEventHandle task_assigned_event;
  VmEventHandle task_finished_event;
  
  Block_Header* cur_compact_block;
  Block_Header* cur_target_block;
  
  Free_Chunk_List *free_chunk_list;

  POINTER_SIZE_INT live_obj_size;
  POINTER_SIZE_INT live_obj_num;
  
  void(*task_func)(void*) ;   /* current task */
  
  /* following three fields are to support LOS extension: to estimate MOS size */
  POINTER_SIZE_INT non_los_live_obj_size;
  POINTER_SIZE_INT los_live_obj_size;
  POINTER_SIZE_INT segment_live_size[NORMAL_SIZE_SEGMENT_NUM];
  unsigned int result;

  Boolean collector_is_active;

  /*for collect statistics info*/
#ifdef GC_GEN_STATS
  void* stats;
#endif

 
}Collector;

void collector_destruct(GC* gc);
void collector_initialize(GC* gc);
void collector_reset(GC* gc);

void collector_execute_task(GC* gc, TaskType task_func, Space* space);
void collector_execute_task_concurrent(GC* gc, TaskType task_func, Space* space, unsigned int num_collectors);
void collector_release_weakref_sets(GC* gc, unsigned int num_collectors);

void collector_restore_obj_info(Collector* collector);
#ifdef USE_32BITS_HASHCODE
void collector_attach_hashcode(Collector *collector);
#endif

#ifndef USE_UNIQUE_MARK_SWEEP_GC
void gc_gen_hook_for_collector_init(Collector *collector);
#endif

Boolean is_collector_finished(GC* gc);
void wait_collection_finish(GC* gc);
int64 gc_get_collector_time(GC* gc);

inline Boolean gc_collection_result(GC* gc)
{
  Boolean result = TRUE;
  for(unsigned i=0; i<gc->num_active_collectors; i++){
    Collector* collector = gc->collectors[i];
    result &= collector->result;
  }  
  return result;
}

inline void gc_reset_collect_result(GC* gc)
{
  for(unsigned i=0; i<gc->num_active_collectors; i++){
    Collector* collector = gc->collectors[i];
    collector->result = TRUE;
  }  
  
  gc->collect_result = TRUE;
  return;
}


#endif //#ifndef _COLLECTOR_H_
