/* Copyright 2004 The Apache Software Foundation or its licensors, as applicable
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

/*
 * Win32 specific natives supporting the file system interface.
 */

#include <windows.h>
#include <harmony.h>

#include "IFileSystem.h"
#include "OSFileSystem.h"

/*
 * Class:     com_ibm_platform_OSFileSystem
 * Method:    mmapImpl
 * Signature: (JJJI)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_platform_OSFileSystem_mmapImpl
  (JNIEnv * env, jobject thiz, jlong fd, jlong offset, jlong size, jint mmode)
{

  DWORD flProtect = 0;
  DWORD dwDesiredAccess = 0;    /* Access control for mapping view. */
  HANDLE hFileMappingObject = 0;
  LPVOID mapAddress = 0;
  DWORD dwFileOffsetLow = (DWORD) (offset & 0xFFFFFFFF);
  DWORD dwFileOffsetHigh = (DWORD) ((offset >> 0x20) & 0x7FFFFFFF);

  // Convert from Java mapping mode to windows mapping mode.
  switch (mmode)
    {
      case com_ibm_platform_IFileSystem_MMAP_READ_ONLY:
        flProtect = PAGE_READONLY;
        dwDesiredAccess = FILE_MAP_READ;
        break;
      case com_ibm_platform_IFileSystem_MMAP_READ_WRITE:
        flProtect = PAGE_READWRITE;
        dwDesiredAccess = FILE_MAP_WRITE;
        break;
      case com_ibm_platform_IFileSystem_MMAP_WRITE_COPY:
        flProtect = PAGE_WRITECOPY;
        dwDesiredAccess = FILE_MAP_COPY;
        break;
      default:
        return -1;
    }

  /* First create a file mapping handle. */
  hFileMappingObject = CreateFileMapping ((HANDLE) fd,  /* [in] file handle */
                                          (LPSECURITY_ATTRIBUTES) NULL, /* [in] mapping is not inherited */
                                          flProtect,    /* [in] protection mode for mapped data */
                                          (DWORD) 0,    /* [in] maximum size high */
                                          (DWORD) 0,    /* [in] maximum size low */
                                          (LPCTSTR) NULL);      /* [in] name for mapping object */

  if (!hFileMappingObject || hFileMappingObject == INVALID_HANDLE_VALUE)
    {
      return -1;
    }

  /* Secondly create a view on that mapping object. */

  mapAddress = MapViewOfFile (hFileMappingObject,       /* [in] open file mapping object */
                              dwDesiredAccess,  /* [in] access to the file view */
                              dwFileOffsetHigh, /* [in] high offset where mapping is to begin */
                              dwFileOffsetLow,  /* [in] low offset where mapping is to begin */
                              (SIZE_T) size);   /* [in] number of bytes to map */

  if (mapAddress == NULL)
    {
      return -1;
    }

  return (jlong) mapAddress;
}

/**
 * Lock the file identified by the given handle.
 * The range and lock type are given.
 */
JNIEXPORT jint JNICALL Java_com_ibm_platform_OSFileSystem_lockImpl
  (JNIEnv * env, jobject thiz, jlong handle, jlong start, jlong length,
   jint typeFlag, jboolean waitFlag)
{
  DWORD dwFlags = 0;
  OVERLAPPED overlapped;
  BOOL rc;
  const DWORD offsetLow = (DWORD) (start & 0xFFFFFFFF);
  const DWORD offsetHigh = (DWORD) ((start >> 0x20) & 0x7FFFFFFF);
  const DWORD nNumberOfBytesToLockLow = (DWORD) (length & 0xFFFFFFFF);
  const DWORD nNumberOfBytesToLockHigh =
    (DWORD) ((length >> 0x20) & 0x7FFFFFFF);

  if (waitFlag == JNI_FALSE)
    {
      dwFlags |= LOCKFILE_FAIL_IMMEDIATELY;
    }

  if (typeFlag & com_ibm_platform_IFileSystem_EXCLUSIVE_LOCK_TYPE)
    {
      dwFlags |= LOCKFILE_EXCLUSIVE_LOCK;
    }

  memset (&overlapped, 0, sizeof (overlapped));
  overlapped.Offset = offsetLow;
  overlapped.OffsetHigh = offsetHigh;

  rc = LockFileEx ((HANDLE) handle,     /* [in] file handle to lock */
                   dwFlags,     /* [in] flags describing lock type */
                   (DWORD) 0,   /* [in] reserved */
                   nNumberOfBytesToLockLow,     /* [in] number of bytes to lock (low) */
                   nNumberOfBytesToLockHigh,    /* [in] number of bytes to lock (high) */
                   (LPOVERLAPPED) & overlapped);        /* [in] contains file offset to lock start */

  if (rc != 0)
    {
      // Success
      return (jint) 0;
    }
  return (jint) - 1;
}

/**
 * Unlocks the specified region of the file.
 */
JNIEXPORT jint JNICALL Java_com_ibm_platform_OSFileSystem_unlockImpl
  (JNIEnv * env, jobject thiz, jlong handle, jlong start, jlong length)
{
  OVERLAPPED overlapped;
  BOOL rc;
  const DWORD offsetLow = (DWORD) (start & 0xFFFFFFFF);
  const DWORD offsetHigh = (DWORD) ((start >> 0x20) & 0x7FFFFFFF);
  const DWORD nNumberOfBytesToUnlockLow = (DWORD) (length & 0xFFFFFFFF);
  const DWORD nNumberOfBytesToUnlockHigh =
    (DWORD) ((length >> 0x20) & 0x7FFFFFFF);

  memset (&overlapped, 0, sizeof (overlapped));
  overlapped.Offset = offsetLow;
  overlapped.OffsetHigh = offsetHigh;

  rc = UnlockFileEx ((HANDLE) handle,   /* [in] file handle to lock */
                     (DWORD) 0, /* [in] reserved */
                     nNumberOfBytesToUnlockLow, /* [in] number of bytes to lock (low) */
                     nNumberOfBytesToUnlockHigh,        /* [in] number of bytes to lock (high) */
                     (LPOVERLAPPED) & overlapped);      /* [in] contains file offset to lock start */

  if (rc != 0)
    {
      // Success
      return (jint) 0;
    }
  return (jint) - 1;
}

