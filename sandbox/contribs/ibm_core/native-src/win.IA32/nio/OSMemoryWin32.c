/* Copyright 2004 The Apache Software Foundation or its licensors, as applicable
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

/*
 * Common natives supporting the memory system interface.
 */

#include <stdlib.h>
#include <string.h>
#include "OSMemory.h"

JNIEXPORT jboolean JNICALL Java_com_ibm_platform_OSMemory_isLittleEndianImpl
  (JNIEnv * env, jclass clazz)
{
  return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_com_ibm_platform_OSMemory_getPointerSizeImpl
  (JNIEnv * env, jclass clazz)
{
#if defined(_WIN64)
  return 8;
#else
  return 4;
#endif

}

JNIEXPORT jlong JNICALL Java_com_ibm_platform_OSMemory_getAddress
  (JNIEnv * env, jobject thiz, jlong address)
{
#if defined(_WIN64)
  return *(POINTER_64) address;
#else
  return (jlong) * (long *) address;
#endif

}

JNIEXPORT void JNICALL Java_com_ibm_platform_OSMemory_setAddress
  (JNIEnv * env, jobject thiz, jlong address, jlong value)
{
#if defined(_WIN64)
  *(POINTER_64) address = value;
#else
  *(long *) address = (long) value;
#endif

}

