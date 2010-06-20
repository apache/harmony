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
#include <malloc.h>
#include <limits.h>
#else
#include <stdlib.h>
#endif

#include "port_malloc_registrar.h"
#include "port_malloc.h"

#ifdef _MEMMGR

ChunkAttributes::ChunkAttributes(size_t size, char *file_name, int file_line) {
    this->size = size;
    this->file_name = file_name;
    this->file_line = file_line;
}

MallocRegistrar::MallocRegistrar() {
#ifdef _MEMMGR_LOG_OR_REPORT
    f_malloc_log = fopen(MALLOC_LOG_FILE_NAME, "w");
    if( f_malloc_log == NULL) {
        f_malloc_log = stdout;
    };
    MEMMGR_LOG((f_malloc_log, "MallocRegistrar::MallocRegistrar invoked\n"));
#endif
    mallocCellar = new MallocCellar();
    mutex = CreateMutex(NULL, FALSE, NULL);
}

MallocRegistrar::~MallocRegistrar() {
    MEMMGR_LOG((f_malloc_log, "MallocRegistrar::~MallocRegistrar invoked\n"));
    fflush(f_malloc_log);
    mutex = NULL;
}

void MallocRegistrar::report() {
    if (mutex == NULL) return;
    WaitForSingleObject(mutex, 0);
    MEMMGR_LOG((f_malloc_log, "Malloc leakage report:\n"));
    for (MallocCellarCI i = mallocCellar->begin(); i!=mallocCellar->end(); ++i) {
        if (NULL == i->second) {
            fprintf(f_malloc_log, "NULL record in mallocCellar\n");
            return;
        }
        ChunkAttributes* attr = i->second;
        fprintf(f_malloc_log, "unfreed pointer %08x size %08u from %s:%u\n", i->first, attr->size, attr->file_name, attr->file_line);
    };
    fflush(f_malloc_log);
    ReleaseMutex(mutex);
}

void MallocRegistrar::register_chunk(void *APTR, ChunkAttributes* attr){
    if (mutex == NULL) return;
    WaitForSingleObject(mutex, 0);
    MallocCellarCI i = mallocCellar->find(APTR);
	if (i != mallocCellar->end()) { // found
        MEMMGR_LOG((f_malloc_log, "Such chunk already exists\n"));
        fflush(f_malloc_log);
		return; 
	}
    (*mallocCellar)[APTR] = attr;
    ReleaseMutex(mutex);
}

ChunkAttributes* MallocRegistrar::unregister_chunk(void *APTR){
    if (mutex == NULL) return NULL;
    WaitForSingleObject(mutex, 0);
    MallocCellarCI i = mallocCellar->find(APTR);
    if (i == mallocCellar->end()) return NULL; // not found
    ChunkAttributes* result = i->second;
    mallocCellar->erase(APTR);
    ReleaseMutex(mutex);
    return result;
}

#endif // _MEMMGR
