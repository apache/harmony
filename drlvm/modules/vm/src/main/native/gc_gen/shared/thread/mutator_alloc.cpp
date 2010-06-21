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

#include "gc_thread.h"
#include "../gen/gen.h"
#include "../mark_sweep/gc_ms.h"
#include "../move_compact/gc_mc.h"
#include "../finalizer_weakref/finalizer_weakref.h"

#ifdef GC_GEN_STATS
#include "../gen/gen_stats.h"
#endif

//#define GC_OBJ_SIZE_STATISTIC

#ifdef GC_OBJ_SIZE_STATISTIC
#define GC_OBJ_SIZE_STA_MAX 256*KB
unsigned int obj_size_distribution_map[GC_OBJ_SIZE_STA_MAX>>10];
void gc_alloc_statistic_obj_distrubution(unsigned int size)
{
    unsigned int sta_precision = 16*KB;
    unsigned int max_sta_size = 128*KB;
    unsigned int sta_current = 0;    

    assert(!(GC_OBJ_SIZE_STA_MAX % sta_precision));
    assert(!(max_sta_size % sta_precision));    
    while( sta_current < max_sta_size ){
        if(size < sta_current){
            unsigned int index = sta_current >> 10;
            obj_size_distribution_map[index] ++;
            return;
        }
        sta_current += sta_precision;
    }
    unsigned int index = sta_current >> 10;
    obj_size_distribution_map[index]++;
    return;
}
#endif

extern Boolean mutator_need_block;

Managed_Object_Handle gc_alloc(unsigned size, Allocation_Handle ah, void *unused_gc_tls) 
{
  Managed_Object_Handle p_obj = NULL;
 
  /* All requests for space should be multiples of 4 (IA32) or 8(IPF) */
  assert((size % GC_OBJECT_ALIGNMENT) == 0);
  assert(ah);

  size = (size & NEXT_TO_HIGH_BIT_CLEAR_MASK);
  
  Allocator* allocator = (Allocator*)gc_get_tls();
  Boolean type_has_fin = type_has_finalizer((Partial_Reveal_VTable*)decode_vt((VT)ah));
  
  if(type_has_fin && !IGNORE_FINREF && mutator_need_block)
    vm_heavy_finalizer_block_mutator();

#ifdef GC_OBJ_SIZE_STATISTIC
  gc_alloc_statistic_obj_distrubution(size);
#endif

#if defined(USE_UNIQUE_MARK_SWEEP_GC)

  p_obj = (Managed_Object_Handle)gc_ms_alloc(size, allocator);

#elif defined(USE_UNIQUE_MOVE_COMPACT_GC)

  p_obj = (Managed_Object_Handle)gc_mc_alloc(size, allocator);

#else

  if ( size > GC_LOS_OBJ_SIZE_THRESHOLD ){
    p_obj = (Managed_Object_Handle)los_alloc(size, allocator);

#ifdef GC_GEN_STATS
    if (p_obj != NULL){
      GC_Gen* gc = (GC_Gen*)allocator->gc;
      gc->stats->obj_num_los_alloc++;
      gc->stats->total_size_los_alloc += size;
    }
#endif /* #ifdef GC_GEN_STATS */

  }else{
      p_obj = (Managed_Object_Handle)nos_alloc(size, allocator);
  }

#endif /* defined(USE_UNIQUE_MARK_SWEEP_GC) else */

  if( p_obj == NULL )
    return NULL;

  assert((((POINTER_SIZE_INT)p_obj) % GC_OBJECT_ALIGNMENT) == 0);
    
  obj_set_vt((Partial_Reveal_Object*)p_obj, (VT)ah);
  
  if(type_has_fin && !IGNORE_FINREF)
    mutator_add_finalizer((Mutator*)allocator, (Partial_Reveal_Object*)p_obj);
    
  return (Managed_Object_Handle)p_obj;
}


Managed_Object_Handle gc_alloc_fast (unsigned size, Allocation_Handle ah, void *unused_gc_tls) 
{
  /* All requests for space should be multiples of 4 (IA32) or 8(IPF) */
  assert((size % GC_OBJECT_ALIGNMENT) == 0);
  assert(ah);
  
  if(type_has_finalizer((Partial_Reveal_VTable *) decode_vt((VT)ah)))
    return NULL;

#ifdef GC_OBJ_SIZE_STATISTIC
  gc_alloc_statistic_obj_distrubution(size);
#endif
   
  Allocator* allocator = (Allocator*)gc_get_tls();
 
  /* Try to allocate an object from the current Thread Local Block */
  Managed_Object_Handle p_obj;

#if defined(USE_UNIQUE_MARK_SWEEP_GC)

  p_obj = (Managed_Object_Handle)gc_ms_fast_alloc(size, allocator);

#elif defined(USE_UNIQUE_MOVE_COMPACT_GC)

  if ( size > GC_LARGE_OBJ_SIZE_THRESHOLD ) return NULL;
  p_obj = (Managed_Object_Handle)thread_local_alloc(size, allocator);

#else
  /* object shoud be handled specially */
  if ( size > GC_LOS_OBJ_SIZE_THRESHOLD ) return NULL;
  p_obj = (Managed_Object_Handle)thread_local_alloc(size, allocator);

#endif

  if(p_obj == NULL) return NULL;

  assert((((POINTER_SIZE_INT)p_obj) % GC_OBJECT_ALIGNMENT) == 0);
  obj_set_vt((Partial_Reveal_Object*)p_obj, (VT)ah);
  
  return p_obj;
}
