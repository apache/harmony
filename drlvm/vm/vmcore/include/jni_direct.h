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
 * @author Intel, Gregory Shimansky
 */  


#ifndef _JNI_DIRECT_H_
#define _JNI_DIRECT_H_

#include <apr_ring.h>
#include <apr_pools.h>

#include "open/types.h"

#include "jni.h"
#include "open/types.h"
#include "class_member.h"

typedef struct JavaVM_Internal JavaVM_Internal;
typedef struct JNIEnv_Internal JNIEnv_Internal;

struct JavaVM_Internal : public JavaVM_External {
    apr_pool_t * pool;
    Global_Env * vm_env;   
    APR_RING_ENTRY(JavaVM_Internal) link;
    void* reserved;
};

struct JNIEnv_Internal : public JNIEnv_External {
    JavaVM_Internal* vm;
    void *reserved0;
};

struct _jfieldID : public Field {
};

jmethodID reflection_unreflect_method(JNIEnv *jenv, jobject ref_method);
jmethodID reflection_unreflect_constructor(JNIEnv *jenv, jobject ref_constructor);
jfieldID reflection_unreflect_field(JNIEnv *jenv, jobject ref_field);
jobject reflection_reflect_method(JNIEnv *jenv, Method_Handle method);
jobject reflection_reflect_constructor(JNIEnv *jenv, Method_Handle constructor);
jobject reflection_reflect_field(JNIEnv *jenv, Field_Handle field);

VMEXPORT jclass JNICALL DefineClass(JNIEnv *env, const char *name, jobject loader, const jbyte *buf, jsize len);

VMEXPORT jclass JNICALL FindClass(JNIEnv *env, const char *name);

VMEXPORT jclass JNICALL GetObjectClass(JNIEnv *env, jobject obj);

VMEXPORT jboolean JNICALL IsAssignableFrom(JNIEnv *env, jclass clazz1, jclass clazz2);

VMEXPORT jclass JNICALL GetSuperclass(JNIEnv *env, jclass clazz);

VMEXPORT jobject JNICALL NewGlobalRef(JNIEnv *env, jobject obj);
VMEXPORT void JNICALL DeleteGlobalRef(JNIEnv *env, jobject globalRef);
VMEXPORT void JNICALL DeleteLocalRef(JNIEnv *env, jobject localRef);

VMEXPORT jobject JNICALL AllocObject(JNIEnv *env, jclass clazz);


VMEXPORT jobject JNICALL NewObject(JNIEnv *env,
                          jclass clazz,
                          jmethodID methodID,
                          ...);
VMEXPORT jobject JNICALL NewObjectV(JNIEnv *env,
                           jclass clazz,
                           jmethodID methodID,
                           va_list args);
VMEXPORT jobject JNICALL NewObjectA(JNIEnv *env,
                           jclass clazz,
                           jmethodID methodID,
                           jvalue *args);

VMEXPORT jstring JNICALL NewString(JNIEnv *env,
                  const jchar *unicodeChars,
                  jsize length);

VMEXPORT jstring JNICALL NewStringUTF(JNIEnv *env,
                     const char *bytes);

VMEXPORT jboolean JNICALL IsSameObject(JNIEnv *env,
                      jobject ref1,
                      jobject ref2);

VMEXPORT jint JNICALL PushLocalFrame(JNIEnv *env, jint cap);
VMEXPORT jobject JNICALL PopLocalFrame(JNIEnv *env, jobject res);

VMEXPORT jobject JNICALL NewLocalRef(JNIEnv *env, jobject ref);
VMEXPORT jint JNICALL EnsureLocalCapacity(JNIEnv *env,
                      jint capacity);

VMEXPORT jboolean JNICALL IsInstanceOf(JNIEnv *env,
                      jobject obj,
                      jclass clazz);

VMEXPORT jfieldID JNICALL GetFieldID(JNIEnv *env,
                    jclass clazz,
                    const char *name,
                    const char *sig);

VMEXPORT jfieldID JNICALL GetStaticFieldID(JNIEnv *env,
                          jclass clazz,
                          const char *name,
                          const char *sig);

VMEXPORT jmethodID JNICALL GetMethodID(JNIEnv *env,
                      jclass clazz,
                      const char *name,
                      const char *sig);


VMEXPORT jmethodID JNICALL GetStaticMethodID(JNIEnv *env,
                            jclass clazz,
                            const char *name,
                            const char *sig);


// This function is not part of the standard JNI interface, but we can use
// it internally.  It is faster and, IMHO, more convenient to use.
// 20020923: This method looks up the class using the bootstrap loader,
// which is incorrect
VMEXPORT jfieldID JNICALL GetFieldID_Quick(JNIEnv *env,
                          const char *class_name,
                          const char *field_name,
                          const char *sig);

VMEXPORT jboolean    JNICALL GetBooleanField (JNIEnv *env, jobject obj, jfieldID fieldID);
VMEXPORT jbyte       JNICALL GetByteField    (JNIEnv *env, jobject obj, jfieldID fieldID);
VMEXPORT jchar       JNICALL GetCharField    (JNIEnv *env, jobject obj, jfieldID fieldID);
VMEXPORT jshort      JNICALL GetShortField   (JNIEnv *env, jobject obj, jfieldID fieldID);
VMEXPORT jint        JNICALL GetIntField     (JNIEnv *env, jobject obj, jfieldID fieldID);
VMEXPORT jlong       JNICALL GetLongField    (JNIEnv *env, jobject obj, jfieldID fieldID);
VMEXPORT jfloat      JNICALL GetFloatField   (JNIEnv *env, jobject obj, jfieldID fieldID);
VMEXPORT jdouble     JNICALL GetDoubleField  (JNIEnv *env, jobject obj, jfieldID fieldID);
VMEXPORT jobject     JNICALL GetObjectField  (JNIEnv *env, jobject obj, jfieldID fieldID);


// Nonstandard
VMEXPORT jboolean    JNICALL GetBooleanFieldOffset (JNIEnv *env, jobject obj, jint offset);
VMEXPORT jbyte       JNICALL GetByteFieldOffset    (JNIEnv *env, jobject obj, jint offset);
VMEXPORT jchar       JNICALL GetCharFieldOffset    (JNIEnv *env, jobject obj, jint offset);
VMEXPORT jshort      JNICALL GetShortFieldOffset   (JNIEnv *env, jobject obj, jint offset);
VMEXPORT jint        JNICALL GetIntFieldOffset     (JNIEnv *env, jobject obj, jint offset);
VMEXPORT jlong       JNICALL GetLongFieldOffset    (JNIEnv *env, jobject obj, jint offset);
VMEXPORT jfloat      JNICALL GetFloatFieldOffset   (JNIEnv *env, jobject obj, jint offset);
VMEXPORT jdouble     JNICALL GetDoubleFieldOffset  (JNIEnv *env, jobject obj, jint offset);
VMEXPORT jobject     JNICALL GetObjectFieldOffset  (JNIEnv *env, jobject obj, jint offset);


VMEXPORT void JNICALL SetBooleanField (JNIEnv *env, jobject obj, jfieldID fieldID, jboolean value);
VMEXPORT void JNICALL SetByteField    (JNIEnv *env, jobject obj, jfieldID fieldID, jbyte value);
VMEXPORT void JNICALL SetCharField    (JNIEnv *env, jobject obj, jfieldID fieldID, jchar value);
VMEXPORT void JNICALL SetShortField   (JNIEnv *env, jobject obj, jfieldID fieldID, jshort value);
VMEXPORT void JNICALL SetIntField     (JNIEnv *env, jobject obj, jfieldID fieldID, jint value);
VMEXPORT void JNICALL SetLongField    (JNIEnv *env, jobject obj, jfieldID fieldID, jlong value);
VMEXPORT void JNICALL SetFloatField   (JNIEnv *env, jobject obj, jfieldID fieldID, jfloat value);
VMEXPORT void JNICALL SetDoubleField  (JNIEnv *env, jobject obj, jfieldID fieldID, jdouble value);
VMEXPORT void JNICALL SetObjectField  (JNIEnv *env, jobject obj, jfieldID fieldID, jobject value);


// Nonstandard
VMEXPORT void JNICALL SetBooleanFieldOffset (JNIEnv *env, jobject obj, jint offset, jboolean value);
VMEXPORT void JNICALL SetByteFieldOffset    (JNIEnv *env, jobject obj, jint offset, jbyte value);
VMEXPORT void JNICALL SetCharFieldOffset    (JNIEnv *env, jobject obj, jint offset, jchar value);
VMEXPORT void JNICALL SetShortFieldOffset   (JNIEnv *env, jobject obj, jint offset, jshort value);
VMEXPORT void JNICALL SetIntFieldOffset     (JNIEnv *env, jobject obj, jint offset, jint value);
VMEXPORT void JNICALL SetLongFieldOffset    (JNIEnv *env, jobject obj, jint offset, jlong value);
VMEXPORT void JNICALL SetFloatFieldOffset   (JNIEnv *env, jobject obj, jint offset, jfloat value);
VMEXPORT void JNICALL SetDoubleFieldOffset  (JNIEnv *env, jobject obj, jint offset, jdouble value);
VMEXPORT void JNICALL SetObjectFieldOffset  (JNIEnv *env, jobject obj, jint offset, jobject value);


VMEXPORT jboolean    JNICALL GetStaticBooleanField (JNIEnv *env, jclass clazz, jfieldID fieldID); 
VMEXPORT jbyte       JNICALL GetStaticByteField    (JNIEnv *env, jclass clazz, jfieldID fieldID); 
VMEXPORT jchar       JNICALL GetStaticCharField    (JNIEnv *env, jclass clazz, jfieldID fieldID); 
VMEXPORT jshort      JNICALL GetStaticShortField   (JNIEnv *env, jclass clazz, jfieldID fieldID); 
VMEXPORT jint        JNICALL GetStaticIntField     (JNIEnv *env, jclass clazz, jfieldID fieldID); 
VMEXPORT jlong       JNICALL GetStaticLongField    (JNIEnv *env, jclass clazz, jfieldID fieldID); 
VMEXPORT jfloat      JNICALL GetStaticFloatField   (JNIEnv *env, jclass clazz, jfieldID fieldID); 
VMEXPORT jdouble     JNICALL GetStaticDoubleField  (JNIEnv *env, jclass clazz, jfieldID fieldID); 
VMEXPORT jobject     JNICALL GetStaticObjectField  (JNIEnv *env, jclass clazz, jfieldID fieldID); 

VMEXPORT void JNICALL SetStaticBooleanField (JNIEnv *env, jclass clazz, jfieldID fieldID, jboolean value);
VMEXPORT void JNICALL SetStaticByteField    (JNIEnv *env, jclass clazz, jfieldID fieldID, jbyte value);
VMEXPORT void JNICALL SetStaticCharField    (JNIEnv *env, jclass clazz, jfieldID fieldID, jchar value);
VMEXPORT void JNICALL SetStaticShortField   (JNIEnv *env, jclass clazz, jfieldID fieldID, jshort value);
VMEXPORT void JNICALL SetStaticIntField     (JNIEnv *env, jclass clazz, jfieldID fieldID, jint value);
VMEXPORT void JNICALL SetStaticLongField    (JNIEnv *env, jclass clazz, jfieldID fieldID, jlong value);
VMEXPORT void JNICALL SetStaticFloatField   (JNIEnv *env, jclass clazz, jfieldID fieldID, jfloat value);
VMEXPORT void JNICALL SetStaticDoubleField  (JNIEnv *env, jclass clazz, jfieldID fieldID, jdouble value);
VMEXPORT void JNICALL SetStaticObjectField  (JNIEnv *env, jclass clazz, jfieldID fieldID, jobject value);

VMEXPORT jsize JNICALL GetArrayLength(JNIEnv *env, jarray array);

VMEXPORT jboolean * JNICALL GetBooleanArrayElements (JNIEnv *env, jbooleanArray array, jboolean *isCopy); 
VMEXPORT jbyte    * JNICALL GetByteArrayElements    (JNIEnv *env, jbyteArray array,    jboolean *isCopy); 
VMEXPORT jchar    * JNICALL GetCharArrayElements    (JNIEnv *env, jcharArray array,    jboolean *isCopy); 
VMEXPORT jshort   * JNICALL GetShortArrayElements   (JNIEnv *env, jshortArray array,   jboolean *isCopy); 
VMEXPORT jint     * JNICALL GetIntArrayElements     (JNIEnv *env, jintArray array,     jboolean *isCopy); 
VMEXPORT jlong    * JNICALL GetLongArrayElements    (JNIEnv *env, jlongArray array,    jboolean *isCopy); 
VMEXPORT jfloat   * JNICALL GetFloatArrayElements   (JNIEnv *env, jfloatArray array,   jboolean *isCopy); 
VMEXPORT jdouble  * JNICALL GetDoubleArrayElements  (JNIEnv *env, jdoubleArray array,  jboolean *isCopy); 


VMEXPORT jobject JNICALL GetObjectArrayElement(JNIEnv *env, jobjectArray array, jsize index);

VMEXPORT void JNICALL SetObjectArrayElement(JNIEnv *env, jobjectArray array, jsize index, jobject value);


VMEXPORT void JNICALL ReleaseBooleanArrayElements(JNIEnv *env, jbooleanArray array, jboolean *elems, jint mode);
VMEXPORT void JNICALL ReleaseByteArrayElements   (JNIEnv *env, jbyteArray array,    jbyte *elems,    jint mode);
VMEXPORT void JNICALL ReleaseCharArrayElements   (JNIEnv *env, jcharArray array,    jchar *elems,    jint mode);
VMEXPORT void JNICALL ReleaseShortArrayElements  (JNIEnv *env, jshortArray array,   jshort *elems,   jint mode);
VMEXPORT void JNICALL ReleaseIntArrayElements    (JNIEnv *env, jintArray array,     jint *elems,     jint mode);
VMEXPORT void JNICALL ReleaseLongArrayElements   (JNIEnv *env, jlongArray array,    jlong *elems,    jint mode);
VMEXPORT void JNICALL ReleaseFloatArrayElements  (JNIEnv *env, jfloatArray array,   jfloat *elems,   jint mode);
VMEXPORT void JNICALL ReleaseDoubleArrayElements (JNIEnv *env, jdoubleArray array,  jdouble *elems,  jint mode);


VMEXPORT void JNICALL GetBooleanArrayRegion (JNIEnv *env, jobjectArray array,
                            jsize start, jsize len,
                            jboolean *buf);
VMEXPORT void JNICALL GetByteArrayRegion    (JNIEnv *env, jobjectArray array,
                            jsize start, jsize len,
                            jbyte *buf);
VMEXPORT void JNICALL GetCharArrayRegion    (JNIEnv *env, jobjectArray array, 
                            jsize start, jsize len,
                            jchar *buf);
VMEXPORT void JNICALL GetShortArrayRegion   (JNIEnv *env, jobjectArray array, 
                            jsize start, jsize len,
                            jshort *buf);
VMEXPORT void JNICALL GetIntArrayRegion     (JNIEnv *env, jobjectArray array,
                            jsize start, jsize len,
                            jint *buf);
VMEXPORT void JNICALL GetLongArrayRegion    (JNIEnv *env, jobjectArray array,
                            jsize start, jsize len,
                            jlong *buf);
VMEXPORT void JNICALL GetFloatArrayRegion   (JNIEnv *env, jobjectArray array,
                            jsize start, jsize len,
                            jfloat *buf);
VMEXPORT void JNICALL GetDoubleArrayRegion  (JNIEnv *env, jobjectArray array,
                            jsize start, jsize len,
                            jdouble *buf);

VMEXPORT void JNICALL SetBooleanArrayRegion (JNIEnv *env, jobjectArray array,
                            jsize start, jsize len,
                            jboolean *buf);
VMEXPORT void JNICALL SetByteArrayRegion    (JNIEnv *env, jobjectArray array,
                            jsize start, jsize len,
                            jbyte *buf);
VMEXPORT void JNICALL SetCharArrayRegion    (JNIEnv *env, jobjectArray array,
                            jsize start, jsize len,
                            jchar *buf);
VMEXPORT void JNICALL SetShortArrayRegion   (JNIEnv *env, jobjectArray array,
                            jsize start, jsize len,
                            jshort *buf);
VMEXPORT void JNICALL SetIntArrayRegion     (JNIEnv *env, jobjectArray array,
                            jsize start, jsize len,
                            jint *buf);
VMEXPORT void JNICALL SetLongArrayRegion    (JNIEnv *env, jobjectArray array,
                            jsize start, jsize len,
                            jlong *buf);
VMEXPORT void JNICALL SetFloatArrayRegion   (JNIEnv *env, jobjectArray array,
                            jsize start, jsize len,
                            jfloat *buf);
VMEXPORT void JNICALL SetDoubleArrayRegion  (JNIEnv *env, jobjectArray array,
                            jsize start, jsize len,
                            jdouble *buf);

VMEXPORT jbooleanArray JNICALL NewBooleanArray (JNIEnv *env, jsize length);
VMEXPORT jbyteArray    JNICALL NewByteArray    (JNIEnv *env, jsize length);
VMEXPORT jcharArray    JNICALL NewCharArray    (JNIEnv *env, jsize length);
VMEXPORT jshortArray   JNICALL NewShortArray   (JNIEnv *env, jsize length);
VMEXPORT jintArray     JNICALL NewIntArray     (JNIEnv *env, jsize length);
VMEXPORT jlongArray    JNICALL NewLongArray    (JNIEnv *env, jsize length);
VMEXPORT jfloatArray   JNICALL NewFloatArray   (JNIEnv *env, jsize length);
VMEXPORT jdoubleArray  JNICALL NewDoubleArray  (JNIEnv *env, jsize length);

VMEXPORT jarray JNICALL NewObjectArray(JNIEnv *env,
                      jsize length,
                      jclass elementClass,
                      jobject initialElement);


VMEXPORT jsize       JNICALL GetStringLength(JNIEnv *env, jstring string);
VMEXPORT const jchar * JNICALL GetStringChars(JNIEnv *env, jstring string, jboolean *isCopy);
VMEXPORT void      JNICALL ReleaseStringChars(JNIEnv *env, jstring string, const jchar *chars);

VMEXPORT jsize       JNICALL GetStringUTFLength(JNIEnv *env, jstring string);
VMEXPORT const char * JNICALL GetStringUTFChars(JNIEnv *env, jstring string, jboolean *isCopy);
VMEXPORT void     JNICALL ReleaseStringUTFChars(JNIEnv *env, jstring string, const char *utf);


VMEXPORT void     JNICALL CallVoidMethod     (JNIEnv *env, jobject obj, jmethodID methodID, ...);
VMEXPORT void     JNICALL CallVoidMethodV    (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
VMEXPORT void     JNICALL CallVoidMethodA    (JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
VMEXPORT jobject  JNICALL CallObjectMethod   (JNIEnv *env, jobject obj, jmethodID methodID, ...);
VMEXPORT jobject  JNICALL CallObjectMethodV  (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
VMEXPORT jobject  JNICALL CallObjectMethodA  (JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
VMEXPORT jboolean JNICALL CallBooleanMethod  (JNIEnv *env, jobject obj, jmethodID methodID, ...);
VMEXPORT jboolean JNICALL CallBooleanMethodV (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
VMEXPORT jboolean JNICALL CallBooleanMethodA (JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
VMEXPORT jbyte    JNICALL CallByteMethod     (JNIEnv *env, jobject obj, jmethodID methodID, ...);
VMEXPORT jbyte    JNICALL CallByteMethodV    (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
VMEXPORT jbyte    JNICALL CallByteMethodA    (JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
VMEXPORT jchar    JNICALL CallCharMethod     (JNIEnv *env, jobject obj, jmethodID methodID, ...);
VMEXPORT jchar    JNICALL CallCharMethodV    (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
VMEXPORT jchar    JNICALL CallCharMethodA    (JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
VMEXPORT jshort   JNICALL CallShortMethod    (JNIEnv *env, jobject obj, jmethodID methodID, ...);
VMEXPORT jshort   JNICALL CallShortMethodV   (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
VMEXPORT jshort   JNICALL CallShortMethodA   (JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
VMEXPORT jint     JNICALL CallIntMethod      (JNIEnv *env, jobject obj, jmethodID methodID, ...);
VMEXPORT jint     JNICALL CallIntMethodV     (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
VMEXPORT jint     JNICALL CallIntMethodA     (JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
VMEXPORT jlong    JNICALL CallLongMethod     (JNIEnv *env, jobject obj, jmethodID methodID, ...);
VMEXPORT jlong    JNICALL CallLongMethodV    (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
VMEXPORT jlong    JNICALL CallLongMethodA    (JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
VMEXPORT jfloat   JNICALL CallFloatMethod    (JNIEnv *env, jobject obj, jmethodID methodID, ...);
VMEXPORT jfloat   JNICALL CallFloatMethodV   (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
VMEXPORT jfloat   JNICALL CallFloatMethodA   (JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);
VMEXPORT jdouble  JNICALL CallDoubleMethod   (JNIEnv *env, jobject obj, jmethodID methodID, ...);
VMEXPORT jdouble  JNICALL CallDoubleMethodV  (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
VMEXPORT jdouble  JNICALL CallDoubleMethodA  (JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);



VMEXPORT void JNICALL CallNonvirtualVoidMethod(JNIEnv *env,
                                       jobject obj,
                                       jclass clazz,
                                       jmethodID methodID,
                                       ...);
VMEXPORT void JNICALL CallNonvirtualVoidMethodV(JNIEnv *env,
                                       jobject obj,
                                       jclass clazz,
                                       jmethodID methodID,
                                       va_list args);
VMEXPORT void JNICALL CallNonvirtualVoidMethodA(JNIEnv *env,
                                       jobject obj,
                                       jclass clazz,
                                       jmethodID methodID,
                                       jvalue *args);
VMEXPORT jobject JNICALL CallNonvirtualObjectMethod(JNIEnv *env,
                                           jobject obj,
                                           jclass clazz,
                                           jmethodID methodID,
                                           ...);
VMEXPORT jobject JNICALL CallNonvirtualObjectMethodV(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            va_list args);
VMEXPORT jobject JNICALL CallNonvirtualObjectMethodA(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            jvalue *args);
VMEXPORT jboolean JNICALL CallNonvirtualBooleanMethod(JNIEnv *env,
                                             jobject obj,
                                             jclass clazz,
                                             jmethodID methodID,
                                             ...);
VMEXPORT jboolean JNICALL CallNonvirtualBooleanMethodV(JNIEnv *env,
                                              jobject obj,
                                              jclass clazz,
                                              jmethodID methodID,
                                              va_list args);
VMEXPORT jboolean JNICALL CallNonvirtualBooleanMethodA(JNIEnv *env,
                                              jobject obj,
                                              jclass clazz,
                                              jmethodID methodID,
                                              jvalue *args);
VMEXPORT jbyte JNICALL CallNonvirtualByteMethod(JNIEnv *env,
                                           jobject obj,
                                           jclass clazz,
                                           jmethodID methodID,
                                           ...);
VMEXPORT jbyte JNICALL CallNonvirtualByteMethodV(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            va_list args);
VMEXPORT jbyte JNICALL CallNonvirtualByteMethodA(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            jvalue *args);
VMEXPORT jchar JNICALL CallNonvirtualCharMethod(JNIEnv *env,
                                           jobject obj,
                                           jclass clazz,
                                           jmethodID methodID,
                                           ...);
VMEXPORT jchar JNICALL CallNonvirtualCharMethodV(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            va_list args);
VMEXPORT jchar JNICALL CallNonvirtualCharMethodA(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            jvalue *args);
VMEXPORT jshort JNICALL CallNonvirtualShortMethod(JNIEnv *env,
                                           jobject obj,
                                           jclass clazz,
                                           jmethodID methodID,
                                           ...);
VMEXPORT jshort JNICALL CallNonvirtualShortMethodV(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            va_list args);
VMEXPORT jshort JNICALL CallNonvirtualShortMethodA(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            jvalue *args);
VMEXPORT jint JNICALL CallNonvirtualIntMethod(JNIEnv *env,
                                           jobject obj,
                                           jclass clazz,
                                           jmethodID methodID,
                                           ...);
VMEXPORT jint JNICALL CallNonvirtualIntMethodV(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            va_list args);
VMEXPORT jint JNICALL CallNonvirtualIntMethodA(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            jvalue *args);
VMEXPORT jlong JNICALL CallNonvirtualLongMethod(JNIEnv *env,
                                           jobject obj,
                                           jclass clazz,
                                           jmethodID methodID,
                                           ...);
VMEXPORT jlong JNICALL CallNonvirtualLongMethodV(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            va_list args);
VMEXPORT jlong JNICALL CallNonvirtualLongMethodA(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            jvalue *args);
VMEXPORT jfloat JNICALL CallNonvirtualFloatMethod(JNIEnv *env,
                                           jobject obj,
                                           jclass clazz,
                                           jmethodID methodID,
                                           ...);
VMEXPORT jfloat JNICALL CallNonvirtualFloatMethodV(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            va_list args);
VMEXPORT jfloat JNICALL CallNonvirtualFloatMethodA(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            jvalue *args);
VMEXPORT jdouble JNICALL CallNonvirtualDoubleMethod(JNIEnv *env,
                                           jobject obj,
                                           jclass clazz,
                                           jmethodID methodID,
                                           ...);
VMEXPORT jdouble JNICALL CallNonvirtualDoubleMethodV(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            va_list args);
VMEXPORT jdouble JNICALL CallNonvirtualDoubleMethodA(JNIEnv *env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            jvalue *args);


VMEXPORT void JNICALL 
CallStaticVoidMethod     (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
VMEXPORT void JNICALL 
CallStaticVoidMethodV    (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
VMEXPORT void JNICALL 
CallStaticVoidMethodA    (JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
VMEXPORT jobject JNICALL 
CallStaticObjectMethod   (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
VMEXPORT jobject JNICALL 
CallStaticObjectMethodV  (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
VMEXPORT jobject JNICALL 
CallStaticObjectMethodA  (JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
VMEXPORT jboolean JNICALL 
CallStaticBooleanMethod  (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
VMEXPORT jboolean JNICALL 
CallStaticBooleanMethodV (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
VMEXPORT jboolean JNICALL 
CallStaticBooleanMethodA (JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
VMEXPORT jbyte JNICALL 
CallStaticByteMethod     (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
VMEXPORT jbyte JNICALL 
CallStaticByteMethodV    (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
VMEXPORT jbyte JNICALL 
CallStaticByteMethodA    (JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
VMEXPORT jchar JNICALL 
CallStaticCharMethod     (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
VMEXPORT jchar JNICALL 
CallStaticCharMethodV    (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
VMEXPORT jchar JNICALL 
CallStaticCharMethodA    (JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
VMEXPORT jshort JNICALL 
CallStaticShortMethod    (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
VMEXPORT jshort JNICALL 
CallStaticShortMethodV   (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
VMEXPORT jshort JNICALL 
CallStaticShortMethodA   (JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
VMEXPORT jint JNICALL 
CallStaticIntMethod      (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
VMEXPORT jint JNICALL 
CallStaticIntMethodV     (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
VMEXPORT jint JNICALL 
CallStaticIntMethodA     (JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
VMEXPORT jlong JNICALL 
CallStaticLongMethod     (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
VMEXPORT jlong JNICALL 
CallStaticLongMethodV    (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
VMEXPORT jlong JNICALL 
CallStaticLongMethodA    (JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
VMEXPORT jfloat JNICALL 
CallStaticFloatMethod    (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
VMEXPORT jfloat JNICALL 
CallStaticFloatMethodV   (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
VMEXPORT jfloat JNICALL 
CallStaticFloatMethodA   (JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);
VMEXPORT jdouble JNICALL 
CallStaticDoubleMethod   (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
VMEXPORT jdouble JNICALL 
CallStaticDoubleMethodV  (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
VMEXPORT jdouble JNICALL 
CallStaticDoubleMethodA  (JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);


VMEXPORT jint JNICALL Throw (JNIEnv *env, jthrowable obj);
VMEXPORT jint JNICALL ThrowNew(JNIEnv *env, jclass clazz, const char *message);
VMEXPORT jint JNICALL ThrowNew_Quick (JNIEnv *env, const char *classname, const char *message);
VMEXPORT jthrowable JNICALL ExceptionOccurred(JNIEnv *env);
VMEXPORT void JNICALL ExceptionClear(JNIEnv *env);
VMEXPORT void JNICALL ExceptionDescribe(JNIEnv *env);
VMEXPORT void JNICALL FatalError(JNIEnv *env, const char *msg);


VMEXPORT jint JNICALL RegisterNatives(JNIEnv *env,
                             jclass clazz,
                             const JNINativeMethod *methods,
                             jint nMethods);
VMEXPORT jint JNICALL UnregisterNatives(JNIEnv *env, jclass clazz);

VMEXPORT jint JNICALL MonitorEnter(JNIEnv *env, jobject obj);
VMEXPORT jint JNICALL MonitorExit(JNIEnv *env, jobject obj);
VMEXPORT jint JNICALL GetJavaVM(JNIEnv *env, JavaVM **vm);

VMEXPORT void JNICALL GetStringRegion(JNIEnv *env, jstring s, jsize off, jsize len, jchar *b);
VMEXPORT void JNICALL GetStringUTFRegion(JNIEnv *env, jstring s, jsize off, jsize len, char *b);

VMEXPORT void* JNICALL GetPrimitiveArrayCritical(JNIEnv *env, jarray array, jboolean* isCopy);
VMEXPORT void JNICALL ReleasePrimitiveArrayCritical(JNIEnv *env, jarray array, void* carray, jint mode);

VMEXPORT const jchar* JNICALL GetStringCritical(JNIEnv *env, jstring s, jboolean* isCopy);
VMEXPORT void JNICALL ReleaseStringCritical(JNIEnv *env, jstring s, const jchar* cstr);

VMEXPORT jweak JNICALL NewWeakGlobalRef(JNIEnv *env, jobject obj);
VMEXPORT void JNICALL DeleteWeakGlobalRef(JNIEnv *env, jweak obj);

VMEXPORT jboolean JNICALL ExceptionCheck(JNIEnv *env);

VMEXPORT jmethodID JNICALL FromReflectedMethod(JNIEnv *env, jobject method);
VMEXPORT jfieldID JNICALL FromReflectedField(JNIEnv *env, jobject field);
VMEXPORT jobject JNICALL ToReflectedMethod(JNIEnv *env, jclass cls, jmethodID methodID, jboolean isStatic);
VMEXPORT jobject JNICALL ToReflectedField(JNIEnv *env, jclass cls, jfieldID fieldID, jboolean isStatic);

// JNI NIO functions are imported from classlib thus no direct access to them
//VMEXPORT jobject JNICALL NewDirectByteBuffer(JNIEnv* env, void* address, jlong capacity);
//VMEXPORT void* JNICALL GetDirectBufferAddress(JNIEnv* env, jobject buf);
//VMEXPORT jlong JNICALL GetDirectBufferCapacity(JNIEnv* env, jobject buf);


VMEXPORT jint JNICALL DestroyJavaVM(JavaVM*);

VMEXPORT jint JNICALL AttachCurrentThread(JavaVM*, void** penv, void* args);
VMEXPORT jint JNICALL DetachCurrentThread(JavaVM*);

VMEXPORT jint JNICALL GetEnv(JavaVM*, void** penv, jint ver);

VMEXPORT jint JNICALL AttachCurrentThreadAsDaemon(JavaVM*, void** penv, void* args);

#endif
