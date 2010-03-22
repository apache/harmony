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




#ifndef _VM_IPF_H_
#define _VM_IPF_H_

#include "open/types.h"

#define SCRATCH_PRED_REG        6
#define SCRATCH_PRED_REG2       7
#define SCRATCH_PRED_REG3       8
#define SCRATCH_PRED_REG4       9
#define SCRATCH_PRED_REG5      10
#define SCRATCH_PRED_REG6      11
#define SCRATCH_PRED_REG7      12
#define SCRATCH_PRED_REG8      13
#define SCRATCH_PRED_REG9      14
#define SCRATCH_PRED_REG10     15
#define SCRATCH_BRANCH_REG      6
#define SCRATCH_BRANCH_REG2     7
#define BRANCH_RETURN_LINK_REG  0
#define GP_REG                  1
#define RETURN_VALUE_REG        8
#define SCRATCH_GENERAL_REG    14
#define SCRATCH_GENERAL_REG2   15
#define SCRATCH_GENERAL_REG3   16
#define SCRATCH_GENERAL_REG4   17
#define SCRATCH_GENERAL_REG5   18
#define SCRATCH_GENERAL_REG6   19
#define SCRATCH_GENERAL_REG7   20
#define SCRATCH_GENERAL_REG8   21
#define SCRATCH_GENERAL_REG9   22
#define SCRATCH_GENERAL_REG10  23
#define SCRATCH_GENERAL_REG11  24
#define SCRATCH_GENERAL_REG12  25
#define SCRATCH_GENERAL_REG13  26
#define SCRATCH_GENERAL_REG14  27
#define SCRATCH_GENERAL_REG15  28
#define SCRATCH_GENERAL_REG16  29
#define SCRATCH_GENERAL_REG17  30
#define SCRATCH_GENERAL_REG18  31
#define PRESERV_GENERAL_REG1    5
#define PRESERV_GENERAL_REG2    6
#define SP_REG                 12
#define FIRST_PRES_FP_REG      16
#define LAST_PRES_FP_REG       31
#define FIRST_FP_ARG_REG        8
#define LAST_FP_ARG_REG        15

// br2 is used for keeping function address to be called.
#define BRANCH_CALL_REG SCRATCH_BRANCH_REG2

#define IN_REG0                32
#define IN_REG1                33
#define IN_REG2                34
#define IN_REG3                35
#define IN_REG4                36
#define IN_REG5                37
#define IN_REG6                38
#define IN_REG7                39




// VM's thread pointer is r4.  According to IPF software conventions,
// r4 is a preserved register but in VM it must contain the thread pointer
// at all times, so the JIT can't use it for storing other values.
#define THREAD_PTR_REG                         4

#define THREAD_ID_REG                          5

#define HEAP_BASE_REG                          6


#define MASK64(_num_bits_in_mask, _shift_in_mask)  (((((uint64)1) << _num_bits_in_mask) - 1) << _shift_in_mask)
#define EXTRACT64(_value, _num_bits, _shift)       ((_value & MASK64(_num_bits, _shift)) >> _shift)
#define EXTRACT64_SOF(_pfs)                          (EXTRACT64(_pfs, 7, 0))
#define EXTRACT64_SOL(_pfs)                          (EXTRACT64(_pfs, 7, 7))



extern "C" void flush_cache_line(void *addr);
extern "C" void sync_i_cache();
extern "C" void *do_flushrs();
extern "C" void do_mf();
extern "C" void do_loadrs(int loadrs);

void flush_hw_cache(U_8* addr, /*int*/ size_t len);

// Save the unwind info passed as the first argument.  Verify that the address
// passed as the second argument is correct.
void* save_unwind_info(void* ar_bsp, void** saved_ar_bsp_addr);



typedef struct FunctionDescriptorIPF
{
    void *entry_point;
    void *gp_value;
} FunctionDescriptorIPF;


#define GET_ENTRY_POINT_IPF(_addr)     (((FunctionDescriptorIPF *)_addr)->entry_point)
#define GET_GP_VALUE_IPF(_addr)        (((FunctionDescriptorIPF *)_addr)->gp_value)

#endif
