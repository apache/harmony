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
 * @author Alexey V. Varlamov
 */  

/**
 * @file java_lang_VMClassRegistry.cpp
 *
 * This file is a part of kernel class natives VM core component.
 * It contains implementation for native methods of 
 * java.lang.VMClassRegistry class.
 */
#define LOG_DOMAIN "vm.core.generics"
#include "cxxlog.h"

#include "environment.h"
#include "Class.h"
#include "annotations.h"
#include "exceptions.h"
#include "jni_utils.h"
#include "vm_log.h"

#include "org_apache_harmony_vm_VMGenericsAndAnnotations.h"

JNIEXPORT jstring JNICALL Java_org_apache_harmony_vm_VMGenericsAndAnnotations_getSignature__J
(JNIEnv *jenv, jclass, jlong jmember) 
{
    Class_Member* member = (Class_Member*) ((POINTER_SIZE_INT) jmember);
    String* sig = member->get_signature();
    TRACE("Signature of " << member << " : " << sig);
    return sig ? NewStringUTF(jenv, sig->bytes) : NULL;
}

JNIEXPORT jstring JNICALL Java_org_apache_harmony_vm_VMGenericsAndAnnotations_getSignature__Ljava_lang_Class_2
(JNIEnv *jenv, jclass, jclass jclazz) 
{
    Class* clazz = jclass_to_struct_Class(jclazz);
    String* sig = clazz->get_signature();
    TRACE("Signature of " << clazz << " : " << sig);
    return sig ? NewStringUTF(jenv, sig->bytes) : NULL;
}

JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_vm_VMGenericsAndAnnotations_getDeclaredAnnotations__J
(JNIEnv *jenv, jclass, jlong jmember) 
{
    Class_Member* member = (Class_Member*) ((POINTER_SIZE_INT) jmember);
    TRACE("Requested annotations for member " << member);
    return get_annotations(jenv, member->get_declared_annotations(),
                member->get_declared_invisible_annotations(),
                member->get_class());
}

JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_vm_VMGenericsAndAnnotations_getDeclaredAnnotations__Ljava_lang_Class_2
(JNIEnv *jenv, jclass, jclass jclazz) 
{
    Class* clazz = jclass_to_struct_Class(jclazz);
    TRACE("Requested annotations for class " << clazz);
    return get_annotations(jenv, clazz->get_annotations(),
                clazz->get_invisible_annotations(), clazz);
}

JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_vm_VMGenericsAndAnnotations_getParameterAnnotations
(JNIEnv *jenv, jclass, jlong jmethod) 
{
    static Global_Env* genv = jni_get_vm_env(jenv);
    Method* method = (Method*) ((POINTER_SIZE_INT) jmethod);
    Class* declaring_class = method->get_class();

    static Class* array_class;
    if(array_class == NULL) {
        array_class = genv->LoadCoreClass(
            "[Ljava/lang/annotation/Annotation;");
    }

    unsigned param_num = method->get_num_param_annotations();
    unsigned num = param_num + method->get_num_invisible_param_annotations();
    TRACE("Requested parameters annotations for method " << method
        << "; num=" << num);

    jobjectArray array = NULL; 
    if (num == 0) {
        unsigned nparams = (method->get_num_args() - (method->is_static() ? 0 : 1));
        if (nparams > 0) {
            static Class* antn_class;
            if(antn_class == NULL) {
                antn_class = jni_get_vm_env(jenv)->LoadCoreClass(
                    "java/lang/annotation/Annotation");
            }

            array = NewObjectArray(jenv, nparams, 
                struct_Class_to_java_lang_Class_Handle(array_class), NewObjectArray(jenv, 0, 
                    struct_Class_to_java_lang_Class_Handle(antn_class), NULL));
            if (!array) {
                assert(exn_raised());
                return NULL;
            }
            return array;
        }
    }


    array = NewObjectArray(jenv, num, 
        struct_Class_to_java_lang_Class_Handle(array_class), NULL);

    if (!array) {
        assert(exn_raised());
        return NULL;
    }

    // According to J2SE specification the 0-length array must be added for
    // the parameter w/o annotation (still this no-annotation-parameter is not
    // skipped from the resulted array).
    unsigned i;
    for (i = 0; i < param_num; ++i) {
        jobject element = get_annotations(jenv,
                                method->get_param_annotations(i),
                                NULL, declaring_class);
        if (!element) {
            assert(exn_raised());
            return NULL;
        } else {
            SetObjectArrayElement(jenv, array, i, element);
            assert(!exn_raised());
        }
    }
    for (i = param_num; i < num; ++i) {
        jobject element = get_annotations(jenv, NULL,
                method->get_invisible_param_annotations(i - param_num),
                declaring_class);
        if (!element) {
            assert(exn_raised());
            return NULL;
        } else {
            SetObjectArrayElement(jenv, array, i, element);
            assert(!exn_raised());
        }
    }
    
    return array;
}

JNIEXPORT jobject JNICALL Java_org_apache_harmony_vm_VMGenericsAndAnnotations_getDefaultValue
(JNIEnv *jenv, jclass, jlong jmethod) 
{
    Method* method = (Method*) ((POINTER_SIZE_INT) jmethod);
    TRACE("Requested default value for method " << method);
    AnnotationValue* value = method->get_default_value();
    if (value) {
        Class* antn_class = method->get_class();
        jthrowable error = NULL;
        // FIXME need to clarify against JSR-175 spec which exception should be raised
        jobject jval = resolve_annotation_value(jenv, antn_class, *value, antn_class, 
            method->get_name(), &error);
        if (!jval && error) {
            assert(!exn_raised());
            exn_raise_object(error);
        }
        assert(jval || exn_raised());
        return jval;
    }
    return NULL;
}
