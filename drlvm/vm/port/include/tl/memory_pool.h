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
#ifndef TL_MEMORY_POOL_H
#define TL_MEMORY_POOL_H
#include "cxxlog.h"

#include <apr_pools.h>
#include "open/types.h"

namespace tl {

class VMEXPORT MemoryPool {
private:
    // denied functions
    MemoryPool(const MemoryPool&) {LDIE(51, "Not implemented");}
    MemoryPool& operator=(const MemoryPool&) {LDIE(51, "Not implemented"); return *this;}
protected:
    apr_pool_t* pool;
public:
    MemoryPool();
    MemoryPool(const MemoryPool * parent);
    ~MemoryPool();
    void *alloc(size_t size);
    apr_status_t create_mutex(apr_thread_mutex_t**, unsigned int flags);
};

class VMEXPORT MemoryPoolMT {
private:
    // denied functions
    MemoryPoolMT(const MemoryPoolMT&) {LDIE(51, "Not implemented");}
    MemoryPoolMT& operator=(const MemoryPoolMT&) {LDIE(51, "Not implemented"); return *this;}

    MemoryPool unsync_pool;
    apr_thread_mutex_t *mutex;
public:
    MemoryPoolMT();
    MemoryPoolMT(const MemoryPool* parent);
    MemoryPoolMT(const MemoryPoolMT* parent);
    ~MemoryPoolMT();

    /**
     * Thread safe memory allocation.
     */
    void *alloc(size_t size);

    /**
     * Thread safe mutex allocation / creation.
     */
    apr_status_t create_mutex(apr_thread_mutex_t**, unsigned int flags);
};

} //namespace tl 

#endif  // TL_MEMORY_POOL_H
