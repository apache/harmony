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

#ifndef _M2N_EM64T_INTERNAL_H_
#define _M2N_EM64T_INTERNAL_H_

// This file describes the internal EM64T interface of m2n frames.
// It can be used by stubs to generate code to push and pop m2n frames, to update object handles fields, and 
// to access the arguments from managed to native code.
// It is also used by stack iterators.

#include "m2n.h"
#include "open/types.h"
#include "encoder.h"

#ifdef _WIN64
const unsigned m2n_sizeof_m2n_frame = 120;
#else
const unsigned m2n_sizeof_m2n_frame = 104;
#endif

typedef struct M2nFrame M2nFrame;

/**
 * There are two types of M2nFrames: those that result from managed code calling a stub,
 * and those that represent suspended managed code. The second type is needed to deal with
 * throwing exceptions from OS contexts with the exception filter or signal mechanisms.
 * For the first type:
 *  rip points to the instruction past the one in question
 *  the bottom two bits of p_lm2nf are zero
 *  regs is not present, and is implicitly the address of the word above rip
 * For the second type:
 *   rip points to the instruction in question
 *   p_lm2nf==1
 *   regs is present
 */
struct M2nFrame {
    M2nFrame *           prev_m2nf;
    M2nFrame **          p_lm2nf;
    ObjectHandles *      local_object_handles;
    Method_Handle        method;
    frame_type           current_frame_type;
    Registers*           pop_regs; // This is only for M2nFrames for suspended managed code (as against ones that call stubs and prepare jvmtiPopFrame)
    uint64               rbx;
    uint64               rbp;
#ifdef _WIN64
    uint64               rsi;
    uint64               rdi;
#endif
    uint64               r15;
    uint64               r14;
    uint64               r13;
    uint64               r12;
    uint64               rip;
    // This is only for M2nFrames for suspended managed code (as against ones that call stubs)
    Registers *          regs;
};

/**
 * returns size of m2n frame in bytes
 */
inline size_t m2n_get_size() {
    return sizeof(M2nFrame);
}

/**
 * Generate code to put the thread local storage pointer into a given register.
 * It destroys outputs.
 */
char * m2n_gen_ts_to_register(char * buf, const R_Opnd * reg,
                              unsigned num_callee_saves_used, unsigned num_callee_saves_max,
                              unsigned num_std_need_to_save, unsigned num_ret_need_to_save);
unsigned m2n_ts_to_register_size(unsigned num_std_need_to_save, unsigned num_ret_need_to_save);

/**
 * Generate code to set the local handles of an M2nFrame.
 * The M2nFrame is located bytes_to_m2n above rsp, and src_reg has the address of the frames.
 */
char * m2n_gen_set_local_handles_r(char * buf, unsigned bytes_to_m2n, const R_Opnd * src_reg);
char * m2n_gen_set_local_handles_imm(char * buf, unsigned bytes_to_m2n, const Imm_Opnd * imm);

/**
 * Generate code to push an M2nFrame onto the stack.
 * It assumes that num_callee_saves registers have already been saved and the rest have been preserved,
 * that the saved registers are immediately below the return address, and that rsp points to the last one saved.
 * The order for callee saves is r12, r13, r14, r15, rbp, rbx.
 * It destroys returns (rax) and outputs.
 * After the sequence, rsp points to the M2nFrame.
 *
 * @param handles Indicates whether the stub will want local handles or not
 * @param bytes_to_m2n_top Number of bytes to the  beginning of m2n frame relative to the current rsp value.
                           Negative value means that current rsp is above m2n bottom.
 */
char * m2n_gen_push_m2n(char * buf, Method_Handle method, frame_type current_frame_type, bool handles,
                        unsigned num_callee_saves, unsigned num_std_need_to_save, I_32 bytes_to_m2n_top);
unsigned m2n_push_m2n_size(unsigned num_callee_saves, unsigned num_std_need_to_save);

/**
 * Generate code to pop an M2nFrame off the stack.
 * @param num_callee_saves Number of callee saves registers to leave
 *                         on the stack as at the entry to push_m2n.
 * @param bytes_to_m2n_bottom Number of bytes between rsp and the bottom of the M2nFrame.
 * @param preserve_ret Number of return registers to preserve, 0 means none,
 *                     1 means rax, 2 means rax & rdx.
 * @param handles As for push_m2n, frees the handles if true.
 */
char * m2n_gen_pop_m2n(char * buf, bool handles, unsigned num_callee_saves,
                       I_32 bytes_to_m2n_bottom, unsigned preserve_ret);
unsigned m2n_pop_m2n_size(bool handles, unsigned num_callee_saves, unsigned preserve_ret);

// returns top of the specified frame on the stack (it should point to return ip)
void * m2n_get_frame_base(M2nFrame *);

#endif // _M2N_EM64T_INTERNAL_H_
