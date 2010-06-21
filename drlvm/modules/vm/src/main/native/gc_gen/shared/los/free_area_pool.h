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
 * @author Ji Qi, 2006/10/05
 */

#ifndef _BUDDY_H_
#define _BUDDY_H_

#include "../common/gc_common.h"
#include "../utils/bit_ops.h"
#include "../utils/bidir_list.h"

#define ADDRESS_IS_KB_ALIGNED(addr) (!(((POINTER_SIZE_INT)addr) & ((1 << BIT_SHIFT_TO_KILO)-1)))
#define ALIGN_UP_TO_KILO(addr) (((POINTER_SIZE_INT)(addr) + (KB - 1)) & (~(KB- 1)))
#define ALIGN_DOWN_TO_KILO(addr) ((POINTER_SIZE_INT)(addr) & (~(KB- 1)))

#define NUM_FREE_LIST 128

typedef struct Lockable_Bidir_List{
  /* <-- First couple of fields overloadded as Bidir_List */
  POINTER_SIZE_INT zero;
  Bidir_List* next;
  Bidir_List* prev;
  /* END of Bidir_List --> */
  SpinLock lock;  
}Lockable_Bidir_List;

typedef struct Free_Area{
  /* <-- First couple of fields overloadded as Bidir_List */
  POINTER_SIZE_INT zero;
  Bidir_List* next;
  Bidir_List* prev;
  /* END of Bidir_List --> */
  POINTER_SIZE_INT size;
}Free_Area;

/* this is the only interface for new area creation. If the new area size is smaller than threshold, return NULL*/
inline Free_Area* free_area_new(void* start, POINTER_SIZE_INT size)
{
  assert(ADDRESS_IS_KB_ALIGNED(start));
  assert(ADDRESS_IS_KB_ALIGNED(size));

  Free_Area* area = (Free_Area*)start;
  area->zero = 0;
  area->next = area->prev = (Bidir_List*)area;
  area->size = size;
  
  if( size < GC_LOS_OBJ_SIZE_THRESHOLD) return NULL;
  else return area;
}

#define NUM_FLAG_WORDS (NUM_FREE_LIST >> BIT_SHIFT_TO_BITS_PER_WORD)

typedef struct Free_Area_Pool{
  Lockable_Bidir_List sized_area_list[NUM_FREE_LIST];
  /* each list corresponds to one bit in below vector */
  POINTER_SIZE_INT list_bit_flag[NUM_FLAG_WORDS];
}Free_Area_Pool;

#define MAX_LIST_INDEX (NUM_FREE_LIST - 1)

inline void pool_list_set_flag(Free_Area_Pool* pool, unsigned int index)
{
  words_set_bit(pool->list_bit_flag, NUM_FLAG_WORDS, index);
}

inline void pool_list_clear_flag(Free_Area_Pool* pool, unsigned int index)
{
  words_clear_bit(pool->list_bit_flag, NUM_FLAG_WORDS, index);
}

inline unsigned int pool_list_get_next_flag(Free_Area_Pool* pool, unsigned int start_idx)
{
  return words_get_next_set_lsb(pool->list_bit_flag, NUM_FLAG_WORDS, start_idx);
}

inline unsigned int pool_list_index_with_size(POINTER_SIZE_INT size)
{
  assert(size >= GC_LOS_OBJ_SIZE_THRESHOLD);
  
  unsigned int index;
  index = (unsigned int) (size >> BIT_SHIFT_TO_KILO);
  if(index > MAX_LIST_INDEX) index = MAX_LIST_INDEX;
  return index;
}

inline Free_Area* free_pool_add_area(Free_Area_Pool* pool, Free_Area* free_area)
{
  assert( free_area->size >= GC_LOS_OBJ_SIZE_THRESHOLD);
  
  unsigned int index = pool_list_index_with_size(free_area->size);
  bidir_list_add_item((Bidir_List*)&(pool->sized_area_list[index]), (Bidir_List*)free_area);
  
  /* set bit flag of the list */
  pool_list_set_flag(pool, index);
  return free_area;
}

inline void free_pool_remove_area(Free_Area_Pool* pool, Free_Area* free_area)
{
  unsigned int index = pool_list_index_with_size(free_area->size);
  bidir_list_remove_item((Bidir_List*)free_area);
  
  /* set bit flag of the list */
  Bidir_List* list = (Bidir_List*)&(pool->sized_area_list[index]);
  if(list->next == list){
    pool_list_clear_flag(pool, index);    
  }
}

void free_area_pool_init(Free_Area_Pool* p_buddy);
void free_area_pool_reset(Free_Area_Pool* p_buddy);
Free_Area* free_pool_find_size_area(Free_Area_Pool* pool, unsigned int size);

#endif /*ifdef _BUDDY_H_*/
