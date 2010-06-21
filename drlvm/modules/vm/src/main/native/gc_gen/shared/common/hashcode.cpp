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

#include "hashcode.h"
#include "gc_metadata.h"

/* <-- processing of hashcode in different GC algorithms */
Obj_Info_Type slide_compact_process_hashcode(Partial_Reveal_Object* p_obj, void* dest_addr, 
                                                unsigned int* p_obj_size, Collector* collector, 
                                                Hashcode_Buf* old_buf, Hashcode_Buf* new_buf)
{
  Obj_Info_Type obj_info = get_obj_info(p_obj);
  POINTER_SIZE_INT hashcode = 0;

  switch(obj_info & HASHCODE_MASK){
    case HASHCODE_SET_UNALLOCATED:
      if((POINTER_SIZE_INT)dest_addr != (POINTER_SIZE_INT)p_obj){
        *p_obj_size += GC_OBJECT_ALIGNMENT; 
        obj_info = obj_info | HASHCODE_ATTACHED_BIT;
        hashcode = (POINTER_SIZE_INT)hashcode_gen(p_obj);
        POINTER_SIZE_INT obj_end_pos = (POINTER_SIZE_INT)dest_addr + vm_object_size(p_obj);
        collector_hashcodeset_add_entry(collector, (Partial_Reveal_Object**)obj_end_pos);
        collector_hashcodeset_add_entry(collector, (Partial_Reveal_Object**)hashcode);
      } 
      break;
      
    case HASHCODE_SET_ATTACHED:
      obj_sethash_in_vt(p_obj);
      break;
      
    case HASHCODE_SET_BUFFERED:
      hashcode = (POINTER_SIZE_INT)hashcode_buf_lookup(p_obj, old_buf);
      if((POINTER_SIZE_INT)dest_addr != (POINTER_SIZE_INT)p_obj){
        *p_obj_size += GC_OBJECT_ALIGNMENT; 
        obj_info = obj_info & ~HASHCODE_BUFFERED_BIT;
        obj_info = obj_info | HASHCODE_ATTACHED_BIT;
        POINTER_SIZE_INT obj_end_pos = (POINTER_SIZE_INT)dest_addr + vm_object_size(p_obj);
        collector_hashcodeset_add_entry(collector, (Partial_Reveal_Object**)obj_end_pos);
        collector_hashcodeset_add_entry(collector, (Partial_Reveal_Object**)hashcode);
      }else{
        hashcode_buf_add((Partial_Reveal_Object*)dest_addr, (I_32)hashcode, new_buf);          
      }
      break;
      
    case HASHCODE_UNSET:
      break;
      
    default:
      assert(0);
  
  }
  return obj_info;
}

void move_compact_process_hashcode(Partial_Reveal_Object* p_obj,Hashcode_Buf* old_buf,  
                                           Hashcode_Buf* new_buf)
{
  if(hashcode_is_set(p_obj) && !hashcode_is_attached(p_obj)){
    int hashcode;
    if(hashcode_is_buffered(p_obj)){
      /*already buffered objects;*/
      hashcode = hashcode_buf_lookup(p_obj, old_buf);
      hashcode_buf_add(p_obj, hashcode, new_buf);
    }else{
      /*objects need buffering.*/
      hashcode = hashcode_gen(p_obj);
      hashcode_buf_add(p_obj, hashcode, new_buf);
      Obj_Info_Type oi = get_obj_info_raw(p_obj);
      set_obj_info(p_obj, oi | HASHCODE_BUFFERED_BIT);
    }
  }
}

/* processing of hashcode in different GC algorithms --> */




