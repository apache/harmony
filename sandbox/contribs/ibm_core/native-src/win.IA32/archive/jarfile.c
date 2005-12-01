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

#include "jcl.h"
#include "iohelp.h"
#include "jclglob.h"
#include "jclprots.h"
#include "zipsup.h"

/* Build a new ZipEntry from the C struct */
jobject
createZipEntry (JNIEnv * env, HyZipFile * zipFile, HyZipEntry * zipEntry)
{
  PORT_ACCESS_FROM_ENV (env);
  jclass javaClass;
  jobject java_ZipEntry, extra, entryName;
  jmethodID mid;

  /* Build a new ZipEntry from the C struct */
  entryName = ((*env)->NewStringUTF (env, zipEntry->filename));
  if (((*env)->ExceptionCheck (env)))
    return NULL;

  extra = NULL;
  if (zipEntry->extraFieldLength > 0)
    {
      zip_getZipEntryExtraField (PORTLIB, zipFile, zipEntry, NULL,
                                 zipEntry->extraFieldLength);
      if (zipEntry->extraField == NULL)
        return NULL;
      extra = ((*env)->NewByteArray (env, zipEntry->extraFieldLength));
      if (((*env)->ExceptionCheck (env)))
        return NULL;
      ((*env)->
       SetByteArrayRegion (env, extra, 0, zipEntry->extraFieldLength,
                           zipEntry->extraField));
      jclmem_free_memory (env, zipEntry->extraField);
      zipEntry->extraField = NULL;
    }

  javaClass = JCL_CACHE_GET (env, CLS_java_util_zip_ZipEntry);
  mid = JCL_CACHE_GET (env, MID_java_util_zip_ZipEntry_init);
  java_ZipEntry = ((*env)->NewObject (env, javaClass, mid, entryName, NULL,
                                      extra,
                                      (jlong) zipEntry->lastModTime,
                                      (jlong) zipEntry->uncompressedSize,
                                      (jlong) zipEntry->compressedSize,
                                      (jlong) zipEntry->crc32,
                                      zipEntry->compressionMethod,
                                      (jlong) zipEntry->lastModDate,
                                      (jlong) zipEntry->dataPointer));
  return java_ZipEntry;
}

jarray JNICALL
Java_java_util_jar_JarFile_getMetaEntriesImpl (JNIEnv * env, jobject recv,
                                               jbyteArray zipName)
{
#define MAX_PATH	1024
#define RESULT_BUF_SIZE 256

  PORT_ACCESS_FROM_ENV (env);

  JCLZipFile *jclZipFile;
  HyZipFile *zipFile;
  HyZipEntry zipEntry;
  jobject current;
  jclass javaClass;
  jobject resultArray[RESULT_BUF_SIZE];
  UDATA resultCount = 0, offset, i;
  void *scanPtr;
  char metaInfName[10];         /* 10 == strlen("META-INF/") + 1 */
  const UDATA metaInfSize = 10; /* 10 == strlen("META-INF/") + 1 */
  jobjectArray result = NULL;
  char *nameBuf, *newNameBuf, *oldNameBuf = NULL;
  char startNameBuf[MAX_PATH];
  UDATA nameBufSize = MAX_PATH;
  IDATA rc;

  nameBuf = (char *) &startNameBuf;

  jclZipFile =
    (JCLZipFile *) (*env)->GetLongField (env, recv,
                                         JCL_CACHE_GET (env,
                                                        FID_java_util_zip_ZipFile_descriptor));
  if (jclZipFile == (void *) -1)
    {
      throwNewIllegalStateException (env, "");
      return NULL;
    }
  zipFile = &(jclZipFile->hyZipFile);

  if (zipFile->cache)
    {
      if (zipCache_enumNew (zipFile->cache, "META-INF/", &scanPtr))
        return NULL;

      if (0 !=
          zipCache_enumGetDirName (scanPtr, (char *) &metaInfName,
                                   sizeof (metaInfName)))
        return NULL;

      for (;;)
        {
          rc = zipCache_enumElement (scanPtr, nameBuf, nameBufSize, &offset);
          if (rc < 0)
            {
              break;            /* we're done, leave the loop */
            }
          else if (rc > 0)
            {
              /* the buffer wasn't big enough, grow it */
              newNameBuf = jclmem_allocate_memory (env, rc);
              nameBufSize = rc;
              if (oldNameBuf)
                {
                  jclmem_free_memory (env, oldNameBuf); /* free old before checking result so we clean up on fail */
                  oldNameBuf = NULL;
                }
              if (!newNameBuf)
                goto cleanup;
              nameBuf = oldNameBuf = newNameBuf;
              continue;         /* go to the top of the loop again */
            }

          zip_initZipEntry (PORTLIB, &zipEntry);
          if (zip_getZipEntryFromOffset (PORTLIB, zipFile, &zipEntry, offset))
            goto cleanup;
          current = createZipEntry (env, zipFile, &zipEntry);
          zip_freeZipEntry (PORTLIB, &zipEntry);
          if (resultCount == RESULT_BUF_SIZE)
            goto cleanup;       /* fail - should fix. */
          if (current)
            resultArray[resultCount++] = current;
          else
            goto cleanup;
        }
      javaClass = JCL_CACHE_GET (env, CLS_java_util_zip_ZipEntry);
      result = ((*env)->NewObjectArray (env, resultCount, javaClass, NULL));
      if (((*env)->ExceptionCheck (env)))
        {
          result = NULL;
          goto cleanup;
        }
      for (i = 0; i < resultCount; i++)
        {
          (*env)->SetObjectArrayElement (env, result, i, resultArray[i]);
        }
    cleanup:
      zipCache_enumKill (scanPtr);
      if (oldNameBuf)
        jclmem_free_memory (env, oldNameBuf);   /* free old before checking result so we clean up on fail */
      return result;
    }
  return NULL;

#undef MAX_PATH
#undef RESULT_BUF_SIZE
}
