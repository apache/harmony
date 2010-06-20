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
/** 
 * @author Andrey Chernyshev
 */  

#include "jni.h"
#include "jni_direct.h"
#include "atomics.h"
#include "port_barriers.h"
#include "org_apache_harmony_util_concurrent_Atomics.h"

JNIEXPORT jint JNICALL Java_org_apache_harmony_util_concurrent_Atomics_arrayBaseOffset
  (JNIEnv * env, jclass self, jclass array)
{
    jlong array_element_size = Java_org_apache_harmony_util_concurrent_Atomics_arrayIndexScale(env, self, array);
    if(array_element_size < 8) {
        return VM_VECTOR_FIRST_ELEM_OFFSET_1_2_4;
    } else {
        return VM_VECTOR_FIRST_ELEM_OFFSET_8;
    }
}

JNIEXPORT jint JNICALL Java_org_apache_harmony_util_concurrent_Atomics_arrayIndexScale
  (JNIEnv * env, jclass self, jclass array)
{
    Class * clz = jclass_to_struct_Class(array);
    return clz->get_array_element_size();
}

JNIEXPORT void JNICALL 
Java_org_apache_harmony_util_concurrent_Atomics_setIntVolatile__Ljava_lang_Object_2JI(JNIEnv * env, jclass self, 
    jobject obj, jlong offset, jint value)
{
    SetIntFieldOffset(env, obj, (jint)offset, value);
    port_rw_barrier();
}


JNIEXPORT jint JNICALL 
Java_org_apache_harmony_util_concurrent_Atomics_getIntVolatile__Ljava_lang_Object_2J(JNIEnv * env, jclass self, 
    jobject obj, jlong offset)
{
    port_rw_barrier();
    return GetIntFieldOffset(env, obj, (jint)offset);
}

JNIEXPORT void JNICALL 
Java_org_apache_harmony_util_concurrent_Atomics_setLongVolatile__Ljava_lang_Object_2JJ(JNIEnv * env, jclass self, 
    jobject obj, jlong offset, jlong value)
{
    SetLongFieldOffset(env, obj, (jint)offset, value);
    port_rw_barrier();
}


JNIEXPORT jlong JNICALL 
Java_org_apache_harmony_util_concurrent_Atomics_getLongVolatile__Ljava_lang_Object_2J(JNIEnv * env, jclass self, 
    jobject obj, jlong offset)
{
    port_rw_barrier();
    return GetLongFieldOffset(env, obj, (jint)offset);
}

JNIEXPORT void JNICALL 
Java_org_apache_harmony_util_concurrent_Atomics_setObjectVolatile__Ljava_lang_Object_2JLjava_lang_Object_2(JNIEnv * env, jclass self, 
    jobject obj, jlong offset, jobject value)
{
    SetObjectFieldOffset(env, obj, (jint)offset, value);
    port_rw_barrier();
}


JNIEXPORT jobject JNICALL 
Java_org_apache_harmony_util_concurrent_Atomics_getObjectVolatile__Ljava_lang_Object_2J(JNIEnv * env, jclass self, 
    jobject obj, jlong offset)
{
    port_rw_barrier();
    return GetObjectFieldOffset(env, obj, (jint)offset);
}

JNIEXPORT jlong JNICALL
Java_org_apache_harmony_util_concurrent_Atomics_getFieldOffset(JNIEnv * env, jclass self, 
    jobject field)
{
    return getFieldOffset(env, field);
}

JNIEXPORT jboolean JNICALL 
Java_org_apache_harmony_util_concurrent_Atomics_compareAndSetObject__Ljava_lang_Object_2JLjava_lang_Object_2Ljava_lang_Object_2
(JNIEnv * env, jobject self, jobject obj, jlong offset, jobject expected, jobject value)
{     
    return compareAndSetObjectField(env, self, obj, offset, expected, value);
}
 
 
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_util_concurrent_Atomics_compareAndSetBoolean__Ljava_lang_Object_2JZZ 
(JNIEnv * env, jobject self, jobject obj, jlong offset, jboolean expected, jboolean value)
{
    return compareAndSetBooleanField(env, self, obj, offset, expected, value);
}
 
                   
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_util_concurrent_Atomics_compareAndSetInt__Ljava_lang_Object_2JII 
(JNIEnv * env, jobject self, jobject obj, jlong offset, jint expected, jint value)
{     
    return compareAndSetIntField(env, self, obj, offset, expected, value);
}
 
 
JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_util_concurrent_Atomics_compareAndSetLong__Ljava_lang_Object_2JJJ 
(JNIEnv * env, jobject self, jobject obj, jlong offset, jlong expected, jlong value)
{     
    return compareAndSetLongField(env, self, obj, offset, expected, value);
}


JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_util_concurrent_Atomics_compareAndSetInt___3IIII
(JNIEnv * env, jobject self, jintArray array, jint index, jint expected, jint value)
{
    return compareAndSetIntArray(env, self, array, index, expected, value);
}


JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_util_concurrent_Atomics_compareAndSetBoolean___3ZIZZ
(JNIEnv * env, jobject self, jbooleanArray array, jint index, jboolean expected, jboolean value)
{
    return compareAndSetBooleanArray(env, self, array, index, expected, value);
}


JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_util_concurrent_Atomics_compareAndSetLong___3JIJJ
(JNIEnv * env, jobject self, jlongArray array, jint index, jlong expected, jlong value)
{
    return compareAndSetLongArray(env, self, array, index, expected, value);
}


JNIEXPORT jboolean JNICALL
Java_org_apache_harmony_util_concurrent_Atomics_compareAndSetObject___3Ljava_lang_Object_2ILjava_lang_Object_2Ljava_lang_Object_2
(JNIEnv * env, jobject self, jobjectArray array, jint index, jobject expected, jobject value)
{
    return compareAndSetObjectArray(env, self, array, index, expected, value);
}

JNIEXPORT jboolean JNICALL 
Java_java_util_concurrent_atomic_AtomicLong_VMSupportsCS8
(JNIEnv *, jclass) 
{
    return vmSupportsCAS8();
}
