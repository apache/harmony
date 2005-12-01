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

jfieldID getJavaIoFileDescriptorDescriptorFID (JNIEnv * env);

/**
  * Throw java.io.IOException with the message provided
  */
void
throwJavaIoIOException (JNIEnv * env, char *message)
{
  jclass exceptionClass = (*env)->FindClass(env, "java/io/IOException");
  if (0 == exceptionClass) { 
    /* Just return if we can't load the exception class. */
    return;
    }
  (*env)->ThrowNew(env, exceptionClass, message);
}

/**
  * This will convert all separators to the proper platform separator
  * and remove duplicates on non POSIX platforms.
  */
void
ioh_convertToPlatform (char *path)
{
  char *pathIndex;
  int length = strlen (path);

  /* Convert all separators to the same type */
  pathIndex = path;
  while (*pathIndex != '\0')
    {
      if ((*pathIndex == '\\' || *pathIndex == '/')
          && (*pathIndex != jclSeparator))
        *pathIndex = jclSeparator;
      pathIndex++;
    }

  /* Remove duplicate separators */
  if (jclSeparator == '/')
    return;                     /* Do not do POSIX platforms */

  /* Remove duplicate initial separators */
  pathIndex = path;
  while ((*pathIndex != '\0') && (*pathIndex == jclSeparator))
    {
      pathIndex++;
    }
  if ((pathIndex > path) && (length > (pathIndex - path))
      && (*(pathIndex + 1) == ':'))
    {
      /* For Example '////c:/*' */
      int newlen = length - (pathIndex - path);
      memmove (path, pathIndex, newlen);
      path[newlen] = '\0';
    }
  else
    {
      if ((pathIndex - path > 3) && (length > (pathIndex - path)))
        {
          /* For Example '////serverName/*' */
          int newlen = length - (pathIndex - path) + 2;
          memmove (path, pathIndex - 2, newlen);
          path[newlen] = '\0';
        }
    }
  /* This will have to handle extra \'s but currently doesn't */
}

/**
  * Throw java.io.IOException with the "File closed" message
  * Consolidate all through here so message is consistent.
  */
void
throwJavaIoIOExceptionClosed (JNIEnv * env)
{
  throwJavaIoIOException (env, "File closed");
}

/**
  * Throw java.lang.IndexOutOfBoundsException
  */
void
throwIndexOutOfBoundsException (JNIEnv * env)
{
  jclass exceptionClass = (*env)->FindClass(env, "java/lang/IndexOutOfBoundsException");
  if (0 == exceptionClass) { 
    /* Just return if we can't load the exception class. */
    return;
    }
  (*env)->ThrowNew(env, exceptionClass, "");
}

/**
  * This will write count bytes from buffer starting at offset
  */
void
ioh_writebytesImpl (JNIEnv * env, jobject recv, jbyteArray buffer,
                    jint offset, jint count, IDATA descriptor)
{
  I_32 result = 0;
  jbyte *buf;
  PORT_ACCESS_FROM_ENV (env);
  jsize len;
  char *errorMessage;

/* TODO: ARRAY PINNING */
#define INTERNAL_MAX 512
  U_8 internalBuffer[INTERNAL_MAX];

  if (buffer == NULL)
    {
      throwNPException (env, "buffer is null");
      return;
    }

  len = (*env)->GetArrayLength (env, buffer);

  /**
   * If offset is negative, or count is negative, or offset+count is greater
   * than the length of the array b, then an IndexOutOfBoundsException is thrown.
   * Must test offset > len, or len - offset < count to avoid int overflow caused
   * by offset + count
   */
  if (offset < 0 || count < 0 || offset > len || (len - offset) < count)
    {
      throwIndexOutOfBoundsException (env);
      return;
    }

  /* If len or count is zero, just return 0 */
  if (len == 0 || count == 0)
    return;

  if (descriptor == -1)
    {
      throwJavaIoIOExceptionClosed (env);
      return;
    }
  if (count > INTERNAL_MAX)
    {
      buf = jclmem_allocate_memory (env, count);
    }
  else
    {
      buf = internalBuffer;
    }

  if (buf == NULL)
    {
      throwNewOutOfMemoryError (env, "");
      return;
    }
  ((*env)->GetByteArrayRegion (env, buffer, offset, count, buf));

  result = hyfile_write (descriptor, buf, count);

  /* if there is an error, find the error message before calling free in case hymem_free_memory changes the error code */
  if (result < 0)
    errorMessage = ioLookupErrorString (env, result);

  if (buf != internalBuffer)
    {
      jclmem_free_memory (env, buf);
    }
#undef INTERNAL_MAX

  if (result < 0)
    throwJavaIoIOException (env, errorMessage);
}

/**
  * This will write one byte
  */
void
ioh_writecharImpl (JNIEnv * env, jobject recv, jint c, IDATA descriptor)
{
  I_32 result = 0;
  char buf[1];
  PORT_ACCESS_FROM_ENV (env);

  if (descriptor == -1)
    {
      throwJavaIoIOExceptionClosed (env);
      return;
    }

  buf[0] = (char) c;
  result = hyfile_write (descriptor, buf, 1);

  if (result < 0)
    throwJavaIoIOException (env, ioLookupErrorString (env, result));
}

/**
  * This will read a single character from the descriptor
  */
jint
ioh_readcharImpl (JNIEnv * env, jobject recv, IDATA descriptor)
{
  I_32 result;
  char buf[1];
  PORT_ACCESS_FROM_ENV (env);

  if (descriptor == -1)
    {
      throwJavaIoIOExceptionClosed (env);
      return 0;
    }

  if (descriptor == 0)
    {
      result = hytty_get_chars (buf, 1);
    }
  else
    {
      result = hyfile_read (descriptor, buf, 1);
    }

  if (result <= 0)
    return -1;

  return (jint) buf[0] & 0xFF;
}

/**
  * This will read a up to count bytes into buffer starting at offset
  */
jint
ioh_readbytesImpl (JNIEnv * env, jobject recv, jbyteArray buffer, jint offset,
                   jint count, IDATA descriptor)
{
  I_32 result;
  jsize len;
  jbyte *buf;

/* TODO: ARRAY PINNING */
#define INTERNAL_MAX 2048
  U_8 internalBuffer[INTERNAL_MAX];

  PORT_ACCESS_FROM_ENV (env);

  if (buffer == NULL)
    {
      throwNPException (env, "buffer is null");
      return 0;
    }

  len = (*env)->GetArrayLength (env, buffer);
  /* Throw IndexOutOfBoundsException according to spec. * Must test offset > len, or len - offset < count to avoid * 
     int overflow caused by offset + count */
  if (offset < 0 || count < 0 || offset > len || (len - offset) < count)
    {
      throwIndexOutOfBoundsException (env);
      return 0;
    }
  /* If len is 0, simply return 0 (even if it is closed) */
  if (len == 0 || count == 0)
    return 0;

  if (descriptor == -1)
    {
      throwJavaIoIOExceptionClosed (env);
      return 0;
    }
  if (len >= INTERNAL_MAX)
    {
      buf = jclmem_allocate_memory (env, len);
    }
  else
    {
      buf = internalBuffer;
    }

  if (buf == NULL)
    {
      throwNewOutOfMemoryError (env, "");
      return 0;
    }
  /* Must FREE buffer before returning */

  if (descriptor == 0)
    {
      if ((result = hytty_get_chars (buf, count)) == 0)
        result = -1;
    }
  else
    {
      result = hyfile_read (descriptor, buf, count);
    }
  if (result > 0)
    (*env)->SetByteArrayRegion (env, buffer, offset, result, buf);

  if (buf != internalBuffer)
    {
      jclmem_free_memory (env, buf);
    }
#undef INTERNAL_MAX

  return result;
}

/**
  * Throw java.lang.NullPointerException with the message provided
  * Note: This is not named throwNullPointerException because is conflicts
  * with a VM function of that same name and this causes problems on
  * some platforms.
  */
void
throwNPException (JNIEnv * env, char *message)
{
  jclass exceptionClass = (*env)->FindClass(env, "java/lang/NullPointerException");
  if (0 == exceptionClass) { 
    /* Just return if we can't load the exception class. */
    return;
    }
  (*env)->ThrowNew(env, exceptionClass, message);
}

/**
  * This will return the number of chars left in the file
  */
jint
new_ioh_available (JNIEnv * env, jobject recv, jfieldID fdFID)
{
  jobject fd;
  I_64 currentPosition, endOfFile;
  IDATA descriptor;
  PORT_ACCESS_FROM_ENV (env);

  /* fetch the fd field from the object */
  fd = (*env)->GetObjectField (env, recv, fdFID);

  /* dereference the C pointer from the wrapper object */
  descriptor = (IDATA) getJavaIoFileDescriptorContentsAsPointer (env, fd);
  if (descriptor == -1)
    {
      throwJavaIoIOExceptionClosed (env);
      return -1;
    }

  /**
   * If the descriptor represents StdIn, call the hytty port library.
   */
  if (descriptor == 0)
    {
      return hytty_available ();
    }

  currentPosition = hyfile_seek (descriptor, 0, HySeekCur);
  endOfFile = hyfile_seek (descriptor, 0, HySeekEnd);
  hyfile_seek (descriptor, currentPosition, HySeekSet);
  return (jint) (endOfFile - currentPosition);
}

/**
  * This will close a file descriptor
  */
void
new_ioh_close (JNIEnv * env, jobject recv, jfieldID fdFID)
{
  jobject fd;
  jfieldID descriptorFID;
  IDATA descriptor;
  PORT_ACCESS_FROM_ENV (env);

  descriptorFID = getJavaIoFileDescriptorDescriptorFID (env);
  if (NULL == descriptorFID)
    {
      return;
    }

  /* fetch the fd field from the object */
  fd = (*env)->GetObjectField (env, recv, fdFID);

  /* dereference the C pointer from the wrapper object */
  descriptor = (IDATA) getJavaIoFileDescriptorContentsAsPointer (env, fd);

  /* Check for closed file, in, out, and err */
  if (descriptor >= -1 && descriptor <= 2)
    {
      return;
    }

  hyfile_close (descriptor);
  setJavaIoFileDescriptorContentsAsPointer (env, fd, (void *) -1);

  return;
}

/**
  * This will retrieve the 'descriptor' field value from a java.io.FileDescriptor
  */
void *
getJavaIoFileDescriptorContentsAsPointer (JNIEnv * env, jobject fd)
{
  jfieldID descriptorFID = getJavaIoFileDescriptorDescriptorFID (env);
  if (NULL == descriptorFID)
    {
      return (void *) -1;
    }
  return (void *) ((*env)->GetLongField (env, fd, descriptorFID));
}

/**
  * This will set the 'descriptor' field value in the java.io.FileDescriptor
  * @fd to the value @desc 
  */
void
setJavaIoFileDescriptorContentsAsPointer (JNIEnv * env, jobject fd,
                                          void *value)
{
  jfieldID fid = getJavaIoFileDescriptorDescriptorFID (env);
  if (NULL != fid)
    {
      (*env)->SetLongField (env, fd, fid, (jlong) value);
    }
}

/**
  * Throw java.lang.OutOfMemoryError
  */
void
throwNewOutOfMemoryError (JNIEnv * env, char *message)
{
  jclass exceptionClass = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
  if (0 == exceptionClass) { 
    /* Just return if we can't load the exception class. */
    return;
    }
  (*env)->ThrowNew(env, exceptionClass, message);
}

/**
 * Answer the errorString corresponding to the errorNumber, if available.
 * This function will answer a default error string, if the errorNumber is not
 * recognized.
 *
 * This function will have to be reworked to handle internationalization properly, removing
 * the explicit strings.
 *
 * @param	anErrorNum		the error code to resolve to a human readable string
 *
 * @return	a human readable error string
 */
char *
ioLookupErrorString (JNIEnv * env, I_32 anErrorNum)
{
  PORT_ACCESS_FROM_ENV (env);
  switch (anErrorNum)
    {
    case HYPORT_ERROR_FILE_NOTFOUND:
      return "File not found";
    case HYPORT_ERROR_FILE_NOPERMISSION:
      return "Lacking proper permissions to perform the operation";
    case HYPORT_ERROR_FILE_DISKFULL:
      return "Disk is full";
    case HYPORT_ERROR_FILE_NOENT:
      return "A component of the path name does not exist";
    case HYPORT_ERROR_FILE_NOTDIR:
      return "A component of the path name is not a directory";
    case HYPORT_ERROR_FILE_BADF:
      return "File descriptor invalid";
    case HYPORT_ERROR_FILE_EXIST:
      return "File already exists";
    case HYPORT_ERROR_FILE_INVAL:
      return "A parameter is invalid";
    case HYPORT_ERROR_FILE_LOOP:
      return "Followed too many symbolic links, possibly stuck in loop";
    case HYPORT_ERROR_FILE_NAMETOOLONG:
      return "Filename exceeds maximum length";
    default:
      return (char *) hyfile_error_message ();
    }
}

/**
  * This will retrieve the 'descriptor' field value from a java.io.FileDescriptor
  */
jfieldID
getJavaIoFileDescriptorDescriptorFID (JNIEnv * env)
{
  jclass descriptorCLS;
  jfieldID descriptorFID;

  descriptorFID = JCL_CACHE_GET (env, FID_java_io_FileDescriptor_descriptor);
  if (NULL != descriptorFID)
    {
      return descriptorFID;
    }

  descriptorCLS = (*env)->FindClass (env, "java/io/FileDescriptor");
  if (NULL == descriptorCLS)
    {
      return NULL;
    }

  descriptorFID = (*env)->GetFieldID (env, descriptorCLS, "descriptor", "J");
  if (NULL == descriptorFID)
    {
      return NULL;
    }
  JCL_CACHE_SET (env, FID_java_io_FileDescriptor_descriptor, descriptorFID);

  return descriptorFID;
}
