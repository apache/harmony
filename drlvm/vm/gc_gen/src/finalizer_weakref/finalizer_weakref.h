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
 * @author Li-Gang Wang, 2006/11/30
 */

#ifndef _FINREF_H_
#define _FINREF_H_

//#define BUILD_IN_REFERENT

#include "finalizer_weakref_metadata.h"
#include "../thread/collector.h"

extern Boolean IGNORE_FINREF;

/* Phanref status: for future use
 * #define PHANTOM_REF_ENQUEUE_STATUS_MASK 0x3
 * #define PHANTOM_REF_ENQUEUED_MASK 0x1
 * #define PHANTOM_REF_PENDING_MASK 0x2
 *
 * inline Partial_Reveal_Object *get_reference_pointer(Partial_Reveal_Object *p_obj)
 * {
 *   return (Partial_Reveal_Object *)((unsigned int)(p_obj)&(~PHANTOM_REF_ENQUEUE_STATUS_MASK));
 * }
 * inline void update_reference_pointer(Partial_Reveal_Object **p_ref, Partial_Reveal_Object *p_target_obj)
 * {
 *   unsigned int temp = (unsigned int)*p_ref;
 * 
 *   temp &= PHANTOM_REF_ENQUEUE_STATUS_MASK;
 *   temp |= (unsigned int)p_target_obj;
 *   *p_ref = (Partial_Reveal_Object *)temp;
 * }
 */

inline REF *obj_get_referent_field(Partial_Reveal_Object *p_obj)
{
  assert(p_obj);
  return (REF*)((U_8*)p_obj+get_gc_referent_offset());
}

extern Boolean DURING_RESURRECTION;
typedef void (* Scan_Slot_Func)(Collector *collector, REF *p_ref);
inline void scan_weak_reference(Collector *collector, Partial_Reveal_Object *p_obj, Scan_Slot_Func scan_slot)
{
  WeakReferenceType type = special_reference_type(p_obj);
  if(type == NOT_REFERENCE)
    return;
  REF *p_referent_field = obj_get_referent_field(p_obj);
  REF p_referent = *p_referent_field;
  if (!p_referent) return;

  if(DURING_RESURRECTION){
    write_slot(p_referent_field, NULL);
    return;
  }
  switch(type){
    case SOFT_REFERENCE :
      if(collect_is_minor())
        scan_slot(collector, p_referent_field);
      else
        collector_add_softref(collector, p_obj);
      break;
    case WEAK_REFERENCE :
      collector_add_weakref(collector, p_obj);
      break;
    case PHANTOM_REFERENCE :
      collector_add_phanref(collector, p_obj);
      break;
    default :
      assert(0);
      break;
  }
}

inline void scan_weak_reference_direct(Collector *collector, Partial_Reveal_Object *p_obj, Scan_Slot_Func scan_slot)
{
  WeakReferenceType type = special_reference_type(p_obj);
  if(type == NOT_REFERENCE)
    return;
  REF *p_referent_field = obj_get_referent_field(p_obj);
  REF p_referent = *p_referent_field;
  if (!p_referent) return;

  scan_slot(collector, p_referent_field);
}

inline Boolean is_reference_obj(Partial_Reveal_Object *p_obj)
{
  WeakReferenceType type = special_reference_type(p_obj);
  if(type == NOT_REFERENCE)
    return FALSE;
  else
    return TRUE;
}

extern void gc_update_weakref_ignore_finref(GC *gc);
extern void collector_identify_finref(Collector *collector);
extern void fallback_finref_cleanup(GC *gc);
extern void gc_put_finref_to_vm(GC *gc);
extern void put_all_fin_on_exit(GC *gc);

extern void gc_update_finref_repointed_refs(GC *gc, Boolean double_fix);
extern void gc_activate_finref_threads(GC *gc);

void gc_copy_finaliable_obj_to_rootset(GC *gc);

#endif // _FINREF_H_
