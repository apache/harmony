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
 * @author Xiao-Feng Li, 2007/01/24
 */
#ifndef _COMPRESSED_REF_H_
#define _COMPRESSED_REF_H_

#include "gc_common.h"
#include "gc_metadata.h"
#include "../utils/vector_block.h"
#include "../utils/sync_pool.h"
#include "../thread/collector.h"
#include "../thread/mutator.h"

void gc_set_uncompressed_rootset(GC *gc);
void gc_fix_uncompressed_rootset(GC *gc);


FORCE_INLINE void gc_compressed_rootset_add_entry(GC *gc, REF *p_ref)
{
  assert( p_ref < gc_heap_base(gc) || p_ref >= gc_heap_ceiling(gc));
  
  GC_Metadata *metadata = gc->metadata;
  Vector_Block *root_set = gc->root_set;
  assert(root_set);
  
  vector_block_add_entry(root_set, (POINTER_SIZE_INT)p_ref);
  
  if(!vector_block_is_full(root_set)) return;
  
  pool_put_entry(metadata->gc_rootset_pool, root_set);
  gc->root_set = free_set_pool_get_entry(metadata);
  assert(gc->root_set);
}

#endif /* #ifndef _COMPRESSED_REF_H_ */
