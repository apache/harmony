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
 * @file java_lang_reflect_VMReflection.cpp
 *
 * This file is a part of kernel class natives VM core component.
 * It contains implementation for native methods of 
 * java.lang.reflect.VMReflection class.
 */

#define LOG_DOMAIN "vm.core.reflection"
#include "cxxlog.h"

#include "reflection.h"
#include "open/vm_field_access.h"
#include "open/vm_method_access.h"
#include "open/vm_class_manipulation.h"
#include "environment.h"
#include "exceptions.h"
#include "vm_strings.h"
#include "primitives_support.h"
#include "jni_utils.h"
#include "Class.h"
#include "classloader.h"

#include "java_lang_VMClassRegistry.h"
#include "java_lang_reflect_VMReflection.h"

JNIEXPORT jobjectArray JNICALL Java_java_lang_reflect_VMReflection_getExceptionTypes
  (JNIEnv *jenv, jclass, jlong member)
{
    Method_Handle method = (Method_Handle) ((POINTER_SIZE_INT) member);
    jclass jlc_class = struct_Class_to_java_lang_Class_Handle(
        VM_Global_State::loader_env->JavaLangClass_Class);

    // Create and fill exceptions array
    int n_exceptions = method->num_exceptions_method_can_throw();
    jobjectArray exceptionTypes = NewObjectArray(jenv, n_exceptions, jlc_class, NULL);

    if (!exceptionTypes) {
        assert(exn_raised());
        return NULL;
    }

    for (int i = 0; i < n_exceptions; i++) {
        Class_Handle exclass = method->get_class()->get_class_loader()->
            LoadVerifyAndPrepareClass(VM_Global_State::loader_env, method->get_exception_name(i));

        if (!exclass) {
            assert(exn_raised());
            return NULL;
        }
        SetObjectArrayElement(jenv, exceptionTypes, i, 
            struct_Class_to_java_lang_Class_Handle(exclass));
    }

    return exceptionTypes;
}

JNIEXPORT jobjectArray JNICALL Java_java_lang_reflect_VMReflection_getParameterTypes
  (JNIEnv *jenv, jclass, jlong member)
{
    Method_Handle mh = (Method_Handle) ((POINTER_SIZE_INT) member);
    return reflection_get_parameter_types(jenv, mh);
}

JNIEXPORT jclass JNICALL Java_java_lang_reflect_VMReflection_getMethodReturnType
  (JNIEnv *jenv, jclass, jlong member)
{
    Method_Handle method = (Method_Handle) ((POINTER_SIZE_INT) member);
    Method_Signature_Handle msh = method_get_signature(method);
    Type_Info_Handle ret_type = method_ret_type_get_type_info(msh);
    return descriptor_to_jclass(ret_type);
}

JNIEXPORT jclass JNICALL Java_java_lang_reflect_VMReflection_getFieldType
  (JNIEnv *jenv, jclass, jlong member)
{
    Field_Handle fh = (Field_Handle) ((POINTER_SIZE_INT) member);
    Type_Info_Handle fti = field_get_type_info(fh);
    return descriptor_to_jclass(fti);
}

/* 
 * Returns false if no exception is set. If exception is set and is not 
 * an OOME instance, wraps it into new InvocationTargetException. 
 */
static bool rethrow_invocation_exception(JNIEnv* jenv) 
{
    Class* exn_class = exn_get_class();
    if (!exn_class)
        return false;

    Global_Env* genv = VM_Global_State::loader_env;
    if (exn_class == genv->java_lang_OutOfMemoryError_Class) {
        return true;
    }

    jthrowable exn = exn_get(); 
    //FIXME need better handling for lazy exceptions
    if (!exn) {
        LWARN(40, "ATTENTION! Could not get cause exception from lazy machinery");
    }
    exn_clear();
    //static Class* ITE_class = genv->LoadCoreClass(
    //     genv->string_pool.lookup("java/lang/reflect/InvocationTargetException"));
    exn_raise_by_name("java/lang/reflect/InvocationTargetException", exn);
    return true;
}

// Invoke method with primitive return type. Return result in wrapper. For void return NULL.
static jobject invoke_primitive_method(JNIEnv* jenv, jobject obj, jclass declaring_class, Method* method, jvalue* jvalue_args)
{
    jmethodID method_id = (jmethodID) method;
    bool is_static = method->is_static();
    Java_Type return_type = method->get_return_java_type();
    jvalue result;

    switch(return_type) {
    case JAVA_TYPE_BOOLEAN:
        if (is_static)
            result.z = CallStaticBooleanMethodA(jenv, declaring_class, method_id, jvalue_args);
        else
            result.z = CallBooleanMethodA(jenv, obj, method_id, jvalue_args);
        break;
    case JAVA_TYPE_BYTE:
        if (is_static)
            result.b = CallStaticByteMethodA(jenv, declaring_class, method_id, jvalue_args);
        else
            result.b = CallByteMethodA(jenv, obj, method_id, jvalue_args);
        break;
    case JAVA_TYPE_CHAR:
        if (is_static)
            result.c = CallStaticCharMethodA(jenv, declaring_class, method_id, jvalue_args);
        else
            result.c = CallCharMethodA(jenv, obj, method_id, jvalue_args);
        break;
    case JAVA_TYPE_SHORT:
        if (is_static)
            result.s = CallStaticShortMethodA(jenv, declaring_class, method_id, jvalue_args);
        else
            result.s = CallShortMethodA(jenv, obj, method_id, jvalue_args);
        break;
    case JAVA_TYPE_INT:
        if (is_static)
            result.i = CallStaticIntMethodA(jenv, declaring_class, method_id, jvalue_args);
        else
            result.i = CallIntMethodA(jenv, obj, method_id, jvalue_args);
        break;
    case JAVA_TYPE_LONG:
        if (is_static)
            result.j = CallStaticLongMethodA(jenv, declaring_class, method_id, jvalue_args);
        else
            result.j = CallLongMethodA(jenv, obj, method_id, jvalue_args);
        break;
    case JAVA_TYPE_FLOAT:
        if (is_static)
            result.f = CallStaticFloatMethodA(jenv, declaring_class, method_id, jvalue_args);
        else
            result.f = CallFloatMethodA(jenv, obj, method_id, jvalue_args);
        break;
    case JAVA_TYPE_DOUBLE:
        if (is_static)
            result.d = CallStaticDoubleMethodA(jenv, declaring_class, method_id, jvalue_args);
        else
            result.d = CallDoubleMethodA(jenv, obj, method_id, jvalue_args);
        break;
    case JAVA_TYPE_VOID:
        if (is_static)
            CallStaticVoidMethodA(jenv, declaring_class, method_id, jvalue_args);
        else
            CallVoidMethodA(jenv, obj, method_id, jvalue_args);

        return NULL;
        break;
    default:
        LDIE(53, "Unexpected java type");
    }

    return exn_raised() ? NULL : wrap_primitive(jenv, result, (char)return_type);
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_VMReflection_invokeMethod
  (JNIEnv *jenv, jclass, jlong member, jobject obj, jobjectArray args)
{
    Class* type = VM_Global_State::loader_env->java_lang_reflect_Method_Class;
    Method_Handle method = (Method_Handle) ((POINTER_SIZE_INT) member);

    TRACE("invoke : " << method->get_class()->get_name()->bytes << "."  << method->get_name()->bytes << "()");

    unsigned num_args = method->get_num_args();
    jvalue *jvalue_args = (jvalue *)STD_ALLOCA(num_args * sizeof(jvalue));
    if (!jobjectarray_to_jvaluearray(jenv, &jvalue_args, method, args)) {
        return NULL;   
    }
    
    jobject jresult;

    // In a case of class initialization error, it must be thrown directly
    if (!ensure_initialised(jenv, method->get_class())) {
        assert(exn_raised());
        return NULL;
    }
    jclass declaring_class = struct_Class_to_java_lang_Class_Handle(method->get_class());
    Java_Type return_type = method->get_return_java_type();
    if (return_type != JAVA_TYPE_CLASS && return_type != JAVA_TYPE_ARRAY) 
    {
        jresult = invoke_primitive_method(jenv, obj, declaring_class, method, jvalue_args);
    } 
    else if (method->is_static()) 
    {
        jresult = CallStaticObjectMethodA(jenv, declaring_class, (jmethodID)method, jvalue_args);
    }
    else
    {
        jresult = CallObjectMethodA(jenv, obj, (jmethodID)method, jvalue_args);
    }

    // check and chain exception, if occurred
    if (rethrow_invocation_exception(jenv)) {
        return NULL;
    }
    
    return jresult;
} // Java_java_lang_reflect_VMReflection_invokeMethod

// create multidimensional array with depth dimensions specified by dims
jobject createArray(JNIEnv *jenv, Class* arrType, jint *dims, int depth)
{
    jint size = dims[0];

    jobjectArray jarray = NewObjectArray(jenv, size, 
        struct_Class_to_java_lang_Class_Handle(arrType), NULL);

    if (exn_raised()) {
        return NULL;
    }

    if (depth > 1)
    {
        Class* compType = arrType->get_array_element_class();
        for (int i = 0; i < size; i++)
        {
            jobject elem = createArray(jenv, compType, dims + 1, depth - 1);
            SetObjectArrayElement(jenv, jarray, i, elem);
        }
    }

    return jarray;
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_VMReflection_newArrayInstance
  (JNIEnv *jenv, jclass, jclass compType, jintArray jdims)
{
    jint depth = GetArrayLength(jenv, jdims);
    TRACE("new array: depth=" << depth);

    if (depth <= 0 || depth > 255) {
        const char *message = (depth <= 0) ? "negative or zero dimensional array specified." : 
                "requested dimensions number exceeds 255 supported limit." ;
        ThrowNew_Quick(jenv, "java/lang/IllegalArgumentException", message);
        return NULL;
    }

    jint* dims = GetIntArrayElements(jenv, jdims, NULL);

    for (int i = 0; i < depth; i++) {
        if (dims[i] < 0) {
            ReleaseIntArrayElements(jenv, jdims, dims, JNI_ABORT);
            ThrowNew_Quick(jenv, "java/lang/NegativeArraySizeException", 
                    "one of the specified dimensions is negative.");
            return NULL;
        }
    }

    Class* arrClss = jclass_to_struct_Class(compType);
    for (int i = depth - 1; i > 0; --i) {
        arrClss = class_get_array_of_class(arrClss);
        if (!arrClss) {
            assert(exn_raised());
            break;
        }
    }

    jobject jarray = arrClss ? createArray(jenv, arrClss, dims, depth) : NULL;

    ReleaseIntArrayElements(jenv, jdims, dims, JNI_ABORT);

    return jarray;
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_VMReflection_newClassInstance
  (JNIEnv *jenv, jclass, jlong member, jobjectArray args)
{
    Method_Handle method = (Method_Handle) ((POINTER_SIZE_INT) member);

    TRACE("new class instance : " << method->get_class()->get_name()->bytes);

    unsigned num_args = method->get_num_args();
    jvalue *jvalue_args = (jvalue *)STD_ALLOCA(num_args * sizeof(jvalue));
    if (!jobjectarray_to_jvaluearray(jenv, &jvalue_args, method, args)) {
        return NULL;   
    }

    jclass declaring_class = struct_Class_to_java_lang_Class_Handle(method->get_class());

    // In a case of class initialization error, it must be thrown directly
    if (!ensure_initialised(jenv, method->get_class()))
        return NULL;

    // Create object
    jobject new_object = NewObjectA(jenv, declaring_class, (jmethodID) method, jvalue_args);

    // check and chain exception, if occurred
    if (rethrow_invocation_exception(jenv)) {
        return NULL;
    }

    return new_object;

} // Java_java_lang_reflect_VMReflection_newClassInstance
