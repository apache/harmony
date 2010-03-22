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

#define LOG_DOMAIN "vm.core"
#include "cxxlog.h"


#include "Class.h"
#include "classloader.h"
#include "environment.h"
#include "nogc.h"
#include "open/vm_util.h"
#include "jit_intf_cpp.h"
#include "port_barriers.h"
#include "cci.h"

#ifdef _IPF_
#include "vm_ipf.h"
#endif // _IPF_


/////////////////////////////////////////////////////////////////
// begin Method
/////////////////////////////////////////////////////////////////



Arg_List_Iterator initialize_arg_list_iterator(const char *descr)
{
    while(*descr != '(') {
        descr++;
    }
    assert(*descr == '(');
    return descr + 1;
} //initialize_iterator

Arg_List_Iterator Method::get_argument_list() {
    return initialize_arg_list_iterator(_descriptor->bytes);
}


Java_Type curr_arg(Arg_List_Iterator it)
{
    return (Java_Type)*(char *)it;
} //curr_arg



Class_Handle get_curr_arg_class(Arg_List_Iterator it,
                                Method_Handle m)
{
    Global_Env *env = VM_Global_State::loader_env;
    Java_Type UNUSED t = curr_arg(it);
    assert(t == JAVA_TYPE_CLASS || t == JAVA_TYPE_ARRAY);
    Arg_List_Iterator next = advance_arg_iterator(it);
    size_t len = ((char *)next) - ((char *)it);
    const char* name = (const char*)it;
    String* n;
    if(name[0] == 'L') {
        n = env->string_pool.lookup(name+1, len-2);
    } else {
        n = env->string_pool.lookup(name, len);
    }
    Class* clss = ((Method *)m)->get_class();
    Class* c = clss->get_class_loader()->LoadClass(env, n);
    return c;
} //get_curr_arg_class



Arg_List_Iterator advance_arg_iterator(Arg_List_Iterator it)
{
    char *iter = (char *)it;
    while(*iter == '[')
        iter++;

    if(*iter == ')') {
        return iter;
    }

    if(*iter == 'L') {
        while(*iter++ != ';')
            ;
        return iter;
    }

    iter++;

    return iter;
} //advance_arg_iterator

static Class_Handle method_get_return_type_class(Method_Handle m)
{
    assert(hythread_is_suspend_enabled());
    Method *method = (Method *)m;
    Global_Env *env = VM_Global_State::loader_env;
    Java_Type UNUSED t = method->get_return_java_type();
    assert(t == JAVA_TYPE_CLASS || t == JAVA_TYPE_ARRAY);
    const char *descr = method->get_descriptor()->bytes;
    while(*descr != ')')
        descr++;
    descr++;
    String *n;
    if(descr[0] == 'L') {
        descr++;
        size_t len = strlen(descr);
        n = env->string_pool.lookup(descr, len-1);
    } else {
        n = env->string_pool.lookup(descr);
    }

    Class *clss = method->get_class();
    Class *c = clss->get_class_loader()->LoadVerifyAndPrepareClass(env, n);
    return c;
} //method_get_return_type_class



// Previously this method is not implemented
// Return the return class of this method (for non-primitive return types)
Class* Method::get_return_class_type()
{
    Class *clss = (Class*)method_get_return_type_class(this);
    
    return clss;
} //Method::get_return_class_type



Handler::Handler()
{
    _start_pc    = 0;
    _end_pc      = 0;
    _handler_pc  = 0;
    _catch_type  = NULL;
} //Handler::Handler


void Method::set_num_target_exception_handlers(JIT *jit, unsigned n)
{
    CodeChunkInfo *jit_info = get_chunk_info_mt(jit, CodeChunkInfo::main_code_chunk_id);
    assert (jit_info->_num_target_exception_handlers == 0);
    jit_info->_num_target_exception_handlers = n;
    assert (!jit_info->_target_exception_handlers);
    jit_info->_target_exception_handlers = (Target_Exception_Handler_Ptr*) Alloc(sizeof(Target_Exception_Handler_Ptr) * n);
    memset(jit_info->_target_exception_handlers, 0, n * sizeof(Target_Exception_Handler_Ptr));
} //Method::set_num_target_exception_handlers



unsigned Method::get_num_target_exception_handlers(JIT *jit) {
    CodeChunkInfo *jit_info = get_chunk_info_mt(jit, CodeChunkInfo::main_code_chunk_id);
    assert(jit_info != NULL);
    return jit_info->_num_target_exception_handlers; 
} //Method::get_num_target_exception_handlers



void Method::set_target_exception_handler_info(JIT *jit,
                                               unsigned eh_number,
                                               void *start_ip,
                                               void *end_ip,
                                               void *handler_ip,
                                               Class *catch_clss,
                                               bool exc_obj_is_dead)
{
    CodeChunkInfo *jit_info = get_chunk_info_mt(jit, CodeChunkInfo::main_code_chunk_id);
    assert(eh_number < jit_info->_num_target_exception_handlers);
    jit_info->_target_exception_handlers[eh_number] =
        new Target_Exception_Handler(start_ip,
                                     end_ip,
                                     handler_ip,
                                     catch_clss,
                                     exc_obj_is_dead);
} //Method::set_target_exception_handler_info



Target_Exception_Handler_Ptr
Method::get_target_exception_handler_info(JIT *jit, unsigned eh_num)
{
    CodeChunkInfo *jit_info = get_chunk_info_mt(jit, CodeChunkInfo::main_code_chunk_id);
    assert(jit_info != NULL);
    return jit_info->_target_exception_handlers[eh_num];
} //Method::get_target_exception_handler_info


void Method::calculate_arguments_slot_num() 
{
    //This method counts length of method parameters in slots.
    //See 4.4.3 paragraph 5 in specification.
    unsigned slot_num = 0;
    if (!is_static()) {
        slot_num = 1;
    }
    Arg_List_Iterator iter = get_argument_list();
    Java_Type typ;
    while((typ = curr_arg(iter)) != JAVA_TYPE_END) {
        switch(typ) {
        case JAVA_TYPE_LONG:
        case JAVA_TYPE_DOUBLE:
            slot_num += 2;
            break;
        default:
            slot_num += 1;
            break;
        }
        iter = advance_arg_iterator(iter);
    }
    _arguments_slot_num = slot_num;
} // Method::calculate_arguments_slot_num

unsigned Method::get_num_ref_args()
{
    unsigned nargs;

    if(is_static())
        nargs = 0;
    else
        nargs = 1;

    Arg_List_Iterator iter = get_argument_list();
    Java_Type typ;
    while((typ = curr_arg(iter)) != JAVA_TYPE_END) {
        switch(typ) {
        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
            nargs++;
            break;
        default:
            break;
        }
        iter = advance_arg_iterator(iter);
    }

    return nargs;
} //Method::get_num_ref_args



unsigned Method::get_num_args()
{
    unsigned nargs;

    if(is_static()) {
        nargs = 0;
    } else {
        nargs = 1;
    }

    Arg_List_Iterator iter = get_argument_list();
    Java_Type UNUSED typ;
    while((typ = curr_arg(iter)) != JAVA_TYPE_END) {
        nargs++;
        iter = advance_arg_iterator(iter);
    }

    return nargs;
} //Method::get_num_args



static unsigned alloc_call_count = 0;

void *Method::allocate_code_block_mt(size_t size, size_t alignment, JIT *jit, unsigned heat, int id, Code_Allocation_Action action)
{
    assert(jit != NULL);
    alloc_call_count++;

#ifdef _IPF_
    // Add one bundle to avoid boundary conditions for exception throwing when the call to athrow is in the last bundle of a method
    // and return link points to the first bundle of the following method.
    size += 16;
#endif
    void *addr;
    if (size == 0) {
        addr = NULL;
    } else {
        addr = get_class()->code_alloc(size, alignment, action);
    }

    if (action == CAA_Simulate) {
        // Simulating allocation: return the chunk address without registering a new CodeChunkInfo for it.
        return addr;
    }

    // Create (if necessary) and initialize a CodeChunkInfo for the new code chunk.
    CodeChunkInfo *jit_info = get_chunk_info_mt(jit, id);

    assert(jit_info);
    assert(jit_info->get_code_block_addr() == NULL);
    lock();
    jit_info->_heat                 = heat;
    jit_info->_code_block           = addr;
    jit_info->_code_block_size      = size;
    jit_info->_code_block_alignment = alignment;
    unlock();

    Global_Env *env = VM_Global_State::loader_env;
    env->em_interface->RegisterCodeChunk(this, addr, size, jit_info); // Method table is thread safe
    return addr;
} // Method::allocate_code_block

// Read/Write data block.
void *Method::allocate_rw_data_block(size_t size, size_t alignment, JIT *jit)
{
    // Make sure alignment is a power of 2.
    assert((alignment & (alignment-1)) == 0);
    void *rw_data_block;
    if(!size) {
        rw_data_block = NULL;
    } else {
        size_t size_with_padding = size + alignment - 1;
        rw_data_block = Alloc(size_with_padding);
        rw_data_block = (void *) ((POINTER_SIZE_INT)((char*)rw_data_block + alignment - 1) & ~(POINTER_SIZE_INT)(alignment-1));

    }
    return rw_data_block;
} //Method::allocate_rw_data_block



// Allocate memory for extra information needed by JIT.
void *Method::allocate_jit_info_block(size_t size, JIT *jit)
{
    assert(size); // should be no need to allocate empty blocks
    U_8* p_block = (U_8*) Alloc(size + sizeof(JIT **));

    // Store a pointer to the JIT before the JIT info block.
    *(JIT **) p_block = jit;

    U_8* jit_info_block = p_block + sizeof(JIT **);

    CodeChunkInfo *jit_info = get_chunk_info_mt(jit, CodeChunkInfo::main_code_chunk_id);
    assert(jit_info != NULL);

    lock();
    jit_info->_jit_info_block = jit_info_block;
    jit_info->_jit_info_block_size = size;
    unlock();
    return jit_info_block;
} // Method::allocate_jit_info_block



// Create a new CodeChunkInfo.
CodeChunkInfo *Method::create_code_chunk_info_mt()
{
    CodeChunkInfo *result_chunk = (CodeChunkInfo *) Alloc(sizeof(CodeChunkInfo));
    CodeChunkInfo::initialize_code_chunk(result_chunk);
    
    return result_chunk;
} //create_code_chunk_info



CodeChunkInfo *Method::get_chunk_info_no_create_mt(JIT *jit, int id)
{
    // NOTE:unlocked access to chunks, this requires sfence in addition of this
    // chunks
    CodeChunkInfo *jit_info;
    for (jit_info = get_first_JIT_specific_info();  jit_info;  jit_info = jit_info->_next) {
        if ((jit_info->get_jit() == jit) && (jit_info->get_id() == id)) {
            return jit_info;
        }
    }
    return NULL;
} // Method::get_chunk_info_no_create_mt



CodeChunkInfo *Method::get_chunk_info_mt(JIT *jit, int id)
{
    CodeChunkInfo *jit_info = get_chunk_info_no_create_mt(jit, id);
    if (jit_info != NULL) {
        return jit_info;
    }

    jit_info = create_code_chunk_info_mt();
    jit_info->set_jit(jit);
    jit_info->set_id(id);
    jit_info->set_method(this);

    // Ensure that the first element of the method's code chunk list is the main code chunk for this or some other jit
    lock();
    bool insert_at_head = true;

    if (!CodeChunkInfo::is_main_code_chunk_id(id) && (_jits != NULL)) {
        if (_jits->get_jit() == jit) {
            assert(_jits->get_id() != id);
            insert_at_head = false;
        }
    }

    if (insert_at_head) {
        jit_info->_next = _jits;
        // Write barrier is needed as we use
        // unlocked fast access to the collection
        port_write_barrier();
        _jits = jit_info;
    } else {
        jit_info->_next = _jits->_next;
        // Write barrier is needed as we use
        // unlocked fast access to the collection
        port_write_barrier();
        _jits->_next = jit_info;
    }
    unlock();
    return jit_info;
} //Method::get_chunk_info


//
//  alignment parameter is applied to &(bytes[0])
//
void *Method::allocate_JIT_data_block(size_t size, JIT *jit, size_t alignment)
{
    // Make sure alignment is a power of 2.
    assert((alignment & (alignment-1)) == 0);
    char *data_block;
    if(!size) {
        data_block = NULL;
    } else {
        size_t size_with_padding = size + sizeof(JIT_Data_Block) + alignment - 1;
        data_block = (char*) Alloc(size_with_padding);

        // aligning data with specified alignment.
        size_t mask = (size_t)(alignment-1);
        size_t data_offset = (size_t) &((JIT_Data_Block *)0)->bytes[0];
        data_block += mask & (0 - (POINTER_SIZE_INT)data_block - data_offset);
    }
    JIT_Data_Block *block = (JIT_Data_Block *)data_block;

    CodeChunkInfo *jit_info;
    for (jit_info = _jits;  jit_info;  jit_info = jit_info->_next) {
        if ((jit_info->get_jit() == jit) && (jit_info->get_id() == CodeChunkInfo::main_code_chunk_id)) {
            block->next = jit_info->_data_blocks;
            jit_info->_data_blocks = block;
            break;
        }
    }
    if (jit_info == NULL) {
        jit_info = create_code_chunk_info_mt();
        jit_info->set_jit(jit);
        jit_info->_next = _jits;
        port_write_barrier();
        _jits = jit_info;
        block->next = jit_info->_data_blocks;
        jit_info->_data_blocks = block;
    }
    return &(block->bytes[0]);
} //Method::allocate_JIT_data_block



void Method::add_vtable_patch(void *patch)
{
    Global_Env * vm_env = VM_Global_State::loader_env;
    
    vm_env->p_vtable_patch_lock->_lock();                         // vvv

    if (_vtable_patch == NULL) {
        VTable_Patches *vp = (VTable_Patches *)STD_MALLOC(sizeof(VTable_Patches));
        memset(vp, 0, sizeof(VTable_Patches));
        _vtable_patch = vp;
    }


    VTable_Patches *curr_vp = _vtable_patch;
    for (int i = 0; i < MAX_VTABLE_PATCH_ENTRIES; i++) {
        if (curr_vp->patch_table[i] == NULL) {
            curr_vp->patch_table[i] = patch;
            vm_env->p_vtable_patch_lock->_unlock();               // ^^
            return;
        }
    }
    VTable_Patches *new_vp = (VTable_Patches *)STD_MALLOC(sizeof(VTable_Patches));
    memset(new_vp, 0, sizeof(VTable_Patches));
    new_vp->next = curr_vp;
    _vtable_patch = new_vp;
    new_vp->patch_table[0] = patch;

    vm_env->p_vtable_patch_lock->_unlock();                     // ^^^
} //Method::add_vtable_patch



void Method::apply_vtable_patches()
{
    Global_Env * vm_env = VM_Global_State::loader_env;

    if (_vtable_patch == NULL) {
        // Constructors are never entered into a vtable.
        return;
    }

    vm_env->p_vtable_patch_lock->_lock();

    void *code_addr = get_code_addr();

    VTable_Patches *vp = (VTable_Patches *)_vtable_patch;
    for(;  (vp != NULL);  vp = vp->next) {
        for (int i = 0;  i < MAX_VTABLE_PATCH_ENTRIES;  i++) {
            if (vp->patch_table[i] != NULL) {
                *((void **)vp->patch_table[i]) = code_addr;
            }
        }
    }

    vm_env->p_vtable_patch_lock->_unlock();
} //Method::apply_vtable_patches



unsigned Method::num_exceptions_method_can_throw() 
{
    return _n_exceptions;   
} //Method::num_exceptions_method_can_throw



String* Method::get_exception_name (int n) 
{
    if (!_exceptions || (n >= _n_exceptions)) return NULL;
    return _exceptions[n];
}

void Method::method_was_overridden() 
{
    _flags.is_overridden = 1;
} //Method::method_was_overridden
////////////////////////////////////////////////////////////////////
// begin support for JIT notification when methods are recompiled

// Notify the given JIT whenever this method is recompiled or initially compiled.
// The callback_data pointer will be passed back to the JIT during the callback.  
// The JIT's callback function is JIT_recompiled_method_callback.
void Method::register_jit_recompiled_method_callback(JIT *jit_to_be_notified, 
                                                     Method* caller,
                                                     void *callback_data)
{
    // Don't insert the same entry repeatedly on the _notify_recompiled_records list.
    Method_Change_Notification_Record *nr = _notify_recompiled_records;
    while (nr != NULL) {
        if (nr->equals(jit_to_be_notified, callback_data)) {
            return;
        }
        nr = nr->next;
    }

    // Insert a new notification record.
    Method_Change_Notification_Record *new_nr = 
        (Method_Change_Notification_Record *)STD_MALLOC(sizeof(Method_Change_Notification_Record));
    new_nr->caller = caller;
    new_nr->jit                = jit_to_be_notified;
    new_nr->callback_data      = callback_data;
    new_nr->next               = _notify_recompiled_records;
    _notify_recompiled_records = new_nr;

    // Record a callback in the caller method to let it unregister itself if unloaded.
    ClassLoader* this_loader = get_class()->get_class_loader();
    ClassLoader* caller_loader = caller->get_class()->get_class_loader();
    if (this_loader == caller_loader || caller_loader->IsBootstrap()) return;

    MethodSet *vec = caller->_recompilation_callbacks;
    if (vec == NULL) {
        vec = caller->_recompilation_callbacks = new MethodSet();
    }
    vec->push_back(this);
} //Method::register_jit_recompiled_method_callback

void Method::unregister_jit_recompiled_method_callbacks(const Method* caller) {
    TRACE2("cu.debug", "unregister jit callback, caller=" << caller << " callee=" << this);
    Method_Change_Notification_Record *nr,*prev = NULL;
    for (nr = _notify_recompiled_records;  nr != NULL;  ) {
        if (nr->caller == caller) {
            if (prev) {
                prev->next = nr->next;
            } else {
                _notify_recompiled_records = nr->next;
            }
            Method_Change_Notification_Record *next = nr->next;
            STD_FREE(nr);
            nr = next;
        } else {
            prev = nr;
            nr = nr->next;
        }
    }
}

void Method::do_jit_recompiled_method_callbacks() 
{
    Method_Change_Notification_Record *nr;
    for (nr = _notify_recompiled_records;  nr != NULL;  nr = nr->next) {
        JIT *jit_to_be_notified = nr->jit;
        Boolean code_was_modified = 
            jit_to_be_notified->recompiled_method_callback(this, nr->callback_data);
        if (code_was_modified) {
#ifdef _IPF_
            CodeChunkInfo *jit_info;
            for (jit_info = get_first_JIT_specific_info(); jit_info; jit_info = jit_info->_next) {
                if (jit_info->get_jit() == jit_to_be_notified) {
                    flush_hw_cache((U_8*)jit_info->get_code_block_addr(), jit_info->get_code_block_size());
                }
            }
            sync_i_cache();
            do_mf();
#endif //_IPF_
        }
    }
} //Method::do_jit_recompiled_method_callbacks

// end support for JIT notification when methods are recompiled
////////////////////////////////////////////////////////////////////



//////////////////////////////////////////////////////
// begin nop analysis

enum Nop_Stack_State {
    NS_StackEmpty,
    NS_ThisPushed,
    NS_ThisAndZeroPushed
};

void Method::_set_nop()
{
    bool verbose = false;

    Global_Env *env = VM_Global_State::loader_env;
    if (get_name() != env->Init_String || get_descriptor() != env->VoidVoidDescriptor_String) {
        return;
    }

    if(is_native()) {
        return;
    }
    unsigned len = _byte_code_length;
    if(!len) {
        return;
    }
    U_8* bc = _byte_codes;
    Nop_Stack_State stack_state = NS_StackEmpty;
    if(verbose) {
        printf("=========== nop[%d]: %s.%s%s\n", len, get_class()->get_name()->bytes, get_name()->bytes, get_descriptor()->bytes);
    }
    for (unsigned idx = 0; idx < len; idx++) {
        U_8 b = bc[idx];
        if(verbose) {
            printf("\tbc[%d]=%d, state=%d\n", idx, b, stack_state);
        }
        if(b == 0xb1) {   // return
            if(verbose) {
                printf("+++++++ nop: %s.%s%s\n", get_class()->get_name()->bytes, get_name()->bytes, get_descriptor()->bytes);
            }
            _flags.is_nop = TRUE;
            return;
        }
        switch(stack_state) {
        case NS_StackEmpty:
            switch(b) {
            case 0x2a:  // aload_0
                stack_state = NS_ThisPushed;
                break;
            default:
                return;
            }
            break;
        case NS_ThisPushed:
            switch(b) {
            case 0x01:  // aconst_null
            case 0x03:  // iconst_0
                stack_state = NS_ThisAndZeroPushed;
                break;
            case 0xb7:  // invokespecial
                {
                    unsigned index = (bc[idx + 1] << 8) + bc[idx + 2];
                    if(verbose) {
                        printf("\tinvokespecial, index=%d\n", index);
                    }
                    Method_Handle mh = resolve_special_method_env(VM_Global_State::loader_env,
                                                                  get_class(),
                                                                  index, false);
                    Method *callee = (Method *)mh;
                    if(!callee) {
                        if(verbose) {
                            printf("\tinvokespecial, callee==null\n");
                        }
                        return;
                    }
                    if(callee == this) {
                        return;
                    }
                    if(verbose) {
                        printf("invokespecial: %s.%s%s\n", callee->get_class()->get_name()->bytes, callee->get_name()->bytes, callee->get_descriptor()->bytes);
                    }
                    if(!callee->is_nop()) {
                        return;
                    }
                    const char *descr = callee->get_descriptor()->bytes;
                    if(descr[1] != ')') {
                        return;
                    }
                    if(verbose) {
                        printf("invokespecial nop: %s.%s%s\n", callee->get_class()->get_name()->bytes, callee->get_name()->bytes, callee->get_descriptor()->bytes);
                    }
                }
                stack_state = NS_StackEmpty;
                idx += 2;
                break;
            default:
                return;
            }
            break;
        case NS_ThisAndZeroPushed:
            switch(b) {
            case 0xb5:  // putfield
                stack_state = NS_StackEmpty;
                if(verbose) {
                    printf("\tputfield\n");
                }
                idx += 2;
                break;
            default:
                return;
            }
            break;
        default:
            LDIE(57, "Unexpected stack state");
            return;
        }
    }
    LDIE(56, "should'nt get here");
} //Method::_set_nop


// end nop analysis
//////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////////
// end Method
/////////////////////////////////////////////////////////////////

