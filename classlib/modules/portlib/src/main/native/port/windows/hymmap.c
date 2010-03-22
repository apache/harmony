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
 * @brief Memory map
 *
 * This module provides memory mapping facilities that allow a user to map files
 * into the virtual address space of the process.  There are various options that
 * can be used when mapping a file into memory, such as copy on write.  Not all 
 * of these options are available on all platforms, @ref hymmap_capabilities 
 * provides the list of supported options.  Note also that on some platforms 
 * memory mapping facilites do not exist at all. On these platforms the API will
 * still be available, but will simply read the file into allocated memory.
 */
#undef CDEV_CURRENT_FUNCTION

#include <windows.h>

#include "hyport.h"

#define CDEV_CURRENT_FUNCTION _prototypes_private
/* no prototypes */
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hymmap_map_file
/**
 * Map a file into memory.
 *
 * @param[in] portLibrary The port library
 * @param[in] path - the path of the file to mapped into memory.
 * @param[out] handle - updates *handle with the memory map handle, this handle is
 *             later passed to unmap.
 *
 * @return pointer to newly mapped memory on success, NULL on error.
 */
void *VMCALL
hymmap_map_file (struct HyPortLibrary *portLibrary, const char *path,
		 void **handle)
{
  HANDLE file;
  HANDLE mapping;
  void *pointer;

  file =
    CreateFile (path, (GENERIC_READ | GENERIC_WRITE),
		(FILE_SHARE_READ | FILE_SHARE_WRITE), NULL, OPEN_ALWAYS,
		FILE_ATTRIBUTE_NORMAL, NULL);
  if (file == INVALID_HANDLE_VALUE)
    {
      return NULL;
    }
  mapping = CreateFileMapping (file, NULL, PAGE_WRITECOPY, 0, 0, NULL);
  if (mapping == NULL)
    {
      CloseHandle (file);
      return NULL;
    }
  pointer = MapViewOfFile (mapping, FILE_MAP_COPY, 0, 0, 0);
  CloseHandle (mapping);
  CloseHandle (file);

  *handle = pointer;
  return pointer;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hymmap_unmap_file
/**
 * UnMap previously mapped memory.
 *
 * @param[in] portLibrary The port library
 *
 * @param[in] handle - the handle from the mmap_map_file.
 */
void VMCALL
hymmap_unmap_file (struct HyPortLibrary *portLibrary, void *handle)
{
  UnmapViewOfFile (handle);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hymmap_shutdown
/**
 * PortLibrary shutdown.
 *
 * @param[in] portLibrary The port library
 *
 * This function is called during shutdown of the portLibrary.  Any resources that
 * were created by @ref hymmap_startup should be destroyed here.
 *
 */
void VMCALL
hymmap_shutdown (struct HyPortLibrary *portLibrary)
{
  /* empty */
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hymmap_startup
/**
 * PortLibrary startup.
 *
 * This function is called during startup of the portLibrary.  Any resources that are required for
 * the memory mapping operations may be created here.  All resources created here should be destroyed
 * in @ref hymmap_shutdown.
 *
 * @param[in] portLibrary The port library
 *
 * @return 0 on success, negative error code on failure.  Error code values returned are
 * \arg HYPORT_ERROR_STARTUP_MMAP
 *
 * @note Most implementations will simply return success.
 */
I_32 VMCALL
hymmap_startup (struct HyPortLibrary *portLibrary)
{
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hymmap_capabilities
/**
 * Check the capabilities available for HYMMAP at runtime for the current platform.
 *
 *
 * @param[in] portLibrary The port library
 *
 * @return a bit map containing the capabilites supported by the hymmap sub component
 *         of the port library.
 * Possible bit values:
 *   HYPORT_MMAP_CAPABILITY_COPYONWRITE - if not present, platform is not capable of "copy on write" memory mapping.
 *
 */
I_32 VMCALL
hymmap_capabilities (struct HyPortLibrary * portLibrary)
{
  return HYPORT_MMAP_CAPABILITY_COPYONWRITE;
}

#undef CDEV_CURRENT_FUNCTION
