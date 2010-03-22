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
 * @brief Dump formatting
 */
#undef CDEV_CURRENT_FUNCTION

#include <windows.h>
#include <winnt.h>
#include <stdlib.h>
#include <float.h>

#include <process.h>
#include <dbghelp.h>

#include "hyport.h"
#include "hysignal.h"

typedef BOOL (WINAPI * PMINIDUMPWRITEDUMP) (IN HANDLE hProcess,
					    IN DWORD ProcessId,
					    IN HANDLE hFile,
					    IN MINIDUMP_TYPE DumpType,
					    IN CONST
					    PMINIDUMP_EXCEPTION_INFORMATION
					    ExceptionParam,
					    OPTIONAL IN CONST
					    PMINIDUMP_USER_STREAM_INFORMATION
					    UserStreamParam,
					    OPTIONAL IN CONST
					    PMINIDUMP_CALLBACK_INFORMATION
					    CallbackParam OPTIONAL);

PMINIDUMPWRITEDUMP dump_fn;

#define MINIDUMPWRITEDUMP "MiniDumpWriteDump"
#define DBGHELP_DLL "DBGHELP.DLL"

typedef struct _WriteDumpFileArgs
{
  PMINIDUMPWRITEDUMP dmp_function;
  HANDLE hDumpFile;
  MINIDUMP_EXCEPTION_INFORMATION *mdei;
} WriteDumpFileArgs, *PWriteDumpFileArgs;

#define HYUSE_UNIQUE_DUMP_NAMES "HYUNIQUE_DUMPS"
#define DUMP_FNAME_KEY "SOFTWARE\\Microsoft\\DrWatson"
#define DUMP_FNAME_VALUE "CrashDumpFile"

#define CDEV_CURRENT_FUNCTION _prototypes_private

static HANDLE openFileInCWD (struct HyPortLibrary *portLibrary,
			     char *fileNameBuff, U_32 fileNameBuffSize);

static HANDLE openFileFromEnvVar (struct HyPortLibrary *portLibrary,
				  char *envVarName, char *fileNameBuff,
				  U_32 fileNameBuffSize);

static HANDLE openFileFromReg (const char *keyName, const char *valName,
			       char *fileNameBuff, U_32 fileNameBuffSize);

static void writeDumpFile (PWriteDumpFileArgs args);

static HINSTANCE loadDumpLib (const char *dllName);

static PMINIDUMPWRITEDUMP linkDumpFn (HINSTANCE dllHandle,
				      const char *fnName);

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hydump_create
/**
 * Create a dump file of the OS state.
 *
 * @param[in] portLibrary The port library.
 * @param[in] filename Buffer for filename optionally containing the filename where dump is to be output.
 * @param[out] filename filename used for dump file or error message.
 * @param[in] dumpType Type of dump to perform.
 * @param[in] userData Implementation specific data.
 *
 * @return 0 on success, non-zero otherwise.
 *
 * @note filename buffer can not be NULL.
 * @note user allocates and frees filename buffer.
 * @note filename buffer length is platform dependent, assumed to be HyMaxPath/MAX_PATH
 *
 * @note if filename buffer is empty, a filename will be generated.
 * @note if HYUNIQUE_DUMPS is set, filename will be unique.
 */
UDATA VMCALL
hydump_create (struct HyPortLibrary *portLibrary, char *filename,
	       char *dumpType, void *userData)
{
  HANDLE hFile;
  WriteDumpFileArgs args;
  HINSTANCE dllHandle;
  HANDLE hThread;
  MINIDUMP_EXCEPTION_INFORMATION mdei;
  EXCEPTION_POINTERS exceptionPointers;
  HyWin32SignalInfo *info = (HyWin32SignalInfo *) userData;

  if (filename == NULL)
    {
      return 1;
    }

  if (*filename == '\0')
    {
      /* no file name provided, so generate one and open the file */

      hFile =
	openFileFromReg (DUMP_FNAME_KEY, DUMP_FNAME_VALUE, filename,
			 HyMaxPath);
      if (hFile == INVALID_HANDLE_VALUE)
	{
	  hFile =
	    openFileFromEnvVar (portLibrary, "USERPROFILE", filename,
				HyMaxPath);
	  if (hFile == INVALID_HANDLE_VALUE)
	    {
	      hFile = openFileInCWD (portLibrary, filename, HyMaxPath);
	      if (hFile == INVALID_HANDLE_VALUE)
		{
		  hFile =
		    openFileFromEnvVar (portLibrary, "TEMP", filename,
					HyMaxPath);
		  if (hFile == INVALID_HANDLE_VALUE)
		    {
		      hFile =
			openFileFromEnvVar (portLibrary, "TMP", filename,
					    HyMaxPath);
		    }
		}
	    }
	}
    }
  else
    {
      /* use name provided */
      /* 0666 is a guess...0?, 0666??...want to grant write permissions */
      hFile =
	(HANDLE) portLibrary->file_open (portLibrary, filename,
					 HyOpenWrite | HyOpenCreate |
					 HyOpenRead, 0666);
    }

  if (hFile == (HANDLE) - 1)
    {
      /*make sure useful */
      portLibrary->str_printf (portLibrary, filename, HyMaxPath,
			       "Dump failed - could not create dump file:\n");
      return 1;
    }

  /* Try to load JRE minidump module before the system one */
  dllHandle = loadDumpLib ("..\\bin\\" DBGHELP_DLL);

  dllHandle = loadDumpLib (DBGHELP_DLL);

  dump_fn = linkDumpFn (dllHandle, MINIDUMPWRITEDUMP);

  if (dump_fn == NULL)
    {
      portLibrary->str_printf (portLibrary, filename, HyMaxPath,
			       "Dump failed - could not link %s in %s\n",
			       MINIDUMPWRITEDUMP, DBGHELP_DLL);
      return 1;
    }

  /* collect exception data */
  if (info)
    {
      exceptionPointers.ExceptionRecord = info->ExceptionRecord;
      exceptionPointers.ContextRecord = info->ContextRecord;
      mdei.ThreadId = GetCurrentThreadId ();
      mdei.ExceptionPointers = &exceptionPointers;
      mdei.ClientPointers = TRUE;
    }

  args.dmp_function = dump_fn;
  args.hDumpFile = hFile;
  args.mdei = info ? &mdei : NULL;

  /* call createCrashDumpFile on a different thread so we can walk the stack back to our code */
  hThread = (HANDLE) _beginthread (writeDumpFile, 0, &args);

  /* exception thread waits while crash dump is printed on other thread */
  if ((HANDLE) - 1 == hThread)
    {
      portLibrary->str_printf (portLibrary, filename, HyMaxPath,
			       "Dump failed - could not begin dump thread\n");
      return 1;
    }
  else
    {
      WaitForSingleObject (hThread, INFINITE);
    }

  if ((portLibrary->file_close (portLibrary, (I_32) hFile)) == -1)
    {
      /*make sure useful */
      portLibrary->str_printf (portLibrary, filename, HyMaxPath,
			       "Dump failed - could not close dump file.\n");
      return 1;
    }
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION linkDumpFn
static PMINIDUMPWRITEDUMP
linkDumpFn (HINSTANCE dllHandle, const char *fnName)
{
  return (PMINIDUMPWRITEDUMP) GetProcAddress (dllHandle, fnName);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION loadDumpLib
/* will remove private from name once duplicate functionality removed from GPHandler */
static HINSTANCE
loadDumpLib (const char *dllName)
{
  HINSTANCE dbghelpDll = GetModuleHandle (dllName);

  if (dbghelpDll == NULL)
    {
      dbghelpDll = LoadLibrary (dllName);
    }

  return dbghelpDll;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION writeDumpFile
static void
writeDumpFile (PWriteDumpFileArgs args)
{
#define MAX_MINIDUMP_ATTEMPTS  10

  HANDLE hFile = args->hDumpFile;
  int i;

  for (i = 0; i < MAX_MINIDUMP_ATTEMPTS; i++)
    {
      BOOL ok = args->dmp_function (GetCurrentProcess (),
				    GetCurrentProcessId (),
				    hFile,
				    2,
				    args->mdei,
				    NULL,
				    NULL);

      if (ok)
	{
	  break;		/* dump taken! */
	}
      else
	{
	  Sleep (100);
	}
    }

  _endthread ();
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION openFileFromEnvVar
static HANDLE
openFileFromEnvVar (struct HyPortLibrary *portLibrary, char *envVarName,
		    char *fileNameBuff, U_32 fileNameBuffSize)
{
  HANDLE hFile;
  I_32 retCode;
  U_32 i;
  char pidStore[25];		/* This is roughly the 3 * sizeof(int)+1 which is more than enough space */

  if (portLibrary == NULL)
    {
      return INVALID_HANDLE_VALUE;
    }

  retCode =
    portLibrary->sysinfo_get_env (portLibrary, envVarName, fileNameBuff,
				  fileNameBuffSize);
  if (retCode != 0)
    {
      return INVALID_HANDLE_VALUE;
    }

  retCode =
    portLibrary->sysinfo_get_env (portLibrary, HYUSE_UNIQUE_DUMP_NAMES, NULL,
				  0);
  if (-1 != retCode)
    {
      pidStore[0] = '-';
      /* this env var is set to append a unique identifier to the dump file name */
      _itoa (GetCurrentProcessId (), &pidStore[1], 10);
    }
  else
    {
      /* we aren't using this feature so make this a zero-length string */
      pidStore[0] = 0;
    }

  i = strlen (fileNameBuff);
  i += strlen (pidStore);
  if (i + 8 > fileNameBuffSize)
    {
      return INVALID_HANDLE_VALUE;
    }
  strcat (fileNameBuff, "\\hy");
  strcat (fileNameBuff, pidStore);
  strcat (fileNameBuff, ".dmp");

  hFile = CreateFileA (fileNameBuff,
		       GENERIC_READ | GENERIC_WRITE,
		       0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);

  return hFile;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION openFileFromReg
static HANDLE
openFileFromReg (const char *keyName, const char *valName, char *fileNameBuff,
		 U_32 fileNameBuffSize)
{
  HANDLE hFile;
  HKEY hKey;
  LONG lRet;
  U_32 buffUsed = fileNameBuffSize;

  lRet = RegOpenKeyEx (HKEY_LOCAL_MACHINE,
		       keyName, 0, KEY_QUERY_VALUE, &hKey);
  if (lRet != ERROR_SUCCESS)
    {
      return INVALID_HANDLE_VALUE;
    }

  lRet = RegQueryValueEx (hKey,
			  valName,
			  NULL, NULL, (LPBYTE) fileNameBuff, &buffUsed);
  if (lRet != ERROR_SUCCESS)
    {
      return INVALID_HANDLE_VALUE;
    }

  RegCloseKey (hKey);

  hFile = CreateFileA (fileNameBuff,
		       GENERIC_READ | GENERIC_WRITE,
		       0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);

  return hFile;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION openFileInCWD
static HANDLE
openFileInCWD (struct HyPortLibrary *portLibrary, char *fileNameBuff,
	       U_32 fileNameBuffSize)
{
  HANDLE hFile;
  U_32 length;

  if (portLibrary == NULL)
    {
      return INVALID_HANDLE_VALUE;
    }

  length = GetModuleFileName (NULL, fileNameBuff, fileNameBuffSize);
  if (length == 0 || length > fileNameBuffSize || length < 3)
    {
      return INVALID_HANDLE_VALUE;
    }
  fileNameBuff[length - 1] = 'p';
  fileNameBuff[length - 2] = 'm';
  fileNameBuff[length - 3] = 'd';

  hFile = CreateFileA (fileNameBuff,
		       GENERIC_READ | GENERIC_WRITE,
		       0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);

  return hFile;
}

#undef CDEV_CURRENT_FUNCTION
