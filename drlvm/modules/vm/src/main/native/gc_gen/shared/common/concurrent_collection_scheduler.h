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

#ifndef __CONCURRENT_COLLECTION_SCHEDULER_H_
#define __CONCURRENT_COLLECTION_SCHEDULER_H_

#define STAT_SAMPLE_WINDOW_SIZE 5

struct GC_MS;
typedef struct Con_Collection_Scheduler {
  /*common field*/
  GC* gc;
  
  /*concurrent scheduler */
  int64 time_delay_to_start_mark;
  POINTER_SIZE_INT space_threshold_to_start_mark;
  
  int64 last_mutator_time;
  int64 last_collector_time;

  unsigned int last_marker_num;

  unsigned int num_window_slots;
  unsigned int last_window_index;
  
  float alloc_rate_window[STAT_SAMPLE_WINDOW_SIZE];
  float trace_rate_window[STAT_SAMPLE_WINDOW_SIZE];
  float space_utilization_ratio[STAT_SAMPLE_WINDOW_SIZE];
  POINTER_SIZE_INT trace_load_window[STAT_SAMPLE_WINDOW_SIZE];
  POINTER_SIZE_INT alloc_load_window[STAT_SAMPLE_WINDOW_SIZE];
} Con_Collection_Scheduler;

void con_collection_scheduler_initialize(GC* gc);
void con_collection_scheduler_destruct(GC* gc);

void gc_update_scheduler_parameter( GC *gc );
void gc_force_update_scheduler_parameter( GC *gc );
Boolean gc_con_perform_collection( GC* gc );
Boolean gc_sched_con_collection(GC* gc, unsigned int gc_cause);

void gc_decide_cc_scheduler_kind(char* cc_scheduler);
void gc_set_default_cc_scheduler_kind();

extern unsigned int mostly_con_final_marker_num;
extern unsigned int mostly_con_long_marker_num;

#endif


