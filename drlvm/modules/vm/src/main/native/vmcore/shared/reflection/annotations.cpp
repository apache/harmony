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
 
#define LOG_DOMAIN "vm.core.annotations"
#include "cxxlog.h"

#include <sstream>
#include <assert.h>

#include "open/vm_method_access.h"
#include "Class.h"
#include "annotations.h"
#include "jni.h"
#include "jni_utils.h"
#include "type.h"
#include "environment.h"
#include "reflection.h"
#include "exceptions.h"
#include "primitives_support.h"
#include "vm_log.h"
#include "open/vm_type_access.h"

jobjectArray get_annotations(JNIEnv* jenv, AnnotationTable* table, AnnotationTable* inv_table, Class* clss) 
{
    unsigned table_num = table ? table->length : 0;
    TRACE("annotations table size = " << table_num);

    unsigned inv_table_num = inv_table ? inv_table->length : 0;
    TRACE("invisible annotations table size = " << inv_table_num);

    unsigned num = table_num + inv_table_num;

    // HARMONY-5086 resolution - because there can be annotations which can't
    // be resolved (and must be just ignored) the size of result array is
    // unknown actually, so temporary location is used.
    jobject* tmp_array = (jobject*) malloc(sizeof(jobject) * num);
    unsigned result_array_size = 0;
    jobjectArray result_array = NULL;

    jthrowable skip = NULL;
    for (unsigned i = 0; i < table_num; i++) {
        jobject element = resolve_annotation(jenv, table->table[i], clss, &skip);
        if (exn_raised()) {
            assert(!element);
            goto bail;
        } else if (!element) { 
            // just skip unresolved annotation
            continue;
        }
        tmp_array[result_array_size++] = element;
    }
    for (unsigned i = 0; i < inv_table_num; i++) {
        jobject element = resolve_annotation(jenv, inv_table->table[i], clss, &skip);
        if (exn_raised()) {
            assert(!element);
            goto bail;
        } else if (!element) { 
            // just skip unresolved annotation
            continue;
        }
        tmp_array[result_array_size++] = element;
   }

    // copy resolved annotations to result java array
    static Class* antn_class;

    if (antn_class == NULL) {
        antn_class = jni_get_vm_env(jenv)->LoadCoreClass(
            "java/lang/annotation/Annotation");
    }
    result_array = NewObjectArray(jenv, result_array_size, 
        struct_Class_to_java_lang_Class_Handle(antn_class), NULL);

    if (!result_array) {
        assert(exn_raised());
        goto bail;
    }

    for (unsigned i = 0; i < result_array_size; i++) {
        SetObjectArrayElement(jenv, result_array, i, tmp_array[i]);
        assert(!exn_raised());
    }
    
bail:
    free(tmp_array);
    return result_array;
}

static Class* field_descriptor_to_type(JNIEnv* jenv, String* desc, Class* clss, 
                                       jthrowable* cause = NULL)
{
    Type_Info_Handle tih = (Type_Info_Handle)
        type_desc_create_from_java_descriptor(desc->bytes, clss->get_class_loader());
    if (tih) {
        Class* type = type_info_get_class(tih);
        if (type) {
            return type;
        }

        assert(exn_raised());
        jthrowable jfailure = exn_get();
        ASSERT(jfailure, ("FIXME lazy exceptions handling"));
        exn_clear();
        jthrowable jnewfailure = exn_create("java/lang/TypeNotPresentException",
            tih->get_type_name()->bytes, jfailure);
        if (jnewfailure) {
            if (cause) {
                *cause = jnewfailure;
            } else {
                exn_raise_object(jnewfailure);
            }
        } else {
            assert(exn_raised());
        }
    } 
    else //malformed descriptor
    {
        std::stringstream ss;
        ss << "Malformed type descriptor : " << desc->bytes;
        ThrowNew_Quick(jenv, "java/lang/annotation/AnnotationFormatError", ss.str().c_str());
    }

    return NULL;
}

jobject resolve_annotation(JNIEnv* jenv, Annotation* antn, Class* clss, jthrowable* cause)
{
    assert(antn);
    Class* antn_type = field_descriptor_to_type(jenv, antn->type, clss, cause);
    if (!antn_type) {
        return NULL;
    }
    if (!antn_type->is_annotation()) {
        std::stringstream ss;
        ss << "Non-annotation type : " << antn->type->bytes;
        ThrowNew_Quick(jenv, "java/lang/annotation/AnnotationFormatError", ss.str().c_str());
        return NULL;
    }
    TRACE("Annotation type " << antn_type);

    static Global_Env* genv = jni_get_vm_env(jenv);
    static Class* factory_class = genv->LoadCoreClass(
        "org/apache/harmony/lang/annotation/AnnotationFactory");
    static jmethodID factory_method = (jmethodID)class_lookup_method(factory_class, "createAnnotation", 
        "(Ljava/lang/Class;[Lorg/apache/harmony/lang/annotation/AnnotationMember;)"
        "Ljava/lang/annotation/Annotation;");

    static Class* element_class = genv->LoadCoreClass(
        "org/apache/harmony/lang/annotation/AnnotationMember");
    static jmethodID element_ctor = (jmethodID)class_lookup_method(element_class, "<init>", 
        "(Ljava/lang/String;Ljava/lang/Object;)V");

    unsigned num = antn->num_elements;
    jobjectArray array = NULL;
    if (num) {
        jclass jelc = struct_Class_to_java_lang_Class_Handle(element_class);
        array = NewObjectArray(jenv, num, jelc, NULL);

        if (!array) {
            assert(exn_raised());
            return NULL;
        }

        for (unsigned i = 0; i < num; ++i) {
            jstring name = NewStringUTF(jenv, antn->elements[i].name->bytes);
            if (name) {
                jobject element = NULL;
                jthrowable error = NULL;
                jobject value = resolve_annotation_value(jenv, clss, 
                    antn->elements[i].value, antn_type, 
                    antn->elements[i].name, &error);
                if (value) {
                    element = NewObject(jenv, jelc, element_ctor, name, value);
                } else if (error) {
                    element = NewObject(jenv, jelc, element_ctor, name, error);
                }
                if (element) {
                    SetObjectArrayElement(jenv, array, i, element);
                    continue;
                }
            } 
            assert(exn_raised());
            return NULL;
        }
    }
    return CallStaticObjectMethod(jenv, 
        struct_Class_to_java_lang_Class_Handle(factory_class), factory_method, 
        struct_Class_to_java_lang_Class_Handle(antn_type), array);
} // resolve_annotation

static jobject process_enum_value(JNIEnv* jenv, AnnotationValue& value, Class* clss, 
                                  String* name, jthrowable* cause) 
{            
    TRACE("resolving enum type of annotation value : " << value.enum_const.type->bytes);

    // fail immediately if no enum type found
    // FIXME this behaviour should be evaluated against JSR-175 spec
    Class* enum_type = field_descriptor_to_type(jenv, value.enum_const.type, clss);
    if (enum_type) {
        if (enum_type->is_enum()) {
            jobject enum_value = reflection_get_enum_value(jenv, enum_type, value.enum_const.name);
            if (enum_value) {
                return enum_value;
            } else {
                static Global_Env* genv = jni_get_vm_env(jenv);
                static Class* ECNPE_class = genv->LoadCoreClass(
                    "java/lang/EnumConstantNotPresentException");
                static jmethodID ECNPE_ctor = (jmethodID)class_lookup_method(ECNPE_class, "<init>", 
                    "(Ljava/lang/Class;Ljava/lang/String;)V");
                jstring jname = NewStringUTF(jenv, value.enum_const.name->bytes);
                if (jname) {
                    *cause = NewObject(jenv, 
                        struct_Class_to_java_lang_Class_Handle(ECNPE_class), ECNPE_ctor, 
                        struct_Class_to_java_lang_Class_Handle(enum_type), jname);
                }
            }
        } else {
            std::stringstream ss;
            ss << "Invalid enum type " << enum_type->get_name()->bytes
                << " specified for value of element \'" << name->bytes << "\'";
            ThrowNew_Quick(jenv, "java/lang/annotation/AnnotationFormatError", ss.str().c_str());
        }
    }
    return NULL;
}

static bool process_array_element(JNIEnv* jenv, Class* clss, 
                                  AnnotationValue& value, Class* antn_type, 
                                  String* name, jthrowable* cause, 
                                  jarray array, unsigned idx, Class* type) 
{
    Global_Env* genv = jni_get_vm_env(jenv);
    switch (value.tag) {
    case AVT_INT:
        {
            if (type == genv->Int_Class) {
                jint buf = (jint)value.const_value.i;
                SetIntArrayRegion(jenv, array, (jsize)idx, (jsize)1, &buf);
                return true;
            }
        }
    case AVT_BOOLEAN:
        {
            if (type == genv->Boolean_Class) {
                jboolean buf = (jboolean)value.const_value.i;
                SetBooleanArrayRegion(jenv, array, (jsize)idx, (jsize)1, &buf);
                return true;
            }
        }
    case AVT_BYTE:
        {
            if (type == genv->Byte_Class) {
                jbyte buf = (jbyte)value.const_value.i;
                SetByteArrayRegion(jenv, array, (jsize)idx, (jsize)1, &buf);
                return true;
            }
        }
    case AVT_SHORT:
        {
            if (type == genv->Short_Class) {
                jshort buf = (jshort)value.const_value.i;
                SetShortArrayRegion(jenv, array, (jsize)idx, (jsize)1, &buf);
                return true;
            }
        }
    case AVT_CHAR:
        {
            if (type == genv->Char_Class) {
                jchar buf = (jchar)value.const_value.i;
                SetCharArrayRegion(jenv, array, (jsize)idx, (jsize)1, &buf);
                return true;
            }
        }
    case AVT_LONG:
        {
            if (type == genv->Long_Class) {
                jlong buf = (jlong)value.const_value.j;
                SetLongArrayRegion(jenv, array, (jsize)idx, (jsize)1, &buf);
                return true;
            }
        }
    case AVT_FLOAT:
        {
            if (type == genv->Float_Class) {
                jfloat buf = (jfloat)value.const_value.f;
                SetFloatArrayRegion(jenv, array, (jsize)idx, (jsize)1, &buf);
                return true;
            }
        }
    case AVT_DOUBLE:
        {
            if (type == genv->Double_Class) {
                jdouble buf = (jdouble)value.const_value.d;
                SetDoubleArrayRegion(jenv, array, (jsize)idx, (jsize)1, &buf);
                return true;
            }

            // primitive value type does not match array type
            std::stringstream ss;
            ss << "Encountered value tag \'" << (char)value.tag 
                << "\' does not match array type " << type->get_name()->bytes << "[]";

            // FIXME should it be AnnotationFormatError??
            // need to check with JSR-175 spec
            *cause = CreateNewThrowable(jenv, genv->java_lang_ArrayStoreException_Class, 
                ss.str().c_str(), NULL);
        }
    default: 
        {
            jobject jelement = resolve_annotation_value(jenv, clss, value, antn_type, name, cause);
            if (jelement) {
                SetObjectArrayElement(jenv, array, idx, jelement);
                if (exn_raised()) {
                    *cause = exn_get();
                    exn_clear();
                } else {
                    return true;
                }
            } 
        }
    } // switch

    assert(cause || exn_raised());
    return false;
}


jobject resolve_annotation_value(JNIEnv* jenv, Class* clss, 
                                 AnnotationValue& value, Class* antn_type, 
                                 String* name, jthrowable* cause)
{
    TRACE("Resolving value of tag " << (char)value.tag);
    jvalue jconst;
    switch (value.tag) {
    case AVT_INT:
    case AVT_BOOLEAN:
    case AVT_BYTE:
    case AVT_SHORT:
    case AVT_CHAR:
        jconst.i = value.const_value.i;
        return wrap_primitive(jenv, jconst, (char)value.tag);
    case AVT_LONG:
        jconst.j = value.const_value.j;
        return wrap_primitive(jenv, jconst, (char)value.tag);
    case AVT_FLOAT:
        jconst.f = value.const_value.f;
        return wrap_primitive(jenv, jconst, (char)value.tag);
    case AVT_DOUBLE:
        jconst.d = value.const_value.d;
        return wrap_primitive(jenv, jconst, (char)value.tag);

    case AVT_STRING:
        return NewStringUTF(jenv, value.const_value.string->bytes);

    case AVT_CLASS: 
        {
            // preserve failure if no Class value found
            Class* type = field_descriptor_to_type(jenv, value.class_name, clss, cause);
            return type ? struct_Class_to_java_lang_Class_Handle(type) : NULL;
        }

    case AVT_ANNOTN:
        return resolve_annotation(jenv, value.nested, clss, cause);

    case AVT_ENUM:
        return process_enum_value(jenv, value, clss, name, cause);

    case AVT_ARRAY:
        {
            // Need to find out class of array elements.
            // As zero-sized arrays are legal, have to check return type 
            // of the element-defining method.
            Class* arr_type = NULL;
            unsigned i;
            for (i = 0; i < antn_type->get_number_of_methods(); i++) {
                Method* m = antn_type->get_method(i);
                if (m->get_name() == name && m->get_num_args() == 1) 
                {
                    Type_Info_Handle tih = method_ret_type_get_type_info(
                        method_get_signature((Method_Handle)m));
                    if (type_info_is_vector(tih)) {
                        arr_type = type_info_get_class(tih);
                        if (!arr_type) {
                            // if loading failed - exception should be raised
                            assert(exn_raised());
                            return NULL;
                        }
                    } 
                    break;
                }
            }
            if (!arr_type || arr_type->get_number_of_dimensions() != 1) {
                std::stringstream ss;
                ss << "Invalid array value for element \'" << name->bytes << "\'";
                ThrowNew_Quick(jenv, "java/lang/annotation/AnnotationFormatError", ss.str().c_str());
                return NULL;
            }
            arr_type = arr_type->get_array_element_class();

            jclass jarr_type = struct_Class_to_java_lang_Class_Handle(arr_type);
            jarray array = NewObjectArray(jenv, (jsize)value.array.length, jarr_type, NULL);

            if (!array) {
                assert(exn_raised());
                return NULL;
            }

            for (i = 0; i < value.array.length; ++i) {
                if (!process_array_element(jenv, clss,
                    value.array.items[i], antn_type, 
                    name, cause, array, i, arr_type)) 
                {
                    return NULL;
                }
                assert(!exn_raised());
            }
            return array;
        }
    } // switch

    std::stringstream ss;
    ss << "Unrecognized value tag \'" << (char)value.tag 
        << "\' for element \'" << name->bytes << "\'";
    ThrowNew_Quick(jenv, "java/lang/annotation/AnnotationFormatError", ss.str().c_str());
    
    return NULL;
} // resolve_annotation_value

