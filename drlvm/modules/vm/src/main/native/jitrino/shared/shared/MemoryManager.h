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

#ifndef _MEMORY_MANAGER_H_
#define _MEMORY_MANAGER_H_

#include <assert.h>
#include <stdio.h>
#include <map>
#include <vector>
#include "Arena.h"

namespace Jitrino {

class MemoryManager {
public:
    MemoryManager(const char* name);
    virtual ~MemoryManager();
    void *alloc(size_t size);
    size_t bytes_allocated() { return _bytes_allocated; }
    char* copy(const char* str);
protected:
    MemoryManager(const MemoryManager&) {assert(0);}
    MemoryManager& operator=(const MemoryManager&) {assert(0); return *this;}
    //
    // allocate a new arena of size
    //
    void _alloc_arena(size_t size);
    //
    // free all the allocated arenas starting from arena a
    //
    void _free_arenas(Arena *a);
    //
    // arena where memory is allocate from
    //
    Arena    *_arena;

    //
    // stats
    //
    size_t _bytes_allocated;
    size_t _bytes_arena;
    //
    // fields for tracing
    //
    const char* _name;
    int _startTraceId;
    int _endTraceId;
    int _numArenas;
};

} //namespace Jitrino 

inline void* operator new (size_t sz, Jitrino::MemoryManager& mm) {return mm.alloc(sz);}
inline void* operator new[](size_t sz, Jitrino::MemoryManager& mm) {return mm.alloc(sz);}
// add delete() to avoid compilation warning
inline void  operator delete (void* p, Jitrino::MemoryManager& mm) {}
inline void  operator delete[](void* p, Jitrino::MemoryManager& mm) {}

#endif    // _MEMORY_MANAGER_H_
