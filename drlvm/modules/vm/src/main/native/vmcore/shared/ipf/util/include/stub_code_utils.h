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
//
//  Routines used by the VM to emit stub code for the IPF. These routines use the IPF code compactor.
//


#ifndef _STUB_CODE_UTILS_H
#define _STUB_CODE_UTILS_H

#include <stdio.h>

#include "Code_Emitter.h"
#include "vm_ipf.h"

// Returns the gp value that the managed code should use.  Ideally, this is the
// same gp value as most of the C code in the VM called by the managed code, so
// saving/restoring gp is minimized.
void *get_vm_gp_value();



// Emits an alloc wrapper for calling a single function, only saving
// gp if necessary.  It returns the register numbers for the first
// out-arg, the saved ar.pfs, the saved b0, and the saved gp.
void emit_alloc_for_single_call(Merced_Code_Emitter& emitter,
                                int num_in_args,
                                int num_out_args,
                                void **function,
                                int& out0_reg,
                                int& save_pfs_reg,
                                int& save_b0_reg,
                                int& save_gp_reg);



// Emits the "dealloc" instructions corresponding to emit_alloc_for_single_call().
// The input arguments should be the same values returned by
// emit_alloc_for_single_call().
void emit_dealloc_for_single_call(Merced_Code_Emitter& emitter,
                                  int save_pfs_reg,
                                  int save_b0_reg,
                                  int save_gp_reg,
                                  int pred = 0);


// Emit a call instruction.  Before the call, the gp is saved in the
// register given as the third argument and gp is set to the value specified
// in the function pointer.  After the call, the gp is restored.
// If saved_gp_reg is not specified, then gp is neither saved nor restored.
void emit_call_with_gp(Merced_Code_Emitter& emitter,
                       void **proc_ptr,
                       bool flushrs,
                       int saved_gp_reg = 0);

// Emit a branch instruction.  Before the branch, the gp is set to the
// value specified in the function pointer.
// This function is normally used to implement tail calls.
void emit_branch_with_gp(Merced_Code_Emitter& emitter,
                         void **proc_ptr);


void emit_movl_compactor(Merced_Code_Emitter& emitter, unsigned dst_reg, uint64 u64_value, unsigned pred=0);

// Convenience procedure to select an appropriate mov instruction for a 64-bit immediate
void emit_mov_imm_compactor(Merced_Code_Emitter& emitter, unsigned dst_reg, uint64 imm, unsigned pred=0);


// Finalize the generation of the stub contained in the emitter object:
// allocated memory, copy the code, sync the caches, add a VTune symbol.
void *finalize_stub(Merced_Code_Emitter &emitter, const char *name);


// Generate code that increments an VM_STATS counter.
// The code in this function is guarded by #ifdef VM_STATS
// so it can be called unconditionally without a performance
// impact.
void increment_stats_counter(Merced_Code_Emitter &emitter, void *counter_addr, unsigned pred=0);


// Generate code that compares a given src register against
// the managed null constant.  It exploits the fact that
// only the lower 32 bits of the register need to be checked,
// and that the lower 32 bits are usually 0.
// The semantics are:
//   cmp4.eq  p1,p2=src,(U_32)managed_null
void gen_compare_to_managed_null(Merced_Code_Emitter &emitter,
                                 int predicate1, int predicate2, // predicate regs to set
                                 int src, // register to compare against
                                 int scratch // scratch register to use if needed
                                 );


void gen_convert_class_arg(Merced_Code_Emitter &emitter, bool check_null);


Boolean jit_clears_ccv_in_monitor_enter();

// Add additional code that enforces calling conventions (most likely regarding
// scratch registers) before calling/returing into managed code.
void enforce_calling_conventions(Merced_Code_Emitter *emitter, int pred=0);


// ST a function that prints the value of a register, along with a message
// it presupposes the existence of two output regs; the values of these regs
// are preserved.
void emit_print_reg(Merced_Code_Emitter &emitter, char *msg, unsigned print_reg, unsigned num_inputs, unsigned first_output, bool do_alloc=true);

#endif // _STUB_CODE_UTILS_H


