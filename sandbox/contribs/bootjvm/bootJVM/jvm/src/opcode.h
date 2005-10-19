#ifndef _opcode_h_included_
#define _opcode_h_included_

/*!
 * @file opcode.h
 *
 * @brief Implementation of <em>The Java Virtual Machine Specification,
 * version 2, Section 9, Operation Mnemonics by Opcode</em>.
 *
 *
 * @section Control
 *
 * \$URL$ \$Id$
 *
 * Copyright 2005 The Apache Software Foundation
 * or its licensors, as applicable.
 *
 * Licensed under the Apache License, Version 2.0 ("the License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * @version \$LastChangedRevision$
 *
 * @date \$LastChangedDate$
 *
 * @author \$LastChangedBy$
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

ARCH_COPYRIGHT_APACHE(opcode, h, "$URL$ $Id$");


/*!
 * @name Java Virtual Machine operation codes, standard set
 *
 */ 

/*@{ */ /* Begin grouped definitions */

#define OPCODE_00_NOP             0x00 /**< 00 (0x00) nop */
#define OPCODE_01_ACONST_NULL     0x01 /**< 01 (0x01) aconst_null */
#define OPCODE_02_ICONST_M1       0x02 /**< 02 (0x02) iconst_m1 */
#define OPCODE_03_ICONST_0        0x03 /**< 03 (0x03) iconst_0 */
#define OPCODE_04_ICONST_1        0x04 /**< 04 (0x04) iconst_1 */
#define OPCODE_05_ICONST_2        0x05 /**< 05 (0x05) iconst_2 */
#define OPCODE_06_ICONST_3        0x06 /**< 06 (0x06) iconst_3 */
#define OPCODE_07_ICONST_4        0x07 /**< 07 (0x07) iconst_4 */
#define OPCODE_08_ICONST_5        0x08 /**< 08 (0x08) iconst_5 */
#define OPCODE_09_LCONST_0        0x09 /**< 09 (0x09) lconst_0 */
#define OPCODE_0A_LCONST_1        0x0a /**< 10 (0x0a) lconst_1 */
#define OPCODE_0B_FCONST_0        0x0b /**< 11 (0x0b) fconst_0 */
#define OPCODE_0C_FCONST_1        0x0c /**< 12 (0x0c) fconst_1 */
#define OPCODE_0D_FCONST_2        0x0d /**< 13 (0x0d) fconst_2 */
#define OPCODE_0E_DCONST_0        0x0e /**< 14 (0x0e) dconst_0 */
#define OPCODE_0F_DCONST_1        0x0f /**< 15 (0x0f) dconst_1 */
#define OPCODE_10_BIPUSH          0x10 /**< 16 (0x10) bipush */
#define OPCODE_11_SIPUSH          0x11 /**< 17 (0x11) sipush */
#define OPCODE_12_LDC             0x12 /**< 18 (0x12) Ldc */
#define OPCODE_13_LDC_W           0x13 /**< 19 (0x13) ldc_w */
#define OPCODE_14_LDC2_W          0x14 /**< 20 (0x14) ldc2_w */
#define OPCODE_15_ILOAD           0x15 /**< 21 (0x15) iload */
#define OPCODE_16_LLOAD           0x16 /**< 22 (0x16) lload */
#define OPCODE_17_FLOAD           0x17 /**< 23 (0x17) fload */
#define OPCODE_18_DLOAD           0x18 /**< 24 (0x18) dload */
#define OPCODE_19_ALOAD           0x19 /**< 25 (0x19) aload */
#define OPCODE_1A_ILOAD_0         0x1a /**< 26 (0x1a) iload_0 */
#define OPCODE_1B_ILOAD_1         0x1b /**< 27 (0x1b) iload_1 */
#define OPCODE_1C_ILOAD_2         0x1c /**< 28 (0x1c) iload_2 */
#define OPCODE_1D_ILOAD_3         0x1d /**< 29 (0x1d) iload_3 */
#define OPCODE_1E_LLOAD_0         0x1e /**< 30 (0x1e) lload_0 */
#define OPCODE_1F_LLOAD_1         0x1f /**< 31 (0x1f) lload_1 */
#define OPCODE_20_LLOAD_2         0x20 /**< 32 (0x20) lload_2 */
#define OPCODE_21_LLOAD_3         0x21 /**< 33 (0x21) lload_3 */
#define OPCODE_22_FLOAD_0         0x22 /**< 34 (0x22) fload_0 */
#define OPCODE_23_FLOAD_1         0x23 /**< 35 (0x23) fload_1 */
#define OPCODE_24_FLOAD_2         0x24 /**< 36 (0x24) fload_2 */
#define OPCODE_25_FLOAD_3         0x25 /**< 37 (0x25) fload_3 */
#define OPCODE_26_DLOAD_0         0x26 /**< 38 (0x26) dload_0 */
#define OPCODE_27_DLOAD_1         0x27 /**< 39 (0x27) dload_1 */
#define OPCODE_28_DLOAD_2         0x28 /**< 40 (0x28) dload_2 */
#define OPCODE_29_DLOAD_3         0x29 /**< 41 (0x29) dload_3 */
#define OPCODE_2A_ALOAD_0         0x2a /**< 42 (0x2a) aload_0 */
#define OPCODE_2B_ALOAD_1         0x2b /**< 43 (0x2b) aload_1 */
#define OPCODE_2C_ALOAD_2         0x2c /**< 44 (0x2c) aload_2 */
#define OPCODE_2D_ALOAD_3         0x2d /**< 45 (0x2d) aload_3 */
#define OPCODE_2E_IALOAD          0x2e /**< 46 (0x2e) iaload */
#define OPCODE_2F_LALOAD          0x2f /**< 47 (0x2f) laload */
#define OPCODE_30_FALOAD          0x30 /**< 48 (0x30) faload */
#define OPCODE_31_DALOAD          0x31 /**< 49 (0x31) daload */
#define OPCODE_32_AALOAD          0x32 /**< 50 (0x32) aaload */
#define OPCODE_33_BALOAD          0x33 /**< 51 (0x33) baload */
#define OPCODE_34_CALOAD          0x34 /**< 52 (0x34) caload */
#define OPCODE_35_SALOAD          0x35 /**< 53 (0x35) saload */
#define OPCODE_36_ISTORE          0x36 /**< 54 (0x36) istore */
#define OPCODE_37_LSTORE          0x37 /**< 55 (0x37) lstore */
#define OPCODE_38_FSTORE          0x38 /**< 56 (0x38) fstore */
#define OPCODE_39_DSTORE          0x39 /**< 57 (0x39) dstore */
#define OPCODE_3A_ASTORE          0x3a /**< 58 (0x3a) astore */
#define OPCODE_3B_ISTORE_0        0x3b /**< 59 (0x3b) istore_0 */
#define OPCODE_3C_ISTORE_1        0x3c /**< 60 (0x3c) istore_1 */
#define OPCODE_3D_ISTORE_2        0x3d /**< 61 (0x3d) istore_2 */
#define OPCODE_3E_ISTORE_3        0x3e /**< 62 (0x3e) istore_3 */
#define OPCODE_3F_LSTORE_0        0x3f /**< 63 (0x3f) lstore_0 */
#define OPCODE_40_LSTORE_1        0x40 /**< 64 (0x40) lstore_1 */
#define OPCODE_41_LSTORE_2        0x41 /**< 65 (0x41) lstore_2 */
#define OPCODE_42_LSTORE_3        0x42 /**< 66 (0x42) lstore_3 */
#define OPCODE_43_FSTORE_0        0x43 /**< 67 (0x43) fstore_0 */
#define OPCODE_44_FSTORE_1        0x44 /**< 68 (0x44) fstore_1 */
#define OPCODE_45_FSTORE_2        0x45 /**< 69 (0x45) fstore_2 */
#define OPCODE_46_FSTORE_3        0x46 /**< 70 (0x46) fstore_3 */
#define OPCODE_47_DSTORE_0        0x47 /**< 71 (0x47) dstore_0 */
#define OPCODE_48_DSTORE_1        0x48 /**< 72 (0x48) dstore_1 */
#define OPCODE_49_DSTORE_2        0x49 /**< 73 (0x49) dstore_2 */
#define OPCODE_4A_DSTORE_3        0x4a /**< 74 (0x4a) dstore_3 */
#define OPCODE_4B_ASTORE_0        0x4b /**< 75 (0x4b) astore_0 */
#define OPCODE_4C_ASTORE_1        0x4c /**< 76 (0x4c) astore_1 */
#define OPCODE_4D_ASTORE_2        0x4d /**< 77 (0x4d) astore_2 */
#define OPCODE_4E_ASTORE_3        0x4e /**< 78 (0x4e) astore_3 */
#define OPCODE_4F_IASTORE         0x4f /**< 79 (0x4f) iastore */
#define OPCODE_50_LASTORE         0x50 /**< 80 (0x50) lastore */
#define OPCODE_51_FASTORE         0x51 /**< 81 (0x51) fastore */
#define OPCODE_52_DASTORE         0x52 /**< 82 (0x52) dastore */
#define OPCODE_53_AASTORE         0x53 /**< 83 (0x53) aastore */
#define OPCODE_54_BASTORE         0x54 /**< 84 (0x54) bastore */
#define OPCODE_55_CASTORE         0x55 /**< 85 (0x55) castore */
#define OPCODE_56_SASTORE         0x56 /**< 86 (0x56) sastore */
#define OPCODE_57_POP             0x57 /**< 87 (0x57) Pop */
#define OPCODE_58_POP2            0x58 /**< 88 (0x58) pop2 */
#define OPCODE_59_DUP             0x59 /**< 089 (0x59) dup */
#define OPCODE_5A_DUP_X1          0x5a /**< 090 (0x5a) dup_x1 */
#define OPCODE_5B_DUP_X2          0x5b /**< 091 (0x5b) dup_x2 */
#define OPCODE_5C_DUP2            0x5C /**< 092 (0x5C) dup2 */
#define OPCODE_5D_DUP2_X1         0x5d /**< 093 (0x5d) dup2_x1 */
#define OPCODE_5E_DUP2_X2         0x5e /**< 094 (0x5e) dup2_x2 */
#define OPCODE_5F_SWAP            0x5f /**< 095 (0x5f) swap */
#define OPCODE_60_IADD            0x60 /**< 096 (0x60) iadd */
#define OPCODE_61_LADD            0x61 /**< 097 (0x61) ladd */
#define OPCODE_62_FADD            0x62 /**< 098 (0x62) fadd */
#define OPCODE_63_DADD            0x63 /**< 099 (0x63) dadd */
#define OPCODE_64_ISUB            0x64 /**< 100 (0x64) isub */
#define OPCODE_65_LSUB            0x65 /**< 101 (0x65) lsub */
#define OPCODE_66_FSUB            0x66 /**< 102 (0x66) fsub */
#define OPCODE_67_DSUB            0x67 /**< 103 (0x67) dsub */
#define OPCODE_68_IMUL            0x68 /**< 104 (0x68) imul */
#define OPCODE_69_LMUL            0x69 /**< 105 (0x69) lmul */
#define OPCODE_6A_FMUL            0x6a /**< 106 (0x6a) fmul */
#define OPCODE_6B_DMUL            0x6b /**< 107 (0x6b) dmul */
#define OPCODE_6C_IDIV            0x6c /**< 108 (0x6c) idiv */
#define OPCODE_6D_LDIV            0x6d /**< 109 (0x6d) ldiv */
#define OPCODE_6E_FDIV            0x6e /**< 110 (0x6e) fdiv */
#define OPCODE_6F_DDIV            0x6f /**< 111 (0x6f) ddiv */
#define OPCODE_70_IREM            0x70 /**< 112 (0x70) irem */
#define OPCODE_71_LREM            0x71 /**< 113 (0x71) lrem */
#define OPCODE_72_FREM            0x72 /**< 114 (0x72) frem */
#define OPCODE_73_DREM            0x73 /**< 115 (0x73) drem */
#define OPCODE_74_INEG            0x74 /**< 116 (0x74) ineg */
#define OPCODE_75_LNEG            0x75 /**< 117 (0x75) lneg */
#define OPCODE_76_FNEG            0x76 /**< 118 (0x76) fneg */
#define OPCODE_77_DNEG            0x77 /**< 119 (0x77) dneg */
#define OPCODE_78_ISHL            0x78 /**< 120 (0x78) ishl */
#define OPCODE_79_LSHL            0x79 /**< 121 (0x79) lshl */
#define OPCODE_7A_ISHR            0x7a /**< 122 (0x7a) ishr */
#define OPCODE_7B_LSHR            0x7b /**< 123 (0x7b) lshr */
#define OPCODE_7C_IUSHR           0x7c /**< 124 (0x7c) iushr */
#define OPCODE_7D_LUSHR           0x7d /**< 125 (0x7d) lushr */
#define OPCODE_7E_IAND            0x7e /**< 126 (0x7e) iand */
#define OPCODE_7F_LAND            0x7f /**< 127 (0x7f) land */
#define OPCODE_80_IOR             0x80 /**< 128 (0x80) ior */
#define OPCODE_81_LOR             0x81 /**< 129 (0x81) lor */
#define OPCODE_82_IXOR            0x82 /**< 130 (0x82) ixor */
#define OPCODE_83_LXOR            0x83 /**< 131 (0x83) lxor */
#define OPCODE_84_IINC            0x84 /**< 132 (0x84) iinc */
#define OPCODE_85_I2L             0x85 /**< 133 (0x85) i2l */
#define OPCODE_86_I2F             0x86 /**< 134 (0x86) i2f */
#define OPCODE_87_I2D             0x87 /**< 135 (0x87) i2d */
#define OPCODE_88_L2I             0x88 /**< 136 (0x88) l2i */
#define OPCODE_89_L2F             0x89 /**< 137 (0x89) l2f */
#define OPCODE_8A_L2D             0x8a /**< 138 (0x8a) l2d */
#define OPCODE_8B_F2I             0x8b /**< 139 (0x8b) f2i */
#define OPCODE_8C_F2L             0x8c /**< 140 (0x8c) f2l */
#define OPCODE_8D_F2D             0x8d /**< 141 (0x8d) f2d */
#define OPCODE_8E_D2I             0x8e /**< 142 (0x8e) d2i */
#define OPCODE_8F_D2L             0x8f /**< 143 (0x8f) d2l */
#define OPCODE_90_D2F             0x90 /**< 144 (0x90) d2f */
#define OPCODE_91_I2B             0x91 /**< 145 (0x91) i2b */
#define OPCODE_92_I2C             0x92 /**< 146 (0x92) i2c */
#define OPCODE_93_I2S             0x93 /**< 147 (0x93) i2s */
#define OPCODE_94_LCMP            0x94 /**< 148 (0x94) lcmp */
#define OPCODE_95_FCMPL           0x95 /**< 149 (0x95) fcmpl */
#define OPCODE_96_FCMPG           0x96 /**< 150 (0x96) fcmpg */
#define OPCODE_97_DCMPL           0x97 /**< 151 (0x97) dcmpl */
#define OPCODE_98_DCMPG           0x98 /**< 152 (0x98) dcmpg */
#define OPCODE_99_IFEQ            0x99 /**< 153 (0x99) ifeq */
#define OPCODE_9A_IFNE            0x9a /**< 154 (0x9a) ifne */
#define OPCODE_9B_IFLT            0x9b /**< 155 (0x9b) iflt */
#define OPCODE_9C_IFGE            0x9c /**< 156 (0x9c) ifge */
#define OPCODE_9D_IFGT            0x9d /**< 157 (0x9d) ifgt */
#define OPCODE_9E_IFLE            0x9e /**< 158 (0x9e) ifle */
#define OPCODE_9F_IF_ICMPEQ       0x9f /**< 159 (0x9f) if_icmpeq */
#define OPCODE_A0_IF_ICMPNE       0xa0 /**< 160 (0xa0) if_icmpne */
#define OPCODE_A1_IF_ICMPLT       0xa1 /**< 161 (0xa1) if_icmplt */
#define OPCODE_A2_IF_ICMPGE       0xa2 /**< 162 (0xa2) if_icmpge */
#define OPCODE_A3_IF_ICMPGT       0xa3 /**< 163 (0xa3) if_icmpgt */
#define OPCODE_A4_IF_ICMPLE       0xa4 /**< 164 (0xa4) if_icmple */
#define OPCODE_A5_IF_ACMPEQ       0xa5 /**< 165 (0xa5) if_acmpeq */
#define OPCODE_A6_IF_ACMPNE       0xa6 /**< 166 (0xa6) if_acmpne */
#define OPCODE_A7_GOTO            0xa7 /**< 167 (0xa7) goto  */
#define OPCODE_A8_JSR             0xa8 /**< 168 (0xa8) jsr */
#define OPCODE_A9_RET             0xa9 /**< 169 (0xa9) ret */
#define OPCODE_AA_TABLESWITCH     0xaa /**< 170 (0xaa) tableswitch */
#define OPCODE_AB_LOOKUPSWITCH    0xab /**< 171 (0xab) lookupswitch */
#define OPCODE_AC_IRETURN         0xac /**< 172 (0xac) ireturn */
#define OPCODE_AD_LRETURN         0xad /**< 173 (0xad) lreturn */
#define OPCODE_AE_FRETURN         0xae /**< 174 (0xae) freturn */
#define OPCODE_AF_DRETURN         0xaf /**< 175 (0xaf) dreturn */
#define OPCODE_B0_ARETURN         0xb0 /**< 176 (0xb0) areturn */
#define OPCODE_B1_RETURN          0xb1 /**< 177 (0xb1) return */
#define OPCODE_B2_GETSTATIC       0xb2 /**< 178 (0xb2) getstatic */
#define OPCODE_B3_PUTSTATIC       0xb3 /**< 179 (0xb3) putstatic */
#define OPCODE_B4_GETFIELD        0xb4 /**< 180 (0xb4) getfield */
#define OPCODE_B5_PUTFIELD        0xb5 /**< 181 (0xb5) putfield */
#define OPCODE_B6_INVOKEVIRTUAL   0xb6 /**< 182 (0xb6) invokevirtual */
#define OPCODE_B7_INVOKESPECIAL   0xb7 /**< 183 (0xb7) invokespecial */
#define OPCODE_B8_INVOKESTATIC    0xb8 /**< 184 (0xb8) invokestatic */
#define OPCODE_B9_INVOKEINTERFACE 0xb9 /**< 185 (0xb9)invokeinterface*/
#define OPCODE_BA_XXXUNUSEDXXX1   0xba /**< 186 (0xba) xxxunusedxxx1 */
#define OPCODE_BB_NEW             0xbb /**< 187 (0xbb) new */
#define OPCODE_BC_NEWARRAY        0xbc /**< 188 (0xbc) newarray */
#define OPCODE_BD_ANEWARRAY       0xbd /**< 189 (0xbd) anewarray */
#define OPCODE_BE_ARRAYLENGTH     0xbe /**< 190 (0xbe) arraylength */
#define OPCODE_BF_ATHROW          0xbf /**< 191 (0xbf) athrow */
#define OPCODE_C0_CHECKCAST       0xc0 /**< 192 (0xc0) checkcast */
#define OPCODE_C1_INSTANCEOF      0xc1 /**< 193 (0xc1) instanceof */
#define OPCODE_C2_MONITORENTER    0xc2 /**< 194 (0xc2) monitorenter */
#define OPCODE_C3_MONITOREXIT     0xc3 /**< 195 (0xc3) monitorexit */
#define OPCODE_C4_WIDE            0xc4 /**< 196 (0xc4) wide */
#define OPCODE_C5_MULTIANEWARRAY  0xc5 /**< 197 (0xc5) multianewarray*/
#define OPCODE_C6_IFNULL          0xc6 /**< 198 (0xc6) ifnull */
#define OPCODE_C7_IFNONNULL       0xc7 /**< 199 (0xc7) ifnonnull */
#define OPCODE_C8_GOTO_W          0xc8 /**< 200 (0xc8) goto_w */
#define OPCODE_C9_JSR_W           0xc9 /**< 201 (0xc9) jsr_w */
#define OPCODE_CA_BREAKPOINT      0xca /**< 202 (0xca) breakpoint */

/*@} */ /* End of grouped definitions */


/*!
 * @name Java Virtual Machine operation codes, reserved and unused set
 *
 */ 

/*@{ */ /* Begin grouped definitions */

#define OPCODE_CB_UNUSED          0xcb /**< 203 (0xcb) unused */
#define OPCODE_CC_UNUSED          0xcc /**< 204 (0xcc) unused */
#define OPCODE_CD_UNUSED          0xcd /**< 205 (0xcd) unused */
#define OPCODE_CE_UNUSED          0xce /**< 206 (0xce) unused */
#define OPCODE_CF_UNUSED          0xcf /**< 207 (0xcf) unused */

#define OPCODE_D0_UNUSED          0xd0 /**< 208 (0xd0) unused */
#define OPCODE_D1_UNUSED          0xd1 /**< 209 (0xd1) unused */
#define OPCODE_D2_UNUSED          0xd2 /**< 210 (0xd2) unused */
#define OPCODE_D3_UNUSED          0xd3 /**< 211 (0xd3) unused */
#define OPCODE_D4_UNUSED          0xd4 /**< 212 (0xd4) unused */
#define OPCODE_D5_UNUSED          0xd5 /**< 213 (0xd5) unused */
#define OPCODE_D6_UNUSED          0xd6 /**< 214 (0xd6) unused */
#define OPCODE_D7_UNUSED          0xd7 /**< 215 (0xd7) unused */
#define OPCODE_D8_UNUSED          0xd8 /**< 216 (0xd8) unused */
#define OPCODE_D9_UNUSED          0xd9 /**< 217 (0xd9) unused */
#define OPCODE_DA_UNUSED          0xda /**< 218 (0xda) unused */
#define OPCODE_DB_UNUSED          0xdb /**< 219 (0xdb) unused */
#define OPCODE_DC_UNUSED          0xdc /**< 220 (0xdc) unused */
#define OPCODE_DD_UNUSED          0xdd /**< 221 (0xdd) unused */
#define OPCODE_DE_UNUSED          0xde /**< 222 (0xde) unused */
#define OPCODE_DF_UNUSED          0xdf /**< 223 (0xdf) unused */

#define OPCODE_E0_UNUSED          0xe0 /**< 224 (0xe0) unused */
#define OPCODE_E1_UNUSED          0xe1 /**< 225 (0xe1) unused */
#define OPCODE_E2_UNUSED          0xe2 /**< 226 (0xe2) unused */
#define OPCODE_E3_UNUSED          0xe3 /**< 227 (0xe3) unused */
#define OPCODE_E4_UNUSED          0xe4 /**< 228 (0xe4) unused */
#define OPCODE_E5_UNUSED          0xe5 /**< 229 (0xe5) unused */
#define OPCODE_E6_UNUSED          0xe6 /**< 230 (0xe6) unused */
#define OPCODE_E7_UNUSED          0xe7 /**< 231 (0xe7) unused */
#define OPCODE_E8_UNUSED          0xe8 /**< 232 (0xe8) unused */
#define OPCODE_E9_UNUSED          0xe9 /**< 233 (0xe9) unused */
#define OPCODE_EA_UNUSED          0xea /**< 234 (0xea) unused */
#define OPCODE_EB_UNUSED          0xeb /**< 234 (0xeb) unused */
#define OPCODE_EC_UNUSED          0xec /**< 235 (0xec) unused */
#define OPCODE_ED_UNUSED          0xed /**< 237 (0xed) unused */
#define OPCODE_EE_UNUSED          0xee /**< 238 (0xee) unused */
#define OPCODE_EF_UNUSED          0xef /**< 239 (0xef) unused */

#define OPCODE_F0_UNUSED          0xf0 /**< 240 (0xf0) unused */
#define OPCODE_F1_UNUSED          0xf1 /**< 241 (0xf1) unused */
#define OPCODE_F2_UNUSED          0xf2 /**< 242 (0xf2) unused */
#define OPCODE_F3_UNUSED          0xf3 /**< 243 (0xf3) unused */
#define OPCODE_F4_UNUSED          0xf4 /**< 244 (0xf4) unused */
#define OPCODE_F5_UNUSED          0xf5 /**< 245 (0xf5) unused */
#define OPCODE_F6_UNUSED          0xf6 /**< 246 (0xf6) unused */
#define OPCODE_F7_UNUSED          0xf7 /**< 247 (0xf7) unused */
#define OPCODE_F8_UNUSED          0xf8 /**< 248 (0xf8) unused */
#define OPCODE_F9_UNUSED          0xf9 /**< 249 (0xf9) unused */
#define OPCODE_FA_UNUSED          0xfa /**< 250 (0xfa) unused */
#define OPCODE_FB_UNUSED          0xfb /**< 251 (0xfb) unused */
#define OPCODE_FC_UNUSED          0xfc /**< 252 (0xfc) unused */
#define OPCODE_FD_UNUSED          0xfd /**< 253 (0xfd) unused */

/*@} */ /* End of grouped definitions */

/*!
@name Java Virtual Machine operation codes, implementation-dependent set
 *
 */ 

/*@{ */ /* Begin grouped definitions */

#define OPCODE_FE_IMPDEP1         0xfe /**< 254 (0xfe) impdep1 */
#define OPCODE_FF_IMPDEP2         0xff /**< 255 (0xff) impdep2 */

/*@} */ /* End of grouped definitions */


/* Prototypes and selected externs for functions in 'opcode.c' */

extern rboolean opcode_calling_java_lang_linkageerror;

extern rvoid opcode_load_run_throwable(rchar           *pThrowableEvent,
                                       jvm_thread_index thridx);

extern rboolean opcode_run(jvm_thread_index thridx,
                           rboolean check_timeslice);

#endif /* _opcode_h_included_ */

/* EOF */
