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

#define LOG_DOMAIN "vm.helpers"
#include "cxxlog.h"

#include <map>

#include "open/vm_class_manipulation.h"
#include "jit_runtime_support.h"
#include "Class.h"
#include "environment.h"
#include "exceptions.h"
#include "init.h"

#ifndef _WIN32
    #define strcmpi strcasecmp
#endif

struct JIT_RT_Function_Entry {
    VM_RT_SUPPORT  function;
    const char    *name;
    HELPER_INTERRUPTIBILITY_KIND i_kind;
    HELPER_CALLING_CONVENTION cc_kind;
    U_32           number_of_args;
    const char    *magic_class_name;
    const char    *magic_method_name;
    const char    *magic_method_descr;
    Method_Handle  magic_mh;
};

typedef std::map<VM_RT_SUPPORT, JIT_RT_Function_Entry*> HelperInfoMap;

static JIT_RT_Function_Entry _jit_rt_function_entries_base[] = {
    {VM_RT_NEW_RESOLVED_USING_VTABLE_AND_SIZE, "VM_RT_NEW_RESOLVED_USING_VTABLE_AND_SIZE",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              2,
            NULL,   NULL,   "(Lorg/vmmagic/unboxed/Address;)Lorg/vmmagic/unboxed/Address;",   NULL},
    {VM_RT_NEW_VECTOR_USING_VTABLE,            "VM_RT_NEW_VECTOR_USING_VTABLE",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              2,
            NULL,   NULL,   "(Lorg/vmmagic/unboxed/Address;I)Lorg/vmmagic/unboxed/Address;",   NULL},
    {VM_RT_MULTIANEWARRAY_RESOLVED,            "VM_RT_MULTIANEWARRAY_RESOLVED",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_MULTIARRAY,           8,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_LDC_STRING,                         "VM_RT_LDC_STRING",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              2,
            NULL,   NULL,   NULL,   NULL},

    {VM_RT_THROW,                              "VM_RT_THROW",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              1,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_THROW_LAZY,                         "VM_RT_THROW_LAZY",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_DRL,                  8,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_THROW_LINKING_EXCEPTION,            "VM_RT_THROW_LINKING_EXCEPTION",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              0,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_THROW_SET_STACK_TRACE,              "VM_RT_THROW_SET_STACK_TRACE",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              1,
            NULL,   NULL,   NULL,   NULL},

    {VM_RT_GET_IDENTITY_HASHCODE,              "VM_RT_GET_IDENTITY_HASHCODE",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              1,
            NULL,   NULL,   "(Ljava/lang/Object;)I",   NULL},


    {VM_RT_MONITOR_ENTER,                      "VM_RT_MONITOR_ENTER",
            INTERRUPTIBLE_SOMETIMES,           CALLING_CONVENTION_STDCALL,              1,
            "org/apache/harmony/drlvm/thread/ThreadHelper",   "monitorEnterUseReservation",
            "(Ljava/lang/Object;)V",   NULL},

    {VM_RT_MONITOR_EXIT,                       "VM_RT_MONITOR_EXIT",
            INTERRUPTIBLE_SOMETIMES,           CALLING_CONVENTION_STDCALL,              1,
            "org/apache/harmony/drlvm/thread/ThreadHelper",   "monitorExit",
            "(Ljava/lang/Object;)V",   NULL},

    {VM_RT_CLASS_2_JLC,                        "VM_RT_CLASS_2_JLC",
            INTERRUPTIBLE_NEVER,               CALLING_CONVENTION_STDCALL,              1,
            NULL,   NULL,   NULL,   NULL},

    {VM_RT_CHECKCAST,                          "VM_RT_CHECKCAST",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              2,
            "org/apache/harmony/drlvm/VMHelperFastPath",   "checkCast",
            "(Lorg/vmmagic/unboxed/Address;Ljava/lang/Object;)V",   NULL},

    {VM_RT_INSTANCEOF,                         "VM_RT_INSTANCEOF",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              2,
            "org/apache/harmony/drlvm/VMHelperFastPath",   "instanceOf",
            "(Lorg/vmmagic/unboxed/Address;Ljava/lang/Object;)Z",   NULL},

    {VM_RT_AASTORE,                            "VM_RT_AASTORE",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              3,
            NULL,   NULL,   NULL,   NULL},

    {VM_RT_AASTORE_TEST,                       "VM_RT_AASTORE_TEST",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              2,
            NULL,   NULL,   NULL,   NULL},

    {VM_RT_GET_INTERFACE_VTABLE_VER0,          "VM_RT_GET_INTERFACE_VTABLE_VER0",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              2,
            "org/apache/harmony/drlvm/VMHelperFastPath",   "getInterfaceVTable3",
            "(Lorg/vmmagic/unboxed/Address;Ljava/lang/Object;)Lorg/vmmagic/unboxed/Address;",   NULL},

    {VM_RT_INITIALIZE_CLASS,                   "VM_RT_INITIALIZE_CLASS",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              1,
            NULL,   NULL,   NULL,   NULL},

    {VM_RT_GC_HEAP_WRITE_REF,                  "VM_RT_GC_HEAP_WRITE_REF",
            INTERRUPTIBLE_NEVER,               CALLING_CONVENTION_CDECL,                3,
            NULL,   NULL,  "(Lorg/vmmagic/unboxed/Address;Lorg/vmmagic/unboxed/Address;Lorg/vmmagic/unboxed/Address;)V", NULL},
    {VM_RT_GC_SAFE_POINT,                      "VM_RT_GC_SAFE_POINT",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              0,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_GC_GET_TLS_BASE,                    "VM_RT_GET_TLS_BASE",
            INTERRUPTIBLE_NEVER,               CALLING_CONVENTION_STDCALL,              0,
            NULL,   NULL,   NULL,   NULL},

    {VM_RT_JVMTI_METHOD_ENTER_CALLBACK,        "VM_RT_JVMTI_METHOD_ENTER_CALLBACK",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              1,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_JVMTI_METHOD_EXIT_CALLBACK,         "VM_RT_JVMTI_METHOD_EXIT_CALLBACK",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              2,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_JVMTI_FIELD_ACCESS_CALLBACK,        "VM_RT_JVMTI_FIELD_ACCESS_CALLBACK",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              4,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_JVMTI_FIELD_MODIFICATION_CALLBACK,  "VM_RT_JVMTI_FIELD_MODIFICATION_CALLBACK",
            INTERRUPTIBLE_ALWAYS,              CALLING_CONVENTION_STDCALL,              5,
            NULL,   NULL,   NULL,   NULL},

    {VM_RT_NEWOBJ_WITHRESOLVE,                      "VM_RT_NEWOBJ_WITHRESOLVE",
            INTERRUPTIBLE_ALWAYS,                   CALLING_CONVENTION_STDCALL,         2,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_NEWARRAY_WITHRESOLVE,                    "VM_RT_NEWARRAY_WITHRESOLVE",
            INTERRUPTIBLE_ALWAYS,                   CALLING_CONVENTION_STDCALL,         3,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_GET_NONSTATIC_FIELD_OFFSET_WITHRESOLVE,  "VM_RT_GET_NONSTATIC_FIELD_OFFSET_WITHRESOLVE",
            INTERRUPTIBLE_ALWAYS,                   CALLING_CONVENTION_STDCALL,         2,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_GET_STATIC_FIELD_ADDR_WITHRESOLVE,       "VM_RT_GET_STATIC_FIELD_ADDR_WITHRESOLVE",
            INTERRUPTIBLE_ALWAYS,                   CALLING_CONVENTION_STDCALL,         2,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_CHECKCAST_WITHRESOLVE,                   "VM_RT_CHECKCAST_WITHRESOLVE",
            INTERRUPTIBLE_ALWAYS,                   CALLING_CONVENTION_STDCALL,         3,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_INSTANCEOF_WITHRESOLVE,                  "VM_RT_INSTANCEOF_WITHRESOLVE",
            INTERRUPTIBLE_ALWAYS,                   CALLING_CONVENTION_STDCALL,         3,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_GET_INVOKESTATIC_ADDR_WITHRESOLVE,       "VM_RT_GET_INVOKESTATIC_ADDR_WITHRESOLVE",
            INTERRUPTIBLE_ALWAYS,                   CALLING_CONVENTION_STDCALL,         2,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_GET_INVOKEINTERFACE_ADDR_WITHRESOLVE,    "VM_RT_GET_INVOKEINTERFACE_ADDR_WITHRESOLVE",
            INTERRUPTIBLE_ALWAYS,                   CALLING_CONVENTION_STDCALL,         3,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_GET_INVOKEVIRTUAL_ADDR_WITHRESOLVE,      "VM_RT_GET_INVOKEVIRTUAL_ADDR_WITHRESOLVE",
            INTERRUPTIBLE_ALWAYS,                   CALLING_CONVENTION_STDCALL,         3,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_GET_INVOKE_SPECIAL_ADDR_WITHRESOLVE,     "VM_RT_GET_INVOKE_SPECIAL_ADDR_WITHRESOLVE",
            INTERRUPTIBLE_ALWAYS,                   CALLING_CONVENTION_STDCALL,         2,
            NULL,   NULL,   NULL,   NULL},
    {VM_RT_INITIALIZE_CLASS_WITHRESOLVE,           "VM_RT_INITIALIZE_CLASS_WITHRESOLVE",
            INTERRUPTIBLE_ALWAYS,                   CALLING_CONVENTION_STDCALL,         2,
            NULL,   NULL,   NULL,   NULL},
};

static JIT_RT_Function_Entry *jit_rt_function_entries = &(_jit_rt_function_entries_base[0]);
static int num_jit_rt_function_entries = sizeof(_jit_rt_function_entries_base) / sizeof(_jit_rt_function_entries_base[0]);

static HelperInfoMap* init_helper_map() {
    // TODO: Use proper MM
    HelperInfoMap *map = new HelperInfoMap();
    for (int i = 0;  i < num_jit_rt_function_entries;  i++) {
        VM_RT_SUPPORT hid = jit_rt_function_entries[i].function;
        assert(map->find(hid) == map->end());
        map->insert(HelperInfoMap::value_type(hid, jit_rt_function_entries + i));
    }
    assert(map->size() == num_jit_rt_function_entries);
    return map;
}

static HelperInfoMap *helper_map = init_helper_map();

VMEXPORT 
const char* vm_helper_get_name(VM_RT_SUPPORT id) {
    HelperInfoMap::const_iterator it = helper_map->find(id);
    if (helper_map->end() != it) {
        assert(it->second);
        return it->second->name;
    } else {
        ASSERT(VM_RT_UNKNOWN == id, ("Unexpected helper id %d", id));
        return "unknown";
    }
}

VMEXPORT 
VM_RT_SUPPORT vm_helper_get_by_name(const char* name) {
    for (int i = 0;  i < num_jit_rt_function_entries;  i++) {
        if( !strcmpi(name, jit_rt_function_entries[i].name)) {
            return jit_rt_function_entries[i].function;
        }
    }
    return VM_RT_UNKNOWN;
}

VMEXPORT 
HELPER_INTERRUPTIBILITY_KIND vm_helper_get_interruptibility_kind(VM_RT_SUPPORT id)
{
    HelperInfoMap::const_iterator it = helper_map->find(id);
    ASSERT(helper_map->end() != it, ("Unexpected helper id %d", id));
    assert(it->second);
    return it->second->i_kind;
}

VMEXPORT 
HELPER_CALLING_CONVENTION vm_helper_get_calling_convention(VM_RT_SUPPORT id) 
{
    HelperInfoMap::const_iterator it = helper_map->find(id);
    ASSERT(helper_map->end() != it, ("Unexpected helper id %d", id));
    assert(it->second);
    return it->second->cc_kind;
}

U_32 vm_helper_get_numargs(VM_RT_SUPPORT id)
{
    HelperInfoMap::const_iterator it = helper_map->find(id);
    ASSERT(helper_map->end() != it, ("Unexpected helper id %d", id));
    assert(it->second);
    return it->second->number_of_args;
}

static Class* load_magic_helper_class(Global_Env * vm_env, const char * class_name) {
    return vm_env->LoadCoreClass(class_name);
}

static void init_magic_helper_class(Class* magic_helper_class){
    tmn_suspend_disable();
    class_initialize(magic_helper_class);

    if (exn_raised()){
        LDIE(63, "Exception raised while initializing helper class {0}" << magic_helper_class->get_name()->bytes);
    }
    tmn_suspend_enable();
}

static Method_Handle resolve_magic_helper(Global_Env * vm_env, 
                                   const char* class_name, 
                                   const char* method_name, 
                                   const char* method_descr) {
    assert (class_name);
    assert (method_name);
    assert (method_descr);

    Class* magic_helper_class = load_magic_helper_class(vm_env, class_name);
    init_magic_helper_class(magic_helper_class);
    return class_lookup_method_recursive(magic_helper_class, method_name, method_descr);
}

jint helper_magic_init(Global_Env * vm_env){
#ifdef _IPF_
    bool vmmagics_supported = false;
#else
    bool vmmagics_supported = true;
#endif
    if (!vmmagics_supported) { //skip vmmagic initialization if the feature is not supported
        return JNI_OK;
    }

    //init VMHelper class - utility class for all VMHelpers
    Class* magic_helper_class = load_magic_helper_class(vm_env, "org/apache/harmony/drlvm/VMHelper");
    init_magic_helper_class(magic_helper_class);
    
    //cache Method_Handle for all registered magic helpers
    //init their classes
    for (int i = 0;  i < num_jit_rt_function_entries;  i++) {
        const char* class_name = jit_rt_function_entries[i].magic_class_name;
        const char* method_name = jit_rt_function_entries[i].magic_method_name;
        const char* method_descr = jit_rt_function_entries[i].magic_method_descr;
    
        if (method_name == NULL){
            continue;
        }

        assert (class_name);
        assert (method_name);
        assert (method_descr);

        Method_Handle method_handle = resolve_magic_helper(vm_env, class_name, method_name, method_descr);
        ASSERT(method_handle, ("Method %s.%s%s not found.", class_name, method_name, method_descr));
        jit_rt_function_entries[i].magic_mh = method_handle;
    }
    return JNI_OK;
}

VMEXPORT
jint vm_helper_register_magic_helper(VM_RT_SUPPORT id, 
                          const char* class_name, 
                          const char* method_name) {
    assert (class_name);
    assert (method_name);
    
    HelperInfoMap::const_iterator it = helper_map->find(id);
    ASSERT(helper_map->end() != it, ("Unexpected helper id %d", id));
    assert(it->second);
    ASSERT(it->second->magic_method_name == NULL, ("Helper %d is registered already.", id));
    it->second->magic_class_name = class_name;
    it->second->magic_method_name = method_name;
    return JNI_OK;
}

VMEXPORT
Method_Handle vm_helper_get_magic_helper(VM_RT_SUPPORT id) {
    HelperInfoMap::const_iterator it = helper_map->find(id);
    ASSERT(helper_map->end() != it, ("Unexpected helper id %d", id));
    assert(it->second);
    return it->second->magic_mh;
}
