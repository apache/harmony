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
#define LOG_DOMAIN "vm.core.init"
#include "cxxlog.h"

#include <apr_env.h>
#include <apr_general.h>
#include <apr_dso.h>
#include "port_dso.h"

#include "open/vm_properties.h"
#include "open/gc.h"
#include "open/hythread_ext.h"
#include "open/vm_class_manipulation.h"

#include "jthread.h"
#include "vtable.h"
#include "init.h"
#include "classloader.h"
#include "jni_utils.h"
#include "mon_enter_exit.h"
#include "heap.h"
#include "port_filepath.h"
#include "component_manager.h"
#include "dll_gc.h"
#include "compile.h"
#include "interpreter.h"
#include "em_intf.h"
#include "dll_jit_intf.h"
#include "jit_runtime_support.h"
#include "jni_utils.h"
#include "platform_lowlevel.h"
#include "verify_stack_enumeration.h"
#include "nogc.h"
#include "vm_strings.h"
#include "slot.h"
#include "classpath_const.h"
#include "finalize.h"
#include "jit_intf.h"
#include "signals.h"

#ifdef _WIN32
// 20040427 Used to turn on heap checking on every allocation
#include <crtdbg.h>
#endif

VTable * cached_object_array_vtable_ptr;
bool parallel_jit = true;
VMEXPORT bool dump_stubs = false;

void* Slot::heap_base = NULL;
void* Slot::heap_ceiling = NULL;

Class* preload_class(Global_Env * vm_env, const char * classname) {
    String * s = vm_env->string_pool.lookup(classname);
    return vm_env->LoadCoreClass(s);
}

Class * preload_class(Global_Env * vm_env, String* s) {
    return vm_env->LoadCoreClass(s);
}

static Class * preload_primitive_class(Global_Env * vm_env, const char * classname) {
    String * s = vm_env->string_pool.lookup(classname);
    ClassLoader * cl = vm_env->bootstrap_class_loader;
    Class *clss = cl->NewClass(vm_env, s);
    clss->setup_as_primitive(cl);
    cl->InsertClass(clss);

    clss->prepare(vm_env);
    return clss;
}


#ifdef LIB_DEPENDENT_OPTS

static Class * class_initialize_by_name(Global_Env * vm_env, const char * classname) {
    ASSERT_RAISE_AREA;

    String *s = vm_env->string_pool.lookup(classname);
    Class *clss = vm_env->bootstrap_class_loader->LoadVerifyAndPrepareClass(vm_env, s);
    if (clss != NULL) {
        class_initialize(clss);
    }
    return clss;
}

static jint lib_dependent_opts() {
    ASSERT_RAISE_AREA;
    return class_initialize_by_name("java/lang/Math") != null ? JNI_OK : JNI_ERR;
}
#endif


// Create the java_lang_Class instance for a struct Class
// and set its "vm_class" field to point back to that structure.
void create_instance_for_class(Global_Env * vm_env, Class *clss) 
{
    clss->get_class_loader()->AllocateAndReportInstance(vm_env, clss);
    // set jlC to vtable - for non BS classes jlc is set in create_vtable
    if (clss->get_vtable()) // vtable = NULL for interfaces 
    {
        assert (!clss->get_vtable()->jlC); // used for BS classes only
        clss->get_vtable()->jlC = *clss->get_class_handle();
        assert (!clss->get_class_loader()->GetLoader());
    }
} //create_instance_for_class

// VM adapter part

static apr_dso_handle_t* get_harmonyvm_handle(){
    apr_dso_handle_t* descriptor;
    apr_pool_t* pool;
    int ret = apr_pool_create(&pool, NULL);
    assert(APR_SUCCESS == ret);
    ret = apr_dso_load(&descriptor, PORT_DSO_NAME("harmonyvm"), pool);
    assert(APR_SUCCESS == ret);
    return descriptor;
}

extern "C" VMEXPORT 
void* vm_get_interface(const char* func_name){
    static apr_dso_handle_t* descriptor = get_harmonyvm_handle();
    void* p_func = NULL;
    int ret = apr_dso_sym((apr_dso_handle_sym_t*) &p_func, descriptor, func_name);

    //assert(APR_SUCCESS == ret);

    //FIXME: temporary solution, should be fixed in next patch
    if (p_func) {
        return p_func;
        
    } else if (strcmp(func_name,"vector_get_first_element_offset") == 0) {
        return (void*)vector_first_element_offset_class_handle;
    } else if (strcmp(func_name,"vector_get_length_offset") == 0) {
        return (void*)vector_length_offset;
    } else if (strcmp(func_name,"vm_tls_alloc") == 0) {
        return (void*)hythread_tls_alloc;
    } else if (strcmp(func_name,"vm_tls_get_offset") == 0) {
        return (void*)hythread_tls_get_offset;
    } else if (strcmp(func_name,"vm_tls_get_request_offset") == 0) {
        return (void*)hythread_tls_get_request_offset;
    } else if (strcmp(func_name,"vm_tls_is_fast") == 0) {
        return (void*)hythread_uses_fast_tls;
    } else if (strcmp(func_name,"vm_get_tls_offset_in_segment") == 0) {
        return (void*)hythread_get_hythread_offset_in_tls;
    } else {
        return NULL;
    }
}

#define GC_DLL_COMP   PORT_DSO_NAME("gc_gen")
#define GC_DLL_UNCOMP PORT_DSO_NAME("gc_gen_uncomp")

#if defined(REFS_USE_COMPRESSED)
#define GC_DLL GC_DLL_COMP
#elif defined(REFS_USE_UNCOMPRESSED)
#define GC_DLL GC_DLL_UNCOMP
#else // for REFS_USE_RUNTIME_SWITCH
#define GC_DLL (vm_env->compress_references ? GC_DLL_COMP : GC_DLL_UNCOMP)
#endif


/**
 * Loads DLLs.
 */
static jint process_properties_dlls(Global_Env * vm_env) {
    jint status;

    if (!vm_env->VmProperties()->is_set("vm.em_dll")) {
        vm_env->VmProperties()->set("vm.em_dll", PORT_DSO_NAME("em"));
    }

    char* dll = vm_env->VmProperties()->get("vm.em_dll");
    TRACE("analyzing em dll " << dll);
    status = CmLoadComponent(dll, "EmInitialize");
    vm_env->VmProperties()->destroy(dll);
    if (status != JNI_OK) {
        LWARN(13, "Cannot load EM component from {0}" << dll);
        return status;
    }

    status = vm_env->cm->GetComponent(&(vm_env->em_component), OPEN_EM);
    if (JNI_OK != status) {
        return status;
    }

    status = vm_env->cm->CreateInstance(&(vm_env->em_instance), OPEN_EM);
    if (JNI_OK != status) {
        LWARN(14, "Cannot instantiate EM");
        return status;
    }

    status = vm_env->em_component->GetInterface((OpenInterfaceHandle*) &(vm_env->em_interface), OPEN_INTF_EM_VM);
    if (JNI_OK != status) {
        LWARN(15, "Cannot get EM_VM interface");
        return status;
    }

    /*
     * Preload <GC>.dll which is specified by 'gc.dll' property.
     * 'gc.dll' property is set when specified in command line.
     * When undefined, set default gc
     */
#ifndef USE_GC_STATIC
    if (!vm_env->VmProperties()->is_set("gc.dll")) {
        vm_env->VmProperties()->set_new("gc.dll", GC_DLL);
    }

    char* gc_dll = vm_env->VmProperties()->get("gc.dll");

    if (!gc_dll) {
        LWARN(44, "{0} internal property is undefined" << "gc.dll");
        return JNI_ERR;
    }
    TRACE("analyzing gc.dll " << gc_dll);

    if (vm_is_a_gc_dll(gc_dll)) {
        vm_add_gc(gc_dll);
    } else {
        LWARN(16, "GC library cannot be loaded: {0}" << gc_dll);
        status = JNI_ERR;
    }
    vm_env->VmProperties()->destroy(gc_dll);
#endif
    return status;
}

/**
 * Check compression modes and adjust if needed
 */
static jint process_compression_modes(Global_Env * vm_env)
{
#if !defined(POINTER64) || defined(REFS_USE_UNCOMPRESSED)
        return JNI_OK;
#else

    if (!vm_env->VmProperties()->is_set("gc.ms") &&
        !vm_env->VmProperties()->is_set("gc.mx"))
    { // Heap size is not specified, use default compressed mode
        return JNI_OK;
    }

    size_t ms = vm_property_get_size("gc.ms", 0, VM_PROPERTIES);
    size_t mx = vm_property_get_size("gc.mx", 0, VM_PROPERTIES);
    // Currently 4Gb is maximum for compressed mode
    // If GC cannot allocate heap up to 4Gb, gc_init() will fail
    size_t max_size = ((int64)4096)*1024*1024;

#ifdef REFS_USE_COMPRESSED
    if (ms >= max_size || mx >= max_size)
    { // Heap is too large for compressed mode
        LWARN(45, "ERROR: Heap size is too large for precompiled compressed mode.");
        return JNI_ERR;
    }
#elif defined(REFS_USE_RUNTIME_SWITCH)
    if (ms >= max_size || mx >= max_size)
    { // Large heap; use uncompressed references
        vm_properties_set_value("vm.compress_references", "false", VM_PROPERTIES);
        vm_env->compress_references = false;
        return JNI_OK;
    }
#endif // REFS_USE_RUNTIME_SWITCH
    return JNI_OK;
#endif // !defined(POINTER64) || defined(REFS_USE_UNCOMPRESSED)
}

/**
 * Checks whether current platform is supported or not.
 */
static jint check_platform() {
#if defined(PLATFORM_NT)
    OSVERSIONINFO osvi;
    osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFO);
    BOOL ok = GetVersionEx(&osvi);
    if(!ok) {
        DWORD e = GetLastError();
        printf("Windows error: %d\n", e);
        return JNI_ERR;
    }
    if((osvi.dwMajorVersion == 4 && osvi.dwMinorVersion == 0) ||  // NT 4.0
       (osvi.dwMajorVersion == 5 && osvi.dwMinorVersion == 0) ||  // Windows 2000
       (osvi.dwMajorVersion == 5 && osvi.dwMinorVersion == 1) ||  // Windows XP
       (osvi.dwMajorVersion == 5 && osvi.dwMinorVersion == 2) ||  // Windows.NET            
       (osvi.dwMajorVersion == 6 && osvi.dwMinorVersion == 0)) {  // Windows Vista            
            return JNI_OK;
    }
    printf("Windows %d.%d is not supported\n", osvi.dwMajorVersion, osvi.dwMinorVersion);
    return JNI_ERR;
#else
    return JNI_OK;
#endif
}

/**
 * Ensures that different VM components have consistent compression modes.
 */
static jint check_compression() {
        // Check for a mismatch between whether the various VM components all compress references or not.
    Boolean vm_compression = vm_is_heap_compressed();
    Boolean gc_compression = gc_supports_compressed_references();
    if (vm_compression) {
        if (!gc_compression) {
            LWARN(17, "VM component mismatch: the VM compresses references but the GC doesn't.");
            return JNI_ERR;
        }
        
        // We actually check the first element in the jit_compilers array, as current JIT
        // always returns FALSE to the supports_compressed_references() call. 
        JIT **jit = &jit_compilers[0];
        if (!interpreter_enabled()) {
            Boolean jit_compression = (*jit)->supports_compressed_references();
            if (!jit_compression) {
                LWARN(18, "VM component mismatch: the VM compresses references but a JIT doesn't");
                return JNI_ERR;
            }
        }
    } else {
        if (gc_compression) {
            LWARN(19, "VM component mismatch: the VM doesn't compress references but the GC does.");
            return JNI_ERR;
        }
        JIT **jit = &jit_compilers[0];
        if (!interpreter_enabled()) {
            Boolean jit_compression = (*jit)->supports_compressed_references();
            if (jit_compression) {
                LWARN(20, "VM component mismatch: the VM doesn't compress references but a JIT does");
                return JNI_ERR;
            }
        }
    }
    return JNI_OK;
}

typedef void* (JNICALL *GDBA) (JNIEnv* env, jobject buf);
typedef jobject (JNICALL *NDB)(JNIEnv* env, void* address, jlong capacity);
typedef jlong (JNICALL *GDBC)(JNIEnv* env, jobject buf);

/**
 * Imports NIO functions to JNI functions table from hynio lib.
 * Note: bootstrap classloader is picky to load classlib's natives earliest,
 * so this should be called after bcl initialization.
 */
static jint populate_jni_nio() {
    bool just_loaded;
    NativeLoadStatus loading_status;
    NativeLibraryHandle handle = natives_load_library(
        PORT_DSO_NAME("hynio"), &just_loaded, &loading_status);
    if (!handle || loading_status) {
        char error_message[1024];
        natives_describe_error(loading_status, error_message, sizeof(error_message));

        LWARN(21, "Failed to initialize JNI NIO support: {0}" << error_message);
        return JNI_ERR;
    }
    
    apr_dso_handle_sym_t gdba, gdbc, ndb;
#if defined WIN32 && !defined _EM64T_
#define GET_DIRECT_BUFFER_ADDRESS "_GetDirectBufferAddress@8"
#define GET_DIRECT_BUFFER_CAPACITY "_GetDirectBufferCapacity@8"
#define NEW_DIRECT_BYTE_BUFFER "_NewDirectByteBuffer@16"
#else
#define GET_DIRECT_BUFFER_ADDRESS "GetDirectBufferAddress"
#define GET_DIRECT_BUFFER_CAPACITY "GetDirectBufferCapacity"
#define NEW_DIRECT_BYTE_BUFFER "NewDirectByteBuffer"
#endif
    if (APR_SUCCESS == apr_dso_sym(&gdba, handle, GET_DIRECT_BUFFER_ADDRESS)
        && APR_SUCCESS == apr_dso_sym(&gdbc, handle, GET_DIRECT_BUFFER_CAPACITY)
        && APR_SUCCESS == apr_dso_sym(&ndb, handle, NEW_DIRECT_BYTE_BUFFER))
    {
        jni_vtable.GetDirectBufferAddress = (GDBA)gdba;
        jni_vtable.GetDirectBufferCapacity = (GDBC)gdbc;
        jni_vtable.NewDirectByteBuffer = (NDB)ndb;
        return JNI_OK;
    } 
    else 
    {
        LWARN(22, "Failed to import JNI NIO functions.");
        return JNI_ERR;
    }
}

/**
 * Loads initial classes. For example j.l.Object, j.l.Class, etc.
 */
static void bootstrap_initial_java_classes(Global_Env * vm_env)
{
    assert(hythread_is_suspend_enabled());
    TRACE("bootstrapping initial java classes");

    vm_env->bootstrap_class_loader->Initialize();

    /*
     *  Bootstrap java.lang.Class class. This requires also loading the other classes 
     *  it inherits/implements: java.io.Serializable and java.lang.Object, and 
     * j.l.reflect.AnnotatedElement, GenericDeclaration and Type as per Java 5
     */
    vm_env->StartVMBootstrap();
    vm_env->JavaLangObject_Class       = preload_class(vm_env, vm_env->JavaLangObject_String);
    vm_env->java_io_Serializable_Class = preload_class(vm_env, vm_env->Serializable_String);
    Class* AnnotatedElement_Class      = preload_class(vm_env, "java/lang/reflect/AnnotatedElement");
    Class* GenericDeclaration_Class    = preload_class(vm_env, "java/lang/reflect/GenericDeclaration");
    Class* Type_Class                  = preload_class(vm_env, "java/lang/reflect/Type");
    vm_env->JavaLangClass_Class        = preload_class(vm_env, vm_env->JavaLangClass_String);

    vm_env->FinishVMBootstrap();

    // Now create the java_lang_Class instance.
    create_instance_for_class(vm_env, vm_env->JavaLangClass_Class);

    ClassTable* table = vm_env->bootstrap_class_loader->GetLoadedClasses();
    
    unsigned num = 0;
    for (ClassTable::const_iterator it = table->begin(), end = table->end(); 
        it != end; ++it, ++num)
    {
        Class* booted = (*it).second;
        if (booted != vm_env->JavaLangClass_Class) {
            create_instance_for_class(vm_env, booted);
        }
        jvmti_send_class_load_event(vm_env, booted);
        jvmti_send_class_prepare_event(booted);
    }

    TRACE("bootstrapping initial java classes complete");
} // bootstrap_initial_java_classes

/**
 * Loads hot classes.
 */
static jint preload_classes(Global_Env * vm_env) {
    // Bootstrap initial classes
    bootstrap_initial_java_classes(vm_env);

    TRACE2("init", "preloading primitive type classes");
    vm_env->Boolean_Class = preload_primitive_class(vm_env, "boolean");
    vm_env->Char_Class    = preload_primitive_class(vm_env, "char");
    vm_env->Float_Class   = preload_primitive_class(vm_env, "float");
    vm_env->Double_Class  = preload_primitive_class(vm_env, "double");
    vm_env->Byte_Class    = preload_primitive_class(vm_env, "byte");
    vm_env->Short_Class   = preload_primitive_class(vm_env, "short");
    vm_env->Int_Class     = preload_primitive_class(vm_env, "int");
    vm_env->Long_Class    = preload_primitive_class(vm_env, "long");

    vm_env->Void_Class    = preload_primitive_class(vm_env, "void");

    vm_env->ArrayOfBoolean_Class   = preload_class(vm_env, "[Z");
    vm_env->ArrayOfByte_Class      = preload_class(vm_env, "[B");
    vm_env->ArrayOfChar_Class      = preload_class(vm_env, "[C");
    vm_env->ArrayOfShort_Class     = preload_class(vm_env, "[S");
    vm_env->ArrayOfInt_Class       = preload_class(vm_env, "[I");
    vm_env->ArrayOfLong_Class      = preload_class(vm_env, "[J");
    vm_env->ArrayOfFloat_Class     = preload_class(vm_env, "[F");
    vm_env->ArrayOfDouble_Class    = preload_class(vm_env, "[D");

    TRACE2("init", "preloading string class");
    vm_env->JavaLangString_Class = preload_class(vm_env, vm_env->JavaLangString_String);
    vm_env->strings_are_compressed =
        (class_lookup_field_recursive(vm_env->JavaLangString_Class, "bvalue", "[B") != NULL);
    vm_env->JavaLangString_VTable = vm_env->JavaLangString_Class->get_vtable();

    Class* VM_class = preload_class(vm_env, "org/apache/harmony/kernel/vm/VM");
    vm_env->VM_intern = class_lookup_method_recursive(VM_class, "intern",
            "(Ljava/lang/String;)Ljava/lang/String;");

    TRACE2("init", "preloading exceptions");
    vm_env->java_lang_Throwable_Class =
        preload_class(vm_env, vm_env->JavaLangThrowable_String);
    vm_env->java_lang_StackTraceElement_Class = 
        preload_class(vm_env, "java/lang/StackTraceElement");
    vm_env->java_lang_Error_Class =
        preload_class(vm_env, "java/lang/Error");
    vm_env->java_lang_ExceptionInInitializerError_Class =
        preload_class(vm_env, "java/lang/ExceptionInInitializerError");
    vm_env->java_lang_NoClassDefFoundError_Class =
        preload_class(vm_env, "java/lang/NoClassDefFoundError");
    vm_env->java_lang_ClassNotFoundException_Class =
        preload_class(vm_env, "java/lang/ClassNotFoundException");
    vm_env->java_lang_NullPointerException_Class =
        preload_class(vm_env, vm_env->JavaLangNullPointerException_String);
    vm_env->java_lang_StackOverflowError_Class =
        preload_class(vm_env, "java/lang/StackOverflowError");
    vm_env->java_lang_ArrayIndexOutOfBoundsException_Class =
        preload_class(vm_env, vm_env->JavaLangArrayIndexOutOfBoundsException_String);
    vm_env->java_lang_ArrayStoreException_Class =
        preload_class(vm_env, "java/lang/ArrayStoreException");
    vm_env->java_lang_ArithmeticException_Class =
        preload_class(vm_env, "java/lang/ArithmeticException");
    vm_env->java_lang_ClassCastException_Class =
        preload_class(vm_env, "java/lang/ClassCastException");
    vm_env->java_lang_OutOfMemoryError_Class = 
        preload_class(vm_env, "java/lang/OutOfMemoryError");
    vm_env->java_lang_InternalError_Class =
        preload_class(vm_env, "java/lang/InternalError");
    vm_env->java_lang_ThreadDeath_Class = 
        preload_class(vm_env, "java/lang/ThreadDeath");

    // String must be initialized before strings from intern pool are
    // used. But for initializing l.j.String we need to have exception
    // classes loaded, because the first call to compilations
    // initializes all of the JIT helpers that hardcode class handles
    // inside of the helpers.
    hythread_suspend_disable();
    class_initialize(vm_env->JavaLangString_Class);
    hythread_suspend_enable();

    vm_env->java_lang_Cloneable_Class =
        preload_class(vm_env, vm_env->Clonable_String);
    vm_env->java_lang_Thread_Class =
        preload_class(vm_env, "java/lang/Thread");
    vm_env->java_lang_ThreadGroup_Class =
        preload_class(vm_env, "java/lang/ThreadGroup");
    vm_env->java_util_LinkedList_Class =
        preload_class(vm_env, "java/util/LinkedList");
    vm_env->java_util_Date_Class = 
        preload_class(vm_env, "java/util/Date");
    vm_env->java_util_Properties_Class = 
        preload_class(vm_env, "java/util/Properties");
    vm_env->java_lang_Runtime_Class = 
        preload_class(vm_env, "java/lang/Runtime");

    vm_env->java_lang_reflect_Constructor_Class = 
        preload_class(vm_env, vm_env->JavaLangReflectConstructor_String);
    vm_env->java_lang_reflect_Field_Class = 
        preload_class(vm_env, vm_env->JavaLangReflectField_String);
    vm_env->java_lang_reflect_Method_Class = 
        preload_class(vm_env, vm_env->JavaLangReflectMethod_String);
    
    return JNI_OK;
}

/**
 * Calls java.lang.ClassLoader.getSystemClassLoader() to obtain system
 * class loader object.
 * @return JNI_OK on success.
 */
static jint initialize_system_class_loader(JNIEnv * jni_env) {
    Global_Env * vm_env = jni_get_vm_env(jni_env);
    jclass cl = jni_env->FindClass("java/lang/ClassLoader");
    if (! cl) 
        return JNI_ERR;

    jmethodID gcl = jni_env->GetStaticMethodID(cl, "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
    if (! gcl) 
        return JNI_ERR;

    jobject scl = jni_env->CallStaticObjectMethod(cl, gcl);
    if (! scl)
        return JNI_ERR;

    hythread_suspend_disable();
    vm_env->system_class_loader = (UserDefinedClassLoader *)
        ClassLoader::LookupLoader(((ObjectHandle)scl)->object);
    hythread_suspend_enable();

    return JNI_OK;
}

jint set_current_thread_context_loader(JNIEnv* jni_env) {
    jthread current_thread = jthread_self();
    jfieldID scl_field = jni_env->GetFieldID(jni_env->GetObjectClass(current_thread),
        "contextClassLoader", "Ljava/lang/ClassLoader;");
    assert(scl_field);
    Global_Env* vm_env = jni_get_vm_env(jni_env);
    jobject loader = jni_env->NewLocalRef((jobject)(vm_env->system_class_loader->GetLoaderHandle()));
    jni_env->SetObjectField(current_thread, scl_field, loader);
    jni_env->DeleteLocalRef(loader);

    return JNI_OK;
}

#define PROCESS_EXCEPTION(messageId, message) \
{ \
    LECHO(messageId, message << "Internal error: "); \
\
    if (jni_env->ExceptionCheck()== JNI_TRUE) \
    { \
        jni_env->ExceptionDescribe(); \
        jni_env->ExceptionClear(); \
    } \
\
    return JNI_ERR; \
} \

/**
 * Executes j.l.VMStart.initialize() method.
 */
static jint run_java_init(JNIEnv * jni_env) {
    assert(hythread_is_suspend_enabled());

    jclass start_class = jni_env->FindClass("java/lang/VMStart");
    if (jni_env->ExceptionCheck()== JNI_TRUE || start_class == NULL) {
        PROCESS_EXCEPTION(35, "{0}can't find starter class: java.lang.VMStart.");
    }

    jmethodID init_method = jni_env->GetStaticMethodID(start_class, "initialize", "()V");
    if (jni_env->ExceptionCheck()== JNI_TRUE || init_method == NULL) {
        PROCESS_EXCEPTION(36, "{0}can't find java.lang.VMStart.initialize() method.");
    }

    jni_env->CallStaticVoidMethod(start_class, init_method);
    if (jni_env->ExceptionCheck()== JNI_TRUE) {
        PROCESS_EXCEPTION(37, "{0}java.lang.VMStart.initialize() method completed with an exception.");
    }
    return JNI_OK;
}

/**
 * Creates new j.l.Thread object
 *
 * @param[out] thread_object pointer to created thread object
 * @param[in] jni_env JNI environment associated with the current thread
 * @param[in] group thread group where new thread should be placed in
 * @param[in] name thread's name
 * @param[in] daemon JNI_TRUE if new thread is a daemon, JNI_FALSE otherwise
 */
static jint vm_create_jthread(jthread * thread_object, JNIEnv * jni_env, jobject group, const char * name, jboolean daemon) {
    static Method * constructor = NULL;
    const char * descriptor = "(Ljava/lang/ThreadGroup;Ljava/lang/String;JJIZ)V";
    jvalue args[7];
    Global_Env * vm_env;
    Class * thread_class;
    ObjectHandle thread_handle;
    hythread_t native_thread;


    assert(!hythread_is_suspend_enabled());

    vm_env = jni_get_vm_env(jni_env);

    thread_class = vm_env->java_lang_Thread_Class;
    class_initialize(thread_class);
    if (exn_raised())
    {
        TRACE("Failed to initialize class for java/lang/Thread class = " << exn_get_name());
        hythread_suspend_enable();
        exn_print_stack_trace(stderr, exn_get());
        hythread_suspend_disable();
        return JNI_ERR;
    }

    // Allocate new j.l.Thread object.
    thread_handle = oh_allocate_global_handle();
    thread_handle->object = class_alloc_new_object(thread_class);
    if (thread_handle->object == NULL) {
        assert(!hythread_is_suspend_enabled());
        assert(exn_raised() && p_TLS_vmthread->thread_exception.exc_object == vm_env->java_lang_OutOfMemoryError->object);
        return JNI_ENOMEM;
    }
    *thread_object = thread_handle;

    if (constructor == NULL) {
        // Initialize created thread object.
        constructor = thread_class->lookup_method(vm_env->Init_String,
            vm_env->string_pool.lookup(descriptor));
        if (constructor == NULL) {
            TRACE("Failed to find thread's constructor " << descriptor << " , exception = " << exn_get());
            return JNI_ERR;
        }
    }

    args[0].l = thread_handle;
    args[1].l = group;

    if (name) {
        args[2].l = oh_allocate_local_handle();
        args[2].l->object = string_create_from_utf8(name,
            (unsigned)strlen(name));
    } else {
        args[2].l = NULL;
    }
    native_thread = hythread_self();
    args[3].j = (POINTER_SIZE_INT) native_thread;
    args[4].j = 0;
    args[5].i = (jint)hythread_get_priority(native_thread);
    args[6].z = daemon;
    
    DebugUtilsTI *ti = VM_Global_State::loader_env->TI;
    if (ti->isEnabled()) {
        ti->doNotReportLocally();//-----------------------------------V
        vm_execute_java_method_array((jmethodID) constructor, 0, args);
        ti->reportLocally();     //-----------------------------------^
    } else {
        vm_execute_java_method_array((jmethodID) constructor, 0, args);
    }

    if (exn_raised()) {
        TRACE("Failed to initialize new thread object, exception = " << exn_get_name());
        hythread_suspend_enable();
        exn_print_stack_trace(stderr, exn_get());
        hythread_suspend_disable();
        return JNI_ERR;
    }
    return JNI_OK;
}

/**
 * Attaches current thread to VM and creates j.l.Thread instance.
 *
 * @param[out] p_jni_env points to created JNI environment
 * @param[out] java_thread global reference holding j.l.Thread object
 * @param[in] java_vm VM to attach thread to
 * @param[in] group thread group for attaching thread
 * @param[in] name thread name
 * @param[in] daemon JNI_TRUE if thread is daemon, JNI_FALSE otherwise
 * @return JNI_OK on success.
 */
jint vm_attach_internal(JNIEnv ** p_jni_env, jthread * java_thread, JavaVM * java_vm, jobject group, const char * name, jboolean daemon) {
    JNIEnv * jni_env;
    hythread_t native_thread;
    jint status;

    native_thread = hythread_self();
    if (!native_thread) {
        native_thread = (hythread_t)jthread_allocate_thread();
        IDATA hy_status = hythread_attach_ex(native_thread, NULL, NULL);
        if (hy_status != TM_ERROR_NONE)
            return JNI_ERR;
    }
    assert(native_thread);

    status = vm_attach(java_vm, &jni_env);
    if (status != JNI_OK)
        return status;

    *p_jni_env = jni_env;

    hythread_suspend_disable();
    // Global reference will be created for new thread object.
    status = vm_create_jthread(java_thread, jni_env, group, name, daemon);
    hythread_suspend_enable();
    
    return status;
}

/**
 * First VM initialization step. At that moment neither JNI is available
 * nor main thread is attached to VM.
 */
int vm_init1(JavaVM_Internal * java_vm, JavaVMInitArgs * vm_arguments) {
    jint status;
    Global_Env * vm_env;
    JNIEnv * jni_env;

    TRACE("Initializing VM");

    vm_env = java_vm->vm_env;

    vm_thread_t main_thread = jthread_allocate_thread();
    assert(main_thread);
    if (hythread_attach_ex((hythread_t)main_thread, NULL, NULL) != TM_ERROR_NONE) {
        return JNI_ERR;
    }

    assert(hythread_is_suspend_enabled());

    status = check_platform();
    if (status != JNI_OK) return status;

    // TODO: global variables should be removed for multi-VM support
    VM_Global_State::loader_env = vm_env;

    // Initialize arguments
    initialize_vm_cmd_state(vm_env, vm_arguments);

    vm_monitor_init();

    /*    BEGIN: Property processing.    */

    // 20030407 Note: property initialization must follow initialization of the default JITs to allow 
    // the command line to override those default JITs.

    status = initialize_properties(vm_env);
    if (status != JNI_OK) return status;

    tm_properties = (struct tm_props*) STD_MALLOC(sizeof(struct tm_props));
    if (!tm_properties) {
        LWARN(30, "failed to allocate mem for tp properties");
        return JNI_ERR;
    }

    tm_properties->use_soft_unreservation = vm_property_get_boolean("thread.soft_unreservation", FALSE, VM_PROPERTIES);

    parse_vm_arguments2(vm_env);

    vm_env->verify = vm_property_get_boolean("vm.use_verifier", TRUE, VM_PROPERTIES);
#ifdef REFS_USE_RUNTIME_SWITCH
    vm_env->compress_references = vm_property_get_boolean("vm.compress_references", TRUE, VM_PROPERTIES);
#endif
    // use platform default values for field sorting and field compaction
    // if these values are not specifed on command line
    // see Global_Env::Global_Env for defaults
    vm_env->sort_fields = vm_property_get_boolean("vm.sort_fields", vm_env->sort_fields, VM_PROPERTIES);
    vm_env->compact_fields = vm_property_get_boolean("vm.compact_fields", vm_env->compact_fields, VM_PROPERTIES);
    vm_env->use_common_jar_cache = vm_property_get_boolean("vm.common_jar_cache", TRUE, VM_PROPERTIES);
    vm_env->map_bootsrtap_jars = vm_property_get_boolean("vm.map_bootstrap_jars", FALSE, VM_PROPERTIES);

    vm_env->init_pools();

    // Check compression modes and heap size
    status = process_compression_modes(vm_env);
    if (status != JNI_OK) return status;

    // "Tool Interface" enabling.
    vm_env->TI->setExecutionMode(vm_env);

    status = process_properties_dlls(vm_env);
    if (status != JNI_OK) return status;
 
    parse_jit_arguments(&vm_env->vm_arguments);

    vm_env->pin_interned_strings = 
        (bool)vm_property_get_boolean("vm.pin_interned_strings", FALSE, VM_PROPERTIES);

    initialize_verify_stack_enumeration();

    /*    END: Property processing.    */

    // Initialize memory allocation.
    status = gc_init();
    if (status != JNI_OK) return status;

    // TODO: change all uses of Class::heap_base to Slot::heap_base
    Slot::init(gc_heap_base_address(), gc_heap_ceiling_address());

    // TODO: find another way to initialize the following.
    vm_env->heap_base = (U_8*)gc_heap_base_address();
    vm_env->heap_end  = (U_8*)gc_heap_ceiling_address();
    vm_env->managed_null = REF_MANAGED_NULL;

    // 20030404 This handshaking protocol isn't quite correct. It doesn't
    // work at the moment because JIT has not yet been modified to support
    // compressed references, so it never answers "true" to supports_compressed_references().
    // ppervov: this check is not correct since a call to
    // gc_supports_compressed_references returns capability while a call to
    // vm_is_heap_compressed returns current VM state, not potential
    // ability to support compressed mode
    // So, this check is turned off for now and it is FIXME
    // 20071109 process_compression_modes() now automatically selects compression
    // mode depending on heap size requested. If compressed mode is selected,
    // check_compression() should check if other components support this mode,
    // and either fail or switch to uncompressed mode
    //status = check_compression();
    //if (status != JNI_OK) return status;

    // Prepares to load natives
    status = natives_init();
    if (status != JNI_OK) return status;

    if (vm_initialize_signals() != 0)
        return JNI_ERR;

    status = vm_attach(java_vm, &jni_env);
    if (status != JNI_OK) return status;
    
    // "Tool Interface" initialization
    status = vm_env->TI->Init(java_vm);
    if (status != JNI_OK) {
        LWARN(24, "Failed to initialize JVMTI.");
        return status;
    }

    status = preload_classes(vm_env);
    if (status != JNI_OK) return status;
    
    populate_jni_nio();

    // Now the thread is attached to VM and it is valid to disable it.
    hythread_suspend_disable();

    // Create java.lang.Object.
    vm_env->java_lang_Object = oh_allocate_global_handle();
    vm_env->java_lang_Object->object =
        class_alloc_new_object(vm_env->JavaLangObject_Class);
    // Create java.lang.OutOfMemoryError.
    class_initialize(vm_env->java_lang_OutOfMemoryError_Class);
    vm_env->java_lang_OutOfMemoryError = oh_allocate_global_handle();
    vm_env->java_lang_OutOfMemoryError->object =
        class_alloc_new_object(vm_env->java_lang_OutOfMemoryError_Class);
    // Create java.lang.ThreadDeath.
    vm_env->java_lang_ThreadDeath = oh_allocate_global_handle();
    vm_env->java_lang_ThreadDeath->object =
        class_alloc_new_object(vm_env->java_lang_ThreadDeath_Class);

    // Create pop frame exception.
    vm_env->popFrameException = oh_allocate_global_handle();
    vm_env->popFrameException->object =
        class_alloc_new_object(vm_env->java_lang_Error_Class);

    // Precompile StackOverflowError.
    class_alloc_new_object_and_run_default_constructor(vm_env->java_lang_StackOverflowError_Class);
    // Precompile ThreadDeath.
    class_alloc_new_object_and_run_default_constructor(vm_env->java_lang_ThreadDeath_Class);
    // Precompile InternalError.
    class_alloc_new_object_and_run_default_constructor(vm_env->java_lang_InternalError_Class);

    // j.l.Class needs to be initialized for loading magics helper
    // class
    class_initialize(vm_env->JavaLangClass_Class);
    hythread_suspend_enable();

    Method * m;

    // pre compile detach and all includes
    if (!interpreter_enabled()) {
        m = vm_env->java_lang_Thread_Class->lookup_method(
            vm_env->Detach_String, vm_env->DetachDescriptor_String);
        assert(m);
        vm_env->em_interface->CompileMethod(m);

        m = vm_env->java_lang_Thread_Class->lookup_method(
            vm_env->GetUncaughtExceptionHandler_String,
            vm_env->GetUncaughtExceptionHandlerDescriptor_String);
        assert(m);
        vm_env->em_interface->CompileMethod(m);

        m = vm_env->java_lang_ThreadGroup_Class->lookup_method(
            vm_env->UncaughtException_String, vm_env->UncaughtExceptionDescriptor_String);
        assert(m);
        vm_env->em_interface->CompileMethod(m);

        m = vm_env->java_lang_Thread_Class->lookup_method(
            vm_env->GetDefaultUncaughtExceptionHandler_String,
            vm_env->GetDefaultUncaughtExceptionHandlerDescriptor_String);
        assert(m);
        vm_env->em_interface->CompileMethod(m);

        m = vm_env->java_lang_Thread_Class->lookup_method(
            vm_env->GetName_String, vm_env->GetNameDescriptor_String);
        assert(m);
        vm_env->em_interface->CompileMethod(m);

        m = vm_env->java_lang_ThreadGroup_Class->lookup_method(
            vm_env->Remove_String, vm_env->RemoveDescriptor_String);
        assert(m);
        vm_env->em_interface->CompileMethod(m);

        m = vm_env->java_util_LinkedList_Class->lookup_method(
            vm_env->LLRemove_String, vm_env->LLRemoveDescriptor_String);
        assert(m);
        vm_env->em_interface->CompileMethod(m);
    }

    // Mark j.l.Throwable() constructor as a side effects free.
    m = vm_env->java_lang_Throwable_Class->lookup_method(
        vm_env->Init_String, vm_env->VoidVoidDescriptor_String);
    assert(m);
    m->set_side_effects(MSE_False);

    // Mark j.l.Throwable(j.l.String) constructor as a side effects free.
    m = vm_env->java_lang_Throwable_Class->lookup_method(
        vm_env->Init_String, vm_env->FromStringConstructorDescriptor_String);
    assert(m);
    m->set_side_effects(MSE_False);

    void global_object_handles_init(JNIEnv *);
    global_object_handles_init(jni_env);

    Class * aoObjectArray = preload_class(vm_env, "[Ljava/lang/Object;");
    cached_object_array_vtable_ptr = aoObjectArray->get_vtable();

    // the following is required for creating exceptions
    preload_class(vm_env, "[Ljava/lang/VMClassRegistry;");
    extern int resolve_const_pool(Global_Env& env, Class *clss);
    status = resolve_const_pool(*vm_env, vm_env->java_lang_Throwable_Class);
    if(status != 0) {
        LWARN(25, "Failed to resolve class {0}" << "java/lang/Throwable");
        return JNI_ERR;
    }

    // We assume, that at this point VM supports exception objects creation.
    vm_env->ReadyForExceptions();

    status = helper_magic_init(vm_env);
    if(status != 0) {
        return JNI_ERR;
    }

    return JNI_OK;
}

/**
 * Second VM initialization stage. At that moment JNI services are available
 * and main thread has been already attached to VM.
 */
jint vm_init2(JNIEnv * jni_env) {
    jint status;
    Global_Env * vm_env;

    assert(hythread_is_suspend_enabled());

    vm_env = jni_get_vm_env(jni_env);


    TRACE("Invoking the java.lang.Class constructor");
    Class * jlc = vm_env->JavaLangClass_Class;
    jobject jlo = struct_Class_to_java_lang_Class_Handle(jlc);

    jmethodID java_lang_class_init = GetMethodID(jni_env, jlo, "<init>", "()V");
    jvalue args[1];
    args[0].l = jlo;

    hythread_suspend_disable();

    vm_execute_java_method_array(java_lang_class_init, 0, args);
    assert(!exn_raised());

    void unsafe_global_object_handles_init(JNIEnv *);
    unsafe_global_object_handles_init(jni_env);

    hythread_suspend_enable();

    if (vm_property_get_boolean("vm.finalize", TRUE, VM_PROPERTIES)) {
        // Load and initialize finalizer thread.
        vm_env->java_lang_FinalizerThread_Class =
            preload_class(vm_env, "java/lang/FinalizerThread");
        assert(vm_env->java_lang_FinalizerThread_Class);

        class_initialize_from_jni(vm_env->java_lang_FinalizerThread_Class);
        vm_obtain_finalizer_fields();
    }
    if(vm_env->TI->isEnabled() && vm_env->TI->needCreateEventThread() ) {
        vm_env->TI->TIenvs_lock._lock();
        jvmti_create_event_thread();
        vm_env->TI->disableEventThreadCreation();
        vm_env->TI->TIenvs_lock._unlock();
    }

    TRACE("initialization of system classes completed");

#ifdef WIN32
    // Code to start up Networking on Win32
    WORD wVersionRequested;
    WSADATA wsaData;
    int err;
    wVersionRequested = MAKEWORD(2, 2);
    err = WSAStartup(wVersionRequested, &wsaData);
    if (err != 0) {
        // Tell the user that we could not find a usable WinSock DLL.                                      
        LWARN(26, "Couldn't startup Winsock 2.0 dll");
    }
#endif

#ifdef LIB_DEPENDENT_OPTS
    lib_dependent_opts();
#endif

    TRACE2("init", "initializing system class loader");
    status = initialize_system_class_loader(jni_env);
    if (status != JNI_OK) {
        LWARN(27, "Failed to initialize system class loader.");
        if(exn_raised()) {
            print_uncaught_exception_message(stderr,
                "system class loader initialization", exn_get());
        }
        return status;
    }

    TRACE("system class loader initialized");

    set_current_thread_context_loader(jni_env);

    status = run_java_init(jni_env);
    if (status != JNI_OK) return status;

    TRACE("VM initialization completed");
    assert(!exn_raised());

    return JNI_OK;
}

JIT_Handle vm_load_jit(const char* file_name, apr_dso_handle_t** handle) {
        Dll_JIT* jit = new Dll_JIT(file_name);
        *handle = jit->get_lib_handle();
        if(!*handle) {
            delete jit;
            return NULL;
        }
        vm_add_jit(jit);
        return (JIT_Handle)jit;
}
