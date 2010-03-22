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
// MemoryManager.cpp

#include <cstdlib>
#include <cstring>

#include "AgentException.h"
#include "MemoryManager.h"
#include "AgentBase.h"
//#include "Log.h"
//#include "jvmti.h"

using namespace jdwp;

// STDMemoryManager intended to use std::malloc(), std::free() etc.

void* STDMemoryManager::AllocateNoThrow(size_t size JDWP_FILE_LINE_PAR) throw() {
    void *p = std::malloc(size);
    JDWP_TRACE_EX(LOG_KIND_MEMORY, file, line, "STD malloc: " << static_cast<long long>(size) << " " << p);
    return p;
}

void* STDMemoryManager::Allocate(size_t size JDWP_FILE_LINE_PAR) throw(AgentException) {
    void *p = std::malloc(size);
    JDWP_TRACE_EX(LOG_KIND_MEMORY, file, line, "STD malloc: " << static_cast<long long>(size) << " " << p);
    if (p == 0) {
        throw OutOfMemoryException();
    }
    return p;
}

void* STDMemoryManager::Reallocate(void* ptr, size_t oldSize, size_t newSize JDWP_FILE_LINE_PAR)
        throw(AgentException) {
    void *p = std::realloc(ptr, newSize);
    JDWP_TRACE_EX(LOG_KIND_MEMORY, file, line, "STD realloc: " << ptr << " " << static_cast<long long>(oldSize)
        << "/" << static_cast<long long>(newSize) << " " << p);
    if (p == 0) {
        throw OutOfMemoryException();
    }
    return p;
}

void STDMemoryManager::Free(void* ptr JDWP_FILE_LINE_PAR) throw() {
    JDWP_TRACE_EX(LOG_KIND_MEMORY, file, line, "STD free: " << ptr);
    std::free(ptr);
}


// VMMemoryManager intended to use JVMTI's Allocate() and Deallocate()

void* VMMemoryManager::AllocateNoThrow(size_t size JDWP_FILE_LINE_PAR) throw() {
    void *p;

    jvmtiError err;
    JVMTI_TRACE(err, AgentBase::GetJvmtiEnv()->Allocate(size,
        reinterpret_cast<unsigned char**>(&p)));
    JDWP_TRACE_EX(LOG_KIND_MEMORY, file, line, "VM malloc: " << static_cast<long long>(size) << ", " << p);
    return ((err == JVMTI_ERROR_NONE) ? p : 0);
}

void* VMMemoryManager::Allocate(size_t size JDWP_FILE_LINE_PAR) throw(AgentException) {
    void *p;

    jvmtiError err;
    JVMTI_TRACE(err, AgentBase::GetJvmtiEnv()->Allocate(size,
        reinterpret_cast<unsigned char**>(&p)));
    JDWP_TRACE_EX(LOG_KIND_MEMORY, file, line, "VM malloc: " << static_cast<long long>(size) << ", " << p);
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }

    return p;
}

void* VMMemoryManager::Reallocate(void* ptr, size_t oldSize, size_t newSize JDWP_FILE_LINE_PAR)
        throw(AgentException) {
    void *p;

    jvmtiError err;
    JVMTI_TRACE(err, AgentBase::GetJvmtiEnv()->Allocate(newSize,
        reinterpret_cast<unsigned char**>(&p)));
    JDWP_TRACE_EX(LOG_KIND_MEMORY, file, line, "VM realloc: " << ptr << " " << static_cast<long long>(oldSize)
        << "/" << static_cast<long long>(newSize) << " " << p);
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    } else {
        std::memcpy(p, ptr, (newSize < oldSize) ? newSize : oldSize);
        JVMTI_TRACE(err, AgentBase::GetJvmtiEnv()->Deallocate(
            reinterpret_cast<unsigned char*>(ptr)));
        JDWP_ASSERT(err==JVMTI_ERROR_NONE);
    }

    return p;
}

void VMMemoryManager::Free(void* ptr JDWP_FILE_LINE_PAR) throw() {
    JDWP_TRACE_EX(LOG_KIND_MEMORY, file, line, "VM free: " << ptr);
    jvmtiError err;
    JVMTI_TRACE(err, AgentBase::GetJvmtiEnv()->Deallocate(
        reinterpret_cast<unsigned char*>(ptr)));
    JDWP_ASSERT(err==JVMTI_ERROR_NONE);
}
