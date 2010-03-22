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

/*
 * @file
 * @ingroup Thread
 */

#include <windows.h>
#include <stdlib.h>
#include "hycomp.h"
#include "hymutex.h"

#define CDEV_CURRENT_FUNCTION ostypes
/* ostypes */

typedef HANDLE OSTHREAD;
typedef DWORD TLSKEY;
typedef HANDLE COND;

#define WRAPPER_TYPE void _cdecl

typedef void *WRAPPER_ARG;

#define WRAPPER_RETURN() return

typedef HANDLE OSSEMAPHORE;
#undef CDEV_CURRENT_FUNCTION

#include "thrtypes.h"
#include "thrdsup.h"

const int priority_map[] = HY_PRIORITY_MAP;

/* Unused ID variable. */
DWORD unusedThreadID;

HyThreadLibrary default_library;

extern void VMCALL hythread_init (HyThreadLibrary * lib);
extern void VMCALL hythread_shutdown (void);
extern void fixupLocks386 (void);

#define CDEV_CURRENT_FUNCTION _prototypes_private
static BOOL WINAPI yield PROTOTYPE ((void));
#if (!defined(HYVM_STATIC_LINKAGE))
BOOL APIENTRY DllMain
PROTOTYPE ((HANDLE hModule, DWORD ul_reason_for_call, LPVOID lpReserved));
#endif /* !HYVM_STATIC_LINKAGE */

#undef CDEV_CURRENT_FUNCTION

BOOL (WINAPI * f_yield) (void);
#define CDEV_CURRENT_FUNCTION yield
static BOOL WINAPI
yield (void)
{
  Sleep (1);
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION DllMain
#if (!defined(HYVM_STATIC_LINKAGE))
/*
 * Initialize OS-specific threading helpers.
 * 
 * @param hModule handle to module being loaded
 * @param ul_reason_for_call reason why DllMain being called
 * @param lpReserved reserved
 * @return TRUE on success, FALSE on failure.
 */
BOOL APIENTRY
DllMain (HANDLE hModule, DWORD ul_reason_for_call, LPVOID lpReserved)
{
  switch (ul_reason_for_call)
    {
    case DLL_PROCESS_ATTACH:
      {
	hythread_library_t lib = GLOBAL_DATA (default_library);
	hythread_init (lib);
	if (lib->initStatus == 1)
	  {
	    OSVERSIONINFO versionInfo;
	    fixupLocks386 ();
	    /* SwitchToThread is not implemented on Win98 */
	    versionInfo.dwOSVersionInfoSize = sizeof (versionInfo);
	    if (GetVersionEx (&versionInfo))
	      {
		if (versionInfo.dwPlatformId == VER_PLATFORM_WIN32_NT)
		  {
		    HMODULE kernel32 = GetModuleHandle ("kernel32");
		    if (kernel32 != NULL)
		      {
			f_yield =
			  (BOOL (WINAPI *) (void)) GetProcAddress (kernel32,
								   "SwitchToThread");
		      }
		  }
	      }
	    if (f_yield == NULL)
	      f_yield = yield;
	  }
	return lib->initStatus == 1;
      }
    case DLL_PROCESS_DETACH:
      hythread_shutdown ();
    }
  return TRUE;
}
#endif /* !HYVM_STATIC_LINKAGE */
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION init_thread_library
#if (defined(HYVM_STATIC_LINKAGE))
/**
 * Perform OS-specific initializations for the threading library.
 * 
 * @return 0 on success or non-zero value on failure.
 */
IDATA
init_thread_library (void)
{
  hythread_library_t lib = GLOBAL_DATA (default_library);
  if (lib->initStatus == 0)
    {
      HANDLE mutex = CreateMutex (NULL, TRUE, "hythread_init_mutex");
      if (mutex == NULL)
	return -1;
      if (lib->initStatus == 0)
	{
	  hythread_init (lib);
	  if (lib->initStatus == 1)
	    {
	      atexit (hythread_shutdown);
	    }
	}
      ReleaseMutex (mutex);
      CloseHandle (mutex);
      if (lib->initStatus == 1)
	{
	  OSVERSIONINFO versionInfo;
	  fixupLocks386 ();
	  /* SwitchToThread is not implemented on Win98 */
	  versionInfo.dwOSVersionInfoSize = sizeof (versionInfo);
	  if (GetVersionEx (&versionInfo))
	    {
	      if (versionInfo.dwPlatformId == VER_PLATFORM_WIN32_NT)
		{
		  HMODULE kernel32 = GetModuleHandle ("kernel32");
		  if (kernel32 != NULL)
		    {
		      f_yield =
			(BOOL (WINAPI *) (void)) GetProcAddress (kernel32,
								 "SwitchToThread");
		    }
		}
	    }
	  if (f_yield == NULL)
	    f_yield = yield;
	}
    }
  return lib->initStatus != 1;
}
#endif /* HYVM_STATIC_LINKAGE */
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION initialize_thread_priority
/**
 * Initialize a thread's priority.
 * 
 * Here the threading library priority value is converted to the appropriate
 * OS-specific value.
 * 
 * @param[in] thread a thread
 * @return none
 */
void
initialize_thread_priority (hythread_t thread)
{
  IDATA priority, i;

  thread->priority = HYTHREAD_PRIORITY_NORMAL;

  if (priority_map[HYTHREAD_PRIORITY_MIN] ==
      priority_map[HYTHREAD_PRIORITY_MAX])
    return;

  priority = GetThreadPriority (thread->handle);

  /* are priorities mapped backwards? (WinCE does this.) */
  if (THREAD_PRIORITY_IDLE > THREAD_PRIORITY_TIME_CRITICAL)
    {
      for (i = HYTHREAD_PRIORITY_MAX; i >= HYTHREAD_PRIORITY_MIN; i--)
	{
	  if (priority <= priority_map[i])
	    {
	      thread->priority = i;
	      return;
	    }
	}
    }
  else
    {
      for (i = HYTHREAD_PRIORITY_MIN; i <= HYTHREAD_PRIORITY_MAX; i++)
	{
	  if (priority <= priority_map[i])
	    {
	      thread->priority = i;
	      return;
	    }
	}
    }
}

#undef CDEV_CURRENT_FUNCTION
