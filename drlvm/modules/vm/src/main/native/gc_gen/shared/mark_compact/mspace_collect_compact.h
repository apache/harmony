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
 * @author Xiao-Feng Li, 2006/12/12
 */

#ifndef _MSPACE_COLLECT_COMPACT_H_
#define _MSPACE_COLLECT_COMPACT_H_

#include "mspace.h"
#include "../thread/collector.h"     
#include "../common/space_tuner.h"

void gc_reset_block_for_collectors(GC* gc, Mspace* mspace);
void gc_init_block_for_collectors(GC* gc, Mspace* mspace);

void mspace_update_info_after_space_tuning(Mspace* mspace);

Block_Header* mspace_get_first_compact_block(Mspace* mspace);
Block_Header* mspace_get_first_target_block(Mspace* mspace);
Block_Header* mspace_get_next_compact_block(Collector* collector, Mspace* mspace);
Block_Header* mspace_get_next_target_block(Collector* collector, Mspace* mspace);

void slide_compact_mspace(Collector* collector);
void move_compact_mspace(Collector* collector);

void mark_scan_heap_for_fallback(Collector* collector);
void mark_scan_heap_for_space_tune(Collector *collector);

void mspace_extend_compact(Collector *collector);

#ifdef USE_32BITS_HASHCODE
void fallback_clear_fwd_obj_oi(Collector* collector);
void fallback_clear_fwd_obj_oi_init(Collector* collector);
#endif

#endif /* _MSPACE_COLLECT_COMPACT_H_ */





