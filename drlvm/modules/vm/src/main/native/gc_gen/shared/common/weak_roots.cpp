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

#include "gc_common.h"
#include "gc_metadata.h"
#include "object_status.h"

void gc_identify_dead_weak_roots(GC *gc)
{
  Pool *weakroot_pool = gc->metadata->weakroot_pool;
  
  pool_iterator_init(weakroot_pool);
  while(Vector_Block *block = pool_iterator_next(weakroot_pool)){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(block);
    for(; !vector_block_iterator_end(block, iter); iter = vector_block_iterator_advance(block, iter)){
      Partial_Reveal_Object** p_ref = (Partial_Reveal_Object**)*iter;
      Partial_Reveal_Object *p_obj = *p_ref;
      if(!p_obj){  // reference has been cleared
        continue;
      }
      assert(p_obj->vt_raw);
      if(collect_is_fallback()) {
          if(obj_belongs_to_nos(p_obj) && obj_is_fw_in_oi(p_obj)){
             assert(!obj_is_marked_in_vt(p_obj));
             assert(obj_get_vt(p_obj) == obj_get_vt(obj_get_fw_in_oi(p_obj)));
             p_obj = obj_get_fw_in_oi(p_obj);
             assert(p_obj);
             *p_ref = p_obj;
          }
      }
      if(gc_obj_is_dead(gc, p_obj))
        *p_ref = 0; 
    }
  }
}

/* parameter pointer_addr_in_pool means it is p_ref or p_obj in pool */
void gc_update_weak_roots(GC *gc, Boolean double_fix)
{
  GC_Metadata* metadata = gc->metadata;
  Pool *weakroot_pool = metadata->weakroot_pool;
  Partial_Reveal_Object** p_ref;
  Partial_Reveal_Object *p_obj;
  
  pool_iterator_init(weakroot_pool);
  while(Vector_Block *repset = pool_iterator_next(weakroot_pool)){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(repset);
    for(; !vector_block_iterator_end(repset,iter); iter = vector_block_iterator_advance(repset,iter)){
      p_ref = (Partial_Reveal_Object**)*iter;
      p_obj = *p_ref;
      if(!p_obj || !obj_need_move(gc, p_obj)){  // reference has been cleared or not moved
        continue;
      }
      /* following code knows p_obj's space is movable. So mark-sweep is not considered below. */
      if( collect_is_compact_move()){ /* move-compact uses offset table */
        if( gc_has_los() && p_obj < los_boundary){
            p_obj = obj_get_fw_in_oi(p_obj);
        }else{ /* this is the case with unique move_compact */
            p_obj = obj_get_fw_in_table(p_obj);
        }

      } else if(collect_is_ms_compact()){ 
        /* ms-compact does not move all live objects, and sometimes need double-fix */
        if(obj_is_fw_in_oi(p_obj)){
          p_obj = obj_get_fw_in_oi(p_obj);
          /* Only major collection in MS Gen GC might need double_fix.
           * Double fixing happens when both forwarding and compaction happen.
           */
          if(double_fix && obj_is_fw_in_oi(p_obj)){
            assert(major_is_marksweep());
            p_obj = obj_get_fw_in_oi(p_obj);
            assert(address_belongs_to_gc_heap(p_obj, gc));
          }
        }
      } else { /* minor collection or slide major compaction */
        assert(obj_is_fw_in_oi(p_obj));
        p_obj = obj_get_fw_in_oi(p_obj);
      }
      
      *p_ref = p_obj;
    }
  }
}
