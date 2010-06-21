
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

#include "interior_pointer.h"
#include <vector>

void gc_add_root_set_entry(Managed_Object_Handle *ref, Boolean is_pinned);

typedef struct slot_offset_entry_struct{
  void** slot;
  unsigned int offset;
  Partial_Reveal_Object *base; 
} slot_offset_entry;

static std::vector<slot_offset_entry> interior_pointer_set;

static const  int initial_vector_size = 100;
static unsigned int interior_pointer_num_count = 0;


void add_root_set_entry_interior_pointer(void **slot, int offset, Boolean is_pinned)
{
  //check size;
  if( interior_pointer_set.size() == interior_pointer_num_count ) 
  {
    int size ;
    if(interior_pointer_num_count == 0){
      size = initial_vector_size ;
      
    }else{
      size = (unsigned int)interior_pointer_set.size()*2;
    }
    interior_pointer_set.resize(size);
  }

  Partial_Reveal_Object* p_obj = (Partial_Reveal_Object*) ((U_8*)*slot - offset);
  assert(p_obj->vt_raw);
  slot_offset_entry* push_back_entry = (slot_offset_entry*)&interior_pointer_set[interior_pointer_num_count++];
  push_back_entry->offset = offset;
  push_back_entry->slot   = slot;
  push_back_entry->base = p_obj;
}

void gc_copy_interior_pointer_table_to_rootset()
{
  unsigned int i;
  for( i = 0; i<interior_pointer_num_count; i++)
  {
    slot_offset_entry* entry_traverser = (slot_offset_entry*)&interior_pointer_set[i];
    gc_add_root_set_entry((Managed_Object_Handle*)(&(entry_traverser->base)), FALSE);
  }
}

void update_rootset_interior_pointer()
{
  unsigned int i;
  for( i = 0; i<interior_pointer_num_count; i++)
  {
    slot_offset_entry* entry_traverser = (slot_offset_entry*)&interior_pointer_set[i];
    void** root_slot = entry_traverser->slot;
    Partial_Reveal_Object* root_base = (Partial_Reveal_Object*)entry_traverser->base;
    unsigned int root_offset = entry_traverser->offset;
    void *new_slot_contents = (void *)((U_8*)root_base + root_offset);  
    *root_slot = new_slot_contents;
  }
  //can not reset the table here, for the rootset may be updated multi times
}

void gc_reset_interior_pointer_table()
{
  interior_pointer_num_count = 0;
  //this function is for the case of out of memory which need to call update_rootset_interior_pointer multi-times
}



