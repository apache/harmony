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
#include "gc_common.h"
#include "gc_metadata.h"
#include "../thread/mutator.h"
#include "../thread/conclctor.h"
#include "../thread/collector.h"
#include "../finalizer_weakref/finalizer_weakref.h"
#include "../gen/gen.h"
#include "../mark_sweep/gc_ms.h"
#include "../mark_sweep/wspace_mark_sweep.h"
#include "interior_pointer.h"
#include "collection_scheduler.h"
#include "gc_concurrent.h"
#include "../common/gc_for_barrier.h"
#include "concurrent_collection_scheduler.h"
#include "../verify/verify_live_heap.h"

struct Con_Collection_Statistics;

volatile Boolean gc_sweep_global_normal_chunk = FALSE;

//just debugging
inline void gc_ms_get_current_heap_usage(GC_MS *gc)
{
  Con_Collection_Statistics *con_collection_stat = gc_ms_get_con_collection_stat(gc);
  unsigned int new_obj_size = gc_get_mutator_new_obj_size((GC *)gc);
  unsigned int current_size = con_collection_stat->surviving_size_at_gc_end + new_obj_size;
  INFO2("gc.con.scheduler", "[Heap Usage]surviving_size("<<con_collection_stat->surviving_size_at_gc_end<<")+new_obj_size("<<new_obj_size << ")="<<current_size<<" bytes");
  INFO2("gc.con.scheduler", "[Heap Usage]usage rate ("<< (float)current_size/gc->committed_heap_size<<")");
}

void gc_con_update_stat_before_enable_alloc_live(GC *gc) 
{
  Con_Collection_Statistics *con_collection_stat = gc_ms_get_con_collection_stat((GC_MS *)gc);
  con_collection_stat->alloc_size_before_alloc_live = gc_get_mutator_new_obj_size(gc);
}
  
volatile Boolean obj_alloced_live;

void gc_enable_alloc_obj_live(GC *gc)
{
  gc_con_update_stat_before_enable_alloc_live(gc);
  obj_alloced_live = TRUE;
}

void gc_mostly_con_update_stat_after_final_marking(GC *gc)
{
  POINTER_SIZE_INT num_live_obj = 0;
  POINTER_SIZE_INT size_live_obj = 0;  
  POINTER_SIZE_INT num_dirty_obj_traced = 0;

  unsigned int num_conclctors = gc->num_conclctors;
  for( unsigned int i=0; i<num_conclctors; i++ ) {
    Conclctor* conclctor = gc->conclctors[i];
    if( conclctor->role != CONCLCTOR_ROLE_MARKER )
      continue;
    num_live_obj += conclctor->live_obj_num;
    size_live_obj += conclctor->live_obj_size;
    num_dirty_obj_traced += conclctor->num_dirty_slots_traced;
    conclctor->live_obj_num = 0;
    conclctor->live_obj_size = 0;
    conclctor->num_dirty_slots_traced = 0;
  }

  Con_Collection_Statistics * con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
  con_collection_stat->live_size_marked += size_live_obj;
  INFO2("gc.con.scheduler", "[Final Mark Finish] live_marked_size:      "<<con_collection_stat->live_size_marked<<" bytes");
  
}

unsigned int gc_get_conclcor_num(GC* gc, unsigned int req_role);
//called by the marker when it finishes
void gc_con_update_stat_after_marking(GC *gc)
{
  POINTER_SIZE_INT num_live_obj = 0;
  POINTER_SIZE_INT size_live_obj = 0;  
  POINTER_SIZE_INT num_dirty_obj_traced = 0;

  unsigned int num_conclctors = gc->num_conclctors;
  for( unsigned int i=0; i<num_conclctors; i++ ) {
    Conclctor* conclctor = gc->conclctors[i];
    if( conclctor->role != CONCLCTOR_ROLE_MARKER )
      continue;
    num_live_obj += conclctor->live_obj_num;
    size_live_obj += conclctor->live_obj_size;
    num_dirty_obj_traced += conclctor->num_dirty_slots_traced;
    conclctor->live_obj_num = 0;
    conclctor->live_obj_size = 0;
    conclctor->num_dirty_slots_traced = 0;
  }

  unsigned int write_barrier_marked_size = gc_get_mutator_write_barrier_marked_size(gc);
  Con_Collection_Statistics * con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
  con_collection_stat->live_size_marked = size_live_obj + write_barrier_marked_size;
  //INFO2("gc.con.scheduler", "[Mark Finish] live_marked_size:      "<<con_collection_stat->live_size_marked<<" bytes");
  
   /*statistics information update (marking_end_time, trace_rate) */
  con_collection_stat->marking_end_time = time_now();
  int64 marking_time = (unsigned int)(con_collection_stat->marking_end_time - con_collection_stat->marking_start_time);

  unsigned int heap_size = 
  	con_collection_stat->surviving_size_at_gc_end + 
  	gc_get_mutator_new_obj_size(gc);
  	
  con_collection_stat->trace_rate = heap_size/trans_time_unit(marking_time);
   


  /*
  //statistics just for debugging
  unsigned int marker_num = gc_get_conclcor_num(gc, CONCLCTOR_ROLE_MARKER);
  float heap_used_rate = (float)heap_size/gc->committed_heap_size;
  unsigned int new_obj_size_marking = gc_get_mutator_new_obj_size(gc) - con_collection_stat->alloc_size_before_alloc_live;
  unsigned int alloc_rate_marking = new_obj_size_marking/trans_time_unit(con_collection_stat->marking_end_time - con_collection_stat->marking_start_time);
  INFO2("gc.con.scheduler", "[Mark Finish] tracing time=" <<marking_time<<" us, trace rate=" << con_collection_stat->trace_rate<<"b/ms, current heap used="<<heap_used_rate );
  INFO2("gc.con.scheduler", "[Mark Finish] marker num="<<marker_num << ", alloc factor=" << (float)alloc_rate_marking/con_collection_stat->alloc_rate);
  */
}

void gc_PSTW_update_stat_after_marking(GC *gc)
{
  unsigned int size_live_obj = gc_ms_get_live_object_size((GC_MS*)gc);
  Con_Collection_Statistics * con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
  con_collection_stat->live_size_marked = size_live_obj;
  con_collection_stat->alloc_size_before_alloc_live = gc_get_mutator_new_obj_size(gc);
  	
  INFO2("gc.con.scheduler", "[Mark Finish] live_marked:      "<<con_collection_stat->live_size_marked<<" bytes");
  INFO2("gc.con.scheduler", "[Mark Finish] alloc_rate:      "<<con_collection_stat->alloc_rate<<" b/ms");
  INFO2("gc.con.scheduler", "[Mark Finish] trace_rate:      "<<con_collection_stat->trace_rate<<" b/ms");
}

//Called only when heap is exhuaset
void gc_con_update_stat_heap_exhausted(GC* gc)
{
  unsigned int new_obj_size = gc_get_mutator_new_obj_size(gc);
  Con_Collection_Statistics * con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
  con_collection_stat->heap_utilization_rate = (float)(con_collection_stat->surviving_size_at_gc_end + new_obj_size)/gc->committed_heap_size;
  //INFO2("gc.con.scheduler", "[Heap exhausted] surviving size="<<con_collection_stat->surviving_size_at_gc_end<<" bytes, new_obj_size="<<new_obj_size<<" bytes");
  //INFO2("gc.con.scheduler", "[Heap exhausted] current utilization rate="<<con_collection_stat->heap_utilization_rate);
}


//just debugging
unsigned int gc_con_get_live_size_from_sweeper(GC *gc)
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
    collector->live_obj_num = 0;
    collector->live_obj_size = 0;
  }
  
  return size_live_obj;
}

//Called when Con GC ends, must called in a STW period
void gc_reset_con_space_stat(GC *gc)
{
  Con_Collection_Statistics * con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
  unsigned int new_obj_size = gc_reset_mutator_new_obj_size((GC *)gc);

  if( gc_is_kind(ALGO_CON_MOSTLY) ) {
    con_collection_stat->live_alloc_size = 0; //mostly concurrent do not make new alloc obj live
  } else if ( gc_is_kind( ALGO_CON_OTF_OBJ ) || gc_is_kind( ALGO_CON_OTF_REF ) ) {
    con_collection_stat->live_alloc_size = new_obj_size - con_collection_stat->alloc_size_before_alloc_live;
  }
  
  /*live obj size at the end of gc = the size of objs belong to {marked_live + alloc_at_marking+alloc_at_sweeping},
  (for mostly concurrent, con_collection_stat->surviving_size_at_gc_end = con_collection_stat->live_size_marked .)*/
  con_collection_stat->surviving_size_at_gc_end = con_collection_stat->live_size_marked + con_collection_stat->live_alloc_size;
  //INFO2( "gc.con.scheduler", "[Mark Live] live_size_marked = " << con_collection_stat->live_size_marked << ", live_alloc_size=" << con_collection_stat->live_alloc_size );

  
  /*
  //just debugging
  if( !gc_is_specify_con_sweep() ) {
    unsigned int surviving_sweeper = gc_con_get_live_size_from_sweeper(gc);
    unsigned int surviving_marker = con_collection_stat->surviving_size_at_gc_end;
    INFO2("gc.con.scheduler", "[Surviving size] by sweeper: " << surviving_sweeper << " bytes, by marker:" << surviving_marker << " bytes, diff=" << (surviving_sweeper - surviving_marker) );
  }*/

  int64 current_time = time_now();
  
  if(gc->cause != GC_CAUSE_RUNTIME_FORCE_GC ) {
  	unsigned int gc_interval_time = 0;
       if( con_collection_stat->pause_start_time != 0 ) //remove the stw time
            gc_interval_time = trans_time_unit(con_collection_stat->pause_start_time - con_collection_stat->gc_end_time);
       else
            gc_interval_time = trans_time_unit(current_time -con_collection_stat->gc_end_time );
       con_collection_stat->alloc_rate = new_obj_size/gc_interval_time;
       gc_update_scheduler_parameter(gc);
  } else {
     gc_force_update_scheduler_parameter(gc);
  }
  
  con_collection_stat->gc_end_time = current_time;
  
  con_collection_stat->live_size_marked = 0;
  con_collection_stat->live_alloc_size = 0;
  con_collection_stat->alloc_size_before_alloc_live = 0;
  con_collection_stat->marking_start_time = 0;
  con_collection_stat->marking_end_time = 0;
  con_collection_stat->sweeping_time = gc_get_conclctor_time((GC *)gc, CONCLCTOR_ROLE_SWEEPER); //be 0 if not CMCS
  con_collection_stat->pause_start_time = 0;
  assert(con_collection_stat->heap_utilization_rate<1);
  
}

void gc_con_stat_information_out(GC *gc)
{
  Con_Collection_Statistics * con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
  INFO2("gc.con.scheduler","=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=");
  INFO2("gc.con.scheduler", "[Reset] surviving_at_end:       "<<con_collection_stat->surviving_size_at_gc_end<<" bytes");
  INFO2("gc.con.scheduler", "[Reset] alloc_rate:      "<<con_collection_stat->alloc_rate<<" b/ms");
  INFO2("gc.con.scheduler", "[Reset] utilization_rate:      "<<con_collection_stat->heap_utilization_rate);
  INFO2("gc.con.scheduler", "[Reset] trace_rate:      "<<con_collection_stat->trace_rate<<" b/ms");
  INFO2("gc.con.scheduler", "[Reset] sweeping time:      "<<con_collection_stat->sweeping_time<<" us");
  INFO2("gc.con.scheduler", "[Reset] gc time:      "<< trans_time_unit(con_collection_stat->gc_end_time - con_collection_stat->gc_start_time) );
  INFO2("gc.con.scheduler","=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=");
}

void gc_reset_after_con_collection(GC* gc)
{
  assert(gc_is_specify_con_gc());
  int64 reset_start = time_now();
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
  reset_start = time_now();
  gc_reset_con_space_stat(gc);
  gc_clear_conclctor_role(gc);  
  vm_reclaim_native_objs();
}



void gc_set_default_con_algo()
{
  assert((GC_PROP & ALGO_CON_MASK) == 0);
  GC_PROP |= ALGO_CON_OTF_OBJ;
}

void gc_decide_con_algo(char* concurrent_algo)
{
  string_to_upper(concurrent_algo);
  GC_PROP &= ~ALGO_CON_MASK;
  if(!strcmp(concurrent_algo, "OTF_OBJ")){ 
    GC_PROP |= ALGO_CON_OTF_OBJ;
  }else if(!strcmp(concurrent_algo, "MOSTLY_CON")){
    GC_PROP |= ALGO_CON_MOSTLY;
  }else if(!strcmp(concurrent_algo, "OTF_SLOT")){
    GC_PROP |= ALGO_CON_OTF_REF;
  }
}


/* 
    gc start enumeration phase, now, it is in a stop-the-world manner
*/
void gc_start_con_enumeration(GC * gc)
{
  gc_set_rootset_type(ROOTSET_IS_OBJ);  
  gc_prepare_rootset(gc);
}

//unsigned int gc_decide_marker_number(GC* gc);
unsigned int gc_get_marker_number(GC* gc);
/*  gc start marking phase */
void gc_start_con_marking(GC *gc) 
{
  unsigned int num_marker;
  num_marker = gc_get_marker_number(gc);

  if(gc_is_kind(ALGO_CON_OTF_OBJ)) {
    gc_enable_alloc_obj_live(gc);
    gc_set_barrier_function(WB_REM_OBJ_SNAPSHOT);
    gc_ms_start_con_mark((GC_MS*)gc, num_marker);
  } else if(gc_is_kind(ALGO_CON_MOSTLY)) {
    gc_set_barrier_function(WB_REM_SOURCE_OBJ);
    gc_ms_start_mostly_con_mark((GC_MS*)gc, num_marker);
  } else if(gc_is_kind(ALGO_CON_OTF_REF)) {
    gc_enable_alloc_obj_live(gc);
    gc_set_barrier_function(WB_REM_OLD_VAR);
    gc_ms_start_con_mark((GC_MS*)gc, num_marker);
  }
}


/* 
    gc start sweeping phase
*/
void gc_prepare_sweeping(GC *gc) {
  INFO2("gc.con.info", "Concurrent collection, current collection = " << gc->num_collections );
  /*FIXME: enable finref*/
  if(!IGNORE_FINREF ){ 
    gc_set_obj_with_fin(gc);
    Collector* collector = gc->collectors[0];
    collector_identify_finref(collector);
  #ifndef BUILD_IN_REFERENT
  } else {
    conclctor_set_weakref_sets(gc);
    gc_update_weakref_ignore_finref(gc);
  #endif
  }
  gc_identify_dead_weak_roots(gc);
}

int64 get_last_check_point();
// for the case pure stop the world
static void gc_partial_con_PSTW( GC *gc) {
  int64 time_collection_start = time_now();
  INFO2("gc.space.stat","Stop-the-world collection = "<<gc->num_collections<<"");
  INFO2("gc.con.info", "from last check point =" << (unsigned int)(time_collection_start -get_last_check_point()) );
  // stop the world enumeration
  gc->num_collections++;
  int disable_count = hythread_reset_suspend_disable();  
  gc_set_rootset_type(ROOTSET_IS_REF);
  gc_prepare_rootset(gc);
   
  if(gc->cause != GC_CAUSE_RUNTIME_FORCE_GC ) {
      unsigned int new_obj_size = gc_get_mutator_new_obj_size(gc);
      Con_Collection_Statistics * con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
      con_collection_stat->heap_utilization_rate = (float)(con_collection_stat->surviving_size_at_gc_end + new_obj_size)/gc->committed_heap_size;
  }
  
  //reclaim heap 
  gc_reset_mutator_context(gc);
  if(!IGNORE_FINREF ) gc_set_obj_with_fin(gc);
  gc_ms_reclaim_heap((GC_MS*)gc);

  //update live size
  gc_PSTW_update_stat_after_marking(gc);
  	
  // reset the collection and resume mutators
  gc_reset_after_con_collection(gc);

  set_con_nil(gc); // concurrent scheduling will continue after mutators are resumed
  vm_resume_threads_after();
  assert(hythread_is_suspend_enabled());
  hythread_set_suspend_disable(disable_count);
}

void terminate_mostly_con_mark();
void wspace_mostly_con_final_mark( GC *gc );

// for the case concurrent marking is not finished before heap is exhausted
static void gc_partial_con_PMSS(GC *gc) {
  INFO2("gc.con.info", "[PMSS] Heap has been exhuasted, current collection = " << gc->num_collections );
  // wait concurrent marking finishes
  int64 wait_start = time_now();
  gc_disable_alloc_obj_live(gc); // in the STW manner, so we can disable it at anytime before the mutators are resumed
  //in the stop the world phase (only conclctors is running at the moment), so the spin lock will not lose more performance
  while( gc->gc_concurrent_status == GC_CON_START_MARKERS || 
  	      gc->gc_concurrent_status == GC_CON_TRACING || 
  	      gc->gc_concurrent_status == GC_CON_TRACE_DONE) 
  {
      vm_thread_yield(); //let the unfinished marker run
  }

  /*just debugging*/
    gc_ms_get_current_heap_usage((GC_MS *)gc);
    int64 pause_time = time_now() - wait_start;
    INFO2("gc.con.info", "[PMSS]wait marking time="<<pause_time<<" us" );
    Con_Collection_Statistics *con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
    unsigned int marking_time_shortage = (unsigned int)(con_collection_stat->marking_end_time - wait_start);
    INFO2("gc.con.info", "[PMSS] marking late time [" << marking_time_shortage << "] us" );
	    
  // start STW reclaiming heap
  gc_con_update_stat_heap_exhausted(gc); // calculate util rate
  gc_reset_mutator_context(gc);
  if(!IGNORE_FINREF ) gc_set_obj_with_fin(gc);
  gc_ms_reclaim_heap((GC_MS*)gc);
  
  // reset after partial stop the world collection
  gc_reset_after_con_collection(gc);
  set_con_nil(gc);
}

// only when current sweep is set to false
static void gc_partial_con_CMSS(GC *gc) {
  
  INFO2("gc.con.info", "[CMSS] Heap has been exhuasted, current collection = " << gc->num_collections );
  gc_disable_alloc_obj_live(gc); // in the STW manner, so we can disable it at anytime before the mutators are resumed

  /*just debugging*/
    gc_ms_get_current_heap_usage((GC_MS *)gc);
    Con_Collection_Statistics *con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
    unsigned int from_marking_end = (unsigned int)(time_now() - con_collection_stat->marking_end_time);
    INFO2("gc.con.info", "[CMSS] marking early time [" << from_marking_end << "] us" );

  gc_con_update_stat_heap_exhausted(gc); // calculate util rate
  
  // start reclaiming heap, it will skip the marking phase
  gc_reset_mutator_context(gc);
  if(!IGNORE_FINREF ) gc_set_obj_with_fin(gc);
  gc_ms_reclaim_heap((GC_MS*)gc);
  
  // reset after partial stop the world collection
  gc_reset_after_con_collection(gc);
  set_con_nil(gc);
}

void gc_merge_free_list_global(GC *gc);
//for the case concurrent marking and partial concurrent sweeping
static void gc_partial_con_CMPS( GC *gc ) {

  while(gc->gc_concurrent_status == GC_CON_SWEEPING || gc->gc_concurrent_status == GC_CON_SWEEP_DONE) {
      vm_thread_yield();  //let the unfinished sweeper run
  }
  gc_merge_free_list_global(gc);
  // reset after partial stop the world collection
  gc_reset_after_con_collection(gc);
  set_con_nil(gc);
}


inline static void partial_stop_the_world_info( unsigned int type, unsigned int pause_time ) {
  switch( type ) {
    case GC_PARTIAL_PSTW :
      INFO2("gc.con.time","[PT] pause ( Heap exhuasted ), PSTW=" << pause_time << " us");
      break;
    case GC_PARTIAL_PMSS :
      INFO2("gc.con.time","[PT] pause ( Heap exhuasted ), PMSS=" << pause_time << " us");
      break;
    case GC_PARTIAL_CMPS :
      INFO2("gc.con.time","[PT] pause ( Heap exhuasted ), CMPS=" << pause_time << " us");
      break;
    case GC_PARTIAL_CMSS :
      INFO2("gc.con.time","[PT] pause ( Heap exhuasted ), CMSS=" << pause_time << " us");
      break;
    case GC_PARTIAL_FCSR :
      INFO2("gc.con.time","[PT] pause ( Heap exhuasted ), FCSR=" << pause_time << " us");
      break;
  }
}

static unsigned int gc_con_heap_full_mostly_con( GC *gc )
{
   while( gc->gc_concurrent_status == GC_CON_START_MARKERS ) { // we should enumerate rootset after old rootset is traced
      vm_thread_yield();
   }

   int64 final_start = time_now();
   int disable_count = hythread_reset_suspend_disable();
   gc_set_rootset_type(ROOTSET_IS_OBJ);
   gc_prepare_rootset(gc);
  
   gc_set_barrier_function(WB_REM_NIL); //in stw phase, so we can remove write barrier at any time
   terminate_mostly_con_mark(); // terminate current mostly concurrent marking

   //in the stop the world phase (only conclctors is running at the moment), so the spin lock will not lose more performance
   while(gc->gc_concurrent_status == GC_CON_TRACING) {
      vm_thread_yield(); //let the unfinished marker run
   }

   //final marking phase
   gc_clear_conclctor_role(gc);
   wspace_mostly_con_final_mark(gc);

   /*just debugging*/
   int64 final_time = time_now() - final_start;
   INFO2("gc.scheduler", "[MOSTLY_CON] final marking time=" << final_time << " us");
   gc_ms_get_current_heap_usage((GC_MS *)gc);
  
  // start STW reclaiming heap
   gc_con_update_stat_heap_exhausted(gc); // calculate util rate
   gc_reset_mutator_context(gc);
   if(!IGNORE_FINREF ) gc_set_obj_with_fin(gc);
   gc_ms_reclaim_heap((GC_MS*)gc);
  
   // reset after partial stop the world collection
   gc_reset_after_con_collection(gc);
   set_con_nil(gc);
  
   vm_resume_threads_after();
   hythread_set_suspend_disable(disable_count);
   return GC_PARTIAL_PMSS;
   
}

static unsigned int gc_con_heap_full_otf( GC *gc )
{
   unsigned int partial_type; //for time measuring and debugging
   int disable_count = vm_suspend_all_threads();
   Con_Collection_Statistics *con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
   con_collection_stat->pause_start_time = time_now();
   switch(gc->gc_concurrent_status) {
   	case GC_CON_START_MARKERS :
       case GC_CON_TRACING :
       case GC_CON_TRACE_DONE :
         partial_type = GC_PARTIAL_PMSS;
         gc_partial_con_PMSS(gc);
	  break;
       case GC_CON_BEFORE_SWEEP : // only when current sweep is set to false
         partial_type = GC_PARTIAL_CMSS; 
         gc_partial_con_CMSS(gc);
         break;
       case GC_CON_SWEEPING :
       case GC_CON_SWEEP_DONE :
         partial_type = GC_PARTIAL_CMPS;
         gc_partial_con_CMPS(gc);
         break;
       case GC_CON_BEFORE_FINISH : //heap can be exhausted when sweeping finishes, very rare
         partial_type = GC_PARTIAL_FCSR;
	  gc_merge_free_list_global(gc);
	  gc_reset_after_con_collection(gc);
	  set_con_nil(gc);
	  break;
	case GC_CON_RESET :
	case GC_CON_NIL :
	case GC_CON_STW_ENUM :
	  /*do nothing, if still in gc_con_reset, will wait to finish after resuming. this case happens rarely*/
	  partial_type = GC_PARTIAL_FCSR; 
	  break;
	/* other state is illegal here */
	default:
         INFO2("gc.con.info", "illegal state when the heap is out [" << gc->gc_concurrent_status << "]");
         RAISE_ERROR;
    }
    vm_resume_all_threads(disable_count);
    return partial_type;
}

void gc_con_stat_information_out(GC *gc);
/* 
this method is called before STW gc start, there is a big lock outside
*/
void gc_wait_con_finish( GC* gc ) {
  int64 time_collection_start = time_now();
  unsigned int partial_type; //for time measuring and debugging
  
   /* cocurrent gc is idle */
   if( state_transformation( gc, GC_CON_NIL, GC_CON_DISABLE ) ) { // for the race condition of con schduling and STW gc
        Con_Collection_Statistics *con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
        con_collection_stat->gc_start_time = time_now();
	 con_collection_stat->pause_start_time = con_collection_stat->gc_start_time;
	 partial_type = GC_PARTIAL_PSTW;
        gc_partial_con_PSTW( gc );
   } else {
      while(gc->gc_concurrent_status == GC_CON_STW_ENUM ) { //wait concurrent gc finish enumeration
	   hythread_safe_point();
	   vm_thread_yield();
       }
       if( gc_is_kind(ALGO_CON_MOSTLY) )
         partial_type = gc_con_heap_full_mostly_con(gc);
	else if( gc_is_kind(ALGO_CON_OTF_OBJ) || gc_is_kind(ALGO_CON_OTF_REF) ) {
	  partial_type = gc_con_heap_full_otf(gc);
	  if(gc->gc_concurrent_status == GC_CON_RESET) {
            while( gc->gc_concurrent_status == GC_CON_RESET ) { //wait concurrent to finish
	       hythread_safe_point();
	       vm_thread_yield();
            }
         }
	}
	else 
	  RAISE_ERROR;
   }
   
  int64 pause_time = time_now()-time_collection_start;
  gc_con_stat_information_out(gc);
  if(GC_CAUSE_RUNTIME_FORCE_GC == gc->cause) {
    INFO2("gc.con.time","[GC][Con]pause(   Forcing GC   ):    "<<(unsigned int)(pause_time)<<"  us ");
  } else {
    partial_stop_the_world_info( partial_type, (unsigned int)pause_time );
  }
}


