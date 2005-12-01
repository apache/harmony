/* Copyright 2004, 2005 The Apache Software Foundation or its licensors, as applicable
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
 * Linux32 specific natives supporting the file system interface.
 */

#include <fcntl.h>
#include <bits/wordsize.h>
#include <errno.h>

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
  // TODO: 
  return (jlong) - 1L;
}

/**
 * Lock the file identified by the given handle.
 * The range and lock type are given.
 */
JNIEXPORT jint JNICALL Java_com_ibm_platform_OSFileSystem_lockImpl
  (JNIEnv * env, jobject thiz, jlong handle, jlong start, jlong length,
   jint typeFlag, jboolean waitFlag)
{
  int rc;
  int waitMode = (waitFlag) ? F_SETLKW : F_SETLK;
  struct flock lock = { 0 };

  // If start or length overflow the max values we can represent, then max them out.
#if __WORDSIZE==32
#define MAX_INT 0x7fffffffL
  if (start > MAX_INT)
    {
      start = MAX_INT;
    }
  if (length > MAX_INT)
    {
      length = MAX_INT;
    }
#endif

  lock.l_whence = SEEK_SET;
  lock.l_start = start;
  lock.l_len = length;

  if ((typeFlag & com_ibm_platform_IFileSystem_SHARED_LOCK_TYPE) ==
      com_ibm_platform_IFileSystem_SHARED_LOCK_TYPE)
    {
      lock.l_type = F_RDLCK;
    }
  else
    {
      lock.l_type = F_WRLCK;
    }

  do
    {
      rc = fcntl (handle, waitMode, &lock);
    }
  while ((rc < 0) && (errno == EINTR));

  return (rc == -1) ? -1 : 0;
}

/**
 * Unlocks the specified region of the file.
 */
JNIEXPORT jint JNICALL Java_com_ibm_platform_OSFileSystem_unlockImpl
  (JNIEnv * env, jobject thiz, jlong handle, jlong start, jlong length)
{
  int rc;
  struct flock lock = { 0 };

  // If start or length overflow the max values we can represent, then max them out.
#if __WORDSIZE==32
#define MAX_INT 0x7fffffffL
  if (start > MAX_INT)
    {
      start = MAX_INT;
    }
  if (length > MAX_INT)
    {
      length = MAX_INT;
    }
#endif

  lock.l_whence = SEEK_SET;
  lock.l_start = start;
  lock.l_len = length;
  lock.l_type = F_UNLCK;

  do
    {
      rc = fcntl (handle, F_SETLKW, &lock);
    }
  while ((rc < 0) && (errno == EINTR));

  return (rc == -1) ? -1 : 0;
}
