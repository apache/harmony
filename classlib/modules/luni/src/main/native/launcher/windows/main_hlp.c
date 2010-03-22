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

#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <malloc.h>

#include "main_hlp.h"

int
main_get_executable_name (char *argv0, char **result)
{
	char *temp;
	TCHAR osTemp[_MAX_PATH + 2];
	DWORD length;

	(void) argv0;			/* unused */

	length = GetModuleFileName (NULL, osTemp, _MAX_PATH + 1);
	if (!length || (length >= _MAX_PATH))
	{
		return -1;
	}
	osTemp[length] = (TCHAR) '\0';	/* jic */

#if defined(UNICODE)
	length =
		WideCharToMultiByte (CP_ACP, WC_COMPOSITECHECK, osTemp, -1, NULL, 0, NULL,
				NULL);
	temp = main_mem_allocate_memory (length + 1);
	if (!temp)
	{
		return -1;
	}
	length =
		WideCharToMultiByte (CP_ACP, WC_COMPOSITECHECK, osTemp, -1, temp, length,
				NULL, NULL);
#else
	temp = main_mem_allocate_memory (length + 1);
	if (!temp)
	{
		return -1;
	}
	strcpy (temp, osTemp);
#endif

*result = temp;
return 0;
}

void *
main_mem_allocate_memory(int byteAmount)
{
	void *pointer = NULL;
	if (byteAmount == 0)
	{				/* prevent GlobalLock from failing causing allocate to return null */
		byteAmount = 1;
	}
	pointer = HeapAlloc (GetProcessHeap(), 0, byteAmount);
	return pointer;
}

void
main_mem_free_memory(void *memoryPointer)
{
	HeapFree (GetProcessHeap(), 0, memoryPointer);
}

#if !defined(HINSTANCE_ERROR)
#define	HINSTANCE_ERROR	32
#endif

static UDATA
EsSharedLibraryLookupName (UDATA descriptor, char *name, UDATA * func)
{
  UDATA lpfnFunction;

  if (descriptor < HINSTANCE_ERROR)
    {
      return 3;
    }
  lpfnFunction =
    (UDATA) GetProcAddress ((HINSTANCE) descriptor, (LPCSTR) name);
  if (lpfnFunction == (UDATA) NULL)
    {
      return 4;
    }
  *func = lpfnFunction;
  return 0;
}

/** 
 * Opens the port library.
 *
 * @param[out] descriptor Pointer to memory which is filled in with shared-library handle on success.
 * 
 * @return 0 on success, any other value on failure.
 *
 * @note contents of descriptor are undefined on failure.
 */
int
main_open_port_library (UDATA * descriptor)
{
  HINSTANCE dllHandle;
  UINT prevMode;

  prevMode = SetErrorMode (SEM_NOOPENFILEERRORBOX | SEM_FAILCRITICALERRORS);

  /* LoadLibrary will try appending .DLL if necessary */
  dllHandle = LoadLibrary ("hyprt");
  if (dllHandle >= (HINSTANCE) HINSTANCE_ERROR)
    {
      *descriptor = (UDATA) dllHandle;
      SetErrorMode (prevMode);
      return 0;
    }
  return -1;
}

/**
 * Close the port library.
 *
 * @param[in] descriptor Shared library handle to close.
 *
 * @return 0 on success, any other value on failure.
 */
int 
main_close_port_library (UDATA descriptor)
{
  if (descriptor < HINSTANCE_ERROR)
    {
      return 2;
    }
  FreeLibrary ((HINSTANCE) descriptor);
  return 0;
}

/**
 * Search for a function named 'name' taking argCount in the shared library 'descriptor'.
 *
 * @param[in] descriptor Shared library to search.
 * @param[in] name Function to look up.
 * @param[out] func Pointer to the function.
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
 *		P	- pointer-width platform data. (in this context an IDATA)
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
UDATA 
main_lookup_name (UDATA descriptor, char *name, UDATA * func)
{
  return EsSharedLibraryLookupName (descriptor, name, func);
}