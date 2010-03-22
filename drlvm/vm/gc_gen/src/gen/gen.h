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

#ifndef _GC_GEN_H_
#define _GC_GEN_H_


#include "../common/gc_common.h"
#include "../thread/gc_thread.h"
#include "../common/gc_for_barrier.h"

#include "../finalizer_weakref/finalizer_weakref_metadata.h"

#ifdef GC_GEN_STATS
struct GC_Gen_Stats;
#endif

void gc_set_gen_mode(Boolean status);

/* some globals */
extern POINTER_SIZE_INT NOS_SIZE;

/* heap size limit is not interesting. only for manual tuning purpose */
extern POINTER_SIZE_INT min_heap_size_bytes;
extern POINTER_SIZE_INT max_heap_size_bytes;

/* fspace size is variable, adjusted adaptively within the range */
extern POINTER_SIZE_INT min_nos_size_bytes;
extern POINTER_SIZE_INT max_nos_size_bytes;

#include "../trace_forward/fspace.h"
#include "../semi_space/sspace.h"
#include "../mark_compact/mspace.h"
#include "../los/lspace.h"
#include "../mark_sweep/wspace.h"

struct Gen_Mode_Adaptor;

typedef struct GC_Gen {
  /* <-- First couple of fields overloaded as GC */
  void* physical_start;
  void* heap_start;
  void* heap_end;
  POINTER_SIZE_INT reserved_heap_size;
  POINTER_SIZE_INT committed_heap_size;
  unsigned int num_collections;
  Boolean in_collection;
  int64 time_collections;
  float survive_ratio;  
  
  /* mutation related info */
  Mutator *mutator_list;
  SpinLock mutator_list_lock;
  unsigned int num_mutators;

  /* collection related info */    
  Collector** collectors;
  unsigned int num_collectors;
  unsigned int num_active_collectors; /* not all collectors are working */

  /*concurrent markers and collectors*/
  Conclctor** conclctors;
  unsigned int num_conclctors;
  //unsigned int num_active_conclctors;
  unsigned int num_active_markers;
  unsigned int num_active_sweepers;

  /* metadata is the pool for rootset, markstack, etc. */  
  GC_Metadata* metadata;
  Finref_Metadata *finref_metadata;

  unsigned int collect_kind; /* MAJOR or MINOR */
  unsigned int last_collect_kind;
  unsigned int cause;/*GC_CAUSE_LOS_IS_FULL, GC_CAUSE_NOS_IS_FULL, or GC_CAUSE_RUNTIME_FORCE_GC*/  
  Boolean collect_result; /* succeed or fail */
  
  Boolean generate_barrier;

  /* FIXME:: this is wrong! root_set belongs to mutator */
  Vector_Block* root_set;
  Vector_Block* weakroot_set;
  Vector_Block* uncompressed_root_set;
  
  //For_LOS_extend
  Space_Tuner* tuner;
  
  volatile unsigned int gc_concurrent_status;
  Collection_Scheduler* collection_scheduler;

  SpinLock lock_con_mark;
  SpinLock lock_enum;
  SpinLock lock_con_sweep;
  SpinLock lock_collect_sched;

  
  /* system info */
  unsigned int _system_alloc_unit;
  unsigned int _machine_page_size_bytes;
  unsigned int _num_processors;
  /* END of GC --> */
  
  Block* blocks;
  Space *nos;
  Space *mos;
  Space *los;
      
  Boolean next_collect_force_major;
  Gen_Mode_Adaptor* gen_mode_adaptor;
  Boolean force_gen_mode;

#ifdef GC_GEN_STATS
  GC_Gen_Stats* stats; /*used to record stats when collection*/
#endif

} GC_Gen;

//////////////////////////////////////////////////////////////////////////////////////////

void gc_gen_initialize(GC_Gen *gc, POINTER_SIZE_INT initial_heap_size, POINTER_SIZE_INT final_heap_size);
void gc_gen_destruct(GC_Gen *gc);
void gc_gen_collection_verbose_info(GC_Gen *gc, int64 pause_time, int64 time_mutator);
void gc_gen_space_verbose_info(GC_Gen *gc);
void gc_gen_init_verbose(GC_Gen *gc);
void gc_gen_wrapup_verbose(GC_Gen* gc);
                        
/////////////////////////////////////////////////////////////////////////////////////////

void gc_nos_initialize(GC_Gen *gc, void *start, POINTER_SIZE_INT nos_size, POINTER_SIZE_INT commit_size);
void gc_nos_destruct(GC_Gen *gc);
void gc_mos_initialize(GC_Gen *gc, void *start, POINTER_SIZE_INT mos_size, POINTER_SIZE_INT commit_size);
void gc_mos_destruct(GC_Gen *gc);
void gc_los_initialize(GC_Gen *gc, void *start, POINTER_SIZE_INT los_size);
void gc_los_destruct(GC_Gen *gc);

inline Space* space_of_addr(GC* gc, void* addr)
{
  assert(address_belongs_to_gc_heap(addr, gc));
  if( addr > nos_boundary) return (Space*)((GC_Gen*)gc)->nos;
  if( addr > los_boundary) return (Space*)((GC_Gen*)gc)->mos;
  return (Space*)((GC_Gen*)gc)->los;
}

extern Space_Alloc_Func mos_alloc;
extern Space_Alloc_Func nos_alloc;
extern Space_Alloc_Func los_alloc;
void* los_try_alloc(POINTER_SIZE_INT size, GC* gc);

Space* gc_get_nos(GC_Gen* gc);
Space* gc_get_mos(GC_Gen* gc);
Space* gc_get_los(GC_Gen* gc);

void gc_set_nos(GC_Gen* gc, Space* nos);
void gc_set_mos(GC_Gen* gc, Space* mos);
void gc_set_los(GC_Gen* gc, Space* los);

GC* gc_gen_decide_collection_algo(char* minor_algo, char* major_algo, Boolean has_los);
void gc_gen_decide_collection_kind(GC_Gen* gc, unsigned int cause);

void gc_gen_adapt(GC_Gen* gc, int64 pause_time);

void gc_gen_reclaim_heap(GC_Gen* gc, int64 gc_start_time);

void gc_gen_assign_free_area_to_mutators(GC_Gen* gc);
void gc_gen_init_collector_alloc(GC_Gen* gc, Collector* collector);
void gc_gen_reset_collector_alloc(GC_Gen* gc, Collector* collector);
void gc_gen_destruct_collector_alloc(GC_Gen* gc, Collector* collector);

void gc_gen_adjust_heap_size(GC_Gen* gc, int64 pause_time);

void gc_gen_update_space_before_gc(GC_Gen* gc);
void gc_gen_update_space_after_gc(GC_Gen* gc);

void gc_gen_mode_adapt_init(GC_Gen *gc);

void gc_gen_iterate_heap(GC_Gen *gc);

void gc_gen_start_concurrent_mark(GC_Gen* gc);

extern Boolean GEN_NONGEN_SWITCH ;

POINTER_SIZE_INT mos_free_space_size(Space* mos);
POINTER_SIZE_INT nos_free_space_size(Space* nos);
POINTER_SIZE_INT mos_used_space_size(Space* mos);
POINTER_SIZE_INT nos_used_space_size(Space* nos);

inline POINTER_SIZE_INT gc_gen_free_memory_size(GC_Gen* gc)
{  return nos_free_space_size((Space*)gc->nos) +
          blocked_space_free_mem_size((Blocked_Space*)gc->mos) +
          lspace_free_memory_size((Lspace*)gc->los);  }
                    
inline POINTER_SIZE_INT gc_gen_total_memory_size(GC_Gen* gc)
{  return space_committed_size((Space*)gc->nos) +
          space_committed_size((Space*)gc->mos) +
          lspace_committed_size((Lspace*)gc->los);  }

#ifndef STATIC_NOS_MAPPING
void* nos_space_adjust(Space* space, void* new_nos_boundary, POINTER_SIZE_INT new_nos_size);
#endif

#endif /* ifndef _GC_GEN_H_ */






