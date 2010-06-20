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
 * @author Ilya Berezhniuk
 */

#include "dec_base.h"
#include "port_modules.h"
#include "native_unwind.h"


static unsigned native_dec_instr(UnwindContext* context, void* addr, void** target)
{
    Inst inst;

    if (!native_is_in_code(context, addr))
        return 0;

    U_32 len = DecoderBase::decode(addr, &inst);

    if (len == 0 ||
        inst.mn != Mnemonic_CALL ||
        inst.argc != 1)
        return 0;

    if (target && inst.operands[0].is_imm())
        *target = (void*)((U_32)addr + len + inst.operands[0].imm());

    return len;
}

static bool native_check_caller(UnwindContext* context, Registers* regs, void** sp)
{
    void* target = NULL;
    char* ptr = (char*)*sp;
    
    if (native_dec_instr(context, ptr - 2, &target) == 2 || // CALL r/m32 w/o SIB w/o disp
        native_dec_instr(context, ptr - 3, &target) == 3 || // CALL r/m32 w/ SIB w/o disp
        native_dec_instr(context, ptr - 4, &target) == 4 || // CALL r/m32 w/ SIB w/ disp8
        native_dec_instr(context, ptr - 5, &target) == 5 || // CALL rel32
        native_dec_instr(context, ptr - 6, &target) == 6 || // CALL r/m32 w/o SIB w/ disp32
        native_dec_instr(context, ptr - 7, &target) == 7 || // CALL r/m32 w/ SIB w/ disp32
        native_dec_instr(context, ptr - 8, &target) == 8)   // CALL r/m32 w/ SIB w/ disp32 + Seg prefix
    {
        if (!target)
            return true;

        native_module_t* cur_module =
            port_find_module(context->modules, regs->get_ip());
        native_module_t* found_module =
            port_find_module(context->modules, target);

        return (cur_module == found_module);
    }

    return false;
}

bool native_is_frame_exists(UnwindContext* context, Registers* regs)
{
    // Check for frame layout and stack values
    if ((regs->ebp < regs->esp) || !native_is_in_stack(context, (void*)regs->ebp))
        return false; // Invalid frame

    void** frame_ptr = (void**)regs->ebp;
    void* eip = frame_ptr[1]; // Return address

    // Check return address for meaning
    return (native_is_in_code(context, eip) && native_check_caller(context, regs, frame_ptr + 1));
}

bool native_unwind_stack_frame(UnwindContext* context, Registers* regs)
{
    void** frame = (void**)regs->ebp;

    void* ebp = frame[0];
    void* eip = frame[1];
//    void* esp = (void*)(frame + 2);
    void* esp = &frame[2];


    if (native_is_in_stack(context, esp) &&
        (native_is_in_code(context, eip)))
    {
        regs->ebp = (U_32)ebp;
        regs->esp = (U_32)esp;
        regs->eip = (U_32)eip;
        return true;
    }

    return false;
}

static bool fill_regs_from_sp(UnwindContext* context, Registers* regs, void** sp)
{
    regs->esp = (U_32)(sp + 1);
    regs->eip = (U_32)*sp;
    regs->ebp = native_is_in_stack(context, sp[-1]) ? (U_32)sp[-1] : regs->esp;
    return true;
}


// Max search depth for return address
#define MAX_SPECIAL_DEPTH 0x2800
#define NATIVE_STRICT_UNWINDING 1

bool native_unwind_special(UnwindContext* context, Registers* regs)
{
    for (void** cur_sp = (void**)regs->esp;
         (char*)cur_sp < ((char*)regs->esp + MAX_SPECIAL_DEPTH) && native_is_in_stack(context, cur_sp);
         ++cur_sp)
    {
        if (!native_is_in_code(context, *cur_sp))
            continue;

#if (!NATIVE_STRICT_UNWINDING)
        return fill_regs_from_sp(context, regs, cur_sp);
#else
        if (native_check_caller(context, regs, cur_sp))
            return fill_regs_from_sp(context, regs, cur_sp);
#endif
    }

    return false;
}
