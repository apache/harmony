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

jboolean JNICALL
Java_java_io_FileDescriptor_valid (JNIEnv * env, jobject recv)
{
  /* Currently only answer false if the descriptor is -1.  Possibly there * could be an OS check to see if the handle 
     has been invalidated */
  void *descriptor = getJavaIoFileDescriptorContentsAsPointer (env, recv);
  return (IDATA) descriptor != -1;
}

void JNICALL
Java_java_io_FileDescriptor_sync (JNIEnv * env, jobject recv)
{
  /* Cause all unwritten data to be written out to the OS */
  IDATA descriptor;
  I_32 syncfailed = 0;
  PORT_ACCESS_FROM_ENV (env);

  descriptor = (IDATA) getJavaIoFileDescriptorContentsAsPointer (env, recv);
  if (descriptor == -1)
    {
      syncfailed = 1;
    }
  if (!syncfailed && (descriptor > 2))
    {
      /* Don't attempt to sync stdin, out, or err */
      syncfailed = hyfile_sync (descriptor) != 0;
    }
  if (syncfailed)
    {
      /* Find and throw SyncFailedException */
      jclass exceptionClass = (*env)->FindClass(env, "java/io/SyncFailedException");
      if (0 == exceptionClass) { 
        /* Just return if we can't load the exception class. */
        return;
        }
      (*env)->ThrowNew(env, exceptionClass, "Failed to Sync File");
      return;
    }
}

void JNICALL
Java_java_io_FileDescriptor_oneTimeInitialization (JNIEnv * env,
               jclass fdClazz)
{
  jfieldID descriptorFID =
    (*env)->GetFieldID (env, fdClazz, "descriptor", "J");
  if (!descriptorFID)
    return;
  JCL_CACHE_SET (env, FID_java_io_FileDescriptor_descriptor, descriptorFID);
}
