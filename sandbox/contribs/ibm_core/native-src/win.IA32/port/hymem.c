/* Copyright 1991, 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define CDEV_CURRENT_FUNCTION _comment_
/**
 * @file
 * @ingroup Port
 * @brief Memory Utilities
 */
#undef CDEV_CURRENT_FUNCTION

/* 
 * This file contains code for the portability library memory management.
 */

#include <windows.h>
#include "hyport.h"
#include "portpriv.h"
#include "hyportpg.h"
#include "ut_hyprt.h"

#define ROUND_TO(granularity, number) ((number) + \
                                       (((number) % (granularity)) ? ((granularity) - ((number) % (granularity))) : 0))

#define CDEV_CURRENT_FUNCTION hymem_allocate_memory
/**
 * Allocate memory.
 *
 * @param[in] portLibrary The port library
 * @param[in] byteAmount Number of bytes to allocate.
 *
 * @return pointer to memory on success, NULL on error.
 * @return Memory is not guaranteed to be zeroed as part of this call
 *
 * @internal @warning Do not call error handling code @ref hyerror upon error as 
 * the error handling code uses per thread buffers to store the last error.  If memory
 * can not be allocated the result would be an infinite loop.
 */
void *VMCALL
hymem_allocate_memory (struct HyPortLibrary *portLibrary, UDATA byteAmount)
{
  void *pointer = NULL;

  Trc_PRT_mem_hymem_allocate_memory_Entry (byteAmount);
  if (byteAmount == 0)
    {				/* prevent GlobalLock from failing causing allocate to return null */
      byteAmount = 1;
    }
  pointer = HeapAlloc (PPG_mem_heap, 0, byteAmount);
  Trc_PRT_mem_hymem_allocate_memory_Exit (pointer);
  return pointer;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hymem_free_memory
/**
 * Deallocate memory.
 *
 * @param[in] portLibrary The port library
 * @param[in] memoryPointer Base address of memory to be deallocated.
 */
void VMCALL
hymem_free_memory (struct HyPortLibrary *portLibrary, void *memoryPointer)
{
  Trc_PRT_mem_hymem_free_memory_Entry (memoryPointer);
  HeapFree (PPG_mem_heap, 0, memoryPointer);
  Trc_PRT_mem_hymem_free_memory_Exit ();
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hymem_reallocate_memory
/**
 * Re-allocate memory.
 *
 * @param[in] portLibrary The port library
 * @param[in] memoryPointer Base address of memory to be re-allocated.
 * @param[in] byteAmount Number of bytes to re-allocated.
 *
 * @return pointer to memory on success, NULL on error.
 *
 * @internal @warning Do not call error handling code @ref hyerror upon error as 
 * the error handling code uses per thread buffers to store the last error.  If memory
 * can not be allocated the result would be an infinite loop.
 */
void *VMCALL
hymem_reallocate_memory (struct HyPortLibrary *portLibrary,
			 void *memoryPointer, UDATA byteAmount)
{
  void *ptr = NULL;

  Trc_PRT_mem_hymem_reallocate_memory_Entry (memoryPointer, byteAmount);
  ptr = HeapReAlloc (PPG_mem_heap, 0, memoryPointer, byteAmount);
  Trc_PRT_mem_hymem_reallocate_memory_Exit (ptr);
  return ptr;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hymem_shutdown
/**
 * PortLibrary shutdown.
 *
 * This function is called during shutdown of the portLibrary.  Any resources that
 * were created by @ref hymem_startup should be destroyed here.
 *
 * @param[in] portLibrary The port library
 *
 * @note Must deallocate portGlobals.
 * @note Most implementations will just deallocate portGlobals.
 */
void VMCALL
hymem_shutdown (struct HyPortLibrary *portLibrary)
{
  if (NULL != portLibrary->portGlobals)
    {
      HANDLE memHeap = GetProcessHeap ();

      /* has to be cleaned up with HeapFree since hymem_startup allocated it with HeapAlloc */
      HeapFree (memHeap, 0, portLibrary->portGlobals);
      portLibrary->portGlobals = NULL;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hymem_startup
/**
 * PortLibrary startup.
 *
 * This function is called during startup of the portLibrary.  Any resources that are required for
 * the memory operations may be created here.  All resources created here should be destroyed
 * in @ref hymem_shutdown.
 *
 * @param[in] portLibrary The port library
 * @param[in] portGlobalSize Size of the global data structure to allocate
 *
 * @return 0 on success, negative error code on failure.  Error code values returned are \arg HYPORT_ERROR_STARTUP_MEM
 *
 * @note Must allocate portGlobals.
 * @note Most implementations will just allocate portGlobals.
 *
 * @internal @note portLibrary->portGlobals must point to an aligned structure
 */
I_32 VMCALL
hymem_startup (struct HyPortLibrary *portLibrary, UDATA portGlobalSize)
{
  HANDLE memHeap = GetProcessHeap ();
  /* done as a HeapAlloc because hymem_allocate_memory requires portGlobals to be initialized */
  portLibrary->portGlobals = HeapAlloc (memHeap, 0, portGlobalSize);
  if (!portLibrary->portGlobals)
    {
      return HYPORT_ERROR_STARTUP_MEM;
    }
  memset (portLibrary->portGlobals, 0, portGlobalSize);

  PPG_mem_heap = memHeap;
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hymem_allocate_memory_callSite
/**
 * Allocate memory.
 *
 * @param[in] portLibrary The port library
 * @param[in] byteAmount Number of bytes to allocate.
 * @param[in] callSite String describing callsite, usually file and line number.
 *
 * @return pointer to memory on success, NULL on error.
 *
 * @internal @warning Do not call error handling code @ref hyerror upon error as 
 * the error handling code uses per thread buffers to store the last error.  If memory
 * can not be allocated the result would be an infinite loop.
 */
void *VMCALL
hymem_allocate_memory_callSite (struct HyPortLibrary *portLibrary,
				UDATA byteAmount, char *callSite)
{
  void *ptr = NULL;

  Trc_PRT_mem_hymem_allocate_memory_callSite_Entry (byteAmount, callSite);
  ptr = portLibrary->mem_allocate_memory (portLibrary, byteAmount);
  Trc_PRT_mem_hymem_allocate_memory_callSite_Exit (ptr);
  return ptr;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hymem_allocate_portLibrary
/**
 * @internal Allocate memory for a portLibrary.
 *
 * @param[in] byteAmount Number of bytes to allocate.
 *
 * @return pointer to memory on success, NULL on error.
 * @note This function is called prior to the portLibrary being initialized
 * @note Must be implemented for all platforms.
 */
void *VMCALL
hymem_allocate_portLibrary (UDATA byteAmount)
{
  HANDLE memHeap = GetProcessHeap ();
  return HeapAlloc (memHeap, 0, byteAmount);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hymem_deallocate_portLibrary
/**
 * @internal Free memory for a portLibrary.
 *
 * @param[in] memoryPointer Base address to be deallocated.
 *
 * @note Must be implemented for all platforms.
 */
void VMCALL
hymem_deallocate_portLibrary (void *memoryPointer)
{
  HANDLE memHeap = GetProcessHeap ();
  HeapFree (memHeap, 0, memoryPointer);
}

#undef CDEV_CURRENT_FUNCTION
