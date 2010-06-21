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
 * @author Intel, Konstantin M. Anisimov, Igor V. Chebykin
 *
 */

#ifndef IPFENCODER_H_
#define IPFENCODER_H_

#include <vector>
#include <bitset>
#include "Type.h"
#include "IpfType.h"

using namespace std;

namespace Jitrino {
namespace IPF {

#define IPF_BUNDLE_SIZE    16                      // 16 bytes

//============================================================================//
// This file is based on following docs:
//   Intel Itanium Architecture
//   Software Developers Manual
//   Volume 1: Application Architecture
//   Volume 2: System Architecture
//   Volume 3: Instruction Set Reference
//   Revision 2.1
//   October 2002
//============================================================================//

// Type of instruction, Description Execution Unit Type
// IT_I...IT_X are used in bundle mask and occupies 1 byte
enum InstructionType {
    IT_I   = 0x01,         // Non-ALU integer, I-unit slot type
    IT_M   = 0x02,         // Memory,          M-unit slot type
    IT_A   = IT_I | IT_M,  // Integer ALU,     I/M-unit slot type
    IT_F   = 0x04,         // Floating-point,  F-unit slot type
    IT_B   = 0x08,         // Branch,          B-unit slot type
    IT_L   = 0x10,         // Extended, occupies 2 slots: 2nd ant 3d
                           //     L+X Major Opcodes 0 - 7 execute on an I-unit. 
                           //     L+X Major Opcodes 8 - F execute on a B-unit.
    IT_X   = 0x20,         // Continue of IT_L
    IT_ANY = 0xFF,         // ANY type above

    // pseudo-op
    IT_PSEUDO_OP = 0x100,  
    IT_IGNORE_OP = 0x200,  // the inst is ignored in all passes (stopping, bundling, etc.)
    
    // pseudo-op special: explicit set of unit, something else for chk.s
    IT_EXPLICIT_IMBFX = 0x1000,  // explicitly set compliter (I,M,B,F,X) due to slot type
    IT_EXPLICIT_IM    = 0x2000,  // explicitly set compliter (I,M) due to slot type
    
    // positon in instruction group
    IT_GF = 0x10000, // first in group
    IT_GL = 0x20000 // last in group
};

// List of instructions
//   - prefixed with INST_
//   - dots in names are replaced by underscores
//   - there is a difference from "Volume 3: Instruction Set Reference":
//        few instructions have variable operands count, but we havn't
//        so there are additional instructions for those cases
enum InstCode {
    INST_INVALID = -1,
    INST_FIRST   = 0,           // MUST BE 0
    INST_ADD     = INST_FIRST,      
    INST_ADDS,                    // imm-14
    INST_ADDL,                    // imm-22
    INST_ADDP4,
    INST_ALLOC,
    INST_AND,
    INST_ANDCM,
    INST_BR_FIRST,              // start of continuous group of branch instructions
    INST_BR=INST_BR_FIRST,
    INST_BRL,
    INST_BRP,                    // ip_relative_form, indirect_form
    INST_BRP_RET,                // return_form, indirect_form
    INST_BR_LAST=INST_BRP_RET,  // end of continuous group of branch instructions
    INST_BREAK,
    INST_BSW_0,
    INST_BSW_1,
    INST_CHK_S,
    INST_CHK_S_I,
    INST_CHK_S_M,
    INST_CHK_A,
    INST_CLRRRB,
    INST_CLRRRB_PR,
    INST_CMP,
    INST_CMP4,
    INST_CMPXCHG,
    INST_CMP8XCHG16,
    INST_COVER,
    INST_CZX1_L,                // one_byte_form, left_form
    INST_CZX1_R,                // one_byte_form, right_form
    INST_CZX2_L,                // two_byte_form, left_form
    INST_CZX2_R,                // two_byte_form, right_form
    INST_DEP,
    INST_DEP_Z,
    INST_EPC,
    INST_EXTR,
    INST_EXTR_U,
    INST_FABS,
    INST_FADD,
    INST_FAMAX,
    INST_FAMIN,
    INST_FAND,
    INST_FANDCM,
    INST_FC,                    // invalidate_line_form
    INST_FC_I,                    // instruction_cache_coherent_form
    INST_FCHKF,
    INST_FCLASS,
    INST_FCLRF,
    INST_FCMP,
    INST_FCVT_FX,                // signed_form
    INST_FCVT_FX_TRUNC,            // signed_form, trunc_form
    INST_FCVT_FXU,                // unsigned_form
    INST_FCVT_FXU_TRUNC,        // unsigned_form, trunc_form
    INST_FCVT_XF,
    INST_FCVT_XUF,
    INST_FETCHADD4,                // four_byte_form
    INST_FETCHADD8,                // eight_byte_form
    INST_FLUSHRS,
    INST_FMA,
    INST_FMAX,
    INST_FMERGE_NS,                // Floating-point Merge Negative Sign
    INST_FMERGE_S,                // Floating-point Merge Sign Operation
    INST_FMERGE_SE,                // Floating-point Merge Sign and Exponent
    INST_FMIN,
    INST_FMIX_L,                // Floating-point Mix Left
    INST_FMIX_R,                // Floating-point Mix Right
    INST_FMIX_LR,                // Floating-point Mix Left-Right
    INST_FMPY,
    INST_FMS,
    INST_FNEG,
    INST_FNEGABS,
    INST_FNMA,
    INST_FNMPY,
    INST_FNORM,
    INST_FOR,
    INST_FPABS,
    INST_FPACK,
    INST_FPAMAX,
    INST_FPAMIN,
    INST_FPCMP,
    INST_FPCVT_FX,
    INST_FPCVT_FX_TRUNC,
    INST_FPCVT_FXU,
    INST_FPCVT_FXU_TRUNC,
    INST_FPMA,
    INST_FPMAX,
    INST_FPMERGE_NS,            // Floating-point Parallel Merge Negative Sign
    INST_FPMERGE_S,                // Floating-point Parallel Merge Sign
    INST_FPMERGE_SE,            // Floating-point Parallel Merge Sign and Exponent Operation
    INST_FPMIN,
    INST_FPMPY,
    INST_FPMS,
    INST_FPNEG,
    INST_FPNEGABS,
    INST_FPNMA,
    INST_FPNMPY,
    INST_FPRCPA,
    INST_FPRSQRTA,
    INST_FRCPA,
    INST_FRSQRTA,
    INST_FSELECT,
    INST_FSETC,
    INST_FSUB,
    INST_FSWAP,                    // Floating-point Swap
    INST_FSWAP_NL,                // Floating-point Swap Negate Left
    INST_FSWAP_NR,                // Floating-point Swap Negate Right
    INST_FSXT_L,                // Floating-point Sign Extend Left
    INST_FSXT_R,                // Floating-point Sign Extend Right
    INST_FWB,
    INST_FXOR,
    INST_GETF_S,
    INST_GETF_D,
    INST_GETF_EXP,
    INST_GETF_SIG,
    INST_HINT,
    INST_INVALA,                // All entries in ALAT are invalidated
    INST_INVALA_E,                // One entry in ALAT is invalidated
    INST_ITC_I,                    // itc.i instruction_form
    INST_ITC_D,                    // itc.d data_form
    INST_ITR_I,                    // instruction_form
    INST_ITR_D,                    // data_form
    INST_LD_FIRST,
    INST_LD = INST_LD_FIRST,
    INST_LD16,
    INST_LD16_ACQ,
    INST_LD8_FILL,
    INST_LDF,                   // 4,8,10 bytes Single precision 
    INST_LDF8,                  // integer form
    INST_LDF_FILL,              // fill form
    INST_LDFPS,
    INST_LDFPD,
    INST_LDFP8,
    INST_LD_LAST = INST_LDFP8,
    INST_LFETCH,
    INST_LFETCH_EXCL,
    INST_LOADRS,
    INST_MF,
    INST_MF_A,
    INST_MIX1_L,                // one_byte_form, left_form 
    INST_MIX2_L,                // two_byte_form, left_form 
    INST_MIX4_L,                // four_byte_form, left_form
    INST_MIX1_R,                // one_byte_form, right_form
    INST_MIX2_R,                // two_byte_form, right_form
    INST_MIX4_R,                // four_byte_form, right_form
    INST_MOV,                   // there are many incarnations of mov, 
                                // so parse it like pseudo-op
    INST_MOV_I,                 
    INST_MOV_M,
    INST_MOV_RET,
    INST_MOVL,
    INST_MUX1,
    INST_MUX2,
    INST_NOP,
    INST_OR,
    INST_PACK2_SSS,
    INST_PACK2_USS,
    INST_PACK4_SSS,
    INST_PADD1,
    INST_PADD1_SSS,
    INST_PADD1_UUS,
    INST_PADD1_UUU,
    INST_PADD2,
    INST_PADD2_SSS,
    INST_PADD2_UUS,
    INST_PADD2_UUU,
    INST_PADD4,
    INST_PAVG1,
    INST_PAVG1_RAZ,
    INST_PAVG2,
    INST_PAVG2_RAZ,
    INST_PAVGSUB1,
    INST_PAVGSUB2,
    INST_PCMP1,
    INST_PCMP2,
    INST_PCMP4,
    INST_PMAX1_U,
    INST_PMAX2,
    INST_PMIN1_U,
    INST_PMIN2,
    INST_PMPY2_R,
    INST_PMPY2_L,
    INST_PMPYSHR2,
    INST_PMPYSHR2_U,
    INST_POPCNT,
    INST_PROBE_R,
    INST_PROBE_W,
    INST_PROBE_R_FAULT,
    INST_PROBE_W_FAULT,
    INST_PROBE_RW_FAULT,
    INST_PSAD1,
    INST_PSHL2,
    INST_PSHL4,
    INST_PSHLADD2,
    INST_PSHR2,
    INST_PSHR2_U,
    INST_PSHR4,
    INST_PSHR4_U,
    INST_PSHRADD2,
    INST_PSUB1,
    INST_PSUB1_SSS,
    INST_PSUB1_UUS,
    INST_PSUB1_UUU,
    INST_PSUB2,
    INST_PSUB2_SSS,
    INST_PSUB2_UUS,
    INST_PSUB2_UUU,
    INST_PSUB4,
    INST_PTC_E,
    INST_PTC_G,
    INST_PTC_GA,
    INST_PTC_L,
    INST_PTR_D,
    INST_PTR_I,
    INST_RFI,
    INST_RSM,
    INST_RUM,
    INST_SETF_S,
    INST_SETF_D,
    INST_SETF_EXP,
    INST_SETF_SIG,
    INST_SHL,
    INST_SHLADD,
    INST_SHLADDP4,
    INST_SHR,
    INST_SHR_U,
    INST_SHRP,
    INST_SRLZ_I,
    INST_SRLZ_D,
    INST_SSM,
    INST_ST_FIRST,                        // start of continuous group of store instructions 
    INST_ST = INST_ST_FIRST,
    INST_ST16,
    INST_ST8_SPILL,
    INST_STF,
    INST_STF8,
    INST_STF_SPILL,
    INST_ST_LAST = INST_STF_SPILL, // end of continuous group of store instructions 
    INST_SUB,
    INST_SUM,
    INST_SXT,
    INST_SYNC_I,
    INST_TAK,
    INST_TBIT,
    INST_THASH,
    INST_TNAT,
    INST_TPA,
    INST_TTAG,
    INST_UNPACK1_H,
    INST_UNPACK2_H,
    INST_UNPACK4_H,
    INST_UNPACK1_L,
    INST_UNPACK2_L,
    INST_UNPACK4_L,
    INST_XCHG,
    INST_XMA_L,
    INST_XMA_LU,
    INST_XMA_H,
    INST_XMA_HU,
    INST_XMPY_L,
    INST_XMPY_LU,
    INST_XMPY_H,
    INST_XMPY_HU,
    INST_XOR,
    INST_ZXT,
    INST_LAST = INST_ZXT,
    // following are pseudo-op
    INST_SWITCH, // switch, ignored
    INST_BREAKPOINT,
    INST_USE,
    INST_DEF,
    INST_BRL13,
    INST_BR13
};

enum Completer {
    CMPLT_INVALID = -1,
    CMPLT_FIRST = 0, // MUST BE 0
    //----------------------------
    // Branch_Types (br)
    CMPLT_BTYPE_COND = CMPLT_FIRST, // br, brl
    CMPLT_BTYPE_IA,
    CMPLT_BTYPE_WEXIT,
    CMPLT_BTYPE_WTOP,
    CMPLT_BTYPE_RET,
    CMPLT_BTYPE_CLOOP,
    CMPLT_BTYPE_CEXIT,
    CMPLT_BTYPE_CTOP,
    CMPLT_BTYPE_CALL,  // br, brl
    //----------------------------
    // Branch_Whether_Hint, NOT for all branch instructions!
    CMPLT_WH_IGNORE, // Ignore all hints. 
    CMPLT_WH_SPTK,   // Presaged branch should be predicted Static Taken
    CMPLT_WH_SPNT,
    CMPLT_WH_LOOP,   // Presaged branch will be br.cloop, br.ctop, or br.wtop
    CMPLT_WH_EXIT,   // Presaged branch will be br.cexit or br.wexit
    CMPLT_WH_DPTK,   // Presaged branch should be predicted Dynamically
    CMPLT_WH_DPNT,
    //----------------------------
    // Branch_Sequential_Prefetch_Hint (br, brl)
    CMPLT_PH_FEW,
    CMPLT_PH_MANY,
    //----------------------------
    // Branch_Cache_Deallocation_Hint (br, brl)
    CMPLT_DH_NOT_CLR,
    CMPLT_DH_CLR,
    //----------------------------
    // Branch_Predict_Importance_Hint (brp)
    CMPLT_IH_NOT_IMP,
    CMPLT_IH_IMP,
    //----------------------------
    // Speculation_Check_ALAT_Clear_Completer (chk.s, chk.a)
    CMPLT_CHK_A_CLR, // Invalidate matching ALAT entry
    CMPLT_CHK_A_NC,   // Dont invalidate
    //----------------------------
    // Comparison_Types ( both int and float! )
    CMPLT_CMP_CTYPE_NONE,
    CMPLT_CMP_CTYPE_NORMAL = CMPLT_CMP_CTYPE_NONE,
    CMPLT_CMP_CTYPE_UNC,
    CMPLT_CMP_CTYPE_OR,
    CMPLT_CMP_CTYPE_AND,
    CMPLT_CMP_CTYPE_OR_ANDCM,
    //----------------------------
    // Comparison_Relations
    CMPLT_CMP_CREL_EQ,
    CMPLT_CMP_CREL_NE,
    CMPLT_CMP_CREL_LT,
    CMPLT_CMP_CREL_LE,
    CMPLT_CMP_CREL_GT,
    CMPLT_CMP_CREL_GE,
    CMPLT_CMP_CREL_LTU, // unsigned
    CMPLT_CMP_CREL_LEU, // unsigned
    CMPLT_CMP_CREL_GTU, // unsigned
    CMPLT_CMP_CREL_GEU, // unsigned
    // Floating_Point_Comparison_Relations
    CMPLT_FCMP_FREL_UNORD,              // unordered f2 ? f3
    CMPLT_FCMP_FREL_NLT,                // not less than !(f2 < f3)
    CMPLT_FCMP_FREL_NLE,                // not less than or equal !(f2 <= f3)
    CMPLT_FCMP_FREL_NGT,                // not greater than !(f2 > f3)
    CMPLT_FCMP_FREL_NGE,                // not greater than or equal !(f2 >= f3)
    CMPLT_FCMP_FREL_ORD,                // ordered !(f2 ? f3)
    //----------------------------
    // Semaphore_Types
    CMPLT_SEM_ACQ,                  // Acquire The memory read/write is made
                                // visible prior to all subsequent data
                                // memory accesses.
    CMPLT_SEM_REL,                  // Release The memory read/write is made
                                // visible after all previous data memory
                                // accesses.
    //----------------------------
    // Floating_Point_pc_Mnemonic_Values
    CMPLT_PC_SINGLE,
    CMPLT_PC_DOUBLE,
    CMPLT_PC_DYNAMIC,
    //----------------------------
    // Floating_Point_sf_Mnemonic_Values
    CMPLT_SF0,
    CMPLT_SF1,
    CMPLT_SF2,
    CMPLT_SF3,
    //----------------------------
    // Floating_Point_Class_Relations
    CMPLT_FCREL_M,                   // FR f2 agrees with the pattern specified 
                                // by fclass9 (is a member)
    CMPLT_FCREL_NM,              // FR f2 does not agree with the pattern
                                // specified by fclass9 (is not a member)
    //----------------------------
    // Floating_Point_Classes
    CMPLT_FCLASS_NAT,       // NaTVal
    CMPLT_FCLASS_QNAN,      // Quiet NaN
    CMPLT_FCLASS_SNAN,      // Signaling NaN
    CMPLT_FCLASS_POS,       // Positive
    CMPLT_FCLASS_NEG,       // Negative
    CMPLT_FCLASS_ZERO,      // Zero
    CMPLT_FCLASS_UNORM,     // Unnormalized
    CMPLT_FCLASS_NORM,      // Normalized
    CMPLT_FCLASS_INF,       // Infinity 
    //----------------------------
    // fsz completer for ldf instruction
    CMPLT_FSZ_S,
    CMPLT_FSZ_D,
    CMPLT_FSZ_E,
    //----------------------------
    // sz completer for ld, st, cmpxchg, ...
    CMPLT_SZ_1,
    CMPLT_SZ_2,
    CMPLT_SZ_4,
    CMPLT_SZ_8,
    //----------------------------
    // Load_Types
    CMPLT_LDTYPE_NORMAL,    // Normal load
    CMPLT_LDTYPE_S,         // Speculative load
    CMPLT_LDTYPE_A,         // Advanced load
    CMPLT_LDTYPE_SA,        // Speculative Advanced load
    CMPLT_LDTYPE_C_NC,      // Check load - no clear
    CMPLT_LDTYPE_C_CLR,     // Check load - clear
    CMPLT_LDTYPE_C_CLR_ACQ, // Ordered check load  clear
    CMPLT_LDTYPE_ACQ,       // Ordered load
    CMPLT_LDTYPE_BIAS,      // Biased load
    //----------------------------
    // Line_Prefetch_Hints 
    // Some of Line_Prefetch_Hints are valid for lfetch, store, etc.
    CMPLT_HINT_NONE, // Temporal locality, level 1
    CMPLT_HINT_NT1,  // No temporal locality, level 1
    CMPLT_HINT_NT2,  // No temporal locality, level 2
    CMPLT_HINT_NTA,  // No temporal locality, all levels
    //----------------------------
    // Saturation
    CMPLT_SAT_NONE, // modulo_form
    CMPLT_SAT_SSS,
    CMPLT_SAT_USS,
    CMPLT_SAT_UUS,
    CMPLT_SAT_UUU,
    //----------------------------
    // Store_Types
    CMPLT_ST_TYPE_NORMAL,
    CMPLT_ST_TYPE_REL,
    //----------------------------
    CMPLT_FP_S,                // single_form, M18/M19
    CMPLT_FP_D,                // double_form, M18/M19
    CMPLT_FP_EXP,              // exponent_form, M18/M19
    CMPLT_FP_SIG,              // significand_form, M18/M19
    //----------------------------
    CMPLT_IREG_FIRST,
    CMPLT_IREG_CPUID = CMPLT_IREG_FIRST,  // Processor Identification Register
    CMPLT_IREG_DBR,                       // Data Breakpoint Register
    CMPLT_IREG_IBR,                       // Instruction Breakpoint Register
    CMPLT_IREG_PKR,                       // Protection Key Register
    CMPLT_IREG_PMC,                       // Performance Monitor Configuration Register
    CMPLT_IREG_PMD,                       // Performance Monitor Data Register
    CMPLT_IREG_RR,                        // Region Register
    CMPLT_IREG_LAST = CMPLT_IREG_RR,
    //==================================================
    CMPLT_LAST = CMPLT_FP_SIG,
    //----------------------------
    // xsz completer for sxt/zxt instructions
    CMPLT_XSZ_1 = CMPLT_SZ_1,
    CMPLT_XSZ_2 = CMPLT_SZ_2,
    CMPLT_XSZ_4 = CMPLT_SZ_4,
    //----------------------------
    // Floating_Point_Comparison_Types
    CMPLT_FCMP_FCTYPE_NORMAL = CMPLT_CMP_CTYPE_NORMAL,
    CMPLT_FCMP_FCTYPE_NONE = CMPLT_FCMP_FCTYPE_NORMAL,
    CMPLT_FCMP_FCTYPE_UNC = CMPLT_CMP_CTYPE_UNC,
    // Floating_Point_Comparison_Relations
    CMPLT_FCMP_FREL_EQ  = CMPLT_CMP_CREL_EQ,                 // equal f2 == f3
    CMPLT_FCMP_FREL_NEQ = CMPLT_CMP_CREL_NE,                 // not equal !(f2 == f3)
    CMPLT_FCMP_FREL_LT  = CMPLT_CMP_CREL_LT,                 // less than f2 < f3
    CMPLT_FCMP_FREL_LE  = CMPLT_CMP_CREL_LE,                 // less than or equal f2 <= f3
    CMPLT_FCMP_FREL_GT  = CMPLT_CMP_CREL_GT,                 // greater than f2 > f3
    CMPLT_FCMP_FREL_GE  = CMPLT_CMP_CREL_GE                  // greater than or equal f2 >= f3
};

// ========================================================================================//
// Instruction Description
// ========================================================================================//

struct InstDescription {
    char     * mnemonic;
    InstCode   inst;
    uint16     numDst;
    uint16     numOpnd;
    int        instType;
};

//============================================================================//
// Compliter Description
//============================================================================//

struct CmpltDescription {
    char        *mnemonic;
    Completer   cmplt;
};


//============================================================================//
#define INST_BREAKPOINT_IMM_VALUE ((uint64)0x4cafe)
#define CAFE                      ((uint64)0x4cafe)

//============================================================================//
// Encoder
//============================================================================//

// forward declaration of Inst class, declared in some other file
//class Inst;
//class Opnd;
//class Cfg;
//typedef vector<Opnd*>           OpndVector;
//typedef vector<Inst*>           InstVector;
typedef StlVector<Completer>      CompVector;

class Encoder {
  public:
    static bool patchCallAddr(char * callAddr, char * methodAddr);
    static void readBundle(uint64 *code, uint64 * tmplt, uint64 * slots);
    
    static char *  getMnemonic(InstCode opcode) { return instDesc[opcode].mnemonic; };
    static uint16  getNumDst(InstCode opcode)   { return instDesc[opcode].numDst; };
    static uint16  getNumOpnd(InstCode opcode)  { return instDesc[opcode].numOpnd; };
    static int     getInstType(InstCode opcode) { return instDesc[opcode].instType; };
    static int     getInstType(Inst *inst);
    static char *  getMnemonic(Completer cmplt) { return cmpltDesc[cmplt].mnemonic; };

    static bool   isBranchInst(Inst *);
    static bool   isBranchCallInst(Inst *);
    static bool   isIgnoreInst(Inst *);
    static bool   isPseudoInst(Inst *);
    static Inst * resolvePseudo(Cfg &, Inst *);
    
    static uint64   getNopBits(InstructionType, unsigned, uint64);
    static uint64   getHintBits(InstructionType, unsigned, uint64);
    static uint64   getBreakBits(InstructionType, unsigned, uint64);

    static uint64   getInstBits(InstructionType, Inst *);
    static uint64   getInstBitsBranch(InstructionType, Inst *, int);
    static uint64 * getInstBitsExtended(Inst *, uint64 *, void *);

    static uint64   ldfx(InstCode icode, Inst * inst, CompVector & cmpls, OpndVector & opnds, uint64 qp);
    static uint64   fcmp(InstCode icode, Inst * inst, CompVector & cmpls, OpndVector & opnds, uint64 qp);
    static uint64   cmp_cmp4(InstCode icode, Inst * inst, CompVector & cmpls, OpndVector & opnds, uint64 qp);
    static uint64   mov(InstructionType unit, InstCode icode, Inst * inst, CompVector & cmpls, OpndVector & opnds, uint64 qp);
    
    //----------------------------------------------------------------------------//
    static uint64 A1(uint64 x4, uint64 ct2d, int64 r3, int64 r2, int64 r1, uint64 qp) {
        return ((uint64)8 << 37) | ((x4 & 0xF) << 29) | (ct2d << 27) 
            | (r3 << 20) | (r2 << 13) | (r1 << 6) | qp;
    }
    
    static uint64 A2(uint64 x4, int64 count2, int64 r3, int64 r2, int64 r1, uint64 qp) {
        // count2 = ct2d + 1
        return ((uint64)8 << 37) | ((x4 & 0xF) << 29) | (((count2-1) & 0x3) << 27) 
            | (r3 << 20) | (r2 << 13) | (r1 << 6) | qp;
    }
    
    static uint64 A3(uint64 x4, uint64 x2b, int64 r3, int64 imm8, int64 r1, uint64 qp) {
        return ((uint64)8 << 37) | (x4 << 29) | (x2b << 27) 
            | (r3 << 20) | ( imm8<0 ? (uint64)1<<36 : 0 ) | ((0x7F & imm8) << 13)
            | (r1 << 6) | qp;
    }
    
    static uint64 A4(uint64 x2a, int64 r3, int64 imm14, int64 r1, uint64 qp) {
        // imm14 = sign_ext(s << 13 | imm6d << 7 | imm7b, 14)
        return ((uint64)8 << 37) | (x2a << 34) 
            | (r3 << 20) | (r1 << 6) | qp
            | (imm14<0 ? (uint64)1<<36 : 0) 
            | ((imm14 & 0x1F80) << 20) | ((imm14 & 0x7F) << 13);
    }
    
    static uint64 A5(int64 r3, int64 imm22, int64 r1, uint64 qp) {
        // imm22 = sign_ext(s << 21 | imm5c << 16 | imm9d << 7 | imm7b, 22)
        return ((uint64)9 << 37)
            | ((r3 & 0x3) << 20) | (r1 << 6) | qp
            | (imm22<0 ? (uint64)1<<36 : 0) 
            | ((imm22 & 0xFF80) << 20) | ((imm22 & 0x1F0000) << 6) | ((imm22 & 0x7F) << 13);
    }
    
    static uint64 A6(uint64 opcode, uint64 x2, uint64 ta, uint64 c, int64 p1, int64 p2, int64 r2, int64 r3, uint64 qp) {
        return (opcode << 37) | (x2 << 34) 
            | (ta << 33) | (p2 << 27) | (r3 << 20) 
            | (r2 << 13) | (c << 12) | (p1 << 6) | qp;
    }
    
    static uint64 A7(uint64 opcode, uint64 x2, uint64 ta, uint64 c, int64 p1, int64 p2, int64 r3, uint64 qp) {
        return (opcode << 37) | ((uint64)1 << 36) | (x2 << 34) 
            | (ta << 33) | (p2 << 27) | (r3 << 20) 
            | (c << 12) | (p1 << 6) | qp;
    }
    
    
    static uint64 A8(uint64 opcode, uint64 x2, uint64 ta, uint64 c, int64 p1, int64 p2, int64 imm8, int64 r3, uint64 qp) {
        return (opcode << 37) | (x2 << 34) | (ta << 33) | (c << 12) 
            | (p1 << 6) | (p2 << 27) | (r3 << 20) 
            | (imm8<0 ? (uint64)1<<36 : 0) | ((imm8 & 0x7F) << 13) | qp;
    }
    
    static uint64 A9(uint64 za, uint64 zb, uint64 x4, uint64 x2b, uint64 r3, uint64 r2, uint64 r1, uint64 qp) {
        return ((uint64)0x8 << 37) | (za << 36) | ((uint64)0x1 << 34) 
            | (zb << 33) | (x4 << 29) | (x2b << 27) 
            | (r3 << 20) | (r2 << 13) | (r1  << 6) | qp;
    }
    
    static uint64 A10(uint64 x4, uint64 ct2d, uint64 r3, uint64 r2, uint64 r1, uint64 qp) {
        return ((uint64)0x8 << 37) | ((uint64)0x1 << 34) 
            | ((uint64)0x1 << 33) | (x4 << 29) | (ct2d << 27) 
            | (r3 << 20) | (r2 << 13) | (r1  << 6) | qp;
    }
    
    static uint64 I1(uint64 ct2d, uint64 x2b, uint64 r3, uint64 r2, uint64 r1, uint64 qp) {
        // count2 = (ct2d == 0) ? 0 : (ct2d == 1) ? 7 : (ct2d == 2) ? 15 : 16
        return ((uint64)7 << 37) | ((uint64)1 << 33) | ((0x3 & ct2d) << 30)
            | ((0x3 & x2b) << 28) | (r3 << 20) | (r2 << 13) | (r1 << 6) | qp;
    }
    
    static uint64 I2(uint64 za, uint64 zb, uint64 x2c, uint64 x2b, uint64 r3, uint64 r2, uint64 r1, uint64 qp) {
        return ((uint64)7 << 37) | ((uint64)2 << 34) | ((0x1 & za) << 36)
            | ((0x1 & zb) << 33) | ((0x3 & x2c) << 30) | ((0x3 & x2b) << 28) 
            | (r3 << 20) | (r2 << 13) | (r1 << 6) | qp;
    }
    
    static uint64 I3(uint64 mbt4c, uint64 r2, uint64 r1, uint64 qp) {
        return ((uint64)7 << 37) | ((uint64)3 << 34) | ((uint64)2 << 28)
            | ((uint64)2 << 30) 
            | ((0xf & mbt4c) << 20) | (r2 << 13) | (r1 << 6) | qp;
    }
    
    static uint64 I4(uint64 mht8c, uint64 r2, uint64 r1, uint64 qp) {
        return ((uint64)7 << 37) | ((uint64)3 << 34) | ((uint64)2 << 28)
            | ((uint64)2 << 30) | ((uint64)1 << 33) 
            | ((0xFF & mht8c) << 20) | (r2 << 13) | (r1 << 6) | qp;
    }
    
    static uint64 I5(uint64 za, uint64 zb, uint64 x2b, uint64 r3, uint64 r2, uint64 r1, uint64 qp) {
        return ((uint64)7 << 37) 
            | ((0x1 & za) << 36) | ((0x1 & zb) << 33) | ((0x3 & x2b) << 28) 
            | (r3 << 20) | (r2 << 13) | (r1 << 6) | qp;
    }
    
    static uint64 I6(uint64 za, uint64 zb, uint64 x2b, uint64 r3, uint64 count5b, uint64 r1, uint64 qp) {
        return ((uint64)7 << 37) | ((uint64)1 << 34)
            | ((0x1 & za) << 36) | ((0x1 & zb) << 33) | ((0x3 & x2b) << 28) 
            | (r3 << 20) | ((0x1F & count5b) << 14) | (r1 << 6) | qp;
    }
    
    static uint64 I7(uint64 za, uint64 zb, uint64 r3, uint64 r2, uint64 r1, uint64 qp) {
        return ((uint64)7 << 37) | ((uint64)1 << 30)
            | ((0x1 & za) << 36) | ((0x1 & zb) << 33)
            | (r3 << 20) | (r2 << 13) | (r1 << 6) | qp;
    }
    
    static uint64 I8(uint64 za, uint64 zb, uint64 count5c, uint64 r2, uint64 r1, uint64 qp) {
        return ((uint64)7 << 37) | ((uint64)3 << 34) | ((uint64)1 << 28) | ((uint64)1 << 30)
            | ((0x1 & za) << 36) | ((0x1 & zb) << 33)
            | (count5c << 20) | (r2 << 13) | (r1 << 6) | qp;
    }
    
    static uint64 I9(uint64 r3, uint64 r1, uint64 qp) {
        return ((uint64)7 << 37) | ((uint64)1 << 33) | ((uint64)1 << 34) 
            | ((uint64)1 << 28) | ((uint64)2 << 30)
            | (r3 << 20) | (r1 << 6) | qp;
    }
    
    static uint64 I10(uint64 count6d, uint64 r3, uint64 r2, uint64 r1, uint64 qp) {
        return ((uint64)5 << 37) | ((uint64)3 << 34)
            | (count6d << 27) | (r3 << 20) | (r2 << 13) | (r1 << 6) | qp;
    }
    
    static uint64 I11(uint64 len6d, uint64 r3, uint64 pos6b, uint64 y, uint64 r1, uint64 qp) {
        return ((uint64)5 << 37) | ((uint64)1 << 34)
            | (len6d << 27) | (r3 << 20) | (pos6b << 14) | (y << 13) | (r1 << 6) | qp;
    }
    
    static uint64 I12(uint64 len6d, uint64 cpos6c, uint64 r2, uint64 r1, uint64 qp) {
        return ((uint64)5 << 37) | ((uint64)1 << 34) | ((uint64)1 << 33)
            | ((0x3F & len6d) << 27) | ((0x3F & cpos6c) << 20) 
            | (r2 << 13) | (r1 << 6) | qp;
    }
    
    static uint64 I13(uint64 len6d, uint64 cpos6c, int64 imm8, uint64 r1, uint64 qp) {
        return ((uint64)5 << 37) | ((uint64)1 << 34) | ((uint64)1 << 33) | ((uint64)1 << 26)
            | ((uint64)(imm8<0?1:0) << 36) | ((0x7F & imm8) << 13)
            | (len6d << 27) | (cpos6c << 20) | (r1 << 6) | qp;
    }
    
    static uint64 I14(int64 imm1, uint64 len6d, uint64 r3, uint64 cpos6b, uint64 r1, uint64 qp) {
        return ((uint64)5 << 37) | ((uint64)3 << 34) | ((uint64)1 << 33)
            | ((0x1 & imm1) << 36) | (len6d << 27) | (r3 << 20) | (cpos6b << 14) | (r1 << 6) | qp;
    }
    
    static uint64 I15(uint64 cpos6d, uint64 len4d, uint64 r3, uint64 r2, uint64 r1, uint64 qp) {
        return ((uint64)4 << 37)
            | (cpos6d << 36) | (len4d << 27) | (r3 << 20) | (r2 << 13) | (r1 << 6) | qp;
    }
    
    static uint64 I20(int64 imm21, uint64 r2, uint64 qp) {
        return ((uint64)(imm21<0?1:0) << 36) | ((uint64)1 << 33) 
            | ((0xFFF80 & imm21) << 20) | (r2 << 13) | ((0x7F & imm21) << 6) | qp;
    }
    
    static uint64 I21(uint64 timm9c, uint64 ih, uint64 x, uint64 wh, uint64 r2, uint64 b1, uint64 qp) {
        return ((uint64)0x7 << 33) | (timm9c << 24) 
            | (ih << 23) | (x << 22) | (wh << 20) | (r2 << 13) | (b1 << 6) | qp;
    }
    
    static uint64 I22(uint64 b2, uint64 r1, uint64 qp) {
        return ((uint64)0x31 << 27)
            | (b2 << 13) | (r1 << 6) | qp;
    }
    
    static uint64 I23(uint64 mask16, uint64 r2, uint64 qp) {
        return ((uint64)0x3 << 33)
            | ((uint64)(mask16&0x8000 ? 1 : 0) << 36) | (((mask16 >> 7) & 0xFF) << 24)
            | ((mask16 & 0x7F) << 6) | (r2 << 13) | qp;
    }
    
    static uint64 I24(int64 imm28, uint64 qp) {
        return ((uint64)0x2 << 33)
            | ((uint64)(imm28<0 ? 1 : 0) << 36) | ((imm28 & 0x7FFFFFF) << 6) | qp;
    }
    
    static uint64 I25(uint64 x6, uint64 r1, uint64 qp) {
        return (x6 << 27) | (r1 << 6) | qp;
    }
    
    static uint64 I26(uint64 ar3, uint64 r2, uint64 qp) {
        return ((uint64)0x2A << 27) | (ar3 << 20) | (r2 << 13) | qp;
    }
    
    static uint64 I27(uint64 ar3, int64 imm8, uint64 qp) {
        return ((uint64)0x0A << 27) | (ar3 << 20) 
            | ((uint64)(imm8 < 0?1:0) << 36) | ((0x7F & imm8) << 13) | qp;
    }
    
    static uint64 I28(uint64 ar3, uint64 r1, uint64 qp) {
        return ((uint64)0x32 << 27) | (ar3 << 20) | (r1 << 6) | qp;
    }
    
    static uint64 I29(uint64 x6, uint64 r3, uint64 r1, uint64 qp) {
        return ((x6 & 0x3f) << 27) | ((r3 & 0x7f) << 20) | ((r1 & 0x7f) << 6) | qp;
    }
    
    static uint64 M1(uint64 x6, uint64 hint, uint64 x, int64 r3, int64 r1, uint64 qp) {
        return ((uint64)4 << 37) | (x6 << 30) | (hint << 28) | (x << 27) 
            | (r3 << 20) | (r1 << 6) | qp;
    }
    
    static uint64 M2(uint64 x6, uint64 hint, int64 r3, int64 r2, int64 r1, uint64 qp) {
        return ((uint64)4 << 37) | ((uint64)1 << 36) | (x6 << 30) | (hint << 28) 
            | (r3 << 20) | (r2 << 13) | (r1 << 6) | qp;
    }
    
    static uint64 M3(uint64 x6, uint64 hint, int64 r3, int64 imm9, int64 r1, uint64 qp) {
        return ((uint64)5 << 37) | (x6 << 30) | (hint << 28) 
            | (r3 << 20) | ((uint64)(imm9<0?1:0) << 36) 
            | (((imm9 >> 7) & 0x1) << 27) | ((imm9 & 0x7F) <<13)
            | (r1 << 6) | qp;
    }
    
    static uint64 M4(uint64 x6, uint64 hint, uint64 x, int64 r3, int64 r2, uint64 qp) {
        return ((uint64)4 << 37) | (x6 << 30) | (hint << 28) |(x << 27) 
            | (r3 << 20) | (r2 << 13) | qp;
    }
    
    static uint64 M5(uint64 x6, uint64 hint, int64 r3, int64 r2, int64 imm9, uint64 qp) {
        // imm9 = sign_ext(s << 8 | i << 7 | imm7a, 9)
        return ((uint64)5 << 37) | (x6 << 30) | (hint << 28)
            | (r3 << 20) | (r2 << 13)
            | (imm9<0 ? (uint64)1<<36 : 0)
            | ((imm9 & 0x80) << 20) | ((imm9 & 0x7F) << 6) | qp;
    }
    
    static uint64 M6(uint64 x6, uint64 hint, int64 r3, int64 f1, uint64 qp) {
        return ((uint64)6 << 37) | (x6 << 30) | (hint << 28)
            | (r3 << 20) | (f1 << 6) | qp;
    }
    
    static uint64 M7(uint64 x6, uint64 hint, int64 r3, int64 r2, int64 f1, uint64 qp) {
        return ((uint64)6 << 37) | ((uint64)1 << 36) | (x6 << 30) | (hint << 28)
            | (r3 << 20) | (r2 << 13) | (f1 << 6) | qp;
    }
    
    static uint64 M8(uint64 x6, uint64 hint, int64 r3, int64 imm9, int64 f1, uint64 qp) {
        return ((uint64)7 << 37) | (x6 << 30) | (hint << 28)
            | ((uint64)(imm9<0?1:0) << 36) | (((imm9 >> 7) & 0x1) << 27) 
            | ((imm9 & 0x7F) << 13) | (r3 << 20) | (f1 << 6) | qp;
    }
    
    static uint64 M9(uint64 x6, uint64 hint, int64 r3, int64 f2, uint64 qp) {
        // 6 m x6 hint x r3 f2 qp
        return ((uint64)6 << 37) | (x6 << 30) | (hint << 28)
            | (r3 << 20) | (f2 << 13) | qp;
    }
    
    static uint64 M10(uint64 x6, uint64 hint, int64 r3, int64 f2, int64 imm9, uint64 qp) {
        // imm9 = sign_ext(s << 8 | i << 7 | imm7a, 9)
        return ((uint64)7 << 37) | (x6 << 30) | (hint << 28)
            | (r3 << 20) | (f2 << 13) | ((uint64)(imm9<0?1:0) << 36) 
            | ((uint64)(imm9 & 0x80) << 20) | ((uint64)(imm9 & 0x7F) << 6) | qp;
    }
    
    static uint64 M18(uint64 x6, int64 r2, int64 f1, int64 qp) {
        return ((uint64)6 << 37) | (x6 << 30) | ((uint64)1 << 27)
            | (r2 << 13) | (f1 << 6) | qp;
    }
    
    static uint64 M19(uint64 x6, int64 f2, int64 r1, int64 qp) {
        return ((uint64)4 << 37) | (x6 << 30) | ((uint64)1 << 27)
            | (f2 << 13) | (r1 << 6) | qp;
    }
    
    static uint64 M28(uint64 x, int64 r3, int64 qp) {
        return ((uint64)1 << 37) | ((uint64)0x30 << 27)
            | (x << 36) | (r3 << 20) | qp;
    }
    
    static uint64 M29(uint64 ar3, int64 r2, int64 qp) {
        return ((uint64)1 << 37) | ((uint64)0x2A << 27)
            | (ar3 << 20) | (r2 << 13) | qp;
    }
    
    static uint64 M30(uint64 ar3, int64 imm8, int64 qp) {
        return ((uint64)0 << 37) | ((uint64)0x2 << 31) | ((uint64)0x8 << 27)
            | (ar3 << 20) | ((uint64)(imm8<0?1:0) << 36) | ((0x7F & imm8) << 13) 
            | qp;
    }
    
    static uint64 M31(uint64 ar3, int64 r1, int64 qp) {
        return ((uint64)1 << 37) | ((uint64)0x22 << 27)
            | (ar3 << 20) | (r1 << 6) | qp;
    }
    
    static uint64 M34(uint64 sor, uint64 sol, uint64 sof, int64 r1, int64 qp) {
        return ((uint64)1 << 37) | ((uint64)0x6 << 33)
            | (sor << 27) | (sol << 20) | (sof << 13) | (r1 << 6) | qp;
    }
    
    
    static uint64 B1_B2(uint64 dh, uint64 bwh, uint64 ph, uint64 btype, int64 imm21, uint64 qp) {
        return ((uint64)4 << 37) 
            | (dh << 35) | (bwh << 33) | (ph << 12) | (btype << 6) | qp
            | (imm21<0 ? (uint64)1<<36 : 0) 
            | ((imm21 & 0xFFFFF) << 13);
    }
    
    static uint64 B3(uint64 p, uint64 wh, uint64 d, int64 imm21, uint64 b1, uint64 qp) {
        // target25 = IP + (sign_ext(s << 20 | imm20b, 21) << 4)
        return ((uint64)5 << 37) 
            | (d << 35) | (wh << 32) | (p << 12) 
            | ((uint64)(imm21<0?1:0) << 36) | ((imm21 & 0xFFFFF) << 13) | (b1 << 6) | qp;
    }
    
    static uint64 B4(uint64 dh, uint64 bwh, uint64 x6, uint64 b2, uint64 ph, uint64 btype, uint64 qp) {
        return ((uint64)0 << 37) 
            | (dh << 35) | (bwh << 33) | (x6 << 27) | (ph << 12) | (btype << 6) | qp
            | (b2 << 13);
    }
    
    static uint64 B5(uint64 p, uint64 wh, uint64 d, uint64 b2, uint64 b1, uint64 qp) {
        return ((uint64)1 << 37) 
            | (d << 35) | (wh << 32) | (p << 12) | (b2 << 13) | (b1 << 6) | qp;
    }
    
    static uint64 F1(uint64 opcode, uint64 x, uint64  sf, int64 f4, int64 f3, int64 f2, int64 f1, uint64 qp) {
        return (opcode << 37) | (x << 36) | (sf << 34)
            | (f4 << 27) | (f3 << 20) | (f2 << 13) | (f1 << 6) | qp;
    }
    
    static uint64 F2(uint64  x2, int64 f4, int64 f3, int64 f2, int64 f1, uint64 qp) {
        return ((uint64)0xE << 37) | ((uint64)1 << 36) | ((x2 & 0x3)<< 34)
            | (f4 << 27) | (f3 << 20) | (f2 << 13) | (f1 << 6) | qp;
    }
    
    static uint64 F4(uint64 rb, uint64 sf, uint64 ra, int64 p2, int64 f3, int64 f2, uint64 ta, int64 p1, uint64 qp) {
        return ((uint64)0x4 << 37) | (rb << 36) | (sf << 34) | (ra << 33) 
            | (p2 << 27) | (f3 << 20) | (f2 << 13) | (ta << 12) | ( p1 << 6) | qp;
    }
    
    static uint64 F5(uint64 fclass9, uint64 p2, uint64 f2, int64 ta, uint64 p1, uint64 qp) {
        return ((uint64)0x5 << 37) | ((fclass9 >> 7) << 33) | ((fclass9 & 0x7F) << 20)
            | (p2 << 27) | (f2 << 13) | (ta << 12) | ( p1 << 6) | qp;
    }
    
    static uint64 F6(uint64 opcode, uint64 sf, int64 p2, int64  f3, int64 f2, uint64 f1, uint64 qp) {
        return (opcode << 37) | (sf << 34) | ((uint64)1 << 33) 
            | (p2 << 27) | (f3 << 20) | (f2 << 13) | (f1 << 6) | qp;
    }
    
    static uint64 F8(uint64 opcode, uint64 sf, uint64 x6, int64 f3, int64 f2, uint64 f1, uint64 qp) {
        return (opcode << 37) | (sf <<34) | (x6 << 27) | (f3 << 20) | (f2 << 13) | (f1 << 6) | qp;
    }
    
    static uint64 F9(uint64 opcode, uint64 x6, int64 f3, int64 f2, uint64 f1, uint64 qp) {
        return (opcode << 37) | (x6 << 27) | (f3 << 20) | (f2 << 13) | (f1 << 6) | qp;
    }
    
    static uint64 F10(uint64 opcode, uint64 sf, uint64 x6, int64 f2, uint64 f1, uint64 qp) {
        return (opcode << 37) | (sf << 34) | (x6 << 27) | (f2 << 13) | (f1 << 6) | qp;
    }
    
    static uint64 F11(int64 f2, int64 f1, int64 qp) {
        return ((uint64)0x1C << 27) 
            | (f2 << 13) | (f1 << 6) | qp;
    }
    
    static uint64 * X2(int vc, uint64 r1, int64 imm64, uint64 qp, uint64 * slots12) {
        // imm64 = i << 63 | imm41 << 22 | ic << 21 | imm5c << 16 | imm9d << 7 | imm7b
        // slot1: 6 i imm9d imm5c ic vc imm7b r1 qp; slot2: imm41
        slots12[1] = ((uint64)6 << 37);
        slots12[1] |= r1 << 6
            | qp | ((vc & 0x01) << 20)
            | ((int64)imm64<0 ? (uint64)1<<36 : 0)
            | ((imm64 & 0x7F) << 13)
            | ((imm64 & (0x1FF<<7)) << 20)
            | ((imm64 & (0x1F<<16)) << 6)
            | (imm64 & (0x01<<21)) ;
        slots12[0] = (imm64 & (0x1FFFFFFFFFF<<22)) >> 22;
        return slots12;
    }
    
    static uint64 * X3(uint64 imm60, uint64 d, uint64 wh, uint64 p, uint64 qp, uint64 * slots12) {
        // (qp) brl.btype.bwh.ph.dh b1 = target64
        // target64 = IP + ((i << 59 | imm39 << 20 | imm20b) << 4)
        slots12[1] = ((uint64)0xC << 37) | ((imm60 >> 59) << 36) | (d << 35) 
            | (wh << 33) | (p << 12) | qp
            | ((imm60 & 0xFFFFF) << 13);
        slots12[0] = (imm60 & (0x7FFFFFFFFF<<20)) >> 18;
        return slots12;
    }
    
    static uint64 * X4(uint64 imm60, uint64 d, uint64 wh, uint64 p, uint64 b1, uint64 qp, uint64 * slots12) {
        // (qp) brl.btype.bwh.ph.dh b1 = target64
        // target64 = IP + ((i << 59 | imm39 << 20 | imm20b) << 4)
        slots12[1] = ((uint64)0xD << 37) | ((imm60 >> 59) << 36) | (d << 35) 
            | (wh << 33) | (p << 12) | (b1 << 6) | qp
            | ((imm60 & 0xFFFFF) << 13);
        slots12[0] = (imm60 & (0x7FFFFFFFFF<<20)) >> 18;
        return slots12;
    }
    
    static uint64 * X5(uint64 y, int64 imm62, uint64 qp, uint64 * slots12) {
        // imm62 = imm41 << 21 | i << 20 | imm20a
        slots12[1] = ((y & 0x01) << 26) | qp
            | ((imm62 & 0xFFFFF) << 6)
            | ((imm62 & 0x100000) << 16);
        slots12[0] = (imm62 & (0x1FFFFFFFFFF<<21)) >> 21;
        return slots12;
    }
    
  protected:
    static const InstDescription instDesc[];
    static const CmpltDescription cmpltDesc[];
};

} // IPF
} // Jitrino

#endif /*IPFENCODER_H_*/
