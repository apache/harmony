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

#include "verifier_common.h"
#include "verify_mutator_effect.h"

/*<-----------verify allocation----------------->*/
void verifier_init_allocation_verifier( Heap_Verifier* heap_verifier)
{
  Allocation_Verifier* allocation_verifier = (Allocation_Verifier*)STD_MALLOC(sizeof(Allocation_Verifier));
  assert(allocation_verifier);
  memset(allocation_verifier, 0, sizeof(Allocation_Verifier));
  allocation_verifier->size_nos_newobjs = allocation_verifier->num_nos_newobjs = 0;
  allocation_verifier->size_los_newobjs = allocation_verifier->size_los_newobjs = 0;
  allocation_verifier->size_nos_objs = allocation_verifier->num_nos_objs = 0;
  allocation_verifier->size_los_objs = allocation_verifier->num_los_objs = 0;
  allocation_verifier->last_size_los_objs = allocation_verifier->last_num_los_objs = 0;
  allocation_verifier->new_objects_set = NULL;
  allocation_verifier->new_objects_set 
      = verifier_free_set_pool_get_entry(heap_verifier->heap_verifier_metadata->free_set_pool);
  heap_verifier->allocation_verifier = allocation_verifier;
}

void verifier_destruct_allocation_verifier(Heap_Verifier* heap_verifier)
{
  //assert(heap_verifier->allocation_verifier->new_objects_set == NULL);
  STD_FREE(heap_verifier->allocation_verifier);
  heap_verifier->allocation_verifier = NULL;
}

void verify_allocation_reset(Heap_Verifier* heap_verifier)
{
  Allocation_Verifier* alloc_verifier = heap_verifier->allocation_verifier;
  GC_Gen* gc    = (GC_Gen*)heap_verifier->gc;
  Space* lspace  = gc_get_los(gc);

  alloc_verifier->size_los_objs = alloc_verifier->num_los_objs = 0;

  verifier_scan_los_objects(lspace, heap_verifier);
  
  alloc_verifier->last_size_los_objs = alloc_verifier->size_los_objs;
  alloc_verifier->last_num_los_objs = alloc_verifier->num_los_objs;
  
  alloc_verifier->size_nos_newobjs = alloc_verifier->num_nos_newobjs = 0;
  alloc_verifier->size_los_newobjs = alloc_verifier->num_los_newobjs = 0;
  alloc_verifier->size_nos_objs = alloc_verifier->num_nos_objs = 0;
  alloc_verifier->size_los_objs = alloc_verifier->num_los_objs = 0;
  
  assert(alloc_verifier->new_objects_set == NULL);
  alloc_verifier->new_objects_set = verifier_free_set_pool_get_entry(verifier_metadata->free_set_pool);
  assert(alloc_verifier->new_objects_set);

}

Boolean verify_new_object(New_Object* new_obj, Heap_Verifier* heap_verifier)
{
  if(!heap_verifier->need_verify_allocation) return TRUE;
  
  GC_Gen* gc    =(GC_Gen*) heap_verifier->gc;
  Space* mspace = gc_get_mos(gc);
  assert(mspace);
  
  if(obj_belongs_to_space(new_obj->address, mspace)){
    assert(0);
    printf("GC Verify ==> Verify Allocation: new Objects in MOS...\n ");
    return FALSE;
  }
  
  Partial_Reveal_Object* p_newobj = new_obj->address;
  if(obj_get_vt_raw(p_newobj) != new_obj->vt_raw){
    assert(0);
    printf("GC Verify ==> Verify Allocation: new Objects Vtable Error...\n ");
    return FALSE;
  }
  
  if(vm_object_size(p_newobj) != new_obj->size){
    assert(0);
    printf("GC Verify ==> Verify Allocation: new Objects size Error...\n ");
    return FALSE;
  }
  
  return TRUE;
}

void verify_allocation(Heap_Verifier* heap_verifier)
{
  Allocation_Verifier* alloc_verifier = heap_verifier->allocation_verifier;
  Heap_Verifier_Metadata* verifier_metadata = heap_verifier->heap_verifier_metadata;
  pool_put_entry(verifier_metadata->new_objects_pool, alloc_verifier->new_objects_set);
  alloc_verifier->new_objects_set = NULL;
  
  GC_Gen* gc    = (GC_Gen*)heap_verifier->gc;
  Space* nspace = gc_get_nos(gc);
  Space* lspace  = gc_get_los(gc);
  assert(nspace);
  assert(lspace);
  
  POINTER_SIZE_INT size_los_newobjs 
                        = alloc_verifier->size_los_objs - alloc_verifier->last_size_los_objs;
  POINTER_SIZE_INT num_los_newobjs 
                        = alloc_verifier->num_los_objs - alloc_verifier->last_num_los_objs;

  assert(alloc_verifier->size_nos_objs == alloc_verifier->size_nos_newobjs);
  assert(alloc_verifier->num_nos_objs == alloc_verifier->num_nos_newobjs);
  if(alloc_verifier->size_nos_objs != alloc_verifier->size_nos_newobjs){
    printf("GC Verify ==> Verify Allocation: NOS new objects size error.\n"); 
    alloc_verifier->is_verification_passed = FALSE;
  }
  if(alloc_verifier->num_nos_objs != alloc_verifier->num_nos_newobjs){
    printf("GC Verify ==> Verify Allocation: NOS new objects number error.\n"); 
    alloc_verifier->is_verification_passed = FALSE;
  }
  
  assert(size_los_newobjs == alloc_verifier->size_los_newobjs);
  assert(num_los_newobjs == alloc_verifier->num_los_newobjs);
  if(size_los_newobjs != alloc_verifier->size_los_newobjs){
    printf("GC Verify ==> Verify Allocation: LOS new objects size error.\n"); 
    alloc_verifier->is_verification_passed = FALSE;
  }
  if(num_los_newobjs != alloc_verifier->num_los_newobjs){
    printf("GC Verify ==> Verify Allocation: LOS new objects number error.\n"); 
    alloc_verifier->is_verification_passed = FALSE;
  }

  
  assert(alloc_verifier->new_objects_set == NULL);
  Vector_Block* new_objects = pool_get_entry(verifier_metadata->new_objects_pool);
  while(new_objects){
    POINTER_SIZE_INT* iter = vector_block_iterator_init(new_objects);
    while(!vector_block_iterator_end(new_objects, iter)){
      New_Object* p_newobj = (New_Object*) *iter;
      iter = vector_block_iterator_advance(new_objects, iter);
      if(!verify_new_object(p_newobj, heap_verifier)){
        assert(0);
        printf("GC Verify ==> Verify Allocation: new objects verify error.\n");
        alloc_verifier->is_verification_passed = FALSE;
      }
      STD_FREE(p_newobj);
    }
    vector_block_clear(new_objects);
    pool_put_entry(verifier_metadata->free_set_pool, new_objects);
    new_objects = pool_get_entry(verifier_metadata->new_objects_pool);
  }
}



void verifier_allocation_update_info(Partial_Reveal_Object *p_obj, Heap_Verifier* heap_verifier)
{
  if(!heap_verifier->need_verify_allocation) return;
  
  Allocation_Verifier* alloc_verifier = heap_verifier->allocation_verifier;
  GC_Gen* gc  =  (GC_Gen*)heap_verifier->gc;
  Space* nspace   = gc_get_nos(gc);
  Space* mspace  = gc_get_mos(gc); 
  Space* lspace    = gc_get_los(gc);
  assert(nspace);
  assert(lspace);
  
  if(obj_belongs_to_space(p_obj, nspace)){
    alloc_verifier->size_nos_objs += vm_object_size(p_obj); 
    alloc_verifier->num_nos_objs ++;
    return;
  }else if(obj_belongs_to_space(p_obj, lspace)){
    alloc_verifier->size_los_objs += vm_object_size(p_obj); 
    alloc_verifier->num_los_objs ++;
    return;
  }else if(obj_belongs_to_space(p_obj, mspace)){
    return;
  }
  assert(0);
}

void verifier_event_mutator_allocate_newobj(Partial_Reveal_Object* p_newobj, POINTER_SIZE_INT size, VT vt_raw)
{
  Heap_Verifier* heap_verifier = get_heap_verifier();
  if(!heap_verifier->need_verify_allocation) return;

  assert(p_newobj);
  assert(obj_get_vt(p_newobj));
  assert(obj_get_vt(p_newobj) == vt_raw);
  
  New_Object* new_obj = (New_Object*) STD_MALLOC(sizeof(New_Object));
  assert(new_obj);
  new_obj->address  = p_newobj;
  new_obj->size         = size;
  new_obj->vt_raw     = (VT) vt_raw;
  
  Heap_Verifier_Metadata* verifier_metadata = heap_verifier->heap_verifier_metadata;
  Allocation_Verifier* alloc_verifier = heap_verifier->allocation_verifier;
  
  /*FIXME: Replace lock */
  lock(alloc_verifier->alloc_lock);
  
  verifier_set_push((void*)new_obj, alloc_verifier->new_objects_set, verifier_metadata->new_objects_pool);
  
  GC_Gen* gc    =(GC_Gen*) heap_verifier->gc;
  Space* nspace = gc_get_nos(gc);
  Space* lspace  = gc_get_los(gc);

  //FIXME: 
  //assert(size == vm_object_size(p_newobj));
  if(obj_belongs_to_space(p_newobj, nspace)){
    alloc_verifier->size_nos_newobjs += size;
    alloc_verifier->num_nos_newobjs ++;
  }else if (obj_belongs_to_space(p_newobj, lspace)){
    alloc_verifier->size_los_newobjs += size;
    alloc_verifier->num_los_newobjs ++;
  }else{
    assert(0);
    alloc_verifier->is_verification_passed = FALSE;

  }
  unlock(alloc_verifier->alloc_lock);
}

/*<-----------verify root sets----------------->*/
/*FIXME: where should root set verifier be placed*/
/*The rootset set verifier is placed here, so the rootset verifier
    verifys the rootset that vm enumerated before GC. The rootset 
    verifying processes before and after gc will be integrated in  gc verifying pass,
    the rootset verifying is considered as slot verifying while verifying heap. */
void verifier_init_rootset_verifier( Heap_Verifier* heap_verifier)
{
  RootSet_Verifier* rs_verifier = (RootSet_Verifier*)STD_MALLOC(sizeof(RootSet_Verifier));
  assert(rs_verifier);
  memset(rs_verifier, 0, sizeof(RootSet_Verifier));
  rs_verifier->num_slots_in_rootset = 0;
  rs_verifier->num_error_slots = 0;
  rs_verifier->is_verification_passed = FALSE;
  heap_verifier->rootset_verifier = rs_verifier;
}
 

void verifier_destruct_rootset_verifier( Heap_Verifier* heap_verifier)
{
  assert(heap_verifier != NULL);
  assert(heap_verifier->rootset_verifier != NULL);
  STD_FREE(heap_verifier->rootset_verifier);
  heap_verifier->rootset_verifier = NULL;
}

void verifier_reset_rootset_verifier( Heap_Verifier* heap_verifier)
{
  RootSet_Verifier* rootset_verifier = heap_verifier->rootset_verifier;
  rootset_verifier->num_slots_in_rootset = 0;
  rootset_verifier->num_error_slots = 0;
}

void verify_root_set(Heap_Verifier* heap_verifier)
{
  assert(heap_verifier);
  GC_Gen* gc    = (GC_Gen*)heap_verifier->gc;
  GC_Metadata* gc_metadata = gc->metadata;

  assert(gc);
  assert(gc_metadata);
  assert(gc_metadata->gc_rootset_pool);

  RootSet_Verifier* rootset_verifier = heap_verifier->rootset_verifier;
  
  pool_iterator_init(gc_metadata->gc_rootset_pool);
  Vector_Block* root_set = pool_iterator_next(gc_metadata->gc_rootset_pool);
  
  /* first step: copy all root objects to trace tasks. */ 
  while(root_set){
    POINTER_SIZE_INT* iter = vector_block_iterator_init(root_set);
    while(!vector_block_iterator_end(root_set,iter)){
      REF* p_ref = (REF* )*iter;
      iter = vector_block_iterator_advance(root_set,iter);
      rootset_verifier->num_slots_in_rootset ++;
      if(!verify_rootset_slot(p_ref, heap_verifier)){
        rootset_verifier->is_verification_passed = FALSE;
        rootset_verifier->num_error_slots ++;
        assert(0);
        continue;
      }
    } 
    root_set = pool_iterator_next(gc_metadata->gc_rootset_pool);
  }
}



/*<-----------verify write barrier----------------->*/

void verifier_init_wb_verifier( Heap_Verifier* heap_verifier)
{
  WriteBarrier_Verifier* wb_verifier = (WriteBarrier_Verifier*) STD_MALLOC(sizeof(WriteBarrier_Verifier));
  assert(wb_verifier);
  memset(wb_verifier, 0, sizeof(WriteBarrier_Verifier));
  wb_verifier->num_ref_wb_after_scanning = 0;
  wb_verifier->num_ref_wb_in_remset = 0;
  wb_verifier->num_slots_in_remset = 0;
  wb_verifier->is_verification_passed = FALSE;
  heap_verifier->writebarrier_verifier = wb_verifier;
}
 

void verifier_destruct_wb_verifier( Heap_Verifier* heap_verifier)
{
  assert(heap_verifier != NULL);
  assert(heap_verifier->writebarrier_verifier != NULL);
  STD_FREE(heap_verifier->writebarrier_verifier);
  heap_verifier->writebarrier_verifier = NULL;
}

void verifier_reset_wb_verifier(Heap_Verifier* heap_verifier)
{
  WriteBarrier_Verifier* wb_verifier = heap_verifier->writebarrier_verifier;
  wb_verifier->num_ref_wb_after_scanning = 0;
  wb_verifier->num_ref_wb_in_remset = 0;
  wb_verifier->num_slots_in_remset = 0;
}

void verifier_mark_wb_slots(Heap_Verifier* heap_verifier)
{
  GC_Gen* gc = (GC_Gen*)(heap_verifier->gc);
  if(collect_is_major() ||!gc_is_gen_mode()) return;

  GC_Metadata*gc_metadata = gc->metadata;
  Space* nspace  = gc_get_nos(gc);

  WriteBarrier_Verifier* wb_verifier = heap_verifier->writebarrier_verifier;
  assert(wb_verifier);
  
  pool_iterator_init(gc_metadata->gc_rootset_pool);
  Vector_Block* rem_set = pool_iterator_next(gc_metadata->gc_rootset_pool);

    
  while(rem_set){
    if(rem_set == gc->root_set) break;
    POINTER_SIZE_INT* iter = vector_block_iterator_init(rem_set);
    while(!vector_block_iterator_end(rem_set, iter)){
      REF* p_ref = (REF* )*iter;
      wb_verifier->num_slots_in_remset ++;
      if(!address_belongs_to_space((void*)p_ref, nspace) && address_belongs_to_space(read_slot(p_ref), nspace)){
        if(!wb_is_marked_in_slot(p_ref)){
          wb_mark_in_slot(p_ref);
          wb_verifier->num_ref_wb_in_remset ++;
        }
      }
      iter = vector_block_iterator_advance(rem_set, iter);
    }
    rem_set = pool_iterator_next(gc_metadata->gc_rootset_pool);
  }  
}

void verify_write_barrier(REF* p_ref, Heap_Verifier* heap_verifier)
{
  GC_Gen* gc    = (GC_Gen*)heap_verifier->gc;
  if(collect_is_major() ||!gc_is_gen_mode()) return;
  
  Space* nspace  = gc_get_nos(gc);
  assert(address_belongs_to_gc_heap((void*)p_ref, (GC *) gc));
  
  WriteBarrier_Verifier* wb_verifier = heap_verifier->writebarrier_verifier;
  assert(wb_verifier);

  if(!address_belongs_to_space((void*)p_ref, nspace) && address_belongs_to_space(read_slot(p_ref), nspace)){
    if(!wb_is_marked_in_slot(p_ref)){
      assert(0);
      printf("GC Verify ==> Verify Write Barrier: unbuffered error!!!\n");
      wb_verifier->is_verification_passed = FALSE;
    }else{
      wb_unmark_in_slot(p_ref);
      wb_verifier->num_ref_wb_after_scanning ++;
    }
    return;
  }else{
    if(wb_is_marked_in_slot(p_ref)){
      assert(0);
      printf("GC Verify ==>Verify Write Barrier: buffered error!!!\n");
    }
    return;
  }
}

/*<------------verify mutator effect common--------------->*/

void verifier_init_mutator_verifiers( Heap_Verifier* heap_verifier)
{
  verifier_init_allocation_verifier(heap_verifier);
  verifier_init_wb_verifier(heap_verifier);
  verifier_init_rootset_verifier(heap_verifier);
}
 

void verifier_destruct_mutator_verifiers( Heap_Verifier* heap_verifier)
{
  verifier_destruct_allocation_verifier(heap_verifier);
  verifier_destruct_wb_verifier(heap_verifier);
  verifier_destruct_rootset_verifier(heap_verifier);
}

void verifier_reset_mutator_verification(Heap_Verifier* heap_verifier)
{
  heap_verifier->allocation_verifier->is_verification_passed = TRUE;
  heap_verifier->writebarrier_verifier->is_verification_passed = TRUE;
  heap_verifier->rootset_verifier->is_verification_passed = TRUE;
  verifier_reset_wb_verifier(heap_verifier);
  verifier_reset_rootset_verifier(heap_verifier);
  if(heap_verifier->need_verify_writebarrier && heap_verifier->gc_is_gen_mode)
    verifier_mark_wb_slots(heap_verifier);

}


void verifier_clear_mutator_verification(Heap_Verifier* heap_verifier)
{
  if(heap_verifier->need_verify_allocation) verify_allocation_reset(heap_verifier);  
}


void verify_mutator_effect(Heap_Verifier* heap_verifier)
{
  if(heap_verifier->need_verify_rootset)  verify_root_set(heap_verifier);
  if(heap_verifier->need_verify_allocation)  verify_allocation(heap_verifier);
}
 





