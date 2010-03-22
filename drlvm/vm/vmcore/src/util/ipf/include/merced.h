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
 * @author Intel, Evgueni Brevnov
 */  

#ifndef _MERCED_EMIT
#define _MERCED_EMIT

#include <fcntl.h> 
#ifdef PLATFORM_POSIX
#include <sys/io.h>
#else
#include <io.h>
#endif
#include <stdio.h>
#include <stdlib.h>

//MVM
#include <fstream>

using namespace std;

#include <string.h>
#include <ctype.h>
#include <assert.h>
#include "open/types.h"

#define  IPF_INSTRUCTION_LEN 16  // 128 bits
#define  ENC_N_TMPLT 16
#define  ENC_N_SLOTS  3
#define  ENC_ALL_AVLB_TMPLTS 0x5bf7

enum EM_Templates
{
    TMPLT_mii   = 0,
    TMPLT_miSi  = 1,
    TMPLT_mli   = 2,
    TMPLT_bad1  = 3,
    TMPLT_mmi   = 4,
    TMPLT_mSmi  = 5,
    TMPLT_mfi   = 6,
    TMPLT_mmf   = 7,
    TMPLT_mib   = 8,
    TMPLT_mbb   = 9,
    TMPLT_bad2  = 10,
    TMPLT_bbb   = 11,
    TMPLT_mmb   = 12,
    TMPLT_bad3  = 13,
    TMPLT_mfb   = 14,
    TMPLT_bad4  = 15,
};

//
//  Slot position
//
enum IpfSlotPos {
    IpfSlot0 = 0,
    IpfSlot1 = 1,
    IpfSlot2 = 2,
    IpfNoSlot = 3
};

//
//  IPF instruction type
//
enum IpfInstType {
    IpfNull_Inst  = 0,
    IpfNop_Inst   = 1,
    IpfA_Inst     = 2,
    IpfM_Inst     = 3,
    IpfI_Inst     = 4,
    IpfF_Inst     = 5,
    IpfB_Inst     = 6,
    IpfX_Inst     = 7
};

enum EM_Syllable_Type 
{
    ST_null         = 0,
    ST_n            = 1,    /* Pseudo inst., etc. with no correspond syll */
    ST_a            = 2,    /* A syll                                     */
    ST_m            = 3,    /* M syll                                     */
    ST_i            = 4,    /* I syll                                     */
    ST_f            = 5,    /* F syll                                     */
    ST_b            = 6,    /* B syll                                     */
    ST_il           = 7,    /* 2 syllables - eg. movl                     */
    ST_bl           = 8,    /* Last B syll in a bundle                    */
    ST_bn           = 9,    /* nop.b, doesn't break a cycle !         */
    ST_ma           = 10,   /* alloc */
    ST_br           = 11,   /* loop branches*/
    ST_fc           = 12,   /* fmisc */
    ST_is           = 13,   /* shift */
    ST_m0           = 14,   /* M0 unit - e.g., getf */
    ST_last_type    = 14,
    ST_first_real   = ST_a
};

//
//  Information about templates
//
class IpfTemplInfo {
public:
    IpfTemplInfo() {}
    static EM_Syllable_Type getSylType(EM_Templates t, IpfSlotPos slot) {
        assert(slot != IpfNoSlot);
        return info[t].sylType[slot];
    }
    static char *           getName(EM_Templates t) {
        return info[t].name;
    }
    struct TemplDesc {
        EM_Syllable_Type sylType[ENC_N_SLOTS];
        char *           name;
    };
private:
    static TemplDesc info[];
};

enum EM_Application_Register
{
    AR_kr0 = 0,
    AR_kr1 = 1,
    AR_kr2 = 2,
    AR_kr3 = 3,
    AR_kr4 = 4,
    AR_kr5 = 5,
    AR_kr6 = 6,
    AR_kr7 = 7,
    AR_rsc = 16,
    AR_bsp = 17,
    AR_bspstore = 18,
    AR_rnat = 19,
    AR_fcr = 21,
    AR_eflag = 24,
    AR_csd = 25,
    AR_ssd = 26,
    AR_cflg = 27,
    AR_fsr = 28,
    AR_fir = 29,
    AR_fdr = 30,
    AR_ccv = 32,
    AR_unat = 36,
    AR_fpsr = 40,
    AR_itc = 44,
    AR_pfs = 64,
    AR_lc = 65,
    AR_ec = 66
};

enum Int_Comp_Rel
{
    icmp_invalid = 0,
    icmp_eq,
    icmp_ne,
    icmp_gt,
    icmp_ge,
    icmp_lt,
    icmp_le,
    icmp_gtu,
    icmp_geu,
    icmp_ltu,
    icmp_leu
};

enum Float_Comp_Rel
{
    fcmp_invalid = 0,
    fcmp_eq,
    fcmp_neq,
    fcmp_lt,
    fcmp_le,
    fcmp_gt,
    fcmp_ge,
    fcmp_nlt,
    fcmp_nle,
    fcmp_ngt,
    fcmp_nge,
    fcmp_unord,
    fcmp_ord
};

enum Compare_Extension
{
    cmp_none = 0,
    cmp_unc,
    cmp_and,
    cmp_or,
    cmp_or_andcm,
    cmp_last
};

enum Branch_Type
{
    br_cond = 0,
    br_ia = 1,
    br_wexit = 2,
    br_wtop = 3,
    br_ret = 4,
    br_cloop = 5,
    br_cexit = 6,
    br_ctop = 7,
    br_call = 8
};

enum Branch_Prefetch_Hint
{
    br_few = 0,
    br_many = 1
};

enum Branch_Whether_Hint
{
    br_sptk = 0,
    br_spnt = 1,
    br_dptk = 2,
    br_dpnt = 3
};

enum Branch_Dealloc_Hint
{
    br_none = 0,
    br_clr = 1
};

enum Branch_Important_Hint {
    br_not_imp = 0,
    br_imp     = 1
};

enum Branch_Predict_Whether_Hint 
{
    brp_sptk = 0,
    brp_none = 1,
    brp_dptk = 2
};

enum Sxt_Size
{   // 21.4.1.1
    sxt_size_1 = 0,
    sxt_size_2 = 1,
    sxt_size_4 = 2,
    sxt_size_invalid = 3
};

enum Float_Precision
{   // 21.6.1.1
    float_none = 0,
    float_s = 1,
    float_d = 2,
    float_parallel = 3
};

enum Float_Status_Field
{   // ??.?.?.?
    sf0 = 0,
    sf1 = 1,
    sf2 = 2,
    sf3 = 3
};

enum FReg_Convert // setf, getf
{   // 21.4.4.1
    freg_sig = 0,
    freg_exp = 1,
    freg_s = 2,
    freg_d = 3
};

enum FFix_Convert // fcvt
{   // 21.6.7.1
    ffix_s = 0,
    ffix_u = 1,
    ffix_s_tr = 2,
    ffix_u_tr = 3
};

enum Float_Merge
{
    fmerge_s = 0,
    fmerge_ns = 1,
    fmerge_se = 2
};

enum Int_Mem_Size
{   // 21.4.1.1
    int_mem_size_1 = 0,
    int_mem_size_2 = 1,
    int_mem_size_4 = 2,
    int_mem_size_8 = 3
};

enum Float_Mem_Size
{   // 21.4.1.7
    float_mem_size_s = 2,
    float_mem_size_d = 3,
    float_mem_size_8 = 1,
    float_mem_size_e = 0
};

enum Ld_Flag
{   // 21.4.1.1
    mem_ld_none = 0,
    mem_ld_s = 1,
    mem_ld_a = 2,
    mem_ld_sa = 3,
    mem_ld_bias = 4,
    mem_ld_acq = 5,
    mem_ld_fill = 6,
    mem_ld_c_clr = 8,
    mem_ld_c_nc = 9,
    mem_ld_c_clr_acq = 10
};

enum St_Flag
{   // 21.4.1.4 -- base = 0x30
    mem_st_none = 0,
    mem_st_rel = 1,
    mem_st_spill = 2
};

enum Cmpxchg_Flag
{
    mem_cmpxchg_acq = 0,
    mem_cmpxchg_rel = 1,
};

enum Mem_Hint
{
    mem_none = 0,
    mem_ntl = 1,
    mem_nta = 3
};

enum Lfetch_Hint 
{
    lfetch_none = 0,
    lfetch_nt1  = 1,
    lfetch_nt2  = 2,
    lfetch_nta  = 3
};

enum Xla_Flag
{   // 21.6.1.2
    l_form  = 0,  // low form
    h_form  = 3,  // high form
    hu_form = 2   // high unsigned form
};



class Encoder_32
{
protected:
    Encoder_32(unsigned os) : _offset(os), _value(0) {}
    void encode(unsigned bits, unsigned left, unsigned right)
    {
        assert(bits <= (unsigned)((1 << (left - right + 1)) - 1));
        assert(left < 32 + _offset);
        _value |= (bits << (right - _offset));
    }
    unsigned emit() { unsigned return_value = _value; _value = 0; return return_value; }
private:
    unsigned _offset;
    unsigned _value;
};

class Encoder_128
{
public:
    void code_emit(unsigned char *code) 
    {
        for (unsigned i = 0; i < IPF_INSTRUCTION_LEN; i++)
            code[_offset+i] = _char_value[i];
        _offset += 16;
    }
    unsigned get_offset() { return _offset; }
protected:
    Encoder_128(unsigned init_offset) : _offset(init_offset) { reset(); }
    virtual void reset()
    {
        for (int i = 0; i < IPF_INSTRUCTION_LEN; i++)
            _char_value[i] = 0;
    }
    void slot_encode(int slot_num, unsigned bits, int offset, int bit_length=32)
    {
        switch (slot_num)
        {
        case 0:
            assert(5+offset+bit_length-1 <= 45);
            _encode_32(bits, 5 + offset + bit_length - 1, 5 + offset);
            break;
        case 1:
            assert(46+offset+bit_length-1 <= 86);
            _encode_32(bits, 46 + offset + bit_length - 1, 46 + offset);
            break;
        case 2:
            assert(87+offset+bit_length-1 <= 127);
            _encode_32(bits, 87 + offset + bit_length - 1, 87 + offset);
            break;
        }
    }
    void slot_reset(int slot_num, int offset, int bit_length = 32);
    void slot_rewrite(int slot_num, unsigned bits, int offset, int bit_length=32) {
        slot_reset(slot_num,offset,bit_length);
        slot_encode(slot_num,bits,offset,bit_length);
    }
    void template_encode(unsigned char bits) { _encode_32((unsigned)bits, 4, 1); }
    void set_stop_bit() { _char_value[0] |= 0x1; }
private:
    void _encode_32(unsigned bits, unsigned left, unsigned right)
    {
        // make sure caller checks that left >= right
        unsigned stop_byte = left >> 3;
        unsigned stop_bit = left & 0x7;
        unsigned current_byte = right >> 3;
        unsigned current_bit = right & 0x7;
        // do from right to left
        int bit_length, pos = 0;
        unsigned mask_bits;
        while (current_byte <= stop_byte)
        {
            bit_length = (current_byte==stop_byte) ? (stop_bit+1 - current_bit) : (8 - current_bit);
            mask_bits = ((1 << bit_length) - 1);
            // encode
            _char_value[current_byte] |= ((bits >> pos) & mask_bits) << current_bit;
            // update
            if (pos==0) current_bit = 0;
            pos += bit_length;
            current_byte++;
        }
    }
    void _reset_32(unsigned left, unsigned right);
protected:
    unsigned char _char_value[IPF_INSTRUCTION_LEN];
    unsigned _offset;
};

class Register_Encoder_offset6 : public Encoder_32
{
public:
    Register_Encoder_offset6() : Encoder_32(6) {}
    unsigned R3_R2_R1(unsigned r3, unsigned r2, unsigned r1);
    unsigned R3_R1(unsigned r3, unsigned r1);
    unsigned R2_R1(unsigned r2, unsigned r1);
    unsigned R3_R2(unsigned r3, unsigned r2);
    unsigned R1(unsigned r1);
    unsigned R2(unsigned r2);
    unsigned R3(unsigned r3);
    unsigned cR3_R1(unsigned r3, unsigned r1);
    unsigned P2_P1_R3_R2(unsigned p2, unsigned p1, unsigned r3, unsigned r2);
    unsigned P2_P1_R3(unsigned p2, unsigned p1, unsigned r3);
    unsigned B2_B1(unsigned b2, unsigned b1);
    unsigned B1(unsigned b1);
    unsigned I20_M20_M21(unsigned r2);
    unsigned F4_F3_F2_F1(unsigned f4, unsigned f3, unsigned f2, unsigned f1);
    unsigned P2_P1_F3_F2(unsigned p2, unsigned p1, unsigned f3, unsigned f2);
    unsigned P2_P1_F2(unsigned p2, unsigned p1, unsigned f2);
    unsigned F3_F2_F1(unsigned f3, unsigned f2, unsigned f1);
    unsigned P2_F3_F2_F1(unsigned p2, unsigned f3, unsigned f2, unsigned f1);
    unsigned F2_F1(unsigned f2, unsigned f1);
    unsigned B2_R1(unsigned b2, unsigned r1);
    unsigned R2_B1(unsigned r2, unsigned b1);
};

class Immediate_Encoder_offset6 : public Encoder_32
{
public:
    Immediate_Encoder_offset6() : Encoder_32(6) {}
    unsigned A2(unsigned count2);
    unsigned A3_A8_I27_M30(unsigned imm8);
    unsigned A4(unsigned imm14);
    unsigned A5(unsigned imm22);
    unsigned I6(unsigned count5);
    unsigned I8(unsigned count5);
    unsigned I10(unsigned count6);
    unsigned I11(unsigned len6, unsigned pos6);
    unsigned I12(unsigned len6, unsigned pos6);
    unsigned I13(unsigned len6, unsigned pos6, unsigned imm8);
    unsigned I14(unsigned len6, unsigned pos6, unsigned imm1);
    unsigned I15(unsigned len4, unsigned pos6);
    unsigned I18(unsigned upper_32, unsigned lower_32);
    unsigned I19_M37_B9_F15(unsigned imm21);
    unsigned I21(unsigned tag13, unsigned IP);
    unsigned I23(unsigned mask17);
    unsigned I24(unsigned imm28);
    unsigned M3_M8_M15(unsigned imm9);
    unsigned M5_M10(unsigned imm9);
    unsigned M34(unsigned il, unsigned o, unsigned r);
    unsigned B1_B2_B3(unsigned target25, unsigned IP);
    unsigned I20_M20_M21(unsigned target25, unsigned IP);
};

class Opcode_Encoder_offset9 : public Encoder_32
{
public:
    Opcode_Encoder_offset9() : Encoder_32(9) {}
    unsigned A1_A2_A3_A4(unsigned opcode, unsigned x2a, unsigned ve, unsigned x4=0, unsigned x2b=0);
    unsigned A5_I15(unsigned opcode);
    unsigned A6_A7(unsigned opcode, unsigned tb, unsigned x2, unsigned ta, unsigned c);
    unsigned A8(unsigned opcode, unsigned x2, unsigned ta, unsigned c);
    unsigned I5_I7(unsigned opcode, unsigned za, unsigned x2a, unsigned zb, unsigned ve, unsigned x2c, unsigned x2b);
    unsigned I10_I11(unsigned opcode, unsigned x2, unsigned x, unsigned y=0);
    unsigned I12_I13_I14(unsigned opcode, unsigned x2, unsigned x, unsigned y=0);
    unsigned I18(unsigned opcode, unsigned vc);
    unsigned I19_I22_I25_I26_I27_I28_I29_M29_M31(unsigned opcode, unsigned x3, unsigned x6);
    unsigned I21(unsigned opcode, unsigned x3, unsigned ih, unsigned x, unsigned wh, unsigned p, unsigned pbtv);
    unsigned I20_I23_I24_M20_M21(unsigned opcode, unsigned x3);
    unsigned I25(unsigned opcode, unsigned x3, unsigned x6);
    unsigned B1_B2_B3_B4_B5(unsigned opcode, unsigned d, unsigned wh, unsigned p, unsigned x6=0);
    unsigned B8_B9(unsigned opcode, unsigned x6);
    unsigned M24_M25_M26_M27_M37(unsigned opcode, unsigned x3, unsigned x4, unsigned x2);
    unsigned M24_M25_M26_M37(unsigned opcode, unsigned x3, unsigned x4, unsigned x2);
    unsigned F15(unsigned opcode, unsigned x, unsigned x6);
    unsigned M1_M2_M4_M6_M7_M9_M11_M12_M13_M14_M16_M17(unsigned opcode, unsigned x6, unsigned hint, unsigned x, unsigned m=0);
    unsigned M3_M5_M8_M10_M15(unsigned opcode, unsigned x6, unsigned hint);
    unsigned M18_M19(unsigned opcode, unsigned m, unsigned x6, unsigned x);
    unsigned M20_M21_M22_M23_M34(unsigned opcode, unsigned x3);
    unsigned F1_F2_F3(unsigned opcode, unsigned x, unsigned sf_x2=0);
    unsigned F8_F9_F10_F11_F12_F13(unsigned opcode, unsigned x, unsigned x6, unsigned sf=0);
    unsigned F4(unsigned opcode, unsigned rb, unsigned sf, unsigned ra, unsigned ta);
    unsigned F5(unsigned opcode, unsigned fc2, unsigned fclass7c, unsigned ta);
    unsigned F6(unsigned opcode, unsigned q, unsigned sf, unsigned x);
};

//////////////////////////////////////////////////////////////////////////

class Merced_Encoder : public Encoder_128
{
public:
    Merced_Encoder(unsigned init_offset=0) : 
    Encoder_128(init_offset), _buffer(NULL), slot_num(0) {}
    virtual void file_begin(void *f)
    {
        _buffer = (unsigned char *)f;
        _offset = 0;
    }
    virtual void file_end(void) {}
    virtual void function_begin(const char *func_name) {}
    virtual void function_end(const char *func_name) {}
    virtual void comment(const char *s) {}
    virtual void code_emit(void)
    {
        Encoder_128::code_emit(_buffer);
    }
    virtual void set_template(unsigned char tf) 
    { 
        reset(); // template_format should be the first one to be called
        template_encode(tf); 
    }
    virtual void stop_bit() { set_stop_bit(); }
    void encode(unsigned opcode9, unsigned imm6, unsigned reg6, unsigned qp=0);
    void encode_long(unsigned opcode9, unsigned imm6, unsigned imm31x, unsigned imm10x, 
                                 unsigned reg6, unsigned qp=0);
    void encode_long_X3_X4(unsigned opcode4,
        Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh,
        unsigned b1, unsigned i, uint64 imm39, unsigned imm20b, unsigned qp=0);
    ////////////////////////////////////////////////////////
    void reset()
    {
        Encoder_128::reset();
        slot_num = 0;
    }
    ////////////////////////////////////////////////////////
    virtual void ipf_nop(EM_Syllable_Type tv, unsigned imm21=0);
    virtual void ipf_add(unsigned dest, unsigned src1, unsigned src2, unsigned pred=0);
    virtual void ipf_sub(unsigned dest, unsigned src1, unsigned src2, unsigned pred=0);
    virtual void ipf_addp4(unsigned dest, unsigned src1, unsigned src2, unsigned pred=0);
    virtual void ipf_and(unsigned dest, unsigned src1, unsigned src2, unsigned pred=0);
    virtual void ipf_or(unsigned dest, unsigned src1, unsigned src2, unsigned pred=0);
    virtual void ipf_xor(unsigned dest, unsigned src1, unsigned src2, unsigned pred=0);
    virtual void ipf_shladd(unsigned dest, unsigned src1, int count, unsigned src2, unsigned pred=0);
    virtual void ipf_subi(unsigned dest, int imm, unsigned src, unsigned pred=0);
    virtual void ipf_andi(unsigned dest, int imm, unsigned src, unsigned pred=0);
    virtual void ipf_ori(unsigned dest, int imm, unsigned src, unsigned pred=0);
    virtual void ipf_xori(unsigned dest, int imm, unsigned src, unsigned pred=0);
    virtual void ipf_adds(unsigned dest, int imm14, unsigned src, unsigned pred=0);
    virtual void ipf_addp4i(unsigned dest, int imm14, unsigned src, unsigned pred=0);
    virtual void ipf_addl(unsigned dest, int imm22, unsigned src, unsigned pred=0);
    virtual void ipf_cmp(Int_Comp_Rel cr, Compare_Extension cx, unsigned p1, unsigned p2, unsigned r2, unsigned r3, bool cmp4=false, unsigned pred=0);
    virtual void ipf_cmpz(Int_Comp_Rel cr, Compare_Extension cx, unsigned p1, unsigned p2, unsigned r3, bool cmp4=false, unsigned pred=0);
    virtual void ipf_cmpi(Int_Comp_Rel cr, Compare_Extension cx, unsigned p1, unsigned p2, int imm, unsigned r3, bool cmp4=false, unsigned pred=0);
    virtual void ipf_movl(unsigned dest, unsigned upper_32, unsigned lower_32, unsigned pred=0);
    virtual void ipf_movi64(unsigned dest, uint64 imm64, unsigned pred=0) { ipf_movl(dest,(unsigned)((imm64 >> 32) & 0x0FFFFFFFF),(unsigned)(imm64 & 0x0FFFFFFFF),pred);}
    virtual void ipf_extr(unsigned dest, unsigned src, int pos6, int len6, unsigned pred=0);
    virtual void ipf_extru(unsigned dest, unsigned src, int pos6, int len6, unsigned pred=0);
    virtual void ipf_depz(unsigned dest, unsigned src, int pos6, int len6, unsigned pred=0);
    virtual void ipf_depiz(unsigned dest, int imm8, int pos6, int len6, unsigned pred=0);
    virtual void ipf_depi(unsigned dest, int imm1, unsigned src, int pos6, int len6, unsigned pred=0);
    virtual void ipf_dep(unsigned dest, unsigned r2, unsigned r3, int pos6, int len4, unsigned pred=0);
    virtual void ipf_br(Branch_Type btype, Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned target25, unsigned pred=0);
    virtual void ipf_brcall(Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b1, unsigned target25, unsigned pred=0);
    virtual void ipf_bri(Branch_Type btype, Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b2, unsigned pred=0);
    virtual void ipf_brret(Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b2, unsigned pred=0);
    virtual void ipf_bricall(Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b1, unsigned b2, unsigned pred=0);
    virtual void ipf_brl_cond(Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, uint64 imm64, unsigned pred=0);
    virtual void ipf_brl_call(Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b1, uint64 imm64, unsigned pred=0);
    virtual void ipf_chk_s_i(unsigned src, unsigned target25, unsigned pred=0);
    virtual void ipf_chk_s_m(unsigned src, unsigned target25, unsigned pred=0);
    virtual void ipf_chk_f_s(unsigned src, unsigned target25, unsigned pred=0);
    virtual void ipf_ld(Int_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned pred=0);
    virtual void ipf_ld_inc_reg(Int_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned inc_reg, unsigned pred=0);
    virtual void ipf_ld_inc_imm(Int_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned inc_imm, unsigned pred=0);
    virtual void ipf_st(Int_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src, unsigned pred=0);
    virtual void ipf_st_inc_imm(Int_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src, unsigned inc_imm, unsigned pred=0);
    virtual void ipf_ldf(Float_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned pred=0);
    virtual void ipf_ldf_inc_reg(Float_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned inc_reg, unsigned pred=0);
    virtual void ipf_ldf_inc_imm(Float_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned inc_imm, unsigned pred=0);
    virtual void ipf_lfetch(bool exclusive, bool fault, Lfetch_Hint hint, unsigned addrreg, unsigned pred=0);
    virtual void ipf_stf(Float_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src, unsigned pred=0);
    virtual void ipf_stf_inc_imm(Float_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src, unsigned inc_imm, unsigned pred=0);
    virtual void ipf_mov(unsigned dest, unsigned src, unsigned pred=0) { ipf_adds(dest, 0, src, pred); } // pseudo-op
    virtual void ipf_movi(unsigned dest, int imm22, unsigned pred=0) { ipf_addl(dest, imm22, 0, pred); } // pseudo-op
    virtual void ipf_neg(unsigned dest, unsigned src, unsigned pred=0) { ipf_subi(dest, 0, src, pred); } // pseudo-op
    virtual void ipf_sxt(Sxt_Size size, unsigned dest, unsigned src, unsigned pred=0);
    virtual void ipf_zxt(Sxt_Size size, unsigned dest, unsigned src, unsigned pred=0);
    virtual void ipf_shl(unsigned dest, unsigned src1, unsigned src2, unsigned pred=0);
    virtual void ipf_shr(unsigned dest, unsigned src1, unsigned src2, unsigned pred=0);
    virtual void ipf_shru(unsigned dest, unsigned src1, unsigned src2, unsigned pred=0);
    virtual void ipf_shli(unsigned dest, unsigned src1, int count, unsigned pred=0) { ipf_depz(dest, src1, count, 64-count, pred); }  // pseudo-op
    virtual void ipf_setf(FReg_Convert form, unsigned fdest, unsigned src, unsigned pred=0);
    virtual void ipf_getf(FReg_Convert form, unsigned dest, unsigned fsrc, unsigned pred=0);
    virtual void ipf_fma(Float_Precision pc, Float_Status_Field sf, unsigned dest, unsigned src1, unsigned src2, unsigned src3, unsigned pred=0);
    virtual void ipf_fnma(Float_Precision pc, Float_Status_Field sf, unsigned dest, unsigned src1, unsigned src2, unsigned src3, unsigned pred=0);
    virtual void ipf_fms(Float_Precision pc, Float_Status_Field sf, unsigned dest, unsigned src1, unsigned src2, unsigned src3, unsigned pred=0);
    virtual void ipf_frcpa(Float_Status_Field sf, unsigned dest, unsigned p2, unsigned src1, unsigned src2, unsigned pred=0);
    virtual void ipf_fadd(Float_Precision pc, Float_Status_Field sf, unsigned dest, unsigned src1, unsigned src2, unsigned pred=0) { ipf_fma(pc,sf,dest,src1,1,src2,pred); }
    virtual void ipf_fsub(Float_Precision pc, Float_Status_Field sf, unsigned dest, unsigned src1, unsigned src2, unsigned pred=0) { ipf_fms(pc,sf,dest,src1,1,src2,pred); }
    virtual void ipf_fmul(Float_Precision pc, Float_Status_Field sf, unsigned dest, unsigned src1, unsigned src2, unsigned pred=0) { ipf_fma(pc,sf,dest,src1,src2,0,pred); }
    virtual void ipf_fnorm(Float_Precision pc, Float_Status_Field sf, unsigned dest, unsigned src, unsigned pred=0) { ipf_fma(pc,sf,dest,src,1,0,pred);}
    virtual void ipf_fmerge(Float_Merge fm, unsigned dest, unsigned src1, unsigned src2, unsigned pred=0);
    virtual void ipf_fcmp(Float_Comp_Rel cr, Compare_Extension cx, unsigned p1, unsigned p2, unsigned f2, unsigned f3, unsigned pred=0);
    virtual void ipf_fclass(Compare_Extension cx, unsigned p1, unsigned p2, unsigned f2, unsigned fclass9, unsigned pred=0);
    virtual void ipf_fcvt_fx(FFix_Convert fc, Float_Status_Field sf, unsigned dest, unsigned src, unsigned pred=0);
    virtual void ipf_fcvt_xf(unsigned dest, unsigned src, unsigned pred=0);
    virtual void ipf_fmov(unsigned dest, unsigned src, unsigned pred=0) { ipf_fmerge(fmerge_s,dest,src,src,pred); } // pseudo-op
    virtual void ipf_fneg(unsigned dest, unsigned src, unsigned pred=0) { ipf_fmerge(fmerge_ns,dest,src,src,pred); } // pseudo-op
    virtual void ipf_alloc(unsigned dest, unsigned i, unsigned l, unsigned o, unsigned r);
    virtual void ipf_mtbr(unsigned bdest, unsigned src, Branch_Predict_Whether_Hint wh=brp_none, bool ret=false, unsigned offset=0, unsigned pred=0);
    virtual void ipf_mfbr(unsigned dest, unsigned bsrc, unsigned pred=0);
    virtual void ipf_mtap(EM_Application_Register adest, unsigned src, unsigned pred=0);
    virtual void ipf_mfap(unsigned dest, EM_Application_Register asrc, unsigned pred=0);
    virtual void ipf_movip(unsigned dest, unsigned pred=0);
    virtual void ipf_xma(unsigned dest, unsigned src1, unsigned src2, unsigned src3, Xla_Flag flag, unsigned pred=0);
    virtual void ipf_cmpxchg(Int_Mem_Size size, Cmpxchg_Flag flag, Mem_Hint hint, unsigned dest, unsigned r3, unsigned r2, unsigned pred=0);
    virtual void ipf_mtpr(unsigned src1, unsigned mask17, unsigned pred=0);
    virtual void ipf_mtpr_rot(unsigned, unsigned pred=0);
    virtual void ipf_mfpr(unsigned dest, unsigned pred=0);
    virtual void ipf_cover();
    virtual void ipf_flushrs();
    virtual void ipf_mf(unsigned pred=0);
    //
    //  Instruction rewriting
    //
    virtual void rewrite_brl_call_target(const unsigned char * oldBundle, uint64 newTarget);
private:
    Register_Encoder_offset6 reg_enc;
    Immediate_Encoder_offset6 imm_enc;
    Opcode_Encoder_offset9 opc_enc;
    unsigned char *_buffer;
protected:
    int slot_num;
};

class Merced_Encoder_File_Binary : public Merced_Encoder
{
public:
    void file_begin(void *f)
    {
        fptr = (FILE *)f;
    }
    void file_end(void)
    {
    }
    void function_begin(const char *func_name)
    {
        fprintf(fptr,".text\n");
        fprintf(fptr,".proc %s#\n",func_name);
        fprintf(fptr,".align 32\n");
        fprintf(fptr,".global %s#\n", func_name);
        fprintf(fptr,"%s:\n",func_name);
    }
    void function_end(const char *func_name)
    {
        fprintf(fptr,".endp %s#\n",func_name);
    }
    void comment(const char *s)
    {
        fprintf(fptr,"#%s\n",s);
    }
    void code_emit(void) 
    {
        fprintf(fptr,"data8");
        fprintf(fptr," 0x");
        int i;
        for (i = 7; i >=0; i--)
            fprintf(fptr,"%02X",_char_value[i]);
        fprintf(fptr,",0x");
        for (i = 15; i >=8; i--)
            fprintf(fptr,"%02X",_char_value[i]);
        fprintf(fptr,"\n");
    }
private:
    FILE *fptr;
};



struct Template_Descr {
   EM_Syllable_Type syl_type[ENC_N_SLOTS];
   // Slot number after stop, or ENC_N_SLOTS if no stop
   U_8 stop_up;
   // Slot number after stop, or 0 if no stop
   U_8 stop_down;
};

extern Template_Descr tmplt_descr[];

#endif

