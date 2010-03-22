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
#include "Code_Emitter.h"
#include "environment.h"
#include "m2n.h"
#include "m2n_ipf_internal.h"
#include "vm_ipf.h"
#include "vm_threads.h"
#include "open/types.h"
#include "open/vm_util.h"
#include "stub_code_utils.h"
#include "interpreter.h"
#include "exceptions.h"


//////////////////////////////////////////////////////////////////////////
// Utilities

extern "C" void *do_flushrs_asm();

extern "C" void *do_flushrs()
{
    return do_flushrs_asm();
} //do_flushrs

// Given a bsp value for register 32 and a stacked register number
// return a pointer to where the stacked register is spilled
uint64* get_stacked_register_address(uint64* bsp, unsigned reg)
{
    if (interpreter_enabled()) {
        return interpreter.interpreter_get_stacked_register_address(bsp, reg);
    }
    assert(bsp && 32<=reg && reg<128);
    unsigned r = (reg-32)<<3;
    uint64 b = (uint64)bsp;
    uint64 d4 = b+r;
    uint64 d5 = (b&0x1f8)+r;
    if (d5>=63*8)
        if (d5>=126*8)
            d4 += 16;
        else
            d4 += 8;
    return (uint64*)d4;
}

// Get the bsp value for register 32 of the M2nFrame
uint64* m2n_get_bsp(M2nFrame* m2nf)
{
    return (uint64*)m2nf;
}

uint64* m2n_get_extra_saved(M2nFrame* m2nf)
{
    do_flushrs();
    return (uint64*)*get_stacked_register_address(m2n_get_bsp(m2nf), M2N_EXTRA_SAVED_PTR);
}

//////////////////////////////////////////////////////////////////////////
// M2nFrame Interface

//***** Generic Interface

// fill m2n frame as empty
void m2n_null_init(M2nFrame* m2n){
    memset(m2n, 0, sizeof(M2nFrame));
}

VMEXPORT // temporary solution for interpreter unplug
M2nFrame* m2n_get_last_frame()
{
    return (M2nFrame*)p_TLS_vmthread->last_m2n_frame;
}

VMEXPORT // temporary solution for interpreter unplug
M2nFrame* m2n_get_last_frame(VM_thread* thread)
{
    return (M2nFrame*)thread->last_m2n_frame;
}

VMEXPORT // temporary solution for interpreter unplug
void m2n_set_last_frame(M2nFrame* lm2nf)
{
    vm_thread_t vm_thread = jthread_self_vm_thread_unsafe();
    vm_thread->last_m2n_frame = lm2nf;
}

VMEXPORT
void m2n_set_last_frame(VM_thread* thread, M2nFrame* lm2nf)
{
    thread->last_m2n_frame = lm2nf;
}

VMEXPORT // temporary solution for interpreter unplug
M2nFrame* m2n_get_previous_frame(M2nFrame* m2nfl)
{
    assert(m2nfl);
    do_flushrs();
    return (M2nFrame*)*get_stacked_register_address(m2n_get_bsp(m2nfl), M2N_SAVED_M2NFL);
}

ObjectHandles* m2n_get_local_handles(M2nFrame* m2nf)
{
    assert(m2nf);
    do_flushrs();
    return (ObjectHandles*)*get_stacked_register_address(m2n_get_bsp(m2nf), M2N_OBJECT_HANDLES);
}

void m2n_set_local_handles(M2nFrame* m2nf, ObjectHandles* handles)
{
    assert(m2nf);
    do_flushrs();
    uint64* p_head = get_stacked_register_address(m2n_get_bsp(m2nf), M2N_OBJECT_HANDLES);
    *p_head = (uint64)handles;
}

NativeCodePtr m2n_get_ip(M2nFrame* m2nf)
{
    assert(m2nf);
    do_flushrs();
    uint64 * UNUSED bsp = (uint64 *)m2nf;
    assert(bsp);
    return (NativeCodePtr)*get_stacked_register_address(m2n_get_bsp(m2nf), M2N_SAVED_RETURN_ADDRESS);
}

// 20040708 New function - needs proper implementation.
void m2n_set_ip(M2nFrame* lm2nf, NativeCodePtr ip)
{
    assert(lm2nf);
    LDIE(51, "Not implemented");
}

// sets pointer to the registers used for jvmti PopFrame
void set_pop_frame_registers(M2nFrame* m2nf, Registers* regs) {
    // FIXME: not sure we want to support this function on IPF
    LDIE(51, "Not implemented");
}

// returns pointer to the registers used for jvmti PopFrame
Registers* get_pop_frame_registers(M2nFrame* m2nf) {
    // FIXME: not sure we want to support this function on IPF
    LDIE(51, "Not implemented");
    return 0;
}

Method_Handle m2n_get_method(M2nFrame* m2nf)
{
    assert(m2nf);
    do_flushrs();
    uint64 * UNUSED bsp = (uint64 *)m2nf;
    assert(bsp);
    return (Method_Handle)*get_stacked_register_address(m2n_get_bsp(m2nf), M2N_METHOD);
}

// Returns type of noted m2n frame
frame_type m2n_get_frame_type(M2nFrame* m2nf) {
    assert(m2nf);
    do_flushrs();
    uint64 * UNUSED bsp = (uint64 *)m2nf;
    assert(bsp);
    return (frame_type)*get_stacked_register_address(m2n_get_bsp(m2nf), M2N_FRAME_TYPE);
}

// Sets type of noted m2n frame
void m2n_set_frame_type(M2nFrame* m2nf, frame_type m2nf_type) {
    assert(m2nf);
    do_flushrs();
    uint64 * UNUSED bsp = (uint64 *)m2nf;
    assert(bsp);
    *get_stacked_register_address(m2n_get_bsp(m2nf), M2N_FRAME_TYPE) = m2nf_type;
}

size_t m2n_get_size() {
    return sizeof(M2nFrame);
}

//***** Stub Interface

// Flushes register stack of the current thread into backing store and calls target procedure.
NativeCodePtr m2n_gen_flush_and_call() {
    static NativeCodePtr addr = NULL;

    if (addr != NULL) {
        return addr;
    }

    tl::MemoryPool mem_pool;
    Merced_Code_Emitter emitter(mem_pool, 2, 0);
    emitter.disallow_instruction_exchange();
    emitter.memory_type_is_unknown();

    // We need to remember pfs & b0 here but there is no space to save them in.
    // Register stack contains valid outputs and we don't know how many registers are used.
    // Memory stack holds output values beyound those 8 which are on register stack.
    // The only place is general caller-saves registers. It is save to use them with out preserving
    // because they are alredy preserved by the corresponding M2N frame.

    // r4 is used to keep a thread pointer...so let's use r5 & r6.

    emitter.ipf_mfap(PRESERV_GENERAL_REG1, AR_pfs);
    emitter.ipf_mfbr(PRESERV_GENERAL_REG2, BRANCH_RETURN_LINK_REG);

    emitter.flush_buffer();
    emitter.ipf_flushrs();

    emitter.ipf_bricall(br_many, br_sptk, br_none, BRANCH_RETURN_LINK_REG, BRANCH_CALL_REG);

    emitter.ipf_mtbr(BRANCH_RETURN_LINK_REG, PRESERV_GENERAL_REG2);
    emitter.ipf_mtap(AR_pfs, PRESERV_GENERAL_REG1);

    emitter.ipf_brret(br_few, br_sptk, br_none, BRANCH_RETURN_LINK_REG);

    addr = finalize_stub(emitter, "");
    return addr;
}

unsigned m2n_gen_push_m2n(Merced_Code_Emitter* emitter, Method_Handle method, frame_type current_frame_type, bool handles, unsigned num_on_stack, unsigned num_local, unsigned num_out, bool do_alloc)
{
    // Allocate new frame
    if (do_alloc) {
        emitter->ipf_alloc(M2N_SAVED_PFS, 8, M2N_NUMBER_LOCALS+num_local, num_out, 0);

        // The alloc instruction saves pfs, now save return address and GP
        emitter->ipf_mfbr(M2N_SAVED_RETURN_ADDRESS, BRANCH_RETURN_LINK_REG);
        emitter->ipf_mov (M2N_SAVED_GP,             GP_REG);
    }

    // 20031205: The alloc writes the CFM and the mfpr reads it, so they must be separated by a stop bit, this is a brutal way of achieving this.
    emitter->flush_buffer();

    // Save predicates, SP, and callee saves general registers
    emitter->ipf_adds(M2N_SAVED_SP, num_on_stack, SP_REG);

    emitter->ipf_mfpr(M2N_SAVED_PR               );
    emitter->ipf_mfap(M2N_SAVED_UNAT,     AR_unat);
    emitter->ipf_mov (M2N_SAVED_R4,             4);
    emitter->ipf_mov (M2N_SAVED_R5,             5);
    emitter->ipf_mov (M2N_SAVED_R6,             6);
    emitter->ipf_mov (M2N_SAVED_R7,             7);

    // Set object handles to NULL and set method information
    emitter->ipf_mov(M2N_OBJECT_HANDLES, 0);
    emit_mov_imm_compactor(*emitter, M2N_METHOD, (uint64)method);
    emit_mov_imm_compactor(*emitter, M2N_FRAME_TYPE, (uint64)current_frame_type);

    const int P1 = SCRATCH_PRED_REG;
    const int P2 = SCRATCH_PRED_REG2;
    const int OLD_RSE_MODE = SCRATCH_GENERAL_REG2;
    const int NEW_RSE_MODE = SCRATCH_GENERAL_REG3;
    // SCRATCH_GENERAL_REG4 & SCRATCH_GENERAL_REG5 are reserved for std places.
    const int BSP = SCRATCH_GENERAL_REG6;
    const int IMM_8 = SCRATCH_GENERAL_REG7;
    const int IMM_1F8 = SCRATCH_GENERAL_REG8;
    const int TMP_REG = SCRATCH_GENERAL_REG9;
    // Scratch branch register.
    const int TMP_BRANCH_REG = 6;

    // Switch RSE to "forced lazy" mode. This is required to access RNAT.
    emitter->ipf_mfap(OLD_RSE_MODE, AR_rsc);
    emitter->ipf_dep(NEW_RSE_MODE, 0, OLD_RSE_MODE, 0, 2);
    emitter->ipf_mtap(AR_rsc, NEW_RSE_MODE);

    // Flush must be the first instruction in the group.
    emitter->flush_buffer();
    // Spill parent frames so that corresponding RNAT bits become valid.
    emitter->ipf_flushrs();
    // Extract backing store pointer
    emitter->ipf_mfap(BSP, AR_bsp);
    // Remember parent RNAT collection.
    emitter->ipf_mfap(M2N_EXTRA_RNAT, AR_rnat);

    // TODO: This is not fully legal reset nat bits for the whole m2n frame because it
    // contains r4-r7 general registers which may have corresponding unat bits up.
    emitter->ipf_mov(M2N_EXTRA_UNAT, 0);

/* The following code spills M2N into backing store.
    emitter->ipf_movl(IMM_1F8, 0, (uint64)0x1f8);
    emitter->ipf_movl(IMM_8, 0, (uint64)0x8);

    // Forcebly spill M2N frame into backing store.
    for(int i = M2N_NUMBER_INPUTS; i < M2N_NUMBER_LOCALS; i++) {
        emitter->ipf_and(TMP_REG, IMM_1F8, BSP);
        emitter->ipf_cmp(icmp_eq, cmp_none, P1, P2, IMM_1F8, TMP_REG);
        emitter->ipf_add(BSP, BSP, IMM_8, P1);
        emitter->ipf_st_inc_imm(int_mem_size_8, mem_st_spill, mem_none, BSP, 32 + i, 8);
    }

    // Remember UNAT collection for the current frame.
    emitter->ipf_sub(BSP, BSP, IMM_8);
    emitter->ipf_mfap(M2N_EXTRA_UNAT, AR_unat);
    emitter->ipf_st(int_mem_size_8, mem_st_none, mem_none, BSP, M2N_EXTRA_UNAT);

    // Restore original UNAT.
    emitter->ipf_mtap(AR_unat, M2N_SAVED_UNAT);
    emitter->flush_buffer();
*/

    // Switch RSE to the original mode.
    emitter->ipf_mtap(AR_rsc, OLD_RSE_MODE);

    // Link M2nFrame into list of current thread
    size_t offset_lm2nf = (size_t)&((VM_thread*)0)->last_m2n_frame;
    emitter->ipf_adds(SCRATCH_GENERAL_REG2, (int)offset_lm2nf, THREAD_PTR_REG);
    emitter->ipf_ld(int_mem_size_8, mem_ld_none, mem_none, M2N_SAVED_M2NFL, SCRATCH_GENERAL_REG2);
    emitter->ipf_mfap(SCRATCH_GENERAL_REG7, AR_bsp);
    emitter->ipf_st(int_mem_size_8, mem_st_none, mem_none, SCRATCH_GENERAL_REG2, SCRATCH_GENERAL_REG7);

    return 32+8+M2N_NUMBER_LOCALS;
}

void m2n_gen_set_local_handles(Merced_Code_Emitter* emitter, unsigned src_reg)
{
    emitter->ipf_mov(M2N_OBJECT_HANDLES, src_reg);
}

void m2n_gen_set_local_handles_imm(Merced_Code_Emitter* emitter, uint64 imm_val)
{
    int64 UNUSED imm = (int64)imm_val;
    assert(imm>=-0x200000 && imm<-0x200000);
    emitter->ipf_movi(M2N_OBJECT_HANDLES, (int)imm_val);
}

static void m2n_pop_local_handles() {
    assert(!hythread_is_suspend_enabled());

    exn_rethrow_if_pending();

    M2nFrame *m2n = m2n_get_last_frame();
    free_local_object_handles2(m2n_get_local_handles(m2n));
}

static void m2n_free_local_handles() {
    assert(!hythread_is_suspend_enabled());

    if (exn_raised()) {
        exn_rethrow();
    }

    M2nFrame * m2n = m2n_get_last_frame();
    // iche free_local_object_handles3(m2n->local_object_handles);
    free_local_object_handles3(m2n_get_local_handles(m2n)); // iche
}

void m2n_gen_pop_m2n(Merced_Code_Emitter* emitter, bool handles, M2nPreserveRet preserve_ret, bool do_alloc, unsigned out_reg, int target)
{
    unsigned free_target;
    
    if (handles) {
        assert(target != -1);  // make sure a target has been provided
        // Do we need to call free?
        free_target = (unsigned) target;
        emitter->ipf_cmp(icmp_eq, cmp_none, SCRATCH_PRED_REG, SCRATCH_PRED_REG2, M2N_OBJECT_HANDLES, 0);
        emitter->ipf_br(br_cond, br_many, br_spnt, br_none, free_target, SCRATCH_PRED_REG);
    }
    // Yes, save return register
    if (preserve_ret == MPR_Gr) {
        emitter->ipf_add(6, RETURN_VALUE_REG, 0);
    } else if (preserve_ret == MPR_Fr) {
        emitter->ipf_stf_inc_imm(float_mem_size_e, mem_st_spill, mem_none, SP_REG, RETURN_VALUE_REG, unsigned(-16));
    }

    if (handles) {
        emit_call_with_gp(*emitter, (void**)m2n_pop_local_handles, false);
    } else {
        emit_call_with_gp(*emitter, (void**)m2n_free_local_handles, false);
    }
    
    // Restore return register
    if (preserve_ret == MPR_Gr) {
        emitter->ipf_add(RETURN_VALUE_REG, 6, 0);
    } else if (preserve_ret == MPR_Fr) {
        emitter->ipf_adds(SP_REG, 16, SP_REG);
        emitter->ipf_ldf(float_mem_size_e, mem_ld_fill, mem_none, RETURN_VALUE_REG, SP_REG);
    }


    if (handles) {
        emitter->set_target(free_target);
    }

    // Unlink the M2nFrame from the list of the current thread
    size_t offset_lm2nf = (size_t)&((VM_thread*)0)->last_m2n_frame;
    emitter->ipf_adds(SCRATCH_GENERAL_REG2, (int)offset_lm2nf, THREAD_PTR_REG);
    emitter->ipf_st(int_mem_size_8, mem_st_none, mem_none, SCRATCH_GENERAL_REG2, M2N_SAVED_M2NFL);

    // Restore callee saved general registers, predicates, return address, and pfs
    emitter->ipf_mov (7,                      M2N_SAVED_R7);
    emitter->ipf_mov (6,                      M2N_SAVED_R6);
    emitter->ipf_mov (5,                      M2N_SAVED_R5);
    emitter->ipf_mov (4,                      M2N_SAVED_R4);
    emitter->ipf_mtpr(                        M2N_SAVED_PR);
    if (do_alloc) {
        emitter->ipf_mov (GP_REG,                 M2N_SAVED_GP);
        emitter->ipf_mtbr(BRANCH_RETURN_LINK_REG, M2N_SAVED_RETURN_ADDRESS);
        emitter->ipf_mtap(AR_pfs,                 M2N_SAVED_PFS);
    }
}

void m2n_gen_save_extra_preserved_registers(Merced_Code_Emitter* emitter)
{
    unsigned reg;

    // Save pointer to saves area
    emitter->ipf_mov(M2N_EXTRA_SAVED_PTR, SP_REG);

    // Save callee saves floating point registers
    for (reg = 2;  reg < 6;  reg++)
        emitter->ipf_stf_inc_imm(float_mem_size_e, mem_st_spill, mem_none, SP_REG, reg, unsigned(-16));
    for (reg = 16;  reg < 32;  reg++)
        emitter->ipf_stf_inc_imm(float_mem_size_e, mem_st_spill, mem_none, SP_REG, reg, unsigned(-16));

    // Save callee saves branch registers
    for (reg = 1;  reg < 6;  reg++) {
        emitter->ipf_mfbr(SCRATCH_GENERAL_REG, reg);
        emitter->ipf_st_inc_imm(int_mem_size_8, mem_st_none, mem_none, SP_REG, SCRATCH_GENERAL_REG, unsigned(-8));
    }

    // Save ar.fpsr, ar.unat, and ar.lc
    emitter->ipf_st_inc_imm(int_mem_size_8, mem_st_none, mem_none, SP_REG, SCRATCH_GENERAL_REG, unsigned(-8));
    emitter->ipf_mfap(SCRATCH_GENERAL_REG, AR_unat);
    emitter->ipf_st_inc_imm(int_mem_size_8, mem_st_none, mem_none, SP_REG, SCRATCH_GENERAL_REG, unsigned(-8));
    emitter->ipf_mfap(SCRATCH_GENERAL_REG, AR_lc);
    emitter->ipf_st_inc_imm(int_mem_size_8, mem_st_none, mem_none, SP_REG, SCRATCH_GENERAL_REG, unsigned(-24));


    // Note that the last postdec (postinc of -24) has created the required scratch area on the memory stack
}

unsigned m2n_get_last_m2n_reg() {
    return M2N_SAVED_PFS + M2N_NUMBER_LOCALS - 1;
}

unsigned m2n_get_pfs_save_reg() {
    return M2N_SAVED_PFS;
}

unsigned m2n_get_return_save_reg() {
    return M2N_SAVED_RETURN_ADDRESS;
}

unsigned m2n_get_gp_save_reg() {
    return M2N_SAVED_GP;
}

uint64* m2n_get_arg_word(M2nFrame* m2nf, unsigned n)
{
    do_flushrs();
    if (n<8)
        return get_stacked_register_address(m2n_get_bsp(m2nf), n+32);
    else
        return ((uint64*)*get_stacked_register_address(m2n_get_bsp(m2nf), M2N_SAVED_SP))+(n-8+2); // +2 is for 16-bytes scratch on mem stack
}

void m2n_push_suspended_frame(M2nFrame* m2nf, Registers* regs)
{
    LDIE(86, "check that it works"); // FIXME: check that it works
    m2n_push_suspended_frame(p_TLS_vmthread, m2nf, regs);
}

void m2n_push_suspended_frame(VM_thread* thread, M2nFrame* m2nf, Registers* regs) 
{
    LDIE(51, "Not implemented");
}


M2nFrame* m2n_push_suspended_frame(Registers* regs)
{
    LDIE(86, "check that it works"); // FIXME: check that it works
    return m2n_push_suspended_frame(p_TLS_vmthread, regs);
}

M2nFrame* m2n_push_suspended_frame(VM_thread* thread, Registers* regs)
{
    LDIE(86, "check that it works"); // FIXME: check that it works
    M2nFrame* m2nf = (M2nFrame*)STD_MALLOC(sizeof(M2nFrame));
    assert(m2nf);
    m2n_push_suspended_frame(thread, m2nf, regs);
    return m2nf;
}

bool m2n_is_suspended_frame(M2nFrame * m2nf) {
    LDIE(51, "Not implemented");
    return false;

}

