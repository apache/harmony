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

#include "IpfEncoder.h"
#include "IpfCfg.h"
#include "IpfOpndManager.h"
#include "IpfIrPrinter.h"

namespace Jitrino {
namespace IPF {

//========================================================================================//
// InstDescription initialization
//========================================================================================//

const InstDescription Encoder::instDesc[] = {
    {"add"              , INST_ADD              , 1, 4, IT_A},
    {"adds"             , INST_ADDS             , 1, 4, IT_A},
    {"addl"             , INST_ADDL             , 1, 4, IT_A},
    {"addp4"            , INST_ADDP4            , 1, 4, IT_A},
    {"alloc"            , INST_ALLOC            , 1, 6, IT_M | IT_GF},
    {"and"              , INST_AND              , 1, 4, IT_A},
    {"andcm"            , INST_ANDCM            , 1, 4, IT_A},
    {"br"               , INST_BR               , 0, 2, IT_B},
    {"brl"              , INST_BRL              , 0, 2, IT_L | IT_GL},
    {"brp"              , INST_BRP              , 0, 3, IT_B},
    {"brp.ret"          , INST_BRP_RET          , 0, 3, IT_B},
    {"break"            , INST_BREAK            , 0, 2, IT_ANY | IT_EXPLICIT_IMBFX },
    {"bsw.0"            , INST_BSW_0            , 0, 1, IT_B},
    {"bsw.1"            , INST_BSW_1            , 0, 1, IT_B},
    {"chk.s"            , INST_CHK_S            , 0, 3, IT_EXPLICIT_IM },
    {"chk.s.i"          , INST_CHK_S_I          , 0, 3, IT_I },
    {"chk.s.m"          , INST_CHK_S_M          , 0, 3, IT_M },
    {"chk.a"            , INST_CHK_A            , 0, 3, IT_M},
    {"clrrrb"           , INST_CLRRRB           , 0, 1, IT_B},
    {"clrrrb.pr"        , INST_CLRRRB_PR        , 0, 1, IT_B},
    {"cmp"              , INST_CMP              , 2, 5, IT_A},
    {"cmp4"             , INST_CMP4             , 2, 5, IT_A},
    {"cmpxchg"          , INST_CMPXCHG          , 1, 4, IT_M},
    {"cmp8xchg16"       , INST_CMP8XCHG16       , 1, 4, IT_M},
    {"cover"            , INST_COVER            , 0, 1, IT_B},
    {"czx1.l"           , INST_CZX1_L           , 1, 3, IT_I},
    {"czx1.r"           , INST_CZX1_R           , 1, 3, IT_I},
    {"czx2.l"           , INST_CZX2_L           , 1, 3, IT_I},
    {"czx2.r"           , INST_CZX2_R           , 1, 3, IT_I},
    {"dep"              , INST_DEP              , 1, 6, IT_I},
    {"dep.z"            , INST_DEP_Z            , 1, 5, IT_I},
    {"epc"              , INST_EPC              , 0, 1, IT_B},
    {"extr"             , INST_EXTR             , 1, 5, IT_I},
    {"extr.u"           , INST_EXTR_U           , 1, 5, IT_I},
    {"fabs"             , INST_FABS             , 1, 3, IT_F},
    {"fadd"             , INST_FADD             , 1, 4, IT_F},
    {"famax"            , INST_FAMAX            , 1, 4, IT_F},
    {"famin"            , INST_FAMIN            , 1, 4, IT_F},
    {"fand"             , INST_FAND             , 1, 4, IT_F},
    {"fandcm"           , INST_FANDCM           , 1, 4, IT_F},
    {"fc"               , INST_FC               , 0, 2, IT_M},
    {"fc.i"             , INST_FC_I             , 0, 2, IT_M},
    {"fchkf"            , INST_FCHKF            , 0, 2, IT_F},
    {"fclass"           , INST_FCLASS           , 2, 5, IT_F},
    {"fclrf"            , INST_FCLRF            , 0, 1, IT_F},
    {"fcmp"             , INST_FCMP             , 2, 5, IT_F},
    {"fcvt.fx"          , INST_FCVT_FX          , 1, 3, IT_F},
    {"fcvt.fx.trunc"    , INST_FCVT_FX_TRUNC    , 1, 3, IT_F},
    {"fcvt.fxu"         , INST_FCVT_FXU         , 1, 3, IT_F},
    {"fcvt.fxu.trunc"   , INST_FCVT_FXU_TRUNC   , 1, 3, IT_F},
    {"fcvt.xf"          , INST_FCVT_XF          , 1, 3, IT_F},
    {"fcvt.xuf"         , INST_FCVT_XUF         , 1, 3, IT_F},
    {"fetchadd4"        , INST_FETCHADD4        , 1, 4, IT_M},
    {"fetchadd8"        , INST_FETCHADD8        , 1, 4, IT_M},
    {"flushrs"          , INST_FLUSHRS          , 0, 1, IT_M},
    {"fma"              , INST_FMA              , 1, 5, IT_F},
    {"fmax"             , INST_FMAX             , 1, 4, IT_F},
    {"fmerge.ns"        , INST_FMERGE_NS        , 1, 4, IT_F},
    {"fmerge.s"         , INST_FMERGE_S         , 1, 4, IT_F},
    {"fmerge.se"        , INST_FMERGE_SE        , 1, 4, IT_F},
    {"fmin"             , INST_FMIN             , 1, 4, IT_F},
    {"fmix.l"           , INST_FMIX_L           , 1, 4, IT_F},
    {"fmix.r"           , INST_FMIX_R           , 1, 4, IT_F},
    {"fmix.lr"          , INST_FMIX_LR          , 1, 4, IT_F},
    {"fmpy"             , INST_FMPY             , 1, 4, IT_F},
    {"fms"              , INST_FMS              , 1, 5, IT_F},
    {"fneg"             , INST_FNEG             , 1, 3, IT_F},
    {"fnegabs"          , INST_FNEGABS          , 1, 3, IT_F},
    {"fnma"             , INST_FNMA             , 1, 5, IT_F},
    {"fnmpy"            , INST_FNMPY            , 1, 4, IT_F},
    {"fnorm"            , INST_FNORM            , 1, 3, IT_F},
    {"for"              , INST_FOR              , 1, 4, IT_F},
    {"fpabs"            , INST_FPABS            , 1, 3, IT_F},
    {"fpack"            , INST_FPACK            , 1, 4, IT_F},
    {"fpamax"           , INST_FPAMAX           , 1, 4, IT_F},
    {"fpamin"           , INST_FPAMIN           , 1, 4, IT_F},
    {"fpcmp"            , INST_FPCMP            , 1, 4, IT_F},
    {"fpcvt.fx"         , INST_FPCVT_FX         , 1, 3, IT_F},
    {"fpcvt.fx.trunc"   , INST_FPCVT_FX_TRUNC   , 1, 3, IT_F},
    {"fpcvt.fxu"        , INST_FPCVT_FXU        , 1, 3, IT_F},
    {"fpcvt.fxu.trunc"  , INST_FPCVT_FXU_TRUNC  , 1, 3, IT_F},
    {"fpma"             , INST_FPMA             , 1, 5, IT_F},
    {"fpmax"            , INST_FPMAX            , 1, 4, IT_F},
    {"fpmerge.ns"       , INST_FPMERGE_NS       , 1, 4, IT_F},
    {"fpmerge.s"        , INST_FPMERGE_S        , 1, 4, IT_F},
    {"fpmerge.se"       , INST_FPMERGE_SE       , 1, 4, IT_F},
    {"fpmin"            , INST_FPMIN            , 1, 4, IT_F},
    {"fpmpy"            , INST_FPMPY            , 1, 4, IT_F},
    {"fpms"             , INST_FPMS             , 1, 5, IT_F},
    {"fpneg"            , INST_FPNEG            , 1, 3, IT_F},
    {"fpnegabs"         , INST_FPNEGABS         , 1, 3, IT_F},
    {"fpnma"            , INST_FPNMA            , 1, 5, IT_F},
    {"fpnmpy"           , INST_FPNMPY           , 1, 4, IT_F},
    {"fprcpa"           , INST_FPRCPA           , 2, 5, IT_F},
    {"fprsqrta"         , INST_FPRSQRTA         , 2, 4, IT_F},
    {"frcpa"            , INST_FRCPA            , 2, 5, IT_F},
    {"frsqrta"          , INST_FRSQRTA          , 2, 4, IT_F},
    {"fselect"          , INST_FSELECT          , 1, 5, IT_F},
    {"fsetc"            , INST_FSETC            , 0, 3, IT_F},
    {"fsub"             , INST_FSUB             , 1, 4, IT_F},
    {"fswap"            , INST_FSWAP            , 1, 4, IT_F},
    {"fswap.nl"         , INST_FSWAP_NL         , 1, 4, IT_F},
    {"fswap.nr"         , INST_FSWAP_NR         , 1, 4, IT_F},
    {"fsxt.l"           , INST_FSXT_L           , 1, 4, IT_F},
    {"fsxt.r"           , INST_FSXT_R           , 1, 4, IT_F},
    {"fwb"              , INST_FWB              , 0, 1, IT_M},
    {"fxor"             , INST_FXOR             , 1, 4, IT_F},
    {"getf.s"           , INST_GETF_S           , 1, 3, IT_M},
    {"getf.d"           , INST_GETF_D           , 1, 3, IT_M},
    {"getf.exp"         , INST_GETF_EXP         , 1, 3, IT_M},
    {"getf.sig"         , INST_GETF_SIG         , 1, 3, IT_M},
    {"hint"             , INST_HINT             , 0, 2, IT_ANY | IT_EXPLICIT_IMBFX },
    {"invala"           , INST_INVALA           , 0, 1, IT_M},
    {"invala.e"         , INST_INVALA_E         , 0, 2, IT_M},
    {"itc.i"            , INST_ITC_I            , 0, 2, IT_M},
    {"itc.d"            , INST_ITC_D            , 0, 2, IT_M},
    {"itr.i"            , INST_ITR_I            , 1, 3, IT_M},
    {"itr.d"            , INST_ITR_D            , 1, 3, IT_M},
    {"ld"               , INST_LD               , 1, 3, IT_M},
    {"ld16"             , INST_LD16             , 1, 3, IT_M},
    {"ld16.acq"         , INST_LD16_ACQ         , 1, 3, IT_M},
    {"ld8.fill"         , INST_LD8_FILL         , 1, 3, IT_M},
    {"ldf"              , INST_LDF              , 1, 3, IT_M}, 
    {"ldf8"             , INST_LDF8             , 1, 3, IT_M},
    {"ldf.fill"         , INST_LDF_FILL         , 1, 3, IT_M},
    {"ldfps"            , INST_LDFPS            , 2, 4, IT_M},
    {"ldfpd"            , INST_LDFPD            , 2, 4, IT_M},
    {"ldfp8"            , INST_LDFP8            , 2, 4, IT_M},
    {"lfetch"           , INST_LFETCH           , 0, 3, IT_M},
    {"lfetch"           , INST_LFETCH_EXCL      , 0, 3, IT_M},
    {"loadrs"           , INST_LOADRS           , 0, 1, IT_M},
    {"mf"               , INST_MF               , 0, 1, IT_M},
    {"mf.a"             , INST_MF_A             , 0, 1 ,IT_M},
    {"mix1.l"           , INST_MIX1_L           , 1, 4, IT_I},
    {"mix2.l"           , INST_MIX2_L           , 1, 4, IT_I},
    {"mix4.l"           , INST_MIX4_L           , 1, 4, IT_I},
    {"mix1.r"           , INST_MIX1_R           , 1, 4, IT_I},
    {"mix2.r"           , INST_MIX2_R           , 1, 4, IT_I},
    {"mix4.r"           , INST_MIX4_R           , 1, 4, IT_I},
    {"mov"              , INST_MOV              , 1, 3, IT_PSEUDO_OP },
    {"mov.i"            , INST_MOV_I            , 1, 3, IT_I },
    {"mov.m"            , INST_MOV_M            , 1, 3, IT_M },
    {"mov.ret"          , INST_MOV_RET          , 1, 3, IT_I },
    {"movl"             , INST_MOVL             , 1, 3, IT_L},
    {"mux1"             , INST_MUX1             , 1, 4, IT_I},
    {"mux2"             , INST_MUX2             , 1, 4, IT_I},
    {"nop"              , INST_NOP              , 0, 1, IT_ANY | IT_EXPLICIT_IMBFX },
    {"or"               , INST_OR               , 1, 4, IT_A},
    {"pack2.sss"        , INST_PACK2_SSS        , 1, 4, IT_I},
    {"pack2.uss"        , INST_PACK2_USS        , 1, 4, IT_I},
    {"pack4.sss"        , INST_PACK4_SSS        , 1, 4, IT_I},
    {"padd1"            , INST_PADD1            , 1, 4, IT_A},
    {"padd1.sss"        , INST_PADD1_SSS        , 1, 4, IT_A},
    {"padd1.uus"        , INST_PADD1_UUS        , 1, 4, IT_A},
    {"padd1.uuu"        , INST_PADD1_UUU        , 1, 4, IT_A},
    {"padd2"            , INST_PADD2            , 1, 4, IT_A},
    {"padd2.sss"        , INST_PADD2_SSS        , 1, 4, IT_A},
    {"padd2.uus"        , INST_PADD2_UUS        , 1, 4, IT_A},
    {"padd2.uuu"        , INST_PADD2_UUU        , 1, 4, IT_A},
    {"padd4"            , INST_PADD4            , 1, 4, IT_A},
    {"pavg1"            , INST_PAVG1            , 1, 4, IT_A},
    {"pavg1.raz"        , INST_PAVG1_RAZ        , 1, 4, IT_A},
    {"pavg2"            , INST_PAVG2            , 1, 4, IT_A},
    {"pavg2.raz"        , INST_PAVG2_RAZ        , 1, 4, IT_A},
    {"pavgsub1"         , INST_PAVGSUB1         , 1, 4, IT_A},
    {"pavgsub2"         , INST_PAVGSUB2         , 1, 4, IT_A},
    {"pcmp1"            , INST_PCMP1            , 1, 4, IT_A},
    {"pcmp2"            , INST_PCMP2            , 1, 4, IT_A},
    {"pcmp4"            , INST_PCMP4            , 1, 4, IT_A},
    {"pmax1.u"          , INST_PMAX1_U          , 1, 4, IT_I},
    {"pmax2"            , INST_PMAX2            , 1, 4, IT_I},
    {"pmin1.u"          , INST_PMIN1_U          , 1, 4, IT_I},
    {"pmin2"            , INST_PMIN2            , 1, 4, IT_I},
    {"pmpy2.r"          , INST_PMPY2_R          , 1, 4, IT_I},
    {"pmpy2.l"          , INST_PMPY2_L          , 1, 4, IT_I},
    {"pmpyshr2"         , INST_PMPYSHR2         , 1, 5, IT_I},
    {"pmpyshr2.u"       , INST_PMPYSHR2_U       , 1, 5, IT_I},
    {"popcnt"           , INST_POPCNT           , 1, 3, IT_I},
    {"probe.r"          , INST_PROBE_R          , 1, 4, IT_M},
    {"probe.w"          , INST_PROBE_W          , 1, 4, IT_M},
    {"probe.r.fault"    , INST_PROBE_R_FAULT    , 1, 3, IT_M},
    {"probe.w.fault"    , INST_PROBE_W_FAULT    , 1, 3, IT_M},
    {"probe.rw.fault"   , INST_PROBE_RW_FAULT   , 1, 3, IT_M},
    {"psad1"            , INST_PSAD1            , 1, 4, IT_I},
    {"pshl2"            , INST_PSHL2            , 1, 4, IT_I},
    {"pshl4"            , INST_PSHL4            , 1, 4, IT_I},
    {"pshladd2"         , INST_PSHLADD2         , 1, 5, IT_A},
    {"pshr2"            , INST_PSHR2            , 1, 4, IT_I},
    {"pshr2.u"          , INST_PSHR2_U          , 1, 4, IT_I},
    {"pshr4"            , INST_PSHR4            , 1, 4, IT_I},
    {"pshr4.u"          , INST_PSHR4_U          , 1, 4, IT_I},
    {"pshradd2"         , INST_PSHRADD2         , 1, 5, IT_A},
    {"psub1"            , INST_PSUB1            , 1, 4, IT_A},
    {"psub1.sss"        , INST_PSUB1_SSS        , 1, 4, IT_A},
    {"psub1.uus"        , INST_PSUB1_UUS        , 1, 4, IT_A},
    {"psub1.uuu"        , INST_PSUB1_UUU        , 1, 4, IT_A},
    {"psub2"            , INST_PSUB2            , 1, 4, IT_A},
    {"psub2.sss"        , INST_PSUB2_SSS        , 1, 4, IT_A},
    {"psub2.uus"        , INST_PSUB2_UUS        , 1, 4, IT_A},
    {"psub2.uuu"        , INST_PSUB2_UUU        , 1, 4, IT_A},
    {"psub4"            , INST_PSUB4            , 1, 4, IT_A},
    {"ptc.e"            , INST_PTC_E            , 0, 2, IT_M},
    {"ptc.g"            , INST_PTC_G            , 0, 3, IT_M},
    {"ptc.ga"           , INST_PTC_GA           , 0, 3, IT_M},
    {"ptc.l"            , INST_PTC_L            , 0, 3, IT_M},
    {"ptr.d"            , INST_PTR_D            , 0, 3, IT_M},
    {"ptr.i"            , INST_PTR_I            , 0, 3, IT_M},
    {"rfi"              , INST_RFI              , 0, 1, IT_B},
    {"rsm"              , INST_RSM              , 0, 2, IT_M},
    {"rum"              , INST_RUM              , 0, 2, IT_M},
    {"setf.s"           , INST_SETF_S           , 1, 3, IT_M},
    {"setf.d"           , INST_SETF_D           , 1, 3, IT_M},
    {"setf.exp"         , INST_SETF_EXP         , 1, 3, IT_M},
    {"setf.sig"         , INST_SETF_SIG         , 1, 3, IT_M},
    {"shl"              , INST_SHL              , 1, 4, IT_I},
    {"shladd"           , INST_SHLADD           , 1, 5, IT_A},
    {"shladdp4"         , INST_SHLADDP4         , 1, 5, IT_A},
    {"shr"              , INST_SHR              , 1, 4, IT_I},
    {"shr.u"            , INST_SHR_U            , 1, 4, IT_I},
    {"shrp"             , INST_SHRP             , 1, 5, IT_I},
    {"srlz.i"           , INST_SRLZ_I           , 0, 1, IT_M},
    {"srlz.d"           , INST_SRLZ_D           , 0, 1, IT_M},
    {"ssm"              , INST_SSM              , 1, 2, IT_M}, 
    {"st"               , INST_ST               , 0, 3, IT_M}, 
    {"st16"             , INST_ST16             , 0, 3, IT_M},
    {"st8.spill"        , INST_ST8_SPILL        , 0, 3, IT_M},
    {"stf"              , INST_STF              , 0, 3, IT_M},
    {"stf8"             , INST_STF8             , 0, 3, IT_M},
    {"stf.spill"        , INST_STF_SPILL        , 0, 3, IT_M},
    {"sub"              , INST_SUB              , 1, 4, IT_A},
    {"sum"              , INST_SUM              , 0, 2, IT_M},
    {"sxt"              , INST_SXT              , 1, 3, IT_I},
    {"sync.i"           , INST_SYNC_I           , 0, 1, IT_M},
    {"tak"              , INST_TAK              , 1, 3, IT_M},
    {"tbit"             , INST_TBIT             , 2, 5, IT_I},
    {"thash"            , INST_THASH            , 1, 3, IT_M},
    {"tnat"             , INST_TNAT             , 2, 4, IT_I},
    {"tpa"              , INST_TPA              , 1, 3, IT_M},
    {"ttag"             , INST_TTAG             , 1, 3, IT_M},
    {"unpack1.h"        , INST_UNPACK1_H        , 1, 4, IT_I},
    {"unpack2.h"        , INST_UNPACK2_H        , 1, 4, IT_I},
    {"unpack4.h"        , INST_UNPACK4_H        , 1, 4, IT_I},
    {"unpack1.l"        , INST_UNPACK1_L        , 1, 4, IT_I},
    {"unpack2.l"        , INST_UNPACK2_L        , 1, 4, IT_I},
    {"unpack4.l"        , INST_UNPACK4_L        , 1, 4, IT_I},
    {"xchg"             , INST_XCHG             , 1, 4, IT_M},
    {"xma.l"            , INST_XMA_L            , 1, 5, IT_F},
    {"xma.lu"           , INST_XMA_LU           , 1, 5, IT_F},
    {"xma.h"            , INST_XMA_H            , 1, 5, IT_F},
    {"xma.hu"           , INST_XMA_HU           , 1, 5, IT_F},
    {"xmpy.l"           , INST_XMPY_L           , 1, 5, IT_F},
    {"xmpy.lu"          , INST_XMPY_LU          , 1, 5, IT_F},
    {"xmpy.h"           , INST_XMPY_H           , 1, 5, IT_F},
    {"xmpy.hu"          , INST_XMPY_HU          , 1, 5, IT_F},
    {"xor"              , INST_XOR              , 1, 4, IT_A},
    {"zxt"              , INST_ZXT              , 1, 3, IT_I},
    // following are pseudo-op
    {"switch"           , INST_SWITCH           , 0, 0, IT_PSEUDO_OP | IT_IGNORE_OP},
    {"break.i"          , INST_BREAKPOINT       , 0, 1, IT_I},
    {"use"              , INST_USE              , 0, 2, IT_PSEUDO_OP | IT_IGNORE_OP},
    {"def"              , INST_DEF              , 1, 2, IT_PSEUDO_OP | IT_IGNORE_OP},
    {"brl-13"           , INST_BRL13            , 1, 4, IT_L | IT_GL},
    {"br-13"            , INST_BR13             , 1, 4, IT_B}
};

const CmpltDescription Encoder::cmpltDesc[] = {
    // Branch_Types (br)
    {".cond",  CMPLT_BTYPE_COND},  // br, brl
    {".ia",    CMPLT_BTYPE_IA},
    {".wexit", CMPLT_BTYPE_WEXIT},
    {".wtop",  CMPLT_BTYPE_WTOP},
    {".ret",   CMPLT_BTYPE_RET},
    {".cloop", CMPLT_BTYPE_CLOOP},
    {".cexit", CMPLT_BTYPE_CEXIT},
    {".ctop",  CMPLT_BTYPE_CTOP},
    {".call",  CMPLT_BTYPE_CALL},  // br, brl
    //----------------------------
    // Branch_Whether_Hint, NOT for all branch instructions!
    {"",      CMPLT_WH_IGNORE}, // Ignore all hints. 
    {".sptk", CMPLT_WH_SPTK},   // Presaged branch should be predicted Static Taken
    {".spnt", CMPLT_WH_SPNT},
    {".loop", CMPLT_WH_LOOP},   // Presaged branch will be br.cloop, br.ctop, or br.wtop
    {".exit", CMPLT_WH_EXIT},   // Presaged branch will be br.cexit or br.wexit
    {".dptk", CMPLT_WH_DPTK},   // Presaged branch should be predicted Dynamically
    {".dpnt", CMPLT_WH_DPNT},
    //----------------------------
    // Branch_Sequential_Prefetch_Hint (br, brl)
    {".few",  CMPLT_PH_FEW},
    {".many", CMPLT_PH_MANY},
    //----------------------------
    // Branch_Cache_Deallocation_Hint (br, brl)
    {"",     CMPLT_DH_NOT_CLR},
    {".clr", CMPLT_DH_CLR},
    //----------------------------
    // Branch_Predict_Importance_Hint (brp)
    {"",     CMPLT_IH_NOT_IMP},
    {".imp", CMPLT_IH_IMP},
    //----------------------------
    // Speculation_Check_ALAT_Clear_Completer (chk.s, chk.a)
    {".clr", CMPLT_CHK_A_CLR}, // Invalidate matching ALAT entry
    {".nc",  CMPLT_CHK_A_NC},  // Dont invalidate
    //----------------------------
    // Comparison_Types 
    {"",          CMPLT_CMP_CTYPE_NONE},
    {".unc",      CMPLT_CMP_CTYPE_UNC},
    {".or",       CMPLT_CMP_CTYPE_OR},
    {".and",      CMPLT_CMP_CTYPE_AND},
    {".or.andcm", CMPLT_CMP_CTYPE_OR_ANDCM},
    //----------------------------
    // Comparison_Relations
    {".eq",  CMPLT_CMP_CREL_EQ},
    {".ne",  CMPLT_CMP_CREL_NE},
    {".lt",  CMPLT_CMP_CREL_LT},
    {".le",  CMPLT_CMP_CREL_LE},
    {".gt",  CMPLT_CMP_CREL_GT},
    {".ge",  CMPLT_CMP_CREL_GE},
    {".ltu", CMPLT_CMP_CREL_LTU}, // unsigned
    {".leu", CMPLT_CMP_CREL_LEU}, // unsigned
    {".gtu", CMPLT_CMP_CREL_GTU}, // unsigned
    {".geu", CMPLT_CMP_CREL_GEU}, // unsigned
    //----------------------------
    // Floating_Point_Comparison_Relations
    {".unord", CMPLT_FCMP_FREL_UNORD},              // unordered f2 ? f3
    {".nlt",   CMPLT_FCMP_FREL_NLT},                // not less than !(f2 < f3)
    {".nle",   CMPLT_FCMP_FREL_NLE},                // not less than or equal !(f2 <= f3)
    {".ngt",   CMPLT_FCMP_FREL_NGT},                // not greater than !(f2 > f3)
    {".nge",   CMPLT_FCMP_FREL_NGE},                // not greater than or equal !(f2 >= f3)
    {".ord",   CMPLT_FCMP_FREL_ORD},                // ordered !(f2 ? f3)
    //----------------------------
    // Semaphore_Types
    {".acq", CMPLT_SEM_ACQ},      // Acquire The memory read/write is made
                            // visible prior to all subsequent data
                            // memory accesses.
    {".rel", CMPLT_SEM_REL},      // Release The memory read/write is made
                            // visible after all previous data memory
                            // accesses.
    //----------------------------
    // Floating_Point_pc_Mnemonic_Values
    {".s", CMPLT_PC_SINGLE},
    {".d", CMPLT_PC_DOUBLE},
    {"",   CMPLT_PC_DYNAMIC},
    //----------------------------
    // Floating_Point_sf_Mnemonic_Values
    {".s0", CMPLT_SF0},
    {".s1", CMPLT_SF1},
    {".s2", CMPLT_SF2},
    {".s3", CMPLT_SF3},
    //----------------------------
    // Floating_Point_Class_Relations
    {".m",  CMPLT_FCREL_M},        // FR f2 agrees with the pattern specified 
                                // by fclass9 (is a member)
    {".nm", CMPLT_FCREL_NM},       // FR f2 does not agree with the pattern
                                // specified by fclass9 (is not a member)
    //----------------------------
    // Floating_Point_Classes fclass9 in fclass instruction
    {"NaTVal",        CMPLT_FCLASS_NAT},       // NaTVal
    {"Quiet NaN",     CMPLT_FCLASS_QNAN},      // Quiet NaN
    {"Signaling NaN", CMPLT_FCLASS_SNAN},      // Signaling NaN
    {"Positive",      CMPLT_FCLASS_POS},       // Positive
    {"Negative",      CMPLT_FCLASS_NEG},       // Negative
    {"Zero",          CMPLT_FCLASS_ZERO},      // Zero
    {"Unnormalized",  CMPLT_FCLASS_UNORM},     // Unnormalized
    {"Normalized",    CMPLT_FCLASS_NORM},      // Normalized
    {"Infinity",      CMPLT_FCLASS_INF},       // Infinity 
    //----------------------------
    // fsz completer for ldf instruction
    {"s", CMPLT_FSZ_S},
    {"d", CMPLT_FSZ_D},
    {"e", CMPLT_FSZ_E},
    //----------------------------
    // sz completer for ld/st instructions
    {"1",      CMPLT_SZ_1},
    {"2",      CMPLT_SZ_2},
    {"4",      CMPLT_SZ_4},
    {"8",      CMPLT_SZ_8},
    //----------------------------
    // Load_Types
    {"",           CMPLT_LDTYPE_NORMAL},    // Normal load
    {".s",         CMPLT_LDTYPE_S},         // Speculative load
    {".a",         CMPLT_LDTYPE_A},         // Advanced load
    {".sa",        CMPLT_LDTYPE_SA},        // Speculative Advanced load
    {".c.nc",      CMPLT_LDTYPE_C_NC},      // Check load - no clear
    {".c.clr",     CMPLT_LDTYPE_C_CLR},     // Check load - clear
    {".c.clr.acq", CMPLT_LDTYPE_C_CLR_ACQ}, // Ordered check load  clear
    {".acq",       CMPLT_LDTYPE_ACQ},       // Ordered load
    {".bias",      CMPLT_LDTYPE_BIAS},      // Biased load
    //----------------------------
    // Line_Prefetch_Hints 
    // Some of Line_Prefetch_Hints are valid for lfetch, store, etc.
    {"",     CMPLT_HINT_NONE}, // Temporal locality, level 1
    {".nt1", CMPLT_HINT_NT1},  // No temporal locality, level 1
    {".nt2", CMPLT_HINT_NT2},  // No temporal locality, level 2
    {".nta", CMPLT_HINT_NTA},  // No temporal locality, all levels
    //----------------------------
    // Saturation
    {"",     CMPLT_SAT_NONE}, // modulo_form
    {".sss", CMPLT_SAT_SSS},
    {".uss", CMPLT_SAT_USS},
    {".uus", CMPLT_SAT_UUS},
    {".uuu", CMPLT_SAT_UUU},
    //----------------------------
    // Store_Types
    {"",     CMPLT_ST_TYPE_NORMAL},
    {".rel", CMPLT_ST_TYPE_REL},
    //----------------------------
    // compliters 
    {".s",   CMPLT_FP_S},                // single_form, M18/M19
    {".d",   CMPLT_FP_D},                // double_form, M18/M19
    {".exp", CMPLT_FP_EXP},              // exponent_form, M18/M19
    {".sig", CMPLT_FP_SIG},               // significand_form, M18/M19
    //----------------------------
    {"cpuid", CMPLT_IREG_CPUID},  // Processor Identification Register
    {"dbr",   CMPLT_IREG_DBR},    // Data Breakpoint Register
    {"ibr",   CMPLT_IREG_IBR},    // Instruction Breakpoint Register
    {"pkr",   CMPLT_IREG_PKR},    // Protection Key Register
    {"pmc",   CMPLT_IREG_PMC},    // Performance Monitor Configuration Register
    {"pmd",   CMPLT_IREG_PMD},    // Performance Monitor Data Register
    {"rr",    CMPLT_IREG_RR}      // Region Register
};

bool Encoder::isBranchInst(Inst * inst) {
    
    if (inst == NULL) return false;
    
    InstCode instCode = inst->getInstCode();
    if (instCode>=INST_BR_FIRST && instCode<=INST_BR_LAST) return true;
    if (instCode == INST_BR13) return true;
    if (instCode == INST_BRL13) return true;
    
    return false; 
}

bool Encoder::isBranchCallInst(Inst * inst) { 
    if (isBranchInst(inst)) {
        CompVector &cmpls = inst->getComps();
        if ( cmpls.size()>0 && cmpls[0]==CMPLT_BTYPE_CALL) return true;
    }
    return false;
}

bool Encoder::isIgnoreInst(Inst * inst ) {
    InstCode icode = inst->getInstCode();
    
    if ((getInstType(icode) & IT_IGNORE_OP)==IT_IGNORE_OP) {
        return true;
    }
    
    return false;
}

bool Encoder::isPseudoInst(Inst * inst ) {
    return (getInstType(inst->getInstCode()) & IT_PSEUDO_OP)==IT_PSEUDO_OP;
}

int  Encoder::getInstType(Inst *inst) {
    if (inst==NULL) return 0;
    
    InstCode icode = inst->getInstCode();
    
    if (icode==INST_MOV) {
        OpndVector & opnds = inst->getOpnds();
        int opndsize=opnds.size();

        if (opndsize>=3) {
            CompVector & cmpls = inst->getComps();
            OpndKind okind1 = opnds[1]->getOpndKind();
            OpndKind okind2 = opnds[2]->getOpndKind();

            if (okind1==OPND_A_REG || okind2==OPND_A_REG)   return IT_I | IT_M;
            if (okind1==OPND_B_REG || okind2==OPND_B_REG)   return IT_I;
            if (okind1==OPND_F_REG && okind2==OPND_F_REG)   return IT_F;
            if (okind1==OPND_G_REG && okind2==OPND_IP_REG)  return IT_I;
            if (okind1==OPND_G_REG && okind2==OPND_P_REG)   return IT_I;
            if (okind1==OPND_P_REG && okind2==OPND_G_REG)   return IT_I;
            if (okind1==OPND_P_REG && opnds[2]->isImm(44))     return IT_I;
            if (okind1==OPND_UM_REG && okind2==OPND_UM_REG) return IT_M;
            if (okind1==OPND_G_REG && okind2==OPND_G_REG && cmpls.size()==0)
                return IT_A;
            if (okind1==OPND_G_REG && okind2==OPND_G_REG && cmpls.size()==1 
                    && cmpls[0]>=CMPLT_IREG_FIRST && cmpls[0]<=CMPLT_IREG_LAST)
                return IT_M;
        }
    }
    return Encoder::getInstType(icode);
}

Inst * Encoder::resolvePseudo(Cfg & cfg, Inst * inst) {
    if (getInstType(inst->getInstCode()) & IT_PSEUDO_OP) {
        MemoryManager& mm=cfg.getMM();
        OpndManager* opndManager = cfg.getOpndManager();
    
        Inst * newinst = NULL;
        OpndVector & opnds = inst->getOpnds();
        CompVector & cmpls = inst->getComps();
        OpndKind okind1 = OPND_INVALID;
        OpndKind okind2 = OPND_INVALID;
        int opndsize=opnds.size();
        
        switch(inst->getInstCode()) {
        case INST_MOV:
            okind1 = opnds[1]->getOpndKind();
            okind2 = opnds[2]->getOpndKind();

            if (opndsize == 3) {
                if (okind1 == OPND_G_REG && opnds[2]->isImm(22)) {
                    newinst = new(mm) Inst(mm, INST_ADDL, opnds[0], opnds[1], opnds[2]
                            , cfg.getOpndManager()->getR0());
                } else if (okind1 == OPND_G_REG && opnds[2]->isImm(64)) {
                    newinst = new(mm) Inst(mm, INST_MOVL, opnds[0], opnds[1], opnds[2]);
                } else if (okind1 == OPND_G_REG && okind2 == OPND_G_REG 
                         && cmpls.size()==0) {
                    newinst = new(mm) Inst(mm, INST_ADDS, opnds[0], opnds[1]
                            , opndManager->newImm(0), opnds[2]);
                } else if (okind1 == OPND_F_REG && okind2 == OPND_F_REG) {
                    newinst = new(mm) Inst(mm, INST_FMERGE_S, opnds[0], opnds[1], opnds[2], opnds[2]);
                } else if (okind1 == OPND_B_REG && okind2 == OPND_G_REG) {
                    newinst = new(mm) Inst(mm, INST_MOV, opnds[0], opnds[1], opnds[2]
                           , opndManager->newImm(0));
                } else if (okind1 == OPND_G_REG && okind2 == OPND_A_REG) {
                    newinst = new(mm) Inst(mm, INST_MOV_M, opnds[0], opnds[1], opnds[2]);
                } else if (okind1 == OPND_G_REG && okind2 == OPND_B_REG) {
                    break;
                } else if (okind1 == OPND_A_REG
                         && (okind2 == OPND_G_REG || opnds[2]->isImm(8))) {
                    break;
                } else {
                    IPF_ERR << __FILE__ << ": " << __LINE__
                            << ": NOT YET IMPLEMENTED INSTRUCTION: "
                            << IrPrinter::toString(inst) << "\n";
                    assert(0);
                }
            } else {
                IPF_ERR << __FILE__ << ": " << __LINE__
                      << ": NOT YET IMPLEMENTED INSTRUCTION: "
                       << IrPrinter::toString(inst) << "\n";
                assert(0);
            }
            break;
        default:
            break;
        }
        if (newinst!=NULL ) {
            return newinst;
        }
    }
    return inst;
}


// nop: I18, B9, M48, F16, X5
uint64 Encoder::getNopBits(InstructionType unit, unsigned qp, uint64 imm21) {
    if( unit==IT_B) {
        return 0x4000000000 
            | ((imm21 & 0x100000) << 16)
            | ((imm21 & 0x0FFFFF) << 6)
            | qp;
    }
    return 0x8000000 
        | ((imm21 & 0x100000) << 16)
        | ((imm21 & 0x0FFFFF) << 6)
        | qp;
}

// nop: I18, B9, M48, F16, X5
uint64 Encoder::getHintBits(InstructionType unit, unsigned qp, uint64 imm) {
    if( unit==IT_B) return 0x4008000000 | qp;
    return 0xC000000 | qp;
}

// break: I19, B9, M37, F15, X1
uint64 Encoder::getBreakBits(InstructionType unit, unsigned qp, uint64 imm21) {
    return (uint64)0x0 
        | ((imm21 & 0x100000) << 16)
        | ((imm21 & 0x0FFFFF) << 6)
        | qp;
}

uint64 Encoder::getInstBits(InstructionType unit, Inst * inst) {
    InstCode   icode  = inst->getInstCode();
    OpndVector & opnds = inst->getOpnds();
    CompVector & cmpls = inst->getComps();
    int    opndcount = opnds.size();
    int    cmplcount = cmpls.size();
    uint64     qp     = opnds[0]->getValue();
    uint64 instbits  = 0;

    switch (icode) {
    // special cases
    case INST_NOP:
        return getNopBits(unit, qp, opnds[1]->getValue());
    case INST_HINT:
        return getHintBits(unit, qp, opnds[1]->getValue());
    case INST_BREAK:
        return getBreakBits(unit, qp, opnds[1]->getValue());
    case INST_BREAKPOINT:
        return getBreakBits(unit, qp, INST_BREAKPOINT_IMM_VALUE);

    // start insts processing
    case INST_ADD: 
        if( opndcount == 5 ) { // add r1 = r2, r3, 1
            return A1(0, 1, opnds[3]->getValue(), opnds[2]->getValue(), opnds[1]->getValue(), qp);
        } else {                  // add r1 = r2, r3
            return A1(0, 0, opnds[3]->getValue(), opnds[2]->getValue(), opnds[1]->getValue(), qp);
        }
    case INST_ADDS: // opnds[2]->getOpndKind() == OPND_IMM14
        return A4(2, opnds[3]->getValue(), opnds[2]->getValue(), opnds[1]->getValue(), qp);
    case INST_ADDL: // opnds[2]->getOpndKind() == OPND_IMM22
        return A5(opnds[3]->getValue(), opnds[2]->getValue(), opnds[1]->getValue(), qp);
    case INST_ADDP4:
        if (opnds[2]->getOpndKind() == OPND_G_REG) {
            return A1(2, 0, opnds[3]->getValue(), opnds[2]->getValue(), opnds[1]->getValue(), qp);
        } else {
            return A4(3, opnds[3]->getValue(), opnds[2]->getValue(), opnds[1]->getValue(), qp);
        }
    case INST_ALLOC:
        {
            uint64 i, l, o, r, sof, sol, sor, r1;
            
            r1 = opnds[1]->getValue();
            i = opnds[2]->getValue();
            l = opnds[3]->getValue();
            o = opnds[4]->getValue();
            r = opnds[5]->getValue();
            sof = i + l + o;
            sol = i +l;
            sor = r >> 3;
            
            return M34(sor, sol, sof, r1, qp);
        }
    case INST_AND: 
    case INST_ANDCM:
    case INST_OR:
    case INST_XOR:
    case INST_SUB:
        {
            uint64 x4=3, x2b=0;

            switch (icode) {
            case INST_AND:   x4=3; x2b=0; break;
            case INST_ANDCM: x4=3; x2b=1; break;
            case INST_OR:    x4=3; x2b=2; break;
            case INST_XOR:   x4=3; x2b=3; break;
            case INST_SUB:   x4=1; if (opndcount == 5) x2b=0; else x2b=1; break;
            default: assert(0); break;
            }            

            if (opnds[2]->isImm(8)) {
                x4 += 0x8;
                return A3(x4, x2b, opnds[3]->getValue(), opnds[2]->getValue(), opnds[1]->getValue(), qp);
            } else {
                return A1(x4, x2b, opnds[3]->getValue(), opnds[2]->getValue(), opnds[1]->getValue(), qp);
            }
        }
    case INST_CMP:
        return cmp_cmp4(icode, inst, cmpls, opnds, qp);
    case INST_CMP4:
        return cmp_cmp4(icode, inst, cmpls, opnds, qp);

    case INST_FABS:
        return F9(0, 0x10, opnds[2]->getValue(), 0, opnds[1]->getValue(), qp);
    case INST_FADD:
        // pseudo-op of: (qp) fma.pc.sf f1 = f3, f1, f2
        {
            uint64 opcode=8, x=0, sf=0;
            
            for (int i=0 ; i<cmplcount ; i++ ) {
                switch (cmpls[i]) {
                case CMPLT_PC_DYNAMIC: x=0; opcode=8; break;
                case CMPLT_PC_SINGLE:  x=1; opcode=8; break;
                case CMPLT_PC_DOUBLE:  x=0; opcode=9; break;
                case CMPLT_SF0: sf=0; break;
                case CMPLT_SF1: sf=1; break;
                case CMPLT_SF2: sf=2; break;
                case CMPLT_SF3: sf=3; break;
                default: assert(0); break;
                }
            }
            return F1(opcode, x, sf, 1, opnds[2]->getValue(), opnds[3]->getValue(), opnds[1]->getValue(), qp);
        }
    case INST_FNORM:
        // (qp) fnorm.pc.sf f1 = f3        pseudo-op of: (qp) fma.pc.sf f1 = f3, f1, f0
        {
            uint64 opcode=8, x=0, sf=0;
            
            for (int i=0 ; i<cmplcount ; i++ ) {
                switch (cmpls[i]) {
                case CMPLT_PC_DYNAMIC: x=0; opcode=8; break;
                case CMPLT_PC_SINGLE:  x=1; opcode=8; break;
                case CMPLT_PC_DOUBLE:  x=0; opcode=9; break;
                case CMPLT_SF0: sf=0; break;
                case CMPLT_SF1: sf=1; break;
                case CMPLT_SF2: sf=2; break;
                case CMPLT_SF3: sf=3; break;
                default: assert(0); break;
                }
            }
            return F1(opcode, x, sf, 1, opnds[2]->getValue(), 0, opnds[1]->getValue(), qp);
        }
    case INST_FAND:
        return F9(0, 0x2C, opnds[3]->getValue(), opnds[2]->getValue(), opnds[1]->getValue(), qp);
    case INST_FANDCM:
        return F9(0, 0x2D, opnds[3]->getValue(), opnds[2]->getValue(), opnds[1]->getValue(), qp);
    case INST_FC:
        return M28(0, opnds[1]->getValue(), qp);
    case INST_FC_I:
        return M28(1, opnds[1]->getValue(), qp);
    case INST_FCMP:
        return fcmp(icode, inst, cmpls, opnds, qp);
    case INST_FCLASS:
        {
            uint64 px, p1=opnds[1]->getValue(), p2=opnds[2]->getValue(), ta=0;
            
            for (int i=0; i<cmplcount; ++i) {
                if (cmpls[i]==CMPLT_FCMP_FCTYPE_UNC) { ta=1; }
                else if (cmpls[i]==CMPLT_FCREL_NM)   { px=p1; p1=p2; p2=px; }
            }
            return F5(opnds[4]->getValue(), p2, opnds[3]->getValue(), ta, p1, qp);
        }
    case INST_FCVT_FX: 
    case INST_FCVT_FXU: 
    case INST_FCVT_FX_TRUNC: 
    case INST_FCVT_FXU_TRUNC: 
    case INST_FPCVT_FX: 
    case INST_FPCVT_FXU: 
    case INST_FPCVT_FX_TRUNC: 
    case INST_FPCVT_FXU_TRUNC: 
        {
            uint64 opcode=0, x6=0x18, sf=0;
            
            if ( cmplcount==1 ) {
                switch(cmpls[0]) {
                case CMPLT_SF0:       sf=0; break;
                case CMPLT_SF1:       sf=1; break;
                case CMPLT_SF2:       sf=2; break;
                case CMPLT_SF3:       sf=3; break;
                default: assert(0); break;
                }
            }
            switch (icode) {
            case INST_FCVT_FX:        opcode=0; x6=0x18; break;
            case INST_FCVT_FXU:       opcode=0; x6=0x19; break;
            case INST_FCVT_FX_TRUNC:  opcode=0; x6=0x1A; break;
            case INST_FCVT_FXU_TRUNC: opcode=0; x6=0x1B; break;
            case INST_FPCVT_FX:        opcode=1; x6=0x18; break;
            case INST_FPCVT_FXU:       opcode=1; x6=0x19; break;
            case INST_FPCVT_FX_TRUNC:  opcode=1; x6=0x1A; break;
            case INST_FPCVT_FXU_TRUNC: opcode=1; x6=0x1B; break;
            default: assert(0); break;
            }
            return F10(opcode, sf, x6, opnds[2]->getValue(), opnds[1]->getValue(), qp);
        }
    case INST_FCVT_XF:
        return F11(opnds[2]->getValue(), opnds[1]->getValue(), qp);
    case INST_FCVT_XUF:
    case INST_FMA: {
            uint64 opcode=0, x=0, pc=CMPLT_PC_DYNAMIC, sf=CMPLT_SF0;
            
            if ( cmpls.size()==2 ) {
                pc=cmpls[0];
                sf=cmpls[1];
            } else if ( cmpls.size()==1 ) {
                switch ( cmpls[0] ) {
                case CMPLT_PC_SINGLE:  
                case CMPLT_PC_DOUBLE:  
                case CMPLT_PC_DYNAMIC: pc=cmpls[0]; break;
                default:               sf=cmpls[0]; break;
                }
            }
            switch (pc) {
            case CMPLT_PC_DYNAMIC: opcode=8; x=0; break;
            case CMPLT_PC_SINGLE:  opcode=8; x=1; break;
            case CMPLT_PC_DOUBLE:  opcode=9; x=0; break;
            }
            switch ( sf ) {
            case CMPLT_SF0: sf=0; break;
            case CMPLT_SF1: sf=1; break;
            case CMPLT_SF2: sf=2; break;
            case CMPLT_SF3: sf=3; break;
            }
            
            if (icode==INST_FMA) {
                return F1(opcode, x, sf
                    , opnds[3]->getValue(), opnds[2]->getValue()
                    , opnds[4]->getValue(), opnds[1]->getValue(), qp);
            } else {
                return F1(opcode, x, sf
                    , 1, opnds[2]->getValue()
                    , 0, opnds[1]->getValue(), qp);
            }
        }
    case INST_FMIN:
    case INST_FMAX:
    case INST_FAMIN:
    case INST_FAMAX:
    case INST_FPMIN:
    case INST_FPMAX:
    case INST_FPAMIN:
    case INST_FPAMAX:
    case INST_FPCMP:
        {
            uint64 opcode=0, x6=0xFF, sf=0;
            uint64 fx, f1=opnds[1]->getValue(), f2=opnds[2]->getValue(), f3=opnds[3]->getValue();
            
            for (int i=0 ; i<cmplcount ; i++ ) {
                switch (cmpls[i]) {
                case CMPLT_SF0: sf=0; break;
                case CMPLT_SF1: sf=1; break;
                case CMPLT_SF2: sf=2; break;
                case CMPLT_SF3: sf=3; break;
                case CMPLT_FCMP_FREL_EQ:    x6=0x30; break;
                case CMPLT_FCMP_FREL_GT:    fx=f2; f2=f3; f3=fx;
                case CMPLT_FCMP_FREL_LT:    x6=0x31; break;
                case CMPLT_FCMP_FREL_GE:    fx=f2; f2=f3; f3=fx;
                case CMPLT_FCMP_FREL_LE:    x6=0x32; break;
                case CMPLT_FCMP_FREL_UNORD: x6=0x33; break;
                case CMPLT_FCMP_FREL_NEQ:   x6=0x34; break;
                case CMPLT_FCMP_FREL_NGT:   fx=f2; f2=f3; f3=fx;
                case CMPLT_FCMP_FREL_NLT:   x6=0x35; break;
                case CMPLT_FCMP_FREL_NGE:   fx=f2; f2=f3; f3=fx;
                case CMPLT_FCMP_FREL_NLE:   x6=0x36; break;
                case CMPLT_FCMP_FREL_ORD:   x6=0x37; break;
                default: assert(0); break;
                }
            }
            switch (icode) {
            case INST_FMIN:   opcode=0; x6=0x14; break;
            case INST_FMAX:   opcode=0; x6=0x15; break;
            case INST_FAMIN:  opcode=0; x6=0x16; break;
            case INST_FAMAX:  opcode=0; x6=0x17; break;
            case INST_FPMIN:  opcode=1; x6=0x14; break;
            case INST_FPMAX:  opcode=1; x6=0x15; break;
            case INST_FPAMIN: opcode=1; x6=0x16; break;
            case INST_FPAMAX: opcode=1; x6=0x17; break;
            case INST_FPCMP:  opcode=1; break;
            default: assert(0); break;
            }
            
            return F8(opcode, sf, x6, f3, f2, f1, qp);
        }
    case INST_FMERGE_S:
        return F9(0, 0x10, opnds[3]->getValue(), opnds[2]->getValue(), opnds[1]->getValue(), qp);
    case INST_FNEG:
        return F9(0, 0x11, opnds[2]->getValue(), opnds[2]->getValue(), opnds[1]->getValue(), qp);
    case INST_FNMA: {
            uint64 opcode=0xC, x=0, sf=0;
            
            for ( int ci=0 ; ci<cmplcount ; ci++ ) {
                switch(cmpls[ci]) {
                case CMPLT_PC_SINGLE: x=1; break;
                case CMPLT_PC_DOUBLE: opcode=0xD; break;
                case CMPLT_SF1:       sf=1; break;
                case CMPLT_SF2:       sf=2; break;
                case CMPLT_SF3:       sf=3; break;
                default: break;
                }
            }

            return F1(opcode, x, sf, opnds[3]->getValue(), opnds[2]->getValue(),
                  opnds[4]->getValue(), opnds[1]->getValue(), qp);
        }
    case INST_FRCPA: {
            uint64 sf=0;
            
            if ( cmplcount==1 ) {
                switch(cmpls[0]) {
                case CMPLT_SF1:       sf=1; break;
                case CMPLT_SF2:       sf=2; break;
                case CMPLT_SF3:       sf=3; break;
                default: break;
                }
            }
            return F6(0, sf, opnds[2]->getValue(), opnds[4]->getValue(),
                  opnds[3]->getValue(), opnds[1]->getValue(), qp);
        }
    case INST_FSUB: { // qp) fsub.pc.sf f1 = f3, f2 pseudo-op of: (qp) fms.pc.sf f1 = f3, f4==1, f2
            uint64 opcode=0xA, x=0, sf=0;
            
            for ( int ci=0 ; ci<cmplcount ; ci++ ) {
                switch(cmpls[ci]) {
                case CMPLT_PC_SINGLE: opcode=0xA; x=1; break;
                case CMPLT_PC_DOUBLE: opcode=0xB; x=0; break;
                case CMPLT_SF0:       sf=0; break;
                case CMPLT_SF1:       sf=1; break;
                case CMPLT_SF2:       sf=2; break;
                case CMPLT_SF3:       sf=3; break;
                default: 
                    assert(0);
                    break;
                }
            }
            return F1(opcode, x, sf, 1, opnds[2]->getValue()
                , opnds[3]->getValue(), opnds[1]->getValue(), qp);
        }
    case INST_GETF_S:
        return M19(0x1e, opnds[2]->getValue(), opnds[1]->getValue(), qp);
    case INST_GETF_D:
        return M19(0x1f, opnds[2]->getValue(), opnds[1]->getValue(), qp);
    case INST_GETF_EXP:
        return M19(0x1d, opnds[2]->getValue(), opnds[1]->getValue(), qp);
    case INST_GETF_SIG:
        return M19(0x1c, opnds[2]->getValue(), opnds[1]->getValue(), qp);
    case INST_LD:
    case INST_LD16:
    case INST_LD16_ACQ:
    case INST_LD8_FILL:
        {
            uint64 x=0, x6=0x0, hint=0;
            
            for (int i=0 ; i<cmplcount ; i++ ) {
                switch (cmpls[i]) {
                case CMPLT_SZ_1: x6 = (x6 & 0x3C) | 0x00; break;
                case CMPLT_SZ_2: x6 = (x6 & 0x3C) | 0x01; break;
                case CMPLT_SZ_4: x6 = (x6 & 0x3C) | 0x02; break;
                case CMPLT_SZ_8: x6 = (x6 & 0x3C) | 0x03; break;
                case CMPLT_LDTYPE_NORMAL:    x6 = (x6 & 0x3) | (0x00 << 2); break;
                case CMPLT_LDTYPE_S:         x6 = (x6 & 0x3) | (0x01 << 2); break;
                case CMPLT_LDTYPE_A:         x6 = (x6 & 0x3) | (0x02 << 2); break;
                case CMPLT_LDTYPE_SA:        x6 = (x6 & 0x3) | (0x03 << 2); break;
                case CMPLT_LDTYPE_BIAS:      x6 = (x6 & 0x3) | (0x04 << 2); break;
                case CMPLT_LDTYPE_ACQ:       x6 = (x6 & 0x3) | (0x05 << 2); break;
                case CMPLT_LDTYPE_C_CLR:     x6 = (x6 & 0x3) | (0x08 << 2); break;
                case CMPLT_LDTYPE_C_NC:      x6 = (x6 & 0x3) | (0x09 << 2); break;
                case CMPLT_LDTYPE_C_CLR_ACQ: x6 = (x6 & 0x3) | (0x0A << 2); break;
                case CMPLT_HINT_NONE: hint=0; break;
                case CMPLT_HINT_NT1:  hint=1; break;
                case CMPLT_HINT_NTA:  hint=3; break;
                default: assert(0); break;
                }
            }
            if (icode==INST_LD16 ) {
                x6 = 0x28;
                x = 1;
            } else if (icode==INST_LD16_ACQ ) {
                x6 = 0x2C;
                x = 1;
            } else if (icode==INST_LD8_FILL ) {
                x6 = 0x1B;
                x = 0;
            }                
            if (opndcount>=4 && opnds[3]->isImm(9)) {
                return M3(x6, hint, opnds[2]->getValue(), opnds[3]->getValue()
                    , opnds[1]->getValue(), qp);
            } else if (opndcount>=4 && opnds[3]->getOpndKind()==OPND_G_REG) {
                return M2(x6, hint, opnds[2]->getValue(), opnds[3]->getValue()
                    , opnds[1]->getValue(), qp);
            } else {
                return M1(x6, hint, x, opnds[2]->getValue(), opnds[1]->getValue(), qp);
            }
        }

    case INST_LDF:
    case INST_LDF8:
    case INST_LDF_FILL:
        return ldfx(icode, inst, cmpls, opnds, qp);

    case INST_MOV:
    case INST_MOV_I:                 
    case INST_MOV_M:
    case INST_MOV_RET:
        return mov(unit, icode, inst, cmpls, opnds, qp);

    case INST_SETF_S:
        return M18(0x1e, opnds[2]->getValue(), opnds[1]->getValue(), qp);
    case INST_SETF_D:
        return M18(0x1f, opnds[2]->getValue(), opnds[1]->getValue(), qp);
    case INST_SETF_EXP:
        return M18(0x1d, opnds[2]->getValue(), opnds[1]->getValue(), qp);
    case INST_SETF_SIG:
        return M18(0x1c, opnds[2]->getValue(), opnds[1]->getValue(), qp);
    
    case INST_SHL:
        assert(opndcount>=4);
        if (opnds[3]->getOpndKind()==OPND_G_REG) {
            return I7(1, 1, opnds[3]->getValue(), opnds[2]->getValue()
                , opnds[1]->getValue(), qp);
        } else {
            // pos6 = count6; len6 = 64-pos6; len6 = len6d + 1; pos6 = 63 - cpos6c
            return I12(63-(uint64)opnds[3]->getValue(), 63-(uint64)opnds[3]->getValue()
                , opnds[2]->getValue(), opnds[1]->getValue(), qp);
        }
    case INST_SHLADD:
        return A2(4, opnds[3]->getValue() & 0x3, opnds[4]->getValue()
            , opnds[2]->getValue(), opnds[1]->getValue(), qp);
    case INST_SHR:
        assert(opndcount>=4);
        if (opnds[3]->getOpndKind()==OPND_G_REG) {
            return I5(1, 1, 2, opnds[2]->getValue(), opnds[3]->getValue()
                , opnds[1]->getValue(), qp);
        } else {
            return I11(64-opnds[3]->getValue(), opnds[2]->getValue()
                , opnds[3]->getValue(), 1, opnds[1]->getValue(), qp);
        }
    case INST_SHR_U:
        assert(opndcount>=4);
        if (opnds[3]->getOpndKind()==OPND_G_REG) {
            return I5(1, 1, 0, opnds[2]->getValue(), opnds[3]->getValue()
                , opnds[1]->getValue(), qp);
        } else {
             return I11(64-opnds[3]->getValue(), opnds[2]->getValue()
                , opnds[3]->getValue(), 0, opnds[1]->getValue(), qp);
        }

    case INST_ST: 
        {
            uint64 x6=0x30, hint=0;
            
            for (int i=0, ii=cmplcount ; i<ii ; i++) {
                switch (cmpls[i]) {
                case CMPLT_SZ_1: x6 = (x6 & 0x3C) | 0x00; break;
                case CMPLT_SZ_2: x6 = (x6 & 0x3C) | 0x01; break;
                case CMPLT_SZ_4: x6 = (x6 & 0x3C) | 0x02; break;
                case CMPLT_SZ_8: x6 = (x6 & 0x3C) | 0x03; break;
                case CMPLT_ST_TYPE_NORMAL: x6 = (x6 & 0x3) | (0x0C << 2);break;
                case CMPLT_ST_TYPE_REL:    x6 = (x6 & 0x3) | (0x0D << 2); break;
                case CMPLT_HINT_NONE: hint=0; break;
                case CMPLT_HINT_NTA:  hint=3; break;
                default: assert(0); break;
                }
            }
            if (opndcount==4  && opnds[3]->isImm(9)) {
                return M5(x6, hint, opnds[1]->getValue(), opnds[2]->getValue(), opnds[3]->getValue(), qp);
            } else {
                return M4(x6, hint, 0, opnds[1]->getValue(), opnds[2]->getValue(), qp);
            }
        }
    case INST_ST8_SPILL:
        if ( opndcount==3 ) {
            return M4(0x3B, (cmplcount>0 && cmpls[0]==CMPLT_HINT_NTA?3:0)
                , 0, opnds[1]->getValue(), opnds[2]->getValue(), qp);
        } else {
            return M5(0x3B, (cmplcount>0 && cmpls[0]==CMPLT_HINT_NTA?3:0)
                , opnds[1]->getValue(), opnds[2]->getValue(), opnds[3]->getValue(), qp);
        }
    case INST_ST16:
        {
            uint64 x6=0x30, hint=0;
            
            for (int i=0, ii=cmpls.size() ; i<ii ; i++) {
                switch (cmpls[i]) {
                case CMPLT_ST_TYPE_NORMAL: x6 = 0x30;break;
                case CMPLT_ST_TYPE_REL:    x6 = 0x34; break;
                case CMPLT_HINT_NONE: hint=0; break;
                case CMPLT_HINT_NTA:  hint=3; break;
                default: assert(0); break;
                }
            }
            return M4(x6, hint, 1, opnds[1]->getValue(), opnds[2]->getValue(), qp);
        }
    case INST_STF:
    case INST_STF8:
    case INST_STF_SPILL:
        {
            uint64 x6=0x32, hint=0;
            
            if (icode==INST_STF_SPILL) {
                x6=0x3B; 
            } else if (icode==INST_STF8) {
                x6=0x31;
            } else {
                switch (cmpls[0]) {
                case CMPLT_FSZ_S: x6=0x32; break;
                case CMPLT_FSZ_D: x6=0x33; break;
                case CMPLT_FSZ_E: x6=0x30; break;
                default: assert(0); break;
                }
            }
            if (cmplcount>=1 && cmpls[0]==CMPLT_HINT_NTA ) hint=3;
            else if (cmplcount>=2 && cmpls[1]==CMPLT_HINT_NTA ) hint=3;
            
            if (opnds.size()>=4 ) {
                return M10(x6, hint, opnds[1]->getValue(), opnds[2]->getValue(), opnds[3]->getValue(), qp);
            } else {
                return M9(x6, hint, opnds[1]->getValue(), opnds[2]->getValue(), qp);
            }
        }
        
    case INST_SXT: 
    case INST_ZXT:
        {
            uint64 x6 = 0xFF;
            
            switch (cmpls[0]) {
            case CMPLT_XSZ_1: x6 = 0x14; break;
            case CMPLT_XSZ_2: x6 = 0x15; break;
            case CMPLT_XSZ_4: x6 = 0x16; break;
            default: assert(0); break;
            }
            if (icode==INST_ZXT) x6 = x6 - 4;
            return I29(x6, opnds[2]->getValue(), opnds[1]->getValue(), qp);
        }

    case INST_XMA_L:
    case INST_XMA_LU:
        return F2(0, opnds[3]->getValue(), opnds[2]->getValue()
            , opnds[4]->getValue(), opnds[1]->getValue(), qp);
    case INST_XMA_H:
        return F2(3, opnds[3]->getValue(), opnds[2]->getValue()
            , opnds[4]->getValue(), opnds[1]->getValue(), qp);
    case INST_XMA_HU:
        return F2(2, opnds[3]->getValue(), opnds[2]->getValue()
            , opnds[4]->getValue(), opnds[1]->getValue(), qp);
    
    default:
        IPF_ERR << __FILE__ << ": " << __LINE__ 
                << " IpfEncoder ERROR: NOT YET IMPLEMENTED INSTRUCTION: " 
                << Encoder::getMnemonic(icode) << "\n";
        break;
    }

    return instbits;
}

uint64 Encoder::getInstBitsBranch(InstructionType unit, Inst * inst, int target) {
    InstCode   icode  = inst->getInstCode();
    OpndVector & opnds  = inst->getOpnds();
    uint64     qp     = opnds[0]->getValue();
    
    if( icode==INST_NOP ) return getNopBits(unit, qp, opnds[1]->getValue());
    if( icode==INST_HINT ) return getHintBits(unit, qp, opnds[1]->getValue());
    
    uint64 instbits  = 0;

    switch (icode) {
    case INST_BR13:
    case INST_BR: 
        {
            unsigned int is13 = (icode==INST_BR13 ? 1 : 0);  // must be 1 or 0
            CompVector & cmpls = inst->getComps();
            OpndKind okind1 = opnds[is13 + 1]->getOpndKind();
            uint64 o1 = opnds[is13 + 1]->getValue();
            uint64 cmplt_btype=CMPLT_BTYPE_COND, btype=0, ph=0, bwh=1, dh=0;
            
            for (int i=0, ii=cmpls.size() ; i<ii ; i++) {
                switch (cmpls[i]) {
                case CMPLT_BTYPE_COND:  btype=0; cmplt_btype=cmpls[i]; break;
                case CMPLT_BTYPE_IA:    btype=1; cmplt_btype=cmpls[i]; break;
                case CMPLT_BTYPE_RET:   btype=4; cmplt_btype=cmpls[i]; break;
                case CMPLT_BTYPE_CLOOP: btype=5; cmplt_btype=cmpls[i]; break;
                case CMPLT_BTYPE_CEXIT: btype=6; cmplt_btype=cmpls[i]; break;
                case CMPLT_BTYPE_CTOP:  btype=7; cmplt_btype=cmpls[i]; break;
                case CMPLT_BTYPE_WEXIT: btype=2; cmplt_btype=cmpls[i]; break;
                case CMPLT_BTYPE_WTOP:  btype=3; cmplt_btype=cmpls[i]; break;
                case CMPLT_BTYPE_CALL:  btype=(uint64)-1; cmplt_btype=cmpls[i]; break;
                //----------------------------
                // Branch_Whether_Hint, NOT for all branch instructions!
                case CMPLT_WH_SPTK: bwh=0; break;
                case CMPLT_WH_SPNT: bwh=1; break;
                case CMPLT_WH_DPTK: bwh=2; break;
                case CMPLT_WH_DPNT: bwh=3; break;
                //----------------------------
                // Branch_Sequential_Prefetch_Hint (br, brl)
                case CMPLT_PH_FEW:  ph=0; break;
                case CMPLT_PH_MANY: ph=1; break;
                //----------------------------
                // Branch_Cache_Deallocation_Hint (br, brl)
                case CMPLT_DH_NOT_CLR: dh=0; break;
                case CMPLT_DH_CLR:     dh=1; break;
                default: assert(0);
                }
            }
            
            if (cmplt_btype==CMPLT_BTYPE_CALL) {
                assert(opnds.size()>=(is13 + 3));
                OpndKind okind2 = opnds[is13 + 2]->getOpndKind();
                uint64 o2 = opnds[is13 + 2]->getValue();

                if (okind2==OPND_B_REG) {
                    return B5(ph, ((bwh%2)==0 ? 1 : bwh), dh, o2, o1, qp);
                } else if ( (opnds[is13 + 2])->getDataKind() == DATA_NODE_REF ) {
                    assert(target != 0);
                    return B3(ph, bwh, dh, target>>4, o1, qp);
                } else {
                    return B3(ph, bwh, dh, o2, o1, qp);
                }
            } else if (cmplt_btype==CMPLT_BTYPE_RET) {
                return B4(0, 0, 0x21, o1, 0, 4, qp);
            } else if (cmplt_btype==CMPLT_BTYPE_CLOOP 
                    || cmplt_btype==CMPLT_BTYPE_CEXIT
                    || cmplt_btype==CMPLT_BTYPE_CTOP) {
                if ( (opnds[is13 + 1])->getDataKind() == DATA_NODE_REF ) {
                    assert(target != 0);
                    return B1_B2(dh, bwh, ph, btype, target >> 4, qp);
                } else {
                    return B1_B2(dh, bwh, ph, btype, o1, qp);
                }
            } else if (okind1==OPND_B_REG) {
                return B4(dh, bwh, 0x20, o1, ph, btype, qp);
            } else {
                if ( (inst->getOpnd(is13 + 1))->getDataKind() == DATA_NODE_REF ) {
                    assert(target!=0);
                    return B1_B2(dh, bwh, ph, btype, target>>4, qp);
                } else {
                    return B1_B2(dh, bwh, ph, btype, o1, qp);
                }
            }
        }
        // fall to assert(0)        
    default:
        IPF_ERR << __FILE__ << ": " << __LINE__ 
            << ": NOT YET IMPLEMENTED INSTRUCTION: " 
            << Encoder::getMnemonic(icode) << "\n";
        assert(0);
        break;
    }
    
    return instbits;
}

uint64 * Encoder::getInstBitsExtended(Inst * inst, uint64 * slots12, void *whereToEmit) {
    InstCode     icode  = inst->getInstCode();
    OpndVector & opnds  = inst->getOpnds();
    CompVector & cmpls = inst->getComps();
    uint64       qp     = opnds[0]->getValue();

    switch (icode) {
    case INST_MOVL:
        assert(opnds.size()==3);
        return X2(0, opnds[1]->getValue(), opnds[2]->getValue(), qp, slots12);
    case INST_NOP:
        return X5(0, 0, qp, slots12);

    case INST_BRL13:
    case INST_BRL:
        {
            unsigned int is13 = (icode==INST_BRL13 ? 1 : 0);  // must be 1 or 0
            if (cmpls[0]==CMPLT_BTYPE_CALL) assert(opnds.size()>=(is13 + 3));
            else assert(opnds.size()>=(is13 + 2));
            
            // (qp) brl.btype.bwh.ph.dh b1 = target64
            // target64 = IP + ((i << 59 | imm39 << 20 | imm20b) << 4)
            uint64 d=0, wh=0, p=0;
            uint64 imm60 = 0;
            uint64 imm64 = 0;
            CompVector & cmpls = inst->getComps();
            
            if (opnds[is13 + 2]->isImm(64)) {
                imm64 = opnds[is13 + 2]->getValue();
                imm60 = (imm64 - (uint64)whereToEmit) >> 4;
            }
            for ( int i=0, ii=cmpls.size() ; i<ii ; i++ ) {
                switch (cmpls[i]) {
                case CMPLT_DH_CLR:  d=1; break;
                case CMPLT_WH_SPTK: wh=0; break;
                case CMPLT_WH_SPNT: wh=1; break;
                case CMPLT_WH_DPTK: wh=2; break;
                case CMPLT_WH_DPNT: wh=3; break;
                case CMPLT_PH_FEW:  p=0; break;
                case CMPLT_PH_MANY: p=1; break;
                default: break;
                }
            }
            
            if (cmpls[0]==CMPLT_BTYPE_CALL)
                return X4(imm60, d, wh, p, opnds[is13 + 1]->getValue(), qp, slots12);
            else
                return X3(imm60, d, wh, p, qp, slots12);
        }
        break;
    default:
        break;
    }
    
    IPF_ERR << __FILE__ << ": " << __LINE__ 
            << ": NOT YET IMPLEMENTED INSTRUCTION: " 
            << Encoder::getMnemonic(icode) << "\n";
    assert(0);
    return slots12;
}

//----------------------------------------------------------------------------//
uint64 Encoder::fcmp(InstCode icode, Inst * inst, CompVector & cmpls, OpndVector & opnds, uint64 qp) {
    assert(opnds.size()==5);
    int64 p1=opnds[1]->getValue(), p2=opnds[2]->getValue();
    int64 f2=opnds[3]->getValue(), f3=opnds[4]->getValue(), tmp;
    Completer frel = cmpls[0];
    Completer fctype = CMPLT_FCMP_FCTYPE_NONE;
    uint64 ra=0, rb=0, ta=0, sf = 0;
    
    switch (frel) {
    case CMPLT_FCMP_FREL_EQ: ra=0; rb=0; break;
    case CMPLT_FCMP_FREL_LT: ra=0; rb=1; break;
    case CMPLT_FCMP_FREL_LE: ra=1; rb=0; break;
    case CMPLT_FCMP_FREL_GT:    
        ra=0; rb=1; frel=CMPLT_FCMP_FREL_LT;
        tmp=f2; f2=f3; f3=tmp;
        break;
    case CMPLT_FCMP_FREL_GE:
        ra=1; rb=0; frel=CMPLT_FCMP_FREL_LE;
        tmp=f2; f2=f3; f3=tmp;
        break;
    case CMPLT_FCMP_FREL_UNORD: 
        ra=1; rb=1; 
        break;
    case CMPLT_FCMP_FREL_NEQ: 
        ra=0; rb=0; frel=CMPLT_FCMP_FREL_EQ;
        tmp=p1; p1=p2; p2=tmp;
        break;
    case CMPLT_FCMP_FREL_NLT:
        ra=0; rb=1; frel=CMPLT_FCMP_FREL_LT;
        tmp=p1; p1=p2; p2=tmp;
        break;
    case CMPLT_FCMP_FREL_NLE:
        ra=1; rb=0; frel=CMPLT_FCMP_FREL_LE;
        tmp=p1; p1=p2; p2=tmp;
        break;
    case CMPLT_FCMP_FREL_NGT:
        ra=0; rb=1; frel=CMPLT_FCMP_FREL_LT;
        tmp=f2; f2=f3; f3=tmp;
        tmp=p1; p1=p2; p2=tmp;
        break;
    case CMPLT_FCMP_FREL_NGE:
        ra=1; rb=0; frel=CMPLT_FCMP_FREL_LE;
        tmp=f2; f2=f3; f3=tmp;
        tmp=p1; p1=p2; p2=tmp;
        break;
    case CMPLT_FCMP_FREL_ORD:
        ra=1; rb=1; frel=CMPLT_FCMP_FREL_UNORD;
        tmp=p1; p1=p2; p2=tmp;
        break;
    default:
        assert(0);
        break;
    }
    if (cmpls.size()>2) {
        switch (cmpls[1]) {
        case CMPLT_FCMP_FCTYPE_NONE: ta=0; fctype = CMPLT_FCMP_FCTYPE_NONE; break;
        case CMPLT_FCMP_FCTYPE_UNC:  ta=1; fctype = CMPLT_FCMP_FCTYPE_UNC; break;
        default:               
            assert(0); break;
        }
        switch (cmpls[2]) {
        case CMPLT_SF0:        sf=0; break;
        case CMPLT_SF1:        sf=1; break;
        case CMPLT_SF2:        sf=2; break;
        case CMPLT_SF3:        sf=3; break;
        default:               
            assert(0); break;
        }
    } else if (cmpls.size()>1) {
        switch (cmpls[1]) {
        case CMPLT_FCMP_FCTYPE_NONE: ta=0; fctype = CMPLT_FCMP_FCTYPE_NONE; break;
        case CMPLT_FCMP_FCTYPE_UNC:  ta=1; fctype = CMPLT_FCMP_FCTYPE_UNC; break;
        case CMPLT_SF0:        sf=0; break;
        case CMPLT_SF1:        sf=1; break;
        case CMPLT_SF2:        sf=2; break;
        case CMPLT_SF3:        sf=3; break;
        default:               
            assert(0); break;
        }
    }
    return F4(rb, sf, ra, p2, f3, f2, ta, p1, qp);
}

//----------------------------------------------------------------------------//
uint64 Encoder::cmp_cmp4(InstCode icode, Inst * inst, CompVector & cmpls, OpndVector & opnds, uint64 qp) {
    assert(opnds.size()>=5);
    
    int64 p1=opnds[1]->getValue(), p2=opnds[2]->getValue();
    int64 r2=opnds[3]->getValue(), r3=opnds[4]->getValue();
    OpndKind okind3=opnds[3]->getOpndKind();
    Completer crel = cmpls[0];
    Completer ctype = ( cmpls.size()>1 ? cmpls[1] : CMPLT_INVALID );
    uint64 opcode=0, ta=0, c=0, x2;
    int64 tmp, & imm8 = r2;

    // parse pseudo-op    
    if ( cmpls.size() == 1 || ctype==CMPLT_CMP_CTYPE_UNC ) {
        if (opnds[3]->isImm(8)) {
            switch (crel) {
            case CMPLT_CMP_CREL_NE:
                crel=CMPLT_CMP_CREL_EQ; 
                tmp = p1; p1 = p2; p2 = tmp;   
                break;
            case CMPLT_CMP_CREL_LE:
                crel=CMPLT_CMP_CREL_LT;
                r2--;   
                break;
            case CMPLT_CMP_CREL_GT:
                crel=CMPLT_CMP_CREL_LT;
                r2--;   
                tmp = p1; p1 = p2; p2 = tmp;   
                break;
            case CMPLT_CMP_CREL_GE:
                crel=CMPLT_CMP_CREL_LT;
                tmp = p1; p1 = p2; p2 = tmp;   
                break;
            case CMPLT_CMP_CREL_LEU: 
                crel=CMPLT_CMP_CREL_LTU;
                r2--;   
                break;
            case CMPLT_CMP_CREL_GTU: 
                crel=CMPLT_CMP_CREL_LTU;
                r2--;   
                tmp = p1; p1 = p2; p2 = tmp;   
                break;
            case CMPLT_CMP_CREL_GEU: 
                crel=CMPLT_CMP_CREL_LTU;
                tmp = p1; p1 = p2; p2 = tmp;   
                break;
            default: 
                break;
            }
        } else {
            switch (crel) {
            case CMPLT_CMP_CREL_NE:
                crel=CMPLT_CMP_CREL_EQ; 
                tmp = p1; p1 = p2; p2 = tmp;   
                break;
            case CMPLT_CMP_CREL_LE:
                crel=CMPLT_CMP_CREL_LT;
                tmp = r3; r3 = r2; r2 = tmp;   
                tmp = p1; p1 = p2; p2 = tmp;   
                break;
            case CMPLT_CMP_CREL_GT:
                crel=CMPLT_CMP_CREL_LT;
                tmp = r3; r3 = r2; r2 = tmp;   
                break;
            case CMPLT_CMP_CREL_GE:
                crel=CMPLT_CMP_CREL_LT;
                tmp = p1; p1 = p2; p2 = tmp;   
                break;
            case CMPLT_CMP_CREL_LEU: 
                crel=CMPLT_CMP_CREL_LTU;
                tmp = r3; r3 = r2; r2 = tmp;   
                tmp = p1; p1 = p2; p2 = tmp;   
                break;
            case CMPLT_CMP_CREL_GTU: 
                crel=CMPLT_CMP_CREL_LTU;
                tmp = r3; r3 = r2; r2 = tmp;   
                break;
            case CMPLT_CMP_CREL_GEU: 
                crel=CMPLT_CMP_CREL_LTU;
                tmp = p1; p1 = p2; p2 = tmp;   
                break;
            default: 
                break;
            }
        }
    } else if ( crel!=CMPLT_CMP_CREL_EQ && crel!=CMPLT_CMP_CREL_NE ) {
        if ( crel==CMPLT_CMP_CREL_LT && r2>0 ) {
            crel=CMPLT_CMP_CREL_GT;
            tmp = r3; r3 = r2; r2 = tmp;   
        } else if ( crel==CMPLT_CMP_CREL_LE && r2>0 ) {
            crel=CMPLT_CMP_CREL_GE;
            tmp = r3; r3 = r2; r2 = tmp;   
        } else if ( crel==CMPLT_CMP_CREL_GT && r2>0 ) {
            crel=CMPLT_CMP_CREL_LT;
            tmp = r3; r3 = r2; r2 = tmp;   
        } else if ( crel==CMPLT_CMP_CREL_GE && r2>0 ) {
            crel=CMPLT_CMP_CREL_LE;
            tmp = r3; r3 = r2; r2 = tmp;   
        }
    }

    switch (ctype) {
    case CMPLT_CMP_CTYPE_AND:      opcode=0xC; break;
    case CMPLT_CMP_CTYPE_OR:       opcode=0xD; break;
    case CMPLT_CMP_CTYPE_OR_ANDCM: opcode=0xE; break;
    default: 
        switch (crel) {
        case CMPLT_CMP_CREL_LT:  opcode=0xC; break;
        case CMPLT_CMP_CREL_LTU: opcode=0xD; break;
        case CMPLT_CMP_CREL_EQ:  opcode=0xE; break;
        default: 
            IPF_ERR << __FILE__ << ": " << __LINE__ << ": BAD ctype/crel\n";
            assert(0);
            break;
        }
    }

    switch (crel) {
    case CMPLT_CMP_CREL_NE:  ta=1; c=1; break;
    case CMPLT_CMP_CREL_GE:  ta=1; c=0; break;
    case CMPLT_CMP_CREL_LE:  ta=0; c=1; break;
    case CMPLT_CMP_CREL_GT:  ta=0; c=0; break;
    case CMPLT_CMP_CREL_LTU: ta=0; c=(ctype==CMPLT_CMP_CTYPE_UNC ? 1 : 0); break;
    case CMPLT_CMP_CREL_EQ:  
        if ( cmpls.size()==1 ) { ta=0; c=0; }
        else if ( ctype==CMPLT_CMP_CTYPE_UNC ) { ta=0; c=1; }
        else { ta=1; c=0; }
        break;
    case CMPLT_CMP_CREL_LT:  
        if ( cmpls.size()==1 ) { ta=0; c=0; }
        else if ( ctype==CMPLT_CMP_CTYPE_UNC ) { ta=0; c=1; }
        else { ta=1; c=1; }
        break;
    default: 
        IPF_ERR << __FILE__ << ": " << __LINE__ << ": BAD ctype/crel\n";
        assert(0);
        break;
    }
    
    if ( (ctype==CMPLT_CMP_CTYPE_NORMAL || ctype==CMPLT_CMP_CTYPE_UNC 
            || crel==CMPLT_CMP_CREL_NE || crel==CMPLT_CMP_CREL_EQ || cmpls.size()==1) 
            && okind3==OPND_G_REG ) {
        x2 = icode==INST_CMP ? 0 : 1;
        return A6(opcode, x2, ta, c, p1, p2, r2, r3, qp);
    } else if (opnds[3]->isImm(8)) {
        x2 = icode==INST_CMP ? 2 : 3;
        return A8(opcode, x2, ta, c, p1, p2, imm8, r3, qp);
    } else {
        assert(r2==0);
        x2 = icode==INST_CMP ? 0 : 1;
        return A7(opcode, x2, ta, c, p1, p2, r3, qp);
    }
    
}

//----------------------------------------------------------------------------//
uint64 Encoder::ldfx(InstCode icode, Inst * inst, CompVector & cmpls, OpndVector & opnds, uint64 qp) {
    uint64 x6=0x02, hint=0;
    int opndcount = opnds.size();
    int cmplcount = cmpls.size();
    
    for (int i=0 ; i<cmplcount ; i++ ) {
        switch (cmpls[i]) {
        case CMPLT_FSZ_E: x6 = (x6 & 0x3C) | 0x00; break;
        case CMPLT_FSZ_S: x6 = (x6 & 0x3C) | 0x02; break;
        case CMPLT_FSZ_D: x6 = (x6 & 0x3C) | 0x03; break;
        case CMPLT_LDTYPE_NORMAL:    x6 = (x6 & 0x3) | (0x00 << 2); break;
        case CMPLT_LDTYPE_S:         x6 = (x6 & 0x3) | (0x01 << 2); break;
        case CMPLT_LDTYPE_A:         x6 = (x6 & 0x3) | (0x02 << 2); break;
        case CMPLT_LDTYPE_SA:        x6 = (x6 & 0x3) | (0x03 << 2); break;
        case CMPLT_LDTYPE_C_CLR:     x6 = (x6 & 0x3) | (0x08 << 2); break;
        case CMPLT_LDTYPE_C_NC:      x6 = (x6 & 0x3) | (0x09 << 2); break;
        case CMPLT_HINT_NONE: hint=0; break;
        case CMPLT_HINT_NT1:  hint=1; break;
        case CMPLT_HINT_NTA:  hint=3; break;
        default: assert(0); break;
        }
    }
    
    if (icode==INST_LDF_FILL ) {
        x6 = 0x1B;
    } else if (icode==INST_LDF8 ) {
        x6 = (x6 & 0x3C) | 0x01;
    }                
    if (opndcount>=4 && opnds[3]->isImm(9)) {
        return M8(x6, hint, opnds[2]->getValue(), opnds[3]->getValue()
            , opnds[1]->getValue(), qp);
    } else if (opndcount>=4 && opnds[3]->getOpndKind()==OPND_G_REG) {
        return M7(x6, hint, opnds[2]->getValue(), opnds[3]->getValue()
            , opnds[1]->getValue(), qp);
    } else {
        return M6(x6, hint, opnds[2]->getValue(), opnds[1]->getValue(), qp);
    }
}

//----------------------------------------------------------------------------//
uint64 Encoder::mov(InstructionType unit, InstCode icode, Inst * inst, CompVector & cmpls, OpndVector & opnds, uint64 qp) {
    OpndKind okind1 = opnds[1]->getOpndKind();
    OpndKind okind2 = opnds[2]->getOpndKind();
    uint64 o1 = opnds[1]->getValue();
    uint64 o2 = opnds[2]->getValue();
    
    if (okind1 == OPND_A_REG && (okind2 == OPND_G_REG || opnds[2]->isImm(8))) {
        if (unit==IT_I || o1==64) icode=INST_MOV_I;
        else if (unit==IT_M) icode=INST_MOV_M;
        else assert(0);
    }
    if (icode==INST_MOV_I) {
             if (okind1==OPND_G_REG) return I28(o2, o1, qp);
        else if (okind2==OPND_G_REG) return I26(o1, o2, qp);
        else                         return I27(o1, o2, qp);
    } else if (icode==INST_MOV_M) {
             if (okind1==OPND_G_REG) return M31(o2, o1, qp);
        else if (okind2==OPND_G_REG) return M29(o1, o2, qp);
        else                         return M30(o1, o2, qp);
    } else if (icode==INST_MOV) {
        if (opnds.size()>=3) {
            if (okind1==OPND_B_REG) {
                uint64 timm9c=0, ih=0, wh=1;
                
                if (opnds.size()>3) 
                    timm9c = opnds[3]->getValue();
                for (int i=0, ii=cmpls.size() ; i<ii ; i++){
                    switch (cmpls[i]) {
                    case CMPLT_IH_IMP:  ih=1; break;
                    case CMPLT_WH_DPTK: wh=2; break;
                    case CMPLT_WH_SPTK: wh=0; break;
                    default: break;
                    }
                }
                return I21(timm9c, ih, 0, wh, o2, o1, qp);
            } else if (okind2==OPND_B_REG) {
                return I22(o2, o1, qp);
            }
            /*
            if (okind1==OPND_F_REG && okind2==OPND_F_REG)   return IT_F;
            if (okind1==OPND_G_REG && okind2==OPND_IP_REG)  return IT_I;
            if (okind1==OPND_G_REG && okind2==OPND_P_REG)   return IT_I;
            if (okind1==OPND_P_REG && okind2==OPND_G_REG)   return IT_I;
            if (okind1==OPND_P_REG && okind2==OPND_IMM44)   return IT_I;
            if (okind1==OPND_UM_REG && okind2==OPND_UM_REG) return IT_M;
            if (okind1==OPND_G_REG && okind2==OPND_G_REG && cmpls.size()==0)
                return IT_A;
            if (okind1==OPND_G_REG && okind2==OPND_G_REG && cmpls.size()==1 
                    && cmpls[0]>=CMPLT_IREG_FIRST && cmpls[0]<=CMPLT_IREG_LAST)
                return IT_M;
            */
        }
    }
    IPF_ERR << __FILE__ << ": " << __LINE__ 
            << ": NOT YET IMPLEMENTED INSTRUCTION: " 
            << Encoder::getMnemonic(icode) << "\n";
    assert(0);

    return 0x0123456789ABCDEF;
}

//----------------------------------------------------------------------------//
void Encoder::readBundle(uint64 *code, uint64 * tmplt, uint64 * slots) 
{
    *tmplt = code[0] & 0x01F;
    slots[0] = (code[0] >> 5) & 0x1FFFFFFFFFF;
    slots[1] = (code[0] >> 46) | ((code[1] & 0x7FFFFF) << 18);
    slots[2] = (code[1] >> 23);
}

//----------------------------------------------------------------------------//
bool Encoder::patchCallAddr(char * callAddr, char * methodAddr)
{
    uint64   p[2];
    uint64   s[3];
    uint64   t;

    // init
    p[0] = ((uint64 *)callAddr)[0];
    p[1] = ((uint64 *)callAddr)[1];
    readBundle(p, &t, s);

    // target64 = IP + ((i << 59 | imm39 << 20 | imm20b) << 4)
    uint64 i, imm39, imm20b, imm60;
    
    imm60 = ((methodAddr - callAddr) >> 4) & 0xFFFFFFFFFFFFFFF;
    i = imm60 >> 59;
    imm39 = (imm60 >> 20) & 0x7FFFFFFFFF;
    imm20b = imm60 & 0xFFFFF;
    
    s[2] = s[2] & ~((uint64)0x1 << 36);
    s[2] |= i << 36;
    
    s[1] = imm39 << 2;
    
    s[2] = s[2] & ~((uint64)0xFFFFF << 13);
    s[2] |= imm20b << 13;

    // write to buffer
    p[0] = t | (s[0] << 5);
    p[0] |= s[1] << 46;
    p[1] = s[1] >> 18;
    p[1] |= s[2] << 23;

    // write to callAddr
    VMInterface::rewriteCodeBlock((U_8*)callAddr, (U_8*)p, IPF_BUNDLE_SIZE);

//    IPF_LOG << "Patch brl.call to 0x" << hex
//            << (uint64)methodAddr << " at 0x" << (uint64)callAddr << "\n"
//            << dec << "\n";
    return true;
}

} // IPF
} // Jitrino
