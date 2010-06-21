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
 * @author Alexander Astapchuk
 */
/**
 * @file
 * @brief Code generation routines for various branches - GOTO, IF_*, 
 * TABLE- and LOOKUP switches, JSR and RET.
 */
 
#include "compiler.h"


namespace Jitrino {
namespace Jet {

/**
 * @brief Maps conditional branches #JavaByteCodes to appropriate 
 *        #ConditionMnemonic.
 *
 * Accepts only opcode in ranges: [OPCODE_IFEQ; OPCODE_IFLE] and 
 * [OPCODE_IF_ICMPEQ; OPCODE_IF_ICMPLE].
 *
 * @param opcod - a byte code
 * @return appropriate ConditionMnemonic 
 */
static COND to_cond(JavaByteCodes opcod)
{
    if (opcod==OPCODE_GOTO) {
        return cond_none;
    }
    unsigned cod;
    if (OPCODE_IFEQ <= opcod && opcod <= OPCODE_IFLE) {
        cod = opcod - OPCODE_IFEQ;
    }
    else {
        assert(OPCODE_IF_ICMPEQ <= opcod && opcod <= OPCODE_IF_ICMPLE);
        cod = opcod - OPCODE_IF_ICMPEQ;
    }
    
    switch (cod) {
    case 0:     return eq;
    case 1:     return ne;
    case 2:     return lt;
    case 3:     return ge;
    case 4:     return gt;
    case 5:     return le;
    default:
        break;
    }
    assert(false);
    return (COND)NOTHING;
}

/**
 * @brief Reverts a condition so operands in comparation may be swapped.
 *
 * Eg. 'op0 <= op1' may become 'op1 >= op0', which is 'lt' changed to 'gt'.
 *
 * Used to swap operands in IF_ICMPx operations when we have an immediate
 * operand as a first operand.
 */
static COND flip(COND cond)
{
    switch(cond) {
    case eq:
    case ne:    return cond;
    case lt:    return gt;
    case ge:    return le;
    case gt:    return lt;
    case le:    return ge;
    default:
        break;
    }
    assert(false);
    return (COND)NOTHING;
}

void Compiler::gen_goto(unsigned target)
{
    if (target <= m_pc) {
        // Back branch
        gen_prof_be();
        gen_gc_safe_point();
    }
    gen_bb_leave(target);
    br(cond_none, target, m_bbinfo->start);
}

void Compiler::gen_if(JavaByteCodes opcod, unsigned target)
{
    if (target <= m_pc) {
        // have back branch here
        gen_prof_be();
        gen_gc_safe_point();
    }
    jtype jt = i32;
    if (opcod == OPCODE_IFNULL) {
        opcod = OPCODE_IFEQ;
        jt = jobj;
    }
    else if (opcod == OPCODE_IFNONNULL) {
        opcod = OPCODE_IFNE;
        jt = jobj;
    }
    OpndKind kind = m_jframe->dip(0).kind();
    bool forceReg = (kind == opnd_imm) || (jt == jobj && g_refs_squeeze);
    Opnd op1 = vstack(0, forceReg).as_opnd();
    vpop();
    rlock(op1);
    COND cond = to_cond(opcod);
    static const Opnd zero((int)0);
    if (jt == jobj && g_refs_squeeze) {
        AR ar = valloc(jobj);
        movp(ar, NULL_REF);
        alu(alu_cmp, Opnd(jobj, ar), op1);
    }
    else if (opcod == OPCODE_IFEQ || opcod == OPCODE_IFNE) {
        if (op1.is_reg()) {
            alu(alu_test, op1, op1);
        }
        else {
            alu(alu_cmp, op1, zero);
        }
    }
    else {
        alu(alu_cmp, op1, zero);
    }
    runlock(op1);
    gen_bb_leave(target);
    br(cond, target, m_bbinfo->start);
}

void Compiler::gen_if_icmp(JavaByteCodes opcod, unsigned target)
{
    if (target <= m_pc) {
        // have back branch here
        gen_prof_be();
        gen_gc_safe_point();
    }
    if (opcod == OPCODE_IF_ACMPEQ) {
        opcod = OPCODE_IF_ICMPEQ;
    }
    else if (opcod == OPCODE_IF_ACMPNE) {
        opcod = OPCODE_IF_ICMPNE;
    }
    Opnd op2 = vstack(0).as_opnd();
    vpop();
    rlock(op2);
    OpndKind kind = m_jframe->dip(0).kind();
    // 'Bad' combinations are 'm,m' and 'imm,<any, but imm>' - have to force
    // an item into a register
    bool forceReg = (op2.is_mem() && kind == opnd_mem) || 
                    (op2.is_imm() && kind == opnd_imm);
    Opnd op1 = vstack(0, forceReg).as_opnd();
    vpop();
    rlock(op1);
    
    COND cond = to_cond(opcod);
    if ( (op1.is_mem() && op2.is_reg()) || op1.is_imm()) {
        // here we have 'mem, reg' or 'imm, mem-or-reg' - swap them so it
        // become 'reg, mem' (more efficient) or 'mem-or-reg, imm' (existent)
        // operations. change the branch condition appropriately.
        alu(alu_cmp, op2, op1);
        cond = flip(cond);
    }
    else {
        alu(alu_cmp, op1, op2);
    }
    
    runlock(op1);
    runlock(op2);
    gen_bb_leave(target);
    br(cond, target, m_bbinfo->start);
}

void Compiler::gen_switch(const JInst & jinst)
{
    assert(jinst.opcode == OPCODE_LOOKUPSWITCH 
           || jinst.opcode == OPCODE_TABLESWITCH);
    Opnd val = vstack(0, true).as_opnd();
    vpop();
    rlock(val);
    gen_bb_leave(NOTHING);

    if (jinst.opcode == OPCODE_LOOKUPSWITCH) {
        unsigned n = jinst.get_num_targets();
        for (unsigned i = 0; i < n; i++) {
            Opnd key(jinst.key(i));
            unsigned pc = jinst.get_target(i);
            alu(alu_cmp, val, key);
            br(eq, pc, m_bbinfo->start);
        }
        runlock(val);
        br(cond_none, jinst.get_def_target(), m_bbinfo->start);
        return;
    }
    //
    // TABLESWITCH
    //
    alu(alu_cmp, val, jinst.high());
    br(gt, jinst.get_def_target(), m_bbinfo->start);
    
    alu(alu_cmp, val, jinst.low());
    br(lt, jinst.get_def_target(), m_bbinfo->start);
    
    AR gr_tabl = valloc(jobj);
    movp(gr_tabl, DATA_SWITCH_TABLE | m_curr_inst->pc, m_bbinfo->start);
#ifdef _EM64T_
    // On EM64T, we operate with I_32 value in a register, but the 
    // register will be used as 64 bit in address form - have to extend
    sx(Opnd(i64, val.reg()), Opnd(i32, val.reg()));
#endif
    // Here, we need to extract 'index-=low()' - can pack this into 
    // complex address form:
    //      [table + index*sizeof(void*) - low()*sizeof(void*)],
    // but only if low()*sizeof(void*) does fit into displacement ...
    int tmp = -jinst.low();
    const int LO_BOUND = INT_MIN/(int)sizeof(void*);
    const int UP_BOUND = INT_MAX/(int)sizeof(void*);
    if (LO_BOUND<=tmp && tmp<=UP_BOUND) {
        ld(jobj, gr_tabl, gr_tabl, -jinst.low()*sizeof(void*), 
        val.reg(), sizeof(void*));
    }
    else {
        // ... otherwise subtract explicitly, but only if the register
        // is not used anywhere else
        if (rrefs(val.reg()) !=0) {
            Opnd vtmp(i32, valloc(i32));
            mov(vtmp, val); // make a copy of val
            runlock(val);
            val = vtmp;
            rlock(val);
        }
        alu(alu_sub, val, jinst.low());
        ld(jobj, gr_tabl, gr_tabl, 0, val.reg(), sizeof(void*));
    }
    runlock(val);
    br(gr_tabl);
}

void Compiler::gen_jsr(unsigned target)
{
    AR gr = valloc(jobj);
    const JInst& jinst = *m_curr_inst;
    movp(gr, jinst.next, m_bbinfo->start);
    vpush(Val(jretAddr, gr));
    gen_bb_leave(target);
    br(cond_none, target, m_bbinfo->start);
}

void Compiler::gen_ret(unsigned idx)
{
    Opnd ret = vlocal(jretAddr, idx, false).as_opnd();
    gen_bb_leave(NOTHING);
    br(ret);
}


}}; // ~namespace Jitrino::Jet
