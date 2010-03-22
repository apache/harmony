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

#ifndef _FROM_SPACE_H_
#define _FROM_SPACE_H_

#include "../thread/gc_thread.h"
#include "../thread/collector_alloc.h"

/*
 * In our Gen GC, not all live objects are copied to tspace space, the newer baby will
 * still be preserved in  fspace, that means to give them time to die. 
 */

extern Boolean forward_first_half;
/* boundary splitting fspace into forwarding part and remaining part */
extern void* object_forwarding_boundary; 

typedef Blocked_Space Fspace;

Fspace *fspace_initialize(GC* gc, void* start, POINTER_SIZE_INT fspace_size, POINTER_SIZE_INT commit_size);
void fspace_destruct(Fspace *fspace);

inline POINTER_SIZE_INT fspace_free_space_size(Fspace* nos)
{ return blocked_space_free_mem_size((Blocked_Space*)nos);}

inline POINTER_SIZE_INT fspace_used_space_size(Fspace* nos)
{ return blocked_space_used_mem_size((Blocked_Space*)nos);}

#ifndef STATIC_NOS_MAPPING
void* fspace_heap_start_adjust(Fspace* fspace, void* new_heap_start, POINTER_SIZE_INT new_heap_size);
#endif

void* fspace_alloc(unsigned size, Allocator *allocator);
Boolean fspace_alloc_block(Fspace* fspace, Allocator* allocator);

void fspace_reset_after_collection(Fspace* fspace);

/* gen mode */
void gen_forward_pool(Collector* collector); 
void gen_forward_steal(Collector* collector);
/* nongen mode */
void nongen_slide_copy(Collector* collector); 

#ifdef MARK_BIT_FLIPPING

void nongen_forward_steal(Collector* collector); 
void nongen_forward_pool(Collector* collector); 

#endif /* MARK_BIT_FLIPPING */


void fspace_collection(Fspace* fspace);

#endif // _FROM_SPACE_H_
