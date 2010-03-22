/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @file
 * AgentAllocator.h
 *
 */

#ifndef _AGENT_ALLOCATOR_H_
#define _AGENT_ALLOCATOR_H_

namespace jdwp {

    /**
     * Agent allocator template.
     */
    template <class T>
    class AgentAllocator : public allocator<T> {

    public:

        typedef T*        pointer;
        typedef const T*  const_pointer;
        typedef T&        reference;
        typedef const T&  const_reference;
        typedef T         value_type;
        typedef size_t    size_type;
        typedef ptrdiff_t difference_type;

        /**
         * Agent allocator rebind.
         */

        template <class U> struct rebind {
            typedef AgentAllocator<U> other;
        };

        /**
         * A constructor.
         */
        AgentAllocator() { }

        /**
         * A copy constructor.
         */
        template <class U>
        AgentAllocator(const AgentAllocator<U> &copy) { }

        pointer allocate(size_t n, const void* = 0) {
            return reinterpret_cast<pointer>(
                AgentBase::GetMemoryManager().Allocate(n * sizeof(T) JDWP_FILE_LINE));
        }

        void deallocate(pointer p, size_t n) {
            AgentBase::GetMemoryManager().Free(p JDWP_FILE_LINE);
        }

    };

} // namespace jdwp

#endif // _AGENT_ALLOCATOR_H_
