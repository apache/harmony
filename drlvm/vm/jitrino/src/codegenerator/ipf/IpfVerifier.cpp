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
#include "IpfVerifier.h"

#define O(n)        ((opnds)[n])
#define K(n)        ((opnds)[n]->getOpndKind())
#define V(n)        ((opnds)[n]->getValue())
#define C(n)        ((cmpls)[n])
#define GR(n)       ((RegOpnd *)O(n))

#define IS_O(n)     ((opnds).size()>=(n+1))
#define IS_C(n)     ((cmpls).size()>=(n+1))
#define IS_AR(n)    (IS_O(n) && K(n)==OPND_A_REG)
#define IS_GR(n)    (IS_O(n) && K(n)==OPND_G_REG)
#define IS_FR(n)    (IS_O(n) && K(n)==OPND_F_REG)
#define IS_PR(n)    (IS_O(n) && K(n)==OPND_P_REG)
#define IS_BR(n)    (IS_O(n) && K(n)==OPND_B_REG)
#define IS_IMM(n)   (IS_O(n) && O(n)->isImm())
#define IS_IMM6(n)  (IS_IMM(n) && Opnd::isFoldableImm(V(n), 6))
#define IS_IMM8(n)  (IS_IMM(n) && Opnd::isFoldableImm(V(n), 8))
#define IS_IMM9(n)  (IS_IMM(n) && Opnd::isFoldableImm(V(n), 9))
#define IS_IMM13(n) (IS_IMM(n) && Opnd::isFoldableImm(V(n), 13))
#define IS_IMM14(n) (IS_IMM(n) && Opnd::isFoldableImm(V(n), 14))
#define IS_IMM22(n) (IS_IMM(n) && Opnd::isFoldableImm(V(n), 22))
#define IS_IMM25(n) (IS_IMM(n) && Opnd::isFoldableImm(V(n), 25))
#define IS_IMM64(n) (IS_IMM(n) && Opnd::isFoldableImm(V(n), 64))

#define IS_CONST_REF(n)  (IS_O(n) && O(n)->isImm() && (O(n)->getDataKind()==DATA_CONST_REF))
#define IS_NODE_REF(n)   (IS_O(n) && O(n)->isImm() && (O(n)->getDataKind()==DATA_NODE_REF))
#define IS_SWITCH_REF(n) (IS_O(n) && O(n)->isImm() && (O(n)->getDataKind()==DATA_SWITCH_REF))
#define IS_REF(n)        (IS_CONST_REF(n) || IS_NODE_REF(n) || IS_SWITCH_REF(n))
                                                                                                
namespace Jitrino {
namespace IPF {

IpfVerifier::IpfVerifier(Cfg & cfg_, CompilationInterface & compilationinterface_) :
    cfg(cfg_), compilationinterface(compilationinterface_), mm(cfg_.getMM())
{
    methodDesc = compilationinterface.getMethodToCompile();
    methodString = new(mm) string("");
    if (methodDesc != NULL) {
        methodString->append((methodDesc->getParentType()!=NULL
            ? methodDesc->getParentType()->getName()
            : ""));
        methodString->append(".");
        methodString->append(methodDesc->getName());
        methodString->append(methodDesc->getSignatureString());
    }
}

//----------------------------------------------------------------------------//

bool IpfVerifier::verifyMethod(char * str) {
    bool ret = true;

    ret = ret && verifyMethodInsts();
    ret = ret && verifyMethodNodes();
    ret = ret && verifyMethodEdges();
    
    return ret;
}

//----------------------------------------------------------------------------//

bool IpfVerifier::verifyMethodInsts(char * str) {
    BbNode  * node = (BbNode *)cfg.getEnterNode();
    bool ret = true;

    do {
        ret = ret && verifyNodeInsts(node);
    } while( (node = node->getLayoutSucc()) != NULL );
    
    return ret;
}

//----------------------------------------------------------------------------//

bool IpfVerifier::verifyMethodNodes(char * str) {
    
    uint16 size1 = cfg.search(SEARCH_DIRECT_ORDER).size();
    uint16 size2 = cfg.search(SEARCH_POST_ORDER).size();
    
    if(size1 != size2) {
        cerr << "VERIFY ERROR: " << *methodString << " SEARCH_DIRECT_ORDER gives size " << size1;
        cerr << " SEARCH_POST_ORDER gives size " << size2 << endl;
        return false;
    }
    
    return true;
}

//----------------------------------------------------------------------------//

bool IpfVerifier::verifyMethodEdges(char * str) {
    BbNode* node = (BbNode*) cfg.getEnterNode();
    bool ret = true;

    do {
        ret = ret && verifyNodeEdges(node);
    } while( (node = node->getLayoutSucc()) != NULL );
    
    return ret;
}

//----------------------------------------------------------------------------//

bool IpfVerifier::verifyNodeInsts(BbNode  * node) {
    InstVector & insts = node->getInsts();
    Inst * inst;
    bool ret = true;
    string * res = new(mm) string();

    for (int i=0, ii=insts.size() ; i<ii ; i++ ) {
        inst = insts[i];
        res->erase();
        if( !verifyInst(res, inst) ) {
            cerr << "VERIFY METHOD ERROR: " << *methodString;
            cerr << "; node id: " << node->getId()
                << ", inst index: " << i
                << ", inst code: " << IrPrinter::toString(inst)
                << (res->empty() ? "" : " : ") << *res
                << "\n";
            ret = false;
#ifndef NDEBUG
            verifyInst(res, inst);  // verify again for debugger
#endif
        }
    }

    return ret;
}

//----------------------------------------------------------------------------//

bool IpfVerifier::verifyInst(string * res, Inst * inst) {
    InstCode   icode  = inst->getInstCode();
    OpndVector & opnds = inst->getOpnds();
    CompVector & cmpls = inst->getComps();

    if ( !IS_PR(0) ) return false;

    switch (icode) {
    // special cases
    case INST_NOP:
        return true;
    case INST_HINT:
        return true;
    case INST_BREAK:
        return true;
    case INST_BREAKPOINT:
        return true;

    case INST_BR13:
    case INST_BRL13: 
    case INST_BR:
    case INST_BRL:
    case INST_BRP:
    case INST_BRP_RET:
        return br(res, inst, icode, opnds, cmpls);
        
    case INST_ADD:
        if (!IS_O(3)) return false;
        if (IS_GR(1) && IS_GR(2) && IS_GR(3)) return true;
        if (IS_GR(1) && IS_IMM14(2) && IS_GR(3)) return true;
        if (IS_GR(1) && IS_IMM22(2) && IS_GR(3)) {
            if (V(2)<=3 && V(2)>=0) return true;
        }
        break;;
    case INST_ADDS:
        if (!IS_O(3)) return false;
        if (IS_GR(1) && IS_IMM14(2) && IS_GR(3)) return true;
        break;;
    case INST_ADDL: // opnds[2]->getOpndKind() == OPND_IMM22
        if (!IS_O(3)) return false;
        if (IS_GR(1) && IS_IMM22(2) && IS_GR(3)) {
            if (opnds[2]->getValue()<=3 && opnds[2]->getValue()>=0) return true;
        }
        break;;
    case INST_ADDP4:
        if (!IS_O(3)) return false;
        if (IS_GR(1) && IS_GR(2) && IS_GR(3)) return true;
        if (IS_GR(1) && IS_IMM14(2) && IS_GR(3)) return true;
        break;
    case INST_ALLOC:
        if (!IS_O(5)) return false;
        if (IS_GR(1) && IS_IMM(2) && IS_IMM(3) && IS_IMM(4) && IS_IMM(5)) {
            if (V(0)!=0) return false;
            if ((V(2) + V(3) + V(4))>96) return false;
            if (V(5)>(V(2) + V(3) + V(4))) return false;
            if ((V(2) + V(3))>(V(2) + V(3) + V(4))) return false;
            if (V(5)%8 != 0) return false;
            return true;
        }
        break;
    case INST_AND: 
    case INST_ANDCM:
    case INST_OR:
    case INST_XOR:
        if (!IS_O(3)) return false;
        if (IS_GR(1) && IS_GR(2) && IS_GR(3)) return true;
        if (IS_GR(1) && IS_IMM8(2) && IS_GR(3)) return true;
        break;
    case INST_SUB:
        if (!IS_O(3)) return false;
        if (IS_GR(1) && IS_GR(2) && IS_GR(3) && IS_O(4)) {
            if (IS_IMM(4) && V(4)==1) return true;
            return false;
        }
        if (IS_GR(1) && IS_GR(2) && IS_GR(3)) return true;
        if (IS_GR(1) && IS_IMM8(2) && IS_GR(3)) return true;
        break;
    case INST_CMP:
    case INST_CMP4:
        return cmp_cmp4(res, inst, icode, opnds, cmpls);
    case INST_FABS:
        if (!IS_O(2)) return false;
        if (IS_FR(1) && IS_FR(2)) return true;
        break;
    case INST_FADD:
        {
            uint64 opcode=8, x=0, sf=0;
            
            if (!IS_O(3) || IS_O(4)) return false;
            if (!(IS_FR(1) && IS_FR(2) && IS_FR(3))) return false;
            for (uint64 i=0 ; IS_C(i) ; i++ ) {
                switch (C(i)) {
                case CMPLT_PC_DYNAMIC: x=0; opcode=8; break;
                case CMPLT_PC_SINGLE:  x=1; opcode=8; break;
                case CMPLT_PC_DOUBLE:  x=0; opcode=9; break;
                case CMPLT_SF0: sf=0; break;
                case CMPLT_SF1: sf=1; break;
                case CMPLT_SF2: sf=2; break;
                case CMPLT_SF3: sf=3; break;
                default: return false;
                }
            }
            return true;
        }
    case INST_FAND:
    case INST_FANDCM:
        if ( !IS_O(3) || IS_O(4)) return false;
        if (IS_FR(1) && IS_FR(2) && IS_FR(3)) return true;
        break;
    case INST_FC:
    case INST_FC_I:
        if (IS_GR(1) && !IS_O(2)) return true;
        break;
    case INST_FCMP:
        return fcmp(res, inst, icode, opnds, cmpls);
    case INST_FCLASS:
        if (!IS_O(4) || IS_O(5)) return false;
        for (unsigned int i=0 ; IS_C(i) ; ++i) {
            if ( !(C(i)==CMPLT_FCMP_FCTYPE_UNC
                    || C(i)==CMPLT_FCMP_FCTYPE_NONE
                    || C(i)==CMPLT_FCMP_FCTYPE_NORMAL
                    || C(i)==CMPLT_FCREL_NM
                    || C(i)==CMPLT_FCREL_M) ) {
                return false;
            }
        }
        if (IS_PR(1) && IS_PR(2) && IS_FR(3) && IS_IMM9(4)) return true;
        
        break;
    case INST_FCVT_FX: 
    case INST_FCVT_FX_TRUNC: 
    case INST_FCVT_FXU: 
    case INST_FCVT_FXU_TRUNC: 
        if ( IS_C(0) ) {
            switch(C(0)) {
            case CMPLT_SF0:
            case CMPLT_SF1:
            case CMPLT_SF2:
            case CMPLT_SF3:
                break;
            default:
                return false;
            }
        }
        if (IS_FR(1) && IS_FR(2)) return true;
        break;
    case INST_FCVT_XF:
        if (IS_FR(1) && IS_FR(2)) return true;
        break;
    case INST_FCVT_XUF:
        if (IS_C(2)) return false;
        for (U_32 i=0 ; IS_C(i) ; i++) {
            switch (C(i)) {
            case CMPLT_PC_SINGLE:  
            case CMPLT_PC_DOUBLE:  
            case CMPLT_PC_DYNAMIC:
            case CMPLT_SF0:
            case CMPLT_SF1:
            case CMPLT_SF2:
            case CMPLT_SF3:
                break;
            default:
                return false;
            }
        }
        if (IS_FR(1) && IS_FR(2)) return true;
        break;
    case INST_FMA:
        for (U_32 i=0 ; IS_C(i) ; i++) {
            switch (C(i)) {
            case CMPLT_PC_SINGLE:  
            case CMPLT_PC_DOUBLE:  
            case CMPLT_PC_DYNAMIC:
            case CMPLT_SF0:
            case CMPLT_SF1:
            case CMPLT_SF2:
            case CMPLT_SF3:
                break;
            default:
                return false;
            }
        }
        if (IS_FR(1) && IS_FR(2) && IS_FR(3) && IS_FR(4)) return true;
        break;
    case INST_FMIN:
    case INST_FMAX:
    case INST_FAMIN:
    case INST_FAMAX:
    case INST_FPMIN:
    case INST_FPMAX:
    case INST_FPAMIN:
    case INST_FPAMAX:
        for (U_32 i=0 ; IS_C(i) ; i++) {
            switch (C(i)) {
            case CMPLT_SF0:
            case CMPLT_SF1:
            case CMPLT_SF2:
            case CMPLT_SF3:
                break;
            default:
                return false;
            }
        }
        if (IS_FR(1) && IS_FR(2) && IS_FR(3)) return true;
        break;
    case INST_FPCMP:
        for (U_32 i=0 ; IS_C(i) ; i++) {
            switch (C(i)) {
            case CMPLT_SF0:
            case CMPLT_SF1:
            case CMPLT_SF2:
            case CMPLT_SF3:
            case CMPLT_FCMP_FREL_EQ:
            case CMPLT_FCMP_FREL_GT:
            case CMPLT_FCMP_FREL_LT:
            case CMPLT_FCMP_FREL_GE:
            case CMPLT_FCMP_FREL_LE:
            case CMPLT_FCMP_FREL_UNORD:
            case CMPLT_FCMP_FREL_NEQ:
            case CMPLT_FCMP_FREL_NGT:
            case CMPLT_FCMP_FREL_NLT:
            case CMPLT_FCMP_FREL_NGE:
            case CMPLT_FCMP_FREL_NLE:
            case CMPLT_FCMP_FREL_ORD:
                break;
            default:
                return false;
            }
        }
        if (IS_FR(1) && IS_FR(2) && IS_FR(3) && !IS_O(4)) return true;
        break;
    case INST_FMERGE_NS:
    case INST_FMERGE_S:
    case INST_FMERGE_SE:
        if (IS_FR(1) && IS_FR(2) && IS_FR(3) && !IS_O(4)) return true;
        break;
    case INST_FNEG:
        if (IS_FR(1) && IS_FR(2) && !IS_O(3) && !IS_C(0)) return true;
        break;
    case INST_FNMA:
        for ( U_32 i=0 ; IS_C(i) ; i++ ) {
            switch(C(i)) {
            case CMPLT_PC_SINGLE:
            case CMPLT_PC_DOUBLE:
            case CMPLT_PC_DYNAMIC:
            case CMPLT_SF0:
            case CMPLT_SF1:
            case CMPLT_SF2:
            case CMPLT_SF3:
                break;
            default:
                return false;
            }
        }
        if (IS_FR(1) && IS_FR(2) && IS_FR(3) && IS_FR(4) && !IS_O(5)) return true;
        break;
    case INST_FNORM:
        for ( U_32 i=0 ; IS_C(i) ; i++ ) {
            switch(C(i)) {
            case CMPLT_PC_SINGLE:
            case CMPLT_PC_DOUBLE:
            case CMPLT_PC_DYNAMIC:
            case CMPLT_SF0:
            case CMPLT_SF1:
            case CMPLT_SF2:
            case CMPLT_SF3:
                break;
            default:
                return false;
            }
        }
        if (IS_FR(1) && IS_FR(2) && !IS_FR(3)) return true;
        break;
    case INST_FSUB:
        if (IS_FR(1) && IS_FR(2) && IS_FR(3) && !IS_O(4) && !IS_C(2)) return true;
        break;
    case INST_FRCPA:
        if ( IS_C(0) ) {
            switch(C(0)) {
            case CMPLT_SF0:
            case CMPLT_SF1:
            case CMPLT_SF2:
            case CMPLT_SF3:
                break;
            default: 
                return false;
            }
        }
        if (IS_FR(1) && IS_PR(2) && IS_FR(3) && IS_FR(4) && !IS_O(5)) return true;
        break;
    case INST_GETF_S:
    case INST_GETF_D:
    case INST_GETF_EXP:
    case INST_GETF_SIG:
        if (IS_GR(1) && IS_FR(2) && !IS_O(3)) return true;
        break;
    case INST_LD:
    case INST_LD16:
    case INST_LD16_ACQ:
    case INST_LD8_FILL:
        return ldx(res, inst, icode, opnds, cmpls);
    case INST_LDF:
    case INST_LDF8:
    case INST_LDF_FILL:
        return ldfx(res, inst, icode, opnds, cmpls);
    case INST_MOV:
    case INST_MOV_I:                 
    case INST_MOV_M:
    case INST_MOV_RET:
        return mov(res, inst, icode, opnds, cmpls);
    case INST_MOVL:
        if (IS_GR(1) && IS_IMM64(2)) return true;
        break;
    case INST_SETF_S:
    case INST_SETF_D:
    case INST_SETF_EXP:
    case INST_SETF_SIG:
        if (!IS_O(3) && !IS_C(0) && IS_FR(1) && IS_GR(2)) return true;
        break;
    case INST_SHL:
    case INST_SHR:
    case INST_SHR_U:
        if (!IS_C(0) && !IS_O(4) && IS_GR(1) && IS_GR(2) 
            && (IS_GR(3) || (IS_IMM(3) && (V(3)&0x3F)==V(3)))) return true;
        break;
    case INST_SHLADD:
        if (V(3)>4 || V(3)<1) { res->append("shift value must be 1...4"); return false; }
        if (!IS_C(0) && !IS_O(5) && IS_GR(1) && IS_GR(2) && IS_GR(4)) return true;
        break;
    case INST_ST: 
    case INST_ST8_SPILL:
    case INST_ST16:
        return stx(res, inst, icode, opnds, cmpls);
    case INST_STF:
    case INST_STF8:
    case INST_STF_SPILL:
        for (uint64 i=0 ; IS_C(i) ; i++) {
            switch (C(i)) {
            case CMPLT_FSZ_D:
            case CMPLT_FSZ_E:
            case CMPLT_FSZ_S:
                if (icode==INST_STF && i!=0) return false;
                break;
            case CMPLT_HINT_NONE:
            case CMPLT_HINT_NTA: 
                if (icode!=INST_STF && i!=0) return false;
                break;
            default:
                return false;
            }
        }
        if (!IS_C(2) && IS_GR(1) && IS_FR(2) && (!IS_O(3) || IS_IMM9(3))) return true;
        break;
    case INST_SXT:
    case INST_ZXT:
        if (!IS_C(0)) return false;
        switch (C(0)) {
        case CMPLT_XSZ_1:
        case CMPLT_XSZ_2:
        case CMPLT_XSZ_4: 
            break;
        default: 
            return false;
        }
        if (!IS_C(1) && IS_GR(1) && IS_GR(2)) return true;
        break;
    case INST_DEF:
    case INST_USE:
        return true;
    case INST_XMA_L:
    case INST_XMA_LU:
    case INST_XMA_H:
    case INST_XMA_HU:
        if (!IS_C(0) && IS_FR(1) && IS_FR(2) && IS_FR(3) && IS_FR(4)) return true;
        break;
    
    default:
        IPF_ERR << ": NOT YET IMPLEMENTED VERIFIER FOR INSTRUCTION: " 
                << IrPrinter::toString(inst) << "\n";
        return true;;
    }

    return false;
}

bool IpfVerifier::br(string * res, Inst * inst, InstCode icode, OpndVector & opnds, CompVector & cmpls) {
    unsigned int is13 = (icode==INST_BR13 || icode==INST_BRL13 ? 1 : 0);  // must be 1 or 0
    
    switch (icode) {
    case INST_BRL13:
    case INST_BRL:
        if (IS_C(0) && C(0)==CMPLT_BTYPE_CALL) {
            if (!IS_O(is13 + 2)) return false;
            if (!IS_IMM64(is13 + 2)) return false;
            if (!IS_BR(is13 + 1)) return false;
        } else {
            if (!IS_O(is13 + 1)) return false;
            if (!IS_IMM64(is13 + 1)) return false;
        }
        for (uint64 i=1 ; IS_C(i) ; i++ ) {
            switch (C(i)) {
            case CMPLT_DH_CLR:
            case CMPLT_WH_SPTK: 
            case CMPLT_WH_SPNT: 
            case CMPLT_WH_DPTK: 
            case CMPLT_WH_DPNT: 
            case CMPLT_PH_FEW:  
            case CMPLT_PH_MANY: 
                break;
            default: 
                return false;
            }
        }
        return true;
        
    case INST_BR:
    case INST_BR13:
        if (!IS_O(is13 + 1)) return false;
        {
            OpndKind okind1 = K(is13 + 1);
            uint64 cmplt_btype=CMPLT_BTYPE_COND, btype=0, ph=0, bwh=0, dh=0;
            
            for (uint64 i=0 ; IS_C(i) ; i++) {
                switch (C(i)) {
                case CMPLT_BTYPE_COND:  btype=0; cmplt_btype=C(i); break;
                case CMPLT_BTYPE_IA:    btype=1; cmplt_btype=C(i); break;
                case CMPLT_BTYPE_RET:   btype=4; cmplt_btype=C(i); break;
                case CMPLT_BTYPE_CLOOP: btype=5; cmplt_btype=C(i); break;
                case CMPLT_BTYPE_CEXIT: btype=6; cmplt_btype=C(i); break;
                case CMPLT_BTYPE_CTOP:  btype=7; cmplt_btype=C(i); break;
                case CMPLT_BTYPE_WEXIT: btype=2; cmplt_btype=C(i); break;
                case CMPLT_BTYPE_WTOP:  btype=3; cmplt_btype=C(i); break;
                case CMPLT_BTYPE_CALL:  btype=(uint64)-1; cmplt_btype=C(i); break;
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
                default: 
                    return false;
                }
            }
            
            if (cmplt_btype==CMPLT_BTYPE_CALL) {
                if (!IS_O(is13 + 2)) return false;
                if (!IS_BR(is13 + 1)) return false;

                if (IS_BR(is13 + 2)) {
                    return true;
                } else {
                    if (!IS_IMM25(is13 + 2)) return false;
                    if ( IS_NODE_REF(is13 + 2) ) {
                        return true;
                    } else {
                        return true;
                    }
                }
            } else if (cmplt_btype==CMPLT_BTYPE_RET) {
                return true;
            } else if (cmplt_btype==CMPLT_BTYPE_CLOOP 
                    || cmplt_btype==CMPLT_BTYPE_CEXIT
                    || cmplt_btype==CMPLT_BTYPE_CTOP) {
                if ( IS_NODE_REF(is13 + 1) ) {
                    //assert(target != 0);
                    return true;
                } else {
                    return true;
                }
            } else if (okind1==OPND_B_REG) {
                return true;
            } else {
                if (!IS_IMM25(is13 + 1)) return false;
                if ( IS_NODE_REF(is13 + 1) ) {
                    //assert(target!=0);
                    return true;
                } else {
                    return true;
                }
            }
        }
        // fall to assert(0)        
    default:
        IPF_LOG << __FILE__ << ": " << __LINE__ 
                << ": NOT YET IMPLEMENTED VERIFIER FOR INSTRUCTION: " 
                << IrPrinter::toString(inst) << "\n";
        return false;
    }
    
    return false;
}

//----------------------------------------------------------------------------//
bool IpfVerifier::fcmp(string * res, Inst * inst, InstCode icode, OpndVector & opnds, CompVector & cmpls) {
    if (!IS_O(4) || !IS_C(0) || IS_O(5)) return false;
    if (!IS_PR(1) || !IS_PR(2) || !IS_FR(3) || !IS_FR(4)) return false;

    switch (C(0)) {
    case CMPLT_FCMP_FREL_EQ:
    case CMPLT_FCMP_FREL_LT:
    case CMPLT_FCMP_FREL_LE:
    case CMPLT_FCMP_FREL_GT:    
    case CMPLT_FCMP_FREL_GE:
    case CMPLT_FCMP_FREL_UNORD: 
    case CMPLT_FCMP_FREL_NEQ: 
    case CMPLT_FCMP_FREL_NLT:
    case CMPLT_FCMP_FREL_NLE:
    case CMPLT_FCMP_FREL_NGT:
    case CMPLT_FCMP_FREL_NGE:
    case CMPLT_FCMP_FREL_ORD:
        break;
    default:
        res->append("WRONG COMPLETER");
        return false;
    }
    if (IS_C(2)) {
        switch (C(1)) {
        case CMPLT_FCMP_FCTYPE_NONE:
        case CMPLT_FCMP_FCTYPE_UNC:  break;
        default:               
            res->append("WRONG COMPLETER");
            return false;
        }
        switch (C(2)) {
        case CMPLT_SF0:
        case CMPLT_SF1:
        case CMPLT_SF2:
        case CMPLT_SF3: break;
        default:               
            res->append("WRONG COMPLETER");
            return false;
        }
    } else if (IS_C(1)) {
        switch (C(1)) {
        case CMPLT_FCMP_FCTYPE_NONE:
        case CMPLT_FCMP_FCTYPE_UNC:
        case CMPLT_SF0:
        case CMPLT_SF1:
        case CMPLT_SF2:
        case CMPLT_SF3: break;
        default:               
            res->append("WRONG COMPLETER");
            return false;
        }
    }
    return true;
}

//----------------------------------------------------------------------------//
bool IpfVerifier::cmp_cmp4(string * res, Inst * inst, InstCode icode, OpndVector & opnds, CompVector & cmpls) {
    if (!IS_O(4) || !IS_C(0)) return false;
    if (!IS_PR(1) || !IS_PR(2) || !(IS_GR(3) || IS_IMM(3)) || !IS_GR(4)) return false;
    if (IS_GR(3) && IS_GR(4)) {
//        if (GR(3)->getDataKind() != GR(4)->getDataKind()) {
//            res->append(IrPrinter::toString(O(3)) 
//                + " and " 
//                + IrPrinter::toString(O(4))
//                + " have different DataKind: "
//                + getDataKindStr(GR(3)->getDataKind())
//                + ", "
//                + getDataKindStr(GR(4)->getDataKind()));
//            return false;
//        }
    }
    
    int64 p1=V(1), p2=V(2);
    int64 r2=V(3), r3=V(4);
    OpndKind okind3=K(3);
    Completer crel = C(0);
    Completer ctype = ( IS_C(1) ? C(1) : CMPLT_INVALID );
    uint64 opcode=0, ta=0, c=0, x2;
    int64 tmp;

    // parse pseudo-op    
    if ( !IS_C(1) || ctype==CMPLT_CMP_CTYPE_UNC ) {
        if ( IS_IMM8(3) ) {
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
            return false;
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
        return false;
    }
    
    if ( (ctype==CMPLT_CMP_CTYPE_NORMAL || ctype==CMPLT_CMP_CTYPE_UNC 
            || crel==CMPLT_CMP_CREL_NE || crel==CMPLT_CMP_CREL_EQ || cmpls.size()==1) 
            && okind3==OPND_G_REG ) {
        x2 = icode==INST_CMP ? 0 : 1;
        return true;
    } else if ( IS_IMM8(3) ) {
        x2 = icode==INST_CMP ? 2 : 3;
        return true;
    } else {
        if (r2!=0) return false;
        x2 = icode==INST_CMP ? 0 : 1;
        return true;
    }
    return false;
}

//----------------------------------------------------------------------------//
bool IpfVerifier::stx(string * res, Inst * inst, InstCode icode, OpndVector & opnds, CompVector & cmpls) {
    switch (icode) {
    case INST_ST: 
        for (uint64 i=0 ; IS_C(i) ; i++) {
            switch (C(i)) {
            case CMPLT_SZ_1:
            case CMPLT_SZ_2:
            case CMPLT_SZ_4:
            case CMPLT_SZ_8:
            case CMPLT_ST_TYPE_NORMAL:
            case CMPLT_ST_TYPE_REL:   
            case CMPLT_HINT_NONE:
            case CMPLT_HINT_NTA: 
                break;
            default:
                return false;
            }
        }
        if (!IS_C(3) && IS_GR(1) && IS_GR(2) && (!IS_O(3) || IS_IMM9(3))) return true;
        break;
    case INST_ST8_SPILL:
        if (!IS_C(1) 
            && (!IS_C(0) || (IS_C(0) && (C(0)==CMPLT_HINT_NONE || C(0)==CMPLT_HINT_NTA)))
            && IS_GR(1) && IS_GR(2) && (!IS_O(3) || IS_IMM9(3))) return true;
        break;
    case INST_ST16:
        for (uint64 i=0 ; IS_C(i) ; i++) {
            switch (C(i)) {
            case CMPLT_ST_TYPE_NORMAL:
            case CMPLT_ST_TYPE_REL:   
            case CMPLT_HINT_NONE: 
            case CMPLT_HINT_NTA:  
                break;
            default: 
                return false;
            }
        }
        if (!IS_C(2) && IS_GR(1) && IS_GR(2)) return true;
        break;
    default:
        return false;
    }
    return false;
}

//----------------------------------------------------------------------------//
bool IpfVerifier::ldx(string * res, Inst * inst, InstCode icode, OpndVector & opnds, CompVector & cmpls) {
    for (U_32 i=0 ; IS_C(i) ; i++ ) {
        switch (C(i)) {
        case CMPLT_SZ_1:
        case CMPLT_SZ_2:
        case CMPLT_SZ_4:
        case CMPLT_SZ_8:
        case CMPLT_LDTYPE_NORMAL:   
        case CMPLT_LDTYPE_S:
        case CMPLT_LDTYPE_A:
        case CMPLT_LDTYPE_SA:
        case CMPLT_LDTYPE_BIAS:
        case CMPLT_LDTYPE_ACQ:
        case CMPLT_LDTYPE_C_CLR:
        case CMPLT_LDTYPE_C_NC:
        case CMPLT_LDTYPE_C_CLR_ACQ:
        case CMPLT_HINT_NONE:
        case CMPLT_HINT_NT1:
        case CMPLT_HINT_NTA:
            break;
        default:
            return false;
        }
    }
    switch (icode) {
    case INST_LD:
        if (IS_C(2)) return false;
        if (IS_GR(1) && IS_GR(2) && (!IS_O(3) || IS_GR(3) || IS_IMM9(3))) return true;
        break;
    case INST_LD16:
    case INST_LD16_ACQ:
        if (IS_C(1)) return false;
        if (IS_GR(1) && IS_GR(2) && !IS_O(3)) return true;
        break;
    case INST_LD8_FILL:
        if (IS_C(1)) return false;
        if (IS_GR(1) && IS_GR(2) && (!IS_O(3) || IS_GR(3) || IS_IMM9(3))) return true;
        break;
    default:
        break;
    }
    return false;
}

//----------------------------------------------------------------------------//
bool IpfVerifier::ldfx(string * res, Inst * inst, InstCode icode, OpndVector & opnds, CompVector & cmpls) {
    for (U_32 i=0 ; IS_C(i) ; i++ ) {
        switch (C(i)) {
        case CMPLT_FSZ_E:
        case CMPLT_FSZ_S:
        case CMPLT_FSZ_D:
        case CMPLT_LDTYPE_NORMAL:
        case CMPLT_LDTYPE_S:      
        case CMPLT_LDTYPE_A:      
        case CMPLT_LDTYPE_SA:     
        case CMPLT_LDTYPE_C_CLR:  
        case CMPLT_LDTYPE_C_NC:  
        case CMPLT_HINT_NONE:
        case CMPLT_HINT_NT1: 
        case CMPLT_HINT_NTA: 
            break;
        default:
            return false;
        }
    }
    switch (icode) {
    case INST_LDF:
        if (IS_C(3)) return false;
        break;
    case INST_LDF8:
        if (IS_C(2)) return false;
        break;
    case INST_LDF_FILL:
        if (IS_C(1)) return false;
        break;
    default: 
        break;
    }
    if (IS_FR(1) && IS_GR(2) && (!IS_O(3) || IS_GR(3) || IS_IMM9(3))) return true;
    return false;
}

//----------------------------------------------------------------------------//
bool IpfVerifier::mov(string * res, Inst * inst, InstCode icode, OpndVector & opnds, CompVector & cmpls) {
   if (icode!=INST_MOV_RET && IS_O(1) && IS_O(2) && !IS_O(3) 
            && ((IS_AR(1) && IS_GR(2)) || (IS_GR(1) && IS_AR(2)) || (IS_AR(1) && IS_IMM8(2))))
        return true;
    if (icode==INST_MOV && !IS_O(3) && ((IS_GR(1) && IS_BR(2)) || (IS_BR(1) && IS_GR(2))))
        return true;
    if ((icode==INST_MOV || icode==INST_MOV_RET) && IS_O(3) && IS_BR(1) && IS_GR(2) && IS_IMM13(3)) {
        for (U_32 i=0 ; IS_C(i) ; i++){
            switch (C(i)) {
            case CMPLT_IH_NOT_IMP:
            case CMPLT_IH_IMP:
            case CMPLT_WH_DPTK:
            case CMPLT_WH_SPTK:
            case CMPLT_WH_IGNORE:
                break;
            default: 
                return false;
            }
        }
        return true;
    }
    if (icode==INST_MOV && !IS_O(3) && !IS_C(0) && IS_FR(1) && IS_FR(2)) return true;
    if (icode==INST_MOV && !IS_O(3) && !IS_C(0) && IS_GR(1) && IS_GR(2)) return true;
    if (icode==INST_MOV && !IS_O(3) && !IS_C(0) && IS_GR(1) && (IS_IMM22(2) || IS_REF(2))) return true;

    return false;
}

//----------------------------------------------------------------------------//

bool IpfVerifier::verifyNodeEdges(BbNode  * node) {
    string * res = new(mm) string();
    ostringstream oss;

    EdgeVector& outEdges = node->getOutEdges();

    if(outEdges.size() == 0 && cfg.getExitNode() != node) {
        oss << endl << "        does not have out edges";
    }
    
    for(uint16 i=0; i<outEdges.size(); i++) {
        switch(outEdges[i]->getEdgeKind()) {
            case EDGE_BRANCH    : edgeBranch(node, outEdges[i], oss); break;
            case EDGE_THROUGH   : edgeThrough(node, outEdges[i], oss); break;
            case EDGE_DISPATCH  : break;
            case EDGE_EXCEPTION : break;
            case EDGE_INVALID   : edgeInvalid(node, oss); break;
        }
    }
    
    res->append(oss.str());
    if(!res->empty()) {
        cerr << "VERIFY ERROR: " << *methodString << " " << *res << endl;
        return false;
    }
    return true;
}

//----------------------------------------------------------------------------//

void IpfVerifier::edgeBranch(Node* node, Edge* edge, ostream& os) {

    if(node->getNodeKind() != NODE_BB) {
        os << endl << "        is not BB, but contains EDGE_BRANCH";
        return;
    }
    
    InstVector& insts = ((BbNode*) node)->getInsts();
    if(insts.size() == 0) {
        os << endl << "        does not have insts, but contains EDGE_BRANCH";
        return;
    }
    
    Inst* branchInst = insts.back();
    if(Encoder::isBranchInst(branchInst) == false) {
        os << endl << "        contains EDGE_BRANCH, but last inst is not \"br\"";
        return;
    }
    
    Opnd* targetOpnd = branchInst->getOpnd(1);
    if(targetOpnd->getDataKind() != DATA_NODE_REF) return; // it must be switch
    
    NodeRef* targetNodeRef = (NodeRef*) targetOpnd;
    Node* targetNode=targetNodeRef->getNode();
    Node* edgeTarget= edge->getTarget();
    if (targetNode!=edgeTarget) {
        os << "node" << node->getId() << " branch edge target and \"br\" instruction target are different";
        return;
    }
}
    
//----------------------------------------------------------------------------//

void IpfVerifier::edgeThrough(Node* node, Edge* edge, ostream& os) {
    
    if(node->getNodeKind() != NODE_BB) {
        os << endl << "        is not BB, but contains EDGE_THROUGH";
        return;
    }

    Node* edgeTarget = edge->getTarget();
    Node* layoutSucc = ((BbNode*) node)->getLayoutSucc();
    if(edgeTarget != layoutSucc) {

        // check if last inst is br.ret
        Inst* lastInst = NULL;
        InstVector & insts = ((BbNode*) node)->getInsts();
        if (insts.size()>0) lastInst = insts.back();

        if(lastInst!=NULL && lastInst->getComps().size()>0 && lastInst->getComp(0)==CMPLT_BTYPE_RET)
            return;

        os << endl 
            << "        through edge target (node" 
            << (edgeTarget ? edgeTarget->getId() : (uint64)-1)
            << ") and layout successor (node" 
            << (layoutSucc ? layoutSucc->getId() : (uint64)-1)
            << ") are different";
        return;
    }
}

//----------------------------------------------------------------------------//

void IpfVerifier::edgeInvalid(Node* node, ostream& os) {

    os << endl << "        contains EDGE_INVALID";
}

//----------------------------------------------------------------------------//

char * IpfVerifier::getDataKindStr(DataKind datakind) {
    switch ( datakind ) {
    case DATA_BASE: return "DATA_BASE";
    case DATA_MPTR: return "DATA_MPTR";
    case DATA_I8:   return "DATA_I8";
    case DATA_U8:   return "DATA_U8";
    case DATA_I16:  return "DATA_I16";
    case DATA_U16:  return "DATA_U16";
    case DATA_I32:  return "DATA_I32";
    case DATA_U32:  return "DATA_U32";
    case DATA_I64:  return "DATA_I64";
    case DATA_U64:  return "DATA_U64";
    case DATA_S:    return "DATA_S";
    case DATA_D:    return "DATA_D";
    case DATA_F:    return "DATA_F";
    case DATA_P:    return "DATA_P";
    default: return "DATA_XXX";
    }
    return "DATA_XXX";
}

} // IPF
} // Jitrino
