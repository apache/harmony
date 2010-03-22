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
#define LOG_DOMAIN "vm.core.classes"
#include "cxxlog.h"

#include "open/vm_class_manipulation.h"
#include "open/vm_class_loading.h"
#include "Package.h"
#include "classloader.h"
#include "jni_utils.h"
#include "reflection.h"
#include "stack_trace.h"

#include "vm_strings.h"

#include "java_lang_VMClassRegistry.h"
#include "java_lang_ClassLoader.h"

// This function is for native library support
// It takes a class name with .s not /s. 
// FIXME: caller could convert it itself
Class_Handle class_find_loaded(Class_Loader_Handle loader, const char* name)
{
    char* name3 = strdup(name);
    char* p = name3;
    while (*p) {
        if (*p=='.') *p='/';
        p++;
    }
    Global_Env* env = VM_Global_State::loader_env;
    String* name2 = env->string_pool.lookup(name3);
    Class* ch;
    if (loader) {
        ch = loader->LookupClass(name2);
    } else {
        ch = env->bootstrap_class_loader->LookupClass(name2);
    }
    STD_FREE(name3);
    if(ch && (!ch->verify(env) || !ch->prepare(env))) return NULL;
    return ch;
}

Class_Handle class_find_class_from_loader(Class_Loader_Handle loader, const char* n, Boolean init)
{
    ASSERT_RAISE_AREA;
    assert(hythread_is_suspend_enabled()); // -salikh
    char *new_name = strdup(n);
    char *p = new_name;
    while (*p) {
        if (*p == '.') *p = '/';
        p++;
    }
    String* name = VM_Global_State::loader_env->string_pool.lookup(new_name);
    STD_FREE(new_name);
    Class* ch;
    if (loader) {
        ch = class_load_verify_prepare_by_loader_jni(
            VM_Global_State::loader_env, name, loader);
    } else {
        assert(hythread_is_suspend_enabled());
        ch = class_load_verify_prepare_from_jni(VM_Global_State::loader_env, name);
    }
    if (!ch) return NULL;
    // All initialization from jni should not propagate exceptions and
    // should return to calling native method.
    if(init) {
        class_initialize_from_jni(ch);

        if (exn_raised()) {
            return NULL;
        }
    }

    if(exn_raised()) {
        return 0;
    }

    return ch;
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    defineClass
 * Signature: (Ljava/lang/String;[BII)Ljava/lang/Class;
 */
JNIEXPORT jclass JNICALL Java_java_lang_ClassLoader_defineClass0 
  (JNIEnv *jenv, jobject cl, jstring name, jbyteArray data, jint offset, jint len)
{
    const char* clssname = NULL;

    // obtain char * for the name if provided
    if(name) {
        clssname = GetStringUTFChars(jenv, name, NULL);
    }

    // obtain raw classfile data data pointer
    jboolean is_copy;
    jbyte *bytes = GetByteArrayElements(jenv, data, &is_copy);
    assert(bytes);

    // define class
    jclass clss = DefineClass(jenv, clssname, cl, bytes + offset, len);

    // release JNI objects
    ReleaseByteArrayElements(jenv, data, bytes, 0);
    
    if(clssname)
      ReleaseStringUTFChars(jenv, name, clssname);

    return clss;
}

JNIEXPORT jclass JNICALL Java_java_lang_VMClassRegistry_loadBootstrapClass
    (JNIEnv *jenv, jclass, jstring name)
{
    // obtain char* for the name
    const char* buf = GetStringUTFChars(jenv, name, NULL);
    // set flag to detect if the requested class is not on the bootclasspath
    p_TLS_vmthread->class_not_found = true;
    Class_Handle clss = class_find_class_from_loader(NULL, buf, FALSE);
    ReleaseStringUTFChars(jenv, name, buf);
    if (clss) {
        // filter out primitive types for compatibility
        return clss->is_primitive() ? NULL : jni_class_from_handle(jenv, clss);
    } else {
        assert(exn_raised());
        if(p_TLS_vmthread->class_not_found) 
        {
            // the requested class is not on the bootclasspath
            // delegation model requires letting child loader(s) to continue 
            // with searching on their paths, so reset the exception
            exn_clear();
        }
        return NULL;
    }
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    findLoadedClass
 * Signature: (Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/Class;
 */
JNIEXPORT jclass JNICALL Java_java_lang_ClassLoader_findLoadedClass
    (JNIEnv *jenv, jobject cl, jstring name)
{
    if (NULL == name) {
        return NULL;
    } 

    const char* buf = GetStringUTFChars(jenv, name, NULL);
    Class_Loader_Handle loader = class_loader_lookup(cl);
    Class_Handle clss = class_find_loaded(loader, buf);
    ReleaseStringUTFChars(jenv, name, buf);

    return clss ? jni_class_from_handle(jenv, clss) : NULL; 
}

JNIEXPORT void JNICALL
Java_java_lang_ClassLoader_registerInitiatedClass(JNIEnv* env, jobject loader, jclass clazz) {
    ClassLoader* cl = class_loader_lookup(loader);
    Class* clss = jclass_to_struct_Class(clazz);
    cl->InsertInitiatedClass(clss);
}


/*
 * Class:     java_lang_VMClassRegistry
 * Method:    getClass
 * Signature: (Ljava/lang/Object;)Ljava/lang/Class;
 */
JNIEXPORT jclass JNICALL Java_java_lang_VMClassRegistry_getClassNative
  (JNIEnv *jenv, jclass, jobject jobj)
{
    // use JNI API function
    return GetObjectClass(jenv, jobj);
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    getClassLoader
 * Signature: (Ljava/lang/Class;)Ljava/lang/ClassLoader;
 */
JNIEXPORT jobject JNICALL Java_java_lang_VMClassRegistry_getClassLoader0
  (JNIEnv *jenv, jclass, jclass clazz)
{
    Class_Handle clss = jni_get_class_handle(jenv, clazz);
    Class_Loader_Handle clh = class_get_class_loader(clss);
    return jni_class_loader_from_handle(jenv, clh);
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    getComponentType
 * Signature: (Ljava/lang/Class;)Ljava/lang/Class;
 */
JNIEXPORT jclass JNICALL Java_java_lang_VMClassRegistry_getComponentType
  (JNIEnv *jenv, jclass, jclass clazz)
{
    Class_Handle ch = jni_get_class_handle(jenv, clazz);
    Class* pCompClass = ch->get_array_element_class();
    return jni_class_from_handle(jenv, pCompClass);
}

// return true if iklass is a declared member of klass
inline static bool
class_is_direct_member( Class_Handle klass, Class_Handle iklass )
{
    // A class must have an EnclosingMethod attribute 
    // if and only if it is a local class or an anonymous class.
    if (0 == iklass->get_enclosing_class_index() 
        && klass == class_get_declaring_class(iklass)) {
        return true;
    }

    return false;
}


/*
 * Class:     java_lang_VMClassRegistry
 * Method:    getDeclaredClasses
 * Signature: (Ljava/lang/Class;)[Ljava/lang/Class;
 */
JNIEXPORT jobjectArray JNICALL Java_java_lang_VMClassRegistry_getDeclaredClasses
  (JNIEnv *jenv, jclass, jclass clazz)
{
    unsigned index, num_ic, num_res;
    Class_Handle clss, iclss;

    // get class and number of inner classes
    clss = jni_get_class_handle(jenv, clazz);
    num_ic = num_res = class_number_inner_classes(clss);

    // calculate number of declared classes
    for(index=0; index < num_ic; index++) {
        iclss = class_get_inner_class(clss, index);
        if (!iclss)
            return NULL;        
        if( !class_is_direct_member( clss, iclss ) )
            num_res--;
    }

    // create array
    jclass cclazz = struct_Class_to_java_lang_Class_Handle(VM_Global_State::loader_env->JavaLangClass_Class);
    jobjectArray res = NewObjectArray(jenv, num_res, cclazz, NULL);

    // set array
    for( index = 0, num_res = 0; index < num_ic; index++) {
        iclss = class_get_inner_class(clss, index);
        if( !class_is_direct_member( clss, iclss ) )
            continue;
        SetObjectArrayElement(jenv, res, num_res++, jni_class_from_handle(jenv, iclss));
    }
    return res;
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    getDeclaredConstructors
 * Signature: (Ljava/lang/Class;)[Ljava/lang/reflect/Constructor;
 */
JNIEXPORT jobjectArray JNICALL Java_java_lang_VMClassRegistry_getDeclaredConstructors
  (JNIEnv *jenv, jclass, jclass clazz)
{
    return reflection_get_class_constructors(jenv, clazz);
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    getDeclaredFields
 * Signature: (Ljava/lang/Class;)[Ljava/lang/reflect/Field;
 */
JNIEXPORT jobjectArray JNICALL Java_java_lang_VMClassRegistry_getDeclaredFields
  (JNIEnv *jenv, jclass, jclass clazz)
{
    return reflection_get_class_fields(jenv, clazz);
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    getDeclaredMethods
 * Signature: (Ljava/lang/Class;)[Ljava/lang/reflect/Method;
 */
JNIEXPORT jobjectArray JNICALL Java_java_lang_VMClassRegistry_getDeclaredMethods
  (JNIEnv *jenv, jclass, jclass clazz)
{
    return reflection_get_class_methods(jenv, clazz);
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    getDeclaringClass
 * Signature: (Ljava/lang/Class;)Ljava/lang/Class;
 */
JNIEXPORT jclass JNICALL Java_java_lang_VMClassRegistry_getDeclaringClass
  (JNIEnv *jenv, jclass, jclass clazz)
{
    Class_Handle clss = jni_get_class_handle(jenv, clazz);
    Class_Handle res = class_get_declaring_class(clss);
    return jni_class_from_handle(jenv, res);
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    getInterfaces
 * Signature: (Ljava/lang/Class;)[Ljava/lang/Class;
 */
JNIEXPORT jobjectArray JNICALL Java_java_lang_VMClassRegistry_getInterfaces
  (JNIEnv *jenv, jclass, jclass clazz)
{
    return reflection_get_class_interfaces(jenv, clazz);
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    getModifiers
 * Signature: (Ljava/lang/Class;)I
 */
JNIEXPORT jint JNICALL Java_java_lang_VMClassRegistry_getModifiers
  (JNIEnv *jenv, jclass, jclass clazz)
{
    Class_Handle clss = jni_get_class_handle(jenv, clazz);
    return clss->get_access_flags() & (~ACC_SUPER);
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    getName
 * Signature: (Ljava/lang/Class;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_java_lang_VMClassRegistry_getName
  (JNIEnv *jenv, jclass, jclass clazz)
{
    ASSERT_RAISE_AREA;
    Class* clss = jclass_to_struct_Class(clazz);
    String* str = clss->get_java_name();
    return String_to_interned_jstring(str);
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    getSuperclass
 * Signature: (Ljava/lang/Class;)Ljava/lang/Class;
 */
JNIEXPORT jclass JNICALL Java_java_lang_VMClassRegistry_getSuperclass
  (JNIEnv *jenv, jclass, jclass clazz)
{
    Class *clss = jni_get_class_handle(jenv, clazz);

    if (clss->is_interface() || clss->is_primitive() || !clss->has_super_class()) {
        // Interfaces and primitive classes have no superclasses.
        return (jclass)0;
    }

    return jni_class_from_handle(jenv, clss->get_super_class());
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    getSystemPackages
 * Signature: (I)[[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_java_lang_VMClassRegistry_getSystemPackages
  (JNIEnv *jenv, jclass, jint len)
{
    Global_Env* genv = VM_Global_State::loader_env;
    ClassLoader* cl = static_cast<ClassLoader*>
        (genv->bootstrap_class_loader);
    Package_Table* ptab = cl->getPackageTable();
    cl->Lock();
    unsigned p_num = (unsigned)ptab->size();
    if (p_num == (unsigned)len) 
    {
        cl->Unlock();
        return NULL;
    }
    const char ** pkgs = (const char **)STD_MALLOC(p_num * 2 * sizeof(char*));
    size_t buf_len = 0;
    unsigned index = 0;
    for (Package_Table::const_iterator it = ptab->begin(), end = ptab->end(); 
        it != end; ++it)
    {
        const char* name = pkgs[index++] = (*it).first->bytes;
        pkgs[index++] = (*it).second->get_jar();
        size_t name_len = (*it).first->len;
        if (name_len > buf_len) {
            buf_len = name_len;
        }
    }
    cl->Unlock();

    jclass string_class = struct_Class_to_java_lang_Class_Handle(genv->JavaLangString_Class);
    static Class* aos = genv->LoadCoreClass("[Ljava/lang/String;");
    jclass string_array_class = struct_Class_to_java_lang_Class_Handle(aos);
    assert(string_class);
    assert(string_array_class);
        
    jobjectArray result = NewObjectArray(jenv, p_num, string_array_class, NULL);
    if (result) 
    {
        char* buf = (char*)STD_MALLOC(buf_len + 1);
        p_num *= 2;
        for (index = 0; index < p_num; )
        {
            jobjectArray pair = NewObjectArray(jenv, 2, string_class, NULL);
            if (!pair) {
                break;
            }
            SetObjectArrayElement(jenv, result, index/2, pair);

            char* name = strcpy(buf, pkgs[index++]);
            for (char* c = name; *c != '\0'; ++c) {
                if (*c == '/') {
                    *c = '.';
                }
            }
            jstring jname = NewStringUTF(jenv, name);
            if (!jname) {
                break;
            }
            SetObjectArrayElement(jenv, pair, 0, jname);

            const char * jar = pkgs[index++];
            if (jar) {
                jstring js = NewStringUTF(jenv, jar);
                if (!js) break;
                SetObjectArrayElement(jenv, pair, 1, js);
            }
        }
        STD_FREE(buf);
    }

    STD_FREE(pkgs);
    
    assert(result || exn_raised());
    return result;
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    initializeClass
 * Signature: (Ljava/lang/Class;)V
 */
JNIEXPORT void JNICALL Java_java_lang_VMClassRegistry_initializeClass
  (JNIEnv *jenv, jclass unused, jclass clazz)
{
    ASSERT_RAISE_AREA;
    assert(clazz != NULL);
    Class *clss = jni_get_class_handle(jenv, clazz);
    Java_java_lang_VMClassRegistry_linkClass(jenv, unused, clazz);
    if(jenv->ExceptionCheck())
        return;
    class_initialize_from_jni(clss);
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    isArray
 * Signature: (Ljava/lang/Class;)Z
 */
JNIEXPORT jboolean JNICALL Java_java_lang_VMClassRegistry_isArray
  (JNIEnv *jenv, jclass, jclass clazz)
{
    Class_Handle ch = jni_get_class_handle(jenv, clazz);
    return (jboolean)(ch->is_array() ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    isAssignableFrom
 * Signature: (Ljava/lang/Class;Ljava/lang/Class;)Z
 */
JNIEXPORT jboolean JNICALL Java_java_lang_VMClassRegistry_isAssignableFrom
  (JNIEnv *jenv, jclass, jclass clazz, jclass fromClazz)
{
    // check parameters
    if (!clazz)
    {
        throw_exception_from_jni(jenv, "java/lang/NullPointerException", "clazz argument");
        return JNI_FALSE;
    }

    if (!fromClazz)
    {
        throw_exception_from_jni(jenv, "java/lang/NullPointerException", "fromClazz argument");
        return JNI_FALSE;
    }

    Class_Handle ch = jni_get_class_handle(jenv, fromClazz);

    // if primitive class
    if (ch->is_primitive())
        return (jboolean)(IsSameObject(jenv, clazz, fromClazz) ? JNI_TRUE : JNI_FALSE);

    // if non primitive
    return IsAssignableFrom(jenv, fromClazz, clazz);
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    isInstance
 * Signature: (Ljava/lang/Class;Ljava/lang/Object;)Z
 */
JNIEXPORT jboolean JNICALL Java_java_lang_VMClassRegistry_isInstance
  (JNIEnv *jenv, jclass, jclass clazz, jobject obj)
{
    // null object
    if (!obj) return JNI_FALSE;

    return IsInstanceOf(jenv, obj, clazz);
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    isPrimitive
 * Signature: (Ljava/lang/Class;)Z
 */
JNIEXPORT jboolean JNICALL Java_java_lang_VMClassRegistry_isPrimitive
  (JNIEnv *jenv, jclass, jclass clazz)
{
    Class_Handle ch = jni_get_class_handle(jenv, clazz);
    return (jboolean)(ch->is_primitive() ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    linkClass
 * Signature: (Ljava/lang/Class;)V
 */
JNIEXPORT void JNICALL Java_java_lang_VMClassRegistry_linkClass
  (JNIEnv *jenv, jclass, jclass clazz)
{
    // ppervov: this method intentionally left blank
    //      as in our VM classes will never get to Java
    //      unlinked (except resolution stage)
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    loadArray
 * Signature: (Ljava/lang/Class;I)Ljava/lang/Class;
 */
JNIEXPORT jclass JNICALL Java_java_lang_VMClassRegistry_loadArray
  (JNIEnv *jenv, jclass, jclass compType, jint dims)
{
    Class *clss = jni_get_class_handle(jenv, compType);
    Class *arr_clss = clss;

    for (int i = 0; i < dims; i++) {
        arr_clss = (Class *)class_get_array_of_class(arr_clss);
        if (!arr_clss)   return 0;
    }

    return jni_class_from_handle(jenv, arr_clss);
}

/*
 * Class:     java_lang_VMClassRegistry
 * Method:    loadLibrary
 * Signature: (Ljava/lang/String;Ljava/lang/ClassLoader;)V
 */
JNIEXPORT void JNICALL Java_java_lang_VMClassRegistry_loadLibrary
  (JNIEnv *jenv, jclass, jstring filename, jobject classLoader)
{
    // check filename
    if (! filename)
    {
        jclass exc_class = FindClass(jenv, VM_Global_State::loader_env->JavaLangNullPointerException_String);
        ThrowNew(jenv, exc_class, "null file name value.");
        return;
    }

    // get filename char string
    const char *str_filename = GetStringUTFChars(jenv, filename, NULL);

    // load native library
    Class_Loader_Handle loader;
    if( classLoader ) {
        loader = class_loader_lookup(classLoader);
    } else {
        // bootstrap class loader
        loader = (Class_Loader_Handle)
            jni_get_vm_env(jenv)->bootstrap_class_loader;
    }
    class_loader_load_native_lib(str_filename, loader);

    // release char string
    ReleaseStringUTFChars(jenv, filename, str_filename);
}

/*
* Class:     java_lang_VMClassRegistry
* Method:    getEnclosingClass
* Signature: (Ljava/lang/Class;)Ljava/lang/Class;
*/
JNIEXPORT jclass JNICALL Java_java_lang_VMClassRegistry_getEnclosingClass
(JNIEnv *, jclass, jclass jclazz)
{
    assert(jclazz);
    Class* clazz = jclass_to_struct_Class(jclazz);
    unsigned idx = clazz->get_enclosing_class_index();
    if (!idx) {
        idx = clazz->get_declaring_class_index();
    }
    if (idx) {
        Class* outer_clss = clazz->_resolve_class(VM_Global_State::loader_env, idx);
        if (outer_clss) {
            return struct_Class_to_jclass(outer_clss);
        }
        if (!exn_raised()) {
            exn_raise_object(clazz->get_constant_pool().get_error_cause(idx));
        }
    } 
    return NULL;
}

/*
* Class:     java_lang_VMClassRegistry
* Method:    getEnclosingMember
* Signature: (Ljava/lang/Class;)Ljava/lang/reflect/Member;
*/
JNIEXPORT jobject JNICALL Java_java_lang_VMClassRegistry_getEnclosingMember
(JNIEnv *jenv, jclass, jclass jclazz)
{
    assert(jclazz);
    Class* clazz = jclass_to_struct_Class(jclazz);
    unsigned method_idx = clazz->get_enclosing_method_index();
    if (method_idx) {
        unsigned c_idx = clazz->get_enclosing_class_index();
        ASSERT(c_idx, ("No class for enclosing method"));
        Class* outer_clss = clazz->_resolve_class(VM_Global_State::loader_env, c_idx);
        if (outer_clss) 
        {
            String* name = clazz->get_constant_pool().get_name_and_type_name(method_idx);
            String* desc = clazz->get_constant_pool().get_name_and_type_descriptor(method_idx);

            TRACE("Looking for enclosing method: class="<<outer_clss->get_name()->bytes 
                <<"; name="<<name->bytes<<"; desc="<<desc->bytes);

            Method* enclosing = outer_clss->lookup_method(name, desc);
            if (enclosing) 
            {
                if (enclosing->is_init()) 
                {
                    return reflection_reflect_constructor(jenv, enclosing);
                } 
                else if (!enclosing->is_clinit()) 
                {
                    return reflection_reflect_method(jenv, enclosing);
                }
            } else {
                //FIXME: check RI compatibility, provide detailed message
                ThrowNew_Quick(jenv, "java/lang/NoSuchMethodException", 
                    "Invalid enclosing method declared");
            }
        } else if (!exn_raised()) {
            exn_raise_object(clazz->get_constant_pool().get_error_cause(c_idx));
        }
    } 
    return NULL;
}

/*
* Class:     java_lang_VMClassRegistry
* Method:    getSimpleName
* Signature: (Ljava/lang/Class;)Ljava/lang/String;
*/
JNIEXPORT jstring JNICALL Java_java_lang_VMClassRegistry_getSimpleName
(JNIEnv *, jclass, jclass jclazz)
{
    ASSERT_RAISE_AREA;
    assert(jclazz);
    Class* clazz = jclass_to_struct_Class(jclazz);
    String* str = clazz->get_simple_name();
    return str ? String_to_interned_jstring(str) : NULL;
}
