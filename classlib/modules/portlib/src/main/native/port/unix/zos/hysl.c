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


#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <dll.h>
#include <stdlib.h> /* for malloc */

#include "hyport.h"
#include "portnls.h"

#define DMESSAGE(x)				/* printf x; */


static void getDLError(struct HyPortLibrary *portLibrary, char *errBuf, UDATA bufLen);


/** 
 * Opens a shared library .
 *
 * @param[in] portLibrary The port library.
 * @param[in] name path Null-terminated string containing the shared library.
 * @param[out] descriptor Pointer to memory which is filled in with shared-library handle on success.
 * @param[out] errBuf Buffer to contain an error message on failure.
 * @param[in] bufLen Size of errBuf.
 * @param[in] decorate Boolean value indicates whether name should be decorated if it contains path information and cannot be found.
 *
 * @return 0 on success, any other value on failure.
 *
 * @note contents of descriptor are undefined on failure.
 */
UDATA VMCALL
hysl_open_shared_library(struct HyPortLibrary *portLibrary, char *name, UDATA *descriptor, BOOLEAN decorate)
{
	void *handle;
	char *openName = name;
	char mangledName[1024];
	char errBuf[512];

	if (decorate) {
		char *p = strrchr(name, '/');
		if ( p ) {
			/* the names specifies a path */
			portLibrary->str_printf(portLibrary, mangledName, 1024, "%.*slib%s.so", (UDATA)p+1-(UDATA)name, name, p+1);
		} else {
			portLibrary->str_printf(portLibrary, mangledName, 1024, "lib%s.so", name);
		}
		openName = mangledName;
	} 

	handle = dllload( openName );
	if( handle == NULL ) {
		getDLError(portLibrary, errBuf, sizeof(errBuf));
		if (portLibrary->file_attr(portLibrary, openName) == HyIsFile) {
			return portLibrary->error_set_last_error_with_message(portLibrary, HYPORT_SL_INVALID, errBuf);
		} else {
			return portLibrary->error_set_last_error_with_message(portLibrary, HYPORT_SL_NOT_FOUND, errBuf);
		}
	}

	*descriptor = (UDATA) handle;
	return 0;
}

/**
 * Close a shared library.
 *
 * @param[in] portLibrary The port library.
 * @param[in] descriptor Shared library handle to close.
 *
 * @return 0 on success, any other value on failure.
 */
UDATA VMCALL
hysl_close_shared_library(struct HyPortLibrary *portLibrary, UDATA descriptor)
{
	int error;
	dllhandle *handle; 

	DMESSAGE (("\nClose library %x\n", *descriptor))
	handle = (dllhandle *)descriptor;
	error = dllfree (handle);

	return error;	
}

/**
 * Search for a function named 'name' taking argCount in the shared library 'descriptor'.
 *
 * @param[in] portLibrary The port library.
 * @param[in] descriptor Shared library to search.
 * @param[in] name Function to look up.
 * @param[out] func Pointer to the function.
 * @param[in] argSignature Argument signature.
 *
 * @return 0 on success, any other value on failure.
 *
 * argSignature is a C (ie: NUL-terminated) string with the following possible values for each character:
 *
 *		V	- void
 *		Z	- boolean
 *		B	- byte
 *		C	- char (16 bits)
 *		I	- integer (32 bits)
 *		J	- long (64 bits)
 *		F	- float (32 bits) 
 *		D	- double (64 bits) 
 *		L	- object / pointer (32 or 64, depending on platform)
 *      P	- pointer-width platform data. (in this context a
 *      IDATA)
 * 
 * Lower case signature characters imply unsigned value.
 * Upper case signature characters imply signed values.
 * If it doesn't make sense to be signed/unsigned (eg: V, L, F, D Z) the character is upper case.
 * 
 * argList[0] is the return type from the function.
 * The argument list is as it appears in english: list is left (1) to right (argCount)
 *
 * @note contents of func are undefined on failure.
 */
UDATA VMCALL
hysl_lookup_name(struct HyPortLibrary *portLibrary, UDATA descriptor, char *name, UDATA *func, const char *argSignature)
{
	void *address;
	dllhandle *handle;

	handle = (dllhandle *)descriptor;
	address = (void *)dllqueryfn( handle, name );
	if( address == NULL ) {
		return 1;
	}
	*func = (UDATA)address;
	return 0;
}

/**
 * PortLibrary shutdown.
 *
 * This function is called during shutdown of the portLibrary.  Any resources that were created by @ref hysl_startup
 * should be destroyed here.
 *
 * @param[in] portLibrary The port library.
 *
 * @note Most implementations will be empty.
 */
void VMCALL
hysl_shutdown(struct HyPortLibrary *portLibrary)
{
}

/**
 * PortLibrary startup.
 *
 * This function is called during startup of the portLibrary.  Any resources that are required for
 * the shared library operations may be created here.  All resources created here should be destroyed
 * in @ref hysl_shutdown.
 *
 * @param[in] portLibrary The port library.
 *
 * @return 0 on success, negative error code on failure.  Error code values returned are
 * \arg HYPORT_ERROR_STARTUP_SL
 *
 * @note Most implementations will simply return success.
 */
I_32 VMCALL
hysl_startup(struct HyPortLibrary *portLibrary)
{
	return 0;
}

static void
getDLError(struct HyPortLibrary *portLibrary, char *errBuf, UDATA bufLen)
{
	char *error;

	if (bufLen == 0) {
		return;
	}

	error = strerror(errno);
	if (error == NULL || error[0] == '\0') {
		/* just in case another thread consumed our error message */
		error =  portLibrary->nls_lookup_message(portLibrary, 
			HYNLS_ERROR|HYNLS_DO_NOT_APPEND_NEWLINE, 
			HYNLS_PORT_SL_UNKOWN_ERROR, 
			NULL);
	}

	strncpy(errBuf, error, bufLen);
	errBuf[bufLen - 1] = '\0';
}
