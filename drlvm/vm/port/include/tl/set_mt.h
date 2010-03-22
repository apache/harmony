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
#ifndef TL_SET_MT_H
#define TL_SET_MT_H

// FIXME this should be thread safe vector implementation with
// custom memory allocator.
// But now this is just wrapper for the current STL vector implementation

#include <vector>
#include <algorithm> // strangely VC defines equal_range here

#include "log_macro.h"
#include "tl/allocator.h"
#include <apr_thread_mutex.h>
#include "tl/memory_pool.h"

namespace tl
{
    /**
     * A MemoryManager-based STL sorted vector container to use
     * as a set.
     */
    template<class T, class Allocator = MPAllocator<T> >
    class vector_set_mt : public ::std::vector<T, Allocator>
    {
        typedef ::std::vector<T, Allocator> std_vector;
        //MVM APN
        typedef typename std_vector::size_type size_type;
        apr_thread_mutex_t* mutex;
        MemoryPool mem_pool;
    public:

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

#if (defined PLATFORM_POSIX) || (defined __INTEL_COMPILER)
        //MVM APN 
        typedef typename std_vector::iterator iterator;
        typedef typename std_vector::const_iterator const_iterator;
#endif

        vector_set_mt(Allocator const& a = Allocator()) : std_vector(a) {
            if (apr_thread_mutex_create(&mutex, APR_THREAD_MUTEX_NESTED, mem_pool.get_pool())) {
                ABORT("Couldn't create mutex");
            }
        }
        vector_set_mt(size_type n, const T& x = T(), Allocator const& a = Allocator()) : std_vector(n, x, a) {
            if (apr_thread_mutex_create(&mutex, APR_THREAD_MUTEX_NESTED, mem_pool.get_pool())) {
                ABORT("Couldn't create mutex");
            }
        }
        vector_set_mt(const std_vector & a) :  std_vector(a) {
            if (apr_thread_mutex_create(&mutex, APR_THREAD_MUTEX_NESTED, mem_pool.get_pool())) {
                ABORT("Couldn't create mutex");
            }
            ::std::sort(std_vector::begin(), std_vector::end());
        }
        vector_set_mt& operator=(const std_vector & a) {
            std_vector::operator=(a);
            return *this;
        };
        vector_set_mt& operator=(const vector_set_mt & a) { 
            std_vector::operator=(a); 
            return *this; 
        };
        ::std::pair<iterator, bool> insert(const T& x) {
            lock();
            ::std::pair<iterator, iterator> found = equal_range(x);
            bool res = false;
            if (found.first == found.second) {
                std_vector::insert(found.second, x);
                found = equal_range(x);
                res = true;
            }
            unlock();
            return ::std::pair<iterator,bool>(found.first, res);
        };
        void insert(iterator pos, const T& x) {
            lock();
            ::std::pair<iterator, iterator> found = equal_range(x);
            if (found.first == found.second) {
                std_vector::insert(found.second, x);
            }
            unlock();
        }
        void insert(iterator i1, iterator i2) {
            lock();
            std_vector::insert(std_vector::end(), i1, i2);
            ::std::sort(std_vector::begin(), std_vector::end());
            unlock();
        }
        size_type erase(const T& x) { 
            lock();
            ::std::pair<iterator, iterator> found= equal_range(x);
            size_type delta = found.first - found.second;
            if (delta != 0) {
                std_vector::erase(found.first, found.second);
            }
            unlock();
            return delta;
        };
        void erase(iterator __position) { 
            lock();
            std_vector::erase(__position); 
            unlock();
        };
        void erase(iterator __first, iterator __last) {
            lock();
            std_vector::erase(__first, __last);
            unlock();
        }
        void clear() {
            lock();
            std_vector::clear();
            unlock();
        };
        size_type count(const T& x) const { 
            ::std::pair<const_iterator, const_iterator> found= equal_range(x);
            if (found.first != found.second) return 1;
            else return 0;
        }
        const_iterator lower_bound(const T& x) const {
            return ::std::lower_bound(std_vector::begin(), std_vector::end(), x);
        }
        iterator lower_bound(const T& x) {
            return ::std::lower_bound(std_vector::begin(), std_vector::end(), x);
        }
        const_iterator upper_bound(const T& x) const {
            return ::std::upper_bound(std_vector::begin(), std_vector::end(), x);
        }
        iterator upper_bound(const T& x) {
            return ::std::upper_bound(std_vector::begin(), std_vector::end(), x);
        }
        ::std::pair<const_iterator, const_iterator> equal_range(const T& x) const {
            return ::std::equal_range(std_vector::begin(), std_vector::end(), x);
        }
        ::std::pair<iterator, iterator> equal_range(const T& x) {
            return ::std::equal_range(std_vector::begin(), std_vector::end(), x);
        }
        const_iterator find(const T& x) const {
            ::std::pair<const_iterator, const_iterator> found= equal_range(x);
            if (found.first == found.second) return std_vector::end();
            else return found.first;
        }
        iterator find(const T& x) { 
            ::std::pair<iterator, iterator> found= equal_range(x);
            if (found.first == found.second) return std_vector::end();
            else return found.first;
        }
        bool has(const T& x) const {
            ::std::pair<const_iterator, const_iterator> found= equal_range(x);
            return (found.first != found.second);
        };
    };
    
} // tl

#endif // TL_SET_MT_H
