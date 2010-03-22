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
 
#define LOG_DOMAIN "vm.core.reflection"
#include "cxxlog.h"

#include <assert.h>

#include "jni.h"
#include "jni_utils.h"
#include "environment.h"
#include "vm_strings.h"
#include "reflection.h"
#include "port_malloc.h"

#include "open/vm_type_access.h"
#include "open/vm_field_access.h"
#include "open/vm_method_access.h"
#include "open/vm_class_info.h"

#include "exceptions.h"
#include "heap.h"
#include "primitives_support.h"

jclass descriptor_to_jclass(Type_Info_Handle desc){
    Class_Handle clss = type_info_get_class(desc);
    if(!clss) {
        assert(exn_raised());
        return NULL;
    }

    return struct_Class_to_java_lang_Class_Handle(clss);
}

// Set parameterTypes and exceptionTypes fields for Method or Constructor object
jobjectArray reflection_get_parameter_types(JNIEnv *jenv, Method* method)
{
    jclass jlc_class = struct_Class_to_java_lang_Class_Handle(VM_Global_State::loader_env->JavaLangClass_Class);

    // Create an array of the argument types
    Method_Signature_Handle msh = method_get_signature(method);
    int nparams = method_args_get_number(msh);
    int start = method->is_static() ? 0 : 1;
    if (start) --nparams;

    jobjectArray arg_types = NewObjectArray(jenv, nparams, jlc_class, NULL);

    if (!arg_types) {
        return NULL;
    }

    int i;
    for (i = 0; i < nparams; i++) 
    {
        Type_Info_Handle arg_type = method_args_get_type_info(msh, i+start);
        jclass arg_clss = descriptor_to_jclass(arg_type);
        if (!arg_clss) return NULL;
        SetObjectArrayElement(jenv, arg_types, i, arg_clss);
    }

    return arg_types;
}

// Construct object of member_class (Constructor, Field, Method). 
static jobject reflect_member(JNIEnv *jenv, Class_Member* member, Class* type)
{
    ASSERT_RAISE_AREA;
    static Global_Env* genv = VM_Global_State::loader_env;
    static String* desc = genv->string_pool.lookup(
        "(JLjava/lang/Class;Ljava/lang/String;Ljava/lang/String;I)V");
    Method* member_constr = type->lookup_method(genv->Init_String, desc);

    jstring jname = String_to_interned_jstring(member->get_name());
    if (jname == NULL) {
        assert(exn_raised());
        return NULL;
    }
    jstring jdesc = String_to_interned_jstring(member->get_descriptor());
    if (jdesc == NULL) {
        assert(exn_raised());
        return NULL;
    }

    jvalue args[5];
    args[0].j = (jlong) ((POINTER_SIZE_INT) member);
    args[1].l = struct_Class_to_java_lang_Class_Handle(member->get_class());
    args[2].l = jname;
    args[3].l = jdesc;
    args[4].i = (jint)member->get_access_flags();
    jobject jmember = NewObjectA(jenv, struct_Class_to_java_lang_Class_Handle(type), 
        (jmethodID)member_constr, args);
    if (!jmember) {
        assert(exn_raised());
    }

    return jmember;
} //reflect_member

jobject reflection_reflect_method(JNIEnv *jenv, Method_Handle method)
{
    return reflect_member(jenv, method, 
        VM_Global_State::loader_env->java_lang_reflect_Method_Class);
}

jobject reflection_reflect_constructor(JNIEnv *jenv, Method_Handle constructor)
{
    return reflect_member(jenv, constructor, 
        VM_Global_State::loader_env->java_lang_reflect_Constructor_Class);
}

jobject reflection_reflect_field(JNIEnv *jenv, Field_Handle field)
{
    // We do not reflect injected fields
    //if (field_is_injected(field)) return NULL;

    return reflect_member(jenv, field, 
        VM_Global_State::loader_env->java_lang_reflect_Field_Class);
}

jobjectArray reflection_get_class_interfaces(JNIEnv* jenv, jclass clazz)
{
    Class_Handle clss = jni_get_class_handle(jenv, clazz);

    unsigned intf_number = class_number_implements(clss);

    jclass cclass = struct_Class_to_java_lang_Class_Handle( 
        VM_Global_State::loader_env->JavaLangClass_Class);

    jobjectArray arr = NewObjectArray(jenv, intf_number, cclass, NULL);
    if (! arr) 
        return NULL;

    // Fill the array
    for (unsigned i = 0; i < intf_number; i++) {
        jclass intf = struct_Class_to_java_lang_Class_Handle(class_get_implements(clss, i));
        SetObjectArrayElement(jenv, arr, i, intf);
    }

    return arr;
}

jobjectArray reflection_get_class_fields(JNIEnv* jenv, jclass clazz)
{
    Class_Handle clss = jni_get_class_handle(jenv, clazz);
    assert(clss);
    TRACE("get class fields : " << clss->get_name()->bytes);

    unsigned num_fields = clss->get_number_of_fields();
    unsigned num_res_fields = 0;

    unsigned i;
    // Determine the number of elements in the result.
    for (i = 0; i < num_fields; i++) {
        Field_Handle fh = clss->get_field(i);
        if (fh->is_injected()) continue;
        num_res_fields++;
    }

    // Create result array
    jclass fclazz = struct_Class_to_java_lang_Class_Handle(VM_Global_State::loader_env->java_lang_reflect_Field_Class);
    if (! fclazz) return NULL;
    jobjectArray farray = (jobjectArray) NewObjectArray(jenv, num_res_fields, fclazz, NULL);
    if (! farray) return NULL;

    // Fill in the array
    num_res_fields = 0;

    for(i = 0; i < num_fields; i++) {
        Field_Handle fh = class_get_field(clss, i);
        if (field_is_injected(fh)) continue;
        //if (class_is_array(clss) && 0 == strcmp("length", field_get_name(fh))) continue;

        jobject jfield = reflection_reflect_field(jenv, fh);
        if (!jfield){
            assert(exn_raised());
            return NULL;
        }
        SetObjectArrayElement(jenv, farray, num_res_fields, jfield);
        if (exn_raised()) return NULL;
        num_res_fields++;
    }

    return farray;
} // reflection_get_class_fields

jobjectArray reflection_get_class_constructors(JNIEnv* jenv, jclass clazz)
{
    Class_Handle clss = jni_get_class_handle(jenv, clazz);
    unsigned num_methods = clss->get_number_of_methods();
    unsigned n_consts = 0;
    TRACE("get class constructors : " << clss->get_name()->bytes);

    unsigned i;
    // Determine the number of elements in the result. Note that fake methods never have the name "<init>".
    for (i = 0; i < num_methods; i++) {
        Method_Handle mh = clss->get_method(i);
        if (strcmp(mh->get_name()->bytes, "<init>") == 0)
            n_consts++;
    }

    // Create result array
    jclass cclazz = struct_Class_to_java_lang_Class_Handle(
        VM_Global_State::loader_env->java_lang_reflect_Constructor_Class);
    if (!cclazz) return NULL;
    jobjectArray carray = (jobjectArray) NewObjectArray(jenv, n_consts, cclazz, NULL);
    if (!carray) return NULL;

    // Fill in the array
    for (i = 0, n_consts = 0; i < num_methods; i++) {
        Method_Handle mh = clss->get_method(i);
        if (strcmp(mh->get_name()->bytes, "<init>") != 0) continue;

        jobject jconst = reflection_reflect_constructor(jenv, mh);
        if (!jconst){
            assert(exn_raised());
            return NULL;
        }
        SetObjectArrayElement(jenv, carray, n_consts++, jconst);
        if (exn_raised()) return NULL;
    }

    return carray;
} // reflection_get_class_constructors

jobjectArray reflection_get_class_methods(JNIEnv* jenv, jclass clazz)
{
    Class_Handle clss = jni_get_class_handle(jenv, clazz);
    unsigned num_methods = clss->get_number_of_methods();
    unsigned num_res_methods = 0;
    TRACE("get class methods : " << clss->get_name()->bytes);

    unsigned i;
    // Determine the number of elements in the result.
    // Note that fake methods never have the name "<init>".
    for (i = 0; i < num_methods; i++) {
        Method_Handle mh = clss->get_method(i);
        if (strcmp(mh->get_name()->bytes, "<init>") == 0
            || strcmp(mh->get_name()->bytes, "<clinit>") == 0
            || mh->is_fake_method())
        {
            continue;
        }

        num_res_methods++;
    }

    // Create result array
    jclass member_class = struct_Class_to_java_lang_Class_Handle(VM_Global_State::loader_env->java_lang_reflect_Method_Class);
    if (! member_class) return NULL;
    jobjectArray member_array = NewObjectArray(jenv, num_res_methods, member_class, NULL);
    if (! member_array) return NULL;

    // Fill in the array
    unsigned member_i = 0;
    for (i = 0; i < num_methods; i++) {
        Method_Handle mh = clss->get_method(i);
        if (strcmp(mh->get_name()->bytes, "<init>") == 0
            || strcmp(mh->get_name()->bytes, "<clinit>") == 0
            || mh->is_fake_method())
        {
            continue;
        }

        jobject member = reflection_reflect_method(jenv, mh);
        if (!member){
            assert(exn_raised());
            return NULL;
        }
        SetObjectArrayElement(jenv, member_array, member_i, member);
        if (exn_raised()) 
            return NULL;

        member_i ++;
    }

    return member_array;
} // reflection_get_class_methods

/*
The following function eases conversion from jobjectArray parameters to 
JNI friendly jvalue array.
*/
bool jobjectarray_to_jvaluearray(JNIEnv *jenv, jvalue **output, Method *method, jobjectArray input)
{
    Arg_List_Iterator iter = method->get_argument_list();
    unsigned arg_number = 0;
    jvalue* array = *output;

    Java_Type type;
    while((type = curr_arg(iter)) != JAVA_TYPE_END) {
        jobject arg = GetObjectArrayElement(jenv, input, arg_number);
        if (type == JAVA_TYPE_ARRAY || type == JAVA_TYPE_CLASS) 
        {
            array[arg_number].l = arg;
        }
        else //unwrap to primitive
        {
            ASSERT(arg, ("Cannot unwrap NULL"));
            Class* arg_clss = jobject_to_struct_Class(arg);
            char arg_sig = is_wrapper_class(arg_clss->get_name()->bytes);
            char param_sig = (char)type;

            // actual parameter is not a wrapper
            if (0 == arg_sig) {
                ThrowNew_Quick(jenv, "java/lang/IllegalArgumentException",
                    "actual parameter for the primitive argument is not a wrapper object");
                return false;
            }

            array[arg_number] = unwrap_primitive(jenv, arg, arg_sig);

            if (!widen_primitive_jvalue(array + arg_number, arg_sig, param_sig)) {
                ThrowNew_Quick(jenv, "java/lang/IllegalArgumentException",
                    "widening conversion failed");
                return false;
            }
        }
        iter = advance_arg_iterator(iter);
        arg_number++;
    }

    return true;
} //jobjectarray_to_jvaluearray

jobject reflection_get_enum_value(JNIEnv *jenv, Class* enum_type, String* name) 
{
    ASSERT(enum_type->is_enum(), ("Requested Class is not ENUM: %s", 
        enum_type->get_name()->bytes));

    for (unsigned i=0; i<enum_type->get_number_of_fields(); i++) {
        if (enum_type->get_field(i)->get_name() == name) {
#ifndef NDEBUG
            ASSERT(enum_type->get_field(i)->is_enum(), ("Requested field is not ENUM: %d", name->bytes));
            const String* type = enum_type->get_name();
            const String* desc = enum_type->get_field(i)->get_descriptor();
            if (desc->len != (type->len + 2)
                || desc->bytes[0] != 'L' 
                || strncmp(desc->bytes + 1, type->bytes, type->len)
                || desc->bytes[type->len + 1] != ';')
            {
                LDIE(26, "Invalid enum field descriptor: {0}" << desc->bytes);
            }
#endif
            return GetStaticObjectField(jenv, 0, (jfieldID)(enum_type->get_field(i)));
        }
    }
    //public EnumConstantNotPresentException(Class<? extends Enum> enumType,String constantName)
    //ThrowNew_Quick(jenv, "java.lang.EnumConstantNotPresentException", name->bytes);
    return NULL;
}
