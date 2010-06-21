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
#define LOG_DOMAIN "gc.base"
#include "gc_common.h"
#include "open/vm_properties.h"
#include "gc_concurrent.h"
#include "concurrent_collection_scheduler.h"

/* FIXME:: need refactoring this function to distribute the options 
   interpretation to their respective modules. */

/* for ALLOC_PREFETCH related micro definition */
#include "../thread/gc_thread.h" 

extern char* GC_VERIFY;
extern POINTER_SIZE_INT NOS_SIZE;
extern POINTER_SIZE_INT MIN_NOS_SIZE;
extern POINTER_SIZE_INT INIT_LOS_SIZE;
extern POINTER_SIZE_INT TOSPACE_SIZE;
extern POINTER_SIZE_INT MOS_RESERVE_SIZE;

extern Boolean GEN_NONGEN_SWITCH;

extern Boolean FORCE_FULL_COMPACT;

extern unsigned int NUM_CONCLCTORS;
extern unsigned int NUM_CON_MARKERS;
extern unsigned int NUM_CON_SWEEPERS;

extern unsigned int NUM_COLLECTORS;
extern unsigned int MINOR_COLLECTORS;
extern unsigned int MAJOR_COLLECTORS;

extern Boolean IGNORE_VTABLE_TRACING;
extern Boolean IGNORE_FINREF;

extern Boolean JVMTI_HEAP_ITERATION ;
extern Boolean IGNORE_FORCE_GC;

POINTER_SIZE_INT HEAP_SIZE_DEFAULT = 256 * MB;
POINTER_SIZE_INT min_heap_size_bytes = 16 * MB;
POINTER_SIZE_INT max_heap_size_bytes = 0;

Boolean share_los_boundary = FALSE;

unsigned int GC_PROP;

GC* gc_mc_create();
GC* gc_ms_create();

static GC* gc_unique_decide_collection_algo(char* unique_algo, Boolean has_los)
{
  /* if unique_algo is not set, gc_gen_decide_collection_algo is called. */
  assert(unique_algo);
  
  GC_PROP = ALGO_POOL_SHARE | ALGO_DEPTH_FIRST | ALGO_IS_UNIQUE;
  
  assert(!has_los); /* currently unique GCs don't use LOS */
  if(has_los) 
    GC_PROP |= ALGO_HAS_LOS;
  
  Boolean use_default = FALSE;

  GC* gc;
  
  string_to_upper(unique_algo);
   
  if(!strcmp(unique_algo, "MOVE_COMPACT")){
    GC_PROP |= ALGO_COMPACT_MOVE;
    gc = gc_mc_create();  

  }else if(!strcmp(unique_algo, "MARK_SWEEP")){
    GC_PROP |= ALGO_MS_NORMAL;
    gc = gc_ms_create();
  }else{
    LWARN(48, "\nGC algorithm setting incorrect. Will use default value.\n");
    GC_PROP |= ALGO_COMPACT_MOVE;
    gc = gc_mc_create();  
  }

  return gc;
}

static int vm_property_get_integer(const char *property_name)
{
    assert(property_name);
    if(!vm_property_is_set(property_name, VM_PROPERTIES)) {
        LDIE(76, "Property value {0} is not set!" << property_name);
    }

    return vm_property_get_integer(property_name, 0, VM_PROPERTIES);
}

static BOOLEAN vm_property_get_boolean(const char *property_name)
{
  assert(property_name);
  if (!vm_property_is_set(property_name, VM_PROPERTIES)){
        LDIE(76, "Property value {0} is not set!" << property_name);
  }

  return vm_property_get_boolean(property_name, FALSE, VM_PROPERTIES);
}

static size_t vm_property_get_size(const char* property_name)
{
    assert(property_name);
    if(!vm_property_is_set(property_name, VM_PROPERTIES)) {
        LDIE(76, "Property value {0} is not set!" << property_name);
    }

    return vm_property_get_size(property_name, 0, VM_PROPERTIES);
}

void gc_decide_con_algo(char* concurrent_algo);
GC* gc_gen_decide_collection_algo(char* minor_algo, char* major_algo, Boolean has_los);
void gc_set_gen_mode(Boolean status);

GC* gc_parse_options() 
{
  TRACE2("gc.process", "GC: parse options ...\n");

  GC* gc;

  /* GC algorithm decision */
  /* Step 1: */
  char* minor_algo = NULL;
  char* major_algo = NULL;
  char* unique_algo = NULL;

  if (vm_property_is_set("gc.minor_algorithm", VM_PROPERTIES) == 1) {
    minor_algo = vm_properties_get_value("gc.minor_algorithm", VM_PROPERTIES);
  }

  if (vm_property_is_set("gc.major_algorithm", VM_PROPERTIES) == 1) {
    major_algo = vm_properties_get_value("gc.major_algorithm", VM_PROPERTIES);
  }

  if (vm_property_is_set("gc.unique_algorithm", VM_PROPERTIES) == 1) {
    unique_algo = vm_properties_get_value("gc.unique_algorithm", VM_PROPERTIES);
  }

  Boolean has_los = FALSE;
  if (vm_property_is_set("gc.has_los", VM_PROPERTIES) == 1) {
    has_los = vm_property_get_boolean("gc.has_los");
  }

  if(unique_algo){
    if(minor_algo || major_algo){
      LWARN(60, "Generational options cannot be set with unique_algo, ignored.");
    }
    gc = gc_unique_decide_collection_algo(unique_algo, has_los);
    vm_properties_destroy_value(unique_algo);  
  }else{ /* default */
    gc = gc_gen_decide_collection_algo(minor_algo, major_algo, has_los);
    if( minor_algo) vm_properties_destroy_value(minor_algo);
    if( major_algo) vm_properties_destroy_value(major_algo);
  }

  if (vm_property_is_set("gc.gen_mode", VM_PROPERTIES) == 1) {
    Boolean gen_mode = vm_property_get_boolean("gc.gen_mode");
    gc_set_gen_mode(gen_mode);
  }

  /* Step 2: */

  /* NOTE:: this has to stay after above!! */
  if (vm_property_is_set("gc.force_major_collect", VM_PROPERTIES) == 1) {
    FORCE_FULL_COMPACT = vm_property_get_boolean("gc.force_major_collect");
    if(FORCE_FULL_COMPACT){
      gc_set_gen_mode(FALSE);
    }
  }

  /* Step 3: */
  /* NOTE:: this has to stay after above!! */
  gc->generate_barrier = gc_is_gen_mode();
  
  if (vm_property_is_set("gc.generate_barrier", VM_PROPERTIES) == 1) {
    Boolean generate_barrier = vm_property_get_boolean("gc.generate_barrier");
    gc->generate_barrier = (generate_barrier || gc->generate_barrier);
  }
  
/* ///////////////////////////////////////////////////   */
  
  POINTER_SIZE_INT max_heap_size = HEAP_SIZE_DEFAULT;
  POINTER_SIZE_INT min_heap_size = min_heap_size_bytes;
  
  if (vm_property_is_set("gc.mx", VM_PROPERTIES) == 1) {
    max_heap_size = vm_property_get_size("gc.mx");

    if (max_heap_size < min_heap_size){
      max_heap_size = min_heap_size;
      LWARN(61, "Max heap size you set is too small, reset to {0}MB" << max_heap_size/MB);
    }
    if (0 == max_heap_size){
      max_heap_size = HEAP_SIZE_DEFAULT;
      LWARN(62, "Max heap size you set equals to zero, reset to {0}MB" << max_heap_size/MB);
    }
 
    min_heap_size = max_heap_size / 10;
    if (min_heap_size < min_heap_size_bytes){
      min_heap_size = min_heap_size_bytes;
      //printf("Min heap size: too small, reset to %d MB! \n", min_heap_size/MB);
    }
  }

  if (vm_property_is_set("gc.ms", VM_PROPERTIES) == 1) {
    min_heap_size = vm_property_get_size("gc.ms");
    if (min_heap_size < min_heap_size_bytes){
      min_heap_size = min_heap_size_bytes;
      LWARN(63, "Min heap size you set is too small, reset to {0}MB" << min_heap_size/MB);
    } 
  }

  if (min_heap_size > max_heap_size){
    max_heap_size = min_heap_size;
    LWARN(61, "Max heap size is too small, reset to {0}MB" << max_heap_size/MB);
  }

  min_heap_size_bytes = min_heap_size;
  max_heap_size_bytes = max_heap_size;

  if (vm_property_is_set("gc.nos_size", VM_PROPERTIES) == 1) {
    NOS_SIZE = vm_property_get_size("gc.nos_size");
  }

  if (vm_property_is_set("gc.min_nos_size", VM_PROPERTIES) == 1) {
    MIN_NOS_SIZE = vm_property_get_size("gc.min_nos_size");
  }

  if (vm_property_is_set("gc.init_los_size", VM_PROPERTIES) == 1) {
    INIT_LOS_SIZE = vm_property_get_size("gc.init_los_size");
  }  

  if (vm_property_is_set("gc.num_collectors", VM_PROPERTIES) == 1) {
    unsigned int num = vm_property_get_integer("gc.num_collectors");
    NUM_COLLECTORS = (num==0)? NUM_COLLECTORS:num;
  }

  if (vm_property_is_set("gc.num_conclctors", VM_PROPERTIES) == 1) {
    unsigned int num = vm_property_get_integer("gc.num_conclctors");
    NUM_CONCLCTORS = (num==0)? NUM_CONCLCTORS:num;
  }

  // for concurrent GC debug
  if (vm_property_is_set("gc.num_con_markers", VM_PROPERTIES) == 1) {
    unsigned int num = vm_property_get_integer("gc.num_con_markers");
    NUM_CON_MARKERS = (num==0)? NUM_CON_MARKERS:num;
  }

  if (vm_property_is_set("gc.num_con_sweepers", VM_PROPERTIES) == 1) {
    unsigned int num = vm_property_get_integer("gc.num_con_sweepers");
    NUM_CON_SWEEPERS = (num==0)? NUM_CON_SWEEPERS:num;
  }


  

  if (vm_property_is_set("gc.tospace_size", VM_PROPERTIES) == 1) {
    TOSPACE_SIZE = vm_property_get_size("gc.tospace_size");
  }

  if (vm_property_is_set("gc.mos_reserve_size", VM_PROPERTIES) == 1) {
    MOS_RESERVE_SIZE = vm_property_get_size("gc.mos_reserve_size");
  }

  if (vm_property_is_set("gc.nos_partial_forward", VM_PROPERTIES) == 1) {
    NOS_PARTIAL_FORWARD = vm_property_get_boolean("gc.nos_partial_forward");
  }
    
  if (vm_property_is_set("gc.minor_collectors", VM_PROPERTIES) == 1) {
    MINOR_COLLECTORS = vm_property_get_integer("gc.minor_collectors");
  }

  if (vm_property_is_set("gc.major_collectors", VM_PROPERTIES) == 1) {
    MAJOR_COLLECTORS = vm_property_get_integer("gc.major_collectors");
  }

  if (vm_property_is_set("gc.ignore_finref", VM_PROPERTIES) == 1) {
    IGNORE_FINREF = vm_property_get_boolean("gc.ignore_finref");
  }

  if (vm_property_is_set("gc.verify", VM_PROPERTIES) == 1) {
    char* value = vm_properties_get_value("gc.verify", VM_PROPERTIES);
    GC_VERIFY = strdup(value);
    vm_properties_destroy_value(value);
  }

  if (vm_property_is_set("gc.gen_nongen_switch", VM_PROPERTIES) == 1){
    GEN_NONGEN_SWITCH= vm_property_get_boolean("gc.gen_nongen_switch");
    gc->generate_barrier = TRUE;
  }

  if (vm_property_is_set("gc.heap_iteration", VM_PROPERTIES) == 1) {
    JVMTI_HEAP_ITERATION = vm_property_get_boolean("gc.heap_iteration");
  }

  if (vm_property_is_set("gc.ignore_vtable_tracing", VM_PROPERTIES) == 1) {
    IGNORE_VTABLE_TRACING = vm_property_get_boolean("gc.ignore_vtable_tracing");
  }

  if (vm_property_is_set("gc.use_large_page", VM_PROPERTIES) == 1){
    char* value = vm_properties_get_value("gc.use_large_page", VM_PROPERTIES);
    large_page_hint = strdup(value);
    vm_properties_destroy_value(value);
  }

  if (vm_property_is_set("gc.share_los_boundary", VM_PROPERTIES) == 1){
    share_los_boundary = vm_property_get_boolean("gc.share_los_boundary");
  }

  if (vm_property_is_set("gc.ignore_force_gc", VM_PROPERTIES) == 1){
    IGNORE_FORCE_GC = vm_property_get_boolean("gc.ignore_force_gc");
  }
  
  if (vm_property_is_set("gc.concurrent_gc", VM_PROPERTIES) == 1){
    Boolean use_all_concurrent_phase= vm_property_get_boolean("gc.concurrent_gc");
    if(use_all_concurrent_phase){
#ifndef USE_UNIQUE_MARK_SWEEP_GC
      LDIE(77, "Please define USE_UNIQUE_MARK_SWEEP_GC macro.");
#endif
      gc_specify_con_enum();
      gc_specify_con_mark();
      gc_specify_con_sweep();
      gc->generate_barrier = TRUE;
    }
  }

  if (vm_property_is_set("gc.concurrent_enumeration", VM_PROPERTIES) == 1){
    Boolean USE_CONCURRENT_ENUMERATION = vm_property_get_boolean("gc.concurrent_enumeration");
    if(USE_CONCURRENT_ENUMERATION){
#ifndef USE_UNIQUE_MARK_SWEEP_GC
      LDIE(77, "Please define USE_UNIQUE_MARK_SWEEP_GC macro.");
#endif
      gc_specify_con_enum();
      gc->generate_barrier = TRUE;
    }
  }

  if (vm_property_is_set("gc.concurrent_mark", VM_PROPERTIES) == 1){
    Boolean USE_CONCURRENT_MARK = vm_property_get_boolean("gc.concurrent_mark");
    if(USE_CONCURRENT_MARK){
#ifndef USE_UNIQUE_MARK_SWEEP_GC
      LDIE(77, "Please define USE_UNIQUE_MARK_SWEEP_GC macro.");
#endif
      gc_specify_con_mark();
      gc->generate_barrier = TRUE;
      IGNORE_FINREF = TRUE; /*TODO: finref is unsupported.*/
    }
  }

  if (vm_property_is_set("gc.concurrent_sweep", VM_PROPERTIES) == 1){
    Boolean USE_CONCURRENT_SWEEP= vm_property_get_boolean("gc.concurrent_sweep");
    if(USE_CONCURRENT_SWEEP){
      /*currently, concurrent sweeping only starts after concurrent marking.*/
      assert(gc_is_specify_con_mark());
#ifndef USE_UNIQUE_MARK_SWEEP_GC
      LDIE(77, "Please define USE_UNIQUE_MARK_SWEEP_GC macro.");
#endif
      gc_specify_con_sweep();
      IGNORE_FINREF = TRUE; /*TODO: finref is unsupported.*/
    }
  }
 
  char* concurrent_algo = NULL;
  
  if (vm_property_is_set("gc.concurrent_algorithm", VM_PROPERTIES) == 1) {
    concurrent_algo = vm_properties_get_value("gc.concurrent_algorithm", VM_PROPERTIES);    
    gc_decide_con_algo(concurrent_algo);
  }else if(gc_is_specify_con_gc()){
    gc_set_default_con_algo();
  }

  char* cc_scheduler = NULL;
  if (vm_property_is_set("gc.cc_scheduler", VM_PROPERTIES) == 1) {
    cc_scheduler = vm_properties_get_value("gc.cc_scheduler", VM_PROPERTIES);    
    gc_decide_cc_scheduler_kind(cc_scheduler);
  }else if(gc_is_specify_con_gc()){
    gc_set_default_cc_scheduler_kind();
  }

#if defined(ALLOC_ZEROING) && defined(ALLOC_PREFETCH)
  if(vm_property_is_set("gc.prefetch",VM_PROPERTIES) ==1) {
    PREFETCH_ENABLED = vm_property_get_boolean("gc.prefetch");
  }

  if(vm_property_is_set("gc.prefetch_distance",VM_PROPERTIES)==1) {
    PREFETCH_DISTANCE = vm_property_get_size("gc.prefetch_distance");
    if(!PREFETCH_ENABLED) {
      LWARN(64, "Prefetch distance set with Prefetch disabled!");
    }
  }

  if(vm_property_is_set("gc.prefetch_stride",VM_PROPERTIES)==1) {
    PREFETCH_STRIDE = vm_property_get_size("gc.prefetch_stride");
    if(!PREFETCH_ENABLED) {
      LWARN(65, "Prefetch stride set  with Prefetch disabled!");
    }  
  }
  
  if(vm_property_is_set("gc.zeroing_size",VM_PROPERTIES)==1) {
    ZEROING_SIZE = vm_property_get_size("gc.zeroing_size");
  }   
#endif

#ifdef PREFETCH_SUPPORTED
  if(vm_property_is_set("gc.mark_prefetch",VM_PROPERTIES) ==1) {
    mark_prefetch = vm_property_get_boolean("gc.mark_prefetch");
  }  
#endif

  return gc;
}




