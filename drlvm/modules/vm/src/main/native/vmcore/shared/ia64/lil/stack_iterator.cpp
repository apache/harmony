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
 * @author Intel, Pavel Afremov
 */  

#define LOG_DOMAIN "port.old"
#include "cxxlog.h"

#include "environment.h"
#include <ostream.h>
#include <pthread.h>
#include <signal.h>
#include <assert.h>

using namespace std;

#include "open/types.h"
#include "jit_intf_cpp.h"
#include "m2n.h"
#include "m2n_ipf_internal.h"
#include "nogc.h"
#include "vm_ipf.h"
#include "vm_threads.h"
#include "stack_iterator.h"
#include "stub_code_utils.h"
#include "root_set_enum_internal.h"
#include "cci.h"

#include "dump.h"
#include "vm_stats.h"

// Invariants:
//   Note that callee saves and stacked registers below means both the pointers and the corresponding nat bits in nats_lo and nats_hi
//   All frames:
//     last_legal_rsnat should point to a valid spilled nat set on the rse stack (for the stack of this frame context)
//     extra_nats should be for the nats bits that are or would be spilled next after last_legal_rsnat
//     the valid bits of extra_nats plus all spilled nat sets at or below last_legal_rsnat must cover all of the relevant parts of the rse stack
//     the bottom 32 bits of nats_lo should reflect the nat status of those registers in the current frame
//   Native frames:
//     m2nfl should point to the m2n frame list for the native frame
//     cci must be zero
//   Managed frames:
//     m2nfl should point to the m2n frame immediately preceeding the current one or NULL if there is no preceeding m2n frame
//     cci must point to the code chunk info for the ip at which the frame is suspended, and must be nonnull
//     bsp should point to the bsp spill location for gr32 of the current frame
//     the callee saves registers and stacked registers of the frame should point to their values at the point of suspension of the frame
//     c.p_ar_pfs should point to the saved pfs for the current frame (ie the cfm for the current frame formatted as for ar.pfs)
//     c.p_eip and c.sp should point-to/have their values at the time the frame was suspended

struct StackIterator {
    CodeChunkInfo*    cci;
    JitFrameContext   c;
    M2nFrame*         m2nfl;
    uint64            ip;
    uint64*           bsp;
    uint64            extra_rnats;
    uint64            extra_unats;
};

//////////////////////////////////////////////////////////////////////////
// Utilities

// At some point the rse engine was flushed upto bsp and rnat is the rnat register immediately after
// this flush.  We assume that all nat bits we are interested in do not change from the time of flush
// to after we are finished with the iterator that calls this function, even if the rse engine has
// returned to a point prior to bsp.
/*
static void si_init_nats(StackIterator* si, uint64* bsp, uint64 rnat)
{
    si->last_legal_rsnat = (uint64*)((uint64)bsp & ~0x1f8);
    si->extra_nats = rnat;
}
*/

// This function flushes the rse and puts the value of bsp/bspstore into res[1] and rnat into res[0]
/*
extern "C" void get_rnat_and_bsp(uint64* res);
extern "C" void get_rnat_and_bspstore(uint64* res);
*/

static uint64 get_rnat(uint64 * bsp) {
    uint64 * last_m2n = (uint64 *)m2n_get_last_frame();
    uint64 * rnat_ptr = (uint64*)((uint64)bsp | (uint64)0x1f8);
    uint64 * extra_nat_ptr;

    if (rnat_ptr <= last_m2n) {
        return *rnat_ptr;
    }

    // All nat bits for last M2N are stored at M2N_EXTRA_UNAT.
    // All nat bits for parent frames are stored at M2N_EXTRA_RNAT.

    if (bsp >= last_m2n) {
        extra_nat_ptr = last_m2n + (M2N_EXTRA_UNAT - 32);
    } else {
        extra_nat_ptr = last_m2n + (M2N_EXTRA_RNAT - 32);
    }

    if (rnat_ptr <= extra_nat_ptr) {
        // There is rnat collection inside M2N. Need to adjust...
        extra_nat_ptr += 1;
    }

    return *extra_nat_ptr;
}

// Setup the stacked register for the current frame given bsp and ar.pfs (cfm for current frame)
static void si_setup_stacked_registers(StackIterator* si)
{
    const uint64 ALL_ONES = ~0;
    uint64 pfs = *si->c.p_ar_pfs;
    unsigned sof = (unsigned)EXTRACT64_SOF(pfs);

    uint64 nats_lo = si->c.nats_lo & 0xffffffff;
    uint64 nats_hi = 0;
    uint64* bsp = si->bsp;
    uint64 nats = get_rnat(bsp);

    unsigned index = (unsigned)(((uint64)bsp & (uint64)0x1f8) >> 3);
    uint64 mask = ((uint64)1) << index;

    for(unsigned i=0; i<sof; i++) {
        // Set the location of the stack register
        si->c.p_gr[32+i] = bsp;
        // Set the nat bit of the register
        if (nats & mask)
            if (i<32)
                nats_lo |= (1<<(i+32));
            else
                nats_hi |= (1<<(i-32));
        // Advance bsp and mask
        bsp++;
        mask <<= 1;
        // If bsp is on a spilled rsnat recompute nats and mask
        if (((uint64)bsp&(uint64)0x1f8) == (uint64)0x1f8) {
            bsp++;
            mask = 1;
            nats = get_rnat(bsp);
        }
    }

    si->c.nats_lo = nats_lo;
    si->c.nats_hi = nats_hi;
}

// Set p_eip, sp, callee saves registers, and ar_pfs for the frame prior to the current m2nfl
static void si_unwind_from_m2n(StackIterator* si)
{
#ifdef VM_STATS
    VM_Statistics::get_vm_stats().num_unwind_native_frames_all++;
#endif
    // First setup the stack registers for the m2n frame
    si->bsp = m2n_get_bsp(si->m2nfl);
    uint64 pfs = M2N_NUMBER_LOCALS+8;
    si->c.p_ar_pfs = &pfs;
    si_setup_stacked_registers(si);

    // Now extract various saved values from the m2n frame

    // Callee saved general registers
    si->c.p_gr[4] = si->c.p_gr[M2N_SAVED_R4];
    si->c.p_gr[5] = si->c.p_gr[M2N_SAVED_R5];
    si->c.p_gr[6] = si->c.p_gr[M2N_SAVED_R6];
    si->c.p_gr[7] = si->c.p_gr[M2N_SAVED_R7];
    assert(M2N_SAVED_R7 < 64);
    si->c.nats_lo = si->c.nats_lo & ~(uint64)0xf0 | (si->c.nats_lo >> (M2N_SAVED_R4-4)) & (uint64)0xf0;

    // IP, SP, PFS, preds, unat, m2nfl
    si->c.p_eip    =  si->c.p_gr[M2N_SAVED_RETURN_ADDRESS];
    si->c.sp       = *si->c.p_gr[M2N_SAVED_SP];
    si->c.p_ar_pfs =  si->c.p_gr[M2N_SAVED_PFS];
    si->c.preds    = *si->c.p_gr[M2N_SAVED_PR];
    si->c.ar_unat     = *si->c.p_gr[M2N_SAVED_UNAT];
    si->m2nfl      = m2n_get_previous_frame(si->m2nfl);
}

// Adjust the bsp based on the old frames bsp and ar.pfs (which is the cfm for the new frame)
static void si_unwind_bsp(StackIterator* si)
{
    uint64 pfs = *si->c.p_ar_pfs;
    unsigned sol = (unsigned)EXTRACT64_SOL(pfs);

    assert(sol<=96);

    uint64 bsp = (uint64)si->bsp;
    uint64 local_area_size = sol << 3;

    // Direct computation, see IPF arch manual, volume 3, table 6.2.
    uint64 d2 = bsp - local_area_size;
    uint64 d3 = 62*8 - (bsp & 0x1f8) + local_area_size;
    if (d3 >= 63*8)
        if (d3 >= 126*8)
            d2 -= 16;
        else
            d2 -= 8;
    si->bsp = (uint64*)d2;
}

typedef void (__cdecl *transfer_control_stub_type)(StackIterator*, uint64*);
struct Fp {
    NativeCodePtr addr;
    void* gp;
};

// The transfer control stub takes two arguments:
//   i0: pointer to the stack iterator with context to transfer to
//   i1: value of the unat register for restoring nonstacked registers with nat bits
// The stub does not return
// The stack iterator should be one for the current thread
static transfer_control_stub_type gen_transfer_control_stub()
{
    static Fp fp = {NULL, NULL};
    if (fp.addr) {
        return (transfer_control_stub_type)&fp;
    }

    tl::MemoryPool mem_pool;
    Merced_Code_Emitter emitter(mem_pool, 2, 0);
    emitter.disallow_instruction_exchange();
    emitter.memory_type_is_unknown();
    unsigned i;

    // This register will hold the pointer to the stack iterator
    const int stack_iterator_ptr = SCRATCH_GENERAL_REG2;
    // This register points to the unat values to use for restoring global general registers
    const int unat_ptr = SCRATCH_GENERAL_REG3;
    // This register will hold the new SP until after the stack is finished with
    const int tmp_sp = SCRATCH_GENERAL_REG4;
    // These registers are used to hold temporary values
    const int tmp1 = SCRATCH_GENERAL_REG5;
    const int tmp2 = SCRATCH_GENERAL_REG6;
    const int tmp3 = SCRATCH_GENERAL_REG7;

    emitter.ipf_mov(stack_iterator_ptr, IN_REG0);
    emitter.ipf_mov(unat_ptr, IN_REG1);

    // Cover and flush the register stack.
    // This is needed to properly set bsp using bspstore below.

    emitter.ipf_cover();
    emitter.ipf_flushrs();

    // Restore ar.pfs, ip to return branch register, sp to a temp register, and the predicates
    uint64 ar_pfs_offset = (uint64)&((StackIterator*)0)->c.p_ar_pfs;
    emitter.ipf_adds(tmp1, (int)ar_pfs_offset, stack_iterator_ptr);
    emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, tmp1, tmp1);
    emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, tmp1, tmp1);
    emitter.ipf_mtap(AR_pfs, tmp1);
    uint64 ip_offset = (uint64)&((StackIterator*)0)->c.p_eip;
    emitter.ipf_adds(tmp1, (int)ip_offset, stack_iterator_ptr);
    emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, tmp1, tmp1);
    emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, tmp1, tmp1);
    emitter.ipf_mtbr(BRANCH_RETURN_LINK_REG, tmp1);
    uint64 sp_offset = (uint64)&((StackIterator*)0)->c.sp;
    emitter.ipf_adds(tmp1, (int)sp_offset, stack_iterator_ptr);
    emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, tmp_sp, tmp1);
    uint64 preds_offset = (uint64)&((StackIterator*)0)->c.preds;
    emitter.ipf_adds(tmp1, (int)preds_offset, stack_iterator_ptr);
    emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, tmp1, tmp1);
    emitter.ipf_mtpr(tmp1);

    // Restore callee-saves general registers and the first return register
    uint64 g4_offset = (uint64)&((StackIterator*)0)->c.p_gr[4];
    emitter.ipf_adds(tmp1, (int)g4_offset, stack_iterator_ptr);
    for(i=4; i<=8; i++) {
        emitter.ipf_ld_inc_imm(int_mem_size_8, mem_ld_none, mem_none, tmp3, unat_ptr, 8);
        emitter.ipf_ld_inc_imm(int_mem_size_8, mem_ld_none, mem_none, tmp2, tmp1, 8);
        emitter.ipf_mtap(AR_unat, tmp3);
        emitter.ipf_cmp(icmp_eq, cmp_none, SCRATCH_PRED_REG, SCRATCH_PRED_REG2, tmp2, 0);
        emitter.ipf_ld(int_mem_size_8, mem_ld_fill, mem_none, i, tmp2, SCRATCH_PRED_REG2);
    }

    // Restore callee-saves floating-point registers
    uint64 f2_offset = (uint64)&((StackIterator*)0)->c.p_fp[2];
    emitter.ipf_adds(tmp1, (int)f2_offset, stack_iterator_ptr);
    for(i=2; i<=5; i++) {
        emitter.ipf_ld_inc_imm(int_mem_size_8, mem_ld_none, mem_none, tmp2, tmp1, (i==5 ? 88 : 8));
        emitter.ipf_ldf(float_mem_size_e, mem_ld_fill, mem_none, i, tmp2);
    }
    for(i=16; i<=31; i++) {
        emitter.ipf_ld_inc_imm(int_mem_size_8, mem_ld_none, mem_none, tmp2, tmp1, 8);
        emitter.ipf_ldf(float_mem_size_e, mem_ld_fill, mem_none, i, tmp2);
    }

    // Restore callee-saves branch registers
    uint64 b1_offset = (uint64)&((StackIterator*)0)->c.p_br[1];
    emitter.ipf_adds(tmp1, (int)b1_offset, stack_iterator_ptr);
    for(i=1; i<=5; i++) {
        emitter.ipf_ld_inc_imm(int_mem_size_8, mem_ld_none, mem_none, tmp2, tmp1, 8);
        emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, tmp2, tmp2);
        emitter.ipf_mtbr(i, tmp2);
    }

    // Restore ar.fpsr, ar.unat, and ar.lc
    uint64 fpsr_offset = (uint64)&((StackIterator*)0)->c.ar_fpsr;
    emitter.ipf_adds(tmp1, (int)fpsr_offset, stack_iterator_ptr);
    emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, tmp1, tmp1);
    uint64 unat_offset = (uint64)&((StackIterator*)0)->c.ar_unat;
    emitter.ipf_adds(tmp1, (int)unat_offset, stack_iterator_ptr);
    emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, tmp1, tmp1);
    emitter.ipf_mtap(AR_unat, tmp1);
    uint64 lc_offset = (uint64)&((StackIterator*)0)->c.ar_lc;
    emitter.ipf_adds(tmp1, (int)lc_offset, stack_iterator_ptr);
    emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, tmp1, tmp1);
    emitter.ipf_mtap(AR_lc, tmp1);

    // Restore rse stack and the memory stack
    // For the rse stack we must go to rse enforced lazy mode
    emitter.ipf_mfap(tmp1, AR_rsc);
    emitter.ipf_andi(tmp2, -4, tmp1);
    emitter.ipf_mtap(AR_rsc, tmp2);
    uint64 bsp_offset = (uint64)&((StackIterator*)0)->bsp;
    emitter.ipf_adds(tmp2, (int)bsp_offset, stack_iterator_ptr);
    emitter.ipf_ld(int_mem_size_8, mem_ld_none, mem_none, tmp2, tmp2);
    emitter.ipf_mtap(AR_rnat, 0);
    emitter.ipf_mtap(AR_bspstore, tmp2);
    emitter.ipf_mtap(AR_rsc, tmp1);
    emitter.ipf_mov(SP_REG, tmp_sp);

    enforce_calling_conventions(&emitter);
    // Return to the code
    emitter.ipf_brret(br_many, br_sptk, br_none, BRANCH_RETURN_LINK_REG);
    
    emitter.flush_buffer();
    size_t stub_size = emitter.get_size();
    fp.addr = (NativeCodePtr)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_DEFAULT, CAA_Allocate);
    emitter.copy((char*)fp.addr);
    flush_hw_cache((U_8*)fp.addr, stub_size);
    sync_i_cache();

    DUMP_STUB(fp.addr, "transfer_control_stub (non-LIL)", stub_size);

    fp.gp = get_vm_gp_value();

    return (transfer_control_stub_type)&fp;
}

//////////////////////////////////////////////////////////////////////////
// Stack Iterator Interface

StackIterator* si_create_from_native()
{
    return si_create_from_native(p_TLS_vmthread);
}

void si_fill_from_native(StackIterator* si)
{
    si_fill_from_native(si, p_TLS_vmthread);
}

StackIterator* si_create_from_native(VM_thread* thread)
{
    // Allocate iterator
    StackIterator* res = (StackIterator*)STD_MALLOC(sizeof(StackIterator));
    assert(res);

    // Setup current frame
    si_fill_from_native(res, thread);
    return res;
}

void si_fill_from_native(StackIterator* si, VM_thread * thread) {
    memset(si, 0, sizeof(StackIterator));

    // Setup current frame
    si->cci = NULL;
    si->m2nfl = m2n_get_last_frame(thread);
    si->ip = 0;
    si->c.p_eip = &si->ip;
}

/*
#if defined (PLATFORM_POSIX)
#elif defined (PLATFORM_NT)

// Get the bspstore and rnat values of another thread from the OS.
// Hopefully this will also flush the RSE of the other stack enough for our purposes.
static void get_bsp_and_rnat_from_os(VM_thread * thread, uint64 ** bspstore, uint64 * rnat) {
    CONTEXT ctx;
    ctx.ContextFlags |= CONTEXT_INTEGER;
    BOOL UNREF stat = GetThreadContext(thread->thread_handle, &ctx));
    assert(stat);
    *bspstore = (uint64*)ctx.RsBSPSTORE;
    *rnat = ctx->RsRNAT;
}

StackIterator* si_create_from_native(VM_thread* thread)
{
    // Allocate iterator
    StackIterator* res = (StackIterator*)STD_MALLOC(sizeof(StackIterator));
    assert(res);

    // Setup last_legal_rsnat and extra_nats
    uint64* bsp;
    uint64 rnat;

    get_bsp_and_rnat_from_os(thread, &bsp, &rnat);
    si_init_nats(res, bsp, rnat);

    // Check that bsp covers everything
    assert((uint64)m2n_get_last_frame(thread)+(M2N_NUMBER_LOCALS+9)*8 <= (uint64)bsp);

    // Setup current frame
    res->cci = NULL;
    res->m2nfl = m2n_get_last_frame(thread);
    res->ip = NULL;
    res->c.p_eip = &res->ip;

    return res;
}

#else
#error Stack iterator is not implemented for the given platform
#endif
*/

// On IPF stack iterators must be created from threads (suspended) in native code.
// We do not support threads suspended in managed code yet.
StackIterator* si_create_from_registers(Registers*, bool is_ip_past, M2nFrame*)
{
    LDIE(51, "Not implemented");
    return NULL;
}

void si_fill_from_registers(StackIterator* si, Registers*, bool is_ip_past, M2nFrame*)
{
    LDIE(51, "Not implemented");
}

size_t si_size(){
    return sizeof(StackIterator);
}

void si_transfer_all_preserved_registers(StackIterator* si)
{
    unsigned i;
    uint64* sp = m2n_get_extra_saved(si->m2nfl);

    // Floating point registers
    for(i=2; i<=5; i++) {
        si->c.p_fp[i] = sp;
        sp -= 2;
    }
    for(i=16; i<=31; i++) {
        si->c.p_fp[i] = sp;
        sp -= 2;
    }

    // Branch registers
    for(i=1; i<=5; i++) {
        si->c.p_br[i] = sp;
        sp--;
    }

    // ar.fpsr, ar.unat, ar.lc
    si->c.ar_fpsr = *sp--;
    si->c.ar_unat = *sp--;
    si->c.ar_lc = *sp--;
}

bool si_is_past_end(StackIterator* si)
{
    return si->cci==NULL && si->m2nfl==NULL;
}

void si_goto_previous(StackIterator* si, bool over_popped)
{
    if (si->cci) {
        assert(si->cci->get_jit() && si->cci->get_method());
        si->cci->get_jit()->unwind_stack_frame(si->cci->get_method(), si_get_jit_context(si));
    } else {
        if (!si->m2nfl) return;
        si_unwind_from_m2n(si);
    }
    Global_Env *env = VM_Global_State::loader_env;
    si->c.is_ip_past = TRUE;
    Method_Handle m = env->em_interface->LookupCodeChunk(
        si_get_ip(si), FALSE,
        NULL, NULL, reinterpret_cast<void **>(&si->cci));
    if (NULL == m)
        si->cci = NULL;
    si_unwind_bsp(si);
    si_setup_stacked_registers(si);
}

StackIterator* si_dup(StackIterator* si)
{
    StackIterator* res = (StackIterator*)STD_MALLOC(sizeof(StackIterator));
    memcpy(res, si, sizeof(StackIterator));
    // If si uses itself for IP then res should also to avoid problems if si is deallocated first.
    if (si->c.p_eip == &si->ip)
        res->c.p_eip = &res->ip;
    return res;
}

void si_free(StackIterator* si)
{
    STD_FREE(si);
}

void* si_get_sp(StackIterator* si) {
    return (void*)si->c.sp;
}

NativeCodePtr si_get_ip(StackIterator* si)
{
    return (NativeCodePtr)*si->c.p_eip;
}

void si_set_ip(StackIterator* si, NativeCodePtr ip, bool also_update_stack_itself)
{
    if (also_update_stack_itself) {
        *(si->c.p_eip) = (uint64)ip;
    } else {
        si->ip = (uint64)ip;
        si->c.p_eip = &si->ip;
    }
}

// 20040713 Experimental: set the code chunk in the stack iterator
void si_set_code_chunk_info(StackIterator* si, CodeChunkInfo* cci)
{
    LDIE(51, "Not implemented");
}

CodeChunkInfo* si_get_code_chunk_info(StackIterator* si)
{
    return si->cci;
}

JitFrameContext* si_get_jit_context(StackIterator* si)
{
    return &si->c;
}

bool si_is_native(StackIterator* si)
{
    return si->cci==NULL;
}

M2nFrame* si_get_m2n(StackIterator* si)
{
    return si->m2nfl;
}

void** si_get_return_pointer(StackIterator* si)
{
    LDIE(51, "Not implemented");
    return 0;
}

void si_set_return_pointer(StackIterator* si, void** return_value)
{
    si->c.p_gr[RETURN_VALUE_REG] = (uint64*)return_value;
    si->c.nats_lo &= ~(1<<RETURN_VALUE_REG);
}

void si_transfer_control(StackIterator* si)
{
    // 1. Copy si to stack
    StackIterator local_si;
    memcpy(&local_si, si, sizeof(StackIterator));
    if (si->c.p_eip == &si->ip)
        local_si.c.p_eip = &local_si.ip;
    //si_free(si);

    // 2. Set the M2nFrame list
    m2n_set_last_frame(local_si.m2nfl);

    // 3. Move bsp to next frame
    uint64 sol = EXTRACT64_SOL(*local_si.c.p_ar_pfs);
    for(unsigned i=0; i<sol; i++) {
        local_si.bsp++;
        if (((uint64)local_si.bsp & 0x1f8) == 0x1f8) local_si.bsp++;
    }

    // 4. Compute the unat values
    uint64 unat[32];
    for(unsigned reg=4; reg<=8; reg++) {
        if (local_si.c.nats_lo & (1 << reg)) {
            uint64 index = ((uint64)local_si.c.p_gr[reg] & 0x1f8) >> 3;
            uint64 mask = 1 << index;
            unat[reg] = mask;
        } else {
            unat[reg] = 0;
        }
    }

    // 5. Call the stub
    transfer_control_stub_type tcs = gen_transfer_control_stub();
    tcs(&local_si, unat+4);
}

void si_copy_to_registers(StackIterator* si, Registers*)
{
    LDIE(51, "Not implemented");
}

void si_set_callback(StackIterator* si, NativeCodePtr* callback) {
    LDIE(51, "Not implemented");
}

extern "C" void do_loadrs_asm(int loadrs);

void si_reload_registers()
{
    do_loadrs_asm(0);
}
