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

#ifndef _GEN_STATS_H_
#define _GEN_STATS_H_

#include "gen.h"

extern Boolean gc_profile;

typedef struct GC_Gen_Stats {
  unsigned int num_minor_collections;
  unsigned int num_major_collections;
  unsigned int num_fallback_collections;

  /*time related info*/
  int64 total_pause_time;  /*total time used for collection*/
  int64 total_mutator_time;  /*total time used for executing application program*/

  unsigned int obj_num_nos_alloc;
  POINTER_SIZE_INT total_size_nos_alloc;
  unsigned int obj_num_los_alloc;
  POINTER_SIZE_INT total_size_los_alloc;

  /*minor related info*/
  unsigned int nos_surviving_obj_num_minor;
  POINTER_SIZE_INT nos_surviving_obj_size_minor;
  float nos_surviving_ratio_minor;
  int nos_collection_algo_minor;

  /*major related info*/
  unsigned int nos_mos_suviving_obj_num_major;
  POINTER_SIZE_INT nos_mos_suviving_obj_size_major;
  float nos_mos_suviving_ratio_major;
  int nos_mos_collection_algo_major;

  /*los related info when minor or major*/
  Boolean is_los_collected; /*whether large obj space is collected or not*/
  unsigned int los_suviving_obj_num;
  POINTER_SIZE_INT los_suviving_obj_size;
  float los_surviving_ratio;
  int los_collection_algo;

}GC_Gen_Stats;

inline void gc_gen_stats_set_nos_algo(GC_Gen* gc, int algo)
{
  gc->stats->nos_collection_algo_minor = algo;
}

inline void gc_gen_stats_set_mos_algo(GC_Gen* gc, int algo)
{
  gc->stats->nos_mos_collection_algo_major = algo;
}

inline void gc_gen_stats_set_los_algo(GC_Gen* gc, int algo)
{
  gc->stats->los_collection_algo = algo;
}

inline void gc_gen_stats_set_los_collected_flag(GC_Gen* gc, Boolean flag)
{
  gc->stats->is_los_collected = flag;
}

inline void gc_gen_update_nos_alloc_obj_stats(GC_Gen_Stats* stats, POINTER_SIZE_INT size)
{
  stats->total_size_nos_alloc += size;
}

inline void gc_gen_update_los_alloc_obj_stats(GC_Gen_Stats* stats, POINTER_SIZE_INT size)
{
  stats->obj_num_nos_alloc++;
  stats->total_size_nos_alloc += size;
}


void gc_gen_stats_initialize(GC_Gen* gc);
void gc_gen_stats_destruct(GC_Gen* gc);
void gc_gen_stats_reset_before_collection(GC_Gen* gc);
void gc_gen_stats_update_after_collection(GC_Gen* gc);
void gc_gen_stats_verbose(GC_Gen* gc);

typedef struct GC_Gen_Collector_Stats {
  unsigned int process_rootset_ref_num; 

  /*minor related info*/
  unsigned int nos_obj_num_moved_minor;
  POINTER_SIZE_INT nos_obj_size_moved_minor;
  unsigned int nos_obj_num_marked_minor;
  unsigned int nonnos_obj_num_marked_minor;

  /*major related info*/
  unsigned int num_obj_marked_major;
  unsigned int nos_mos_obj_num_moved_major;
  POINTER_SIZE_INT nos_mos_obj_size_moved_major;
  unsigned int los_obj_num_moved_major;
  POINTER_SIZE_INT los_obj_size_moved_major;

}GC_Gen_Collector_Stats;

inline void gc_gen_collector_update_rootset_ref_num(GC_Gen_Collector_Stats* stats)
{
  stats->process_rootset_ref_num++;
}

inline void gc_gen_collector_update_moved_nos_obj_stats_minor(GC_Gen_Collector_Stats* stats, POINTER_SIZE_INT size)
{
  stats->nos_obj_num_moved_minor++;
  stats->nos_obj_size_moved_minor += size;
}

inline void gc_gen_collector_update_marked_nos_obj_stats_minor(GC_Gen_Collector_Stats* stats)
{
  stats->nos_obj_num_marked_minor++;
}

inline void gc_gen_collector_update_marked_nonnos_obj_stats_minor(GC_Gen_Collector_Stats* stats)
{
  stats->nos_obj_num_marked_minor++;
}

inline void gc_gen_collector_update_marked_obj_stats_major(GC_Gen_Collector_Stats* stats)
{
  stats->num_obj_marked_major++;
}

inline void gc_gen_collector_update_moved_nos_mos_obj_stats_major(GC_Gen_Collector_Stats* stats, POINTER_SIZE_INT size)
{
  stats->nos_mos_obj_num_moved_major++;
  stats->nos_mos_obj_size_moved_major += size;
}

inline void gc_gen_collector_update_moved_los_obj_stats_major(GC_Gen_Collector_Stats* stats, POINTER_SIZE_INT size)
{
  stats->los_obj_num_moved_major++;
  stats->los_obj_size_moved_major += size;
}

void gc_gen_collector_stats_reset(GC_Gen* gc);
void gc_gen_collector_stats_initialize(Collector* collector);
void gc_gen_collector_stats_destruct(Collector* collector);
void gc_gen_collector_stats_verbose_minor_collection(GC_Gen* gc);
void gc_gen_collector_stats_verbose_major_collection(GC_Gen* gc);

#endif
