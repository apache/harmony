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
#ifndef _PORT_MALLOC_REGISTRAR_H_
#define _PORT_MALLOC_REGISTRAR_H_

#ifdef WIN32
    #include <malloc.h>
#else
    #include <stdio.h>
    #include <stdlib.h>
    #include <pthread.h>
#endif

#include <map>
#include <apr_thread_mutex.h>

/**
 * Memory block descriptor. 
 */
class ChunkAttributes {
public:
    /** Size of memory block */
    size_t size;

    /** file name where <code>port_malloc</code> is called */
    char *file_name;

    /** file line number where <code>port_malloc</code> is called */
    int file_line;

    /**
     * Constructor for memory block descriptor. Initializes the field
     * corresponding to arguments.
     * @param[in] size        - size in bytes
     * @param[in] file_name   - file name where this block is created
     * @param[in] file_line   - file line number where this block is created
     */
    ChunkAttributes(size_t size, char *file_name, int file_line);
};

/**
 * Map for storing memory block descriptions
 * @see ChunkAttributes
 */
typedef std::map<void* ,ChunkAttributes*> MallocCellar;

/**
 * Constant iterator for MallocCellar
 * @see MallocCellar
 */
typedef std::map<void* ,ChunkAttributes*>::const_iterator MallocCellarCI;

/**
 * Memory blocks registerer and statistics and problems reports provider
 */
class MallocRegistrar {
public:
    /**
     * Initializes log
     */
    MallocRegistrar();

    /**
     * Stop and flash logging and registering
     */
    ~MallocRegistrar();

    /**
     * Stores memory block info. 
     * @param[in] APTR        - address of memory block which info would be stored
     * @param[in] attr        - memory block attributes for storing
     * @see ChunkAttributes
     */
    void register_chunk(void *APTR, ChunkAttributes* attr);

    /**
     * Removes memory block info. 
     * @param[in] APTR        - address of memory block which info would be removed
     * @return  attributes</code> for removed memory block
     * @see ChunkAttributes
     */
    ChunkAttributes* unregister_chunk(void *APTR);

    /**
     * Reports info for all currently stores memory blocks. If called at the end of
     * program it contains list of leacked memory
     */
    void report();

    /**
     * Report file handler for reporting. Defailt is defined in MALLOC_LOG_FILE_NAME
     * @see MALLOC_LOG_FILE_NAME
     */
    FILE *f_malloc_log;
private:
    MallocCellar* mallocCellar;
#ifdef WIN32
    HANDLE mutex;
#else
    pthread_mutex_t mutex;
#endif
        
};


#endif // _PORT_MALLOC_REGISTRAR_H_
