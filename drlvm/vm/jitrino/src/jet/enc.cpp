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
 * @brief Platform-independent stuff of Encoder class.
 */
#include "enc.h"
#include <stdio.h>
#include "trace.h"

#ifdef PLATFORM_POSIX
#include <signal.h>
#else    

#endif

#include "compiler.h"

namespace Jitrino {
namespace Jet {

bitset<ar_num> Encoder::isCalleeSave;

void Encoder::init(void)
{
    for (unsigned i=0; i<ar_num; i++) {
        AR ar = _ar(i);
        isCalleeSave.set(i, is_callee_save_impl(ar));
    }
}


void Encoder::debug(void)
{
#ifdef PLATFORM_POSIX
    raise(SIGTRAP);
#else
    DebugBreak();
#endif
}

unsigned gen_num_calle_save(void)
{
    static int num = -1;
    if (num == -1) {
        num = 0;
        for (int i=0; i<ar_num; i++) {
            AR ar = (AR)(gr0+i);
            if (is_callee_save(ar)) {
                ++num;
            }
        }
    }
    return num;
}

void Encoder::lea(const Opnd& reg, const Opnd& mem)
{
    assert(reg.is_reg());
    assert(mem.is_mem());
    if (is_trace_on()) {
        trace("lea", to_str(reg), to_str(mem));
    }
    lea_impl(reg, mem);
}


unsigned Encoder::movp(AR op0, unsigned udata, unsigned ubase)
{
    assert(op0 != ar_x);
    if (is_trace_on()) {
        trace("movp.patch", to_str(op0), to_str(udata));
    }
    unsigned pid = reg_patch(true, udata, ubase);
    movp_impl(op0, NULL);
    reg_patch_end(pid);
    return pid;
}

void Encoder::sx1(const Opnd& op0, const Opnd& op1)
{
    assert(op0 != ar_x && op1 != ar_x);
    if (is_trace_on()) {
        trace("sx1", to_str(op0), to_str(op1));
    }
    sx1_impl(op0, op1);
}

void Encoder::sx2(const Opnd& op0, const Opnd& op1)
{
    assert(op0 != ar_x && op1 != ar_x);
    if (is_trace_on()) {
        trace("sx2", to_str(op0), to_str(op1));
    }
    sx2_impl(op0, op1);
}

void Encoder::sx(const Opnd& op0, const Opnd& op1)
{
    assert(op0 != ar_x && op1 != ar_x);
    if (is_trace_on()) {
        trace("sx", to_str(op0), to_str(op1));
    }
    sx_impl(op0, op1);
}


void Encoder::zx2(const Opnd& op0, const Opnd& op1)
{
    assert(op0 != ar_x && op1 != ar_x);
    if (is_trace_on()) {
        trace("zx2", to_str(op0), to_str(op1));
    }
    zx2_impl(op0, op1);
}

void Encoder::fld(jtype jt, AR op0, AR base, int disp, AR index, unsigned scale)
{
    assert(op0 != ar_x);
    assert(is_f(jt));
    if (is_trace_on()) {
        trace("fld", to_str(op0), to_str(base, disp, index, scale));
    }
    fld_impl(jt, op0, base, disp, index, scale);
}

void Encoder::fst(jtype jt, AR op0, AR base, int disp, AR index, unsigned scale)
{
    assert(op0 != ar_x);
    assert(is_f(jt));
    if (is_trace_on()) {
        trace("fst", to_str(base, disp, index, scale), to_str(op0));
    }
    fst_impl(jt, op0, base, disp, index, scale);
}


void Encoder::trap(void)
{
    if (is_trace_on()) {
        trace("trap", "", "");
    }
    trap_impl();
}

void Encoder::trace(const string& func, const string& op0, const string& op1)
{
    if (!m_trace) {
        return;
    }
    dbg("\t; %s%s", m_prefix.c_str(), func.c_str());
    if (!op0.empty()) {
        dbg(" %s", op0.c_str());
        if (!op1.empty()) {
            dbg(", %s", op1.c_str());
        }
    }
    else {
        assert(op1.empty());
    }
    dbg("\n");
}

string Encoder::to_str(AR ar, bool platf)
{
    if (ar == ar_x) {
        return "";
    }
    if (ar == bp) {
        return "bp";
    }
    if (ar == sp) {
        return "sp";
    }
    if (ar == fp0) {
        return "fp0";
    }
    if (platf) {
        return to_str_impl(ar);
    }
    char buf[20];
    snprintf(buf, sizeof(buf)-1, "%s%d", 
        is_f(ar) ?  (is_callee_save(ar) ? "FR" : "fr") : 
                    (is_callee_save(ar) ? "GR" : "gr"), 
        type_idx(ar));
    return string(buf); 
}

string Encoder::to_str(const Opnd& op)
{
    if (op.is_reg()) return to_str(op.reg());
    if (op.is_mem()) return to_str(op.base(), op.disp(), op.index(), op.scale());
    assert(op.is_imm());
    return to_str(op.ival());
}

string Encoder::to_str(jtype jt)
{
    return jt == jvoid ? "" : jtypes[jt].name;
}

string Encoder::to_str(AR base, int disp, AR index, unsigned scale)
{
    if (base == (AR)NOTHING) {
        base = ar_x;
    }
    assert(base == gr_x  || is_gr(base));
    assert(index == gr_x || is_gr(index));
    
    char buf[100] = "[";
    if (base != gr_x) {
        strcat(buf, to_str(base).c_str());
    }
    if (index != gr_x) {
        if (base != gr_x) {
            strcat(buf, "+");
        }
        strcat(buf, to_str(index).c_str());
        strcat(buf, "*");
        char tmp[20];
        snprintf(tmp, sizeof(tmp)-1, "%d", scale);
        strcat(buf, tmp);
    }
    if (disp != 0) {
        char tmp[20];
        if (base == ar_x && index==ar_x) {
            snprintf(tmp, sizeof(tmp)-1, "0x%X", disp);
        }
        else {
            bool _plus = (index != gr_x || base != gr_x) && disp>0;
            if (_plus) {
                strcat(buf, "+");
            }
            if (abs(disp)<256*1024) {
                snprintf(tmp, sizeof(tmp)-1, "%d", disp);
            }
            else {
                if (_plus) {
                    snprintf(tmp, sizeof(tmp)-1, "0x%X", disp);
                }
                else {
                    snprintf(tmp, sizeof(tmp)-1, "+0x%X", disp);
                }
            }
        }
        strcat(buf, tmp);
    }
    strcat(buf, "]");
    return buf;
}

string Encoder::to_str(const void * addr)
{
    char buf[50];
    snprintf(buf, sizeof(buf)-1, "%p", addr);
    return buf;    
}

string Encoder::to_str(int i)
{
    char buf[50];
    snprintf(buf, sizeof(buf)-1, "0x%X/%d", i, i);
    return buf;    
}

string Encoder::to_str(ALU op)
{
    if (op==alu_add) return "add";
    if (op==alu_sub) return "sub";
    if (op==alu_mul) return "mul";
    if (op==alu_div) return "div";
    if (op==alu_rem) return "rem";
    if (op==alu_or) return "or";
    if (op==alu_xor) return "xor";
    if (op==alu_and) return "and";
    if (op==alu_cmp) return "cmp";
    if (op==alu_test) return "test";
    if (op==alu_shl) return "shl";
    if (op==alu_shr) return "shr";
    if (op==alu_sar) return "sar";
    assert(false);
    return "???";
}

string Encoder::to_str(COND cond)
{
    if (cond==cond_none) return "";
    if (cond==ge) return ":ge";
    if (cond==le) return ":le";
    if (cond==gt) return ":gt";
    if (cond==lt) return ":lt";
    if (cond==eq) return ":eq";
    if (cond==ne) return ":ne";
    if (cond==ae) return ":ae";
    if (cond==be) return ":be";
    if (cond==above) return ":above";
    if (cond==below) return ":below";
    assert(false);
    return "???";
}

string Encoder::to_str(HINT hint)
{
    if (hint==hint_none) return "";
    if (hint==taken) return "[hint:taken]";
    if (hint==not_taken) return "[hint:not taken]";
    assert(false);
    return "???";
}

void Encoder::call(bool check_stack, AR gr, const void * target, 
                   const CallSig& cs, unsigned idx, ...)
{
//TODO: Add IA-64 check
#ifdef _EM64T_
    if (check_stack) {
        alu(alu_test, sp, 0x0F);
        unsigned br_off = br(z, 0, 0, hint_none);
        trap();
        patch(br_off, ip(br_off), ip());
    }
#endif
    va_list valist;
    va_start(valist, idx);
    call_va(check_stack, gr, target, cs, idx, valist);
}

void Encoder::call_va(bool check_stack, AR ar, const void *target, 
                      const CallSig& cs, unsigned idx, va_list& valist)
{
    if (is_trace_on()) {
        trace("[+]call:", to_str(ar), to_str(target));
        m_prefix = " |";
    }
    if (idx == 0 && cs.size()) {
        alu(alu_sub, sp, cs.size());
    }
    
    for (unsigned i=idx; i<cs.count(); i++) {
        jtype jt = cs.jt(i);
        assert(!is_f(jt)); // not implemented
        AR gr = cs.reg(i); ;
        if (jt == jobj) {
            void * addr = va_arg(valist, void*);
            if (gr != ar_x) {
                movp(gr, addr);
            }
            else if (is_ia32()) {
                int val = (int)(int_ptr)addr;
                mov(Opnd(i32, sp, cs.off(i)), val);
            }
            else {
#ifdef _EM64T_
                int_ptr val = va_arg(valist, int_ptr);
                mov(Opnd(i64, sp, cs.off(i)), val);
#else
                int val = lo32((jlong)(int_ptr)addr);
                mov(Opnd(i32, sp, cs.off(i)), val);
                val = hi32((jlong)(int_ptr)addr);
                mov(Opnd(i32, sp, cs.off(i)+4), val);
#endif
            }
        }
        else if (jt==i64) {
#ifdef _EM64T_
            int_ptr val = va_arg(valist, int_ptr);
            mov(gr == gr_x ? Opnd(i64, sp, cs.off(i)) : Opnd(i64, gr), val);
#else
            assert(false);
#endif
        }
        else {
            int val = va_arg(valist, int);
            mov(gr == gr_x ? Opnd(i32, sp, cs.off(i)) : Opnd(i32, gr), val);
        }
    }
    movp(ar, target);
    call(Opnd(jobj, ar), cs, check_stack);
    if (is_trace_on()) {
        m_prefix.clear();
        trace("[^]call:", to_str(ar), to_str(target));
    }
}

void Encoder::gen_args(const CallSig& cs, AR grtmp, unsigned idx, 
                       unsigned count, ...)
{
    if (idx == 0 && cs.size() != 0) {
        alu(alu_sub, sp, cs.size());
    }
    
    va_list valist;
    va_start(valist, count);
    
    for (int i=idx; i!=(int)(idx+count); i++) {
        jtype jt = cs.jt(i);
        assert(!is_f(jt)); // not implemented
        AR gr = cs.reg(i); ;
        if (gr == gr_x) {
            assert(grtmp != gr_x); // NYI
            gr = grtmp;
        }
        else {
            //assert(gr_idx(cs.reg(i)) != gr_idx(gr_call));
        }
        if (jt == jobj) {
            void * addr = va_arg(valist, void*);
            movp(gr, addr);
        }
        else if (jt==i64) {
            //jlong jl = va_arg(valist, jlong);
            //Opnd op()
            //movl(gr, jl);
            assert(false); // not expected - there were no usage.
        }
        else {
            int i = va_arg(valist, int);
            mov(gr, i);
        }
        if (cs.reg(i) == gr_x) {
            st(jt, gr, sp, cs.off(i));
        }
    }
}

int Encoder::push(const Opnd& op0)
{
    if (is_trace_on()) {
        trace("push", to_str(op0), "");
    }
    return push_impl(op0);
}

int Encoder::pop(const Opnd& op0)
{
    if (is_trace_on()) {
        trace("pop", to_str(op0), "");
    }
    return pop_impl(op0);
}

int Encoder::push_all(bool includeCalleeSave)
{
    if (is_trace_on()) {
        trace("[+]push_all", "", "");
        m_prefix = " |";
    }
    int size = get_all_regs_size();
    size = (size+15) & (~0xF);
    alu(alu_sub, sp, size);
    for (unsigned i=0; i<gr_num; i++) {
        AR gr = _gr(i);
        if (is_callee_save(gr) && !includeCalleeSave) continue;
        Opnd reg(jobj, gr);
        Opnd mem(jobj, sp, i*STACK_SLOT_SIZE);
        mov(mem, reg);
    }
    for (unsigned i=0; i<fr_num; i++) {
        AR fr = _fr(i);
        if (is_callee_save(fr) && !includeCalleeSave) continue;
        Opnd reg(dbl64, fr);
        Opnd mem(dbl64, sp, gr_num*STACK_SLOT_SIZE+i*8);
        mov(mem, reg);
    }
    if (is_trace_on()) {
        m_prefix.clear();
        trace("[^]push_all", "", "");
    }
    return -size;
}

int Encoder::pop_all(bool includeCalleeSave)
{
    if (is_trace_on()) {
        trace("[+]pop_all", "", "");
        m_prefix = " |";
    }
    int size = get_all_regs_size();
    size = (size+15) & (~0xF);
    for (unsigned i=0; i<gr_num; i++) {
        AR gr = _gr(i);
        if (is_callee_save(gr) && !includeCalleeSave) continue;
        Opnd reg(jobj, gr);
        Opnd mem(jobj, sp, i*STACK_SLOT_SIZE);
        mov(reg, mem);
    }
    for (unsigned i=0; i<fr_num; i++) {
        AR fr = _fr(i);
        if (is_callee_save(fr) && !includeCalleeSave) continue;
        Opnd reg(dbl64, fr);
        Opnd mem(dbl64, sp, gr_num*STACK_SLOT_SIZE+i*8);
        mov(reg, mem);
    }
    alu(alu_add, sp, size);
    if (is_trace_on()) {
        m_prefix.clear();
        trace("[^]pop_all", "", "");
    }
    return size;
}


void Encoder::call(const Opnd& target, const CallSig& ci, bool check_stack)
{
    assert(!target.is_reg() || !is_f(target.reg()));
    if (is_trace_on()) {
        trace("call", to_str(target), "");
    }
        
    unsigned alignment = (ci.cc() &  CCONV_STACK_ALIGN_HALF16) ? CCONV_STACK_ALIGN16
        : ci.cc() & CCONV_STACK_ALIGN_MASK;
    if (check_stack && alignment != 0) {
        alu(alu_sub, sp, (unsigned)STACK_SLOT_SIZE);
        if (ci.cc() & CCONV_STACK_ALIGN_HALF16) {
            alu(alu_sub, sp, (unsigned)STACK_SLOT_SIZE);
        }
        alu(alu_test, sp, (alignment - 1));
        unsigned br_off = br(z, 0, 0, taken);
        trap();
        patch(br_off, ip());
        if (ci.cc() & CCONV_STACK_ALIGN_HALF16) {
            alu(alu_add, sp, (unsigned)STACK_SLOT_SIZE);
        }
        alu(alu_add, sp, (unsigned)STACK_SLOT_SIZE);
    }
    
    call_impl(target);
    if (ci.caller_pops() && ci.size() != 0) {
        alu(alu_add, sp, ci.size());
    }
}

void Encoder::ret(unsigned pop)
{
    if (is_trace_on()) {
        trace("ret", to_str(pop), "");
    }
    ret_impl(pop);
}

unsigned Encoder::reg_patch(bool data, unsigned udata, unsigned ubase)
{
    CodePatchItem cpi;
    cpi.data = data;
    cpi.len = (unsigned)-1;
    cpi.done = false;
    cpi.udata = udata;
    cpi.ubase = ubase;
    unsigned pid = ipoff();
    assert(m_patches.count(pid) == 0);
    m_patches[pid] = cpi;
    return pid;
}

void Encoder::reg_patch_end(unsigned pid)
{
    assert(m_patches.count(pid) != 0);
    CodePatchItem& cpi = m_patches[pid];
    assert(cpi.len == (unsigned)-1);
    cpi.len = m_codeStream.ipoff() - pid; // pid is ipoff
    assert(cpi.len != 0);
}

unsigned Encoder::br(COND cond, unsigned udata, unsigned base, HINT hint)
{
    if (is_trace_on()) {
        trace(to_str(hint) + "br"+to_str(cond), to_str(udata), to_str(base));
    }
    unsigned pid = reg_patch(false, udata, base);
    br_impl(cond, hint);
    reg_patch_end(pid);
    return pid;
}

void Encoder::br(const Opnd& op, COND cond, HINT hint)
{
    if (is_trace_on()) {
        trace(to_str(hint)+"br"+to_str(cond), to_str(op), "");
    }
    br_impl(op, cond, hint);
}

void Encoder::patch(unsigned pid, void * inst_addr, void * target_addr)
{
    assert(m_patches.count(pid) != 0);
    CodePatchItem& cpi = m_patches[pid];
    if (cpi.done) {
        return;
    }
    if (cpi.data) {
        assert(cpi.len>sizeof(void*)); // We're writing address there
        // Address is normally placed at the end of instruction
        char* addr_addr = (char*)inst_addr+cpi.len-sizeof(void*);
        *(void**)addr_addr = target_addr;
    }
    else {
        // only long JMPs currently, short JMPs to be done
        assert(cpi.len>4);
        int offset = (int)((char*)target_addr-((char*)inst_addr+cpi.len));
        // Offset is normally placed at the end of instruction
        char* offset_addr = (char*)inst_addr+cpi.len-4;
        *(int*)offset_addr = offset;
    }
    cpi.done = true;
}


}
}; // ~namespace Jitrino::Jet
