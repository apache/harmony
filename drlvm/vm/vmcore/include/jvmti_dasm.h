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
 * @brief Disassembler for JVMTI interface declaration.
 */
#if !defined(__JVMTI_DASM_H_INCLUDED__)
#define __JVMTI_DASM_H_INCLUDED__

#include "open/types.h"
#include "vm_core_types.h"
#include "jni_types.h"
#include <assert.h>

class InstructionDisassembler {
public:
    /**
     * Type of instruction.
     */
    enum Type {
        OPCODEERROR = 0,
        UNKNOWN,
        OTHER=UNKNOWN,
        RELATIVE_JUMP,
        RELATIVE_COND_JUMP,
        RELATIVE_CALL,
        INDIRECT_JUMP,
        INDIRECT_CALL,
        RET
    };
    /**
     * General-purpose registers set.
     */
    enum Register {
        DISASM_REG_NONE,
#ifdef _IA32_
        DISASM_REG_EAX, DISASM_REG_EBX, DISASM_REG_ECX, DISASM_REG_EDX,
        DISASM_REG_ESI, DISASM_REG_EDI, DISASM_REG_EBP, DISASM_REG_ESP
#elif defined _EM64T_
        DISASM_REG_RAX, DISASM_REG_RBX, DISASM_REG_RCX, DISASM_REG_RDX,
        DISASM_REG_RSI, DISASM_REG_RDI, DISASM_REG_RSP, DISASM_REG_RBP,
        DISASM_REG_R8 , DISASM_REG_R9 , DISASM_REG_R10, DISASM_REG_R11,
        DISASM_REG_R12, DISASM_REG_R13, DISASM_REG_R14, DISASM_REG_R15
#endif
    };
    /**
     * Kind of operand 
     * @see Opnd
     */
    enum Kind {
        /// Operand represents a constant
        Kind_Imm,
        /// Operand is memory reference
        Kind_Mem,
        /// Operand is register
        Kind_Reg
    };
    /**
     * Describes an argument of instruction.
     */
    struct Opnd {
        Opnd()
            {
                kind = Kind_Imm;
                base = index = reg = DISASM_REG_NONE;
                scale = 0;
                disp = 0;
            }
        Kind kind;
        Register    base;
        Register    index;
        unsigned    scale;
        union {
            int         disp;
            Register    reg;
            int         imm;
        };
    };
    /**
     * @brief Enum of possible condtions for conditional jump-s.
     *
     * @note To simplify decoding, the current order of the enum constants 
     *       exactly matches the order of ConditionalMnemonic constants
     *       in #EncoderBase. Reordering of this constants requires some 
     *       changes in InstructionDisassembler::disasm.
     */
    enum CondJumpType
    {
        JUMP_OVERFLOW=0,
        JUMP_NOT_OVERFLOW=1,
        JUMP_BELOW=2, JUMP_NOT_ABOVE_OR_EQUAL=JUMP_BELOW, JUMP_CARRY=JUMP_BELOW,
        JUMP_NOT_BELOW=3, JUMP_ABOVE_OR_EQUAL=JUMP_NOT_BELOW, JUMP_NOT_CARRY=JUMP_NOT_BELOW,
        JUMP_ZERO=4, JUMP_EQUAL=JUMP_ZERO,
        JUMP_NOT_ZERO=5, JUMP_NOT_EQUAL=JUMP_NOT_ZERO,
        JUMP_BELOW_OR_EQUAL=6, JUMP_NOT_ABOVE=JUMP_BELOW_OR_EQUAL,
        JUMP_NOT_BELOW_OR_EQUAL=7, JUMP_ABOVE=JUMP_NOT_BELOW_OR_EQUAL,

        JUMP_SIGN=8,
        JUMP_NOT_SIGN=9,
        JUMP_PARITY=10, JUMP_PARITY_EVEN=JUMP_PARITY,
        JUMP_NOT_PARITY=11, JUMP_PARITY_ODD=JUMP_NOT_PARITY,
        JUMP_LESS=12, JUMP_NOT_GREATER_OR_EQUAL=JUMP_LESS,
        JUMP_NOT_LESS=13, JUMP_GREATER_OR_EQUAL=JUMP_NOT_LESS,
        JUMP_LESS_OR_EQUAL=14, JUMP_NOT_GREATER=JUMP_LESS_OR_EQUAL,
        JUMP_NOT_LESS_OR_EQUAL=15, JUMP_GREATER=JUMP_NOT_LESS_OR_EQUAL,

        CondJumpType_Count = 16
    };

    InstructionDisassembler(void) :
        m_type(OPCODEERROR), m_target(0), m_len(0),
        m_cond_jump_type(JUMP_OVERFLOW), m_argc(0)
    {
    }
    
    InstructionDisassembler(NativeCodePtr address) :
        m_type(OPCODEERROR), m_target(0), m_len(0),
        m_cond_jump_type(JUMP_OVERFLOW), m_argc(0)
    {
        disasm(address, this);
    }

    InstructionDisassembler(InstructionDisassembler &d)
    {
        m_type = d.m_type;
        m_target = d.m_target;
        m_len = d.m_len;
        m_cond_jump_type = d.m_cond_jump_type;
        m_argc = d.m_argc;
        m_opnds[0] = d.m_opnds[0];
        m_opnds[1] = d.m_opnds[1];
        m_opnds[2] = d.m_opnds[2];
    }

    /**
     * @brief Returns type of underlying instruction.
     */
    Type get_type(void) const
    {
        return m_type;
    }
    /**
     * @brief Returns length (in bytes) of underlying instruction.
     * 
     * The size includes instruction's prefixes, if any.
     */
    jint get_length_with_prefix(void) const
    {
        assert(get_type() != OPCODEERROR);
        return m_len;
    }
    /**
     * @brief Returns absolute address of target, if applicable.
     *
     * For instructions other than relative JMP, CALL and conditional jumps, 
     * the value is undefined.
     */
    NativeCodePtr get_jump_target_address(void) const
    {
        assert(get_type() == RELATIVE_JUMP || 
               get_type() == RELATIVE_COND_JUMP ||
               get_type() == RELATIVE_CALL);
        return m_target;
    }
    /**
     * @brief Returns type of conditional jump.
     * 
     * @note For instructions other than conditional jump, the value is
     *       undefined.
     */
    CondJumpType get_cond_jump_type(void) const
    {
        assert(get_type() == RELATIVE_COND_JUMP);
        return m_cond_jump_type;
    }
    
    /**
     * Returns number of operands of the instruction.
     */
    unsigned get_operands_count(void) const
    {
        return m_argc;
    }
    
    /**
     * Returns \c i-th operand.
     */
    const Opnd& get_opnd(unsigned i) const
    {
        assert(i<get_operands_count());
        return m_opnds[i];
    }
    /**
     * Calculates and returns address of target basing on the decoded 
     * arguments and provided register context.
     * 
     * Works for both indirect and direct branches.
     *
     * @note Only valid for branch instructions like JMPs, CALLs, etc.
     */
    NativeCodePtr get_target_address_from_context(const Registers* pregs) const;

    /**
     * Returns the appropriate register value for the register operand reg
     */
    const char* get_reg_value(Register reg, const Registers* pcontext) const;
private:
    /**
     * @brief Performs disassembling, fills out InstructionDisassembler's 
     *        fields.
     *
     * If it's impossible (for any reason) to decode an instruction, then 
     * type is set to OPCODEERROR and other fields' values are undefined.
     */
    static void disasm(const NativeCodePtr addr, 
                       InstructionDisassembler * pidi);
    /**
     * @brief Type of instruction.
     */
    Type    m_type;
    /**
     * @brief Absolute address of target, if applicable.
     */
    NativeCodePtr   m_target;
    /**
     * @brief Length of the instruction, in bytes.
     */
    unsigned    m_len;
    /**
     * @brief Type of conditional jump.
     */
    CondJumpType m_cond_jump_type;
    /**
     * @brief Number of arguments of the instruction.
     */
    unsigned    m_argc;
    /**
     * @brief Arguments of the instruction.
     */
    Opnd        m_opnds[3];
};


#endif  // __JVMTI_DASM_H_INCLUDED__
