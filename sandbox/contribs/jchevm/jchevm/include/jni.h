
/*
 * Copyright 2005 The Apache Software Foundation or its licensors,
 * as applicable.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * $Id: jni.h,v 1.7 2005/02/27 04:52:59 archiecobbs Exp $
 */

#ifndef _JNI_H_
#define _JNI_H_

#include <stdarg.h>
#include "jni_machdep.h"

/************************************************************************
 *			      Definitions				*
 ************************************************************************/

#define JNI_FALSE	((int)0)
#define JNI_TRUE	1

#define JNI_COMMIT	1
#define JNI_ABORT	2

#define JNI_VERSION_1_1	0x00010001
#define JNI_VERSION_1_2	0x00010002
#define JNI_VERSION_1_4	0x00010004

#define JNI_OK		((int)0)
#define JNI_ERR		(-1)
#define JNI_EDETACHED	(-2)
#define JNI_EVERSION	(-3)

#define JNICALL
#define JNIEXPORT

/************************************************************************
 *				Typedefs				*
 ************************************************************************/

#if _JC_VIRTUAL_MACHINE

typedef struct _jc_object		**jobject;
typedef struct _jc_object		**jstring;
typedef struct _jc_object		**jthrowable;
typedef struct _jc_object		**jweak;
typedef struct _jc_object		**jclass;
typedef struct _jc_array		**jarray;
typedef struct _jc_boolean_array	**jbooleanArray;
typedef struct _jc_byte_array		**jbyteArray;
typedef struct _jc_char_array		**jcharArray;
typedef struct _jc_short_array		**jshortArray;
typedef struct _jc_int_array		**jintArray;
typedef struct _jc_long_array		**jlongArray;
typedef struct _jc_float_array		**jfloatArray;
typedef struct _jc_double_array		**jdoubleArray;
typedef struct _jc_object_array		**jobjectArray;
typedef struct _jc_field		*jfieldID;
typedef struct _jc_method		*jmethodID;

typedef struct JNINativeInterface	*JNIEnv;
typedef struct JNIInvokeInterface	*JavaVM;

#else	/* !_JC_VIRTUAL_MACHINE */

typedef const struct _jc_jni_object	*jobject;
typedef jobject				jstring;
typedef jobject				jthrowable;
typedef jobject				jweak;
typedef jobject				jclass;
typedef jobject				jarray;
typedef jobject				jbooleanArray;
typedef jobject				jbyteArray;
typedef jobject				jcharArray;
typedef jobject				jshortArray;
typedef jobject				jintArray;
typedef jobject				jlongArray;
typedef jobject				jfloatArray;
typedef jobject				jdoubleArray;
typedef jobject				jobjectArray;
typedef const struct _jc_jni_field	*jfieldID;
typedef const struct _jc_jni_method	*jmethodID;

typedef const struct JNINativeInterface *JNIEnv;
typedef const struct JNIInvokeInterface *JavaVM;

#endif	/* !_JC_VIRTUAL_MACHINE */

typedef jint jsize;

union jvalue {
	jboolean	z;
	jbyte		b;
	jchar		c;
	jshort		s;
	jint		i;
	jlong		j;
	jfloat		f;
	jdouble		d;
	jobject		l;
};
typedef union jvalue jvalue;

struct JNINativeMethod {
	char		*name;
	char		*signature;
	void		*fnPtr;
};
typedef struct JNINativeMethod JNINativeMethod;

struct JavaVMOption {
	char		*optionString;
	void		*extraInfo;
};
typedef struct JavaVMOption JavaVMOption;

struct JavaVMInitArgs {
	jint		version;
	jint		nOptions;
	JavaVMOption	*options;
	jboolean	ignoreUnrecognized;
};
typedef struct JavaVMInitArgs JavaVMInitArgs;

struct JavaVMAttachArgs {
	jint		version;
	char		*name;
	jobject		group;
};
typedef struct JavaVMAttachArgs JavaVMAttachArgs;

/* JNINativeInterface type */
struct JNINativeInterface {
  void *null_0;
  void *null_1;
  void *null_2;
  void *null_3;
  jint (JNICALL *GetVersion)(JNIEnv *env);	/* 4 */
  jclass (JNICALL *DefineClass)(JNIEnv *env, const char *name, jobject loader, const jbyte *buf, jsize bufLen);	/* 5 */
  jclass (JNICALL *FindClass)(JNIEnv *env, const char *name);	/* 6 */
  jmethodID (JNICALL *FromReflectedMethod)(JNIEnv *env, jobject method);	/* 7 */
  jfieldID (JNICALL *FromReflectedField)(JNIEnv *env, jobject field);	/* 8 */
  jobject (JNICALL *ToReflectedMethod)(JNIEnv *env, jclass cls, jmethodID methodID);	/* 9 */
  jclass (JNICALL *GetSuperclass)(JNIEnv *env, jclass clazz);	/* 10 */
  jboolean (JNICALL *IsAssignableFrom)(JNIEnv *env, jclass clazz1, jclass clazz2);	/* 11 */
  jobject (JNICALL *ToReflectedField)(JNIEnv *env, jclass cls, jfieldID fieldID);	/* 12 */
  jint (JNICALL *Throw)(JNIEnv *env, jthrowable obj);	/* 13 */
  jint (JNICALL *ThrowNew)(JNIEnv *env, jclass clazz, const char *message);	/* 14 */
  jthrowable (JNICALL *ExceptionOccurred)(JNIEnv *env);	/* 15 */
  void (JNICALL *ExceptionDescribe)(JNIEnv *env);	/* 16 */
  void (JNICALL *ExceptionClear)(JNIEnv *env);	/* 17 */
  void (JNICALL *FatalError)(JNIEnv *env, const char *msg);	/* 18 */
  jint (JNICALL *PushLocalFrame)(JNIEnv *env, jint capacity);	/* 19 */
  jobject (JNICALL *PopLocalFrame)(JNIEnv *env, jobject result);	/* 20 */
  jobject (JNICALL *NewGlobalRef)(JNIEnv *env, jobject obj);	/* 21 */
  void (JNICALL *DeleteGlobalRef)(JNIEnv *env, jobject gref);	/* 22 */
  void (JNICALL *DeleteLocalRef)(JNIEnv *env, jobject lref);	/* 23 */
  jboolean (JNICALL *IsSameObject)(JNIEnv *env, jobject ref1, jobject ref2);	/* 24 */
  jobject (JNICALL *NewLocalRef)(JNIEnv *env, jobject ref);	/* 25 */
  jint (JNICALL *EnsureLocalCapacity)(JNIEnv *env, jint capacity);	/* 26 */
  jobject (JNICALL *AllocObject)(JNIEnv *env, jclass clazz);	/* 27 */
  jobject (JNICALL *NewObject)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);	/* 28 */
  jobject (JNICALL *NewObjectV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);	/* 29 */
  jobject (JNICALL *NewObjectA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);	/* 30 */
  jclass (JNICALL *GetObjectClass)(JNIEnv *env, jobject obj);	/* 31 */
  jboolean (JNICALL *IsInstanceOf)(JNIEnv *env, jobject obj, jclass clazz);	/* 32 */
  jmethodID (JNICALL *GetMethodID)(JNIEnv *env, jclass clazz, const char *name, const char *sig);	/* 33 */
  jobject (JNICALL *CallObjectMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);	/* 34 */
  jobject (JNICALL *CallObjectMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);	/* 35 */
  jobject (JNICALL *CallObjectMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);	/* 36 */
  jboolean (JNICALL *CallBooleanMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);	/* 37 */
  jboolean (JNICALL *CallBooleanMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);	/* 38 */
  jboolean (JNICALL *CallBooleanMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);	/* 39 */
  jbyte (JNICALL *CallByteMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);	/* 40 */
  jbyte (JNICALL *CallByteMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);	/* 41 */
  jbyte (JNICALL *CallByteMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);	/* 42 */
  jchar (JNICALL *CallCharMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);	/* 43 */
  jchar (JNICALL *CallCharMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);	/* 44 */
  jchar (JNICALL *CallCharMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);	/* 45 */
  jshort (JNICALL *CallShortMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);	/* 46 */
  jshort (JNICALL *CallShortMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);	/* 47 */
  jshort (JNICALL *CallShortMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);	/* 48 */
  jint (JNICALL *CallIntMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);	/* 49 */
  jint (JNICALL *CallIntMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);	/* 50 */
  jint (JNICALL *CallIntMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);	/* 51 */
  jlong (JNICALL *CallLongMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);	/* 52 */
  jlong (JNICALL *CallLongMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);	/* 53 */
  jlong (JNICALL *CallLongMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);	/* 54 */
  jfloat (JNICALL *CallFloatMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);	/* 55 */
  jfloat (JNICALL *CallFloatMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);	/* 56 */
  jfloat (JNICALL *CallFloatMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);	/* 57 */
  jdouble (JNICALL *CallDoubleMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);	/* 58 */
  jdouble (JNICALL *CallDoubleMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);	/* 59 */
  jdouble (JNICALL *CallDoubleMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);	/* 60 */
  void (JNICALL *CallVoidMethod)(JNIEnv *env, jobject obj, jmethodID methodID, ...);	/* 61 */
  void (JNICALL *CallVoidMethodV)(JNIEnv *env, jobject obj, jmethodID methodID, va_list args);	/* 62 */
  void (JNICALL *CallVoidMethodA)(JNIEnv *env, jobject obj, jmethodID methodID, jvalue *args);	/* 63 */
  jobject (JNICALL *CallNonvirtualObjectMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);	/* 64 */
  jobject (JNICALL *CallNonvirtualObjectMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);	/* 65 */
  jobject (JNICALL *CallNonvirtualObjectMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);	/* 66 */
  jboolean (JNICALL *CallNonvirtualBooleanMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);	/* 67 */
  jboolean (JNICALL *CallNonvirtualBooleanMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);	/* 68 */
  jboolean (JNICALL *CallNonvirtualBooleanMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);	/* 69 */
  jbyte (JNICALL *CallNonvirtualByteMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);	/* 70 */
  jbyte (JNICALL *CallNonvirtualByteMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);	/* 71 */
  jbyte (JNICALL *CallNonvirtualByteMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);	/* 72 */
  jchar (JNICALL *CallNonvirtualCharMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);	/* 73 */
  jchar (JNICALL *CallNonvirtualCharMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);	/* 74 */
  jchar (JNICALL *CallNonvirtualCharMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);	/* 75 */
  jshort (JNICALL *CallNonvirtualShortMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);	/* 76 */
  jshort (JNICALL *CallNonvirtualShortMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);	/* 77 */
  jshort (JNICALL *CallNonvirtualShortMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);	/* 78 */
  jint (JNICALL *CallNonvirtualIntMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);	/* 79 */
  jint (JNICALL *CallNonvirtualIntMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);	/* 80 */
  jint (JNICALL *CallNonvirtualIntMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);	/* 81 */
  jlong (JNICALL *CallNonvirtualLongMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);	/* 82 */
  jlong (JNICALL *CallNonvirtualLongMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);	/* 83 */
  jlong (JNICALL *CallNonvirtualLongMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);	/* 84 */
  jfloat (JNICALL *CallNonvirtualFloatMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);	/* 85 */
  jfloat (JNICALL *CallNonvirtualFloatMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);	/* 86 */
  jfloat (JNICALL *CallNonvirtualFloatMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);	/* 87 */
  jdouble (JNICALL *CallNonvirtualDoubleMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);	/* 88 */
  jdouble (JNICALL *CallNonvirtualDoubleMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);	/* 89 */
  jdouble (JNICALL *CallNonvirtualDoubleMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);	/* 90 */
  void (JNICALL *CallNonvirtualVoidMethod)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);	/* 91 */
  void (JNICALL *CallNonvirtualVoidMethodV)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, va_list args);	/* 92 */
  void (JNICALL *CallNonvirtualVoidMethodA)(JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, jvalue *args);	/* 93 */
  jfieldID (JNICALL *GetFieldID)(JNIEnv *env, jclass clazz, const char *name, const char *sig);	/* 94 */
  jobject (JNICALL *GetObjectField)(JNIEnv *env, jobject obj, jfieldID fieldID);	/* 95 */
  jboolean (JNICALL *GetBooleanField)(JNIEnv *env, jobject obj, jfieldID fieldID);	/* 96 */
  jbyte (JNICALL *GetByteField)(JNIEnv *env, jobject obj, jfieldID fieldID);	/* 97 */
  jchar (JNICALL *GetCharField)(JNIEnv *env, jobject obj, jfieldID fieldID);	/* 98 */
  jshort (JNICALL *GetShortField)(JNIEnv *env, jobject obj, jfieldID fieldID);	/* 99 */
  jint (JNICALL *GetIntField)(JNIEnv *env, jobject obj, jfieldID fieldID);	/* 100 */
  jlong (JNICALL *GetLongField)(JNIEnv *env, jobject obj, jfieldID fieldID);	/* 101 */
  jfloat (JNICALL *GetFloatField)(JNIEnv *env, jobject obj, jfieldID fieldID);	/* 102 */
  jdouble (JNICALL *GetDoubleField)(JNIEnv *env, jobject obj, jfieldID fieldID);	/* 103 */
  void (JNICALL *SetObjectField)(JNIEnv *env, jobject obj, jfieldID fieldID, jobject value);	/* 104 */
  void (JNICALL *SetBooleanField)(JNIEnv *env, jobject obj, jfieldID fieldID, jboolean value);	/* 105 */
  void (JNICALL *SetByteField)(JNIEnv *env, jobject obj, jfieldID fieldID, jbyte value);	/* 106 */
  void (JNICALL *SetCharField)(JNIEnv *env, jobject obj, jfieldID fieldID, jchar value);	/* 107 */
  void (JNICALL *SetShortField)(JNIEnv *env, jobject obj, jfieldID fieldID, jshort value);	/* 108 */
  void (JNICALL *SetIntField)(JNIEnv *env, jobject obj, jfieldID fieldID, jint value);	/* 109 */
  void (JNICALL *SetLongField)(JNIEnv *env, jobject obj, jfieldID fieldID, jlong value);	/* 110 */
  void (JNICALL *SetFloatField)(JNIEnv *env, jobject obj, jfieldID fieldID, jfloat value);	/* 111 */
  void (JNICALL *SetDoubleField)(JNIEnv *env, jobject obj, jfieldID fieldID, jdouble value);	/* 112 */
  jmethodID (JNICALL *GetStaticMethodID)(JNIEnv *env, jclass clazz, const char *name, const char *sig);	/* 113 */
  jobject (JNICALL *CallStaticObjectMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);	/* 114 */
  jobject (JNICALL *CallStaticObjectMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);	/* 115 */
  jobject (JNICALL *CallStaticObjectMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);	/* 116 */
  jboolean (JNICALL *CallStaticBooleanMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);	/* 117 */
  jboolean (JNICALL *CallStaticBooleanMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);	/* 118 */
  jboolean (JNICALL *CallStaticBooleanMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);	/* 119 */
  jbyte (JNICALL *CallStaticByteMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);	/* 120 */
  jbyte (JNICALL *CallStaticByteMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);	/* 121 */
  jbyte (JNICALL *CallStaticByteMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);	/* 122 */
  jchar (JNICALL *CallStaticCharMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);	/* 123 */
  jchar (JNICALL *CallStaticCharMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);	/* 124 */
  jchar (JNICALL *CallStaticCharMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);	/* 125 */
  jshort (JNICALL *CallStaticShortMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);	/* 126 */
  jshort (JNICALL *CallStaticShortMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);	/* 127 */
  jshort (JNICALL *CallStaticShortMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);	/* 128 */
  jint (JNICALL *CallStaticIntMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);	/* 129 */
  jint (JNICALL *CallStaticIntMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);	/* 130 */
  jint (JNICALL *CallStaticIntMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);	/* 131 */
  jlong (JNICALL *CallStaticLongMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);	/* 132 */
  jlong (JNICALL *CallStaticLongMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);	/* 133 */
  jlong (JNICALL *CallStaticLongMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);	/* 134 */
  jfloat (JNICALL *CallStaticFloatMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);	/* 135 */
  jfloat (JNICALL *CallStaticFloatMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);	/* 136 */
  jfloat (JNICALL *CallStaticFloatMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);	/* 137 */
  jdouble (JNICALL *CallStaticDoubleMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);	/* 138 */
  jdouble (JNICALL *CallStaticDoubleMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);	/* 139 */
  jdouble (JNICALL *CallStaticDoubleMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);	/* 140 */
  void (JNICALL *CallStaticVoidMethod)(JNIEnv *env, jclass clazz, jmethodID methodID, ...);	/* 141 */
  void (JNICALL *CallStaticVoidMethodV)(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);	/* 142 */
  void (JNICALL *CallStaticVoidMethodA)(JNIEnv *env, jclass clazz, jmethodID methodID, jvalue *args);	/* 143 */
  jfieldID (JNICALL *GetStaticFieldID)(JNIEnv *env, jclass clazz, const char *name, const char *sig);	/* 144 */
  jobject (JNICALL *GetStaticObjectField)(JNIEnv *env, jclass clazz, jfieldID fieldID);	/* 145 */
  jboolean (JNICALL *GetStaticBooleanField)(JNIEnv *env, jclass clazz, jfieldID fieldID);	/* 146 */
  jbyte (JNICALL *GetStaticByteField)(JNIEnv *env, jclass clazz, jfieldID fieldID);	/* 147 */
  jchar (JNICALL *GetStaticCharField)(JNIEnv *env, jclass clazz, jfieldID fieldID);	/* 148 */
  jshort (JNICALL *GetStaticShortField)(JNIEnv *env, jclass clazz, jfieldID fieldID);	/* 149 */
  jint (JNICALL *GetStaticIntField)(JNIEnv *env, jclass clazz, jfieldID fieldID);	/* 150 */
  jlong (JNICALL *GetStaticLongField)(JNIEnv *env, jclass clazz, jfieldID fieldID);	/* 151 */
  jfloat (JNICALL *GetStaticFloatField)(JNIEnv *env, jclass clazz, jfieldID fieldID);	/* 152 */
  jdouble (JNICALL *GetStaticDoubleField)(JNIEnv *env, jclass clazz, jfieldID fieldID);	/* 153 */
  void (JNICALL *SetStaticObjectField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jobject value);	/* 154 */
  void (JNICALL *SetStaticBooleanField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jboolean value);	/* 155 */
  void (JNICALL *SetStaticByteField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jbyte value);	/* 156 */
  void (JNICALL *SetStaticCharField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jchar value);	/* 157 */
  void (JNICALL *SetStaticShortField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jshort value);	/* 158 */
  void (JNICALL *SetStaticIntField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jint value);	/* 159 */
  void (JNICALL *SetStaticLongField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jlong value);	/* 160 */
  void (JNICALL *SetStaticFloatField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jfloat value);	/* 161 */
  void (JNICALL *SetStaticDoubleField)(JNIEnv *env, jclass clazz, jfieldID fieldID, jdouble value);	/* 162 */
  jstring (JNICALL *NewString)(JNIEnv *env, const jchar *uChars, jsize len);	/* 163 */
  jsize (JNICALL *GetStringLength)(JNIEnv *env, jstring string);	/* 164 */
  const jchar *(JNICALL *GetStringChars)(JNIEnv *env, jstring string, jboolean *isCopy);	/* 165 */
  void (JNICALL *ReleaseStringChars)(JNIEnv *env, jstring string, const jchar *chars);	/* 166 */
  jstring (JNICALL *NewStringUTF)(JNIEnv *env, const char *bytes);	/* 167 */
  jsize (JNICALL *GetStringUTFLength)(JNIEnv *env, jstring string);	/* 168 */
  const char *(JNICALL *GetStringUTFChars)(JNIEnv *env, jstring string, jboolean *isCopy);	/* 169 */
  void (JNICALL *ReleaseStringUTFChars)(JNIEnv *env, jstring string, const char *utf);	/* 170 */
  jsize (JNICALL *GetArrayLength)(JNIEnv *env, jarray array);	/* 171 */
  jobjectArray (JNICALL *NewObjectArray)(JNIEnv *env, jsize length, jclass elementType, jobject initialElement);	/* 172 */
  jobject (JNICALL *GetObjectArrayElement)(JNIEnv *env, jobjectArray array, jsize indx);	/* 173 */
  void (JNICALL *SetObjectArrayElement)(JNIEnv *env, jobjectArray array, jsize indx, jobject value);	/* 174 */
  jbooleanArray (JNICALL *NewBooleanArray)(JNIEnv *env, jsize length);	/* 175 */
  jbyteArray (JNICALL *NewByteArray)(JNIEnv *env, jsize length);	/* 176 */
  jcharArray (JNICALL *NewCharArray)(JNIEnv *env, jsize length);	/* 177 */
  jshortArray (JNICALL *NewShortArray)(JNIEnv *env, jsize length);	/* 178 */
  jintArray (JNICALL *NewIntArray)(JNIEnv *env, jsize length);	/* 179 */
  jlongArray (JNICALL *NewLongArray)(JNIEnv *env, jsize length);	/* 180 */
  jfloatArray (JNICALL *NewFloatArray)(JNIEnv *env, jsize length);	/* 181 */
  jdoubleArray (JNICALL *NewDoubleArray)(JNIEnv *env, jsize length);	/* 182 */
  jboolean *(JNICALL *GetBooleanArrayElements)(JNIEnv *env, jbooleanArray array, jboolean *isCopy);	/* 183 */
  jbyte *(JNICALL *GetByteArrayElements)(JNIEnv *env, jbyteArray array, jboolean *isCopy);	/* 184 */
  jchar *(JNICALL *GetCharArrayElements)(JNIEnv *env, jcharArray array, jboolean *isCopy);	/* 185 */
  jshort *(JNICALL *GetShortArrayElements)(JNIEnv *env, jshortArray array, jboolean *isCopy);	/* 186 */
  jint *(JNICALL *GetIntArrayElements)(JNIEnv *env, jintArray array, jboolean *isCopy);	/* 187 */
  jlong *(JNICALL *GetLongArrayElements)(JNIEnv *env, jlongArray array, jboolean *isCopy);	/* 188 */
  jfloat *(JNICALL *GetFloatArrayElements)(JNIEnv *env, jfloatArray array, jboolean *isCopy);	/* 189 */
  jdouble *(JNICALL *GetDoubleArrayElements)(JNIEnv *env, jdoubleArray array, jboolean *isCopy);	/* 190 */
  void (JNICALL *ReleaseBooleanArrayElements)(JNIEnv *env, jbooleanArray array, jboolean *elems, jint mode);	/* 191 */
  void (JNICALL *ReleaseByteArrayElements)(JNIEnv *env, jbyteArray array, jbyte *elems, jint mode);	/* 192 */
  void (JNICALL *ReleaseCharArrayElements)(JNIEnv *env, jcharArray array, jchar *elems, jint mode);	/* 193 */
  void (JNICALL *ReleaseShortArrayElements)(JNIEnv *env, jshortArray array, jshort *elems, jint mode);	/* 194 */
  void (JNICALL *ReleaseIntArrayElements)(JNIEnv *env, jintArray array, jint *elems, jint mode);	/* 195 */
  void (JNICALL *ReleaseLongArrayElements)(JNIEnv *env, jlongArray array, jlong *elems, jint mode);	/* 196 */
  void (JNICALL *ReleaseFloatArrayElements)(JNIEnv *env, jfloatArray array, jfloat *elems, jint mode);	/* 197 */
  void (JNICALL *ReleaseDoubleArrayElements)(JNIEnv *env, jdoubleArray array, jdouble *elems, jint mode);	/* 198 */
  void (JNICALL *GetBooleanArrayRegion)(JNIEnv *env, jbooleanArray array, jsize start, jsize len, jboolean *buf);	/* 199 */
  void (JNICALL *GetByteArrayRegion)(JNIEnv *env, jbyteArray array, jsize start, jsize len, jbyte *buf);	/* 200 */
  void (JNICALL *GetCharArrayRegion)(JNIEnv *env, jcharArray array, jsize start, jsize len, jchar *buf);	/* 201 */
  void (JNICALL *GetShortArrayRegion)(JNIEnv *env, jshortArray array, jsize start, jsize len, jshort *buf);	/* 202 */
  void (JNICALL *GetIntArrayRegion)(JNIEnv *env, jintArray array, jsize start, jsize len, jint *buf);	/* 203 */
  void (JNICALL *GetLongArrayRegion)(JNIEnv *env, jlongArray array, jsize start, jsize len, jlong *buf);	/* 204 */
  void (JNICALL *GetFloatArrayRegion)(JNIEnv *env, jfloatArray array, jsize start, jsize len, jfloat *buf);	/* 205 */
  void (JNICALL *GetDoubleArrayRegion)(JNIEnv *env, jdoubleArray array, jsize start, jsize len, jdouble *buf);	/* 206 */
  void (JNICALL *SetBooleanArrayRegion)(JNIEnv *env, jbooleanArray array, jsize start, jsize len, const jboolean *buf);	/* 207 */
  void (JNICALL *SetByteArrayRegion)(JNIEnv *env, jbyteArray array, jsize start, jsize len, const jbyte *buf);	/* 208 */
  void (JNICALL *SetCharArrayRegion)(JNIEnv *env, jcharArray array, jsize start, jsize len, const jchar *buf);	/* 209 */
  void (JNICALL *SetShortArrayRegion)(JNIEnv *env, jshortArray array, jsize start, jsize len, const jshort *buf);	/* 210 */
  void (JNICALL *SetIntArrayRegion)(JNIEnv *env, jintArray array, jsize start, jsize len, const jint *buf);	/* 211 */
  void (JNICALL *SetLongArrayRegion)(JNIEnv *env, jlongArray array, jsize start, jsize len, const jlong *buf);	/* 212 */
  void (JNICALL *SetFloatArrayRegion)(JNIEnv *env, jfloatArray array, jsize start, jsize len, const jfloat *buf);	/* 213 */
  void (JNICALL *SetDoubleArrayRegion)(JNIEnv *env, jdoubleArray array, jsize start, jsize len, const jdouble *buf);	/* 214 */
  jint (JNICALL *RegisterNatives)(JNIEnv *env, jclass clazz, const JNINativeMethod *methods, jint nMethods);	/* 215 */
  jint (JNICALL *UnregisterNatives)(JNIEnv *env, jclass clazz);	/* 216 */
  jint (JNICALL *MonitorEnter)(JNIEnv *env, jobject obj);	/* 217 */
  jint (JNICALL *MonitorExit)(JNIEnv *env, jobject obj);	/* 218 */
  jint (JNICALL *GetJavaVM)(JNIEnv *env, JavaVM **vm);	/* 219 */
  void (JNICALL *GetStringRegion)(JNIEnv *env, jstring str, jsize start, jsize len, jchar *buf);	/* 220 */
  void (JNICALL *GetStringUTFRegion)(JNIEnv *env, jstring str, jsize start, jsize len, char *buf);	/* 221 */
  void *(JNICALL *GetPrimitiveArrayCritical)(JNIEnv *env, jarray array, jboolean *isCopy);	/* 222 */
  void (JNICALL *ReleasePrimitiveArrayCritical)(JNIEnv *env, jarray array, void *carray, jint mode);	/* 223 */
  const jchar *(JNICALL *GetStringCritical)(JNIEnv *env, jstring string, jboolean *isCopy);	/* 224 */
  void (JNICALL *ReleaseStringCritical)(JNIEnv *env, jstring string, const jchar *carray);	/* 225 */
  jweak (JNICALL *NewWeakGlobalRef)(JNIEnv *env, jobject obj);	/* 226 */
  void (JNICALL *DeleteWeakGlobalRef)(JNIEnv *env, jweak wref);	/* 227 */
  jboolean (JNICALL *ExceptionCheck)(JNIEnv *env);	/* 228 */
  jobject (JNICALL *NewDirectByteBuffer)(JNIEnv *env, void *address, jlong capacity);	/* 229 */
  void *(JNICALL *GetDirectBufferAddress)(JNIEnv *env, jobject buf);	/* 230 */
  jlong (JNICALL *GetDirectBufferCapacity)(JNIEnv *env, jobject buf);	/* 231 */
};

struct JNIInvokeInterface {
  void *null_0;
  void *null_1;
  void *null_2;
  jint (JNICALL *DestroyJavaVM)(JavaVM *vm);	/* 3 */
  jint (JNICALL *AttachCurrentThread)(JavaVM *vm, void **penv, void *args);	/* 4 */
  jint (JNICALL *DetachCurrentThread)(JavaVM *vm);	/* 5 */
  jint (JNICALL *GetEnv)(JavaVM *vm, void **penv, jint interface_id);	/* 6 */
  jint (JNICALL *AttachCurrentThreadAsDaemon)(JavaVM *vm, void **penv, void *args);	/* 7 */
};

#ifdef __cplusplus
extern "C" {
#endif

/************************************************************************
 *				Functions				*
 ************************************************************************/

/* JNI invocation API */
JNIEXPORT jint JNICALL JNI_GetDefaultJavaVMInitArgs(void *vm_args);
JNIEXPORT jint JNICALL JNI_GetCreatedJavaVMs(JavaVM **vmBuf,
	jsize bufLen, jsize *nVMs);
JNIEXPORT jint JNICALL JNI_CreateJavaVM(JavaVM **pvm,
	void **penv, void *vm_args);

/* Library and version management */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved);
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved);

#ifdef __cplusplus
}
#endif

#endif	/* _JNI_H_ */
