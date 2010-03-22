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

#include "MemMacros.h"
#include "org_apache_harmony_misc_accessors_ArrayAccessor.h"
#if defined(NEEDS_SYS_TYPES)
#include <sys/types.h>
#endif

/*
 * Class:     org_apache_harmony_misc_accessors_ArrayAccessor
 * Method:    staticLockArray
 * Signature: (Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_ArrayAccessor_staticLockArray
  (JNIEnv *env, jclass clazz, jobject array) {
    return addr2jlong((*env)->GetPrimitiveArrayCritical(env, (jarray)array, NULL));
}


/*
 * Class:     org_apache_harmony_misc_accessors_ArrayAccessor
 * Method:    staticUnlockArray
 * Signature: (Ljava/lang/Object;J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_ArrayAccessor_staticUnlockArray
  (JNIEnv *env, jclass clazz, jobject array, jlong addr) {
    (*env)->ReleasePrimitiveArrayCritical(env, (jarray)array,  jlong2addr(jlong, addr), 0);
}

/*
 * Class:     org_apache_harmony_misc_accessors_ArrayAccessor
 * Method:    staticUnlockArrayNoCopy
 * Signature: (Ljava/lang/Object;J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_ArrayAccessor_staticUnlockArrayNoCopy
  (JNIEnv *env, jclass clazz, jobject array, jlong addr) {
    (*env)->ReleasePrimitiveArrayCritical(env, (jarray)array,  jlong2addr(jlong, addr), JNI_ABORT);
}



/*
 * Class:     org_apache_harmony_misc_accessors_ArrayAccessor
 * Method:    staticPin<Type>Array
 * Signature: (Ljava/lang/Object;)J
 * Method:    staticUnpin<Type>Array
 * Signature: (Ljava/lang/Object;J)V
 * Method:    staticUnpin<Type>ArrayNoCopy
 * Signature: (Ljava/lang/Object;J)V
 */
#define pinFunctions(Type, t) \
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_ArrayAccessor_staticPin##Type##Array \
(JNIEnv *env, jclass clss, jobject array) { \
    jboolean isCopy; \
    return addr2jlong((*env)->Get##Type##ArrayElements(env, (t##Array)array, &isCopy)); \
} \
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_ArrayAccessor_staticUnpin##Type##Array \
(JNIEnv *env, jclass clss, jobject array, jlong addr) { \
  (*env)->Release##Type##ArrayElements(env, (t##Array)array, jlong2addr(t, addr), 0); \
} \
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_ArrayAccessor_staticUnpin##Type##ArrayNoCopy \
(JNIEnv *env, jclass clss, jobject array, jlong addr) { \
  (*env)->Release##Type##ArrayElements(env, (t##Array)array, jlong2addr(t, addr), JNI_ABORT); \
}

pinFunctions(Byte, jbyte)
pinFunctions(Char, jchar)
pinFunctions(Short, jshort)
pinFunctions(Int, jint)
pinFunctions(Long, jlong)
pinFunctions(Boolean, jboolean)
pinFunctions(Float, jfloat)
pinFunctions(Double, jdouble)



/*
 * Class:     org_apache_harmony_misc_accessors_ArrayAccessor
 * Method:    getElement
 * Signature: ([TI)T
 * Method:    setElement
 * Signature: ([TIT)V
 */
#define setGetFunctions(T, t) \
 JNIEXPORT t JNICALL Java_org_apache_harmony_misc_accessors_ArrayAccessor_getElement___3##T##I \
  (JNIEnv *env, jobject obj, t##Array array, jint index) { \
    t* ptr = (t*)(*env)->GetPrimitiveArrayCritical(env, (jarray)array, NULL); \
    t res = ptr[index]; \
    (*env)->ReleasePrimitiveArrayCritical(env, (jarray)array, ptr, 0); \
    return res; \
  } \
 JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_ArrayAccessor_setElement___3##T##I##T \
(JNIEnv *env, jobject obj, t##Array array, jint index, t value) { \
    t* ptr = (t*)(*env)->GetPrimitiveArrayCritical(env, (jarray)array, NULL); \
    ptr[index] = value; \
    (*env)->ReleasePrimitiveArrayCritical(env, (jarray)array, ptr, 0); \
}

setGetFunctions(B, jbyte)
setGetFunctions(Z, jboolean)
setGetFunctions(S, jshort)
setGetFunctions(C, jchar)
setGetFunctions(I, jint)
setGetFunctions(J, jlong)
setGetFunctions(F, jfloat)
setGetFunctions(D, jdouble)

/*
 * Class:     org_apache_harmony_misc_accessors_ArrayAccessor
 * Method:    getElement
 * Signature: ([Ljava/lang/Object;I)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_apache_harmony_misc_accessors_ArrayAccessor_getElement___3Ljava_lang_Object_2I
(JNIEnv *env, jobject obj, jobjectArray array, jint index) {
    return (*env)->GetObjectArrayElement(env, array, index);
}

/*
 * Class:     org_apache_harmony_misc_accessors_ArrayAccessor
 * Method:    setElement
 * Signature: ([Ljava/lang/Object;ILjava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_ArrayAccessor_setElement___3Ljava_lang_Object_2ILjava_lang_Object_2
(JNIEnv *env, jobject obj, jobjectArray array, jint index, jobject value) {
    (*env)->SetObjectArrayElement(env, array, index, value);
}

