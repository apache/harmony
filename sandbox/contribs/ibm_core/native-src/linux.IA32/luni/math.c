/* Copyright 2004, 2005 The Apache Software Foundation or its licensors, as applicable
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

#include "jni.h"
/* #include "fltconst.h" */
#undef __P
#include "fdlibm.h"

jdouble internal_ceil (jdouble arg1);
jdouble internal_log (jdouble arg1);
jdouble internal_cos (jdouble arg1);
jdouble internal_pow (jdouble arg1, jdouble arg2);
jdouble internal_sqrt (jdouble arg1);
void traceCall (char *name, double *arg1, double *arg2, double *result);
jdouble internal_atan (jdouble arg1);
jdouble internal_atan2 (jdouble arg1, jdouble arg2);
jdouble internal_asin (jdouble arg1);
jdouble internal_IEEEremainder (jdouble arg1, jdouble arg2);
jdouble internal_floor (jdouble arg1);
jdouble internal_acos (jdouble arg1);
jdouble internal_exp (jdouble arg1);
jdouble internal_tan (jdouble arg1);
jdouble internal_sin (jdouble arg1);
jdouble internal_rint (jdouble arg1);

extern scaleUpDouble (double *, int);

jdouble
internal_acos (jdouble arg1)
{
  jdouble result;

  result = fdlibm_acos (arg1);

  return result;
}

jdouble JNICALL
Java_java_lang_StrictMath_acos (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_acos (arg1);
}

jdouble JNICALL
Java_java_lang_Math_acos (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_acos (arg1);
}

jdouble
internal_asin (jdouble arg1)
{
  jdouble result;

  result = fdlibm_asin (arg1);

  return result;
}

jdouble
internal_atan (jdouble arg1)
{
  jdouble result;

  result = fdlibm_atan (arg1);

  return result;
}

jdouble
internal_atan2 (jdouble arg1, jdouble arg2)
{
  jdouble result;

  result = fdlibm_atan2 (arg1, arg2);

  return result;
}

jdouble
internal_ceil (jdouble arg1)
{
  jdouble result;

  result = fdlibm_ceil (arg1);

  return result;
}

jdouble
internal_cos (jdouble arg1)
{
  jdouble result;

  result = fdlibm_cos (arg1);

  return result;
}

jdouble
internal_exp (jdouble arg1)
{
  jdouble result;

  result = fdlibm_exp (arg1);

  return result;
}

jdouble
internal_floor (jdouble arg1)
{
  jdouble result;

  result = fdlibm_floor (arg1);

  return result;
}

jdouble
internal_IEEEremainder (jdouble arg1, jdouble arg2)
{
  jdouble result;

  result = fdlibm_remainder (arg1, arg2);

  return result;
}

jdouble
internal_log (jdouble arg1)
{
  jdouble result;

  result = fdlibm_log (arg1);

  return result;
}

jdouble
internal_pow (jdouble arg1, jdouble arg2)
{
  jdouble result;

  result = fdlibm_pow (arg1, arg2);

  return result;
}

jdouble
internal_rint (jdouble arg1)
{
  jdouble result;

  result = fdlibm_rint (arg1);

  return result;
}

jdouble
internal_sin (jdouble arg1)
{
  jdouble result;

  result = fdlibm_sin (arg1);

  return result;
}

jdouble
internal_sqrt (jdouble arg1)
{
  jdouble result;

  result = fdlibm_sqrt (arg1);

  return result;
}

jdouble
internal_tan (jdouble arg1)
{
  jdouble result;

  result = fdlibm_tan (arg1);

  return result;
}

jdouble JNICALL
Java_java_lang_StrictMath_asin (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_asin (arg1);
}

jdouble JNICALL
Java_java_lang_Math_asin (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_asin (arg1);
}

jdouble JNICALL
Java_java_lang_StrictMath_atan (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_atan (arg1);
}

jdouble JNICALL
Java_java_lang_Math_atan (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_atan (arg1);
}

jdouble JNICALL
Java_java_lang_StrictMath_atan2 (JNIEnv * env, jclass jclazz, jdouble arg1,
                                 jdouble arg2)
{
  return internal_atan2 (arg1, arg2);
}

jdouble JNICALL
Java_java_lang_Math_atan2 (JNIEnv * env, jclass jclazz, jdouble arg1,
                           jdouble arg2)
{
  return internal_atan2 (arg1, arg2);
}

jdouble JNICALL
Java_java_lang_StrictMath_ceil (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_ceil (arg1);
}

jdouble JNICALL
Java_java_lang_Math_ceil (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_ceil (arg1);
}

jdouble JNICALL
Java_java_lang_StrictMath_cos (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_cos (arg1);
}

jdouble JNICALL
Java_java_lang_Math_cos (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_cos (arg1);
}

jdouble JNICALL
Java_java_lang_StrictMath_exp (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_exp (arg1);
}

jdouble JNICALL
Java_java_lang_Math_exp (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_exp (arg1);
}

jdouble JNICALL
Java_java_lang_StrictMath_floor (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_floor (arg1);
}

jdouble JNICALL
Java_java_lang_Math_floor (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_floor (arg1);
}

jdouble JNICALL
Java_java_lang_StrictMath_IEEEremainder (JNIEnv * env, jclass jclazz,
                                         jdouble arg1, jdouble arg2)
{
  return internal_IEEEremainder (arg1, arg2);
}

jdouble JNICALL
Java_java_lang_Math_IEEEremainder (JNIEnv * env, jclass jclazz, jdouble arg1,
                                   jdouble arg2)
{
  return internal_IEEEremainder (arg1, arg2);
}

jdouble JNICALL
Java_java_lang_StrictMath_log (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_log (arg1);
}

jdouble JNICALL
Java_java_lang_Math_log (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_log (arg1);
}

jdouble JNICALL
Java_java_lang_StrictMath_pow (JNIEnv * env, jclass jclazz, jdouble arg1,
                               jdouble arg2)
{
  return internal_pow (arg1, arg2);
}

jdouble JNICALL
Java_java_lang_Math_pow (JNIEnv * env, jclass jclazz, jdouble arg1,
                         jdouble arg2)
{
  return internal_pow (arg1, arg2);
}

jdouble JNICALL
Java_java_lang_StrictMath_rint (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_rint (arg1);
}

jdouble JNICALL
Java_java_lang_Math_rint (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_rint (arg1);
}

jdouble JNICALL
Java_java_lang_StrictMath_sin (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_sin (arg1);
}

jdouble JNICALL
Java_java_lang_Math_sin (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_sin (arg1);
}

jdouble JNICALL
Java_java_lang_StrictMath_sqrt (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_sqrt (arg1);
}

jdouble JNICALL
Java_java_lang_Math_sqrt (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_sqrt (arg1);
}

jdouble JNICALL
Java_java_lang_StrictMath_tan (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_tan (arg1);
}

jdouble JNICALL
Java_java_lang_Math_tan (JNIEnv * env, jclass jclazz, jdouble arg1)
{
  return internal_tan (arg1);
}
