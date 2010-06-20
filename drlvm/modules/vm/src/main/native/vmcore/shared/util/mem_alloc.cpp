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
#define LOG_DOMAIN "vm.core"
#include "cxxlog.h"

#include <assert.h>

#include "environment.h"
#include "nogc.h"
#include "open/vm.h" // for the declaration of vm_get_vtable_base()
#include "mem_alloc.h"
#include "vm_stats.h"
#include "port_malloc.h"
#include "port_threadunsafe.h"

////////////////////////////////////////////////////////////
// allocation memory for code for stubs

void *malloc_fixed_code_for_jit(size_t size, size_t alignment, unsigned heat, Code_Allocation_Action action)
{
    return VM_Global_State::loader_env->GlobalCodeMemoryManager->alloc(size, alignment, action);
} //malloc_fixed_code_for_jit


////////////////////////////////////////////////////////////////////////////
//////////////////////MemoryManager ////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

BasePoolManager::BasePoolManager(size_t initial_size,
                                 bool use_large_pages,
                                 bool is_code) :
        _use_large_pages(use_large_pages),
        _default_pool_size(initial_size),
        _is_code(is_code)
{
    size_t* ps = port_vmem_page_sizes();

    _page_size = ps[0];

    if (_use_large_pages && ps[1] != 0)
        _page_size = ps[1];

    VERIFY_SUCCESS(apr_pool_create(&aux_pool, 0));
    VERIFY_SUCCESS(apr_thread_mutex_create(&aux_mutex, APR_THREAD_MUTEX_NESTED, aux_pool));

#ifdef VM_STATS
    VM_Statistics::get_vm_stats().number_memorymanager_created++;
#endif
}

BasePoolManager::~BasePoolManager()
{
    VERIFY_SUCCESS(apr_thread_mutex_destroy(aux_mutex));
    apr_pool_destroy(aux_pool);
}

void BasePoolManager::_lock()
{
    VERIFY_SUCCESS(apr_thread_mutex_lock(aux_mutex));
}

void BasePoolManager::_unlock()
{
    VERIFY_SUCCESS(apr_thread_mutex_unlock(aux_mutex));
}

size_t BasePoolManager::round_up_to_page_size_multiple(size_t size)
{
    return ((size + _page_size - 1) / _page_size) * _page_size;
}

PoolManager::PoolManager(size_t initial_size,
                         bool use_large_pages,
                         bool is_code) :
        BasePoolManager(initial_size, use_large_pages, is_code)
{
    _active_pool = allocate_pool_storage(_default_pool_size);
    _passive_pool = NULL;
}

PoolManager::~PoolManager()
{
    PoolDescriptor* pDesc = NULL;

    while (_passive_pool)
    {
        pDesc = _passive_pool;
        port_vmem_release(pDesc->_descriptor);
        _passive_pool = _passive_pool->_next;
    }

    while (_active_pool)
    {
        pDesc = _active_pool;
        port_vmem_release(pDesc->_descriptor);
        _active_pool = _active_pool->_next;
    }
}

PoolDescriptor* PoolManager::allocate_pool_storage(size_t size)
{
    PoolDescriptor* pDesc = (PoolDescriptor*) apr_palloc(aux_pool, sizeof(PoolDescriptor));
    memset(pDesc, 0, sizeof(PoolDescriptor));

    void *pool_storage = NULL;
    size = round_up_to_page_size_multiple(size);
    pDesc->_size = size;
     unsigned int mem_protection = PORT_VMEM_MODE_READ | PORT_VMEM_MODE_WRITE;
    if (_is_code) {
         mem_protection |= PORT_VMEM_MODE_EXECUTE;
    }

    size_t ps = (!_is_code && _use_large_pages) ?
         PORT_VMEM_PAGESIZE_LARGE : PORT_VMEM_PAGESIZE_DEFAULT;

    apr_status_t status = port_vmem_reserve(&pDesc->_descriptor, &pool_storage,
         size, mem_protection, ps, aux_pool);
    if (status != APR_SUCCESS)  {
         LDIE(27, "Cannot allocate pool storage: {0} bytes of virtual memory for code or data.\n"
             "Error code = {1}" << (void *)size << status);
     }

    status = port_vmem_commit(&pool_storage, size, pDesc->_descriptor);
    if (status != APR_SUCCESS || pool_storage == NULL)  {
         LDIE(27, "Cannot allocate pool storage: {0} bytes of virtual memory for code or data.\n"
             "Error code = {1}" << (void *)size << status);
     }

#ifdef VM_STATS
    VM_Statistics::get_vm_stats().number_memoryblock_allocations++;
    VM_Statistics::get_vm_stats().total_memory_allocated += size;
#endif

    pDesc->_begin  = (U_8*)pool_storage;
    pDesc->_end = (U_8*)(pool_storage) + size;

    return pDesc;
}

void* PoolManager::alloc(size_t size, size_t alignment, Code_Allocation_Action action)
{
    // Make sure alignment is a power of 2.
    assert((alignment & (alignment-1)) == 0);
    size_t mask = alignment - 1;

    // align the requested size
    size = (size + mask) & ~mask;

    // CAA_Simulate functionality support
    if (action == CAA_Simulate)
        size = 0;

    _lock();

    assert(_active_pool);
    U_8* pool_start = _active_pool->_begin;
    pool_start = (U_8*)((POINTER_SIZE_INT)(pool_start + mask) & ~(POINTER_SIZE_INT)mask);
    U_8* pool_end = _active_pool->_end;

    size_t mem_left_in_pool = (pool_end - pool_start);
    while (size > mem_left_in_pool) {
        // memory utilization logic
        // check that required size less than MEMORY_UTILIZATION_LIMIT % of active memory block size - all active memory
        // blocks have size more than MEMORY_UTILIZATION_LIMIT % of active memory block size
        PoolDescriptor* pDesc = _active_pool->_next;
        if (pDesc)
        {
            if ((size + mask)*MEMORY_UTILIZATION_LIMIT < (POINTER_SIZE_INT)(pDesc->_size))
            {
                _active_pool->_next = _passive_pool;
                _passive_pool = _active_pool;
                _active_pool = pDesc;

                pool_start = _active_pool->_begin;
                pool_start = (U_8*)((POINTER_SIZE_INT)(pool_start + mask) & ~(POINTER_SIZE_INT)mask);

                break;
            }
        }

        assert(_default_pool_size);
        size_t new_pool_size = ((size > _default_pool_size)? size : _default_pool_size);
        new_pool_size += mask;
        PoolDescriptor* p_pool = allocate_pool_storage(new_pool_size);
        assert (p_pool);

        // memory utilization logic
        // left size of pool more than MEMORY_UTILIZATION_LIMIT % of the pool's size
        if ((mem_left_in_pool * MEMORY_UTILIZATION_LIMIT) > _active_pool->_size) //put pool in _active_pool list
        {
            p_pool->_next = _active_pool;
            _active_pool = p_pool;
        }
        else // put in _passive_pool list
        {
            p_pool->_next = _active_pool->_next;
            _active_pool->_next = _passive_pool;
            _passive_pool = _active_pool;
            _active_pool = p_pool;
        }

        pool_start = p_pool->_begin;
        pool_start = (U_8*)((POINTER_SIZE_INT)(pool_start + mask) & ~(POINTER_SIZE_INT)mask);
        break;
     }
    void *p = pool_start;
    _active_pool->_begin += size;

    _unlock();

 #ifdef VM_STATS
    UNSAFE_REGION_START
    VM_Statistics::get_vm_stats().total_memory_used += size;
    UNSAFE_REGION_END
#endif

    return p;
 }

VirtualMemoryPool::VirtualMemoryPool(size_t initial_size,
                                     bool use_large_pages,
                                     bool is_code) :
        BasePoolManager(initial_size, use_large_pages, is_code),
        _base(NULL),
        _reserved(0),
        _committed(0),
        _allocated(0)
{
    void *pool_storage = NULL;
    _reserved = round_up_to_page_size_multiple(initial_size);

    unsigned int mem_protection = PORT_VMEM_MODE_READ | PORT_VMEM_MODE_WRITE;
    if (_is_code)
         mem_protection |= PORT_VMEM_MODE_EXECUTE;

    size_t ps = (!_is_code && _use_large_pages) ?
         PORT_VMEM_PAGESIZE_LARGE : PORT_VMEM_PAGESIZE_DEFAULT;

    apr_status_t status = port_vmem_reserve(&_vmem, (void**) &_base, _reserved,
            mem_protection, ps, aux_pool);
    if (status != APR_SUCCESS)  {
         LDIE(27, "Cannot allocate pool storage: {0} bytes of virtual memory for code or data.\n"
             "Error code = {1}" << (void *)_reserved << status);
    }

	assert(_vmem);
}

VirtualMemoryPool::~VirtualMemoryPool()
{
    port_vmem_release(_vmem);
}

void* VirtualMemoryPool::alloc(size_t size, size_t alignment, Code_Allocation_Action action)
{
    // Make sure alignment is a power of 2.
    assert((alignment & (alignment-1)) == 0);
    size_t mask = alignment - 1;

    // align the requested size
    size = (size + mask) & ~mask;

    // CAA_Simulate functionality support
    if (action == CAA_Simulate)
        size = 0;

    _lock();

    assert(_base);
    assert(_reserved);
    assert(_committed <= _reserved);
    assert(_allocated <= _committed);

    size_t new_allocated = _allocated + size;

    if (new_allocated > _committed) {
        apr_status_t status = APR_ENOMEM;

        size_t new_committed = round_up_to_page_size_multiple(new_allocated);

        if (new_committed <= _reserved) {
            U_8* commit_start = _base + _committed;
            status = port_vmem_commit((void**) &commit_start, new_committed - _committed, _vmem);
        }

        if (status != APR_SUCCESS)  {
             LDIE(27, "Cannot allocate pool storage: {0} bytes of virtual memory for code or data.\n"
                 "Error code = {1}" << (void *)size << status);
        }

        _committed = new_committed;

    }

    U_8* result = _base + _allocated;
    _allocated = new_allocated;

    _unlock();

    return result;
 }

U_8* VirtualMemoryPool::get_base()
{
    assert(_base);
    return _base;
}
