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

#include "hyport.h"

#define CDEV_CURRENT_FUNCTION hymmap_map_file
 /**
 * Map a file into memory.
 *
 * @param[in] portLibrary The port library
 * @param[in] path - the path of the file to mapped into memory.
 * @param[out] handle - updates *handle with the memory map handle, this handle is later passed to unmap.
 *
 * @return pointer to newly mapped memory on success, NULL on error.
 */
void *VMCALL
hymmap_map_file (struct HyPortLibrary *portLibrary, const char *path,
                 void **handle)
{
    UDATA numBytesRead;
	IDATA rc;
	void *mappedMemory;
	void *allocPointer;    
    IDATA file;
	UDATA size;

    /* default implementation will allocate memory and read the file into it */
    size = (UDATA) portLibrary->file_length(portLibrary, path);
	file = portLibrary->file_open(portLibrary, path, (I_32) HyOpenRead, (I_32) 0);

    if ( file == -1 ) {
		return NULL;
	}

    /* ensure that allocated memory is 8 byte aligned, just in case it matters */
	allocPointer = portLibrary->mem_allocate_memory(portLibrary, size + 8);
	
	if ( allocPointer == NULL ) {
		portLibrary->file_close(portLibrary, file);
		return NULL;
	}

	if ((UDATA)allocPointer%8) {
		mappedMemory = (void *) ((UDATA)allocPointer + 8 - ((UDATA)allocPointer%8));
	} else {
		mappedMemory = allocPointer;
	}

	numBytesRead = 0;
	rc = 0;
	while ( size - numBytesRead > 0 ) { 
		rc = portLibrary->file_read(portLibrary, file, (void *)(((UDATA)mappedMemory) + numBytesRead), size-numBytesRead);
		if ( rc == -1 ) {
			/* failed to completely read the file */
			portLibrary->mem_free_memory(portLibrary, allocPointer);
			return NULL;
		}
		numBytesRead += rc;
	}
	
	return mappedMemory;
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
hymmap_unmap_file(struct HyPortLibrary *portLibrary, void *handle) 
{
	portLibrary->mem_free_memory( portLibrary, handle );
}
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hymmap_shutdown
/**
 * PortLibrary shutdown.
 *
 * @param[in] portLibrary The port library
 *
 * This function is called during shutdown of the portLibrary.  Any resources that were created by @ref hymmap_startup
 * should be destroyed here.
 *
 */
void VMCALL
hymmap_shutdown(struct HyPortLibrary *portLibrary)
{
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
hymmap_startup(struct HyPortLibrary *portLibrary)
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
 * @return a bit map containing the capabilites supported by the hymmap sub component of the port library.
 * Possible bit values:
 *   HYPORT_MMAP_CAPABILITY_COPYONWRITE - if not present, platform is not capable of "copy on write" memory mapping.
 *
 */
I_32 VMCALL
hymmap_capabilities(struct HyPortLibrary *portLibrary)
{
	return HYPORT_MMAP_CAPABILITY_COPYONWRITE | HYPORT_MMAP_CAPABILITY_READ;
}
#undef CDEV_CURRENT_FUNCTION

