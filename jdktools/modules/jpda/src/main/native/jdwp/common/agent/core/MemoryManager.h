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
 * @author Pavel N. Vyssotski
 */

/**
 * @file
 * MemoryManager.h
 *
 */

#ifndef _MEMORY_MANAGER_H_
#define _MEMORY_MANAGER_H_

#include "AgentException.h"
#include "Log.h"

namespace jdwp {

    /**
     * Agent memory manager interface.
     */
    class MemoryManager {

    public:

        /**
         * Allocates the memory block with the given size without throwing
         * the <code>OutOfMemoryException</code> exception.
         *
         * @param size - the allocation size
         *
         * @return Pointer to the allocated memory block.
         */
        virtual void* AllocateNoThrow(size_t size
            JDWP_FILE_LINE_PAR) throw() = 0;

        /**
         * Allocates the memory block with the given size.
         *
         * @param size - the allocation size
         *
         * @return Pointer to the allocated memory block.
         *
         * @exception If memory cannot be allocated,
         *            <code>OutOfMemoryException</code> is thrown.
         */
        virtual void* Allocate(size_t size
            JDWP_FILE_LINE_PAR) throw(AgentException) = 0;

        /**
         * Reallocates the memory block with the given size over the previously
         * allocated block.
         *
         * @param size - the allocation size
         *
         * @return Pointer to the allocated memory block.
         *
         * @exception If memory cannot be allocated,
         *            <code>OutOfMemoryException</code> is thrown.
         */
        virtual void* Reallocate(void* ptr, size_t oldSize, size_t newSize
            JDWP_FILE_LINE_PAR) throw(AgentException) = 0;

        /**
         * Frees the memory block with the given size.
         *
         * @param ptr - the pointer to the allocated memory block
         */
        virtual void Free(void* ptr
            JDWP_FILE_LINE_PAR) throw() = 0;
    };

    /**
     * The standard memory manager implementation that uses 
     * allocation/deallocation functions from std namespace.
     */
    class STDMemoryManager : public MemoryManager {

    public:

        /**
         * Allocates the memory block with the given size without throwing
         * <code>OutOfMemoryException</code> exception.
         *
         * @param size - the allocation size
         *
         * @return Pointer to the allocated memory block.
         */
        void* AllocateNoThrow(size_t size
            JDWP_FILE_LINE_PAR) throw();

        /**
         * Allocates the memory block with the given size.
         *
         * @param size - the allocation size
         *
         * @return Pointer to the allocated memory block.
         *
         * @exception If memory cannot be allocated,
         *            <code>OutOfMemoryException</code> is thrown.
         */
        void* Allocate(size_t size
            JDWP_FILE_LINE_PAR) throw(AgentException);

        /**
         * Reallocates the memory block with the given size over the previously
         * allocated block.
         *
         * @param size - the allocation size
         *
         * @return Pointer to the allocated memory block.
         *
         * @exception If memory cannot be allocated,
         *            <code>OutOfMemoryException</code> is thrown.
         */
        void* Reallocate(void* ptr, size_t oldSize, size_t newSize
            JDWP_FILE_LINE_PAR) throw(AgentException);

        /**
         * Frees the memory block with the given size.
         *
         * @param ptr - the pointer to the allocated memory block
         */
        void Free(void* ptr
            JDWP_FILE_LINE_PAR) throw();
    };


    /**
     * The memory manager implementation that uses 
     * allocation/deallocation methods from the JVMTI environment.
     */
    class VMMemoryManager : public MemoryManager {

    public:

        /**
         * Allocates the memory block with the given size without throwing
         * <code>OutOfMemoryException</code> exception.
         *
         * @param size - the allocation size
         *
         * @return Pointer to the allocated memory block.
         */
        void* AllocateNoThrow(size_t size
            JDWP_FILE_LINE_PAR) throw();

        /**
         * Allocates the memory block with the given size.
         *
         * @param size - the allocation size
         *
         * @return Pointer to the allocated memory block.
         *
         * @exception If memory cannot be allocated,
         *            <code>OutOfMemoryException</code> is thrown.
         */
        void* Allocate(size_t size
            JDWP_FILE_LINE_PAR) throw(AgentException);

        /**
         * Reallocates the memory block with the given size over the previously
         * allocated block.
         *
         * @param size - the allocation size
         *
         * @return Pointer to the allocated memory block.
         *
         * @exception If memory cannot be allocated,
         *            <code>OutOfMemoryException</code> is thrown.
         */
        void* Reallocate(void* ptr, size_t oldSize, size_t newSize
            JDWP_FILE_LINE_PAR) throw(AgentException);

        /**
         * Frees the memory block with the given size.
         *
         * @param ptr - the pointer to the allocated memory block
         */
        void Free(void* ptr
            JDWP_FILE_LINE_PAR) throw();
    };
}

#endif // _MEMORY_MANAGER_H_
