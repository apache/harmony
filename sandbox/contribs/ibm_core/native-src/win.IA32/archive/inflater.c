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
#include "inflater.h"

void throwNewDataFormatException (JNIEnv * env, char *message);

/* Create a new stream . This stream cannot be used until it has been properly initialized. */
jlong JNICALL
Java_java_util_zip_Inflater_createStream (JNIEnv * env, jobject recv,
                                          jboolean noHeader)
{
  PORT_ACCESS_FROM_ENV (env);

  JCLZipStream *jstream;
  z_stream *stream;
  int err = 0;
  int wbits = 15;               /*Use MAX for fastest */

  /*Allocate mem for wrapped struct */
  jstream = jclmem_allocate_memory (env, sizeof (JCLZipStream));
  if (jstream == NULL)
    {
      throwNewOutOfMemoryError (env, "");
      return -1;
    }
  /*Allocate the z_stream */
  stream = jclmem_allocate_memory (env, sizeof (z_stream));
  if (stream == NULL)
    {
      jclmem_free_memory (env, jstream);
      throwNewOutOfMemoryError (env, "");
      return -1;
    }

  stream->opaque = (void *) privatePortLibrary;
  stream->zalloc = zalloc;
  stream->zfree = zfree;
  stream->adler = 1;
  jstream->stream = stream;
  jstream->dict = NULL;
  jstream->inaddr = NULL;

  /*Unable to find official doc that this is the way to avoid zlib header use. However doc in zipsup.c claims it is so. */
  if (noHeader)
    wbits = wbits / -1;
  err = inflateInit2 (stream, wbits);   /*Window bits to use. 15 is fastest but consumes the most memory */

  if (err != Z_OK)
    {
      jclmem_free_memory (env, stream);
      jclmem_free_memory (env, jstream);
      throwNewIllegalArgumentException (env, "");
      return -1;
    }

  return (jlong) jstream;
}

void JNICALL
Java_java_util_zip_Inflater_setInputImpl (JNIEnv * env, jobject recv,
                                          jbyteArray buf, jint off, jint len,
                                          jlong handle)
{
  PORT_ACCESS_FROM_ENV (env);

  jbyte *in;
  U_8 *baseAddr;
  JCLZipStream *stream;

  stream = (JCLZipStream *) handle;

  if (stream->inaddr != NULL)   /*Input has already been provided, free the old buffer */
    jclmem_free_memory (env, stream->inaddr);
  baseAddr = jclmem_allocate_memory (env, len);
  if (baseAddr == NULL)
    {
      throwNewOutOfMemoryError (env, "");
      return;
    }
  stream->inaddr = baseAddr;
  stream->stream->next_in = (Bytef *) baseAddr;
  stream->stream->avail_in = len;
  in = ((*env)->GetPrimitiveArrayCritical (env, buf, 0));
  if (in == NULL)
    return;
  memcpy (baseAddr, (in + off), len);
  ((*env)->ReleasePrimitiveArrayCritical (env, buf, in, JNI_ABORT));
  return;
}

jint JNICALL
Java_java_util_zip_Inflater_inflateImpl (JNIEnv * env, jobject recv,
                                         jbyteArray buf, int off, int len,
                                         jlong handle)
{
  PORT_ACCESS_FROM_ENV (env);

  jbyte *out;
  JCLZipStream *stream;
  jint err = 0;
  jfieldID fid = 0, fid2 = 0;
  jint sin, sout, inBytes = 0;

  /* We need to get the number of bytes already read */
  fid = JCL_CACHE_GET (env, FID_java_util_zip_Inflater_inRead);
  inBytes = ((*env)->GetIntField (env, recv, fid));

  stream = (JCLZipStream *) handle;
  stream->stream->avail_out = len;
  sin = stream->stream->total_in;
  sout = stream->stream->total_out;
  out = ((*env)->GetPrimitiveArrayCritical (env, buf, 0));
  if (out == NULL)
    return -1;
  stream->stream->next_out = (Bytef *) out + off;
  err = inflate (stream->stream, Z_SYNC_FLUSH);
  ((*env)->ReleasePrimitiveArrayCritical (env, buf, out, 0));

  if (err != Z_OK)
    {
      if (err == Z_STREAM_END || err == Z_NEED_DICT)
        {
          ((*env)->SetIntField (env, recv, fid, (jint) stream->stream->total_in - sin + inBytes));      /* Update inRead */
          if (err == Z_STREAM_END)
            fid2 = JCL_CACHE_GET (env, FID_java_util_zip_Inflater_finished);
          else
            fid2 =
              JCL_CACHE_GET (env, FID_java_util_zip_Inflater_needsDictionary);

          ((*env)->SetBooleanField (env, recv, fid2, JNI_TRUE));
          return stream->stream->total_out - sout;
        }
      else
        {
          throwNewDataFormatException (env, "");
          return -1;
        }
    }

  /* Need to update the number of input bytes read. Is there a better way(Maybe global the fid then delete when end
     is called)? */
  ((*env)->
   SetIntField (env, recv, fid,
                (jint) stream->stream->total_in - sin + inBytes));

  return stream->stream->total_out - sout;
}

jint JNICALL
Java_java_util_zip_Inflater_getAdlerImpl (JNIEnv * env, jobject recv,
                                          jlong handle)
{
  JCLZipStream *stream;

  stream = (JCLZipStream *) handle;

  return stream->stream->adler;
}

void JNICALL
Java_java_util_zip_Inflater_endImpl (JNIEnv * env, jobject recv, jlong handle)
{
  PORT_ACCESS_FROM_ENV (env);
  JCLZipStream *stream;

  stream = (JCLZipStream *) handle;
  inflateEnd (stream->stream);
  if (stream->inaddr != NULL)   /*Input has been provided, free the buffer */
    jclmem_free_memory (env, stream->inaddr);
  if (stream->dict != NULL)
    jclmem_free_memory (env, stream->dict);
  jclmem_free_memory (env, stream->stream);
  jclmem_free_memory (env, stream);
}

void JNICALL
Java_java_util_zip_Inflater_setDictionaryImpl (JNIEnv * env, jobject recv,
                                               jbyteArray dict, int off,
                                               int len, jlong handle)
{
  PORT_ACCESS_FROM_ENV (env);
  int err = 0;
  U_8 *dBytes;
  JCLZipStream *stream = (JCLZipStream *) handle;

  dBytes = jclmem_allocate_memory (env, len);
  if (dBytes == NULL)
    {
      throwNewOutOfMemoryError (env, "");
      return;
    }
  (*env)->GetByteArrayRegion (env, dict, off, len, dBytes);
  err = inflateSetDictionary (stream->stream, (Bytef *) dBytes, len);
  if (err != Z_OK)
    {
      jclmem_free_memory (env, dBytes);
      throwNewIllegalArgumentException (env, "");
      return;
    }
  stream->dict = dBytes;
}

void JNICALL
Java_java_util_zip_Inflater_resetImpl (JNIEnv * env, jobject recv,
                                       jlong handle)
{
  JCLZipStream *stream;
  int err = 0;
  stream = (JCLZipStream *) handle;

  err = inflateReset (stream->stream);
  if (err != Z_OK)
    {
      throwNewIllegalArgumentException (env, "");
      return;
    }
}

/**
  * Throw java.util.zip.DataFormatException
  */
void
throwNewDataFormatException (JNIEnv * env, char *message)
{
  jclass exceptionClass = (*env)->FindClass(env, "java/util/zip/DataFormatException");
  if (0 == exceptionClass) { 
    /* Just return if we can't load the exception class. */
    return;
    }
  (*env)->ThrowNew(env, exceptionClass, message);
}

int JNICALL
Java_java_util_zip_Inflater_getTotalOutImpl (JNIEnv * env, jobject recv,
                                             jlong handle)
{
  JCLZipStream *stream;

  stream = (JCLZipStream *) handle;
  return stream->stream->total_out;
}

int JNICALL
Java_java_util_zip_Inflater_getTotalInImpl (JNIEnv * env, jobject recv,
                                            jlong handle)
{
  JCLZipStream *stream;

  stream = (JCLZipStream *) handle;
  return stream->stream->total_in;
}

void JNICALL
Java_java_util_zip_Inflater_oneTimeInitialization (JNIEnv * env, jclass clazz)
{
  jfieldID fid;

  fid = (*env)->GetFieldID (env, clazz, "inRead", "I");
  if (!fid)
    return;

  JCL_CACHE_SET (env, FID_java_util_zip_Inflater_inRead, fid);

  fid = (*env)->GetFieldID (env, clazz, "finished", "Z");
  if (!fid)
    return;

  JCL_CACHE_SET (env, FID_java_util_zip_Inflater_finished, fid);

  fid = (*env)->GetFieldID (env, clazz, "needsDictionary", "Z");
  if (!fid)
    return;

  JCL_CACHE_SET (env, FID_java_util_zip_Inflater_needsDictionary, fid);
}
