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

#include <stdlib.h>
#include <sys/utsname.h>

#include <sys/stat.h>
#include <limits.h>
#include <dirent.h>
#include <errno.h>
#include <string.h>
#include <pwd.h>
#include <sys/types.h>

#if defined(ZOS)
/* zOS does not define PATH_MAX, so just set it to be _POSIX_PATH_MAX */
#ifndef PATH_MAX
#define PATH_MAX _POSIX_PATH_MAX
#endif
/* Need to define this to get RTLD_NOW on zOS */
#ifndef __SUSV3
#define __SUSV3
#endif
#endif

#if defined(LINUX)
#include <sys/sysinfo.h>
#endif
#if defined(FREEBSD) || defined(MACOSX)
#include <sys/types.h>
#include <sys/sysctl.h>
#endif

#include <unistd.h>

#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <dlfcn.h>


#include "main_hlp.h"


#if !defined(LINUX)
static BOOLEAN isSymbolicLink (char *filename);
static IDATA cwdname (char **result);
static IDATA searchSystemPath (char *filename, char **result);
#endif
static IDATA readSymbolicLink (char *linkFilename, char **result);


int
main_get_executable_name (char *argv0, char **result)
{

#if defined(LINUX)
  return readSymbolicLink ("/proc/self/exe", result);
#else
  IDATA retval = -1;
  IDATA length;
  char *p;
  char *currentName = NULL;
  char *currentPath = NULL;
  char *originalWorkingDirectory = NULL;

  if (!argv0)
    {
      return -1;
    }
  currentPath = main_mem_allocate_memory(strlen (argv0) + 1);
  if (currentPath)
    {
      strcpy(currentPath, argv0);
    }
  if (!currentPath)
    {
      retval = -1;
      goto cleanup;
    }
  retval = cwdname(&originalWorkingDirectory);
  if (retval)
    {
      retval = -1;
      goto cleanup;
    }
gotPathName:
  /* split path into directory part and filename part. */
  p = strrchr (currentPath, '/');
  if (p)
    {
      *p++ = '\0';
      currentName = main_mem_allocate_memory(strlen (p) + 1);
      if (!currentName)
        {
          retval = -1;
          goto cleanup;
        }
      strcpy (currentName, p);
    }
  else
    {
      currentName = currentPath;
      currentPath = NULL;
      retval = searchSystemPath (currentName, &currentPath);
      if (retval)
        {
          retval = -1;
          goto cleanup;
        }
    }
  /* go there */
  if (currentPath)
    {
      if (currentPath[0])
        {
          if (0 != chdir (currentPath))
            {
              retval = -1;
              goto cleanup;
            }
        }
      main_mem_free_memory(currentPath);
      currentPath = NULL;
    }
  if (isSymbolicLink (currentName))
    {
      /* try to follow the link. */
      retval = readSymbolicLink (currentName, &currentPath);
      if (retval)
        {
          retval = -1;
          goto cleanup;
        }
      main_mem_free_memory(currentName);
      currentName = NULL;
      goto gotPathName;
    }
  retval = cwdname (&currentPath);
  if (retval)
    {
      retval = -1;
      goto cleanup;
    }
  /* Put name and path back together */
  *result = main_mem_allocate_memory(strlen(currentPath) + strlen(currentName) + 2);
  if (!*result)
    {
      retval = -1;
      goto cleanup;
    }
  strcpy (*result, currentPath);
  if (currentPath[0] && (currentPath[strlen (currentPath) - 1] != '/'))
    {
      strcat (*result, "/");
    }
  strcat (*result, currentName);
  /* Finished. */
  retval = 0;
cleanup:
  if (originalWorkingDirectory)
    {
      chdir (originalWorkingDirectory);
      main_mem_free_memory(originalWorkingDirectory);
      originalWorkingDirectory = NULL;
    }
  if (currentPath)
    {
      main_mem_free_memory(currentPath);
      currentPath = NULL;
    }
  if (currentName)
    {
      main_mem_free_memory(currentName);
      currentName = NULL;
    }
  return retval;
#endif
}

void *
main_mem_allocate_memory (int byteAmount)
{
	void *pointer = NULL;
	if (byteAmount == 0)
	{                           /* prevent malloc from failing causing allocate to return null */
		byteAmount = 1;
	}
	pointer = malloc(byteAmount);
	return pointer;
}

void
main_mem_free_memory (void *memoryPointer)
{
	free (memoryPointer);
}


#if !defined(LINUX)
/**
 * @internal  Examines the named file to determine if it is a symbolic link.  On platforms which don't have
 * symbolic links (or where we can't tell) or if an unexpected error occurs, just answer FALSE.
 */
static BOOLEAN
isSymbolicLink (char *filename)
{
  struct stat statbuf;
  if (!lstat (filename, &statbuf))
    {
      if (S_ISLNK (statbuf.st_mode))
        {
          return TRUE;
        }
    }
  return FALSE;
}

/**
 * @internal  Returns the current working directory.
 *
 * @return 0 on success, -1 on failure.
 *
 * @note The buffer to hold this string (including its terminating NUL) is allocated with
 * main_mem_allocate_memory.  The caller should free this memory with
 * main_mem_free_memory when it is no longer needed.
 */
static IDATA
cwdname (char **result)
{
  char *cwd;
  int allocSize = 256;

doAlloc:
  cwd = main_mem_allocate_memory(allocSize);
  if (!cwd)
    {
      return -1;
    }
  if (!getcwd (cwd, allocSize - 1))
    {
      main_mem_free_memory(cwd);
      if (errno == ERANGE)
        {
          allocSize += 256;
          goto doAlloc;
        }
      return -1;
    }
  *result = cwd;
  return 0;
}
#endif

/**
 * @internal  Attempts to read the contents of a symbolic link.  (The contents are the relative pathname of
 * the thing linked to).  A buffer large enough to hold the result (and the terminating NUL) is
 * allocated with main_mem_allocate_memory.  The caller should free this buffer with
 * main_mem_free_memory when it is no longer needed.
 * On success, returns 0.  On error, returns -1.
 */
static IDATA
readSymbolicLink (char *linkFilename,
                  char **result)
{
  char fixedBuffer[PATH_MAX + 1];
  int size = readlink (linkFilename, fixedBuffer, sizeof (fixedBuffer) - 1);
  if (size <= 0)
    {
      return -1;
    }
  fixedBuffer[size++] = '\0';
  *result = main_mem_allocate_memory(size);
  if (!*result)
    {
      return -1;
    }
  strcpy (*result, fixedBuffer);
  return 0;
}


#if !defined(LINUX)
/**
 * @internal  Searches through the system PATH for the named file.  If found, it returns the path entry
 * which matched the file.  A buffer large enough to hold the proper path entry (without a
 * trailing slash, but with the terminating NUL) is allocated with main_mem_allocate_memory.
 * The caller should free this buffer with main_mem_free_memory when it is no longer
 * needed.  On success, returns 0.  On error (including if the file is not found), -1 is returned.
 */
static IDATA
searchSystemPath (char *filename, char **result)
{
  char *pathCurrent;
  char *pathNext;
  int length;
  DIR *sdir = NULL;
  struct dirent *dirEntry;
  /* This should be sufficient for a single entry in the PATH var, though the var itself */
  /* could be considerably longer.. */
  char temp[PATH_MAX + 1];

  if (!(pathNext = getenv ("PATH")))
    {
      return -1;
    }

  while (pathNext)
    {
      pathCurrent = pathNext;
      pathNext = strchr (pathCurrent, ':');
      if (pathNext)
        {
          length = (pathNext - pathCurrent);
          pathNext += 1;
        }
      else
        {
          length = strlen (pathCurrent);
        }
      if (length > PATH_MAX)
        {
          length = PATH_MAX;
        }
      memcpy (temp, pathCurrent, length);
      temp[length] = '\0';

      if (!length)
        {                       /* empty path entry */
          continue;
        }
      if ((sdir = opendir (temp)))
        {
          while ((dirEntry = readdir (sdir)))
            {
              if (!strcmp (dirEntry->d_name, filename))
                {
                  closedir (sdir);
                  /* found! */
                  *result = main_mem_allocate_memory(strlen (temp) + 1);
                  if (!result)
                    {
                      return -1;
                    }
                  strcpy (*result, temp);
                  return 0;
                }
            }
          closedir (sdir);
        }
    }
  /* not found */
  return -1;
}
#endif

/**
 * Close a shared library.
 *
 * @param[in] descriptor Shared library handle to close.
 *
 * @return 0 on success, any other value on failure.
 */
int VMCALL
main_close_port_library (UDATA descriptor)
{
  return (UDATA) dlclose ((void *)descriptor);
}

/**
 * Opens a shared library .
 *
 * @param[out] descriptor Pointer to memory which is filled in with shared-library handle on success.
 *
 * @return 0 on success, any other value on failure.
 *
 * @note contents of descriptor are undefined on failure.
 */
#include <stdio.h>
int VMCALL
main_open_port_library (UDATA * descriptor)
{
  void *handle;
  char *openName = ("libhyprt" PLATFORM_DLL_EXTENSION);

  handle = dlopen (openName, RTLD_NOW);
  if (handle == NULL)
    {
puts(dlerror());
	  return -1;
    }

  *descriptor = (UDATA) handle;
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
 * @note contents of func are undefined on failure.
 */
UDATA VMCALL
main_lookup_name (UDATA descriptor, char *name, UDATA * func)
{
  void *address;
  address = dlsym ((void *)descriptor, name);
  if (address == NULL)
    {
      return 1;
    }
  *func = (UDATA) address;
  return 0;
}
