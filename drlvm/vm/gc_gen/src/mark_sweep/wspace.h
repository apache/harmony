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

#ifndef _SWEEP_SPACE_H_
#define _SWEEP_SPACE_H_

#include "../thread/gc_thread.h"
#include "../thread/collector_alloc.h"
#include "../thread/mutator.h"
#include "../common/gc_common.h"
#include "../common/gc_concurrent.h"

#define DEFAULT_HEAP_UTILIZATION_RATE 0.92f
/*
 * The sweep space accomodates objects collected by mark-sweep
 */

struct Size_Segment;
struct Free_Chunk_List;
struct Con_Collection_Statistics;

typedef struct Wspace {
  /* <-- first couple of fields are overloadded as Space */
  void *heap_start;
  void *heap_end;
  POINTER_SIZE_INT reserved_heap_size;
  POINTER_SIZE_INT committed_heap_size;
  unsigned int num_collections;
  int64 time_collections;
  float survive_ratio;
  unsigned int collect_algorithm;
  GC *gc;
  Boolean move_object;

  Space_Statistics* space_statistic;

  /* Size allocted since last minor collection. */
  volatile POINTER_SIZE_INT last_alloced_size;
  /* Size allocted since last major collection. */
  volatile POINTER_SIZE_INT accumu_alloced_size;
  /* Total size allocated since VM starts. */
  volatile POINTER_SIZE_INT total_alloced_size;

  /* Size survived from last collection. */
  POINTER_SIZE_INT last_surviving_size;
  /* Size survived after a certain period. */
  POINTER_SIZE_INT period_surviving_size;  

  /* END of Space --> */
  
  Boolean need_compact;
  Boolean need_fix;   /* There are repointed ref needing fixing */
  Size_Segment **size_segments;
  Pool ***pfc_pools;
  Pool ***pfc_pools_backup;
  Pool* used_chunk_pool;
  Pool* unreusable_normal_chunk_pool;
  Pool* live_abnormal_chunk_pool;
  Free_Chunk_List *aligned_free_chunk_lists;
  Free_Chunk_List *unaligned_free_chunk_lists;
  Free_Chunk_List *hyper_free_chunk_list;
  
  Con_Collection_Statistics *con_collection_statistics;
  
} Wspace;

Wspace *wspace_initialize(GC *gc, void *start, POINTER_SIZE_INT wspace_size, POINTER_SIZE_INT commit_size);
void wspace_destruct(Wspace *wspace);
void wspace_reset_after_collection(Wspace *wspace);

void *wspace_thread_local_alloc(unsigned size, Allocator *allocator);
void *wspace_alloc(unsigned size, Allocator *allocator);

void wspace_collection(Wspace *wspace);

void allocator_init_local_chunks(Allocator *allocator);
void allocactor_destruct_local_chunks(Allocator *allocator);
void gc_init_collector_free_chunk_list(Collector *collector);

POINTER_SIZE_INT wspace_free_memory_size(Wspace *wspace);


#ifndef USE_UNIQUE_MARK_SWEEP_GC
#define gc_get_wspace(gc) ((Wspace*)gc_get_mos((GC_Gen*)(gc)))
#else
#define gc_get_wspace(gc) (gc_ms_get_wspace((GC_MS*)(gc)))
#endif

#endif // _SWEEP_SPACE_H_
