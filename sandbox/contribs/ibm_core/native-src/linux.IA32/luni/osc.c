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
#include "jclglob.h"

jobject JNICALL
Java_java_io_ObjectStreamClass_getFieldSignature (JNIEnv * env, jclass clazz,
                                                  jobject reflectField)
{
  jclass fieldClass = (*env)->GetObjectClass (env, reflectField);
  return (*env)->CallNonvirtualObjectMethod (env, reflectField, fieldClass,
                                             JCL_CACHE_GET (env,
                                                            MID_java_lang_reflect_Field_getSignature));
}

jobject JNICALL
Java_java_io_ObjectStreamClass_getMethodSignature (JNIEnv * env, jclass clazz,
                                                   jobject reflectMethod)
{
  jclass methodClass = (*env)->GetObjectClass (env, reflectMethod);
  return (*env)->CallNonvirtualObjectMethod (env, reflectMethod, methodClass,
                                             JCL_CACHE_GET (env,
                                                            MID_java_lang_reflect_Method_getSignature));
}

jobject JNICALL
Java_java_io_ObjectStreamClass_getConstructorSignature (JNIEnv * env,
                                                        jclass clazz,
                                                        jobject
                                                        reflectConstructor)
{
  jclass constructorClass = (*env)->GetObjectClass (env, reflectConstructor);
  return (*env)->CallNonvirtualObjectMethod (env, reflectConstructor,
                                             constructorClass,
                                             JCL_CACHE_GET (env,
                                                            MID_java_lang_reflect_Constructor_getSignature));
}

jboolean JNICALL
Java_java_io_ObjectStreamClass_hasClinit (JNIEnv * env, jclass clazz,
                                          jobject targetClass)
{
  jmethodID mid =
    (*env)->GetStaticMethodID (env, targetClass, "<clinit>", "()V");
  (*env)->ExceptionClear (env);

  /* Can I just return mid and rely on typecast to convert to jboolean ? Safe implementation for now */
  if (mid == 0)
    {
      /* No <clinit>... */
      return (jboolean) 0;
    }
  else
    {
      return (jboolean) 1;
    }
}

void JNICALL
Java_java_io_ObjectStreamClass_oneTimeInitialization (JNIEnv * env,
                                                      jclass clazz)
{
  jclass lookupClass;
  jmethodID mid;

  lookupClass = (*env)->FindClass (env, "java/lang/reflect/Field");
  if (!lookupClass)
    return;
  mid =
    (*env)->GetMethodID (env, lookupClass, "getSignature",
                         "()Ljava/lang/String;");
  if (!mid)
    return;
  JCL_CACHE_SET (env, MID_java_lang_reflect_Field_getSignature, mid);

  lookupClass = (*env)->FindClass (env, "java/lang/reflect/Method");
  if (!lookupClass)
    return;
  mid =
    (*env)->GetMethodID (env, lookupClass, "getSignature",
                         "()Ljava/lang/String;");
  if (!mid)
    return;
  JCL_CACHE_SET (env, MID_java_lang_reflect_Method_getSignature, mid);

  lookupClass = (*env)->FindClass (env, "java/lang/reflect/Constructor");
  if (!lookupClass)
    return;
  mid =
    (*env)->GetMethodID (env, lookupClass, "getSignature",
                         "()Ljava/lang/String;");
  if (!mid)
    return;
  JCL_CACHE_SET (env, MID_java_lang_reflect_Constructor_getSignature, mid);
}
