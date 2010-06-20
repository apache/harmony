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


#define LOG_DOMAIN "jni.method"
#include "cxxlog.h"

#include <assert.h>

#include "jni.h"
#include "jni_utils.h"
#include "jni_direct.h"

#include "vtable.h"
#include "Class.h"
#include "environment.h"
#include "exceptions.h"
#include "object_handles.h"
#include "open/vm_util.h"
#include "vm_threads.h"

#include "ini.h"
#include "nogc.h"
#include "open/vm_method_access.h"


static Method* lookup_method_init(Class* clss, const char* descr)
{
    String* method_name = VM_Global_State::loader_env->Init_String;
    String* method_descr =
        VM_Global_State::loader_env->string_pool.lookup(descr);

    return clss->lookup_method(method_name, method_descr);
} // lookup_method_init


static Method* lookup_method_clinit(Class* clss)
{
    return clss->lookup_method(VM_Global_State::loader_env->Clinit_String,
        VM_Global_State::loader_env->VoidVoidDescriptor_String);
} // lookup_method_clinit


jmethodID JNICALL GetMethodID(JNIEnv * jni_env,
                              jclass clazz,
                              const char *name,
                              const char *descr)
{
    TRACE2("jni", "GetMethodID called: " << name << descr);
    assert(hythread_is_suspend_enabled());
    assert(clazz);
    
    if (exn_raised()) return NULL;

    Class* clss = jclass_to_struct_Class(clazz);
    Method *method;
    if ('<' == *name) {
        if (!strcmp(name + 1, "init>")) {
            method = lookup_method_init(clss, descr);
        } else {
            ThrowNew_Quick(jni_env, "java/lang/NoSuchMethodError", name);
            return NULL;
        }
    } else {
        method = class_lookup_method_recursive(clss, name, descr);
    }

    if(!method || method->is_static()) {
        ThrowNew_Quick(jni_env, "java/lang/NoSuchMethodError", name);
        return NULL;
    }
    TRACE2("jni", "GetMethodID " << clss->get_name()->bytes
        << "." << name << " " << descr << " = " << (jmethodID)method);

    return (jmethodID)method;
} //GetMethodID



jmethodID JNICALL GetStaticMethodID(JNIEnv *jni_env,
                                    jclass clazz,
                                    const char *name,
                                    const char *descr)
{
    TRACE2("jni", "GetStaticMethodID called: " << name << descr);
    assert(hythread_is_suspend_enabled());
    Class* clss = jclass_to_struct_Class(clazz);
        
    if (exn_raised()) return NULL;

    Method *method;
    if ('<' == *name) {
        if (!strcmp(name + 1, "clinit>") && !strcmp(descr, "()V")) {
            method = lookup_method_clinit(clss);
        } else {
            ThrowNew_Quick(jni_env, "java/lang/NoSuchMethodError", name);
            return NULL;
        }
    } else {
        method = class_lookup_method_recursive(clss, name, descr);
    }

    if(!method || !method->is_static()) {
        ThrowNew_Quick(jni_env, "java/lang/NoSuchMethodError", name);
        return NULL;
    }
    TRACE2("jni", "GetStaticMethodID " << clss->get_name()->bytes
        << "." << name << " " << descr << " = " << (jmethodID)method);

    return (jmethodID)method;
} //GetStaticMethodID

static Method *object_lookup_method(jobject obj, const String* name, const String* desc) 
{
    ObjectHandle h = (ObjectHandle)obj;

    tmn_suspend_disable(); // v----------
    VTable *vtable = ((ManagedObject *)h->object)->vt();
    tmn_suspend_enable();  // ^----------

    assert(vtable);

    Method *method = class_lookup_method_recursive(vtable->clss, name, desc);
    if (method == 0) {
        LDIE(21, "Can't find method {0} {1}" << name->bytes << desc->bytes);
    }

    return method;
}


/////////////////////////////////////////////////////////////////////////////
// begin Call<Type>MethodA functions


static void call_method_no_ref_result(JNIEnv * jni_env,
                                      jobject obj,
                                      jmethodID methodID,
                                      jvalue *args,
                                      jvalue *result,
                                      int non_virtual)
{
    assert(hythread_is_suspend_enabled());
    Method *method = (Method *)methodID;

    if ( !non_virtual && !method_is_private(method) ) {
        // lookup the underlying "real" (e.g. abstract) method
        method = object_lookup_method(obj, method->get_name(), method->get_descriptor());
    }

    // Check method is not abstract
    // Alternative solution is to restore exception stubs in 
    // class_parse_methods() in Class_File_Loader.cpp, 
    // and to add similar functionality to interpreter
    if (method->is_abstract()) {
        ThrowNew_Quick (jni_env, "java/lang/AbstractMethodError", 
                "attempt to invoke abstract method");
        return;
    }

    if (!ensure_initialised(jni_env, method->get_class()))
        return;

    unsigned num_args = method->get_num_args();
    jvalue *all_args = (jvalue*) STD_ALLOCA(num_args * sizeof(jvalue));

    all_args[0].l = obj;
    memcpy(all_args + 1, args, (num_args - 1) * sizeof(jvalue));

    tmn_suspend_disable(); // v----------
    vm_execute_java_method_array((jmethodID)method, result, all_args);
    tmn_suspend_enable();  // ^----------
} //call_method_no_ref_result



void JNICALL CallVoidMethod(JNIEnv * jni_env, jobject obj, jmethodID methodID, ...)
{
    TRACE2("jni", "CallVoidMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    CallVoidMethodV(jni_env, obj, methodID, args);
} //CallVoidMethod



void JNICALL CallVoidMethodV(JNIEnv * jni_env, jobject obj, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallVoidMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    CallVoidMethodA(jni_env, obj, methodID, jvalue_args);
    STD_FREE(jvalue_args);
} //CallVoidMethodV



void JNICALL CallVoidMethodA(JNIEnv * jni_env,
                             jobject obj,
                             jmethodID methodID,
                             jvalue *args)
{
    TRACE2("jni", "CallVoidMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue UNREF result;
    
    if (exn_raised()) return;

    String *name = ((Method*)methodID)->get_name();
    bool non_virtual = name == VM_Global_State::loader_env->Init_String;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, non_virtual);
} //CallVoidMethodA



jobject JNICALL CallObjectMethod(JNIEnv * jni_env, jobject obj, jmethodID methodID, ...)
{
    TRACE2("jni", "CallObjectMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallObjectMethodV(jni_env, obj, methodID, args);
} //CallObjectMethod



jobject JNICALL CallObjectMethodV(JNIEnv * jni_env, jobject obj, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallObjectMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jobject result = CallObjectMethodA(jni_env, obj, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallObjectMethodV



jobject JNICALL CallObjectMethodA(JNIEnv * jni_env,
                                  jobject obj,
                                  jmethodID methodID,
                                  jvalue *args)
{
    TRACE2("jni", "CallObjectMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return NULL;

    jvalue result;
    Method *method = (Method *)methodID; // resolve to actual vtable entry below

    if (! method_is_private(method)) {
        // lookup the underlying "real" (e.g. abstract) method
        method = object_lookup_method(obj, method->get_name(), method->get_descriptor());
    }

    // Check method is not abstract
    // Alternative solution is to restore exception stubs in 
    // class_parse_methods() in Class_File_Loader.cpp, 
    // and to add similar functionality to interpreter
    if (method->is_abstract()) {
        ThrowNew_Quick (jni_env, "java/lang/AbstractMethodError", 
                "attempt to invoke abstract method");
        return NULL;
    }

    if (!ensure_initialised(jni_env, method->get_class()))
        return NULL;

    unsigned num_args = method->get_num_args();
    jvalue *all_args = (jvalue*) STD_ALLOCA(num_args * sizeof(jvalue));

    all_args[0].l = obj;
    memcpy(all_args + 1, args, (num_args - 1) * sizeof(jvalue));

    tmn_suspend_disable(); // v----------
    vm_execute_java_method_array((jmethodID)method, &result, all_args);
    tmn_suspend_enable();  // ^----------

    return result.l;
} //CallObjectMethodA



jboolean JNICALL CallBooleanMethod(JNIEnv * jni_env, jobject obj, jmethodID methodID, ...)
{
    TRACE2("jni", "CallBooleanMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallBooleanMethodV(jni_env, obj, methodID, args);
} //CallBooleanMethod



jboolean JNICALL CallBooleanMethodV(JNIEnv * jni_env, jobject obj, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallBooleanMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jboolean result = CallBooleanMethodA(jni_env, obj, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallBooleanMethodV



jboolean JNICALL CallBooleanMethodA(JNIEnv * jni_env,
                                    jobject obj,
                                    jmethodID methodID,
                                    jvalue *args)
{
    TRACE2("jni", "CallBooleanMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
        
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, FALSE);
    return result.z;
} //CallBooleanMethodA



jbyte JNICALL CallByteMethod(JNIEnv * jni_env, jobject obj, jmethodID methodID, ...)
{
    TRACE2("jni", "CallByteMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallByteMethodV(jni_env, obj, methodID, args);
} //CallByteMethod



jbyte JNICALL CallByteMethodV(JNIEnv * jni_env, jobject obj, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallByteMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jbyte result = CallByteMethodA(jni_env, obj, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallByteMethodV



jbyte JNICALL CallByteMethodA(JNIEnv * jni_env,
                              jobject obj,
                              jmethodID methodID,
                              jvalue *args)
{
    TRACE2("jni", "CallByteMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, FALSE);
    return result.b;
} //CallByteMethodA




jchar JNICALL CallCharMethod(JNIEnv * jni_env, jobject obj, jmethodID methodID, ...)
{
    TRACE2("jni", "CallCharMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallCharMethodV(jni_env, obj, methodID, args);
} //CallCharMethod



jchar JNICALL CallCharMethodV(JNIEnv * jni_env, jobject obj, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallCharMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jchar result = CallCharMethodA(jni_env, obj, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallCharMethodV



jchar JNICALL CallCharMethodA(JNIEnv * jni_env,
                              jobject obj,
                              jmethodID methodID,
                              jvalue *args)
{
    TRACE2("jni", "CallCharMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, FALSE);
    return result.c;
} //CallCharMethodA




jshort JNICALL CallShortMethod(JNIEnv * jni_env, jobject obj, jmethodID methodID, ...)
{
    TRACE2("jni", "CallShortMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallShortMethodV(jni_env, obj, methodID, args);
} //CallShortMethod



jshort JNICALL CallShortMethodV(JNIEnv * jni_env, jobject obj, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallShortMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jshort result = CallShortMethodA(jni_env, obj, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallShortMethodV



jshort JNICALL CallShortMethodA(JNIEnv * jni_env,
                                jobject obj,
                                jmethodID methodID,
                                jvalue *args)
{
    TRACE2("jni", "CallShortMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, FALSE);
    return result.s;
} //CallShortMethodA




jint JNICALL CallIntMethod(JNIEnv * jni_env, jobject obj, jmethodID methodID, ...)
{
    TRACE2("jni", "CallIntMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallIntMethodV(jni_env, obj, methodID, args);
} //CallIntMethod



jint JNICALL CallIntMethodV(JNIEnv * jni_env, jobject obj, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallIntMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jint result = CallIntMethodA(jni_env, obj, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallIntMethodV



jint JNICALL CallIntMethodA(JNIEnv * jni_env,
                              jobject obj,
                              jmethodID methodID,
                              jvalue *args)
{
    TRACE2("jni", "CallIntMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, FALSE);
    return result.i;
} //CallIntMethodA




jlong JNICALL CallLongMethod(JNIEnv * jni_env, jobject obj, jmethodID methodID, ...)
{
    TRACE2("jni", "CallLongMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallLongMethodV(jni_env, obj, methodID, args);
} //CallLongMethod



jlong JNICALL CallLongMethodV(JNIEnv * jni_env, jobject obj, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallLongMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jlong result = CallLongMethodA(jni_env, obj, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallLongMethodV



jlong JNICALL CallLongMethodA(JNIEnv * jni_env,
                              jobject obj,
                              jmethodID methodID,
                              jvalue *args)
{
    TRACE2("jni", "CallLongMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, FALSE);
    return result.j;
} //CallLongMethodA




jfloat JNICALL CallFloatMethod(JNIEnv * jni_env, jobject obj, jmethodID methodID, ...)
{
    TRACE2("jni", "CallFloatMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallFloatMethodV(jni_env, obj, methodID, args);
} //CallFloatMethod



jfloat JNICALL CallFloatMethodV(JNIEnv * jni_env, jobject obj, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallFloatMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jfloat result = CallFloatMethodA(jni_env, obj, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallFloatMethodV



jfloat JNICALL CallFloatMethodA(JNIEnv * jni_env,
                                jobject obj,
                                jmethodID methodID,
                                jvalue *args)
{
    TRACE2("jni", "CallFloatMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, FALSE);
    return result.f;
} //CallFloatMethodA




jdouble JNICALL CallDoubleMethod(JNIEnv * jni_env, jobject obj, jmethodID methodID, ...)
{
    TRACE2("jni", "CallDoubleMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallDoubleMethodV(jni_env, obj, methodID, args);
} //CallDoubleMethod



jdouble JNICALL CallDoubleMethodV(JNIEnv * jni_env, jobject obj, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallDoubleMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jdouble result = CallDoubleMethodA(jni_env, obj, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallDoubleMethodV



jdouble JNICALL CallDoubleMethodA(JNIEnv * jni_env,
                                  jobject obj,
                                  jmethodID methodID,
                                  jvalue *args)
{
    TRACE2("jni", "CallDoubleMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, FALSE);
    return result.d;
} //CallDoubleMethodA




// end Call<Type>MethodA functions
/////////////////////////////////////////////////////////////////////////////



/////////////////////////////////////////////////////////////////////////////
// begin CallNonvirtual<Type>MethodA functions


void JNICALL CallNonvirtualVoidMethod(JNIEnv * jni_env,
                                       jobject obj,
                                       jclass clazz,
                                       jmethodID methodID,
                                       ...)
{
    TRACE2("jni", "CallNonvirtualVoidMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    CallNonvirtualVoidMethodV(jni_env, obj, clazz, methodID, args);
} //CallNonvirtualVoidMethod



void JNICALL CallNonvirtualVoidMethodV(JNIEnv * jni_env,
                                       jobject obj,
                                       jclass clazz,
                                       jmethodID methodID,
                                       va_list args)
{
    TRACE2("jni", "CallNonvirtualVoidMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    CallNonvirtualVoidMethodA(jni_env, obj, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
} //CallNonvirtualVoidMethodV



void JNICALL CallNonvirtualVoidMethodA(JNIEnv * jni_env,
                                       jobject obj,
                                       jclass UNREF clazz,
                                       jmethodID methodID,
                                       jvalue *args)
{
    TRACE2("jni", "CallNonvirtualVoidMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return;

    call_method_no_ref_result(jni_env, obj, methodID, args, 0, TRUE);
} //CallNonvirtualVoidMethodA



jobject JNICALL CallNonvirtualObjectMethod(JNIEnv * jni_env,
                                           jobject obj,
                                           jclass clazz,
                                           jmethodID methodID,
                                           ...)
{
    TRACE2("jni", "CallNonvirtualObjectMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallNonvirtualObjectMethodV(jni_env, obj, clazz, methodID, args);
} //CallNonvirtualObjectMethod



jobject JNICALL CallNonvirtualObjectMethodV(JNIEnv * jni_env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            va_list args)
{
    TRACE2("jni", "CallNonvirtualObjectMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jobject result = CallNonvirtualObjectMethodA(jni_env, obj, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallNonvirtualObjectMethodV



jobject JNICALL CallNonvirtualObjectMethodA(JNIEnv * jni_env,
                                            jobject obj,
                                            jclass UNREF clazz,
                                            jmethodID methodID,
                                            jvalue *args)
{
    TRACE2("jni", "CallNonvirtualObjectMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return NULL;

    jvalue result;
    Method *method = (Method *)methodID;
    if (!ensure_initialised(jni_env, method->get_class()))
        return NULL;
    unsigned num_args = method->get_num_args();
    jvalue *all_args = (jvalue*) STD_ALLOCA(num_args * sizeof(jvalue));


    all_args[0].l = obj;
    memcpy(all_args + 1, args, (num_args - 1) * sizeof(jvalue));
    tmn_suspend_disable();
    vm_execute_java_method_array(methodID, &result, all_args);
    tmn_suspend_enable();
 
    return result.l;
} //CallNonvirtualObjectMethodA



jboolean JNICALL CallNonvirtualBooleanMethod(JNIEnv * jni_env,
                                             jobject obj,
                                             jclass clazz,
                                             jmethodID methodID,
                                             ...)
{
    TRACE2("jni", "CallNonvirtualBooleanMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallNonvirtualBooleanMethodV(jni_env, obj, clazz, methodID, args);
} //CallNonvirtualBooleanMethod



jboolean JNICALL CallNonvirtualBooleanMethodV(JNIEnv * jni_env,
                                              jobject obj,
                                              jclass clazz,
                                              jmethodID methodID,
                                              va_list args)
{
    TRACE2("jni", "CallNonvirtualBooleanMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jboolean result = CallNonvirtualBooleanMethodA(jni_env, obj, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallNonvirtualBooleanMethodV



jboolean JNICALL CallNonvirtualBooleanMethodA(JNIEnv * jni_env,
                                              jobject obj,
                                              jclass UNREF clazz,
                                              jmethodID methodID,
                                              jvalue *args)
{
    TRACE2("jni", "CallNonvirtualBooleanMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, TRUE);
    return result.z;
} //CallNonvirtualBooleanMethodA



jbyte JNICALL CallNonvirtualByteMethod(JNIEnv * jni_env,
                                       jobject obj,
                                       jclass clazz,
                                       jmethodID methodID,
                                       ...)
{
    TRACE2("jni", "CallNonvirtualByteMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallNonvirtualByteMethodV(jni_env, obj, clazz, methodID, args);
} //CallNonvirtualByteMethod



jbyte JNICALL CallNonvirtualByteMethodV(JNIEnv * jni_env,
                                        jobject obj,
                                        jclass clazz,
                                        jmethodID methodID,
                                        va_list args)
{
    TRACE2("jni", "CallNonvirtualByteMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jbyte result = CallNonvirtualByteMethodA(jni_env, obj, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallNonvirtualByteMethodV



jbyte JNICALL CallNonvirtualByteMethodA(JNIEnv * jni_env,
                                        jobject obj,
                                        jclass UNREF clazz,
                                        jmethodID methodID,
                                        jvalue *args)
{
    TRACE2("jni", "CallNonvirtualByteMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, TRUE);
    return result.b;
} //CallNonvirtualByteMethodA



jchar JNICALL CallNonvirtualCharMethod(JNIEnv * jni_env,
                                       jobject obj,
                                       jclass clazz,
                                       jmethodID methodID,
                                       ...)
{
    TRACE2("jni", "CallNonvirtualCharMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallNonvirtualCharMethodV(jni_env, obj, clazz, methodID, args);
} //CallNonvirtualCharMethod



jchar JNICALL CallNonvirtualCharMethodV(JNIEnv * jni_env,
                                        jobject obj,
                                        jclass clazz,
                                        jmethodID methodID,
                                        va_list args)
{
    TRACE2("jni", "CallNonvirtualCharMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jchar result = CallNonvirtualCharMethodA(jni_env, obj, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallNonvirtualCharMethodV



jchar JNICALL CallNonvirtualCharMethodA(JNIEnv * jni_env,
                                        jobject obj,
                                        jclass UNREF clazz,
                                        jmethodID methodID,
                                        jvalue *args)
{
    TRACE2("jni", "CallNonvirtualCharMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, TRUE);
    return result.c;
} //CallNonvirtualCharMethodA



jshort JNICALL CallNonvirtualShortMethod(JNIEnv * jni_env,
                                         jobject obj,
                                         jclass clazz,
                                         jmethodID methodID,
                                         ...)
{
    TRACE2("jni", "CallNonvirtualShortMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallNonvirtualShortMethodV(jni_env, obj, clazz, methodID, args);
} //CallNonvirtualShortMethod



jshort JNICALL CallNonvirtualShortMethodV(JNIEnv * jni_env,
                                          jobject obj,
                                          jclass clazz,
                                          jmethodID methodID,
                                          va_list args)
{
    TRACE2("jni", "CallNonvirtualShortMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jshort result = CallNonvirtualShortMethodA(jni_env, obj, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallNonvirtualShortMethodV



jshort JNICALL CallNonvirtualShortMethodA(JNIEnv * jni_env,
                                          jobject obj,
                                          jclass UNREF clazz,
                                          jmethodID methodID,
                                          jvalue *args)
{
    TRACE2("jni", "CallNonvirtualShortMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, TRUE);
    return result.s;
} //CallNonvirtualShortMethodA



jint JNICALL CallNonvirtualIntMethod(JNIEnv * jni_env,
                                     jobject obj,
                                     jclass clazz,
                                     jmethodID methodID,
                                     ...)
{
    TRACE2("jni", "CallNonvirtualIntMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallNonvirtualIntMethodV(jni_env, obj, clazz, methodID, args);
} //CallNonvirtualIntMethod



jint JNICALL CallNonvirtualIntMethodV(JNIEnv * jni_env,
                                      jobject obj,
                                      jclass clazz,
                                      jmethodID methodID,
                                      va_list args)
{
    TRACE2("jni", "CallNonvirtualIntMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jint result = CallNonvirtualIntMethodA(jni_env, obj, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallNonvirtualIntMethodV



jint JNICALL CallNonvirtualIntMethodA(JNIEnv * jni_env,
                                      jobject obj,
                                      jclass UNREF clazz,
                                      jmethodID methodID,
                                      jvalue *args)
{
    TRACE2("jni", "CallNonvirtualIntMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result,  TRUE);
    return result.i;
} //CallNonvirtualIntMethodA



jlong JNICALL CallNonvirtualLongMethod(JNIEnv * jni_env,
                                       jobject obj,
                                       jclass clazz,
                                       jmethodID methodID,
                                       ...)
{
    TRACE2("jni", "CallNonvirtualLongMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallNonvirtualLongMethodV(jni_env, obj, clazz, methodID, args);
} //CallNonvirtualLongMethod



jlong JNICALL CallNonvirtualLongMethodV(JNIEnv * jni_env,
                                        jobject obj,
                                        jclass clazz,
                                        jmethodID methodID,
                                        va_list args)
{
    TRACE2("jni", "CallNonvirtualLongMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jlong result = CallNonvirtualLongMethodA(jni_env, obj, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallNonvirtualLongMethodV



jlong JNICALL CallNonvirtualLongMethodA(JNIEnv * jni_env,
                                        jobject obj,
                                        jclass UNREF clazz,
                                        jmethodID methodID,
                                        jvalue *args)
{
    TRACE2("jni", "CallNonvirtualLongMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, TRUE);
    return result.j;
} //CallNonvirtualLongMethodA



jfloat JNICALL CallNonvirtualFloatMethod(JNIEnv * jni_env,
                                         jobject obj,
                                         jclass clazz,
                                         jmethodID methodID,
                                         ...)
{
    TRACE2("jni", "CallNonvirtualFloatMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallNonvirtualFloatMethodV(jni_env, obj, clazz, methodID, args);
} //CallNonvirtualFloatMethod



jfloat JNICALL CallNonvirtualFloatMethodV(JNIEnv * jni_env,
                                          jobject obj,
                                          jclass clazz,
                                          jmethodID methodID,
                                          va_list args)
{
    TRACE2("jni", "CallNonvirtualFloatMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jfloat result = CallNonvirtualFloatMethodA(jni_env, obj, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallNonvirtualFloatMethodV



jfloat JNICALL CallNonvirtualFloatMethodA(JNIEnv * jni_env,
                                          jobject obj,
                                          jclass UNREF clazz,
                                          jmethodID methodID,
                                          jvalue *args)
{
    TRACE2("jni", "CallNonvirtualFloatMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, TRUE);
    return result.f;
} //CallNonvirtualFloatMethodA



jdouble JNICALL CallNonvirtualDoubleMethod(JNIEnv * jni_env,
                                           jobject obj,
                                           jclass clazz,
                                           jmethodID methodID,
                                           ...)
{
    TRACE2("jni", "CallNonvirtualDoubleMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallNonvirtualDoubleMethodV(jni_env, obj, clazz, methodID, args);
} //CallNonvirtualDoubleMethod



jdouble JNICALL CallNonvirtualDoubleMethodV(JNIEnv * jni_env,
                                            jobject obj,
                                            jclass clazz,
                                            jmethodID methodID,
                                            va_list args)
{
    TRACE2("jni", "CallNonvirtualDoubleMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jdouble result = CallNonvirtualDoubleMethodA(jni_env, obj, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallNonvirtualDoubleMethodV



jdouble JNICALL CallNonvirtualDoubleMethodA(JNIEnv * jni_env,
                                            jobject obj,
                                            jclass UNREF clazz,
                                            jmethodID methodID,
                                            jvalue *args)
{
    TRACE2("jni", "CallNonvirtualDoubleMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_method_no_ref_result(jni_env, obj, methodID, args, &result, TRUE);
    return result.d;
} //CallNonvirtualDoubleMethodA



// end CallNonvirtual<Type>MethodA functions
/////////////////////////////////////////////////////////////////////////////



/////////////////////////////////////////////////////////////////////////////
// begin CallStatic<Type>MethodA functions


static void call_static_method_no_ref_result(JNIEnv * jni_env,
                                             jclass UNREF clazz,
                                             jmethodID methodID,
                                             jvalue *args,
                                             jvalue *result)
{
    assert(hythread_is_suspend_enabled());
    Method *method = (Method *)methodID;
    if (!ensure_initialised(jni_env, method->get_class()))
        return;
    tmn_suspend_disable();
    vm_execute_java_method_array(methodID, result, args);
    tmn_suspend_enable();
} //call_static_method_no_ref_result



jobject JNICALL CallStaticObjectMethod(JNIEnv * jni_env, jclass clazz, jmethodID methodID, ...)
{
    TRACE2("jni", "CallStaticObjectMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallStaticObjectMethodV(jni_env, clazz, methodID, args);
} //CallStaticObjectMethod



jobject JNICALL CallStaticObjectMethodV(JNIEnv * jni_env, jclass clazz, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallStaticObjectMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jobject result = CallStaticObjectMethodA(jni_env, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallStaticObjectMethodV



jobject JNICALL CallStaticObjectMethodA(JNIEnv * jni_env,
                                        jclass UNREF clazz,
                                        jmethodID methodID,
                                        jvalue *args)
{
    TRACE2("jni", "CallStaticObjectMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return NULL;

    jvalue result;
    Method *method = (Method *)methodID;
    if (!ensure_initialised(jni_env, method->get_class()))
        return NULL;
    unsigned num_args = method->get_num_args();
    jvalue *all_args = (jvalue*) STD_ALLOCA(num_args * sizeof(jvalue));

    memcpy(all_args, args, num_args * sizeof(jvalue));
    tmn_suspend_disable();
    vm_execute_java_method_array(methodID, &result, all_args);
    tmn_suspend_enable();
 
    return result.l;
} //CallStaticObjectMethodA



jboolean JNICALL CallStaticBooleanMethod(JNIEnv * jni_env, jclass clazz, jmethodID methodID, ...)
{
    TRACE2("jni", "CallStaticBooleanMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallStaticBooleanMethodV(jni_env, clazz, methodID, args);
} //CallStaticBooleanMethod



jboolean JNICALL CallStaticBooleanMethodV(JNIEnv * jni_env, jclass clazz, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallStaticBooleanMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jboolean result = CallStaticBooleanMethodA(jni_env, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallStaticBooleanMethodV



jboolean JNICALL CallStaticBooleanMethodA(JNIEnv * jni_env,
                                          jclass clazz,
                                          jmethodID methodID,
                                          jvalue *args)
{
    TRACE2("jni", "CallStaticBooleanMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_static_method_no_ref_result(jni_env, clazz, methodID, args, &result);
    return result.z;
} //CallStaticBooleanMethodA



jbyte JNICALL CallStaticByteMethod(JNIEnv * jni_env, jclass clazz, jmethodID methodID, ...)
{
    TRACE2("jni", "CallStaticByteMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallStaticByteMethodV(jni_env, clazz, methodID, args);
} //CallStaticByteMethod



jbyte JNICALL CallStaticByteMethodV(JNIEnv * jni_env, jclass clazz, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallStaticByteMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jbyte result = CallStaticByteMethodA(jni_env, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallStaticByteMethodV



jbyte JNICALL CallStaticByteMethodA(JNIEnv * jni_env,
                                    jclass clazz,
                                    jmethodID methodID,
                                    jvalue *args)
{
    TRACE2("jni", "CallStaticByteMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_static_method_no_ref_result(jni_env, clazz, methodID, args, &result);
    return result.b;
} //CallStaticByteMethodA



jchar JNICALL CallStaticCharMethod(JNIEnv * jni_env, jclass clazz, jmethodID methodID, ...)
{
    TRACE2("jni", "CallStaticCharMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallStaticCharMethodV(jni_env, clazz, methodID, args);
} //CallStaticCharMethod



jchar JNICALL CallStaticCharMethodV(JNIEnv * jni_env, jclass clazz, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallStaticCharMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jchar result = CallStaticCharMethodA(jni_env, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallStaticCharMethodV



jchar JNICALL CallStaticCharMethodA(JNIEnv * jni_env,
                                    jclass clazz,
                                    jmethodID methodID,
                                    jvalue *args)
{
    TRACE2("jni", "CallStaticCharMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_static_method_no_ref_result(jni_env, clazz, methodID, args, &result);
    return result.c;
} //CallStaticCharMethodA



jshort JNICALL CallStaticShortMethod(JNIEnv * jni_env, jclass clazz, jmethodID methodID, ...)
{
    TRACE2("jni", "CallStaticShortMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallStaticShortMethodV(jni_env, clazz, methodID, args);
} //CallStaticShortMethod



jshort JNICALL CallStaticShortMethodV(JNIEnv * jni_env, jclass clazz, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallStaticShortMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jshort result = CallStaticShortMethodA(jni_env, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallStaticShortMethodV



jshort JNICALL CallStaticShortMethodA(JNIEnv * jni_env,
                                      jclass clazz,
                                      jmethodID methodID,
                                      jvalue *args)
{
    TRACE2("jni", "CallStaticShortMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_static_method_no_ref_result(jni_env, clazz, methodID, args, &result);
    return result.s;
} //CallStaticShortMethodA



jint JNICALL CallStaticIntMethod(JNIEnv * jni_env, jclass clazz, jmethodID methodID, ...)
{
    TRACE2("jni", "CallStaticIntMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallStaticIntMethodV(jni_env, clazz, methodID, args);
} //CallStaticIntMethod



jint JNICALL CallStaticIntMethodV(JNIEnv * jni_env, jclass clazz, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallStaticIntMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jint result = CallStaticIntMethodA(jni_env, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallStaticIntMethodV



jint JNICALL CallStaticIntMethodA(JNIEnv * jni_env,
                                  jclass clazz,
                                  jmethodID methodID,
                                  jvalue *args)
{
    TRACE2("jni", "CallStaticIntMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_static_method_no_ref_result(jni_env, clazz, methodID, args, &result);
    return result.i;
} //CallStaticIntMethodA



jlong JNICALL CallStaticLongMethod(JNIEnv * jni_env, jclass clazz, jmethodID methodID, ...)
{
    TRACE2("jni", "CallStaticLongMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallStaticLongMethodV(jni_env, clazz, methodID, args);
} //CallStaticLongMethod



jlong JNICALL CallStaticLongMethodV(JNIEnv * jni_env, jclass clazz, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallStaticLongMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jlong result = CallStaticLongMethodA(jni_env, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallStaticLongMethodV



jlong JNICALL CallStaticLongMethodA(JNIEnv * jni_env,
                                    jclass clazz,
                                    jmethodID methodID,
                                    jvalue *args)
{
    TRACE2("jni", "CallStaticLongMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_static_method_no_ref_result(jni_env, clazz, methodID, args, &result);
    return result.j;
} //CallStaticLongMethodA



jfloat JNICALL CallStaticFloatMethod(JNIEnv * jni_env, jclass clazz, jmethodID methodID, ...)
{
    TRACE2("jni", "CallStaticFloatMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallStaticFloatMethodV(jni_env, clazz, methodID, args);
} //CallStaticFloatMethod



jfloat JNICALL CallStaticFloatMethodV(JNIEnv * jni_env, jclass clazz, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallStaticFloatMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jfloat result = CallStaticFloatMethodA(jni_env, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallStaticFloatMethodV



jfloat JNICALL CallStaticFloatMethodA(JNIEnv * jni_env,
                                      jclass clazz,
                                      jmethodID methodID,
                                      jvalue *args)
{
    TRACE2("jni", "CallStaticFloatMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_static_method_no_ref_result(jni_env, clazz, methodID, args, &result);
    return result.f;
} //CallStaticFloatMethodA



jdouble JNICALL CallStaticDoubleMethod(JNIEnv * jni_env, jclass clazz, jmethodID methodID, ...)
{
    TRACE2("jni", "CallStaticDoubleMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    return CallStaticDoubleMethodV(jni_env, clazz, methodID, args);
} //CallStaticDoubleMethod



jdouble JNICALL CallStaticDoubleMethodV(JNIEnv * jni_env, jclass clazz, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallStaticDoubleMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    jdouble result = CallStaticDoubleMethodA(jni_env, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
    return result;
} //CallStaticDoubleMethodV



jdouble JNICALL CallStaticDoubleMethodA(JNIEnv * jni_env,
                                        jclass clazz,
                                        jmethodID methodID,
                                        jvalue *args)
{
    TRACE2("jni", "CallStaticDoubleMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return 0;

    jvalue result;
    call_static_method_no_ref_result(jni_env, clazz, methodID, args, &result);
    return result.d;
} //CallStaticDoubleMethodA



void JNICALL CallStaticVoidMethod(JNIEnv * jni_env, jclass clazz, jmethodID methodID, ...)
{
    TRACE2("jni", "CallStaticVoidMethod called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    va_list args;
    va_start(args, methodID);
    CallStaticVoidMethodV(jni_env, clazz, methodID, args);
} //CallStaticVoidMethod



void JNICALL CallStaticVoidMethodV(JNIEnv * jni_env, jclass clazz, jmethodID methodID, va_list args)
{
    TRACE2("jni", "CallStaticVoidMethodV called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    jvalue *jvalue_args = get_jvalue_arg_array((Method *)methodID, args);
    CallStaticVoidMethodA(jni_env, clazz, methodID, jvalue_args);
    STD_FREE(jvalue_args);
} //CallStaticVoidMethodV



void JNICALL CallStaticVoidMethodA(JNIEnv * jni_env,
                                   jclass clazz,
                                   jmethodID methodID,
                                   jvalue *args)
{
    TRACE2("jni", "CallStaticVoidMethodA called, id = " << methodID);
    assert(hythread_is_suspend_enabled());
    
    if (exn_raised()) return;

    call_static_method_no_ref_result(jni_env, clazz, methodID, args, 0);
} //CallStaticVoidMethodA



// end CallStatic<Type>MethodA functions
/////////////////////////////////////////////////////////////////////////////
