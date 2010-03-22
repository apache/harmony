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
 * @author Alexey V. Varlamov
 */  

#ifndef _PORT_VMEM_H_
#define _PORT_VMEM_H_
/**
 * @file
 * Virtual memory support
 *
 * Functions to manipulate memory pages in virtual address space: 
 * reserve, de/commit, free, control size and access protection of pages.
 */

#include "port_general.h"
#include <apr_pools.h>

#ifdef __cplusplus
extern "C" {
#endif
/** @defgroup port_apr Portability layer */

/**
 * @defgroup port_vmem Virtual memory support
 * @ingroup port_apr
 * @{
 */

/**
 * @defgroup vmem_protection Memory protection flags
 * @{
 */
/** Enabling read access to the committed memory region */
#define PORT_VMEM_MODE_READ 0x1
/** Enabling write access to the committed memory region */
#define PORT_VMEM_MODE_WRITE 0x2
/** Enabling read access to the committed memory region */
#define PORT_VMEM_MODE_EXECUTE 0x4
/** @} */

/**
 * @defgroup vmem_size Memory page size directives
 * These defines can be used instead of explicit calls to <code>port_vmem_page_sizes()</code>.
 * @{
 */
/** System default page size*/
#define PORT_VMEM_PAGESIZE_DEFAULT 0
/** Large page (if supported by system) */
#define PORT_VMEM_PAGESIZE_LARGE 1
/** @} */

/**
 * Virtual memory block descriptor. This is an <i>incomplete type</i>, 
 * the run-time instance should be obtained via port_vmem_reserve() call
 */
typedef struct port_vmem_t port_vmem_t;

/**
 * Reserves a continuous memory region in the virtual address space 
 * of the calling process. 
 * @param[out] block      - descriptor for the reserved memory, required for 
 *                          further operations with the memory
 * @param[in,out] address - desired starting address of the region to allocate. If
 *                          <code>NULL</code>, the system determines the
 *                          appropriate location.On success, the actual
 *                          allocated address is returned
 * @param amount          - the size of the region in bytes. For large pages,
                            the size must be multiply of page size
 * @param protectionMode  - the bit mask of <code>PORT_VMEM_MODE_*</code> flags
 * @param pageSize        - the desired size of the memory page; should contain 
 *                          <code>PORT_VMEM_PAGESIZE_DEFAULT</code>,
 *                          <code>PORT_VMEM_PAGESIZE_LARGE</code> or the actual size in bytes
 * @param pool            - the auxiliary pool to allocate the descriptor data, etc
 * @return <code>APR_SUCCESS</code> if OK; otherwise, an error code.
 * @see port_vmem_page_sizes()
 */
APR_DECLARE(apr_status_t) port_vmem_reserve(port_vmem_t **block, void **address, 
                                           size_t amount, 
                                           unsigned int protectionMode, 
                                           size_t pageSize, apr_pool_t *pool);

/**
* Commits (a part of) the previously reserved memory region. The allocated memory 
* is initialized to zero.
* @param[in,out] address - the starting address of the region to commit; the returned value 
*                          may differ due to page alignment
* @param amount          - the size of the region in bytes
* @param block           - the descriptor to the reserved virtual memory
* @return <code>APR_SUCCESS</code> if OK; otherwise, an error code.
*/
APR_DECLARE(apr_status_t) port_vmem_commit(void **address, size_t amount, 
                                          port_vmem_t *block);

/**
* Decommits the specified region of committed memory. It is safe to 
* decommit a reserved (but not committed) region.
* @param address - the starting address of the region to decommit
* @param amount  - the size of the region in bytes
* @param block   - the memory region descriptor
* @return <code>APR_SUCCESS</code> if OK; otherwise, an error code.
*/
APR_DECLARE(apr_status_t) port_vmem_decommit(void *address, size_t amount, 
                                            port_vmem_t *block);

/**
* Releases previously reserved virtual memory region as a whole.
* If the region was committed, the function first decommits it.
* @param block - the memory region descriptor
* @return <code>APR_SUCCESS</code> if OK; otherwise, an error code.
*/
APR_DECLARE(apr_status_t) port_vmem_release(port_vmem_t *block);

/**
* Returns a zero-terminated array of supported memory page sizes.
* The first element refers to the system default size and is guaranteed
* to be non-zero. Subsequent elements (if any) provide large page
* sizes.
*/
APR_DECLARE(size_t *) port_vmem_page_sizes();

/**
 * Returns the amount of currently used memory in bytes
 * or 0 if this value could not be calculated.
 */
APR_DECLARE(size_t) port_vmem_used_size();

/**
 * Returns the amount of committed memory in bytes
 * or 0 if this value could not be calculated.
 */
APR_DECLARE(size_t) port_vmem_committed_size();

/**
 * Returns the amount of reserved memory in bytes
 * or 0 if this value could not be calculated.
 */
APR_DECLARE(size_t) port_vmem_reserved_size();

/**
 * Returns the maximum amount of memory which could be reserved in bytes
 * or 0 if this value could not be calculated
 */
APR_DECLARE(size_t) port_vmem_max_size();

/**
 * Allocate memory with default page size.
 * @param[in,out] addr - desired starting address of the region to allocate. If
 *                       <code>NULL</code>, the system determines the
 *                       appropriate location.On success, the actual
 *                       allocated address is returned
 * @param size         - the size of the region in bytes. For large pages,
                         the size must be multiply of page size
 * @param mode  - the bit mask of <code>PORT_VMEM_MODE_*</code> flags
 */
APR_DECLARE(apr_status_t) port_vmem_allocate(void **addr, size_t size, unsigned int mode);

/**
 * Releases previously reserved virtual memory region as a whole.
 * If the region was committed, the function first decommits it.
 * @param addr - the memory region address
 * @param size - size of the allocated memory
 * @return <code>APR_SUCCESS</code> if OK; otherwise, an error code.
 */
APR_DECLARE(apr_status_t) port_vmem_free(void *addr, size_t size);

/** @} */

#ifdef __cplusplus
}
#endif

#endif /* _PORT_VMEM_H_ */
