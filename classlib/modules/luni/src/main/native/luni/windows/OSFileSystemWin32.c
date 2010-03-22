/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

#include "vmi.h"
#include "hysock.h"
#include <mswsock.h>

#include <windows.h>
#include "nethelp.h"
#include <stdio.h>
#include "IFileSystem.h"
#include "OSFileSystem.h"

/**
 * Lock the file identified by the given handle.
 * The range and lock type are given.
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_luni_platform_OSFileSystem_lockImpl
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

  if (typeFlag & org_apache_harmony_luni_platform_IFileSystem_EXCLUSIVE_LOCK_TYPE)
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
JNIEXPORT jint JNICALL Java_org_apache_harmony_luni_platform_OSFileSystem_unlockImpl
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

/**
 * Returns the granularity of the starting address for virtual memory allocation.
 * (Note, that it differs from page size.)
 * Class:     org_apache_harmony_luni_platform_OSFileSystem
 * Method:    getAllocGranularity
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_luni_platform_OSFileSystem_getAllocGranularity
  (JNIEnv * env, jobject thiz)
{
  static DWORD allocGranularity = 0;
  if(!allocGranularity){
      SYSTEM_INFO info;
      GetSystemInfo(&info);
      allocGranularity = info.dwAllocationGranularity;
  }
  return allocGranularity;
}

/*
 * Class:     org_apache_harmony_luni_platform_OSFileSystem
 * Method:    readvImpl
 * Signature: (J[J[I[I)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_luni_platform_OSFileSystem_readvImpl
  (JNIEnv *env, jobject thiz, jlong fd, jlongArray jbuffers, jintArray joffsets, jintArray jlengths, jint size){
  PORT_ACCESS_FROM_ENV (env);
  jboolean bufsCopied = JNI_FALSE;
  jboolean offsetsCopied = JNI_FALSE;
  jboolean lengthsCopied = JNI_FALSE;
  jlong *bufs = (*env)->GetLongArrayElements(env, jbuffers, &bufsCopied);
  jint *offsets = (*env)->GetIntArrayElements(env, joffsets, &offsetsCopied);
  jint *lengths = (*env)->GetIntArrayElements(env, jlengths, &lengthsCopied);
  long totalRead = 0;  
  int i = 0;
  while(i<size){
    long bytesRead = hyfile_read ((IDATA) fd, (void *) (*(bufs+i)+*(offsets+i)), (IDATA) *(lengths+i));
    if(bytesRead == -1 && hyerror_last_error_number() == HYPORT_ERROR_FILE_LOCKED){
        throwNewExceptionByName(env, "java/io/IOException", netLookupErrorString(env, HYPORT_ERROR_FILE_LOCKED));
	break;
    }
    if(bytesRead == -1){
        if (totalRead == 0){
                totalRead = -1;
        }
        break;
    }
    totalRead += bytesRead;
    i++;
  }
  if(bufsCopied){
    (*env)->ReleaseLongArrayElements(env, jbuffers, bufs, JNI_ABORT);
  }
  if(offsetsCopied){
    (*env)->ReleaseIntArrayElements(env, joffsets, offsets, JNI_ABORT);
  }
  if(lengthsCopied){
    (*env)->ReleaseIntArrayElements(env, jlengths, lengths, JNI_ABORT);
  }
  return totalRead;
}

/*
 * Class:     org_apache_harmony_luni_platform_OSFileSystem
 * Method:    writevImpl
 * Signature: (J[J[I[I)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_luni_platform_OSFileSystem_writevImpl
  (JNIEnv *env, jobject thiz, jlong fd, jlongArray jbuffers, jintArray joffsets, jintArray jlengths, jint size){
  PORT_ACCESS_FROM_ENV (env);
  jboolean bufsCopied = JNI_FALSE;
  jboolean offsetsCopied = JNI_FALSE;
  jboolean lengthsCopied = JNI_FALSE;
  jlong *bufs = (*env)->GetLongArrayElements(env, jbuffers, &bufsCopied);
  jint *offsets = (*env)->GetIntArrayElements(env, joffsets, &offsetsCopied);
  jint *lengths = (*env)->GetIntArrayElements(env, jlengths, &lengthsCopied);
  long totalWritten = 0;  
  int i = 0;
  while(i<size){
    long bytesWritten = hyfile_write ((IDATA) fd, (void *) (*(bufs+i)+*(offsets+i)), (IDATA) *(lengths+i));
    if(bytesWritten == -1 && hyerror_last_error_number() == HYPORT_ERROR_FILE_LOCKED){
        throwNewExceptionByName(env, "java/io/IOException", netLookupErrorString(env, HYPORT_ERROR_FILE_LOCKED));
	break;
    }
    if(bytesWritten == -1){
        totalWritten = -1;
        break;
    }
    totalWritten += bytesWritten;
    i++;
  }
  if(bufsCopied){
    (*env)->ReleaseLongArrayElements(env, jbuffers, bufs, JNI_ABORT);
  }
  if(offsetsCopied){
    (*env)->ReleaseIntArrayElements(env, joffsets, offsets, JNI_ABORT);
  }
  if(lengthsCopied){
    (*env)->ReleaseIntArrayElements(env, jlengths, lengths, JNI_ABORT);
  }
  return totalWritten;
}

/*
 * Class:     org_apache_harmony_luni_platform_OSFileSystem
 * Method:    transferImpl
 * Signature: (JLjava/io/FileDescriptor;JJ)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_luni_platform_OSFileSystem_transferImpl
  (JNIEnv *env, jobject thiz, jlong fd, jobject sd, jlong offset, jlong count)
{
	PORT_ACCESS_FROM_ENV (env);
	HANDLE hfile = (HANDLE)fd;    	
    SOCKET socket;
    hysocket_t hysocketP;
	DWORD pos_high = 0;
    DWORD pos_low = 0;        
	   
    if(0 !=(count>>31)) {
        count = 0x7FFFFFFF;
    }

	hysocketP = getJavaIoFileDescriptorContentsAsAPointer(env,sd);	
	socket = (SOCKET)hysocketP->ipv4;	
    
	pos_low  = SetFilePointer(hfile,0,&pos_high,FILE_CURRENT);
	if(INVALID_SET_FILE_POINTER == pos_low){
        return -1;
	}
			
	if(INVALID_SET_FILE_POINTER == SetFilePointer(hfile,(DWORD)offset,(DWORD *)(((BYTE *)&offset)+4),FILE_BEGIN)){
        return -1;
	}
    
	if(!TransmitFile(socket,hfile,(DWORD)count,0,NULL,NULL,0)){
        return -1;
	}		

	if(INVALID_SET_FILE_POINTER == SetFilePointer(hfile,pos_low,&pos_high,FILE_BEGIN)){
        return -1;
	}
    return count;	
}

