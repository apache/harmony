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

#include "gc_common.h"
#include "../utils/vector_block.h"
#include "../utils/sync_pool.h"
#include "../thread/collector.h"
#include "../thread/mutator.h"
#include "compressed_ref.h"

typedef struct Uncompressed_Root{
  Partial_Reveal_Object **p_ref;   /* pointing to the uncompressed address of the root object */
  REF ref;  /* temporal compressed pointer pointing to the root object */
}Compressed_Root;

POINTER_SIZE_INT vtable_base = 0;
POINTER_SIZE_INT HEAP_BASE = 0;

void gc_set_uncompressed_rootset(GC *gc)
{
  Pool *rootset_pool = gc->metadata->gc_uncompressed_rootset_pool;
  
  pool_put_entry(rootset_pool, gc->uncompressed_root_set);
  gc->uncompressed_root_set = NULL;
  
  pool_iterator_init(rootset_pool);
  while(Vector_Block *root_set = pool_iterator_next(rootset_pool)){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(root_set);
    for(; !vector_block_iterator_end(root_set, iter); iter = vector_block_iterator_advance(root_set, iter)){
      iter = vector_block_iterator_advance(root_set, iter);
      assert(!vector_block_iterator_end(root_set, iter));
      /* add the pointer to ref of Uncmpressed_Root to rootset */
      gc_compressed_rootset_add_entry(gc, (REF *)iter);
    }
  }
}

void gc_fix_uncompressed_rootset(GC *gc)
{
  Pool *rootset_pool = gc->metadata->gc_uncompressed_rootset_pool;
  
  pool_iterator_init(rootset_pool);
  while(Vector_Block *root_set = pool_iterator_next(rootset_pool)){
    POINTER_SIZE_INT *iter = vector_block_iterator_init(root_set);
    for(; !vector_block_iterator_end(root_set, iter); iter = vector_block_iterator_advance(root_set, iter)){
      Partial_Reveal_Object **p_ref = (Partial_Reveal_Object **)*iter;
      iter = vector_block_iterator_advance(root_set, iter);
      assert(!vector_block_iterator_end(root_set, iter));
      Partial_Reveal_Object *p_obj = read_slot((REF*)iter);
      *p_ref = p_obj;
    }
  }
}




