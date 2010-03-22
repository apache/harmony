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

#include "mutator.h"
#include "../trace_forward/fspace.h"
#include "../mark_sweep/gc_ms.h"
#include "../mark_sweep/wspace.h"
#include "../finalizer_weakref/finalizer_weakref.h"

struct GC_Gen;
Space* gc_get_nos(GC_Gen* gc);
void mutator_initialize(GC* gc, void *unused_gc_information) 
{
  /* FIXME:: make sure gc_info is cleared */
  Mutator *mutator = (Mutator *)STD_MALLOC(sizeof(Mutator));
  memset(mutator, 0, sizeof(Mutator));
  mutator->alloc_space = gc_get_nos((GC_Gen*)gc);
  mutator->gc = gc;
    
  if(gc_is_gen_mode()){
    mutator->rem_set = free_set_pool_get_entry(gc->metadata);
    assert(vector_block_is_empty(mutator->rem_set));
  }
  mutator->dirty_set = free_set_pool_get_entry(gc->metadata);
  
  if(!IGNORE_FINREF )
    mutator->obj_with_fin = finref_get_free_block(gc);
  else
    mutator->obj_with_fin = NULL;

#ifdef USE_UNIQUE_MARK_SWEEP_GC
  allocator_init_local_chunks((Allocator*)mutator);
#endif
  
  lock(gc->mutator_list_lock);     // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

  mutator->next = (Mutator *)gc->mutator_list;
  gc->mutator_list = mutator;
  gc->num_mutators++;
  /*Begin to measure the mutator thread execution time. */
  mutator->time_measurement_start = time_now();
  unlock(gc->mutator_list_lock); // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  
  gc_set_tls(mutator);
  
  return;
}

void mutator_register_new_obj_size(Mutator * mutator);

void mutator_destruct(GC* gc, void *unused_gc_information)
{

  Mutator *mutator = (Mutator *)gc_get_tls();

  alloc_context_reset((Allocator*)mutator);


  lock(gc->mutator_list_lock);     // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

#ifdef USE_UNIQUE_MARK_SWEEP_GC
  allocactor_destruct_local_chunks((Allocator*)mutator);
#endif
  mutator_register_new_obj_size(mutator);

  volatile Mutator *temp = gc->mutator_list;
  if (temp == mutator) {  /* it is at the head of the list */
    gc->mutator_list = temp->next;
  } else {
    while (temp->next != mutator) {
      temp = temp->next;
      assert(temp);
    }
    temp->next = mutator->next;
  }
  gc->num_mutators--;

  unlock(gc->mutator_list_lock); // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  
  if(gc_is_gen_mode()){ /* put back the remset when a mutator exits */
    pool_put_entry(gc->metadata->mutator_remset_pool, mutator->rem_set);
    mutator->rem_set = NULL;
  }
  
  if(mutator->obj_with_fin){
    pool_put_entry(gc->finref_metadata->obj_with_fin_pool, mutator->obj_with_fin);
    mutator->obj_with_fin = NULL;
  }

  lock(mutator->dirty_set_lock);
  if( mutator->dirty_set != NULL){
    if(vector_block_is_empty(mutator->dirty_set))
      pool_put_entry(gc->metadata->free_set_pool, mutator->dirty_set);
    else{ /* FIXME:: this condition may be released. */
      pool_put_entry(gc->metadata->gc_dirty_set_pool, mutator->dirty_set);
      mutator->dirty_set = NULL;
    }
  }
  unlock(mutator->dirty_set_lock);
  STD_FREE(mutator);
  gc_set_tls(NULL);
  
  return;
}

void gc_reset_mutator_context(GC* gc)
{
  TRACE2("gc.process", "GC: reset mutator context  ...\n");
  Mutator *mutator = gc->mutator_list;
  while (mutator) {
    alloc_context_reset((Allocator*)mutator);    
    mutator = mutator->next;
  }  
  return;
}

void gc_prepare_mutator_remset(GC* gc)
{
  Mutator *mutator = gc->mutator_list;
  while (mutator) {
    mutator->rem_set = free_set_pool_get_entry(gc->metadata);
    mutator = mutator->next;
  }  
  return;
}

Boolean gc_local_dirtyset_is_empty(GC* gc)
{
  lock(gc->mutator_list_lock);

  Mutator *mutator = gc->mutator_list;
  while (mutator) {
    Vector_Block* local_dirty_set = mutator->dirty_set;
    if(!vector_block_is_empty(local_dirty_set)){
      unlock(gc->mutator_list_lock); 
      return FALSE;
    }
    mutator = mutator->next;
  }  

  unlock(gc->mutator_list_lock); 
  return TRUE;
}

Vector_Block* gc_get_local_dirty_set(GC* gc, unsigned int shared_id)
{
  lock(gc->mutator_list_lock);

  Mutator *mutator = gc->mutator_list;
  while (mutator) {
    Vector_Block* local_dirty_set = mutator->dirty_set;
    if(!vector_block_is_empty(local_dirty_set) && vector_block_set_shared(local_dirty_set,shared_id)){
      unlock(gc->mutator_list_lock); 
      return local_dirty_set;
    }
    mutator = mutator->next;
  }  

  unlock(gc->mutator_list_lock); 
  return NULL;
}

void gc_start_mutator_time_measure(GC* gc)
{
  lock(gc->mutator_list_lock);
  Mutator* mutator = gc->mutator_list;
  while (mutator) {
    mutator->time_measurement_start = time_now();
    mutator = mutator->next;
  }  
  unlock(gc->mutator_list_lock);
}

int64 gc_get_mutator_time(GC* gc)
{
  int64 time_mutator = 0;
  lock(gc->mutator_list_lock);
  Mutator* mutator = gc->mutator_list;
  while (mutator) {
#ifdef _DEBUG
    mutator->time_measurement_end = time_now();
#endif
    int64 time_measured = time_now() - mutator->time_measurement_start;
    if(time_measured > time_mutator){
      time_mutator = time_measured;
    }
    mutator = mutator->next;
  }  
  unlock(gc->mutator_list_lock);
  return time_mutator;
}

static POINTER_SIZE_INT desturcted_mutator_alloced_size;
static POINTER_SIZE_INT desturcted_mutator_alloced_num;
static POINTER_SIZE_INT desturcted_mutator_alloced_occupied_size;
static POINTER_SIZE_INT desturcted_mutator_write_barrier_marked_size;

void mutator_register_new_obj_size(Mutator * mutator)
{
  desturcted_mutator_alloced_size += mutator->new_obj_size;
  desturcted_mutator_alloced_num += mutator->new_obj_num;
  desturcted_mutator_alloced_occupied_size += mutator->new_obj_occupied_size;
  desturcted_mutator_write_barrier_marked_size += mutator->write_barrier_marked_size;
}


unsigned int gc_get_mutator_write_barrier_marked_size(GC* gc)
{
  POINTER_SIZE_INT write_barrier_marked_size = 0;
  lock(gc->mutator_list_lock);
  Mutator* mutator = gc->mutator_list;
  while (mutator) {
    write_barrier_marked_size += mutator->write_barrier_marked_size;
    mutator = mutator->next;
  }  
  unlock(gc->mutator_list_lock);
  return write_barrier_marked_size;
}
unsigned int gc_get_mutator_dirty_obj_num(GC *gc)
{
  POINTER_SIZE_INT dirty_obj_num = 0;
  lock(gc->mutator_list_lock);
  Mutator* mutator = gc->mutator_list;
  while (mutator) {
    dirty_obj_num += mutator->dirty_obj_num;
    mutator = mutator->next;
  }  
  unlock(gc->mutator_list_lock);
  return dirty_obj_num;
}

unsigned int gc_get_mutator_new_obj_size(GC* gc) 
{
  POINTER_SIZE_INT new_obj_occupied_size = 0;
  lock(gc->mutator_list_lock);
  Mutator* mutator = gc->mutator_list;
  while (mutator) {
    new_obj_occupied_size += mutator->new_obj_occupied_size;
    mutator = mutator->next;
  }  
  unlock(gc->mutator_list_lock);

  return new_obj_occupied_size + desturcted_mutator_alloced_occupied_size;
  
}

unsigned int gc_reset_mutator_new_obj_size(GC * gc)
{
  POINTER_SIZE_INT new_obj_occupied_size = 0;
  lock(gc->mutator_list_lock);
  Mutator* mutator = gc->mutator_list;
  while (mutator) {
    new_obj_occupied_size += mutator->new_obj_occupied_size;
    mutator->new_obj_size = 0;
    mutator->new_obj_num = 0;
    mutator->new_obj_occupied_size = 0;
    mutator->write_barrier_marked_size = 0;
    mutator->dirty_obj_num = 0;
    mutator = mutator->next;
  }
  new_obj_occupied_size += desturcted_mutator_alloced_occupied_size;
  desturcted_mutator_alloced_size = 0;
  desturcted_mutator_alloced_num = 0;
  desturcted_mutator_alloced_occupied_size = 0;
  unlock(gc->mutator_list_lock);
  
  return new_obj_occupied_size;
}

unsigned int gc_get_mutator_number(GC *gc)
{
  return gc->num_mutators;
}



