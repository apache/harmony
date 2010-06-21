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

#include "gen.h"
#include "../common/space_tuner.h"
#include <math.h>

#define NOS_COPY_RESERVE_DELTA (GC_BLOCK_SIZE_BYTES<<1)
/*Tune this number in case that MOS could be too small, so as to avoid or put off fall back.*/
POINTER_SIZE_INT MOS_RESERVE_SIZE;
POINTER_SIZE_INT DEFAULT_MOS_RESERVE_SIZE = 36*MB;

static float Tslow = 0.0f;
static POINTER_SIZE_INT SMax = 0;
static POINTER_SIZE_INT last_total_free_size = 0;

typedef struct Gen_Mode_Adaptor{
  float gen_minor_throughput;
  float nongen_minor_throughput;

  /*for obtaining the gen minor collection throughput.*/
  int gen_mode_trial_count;

  float major_survive_ratio_threshold;
  unsigned int major_repeat_count;

  POINTER_SIZE_INT adapt_nos_size;
}Gen_Mode_Adaptor;

void gc_gen_mode_adapt_init(GC_Gen *gc)
{
  gc->gen_mode_adaptor = (Gen_Mode_Adaptor*)STD_MALLOC( sizeof(Gen_Mode_Adaptor));
  Gen_Mode_Adaptor* gen_mode_adaptor = gc->gen_mode_adaptor;
  
  gen_mode_adaptor->gen_minor_throughput = 0.0f;
  /*reset the nongen_minor_throughput: the first default nongen minor (maybe testgc)may caused the result
  calculated to be zero. so we initial the value to 1.0f here. */
  gen_mode_adaptor->nongen_minor_throughput = 1.0f;
  gen_mode_adaptor->gen_mode_trial_count = 0;

  gen_mode_adaptor->major_survive_ratio_threshold = 1.0f;
  gen_mode_adaptor->major_repeat_count  = 1;

  gen_mode_adaptor->adapt_nos_size = min_nos_size_bytes;
}

static float mini_free_ratio(float k, float m)
{
  /*fixme: the check should be proved!*/
  if(m < 0.005f) m = 0.005f;
  if(k > 100.f) k = 100.f;
  
  float b = - (2 + 2 * k * m);
  float c = k * m * m + 2 * m + 1;
  float D = b * b - 4 * c;
  if (D <= 0) {
    //printf("output 0.8f from k: %5.3f, m: %5.3f\n", k, m);
    return 0.8f;
  }
  float pm = sqrt (D) / 2 ;
  float base = - b / 2 ;
  float res = base - pm;
  if (res > 1.f) res = 0.8f;

  /*fixme: the check should be proved!*/
  if (res < 0.0f) res = 0.8f;

  //printf("output %5.3f from k: %5.3f, m: %5.3f\n", res, k, m);
  return res;
}

#define MAX_MAJOR_REPEAT_COUNT 3
#define MAX_MINOR_TRIAL_COUNT 2
#define MAX_INT32 0x7fffffff

void gc_gen_mode_adapt(GC_Gen* gc, int64 pause_time)
{
  if(GEN_NONGEN_SWITCH == FALSE) return;
  
  Blocked_Space* nos = (Blocked_Space*)gc->nos;
  Blocked_Space* mos = (Blocked_Space*)gc->mos;
  Gen_Mode_Adaptor* gen_mode_adaptor = gc->gen_mode_adaptor;

  POINTER_SIZE_INT mos_free_size = blocked_space_free_mem_size(mos);
  POINTER_SIZE_INT nos_free_size = blocked_space_free_mem_size(nos);
  POINTER_SIZE_INT total_free_size = mos_free_size  + nos_free_size;
  
  if(collect_is_major()) {
    assert(!gc_is_gen_mode());
    
    if(gen_mode_adaptor->major_survive_ratio_threshold != 0 && mos->survive_ratio > gen_mode_adaptor->major_survive_ratio_threshold){    
      if(gen_mode_adaptor->major_repeat_count > MAX_MAJOR_REPEAT_COUNT ){
        gc->force_gen_mode = TRUE;
        gc_set_gen_mode(TRUE);
        gc->next_collect_force_major = FALSE;
        return;
      }else{
        gen_mode_adaptor->major_repeat_count++;
      }
    }else{
      gen_mode_adaptor->major_repeat_count = 1;
    }
    
  }else{
    /*compute throughput*/
    if(!collect_last_is_minor((GC*)gc)){
      gen_mode_adaptor->nongen_minor_throughput = 1.0f;
    }
    if(gc->force_gen_mode){
      if(pause_time!=0){
        if(gen_mode_adaptor->gen_minor_throughput != 0)
          gen_mode_adaptor->gen_minor_throughput = (gen_mode_adaptor->gen_minor_throughput + (float) nos_free_size/(float)pause_time)/2.0f;
        else
          gen_mode_adaptor->gen_minor_throughput =(float) nos_free_size/(float)pause_time;
      }
    }else{
      if(pause_time!=0){
        if(gen_mode_adaptor->gen_minor_throughput != 1.0f)
          gen_mode_adaptor->nongen_minor_throughput = (gen_mode_adaptor->nongen_minor_throughput + (float) nos_free_size/(float)pause_time)/2.0f;      
        else
          gen_mode_adaptor->nongen_minor_throughput = (float) nos_free_size/(float)pause_time;
      }
   }

    if(gen_mode_adaptor->nongen_minor_throughput <=  gen_mode_adaptor->gen_minor_throughput ){
      if( !collect_last_is_minor((GC*)gc) ){
        gen_mode_adaptor->major_survive_ratio_threshold = mos->survive_ratio;
      }else if( !gc->force_gen_mode ){
        gc->force_gen_mode = TRUE;
        gen_mode_adaptor->gen_mode_trial_count = MAX_INT32;        
      } 
    }

    if(gc->next_collect_force_major && !gc->force_gen_mode){
        gc->next_collect_force_major = FALSE;
        gc->force_gen_mode = TRUE;
        gen_mode_adaptor->gen_mode_trial_count = 2;
    }else if( collect_last_is_minor((GC*)gc) && gc->force_gen_mode){
       gen_mode_adaptor->gen_mode_trial_count = MAX_INT32;
    }

    if(gc->force_gen_mode && (total_free_size <= ((float)min_nos_size_bytes) * 1.3 )){
        gc->force_gen_mode = FALSE;
        gc_set_gen_mode(FALSE);
        gc->next_collect_force_major = TRUE;
        gen_mode_adaptor->gen_mode_trial_count = 0;
        return;
    }
    
    if( gc->force_gen_mode ){
      assert( gen_mode_adaptor->gen_mode_trial_count >= 0);

      gen_mode_adaptor->gen_mode_trial_count --;
      if( gen_mode_adaptor->gen_mode_trial_count >= 0){
        gc_set_gen_mode(TRUE);
        return;
      }
          
      gc->force_gen_mode = FALSE;
      gc->next_collect_force_major = TRUE;    
      gen_mode_adaptor->gen_mode_trial_count = 0;
    }
  }
  
  gc_set_gen_mode(FALSE);
  return;
}

struct Mspace;
void mspace_set_expected_threshold_ratio(Mspace* mos, float threshold_ratio);

static void gc_decide_next_collect(GC_Gen* gc, int64 pause_time)
{
  Space* nos = (Space*)gc->nos;
  Space* mos = (Space*)gc->mos;

  float survive_ratio = 0.2f;
  
  if( MOS_RESERVE_SIZE != 0)
    DEFAULT_MOS_RESERVE_SIZE = MOS_RESERVE_SIZE;

  POINTER_SIZE_INT mos_free_size = mos_free_space_size(mos); 
  /* for space free size computation, semispace may leave some nos space used. But we use a simple approximation here.
      That is, we just use the totoal nos size as nos free size. This is important. We can't use real nos_free_space_size(), because
      the whole algorithm here in gc_decide_next_collect() assumes total free size is reduced after every minor collection, and 
      can only be increased after major collection. Otherwise the algorithm is invalid. If we use nos_free_space_size(), we may
      get an increased total free size after a minor collection. */
  POINTER_SIZE_INT nos_free_size = space_committed_size(nos);  

  POINTER_SIZE_INT total_free_size = mos_free_size  + nos_free_size;
  if(collect_is_major()) gc->force_gen_mode = FALSE;
  if(!gc->force_gen_mode){
    /*Major collection:*/
    if(collect_is_major()){
      mos->time_collections += pause_time;
  
      Tslow = (float)pause_time;
      SMax = total_free_size;
      /*If fall back happens, and nos_boundary reaches heap_ceiling, then we force major.*/
      if( nos_free_size == 0)
        gc->next_collect_force_major = TRUE;
      else gc->next_collect_force_major = FALSE;
      
      /*If major is caused by LOS, or collection kind is ALGO_MAJOR_EXTEND, all survive ratio is not updated.*/
      extern Boolean mos_extended;
      if((gc->cause != GC_CAUSE_LOS_IS_FULL) && !mos_extended ){
        survive_ratio = (float)mos->period_surviving_size/(float)mos->committed_heap_size;
        mos->survive_ratio = survive_ratio;
      }
      /* why do I set it FALSE here? because here is the only place where it's used. */
      mos_extended = FALSE;
      
      /*If there is no minor collection at all, we must give mos expected threshold a reasonable value.*/
      if((gc->tuner->kind != TRANS_NOTHING) && (nos->num_collections == 0))
        mspace_set_expected_threshold_ratio((Mspace *)mos, 0.5f);
      /*If this major is caused by fall back compaction, we must give nos->survive_ratio 
        *a conservative and reasonable number to avoid next fall back.
        *In fallback compaction, the survive_ratio of mos must be 1.*/
      if(collect_is_fallback()) nos->survive_ratio = 1;

    }
    /*Minor collection:*/    
    else
    {
      /*Give a hint to mini_free_ratio. */
      if(nos->num_collections == 1){
        /*Fixme: This is only set for tuning the first warehouse!*/
        Tslow = pause_time / gc->survive_ratio;
        SMax = (POINTER_SIZE_INT)((float)(gc->committed_heap_size - gc->los->committed_heap_size) * ( 1 - gc->survive_ratio ));
        last_total_free_size = gc->committed_heap_size - gc->los->committed_heap_size;
      }
  
      nos->time_collections += pause_time;  
      POINTER_SIZE_INT free_size_threshold;

      POINTER_SIZE_INT minor_surviving_size = last_total_free_size - total_free_size;
      /*If the first GC is caused by LOS, mos->last_alloced_size should be smaller than this minor_surviving_size
        *Because the last_total_free_size is not accurate.*/

      if(nos->num_collections != 1){
      	assert(minor_surviving_size == mos->last_alloced_size);
      }
        
      float k = Tslow * nos->num_collections/nos->time_collections;
      float m = ((float)minor_surviving_size)*1.0f/((float)(SMax - DEFAULT_MOS_RESERVE_SIZE ));
      float free_ratio_threshold = mini_free_ratio(k, m);

      if(SMax > DEFAULT_MOS_RESERVE_SIZE )
        free_size_threshold = (POINTER_SIZE_INT)(free_ratio_threshold * (SMax - DEFAULT_MOS_RESERVE_SIZE  ) + DEFAULT_MOS_RESERVE_SIZE  );
      else
        free_size_threshold = (POINTER_SIZE_INT)(free_ratio_threshold * SMax);

      /* FIXME: if the total free size is lesser than threshold, the time point might be too late!
       * Have a try to test whether the backup solution is better for specjbb.
       */
      //   if ((mos_free_size + nos_free_size + minor_surviving_size) < free_size_threshold) gc->next_collect_force_major = TRUE;  
      if ((mos_free_size + nos_free_size)< free_size_threshold) gc->next_collect_force_major = TRUE;
  
      survive_ratio = (float)minor_surviving_size/(float)space_committed_size((Space*)nos);
      nos->survive_ratio = survive_ratio;
      /*For LOS_Adaptive*/
      POINTER_SIZE_INT mos_committed_size = space_committed_size((Space*)mos);
      POINTER_SIZE_INT nos_committed_size = space_committed_size((Space*)nos);
      if(mos_committed_size  + nos_committed_size > free_size_threshold){
        POINTER_SIZE_INT mos_size_threshold;
        mos_size_threshold = mos_committed_size  + nos_committed_size - free_size_threshold;
        float mos_size_threshold_ratio = (float)mos_size_threshold / (mos_committed_size  + nos_committed_size);
        mspace_set_expected_threshold_ratio((Mspace *)mos, mos_size_threshold_ratio);
      }
    }
  
    gc->survive_ratio =  (gc->survive_ratio + survive_ratio)/2.0f;
    last_total_free_size = total_free_size;
  }

  gc_gen_mode_adapt(gc,pause_time);

  return;
}

/* FIXME:: In this algorithm, it assumes NOS is a full forward space. 
   Semispace GC's NOS has survivor_area. Need careful rethinking. 
   But this algorithm so far can be a good approximation. */
Boolean gc_compute_new_space_size(GC_Gen* gc, POINTER_SIZE_INT* mos_size, POINTER_SIZE_INT* nos_size)
{
  Blocked_Space* nos = (Blocked_Space*)gc->nos;
  Blocked_Space* mos = (Blocked_Space*)gc->mos;
  Space* los = gc->los;  
  
  POINTER_SIZE_INT new_nos_size;
  POINTER_SIZE_INT new_mos_size;

  POINTER_SIZE_INT curr_nos_size = space_committed_size((Space*)nos);
  // POINTER_SIZE_INT used_nos_size = nos_used_space_size((Space*)nos);
  POINTER_SIZE_INT used_mos_size = mos_used_space_size((Space*)mos);

  POINTER_SIZE_INT total_size; /* total_size is no-LOS spaces size */

#ifdef STATIC_NOS_MAPPING
    total_size = max_heap_size_bytes - space_committed_size(los);
#else
    POINTER_SIZE_INT curr_heap_commit_end;
   
    if(LOS_ADJUST_BOUNDARY) {
      curr_heap_commit_end=(POINTER_SIZE_INT)gc->heap_start + LOS_HEAD_RESERVE_FOR_HEAP_BASE + gc->committed_heap_size;
      assert(curr_heap_commit_end > (POINTER_SIZE_INT)mos->heap_start);
      total_size = curr_heap_commit_end - (POINTER_SIZE_INT)mos->heap_start;
    }else {/*LOS_ADJUST_BOUNDARY else */
      curr_heap_commit_end =  (nos->committed_heap_size)? (POINTER_SIZE_INT) nos->heap_start + nos->committed_heap_size: 
               (POINTER_SIZE_INT) mos->heap_start+mos->committed_heap_size;
      total_size = curr_heap_commit_end - (POINTER_SIZE_INT) mos->heap_start;
    }
#endif
  assert(total_size >= used_mos_size);
  POINTER_SIZE_INT total_free = total_size - used_mos_size;
  /*If total free is smaller than one block, there is no room for us to adjust*/
  if(total_free < GC_BLOCK_SIZE_BYTES)  return FALSE;

  POINTER_SIZE_INT nos_reserve_size;
  if( MOS_RESERVE_SIZE == 0){
    /*To reserve some MOS space to avoid fallback situation. 
     *But we need ensure nos has at least one block.
     *We have such fomula here:
     *NOS_SIZE + NOS_SIZE * anti_fall_back_ratio + NOS_SIZE * survive_ratio = TOTAL_FREE*/
    POINTER_SIZE_INT anti_fallback_size_in_mos;
    float ratio_of_anti_fallback_size_to_nos = 0.25f;
    anti_fallback_size_in_mos = (POINTER_SIZE_INT)(((float)total_free * ratio_of_anti_fallback_size_to_nos)/(1.0f + ratio_of_anti_fallback_size_to_nos + nos->survive_ratio));
    if(anti_fallback_size_in_mos > DEFAULT_MOS_RESERVE_SIZE ){
      /*If the computed anti_fallback_size_in_mos is too large, we reset it back to DEFAULT_MOS_RESERVE_SIZE .*/
      anti_fallback_size_in_mos = DEFAULT_MOS_RESERVE_SIZE ;
      /*Here, anti_fallback_size_in_mos must be smaller than TOTAL_FREE*/
      nos_reserve_size = (POINTER_SIZE_INT)(((float)(total_free - anti_fallback_size_in_mos))/(1.0f + nos->survive_ratio)); 
    }else{
      nos_reserve_size = (POINTER_SIZE_INT)(((float)total_free)/(1.0f + ratio_of_anti_fallback_size_to_nos + nos->survive_ratio));
    }
    
  }else{
    nos_reserve_size = total_free - MOS_RESERVE_SIZE;
  }
  
  /*NOS should not be zero, if there is only one block in non-los, i.e. in the former if sentence,
    *if total_free = GC_BLOCK_SIZE_BYTES, then the computed nos_reserve_size is between zero
    *and GC_BLOCK_SIZE_BYTES. In this case, we assign this block to NOS*/
  if(nos_reserve_size <= GC_BLOCK_SIZE_BYTES)  nos_reserve_size = GC_BLOCK_SIZE_BYTES;

#ifdef STATIC_NOS_MAPPING
  if(nos_reserve_size > nos->reserved_heap_size) nos_reserve_size = nos->reserved_heap_size;
#endif  

  new_nos_size = round_down_to_size((POINTER_SIZE_INT)nos_reserve_size, GC_BLOCK_SIZE_BYTES); 

  if(gc->force_gen_mode){
    new_nos_size = min_nos_size_bytes;
  }
 
  new_mos_size = total_size - new_nos_size;
#ifdef STATIC_NOS_MAPPING
  if(new_mos_size > mos->reserved_heap_size) new_mos_size = mos->reserved_heap_size;
#endif

  *nos_size = new_nos_size;
  *mos_size = new_mos_size;
  return TRUE;;
}

#ifndef STATIC_NOS_MAPPING
void gc_gen_adapt(GC_Gen* gc, int64 pause_time)
{
  gc_decide_next_collect(gc, pause_time);

  if(NOS_SIZE) return;

  Blocked_Space* nos = (Blocked_Space*)gc->nos;
  Blocked_Space* mos = (Blocked_Space*)gc->mos;
  
  POINTER_SIZE_INT new_nos_size;
  POINTER_SIZE_INT new_mos_size;

  Boolean result = gc_compute_new_space_size(gc, &new_mos_size, &new_nos_size);

  if(!result) return;

  POINTER_SIZE_INT curr_nos_size = space_committed_size((Space*)nos);

  //if( ABS_DIFF(new_nos_size, curr_nos_size) < NOS_COPY_RESERVE_DELTA )
  if( new_nos_size == curr_nos_size ){
    return;
  }else if ( new_nos_size >= curr_nos_size ){
    INFO2("gc.process", "GC: gc_gen space adjustment after GC["<<gc->num_collections<<"] ...");
    POINTER_SIZE_INT adapt_size = new_nos_size - curr_nos_size;
    INFO2("gc.space", "GC: Space Adapt:  mos  --->  nos  ("
      <<verbose_print_size(adapt_size)
      <<" size was transferred from mos to nos)\n"); 
  } else {
    INFO2("gc.process", "GC: gc_gen space adjustment after GC["<<gc->num_collections<<"] ...");
    POINTER_SIZE_INT  adapt_size = curr_nos_size - new_nos_size;
    INFO2("gc.space", "GC: Space Adapt:  nos  --->  mos  ("
      <<verbose_print_size(adapt_size)
      <<" size was transferred from nos to mos)\n"); 
  }

  /* below are ajustment */  
  POINTER_SIZE_INT curr_heap_commit_end;
  if(LOS_ADJUST_BOUNDARY)
    curr_heap_commit_end = (POINTER_SIZE_INT)gc->heap_start + LOS_HEAD_RESERVE_FOR_HEAP_BASE + gc->committed_heap_size;
  else
    curr_heap_commit_end = (POINTER_SIZE_INT)nos->heap_start + nos->committed_heap_size;
  
  void* new_nos_boundary = (void*)(curr_heap_commit_end - new_nos_size);

  nos_boundary = nos_space_adjust((Space*)nos, new_nos_boundary, new_nos_size);
  /* it's possible that nos can't accept the specified nos boundary, it gives a new one. */
  assert(nos_boundary <= new_nos_boundary); 
  
  new_mos_size -= (POINTER_SIZE_INT)new_nos_boundary - (POINTER_SIZE_INT)nos_boundary;
  blocked_space_adjust(mos, mos->heap_start, new_mos_size);
  
  Block_Header* mos_last_block = (Block_Header*)&mos->blocks[mos->num_managed_blocks-1];
  assert(mos->ceiling_block_idx == mos_last_block->block_idx);
  Block_Header* nos_first_block = (Block_Header*)&nos->blocks[0];
  /* this is redundant, because blocked_space_adjust doesn't set last block next to NULL.
  mos_last_block->next = nos_first_block; */

  if( gc_is_gen_mode())
    HelperClass_set_NosBoundary(nos_boundary);
  
  return;
}

#else /* i.e., #ifdef STATIC_NOS_MAPPING */

void gc_gen_adapt(GC_Gen* gc, int64 pause_time)
{
  gc_decide_next_collect(gc, pause_time);

  if(NOS_SIZE) return;

  POINTER_SIZE_INT new_nos_size;
  POINTER_SIZE_INT new_mos_size;

  Boolean result = gc_compute_new_space_size(gc, &new_mos_size, &new_nos_size);

  if(!result) return;

  Blocked_Space* nos = (Blocked_Space*)gc->nos;
  Blocked_Space* mos = (Blocked_Space*)gc->mos;
  
  POINTER_SIZE_INT curr_nos_size = space_committed_size((Space*)nos);

  //if( ABS_DIFF(new_nos_size, curr_nos_size) < NOS_COPY_RESERVE_DELTA )
  if( new_nos_size == curr_nos_size ){
    return;
  }else if ( new_nos_size >= curr_nos_size ){
    INFO2("gc.process", "GC: gc_gen space adjustment after GC["<<gc->num_collections<<"] ...\n");
    POINTER_SIZE_INT adapt_size = new_nos_size - curr_nos_size;
    INFO2("gc.space", "GC: Space Adapt:  mos  --->  nos  ("
      <<verbose_print_size(adapt_size)
      <<" size was transferred from mos to nos)\n"); 
  } else {
    INFO2("gc.process", "GC: gc_gen space adjustment after GC["<<gc->num_collections<<"] ...\n");
    POINTER_SIZE_INT  adapt_size = curr_nos_size - new_nos_size;
    INFO2("gc.space", "GC: Space Adapt:  nos  --->  mos  ("
      <<verbose_print_size(adapt_size)
      <<" size was transferred from nos to mos)\n"); 
  }
  
  POINTER_SIZE_INT used_mos_size = blocked_space_used_mem_size((Blocked_Space*)mos);
  POINTER_SIZE_INT free_mos_size = blocked_space_free_mem_size((Blocked_Space*)mos);

  POINTER_SIZE_INT new_free_mos_size = new_mos_size -  used_mos_size;
  
  POINTER_SIZE_INT curr_mos_end = (POINTER_SIZE_INT)&mos->blocks[mos->free_block_idx - mos->first_block_idx];
  POINTER_SIZE_INT mos_border = (POINTER_SIZE_INT)mos->heap_end;
  if(  curr_mos_end + new_free_mos_size > mos_border){
    /* we can't let mos cross border */
    new_free_mos_size = mos_border - curr_mos_end;    
  }

  if(new_nos_size < curr_nos_size){
  /* lets shrink nos */
    assert(new_free_mos_size > free_mos_size);
    blocked_space_shrink((Blocked_Space*)nos, curr_nos_size - new_nos_size);
    blocked_space_extend((Blocked_Space*)mos, new_free_mos_size - free_mos_size);
  }else if(new_nos_size > curr_nos_size){
    /* lets grow nos */
    assert(new_free_mos_size < free_mos_size);
    blocked_space_shrink((Blocked_Space*)mos, free_mos_size - new_free_mos_size);
    blocked_space_extend((Blocked_Space*)nos, new_nos_size - curr_nos_size);     
  }

  Block_Header* mos_last_block = (Block_Header*)&mos->blocks[mos->num_managed_blocks-1];
  Block_Header* nos_first_block = (Block_Header*)&nos->blocks[0];
  mos_last_block->next = nos_first_block;
  
  return;
}

#endif /* STATIC_NOS_MAPPING */
