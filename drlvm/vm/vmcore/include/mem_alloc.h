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
#ifndef _MEM_ALLOC_H
#define _MEM_ALLOC_H

#include "open/rt_types.h"
#include "port_vmem.h"

#define KBYTE 1024
#define MBYTE 1024*KBYTE
#define GBYTE 1024*MBYTE

// pool is used for common stub code
#define DEFAULT_JIT_CODE_POOL_SIZE                  256*KBYTE
// used for compiled code of a user class loader
#define DEFAULT_CLASSLOADER_JIT_CODE_POOL_SIZE      256*KBYTE
// used for compiled code of the bootstrap class loader
#define DEFAULT_BOOTSTRAP_JIT_CODE_POOL_SIZE        1*MBYTE
// used for a string pool
#define DEFAULT_STRING_TABLE_SIZE                   262143

#define MEMORY_UTILIZATION_LIMIT 15

typedef struct PoolDescriptor {
    U_8*    _begin;     // next free byte in memory chunk
    U_8*    _end;       // end of memory chunk
    size_t  _size;      // size of memory chunk
    port_vmem_t*    _descriptor; // for further memory deallocation
    PoolDescriptor* _next; 
} PoolDescriptor;

// PoolManager is a thread safe memory manager
// PoolDescriptor describes allocated memory chunk inside pool
// There are 2 kinds of PoolDescriptor in PoolManager: active and passive
// PoolManager uses active PoolDescriptors for memory allocations, passive PoolDescriptors are filled with allocated memory and not used. 
// Division into active and passive PoolDescriptors is done on the basis of MEMORY_UTILIZATION_LIMIT value. 
// if PoolDescriptors is filled less than (MEMORY_UTILIZATION_LIMIT)% of its size then it is considered to be passive,
// otherwise it is active (allows further memory allocations from it)

class BasePoolManager {
public:
    BasePoolManager(size_t initial_size, bool use_large_pages, bool is_code);
    virtual ~BasePoolManager();
    
protected:
    size_t            _page_size;
    bool              _use_large_pages;
    size_t            _default_pool_size;
    bool              _is_code;

    apr_pool_t* aux_pool;
    apr_thread_mutex_t* aux_mutex;

protected:
    inline void _lock();
    inline void _unlock();
    inline size_t round_up_to_page_size_multiple(size_t size);
};

class PoolManager : public BasePoolManager {
public:
    PoolManager(size_t initial_size, bool use_large_pages, bool is_code = false);
    virtual ~PoolManager();

    // alloc is synchronized inside the class
    void* alloc(size_t size, size_t alignment, Code_Allocation_Action action);

protected:
    PoolDescriptor*   _active_pool;
    PoolDescriptor*   _passive_pool;

protected:
    inline PoolDescriptor* allocate_pool_storage(size_t size); // allocate memory for new PoolDescriptor
};


class VirtualMemoryPool : public BasePoolManager {
public:
    VirtualMemoryPool(size_t initial_size, bool use_large_pages, bool is_code = false);
    virtual ~VirtualMemoryPool();

    // alloc is synchronized inside the class
    void* alloc(size_t size, size_t alignment, Code_Allocation_Action action);
    U_8* get_base();

protected:
    U_8*   _base;
    size_t _reserved;
    size_t _committed;
    size_t _allocated;
    port_vmem_t* _vmem;
};

#endif /* _MEM_ALLOC_H */
