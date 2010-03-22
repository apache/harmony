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

#include <Windows.h>
#include <shlobj.h>
#include "hyport.h"
#include "portpriv.h"
#include "portnls.h"
#include "ut_hyprt.h"

#define CDEV_CURRENT_FUNCTION include_header
#include "hyshmem.h"
#undef CDEV_CURRENT_FUNCTION

#define ENV_APPDATA "APPDATA"
#define ENV_TEMP "TEMP"
#define DIR_TEMP "C:\\TEMP"
#define DIR_CROOT "C:\\"

#define SUCCESS 0
#define FAIL 1

#define HYPORT_SHMEM_CREATIONMUTEX "hyshmemcreationMutex"
#define HYPORT_SHMEM_WAITTIME (1000)	/* wait time in ms for creationMutex */

#define CDEV_CURRENT_FUNCTION _prototypes_private
static I_32 convertFileMapPerm (I_32 perm);
static I_32 convertPerm (I_32 perm);
static IDATA createDirectory (struct HyPortLibrary *portLibrary,
			      char *pathname);
static I_32 createMappedFile (struct HyPortLibrary *portLibrary,
			      struct hyshmem_handle *handle);
static void getNameFromSharedMemoryFileName (struct HyPortLibrary
					     *portLibrary, char *buffer,
					     UDATA size, const char *name);
static char *getSharedMemoryFileName (struct HyPortLibrary *portLibrary,
				      const char *rootName);
static char *getSharedMemoryPathandFileName (struct HyPortLibrary
					     *portLibrary,
					     const char
					     *sharedMemoryFileName);
static IDATA ensureDirectory (struct HyPortLibrary *portLibrary);
static I_32 findError (I_32 errorCode, I_32 errorCode2);
static UDATA isSharedMemoryFileName (struct HyPortLibrary *portLibrary,
				     const char *filename);
static void convertSlash (char *pathname);
static I_64 convertFileTimeToUnixEpoch (const FILETIME * time);
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
 * @param[out] handle This handle is required for further attach/destroy of the memory region
 * @param[in] rootname Shared name for the region, which used to identify the region. 
 * @param[in] size Size of the region in bytes
 * @param[in] perm permission for the region.
 * 
 * @return
 * \arg HYPORT_ERROR_SHMEM_OPFAILED Failure - Cannot open the shared memory region
 * \arg HYPORT_INFO_SHMEM_OPENED Success - Existing memory region has been opened
 * \arg HYPORT_INFO_SHMEM_CREATED Success - A new shared memory region has been created
 * 
 */
IDATA VMCALL
hyshmem_open (struct HyPortLibrary *portLibrary,
	      struct hyshmem_handle **handle, const char *rootname, I_32 size,
	      I_32 perm)
{
  IDATA rc = HYPORT_ERROR_SHMEM_OPFAILED;
  UDATA rootNameLength = 0;
  DWORD waitResult;
  struct hyshmem_handle *myHandle;

  Trc_PRT_shmem_hyshmem_open_Entry (rootname, size, perm);
  myHandle =
    portLibrary->mem_allocate_memory (portLibrary,
				      sizeof (struct hyshmem_handle));
  if (NULL == myHandle)
    {
      Trc_PRT_shmem_hyshmem_open_Exit (HYPORT_ERROR_SHMEM_OPFAILED, myHandle);
      return HYPORT_ERROR_SHMEM_OPFAILED;
    }

  if (ensureDirectory (portLibrary) == FAIL)
    {
      Trc_PRT_shmem_hyshmem_open_Exit (HYPORT_ERROR_SHMEM_OPFAILED, myHandle);
      return HYPORT_ERROR_SHMEM_OPFAILED;
    }

  /* Create filename to open */
  myHandle->rootName = getSharedMemoryFileName (portLibrary, rootname);
  if (NULL == myHandle->rootName)
    {
      portLibrary->mem_free_memory (portLibrary, myHandle->rootName);
      portLibrary->mem_free_memory (portLibrary, myHandle);
      Trc_PRT_shmem_hyshmem_open_Exit (HYPORT_ERROR_SHMEM_OPFAILED, myHandle);
      return HYPORT_ERROR_SHMEM_OPFAILED;
    }

  /* Lock the creation mutex */
  waitResult =
    WaitForSingleObject (PPG_shmem_creationMutex, HYPORT_SHMEM_WAITTIME);
  if (WAIT_TIMEOUT == waitResult)
    {
      portLibrary->mem_free_memory (portLibrary, myHandle->rootName);
      portLibrary->mem_free_memory (portLibrary, myHandle);
      Trc_PRT_shmem_hyshmem_open_Exit (HYPORT_ERROR_SHMEM_OPFAILED, myHandle);
      return HYPORT_ERROR_SHMEM_OPFAILED;
    }

#if defined(HYSHMEM_DEBUG)
  portLibrary->tty_printf (portLibrary,
			   "hyshmem_open: calling OpenFileMapping: %s\n",
			   myHandle->rootName);
#endif /* HYSHMEM_DEBUG */

  /* copy all the flags into the handle, so that createMappedFile knows what to do */
  myHandle->region = NULL;
  myHandle->perm = perm;
  myHandle->size = size;

  /* First we would try to see whether we can open an existing File mapping, if we can use it */
  myHandle->shmHandle =
    OpenFileMapping (convertFileMapPerm (perm), FALSE, myHandle->rootName);
  if (NULL == myHandle->shmHandle)
    {
      Trc_PRT_shmem_hyshmem_open_Event1 (myHandle->rootName);
      if (HYPORT_ERROR_SHMEM_OPFAILED ==
	  (rc = createMappedFile (portLibrary, myHandle)))
	{
	  portLibrary->mem_free_memory (portLibrary, myHandle->rootName);
	  portLibrary->mem_free_memory (portLibrary, myHandle);
	  Trc_PRT_shmem_hyshmem_open_Exit1 ();
	}
    }
  else
    {
      Trc_PRT_shmem_hyshmem_open_Event2 (myHandle->rootName);
      myHandle->mappedFile = 0;
      rc = HYPORT_INFO_SHMEM_OPENED;
    }

  /* release the creation mutex */
  ReleaseMutex (PPG_shmem_creationMutex);

  if (HYPORT_ERROR_SHMEM_OPFAILED == rc)
    {
      portLibrary->file_error_message (portLibrary);
      Trc_PRT_shmem_hyshmem_open_Exit1 ();
    }
  else
    {
      *handle = myHandle;
      Trc_PRT_shmem_hyshmem_open_Exit (rc, myHandle);
    }
  return rc;
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
  Trc_PRT_shmem_hyshmem_attach_Entry (handle);

  if (NULL != handle)
    {
      if (NULL != handle->region)
	{
	  Trc_PRT_shmem_hyshmem_attach_Exit (handle->region);
	  return handle->region;
	}
      if (NULL != handle->shmHandle)
	{
	  I_32 permission = convertFileMapPerm (handle->perm);
	  handle->region =
	    MapViewOfFile (handle->shmHandle, permission, 0, 0, 0);
	  if (NULL != handle->region)
	    {
	      Trc_PRT_shmem_hyshmem_attach_Exit (handle->region);
	      return handle->region;
	    }
	  else
	    {
	      Trc_PRT_shmem_hyshmem_attach_Exit2 (GetLastError ());
	    }
	}
    }

  Trc_PRT_shmem_hyshmem_attach_Exit1 ();
  return NULL;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_detach
/**
 * Detaches the shared memory region from the caller's process address space
 * Use @ref hyshmem_destroy to actually remove the memory region from the Operating system
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
  Trc_PRT_shmem_hyshmem_detach_Entry (*handle);
  if (NULL == (*handle)->region)
    {
      Trc_PRT_shmem_hyshmem_detach_Exit ();
      return 0;
    }

  if (UnmapViewOfFile ((*handle)->region))
    {
      (*handle)->region = NULL;
      Trc_PRT_shmem_hyshmem_detach_Exit ();
      return 0;
    }

  Trc_PRT_shmem_hyshmem_detach_Exit1 ();
  return -1;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_destroy
/**
 * Destroy and removes the shared memory region from OS.
 * 
 * The timing of which OS removes the memory is OS dependent. However when 
 * you make a call you can considered that you can no longer access the region through
 * the handle. Memory allocated for handle structure is freed as well.
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
  IDATA rc = 0;
  char *sharedMemoryMappedFile;

  Trc_PRT_shmem_hyshmem_destroy_Entry (*handle);

  sharedMemoryMappedFile =
    getSharedMemoryPathandFileName (portLibrary, (*handle)->rootName);
  if (NULL == sharedMemoryMappedFile)
    {
      return rc;
    }

  hyshmem_close (portLibrary, handle);

  rc = portLibrary->file_unlink (portLibrary, sharedMemoryMappedFile);

  Trc_PRT_shmem_hyshmem_destroy_Debug1 (sharedMemoryMappedFile, rc,
					GetLastError ());

  portLibrary->mem_free_memory (portLibrary, sharedMemoryMappedFile);

  if (-1 == rc)
    {
      Trc_PRT_shmem_hyshmem_destroy_Exit1 ();
    }
  else
    {
      Trc_PRT_shmem_hyshmem_destroy_Exit ();
    }

  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_shutdown
/**
 * PortLibrary shutdown.
 *
 * This function is called during shutdown of the portLibrary.  Any resources that were created by @ref hyshsem_startup
 * should be destroyed here.
 *
 * @param[in] portLibrary The port library.
 *
 * @note Most implementations will be empty.
 */
void VMCALL
hyshmem_shutdown (struct HyPortLibrary *portLibrary)
{
  if (PPG_shmem_directory != NULL)
    {
      portLibrary->mem_free_memory (portLibrary, PPG_shmem_directory);
    }

  CloseHandle (PPG_shmem_creationMutex);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_startup
/**
 * PortLibrary startup.
 *
 * This function is called during startup of the portLibrary.  Any resources that are required for
 * the file operations may be created here.  All resources created here should be destroyed
 * in @ref hyshsem_shutdown.
 *
 * @param[in] portLibrary The port library.
 *
 * @return 0 on success, negative error code on failure.  Error code values returned are
 * \arg HYPORT_ERROR_STARTUP_SHMEM
 *
 * @note Most implementations will simply return success.
 */
I_32 VMCALL
hyshmem_startup (struct HyPortLibrary *portLibrary)
{
  /* Security attributes for the creationMutex is set to public accessible */
  SECURITY_DESCRIPTOR secdes;
  SECURITY_ATTRIBUTES secattr;

  InitializeSecurityDescriptor (&secdes, SECURITY_DESCRIPTOR_REVISION);
  SetSecurityDescriptorDacl (&secdes, TRUE, NULL, TRUE);

  secattr.nLength = sizeof (SECURITY_ATTRIBUTES);
  secattr.lpSecurityDescriptor = &secdes;
  secattr.bInheritHandle = FALSE;

  /* Initialise the creationMutex */
  PPG_shmem_creationMutex =
    CreateMutex (&secattr, FALSE, HYPORT_SHMEM_CREATIONMUTEX);
  if (NULL == PPG_shmem_creationMutex)
    {
      if (GetLastError () == ERROR_ALREADY_EXISTS)
	{
	  PPG_shmem_creationMutex =
	    OpenMutex (MUTEX_ALL_ACCESS, FALSE, HYPORT_SHMEM_CREATIONMUTEX);
	  if (NULL == PPG_shmem_creationMutex)
	    {
	      return HYPORT_ERROR_STARTUP_SHMEM;
	    }
	}
      else
	{
	  return HYPORT_ERROR_STARTUP_SHMEM;
	}
    }

  /* make sure PPG_shmem_directory is null */
  PPG_shmem_directory = NULL;

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_close
/**
 * Detach, Close and remove the shared memory handle.
 * 
 * @note This method does not remove the shared memory region from the OS
 *	 use @ref hyshmem_destroy instead. However this will free all the memory
 * resources used by the handle, and detach the region specified by the handle
 *
 * @param[in] portLibrary The port Library
 * @param[in] handle Pointer to a valid shared memory handle
 */
void VMCALL
hyshmem_close (struct HyPortLibrary *portLibrary,
	       struct hyshmem_handle **handle)
{
  Trc_PRT_shmem_hyshmem_close_Entry (*handle);
  hyshmem_detach (portLibrary, handle);
  if (0 != (*handle)->mappedFile)
    {
      portLibrary->file_close (portLibrary, (*handle)->mappedFile);
    }
  CloseHandle ((*handle)->shmHandle);
  portLibrary->mem_free_memory (portLibrary, (*handle)->rootName);
  portLibrary->mem_free_memory (portLibrary, *handle);
  *handle = NULL;
  Trc_PRT_shmem_hyshmem_close_Exit ();
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
  Trc_PRT_shmem_hyshmem_findclose_Entry (findhandle);
  portLibrary->file_findclose (portLibrary, findhandle);
  Trc_PRT_shmem_hyshmem_findclose_Exit ();
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_findfirst
/**
 * Find the name of a shared memory region on the system. Answers a handle
 * to be used in subsequent calls to @ref hyshmem_findnext and @ref hyshmem_findclose. 
 *
 * @param[in] portLibrary The port library
 * @param[out] resultbuf filename and path matching path.
 *
 * @return valid handle on success, -1 on failure.
 */
UDATA VMCALL
hyshmem_findfirst (struct HyPortLibrary *portLibrary, char *resultbuf)
{
  UDATA findHandle;
  char file[HyMaxPath];

  Trc_PRT_shmem_hyshmem_findfirst_Entry ();

  if (ensureDirectory (portLibrary) == FAIL)
    {
      return -1;
    }

  findHandle =
    portLibrary->file_findfirst (portLibrary, PPG_shmem_directory, file);

  if (findHandle == -1)
    {
      Trc_PRT_shmem_hyshmem_findfirst_Exit1 ();
      return -1;
    }

  while (!isSharedMemoryFileName (portLibrary, file))
    {
      if (-1 == portLibrary->file_findnext (portLibrary, findHandle, file))
	{
	  portLibrary->file_findclose (portLibrary, findHandle);
	  Trc_PRT_shmem_hyshmem_findfirst_Exit2 ();
	  return -1;
	}
    }

  getNameFromSharedMemoryFileName (portLibrary, resultbuf, HyMaxPath, file);
  Trc_PRT_shmem_hyshmem_findfirst_Exit ();
  return findHandle;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_findnext
/**
 * Find the name of the next shared memory region.
 *
 * @param[in] portLibrary The port library
 * @param[in] findHandle handle returned from @ref hyshmem_findfirst.
 * @param[out] resultbuf next filename and path matching findhandle.
 *
 * @return 0 on success, -1 on failure or if no matching entries.
 */
I_32 VMCALL
hyshmem_findnext (struct HyPortLibrary * portLibrary, UDATA findHandle,
		  char *resultbuf)
{
  char file[HyMaxPath];

  Trc_PRT_shmem_hyshmem_findnext_Entry (findHandle);
  if (portLibrary->file_findnext (portLibrary, findHandle, file) == -1)
    {
      Trc_PRT_shmem_hyshmem_findnext_Exit1 ();
      return -1;
    }

  while (!isSharedMemoryFileName (portLibrary, file))
    {
      if (-1 == portLibrary->file_findnext (portLibrary, findHandle, file))
	{
	  Trc_PRT_shmem_hyshmem_findnext_Exit2 ();
	  return -1;
	}
    }

  getNameFromSharedMemoryFileName (portLibrary, resultbuf, HyMaxPath, file);
  Trc_PRT_shmem_hyshmem_findnext_Exit ();
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshmem_stat
/**
 * Return the statistic for a shared memory region
 *
 * @note notice that the implementation can decided to put -1 in the fields of statbuf
 * if it does not make sense on this platform, or it is impossible to obtain.
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
  char *sharedMemoryFile;
  HANDLE memHandle;
  BY_HANDLE_FILE_INFORMATION FileInformation;

  Trc_PRT_shmem_hyshmem_stat_Entry (name);
  if (statbuf == NULL)
    {
      return -1;
    }

  statbuf->nattach = 0;

  sharedMemoryFile = getSharedMemoryFileName (portLibrary, name);
  memHandle = OpenFileMapping (FILE_MAP_READ, FALSE, sharedMemoryFile);

  if (NULL == memHandle)
    {
      char *sharedMemoryFullPath;

      sharedMemoryFullPath =
	getSharedMemoryPathandFileName (portLibrary, sharedMemoryFile);
      memHandle =
	CreateFile (sharedMemoryFullPath, GENERIC_READ, 0, NULL,
		    OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);

      if (NULL == memHandle)
	{
	  Trc_PRT_shmem_hyshmem_stat_Exit2 (sharedMemoryFullPath);
	  portLibrary->mem_free_memory (portLibrary, sharedMemoryFullPath);
	  portLibrary->mem_free_memory (portLibrary, sharedMemoryFile);
	  CloseHandle (memHandle);
	  return -1;
	}

      portLibrary->mem_free_memory (portLibrary, sharedMemoryFullPath);
    }
  else
    {
      /* If we can open the file mapping, it means that someone else is using the memory mapping,
       * so we can assume there is at least 1 JVM attached to the memory area */
      statbuf->nattach = 1;
    }

  portLibrary->mem_free_memory (portLibrary, sharedMemoryFile);

  if (GetFileInformationByHandle (memHandle, &FileInformation) == 0)
    {
      /* We can't use GetFileInformationByHandle on shared memory handle, 
         so just tell caller we can't do anything about it */

      CloseHandle (memHandle);
      statbuf->file = NULL;
      statbuf->shmid = -1;
      statbuf->atime = -1;
      statbuf->dtime = -1;
      statbuf->chtime = -1;
      statbuf->perm = -1;
      Trc_PRT_shmem_hyshmem_stat_Exit ();
      return 0;
    }

  statbuf->file = NULL;
  statbuf->shmid = -1;
  statbuf->atime = -1;
  statbuf->dtime =
    convertFileTimeToUnixEpoch (&FileInformation.ftLastAccessTime);
  statbuf->chtime =
    convertFileTimeToUnixEpoch (&FileInformation.ftLastWriteTime);
  statbuf->perm = -1;

  CloseHandle (memHandle);
  Trc_PRT_shmem_hyshmem_stat_Exit ();
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION convertPerm
static I_32
convertPerm (I_32 perm)
{
  /*FIXME: for now, just return PAGE_READWRITE.
   * once we have defined our own permission flags we do the proper
   * convert */
  return PAGE_READWRITE;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION convertFileMapPerm
static I_32
convertFileMapPerm (I_32 perm)
{
  return FILE_MAP_ALL_ACCESS;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION convertSlash
static void
convertSlash (char *pathname)
{
  UDATA i;
  UDATA length = strlen (pathname);

  for (i = 0; i < length; i++)
    {
      if (pathname[i] == '\\')
	{
	  pathname[i] = '/';
	}
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION createDirectory
static IDATA
createDirectory (struct HyPortLibrary *portLibrary, char *pathname)
{
  char tempPath[HYSH_MAXPATH];
  char *current;

  /* 
   * TODO portlib mkdir does not takes permission, and does not return error code!
   * try making it directly, if we can then do it and save ourselves running the loop
   * also help detecting the case when the directory caller trying to make exist
   */
  if (0 == portLibrary->file_mkdir (portLibrary, pathname))
    {
      if (GetLastError () == ERROR_ALREADY_EXISTS)
	{
	  return 0;
	}
    }

  portLibrary->str_printf (portLibrary, tempPath, HYSH_MAXPATH, "%s",
			   pathname);

  current = strchr (tempPath + 1, DIR_SEPARATOR);	/* skip the first '/' */

  while (portLibrary->file_attr (portLibrary, pathname) != HyIsDir)
    {
      char *previous;

      *current = '\0';

#if defined(HYSHSEM_DEBUG)
      portLibrary->tty_printf (portLibrary, "mkdir %s\n", tempPath);
#endif

      if (portLibrary->file_mkdir (portLibrary, tempPath))
	{
	  /* TODO: need a way of getting error code from file port lib! */
	  if (GetLastError () != ERROR_ALREADY_EXISTS)
	    {
	      /* there is a geniune error, exit! */
	      return errno;
	    }
	}

      previous = current;
      current = strchr (current + 1, DIR_SEPARATOR);
      *previous = DIR_SEPARATOR;
    }

  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION createMappedFile
/**
 * @internal
 * Creates a file in mapped memory
 *
 * @param[in] portLibrary The port library
 * @param[in] handle the shared memory semaphore to be created
 *
 * @return HYPORT_ERROR_SHMEM_OPFAILED on failure, HYPORT_INFO_SHMEM_OPENED or HYPORT_INFO_SHMEM_CREATED on success
 */
static I_32
createMappedFile (struct HyPortLibrary *portLibrary,
		  struct hyshmem_handle *handle)
{
  I_32 rc;
  char *shareMemoryFileName;

  shareMemoryFileName =
    getSharedMemoryPathandFileName (portLibrary, handle->rootName);
  if (NULL == shareMemoryFileName)
    {
      return HYPORT_ERROR_SHMEM_OPFAILED;
    }

#if defined(HYSHMEM_DEBUG)
  portLibrary->tty_printf (portLibrary,
			   "createMappedFile - trying to create Memory Mapped file = %s!\n",
			   shareMemoryFileName);
#endif /* HYSHMEM_DEBUG */

  handle->mappedFile =
    portLibrary->file_open (portLibrary, shareMemoryFileName,
			    HyOpenCreateNew | HyOpenRead | HyOpenWrite, 0);
  if (-1 == handle->mappedFile)
    {
#if defined(HYSHMEM_DEBUG)
      portLibrary->tty_printf (portLibrary,
			       "createMappedFile - createNew failed, so old file must be there!\n");
#endif /*HYSHMEM_DEBUG */

      /* CreateNew failed, it probably means that memory mapped file is there */
      handle->mappedFile =
	portLibrary->file_open (portLibrary, shareMemoryFileName,
				HyOpenRead | HyOpenWrite, 0);
      if (-1 == handle->mappedFile)
	{
#if defined(HYSHMEM_DEBUG)
	  portLibrary->tty_printf (portLibrary,
				   "createMappedFile - Opening existing file failed - weird!\n");
#endif /*HYSHMEM_DEBUG */

	  portLibrary->mem_free_memory (portLibrary, shareMemoryFileName);
	  return HYPORT_ERROR_SHMEM_OPFAILED;
	}
      handle->size = 0;
      rc = HYPORT_INFO_SHMEM_OPENED;
    }
  else
    {
      /* MoveFileEx allows us to delete the share caceh after the next reboot */
      MoveFileEx (shareMemoryFileName, NULL, MOVEFILE_DELAY_UNTIL_REBOOT);
      rc = HYPORT_INFO_SHMEM_CREATED;
    }

  handle->shmHandle =
    CreateFileMapping ((HANDLE) handle->mappedFile, NULL,
		       convertPerm (handle->perm), 0, handle->size,
		       handle->rootName);
  if (NULL == handle->shmHandle)
    {
      /* Need to clean up the file */
      portLibrary->file_close (portLibrary, handle->mappedFile);
      portLibrary->file_unlink (portLibrary, shareMemoryFileName);

#if defined(HYSHMEM_DEBUG)
      portLibrary->tty_printf (portLibrary, "Error create file mapping\n");
#endif /*HYSHMEM_DEBUG */

      rc = HYPORT_ERROR_SHMEM_OPFAILED;
    }

  portLibrary->mem_free_memory (portLibrary, shareMemoryFileName);
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION getSharedMemoryFileName
static char *
getSharedMemoryFileName (struct HyPortLibrary *portLibrary,
			 const char *rootName)
{
  char sharedMemoryFile[HYSH_MAXPATH];
  char *result;
  char versionString[30];

  GET_VERSION_STRING (portLibrary, versionString);

  portLibrary->str_printf (portLibrary, sharedMemoryFile, HYSH_MAXPATH,
			   "%s_%s", versionString, rootName);

  result =
    portLibrary->mem_allocate_memory (portLibrary,
				      strlen (sharedMemoryFile) + 1);
  if (NULL == result)
    {
      return NULL;
    }

  portLibrary->str_printf (portLibrary, result, strlen (sharedMemoryFile) + 1,
			   sharedMemoryFile);
  return result;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION getSharedMemoryPathandFileName
static char *
getSharedMemoryPathandFileName (struct HyPortLibrary *portLibrary,
				const char *sharedMemoryFileName)
{
  char *result;
  UDATA resultLen;

  /* the string is PPG_shm_directory\sharedMemoryFileName\0 */
  resultLen =
    strlen (sharedMemoryFileName) + strlen (PPG_shmem_directory) + 2;
  result = portLibrary->mem_allocate_memory (portLibrary, resultLen);
  if (NULL == result)
    {
      return NULL;
    }

  portLibrary->str_printf (portLibrary, result, resultLen, "%s\\%s",
			   PPG_shmem_directory, sharedMemoryFileName);

  return result;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION getNameFromSharedMemoryFileName
static void
getNameFromSharedMemoryFileName (struct HyPortLibrary *portLibrary,
				 char *buffer, UDATA size, const char *name)
{
  char versionString[30];
  char *nameStart;

  GET_VERSION_STRING (portLibrary, versionString);
  nameStart = strstr (name, versionString);

  if (NULL == nameStart)
    {
      return;
    }

  nameStart = nameStart + strlen (versionString) + 1;

  portLibrary->str_printf (portLibrary, buffer, size, nameStart);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION isSharedMemoryFileName
static UDATA
isSharedMemoryFileName (struct HyPortLibrary *portLibrary,
			const char *filename)
{
  char versionString[30];
  GET_VERSION_STRING (portLibrary, versionString);

  return NULL != strstr (filename, versionString);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION convertFileTimeToUnixEpoch
static I_64
convertFileTimeToUnixEpoch (const FILETIME * time)
{
  /*This function is copied from hyfile_lastmod */
  I_64 tempResult, result;
  tempResult =
    ((I_64) time->dwHighDateTime << (I_64) 32) | (I_64) time->dwLowDateTime;

  result = (tempResult - 116444736000000000) / 10000000;

  return result;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION ensureDirectory
static IDATA
ensureDirectory (struct HyPortLibrary *portLibrary)
{
  I_32 rc;
  char *appdatadir;

  /* Create the directory specified by the two environmet variables,
   * If the environment variables are not found then  error
   */
  appdatadir =
    portLibrary->mem_allocate_memory (portLibrary, HYSH_MAXPATH + 1);
  if (NULL == appdatadir)
    {
      return FAIL;
    }

  rc = SHGetFolderPath (NULL, CSIDL_LOCAL_APPDATA, NULL, 0, appdatadir);
  if (FAILED (rc))
    {
      /* The sequence for which we look for the shared memory directory:
       * %APPDATA%, %TEMP%, c:\\temp, c:\\
       */
      if (-1 ==
	  portLibrary->sysinfo_get_env (portLibrary, ENV_APPDATA, appdatadir,
					HYSH_MAXPATH))
	{
	  if (-1 ==
	      portLibrary->sysinfo_get_env (portLibrary, ENV_TEMP, appdatadir,
					    HYSH_MAXPATH))
	    {
	      if (portLibrary->file_attr (portLibrary, DIR_TEMP) == HyIsDir)
		{
		  portLibrary->str_printf (portLibrary, appdatadir,
					   HYSH_MAXPATH, DIR_TEMP);
		}
	      else
		{
		  portLibrary->str_printf (portLibrary, appdatadir,
					   HYSH_MAXPATH, DIR_CROOT);
		}
	    }
	}
    }

  portLibrary->str_printf (portLibrary, appdatadir, HYSH_MAXPATH, "%s\\%s",
			   appdatadir, HYSH_BASEDIR);

  Trc_PRT_shmem_hyshmem_ensureDirectory_path (appdatadir);

  rc = portLibrary->file_attr (portLibrary, appdatadir);
  switch (rc)
    {
    case HyIsFile:
      break;
    case HyIsDir:
      PPG_shmem_directory = appdatadir;
      return SUCCESS;
    default:
      if (0 == createDirectory (portLibrary, appdatadir))
	{
	  PPG_shmem_directory = appdatadir;
	  return SUCCESS;
	}
    }
  portLibrary->mem_free_memory (portLibrary, appdatadir);
  return FAIL;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION findError
/**
 * @internal
 * Determines the proper portable error code to return given a native error code
 *
 * @param[in] errorCode The error code reported by the OS
 *
 * @return	the (negative) portable error code
 */
static I_32
findError (I_32 errorCode, I_32 errorCode2)
{
  switch (errorCode)
    {
    default:
      return HYPORT_ERROR_SHMEM_OPFAILED;
    }
}

#undef CDEV_CURRENT_FUNCTION
