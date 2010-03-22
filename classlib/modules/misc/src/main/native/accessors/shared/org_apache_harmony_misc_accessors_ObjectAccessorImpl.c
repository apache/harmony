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

#include <stdlib.h>
#include <assert.h>
#if defined(NEEDS_SYS_TYPES)
#include <sys/types.h>
#endif
#include "MemMacros.h"
#include "org_apache_harmony_misc_accessors_ObjectAccessor.h"

/*
 * Class:     org_apache_harmony_misc_accessors_ObjectAccessor
 * Method:    get<Type>
 * Signature: (Ljava/lang/Object;J)Z
 * Method:    getStatic<Type>
 * Signature: (Ljava/lang/Class;J)Z
 * Method:    set<Type>
 * Signature: (Ljava/lang/Object;JZ)V
 * Method:    setStatic<Type>
 * Signature: (Ljava/lang/Class;JZ)V
 */
#define fieldAccessFunctions(t,T,F) \
JNIEXPORT t JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_get##T \
(JNIEnv *env, jobject accessorObj, jobject obj, jlong fieldID) { \
    return (*env)->Get##F(env, obj, (jfieldID)(intptr_t)fieldID); \
} \
JNIEXPORT t JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_getStatic##T \
  (JNIEnv *env, jobject accessorObj, jclass clss, jlong fieldID) { \
  return (*env)->GetStatic##F(env, clss, (jfieldID)(intptr_t)fieldID); \
} \
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_set##T \
(JNIEnv *env, jobject accessorObj, jobject obj, jlong fieldID, t value) { \
    (*env)->Set##F(env, obj, (jfieldID)(intptr_t)fieldID, value); \
} \
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_setStatic##T \
(JNIEnv *env, jobject accessorObj, jclass clss, jlong fieldID, t value) { \
    (*env)->SetStatic##F(env, clss, (jfieldID)(intptr_t)fieldID, value); \
}


fieldAccessFunctions(jboolean, Boolean, BooleanField)
fieldAccessFunctions(jbyte, Byte, ByteField)
fieldAccessFunctions(jchar, Char, CharField)
fieldAccessFunctions(jint, Int, IntField)
fieldAccessFunctions(jlong, Long, LongField)
fieldAccessFunctions(jshort, Short, ShortField)
fieldAccessFunctions(jfloat, Float, FloatField)
fieldAccessFunctions(jdouble, Double, DoubleField)
fieldAccessFunctions(jobject, Object, ObjectField)


// Cached references to primitive type wrapping classes
jclass findIntClass(JNIEnv* env) {
    static jclass intClass;
    return intClass == NULL ? (jclass)(*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Integer")) : intClass;
}
jclass findBooleanClass(JNIEnv* env) {
    static jclass booleanClass;
    return booleanClass == NULL ? (jclass)(*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Boolean")) : booleanClass;
}
jclass findByteClass(JNIEnv* env) {
    static jclass byteClass;
    return byteClass == NULL ? (jclass)(*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Byte")) : byteClass;
}
jclass findShortClass(JNIEnv* env) {
    static jclass shortClass;
    return shortClass == NULL ? (jclass)(*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Short")) : shortClass;
}
jclass findCharClass(JNIEnv* env) {
    static jclass charClass;
    return charClass == NULL ? (jclass)(*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Character")) : charClass;
}
jclass findLongClass(JNIEnv* env) {
    static jclass longClass;
    return longClass == NULL ? (jclass)(*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Long")) : longClass;
}
jclass findFloatClass(JNIEnv* env) {
    static jclass floatClass;
    return floatClass == NULL ? (jclass)(*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Float")) : floatClass;
}
jclass findDoubleClass(JNIEnv* env) {
    static jclass doubleClass;
    return doubleClass == NULL ? (jclass)(*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Double")) : doubleClass;
}



/**
 * Method for debugging purposes - can be used to print java object name.
 */
void printObjectName(JNIEnv* env, jobject obj) {
    jmethodID id = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, obj), "toString", "()Ljava/lang/String;");
    jstring jstr;
    jboolean isCopy;
    const char* str;

    assert(id != NULL);
    jstr = (jstring)(*env)->CallObjectMethodA(env, obj, id, NULL);
    assert(jstr != NULL);
    str = (*env)->GetStringUTFChars(env, jstr, &isCopy);

    printf("Object: %s\n", str);
    (*env)->ReleaseStringUTFChars(env, jstr, str);
}


/**
 * Convenience method - converts an object array to array of jvalues suitable for CallXXX functions.
 */
jvalue* jarrayToValues(JNIEnv* env, jobjectArray array) {
    int len = (*env)->GetArrayLength(env, array);
    jvalue* pargs = (jvalue*)malloc(len * sizeof(jvalue));
    jobject obj;
    jclass clss;
    jmethodID methodID;
    int i;
    for (i = 0; i < len; i++) {
        obj = (*env)->GetObjectArrayElement(env, array, i);
        clss = (*env)->GetObjectClass(env, obj);
        // Do unboxing for primitive type wrappers
        if ((*env)->IsSameObject(env, clss, findIntClass(env))) {
            methodID = (*env)->GetMethodID(env, clss, "intValue", "()I");
            pargs[i].i = (*env)->CallIntMethodA(env, obj, methodID, NULL);
        } else if((*env)->IsSameObject(env, clss, findLongClass(env))) {
            methodID = (*env)->GetMethodID(env, clss, "longValue", "()J");
            pargs[i].j = (*env)->CallLongMethodA(env, obj, methodID, NULL);
        } else if((*env)->IsSameObject(env, clss, findShortClass(env))) {
            methodID = (*env)->GetMethodID(env, clss, "shortValue", "()S");
            pargs[i].s = (*env)->CallShortMethodA(env, obj, methodID, NULL);
        } else if((*env)->IsSameObject(env, clss, findByteClass(env))) {
            methodID = (*env)->GetMethodID(env, clss, "byteValue", "()B");
            pargs[i].b = (*env)->CallByteMethodA(env, obj, methodID, NULL);
        } else if((*env)->IsSameObject(env, clss, findCharClass(env))) {
            methodID = (*env)->GetMethodID(env, clss, "charValue", "()C");
            pargs[i].c = (*env)->CallCharMethodA(env, obj, methodID, NULL);
        } else if((*env)->IsSameObject(env, clss, findFloatClass(env))) {
            methodID = (*env)->GetMethodID(env, clss, "floatValue", "()F");
            pargs[i].f = (*env)->CallFloatMethodA(env, obj, methodID, NULL);
        } else if((*env)->IsSameObject(env, clss, findDoubleClass(env))) {
            methodID = (*env)->GetMethodID(env, clss, "doubleValue", "()D");
            pargs[i].d = (*env)->CallDoubleMethodA(env, obj, methodID, NULL);
        } else if((*env)->IsSameObject(env, clss, findBooleanClass(env))) {
            methodID = (*env)->GetMethodID(env, clss, "booleanValue", "()Z");
            pargs[i].z = (*env)->CallBooleanMethodA(env, obj, methodID, NULL);
        } else {
            pargs[i].l = obj;
        }
    }
    return pargs;
}


// invokeMethodFunctions(void, Void, VoidMethodA)
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_invokeNonVirtualVoid
(JNIEnv *env, jobject accessorObj, jclass clss, jobject obj, jlong methodID, jobjectArray args){
    jvalue* pargs = jarrayToValues(env, args);
    (*env)->CallNonvirtualVoidMethodA(env, obj, clss, (jmethodID)(intptr_t)methodID, pargs);
    free(pargs);
}
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_invokeStaticVoid
(JNIEnv *env, jobject accessorObj, jclass clss, jlong methodID, jobjectArray args) {
    jvalue* pargs = jarrayToValues(env, args);
    (*env)->CallStaticVoidMethodA(env, clss, (jmethodID)(intptr_t)methodID, pargs);
    free(pargs);
}
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_invokeVirtualVoid
(JNIEnv *env, jobject accessorObj, jobject obj, jlong methodID, jobjectArray args) {
    jvalue* pargs = jarrayToValues(env, args);
    (*env)->CallVoidMethodA(env, obj, (jmethodID)(intptr_t)methodID, pargs);
    free(pargs);
}



/*
 * Class:     org_apache_harmony_misc_accessors_ObjectAccessor
 * Method:    invokeStaticVoid
 * Signature: (Ljava/lang/Class;J[Ljava/lang/Object;)V
 * Method:    invokeVirtualVoid
 * Signature: (Ljava/lang/Object;J[Ljava/lang/Object;)V
 * Method:    invokeNonVirtualVoid
 * Signature: (Ljava/lang/Class;Ljava/lang/Object;J[Ljava/lang/Object;)V
 */
#define invokeMethodFunctions(t, T, M) \
 JNIEXPORT t JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_invokeNonVirtual##T \
(JNIEnv *env, jobject accessorObj, jclass clss, jobject obj, jlong methodID, jobjectArray args){ \
    jvalue* pargs = jarrayToValues(env, args); \
    t res = (*env)->CallNonvirtual##M(env, obj, clss, (jmethodID)(intptr_t)methodID, pargs); \
    free(pargs); \
    return res; \
} \
JNIEXPORT t JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_invokeStatic##T \
(JNIEnv *env, jobject accessorObj, jclass clss, jlong methodID, jobjectArray args) { \
    jvalue* pargs = jarrayToValues(env, args); \
    t res = (*env)->CallStatic##M(env, clss, (jmethodID)(intptr_t)methodID, pargs); \
    free(pargs); \
    return res; \
} \
JNIEXPORT t JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_invokeVirtual##T \
(JNIEnv *env, jobject accessorObj, jobject obj, jlong methodID, jobjectArray args) { \
    jvalue* pargs = jarrayToValues(env, args); \
    t res = (*env)->Call##M(env, obj, (jmethodID)(intptr_t)methodID, pargs); \
    free(pargs); \
    return res; \
}



invokeMethodFunctions(jobject, Object, ObjectMethodA)
invokeMethodFunctions(jboolean, Boolean, BooleanMethodA)
invokeMethodFunctions(jbyte, Byte, ByteMethodA)
invokeMethodFunctions(jchar, Char, CharMethodA)
invokeMethodFunctions(jshort, Short, ShortMethodA)
invokeMethodFunctions(jint, Int, IntMethodA)
invokeMethodFunctions(jlong, Long, LongMethodA)
invokeMethodFunctions(jfloat, Float, FloatMethodA)
invokeMethodFunctions(jdouble, Double, DoubleMethodA)





/*
 * Class:     org_apache_harmony_misc_accessors_ObjectAccessor
 * Method:    getFieldID
 * Signature: (Ljava/lang/reflect/Field;)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_getFieldID__Ljava_lang_reflect_Field_2
(JNIEnv *env, jobject accessorObj, jobject field) {
        return (jlong)(intptr_t)(*env)->FromReflectedField(env, field);
}

/*
 * Class:     org_apache_harmony_misc_accessors_ObjectAccessor
 * Method:    getMethodID0
 * Signature: (Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_getMethodID0__Ljava_lang_Class_2Ljava_lang_String_2Ljava_lang_String_2
(JNIEnv *env, jclass cl, jclass clss, jstring methodName, jstring methodSignature) {
    jboolean isCopy;
    char* method = (char *)(*env)->GetStringUTFChars(env, methodName, &isCopy);
    char* sig = (char *)(*env)->GetStringUTFChars(env, methodSignature, &isCopy);
    jlong res = (jlong)(intptr_t)(*env)->GetMethodID(env, clss, method, sig);
    (*env)->ReleaseStringUTFChars(env, methodName, method);
    (*env)->ReleaseStringUTFChars(env, methodSignature, sig);
    return res;
}

/*
 * Class:     org_apache_harmony_misc_accessors_ObjectAccessor
 * Method:    getStaticMethodID0
 * Signature: (Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_getStaticMethodID0
(JNIEnv *env, jclass cl, jclass clss, jstring methodName, jstring methodSignature) {
    jboolean isCopy;
    char* method = (char *)(*env)->GetStringUTFChars(env, methodName, &isCopy);
    char* sig = (char *)(*env)->GetStringUTFChars(env, methodSignature, &isCopy);
    jlong res = (jlong)(intptr_t)(*env)->GetStaticMethodID(env, clss, method, sig);
    (*env)->ReleaseStringUTFChars(env, methodName, method);
    (*env)->ReleaseStringUTFChars(env, methodSignature, sig);
    return res;
}


/*
 * Class:     org_apache_harmony_misc_accessors_ObjectAccessor
 * Method:    getMethodID0
 * Signature: (Ljava/lang/reflect/Member;)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_getMethodID0__Ljava_lang_reflect_Member_2
(JNIEnv *env, jclass cl, jobject method) {
    return (jlong)(intptr_t)(*env)->FromReflectedField(env, method);
}



/*
 * Class:     org_apache_harmony_misc_accessors_ObjectAccessor
 * Method:    allocateObject
 * Signature: (Ljava/lang/Class;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_allocateObject
(JNIEnv *env, jobject accessorObj, jclass clazz) {
     return (*env)->AllocObject(env, clazz);
}

/*
 * Class:     org_apache_harmony_misc_accessors_ObjectAccessor
 * Method:    newInstance
 * Signature: (Ljava/lang/Class;J[Ljava/lang/Object;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_newInstance__Ljava_lang_Class_2J_3Ljava_lang_Object_2
(JNIEnv *env, jobject accessorObj, jclass clss, jlong ctorID, jobjectArray args) {
    jvalue *pargs = NULL;
    jobject res;

    if (args != NULL) {
        pargs = jarrayToValues(env, args);
    }
    res = (*env)->NewObjectA(env, clss, (jmethodID)(intptr_t)ctorID, pargs);
    free(pargs);
    return res;
}




/*
 * Class:     org_apache_harmony_misc_accessors_ObjectAccessor
 * Method:    newInstance
 * Signature: (Ljava/lang/Class;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_newInstance__Ljava_lang_Class_2
(JNIEnv *env, jobject accessorObj, jclass c) {
    jmethodID ctorID = (*env)->GetMethodID(env, c, "<init>", "()V");
    return (*env)->NewObject(env, c, ctorID);
}

/*
 * Class:     org_apache_harmony_misc_accessors_ObjectAccessor
 * Method:    hasStaticInitializer
 * Signature: (Ljava/lang/Class;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_hasStaticInitializer
(JNIEnv *env, jobject accessorObj, jclass c) {
    jboolean res = (*env)->GetStaticMethodID(env, c, "<clinit>", "()V") != 0 ? JNI_TRUE : JNI_FALSE;
    (*env)->ExceptionClear(env);
    return res;
}

/*
 * Class:     org_apache_harmony_misc_accessors_ObjectAccessor
 * Method:    monitorEnter
 * Signature: (Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_monitorEnter
(JNIEnv *env, jobject accessorObj, jobject obj) {
    (*env)->MonitorEnter(env, obj);
}

/*
 * Class:     org_apache_harmony_misc_accessors_ObjectAccessor
 * Method:    monitorExit
 * Signature: (Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_monitorExit
(JNIEnv *env, jobject accessorObj, jobject obj) {
    (*env)->MonitorExit(env, obj);
}

/*
 * Class:     org_apache_harmony_misc_accessors_ObjectAccessor
 * Method:    getFieldID
 * Signature: (Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_getFieldID__Ljava_lang_Class_2Ljava_lang_String_2Ljava_lang_String_2
(JNIEnv *env, jobject accessorObj, jclass clss, jstring fieldName, jstring fieldSig) {
    jboolean isCopy;
    char* name = (char *)(*env)->GetStringUTFChars(env, fieldName, &isCopy);
    char* sig = (char *)(*env)->GetStringUTFChars(env, fieldSig, &isCopy);
    jlong res = (jlong)(intptr_t)(*env)->GetFieldID(env, clss, name, sig);
    (*env)->ReleaseStringUTFChars(env, fieldName, name);
    (*env)->ReleaseStringUTFChars(env, fieldSig, sig);
    return res;
}

/*
 * Class:     org_apache_harmony_misc_accessors_ObjectAccessor
 * Method:    getStaticFieldID
 * Signature: (Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_getStaticFieldID
(JNIEnv *env, jobject accessorObj, jclass clss, jstring fieldName, jstring fieldSig) {
    jboolean isCopy;
    char* name = (char *)(*env)->GetStringUTFChars(env, fieldName, &isCopy);
    char* sig = (char *)(*env)->GetStringUTFChars(env, fieldSig, &isCopy);
    jlong res = (jlong)(intptr_t)(*env)->GetStaticFieldID(env, clss, name, sig);
    (*env)->ReleaseStringUTFChars(env, fieldName, name);
    (*env)->ReleaseStringUTFChars(env, fieldSig, sig);
    return res;
}

/*
 * Class: org_apache_harmony_misc_accessors_ObjectAccessor
 * Method: getGlobalReference
 * Signature: (Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_getGlobalReference
(JNIEnv * env, jobject accessorObj, jobject obj) {
    return (jlong)(intptr_t)(*env)->NewGlobalRef(env, obj);
}

/*
 * Class: org_apache_harmony_misc_accessors_ObjectAccessor
 * Method: releaseGlobalReference
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_releaseGlobalReference
(JNIEnv * env, jobject accessorObj, jlong ref) {
    (*env)->DeleteGlobalRef(env, (jobject)(intptr_t)ref);
}

/*
 * Class: org_apache_harmony_misc_accessors_ObjectAccessor
 * Method: getObjectFromReference
 * Signature: (J)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_apache_harmony_misc_accessors_ObjectAccessor_getObjectFromReference
(JNIEnv * env, jobject accessorObj, jlong ref) {
    return (jobject)(intptr_t)ref;
}





