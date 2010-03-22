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


#ifndef _JNI_UTILS_H_
#define _JNI_UTILS_H_

#include "object_handles.h"
#include "jni_direct.h"
#include "ini.h"
#include "String_Pool.h"

#ifdef __cplusplus
extern "C" {
#endif

VMEXPORT Class_Handle jni_get_class_handle(JNIEnv*, jclass);
VMEXPORT jclass jni_class_from_handle(JNIEnv*, Class_Handle);
VMEXPORT jobject jni_class_loader_from_handle(JNIEnv*, Class_Loader_Handle);
Class_Loader_Handle class_loader_lookup(jobject loader);
void class_loader_load_native_lib(const char* lib, Class_Loader_Handle loader);

VMEXPORT jvalue *get_jvalue_arg_array(Method *method, va_list args);

VMEXPORT void throw_exception_from_jni(JNIEnv* jenv, const char* exc, const char* msg);

VMEXPORT void array_copy_jni(JNIEnv*, jobject src, jint src_off, jobject dst, jint dst_off, jint count);

VMEXPORT Field* LookupField (Class*, const char*);
VMEXPORT Method* LookupMethod (Class*, const char*, const char*);

VMEXPORT char* ParameterTypesToMethodSignature (JNIEnv*, jobjectArray, const char *name);
VMEXPORT char PrimitiveNameToSignature (const char*);

#ifdef __cplusplus
}
#endif

bool ensure_initialised(JNIEnv* jenv, Class* clss);

jobject create_default_instance(Class* clss);

jobject CreateNewThrowable(JNIEnv* jenv, Class* clazz, const char * message, jthrowable cause);

jclass* GetMethodParameterTypes (JNIEnv*, const char*, int*);
jclass* GetMethodParameterTypes (JNIEnv*, const char*, int*, ClassLoader*);

VMEXPORT jclass SignatureToClass (JNIEnv*, const char*);
VMEXPORT jclass SignatureToClass (JNIEnv*, const char*, ClassLoader *loader);

char* GetClassSignature (Class*);
size_t GetClassSignatureLength (Class*);
void GetClassSignature (Class*, char*);


char* PrimitiveSignatureToName (const char sig);
void PrimitiveSignatureToName (const char sig, char *classname);

const char* SignatureToName (const char* sig);
void SignatureToName (const char* sig, char *name);

jclass FindClassWithClassLoader(JNIEnv* env_ext, const char *name, ClassLoader *loader);
jclass FindClassWithClassLoader(JNIEnv* env_ext, String*name, ClassLoader *loader);

Field* LookupDeclaredField (Class *clss, const char *name);
Method* LookupDeclaredMethod (Class *clss, const char *mname, const char *msig);

// internal VM function that provide Class searching functionality using 
// String* parameter (instead of const char* in JNI FindClass)
jclass FindClass(JNIEnv* env_ext, String* name);

JavaVM * jni_get_java_vm(JNIEnv * jni_env);
Global_Env * jni_get_vm_env(JNIEnv * jni_env);

/* global handles */
extern ObjectHandle gh_jlc;         //java.lang.Class
extern ObjectHandle gh_jls;         //java.lang.String
extern ObjectHandle gh_jlcloneable; //java.lang.Clonable
extern ObjectHandle gh_aoboolean;   //[Z
extern ObjectHandle gh_aobyte;      //[B
extern ObjectHandle gh_aochar;      //[C
extern ObjectHandle gh_aoshort;     //[S
extern ObjectHandle gh_aoint;       //[I
extern ObjectHandle gh_aolong;      //[J    
extern ObjectHandle gh_aofloat;     //[F
extern ObjectHandle gh_aodouble;    //[D

extern ObjectHandle gh_jlboolean;   //java.lang.Boolean
extern ObjectHandle gh_jlbyte;      //java.lang.Byte
extern ObjectHandle gh_jlchar;      //java.lang.Character
extern ObjectHandle gh_jlshort;     //java.lang.Short
extern ObjectHandle gh_jlint;       //java.lang.Integer
extern ObjectHandle gh_jllong;      //java.lang.Long
extern ObjectHandle gh_jlfloat;     //java.lang.Float
extern ObjectHandle gh_jldouble;    //java.lang.Double

extern jfieldID gid_boolean_value;  //java.lang.Boolean's field: value;
extern jfieldID gid_byte_value;     //java.lang.Byte's field: value;
extern jfieldID gid_char_value;     //java.lang.Character's field: value;
extern jfieldID gid_short_value;    //java.lang.Short's field: value;
extern jfieldID gid_int_value;      //java.lang.Integer's field: value;
extern jfieldID gid_long_value;     //java.lang.Long's field: value;
extern jfieldID gid_float_value;    //java.lang.Float's field: value;
extern jfieldID gid_double_value;   //java.lang.Double's field: value;

extern jfieldID  gid_throwable_traceinfo; //java.lang.Throwable's field: String traceinfo;

extern jfieldID gid_string_field_value; //java.lang.String's field: char[] value;
extern jfieldID gid_string_field_bvalue; //java.lang.String's optional field: byte[] bvalue;
extern jfieldID gid_string_field_offset;//java.lang.String's field: int offset;
extern jfieldID gid_string_field_count; //java.lang.String's field: int count;

extern jmethodID gid_stringinit;      //method ID of java.lang.String's "<init>([Z)V"
extern jmethodID gid_doubleisNaN;     //method ID of java.lang.Double's "isNaN(D)Z"
extern jdouble  gc_double_POSITIVE_INFINITY; //java.lang.Double's const: POSITIVE_INFINITY
extern jdouble  gc_double_NEGATIVE_INFINITY; //java.lang.Double's const: NEGATIVE_INFINITY

#endif
