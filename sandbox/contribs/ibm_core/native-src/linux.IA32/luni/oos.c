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

#include "jni.h"

jlong JNICALL
Java_java_io_ObjectOutputStream_getFieldLong (JNIEnv * env, jclass clazz,
                                              jobject targetObject,
                                              jobject declaringClass,
                                              jobject fieldName)
{
  const char *fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  jfieldID fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "J");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid == 0)
    {
      /* Field not found. I believe we must throw an exception here */
      return (jlong) 0L;
    }
  else
    {
      return (*env)->GetLongField (env, targetObject, fid);
    }
}
jshort JNICALL
Java_java_io_ObjectOutputStream_getFieldShort (JNIEnv * env, jclass clazz,
                                               jobject targetObject,
                                               jobject declaringClass,
                                               jobject fieldName)
{
  const char *fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  jfieldID fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "S");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid == 0)
    {
      /* Field not found. I believe we must throw an exception here */
      return (jshort) 0;
    }
  else
    {
      return (*env)->GetShortField (env, targetObject, fid);
    }
}
jdouble JNICALL
Java_java_io_ObjectOutputStream_getFieldDouble (JNIEnv * env, jclass clazz,
                                                jobject targetObject,
                                                jobject declaringClass,
                                                jobject fieldName)
{
  const char *fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  jfieldID fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "D");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid == 0)
    {
      /* Field not found. I believe we must throw an exception here */
      return (jdouble) 0.0;
    }
  else
    {
      return (*env)->GetDoubleField (env, targetObject, fid);
    }
}
jboolean JNICALL
Java_java_io_ObjectOutputStream_getFieldBool (JNIEnv * env, jclass clazz,
                                              jobject targetObject,
                                              jobject declaringClass,
                                              jobject fieldName)
{
  const char *fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  jfieldID fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "Z");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid == 0)
    {
      /* Field not found. I believe we must throw an exception here */
      return (jboolean) 0;
    }
  else
    {
      return (*env)->GetBooleanField (env, targetObject, fid);
    }
}
jbyte JNICALL
Java_java_io_ObjectOutputStream_getFieldByte (JNIEnv * env, jclass clazz,
                                              jobject targetObject,
                                              jobject declaringClass,
                                              jobject fieldName)
{
  const char *fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  jfieldID fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "B");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid == 0)
    {
      /* Field not found. I believe we must throw an exception here */
      return (jbyte) 0;
    }
  else
    {
      return (*env)->GetByteField (env, targetObject, fid);
    }
}
jfloat JNICALL
Java_java_io_ObjectOutputStream_getFieldFloat (JNIEnv * env, jclass clazz,
                                               jobject targetObject,
                                               jobject declaringClass,
                                               jobject fieldName)
{
  const char *fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  jfieldID fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "F");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid == 0)
    {
      /* Field not found. I believe we must throw an exception here */
      return (jfloat) 0.0f;
    }
  else
    {
      return (*env)->GetFloatField (env, targetObject, fid);
    }

}

jchar JNICALL
Java_java_io_ObjectOutputStream_getFieldChar (JNIEnv * env, jclass clazz,
                                              jobject targetObject,
                                              jobject declaringClass,
                                              jobject fieldName)
{
  const char *fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  jfieldID fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "C");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid == 0)
    {
      /* Field not found. I believe we must throw an exception here */
      return (jchar) 0;
    }
  else
    {
      return (*env)->GetCharField (env, targetObject, fid);
    }
}
jobject JNICALL
Java_java_io_ObjectOutputStream_getFieldObj (JNIEnv * env, jclass clazz,
                                             jobject targetObject,
                                             jobject declaringClass,
                                             jobject fieldName,
                                             jobject fieldTypeName)
{
  const char *fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  const char *fieldTypeNameInC =
    (*env)->GetStringUTFChars (env, fieldTypeName, NULL);
  jfieldID fid =
    (*env)->GetFieldID (env, declaringClass, fieldNameInC, fieldTypeNameInC);
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);
  (*env)->ReleaseStringUTFChars (env, fieldTypeName, fieldTypeNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid == 0)
    {
      /* Field not found. I believe we must throw an exception here */
      return (jobject) 0;
    }
  else
    {
      return (*env)->GetObjectField (env, targetObject, fid);
    }
}
jint JNICALL
Java_java_io_ObjectOutputStream_getFieldInt (JNIEnv * env, jclass clazz,
                                             jobject targetObject,
                                             jobject declaringClass,
                                             jobject fieldName)
{
  const char *fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  jfieldID fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "I");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid == 0)
    {
      /* Field not found. I believe we must throw an exception here */
      return (jint) 0;
    }
  else
    {
      return (*env)->GetIntField (env, targetObject, fid);
    }
}
