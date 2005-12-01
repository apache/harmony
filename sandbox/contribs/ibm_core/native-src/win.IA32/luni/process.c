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
#include "procimpl.h"

#include "jclglob.h"

/* Create a System Process with the specified */
/* environment and arguments */
jlongArray JNICALL
Java_com_ibm_oti_lang_SystemProcess_createImpl (JNIEnv * env, jclass clazz,
            jobject recv,
            jobjectArray arg1,
            jobjectArray arg2,
            jbyteArray dir)
{
  jbyteArray envString;
  jlongArray pVals = NULL;
  jlong npVals[4];
  char *envArray[256];
  char *command[256];
  int i, retVal;
  IDATA pHandle, inHandle, outHandle, errHandle;
  int envLength, commandLineLength, len;
  char *workingDir = NULL;
  PORT_ACCESS_FROM_ENV (env);

  /* validate sizes */
  commandLineLength = (*env)->GetArrayLength (env, arg1);
  envLength = (*env)->GetArrayLength (env, arg2);
  if (commandLineLength >= 255)
    {
      jclass exClass = (*env)->FindClass (env, "java/io/IOException");
      (*env)->ThrowNew (env, exClass, "Too many arguments");
      return NULL;
    }
  if (envLength >= 255)
    {
      jclass exClass = (*env)->FindClass (env, "java/io/IOException");
      (*env)->ThrowNew (env, exClass, "Too many environment arguments");
      return NULL;
    }

  memset (command, 0, sizeof (command));
  memset (envArray, 0, sizeof (envArray));

  /* Get the command string and arguments */
  /* convert java.lang.String into C char* */
  for (i = commandLineLength; --i >= 0;)
    {
      jbyteArray element = (*env)->GetObjectArrayElement (env, arg1, i);
      len = (*env)->GetArrayLength (env, element);
      command[i] = jclmem_allocate_memory (env, len + 1);
      if (command[i] == NULL)
        {
          throwNewOutOfMemoryError (env, "");
          goto failed;
        }
      (*env)->GetByteArrayRegion (env, element, 0, len, command[i]);
      command[i][len] = 0;
    }
  if (envLength)
    for (i = 0; i < envLength; i++)
      {
        envString = (*env)->GetObjectArrayElement (env, arg2, i);
        len = (*env)->GetArrayLength (env, envString);
        envArray[i] = jclmem_allocate_memory (env, len + 1);
        if (envArray[i] == NULL)
          {
            throwNewOutOfMemoryError (env, "");
            goto failed;
          }
        (*env)->GetByteArrayRegion (env, envString, 0, len, envArray[i]);
        envArray[i][len] = 0;
      }
  /* NULL terminate for UNIX (does work on windows too; in fact, it doesn't care) */
  command[commandLineLength] = NULL;
  envArray[envLength] = NULL;

  if (dir != NULL)
    {
      jsize dirLength = (*env)->GetArrayLength (env, dir);

      workingDir = jclmem_allocate_memory (env, dirLength + 1);
      if (workingDir)
        {
          (*env)->GetByteArrayRegion (env, dir, 0, dirLength,
            (jbyte *) workingDir);
          workingDir[dirLength] = '\0';
        }
    }

  retVal = execProgram (env, recv,
      command, commandLineLength, envArray, envLength,
      workingDir, &pHandle, &inHandle, &outHandle,
      &errHandle);

  if (workingDir)
    {
      jclmem_free_memory (env, workingDir);
    }

  if (!retVal)
    {
      /* Failed to exec program */
      jclass exClass = (*env)->FindClass (env, "java/io/IOException");
      (*env)->ThrowNew (env, exClass, "Unable to start program");
      goto failed;
    }
  pVals = (*env)->NewLongArray (env, 4);
  if (pVals)
    {
      npVals[0] = (jlong) pHandle;
      npVals[1] = (jlong) inHandle;
      npVals[2] = (jlong) outHandle;
      npVals[3] = (jlong) errHandle;
      (*env)->SetLongArrayRegion (env, pVals, 0, 4, (jlong *) (&npVals));
    }

failed:

  for (i = 0; i < envLength; i++)
    {
      if (envArray[i])
        jclmem_free_memory (env, envArray[i]);
    }
  for (i = commandLineLength; --i >= 0;)
    {
      if (command[i])
        jclmem_free_memory (env, command[i]);
    }

  return pVals;
}

/* Kill the receiver */
void JNICALL
Java_com_ibm_oti_lang_SystemProcess_destroyImpl (JNIEnv * env, jobject recv)
{
  jlong pHandle;
  pHandle =
    (*env)->GetLongField (env, recv,
        JCL_CACHE_GET (env,
           FID_com_ibm_oti_lang_SystemProcess_handle));
  termProc ((IDATA) pHandle);
}

/* Close the input stream*/
void JNICALL
Java_com_ibm_oti_lang_ProcessInputStream_closeImpl (JNIEnv * env,
                jobject recv)
{
  PORT_ACCESS_FROM_ENV (env);

  new_ioh_close (env, recv,
     JCL_CACHE_GET (env,
        FID_com_ibm_oti_lang_ProcessInputStream_fd));
}

void JNICALL
Java_com_ibm_oti_lang_ProcessOutputStream_closeImpl (JNIEnv * env,
                 jobject recv)
{
  PORT_ACCESS_FROM_ENV (env);
  new_ioh_close (env, recv,
     JCL_CACHE_GET (env,
        FID_com_ibm_oti_lang_ProcessOutputStream_fd));
}

/* Read nbytes from the receiver */
jint JNICALL
Java_com_ibm_oti_lang_ProcessInputStream_readImpl (JNIEnv * env, jobject recv,
               jbyteArray buffer,
               jint offset, jint nbytes,
               jlong handle)
{

  return (jint) ioh_readbytesImpl (env, recv, buffer, offset, nbytes,
           (IDATA) handle);

}

/* Return the number of byes available to be read without blocking */
jint JNICALL
Java_com_ibm_oti_lang_ProcessInputStream_availableImpl (JNIEnv * env,
              jobject recv)
{
  jlong sHandle;
  int retVal;

  sHandle =
    (*env)->GetLongField (env, recv,
        JCL_CACHE_GET (env,
           FID_com_ibm_oti_lang_ProcessInputStream_handle));
  retVal = getAvailable ((jint)sHandle);
  if (retVal < 0)
    {
      /* Couldn't read bytes */
      jclass exClass = (*env)->FindClass (env, "java/io/IOException");
      (*env)->ThrowNew (env, exClass, "Unable to peek on stream");
    }
  return (jint) retVal;
}

/* Write nbytes to the receiver */
void JNICALL
Java_com_ibm_oti_lang_ProcessOutputStream_writeImpl (JNIEnv * env,
                 jobject recv,
                 jbyteArray buffer,
                 jint offset, jint nbytes,
                 jlong handle)
{

  ioh_writebytesImpl (env, recv, buffer, offset, nbytes, (IDATA) handle);

}

/* Set the descriptor field od the receiver */
void JNICALL
Java_com_ibm_oti_lang_ProcessInputStream_setFDImpl (JNIEnv * env,
                jobject recv,
                jobject arg1, jlong arg2)
{

  setJavaIoFileDescriptorContentsAsPointer (env, arg1, (void *) arg2);
}

void JNICALL
Java_com_ibm_oti_lang_ProcessOutputStream_setFDImpl (JNIEnv * env,
                 jobject recv,
                 jobject arg1, jlong arg2)
{

  setJavaIoFileDescriptorContentsAsPointer (env, arg1, (void *) arg2);
}

/* Wait for the receiver to finish then return the exit value */
jint JNICALL
Java_com_ibm_oti_lang_SystemProcess_waitForCompletionImpl (JNIEnv * env,
                 jobject recv)
{
  jlong pHandle;
  pHandle =
    (*env)->GetLongField (env, recv,
        JCL_CACHE_GET (env,
           FID_com_ibm_oti_lang_SystemProcess_handle));
  return (jint) waitForProc ((IDATA) pHandle);
}

void JNICALL
Java_com_ibm_oti_lang_SystemProcess_oneTimeInitialization (JNIEnv * env,
                 jclass clazz)
{
  jfieldID fid = (*env)->GetFieldID (env, clazz, "handle", "J");
  if (!fid)
    return;
  JCL_CACHE_SET (env, FID_com_ibm_oti_lang_SystemProcess_handle, fid);
}

void JNICALL
Java_com_ibm_oti_lang_ProcessOutputStream_oneTimeInitialization (JNIEnv * env,
                 jclass clazz)
{
  jfieldID fid;

  fid = (*env)->GetFieldID (env, clazz, "handle", "J");
  if (!fid)
    return;
  JCL_CACHE_SET (env, FID_com_ibm_oti_lang_ProcessOutputStream_handle, fid);

  fid = (*env)->GetFieldID (env, clazz, "fd", "Ljava/io/FileDescriptor;");
  if (!fid)
    return;
  JCL_CACHE_SET (env, FID_com_ibm_oti_lang_ProcessOutputStream_fd, fid);
}

void JNICALL
Java_com_ibm_oti_lang_ProcessInputStream_oneTimeInitialization (JNIEnv * env,
                jclass clazz)
{
  jfieldID fid;

  fid = (*env)->GetFieldID (env, clazz, "handle", "J");
  if (!fid)
    return;
  JCL_CACHE_SET (env, FID_com_ibm_oti_lang_ProcessInputStream_handle, fid);

  fid = (*env)->GetFieldID (env, clazz, "fd", "Ljava/io/FileDescriptor;");
  if (!fid)
    return;
  JCL_CACHE_SET (env, FID_com_ibm_oti_lang_ProcessInputStream_fd, fid);
}

/* Close the handle */
void JNICALL
Java_com_ibm_oti_lang_SystemProcess_closeImpl (JNIEnv * env, jobject recv)
{
  jlong pHandle;
  pHandle =
    (*env)->GetLongField (env, recv,
        JCL_CACHE_GET (env,
           FID_com_ibm_oti_lang_SystemProcess_handle));
  closeProc ((IDATA) pHandle);
}
