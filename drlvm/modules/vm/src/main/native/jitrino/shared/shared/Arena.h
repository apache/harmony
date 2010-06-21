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

#ifndef _ARENA_H_
#define _ARENA_H_

#ifdef unix
#include <unistd.h>
#endif
#include "open/types.h"


namespace Jitrino {

typedef struct Arena {
    struct Arena *next_arena; // next arena
    char         *next_byte;  // next byte available in arena
    char         *last_byte;  // end of arena   space
    char         bytes[1];    // start of arena space
} Arena;

#define ARENA_HEADER_SIZE ((sizeof(struct Arena *)+sizeof(char *)+sizeof(char *) + BITS_TO_CLEAR) & ~BITS_TO_CLEAR);

static const U_32 default_arena_size = 1024;
//
// give empty space of memory, make it into an arena of given size.
//
Arena *init_arena(void *space, Arena *next_arena,size_t size);
//
// malloc (free) an arena from the global (thread-safe) heap
//
Arena *alloc_arena(Arena *next,size_t size);
void free_arena(Arena *a);
//
// allocates and return space from Arena
//
void *arena_alloc_space(Arena *arena,size_t size);

} //namespace Jitrino 

#endif // _ARENA_H_
