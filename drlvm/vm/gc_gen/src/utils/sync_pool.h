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
 * @author Xiao-Feng Li, 2006/10/25
 */
 
#ifndef _SYNC_POOL_H_
#define _SYNC_POOL_H_

#include "sync_stack.h"

typedef Sync_Stack Pool;

inline Pool* sync_pool_create(){ return sync_stack_init(); }
inline void sync_pool_destruct(Pool* pool){ sync_stack_destruct(pool); }

inline Boolean pool_is_empty(Pool* pool){ return sync_stack_is_empty(pool);}
inline void pool_empty(Pool* pool) { sync_stack_empty(pool); }

inline unsigned int pool_size(Pool* pool){ return sync_stack_size(pool); }

inline Vector_Block* pool_get_entry(Pool* pool)
{ 
  Vector_Block* block = (Vector_Block*)sync_stack_pop(pool); 
  return block;
}

inline void pool_put_entry(Pool* pool, void* value)
{ 
  assert(value); 
  Boolean ok = sync_stack_push(pool, (Node*)value); 
  assert(ok);
}

inline void pool_iterator_init(Pool* pool){ sync_stack_iterate_init(pool);}
inline Vector_Block* pool_iterator_next(Pool* pool){ return (Vector_Block*)sync_stack_iterate_next(pool);}

#endif /* #ifndef _SYNC_POOL_H_ */





