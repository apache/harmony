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

#define CDEV_CURRENT_FUNCTION _comment_
/**
 * @file
 * @ingroup Port
 * @brief Virtual memory
 */

#undef CDEV_CURRENT_FUNCTION

#include "hyport.h"
#include "portpriv.h"
#include "hyportpg.h"
#include "ut_hyprt.h"

#define CDEV_CURRENT_FUNCTION hyvmem_shutdown
/**
 * PortLibrary shutdown.
 *
 * This function is called during shutdown of the portLibrary.  Any resources that were created by @ref hyvmem_startup
 * should be destroyed here.
 *
 * @param[in] portLibrary The port library.
 *
 * @note Most implementations will be empty.
 */
void VMCALL
hyvmem_shutdown (struct HyPortLibrary *portLibrary)
{
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyvmem_startup
/**
 * PortLibrary startup.
 *
 * This function is called during startup of the portLibrary.  Any
 * resources that are required for the virtual memory operations may
 * be created here.  All resources created here should be destroyed in
 * @ref hyvmem_shutdown.
 *
 * @param[in] portLibrary The port library.
 *
 * @return 0 on success, negative error code on failure.  Error code
 * values returned are \arg HYPORT_ERROR_STARTUP_VMEM
 *
 * @note Most implementations will simply return success.
 */
I_32 VMCALL
hyvmem_startup (struct HyPortLibrary *portLibrary)
{
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyvmem_commit_memory
/**
 * Commit memory in virtual address space.
 *
 * @param[in] portLibrary The port library.
 * @param[in] address The page aligned starting address of the memory to commit.
 * @param[in] byteAmount The number of bytes to commit.
 * @param[in] identifier Descriptor for virtual memory block.
 *
 * @return pointer to the allocated memory on success, NULL on failure.
 */
void *VMCALL
hyvmem_commit_memory (struct HyPortLibrary *portLibrary, void *address,
                      UDATA byteAmount,
                      struct HyPortVmemIdentifier *identifier)
{
  return NULL;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyvmem_decommit_memory
/**
 * Decommit memory in virtual address space.
 *
 * Decommits physical storage of the size specified starting at the
 * address specified.
 *
 * @param[in] portLibrary The port library.
 * @param[in] address The starting address of the memory to be decommitted.
 * @param[in] byteAmount The number of bytes to be decommitted.
 * @param[in] identifier Descriptor for virtual memory block.
 *
 * @return 0 on success, non zero on failure.
 */
IDATA VMCALL
hyvmem_decommit_memory (struct HyPortLibrary * portLibrary, void *address,
                        UDATA byteAmount,
                        struct HyPortVmemIdentifier * identifier)
{
  return (IDATA) 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyvmem_free_memory
/**
 * Free memory in virtual address space.
 *
 * Frees physical storage of the size specified starting at the
 * address specified.
 *
 * @param[in] portLibrary The port library.
 * @param[in] address The starting address of the memory to be de-allocated.
 * @param[in] byteAmount The number of bytes to be allocated.
 * @param[in] identifier Descriptor for virtual memory block.
 *
 * @return 0 on success, non zero on failure.
 */
I_32 VMCALL
hyvmem_free_memory (struct HyPortLibrary * portLibrary, void *address,
                    UDATA byteAmount,
                    struct HyPortVmemIdentifier * identifier)
{
  return -1;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyvmem_reserve_memory
/**
 * Reserve memory in virtual address space.
 *
 * Reserves a range of virtual address space without allocating any
 * actual physical storage.
 * The memory is not available for use until committed @ref
 * hyvmem_commit_memory.
 * The memory may not be used by other memory allocation routines
 * until it is explicitly released.
 *
 * @param[in] portLibrary The port library.
 * @param[in] address The starting address of the memory to be reserved.
 * @param[in] byteAmount The number of bytes to be reserved.
 * @param[in] identifier Descriptor for virtual memory block.
 * @param[in] mode Bitmap indicating how memory is to be reserved.
 * Expected values combination of:
 * \arg HYPORT_VMEM_MEMORY_MODE_READ memory is readable
 * \arg HYPORT_VMEM_MEMORY_MODE_WRITE memory is writable
 * \arg HYPORT_VMEM_MEMORY_MODE_EXECUTE memory is executable
 * \arg HYPORT_VMEM_MEMORY_MODE_COMMIT commits memory as part of the reserve
 * @param[in] pageSize Size of the page requested, a value returned by
 * @ref hyvmem_supported_page_sizes, or the constant
 * HYPORT_VMEM_PAGE_SIZE_DEFAULT for the system default page size.
 *
 * @return pointer to the reserved memory on success, NULL on failure.
 *
 * @internal @warning Do not call error handling code @ref hyerror
 * upon error as the error handling code uses per thread buffers to
 * store the last error.  If memory can not be allocated the result
 * would be an infinite loop.
 */
void *VMCALL
hyvmem_reserve_memory (struct HyPortLibrary *portLibrary, void *address,
                       UDATA byteAmount,
                       struct HyPortVmemIdentifier *identifier, UDATA mode,
                       UDATA pageSize)
{
  return NULL;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyvmem_supported_page_sizes
/**
 * Determine the page sizes supported.
 *
 * @param[in] portLibrary The port library.
 *
 * @return A 0 terminated array of supported page sizes.  The first entry is the default page size, other entries
 * are the large page sizes supported.
 */
UDATA *VMCALL
hyvmem_supported_page_sizes (struct HyPortLibrary * portLibrary)
{
  return PPG_vmem_pageSize;
}
#undef CDEV_CURRENT_FUNCTION
