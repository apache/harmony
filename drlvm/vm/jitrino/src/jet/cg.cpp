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
 
#include "compiler.h"
#include "arith_rt.h"
#include "trace.h"
#include "stats.h"

#ifdef WIN32
#include <malloc.h>
#else
#include <stdlib.h>
#endif

#include <stdarg.h>
#include <algorithm>
using std::min;

#include <open/vm.h>
#include <open/vm_ee.h>
#include <open/vm_class_info.h>
#include <open/vm_class_loading.h>
#include <jit_runtime_support.h>
#include <jit_intf.h>
#include <jni_types.h>
#include "port_threadunsafe.h"

/**
 * @file
 * @brief Common CodeGen's routines and datas.
 */
 
namespace Jitrino {
namespace Jet {

const CallSig ci_helper_o(CCONV_HELPERS, jvoid, jobj);
const CallSig ci_helper_v(CCONV_HELPERS, jvoid);
const CallSig ci_helper_oi(CCONV_HELPERS, jobj, jobj, i32);
const CallSig ci_helper_lazy(CCONV_MANAGED, jvoid, jobj, jobj);
const CallSig ci_helper_linkerr(CCONV_HELPERS, jvoid, jobj, i32, i32);

void CodeGen::do_mov(const Val& dst_s, const Val& src_s, bool skipTypeCheck)
{
    assert(!dst_s.is_imm());
    jtype sjt = jtmov(src_s.jt());
    jtype djt = jtmov(dst_s.jt());
    assert(skipTypeCheck || sjt == djt);
    
    if (dst_s.is_mem() && src_s.is_mem()) {
        // Need to allocate a reg
        Opnd src(sjt, src_s.base(), src_s.disp(), src_s.index(), src_s.scale());
        Opnd dst(djt, dst_s.base(), dst_s.disp(), dst_s.index(), dst_s.scale());
        rlock(dst_s);
        rlock(src_s);
        AR ar = valloc(src.jt());
        Opnd reg(src.jt(), ar);
        mov(reg, src);
        mov(dst, reg);
        runlock(dst_s);
        runlock(src_s);
        return;
    }
    
    if (src_s.is_imm() && (is_f(src_s.jt()))) {
        // store to memory [first, then upload to register]
        rlock(dst_s);
        rlock(src_s);
        Opnd dst;
        if (dst_s.is_mem()) {
            dst = dst_s.as_opnd(i32);
        }
        else {
            // 'mov (flt32/dbl64) fr, imm' - store to scratch area first, 
            // then upload to register
            assert(dst_s.is_reg());
            dst = Opnd(i32, m_base, voff(m_stack.scratch()));
        }
        if (src_s.jt() == flt32) {
            float f = src_s.fval();
            Opnd val(*(int*)&f);
            mov(dst, val);
        }
        else {
            double d = src_s.dval();
            Opnd val_lo(*(int*)&d);
            mov(dst, val_lo);
            Opnd dst_hi(i32, dst.base(), dst.disp()+4, dst.index(), 
                        dst.scale());
            Opnd val_hi(*(1+(int*)&d));
            mov(dst_hi, val_hi);
        }
        // Succesfully stored immediate to memory. Now, if dst_s is regiter
        // upload the value to it
        if (dst_s.is_reg()) {
            Opnd scratch = Opnd(sjt, m_base, voff(m_stack.scratch()));
            mov(dst_s.as_opnd(), scratch);
        }
        runlock(dst_s);
        runlock(src_s);
        return;
    }
    mov(dst_s.as_opnd(djt), src_s.as_opnd(sjt));
}

void CodeGen::gen_check_null(unsigned depth)
{
    Val& obj = vstack(depth);
    gen_check_null(obj, true);
}

void CodeGen::gen_check_null(Val& obj, bool hw_ok)
{
    assert(obj.jt() == jobj);
    if (obj.has(VA_NZ)) {
        UNSAFE_REGION_START
        STATS_INC(Stats::npesEliminated,1);
        UNSAFE_REGION_END
        if (is_set(DBG_TRACE_CG)) {
            dbg(";;>check.npe for %s - skipped\n", 
                                            to_str(obj.as_opnd()).c_str());
        }
        return;
    }
    if (obj.is_imm()) {
        STATS_INC(Stats::npesEliminated,1);
        if (obj.pval() == NULL_REF) {
            //gen_args(const CallSig& cs, unsigned idx, const Val * parg0 = NULL, 
            //      const Val * parg1 = NULL, const Val * parg2 = NULL);
            Class_Handle npeClass = vm_lookup_class_with_bootstrap(NULL_POINTER_EXCEPTION);
            gen_throw(npeClass, false);
        }
        return;
    }
    // Try to guess whether we can use hardware NPE @ this point.
    // First, check if the method may use it at all:
    // 
    // No catch handler in the method - any exception exits the method
    // !synchronized => to avoid the following:
    //  synchronized method throws HW NPE
    //  GC starts during handling in VM
    //  rt_enum() detects that the exception cames from HW and reports
    //      nothing => 'this' gets no references and killed by GC
    // after the GC, VM gets that exception comes from inside the synch
    //  method. VM tries to release monitor => asks for 
    //  jit_get_address_of_this, but this address already collected by 
    //  GC as garbage - everyone's dead.
    //
    // Also, we can not use HW checks for compressed references - the 
    // uncompressed null reference in not zero.
    //
    bool useHW = hw_ok && get_bool_arg("hwnpe", true) && !g_refs_squeeze;
    if (meth_is_sync_inst() || meth_num_handlers() != 0) {
        useHW = false;
    }
    // Now, all variables must be as if we were entering catch handler - 
    // the locals on callee-save registers must be on that registers and 
    // other locals must be in the memory
    if (useHW) {
        unsigned vars=m_infoBlock.get_num_locals();
        for (unsigned i=0; i<vars; i++){
            const Val& var = m_jframe->var(i);
            AR ar = vreg(var.jt(), i);
            if (ar == ar_x) continue; // no global allocation - skip it
            if (is_callee_save(ar)) {
                if (!var.is_reg()) {
                    // spilled callee-save globally allocated register.
                    // hmmm... how comes ? anyway, can't use HW check
                    useHW = false;
                    break;
                }
                else {
                    assert(var.reg() == ar);
                }
            }
            else {
                // Scratch register holds a variable - can't use HW check
                if (!var.is_mem()) {
                    useHW = false;
                    break;
                }
            }
        }
    }
    if (useHW) {
        //TODO: 
        // Current check is based on the presumption that the fisrt 64K of 
        // address space raises hardware NPE. However, an object may occupy
        // more than 64K - e.g. for array of longs, the (8192+1) items 
        // reside in more that 64K. Need to check whether  64k+ also raises
        // the NPE, or change the NPE checks for field and array accesses.
        if (is_set(DBG_TRACE_CG)) { 
            dbg(";;>check.npe.HW for %s\n", to_str(obj.as_opnd()).c_str());
        }
    }
    else {    
        if (is_set(DBG_TRACE_CG)) {
            dbg(";;>check.npe for %s\n", to_str(obj.as_opnd()).c_str());
        }
        STATS_INC(Stats::npesPerformed,1);
        rlock(obj);
        Opnd opnd = obj.as_opnd();
        if (g_refs_squeeze) {
            //AR gr_null = rfind(Val(jobj, NULL_REF));
            //if (gr_null == ar_x) {
            //    gr_null = valloc(jobj);
            //    movp(gr_null, NULL_REF);
            //    rset(gr_null, Val(jobj, NULL_REF));
            //}
            AR gr_null = valloc(jobj);
            movp(gr_null, NULL_REF);
            alu(alu_cmp, opnd, gr_null);
        }
        else {
            assert(NULL_REF == 0);
            if (opnd.is_reg()) {
                alu(alu_test, opnd, opnd);
            } else {
                // There is no imm64 for cmp. But it is not a problem as we compare with zero.
                alu(alu_cmp, opnd, Opnd((int)(int_ptr)NULL_REF));
            }
        }
        runlock(obj);
        unsigned br_off = br(ne, 0, 0, taken);
        Class_Handle npeClass = vm_lookup_class_with_bootstrap(NULL_POINTER_EXCEPTION);
        gen_throw(npeClass, true);

        patch(br_off, ip());
    } // if !useHW
    
    // Mark the object 'non-null, guaranteed'
    obj.set(VA_NZ);
    // Propagate 'non-null' attribute to the same items in the frame
    for (unsigned i=1; i<m_jframe->size(); i++) {
        Val& that = m_jframe->dip(i);
        if (that == obj) {
            that.set(VA_NZ);
        }
    }
    for (unsigned i=0; i<m_infoBlock.get_num_locals(); i++) {
        Val& var = m_jframe->var(i);
        if (var == obj) {
            var.set(VA_NZ);
        }
    }
    if (is_set(DBG_TRACE_CG) && !useHW) { dbg(";;>~check.npe\n"); }
}

void CodeGen::gen_check_bounds(unsigned ref_depth, unsigned index_depth)
{
    if (is_set(DBG_TRACE_CG)) {dbg(";;>check.bounds\n");}
    const Opnd arr = vstack(ref_depth, true).as_opnd();
    const Opnd idx = vstack(index_depth, vis_mem(index_depth)).as_opnd();
    Opnd len(i32, arr.reg(), rt_array_length_offset);
    if (idx.is_reg()) {
        alu(alu_cmp, len, Opnd(i32, idx.reg()));
    }
    else {
        alu(alu_cmp, len, Opnd(idx.ival()));
    }
    // Unsigned condition here - aka 'len > (unsigned)index' - this also 
    // covers 'index < 0' - in a single comparation.
    unsigned br_off = br(above, 0, 0, taken);
    //gen_call_vm_restore(true, ci_helper_v, rt_helper_throw_out_of_bounds, 0);
    Class_Handle ioobClass = vm_lookup_class_with_bootstrap(INDEX_OUT_OF_BOUNDS);
    gen_throw(ioobClass, true);
    patch(br_off, ip());
    if (is_set(DBG_TRACE_CG)) {dbg(";;>~check.bounds\n");}
}

void CodeGen::gen_check_div_by_zero(jtype jt, unsigned divizor_depth)
{
    const Val& s = vstack(divizor_depth);
    // 
    if ((s.is_imm() && s.ival() != 0) || s.has(VA_NZ)) {
        // not zero - guaranteed
        return;
    }
    if (is_set(DBG_TRACE_CG)) {dbg(";;>check.div_by_zero\n");}
    // The first Val is immediate and zero
    if (s.is_imm() && s.ival() == 0) {
        // if it's i32, then nothing to do more - throw exception ...
        if (jt == i32) {
            // IS zero. Why do people want to divide on zero explicitly?..
            //gen_call_throw(ci_helper_v, rt_helper_throw_div_by_zero_exc, 0);
            Class_Handle aeClass = vm_lookup_class_with_bootstrap(DIVIDE_BY_ZERO_EXCEPTION);
            gen_throw(aeClass, false);
            if (is_set(DBG_TRACE_CG)) {dbg(";;>~check.div_by_zero\n");}
            return;
        }
        else if (is_big(jt)) {
            // ... otherwise check high part of long constant ...
            const Val& shi = m_jframe->dip(divizor_depth+1);
            if (shi.is_imm() && shi.ival() == 0) {
                // ... yes, it's zero too - throw ...
                // Why do people want to divide on zero explicitly?..
                //gen_call_throw(ci_helper_v, rt_helper_throw_div_by_zero_exc, 0);
                Class_Handle aeClass = vm_lookup_class_with_bootstrap(DIVIDE_BY_ZERO_EXCEPTION);
                gen_throw(aeClass, false);
                if (is_set(DBG_TRACE_CG)) {dbg(";;>~check.div_by_zero\n");}
                return;
            }
            else if (shi.is_imm() && shi.ival() != 0) {
                // ... no, the high part is not zero - may return.
                if (is_set(DBG_TRACE_CG)) {dbg(";;>~check.div_by_zero\n");}
                return;
            }
        }
        // fall through to the next checks
    }
    // Long value on the stack and we are on 64bit platform - check 
    // a single constant at once
    if (s.is_imm() && jt == i64 && !is_big(i64)) {
        if (s.lval() == 0) {
            // IS zero. Why do people want to divide on zero explicitly?..
            //gen_call_throw(ci_helper_v, rt_helper_throw_div_by_zero_exc, 0);
            Class_Handle aeClass = vm_lookup_class_with_bootstrap(DIVIDE_BY_ZERO_EXCEPTION);
            gen_throw(aeClass, false);
        }
        if (is_set(DBG_TRACE_CG)) {dbg(";;>~check.div_by_zero\n");}
        return;
    }
    // at this point:
    assert(!s.is_imm() || (s.is_imm() && s.ival()==0 && jt == i64));
    // .. and 'shi' is not immediate 
    assert(jt != i64 || !vis_imm(divizor_depth+1));
    
    if (s.is_reg()) {
        Opnd reg = s.as_opnd(jtmov(jt));
        alu(alu_test, reg, reg);
    }
    else {
        Opnd mem = s.as_opnd(jtmov(jt));
        alu(alu_cmp, mem, Opnd(0));
    }
    if (jt == i32 || !is_big(jt)) {
        unsigned br_off = br(nz, 0, 0, taken);
        //gen_call_vm_restore(true, ci_helper_v, rt_helper_throw_div_by_zero_exc, 0);
        Class_Handle aeClass = vm_lookup_class_with_bootstrap(DIVIDE_BY_ZERO_EXCEPTION);
        gen_throw(aeClass, true);
        patch(br_off, ip());
        if (is_set(DBG_TRACE_CG)) {dbg(";;>~check.div_by_zero\n");}
        return;
    }
    unsigned br_off = NOTHING;
    const Val& shi = m_jframe->dip(divizor_depth+1);
    if (!s.is_imm()) {
        // jump around the further check --> [1]
        br_off = br(nz, 0, 0, hint_none); 
    }
    
    //
    // NB: the code generated below (till if(is_set)) may be jumped over 
    // so no methods should be invoked that may change location of an item
    // - like vstack(), vswap() etc. If it's necessary to invoke such 
    // methods, this should be done before the br() above.
    //
    
    if (shi.is_reg()) {
        Opnd reg = shi.as_opnd(i32);
        alu(alu_test, reg, reg);
    }
    else {
        Opnd mem = shi.as_opnd(i32);
        alu(alu_cmp, mem, Opnd(0));
    }
    unsigned br_hi = br(nz, 0, 0, taken);
    //gen_call_vm_restore(true, ci_helper_v, rt_helper_throw_div_by_zero_exc, 0);
    Class_Handle aeClass = vm_lookup_class_with_bootstrap(DIVIDE_BY_ZERO_EXCEPTION);
    gen_throw(aeClass, true);
    patch(br_hi, ip());
    if (!s.is_imm()) {
        // [1] --> connect to here
        patch(br_off, ip());
    }
    if (is_set(DBG_TRACE_CG)) {dbg(";;>~check.div_by_zero\n");}
}

void CodeGen::gen_brk(void)
{
    trap();
}

void CodeGen::gen_gc_stack(int depth /*=-1*/, bool trackIt /*=false*/)
{
    if (depth == -1) {
        depth = m_jframe->size();
    }
    // prepare GC info for stack
    // Store the current depth
    if (m_bbstate->stack_depth == (unsigned)depth) {
        if (is_set(DBG_TRACE_CG)) {
            dbg(";;>GC.stack.depth - skipped (%d)\n", depth);
        }
    }
    else {
        Opnd op_depth(i32, m_base, voff(m_stack.info_gc_stack_depth()));
        mov(op_depth, depth);
        if (trackIt) {
            m_bbstate->stack_depth = depth;
        }
    };
    if (depth == 0) {
        return;
    }
    unsigned n_words = words(depth);
    if (n_words != 0) {
        unsigned gc_word = 0;
        unsigned size = min(m_jframe->size(), WORD_SIZE);
        for (unsigned i=0; i<size; i++) {
            const Val& s = m_jframe->at(i);
            if (s.jt() != jobj) continue;
            if (vvar_idx(s) != -1) continue;
            if (s.is_reg() && is_callee_save(s.reg())) continue;
            if (s.survive_calls()) continue;
            gc_word |= 1<<i;
        }
        // check whether we do need to store first word
        if (m_bbstate->stack_mask_valid && 
            gc_word == m_bbstate->stack_mask) {
            // do not need to update the GC mask, it's the same
            if (is_set(DBG_TRACE_CG)) {
                dbg(";;>GC.stack.mask - skipped(0x%X)\n", gc_word);
            }
        }
        else {
            Opnd op_mask(i32, m_base, 0*sizeof(int)+ voff(m_stack.info_gc_stack()));
            mov(op_mask, gc_word);
            if (trackIt) {
                m_bbstate->stack_mask = gc_word;
                m_bbstate->stack_mask_valid = true;
            }
        }
    }
    // store the bit masks
    unsigned size = m_jframe->size();    
    for (unsigned i = 1; i < n_words; i++) {
        unsigned pos = i*WORD_SIZE; // where to start
        unsigned end_pos = min(pos + WORD_SIZE, size);
        unsigned gc_word = 0;
        for ( ; pos < end_pos; pos++) {
            const Val& s = m_jframe->at(pos);
            if (s.jt() != jobj) continue;
            if (vvar_idx(s) != -1) continue;
            if (s.is_reg() && is_callee_save(s.reg())) continue;
            if (s.survive_calls()) continue;
            gc_word |= 1<<pos;
        }
        Opnd op_mask(i32, m_base, i*sizeof(int)+ voff(m_stack.info_gc_stack()));
        mov(op_mask, gc_word);
    }
}

void CodeGen::gen_gc_mark_local(jtype jt, unsigned idx)
{
    jtype jtvar = vtype(idx);
    if (jtvar != jvoid && jtvar != jobj) {
        // the variable is known as never contains an object.
        assert(jt != jobj);
        if (m_infoBlock.get_flags() & DBG_TRACE_CG) {
            dbg(";;> skipping GC mark - the item is known to be non-object\n");
        }
        return;
    }
    bool mark = jt == jobj;
    Val& v = vlocal(jt, idx);
    // If an item was already marked and its type is still the same, then 
    // skip the mark
    if ((v.has(VA_MARKED)) && 
        ((v.type() == jobj && mark) || (v.type() != jobj && !mark))) {
        if (is_set(DBG_TRACE_CG)) {
            dbg(";;>GC mark skipped - object type is known\n");
        }
        return;
    }
    
    // prepare GC info for variable
    unsigned offset;
    unsigned bitno;
    
    AR ar = vreg(jt, idx);
    if (ar != ar_x && is_callee_save(ar)) {
        // mark the callee-save register
        offset = m_stack.info_gc_regs();
        bitno = bit_no(ar_idx(ar));
    }
    else if (vis_arg(idx)) {
        // mark input argument
        unsigned i = vget_arg(idx);
        assert(m_ci.reg(i) == ar_x);
        assert(0 == m_ci.off(i)%STACK_SLOT_SIZE);
        int inVal = m_ci.off(i)/STACK_SLOT_SIZE;
        unsigned word = word_no(inVal);
        offset = word*sizeof(int) + m_stack.info_gc_args();
        bitno = bit_no(inVal);
    }
    else {
        // mark the Val in the stack frame
        unsigned word = word_no(idx);
        offset = word*sizeof(int) + m_stack.info_gc_locals();
        bitno = bit_no(idx);
    }
    unsigned mask = 1<<bitno;
    if (is_wide(jt)) {
        // We must also clear bit for the next Val
        assert(!mark);
        if (bitno < 31) {
            // good. can do in one touch
            mask |= 1 << (bitno+1);
        }
        else {
            // Bad, the next Val crosses the word's boundary
            unsigned offset2 = offset + sizeof(int);
            unsigned mask2 = 1;
            const Opnd opnd2(i32, m_base, voff(offset2));
            alu(alu_and, opnd2, Opnd(~mask2));
        }
    }
    const Opnd opnd(i32, m_base, voff(offset));
    if (mark) {
        alu(alu_or, opnd, Opnd(mask));
    }
    else {
        alu(alu_and, opnd, Opnd(~mask));
    }
    v.set(VA_MARKED);
}

unsigned CodeGen::gen_stack_to_args(bool pop, const CallSig& cs, 
                                    unsigned idx, int cnt)
{
    assert(idx <= cs.count());
    unsigned num = cs.count() - idx;
    
    if (cnt == -1) {
        cnt = num;
    }
    // A special case on IA32 - our frame layout fits best for the managed
    // calling convention, and the args can be prepared in a few 
    // instructions. - later
    // TODO: it's not only for 'MANAGED_IA32' but for 'l2r && callee pops 
    // && !align stack'.
    if (false && pop && idx == 0 && cnt == (int)cs.count() && 
        (cs.cc() == CCONV_MANAGED_IA32)) {
        int fix = 0;
        if (cnt != 0) {
            // find the difference between last used slot and the end of 
            // stack frame
            int s = m_stack.stack_slot(m_jframe->depth2slot(0));
            fix = m_stack.size() + s; // s is < 0
        }
        for (unsigned i=0; i<(unsigned)cnt; i++) {
            jtype jt = m_jframe->top();
            vswap(0);
            if (is_big(jt)) {
                vswap(1);
            }
            vpop();
        }
        if (fix != 0) {
            alu(alu_add, sp, fix);
        }
        return fix;
    }

    if (idx == 0 && cs.size() != 0) {
        alu(alu_sub, sp, cs.size());
    }
    int depth = 0;
    // 1st pass - free all register that are used for args passing
    for (int i=0; !(cs.cc() & CCONV_MEM) && i<cnt; i++) {
        unsigned arg_id = idx+cnt-i-1;
        AR ar = cs.reg(arg_id);
        if (ar == ar_x) continue;
        vpark(ar);
    }

    rlock(cs);

    for (int i=0; i<cnt; i++) {
        unsigned arg_id = idx+cnt-i-1;
        jtype jt = cs.jt(arg_id);
        if (jt<i32) {
            jt = i32;
        }
        const Val& s = m_jframe->dip(depth);
        if (cs.reg(arg_id) != ar_x) {
            Opnd rarg(jt, cs.reg(arg_id));
            do_mov(rarg, s);
        }
        else {
            jtype jtm = jtmov(jt);
            Opnd arg(jtm, sp, cs.off(arg_id));
            do_mov(arg, s);
            if (is_big(jt)) {
                Opnd arg_hi(jtm, sp, cs.off(arg_id)+4);
                const Val& s_hi = m_jframe->dip(depth+1);
                do_mov(arg_hi, s_hi);
            }
        }
        if (pop) {
            vpop();
        }
        else {
            depth += is_wide(jt) ? 2 : 1;
        }
    }
    return 0;
}

void CodeGen::gen_call_throw(const CallSig& cs, void * target, 
                             unsigned idx, ...)
{
    // say 'stack is empty'
    gen_gc_stack(0, false);
    vpark(false);
    
    va_list valist;
    va_start(valist, idx);
    rlock(cs);
    AR gr = valloc(jobj);
    call_va(is_set(DBG_CHECK_STACK), gr, target, cs, idx, valist);
    runlock(cs);
    
#ifdef _DEBUG
    // just to make sure we do not return from there
    gen_brk();
#endif
}

void CodeGen::gen_call_vm(const CallSig& cs, void * target,
                          unsigned idx, ...)
{
    vpark();
    gen_gc_stack(-1, true);
    va_list valist;
    va_start(valist, idx);
    rlock(cs);
    AR gr = valloc(jobj);
    call_va(is_set(DBG_CHECK_STACK), gr, target, cs, idx, valist);
    runlock(cs);
}

void CodeGen::gen_call_novm(const CallSig& cs, void * target,
                            unsigned idx, ...) 
{
    vpark();
    va_list valist;
    va_start(valist, idx);
    rlock(cs);
    AR gr = valloc(jobj);
    call_va(is_set(DBG_CHECK_STACK), gr, target, cs, idx, valist);
    runlock(cs);
}

void CodeGen::gen_call_vm_restore(bool exc, const CallSig& cs, 
                                  void * target, unsigned idx, ...)
{
    BBState saveBB = *m_bbstate;
    // 1. store scratch registers in a secret place
    // 2. park everything
    // 3. call whatever
    // 4. restore scratch regs from the secret place
    // 5. restore the state for callee-save registers
    //-----------------------------------------------
    // 1. 
    bool saveScratch = !exc;
    for (unsigned i=0; i<ar_num; i++) {
        AR ar = _ar(i);
        if (is_callee_save(ar)) continue;
        if (saveScratch && rrefs(ar) != 0) {
            jtype jt = is_f(ar) ? dbl64 : jobj;
            Opnd mem(jt, m_base, voff(m_stack.spill(ar)));
            Opnd reg(jt, ar);
            mov(mem, reg);
        }
        if (rlocks(ar) != 0) {
            runlock(ar, true);
        }
    }
    // 2. 
    vpark();
    gen_gc_stack(-1, true);
    // 3.
    va_list valist;
    va_start(valist, idx);
    rlock(cs);
    AR gr = valloc(jobj);
    call_va(is_set(DBG_CHECK_STACK), gr, target, cs, idx, valist);
    runlock(cs);
    // 4.
    // Restore BBState first, so ref_counts for registers become valid
    *m_bbstate = saveBB;
    // restore the registers state
    for (unsigned i=0; saveScratch && i<ar_num; i++) {
        AR ar = _ar(i);
        if (is_callee_save(ar)) continue;
        if (rrefs(ar) != 0) {
            jtype jt = is_f(ar) ? dbl64 : jobj;
            Opnd mem(jt, m_base, voff(m_stack.spill(ar)));
            Opnd reg(jt, ar);
            mov(reg, mem);
        }
    }
    // 5. 
    // Actually nothing to do here.
    // If we had a local var on register before, then it's still on the reg
    // If we had the var with static assignment which was in memory, before,
    // then the memory was not corrupted.
    // So, just nothing to do with callee-save regs
    //
}

void CodeGen::gen_throw(Class_Handle exnClass, bool restore)
{
#ifdef _EM64T_
    bool lazy = false;
#else
    bool lazy = true;
#endif
    BBState saveBB;

    //TODO: Workaround for x86-64 stack should be aligned to half of 16
#ifdef _EM64T_
    alu(alu_sub, sp, (unsigned)STACK_SLOT_SIZE);
    alu(alu_and, sp, ~((unsigned)STACK_SLOT_SIZE));
    alu(alu_add, sp, (unsigned)STACK_SLOT_SIZE);
#endif

    if (restore){
        saveBB = *m_bbstate;
        for (unsigned i=0; i<ar_num; i++) {
            AR ar = _ar(i);
            if (rlocks(ar) != 0) {
                runlock(ar, true);
            }
        }
    }

    if (lazy) {
        gen_call_throw(ci_helper_lazy, rt_helper_throw_lazy, 0, exnClass, NULL);
    } else {
        static const CallSig ci_new(CCONV_HELPERS, jobj, i32, jobj);
        unsigned size = (unsigned)class_get_object_size(exnClass);
        unsigned stackFix;
        Allocation_Handle ah = class_get_allocation_handle(exnClass);
        gen_call_vm(ci_new, rt_helper_new, 0, size, ah);
        gen_save_ret(ci_new);

        static const CallSig cs_constructor(CCONV_MANAGED, jvoid, jobj);
        static Method_Handle constructorMethDesc = 
            class_lookup_method_recursively(exnClass, DEFAUlT_COSTRUCTOR_NAME, 
                DEFAUlT_COSTRUCTOR_DESCRIPTOR);
        static char* constructorMethAddr = 
            *(char**)method_get_indirect_address(constructorMethDesc);
        stackFix = gen_stack_to_args(false, cs_constructor, 0);
        gen_call_vm(cs_constructor, constructorMethAddr, 1);
        runlock(cs_constructor);
        if (stackFix != 0) {
            alu(alu_sub, sp, stackFix);
        }

        static const CallSig cs_throw(CCONV_HELPERS, jvoid, jobj);
        stackFix = gen_stack_to_args(true, cs_throw, 0);
        gen_call_vm(cs_throw, rt_helper_throw, 1);
        runlock(cs_throw);
        if (stackFix != 0) {
            alu(alu_sub, sp, stackFix);
        }
    }

    // Restore BBState first, so ref_counts for registers become valid
    if (restore){
        *m_bbstate = saveBB;
    }
}

}};             // ~namespace Jitrino::Jet
