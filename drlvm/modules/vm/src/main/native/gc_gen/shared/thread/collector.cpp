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

#include "open/vm_util.h"

#include "collector.h"
#include "../mark_compact/mspace.h"
#include "../finalizer_weakref/finalizer_weakref.h"
#include "../common/space_tuner.h"
#include "../mark_sweep/wspace.h"

unsigned int MINOR_COLLECTORS = 0;
unsigned int MAJOR_COLLECTORS = 0;
static volatile unsigned int live_collector_num = 0;

void collector_restore_obj_info(Collector* collector)
{
  Pool *remset_pool = collector->gc->metadata->collector_remset_pool;
  Pool *free_pool = collector->gc->metadata->free_set_pool;
  assert(!collector->rem_set);
  
  while(Vector_Block *oi_block = pool_get_entry(remset_pool)){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(oi_block);
    while(!vector_block_iterator_end(oi_block, iter)){
      Partial_Reveal_Object *p_target_obj = (Partial_Reveal_Object *)*iter;
      iter = vector_block_iterator_advance(oi_block, iter);
      Obj_Info_Type obj_info = (Obj_Info_Type)*iter;
      iter = vector_block_iterator_advance(oi_block, iter);
      set_obj_info(p_target_obj, obj_info);
    }
    vector_block_clear(oi_block);
    pool_put_entry(free_pool, oi_block);
  }
}

#ifdef USE_32BITS_HASHCODE
void collector_attach_hashcode(Collector *collector)
{
  Pool* hashcode_pool = collector->gc->metadata->collector_hashcode_pool;
  Pool *free_pool = collector->gc->metadata->free_set_pool;
  assert(!collector->hashcode_set);

  while(Vector_Block* hashcode_block = pool_get_entry(hashcode_pool)){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(hashcode_block);
    while(!vector_block_iterator_end(hashcode_block, iter)){
      POINTER_SIZE_INT* obj_end_pos = (POINTER_SIZE_INT*)*iter;
      iter = vector_block_iterator_advance(hashcode_block, iter);
      POINTER_SIZE_INT hashcode = *iter;
      iter = vector_block_iterator_advance(hashcode_block, iter);
      *obj_end_pos = hashcode;
    }
    vector_block_clear(hashcode_block);
    pool_put_entry(free_pool, hashcode_block);
  }
}
#endif

static void collector_reset_thread(Collector *collector) 
{
  collector->task_func = NULL;

  /*
  vm_reset_event(collector->task_assigned_event);
  vm_reset_event(collector->task_finished_event);
  */
    
  GC_Metadata* metadata = collector->gc->metadata;
  
  if(gc_is_gen_mode() && collect_is_minor()){
    if( NOS_PARTIAL_FORWARD || minor_is_semispace() ){
      assert(collector->rem_set==NULL);
      collector->rem_set = free_set_pool_get_entry(metadata);
    }
  }
  
#ifndef BUILD_IN_REFERENT
  collector_reset_weakref_sets(collector);
#endif

#if !defined(USE_UNIQUE_MARK_SWEEP_GC) && !defined(USE_UNIQUE_MOVE_COMPACT_GC)
  /*For LOS_Shrink and LOS_Extend*/
  if(gc_has_space_tuner(collector->gc) && collector->gc->tuner->kind != TRANS_NOTHING){
    collector->non_los_live_obj_size = 0;
    collector->los_live_obj_size = 0;
  }
#endif

  collector->result = TRUE;
  return;
}

static void wait_collector_to_finish(Collector *collector) 
{
  vm_wait_event(collector->task_finished_event);
}

static void notify_collector_to_work(Collector* collector)
{
  vm_set_event(collector->task_assigned_event);  
}

static void collector_wait_for_task(Collector *collector) 
{
  vm_wait_event(collector->task_assigned_event);
}

static void collector_notify_work_done(Collector *collector) 
{
  vm_set_event(collector->task_finished_event);
}

static void assign_collector_with_task(GC* gc, TaskType task_func, Space* space)
{
  /* FIXME:: to adaptively identify the num_collectors_to_activate */
  if( MINOR_COLLECTORS && collect_is_minor()){
    gc->num_active_collectors = MINOR_COLLECTORS;
  }else if ( MAJOR_COLLECTORS && collect_is_major()){
    gc->num_active_collectors = MAJOR_COLLECTORS;
  }else{
    gc->num_active_collectors = gc->num_collectors;
  }
  
  for(unsigned int i=0; i<gc->num_active_collectors; i++)
  {
    Collector* collector = gc->collectors[i];
    
    collector_reset_thread(collector);
    collector->task_func = task_func;
    collector->collect_space = space;
    collector->collector_is_active = TRUE;
    notify_collector_to_work(collector);    
  }
  return;
}

void wait_collection_finish(GC* gc)
{
  unsigned int num_active_collectors = gc->num_active_collectors;
  for(unsigned int i=0; i<num_active_collectors; i++)
  {
    Collector* collector = gc->collectors[i];
    wait_collector_to_finish(collector);
  }  
  return;
}

static int collector_thread_func(void *arg) 
{
  Collector *collector = (Collector *)arg;
  assert(collector);
  
  while(true){
    /* Waiting for newly assigned task */
    collector_wait_for_task(collector); 
    collector->collector_is_active = TRUE;
    
    /* waken up and check for new task */
    TaskType task_func = collector->task_func;
    if(task_func == NULL){
      atomic_dec32(&live_collector_num);
      return 1;
    }
      
    task_func(collector);

   //conducted after collection to return last TLB in hand 
   #if !defined(USE_UNIQUE_MARK_SWEEP_GC) && !defined(USE_UNIQUE_MOVE_COMPACT_GC)
    gc_reset_collector_alloc(collector->gc, collector);
   #endif
    collector_notify_work_done(collector);
    
    collector->collector_is_active = FALSE;
  }

  return 0;
}

static void collector_init_thread(Collector *collector) 
{
  collector->rem_set = NULL;
  collector->rep_set = NULL;

  int status = vm_create_event(&collector->task_assigned_event);
  assert(status == THREAD_OK);

  status = vm_create_event(&collector->task_finished_event);
  assert(status == THREAD_OK);

  status = (unsigned int)vm_create_thread(collector_thread_func, (void*)collector);

  assert(status == THREAD_OK);
  
  return;
}

static void collector_terminate_thread(Collector* collector)
{
  assert(live_collector_num);
  unsigned int old_live_collector_num = live_collector_num;
  collector->task_func = NULL; /* NULL to notify thread exit */
  notify_collector_to_work(collector);
  while(old_live_collector_num == live_collector_num)
    vm_thread_yield(); /* give collector time to die */
  
  return;
}

#include "../common/gc_common.h"
#ifdef GC_GEN_STATS

#include "../gen/gen_stats.h"

void collector_init_stats(Collector* collector)
{
#if !defined(USE_UNIQUE_MARK_SWEEP_GC) && !defined(USE_UNIQUE_MOVE_COMPACT_GC)
  gc_gen_collector_stats_initialize(collector);
#endif
}

void collector_destruct_stats(Collector* collector)
{
#if !defined(USE_UNIQUE_MARK_SWEEP_GC) && !defined(USE_UNIQUE_MOVE_COMPACT_GC)
  gc_gen_collector_stats_destruct(collector);
#endif
}

#endif

void collector_destruct(GC* gc) 
{
  TRACE2("gc.process", "GC: GC collectors destruct ...");
  for(unsigned int i=0; i<gc->num_collectors; i++)
  {
    Collector* collector = gc->collectors[i];
    collector_terminate_thread(collector);
#ifdef GC_GEN_STATS
    collector_destruct_stats(collector);
#endif
    gc_destruct_collector_alloc(gc, collector);   
  }
  assert(live_collector_num == 0);
  
  STD_FREE(gc->collectors);
  return;
}

unsigned int NUM_COLLECTORS = 0;

void collector_initialize(GC* gc)
{
  TRACE2("gc.process", "GC: GC collectors init ... \n");

  //FIXME::
  unsigned int num_processors = gc_get_processor_num(gc);
  
  unsigned int nthreads = max( max( MAJOR_COLLECTORS, MINOR_COLLECTORS), max(NUM_COLLECTORS, num_processors)); 

  unsigned int size = sizeof(Collector *) * nthreads;
  gc->collectors = (Collector **) STD_MALLOC(size); 
  memset(gc->collectors, 0, size);

  size = sizeof(Collector);
  for (unsigned int i = 0; i < nthreads; i++) {
    Collector* collector = (Collector *)STD_MALLOC(size);
    memset(collector, 0, size);
    
    /* FIXME:: thread_handle is for temporary control */
    collector->thread_handle = (VmThreadHandle)(POINTER_SIZE_INT)i;
    collector->gc = gc;
    //init collector allocator (mainly for semi-space which has two target spaces)
    gc_init_collector_alloc(gc, collector);
    //init thread scheduling related stuff, creating collector thread
    collector_init_thread(collector); 

#ifdef GC_GEN_STATS
    collector_init_stats(collector);
#endif

    gc->collectors[i] = collector;
  }

  gc->num_collectors = NUM_COLLECTORS? NUM_COLLECTORS:num_processors;
  live_collector_num = gc->num_collectors;

  return;
}

void collector_execute_task(GC* gc, TaskType task_func, Space* space)
{
  assign_collector_with_task(gc, task_func, space);
  wait_collection_finish(gc);
    
  return;
}

/* FIXME:: unimplemented. the design intention for this API is to lauch specified num of collectors. There might be already
   some collectors running. The specified num would be additional num. */
void collector_execute_task_concurrent(GC* gc, TaskType task_func, Space* space, unsigned int num_collectors)
{
  assign_collector_with_task(gc, task_func, space);

  return;
}

void collector_release_weakref_sets(GC* gc, unsigned int num_collectors)
{
  Finref_Metadata *metadata = gc->finref_metadata;
  unsigned int num_active_collectors = gc->num_active_collectors;
  unsigned int i = 0;
  for(; i<num_active_collectors; i++){
    Collector* collector = gc->collectors[i];
    pool_put_entry(metadata->free_pool, collector->softref_set);
    pool_put_entry(metadata->free_pool, collector->weakref_set);
    pool_put_entry(metadata->free_pool, collector->phanref_set);
    collector->softref_set = NULL;
    collector->weakref_set = NULL;
    collector->phanref_set = NULL;
  }
}

Boolean is_collector_finished(GC* gc)
{
  unsigned int num_active_collectors = gc->num_active_collectors;
  unsigned int i = 0;
  for(; i<num_active_collectors; i++){
    Collector* collector = gc->collectors[i];
    if(collector->collector_is_active){
      return FALSE;
    }
  }
  return TRUE;

}

int64 gc_get_collector_time(GC* gc)
{
  int64 time_collector = 0;
  unsigned int num_active_collectors = gc->num_active_collectors;
  unsigned int i = 0;
  for(; i<num_active_collectors; i++){
    Collector* collector = gc->collectors[i];
    int64 time_measured = collector->time_measurement_end - collector->time_measurement_start;
    if(time_measured > time_collector)
      time_collector = time_measured;
  }
  return time_collector;
}




