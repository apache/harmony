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
#include "../gen/gen.h"
#include "../mark_sweep/gc_ms.h"
#include "../mark_sweep/wspace.h"
#include "collection_scheduler.h"
#include "concurrent_collection_scheduler.h"
#include "gc_concurrent.h"
#include "../thread/conclctor.h"
#include "../verify/verify_live_heap.h"

#define NUM_TRIAL_COLLECTION 2
#define MIN_DELAY_TIME 0x0
#define MAX_DELAY_TIME 0x7fFfFfFf
#define MAX_TRACING_RATE 100
#define MIN_TRACING_RATE 1
#define MAX_SPACE_THRESHOLD (POINTER_SIZE_INT)((POINTER_SIZE_INT)1<<(BITS_OF_POINTER_SIZE_INT-1))
#define MIN_SPACE_THRESHOLD 0

enum CC_Scheduler_Kind{
  SCHEDULER_NIL = 0x00,
  TIME_BASED_SCHEDULER = 0x01,
  SPACE_BASED_SCHEDULER = 0x02
};

static unsigned int cc_scheduler_kind = SCHEDULER_NIL;

void gc_enable_time_scheduler()
{ cc_scheduler_kind |= TIME_BASED_SCHEDULER; }

void gc_enable_space_scheduler()
{ cc_scheduler_kind |= SPACE_BASED_SCHEDULER; }

Boolean gc_use_time_scheduler()
{ return cc_scheduler_kind & TIME_BASED_SCHEDULER; }

Boolean gc_use_space_scheduler()
{ return cc_scheduler_kind & SPACE_BASED_SCHEDULER; }


static int64 time_delay_to_start_mark = MAX_DELAY_TIME;
static POINTER_SIZE_INT space_threshold_to_start_mark = MAX_SPACE_THRESHOLD;

void con_collection_scheduler_initialize(GC* gc)
{
  Con_Collection_Scheduler* cc_scheduler = (Con_Collection_Scheduler*) STD_MALLOC(sizeof(Con_Collection_Scheduler));
  assert(cc_scheduler);
  memset(cc_scheduler, 0, sizeof(Con_Collection_Scheduler));
  
  cc_scheduler->gc = gc;
  gc->collection_scheduler = (Collection_Scheduler*)cc_scheduler;
  time_delay_to_start_mark = MAX_DELAY_TIME;
  space_threshold_to_start_mark = MAX_SPACE_THRESHOLD;
  
  return;
}

void con_collection_scheduler_destruct(GC* gc)
{
  STD_FREE(gc->collection_scheduler);
}


void gc_decide_cc_scheduler_kind(char* cc_scheduler)
{
  string_to_upper(cc_scheduler);
  if(!strcmp(cc_scheduler, "time")){
    gc_enable_time_scheduler();
  }else if(!strcmp(cc_scheduler, "space")){
    gc_enable_space_scheduler();
  }else if(!strcmp(cc_scheduler, "all")){
    gc_enable_time_scheduler();
    gc_enable_space_scheduler();
  }
}

void gc_set_default_cc_scheduler_kind()
{
  gc_enable_time_scheduler();
}

/*====================== new scheduler ===================*/
extern unsigned int NUM_CON_MARKERS;
extern unsigned int NUM_CON_SWEEPERS;
unsigned int gc_get_mutator_number(GC *gc);

#define MOSTLY_CON_MARKER_DIVISION 0.5
unsigned int mostly_con_final_marker_num=1;
unsigned int mostly_con_long_marker_num=1;

unsigned int gc_get_marker_number(GC* gc) {
  unsigned int mutator_num = gc_get_mutator_number(gc);
  unsigned int marker_specified = NUM_CON_MARKERS;
  if(marker_specified == 0) {
    if( gc_is_kind(ALGO_CON_OTF_OBJ) || gc_is_kind(ALGO_CON_OTF_REF) ) {
	marker_specified = min(gc->num_conclctors, mutator_num>>1);
	INFO2("gc.con.scheduler", "[Marker Num] mutator num="<<mutator_num<<", assign marker num="<<marker_specified);
    } else if(gc_is_kind(ALGO_CON_MOSTLY)) {
       marker_specified = min(gc->num_conclctors, mutator_num>>1);
	mostly_con_final_marker_num = max(marker_specified, mostly_con_final_marker_num); // in the STW phase, so all the conclctor can be used
	mostly_con_long_marker_num = (unsigned int)(marker_specified*MOSTLY_CON_MARKER_DIVISION);
       //INFO2("gc.con.scheduler", "[Marker Num] common marker="<<marker_specified<<", final marker="<<mostly_con_final_marker_num);
    }
  }

  assert(marker_specified);
  return marker_specified;
}

#define CON_SWEEPER_DIVISION 0.8
unsigned int gc_get_sweeper_numer(GC *gc) {
  unsigned int sweeper_specified = NUM_CON_SWEEPERS;
  if(sweeper_specified == 0) 
    sweeper_specified = (unsigned int)(gc->num_conclctors*CON_SWEEPER_DIVISION);
  //INFO2("gc.con.scheduler", "[Sweeper Num] assign sweeper num="<<sweeper_specified);
  assert(sweeper_specified);
  return sweeper_specified;
}




#define DEFAULT_CONSERCATIVE_FACTOR (1.0f)
#define CONSERCATIVE_FACTOR_FULLY_CONCURRENT (0.95f)
static float conservative_factor = DEFAULT_CONSERCATIVE_FACTOR;

/* for checking heap effcient*/
#define SMALL_DELTA 1000 //minimal check frequency is about delta us
#define SPACE_CHECK_STAGE_TWO_TIME (SMALL_DELTA<<6)
#define SPACE_CHECK_STAGE_ONE_TIME (SMALL_DELTA<<12)

#define DEFAULT_ALLOC_RATE (1<<19) //500k/ms
#define DEFAULT_MARKING_TIME (1<<9) //512 ms

static int64 last_check_time_point = time_now();
static int64 check_delay_time = time_now(); //  initial value is just for modifying

//just debugging
int64 get_last_check_point()
{
   return last_check_time_point;
}

static unsigned int alloc_space_threshold = 0;

static unsigned int space_check_stage_1; //SPACE_CHECK_EXPECTED_START_TIME
static unsigned int space_check_stage_2; //BIG_DELTA

static unsigned int calculate_start_con_space_threshold(Con_Collection_Statistics *con_collection_stat, unsigned int heap_size)
{
  
  float util_rate = con_collection_stat->heap_utilization_rate;
  unsigned int space_threshold = 0;
  if( gc_is_kind(ALGO_CON_OTF_OBJ) || gc_is_kind(ALGO_CON_OTF_REF) ) {
    if( con_collection_stat->trace_rate == 0 )  //for initial iteration
         con_collection_stat->trace_rate = con_collection_stat->alloc_rate*20;
    unsigned int alloc_rate = con_collection_stat->alloc_rate;
    if(alloc_rate<con_collection_stat->trace_rate) {       //  THRESHOLD = Heap*utilization_rate*(1-alloc_rate/marking_rate), accurate formaler
      float alloc_marking_rate_ratio = (float)(alloc_rate)/con_collection_stat->trace_rate;
     
      space_threshold = (unsigned int)(heap_size*util_rate*(1-alloc_marking_rate_ratio)*conservative_factor);
    } else {  //use default
       unsigned int alloc_while_marking = DEFAULT_MARKING_TIME*con_collection_stat->alloc_rate;
       space_threshold = (unsigned int)(heap_size*util_rate) -alloc_while_marking;
    }
  } else if(gc_is_kind(ALGO_CON_MOSTLY)) {
    unsigned int alloc_while_marking = DEFAULT_MARKING_TIME*con_collection_stat->alloc_rate;
    space_threshold = (unsigned int)(heap_size*util_rate) -alloc_while_marking;
  }

  if( space_threshold > con_collection_stat->surviving_size_at_gc_end )
    alloc_space_threshold = space_threshold - con_collection_stat->surviving_size_at_gc_end;
  else
    alloc_space_threshold = MIN_SPACE_THRESHOLD;
    
  //INFO2("gc.con.info", "[Threshold] alloc_space_threshold=" << alloc_space_threshold);
  return space_threshold;
}

/* this parameters are updated at end of GC */
void gc_update_scheduler_parameter( GC *gc )
{
   Con_Collection_Statistics *con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
   last_check_time_point = time_now();
   
   unsigned int alloc_rate = con_collection_stat->alloc_rate;
   space_check_stage_1 = alloc_rate * trans_time_unit(SPACE_CHECK_STAGE_ONE_TIME);
   space_check_stage_2 = alloc_rate * trans_time_unit(SPACE_CHECK_STAGE_TWO_TIME);
   //INFO2( "gc.con.scheduler", "space_check_stage_1=["<<space_check_stage_1<<"], space_check_stage_2=["<<space_check_stage_2<<"]" );

   check_delay_time = (con_collection_stat->gc_start_time - con_collection_stat->gc_end_time)>>2;
   //INFO2("gc.con.scheduler", "next check time = [" << trans_time_unit(check_delay_time) << "] ms" );
   if(gc_is_specify_con_sweep()) {
	  conservative_factor = CONSERCATIVE_FACTOR_FULLY_CONCURRENT;
   }
   calculate_start_con_space_threshold(con_collection_stat, gc->committed_heap_size);
}

void gc_force_update_scheduler_parameter( GC *gc ) 
{
    last_check_time_point = time_now();
    //check_delay_time = SPACE_CHECK_STAGE_ONE_TIME;
    check_delay_time = time_now();
    //INFO2("gc.con.scheduler", "next check time = [" << trans_time_unit(check_delay_time) << "] ms" );
    Con_Collection_Statistics *con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
    con_collection_stat->alloc_rate = DEFAULT_ALLOC_RATE;
}



static inline Boolean check_start_mark( GC *gc ) 
{
   unsigned int new_object_occupied_size = gc_get_mutator_new_obj_size(gc);
   Con_Collection_Statistics *con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
   /*just debugging*/
   float used_rate = (float)(con_collection_stat->surviving_size_at_gc_end + new_object_occupied_size)/gc->committed_heap_size;
   if( alloc_space_threshold < new_object_occupied_size ) {
	INFO2( "gc.con.info", "[Start Con] check has been delayed " << check_delay_time << " us, until ratio at start point="<<used_rate );
	return TRUE;
   }
   
   unsigned int free_space = alloc_space_threshold - new_object_occupied_size;
     //INFO2("gc.con.info", "[GC Scheduler debug] alloc_space_threshold="<<alloc_space_threshold<<", new_object_occupied_size"<<new_object_occupied_size);
   int64 last_check_delay = check_delay_time;
     
   if( free_space < space_check_stage_2 ) {
	check_delay_time = SMALL_DELTA;
   } else if( free_space < space_check_stage_1 ) {
       if(check_delay_time>SPACE_CHECK_STAGE_TWO_TIME ) { //if time interval is too small, the alloc rate will not be updated
           unsigned int interval_time = trans_time_unit(time_now() - con_collection_stat->gc_end_time);
	    unsigned int interval_space = new_object_occupied_size;
	    con_collection_stat->alloc_rate = interval_space/interval_time;
	}
	check_delay_time = ((alloc_space_threshold - new_object_occupied_size)/con_collection_stat->alloc_rate)<<9;
   }
   last_check_time_point = time_now();

   //INFO2("gc.con.info", "[GC Scheduler] check has been delayed=" << last_check_delay << " us, used_rate=" << used_rate << ", free_space=" << free_space << " bytes, next delay=" << check_delay_time << " us" );
   return FALSE;
}

static SpinLock check_lock;
static inline Boolean space_should_start_mark( GC *gc) 
{
  if( ( time_now() -last_check_time_point ) > check_delay_time && try_lock(check_lock) ) { //first condition is checked frequently, second condition is for synchronization
      Boolean should_start = check_start_mark(gc);
      unlock(check_lock); 
      return should_start;
  }
  return FALSE;
}

inline static Boolean gc_con_start_condition( GC* gc ) {
   return space_should_start_mark(gc);
}


void gc_reset_after_con_collection(GC *gc);
void gc_merge_free_list_global(GC *gc);
void gc_con_stat_information_out(GC *gc);

unsigned int sub_time = 0;
int64 pause_time = 0;
/* 
   concurrent collection entry function, it may start proper phase according to the current state.
*/
Boolean gc_con_perform_collection( GC* gc ) {
  int disable_count;
  int64 pause_start;
  Con_Collection_Statistics *con_collection_stat = gc_ms_get_con_collection_stat((GC_MS*)gc);
  switch( gc->gc_concurrent_status ) {
    case GC_CON_NIL :
      if( !gc_con_start_condition(gc) )
        return FALSE;
      if( !state_transformation( gc, GC_CON_NIL, GC_CON_STW_ENUM ) )
        return FALSE;
      
      gc->num_collections++;
      gc->cause = GC_CAUSE_CONCURRENT_GC;

      con_collection_stat->gc_start_time = time_now();
      disable_count = hythread_reset_suspend_disable();
	  
      gc_start_con_enumeration(gc); //now, it is a stw enumeration
      con_collection_stat->marking_start_time = time_now();
      state_transformation( gc, GC_CON_STW_ENUM, GC_CON_START_MARKERS );
      gc_start_con_marking(gc);

      INFO2("gc.con.time","[ER] start con pause, ERSM="<<((unsigned int)(time_now()-con_collection_stat->gc_start_time))<<"  us "); // ERSM means enumerate rootset and start concurrent marking
      vm_resume_threads_after();
      hythread_set_suspend_disable(disable_count);
      break;
	  
    case GC_CON_BEFORE_SWEEP :
      if(!gc_is_specify_con_sweep())
	  return FALSE;
      if( !state_transformation( gc, GC_CON_BEFORE_SWEEP, GC_CON_SWEEPING ) )
	  return FALSE;
      gc_ms_start_con_sweep((GC_MS*)gc, gc_get_sweeper_numer(gc));
      break;
     
	
    case GC_CON_BEFORE_FINISH :
	 if( !state_transformation( gc, GC_CON_BEFORE_FINISH, GC_CON_RESET ) )
		  return FALSE;
        /* thread should be suspended before the state transformation,
            it is for the case that the heap is exhausted in the reset state, although it is almost impossible */
        disable_count = vm_suspend_all_threads();
	 pause_start = time_now();
	 
        gc_merge_free_list_global(gc);
        gc_reset_after_con_collection(gc);
        state_transformation( gc, GC_CON_RESET, GC_CON_NIL );
	 pause_time = time_now()-pause_start;
	 
        vm_resume_all_threads(disable_count);
	 gc_con_stat_information_out(gc);
	 INFO2("gc.con.time","[GC][Con]pause(reset collection):  CRST="<<pause_time<<"  us\n\n"); // CRST means concurrent reset
        break;
    default :
      return FALSE;
  }
  return TRUE;
}


