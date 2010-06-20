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
//  Routines used by the VM to emit stub code for the ipf. These routines use the ipf code compactor.
//

#include <assert.h>

#include "environment.h"
#include "merced.h"
#include "open/vm_util.h"
#include "vm_ipf.h"
#include "nogc.h"

#include "stub_code_utils.h"


void *get_vm_gp_value()
{
    void **function_pointer = (void **)get_vm_gp_value;
    return function_pointer[1];
}



void emit_alloc_for_single_call(Merced_Code_Emitter& emitter,
                                int num_in_args,
                                int num_out_args,
                                void **function,
                                int& out0_reg,
                                int& save_pfs_reg,
                                int& save_b0_reg,
                                int& save_gp_reg)
{
    bool should_save_gp = (function[1] != get_vm_gp_value());
    int num_local = (should_save_gp ? 3 : 2);
    save_pfs_reg = IN_REG0 + num_in_args;
    save_b0_reg = save_pfs_reg + 1;
    save_gp_reg = (should_save_gp ? save_b0_reg + 1 : 0);
    out0_reg = IN_REG0 + num_in_args + num_local;
    emitter.ipf_alloc(save_pfs_reg, num_in_args, num_local, num_out_args, 0);
    emitter.ipf_mfbr(save_b0_reg, BRANCH_RETURN_LINK_REG);
    if (should_save_gp)
        emitter.ipf_mov(save_gp_reg, GP_REG);
}



void emit_dealloc_for_single_call(Merced_Code_Emitter& emitter,
                                  int save_pfs_reg,
                                  int save_b0_reg,
                                  int save_gp_reg,
                                  int pred)
{
    if (save_gp_reg != 0)
        emitter.ipf_mov(GP_REG, save_gp_reg, pred);
    emitter.ipf_mtap(AR_pfs, save_pfs_reg, pred);
    emitter.ipf_mtbr(BRANCH_RETURN_LINK_REG, save_b0_reg, pred);
}

NativeCodePtr m2n_gen_flush_and_call();

void emit_call_with_gp(Merced_Code_Emitter& emitter,
                       void **proc_ptr,
                       bool flushrs,
                       int saved_gp_reg)
{
    void *new_gp = proc_ptr[1];
    void *vm_gp = get_vm_gp_value();

    // Set the gp according to the IPF software conventions. 
    if (new_gp != vm_gp)
    {
        if (saved_gp_reg != 0)
            emitter.ipf_mov(saved_gp_reg, GP_REG);
        emit_mov_imm_compactor(emitter, GP_REG, (uint64)new_gp);
    }

    uint64 branch_target = (uint64)(*proc_ptr);

    emit_mov_imm_compactor(emitter, SCRATCH_GENERAL_REG, branch_target, 0);
    if (flushrs) {
        emitter.ipf_mtbr(BRANCH_CALL_REG, SCRATCH_GENERAL_REG);
        emit_mov_imm_compactor(emitter, SCRATCH_GENERAL_REG, (uint64)m2n_gen_flush_and_call(), 0);
    }
    emitter.ipf_mtbr(SCRATCH_BRANCH_REG, SCRATCH_GENERAL_REG);
    emitter.ipf_bricall(br_few, br_sptk, br_none, BRANCH_RETURN_LINK_REG, SCRATCH_BRANCH_REG);

    // Restore the saved GP.
    if (new_gp != vm_gp && saved_gp_reg != 0)
        emitter.ipf_mov(GP_REG, saved_gp_reg);
} //emit_call_with_gp


void emit_branch_with_gp(Merced_Code_Emitter& emitter,
                         void **proc_ptr)
{
    void *new_gp = proc_ptr[1];
    void *vm_gp = get_vm_gp_value();

    // Set the gp according to the IPF software conventions. 
    if (new_gp != vm_gp)
    {
        emit_mov_imm_compactor(emitter, GP_REG, (uint64)new_gp);
    }

    uint64 branch_target = (uint64)(*proc_ptr);

    emit_mov_imm_compactor(emitter, SCRATCH_GENERAL_REG, branch_target, 0);
    emitter.ipf_mtbr(SCRATCH_BRANCH_REG, SCRATCH_GENERAL_REG);
    emitter.ipf_bri(br_cond, br_few, br_sptk, br_none, SCRATCH_BRANCH_REG);

    // someone else will restore the gp, according to the software conventions
} //emit_branch_with_gp


// Convenience procedure to emit a movl instruction using a 64 bit constant rather than two 32 bit constants.
void emit_movl_compactor(Merced_Code_Emitter& emitter, unsigned dst_reg, uint64 u64_value, unsigned pred)
{
    uint64 hi32 = (u64_value >> 32);
    uint64 lo32 = (u64_value & 0xFFffFFff);
    emitter.ipf_movl(dst_reg, (unsigned)hi32, (unsigned)lo32, pred);
} //emit_movl_compactor


// Convenience procedure to select an appropriate mov instruction for a 64-bit immediate
void emit_mov_imm_compactor(Merced_Code_Emitter& emitter, unsigned dst_reg, uint64 imm, unsigned pred)
{
    if (imm==0)
        emitter.ipf_mov(dst_reg, 0, pred);
    else if (0xffffffffffe00000<=imm || imm<0x1fffff)  // imm has a valid 22-bit signed rep
        emitter.ipf_movi(dst_reg, (int)imm, pred);
    else
        emit_movl_compactor(emitter, dst_reg, imm, pred);
} //emit_mov_imm_compactor


void *finalize_stub(Merced_Code_Emitter &emitter, const char *name)
{
    emitter.flush_buffer();
    size_t stub_size = emitter.get_size();
    void *stub = (void *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_DEFAULT, CAA_Allocate);
    emitter.copy((char *)stub);
    flush_hw_cache((U_8*)stub, stub_size);
    sync_i_cache();
    return stub;
} //finalize_stub



void increment_stats_counter(Merced_Code_Emitter &emitter, void *counter_addr, unsigned pred)
{
#ifdef VM_STATS
    const int counter_addr_reg              = SCRATCH_GENERAL_REG;
    const int counter_reg                   = SCRATCH_GENERAL_REG2;

    uint64 addr = (uint64)(counter_addr);
    emit_movl_compactor(emitter, counter_addr_reg, addr, pred);

    emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, counter_reg, counter_addr_reg, pred);
    emitter.ipf_adds(counter_reg, 1, counter_reg, pred);
    emitter.ipf_st(int_mem_size_8, mem_st_none, mem_none, counter_addr_reg, counter_reg, pred);
#endif // VM_STATS
} //increment_stats_counter



void gen_compare_to_managed_null(Merced_Code_Emitter &emitter,
                                 int predicate1, int predicate2, // predicate regs to set
                                 int src, // register to compare against
                                 int scratch // scratch register to use if needed
                                 )
{
    assert(VM_Global_State::loader_env->compress_references);
    const bool cmp4 = true;
    U_32 null_low = (U_32) (uint64) VM_Global_State::loader_env->managed_null;
    if (null_low == 0)
    {
        emitter.ipf_cmp(icmp_eq, cmp_none, predicate1, predicate2, src, 0, cmp4);
    }
    else
    {
        emit_mov_imm_compactor(emitter, scratch, null_low);
        emitter.ipf_cmp(icmp_eq, cmp_none, predicate1, predicate2, src, scratch, cmp4);
    }
}

Boolean jit_clears_ccv_in_monitor_enter()
{
    return FALSE;
}


void enforce_calling_conventions(Merced_Code_Emitter *emitter, int pred)
{
    emitter->ipf_mtap(AR_ccv, 0, pred);
}


// ST a helper function used by emit_print_reg
static void print_helper(char *str, POINTER_SIZE_INT arg) {
    fprintf(stderr, str, arg);
}


// ST a function that prints the value of a register, along with a message
// it presupposes the existence of two output regs; the values of these regs
// are preserved.
void emit_print_reg(Merced_Code_Emitter &emitter, char *msg, unsigned print_reg, unsigned num_inputs, unsigned first_output, bool do_alloc) {
    int out0, save_pfs, save_b0, save_gp;
    if (do_alloc) {
        // call alloc and save various things
        emit_alloc_for_single_call(emitter, num_inputs, 2,
                                   (void **) print_helper,
                                   out0, save_pfs, save_b0, save_gp);
    }
    else {
        out0 = first_output;
    }


    // push the two output regs on the stack
    emitter.ipf_st_inc_imm(int_mem_size_8, mem_st_spill, mem_none, SP_REG, out0, unsigned(-16));
    emitter.ipf_st_inc_imm(int_mem_size_8, mem_st_spill, mem_none, SP_REG, out0+1, unsigned(-16));
    // push all scratch regs on the stack
    for (unsigned i=14; i <= 31; i++) {
        emitter.ipf_st_inc_imm(int_mem_size_8, mem_st_spill, mem_none, SP_REG, i, unsigned(-16));
    }


    // fill in the two output regs
    emit_movl_compactor(emitter, out0, (uint64) msg);
    emitter.ipf_mov(out0+1, print_reg);
    // call a helper function
    emit_call_with_gp(emitter, (void **) print_helper, false);

    emitter.ipf_adds(SP_REG, 16, SP_REG);
    // restore the scratch regs
    for (unsigned i=31; i >= 14; i--)
        emitter.ipf_ld_inc_imm(int_mem_size_8, mem_ld_fill, mem_none, i, SP_REG, 16);

    // restore the two output regs
    emitter.ipf_ld_inc_imm(int_mem_size_8, mem_ld_fill, mem_none, out0+1, SP_REG, 16);
    emitter.ipf_ld(int_mem_size_8, mem_ld_fill, mem_none, out0, SP_REG);

    if (do_alloc) {
        // dealloc and restore various things
        emit_dealloc_for_single_call(emitter, save_pfs, save_b0, save_gp);
    }
}
