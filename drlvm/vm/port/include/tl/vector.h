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
#ifndef TL_VECTOR_H
#define TL_VECTOR_H

/**
 * This is just wrapper for the current STL vector
 * implementation
 * with default memory allocator replaced.
 */
#include "tl/allocator.h"
#include <vector>

namespace tl
{
    /**
     * A MemoryManager-based STL vector container.
     */
    template<class T, class Allocator = BasicAllocator<T> >
    class vector : public ::std::vector<T, Allocator>
    {
        typedef ::std::vector<T, Allocator> std_vector;
    public:
        //MVM APN
        // typedef typename std_vector::size_type size_type;

        vector(Allocator const& a): std_vector(a) {}
        // vector(Allocator const& a, size_type n, const T& x = T()): std_vector(n, x, a) {}

    };

} // tl

#endif // TL_VECTOR_H
