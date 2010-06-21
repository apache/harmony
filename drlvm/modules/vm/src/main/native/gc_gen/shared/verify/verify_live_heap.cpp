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

#include "verify_live_heap.h"
#include "verifier_common.h"
#include "verify_gc_effect.h"
#include "verify_mutator_effect.h"

char* GC_VERIFY = NULL;
Boolean verify_live_heap;

Heap_Verifier* heap_verifier;


void gc_init_heap_verification(GC* gc)
{
  if(GC_VERIFY == NULL){
    verify_live_heap = FALSE;
    return;
  }
  heap_verifier = (Heap_Verifier*) STD_MALLOC(sizeof(Heap_Verifier));
  assert(heap_verifier);
  memset(heap_verifier, 0, sizeof(Heap_Verifier));

  heap_verifier->gc = gc;

  verifier_metadata_initialize(heap_verifier);
  verifier_init_mutator_verifiers(heap_verifier);
  verifier_init_GC_verifier(heap_verifier);
  verifier_init_object_scanner(heap_verifier);

  heap_verifier->is_before_gc = heap_verifier->gc_is_gen_mode = FALSE;
  heap_verifier->need_verify_gc =  heap_verifier->need_verify_rootset
    = heap_verifier->need_verify_allocation =  heap_verifier->need_verify_writebarrier = FALSE;
  
  if(!verifier_parse_options(heap_verifier, GC_VERIFY)){
    printf("GC Verify options error, verifier will not be started.\n");
    gc_terminate_heap_verification(gc);
    return;
  }
  
  verify_live_heap = TRUE;
  return;
}

void gc_terminate_heap_verification(GC* gc)
{
  gc_verifier_metadata_destruct(heap_verifier);
  verifier_destruct_mutator_verifiers(heap_verifier);
  verifier_destruct_GC_verifier(heap_verifier);
  STD_FREE(heap_verifier);
  heap_verifier = NULL;
  return; 
}

void verify_heap_before_gc(GC* gc)
{
  verifier_set_gc_collect_kind(heap_verifier->gc_verifier, GC_PROP);  
  verifier_set_gen_mode(heap_verifier);
  verifier_reset_mutator_verification(heap_verifier);
  verifier_reset_gc_verification(heap_verifier);

  if(need_scan_all_objs(heap_verifier))
    (*heap_verifier->all_obj_scanner)(heap_verifier);  

  /*verify mutator side effect before gc after scanning all objs.*/
  if(need_verify_mutator_effect(heap_verifier))
    verify_mutator_effect(heap_verifier);

  if(need_scan_live_objs(heap_verifier))
    (*heap_verifier->live_obj_scanner)(heap_verifier);
  
  verifier_log_before_gc(heap_verifier);

}

void verifier_cleanup_block_info(GC* gc);

void verify_heap_after_gc(GC* gc)
{
  if(!major_is_marksweep())
    verifier_cleanup_block_info(gc);
      
  if(need_scan_live_objs(heap_verifier))
    (*heap_verifier->live_obj_scanner)(heap_verifier);
  if(need_verify_gc_effect(heap_verifier))
    verify_gc_effect(heap_verifier);

  verifier_log_after_gc(heap_verifier);
  
  verifier_clear_mutator_verification(heap_verifier);
  verifier_clear_gc_verification(heap_verifier);
}

void gc_verify_heap(GC* gc, Boolean is_before_gc)
{
  heap_verifier->is_before_gc = is_before_gc;

  if(is_before_gc){
    verify_heap_before_gc(gc);
  }else{
    verify_heap_after_gc(gc);
  }
}

void event_gc_collect_kind_changed(GC* gc)
{
  /*GC collection kind were changed from normal MINOR or MAJOR  to FALLBACK MAJOR*/
  assert(collect_is_fallback());
  if(!heap_verifier->need_verify_gc) return;
  
  if(!major_is_marksweep())
    verifier_cleanup_block_info(gc);

  /*finish the fallbacked gc verify*/
  heap_verifier->is_before_gc = FALSE;
  verifier_set_fallback_collection(heap_verifier->gc_verifier, TRUE);  
  (*heap_verifier->live_obj_scanner)(heap_verifier);  
  verify_gc_effect(heap_verifier);
  verifier_log_after_gc(heap_verifier);
  verifier_clear_gc_verification(heap_verifier);

  verifier_log_start("GC start");
  /*start fallback major gc verify */
  heap_verifier->is_before_gc = TRUE;
  verifier_set_fallback_collection(heap_verifier->gc_verifier, TRUE);  
  verifier_set_gc_collect_kind(heap_verifier->gc_verifier, GC_PROP);
  verifier_set_gen_mode(heap_verifier);
  verifier_reset_gc_verification(heap_verifier);

  (*heap_verifier->live_obj_scanner)(heap_verifier);
  verifier_set_fallback_collection(heap_verifier->gc_verifier, FALSE);  
}

void event_mutator_allocate_newobj(Partial_Reveal_Object* p_newobj, POINTER_SIZE_INT size, VT vt_raw)
{
  verifier_event_mutator_allocate_newobj(p_newobj, size, vt_raw);
}

Heap_Verifier* get_heap_verifier()
{ return heap_verifier; }




