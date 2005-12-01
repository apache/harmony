/* Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
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

#include "jcl.h"
#include "jclprots.h"
#include "jclglob.h"

#include "portsock.h"

int platformReadLink (char *link);
jbyteArray getPlatformPath (JNIEnv * env, jbyteArray path);
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

UDATA platformFindfirst (char *path, char *resultbuf);
int portCmp (const void **a, const void **b);
static void unmangle (JNIEnv * env, LPWSTR data);
I_32 getPlatformAttribute (JNIEnv * env, char *path, DWORD attribute);
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

/**
 * Answer 1 if the path is write-only, 0 otherwise even in fail cases.
 */
I_32
getPlatformIsWriteOnly (JNIEnv * env, char *path)
{
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
