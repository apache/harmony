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
#define LOG_DOMAIN "vm.helpers"
#include "cxxlog.h"

#include <string.h>

#include "open/types.h"
#include "port_malloc.h"
#include "vm_threads.h"
#include "exceptions.h"

#include "m2n.h"
#include "encoder.h"
#include "m2n_em64t_internal.h"
#include "lil_code_generator_em64t.h"

/*    Generic Interface    */

void m2n_null_init(M2nFrame * m2n){
    memset(m2n, 0, sizeof(M2nFrame));
}

M2nFrame* m2n_get_last_frame() {
    return (M2nFrame*)p_TLS_vmthread->last_m2n_frame;
}

M2nFrame* m2n_get_last_frame(VM_thread * thread) {
    return (M2nFrame*)thread->last_m2n_frame;
}

void m2n_set_last_frame(VM_thread* thread, M2nFrame * lm2nf) {
    thread->last_m2n_frame = lm2nf;
}

void m2n_set_last_frame(M2nFrame * lm2nf) {
    vm_thread_t vm_thread = jthread_self_vm_thread_unsafe();
    vm_thread->last_m2n_frame = lm2nf;
}

M2nFrame * m2n_get_previous_frame(M2nFrame * m2nf) {
    return m2nf->prev_m2nf;
}

ObjectHandles* m2n_get_local_handles(M2nFrame * m2nf) {
    return m2nf->local_object_handles;
}

void m2n_set_local_handles(M2nFrame * m2nf, ObjectHandles * handles) {
    m2nf->local_object_handles = handles;
}

NativeCodePtr m2n_get_ip(M2nFrame * m2nf) {
    return (NativeCodePtr *) m2nf->rip;
}

void m2n_set_ip(M2nFrame * lm2nf, NativeCodePtr ip) {
    lm2nf->rip = (uint64)ip;
} 

Method_Handle m2n_get_method(M2nFrame * m2nf) {
    return m2nf->method;
}

frame_type m2n_get_frame_type(M2nFrame * m2nf) {
    return m2nf->current_frame_type;
}

void m2n_set_frame_type(M2nFrame * m2nf, frame_type m2nf_type) 
{
    m2nf->current_frame_type = m2nf_type;
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

    m2nf->rip  = (POINTER_SIZE_INT)regs->get_ip();
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
    return (uint64)m2nf->p_lm2nf == 1;

}

void * m2n_get_frame_base(M2nFrame * m2nf) {
    return &m2nf->rip;
}

/*    Internal Interface    */

unsigned m2n_ts_to_register_size(unsigned num_std_need_to_save,
                                 unsigned num_ret_need_to_save) {
    return 13 + (6 * num_std_need_to_save) +
            3 + (6 * num_ret_need_to_save);
}

// rsp should point to the bottom of the activation frame since push may occur
// inputs should be preserved outside if required since we do a call
// num_std_need_to_save registers will be preserved
char * m2n_gen_ts_to_register(char * buf, const R_Opnd * reg,
                              unsigned num_callee_saves_used,
                              unsigned num_callee_saves_max,
                              unsigned num_std_need_to_save,
                              unsigned num_ret_need_to_save) {
    // we can't preserve rax and return value on it at the same time
    assert (num_ret_need_to_save == 0 || reg != &rax_opnd);


//#ifdef PLATFORM_POSIX

    // preserve std places
    unsigned i;
    unsigned num_std_saved = 0;
    // use calle-saves registers first
        while (num_std_saved < num_std_need_to_save &&
        (i = num_callee_saves_used + num_std_saved) < num_callee_saves_max) {
            buf = mov(buf,
                LcgEM64TContext::get_reg_from_map(
                LcgEM64TContext::GR_LOCALS_OFFSET + i),
                LcgEM64TContext::get_reg_from_map(
                    LcgEM64TContext::STD_PLACES_OFFSET + num_std_saved),
                size_64);
            ++num_std_saved;
        }
    // if we still have not preserved std places save them on the stack
    while (num_std_saved < num_std_need_to_save) {
        buf = push(buf,
            LcgEM64TContext::get_reg_from_map(
                LcgEM64TContext::STD_PLACES_OFFSET + num_std_saved),
            size_64);
        ++num_std_saved;
    }
    assert(num_std_saved == num_std_need_to_save);

    // preserve returns
    unsigned num_ret_saved = 0;
        while (num_ret_saved < num_ret_need_to_save &&
        (i = num_callee_saves_used + num_std_saved + num_ret_saved) < num_callee_saves_max) {
            buf = mov(buf,
                LcgEM64TContext::get_reg_from_map(
                    LcgEM64TContext::GR_LOCALS_OFFSET + i),
                LcgEM64TContext::get_reg_from_map(
                    LcgEM64TContext::GR_RETURNS_OFFSET + num_ret_saved),
                    size_64);
                ++num_ret_saved;
            }
    // if we still have not preserved returns save them on the stack
    while (num_ret_saved < num_ret_need_to_save) {
        buf = push(buf,
            LcgEM64TContext::get_reg_from_map(
                LcgEM64TContext::GR_RETURNS_OFFSET + num_std_saved),
            size_64);
        ++num_ret_saved;
    }
    assert(num_ret_saved == num_ret_need_to_save);

    // TODO: FIXME: only absolute addressing mode is supported now
    buf = mov(buf, rax_opnd, Imm_Opnd(size_64, (uint64)get_thread_ptr), size_64);
#ifdef _WIN64
    buf = alu(buf, add_opc, rsp_opnd, Imm_Opnd(-SHADOW));
#endif
    buf = call(buf, rax_opnd, size_64);
#ifdef _WIN64
    buf = alu(buf, add_opc, rsp_opnd, Imm_Opnd(SHADOW));
#endif
    if (reg != &rax_opnd) {
        buf = mov(buf, *reg,  rax_opnd, size_64);
    }

    // restore returns from the stack
    i = num_callee_saves_used + num_std_saved;
    while (num_ret_saved > 0 && i + num_ret_saved > num_callee_saves_max) {
            --num_ret_saved;
        buf = pop(buf,
            LcgEM64TContext::get_reg_from_map(
                LcgEM64TContext::GR_RETURNS_OFFSET + num_ret_saved),
            size_64);
        }
    // restore std places from the stack
    while (num_std_saved > 0 && num_callee_saves_used + num_std_saved > num_callee_saves_max) {
            --num_std_saved;
        buf = pop(buf,
            LcgEM64TContext::get_reg_from_map(
                LcgEM64TContext::STD_PLACES_OFFSET + num_std_saved),
            size_64);
        }
    // restore returns from callee-saves registers
    i = num_callee_saves_used + num_std_saved;
        while (num_ret_saved > 0) {
            --num_ret_saved;
        buf = mov(buf,
            LcgEM64TContext::get_reg_from_map(
                LcgEM64TContext::GR_RETURNS_OFFSET + num_ret_saved),
                LcgEM64TContext::get_reg_from_map(
                LcgEM64TContext::GR_LOCALS_OFFSET + i + num_ret_saved),
                size_64);
        }
    // restore std places from callee-saves registers
        while (num_std_saved > 0) {
            --num_std_saved;
        buf = mov(buf,
            LcgEM64TContext::get_reg_from_map(
                LcgEM64TContext::STD_PLACES_OFFSET + num_std_saved),
                LcgEM64TContext::get_reg_from_map(
                LcgEM64TContext::GR_LOCALS_OFFSET + num_callee_saves_used + num_std_saved),
                size_64);
        }
//#else //!PLATFORM_POSIX
//    buf = prefix(buf, prefix_fs);
//    buf = mov(buf, *reg,  M_Opnd(0x14), size_64);
//#endif //!PLATFORM_POSIX
    return buf;
}

char * m2n_gen_set_local_handles_r(char * buf, unsigned bytes_to_m2n, const R_Opnd * src_reg) {
    unsigned offset_local_handles = (unsigned)(uint64) &((M2nFrame*)0)->local_object_handles;
    buf = mov(buf, M_Base_Opnd(rsp_reg, bytes_to_m2n + offset_local_handles), *src_reg, size_64);
    return buf;
}

char * m2n_gen_set_local_handles_imm(char * buf, unsigned bytes_to_m2n, const Imm_Opnd * imm) {
    unsigned offset_local_handles = (unsigned)(uint64)&((M2nFrame*)0)->local_object_handles;
    buf = mov(buf, M_Base_Opnd(rsp_reg, bytes_to_m2n + offset_local_handles), *imm, size_64);
    return buf;
}

unsigned m2n_push_m2n_size(unsigned num_callee_saves,
                           unsigned num_std_need_to_save) {
    return 91 - (5 * num_callee_saves) +
            m2n_ts_to_register_size(num_std_need_to_save, 0);
}

// inputs should be preserved outside if required since we do a call
// num_std_need_to_save registers will be preserved
char * m2n_gen_push_m2n(char * buf, Method_Handle method, 
                        frame_type current_frame_type, 
                        bool handles,
                        unsigned num_callee_saves,
                        unsigned num_std_need_to_save,
                        I_32 bytes_to_m2n_top) {
    // skip callee-saves registers
    bytes_to_m2n_top -= num_callee_saves * LcgEM64TContext::GR_SIZE;
    // TODO: check if it makes sense to save all callee-saves registers here
    //store rest of callee-saves registers
    for (unsigned i = num_callee_saves; i < LcgEM64TContext::MAX_GR_LOCALS; i++) {
        bytes_to_m2n_top -= LcgEM64TContext::GR_SIZE;
        buf = mov(buf,
            M_Base_Opnd(rsp_reg, bytes_to_m2n_top),
            LcgEM64TContext::get_reg_from_map(LcgEM64TContext::GR_LOCALS_OFFSET + i),
            size_64);        
    }
    // init pop_regs to null
    bytes_to_m2n_top -= LcgEM64TContext::GR_SIZE;
    buf = mov(buf, M_Base_Opnd(rsp_reg, bytes_to_m2n_top),
        Imm_Opnd(size_32, 0), size_64);
    // store current_frame_type
    bytes_to_m2n_top -= LcgEM64TContext::GR_SIZE;
    assert(fit32(current_frame_type));
    buf = mov(buf, M_Base_Opnd(rsp_reg, bytes_to_m2n_top),
        Imm_Opnd(size_32, current_frame_type), size_64);
    // store a method associated with the current m2n frame
    bytes_to_m2n_top -= LcgEM64TContext::GR_SIZE;
    if (fit32((int64)method)) {
        buf = mov(buf, M_Base_Opnd(rsp_reg, bytes_to_m2n_top),
            Imm_Opnd(size_32, (int64)method), size_64);
    } else {
        buf = mov(buf, rax_opnd, Imm_Opnd(size_64, (int64)method), size_64);
        buf = mov(buf, M_Base_Opnd(rsp_reg, bytes_to_m2n_top), rax_opnd);
    }
    // store local object handles
    bytes_to_m2n_top -= LcgEM64TContext::GR_SIZE;
    buf = mov(buf, M_Base_Opnd(rsp_reg, bytes_to_m2n_top),
        Imm_Opnd(size_64, (int64)0), size_64);

    // move pointer to the current VM_Thread structure to rax
    buf = m2n_gen_ts_to_register(buf, &rax_opnd,
        num_callee_saves, LcgEM64TContext::MAX_GR_LOCALS,
        num_std_need_to_save, 0);
    
    // shift to the last_m2n_frame field
    I_32 last_m2n_frame_offset = (I_32)(int64)&((VM_thread*)0)->last_m2n_frame;
    buf = alu(buf, add_opc,  rax_opnd,  Imm_Opnd(size_32, last_m2n_frame_offset), size_64);
    // store pointer to pointer to last m2n frame
    bytes_to_m2n_top -= LcgEM64TContext::GR_SIZE;
    buf = mov(buf, M_Base_Opnd(rsp_reg, bytes_to_m2n_top), rax_opnd, size_64);
    // save pointer to the previous m2n frame
    bytes_to_m2n_top -= LcgEM64TContext::GR_SIZE;
    buf = mov(buf, r9_opnd, M_Base_Opnd(rax_reg, 0));
    buf = mov(buf, M_Base_Opnd(rsp_reg, bytes_to_m2n_top), r9_opnd, size_64);
    // update last m2n frame of the current thread
    buf = lea(buf, r9_opnd, M_Base_Opnd(rsp_reg, bytes_to_m2n_top));
    buf = mov(buf,  M_Base_Opnd(rax_reg, 0), r9_opnd, size_64);
    return buf;
}

unsigned m2n_pop_m2n_size(bool handles, unsigned num_callee_saves, unsigned preserve_ret)
{
    return 56 - 5*num_callee_saves + (preserve_ret ? 4: 0);
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

char * m2n_gen_pop_m2n(char * buf, bool handles, unsigned num_callee_saves,
                      I_32 bytes_to_m2n_bottom, unsigned num_preserve_ret) {
    assert (num_preserve_ret <= 2);
    assert(LcgEM64TContext::GR_SIZE == 8);

    if (num_preserve_ret > 0) {
        // Save return value
        // NOTE: don't break stack allignment by pushing only one register.
        buf = push(buf,  rax_opnd, size_64);
        buf = push(buf,  rdx_opnd, size_64);
    }

    if (handles) {
        // There are handles located on the stack
        buf = mov(buf, rax_opnd, Imm_Opnd(size_64, (uint64)m2n_pop_local_handles), size_64);
    } else {
        buf = mov(buf, rax_opnd, Imm_Opnd(size_64, (uint64)m2n_free_local_handles), size_64);
    }
    
    // NOTE: the following should be true before the call ($rsp % 8 == 0 && $rsp % 16 != 0)!

    // Call m2n_pop_local_handles or m2n_free_local_handles
#ifdef _WIN64
    buf = alu(buf, add_opc, rsp_opnd, Imm_Opnd(-SHADOW));
#endif
    buf = call(buf, rax_opnd, size_64);
#ifdef _WIN64
    buf = alu(buf, add_opc, rsp_opnd, Imm_Opnd(SHADOW));
#endif
    
    if (num_preserve_ret > 0) {
        // Restore return value
        buf = pop(buf,  rdx_opnd, size_64);
        buf = pop(buf,  rax_opnd, size_64);
    }

    // pop prev_m2nf
    buf = mov(buf, r10_opnd, M_Base_Opnd(rsp_reg, bytes_to_m2n_bottom), size_64);
    bytes_to_m2n_bottom += LcgEM64TContext::GR_SIZE;
    // pop p_lm2nf
    buf = mov(buf, r11_opnd, M_Base_Opnd(rsp_reg, bytes_to_m2n_bottom), size_64);
    bytes_to_m2n_bottom += LcgEM64TContext::GR_SIZE;
    buf = mov(buf, M_Base_Opnd(r11_reg, 0), r10_opnd, size_64);
    // skip local_object_handles, method, current_frame_type, pop_regs
    bytes_to_m2n_bottom += 4 * LcgEM64TContext::GR_SIZE;

    // restore part of callee-saves registers
    for (int i = LcgEM64TContext::MAX_GR_LOCALS - 1; i >= (int)num_callee_saves; i--) {
        buf = mov(buf,
            LcgEM64TContext::get_reg_from_map(LcgEM64TContext::GR_LOCALS_OFFSET + i),
            M_Base_Opnd(rsp_reg, bytes_to_m2n_bottom),
            size_64);
        bytes_to_m2n_bottom += LcgEM64TContext::GR_SIZE;
    }

    return buf;
}//m2n_gen_pop_m2n

// returns pointer to the registers used for jvmti PopFrame
Registers* get_pop_frame_registers(M2nFrame* m2nf) {
    return m2nf->pop_regs;
}

// sets pointer to the registers used for jvmti PopFrame
void set_pop_frame_registers(M2nFrame* m2nf, Registers* regs) {
    m2nf->pop_regs = regs;
}

