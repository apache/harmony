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

#include <stdarg.h>
#include <string.h>
#include <assert.h>
#include <stdlib.h>

#include <apr_dso.h>
#include <apr_strings.h>
#include <apr_portable.h>

#include "port_disasm.h"

// this is mostly imperical data 
#if defined(_IA32_)
    #define ADDR_SIZE       16
    #define MNEMONIC_SIZE   15
    #define BYTES_PER_LINE  2
    #define BYTES_TOTAL     100
    //BYTES_SIZE = BYTES_PER_LINE * 3
    #define BYTES_SIZE      6
#else
    #define ADDR_SIZE       0
    #define MNEMONIC_SIZE   0
    #define BYTES_PER_LINE  0
    #define BYTES_TOTAL     0
    #define BYTES_SIZE      0
#endif

typedef int (* fprinf_func_t)(port_disassembler_t *, const char *, ...);

/*    Private Interface    */

struct port_disassembler_t {
    fprinf_func_t fprintf_func;
    port_disasm_info_t port_info;
    int line_size;
    char * real_stream;
    apr_pool_t * user_pool;
    apr_file_t * user_file;
    apr_size_t num_bytes_total;
    apr_size_t num_bytes_used;
};

#if defined(_IA32_)

/*    General printing routines    */

static int disasm_sprint_default(port_disassembler_t * disassembler, const char * fmt, ...) {
    int required_length;
    va_list args;
    va_start(args, fmt);
    required_length = apr_vsnprintf(NULL, 0, fmt, args);
    assert(required_length >= 0);
    // insure space
    while ((unsigned int)required_length >= disassembler->num_bytes_total -
             disassembler->num_bytes_used) {
        void * buf = malloc(disassembler->num_bytes_used);
        memcpy(buf, disassembler->real_stream, disassembler->num_bytes_used);
        apr_pool_clear(disassembler->user_pool);
        disassembler->num_bytes_total *= 2;
        disassembler->real_stream = apr_palloc(disassembler->user_pool,
            disassembler->num_bytes_total);
        memcpy(disassembler->real_stream, buf, disassembler->num_bytes_used);
        free(buf);
    }
    apr_vsnprintf(disassembler->real_stream + disassembler->num_bytes_used,
        required_length + 1, fmt, args);
    disassembler->num_bytes_used += required_length;
    return required_length;
}

static int disasm_fprint_default(port_disassembler_t * disassembler, const char * fmt, ...) {
    int required_length;
    va_list args;
    va_start(args, fmt);
    required_length = apr_vsnprintf(NULL, 0, fmt, args);
    assert(required_length >= 0);
    // insure space
    if ((unsigned int)required_length >= disassembler->num_bytes_total -
            disassembler->num_bytes_used) {
        apr_file_write(disassembler->user_file, disassembler->real_stream,
            &disassembler->num_bytes_used);
        disassembler->num_bytes_used = 0;
    }
    while ((unsigned int)required_length >= disassembler->num_bytes_total -
            disassembler->num_bytes_used) {
        disassembler->num_bytes_total *= 2;
    }
    apr_vsnprintf(disassembler->real_stream + disassembler->num_bytes_used,
        required_length + 1, fmt, args);
    disassembler->num_bytes_used += required_length;
    return 0;
}

static void disasm_print(port_disassembler_t * disassembler,
                         const char * code, 
                         apr_int64_t len) {
    // iterate over the code buffer
    while (len > 0) {
        int bytes_read = 1;
        
        // print instruction address
        if (disassembler->port_info.print_addr) {
            disassembler->fprintf_func(disassembler,
                "0x%08X\t", (apr_uint32_t)code);
        }
        
        // print mnemonic
        // FIXME: no support
        assert(disassembler->port_info.print_mnemonic == 0);

        // print native bytes
        if (disassembler->port_info.print_bytes) {
            int i;
            disassembler->fprintf_func(disassembler, "\t");
            for (i = 0; i < bytes_read; i++) {
                disassembler->fprintf_func(disassembler,
                    "%02X ", ((int)*(code + i)) & 0xff);
            }
        }
        disassembler->fprintf_func(disassembler, "\n");
        code += bytes_read;
        len -= bytes_read;
#ifndef NDEBUG
        if (len < 0) {
            fprintf(stderr, "WARNING: Disassembler read %i byte(s) more "
                "than specified buffer length\n", (apr_int32_t)-len);
        }
#endif
    }
}
#endif // defined(_IA32_)

/*    Public Interface    */

APR_DECLARE(apr_status_t) port_disasm_initialize() {
#if defined(_IA32_)
    return APR_SUCCESS;
#else
    return APR_ENOTIMPL;
#endif
}

APR_DECLARE(apr_status_t) port_disassembler_create(port_disassembler_t ** disassembler,
                                                   apr_pool_t * pool) {
#if defined(_IA32_)
    apr_status_t status;
    port_disasm_info_t info = {1, 0, 1};
    
    if ((status = port_disasm_initialize()) != APR_SUCCESS) {
        return status;
    }

    *disassembler = (port_disassembler_t *)
        apr_palloc(pool, sizeof(port_disassembler_t));
    
    // initialize port_info
    port_disasm_set_info(*disassembler, info, NULL);


    // initialize the rest fields
    (*disassembler)->real_stream = NULL;
    (*disassembler)->user_pool = pool;
    (*disassembler)->user_file = NULL;
    (*disassembler)->num_bytes_total = 0;
    (*disassembler)->num_bytes_used = 0;

    return APR_SUCCESS;
#else
    return APR_ENOTIMPL;
#endif
}

APR_DECLARE(apr_status_t) port_disasm_set_info(port_disassembler_t * disassembler,
                                               const port_disasm_info_t new_info,
                                               port_disasm_info_t * old_info) {
#if defined(_IA32_)
    if (old_info != NULL) {
        *old_info = disassembler->port_info;
    }
    disassembler->port_info = new_info;
    disassembler->line_size = 0;
    if (disassembler->port_info.print_addr) {
        disassembler->line_size += ADDR_SIZE;
    }
    // FIXME: no support for mnemonic
    disassembler->port_info.print_mnemonic = 0;

    if (disassembler->port_info.print_bytes) {
        disassembler->line_size += BYTES_SIZE;
    }
    return APR_SUCCESS;
#else
    return APR_ENOTIMPL;
#endif
}

APR_DECLARE(apr_status_t) port_disasm(port_disassembler_t * disassembler,
                                      const char * code, 
                                      unsigned int len,
                                      char ** disasm_code) {
#if defined(_IA32_)    
    // check if nothing should be printed
    if (disassembler->line_size == 0) {
        *disasm_code = NULL;
        return APR_SUCCESS;
    } 

    if (disassembler->num_bytes_total == 0) {
        // Calculate required number of bytes
        disassembler->num_bytes_total = disassembler->line_size * len / BYTES_PER_LINE;
        // initialize stream
        disassembler->real_stream = apr_palloc(disassembler->user_pool,
            disassembler->num_bytes_total);
    }

    strcpy(disassembler->real_stream, "");
    
    disassembler->fprintf_func = disasm_sprint_default;

    disasm_print(disassembler, code, len);

    *disasm_code = disassembler->real_stream;

    disassembler->real_stream = NULL;
    disassembler->num_bytes_total = 0;
    disassembler->num_bytes_used = 0;

	return APR_SUCCESS;
#else
    return APR_ENOTIMPL;
#endif
}

APR_DECLARE(apr_status_t) port_disasm_to_file(port_disassembler_t * disassembler,
                                              const char * code,
                                              unsigned int len,
                                              apr_file_t * thefile) {
#if defined(_IA32_)
    // check if nothing should be printed
    if (disassembler->line_size == 0) {
        return APR_SUCCESS;
    } 
    
    if (disassembler->num_bytes_total == 0) {
        // Calculate required number of bytes
        disassembler->num_bytes_total = disassembler->line_size * BYTES_TOTAL / BYTES_PER_LINE;
        // initialize stream
        disassembler->real_stream = apr_palloc(disassembler->user_pool,
            disassembler->num_bytes_total);
    }
    
    // initialize file
    disassembler->user_file = thefile;

    strcpy(disassembler->real_stream, "");

    disassembler->fprintf_func = disasm_fprint_default;

    disasm_print(disassembler, code, len);

    // flush
    apr_file_write(disassembler->user_file, disassembler->real_stream,
        &disassembler->num_bytes_used);
    apr_file_flush(disassembler->user_file);

    disassembler->user_file = NULL;
    disassembler->num_bytes_used = 0;

	return APR_SUCCESS;
#else
    return APR_ENOTIMPL;
#endif
}
