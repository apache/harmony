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

#ifndef _VERIFY_GC_EFFECT_H_
#define _VERIFY_GC_EFFECT_H_

#include "verifier_common.h"

typedef struct GC_Verifier{
  Vector_Block* trace_stack;
  Vector_Block* root_set;
  Vector_Block* objects_set;
  Vector_Block* hashcode_set;

  Boolean is_tracing_resurrect_obj;
  unsigned int collect_kind;
  Boolean is_before_fallback_collection;
  
  POINTER_SIZE_INT num_live_objects_before_gc;
  POINTER_SIZE_INT num_live_objects_after_gc;
  POINTER_SIZE_INT size_live_objects_before_gc;
  POINTER_SIZE_INT size_live_objects_after_gc;
  
  POINTER_SIZE_INT num_resurrect_objects_before_gc;
  POINTER_SIZE_INT num_resurrect_objects_after_gc;
  POINTER_SIZE_INT size_resurrect_objects_before_gc;
  POINTER_SIZE_INT size_resurrect_objects_after_gc;

  POINTER_SIZE_INT num_hash_buffered_before_gc;
  POINTER_SIZE_INT num_hash_buffered_after_gc;
  POINTER_SIZE_INT num_hash_attached_before_gc;
  POINTER_SIZE_INT num_hash_attached_after_gc;
  POINTER_SIZE_INT num_hash_set_unalloc_before_gc;
  POINTER_SIZE_INT num_hash_set_unalloc_after_gc;

  POINTER_SIZE_INT num_hash_before_gc;
  POINTER_SIZE_INT num_hash_after_gc;

 
  Boolean is_verification_passed;
}GC_Verifier;

#define OBJ_INFO_MASK (~0x3ff)

typedef struct Live_Object_Inform_struct{
  VT vt_raw;
  Partial_Reveal_Object* address;
  Obj_Info_Type obj_info;
} Live_Object_Inform;

typedef struct Live_Object_Ref_Slot_Inform_Struct{
  VT vt_raw;
  Partial_Reveal_Object* address;
  Obj_Info_Type obj_info;
  VT ref_slot[1];
} Live_Object_Ref_Slot_Inform;

typedef struct Object_Hashcode_Inform_struct{
  int hashcode;
  Partial_Reveal_Object* address;
  POINTER_SIZE_INT hash_obj_distance;
}Object_Hashcode_Inform;

void verifier_init_GC_verifier(Heap_Verifier* heap_verifier);
void verifier_destruct_GC_verifier(Heap_Verifier* heap_verifier);
void verifier_reset_gc_verification(Heap_Verifier* heap_verifier);
void verifier_clear_gc_verification(Heap_Verifier* heap_verifier);

void verifier_update_verify_info(Partial_Reveal_Object* p_obj, Heap_Verifier* heap_verifier);
void verify_live_finalizable_obj(Heap_Verifier* heap_verifier, Pool* live_finalizable_objs_pool);
void verifier_clear_objs_mark_bit(Heap_Verifier* heap_verifier);


void verifier_update_info_before_resurrect(Heap_Verifier* heap_verifier);
void verifier_update_info_after_resurrect(Heap_Verifier* heap_verifier);

void verify_gc_effect(Heap_Verifier* heap_verifier);

void verifier_reset_hash_distance();

inline void verifier_set_gc_collect_kind(GC_Verifier* gc_verifier, unsigned int collect_kind)
{  gc_verifier->collect_kind = collect_kind;  }

inline Boolean verifier_collect_is_minor(GC_Verifier* gc_verifier)
{
  return (gc_verifier->collect_kind & ALGO_MAJOR) == 0; 
}

inline void verifier_set_fallback_collection(GC_Verifier* gc_verifier, Boolean is_before_fallback)
{  gc_verifier->is_before_fallback_collection = is_before_fallback;  }

#endif




