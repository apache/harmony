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
#define LOG_DOMAIN "tl.memory"
#include "tl/memory_pool.h"

tl::MemoryPool::MemoryPool()
{
    VERIFY_SUCCESS(apr_initialize());
    VERIFY_SUCCESS(apr_pool_create(&pool, NULL));
}

tl::MemoryPool::MemoryPool(const MemoryPool * parent)
{
    VERIFY_SUCCESS(apr_pool_create(&pool, parent->pool));
}

tl::MemoryPool::~MemoryPool()
{
    apr_pool_destroy(pool);
    // decrease number of APR instances
    apr_terminate();
}

void * tl::MemoryPool::alloc(size_t size)
{
    return apr_palloc(pool, size);
}

apr_status_t tl::MemoryPool::create_mutex(apr_thread_mutex_t** mutex, unsigned int flags) {
    return apr_thread_mutex_create(mutex, flags, pool);
}


tl::MemoryPoolMT::MemoryPoolMT()
{
    VERIFY_SUCCESS(unsync_pool.create_mutex(&mutex, APR_THREAD_MUTEX_UNNESTED));
}

tl::MemoryPoolMT::MemoryPoolMT(const MemoryPoolMT * parent) :
    unsync_pool(&parent->unsync_pool)
{
    VERIFY_SUCCESS(unsync_pool.create_mutex(&mutex, APR_THREAD_MUTEX_UNNESTED));
}

tl::MemoryPoolMT::MemoryPoolMT(const MemoryPool * parent) : unsync_pool(parent)
{
    VERIFY_SUCCESS(unsync_pool.create_mutex(&mutex, APR_THREAD_MUTEX_UNNESTED));
}

tl::MemoryPoolMT::~MemoryPoolMT()
{
    VERIFY_SUCCESS(apr_thread_mutex_destroy(mutex));
}

void * tl::MemoryPoolMT::alloc(size_t size)
{
    apr_thread_mutex_lock(mutex);
    void* ptr = unsync_pool.alloc(size);
    apr_thread_mutex_unlock(mutex);
    return ptr;
}

apr_status_t tl::MemoryPoolMT::create_mutex(apr_thread_mutex_t** m, unsigned int flags) {
    apr_thread_mutex_lock(mutex);
    apr_status_t res = unsync_pool.create_mutex(m, flags);
    apr_thread_mutex_unlock(mutex);
    return res;
}
