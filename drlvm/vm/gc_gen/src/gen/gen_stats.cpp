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

#ifdef GC_GEN_STATS

#include "gen.h"
#include "gen_stats.h"

Boolean gc_profile = FALSE;

void gc_gen_stats_initialize(GC_Gen* gc)
{
  GC_Gen_Stats* stats = (GC_Gen_Stats*)STD_MALLOC(sizeof(GC_Gen_Stats));

  memset(stats, 0, sizeof(GC_Gen_Stats));
  stats->is_los_collected = FALSE;

  gc->stats = stats; 
}

void gc_gen_stats_destruct(GC_Gen* gc)
{
  STD_FREE(gc->stats);
}

void gc_gen_stats_reset_before_collection(GC_Gen* gc)
{
  GC_Gen_Stats* stats = gc->stats;

  if(collect_is_minor()){
    stats->nos_surviving_obj_num_minor = 0;
    stats->nos_surviving_obj_size_minor = 0;
    stats->los_suviving_obj_num = 0;
    stats->los_suviving_obj_size = 0;
    stats->is_los_collected = false;
  }else{
    stats->nos_mos_suviving_obj_num_major = 0;
    stats->nos_mos_suviving_obj_size_major = 0;
    stats->los_suviving_obj_num = 0;
    stats->los_suviving_obj_size = 0;
    stats->is_los_collected = false;  
  }
}

void gc_gen_stats_update_after_collection(GC_Gen* gc)
{
  Collector** collector = gc->collectors;
  GC_Gen_Stats* gc_gen_stats = gc->stats;
  GC_Gen_Collector_Stats* collector_stats;
  Boolean is_los_collected = gc_gen_stats->is_los_collected;

  if(collect_is_minor()) {

    for (unsigned int i=0; i<gc->num_active_collectors; i++) {
      collector_stats = (GC_Gen_Collector_Stats*)collector[i]->stats;
      gc_gen_stats->nos_surviving_obj_num_minor += collector_stats->nos_obj_num_moved_minor;
      gc_gen_stats->nos_surviving_obj_size_minor += collector_stats->nos_obj_size_moved_minor;
    }

    gc_gen_stats->nos_surviving_ratio_minor = ((float)gc_gen_stats->nos_surviving_obj_size_minor)/gc->nos->committed_heap_size;

  }else{

    for (unsigned int i=0; i < gc->num_active_collectors; i++) {
      collector_stats = (GC_Gen_Collector_Stats*)collector[i]->stats;
      gc_gen_stats->nos_mos_suviving_obj_num_major += collector_stats->nos_mos_obj_num_moved_major;
      gc_gen_stats->nos_mos_suviving_obj_size_major += collector_stats->nos_mos_obj_size_moved_major;

      /*need to accumulate the los related info if los is collected when major*/
      if(is_los_collected) {
        gc_gen_stats->los_suviving_obj_num += collector_stats->los_obj_num_moved_major;
        gc_gen_stats->los_suviving_obj_size += collector_stats->los_obj_size_moved_major;
      }
    }

    gc_gen_stats->nos_mos_suviving_ratio_major = ((float)gc_gen_stats->nos_mos_suviving_obj_size_major)/(gc->nos->committed_heap_size+gc->mos->committed_heap_size);
  }

  if (is_los_collected) {
    gc_gen_stats->los_surviving_ratio = ((float)gc_gen_stats->los_suviving_obj_size)/gc->los->committed_heap_size;
  }
}

void gc_gen_stats_verbose(GC_Gen* gc)
{
  GC_Gen_Stats* stats = gc->stats;
  Boolean is_los_collected = stats->is_los_collected;
  if (collect_is_minor()){
    TRACE2("gc.space", "GC: NOS Collection stats: "
      <<"\nGC: " << (gc_is_gen_mode()?"generational":"nongenerational")
      <<"\nGC: collection algo: " << (minor_is_semispace()?"semi-space":"partial-forward")
      <<"\nGC: num surviving objs: " << stats->nos_surviving_obj_num_minor
      <<"\nGC: size surviving objs: " << verbose_print_size(stats->nos_surviving_obj_size_minor)
      <<"\nGC: surviving ratio: " << (int)(stats->nos_surviving_ratio_minor*100) << "%\n");
  }else{
    TRACE2("gc.space", "GC: MOS Collection stats: "
      <<"\nGC: collection algo: " << (major_is_marksweep()?"mark-sweep":"slide compact")
      <<"\nGC: num surviving objs: "<<stats->nos_mos_suviving_obj_num_major
      <<"\nGC: size surviving objs: "<<verbose_print_size(stats->nos_mos_suviving_obj_size_major)
      <<"\nGC: surviving ratio: "<<(int)(stats->nos_mos_suviving_ratio_major*100)<<"%\n");
  }

  if(stats->is_los_collected) { /*if los is collected, need to output los related info*/
    TRACE2("gc.space", "GC: Lspace Collection stats: "
      <<"\nGC: collection algo: "<<(collect_is_major()?"slide compact":"mark sweep")
      <<"\nGC: num surviving objs: "<<stats->los_suviving_obj_num
      <<"\nGC: size surviving objs: "<<verbose_print_size(stats->los_suviving_obj_size)
      <<"\nGC: surviving ratio: "<<(int)(stats->los_surviving_ratio*100)<<"%\n");
  }

}

void gc_gen_collector_stats_initialize(Collector* collector)
{
  GC_Gen_Collector_Stats* stats = (GC_Gen_Collector_Stats*)STD_MALLOC(sizeof(GC_Gen_Collector_Stats));
  memset(stats, 0, sizeof(GC_Gen_Collector_Stats));
  collector->stats = (void*)stats; 
}


void gc_gen_collector_stats_destruct(Collector* collector)
{
  STD_FREE(collector->stats);
}

void gc_gen_collector_stats_reset(GC_Gen* gc)
{
  Collector** collector = gc->collectors;
  GC_Gen_Collector_Stats* stats;
  for (unsigned int i=0; i<gc->num_active_collectors; i++){
    stats = (GC_Gen_Collector_Stats*)collector[i]->stats;
    memset(stats, 0, sizeof(GC_Gen_Collector_Stats));
  }
}


void gc_gen_collector_stats_verbose_minor_collection(GC_Gen* gc)
{
  Collector** collector = gc->collectors;
  GC_Gen_Collector_Stats* stats;

  /*variable used to accumulate each collector's stats when minor collection*/
  unsigned int total_process_rootset_ref = 0;
  unsigned int total_mark_nos_obj_num = 0;
  unsigned int total_mark_non_nos_obj_num = 0;
  unsigned int total_forward_obj_num = 0;
  POINTER_SIZE_INT total_forward_obj_size = 0;

  for (unsigned int i=0; i<gc->num_active_collectors; i++){
    stats = (GC_Gen_Collector_Stats*)collector[i]->stats;

    total_process_rootset_ref += stats->process_rootset_ref_num;
    total_mark_nos_obj_num += stats->nos_obj_num_marked_minor;
    total_mark_non_nos_obj_num += stats->nonnos_obj_num_marked_minor;
    total_forward_obj_num += stats->nos_obj_num_moved_minor;
    total_forward_obj_size += stats->nos_obj_size_moved_minor;

    /*output each collector's stats*/
    TRACE2("gc.collect", "GC: Collector["<<((POINTER_SIZE_INT)collector[i]->thread_handle)<<"] stats when collection:"
      <<"\nGC: process rootset ref num: "<<stats->process_rootset_ref_num
      <<"\nGC: mark nos obj num: "<<stats->nos_obj_num_marked_minor
      <<"\nGC: mark nonnos obj num: "<<stats->nonnos_obj_num_marked_minor
      <<" \nGC: forword obj num: "<<stats->nos_obj_num_moved_minor
      <<" \nGC: forward obj size: "<<verbose_print_size(stats->nos_obj_size_moved_minor)<<"\n");
  }

  /*output accumulated info for all collectors*/
  TRACE2("gc.collect", "GC: Total Collector Stats when collection: "
    <<"\nGC: process rootset ref num: "<<total_process_rootset_ref
    <<"\nGC: mark nos obj num: "<<total_mark_nos_obj_num
    <<"\nGC: mark nonnos obj num: "<<total_mark_non_nos_obj_num
    <<"\nGC: forword obj num: "<<total_forward_obj_num
    <<"\nGC: forward obj size: "<<verbose_print_size(total_forward_obj_size)<<"\n");

}

void gc_gen_collector_stats_verbose_major_collection(GC_Gen* gc)
{
  Collector** collector = gc->collectors;
  GC_Gen_Collector_Stats* stats;

  Boolean is_los_collected = gc->stats->is_los_collected;

  /*variable used to accumulate each collector's stats when major collection*/
  unsigned int total_process_rootset_ref = 0;
  unsigned int total_mark_heap_live_obj_num = 0;
  unsigned int total_move_mos_nos_live_obj_num = 0;
  POINTER_SIZE_INT total_move_mos_nos_live_obj_size = 0;
  unsigned int total_move_los_live_obj_num = 0;
  POINTER_SIZE_INT total_move_los_live_obj_size = 0;

  for (unsigned int i=0; i<gc->num_active_collectors; i++){
    stats = (GC_Gen_Collector_Stats*)collector[i]->stats;

    total_process_rootset_ref = stats->process_rootset_ref_num;
    total_mark_heap_live_obj_num = stats->num_obj_marked_major;
    total_move_mos_nos_live_obj_num = stats->nos_mos_obj_num_moved_major;
    total_move_mos_nos_live_obj_size = stats->nos_mos_obj_size_moved_major;
    if (is_los_collected){/*if los is collected when major collection happened,  need to accumulate los related info*/
      total_move_los_live_obj_num = stats->los_obj_num_moved_major;
      total_move_los_live_obj_size = stats->los_obj_size_moved_major;
    }
    if(is_los_collected){
      TRACE2("gc.collect", "GC: Collector["<<((POINTER_SIZE_INT)collector[i]->thread_handle)<<"] stats when collection:"
        <<"\nGC: process rootset ref num: "<<stats->process_rootset_ref_num
        <<"\nGC: mark obj num: "<<stats->num_obj_marked_major
        <<"\nGC: move mos and nos obj num: "<<stats->nos_mos_obj_num_moved_major
        <<"\nGC: move obj size: "<<verbose_print_size(stats->nos_mos_obj_size_moved_major)
        <<"\nGC: move los obj num: "<<stats->los_obj_num_moved_major
        <<"\nGC: move obj size: "<<verbose_print_size(stats->los_obj_size_moved_major)<<"\n");
    }else{
      TRACE2("gc.collect", "GC: Collector["<<((POINTER_SIZE_INT)collector[i]->thread_handle)<<"] stats when collection:"
        <<"\nGC: process rootset ref num: "<<stats->process_rootset_ref_num
        <<"\nGC: mark obj num: "<<stats->num_obj_marked_major
        <<"\nGC: move obj num: "<<stats->nos_mos_obj_num_moved_major
        <<"\nGC: move obj size: "<<verbose_print_size(stats->nos_mos_obj_size_moved_major)<<"\n");
    }
  }


  if(is_los_collected){/*if los is collected when major collection happened,  need to output los related collector info*/
    TRACE2("gc.collect", "GC: Total Collector Stats when collection: "
      <<"\nGC: process rootset ref num: "<<total_process_rootset_ref
      <<"\nGC: mark obj num: "<<total_mark_heap_live_obj_num
      <<"\nGC: move mos and nos obj num: "<<total_move_mos_nos_live_obj_num
      <<"\nGC: move obj size: "<<verbose_print_size(total_move_mos_nos_live_obj_size)
      <<"\nGC: move los obj num: "<<total_move_los_live_obj_num
      <<"\nGC: move obj size: "<<verbose_print_size(total_move_los_live_obj_size)<<"\n");
  }else{
    TRACE2("gc.collect", "GC: Total Collector Stats when collection: "
      <<"\nGC: process rootset ref num: "<<total_process_rootset_ref
      <<"\nGC: mark obj num: "<<total_mark_heap_live_obj_num
      <<"\nGC: move mos and nos obj num: "<<total_move_mos_nos_live_obj_num
      <<"\nGC: move obj size: "<<verbose_print_size(total_move_mos_nos_live_obj_size)<<"\n");
  }
}


#endif
