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

#include "iohelp.h"
#include "jclglob.h"

jlong JNICALL
Java_java_io_FileInputStream_skip (JNIEnv * env, jobject recv, jlong count)
{
  jobject fd;
  IDATA descriptor;
  I_64 currentPosition, endOfFile, charsToSkip;
  PORT_ACCESS_FROM_ENV (env);
  if (count <= 0)
    return 0;

  fd =
    (*env)->GetObjectField (env, recv,
          JCL_CACHE_GET (env,
             FID_java_io_FileInputStream_fd));
  descriptor = (IDATA) getJavaIoFileDescriptorContentsAsPointer (env, fd);

  if (descriptor == -1)
    {
      throwJavaIoIOExceptionClosed (env);
      return 0;
    }

  currentPosition = hyfile_seek (descriptor, 0, HySeekCur);
  endOfFile = hyfile_seek (descriptor, 0, HySeekEnd);
  charsToSkip = (I_64) count > endOfFile
    || (endOfFile - (I_64) count <
  currentPosition) ? endOfFile - currentPosition : (I_64) count;
  hyfile_seek (descriptor, charsToSkip + currentPosition, HySeekSet);
  return (jlong) charsToSkip;
}

jint JNICALL
Java_java_io_FileInputStream_openImpl (JNIEnv * env, jobject recv,
               jbyteArray path)
{
  PORT_ACCESS_FROM_ENV (env);
  IDATA portFD;
  jobject fd;
  jsize length;
  char pathCopy[HyMaxPath];
  if (path == NULL)
    {
      throwNPException (env, "path is null");
      return 0;
    }
  length = (*env)->GetArrayLength (env, path);
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);

  /* Now have the filename, open the file using a portlib call */
  portFD = hyfile_open (pathCopy, (I_32) HyOpenRead, (I_32) 0);

  if (portFD == -1)
    return 1;

  fd =
    (*env)->GetObjectField (env, recv,
          JCL_CACHE_GET (env,
             FID_java_io_FileInputStream_fd));
  setJavaIoFileDescriptorContentsAsPointer (env, fd, (void *) portFD);
  return 0;
}

jint JNICALL
Java_java_io_FileInputStream_available (JNIEnv * env, jobject recv)
{
  /* Call the helper. The helper may throw an exception so either * check for -1 or return immediately */
  return new_ioh_available (env, recv,
          JCL_CACHE_GET (env,
             FID_java_io_FileInputStream_fd));
}

jint JNICALL
Java_java_io_FileInputStream_readImpl (JNIEnv * env, jobject recv,
               jbyteArray buffer, jint offset,
               jint count, jlong descriptor)
{
  /* Call the helper. The helper may throw an exception so this
   * must return immediately.
   */
  return ioh_readbytesImpl (env, recv, buffer, offset, count,
          (IDATA) descriptor);
}

jint JNICALL
Java_java_io_FileInputStream_readByteImpl (JNIEnv * env, jobject recv,
             jlong descriptor)
{
  /* Call the helper. The helper may throw an exception so this
   * must return immediately.
   */
  return ioh_readcharImpl (env, recv, (IDATA) descriptor);
}

void JNICALL
Java_java_io_FileInputStream_oneTimeInitialization (JNIEnv * env,
                jclass clazz)
{
  jfieldID fdFID =
    (*env)->GetFieldID (env, clazz, "fd", "Ljava/io/FileDescriptor;");
  if (!fdFID)
    return;
  JCL_CACHE_SET (env, FID_java_io_FileInputStream_fd, fdFID);
}

void JNICALL
Java_java_io_FileInputStream_closeImpl (JNIEnv * env, jobject recv)
{
  /* Call the helper */
  new_ioh_close (env, recv,
     JCL_CACHE_GET (env, FID_java_io_FileInputStream_fd));
}
