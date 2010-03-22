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

#include <string.h>
#if defined(NEEDS_SYS_TYPES)
#include <sys/types.h>
#endif
#include "MemMacros.h"
#include "org_apache_harmony_misc_accessors_MemoryAccessor.h"

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getNativeByteOrder0
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getNativeByteOrder0
  (JNIEnv *env, jobject obj) 
{
    short value = 0x1234;
    return (*(char *)&value) == 0x34 ? 1 : 0;
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getByte
 * Signature: (J)B
 */
JNIEXPORT jbyte JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getByte
  (JNIEnv *env, jobject self, jlong addr)
{
    return *(jlong2addr(jbyte, addr));
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getChar
 * Signature: (J)C
 */
JNIEXPORT jchar JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getChar
  (JNIEnv *env, jobject self, jlong addr)
{
    return get_unaligned(jchar, addr);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getShort
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getShort
  (JNIEnv *env, jobject self, jlong addr)
{
    return get_unaligned(jshort, addr);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getShortReorder
 * Signature: (J)S
 */
JNIEXPORT jshort JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getShortReorder
  (JNIEnv *env, jobject self, jlong addr)
{
    return byte_swap_16(get_unaligned(jshort, addr));
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getInt
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getInt
  (JNIEnv *env, jobject self, jlong addr)
{
    return get_unaligned(jint, addr);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getIntReorder
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getIntReorder
  (JNIEnv *env, jobject self, jlong addr)
{
    return byte_swap_32(get_unaligned(jint, addr));
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getLong
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getLong
  (JNIEnv *env, jobject self, jlong addr)
{
    return get_unaligned(jlong, addr);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getLongReorder
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getLongReorder
  (JNIEnv *env, jobject self, jlong addr)
{
    return byte_swap_64(get_unaligned(jlong, addr));
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getFloat
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getFloat
  (JNIEnv *env, jobject self, jlong addr)
{
    return get_unaligned(jfloat, addr);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getFloatReorder
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getFloatReorder
  (JNIEnv *env, jobject self, jlong addr)
{
    jint value = byte_swap_32(get_unaligned(jint, addr));
    return jint2float(value);
}


/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getBoolean
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getBoolean
  (JNIEnv *env, jobject self, jlong addr)
{
    return *(jlong2addr(jboolean, addr));
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getDouble
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getDouble
  (JNIEnv *env, jobject self, jlong addr)
{
    return get_unaligned(jdouble, addr);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getDoubleReorder
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getDoubleReorder
  (JNIEnv *env, jobject self, jlong addr)
{
    jlong value = byte_swap_64(get_unaligned(jlong, addr));
    return jlong2double(value);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setBoolean
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setBoolean
  (JNIEnv *env, jobject self, jlong addr, jboolean value)
{
   *(jlong2addr(jboolean, addr)) = value;
}
/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setByte
 * Signature: (JB)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setByte
  (JNIEnv *env, jobject self, jlong addr, jbyte value)
{
    *(jlong2addr(jbyte, addr)) = value;
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setChar
 * Signature: (JC)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setChar
  (JNIEnv *env, jobject self, jlong addr, jchar value)
{
    set_unaligned(jchar, addr, value);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setDouble
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setDouble
  (JNIEnv *env, jobject self, jlong addr, jdouble value)
{
   set_unaligned(jdouble, addr, value);
}
/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setFloat
 * Signature: (JF)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setFloat
  (JNIEnv *env, jobject self, jlong addr, jfloat value)
{
    set_unaligned(jfloat, addr, value);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setFloatReorder
 * Signature: (JF)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setFloatReorder
  (JNIEnv *env, jobject self, jlong addr, jfloat value)
{
    jint swapped = byte_swap_32(float2jint(value));
    set_unaligned(jint, addr, swapped);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setInt
 * Signature: (JI)V
 */
 
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setInt
  (JNIEnv *env, jobject self, jlong addr, jint value)
{
    set_unaligned(jint, addr, value);
}
/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setShortReorder
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setShortReorder
  (JNIEnv *env, jobject self, jlong addr, jshort value)
{
    jshort swapped = byte_swap_16(value);
    set_unaligned(jshort, addr, swapped);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setShort
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setShort
  (JNIEnv *env, jobject self, jlong addr, jshort value)
{
    set_unaligned(jshort, addr, value);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getPointer
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getPointer
  (JNIEnv *env, jobject self, jlong addr)
{
    return *(jlong2addr(uintptr_t, addr));
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setPointer
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setPointer
  (JNIEnv *env, jobject self, jlong addr, jlong value)
{
    *(jlong2addr(uintptr_t, addr)) = (uintptr_t)value;
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setIntReorder
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setIntReorder
  (JNIEnv *env, jobject self, jlong addr, jint value)
{
    jint swapped = byte_swap_32(value);
    set_unaligned(jint, addr, swapped);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setLong
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setLong
  (JNIEnv *env, jobject self, jlong addr, jlong value)
{
    set_unaligned(jlong, addr, value);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setLongReorder
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setLongReorder
  (JNIEnv *env, jobject self, jlong addr, jlong value)
{
    jlong swapped = byte_swap_64(value);
    set_unaligned(jlong, addr, swapped);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setDoubleReorder
 * Signature: (JD)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setDoubleReorder
  (JNIEnv *env, jobject self, jlong addr, jdouble value)
{
    jlong swapped = byte_swap_64(double2jlong(value));
    set_unaligned(jlong, addr, swapped);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getArray
 * Signature: (JLjava/lang/Object;JJ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getArray
  (JNIEnv *env, jobject self, jlong addr, jobject array, jlong offset, jlong size)
{
  char *buf = (char*)(*env)->GetPrimitiveArrayCritical(env, (jarray)array, 0);

  memcpy(buf + offset, jlong2addr(char, addr), (size_t)size);

  (*env)->ReleasePrimitiveArrayCritical(env, (jarray)array, buf, 0);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setArray
 * Signature: (JLjava/lang/Object;JJ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setArray
  (JNIEnv *env, jobject self, jlong addr, jobject array, jlong offset, jlong size)
{
  char *buf = (char*)(*env)->GetPrimitiveArrayCritical(env, (jarray)array, 0);

  memcpy(jlong2addr(char, addr), buf + offset, (size_t)size);

  (*env)->ReleasePrimitiveArrayCritical(env, (jarray)array, buf, 0);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getArrayReorder16
 * Signature: (JLjava/lang/Object;JJ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getArrayReorder16
  (JNIEnv *env, jobject self, jlong addr, jobject array, jlong offset, jlong size)
{
  int i;
  char *buf = (char*)(*env)->GetPrimitiveArrayCritical(env, (jarray)array, 0);

  jshort *src = jlong2addr(jshort, addr);
  jshort *dst = (jshort*)buf + offset;
  for(i = 0; i < size; ++i)  {
      dst[i] = byte_swap_16(src[i]);
  }

  (*env)->ReleasePrimitiveArrayCritical(env, (jarray)array, buf, 0);
}
/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getArrayReorder32
 * Signature: (JLjava/lang/Object;JJ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getArrayReorder32
  (JNIEnv *env, jobject self, jlong addr, jobject array, jlong offset, jlong size)
{
    int i;
    char *buf = (char*)(*env)->GetPrimitiveArrayCritical(env, (jarray)array, 0);

    jint *src = jlong2addr(jint, addr);
    jint *dst = (jint*)buf + offset;
    for(i = 0; i < size; ++i)  {
        dst[i] = byte_swap_32(src[i]);
    }

    (*env)->ReleasePrimitiveArrayCritical(env, (jarray)array, buf, 0);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getArrayReorder64
 * Signature: (JLjava/lang/Object;JJ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getArrayReorder64
  (JNIEnv *env, jobject self, jlong addr, jobject array, jlong offset, jlong size)
{
    int i;
    char *buf = (char*)(*env)->GetPrimitiveArrayCritical(env, (jarray)array, 0);

    jlong *src = jlong2addr(jlong, addr);
    jlong *dst = (jlong*)buf + offset;
    for(i = 0; i < size; ++i)  {
        dst[i] = byte_swap_64(src[i]);
    }

    (*env)->ReleasePrimitiveArrayCritical(env, (jarray)array, buf, 0);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setArrayReorder16
 * Signature: (JLjava/lang/Object;JJ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setArrayReorder16
  (JNIEnv *env, jobject self, jlong addr, jobject array, jlong offset, jlong size)
{
  int i;
  char *buf = (char*)(*env)->GetPrimitiveArrayCritical(env, (jarray)array, 0);

  jshort *dst = jlong2addr(jshort, addr);
  jshort *src = (jshort*)buf + offset;
  for(i = 0; i < size; ++i)  {
      dst[i] = byte_swap_16(src[i]);
  }

  (*env)->ReleasePrimitiveArrayCritical(env, (jarray)array, buf, 0);
}
/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setArrayReorder32
 * Signature: (JLjava/lang/Object;JJ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setArrayReorder32
  (JNIEnv *env, jobject self, jlong addr, jobject array, jlong offset, jlong size)
{
    int i;
    char *buf = (char*)(*env)->GetPrimitiveArrayCritical(env, (jarray)array, 0);

    jint *dst = jlong2addr(jint, addr);
    jint *src = (jint*)buf + offset;
    for(i = 0; i < size; ++i)  {
        dst[i] = byte_swap_32(src[i]);
    }

    (*env)->ReleasePrimitiveArrayCritical(env, (jarray)array, buf, 0);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    setArrayReorder64
 * Signature: (JLjava/lang/Object;JJ)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_setArrayReorder64
  (JNIEnv *env, jobject self, jlong addr, jobject array, jlong offset, jlong size)
{
    int i;
    char *buf = (char*)(*env)->GetPrimitiveArrayCritical(env, (jarray)array, 0);

    jlong *dst = jlong2addr(jlong, addr);
    jlong *src = (jlong*)buf + offset;
    for(i = 0; i < size; ++i)  {
        dst[i] = byte_swap_64(src[i]);
    }

    (*env)->ReleasePrimitiveArrayCritical(env, (jarray)array, buf, 0);
}

/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    findFirstDiff
 * Signature: (JJJ)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_findFirstDiff
  (JNIEnv *env, jobject self, jlong addr1, jlong addr2, jlong size)
{
    jbyte* block1 = jlong2addr(jbyte, addr1);
    jbyte* block2 = jlong2addr(jbyte, addr2);
    int i = 0;
    while (i < size && block1[i] == block2[i]) {
        ++i;
    }
    return i;
}
/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    findFirstDiffReorder16
 * Signature: (JJJ)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_findFirstDiffReorder16
  (JNIEnv *env, jobject self, jlong addr1, jlong addr2, jlong size)
{
    jshort* block1 = jlong2addr(jshort, addr1);
    jshort* block2 = jlong2addr(jshort, addr2);
    int i = 0;
    while (i < size && byte_swap_16(block1[i]) == block2[i]) {
        ++i;
    }
    return i;
}
/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    findFirstDiffReorder32
 * Signature: (JJJ)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_findFirstDiffReorder32
  (JNIEnv *env, jobject self, jlong addr1, jlong addr2, jlong size)
{
    jint* block1 = jlong2addr(jint, addr1);
    jint* block2 = jlong2addr(jint, addr2);
    int i = 0;
    while (i < size && byte_swap_32(block1[i]) == block2[i]) {
        ++i;
    }
    return i;
}
/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    findFirstDiffReorder64
 * Signature: (JJJ)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_findFirstDiffReorder64
  (JNIEnv *env, jobject self, jlong addr1, jlong addr2, jlong size)
{
    jlong* block1 = jlong2addr(jlong, addr1);
    jlong* block2 = jlong2addr(jlong, addr2);
    int i = 0;
    while (i < size && byte_swap_64(block1[i]) == block2[i]) {
        ++i;
    }
    return i;
}
/*
 * Class:     org_apache_harmony_misc_accessors_MemoryAccessor
 * Method:    getHashCode
 * Signature: (JJ)I
 */
JNIEXPORT jint JNICALL Java_org_apache_harmony_misc_accessors_MemoryAccessor_getHashCode
  (JNIEnv *env, jobject self, jlong addr, jlong size)
{
    int mult = 1;
    jlong i;
    jbyte* block = jlong2addr(jbyte, addr);
    jint res = 0;
    for (i = size - 1; i >= 0; i--) {
        res += block[i] * mult;
        mult *= 31;
    }
    return res;
}
