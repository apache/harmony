/* Copyright 2005 The Apache Software Foundation or its licensors, as applicable
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

#include <unicode/ubidi.h>

#include "harmony.h"
#include "BidiWrapper.h"

void check_fail (JNIEnv * env, int err);

JNIEXPORT jlong JNICALL Java_com_ibm_text_BidiWrapper_ubidi_1open
  (JNIEnv * env, jclass clazz)
{
  UBiDi *pBiDi = ubidi_open ();
  return (jlong) ((IDATA) pBiDi);
}

JNIEXPORT void JNICALL Java_com_ibm_text_BidiWrapper_ubidi_1close
  (JNIEnv * env, jclass clazz, jlong pBiDi)
{
  ubidi_close ((UBiDi *) ((IDATA) pBiDi));
}

JNIEXPORT void JNICALL Java_com_ibm_text_BidiWrapper_ubidi_1setPara
  (JNIEnv * env, jclass clazz, jlong pBiDi, jcharArray text, jint length,
   jbyte paraLevel, jbyteArray embeddingLevels)
{
  UErrorCode err = 0;
  jchar *_text = NULL;
  jbyte *_embeddingLevels = NULL;

  _text = (*env)->GetCharArrayElements (env, text, NULL);

  if (embeddingLevels != NULL)
    {
      _embeddingLevels =
	(*env)->GetByteArrayElements (env, embeddingLevels, NULL);
    }

  ubidi_setPara ((UBiDi *) ((IDATA) pBiDi), _text, length, paraLevel,
		 _embeddingLevels, &err);
  check_fail (env, err);

  (*env)->ReleaseCharArrayElements (env, text, _text, 0);
  if (_embeddingLevels != NULL)
    {
      (*env)->ReleaseByteArrayElements (env, embeddingLevels,
					_embeddingLevels, 0);
    }
}

JNIEXPORT jlong JNICALL Java_com_ibm_text_BidiWrapper_ubidi_1setLine
  (JNIEnv * env, jclass clazz, jlong pParaBiDi, jint start, jint limit)
{
  UErrorCode err = 0;

  UBiDi *pLineBiDi = ubidi_openSized (limit - start, 0, &err);
  check_fail (env, err);

  ubidi_setLine ((UBiDi *) ((IDATA) pParaBiDi), start, limit, pLineBiDi,
		 &err);
  check_fail (env, err);

  return (jlong) ((IDATA) pLineBiDi);
}

JNIEXPORT jint JNICALL Java_com_ibm_text_BidiWrapper_ubidi_1getDirection
  (JNIEnv * env, jclass clazz, jlong pBiDi)
{
  return ubidi_getDirection ((const UBiDi *) ((IDATA) pBiDi));
}

JNIEXPORT jint JNICALL Java_com_ibm_text_BidiWrapper_ubidi_1getLength
  (JNIEnv * env, jclass clazz, jlong pBiDi)
{
  return ubidi_getLength ((const UBiDi *) ((IDATA) pBiDi));
}

JNIEXPORT jbyte JNICALL Java_com_ibm_text_BidiWrapper_ubidi_1getParaLevel
  (JNIEnv * env, jclass clazz, jlong pBiDi)
{
  return ubidi_getParaLevel ((const UBiDi *) ((IDATA) pBiDi));
}

JNIEXPORT jbyteArray JNICALL Java_com_ibm_text_BidiWrapper_ubidi_1getLevels
  (JNIEnv * env, jclass clazz, jlong pBiDi)
{
  UErrorCode err = 0;
  const UBiDiLevel *levels = NULL;
  jbyteArray result = NULL;
  int len = 0;

  levels = ubidi_getLevels ((UBiDi *) ((IDATA) pBiDi), &err);
  check_fail (env, err);

  len = ubidi_getLength ((const UBiDi *) ((IDATA) pBiDi));
  result = (*env)->NewByteArray (env, len);
  (*env)->SetByteArrayRegion (env, result, 0, len, (jbyte *) levels);

  return result;
}

JNIEXPORT jint JNICALL Java_com_ibm_text_BidiWrapper_ubidi_1countRuns
  (JNIEnv * env, jclass clazz, jlong pBiDi)
{
  UErrorCode err = 0;

  int count = ubidi_countRuns ((UBiDi *) ((IDATA) pBiDi), &err);
  check_fail (env, err);

  return count;
}

JNIEXPORT jobject JNICALL Java_com_ibm_text_BidiWrapper_ubidi_1getRun
  (JNIEnv * env, jclass clazz, jlong pBiDi, jint index)
{
  int start = 0;
  int length = 0;
  UBiDiLevel level = 0;
  jclass run_clazz = 0;
  jmethodID initID = 0;
  jobject run = 0;

  ubidi_getVisualRun ((UBiDi *) ((IDATA) pBiDi), index, &start, &length);
  ubidi_getLogicalRun ((const UBiDi *) ((IDATA) pBiDi), start, NULL, &level);

  run_clazz = (*env)->FindClass (env, "com/ibm/text/BidiRun");
  initID = (*env)->GetMethodID (env, run_clazz, "<init>", "(III)V");

  run =
    (*env)->NewObject (env, run_clazz, initID, start, start + length, level);

  return run;
}

void
check_fail (JNIEnv * env, int err)
{
  jclass exception;
  char message[] = "ICU Internal Error:                     ";

  if (U_FAILURE (err))
    {
      sprintf (message, "ICU Internal Error: %d", err);
      exception = (*env)->FindClass (env, "java/lang/RuntimeException");
      (*env)->ThrowNew (env, exception, message);
    }
}

JNIEXPORT jintArray JNICALL Java_com_ibm_text_BidiWrapper_ubidi_1reorderVisual
  (JNIEnv * env, jclass clazz, jbyteArray levels, jint length)
{
  PORT_ACCESS_FROM_ENV (env);
  UBiDiLevel *local_levels = 0;
  int *local_indexMap = 0;
  jintArray result = 0;

  local_indexMap = (int *) hymem_allocate_memory (sizeof (int) * length);
  local_levels = (*env)->GetByteArrayElements (env, levels, NULL);

  ubidi_reorderVisual (local_levels, length, local_indexMap);

  result = (*env)->NewIntArray (env, length);
  (*env)->SetIntArrayRegion (env, result, 0, length, (jint *) local_indexMap);

  hymem_free_memory (local_indexMap);
  (*env)->ReleaseByteArrayElements (env, levels, local_levels, 0);

  return result;
}
