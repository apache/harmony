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

#include <windows.h>

#include "hyport.h"
#include "portpriv.h"
#include "hyportpg.h"
#include "ut_hyprt.h"

#define CDEV_CURRENT_FUNCTION _prototypes_private

BOOL SetLockPagesPrivilege (HANDLE hProcess, BOOL bEnable);

DWORD getProtectionBits (UDATA mode);
void VMCALL update_vmemIdentifier (HyPortVmemIdentifier * identifier,
				   void *address, void *handle,
				   UDATA byteAmount, UDATA mode,
				   UDATA pageSize);

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
  void *ptr = NULL;

  Trc_PRT_vmem_hyvmem_commit_memory_Entry (address, byteAmount);
  if (PPG_vmem_pageSize[0] == identifier->pageSize)
    {
      ptr =
	(void *) VirtualAlloc ((LPVOID) address, (size_t) byteAmount,
			       MEM_COMMIT,
			       getProtectionBits (identifier->mode));
    }
  else
    {
      ptr = address;
    }
  Trc_PRT_vmem_hyvmem_commit_memory_Exit (ptr);
  return ptr;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyvmem_decommit_memory
/**
 * Decommit memory in virtual address space.
 *
 * Decommits physical storage of the size specified starting at the address specified.
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
  IDATA ret = 0;

  Trc_PRT_vmem_hyvmem_decommit_memory_Entry (address, byteAmount);
  if (PPG_vmem_pageSize[0] == identifier->pageSize)
    {
      ret =
	(IDATA) VirtualFree ((LPVOID) address, (size_t) byteAmount,
			     MEM_DECOMMIT);
    }
  else
    {
      ret = 0;
    }
  Trc_PRT_vmem_hyvmem_decommit_memory_Exit (ret);
  return ret;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyvmem_free_memory
/**
 * Free memory in virtual address space.
 *
 * Frees physical storage of the size specified starting at the address specified.
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
  I_32 ret = 0;

  Trc_PRT_vmem_hyvmem_free_memory_Entry (address, byteAmount);
  update_vmemIdentifier (identifier, NULL, NULL, 0, 0, 0);
  ret = (I_32) VirtualFree ((LPVOID) address, (size_t) 0, MEM_RELEASE);
  Trc_PRT_vmem_hyvmem_free_memory_Exit (ret);
  return ret;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyvmem_reserve_memory
/**
 * Reserve memory in virtual address space.
 *
 * Reserves a range of  virtual address space without allocating any actual physical storage.
 * The memory is not available for use until committed @ref hyvmem_commit_memory.
 * The memory may not be used by other memory allocation routines until it is explicitly released.
 *
 * @param[in] portLibrary The port library.
 * @param[in] address The starting address of the memory to be reserved.
 * @param[in] byteAmount The number of bytes to be reserved.
 * @param[in] identifier Descriptor for virtual memory block.
 * @param[in] mode Bitmap indicating how memory is to be reserved.  Expected values combination of:
 * \arg HYPORT_VMEM_MEMORY_MODE_READ memory is readable
 * \arg HYPORT_VMEM_MEMORY_MODE_WRITE memory is writable
 * \arg HYPORT_VMEM_MEMORY_MODE_EXECUTE memory is executable
 * \arg HYPORT_VMEM_MEMORY_MODE_COMMIT commits memory as part of the reserve
 * @param[in] pageSize Size of the page requested, a value returned by @ref hyvmem_supported_page_sizes,
 * or the constant HYPORT_VMEM_PAGE_SIZE_DEFAULT for the system default page size.
 *
 * @return pointer to the reserved memory on success, NULL on failure.
 *
 * @internal @warning Do not call error handling code @ref hyerror upon error as 
 * the error handling code uses per thread buffers to store the last error.  If memory
 * can not be allocated the result would be an infinite loop.
 */
void *VMCALL
hyvmem_reserve_memory (struct HyPortLibrary *portLibrary, void *address,
		       UDATA byteAmount,
		       struct HyPortVmemIdentifier *identifier, UDATA mode,
		       UDATA pageSize)
{
  LPVOID baseAddress = NULL;
  DWORD protection = getProtectionBits (mode);

  Trc_PRT_vmem_hyvmem_reserve_memory_Entry (address, byteAmount);
  /* Invalid input */
  if (0 == pageSize)
    {
      update_vmemIdentifier (identifier, NULL, NULL, 0, 0, 0);
      Trc_PRT_vmem_hyvmem_reserve_memory_Exit1 ();
      return NULL;
    }

  /* Handle default page size */
  if ((HYPORT_VMEM_PAGE_SIZE_DEFAULT == pageSize)
      || (PPG_vmem_pageSize[0] == pageSize))
    {
      DWORD allocationType;

      /* Determine if a commit is required */
      if (0 != (HYPORT_VMEM_MEMORY_MODE_COMMIT & mode))
	{
	  allocationType = MEM_COMMIT;
	}
      else
	{
	  /* 
	   * If we don't reserve with PAGE_NOACCESS, CE won't give us large blocks. 
	   * On Win32 the protection bits appear to be ignored for uncommitted memory. 
	   */
	  allocationType = MEM_RESERVE;
	  protection = PAGE_NOACCESS;
	}

      baseAddress =
	VirtualAlloc ((LPVOID) address, (size_t) byteAmount, allocationType,
		      protection);
      update_vmemIdentifier (identifier, (void *) baseAddress,
			     (void *) baseAddress, byteAmount, mode,
			     PPG_vmem_pageSize[0]);
      Trc_PRT_vmem_hyvmem_reserve_memory_Exit2 (baseAddress);
      return (void *) baseAddress;
    }

  /* Must be large pages, make sure size is correct, else return an error */
  if (PPG_vmem_pageSize[1] == pageSize)
    {
      UDATA largePageSize = PPG_vmem_pageSize[1];
      UDATA numOfPages = byteAmount / largePageSize;
      UDATA leftOver = byteAmount - numOfPages * largePageSize;
      UDATA totalAllocateSize = 0;

      if (leftOver != 0)
	{
	  numOfPages++;
	}
      totalAllocateSize = numOfPages * largePageSize;

      /* Allocate large pages in the process's virtual address space, must commit. */
      SetLockPagesPrivilege (GetCurrentProcess (), TRUE);
      baseAddress =
	VirtualAlloc (NULL, totalAllocateSize, MEM_LARGE_PAGES | MEM_COMMIT,
		      protection);
      SetLockPagesPrivilege (GetCurrentProcess (), FALSE);

      update_vmemIdentifier (identifier, (void *) baseAddress,
			     (void *) baseAddress, totalAllocateSize, mode,
			     largePageSize);
      Trc_PRT_vmem_hyvmem_reserve_memory_Exit (baseAddress);
      return (void *) baseAddress;
    }

  /* if here, error */
  update_vmemIdentifier (identifier, NULL, NULL, 0, 0, 0);
  Trc_PRT_vmem_hyvmem_reserve_memory_Exit4 ();
  return NULL;
}

#undef CDEV_CURRENT_FUNCTION

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
  /* empty */
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyvmem_startup
/**
 * PortLibrary startup.
 *
 * This function is called during startup of the portLibrary.  Any resources that are required for
 * the virtual memory operations may be created here.  All resources created here should be destroyed
 * in @ref hyvmem_shutdown.
 *
 * @param[in] portLibrary The port library.
 *
 * @return 0 on success, negative error code on failure.  Error code values returned are
 * \arg HYPORT_ERROR_STARTUP_VMEM
 *
 * @note Most implementations will simply return success.
 */
I_32 VMCALL
hyvmem_startup (struct HyPortLibrary *portLibrary)
{
  SYSTEM_INFO systemInfo;
  UDATA handle;
  UDATA (WINAPI * GetLargePageMinimumFunc) ();

  /* 0 terminate the table */
  memset (PPG_vmem_pageSize, 0, HYPORT_VMEM_PAGESIZE_COUNT * sizeof (UDATA));

  /* Default page size */
  GetSystemInfo (&systemInfo);
  PPG_vmem_pageSize[0] = (UDATA) systemInfo.dwPageSize;

  /* Determine if largePages are supported on this platform */
  /* Look for GetLargePageMinimum in the Kernel32 DLL */
  if (0 ==
      portLibrary->sl_open_shared_library (portLibrary, "Kernel32", &handle,
					   TRUE))
    {
      if (0 ==
	  portLibrary->sl_lookup_name (portLibrary, handle,
				       "GetLargePageMinimum",
				       (UDATA *) & GetLargePageMinimumFunc,
				       "PV"))
	{
	  PPG_vmem_pageSize[1] = GetLargePageMinimumFunc ();
	  /* Safety check, best guess if necessary */
	  if (PPG_vmem_pageSize[1] == 0)
	    {
	      PPG_vmem_pageSize[1] = 4194304;
	    }
	}

      if (portLibrary->sl_close_shared_library (portLibrary, handle))
	{
	  /* NLS has not yet initialized */
	  portLibrary->tty_printf (portLibrary,
				   "Failed to close DLL Kernel32\n");
	}
    }

  return 0;
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

#define CDEV_CURRENT_FUNCTION update_vmemIdentifier
/**
 * @internal
 * Update HyPortVmIdentifier structure
 *
 * @param[in] identifier The structure to be updated
 * @param[in] address Base address
 * @param[in] handle Platform specific handle for reserved memory
 * @param[in] byteAmount Size of allocated area
 * @param[in] mode Access Mode
 * @param[in] pageSize Constant describing pageSize
 */
void VMCALL
update_vmemIdentifier (HyPortVmemIdentifier * identifier, void *address,
		       void *handle, UDATA byteAmount, UDATA mode,
		       UDATA pageSize)
{
  identifier->address = address;
  identifier->handle = handle;
  identifier->size = byteAmount;
  identifier->pageSize = pageSize;
  identifier->mode = mode;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION SetLockPagesPrivilege
BOOL
SetLockPagesPrivilege (HANDLE hProcess, BOOL bEnable)
{
  struct
  {
    DWORD Count;
    LUID_AND_ATTRIBUTES Privilege[1];
  } Info;

  HANDLE Token;

  /* Open the token. */
  if (OpenProcessToken (hProcess, TOKEN_ADJUST_PRIVILEGES, &Token) != TRUE)
    {
      return FALSE;
    }

  /* Enable or disable? */
  Info.Count = 1;
  if (bEnable)
    {
      Info.Privilege[0].Attributes = SE_PRIVILEGE_ENABLED;
    }
  else
    {
      Info.Privilege[0].Attributes = 0;
    }

  /* Get the LUID. */
  if (LookupPrivilegeValue
      (NULL, SE_LOCK_MEMORY_NAME, &(Info.Privilege[0].Luid)) != TRUE)
    {
      return FALSE;
    }

  /* Adjust the privilege. */
  if (AdjustTokenPrivileges
      (Token, FALSE, (PTOKEN_PRIVILEGES) & Info, (DWORD) NULL, NULL,
       NULL) != TRUE)
    {
      return FALSE;
    }
  else
    {
      if (GetLastError () != ERROR_SUCCESS)
	{
	  return FALSE;
	}
    }

  CloseHandle (Token);

  return TRUE;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION getProtectionBits
DWORD
getProtectionBits (UDATA mode)
{
  if (0 != (HYPORT_VMEM_MEMORY_MODE_EXECUTE & mode))
    {
      if (0 != (HYPORT_VMEM_MEMORY_MODE_READ & mode))
	{
	  if (0 != (HYPORT_VMEM_MEMORY_MODE_WRITE & mode))
	    {
	      return PAGE_EXECUTE_READWRITE;
	    }
	  else
	    {
	      return PAGE_EXECUTE_READ;
	    }
	}
      else
	{
	  return PAGE_NOACCESS;
	}
    }
  else
    {
      if (0 != (HYPORT_VMEM_MEMORY_MODE_READ & mode))
	{
	  if (0 != (HYPORT_VMEM_MEMORY_MODE_WRITE & mode))
	    {
	      return PAGE_READWRITE;
	    }
	  else
	    {
	      return PAGE_READONLY;
	    }
	}
      else
	{
	  return PAGE_NOACCESS;
	}
    }
}

#undef CDEV_CURRENT_FUNCTION
