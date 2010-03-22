/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#include "org_apache_harmony_misc_accessors_Malloc.h"

#include <stdlib.h>
#include <string.h>
#if defined(NEEDS_SYS_TYPES)
#include <sys/types.h>
#endif

#include "MemMacros.h"

/*
 * Class:     org_apache_harmony_misc_accessors_Malloc
 * Method:    malloc
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_Malloc_malloc
  (JNIEnv *env, jclass clazz, jlong size)
{
    return addr2jlong(malloc((size_t)size));
}

/*
 * Class:     org_apache_harmony_misc_accessors_Malloc
 * Method:    realloc
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_Malloc_realloc
  (JNIEnv *env, jclass clazz, jlong addr, jlong size)
{
    return addr2jlong(realloc(jlong2addr(void, addr), (size_t)size));
}

/*
 * Class:     org_apache_harmony_misc_accessors_Malloc
 * Method:    free
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_Malloc_free
  (JNIEnv *env, jclass clazz, jlong addr) 
{
    free(jlong2addr(void, addr));
}

/*
 * Class:     org_apache_harmony_misc_accessors_Malloc
 * Method:    memcpy
 * Signature: (JJJ)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_Malloc_memcpy
  (JNIEnv *env, jclass clazz, jlong dst, jlong src, jlong len) 
{
    return addr2jlong(memcpy(jlong2addr(char, dst), jlong2addr(char, src), (size_t)len));
}

/*
 * Class:     org_apache_harmony_misc_accessors_Malloc
 * Method:    memcmp
 * Signature: (JJJ)I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_misc_accessors_Malloc_memcmp
  (JNIEnv *env, jclass clazz, jlong b1, jlong b2, jlong len)
{
    return memcmp(jlong2addr(char, b1), jlong2addr(char, b2), (size_t)len);
}

/*
 * Class:     org_apache_harmony_misc_accessors_Malloc
 * Method:    memset
 * Signature: (JIJ)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_Malloc_memset
  (JNIEnv *env, jclass clazz, jlong addr, jint c, jlong len)
{
    return addr2jlong(memset(jlong2addr(char, addr), (int)c, (size_t)len));
}

/*
 * Class:     org_apache_harmony_misc_accessors_Malloc
 * Method:    memmove
 * Signature: (JJJ)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_Malloc_memmove
  (JNIEnv *env, jclass clazz, jlong dst, jlong src, jlong len)
{
    return addr2jlong(memmove(jlong2addr(char, dst), jlong2addr(char, src), (size_t)len));
}

/*
 * Class:     org_apache_harmony_misc_accessors_Malloc
 * Method:    strncpy
 * Signature: (JJJ)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_Malloc_strncpy
  (JNIEnv *env, jclass self, jlong addr1, jlong addr2, jlong len)
{
    strncpy(jlong2addr(char, addr1), jlong2addr(const char, addr2), (size_t)len);
    return addr1;
}

/*
 * Class:     org_apache_harmony_misc_accessors_Malloc
 * Method:    getPointerSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_misc_accessors_Malloc_getPointerSize
  (JNIEnv *env, jclass clazz) 
{
    return sizeof(intptr_t);
}

/*
 * Class:     org_apache_harmony_misc_accessors_Malloc
 * Method:    getCLongSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_misc_accessors_Malloc_getCLongSize
  (JNIEnv *env, jclass clazz)
{
    return sizeof(long);
}

