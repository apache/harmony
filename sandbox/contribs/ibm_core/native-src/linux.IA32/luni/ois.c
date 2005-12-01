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

void JNICALL
  Java_java_io_ObjectInputStream_setField__Ljava_lang_Object_2Ljava_lang_Class_2Ljava_lang_String_2Z
  (JNIEnv * env, jclass clazz, jobject targetObject, jobject declaringClass,
   jobject fieldName, jboolean newValue)
{
  const char *fieldNameInC;
  jfieldID fid;
  if (targetObject == NULL)
    return;
  fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "Z");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid != 0)
    {
      (*env)->SetBooleanField (env, targetObject, fid, newValue);
    }
}

void JNICALL
  Java_java_io_ObjectInputStream_setField__Ljava_lang_Object_2Ljava_lang_Class_2Ljava_lang_String_2C
  (JNIEnv * env, jclass clazz, jobject targetObject, jobject declaringClass,
   jobject fieldName, jchar newValue)
{
  const char *fieldNameInC;
  jfieldID fid;
  if (targetObject == NULL)
    return;
  fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "C");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid != 0)
    {
      (*env)->SetCharField (env, targetObject, fid, newValue);
    }
}
void JNICALL
  Java_java_io_ObjectInputStream_setField__Ljava_lang_Object_2Ljava_lang_Class_2Ljava_lang_String_2I
  (JNIEnv * env, jclass clazz, jobject targetObject, jobject declaringClass,
   jobject fieldName, jint newValue)
{
  const char *fieldNameInC;
  jfieldID fid;
  if (targetObject == NULL)
    return;
  fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "I");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid != 0)
    {
      (*env)->SetIntField (env, targetObject, fid, newValue);
    }
}

void JNICALL
  Java_java_io_ObjectInputStream_setField__Ljava_lang_Object_2Ljava_lang_Class_2Ljava_lang_String_2F
  (JNIEnv * env, jclass clazz, jobject targetObject, jobject declaringClass,
   jobject fieldName, jfloat newValue)
{
  const char *fieldNameInC;
  jfieldID fid;
  if (targetObject == NULL)
    return;
  fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "F");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid != 0)
    {
      (*env)->SetFloatField (env, targetObject, fid, newValue);
    }
}
void JNICALL
  Java_java_io_ObjectInputStream_setField__Ljava_lang_Object_2Ljava_lang_Class_2Ljava_lang_String_2D
  (JNIEnv * env, jclass clazz, jobject targetObject, jobject declaringClass,
   jobject fieldName, jdouble newValue)
{
  const char *fieldNameInC;
  jfieldID fid;
  if (targetObject == NULL)
    return;
  fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "D");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid != 0)
    {
      (*env)->SetDoubleField (env, targetObject, fid, newValue);
    }

}
void JNICALL
  Java_java_io_ObjectInputStream_setField__Ljava_lang_Object_2Ljava_lang_Class_2Ljava_lang_String_2S
  (JNIEnv * env, jclass clazz, jobject targetObject, jobject declaringClass,
   jobject fieldName, jshort newValue)
{
  const char *fieldNameInC;
  jfieldID fid;
  if (targetObject == NULL)
    return;
  fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "S");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid != 0)
    {
      (*env)->SetShortField (env, targetObject, fid, newValue);
    }

}
void JNICALL
  Java_java_io_ObjectInputStream_setField__Ljava_lang_Object_2Ljava_lang_Class_2Ljava_lang_String_2J
  (JNIEnv * env, jclass clazz, jobject targetObject, jobject declaringClass,
   jobject fieldName, jlong newValue)
{
  const char *fieldNameInC;
  jfieldID fid;
  if (targetObject == NULL)
    return;
  fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "J");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid != 0)
    {
      (*env)->SetLongField (env, targetObject, fid, newValue);
    }
}
jobject JNICALL
Java_java_io_ObjectInputStream_newInstance (JNIEnv * env, jclass clazz,
                                            jobject instantiationClass,
                                            jobject constructorClass)
{
  jmethodID mid =
    (*env)->GetMethodID (env, constructorClass, "<init>", "()V");

  if (mid == 0)
    {
      /* Cant newInstance,No empty constructor... */
      return (jobject) 0;
    }
  else
    {
      return (jobject) (*env)->NewObject (env, instantiationClass, mid);        /* Instantiate an object of a given class */
    }

}
void JNICALL
  Java_java_io_ObjectInputStream_setField__Ljava_lang_Object_2Ljava_lang_Class_2Ljava_lang_String_2B
  (JNIEnv * env, jclass clazz, jobject targetObject, jobject declaringClass,
   jobject fieldName, jbyte newValue)
{
  const char *fieldNameInC;
  jfieldID fid;
  if (targetObject == NULL)
    return;
  fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  fid = (*env)->GetFieldID (env, declaringClass, fieldNameInC, "B");
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid != 0)
    {
      (*env)->SetByteField (env, targetObject, fid, newValue);
    }
}
void JNICALL
Java_java_io_ObjectInputStream_objSetField (JNIEnv * env, jclass clazz,
                                            jobject targetObject,
                                            jobject declaringClass,
                                            jobject fieldName,
                                            jobject fieldTypeName,
                                            jobject newValue)
{
  const char *fieldNameInC, *fieldTypeNameInC;
  jfieldID fid;
  if (targetObject == NULL)
    return;
  fieldNameInC = (*env)->GetStringUTFChars (env, fieldName, NULL);
  fieldTypeNameInC = (*env)->GetStringUTFChars (env, fieldTypeName, NULL);
  fid =
    (*env)->GetFieldID (env, declaringClass, fieldNameInC, fieldTypeNameInC);
  (*env)->ReleaseStringUTFChars (env, fieldName, fieldNameInC);
  (*env)->ReleaseStringUTFChars (env, fieldTypeName, fieldTypeNameInC);

  /* Two options now. Maybe getFieldID caused an exception, or maybe it returned the real value */
  if (fid != 0)
    {
      (*env)->SetObjectField (env, targetObject, fid, newValue);
    }
}
