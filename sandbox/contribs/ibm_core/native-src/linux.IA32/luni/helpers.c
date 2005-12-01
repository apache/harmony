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
#include "jclprots.h"
#include "jclglob.h"

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
  rootStrings[1] = (char) NULL;
  rootStrings[2] = (char) NULL;
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

  PORT_ACCESS_FROM_ENV (env);
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

jbyteArray
getPlatformPath (JNIEnv * env, jbyteArray path)
{
  return NULL;
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
  I_32 result;
  struct stat buffer;

  result = stat (path, &buffer);
  if (result == -1)
    return 0;

  if (buffer.st_uid == geteuid ())
    return (buffer.st_mode & S_IWUSR) == 0;
  else if (buffer.st_gid == getegid ())
    return (buffer.st_mode & S_IWGRP) == 0;

  return (buffer.st_mode & S_IWOTH) == 0;

}

/**
 * Answer 1 if the path is write-only, 0 otherwise even in fail cases.
 */
I_32
getPlatformIsWriteOnly (JNIEnv * env, char *path)
{
  I_32 result;
  struct stat buffer;

  result = stat (path, &buffer);
  if (result == -1)
    return 0;

  if (buffer.st_uid == geteuid ())
    return (buffer.st_mode & S_IRUSR) == 0;
  else if (buffer.st_gid == getegid ())
    return (buffer.st_mode & S_IRGRP) == 0;

  return (buffer.st_mode & S_IROTH) == 0;

}

/* Resolve link if it is a symbolic link and put the result in link. */
int
platformReadLink (char *link)
{

  int size = readlink (link, link, HyMaxPath);
  if (size <= 0)
    return FALSE;
  if (size >= HyMaxPath)
    link[HyMaxPath - 1] = 0;
  else
    link[size] = 0;
  return TRUE;

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
  PORT_ACCESS_FROM_ENV (env);
  BOOLEAN value = TRUE;

  hysock_setopt_bool (socketP, HY_SOL_SOCKET, HY_SO_REUSEADDR, &value);
}
