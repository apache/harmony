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
#define LOG_DOMAIN "vm.helpers"
#include "cxxlog.h"

#include "open/types.h"
#include "open/vm_type_access.h"
#include "open/vm_method_access.h"
#include "open/vm_util.h"
#include "environment.h"
#include "encoder.h"
#include "object_handles.h"
#include "vm_threads.h"
#include "compile.h"
#include "internal_jit_intf.h"

#include "nogc.h"
#include "m2n.h"
#include "../m2n_em64t_internal.h"
#include "exceptions.h"
#include "exceptions_jit.h"

#include "dump.h"
#include "vm_stats.h"

void compile_flush_generated_code_block(U_8*, size_t) {
    // Nothing to do on EM64T
}

void compile_flush_generated_code() {
    // Nothing to do on EM64T
}

void patch_code_with_threads_suspended(U_8* UNREF code_block, U_8* UNREF new_code, size_t UNREF size) {
    LDIE(47, "Not supported on EM64T currently");
}

void compile_protect_arguments(Method_Handle method, GcFrame * gc) {
    // adress of the top of m2n frame
    uint64 * const m2n_base_addr = (uint64 *)m2n_get_frame_base(m2n_get_last_frame());
     // 6(scratched registers on the stack)
    assert(m2n_get_size() % 8 == 0);
    // 14 = 0(alignment) + n(fp) + n(gp) registers were preserved on the stack
    uint64 * const inputs_addr = m2n_base_addr
            - (m2n_get_size() / 8) + 2
            - MAX_GR - MAX_FR;
     // 1(return ip);
#ifdef _WIN64
    // WIN64, reserve 4 words of shadow space
    //uint64 * extra_inputs_addr = m2n_base_addr + SHADOW/8 + 1;
    // but jit doesn't suppoert it now
    uint64 * extra_inputs_addr = m2n_base_addr + 1;
#else
    uint64 * extra_inputs_addr = m2n_base_addr + 1;
#endif

    assert(!hythread_is_suspend_enabled());
    Method_Signature_Handle msh = method_get_signature(method);

    if (msh == NULL) {
        return;
    }

    assert(msh);

    unsigned num_gp_used = 0;
#ifdef _WIN64
#define num_fp_used num_gp_used
#else // _WIN64
    unsigned num_fp_used = 0;
#endif // _WIN64
    for(unsigned i = 0; i < method_args_get_number(msh); i++) {
        Type_Info_Handle tih = method_args_get_type_info(msh, i);
        switch (type_info_get_type(tih)) {
        case VM_DATA_TYPE_INT64:
        case VM_DATA_TYPE_UINT64:
        case VM_DATA_TYPE_INT8:
        case VM_DATA_TYPE_UINT8:
        case VM_DATA_TYPE_INT16:
        case VM_DATA_TYPE_UINT16:
        case VM_DATA_TYPE_INT32:
        case VM_DATA_TYPE_UINT32:
        case VM_DATA_TYPE_INTPTR:
        case VM_DATA_TYPE_UINTPTR:
        case VM_DATA_TYPE_BOOLEAN:
        case VM_DATA_TYPE_CHAR:
        case VM_DATA_TYPE_UP:
            if (num_gp_used < MAX_GR) {
                ++num_gp_used;
            } else {
                ++extra_inputs_addr;
            }
            break;
        case VM_DATA_TYPE_CLASS:
        case VM_DATA_TYPE_ARRAY: {
            uint64 * ref_addr;
            if (num_gp_used < MAX_GR) {
                ref_addr =  inputs_addr + num_gp_used;
                ++num_gp_used;
            } else {
                ref_addr = extra_inputs_addr;
                ++extra_inputs_addr;
            }
            gc->add_object((ManagedObject**)ref_addr);
            break;
        }
        case VM_DATA_TYPE_MP: {
            uint64 * ref_addr;
            if (num_gp_used < MAX_GR) {
                ref_addr =  inputs_addr + num_gp_used;
                ++num_gp_used;
            } else {
                ref_addr = extra_inputs_addr;
                ++extra_inputs_addr;
            }
            gc->add_managed_pointer((ManagedPointer*)ref_addr);
            break;
        }
        case VM_DATA_TYPE_F4:
        case VM_DATA_TYPE_F8:
            if (num_fp_used < MAX_FR) {
                ++num_fp_used;
            } else {
                ++extra_inputs_addr;
            }
            break;
        case VM_DATA_TYPE_VALUE:
            LDIE(30, "This functionality is not currently supported");
        default:
            DIE(("Unexpected data type: %d", type_info_get_type(tih)));
        }
    }
}

// Convert a reference, if null, from a managed null
// (represented by heap_base) to an unmanaged one (NULL/0). Uses %rdi.
char * gen_convert_managed_to_unmanaged_null_em64t(char * ss,
                                                  const R_Opnd & input_param1) {
#ifdef REFS_RUNTIME_OR_COMPRESSED
    REFS_RUNTIME_SWITCH_IF
        ss = mov(ss, r11_opnd, Imm_Opnd(size_64, (int64)VM_Global_State::loader_env->heap_base));
        ss = alu(ss, cmp_opc, input_param1, r11_opnd, size_64);
        ss = branch8(ss, Condition_NE, Imm_Opnd(size_8, 0));  // not null, branch around the mov 0
        char *backpatch_address__not_managed_null = ((char *)ss) - 1;
        ss = mov(ss, input_param1, Imm_Opnd(0));
        POINTER_SIZE_SINT offset = (POINTER_SIZE_SINT)ss - (POINTER_SIZE_SINT)backpatch_address__not_managed_null - 1;
        *backpatch_address__not_managed_null = (char)offset;
    REFS_RUNTIME_SWITCH_ENDIF
#endif // REFS_RUNTIME_OR_COMPRESSED
    return ss;
}


/*    BEGIN COMPILE-ME STUBS    */

// compile_me stack frame
//    m2n frame
//    8 byte alignment
//    8 xmm registers on linux and 4 on windows
//    6 gp registers on linux and 4 on windows
//    0 byte shadow on linux and 32 byte on windows
//    method handle

// Stack size should be (% 8 == 0) but shouldn't be (% 16 == 0)
const int ALIGNMENT = 0;

const I_32 gr_stack_size = (1 + MAX_GR)*GR_STACK_SIZE
        + SHADOW;
const I_32 stack_size = (I_32)(m2n_get_size() - 2*sizeof(void*))
        + MAX_FR*FR_STACK_SIZE
        + gr_stack_size + ALIGNMENT;

static NativeCodePtr compile_get_compile_me_generic() {
    static NativeCodePtr addr = NULL;
    if (addr) {
        return addr;
    }

#ifdef _WIN64
    const int STUB_SIZE = 400;
#else
    const int STUB_SIZE = 416;
#endif
    char * stub = (char *) malloc_fixed_code_for_jit(STUB_SIZE,
        DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_DEFAULT, CAA_Allocate);
    addr = stub;
#ifndef NDEBUG
    memset(stub, 0xcc /*int 3*/, STUB_SIZE);
#endif
    assert(stack_size % 8 == 0);
    assert(stack_size % 16 != 0);

    // set up stack frame
    stub = alu(stub, sub_opc, rsp_opnd, Imm_Opnd(stack_size));

    // TODO: think over saving xmm registers conditionally
#ifndef _WIN64
    stub = movq(stub, M_Base_Opnd(rsp_reg, 7*FR_STACK_SIZE + gr_stack_size), xmm7_opnd);
    stub = movq(stub, M_Base_Opnd(rsp_reg, 6*FR_STACK_SIZE + gr_stack_size), xmm6_opnd);
    stub = movq(stub, M_Base_Opnd(rsp_reg, 5*FR_STACK_SIZE + gr_stack_size), xmm5_opnd);
    stub = movq(stub, M_Base_Opnd(rsp_reg, 4*FR_STACK_SIZE + gr_stack_size), xmm4_opnd);
#endif
    stub = movq(stub, M_Base_Opnd(rsp_reg, 3*FR_STACK_SIZE + gr_stack_size), xmm3_opnd);
    stub = movq(stub, M_Base_Opnd(rsp_reg, 2*FR_STACK_SIZE + gr_stack_size), xmm2_opnd);
    stub = movq(stub, M_Base_Opnd(rsp_reg, 1*FR_STACK_SIZE + gr_stack_size), xmm1_opnd);
    stub = movq(stub, M_Base_Opnd(rsp_reg, 0*FR_STACK_SIZE + gr_stack_size), xmm0_opnd);

    // we need to preserve all general purpose registers here
    // to protect managed objects from GC during compilation
#ifdef _WIN64
    stub = mov(stub, M_Base_Opnd(rsp_reg, (1 + 3)*GR_STACK_SIZE + SHADOW), r9_opnd);
    stub = mov(stub, M_Base_Opnd(rsp_reg, (1 + 2)*GR_STACK_SIZE + SHADOW), r8_opnd);
    stub = mov(stub, M_Base_Opnd(rsp_reg, (1 + 1)*GR_STACK_SIZE + SHADOW), rdx_opnd);
    stub = mov(stub, M_Base_Opnd(rsp_reg, (1 + 0)*GR_STACK_SIZE + SHADOW), rcx_opnd);
#else
    stub = mov(stub, M_Base_Opnd(rsp_reg, (1 + 5)*GR_STACK_SIZE + SHADOW), r9_opnd);
    stub = mov(stub, M_Base_Opnd(rsp_reg, (1 + 4)*GR_STACK_SIZE + SHADOW), r8_opnd);
    stub = mov(stub, M_Base_Opnd(rsp_reg, (1 + 3)*GR_STACK_SIZE + SHADOW), rcx_opnd);
    stub = mov(stub, M_Base_Opnd(rsp_reg, (1 + 2)*GR_STACK_SIZE + SHADOW), rdx_opnd);
    stub = mov(stub, M_Base_Opnd(rsp_reg, (1 + 1)*GR_STACK_SIZE + SHADOW), rsi_opnd);
    stub = mov(stub, M_Base_Opnd(rsp_reg, (1 + 0)*GR_STACK_SIZE + SHADOW), rdi_opnd);
#endif

    // push m2n to the stack
    stub = m2n_gen_push_m2n(stub, NULL,
        FRAME_COMPILATION, false, 0, 0, stack_size);

    // restore Method_Handle
#ifdef _WIN64
    stub = mov(stub, rcx_opnd, M_Base_Opnd(rsp_reg, 0 + SHADOW));
#else
    stub = mov(stub, rdi_opnd, M_Base_Opnd(rsp_reg, 0));
#endif

    // compile the method
    stub = call(stub, (char *)&compile_me);

    // pop m2n from the stack
    const I_32 bytes_to_m2n_bottom = (I_32)(stack_size - (m2n_get_size() - 2*sizeof(void*)));
    stub = m2n_gen_pop_m2n(stub, false, 0, bytes_to_m2n_bottom, 1);

    // restore gp inputs from the stack
#ifdef _WIN64
    stub = mov(stub, rcx_opnd, M_Base_Opnd(rsp_reg, (1 + 0)*GR_STACK_SIZE + SHADOW));
    stub = mov(stub, rdx_opnd, M_Base_Opnd(rsp_reg, (1 + 1)*GR_STACK_SIZE + SHADOW));
    stub = mov(stub, r8_opnd, M_Base_Opnd(rsp_reg, (1 + 2)*GR_STACK_SIZE + SHADOW));
    stub = mov(stub, r9_opnd, M_Base_Opnd(rsp_reg, (1 + 3)*GR_STACK_SIZE + SHADOW));
#else
    stub = mov(stub, rdi_opnd, M_Base_Opnd(rsp_reg, (1 + 0)*GR_STACK_SIZE + SHADOW));
    stub = mov(stub, rsi_opnd, M_Base_Opnd(rsp_reg, (1 + 1)*GR_STACK_SIZE + SHADOW));
    stub = mov(stub, rdx_opnd, M_Base_Opnd(rsp_reg, (1 + 2)*GR_STACK_SIZE + SHADOW));
    stub = mov(stub, rcx_opnd, M_Base_Opnd(rsp_reg, (1 + 3)*GR_STACK_SIZE + SHADOW));
    stub = mov(stub, r8_opnd, M_Base_Opnd(rsp_reg, (1 + 4)*GR_STACK_SIZE + SHADOW));
    stub = mov(stub, r9_opnd, M_Base_Opnd(rsp_reg, (1 + 5)*GR_STACK_SIZE + SHADOW));
#endif

    // restore fp inputs from the stack
    stub = movq(stub, xmm0_opnd, M_Base_Opnd(rsp_reg, 0*FR_STACK_SIZE + gr_stack_size));
    stub = movq(stub, xmm1_opnd, M_Base_Opnd(rsp_reg, 1*FR_STACK_SIZE + gr_stack_size));
    stub = movq(stub, xmm2_opnd, M_Base_Opnd(rsp_reg, 2*FR_STACK_SIZE + gr_stack_size));
    stub = movq(stub, xmm3_opnd, M_Base_Opnd(rsp_reg, 3*FR_STACK_SIZE + gr_stack_size));
#ifndef _WIN64
    stub = movq(stub, xmm4_opnd, M_Base_Opnd(rsp_reg, 4*FR_STACK_SIZE + gr_stack_size));
    stub = movq(stub, xmm5_opnd, M_Base_Opnd(rsp_reg, 5*FR_STACK_SIZE + gr_stack_size));
    stub = movq(stub, xmm6_opnd, M_Base_Opnd(rsp_reg, 6*FR_STACK_SIZE + gr_stack_size));
    stub = movq(stub, xmm7_opnd, M_Base_Opnd(rsp_reg, 7*FR_STACK_SIZE + gr_stack_size));
#endif

    // adjust stack pointer
    stub = alu(stub, add_opc, rsp_opnd, Imm_Opnd(stack_size));
    // transfer control to the compiled code
    stub = jump(stub, rax_opnd);
    
    assert(stub - (char *)addr <= STUB_SIZE);

    compile_add_dynamic_generated_code_chunk("compile_me_generic", false, addr, STUB_SIZE);
    if(jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("compile_me_generic", stub, STUB_SIZE);
    }

    DUMP_STUB(addr, "compileme_generic", stub - (char *)addr);

    return addr;
}

NativeCodePtr compile_gen_compile_me(Method_Handle method) {
    ASSERT_RAISE_AREA;

    int STUB_SIZE = 64;
#ifdef VM_STATS
    ++VM_Statistics::get_vm_stats().num_compileme_generated;
#endif
    char * stub = (char *) malloc_fixed_code_for_jit(STUB_SIZE,
        DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_DEFAULT, CAA_Allocate);
    NativeCodePtr addr = stub; 
#ifndef NDEBUG
    memset(stub, 0xcc /*int 3*/, STUB_SIZE);
#endif

#ifdef VM_STATS
    // FIXME: vm_stats_total is not yet initialized :-(
    //stub = mov(stub, r9_opnd, (int64)&VM_Statistics::get_vm_stats().num_compileme_used);
    //stub = inc(stub, M_Base_Opnd(r9_reg, 0));
#endif
    // preserve method handle
    stub = mov(stub, r10_opnd, Imm_Opnd(size_64, (int64)method));
    stub = mov(stub, M_Base_Opnd(rsp_reg, - stack_size + SHADOW), r10_opnd);
    // transfer control to generic part
    stub = jump(stub, (char *)compile_get_compile_me_generic());
    assert(stub - (char *)addr <= STUB_SIZE);

    char * name;
    const char* c = method->get_class()->get_name()->bytes;
    const char* m = method->get_name()->bytes;
    const char* d = method->get_descriptor()->bytes;
    size_t sz = strlen(c)+strlen(m)+strlen(d)+12;
    name = (char *)STD_MALLOC(sz);
    sprintf(name, "compileme.%s.%s%s", c, m, d);
    compile_add_dynamic_generated_code_chunk(name, true, addr, STUB_SIZE);

    if(jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event(name, addr, STUB_SIZE);
    }

#ifndef NDEBUG
    static unsigned done = 0;
    // dump first 10 compileme stubs
    if (dump_stubs && ++done <= 10) {
        char * buf;
        const char* c = method->get_class()->get_name()->bytes;
        const char* m = method->get_name()->bytes;
        const char* d = method->get_descriptor()->bytes;
        size_t sz = strlen(c)+strlen(m)+strlen(d)+12;
        buf = (char *)STD_MALLOC(sz);
        sprintf(buf, "compileme.%s.%s%s", c, m, d);
        assert(strlen(buf) < sz);
        DUMP_STUB(addr, buf, stub - (char *)addr);
        STD_FREE(buf);
    }
#endif
    return addr;
}

/*    END COMPILE-ME STUBS    */


/*    BEGIN SUPPORT FOR STUB OVERRIDE CODE SEQUENCES    */
// FIXME: as we now do not have native overrides on em64t
// we declare this array as having 1 element to make it compilable
// by Microsoft Visual C++ compilers and have 1 subtracted from
// sizeof_stub_override_entries to keep it of zero length.
// Once we have some NSO implemented on em64t that -1 should be removed.
static Stub_Override_Entry _stub_override_entries_base[1];

Stub_Override_Entry * stub_override_entries = &(_stub_override_entries_base[0]);

int sizeof_stub_override_entries = sizeof(_stub_override_entries_base) / sizeof(_stub_override_entries_base[0]) - 1; // <<< Remove -1 if NSO implemented;

/*    END SUPPORT FOR STUB OVERRIDE CODE SEQUENCES    */
