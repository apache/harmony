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
#include "open/types.h"
#include "open/hythread.h"
#include "thread_helpers.h"

#include "m2n.h"
#include "m2n_ia32_internal.h"
#include "port_malloc.h"
#include "object_handles.h"
#include "vm_threads.h"
#include "encoder.h"
#include "interpreter.h" // for asserts only
#include "exceptions.h"


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
M2nFrame* m2n_get_previous_frame(M2nFrame* lm2nf)
{
    return lm2nf->prev_m2nf;
}

VMEXPORT // temporary solution for interpreter unplug
ObjectHandles* m2n_get_local_handles(M2nFrame* lm2nf)
{
    return lm2nf->local_object_handles;
}

VMEXPORT // temporary solution for interpreter unplug
void m2n_set_local_handles(M2nFrame* lm2nf, ObjectHandles* h)
{
    lm2nf->local_object_handles = h;
}

void* m2n_get_ip(M2nFrame* lm2nf)
{
    return (void*)lm2nf->eip;
}

// 20040708 New function - needs proper implementation.
void m2n_set_ip(M2nFrame* lm2nf, NativeCodePtr ip)
{
    lm2nf->eip = (U_32)ip;
} 

Method_Handle m2n_get_method(M2nFrame* m2nf)
{
    ASSERT_NO_INTERPRETER
    return m2nf->method;
}

// Returns type of noted m2n frame
frame_type m2n_get_frame_type(M2nFrame* m2nf) {
    return m2nf->current_frame_type;
}

// Sets type of noted m2n frame
void m2n_set_frame_type(M2nFrame* m2nf, frame_type m2nf_type) {
    m2nf->current_frame_type = m2nf_type;
}

size_t m2n_get_size() {
    return sizeof(M2nFrame);
}

void m2n_push_suspended_frame(M2nFrame* m2nf, Registers* regs)
{
    m2n_push_suspended_frame(p_TLS_vmthread, m2nf, regs);
}

void m2n_push_suspended_frame(VM_thread* thread, M2nFrame* m2nf, Registers* regs)
{
    assert(m2nf);
    m2nf->p_lm2nf = (M2nFrame**)1;
    m2nf->method = NULL;
    m2nf->local_object_handles = NULL;
    m2nf->current_frame_type = FRAME_UNKNOWN;

    m2nf->eip  = regs->eip;
    m2nf->regs = regs;

    m2nf->prev_m2nf = m2n_get_last_frame(thread);
    m2n_set_last_frame(thread, m2nf);
}

M2nFrame* m2n_push_suspended_frame(Registers* regs)
{
    return m2n_push_suspended_frame(p_TLS_vmthread, regs);
}

M2nFrame* m2n_push_suspended_frame(VM_thread* thread, Registers* regs)
{
    M2nFrame* m2nf = (M2nFrame*)STD_MALLOC(sizeof(M2nFrame));
    assert(m2nf);
    m2n_push_suspended_frame(thread, m2nf, regs);
    return m2nf;
}

bool m2n_is_suspended_frame(M2nFrame * m2nf) {
    return (U_32)m2nf->p_lm2nf == 1;

}

void * m2n_get_frame_base(M2nFrame * m2nf) {
    // regs should be last field in the M2nFrame structure
    return &m2nf->regs;
}

//***** Stub Interface

U_32* m2n_get_args(M2nFrame* m2nf)
{
    return (U_32*)&m2nf->regs;
}

unsigned m2n_ts_to_register_size()
{
    return 22;
}

char* m2n_gen_ts_to_register(char* buf, R_Opnd* reg)
{
    if (reg!=&eax_opnd)
        buf = push(buf,  eax_opnd);

    buf = gen_hythread_self_helper(buf);

    if (reg!=&eax_opnd) {
        buf = mov(buf, *reg, eax_opnd);
        buf = pop(buf,  eax_opnd);
    }
    return buf;
}

unsigned m2n_push_m2n_size(bool UNREF handles, unsigned num_callee_saves)
{
    return 20+(4-num_callee_saves)+m2n_ts_to_register_size()+11+8;
}

char* m2n_gen_push_m2n(char* buf, Method_Handle method, frame_type current_frame_type, bool UNREF handles, unsigned num_callee_saves)
{
    // The -4 is because the normal M2nFrames do not include the esp field.
    assert(m2n_sizeof_m2n_frame == sizeof(M2nFrame)-4);

    if (num_callee_saves<1) buf = push(buf,  ebp_opnd);
    if (num_callee_saves<2) buf = push(buf,  ebx_opnd);
    if (num_callee_saves<3) buf = push(buf,  esi_opnd);
    if (num_callee_saves<4) buf = push(buf,  edi_opnd);
    
    buf = m2n_gen_ts_to_register(buf, &eax_opnd);
    
    buf = push(buf, Imm_Opnd(size_32, 0));
    //MVM APN 20050513 work with frame_type set up current_frame_type to NULL
    //int frame_type_offset = (int)&((VM_thread*)0)->current_frame_type;
    buf = push(buf, Imm_Opnd(current_frame_type));
    
    int last_m2n_frame_offset = (int)&((VM_thread*)0)->last_m2n_frame;
    Imm_Opnd imm1(last_m2n_frame_offset);
    buf = alu(buf, add_opc,  eax_opnd,  imm1);

    Imm_Opnd imm2((unsigned)method);
    buf = push(buf,  imm2);
    Imm_Opnd imm3(0);
    buf = push(buf,  imm3);
    buf = push(buf,  eax_opnd);
    buf = push(buf,  M_Base_Opnd(eax_reg, +0));
    buf = mov(buf,  M_Base_Opnd(eax_reg, +0),  esp_opnd );

    return buf;
}

unsigned m2n_set_local_handles_size(unsigned bytes_to_m2n)
{
    unsigned num = bytes_to_m2n + (unsigned)&((M2nFrame*)0)->local_object_handles;
    return num<128 ? 4 : 7;
}

char* m2n_gen_set_local_handles(char* buf, unsigned bytes_to_m2n, R_Opnd* src_reg)
{
    unsigned offset_local_handles = (unsigned)&((M2nFrame*)0)->local_object_handles;
    buf = mov(buf, M_Base_Opnd(esp_reg, bytes_to_m2n+offset_local_handles), *src_reg);
    return buf;
}

unsigned m2n_pop_m2n_size(bool handles, unsigned num_callee_saves, unsigned extra_on_stack, unsigned preserve_ret)
{
    unsigned size = 7+(4-num_callee_saves);
    if (handles) size += (extra_on_stack+8<128 ? 12 : 15)+preserve_ret*2;
    else size += 11;
    if (extra_on_stack) size += (extra_on_stack<128 ? 3 : 6);
    return size + 128;
}

static void m2n_pop_local_handles() {
    assert(!hythread_is_suspend_enabled());
    
    if (exn_raised()) {
        exn_rethrow();
    }
    
    M2nFrame * m2n = m2n_get_last_frame();
    free_local_object_handles2(m2n->local_object_handles);
}

static void m2n_free_local_handles() {
    assert(!hythread_is_suspend_enabled());

    if (exn_raised()) {
        exn_rethrow();
    }

    M2nFrame * m2n = m2n_get_last_frame();
    free_local_object_handles3(m2n->local_object_handles);
}

char* m2n_gen_pop_m2n(char* buf, bool handles, unsigned num_callee_saves, unsigned extra_on_stack, unsigned preserve_ret)
{
    if (preserve_ret > 0) {
        // Save return value
        buf = push(buf,  eax_opnd);
        if (preserve_ret > 1) {
            buf = push(buf,  edx_opnd);
        }
    }
    
    if (handles) {
        // There are handles located on the stack
        buf = call(buf, (char*)m2n_pop_local_handles);
    } else {
        buf = call(buf, (char*)m2n_free_local_handles);
    }
    
    if (preserve_ret > 0) {
        // Restore return value
        if (preserve_ret > 1) {
            buf = pop(buf,  edx_opnd);
        }
        buf = pop(buf,  eax_opnd);
    }
    
    // pop "garbage" from the stack
    if (extra_on_stack) {
        Imm_Opnd imm(extra_on_stack);
        buf = alu(buf, add_opc,  esp_opnd,  imm);
    }
    
    // Unlink the M2nFrame from the list of the current thread
    buf = pop(buf,  esi_opnd);
    buf = pop(buf,  ebx_opnd);
    buf = mov(buf,  M_Base_Opnd(ebx_reg, +0),  esi_opnd);
    buf = alu(buf, add_opc,  esp_opnd,  Imm_Opnd(+16));
    
    // TODO: check if there is no need to restore callee saved registers
    // JUSTIFICATION: m2n frame is popped as a result of "normal"
    //                (opposite to destuctive) stack unwinding
    // Restore callee saved general registers
    if (num_callee_saves<4) buf = pop(buf,  edi_opnd);
    if (num_callee_saves<3) buf = pop(buf,  esi_opnd);
    if (num_callee_saves<2) buf = pop(buf,  ebx_opnd);
    if (num_callee_saves<1) buf = pop(buf,  ebp_opnd);

    return buf;
}

// returns pointer to the registers used for jvmti PopFrame
Registers* get_pop_frame_registers(M2nFrame* m2nf) {
    return m2nf->pop_regs;
}

// sets pointer to the registers used for jvmti PopFrame
void set_pop_frame_registers(M2nFrame* m2nf, Registers* regs) {
    m2nf->pop_regs = regs;
}

