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


#ifndef _CONCLCTOR_H_
#define _CONCLCTOR_H_

#include "../common/gc_space.h"
#include "../mark_sweep/wspace_chunk.h"

extern SpinLock print_lock; //just for debug, print information

enum CONCLCTOR_STATUS {
  CONCLCTOR_NIL = 0x00,
  CONCLCTOR_ACTIVE = 0x01,
  CONCLCTOR_DEAD = 0x02,
};

enum CONCLCTOR_ROLE {
   CONCLCTOR_ROLE_NIL = 0x0,
   CONCLCTOR_ROLE_MARKER = 0x1,
   CONCLCTOR_ROLE_SWEEPER = 0x2,
};

typedef struct Conclctor {
  void *free;
  void *ceiling;
  void *end;
  void *alloc_block;
  Chunk_Header ***local_chunks;
  Space* alloc_space;
  GC* gc;
  VmThreadHandle thread_handle;   /* This thread; */
  unsigned int handshake_signal; /*Handshake is used in concurrent GC.*/  
  unsigned int num_alloc_blocks; /* the number of allocated blocks in this collection. */
  int64 time_measurement_start;
  int64 time_measurement_end;
  /* End of Allocator --> */

  /* FIXME:: for testing */
  Space* con_space;
  
  /* backup allocator in case there are two target copy spaces, such as semispace GC */
  Allocator* backup_allocator;

  Vector_Block *trace_stack;
  
  Vector_Block* rep_set; /* repointed set */
  Vector_Block* rem_set;
#ifdef USE_32BITS_HASHCODE
  Vector_Block* hashcode_set;
#endif
  
  Vector_Block *softref_set;
  Vector_Block *weakref_set;
  Vector_Block *phanref_set;
  
  VmEventHandle task_assigned_event;
  VmEventHandle task_finished_event;
  
  Block_Header* cur_compact_block;
  Block_Header* cur_target_block;
  
  Free_Chunk_List *free_chunk_list;

  POINTER_SIZE_INT live_obj_size;
  POINTER_SIZE_INT live_obj_num;
  
  void(*task_func)(void*) ;   /* current task */
  
  POINTER_SIZE_INT non_los_live_obj_size;
  POINTER_SIZE_INT los_live_obj_size;
  POINTER_SIZE_INT segment_live_size[NORMAL_SIZE_SEGMENT_NUM];

  unsigned int result;

  /* idle, active or dead */
  unsigned int status;
  /* null, marker or sweeper */
  unsigned int role;
  //VmEventHandle markroot_finished_event;
  int64 time_conclctor;

  unsigned int num_dirty_slots_traced;

  Conclctor* next;

} Conclctor;

//#define MAX_NUM_CONCLCTORS 0xff
//#define MIN_NUM_CONCLCTORS 0x01
#define MAX_NUM_MARKERS 0xff
#define MIN_NUM_MARKERS 0x01

typedef Conclctor* Conclctor_List;

void conclctor_destruct(GC* gc);
void conclctor_initialize(GC* gc);
void conclctor_execute_task_concurrent(GC* gc, TaskType task_func, Space* space, unsigned int num_conclctors, unsigned int role);
int64 gc_get_conclctor_time(GC* gc, unsigned int req_role);
void gc_clear_conclctor_role(GC *gc);
void conclctor_set_weakref_sets(GC* gc);
void conclctor_release_weakref_sets(GC* gc);
void conclctor_reset_weakref_sets(Conclctor *conclctor);

//void conclctor_release_weakref_sets(GC* gc, unsigned int num_conclctor);
//void conclctor_restore_obj_info(Collector* collector);

extern TaskType marker_final_func;
extern TaskType sweeper_final_func;

inline void set_marker_final_func( TaskType func ) {
  marker_final_func = func;
}

inline void set_sweeper_final_func( TaskType func ) {
  sweeper_final_func = func;
}

#endif //#ifndef _CONCLCTOR_H_

