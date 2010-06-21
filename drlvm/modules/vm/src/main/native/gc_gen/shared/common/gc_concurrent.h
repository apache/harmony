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

#ifndef _GC_CONCURRENT_H_
#define _GC_CONCURRENT_H_
#include "gc_common.h"


#define RATE_CALCULATE_DENOMINATOR_FACTOR 10; //trans us to ms
inline unsigned int trans_time_unit(int64 x) 
{
  int64 result = x>>10;
  if(result) return (unsigned int)result;
  return 1;
}

#define RAISE_ERROR  assert(0);
/* concurrent collection states in new design */
enum GC_CONCURRENT_STATUS {
  GC_CON_NIL = 0x00,
  GC_CON_STW_ENUM = 0x01,
  GC_CON_START_MARKERS = 0x02,
  GC_CON_TRACING = 0x03,
  GC_CON_TRACE_DONE = 0x04,
  GC_CON_BEFORE_SWEEP = 0x05,
  GC_CON_SWEEPING = 0x06,
  GC_CON_SWEEP_DONE = 0x07,
  GC_CON_BEFORE_FINISH = 0x08,
  GC_CON_RESET = 0x09,
  GC_CON_DISABLE = 0x0A,
};

// this type is just for debugging and time measuring
enum GC_PARTIAL_STW_TYPE {
  GC_PARTIAL_PSTW = 0x00,  //pure stop the world
  GC_PARTIAL_PMSS = 0x01,  //concurrent marking has finished and stop the world sweeping
  GC_PARTIAL_CMSS = 0x02,  // partial concurrent marking and stop the world sweeping
  GC_PARTIAL_CMPS = 0x03,  //concurrent marking and sweeping
  GC_PARTIAL_FCSR = 0x04, //fully concurrent marking and sweeping, but stw finish reset
};

enum HANDSHAKE_SINGAL{
  HSIG_MUTATOR_SAFE = 0x0,
  HSIG_DISABLE_SWEEP_LOCAL_CHUNKS  = 0x01,
  HSIG_DISABLE_SWEEP_GLOBAL_CHUNKS = 0x02,
  HSIG_MUTATOR_ENTER_ALLOC_MARK    = 0x03,
};

typedef struct Con_Collection_Statistics {
    POINTER_SIZE_INT live_size_marked;     //marked objects size
    POINTER_SIZE_INT alloc_size_before_alloc_live;  //alloc objects size before marking
    POINTER_SIZE_INT live_alloc_size;
    POINTER_SIZE_INT surviving_size_at_gc_end; //total live object size when gc is ended
    
    POINTER_SIZE_INT trace_rate;  //bytes per ms
    POINTER_SIZE_INT alloc_rate;       //bytes per ms

    float heap_utilization_rate;

    int64 gc_start_time;
    int64 gc_end_time;

    int64 marking_start_time;
    int64 marking_end_time;
	
    int64 sweeping_time;
    int64 pause_start_time;

} Con_Space_Statistics;

inline void gc_set_con_gc(unsigned int con_phase)
{ GC_PROP |= con_phase;  }

inline void gc_specify_con_enum()
{ gc_set_con_gc(ALGO_CON_ENUM); }

inline void gc_specify_con_mark()
{ gc_set_con_gc(ALGO_CON_MARK);  }

inline void gc_specify_con_sweep()
{ gc_set_con_gc(ALGO_CON_SWEEP); }

inline Boolean gc_is_specify_con_gc()
{ return (GC_PROP & ALGO_CON) != 0; }

inline Boolean gc_is_specify_con_enum()
{ return (GC_PROP & ALGO_CON_ENUM) == ALGO_CON_ENUM;  }

inline Boolean gc_is_specify_con_mark()
{ return (GC_PROP & ALGO_CON_MARK) == ALGO_CON_MARK;  }

inline Boolean gc_is_specify_con_sweep()
{ return (GC_PROP & ALGO_CON_SWEEP) == ALGO_CON_SWEEP; }


extern volatile Boolean obj_alloced_live;

inline Boolean is_obj_alloced_live()
{ return obj_alloced_live;  }

inline void gc_disable_alloc_obj_live(GC *gc)
{ 
  obj_alloced_live = FALSE;
}

void gc_enable_alloc_obj_live(GC * gc);

/*  
    tranform the states across the collection process, 
  which should be a atomic operation because there are several collector run parallel
*/
inline Boolean state_transformation( GC* gc, unsigned int from_state, unsigned int to_state ) 
{
  unsigned int old_state = apr_atomic_cas32( &gc->gc_concurrent_status, to_state, from_state );
  if( old_state != from_state )
    return FALSE;
  else
    return TRUE;
}

/* set concurrent to idle,
    Or enable concurrent gc, called when STW gc finishes
 */
inline void set_con_nil( GC *gc ) {
  apr_atomic_set32( &gc->gc_concurrent_status, GC_CON_NIL );
}


/* gc start enumeration phase, now, it is in a stop-the-world manner */
void gc_start_con_enumeration(GC * gc);

/* gc start marking phase */
void gc_start_con_marking(GC *gc);


/* prepare for sweeping */
void gc_prepare_sweeping(GC *gc);

/* gc start sweeping phase */
void gc_start_con_sweeping(GC *gc);

/* gc finish concurrent collection */
void gc_con_final_work(GC* gc);


/* gc wait cocurrent collection finishes */
void gc_wait_con_finish( GC* gc );

/* is in gc marking phase */
inline Boolean in_con_marking_phase( GC *gc ) {
  unsigned int status = gc->gc_concurrent_status;
  return (status == GC_CON_TRACING) || (status == GC_CON_TRACE_DONE);
}

/* is in gc sweeping phase */
inline Boolean in_con_sweeping_phase( GC *gc ) {
  unsigned int status = gc->gc_concurrent_status;
  return (status == GC_CON_SWEEPING) || (status == GC_CON_SWEEP_DONE);
}

inline Boolean in_con_idle( GC *gc ) {
  return gc->gc_concurrent_status == GC_CON_NIL;
}

inline Boolean gc_con_is_in_STW( GC *gc ) {
  return gc->gc_concurrent_status == GC_CON_DISABLE;
}

/* is gc ready to sweeping */
inline Boolean in_con_ready_sweep( GC *gc ) {
  return gc->gc_concurrent_status == GC_CON_BEFORE_SWEEP;
}

/* is gc sweeping */
inline Boolean in_con_sweep( GC *gc ) {
  return ( gc->gc_concurrent_status == GC_CON_SWEEPING || gc->gc_concurrent_status == GC_CON_SWEEP_DONE );
  
}

void gc_con_update_stat_after_marking( GC *gc );


void gc_decide_con_algo(char* concurrent_algo);
void gc_set_default_con_algo();


extern volatile Boolean gc_sweep_global_normal_chunk;


inline Boolean gc_is_sweep_global_normal_chunk()
{ return gc_sweep_global_normal_chunk; }

inline void gc_set_sweep_global_normal_chunk()
{ gc_sweep_global_normal_chunk = TRUE; }

inline void gc_unset_sweep_global_normal_chunk()
{ gc_sweep_global_normal_chunk = FALSE; }
#endif
