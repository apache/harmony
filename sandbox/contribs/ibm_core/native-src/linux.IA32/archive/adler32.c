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
#include "zconf.h"
uLong adler32 PROTOTYPE ((uLong crc, const Bytef * buf, uInt size));

jlong JNICALL
Java_java_util_zip_Adler32_updateImpl (JNIEnv * env, jobject recv,
                                       jbyteArray buf, int off, int len,
                                       jlong crc)
{
  PORT_ACCESS_FROM_ENV (env);

  jbyte *b;
  jboolean isCopy;
  jlong result;

  b = (*env)->GetPrimitiveArrayCritical (env, buf, &isCopy);
  result = (jlong) adler32 ((uLong) crc, (Bytef *) (b + off), (uInt) len);
  (*env)->ReleasePrimitiveArrayCritical (env, buf, b, JNI_ABORT);

  return result;
}

jlong JNICALL
Java_java_util_zip_Adler32_updateByteImpl (JNIEnv * env, jobject recv,
                                           jint val, jlong crc)
{
  PORT_ACCESS_FROM_ENV (env);

  return adler32 ((uLong) crc, (Bytef *) (&val), 1);
}
