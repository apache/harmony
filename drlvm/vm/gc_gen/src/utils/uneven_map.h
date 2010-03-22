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

#ifndef _UNEVEN_MAP_H_
#define _UNEVEN_MAP_H_

#include "../common/gc_metadata.h"

#define MAP_ARRAY_SIZE 512
#define MAP_VECTOR_ENTRY_SHIFT 8
#define MAP_VECTOR_ENTRY_NUM (1<<MAP_VECTOR_ENTRY_SHIFT)

typedef Vector_Block* Uneven_Map[MAP_ARRAY_SIZE];

extern GC_Metadata gc_metadata;

FORCE_INLINE void to_from_hash_insert(Uneven_Map* map, unsigned int to_block_index, unsigned int from_block_index)
{
  unsigned int vector_block_index = to_block_index >> MAP_VECTOR_ENTRY_SHIFT;
  unsigned int vector_block_entry = to_block_index & (MAP_VECTOR_ENTRY_NUM - 1);
  Vector_Block *vector_block = (*map)[vector_block_index];
  if(vector_block == NULL){    
    Vector_Block *new_vector_block = free_set_pool_get_entry(&gc_metadata);
    assert(new_vector_block);
    vector_block_set_zero(new_vector_block);
    (*map)[vector_block_index] = new_vector_block;
    vector_block = new_vector_block;
  }
  assert(vector_block_entry < MAP_VECTOR_ENTRY_NUM);
  vector_block_set_at_index(vector_block, vector_block_entry, (POINTER_SIZE_INT)from_block_index);
}

FORCE_INLINE unsigned int uneven_map_get(Uneven_Map* map, unsigned int to_block_index)
{
  unsigned int vector_block_index = to_block_index >> MAP_VECTOR_ENTRY_SHIFT;
  unsigned int vector_block_entry = to_block_index & (MAP_VECTOR_ENTRY_NUM - 1);
  Vector_Block *vector_block = (*map)[vector_block_index];
  assert(vector_block_entry < MAP_VECTOR_ENTRY_NUM);

  if(vector_block)
    return (unsigned int)vector_block_get_at_index(vector_block, vector_block_entry);

  return 0;
}

FORCE_INLINE void uneven_map_free(Uneven_Map* map)
{
  Vector_Block *block;
  for(unsigned int i = 0; i<MAP_ARRAY_SIZE; i++){
    block = (*map)[i];
    if(block){      
      free_set_pool_put_entry(block, &gc_metadata);
      (*map)[i] = NULL;  
    }
  }
}

#endif
