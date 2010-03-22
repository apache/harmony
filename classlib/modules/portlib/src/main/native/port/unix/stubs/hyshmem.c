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
 * @brief Shared Memory Semaphores
 */

#undef CDEV_CURRENT_FUNCTION

#include "hyport.h"
#include "ut_hyprt.h"

#include "portnls.h"
#include "portpriv.h"
#include "hysharedhelper.h"
#define CDEV_CURRENT_FUNCTION include_header
#include "hyshmem.h"
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_open
/**
 * Creates/open a shared memory region
 * 
 * The rootname will uniquely identify the shared memory region, 
 * and is valid across different JVM instance. 
 * 
 * The shared memory region should persist across process, until OS reboots 
 * or destroy call is being made.
 * 
 * @param[in] portLibrary The port Library
 * @param[out] handle This handle is required for further
 * attach/destroy of the memory region
 * @param[in] rootname Shared name for the region, which used to
 * identify the region.
 * @param[in] size Size of the region in bytes
 * @param[in] perm permission for the region.
 * 
 * @return
 * \arg HYPORT_ERROR_SHMEM_OPFAILED Failure - Cannot open the shared
 * memory region
 * \arg HYPORT_INFO_SHMEM_OPENED Success - Existing memory region has
 * been opened
 * \arg HYPORT_INFO_SHMEM_CREATED Success - A new shared memory region
 * has been created
 */
IDATA VMCALL
hyshmem_open (HyPortLibrary * portLibrary, struct hyshmem_handle **handle,
              const char *rootname, I_32 size, I_32 perm)
{
  return HYPORT_ERROR_SHMEM_OPFAILED;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_close
/**
 * Detach, Close and remove the shared memory handle.
 * 
 * @note This method does not remove the shared memory region from the
 *	 OS use @ref hyshmem_destroy instead. However this will free
 *	 all the memory resources used by the handle, and detach the
 *	 region specified by the handle
 *
 * @param[in] portLibrary The port Library
 * @param[in] handle Pointer to a valid shared memory handle
 * 
 */
void VMCALL
hyshmem_close (struct HyPortLibrary *portLibrary,
               struct hyshmem_handle **handle)
{
}
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_attach
/**
 * Attaches the shared memory represented by the handle
 * 
 * @param[in] portLibrary The port Library
 * @param[in] handle A valid shared memory handle
 * 
 * @return: A pointer to the shared memory region, NULL on failure
 */
void *VMCALL
hyshmem_attach (struct HyPortLibrary *portLibrary,
                struct hyshmem_handle *handle)
{
  return NULL;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_destroy
/**
 * Destroy and removes the shared memory region from OS.
 * 
 * The timing of which OS removes the memory is OS dependent. However
 * when you make a call you can considered that you can no longer
 * access the region through the handle. Memory allocated for handle
 * structure is freed as well.
 * 
 * @param[in] portLibrary The port Library
 * @param[in] handle Pointer to a valid shared memory handle
 * 
 * @return 0 on success, -1 on failure.
 */
IDATA VMCALL
hyshmem_destroy (struct HyPortLibrary * portLibrary,
                 struct hyshmem_handle ** handle)
{
  return -1;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_detach
/**
 * Detaches the shared memory region from the caller's process address space
 * Use @ref hyshmem_destroy to actually remove the memory region from
 * the Operating system
 *
 * @param[in] portLibrary the Port Library.
 * @param[in] handle Pointer to the shared memory region.
 * 
 * @return 0 on success, -1 on failure.
 */
IDATA VMCALL
hyshmem_detach (struct HyPortLibrary * portLibrary,
                struct hyshmem_handle ** handle)
{
  return -1;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_findclose
/**
 * Close the handle returned from @ref hyshmem_findfirst.
 *
 * @param[in] portLibrary The port library
 * @param[in] findhandle  Handle returned from @ref hyshmem_findfirst.
 */
void VMCALL
hyshmem_findclose (struct HyPortLibrary *portLibrary, UDATA findhandle)
{
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_findfirst
/**
 * Find the name of a shared memory region on the system. Answers a
 * handle to be used in subsequent calls to @ref hyshmem_findnext and
 * @ref hyshmem_findclose.
 *
 * @param[in] portLibrary The port library
 * @param[out] resultbuf filename and path matching path.
 *
 * @return valid handle on success, -1 on failure.
 */
UDATA VMCALL
hyshmem_findfirst (struct HyPortLibrary *portLibrary, char *resultbuf)
{
  return -1;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_findnext
/**
 * Find the name of the next shared memory region.
 *
 * @param[in] portLibrary The port library
 * @param[in] findhandle handle returned from @ref hyshmem_findfirst.
 * @param[out] resultbuf next filename and path matching findhandle.
 *
 * @return 0 on success, -1 on failure or if no matching entries.
 */
I_32 VMCALL
hyshmem_findnext (struct HyPortLibrary * portLibrary, UDATA findHandle,
                  char *resultbuf)
{
  return -1;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_stat
/**
 * Return the statistic for a shared memory region
 *
 * @note notice that the implementation can decided to put -1 in the
 * fields of @ref statbuf if it does not make sense on this platform,
 * or it is impossible to obtain.
 * 
 * @param[in] portLibrary The port library
 * @param[in] name The name of the shared memory area.
 * @param[out] statbuf the statistics returns by the operating system
 *
 * @return 0 on success, -1 on failure or if there is no matching entries.
 */
UDATA VMCALL
hyshmem_stat (struct HyPortLibrary * portLibrary, const char *name,
              struct HyPortShmemStatistic * statbuf)
{
  return -1;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_shutdown
/**
 * PortLibrary shutdown.
 *
 * This function is called during shutdown of the portLibrary.  Any
 * resources that were created by @ref hyshsem_startup should be
 * destroyed here.
 *
 * @param[in] portLibrary The port library.
 *
 * @note Most implementations will be empty.
 */
void VMCALL
hyshmem_shutdown (struct HyPortLibrary *portLibrary)
{
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_startup
/**
 * PortLibrary startup.
 *
 * This function is called during startup of the portLibrary.  Any
 * resources that are required for the file operations may be created
 * here.  All resources created here should be destroyed in @ref
 * hyshsem_shutdown.
 *
 * @param[in] portLibrary The port library.
 *
 * @return 0 on success, negative error code on failure.  Error code
 * values returned are
 * \arg HYPORT_ERROR_STARTUP_SHMEM
 *
 * @note Most implementations will simply return success.
 */
I_32 VMCALL
hyshmem_startup (struct HyPortLibrary *portLibrary)
{
  return 0;
}

#undef CDEV_CURRENT_FUNCTION
