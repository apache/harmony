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
#define LOG_DOMAIN "exn"
#include "clog.h"

#include "environment.h"
#include "jit_intf_cpp.h"
#include "m2n.h"
#include "m2n_ia32_internal.h"
#include "nogc.h"
#include "stack_iterator.h"
#include "vm_stats.h"
#include "open/types.h"
#include "encoder.h"
#include "interpreter.h"
#include "cci.h"

#include "dump.h"
#include "port_threadunsafe.h"
#include "open/vm_method_access.h"

// Invariants:
//   Native frames:
//     cci should be NULL
//     m2nfl should point to the m2n frame list for the native frame
//     c.p_eip should point to an address that is not a valid IP for managed code
//   Managed frames:
//     cci should point to the code chunk info for the method and ip in question
//     m2nfl should point to the m2n frame immediately preceeding the current one or NULL is there is no preceeding m2n frame
//     the callee saves registers should point to their values at the time the frame was suspended
//     for frames suspended at noncall sites, the caller saves registers should point to their values at the time of suspension
//     c.p_eip and c.esp should point-to/have their values at the time the frame was suspended
//     c.p_eax is also valid for returning a pointer in transfer control

struct StackIterator {
    CodeChunkInfo*    cci;
    JitFrameContext   c;
    M2nFrame*         m2nfl;
    U_32            ip;
};

//////////////////////////////////////////////////////////////////////////
// Utilities

// Goto the managed frame immediately prior to m2nfl
static void si_unwind_from_m2n(StackIterator* si, bool over_popped = true)
{
#ifdef VM_STATS
    UNSAFE_REGION_START
    VM_Statistics::get_vm_stats().num_unwind_native_frames_all++;
    UNSAFE_REGION_END
#endif

    M2nFrame* m2nfl = si->m2nfl;
    assert(m2nfl);

    si->m2nfl   = m2n_get_previous_frame(m2nfl);

    CTRACE(("si_unwind_from_m2n, ip = %p",(void*)m2nfl->eip));

    // Is it a normal M2nFrame or one for suspended managed code?
    if ((U_32)m2nfl->p_lm2nf==1) {
        // Suspended managed code, eip is at instruction, esp & registers are in regs structure
        CTRACE(("si_unwind_from_m2n from suspended managed code, ip = %p", 
            (void*)m2nfl->regs->eip));
        si->c.esp = m2nfl->regs->esp;
        si->c.p_eip = &(m2nfl->regs->eip);
        si->c.is_ip_past = FALSE;
        si->c.p_eax = &m2nfl->regs->eax;
        si->c.p_ebx = &m2nfl->regs->ebx;
        si->c.p_ecx = &m2nfl->regs->ecx;
        si->c.p_edx = &m2nfl->regs->edx;
        si->c.p_esi = &m2nfl->regs->esi;
        si->c.p_edi = &m2nfl->regs->edi;
        si->c.p_ebp = &m2nfl->regs->ebp;
        si->c.eflags = m2nfl->regs->eflags;
    } else if (over_popped &&
            (FRAME_MODIFIED_STACK == (FRAME_MODIFIED_STACK & m2n_get_frame_type(m2nfl)))) {
        si->c.esp = m2nfl->pop_regs->esp;
        si->c.p_eip = &(m2nfl->pop_regs->eip);
        si->c.is_ip_past = FALSE;
        si->c.p_eax = &m2nfl->pop_regs->eax;
        si->c.p_ebx = &m2nfl->pop_regs->ebx;
        si->c.p_ecx = &m2nfl->pop_regs->ecx;
        si->c.p_edx = &m2nfl->pop_regs->edx;
        si->c.p_esi = &m2nfl->pop_regs->esi;
        si->c.p_edi = &m2nfl->pop_regs->edi;
        si->c.p_ebp = &m2nfl->pop_regs->ebp;
        si->c.eflags = m2nfl->pop_regs->eflags;
    } else {
        // Normal M2nFrame, eip is past instruction, esp is implicitly address just beyond the frame, callee saves registers in M2nFrame
        si->c.esp   = (U_32)m2nfl + m2n_sizeof_m2n_frame;
        si->c.p_eip = &m2nfl->eip;
        si->c.is_ip_past = TRUE;
        si->c.p_edi = &m2nfl->edi;
        si->c.p_esi = &m2nfl->esi;
        si->c.p_ebx = &m2nfl->ebx;
        si->c.p_ebp = &m2nfl->ebp;
    }
}

static char* get_reg(char* ss, R_Opnd* dst, Reg_No dst_reg, Reg_No si_ptr_var, unsigned offset)
{
    ss = mov(ss, *dst,  M_Base_Opnd(si_ptr_var, offset));
    ss = mov(ss, *dst,  M_Base_Opnd(dst_reg, 0));
    return ss;
}

typedef void (__cdecl *transfer_control_stub_type)(StackIterator*);

#define CONTEXT_OFFSET(_field_) \
    ((unsigned)&((StackIterator*)0)->c._field_)

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

    const int stub_size = 0x57;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_COLD, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;

    //
    // ************* LOW LEVEL DEPENDENCY! ***************
    // This code sequence must be atomic.  The "atomicity" effect is achieved by
    // changing the esp at the very end of the sequence.
    //

    M_Base_Opnd m1(esp_reg, 4);
    ss = mov(ss,  edx_opnd,  m1);

    ss = get_reg(ss, &ebx_opnd, ebx_reg, edx_reg, CONTEXT_OFFSET(p_eip));

    M_Base_Opnd m2(edx_reg, CONTEXT_OFFSET(esp));
    ss = mov(ss,  ecx_opnd,  m2);

    ss = alu(ss, sub_opc, ecx_opnd, Imm_Opnd(4));
    ss = mov(ss,  m1,  ecx_opnd);

    ss = get_reg(ss, &esi_opnd, esi_reg, edx_reg, CONTEXT_OFFSET(p_esi));
    ss = get_reg(ss, &edi_opnd, edi_reg, edx_reg, CONTEXT_OFFSET(p_edi));
    ss = get_reg(ss, &ebp_opnd, ebp_reg, edx_reg, CONTEXT_OFFSET(p_ebp));

    M_Base_Opnd m3(ecx_reg, 0);
    ss = mov(ss,  m3,  ebx_opnd);

    ss = get_reg(ss, &eax_opnd, eax_reg, edx_reg, CONTEXT_OFFSET(p_eax));
    ss = get_reg(ss, &ebx_opnd, ebx_reg, edx_reg, CONTEXT_OFFSET(p_ebx));

    ss = mov(ss, ecx_opnd,  M_Base_Opnd(edx_reg, CONTEXT_OFFSET(eflags)));
    ss = test(ss, ecx_opnd, ecx_opnd);
    ss = branch8(ss, Condition_Z,  Imm_Opnd(size_8, 0));
    char* patch_offset = ((char *)ss) - 1; // Store location for jump patch
    *ss++ = (char)0x9C; // PUSHFD
    M_Base_Opnd m4(esp_reg, 0);
    ss = alu(ss, and_opc, m4, Imm_Opnd(FLG_CLEAR_MASK));
    ss = alu(ss, and_opc, ecx_opnd, Imm_Opnd(FLG_SET_MASK));
    ss = alu(ss, or_opc, m4, ecx_opnd);
    *ss++ = (char)0x9D; // POPFD
    // Patch conditional jump
    signed offset = (signed)ss - (signed)patch_offset - 1;
    *patch_offset = (char)offset;

    ss = get_reg(ss, &ecx_opnd, ecx_reg, edx_reg, CONTEXT_OFFSET(p_ecx));
    ss = get_reg(ss, &edx_opnd, edx_reg, edx_reg, CONTEXT_OFFSET(p_edx));

    ss = mov(ss,  esp_opnd,  m1);
    ss = ret(ss);

    addr = (transfer_control_stub_type)stub;
    assert(ss-stub <= stub_size);

    /*
       The following code will be generated:

        mov         edx,dword ptr [esp+4]
        mov         ebx,dword ptr [edx+0Ch]
        mov         ebx,dword ptr [ebx]
        mov         ecx,dword ptr [edx+4]
        sub         ecx,4
        mov         dword ptr [esp+4],ecx
        mov         esi,dword ptr [edx+14h]
        mov         esi,dword ptr [esi]
        mov         edi,dword ptr [edx+10h]
        mov         edi,dword ptr [edi]
        mov         ebp,dword ptr [edx+8]
        mov         ebp,dword ptr [ebp]
        mov         dword ptr [ecx],ebx
        mov         eax,dword ptr [edx+1Ch]
        mov         eax,dword ptr [eax]
        mov         ebx,dword ptr [edx+18h]
        mov         ebx,dword ptr [ebx]
        movzx       ecx,byte ptr [edx+28h]
        test        ecx,ecx
        je          _label_
        pushfd
        and         dword ptr [esp], 0x003F7202
        and         ecx, 0x00000CD5
        or          dword ptr [esp], ecx
        popfd
_label_:
        mov         ecx,dword ptr [edx+20h]
        mov         ecx,dword ptr [ecx]
        mov         edx,dword ptr [edx+24h]
        mov         edx,dword ptr [edx]
        mov         esp,dword ptr [esp+4]
        ret
    */

    DUMP_STUB(stub, "getaddress__transfer_control", ss - stub);

    return addr;
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
    ASSERT_NO_INTERPRETER
    // Allocate iterator
    StackIterator* res = (StackIterator*)STD_MALLOC(sizeof(StackIterator));
    assert(res);

    // Setup current frame
    si_fill_from_native(res, thread);

    return res;
}

void si_fill_from_native(StackIterator* si, VM_thread* thread)
{
    memset(si, 0, sizeof(StackIterator));

    // Setup current frame
    si->cci = NULL;
    si->m2nfl = m2n_get_last_frame(thread);
    si->ip = 0;
    si->c.p_eip = &si->ip;
}


StackIterator* si_create_from_registers(Registers* regs, bool is_ip_past, M2nFrame* lm2nf)
{
    // Allocate iterator
    StackIterator* res = (StackIterator*)STD_MALLOC(sizeof(StackIterator));
    assert(res);

    si_fill_from_registers(res, regs, is_ip_past, lm2nf);

    return res;
}
void si_fill_from_registers(StackIterator* si,Registers* regs, bool is_ip_past, M2nFrame* lm2nf)
{
    memset(si, 0, sizeof(StackIterator));

    // Setup current frame
    Global_Env *env = VM_Global_State::loader_env;
    // It's possible that registers represent native code and si->cci==NULL
    Method_Handle m = env->em_interface->LookupCodeChunk(
        reinterpret_cast<void *>(regs->eip), is_ip_past,
        NULL, NULL, reinterpret_cast<void **>(&si->cci));
    if (NULL == m)
        si->cci = NULL;

    si->c.esp = regs->esp;
    si->c.p_eip = &regs->eip;
    si->c.p_ebp = &regs->ebp;
    si->c.p_edi = &regs->edi;
    si->c.p_esi = &regs->esi;
    si->c.p_ebx = &regs->ebx;
    si->c.p_eax = &regs->eax;
    si->c.p_ecx = &regs->ecx;
    si->c.p_edx = &regs->edx;
    si->c.is_ip_past = is_ip_past;
    si->c.eflags = regs->eflags;
    si->m2nfl = lm2nf;
}

size_t si_size(){
    return sizeof(StackIterator);
}

// On IA32 all registers are preserved automatically, so this is a nop.
void si_transfer_all_preserved_registers(StackIterator*)
{
    // Do nothing
}

bool si_is_past_end(StackIterator* si)
{
    ASSERT_NO_INTERPRETER
    return si->cci==NULL && si->m2nfl==NULL;
}

void si_goto_previous(StackIterator* si, bool over_popped)
{
    ASSERT_NO_INTERPRETER
    if (si->cci) {
        CTRACE(("si_goto_previous from ip = %p (%s%s)",
            (void*)si_get_ip(si),
            method_get_name(si->cci->get_method()),
            method_get_descriptor(si->cci->get_method())));
        assert(si->cci->get_jit() && si->cci->get_method());
        si->cci->get_jit()->unwind_stack_frame(si->cci->get_method(), si_get_jit_context(si));
        si->c.is_ip_past = TRUE;
    } else {
        CTRACE(("si_goto_previous from ip = %p (M2N)",
            (void*)si_get_ip(si)));
        if (!si->m2nfl) return;
        si_unwind_from_m2n(si, over_popped);
    }
    Global_Env *vm_env = VM_Global_State::loader_env;
    Method_Handle m = vm_env->em_interface->LookupCodeChunk(si_get_ip(si),
        si_get_jit_context(si)->is_ip_past, NULL, NULL,
        reinterpret_cast<void **>(&si->cci));
    if (NULL == m)
        si->cci = NULL;

    if (si->cci) {
        CTRACE(("si_goto_previous to ip = %p (%s%s)",
            (void*)si_get_ip(si),
            method_get_name(si->cci->get_method()),
            method_get_descriptor(si->cci->get_method())));
    } else {
        CTRACE(("si_goto_previous to ip = %p (M2N)",
            (void*)si_get_ip(si)));
    }
}

StackIterator* si_dup(StackIterator* si)
{
    ASSERT_NO_INTERPRETER
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
    return (void*)si->c.esp;
}

NativeCodePtr si_get_ip(StackIterator* si)
{
    ASSERT_NO_INTERPRETER
    return (NativeCodePtr)*si->c.p_eip;
}

void si_set_ip(StackIterator* si, NativeCodePtr ip, bool also_update_stack_itself)
{
    if (also_update_stack_itself) {
        *(si->c.p_eip) = (U_32)ip;
    } else {
        si->ip = (U_32)ip;
        si->c.p_eip = &si->ip;
    }
}

// Set the code chunk in the stack iterator
void si_set_code_chunk_info(StackIterator* si, CodeChunkInfo* cci)
{
    ASSERT_NO_INTERPRETER
    assert(si);
    si->cci = cci;
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
    ASSERT_NO_INTERPRETER
    return si->cci==NULL;
}

M2nFrame* si_get_m2n(StackIterator* si)
{
    ASSERT_NO_INTERPRETER
    return si->m2nfl;
}

void** si_get_return_pointer(StackIterator* si)
{
    return (void**) si->c.p_eax;
}

void si_set_return_pointer(StackIterator* si, void** return_value)
{
    si->c.p_eax = (U_32*)return_value;
}

#ifdef _WIN32

#ifdef _MSC_VER
#pragma warning(push)
#pragma warning(disable : 4733) // Inline asm assigning to 'FS:0' : handler not registered as safe handler
#endif
/**
 * Temporary workaround and detection of problem.
 *
 * This is a hack function to detect problems with C++ object living on
 * while destructive unwinding is used. Known problem is logger in release
 * mode. No (even potential) logging or other c++ objects created on stack
 * is alowed in any functions in the stack when calling destructive unwinding.
 *
 * The function prints warning message and corrects exception handlers if
 * the objects exist.
 *
 * See also function: JIT_execute_method_default
 */
static void cpp_check_last_frame()
{
    void *handler;
    __asm {
        mov eax, fs:[0]
        mov handler, eax
    }

    void *lastFrame = p_TLS_vmthread->lastFrame;

    CTRACE(("  curr = 0x%p\n", lastFrame));
    CTRACE((" handler=0x%p\n", handler));

    if (!(handler < lastFrame)) {
        CTRACE(("all ok\n"));
        return;
    }

    WARN(("ERROR: Destructive unwinding: C++ objects detected on stack!\n"));

    while(handler < lastFrame) {
        WARN(("  droping 0x%p\n", handler));
        handler = *(int**)handler;
    }
    WARN((" setting curr 0x%p\n", handler));
    __asm {
        mov eax, handler
        mov fs:[0], eax
    }
}
#ifdef _MSC_VER
#pragma warning(pop)
#endif
#endif // _WIN32

void si_transfer_control(StackIterator* si)
{
/* !!!! NO CXX LOGGER IS ALLOWED IN THIS FUNCTION !!!
 * !!!! RELEASE BUILD WILL BE BROKEN          !!!*/
    // 1. Copy si to stack
    void* null_pointer = NULL;
    StackIterator local_si;
    memcpy(&local_si, si, sizeof(StackIterator));

    if (NULL == si->c.p_eax)
        local_si.c.p_eax = (U_32*)&null_pointer;
    if (NULL == si->c.p_ebx)
        local_si.c.p_ebx = (U_32*)&null_pointer;
    if (NULL == si->c.p_ecx)
        local_si.c.p_ecx = (U_32*)&null_pointer;
    if (NULL == si->c.p_edx)
        local_si.c.p_edx = (U_32*)&null_pointer;

    if (si->c.p_eip == &si->ip)
        local_si.c.p_eip = &local_si.ip;
    //si_free(si);

    // 2. Set the M2nFrame list
    m2n_set_last_frame(local_si.m2nfl);
#ifdef _WIN32 // Workaround and detection of possible problems with
              // objects on stack.
    cpp_check_last_frame();
#endif // _WIN32

    CTRACE(("generating control transfer stub"));
    // 3. Call the stub
    transfer_control_stub_type tcs = gen_transfer_control_stub();
    CTRACE(("tcs"));
    tcs(&local_si);
}

inline static U_32 unref_reg(U_32* p_reg) {
    return p_reg ? *p_reg : 0;
}
void si_copy_to_registers(StackIterator* si, Registers* regs)
{
    ASSERT_NO_INTERPRETER

    regs->esp = si->c.esp;
    regs->eflags = si->c.eflags;
    regs->eip = unref_reg(si->c.p_eip);
    regs->ebp = unref_reg(si->c.p_ebp);
    regs->edi = unref_reg(si->c.p_edi);
    regs->esi = unref_reg(si->c.p_esi);
    regs->edx = unref_reg(si->c.p_edx);
    regs->ecx = unref_reg(si->c.p_ecx);
    regs->ebx = unref_reg(si->c.p_ebx);
    regs->eax = unref_reg(si->c.p_eax);
}

void si_set_callback(StackIterator* si, NativeCodePtr* callback) {
    si->c.esp = si->c.esp - 4;
    *((U_32*) si->c.esp) = *(si->c.p_eip);
    si->c.p_eip = ((U_32*)callback);
}

void si_reload_registers()
{
    // Do nothing
}
