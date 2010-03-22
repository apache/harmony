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
 * @brief Shared Semaphores
 */
#undef CDEV_CURRENT_FUNCTION

#include <Windows.h>
#include "hyport.h"
#include "portpriv.h"
#include "ut_hyprt.h"

#define CDEV_CURRENT_FUNCTION include_header
#include "hyshsem.h"
#undef CDEV_CURRENT_FUNCTION

#define HYPORT_SHSEM_CREATIONMUTEX "hyshsemcreationMutex"
#define HYPORT_SHSEM_WAITTIME (2000)	/* wait time in ms for creationMutex */
#define HYPORT_SHSEM_NAME_PREFIX "javasemaphore"

/* Used Internally */
#define DIR_SEP DIR_SEPARATOR

#define OK 0
#define SUCCESS 0

#define CDEV_CURRENT_FUNCTION _prototypes_private
IDATA createMutex (struct HyPortLibrary * portLibrary,
		   struct hyshsem_handle * shsem_handle);
static hyshsem_handle *createsemHandle (struct HyPortLibrary *portLibrary,
					int nsems, char *baseName);
IDATA openMutex (struct HyPortLibrary *portLibrary,
		 struct hyshsem_handle *shsem_handle);
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshsem_open
/**
 * Open an existing semaphore set, or create a new one if it does not exist
 * 
 * @param[in] portLibrary The port library.
 * @param[out] handle A semaphore handle is allocated and initialised for use with further calls, NULL on failure.
 * @param[in] semname Unique identifier of the semaphore.
 * @param[in] setSize Size of the semaphore set.
 * @param[in] permission Permission to the semaphore set.
 *
 * @return
 * \arg HYPORT_ERROR_SHSEM_OPFAILED   Failure - Error opening the semaphore
 * \arg HYPORT_INFO_SHSEM_CREATED Success - Semaphore has been created
 * \arg HYPORT_INFO_SHSEM_OPENED  Success - Existing semaphore has been opened
 * \arg HYPORT_INFO_SHSEM_SEMID_DIFF Success - Existing semaphore opened, but OS Semaphore key is different
 */
IDATA VMCALL
hyshsem_open (struct HyPortLibrary *portLibrary,
	      struct hyshsem_handle **handle, const char *semname,
	      int setSize, int permission)
{
  /* TODO: what happens if setSize == 0? We used to allow the setSize to be 0 so that when the user trying to open
     an existing semaphore they won't need to specify that. However because semaphore set is not part of Windows API
     so we are emulating it using seperate name - we need code to find out how large a semaphore set is */
  char baseFile[HYSH_MAXPATH], mutexName[HYSH_MAXPATH];
  char versionStr[32];
  hyshsem_handle *shsem_handle;
  I_32 rc;
  DWORD waitResult;

  Trc_PRT_shsem_hyshsem_open_Entry (semname, setSize, permission);
  GET_VERSION_STRING (portLibrary, versionStr);

  portLibrary->str_printf (portLibrary, baseFile, HYSH_MAXPATH, "%s%s_%s",
			   HYPORT_SHSEM_NAME_PREFIX, versionStr, semname);

  shsem_handle = (*handle) = createsemHandle (portLibrary, setSize, baseFile);
  if (!shsem_handle)
    {
      return HYPORT_ERROR_SHSEM_OPFAILED;
    }

  Trc_PRT_shsem_hyshsem_open_Debug1 (baseFile);

  /* Lock the creation mutex */
  waitResult =
    WaitForSingleObject (PPG_shsem_creationMutex, HYPORT_SHSEM_WAITTIME);
  if (WAIT_TIMEOUT == waitResult)
    {
      portLibrary->mem_free_memory (portLibrary, (*handle)->rootName);
      portLibrary->mem_free_memory (portLibrary, (*handle)->semHandles);
      portLibrary->mem_free_memory (portLibrary, *handle);
      *handle = NULL;
      return HYPORT_ERROR_SHSEM_OPFAILED;
    }

  /*First try and see whether the main mutex exists */
  portLibrary->str_printf (portLibrary, mutexName, HYSH_MAXPATH, "%s",
			   baseFile);
  shsem_handle->mainLock = OpenMutex (MUTEX_ALL_ACCESS, FALSE, mutexName);

  if (shsem_handle->mainLock == NULL)
    {
      Trc_PRT_shsem_hyshsem_open_Event1 (mutexName);
      rc = createMutex (portLibrary, shsem_handle);
    }
  else
    {
      Trc_PRT_shsem_hyshsem_open_Event2 (mutexName);
      rc = openMutex (portLibrary, shsem_handle);
    }

  /* release the creation mutex */
  ReleaseMutex (PPG_shsem_creationMutex);

  if (HYPORT_ERROR_SHSEM_OPFAILED == rc)
    {
      Trc_PRT_shsem_hyshsem_open_Exit1 ();
      portLibrary->file_error_message (portLibrary);
      return rc;
    }

  Trc_PRT_shsem_hyshsem_open_Exit (rc, (*handle));
  return rc;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshsem_post
/**
 * post operation increments the counter in the semaphore by 1 if there is no one in wait for the semaphore. 
 * if there are other processes suspended by wait then one of them will become runnable and 
 * the counter remains the same. 
 * 
 * @param[in] portLibrary The port library.
 * @param[in] handle Semaphore set handle.
 * @param[in] semset The no of semaphore in the semaphore set that you want to post.
 * @param[in] flag The semaphore operation flag:
 * \arg HYPORT_SHSEM_MODE_DEFAULT The default operation flag, same as 0
 * \arg HYPORT_SHSEM_MODE_UNDO The changes made to the semaphore will be undone when this process finishes.
 *
 * @return 0 on success, -1 on failure.
 */
IDATA VMCALL
hyshsem_post (struct HyPortLibrary * portLibrary,
	      struct hyshsem_handle * handle, UDATA semset, UDATA flag)
{
  Trc_PRT_shsem_hyshsem_post_Entry (handle, semset, flag);
  /* flag is ignored on Win32 for now - there is no Undo for semaphore */
  if (handle == NULL)
    {
      Trc_PRT_shsem_hyshsem_post_Exit1 ();
      return HYPORT_ERROR_SHSEM_HANDLE_INVALID;
    }

  if (semset < 0 || semset >= handle->setSize)
    {
      Trc_PRT_shsem_hyshsem_post_Exit2 ();
      return HYPORT_ERROR_SHSEM_SEMSET_INVALID;
    }

  if (ReleaseMutex (handle->semHandles[semset]))
    {
      Trc_PRT_shsem_hyshsem_post_Exit (0);
      return 0;
    }
  else
    {
      Trc_PRT_shsem_hyshsem_post_Exit3 (0, GetLastError ());
      return -1;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshsem_wait
/**
 * Wait operation decrements the counter in the semaphore set if the counter > 0
 * if counter == 0 then the caller will be suspended.
 * 
 * @param[in] portLibrary The port library.
 * @param[in] handle Semaphore set handle.
 * @param[in] semset The no of semaphore in the semaphore set that you want to post.
 * @param[in] flag The semaphore operation flag:
 * \arg HYPORT_SHSEM_MODE_DEFAULT The default operation flag, same as 0
 * \arg HYPORT_SHSEM_MODE_UNDO The changes made to the semaphore will be undone when this process finishes.
 * \arg HYPORT_SHSEM_MODE_NOWAIT The caller will not be suspended if sem == 0, a -1 is returned instead.
 * 
 * @return 0 on success, -1 on failure.
 */
IDATA VMCALL
hyshsem_wait (struct HyPortLibrary * portLibrary,
	      struct hyshsem_handle * handle, UDATA semset, UDATA flag)
{
  DWORD timeout, rc;

  Trc_PRT_shsem_hyshsem_wait_Entry (handle, semset, flag);

  if (handle == NULL)
    {
      Trc_PRT_shsem_hyshsem_wait_Exit1 ();
      return HYPORT_ERROR_SHSEM_HANDLE_INVALID;
    }
  if (semset < 0 || semset >= handle->setSize)
    {
      Trc_PRT_shsem_hyshsem_wait_Exit2 ();
      return HYPORT_ERROR_SHSEM_SEMSET_INVALID;
    }

  if (flag & HYPORT_SHSEM_MODE_NOWAIT)
    {
      timeout = 0;
    }
  else
    {
      timeout = INFINITE;
    }

  rc = WaitForSingleObject (handle->semHandles[semset], timeout);

  switch (rc)
    {
    case WAIT_ABANDONED:	/* This means someone has crash but hasn't relase the mutex, we are okay with this */
    case WAIT_OBJECT_0:
      Trc_PRT_shsem_hyshsem_wait_Exit (0);
      return 0;
    case WAIT_TIMEOUT:		/* Falls through */
    case WAIT_FAILED:
      Trc_PRT_shsem_hyshsem_wait_Exit3 (-1, rc);
      return -1;
    default:
      Trc_PRT_shsem_hyshsem_wait_Exit3 (-1, rc);
      return -1;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshsem_getVal
/**
 * reading the value of the semaphore in the set. This function
 * uses no synchronisation prmitives
  * 
 * @pre caller has to deal with synchronisation issue.
 *
 * @param[in] portLibrary The port library.
 * @param[in] handle Semaphore set handle.
 * @param[in] semset The number of semaphore in the semaphore set that you want to post.
 * 
 * @return -1 on failure, the value of the semaphore on success
 * 
 * @warning: The user will need to make sure locking is done correctly when
 * accessing semaphore values. This is because getValue simply reads the semaphore
 * value without stopping the access to the semaphore. Therefore the value of the semaphore
 * can change before the function returns. 
 */
IDATA VMCALL
hyshsem_getVal (struct HyPortLibrary * portLibrary,
		struct hyshsem_handle * handle, UDATA semset)
{
  Trc_PRT_shsem_hyshsem_getVal_Entry (*handle, semset);
  Trc_PRT_shsem_hyshsem_getVal_Exit (0);
  return -1;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshsem_setVal
/**
 * 
 * setting the value of the semaphore specified in semset. This function
 * uses no synchronisation prmitives
 * 
 * @pre Caller has to deal with synchronisation issue.
 * 
 * @param[in] portLibrary The port Library.
 * @param[in] handle Semaphore set handle.
 * @param[in] semset The no of semaphore in the semaphore set that you want to post.
 * @param[in] value The value that you want to set the semaphore to
 * 
 * @warning The user will need to make sure locking is done correctly when
 * accessing semaphore values. This is because setValue simply set the semaphore
 * value without stopping the access to the semaphore. Therefore the value of the semaphore
 * can change before the function returns. 
 *
 * @return 0 on success, -1 on failure.
 */
IDATA VMCALL
hyshsem_setVal (struct HyPortLibrary * portLibrary,
		struct hyshsem_handle * handle, UDATA semset, IDATA value)
{
  Trc_PRT_shsem_hyshsem_setVal_Entry (handle, semset, value);
  Trc_PRT_shsem_hyshsem_setVal_Exit (-1);
  return -1;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshsem_close
/**
 * Release the resources allocated for the semaphore handles.
 * 
 * @param[in] portLibrary The port library.
 * @param[in] handle Semaphore set handle.
 * 
 * @note The actual semaphore is not destroyed.  Once the close operation has been performed 
 * on the semaphore handle, it is no longer valid and user needs to reissue @ref hyshsem_open call.
 */
void VMCALL
hyshsem_close (struct HyPortLibrary *portLibrary,
	       struct hyshsem_handle **handle)
{
  U_32 i;
  hyshsem_handle *sem_handle = (*handle);

  Trc_PRT_shsem_hyshsem_close_Entry (*handle);

  if (*handle == NULL)
    {
      return;
    }

  for (i = 0; i < sem_handle->setSize; i++)
    {
      CloseHandle (sem_handle->semHandles[i]);
    }

  CloseHandle (sem_handle->mainLock);

  portLibrary->mem_free_memory (portLibrary, (*handle)->rootName);
  portLibrary->mem_free_memory (portLibrary, (*handle)->semHandles);
  portLibrary->mem_free_memory (portLibrary, *handle);
  *handle = NULL;

  Trc_PRT_shsem_hyshsem_close_Exit ();
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshsem_destroy
/**
 * Destroy the semaphore and release the resources allocated for the semaphore handles.
 * 
 * @param[in] portLibrary The port library.
 * @param[in] handle Semaphore set handle.
 * 
 * @return 0 on success, -1 on failure.
 * @note Due to operating system restriction we may not be able to destroy the semaphore
 */
IDATA VMCALL
hyshsem_destroy (struct HyPortLibrary *portLibrary,
		 struct hyshsem_handle **handle)
{
  Trc_PRT_shsem_hyshsem_destroy_Entry (*handle);
  /*On Windows this just maps to hyshsem_close */
  if ((*handle) == NULL)
    {
      return 0;
    }

  hyshsem_close (portLibrary, handle);
  Trc_PRT_shsem_hyshsem_close_Exit ();
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshsem_startup
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
 * \arg HYPORT_ERROR_STARTUP_SHSEM
 *
 * @note Most implementations will simply return success.
 */
I_32 VMCALL
hyshsem_startup (struct HyPortLibrary * portLibrary)
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
  PPG_shsem_creationMutex =
    CreateMutex (&secattr, FALSE, HYPORT_SHSEM_CREATIONMUTEX);
  if (NULL == PPG_shsem_creationMutex)
    {
      if (GetLastError () == ERROR_ALREADY_EXISTS)
	{
	  PPG_shsem_creationMutex =
	    OpenMutex (MUTEX_ALL_ACCESS, FALSE, HYPORT_SHSEM_CREATIONMUTEX);
	  if (NULL == PPG_shsem_creationMutex)
	    {
	      return HYPORT_ERROR_STARTUP_SHSEM;
	    }
	}
      else
	{
	  return HYPORT_ERROR_STARTUP_SHSEM;
	}
    }
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshsem_shutdown
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
hyshsem_shutdown (struct HyPortLibrary *portLibrary)
{
  CloseHandle (PPG_shsem_creationMutex);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION createsemHandle
static hyshsem_handle *
createsemHandle (struct HyPortLibrary *portLibrary, int nsems, char *baseName)
{
  hyshsem_handle *result;

  result =
    portLibrary->mem_allocate_memory (portLibrary, sizeof (hyshsem_handle));
  if (result == NULL)
    {
      return NULL;
    }

  result->rootName =
    portLibrary->mem_allocate_memory (portLibrary, strlen (baseName) + 1);
  if (result->rootName == NULL)
    {
      portLibrary->mem_free_memory (portLibrary, result);
      return NULL;
    }
  portLibrary->str_printf (portLibrary, result->rootName, HYSH_MAXPATH, "%s",
			   baseName);

  /*Allocating semHandle array */
  result->semHandles =
    portLibrary->mem_allocate_memory (portLibrary, nsems * sizeof (HANDLE));
  if (result->semHandles == NULL)
    {
      portLibrary->mem_free_memory (portLibrary, result->rootName);
      portLibrary->mem_free_memory (portLibrary, result);
      return NULL;
    }

  result->setSize = nsems;

  /* TODO: need to check whether baseName is too long - what should we do if it is?! */
  return result;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION createSemaphore
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION openSemaphore
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION createMutex
IDATA
createMutex (struct HyPortLibrary * portLibrary,
	     struct hyshsem_handle * shsem_handle)
{
  U_32 i;
  char mutexName[HYSH_MAXPATH];

  shsem_handle->mainLock = CreateMutex (NULL, TRUE, shsem_handle->rootName);
  if (shsem_handle->mainLock == NULL)
    {
      /* can't create mainlock, we can't do anything else :-( */
      return HYPORT_ERROR_SHSEM_OPFAILED;
    }

  for (i = 0; i < shsem_handle->setSize; i++)
    {
      HANDLE debugHandle;
      portLibrary->str_printf (portLibrary, mutexName, HYSH_MAXPATH,
			       "%s_set%d", shsem_handle->rootName, i);

      debugHandle = shsem_handle->semHandles[i] =
	CreateMutex (NULL, TRUE, mutexName);
      if (shsem_handle->semHandles[i] == NULL)
	{
	  return HYPORT_ERROR_SHSEM_OPFAILED;
	}
    }

  return HYPORT_INFO_SHSEM_CREATED;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION openMutex
IDATA
openMutex (struct HyPortLibrary * portLibrary,
	   struct hyshsem_handle * shsem_handle)
{
  /*Open and setup the mutex arrays */
  U_32 i;
  char mutexName[HYSH_MAXPATH];

  for (i = 0; i < shsem_handle->setSize; i++)
    {
      portLibrary->str_printf (portLibrary, mutexName, HYSH_MAXPATH,
			       "%s_set%d", shsem_handle->rootName, i);

      shsem_handle->semHandles[i] =
	OpenMutex (MUTEX_ALL_ACCESS, FALSE, mutexName);
      if (shsem_handle->semHandles[i] == NULL)
	{
	  U_32 j;
	  for (j = 0; j < i; j++)
	    {
	      CloseHandle (shsem_handle->semHandles[i]);
	    }
	  return HYPORT_ERROR_SHSEM_OPFAILED;
	}
    }

  return HYPORT_INFO_SHSEM_OPENED;
}

#undef CDEV_CURRENT_FUNCTION
