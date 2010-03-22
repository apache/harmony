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
 * @author Intel, Evgueni Brevnov, Ivan Volosyuk
 */  
#define LOG_DOMAIN "vm.helpers"
#include "cxxlog.h"

#include "open/types.h"
#include "open/gc.h"
#include "open/vm_class_manipulation.h"
#include "vtable.h"
#include "environment.h"
#include "Class.h"
#include "object_layout.h"
#include "mon_enter_exit.h"
#include "vm_threads.h"

#include "thread_helpers.h"

#include "vm_arrays.h"
#include "vm_strings.h"
#include "ini.h"
#include "nogc.h"
#include "compile.h"
#include "exceptions.h"
#include "exceptions_jit.h"
#include "sync_bits.h"

#include "encoder.h"
#include "lil.h"
#include "lil_code_generator.h"
#include "jit_runtime_support_common.h"
#include "jit_runtime_support.h"
#include "internal_jit_intf.h"
#include "m2n.h"
#include "../m2n_ia32_internal.h"

#include "dump.h"
#include "vm_stats.h"

char * gen_convert_managed_to_unmanaged_null_ia32(char * ss, 
                                                  unsigned stack_pointer_offset);

/////////////////////////////////////////////////////////////////
// begin VM_Runtime_Support
/////////////////////////////////////////////////////////////////

static void vm_throw_java_lang_ClassCastException()
{
    ASSERT_THROW_AREA;
    assert(!hythread_is_suspend_enabled());
    exn_throw_by_name("java/lang/ClassCastException");
    LDIE(49, "The last called function should not return");
} //vm_throw_java_lang_ClassCastException

#ifdef VM_STATS // exclude remark in release mode (defined but not used)
static void update_checkcast_stats(ManagedObject *obj, Class *c)
{
    VM_Statistics::get_vm_stats().num_checkcast ++;
    if (obj == NULL)
        VM_Statistics::get_vm_stats().num_checkcast_null ++;
    if (obj != NULL && obj->vt()->clss == c)
        VM_Statistics::get_vm_stats().num_checkcast_equal_type ++;
    if (obj != NULL && c->get_fast_instanceof_flag())
        VM_Statistics::get_vm_stats().num_checkcast_fast_decision ++;
} //update_checkcast_stats
#endif

// 20030321 This JIT support routine expects to be called directly from managed code.
// NOTE: We do not translate null references since vm_instanceof() also expects to be 
//       called directly by managed code.
static void *getaddress__vm_checkcast_naked()
{
    static void *addr = 0;
    if (addr) {
        return addr;
    }
    if (VM_Global_State::loader_env->use_lil_stubs) {
        LilCodeStub *cs = gen_lil_typecheck_stub(true);
        addr = LilCodeGenerator::get_platform()->compile(cs);

        DUMP_STUB(addr, "vm_rt_checkcast", lil_cs_get_code_size(cs));

        lil_free_code_stub(cs);
        return addr;
    }
    const int stub_size = (REFS_IS_COMPRESSED_MODE ? 82 : 58);
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_COLD, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;

#ifdef VM_STATS
    ss = push(ss,  M_Base_Opnd(esp_reg, 8));
    ss = push(ss,  M_Base_Opnd(esp_reg, 8));
    ss = call(ss, (char *)update_checkcast_stats);
    ss = alu(ss, add_opc,  esp_opnd,  Imm_Opnd(8));
#endif // VM_STATS

    ss = mov(ss,  eax_opnd,  M_Base_Opnd(esp_reg, +4) );
    REFS_RUNTIME_SWITCH_IF
#ifdef REFS_RUNTIME_OR_COMPRESSED
        ss = alu(ss, cmp_opc,  eax_opnd,  Imm_Opnd((unsigned)VM_Global_State::loader_env->managed_null)); //is eax == NULL?
        ss = branch8(ss, Condition_NZ,  Imm_Opnd(size_8, 0));
#endif // REFS_RUNTIME_OR_COMPRESSED
    REFS_RUNTIME_SWITCH_ELSE
#ifdef REFS_RUNTIME_OR_UNCOMPRESSED
        ss = test(ss,  eax_opnd,  eax_opnd);
        ss = branch8(ss, Condition_NE,  Imm_Opnd(size_8, 0));  // will get backpatched
#endif // REFS_RUNTIME_OR_UNCOMPRESSED
    REFS_RUNTIME_SWITCH_ENDIF
    char *backpatch_address__not_null = ((char *)ss) - 1;

    // The object reference is null, so return null.     
    ss = ret(ss,  Imm_Opnd(8));

    // Non-null object reference. Call instanceof.
    signed offset = (signed)ss - (signed)backpatch_address__not_null - 1;
    *backpatch_address__not_null = (char)offset;
    ss = push(ss,  M_Base_Opnd(esp_reg, 8));
    ss = push(ss,  eax_opnd);
    // 20030321 If compressing references, no need to convert a null reference before calling the instanceof JIT support routine
    // since the object is known to be non-null.
    ss = call(ss, (char *)vm_instanceof);

    ss = test(ss,  eax_opnd,  eax_opnd);
    ss = branch8(ss, Condition_E,  Imm_Opnd(size_8, 0));  // will get backpatched
    char *backpatch_address__instanceof_failed = ((char *)ss) - 1;
    ss = mov(ss,  eax_opnd,  M_Base_Opnd(esp_reg, +4) );
    // 20030321 If compressing references, no need to convert a null reference before returning since the object is known to be non-null.
    ss = ret(ss,  Imm_Opnd(8));

    // The checkcast failed so throw java_lang_ClassCastException.
    offset = (signed)ss - (signed)backpatch_address__instanceof_failed - 1;
    *backpatch_address__instanceof_failed = (char)offset;

    ss = gen_setup_j2n_frame(ss);
    ss = call(ss, (char *)vm_throw_java_lang_ClassCastException);

    addr = stub;
    assert((ss - stub) <= stub_size);

    compile_add_dynamic_generated_code_chunk("vm_rt_checkcast", false, stub, stub_size);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("vm_rt_checkcast", stub, stub_size);
    }

    DUMP_STUB(stub, "getaddress__vm_checkcast_naked", ss - stub);

    return addr;
}  //getaddress__vm_checkcast_naked




// The code is added to this function so that we can have a LIL version of
// instanceof.  If LIL is turned off, the address of vm_instanceof is
// returned, just like before.
static void *getaddress__vm_instanceof()
{
    static void *addr = 0;
    if (addr) {
        return addr;
    }

    if (VM_Global_State::loader_env->use_lil_stubs)
    {
        LilCodeStub *cs = gen_lil_typecheck_stub(false);
        assert(lil_is_valid(cs));
        addr = LilCodeGenerator::get_platform()->compile(cs);

        DUMP_STUB(addr, "vm_instanceof", lil_cs_get_code_size(cs));

        lil_free_code_stub(cs);
        return addr;
    }

    // just use vm_instanceof
    addr = (void *) vm_instanceof;
    return addr;
}


static Boolean is_class_initialized(Class *clss)
{
#ifdef VM_STATS
    VM_Statistics::get_vm_stats().num_is_class_initialized++;
    clss->initialization_checked();
#endif // VM_STATS
    assert(!hythread_is_suspend_enabled());
    return clss->is_initialized();
} //is_class_initialized



static void *getaddress__vm_initialize_class_naked()
{
    static void *addr = 0;
    if (addr) {
        return addr;
    }

    if (VM_Global_State::loader_env->use_lil_stubs) {
        LilCodeStub* cs = lil_parse_code_stub(
            "entry 0:stdcall:pint:void;   // The single argument is a Class_Handle \n"
            "locals 3;\
             in2out platform:pint; \
             call %0i; \
             jc r=0,not_initialized; \
             ret; \
             :not_initialized; \
             push_m2n 0, %1i; \
             in2out platform:void; \
             call %2i; \
             l1 = ts; \
             ld l2, [l1 + %3i:ref]; \
             jc l2 != 0,_exn_raised; \
             ld l2, [l1 + %4i:ref]; \
             jc l2 != 0,_exn_raised; \
             pop_m2n; \
             ret; \
             :_exn_raised; \
             out platform::void; \
             call.noret %5i;",
             (void *)is_class_initialized,
             (FRAME_JNI | FRAME_POPABLE),
             (void *)class_initialize,
             APR_OFFSETOF(VM_thread, thread_exception.exc_object),
             APR_OFFSETOF(VM_thread, thread_exception.exc_class),
             (void*)exn_rethrow);
        assert(lil_is_valid(cs));
        addr = LilCodeGenerator::get_platform()->compile(cs);

        DUMP_STUB(addr, "vm_initialize_class_naked", lil_cs_get_code_size(cs));
        
        lil_free_code_stub(cs);
        return addr;
    }

    const int stub_size = 44;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_COLD, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;

    ss = push(ss,  M_Base_Opnd(esp_reg, 4));        // push the argument, a ClassHandle
    ss = call(ss, (char *)is_class_initialized);
    ss = alu(ss, add_opc,  esp_opnd,  Imm_Opnd(4)); // pop the argument since is_class_initialized() is a CDecl function

    ss = test(ss,  eax_opnd,  eax_opnd);
    ss = branch8(ss, Condition_Z,  Imm_Opnd(size_8, 0));
    char *backpatch_address__class_not_initialized = ((char *)ss) - 1; 
    ss = ret(ss,  Imm_Opnd(4));

    signed offset = (signed)ss - (signed)backpatch_address__class_not_initialized - 1;
    *backpatch_address__class_not_initialized = (char)offset;
    ss = gen_setup_j2n_frame(ss);
    ss = push(ss,  M_Base_Opnd(esp_reg, m2n_sizeof_m2n_frame));
    ss = call(ss, (char *)vm_rt_class_initialize);
    ss = alu(ss, add_opc,  esp_opnd,  Imm_Opnd(4));
    ss = gen_pop_j2n_frame(ss);
    ss = ret(ss,  Imm_Opnd(4));

    addr = stub;
    assert((ss - stub) <= stub_size);

    compile_add_dynamic_generated_code_chunk("vm_initialize_class_naked", false, stub, stub_size);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("vm_initialize_class_naked", stub, stub_size);
    }

    DUMP_STUB(stub, "getaddress__vm_initialize_class_naked", ss - stub);

    return addr;
} //getaddress__vm_initialize_class_naked



//////////////////////////////////////////////////////////////////////
// Object allocation
//////////////////////////////////////////////////////////////////////

static void *generate_object_allocation_stub_with_thread_pointer(char *fast_obj_alloc_proc,
                                                                 char *slow_obj_alloc_proc,
                                                                 char *stub_name)
{
    const int stub_size = 52+26;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_MAX/2, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;
// openTM integration 
//#ifdef PLATFORM_POSIX
    ss = call(ss, (char *)vm_get_gc_thread_local);
/*#else // !PLATFORM_POSIX
    *ss++ = (char)0x64;
    *ss++ = (char)0xa1;
    *ss++ = (char)0x14;
    *ss++ = (char)0x00;
    *ss++ = (char)0x00;
    *ss++ = (char)0x00;
    ss = alu(ss, add_opc,  eax_opnd,  Imm_Opnd((U_32)&((VM_thread *)0)->_gc_private_information));
#endif // !PLATFORM_POSIX
*/
    ss = push(ss,  eax_opnd);

    ss = push(ss,  M_Base_Opnd(esp_reg, 12));
    ss = push(ss,  M_Base_Opnd(esp_reg, 12));
    ss = call(ss, (char *)fast_obj_alloc_proc);
    ss = alu(ss, add_opc,  esp_opnd,  Imm_Opnd(12));

    ss = alu(ss, or_opc,  eax_opnd,  eax_opnd);

    ss = branch8(ss, Condition_Z,  Imm_Opnd(size_8, 0));
    char *backpatch_address__fast_alloc_failed = ((char *)ss) - 1;

    ss = ret(ss,  Imm_Opnd(8));

    signed offset = (signed)ss - (signed)backpatch_address__fast_alloc_failed - 1;
    *backpatch_address__fast_alloc_failed = (char)offset;
    
    ss = gen_setup_j2n_frame(ss);
// openTM integration 
//#ifdef PLATFORM_POSIX
    ss = call(ss, (char *)vm_get_gc_thread_local);
/*#else // !PLATFORM_POSIX
    *ss++ = (char)0x64;
    *ss++ = (char)0xa1;
    *ss++ = (char)0x14;
    *ss++ = (char)0x00;
    *ss++ = (char)0x00;
    *ss++ = (char)0x00;
    ss = alu(ss, add_opc,  eax_opnd,  Imm_Opnd((U_32)&((VM_thread *)0)->_gc_private_information));
#endif // !PLATFORM_POSIX
*/  
    ss = push(ss,  eax_opnd);
    ss = push(ss,  M_Base_Opnd(esp_reg, 8+m2n_sizeof_m2n_frame));
    ss = push(ss,  M_Base_Opnd(esp_reg, 8+m2n_sizeof_m2n_frame));
    ss = call(ss, (char *)slow_obj_alloc_proc);
    ss = alu(ss, add_opc,  esp_opnd,  Imm_Opnd(12));

    ss = gen_pop_j2n_frame(ss);

    ss = ret(ss,  Imm_Opnd(8));

    assert((ss - stub) <= stub_size);

    compile_add_dynamic_generated_code_chunk("object_allocation_stub_with_thread_pointer", false, stub, stub_size);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("object_allocation_stub_with_thread_pointer", stub, stub_size);
    }

    DUMP_STUB(stub, stub_name, ss - stub);

    return (void *)stub;
} //generate_object_allocation_stub_with_thread_pointer


static void *getaddress__vm_gethashcode_java_object_resolved_using_gethashcode_naked()
{
    const int stub_size = 16;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_MAX/2, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;

    ss = push(ss,  M_Base_Opnd(esp_reg, 4));
    ss = call(ss, (char *)gc_get_hashcode0);
    ss = alu(ss, add_opc,  esp_opnd,  Imm_Opnd(4));
    ss = ret(ss,  Imm_Opnd(4));
    assert((ss - stub) <= stub_size);

    compile_add_dynamic_generated_code_chunk("gethashcode_java_object_resolved_using_gethashcode_naked", false, stub, stub_size);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("gethashcode_java_object_resolved_using_gethashcode_naked", stub, stub_size);
    }

    DUMP_STUB(stub, "getaddress__vm_gethashcode_java_object_resolved_using_gethashcode_naked", ss - stub);

    return (void *)stub;
} //generate_object_allocation_stub_with_thread_pointer

static void *getaddress__vm_alloc_java_object_resolved_using_vtable_and_size_naked()
{
    static void *addr = 0;
    if (addr) {
        return addr;
    }

    addr = generate_object_allocation_stub_with_thread_pointer((char *) gc_alloc_fast,
        (char *) vm_malloc_with_thread_pointer,
        "getaddress__vm_alloc_java_object_resolved_using_thread_pointer_naked");
    
    return addr;
} //getaddress__vm_alloc_java_object_resolved_using_vtable_and_size_naked


static void* vm_aastore_nullpointer()
{
    static NativeCodePtr addr = NULL;
    if (addr) return addr;

    const int stub_size = 9;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_COLD, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;    

    Imm_Opnd imm1(12);
    ss = alu(ss, sub_opc,  esp_opnd,  imm1);
    ss = push(ss,  eax_opnd);
    ss = jump(ss, (char*)exn_get_rth_throw_null_pointer());

    addr = stub;
    assert((ss - stub) <= stub_size);

    DUMP_STUB(stub, "vm_aastore_nullpointer", ss - stub);

    return addr;
} //vm_aastore_nullpointer


static void* vm_aastore_array_index_out_of_bounds()
{
    static NativeCodePtr addr = NULL;
    if (addr) return addr;

    const int stub_size = 9;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_COLD, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;    

    Imm_Opnd imm1(12);
    ss = alu(ss, sub_opc,  esp_opnd,  imm1);
    ss = push(ss,  eax_opnd);
    ss = jump(ss, (char*)exn_get_rth_throw_array_index_out_of_bounds());

    addr = stub;
    assert((ss - stub) <= stub_size);

    compile_add_dynamic_generated_code_chunk("vm_aastore_nullpointer", false, stub, stub_size);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("vm_aastore_nullpointer", stub, stub_size);
    }

    DUMP_STUB(stub, "vm_aastore_array_index_out_of_bounds",  ss - stub);

    return addr;
} //vm_aastore_array_index_out_of_bounds


static void* vm_aastore_arraystore()
{
    static NativeCodePtr addr = NULL;
    if (addr != NULL) {
        return addr;
    }

    const int stub_size = 9;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_COLD, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;    

    Imm_Opnd imm1(12);
    ss = alu(ss, sub_opc,  esp_opnd,  imm1);
    ss = push(ss,  eax_opnd);
    ss = jump(ss, (char*)exn_get_rth_throw_array_store());

    addr = stub;
    assert((ss - stub) <= stub_size);

    compile_add_dynamic_generated_code_chunk("vm_aastore_arraystore", false, stub, stub_size);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("vm_aastore_arraystore", stub, stub_size);
    }

    DUMP_STUB(stub, "vm_aastore_arraystore", ss - stub);

    return addr;
} //vm_aastore_arraystore



static void *__stdcall
aastore_ia32(Vector_Handle array,
             int idx,
             volatile ManagedObject *elem);


// 20030321 This JIT support routine expects to be called directly from managed code. 
static void *__stdcall
aastore_ia32(Vector_Handle array,
            int idx,
            volatile ManagedObject *elem)
{
#ifdef REFS_RUNTIME_OR_COMPRESSED
    REFS_RUNTIME_SWITCH_IF
        // 20030321 Convert a null reference from a managed (heap_base) to an unmanaged null (NULL/0).
        if (elem == (volatile ManagedObject *)VM_Global_State::loader_env->managed_null) {
            elem = NULL;
        }
        if (array == (ManagedObject *)VM_Global_State::loader_env->managed_null) {
            array = NULL;
        }
    REFS_RUNTIME_SWITCH_ENDIF
#endif // REFS_RUNTIME_OR_UNCOMPRESSED

    assert ((elem == NULL) || (((ManagedObject *)elem)->vt() != NULL));
#ifdef VM_STATS
    VM_Statistics::get_vm_stats().num_aastore++;
#endif // VM_STATS
    void *new_eip = 0;
    if (array == NULL) {
        new_eip = vm_aastore_nullpointer();
    } else if ((unsigned)get_vector_length(array) <= (unsigned)idx) {
        new_eip = vm_aastore_array_index_out_of_bounds();
    } else {
        assert(idx >= 0);
        if (elem != NULL) {
            VTable *vt = get_vector_vtable(array);
#ifdef VM_STATS
            if (vt == cached_object_array_vtable_ptr)
                VM_Statistics::get_vm_stats().num_aastore_object_array ++;
            if (vt->clss->get_array_element_class()->get_vtable() == ((ManagedObject *)elem)->vt())
                VM_Statistics::get_vm_stats().num_aastore_equal_type ++;
            if (vt->clss->get_array_element_class()->get_fast_instanceof_flag())
                VM_Statistics::get_vm_stats().num_aastore_fast_decision ++;
#endif // VM_STATS
            if(vt == cached_object_array_vtable_ptr
                || class_is_subtype_fast(((ManagedObject *)elem)->vt(), vt->clss->get_array_element_class()))
            {
                STORE_REFERENCE((ManagedObject *)array, get_vector_element_address_ref(array, idx), (ManagedObject *)elem);
                return 0;           
            }
            new_eip = vm_aastore_arraystore();
        } else {
            // A null reference. No need to check types for a null reference.
            assert(elem == NULL);
#ifdef VM_STATS
            VM_Statistics::get_vm_stats().num_aastore_null ++;
#endif // VM_STATS
            // 20030502 Someone earlier commented out a call to the GC interface function gc_heap_slot_write_ref() and replaced it
            // by code to directly store a NULL in the element without notifying the GC. I've retained that change here but I wonder if
            // there could be a problem later with, say, concurrent GCs.
            ManagedObject **elem_ptr = get_vector_element_address_ref(array, idx);
            REF_INIT_BY_ADDR(elem_ptr, NULL);
            return 0;
        }
    }

    // This may possibly break if the C compiler applies very aggresive optimizations.
    void **saved_eip = ((void **)&elem) - 1;
    void *old_eip = *saved_eip;
    *saved_eip = new_eip;
    return old_eip;
} //aastore_ia32


static void *getaddress__vm_aastore()
{
    assert(VM_Global_State::loader_env->use_lil_stubs);
    static void *addr = NULL;
    if (addr != NULL) {
        return addr;
    }

    LilCodeStub* cs = lil_parse_code_stub(
        "entry 0:stdcall:ref,pint,ref:void;   // The args are the array to store into, the index, and the element ref to store\n"
        "in2out stdcall:pint; "
        "call %0i;                            // vm_rt_aastore either returns NULL or the ClassHandle of an exception to throw \n"
        "jc r!=0,aastore_failed; \
         ret; \
         :aastore_failed; \
         std_places 1; \
         sp0=r; \
         tailcall %1i;",
        (void *)vm_rt_aastore,
        exn_get_rth_throw_lazy_trampoline());
    assert(lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "vm_aastore", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);
    return addr;
} //getaddress__vm_aastore



static void * gen_new_vector_stub(char *stub_name, char *fast_new_vector_proc, char *slow_new_vector_proc)
{
    const int stub_size = 52;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_MAX/2, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;

    ss = push(ss,  M_Base_Opnd(esp_reg, 8));   // repush length
    ss = push(ss,  M_Base_Opnd(esp_reg, 8));   // repush vector_class
    ss = call(ss, (char *)fast_new_vector_proc);
    ss = alu(ss, add_opc,  esp_opnd,  Imm_Opnd(8));

    ss = alu(ss, or_opc,  eax_opnd,  eax_opnd);
    ss = branch8(ss, Condition_Z,  Imm_Opnd(size_8, 0));
    char *backpatch_address__fast_alloc_failed = ((char *)ss) - 1;

    ss = ret(ss,  Imm_Opnd(8));

    signed offset = (signed)ss - (signed)backpatch_address__fast_alloc_failed - 1;
    *backpatch_address__fast_alloc_failed = (char)offset;

    ss = gen_setup_j2n_frame(ss);
    ss = push(ss,  M_Base_Opnd(esp_reg, m2n_sizeof_m2n_frame+4));
    ss = push(ss,  M_Base_Opnd(esp_reg, m2n_sizeof_m2n_frame+4));
    ss = call(ss, (char *)slow_new_vector_proc);
    ss = alu(ss, add_opc,  esp_opnd,  Imm_Opnd(8));

    ss = gen_pop_j2n_frame(ss);

    ss = ret(ss,  Imm_Opnd(8));

    assert((ss - stub) <= stub_size);

    compile_add_dynamic_generated_code_chunk("vm_new_vector_naked", false, stub, stub_size);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("vm_new_vector_naked", stub, stub_size);
    }

    DUMP_STUB(stub, stub_name, ss - stub);

    return stub;
} //gen_new_vector_stub

static void *getaddress__vm_new_vector_using_vtable_naked() {
    static void *addr = 0;
    if (addr) {
        return addr;
    }
    
    addr = generate_object_allocation_stub_with_thread_pointer(
        (char *)vm_new_vector_or_null_using_vtable_and_thread_pointer,
        (char *)vm_rt_new_vector_using_vtable_and_thread_pointer,
        "getaddress__vm_new_vector_using_vtable_naked");
    return addr;
} //getaddress__vm_new_vector_using_vtable_naked

// this is a helper routine for rth_get_lil_multianewarray(see jit_runtime_support.cpp).
Vector_Handle
vm_rt_multianewarray_recursive(Class    *c,
                             int      *dims_array,
                             unsigned  dims);

Vector_Handle rth_multianewarrayhelper()
{
    ASSERT_THROW_AREA;
    M2nFrame* m2nf = m2n_get_last_frame();
    const unsigned max_dim = 255;
    int lens[max_dim];

#ifdef VM_STATS
    VM_Statistics::get_vm_stats().num_multianewarray++;  
#endif

    POINTER_SIZE_INT* p_args = m2n_get_args(m2nf);
    Class* c = (Class*)p_args[0];
    unsigned dims = p_args[1];
    assert(dims<=max_dim);
    U_32* lens_base = (U_32*)(p_args+2);
    for(unsigned i = 0; i < dims; i++) {
        lens[i] = lens_base[dims-i-1];
    }
    return vm_rt_multianewarray_recursive(c, lens, dims);
}

// This is a __cdecl function and the caller must pop the arguments.
static void *getaddress__vm_multianewarray_resolved_naked()
{
    static void *addr = 0;
    if (addr) {
        return addr;
    }

    const int stub_size = 47;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_DEFAULT, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;

    ss = gen_setup_j2n_frame(ss);
    ss = mov(ss,  ecx_opnd,  M_Base_Opnd(esp_reg, m2n_sizeof_m2n_frame+4));

    ss = lea(ss,  eax_opnd,  M_Index_Opnd(esp_reg, ecx_reg, m2n_sizeof_m2n_frame+4, 2));
    
    char *address_push_count_arg = (char *)ss;

    ss = push(ss,  M_Base_Opnd(eax_reg, 0) );
    ss = alu(ss, sub_opc,  eax_opnd,  Imm_Opnd(4));
    ss = dec(ss,  ecx_opnd);

    signed offset = (signed)address_push_count_arg - (signed)ss - 2;
    ss = branch8(ss, Condition_NZ,  Imm_Opnd(size_8, offset));

    ss = push(ss,  M_Base_Opnd(eax_reg, 0) );
    ss = push(ss,  M_Base_Opnd(eax_reg, -4) );

    ss = call(ss, (char *)vm_multianewarray_resolved);

    ss = mov(ss,  ecx_opnd,  M_Base_Opnd(esp_reg, +4) );

    ss = lea(ss,  esp_opnd,  M_Index_Opnd(esp_reg, ecx_reg, +8, 2) );

    ss = gen_pop_j2n_frame(ss);
    ss = ret(ss);

    addr = stub;
    assert((ss - stub) <= stub_size);

    compile_add_dynamic_generated_code_chunk("vm_multinewarray_resolved_naked", false, stub, stub_size);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("vm_multinewarray_resolved_naked", stub, stub_size);
    }

    DUMP_STUB(stub, "getaddress__vm_multianewarray_resolved_naked", ss - stub);

    return addr;
} //getaddress__vm_multianewarray_resolved_naked



static void *getaddress__vm_instantiate_cp_string_naked()
{
    static void *addr = 0;
    if (addr) {
        return addr;
    }

    const int stub_size = 52;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_COLD, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;
    ss = gen_setup_j2n_frame(ss);
    ss = push(ss,  M_Base_Opnd(esp_reg, m2n_sizeof_m2n_frame+4));
    ss = push(ss,  M_Base_Opnd(esp_reg, m2n_sizeof_m2n_frame+4));
    ss = call(ss, (char *)vm_instantiate_cp_string_slow);
    ss = alu(ss, add_opc,  esp_opnd,  Imm_Opnd(8));
    ss = gen_pop_j2n_frame(ss);
    ss = ret(ss,  Imm_Opnd(8) );
    addr = stub;
    assert((ss - stub) <= stub_size);

    compile_add_dynamic_generated_code_chunk("vm_instantiate_cp_string_naked", false, stub, stub_size);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("vm_instantiate_cp_string_naked", stub, stub_size);
    }

    DUMP_STUB(stub, "getaddress__vm_instantiate_cp_string_naked", ss - stub);

    return addr;
} //getaddress__vm_instantiate_cp_string_naked



static void vm_throw_java_lang_IncompatibleClassChangeError()
{
    ASSERT_THROW_AREA;
    exn_throw_by_name("java/lang/IncompatibleClassChangeError");
    LDIE(49, "The last called function should not return");
} //vm_throw_java_lang_IncompatibleClassChangeError



// 20030321 This JIT support routine expects to be called directly from managed code. 
void * getaddress__vm_get_interface_vtable_old_naked()  //wjw verify that this works
{
    static void *addr = 0;
    if (addr) {
        return addr;
    }

    const int stub_size = (REFS_IS_COMPRESSED_MODE ? 69 : 50);
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_DEFAULT, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;

    ss = (char *)gen_convert_managed_to_unmanaged_null_ia32((Emitter_Handle)ss, /*stack_pointer_offset*/ +4);

    ss = mov(ss,  edx_opnd,  M_Base_Opnd(esp_reg, +8) );
    ss = mov(ss,  ecx_opnd,  M_Base_Opnd(esp_reg, +4) );
    ss = alu(ss, or_opc,  ecx_opnd,  ecx_opnd);

    ss = branch8(ss, Condition_Z,  Imm_Opnd(size_8, 0));
    char *backpatch_address__null_reference = ((char *)ss) - 1;

    ss = push(ss,  M_Base_Opnd(esp_reg, 8));
    ss = push(ss,  M_Base_Opnd(esp_reg, 8));
    ss = call(ss, (char *)vm_get_interface_vtable);
    ss = alu(ss, add_opc,  esp_opnd,  Imm_Opnd(8));

    ss = alu(ss, or_opc,  eax_opnd,  eax_opnd);

    ss = branch8(ss, Condition_Z,  Imm_Opnd(size_8, 0));
    char *backpatch_address__interface_not_found = ((char *)ss) - 1;

    ss = ret(ss,  Imm_Opnd(8) );

    signed offset = (signed)ss - (signed)backpatch_address__interface_not_found - 1;
    *backpatch_address__interface_not_found = (char)offset;

    ss = gen_setup_j2n_frame(ss);

    ss = call(ss, (char *)vm_throw_java_lang_IncompatibleClassChangeError);

    offset = (signed)ss - (signed)backpatch_address__null_reference - 1;
    *backpatch_address__null_reference = (char)offset;

    ss = alu(ss, xor_opc,  eax_opnd,  eax_opnd);
    ss = ret(ss,  Imm_Opnd(8) );
    
    addr = stub;
    assert((ss - stub) <= stub_size);

    compile_add_dynamic_generated_code_chunk("vm_get_interface_vtable_old_naked", false, stub, stub_size);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("vm_get_interface_vtable_old_naked", stub, stub_size);
    }

    DUMP_STUB(stub, "getaddress__vm_get_interface_vtable_old_naked", ss - stub);

    return addr;
} //getaddress__vm_get_interface_vtable_old_naked


static void vm_throw_java_lang_ArithmeticException()
{
    ASSERT_THROW_AREA;
    assert(!hythread_is_suspend_enabled());
    exn_throw_by_name("java/lang/ArithmeticException");
    LDIE(49, "The last called function should not return");
} //vm_throw_java_lang_ArithmeticException


static void* getaddress__setup_java_to_native_frame()
{
    static void *addr = 0;
    if (addr) {
        return addr;
    }

    const int stub_size = 3+m2n_push_m2n_size(false, 1)+1+1;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_MAX/2, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;

    ss = xchg(ss,  M_Base_Opnd(esp_reg, 0),  ebp_opnd, size_32);
    ss = m2n_gen_push_m2n(ss, NULL, FRAME_UNKNOWN, false, 1);
    ss = push(ss,  ebp_opnd);
    ss = ret(ss);
    
    assert((ss - stub) <= stub_size);
    addr = stub;

    compile_add_dynamic_generated_code_chunk("setup_java_to_native_frame", false, stub, stub_size);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("setup_java_to_native_frame", stub, stub_size);
    }

    DUMP_STUB(stub, "getaddress__setup_java_to_native_frame", ss - stub);

    return addr;
} //getaddress__setup_java_to_native_frame


char *gen_setup_j2n_frame(char *s)
{
    s = call(s, (char *)getaddress__setup_java_to_native_frame() );
    return s;
} //setup_j2n_frame


static void* getaddress__pop_java_to_native_frame()
{
    static void *addr = 0;
    if (addr) {
        return addr;
    }

    const int stub_size = 1+m2n_pop_m2n_size(false, 0, 0, 2)+1+1;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_MAX/2, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;

    ss = pop(ss,  edx_opnd);
    ss = m2n_gen_pop_m2n(ss, false, 0, 0, 2);
    ss = push(ss,  edx_opnd);
    ss = ret(ss);

    assert((ss - stub) <= stub_size);
    addr = stub;

    compile_add_dynamic_generated_code_chunk("pop_java_to_native_frame", false, stub, stub_size);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("pop_java_to_native_frame", stub, stub_size);
    }

    DUMP_STUB(stub, "getaddress__pop_java_to_native_frame", ss - stub);

    return addr;
} //getaddress__pop_java_to_native_frame


char *gen_pop_j2n_frame(char *s)
{
    s = call(s, (char *)getaddress__pop_java_to_native_frame() );
    return s;
} //setup_j2n_frame


/////////////////////////////////////////////////////////////////
// end VM_Runtime_Support
/////////////////////////////////////////////////////////////////


static void
vm_throw_linking_exception(Class *clss,
                           unsigned cp_index,
                           unsigned opcode)
{
    TRACE("vm_throw_linking_exception, idx=" << cp_index << "\n");
    vm_rt_class_throw_linking_error(clss, cp_index, opcode);
    LDIE(49, "The last called function should not return");
} //vm_throw_linking_exception


void * getaddress__vm_throw_linking_exception_naked()
{
    static void *addr = 0;
    if (addr) {
        return addr;
    }

    const int stub_size = 100;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_COLD, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;

    ss = gen_setup_j2n_frame(ss);
    ss = push(ss,  M_Base_Opnd(esp_reg, m2n_sizeof_m2n_frame+8));
    ss = push(ss,  M_Base_Opnd(esp_reg, m2n_sizeof_m2n_frame+8));
    ss = push(ss,  M_Base_Opnd(esp_reg, m2n_sizeof_m2n_frame+8));
    ss = call(ss, (char *)vm_throw_linking_exception);
    
    addr = stub;
    assert((ss - stub) < stub_size);

    compile_add_dynamic_generated_code_chunk("vm_throw_linking_exception_naked", false, stub, stub_size);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("vm_throw_linking_exception_naked", stub, stub_size);
    }

    DUMP_STUB(stub, "getaddress__vm_throw_linking_exception_naked", ss - stub);

    return addr;
} //getaddress__vm_throw_linking_exception_naked


#ifdef VM_STATS

static void register_request_for_rt_function(VM_RT_SUPPORT f) {
    // Increment the number of times that f was requested by a JIT. This is not the number of calls to that function,
    // but this does tell us how often a call to that function is compiled into JITted code.
    VM_Statistics::get_vm_stats().rt_function_requests.add((void *)f, /*value*/ 1, /*value1*/ NULL);
} //register_request_for_rt_function

#endif //VM_STATS

void * getaddress__vm_monitor_enter_naked();
void * getaddress__vm_monitor_enter_static_naked();
void * getaddress__vm_monitor_exit_naked();

/**
  * Returns fast monitor exit static function.
  */
void * getaddress__vm_monitor_exit_static_naked();
void *vm_helper_get_addr(VM_RT_SUPPORT f)
{
#ifdef VM_STATS
    register_request_for_rt_function(f);
#endif // VM_STATS

        NativeCodePtr res = rth_get_lil_helper(f);
        if (res) return res;

    switch(f) {
    case VM_RT_THROW:
    case VM_RT_THROW_SET_STACK_TRACE:
        return exn_get_rth_throw();
    case VM_RT_THROW_LAZY:
        return exn_get_rth_throw_lazy();
    case VM_RT_LDC_STRING:
        return getaddress__vm_instantiate_cp_string_naked();
    case VM_RT_NEW_RESOLVED_USING_VTABLE_AND_SIZE:
        return getaddress__vm_alloc_java_object_resolved_using_vtable_and_size_naked(); 
    case VM_RT_MULTIANEWARRAY_RESOLVED:
        return getaddress__vm_multianewarray_resolved_naked();
    case VM_RT_NEW_VECTOR_USING_VTABLE:
        return getaddress__vm_new_vector_using_vtable_naked();
    case VM_RT_AASTORE:
        if (VM_Global_State::loader_env->use_lil_stubs) {
            return getaddress__vm_aastore();
        } else {
            return (void *)aastore_ia32;
        }
    case VM_RT_AASTORE_TEST:
        return (void *)vm_aastore_test;
    case VM_RT_CHECKCAST:
        return getaddress__vm_checkcast_naked();

    case VM_RT_INSTANCEOF:
    return getaddress__vm_instanceof();

    case VM_RT_MONITOR_ENTER:
        return getaddress__vm_monitor_enter_naked();
    case VM_RT_MONITOR_EXIT:
        return getaddress__vm_monitor_exit_naked();


    case VM_RT_GET_INTERFACE_VTABLE_VER0:
        return getaddress__vm_get_interface_vtable_old_naked();  //tryitx
    case VM_RT_INITIALIZE_CLASS:
        return getaddress__vm_initialize_class_naked();
    case VM_RT_THROW_LINKING_EXCEPTION:
        return getaddress__vm_throw_linking_exception_naked();

    case VM_RT_GC_HEAP_WRITE_REF:
        return (void*)gc_heap_slot_write_ref;
            
    case VM_RT_GET_IDENTITY_HASHCODE:
        return getaddress__vm_gethashcode_java_object_resolved_using_gethashcode_naked();
        
    default:
        LDIE(50, "Unexpected helper id {0}" << f);
        return 0;
    }
} //vm_helper_get_addr



/**************************************************
 * The following code has to do with the LIL stub inlining project.
 * Modifying it should not affect anything.
 */



/* ? 03/07/30: temporary interface change!!! */
void *vm_helper_get_addr_optimized(VM_RT_SUPPORT f, Class_Handle c) {
    if (c == NULL)
    {
        return vm_helper_get_addr(f);
    }

    switch (f) {
    case VM_RT_CHECKCAST:
            return vm_helper_get_addr(f);
        // break; // remark #111: statement is unreachable
    case VM_RT_INSTANCEOF:
            return vm_helper_get_addr(f);
        // break;// remark #111: statement is unreachable
    case VM_RT_NEW_RESOLVED_USING_VTABLE_AND_SIZE:
        if (c->has_finalizer())
            return getaddress__vm_alloc_java_object_resolved_using_vtable_and_size_naked();
        else
            return vm_helper_get_addr(f);
        // break;// remark #111: statement is unreachable
    default:
        return vm_helper_get_addr(f);
        // break;// remark #111: statement is unreachable
    }
}

/*
 * LIL inlining code - end
 **************************************************/
