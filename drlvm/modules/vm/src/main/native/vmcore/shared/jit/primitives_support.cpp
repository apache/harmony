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

#include "primitives_support.h"
#include "jni_utils.h"

bool widen_primitive_jvalue(jvalue* val, char from_type, char to_type)
{
    switch (to_type) {
    case 'B':
        switch (from_type) {
        case 'B':
            return true;
        default:
            return false;
        }
    case 'C':
        switch (from_type) {
        case 'C':
            return true;
        default:
            return false;
        }
    case 'D':
        switch (from_type) {
        case 'B':
            val->d = val->b;
            return true;
        case 'C':
            val->d = val->c;
            return true;
        case 'D':
            return true;
        case 'F':
            val->d = val->f;
            return true;
        case 'I':
            val->d = val->i;
            return true;
        case 'J':
            val->d = (jdouble)val->j;
            return true;
        case 'S':
            val->d = val->s;
            return true;
        default:
            return false;
        }
    case 'F':
        switch (from_type) {
        case 'B':
            val->f = val->b;
            return true;
        case 'C':
            val->f = val->c;
            return true;
        case 'F':
            return true;
        case 'I':
            val->f = (jfloat)val->i;
            return true;
        case 'J':
            val->f = (jfloat)val->j;
        case 'S':
            val->f = val->s;
            return true;
        default:
            return false;
            }
        case 'I':
            switch (from_type) {
        case 'B':
            val->i = val->b;
            return true;
        case 'C':
            val->i = val->c;
            return true;
        case 'I':
            return true;
        case 'S':
            val->i = val->s;
            return true;
        default:
            return false;
        }
    case 'J':
        switch (from_type) {
        case 'B':
            val->j = val->b;
            return true;
        case 'C':
            val->j = val->c;
            return true;
        case 'I':
            val->j = val->i;
            return true;
        case 'J':
            return true;
        case 'S':
            val->j = val->s;
            return true;
        default:
            return false;
        }
    case 'S':
        switch (from_type) {
        case 'B':
            val->s = val->b;
            return true;
        case 'S':
            return true;
        default:
            return false;
        }
    case 'Z':
        switch (from_type) {
        case 'Z':
            return true;
        default:
            return false;
        }
    default:
        return false;
    }
} //widen_primitive_jvalue

jobject wrap_primitive(JNIEnv *env, jvalue value, char sig)
{
    jvalue args[1];
    jobject retobj = NULL;
    jclass clazz;

    // Initialization of static variables in scope changed to ifs
    // because Microsoft Visual Studio (.NET 2003) generates the code
    // which first checks variable initialization mask and than calls
    // to initialization code. This may lead to race condition between
    // two threads initializing the variable and one of threads works
    // with unitialized value for some time.
    // This implementation may lead to several initializations of the
    // static variable, but this is harmful in terms of stability.
    // NB: it is worth checking all usage of statics in DRLVM code.
    switch (sig) {
    case 'Z':
    {
        args[0].z = value.z;

        // Allocate java/lang/Boolean object:
        clazz = (jclass)gh_jlboolean;
        static jmethodID init_Boolean;
        if(!init_Boolean) {
            init_Boolean = GetMethodID(env, clazz, "<init>",  "(Z)V");
        }
        retobj = NewObjectA (env, clazz, init_Boolean, args);
        break;
    }
    case 'B':
    {
        args[0].b = value.b;

        // Allocate java/lang/Byte object
        clazz = (jclass)gh_jlbyte;
        static jmethodID init_Byte;
        if(!init_Byte) {
            init_Byte = GetMethodID(env, clazz, "<init>",  "(B)V");
        }
        retobj = NewObjectA (env, clazz, init_Byte, args);
        break;
    }
    case 'C':
    {
        args[0].c = value.c;

        // Allocate java/lang/Character object:
        clazz = (jclass)gh_jlchar;
        static jmethodID init_Char;
        if(!init_Char) {
            init_Char = GetMethodID(env, clazz, "<init>",  "(C)V");
        }
        retobj = NewObjectA (env, clazz, init_Char, args);
        break;
    }
    case 'S':
    {
        args[0].s = value.s;

        // Allocate java/lang/Short object:
        clazz = (jclass)gh_jlshort;
        static jmethodID init_Short;
        if(!init_Short) {
            init_Short = GetMethodID(env, clazz, "<init>",  "(S)V");
        }
        retobj = NewObjectA (env, clazz, init_Short, args);
        break;
    }
    case 'I':
    {
        args[0].i = value.i;

        // Allocate java/lang/Integer object:
        clazz = (jclass)gh_jlint;
        static jmethodID init_Int;
        if(!init_Int) {
            init_Int = GetMethodID(env, clazz, "<init>",  "(I)V");
        }
        assert(init_Int);
        retobj = NewObjectA (env, clazz, init_Int, args);
        break;
    }
    case 'J':
    {
        args[0].j = value.j;

        // Allocate java/lang/Long object:
        clazz = (jclass)gh_jllong;
        static jmethodID init_Long;
        if(!init_Long) {
           init_Long = GetMethodID(env, clazz, "<init>",  "(J)V");
        }
        retobj = NewObjectA (env, clazz, init_Long, args);
        break;
    }
    case 'F':
    {
        args[0].f = value.f;

        // Allocate java/lang/Float object:
        clazz = (jclass)gh_jlfloat;
        static jmethodID init_Float;
        if(!init_Float) {
            init_Float = GetMethodID(env, clazz, "<init>",  "(F)V");
        }
        retobj = NewObjectA (env, clazz, init_Float, args);
        break;
    }
    case 'D':
    {
        args[0].d = value.d;

        // Allocate java/lang/Double object:
        clazz = (jclass)gh_jldouble;
        static jmethodID init_Double;
        if(!init_Double) {
            init_Double = GetMethodID(env, clazz, "<init>",  "(D)V");
        }
        retobj = NewObjectA (env, clazz, init_Double, args);
        break;
    }
    default:
        LDIE(58, "Unknown type descriptor");
    }

    return retobj;
} // wrap_primitive

jvalue unwrap_primitive(JNIEnv *env, jobject wobj, char sig)
{
    jvalue value;
    jfieldID value_id;

    switch (sig) { // Value argument signature
    case 'Z':
        // Get the fieldID of the value field of a Boolean object:
        value_id = gid_boolean_value;
        value.z = GetBooleanField (env, wobj, value_id);
        break;
    case 'B':
        // Get the fieldID of the value field of a Byte object:
        value_id = gid_byte_value;
        value.b = GetByteField (env, wobj, value_id);
        break;  
    case 'C':
        // Get the fieldID of the value field of a Character object:
        value_id = gid_char_value;
        value.c = GetCharField (env, wobj, value_id);
        break;
    case 'S':
        // Get the fieldID of the value field of a Short object:
        value_id = gid_short_value;
        value.s = GetShortField (env, wobj, value_id);
        break;
    case 'I':
        // Get the fieldID of the value field of a Integer object:
        value_id = gid_int_value;
        value.i = GetIntField (env, wobj, value_id);
        break;
    case 'J':
        // Get the fieldID of the value field of a Long object:
        value_id = gid_long_value;
        value.j = GetLongField (env, wobj, value_id);
        break;  
    case 'F':
        // Get the fieldID of the value field of a Float object:
        value_id = gid_float_value;
        value.f = GetFloatField (env, wobj, value_id);
        break;
    case 'D':
        // Get the fieldID of the value field of a Double object:
        value_id = gid_double_value;
        value.d = GetDoubleField (env, wobj, value_id);
        break;
    default:
        LDIE(58, "Unknown type descriptor");
    }

    return value;
} // unwrap_primitive

char is_wrapper_class(const char* name)
{
    char _sig = '\0';
    const char* c_ptr = (name);
    const char* type = "java/lang/";

    while (*type++ == *c_ptr++);

    type--;
    c_ptr--;

    if (*type != '\0') _sig = '\0';
    else {
        switch (*c_ptr) {
        case 'B':
            if (*(c_ptr+1) == 'o') {
                _sig = 'Z';
                type = "Boolean";
            }
            else {
                _sig = 'B';
                type = "Byte";
            }
            break;
        case 'C':
            _sig = 'C';
            type = "Character";
            break;
        case 'D':
            _sig = 'D';
            type = "Double";
            break;
        case 'F':
            _sig = 'F';
            type = "Float";
            break;
        case 'I':
            _sig = 'I';
            type = "Integer";
            break;
        case 'L':
            _sig = 'J';
            type = "Long";
            break;
        case 'S':
            _sig = 'S';
            type = "Short";
        }

        while ((*c_ptr == *type) && (*c_ptr != '\0')) {
            c_ptr++;
            type++;
        }
        if ((*c_ptr != '\0') || (*type != '\0'))
            _sig = '\0';
    }

    return _sig;
} // is_wrapper_class
