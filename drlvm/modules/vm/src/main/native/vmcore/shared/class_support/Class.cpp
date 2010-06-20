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
 * @author Pavel Pervov
 */  

#define LOG_DOMAIN "class"
#include "cxxlog.h"

#include <cctype>

#include <sstream>

#include "open/vm_class_manipulation.h"
#include "Class.h"
#include "classloader.h"
#include "environment.h"
#include "lock_manager.h"
#include "exceptions.h"
#include "compile.h"
#include "open/gc.h"
#include "nogc.h"
#include "vm_stats.h"
#include "jit_intf_cpp.h"
#include "type.h"
#include "cci.h"
#include "interpreter.h"
#include "port_threadunsafe.h"
#include "vtable.h"
#include "inline_info.h"

#ifdef _IPF_
#include "vm_ipf.h"
#endif // _IPF_

//
// private static variable containing the id of the next class
// access to this needs to be thread safe; also, this will have to
// change if we want to reuse class ids
//
// an id of 0 is reserved to mean null; therefore, ids start from 1
//
// ppervov: FIXME: usage of this variable is not currently thread safe
unsigned class_next_id = 1;

void Class::init_internals(const Global_Env* env, const String* name, ClassLoader* cl)
{
    memset(this, 0, sizeof(Class));

    m_super_class.name = NULL;
    m_name = name;
    m_simple_name = m_java_name = m_signature = NULL;

    m_allocated_size = 0;
    m_array_element_size = 0;

#ifdef POINTER64
    m_alignment = ((GC_OBJECT_ALIGNMENT<8)?8:GC_OBJECT_ALIGNMENT);;
#else
    m_alignment = GC_OBJECT_ALIGNMENT;
#endif

    m_id = class_next_id++;
    m_class_loader = cl;
    m_class_handle = NULL;
    m_is_primitive = 0;
    m_is_array = 0;
    m_is_array_of_primitives = 0;
    m_has_finalizer = 0;
    m_can_access_all = 0;
    m_is_fast_allocation_possible = 0;

    m_num_dimensions = 0;
    m_array_base_class = m_array_element_class = NULL;

    m_access_flags = 0;
    m_num_superinterfaces = 0;
    m_num_fields = m_num_static_fields = m_num_methods = 0;

    m_declaring_class_index = 0;
    m_enclosing_class_index = 0;
    m_enclosing_method_index = 0;
    m_num_innerclasses = 0;
    m_innerclasses = NULL;

    m_fields = NULL;
    m_methods = NULL;

    m_superinterfaces = NULL;

    m_const_pool.init();

    m_class_file_name = NULL;
    m_src_file_name = NULL;

    m_state = ST_Start;

    m_package = NULL;

    m_num_instance_refs = 0;

    m_num_virtual_method_entries = m_num_intfc_method_entries = 0;

    m_finalize_method = m_static_initializer = m_default_constructor = NULL;

    m_static_data_size = 0;
    m_static_data_block = NULL;

    m_unpadded_instance_data_size = m_instance_data_size = 0;
    m_vtable = NULL;
    m_allocation_handle = 0;

    m_vtable_descriptors = NULL;

    m_initializing_thread = NULL;

    m_num_class_init_checks = m_num_throws = m_num_instanceof_slow
        = m_num_allocations = m_num_bytes_allocated = 0;

    m_num_field_padding_bytes = 0;

    m_depth = 0;
    m_is_suitable_for_fast_instanceof = 0;

    m_cha_first_child = m_cha_next_sibling = NULL;

    m_sourceDebugExtension = NULL;
    m_lock = new Lock_Manager();
    m_verify_data = 0;
}

void Class::notify_unloading() {
    if(m_methods != NULL) {
        for (int i = 0; i < m_num_methods; i++){
            m_methods[i].NotifyUnloading();
        }
    }
}

void Class::clear_internals() {
    if(m_fields != NULL)
    {
        delete[] m_fields;
        m_fields = NULL;
    }
    if(m_methods != NULL) {
        for (int i = 0; i < m_num_methods; i++){
            m_methods[i].MethodClearInternals();
        }
        delete[] m_methods;
        m_methods = NULL;
    }
    m_const_pool.clear();
    if(m_vtable_descriptors)
        delete[] m_vtable_descriptors;

    if(m_lock)
        delete m_lock;
}


Field* Class::get_field(uint16 index) const
{
    assert(index < m_num_fields);
    return &(m_fields[index]);
}


Method* Class::get_method(uint16 index) const
{
    assert(index < m_num_methods);
    return &m_methods[index];
}


bool Class::is_instanceof(Class* clss)
{
    assert(!is_interface());

#ifdef VM_STATS
    UNSAFE_REGION_START
    VM_Statistics::get_vm_stats().num_type_checks++;
    if(this == clss)
        VM_Statistics::get_vm_stats().num_type_checks_equal_type++;
    if(clss->m_is_suitable_for_fast_instanceof)
        VM_Statistics::get_vm_stats().num_type_checks_fast_decision++;
    else if(clss->is_array())
        VM_Statistics::get_vm_stats().num_type_checks_super_is_array++;
    else if(clss->is_interface())
        VM_Statistics::get_vm_stats().num_type_checks_super_is_interface ++;
    else if((unsigned)clss->m_depth >= vm_max_fast_instanceof_depth())
        VM_Statistics::get_vm_stats().num_type_checks_super_is_too_deep++;
    UNSAFE_REGION_END
#endif // VM_STATS

    if(this == clss) return true;

    Global_Env* env = VM_Global_State::loader_env;

    if(is_array()) {
        Class* object_class = env->JavaLangObject_Class;
        assert(object_class != NULL);
        if(clss == object_class) return true;
        if(clss == env->java_io_Serializable_Class) return true;
        if(clss == env->java_lang_Cloneable_Class) return true;
        if(!clss->is_array()) return false;

        return class_is_subtype(get_array_element_class(), clss->get_array_element_class());
    } else {
        if(clss->m_is_suitable_for_fast_instanceof)
        {
            return m_vtable->superclasses[clss->m_depth - 1] == clss;
        }

        if(!clss->is_interface()) {
            for(Class *c = this; c; c = c->get_super_class()) {
                if(c == clss) return true;
            }
        } else {
            for(Class *c = this; c; c = c->get_super_class()) {
                unsigned n_intf = c->get_number_of_superinterfaces();
                for(unsigned i = 0; i < n_intf; i++) {
                    Class* intf = c->get_superinterface(i);
                    assert(intf);
                    assert(intf->is_interface());
                    if(class_is_subtype(intf, clss)) return true;
                }
            }
        }
    }

    return false;
}


bool Class::load_ancestors(Global_Env* env)
{
    m_state = ST_LoadingAncestors;

    const String* superName = get_super_class_name();

    if(superName == NULL) {
        if(env->InBootstrap() || get_name() != env->JavaLangClass_String) {
            // This class better be java.lang.Object
            if(get_name() != env->JavaLangObject_String) {
                // ClassFormatError
                std::stringstream ss;
                ss << get_name()->bytes
                    << ": class does not have superclass "
                    << "but the class is not java.lang.Object";
                REPORT_FAILED_CLASS_CLASS(m_class_loader, this,
                    "java/lang/ClassFormatError", ss.str().c_str());
                return false;
            }
        }
    } else {
        // Load super class
        Class* superClass;
        m_super_class.name = NULL;
        superClass = m_class_loader->LoadVerifyAndPrepareClass(env, superName);
        if(superClass == NULL) {
            assert(exn_raised());
            return false;
        }

        if(superClass->is_interface()) {
            REPORT_FAILED_CLASS_CLASS(m_class_loader, this,
                "java/lang/IncompatibleClassChangeError",
                "class " << m_name->bytes << " has interface "
                << superClass->get_name()->bytes << " as super class");
            return false;
        }
        if(superClass->is_final()) {
            REPORT_FAILED_CLASS_CLASS(m_class_loader, this,
                "java/lang/VerifyError",
                m_name->bytes << " cannot inherit from final class "
                << superClass->get_name()->bytes);
            return false;
        }

        // super class was successfully loaded
        m_super_class.clss = superClass;
        if(m_super_class.cp_index) {
            m_const_pool.resolve_entry(m_super_class.cp_index, superClass);
        }

        // if it's an interface, its superclass must be java/lang/Object
        if(is_interface()) {
            if((env->JavaLangObject_Class != NULL) && (superClass != env->JavaLangObject_Class)) {
                std::stringstream ss;
                ss << get_name()->bytes << ": interface superclass is not java.lang.Object";
                REPORT_FAILED_CLASS_CLASS(m_class_loader, this,
                    "java/lang/ClassFormatError", ss.str().c_str());
                return false;
            }
        }

        // Update the cha_first_child and cha_next_sibling fields.
        m_cha_first_child = NULL;
        if(has_super_class())
        {
            m_cha_next_sibling = get_super_class()->m_cha_first_child;
            get_super_class()->m_cha_first_child = this;
        }
    }

    //
    // load in super interfaces
    //
    for(unsigned i = 0; i < m_num_superinterfaces; i++ ) {
        const String* intfc_name = m_superinterfaces[i].name;
        Class* intfc = m_class_loader->LoadVerifyAndPrepareClass(env, intfc_name);
        if(intfc == NULL) {
            assert(exn_raised());
            return false;
        }
        if(!intfc->is_interface()) {
            REPORT_FAILED_CLASS_CLASS(m_class_loader, this,
                "java/lang/IncompatibleClassChangeError",
                get_name()->bytes << ": " << intfc->get_name()->bytes
                << " is not an interface");
            return false;
        }

        // superinterface was successfully loaded
        m_superinterfaces[i].clss = intfc;
        if(m_superinterfaces[i].cp_index != 0) {
            // there are no constant pool entries for array classes
            m_const_pool.resolve_entry(m_superinterfaces[i].cp_index, intfc);
        }
    }
    // class, superclass, and superinterfaces successfully loaded

    m_state = ST_Loaded;
    if(!is_array())
        m_package = m_class_loader->ProvidePackage(env, m_name, NULL);

    return true;
}


Class* Class::resolve_declaring_class(Global_Env* env)
{
    if(m_declaring_class_index == 0) return NULL;
    return _resolve_class(env, m_declaring_class_index);
}


void Class::setup_as_array(Global_Env* env, unsigned char num_dimensions,
    bool isArrayOfPrimitives, Class* baseClass, Class* elementClass)
{
    m_is_array = 1;
    m_num_dimensions = (unsigned char)num_dimensions;
    if(m_num_dimensions == 1) {
        m_is_array_of_primitives = isArrayOfPrimitives;
    } else {
        m_is_array_of_primitives = false;
    }
    m_array_element_class = elementClass;
    m_array_base_class = baseClass;
    m_state = ST_Initialized;

    assert(elementClass);

    // insert Java field, required by spec - 'length I'
    m_num_fields = 1;
    m_fields = new Field[1];
    m_fields[0].set(this, env->Length_String,
        env->string_pool.lookup("I"), ACC_PUBLIC|ACC_FINAL);
    m_fields[0].set_field_type_desc(
        type_desc_create_from_java_descriptor("I", NULL));
    m_fields[0].set_injected();

    m_super_class.name = env->JavaLangObject_String;
    m_super_class.cp_index = 0;

    m_access_flags = (ACC_FINAL | ACC_ABSTRACT);
    if(isArrayOfPrimitives) {
        m_access_flags |= ACC_PUBLIC;
    } else {
        // set array access flags the same as in its base class
        m_access_flags = (uint16)(m_access_flags
            | (baseClass->get_access_flags()
            & (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED)));
    }
    m_package = elementClass->m_package;

    // array classes implement two interfaces: Cloneable and Serializable
    m_superinterfaces = (Class_Super*) STD_MALLOC(2 * sizeof(Class_Super));
    m_superinterfaces[0].name = env->Clonable_String;
    m_superinterfaces[0].cp_index = 0;
    m_superinterfaces[1].name = env->Serializable_String;
    m_superinterfaces[1].cp_index = 0;
    m_num_superinterfaces = 2;
}


//
// This function doesn't check for fields inherited from superclasses.
//
Field* Class::lookup_field(const String* name, const String* desc)
{
    for(uint16 i = 0; i < m_num_fields; i++) {
        if(m_fields[i].get_name() == name && m_fields[i].get_descriptor() == desc)
            return &m_fields[i];
    }
    return NULL;
} // Class::lookup_field


Field* class_lookup_field_recursive(Class* clss,
                                    const char* name,
                                    const char* descr)
{
    String *field_name =
        VM_Global_State::loader_env->string_pool.lookup(name);
    String *field_descr =
        VM_Global_State::loader_env->string_pool.lookup(descr);

    return clss->lookup_field_recursive(field_name, field_descr);
}


Field* Class::lookup_field_recursive(const String* name, const String* desc)
{
    // Step 1: lookup in self
    Field* field = lookup_field(name, desc);
    if(field) return field;

    // Step 2: lookup in direct superinterfaces recursively
    for(uint16 in = 0; in < m_num_superinterfaces; in++) {
        field = get_superinterface(in)->lookup_field_recursive(name, desc);
        if(field) return field;
    }

    // Step 3: lookup in super classes recursively
    if(has_super_class()) {
        field = get_super_class()->lookup_field_recursive(name, desc);
    }

    return field;
} // Class::lookup_field_recursive


Method* Class::lookup_method(const String* name, const String* desc)
{
    for(unsigned i = 0; i < m_num_methods; i++) {
        if(m_methods[i].get_name() == name && m_methods[i].get_descriptor() == desc)
            return &m_methods[i];
    }
    return NULL;
} // Class::lookup_method


ManagedObject* Class::allocate_instance()
{
    assert(!hythread_is_suspend_enabled());
    //assert(is_initialized());
    ManagedObject* new_instance =
        (ManagedObject*)vm_alloc_and_report_ti(m_instance_data_size,
            m_allocation_handle, vm_get_gc_thread_local(), this);
    if(new_instance == NULL)
    {
        return NULL;
    }

#ifdef VM_STATS
    UNSAFE_REGION_START
    VM_Statistics::get_vm_stats().num_class_alloc_new_object++;
    instance_allocated(m_instance_data_size);
    UNSAFE_REGION_END
#endif //VM_STATS

    return new_instance;
}


void* Class::helper_get_interface_vtable(ManagedObject* obj, Class* iid)
{
    VTable* vt = obj->vt();
    Intfc_Table* intfTable  = vt->intfc_table;
    unsigned num_intfc = intfTable->n_entries;
#ifdef VM_STATS
    UNSAFE_REGION_START
    VM_Statistics::get_vm_stats().num_invokeinterface_calls++;
    switch(num_intfc) {
    case 1:  VM_Statistics::get_vm_stats().num_invokeinterface_calls_size_1++;    break;
    case 2:  VM_Statistics::get_vm_stats().num_invokeinterface_calls_size_2++;    break;
    default: VM_Statistics::get_vm_stats().num_invokeinterface_calls_size_many++; break;
    }
    if(num_intfc > VM_Statistics::get_vm_stats().invokeinterface_calls_size_max)
        VM_Statistics::get_vm_stats().invokeinterface_calls_size_max = num_intfc;
    UNSAFE_REGION_END
#endif
    for(unsigned i = 0; i < num_intfc; i++) {
        const Intfc_Table_Entry& intfEntry = intfTable->entry[i];
        Class* intfc = intfEntry.intfc_class;
        if(intfc == iid) {
#ifdef VM_STATS
            UNSAFE_REGION_START
            switch(i) {
            case 0:  VM_Statistics::get_vm_stats().num_invokeinterface_calls_searched_1++;    break;
            case 1:  VM_Statistics::get_vm_stats().num_invokeinterface_calls_searched_2++;    break;
            default: VM_Statistics::get_vm_stats().num_invokeinterface_calls_searched_many++; break;
            }
            if(i > VM_Statistics::get_vm_stats().invokeinterface_calls_searched_max)
                VM_Statistics::get_vm_stats().invokeinterface_calls_searched_max = i;
            UNSAFE_REGION_END
#endif
            unsigned char** table = intfEntry.table;
            return (void*)table;
        }
    }
    return NULL;
}


Method* class_lookup_method_recursive(Class *clss, const String* name, const String* desc)
{
    assert(clss);
    Method *m = 0;
    Class *oclss = clss;
    m = clss->lookup_method(name, desc);
    if(m)return m;

    for(clss = clss->get_super_class(); clss && !m; clss = clss->get_super_class()) {
        m = class_lookup_method_recursive(clss, name, desc);
    }
    if(m)return m;

    //if not found, search in interfaces, that means
    // clss itself is also interface
    for(int i = 0; i < oclss->get_number_of_superinterfaces(); i++)
        if((m = class_lookup_method_recursive(oclss->get_superinterface(i), name, desc)))
            return m;
    return NULL;
} //class_lookup_method_recursive



Method *class_lookup_method_recursive(Class *clss,
                                      const char *name,
                                      const char *descr)
{
    String *method_name =
        VM_Global_State::loader_env->string_pool.lookup(name);
    String *method_descr =
        VM_Global_State::loader_env->string_pool.lookup(descr);

    return class_lookup_method_recursive(clss, method_name, method_descr);
} //class_lookup_method_recursive


Method* class_lookup_method(Class* clss,
                            const char* name,
                            const char* descr)
{
    String* method_name =
        VM_Global_State::loader_env->string_pool.lookup(name);
    String* method_descr =
        VM_Global_State::loader_env->string_pool.lookup(descr);

    return clss->lookup_method(method_name, method_descr);
} // class_lookup_method

Method* class_get_method_from_vt_offset(VTable* vt,
                                        unsigned offset)
{
    assert(vt);
    unsigned index = (offset - VTABLE_OVERHEAD) / sizeof(void*);
    return vt->clss->get_method_from_vtable(index);
} // class_get_method_from_vt_offset

void* Field::get_address()
{
    assert(is_static());
    assert(is_offset_computed());
    return (char*)(get_class()->get_static_data_address()) + get_offset();
} // Field::get_address


unsigned Field::calculate_size() {
    static unsigned size = sizeof(Field) + sizeof(TypeDesc);
    return size;
}




Method::Method()
{
    //
    // _vtable_patch may be in one of three states:
    // 1. NULL -- before a vtable is initialized or after all patching is done.
    // 2. Points to a vtable entry which must be patched.  
    //    This state can be recognized because *_vtable_patch == _code
    // 3. Otherwise it points to a list containing multiple vtable patch info.
    //
    _vtable_patch = 0;

    _code = NULL;
    _registered_native_func = NULL;

    _state = ST_NotCompiled;
    _jits = NULL;
    _side_effects = MSE_Unknown;
    _method_sig = 0;

    _notify_recompiled_records = NULL;
    _recompilation_callbacks = NULL;
    _index = 0;
    _max_stack=_max_locals=_n_exceptions=_n_handlers=0;
    _exceptions = NULL;
    _byte_code_length = 0;
    _byte_codes = NULL;
    _handlers = NULL;
    _flags.is_init = 0;
    _flags.is_clinit = 0;
    _flags.is_overridden = 0;
    _flags.is_finalize = 0;
    _flags.is_nop = FALSE;

    _line_number_table = NULL;
    _local_vars_table = NULL;
    _num_param_annotations = 0;
    _num_invisible_param_annotations = 0;    
    _param_annotations = NULL;
    _invisible_param_annotations = NULL;    
    _default_value = NULL;

    pending_breakpoints = 0;
    _inline_info = NULL;
} //Method::Method


void Method::NotifyUnloading()
{
    if (_recompilation_callbacks != NULL) {
        MethodSet::const_iterator it;
        for (it = _recompilation_callbacks->begin(); it != _recompilation_callbacks->end(); it++)
        {
            (*it)->unregister_jit_recompiled_method_callbacks(this);
        }
    }
}

void Method::MethodClearInternals()
{
    CodeChunkInfo *jit_info;
    for (jit_info = _jits;  jit_info;  jit_info = jit_info->_next) {
        Boolean result = VM_Global_State::loader_env->em_interface->UnregisterCodeChunk(
            jit_info->get_code_block_addr());
        assert(TRUE == result);
        // ensure that jit_info was deleted
        assert (VM_Global_State::loader_env->em_interface->LookupCodeChunk(
                jit_info->get_code_block_addr(), FALSE, NULL, NULL, NULL) == NULL);

        for(unsigned k = 0; k < jit_info->_num_target_exception_handlers; k++) {
            delete jit_info->_target_exception_handlers[k];
            jit_info->_target_exception_handlers[k] = NULL;
        }
        jit_info->_target_exception_handlers = NULL;
    }
    
    if (_recompilation_callbacks != NULL) {
        delete _recompilation_callbacks;
    }

    if (_notify_recompiled_records != NULL)
    {
        Method_Change_Notification_Record *nr, *prev_nr;
        nr = _notify_recompiled_records;
        while(nr != NULL) {
            prev_nr = nr;
            nr = nr->next;
            STD_FREE(prev_nr);
        }
        _notify_recompiled_records = NULL;
    }   

    if (_line_number_table != NULL)
    {
        STD_FREE(_line_number_table);
        _line_number_table = NULL;
    }

    if (_byte_codes != NULL)
    {
        delete []_byte_codes;
        _byte_codes = NULL;
    }

    /*if (_local_vars_table != NULL)
    {
        STD_FREE(_local_vars_table);
        _local_vars_table = NULL;
    }*/

    if (_handlers != NULL)
    {
        delete []_handlers;
        _handlers = NULL;
    }

    if (_method_sig != 0)
    {
        _method_sig->reset();
        delete _method_sig;
    }

    if (_exceptions != NULL)
        delete []_exceptions;

    VTable_Patches *patch = NULL;
    while (_vtable_patch)
    {
        patch = _vtable_patch;
        _vtable_patch = _vtable_patch->next;
        STD_FREE(patch);
    }

    delete _inline_info;
}

void Method::lock()
{
    _class->lock();
}

void Method::unlock()
{
    _class->unlock();
}

void Method::add_inline_info_entry(Method* method, U_32 codeSize, void* codeAddr,
        U_32 mapLength, AddrLocation* addrLocationMap) {
    if (NULL == _inline_info) 
        _inline_info = new InlineInfo();

    _inline_info->add(method, codeSize, codeAddr, mapLength, 
            addrLocationMap);
}

void Method::send_inlined_method_load_events(Method *method) {
    if (NULL == _inline_info) 
        return;

    _inline_info->send_compiled_method_load_event(method);
}

////////////////////////////////////////////////////////////////////
// beginpointers between struct Class and java.lang.Class


// 20020419
// Given a struct Class, find its corresponding java.lang.Class instance.
//
// To split struct Class and java.lang.Class into two separate data
// structures, we have to replace all places in the VM source code where
// we map struct Class to java.lang.Class and vice versa by performing a cast.
// Here's an example from FindClass()
// --- begin old code
//        new_handle->java_reference = (ManagedObject *)clss;
// --- end old code
// --- begin new code
//        tmn_suspend_disable();       //---------------------------------v
//        new_handle->java_reference = struct_Class_to_java_lang_Class(clss);
//        tmn_suspend_enable();        //---------------------------------^
// --- end new code
// NB: in the past instances of java.lang.Class were guaranteed to be
// allocated in the fixed space.  Instances of java.lang.Class are now treated 
// by GC the same way as any other Java objects and the GC may choose to move them.
// This is the reason for disabling GC in the example above.
//
ManagedObject *struct_Class_to_java_lang_Class(Class *clss)
{
//sundr    printf("struct to class %s, %p, %p\n", clss->name->bytes, clss, clss->super_class);
    assert(!hythread_is_suspend_enabled());
    assert(clss);
    ManagedObject** hjlc = clss->get_class_handle();
    assert(hjlc);
    ManagedObject* jlc  = *hjlc;
    assert(jlc != NULL);
    assert(jlc->vt());
    assert(jlc->vt()->clss == VM_Global_State::loader_env->JavaLangClass_Class);
    assert(java_lang_Class_to_struct_Class(jlc) == clss);    // else clss's java.lang.Class had a bad "back" pointer
    return jlc;
} //struct_Class_to_java_lang_Class

/**
 * this function returns the reference to the Class->java_lang_Class
 *
 * @note The returned handle is unsafe in respect to DeleteLocalRef, 
 *       and should not be passed to user JNI functions.
 */
jobject struct_Class_to_java_lang_Class_Handle(Class *clss) {
    // used only for protecting assertions
  #ifndef NDEBUG
    tmn_suspend_disable_recursive();
  #endif
    assert(clss);
    assert(clss->get_class_handle());
    ManagedObject* UNUSED jlc = *(clss->get_class_handle());
    assert(jlc);
    assert(jlc->vt());
    //assert(jlc->vt()->clss == VM_Global_State::loader_env->JavaLangClass_Class);
  #ifndef NDEBUG
    // gc disabling was needed only to protect assertions
   tmn_suspend_enable_recursive();
  #endif
    // salikh 2005-04-11
    // this operation is safe because
    //    1 Class.java_lang_Class is enumerated during GC
    //    2 no access to pointer is being made, only slot address is taken
    // 
    // However, it would be unsafe to pass this local reference to JNI code,
    // as user may want to call DeleteLocalRef on it, and it would reset
    // java_lang_Class field of the struct Class
    // return (jclass)(&clss->java_lang_Class);
    //
    // ppervov 2005-04-18
    // redone struct Class to contain class handle instead of raw ManagedObject*
    return (jclass)(clss->get_class_handle());
}

/* The following two utility functions to ease
   conversion between struct Class and jclass
*/
jclass struct_Class_to_jclass(Class *c)
{
    assert(hythread_is_suspend_enabled());
    tmn_suspend_disable();  // ----------vvv
    ObjectHandle h = oh_allocate_local_handle();
    h->object = struct_Class_to_java_lang_Class(c);
    tmn_suspend_enable();   // ----------^^^
    return (jclass)h;
}

Class *jclass_to_struct_Class(jclass jc)
{
    Class *c;
    tmn_suspend_disable_recursive();
    c = java_lang_Class_to_struct_Class(jc->object);
    tmn_suspend_enable_recursive();
    return c;
}

Class *jobject_to_struct_Class(jobject jobj) 
{
    tmn_suspend_disable();
    assert(jobj->object);
    assert(jobj->object->vt());
    Class *clss = jobj->object->vt()->clss;
    assert(clss);
    tmn_suspend_enable();
    return clss;
}

// Given a class instance, find its corresponding struct Class.
Class *java_lang_Class_to_struct_Class(ManagedObject *jlc)
{
    assert(!hythread_is_suspend_enabled());
    assert(jlc != NULL);
    assert(jlc->vt());
    //assert(jlc->vt()->clss == VM_Global_State::loader_env->JavaLangClass_Class);

    assert(VM_Global_State::loader_env->vm_class_offset != 0);
    Class** vm_class_ptr = (Class**)(((U_8*)jlc) + VM_Global_State::loader_env->vm_class_offset);
    assert(vm_class_ptr != NULL);

    Class* clss = *vm_class_ptr;
    assert(clss != NULL);
    assert(clss->get_class_handle());
    assert(*(clss->get_class_handle()) == jlc);
    assert(clss != (Class*) jlc);           // else the two structures still overlap!
    return clss;
} //java_lang_Class_to_struct_Class


String* Class::get_simple_name()
{
    Global_Env* env = VM_Global_State::loader_env;
    if(m_simple_name == NULL) 
    {
        if (is_array()) 
        {
            String* simple_base_name = m_array_base_class->get_simple_name();
            unsigned len = simple_base_name->len;
            unsigned dims = m_num_dimensions;
            char * buf = (char*)STD_ALLOCA(dims * 2 + len);
            strcpy(buf, simple_base_name->bytes);
            while (dims-- > 0) {
                buf[len++] = '[';
                buf[len++] = ']';
            }
            m_simple_name = env->string_pool.lookup(buf, len);
        } 
        else 
        {
            const char* fn = m_name->bytes;
            const char* start;
            if(m_enclosing_class_index) 
            {
                const char* enclosing_name =
                    class_cp_get_class_name(this, m_enclosing_class_index);
                start = fn + strlen(enclosing_name);
                while (*start == '$' || isdigit(*start)) start++;
            } 
            else 
            {
                start = strrchr(fn, '/');
            }

            if(start) {
                m_simple_name = env->string_pool.lookup(start + 1);
            } else {
                m_simple_name = const_cast<String*>(m_name);
            }
        }
    }
    return m_simple_name;
}


String* class_name_get_java_name(const String* class_name) {
    unsigned len = class_name->len + 1;
    char* name = (char*)STD_ALLOCA(len);
    memcpy(name, class_name->bytes, len);
    for(char *p = name; *p; ++p) {
        if (*p=='/') *p='.';
    }
    String* str = VM_Global_State::loader_env->string_pool.lookup(name);
    return str;
}


// end pointers between struct Class and java.lang.Class
////////////////////////////////////////////////////////////////////


////////////////////////////////////////////////////////////////////
// begin Support for compressed and raw reference pointers

#ifndef REFS_USE_UNCOMPRESSED
bool is_compressed_reference(COMPRESSED_REFERENCE compressed_ref) 
{
    // A compressed reference is an offset into the heap.
    uint64 heap_max_size = (VM_Global_State::loader_env->heap_end
        - VM_Global_State::loader_env->heap_base);
    return ((uint64) compressed_ref) < heap_max_size;
} // is_compressed_reference



COMPRESSED_REFERENCE compress_reference(ManagedObject *obj) {
#ifdef REFS_USE_RUNTIME_SWITCH
    assert(VM_Global_State::loader_env->compress_references);
#endif // REFS_USE_RUNTIME_SWITCH
     COMPRESSED_REFERENCE compressed_ref;
     if(obj == NULL)
         compressed_ref = 0;
     else
         compressed_ref = (COMPRESSED_REFERENCE)((POINTER_SIZE_INT)obj
            - (POINTER_SIZE_INT)VM_Global_State::loader_env->heap_base);
    assert(is_compressed_reference(compressed_ref));
    return compressed_ref;
} //compress_reference



ManagedObject *uncompress_compressed_reference(COMPRESSED_REFERENCE compressed_ref) {
#ifdef REFS_USE_RUNTIME_SWITCH
    assert(VM_Global_State::loader_env->compress_references);
#endif // REFS_USE_RUNTIME_SWITCH
    assert(is_compressed_reference(compressed_ref));
    if (compressed_ref == 0) {
        return NULL;
    } else {
        return (ManagedObject *)(VM_Global_State::loader_env->heap_base + compressed_ref);
    }
} //uncompress_compressed_reference
#endif // REFS_USE_UNCOMPRESSED


// end Support for compressed and raw reference pointers
////////////////////////////////////////////////////////////////////

// Function registers a number of native methods to a given class.
bool
class_register_methods(Class_Handle klass,
                       const JNINativeMethod* methods,
                       int num_methods)
{
    // get class and string pool
    String_Pool &pool = VM_Global_State::loader_env->string_pool;
    for( int index = 0; index < num_methods; index++ ) {
        // look up strings in string pool
        const String *name = pool.lookup(methods[index].name);
        const String *desc = pool.lookup(methods[index].signature);

        // find method from class
        Method *class_method = NULL;
        bool found = false;

        for(int count = 0; count < klass->get_number_of_methods(); count++ ) {
            class_method = klass->get_method(count);

            if( class_method->get_name() == name && 
                    class_method->get_descriptor() == desc ) {
                // found method
                found = true;
                break;
            }
        }

        if (found) {
            TRACE2("class.native", "Register native method: "
                << klass->get_name()->bytes
                << "." << name->bytes << desc->bytes);

            // Calling callback for NativeMethodBind event
            NativeCodePtr native_addr = methods[index].fnPtr;

            jvmti_process_native_method_bind_event( (jmethodID) class_method, native_addr, &native_addr);

            if (! interpreter_enabled()) {
                NativeCodePtr stub = compile_create_lil_jni_stub(class_method, native_addr, NULL);
                if (!stub)
                    return true;

                class_method->lock();
                class_method->set_code_addr(stub);
                class_method->unlock();

                // the following lines were copy-pasted from compile_do_compilation() function
                // it is not obvious that they should be here.
                compile_flush_generated_code();
                //class_method->set_state(Method::ST_Compiled);
                //class_method->do_jit_recompiled_method_callbacks();

                class_method->apply_vtable_patches();
            }

            class_method->set_registered_native_func(native_addr);
        } else {
            // create error string "<class_name>.<method_name><method_descriptor>
            int clen = klass->get_name()->len;
            int mlen = name->len;
            int dlen = desc->len;
            int len = clen + 1 + mlen + dlen;
            char *error = (char*)STD_ALLOCA(len + 1);
            memcpy(error, klass->get_name()->bytes, clen);
            error[clen] = '.';
            memcpy(error + clen + 1, name->bytes, mlen);
            memcpy(error + clen + 1 + mlen, desc->bytes, dlen);
            error[len] = '\0';

            TRACE2("class.native", "Native could not be registered: "
                << klass->get_name()->bytes << "." << name->bytes << desc->bytes);

            // raise an exception
            jthrowable exc_object = exn_create("java/lang/NoSuchMethodError", error);
            exn_raise_object(exc_object);
            return true;
        }
    }
    return false;
} // class_register_methods

// Function unregisters registered native methods of a given class.
bool
class_unregister_methods(Class_Handle klass)
{
    // lock class
    klass->lock();
    for(int count = 0; count < klass->get_number_of_methods(); count++ ) {
        Method* method = klass->get_method(count);
        if (NULL != method->get_registered_native_func()) {
            // trace
            TRACE2("class.native", "Unregister native method: "
                << klass->get_name() << "." << method->get_name()->bytes
                << method->get_descriptor()->bytes);

            // reset registered_native_func
            method->set_registered_native_func(NULL);
        }
    }
    // unlock class
    klass->unlock();
    return false;
} // class_unregister_methods


////////////////////////////////////////////////////////////////////
// begin support for JIT notification when classes are extended

struct Class_Extended_Notification_Record {
    Class *class_of_interest;
    JIT   *jit;
    void  *callback_data;
    Class_Extended_Notification_Record *next;

    bool equals(Class *class_of_interest_, JIT *jit_, void *callback_data_) {
        if ((class_of_interest == class_of_interest_) &&
            (jit == jit_) &&
            (callback_data == callback_data_)) {
            return true;
        }
        return false;
    }
};

void Class::lock()
{
    m_lock->_lock();
}


void Class::unlock()
{
    m_lock->_unlock();
}


unsigned Class::calculate_size()
{
    unsigned size = 0;
    size += sizeof(Class);
    size += m_num_innerclasses*sizeof(InnerClass);
    size += sizeof(ConstantPool)
        + m_const_pool.get_size()*sizeof(ConstPoolEntry);
    for(unsigned i = 0; i < m_num_fields; i++) {
        size += m_fields[i].calculate_size();
    }
    for(unsigned i = 0; i < m_num_methods; i++) {
        size += m_methods[i].calculate_size();
    }
    size += m_num_superinterfaces*sizeof(Class_Super);
    size += m_static_data_size;
    if(!is_interface())
        size += sizeof(VTable);
    size += sizeof(Lock_Manager);

    return size;
}

void* Class::code_alloc(size_t size, size_t alignment, Code_Allocation_Action action)
{
    assert (m_class_loader);
    return m_class_loader->CodeAlloc(size, alignment, action);
}


