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
 * @author Xiao-Feng Li, 2006/12/3
 */

#include "gc_common.h"
#include "gc_metadata.h"
#include "../thread/mutator.h"
#include "../thread/conclctor.h"
#include "../finalizer_weakref/finalizer_weakref.h"
#include "../gen/gen.h"
#include "../mark_sweep/gc_ms.h"
#include "../move_compact/gc_mc.h"
#include "../common/space_tuner.h"
#include "interior_pointer.h"
#include "collection_scheduler.h"
#include "gc_concurrent.h"

unsigned int Cur_Mark_Bit = 0x1;
unsigned int Cur_Forward_Bit = 0x2;

unsigned int SPACE_ALLOC_UNIT;
Boolean IGNORE_FORCE_GC = FALSE;

void gc_assign_free_area_to_mutators(GC* gc)
{
#if !defined(USE_UNIQUE_MARK_SWEEP_GC) && !defined(USE_UNIQUE_MOVE_COMPACT_GC)
  gc_gen_assign_free_area_to_mutators((GC_Gen*)gc);
#endif
}

void gc_init_collector_alloc(GC* gc, Collector* collector)
{
#ifndef USE_UNIQUE_MARK_SWEEP_GC
  gc_gen_init_collector_alloc((GC_Gen*)gc, collector);
#else    
  gc_init_collector_free_chunk_list(collector);
#endif
}

void gc_reset_collector_alloc(GC* gc, Collector* collector)
{
#if !defined(USE_UNIQUE_MARK_SWEEP_GC) && !defined(USE_UNIQUE_MOVE_COMPACT_GC)
  gc_gen_reset_collector_alloc((GC_Gen*)gc, collector);
#endif    
}

void gc_destruct_collector_alloc(GC* gc, Collector* collector)
{
#ifndef USE_UNIQUE_MARK_SWEEP_GC
  gc_gen_destruct_collector_alloc((GC_Gen*)gc, collector);
#endif    
}

void gc_copy_interior_pointer_table_to_rootset();

/*used for computing collection time and mutator time*/
static int64 collection_start_time = time_now();
static int64 collection_end_time = time_now();

int64 get_gc_start_time() 
{ return collection_start_time; }

void set_gc_start_time() 
{ collection_start_time = time_now(); }

int64 get_gc_end_time()
{ return collection_end_time; }

void set_gc_end_time()
{ 
  collection_end_time = time_now();
}

void gc_decide_collection_kind(GC* gc, unsigned int cause)
{
  /* this is for debugging and for gen-nongen-switch. */
  gc->last_collect_kind = GC_PROP;

#if !defined(USE_UNIQUE_MARK_SWEEP_GC) && !defined(USE_UNIQUE_MOVE_COMPACT_GC)
  
  gc_gen_decide_collection_kind((GC_Gen*)gc, cause);

#endif

}

void gc_update_space_stat(GC* gc)
{
#ifdef USE_UNIQUE_MARK_SWEEP_GC
      gc_ms_update_space_stat((GC_MS *)gc);
#endif
}

void gc_reset_space_stat(GC* gc)
{
#ifdef USE_UNIQUE_MARK_SWEEP_GC
      gc_ms_reset_space_stat((GC_MS *)gc);
#endif
}

void gc_prepare_rootset(GC* gc)
{
  /* Stop the threads and collect the roots. */
  INFO2("gc.process", "GC: stop the threads and enumerate rootset ...\n");
  gc_clear_rootset(gc);
  gc_reset_rootset(gc);
  vm_enumerate_root_set_all_threads();
  gc_copy_interior_pointer_table_to_rootset();
  gc_set_rootset(gc);
}

void gc_reset_after_collection(GC* gc)
{
  if(gc_is_gen_mode()) gc_prepare_mutator_remset(gc);

  /* Clear rootset pools here rather than in each collection algorithm */
  gc_clear_rootset(gc);
  
  if(!gc_is_specify_con_gc()) gc_metadata_verify(gc, FALSE);
  
  if(!IGNORE_FINREF ){
    INFO2("gc.process", "GC: finref process after collection ...\n");
    gc_put_finref_to_vm(gc);
    gc_reset_finref_metadata(gc);
    gc_activate_finref_threads((GC*)gc);
#ifndef BUILD_IN_REFERENT
  } else {
    gc_clear_weakref_pools(gc);
    gc_clear_finref_repset_pool(gc);
#endif
  }

  gc_update_space_stat(gc);
  
  gc_reset_space_stat(gc);

  gc_reset_collector_state(gc);

  gc_clear_dirty_set(gc);
    
  vm_reclaim_native_objs();
  gc->in_collection = FALSE;

}

void set_check_delay( int64 mutator_time );

void gc_reclaim_heap(GC* gc, unsigned int gc_cause)
{  
  INFO2("gc.process", "\nGC: GC start ...\n");

  gc->cause = gc_cause;

  if(gc_is_specify_con_gc()){
    gc_wait_con_finish(gc);
    INFO2("gc.process", "GC: GC end\n");
    return;
  }

   set_gc_start_time();
  int64 time_mutator = get_gc_start_time() - get_gc_end_time();
  
  gc->num_collections++;

  /* FIXME:: before mutators suspended, the ops below should be very careful
     to avoid racing with mutators. */

  gc_decide_collection_kind(gc, gc_cause);

#ifdef MARK_BIT_FLIPPING
  if(collect_is_minor()) mark_bit_flip();
#endif

  gc_metadata_verify(gc, TRUE);
#ifndef BUILD_IN_REFERENT
  gc_finref_metadata_verify((GC*)gc, TRUE);
#endif

  /* Stop the threads and collect the roots. */
  lock(gc->lock_enum);
  int disable_count = hythread_reset_suspend_disable();
  gc_set_rootset_type(ROOTSET_IS_REF);
  gc_prepare_rootset(gc);
  unlock(gc->lock_enum);
    
  gc->in_collection = TRUE;
  
  /* this has to be done after all mutators are suspended */
  gc_reset_mutator_context(gc);
  
  if(!IGNORE_FINREF ) gc_set_obj_with_fin(gc);

#if defined(USE_UNIQUE_MARK_SWEEP_GC)
  gc_ms_reclaim_heap((GC_MS*)gc);
#elif defined(USE_UNIQUE_MOVE_COMPACT_GC)
  gc_mc_reclaim_heap((GC_MC*)gc);
#else
  gc_gen_reclaim_heap((GC_Gen*)gc, collection_start_time);
#endif

  set_gc_end_time();

  int64 time_collection = get_gc_end_time() - get_gc_start_time();

#if !defined(USE_UNIQUE_MARK_SWEEP_GC)&&!defined(USE_UNIQUE_MOVE_COMPACT_GC)
  gc_gen_collection_verbose_info((GC_Gen*)gc, time_collection, time_mutator);
  gc_gen_space_verbose_info((GC_Gen*)gc);
#endif

  gc_reset_after_collection(gc);

  gc_assign_free_area_to_mutators(gc);
  
  vm_resume_threads_after();
  assert(hythread_is_suspend_enabled());
  hythread_set_suspend_disable(disable_count);
  INFO2("gc.process", "GC: GC end\n");
  return;
}




