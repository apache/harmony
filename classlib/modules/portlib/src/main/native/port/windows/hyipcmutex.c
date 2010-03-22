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
 * @brief Shared Resource Mutex
 *
 * The HYIPCMutex is used to protect a shared resource from simultaneous access by processes
 * or threads executing in the same or different VMs.
 * Each process/thread must request and wait for the ownership of the shared resource before
 * it can use that resource. It must also release the ownership of the resource as soon as it
 * has finished using it so that other processes competing for the same resource are not delayed.
 */
#undef CDEV_CURRENT_FUNCTION

#include <windows.h>
#include "hyport.h"

#define CDEV_CURRENT_FUNCTION _prototypes_private
/* no prototypes */
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyipcmutex_acquire
/**
 * Acquires a named mutex for the calling process.
 *
 * If a Mutex with the same Name already exists, the function opens the existing Mutex and tries
 * to lock it. If another process already has the Mutex locked, the function will block indefinitely. 
 * If there is no Mutex with the same Name, the function will create it and lock it for the calling
 * process of this function.
 *
 * @param[in] portLibrary The port library
 * @param[in] name Mutex to be acquired
 *
 * @return 0 on success, -1 on error.
 *
 * @note The Mutex must be explicitly released by calling the @ref hyipcmutex_release function as 
 * soon as the lock is no longer required.
 */
I_32 VMCALL
hyipcmutex_acquire (struct HyPortLibrary * portLibrary, const char *name)
{
  HANDLE hMutex;
  DWORD dwResult;

  /* check length of name */
  if (strlen (name) > MAX_PATH)
    {
      return -1;
    }

  /* Create a named mutex or get its handle if it already exists */
  hMutex = CreateMutex (NULL, FALSE,	/* Not owned on creation */
			(LPCTSTR) name);	/* Mutex name */

  if (hMutex == NULL)
    {
      return -1;
    }
  else
    {
      /* Wait until mutex is released by other process */
      dwResult = WaitForSingleObject (hMutex,	/* Handle to mutex */
				      INFINITE);	/* Block indefinitely */

      /* if state of mutex was signaled within wait interval or mutex was abandoned, return true */
      /* otherwise function timeout, so return false */
      if (dwResult == WAIT_OBJECT_0 || dwResult == WAIT_ABANDONED)
	{
	  return 0;
	}
      else
	{
	  return -1;
	}
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyipcmutex_release
/**
 * Releases a named Mutex from the calling process.
 *
 * If a Mutex with the same Name already exists, the function opens the existing Mutex
 * and tries to unlock it.
 * This function will fail if a Mutex with the given Name is not found or if the Mutex
 * cannot be unlocked.
 *
 * @param[in] portLibrary The port library
 * @param[in] name Mutex to be released.
 *
 * @return 0 on success, -1 on error.
 *
 * @note Callers of this function must have called the function @ref hyipcmutex_acquire
 * prior to calling this function.
 */
I_32 VMCALL
hyipcmutex_release (struct HyPortLibrary * portLibrary, const char *name)
{
  HANDLE hMutex;

  /* check length of name */
  if (strlen (name) > MAX_PATH)
    {
      return -1;
    }

  /* Open handle to mutex */
  hMutex = OpenMutex (MUTEX_ALL_ACCESS, FALSE,	/* Will be ignored for existing mutex */
		      (LPCTSTR) name);	/* Mutex name */

  if (hMutex == NULL)
    {
      return -1;
    }

  /* release mutex */
  if (ReleaseMutex (hMutex) == 0)
    {
      return -1;
    }
  else
    {
      return 0;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyipcmutex_shutdown
/**
 * PortLibrary shutdown.
 *
 * This function is called during shutdown of the portLibrary.  Any resources that were created
 * by @ref hyipcmutex_startup should be destroyed here.
 *
 * @param[in] portLibrary The port library
 *
 * @note Most implementations will be empty.
 */
void VMCALL
hyipcmutex_shutdown (struct HyPortLibrary *portLibrary)
{
  /* empty */
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyipcmutex_startup
/**
 * PortLibrary startup.
 *
 * This function is called during startup of the portLibrary.  Any resources that are required for
 * the IPC mutex operations may be created here.  All resources created here should be destroyed
 * in @ref hyipcmutex_shutdown.
 *
 * @param[in] portLibrary The port library
 *
 * @return 0 on success, negative error code on failure.  Error code values returned are
 * \arg HYPORT_ERROR_STARTUP_IPCMUTEX
 *
 * @note Most implementations will simply return success.
 */
I_32 VMCALL
hyipcmutex_startup (struct HyPortLibrary *portLibrary)
{
  return 0;
}

#undef CDEV_CURRENT_FUNCTION
