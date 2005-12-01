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

jint JNICALL
Java_java_io_FileOutputStream_openImpl (JNIEnv * env, jobject recv,
          jbyteArray path, jboolean append)
{
  jobject fd;
  I_32 flags =
    append ==
    0 ? HyOpenCreate | HyOpenWrite | HyOpenTruncate : HyOpenWrite |
    HyOpenCreate;
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
  portFD = hyfile_open (pathCopy, flags, 0666);

  if (portFD == -1)
    {
      return 1;
    }

  if (append != 0)
    {
      hyfile_seek (portFD, 0, HySeekEnd);
    }

  fd =
    (*env)->GetObjectField (env, recv,
          JCL_CACHE_GET (env,
             FID_java_io_FileOutputStream_fd));
  setJavaIoFileDescriptorContentsAsPointer (env, fd, (void *) portFD);
  return 0;
}

void JNICALL
Java_java_io_FileOutputStream_writeImpl (JNIEnv * env, jobject recv,
           jbyteArray buffer, jint offset,
           jint count, jlong descriptor)
{
  /* Call the helper. The helper may throw an exception so this
   * must return immediately.
   */
  ioh_writebytesImpl (env, recv, buffer, offset, count, (IDATA) descriptor);
}

void JNICALL
Java_java_io_FileOutputStream_writeByteImpl (JNIEnv * env, jobject recv,
               jint c, jlong descriptor)
{
  /* Call the helper. The helper may throw an exception so this
   * must return immediately.
   */
  ioh_writecharImpl (env, recv, c, (IDATA) descriptor);
}

void JNICALL
Java_java_io_FileOutputStream_oneTimeInitialization (JNIEnv * env,
                 jclass clazz)
{
  jfieldID fdFID =
    (*env)->GetFieldID (env, clazz, "fd", "Ljava/io/FileDescriptor;");
  if (!fdFID)
    return;
  JCL_CACHE_SET (env, FID_java_io_FileOutputStream_fd, fdFID);
}

void JNICALL
Java_java_io_FileOutputStream_closeImpl (JNIEnv * env, jobject recv)
{
  /* Call the helper */
  new_ioh_close (env, recv,
     JCL_CACHE_GET (env, FID_java_io_FileOutputStream_fd));
}
