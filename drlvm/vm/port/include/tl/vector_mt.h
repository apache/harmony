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
 * @author Intel, Evgueni Brevnov
 */  
#ifndef TL_VECTOR_MT_H
#define TL_VECTOR_MT_H

// FIXME this should be thread safe vector implementation with
// custom memory allocator.
// But now this is just wrapper for the current STL vector
// implementation

#include "tl/vector.h"
#include "log_macro.h"
#include <apr_thread_mutex.h>
#include "tl/memory_pool.h"


namespace tl
{
    /**
     * Thread safe vector.
     */
    template<class T, class Allocator = MPAllocator<T> >
    class vector_mt : public ::std::vector<T, Allocator>
    {
        typedef ::std::vector<T, Allocator> std_vector;
        apr_thread_mutex_t* mutex;
        MemoryPool mem_pool;
    public:
        //MVM APN
        typedef typename std_vector::size_type size_type;
        
        vector_mt(Allocator const& a = Allocator()): std_vector(a) {
            if (apr_thread_mutex_create(&mutex, APR_THREAD_MUTEX_NESTED, mem_pool.get_pool())) {
                ABORT("Couldn't create mutex");
            }
        }
        vector_mt(size_type n, const T& x = T(),Allocator const& a = Allocator()): std_vector(n, x, a) {
            if (apr_thread_mutex_create(&mutex, APR_THREAD_MUTEX_NESTED, mem_pool.get_pool())) {
                ABORT("Couldn't create mutex");
            }
        }
        /**
         * Lock container access. Use it before accessing iterators.
         */
        void lock() {
            apr_thread_mutex_lock(mutex);
        }
        /**
         * Unock container access.
         */
        void unlock() {
            apr_thread_mutex_unlock(mutex);
        }
        /**
         * Add an element to the end of the vector.
         */
        void push_back(const T& val) {
            lock();
            std_vector::push_back(val);
            unlock();
        }

    };
} // tl

#endif // TL_VECTOR_MT_H
