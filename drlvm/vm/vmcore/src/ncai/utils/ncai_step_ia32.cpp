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

#define LOG_DOMAIN "ncai.step"
#include "cxxlog.h"
#include "port_crash_handler.h"
#include "jvmti_break_intf.h"

#include "ncai_utils.h"
#include "ncai_direct.h"
#include "ncai_internal.h"

// Instrument predicted exit points for STEP_OUT mode
static bool ncai_instrument_exits(void* addr, NCAISingleStepState* sss);


void ncai_setup_single_step(GlobalNCAI* ncai,
            const VMBreakPoint* bp, jvmti_thread_t jvmti_thread)
{
    VMBreakPoints* vm_brpt = VM_Global_State::loader_env->TI->vm_brpt;
    LMAutoUnlock lock(vm_brpt->get_lock());
    NCAISingleStepState* sss = jvmti_thread->ncai_ss;

    TRACE2("ncai.step", "Setup predicted single step breakpoints for address "
        << ((bp) ? bp->addr : NULL));

    if (!ncai->step_enabled) // SS is now disabled
    {
        if (sss)
            ncai_stop_thread_single_step(jvmti_thread);
        return;
    }

    assert(sss && sss->breakpoints);
    sss->breakpoints->remove_all_reference();

    VMBreakInterface* intf = sss->breakpoints;
    ncaiStepMode step_mode = ncai_get_thread_ss_mode(jvmti_thread);

    // Set breakpoints to possible next instructions
    // If step mode is STEP_OFF, set it like for STEP_INTO
    if (step_mode == NCAI_STEP_OFF)
        step_mode = NCAI_STEP_INTO;

    InstructionDisassembler* dasm = bp->disasm;
    InstructionDisassembler::Type type = dasm->get_type();
    assert(type != InstructionDisassembler::OPCODEERROR);
    char* address = (char*)bp->addr;
    jint length = dasm->get_length_with_prefix();

    if (step_mode == NCAI_STEP_OUT)
    {
        if (type == InstructionDisassembler::RET)
        {
            sss->flag_out = true;
            step_mode = NCAI_STEP_INTO;
        }
        else
        {
            sss->flag_out = false;
            bool UNREF res = ncai_instrument_exits(bp->addr, sss);
            assert(res);
            return;
        }
    }

    // Set breakpoint to instruction which follows the current one in memory
    if (type == InstructionDisassembler::OTHER ||
        type == InstructionDisassembler::RELATIVE_COND_JUMP ||
        ((type == InstructionDisassembler::RELATIVE_CALL ||
          type == InstructionDisassembler::INDIRECT_CALL) &&
            step_mode == NCAI_STEP_OVER))
    {
        char* next_addr = address + length;

        TRACE2("ncai.step", "Setting up SS breakpoint to instruction "
            << "following linear instruction: " << (void*)next_addr);

        VMBreakPointRef* ref =
            intf->add_reference((NativeCodePtr)next_addr, 0);
        assert(ref);
    }

    // Set breakpoint to result of direct/conditional transfers
    if (type == InstructionDisassembler::RELATIVE_JUMP ||
        type == InstructionDisassembler::RELATIVE_COND_JUMP)
    {
        char* target = (char*)dasm->get_jump_target_address();

        TRACE2("ncai.step", "Setting up SS breakpoint to relative JUMP target: "
            << (void*)target);

        VMBreakPointRef* ref =
            intf->add_reference((NativeCodePtr)target, 0);
        assert(ref);
    }

    // Set breakpoint to result of context-dependent transfers
    if (type == InstructionDisassembler::INDIRECT_JUMP ||
        type == InstructionDisassembler::RET)
    {
        char* target = (char*)dasm->get_target_address_from_context(&bp->regs);

        TRACE2("ncai.step", "Setting up SS breakpoint to "
            << "INDIRECT_JUMP or RET target: " << (void*)target);

        VMBreakPointRef* ref =
            intf->add_reference((NativeCodePtr)target, 0);
        assert(ref);
    }

    // Check if step mode is STEP_INTO and target address is in VM code
    // If so, we should step over this call to avoid deadlocks and recursion
    if (step_mode == NCAI_STEP_INTO &&
        (   type == InstructionDisassembler::RELATIVE_CALL ||
            type == InstructionDisassembler::INDIRECT_CALL))
    {
        char* target;

        if (type == InstructionDisassembler::RELATIVE_CALL)
            target = (char*)dasm->get_jump_target_address();
        else
            target = (char*)dasm->get_target_address_from_context(&bp->regs);

        ncaiModuleKind mod_type =
            ncai_get_target_address_type(target);

        VMBreakPointRef* ref;
        char* next_addr = address + length;

//        if (mod_type == NCAI_MODULE_VM_INTERNAL)
        if (mod_type != NCAI_MODULE_JNI_LIBRARY)
        {
            TRACE2("ncai.step", "Setting up SS breakpoint to "
                << "instruction following CALL: " << (void*)next_addr);

            ref = intf->add_reference((NativeCodePtr)next_addr, 0);
        }
        else
        {
            TRACE2("ncai.step", "Setting up SS breakpoint to "
                << "CALL target: " << (void*)target);

            ref = intf->add_reference((NativeCodePtr)target, 0);
        }

        assert(ref);
    }
}

void ncai_setup_signal_step(jvmti_thread_t jvmti_thread, NativeCodePtr addr)
{
    GlobalNCAI* ncai = VM_Global_State::loader_env->NCAI;
    if (!ncai->isEnabled() || !ncai->step_enabled)
        return;

    TRACE2("ncai.step",
        "Setting breakpoint to HWE handler address: " << addr);

    NCAISingleStepState* sss = jvmti_thread->ncai_ss;
    assert(sss);

    VMBreakPoints *vm_brpt = VM_Global_State::loader_env->TI->vm_brpt;
    LMAutoUnlock lock(vm_brpt->get_lock());

    if (sss && sss->breakpoints)
        sss->breakpoints->remove_all_reference();

    VMBreakInterface* intf = sss->breakpoints;
    ncaiStepMode step_mode = ncai_get_thread_ss_mode(jvmti_thread);

    if (step_mode == NCAI_STEP_OUT)
        sss->flag_out = true; // To report next step

    // Check if target address is in VM code
    ncaiModuleKind mod_type =
        ncai_get_target_address_type(addr);

// Trying to set breakpoint to any type of code
//    if (mod_type != NCAI_MODULE_VM_INTERNAL)
//    if (mod_type == NCAI_MODULE_JNI_LIBRARY)
    {
        VMBreakPointRef* UNREF ref = intf->add_reference(addr, 0);
        assert(ref);
    }
}

//////////////////////////////////////////////////////////////////////////////
//  Utility functions for STEP_OUT processing

struct stAddrListItem
{
    void*           addr;
    stAddrListItem* next;
};

struct stMovItem
{
    InstructionDisassembler*    dasm;
    stMovItem*                  next;
};


static void add_mov_list(stMovItem** plist, InstructionDisassembler* dasm)
{
    bool has_reg = false;
    bool has_imm = false;
    bool has_mem = false;

    for (unsigned i = 0; i < dasm->get_operands_count(); i++)
    {
        const InstructionDisassembler::Opnd& op = dasm->get_opnd(i);
        if (op.kind == InstructionDisassembler::Kind_Reg)
            has_reg = true;
        else if (op.kind == InstructionDisassembler::Kind_Mem)
            has_mem = true;
        else if (op.kind == InstructionDisassembler::Kind_Imm)
            has_imm = true;

        if (has_reg && (has_mem || has_imm))
        {
            stMovItem* mov = (stMovItem*)STD_MALLOC(sizeof(stMovItem));
            assert(mov);
            mov->dasm = dasm;
            mov->next = *plist;
            *plist = mov;
            return;
        }
    }

    delete dasm;
}

static void free_mov_list(stMovItem** plist)
{
    while (*plist)
    {
        stMovItem* next = (*plist)->next;
        InstructionDisassembler* dasm = (*plist)->dasm;
        delete dasm;
        STD_FREE(*plist);
        *plist = next;
    }
}

static bool add_addr(stAddrListItem** plist, void* addr)
{
    for (stAddrListItem* ptr = *plist; ptr; ptr = ptr->next)
    {
        if (ptr->addr == addr)
            return false;
    }

    stAddrListItem* item = (stAddrListItem*)STD_MALLOC(sizeof(stAddrListItem));
    assert(item);
    item->next = *plist;
    item->addr = addr;
    *plist = item;

    return true;
}

static void free_addr_list(stAddrListItem** plist)
{
    while (*plist)
    {
        stAddrListItem* next = (*plist)->next;
        STD_FREE(*plist);
        *plist = next;
    }
}

static inline void add_branch(stAddrListItem** plist, void* addr)
{
    for (stAddrListItem* ptr = *plist; ptr; ptr = ptr->next)
    {
        if (ptr->addr == addr)
            return;
    }

    stAddrListItem* item = (stAddrListItem*)STD_MALLOC(sizeof(stAddrListItem));
    assert(item);

    item->next = *plist;
    item->addr = addr;
    *plist = item;
}

static bool ncai_fill_jump_targets(void* cur, InstructionDisassembler* pdasm,
    stAddrListItem** pbranches, stMovItem* movs)
{
    assert(pdasm->get_type() == InstructionDisassembler::INDIRECT_JUMP);
#define MAX_FUN_SIZE    1024
    // Indirect jump can represent table switch instruction
    // Not implemented yet
    assert(0);
    return true;
}

static InstructionDisassembler* get_local_disasm(void* addr)
{
    VMBreakPoints* vm_brpt = VM_Global_State::loader_env->TI->vm_brpt;
    InstructionDisassembler* pdasm;

    if (port_is_breakpoint_set(addr))
    { // Address was instrumented by another thread or by breakpoint
        VMBreakPoint* bp = vm_brpt->find_breakpoint(addr);
        assert(bp && bp->disasm);
        pdasm = new InstructionDisassembler(*bp->disasm);
    }
    else
        pdasm = new InstructionDisassembler(addr);

    assert(pdasm);
    return pdasm;
}

static bool ncai_instrument_exits(void* addr, NCAISingleStepState* sss)
{
    assert(addr && sss);
    stAddrListItem* branch_list = NULL;
    stMovItem* mov_list = NULL;
    stAddrListItem* addr_list = NULL;
    void* cur = addr;

    while (cur || branch_list)
    {
        if (!cur || !add_addr(&addr_list, cur))
        {
            free_mov_list(&mov_list);
            cur = NULL;

            if (branch_list)
            {
                stAddrListItem* br = branch_list;
                cur = br->addr;
                branch_list = br->next;
                STD_FREE(br);
            }

            continue;
        }

        InstructionDisassembler* pdasm = get_local_disasm(cur);
        InstructionDisassembler::Type type = pdasm->get_type();
        size_t len = (size_t)pdasm->get_length_with_prefix();

        if (type == InstructionDisassembler::INDIRECT_JUMP)
        {
            bool UNREF res =
                ncai_fill_jump_targets(cur, pdasm, &branch_list, mov_list);
            assert(res);

            free_mov_list(&mov_list);
            delete pdasm;
            cur = NULL;
            continue;
        }

        if (type == InstructionDisassembler::RET)
        {
            VMBreakInterface* intf = sss->breakpoints;
            assert(intf);

            VMBreakPointRef* ref =
                intf->add_reference((NativeCodePtr)cur, 0);
            assert(ref);

            free_mov_list(&mov_list);
            delete pdasm;
            cur = NULL;
            continue;
        }

        if (type == InstructionDisassembler::RELATIVE_JUMP)
        {
            void* target = pdasm->get_jump_target_address();

            if (target >= cur)
                cur = target;

            free_mov_list(&mov_list);
            delete pdasm;
            continue;
        }

        if (type == InstructionDisassembler::OTHER)
        {
            add_mov_list(&mov_list, pdasm);
            cur = (void*)((char*)cur + len);
            continue;
        }

        if (type == InstructionDisassembler::RELATIVE_CALL ||
            type == InstructionDisassembler::INDIRECT_CALL)
        {
            free_mov_list(&mov_list);
            delete pdasm;
            cur = (void*)((char*)cur + len);
            continue;
        }

        if (type == InstructionDisassembler::RELATIVE_COND_JUMP)
        {
            void* target = pdasm->get_jump_target_address();
            assert(target);

            if (target >= cur)
                add_branch(&branch_list, target);

            delete pdasm;
            cur = (void*)((char*)cur + len);
            continue;
        }

        assert(0);
    }

    free_mov_list(&mov_list);
    free_addr_list(&addr_list);
    return true;
}
