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
 * @file org_apache_harmony_vm_VMStack.cpp
 *
 * This file is a part of kernel class natives VM core component.
 * It contains implementation for native methods of 
 * org.apache.harmony.drl.vm.VMStack class.
 */

#define LOG_DOMAIN "kernel.stack"
#include "cxxlog.h"

#include <vector>

#include "org_apache_harmony_vm_VMStack.h"
#include "open/vm_class_manipulation.h"

#include "stack_trace.h"
#include "jthread.h"
#include "jni_direct.h"
#include "jni_utils.h"
#include "environment.h"
#include "exceptions.h"
#include "vm_strings.h"
#include "vm_arrays.h"
#include "thread_generic.h"

#include "java_lang_VMClassRegistry.h"
#include "java_security_AccessController.h"

/*
 * Class:     org_apache_harmony_vm_VMStack
 * Method:    getCallerClass
 * Signature: (I)Ljava/lang/Class;
 */
JNIEXPORT jclass JNICALL Java_org_apache_harmony_vm_VMStack_getCallerClass
  (JNIEnv * UNREF jenv, jclass, jint depth)
{
    // If depth is equal to 0 then caller of caller of this method should be
    // returned. Thus increment depth by 2.
    depth += 2;

    // obtain requested frame
    StackTraceFrame frame;
    bool res = st_get_frame(depth, &frame);

    // if no frame on specified depth
    if (!res) return NULL;

    // obtain and return class of the frame
    assert(hythread_is_suspend_enabled());
    return struct_Class_to_jclass(frame.method->get_class());
}

inline static bool isReflectionFrame(Method* method, Global_Env* genv) {
    // for simplicity, any method of 
    // java/lang/reflect/VMReflection or java/lang/reflect/Method 
    static Class *VMReflection = genv->LoadCoreClass("java/lang/reflect/VMReflection");
    
    Class* mc = method->get_class();
    return (mc == VMReflection || mc == genv->java_lang_reflect_Method_Class);
}

inline static bool isPrivilegedFrame(Method_Handle method, Global_Env* genv) {
    static Method_Handle doPrivileged[4];

    if (!doPrivileged[0]) {
        Class* ac = genv->LoadCoreClass("java/security/AccessController");
        doPrivileged[3] = (Method_Handle)class_lookup_method(ac, "doPrivileged", 
            "(Ljava/security/PrivilegedAction;)Ljava/lang/Object;");
        doPrivileged[2] = (Method_Handle)class_lookup_method(ac, "doPrivileged", 
            "(Ljava/security/PrivilegedExceptionAction;)Ljava/lang/Object;");
        doPrivileged[1] = (Method_Handle)class_lookup_method(ac, "doPrivileged", 
            "(Ljava/security/PrivilegedAction;Ljava/security/AccessControlContext;)Ljava/lang/Object;");
        doPrivileged[0] = (Method_Handle)class_lookup_method(ac, "doPrivileged", 
            "(Ljava/security/PrivilegedExceptionAction;Ljava/security/AccessControlContext;)Ljava/lang/Object;");
        unsigned i = 0;
        for (; i < 4; i++) assert(doPrivileged[i]);
    }

    unsigned i = 0;
    for (; i < 4; i++) {
        if (method == doPrivileged[i]) return true;
    }
    return false;
}

/*
 * Class:     org_apache_harmony_vm_VMStack
 * Method:    getClasses
 * Signature: (IZ)[Ljava/lang/Class;
 */
JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_vm_VMStack_getClasses
  (JNIEnv *jenv, jclass, jint signedMaxSize, jboolean considerPrivileged)
{
    
    // if signedMaxSize is negative, maxSize will be > MAX_INT_VALUE
    unsigned maxSize = signedMaxSize;


    assert(hythread_is_suspend_enabled());
    unsigned size;
    StackTraceFrame* frames;
    st_get_trace(get_thread_ptr(), &size, &frames);

    // The caller of the caller of the caller of this method is stored as a first element of the array.
    // For details look at the org/apache/harmony/vm/VMStack.java file. Thus skipping 3 frames.
    unsigned skip = 3;

    Global_Env* genv = jni_get_vm_env(jenv);

    // count target array length ignoring reflection frames
    unsigned length = 0, s;
    for (s = skip; s < size && length < maxSize; s++) {
        Method_Handle method = frames[s].method;

        if (isReflectionFrame(method, genv))
            continue;

        if (considerPrivileged && isPrivilegedFrame(method, genv) 
                && maxSize > length + 2)
            maxSize = length + 2;

        length ++;
    }

    assert(hythread_is_suspend_enabled());

    jclass ste = struct_Class_to_java_lang_Class_Handle(genv->JavaLangClass_Class);
    assert(ste);

    // creating java array
    jarray arr = jenv->NewObjectArray(length, ste, NULL);
    if (arr == NULL) {
        // OutOfMemoryError
        STD_FREE(frames);
        assert(hythread_is_suspend_enabled());
        return NULL;
    }

    // filling java array
    unsigned i = 0;
    for (s = skip; s < size && i < length; s++) {
        Method_Handle method = frames[s].method;

        if (isReflectionFrame(method, genv))
            continue;

        // obtain frame class
        jclass clz = struct_Class_to_java_lang_Class_Handle(method->get_class());

        jenv->SetObjectArrayElement(arr, i, clz);

        i++;
    }

    STD_FREE(frames);
    assert(hythread_is_suspend_enabled());
    return arr;
}

/*
* Method: java.security.AccessController.getStackDomains()[Ljava/security/ProtectionDomain;
*/
JNIEXPORT jobjectArray JNICALL
Java_java_security_AccessController_getStackDomains(JNIEnv *jenv, jclass UNREF)
{
    assert(hythread_is_suspend_enabled());
    unsigned size;
    StackTraceFrame* frames;
    st_get_trace(get_thread_ptr(), &size, &frames);

    std::vector<jobject> domains = std::vector<jobject>();
    Global_Env* genv = jni_get_vm_env(jenv);

    unsigned domain_field_offset = genv->Class_domain_field_offset;

    if (domain_field_offset == 0) {
        // initialize preloaded values
        genv->java_security_ProtectionDomain_Class = genv->LoadCoreClass("java/security/ProtectionDomain");
        String* name = genv->string_pool.lookup("domain");
        String* desc = genv->string_pool.lookup("Ljava/security/ProtectionDomain;");
        Field* f = genv->JavaLangClass_Class->lookup_field(name, desc);
        assert(f);
        domain_field_offset = genv->Class_domain_field_offset = f->get_offset();
    }

    // The caller of the caller of this method is stored as a first element of the array.
    // For details look at the org/apache/harmony/vm/VMStack.java file. Thus skipping 2 frames.
    unsigned s = 2;
    for (; s < size; s++) {
        Method_Handle method = frames[s].method;

        if (isReflectionFrame(method, genv))
            continue;

        if (isPrivilegedFrame(method, genv)) {
            // find nearest non-reflection frame and finish looping
            while (++s < size && isReflectionFrame(frames[s].method, genv));
            TRACE("Privileged frame at " << s << " of " << size);
            size = s;
            method = frames[s].method;
        }

        jobject pd = GetObjectFieldOffset(jenv, 
            struct_Class_to_java_lang_Class_Handle(method->get_class()), domain_field_offset);
        if (pd) {
            domains.push_back(pd);
        }
    }

    jclass pdc = struct_Class_to_java_lang_Class_Handle(genv->java_security_ProtectionDomain_Class);
    assert(pdc);
    size = (unsigned)domains.size();
    TRACE("Domains on stack: " << size);
    // create & fill  java array
    jarray arr = jenv->NewObjectArray(size, pdc, NULL);
    if (arr != NULL) {
        for (s = 0; s < size; s++) {
            jenv->SetObjectArrayElement(arr, s, domains[s]);
        }
    } else {
        // OutOfMemoryError
        assert(exn_raised());
    }
    STD_FREE(frames);
    return arr;
}

/*
 * Class:     org_apache_harmony_vm_VMStack
 * Method:    getStackState
 * Signature: ()Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_org_apache_harmony_vm_VMStack_getStackState
  (JNIEnv *jenv, jclass)
{
    assert(hythread_is_suspend_enabled());
    unsigned size;
    StackTraceFrame* frames;
    st_get_trace(get_thread_ptr(), &size, &frames);

    if (frames == NULL) {
        exn_raise_object(VM_Global_State::loader_env->java_lang_OutOfMemoryError);
        return 0;
    }
    assert(frames);
    unsigned data_size = size * sizeof(StackTraceFrame);

    // pack trace into long[] array
    unsigned const elem_size = 8; // 8 bytes in each long element 
    unsigned array_length = (data_size + elem_size - 1) / elem_size; // array dimension

    // create long[] array
    jlongArray array = jenv->NewLongArray(array_length);
    if (!array) {
        return 0;
    }

    // copy data to array
    jlong* array_data = jenv->GetLongArrayElements(array, NULL);
    memcpy(array_data, frames, data_size);
    jenv->ReleaseLongArrayElements(array, array_data, 0);

    STD_FREE(frames);

    return array;
} // Java_org_apache_harmony_vm_VMStack_getStackState

/*
 * Class:     org_apache_harmony_vm_VMStack
 * Method:    getStackClasses
 * Signature: (Ljava/lang/Object;)[Ljava/lang/Class;
 */
JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_vm_VMStack_getStackClasses
  (JNIEnv *jenv, jclass, jobject state)
{
    ASSERT_RAISE_AREA;
    if (NULL == state)
        return NULL;

    Global_Env* genv = jni_get_vm_env(jenv);

    // get class array class
    static Class *arr_class = (Class *)class_get_array_of_class(genv->JavaLangClass_Class);
    assert(arr_class);

    // state object contains raw data as long array
    jlongArray array = (jlongArray)state;
    assert(array);

    // get depth of the stack
    unsigned size = jenv->GetArrayLength(array) * 8 / sizeof(StackTraceFrame);

    tmn_suspend_disable();       //---------------------------------v

    // create java array
    Vector_Handle arr = vm_new_vector(arr_class, size);
    if (arr == NULL) {
        tmn_suspend_enable();
        return NULL;
    }

    // copy data to array
    Vector_Handle ja = (Vector_Handle)array->object;
    StackTraceFrame* frames = (StackTraceFrame*)get_vector_element_address_int64(ja, 0);

    // find and store classes of the methods on the stack
    for (unsigned i=0; i < size; i++) {
        Method* m = frames[i].method;
        Class* c = m->get_class();

        ManagedObject *elem= struct_Class_to_java_lang_Class(c);
        STORE_REFERENCE((ManagedObject *)arr, get_vector_element_address_ref(arr, i), elem);
    }

    ObjectHandle java_array = oh_allocate_local_handle();
    java_array->object = (ManagedObject*)arr;

    tmn_suspend_enable();        //---------------------------------^

    return (jobjectArray)java_array;

}

/*
 * Class:     org_apache_harmony_vm_VMStack
 * Method:    getStackTrace
 * Signature: (Ljava/lang/Object;)[Ljava/lang/StackTraceElement;
 */
JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_vm_VMStack_getStackTrace
  (JNIEnv * jenv, jclass, jobject state)
{

    ASSERT_RAISE_AREA;
    if (NULL == state)
        return NULL;

    Global_Env* genv = jni_get_vm_env(jenv);

     // state object contains raw data as long array
    jlongArray array = (jlongArray)state;
    assert(array);

    // copy data to array
    jlong* array_data = jenv->GetLongArrayElements(array, NULL);

    StackTraceFrame* frames = (StackTraceFrame*) array_data;
    unsigned size = jenv->GetArrayLength(array) * 8 / sizeof(StackTraceFrame);

    static Method_Handle fillInStackTrace = NULL;
    if (!fillInStackTrace) {
        fillInStackTrace =
            class_lookup_method(genv->java_lang_Throwable_Class,
                "fillInStackTrace", "()Ljava/lang/Throwable;");
        assert(fillInStackTrace);
    }

    static Method_Handle threadRunImpl = NULL;
    if (!threadRunImpl) {
        threadRunImpl =
            class_lookup_method(genv->java_lang_Thread_Class,
                "runImpl", "()V");
        assert(threadRunImpl);
    }

    unsigned skip;

    // skip frames up to one with fillInStackTrace method and remember this
    // pointer
    for (skip = 0; skip < size; skip++) {
        Method_Handle method = frames[skip].method;
        assert(method);

        if (method == fillInStackTrace) {
            skip++;
            break;
        }
    }

    if (frames[size -1].method == threadRunImpl) size--;

    if (skip < size) {
        Method *method = frames[skip].method;
        // skip Throwable constructor
        if (method->is_init() && method->get_class() ==
                genv->java_lang_Throwable_Class) {
            void *old_this = frames[skip].outdated_this;
            skip++;

            // skip all exception constructors for this exception
            for (;skip < size; skip++) {
                Method *method = frames[skip].method;
                assert(method);

                if (!method->is_init()
                        || old_this != frames[skip].outdated_this) {
                    break;
                }
            }
        }
    }

    ASSERT(size >= skip, ("Trying to skip %u frames but there are only %u frames in stack",
        skip, size));
    
    assert(hythread_is_suspend_enabled());
    jclass ste = struct_Class_to_java_lang_Class_Handle(genv->java_lang_StackTraceElement_Class);
    assert(ste);

    static jmethodID init = (jmethodID) genv->java_lang_StackTraceElement_Class->lookup_method(
        genv->Init_String,
        genv->string_pool.lookup("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V"));
   
    jarray arr = jenv->NewObjectArray(size - skip, ste, NULL);
    if (!arr) {
        assert(exn_raised());
        return NULL;
    }

    tmn_suspend_disable();
    ObjectHandle strMethodName = oh_allocate_local_handle();
    ObjectHandle strClassName = oh_allocate_local_handle();
    tmn_suspend_enable();

    for(unsigned i = skip; i < size; i++) {
        Method_Handle method = frames[i].method;
        NativeCodePtr ip = frames[i].ip;
        int inl_depth = frames[i].depth;
        int lineNumber;
        const char* fileName;

        get_file_and_line(method, ip, true, inl_depth, &fileName, &lineNumber);
        jstring strFileName;
        if (fileName != NULL) {
            strFileName = jenv->NewStringUTF(fileName);
            if (!strFileName) {
                assert(exn_raised());
                return NULL;
            }
        } else {
            strFileName = NULL;
        }

     
        tmn_suspend_disable();
        // class name
        String* className = method->get_class()->get_java_name();
        strClassName->object = vm_instantiate_cp_string_resolved(className);
        if (!strClassName->object) {
            tmn_suspend_enable();
            assert(exn_raised());
            return NULL;
        }
        // method name
        strMethodName->object = vm_instantiate_cp_string_resolved(method->get_name());
        if (!strMethodName->object) {
            tmn_suspend_enable();
            assert(exn_raised());
            return NULL;
        }
        tmn_suspend_enable();

        // creating StackTraceElement object
        jobject obj = jenv->NewObject(ste, init, strClassName, strMethodName, 
                strFileName, lineNumber);
        if (!obj) {
            assert(exn_raised());
            return NULL;
        }

        jenv->SetObjectArrayElement(arr, i - skip, obj);
    }

    jenv->ReleaseLongArrayElements(array, array_data, JNI_ABORT);
    return arr;
}

/*
 * Class:     org_apache_harmony_vm_VMStack
 * Method:    getClassLoader
 * Signature: (Ljava/lang/Class;)Ljava/lang/ClassLoader;
 */
JNIEXPORT jobject JNICALL Java_org_apache_harmony_vm_VMStack_getClassLoader
  (JNIEnv *jenv, jclass, jclass clazz)
{
    // reuse similar method in VMClassRegistry
    return Java_java_lang_VMClassRegistry_getClassLoader0(jenv, NULL, clazz);
}

/*
* Class:     org_apache_harmony_vm_VMStack
* Method:    getThreadStackTrace
* Signature: (Ljava/lang/Thread;)[Ljava/lang/StackTraceElement;
*/
JNIEXPORT jobjectArray JNICALL Java_org_apache_harmony_vm_VMStack_getThreadStackTrace
  (JNIEnv *jenv, jclass, jobject thread)
{
    unsigned size = 0;
    StackTraceFrame* frames;

    vm_thread_t p_thread = jthread_get_vm_thread_ptr_safe(thread);
    if (p_thread != NULL) {
        if (p_thread == get_thread_ptr()) {
            st_get_trace(p_thread, &size, &frames);
        } else {
            // to avoid suspension of each other due to race condition
            // get global thread lock as it's done in hythread_suspend_all().
            IDATA UNREF status = hythread_global_lock();
            assert(0 == status);

            jthread_suspend(thread);
            st_get_trace(p_thread, &size, &frames);
            jthread_resume(thread);

            status = hythread_global_unlock();
            assert(0 == status);
        }
    }

    if (0 == size)
        return NULL;

    Global_Env* genv = VM_Global_State::loader_env;

    // skip the VMStart$MainThread if one exits from the bottom of the stack
    // along with 2 reflection frames used to invoke method main
    static String* starter_String = genv->string_pool.lookup("java/lang/Thread");
    Method_Handle method = frames[size-1].method;
    assert(method);
    // skip only for main application thread
    if (!strcmp(method->get_name()->bytes, "runImpl")
        && method->get_class()->get_name() == starter_String) {

        size --;
    }

    assert(hythread_is_suspend_enabled());
    jclass ste = struct_Class_to_java_lang_Class_Handle(genv->java_lang_StackTraceElement_Class);
    assert(ste);

    static jmethodID init = (jmethodID) genv->java_lang_StackTraceElement_Class->lookup_method(
        genv->Init_String,
        genv->string_pool.lookup("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V"));

    jarray arr = jenv->NewObjectArray(size, ste, NULL);
    if (!arr) {
        assert(exn_raised());
        return NULL;
    }

    tmn_suspend_disable();
    ObjectHandle strMethodName = oh_allocate_local_handle();
    ObjectHandle strClassName = oh_allocate_local_handle();
    tmn_suspend_enable();

    for(unsigned i = 0; i < size; i++) {
        Method_Handle method = frames[i].method;
        NativeCodePtr ip = frames[i].ip;
        int inl_depth = frames[i].depth;
        int lineNumber;
        const char* fileName;

        get_file_and_line(method, ip, true, inl_depth, &fileName, &lineNumber);
        if (fileName == NULL) fileName = "";

        jstring strFileName = jenv->NewStringUTF(fileName);
        if (!strFileName) {
            assert(exn_raised());
            return NULL;
        }

        tmn_suspend_disable();
        // class name
        String* className = method->get_class()->get_java_name();
        strClassName->object = vm_instantiate_cp_string_resolved(className);
        if (!strClassName->object) {
            tmn_suspend_enable();
            assert(exn_raised());
            return NULL;
        }
        // method name
        strMethodName->object = vm_instantiate_cp_string_resolved(method->get_name());
        if (!strMethodName->object) {
            tmn_suspend_enable();
            assert(exn_raised());
            return NULL;
        }
        tmn_suspend_enable();

        // creating StackTraceElement object
        jobject obj = jenv->NewObject(ste, init, strClassName, strMethodName, 
            strFileName, lineNumber);
        if (!obj) {
            assert(exn_raised());
            return NULL;
        }

        jenv->SetObjectArrayElement(arr, i, obj);
    }
    STD_FREE(frames);
    return arr;
}
