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

//
// $Header: /cvs/drl/mrt/vm/vmcore/src/util/ipf/code_emit/merced.cpp,v 1.1.2.1.4.3 2006/03/28 15:08:51 aycherny Exp $
//

#include "open/types.h"
#include "merced.h"


// 20021204 Arrays tmplt_descr and IpfTemplInfo::info are a little
// different and they should be eventually merged.
Template_Descr tmplt_descr[] = {
    {{ST_m,    ST_i,    ST_i},    3, 0},
    {{ST_m,    ST_i,    ST_i},    2, 2},
    {{ST_m,    ST_il,   ST_null}, 3, 0},
    {{ST_null, ST_null, ST_null}, 3, 0},
    {{ST_m,    ST_m,    ST_i},    3, 0},
    {{ST_m,    ST_m,    ST_i},    1, 1},
    {{ST_m,    ST_f,    ST_i},    3, 0},
    {{ST_m,    ST_m,    ST_f},    3, 0},
    {{ST_m,    ST_i,    ST_b},    3, 0},
    {{ST_m,    ST_b,    ST_b},    3, 0},
    {{ST_null, ST_null, ST_null}, 3, 0},
    {{ST_b,    ST_b,    ST_b},    3, 0},
    {{ST_m,    ST_m,    ST_b},    3, 0},
    {{ST_null, ST_null, ST_null}, 3, 0},
    {{ST_m,    ST_f,    ST_b},    3, 0},
    {{ST_null, ST_null, ST_null}, 3, 0}
};

IpfTemplInfo::TemplDesc IpfTemplInfo::info[] = {
    {{ST_m,    ST_i,    ST_i},    ".mii"},
    {{ST_m,    ST_i,    ST_i},    ".mi;i"},
    {{ST_m,    ST_il,   ST_null}, ".mxi"},
    {{ST_null, ST_null, ST_null}, "bad1"},
    {{ST_m,    ST_m,    ST_i},    ".mmi"},
    {{ST_m,    ST_m,    ST_i},    ".m;mi"},
    {{ST_m,    ST_f,    ST_i},    ".mfi"},
    {{ST_m,    ST_m,    ST_f},    ".mmf"},
    {{ST_m,    ST_i,    ST_b},    ".mib"},
    {{ST_m,    ST_b,    ST_b},    ".mbb"},
    {{ST_null, ST_null, ST_null}, ".bad2"},
    {{ST_b,    ST_b,    ST_b},    ".bbb"},
    {{ST_m,    ST_m,    ST_b},    ".mmb"},
    {{ST_null, ST_null, ST_null}, ".bad3"},
    {{ST_m,    ST_f,    ST_b},    ".mfb"},
    {{ST_null, ST_null, ST_null}, ".bad4"}
};

void Encoder_128::slot_reset(int slot_num, int offset, int bit_length) {
    switch (slot_num)
    {
    case 0:
        if (5+offset+bit_length-1 > 45) { assert(0); }
        _reset_32(5 + offset + bit_length - 1, 5 + offset);
        break;
    case 1:
        if (46+offset+bit_length-1 > 86) { assert(0); }
        _reset_32(46 + offset + bit_length - 1, 46 + offset);
        break;
    case 2:
        if (87+offset+bit_length-1 > 127) { assert(0); }
        _reset_32(87 + offset + bit_length - 1, 87 + offset);
        break;
    }
}

void Encoder_128::_reset_32(unsigned left, unsigned right) {
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
        _char_value[current_byte] &= ~(mask_bits << current_bit);
        // update
        if (pos==0) current_bit = 0;
        pos += bit_length;
        current_byte++;
    }
}

unsigned Opcode_Encoder_offset9::A1_A2_A3_A4(unsigned opcode, unsigned x2a, unsigned ve, unsigned x4, unsigned x2b)
{
    encode(opcode, 40, 37);
    encode(x2a, 35, 34);
    encode(ve, 33, 33);
    encode(x4, 32, 29);
    encode(x2b, 28, 27);
    return emit();
}

unsigned Opcode_Encoder_offset9::A5_I15(unsigned opcode)
{
    encode(opcode, 40, 37);
    return emit();
}

unsigned Opcode_Encoder_offset9::A6_A7(unsigned opcode, unsigned tb, unsigned x2, unsigned ta, unsigned c)
{
    encode(opcode, 40, 37);
    encode(tb, 36, 36);
    encode(x2, 35, 34);
    encode(ta, 33, 33);
    encode(c, 12, 12);
    return emit();
}

unsigned Opcode_Encoder_offset9::A8(unsigned opcode, unsigned x2, unsigned ta, unsigned c)
{
    encode(opcode, 40, 37);
    encode(x2, 35, 34);
    encode(ta, 33, 33);
    encode(c, 12, 12);
    return emit();
}

unsigned Opcode_Encoder_offset9::I5_I7(unsigned opcode, unsigned za, unsigned x2a, unsigned zb, unsigned ve, unsigned x2c, unsigned x2b)
{
    encode(opcode, 40, 37);
    encode(za, 36, 36);
    encode(x2a, 35, 34);
    encode(zb, 33, 33);
    encode(ve, 32, 32);
    encode(x2c, 31, 30);
    encode(x2b, 29, 28);
    return emit();
}

unsigned Opcode_Encoder_offset9::I10_I11(unsigned opcode, unsigned x2, unsigned x, unsigned y)
{
    encode(opcode, 40, 37);
    encode(x2, 35, 34);
    encode(x, 33, 33);
    encode(y, 13, 13);
    return emit();
}

unsigned Opcode_Encoder_offset9::I12_I13_I14(unsigned opcode, unsigned x2, unsigned x, unsigned y)
{
    encode(opcode, 40, 37);
    encode(x2, 35, 34);
    encode(x, 33, 33);
    encode(y, 26, 26);
    return emit();
}

unsigned Opcode_Encoder_offset9::I18(unsigned opcode, unsigned vc)
{
    encode(opcode, 40, 37);
    encode(vc, 20, 20);
    return emit();
}

unsigned Opcode_Encoder_offset9::I19_I22_I25_I26_I27_I28_I29_M29_M31(unsigned opcode, unsigned x3, unsigned x6)
{
    encode(opcode, 40, 37);
    encode(x3, 35, 33);
    encode(x6, 32, 27);
    return emit();
}


unsigned Opcode_Encoder_offset9::I21(unsigned opcode, unsigned x3, unsigned ih, unsigned x, unsigned wh, unsigned p, unsigned pbtv)
{
    encode(opcode, 40, 37);
    encode(x3, 35, 33);
    encode(ih, 23, 23);
    encode(x, 22, 22);
    encode(wh, 21, 20);
    encode(p, 12, 12);
    encode(pbtv, 11, 9);
    return emit();
}

unsigned Opcode_Encoder_offset9::I20_I23_I24_M20_M21(unsigned opcode, unsigned x3)
{
    encode(opcode, 40, 37);
    encode(x3, 35, 33);
    return emit();
}

unsigned Opcode_Encoder_offset9::I25(unsigned opcode, unsigned x3, unsigned x6)
{
    encode(opcode, 40, 37);
    encode(x3, 35, 33);
    encode(x6, 32, 27);
    return emit();
}

unsigned Opcode_Encoder_offset9::B1_B2_B3_B4_B5(unsigned opcode, unsigned d, unsigned wh, unsigned p, unsigned x6)
{
    encode(opcode, 40, 37);
    encode(d, 35, 35);
    encode(wh, 34, 33);
    encode(p, 12, 12);
    encode(x6, 32, 27);
    return emit();
}

unsigned Opcode_Encoder_offset9::B8_B9(unsigned opcode, unsigned x6)
{
    encode(opcode, 40, 37);
    encode(x6, 32, 27);
    return emit();
}

unsigned Opcode_Encoder_offset9::M24_M25_M26_M27_M37(unsigned opcode, unsigned x3, unsigned x4, unsigned x2)
{
    encode(opcode, 40, 37);
    encode(x3, 35, 33);
    encode(x2, 32, 31);
    encode(x4, 30, 27);
    return emit();
}

unsigned Opcode_Encoder_offset9::M24_M25_M26_M37(unsigned opcode, unsigned x3, unsigned x4, unsigned x2)
{
    encode(opcode, 40, 37);
    encode(x3, 35, 33);
    encode(x2, 32, 31);
    encode(x4, 30, 27);
    return emit();
}

unsigned Opcode_Encoder_offset9::F15(unsigned opcode, unsigned x, unsigned x6)
{
    encode(opcode, 40, 37);
    encode(x, 33, 33);
    encode(x6, 32, 27);
    return emit();
}

unsigned Opcode_Encoder_offset9::M1_M2_M4_M6_M7_M9_M11_M12_M13_M14_M16_M17(unsigned opcode, unsigned x6, unsigned hint, unsigned x, unsigned m)
{
    encode(opcode, 40, 37);
    encode(m, 36, 36);
    encode(x6, 35, 30);
    encode(hint, 29, 28);
    encode(x, 27, 27);
    return emit();
}

unsigned Opcode_Encoder_offset9::M3_M5_M8_M10_M15(unsigned opcode, unsigned x6, unsigned hint)
{
    encode(opcode, 40, 37);
    encode(x6, 35, 30);
    encode(hint, 29, 28);
    return emit();
}

unsigned Opcode_Encoder_offset9::M18_M19(unsigned opcode, unsigned m, unsigned x6, unsigned x)
{
    encode(opcode, 40, 37);
    encode(m, 36, 36);
    encode(x6, 35, 30);
    encode(x, 27, 27);
    return emit();
}

unsigned Opcode_Encoder_offset9::M20_M21_M22_M23_M34(unsigned opcode, unsigned x3)
{
    encode(opcode, 40, 37);
    encode(x3, 35, 33);
    return emit();
}

unsigned Opcode_Encoder_offset9::F1_F2_F3(unsigned opcode, unsigned x, unsigned sf_x2)
{
    encode(opcode, 40, 37);
    encode(x, 36, 36);
    encode(sf_x2, 35, 34);
    return emit();
}

unsigned Opcode_Encoder_offset9::F4(unsigned opcode, unsigned rb, unsigned sf, unsigned ra, unsigned ta)
{
    encode(opcode, 40, 37);
    encode(rb, 36, 36);
    encode(sf, 35, 34);
    encode(ra, 33, 33);
    encode(ta, 12, 12);
    return emit();
}

unsigned Opcode_Encoder_offset9::F5(unsigned opcode, unsigned fc2, unsigned fclass7c, unsigned ta)
{
    encode(opcode, 40, 37);
    encode(fc2, 34, 33);
    encode(fclass7c, 26, 20);
    encode(ta, 12, 12);
    return emit();
}

unsigned Opcode_Encoder_offset9::F6(unsigned opcode, unsigned q, unsigned sf, unsigned x)
{
    encode(opcode, 40, 37);
    encode(q, 36, 36);
    encode(sf, 35, 34);
    encode(x, 33, 33);
    return emit();
}

unsigned Opcode_Encoder_offset9::F8_F9_F10_F11_F12_F13(unsigned opcode, unsigned x, unsigned x6, unsigned sf)
{
    encode(opcode, 40, 37);
    encode(sf, 35, 34);
    encode(x, 33, 33);
    encode(x6, 32, 27);
    return emit();
}

unsigned Register_Encoder_offset6::R3_R2_R1(unsigned r3, unsigned r2, unsigned r1)
{
    encode(r3, 26, 20);
    encode(r2, 19, 13);
    encode(r1, 12, 6);
    return emit();
}

unsigned Register_Encoder_offset6::R3_R1(unsigned r3, unsigned r1)
{
    encode(r3, 26, 20);
    encode(r1, 12, 6);
    return emit();
}

unsigned Register_Encoder_offset6::R2_R1(unsigned r2, unsigned r1)
{
    encode(r2, 19, 13);
    encode(r1, 12, 6);
    return emit();
}

unsigned Register_Encoder_offset6::R3_R2(unsigned r3, unsigned r2)
{
    encode(r3, 26, 20);
    encode(r2, 19, 13);
    return emit();
}

unsigned Register_Encoder_offset6::cR3_R1(unsigned r3, unsigned r1)
{
    encode(r3, 21, 20);
    encode(r1, 12, 6);
    return emit();
}

unsigned Register_Encoder_offset6::R1(unsigned r1)
{
    encode(r1, 12, 6);
    return emit();
}

unsigned Register_Encoder_offset6::R2(unsigned r2)
{
    encode(r2, 19, 13);
    return emit();
}

unsigned Register_Encoder_offset6::R3(unsigned r3) 
{
    encode(r3,26,20);
    return emit();
}

unsigned Register_Encoder_offset6::P2_P1_R3_R2(unsigned p2, unsigned p1, unsigned r3, unsigned r2)
{
    encode(p2, 32, 27);
    encode(p1, 11, 6);
    encode(r3, 26, 20);
    encode(r2, 19, 13);
    return emit();
}

unsigned Register_Encoder_offset6::P2_P1_R3(unsigned p2, unsigned p1, unsigned r3)
{
    encode(p2, 32, 27);
    encode(p1, 11, 6);
    encode(r3, 26, 20);
    return emit();
}

unsigned Register_Encoder_offset6::B2_B1(unsigned b2, unsigned b1)
{
    encode(b2, 15, 13);
    encode(b1, 8, 6);
    return emit();
}

unsigned Register_Encoder_offset6::B1(unsigned b1)
{
    encode(b1, 8, 6);
    return emit();
}

unsigned Register_Encoder_offset6::I20_M20_M21(unsigned r2) {
    encode(r2,19,13);
    return emit();
}

unsigned Register_Encoder_offset6::F4_F3_F2_F1(unsigned f4, unsigned f3, unsigned f2, unsigned f1)
{
    encode(f4, 33, 27);
    encode(f3, 26, 20);
    encode(f2, 19, 13);
    encode(f1, 12, 6);
    return emit();
}

unsigned Register_Encoder_offset6::P2_P1_F3_F2(unsigned p2, unsigned p1, unsigned f3, unsigned f2)
{
    encode(p2, 32, 27);
    encode(p1, 11, 6);
    encode(f3, 26, 20);
    encode(f2, 19, 13);
    return emit();
}

unsigned Register_Encoder_offset6::P2_P1_F2(unsigned p2, unsigned p1, unsigned f2)
{
    encode(p2, 32, 27);
    encode(p1, 11, 6);
    encode(f2, 19, 13);
    return emit();
}

unsigned Register_Encoder_offset6::F3_F2_F1(unsigned f3, unsigned f2, unsigned f1)
{
    encode(f3, 26, 20);
    encode(f2, 19, 13);
    encode(f1, 12, 6);
    return emit();
}

unsigned Register_Encoder_offset6::P2_F3_F2_F1(unsigned p2, unsigned f3, unsigned f2, unsigned f1)
{
    encode(p2, 32, 27);
    encode(f3, 26, 20);
    encode(f2, 19, 13);
    encode(f1, 12, 6);
    return emit();
}

unsigned Register_Encoder_offset6::F2_F1(unsigned f2, unsigned f1)
{
    encode(f2, 19, 13);
    encode(f1, 12, 6);
    return emit();
}

unsigned Register_Encoder_offset6::B2_R1(unsigned b2, unsigned r1)
{
    encode(b2, 15, 13);
    encode(r1, 12, 6);
    return emit();
}

unsigned Register_Encoder_offset6::R2_B1(unsigned r2, unsigned b1)
{
    encode(r2, 19, 13);
    encode(b1, 8, 6);
    return emit();
}

unsigned Immediate_Encoder_offset6::A2(unsigned count2)
{
    // count2 = ct2d + 1
    encode(count2 - 1, 28, 27);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::A3_A8_I27_M30(unsigned imm8)
{
    // imm8 = sign_ext(s<<7 | imm7b, 8)
    encode((0x80 & imm8) >> 7, 36, 36);
    encode((0x7F & imm8), 19, 13);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::A4(unsigned imm14)
{
    // imm14 = sign_ext(s<<13 | imm6d<<7 | imm7b, 14)
    encode((0x2000 & imm14) >> 13, 36, 36);
    encode((0x1F80 & imm14) >> 7, 32, 27);
    encode((0x7F & imm14), 19, 13);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::A5(unsigned imm22)
{
    // imm22 = sign_ext(s<<21 | imm5c<<16 | imm9d<<7 | imm7b, 22)
    encode((0x200000 & imm22) >> 21, 36, 36);
    encode((0x1F0000 & imm22) >> 16, 26, 22);
    encode((0xFF80 & imm22) >> 7, 35, 27);
    encode((0x7F & imm22), 19, 13);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::I6(unsigned count5)
{
    // count5 = count5b
    encode(count5, 18, 14);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::I8(unsigned count5)
{
    // count5 = 31 - ccount5c
    encode(31-count5, 24, 20);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::I10(unsigned count6)
{
    // count6 = count6d
    encode(count6, 32, 27);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::I11(unsigned len6, unsigned pos6)
{
    // len6 = len6d + 1
    // pos6 = pos6b
    encode(len6 - 1, 32, 27);
    encode(pos6, 19, 14);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::I12(unsigned len6, unsigned pos6)
{
    // len6 = len6d + 1
    // pos6 = 63 - cpos6c
    encode(len6 - 1, 32, 27);
    encode(63 - pos6, 25, 20);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::I13(unsigned len6, unsigned pos6, unsigned imm8)
{
    // len6 = len6d + 1
    // pos6 = 63 - cpos6c
    // imm8 = sign_ext(s<<7|imm7b,8)
    encode(len6 - 1, 32, 27);
    encode(63 - pos6, 25, 20);
    encode((0x80 & imm8) >> 7, 36, 36);
    encode((0x7F & imm8), 19, 13);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::I14(unsigned len6, unsigned pos6, unsigned imm1)
{
    // len6 = len6d + 1
    // pos6 = 63 - cpos6b
    // imm1 = sign_ext(s,1)
    encode(len6 - 1, 32, 27);
    encode(63 - pos6, 19, 14);
    encode(0x1 & imm1, 36, 36);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::I15(unsigned len4, unsigned pos6)
{
    // len4 = len4d + 1
    // pos6 = 63 - cpos6d
    encode(len4 - 1, 30, 27);
    encode(63 - pos6, 36, 31);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::I18(unsigned upper_32, unsigned lower_32)
{
    // imm64 = i << 63 | imm41<<22 | ic<<21 | imm5c<<16 | imm9d<<7 | imm7b
    encode((0x80000000 & upper_32) >> 31, 36, 36);
    encode((0x200000 & lower_32) >> 21, 21, 21);
    encode((0x1F0000 & lower_32) >> 16, 26, 22);
    encode((0xFF80 & lower_32) >> 7, 35, 27);
    encode((0x7F & lower_32), 19, 13);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::I21(unsigned tag13, unsigned IP)
{
    // tag13 = IP + (sign_ext(timm9c, 9) << 4)
    unsigned imm9 = (tag13-IP) >> 4;
    encode((0x1FF & imm9), 32, 24);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::I23(unsigned mask17)
{
    // mask17 = sign_ext(s<<16 | mask8c<<8 | mask7a<<1, 17)
    encode((0x10000 & mask17) >> 16, 36, 36);
    encode((0xFF00 & mask17) >> 8, 31, 24);
    encode((0x00FE & mask17) >> 1, 12, 6);
    return emit();
}

unsigned Immediate_Encoder_offset6::I24(unsigned imm28)
{
    // imm44 = imm28 << 16 
    // imm44 = sign_ext(s<<43 | imm27a <<16, 44)
    encode((0x8000000 & imm28) >> 27, 36, 36);
    encode((0x7FFFFFF & imm28), 32, 6);
    return emit();
}

unsigned Immediate_Encoder_offset6::M3_M8_M15(unsigned imm9)
{
    // imm9 = sign_ext(s<<8 | i<<7 | imm7b, 9)
    encode((0x100 & imm9) >> 8, 36, 36);
    encode((0x80 & imm9) >> 7, 27, 27);
    encode((0x7F & imm9), 19, 13);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::M5_M10(unsigned imm9)
{
    // imm9 = sign_ext(s<<8 | i<<7 | imm7a, 9)
    encode((0x100 & imm9) >> 8, 36, 36);
    encode((0x80 & imm9)>> 7, 27, 27);
    encode((0x7F & imm9), 12, 6);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::M34(unsigned il, unsigned o, unsigned r)
{
    // il = sol
    // o = sof - sol
    // r = sor << 3
    encode(il, 26, 20);
    encode(il+o, 19, 13);
    encode(r >> 3, 30, 27);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::B1_B2_B3(unsigned target25, unsigned IP)
{
    // target25 = IP + (sign_ext(s<<20 | imm20b, 21) << 4)
    unsigned imm21 = (target25-IP) >> 4;
    encode((0x100000 & imm21) >> 20, 36, 36);
    encode((0xFFFFF & imm21), 32, 13);
    return emit(); 
}

unsigned Immediate_Encoder_offset6::I20_M20_M21(unsigned target25, unsigned IP)
{
    unsigned imm21 = (target25-IP) >> 4;
    encode ((0x100000 & imm21) >> 20, 36, 36);
    encode ((0x0fff80 & imm21) >> 7,  32, 20);
    encode ((0x00007f & imm21),       12,  6);
    return emit();
}

unsigned Immediate_Encoder_offset6::I19_M37_B9_F15(unsigned imm21)
{
    // imm21 = i << 20 | imm20a
    encode((0x100000 & imm21) >> 20, 36, 36);
    encode((0xFFFFF & imm21), 25, 6);
    return emit(); 
}


void Merced_Encoder::encode(unsigned opcode9, unsigned imm6, unsigned reg6, unsigned qp)
{
    assert(slot_num < 3);
    slot_encode(slot_num, opcode9, 9, 32); // 40:9
    slot_encode(slot_num, imm6, 6, 31); // 36:6
    slot_encode(slot_num, reg6, 6, 28); // 33:6
    slot_encode(slot_num, qp, 0, 6); // qp, default to 0
    slot_num++;
}

void Merced_Encoder::encode_long(unsigned opcode9, unsigned imm6, unsigned imm31x, unsigned imm10x,
                                 unsigned reg6, unsigned qp)
{
    assert(slot_num < 2);
    slot_encode(slot_num+1, opcode9, 9, 32); // 40:9
    slot_encode(slot_num+1, imm6, 6, 31); // 36:6
    slot_encode(slot_num+1, reg6, 6, 27); // 32:6
    slot_encode(slot_num+1, qp, 0, 6); // qp, default to 0
    slot_encode(slot_num, imm31x, 10, 31);
    slot_encode(slot_num, imm10x, 0, 10);
    slot_num += 2;
}



// The section numbers below (x.x.x.x and x.x.x) are reffering to the Intel
// Itanium Architecture Software Developer's Manual, volume 3, revision 2.1,
// October 2002.
void Merced_Encoder::encode_long_X3_X4(unsigned opcode4,
									   Branch_Prefetch_Hint ph,
									   Branch_Whether_Hint wh,
									   Branch_Dealloc_Hint dh,
									   unsigned b,
									   unsigned i,
									   uint64 imm39,
									   unsigned imm20b,
									   unsigned qp)
{ // 4.7.3
    assert(slot_num < 2);

	int slot1 = slot_num + 1;
	int slot2 = slot_num;

    slot_encode(slot1, opcode4, 37, 4);       // 37..40
    slot_encode(slot1, i, 36, 1);        // 36..36
    slot_encode(slot1, dh, 35, 1);       // 35..35
    slot_encode(slot1, wh, 33, 2);       // 33..34
    slot_encode(slot1, imm20b, 13, 20);  // 13..32
    slot_encode(slot1, ph, 12, 1);       // 12..12
    slot_encode(slot1, b, 6, 3);         //  6..8
    slot_encode(slot1, qp, 0, 6);        // qp

	// Encode imm39 in two chunks since slot_encode can't take a 64-bit
	// cnstant as an argument.
    slot_encode(slot2, (unsigned)((imm39 >> 19) & 0xFffFF), 21, 20);
    slot_encode(slot2, (unsigned)(imm39 & 0x7ffFF), 2, 19);

    slot_num += 2;
} //Merced_Encoder::encode_long_X3_X4



void Merced_Encoder::ipf_nop(EM_Syllable_Type tv, unsigned imm21)
{
    assert(imm21 < 1u<<21);
    switch (tv)
    {
    case ST_m:
        encode( // 4.4.9.4
            opc_enc.M24_M25_M26_M37(0, 0, 1, 0), 
            imm_enc.I19_M37_B9_F15(imm21), 
            0
            );
        break;
    case ST_i:
        encode( // 4.4.9.4
            opc_enc.I19_I22_I25_I26_I27_I28_I29_M29_M31(0, 0, 1), 
            imm_enc.I19_M37_B9_F15(imm21), 
            0
            );
        break;
    case ST_f:
        encode( // 4.6.9.1
            opc_enc.F15(0, 0, 1), 
            imm_enc.I19_M37_B9_F15(imm21), 
            0
            );
        break;
    case ST_b:
        encode( // 4.5.3.2
            opc_enc.B8_B9(2, 0), 
            imm_enc.I19_M37_B9_F15(imm21), 
            0
            );
        break;
    default:
        assert(0);
    }
}

void Merced_Encoder::ipf_add(unsigned dest, unsigned src1, unsigned src2, unsigned pred)
{
    encode( // 4.2.1.1
        opc_enc.A1_A2_A3_A4(8, 0, 0, 0, 0), 
        0, 
        reg_enc.R3_R2_R1(src2, src1, dest),
        pred
        );
}

void Merced_Encoder::ipf_sub(unsigned dest, unsigned src1, unsigned src2, unsigned pred)
{
    encode( // 4.2.1.1
        opc_enc.A1_A2_A3_A4(8, 0, 0, 1, 1), 
        0, 
        reg_enc.R3_R2_R1(src2, src1, dest),
        pred
        );
}

void Merced_Encoder::ipf_addp4(unsigned dest, unsigned src1, unsigned src2, unsigned pred)
{
    encode( // 4.2.1.1
        opc_enc.A1_A2_A3_A4(8, 0, 0, 2, 0), 
        0, 
        reg_enc.R3_R2_R1(src2, src1, dest),
        pred
        );
}

void Merced_Encoder::ipf_and(unsigned dest, unsigned src1, unsigned src2, unsigned pred)
{
    encode( // 4.2.1.1
        opc_enc.A1_A2_A3_A4(8, 0, 0, 3, 0), 
        0, 
        reg_enc.R3_R2_R1(src2, src1, dest),
        pred
        );
}

void Merced_Encoder::ipf_or(unsigned dest, unsigned src1, unsigned src2, unsigned pred)
{
    encode( // 4.2.1.1
        opc_enc.A1_A2_A3_A4(8, 0, 0, 3, 2), 
        0, 
        reg_enc.R3_R2_R1(src2, src1, dest),
        pred
        );
}

void Merced_Encoder::ipf_xor(unsigned dest, unsigned src1, unsigned src2, unsigned pred)
{
    encode( // 4.2.1.1
        opc_enc.A1_A2_A3_A4(8, 0, 0, 3, 3), 
        0, 
        reg_enc.R3_R2_R1(src2, src1, dest),
        pred
        );
}

void Merced_Encoder::ipf_shladd(unsigned dest, unsigned src1, int count, unsigned src2, unsigned pred)
{
    encode( // 4.2.1.2
        opc_enc.A1_A2_A3_A4(8, 0, 0, 4), 
        imm_enc.A2(count), 
        reg_enc.R3_R2_R1(src2, src1, dest),
        pred
        );
}

void Merced_Encoder::ipf_subi(unsigned dest, int imm, unsigned src, unsigned pred)
{
    encode( // 4.2.1.3
        opc_enc.A1_A2_A3_A4(8, 0, 0, 9, 1), 
        imm_enc.A3_A8_I27_M30(imm), 
        reg_enc.R3_R1(src, dest),
        pred
        );
}

void Merced_Encoder::ipf_andi(unsigned dest, int imm, unsigned src, unsigned pred)
{
    encode( // 4.2.1.3
        opc_enc.A1_A2_A3_A4(8, 0, 0, 0xB, 0), 
        imm_enc.A3_A8_I27_M30(imm), 
        reg_enc.R3_R1(src, dest),
        pred
        );
}

void Merced_Encoder::ipf_ori(unsigned dest, int imm, unsigned src, unsigned pred)
{
    encode( // 4.2.1.3
        opc_enc.A1_A2_A3_A4(8, 0, 0, 0xB, 2), 
        imm_enc.A3_A8_I27_M30(imm), 
        reg_enc.R3_R1(src, dest),
        pred
        );
}

void Merced_Encoder::ipf_xori(unsigned dest, int imm, unsigned src, unsigned pred)
{
    encode( // 4.2.1.3
        opc_enc.A1_A2_A3_A4(8, 0, 0, 0xB, 3), 
        imm_enc.A3_A8_I27_M30(imm), 
        reg_enc.R3_R1(src, dest),
        pred
        );
}

void Merced_Encoder::ipf_adds(unsigned dest, int imm14, unsigned src, unsigned pred)
{
    encode( // 4.2.1.3
        opc_enc.A1_A2_A3_A4(8, 2, 0), 
        imm_enc.A4(imm14), 
        reg_enc.R3_R1(src, dest),
        pred
        );
}

void Merced_Encoder::ipf_addl(unsigned dest, int imm22, unsigned src, unsigned pred)
{
    encode( // 4.2.1.3
        opc_enc.A5_I15(9), 
        imm_enc.A5(imm22), 
        reg_enc.R3_R1(src, dest),
        pred
        );
}

void Merced_Encoder::ipf_addp4i(unsigned dest, int imm14, unsigned src, unsigned pred)
{
    encode( // 4.2.1.3
        opc_enc.A1_A2_A3_A4(8, 3, 0), 
        imm_enc.A4(imm14), 
        reg_enc.R3_R1(src, dest),
        pred
        );
}

void Merced_Encoder::ipf_cmp(Int_Comp_Rel xcr, Compare_Extension cx, unsigned xp1, unsigned xp2, unsigned xr2, unsigned xr3, bool cmp4, unsigned pred)
{
    unsigned x2, tb = 0, ta = 0, c = 0;
    unsigned opcode=0, p1=xp1, p2=xp2, r2=xr2, r3=xr3;
    Int_Comp_Rel cr;

    if (cx==cmp_none || cx==cmp_unc)
    {
        switch (xcr)
        {   //switch predicates
        case icmp_ne:
        case icmp_ge:
        case icmp_geu:
        case icmp_le:
        case icmp_leu:
            p1 = xp2; p2 = xp1; break;
	    default: break;
        }
        switch (xcr)
        {   // switch registers
        case icmp_le:
        case icmp_gt:
        case icmp_leu:
        case icmp_gtu:
            r2 = xr3;
            r3 = xr2;
            break;
	    default: break;
        }
        switch (xcr)
        {   // reassign cr
        case icmp_lt:
        case icmp_le:
        case icmp_gt:
        case icmp_ge:
            cr = icmp_lt;
            opcode = 0xC;
            break;
        case icmp_ltu:
        case icmp_leu:
        case icmp_gtu:
        case icmp_geu:
            cr = icmp_ltu;
            opcode = 0xD;
            break;
        case icmp_eq:
        case icmp_ne:
            cr = icmp_eq;
            opcode = 0xE;
            break;
        default:
            assert(0); // compare type not covered
        }
        switch (cx)
        {   // assign bits
        case cmp_none:
            tb = 0; ta = 0; c = 0; break;
        case cmp_unc:
            tb = 0; ta = 0; c = 1; break;
        default:
            assert(0);
        }
    }
    else if (cx==cmp_and || cx==cmp_or || cx==cmp_or_andcm) // parallel compares
    {
        cr = xcr;
        if (r3==0 && (xcr!=icmp_eq && xcr!=icmp_ne))
        {   // if r3 is register 0 then switch
            assert(r2!=0); // compare zero with zero?
            switch (xcr)
            {
            case icmp_lt:
                cr = icmp_gt; 
                break;
            case icmp_le:
                cr = icmp_ge; 
                break;
            case icmp_gt:
                cr = icmp_lt; 
                break;
            case icmp_ge:
                cr = icmp_le; 
                break;
	    default: assert(0);
            }
            // switch registers
            r2 = xr3;
            r3 = xr2;
        }
        switch (cx)
        {   // assign opcode
        case cmp_and:
            opcode = 0xC;
            break;
        case cmp_or:
            opcode = 0xD;
            break;
        case cmp_or_andcm:
            opcode = 0xE;
            break;
        default:
            assert(0); // compare type not covered
        }
        switch (cr)
        {   // assign bits
        case icmp_eq:
            tb = 0; ta = 1; c = 0; break;
        case icmp_ne:
            tb = 0; ta = 1; c = 1; break;
        case icmp_gt:
            tb = 1; ta = 0; c = 0; break;
        case icmp_le:
            tb = 1; ta = 0; c = 1; break;
        case icmp_ge:
            tb = 1; ta = 1; c = 0; break;
        case icmp_lt:
            tb = 1; ta = 1; c = 1; break;
        default:
            assert(0);
        }
    }
    else
        assert(0);
    if (cmp4)
        x2 = 1;
    else
        x2 = 0;
    assert(opcode);
    encode( // 21.2.2
        opc_enc.A6_A7(opcode, tb, x2, ta, c), 
        0,
        reg_enc.P2_P1_R3_R2(p2, p1, r3, r2),
        pred
        );
}

void Merced_Encoder::ipf_cmpz(Int_Comp_Rel cr, Compare_Extension cx, unsigned xp1, unsigned xp2, unsigned r3, bool cmp4, unsigned pred)
{
    ipf_cmp(cr, cx, xp1, xp2, 0, r3, cmp4, pred);
}

void Merced_Encoder::ipf_cmpi(Int_Comp_Rel xcr, Compare_Extension cx, unsigned xp1, unsigned xp2, int ximm, unsigned xr3, bool cmp4, unsigned pred)
{
    unsigned x2, ta = 0, c = 0;
    unsigned opcode=0, p1=xp1, p2=xp2, r3=xr3;
    int imm = ximm;
    Int_Comp_Rel cr;

    if (cx==cmp_none || cx==cmp_unc)
    {
        switch (xcr)
        {   //switch predicates
        case icmp_ne:
        case icmp_ge:
        case icmp_geu:
        case icmp_gt:
        case icmp_gtu:
            p1 = xp2; p2 = xp1;
            break;
	    default: 
            break; // do nothing
        }
        switch (xcr)
        {   // subtract 1 from the immediate
        case icmp_le:
        case icmp_gt:
        case icmp_leu:
        case icmp_gtu:
            imm = ximm - 1;
            break;
	    default: 
            break; // do nothing
        }
        switch (xcr)
        {   // reassign cr
        case icmp_lt:
        case icmp_le:
        case icmp_gt:
        case icmp_ge:
            cr = icmp_lt;
            opcode = 0xC;
            break;
        case icmp_ltu:
        case icmp_leu:
        case icmp_gtu:
        case icmp_geu:
            cr = icmp_ltu;
            opcode = 0xD;
            break;
        case icmp_eq:
        case icmp_ne:
            cr = icmp_eq;
            opcode = 0xE;
            break;
        default:
            assert(0); // compare type not covered
        }
        switch (cx)
        {   // assign bits
        case cmp_none:
            ta = 0; c = 0; break;
        case cmp_unc:
            ta = 0; c = 1; break;
        default:
            assert(0);
        }
    }
    else if (cx==cmp_and || cx==cmp_or || cx==cmp_or_andcm) // parallel compares
    {
        cr = xcr;
        switch (cx)
        {   // assign opcode
        case cmp_and:
            opcode = 0xC;
            break;
        case cmp_or:
            opcode = 0xD;
            break;
        case cmp_or_andcm:
            opcode = 0xE;
            break;
        default:
            assert(0); // compare type not covered
        }
        switch (cr)
        {   // assign bits
        case icmp_eq:
            ta = 1; c = 0; break;
        case icmp_ne:
            ta = 1; c = 1; break;
        default:
            assert(0);
        }
    }
    else
        assert(0);
    if (cmp4)
        x2 = 3;
    else
        x2 = 2;
    assert(opcode);
    encode( // 21.2.2.3
        opc_enc.A8(opcode, x2, ta, c), 
        imm_enc.A3_A8_I27_M30((unsigned)imm),
        reg_enc.P2_P1_R3(p2, p1, r3),
        pred
        );
}

void Merced_Encoder::ipf_movl(unsigned dest, unsigned upper_32, unsigned lower_32, unsigned pred)
{
    encode_long( // 4.3.4
        opc_enc.I18(6, 0), 
        imm_enc.I18(upper_32, lower_32), 
        (upper_32 & 0x7FFFFFFF),
        (lower_32 & 0xFFC00000) >> 22,
        reg_enc.R1(dest),
        pred
        );
}



void Merced_Encoder::ipf_brl_call(Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b1, uint64 imm64, unsigned pred)
{
	uint64 temp = imm64;
	unsigned i;
	uint64 imm39;
	unsigned imm20b;
	temp >>= 4;  // ignore 4 least significant bits
	imm20b = (unsigned)(temp & 0xfFfFf);
	temp >>= 20;
	imm39 = temp & 0x7fFFFFffff;
	temp >>= 39;
	i = (unsigned)(temp & 1);
	temp >>= 1;
	assert(temp == 0);
	encode_long_X3_X4(0xd, ph, wh, dh, b1, i, imm39, imm20b, pred);
} //Merced_Encoder::ipf_brl_call

void Merced_Encoder::rewrite_brl_call_target(const unsigned char * oldBundle, uint64 newTarget) {
    slot_num = 0;
    for (int index = 0; index < IPF_INSTRUCTION_LEN; index++)
        _char_value[index] = oldBundle[index];
	newTarget >>= 4;  // ignore 4 least significant bits
	unsigned imm20b = (unsigned)(newTarget & 0xfFfFf);
	newTarget >>= 20;
	uint64 imm39 = newTarget & 0x7fFFFFffff;
	newTarget >>= 39;
	unsigned i = (unsigned)(newTarget & 1);
	newTarget >>= 1;
	assert(newTarget == 0);
	int slot1 = 2;
	int slot2 = 1;
    // Encode i
    slot_rewrite(slot1, i, 36, 1);        // 36..36
    // Encode imm20b
    slot_rewrite(slot1, imm20b, 13, 20);  // 13..32
	// Encode imm39 in two chunks since slot_encode can't take a 64-bit
	// cnstant as an argument.
    slot_rewrite(slot2, (unsigned)((imm39 >> 19) & 0xFffFF), 21, 20);
    slot_rewrite(slot2, (unsigned)(imm39 & 0x7ffFF), 2, 19);
}


void Merced_Encoder::ipf_brl_cond(Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, uint64 imm64, unsigned pred)
{
	uint64 temp = imm64;
	unsigned i;
	uint64 imm39;
	unsigned imm20b;
	temp >>= 4;  // ignore 4 least significant bits
	imm20b = (unsigned)(temp & 0xfFfFf);
	temp >>= 20;
	imm39 = temp & 0x7fFFFFffff;
	temp >>= 39;
	i = (unsigned)(temp & 1);
	temp >>= 1;
	assert(temp == 0);
	encode_long_X3_X4(0xC, ph, wh, dh, 0, i, imm39, imm20b, pred);
} //Merced_Encoder::ipf_brl_cond

void Merced_Encoder::ipf_shr(unsigned dest, unsigned src1, unsigned src2, unsigned pred)
{
    encode( // 4.3.1.5
        opc_enc.I5_I7(7, 1, 0, 1, 0, 0, 2), 
        0, 
        reg_enc.R3_R2_R1(src1, src2, dest),
        pred
        );
}

void Merced_Encoder::ipf_shru(unsigned dest, unsigned src1, unsigned src2, unsigned pred)
{
    encode( // 4.3.1.5
        opc_enc.I5_I7(7, 1, 0, 1, 0, 0, 0), 
        0, 
        reg_enc.R3_R2_R1(src1, src2, dest),
        pred
        );
}

void Merced_Encoder::ipf_shl(unsigned dest, unsigned src1, unsigned src2, unsigned pred)
{
    encode( // 4.3.1.7
        opc_enc.I5_I7(7, 1, 0, 1, 0, 1, 0), 
        0, 
        reg_enc.R3_R2_R1(src2, src1, dest),
        pred
        );
}

void Merced_Encoder::ipf_extr(unsigned dest, unsigned src, int pos6, int len6, unsigned pred)
{
    encode( // 4.3.2.2
        opc_enc.I10_I11(5, 1, 0, 1), 
        imm_enc.I11(len6, pos6), 
        reg_enc.R3_R1(src, dest),
        pred
        );
}

void Merced_Encoder::ipf_extru(unsigned dest, unsigned src, int pos6, int len6, unsigned pred)
{
    encode( // 4.3.2.2
        opc_enc.I10_I11(5, 1, 0, 0), 
        imm_enc.I11(len6, pos6), 
        reg_enc.R3_R1(src, dest),
        pred
        );
}

void Merced_Encoder::ipf_depz(unsigned dest, unsigned src, int pos6, int len6, unsigned pred)
{
    encode( // 4.3.2.3
        opc_enc.I12_I13_I14(5, 1, 1, 0), 
        imm_enc.I12(len6, pos6), 
        reg_enc.R2_R1(src, dest),
        pred
        );
}

void Merced_Encoder::ipf_depiz(unsigned dest, int imm8, int pos6, int len6, unsigned pred)
{
    encode( // 4.3.2.4
        opc_enc.I12_I13_I14(5, 1, 1, 1), 
        imm_enc.I13(len6, pos6, imm8), 
        reg_enc.R1(dest),
        pred
        );
}

void Merced_Encoder::ipf_depi(unsigned dest, int imm1, unsigned src, int pos6, int len6, unsigned pred)
{
    encode( // 4.3.2.5
        opc_enc.I12_I13_I14(5, 3, 1), 
        imm_enc.I14(len6, pos6, imm1), 
        reg_enc.R3_R1(src, dest),
        pred
        );
}

void Merced_Encoder::ipf_dep(unsigned dest, unsigned r2, unsigned r3, int pos6, int len4, unsigned pred)
{
    encode( // 4.3.2.6
        opc_enc.A5_I15(4), 
        imm_enc.I15(len4, pos6), 
        reg_enc.R3_R2_R1(r3, r2, dest),
        pred
        );
}

void Merced_Encoder::ipf_sxt(Sxt_Size size, unsigned dest, unsigned src, unsigned pred)
{
    encode( // 4.3.9
        opc_enc.I19_I22_I25_I26_I27_I28_I29_M29_M31(0, 0, (0x14+size)), // 0x14, 0x15, 0x16
        0, 
        reg_enc.R3_R1(src, dest),
        pred
        );
}

void Merced_Encoder::ipf_zxt(Sxt_Size size, unsigned dest, unsigned src, unsigned pred)
{
    encode( // 4.3.9
        opc_enc.I19_I22_I25_I26_I27_I28_I29_M29_M31(0, 0, (0x10+size)), // 0x10, 0x11, 0x12
        0, 
        reg_enc.R3_R1(src, dest),
        pred
        );
}

/// ******** remember, for counted loop, must force qp = 0.  See 4.5.1.2
void Merced_Encoder::ipf_br(Branch_Type btype, Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned target25, unsigned pred)
{
    unsigned curr_ip = _offset;
    encode( // 4.5.1.1 and 4.5.1.2
        opc_enc.B1_B2_B3_B4_B5(4, dh, wh, ph), 
        imm_enc.B1_B2_B3(target25, curr_ip), 
        reg_enc.B1(btype), // btyte occupies the same space as b1, so be tricky
        pred
        );
}

void Merced_Encoder::ipf_brcall(Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b1, unsigned target25, unsigned pred)
{
    unsigned curr_ip = _offset;
    encode( // 4.5.1.3
        opc_enc.B1_B2_B3_B4_B5(5, dh, wh, ph), 
        imm_enc.B1_B2_B3(target25, curr_ip), 
        reg_enc.B1(b1),
        pred
        );
}

void Merced_Encoder::ipf_bri(Branch_Type btype, Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b2, unsigned pred)
{
    encode( // 4.5.1.4
        opc_enc.B1_B2_B3_B4_B5(0, dh, wh, ph, 0x20), 
        0,
        reg_enc.B2_B1(b2, btype),
        pred
        );
}

// specialized for return
void Merced_Encoder::ipf_brret(Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b2, unsigned pred)
{
    encode( // 4.5.1.4
        opc_enc.B1_B2_B3_B4_B5(0, dh, wh, ph, 0x21), 
        0,
        reg_enc.B2_B1(b2, 4),
        pred
        );
}

void Merced_Encoder::ipf_bricall(Branch_Prefetch_Hint ph, Branch_Whether_Hint wh, Branch_Dealloc_Hint dh, unsigned b1, unsigned b2, unsigned pred)
{
    encode( // 4.5.1.5
        opc_enc.B1_B2_B3_B4_B5(1, dh, wh, ph), 
        0,
        reg_enc.B2_B1(b2, b1),
        pred
        );
}

void Merced_Encoder::ipf_chk_s_i(unsigned src, unsigned target25, unsigned pred) {
    unsigned curr_ip = _offset;
    encode( // I20
        opc_enc.I20_I23_I24_M20_M21(0,1),
        imm_enc.I20_M20_M21(target25, curr_ip),
        reg_enc.I20_M20_M21(src),
        pred
        );
}

void Merced_Encoder::ipf_chk_s_m(unsigned src, unsigned target25, unsigned pred) {
    unsigned curr_ip = _offset;
    encode( // M20
        opc_enc.I20_I23_I24_M20_M21(1,1),
        imm_enc.I20_M20_M21(target25, curr_ip),
        reg_enc.I20_M20_M21(src),
        pred
        );
}

void Merced_Encoder::ipf_chk_f_s(unsigned src, unsigned target25, unsigned pred) {
    unsigned curr_ip = _offset;
    encode( // M21
        opc_enc.I20_I23_I24_M20_M21(1,3),
        imm_enc.I20_M20_M21(target25, curr_ip),
        reg_enc.I20_M20_M21(src),
        pred
        );
}

void Merced_Encoder::ipf_ld(Int_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned pred)
{
    unsigned x6 = (flag << 2) + size;
    encode( // 4.4.1.1
        opc_enc.M1_M2_M4_M6_M7_M9_M11_M12_M13_M14_M16_M17(4, x6, hint, 0, 0),
        0,
        reg_enc.R3_R1(addrreg, dest),
        pred
        );
}

void Merced_Encoder::ipf_ld_inc_reg(Int_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned inc_reg, unsigned pred)
{
    unsigned x6 = (flag << 2) + size;
    encode( // 4.4.1.2
        opc_enc.M1_M2_M4_M6_M7_M9_M11_M12_M13_M14_M16_M17(4, x6, hint, 0, 1), 
        0,
        reg_enc.R3_R2_R1(addrreg, inc_reg, dest),
        pred
        );
}

void Merced_Encoder::ipf_ld_inc_imm(Int_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned inc_imm, unsigned pred)
{
    unsigned x6 = (flag << 2) + size;
    encode( // 4.4.1.3
        opc_enc.M3_M5_M8_M10_M15(5, x6, hint), 
        imm_enc.M3_M8_M15(inc_imm),
        reg_enc.R3_R1(addrreg, dest),
        pred
        );
}

void Merced_Encoder::ipf_st(Int_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src, unsigned pred)
{
    unsigned x6 = ((flag << 2) + size) + 0x30;
    // make sure hint != 2
    encode( // 4.4.1.4
        opc_enc.M1_M2_M4_M6_M7_M9_M11_M12_M13_M14_M16_M17(4, x6, hint, 0, 0), 
        0,
        reg_enc.R3_R2(addrreg, src),
        pred
        );
}

void Merced_Encoder::ipf_st_inc_imm(Int_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src, unsigned inc_imm, unsigned pred)
{
    unsigned x6 = ((flag << 2) + size) + 0x30;
    // make sure hint != 2
    encode( // 4.4.1.5
        opc_enc.M3_M5_M8_M10_M15(5, x6, hint), 
        imm_enc.M5_M10(inc_imm),
        reg_enc.R3_R2(addrreg, src),
        pred
        );
}

void Merced_Encoder::ipf_lfetch(bool exclusive, bool fault, Lfetch_Hint hint, unsigned addrreg, unsigned pred)
{
    unsigned x6 = 0x2C;
    if (exclusive)
        x6++;
    if (fault)
        x6 += 2;
    encode ( // M13
        opc_enc.M1_M2_M4_M6_M7_M9_M11_M12_M13_M14_M16_M17(6,x6,hint,0,0),
        0,
        reg_enc.R3(addrreg),
        pred
        );
}

void Merced_Encoder::ipf_setf(FReg_Convert form, unsigned fdest, unsigned src, unsigned pred)
{
    encode( // 4.4.4.1
        opc_enc.M18_M19(6, 0, (0x1C+form), 1), 
        0,
        reg_enc.R2_R1(src, fdest),
        pred
        );
}

void Merced_Encoder::ipf_getf(FReg_Convert form, unsigned dest, unsigned fsrc, unsigned pred)
{
    encode( // 4.4.4.2
        opc_enc.M18_M19(4, 0, (0x1C+form), 1), 
        0,
        reg_enc.R2_R1(fsrc, dest),
        pred
        );
}

void Merced_Encoder::ipf_fma(Float_Precision pc, Float_Status_Field sf, unsigned dest, unsigned src1, unsigned src2, unsigned src3, unsigned pred)
{
    unsigned opcode = (pc & 0x2) ? 0x9 : 0x8; // either 9 or 8
    unsigned x = (pc & 0x1) ? 1 : 0; // either 1 or 0
    encode( // 4.6.1.1
        opc_enc.F1_F2_F3(opcode, x, sf),
        0,
        reg_enc.F4_F3_F2_F1(src2, src1, src3, dest),
        pred
        );
}

void Merced_Encoder::ipf_fnma(Float_Precision pc, Float_Status_Field sf, unsigned dest, unsigned src1, unsigned src2, unsigned src3, unsigned pred)
{
    unsigned opcode = (pc & 0x2) ? 0xD : 0xC; // either D or C
    unsigned x = (pc & 0x1) ? 1 : 0; // either 1 or 0
    encode( // 4.6.1.1
        opc_enc.F1_F2_F3(opcode, x, sf),
        0,
        reg_enc.F4_F3_F2_F1(src2, src1, src3, dest),
        pred
        );
}

void Merced_Encoder::ipf_fms(Float_Precision pc, Float_Status_Field sf, unsigned dest, unsigned src1, unsigned src2, unsigned src3, unsigned pred)
{
    unsigned opcode = (pc & 0x2) ? 0xB : 0xA; // either B or A
    unsigned x = (pc & 0x1) ? 1 : 0; // either 1 or 0
    encode( // 4.6.1.1
        opc_enc.F1_F2_F3(opcode, x, sf),
        0,
        reg_enc.F4_F3_F2_F1(src2, src1, src3, dest),
        pred
        );
}

void Merced_Encoder::ipf_frcpa(Float_Status_Field sf, unsigned dest, unsigned p2, unsigned src1, unsigned src2, unsigned pred)
{
    unsigned opcode = 0;
    unsigned x = 1;
    unsigned q = 0;
    encode(
        opc_enc.F6(opcode, q, sf, x),
        0,
        reg_enc.P2_F3_F2_F1(p2, src2, src1, dest),
        pred
        );
}

void Merced_Encoder::ipf_fmerge(Float_Merge fm, unsigned dest, unsigned src1, unsigned src2, unsigned pred)
{
    encode( // 4.6.1.1
        opc_enc.F8_F9_F10_F11_F12_F13(0, 0, (0x10+fm)),
        0,
        reg_enc.F3_F2_F1(src2, src1, dest),
        pred
        );
}

void Merced_Encoder::ipf_fcmp(Float_Comp_Rel xcr, Compare_Extension cx, unsigned xp1, unsigned xp2, unsigned xf2, unsigned xf3, unsigned pred)
{
    unsigned ra = 0, rb = 0, sf, ta = 0, p1=xp1, p2=xp2, f2=xf2, f3=xf3;
    Float_Comp_Rel cr;
    switch (xcr)
    {   //switch predicates
    case fcmp_neq:
    case fcmp_nlt:
    case fcmp_nle:
    case fcmp_ngt:
    case fcmp_nge:
    case fcmp_ord:
        p1 = xp2; p2 = xp1;break;
    default: break;
    }
    switch (xcr)
    {   // switch registers
    case fcmp_gt:
    case fcmp_ge:
    case fcmp_ngt:
    case fcmp_nge:
        f2 = xf3;
        f3 = xf2;
        break;
    default: break;
    }
    switch (xcr)
    {   // reassign cr
    case fcmp_eq:
    case fcmp_neq:
        cr = fcmp_eq;
        ra = 0; rb = 0;
        break;
    case fcmp_lt:
    case fcmp_gt:
    case fcmp_nlt:
    case fcmp_ngt:
        cr = fcmp_lt;
        ra = 0; rb = 1;
        break;
    case fcmp_le:
    case fcmp_ge:
    case fcmp_nle:
    case fcmp_nge:
        cr = fcmp_le;
        ra = 1; rb = 0;
        break;
    case fcmp_unord:
    case fcmp_ord:
        cr = fcmp_unord;
        ra = 1; rb = 1;
        break;
    default:
        assert(0); // compare type not covered
    }
    switch (cx)
    {   // assign ta
    case cmp_none:
        ta = 0; break;
    case cmp_unc:
        ta = 1; break;
    default:
        assert(0);
    }
    sf = 0;
    encode( // 4.6.3.1
        opc_enc.F4(4, rb, sf, ra, ta),
        0,
        reg_enc.P2_P1_F3_F2(p2, p1, f3, f2),
        pred
        );
}

void Merced_Encoder::ipf_fclass(Compare_Extension cx, unsigned p1, unsigned p2, unsigned f2, unsigned fclass9, unsigned pred)
{
    unsigned fc2 = fclass9 & 3;
    unsigned fclass7c = fclass9 >> 2;
    unsigned ta = 0;
    switch (cx)
    {   // assign ta
    case cmp_none:
        ta = 0; break;
    case cmp_unc:
        ta = 1; break;
    default:
        assert(0);
    }
    encode( // 4.6.3.2
        opc_enc.F5(5, fc2, fclass7c, ta),
        0,
        reg_enc.P2_P1_F2(p2, p1, f2),
        pred
        );
}

void Merced_Encoder::ipf_fcvt_fx(FFix_Convert fc, Float_Status_Field sf, unsigned dest, unsigned src, unsigned pred)
{
    encode( // 4.6.7.1
        opc_enc.F8_F9_F10_F11_F12_F13(0, 0, (0x18+fc), sf),
        0,
        reg_enc.F2_F1(src, dest),
        pred
        );
}

void Merced_Encoder::ipf_fcvt_xf(unsigned dest, unsigned src, unsigned pred)
{
    encode( // 4.6.7.2
        opc_enc.F8_F9_F10_F11_F12_F13(0, 0, 0x1C, 0),
        0,
        reg_enc.F2_F1(src, dest),
        pred
        );
}

void Merced_Encoder::ipf_ldf(Float_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned pred)
{
    unsigned x6;
    if (flag == mem_ld_fill)
        x6 = 0x1B;
    else
        x6 = (flag << 2) + size;
    encode( // 4.4.1.6
        opc_enc.M1_M2_M4_M6_M7_M9_M11_M12_M13_M14_M16_M17(6, x6, hint, 0, 0), 
        0,
        reg_enc.R3_R1(addrreg, dest),
        pred
        );
}

void Merced_Encoder::ipf_ldf_inc_reg(Float_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned inc_reg, unsigned pred)
{
    unsigned x6;
    if (flag == mem_ld_fill)
        x6 = 0x1B;
    else
        x6 = (flag << 2) + size;
    encode( // 4.4.1.7
        opc_enc.M1_M2_M4_M6_M7_M9_M11_M12_M13_M14_M16_M17(6, x6, hint, 0, 1),
        0,
        reg_enc.R3_R2_R1(addrreg, inc_reg, dest),
        pred
        );
}

void Merced_Encoder::ipf_ldf_inc_imm(Float_Mem_Size size, Ld_Flag flag, Mem_Hint hint, unsigned dest, unsigned addrreg, unsigned inc_imm, unsigned pred)
{
    unsigned x6;
    if (flag == mem_ld_fill)
        x6 = 0x1B;
    else
        x6 = (flag << 2) + size;
    encode( // 4.4.1.8
        opc_enc.M3_M5_M8_M10_M15(7, x6, hint),
        imm_enc.M3_M8_M15(inc_imm),
        reg_enc.R3_R1(addrreg, dest),
        pred
        );
}

void Merced_Encoder::ipf_stf(Float_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src, unsigned pred)
{
    unsigned x6;
    if (flag == mem_st_spill)
        x6 = 0x3B;
    else
        x6 = ((flag << 2) + size) + 0x30;
    // make sure hint != 2
    encode( // 4.4.1.9
        opc_enc.M1_M2_M4_M6_M7_M9_M11_M12_M13_M14_M16_M17(6, x6, hint, 0, 0), 
        0,
        reg_enc.R3_R2(addrreg, src),
        pred
        );
}

void Merced_Encoder::ipf_stf_inc_imm(Float_Mem_Size size, St_Flag flag, Mem_Hint hint, unsigned addrreg, unsigned src, unsigned inc_imm, unsigned pred)
{
    unsigned x6;
    if (flag == mem_st_spill)
        x6 = 0x3B;
    else
        x6 = ((flag << 2) + size) + 0x30;
    // make sure hint != 2
    encode( // 4.4.1.10
        opc_enc.M3_M5_M8_M10_M15(7, x6, hint), 
        imm_enc.M5_M10(inc_imm),
        reg_enc.R3_R2(addrreg, src),
        pred
        );
}

void Merced_Encoder::ipf_alloc(unsigned dest, unsigned i, unsigned l, unsigned o, unsigned r)
{
    encode( // 4.4.9.1
        opc_enc.M20_M21_M22_M23_M34(1, 6),
        imm_enc.M34(i+l, o, r),
        reg_enc.R1(dest)
        );
}

void Merced_Encoder::ipf_mtbr(unsigned bdest, unsigned src, Branch_Predict_Whether_Hint wh, bool ret, unsigned offset, unsigned pred)
{
    unsigned curr_ip = _offset;
    unsigned x = (ret ? 1 : 0);
    unsigned ih = 0; // not important
    unsigned tag13 = curr_ip + offset;
    encode( // 4.3.6.1
        opc_enc.I21(0, 7, ih, x, wh, 0, 0), 
        imm_enc.I21(tag13, curr_ip),
        reg_enc.R2_B1(src, bdest),
        pred
        );
}

void Merced_Encoder::ipf_mfbr(unsigned dest, unsigned bsrc, unsigned pred)
{
    encode( // 4.3.6.2
        opc_enc.I19_I22_I25_I26_I27_I28_I29_M29_M31(0, 0, 0x31),
        0,
        reg_enc.B2_R1(bsrc, dest),
        pred
        );
}

void Merced_Encoder::ipf_mtap(EM_Application_Register adest, unsigned src, unsigned pred)
{
    unsigned opcode = 0, x6 = 0;
    switch (adest)
    {
    case AR_kr0:
    case AR_kr1:
    case AR_kr2:
    case AR_kr3:
    case AR_kr4:
    case AR_kr5:
    case AR_kr6:
    case AR_kr7:
    case AR_rsc:
    case AR_bsp:
    case AR_bspstore:
    case AR_rnat:
    case AR_fcr:
    case AR_eflag:
    case AR_csd:
    case AR_ssd:
    case AR_cflg:
    case AR_fsr:
    case AR_fir:
    case AR_fdr:
    case AR_ccv:
    case AR_unat:
    case AR_fpsr:
    case AR_itc:
        // M template
        opcode = 1; 
        x6 = 0x2a;
        break;
    case AR_pfs:
    case AR_lc:
    case AR_ec:
        // I template
        opcode = 0; 
        x6 = 0x2a;
        break;
    default:
        assert(0);
    }
    encode( // 4.3.8.1 and 4.4.7.1
        opc_enc.I19_I22_I25_I26_I27_I28_I29_M29_M31(opcode, 0, x6),
        0,
        reg_enc.R3_R2(adest, src),
        pred
        );
}

void Merced_Encoder::ipf_mfap(unsigned dest, EM_Application_Register asrc, unsigned pred)
{
    unsigned opcode = 0, x6 = 0;
    switch (asrc)
    {
    case AR_kr0:
    case AR_kr1:
    case AR_kr2:
    case AR_kr3:
    case AR_kr4:
    case AR_kr5:
    case AR_kr6:
    case AR_kr7:
    case AR_rsc:
    case AR_bsp:
    case AR_bspstore:
    case AR_rnat:
    case AR_fcr:
    case AR_eflag:
    case AR_csd:
    case AR_ssd:
    case AR_cflg:
    case AR_fsr:
    case AR_fir:
    case AR_fdr:
    case AR_ccv:
    case AR_unat:
    case AR_fpsr:
    case AR_itc:
        // M template
        opcode = 1; 
        x6 = 0x22;
        break;
    case AR_pfs:
    case AR_lc:
    case AR_ec:
        // I template
        opcode = 0; 
        x6 = 0x32;
        break;
    default:
        assert(0);
    }
    encode( // 4.3.8.3 and 4.4.7.3
        opc_enc.I19_I22_I25_I26_I27_I28_I29_M29_M31(opcode, 0, x6),
        0,
        reg_enc.R3_R1(asrc, dest),
        pred
        );
}

void Merced_Encoder::ipf_movip(unsigned dest, unsigned pred)
{
    encode( // 4.3.7.3
        opc_enc.I19_I22_I25_I26_I27_I28_I29_M29_M31(0, 0, 0x30),
        0,
        reg_enc.R1(dest),
        pred
        );
}

void Merced_Encoder::ipf_xma(unsigned dest, unsigned src1, unsigned src2, unsigned src3, Xla_Flag flag, unsigned pred)
{
    unsigned opcode = 0xE;
    unsigned x      = 1;
    unsigned x2     = flag;
    encode( // 4.6.1.1
        opc_enc.F1_F2_F3(opcode, x, x2),
        0,
        reg_enc.F4_F3_F2_F1(src2, src1, src3, dest),
        pred
        );
}

void Merced_Encoder::ipf_cmpxchg(Int_Mem_Size size, Cmpxchg_Flag flag, Mem_Hint hint, unsigned dest, unsigned r3, unsigned r2, unsigned pred)
{
    unsigned x6 = (flag << 2) + size;
    encode( // 4.4.1.1
        opc_enc.M1_M2_M4_M6_M7_M9_M11_M12_M13_M14_M16_M17(4, x6, hint, 1, 0),
        0,
        reg_enc.R3_R2_R1(r3, r2, dest),
        pred
        );
}

void Merced_Encoder::ipf_mtpr(unsigned src1, unsigned mask17, unsigned pred)
{
    encode( // 4.3.6.1
        opc_enc.I20_I23_I24_M20_M21(0, 3),
        imm_enc.I23(mask17),
        reg_enc.R2(src1),
        pred
        );
}

void Merced_Encoder::ipf_mtpr_rot(unsigned imm28, unsigned pred)
{
    encode( // 4.3.6.2
        opc_enc.I20_I23_I24_M20_M21(0, 2),
        imm_enc.I24(imm28),
        0,
        pred);
}


void Merced_Encoder::ipf_mfpr(unsigned dest, unsigned pred)
{
    encode( // 4.3.6.3
        opc_enc.I25(0, 0, 0x33),
        0,
        reg_enc.R1(dest),
        pred);
}



void Merced_Encoder::ipf_cover()
{
    encode( // 4.5.3.1
        opc_enc.B8_B9(0, 2), 
        0,
        0,
        0);
} //Merced_Encoder::ipf_cover



void Merced_Encoder::ipf_flushrs()
{
    encode( // 4.4.6.2
    //unsigned M24_M25_M26_M37(unsigned opcode, unsigned x3, unsigned x4, unsigned x2);
        opc_enc.M24_M25_M26_M37(0, 0, 0xc, 0), 
        0,
        0,
        0);
} //Merced_Encoder::ipf_flushrs

void Merced_Encoder::ipf_mf(unsigned pred) {
    encode( // M24
        opc_enc.M24_M25_M26_M27_M37(0, 0, 2, 2),
        0,
        0,
        pred);
}



