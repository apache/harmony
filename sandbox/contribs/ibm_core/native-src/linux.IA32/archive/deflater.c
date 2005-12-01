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

#include "zlib.h"
#include "inflater.h"
#include "jclglob.h"
#include "jclprots.h"

void zfree PROTOTYPE ((void *opaque, void *address));
void *zalloc PROTOTYPE ((void *opaque, U_32 items, U_32 size));

void JNICALL
Java_java_util_zip_Deflater_setDictionaryImpl (JNIEnv * env, jobject recv,
					       jbyteArray dict, int off,
					       int len, jlong handle)
{
  PORT_ACCESS_FROM_ENV (env);
  int err = 0;
  char *dBytes;
  JCLZipStream *stream = (JCLZipStream *) ((IDATA) handle);

  dBytes = jclmem_allocate_memory (env, len);
  if (dBytes == NULL)
    {
      throwNewOutOfMemoryError (env, "");
      return;
    }
  (*env)->GetByteArrayRegion (env, dict, off, len, dBytes);
  err = deflateSetDictionary (stream->stream, (Bytef *) dBytes, len);
  if (err != Z_OK)
    {
      jclmem_free_memory (env, dBytes);
      throwNewIllegalArgumentException (env, "");
      return;
    }
  stream->dict = dBytes;
}

jint JNICALL
Java_java_util_zip_Deflater_getTotalInImpl (JNIEnv * env, jobject recv,
					    jlong handle)
{
  JCLZipStream *stream;

  stream = (JCLZipStream *) ((IDATA) handle);
  return stream->stream->total_in;
}

int JNICALL
Java_java_util_zip_Deflater_getTotalOutImpl (JNIEnv * env, jobject recv,
					     jlong handle)
{
  JCLZipStream *stream;

  stream = (JCLZipStream *) ((IDATA) handle);
  return stream->stream->total_out;
}

jint JNICALL
Java_java_util_zip_Deflater_getAdlerImpl (JNIEnv * env, jobject recv,
					  jlong handle)
{
  JCLZipStream *stream;

  stream = (JCLZipStream *) ((IDATA) handle);

  return stream->stream->adler;
}

/* Create a new stream . This stream cannot be used until it has been properly initialized. */
jlong JNICALL
Java_java_util_zip_Deflater_createStream (JNIEnv * env, jobject recv,
					  jint level, jint strategy,
					  jboolean noHeader)
{  
  PORT_ACCESS_FROM_ENV (env);

  JCLZipStream *jstream;
  z_stream *stream;
  int err = 0;
  int wbits = 15;		/*Use MAX for fastest */

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
  jstream->stream = stream;
  jstream->dict = NULL;
  jstream->inaddr = NULL;

  if (noHeader)			/*Unable to find official doc that this is the way to avoid zlib header use. However doc in zipsup.c claims it is so */
    wbits = wbits / -1;
  err = deflateInit2 (stream, level, Z_DEFLATED,	/*Only supported ZLIB method */
		      wbits,	/*Window bits to use. 15 is fastest but consumes the most memory */
		      9,	/*Memory allocation for internal compression state. 9 uses the most. */
		      strategy);
  if (err != Z_OK)
    {
      throwNewIllegalArgumentException (env, "");
      return -1;
    }

  return (jlong) ((IDATA) jstream);
}

void JNICALL
Java_java_util_zip_Deflater_setInputImpl (JNIEnv * env, jobject recv,
					  jbyteArray buf, jint off, jint len,
					  jlong handle)
{
  PORT_ACCESS_FROM_ENV (env);

  jbyte *in;
  JCLZipStream *stream;

  stream = (JCLZipStream *) ((IDATA) handle);
  if (stream->inaddr != NULL)	/*Input has already been provided, free the old buffer */
    jclmem_free_memory (env, stream->inaddr);
  stream->inaddr = jclmem_allocate_memory (env, len);
  if (stream->inaddr == NULL)
    {
      throwNewOutOfMemoryError (env, "");
      return;
    }
  in = ((*env)->GetPrimitiveArrayCritical (env, buf, 0));
  if (in == NULL)
    return;
  memcpy (stream->inaddr, (in + off), len);
  ((*env)->ReleasePrimitiveArrayCritical (env, buf, in, JNI_ABORT));
  stream->stream->next_in = (Bytef *) stream->inaddr;
  stream->stream->avail_in = len;

  return;
}

jint JNICALL
Java_java_util_zip_Deflater_deflateImpl (JNIEnv * env, jobject recv,
					 jbyteArray buf, int off, int len,
					 jlong handle, int flushParm)
{
  PORT_ACCESS_FROM_ENV (env);

  jbyte *out;
  JCLZipStream *stream;
  jint err = 0;
  jint sin, sout, inBytes = 0;

  /* We need to get the number of bytes already read */
  inBytes =
    ((*env)->
     GetIntField (env, recv,
		  JCL_CACHE_GET (env, FID_java_util_zip_Deflater_inRead)));

  stream = (JCLZipStream *) ((IDATA) handle);
  stream->stream->avail_out = len;
  sin = stream->stream->total_in;
  sout = stream->stream->total_out;
  out = ((*env)->GetPrimitiveArrayCritical (env, buf, 0));
  if (out == NULL)
    return -1;
  stream->stream->next_out = (Bytef *) out + off;
  err = deflate (stream->stream, flushParm);
  ((*env)->ReleasePrimitiveArrayCritical (env, buf, out, 0));
  if (err != Z_OK)
    {
      if (err == Z_STREAM_END)
	{
	  ((*env)->
	   SetBooleanField (env, recv,
			    JCL_CACHE_GET (env,
					   FID_java_util_zip_Deflater_finished),
			    JNI_TRUE));
	  return stream->stream->total_out - sout;
	}
    }
  if (flushParm != Z_FINISH)
    {
      /* Need to update the number of input bytes read. */
      ((*env)->
       SetIntField (env, recv,
		    JCL_CACHE_GET (env, FID_java_util_zip_Deflater_inRead),
		    (jint) stream->stream->total_in - sin + inBytes));
    }
  return stream->stream->total_out - sout;
}

void JNICALL
Java_java_util_zip_Deflater_endImpl (JNIEnv * env, jobject recv, jlong handle)
{
  PORT_ACCESS_FROM_ENV (env);
  JCLZipStream *stream;

  stream = (JCLZipStream *) ((IDATA) handle);

  deflateEnd (stream->stream);
  if (stream->inaddr != NULL)
    jclmem_free_memory (env, stream->inaddr);
  if (stream->dict != NULL)
    jclmem_free_memory (env, stream->dict);
  jclmem_free_memory (env, stream->stream);
  jclmem_free_memory (env, stream);
}

void JNICALL
Java_java_util_zip_Deflater_resetImpl (JNIEnv * env, jobject recv,
				       jlong handle)
{
  JCLZipStream *stream;

  stream = (JCLZipStream *) ((IDATA) handle);
  deflateReset (stream->stream);
}

void JNICALL
Java_java_util_zip_Deflater_setLevelsImpl (JNIEnv * env, jobject recv,
					   int level, int strategy,
					   jlong handle)
{
  PORT_ACCESS_FROM_ENV (env);

  JCLZipStream *stream;
  jbyte b = 0;
  int err = 0;

  if (handle == -1)
    {
      throwNewIllegalStateException (env, "");
      return;
    }
  stream = (JCLZipStream *) ((IDATA) handle);
  stream->stream->next_out = (Bytef *) & b;
  err = deflateParams (stream->stream, level, strategy);
  if (err != Z_OK)
    throwNewIllegalStateException (env, "");
}

void JNICALL
Java_java_util_zip_Deflater_oneTimeInitialization (JNIEnv * env, jclass clazz)
{
  jfieldID fid;

  fid = (*env)->GetFieldID (env, clazz, "inRead", "I");
  if (!fid)
    return;
  JCL_CACHE_SET (env, FID_java_util_zip_Deflater_inRead, fid);

  fid = (*env)->GetFieldID (env, clazz, "finished", "Z");
  if (!fid)
    return;
  JCL_CACHE_SET (env, FID_java_util_zip_Deflater_finished, fid);
}
