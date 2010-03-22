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

/* Undefine the winsockapi because winsock2 defines it.  Removes warnings. */
#if defined(_WINSOCKAPI_) && !defined(_WINSOCK2API_)
#undef _WINSOCKAPI_
#endif

#include <winsock2.h>

#include <windows.h>
#include <winbase.h>
#include <stdlib.h>
#include <LMCONS.H>
#include <direct.h>

#include "vmi.h"
#include "harmonyglob.h"
#include "charsetmap.h"

#include "helpers.h"
#include "nethelp.h"
#include "hysock.h"
#include <iphlpapi.h>

int platformReadLink (char *link);
void setDefaultServerSocketOptions (JNIEnv * env, hysocket_t socketP);
jint getPlatformDatagramNominalSize (JNIEnv * env, hysocket_t socketP);
I_32 getPlatformRoots (char *rootStrings);
jstring getCustomTimeZoneInfo (JNIEnv * env, jintArray tzinfo,
             jbooleanArray isCustomTimeZone);
I_32 getPlatformIsHidden (JNIEnv * env, char *path);
jint getPlatformDatagramMaxSize (JNIEnv * env, hysocket_t socketP);
char *getCommports (JNIEnv * env);
I_32 getPlatformIsWriteOnly (JNIEnv * env, char *path);
I_32 setPlatformFileLength (JNIEnv * env, IDATA descriptor, jlong newLength);
void platformCanonicalPath (char *pathCopy);
I_32 getPlatformIsReadOnly (JNIEnv * env, char *path);
void setPlatformBindOptions (JNIEnv * env, hysocket_t socketP);
I_32 setPlatformLastModified (JNIEnv * env, char *path, I_64 time);
I_32 setPlatformReadOnly (JNIEnv * env, char *path);
jlong getPlatformTotal (JNIEnv * env, char *path);
jlong getPlatformUsableTotal (JNIEnv * env, char *path);
jlong getPlatformFreeTotal (JNIEnv * env, char *path);
UDATA platformFindfirst (char *path, char *resultbuf);
int portCmp (const void **a, const void **b);
static void unmangle (JNIEnv * env, LPWSTR data);
I_32 getPlatformAttribute (JNIEnv * env, char *path, DWORD attribute);
typedef enum {
	OPERSTAT,
	INTERFACETYPE,
	FLAGS,
	MTU
}FLAGTYPE;
jboolean getPlatformNetworkInterfaceAttribute(JNIEnv * env, FLAGTYPE type , jint jindex, DWORD flag);
static LPWSTR mangle (JNIEnv * env, char *path);

/* This function converts a char array into a unicode wide string */
static LPWSTR
mangle (JNIEnv * env, char *path)
{
  PORT_ACCESS_FROM_ENV (env);
  int convSize;
  LPWSTR unicodeBuffer;

  convSize = MultiByteToWideChar (CP_ACP, MB_PRECOMPOSED, path, -1, NULL, 0);
  convSize = (convSize + 1) * 2;
  unicodeBuffer = jclmem_allocate_memory (env, convSize);
  if (!unicodeBuffer)
    return NULL;
  MultiByteToWideChar (CP_ACP, MB_PRECOMPOSED, path, -1, unicodeBuffer,
           convSize);
  return unicodeBuffer;
}

/* This function frees the memory allocated in mangle */
static void
unmangle (JNIEnv * env, LPWSTR data)
{
  PORT_ACCESS_FROM_ENV (env);
  jclmem_free_memory (env, data);
}

/**
 * It is the responsibility of #getPlatformRoots to return a char array
 * with volume names separated by null with a trailing extra null, so for
 * Unix it should be '\<null><null>' .
 */
I_32
getPlatformRoots (char *rootStrings)
{

  I_32 result = GetLogicalDriveStrings (HyMaxPath, rootStrings);
  return result / 4;    /* Letter, colon, slash, null = 4 bytes */

}

/**
 * Answer 1 if the path is hidden, 0 otherwise even in fail cases.
 */
I_32
getPlatformIsHidden (JNIEnv * env, char *path)
{
  return getPlatformAttribute (env, path, FILE_ATTRIBUTE_HIDDEN);
}

/**
 * Answer 1 if the file time was updated, 0 otherwise even in fail cases.
 */
I_32
setPlatformLastModified (JNIEnv * env, char *path, I_64 time)
{
  PORT_ACCESS_FROM_ENV (env);
  FILETIME fileTime;
  I_32 result, dwFlagsAndAttributes = FILE_ATTRIBUTE_NORMAL;
  IDATA hFile;
  I_64 tempLongLong;

  char *unicodePath;
  unicodePath = path;

  /**
   * Open the path ensuring GENERIC_WRITE and FILE_FLAG_BACKUP_SEMANTICS if it's a directory.
   * The directory modification is only supported on some platforms (NT, Windows2000).
   */
  result = GetFileAttributes ((LPCTSTR) unicodePath);
  if (result == 0xFFFFFFFF)
    {
      return 0;
    }
  if (result & FILE_ATTRIBUTE_DIRECTORY)
    dwFlagsAndAttributes = FILE_FLAG_BACKUP_SEMANTICS;

  hFile = (IDATA) CreateFile (unicodePath,
            GENERIC_WRITE,
            FILE_SHARE_READ | FILE_SHARE_WRITE,
            NULL,
            OPEN_EXISTING, dwFlagsAndAttributes, NULL);
  if ((IDATA) hFile == (IDATA) INVALID_HANDLE_VALUE)
    {
      return 0;
    }

  tempLongLong = (time * (I_64) 10000) + 116444736000000000;

  fileTime.dwHighDateTime = (I_32) (tempLongLong >> 32);
  fileTime.dwLowDateTime = (I_32) tempLongLong;
  result =
    SetFileTime ((HANDLE) hFile, (LPFILETIME) NULL, (LPFILETIME) NULL,
     &fileTime);
  hyfile_close (hFile);

  return result;
}

/**
 * Answer 1 if the path is now readOnly, 0 otherwise even in fail cases.
 */
I_32
setPlatformReadOnly (JNIEnv * env, char *path)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 attrs, result;

  char *unicodePath;
  unicodePath = path;

  attrs = GetFileAttributes (unicodePath);
  if (attrs == 0xFFFFFFFF)
    {
      return 0;
    }
  attrs = attrs | FILE_ATTRIBUTE_READONLY;
  result = SetFileAttributes (unicodePath, attrs);

  return result;
}

/**
 * Answer 1 if the writable attribute was set, 0 otherwise even in fail cases.
 */
I_32
setPlatformWritable (JNIEnv * env, char *path, jboolean writable, jboolean ownerOnly)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 attrs, result;

  char *unicodePath;
  unicodePath = path;

  attrs = GetFileAttributes (unicodePath);
  if(writable)
      attrs &= (~FILE_ATTRIBUTE_READONLY);
  else
      attrs |= FILE_ATTRIBUTE_READONLY;

  result = SetFileAttributes (unicodePath, attrs);

  return result;
}

/**
 * Answer 1 if the file length was set, 0 otherwise even in fail cases.
 */
I_32
setPlatformFileLength (JNIEnv * env, IDATA descriptor, jlong newLength)
{
  I_32 result;
  I_32 lowValue, highValue;
  lowValue = (I_32) newLength;
  highValue = (I_32) (newLength >> 32);
  result =
    SetFilePointer ((HANDLE) descriptor, lowValue, &highValue, FILE_BEGIN);
  if (result == INVALID_FILE_SIZE && (GetLastError ()) != NO_ERROR)
    return 0;
  return SetEndOfFile ((HANDLE) descriptor);
}

/**
 * Answers the total bytes of the partition designated by path.
 */
typedef BOOL (WINAPI *PGETDISKFREESPACEEX)(LPCSTR,
   PULARGE_INTEGER, PULARGE_INTEGER, PULARGE_INTEGER);
jlong getPlatformTotal (JNIEnv * env, char *path){
	PGETDISKFREESPACEEX pGetDiskFreeSpaceEx;
   __int64 i64FreeBytesToCaller, i64TotalBytes, i64FreeBytes;
   
   DWORD dwSectPerClust, 
         dwBytesPerSect, 
         dwFreeClusters, 
         dwTotalClusters;

   BOOL fResult;

   LPCSTR drive;

   pGetDiskFreeSpaceEx = (PGETDISKFREESPACEEX) GetProcAddress( 
                           GetModuleHandle("kernel32.dll"),
                          "GetDiskFreeSpaceExA");
   drive = strtok(path, "\\");

   if (pGetDiskFreeSpaceEx)
   {
      fResult = pGetDiskFreeSpaceEx (drive,
                 (PULARGE_INTEGER)&i64FreeBytesToCaller,
                 (PULARGE_INTEGER)&i64TotalBytes,
                 (PULARGE_INTEGER)&i64FreeBytes);

      // Process GetDiskFreeSpaceEx results.
      if(fResult) 
      {
         return i64TotalBytes;
      }
   }

   else 
   {
      fResult = GetDiskFreeSpaceA (drive, 
                 &dwSectPerClust, 
                 &dwBytesPerSect,
                 &dwFreeClusters, 
                 &dwTotalClusters);

   // Process GetDiskFreeSpace results.
      if(fResult) 
      {
         return
             dwTotalClusters*dwSectPerClust*dwBytesPerSect;
      }
   }
	return (__int64)0;
}

jlong getPlatformUsableTotal (JNIEnv * env, char *path) {
    PGETDISKFREESPACEEX pGetDiskFreeSpaceEx;
   __int64 i64FreeBytesToCaller, i64TotalBytes, i64FreeBytes;
   
   DWORD dwSectPerClust, 
         dwBytesPerSect, 
         dwFreeClusters, 
         dwTotalClusters;

   BOOL fResult;

   LPCSTR drive;

   pGetDiskFreeSpaceEx = (PGETDISKFREESPACEEX) GetProcAddress( 
                           GetModuleHandle("kernel32.dll"),
                          "GetDiskFreeSpaceExA");
   drive = strtok(path, "\\");

   if (pGetDiskFreeSpaceEx)
   {
      fResult = pGetDiskFreeSpaceEx (drive,
                 (PULARGE_INTEGER)&i64FreeBytesToCaller,
                 (PULARGE_INTEGER)&i64TotalBytes,
                 (PULARGE_INTEGER)&i64FreeBytes);

      // Process GetDiskFreeSpaceEx results.
      if(fResult) 
      {
         return i64FreeBytesToCaller;
      }
   }

   else 
   {
      fResult = GetDiskFreeSpaceA (drive, 
                 &dwSectPerClust, 
                 &dwBytesPerSect,
                 &dwFreeClusters, 
                 &dwTotalClusters);

   // Process GetDiskFreeSpace results.
      if(fResult) 
      {
         return
             dwFreeClusters*dwSectPerClust*dwBytesPerSect;
      }
   }
	return (__int64)0;
}

jlong getPlatformFreeTotal (JNIEnv * env, char *path) {
	PGETDISKFREESPACEEX pGetDiskFreeSpaceEx;
   __int64 i64FreeBytesToCaller, i64TotalBytes, i64FreeBytes;
   
   DWORD dwSectPerClust, 
         dwBytesPerSect, 
         dwFreeClusters, 
         dwTotalClusters;

   BOOL fResult;

   LPCSTR drive;

   pGetDiskFreeSpaceEx = (PGETDISKFREESPACEEX) GetProcAddress( 
                           GetModuleHandle("kernel32.dll"),
                          "GetDiskFreeSpaceExA");
   drive = strtok(path, "\\");

   if (pGetDiskFreeSpaceEx)
   {
      fResult = pGetDiskFreeSpaceEx (drive,
                 (PULARGE_INTEGER)&i64FreeBytesToCaller,
                 (PULARGE_INTEGER)&i64TotalBytes,
                 (PULARGE_INTEGER)&i64FreeBytes);

      // Process GetDiskFreeSpaceEx results.
      if(fResult) 
      {
         return i64FreeBytes;
      }
   }

   else 
   {
      fResult = GetDiskFreeSpaceA (drive, 
                 &dwSectPerClust, 
                 &dwBytesPerSect,
                 &dwFreeClusters, 
                 &dwTotalClusters);

   // Process GetDiskFreeSpace results.
      if(fResult) 
      {
         return
             dwFreeClusters*dwSectPerClust*dwBytesPerSect;
      }
   }
	return (__int64)0;
}
jbyteArray
getPlatformPath (JNIEnv * env, jbyteArray path)
{
  char buffer[256];
  jbyteArray answer = NULL;
  jsize length = (*env)->GetArrayLength (env, path);
  jbyte *lpath = (*env)->GetByteArrayElements (env, path, 0);

  if (lpath != NULL)
    {
      if (length >= 2 && lpath[1] == ':')
        {
          char next = lpath[2];
          int drive = tolower (lpath[0]) - 'a' + 1;
          if ((next == 0 || (lpath[2] != '/' && next != '\\')) && drive >= 1
            && drive <= 26)
            {
              int buflen = 2, needSlash;
              if (_getdcwd (drive, buffer, sizeof (buffer)) != NULL)
                {
                  buflen = strlen (buffer);
                  if (buffer[buflen - 1] == '\\')
                    buflen--;
                }
              needSlash = length > 2 || buflen < 3;
              answer =
                (*env)->NewByteArray (env,
                buflen + length - (needSlash ? 1 : 2));
              if (answer != NULL)
                {
                  /* Copy drive and colon */
                  (*env)->SetByteArrayRegion (env, answer, 0, 2, lpath);
                  if (buflen > 2)
                    (*env)->SetByteArrayRegion (env, answer, 2, buflen - 2,
                    buffer + 2);
                  if (needSlash)
                    (*env)->SetByteArrayRegion (env, answer, buflen, 1, "\\");
                  if (length > 2)
                    (*env)->SetByteArrayRegion (env, answer, buflen + 1,
                    length - 2, lpath + 2);
                }
            }
        }
      (*env)->ReleaseByteArrayElements (env, path, lpath, JNI_ABORT);
    }
  return answer;
}

UDATA
platformFindfirst (char *path, char *resultbuf)
{
  /*
   * Takes a path and a preallocated resultbuf.  Answers a handle to be used
   * in subsequent calls to hyfile_findnext and hyfile_findclose.  Handle may
   * be -1 if hyfile_findfirst fails.
   * The parameter @path will only be a valid directory name, any info that must
   * be added to it and passed to the os (c:\\) should be (c:\\*) for Win32
   */
  WIN32_FIND_DATA lpFindFileData;
  HANDLE handle;

  handle = FindFirstFile ((LPCTSTR) path, &lpFindFileData);

  if (handle == INVALID_HANDLE_VALUE)
    return (UDATA) - 1;

  lstrcpy (resultbuf, lpFindFileData.cFileName);
  FindClose (handle);
  return 1;
}

void
platformCanonicalPath (char *pathCopy)
{
  UDATA result;
  U_32 pos, start, length, rpos, rlen;
  char newpath[HyMaxPath], filename[HyMaxPath];

  pos = 0;
  length = strlen (pathCopy);
  if (length >= 2 && pathCopy[1] == ':')
    {
      pathCopy[0] = toupper (pathCopy[0]);
      pos = 2;
    }
  else if (pathCopy[0] == '\\')
    {
      pos++;
      if (length >= 2 && pathCopy[1] == '\\')
        {
          pos++;
          /* Found network path, skip server and volume */
          while (pos < length && pathCopy[pos] != '\\')
            pos++;
          if (pathCopy[pos] == '\\')
            pos++;
          while (pos < length && pathCopy[pos] != '\\')
            pos++;
          if (pos == length)
            return;
        }
    }
  if (pathCopy[pos] == '\\')
    pos++;
  start = pos;
  memcpy (newpath, pathCopy, pos);
  rpos = pos;
  while (pos <= length)
    {
      if (pathCopy[pos] == '\\' || pos == length)
        {
          if (pos == length && pathCopy[pos - 1] == '\\')
            break;
          pathCopy[pos] = 0;
          result = platformFindfirst (pathCopy, filename);
          if (pos != length)
            pathCopy[pos] = '\\';
          if (result == (UDATA) - 1)
            {
              break;
            }
          else
            {
              rlen = strlen (filename);
              if (rpos + rlen + 2 >= HyMaxPath)
                break;
              strcpy (&newpath[rpos], filename);
              rpos += rlen;
              if (pos != length)
                newpath[rpos++] = '\\';
              else
                newpath[rpos] = 0;
              start = pos + 1;
            }
        }
      else if (pathCopy[pos] == '*' || pathCopy[pos] == '?')
        break;
      pos++;
    }
  if (start <= length)
    strncpy (&newpath[rpos], &pathCopy[start], HyMaxPath - 1 - rpos);
  strcpy (pathCopy, newpath);

}

void
setPlatformBindOptions (JNIEnv * env, hysocket_t socketP)
{
}

/**
 * Answer 1 if the path is hidded, 0 otherwise even in fail cases.
 */
I_32
getPlatformAttribute (JNIEnv * env, char *path, DWORD attribute)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 attrs;

  char *unicodePath;
  unicodePath = path;

  attrs = GetFileAttributes (unicodePath);

  if (attrs == 0xFFFFFFFF)
    return 0;
  return (attrs & attribute) == attribute;
}

/**
 * Answer 1 if the path is read-only, 0 otherwise even in fail cases.
 */
I_32
getPlatformIsReadOnly (JNIEnv * env, char *path)
{
  return getPlatformAttribute (env, path, FILE_ATTRIBUTE_READONLY);
}

void
convert_path_to_unicode(JNIEnv * env,const char *path,
	     wchar_t **pathW)
{
PORT_ACCESS_FROM_ENV (env);
    int len = strlen(path);
    int wlen;
    char *canonicalpath;
    int srcArrayCount=0;
    int destArrayCount=0;
    int slashCount=0; //record how many slashes it met.
    int dotsCount=0; //record how many dots following a separator.
    int *slashStack; //record position of every separator.
    slashStack = jclmem_allocate_memory (env, len*sizeof(int));
    canonicalpath = jclmem_allocate_memory (env, len+5);

    strcpy(canonicalpath,"\\\\?\\");

    for(srcArrayCount=0,destArrayCount=4;srcArrayCount<len;srcArrayCount++){
        // the input path of this method has been parsed to absolute path already.
        if(path[srcArrayCount]=='.'){
            // count the dots following last separator.
            if(dotsCount>0 || path[srcArrayCount-1]=='\\'){
                dotsCount++;
                continue;
            }
        }
        // deal with the dots when we meet next separator.
        if(path[srcArrayCount]=='\\'){
            if(dotsCount == 1){
        	dotsCount = 0;
        	continue;
            }else if (dotsCount > 1){
                if(slashCount-2<0){
                    slashCount=2;
                }
                destArrayCount=slashStack[slashCount-2];
                dotsCount = 0;
                slashCount--;
            }else{
                while(canonicalpath[destArrayCount-1] == '.'){
                    destArrayCount--;
                }
                slashStack[slashCount++]=destArrayCount;
            }
        }
        // for normal character.
        while(dotsCount >0){
            canonicalpath[destArrayCount++]='.';
            dotsCount--;
        }
        canonicalpath[destArrayCount++]=path[srcArrayCount];
    }
    while(canonicalpath[destArrayCount-1] == '.'){
        destArrayCount--;
    }        
    canonicalpath[destArrayCount]='\0';
    wlen = MultiByteToWideChar(CP_UTF8, 0, canonicalpath, -1, *pathW, 0);
    *pathW = jclmem_allocate_memory (env, wlen*sizeof(wchar_t));
    MultiByteToWideChar(CP_UTF8, 0, canonicalpath, -1, *pathW, wlen);
    jclmem_free_memory (env, canonicalpath);
    jclmem_free_memory (env, slashStack);
}

/**
 * Answer 1 if the path is write-only, 0 otherwise even in fail cases.
 */
I_32
getPlatformIsWriteOnly (JNIEnv * env, char *path)
{
  HANDLE fHandle;
  wchar_t *pathW;
  convert_path_to_unicode(env,path,&pathW);

  fHandle = CreateFileW(pathW, GENERIC_READ, FILE_SHARE_READ, NULL, OPEN_EXISTING, FILE_FLAG_BACKUP_SEMANTICS, NULL);
  if (fHandle == INVALID_HANDLE_VALUE) {
    return 1;
  }

  CloseHandle(fHandle);
  return 0;
}


/* Resolve link if it is a symbolic link and put the result in link. */
int
platformReadLink (char *link)
{
  return FALSE;
}

jstring
getCustomTimeZoneInfo (JNIEnv * env, jintArray tzinfo,
           jbooleanArray isCustomTimeZone)
{
  return NULL;
}

void
setDefaultServerSocketOptions (JNIEnv * env, hysocket_t socketP)
{
}

jboolean isIPv6Enabled (PIP_ADAPTER_ADDRESSES adapterAddrs)
{
	jboolean isIPv6 = JNI_FALSE;
	PIP_ADAPTER_ADDRESSES adapterPtr = adapterAddrs;

	while (adapterAddrs != NULL) {
		if(adapterAddrs->Ipv6IfIndex != 0) {
			isIPv6 = JNI_TRUE;
			break;
		}
		adapterAddrs = adapterAddrs->Next;
	}
	adapterAddrs = adapterPtr; 
	return isIPv6;
}

PIP_ADAPTER_ADDRESSES initAdapters(JNIEnv * env, ULONG RetVal)
{
	PIP_ADAPTER_ADDRESSES AdapterAddresses = NULL;
	ULONG OutBufferLength = 0;
	int i;

    PORT_ACCESS_FROM_ENV (env);

	// One call to get the size and one call to get the actual parameters.
	for (i = 0; i < 2; i++) {
		RetVal =  GetAdaptersAddresses(AF_UNSPEC, GAA_FLAG_INCLUDE_PREFIX, NULL, AdapterAddresses, &OutBufferLength);

		if (RetVal != ERROR_BUFFER_OVERFLOW) {
			break;
		}

		if (AdapterAddresses != NULL) {
			hymem_free_memory (AdapterAddresses);
		}

		AdapterAddresses = hymem_allocate_memory (OutBufferLength);
		if (AdapterAddresses == NULL) {
			RetVal = GetLastError();
			break;
		}
	}
	return AdapterAddresses;
}

jboolean
getPlatformNetworkInterfaceAttribute(JNIEnv * env, FLAGTYPE type , jint jindex, DWORD flag)
{
	PIP_ADAPTER_ADDRESSES AdapterAddresses = NULL;
	ULONG OutBufferLength = 0;
	ULONG RetVal = 0;
	jboolean isIPv6 = JNI_FALSE;
	jboolean isSet = JNI_FALSE;

	/* required call if we are going to call port library methods */
	PORT_ACCESS_FROM_ENV (env);

    AdapterAddresses = initAdapters(env, RetVal);
	isIPv6 = isIPv6Enabled (AdapterAddresses);
	if (RetVal == NO_ERROR) {		
		while (AdapterAddresses != NULL) {
			// when ipv4 in port library, the index is equal to IfIndex while ipv6 it is equal to Ipv6IfIndex.
			U_32 interfaceIndex = isIPv6 ? AdapterAddresses->Ipv6IfIndex : AdapterAddresses->IfIndex;
			if (interfaceIndex == jindex)
			{
				switch(type) {
					case OPERSTAT:
						isSet = ((AdapterAddresses->OperStatus & flag) == flag);
						break;
					case INTERFACETYPE:
						if(flag == IF_TYPE_PPP && isIPv6)
							isSet = ((AdapterAddresses->IfType & IF_TYPE_TUNNEL) == IF_TYPE_TUNNEL);
						else
							isSet = ((AdapterAddresses->IfType & flag) == flag);
						break;
					case FLAGS:
						isSet = ((AdapterAddresses->Flags & flag) == flag);
						break;
					default:
						isSet = JNI_FALSE;
				}
			}
			AdapterAddresses = AdapterAddresses->Next;
		}
	} else {
		throwJavaNetSocketException (env, hyerror_set_last_error(RetVal, HYPORT_ERROR_SOCKET_NORECOVERY));
		return 0;
	}

	if (AdapterAddresses != NULL) {
		hymem_free_memory (AdapterAddresses);
	}
	return isSet;
}

jboolean
getPlatformIsUp(JNIEnv * env, jstring ifname, jint jindex)
{
	return getPlatformNetworkInterfaceAttribute(env,OPERSTAT,jindex,IfOperStatusUp);
}


jboolean getPlatformIsLoopback(JNIEnv * env, jstring ifname, jint jindex)
{
	return getPlatformNetworkInterfaceAttribute(env,INTERFACETYPE,jindex,IF_TYPE_SOFTWARE_LOOPBACK);
}

jboolean getPlatformIsPoint2Point(JNIEnv * env, jstring ifname, jint jindex)
{
	return getPlatformNetworkInterfaceAttribute(env,INTERFACETYPE,jindex,IF_TYPE_PPP);
}


jboolean getPlatformSupportMulticast(JNIEnv * env, jstring ifname, jint jindex)
{
	return !getPlatformNetworkInterfaceAttribute(env,FLAGS,jindex,IP_ADAPTER_NO_MULTICAST);
}


jint
getPlatformMTU(JNIEnv * env, jstring ifname, jint index)
{
	PIP_ADAPTER_ADDRESSES AdapterAddresses = NULL;
	ULONG OutBufferLength = 0;
	ULONG RetVal = 0;
	DWORD mtu = 0;
	jboolean isIPv6 = JNI_FALSE;

	/* required call if we are going to call port library methods */
	PORT_ACCESS_FROM_ENV (env);

    AdapterAddresses = initAdapters(env, RetVal);
	isIPv6 = isIPv6Enabled (AdapterAddresses);

	if (RetVal == NO_ERROR) {		
		while (AdapterAddresses != NULL) {
			// when ipv4 in port library, the index is equal to IfIndex while ipv6 it is equal to Ipv6IfIndex.
			U_32 interfaceIndex = isIPv6 ? AdapterAddresses->Ipv6IfIndex : AdapterAddresses->IfIndex;
			if (interfaceIndex == index)
			{
				mtu =  AdapterAddresses->Mtu;
				break;
			}
			AdapterAddresses = AdapterAddresses->Next;
		}
	} else {
		throwJavaNetSocketException (env, hyerror_set_last_error(RetVal, HYPORT_ERROR_SOCKET_NORECOVERY));
		return 0;
	}

	if (AdapterAddresses != NULL) {
		hymem_free_memory (AdapterAddresses);
	}
	return mtu;
}

jbyteArray 
getPlatformHardwareAddress(JNIEnv * env, jstring ifname, jint index)
{   
	PIP_ADAPTER_ADDRESSES AdapterAddresses = NULL;
	ULONG OutBufferLength = 0;
	ULONG RetVal = 0; 
	jboolean isIPv6 = JNI_FALSE;
	jbyteArray byteArray = NULL;	

	/* required call if we are going to call port library methods */
	PORT_ACCESS_FROM_ENV (env);

	AdapterAddresses = initAdapters(env, RetVal);
	isIPv6 = isIPv6Enabled(AdapterAddresses);

	if (RetVal == NO_ERROR) {		
		while (AdapterAddresses != NULL) {
			U_32 interfaceIndex = isIPv6 ? AdapterAddresses->Ipv6IfIndex : AdapterAddresses->IfIndex;			
			if (interfaceIndex == index)
			{
				// Follow RI here. RI returns null when PhysicalAddressLength is 0 under ipv4 while a zero-length array under ipv6.
				if (AdapterAddresses->PhysicalAddressLength > 0 || isIPv6)
				{
					byteArray = (*env)->NewByteArray(env, AdapterAddresses->PhysicalAddressLength);
					(*env)->SetByteArrayRegion(env, byteArray, 0, AdapterAddresses->PhysicalAddressLength, (jbyte * )AdapterAddresses->PhysicalAddress);
				}				
				break;
			}
			AdapterAddresses = AdapterAddresses->Next;
		}
	} else {
		throwJavaNetSocketException (env, hyerror_set_last_error(RetVal, HYPORT_ERROR_SOCKET_NORECOVERY));
		return NULL;
	}

	if (AdapterAddresses != NULL) {
		hymem_free_memory (AdapterAddresses);
	}
	return byteArray;
}


void
getPlatformInterfaceAddressesImpl(JNIEnv * env, 
									 char* name, 
									 jint index,
									 PIP_ADAPTER_ADDRESSES adapters,
									 interfaceAddressArray_struct* array)
{	
	interfaceAddress_struct *interfaces = NULL;
	interfaceAddress_struct *backInterfaceAddrs = NULL;
	int numAddresses = 0;
	int previousNumAddress = 0;
	jboolean isLoopback = JNI_FALSE;
	jboolean isIPv6 = JNI_FALSE;

	PORT_ACCESS_FROM_ENV (env);	

	// if the interface is a loopback one, it needs some special treatment
	// according to RI's behavior
	isLoopback = strcmp(name, "lo") == 0 ? JNI_TRUE : JNI_FALSE;

	isIPv6 =  isIPv6Enabled(adapters);

	while (adapters != NULL) {
		PIP_ADAPTER_UNICAST_ADDRESS unicastAddr = adapters->FirstUnicastAddress;
		PIP_ADAPTER_PREFIX prefixAddr = adapters->FirstPrefix;
		PIP_ADAPTER_UNICAST_ADDRESS addrPtr = NULL;	
		jboolean isFound = JNI_FALSE;		

		// if the interface is a loopback one, concatenate all the addresses
		isFound = (!isLoopback && index == (isIPv6? adapters->Ipv6IfIndex : adapters->IfIndex)) ||
			(isLoopback && adapters->IfType == IF_TYPE_SOFTWARE_LOOPBACK);

		// calculate number of addresses
		if (isFound) {
			// save the head pointer
			addrPtr = unicastAddr;
			while (unicastAddr != NULL)	{
				numAddresses++;
				unicastAddr = unicastAddr->Next;
			}			
			// recover it after iteration
			unicastAddr = addrPtr;     
		}

		if (isFound && numAddresses > 0) {							
			int counter = 0;

			// initialize the array. 
			backInterfaceAddrs = interfaces;
			interfaces = hymem_allocate_memory (sizeof(interfaceAddress_struct) * (numAddresses));
			for (counter = 0; counter < numAddresses; counter++)
			{
				interfaces[counter].address = NULL;
				interfaces[counter].prefixLength = 0;
			}
			counter = 0;

			if (backInterfaceAddrs != NULL) {
				// If it is not null, copy the previous loopback addresses.
				for (counter = 0; counter < previousNumAddress; counter++)
				{
					interfaces[counter].address = hymem_allocate_memory(sizeof(hyipAddress_struct));
#if defined(VALIDATE_ALLOCATIONS)
					if (NULL == interfaces[counter].address){						
						return ;
					}
#endif
					// copy from original addresses to concatenate 
					if (backInterfaceAddrs[counter].address->length == sizeof(struct in_addr)) {
						interfaces[counter].address->addr.inAddr.S_un.S_addr = backInterfaceAddrs[counter].address->addr.inAddr.S_un.S_addr;
					} else if (backInterfaceAddrs[counter].address->length == sizeof (struct in6_addr)) {						
						memcpy (backInterfaceAddrs[counter].address->addr.bytes,  
							interfaces[counter].address->addr.bytes, sizeof (struct in6_addr));
					}
					interfaces[counter].address->length = backInterfaceAddrs[counter].address->length;
					interfaces[counter].address->scope = backInterfaceAddrs[counter].address->scope;	
					interfaces[counter].prefixLength = backInterfaceAddrs[counter].prefixLength;                    
				}

				// cleanup 
				for (counter = 0; counter < previousNumAddress; counter++)
				{
					hymem_free_memory (backInterfaceAddrs[counter].address);
				}

				hymem_free_memory (backInterfaceAddrs);
			}

			while (unicastAddr != NULL){
				struct sockaddr_in* addr = NULL;
				struct sockaddr_in6* addr6 = NULL;				
				struct sockaddr_in6_old* addr6_old = NULL;

				LPSOCKADDR sock_addr = unicastAddr->Address.lpSockaddr;
				int length = unicastAddr->Address.iSockaddrLength;
				int currentIPAddressIndex = 0;

				if (length == sizeof (struct sockaddr_in)) {				
					addr = (struct sockaddr_in*)sock_addr;
				} else if (length == sizeof (struct sockaddr_in6)){
					addr6 = (struct sockaddr_in6*)sock_addr;
				} else if (length == sizeof (struct sockaddr_in6_old)){
					addr6_old = (struct sockaddr_in6_old*)sock_addr;
				}					

				interfaces[counter].address = hymem_allocate_memory(sizeof(hyipAddress_struct));
#if defined(VALIDATE_ALLOCATIONS)
				if (NULL == interfaces[counter].address){
					return ;
				}
#endif
				if (addr != NULL) {
					// deal with sockaddr_in, copy address					
					interfaces[counter].address->addr.inAddr.S_un.S_addr = addr->sin_addr.S_un.S_addr;
					interfaces[counter].address->length = sizeof (struct in_addr);
					interfaces[counter].address->scope = 0;					
				} else if (addr6 != NULL){
					// deal with sockaddr_in6, copy address
					memcpy (interfaces[counter].address->addr.bytes,
						&(addr6->sin6_addr.u.Byte), sizeof (struct in6_addr));
					interfaces[counter].address->length = sizeof (struct in6_addr);
					interfaces[counter].address->scope = addr6->sin6_scope_id;						
				} else {
					// deal with sockaddr_in6_old, copy address
					memcpy (interfaces[counter].address->addr.bytes,  
						&(addr6_old->sin6_addr.u.Byte), sizeof (struct in6_addr));
					interfaces[counter].address->length = sizeof (struct in6_addr);
					interfaces[counter].address->scope = 0;
				}

				// copy prefixLength
				if (prefixAddr != NULL) {
					interfaces[counter].prefixLength = prefixAddr->PrefixLength;
					prefixAddr = prefixAddr -> Next;
				} else {
					// since GetAdaptersAddresses cannot regonize the prefix length 
					// for ipv4 loopback interface, we hard code the value here
					interfaces[counter].prefixLength = isIPv6 ? 0 : 8;
				}

				unicastAddr = unicastAddr -> Next;		
				counter++;				
			}

			if (isLoopback && adapters->IfType == IF_TYPE_SOFTWARE_LOOPBACK) {
				// If a loopback interface, record the previous address in order to do concatenate
				previousNumAddress = numAddresses;                
			} else {
				break;
			}		
		}
		adapters = adapters->Next;
	}
	array -> length = numAddresses;
	array -> elements = interfaces;
}

I_32 
getPlatformInterfaceAddresses(JNIEnv * env, jstring ifname, jint index, interfaceAddressArray_struct * interfaceAddressArray)
{	
	PIP_ADAPTER_ADDRESSES AdapterAddresses = NULL;
	PIP_ADAPTER_ADDRESSES Adapters = NULL;
	ULONG RetVal = 0;	

	/* required call if we are going to call port library methods */
	PORT_ACCESS_FROM_ENV (env);
	
	AdapterAddresses = initAdapters(env, RetVal);	

	if (RetVal == NO_ERROR) {			
		char* adapterName = convertInterfaceName(env, ifname);
		if (NULL != adapterName)
		{
			getPlatformInterfaceAddressesImpl (env, adapterName, index, AdapterAddresses, interfaceAddressArray);
			hymem_free_memory (adapterName);
		}		
	} else {
		throwJavaNetSocketException (env, hyerror_set_last_error(RetVal, HYPORT_ERROR_SOCKET_NORECOVERY));
		return -1;
	}

	hymem_free_memory (AdapterAddresses);
	return 0;
}

/* Get charset from the OS */
void getOSCharset(char *locale, const size_t size) {
  size_t cp;
  DWORD holder;
#if defined(_WIN32_WCE)
  LCID localeId = GetUserDefaultLCID();
#else
  LCID localeId = GetThreadLocale();
#endif
  if (0 < GetLocaleInfo(localeId, LOCALE_IDEFAULTANSICODEPAGE | LOCALE_RETURN_NUMBER,
                        (LPTSTR)&holder, sizeof(holder) / sizeof(TCHAR))) {
    cp = (size_t)holder;
  } else {
    cp = (size_t)GetACP();
  }
  getCharset(cp, locale, size);
  return;
}
