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
 * Common natives supporting the file system interface.
 */

#include <harmony.h>

#include "OSFileSystem.h"
#include "IFileSystem.h"

/*
 * Class:     com_ibm_platform_OSFileSystem
 * Method:    readDirectImpl
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_platform_OSFileSystem_readDirectImpl
  (JNIEnv * env, jobject thiz, jlong fd, jlong buf, jint nbytes)
{
  PORT_ACCESS_FROM_ENV (env);
  return (jlong) hyfile_read ((IDATA) fd, (void *) buf, (IDATA) nbytes);
}

/*
 * Class:     com_ibm_platform_OSFileSystem
 * Method:    writeDirectImpl
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_platform_OSFileSystem_writeDirectImpl
  (JNIEnv * env, jobject thiz, jlong fd, jlong buf, jint nbytes)
{
  PORT_ACCESS_FROM_ENV (env);
  return (jlong) hyfile_write ((IDATA) fd, (const void *) buf,
                               (IDATA) nbytes);
}

/*
 * Class:     com_ibm_platform_OSFileSystem
 * Method:    readImpl
 * Signature: (J[BII)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_platform_OSFileSystem_readImpl
  (JNIEnv * env, jobject thiz, jlong fd, jbyteArray byteArray, jint offset,
   jint nbytes)
{
  PORT_ACCESS_FROM_ENV (env);
  jboolean isCopy;
  jbyte *bytes = (*env)->GetByteArrayElements (env, byteArray, &isCopy);
  jlong result;

  result =
    (jlong) hyfile_read ((IDATA) fd, (void *) (bytes + offset),
                         (IDATA) nbytes);
  if (isCopy == JNI_TRUE)
    {
      (*env)->ReleaseByteArrayElements (env, byteArray, bytes, 0);
    }

  return result;
}

/**
 * Seeks a file descriptor to a given file position.
 * 
 * @param env pointer to Java environment
 * @param thiz pointer to object receiving the message
 * @param fd handle of file to be seeked
 * @param offset distance of movement in bytes relative to whence arg
 * @param whence enum value indicating from where the offset is relative
 * The valid values are defined in fsconstants.h.
 * @return the new file position from the beginning of the file, in bytes;
 * or -1 if a problem occurs.
 */
JNIEXPORT jlong JNICALL Java_com_ibm_platform_OSFileSystem_seekImpl
  (JNIEnv * env, jobject thiz, jlong fd, jlong offset, jint whence)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 hywhence = 0;

  /* Convert whence argument */
  switch (whence)
    {
      case com_ibm_platform_IFileSystem_SEEK_SET:
        hywhence = HySeekSet;
        break;
      case com_ibm_platform_IFileSystem_SEEK_CUR:
        hywhence = HySeekCur;
        break;
      case com_ibm_platform_IFileSystem_SEEK_END:
        hywhence = HySeekEnd;
        break;
      default:
        return -1;
    }

  return (jlong) hyfile_seek ((IDATA) fd, (IDATA) offset, hywhence);
}

/**
 * Flushes a file state to disk.
 *
 * @param env pointer to Java environment
 * @param thiz pointer to object receiving the message
 * @param fd handle of file to be flushed
 * @param metadata if true also flush metadata, otherwise just flush data is possible.
 * @return zero on success and -1 on failure
 *
 * Method:    fflushImpl
 * Signature: (JZ)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_platform_OSFileSystem_fflushImpl
  (JNIEnv * env, jobject thiz, jlong fd, jboolean metadata)
{
  PORT_ACCESS_FROM_ENV (env);

  return (jint) hyfile_sync ((IDATA) fd);
}

/**
 * Closes the given file handle
 * 
 * @param env pointer to Java environment
 * @param thiz pointer to object receiving the message
 * @param fd handle of file to be closed
 * @return zero on success and -1 on failure
 *
 * Class:     com_ibm_platform_OSFileSystem
 * Method:    closeImpl
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_platform_OSFileSystem_closeImpl
  (JNIEnv * env, jobject thiz, jlong fd)
{
  PORT_ACCESS_FROM_ENV (env);

  return (jint) hyfile_close ((IDATA) fd);
}

