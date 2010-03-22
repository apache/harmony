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

#ifdef WIN32
#include <malloc.h>
#else
#include <stdlib.h>
#include <string.h>
#endif
#include <iostream>
#include <stdio.h>
#include <assert.h>
#include "MemoryManager.h"

//#define JIT_MEM_CHECK

#ifdef JIT_MEM_CHECK
#include "mkernel.h"
#endif

namespace Jitrino {



#ifdef JIT_MEM_CHECK

#define JIT_MEM_CHECK_PADDING_SIZE 8
#define JIT_MEM_CHECK_DEFAULT_VAL 'x'
#define JIT_MEM_CHECK_PADDING_VAL 'p'

static Mutex checkPointsMutex;
typedef std::map<const Arena*, std::vector<size_t>*> CheckPointsByArena;
static CheckPointsByArena checkPointsByArena;

static void _check_arena_paddings(const Arena* a) ;
static void _check_arena_default_val_on_alloc(const Arena* a, size_t offset, size_t len) ;
static inline void m_assert(bool cond);

#endif //JIT_MEM_CHECK


#define BITS_TO_CLEAR (sizeof(void*)-1)
//
// For tracing
//

#ifdef USE_TRACE_MEM_MANANGER

static int  traceId = 0;
static ::std::ostream& traceStream = ::std::cerr;
#endif

static const U_32 mm_default_next_arena_size = 4096-ARENA_HEADER_SIZE;

MemoryManager::MemoryManager(const char* name)
{
    _arena = NULL;
    _bytes_allocated = 0;
    _bytes_arena = 0;
    _name = name;
    _numArenas = 0;

#ifdef USE_TRACE_MEM_MANANGER
    // get start time for this MemoryManager
    _startTraceId = traceId++;
#ifdef DEBUG_MEMORY_MANAGER
    traceStream << "### MemoryManager," << _name  << ", START" << ::std::endl;
#endif //DEBUG_MEMORY_MANAGER

#endif //USE_TRACE_MEM_MANANGER
}

#ifdef DEBUG_MEMORY_MANAGER
static size_t totalAllocated = 0;
#endif

MemoryManager::~MemoryManager()
{

#ifdef USE_TRACE_MEM_MANANGER
    // get end time for this MemoryManager
    _endTraceId = traceId++;
#ifdef DEBUG_MEMORY_MANAGER
    // print out trace
    traceStream << "### MemoryManager," << _name 
         << "," << _startTraceId 
         << "," << (_endTraceId - _startTraceId)
         << "," << (int)_bytes_allocated 
         << "," << (int)_bytes_arena
         << "," << _numArenas 
         << ::std::endl;
    traceStream << "### TotalMalloced=" << (int) totalAllocated << ::std::endl;
#endif //DEBUG_MEMORY_MANAGER
#endif //USE_TRACE_MEM_MANANGER
    _free_arenas(_arena);
    _arena = 0;
}

void MemoryManager::_alloc_arena(size_t size)
{
    _numArenas++;
    _bytes_arena += size;
    _arena = alloc_arena(_arena,size);
    assert(((POINTER_SIZE_INT)_arena->next_byte & BITS_TO_CLEAR) == 0);

#ifdef JIT_MEM_CHECK
    checkPointsMutex.lock();
    checkPointsByArena[_arena] = new std::vector<size_t>();
    checkPointsMutex.unlock();
    size_t allocated_size = _arena->last_byte - _arena->bytes; 
    memset(_arena->bytes, JIT_MEM_CHECK_DEFAULT_VAL, allocated_size);
#endif

}

void MemoryManager::_free_arenas(Arena *a)
{
    while(a != NULL) {
        Arena* last = a;
        a = a->next_arena;
#ifdef JIT_MEM_CHECK
        checkPointsMutex.lock();
        _check_arena_paddings(last);
        CheckPointsByArena::iterator it = checkPointsByArena.find(last);
        m_assert(it!=checkPointsByArena.end());
        std::vector<size_t>* v = it->second;
        checkPointsByArena.erase(it);
        delete v;
        checkPointsMutex.unlock();
#endif
        free_arena(last);
    }
}


#ifdef JIT_MEM_CHECK

static inline void m_assert(bool cond)  {
#ifdef _DEBUG
    assert(cond);
#else
#ifdef WIN32
    if (!cond) {
        __asm {
            int 3;
        }
    }
#endif
#endif    
}

void _check_arena_paddings(const Arena* a) {
    CheckPointsByArena::const_iterator ait = checkPointsByArena.find(a);
    m_assert(ait!=checkPointsByArena.end());
    std::vector<size_t>* t = ait->second;
    for (std::vector<size_t>::const_iterator it = t->begin(), end = t->end(); it!=end; ++it) {
        size_t offset = *it;
        const char* borderStart = a->bytes + offset;
        for (int j=0; j<JIT_MEM_CHECK_PADDING_SIZE; j++) {
            char c = borderStart[j];
            m_assert(c == JIT_MEM_CHECK_PADDING_VAL);
        }
    }
}

void _check_arena_default_val_on_alloc(const Arena* a, size_t offset, size_t len)  {
    const char* mem = a->bytes + offset;
    for (size_t j=0; j<len; j++) {
        char c = mem[j];
        m_assert(c == JIT_MEM_CHECK_DEFAULT_VAL); 
    }
}

#endif //JIT_MEM_CHECK

void *MemoryManager::alloc(size_t size)
{
    if (size == 0)
        return NULL;

    // _arena=>next_byte is guaranteed to be aligned initially
    // to maintain the above, make sure that size is rounded up 
    // if necessary

#ifdef JIT_MEM_CHECK
    size+=JIT_MEM_CHECK_PADDING_SIZE;
#endif

    size = (size + BITS_TO_CLEAR) & ~BITS_TO_CLEAR;
    _bytes_allocated += size;

    if (_arena == NULL || _arena->next_byte + size > _arena->last_byte) {
        //
        // allocate another arena
        //
        size_t arena_size;
        if (size < mm_default_next_arena_size) {
            arena_size = mm_default_next_arena_size;
        } else {
            arena_size = size;
        }
        _alloc_arena(arena_size);
        assert(((POINTER_SIZE_INT)_arena->next_byte & BITS_TO_CLEAR) == 0);
    }
    //
    // already aligned 
    //
    char *mem = _arena->next_byte;
    //
    // still aligned
    //
    _arena->next_byte += size;
    assert(((POINTER_SIZE_INT)_arena->next_byte & BITS_TO_CLEAR) == 0);

#ifdef JIT_MEM_CHECK
    checkPointsMutex.lock();
    _check_arena_default_val_on_alloc(_arena, mem  - _arena->bytes, size);
    CheckPointsByArena::iterator it = checkPointsByArena.find(_arena);
    assert(it!=checkPointsByArena.end());
    std::vector<size_t>* v = it->second;
    //push offset of the current padding from the start of arena
    v->push_back((_arena->next_byte - _arena->bytes) - JIT_MEM_CHECK_PADDING_SIZE); 
    memset(mem + size - JIT_MEM_CHECK_PADDING_SIZE, JIT_MEM_CHECK_PADDING_VAL, JIT_MEM_CHECK_PADDING_SIZE);
    checkPointsMutex.unlock();
#endif


    return (void*)mem;
}

char* 
MemoryManager::copy(const char* str) {
    if (!str) return NULL;
    size_t len = strlen(str);
    char* cp = (char*)alloc(len + 1);
    return strcpy(cp, str);
}

} //namespace Jitrino 

#ifdef DEBUG_MEMORY_MANAGER
void* operator new (size_t sz) {
    Jitrino::totalAllocated += sz;
    return malloc(sz);
}
#endif

