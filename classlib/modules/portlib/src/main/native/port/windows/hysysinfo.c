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
 * @brief System information
 */
#undef CDEV_CURRENT_FUNCTION

#include <stdio.h>
#include <windows.h>
#include "portpriv.h"
#include "hyportpg.h"

/* Missing from the ALPHA include files */
#if !defined(VER_PLATFORM_WIN32_WINDOWS)
#define VER_PLATFORM_WIN32_WINDOWS 1
#endif

#define CDEV_CURRENT_FUNCTION _prototypes_private
/* none */
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION _prototypes_public
IDATA VMCALL hysysinfo_get_executable_name (struct HyPortLibrary *
					    portLibrary, char *argv0,
					    char **result);
const char *VMCALL hysysinfo_get_OS_type (struct HyPortLibrary *portLibrary);
U_64 VMCALL hysysinfo_get_physical_memory (struct HyPortLibrary *portLibrary);
UDATA VMCALL hysysinfo_DLPAR_max_CPUs (struct HyPortLibrary *portLibrary);
UDATA VMCALL hysysinfo_get_number_CPUs (struct HyPortLibrary *portLibrary);
const char *VMCALL hysysinfo_get_CPU_architecture (struct HyPortLibrary
						   *portLibrary);
UDATA VMCALL hysysinfo_get_processing_capacity (struct HyPortLibrary
						*portLibrary);
const char *VMCALL hysysinfo_get_OS_version (struct HyPortLibrary
					     *portLibrary);
I_32 VMCALL hysysinfo_startup (struct HyPortLibrary *portLibrary);
UDATA VMCALL hysysinfo_DLPAR_enabled (struct HyPortLibrary *portLibrary);
void VMCALL hysysinfo_shutdown (struct HyPortLibrary *portLibrary);
UDATA VMCALL hysysinfo_get_pid (struct HyPortLibrary *portLibrary);
U_16 VMCALL hysysinfo_get_classpathSeparator (struct HyPortLibrary
					      *portLibrary);
IDATA VMCALL hysysinfo_get_username (struct HyPortLibrary *portLibrary,
				     char *buffer, UDATA length);
UDATA VMCALL hysysinfo_weak_memory_consistency (struct HyPortLibrary
						*portLibrary);
IDATA VMCALL hysysinfo_get_env (struct HyPortLibrary *portLibrary,
				char *envVar, char *infoString,
				UDATA bufSize);
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_get_CPU_architecture
/**
 * Determine the CPU architecture.
 *
 * @param[in] portLibrary The port library.
 *
 * @return A null-terminated string describing the CPU architecture of the hardware, NULL on error.
 * 
 * @note portLibrary is responsible for allocation/deallocation of returned buffer.
 * @note See http://www.tolstoy.com/samizdat/sysprops.html for good values to return.
 */
const char *VMCALL
hysysinfo_get_CPU_architecture (struct HyPortLibrary *portLibrary)
{
#if defined(_ALPHA_)
  return HYPORT_ARCH_ALPHA;
#elif defined(_ARM_)
  return HYPORT_ARCH_ARM;
#elif defined( _MIPS_)
  return HYPORT_ARCH_MIPS;
#elif defined(_PPC_)
  return HYPORT_ARCH_PPC;
#elif defined( _SH4_)
  return HYPORT_ARCH_SH4;
#elif defined( _SH3_)
  return HYPORT_ARCH_SH3;
#elif defined(_X86_)
  return HYPORT_ARCH_X86;
#elif defined( _AMD64_)
  return HYPORT_ARCH_HAMMER;
#else
  return "unknown";
#endif
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_get_env
/**
 * Query the operating system for environment variables.
 *
 * Obtain the value of the environment variable specified by envVar from the operating system 
 * and write out to supplied buffer.
 *
 * @param[in] portLibrary The port library.
 * @param[in] envVar Environment variable to query.
 * @param[out] infoString Buffer for value string describing envVar.
 * @param[in] bufSize Size of buffer to hold returned string.
 *
 * @return 0 on success, number of bytes required to hold the 
 *	information if the output buffer was too small, -1 on failure.
 *
 * @note infoString is undefined on error or when supplied buffer was too small.
 */
IDATA VMCALL
hysysinfo_get_env (struct HyPortLibrary * portLibrary, char *envVar,
		   char *infoString, UDATA bufSize)
{
  DWORD rc;

  rc = GetEnvironmentVariable (envVar, infoString, bufSize);

  /* If the function succeeds, the return value is the number of characters stored into 
     the buffer, not including the terminating null character. If the specified environment 
     variable name was not found in the environment block for the current process, the 
     return value is zero. 

     If the buffer pointed to by lpBuffer is not large enough, the return value is the buffer 
     size, in characters, required to hold the value string and its terminating null character. 
   */

  if (rc > bufSize)
    {
      return rc;
    }
  else if (rc == 0)
    {
      return -1;
    }
  else
    {
      return 0;			/* success */
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_get_OS_type
/**
 * Determine the OS type.
 * 
 * @param[in] portLibrary The port library.
 *
 * @return OS type string (NULL terminated) on success, NULL on error.
 *
 * @note portLibrary is responsible for allocation/deallocation of returned buffer.
 */
const char *VMCALL
hysysinfo_get_OS_type (struct HyPortLibrary *portLibrary)
{
  if (NULL == PPG_si_osType)
    {
      OSVERSIONINFOEX versionInfo;

      versionInfo.dwOSVersionInfoSize = sizeof (versionInfo);
      if (!GetVersionEx ((OSVERSIONINFO *) & versionInfo))
	{
	  return NULL;
	}

      switch (versionInfo.dwPlatformId)
	{
	case VER_PLATFORM_WIN32s:
	  PPG_si_osType = "Windows 3.1";
	  break;

	case VER_PLATFORM_WIN32_WINDOWS:
	  switch (versionInfo.dwMinorVersion)
	    {
	    case 0:
	      PPG_si_osType = "Windows 95";
	      break;
	    case 90:
	      PPG_si_osType = "Windows Me";
	      break;
	    default:
	      PPG_si_osType = "Windows 98";
	      break;
	    }
	  break;

	case VER_PLATFORM_WIN32_NT:
	  if (versionInfo.dwMajorVersion < 5)
	    {
	      PPG_si_osType = "Windows NT";
	    }
	  else
	    {
	      switch (versionInfo.dwMinorVersion)
		{
		case 0:
		  PPG_si_osType = "Windows 2000";
		  break;

		  /* case 1: WinNT 5.1 => Windows XP. Handled by the default. */

		case 2:
		  /* WinNT 5.2 can be either Win2003 Server or Workstation (e.g. XP64). 
		     Report workstation products as "Windows XP". */
		  if (versionInfo.wProductType == VER_NT_WORKSTATION)
		    {
		      PPG_si_osType = "Windows XP";
		    }
		  else
		    {
		      PPG_si_osType = "Windows Server 2003";
		    }
		  break;

		default:
		  PPG_si_osType = "Windows XP";
		  break;
		}
	    }
	  break;

	default:
	  PPG_si_osType = "unknown";
	  break;
	}
    }
  return PPG_si_osType;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_get_OS_version
/**
 * Determine version information from the operating system.
 *
 * @param[in] portLibrary The port library.
 *
 * @return OS version string (NULL terminated) on success, NULL on error.
 *
 * @note portLibrary is responsible for allocation/deallocation of returned buffer.
 */
const char *VMCALL
hysysinfo_get_OS_version (struct HyPortLibrary *portLibrary)
{
  if (NULL == PPG_si_osVersion)
    {
      OSVERSIONINFO versionInfo;
      int len = sizeof ("0123456789.0123456789 build 0123456789") + 1;
      char *buffer;
#if defined(UNICODE)
      int convSize;
      int position;
#endif /* UNICODE */

      versionInfo.dwOSVersionInfoSize = sizeof (OSVERSIONINFO);
      if (!GetVersionEx (&versionInfo))
	{
	  return NULL;
	}

#if defined(UNICODE)
      convSize =
	WideCharToMultiByte (CP_ACP, WC_COMPOSITECHECK,
			     versionInfo.szCSDVersion, -1, NULL, 0, NULL,
			     NULL);
      convSize = (convSize + 1) * 2;
      len = len + convSize + 2;
      buffer = portLibrary->mem_allocate_memory (portLibrary, len);
      if (NULL == buffer)
	{
	  return NULL;
	}
      position = sprintf (buffer, "%d.%d build %d%s",
			  versionInfo.dwMajorVersion,
			  versionInfo.dwMinorVersion,
			  versionInfo.dwBuildNumber & 0x0000FFFF,
			  versionInfo.szCSDVersion ? " " : "");
      WideCharToMultiByte (CP_ACP, WC_COMPOSITECHECK,
			   versionInfo.szCSDVersion, -1,
			   (WCHAR *) & buffer[position], convSize, NULL,
			   NULL);
      PPG_si_osVersion = buffer;
#else
      len = len + strlen (versionInfo.szCSDVersion) + 2;
      buffer = portLibrary->mem_allocate_memory (portLibrary, len);
      if (NULL == buffer)
	{
	  return NULL;
	}
      sprintf (buffer, "%d.%d build %d%s%s",
	       versionInfo.dwMajorVersion,
	       versionInfo.dwMinorVersion,
	       versionInfo.dwBuildNumber & 0x0000FFFF,
	       versionInfo.szCSDVersion ? " " : "",
	       versionInfo.szCSDVersion ? versionInfo.szCSDVersion : "");
      PPG_si_osVersion = buffer;
#endif

    }
  return PPG_si_osVersion;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_get_pid
/**
 * Determine the process ID of the calling process.
 *
 * @param[in] portLibrary The port library.
 *
 * @return the PID.
 */
UDATA VMCALL
hysysinfo_get_pid (struct HyPortLibrary * portLibrary)
{
  return GetCurrentProcessId ();
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_get_classpathSeparator
/**
 * Determine the character used to separate entries on the classpath.
 *
 * @param[in] portLibrary The port library.
 *
 * @return the classpathSeparator character.
 */
U_16 VMCALL
hysysinfo_get_classpathSeparator (struct HyPortLibrary * portLibrary)
{
  return ';';
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_get_executable_name
/**
 * Determines an absolute pathname for the executable.
 * 
 * @param[in] portLibrary The port library.
 * @param[in] argv0 argv[0] value
 * @param[out] result Null terminated pathname string
 * 
 * @return 0 on success, -1 on error (or information is not available).
 *
 * @note Caller is responsible for de-allocating memory for result buffer with @ref hymem_free_memory.
 */
IDATA VMCALL
hysysinfo_get_executable_name (struct HyPortLibrary * portLibrary,
			       char *argv0, char **result)
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
  temp = portLibrary->mem_allocate_memory (portLibrary, length + 1);
  if (!temp)
    {
      return -1;
    }
  length =
    WideCharToMultiByte (CP_ACP, WC_COMPOSITECHECK, osTemp, -1, temp, length,
			 NULL, NULL);
#else
  temp = portLibrary->mem_allocate_memory (portLibrary, length + 1);
  if (!temp)
    {
      return -1;
    }
  strcpy (temp, osTemp);
#endif

  *result = temp;
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_get_number_CPUs
/**
 * Determine the number of CPUs on this platform.
 *
 * @param[in] portLibrary The port library.
 *
 * @return The number of supported CPUs.
 */
UDATA VMCALL
hysysinfo_get_number_CPUs (struct HyPortLibrary * portLibrary)
{
  SYSTEM_INFO aSysInfo;

  GetSystemInfo (&aSysInfo);
  return aSysInfo.dwNumberOfProcessors;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_get_physical_memory
/**
 * Determine the size of the total physical memory in the system, in bytes.
 * 
 * @param[in] portLibrary The port library.
 *
 * @return 0 if the information was unavailable, otherwise total physical memory in bytes.
 */
U_64 VMCALL
hysysinfo_get_physical_memory (struct HyPortLibrary * portLibrary)
{
  MEMORYSTATUS aMemStatus;

  /* Function does not work properly on systems with more than 4 GB, when not running Win2K */
  SetLastError (ERROR_SUCCESS);
  GlobalMemoryStatus (&aMemStatus);
  if (GetLastError () != ERROR_SUCCESS)
    {
      return HYCONST64 (0);
    }

  return (U_64) aMemStatus.dwTotalPhys;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_DLPAR_enabled
/**
 * Determine if DLPAR (i.e. the ability to change number of CPUs and amount of memory dynamically)
 * is enabled on this platform.
 *
 * @param[in] portLibrary The port library.
 *
 * @return 1 if DLPAR is supported, otherwise 0.
 */
UDATA VMCALL
hysysinfo_DLPAR_enabled (struct HyPortLibrary * portLibrary)
{
  return FALSE;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_weak_memory_consistency
/**
 * Determine if the platform has weak memory consistency behaviour.
 * 
 * @param[in] portLibrary The port library.
 *
 * @return 1 if weak memory consistency, 0 otherwise.
 */
UDATA VMCALL
hysysinfo_weak_memory_consistency (struct HyPortLibrary * portLibrary)
{
  return FALSE;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_shutdown
/**
 * PortLibrary shutdown.
 *
 * This function is called during shutdown of the portLibrary.  Any resources that were created by @ref hysysinfo_startup
 * should be destroyed here.
 *
 * @param[in] portLibrary The port library.
 *
 * @note Most implementations will be empty.
 */
void VMCALL
hysysinfo_shutdown (struct HyPortLibrary *portLibrary)
{
  if (PPG_si_osVersion)
    {
      portLibrary->mem_free_memory (portLibrary, PPG_si_osVersion);
      PPG_si_osVersion = NULL;
    }

  /* PPG_si_osType is not dynamically allocated, so no need to free */
  PPG_si_osType = NULL;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_startup
/**
 * PortLibrary startup.
 *
 * This function is called during startup of the portLibrary.  Any resources that are required for
 * the system information operations may be created here.  All resources created here should be destroyed
 * in @ref hysysinfo_shutdown.
 *
 * @param[in] portLibrary The port library.
 *
 * @return 0 on success, negative error code on failure.  Error code values returned are
 * \arg HYPORT_ERROR_STARTUP_SYSINFO
 *
 * @note Most implementations will simply return success.
 */
I_32 VMCALL
hysysinfo_startup (struct HyPortLibrary *portLibrary)
{
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_DLPAR_max_CPUs
/**
 * Determine the maximum number of CPUs on this platform
 *
 * @param[in] portLibrary The port library.
 *
 * @return The maximum number of supported CPUs..
 */
UDATA VMCALL
hysysinfo_DLPAR_max_CPUs (struct HyPortLibrary * portLibrary)
{
  return portLibrary->sysinfo_get_number_CPUs (portLibrary);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_get_username
/**
 * Query the operating system for the name of the user associate with the current thread
 * 
 * Obtain the value of the name of the user associated with the current thread, and then
 * write it out into the buffer supplied by the user
 *
 * @param[in] portLibrary The port Library
 * @param[out] buffer Buffer for the name of the user
 * @param[in,out] length The length of the buffer
 *
 * @return 0 on success, number of bytes required to hold the 
 * information if the output buffer was too small, -1 on failure.
 *
 * @note buffer is undefined on error or when supplied buffer was too small.
 */
IDATA VMCALL
hysysinfo_get_username (struct HyPortLibrary * portLibrary, char *buffer,
			UDATA length)
{
  int resultLength = length;

  if (GetUserName (buffer, &resultLength))
    {
      return 0;
    }

  if (GetLastError () == ERROR_INSUFFICIENT_BUFFER)
    {
      return resultLength;
    }

  return -1;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hysysinfo_get_processing_capacity
/**
 * Determine the collective processing capacity available to the VM
 * in units of 1% of a physical processor. In environments without
 * some kind of virtual partitioning, this will simply be the number
 * of CPUs * 100.
 *
 * @param[in] portLibrary The port library.
 *
 * @return The processing capacity available to the VM.
 */
UDATA VMCALL
hysysinfo_get_processing_capacity (struct HyPortLibrary * portLibrary)
{
  return portLibrary->sysinfo_get_number_CPUs (portLibrary) * 100;
}

#undef CDEV_CURRENT_FUNCTION
