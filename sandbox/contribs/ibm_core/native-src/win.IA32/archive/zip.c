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
#include "jclprots.h"
#include "zip.h"

void zfree PROTOTYPE ((void *opaque, void *address));
void *zalloc PROTOTYPE ((void *opaque, U_32 items, U_32 size));

void throwNewOutOfMemoryError (JNIEnv * env, char *message);

/**
  * Throw java.lang.InternalError
  */
void
throwNewInternalError (JNIEnv * env, char *message)
{
  jclass exceptionClass = (*env)->FindClass(env, "java/lang/InternalError");
  if (0 == exceptionClass) { 
    /* Just return if we can't load the exception class. */
    return;
    }
  (*env)->ThrowNew(env, exceptionClass, message);
}

/**
  * Throw java.util.zip.ZipException with the message provided
  */
void
throwJavaZIOException (JNIEnv * env, char *message)
{
  jclass exceptionClass = (*env)->FindClass(env, "java/util/zip/ZipException");
  if (0 == exceptionClass) { 
    /* Just return if we can't load the exception class. */
    return;
    }
  (*env)->ThrowNew(env, exceptionClass, message);
}

jint JNICALL
Java_java_util_zip_ZipFile_openZipImpl (JNIEnv * env, jobject recv,
                                        jbyteArray zipName)
{
  VMI_ACCESS_FROM_ENV (env);
  PORT_ACCESS_FROM_ENV (env);

  I_32 retval;
  JCLZipFile *jclZipFile;
  JCLZipFileLink *zipfileHandles;
  jsize length;
  char pathCopy[HyMaxPath];
  HyZipCachePool *zipCachePool;

  jclZipFile = jclmem_allocate_memory (env, sizeof (*jclZipFile));
  if (!jclZipFile)
    return 3;

  length = (*env)->GetArrayLength (env, zipName);
  length = length < HyMaxPath - 1 ? length : HyMaxPath - 1;
  ((*env)->GetByteArrayRegion (env, zipName, 0, length, pathCopy));
  pathCopy[length++] = '\0';
  ioh_convertToPlatform (pathCopy);

  /* Open the zip file (caching will be managed automatically by zipsup) */
  zipCachePool = (*VMI)->GetZipCachePool (VMI);
  retval =
    zip_openZipFile (privatePortLibrary, pathCopy, &(jclZipFile->hyZipFile),
                     zipCachePool);

  if (retval)
    {
      jclmem_free_memory (env, jclZipFile);     /* free on fail */

      if (retval == ZIP_ERR_FILE_OPEN_ERROR)
        return 1;
      else
        return 2;
    }

  /* Add the zipFile we just allocated to the list of zip files -- we will free this on UnLoad if its not already
     free'd */
  zipfileHandles = JCL_CACHE_GET (env, zipfile_handles);
  jclZipFile->last = (JCLZipFile *) zipfileHandles;
  jclZipFile->next = zipfileHandles->next;
  if (zipfileHandles->next != NULL)
    zipfileHandles->next->last = jclZipFile;
  zipfileHandles->next = jclZipFile;

  (*env)->SetLongField (env, recv,
                        JCL_CACHE_GET (env,
                                       FID_java_util_zip_ZipFile_descriptor),
                        ((jlong) jclZipFile));
  return 0;
}

jobject JNICALL
Java_java_util_zip_ZipFile_getEntryImpl (JNIEnv * env, jobject recv,
                                         jlong zipPointer, jstring entryName)
{
  PORT_ACCESS_FROM_ENV (env);

  I_32 retval;
  HyZipFile *zipFile;
  HyZipEntry zipEntry;
  jobject java_ZipEntry, extra;
  jclass entryClass;
  jmethodID mid;
  const char *entryCopy;

  if ((JCLZipFile *) zipPointer == (void *) -1)
    {
      throwNewIllegalStateException (env, "");
      return NULL;
    }

  zipFile = &(((JCLZipFile *) zipPointer)->hyZipFile);
  entryCopy = (*env)->GetStringUTFChars (env, entryName, NULL);
  if (entryCopy == NULL)
    return (jobject) NULL;

  zip_initZipEntry (PORTLIB, &zipEntry);
  retval = zip_getZipEntry (PORTLIB, zipFile, &zipEntry, entryCopy, TRUE);
  (*env)->ReleaseStringUTFChars (env, entryName, entryCopy);
  if (retval)
    {
      zip_freeZipEntry (PORTLIB, &zipEntry);
      return (jobject) NULL;
    }

  extra = NULL;
  if (zipEntry.extraFieldLength > 0)
    {
      zip_getZipEntryExtraField (PORTLIB, zipFile, &zipEntry, NULL,
                                 zipEntry.extraFieldLength);
      if (zipEntry.extraField == NULL)
        {
          zip_freeZipEntry (PORTLIB, &zipEntry);
          return (jobject) NULL;
        }
      extra = ((*env)->NewByteArray (env, zipEntry.extraFieldLength));
      if (((*env)->ExceptionCheck (env)))
        {
          zip_freeZipEntry (PORTLIB, &zipEntry);
          return (jobject) NULL;
        }
      ((*env)->
       SetByteArrayRegion (env, extra, 0, zipEntry.extraFieldLength,
                           zipEntry.extraField));
    }

  entryClass = JCL_CACHE_GET (env, CLS_java_util_zip_ZipEntry);
  mid = JCL_CACHE_GET (env, MID_java_util_zip_ZipEntry_init);
  /* Build a new ZipEntry from the C struct */
  java_ZipEntry = ((*env)->NewObject (env, entryClass, mid, entryName, NULL,
                                      extra,
                                      (jlong) zipEntry.lastModTime,
                                      (jlong) zipEntry.uncompressedSize,
                                      (jlong) zipEntry.compressedSize,
                                      (jlong) zipEntry.crc32,
                                      zipEntry.compressionMethod,
                                      (jlong) zipEntry.lastModDate,
                                      (jlong) zipEntry.dataPointer));
  zip_freeZipEntry (PORTLIB, &zipEntry);
  return java_ZipEntry;
}

void JNICALL
Java_java_util_zip_ZipFile_closeZipImpl (JNIEnv * env, jobject recv)
{
  PORT_ACCESS_FROM_ENV (env);

  I_32 retval = 0;
  JCLZipFile *jclZipFile;
  jfieldID descriptorFID =
    JCL_CACHE_GET (env, FID_java_util_zip_ZipFile_descriptor);

  jclZipFile = (JCLZipFile *) (*env)->GetLongField (env, recv, descriptorFID);
  if (jclZipFile != (void *) -1)
    {
      retval =
        zip_closeZipFile (privatePortLibrary, &(jclZipFile->hyZipFile));
      (*env)->SetLongField (env, recv, descriptorFID, -1);

      /* Free the zip struct */
      if (jclZipFile->last != NULL)
        jclZipFile->last->next = jclZipFile->next;
      if (jclZipFile->next != NULL)
        jclZipFile->next->last = jclZipFile->last;

      jclmem_free_memory (env, jclZipFile);
      if (retval)
        {
          throwJavaZIOException (env, "");
          return;
        }
    }
}

/**
  * Throw java.lang.IllegalStateException
  */
void
throwNewIllegalStateException (JNIEnv * env, char *message)
{
  jclass exceptionClass = (*env)->FindClass(env, "java/lang/IllegalStateException");
  if (0 == exceptionClass) { 
    /* Just return if we can't load the exception class. */
    return;
    }
  (*env)->ThrowNew(env, exceptionClass, message);
}

/**
  * Throw java.lang.IllegalArgumentException
  */
void
throwNewIllegalArgumentException (JNIEnv * env, char *message)
{
  jclass exceptionClass = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
  if (0 == exceptionClass) { 
    /* Just return if we can't load the exception class. */
    return;
    }
  (*env)->ThrowNew(env, exceptionClass, message);
}

void JNICALL
Java_java_util_zip_ZipFile_ntvinit (JNIEnv * env, jclass cls)
{
  PORT_ACCESS_FROM_ENV (env);
  jmethodID mid;
  jfieldID descriptorFID;
  jclass javaClass;
  JCLZipFileLink *zipfileHandles;

  javaClass = (*env)->FindClass (env, "java/util/zip/ZipEntry");
  javaClass = (*env)->NewWeakGlobalRef (env, javaClass);
  if (!javaClass)
    return;
  mid =
    ((*env)->
     GetMethodID (env, javaClass, "<init>",
                  "(Ljava/lang/String;Ljava/lang/String;[BJJJJIJJ)V"));
  if (!mid)
    return;
  JCL_CACHE_SET (env, CLS_java_util_zip_ZipEntry, javaClass);
  JCL_CACHE_SET (env, MID_java_util_zip_ZipEntry_init, mid);

  descriptorFID = (*env)->GetFieldID (env, cls, "descriptor", "J");
  if (!descriptorFID)
    return;
  JCL_CACHE_SET (env, FID_java_util_zip_ZipFile_descriptor, descriptorFID);

  javaClass = (*env)->FindClass (env, "java/util/zip/ZipFile$ZFEnum");
  if (!javaClass)
    return;
  descriptorFID =
    (*env)->GetFieldID (env, javaClass, "nextEntryPointer", "J");
  if (!descriptorFID)
    return;
  JCL_CACHE_SET (env, FID_java_util_zip_ZipFile_nextEntryPointer,
                 descriptorFID);

  zipfileHandles = jclmem_allocate_memory (env, sizeof (JCLZipFileLink));
  if (!zipfileHandles)
    return;
  zipfileHandles->last = NULL;
  zipfileHandles->next = NULL;
  JCL_CACHE_SET (env, zipfile_handles, zipfileHandles);
}

jlong JNICALL
Java_java_util_zip_ZipFile_00024ZFEnum_resetZip (JNIEnv * env, jobject recv,
                                                 jlong descriptor)
{
  PORT_ACCESS_FROM_ENV (env);

  IDATA nextEntryPointer;

  if ((JCLZipFile *) descriptor == (void *) -1)
    {
      throwNewIllegalStateException (env, "");
      return 0;
    }
  zip_resetZipFile (privatePortLibrary,
                    &(((JCLZipFile *) descriptor)->hyZipFile),
                    &nextEntryPointer);
  return nextEntryPointer;
}

jobject JNICALL
Java_java_util_zip_ZipFile_00024ZFEnum_getNextEntry (JNIEnv * env,
                                                     jobject recv,
                                                     jlong descriptor,
                                                     jlong nextEntry)
{
  PORT_ACCESS_FROM_ENV (env);

  I_32 retval;
  HyZipFile *zipFile;
  HyZipEntry zipEntry;
  jobject java_ZipEntry, extra;
  jclass javaClass;
  jmethodID mid;
  jstring entryName = NULL;
  IDATA nextEntryPointer;

  if ((JCLZipFile *) descriptor == (void *) -1)
    {
      throwNewIllegalStateException (env, "");
      return NULL;
    }
  zipFile = &(((JCLZipFile *) descriptor)->hyZipFile);
  zip_initZipEntry (PORTLIB, &zipEntry);

  nextEntryPointer = (IDATA) nextEntry;
  retval =
    zip_getNextZipEntry (PORTLIB, zipFile, &zipEntry, &nextEntryPointer);
  if (retval)
    {
      if (retval != ZIP_ERR_NO_MORE_ENTRIES)
        {
          char buf[40];
          sprintf (buf, "Error %d getting next zip entry", retval);
          throwNewInternalError (env, buf);
        }
      return (jobject) NULL;
    }

  /* Build a new ZipEntry from the C struct */
  entryName = ((*env)->NewStringUTF (env, zipEntry.filename));

  if (((*env)->ExceptionCheck (env)))
    return NULL;

  extra = NULL;
  if (zipEntry.extraFieldLength > 0)
    {
      zip_getZipEntryExtraField (PORTLIB, zipFile, &zipEntry, NULL,
                                 zipEntry.extraFieldLength);
      extra = ((*env)->NewByteArray (env, zipEntry.extraFieldLength));
      if (((*env)->ExceptionCheck (env)))
        {
          /* free the extraField entry */
          zip_freeZipEntry (PORTLIB, &zipEntry);
          return NULL;
        }
      ((*env)->
       SetByteArrayRegion (env, extra, 0, zipEntry.extraFieldLength,
                           zipEntry.extraField));
      jclmem_free_memory (env, zipEntry.extraField);
      zipEntry.extraField = NULL;
    }

  javaClass = JCL_CACHE_GET (env, CLS_java_util_zip_ZipEntry);
  mid = JCL_CACHE_GET (env, MID_java_util_zip_ZipEntry_init);
  java_ZipEntry = ((*env)->NewObject (env, javaClass, mid, entryName, NULL,     /* comment */
                                      extra,
                                      (jlong) zipEntry.lastModTime,
                                      (jlong) zipEntry.uncompressedSize,
                                      (jlong) zipEntry.compressedSize,
                                      (jlong) zipEntry.crc32,
                                      zipEntry.compressionMethod,
                                      (jlong) zipEntry.lastModDate,
                                      (jlong) zipEntry.dataPointer));
  zip_freeZipEntry (PORTLIB, &zipEntry);
  (*env)->SetLongField (env, recv,
                        JCL_CACHE_GET (env,
                                       FID_java_util_zip_ZipFile_nextEntryPointer),
                        nextEntryPointer);
  return java_ZipEntry;
}

jbyteArray JNICALL
Java_java_util_zip_ZipFile_inflateEntryImpl2 (JNIEnv * env, jobject recv,
                                              jlong zipPointer,
                                              jstring entryName)
{
  PORT_ACCESS_FROM_ENV (env);

  I_32 retval;
  HyZipFile *zipFile;
  HyZipEntry zipEntry;
  const char *entryCopy;
  jbyteArray buf;

  /* Build the zipFile */
  if ((JCLZipFile *) zipPointer == (void *) -1)
    {
      throwNewIllegalStateException (env, "");
      return NULL;
    }
  zipFile = &(((JCLZipFile *) zipPointer)->hyZipFile);
  entryCopy = (*env)->GetStringUTFChars (env, entryName, NULL);
  if (entryCopy == NULL)
    return NULL;

  zip_initZipEntry (privatePortLibrary, &zipEntry);
  retval =
    zip_getZipEntry (privatePortLibrary, zipFile, &zipEntry, entryCopy, TRUE);
  (*env)->ReleaseStringUTFChars (env, entryName, entryCopy);
  if (retval)
    {
      zip_freeZipEntry (privatePortLibrary, &zipEntry);
      if (retval == ZIP_ERR_OUT_OF_MEMORY)
        throwNewOutOfMemoryError (env, "");
      return NULL;
    }

  buf = (*env)->NewByteArray (env, zipEntry.uncompressedSize);
  if (!buf)
    {
      throwNewOutOfMemoryError (env, "");
      return NULL;
    }

  retval =
    zip_getZipEntryData (privatePortLibrary, zipFile, &zipEntry, NULL,
                         zipEntry.uncompressedSize);
  if (retval == 0)
    (*env)->SetByteArrayRegion (env, buf, 0, zipEntry.uncompressedSize,
                                zipEntry.data);
  zip_freeZipEntry (privatePortLibrary, &zipEntry);
  if (!retval)
    return buf;

  if (retval == ZIP_ERR_OUT_OF_MEMORY)
    throwNewOutOfMemoryError (env, "");
  else
    throwJavaZIOException (env, "");

  return NULL;
}
