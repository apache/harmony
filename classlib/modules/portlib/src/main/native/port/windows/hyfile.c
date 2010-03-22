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
 * @brief file
 */
#undef CDEV_CURRENT_FUNCTION

#include <windows.h>
#include <sys/stat.h>
#include <time.h>
#include "hyport.h"
#include "portpriv.h"
#include "hystdarg.h"
#include "portnls.h"
#include "ut_hyprt.h"

#define CDEV_CURRENT_FUNCTION _prototypes_private

static I_32 findError (I_32 errorCode);
void convert_path_to_unicode(struct HyPortLibrary * portLibrary, const char *path,
	     wchar_t **pathW);

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION findError
/**
 * @internal
 * Determines the proper portable error code to return given a native error code
 *
 * @param[in] errorCode The error code reported by the OS
 *
 * @return	the (negative) portable error code
 */
static I_32
findError (I_32 errorCode)
{
  switch (errorCode)
    {
    case ERROR_FILENAME_EXCED_RANGE:
      return HYPORT_ERROR_FILE_NAMETOOLONG;
    case ERROR_ACCESS_DENIED:
      return HYPORT_ERROR_FILE_NOPERMISSION;
    case ERROR_FILE_NOT_FOUND:
      return HYPORT_ERROR_FILE_NOTFOUND;
    case ERROR_DISK_FULL:
      return HYPORT_ERROR_FILE_DISKFULL;
    case ERROR_FILE_EXISTS:
    case ERROR_ALREADY_EXISTS:
      return HYPORT_ERROR_FILE_EXIST;
    case ERROR_NOT_ENOUGH_MEMORY:
      return HYPORT_ERROR_FILE_SYSTEMFULL;
    case ERROR_LOCK_VIOLATION:
      return HYPORT_ERROR_FILE_LOCKED;
    default:
      return HYPORT_ERROR_FILE_OPFAILED;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_attr
/**
 * Determine whether path is a file or directory.
 *
 * @param[in] portLibrary The port library
 * @param[in] path file/path name being queried.
 *
 * @return EslsFile if a file, EslsDir if a directory, negative portable error code on failure.
 */
I_32 VMCALL
hyfile_attr (struct HyPortLibrary * portLibrary, const char *path)
{
  DWORD result;
  wchar_t *pathW;

  convert_path_to_unicode(portLibrary, path, &pathW);
  result = GetFileAttributesW ((LPCWSTR) pathW);
  portLibrary->mem_free_memory(portLibrary, pathW);

  if (result == 0xFFFFFFFF)
    {
      result = GetLastError ();
      return portLibrary->error_set_last_error (portLibrary, result,
						findError (result));
    }

  if (result & FILE_ATTRIBUTE_DIRECTORY)
    {
      return HyIsDir;
    }

  /* otherwise assume it's a normal file */
  return HyIsFile;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_close
/**
 * Closes a file descriptor.
 *
 * @param[in] portLibrary The port library
 * @param[in] fd The file descriptor.
 *
 * @return 0 on success, -1 on failure.
 * @internal @todo return negative portable return code on failure.
 */
I_32 VMCALL
hyfile_close (struct HyPortLibrary * portLibrary, IDATA fd)
{
  if (CloseHandle ((HANDLE) fd))
    {
      return 0;
    }
  else
    {
      I_32 error = GetLastError ();
      portLibrary->error_set_last_error (portLibrary, error,
					 findError (error));
      return -1;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_error_message
/**
 * Return an error message describing the last OS error that occurred.  The last
 * error returned is not thread safe, it may not be related to the operation that
 * failed for this thread.
 *
 * @param[in] portLibrary The port library
 *
 * @return	error message describing the last OS error, may return NULL.
 *
 * @internal
 * @note  This function gets the last error code from the OS and then returns
 * the corresponding string.  It is here as a helper function for JCL.  Once hyerror
 * is integrated into the port library this function should probably disappear.
 */
const char *VMCALL
hyfile_error_message (struct HyPortLibrary *portLibrary)
{
  return portLibrary->error_last_error_message (portLibrary);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_findclose
/**
 * Close the handle returned from @ref hyfile_findfirst.
 *
 * @param[in] portLibrary The port library
 * @param[in] findhandle  Handle returned from @ref hyfile_findfirst.
 */
void VMCALL
hyfile_findclose (struct HyPortLibrary *portLibrary, UDATA findhandle)
{
  if (0 == FindClose ((HANDLE) findhandle))
    {
      I_32 error = GetLastError ();
      portLibrary->error_set_last_error (portLibrary, error,
					 findError (error));
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_findfirst
/**
 * Find the first occurrence of a file identified by path.  Answers a handle
 * to be used in subsequent calls to @ref hyfile_findnext and @ref hyfile_findclose. 
 *
 * @param[in] portLibrary The port library
 * @param[in] path file/path name being queried.
 * @param[out] resultbuf filename and path matching path.
 *
 * @return valid handle on success, -1 on failure.
 */
UDATA VMCALL
hyfile_findfirst (struct HyPortLibrary *portLibrary, const char *path,
		  char *resultbuf)
{
  WIN32_FIND_DATAW lpFindFileDataW;
  char newPath[HyMaxPath];
  HANDLE result;
  wchar_t *pathW;  
  
  strcpy (newPath, path);
  strcat (newPath, "*");
  
  convert_path_to_unicode(portLibrary, newPath, &pathW);
  result = FindFirstFileW ((LPCWSTR) pathW, &lpFindFileDataW);
  portLibrary->mem_free_memory(portLibrary, pathW);

  if (result == INVALID_HANDLE_VALUE)
    {
      I_32 error = GetLastError ();
      portLibrary->error_set_last_error (portLibrary, error,
					 findError (error));
      return (UDATA) - 1;
    }
  WideCharToMultiByte(CP_UTF8, 0, lpFindFileDataW.cFileName, -1, resultbuf, HyMaxPath, NULL, NULL);  
  return (UDATA) result;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_findnext
/**
 * Find the next filename and path matching a given handle.
 *
 * @param[in] portLibrary The port library
 * @param[in] findhandle handle returned from @ref hyfile_findfirst.
 * @param[out] resultbuf next filename and path matching findhandle.
 *
 * @return 0 on success, -1 on failure or if no matching entries.
 * @internal @todo return negative portable return code on failure.
 */
I_32 VMCALL
hyfile_findnext (struct HyPortLibrary * portLibrary, UDATA findhandle,
		 char *resultbuf)
{
  WIN32_FIND_DATAW lpFindFileDataW;  
  if (!FindNextFileW ((HANDLE) findhandle, &lpFindFileDataW))
    {
      I_32 error = GetLastError ();
      portLibrary->error_set_last_error (portLibrary, error,
					 findError (error));
      return -1;
    }
  WideCharToMultiByte(CP_UTF8, 0, lpFindFileDataW.cFileName, -1, resultbuf, HyMaxPath, NULL, NULL); 
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_lastmod
/**
 *  Return the last modification time of the file path in milliseconds.
 *
 * @param[in] portLibrary The port library
 * @param[in] path file/path name being queried.
 *
 * @return last modification time on success, -1 on failure.
 */
I_64 VMCALL
hyfile_lastmod (struct HyPortLibrary * portLibrary, const char *path)
{
    FILETIME lastWriteTime;
    HANDLE newHandle;
    I_64 result, tempResult;
    I_32 error;
    wchar_t *pathW;

    convert_path_to_unicode(portLibrary, path, &pathW);
    newHandle = CreateFileW(pathW, FILE_READ_ATTRIBUTES,  
        FILE_SHARE_READ,  
        NULL,  
        OPEN_EXISTING,  
        FILE_FLAG_BACKUP_SEMANTICS,  
        NULL);
    portLibrary->mem_free_memory(portLibrary, pathW);

    if (newHandle == INVALID_HANDLE_VALUE)
    {
        error = GetLastError ();
        portLibrary->error_set_last_error (portLibrary, error,
            findError (error));
        return -1;
    }

    if (0 == GetFileTime(newHandle, NULL, NULL, &lastWriteTime)) {
        error = GetLastError ();          
    	portLibrary->error_set_last_error (portLibrary, error,
			findError (error));
		return -1;
	}

    /*
    * Search MSDN for 'Converting a time_t Value to a File Time' for following implementation.
    */
    tempResult =
        ((I_64) lastWriteTime.dwHighDateTime << (I_64) 32) | (I_64) lastWriteTime.dwLowDateTime;
	result = (tempResult - 116444736000000000) / 10000;

    if (0 == CloseHandle (newHandle))
    {
        error = GetLastError ();
        portLibrary->error_set_last_error (portLibrary, error, findError (error));    /* continue */
    }

    return result;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_length
/**
 * Answer the length in bytes of the file.
 *
 * @param[in] portLibrary The port library
 * @param[in] path file/path name being queried.
 *
 * @return Length in bytes of the file on success, negative portable error code on failure
 */
I_64 VMCALL
hyfile_length (struct HyPortLibrary * portLibrary, const char *path)
{
    I_64 result;
    I_32 error;
    WIN32_FILE_ATTRIBUTE_DATA myStat;
    wchar_t *pathW;
    int ret;
    
    convert_path_to_unicode(portLibrary, path, &pathW);
    ret = GetFileAttributesExW(pathW,GetFileExInfoStandard,&myStat);
    portLibrary->mem_free_memory(portLibrary, pathW);    

    if(ret == 0) {
        error = GetLastError ();
        return portLibrary->error_set_last_error (portLibrary, error, findError (error));
    }

    result = ((I_64) myStat.nFileSizeHigh) << 32;
    result += (I_64) myStat.nFileSizeLow;
    return result;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION is_device_name

/**
 * Determines if the given file name is a reserved device name
  
 * @param[in] path the path
 * @return length of device name if given file name is a device name or
 *   0 otherwise 
 */
int
is_device_name(const char *path)
{
    const char *reserved[] = {"con", "prn", "aux", "nul", "com", "lpt"};
    char *fname = strrchr(path, '\\');
    int i, len;

    fname = fname ? (fname + 1) : (char *) path;
    len = strlen(fname);
    
    for (i = 0; i < 6; i++) {
        if (i < 4 && len == 3 && !_stricmp(fname, reserved[i])) {
            return 3;
        } else if (i >= 4 && len == 4 && !_strnicmp(fname, reserved[i], 3) &&
                   isdigit(fname[3]) && fname[3] != '0') {
            return 4;
        }
    }
    return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION convert_path_to_unicode


// Maximum length of path accepted by unicode versions of
// WinAPI functions is limited to 32767 characters;
// each character can take up to three bytes in UTF-8.
#define ABS_PATH_BUF_LEN 32767*3
   
/**
 * Convert UTF-8 encoded path to UTF-16 so it can be used as an argument to
 * unicode versions of WinAPI fucntions. This function also converts all
 * relative paths to absolute ones.
 *
 * @param[in] portLibrary The port library
 * @param[in] path UTF-8 encoded null-terminated path
 * @param[out] pathW Pointer to wide characters array that contains converted
 *   path
 *
 * @note Sets *pathW to null and returns if path cannot be converted 
 */
void
convert_path_to_unicode(struct HyPortLibrary * portLibrary, const char *path,
	     wchar_t **pathW)
{
    int len;
    int wlen;
    wchar_t *wPath = 0;
    wchar_t tmpPath[ABS_PATH_BUF_LEN];

    if (!path) {
        *pathW = (void*) 0;
        return;
    }

    // check if given path is a name of device: nul, con, lpt1 and etc.
    if (len = is_device_name(path)) {
        wlen = MultiByteToWideChar(CP_UTF8, 0, path, -1, *pathW, 0);
        *pathW = portLibrary->mem_allocate_memory(portLibrary, wlen*sizeof(wchar_t));
        MultiByteToWideChar(CP_UTF8, 0, path + strlen(path) - len, -1, *pathW, wlen);
        return;
    }

    wlen = MultiByteToWideChar(CP_UTF8, 0, path, -1, wPath, 0);
    wPath = portLibrary->mem_allocate_memory(portLibrary, wlen*sizeof(wchar_t));
    MultiByteToWideChar(CP_UTF8, 0, path, -1, wPath, wlen);

    if (!GetFullPathNameW(wPath, ABS_PATH_BUF_LEN, tmpPath, (void*)0)) {
        *pathW = (void*)0;
    } else {
        wlen = sizeof(tmpPath) + 5;
        *pathW = portLibrary->mem_allocate_memory(portLibrary, wlen);
        wcscpy(*pathW, L"\\\\?\\");
        wcscat(*pathW, tmpPath);
    }

    portLibrary->mem_free_memory(portLibrary, wPath);
}

#undef ABS_PATH_BUF_LEN
#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_mkdir
/**
 * Create a directory.
 *
 * @param[in] portLibrary The port library
 * @param[in] path Directory to be created.
 *
 * @return 0 on success, -1 on failure.
 * @note Assumes all components of path up to the last directory already exist. 
 * @internal @todo return negative portable return code on failure.
 */
I_32 VMCALL
hyfile_mkdir (struct HyPortLibrary * portLibrary, const char *path)
{
    int returnVar=0;
    wchar_t *pathW;

    convert_path_to_unicode(portLibrary, path, &pathW);
    returnVar = CreateDirectoryW (pathW, 0);
    portLibrary->mem_free_memory(portLibrary, pathW);

    if (returnVar)
    {
        return 0;
    }
    else
    { 
        I_32 error = GetLastError ();
        portLibrary->error_set_last_error (portLibrary, error,
                                        findError (error));
        return -1;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_move
/**
 * Move the file pathExist to a new name pathNew.
 *
 * @param[in] portLibrary The port library
 * @param[in] pathExist The existing file name.
 * @param[in] pathNew The new file name.
 *
 * @return 0 on success, -1 on failure.
 * @internal @todo return negative portable return code on failure.
 */
I_32 VMCALL
hyfile_move (struct HyPortLibrary * portLibrary, const char *pathExist,
	     const char *pathNew)
{
  wchar_t *pathExistW, *pathNewW;
  int ret;
  
  convert_path_to_unicode(portLibrary, pathExist, &pathExistW);
  convert_path_to_unicode(portLibrary, pathNew, &pathNewW);
  ret = MoveFileW (pathExistW, pathNewW);
  portLibrary->mem_free_memory(portLibrary, pathNewW);
  portLibrary->mem_free_memory(portLibrary, pathExistW);
  
  if (ret)
    {
      return 0;
    }
  else
    {
      I_32 error = GetLastError ();
      portLibrary->error_set_last_error (portLibrary, error,
					 findError (error));
      return -1;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_open
/**
 * Convert a pathname into a file descriptor.
 *
 * @param[in] portLibrary The port library
 * @param[in] path Name of the file to be opened.
 * @param[in] flags Portable file read/write attributes.
 * @param[in] mode Platform file permissions.
 *
 * @return The file descriptor of the newly opened file, -1 on failure.
 */
IDATA VMCALL
hyfile_open (struct HyPortLibrary * portLibrary, const char *path, I_32 flags,
	     I_32 mode)
{
  DWORD accessMode, shareMode, createMode, flagsAndAttributes;
  HANDLE aHandle;
  I_32 error;
  wchar_t *pathW;

  Trc_PRT_file_open_Entry (path, flags, mode);

  accessMode = 0;
  if (flags & HyOpenRead)
    {
      accessMode |= GENERIC_READ;
    }
  if (flags & HyOpenWrite)
    {
      accessMode |= GENERIC_WRITE;
    }

  shareMode = FILE_SHARE_READ | FILE_SHARE_WRITE;

  if ((flags & HyOpenCreate) == HyOpenCreate)
    {
      createMode = OPEN_ALWAYS;
    }
  else if ((flags & HyOpenCreateNew) == HyOpenCreateNew)
    {
      createMode = CREATE_NEW;
    }
  else
    {
      createMode = OPEN_EXISTING;
    }

  flagsAndAttributes = FILE_ATTRIBUTE_NORMAL;
	if (flags & HyOpenSync) {
		flagsAndAttributes |= FILE_FLAG_WRITE_THROUGH;
	}

  convert_path_to_unicode(portLibrary, path, &pathW);
  aHandle =
    CreateFileW (pathW, accessMode, shareMode, NULL, createMode,
		flagsAndAttributes, NULL);

  if (aHandle == INVALID_HANDLE_VALUE)
    {
      error = GetLastError ();
      portLibrary->error_set_last_error (portLibrary, error,
					 findError (error));
      portLibrary->mem_free_memory(portLibrary, pathW); 
      Trc_PRT_file_open_Exit2 (error, findError (error));
      return -1;
    }

  if ((GetFileType(aHandle) == FILE_TYPE_DISK) && ((flags & HyOpenTruncate) == HyOpenTruncate))
    {
      if (0 == CloseHandle (aHandle))
	{
	  error = GetLastError ();
	  portLibrary->error_set_last_error (portLibrary, error, findError (error));	/* continue */
	}

      aHandle =
	CreateFileW (pathW, accessMode, shareMode, NULL, TRUNCATE_EXISTING,
		    flagsAndAttributes, NULL);
      if (aHandle == INVALID_HANDLE_VALUE)
      {
	  error = GetLastError ();
	  portLibrary->error_set_last_error (portLibrary, error,
					     findError (error));
      portLibrary->mem_free_memory(portLibrary, pathW); 
	  Trc_PRT_file_open_Exit3 (error, findError (error));
	  return -1;
	}
    }

  if (flags & HyOpenAppend)
    {
      portLibrary->file_seek (portLibrary, (IDATA) aHandle, 0, HySeekEnd);
    }

  portLibrary->mem_free_memory(portLibrary, pathW); 
  Trc_PRT_file_open_Exit (aHandle);
  return ((IDATA) aHandle);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_read
/**
 * Read bytes from a file descriptor into a user provided buffer.
 *
 * @param[in] portLibrary The port library
 * @param[in] fd The file descriptor.
 * @param[in,out] buf Buffer to read into.
 * @param[in] nbytes Size of buffer.
 *
 * @return The number of bytes read, or -1 on failure.
 */
IDATA VMCALL
hyfile_read (struct HyPortLibrary * portLibrary, IDATA fd, void *buf,
	     IDATA nbytes)
{
  DWORD bytesRead;

  if (nbytes == 0)
    {
      return 0;
    }

  if (!ReadFile ((HANDLE) fd, buf, nbytes, &bytesRead, NULL))
    {
      I_32 error = GetLastError ();
      portLibrary->error_set_last_error (portLibrary, error,
					 findError (error));
      return -1;
    }
  if (bytesRead == 0)
    {
      portLibrary->error_set_last_error (portLibrary, 0, 0);
      return -1;
    }

  return bytesRead;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_seek
/**
 * Repositions the offset of the file descriptor to a given offset as per directive whence.
 *
 * @param[in] portLibrary The port library
 * @param[in] fd The file descriptor.
 * @param[in] offset The offset in the file to position to.
 * @param[in] whence Portable constant describing how to apply the offset.
 *
 * @return The resulting offset on success, -1 on failure.
 * @note whence is one of HySeekSet (seek from beginning of file), 
 * HySeekCur (seek from current file pointer) or HySeekEnd (seek backwards from
 * end of file).
 * @internal @note seek operations return -1 on failure.  Negative offsets
 * can be returned when seeking beyond end of file.
 */
I_64 VMCALL
hyfile_seek (struct HyPortLibrary * portLibrary, IDATA fd, I_64 offset,
	     I_32 whence)
{
  DWORD moveMethod, moveResult;
  DWORD lowerOffset, upperOffset;
  I_64 result;
  I_32 error;

  lowerOffset = (DWORD) (offset & 0xFFFFFFFF);
  upperOffset = (DWORD) ((offset >> 32) & 0xFFFFFFFF);

  if ((whence < HySeekSet) || (whence > HySeekEnd))
    {
      return -1;
    }
  if (whence == HySeekSet)
    {
      moveMethod = FILE_BEGIN;
    }
  if (whence == HySeekEnd)
    {
      moveMethod = FILE_END;
    }
  if (whence == HySeekCur)
    {
      moveMethod = FILE_CURRENT;
    }
  moveResult =
    SetFilePointer ((HANDLE) fd, lowerOffset, &upperOffset, moveMethod);
  if (-1 == moveResult)
    {
      error = GetLastError ();
      if (error != NO_ERROR)
	{
	  portLibrary->error_set_last_error (portLibrary, error,
					     findError (error));
	  return -1;
	}
    }

  result = (I_64) upperOffset << 32;
  result |= moveResult;

  return result;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_shutdown
/**
 * PortLibrary shutdown.
 *
 * This function is called during shutdown of the portLibrary.  Any resources that were created by @ref hyfile_startup
 * should be destroyed here.
 *
 * @param[in] portLibrary The port library
 *
 * @note Most implementations will be empty.
 */
void VMCALL
hyfile_shutdown (struct HyPortLibrary *portLibrary)
{
  /* empty */
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_startup
/**
 * PortLibrary startup.
 *
 * This function is called during startup of the portLibrary.  Any resources that are required for
 * the file operations may be created here.  All resources created here should be destroyed
 * in @ref hyfile_shutdown.
 *
 * @param[in] portLibrary The port library
 *
 * @return 0 on success, negative error code on failure.  Error code values returned are
 * \arg HYPORT_ERROR_STARTUP_FILE
 *
 * @note Most implementations will simply return success.
 */
I_32 VMCALL
hyfile_startup (struct HyPortLibrary *portLibrary)
{
  return 0;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_sync
/**
 * Synchronize a file's state with the state on disk.
 *
 * @param[in] portLibrary The port library
 * @param[in] fd The file descriptor.
 *
 * @return 0 on success, -1 on failure.
 * @internal @todo return negative portable return code on failure.
 */
I_32 VMCALL
hyfile_sync (struct HyPortLibrary * portLibrary, IDATA fd)
{
  if (FlushFileBuffers ((HANDLE) fd))
    {
      return 0;
    }
  else
    {
      I_32 error = GetLastError ();
      portLibrary->error_set_last_error (portLibrary, error,
					 findError (error));
      return -1;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_unlink
/**
 * Remove a file from the file system.
 *
 * @param[in] portLibrary The port library
 * @param[in] path file/path name to remove.
 *
 * @return 0 on success, -1 on failure.
 * @internal @todo return negative portable return code on failure.
 */
I_32 VMCALL
hyfile_unlink (struct HyPortLibrary * portLibrary, const char *path)
{
  wchar_t *pathW;
  int ret;

  /* should be able to delete read-only dirs, so we set the file attribute back to normal */
  convert_path_to_unicode(portLibrary, path, &pathW);
  if (0 == SetFileAttributesW (pathW, FILE_ATTRIBUTE_NORMAL))
    {
      I_32 error = GetLastError ();
      portLibrary->error_set_last_error (portLibrary, error, findError (error));	/* continue */
    }

  ret = DeleteFileW (pathW);
  portLibrary->mem_free_memory(portLibrary, pathW);  
  
  if (ret)
    {
      return 0;
    }
  else
    {
      I_32 error = GetLastError ();
      portLibrary->error_set_last_error (portLibrary, error,
					 findError (error));
      return -1;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_unlinkdir
/**
 * Remove the trailing directory of the path. If the path is a symbolic link to a directory, remove
 * the symbolic link.
 *
 * @param[in] portLibrary The port library
 * @param[in] path directory name being removed.
 *
 * @return 0 on success, -1 on failure.
 * @internal @todo return negative portable return code on failure..
 */
I_32 VMCALL
hyfile_unlinkdir (struct HyPortLibrary * portLibrary, const char *path)
{
  wchar_t *pathW;
  convert_path_to_unicode(portLibrary, path, &pathW);

  /* should be able to delete read-only dirs, so we set the file attribute back to normal */
  if (0 == SetFileAttributesW (pathW, FILE_ATTRIBUTE_NORMAL))
    {
      I_32 error = GetLastError ();
      portLibrary->error_set_last_error (portLibrary, error, findError (error));	/* continue */
    }

  if (RemoveDirectoryW (pathW))
    {
      portLibrary->mem_free_memory(portLibrary, pathW);
      return 0;
    }
  else
    {
      I_32 error = GetLastError ();
      portLibrary->mem_free_memory(portLibrary, pathW);
      portLibrary->error_set_last_error (portLibrary, error,
					 findError (error));
      return -1;
    }
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_vprintf
/**
 * Write to a file.
 *
 * Writes formatted output  to the file referenced by the file descriptor.
 *
 * @param[in] portLibrary The port library
 * @param[in] fd File descriptor to write.
 * @param[in] format The format String.
 * @param[in] args Variable argument list.
 */
void VMCALL
hyfile_vprintf (struct HyPortLibrary *portLibrary, IDATA fd,
		const char *format, va_list args)
{
  char outputBuffer[256];

  char *allocatedBuffer;
  U_32 numberWritten;
  va_list copyOfArgs;

  /* Attempt to write output to stack buffer */
  COPY_VA_LIST (copyOfArgs, args);
  numberWritten =
    portLibrary->str_vprintf (portLibrary, outputBuffer,
			      sizeof (outputBuffer), format, copyOfArgs);

  /* str_vprintf always null terminates, returns number characters written excluding the null terminator */
  if (sizeof (outputBuffer) > (numberWritten + 1))
    {
      /* write out the buffer */
      portLibrary->file_write_text (portLibrary, fd, outputBuffer,
				    numberWritten);
      return;
    }

  /* Either the buffer was too small, or it was the exact size.  Unfortunately can't tell the difference,
   * need to determine the size of the buffer (another call to str_vprintf) then print to the buffer,
   * a third call to str_vprintf
   */
  COPY_VA_LIST (copyOfArgs, args);

  /* What is size of buffer required ? Does not include the \0 */
  numberWritten =
    portLibrary->str_vprintf (portLibrary, NULL, (U_32) (-1), format,
			      copyOfArgs);
  numberWritten += 1;

  allocatedBuffer =
    portLibrary->mem_allocate_memory (portLibrary, numberWritten);
  if (NULL == allocatedBuffer)
    {
      portLibrary->nls_printf (portLibrary, HYNLS_ERROR,
			       HYNLS_PORT_FILE_MEMORY_ALLOCATE_FAILURE);
      return;
    }

  numberWritten =
    portLibrary->str_vprintf (portLibrary, allocatedBuffer, numberWritten,
			      format, args);
  portLibrary->file_write_text (portLibrary, fd, allocatedBuffer,
				numberWritten);
  portLibrary->mem_free_memory (portLibrary, allocatedBuffer);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_write
/**
 * Write to a file.
 *
 * Writes up to nbytes from the provided buffer  to the file referenced by the file descriptor.
 *
 * @param[in] portLibrary The port library
 * @param[in] fd File descriptor to write.
 * @param[in] buf Buffer to be written.
 * @param[in] nbytes Size of buffer.
 *
 * @return Number of bytes written on success,  -1 on failure.
 * @internal @todo return negative portable return code on failure.
 */
IDATA VMCALL
hyfile_write (struct HyPortLibrary * portLibrary, IDATA fd, void *buf,
	      IDATA nbytes)
{
  DWORD nCharsWritten;
  IDATA toWrite, offset = 0;
  I_32 errorCode;
  HANDLE handle;

  if (fd == HYPORT_TTY_OUT)
    {
      handle = PPG_tty_consoleOutputHd;
    }
  else if (fd == HYPORT_TTY_ERR)
    {
      handle = PPG_tty_consoleErrorHd;
    }
  else
    {
      handle = (HANDLE) fd;
    }

  toWrite = nbytes;
  while (nbytes > 0)
    {
      if (toWrite > nbytes)
	{
	  toWrite = nbytes;
	}
      if (!WriteFile
	  (handle, (char *) buf + offset, toWrite, &nCharsWritten, NULL))
	{
	  errorCode = GetLastError ();
	  if (errorCode == ERROR_NOT_ENOUGH_MEMORY)
	    {
	      /* Use 48K chunks to get around out of memory problem */
	      if (toWrite > (48 * 1024))
		{
		  toWrite = 48 * 1024;
		}
	      else
		{
		  toWrite /= 2;
		}
	      /* If we can't write 128 bytes, just return */
	      if (toWrite >= 128)
		{
		  continue;
		}
	    }
	  return portLibrary->error_set_last_error (portLibrary, errorCode,
						    findError (errorCode));
	}
      offset += nCharsWritten;
      nbytes -= nCharsWritten;
    }

  return (IDATA) offset;
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_printf
/** 
 * Write to a file.
 *
 * Writes formatted output  to the file referenced by the file descriptor.
 *
 * @param[in] portLibrary The port library
 * @param[in] fd File descriptor to write to
 * @param[in] format The format string to be output.
 * @param[in] ... arguments for format.
 */
void VMCALL
hyfile_printf (struct HyPortLibrary *portLibrary, IDATA fd,
	       const char *format, ...)
{
  va_list args;

  va_start (args, format);
  portLibrary->file_vprintf (portLibrary, fd, format, args);
  va_end (args);
}

#undef CDEV_CURRENT_FUNCTION

#define CDEV_CURRENT_FUNCTION hyfile_set_length
/**
 * Set the length of a file to a specified value.
 *
 * @param[in] portLibrary The port library
 * @param[in] fd The file descriptor.
 * @param[in] newLength Length to be set
 *
 * @return 0 on success, negative portable error code on failure
 */
I_32 VMCALL
hyfile_set_length (struct HyPortLibrary *portLibrary, IDATA fd,
		   I_64 newLength)
{
  I_32 result, error;
  I_32 lowValue, highValue;

  lowValue = (I_32) (newLength & 0xFFFFFFFF);
  highValue = (I_32) ((newLength >> 32) & 0x7FFFFFFF);

  result = SetFilePointer ((HANDLE) fd, lowValue, &highValue, FILE_BEGIN);
  error = GetLastError ();
  if ((result == -1) && (error != NO_ERROR))
    {
      return portLibrary->error_set_last_error (portLibrary, error,
						findError (error));
    }
  else
    {
      if (0 == SetEndOfFile ((HANDLE) fd))
	{
	  error = GetLastError ();
	  return portLibrary->error_set_last_error (portLibrary, error,
						    findError (error));
	}
      /* Put pointer back to where it started */
      result = SetFilePointer ((HANDLE) fd, result, &highValue, FILE_BEGIN);
      error = GetLastError ();
      if ((result == -1) && (error != NO_ERROR))
	{
	  return portLibrary->error_set_last_error (portLibrary, error,
						    findError (error));
	}
    }
  return 0;
}

#undef CDEV_CURRENT_FUNCTION
