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

#include "free_area_pool.h"

void free_area_pool_init(Free_Area_Pool* pool)
{
  for(unsigned int i = 0; i < NUM_FREE_LIST; i ++){
    Bidir_List* list = (Bidir_List*)(&pool->sized_area_list[i]);
    list->next = list->prev = list;
    ((Lockable_Bidir_List*)list)->lock = 0;
    ((Lockable_Bidir_List*)list)->zero = 0;
  }
  
  memset((void*)pool->list_bit_flag, 0, NUM_FLAG_WORDS << BIT_SHIFT_TO_BYTES_PER_WORD);
  return;
}

void free_area_pool_reset(Free_Area_Pool* pool)
{
  free_area_pool_init(pool);
}

Free_Area* free_pool_find_size_area(Free_Area_Pool* pool, POINTER_SIZE_INT size)
{
  assert(size >= GC_LOS_OBJ_SIZE_THRESHOLD);
  
  size = ALIGN_UP_TO_KILO(size);
  unsigned int index = pool_list_index_with_size(size);
  /* Get first list index that is not empty */
  index = pool_list_get_next_flag(pool, index);
  assert(index <= NUM_FREE_LIST);
  
  /*No free area left*/
  if(index == NUM_FREE_LIST) 
  return NULL; 
  
  Bidir_List* list = (Bidir_List*)&pool->sized_area_list[index];
  Free_Area* area = (Free_Area*)list->next;
  
  if(index != MAX_LIST_INDEX)
  return area;
  
  /* Else, for last bucket MAX_LIST_INDEX, we must traverse it */
  while(  area != (Free_Area*)list ){
    if(area->size >= size)  return area;
    area = (Free_Area*)(area->next);
  }
  
  return NULL;
}
