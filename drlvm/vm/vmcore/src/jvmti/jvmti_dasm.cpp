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
 * @author Alexander V. Astapchuk
 */
/**
 * @file
 * @brief Disassembler for JVMTI implementation.
 */

// FIXME: Highly platform specific, should be renamed to jvmti_dasm_x86.cpp
//        or go to arch specific directory.
#if (defined _IA32_) || (defined _EM64T_)

#include "jvmti_dasm.h"
#include "dec_base.h"

static InstructionDisassembler::Register convertRegName2Register(RegName reg)
{
    switch(reg) {
#ifdef _IA32_
    case RegName_Null:  return InstructionDisassembler::DISASM_REG_NONE;
    case RegName_EAX:   return InstructionDisassembler::DISASM_REG_EAX;
    case RegName_EBX:   return InstructionDisassembler::DISASM_REG_EBX;
    case RegName_ECX:   return InstructionDisassembler::DISASM_REG_ECX;
    case RegName_EDX:   return InstructionDisassembler::DISASM_REG_EDX;
    case RegName_ESI:   return InstructionDisassembler::DISASM_REG_ESI;
    case RegName_EDI:   return InstructionDisassembler::DISASM_REG_EDI;
    case RegName_EBP:   return InstructionDisassembler::DISASM_REG_EBP;
    case RegName_ESP:   return InstructionDisassembler::DISASM_REG_ESP;
#elif defined(_EM64T_)
    case RegName_Null:  return InstructionDisassembler::DISASM_REG_NONE;
    case RegName_RAX:   return InstructionDisassembler::DISASM_REG_RAX;
    case RegName_RBX:   return InstructionDisassembler::DISASM_REG_RBX;
    case RegName_RCX:   return InstructionDisassembler::DISASM_REG_RCX;
    case RegName_RDX:   return InstructionDisassembler::DISASM_REG_RDX;
    case RegName_RSI:   return InstructionDisassembler::DISASM_REG_RSI;
    case RegName_RDI:   return InstructionDisassembler::DISASM_REG_RDI;
    case RegName_RSP:   return InstructionDisassembler::DISASM_REG_RSP;
    case RegName_RBP:   return InstructionDisassembler::DISASM_REG_RBP;
    case RegName_R8 :   return InstructionDisassembler::DISASM_REG_R8;
    case RegName_R9 :   return InstructionDisassembler::DISASM_REG_R9;
    case RegName_R10:   return InstructionDisassembler::DISASM_REG_R10;
    case RegName_R11:   return InstructionDisassembler::DISASM_REG_R11;
    case RegName_R12:   return InstructionDisassembler::DISASM_REG_R12;
    case RegName_R13:   return InstructionDisassembler::DISASM_REG_R13;
    case RegName_R14:   return InstructionDisassembler::DISASM_REG_R14;
    case RegName_R15:   return InstructionDisassembler::DISASM_REG_R15;
#endif
    default:
        break;
    }
    // Some other registers (e.g. AL or XMM or whatever) - not
    // supported currently
    return InstructionDisassembler::DISASM_REG_NONE;
}

static void convertOperand2Opnd(
    InstructionDisassembler::Opnd* pdst, 
    const EncoderBase::Operand& src)
{
    if (src.is_imm()) {
        pdst->kind = InstructionDisassembler::Kind_Imm;
        pdst->imm = (int)src.imm();
    }
    else if (src.is_mem()) {
        pdst->kind = InstructionDisassembler::Kind_Mem;
        pdst->base = convertRegName2Register(src.base());
        pdst->index = convertRegName2Register(src.index());
        pdst->disp = src.disp();
        pdst->scale = src.scale();
    }
    else {
        assert(src.is_reg());
        pdst->kind = InstructionDisassembler::Kind_Reg;
        pdst->reg = convertRegName2Register(src.reg());
    }
}

#ifdef _IPF_
const char* InstructionDisassembler::get_reg_value(
    Register reg,
    const Registers* pcontext) const
{
    assert(0);
    return NULL;
}

#elif defined _EM64T_

const char* InstructionDisassembler::get_reg_value(
    Register reg,
    const Registers* pcontext) const
{
    switch(reg) {
    case DISASM_REG_NONE: return NULL;
    case DISASM_REG_RAX:  return (const char*)pcontext->rax;
    case DISASM_REG_RBX:  return (const char*)pcontext->rbx;
    case DISASM_REG_RCX:  return (const char*)pcontext->rcx;
    case DISASM_REG_RDX:  return (const char*)pcontext->rdx;
    case DISASM_REG_RSI:  return (const char*)pcontext->rsi;
    case DISASM_REG_RDI:  return (const char*)pcontext->rdi;
    case DISASM_REG_RBP:  return (const char*)pcontext->rbp;
    case DISASM_REG_RSP:  return (const char*)pcontext->rsp;
    case DISASM_REG_R8 :  return (const char*)pcontext->r8 ;
    case DISASM_REG_R9 :  return (const char*)pcontext->r9 ;
    case DISASM_REG_R10:  return (const char*)pcontext->r10;
    case DISASM_REG_R11:  return (const char*)pcontext->r11;
    case DISASM_REG_R12:  return (const char*)pcontext->r12;
    case DISASM_REG_R13:  return (const char*)pcontext->r13;
    case DISASM_REG_R14:  return (const char*)pcontext->r14;
    case DISASM_REG_R15:  return (const char*)pcontext->r15;
    default: assert(false);
    }
    return NULL;
}

#else // _IA32_

const char* InstructionDisassembler::get_reg_value(
    Register reg,
    const Registers* pcontext) const
{
    switch(reg) {
    case DISASM_REG_NONE: return NULL;
    case DISASM_REG_EAX:  return (const char*)pcontext->eax;
    case DISASM_REG_EBX:  return (const char*)pcontext->ebx;
    case DISASM_REG_ECX:  return (const char*)pcontext->ecx;
    case DISASM_REG_EDX:  return (const char*)pcontext->edx;
    case DISASM_REG_ESI:  return (const char*)pcontext->esi;
    case DISASM_REG_EDI:  return (const char*)pcontext->edi;
    case DISASM_REG_EBP:  return (const char*)pcontext->ebp;
    case DISASM_REG_ESP:  return (const char*)pcontext->esp;
    default: assert(false);
    }
    return NULL;
}

#endif // _IA32_

void InstructionDisassembler::disasm(const NativeCodePtr addr, 
                                     InstructionDisassembler * pidi)
{
    assert(pidi != NULL);
    Inst inst;
    pidi->m_len = DecoderBase::decode(addr, &inst);
    if (pidi->m_len == 0) {
        // Something wrong happened
        pidi->m_type = OPCODEERROR;
        return;
    }
    
    pidi->m_type = UNKNOWN;
    pidi->m_argc = inst.argc;
    for (unsigned i=0; i<inst.argc; i++) {
        convertOperand2Opnd(&pidi->m_opnds[i], inst.operands[i]);
    }
    
    if (inst.mn == Mnemonic_CALL) {
        assert(pidi->m_argc == 1);
        if (inst.operands[0].is_imm()) {
            pidi->m_type = RELATIVE_CALL;
            pidi->m_target = (NativeCodePtr)((char*)addr + pidi->m_len + inst.operands[0].imm());
        }
        else {
            pidi->m_type = INDIRECT_CALL;
        }
    }
    else if (inst.mn == Mnemonic_JMP) {
        assert(pidi->m_argc == 1);
        if (inst.operands[0].is_imm()) {
            pidi->m_type = RELATIVE_JUMP;
            pidi->m_target = (NativeCodePtr)((char*)addr + pidi->m_len + inst.operands[0].imm());
        }
        else {
            pidi->m_type = INDIRECT_JUMP;
        }
    }
    else if (inst.mn == Mnemonic_RET) {
        pidi->m_type = RET;
    }
    else if (is_jcc(inst.mn)) {
        // relative Jcc is the only possible variant
        assert(pidi->m_argc == 1);
        assert(inst.odesc->opnds[0].kind == OpndKind_Imm);
        pidi->m_cond_jump_type = (CondJumpType)(inst.mn-Mnemonic_Jcc);
        assert(pidi->m_cond_jump_type < CondJumpType_Count);
        pidi->m_target = (NativeCodePtr)((char*)addr + pidi->m_len + inst.operands[0].imm());
        pidi->m_type = RELATIVE_COND_JUMP;
    }
}

NativeCodePtr 
InstructionDisassembler::get_target_address_from_context(const Registers* pcontext) const
{
    switch(get_type()) {
    case RELATIVE_JUMP:
    case RELATIVE_COND_JUMP:
    case RELATIVE_CALL:
        return m_target;
    case INDIRECT_JUMP:
    case INDIRECT_CALL:
        // Only CALL/JMP mem/reg expected - single argument
        assert(m_argc == 1);
        {
            const Opnd& op = get_opnd(0);
            if (op.kind == Kind_Reg) {
                assert(op.reg != DISASM_REG_NONE);
                return (NativeCodePtr)get_reg_value(op.reg, pcontext);
            }
            else if (op.kind == Kind_Mem) {
                char* base = (char*)get_reg_value(op.base, pcontext);
                char* index = (char*)get_reg_value(op.index, pcontext);
                unsigned scale = op.scale;
                int disp = op.disp;
                char* targetAddrPtr = base + ((long)index)*scale + disp;
                return (NativeCodePtr)*(void**)targetAddrPtr;
            }
        }
        // 0th arg is neither memory, nor register, possibly immediate? 
        // can't happen for INDIRECT_xxx.
        assert(false);
        return NULL;
#ifdef _IA32_
    case RET:
        {
        const char* sp_value = get_reg_value(DISASM_REG_ESP, pcontext);
        const char* retAddr = *(char**)sp_value;
        return (NativeCodePtr)retAddr;
        }
#endif // _IA32_
    default:
        // This method should not be called for non-branch instructions.
        assert(false);
    }
    return NULL;
}

#endif
