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
 * @author Ilya Berezhniuk
 */

#ifndef _PORT_MODULES_H_
#define _PORT_MODULES_H_

#include <stddef.h>
#include <stdio.h>
#include "open/platform_types.h"
#include "port_general.h"


typedef enum {
    SEGMENT_TYPE_UNKNOWN,
    SEGMENT_TYPE_CODE,
    SEGMENT_TYPE_DATA
} native_seg_type_t;

typedef struct {
    native_seg_type_t   type;
    void*               base;
    size_t              size;
} native_segment_t;

typedef struct native_module_t native_module_t;

struct native_module_t {
    char*               filename;
    size_t              seg_count;
    native_module_t*    next;
    native_segment_t    segments[1];
};


#ifdef __cplusplus
extern "C" {
#endif


/**
* Returns the list of modules loaded to the current process.
* Module includes one or more segments of type <code>native_segment_t</code>.
* @param list_ptr  - an address of modules list pointer to fill
* @param count_ptr - count of modules in the returned list
* @return <code>TRUE</code> if OK; FALSE if error occured.
*/
VMEXPORT Boolean port_get_all_modules(native_module_t** list_ptr, int* count_ptr);

/**
* Dumps the list of modules loaded to the current process..
* @param modules  - pointer to the list of modules to dump.
* @param out      - stream for printing the dump.
*/
VMEXPORT void port_dump_modules(native_module_t* modules, FILE *out);

/**
* Clears the list of modules passed, writes NULL to the poiner.
* @param modules  - pointer to the list of modules to clear.
*/
VMEXPORT void port_clear_modules(native_module_t** list_ptr);

/**
* Searches for the specific address in the list of modules.
* @param modules   - pointer to the list of modules to inspect.
* @param code_ptr  - the address to look for.
* @return <code>native_module_t</code> pointer if OK; otherwise, NULL.
*/
VMEXPORT native_module_t* port_find_module(native_module_t* modules, void* code_ptr);


#ifdef __cplusplus
}
#endif

#endif // _PORT_MODULES_H_
