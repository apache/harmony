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
 
#ifndef _VERIFY_MUTATOR_EFFECT_H_
#define _VERIFY_MUTATOR_EFFECT_H_

#include "verifier_common.h"

typedef struct Allocation_Verifier{
  SpinLock alloc_lock;
    
  /*calculated in GC allocation phase.*/
  POINTER_SIZE_INT size_nos_newobjs;
  POINTER_SIZE_INT num_nos_newobjs;
  POINTER_SIZE_INT size_los_newobjs;
  POINTER_SIZE_INT num_los_newobjs;
  
  /*size and number of objects in LOS after last GC*/
  POINTER_SIZE_INT last_size_los_objs;
  POINTER_SIZE_INT last_num_los_objs;
  
  /*calculated in whole heap scanning phase.*/
  POINTER_SIZE_INT size_nos_objs;
  POINTER_SIZE_INT num_nos_objs;
  POINTER_SIZE_INT size_los_objs;
  POINTER_SIZE_INT num_los_objs;
  
  Vector_Block* new_objects_set;
  
  Boolean is_verification_passed;  
}Allocation_Verifier;

typedef struct WriteBarrier_Verifier{
  POINTER_SIZE_INT num_slots_in_remset;
  POINTER_SIZE_INT num_ref_wb_in_remset;
  POINTER_SIZE_INT num_ref_wb_after_scanning;
  Boolean is_verification_passed;
}WriteBarrier_Verifier;

typedef struct RootSet_Verifier{
  POINTER_SIZE_INT num_slots_in_rootset;
  POINTER_SIZE_INT num_error_slots;
  Boolean is_verification_passed;
}RootSet_Verifier;


typedef struct New_Object_struct{
  Partial_Reveal_Object* address;
  POINTER_SIZE_INT size;
  VT vt_raw;
} New_Object;

void verify_write_barrier(REF* p_ref, Heap_Verifier* heap_verifier);
void verify_allocation(Heap_Verifier* heap_verifier);
void verify_root_set(Heap_Verifier* heap_verifier);

void verifier_init_mutator_verifiers(Heap_Verifier* heap_verifier);
void verifier_event_mutator_allocate_newobj(Partial_Reveal_Object* p_newobj, POINTER_SIZE_INT size, VT vt_raw);
void verifier_destruct_mutator_verifiers(Heap_Verifier* heap_verifier);
void verifier_allocation_update_info(Partial_Reveal_Object *p_obj, Heap_Verifier* heap_verifier);

void verifier_reset_mutator_verification(Heap_Verifier* heap_verifier);
void verifier_clear_mutator_verification(Heap_Verifier* heap_verifier);

void verify_mutator_effect(Heap_Verifier* heap_verifier);


#endif //_VERIFY_MUTATOR_EFFECT_H_
