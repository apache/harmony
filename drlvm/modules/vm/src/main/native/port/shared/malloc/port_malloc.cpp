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
 * @author Andrey Yakushev
 */


#ifdef WIN32
#include <stdio.h>
#include <malloc.h>
#include <limits.h>
#else
#include <stdlib.h>
#endif

#include "port_malloc.h"
#include "port_malloc_registrar.h"

#ifdef _MEMMGR

extern "C" {

#define MALLOC_LOG_FILE_NAME "malloc.log"

static size_t mem_used_size = 0;
static size_t mem_committed_size = 0;
static size_t mem_reserved_size = 0;
static size_t mem_max_size = UINT_MAX;

static MallocRegistrar* malloc_registrar;

void start_monitor_malloc() {
    malloc_registrar = new MallocRegistrar();
}

void report_leaked_malloc() {
    malloc_registrar->report();
}

size_t port_mem_used_size() {
    return mem_used_size;
}

size_t port_mem_committed_size() {
    return mem_committed_size;
}

size_t port_mem_reserved_size() {
    return mem_reserved_size;
}

size_t port_mem_max_size() {
    return mem_max_size;
}


APR_DECLARE(void*) port_malloc(size_t NBYTES, char *file_name, int file_line) {
    void *result = malloc(NBYTES);
    mem_used_size += NBYTES;
    if (NULL != malloc_registrar) {
        malloc_registrar->register_chunk(result, new ChunkAttributes(NBYTES, file_name, file_line));
        MEMMGR_LOG((malloc_registrar->f_malloc_log,
            "malloc(%08u)=%08x from %s:%u\n",
            NBYTES,
            result,
            file_name,
            file_line));
        fflush(malloc_registrar->f_malloc_log);
    }
    return result;
}

APR_DECLARE(void) port_free(void *APTR, char *file_name, int file_line) {
    if (NULL == malloc_registrar) return;
    ChunkAttributes* attr = malloc_registrar->unregister_chunk(APTR);
    if (NULL == attr) {
        if (NULL == APTR) return;
        MEMMGR_LOG((malloc_registrar->f_malloc_log,
            "Probably double free call for %08x from %s:%u\n",
            APTR,
            file_name,
            file_line));
        fflush(malloc_registrar->f_malloc_log);
        return;
    }
    free(APTR);
    mem_used_size -= attr->size;
    MEMMGR_LOG((malloc_registrar->f_malloc_log,
        "free(%08x) at %s:%u\n    malloc(%08u) from %s:%u\n    current allocated size = %u\n",
        APTR,
        file_name,
        file_line,
        attr->size,
        attr->file_name,
        attr->file_line,
        mem_used_size));
    fflush(malloc_registrar->f_malloc_log);
    free(attr);
}

} // extern "C"

#endif // _MEMMGR
