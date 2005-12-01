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
#include <ctype.h>
#include "iohelp.h"
#include "jclglob.h"
#include "helpers.h"
#include "jclprots.h"

jboolean JNICALL
Java_java_io_File_deleteFileImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 result;
  jsize length = (*env)->GetArrayLength (env, path);
  char pathCopy[HyMaxPath];
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);
  result = hyfile_unlink (pathCopy);
  return result == 0;
}

jboolean JNICALL
Java_java_io_File_deleteDirImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 result;
  jsize length = (*env)->GetArrayLength (env, path);
  char pathCopy[HyMaxPath];
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);
  result = hyfile_unlinkdir (pathCopy);
  return result == 0;
}

jobject JNICALL
Java_java_io_File_listImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  struct dirEntry
  {
    char pathEntry[HyMaxPath];
    struct dirEntry *next;
  } *dirList, *currentEntry;

  PORT_ACCESS_FROM_ENV (env);
  jsize length = (*env)->GetArrayLength (env, path);
  char pathCopy[HyMaxPath];
  char filename[HyMaxPath];
  I_32 result = 0, index;
  I_32 numEntries = 0;
  UDATA findhandle;
  jarray answer = NULL;

  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  if (length >= 1 && pathCopy[length - 1] != '\\'
      && pathCopy[length - 1] != '/')
    {
      pathCopy[length] = jclSeparator;
      length++;
    }
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);
  findhandle = hyfile_findfirst (pathCopy, filename);
  if (findhandle == (UDATA) - 1)
    return NULL;

  while (result > -1)
    {
      if (strcmp (".", filename) != 0 && (strcmp ("..", filename) != 0))
        {
          if (numEntries > 0)
            {
              currentEntry->next =
                (struct dirEntry *) jclmem_allocate_memory (env,
                                                            sizeof (struct
                                                                    dirEntry));
              currentEntry = currentEntry->next;
            }
          else
            {
              dirList =
                (struct dirEntry *) jclmem_allocate_memory (env,
                                                            sizeof (struct
                                                                    dirEntry));
              currentEntry = dirList;
            }
          if (currentEntry == NULL)
            {
              hyfile_findclose (findhandle);
              throwNewOutOfMemoryError (env, "");
              goto cleanup;
            }
          strcpy (currentEntry->pathEntry, filename);
          numEntries++;
        }
      result = hyfile_findnext (findhandle, filename);
    }
  hyfile_findclose (findhandle);

  if (numEntries == 0)
    return NULL;

  answer =
    (*env)->NewObjectArray (env, numEntries,
                            JCL_CACHE_GET (env, CLS_array_of_byte), NULL);
cleanup:
  for (index = 0; index < numEntries; index++)
    {
      jbyteArray entrypath;
      jsize entrylen = strlen (dirList->pathEntry);
      currentEntry = dirList;
      if (answer)
        {
          entrypath = (*env)->NewByteArray (env, entrylen);
          (*env)->SetByteArrayRegion (env, entrypath, 0, entrylen,
                                      (jbyte *) dirList->pathEntry);
          (*env)->SetObjectArrayElement (env, answer, index, entrypath);
          (*env)->DeleteLocalRef (env, entrypath);
        }
      dirList = dirList->next;
      jclmem_free_memory (env, currentEntry);
    }
  return answer;
}

jboolean JNICALL
Java_java_io_File_isDirectoryImpl (JNIEnv * env, jobject recv,
                                   jbyteArray path)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 result;
  jsize length = (*env)->GetArrayLength (env, path);
  char pathCopy[HyMaxPath];
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);
  result = hyfile_attr (pathCopy);
  return result == HyIsDir;
}

jboolean JNICALL
Java_java_io_File_existsImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 result;
  char pathCopy[HyMaxPath];
  jsize length = (*env)->GetArrayLength (env, path);
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);
  result = hyfile_attr (pathCopy);
  return result >= 0;
}

jboolean JNICALL
Java_java_io_File_isFileImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 result;
  jsize length = (*env)->GetArrayLength (env, path);
  char pathCopy[HyMaxPath];
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);
  result = hyfile_attr (pathCopy);
  return result == HyIsFile;
}

jlong JNICALL
Java_java_io_File_lastModifiedImpl (JNIEnv * env, jobject recv,
                                    jbyteArray path)
{
  PORT_ACCESS_FROM_ENV (env);
  I_64 result;
  jsize length = (*env)->GetArrayLength (env, path);
  char pathCopy[HyMaxPath];
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);
  result = hyfile_lastmod (pathCopy);
  return result;
}

jlong JNICALL
Java_java_io_File_lengthImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  PORT_ACCESS_FROM_ENV (env);
  I_64 result;
  jsize length = (*env)->GetArrayLength (env, path);
  char pathCopy[HyMaxPath];

  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);
  result = hyfile_length (pathCopy);
  if (result < 0)
    {
      return 0L;
    }
  return result;
}

jboolean JNICALL
Java_java_io_File_isAbsoluteImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  I_32 result = 0;
  jsize length = (*env)->GetArrayLength (env, path);
  jbyte *lpath = (jbyte *) ((*env)->GetPrimitiveArrayCritical (env, path, 0));

  if (jclSeparator == '/' && length > 0)
    {
      result = (lpath[0] == jclSeparator);
      goto release;
    }
  if (length > 1 && lpath[0] == '\\' && lpath[1] == '\\')
    {
      result = 1;
      goto release;
    }
  if (length > 2)
    {
      if (isalpha (lpath[0]) && lpath[1] == ':'
          && (lpath[2] == '\\' || lpath[2] == '/'))
        result = 1;
    }

release:
  /* Easier to release in one area than copy the code around */
  (*env)->ReleasePrimitiveArrayCritical (env, path, lpath, JNI_ABORT);
  return result;
}

jboolean JNICALL
Java_java_io_File_mkdirImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 result;
  jsize length = (*env)->GetArrayLength (env, path);
  char pathCopy[HyMaxPath];
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);
  result = hyfile_mkdir (pathCopy);
  return result == 0;
}

jboolean JNICALL
Java_java_io_File_renameToImpl (JNIEnv * env, jobject recv,
                                jbyteArray pathExist, jbyteArray pathNew)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 result;
  jsize length;
  char pathExistCopy[HyMaxPath], pathNewCopy[HyMaxPath];
  length = (*env)->GetArrayLength (env, pathExist);
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, pathExist, 0, length, pathExistCopy));
  pathExistCopy[length] = '\0';
  length = (*env)->GetArrayLength (env, pathNew);
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, pathNew, 0, length, pathNewCopy));
  pathNewCopy[length] = '\0';
  ioh_convertToPlatform (pathExistCopy);
  ioh_convertToPlatform (pathNewCopy);
  result = hyfile_move (pathExistCopy, pathNewCopy);
  return result == 0;
}

jobject JNICALL
Java_java_io_File_getCanonImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  /* This needs work.  Currently is does no more or less than VAJ-20 ST implementation
   * but really should figure out '..', '.', and really resolve references.
   */
  jbyteArray answer;
  jsize answerlen;
  char pathCopy[HyMaxPath];
  U_32 length = (U_32) (*env)->GetArrayLength (env, path);
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  (*env)->GetByteArrayRegion (env, path, 0, length, pathCopy);
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);
#if defined(WIN32)
  platformCanonicalPath (pathCopy);
#endif

  answerlen = strlen (pathCopy);
  answer = (*env)->NewByteArray (env, answerlen);
  (*env)->SetByteArrayRegion (env, answer, 0, answerlen, (jbyte *) pathCopy);
  return answer;
}

jint JNICALL
Java_java_io_File_newFileImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 result;
  IDATA portFD;
  jsize length = (*env)->GetArrayLength (env, path);
  char pathCopy[HyMaxPath];
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);

  /* First check to see if file already exists */
  result = hyfile_attr (pathCopy);
  if (result == HyIsDir)
    return 3;
  if (result >= 0)
    return 1;

  /* Now create the file and close it */
  portFD =
    hyfile_open (pathCopy, HyOpenCreate | HyOpenWrite | HyOpenTruncate, 0666);
  if (portFD == -1)
    return 2;
  hyfile_close (portFD);
  return 0;
}

jobject JNICALL
Java_java_io_File_rootsImpl (JNIEnv * env, jclass clazz)
{
  char rootStrings[HyMaxPath], *rootCopy;
  I_32 numRoots;
  I_32 index = 0;
  jarray answer;

   /**
	 * It is the responsibility of #getPlatformRoots to return a char array
	 * with volume names separated by null with a trailing extra null, so for
	 * Unix it should be '\<null><null>' .
	 */
  numRoots = getPlatformRoots (rootStrings);
  if (numRoots == 0)
    return NULL;
  rootCopy = rootStrings;

  answer =
    (*env)->NewObjectArray (env, numRoots,
                            JCL_CACHE_GET (env, CLS_array_of_byte), NULL);
  if (!answer)
    {
      return NULL;
    }
  while (TRUE)
    {
      jbyteArray rootname;
      jsize entrylen = strlen (rootCopy);
      /* Have we hit the second null? */
      if (entrylen == 0)
        break;
      rootname = (*env)->NewByteArray (env, entrylen);
      (*env)->SetByteArrayRegion (env, rootname, 0, entrylen,
                                  (jbyte *) rootCopy);
      (*env)->SetObjectArrayElement (env, answer, index++, rootname);
      (*env)->DeleteLocalRef (env, rootname);
      rootCopy = rootCopy + entrylen + 1;
    }
  return answer;
}

jboolean JNICALL
Java_java_io_File_isHiddenImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  I_32 result;
  char pathCopy[HyMaxPath];
  jsize length = (*env)->GetArrayLength (env, path);
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);
  result = getPlatformIsHidden (env, pathCopy);
  return result;
}

jboolean JNICALL
Java_java_io_File_setLastModifiedImpl (JNIEnv * env, jobject recv,
                                       jbyteArray path, jlong time)
{
  PORT_ACCESS_FROM_ENV (env);
  I_32 result;
  jsize length = (*env)->GetArrayLength (env, path);
  char pathCopy[HyMaxPath];
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);

  result = setPlatformLastModified (env, pathCopy, (I_64) time);

  return result;
}

jboolean JNICALL
Java_java_io_File_setReadOnlyImpl (JNIEnv * env, jobject recv,
                                   jbyteArray path)
{
  PORT_ACCESS_FROM_ENV (env);
  jsize length = (*env)->GetArrayLength (env, path);
  char pathCopy[HyMaxPath];
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);
  return setPlatformReadOnly (env, pathCopy);
}

void JNICALL
Java_java_io_File_oneTimeInitialization (JNIEnv * env, jclass clazz)
{
  jclass arrayClass = (*env)->FindClass (env, "[B");
  if (arrayClass)
    {
      jobject globalRef = (*env)->NewWeakGlobalRef (env, arrayClass);
      if (globalRef)
        JCL_CACHE_SET (env, CLS_array_of_byte, globalRef);
    }
  return;
}

jbyteArray JNICALL
Java_java_io_File_properPathImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  return getPlatformPath (env, path);
}

jboolean JNICALL
Java_java_io_File_isReadOnlyImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  I_32 result;
  char pathCopy[HyMaxPath];
  jsize length = (*env)->GetArrayLength (env, path);
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);
  result = getPlatformIsReadOnly (env, pathCopy);
  return result;
}

jboolean JNICALL
Java_java_io_File_isWriteOnlyImpl (JNIEnv * env, jobject recv,
                                   jbyteArray path)
{
  I_32 result;
  char pathCopy[HyMaxPath];
  jsize length = (*env)->GetArrayLength (env, path);
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, path, 0, length, pathCopy));
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);
  result = getPlatformIsWriteOnly (env, pathCopy);
  return result;
}

jobject JNICALL
Java_java_io_File_getLinkImpl (JNIEnv * env, jobject recv, jbyteArray path)
{
  jbyteArray answer;
  jsize answerlen;
  char pathCopy[HyMaxPath];
  U_32 length = (U_32) (*env)->GetArrayLength (env, path);
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  (*env)->GetByteArrayRegion (env, path, 0, length, pathCopy);
  pathCopy[length] = '\0';
  ioh_convertToPlatform (pathCopy);
  if (platformReadLink (pathCopy))
    {
      answerlen = strlen (pathCopy);
      answer = (*env)->NewByteArray (env, answerlen);
      (*env)->SetByteArrayRegion (env, answer, 0, answerlen,
                                  (jbyte *) pathCopy);
    }
  else
    {
      answer = path;
    }
  return answer;
}

jboolean JNICALL
Java_java_io_File_isCaseSensitiveImpl (JNIEnv * env, jclass clazz)
{
/* Assume all other platforms ARE case sensitive and add to this list when they prove otherwise */
#if (defined(WIN32))
  return FALSE;
#else
  return TRUE;
#endif

}
