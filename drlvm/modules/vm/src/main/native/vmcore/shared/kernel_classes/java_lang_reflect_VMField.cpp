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
 * @file java_lang_reflect_VMField.cpp
 *
 * This file is a part of kernel class natives VM core component.
 * It contains implementation for native methods of 
 * java.lang.reflect.VMField class.
 */

#define LOG_DOMAIN "vm.core.reflection"
#include "cxxlog.h"
#include "vm_log.h"

#include "reflection.h"
#include "environment.h"
#include "exceptions.h"
#include "primitives_support.h"
#include "jni_utils.h"
#include "type.h"

#include "java_lang_reflect_VMField.h"

// return value of a field of primitive type.
static jvalue read_primitive(JNIEnv* jenv, jfieldID field_id, jobject obj, char field_sig)
{
    jclass declaring_class = NULL; //unused
    bool is_static = ((Field*) field_id)->is_static();
    jvalue primitive_value;
    primitive_value.j = 0;

    switch (field_sig) {
    case 'B':
        primitive_value.b = (is_static) ?
            GetStaticByteField(jenv, declaring_class, field_id) :
        GetByteField(jenv, obj, field_id);
        break;
    case 'C':
        primitive_value.c = (is_static) ?
            GetStaticCharField(jenv, declaring_class, field_id) :
        GetCharField(jenv, obj, field_id);
        break;
    case 'D':
        primitive_value.d = (is_static) ?
            GetStaticDoubleField(jenv, declaring_class, field_id) :
        GetDoubleField(jenv, obj, field_id);
        break;
    case 'F':
        primitive_value.f = (is_static) ?
            GetStaticFloatField(jenv, declaring_class, field_id) :
        GetFloatField(jenv, obj, field_id);
        break;
    case 'I':
        primitive_value.i = (is_static) ?
            GetStaticIntField(jenv, declaring_class, field_id) :
        GetIntField(jenv, obj, field_id);
        break;
    case 'J':
        primitive_value.j = (is_static) ?
            GetStaticLongField(jenv, declaring_class, field_id) :
        GetLongField(jenv, obj, field_id);
        break;
    case 'S':
        primitive_value.s = (is_static) ?
            GetStaticShortField(jenv, declaring_class, field_id) :
        GetShortField(jenv, obj, field_id);
        break;
    case 'Z':
        primitive_value.z = (is_static) ?
            GetStaticBooleanField(jenv, declaring_class, field_id) :
        GetBooleanField(jenv, obj, field_id);
        break;
    default:
        DIE(("Unexpected type descriptor: %c", field_sig));
    }

    return primitive_value;
}

static jvalue get_primitive_field(JNIEnv *jenv, jobject obj, jlong jmember, char to_type) 
{
    Field_Handle field = (Field_Handle) ((POINTER_SIZE_INT) jmember);

    TRACE("read field value : " << field);

    jvalue result;
    result.j = 0;
    if (field->get_field_type_desc()->is_primitive()) {
        char field_sig = field->get_descriptor()->bytes[0];
        result = read_primitive(jenv, (jfieldID)field, obj, field_sig);
        if (widen_primitive_jvalue(&result, field_sig, to_type)) {
            return result;
        }
    }
    if (!exn_raised()) {
        ThrowNew_Quick(jenv, "java/lang/IllegalArgumentException", field->get_descriptor()->bytes);
    }
    return result;
}

JNIEXPORT jboolean JNICALL Java_java_lang_reflect_VMField_getBoolean
(JNIEnv *jenv, jclass, jobject obj, jlong jmember) 
{
    return get_primitive_field(jenv, obj, jmember, 'Z').z;
}

JNIEXPORT jbyte JNICALL Java_java_lang_reflect_VMField_getByte
(JNIEnv *jenv, jclass, jobject obj, jlong jmember) 
{
    return get_primitive_field(jenv, obj, jmember, 'B').b;
}

JNIEXPORT jchar JNICALL Java_java_lang_reflect_VMField_getChar
(JNIEnv *jenv, jclass, jobject obj, jlong jmember) 
{
    return get_primitive_field(jenv, obj, jmember, 'C').c;
}

JNIEXPORT jshort JNICALL Java_java_lang_reflect_VMField_getShort
(JNIEnv *jenv, jclass, jobject obj, jlong jmember) 
{
    return get_primitive_field(jenv, obj, jmember, 'S').s;
}

JNIEXPORT jint JNICALL Java_java_lang_reflect_VMField_getInt
(JNIEnv *jenv, jclass, jobject obj, jlong jmember) 
{
    return get_primitive_field(jenv, obj, jmember, 'I').i;
}

JNIEXPORT jlong JNICALL Java_java_lang_reflect_VMField_getLong
(JNIEnv *jenv, jclass, jobject obj, jlong jmember) 
{
    return get_primitive_field(jenv, obj, jmember, 'J').j;
}

JNIEXPORT jfloat JNICALL Java_java_lang_reflect_VMField_getFloat
(JNIEnv *jenv, jclass, jobject obj, jlong jmember) 
{
    return get_primitive_field(jenv, obj, jmember, 'F').f;
}

JNIEXPORT jdouble JNICALL Java_java_lang_reflect_VMField_getDouble
(JNIEnv *jenv, jclass, jobject obj, jlong jmember) 
{
    return get_primitive_field(jenv, obj, jmember, 'D').d;
}

/*
 * Class:     java_lang_reflect_VMField
 * Method:    getObject
 * Signature: (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_java_lang_reflect_VMField_getObject
  (JNIEnv *jenv, jclass, jobject obj, jlong member)
{
    Field_Handle field = (Field_Handle) ((POINTER_SIZE_INT) member);
    TRACE("get field value : " << field);

    jobject retobj = NULL;

    if (field->get_field_type_desc()->is_primitive())
    {
        char field_sig = field->get_descriptor()->bytes[0];
        jvalue primitive_value = read_primitive(jenv, (jfieldID)field, obj, field_sig);
        if (!exn_raised()) {
            retobj = wrap_primitive(jenv, primitive_value, field_sig);
        }
    } 
    else if (field->is_static()) 
    { 
        retobj = GetStaticObjectField(jenv, NULL, (jfieldID)field);
    } 
    else 
    {
        retobj = GetObjectField(jenv, obj, (jfieldID)field);    
    } 

    return retobj;
} // Java_java_lang_reflect_VMField_getObject

static void write_primitive(JNIEnv* jenv, Field* field, jobject obj, jvalue primitive_value, char value_sig) 
{
    char field_sig = field->get_descriptor()->bytes[0];

    if (! widen_primitive_jvalue(&primitive_value, value_sig, field_sig)) {
        ThrowNew_Quick(jenv, "java/lang/IllegalArgumentException", 
            "Widening conversion failed");
        return;
    }

    jclass declaring_class = NULL; //UNUSED
    jfieldID field_id = (jfieldID) field;
    bool is_static = field->is_static();

    switch (field_sig) {
    case 'B':
        if (is_static)
            SetStaticByteField(jenv, declaring_class, field_id, primitive_value.b);
        else
            SetByteField(jenv, obj, field_id, primitive_value.b);

        break;
    case 'C':
        if (is_static)
            SetStaticCharField(jenv, declaring_class, field_id, primitive_value.c);
        else
            SetCharField(jenv, obj, field_id, primitive_value.c);

        break;
    case 'D':
        if (is_static)
            SetStaticDoubleField(jenv, declaring_class, field_id, primitive_value.d);
        else
            SetDoubleField(jenv, obj, field_id, primitive_value.d);

        break;
    case 'F':
        if (is_static)
            SetStaticFloatField(jenv, declaring_class, field_id, primitive_value.f);
        else
            SetFloatField(jenv, obj, field_id, primitive_value.f);

        break;
    case 'I':
        if (is_static)
            SetStaticIntField(jenv, declaring_class, field_id, primitive_value.i);
        else
            SetIntField(jenv, obj, field_id, primitive_value.i);

        break;
    case 'J':
        if (is_static)
            SetStaticLongField(jenv, declaring_class, field_id, primitive_value.j);
        else
            SetLongField(jenv, obj, field_id, primitive_value.j);

        break;
    case 'S':
        if (is_static)
            SetStaticShortField(jenv, declaring_class, field_id, primitive_value.s);
        else
            SetShortField(jenv, obj, field_id, primitive_value.s);

        break;
    case 'Z':
        if (is_static)
            SetStaticBooleanField(jenv, declaring_class, field_id, primitive_value.z);
        else
            SetBooleanField(jenv, obj, field_id, primitive_value.z);

        break;
    default:
        DIE(("Unexpected type descriptor"));
    }

    return;
}

/*
 * Class:     java_lang_reflect_VMField
 * Method:    setFieldValue
 * Signature: (Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_java_lang_reflect_VMField_setObject
  (JNIEnv *jenv, jclass, jobject obj, jlong jfield, jobject value)
{
    Field_Handle field = (Field_Handle) ((POINTER_SIZE_INT) jfield);
    TRACE("set field value : " << field);

    if (field->get_field_type_desc()->is_primitive()) 
    {
        char value_sig = value ? is_wrapper_class(jobject_to_struct_Class(value)->get_name()->bytes) : 0;
        if (!value_sig) {
            // the value is not primitive
            ThrowNew_Quick(jenv, "java/lang/IllegalArgumentException",
                "The specified value cannot be unboxed to primitive");
            return;
        }    
        jvalue primitive_value = unwrap_primitive(jenv, value, value_sig);
        write_primitive(jenv, field, obj, primitive_value, value_sig);
        return;
    } 
    if (value) {
        // check type
        Class* value_clss = jobject_to_struct_Class(value);
        Class* clss = field->get_field_type_desc()->load_type_desc();
        assert(clss);
        if (!value_clss->is_instanceof(clss)) {
            ThrowNew_Quick(jenv, "java/lang/IllegalArgumentException",
                "The specified value cannot be converted to the field's type type by an identity or widening conversions");
            return;
        }
    }

    if (field->is_static())
    {
        SetStaticObjectField(jenv, NULL, (jfieldID)field, value);
    } 
    else 
    {
        SetObjectField(jenv, obj, (jfieldID)field, value);
    }
}

// set value to field of primitive type
static void set_primitive_field(JNIEnv* jenv, jlong jfield, jobject obj,
                                       jvalue primitive, char value_type)
{
    Field_Handle field = (Field_Handle) ((POINTER_SIZE_INT) jfield);
    TRACE("set field value : " << field);
    write_primitive(jenv, field, obj, primitive, value_type);
}


JNIEXPORT void JNICALL Java_java_lang_reflect_VMField_setBoolean
(JNIEnv *jenv, jclass, jobject obj, jlong jfield, jboolean value)
{
    jvalue primitive;
    primitive.z = value;
    set_primitive_field(jenv, jfield, obj, primitive, 'Z');
} 

JNIEXPORT void JNICALL Java_java_lang_reflect_VMField_setChar
(JNIEnv *jenv, jclass, jobject obj, jlong jfield, jchar value)
{
    jvalue primitive;
    primitive.c = value;
    set_primitive_field(jenv, jfield, obj, primitive, 'C');
} 

JNIEXPORT void JNICALL Java_java_lang_reflect_VMField_setByte
(JNIEnv *jenv, jclass, jobject obj, jlong jfield, jbyte value)
{
    jvalue primitive;
    primitive.b = value;
    set_primitive_field(jenv, jfield, obj, primitive, 'B');
} 

JNIEXPORT void JNICALL Java_java_lang_reflect_VMField_setShort
(JNIEnv *jenv, jclass, jobject obj, jlong jfield, jshort value)
{
    jvalue primitive;
    primitive.s = value;
    set_primitive_field(jenv, jfield, obj, primitive, 'S');
} 

JNIEXPORT void JNICALL Java_java_lang_reflect_VMField_setInt
(JNIEnv *jenv, jclass, jobject obj, jlong jfield, jint value)
{
    jvalue primitive;
    primitive.i = value;
    set_primitive_field(jenv, jfield, obj, primitive, 'I');
} 

JNIEXPORT void JNICALL Java_java_lang_reflect_VMField_setLong
(JNIEnv *jenv, jclass, jobject obj, jlong jfield, jlong value)
{
    jvalue primitive;
    primitive.j = value;
    set_primitive_field(jenv, jfield, obj, primitive, 'J');
} 

JNIEXPORT void JNICALL Java_java_lang_reflect_VMField_setFloat
(JNIEnv *jenv, jclass, jobject obj, jlong jfield, jfloat value)
{
    jvalue primitive;
    primitive.f = value;
    set_primitive_field(jenv, jfield, obj, primitive, 'F');
} 

JNIEXPORT void JNICALL Java_java_lang_reflect_VMField_setDouble
(JNIEnv *jenv, jclass, jobject obj, jlong jfield, jdouble value)
{
    jvalue primitive;
    primitive.d = value;
    set_primitive_field(jenv, jfield, obj, primitive, 'D');
} 
