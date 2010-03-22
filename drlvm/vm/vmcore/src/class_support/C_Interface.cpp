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
#define LOG_DOMAIN "vm.core"
#include "cxxlog.h"

#include "open/vm_properties.h"
#include "open/vm_type_access.h"
#include "open/vm_field_access.h"
#include "open/vm_method_access.h"
#include "open/vm_class_loading.h"
#include "open/vm_class_manipulation.h"
#include "open/vm_class_info.h"
#include "open/vm_ee.h"

#include "classloader.h"
#include "class_interface.h"
#include "Package.h"
#include "vtable.h"

#include "vm_arrays.h"
#include "compile.h"
#include "port_mutex.h"
#include "cci.h"
#include "nogc.h"
#include "exceptions.h"

BOOLEAN class_is_final(Class_Handle cl) {
    assert(cl);
    return cl->is_final();
}

BOOLEAN class_is_abstract(Class_Handle cl) {
    assert(cl);
    return cl->is_abstract();
}

BOOLEAN class_is_interface(Class_Handle cl) {
    assert(cl);
    return cl->is_interface();
}


BOOLEAN class_is_array(Class_Handle cl) {
    assert(cl);
    return cl->is_array();
} //class_is_array


static unsigned countLeadingChars(const char* str, char c) {
    U_32 n=0;
    while (str[n]==c) {
        n++;
    }
    return n;
}
 
unsigned class_cp_get_num_array_dimensions(Class_Handle cl, unsigned short cpIndex) {
    ConstantPool& cp  = cl->get_constant_pool();
    unsigned char tag = cp.get_tag(cpIndex);
    char c = '[';
    switch (tag) {
        case CONSTANT_Class: {
            uint16 nameIdx = cp.get_class_name_index(cpIndex);
            const char* str = cp.get_utf8_string(nameIdx)->bytes;
            return countLeadingChars(str, c);
        }
        case CONSTANT_Fieldref: {
            const char* str = class_cp_get_entry_descriptor(cl, cpIndex);
            return countLeadingChars(str, c);
        }
        default:
            assert(0);
    }
    return 0;
}

const char* class_get_package_name(Class_Handle cl) {
    assert(cl);
    return cl->get_package()->get_name()->bytes;
}

BOOLEAN method_is_abstract(Method_Handle m) {
    assert(m);
    return m->is_abstract();
} // method_is_abstract



BOOLEAN field_is_static(Field_Handle f)
{
    assert(f);
    return f->is_static();
}



BOOLEAN field_is_final(Field_Handle f)
{
    assert(f);
    return f->is_final();
}


BOOLEAN field_is_volatile(Field_Handle f)
{
    assert(f);
    return f->is_volatile();
}



BOOLEAN field_is_private(Field_Handle f)
{
    assert(f);
    return f->is_private();
}



BOOLEAN field_is_public(Field_Handle f)
{
    assert(f);
    return f->is_public();
}



unsigned field_get_offset(Field_Handle f)
{
    assert(f);
    assert(!f->is_static());
    return f->get_offset();
} //field_get_offset



void* field_get_address(Field_Handle f)
{
    assert(f);
    assert(f->is_static());
    return f->get_address();
} // field_get_address



const char* field_get_name(Field_Handle f)
{
    assert(f);
    return f->get_name()->bytes;
}



const char* field_get_descriptor(Field_Handle f)
{
    assert(f);
    return f->get_descriptor()->bytes;
}



Java_Type field_get_type(Field_Handle f)
{
    assert(f);
    return f->get_java_type();
} // field_get_type



unsigned field_get_flags(Field_Handle f)
{
    assert(f);
    return f->get_access_flags();
}



Class_Handle field_get_class(Field_Handle f)
{
    assert(f);
    return f->get_class();
} //field_get_class

void field_get_track_access_flag(Field_Handle f, char** address,
                                 char* mask)
{
    return f->get_track_access_flag(address, mask);
}

void field_get_track_modification_flag(Field_Handle f, char** address,
                                 char* mask)
{
    return f->get_track_modification_flag(address, mask);
}

BOOLEAN method_is_static(Method_Handle m)
{
    assert(m);
    return m->is_static();
} // method_is_static


BOOLEAN method_is_final(Method_Handle m)
{
    assert(m);
    return m->is_final();
} // method_is_final


BOOLEAN method_is_synchronized(Method_Handle m)
{
    assert(m);
    return m->is_synchronized();
} // method_is_synchronized


BOOLEAN method_is_private(Method_Handle m)
{
    assert(m);
    return m->is_private();
} // method_is_private


BOOLEAN method_is_strict(Method_Handle m)
{
    assert(m);
    return m->is_strict();
} // method_is_strict


BOOLEAN method_is_native(Method_Handle m)
{
    assert(m);
    return m->is_native();
} // method_is_native


U_8* method_allocate_info_block(Method_Handle m, JIT_Handle j, size_t size)
{
    assert(m);
    return (U_8*)(m->allocate_jit_info_block(size, (JIT *)j));
} //method_allocate_info_block


U_8* method_allocate_jit_data_block(Method_Handle m, JIT_Handle j, size_t size, size_t alignment)
{
    assert(m);
    return (U_8*)(m->allocate_JIT_data_block(size, (JIT *)j, alignment));
} //method_allocate_jit_data_block


U_8* method_get_info_block_jit(Method_Handle m, JIT_Handle j)
{
    assert(m);
    CodeChunkInfo *jit_info = m->get_chunk_info_no_create_mt((JIT *)j, CodeChunkInfo::main_code_chunk_id);
    if (jit_info == NULL) {
        return NULL;
    }
    return (U_8*)jit_info->_jit_info_block;
} //method_get_info_block_jit




unsigned method_get_info_block_size_jit(Method_Handle m, JIT_Handle j)
{
    assert(m);
    CodeChunkInfo *jit_info = ((Method *)m)->get_chunk_info_no_create_mt((JIT *)j, CodeChunkInfo::main_code_chunk_id);
    return jit_info == 0 ? 0 : (unsigned) jit_info->_jit_info_block_size;
} //method_get_info_block_size_jit



U_8* method_allocate_data_block(Method_Handle m, JIT_Handle j, size_t size, size_t alignment)
{
    assert(m);
    return (U_8*)(m->allocate_rw_data_block(size, alignment, (JIT*)j));
} //method_allocate_data_block


U_8*
method_allocate_code_block(Method_Handle m,
                           JIT_Handle j,
                           size_t size,
                           size_t alignment,
                           CodeBlockHeat heat,
                           int id,
                           Code_Allocation_Action action)
{
    assert(m);

    JIT *jit = (JIT *)j;
    assert(jit);

    U_32 drlHeat;
    if (heat == CodeBlockHeatMin)
        drlHeat = CODE_BLOCK_HEAT_COLD;
    else if (heat == CodeBlockHeatMax)
        drlHeat = CODE_BLOCK_HEAT_MAX/2;
    else {
        assert (heat == CodeBlockHeatDefault);
        drlHeat = CODE_BLOCK_HEAT_DEFAULT;
    }

    // the following method is safe to call from multiple threads
    U_8* code_block = (U_8*)m->allocate_code_block_mt(size, alignment, jit, drlHeat, id, action);

    return code_block;
} // method_allocate_code_block


U_8* method_get_code_block_jit(Method_Handle m, JIT_Handle j)
{
    assert(m);
    return method_get_code_block_addr_jit_new(m, j, 0);
} //method_get_code_block_addr_jit



unsigned method_get_code_block_size_jit(Method_Handle m, JIT_Handle j)
{
    assert(m);
    return method_get_code_block_size_jit_new(m, j, 0);
} //method_get_code_block_size_jit



U_8* method_get_code_block_addr_jit_new(Method_Handle method,
                                         JIT_Handle j,
                                         int id)
{
    assert(method);
    CodeChunkInfo* jit_info = method->get_chunk_info_no_create_mt((JIT *)j, id);
    assert(jit_info);
    return (U_8*)jit_info->get_code_block_addr();
} //method_get_code_block_addr_jit_new



unsigned method_get_code_block_size_jit_new(Method_Handle method,
                                            JIT_Handle j,
                                            int id)
{
    assert(method);
    CodeChunkInfo *jit_info = ((Method *)method)->get_chunk_info_no_create_mt((JIT *)j, id);
    return jit_info == 0 ? 0 :(unsigned) jit_info->get_code_block_size();
} //method_get_code_block_size_jit_new


const U_8* method_get_bytecode(Method_Handle m)
{
    assert(m);
    return m->get_byte_code_addr();
}



U_32
method_get_bytecode_length(Method_Handle m)
{
    assert(m);
    return m->get_byte_code_size();
}



U_32 method_get_max_locals(Method_Handle m)
{
    assert(m);
    return m->get_max_locals();
}


uint16 method_get_max_stack(Method_Handle m)
{
    assert(m);
    return m->get_max_stack();
}


size_t  method_get_vtable_offset(Method_Handle m)
{
    assert(m);
    return m->get_offset();
}


void *method_get_indirect_address(Method_Handle m)
{
    assert(m);
    return m->get_indirect_address();
} //method_get_indirect_address

const char *
method_get_name(Method_Handle m)
{
    assert(m);
    return m->get_name()->bytes;
}



const char* method_get_descriptor(Method_Handle m)
{
    assert(m);
    return m->get_descriptor()->bytes;
}



Class_Handle method_get_class(Method_Handle m)
{
    assert(m);
    return m->get_class();
}

void method_lock(Method_Handle m)
{
    assert(m);
    return m->lock();
}

void method_unlock(Method_Handle m)
{
    assert(m);
    return m->unlock();
}


Java_Type method_get_return_type(Method_Handle m)
{
    assert(m);
    return m->get_return_java_type();
}



BOOLEAN method_is_overridden(Method_Handle m)
{
    assert(m);
    return m->is_overridden();
} // method_is_overridden


const char* class_get_name(Class_Handle cl)
{
    assert(cl);
    return cl->get_name()->bytes;
} //class_get_name

unsigned short class_get_version(Class_Handle klass)
{
    assert(klass);
    return klass->get_version();
} // class_get_version

unsigned class_get_flags(Class_Handle cl)
{
    assert(cl);
    return cl->get_access_flags();
} //class_get_flags


U_32 class_get_depth(Class_Handle cl)
{
    assert(cl);
    return cl->get_depth();
} //class_get_depth

BOOLEAN class_is_support_fast_instanceof(Class_Handle cl)
{
    assert(cl);
    return cl->get_fast_instanceof_flag();
} //class_get_depth


Class_Handle vtable_get_class(VTable_Handle vh) {
    return vh->clss;
} // vtable_get_class

BOOLEAN class_is_initialized(Class_Handle ch)
{
    assert(ch);
    return ch->is_initialized();
} //class_is_initialized

Class_Handle class_get_super_class(Class_Handle cl)
{
    assert(cl);
    return cl->get_super_class();
} // class_get_super_class

Class_Handle class_get_extended_class(Class_Handle klass, const char* super_name)
{
    assert(klass);
    assert(super_name);

    Global_Env* env = VM_Global_State::loader_env;
    String* pooled_name = env->string_pool.lookup(super_name);

    for(Class* clss = klass; clss; clss = clss->get_super_class()) {
        if(clss->get_name() == pooled_name) {
            // found class with given name
            return clss;
        }
    }
    return NULL;
} // class_get_extended_class

unsigned class_number_implements(Class_Handle ch)
{
    assert(ch);
    return ch->get_number_of_superinterfaces();
}

Class_Handle class_get_implements(Class_Handle ch, unsigned idx)
{
    assert(ch);
    return ch->get_superinterface(idx);
}


Class_Handle class_get_declaring_class(Class_Handle ch)
{
    return ch->resolve_declaring_class(VM_Global_State::loader_env);
}

unsigned class_number_inner_classes(Class_Handle ch)
{
    assert(ch);
    return ch->get_number_of_inner_classes();
}

Boolean class_is_inner_class_public(Class_Handle ch, unsigned idx)
{
    assert(ch);
    return (ch->get_inner_class_access_flags(idx) & ACC_PUBLIC) != 0;
}

Class_Handle class_get_inner_class(Class_Handle ch, unsigned idx)
{
    assert(ch);
    uint16 cp_index = ch->get_inner_class_index(idx);
    return ch->_resolve_class(VM_Global_State::loader_env, cp_index);
}

BOOLEAN class_is_instanceof(Class_Handle s, Class_Handle t)
{
    assert(s);
    assert(t);
    return class_is_subtype(s, t);
} // class_is_instanceof



unsigned class_get_array_element_size(Class_Handle ch) 
{
    assert(ch);
    return ch->get_array_element_size();
}

Class_Handle class_get_array_element_class(Class_Handle cl)
{
    assert(cl);
    return cl->get_array_element_class();
} //class_get_array_element_class



BOOLEAN class_is_throwable(Class_Handle ch)
{
    assert(ch);
    Global_Env *env = VM_Global_State::loader_env;
    Class *exc_base_clss = env->java_lang_Throwable_Class;
    while(ch != NULL) {
        if(ch == exc_base_clss) {
            return TRUE;
        }
        //sundr printf("class name before super: %s %p\n", ch->name->bytes, ch);
        ch = class_get_super_class(ch);
    }
    return FALSE;
} // class_is_throwable

Class_Handle class_get_class_of_primitive_type(VM_Data_Type typ)
{
    Global_Env *env = VM_Global_State::loader_env;
    Class *clss = NULL;
    switch(typ) {
    case VM_DATA_TYPE_INT8:
        clss = env->Byte_Class;
        break;
    case VM_DATA_TYPE_INT16:
        clss = env->Short_Class;
        break;
    case VM_DATA_TYPE_INT32:
        clss = env->Int_Class;
        break;
    case VM_DATA_TYPE_INT64:
        clss = env->Long_Class;
        break;
    case VM_DATA_TYPE_F8:
        clss = env->Double_Class;
        break;
    case VM_DATA_TYPE_F4:
        clss = env->Float_Class;
        break;
    case VM_DATA_TYPE_BOOLEAN:
        clss = env->Boolean_Class;
        break;
    case VM_DATA_TYPE_CHAR:
        clss = env->Char_Class;
        break;
    case VM_DATA_TYPE_VOID:
        clss = env->Void_Class;
        break;
    case VM_DATA_TYPE_INTPTR:
    case VM_DATA_TYPE_UINTPTR:
    case VM_DATA_TYPE_UINT8:
    case VM_DATA_TYPE_UINT16:
    case VM_DATA_TYPE_UINT32:
    case VM_DATA_TYPE_UINT64:
        clss = NULL;    // to allow star jit initialization
        break;
    default:
        LDIE(69, "Unknown vm data type");          // We need a better way to indicate an internal error
    }
    return clss;
} // class_get_class_of_primitive_type

VTable_Handle class_get_vtable(Class_Handle cl)
{
    assert(cl);
    return cl->get_vtable();
} //class_get_vtable


const char* class_get_source_file_name(Class_Handle cl)
{
    assert(cl);
    if(!cl->has_source_information())
        return NULL;
    return cl->get_source_file_name();
} // class_get_source_file_name


const char* class_cp_get_const_string(Class_Handle cl, U_16 index)
{
    assert(cl);
    return cl->get_constant_pool().get_string_chars(index);
} //class_get_const_string



// Returns the address where the interned version of the string is stored: this will be the address
// of a slot containing a Java_java_lang_String* or a U_32 compressed reference. Also interns the
// string so that the JIT can load a reference to the interned string without checking if it is null.
const void *class_get_const_string_intern_addr(Class_Handle cl, unsigned short index)
{
    assert(cl);
    Global_Env* env = VM_Global_State::loader_env;
    String* str = cl->get_constant_pool().get_string(index);
    assert(str);

    assert((void *)(&(str->intern.raw_ref)) == (void *)(&(str->intern.compressed_ref))); 
    return &(str->intern.raw_ref);

} //class_get_const_string_intern_addr


const char* class_cp_get_entry_descriptor(Class_Handle src_class, unsigned short index)
{
    ConstantPool& cp = src_class->get_constant_pool();
    if(!(cp.is_fieldref(index)
        || cp.is_methodref(index)
        || cp.is_interfacemethodref(index)))
    {
        return NULL;
    }

    index = cp.get_ref_name_and_type_index(index);
    index = cp.get_name_and_type_descriptor_index(index);
    return cp.get_utf8_chars(index);
} // class_cp_get_entry_descriptor


VM_Data_Type class_cp_get_field_type(Class_Handle src_class, U_16 cp_index)
{
    assert(src_class->get_constant_pool().is_fieldref(cp_index));

    char class_id = (class_cp_get_entry_descriptor(src_class, cp_index))[0];
    switch(class_id)
    {
    case VM_DATA_TYPE_BOOLEAN:
    case VM_DATA_TYPE_CHAR:
    case VM_DATA_TYPE_INT8:
    case VM_DATA_TYPE_INT16:
    case VM_DATA_TYPE_INT32:
    case VM_DATA_TYPE_INT64:
    case VM_DATA_TYPE_F4:
    case VM_DATA_TYPE_F8:
        return (VM_Data_Type)class_id;
    case VM_DATA_TYPE_ARRAY:
    case VM_DATA_TYPE_CLASS:
        return VM_DATA_TYPE_CLASS;
    default:
        LDIE(69, "Unknown vm data type");
    }
    return VM_DATA_TYPE_INVALID;
} // class_cp_get_field_type


VM_Data_Type class_cp_get_const_type(Class_Handle cl, U_16 index)
{
    assert(cl);
    Java_Type jt = JAVA_TYPE_INVALID;
    ConstantPool& cp = cl->get_constant_pool();
    switch(cp.get_tag(index)) {
    case CONSTANT_String:
        jt = JAVA_TYPE_STRING;
        break;
    case CONSTANT_Integer:
        jt = JAVA_TYPE_INT;
        break;
    case CONSTANT_Float:
        jt = JAVA_TYPE_FLOAT;
        break;
    case CONSTANT_Long:
        jt = JAVA_TYPE_LONG;
        break;
    case CONSTANT_Double:
        jt = JAVA_TYPE_DOUBLE;
        break;
    case CONSTANT_Class:
        jt = JAVA_TYPE_CLASS;
        break;
    case CONSTANT_UnusedEntry:
        if(cp.get_tag(index - 1) == CONSTANT_Double) {
            jt = JAVA_TYPE_DOUBLE;
            break;
        } else if(cp.get_tag(index - 1) == CONSTANT_Long) {
            jt = JAVA_TYPE_LONG;
            break;
        }
    default:
        LDIE(5, "non-constant type is requested from constant pool : {0}" << cp.get_tag(index));
    }

    return (VM_Data_Type)jt;
} //class_cp_get_const_type



const void *class_cp_get_const_addr(Class_Handle cl, U_16 index)
{
    assert(cl);
    return cl->get_constant_pool().get_address_of_constant(index);
} //class_cp_get_const_addr


void* method_get_native_func_addr(Method_Handle method) {
    return (void*)classloader_find_native(method);
}

Arg_List_Iterator method_get_argument_list(Method_Handle m)
{
    assert(m);
    return (Arg_List_Iterator)((Method *)m)->get_argument_list();
}

Class_Handle resolve_class_from_constant_pool(Class_Handle c_handle, unsigned index)
{
    assert(c_handle);
    return c_handle->_resolve_class(VM_Global_State::loader_env, index);
}


Field_Handle class_resolve_nonstatic_field(Class_Handle clss, unsigned short cp_index)
{
    assert(clss);
    Field* f = clss->_resolve_field(VM_Global_State::loader_env, cp_index);
    return (!f || f->is_static()) ? NULL : f;
} // class_resolve_nonstatic_field


Class_Loader_Handle
class_get_class_loader(Class_Handle ch)
{
    assert(ch);
    return ch->get_class_loader();
} //class_get_class_loader

Class_Handle
vm_load_class_with_bootstrap(const char *name)
{
    Global_Env *env = VM_Global_State::loader_env;
    String *n = env->string_pool.lookup(name);
    return env->bootstrap_class_loader->LoadClass(env, n);
} 

Class_Handle
vm_lookup_class_with_bootstrap(const char* name)
{
    Global_Env *env = VM_Global_State::loader_env;
    String *n = env->string_pool.lookup(name);
    Class *clss = env->bootstrap_class_loader->LookupClass(n);
    return (Class_Handle)clss;
} 



Class_Handle
class_load_class_by_descriptor(const char *descr,
                               Class_Handle ch)
{
    assert(ch);
    Global_Env *env = VM_Global_State::loader_env;
    String *n;
    switch(descr[0]) {
    case '[':
        n = env->string_pool.lookup(descr);
        break;
    case 'L':
        {
            int len = (int) strlen(descr);
            n = env->string_pool.lookup(descr + 1, len - 2);
        }
        break;
    case 'B':
        return env->Byte_Class;
    case 'C':
        return env->Char_Class;
    case 'D':
        return env->Double_Class;
    case 'F':
        return env->Float_Class;
    case 'I':
        return env->Int_Class;
    case 'J':
        return env->Long_Class;
    case 'S':
        return env->Short_Class;
    case 'Z':
        return env->Boolean_Class;
    default:
        n = 0;
    }
    assert(n);
    return ch->get_class_loader()->LoadClass(env, n);;
} //class_load_class_by_descriptor

//
// The following do not cause constant pools to be resolve, if they are not
// resolved already
//
const char* class_cp_get_entry_name(Class_Handle cl, U_16 index)
{
    assert(cl);
    ConstantPool& const_pool = cl->get_constant_pool();
    if (!(const_pool.is_fieldref(index)
        || const_pool.is_methodref(index)
        || const_pool.is_interfacemethodref(index)))
    {
        LDIE(70, "Wrong index");
        return 0;
    }
    index = const_pool.get_ref_name_and_type_index(index);
    index = const_pool.get_name_and_type_name_index(index);
    return const_pool.get_utf8_chars(index);
} // class_cp_get_entry_name

const char* class_cp_get_entry_class_name(Class_Handle cl, unsigned short index)
{
    assert(cl);
    ConstantPool& const_pool = cl->get_constant_pool();
    if (!(const_pool.is_fieldref(index)
        || const_pool.is_methodref(index)
        || const_pool.is_interfacemethodref(index)))
    {
        LDIE(70, "Wrong index");
        return 0;
    }
    index = const_pool.get_ref_class_index(index);
    return class_cp_get_class_name(cl,index);
} // class_cp_get_entry_class_name

const char* class_cp_get_class_name(Class_Handle cl, unsigned short index)
{
    assert(cl);
    ConstantPool& const_pool = cl->get_constant_pool();
    if (!const_pool.is_class(index)) {
        LDIE(70, "Wrong index");
        return 0;
    }
    return const_pool.get_utf8_chars(const_pool.get_class_name_index(index));
} // class_cp_get_class_name


Class_Handle method_get_throws(Method_Handle mh, unsigned idx)
{
    assert(hythread_is_suspend_enabled());
    assert(mh);
    Method* m = (Method*)mh;
    String* exn_name = m->get_exception_name(idx);
    if (!exn_name) return NULL;
    ClassLoader* class_loader = m->get_class()->get_class_loader();
    Class *c =
        class_loader->LoadVerifyAndPrepareClass(VM_Global_State::loader_env, exn_name);
    // if loading failed - exception should be raised
    assert((!c && exn_raised()) || (c && !exn_raised()));
    return c;
}


uint16 method_get_exc_handler_number(Method_Handle m)
{
    assert(m);
    return m->num_bc_exception_handlers();
} // method_get_exc_handler_number


void method_get_exc_handler_info(Method_Handle m,
                                 uint16 handler_id,
                                 uint16* begin_offset,
                                 uint16* end_offset,
                                 uint16* handler_offset,
                                 uint16* handler_cpindex)
{
    assert(m);
    Handler* h = m->get_bc_exception_handler_info(handler_id);
    *begin_offset    = h->get_start_pc();
    *end_offset      = h->get_end_pc();
    *handler_offset  = h->get_handler_pc();
    *handler_cpindex = h->get_catch_type_index();
}



void method_set_num_target_handlers(Method_Handle m,
                                    JIT_Handle j,
                                    unsigned num_handlers)
{
    assert(m);
    ((Method *)m)->set_num_target_exception_handlers((JIT *)j, num_handlers);
} //method_set_num_target_handlers



void method_set_target_handler_info(Method_Handle m,
                                    JIT_Handle    j,
                                    unsigned      eh_number,
                                    void         *start_ip,
                                    void         *end_ip,
                                    void         *handler_ip,
                                    Class_Handle  catch_cl,
                                    Boolean       exc_obj_is_dead)
{
    assert(m);
    ((Method *)m)->set_target_exception_handler_info((JIT *)j,
                                                     eh_number,
                                                     start_ip,
                                                     end_ip,
                                                     handler_ip,
                                                     (Class *)catch_cl,
                                                     (exc_obj_is_dead == TRUE));
} //method_set_target_handler_info

Method_Side_Effects method_get_side_effects(Method_Handle m)
{
    assert(m);
    return ((Method *)m)->get_side_effects();
} //method_get_side_effects



void method_set_side_effects(Method_Handle m, Method_Side_Effects mse)
{
    assert(m);
    ((Method *)m)->set_side_effects(mse);
} //method_set_side_effects



Method_Handle class_lookup_method_recursively(Class_Handle clss,
                                              const char *name,
                                              const char *descr)
{
    assert(clss);
    return (Method_Handle) class_lookup_method_recursive((Class *)clss, name, descr);
} //class_lookup_method_recursively


size_t object_get_vtable_offset()
{
    return 0;
} //object_get_vtable_offset

Class_Handle vm_get_system_object_class()
{
    Global_Env *env = VM_Global_State::loader_env;
    return env->JavaLangObject_Class;
} // vm_get_system_object_class



Class_Handle vm_get_system_class_class()
{
    Global_Env *env = VM_Global_State::loader_env;
    return env->JavaLangClass_Class;
} // vm_get_system_class_class



Class_Handle vm_get_system_string_class()
{
    Global_Env *env = VM_Global_State::loader_env;
    return env->JavaLangString_Class;
} // vm_get_system_string_class



BOOLEAN field_is_literal(Field_Handle fh)
{
    assert(fh);
    Field *f = (Field *)fh;
    return f->get_const_value_index() != 0;
} //field_is_literal


int vector_first_element_offset_unboxed(Class_Handle element_type)
{
    assert(element_type);
    Class *clss = (Class *)element_type;
    int offset = 0;
    if(clss->is_primitive()) {
        Global_Env *env = VM_Global_State::loader_env;
        if(clss == env->Double_Class) {
            offset = VM_VECTOR_FIRST_ELEM_OFFSET_8;
        } else if(clss == env->Long_Class) {
            offset = VM_VECTOR_FIRST_ELEM_OFFSET_8;
        } else {
            offset = VM_VECTOR_FIRST_ELEM_OFFSET_1_2_4;
        }
    } else {
        offset = VM_VECTOR_FIRST_ELEM_OFFSET_REF;
    }
    assert(offset);
    return offset;
} //vector_first_element_offset_unboxed


int vector_first_element_offset(VM_Data_Type element_type)
{
    switch(element_type) {
    case VM_DATA_TYPE_CLASS:
        return VM_VECTOR_FIRST_ELEM_OFFSET_REF;
    default:
        {
            Class_Handle elem_class = class_get_class_of_primitive_type(element_type);
            return vector_first_element_offset_unboxed(elem_class);
        }
    }
} //vector_first_element_offset




int vector_first_element_offset_class_handle(Class_Handle element_type)
{
    return vector_first_element_offset_unboxed(element_type);
    //return VM_VECTOR_FIRST_ELEM_OFFSET_REF;
}

Boolean method_is_java(Method_Handle mh)
{
    assert(mh);
    return TRUE;
} //method_is_java


BOOLEAN class_is_enum(Class_Handle ch)
{
    assert(ch);
    return ch->is_enum() ? TRUE : FALSE;
} // class_is_enum

static Class *class_get_array_of_primitive_type(VM_Data_Type typ)
{
    Global_Env *env = VM_Global_State::loader_env;
    Class *clss = NULL;
    switch(typ) {
    case VM_DATA_TYPE_INT8:
        clss = env->ArrayOfByte_Class;
        break;
    case VM_DATA_TYPE_INT16:
        clss = env->ArrayOfShort_Class;
        break;
    case VM_DATA_TYPE_INT32:
        clss = env->ArrayOfInt_Class;
        break;
    case VM_DATA_TYPE_INT64:
        clss = env->ArrayOfLong_Class;
        break;
    case VM_DATA_TYPE_F8:
        clss = env->ArrayOfDouble_Class;
        break;
    case VM_DATA_TYPE_F4:
        clss = env->ArrayOfFloat_Class;
        break;
    case VM_DATA_TYPE_BOOLEAN:
        clss = env->ArrayOfBoolean_Class;
        break;
    case VM_DATA_TYPE_CHAR:
        clss = env->ArrayOfChar_Class;
        break;
    case VM_DATA_TYPE_UINT8:
    case VM_DATA_TYPE_UINT16:
    case VM_DATA_TYPE_UINT32:
    case VM_DATA_TYPE_UINT64:
    case VM_DATA_TYPE_INTPTR:
    case VM_DATA_TYPE_UINTPTR:
    default:
        DIE(("Unexpected vm data type"));          // We need a better way to indicate an internal error
        break;
    }
    return clss;
} // class_get_array_of_primitive_type

BOOLEAN class_is_primitive(Class_Handle ch)
{
    assert(ch);
    return ch->is_primitive();
} // class_is_primitive

VM_Data_Type class_get_primitive_type_of_class(Class_Handle ch)
{
    assert(ch);
    Global_Env *env = VM_Global_State::loader_env;
    if (ch == env->Boolean_Class)
        return VM_DATA_TYPE_BOOLEAN;
    if (ch == env->Char_Class)
        return VM_DATA_TYPE_CHAR;
    if (ch == env->Byte_Class)
        return VM_DATA_TYPE_INT8;
    if (ch == env->Short_Class)
        return VM_DATA_TYPE_INT16;
    if (ch == env->Int_Class)
        return VM_DATA_TYPE_INT32;
    if (ch == env->Long_Class)
        return VM_DATA_TYPE_INT64;
    if (ch == env->Float_Class)
        return VM_DATA_TYPE_F4;
    if (ch == env->Double_Class)
        return VM_DATA_TYPE_F8;

    return VM_DATA_TYPE_CLASS;
} // class_get_primitive_type_of_class


// Returns the number of arguments defined for the method.
// This number includes the this pointer (if present).
unsigned char method_args_get_number(Method_Signature_Handle mh)
{
    assert(mh);
    assert(!mh->sig);
    return mh->method->get_num_args();
} // method_args_get_number


// 20020303 At the moment we resolve the return type eagerly, but
// arguments are resolved lazily.  We should rewrite return type resolution
// to be lazy too.
void Method_Signature::initialize_from_java_method(Method *meth)
{
    assert(meth);
    num_args = meth->get_num_args();

    ClassLoader* cl = meth->get_class()->get_class_loader();
    const char* d = meth->get_descriptor()->bytes;
    assert(d[0]=='(');
    d++;
    arg_type_descs = (TypeDesc**)STD_MALLOC(sizeof(TypeDesc*)*num_args);
    assert(arg_type_descs);
    unsigned cur = 0;
    if (!meth->is_static()) {
        arg_type_descs[cur] = type_desc_create_from_java_class(meth->get_class());
        assert(arg_type_descs[cur]);
        cur++;
    }
    while (d[0]!=')') {
        assert(cur<num_args);
        arg_type_descs[cur] = type_desc_create_from_java_descriptor(d, cl);
        assert(arg_type_descs[cur]);
        cur++;
        // Advance d to next argument
        while (d[0]=='[') d++;
        d++;
        if (d[-1]=='L') {
            while(d[0]!=';') d++;
            d++;
        }
    }
    assert(cur==num_args);
    d++;
    return_type_desc = type_desc_create_from_java_descriptor(d, cl);
    assert(return_type_desc);
} //Method_Signature::initialize_from_java_method



void Method_Signature::reset()
{
    return_type_desc = 0;
    if (arg_type_descs)
        STD_FREE(arg_type_descs);

    num_args         = 0;
    arg_type_descs   = 0;
    method           = 0;
    sig              = 0;
} //Method_Signature::reset

void Method_Signature::initialize_from_method(Method *meth)
{
    assert(meth);
    reset();
    method      = meth;
    initialize_from_java_method(meth);
} //Method_Signature::initialize_from_method



Method_Signature_Handle method_get_signature(Method_Handle mh)
{
    assert(mh);
    Method *m = (Method *)mh;
    Method_Signature *ms = m->get_method_sig();
    if(!ms) {
        ms = new Method_Signature();

        if (ms == NULL) {
            exn_raise_object(VM_Global_State::loader_env->java_lang_OutOfMemoryError);
            return NULL;
        }

        assert(ms);
        ms->initialize_from_method(m);
        m->set_method_sig(ms);
    }
    return ms;
} // method_get_signature


U_16 class_number_fields(Class_Handle ch)
{
    assert(ch);
    return ch->get_number_of_fields();
}

unsigned class_num_instance_fields(Class_Handle ch)
{
    assert(ch);
    return ch->get_number_of_fields() - ch->get_number_of_static_fields();
} //class_num_instance_fields



unsigned class_num_instance_fields_recursive(Class_Handle ch)
{
    assert(ch);
    unsigned num_inst_fields = 0;
    while(ch) {
        num_inst_fields += class_num_instance_fields(ch);
        ch = class_get_super_class(ch);
    }
    return num_inst_fields;
} // class_num_instance_fields_recursive


Field_Handle class_get_field(Class_Handle ch, U_16 idx)
{
    assert(ch);
    if(idx >= ch->get_number_of_fields()) return NULL;
    return ch->get_field(idx);
} // class_get_field

Method_Handle class_get_method_by_name(Class_Handle ch, const char* name)
{
    assert(ch);
    for(int idx = 0; idx < ch->get_number_of_methods(); idx++) {
        Method_Handle meth = ch->get_method(idx);
        if(strcmp(meth->get_name()->bytes, name) == 0) {
            return meth;
        }
    }
    return NULL;
} // class_get_method_by_name

Field_Handle class_get_field_by_name(Class_Handle ch, const char* name)
{
    assert(ch);
    for(int idx = 0; idx < ch->get_number_of_fields(); idx++) {
        Field_Handle fld = ch->get_field(idx);
        if(strcmp(fld->get_name()->bytes, name) == 0) {
            return fld;
        }
    }
    return NULL;
} // class_get_field_by_name

Field_Handle class_get_instance_field(Class_Handle ch, unsigned idx)
{
    assert(ch);
    return ch->get_field(ch->get_number_of_static_fields() + idx);
} // class_get_instance_field



Field_Handle class_get_instance_field_recursive(Class_Handle ch, unsigned idx)
{
    assert(ch);
    unsigned num_fields_recursive = class_num_instance_fields_recursive(ch);
    assert(idx < num_fields_recursive);
    while(ch) {
        unsigned num_fields = class_num_instance_fields(ch);
        unsigned num_inherited_fields = num_fields_recursive - num_fields;
        if(idx >= num_inherited_fields) {
            Field_Handle fh = class_get_instance_field(ch, idx - num_inherited_fields);
            return fh;
        }
        num_fields_recursive = num_inherited_fields;
        ch = class_get_super_class(ch);
    }
    return 0;
} // class_get_instance_field_recursive


unsigned class_get_number_methods(Class_Handle ch)
{
    assert(ch);
    return ch->get_number_of_methods();
} // class_get_number_methods


// -gc magic needs this to do the recursive load.
Class_Handle field_get_class_of_field_type(Field_Handle fh)
{
    assert(hythread_is_suspend_enabled());
    assert(fh);
    Class_Handle ch = class_load_class_by_descriptor(field_get_descriptor(fh),
                                          field_get_class(fh));
    if(!ch->verify(VM_Global_State::loader_env))
        return NULL;
    if(!ch->prepare(VM_Global_State::loader_env))
        return NULL;
    return ch;
} // field_get_class_of_field_type


BOOLEAN field_is_reference(Field_Handle fh)
{
    assert((Field *)fh);
    Java_Type typ = fh->get_java_type();
    return (typ == JAVA_TYPE_CLASS || typ == JAVA_TYPE_ARRAY);
} //field_is_reference

BOOLEAN field_is_magic(Field_Handle fh)
{
    assert((Field *)fh);
    
    return fh->is_magic_type();
} //field_is_magic


Boolean field_is_enumerable_reference(Field_Handle fh)
{
    assert((Field *)fh);
    return ((field_is_reference(fh) && !field_is_magic(fh)));
} //field_is_enumerable_reference


BOOLEAN field_is_injected(Field_Handle f)
{
    assert(f);
    return f->is_injected();
} //field_is_injected


/////////////////////////////////////////////////////////////////////
// New GC interface demo

// This is just for the purposes of the demo.  The GC is free to define
// whatever it wants in this structure, as long as it doesn't exceed the
// size limit.  What should the size limit be?

struct GC_VTable {
    Class_Handle ch;  // for debugging
    U_32 num_ref_fields;
    U_32 *ref_fields_offsets;
    unsigned is_array : 1;
    unsigned is_primitive : 1;
};


#define VERBOSE_GC_CLASS_PREPARED 0

/////////////////////////////////////////////////////////////////////
//  New signature stuff

Type_Info_Handle method_args_get_type_info(Method_Signature_Handle msh,
                                           unsigned idx)
{
    assert(msh);
    Method_Signature *ms = (Method_Signature *)msh;
    if(idx >= ms->num_args) {
        LDIE(70, "Wrong index");
        return 0;
    }
    assert(ms->arg_type_descs);
    return ms->arg_type_descs[idx];
} //method_args_get_type_info



Type_Info_Handle method_ret_type_get_type_info(Method_Signature_Handle msh)
{
    assert(msh);
    Method_Signature *ms = (Method_Signature *)msh;
    return ms->return_type_desc;
} //method_ret_type_get_type_info

Type_Info_Handle field_get_type_info(Field_Handle fh)
{
    assert(fh);
    Field *field = (Field *)fh;
    TypeDesc* td = field->get_field_type_desc();
    assert(td);
    return td;
} // field_get_type_info


/////////////////////////////////////////////////////
// New GC stuff

BOOLEAN class_is_non_ref_array(Class_Handle ch)
{
    assert(ch);
    // Use the if statement to normalize the value of TRUE
    if((ch->get_vtable()->class_properties & CL_PROP_NON_REF_ARRAY_MASK) != 0)
    {
        assert(ch->is_array());
        return TRUE;
    } else {
        return FALSE;
    }
} // class_is_non_ref_array

BOOLEAN class_is_finalizable(Class_Handle ch)
{
    assert(ch);
    assert(ch->has_finalizer() == 
        ((ch->get_vtable()->class_properties & CL_PROP_FINALIZABLE_MASK) != 0));
    return ch->has_finalizer() ? TRUE : FALSE;
} // class_is_finalizable

WeakReferenceType class_is_reference(Class_Handle clss)
{
    assert(clss);
    if(class_get_extended_class(clss, "java/lang/ref/WeakReference") != NULL)
        return WEAK_REFERENCE;
    else if(class_get_extended_class(clss, "java/lang/ref/SoftReference") != NULL)
        return SOFT_REFERENCE;
    else if(class_get_extended_class(clss, "java/lang/ref/PhantomReference") != NULL)
        return PHANTOM_REFERENCE;
    else
        return NOT_REFERENCE;
}

unsigned class_get_referent_offset(Class_Handle ch)
{
    Field_Handle referent =
        class_lookup_field_recursive(ch, "referent", "Ljava/lang/Object;");
    if (!referent) {
        LDIE(6, "Class {0} has no 'Object referent' field" << class_get_name(ch));
    }
    return referent->get_offset();
}

void* class_alloc_via_classloader(Class_Handle ch, I_32 size)
{
    assert(ch);
    assert(size >= 0);
    Class *clss = (Class *)ch;
    assert (clss->get_class_loader());
    return clss->get_class_loader()->Alloc(size);
} //class_alloc_via_classloader

unsigned class_get_alignment(Class_Handle ch)
{
    assert(ch);
    return (unsigned)(ch->get_vtable()->class_properties
        & CL_PROP_ALIGNMENT_MASK);
} //class_get_alignment

//
// Returns the size of an element in the array class.
//
unsigned class_element_size(Class_Handle ch)
{
    assert(ch);
    return ch->get_array_element_size();
} //class_element_size

size_t class_get_object_size(Class_Handle ch)
{
    assert(ch);
    return ch->get_allocated_size();
} //class_get_object_size



static struct {
    const char* c;
    const char* m;
    const char* d;
} no_inlining_table[] = {
    { "java/lang/ClassLoader", "getCallerClassLoader", "()Ljava/lang/ClassLoader;" },
    { "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;" },
};

static unsigned no_inlining_table_count = sizeof(no_inlining_table)/sizeof(no_inlining_table[0]);

BOOLEAN method_is_no_inlining(Method_Handle mh)
{
    assert(mh);
    const char* c = class_get_name(method_get_class(mh));
    const char* m = method_get_name(mh);
    const char* d = method_get_descriptor(mh);
    for(unsigned i=0; i<no_inlining_table_count; i++)
        if (strcmp(c, no_inlining_table[i].c)==0 &&
            strcmp(m, no_inlining_table[i].m)==0 &&
            strcmp(d, no_inlining_table[i].d)==0)
            return TRUE;
    return FALSE;
} // method_is_no_inlining


// Class ch is a subclass of method_get_class(mh).  The function returns a method handle
// for an accessible method overriding mh in ch or in its closest superclass that overrides mh.
// Class ch must be a class not an interface.
Method_Handle method_get_overriding_method(Class_Handle ch, Method_Handle mh)
{
    assert(ch);
    assert(mh);
    Method *method = (Method *)mh;
    assert(!ch->is_interface());   // ch cannot be an interface

    const String *name = method->get_name();
    const String *desc = method->get_descriptor();
    Method *m = NULL;
    for(; ch;  ch = ch->get_super_class()) {
        m = ch->lookup_method(name, desc);
        if (m != NULL) {
            // IllegalAccessError will be thrown if implementation of
            // interface method is not public
            if (method->get_class()->is_interface() && !m->is_public()) {
                return NULL;
            }
            // The method m can only override mh/method
            // if m's class can access mh/method (JLS 6.6.5).
            if(m->get_class()->can_access_member(method)) {
                break;
            }
        }
    }
    return m;
} // method_get_overriding_method



I_32 vector_get_length(Vector_Handle vector)
{
    assert(vector);
    // XXX- need some assert that "vector" is really an array type
    return get_vector_length(vector);
} //vector_get_length



Managed_Object_Handle *vector_get_element_address_ref(Vector_Handle vector, I_32 idx)
{
    assert(vector);
    Managed_Object_Handle *elem = (Managed_Object_Handle *)get_vector_element_address_ref(vector, idx);
    return elem;
} //vector_get_element_address_ref



unsigned vm_vector_size(Class_Handle vector_class, int length)
{
    assert(vector_class);
    return vector_class->calculate_array_size(length);
} // vm_vector_size

static osmutex_t vm_gc_lock;

void vm_gc_lock_init()
{
    IDATA UNUSED status = port_mutex_create(&vm_gc_lock, APR_THREAD_MUTEX_NESTED);
    assert(status == TM_ERROR_NONE);
} // vm_gc_lock_init

void vm_gc_lock_enum()
{
    hythread_t self = tm_self_tls;
    int disable_count = self->disable_count;
    self->disable_count = 0;

    while (true) {
        IDATA UNUSED status = port_mutex_lock(&vm_gc_lock);
        assert(status == TM_ERROR_NONE);

        self->disable_count = disable_count;
        if (disable_count && self->suspend_count) {
            status = port_mutex_unlock(&vm_gc_lock);
            assert(status == TM_ERROR_NONE);

            self->disable_count = 0;
            hythread_safe_point_other(self);
        } else {
            break;
        }
    }
} // vm_gc_lock_enum

void vm_gc_unlock_enum()
{
    IDATA UNUSED status = port_mutex_unlock(&vm_gc_lock);
    assert(status == TM_ERROR_NONE);
} // vm_gc_unlock_enum



void *vm_get_gc_thread_local()
{
    return (void *) &p_TLS_vmthread->_gc_private_information;
}


size_t vm_number_of_gc_bytes_in_vtable()
{
    return GC_BYTES_IN_VTABLE;
}


size_t vm_number_of_gc_bytes_in_thread_local()
{
    return GC_BYTES_IN_THREAD_LOCAL;
}


BOOLEAN vm_is_heap_compressed()
{
    return REFS_IS_COMPRESSED_MODE;
} //vm_is_heap_compressed


void *vm_get_heap_base_address()
{
    return (void*)VM_Global_State::loader_env->heap_base;
} //vm_get_heap_base_address


void *vm_get_heap_ceiling_address()
{
    return (void *)VM_Global_State::loader_env->heap_end;
} //vm_get_heap_ceiling_address


BOOLEAN vm_is_vtable_compressed()
{
    return ManagedObject::are_vtable_pointers_compressed();
} //vm_is_vtable_compressed


Class_Handle allocation_handle_get_class(Allocation_Handle ah)
{
    assert(ah);
    VTable *vt;

    if (vm_is_vtable_compressed())
    {
        vt = (VTable *) ((UDATA)ah + (UDATA)vm_get_vtable_base_address());
    }
    else
    {
        vt = (VTable *) ah;
    }
    return (Class_Handle) vt->clss;
}


Allocation_Handle class_get_allocation_handle(Class_Handle ch)
{
    assert(ch);
    return ch->get_allocation_handle();
}

////////////////////////////////////////////////////////////////////////////////////
// Direct call-related functions that allow a JIT to be notified whenever a VM data
// structure changes that would require code patching or recompilation.
////////////////////////////////////////////////////////////////////////////////////

// Called by a JIT in order to be notified whenever the given method is recompiled or
// initially compiled. The callback_data pointer will be passed back to the JIT during the callback.
// The callback method is JIT_recompiled_method_callback.
void vm_register_jit_recompiled_method_callback(JIT_Handle jit, Method_Handle method,
                                                Method_Handle caller,
                                                 void *callback_data)
{
    assert(method);
    assert(caller);
    JIT *jit_to_be_notified = (JIT *)jit;
    Method *m = (Method *)method;
    m->register_jit_recompiled_method_callback(jit_to_be_notified, caller, callback_data);
} //vm_register_jit_recompiled_method_callback


void vm_patch_code_block(U_8* code_block, U_8* new_code, size_t size)
{
    assert(code_block != NULL);
    assert(new_code != NULL);

    // 20030203 We ensure that no thread is executing code that is simultaneously being patched.
    // We do this in part by stopping the other threads. This ensures that no thread will try to
    // execute code while it is being patched. Also, we take advantage of restrictions on the
    // patches done by JIT on IPF: it replaces the branch offset in a single bundle containing
    // a branch long. Note that this function does not synchronize the I- or D-caches.

    // Run through list of active threads and suspend the other ones.
    hythread_suspend_all(NULL, NULL);
    patch_code_with_threads_suspended(code_block, new_code, size);

    hythread_resume_all(NULL);

} //vm_patch_code_block

// Called by JIT during compilation to have the VM synchronously request a JIT (maybe another one)
// to compile another method.
JIT_Result vm_compile_method(JIT_Handle jit, Method_Handle method)
{
    return compile_do_compilation_jit((Method*) method, (JIT*) jit);
} // vm_compile_method


void vm_properties_set_value(const char* key, const char* value, PropertyTable table_number) 
{
    assert(key);
    switch(table_number) {
    case JAVA_PROPERTIES: 
        VM_Global_State::loader_env->JavaProperties()->set(key, value);
        break;
    case VM_PROPERTIES: 
        VM_Global_State::loader_env->VmProperties()->set(key, value);
        break;
    default:
        LDIE(71, "Unknown property table: {0}" << table_number);
    }
}

char* vm_properties_get_value(const char* key, PropertyTable table_number)
{
    assert(key);
    char* value;
    switch(table_number) {
    case JAVA_PROPERTIES: 
        value = VM_Global_State::loader_env->JavaProperties()->get(key);
        break;
    case VM_PROPERTIES: 
        value = VM_Global_State::loader_env->VmProperties()->get(key);
        break;
    default:
        value = NULL;
        LDIE(71, "Unknown property table: {0}" << table_number);
    }
    return value;
}

void vm_properties_destroy_value(char* value)
{
    if (value)
    {
        //FIXME which properties?
        VM_Global_State::loader_env->JavaProperties()->destroy(value);
    }
}

BOOLEAN vm_property_is_set(const char* key, PropertyTable table_number)
{
    bool value = false;
    assert(key);
    switch(table_number) {
    case JAVA_PROPERTIES: 
        value = VM_Global_State::loader_env->JavaProperties()->is_set(key);
        break;
    case VM_PROPERTIES: 
        value = VM_Global_State::loader_env->VmProperties()->is_set(key);
        break;
    default:
        LDIE(71, "Unknown property table: {0}" << table_number);
    }
    return value ? TRUE : FALSE;
}

char** vm_properties_get_keys(PropertyTable table_number)
{
    char** value;
    switch(table_number) {
    case JAVA_PROPERTIES: 
        value = VM_Global_State::loader_env->JavaProperties()->get_keys();
        break;
    case VM_PROPERTIES: 
        value = VM_Global_State::loader_env->VmProperties()->get_keys();
        break;
    default:
        value = NULL;
        LDIE(71, "Unknown property table: {0}" << table_number);
    }
    return value;
}

char** vm_properties_get_keys_starting_with(const char* prefix, PropertyTable table_number)
{
    assert(prefix);
    char** value;
    switch(table_number) {
    case JAVA_PROPERTIES: 
        value = VM_Global_State::loader_env->JavaProperties()->get_keys_staring_with(prefix);
        break;
    case VM_PROPERTIES: 
        value = VM_Global_State::loader_env->VmProperties()->get_keys_staring_with(prefix);
        break;
    default:
        value = NULL;
        LDIE(71, "Unknown property table: {0}" << table_number);
    }
    return value;
}

void vm_properties_destroy_keys(char** keys)
{
    if (keys)
    {
        //FIXME which properties?
        VM_Global_State::loader_env->JavaProperties()->destroy(keys);
    }
}

int vm_property_get_integer(const char *property_name, int default_value, PropertyTable table_number)
{
    assert(property_name);
    char *value = vm_properties_get_value(property_name, table_number);
    int return_value = default_value;
    if (NULL != value)
    {
        return_value = atoi(value);
        vm_properties_destroy_value(value);
    }
    return return_value;
}

BOOLEAN vm_property_get_boolean(const char *property_name, BOOLEAN default_value, PropertyTable table_number)
{
    assert(property_name);
    char *value = vm_properties_get_value(property_name, table_number);
    if (NULL == value)
    {
        return default_value;
    }
    Boolean return_value = default_value;
    if (0 == strcmp("no", value)
        || 0 == strcmp("off", value)
        || 0 == strcmp("false", value)
        || 0 == strcmp("0", value))
    {
        return_value = FALSE;
    }
    else if (0 == strcmp("yes", value)
             || 0 == strcmp("on", value)
             || 0 == strcmp("true", value)
             || 0 == strcmp("1", value))
    {
        return_value = TRUE;
    }
    vm_properties_destroy_value(value);
    return return_value;
}

size_t vm_property_get_size(const char *property_name, size_t default_value, PropertyTable table_number) 
{
  char* size_string; 
  size_t size; 
  int sizeModifier;
  size_t unit = 1;

  size_string = vm_properties_get_value(property_name, table_number);

  if (size_string == NULL) {
    return default_value;
  }

  size = atol(size_string);
  sizeModifier = tolower(size_string[strlen(size_string) - 1]);
  vm_properties_destroy_value(size_string);

  switch (sizeModifier) {
  case 'k': unit = 1024; break;
  case 'm': unit = 1024 * 1024; break;
  case 'g': unit = 1024 * 1024 * 1024;break;
  }

  size_t res = size * unit;
  if (res / unit != size) {
    /* overflow happened */
    return 0;
  }
  return res;
}


static Annotation* lookup_annotation(AnnotationTable* table, Class* owner, Class* antn_type) {
    for (int i = table->length - 1; i >= 0; --i) {
        Annotation* antn = table->table[i];
        Type_Info_Handle tih = (Type_Info_Handle) type_desc_create_from_java_descriptor(antn->type->bytes, owner->get_class_loader());
        if (tih) {
            //FIXME optimize: first check if class name matches
            Class* type = type_info_get_class(tih);
            if (antn_type == type) {
                return antn;
            }
        }
    }
    TRACE("No such annotation " << antn_type->get_name()->bytes);
    return NULL;
}


BOOLEAN method_has_annotation(Method_Handle target, Class_Handle antn_type) {
    assert(target);
    assert(antn_type);
    if (target->get_declared_annotations()) {
        Annotation* antn = lookup_annotation(target->get_declared_annotations(), target->get_class(), antn_type);
        return antn!=NULL;
    }
    return false;
}

char * get_method_entry_flag_address()
{
    return VM_Global_State::loader_env->TI->get_method_entry_flag_address();
}

char * get_method_exit_flag_address()
{
    return VM_Global_State::loader_env->TI->get_method_exit_flag_address();
}
