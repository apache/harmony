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

#ifndef _DISASM_H_
#define _DISASM_H_

#include <apr_general.h>
#include <apr_pools.h>
#include <apr_file_io.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
* @file
* Provides information on dynamically-generated code
*/

/**
 * @defgroup port_disasm Disassembler of machine codes
 * @ingroup port_apr
 * @{
 */


typedef struct port_disassembler_t port_disassembler_t;

typedef struct port_disasm_info_t {
    int print_addr;
    int print_mnemonic;
    int print_bytes;
} port_disasm_info_t;


/**
 * Initializes the <code>disasm</code> module.
 *
 * @remark It's safe to call the function several times.
 */
APR_DECLARE(apr_status_t) port_disasm_initialize();

/**
 * Creates the disassembler.
 *
 * @param disassembler - the created disassembler
 * @param pool         - the pool to use
 * @remark It's safe to call this function by multiple threads with different pools.
 */
APR_DECLARE(apr_status_t) port_disassembler_create(port_disassembler_t ** disassembler,
                                                   apr_pool_t * pool);
/**
 * @param new_info - the info to set up
 * @param old_info - the pointer to the memory to store old info in; <code>NULL</code> 
 *                   should be specified if no interest for the old info
 * @warning Not thread safe. Only one thread can change the info for the specified
           disassembler.
 */
APR_DECLARE(apr_status_t) port_disasm_set_info(port_disassembler_t * disassembler,
                                               const port_disasm_info_t new_info,
                                               port_disasm_info_t * old_info);


/**
 * Translates from machine native to human readable code.
 *
 * @param disassembler   - the disassembler
 * @param code           - the assembler code to translate
 * @param len            - the number of bytes to be translated
 * @param disasm_code    - the pointer to the pointer to the disassembled code on exit;
 *                         is valid when the disassembler is valid
 * @warning Not thread safe. Only one thread can use the specified disassembler.
 */
APR_DECLARE(apr_status_t) port_disasm(port_disassembler_t * disassembler,
                                      const char * code, 
                                      unsigned int len,
                                      char ** disasm_code);

/**
 * Translates from machine native to human readable code.
 *
 * @param code    - the assembler code to translate
 * @param len     - the number of bytes to be translated
 * @param thefile - the destination file
 * @warning Not thread safe. Only one thread can use the specified disassembler
 */
APR_DECLARE(apr_status_t) port_disasm_to_file(port_disassembler_t * disassembler,
                                              const char * code,
                                              unsigned int len,
                                              apr_file_t * thefile);

/** @} */


#ifdef __cplusplus
}
#endif

#endif // _DISASM_H_




