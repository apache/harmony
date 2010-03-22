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
#include "open/vm_method_access.h"
#include "compiler.h"
#include "trace.h"

/**
 * @file
 * @brief Registers manipulation routines and operand stack mimics.
 */
 
namespace Jitrino {
namespace Jet {


AR CodeGen::valloc(jtype jt)
{
    unsigned start = is_f(jt) ? ar_idx(fr0) : ar_idx(gr0);
    unsigned count = is_f(jt) ? fr_num : gr_num;
    unsigned highest_index = start+count-1;
    //
    // We are going to allocate a temporary register - we do several tries 
    // to reach the following goals:
    // - minimize memory accesses
    // - minimize callee-saved registers usages
    
    // Try to find the first SCRATCH (non callee-save) register
    
    AR last_used = rlast(jt);
    for (unsigned i=ar_idx(last_used)+1; i<highest_index; i++) {
        AR ar = _ar(i);
        if (is_callee_save(ar) || ar == sp || ar == m_base) continue;
        if (rrefs(ar) == 0 && rlocks(ar) == 0 && !m_global_rusage.test(i)) {
            // good, found available scratch register
            rlast(ar);
            return ar;
        }
    }
    // No free scratch registers above the 'last used'. Try any scratch reg.
    for (unsigned i=start; i<highest_index; i++) {
        AR ar = _ar(i);
        if (is_callee_save(ar) || ar == sp || ar == m_base) continue;
        if (rrefs(ar) == 0 && rlocks(ar) == 0 && !m_global_rusage.test(i)) {
            // good, found available scratch register
            rlast(ar);
            return ar;
        }
    }
#if 0
    //
    // No free scratch registers. How about free callee-save ?
    //
    // not now. currently, only scratch regs are used as temporaries,
    // and callee-save are used as global regs. may revisit it.
    for (unsigned i=start; i<highest_index; i++) {
        AR ar = _ar(i);
        if (ar == sp || ar == m_base) continue;
        if (m_bbstate->regs[i].refs == 0 && m_bbstate->regs[i].locks==0 && 
            !m_static_rs.test(i)) {
            last_used = ar;
            rrefs(ar);
            return ar;
        }
    }
#endif
    //
    // Ugh, no free registers of the needed kind available, need to spill 
    // someone out
    
    // try to locate first non locked scratch register with min number of 
    // refs
    unsigned min_ref = NOTHING, min_idx = NOTHING;
    for (unsigned i=start; i<highest_index; i++) {
        AR ar = _ar(i);
        if (is_callee_save(ar) || ar == sp || ar == m_base) continue;
        if (min_ref > rrefs(ar) && rlocks(ar)==0 && !m_global_rusage.test(i)) {
            min_ref = rrefs(ar);
            min_idx = i;
        }
    }
    
    // this means that all scratch registers are locked. Cant happen.
    assert(min_idx != NOTHING);
    
    AR ar = _ar(min_idx);
     
#if 0 // TODO: checkit
    // How about scratch registers of other kind ?
    // Storing to a register allows us avoid memory access. This might seem 
    // questionable, as on IA32 'MOVD xmm, r32' has the latency of incredible
    // 10 cycles.
    // However, the throughput is just 1 cycle, so the port is free again 
    // very soon. Also, as we're allocating, say, GP register, then we'll
    // operate on it in next few instructions, and will not use FPU during 
    // this, so the latency will [hopefully] be masked. 
    //
    
    //Note: kinda prototype, not even tested.
    start = !is_f(jt) ? ar_idx(fr0) : ar_idx(gr0);
    count = !is_f(jt) ? fr_num : gr_num;
    AR otherKind = ar_x;
    for (unsigned i=start; i<highest_index; i++) {
        AR ar = _ar(i);
        if (is_callee_save(ar) || ar == sp || ar == m_base) continue;
        if (m_bbstate->regs[i].refs == 0 && !m_bbstate->regs[i].locked) {
            otherKind = ar;
            break;
        }
    }
    if (otherKind != ar_x) {
        // Cool - do have a scratch register of other kind, let's unload there
        mov(otherKind, ar);
        m_bbstate->regs[min_idx].temp = otherKind;
    }
#endif // if 0

    // Ugh... No way out, have to spill to the memory...

    if (is_set(DBG_TRACE_CG)) { dbg(";;>spill %s\n", to_str(ar).c_str()); }
     
    // First, free out the stack items, which are the register
    for (unsigned i=0; i<m_jframe->size(); i++) {
        Val& s = m_jframe->dip(i);
        if (!s.is_reg() || s.reg() != ar) { continue; };
        jtype mov_jt = jtmov(s.jt());
        st(mov_jt, ar, m_base, vstack_off(i));
        rfree(s);
        s.to_mem(m_base, vstack_off(i));
        rref(s);
    }
    // Next, free out the locals, which are the register
    for (unsigned i=0; i<m_jframe->num_vars(); i++) {
        Val& s = m_jframe->var(i);
        if (!s.is_reg() || s.reg() != ar) { continue; };
        jtype mov_jt = jtmov(s.jt());
        st(mov_jt, ar, m_base, vlocal_off(i));
        rfree(s);
        s.to_mem(m_base, vlocal_off(i));
        rref(s);
    }
    //
    // Now, free up the stack items that are the memory
    // addressed via register (e.g. an instance field value)
    //
    for (unsigned i=0; is_gr(ar) && i<m_jframe->size(); i++) {
        Val& s = m_jframe->dip(i);
        if (!s.is_mem() || !s.uses(ar)) { continue; };
        
        //WARN: both slots for i64 type have register assigned, but for dbl64 - only first slot is marked..
        bool need_double_slot = is_ia32() && s.jt()==dbl64; 
        
        push(s.as_opnd(iplatf));
        if (need_double_slot) {
            Opnd hi_mem(iplatf, s.base(), s.disp() + STACK_SLOT_SIZE, s.index(), s.scale());
            push(hi_mem);
        }
        rfree(s);
        int stack_off = vstack_off(i);
        s.to_mem(m_base, stack_off);
        rref(s);
        if (need_double_slot) {
            Opnd hi_stk(iplatf, m_base, stack_off + STACK_SLOT_SIZE);
            pop(hi_stk);
        }
        Opnd stk(iplatf, m_base, stack_off);
        pop(stk);
    }
    if (is_set(DBG_TRACE_CG)) { dbg(";;>~spill\n"); }

    return ar;
}

Val& CodeGen::vstack(unsigned depth, bool toReg)
{
    Val& s = m_jframe->dip(depth);
    
    if (s.is_reg()) {
        return s;
    }
    
    jtype jtm = jtmov(m_jframe->top(depth));
    
    if (s.is_mem()) {
        if (toReg) {
            // TODO: if the item represents the local variable then it may
            // worth to upload the local first, then return the register 
            // reference
            AR ar = valloc(jtm);
            // 's' must still reside in memory
            assert(s.is_mem());
            // but it's location may change. 
            // Eg if we had arraylength ([gr0+offset]) on the stack,
            // after allocation and possible spilling may have
            // [m_base+stack_offset].
            Opnd reg(jtm, ar);
            do_mov(reg, s.as_opnd(jtm));
            rfree(s);
            s.to_reg(ar);
            rref(s);
        }
        return s;
    }
    
    assert(s.is_imm());
    
    if (is_f(s.jt()) && s.caddr() != NULL) {
        AR fr = valloc(s.jt());
        Opnd reg(s.jt(), fr);
        Opnd mem = vaddr(s.jt(), s.caddr());
        do_mov(reg, mem);
        rfree(s);
        s.to_reg(fr);
        rref(s);
        return s;
    }

    if (is_f(s.jt())) {
        if (toReg) {
            // swap to memory first
            vswap(depth);
            // allocate register and move to it from memory
            AR ar = valloc(jtm);
            Val reg(jtm, ar);
            do_mov(reg, s);
            rfree(s);
            s.to_reg(ar);
            rref(s);
        }
        return s;
    }        
                                                                                                                                                

    //if (s.jt() == i32 || (s.jt() == i64 && is_big(i64))) {
    if (s.jt() == i32 || s.jt() == i64) {
        if (toReg) {
            AR ar = valloc(jtm);
            Val reg(jtm, ar);
            do_mov(reg, s);
            rfree(s);
            s.to_reg(ar);
            rref(s);
        }
        return s;
    }
    
    //if (s.jt() == i64) {
    //    assert(!is_big(i64));
    //    return s;
    //}
    
    if (s.jt() == jobj) {
#ifdef _EM64T_
        //TODO: it may be not always necessary to upload to register.
        toReg = true;
#endif    
        if (toReg) {
            AR ar = valloc(s.jt());
            movp(ar, s.pval());
            rfree(s);
            s.to_reg(ar);
            rref(s);
        }
        return s;
    }
    //assert(false);
    return s;
}


AR CodeGen::vreg(jtype jt, unsigned idx)
{
    return m_ra[idx];
}

jtype CodeGen::vtype(unsigned idx)
{
    return m_staticTypes[idx];
}

void CodeGen::vassign(jtype jt, unsigned idx, const Val& s)
{
    Val& v = m_jframe->var(idx);
    assert(!v.is_reg() && s.is_reg());
    rfree(v);
    v = s.as_opnd(jt);
    v.attrs(s.attrs());
    rref(v);
}

bool CodeGen::vis_stack(const Val& s) const
{
    if (!s.is_mem()) return false;
    if (s.base() != m_base) return false;
    // Presumption about the operand stack layout in the native frame
    assert(m_stack.stack_bot() >= m_stack.unused());
    if (m_stack.stack_bot()<s.disp()) return false;
    if (s.disp()<m_stack.unused()) return false;
    return true;
}

int CodeGen::vvar_idx(const Val& s) const
{
    if (!s.is_mem()) return -1;
    if (s.base() != ar_x && s.base() != m_base) return -1;
    for (unsigned i=0; i<m_stack.get_num_locals(); i++) {
        int off = vlocal_off(i);
        if (s.disp() == off) return i;
    }
    return -1;
}

void CodeGen::vunref(jtype jt)
{
    // TODO(?): may optimize a bit. Eg. when we do PUTFIELD, there is no 
    // need to un-shadow say array items on operand stack and vise versa.
    // this will require adding a trait to Val - field, static, array items
    bool do_print = true;
    for (unsigned i=0; i<m_jframe->size(); i++) {
        Val& s = m_jframe->dip(i);
        if (s.jt() != jt) continue;
        // The stack item itself - dont care
        if (vis_stack(s)) continue;
        // Not a memory ref or a register/immediate - don't care
        if (!s.is_mem() || s.is_dummy()) continue;
        // Refers to a local var - will be unreferenced explicitly 
        // when needed (via vunref(Val&) call)
        if (vvar_idx(s) != -1) continue;
        if (do_print && is_set(DBG_TRACE_CG)) {
            dbg(";;>vunref(%s)\n", jtypes[jt].name);
            do_print = false;
        }
        Val save = s;
        Val op = vstack(i, true);
        for (unsigned j=i+1; j<m_jframe->size(); j++) {
            Val& s1 = m_jframe->dip(j);
            if (s1 == save) {
                rfree(s1);
                s1.to_reg(op.reg());
                rref(s1);
            }
        }
    }
    if (!do_print && is_set(DBG_TRACE_CG)) {
        dbg(";;>~vunref(%s)\n", jtypes[jt].name);
    }
}

void CodeGen::vunref(jtype jt, const Val& op, unsigned skipDepth)
{
    if (op.is_imm()) {
        // Nothing to do with immediates
        return;
    }
    jtype jtm = jtmov(jt);
    bool printDone = false;
    for (unsigned i=0; i<m_jframe->size(); i++) {
        if (i == skipDepth) {
            continue;
        }
        Val& s = m_jframe->dip(i);
        if (s != op && !(op.is_reg() && s.uses(op.reg()))) {
            continue;
        }
        if (is_set(DBG_TRACE_CG) && !printDone) {
            dbg(";;>vunref for %s\n", to_str(op.as_opnd()).c_str());
            printDone = true;
        }
        Opnd stk(jtm, m_base, vstack_off(i));
        do_mov(stk, s);
        rfree(s);
        s.to_mem(m_base, vstack_off(i));
        rref(s);
    }
    if (is_set(DBG_TRACE_CG) && printDone) {
        dbg(";;>~vunref for %s\n", to_str(op.as_opnd()).c_str());
    }
}

void CodeGen::vvar_def(jtype jt, unsigned idx)
{
    Val& v = m_jframe->var(idx);
    if (v.jt() != jt) {
        rfree(v);
        v = Val(jt, m_base, m_stack.local(idx));
        rref(v);
    }
    if (is_wide(jt)) {
        Val& v1 = m_jframe->var(idx+1);
        if (v1.jt() != jt) {
            rfree(v1);
            v1 = Val(jt, m_base, m_stack.local(idx+1));
            rref(v1);
        }
    }
}


Val& CodeGen::vlocal(jtype jt, unsigned idx, bool forDef)
{
    Val& v = m_jframe->var(idx);
    if (v.is_dummy()) {
        // Not initialized - set up default values
        v.to_mem(m_base, vlocal_off(idx));
        rref(v);
    }
    // jretAddr - special case for ASTORE_ in JSR block.
    assert(v.jt() == jvoid || v.jt() == jt || 
          (v.jt() == jretAddr && jt == jobj) ||
          (v.jt() == jobj && jt == jretAddr));
    // The following combinations are possible:
    // has global assignment ? | where's currently ? | what to do ?
    //  yes | reg | if (forDef) {unref} else {noop}
    //  yes | mem | if (forDef) {no op} else {load into reg}
    //
    //  no  | reg | if (forDef) {unref} else {noop}
    //  no  | mem |
    
    if (v.jt() == jvoid) {
        rfree(v);
        assert(v.is_mem());
        v = Val(jt, v.base(), v.disp());
        rref(v);
    }
    
    if (forDef) {
        vunref(jt, v);
    }
    
    AR ar = vreg(jt, idx);
    
    if (ar != ar_x && v.is_reg()) {
        // Must be exactly the same as statically allocated
        assert(v.reg() == ar);
    }
    else if (ar != ar_x) {
        assert(v.is_mem());
        // unref() - actually, no need to unref() - 
        // as the var has global assignment, we just could not
        // load onto the operant stack the memory reference 
        for (unsigned i=0; i<m_jframe->size(); i++) {
            assert(vvar_idx(m_jframe->dip(i)) != (int)idx);
        }
        rfree(v);
        v.to_reg(ar);
        rref(v);
        if (!forDef) {
            ld(jtmov(jt), ar, m_base, vlocal_off(idx));
        }
    }
    return v;
}

void CodeGen::vpop(void)
{
    Val& s = m_jframe->dip(0);
    rfree(s);
    if (is_wide(s.jt())) {
        Val& s1 = m_jframe->dip(1);
        if (!s1.is_dummy()) rfree(s1);
    }
    m_jframe->pop(m_jframe->top());
}



void CodeGen::vpush(jtype jt)
{
    m_jframe->push(jt);
    Val& s = m_jframe->dip(0);
    s.to_mem(m_base, vstack_off(0));
    rref(s);
}

void CodeGen::vpush(const Val& s)
{
    assert(s.jt() != jvoid);
    // use vpush2() for big types
    assert(!is_big(s.jt())); 
    m_jframe->push(s.jt());
    m_jframe->dip(0) = s;
    rref(s);
}

void CodeGen::vpush2(const Val& op_lo, const Val& op_hi)
{
    assert(op_lo.jt() != jvoid && is_big(op_lo.jt()));
    assert(op_hi.jt() != jvoid && is_big(op_hi.jt()));
    assert(op_hi.jt() == op_lo.jt());
    // not a big problem, just a self-check - this is expected usage scheme
    //assert(op_hi.kind() == op_lo.kind());
    
    m_jframe->push(op_lo.jt());
    m_jframe->dip(1) = op_hi;
    m_jframe->dip(0) = op_lo;
    rref(op_hi);
    rref(op_lo);
}

void CodeGen::vpop2(Val* pop_lo, Val* pop_hi)
{
    assert(is_big(m_jframe->top()));
    *pop_hi  = vstack(1);
    *pop_lo = vstack(0);
    rfree(*pop_hi);
    rfree(*pop_lo);
    m_jframe->pop2();
}

bool CodeGen::vis_arg(unsigned local_idx) const
{
    if (local_idx >= m_argSlots)return false;
    int argid = m_argids[local_idx];
    if(argid == -1)             return false;
    if (m_ci.reg(argid) != ar_x) return false;
    return true;
}

unsigned CodeGen::vget_arg(unsigned local_idx) const
{
    assert(vis_arg(local_idx));
    int argid = m_argids[local_idx];
    return argid;
}


int CodeGen::vlocal_off(unsigned idx) const
{
    if (!vis_arg(idx)) {
        return voff(m_stack.local(idx));
    }
    int argid = m_argids[idx];
    assert(argid != -1);
    return voff(m_ci.off(argid) + STACK_SLOT_SIZE);//SSS <=retAddr
}

int CodeGen::vstack_off(unsigned depth) const
{
    unsigned slot = m_jframe->depth2slot(depth);
    int off = m_stack.stack_slot(slot);
    return voff(off);
}

int CodeGen::voff(int off) const
{
    if (m_base != sp) {
        return off;
    }
    return off + m_stack.size() + m_depth;
}

void CodeGen::vswap(unsigned depth)
{
    Val& v = m_jframe->dip(depth);
    if (v.is_dummy() || vis_stack(v)) {
        return;
    }
    Val mem(v.jt(), m_base, vstack_off(depth));
    rlock(v);
    do_mov(mem, v);
    runlock(v);
    
    rfree(v);
    v.to_mem(m_base, vstack_off(depth));
    rref(v);
    assert(vis_stack(v));
}

void Compiler::gen_bb_leave(unsigned to)
{
    if (is_set(DBG_TRACE_CG)) { dbg(";;>bb_leave to %d\n", to); }
    bool targetIsMultiRef = false;
    bool to_eh;
    if (to == NOTHING || m_pc == 0 || g_jvmtiMode) {
        // leaving JSR block - act as if were leaving to multiref block
        // Also, special processing for 0th BB - see also gen_bb_enter() 
        // and gen_prolog().
        // The same for JVMTI mode - do not allow a thing to be transferred
        // on a temporary register between basic blocks
        targetIsMultiRef = true;
        to_eh = false;
    }
    else {
        // Must be BB
        assert(m_bbs.find(to) != m_bbs.end());
        const BBInfo& bbto = m_bbs[to];
        // Jumps to ehandler ?
        to_eh = bbto.ehandler;
         // Now, check where the control flow may be transferred from the 
         // current instruction. If *any* of the targets is multi-ref, then
         // perform a full sync.
         const JInst& jinst = *m_curr_inst;
         for (unsigned i=0; i<jinst.get_num_targets(); i++) {
             unsigned targetPC = jinst.get_target(i);
             targetIsMultiRef = targetIsMultiRef || (m_insts[targetPC].ref_count > 1);
         }
         if (jinst.is_switch()) {
             unsigned targetPC = jinst.get_def_target();
             targetIsMultiRef = targetIsMultiRef || (m_insts[targetPC].ref_count > 1);
         }
         if (!jinst.is_set(OPF_DEAD_END)) {
             unsigned targetPC  = jinst.next;
             targetIsMultiRef = targetIsMultiRef || (m_insts[targetPC].ref_count > 1);
         }
    }

    if (!targetIsMultiRef && !to_eh) {
        // nothing to do anymore
        if (is_set(DBG_TRACE_CG)) { dbg(";;>~bb_leave\n"); }
        return;
    }
    
    for (unsigned i=0; !to_eh && i<m_jframe->size(); i++) {
        Val& v = m_jframe->dip(i);
        // already in the memory - nothing to do
        if (vis_stack(v)) continue;
        vswap(i);
        //assert(vis_stack(v));
        //if (is_wide(v.jt()) && !is_big(v.jt())) {
        //    ++i;
        //}
    }
    
    unsigned vars = m_infoBlock.get_num_locals();
    // park all locals
    for (unsigned i=0; i<vars; i++) {
        Val& v = m_jframe->var(i);
        AR ar = m_ra[i];
        // 
        if (v.is_mem() && ar == ar_x) { continue; }
        if (ar == ar_x) {
            assert(v.is_reg());
            // need to spill out
            assert(v.jt() != jvoid);
            jtype jtm = jtmov(v.jt());
            st(jtm, v.reg(), m_base, vlocal_off(i));
            rfree(v);
            v = Val(v.jt(), m_base, vlocal_off(i));
            rref(v);
            continue;
        }
        // must have global assignment
        jtype jt = m_staticTypes[i];
        assert(jt != jvoid);
        jtype jtm = jtmov(jt);
        
        if (v.is_mem()) {
            if (!is_callee_save(ar) && to_eh) {
                // entering catch handler - must leave the var on memory
                continue;
            }
            ld(jtm, ar, m_base, vlocal_off(i));
            rfree(v);
            v = Val(jt, ar);
            rref(v);
            continue;
        }
        assert(v.is_reg() && v.reg() == ar);
        if (is_callee_save(ar) || !to_eh) {
            continue;
        }
        // entering catch handler - must swap the var to memory
        st(jtm, ar, m_base, vlocal_off(i));
        rfree(v);
        v = Val(jt, m_base, vlocal_off(i));
        rref(v);
    }

    if (to_eh) {
        // we're generating a 'goto/fall through/if_' to a basic block
        // if the BB is also the catch handler, then it can't have a 
        // ref_count less than 2.
        assert(m_insts[to].ref_count > 1);
        // Must have only Exception on the top of stack
        assert(m_jframe->size() == 1 && m_jframe->top(0) == jobj);
        // if top of the stack is currently not on the gr_ret, then 
        // force it to be there
        Val& ev = m_jframe->dip(0);
        if (!ev.is_reg() || ev.reg() != gr0) {
            // locals were just spilled and no other stack items left
            // therefore gr0 must be unused, simply load the Exception
            assert(rrefs(gr0) == 0);
            Opnd reg(ev.jt(), gr0);
            do_mov(reg, ev.as_opnd());
            rfree(ev);
            ev.to_reg(gr0);
            rref(ev);
        }
    }

    if (is_set(DBG_TRACE_CG)) { dbg(";;>~bb_leave\n"); }
}

void Compiler::gen_bb_enter(void)
{
    assert(m_bbs.find(m_pc) != m_bbs.end());
    const BBInfo& bbinfo = m_bbs[m_pc];
    if (is_set(DBG_TRACE_CG)) { dbg(";;>bb_enter to %d\n", m_pc); }
    if (bbinfo.ehandler) {
        assert(m_jframe->size() == 1);
        Val& s = m_jframe->dip(0);
        if (!s.is_reg()) {
            rref(gr0);
            s = Val(jobj, gr0);
            // We're entering exception handler - that do not have 'direct'
            // (non exception) ways in it - the object on the top of the 
            // stack is exception and is guaranteed to be non-null.
            if (m_insts[m_pc].ref_count == 1) {
                s.set(VA_NZ);
            }
        }
        else {
            assert(s.reg() == gr0);
        }
    }
    // We always process 0th BB as multiref BB - see also gen_prolog() 
    // when it calls gen_bb_leave(NOTHING).
    if (m_insts[m_pc].ref_count == 1 && m_pc != 0 && !bbinfo.ehandler) {
        if (is_set(DBG_TRACE_CG)) { dbg(";;>~bb_enter\n"); }
        return;
    }

    rclear();    
    
    unsigned vars = m_infoBlock.get_num_locals();
    for (unsigned i=0; i<vars; i++) {
        AR ar = m_ra[i];
        jtype jtglobal = m_staticTypes[i];
        Val& v = m_jframe->var(i);
        // If we enter a BB - then every reg-assigned local is on its reg, 
        // until we're entering exception handler...
        if (ar != ar_x && (is_callee_save(ar) || !bbinfo.ehandler)) {
            assert(jtglobal != jvoid);
            // Overwrite attributes as well
            v = Val(jtglobal, ar);
            rref(v);
            continue;
        }
        // ... in this case, scratch-regs assigned variables are in memory
        assert(ar == ar_x || bbinfo.ehandler);
        if (jtglobal != jvoid) {
            v = Val(jtglobal, m_base, vlocal_off(i));
        }
        else {
            v = Val(jvoid, m_base, vlocal_off(i));
        }
        assert(v.is_mem());
        rref(v);
    }
    for (unsigned i=0; i<m_jframe->size(); i++) {
        Val& s = m_jframe->dip(i);
        if (i==0 && bbinfo.ehandler) {
            assert(s.is_reg() && s.reg() == gr0); 
        }
        else {
            s = Val(s.jt(), m_base, vstack_off(i));
        }
        rref(s);
    }
    // For non-static methods, that also do not write into the 0th slot,
    // we can guarantee that 0th slot (thiz) is always non-null.
    if (!(meth_is_static()) && m_defs[0] == 0) {
        Val& v = m_jframe->var(0);
        v.set(VA_NZ);
    }
    if (is_set(DBG_TRACE_CG)) { dbg(";;>~bb_enter\n"); }
}

void CodeGen::vpark(bool doStack)
{
    bool do_print = true;
    for (unsigned i=0; i<m_infoBlock.get_num_locals(); i++) {
        Val& v = m_jframe->var(i);
        if (!v.is_reg() || is_callee_save(v.reg())) continue;
        if (is_set(DBG_TRACE_CG) && do_print) {
            dbg(";; >vpark(all)\n");
            do_print = false;
        }
        vpark(v.reg(), doStack);
    }
    for (unsigned i=0; doStack && i<m_jframe->size(); i++) {
        Val& s = m_jframe->dip(i);
        unsigned saveI = i;
        // refers to local variable - will be no changes as result of call
        if (vvar_idx(s) != -1) continue;
        // the stack item itself, will not change during the call
        if (vis_stack(s)) continue;
        // Something constant or a register
        if (s.survive_calls()) continue;
        if (s.is_reg() && is_callee_save(s.reg())) continue;
        if (is_set(DBG_TRACE_CG) && do_print) {
            dbg(";; >vpark(all)\n");
            do_print = false;
        }
        vswap(saveI);
    }
    if (is_set(DBG_TRACE_CG) && !do_print) {
        dbg(";; >~vpark(all)\n");
    }
}

void CodeGen::vpark(AR ar, bool doStack)
{
    bool do_print = true;
    rlock(ar);
    for (unsigned i=0; i<m_infoBlock.get_num_locals(); i++) {
        Val& v = m_jframe->var(i);
        if (!v.is_reg() || v.reg() != ar) continue;
        if (do_print && is_set(DBG_TRACE_CG)) {
            dbg(";;>vpark(%s)\n", to_str(ar).c_str());
            do_print = false;
        }
        
        jtype jtm = jtmov(v.jt());
        if (true || (m_defs[i] != 0)) {
            // XXX actually, we need an attribute, whether the variable
            // was changed. 
            // m_defs[i] != 0 works bad for example if float point
            // parameter came on an register
            st(jtm, v.reg(), m_base, vlocal_off(i));
        }
        rfree(v);
        v.to_mem(m_base, vlocal_off(i));
        rref(v);
    }
    for (unsigned k=0; doStack && k<m_jframe->size(); k++) {
        Val& s = m_jframe->dip(k);
        if (!s.uses(ar)) continue;
        jtype jtm = jtmov(s.jt());
        if (do_print && is_set(DBG_TRACE_CG)) {
            dbg(";;>vpark(%s)\n", to_str(ar).c_str());
            do_print = false;
        }
        rfree(s);
        Opnd stk(jtm, m_base, vstack_off(k));
        if (s.is_reg()) {
            mov(stk, Opnd(jtm, ar));
        }
        else {
            do_mov(stk, s);
        }
        s.to_mem(m_base, vstack_off(k));
        rref(s);
    }
    runlock(ar);
    if (!do_print && is_set(DBG_TRACE_CG)) {
        dbg(";;>~vpark(%s)\n", to_str(ar).c_str());
    }
}

void CodeGen::vcheck(void)
{
    unsigned regs[ar_num];
    for (unsigned i=0; i<ar_num; i++) {
        regs[i] = 0;
    }
    const AR noar = (AR)NOTHING;
    for (unsigned i=0; i<m_jframe->size(); i++) {
        const Val& s = m_jframe->dip(i);
        if (s.is_reg()) {
            unsigned idx = ar_idx(s.reg());
            regs[idx]++;
        }
        else if (s.is_mem()) {
            if (s.base() != ar_x && s.base() != noar) { regs[ar_idx(s.base())]++; };
            assert(s.index() != noar);
            if (s.index() != ar_x) { regs[ar_idx(s.index())]++; };
        }
    }
    for (unsigned i=0; i<m_jframe->num_vars(); i++) {
        Val& s = m_jframe->var(i);
        if (s.is_reg()) {
            unsigned idx = ar_idx(s.reg());
            regs[idx]++;
        }
        else if (s.is_mem()) {
            if (s.base() != ar_x && s.base() != noar) { regs[ar_idx(s.base())]++; };
            assert(s.index() != noar);
            if (s.index() != ar_x) { regs[ar_idx(s.index())]++; };
        }
    }
    for (unsigned i=0; i<ar_num; i++) {
        AR ar = _ar(i);
        // 
        if (rlocks(ar) != 0) {
            dbg_dump_state("Register lock cant cross instruction boundaries", 
                           m_bbstate);
            assert(false);
        }
        if (rrefs(ar) != regs[i]) {
            dbg("ERROR: leaked/lost register: %s. refs=%u, must be=%u",
                Encoder::to_str(ar).c_str(), rrefs(ar), regs[i]);
            dbg_dump_state("Problematic frame:", m_bbstate);
            assert(false);
        }
    }
}

}};     // ~namespace Jitrino::Jet
