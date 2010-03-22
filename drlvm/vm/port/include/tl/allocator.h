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
#ifndef TL_ALLOCATOR_H
#define TL_ALLOCATOR_H

#include "port_malloc.h"
#include "tl/memory_pool.h"

namespace tl
{
    /**
     * An allocator based on the basic memory allocation primitives. 
     */
    template <class T>
    class BasicAllocator
    {
    public:
        // Default constructor.
        BasicAllocator() {}

        // Copy constructor.
        template <class U> BasicAllocator(const BasicAllocator<U>& allocator) {}

        // Destructor.
        ~BasicAllocator() {}

        // Underlying pointer, reference, etc types for this allocator.
        typedef T* pointer;
        typedef const T* const_pointer;
        typedef T& reference;
        typedef const T& const_reference;
        typedef T value_type;
        typedef size_t size_type;
        typedef ptrdiff_t difference_type;

        // Pointer/reference conversion.
        pointer address(reference x) const { return &x; }
        const_pointer address(const_reference x) const { return &x; }

        // Allocation/deallocation operations.
        pointer allocate(size_type n, const void* = 0)
        {
            pointer p = (pointer) STD_MALLOC(n * sizeof(T));
            LOG_ASSERT(p != NULL, "Out of memory");
            return p;
        }
                void deallocate(void * UNREF p, size_type) 
        {
            //STD_FREE(p);
        }

        // Maximum allocatable size based upon size_type.
        size_type max_size() const { return ((size_type) -1) / sizeof(value_type); }

        // Initialization/finalization operations.
        void construct(pointer p, const value_type& x) { new (p) value_type(x); }
        void destroy(pointer p) { p->~value_type(); }

        // Allocator equality tests
        template <class U>
        bool operator==(const BasicAllocator<U>& allocator) { return TRUE; }
        template <class U>
        bool operator!=(const BasicAllocator<U>& allocator) { return FALSE; }

        // Type conversion utility to obtain BasicAllocator for different underlying type.
        template <class U> struct rebind { typedef BasicAllocator<U> other; };

    };

    
    /**
     * A MemoryPool based STL allocator.                                                                     
     */
    template <class T>
    class MPAllocator 
    {
    public:
        // Standard constructor
        MPAllocator(MemoryPool& mp) : pmp(&mp) {}

        // Copy constructor.
        template <class U> MPAllocator(const MPAllocator<U>& allocator) : 
        pmp(&allocator.getMemoryPool()) {}

        // Destructor.
        ~MPAllocator() {}

        // Underlying pointer, reference, etc types for this allocator.
        typedef T* pointer;
        typedef const T* const_pointer;
        typedef T& reference;
        typedef const T& const_reference;
        typedef T value_type;
        typedef size_t size_type;
        typedef ptrdiff_t difference_type;

        // Pointer/reference conversion.
        pointer address(reference x) const { return &x; }
        const_pointer address(const_reference x) const { return &x; }

        // Underlying MemoryPool.
        MemoryPool& getMemoryPool() const { return *pmp; }

        // Allocation/deallocation operations.
        pointer allocate(size_type n, const void* = 0)
        {
            pointer p = (pointer) pmp->alloc(n * sizeof(T));
            LOG_ASSERT(p != NULL, "Out of memory");
            return p;
        }
        void deallocate(void *p, size_type) {}

        // Maximum allocatable size based upon size_type.
        size_type max_size() const { return ((size_type) -1) / sizeof(value_type); }

        // Initialization/finalization operations.
        void construct(pointer p, const value_type& x) { new (p) value_type(x); }
        void destroy(pointer p) { p->~value_type(); }

        // Allocator equality tests
        template <class U>
        bool operator==(const MPAllocator<U>& allocator) { return pmp == allocator.pmp; }
        template <class U>
        bool operator!=(const MPAllocator<U>& allocator) { return pmp != allocator.pmp; }

        // Type conversion utility to obtain MPAllocator for different underlying type.
        template <class U> struct rebind { typedef MPAllocator<U> other; };

    private:
        // Disable.  Cannot be instantiated without a MemoryPool.
        MPAllocator();

        MemoryPool* pmp;
    };

} //namespace tl

#endif // TL_ALLOCATOR_H
