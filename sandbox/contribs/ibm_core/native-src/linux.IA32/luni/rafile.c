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

#include <string.h>
#include "iohelp.h"
#include "jclglob.h"
#include "helpers.h"

jlong JNICALL
Java_java_io_RandomAccessFile_getFilePointer (JNIEnv * env, jobject recv)
{
  jobject fd;
  IDATA descriptor;
  PORT_ACCESS_FROM_ENV (env);

  fd =
    (*env)->GetObjectField (env, recv,
                            JCL_CACHE_GET (env,
                                           FID_java_io_RandomAccessFile_fd));
  descriptor = (IDATA) getJavaIoFileDescriptorContentsAsPointer (env, fd);

  if (descriptor == -1)
    {
      throwJavaIoIOExceptionClosed (env);
      return 0;
    }

  return (jlong) hyfile_seek (descriptor, 0, HySeekCur);
}

void JNICALL
Java_java_io_RandomAccessFile_seek (JNIEnv * env, jobject recv, jlong pos)
{
  jobject fd;
  IDATA descriptor;
  PORT_ACCESS_FROM_ENV (env);

  if (pos < 0)
    {
      throwJavaIoIOException (env, "");
      return;
    }

  fd =
    (*env)->GetObjectField (env, recv,
                            JCL_CACHE_GET (env,
                                           FID_java_io_RandomAccessFile_fd));
  descriptor = (IDATA) getJavaIoFileDescriptorContentsAsPointer (env, fd);

  if (descriptor == -1)
    {
      throwJavaIoIOExceptionClosed (env);
      return;
    }

  hyfile_seek (descriptor, (I_64) pos, HySeekSet);
}

jint JNICALL
Java_java_io_RandomAccessFile_openImpl (JNIEnv * env, jobject recv,
                                        jbyteArray path, jboolean writable)
{
  jobject fd;
  I_32 flags =
    writable == 0 ? HyOpenRead : HyOpenRead | HyOpenWrite | HyOpenCreate;
  I_32 mode = writable == 0 ? 0 : 0666;
  IDATA portFD;
  PORT_ACCESS_FROM_ENV (env);
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
  portFD = hyfile_open (pathCopy, flags, mode);

  if (portFD == -1)
    return 1;

  fd =
    (*env)->GetObjectField (env, recv,
                            JCL_CACHE_GET (env,
                                           FID_java_io_RandomAccessFile_fd));
  setJavaIoFileDescriptorContentsAsPointer (env, fd, (void *) portFD);
  return 0;
}

jlong JNICALL
Java_java_io_RandomAccessFile_length (JNIEnv * env, jobject recv)
{
  jobject fd;
  I_64 currentPosition, endOfFile;
  IDATA descriptor;
  PORT_ACCESS_FROM_ENV (env);

  fd =
    (*env)->GetObjectField (env, recv,
                            JCL_CACHE_GET (env,
                                           FID_java_io_RandomAccessFile_fd));
  descriptor = (IDATA) getJavaIoFileDescriptorContentsAsPointer (env, fd);

  if (descriptor == -1)
    {
      throwJavaIoIOExceptionClosed (env);
      return 0;
    }

  currentPosition = hyfile_seek (descriptor, 0, HySeekCur);
  endOfFile = hyfile_seek (descriptor, 0, HySeekEnd);
  hyfile_seek (descriptor, currentPosition, HySeekSet);
  return (jlong) endOfFile;
}

jint JNICALL
Java_java_io_RandomAccessFile_readByteImpl (JNIEnv * env, jobject recv,
                                            jlong descriptor)
{
  /* Call the helper. The helper may throw an exception so this
   * must return immediately.
   */
  return ioh_readcharImpl (env, recv, (IDATA) descriptor);
}

jint JNICALL
Java_java_io_RandomAccessFile_readImpl (JNIEnv * env, jobject recv,
                                        jbyteArray buffer, jint offset,
                                        jint count, jlong descriptor)
{
  /* Call the helper. The helper may throw an exception so this
   * must return immediately.
   */
  return ioh_readbytesImpl (env, recv, buffer, offset, count,
                            (IDATA) descriptor);
}

void JNICALL
Java_java_io_RandomAccessFile_writeImpl (JNIEnv * env, jobject recv,
                                         jbyteArray buffer, jint offset,
                                         jint count, jlong descriptor)
{
  /* Call the helper. The helper may throw an exception so this
   * must return immediately.
   */
  ioh_writebytesImpl (env, recv, buffer, offset, count, (IDATA) descriptor);
}

void JNICALL
Java_java_io_RandomAccessFile_writeByteImpl (JNIEnv * env, jobject recv,
                                             jint c, jlong descriptor)
{
  /* Call the helper. The helper may throw an exception so this
   * must return immediately.
   */
  ioh_writecharImpl (env, recv, c, (IDATA) descriptor);
}

void JNICALL
Java_java_io_RandomAccessFile_oneTimeInitialization (JNIEnv * env,
                                                     jclass rafClazz)
{
  jfieldID fdFid =
    (*env)->GetFieldID (env, rafClazz, "fd", "Ljava/io/FileDescriptor;");
  if (!fdFid)
    return;
  JCL_CACHE_SET (env, FID_java_io_RandomAccessFile_fd, fdFid);
}

void JNICALL
Java_java_io_RandomAccessFile_setLengthImpl (JNIEnv * env, jobject recv,
                                             jlong newLength)
{
  jobject fd;
  IDATA descriptor;
  I_64 oldPos;
  PORT_ACCESS_FROM_ENV (env);

  if (newLength < 0)
    {
      throwJavaIoIOException (env, "Length must be positive");
      return;
    }

  fd =
    (*env)->GetObjectField (env, recv,
                            JCL_CACHE_GET (env,
                                           FID_java_io_RandomAccessFile_fd));
  descriptor = (IDATA) getJavaIoFileDescriptorContentsAsPointer (env, fd);

  if (descriptor == -1)
    {
      throwJavaIoIOExceptionClosed (env);
      return;
    }

  oldPos = hyfile_seek (descriptor, 0, HySeekCur);
  if (!setPlatformFileLength (env, descriptor, newLength))
    {
      throwJavaIoIOException (env, "SetLength failed");
      return;
    }
  if (oldPos < (I_64) newLength)
    {
      hyfile_seek (descriptor, oldPos, HySeekSet);
    }
  else
    {
      hyfile_seek (descriptor, (I_64) newLength, HySeekSet);
    }
}

void JNICALL
Java_java_io_RandomAccessFile_closeImpl (JNIEnv * env, jobject recv)
{
  /* Call the helper */
  new_ioh_close (env, recv,
                 JCL_CACHE_GET (env, FID_java_io_RandomAccessFile_fd));
}
