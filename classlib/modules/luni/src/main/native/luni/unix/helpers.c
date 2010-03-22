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

#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>

#include <utime.h>

#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <locale.h>

#include <langinfo.h>

#include <dirent.h>

#include "helpers.h"
#include "harmonyglob.h"

int portCmp (const void **a, const void **b);

/**
 * It is the responsibility of #getPlatformRoots to return a char array
 * with volume names separated by null with a trailing extra null, so for
 * Unix it should be '\<null><null>' .
 */
I_32
getPlatformRoots (char *rootStrings)
{
  rootStrings[0] = (char) '/';
  rootStrings[1] = (char) 0;
  rootStrings[2] = (char) 0;
  return 1;
}

/**
 * Answer 1 if the path is hidden, 0 otherwise even in fail cases.
 */
I_32
getPlatformIsHidden (JNIEnv * env, char *path)
{
  PORT_ACCESS_FROM_ENV (env);

  /* Answer true if the file exists and starts with a period */
  I_32 length = strlen (path), index, existsResult;
  existsResult = hyfile_attr (path);
  if (existsResult < 0)
    return 0;

  if (length == 0)
    return 0;
  for (index = length; index >= 0; index--)
    {
      if (path[index] == '.' && (index > 0 && path[index - 1] == '/'))
        return 1;
    }

  return 0;
}

/**
 * Answer 1 if the file time was updated, 0 otherwise even in fail cases.
 */
I_32
setPlatformLastModified (JNIEnv * env, char *path, I_64 time)
{

  struct stat statbuf;
  struct utimbuf timebuf;
  if (stat (path, &statbuf))
    return FALSE;
  timebuf.actime = statbuf.st_atime;
  timebuf.modtime = (time_t) (time / 1000);
  return utime (path, &timebuf) == 0;

}

/**
 * Answer 1 if the path is now readOnly, 0 otherwise even in fail cases.
 */
I_32
setPlatformReadOnly (JNIEnv * env, char *path)
{
  struct stat buffer;
  mode_t mode;
  if (stat (path, &buffer))
    {
      return 0;
    }
  mode = buffer.st_mode;
  mode = mode & 07555;
  return chmod (path, mode) == 0;

}

/**
 * Answer 1 if the file length was set, 0 otherwise even in fail cases.
 */
I_32
setPlatformFileLength (JNIEnv * env, IDATA descriptor, jlong newLength)
{

  return (ftruncate ((int) descriptor, newLength) == 0);

}

void
setPlatformBindOptions (JNIEnv * env, hysocket_t socketP)
{
  PORT_ACCESS_FROM_ENV (env);
  BOOLEAN value = TRUE;

  hysock_setopt_bool (socketP, HY_SOL_SOCKET, HY_SO_REUSEADDR, &value);
}

/**
 * Answer 1 if the path is read-only, 0 otherwise even in fail cases.
 */
I_32
getPlatformIsReadOnly (JNIEnv * env, char *path)
{
  return access(path, W_OK) !=0;
}

/**
 * Answer 1 if the path is write-only, 0 otherwise even in fail cases.
 */
I_32
getPlatformIsWriteOnly (JNIEnv * env, char *path)
{
  return access(path, R_OK) !=0;
}

/* Resolve link if it is a symbolic link and put the result in link. */
int
platformReadLink (char *link)
{

  int size = readlink (link, link, HyMaxPath-1);
  if (size <= 0)
    return FALSE;
  link[size] = 0;
  return TRUE;

}

jstring
getCustomTimeZoneInfo (JNIEnv * env, jintArray tzinfo,
                       jbooleanArray isCustomTimeZone)
{
    time_t curTime;
    struct tm *tmStruct;
    char tzInfo[9];
    int h, m;
    jboolean fls;

    time(&curTime);
    //curTime += 15552000l;
    tmStruct = localtime(&curTime);
    // timezone is now set to time zone offset
    // tmStruct->tm_isdst is set to 1 if DST is in effect
    strcpy(tzInfo, "GMT");
    tzInfo[3] = timezone > 0 ? '-' : '+';
#if defined (FREEBSD) || defined(MACOSX)
    h = labs(tmStruct->tm_gmtoff) / 3600;
#else /* !FREEBSD */
    h = labs(timezone) / 3600;
#endif /* FREEBSD */
    if (tmStruct->tm_isdst) {
        if (timezone > 0) {
            h--;
        } else {
            h++;
        }
    }
#if defined (FREEBSD) || defined(MACOSX)
    m = (labs(tmStruct->tm_isdst) % 3600) / 60;
#else /* !FREEBSD */
    m = (labs(timezone) % 3600) / 60;
#endif /* FREEBSD */
   tzInfo[4] = h / 10 + '0';
    tzInfo[5] = h % 10 + '0';
    tzInfo[6] = m / 10 + '0';
    tzInfo[7] = m % 10 + '0';
    tzInfo[8] = 0;

    fls = JNI_FALSE;

    (*env)->SetBooleanArrayRegion(env, isCustomTimeZone, 0, 1, &fls);
    return (*env)->NewStringUTF(env, tzInfo);
}

void
setDefaultServerSocketOptions (JNIEnv * env, hysocket_t socketP)
{
  PORT_ACCESS_FROM_ENV (env);
  BOOLEAN value = TRUE;

  hysock_setopt_bool (socketP, HY_SOL_SOCKET, HY_SO_REUSEADDR, &value);
}

/* Get charset from the OS */
void getOSCharset(char *locale, const size_t size) {
  char * codec = NULL;
  size_t cur = 0;
  short flag = 0;
  setlocale(LC_CTYPE, "");
  codec = setlocale(LC_CTYPE, NULL);
  // get codeset from language[_territory][.codeset][@modifier]
  while (*codec) {
    if (!flag) {
      if (*codec != '.') {
        codec++;
        continue;
      } else {
        flag = 1;
        codec++;
      }
    } else {
      if (*codec == '@') {
        break;
      } else {
        locale[cur++] = (*codec);
        codec++;
        if (cur >= size) {
          // Not enough size
          cur = 0;
          break;
        }
      }
    }
  }
  locale[cur] = '\0';
  if (!strlen(locale)) {
    strcpy(locale, "8859_1");
  }
  return;
}
