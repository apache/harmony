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
 * @author Evgueni Brevnov
 */  

#include <assert.h>

#include "dump.h"
#include <apr_file_io.h>
#include <apr_pools.h>
#include "port_disasm.h"


// FIXME: this should not be a global variable
// this variable is filled in by parse_args()
const char * dump_file_name = "file.dump";

int dump(const char * code, const char * name, size_t length) {
    static apr_pool_t * pool  = NULL;
    static port_disassembler_t * disassembler;
    static apr_file_t * file = NULL;
    apr_status_t stat;
    
    if (!pool && (stat = apr_pool_create(&pool, NULL)) != APR_SUCCESS) {
        return stat;
    }

    if (!disassembler && (stat = port_disassembler_create(&disassembler, pool)) != APR_SUCCESS) {
        apr_pool_destroy(pool);
        return stat;
    }

    if (!file && (stat = apr_file_open(&file, dump_file_name,
            APR_FOPEN_READ | APR_FOPEN_WRITE | APR_FOPEN_CREATE /*| APR_FOPEN_APPEND*/,
            APR_FPROT_UREAD | APR_FPROT_UWRITE, pool)) != APR_SUCCESS) {
        apr_pool_destroy(pool);
        return stat;
    }

    apr_file_printf(file, "Function dump begin: %s\n", name);
    // FIXME64: no support for large methods
    // with compiled code size greater than 2GB
    port_disasm_to_file(disassembler, code, (int)length, file);
    apr_file_printf(file, "Function dump end: %s\n", name);

//    apr_pool_destroy(pool);
    return APR_SUCCESS;
}


