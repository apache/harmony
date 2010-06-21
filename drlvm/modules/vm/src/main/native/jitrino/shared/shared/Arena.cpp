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
 * @author Intel, Mikhail Y. Fursov
 *
 */

#ifdef _WIN32
#include <malloc.h>
#endif
#include <stdlib.h>
#include <assert.h>
#include "Arena.h"

namespace Jitrino {

#define BITS_TO_CLEAR (sizeof(void*)-1)


Arena *init_arena(void *space,Arena *next_arena,size_t size)
{
    // memory should be aligned
    assert(((POINTER_SIZE_INT)space  & BITS_TO_CLEAR) == 0);
    Arena *arena = (Arena *)space;
    arena->next_arena = next_arena;
    arena->next_byte = arena->bytes;
    arena->last_byte = arena->bytes + size;
    assert(((POINTER_SIZE_INT)arena->next_byte & BITS_TO_CLEAR) == 0);
    return arena;
}


Arena *alloc_arena(Arena *next,size_t size)
{
    //
    // malloc a chunk of memory for a new arena
    // we add space for 3 pointers - the arena's next and end fields and next arena
    // make sure it is rounded up or else space will be wasted 
    // and unnecessary realloc will occur
    //
    size = (size + BITS_TO_CLEAR) & ~BITS_TO_CLEAR;
    size_t header_size = ARENA_HEADER_SIZE;
    void *space = malloc(size + header_size);
    Arena *arena = init_arena(space,next,size);
    return arena;
}

void free_arena(Arena *a)
{
    free(a);
}

void *arena_alloc_space(Arena *arena,size_t size)
{
    if (size == 0)
        return NULL;
    
    assert(((POINTER_SIZE_INT)arena->next_byte & BITS_TO_CLEAR) == 0);
    size = (size + BITS_TO_CLEAR) & ~BITS_TO_CLEAR;

    if (arena->next_byte + size > arena->last_byte) {
        // not enough space
        return NULL;
    }
    // return the space and bump next_byte pointer
    char *mem = arena->next_byte;
    arena->next_byte += size;
    assert (arena->next_byte <= arena->last_byte);
    return (void*)mem;
}



} //namespace Jitrino 
