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

#include "../common/gc_common.h"

#include "gc_ms.h"
#include "../common/gc_concurrent.h"
#include "wspace_mark_sweep.h"
#include "../finalizer_weakref/finalizer_weakref.h"
#include "../common/compressed_ref.h"
#include "../thread/conclctor.h"
#include "../verify/verify_live_heap.h"
#ifdef USE_32BITS_HASHCODE
#include "../common/hashcode.h"
#endif

GC* gc_ms_create()
{
  GC* gc = (GC*)STD_MALLOC(sizeof(GC_MS));  
  assert(gc);
  memset(gc, 0, sizeof(GC_MS));
  return gc;
}

void gc_ms_initialize(GC_MS *gc_ms, POINTER_SIZE_INT min_heap_size, POINTER_SIZE_INT max_heap_size)
{
  assert(gc_ms);
  
  max_heap_size = round_down_to_size(max_heap_size, SPACE_ALLOC_UNIT);
  min_heap_size = round_up_to_size(min_heap_size, SPACE_ALLOC_UNIT);
  assert(max_heap_size <= max_heap_size_bytes);
  assert(max_heap_size >= min_heap_size_bytes);
  
  void *wspace_base;
  wspace_base = vm_reserve_mem(0, max_heap_size);
  wspace_initialize((GC*)gc_ms, wspace_base, max_heap_size, max_heap_size);
  
  HEAP_BASE = (POINTER_SIZE_INT)wspace_base;
  
  gc_ms->heap_start = wspace_base;
  gc_ms->heap_end = (void*)((POINTER_SIZE_INT)wspace_base + max_heap_size);
  gc_ms->reserved_heap_size = max_heap_size;
  gc_ms->committed_heap_size = max_heap_size;
  gc_ms->num_collections = 0;
  gc_ms->time_collections = 0;
}

void gc_ms_destruct(GC_MS *gc_ms)
{
  Wspace *wspace = gc_ms->wspace;
  void *wspace_start = wspace->heap_start;
  wspace_destruct(wspace);
  gc_ms->wspace = NULL;
  vm_unmap_mem(wspace_start, space_committed_size((Space*)wspace));
}

void gc_ms_reclaim_heap(GC_MS *gc)
{
  //if(verify_live_heap) gc_verify_heap((GC*)gc, TRUE);
  
  Wspace *wspace = gc_ms_get_wspace(gc);
  
  wspace_collection(wspace);
  
  wspace_reset_after_collection(wspace);
  
  //if(verify_live_heap) gc_verify_heap((GC*)gc, FALSE);
}

void wspace_mark_scan_concurrent(Conclctor* marker);
void wspace_last_mc_marker_work(Conclctor *last_marker);
void wspace_last_otf_marker_work(Conclctor *last_marker);

void gc_ms_start_con_mark(GC_MS* gc, unsigned int num_markers)
{
  if(gc->num_active_markers == 0)
    pool_iterator_init(gc->metadata->gc_rootset_pool);

  set_marker_final_func( (TaskType)wspace_last_otf_marker_work );
  conclctor_execute_task_concurrent((GC*)gc,(TaskType)wspace_mark_scan_concurrent,(Space*)gc->wspace, num_markers, CONCLCTOR_ROLE_MARKER);
}

void wspace_mark_scan_mostly_concurrent(Conclctor* marker);
void wspace_last_mc_marker_work(Conclctor* marker);

void gc_ms_start_mostly_con_mark(GC_MS* gc, unsigned int num_markers)
{
  if(gc->num_active_markers == 0)
    pool_iterator_init(gc->metadata->gc_rootset_pool);
  
  set_marker_final_func( (TaskType)wspace_last_mc_marker_work );
  conclctor_execute_task_concurrent((GC*)gc,(TaskType)wspace_mark_scan_mostly_concurrent,(Space*)gc->wspace, num_markers, CONCLCTOR_ROLE_MARKER);
}


void wspace_final_mark_scan_mostly_concurrent( Conclctor *marker );
void conclctor_execute_task_synchronized(GC* gc, TaskType task_func, Space* space, unsigned int num_markers, unsigned int role);

void gc_ms_start_mostly_con_final_mark(GC_MS* gc, unsigned int num_markers)
{
  pool_iterator_init(gc->metadata->gc_rootset_pool);
  
  conclctor_execute_task_synchronized( (GC*)gc,(TaskType)wspace_final_mark_scan_mostly_concurrent,(Space*)gc->wspace, num_markers, CONCLCTOR_ROLE_MARKER ); 
  
  /*
  collector_execute_task( (GC*)gc,(TaskType)wspace_mark_scan_mostly_concurrent,(Space*)gc->wspace );
  collector_set_weakref_sets( (GC*)gc );
  */
}

/*FIXME: move this function out of this file.*/
void gc_check_mutator_allocation(GC* gc)
{
  lock(gc->mutator_list_lock);     // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

  Mutator *mutator = gc->mutator_list;
  while(mutator){
    wait_mutator_signal(mutator, HSIG_MUTATOR_SAFE);
    mutator = mutator->next;
  }

  unlock(gc->mutator_list_lock);
}


void wspace_sweep_concurrent(Conclctor* collector);
void wspace_last_sweeper_work(Conclctor *last_sweeper);
//void gc_con_print_stat_heap_utilization_rate(GC *gc);
  void gc_ms_get_current_heap_usage(GC_MS *gc);

void gc_ms_start_con_sweep(GC_MS* gc, unsigned int num_conclctors)
{
  ops_color_flip();
  mem_fence();
  gc_check_mutator_allocation((GC*)gc);
  gc_disable_alloc_obj_live((GC*)gc);
  //just debugging
  //gc_con_print_stat_heap_utilization_rate((GC*)gc);
  //INFO2("gc.scheduler", "=== Start Con Sweeping ===");
  Con_Collection_Statistics *con_collection_stat = gc_ms_get_con_collection_stat(gc);
  con_collection_stat->sweeping_time = time_now();
  
  gc_ms_get_current_heap_usage(gc);
  gc_clear_conclctor_role((GC*)gc);
  wspace_init_pfc_pool_iterator(gc->wspace);
  set_sweeper_final_func( (TaskType)wspace_last_sweeper_work );
  conclctor_execute_task_concurrent((GC*)gc, (TaskType)wspace_sweep_concurrent, (Space*)gc->wspace, num_conclctors, CONCLCTOR_ROLE_SWEEPER);

  //conclctor_release_weakref_sets((GC*)gc);
}

unsigned int gc_ms_get_live_object_size(GC_MS* gc)
{
  POINTER_SIZE_INT num_live_obj = 0;
  POINTER_SIZE_INT size_live_obj = 0;
  
  unsigned int num_collectors = gc->num_active_collectors;
  Collector** collectors = gc->collectors;
  unsigned int i;
  for(i = 0; i < num_collectors; i++){
    Collector* collector = collectors[i];
    num_live_obj += collector->live_obj_num;
    size_live_obj += collector->live_obj_size;
  }
  return size_live_obj;
}


void gc_ms_update_space_stat(GC_MS* gc)
{
  POINTER_SIZE_INT num_live_obj = 0;
  POINTER_SIZE_INT size_live_obj = 0;
  
  Space_Statistics* wspace_stat = gc->wspace->space_statistic;

  unsigned int num_collectors = gc->num_active_collectors;
  Collector** collectors = gc->collectors;
  unsigned int i;
  for(i = 0; i < num_collectors; i++){
    Collector* collector = collectors[i];
    num_live_obj += collector->live_obj_num;
    size_live_obj += collector->live_obj_size;
  }

  wspace_stat->size_new_obj = gc_get_mutator_new_obj_size( (GC*)gc );  
  wspace_stat->num_live_obj = num_live_obj;
  wspace_stat->size_live_obj = size_live_obj;  
  wspace_stat->last_size_free_space = wspace_stat->size_free_space;
  wspace_stat->size_free_space = gc->committed_heap_size - size_live_obj;/*TODO:inaccurate value.*/  
  wspace_stat->space_utilization_ratio = (float)wspace_stat->size_new_obj / wspace_stat->last_size_free_space;
  
  INFO2("gc.space.stat","[GC][Space Stat] num_live_obj        : "<<wspace_stat->num_live_obj<<" ");
  INFO2("gc.space.stat","[GC][Space Stat] size_live_obj       : "<<wspace_stat->size_live_obj<<" ");
  INFO2("gc.space.stat","[GC][Space Stat] size_free_space     : "<<wspace_stat->size_free_space<<" ");
  INFO2("gc.space.stat","[GC][Space Stat] last_size_free_space: "<<wspace_stat->last_size_free_space<<" ");
  INFO2("gc.space.stat","[GC][Space Stat] size_new_obj        : "<<wspace_stat->size_new_obj<<" ");  
  INFO2("gc.space.stat","[GC][Space Stat] utilization_ratio   : "<<wspace_stat->space_utilization_ratio<<" ");

}

void gc_ms_reset_space_stat(GC_MS* gc)
{
  Space_Statistics* wspace_stat = gc->wspace->space_statistic;
  wspace_stat->size_new_obj = 0;
  wspace_stat->num_live_obj = 0;
  wspace_stat->size_live_obj = 0; 
  wspace_stat->space_utilization_ratio = 0;
}

void gc_ms_iterate_heap(GC_MS *gc)
{
}


