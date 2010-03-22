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
 * @brief Arithmetic, conversion and comparison routines.
 */
 
#include "cg.h"
#include "arith_rt.h"

namespace Jitrino {
namespace Jet {

static int log2(int val)
{
    if (val == 0) return -1;
    for (unsigned i=0; i<31; i++) {
        if (val&1) {
            return val == 1 ? (int)i : -1;
        }
        val >>= 1;
    }
    return 31;
}


static ALU to_alu(JavaByteCodes opc)
{
    switch(opc) {
    case OPCODE_IADD:   return alu_add;
    case OPCODE_ISUB:   return alu_sub;
    case OPCODE_IMUL:   return alu_mul;
    case OPCODE_IOR:    return alu_or;
    case OPCODE_IXOR:   return alu_xor;
    case OPCODE_IAND:   return alu_and;
    case OPCODE_IDIV:   return alu_div;
    case OPCODE_IREM:   return alu_rem;
    case OPCODE_ISHL:   return alu_shl;
    case OPCODE_ISHR:   return alu_sar;
    case OPCODE_IUSHR:  return alu_shr;
    default:
        break;
    }
    assert(false);
    return (ALU)0;
}

void CodeGen::gen_iinc(unsigned idx, int value)
{
    Val& var = vlocal(i32, idx, false);
    rlock(var);
    vunref(i32, var);
    alu(alu_add, var.as_opnd(), value);
    runlock(var);
}

bool CodeGen::gen_a_f(JavaByteCodes op, jtype jt)
{
    if (op != OPCODE_IADD && op != OPCODE_ISUB && 
        op != OPCODE_IMUL && op != OPCODE_IDIV) {
        return false;
    }

    bool is_dbl = jt == dbl64;
    unsigned v1_depth = is_dbl ? 2 : 1;
    const Val& v1 = m_jframe->dip(v1_depth);
    const Val& v2 = m_jframe->dip(0);
    if (v1.is_imm() && v2.is_imm()) {
        if (is_dbl) {
            double res =  rt_h_dbl_a(v1.dval(), v2.dval(), op);
            vpop();
            vpop();
            vpush(Val(res));
        }
        else {
            float res =  rt_h_flt_a(v1.fval(), v2.fval(), op);
            vpop();
            vpop();
            vpush(Val(res));
        }
        return true;
    } // if v1.is_imm() && v2.is_imm()

    const Val& val1 = vstack(v1_depth, !(v1.is_reg() && rrefs(v1.reg())));
    rlock(val1);
    if (v2.is_imm()) {
        //TODO - can optimize a bit for is_caddr() case.
        vswap(0);
    }
    assert(v2.is_mem() || v2.is_reg());
    rlock(v2);

    AR rres = valloc(jt);
    Opnd reg(jt, rres);
    mov(reg, val1.as_opnd());
    alu(to_alu(op), reg, v2.as_opnd());

    runlock(v2);
    runlock(val1);
    vpop();
    vpop();
    vpush(reg);
    return true;
}

bool CodeGen::gen_a_i32(JavaByteCodes op)
{
    Val& v2 = m_jframe->dip(0);
    if (op == OPCODE_IMUL && v2.is_imm()) {
        assert(!vis_imm(1)); // must be handled in gen_a_generic()
        if (v2.ival() == 0) {
            // x*0 == 0
            vpop();
            vpop();
            gen_push((int)0);
            return true;
        }
        if (v2.ival() == 1) {
            // x*1 == x - nothing to do
            vpop();
            //vpop(jt); + vpush(jt); == leaving v1 on stack
            return true;
        }
        int l2 = log2(v2.ival());
        if (l2 != -1) {
            // x*<pow_of_2> => replace by shift left
            // Force the item to be on register
            Val& v = vstack(1, !vis_reg(1));
            Val r;
            if (rrefs(v.reg()) != 1) {
                // If there are several references on the item, then make a
                // copy
                rlock(v);
                r = Val(i32, valloc(i32));
                do_mov(r, v);
                runlock(v);
            }
            else {
                r = v;
            }
            assert(r.is_reg() && rrefs(r.reg())<=1);
            alu(alu_shl, r.as_opnd(), Opnd(l2));
            vpop(); vpop();
            vpush(r);
            return true;
        }
    }
    return false;
}

bool CodeGen::gen_a_generic(JavaByteCodes op, jtype jt)
{
    if (op == OPCODE_INEG) {
        return false; // later
    }
    if (jt == i32) {
        bool v2_imm = vis_imm(0);
        if (v2_imm &&
            (op == OPCODE_ISHL || op == OPCODE_ISHL || op == OPCODE_IUSHR)) {
            // accept it
        }
        /*else if (v2_imm && (op == OPCODE_IMUL || op == OPCODE_IDIV)) {
            // accept it
        }
        else if (op == OPCODE_IMUL) {
            // accept it
        }*/
        else if (op == OPCODE_IADD || op == OPCODE_ISUB) {
            // accept it
        }
        else if (op == OPCODE_IOR || op == OPCODE_IAND || op == OPCODE_IXOR) {
            // accept it
        }
        else if (vis_imm(0) && m_jframe->size()>1 && vis_imm(1)) {
            // accept it
        }
        else {
            return false;
        }
    }
    else if (is_f(jt)) {
        if (op != OPCODE_IADD && op != OPCODE_ISUB && 
            op != OPCODE_IMUL && op != OPCODE_IDIV) {
            return false;    
        }
    }
    else {
        return false;
    }
    
    bool is_dbl = jt == dbl64;
    unsigned v1_depth = is_dbl?2:1;
    
    if (vis_imm(v1_depth) && vis_imm(0)) {
        const Val& v1 = m_jframe->dip(v1_depth);
        const Val& v2 = m_jframe->dip(0);
        Val res;
        if (jt==dbl64) {
            double d = rt_h_dbl_a(v1.dval(), v2.dval(), op);
            res = Val(d);
        }
        else if (jt==flt32) {
            float f = rt_h_flt_a(v1.fval(), v2.fval(), op);
            res = Val(f);
        }
        else {
            assert(jt==i32);
            int i = rt_h_i32_a(v1.ival(), v2.ival(), op);
            res = Val(i);
        }
        vpop();
        vpop();
        vpush(res);
        return true;
    } // if v1.is_imm() && v2.is_imm()
    

    const Val& v1 = vstack(v1_depth, true);
    Opnd res = v1.as_opnd();
    if (rrefs(v1.reg()) > 1) {
        rlock(v1);
        AR ar = valloc(jt);
        runlock(v1);
        Opnd reg(jt, ar);
        mov(reg, v1.as_opnd());
        res = reg;
    }
    rlock(res);
    rlock(v1);
    const Val& v2 = m_jframe->dip(0);
/*    if (false )v2.
        
#ifdef _IA32_
        // on IA32 can use address in a displacement
        alu(to_alu(op), v1, ar_x, (int)v2.addr());
#else
        AR addr = valloc(jobj); rlock(addr);
        movp(addr, v2.addr());
        alu_mem(jt, to_alu(op), r1, addr);
        runlock(addr);
#endif
    }
    else */
    if(v2.is_mem()) {
        // Everyone can do 'reg, mem' operation
        alu(to_alu(op), res, v2.as_opnd());
    }
    else if(v2.is_imm() && jt==i32) {
        // 'reg, imm' is only for i32 operations
        alu(to_alu(op), res, v2.ival());
    }
    else {
        Opnd v2 = vstack(0, true).as_opnd();
        alu(to_alu(op), res, v2);
    }
    vpop();
    vpop();
    runlock(v1);
    runlock(res);
    vpush(res);

    return true;
}

void CodeGen::gen_a(JavaByteCodes op, jtype jt)
{
    if (gen_a_platf(op, jt)) {
        return;
    }
    
    if (gen_a_generic(op, jt)) {
        return;
    }
    
    if (is_f(jt) && gen_a_f(op, jt)) {
        return;
    }
    
    if (jt == i32 && gen_a_i32(op)) {
        return;
    }

    unsigned stackFix = 0;
    bool shft = op == OPCODE_ISHL || op == OPCODE_ISHR || op == OPCODE_IUSHR;
    const CallSig* rcs = NULL;
    if (is_f(jt)) {
        assert(jt == dbl64 || jt == flt32);
        char * helper = NULL;
        bool is_dbl = jt == dbl64;
        if (op == OPCODE_INEG) {
            SYNC_FIRST(static const CallSig cs_dbl(CCONV_STDCALL, dbl64, dbl64));
            SYNC_FIRST(static const CallSig cs_flt(CCONV_STDCALL, flt32, flt32));
            rcs = is_dbl? &cs_dbl : &cs_flt;
            stackFix = gen_stack_to_args(true, *rcs, 0, 1);
            helper = is_dbl ? (char*)&rt_h_neg_dbl64 : (char*)&rt_h_neg_flt32;
            gen_call_novm(*rcs, helper, 1);
            runlock(*rcs);
        }
        else {
            //if (m_jframe->dip(1).stype == st_imm && )
            SYNC_FIRST(static const CallSig cs_dbl(CCONV_STDCALL, dbl64, dbl64, dbl64, i32));
            SYNC_FIRST(static const CallSig cs_flt(CCONV_STDCALL, flt32, flt32, flt32, i32));
            rcs = is_dbl? &cs_dbl : &cs_flt;
            stackFix = gen_stack_to_args(true, *rcs, 0, 2);
            helper = is_dbl ? (char*)&rt_h_dbl_a : (char*)&rt_h_flt_a;
            gen_call_novm(*rcs, helper, 2, op);
            runlock(*rcs);
        }
    }
    else if (jt==i64) {
        if (op == OPCODE_INEG) {
            SYNC_FIRST(static const CallSig cs(CCONV_STDCALL, i64, i64));
            rcs = &cs;
            stackFix = gen_stack_to_args(true, *rcs, 0, 1);
            gen_call_novm(*rcs, (void*)&rt_h_neg_i64, 1);
            runlock(*rcs);
        }
        else if (shft) {
            SYNC_FIRST(static const CallSig cs(CCONV_STDCALL, i64, i64, i32, i32));
            rcs = &cs;
            stackFix = gen_stack_to_args(true, *rcs, 0, 2);
            gen_call_novm(*rcs, (void*)&rt_h_i64_shift, 2, op);
            runlock(*rcs);
        }
        else {
            SYNC_FIRST(static const CallSig cs(CCONV_STDCALL, i64, i64, i64, i32));
            rcs = &cs;
            stackFix = gen_stack_to_args(true, *rcs, 0, 2);
            gen_call_novm(*rcs, (void*)&rt_h_i64_a, 2, op);
            runlock(*rcs);
        }
    }
    else {
        assert(jt==i32);
        if (op == OPCODE_INEG) {
            SYNC_FIRST(static const CallSig cs(CCONV_STDCALL, i32, i32));
            rcs = &cs;
            stackFix = gen_stack_to_args(true, *rcs, 0, 1);
            gen_call_novm(*rcs, (void*)&rt_h_neg_i32, 1);
            runlock(*rcs);
        }
        else if (op == OPCODE_IADD || op == OPCODE_ISUB) {
            const Val& op2 = vstack(0);
            vpop();
            rlock(op2);
            const Val& op1 = vstack(0);
            vpop();
            rlock(op1);
            AR ar = valloc(i32);
            Opnd reg(i32, ar);
            //TODO: may eliminate additional register allocation
            mov(reg, op1.as_opnd());
            alu(op == OPCODE_IADD ? alu_add : alu_sub, reg, op2.as_opnd());
            runlock(op1);
            runlock(op2);
            vpush(Val(i32, ar));
            return;
        }
        else {
            SYNC_FIRST(static const CallSig cs(CCONV_STDCALL, i32, i32, i32, i32));
            rcs = &cs;
            stackFix = gen_stack_to_args(true, *rcs, 0, 2);
            gen_call_novm(*rcs, (void*)&rt_h_i32_a, 2, op);
            runlock(*rcs);
        }
    }
    assert(rcs != NULL);
    gen_save_ret(*rcs);
    if (stackFix != 0) {
        alu(alu_sub, sp, stackFix);
    }
}

void CodeGen::gen_cnv(jtype from, jtype to)
{
    if (from<i32 && to==i32) {
        // no op
        return;
    }
    char *helper = (char *) cnv_matrix_impls[from][to];
    const CallSig cs(CCONV_STDCALL, to, from);
    unsigned stackFix = gen_stack_to_args(true, cs, 0);
    gen_call_novm(cs, helper, 1);
    if (stackFix != 0) {
        alu(alu_sub, sp, stackFix);
    }
    runlock(cs);
    gen_save_ret(cs);
}

void CodeGen::gen_x_cmp(JavaByteCodes op, jtype jt)
{
    if (is_f(jt)) {
        char *helper;
        if (jt == dbl64) {
            assert(op == OPCODE_DCMPG || op == OPCODE_DCMPL);
            helper = op == OPCODE_DCMPG ? 
                                 (char*)&rt_h_dcmp_g : (char*)&rt_h_dcmp_l;
        }
        else {
            assert(op == OPCODE_FCMPG || op == OPCODE_FCMPL);
            helper = op == OPCODE_FCMPG ? 
                                 (char*)&rt_h_fcmp_g : (char*)&rt_h_fcmp_l;
        }
        const CallSig cs(CCONV_STDCALL, i32, jt, jt);
        unsigned stackFix = gen_stack_to_args(true, cs, 0);
        gen_call_novm(cs, helper, 2);
        if (stackFix != 0) {
            alu(alu_sub, sp, stackFix);
        }
        runlock(cs);
        gen_save_ret(cs);
        return;
    }
    assert(op == OPCODE_LCMP);
    char *helper = (char *)rt_h_lcmp;
    SYNC_FIRST(static const CallSig cs(CCONV_STDCALL, i32, i64, i64));
    unsigned stackFix = gen_stack_to_args(true, cs, 0);
    gen_call_novm(cs, helper, 2);
    if (stackFix != 0) {
        alu(alu_sub, sp, stackFix);
    }
    runlock(cs);
    gen_save_ret(cs);
}


}}; // ~namespace Jitrino::Jet
