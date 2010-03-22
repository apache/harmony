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
 * @author Evgueni Brevnov
 */

#include <string.h>

#include "open/vm_method_access.h"
#include "environment.h"
#include "stack_iterator.h"
#include "vm_threads.h"
#include "jit_intf_cpp.h"
#include "encoder.h"
#include "m2n.h"
#include "m2n_em64t_internal.h"
#include "nogc.h"
#include "interpreter.h" // for ASSERT_NO_INTERPRETER
#include "cci.h"

#include "dump.h"
#include "vm_stats.h"

#include "cxxlog.h"

// see stack_iterator_ia32.cpp
struct StackIterator {
    CodeChunkInfo *   cci;
    JitFrameContext   jit_frame_context;
    M2nFrame *        m2n_frame;
    uint64            ip;
};

//////////////////////////////////////////////////////////////////////////
// Utilities

static void si_copy(StackIterator * dst, const StackIterator * src) {
    memcpy(dst, src, sizeof(StackIterator));
    // If src uses itself for ip then the dst should also do
    // to avoid problems if src is deallocated first.
    if (src->jit_frame_context.p_rip == &src->ip) {
        dst->jit_frame_context.p_rip = &dst->ip;
    }
}

static void init_context_from_registers(JitFrameContext & context,
                                        Registers & regs, bool is_ip_past) {
    context.rsp   = regs.rsp;
    context.p_rbp = &regs.rbp;
    context.p_rip = &regs.rip;

    context.p_rbx = &regs.rbx;
    context.p_r12 = &regs.r12;
    context.p_r13 = &regs.r13;
    context.p_r14 = &regs.r14;
    context.p_r15 = &regs.r15;
    
    context.p_rax = &regs.rax;
    context.p_rcx = &regs.rcx;
    context.p_rdx = &regs.rdx;
    context.p_rsi = &regs.rsi;
    context.p_rdi = &regs.rdi;
    context.p_r8  = &regs.r8;
    context.p_r9  = &regs.r9;
    context.p_r10 = &regs.r10;
    context.p_r11 = &regs.r11;
    
    context.eflags = regs.eflags;

    context.is_ip_past = is_ip_past;
}


// Goto the managed frame immediately prior to m2nfl
static void si_unwind_from_m2n(StackIterator * si) {
#ifdef VM_STATS
    VM_Statistics::get_vm_stats().num_unwind_native_frames_all++;
#endif

    M2nFrame * current_m2n_frame = si->m2n_frame;
    assert(current_m2n_frame);

    si->m2n_frame = m2n_get_previous_frame(current_m2n_frame);

    TRACE2("si", "si_unwind_from_m2n, ip = " 
        << (void*)current_m2n_frame->rip);

    // Is it a normal M2nFrame or one for suspended managed code?
    if (m2n_is_suspended_frame(current_m2n_frame)) {
        // Suspended managed code, rip is at instruction,
        // rsp & registers are in regs structure
        TRACE2("si", "si_unwind_from_m2n from suspended managed code, ip = " 
            << (void*)current_m2n_frame->regs->rip);
        init_context_from_registers(si->jit_frame_context, *current_m2n_frame->regs, false);
    } else {
        // Normal M2nFrame, rip is past instruction,
        // rsp is implicitly address just beyond the frame,
        // callee saves registers in M2nFrame
        
        si->jit_frame_context.rsp = (uint64)((uint64*) m2n_get_frame_base(current_m2n_frame) + 1);
        
        si->jit_frame_context.p_rbp = &current_m2n_frame->rbp;
        si->jit_frame_context.p_rip = &current_m2n_frame->rip;

#ifdef _WIN64
        si->jit_frame_context.p_rdi = &current_m2n_frame->rdi;
        si->jit_frame_context.p_rsi = &current_m2n_frame->rsi;
#endif
        si->jit_frame_context.p_rbx = &current_m2n_frame->rbx;
        si->jit_frame_context.p_r12 = &current_m2n_frame->r12;
        si->jit_frame_context.p_r13 = &current_m2n_frame->r13;
        si->jit_frame_context.p_r14 = &current_m2n_frame->r14;
        si->jit_frame_context.p_r15 = &current_m2n_frame->r15;
        si->jit_frame_context.is_ip_past = true;
    }
}

static char* get_reg(char* ss, const R_Opnd & dst, Reg_No base, int64 offset,
                       bool check_null = false, bool preserve_flags = false)
{
    char* patch_offset = NULL;

    ss = mov(ss, dst,  M_Base_Opnd(base, (I_32)offset));

    if (check_null)
    {
        if (preserve_flags)
            *ss++ = (char)0x9C; // PUSHFD

        ss = test(ss, dst, dst);
        ss = branch8(ss, Condition_Z,  Imm_Opnd(size_8, 0));
        patch_offset = ((char*)ss) - 1; // Store location for jump patch
    }

    ss = mov(ss, dst,  M_Base_Opnd(dst.reg_no(), 0));

    if (check_null)
    {
        // Patch conditional jump
        POINTER_SIZE_SINT offset =
            (POINTER_SIZE_SINT)ss - (POINTER_SIZE_SINT)patch_offset - 1;
        assert(offset >= -128 && offset < 127);
        *patch_offset = (char)offset;

        if (preserve_flags)
            *ss++ = (char)0x9D; // POPFD
    }

    return ss;
}

typedef void (* transfer_control_stub_type)(StackIterator *);

#define CONTEXT_OFFSET(_field_) \
    ((int64)&((StackIterator*)0)->jit_frame_context._field_)

// Clear OF, DF, TF, SF, ZF, AF, PF, CF, do not touch reserved bits
#define FLG_CLEAR_MASK ((unsigned)0x003F7202)
// Set OF, DF, SF, ZF, AF, PF, CF
#define FLG_SET_MASK ((unsigned)0x00000CD5)

static transfer_control_stub_type gen_transfer_control_stub()
{
    static transfer_control_stub_type addr = NULL;

    if (addr) {
        return addr;
    }

    const int STUB_SIZE = 255;
    char * stub = (char *)malloc_fixed_code_for_jit(STUB_SIZE,
        DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_COLD, CAA_Allocate);
    char * ss = stub;
#ifndef NDEBUG
    memset(stub, 0xcc /*int 3*/, STUB_SIZE);
#endif

    //
    // ************* LOW LEVEL DEPENDENCY! ***************
    // This code sequence must be atomic.  The "atomicity" effect is achieved by
    // changing the rsp at the very end of the sequence.

    // rdx holds the pointer to the stack iterator
#if defined (PLATFORM_POSIX) // RDI holds 1st parameter on Linux
    ss = mov(ss, rdx_opnd, rdi_opnd);
#else // RCX holds 1st parameter on Windows
    ss = mov(ss, rdx_opnd, rcx_opnd);
#endif

    // Restore general registers
    ss = get_reg(ss, rbp_opnd, rdx_reg, CONTEXT_OFFSET(p_rbp), false);
    ss = get_reg(ss, rbx_opnd, rdx_reg, CONTEXT_OFFSET(p_rbx), true);
    ss = get_reg(ss, r12_opnd, rdx_reg, CONTEXT_OFFSET(p_r12), true);
    ss = get_reg(ss, r13_opnd, rdx_reg, CONTEXT_OFFSET(p_r13), true);
    ss = get_reg(ss, r14_opnd, rdx_reg, CONTEXT_OFFSET(p_r14), true);
    ss = get_reg(ss, r15_opnd, rdx_reg, CONTEXT_OFFSET(p_r15), true);
    ss = get_reg(ss, rsi_opnd, rdx_reg, CONTEXT_OFFSET(p_rsi), true);
    ss = get_reg(ss, rdi_opnd, rdx_reg, CONTEXT_OFFSET(p_rdi), true);
    ss = get_reg(ss, r8_opnd,  rdx_reg, CONTEXT_OFFSET(p_r8),  true);
    ss = get_reg(ss, r9_opnd,  rdx_reg, CONTEXT_OFFSET(p_r9),  true);
    ss = get_reg(ss, r10_opnd, rdx_reg, CONTEXT_OFFSET(p_r10), true);
    ss = get_reg(ss, r11_opnd, rdx_reg, CONTEXT_OFFSET(p_r11), true);

    // Get the new RSP
    M_Base_Opnd saved_rsp(rdx_reg, CONTEXT_OFFSET(rsp));
    ss = mov(ss, rax_opnd, saved_rsp);
    // Store it over return address for future use
    ss = mov(ss, M_Base_Opnd(rsp_reg, 0), rax_opnd);
    // Get the new RIP
    ss = get_reg(ss, rcx_opnd, rdx_reg, CONTEXT_OFFSET(p_rip), false);
    // Store RIP to [<new RSP> - 136] to preserve 128 bytes under RSP
    // which are 'reserved' on Linux
    ss = mov(ss,  M_Base_Opnd(rax_reg, -136), rcx_opnd);

    ss = get_reg(ss, rax_opnd, rdx_reg, CONTEXT_OFFSET(p_rax), true);

    // Restore processor flags
    ss = movzx(ss, rcx_opnd,  M_Base_Opnd(rdx_reg, CONTEXT_OFFSET(eflags)), size_16);
    ss = test(ss, rcx_opnd, rcx_opnd);
    ss = branch8(ss, Condition_Z,  Imm_Opnd(size_8, 0));
    char* patch_offset = ((char*)ss) - 1; // Store location for jump patch
    *ss++ = (char)0x9C; // PUSHFQ
    M_Base_Opnd sflags(rsp_reg, 0);
    ss = alu(ss, and_opc, sflags, Imm_Opnd(size_32,FLG_CLEAR_MASK), size_32);
    ss = alu(ss, and_opc, rcx_opnd, Imm_Opnd(size_32,FLG_SET_MASK), size_32);
    ss = alu(ss, or_opc, sflags, rcx_opnd, size_32);
    *ss++ = (char)0x9D; // POPFQ
    // Patch conditional jump
    POINTER_SIZE_SINT offset =
        (POINTER_SIZE_SINT)ss - (POINTER_SIZE_SINT)patch_offset - 1;
    *patch_offset = (char)offset;

    ss = get_reg(ss, rcx_opnd, rdx_reg, CONTEXT_OFFSET(p_rcx), true, true);
    ss = get_reg(ss, rdx_opnd, rdx_reg, CONTEXT_OFFSET(p_rdx), true, true);

    // Setup stack pointer to previously saved value
    ss = mov(ss,  rsp_opnd,  M_Base_Opnd(rsp_reg, 0));

    // Jump to address stored to [<new RSP> - 136]
    ss = jump(ss,  M_Base_Opnd(rsp_reg, -136));

    addr = (transfer_control_stub_type)stub;
    assert(ss-stub <= STUB_SIZE);

    /*
       The following code will be generated:

        mov         rdx,rcx
        mov         rbp,qword ptr [rdx+10h]
        mov         rbp,qword ptr [rbp]
        mov         rbx,qword ptr [rdx+20h]
        test        rbx,rbx
        je          __label1__
        mov         rbx,qword ptr [rbx]
__label1__
        ; .... The same for r12,r13,r14,r15,rsi,rdi,r8,r9,r10
        mov         r11,qword ptr [rdx+88h]
        test        r11,r11
        je          __label11__
        mov         r11,qword ptr [r11]
__label11__
        mov         rax,qword ptr [rdx+8]
        mov         qword ptr [rsp],rax
        mov         rcx,qword ptr [rdx+18h]
        mov         rcx,qword ptr [rcx]
        mov         qword ptr [rax-88h],rcx
        mov         rax,qword ptr [rdx+48h]
        test        rax,rax
        je          __label12__
        mov         rax,qword ptr [rax]
__label12__
        movzx       rcx,word ptr [rdx+90h]
        test        rcx,rcx
        je          __label13__
        pushfq
        and         dword ptr [rsp], 0x003F7202
        and         ecx, 0x00000CD5
        or          dword ptr [esp], ecx
        popfq
__label13__
        mov         rcx,qword ptr [rdx+50h]
        pushfq
        test        rcx,rcx
        je          __label14__
        mov         rcx,qword ptr [rcx]
__label14__
        popfq
        mov         rdx,qword ptr [rdx+58h]
        pushfq
        test        rdx,rdx
        je          __label15__
        mov         rdx,qword ptr [rdx]
__label15__
        popfq
        mov         rsp,qword ptr [rsp]
        jmp         qword ptr [rsp-88h]
    */

    DUMP_STUB(stub, "getaddress__transfer_control", ss-stub);

    return addr;
}

#undef CONTEXT_OFFSET
//////////////////////////////////////////////////////////////////////////
// Stack Iterator Interface

StackIterator * si_create_from_native() {
    return si_create_from_native(p_TLS_vmthread);
}

void si_fill_from_native(StackIterator* si) {
    si_fill_from_native(si, p_TLS_vmthread);
}


StackIterator * si_create_from_native(VM_thread * thread) {
    ASSERT_NO_INTERPRETER
    // Allocate iterator
    StackIterator * si = (StackIterator *)STD_MALLOC(sizeof(StackIterator));

    si_fill_from_native(si, thread);
    return si;
}

void si_fill_from_native(StackIterator* si, VM_thread * thread) {
    memset(si, 0, sizeof(StackIterator));

    si->cci = NULL;
    si->jit_frame_context.p_rip = &si->ip;
    si->m2n_frame = m2n_get_last_frame(thread);
    si->ip = 0;
}

StackIterator * si_create_from_registers(Registers * regs, bool is_ip_past,
                                        M2nFrame * lm2nf) {
    ASSERT_NO_INTERPRETER
    // Allocate iterator
    StackIterator * si = (StackIterator *)STD_MALLOC(sizeof(StackIterator));
    assert(si);
    
    si_fill_from_registers(si, regs, is_ip_past, lm2nf);

    return si;
}

void si_fill_from_registers(StackIterator* si, Registers* regs, bool is_ip_past, M2nFrame* lm2nf)
{
    memset(si, 0, sizeof(StackIterator));

    Global_Env *env = VM_Global_State::loader_env;
    // Setup current frame
    // It's possible that registers represent native code and res->cci==NULL

    Method_Handle m = env->em_interface->LookupCodeChunk(
        reinterpret_cast<void *>(regs->rip), is_ip_past,
        NULL, NULL, reinterpret_cast<void **>(&si->cci));
    if (NULL == m)
        si->cci = NULL;

    init_context_from_registers(si->jit_frame_context, *regs, is_ip_past);
    
    si->m2n_frame = lm2nf;
    si->ip = regs->rip;
}

size_t si_size(){
    return sizeof(StackIterator);
}

// On EM64T all registers are preserved automatically, so this is a nop.
void si_transfer_all_preserved_registers(StackIterator *) {
    ASSERT_NO_INTERPRETER
    // Do nothing
}

bool si_is_past_end(StackIterator * si) {
    ASSERT_NO_INTERPRETER
    // check if current position neither corresponds
    // to jit frame nor to m2n frame
    return si->cci == NULL && si->m2n_frame == NULL;
}

void si_goto_previous(StackIterator * si, bool over_popped) {
    ASSERT_NO_INTERPRETER
    if (si_is_native(si)) {
        TRACE2("si", "si_goto_previous from ip = " 
            << (void*)si_get_ip(si) << " (M2N)");
        if (si->m2n_frame == NULL) return;
        si_unwind_from_m2n(si);
    } else {
        assert(si->cci->get_jit() && si->cci->get_method());
        TRACE2("si", "si_goto_previous from ip = "
            << (void*)si_get_ip(si) << " ("
            << method_get_name(si->cci->get_method())
            << method_get_descriptor(si->cci->get_method()) << ")");
        si->cci->get_jit()->unwind_stack_frame(si->cci->get_method(), si_get_jit_context(si));
        si->jit_frame_context.is_ip_past = TRUE;
    }

    Global_Env *vm_env = VM_Global_State::loader_env;

    Method_Handle m = vm_env->em_interface->LookupCodeChunk(si_get_ip(si),
        si_get_jit_context(si)->is_ip_past, NULL, NULL,
        reinterpret_cast<void **>(&si->cci));
    if (NULL == m)
        si->cci = NULL;

#ifndef NDEBUG
    if (si_is_native(si)) {
        TRACE2("si", "si_goto_previous to ip = " << (void*)si_get_ip(si)
            << " (M2N)");
    } else {
        TRACE2("si", "si_goto_previous to ip = " << (void*)si_get_ip(si)
            << " (" << method_get_name(si->cci->get_method())
            << method_get_descriptor(si->cci->get_method()) << ")");
    }
#endif
}

StackIterator * si_dup(StackIterator * si) {
    ASSERT_NO_INTERPRETER
    StackIterator * dup_si = (StackIterator *)STD_MALLOC(sizeof(StackIterator));
    si_copy(dup_si, si);
    return dup_si;
}

void si_free(StackIterator * si) {
    STD_FREE(si);
}

void* si_get_sp(StackIterator* si) {
    return (void*)si->jit_frame_context.rsp;
}

NativeCodePtr si_get_ip(StackIterator * si) {
    ASSERT_NO_INTERPRETER
    return (NativeCodePtr)(*si->jit_frame_context.p_rip);
}

void si_set_ip(StackIterator * si, NativeCodePtr ip, bool also_update_stack_itself) {
    if (also_update_stack_itself) {
        *(si->jit_frame_context.p_rip) = (uint64)ip;
    } else {
        si->ip = (uint64)ip;
        si->jit_frame_context.p_rip = &si->ip;
    }
}

// 20040713 Experimental: set the code chunk in the stack iterator
void si_set_code_chunk_info(StackIterator * si, CodeChunkInfo * cci) {
    ASSERT_NO_INTERPRETER
    assert(si);
    si->cci = cci;
}

CodeChunkInfo * si_get_code_chunk_info(StackIterator * si) {
    return si->cci;
}

JitFrameContext * si_get_jit_context(StackIterator * si) {
    return &si->jit_frame_context;
}

bool si_is_native(StackIterator * si) {
    ASSERT_NO_INTERPRETER
    return si->cci == NULL;
}

M2nFrame * si_get_m2n(StackIterator * si) {
    ASSERT_NO_INTERPRETER
    return si->m2n_frame;
}

void** si_get_return_pointer(StackIterator* si)
{
    return (void**)si->jit_frame_context.p_rax;
}

void si_set_return_pointer(StackIterator * si, void ** return_value) {
    // TODO: check if it is needed to dereference return_value
    si->jit_frame_context.p_rax = (uint64 *)return_value;
}

void si_transfer_control(StackIterator * si) {
    // !!! NO LOGGER IS ALLOWED IN THIS FUNCTION !!!
    // !!! RELEASE BUILD WILL BE BROKEN          !!!
    // !!! NO TRACE2, INFO, WARN, ECHO, ASSERT,  ...
    
    // 1. Copy si to stack
    StackIterator local_si;
    si_copy(&local_si, si);
    //si_free(si);

    // 2. Set the M2nFrame list
    m2n_set_last_frame(local_si.m2n_frame);
    
    // 3. Call the stub
    transfer_control_stub_type tcs = gen_transfer_control_stub();
    tcs(&local_si);
}

inline static uint64 unref_reg(uint64* p_reg) {
    return p_reg ? *p_reg : 0;
}

void si_copy_to_registers(StackIterator * si, Registers * regs) {
    ASSERT_NO_INTERPRETER    
  
    regs->rsp = si->jit_frame_context.rsp;
    regs->rbp = unref_reg(si->jit_frame_context.p_rbp);
    regs->rip = unref_reg(si->jit_frame_context.p_rip);

    regs->rbx = unref_reg(si->jit_frame_context.p_rbx);
    regs->r12 = unref_reg(si->jit_frame_context.p_r12);
    regs->r13 = unref_reg(si->jit_frame_context.p_r13);
    regs->r14 = unref_reg(si->jit_frame_context.p_r14);
    regs->r15 = unref_reg(si->jit_frame_context.p_r15);

    regs->rax = unref_reg(si->jit_frame_context.p_rax);
    regs->rcx = unref_reg(si->jit_frame_context.p_rcx);
    regs->rdx = unref_reg(si->jit_frame_context.p_rdx);
    regs->rsi = unref_reg(si->jit_frame_context.p_rsi);
    regs->rdi = unref_reg(si->jit_frame_context.p_rdi);
    regs->r8 = unref_reg(si->jit_frame_context.p_r8);
    regs->r9 = unref_reg(si->jit_frame_context.p_r9);
    regs->r10 = unref_reg(si->jit_frame_context.p_r10);
    regs->r11 = unref_reg(si->jit_frame_context.p_r11);

    regs->eflags = si->jit_frame_context.eflags;
}

void si_set_callback(StackIterator* si, NativeCodePtr* callback) {
#ifdef WIN32
    // Shadow memory to save 4 registers into stack,
    // this is necessary for WIN64 calling conventions.
    // NOTE: This file is used only for x86_64 architectures
    const static uint64 red_zone_size = 0x28;
#else
    const static uint64 red_zone_size = 0x88;
#endif
    si->jit_frame_context.rsp = si->jit_frame_context.rsp - red_zone_size - sizeof(void*);
    *((uint64*) si->jit_frame_context.rsp) = *(si->jit_frame_context.p_rip);
    si->jit_frame_context.p_rip = ((uint64*)callback);
}

void si_reload_registers() {
    // Nothing to do
}
