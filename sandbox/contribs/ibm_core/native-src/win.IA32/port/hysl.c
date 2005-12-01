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
 * @brief shared library
 */
#undef CDEV_CURRENT_FUNCTION

#include <windows.h>
#include "hyport.h"
#include <stdio.h>
#include <stdlib.h>
#include <malloc.h>
#include "hylib.h"
#include "portnls.h"

#if !defined(HINSTANCE_ERROR)
#define	HINSTANCE_ERROR	32
#endif

#define CDEV_CURRENT_FUNCTION _prototypes_private
UDATA VMCALL EsSharedLibraryLookupName (UDATA descriptor, char *name,
					UDATA * func);
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION EsSharedLibraryLookupName
UDATA VMCALL
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

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysl_open_shared_library
/** 
 * Opens a shared library.
 *
 * @param[in] portLibrary The port library.
 * @param[in] name path Null-terminated string containing the shared library.
 * @param[out] descriptor Pointer to memory which is filled in with shared-library handle on success.
 * @param[in] decorate Boolean value indicates whether name should be decorated
 *            if it contains path information and cannot be found.
 *
 * @return 0 on success, any other value on failure.
 *
 * @note contents of descriptor are undefined on failure.
 */
UDATA VMCALL
hysl_open_shared_library (struct HyPortLibrary * portLibrary, char *name,
			  UDATA * descriptor, BOOLEAN decorate)
{
  HINSTANCE dllHandle;
  UINT prevMode;
  DWORD error;
  UDATA notFound;
  const char *errorMessage;
  char errBuf[512];
  char mangledName[1024];
  char *openName = name;

  if (decorate)
    {
      portLibrary->str_printf (portLibrary, mangledName, 1024, "%s.dll",
			       name);
      openName = mangledName;
    }				/* TODO make windows not try to append .dll if we do not want it to */

  prevMode = SetErrorMode (SEM_NOOPENFILEERRORBOX | SEM_FAILCRITICALERRORS);

  /* LoadLibrary will try appending .DLL if necessary */
  dllHandle = LoadLibrary ((LPCSTR) openName);
  if (dllHandle >= (HINSTANCE) HINSTANCE_ERROR)
    {
      *descriptor = (UDATA) dllHandle;
      SetErrorMode (prevMode);
      return 0;
    }

  error = GetLastError ();

  notFound = (error == ERROR_MOD_NOT_FOUND || error == ERROR_DLL_NOT_FOUND);
  if (notFound)
    {
      /* try to report a better error message.  Check if the library can be found at all. */
      dllHandle =
	LoadLibraryEx ((LPCSTR) openName, NULL, DONT_RESOLVE_DLL_REFERENCES);
      if (dllHandle)
	{
	  if (sizeof (errBuf))
	    {
	      errorMessage = portLibrary->nls_lookup_message (portLibrary,
							      HYNLS_ERROR |
							      HYNLS_DO_NOT_APPEND_NEWLINE,
							      HYNLS_PORT_SL_UNABLE_TO_RESOLVE_REFERENCES,
							      NULL);
	      strncpy (errBuf, errorMessage, sizeof (errBuf));
	      errBuf[sizeof (errBuf) - 1] = '\0';
	    }
	  FreeLibrary (dllHandle);
	  SetErrorMode (prevMode);
	  return portLibrary->error_set_last_error_with_message (portLibrary,
								 HYPORT_SL_INVALID,
								 errBuf);
	}
    }

  if (sizeof (errBuf))
    {
      LPWSTR message = NULL;
      int nameSize = strlen (name);
      LPWSTR filename =
	(LPWSTR) portLibrary->mem_allocate_memory (portLibrary,
						   nameSize * 2 + 2);

      if (filename == NULL)
	{
	  errorMessage = portLibrary->nls_lookup_message (portLibrary,
							  HYNLS_ERROR |
							  HYNLS_DO_NOT_APPEND_NEWLINE,
							  HYNLS_PORT_SL_INTERNAL_ERROR,
							  NULL);
	  portLibrary->str_printf (portLibrary, errBuf, sizeof (errBuf),
				   errorMessage, error);
	  errBuf[sizeof (errBuf) - 1] = '\0';
	  SetErrorMode (prevMode);
	  return portLibrary->error_set_last_error_with_message (portLibrary,
								 notFound ?
								 HYPORT_SL_NOT_FOUND
								 :
								 HYPORT_SL_INVALID,
								 errBuf);
	}

      MultiByteToWideChar (CP_ACP, 0, (LPCTSTR) name, -1, filename,
			   nameSize * 2 + 2);
      filename[nameSize] = '\0';

      FormatMessageW (FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_ARGUMENT_ARRAY | FORMAT_MESSAGE_MAX_WIDTH_MASK, NULL, error, MAKELANGID (LANG_NEUTRAL, SUBLANG_DEFAULT),	/* Default language */
		      (LPWSTR) & message, 0, (va_list *) & filename);

      portLibrary->mem_free_memory (portLibrary, filename);

      if (message)
	{
	  WideCharToMultiByte (CP_UTF8, 0, (LPCWSTR) message, -1, errBuf,
			       sizeof (errBuf) - 1, NULL, NULL);
	  LocalFree (message);
	}
      else
	{
	  errorMessage = portLibrary->nls_lookup_message (portLibrary,
							  HYNLS_ERROR |
							  HYNLS_DO_NOT_APPEND_NEWLINE,
							  HYNLS_PORT_SL_INTERNAL_ERROR,
							  NULL);
	  portLibrary->str_printf (portLibrary, errBuf, sizeof (errBuf),
				   errorMessage, error);
	}
      errBuf[sizeof (errBuf) - 1] = '\0';
    }

  SetErrorMode (prevMode);

  return portLibrary->error_set_last_error_with_message (portLibrary,
							 notFound ?
							 HYPORT_SL_NOT_FOUND :
							 HYPORT_SL_INVALID,
							 errBuf);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysl_close_shared_library
/**
 * Close a shared library.
 *
 * @param[in] portLibrary The port library.
 * @param[in] descriptor Shared library handle to close.
 *
 * @return 0 on success, any other value on failure.
 */
UDATA VMCALL
hysl_close_shared_library (struct HyPortLibrary * portLibrary,
			   UDATA descriptor)
{
  if (descriptor < HINSTANCE_ERROR)
    {
      return 2;
    }
  FreeLibrary ((HINSTANCE) descriptor);
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysl_lookup_name
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
UDATA VMCALL
hysl_lookup_name (struct HyPortLibrary * portLibrary, UDATA descriptor,
		  char *name, UDATA * func, const char *argSignature)
{
  char *mangledName;
  UDATA result;

  result = EsSharedLibraryLookupName (descriptor, name, func);
  if (result != 0)
    {
      int i, len, pad = 0;

      /* + 7 comes from: 256 parameters * 8 bytes each = 2048 bytes -> 4 digits + @ symbol + leading '_' + NULL */
      size_t length = sizeof (char) * (strlen (name) + 7);
      if (!(mangledName = (char *) alloca (length)))
	{
	  return 1;
	}

      len = strlen (argSignature);

      for (i = 1; i < len; i++)
	{
	  if (argSignature[i] == 'J' || argSignature[i] == 'D')
	    {
	      pad++;
	    }
	}

      portLibrary->str_printf (portLibrary, mangledName, length, "_%s@%d",
			       name, (len + pad - 1) * 4);
      result = EsSharedLibraryLookupName (descriptor, mangledName, func);
    }
  return result;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysl_shutdown
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
hysl_shutdown (struct HyPortLibrary *portLibrary)
{
  /* empty */
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysl_startup
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
hysl_startup (struct HyPortLibrary *portLibrary)
{
  return 0;
}

#undef CDEV_CURRENT_FUNCTION
