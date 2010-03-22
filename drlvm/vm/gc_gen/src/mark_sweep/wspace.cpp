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

#include "wspace.h"
#include "wspace_chunk.h"
#include "wspace_verify.h"
#include "gc_ms.h"
#include "../gen/gen.h"

struct GC_Gen;

Wspace *wspace_initialize(GC *gc, void *start, POINTER_SIZE_INT wspace_size, POINTER_SIZE_INT commit_size)
{
  /* With wspace in the heap, the heap must be composed of a single wspace or a wspace and a NOS.
   * In either case, the reserved size and committed size of wspace must be the same.
   * Because wspace has only mark-sweep collection, it is not possible to shrink wspace.
   * So there is no need to use dynamic space resizing.
   */
  assert(wspace_size == commit_size);
  
  Wspace *wspace = (Wspace*)STD_MALLOC(sizeof(Wspace));
  assert(wspace);
  memset(wspace, 0, sizeof(Wspace));
  
  wspace->reserved_heap_size = wspace_size;
  
  void *reserved_base = start;
  
  /* commit wspace mem */
  if(!large_page_hint)
    vm_commit_mem(reserved_base, commit_size);
  memset(reserved_base, 0, commit_size);
  wspace->committed_heap_size = commit_size;
  
  wspace->heap_start = reserved_base;
  wspace->heap_end = (void *)((POINTER_SIZE_INT)reserved_base + wspace_size);
    
  wspace->num_collections = 0;
  wspace->time_collections = 0;
  wspace->survive_ratio = 0.2f;

  wspace->move_object = FALSE;
  wspace->gc = gc;
  
  wspace_init_chunks(wspace);

  wspace->space_statistic = (Space_Statistics*)STD_MALLOC(sizeof(Space_Statistics));
  assert(wspace->space_statistic);
  memset(wspace->space_statistic, 0, sizeof(Space_Statistics));
  wspace->space_statistic->size_free_space = commit_size;

  wspace->con_collection_statistics = (Con_Collection_Statistics*)STD_MALLOC(sizeof(Con_Collection_Statistics));
  memset(wspace->con_collection_statistics, 0, sizeof(Con_Collection_Statistics));
  wspace->con_collection_statistics->heap_utilization_rate = DEFAULT_HEAP_UTILIZATION_RATE;

#ifdef USE_UNIQUE_MARK_SWEEP_GC
  gc_ms_set_wspace((GC_MS*)gc, wspace);
#else
  gc_set_mos((GC_Gen*)gc, (Space*)wspace);
#endif

#ifdef SSPACE_VERIFY
  wspace_verify_init(gc);
#endif
  return wspace;
}

static void wspace_destruct_chunks(Wspace *wspace) { return; }

void wspace_destruct(Wspace *wspace)
{
  //FIXME:: when map the to-half, the decommission start address should change
  wspace_destruct_chunks(wspace);

  /* we don't free the real space here, the heap will be freed altogether */
  STD_FREE(wspace);
  wspace = NULL;
}

void wspace_reset_after_collection(Wspace *wspace)
{
  wspace->move_object = FALSE;
  wspace->need_compact = FALSE;
  wspace->need_fix = FALSE;
}

void allocator_init_local_chunks(Allocator *allocator)
{
  Wspace *wspace = gc_get_wspace(allocator->gc);
  Size_Segment **size_segs = wspace->size_segments;
  
  /* Alloc mem for size segments (Chunk_Header**) */
  unsigned int seg_size = sizeof(Chunk_Header**) * SIZE_SEGMENT_NUM;
  Chunk_Header ***local_chunks = (Chunk_Header***)STD_MALLOC(seg_size);
  memset(local_chunks, 0, seg_size);
  
  /* Alloc mem for local chunk pointers */
  unsigned int chunk_ptr_size = 0;
  for(unsigned int i = SIZE_SEGMENT_NUM; i--;){
    if(size_segs[i]->local_alloc){
      chunk_ptr_size += size_segs[i]->chunk_num;
    }
  }
  chunk_ptr_size *= sizeof(Chunk_Header*);
  Chunk_Header **chunk_ptrs = (Chunk_Header**)STD_MALLOC(chunk_ptr_size);
  memset(chunk_ptrs, 0, chunk_ptr_size);
  
  for(unsigned int i = 0; i < SIZE_SEGMENT_NUM; ++i){
    if(size_segs[i]->local_alloc){
      local_chunks[i] = chunk_ptrs;
      chunk_ptrs += size_segs[i]->chunk_num;
    }
  }
  
  allocator->local_chunks = local_chunks;
}

void allocactor_destruct_local_chunks(Allocator *allocator)
{
  Wspace *wspace = gc_get_wspace(allocator->gc);
  Size_Segment **size_segs = wspace->size_segments;
  Chunk_Header ***local_chunks = allocator->local_chunks;
  Chunk_Header **chunk_ptrs = NULL;
  unsigned int chunk_ptr_num = 0;
  
  /* Find local chunk pointers' head and their number */
  for(unsigned int i = 0; i < SIZE_SEGMENT_NUM; ++i){
    if(size_segs[i]->local_alloc){
      chunk_ptr_num = size_segs[i]->chunk_num;
      assert(local_chunks[i]);
      if(!chunk_ptrs){
        chunk_ptrs = local_chunks[i];

        /* Put local pfc to the according pools */
        for(unsigned int j = 0; j < chunk_ptr_num; ++j){
          if(chunk_ptrs[j]){
            if(!gc_is_specify_con_gc()){
              wspace_put_pfc(wspace, chunk_ptrs[j]);
            }else{
              Chunk_Header* chunk_to_rem = chunk_ptrs[j];
              chunk_to_rem->status = CHUNK_USED | CHUNK_NORMAL;
              wspace_reg_used_chunk(wspace, chunk_to_rem);
            }
          }
        }

        chunk_ptrs = NULL;
      }
    }
  }
  
  /* Free mem for local chunk pointers */  
  STD_FREE(*local_chunks);
  /* Free mem for size segments (Chunk_Header**) */
  STD_FREE(local_chunks);
}

static void allocator_clear_local_chunks(Allocator *allocator)
{
  Wspace *wspace = gc_get_wspace(allocator->gc);
  Size_Segment **size_segs = wspace->size_segments;
  Chunk_Header ***local_chunks = allocator->local_chunks;
  
  for(unsigned int i = SIZE_SEGMENT_NUM; i--;){
    if(!size_segs[i]->local_alloc){
      assert(!local_chunks[i]);
      continue;
    }
    Chunk_Header **chunks = local_chunks[i];
    assert(chunks);
    for(unsigned int j = size_segs[i]->chunk_num; j--;){
      if(chunks[j])
        wspace_put_pfc(wspace, chunks[j]);
      chunks[j] = NULL;
    }
  }
}

static void gc_clear_mutator_local_chunks(GC *gc)
{
#ifdef USE_UNIQUE_MARK_SWEEP_GC
  /* release local chunks of each mutator in unique mark-sweep GC */
  Mutator *mutator = gc->mutator_list;
  while(mutator){
    allocator_clear_local_chunks((Allocator*)mutator);
    mutator = mutator->next;
  }
#endif
}

void gc_clear_collector_local_chunks(GC *gc)
{
  if(!collect_is_major()) return;
  /* release local chunks of each collector in gen GC */
  for(unsigned int i = gc->num_collectors; i--;){
    allocator_clear_local_chunks((Allocator*)gc->collectors[i]);
  }
}


extern void wspace_decide_compaction_need(Wspace *wspace);
extern void mark_sweep_wspace(Collector *collector);

void wspace_collection(Wspace *wspace) 
{
  GC *gc = wspace->gc;
  wspace->num_collections++;
  
  gc_clear_mutator_local_chunks(gc);
  gc_clear_collector_local_chunks(gc);
  
#ifdef SSPACE_ALLOC_INFO
  wspace_alloc_info_summary();
#endif
#ifdef SSPACE_CHUNK_INFO
  wspace_chunks_info(wspace, TRUE);
#endif
  wspace_clear_used_chunk_pool(wspace);


  wspace_decide_compaction_need(wspace);
  if(wspace->need_compact && gc_is_kind(ALGO_MARKSWEEP)){
    assert(!collect_move_object());
    GC_PROP |= ALGO_MS_COMPACT;
  }
  if(wspace->need_compact || collect_is_major())
    wspace->need_fix = TRUE;

  //printf("\n\n>>>>>>>>%s>>>>>>>>>>>>\n\n", wspace->need_compact ? "COMPACT" : "NO COMPACT");
#ifdef SSPACE_VERIFY
  wspace_verify_before_collection(gc);
  wspace_verify_vtable_mark(gc);
#endif

#ifdef SSPACE_TIME
  wspace_gc_time(gc, TRUE);
#endif

  pool_iterator_init(gc->metadata->gc_rootset_pool);
  wspace_clear_chunk_list(wspace);
  
  collector_execute_task(gc, (TaskType)mark_sweep_wspace, (Space*)wspace);

  /* set the collection type back to ms_normal in case it's ms_compact */
  collect_set_ms_normal();
  
#ifdef SSPACE_TIME
  wspace_gc_time(gc, FALSE);
#endif

#ifdef SSPACE_CHUNK_INFO
  wspace_chunks_info(wspace, TRUE);
#endif

}
