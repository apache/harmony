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

#include "MemoryManager.h"
#include "ExceptionManager.h"
#include "AgentBase.h"
#include "Log.h"
//#include "jvmti.h"

#include <string.h>
#include <stdlib.h> 

using namespace jdwp;

// should never be invoked
void *operator new(size_t size)
{
    void* p = malloc(size);
    JDWP_TRACE(LOG_RELEASE, (LOG_KIND_MEMORY, __FILE__, __LINE__, "VM malloc: %lld, %p", static_cast<long long>(size), p));
    return p;
}

// should never be invoked
void operator delete(void *p)
{
    JDWP_TRACE(LOG_RELEASE, (LOG_KIND_MEMORY, __FILE__, __LINE__, "VM free: %p", p));
    free(p);
}

// STDMemoryManager intended to use malloc(), free() etc.

void* STDMemoryManager::AllocateNoThrow(size_t size JDWP_FILE_LINE_PAR) {
    void *p = malloc(size);
    JDWP_TRACE(LOG_RELEASE, (LOG_KIND_MEMORY, file, line, "STD malloc: %lld %p", static_cast<long long>(size), p));
    return p;
}

void* STDMemoryManager::Allocate(size_t size JDWP_FILE_LINE_PAR) {
    void *p = malloc(size);
    JDWP_TRACE(LOG_RELEASE, (LOG_KIND_MEMORY, file, line, "STD malloc: %lld %p", static_cast<long long>(size), p));
    if (p == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_KIND_ERROR, file, line, "STD malloc failed: %lld %p", static_cast<long long>(size), p));
    }
    return p;
}

void* STDMemoryManager::Reallocate(void* ptr, size_t oldSize, size_t newSize JDWP_FILE_LINE_PAR)
        {
    void *p = realloc(ptr, newSize);
    JDWP_TRACE(LOG_RELEASE, (LOG_KIND_MEMORY, file, line, "STD realloc: %p %lld/%lld %p", ptr, static_cast<long long>(oldSize),
        static_cast<long long>(newSize), p));
    if (p == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_KIND_ERROR, file, line, "STD realloc failed: %p %lld/%lld %p", ptr, static_cast<long long>(oldSize),
                                 static_cast<long long>(newSize), p));
    }    
    return p;
}

void STDMemoryManager::Free(void* ptr JDWP_FILE_LINE_PAR) {
    JDWP_TRACE(LOG_RELEASE, (LOG_KIND_MEMORY, file, line, "STD free: %p", ptr));
    free(ptr);
}


// VMMemoryManager intended to use JVMTI's Allocate() and Deallocate()

void* VMMemoryManager::AllocateNoThrow(size_t size JDWP_FILE_LINE_PAR) {
    void *p;

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, AgentBase::GetJvmtiEnv()->Allocate(size,
        reinterpret_cast<unsigned char**>(&p)));
    JDWP_TRACE(LOG_RELEASE, (LOG_KIND_MEMORY, file, line, "VM malloc: %lld, %p", static_cast<long long>(size), p));
    return ((err == JVMTI_ERROR_NONE) ? p : 0);
}

void* VMMemoryManager::Allocate(size_t size JDWP_FILE_LINE_PAR) {
    void *p;

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, AgentBase::GetJvmtiEnv()->Allocate(size,
        reinterpret_cast<unsigned char**>(&p)));
    JDWP_TRACE(LOG_RELEASE, (LOG_KIND_MEMORY, file, line, "VM malloc: %lld, %p", static_cast<long long>(size), p));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_KIND_ERROR, file, line, "VM malloc failed: %lld, %p", static_cast<long long>(size), p));
        AgentException ex(err);
        JDWP_SET_EXCEPTION(ex);
        return 0;
    }
    return p;
}

void* VMMemoryManager::Reallocate(void* ptr, size_t oldSize, size_t newSize JDWP_FILE_LINE_PAR)
        {
    void *p;

    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, AgentBase::GetJvmtiEnv()->Allocate(newSize,
        reinterpret_cast<unsigned char**>(&p)));
    JDWP_TRACE(LOG_RELEASE, (LOG_KIND_MEMORY, file, line, "VM realloc: %p %lld/%lld %p", ptr, static_cast<long long>(oldSize),
        static_cast<long long>(newSize), p));
    if (err != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_KIND_ERROR, file, line, "VM realloc failed: %p %lld/%lld %p", ptr, static_cast<long long>(oldSize),
                                 static_cast<long long>(newSize), p));
        AgentException ex(err);
        JDWP_SET_EXCEPTION(ex);
        return 0;
    } else {
        memcpy(p, ptr, (newSize < oldSize) ? newSize : oldSize);
        JVMTI_TRACE(LOG_DEBUG, err, AgentBase::GetJvmtiEnv()->Deallocate(
            reinterpret_cast<unsigned char*>(ptr)));
        JDWP_ASSERT(err==JVMTI_ERROR_NONE);
    }

    return p;
}

void VMMemoryManager::Free(void* ptr JDWP_FILE_LINE_PAR) {
    JDWP_TRACE(LOG_RELEASE, (LOG_KIND_MEMORY, file, line, "VM free: %p", ptr));
    jvmtiError err;
    JVMTI_TRACE(LOG_DEBUG, err, AgentBase::GetJvmtiEnv()->Deallocate(
        reinterpret_cast<unsigned char*>(ptr)));
    JDWP_ASSERT(err==JVMTI_ERROR_NONE);
}
