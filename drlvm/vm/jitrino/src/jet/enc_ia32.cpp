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
 * @brief Implementation of Encoder class for IA-32 and Intel 64 platforms.
 */
#include "enc.h"
#include "enc_ia32.h"
#include "trace.h"

#ifdef _IA32_
#include "enc_prvt.h"
#endif

namespace Jitrino {
namespace Jet {

const Opnd eax(i32, virt(RegName_EAX));
const Opnd ecx(i32, virt(RegName_ECX));
const Opnd edx(i32, virt(RegName_EDX));

static OpndSize to_size(jtype jt)
{
    switch (jt) {
    case i8:    return OpndSize_8;
    case i16:
    case u16:   return OpndSize_16;
    case i32:   return OpndSize_32;
    case i64:   return OpndSize_64;
    case flt32: return OpndSize_32;
    case dbl64: return OpndSize_64;
    default:
        assert(jt == jretAddr);
        // fall through to jobj
    case jobj:
        assert(sizeof(void*)==4 || sizeof(void*)==8);
        return sizeof(void*)==4 ? OpndSize_32 : OpndSize_64;
    }
}

static const RegName reg_map[] = {
    RegName_EAX, RegName_EBX, RegName_ECX, RegName_EDX, 
    RegName_ESI, RegName_EDI, 
#ifdef _EM64T_
    RegName_R8D, RegName_R9D, RegName_R10D, RegName_R11D, 
    RegName_R12D, RegName_R13D, RegName_R14D, RegName_R15D, 
#endif //_EM64T_
    RegName_EBP,  RegName_ESP
};

RegName devirt(AR ar, jtype jt)
{
    RegName reg = RegName_Null;
    if (ar != gr_x) { // && ar!=ar_total) {
        //return RegName_Null;
        assert(COUNTOF(reg_map) == gr_total);
        unsigned idx = type_idx(ar);
        if (is_f(ar)) {
            reg = getRegName(OpndKind_XMMReg, 
                            jt == jvoid ? OpndSize_64 : to_size(jt), idx);
        }
        else if (ar == fp0) {
            reg = RegName_FP0;
        } else {
            assert(idx<COUNTOF(reg_map));
            reg = reg_map[idx];
        
#if !defined(_EM64T_) && defined(_DEBUG)
            // On IA32 can not encode the lower 8 bits of the following regs
            if (equals(reg, RegName_EBP) || equals(reg, RegName_ESI) ||
                equals(reg, RegName_EDI) || equals(reg, RegName_ESP)) {
                assert(jt != i8);
            }
#endif
            OpndSize sz;
            if (jt == jvoid) {
#ifdef _EM64T_
                sz = OpndSize_64;
#else
                sz = OpndSize_32;
#endif
            }
            else {
                sz = to_size(jt);
            }
            reg = getAliasReg(reg, sz);
        }   // if !is_f
    }
    return reg;
}

static void check_args(const Opnd& op0, const Opnd& op1)
{
    assert(!op0.is_mem() || !op1.is_mem());
    assert(!op0.is_imm());
}

static void add_arg(EncoderBase::Operands& args, const Opnd& op, 
                    bool can_shrink = false)
{
    OpndSize sz = to_size(op.jt());
    if (op.is_reg()) {
        RegName reg = devirt(op.reg(), op.jt());
        //::OpndKind kind = is_f(op.reg()) ? OpndKind_XMMReg : OpndKind_GPReg;
        //args.add(EncoderBase::Operand(sz, kind, reg));
        args.add(EncoderBase::Operand(reg));
    }
    else if (op.is_mem()) {
        RegName base = op.base() != ar_x ? devirt(op.base()) : RegName_Null;
        RegName idx = op.index() == ar_x ? RegName_Null : devirt(op.index());
        EncoderBase::Operand mem(sz, base, idx, op.scale(), op.disp());
        args.add(mem);
    }
    else {
        assert(!is_f(op.jt()));
        if (op.jt() == i64) {
            EncoderBase::Operand imm(sz, op.lval());
            args.add(imm);
        }
        else if (op.jt() == jobj) {
            EncoderBase::Operand imm(sz, (int_ptr)(void*)op.lval());
            args.add(imm);
        }
        else {
            assert(op.jt()<=i32);
            if (can_shrink && fits_i8(op.ival())) {
                sz = OpndSize_8;
            }
            EncoderBase::Operand imm(sz, op.ival());
            args.add(imm);
        }
    }
}

static void add_args(EncoderBase::Operands& args, const Opnd& op)
{
    add_arg(args, op);
}

static void add_args(EncoderBase::Operands& args, const Opnd& op0, 
                     const Opnd& op1)
{
    add_arg(args, op0);
    add_arg(args, op1);
}

static void add_args_same_size(EncoderBase::Operands& args, 
                               const Opnd& op0, const Opnd& op1)
{
    OpndSize sz = to_size(op0.jt());
    assert(to_size(op1.jt()) == sz);
    if (op0.is_reg()) {
        RegName reg = devirt(op0.reg(), op0.jt());
        args.add(EncoderBase::Operand(reg));
    }
    else if (op0.is_mem()) {
        RegName base = op0.base() != ar_x ? devirt(op0.base()) : RegName_Null;
        RegName idx = op0.index() == ar_x ? RegName_Null : devirt(op0.index());
        EncoderBase::Operand mem(sz, base, idx, op0.scale(), op0.disp());
        args.add(mem);
    }
    else {
        assert(false);
    }
    
    if (op1.is_reg()) {
        RegName reg = devirt(op1.reg(), op1.jt());
        args.add(EncoderBase::Operand(reg));
    }
    else if (op1.is_mem()) {
        RegName base = op1.base() != ar_x ? devirt(op1.base()) : RegName_Null;
        RegName idx = op1.index() == ar_x ? RegName_Null : devirt(op1.index());
        EncoderBase::Operand mem(sz, base, idx, op1.scale(), op1.disp());
        args.add(mem);
    }
    else {
        assert(!is_f(op1.jt()));
        EncoderBase::Operand imm(sz, (int_ptr)(void*)op1.lval());
        args.add(imm);
    }
}

AR get_cconv_fr(unsigned i, unsigned pos_in_args) {
#ifdef _EM64T_
#ifdef _WIN64
    bool compact = false;
#else
    bool compact = true;
#endif
    static const AR frs[] = {
        virt(RegName_XMM0), virt(RegName_XMM1), 
        virt(RegName_XMM2), virt(RegName_XMM3), 
#ifndef _WIN64
        virt(RegName_XMM4), virt(RegName_XMM5), 
        virt(RegName_XMM6), virt(RegName_XMM7), 
#endif
    };
    const unsigned  count = COUNTOF(frs);
    unsigned pos = compact ? i : pos_in_args;
    return (pos<count) ? frs[pos] : fr_x;
#else
    assert(false);
    return gr_x;
#endif
}

AR get_cconv_gr(unsigned i, unsigned pos_in_args) {
#ifdef _EM64T_
#ifdef _WIN64
    bool compact = false;
	static const AR grs[] = {
		virt(RegName_RCX),  virt(RegName_RDX), 
		virt(RegName_R8),  virt(RegName_R9), 
	};
#else
    bool compact = true;
    static const AR grs[] = {
        virt(RegName_RDI),  virt(RegName_RSI), 
        virt(RegName_RDX),  virt(RegName_RCX), 
        virt(RegName_R8),   virt(RegName_R9), 
    };
#endif
    const unsigned count = COUNTOF(grs);
    unsigned pos = compact ? i : pos_in_args;
    return (pos<count) ? grs[pos] : gr_x;
#else
    assert(false);
    return gr_x;
#endif
}


AR virt(RegName reg)
{
    if (getRegKind(reg) == OpndKind_XMMReg) {
        return _ar(dbl64, getRegIndex(reg));
    }
    assert(getRegKind(reg) == OpndKind_GPReg);
    for(unsigned i=0; i<COUNTOF(reg_map); i++) {
        if (equals(reg, reg_map[i])) {
            return _ar(jobj, i);
        }
    }
    assert(false);
    return gr_x;
}


ConditionMnemonic devirt(COND cond)
{
    switch (cond) {
        case ge: return ConditionMnemonic_GE;
        case le: return ConditionMnemonic_LE;
        case gt: return ConditionMnemonic_G;
        case lt: return ConditionMnemonic_L;

        case eq: return ConditionMnemonic_Z;
        case ne: return ConditionMnemonic_NZ;

        case be   : return ConditionMnemonic_BE;
        case ae   : return ConditionMnemonic_AE;
        case above: return ConditionMnemonic_A;
        case below: return ConditionMnemonic_B;

        default: break;
    }
    assert(false);
    return ConditionMnemonic_Count;
}

InstPrefix devirt(HINT hint) 
{
    if (hint==taken) return InstPrefix_HintTaken;
    if (hint==not_taken) return InstPrefix_HintNotTaken;
    assert(false);
    return (InstPrefix)0xCC;
}

static bool is_callee_save(RegName reg)
{
    if(reg == RegName_Null) {
        return false;
    }
    if (getRegKind(reg) != OpndKind_GPReg) {
        return false;
    }
    assert(getRegKind(reg)==OpndKind_GPReg);
    if (equals(reg, RegName_EBX))   return true;
    if (equals(reg, RegName_EBP))   return true;
#ifdef _EM64T_
#ifdef _WIN64
    if (equals(reg, RegName_RDI))   return true;
    if (equals(reg, RegName_RSI))   return true;
#endif
    if (equals(reg, RegName_R12))   return true;
    if (equals(reg, RegName_R13))   return true;
    if (equals(reg, RegName_R14))   return true;
    if (equals(reg, RegName_R15))   return true;
#else
    if (equals(reg, RegName_ESI))   return true;
    if (equals(reg, RegName_EDI))   return true;
#endif
    return false;
}

static Mnemonic to_mn(jtype jt, ALU alu)
{
    assert(alu_add==0);
    assert(alu_sub==1);
    assert(alu_mul==2);
    assert(alu_div==3);
    assert(alu_rem==4);
    assert(alu_or==5);
    assert(alu_xor==6);
    assert(alu_and==7);
    assert(alu_cmp==8);
    assert(alu_test==9);
    assert(alu_shl==10);
    assert(alu_shr==11);
    assert(alu_sar==12);
    static const Mnemonic i_map[] = {
        Mnemonic_ADD, Mnemonic_SUB, 
        Mnemonic_IMUL, Mnemonic_IDIV, Mnemonic_IDIV,
        Mnemonic_OR, Mnemonic_XOR, Mnemonic_AND, Mnemonic_CMP, Mnemonic_TEST,
        Mnemonic_SHL, Mnemonic_SHR, Mnemonic_SAR,
    };
    static const Mnemonic s_map[] = {
        Mnemonic_ADDSS, Mnemonic_SUBSS, 
        Mnemonic_MULSS, Mnemonic_DIVSS, Mnemonic_Null,
        Mnemonic_Null, Mnemonic_PXOR, Mnemonic_Null, Mnemonic_COMISS, Mnemonic_Null,
        Mnemonic_Null, Mnemonic_Null, Mnemonic_Null
    };
    static const Mnemonic d_map[] = {
        Mnemonic_ADDSD, Mnemonic_SUBSD, 
        Mnemonic_MULSD, Mnemonic_DIVSD, Mnemonic_Null,
        Mnemonic_Null, Mnemonic_PXOR, Mnemonic_Null, Mnemonic_COMISD, Mnemonic_Null,
        Mnemonic_Null, Mnemonic_Null, Mnemonic_Null
    };
    // all items covered ?
    assert(COUNTOF(i_map) == alu_count);
    assert(COUNTOF(i_map) == COUNTOF(s_map));
    assert(COUNTOF(i_map) == COUNTOF(d_map));
    Mnemonic mn = (jt==dbl64 ? d_map : (jt==flt32 ? s_map : i_map))[alu];
    assert(mn != Mnemonic_Null);
    return mn;
}

OpndSize to_sz(jtype jt) 
{
    if (jt == dbl64 || jt == i64) return OpndSize_64;
    if (jt == i32 || jt == flt32) return OpndSize_32;
    if (jt == i16 || jt == u16) return OpndSize_16;
    if (jt == i8) return OpndSize_8;
#ifdef _IA32_
    if (jt == jobj) return OpndSize_32;
#else
    if (jt == jobj) return OpndSize_64;
#endif
    assert(false);
    return OpndSize_Null;
}

static void emu_fix_opnds(Encoder * enc, 
                          Opnd& op0, Opnd& op1, 
                          const Opnd& _op0, const Opnd& _op1)
{
#ifdef _EM64T_
    // no op.
    op0 = _op0;
    op1 = _op1;
    if (true) return;
#endif
    op0 = _op0;
    op1 = _op1;
    if (!op0.is_reg() && !op1.is_reg()) {
        return;
    }
    if (!(op0.is_reg() && op0.jt() == i8) && 
        !(op1.is_reg() && op1.jt() == i8)) {
        return;
    }
    Opnd& reg_op = op0.is_reg() ? op0 : op1;
    const Opnd& mem_op = op0.is_reg() ? op1 : op0;
    
    RegName reg = devirt(reg_op.reg());
    // On IA32 can not encode the lower 8 bits of the following regs
    if (!equals(reg, RegName_EBP) &&  !equals(reg, RegName_ESI) &&
        !equals(reg, RegName_EDI)) {
        assert(!equals(reg, RegName_ESP));
        return;
    }
    // Try to find a register which is neither a op0 itself, nor it's used
    // in the op1.
    RegName r1, r2;
    if (mem_op.is_reg()) {
        r1 = devirt(mem_op.reg());
        r2 = RegName_Null;
    }
    else {
        assert(mem_op.is_mem());
        r1 = devirt(mem_op.base());
        r2 = devirt(mem_op.index());
    }
    RegName emu = RegName_EAX;
    if (equals(reg, emu) || equals(r1, emu) || equals(r2, emu)) {
        emu = RegName_EDX;
        if (equals(reg, emu) || equals(r1, emu) || equals(r2, emu)) {
            emu = RegName_ECX;
            if (equals(reg, emu) || equals(r1, emu) || equals(r2, emu)) {
                emu = RegName_EDX;
            }
        }
    }
    EncoderBase::Operands xargs;
    xargs.add(emu);
    xargs.add(reg);
    enc->ip(EncoderBase::encode(enc->ip(), Mnemonic_XCHG, xargs));
    reg_op = Opnd(reg_op.jt(), virt(emu));
}

static void emu_unfix_opnds(Encoder * enc, 
                          const Opnd& op0, const Opnd& op1, 
                          const Opnd& _op0, const Opnd& _op1)
{
#ifdef _EM64T_
    // no op.
    if (true) return;
#endif
    if (op0 == _op0 && op1 == _op1) {
        return;
    }
    const Opnd& emu_op = op0.is_reg() ? op0 : op1;
    const Opnd& true_op = op0.is_reg() ? _op0 : _op1;
    
    assert(emu_op.is_reg() && true_op.is_reg());
    
    RegName emu = devirt(emu_op.reg());
    RegName reg = devirt(true_op.reg());
    
    EncoderBase::Operands xargs;
    xargs.add(emu);
    xargs.add(reg);
    enc->ip(EncoderBase::encode(enc->ip(), Mnemonic_XCHG, xargs));
}

bool Encoder::is_callee_save_impl(AR gr)
{
    return Jitrino::Jet::is_callee_save(devirt(gr));
}

string Encoder::to_str_impl(AR ar)
{
    RegName reg = devirt(ar);
    return getRegNameString(reg);
}

#if defined(_IA32_)
static int get_reg_idx(AR ar)
{
    assert(is_gr(ar) && ar != ar_x);
    return getRegIndex(reg_map[gr_idx(ar)]);
}


/**
 * Generates (encodes) memory operand.
 *
 * Kind of copy of EncoderBase::encodeModRM.
 */
static char* do_mem(char* buf, const Opnd& op) 
{
    assert(op.is_mem());
    
    ModRM& modrm = *(ModRM*)buf;
    ++buf;
    SIB& sib = *(SIB*)buf;
    AR base = op.base();
    bool haveSib = false;
    haveSib = haveSib || (sp == base);
    haveSib = haveSib || (op.index() != ar_x);
    
    if (haveSib) {
        ++buf;
    }
    if ((op.disp() == 0 || base == ar_x) && base != bp) {
        modrm.mod = 0;
        modrm.rm = haveSib ? 4 : (base==ar_x ? 5 : get_reg_idx(base));
        if (base == ar_x) {
            *(int*)buf = op.disp();
            buf += 4;
        }
    }
    else if (fits_i8(op.disp())) {
        modrm.mod = 1;
        modrm.rm = haveSib ? 4 : get_reg_idx(base);
        *buf = (char)op.disp();
        buf += 1;
    }
    else {
        modrm.mod = 2;
        modrm.rm = haveSib ? 4 : get_reg_idx(base);
        *(int*)buf = op.disp();
        buf += 4;
    }
    if (haveSib) {
        sib.base = base == ar_x ? 5 : get_reg_idx(base);
        sib.index = op.index() == ar_x ? 4 : get_reg_idx(op.index());
        if (op.index() != ar_x) {
            if (op.scale() == 1) sib.scale = 0;
            else if (op.scale() == 2) sib.scale = 1;
            else if (op.scale() == 4) sib.scale = 2;
            else sib.scale = 3;
        }
        else { sib.scale = 0; }
    }
    return buf;
}

static void do_reg(char* buf, const Opnd& op) 
{
    assert(op.is_reg());
    RegName reg = devirt(op.reg(), op.jt());
    ((ModRM*)buf)->reg = getRegIndex(reg);
}

#endif // ifdef _IA32_

void Encoder::mov_impl(const Opnd& _op0, const Opnd& _op1)
{
#if defined(_IA32_)
    //
    // mov_impl() and its calls to add_arg() are the hottest methods on 
    // client startups, especially with the massive ones like Eclipse.
    // Here is a simple, but very useful optimization - we do not call 
    // to regular encoding engine, but rather generate instructions right 
    // here, in-place. We do this only for the most possible variants,
    // that cover about 80-90% of all mov instructions.
    // On Eclipse startups this removes about 1'000'000 trips to Encoder 
    // and related stuff.
    //
    if (_op0.jt() == _op1.jt() && (_op0.jt() == i32 || _op0.jt() == jobj)) {
        char* p = ip();
        char * saveP = p;
#ifdef _DEBUG
        char dbgbuff[20];
        unsigned dbglen;
        char * dbgSave = saveP;
        {
        EncoderBase::Operands args;
        add_args(args, _op0);
        add_args(args, _op1);
        char * p = EncoderBase::encode(dbgbuff, Mnemonic_MOV, args);
        dbglen = p-dbgbuff;
        }
#endif
        if (_op0.is_mem() && _op1.is_imm() && (p=do_mem(p+1, _op0)) != NULL)  {
            // mov mem, imm: C7 /0 
            *(unsigned char*)saveP = 0xC7;
            ((ModRM*)(saveP+1))->reg = 0;
            *(int*)p = _op1.ival();
            assert(!memcmp(dbgbuff, dbgSave, dbglen));
            ip(p+4);
            return;
        }
        else if (_op0.is_reg() && _op1.is_mem() && (p=do_mem(p+1, _op1)) != NULL) {
            // mov reg, mem: 8B /r
            *(unsigned char*)saveP = 0x8B;
            ++saveP;
            do_reg(saveP, _op0); 
            assert(!memcmp(dbgbuff, dbgSave, dbglen));
            ip(p);
            return;
        }
        else if (_op1.is_reg() && _op0.is_mem() && (p=do_mem(p+1, _op0)) != NULL) {
            // mov mem, reg: 89 /r
            *(unsigned char*)saveP = 0x89;
            ++saveP;
            do_reg(saveP, _op1);
            assert(!memcmp(dbgbuff, dbgSave, dbglen));
            ip(p);
            return;
        }
    }
#endif    
    EncoderBase::Operands args;

    assert(_op0.reg() != fp0 && _op1.reg() != fp0);
    assert(is_f(_op0.jt()) == is_f(_op1.jt()));
    
#ifdef _EM64T_
    // A special case on EM64T - emulation of 'mov mem64, imm64'
    if (_op0.is_mem() && _op1.is_imm() && 
        (_op1.jt() == i64 || _op1.jt() == jobj)) {
        // if _op1 fits into 32 bits, then we use a single 
        // 'MOV mem64, imm32' which sign-extend operand - for i64. 
        // Otherwise we generate 2 moves.
        assert(_op0.jt() == i64 || _op0.jt() == jobj);
        if (fits32(_op1.lval()) && _op0.jt() == i64) {
            add_arg(args, _op0);
            add_arg(args, Opnd(_op1.ival()));
            ip(EncoderBase::encode(ip(), Mnemonic_MOV, args));
        }
        else {
            RegName base = devirt(_op0.base());
            RegName index = devirt(_op0.index());
            int disp = _op0.disp();
            unsigned scale = _op0.scale();
            const OpndSize sz = OpndSize_32;
            EncoderBase::Operand mem_lo(sz, base, index, scale, disp);
            
            args.add(mem_lo);
            add_arg(args, Opnd(lo32(_op1.lval())));
            ip(EncoderBase::encode(ip(), Mnemonic_MOV, args));
            
            args.clear();
            disp += 4;
            EncoderBase::Operand mem_hi(sz, base, index, scale, disp);
            args.add(mem_hi);
            add_arg(args, Opnd(hi32(_op1.lval())));
            ip(EncoderBase::encode(ip(), Mnemonic_MOV, args));
        }
        return;
    }
#endif
    Opnd op0, op1;
    emu_fix_opnds(this, op0, op1, _op0, _op1);
    if (op0.jt() == op1.jt()) {
        add_args_same_size(args, op0, op1);
    }
    else {
        add_args(args, op0);
        add_args(args, op1);
    }
    Mnemonic mn = 
        op0.jt() == flt32 ? Mnemonic_MOVSS :
            (op0.jt() == dbl64 ? Mnemonic_MOVQ : Mnemonic_MOV);
    ip(EncoderBase::encode(ip(), mn, args));
    emu_unfix_opnds(this, op0, op1, _op0, _op1);
}

void Encoder::not_impl(const Opnd& op0)
{
    Mnemonic mn = Mnemonic_NOT;
    EncoderBase::Operands args;
    add_arg(args, op0, false);
    ip(EncoderBase::encode(ip(), mn, args));
}

void Encoder::alu_impl(ALU alu, const Opnd& op0, const Opnd& op1)
{
    Mnemonic mn = to_mn(op0.jt(), alu);
    EncoderBase::Operands args;
    add_arg(args, op0, false);//devirt(op0, OpndSize_32));
    // For alu_test can not shrink imm32 to imm8.
    add_arg(args, Opnd(op1), alu != alu_test);
    ip(EncoderBase::encode(ip(), mn, args));
}

void Encoder::nop_impl(U_32 n) 
{
    ip(EncoderBase::nops(ip(), n));
}


void Encoder::cmovcc_impl(COND c, const Opnd& op0, const Opnd& op1) 
{
    ConditionMnemonic cm = devirt(c);
    Mnemonic mn = (Mnemonic)(Mnemonic_CMOVcc + cm);
    EncoderBase::Operands args;
    add_args(args, op0);
    add_args(args, op1);
    ip(EncoderBase::encode(ip(), mn, args));
}

//TODO: reuse the same func for all XCHG ops in this file
static void xchg_regs(Encoder * enc, RegName reg1, RegName reg2) {
    EncoderBase::Operands xargs;
    xargs.add(reg1);
    xargs.add(reg2);
    enc->ip(EncoderBase::encode(enc->ip(), Mnemonic_XCHG, xargs));
}

void Encoder::cmpxchg_impl(bool lockPrefix, AR addrReg, AR newReg, AR oldReg) {
    RegName dNewReg = devirt(newReg);
    RegName dOldReg = devirt(oldReg);
    RegName dAddrReg = devirt(addrReg);
    bool eaxFix = dOldReg != RegName_EAX;
    if (eaxFix) {
        if (dAddrReg == RegName_EAX) {
            dAddrReg = dOldReg;
        } else if (dNewReg == RegName_EAX) {
            dNewReg = dOldReg;
        }
        xchg_regs(this, dOldReg, RegName_EAX);
    }

    if (lockPrefix) {
        ip(EncoderBase::prefix(ip(), InstPrefix_LOCK));
    }

    EncoderBase::Operands args;
    args.add(EncoderBase::Operand(OpndSize_32, dAddrReg, 0)); //TODO: EM64t fix!
    args.add(dNewReg);
    args.add(RegName_EAX);
    ip(EncoderBase::encode(ip(), Mnemonic_CMPXCHG, args));

    if (eaxFix) {
        xchg_regs(this, RegName_EAX, devirt(oldReg));
    }

}

static void mov_regs(Encoder* enc, RegName r_dst, RegName r_src) {
    EncoderBase::Operands args;
    args.add(r_dst);
    args.add(r_src);
    enc->ip(EncoderBase::encode(enc->ip(), Mnemonic_MOV, args));
}

void Encoder::volatile64_op_impl(Opnd& where, AR hi_part, AR lo_part, bool is_put) {
    int regSize=sizeof(void*);
    RegName regs[] = {RegName_EAX, RegName_EBX, RegName_ECX, RegName_EDX, RegName_ESI};
    {//free EAX,EBX,ECX,EDX and ESI registers to use cmpxchg8b
        { //SUB ESP 20 -> protect spill area from OS
            EncoderBase::Operands args;
            args.add(RegName_ESP);
            args.add(EncoderBase::Operand(OpndSize_32, (long long)5*regSize));
            ip(EncoderBase::encode(ip(), Mnemonic_SUB, args));
        }
        for (int i=0;i<5;i++) {
            EncoderBase::Operands args;
            args.add(EncoderBase::Operand(OpndSize_32, RegName_ESP, i*regSize));
            args.add(regs[i]);
            ip(EncoderBase::encode(ip(), Mnemonic_MOV, args));
        }
    }

    //load address to ESI
    lea_impl(virt(RegName_ESI), where);
    
    

    RegName r_hi = devirt(hi_part);
    RegName r_lo = devirt(lo_part);

    if (is_put) {
        //load value to ECX/EBX.
        RegName r_lo2=r_lo;
        if (r_lo2 == RegName_ECX) {
            RegName r_free = r_lo2!=RegName_EAX ? RegName_EAX : RegName_EDX;
            mov_regs(this, r_free, r_lo2);
            r_lo2 = r_free;
        }
        mov_regs(this, RegName_ECX, r_hi);
        mov_regs(this, RegName_EBX, r_lo2);
    }

    //3. set lock prefix
    unsigned loop_ip = ipoff();
    ip(EncoderBase::prefix(ip(), InstPrefix_LOCK));
     
    //do cmpxchg8b
    {
        EncoderBase::Operands args;
        args.add(EncoderBase::Operand(OpndSize_64, RegName_ESI, 0));
        ip(EncoderBase::encode(ip(), Mnemonic_CMPXCHG8B, args));
    }

    if (is_put) {
        //loop until not successful
        unsigned br_off = br(nz, 0, 0);
        patch(br_off, ip(loop_ip));
    } else {
        //store result of get to lo_part and hi_part
        RegName lo_res = RegName_EAX;
        if (r_hi == lo_res) {
            mov_regs(this, RegName_EBX, lo_res);
            lo_res = RegName_EBX;
        }
        mov_regs(this, r_hi, RegName_EDX);
        mov_regs(this, r_lo, lo_res);
    }

    //restore initial regs values
    {
        for (int i=0;i<5;i++) {
            RegName r = regs[i];
            if (!is_put && (r == r_hi || r == r_lo)) { 
                continue;
            }
            EncoderBase::Operands args;
            args.add(regs[i]);
            args.add(EncoderBase::Operand(OpndSize_32, RegName_ESP, i*regSize));
            ip(EncoderBase::encode(ip(), Mnemonic_MOV, args));
        }
        { //ADD ESP 20 -> restore ESP
            EncoderBase::Operands args;
            args.add(RegName_ESP);
            args.add(EncoderBase::Operand(OpndSize_32, (long long)5*regSize));
            ip(EncoderBase::encode(ip(), Mnemonic_ADD, args));
        }

    }
}

void Encoder::lea_impl(const Opnd& reg, const Opnd& mem)
{
    EncoderBase::Operands args;
    add_args(args, reg);
    add_args(args, mem);
    ip(EncoderBase::encode(ip(), Mnemonic_LEA, args));
}

void Encoder::movp_impl(AR op0, const void *op1)
{
    EncoderBase::Operands args;
    args.add(devirt(op0));
#ifdef _EM64T_
    args.add(EncoderBase::Operand(OpndSize_64, (int_ptr)op1));
#else
    args.add(EncoderBase::Operand(OpndSize_32, (int_ptr)op1));
#endif
    ip(EncoderBase::encode(ip(), Mnemonic_MOV, args));
}

void Encoder::sx1_impl(const Opnd& _op0, const Opnd& _op1)
{
    check_args(_op0, _op1);
    Opnd op0, op1;
    emu_fix_opnds(this, op0, op1, _op0, _op1);
    EncoderBase::Operands args;
    add_args(args, op0, op1);
    ip(EncoderBase::encode(ip(), Mnemonic_MOVSX, args));
    emu_unfix_opnds(this, op0, op1, _op0, _op1);
}

void Encoder::sx2_impl(const Opnd& op0, const Opnd& op1)
{
    check_args(op0, op1);
    EncoderBase::Operands args;
    add_args(args, op0, op1);
    ip(EncoderBase::encode(ip(), Mnemonic_MOVSX, args));
}

void Encoder::sx_impl(const Opnd& op0, const Opnd& op1)
{
    check_args(op0, op1);
    if (op1.jt() == i8) {
        sx1(op0, op1);
    }
    else if (op1.jt() == i16 || op1.jt() == u16) {
        sx2(op0, op1);
    }
    else {
        EncoderBase::Operands args;
        add_args(args, op0, op1);
        ip(EncoderBase::encode(ip(), Mnemonic_MOVSX, args));
    }
}

void Encoder::zx1_impl(const Opnd& _op0, const Opnd& _op1)
{
    check_args(_op0, _op1);
    Opnd op0, op1;
    emu_fix_opnds(this, op0, op1, _op0, _op1);
    EncoderBase::Operands args;
    add_args(args, op0, op1);
    ip(EncoderBase::encode(ip(), Mnemonic_MOVZX, args));
    emu_unfix_opnds(this, op0, op1, _op0, _op1);
}

void Encoder::zx2_impl(const Opnd& op0, const Opnd& op1)
{
    check_args(op0, op1);
    EncoderBase::Operands args;
    add_args(args, op0, op1);
    ip(EncoderBase::encode(ip(), Mnemonic_MOVZX, args));
}

void Encoder::fld_impl(jtype jt, AR op0, AR base, int disp, AR index, unsigned scale)
{
    EncoderBase::Operands args;
    Mnemonic mn = jt == dbl64 ? Mnemonic_MOVSD : Mnemonic_MOVSS;
    OpndSize sz = jt == dbl64 ? OpndSize_64 : OpndSize_32;
    if (op0 == fp0) {
        mn = Mnemonic_FLD;
        args.add(jt == dbl64 ? RegName_FP0D : RegName_FP0S);
    }
    else {
        args.add(devirt(op0, jt));
    }
    args.add(EncoderBase::Operand(sz, devirt(base), devirt(index), scale, disp));
    ip(EncoderBase::encode(ip(), mn, args));
}

void Encoder::fst_impl(jtype jt, AR op0, AR base, int disp, AR index,
                       unsigned scale)
{
    EncoderBase::Operands args;
    OpndSize sz = jt == dbl64 ? OpndSize_64 : OpndSize_32;
    Mnemonic mn = jt == dbl64 ? Mnemonic_MOVSD : Mnemonic_MOVSS;
    args.add(EncoderBase::Operand(sz, devirt(base), devirt(index), scale, disp));
    if (op0 == fp0) {
        mn = Mnemonic_FSTP;
        args.add(jt == dbl64 ? RegName_FP0D : RegName_FP0S);
    }
    else {
        args.add(devirt(op0, jt));
    }
    ip(EncoderBase::encode(ip(), mn, args));
}

int Encoder::push_impl(const Opnd& op0)
{
    //assert(!is_f(gr));
    EncoderBase::Operands args;
    add_args(args, op0);
    ip(EncoderBase::encode(ip(), Mnemonic_PUSH, args));
    return STACK_SLOT_SIZE;
}

int Encoder::pop_impl(const Opnd& op0)
{
    //assert(!is_f(gr));
    EncoderBase::Operands args;
    add_args(args, op0);
    ip(EncoderBase::encode(ip(), Mnemonic_POP, args));
    return STACK_SLOT_SIZE;
}

void Encoder::call_impl(const Opnd& target)
{
    EncoderBase::Operands args;
    add_args(args, target);
    ip(EncoderBase::encode(ip(), Mnemonic_CALL, args));
}

void Encoder::ret_impl(unsigned pop)
{
    EncoderBase::Operands args;
    if (pop != 0) {
        assert(fits_i16(pop));
        args.add(EncoderBase::Operand(OpndSize_16, pop));
    }
    ip(EncoderBase::encode(ip(), Mnemonic_RET, args));
}

void Encoder::br_impl(COND cond, HINT hint)
{
    if (hint != hint_none) {
        // Hints are only allowed for conditional branches
        assert(cond != cond_none);
        ip(EncoderBase::prefix(ip(), devirt(hint)));
    }
    EncoderBase::Operands args(0);
    Mnemonic mn;
    if (cond == cond_none) {
        mn = Mnemonic_JMP;
    }
    else {
        mn = (Mnemonic)(Mnemonic_Jcc+devirt(cond));
    }
    ip(EncoderBase::encode(ip(), mn, args));
}

void Encoder::br_impl(const Opnd& op, COND cond, HINT hint)
{
    // Conditional indirect branches are not supported on IA32/EM64T
    assert(cond==cond_none);
    // makes no sense to have hint with unconditional branch
    assert(hint==hint_none);
    EncoderBase::Operands args;
    add_args(args, op);
    ip(EncoderBase::encode(ip(), Mnemonic_JMP, args));
}

void Encoder::trap_impl(void)
{
    ip(EncoderBase::encode(ip(), Mnemonic_INT3, EncoderBase::Operands()));
}

}
}; // ~namespace Jitrino::Jet
