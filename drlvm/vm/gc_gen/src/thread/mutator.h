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

#ifndef _MUTATOR_H_
#define _MUTATOR_H_

#include "../common/gc_space.h"

struct Chunk_Header;

/* Mutator thread local information for GC */
typedef struct Mutator {
  /* <-- first couple of fields are overloaded as Allocator */
  void* free;
  void* ceiling;
  void* end;
  void* alloc_block;
  Chunk_Header ***local_chunks;
  Space* alloc_space;
  GC* gc;
  VmThreadHandle thread_handle;   /* This thread; */
  volatile unsigned int handshake_signal; /*Handshake is used in concurrent GC.*/
  unsigned int num_alloc_blocks; /* the number of allocated blocks since last collection. */
  int64 time_measurement_start;
  int64 time_measurement_end;
  /* END of Allocator --> */
  
  Vector_Block* rem_set;
  Vector_Block* obj_with_fin;
  Mutator* next;  /* The gc info area associated with the next active thread. */
  Vector_Block* dirty_set;
  SpinLock dirty_set_lock;
  unsigned int dirty_obj_slot_num; //only ON_THE_FLY
  unsigned int dirty_obj_num; //concurrent mark  
  
  /* obj alloc information */
  POINTER_SIZE_INT new_obj_size;
  /* accurate object number and total size*/
  POINTER_SIZE_INT new_obj_num;
  POINTER_SIZE_INT new_obj_occupied_size;
  POINTER_SIZE_INT write_barrier_marked_size;

} Mutator;

void mutator_initialize(GC* gc, void* tls_gc_info);
void mutator_destruct(GC* gc, void* tls_gc_info); 
void mutator_reset(GC *gc);

void gc_reset_mutator_context(GC* gc);
void gc_prepare_mutator_remset(GC* gc);

Boolean gc_local_dirtyset_is_empty(GC* gc);
Vector_Block* gc_get_local_dirty_set(GC* gc, unsigned int shared_id);
void gc_start_mutator_time_measure(GC* gc);
int64 gc_get_mutator_time(GC* gc);

unsigned int gc_get_mutator_write_barrier_marked_size( GC *gc );
unsigned int gc_get_mutator_dirty_obj_num(GC *gc);
unsigned int gc_get_mutator_new_obj_size( GC* gc );
unsigned int gc_reset_mutator_new_obj_size( GC* gc );

inline void mutator_post_signal(Mutator* mutator, unsigned int handshake_signal)
{ 
  mem_fence();
  mutator->handshake_signal = handshake_signal; 
  mem_fence();
}

inline void wait_mutator_signal(Mutator* mutator, unsigned int handshake_signal)
{ while(mutator->handshake_signal != handshake_signal); }


#endif /*ifndef _MUTATOR_H_ */
