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

#include "portpriv.h"
#include "hyport.h"

#define CDEV_CURRENT_FUNCTION include_header
#include "hyshsem.h"
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshsem_open
/**
 * Open an existing semaphore set, or create a new one if it does not exist
 * 
 * @param[in] portLibrary The port library.
 * @param[out] handle A semaphore handle is allocated and initialised
 * for use with further calls, NULL on failure.
 * @param[in] semname Unique identifier of the semaphore.
 * @param[in] setSize Size of the semaphore set.
 * @param[in] permission Permission to the semaphore set.
 *
 * @return
 * \arg HYPORT_ERROR_SHSEM_OPFAILED   Failure - Error opening the semaphore
 * \arg HYPORT_INFO_SHSEM_CREATED Success - Semaphore has been created
 * \arg HYPORT_INFO_SHSEM_OPENED  Success - Existing semaphore has been opened
 * \arg HYPORT_INFO_SHSEM_SEMID_DIFF Success - Existing semaphore
 * opened, but OS Semaphore key is different
 */
IDATA VMCALL
hyshsem_open (struct HyPortLibrary *portLibrary,
              struct hyshsem_handle **handle, const char *semname,
              int setSize, int permission)
{
  return HYPORT_ERROR_SHSEM_OPFAILED;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshsem_post
/**
 * post operation increments the counter in the semaphore by 1 if
 * there is no one in wait for the semaphore.  if there are other
 * processes suspended by wait then one of them will become runnable
 * and the counter remains the same.
 * 
 * @param[in] portLibrary The port library.
 * @param[in] handle Semaphore set handle.
 * @param[in] semset The no of semaphore in the semaphore set that you
 * want to post.
 * @param[in] flag The semaphore operation flag:
 * \arg HYPORT_SHSEM_MODE_DEFAULT The default operation flag, same as 0
 * \arg HYPORT_SHSEM_MODE_UNDO The changes made to the semaphore will
 * be undone when this process finishes.
 *
 * @return 0 on success, -1 on failure.
 */
IDATA VMCALL
hyshsem_post (struct HyPortLibrary * portLibrary,
              struct hyshsem_handle * handle, UDATA semset, UDATA flag)
{
  return -1;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshsem_wait
/**
 * Wait operation decrements the counter in the semaphore set if the counter > 0
 * if counter == 0 then the caller will be suspended.
 * 
 * @param[in] portLibrary The port library.
 * @param[in] handle Semaphore set handle.
 * @param[in] semset The no of semaphore in the semaphore set that you
 * want to post.
 * @param[in] flag The semaphore operation flag:
 * \arg HYPORT_SHSEM_MODE_DEFAULT The default operation flag, same as 0
 * \arg HYPORT_SHSEM_MODE_UNDO The changes made to the semaphore will
 * be undone when this process finishes.
 * \arg HYPORT_SHSEM_MODE_NOWAIT The caller will not be suspended if
 * sem == 0, a -1 is returned instead.
 * 
 * @return 0 on success, -1 on failure.
 */
IDATA VMCALL
hyshsem_wait (struct HyPortLibrary * portLibrary,
              struct hyshsem_handle * handle, UDATA semset, UDATA flag)
{
  return -1;
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
 * @param[in] semset The number of semaphore in the semaphore set that
 * you want to post.
 * 
 * @return -1 on failure, the value of the semaphore on success
 * 
 * @warning: The user will need to make sure locking is done correctly
 * when accessing semaphore values. This is because getValue simply
 * reads the semaphore value without stopping the access to the
 * semaphore. Therefore the value of the semaphore can change before
 * the function returns.
 */
IDATA VMCALL
hyshsem_getVal (struct HyPortLibrary * portLibrary,
                struct hyshsem_handle * handle, UDATA semset)
{
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
 * @param[in] semset The no of semaphore in the semaphore set that you
 * want to post.
 * @param[in] value The value that you want to set the semaphore to
 * 
 * @warning The user will need to make sure locking is done correctly
 * when accessing semaphore values. This is because setValue simply
 * set the semaphore value without stopping the access to the
 * semaphore. Therefore the value of the semaphore can change before
 * the function returns.
 *
 * @return 0 on success, -1 on failure.
 */
IDATA VMCALL
hyshsem_setVal (struct HyPortLibrary * portLibrary,
                struct hyshsem_handle * handle, UDATA semset, IDATA value)
{
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
 * @note The actual semaphore is not destroyed.  Once the close
 * operation has been performed on the semaphore handle, it is no
 * longer valid and user needs to reissue @ref hyshsem_open call.
 */
void VMCALL
hyshsem_close (struct HyPortLibrary *portLibrary,
               struct hyshsem_handle **handle)
{
  return;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshsem_destroy
/**
 * Destroy the semaphore and release the resources allocated for the
 * semaphore handles.
 * 
 * @param[in] portLibrary The port library.
 * @param[in] handle Semaphore set handle.
 * 
 * @return 0 on success, -1 on failure.
 * @note Due to operating system restriction we may not be able to
 * destroy the semaphore
 */
IDATA VMCALL
hyshsem_destroy (struct HyPortLibrary * portLibrary,
                 struct hyshsem_handle ** handle)
{
  return -1;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshsem_startup
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
 * \arg HYPORT_ERROR_STARTUP_SHSEM
 *
 * @note Most implementations will simply return success.
 */
I_32 VMCALL
hyshsem_startup (struct HyPortLibrary * portLibrary)
{
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyshsem_shutdown
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
hyshsem_shutdown (struct HyPortLibrary *portLibrary)
{
}

#undef CDEV_CURRENT_FUNCTION
