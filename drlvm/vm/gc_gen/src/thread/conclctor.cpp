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

#include "conclctor.h"
#include "../finalizer_weakref/finalizer_weakref.h"
#include "../gen/gen.h"
#include "../mark_sweep/gc_ms.h"

TaskType marker_final_func;
TaskType sweeper_final_func;

SpinLock print_lock;

static volatile unsigned int live_conclctor_num = 0;

static inline void notify_conclctor_to_work(Conclctor* conclctor)
{
  vm_set_event(conclctor->task_assigned_event);
}

static inline void conclctor_wait_for_task(Conclctor* conclctor)
{
  vm_wait_event(conclctor->task_assigned_event);
}

static inline void conclctor_reset_thread(Conclctor *conclctor) 
{
  conclctor->task_func = NULL;
#ifndef BUILD_IN_REFERENT
  if(conclctor->role == CONCLCTOR_ROLE_MARKER) //only marker use weakref sets
    conclctor_reset_weakref_sets(conclctor);
#endif
  return;
}



static inline void conclctor_finish(Conclctor *conclctor) 
{
  GC *gc = conclctor->gc;
  switch( conclctor->role ) {
    case CONCLCTOR_ROLE_MARKER:
      if(apr_atomic_dec32(&gc->num_active_markers) == 0 ) {
       if(marker_final_func!=NULL)
	 marker_final_func( conclctor );
      }
      break;
    case CONCLCTOR_ROLE_SWEEPER:
      if(apr_atomic_dec32(&gc->num_active_sweepers) == 0) {
        if(sweeper_final_func!=NULL)
        sweeper_final_func( conclctor );
      }
      break;
    default:
      assert(0);
  }
}

static inline int round_conclctor_num(GC* gc, unsigned int req_num) 
{
  unsigned int free_num = gc->num_conclctors - gc->num_active_markers - gc->num_active_sweepers;
  assert(free_num>=0);
  if( free_num > req_num )
    return req_num;
  return free_num;
}

/*just for debugging*/
inline static void assign_event_info( unsigned int role, unsigned int index ) {
  switch( role ) {
  case CONCLCTOR_ROLE_MARKER:
    INFO2("gc.con.info", "Activate a MARKER at index ["<<index<<"]");
    break;
  case CONCLCTOR_ROLE_SWEEPER:
    INFO2("gc.con.info", "Activate a SWEEPER at index ["<<index<<"]");
    break;
  default:
      assert(0);
  }
}

static void assign_conclctor_with_task(GC* gc, TaskType task_func, Space* space, unsigned int num_conclctors, unsigned int role )
{
   
   unsigned int num_assign = round_conclctor_num(gc, num_conclctors);
   if( num_assign < num_conclctors ) {
     INFO2( "gc.con.info", "<Oops> There is no free conclctors" );
     assert(0);
     return;
   }
   //INFO2("gc.con.info", "request number = " << num_conclctors << ", actual num = " << num_assign );
   switch( role ) {
    case CONCLCTOR_ROLE_MARKER:
      apr_atomic_add32(&gc->num_active_markers, num_assign);
      break;
    case CONCLCTOR_ROLE_SWEEPER:
      apr_atomic_add32(&gc->num_active_sweepers, num_assign);
      break;
    default:
      assert(0);
  }
   //INFO2("gc.con.info", "active markers=" <<gc->num_active_markers);
   unsigned int j = 0;
   for(unsigned int i=0; i<gc->num_conclctors; i++)
   {
     Conclctor* conclctor = gc->conclctors[i];
     if( conclctor->status != CONCLCTOR_NIL )
      continue;
     conclctor_reset_thread(conclctor);
     conclctor->task_func = task_func;
     conclctor->con_space = space;
     conclctor->role = role;
     conclctor->status = CONCLCTOR_ACTIVE;
     //assign_event_info( role, i );
     notify_conclctor_to_work(conclctor);
     if( ++j >= num_assign) break;
   }
  return;
}



static int conclctor_thread_func(void *arg) 
{
  Conclctor *conclctor = (Conclctor *)arg;
  assert(conclctor);
  
  while(true){
    /* Waiting for newly assigned task */
    conclctor_wait_for_task(conclctor); 
    //conclctor->status = CONCLCTOR_ACTIVE;
    /* waken up and check for new task */
    TaskType task_func = conclctor->task_func;
    if(task_func == NULL) {
      atomic_dec32(&live_conclctor_num);
    conclctor->status = CONCLCTOR_DEAD;
    //INFO2( "gc.con.info", "CONCLCTOR DEAD");
      return 1;
    }
    conclctor->time_measurement_start = time_now();
    task_func(conclctor);
    
  
  /*
  if( conclctor->role == CONCLCTOR_ROLE_MARKER ) {
    int64 marking_time = conclctor->time_measurement_end - conclctor->time_measurement_start;
    double marking_rate = conclctor->num_dirty_slots_traced;
    if( marking_time != 0 )
      marking_rate = (double)conclctor->num_dirty_slots_traced/(marking_time>>10);
    lock( print_lock );
    INFO2( "gc.con.info", "[MR] Marking Time=" << (unsigned int)marking_time << ", Dirty Slots Traced=" << conclctor->num_dirty_slots_traced << ", Trace Rate=" << marking_rate << "/ms" );
    unlock( print_lock );
  }*/
  
    conclctor_finish(conclctor);
    conclctor->time_measurement_end = time_now();
    conclctor->status = CONCLCTOR_NIL;  
  }

  return 0;
}


static void conclctor_init_thread(Conclctor *conclctor) 
{
  conclctor->rem_set = NULL;
  conclctor->rep_set = NULL;

  int status = vm_create_event(&conclctor->task_assigned_event);
  assert(status == THREAD_OK);
  /* for concurrent collector, we do not need finished event */
  status = (unsigned int)vm_create_thread(conclctor_thread_func, (void*)conclctor);
  assert(status == THREAD_OK);
  
  return;
}



static void conclctor_terminate_thread(Conclctor* conclctor)
{
  assert(live_conclctor_num);
  unsigned int old_live_conclctor_num = live_conclctor_num;
  while (conclctor->status == CONCLCTOR_ACTIVE) {  //wait conclctor to finish
     vm_thread_yield();
  }
  conclctor->task_func = NULL; /* NULL to notify thread exit */
  notify_conclctor_to_work(conclctor);
  while(conclctor->status != CONCLCTOR_DEAD)
    vm_thread_yield(); /* give conclctor time to die */
  return;
}

 void terminate_mostly_con_mark();

void conclctor_destruct(GC* gc) 
{
  TRACE2("gc.process", "GC: GC conclctors destruct ...");

  set_marker_final_func(NULL);
  set_sweeper_final_func(NULL);

  terminate_mostly_con_mark(); // mostly concurrent marker may be still running and never stops because heap will not be exhuasted

  for(unsigned int i=0; i<gc->num_conclctors; i++)
  {
    Conclctor* conclctor = gc->conclctors[i];
    conclctor_terminate_thread(conclctor);
    STD_FREE(conclctor);
  }
  assert(live_conclctor_num == 0);
  STD_FREE(gc->conclctors);
  return;
}

void conclctor_init_free_chunk_list(Conclctor *conclctor)
{
  Free_Chunk_List *list = (Free_Chunk_List*)STD_MALLOC(sizeof(Free_Chunk_List));
  free_chunk_list_init(list);
  conclctor->free_chunk_list = list;
}

unsigned int NUM_CONCLCTORS = 0;
unsigned int NUM_CON_MARKERS = 0;
unsigned int NUM_CON_SWEEPERS = 0;

void conclctor_initialize(GC* gc)
{
  TRACE2("gc.process", "GC: GC conclctors init ... \n");
  //FIXME::
  unsigned int num_processors = gc_get_processor_num(gc);
  
  unsigned int nthreads = max(NUM_CONCLCTORS, num_processors);

  unsigned int size = sizeof(Conclctor *) * nthreads;
  gc->conclctors = (Conclctor **) STD_MALLOC(size); 
  memset(gc->conclctors, 0, size);

  size = sizeof(Conclctor);
  for (unsigned int i = 0; i < nthreads; i++) {
    Conclctor* conclctor = (Conclctor *)STD_MALLOC(size);
    memset(conclctor, 0, size);
    /* FIXME:: thread_handle is for temporary control */
    conclctor->thread_handle = (VmThreadHandle)(POINTER_SIZE_INT)i;
    conclctor->gc = gc;
    conclctor->status = CONCLCTOR_NIL;
    conclctor->role = CONCLCTOR_ROLE_NIL;
    conclctor_init_free_chunk_list(conclctor);
    //init thread scheduling related stuff, creating conclctor thread
    conclctor_init_thread(conclctor);
    //#ifdef GC_GEN_STATS
    //collector_init_stats((Collector *)conclctor);
    //#endif
    gc->conclctors[i] = conclctor;
  }
  gc->num_conclctors = NUM_CONCLCTORS? NUM_CONCLCTORS:num_processors;
  live_conclctor_num = gc->num_conclctors;
  return;
}


void conclctor_execute_task_concurrent(GC* gc, TaskType task_func, Space* space, unsigned int num_conclctors, unsigned int role)
{
  assign_conclctor_with_task(gc, task_func, space, num_conclctors, role);
  return;
}

/* this method is an exception for concurrent gc, it will wait the task finished and then return,
basiclly, it is for mostly concurrent's final marking phase. of course, it can be used for other propurse.
in most cases, this method is used in a short STW phase, so the spin lock here will not effect the whole performance badly
*/
void conclctor_execute_task_synchronized(GC* gc, TaskType task_func, Space* space, unsigned int num_conclctors, unsigned int role)
{
  assign_conclctor_with_task(gc, task_func, space, num_conclctors, role);
  switch( role ) {
    case CONCLCTOR_ROLE_MARKER:
      while( gc->num_active_markers != 0 ) {
        vm_thread_yield();
      }
      break;
    case CONCLCTOR_ROLE_SWEEPER: /* now, this case will never be reached*/
      while( gc->num_active_sweepers != 0 ) {
        vm_thread_yield();
      }
      break;
    default:
      assert(0);
  }
  
}

unsigned int gc_get_conclcor_num(GC* gc, unsigned int req_role) {
  assert( req_role != CONCLCTOR_ROLE_NIL );
  unsigned int i = 0;
  unsigned int num = 0;
  for(; i<gc->num_conclctors; i++){
    Conclctor* conclctor = gc->conclctors[i];
    if( conclctor->role != req_role )
      continue;
    num++;
  }
  return num;
}

int64 gc_get_conclctor_time(GC* gc, unsigned int req_role)
{
  assert( req_role != CONCLCTOR_ROLE_NIL );
  int64 time_conclctor = 0;
  unsigned int i = 0;
  for(; i<gc->num_conclctors; i++){
    Conclctor* conclctor = gc->conclctors[i];
    if( conclctor->role != req_role )
      continue;
    int64 time_measured = conclctor->time_measurement_end - conclctor->time_measurement_start;
    if(time_measured > time_conclctor)
      time_conclctor = time_measured;
  }
  return time_conclctor;
}

void gc_clear_conclctor_role(GC *gc) {
  unsigned int i = 0;
  for(; i<gc->num_conclctors; i++){
    Conclctor* conclctor = gc->conclctors[i];
    conclctor->live_obj_num = 0;
    conclctor->live_obj_size = 0;
    conclctor->time_measurement_start = 0;
    conclctor->time_measurement_end = 0;
    conclctor->role = CONCLCTOR_ROLE_NIL;
  }
  gc->num_active_sweepers = 0;
  gc->num_active_markers = 0;
}

void conclctor_set_weakref_sets(GC* gc)// now only marker uses this
{
  unsigned int req_role = CONCLCTOR_ROLE_MARKER;
  Finref_Metadata *metadata = gc->finref_metadata;
  unsigned int num_conclctors = gc->num_conclctors;
  unsigned int i = 0;
  for(; i<num_conclctors; i++){
    Conclctor* conclctor = gc->conclctors[i];
  if( conclctor->role != req_role )
    continue;
    //check_ref_pool(conclctor);
  /* for mostly concurrent, some conclctors's weak sets have already been reclaimed, so the NOT NULL check is need here */
  if( conclctor->softref_set != NULL ) {
    pool_put_entry(metadata->softref_pool, conclctor->softref_set);
    conclctor->softref_set = NULL;
  }

  if( conclctor->weakref_set != NULL ) {
    pool_put_entry(metadata->weakref_pool, conclctor->weakref_set);
    conclctor->weakref_set = NULL;
  }

  if( conclctor->phanref_set != NULL ) {
    pool_put_entry(metadata->phanref_pool, conclctor->phanref_set);
    conclctor->phanref_set = NULL;
  }
    
  }
}


void conclctor_release_weakref_sets(GC* gc) // now only sweeper use this
{
  unsigned int req_role = CONCLCTOR_ROLE_SWEEPER;
  Finref_Metadata *metadata = gc->finref_metadata;
  unsigned int num_conclctors = gc->num_conclctors;
  unsigned int i = 0;
  for(; i<num_conclctors; i++){
    Conclctor* conclctor = gc->conclctors[i];
  if( conclctor->role != req_role )
    continue;

    pool_put_entry(metadata->free_pool, conclctor->softref_set);
    pool_put_entry(metadata->free_pool, conclctor->weakref_set);
    pool_put_entry(metadata->free_pool, conclctor->phanref_set);
    conclctor->softref_set = NULL;
    conclctor->weakref_set = NULL;
    conclctor->phanref_set = NULL;
  }
}


/* reset weak references vetctor block of each conclctor */
void conclctor_reset_weakref_sets(Conclctor *conclctor)
{
  GC *gc = conclctor->gc;
  assert(conclctor->softref_set == NULL);
  assert(conclctor->weakref_set == NULL);
  assert(conclctor->phanref_set == NULL);
  conclctor->softref_set = finref_get_free_block(gc);
  conclctor->weakref_set = finref_get_free_block(gc);
  conclctor->phanref_set= finref_get_free_block(gc);
}
