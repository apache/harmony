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
 * @author Intel, Evgueni Brevnov
 */  
//

// Functions needed to compile a method.
// For methods without the "native" attribute, we invoke a JIT, for those that
// have the "native" attribute, we locate the native function and generate the
// appropriate stub.
//

//MVM
#include <iostream>

using namespace std;

#include <stdlib.h>
#include <stdio.h>
#include <assert.h>

#define LOG_DOMAIN "vm.core"
#include "cxxlog.h"

#include "open/types.h"
#include "open/vm_type_access.h"
#include "open/vm_field_access.h"
#include "open/vm_method_access.h"
#include "open/vm_class_manipulation.h"
#include "Class.h"
#include "environment.h"
#include "m2n.h"
#include "../m2n_ipf_internal.h"
#include "exceptions.h"
#include "jit_intf.h"
#include "jit_intf_cpp.h"

#include "merced.h"

#include "jit_runtime_support.h"
#include "object_layout.h"
#include "nogc.h"
#include "open/gc.h"
#include "vm_threads.h"

#include "ini.h"

#define METHOD_NAME_BUF_SIZE 512

#include "vm_ipf.h"

#include "compile.h"

#include "Code_Emitter.h"

#include "lil.h"
#include "lil_code_generator.h"
#include "stub_code_utils.h"

#include "dump.h"
#include "vm_stats.h"

void emit_hashcode_override(Emitter_Handle eh, Method *method);
void emit_arraycopy_override(Emitter_Handle eh, Method *method);
void emit_system_currenttimemillis_override(Emitter_Handle eh, Method *method);
void emit_readinternal_override(Emitter_Handle eh, Method *method);

void flush_hw_cache(U_8* addr, size_t len)
{
    for(unsigned int i = 0; i < len; i += 32) {
        flush_cache_line((void*)&(addr[i]));
    }
} //flush_hw_cache


/*    BEGIN UTILITIES TO CONVERT BETWEEN MANAGED AND UNMANAGED NULLS    */

// Convert a reference in register "reg", if null, from a managed null (heap_base) to an unmanaged one (NULL/0). Uses SCRATCH_GENERAL_REG.
void gen_convert_managed_to_unmanaged_null_ipf(Emitter_Handle emitter, unsigned reg) {
    assert(reg != SCRATCH_GENERAL_REG);
    assert(reg != SCRATCH_GENERAL_REG2); // because of the call to increment_stats_counter()
    if (VM_Global_State::loader_env->compress_references) {
        Merced_Code_Emitter *mce = (Merced_Code_Emitter *)emitter;
#ifdef VM_STATS
        increment_stats_counter(*mce, &VM_Statistics::get_vm_stats().num_convert_null_m2u);
#endif
        gen_compare_to_managed_null(*mce, SCRATCH_PRED_REG, SCRATCH_PRED_REG2, reg, SCRATCH_GENERAL_REG);
        mce->ipf_mov(reg, 0, SCRATCH_PRED_REG);
    } 
}

// Convert a reference in "reg", if null, from an unmanaged null (NULL/0) to an managed one (heap_base). 
// If a null must be translated, will rewrite rewrite "reg".
void gen_convert_unmanaged_to_managed_null_ipf(Emitter_Handle emitter, unsigned reg)
{
    assert(reg != SCRATCH_GENERAL_REG); // because of the call to increment_stats_counter()
    assert(reg != SCRATCH_GENERAL_REG2); // because of the call to increment_stats_counter()
    if (VM_Global_State::loader_env->compress_references) {
        Merced_Code_Emitter *mce = (Merced_Code_Emitter *)emitter;
#ifdef VM_STATS
        increment_stats_counter(*mce, &VM_Statistics::get_vm_stats().num_convert_null_u2m);
#endif
        mce->ipf_cmp(icmp_eq, cmp_none, SCRATCH_PRED_REG, SCRATCH_PRED_REG2, reg, 0);
        emit_mov_imm_compactor(*mce, reg, (uint64)VM_Global_State::loader_env->heap_base, SCRATCH_PRED_REG);
    } 
} //gen_convert_unmanaged_to_managed_null_ipf

/*    END UTILITIES TO CONVERT BETWEEN MANAGED AND UNMANAGED NULLS    */


/*    BEGIN SUPPORT FOR STUB OVERRIDE CODE SEQUENCES    */

// Override for the RNI method java_lang_Class.newInstance(Java_java_lang_Class *clss).
static void emit_newinstance_override(Emitter_Handle eh, Method *method) {
    Merced_Code_Emitter *mce = (Merced_Code_Emitter *)eh;
    Merced_Code_Emitter &emitter = *mce;
    // Emit code to examine the argument class (a java_lang_Class instance) and allocate an instance using fast inline code
    // if the class is instantiatable (e.g., is not an array or primitive), no constructor must be invoked, and no GC is required.
    Global_Env *env = VM_Global_State::loader_env;
    unsigned current_offset = 1;
    unsigned limit_offset = 1;
    if (//env->use_inline_newinstance_sequence &&
        gc_supports_frontier_allocation(&current_offset, &limit_offset)) {
        // Get the struct Class* that corresponds to the java_lang_Class reference argument.
        // (p1) g1 = in0 + env->vm_class_offset         // address of the struct Class* field in a java_lang_Class instance
        // (p1) g7 = [g1]                                // struct Class* corresponding to the argument java_lang_Class
        assert(env->vm_class_offset != 0);              // else VM bootstrapping isn't finished and this offset is unknown
        emitter.ipf_adds(SCRATCH_GENERAL_REG, (int)(env->vm_class_offset), IN_REG0);
        emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, SCRATCH_GENERAL_REG7, SCRATCH_GENERAL_REG);

        // Check that the argument class is instantiatable. Note: we use Void_Class here but any class will do.
        // g1 = g7 + offset_is_fast_allocation_possible  // address of (clss->m_is_fast_allocation_possible)
        // g2 = [g1]                                     // clss->m_is_fast_allocation_possible
        // p1 = (g2 != 0)                                // p1 = is clss->m_is_fast_allocation_possible?
        size_t offset_is_fast_allocation_possible = env->Void_Class->get_offset_of_fast_allocation_flag();

        emitter.ipf_adds(SCRATCH_GENERAL_REG, (int)offset_is_fast_allocation_possible, SCRATCH_GENERAL_REG7);
        emitter.ipf_ld(int_mem_size_1, mem_ld_none, mem_none, SCRATCH_GENERAL_REG2, SCRATCH_GENERAL_REG);
        emitter.ipf_cmp(icmp_ne, cmp_none, SCRATCH_PRED_REG, SCRATCH_PRED_REG2, 0, SCRATCH_GENERAL_REG2);

        // The following instructions are executed only if p1 (i.e., if clss->m_is_fast_allocation_possible).
        // This is a significantly modified version of the inline code in get_vm_rt_new_with_thread_pointer_compactor().
        // Do the fast path without creating a frame. This sequence uses predicated instructions rather than branching,
        // although I wonder if it has become too long for this to be effective.
        //
        // (p1) g1 = r4 + offset_gc_local                // address of current
        // (p1) g2 = r4 + offset_gc_local+sizeof(void*)  // address of limit
        // (p1) r8 = [g1]                                // current
        // (p1) g3 = [g2]                                // limit
        //
        // (p1) g5 = g7 + offset_instance_data_size      // address of the class's size
        // (p1) g6 = [g5]                                // size
        // (p1) g4 = r8 + g6                             // current+size
        // (p1) p3 = (g4 <= g3)                          // p3 = is (current+size <= limit)?
        //
        // (p3) g2 = g7 + offset_allocation_handle       // address of clss->allocation_handle (a compressed/uncompressed vtable pointer)
        // (p3) g3 = [g2]                                // vtable (which is compressed if vm_vtables_are_compressed())
        // (p3) [r8] = g3                                // write vtable to the newly allocated object
        // (p3) [g1] = g4                                // set new current value
        // (p3) return
        size_t offset_gc_local           = (U_8*)&(p_TLS_vmthread->_gc_private_information) - (U_8*)p_TLS_vmthread;
        size_t offset_allocation_handle  = env->Void_Class->get_offset_of_allocation_handle();
        size_t offset_instance_data_size = env->Void_Class->get_offset_of_instance_data_size();
        current_offset += (unsigned) offset_gc_local;
        limit_offset += (unsigned) offset_gc_local;
        
        emitter.ipf_adds(SCRATCH_GENERAL_REG,  (int)current_offset,                 THREAD_PTR_REG, SCRATCH_PRED_REG);
        emitter.ipf_adds(SCRATCH_GENERAL_REG2, (int)limit_offset,                   THREAD_PTR_REG, SCRATCH_PRED_REG);
        emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, RETURN_VALUE_REG,     SCRATCH_GENERAL_REG,  SCRATCH_PRED_REG);
        emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, SCRATCH_GENERAL_REG3, SCRATCH_GENERAL_REG2, SCRATCH_PRED_REG);

        emitter.ipf_adds(SCRATCH_GENERAL_REG5, (int)offset_instance_data_size, SCRATCH_GENERAL_REG7, SCRATCH_PRED_REG);
        emitter.ipf_ld(int_mem_size_4, mem_ld_none, mem_none, SCRATCH_GENERAL_REG6, SCRATCH_GENERAL_REG5, SCRATCH_PRED_REG);
        emitter.ipf_add(SCRATCH_GENERAL_REG4, RETURN_VALUE_REG, SCRATCH_GENERAL_REG6, SCRATCH_PRED_REG);
        // Note the use of "cmp_unc" to unconditionally set both SCRATCH_PRED_REG3 and SCRATCH_PRED_REG4 to 0 (false) beforehand.
        emitter.ipf_cmp(icmp_le, cmp_unc, SCRATCH_PRED_REG3, SCRATCH_PRED_REG4, SCRATCH_GENERAL_REG4, SCRATCH_GENERAL_REG3,
                         /*cmp4*/ false, SCRATCH_PRED_REG);

        emitter.ipf_adds(SCRATCH_GENERAL_REG2, (int)offset_allocation_handle, SCRATCH_GENERAL_REG7, SCRATCH_PRED_REG3);
        emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, SCRATCH_GENERAL_REG3, SCRATCH_GENERAL_REG2, SCRATCH_PRED_REG3);
        if (vm_is_vtable_compressed())
        {
            emitter.ipf_st(int_mem_size_4, mem_st_none, mem_none, RETURN_VALUE_REG,     SCRATCH_GENERAL_REG3, SCRATCH_PRED_REG3);
        }
        else
        {
            emitter.ipf_st(int_mem_size_8, mem_st_none, mem_none, RETURN_VALUE_REG,     SCRATCH_GENERAL_REG3, SCRATCH_PRED_REG3);
        }
        emitter.ipf_st(int_mem_size_8, mem_st_none, mem_none, SCRATCH_GENERAL_REG,  SCRATCH_GENERAL_REG4, SCRATCH_PRED_REG3);
        emitter.ipf_brret(br_many, br_sptk, br_none, BRANCH_RETURN_LINK_REG, SCRATCH_PRED_REG3);   
    }
}

static void emit_convertToByteArray_override(Emitter_Handle eh, Method *method) {
    Merced_Code_Emitter *mce = (Merced_Code_Emitter *)eh;
    Merced_Code_Emitter &emitter = *mce;
    const int v0 = RETURN_VALUE_REG;
    const int carray = IN_REG0;
    const int barray = IN_REG1;
    const int first = IN_REG2;
    const int sc1 = SCRATCH_GENERAL_REG;
    const int sc2 = SCRATCH_GENERAL_REG2;
    const int sc3 = SCRATCH_GENERAL_REG3;
    const int sc4 = SCRATCH_GENERAL_REG4;
    const int sc5 = SCRATCH_GENERAL_REG5;
    const int sc6 = SCRATCH_GENERAL_REG6;
    const int sc7 = SCRATCH_GENERAL_REG7;
    const int sc8 = SCRATCH_GENERAL_REG8;
    const int p1 = SCRATCH_PRED_REG;
    const int p2 = SCRATCH_PRED_REG2;
    const int p3 = SCRATCH_PRED_REG3;
    const int p4 = SCRATCH_PRED_REG4;
    const int length_offset = vector_length_offset();
    const int first_char_offset = vector_first_element_offset(VM_DATA_TYPE_CHAR);
    const int first_byte_offset = vector_first_element_offset(VM_DATA_TYPE_INT8);
    const int target_loop = 2;
    const int target_end = 3;
    const int target_fail = 4;
    const int target_loopExit = 5;
    // v0 = barray
    // sc1 = &barray->length
    // sc2 = [sc1]
    // cmp p1,p2 = (sc2 == 0)
    // cmp p3,p4 = (sc2 == 1)
    // (p1) br.ret
    // sc2 = sc2 - 2
    // sc3 = ar.lc
    // ar.lc = sc2
    // sc4 = &carray[0];
    // sc5 = first<<1 + sc4
    // sc6 = &barray[0];
    // sc7 = 0xff;
    // sc8 = [sc5],2
    // (p3) br.cond loopExit
    // loop:
    // [sc6],1 = sc8
    // cmp p1,p2 = (sc8 > sc7)
    // (p1) br.cond fail
    // sc8 = [sc5],2
    // br.cloop loop
    // loopExit:
    // [sc6],1 = sc8
    // cmp p1,p2 = (sc8 > sc7)
    // (p1) br.cond fail
    // end:
    // ar.lc = sc3
    // br.ret
    // fail:
    // v0 = 0
    // br end

    emitter.ipf_mov(v0, barray);
    emitter.ipf_adds(sc1, length_offset, barray);
    emitter.ipf_ld(int_mem_size_4, mem_ld_none, mem_none, sc2, sc1);
    emitter.ipf_cmp(icmp_eq, cmp_none, p1, p2, sc2, 0);
    emitter.ipf_cmpi(icmp_eq, cmp_none, p3, p4, 1, sc2);
    emitter.ipf_brret(br_many, br_spnt, br_none, BRANCH_RETURN_LINK_REG, p1);
    emitter.ipf_adds(sc2, -2, sc2);
    emitter.ipf_mfap(sc3, AR_lc);
    emitter.ipf_mtap(AR_lc, sc2);
    emitter.ipf_adds(sc4, first_char_offset, carray);
    emitter.ipf_shladd(sc5, first, 1, sc4);
    emitter.ipf_adds(sc6, first_byte_offset, barray);
    emitter.ipf_movi(sc7, 0xff);
    emitter.ipf_ld_inc_imm(int_mem_size_2, mem_ld_none, mem_none, sc8, sc5, 2);
    emitter.ipf_br(br_cond, br_few, br_spnt, br_none, target_loopExit, p3);
    emitter.set_target(target_loop);
    emitter.ipf_st_inc_imm(int_mem_size_1, mem_st_none, mem_none, sc6, sc8, 1);
    emitter.ipf_cmp(icmp_gt, cmp_none, p1, p2, sc8, sc7);
    emitter.ipf_br(br_cond, br_few, br_spnt, br_none, target_fail, p1);
    emitter.ipf_ld_inc_imm(int_mem_size_2, mem_ld_none, mem_none, sc8, sc5, 2);
    emitter.ipf_br(br_cloop, br_few, br_sptk, br_none, target_loop);
    emitter.set_target(target_loopExit);
    emitter.ipf_st_inc_imm(int_mem_size_1, mem_st_none, mem_none, sc6, sc8, 1);
    emitter.ipf_cmp(icmp_gt, cmp_none, p1, p2, sc8, sc7);
    emitter.ipf_br(br_cond, br_few, br_spnt, br_none, target_fail, p1);
    emitter.set_target(target_end);
    emitter.ipf_mtap(AR_lc, sc3);
    emitter.ipf_brret(br_many, br_spnt, br_none, BRANCH_RETURN_LINK_REG);
    emitter.set_target(target_fail);
    emitter.ipf_mov(v0, 0);
    emitter.ipf_br(br_cond, br_few, br_sptk, br_none, target_end);
}

static unsigned default_override_size(Method *m)
{
    LDIE(51, "Not implemented");
    return 0;
}

static Stub_Override_Entry _stub_override_entries_base[] = {
    {"java/lang/Class", "newInstance", "()Ljava/lang/Object;", emit_newinstance_override, default_override_size},
    {"java/lang/VMSystem", "identityHashCode", "(Ljava/lang/Object;)I", emit_hashcode_override, default_override_size},
    {"java/lang/VMSystem", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", emit_arraycopy_override, default_override_size},
    {"java/lang/System", "currentTimeMillis", "()J", emit_system_currenttimemillis_override, default_override_size},
    {"java/lang/String", "convertToByteArray0", "([C[BI)[B", emit_convertToByteArray_override, default_override_size},
    {"java/io/FileInputStream", "readInternal", "(I[BII)I", emit_readinternal_override, default_override_size}
};

Stub_Override_Entry *stub_override_entries = &(_stub_override_entries_base[0]);

int sizeof_stub_override_entries = sizeof(_stub_override_entries_base) / sizeof(_stub_override_entries_base[0]);

/*    END SUPPORT FOR STUB OVERRIDE CODE SEQUENCES    */


void compile_flush_generated_code_block(U_8* b, size_t sz) {
    flush_hw_cache(b, sz);
}

extern "C" void do_mf_asm();

extern "C" void do_mf() {
    do_mf_asm();
}

void compile_flush_generated_code() {
    sync_i_cache();
    do_mf();
}

// 20030711: This function might not be compressed references safe
static void protect_value_type(Class_Handle c, uint64* start, GcFrame* gc) {
    DIE(("It is supposed that the function is never called"));
    assert(!hythread_is_suspend_enabled());
    unsigned num_fields = class_num_instance_fields_recursive(c);
    for(unsigned i=0; i<num_fields; i++) {
        Field_Handle f = class_get_instance_field_recursive(c, i);
        Type_Info_Handle ftih = field_get_type_info(f);
        unsigned offset = 0;
        // field_get_offset_unboxed asserts 0 anyway
        switch (type_info_get_type(ftih)) {
        case VM_DATA_TYPE_CLASS:
        case VM_DATA_TYPE_ARRAY:
            assert((offset&7) == 0);
            gc->add_object((ManagedObject**)(start+(offset>>3)));
            break;
        case VM_DATA_TYPE_MP:
            assert((offset&7) == 0);
            gc->add_managed_pointer((ManagedPointer*)(start+(offset>>3)));
            break;
        case VM_DATA_TYPE_VALUE:
        {
            // This should never cause loading
            Class_Handle fc = type_info_get_class(ftih);
            assert(fc);
            assert((offset&7) == 0);
            protect_value_type(fc, start+(offset>>3), gc);
            break;
        }
        default:
            // Ignore everything else
            break;
        }
    }
}

void compile_protect_arguments(Method_Handle method, GcFrame* gc) {
    assert(!hythread_is_suspend_enabled());
    Method_Signature_Handle msh = method_get_signature(method);

    if (msh == NULL) {
        return;
    }

    assert(msh);
    unsigned num_args = method_args_get_number(msh);
    M2nFrame* m2nf = m2n_get_last_frame();

    unsigned cur_word = 0;
    for(unsigned i=0; i<num_args; i++) {
        Type_Info_Handle tih = method_args_get_type_info(msh, i);
        switch (type_info_get_type(tih)) {
        case VM_DATA_TYPE_INT64:
        case VM_DATA_TYPE_UINT64:
        case VM_DATA_TYPE_F8:
        case VM_DATA_TYPE_INT8:
        case VM_DATA_TYPE_UINT8:
        case VM_DATA_TYPE_INT16:
        case VM_DATA_TYPE_UINT16:
        case VM_DATA_TYPE_INT32:
        case VM_DATA_TYPE_UINT32:
        case VM_DATA_TYPE_INTPTR:
        case VM_DATA_TYPE_UINTPTR:
        case VM_DATA_TYPE_F4:
        case VM_DATA_TYPE_BOOLEAN:
        case VM_DATA_TYPE_CHAR:
        case VM_DATA_TYPE_UP:
            cur_word++;
            break;
        case VM_DATA_TYPE_CLASS:
        case VM_DATA_TYPE_ARRAY:
            gc->add_object((ManagedObject**)m2n_get_arg_word(m2nf, cur_word));
            cur_word++;
            break;
        case VM_DATA_TYPE_MP:
            gc->add_managed_pointer((ManagedPointer*)m2n_get_arg_word(m2nf, cur_word));
            break;
        case VM_DATA_TYPE_VALUE:
        {
            // 20030711: Must verify this with the calling convention
            LDIE(52, "Unexpected data type");
            // This should never cause loading
            Class_Handle c = type_info_get_class(tih);
            assert(c);
            protect_value_type(c, m2n_get_arg_word(m2nf, cur_word), gc);
            break;
        }
        default:
            LDIE(52, "Unexpected data type");
        }
    }
}

void patch_code_with_threads_suspended(U_8* code_block, U_8* new_code, size_t size) {
    // Check that the code being modified is one or more complete bundles on IPF.
    assert((((size_t)code_block) % 16) == 0);  // else did not start at a possible bundle address

    // 20030203 Check for current restrictions on code patching
    assert(size == 16);                        // currently support exactly one bundle
    // Check that the original bundle had the form: <template>, <slot 0 inst>, brl <offset0> or <template>, <slot 0 inst>, <nop.i>, br <offset0>
    uint64 original_bundle_low = *((uint64 *)code_block);
    unsigned UNUSED original_template = (unsigned)(original_bundle_low & 0x1F);
    assert((original_template == 4) || (original_template == 5) ||
           (original_template == 16) || (original_template == 17));  // else not mlx or mlxS or mib or mibS template
    // Should check for a brl instruction - need help on its encoding
    uint64 UNUSED original_inst0 = (original_bundle_low >> 5) & 0x1FfFfFfFfFf; 

    // Similarly, check that the new bundle has the same form
    uint64 new_bundle_low = *((uint64 *)code_block);
    unsigned UNUSED new_template = (unsigned)(new_bundle_low & 0x1F);
    assert(original_template == new_template);
    // Should check for a brl instruction
    uint64 UNUSED new_inst0 = (original_bundle_low >> 5) & 0x1FfFfFfFfFf; 
    assert(original_inst0 == new_inst0);

    memcpy(code_block, new_code, size);
}


/*    BEGIN COMPILE-ME STUBS    */

static NativeCodePtr compile_get_compile_me_generic() {
    static NativeCodePtr addr = NULL;
    if (!addr) {
        NativeCodePtr (*p_jitter)(Method*) = compile_me;
        void (*p_rethrow)() = exn_rethrow_if_pending;
        LilCodeStub* cs = lil_parse_code_stub(
            "entry 1:managed:arbitrary;"
            "push_m2n 0, %0i;"
            "m2n_save_all;"
            "out platform:pint:pint;"
            "o0=sp0;"
            "call %1i;"
	    "locals 1;"
	    "l0 = r;"
	    "out platform::void;"
            "call %2i;"
            "pop_m2n;"
            "tailcall l0;",
            FRAME_COMPILATION, p_jitter, p_rethrow);
        assert(cs && lil_is_valid(cs));
        addr = LilCodeGenerator::get_platform()->compile(cs);

        DUMP_STUB(addr, "compile_me_generic", lil_cs_get_code_size(cs));

        lil_free_code_stub(cs);
    }
    return addr;
}

NativeCodePtr compile_gen_compile_me(Method_Handle method) {
    LilCodeStub * cs = lil_parse_code_stub(
        "entry 0:managed:arbitrary;"
        "std_places 1;"
        "sp0=%0i;"
        "tailcall %1i;",
        method, lil_npc_to_fp(compile_get_compile_me_generic()));

    assert(cs && lil_is_valid(cs));
    
    NativeCodePtr addr = LilCodeGenerator::get_platform()->compile(cs);
#ifndef NDEBUG
    static unsigned done = 0;
    // dump first 10 compileme stubs
    if (dump_stubs && ++done <= 10) {
        char * buf;
        const char * c = class_get_name(method_get_class(method));
        const char * m = method_get_name(method);
        const char * d = method_get_descriptor(method);
        size_t sz = strlen(c)+strlen(m)+strlen(d)+12;
        buf = (char *)STD_MALLOC(sz);
        sprintf(buf, "compileme.%s.%s%s", c, m, d);
        assert(strlen(buf) < sz);
        DUMP_STUB(addr, buf, lil_cs_get_code_size(cs));
        STD_FREE(buf);
    }
#endif
    lil_free_code_stub(cs);
    return addr;
}

/*    END COMPILE-ME STUBS    */
