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


#define LOG_DOMAIN "jni"
#include "cxxlog.h"

#include "open/vm_method_access.h"
#include "Class.h"
#include "classloader.h"
#include "ini.h"

#include "native_utils.h"
#include "jni_utils.h"
#include "jni_direct.h"
#include "object_handles.h"
#include "vm_arrays.h"
#include "open/vm_util.h"
#include "properties.h"
#include "environment.h"
#include "exceptions.h"
#include "nogc.h"
#include "m2n.h"


#include "stack_trace.h"

Class_Handle jni_get_class_handle(JNIEnv* UNREF jenv, jclass clazz)
{
    return jclass_to_struct_Class(clazz);
}

jclass jni_class_from_handle(JNIEnv* UNREF jenv, Class_Handle clss)
{
    if (!clss) return NULL;
    assert(hythread_is_suspend_enabled());
    return struct_Class_to_jclass((Class*)clss);
}

jobject jni_class_loader_from_handle(JNIEnv*, Class_Loader_Handle clh)
{
    if (!clh) return NULL;
    hythread_suspend_disable();
    ManagedObject* obj = clh->GetLoader();
    if( !obj ) {
        hythread_suspend_enable();
        return NULL;
    }
    ObjectHandle res = oh_allocate_local_handle_from_jni();
    if (res) {
        res->object = obj;
    }
    hythread_suspend_enable();
    return (jobject)res;
}

Class_Loader_Handle class_loader_lookup(jobject loader)
{
    if (!loader) return NULL;

    ObjectHandle h = (ObjectHandle)loader;

    hythread_suspend_disable();       //---------------------------------v
    ClassLoader* cl = ClassLoader::LookupLoader(h->object);
    hythread_suspend_enable();        //---------------------------------^

    return cl;
} //class_loader_lookup

void class_loader_load_native_lib(const char* lib,
                                   Class_Loader_Handle cl)
{
    cl->LoadNativeLibrary(lib);
}


VMEXPORT
jvalue *get_jvalue_arg_array(Method *method, va_list args)
{
    unsigned num_args = method->get_num_args();
    if(!num_args) {
        return NULL;
    }
    jvalue *jvalue_args = (jvalue *)STD_MALLOC(num_args * sizeof(jvalue));

    Arg_List_Iterator iter = method->get_argument_list();
    unsigned arg_number = 0;
    Java_Type typ;
    while((typ = curr_arg(iter)) != JAVA_TYPE_END) {
        switch(typ) {
        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
            jvalue_args[arg_number].l = va_arg(args, jobject);
            break;
        case JAVA_TYPE_INT:
            jvalue_args[arg_number].i = va_arg(args, int);
            break;
        case JAVA_TYPE_BYTE:
            jvalue_args[arg_number].b = (jbyte)va_arg(args, int);
            break;
        case JAVA_TYPE_BOOLEAN:
            jvalue_args[arg_number].z = (jboolean)va_arg(args, int);
            break;
        case JAVA_TYPE_CHAR:
            jvalue_args[arg_number].c = (jchar)va_arg(args, int);
            break;
        case JAVA_TYPE_SHORT:
            jvalue_args[arg_number].s = (jshort)va_arg(args, int);
            break;
        case JAVA_TYPE_LONG:
            jvalue_args[arg_number].j = va_arg(args, jlong);
            break;
        case JAVA_TYPE_FLOAT:
            jvalue_args[arg_number].f = (float)va_arg(args, jdouble);
            break;
        case JAVA_TYPE_DOUBLE:
            jvalue_args[arg_number].d = va_arg(args, jdouble);
            break;
        default:
            LDIE(53, "Unexpected java type");
            break;
        }
        iter = advance_arg_iterator(iter);
        arg_number++;
    }

    return jvalue_args;
} //get_jvalue_arg_array

jclass* GetMethodParameterTypes (JNIEnv* env, const char* sig, int *nparams, ClassLoader *class_loader)
{
  int    _parsed =0;
  char*  param;
  const char* sparray[256 * sizeof(char*)];
  jclass* parray;
  const char*  s_ptr = sig;
  size_t sz = strlen(sig) + 257;
  char*  p_ptr = (char*)STD_MALLOC(sz);
  char* o_ptr = p_ptr;

  assert (sig); assert (nparams);

  *nparams = 0;
  s_ptr++; //skip leading '('

  while (*s_ptr != ')') {
    param = p_ptr;
    while (!_parsed) {
        switch (*s_ptr) {
        case 'L':
            while ((*p_ptr++ = *s_ptr++) != ';');
            *p_ptr++ = '\0';
            _parsed = 1;
            break;
        case '[':
            while (*s_ptr == '[')
                *p_ptr++ = *s_ptr++;
            break;
        case 'B': case 'C': case 'D':
        case 'F': case 'I': case 'J':
        case 'S': case 'V': case 'Z':
            *p_ptr++ = *s_ptr++;
            *p_ptr++ = '\0';
            _parsed = 1;
            break;
        }
    }

    (*nparams)++;
    sparray[*nparams-1] = param;
    _parsed = 0;
  }
  sparray[(*nparams)++] = ++s_ptr;

  parray = (jclass*) STD_MALLOC (*nparams * sizeof(jclass));
  assert (parray);

  // Now convert parameter and return type signatures to class objects
  // and construct the parameterTypes array.
  int ii = 0;
  for (ii =0; ii < *nparams; ii++) {
      parray[ii] = SignatureToClass (env, sparray[ii], class_loader);
      if (exn_raised()) {
          // class loading error occurred
          break;
      }
  }

  assert((size_t)(p_ptr - o_ptr) <= sz);
  STD_FREE(o_ptr);
  
  return parray;
} // GetMethodParameterTypes

char* ParameterTypesToMethodSignature (JNIEnv* env, jobjectArray parameterTypes, const char *name)
{
    if (!parameterTypes) {
        return (char*)0;
    }

    jsize nelem = GetArrayLength (env, parameterTypes);

    size_t len = 3; // Parentness and zero byte
    jsize i;
    for ( i = 0; i < nelem; i++) {
        jclass pclazz = (jclass) GetObjectArrayElement (env, parameterTypes, i);

        if (NULL == pclazz) {
            throw_exception_from_jni (env, "java/lang/NoSuchMethodException", name);
            return (char *)0;
        }

        // Access the internal class handle (Class) for the parameter type:
        Class* pcl = jclass_to_struct_Class(pclazz);

        len += GetClassSignatureLength(pcl);
    }

    char* sig = (char *) STD_MALLOC(len);

    if (NULL == sig) {
        //throw_exception_from_jni (env, "java/lang/OutOfMemoryError", name);
        exn_raise_object(VM_Global_State::loader_env->java_lang_OutOfMemoryError);
        return (char *)0;
    }

    sig[0] = '(';
    sig[1] = '\0';

    char *asig = sig + 1; // Point to zero byte
    for (i =0; i < nelem; i++) {
        jclass pclazz = (jclass) GetObjectArrayElement (env, parameterTypes, i);

        // Access the internal class handle (Class) for the parameter type:
        Class* pcl = jclass_to_struct_Class(pclazz);

        GetClassSignature (pcl, asig);
        asig += strlen(asig); // point to zero byte
    }

    asig[0] = ')';
    asig[1] = '\0';

    assert(strlen(sig) < len);
    return sig;

} // ParameterTypesToMethodSignature

/**
 * This function resembles strlen. It returns number
 * of characters in a signature before terminating zero.
 * If you allocate memory, you should ask for
 * GetClassSignatureLength() + 1 bytes.
 */
//should we care about buffer overflow and return len?
size_t GetClassSignatureLength(Class* cl)
{
    assert(cl);

    const String* name = cl->get_name();

    if (name->bytes[0] == '[') {
        return name->len;
    }
    else if (cl->is_primitive()) {
        return 1;
    }
    else {
        return 2 + name->len; // 2 bytes are for L and ;
    }
} // GetClassSignatureLength

//should we care about buffer overflow and return len?
void GetClassSignature(Class* cl, char* sig)
{
    assert (cl);

    const char* name = cl->get_name()->bytes;

    if (name[0] == '[') {
        sprintf (sig, "%s",name);
    }
    else if (cl->is_primitive()) {
        sig[0] = PrimitiveNameToSignature (name);
        sig[1] = '\0';
    }
    else {
        sprintf (sig, "L%s;", name);
    }
} // GetClassSignature

//should we care about buffer overflow and return len?
void ClassSignatureToName (const char* sig, char *name) 
{
    assert (sig);
    const char* sig_ptr = sig;
 
    if (*sig_ptr++ == 'L') {   // if signature for reference type:
        while (*sig_ptr != ';') {
            *name++ = *sig_ptr++;
        }
        *name = '\0';
    } else {                 
        strcpy(name, sig);
    }
} // ClassSignatureToName

//should we care about buffer overflow and return len?
void PrimitiveSignatureToName (const char sig, char *classname)
{
    switch (sig) {
    case 'Z':
        strcpy(classname, "boolean");
        break;
    case 'B':
        strcpy(classname, "byte");
        break;
    case 'C':
        strcpy(classname, "char");
        break;
    case 'S':
        strcpy(classname, "short");
        break;
    case 'I':   
        strcpy(classname, "int");
        break;
    case 'J':
        strcpy(classname, "long");
        break;
    case 'F':
        strcpy(classname, "float");
        break;
    case 'D':
        strcpy(classname, "double");
        break;
    case 'V':
        strcpy(classname, "void");
        break;
    default:
        DIE(("Invalid type descriptor"));
        break;
    }
} // PrimitiveSignatureToName


//
// Given its name, lookup for a field in the class hierarchy:
//
Field* LookupField (Class *clss, const char *name)
{
    Field *f = 0;
    for(; clss && !f; clss = clss->get_super_class()) {
        if((f = LookupDeclaredField(clss, name)))
            return f;
    }

    return NULL;
} // LookupField

Method* LookupMethod(Class* clss, const char* mname, const char* mdesc)
{
    Method* m = 0;
    Class* oclss = clss;
    for (; clss; clss = clss->get_super_class()) {      // for each superclass
        if((m = LookupDeclaredMethod(clss, mname, mdesc)))
            return m;
    }

    for(int i = 0; i < oclss->get_number_of_superinterfaces(); i++)
        if((m = LookupMethod(oclss->get_superinterface(i), mname, mdesc)))
            return m;

    return NULL;                                        // method not found
} // LookupMethod

char PrimitiveNameToSignature (const char* name)
{
    char sig = '\0';

    switch (name[0]) {
    case 'b':
        if (name[1] == 'o') sig = 'Z';
        else sig = 'B';
        break;
    case 'c': sig = 'C'; break;
    case 'd': sig = 'D'; break;
    case 'f': sig = 'F'; break;
    case 's': sig = 'S'; break;
    case 'i': sig = 'I'; break;
    case 'l': sig = 'J'; break;
    case 'v': sig = 'V'; break;
    default: DIE(("Invalid type name"));
    }
    return sig;
} // PrimitiveNameToSignature

//should we care about buffer overflow and return len?
void SignatureToName (const char* sig, char *name)
{
    assert (sig);

    if (sig[0] == 'L') {                      // declared type
        ClassSignatureToName (sig, name);
    } else if (sig[0] == '[') {                // array type
        strcpy(name, sig);
    } else {                                   // primitive type
        PrimitiveSignatureToName (sig[0], name);
    }
} // SignatureToName

jclass SignatureToClass (JNIEnv* env, const char* sig)
{
    assert (sig);
    char classname[512];
    SignatureToName(sig, classname);
    jclass clazz = FindClass (env, classname);
    return clazz;
} // SignatureToClass

//We'd better use this routine
jclass SignatureToClass (JNIEnv* env_ext, const char* sig, ClassLoader *class_loader)
{
    assert(hythread_is_suspend_enabled());
    assert (sig);

    if (sig[0] == 'L' || sig[0] == '[') {
        char classname[512];
        SignatureToName(sig, classname);
        jclass clazz = FindClassWithClassLoader(env_ext, classname, class_loader);
        return clazz;
    }

    Global_Env *ge = jni_get_vm_env(env_ext);
    Class* clss = NULL;

    switch (sig[0]) {
        case 'Z':
            clss = ge->Boolean_Class;
            break;
        case 'B':
            clss = ge->Byte_Class;
            break;
        case 'C':
            clss = ge->Char_Class;
            break;
        case 'D':
            clss = ge->Double_Class;
            break;
        case 'F':
            clss = ge->Float_Class;
            break;
        case 'I':
            clss = ge->Int_Class;
            break;
        case 'J':
            clss = ge->Long_Class;
            break;
        case 'S':
            clss = ge->Short_Class;
            break;
        case 'V':
            clss = ge->Void_Class;
            break;
        default:
            DIE(("Invalid type descriptor"));
            break;
    }

    return jni_class_from_handle(env_ext, clss);
} // SignatureToClass


//////////////////////// add stuff VVVVVVVVVVVv

char* JavaStringToCharArray (JNIEnv * UNREF env, jstring jstr, jint* UNREF len)
{
    unsigned count = env->GetStringLength(jstr);
    jboolean isCopy;
    const jchar* chars = env->GetStringChars(jstr, &isCopy);
    size_t sz = count+1;
    char* cstr = (char*)STD_MALLOC(sz);
    assert(cstr);
    for(unsigned i=0; i<count; i++)
        cstr[i] = (char)((char)chars[i] & 0xff);
    cstr[count] = '\0';
    if (isCopy) env->ReleaseStringChars(jstr, chars);
    assert(strlen(cstr) < sz);
    return cstr;
} // JavaStringToCharArray

//
// Given its name, lookup for a field in the given class
//
Field* LookupDeclaredField (Class *clss, const char *name)
{
    String *field_name = VM_Global_State::loader_env->string_pool.lookup(name);

    assert (clss);

    for (unsigned i =0; i < clss->get_number_of_fields(); i++) {
        if (clss->get_field(i)->get_name() == field_name) {
            return clss->get_field(i);
        }
    }

    return NULL;
} // LookupDeclaredField


void VerifyArray (JNIEnv* env, jarray array)
{
    if (!array) {
        ThrowNew_Quick (env, "java/lang/NullPointerException", 0);
        return;
    }

    jclass aclazz = GetObjectClass (env, array);

    // Acquire handle to internal class handle (Class):
    Class* clss = jclass_to_struct_Class(aclazz);

    if (!clss->is_array()) {
        ThrowNew_Quick (env, "java/lang/IllegalArgumentException", 0);
        return;
    }
} // VerifyArray


char GetComponentSignature (JNIEnv *env, jarray array)
{
    jclass aclazz = GetObjectClass (env, array);

    // Acquire handle to internal class handle (Class):
    Class* clss = jclass_to_struct_Class(aclazz);
    return clss->get_name()->bytes[1]; // return component first character
}


Method* LookupDeclaredMethod(Class *clss, const char *mname, const char *mdesc)
{
    String *method_name = VM_Global_State::loader_env->string_pool.lookup(mname);
   
    size_t len = strlen (mdesc);
    Method *m = 0;
    for (unsigned i =0; i < clss->get_number_of_methods(); i++) {   // for each method
        m = clss->get_method(i);
        if (m->is_fake_method()) {
            continue;   // ignore fake methods
        }
        const char* desc = m->get_descriptor()->bytes;
        if ((m->get_name() == method_name)          // if names and signatures
            && (strncmp (mdesc, desc, len) == 0))   // (excluding return types) match
        {
            return m;                               // return method
        }
    }

    return NULL;                                    // method not found
} // LookupDeclaredMethod

//Checks if the object is null.
jboolean IsNullRef(jobject jobj)
{
    if (!jobj) return JNI_FALSE;

    ObjectHandle h = (ObjectHandle)jobj;

    hythread_suspend_disable();       //---------------------------------v

    jboolean ret = (h->object == NULL) ? true : false;

    hythread_suspend_enable();        //---------------------------------^

    return ret;
}

jclass FindClassWithClassLoader(JNIEnv* jenv, const char *name, ClassLoader *loader)
{
    String *str = VM_Global_State::loader_env->string_pool.lookup(name);
    return FindClassWithClassLoader(jenv, str, loader);
}

jclass FindClassWithClassLoader(JNIEnv* jenv, String *name, ClassLoader *loader)
{
    assert(hythread_is_suspend_enabled());

    Class* c = loader->LoadVerifyAndPrepareClass( VM_Global_State::loader_env, name);
    // if loading failed - exception should be raised
    if(!c) {
        assert(exn_raised());
        return NULL;
    }

    jclass clazz = struct_Class_to_jclass(c);

    return (jclass)clazz;
}

/* 
 * Utility function for throwing exceptions from JNI
 */

void throw_exception_from_jni(JNIEnv *jenv, const char *exc, const char *msg)
{
    jclass exclazz = jenv->FindClass(exc);
    assert(exclazz);

    // Clear pending exceptions
    if (jenv->ExceptionOccurred())
        jenv->ExceptionClear();

    jenv->ThrowNew(exclazz, msg);
} //throw_exception_from_jni


void array_copy_jni(JNIEnv* jenv, jobject src, jint src_off, jobject dst, jint dst_off, jint count)
{
    ArrayCopyResult res;
    hythread_suspend_disable();
    if (src && dst) {
        res = array_copy(((ObjectHandle)src)->object, src_off, ((ObjectHandle)dst)->object, dst_off, count);
    } else {
        res = ACR_NullPointer;
    }
    hythread_suspend_enable();
    jclass tclass = NULL;
    switch (res) {
    case ACR_Okay:
        return;
    case ACR_NullPointer:
        tclass = FindClass(jenv, VM_Global_State::loader_env->JavaLangNullPointerException_String);
        break;
    case ACR_TypeMismatch:
        tclass = jenv->FindClass("java/lang/ArrayStoreException");
        break;
    case ACR_BadIndices:
        tclass = FindClass(jenv, VM_Global_State::loader_env->JavaLangArrayIndexOutOfBoundsException_String);
        break;
    }
    jenv->ThrowNew(tclass, "bad arrayCopy");
}


jclass FindClass(JNIEnv* env_ext, String* name)
{
    ASSERT_RAISE_AREA;
    TRACE2("jni", "FindClass called, name = " << name->bytes);

#ifdef _DEBUG
    // must already be checked by JNI FindClass - when this function is 
    // used inside VM - developer is to check this
    char *ch = strchr(name->bytes, '.');
    if (NULL != ch)
    {
        DIE(("Class name should not contain dots"));
    }
#endif

    JNIEnv_Internal *env = (JNIEnv_Internal *)env_ext;
    assert(hythread_is_suspend_enabled());
    // Determine loader
    StackTraceFrame stf;
    ClassLoader* loader = p_TLS_vmthread->onload_caller;
    if(loader == NULL) {
        bool res = st_get_frame(0, &stf);
        if (res)
            loader = stf.method->get_class()->get_class_loader();
        else
            loader = env->vm->vm_env->system_class_loader;
    }
    String *s = name;
    Class* clss =
        class_load_verify_prepare_by_loader_jni(VM_Global_State::loader_env, s, loader);
    if(clss) {
        class_initialize_from_jni(clss);
        if (exn_raised()) return NULL;
        assert(hythread_is_suspend_enabled());
        return struct_Class_to_jclass(clss);
    } else {
        assert(exn_raised());
        return NULL;
    }
}

jobject CreateNewThrowable(JNIEnv* jenv, Class* clazz, 
                           const char * message, jthrowable cause = 0)
{
    assert(hythread_is_suspend_enabled());

    if (!clazz) {
        return NULL;
    }

    Global_Env* genv = VM_Global_State::loader_env;

    jvalue args[1];
    jstring str = NULL;

    if(message) {
        str = NewStringUTF(jenv, message);
        if(!str) {
            return NULL;
        }
        args[0].l = str;
    }

    Method* ctor;
    if (message)
        ctor = clazz->lookup_method(genv->Init_String, genv->FromStringConstructorDescriptor_String);
    else
        ctor = clazz->lookup_method(genv->Init_String, genv->VoidVoidDescriptor_String);
    assert(ctor);
    jclass jclazz = struct_Class_to_jclass(clazz);
    assert(jclazz);
    jobject obj = NewObjectA(jenv, jclazz, (jmethodID)ctor, args);

    if (str) {
        jenv->DeleteLocalRef(str);
    }

    if (cause && obj && !exn_raised()) {
        //call initCause
        Method* initCause = class_lookup_method_recursive(clazz, genv->InitCause_String, genv->InitCauseDescriptor_String);
        assert(initCause);

        jvalue initArgs[2];
        initArgs[0].l = obj;
        initArgs[1].l = cause;
        jvalue res;

        assert(hythread_is_suspend_enabled());
        hythread_suspend_disable();
        vm_execute_java_method_array((jmethodID) initCause, &res, initArgs);
        hythread_suspend_enable();
    }

    return obj;
}

jobject create_default_instance(Class* clss) 
{    
    hythread_suspend_disable();
    ManagedObject *new_obj = class_alloc_new_object_and_run_default_constructor(clss);
    if (new_obj == NULL) {
        hythread_suspend_enable();
        assert(exn_raised());
        return NULL;
    }
    jobject h = oh_allocate_local_handle_from_jni();
    if (h == NULL) {
        hythread_suspend_enable();
        return NULL;
    }
    h->object = new_obj;
    hythread_suspend_enable();
    return h;
}

bool ensure_initialised(JNIEnv* env, Class* clss)
{
    ASSERT_RAISE_AREA;
    assert(hythread_is_suspend_enabled());

    if(!clss->is_initialized()) {
        class_initialize_from_jni(clss);
        if(clss->in_error()) {
            assert(exn_raised());
            return false;
        }
    }
    return true;
}

JavaVM * jni_get_java_vm(JNIEnv * jni_env) {
    return ((JNIEnv_Internal *)jni_env)->vm;
}

Global_Env * jni_get_vm_env(JNIEnv * jni_env) {
    return ((JNIEnv_Internal *)jni_env)->vm->vm_env;
}
