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
 * @author Intel, Evgueni Brevnov, Ivan Volosyuk
 */  


#ifndef _M2N_IA32_INTERNAL_H_
#define _M2N_IA32_INTERNAL_H_

// This file describes the internal IPF interface of m2n frames.
// It can be used by stubs to generate code to push and pop m2n frames, to update object handles fields, and 
// to access the arguments from managed to native code.
// It is also used by stack iterators.

#include "m2n.h"
#include "vm_threads.h"
#include "open/types.h"

class R_Opnd;

// Return a pointer to the argument just above the return address of an M2nFrame
U_32* m2n_get_args(M2nFrame*);

// An M2nFrame is a structure that resides on the stack.
// It takes up space below and including the return address to the managed code, and thus is immediately below the arguments.

// This is the size of the structure that goes on the stack.
const unsigned m2n_sizeof_m2n_frame = 44;

// Generate code to put the thread local storage pointer into a given register
unsigned m2n_ts_to_register_size();
char* m2n_gen_ts_to_register(char* buf, R_Opnd* reg);

// Generate code to push an M2nFrame onto the stack.
// It assumes that num_callee_saves registers have already been saved and the rest have been preserved, that the saved registers are immediately
// below the return address, and that esp points to the last one saved.  The order for callee saves is ebp, ebx, esi, edi.
// It destroys eax.
// After the sequence, esp points to the M2nFrame.
// handles: will the stub want local handles or not
unsigned m2n_push_m2n_size(bool handles, unsigned num_callee_saves);
char* m2n_gen_push_m2n(char* buf, Method_Handle method, frame_type current_frame_type, bool handles, unsigned num_callee_saves);

// Generate code to set the local handles of an M2nFrame
// The M2nFrame is located bytes_to_m2n above esp, and src_reg has the address of the frames.
unsigned m2n_set_local_handles_size(unsigned bytes_to_m2n);
char* m2n_gen_set_local_handles(char* buf, unsigned bytes_to_m2n, R_Opnd* src_reg);

// Generate code to pop an M2nFrame off the stack.
// num_callee_saves: the number of callee saves registers to leave on the stack as at the entry to push_m2n.
// extra_on_stack: the number of bytes between esp and the bottom of the M2nFrame.
// preserve_ret: the number of return registers to preserve, 0 means none, 1 means eax, 2 means eax & edx; st(0) is always preserved.
// handles: as for push_m2n, frees the handles if true.
unsigned m2n_pop_m2n_size(bool handles, unsigned num_callee_saves, unsigned extra_on_stack, unsigned preserve_ret);
char* m2n_gen_pop_m2n(char* buf, bool handles, unsigned num_callee_saves, unsigned extra_on_stack, unsigned preserve_ret);

//////////////////////////////////////////////////////////////////////////
// Implementation details

// This information is used by the m2n implementation and the stack iterator implementation.
// It may not be used by any other module.

// There are two types of M2nFrames: those that result from managed code calling a stub, and those that represent suspended managed code.
// The second type is needed to deal with throwing exceptions from OS contexts with the exception filter or signal mechanisms.
// For the first type:
//   eip points to the instruction past the one in question (ie is the return address into managed code)
//   the bottom two bits of p_lm2nf are zero
//   regs is not present, and is implicitly the address of the word above eip
// For the second type:
//   eip points to the instruction in question
//   p_lm2nf==1
//   regs is present

#ifdef _EM64T_
#error Wrong header file.
#endif

struct M2nFrame {
    M2nFrame*            prev_m2nf;
    M2nFrame**           p_lm2nf;
    ObjectHandles*       local_object_handles;
    Method_Handle        method;
    frame_type           current_frame_type; // type of the current frame also shows is the frame unwindable
    Registers*           pop_regs; // This is only for M2nFrames for suspended managed code (as against ones that call stubs and prepare jvmtiPopFrame)
    U_32               edi;
    U_32               esi;
    U_32               ebx;
    U_32               ebp;
    U_32               eip;
    Registers*           regs; // This is only for M2nFrames for suspended managed code (as against ones that call stubs and prepare jvmtiPopFrame)
};

#endif //!_M2N_IA32_INTERNAL_H_
