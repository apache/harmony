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
 * @author Nikolay A. Sidelnikov
 */

#include "Ia32IRManager.h"
#include "Ia32Printer.h"
#include "Interval.h"

namespace Jitrino
{
namespace Ia32{

#define FPU_FLAG_C0 0x100
#define FPU_FLAG_C2 0x400
#define FPU_FLAG_C3 0x4000
#define FPU_FLAGS   (FPU_FLAG_C0 | FPU_FLAG_C2 |FPU_FLAG_C3)
#define ROUND_FLAG 0xC00

//========================================================================================
//========================================================================================
/**
    I586InstsExpansion translates SSE2 instructions and newer to the corresponding SSE and x87 instructions 
    and SETcc and CMOVcc instructions to branches
*/
class I586InstsExpansion : public SessionAction {

    void runImpl();


    void lowerToX87();
    void lowerToSSE();

    U_32 getNeedInfo()const{ return 0; }
    U_32 getSideEffects()const{ return hasSideEffects ? SideEffect_InvalidatesLivenessInfo: 0; }
    bool isIRDumpEnabled(){ return hasSideEffects;}

    bool hasSideEffects;
public:
    I586InstsExpansion() : hasSideEffects(false){}

};


static ActionFactory<I586InstsExpansion> _i586("i586");




//NOTE: today the following methods contains info only for mnemonics used in CG. 
//TODO: update them to have all mnemonics from Architecture Manual or add this info to Encoder
static bool isSSE2OrNewer(Mnemonic mn) {
    switch (mn) {
        case Mnemonic_MOVAPD:
            return true;
        default:
            return false;
    }
}

//return true if instruction is SSE or newer
static bool isSSEOrNewer(Mnemonic mn) {
    switch (mn) {
        case Mnemonic_ADDSS:
        case Mnemonic_ADDSD:
        case Mnemonic_SUBSS:
        case Mnemonic_SUBSD:
        case Mnemonic_MULSS:
        case Mnemonic_MULSD:
        case Mnemonic_DIVSS:
        case Mnemonic_DIVSD:
        case Mnemonic_XORPD:
        case Mnemonic_XORPS:
        case Mnemonic_PXOR:
        case Mnemonic_MOVQ:
        case Mnemonic_MOVD:
        case Mnemonic_MOVSD:
        case Mnemonic_MOVSS:
        case Mnemonic_UCOMISD:
        case Mnemonic_UCOMISS:
        case Mnemonic_CVTTSS2SI:
        case Mnemonic_CVTTSD2SI:
        case Mnemonic_CVTSI2SS:
        case Mnemonic_CVTSI2SD:
            return true;
        default:
            return isSSE2OrNewer(mn);
    }
}

static void mapToX87(Mnemonic mn, Mnemonic &fMnem1, Mnemonic &fMnem2) {
    switch (mn) {
        case Mnemonic_ADDSS:
        case Mnemonic_ADDSD:
            fMnem1 = Mnemonic_FADDP;
            fMnem2 = Mnemonic_FADD;
            return;
        case Mnemonic_SUBSS:
        case Mnemonic_SUBSD:
            fMnem1 = Mnemonic_FSUBP;
            fMnem2 = Mnemonic_FSUB;
            return;
        case Mnemonic_MULSS:
        case Mnemonic_MULSD:
            fMnem1 = Mnemonic_FMULP;
            fMnem2 = Mnemonic_FMUL;
            return;
        case Mnemonic_DIVSS:
        case Mnemonic_DIVSD:
            fMnem1 = Mnemonic_FDIVP;
            fMnem2 = Mnemonic_FDIV;
            return;
        default:
            return;
    }
}

enum FPUMode {
    FPUMode_X87     = 1,
    FPUMode_SSE     = 2,
    FPUMode_SSE2    = 3
};

void I586InstsExpansion::runImpl() {
    FPUMode mode = CPUID::isSSE2Supported() ? FPUMode_SSE2 : FPUMode_SSE;

    const char* modeStr = getArg("mode");
    if (modeStr!=NULL && !strcmp(modeStr, "sse")) {
        mode = FPUMode_SSE;
    } else if (modeStr!=NULL && !strcmp(modeStr, "x87")) {
        mode = FPUMode_X87;
    }

    if (Log::isEnabled()) {
        Log::out()<<"has sse2:" << CPUID::isSSE2Supported() << " mode:"<<(int)mode<<std::endl; 
    }

    switch(mode) {
        case FPUMode_X87:
            lowerToX87();
            return;
        case FPUMode_SSE:
            lowerToSSE();
            return;
        case FPUMode_SSE2:
            //do nothing;
            break;
        default: assert(0);
    }
}

void I586InstsExpansion::lowerToSSE() {
    const Nodes& nodes = irManager->getFlowGraph()->getNodes();
    for (Nodes::const_iterator cit = nodes.begin(); cit != nodes.end(); ++cit) {
        Node* node = *cit;
        if (!node->isBlockNode()) {
            continue;
        }
        for(Inst * inst = (Inst *)node->getFirstInst(); inst != NULL; ) {
            Mnemonic mn = inst->getMnemonic();
            if (!isSSE2OrNewer(mn)) {
                continue;
            }
            //Mode is not implemented for a SSE2 and newer systems.
            //Must never hit for SSE-only hardware.
            assert(0); 

        }
    }
}


void I586InstsExpansion::lowerToX87() {
    //check if to use FPU mode. Otherwise SSE1 insts will be allowed
    irManager->updateLivenessInfo();
    hasSideEffects = true;

    //create stack memory operands for storing values of XMM registers
    StlMap<unsigned, Opnd *> xmmMemOp32Map(irManager->getMemoryManager());
    StlMap<unsigned, Opnd *> xmmMemOp64Map(irManager->getMemoryManager());
    for (unsigned i = 0; i< 16; i++) {
        xmmMemOp32Map[i] = NULL;
        xmmMemOp64Map[i] = NULL;
    }
    StlMap<unsigned, Opnd *> * xmmMemOpsPtr = &xmmMemOp64Map;

    //create FP registers operands with corresponding size
    Opnd * fp0Op32 = irManager->newOpnd(irManager->getTypeManager().getSingleType(), RegName_FP0S);
    fp0Op32->assignRegName(RegName_FP0S);
    Opnd * fp0Op64 = irManager->newOpnd(irManager->getTypeManager().getDoubleType(), RegName_FP0D);
    fp0Op64->assignRegName(RegName_FP0D);
    Opnd * fp1Op32 = irManager->newOpnd(irManager->getTypeManager().getSingleType(), RegName_FP1S);
    fp1Op32->assignRegName(RegName_FP1S);
    Opnd * fp1Op64 = irManager->newOpnd(irManager->getTypeManager().getDoubleType(), RegName_FP1D);
    fp1Op64->assignRegName(RegName_FP1D);
    
    const Nodes& nodes = irManager->getFlowGraph()->getNodes();
    for (Nodes::const_iterator cit = nodes.begin(); cit != nodes.end(); ++cit) {
        Node* node = *cit;
        if (!node->isBlockNode()) {
            continue;
        }
        for(Inst * inst = (Inst *)node->getFirstInst(); inst != NULL; ) {
            Inst * tmpInst = inst->getNextInst();
            Mnemonic mn = inst->getMnemonic();
            
            Mnemonic fMnem1 = Mnemonic_NULL, fMnem2 = Mnemonic_NULL;
            if (!isSSEOrNewer(mn)) {
                //check all other instruction for XMM registers operands
                Inst::Opnds xmms(inst, Inst::OpndRole_Explicit|Inst::OpndRole_UseDef);
                for (Inst::Opnds::iterator it = xmms.begin(); it != xmms.end(); it = xmms.next(it)) {
                    Opnd * op = inst->getOpnd(it);
                    if (op->isPlacedIn(OpndKind_XMMReg)) {
                        std::string str = std::string("SSE instruction found: ") + Encoder::getMnemonicString(mn);
                        Jitrino::crash(str.c_str());
                    }
                }
                inst = tmpInst;
                continue;
            }

            mapToX87(mn, fMnem1, fMnem2);


            Opnd * fp0;
            Opnd * fp1;

            //number of registers in instruction
            unsigned regNum1 = 0;

            //XMM-suspected operands
            Inst::Opnds xmms(inst, Inst::OpndRole_Explicit|Inst::OpndRole_UseDef);
            Opnd * op1 = inst->getOpnd(xmms.begin());
            Opnd * op2 = inst->getOpnd(xmms.begin()+1);

            //choose operands size
            if (op2->getSize() == OpndSize_64) {
                fp0 = fp0Op64;
                fp1 = fp1Op64;
                xmmMemOpsPtr = &xmmMemOp64Map;
            } else {
                fp0 = fp0Op32;
                fp1 = fp1Op32;
                xmmMemOpsPtr = &xmmMemOp32Map;
            }

            switch (mn) {
                case Mnemonic_XORPD:
                case Mnemonic_XORPS:
                case Mnemonic_PXOR:
                {
                    //Packed XOR is used only for loading of zero into a XMM register
                    //replace: XORPD xmm2, xmm2     ->      FLDZ fp0
                    //                                      FSTP [xmm2_Mem] 
                    if (op1 == op2) {
                        RegName reg = op1->getRegName();
                        unsigned regNum = (unsigned)reg & 0xF;
                        
                        //create memory operand for the XMM register if it doesn't exist
                        if (!(*xmmMemOpsPtr)[regNum]  || (*xmmMemOpsPtr)[regNum]->getSize() != op1->getSize()) {
                            Opnd* opnd = irManager->newMemOpnd(fp0->getType(), MemOpndKind_StackAutoLayout, irManager->getRegOpnd(STACK_REG), 0);
                            opnd->setMemOpndAlignment(Opnd::MemOpndAlignment_16);
                            (*xmmMemOpsPtr)[regNum] = opnd; 
                        }
                        
                        //load zero
                        node->prependInst(irManager->newInst(Mnemonic_FLDZ, fp0),inst);
                        
                        //store zero and pop FPU stack
                        node->prependInst(irManager->newInst(Mnemonic_FSTP, (*xmmMemOpsPtr)[regNum], fp0), inst);
                    } else {
                        assert(0);
                    }
                    break;
                }
                case Mnemonic_ADDSS:
                case Mnemonic_ADDSD:
                case Mnemonic_SUBSS:
                case Mnemonic_SUBSD:
                case Mnemonic_MULSS:
                case Mnemonic_MULSD:
                case Mnemonic_DIVSS:
                case Mnemonic_DIVSD:
                {
                    //replace: ADDSS xmm1, xmm2 (op2) ->    FLD     fp0, [xmm1_Mem]
                    //                                      FLD     fp0, [xmm2_Mem]     ;also move previous fp0 value to fp1)
                    //                                      FADDP   fp0, fp1            (FADD   fp0, op2)
                    //                                      FSTP    [xmm1_Mem] 
                    
                    //push first operand on stack
                    if (op1->isPlacedIn(OpndKind_XMMReg)) {
                        RegName reg1 = op1->getRegName();
                        regNum1 = (unsigned)reg1 & 0xF;
                        assert((*xmmMemOpsPtr)[regNum1]);
                        node->prependInst(irManager->newInst(Mnemonic_FLD, fp0, (*xmmMemOpsPtr)[regNum1]),inst);
                    } else {
                        assert(0);
                    }

                    //push second operand on FPU stack if it is an XMM register
                    if (op2->isPlacedIn(OpndKind_XMMReg)) {
                        RegName reg2 = op2->getRegName();
                        unsigned regNum2 = (unsigned)reg2 & 0xF;
                        assert((*xmmMemOpsPtr)[regNum2]);
                        node->prependInst(irManager->newInst(Mnemonic_FLD, fp0, (*xmmMemOpsPtr)[regNum2]),inst);
                      
                        //do the operation and pop FPU stack  
                        node->prependInst(irManager->newInst(fMnem1, fp0, fp1),inst);
                    } else {
                        //do the operation
                        node->prependInst(irManager->newInst(fMnem2, fp0, op2),inst);
                    }
                    
                    //store result and pop FPU stack
                    node->prependInst(irManager->newInst(Mnemonic_FSTP, (*xmmMemOpsPtr)[regNum1], fp0),inst);
                    break;
                }
                case Mnemonic_MOVQ:
                case Mnemonic_MOVD:
                case Mnemonic_MOVSD:
                case Mnemonic_MOVSS:
                {
                    if (op1->isPlacedIn(OpndKind_XMMReg)) {
                        RegName reg1 = op1->getRegName();
                        regNum1 = (unsigned)reg1 & 0xF;
                        
                        //create memory operand for the destination XMM register if it doesn't exist
                        if (!(*xmmMemOpsPtr)[regNum1] || (*xmmMemOpsPtr)[regNum1]->getSize() != op1->getSize()) {
                            (*xmmMemOpsPtr)[regNum1] = irManager->newMemOpnd(fp0->getType(), MemOpndKind_StackAutoLayout, irManager->getRegOpnd(STACK_REG), 0);
                        }

                        //store value to XMM-memory slot and pop FPU stack
                        node->appendInst(irManager->newInst(Mnemonic_FSTP, (*xmmMemOpsPtr)[regNum1], fp0),inst);
                    } else {
                        //store value to memory and pop FPU stack
                        node->appendInst(irManager->newInst(Mnemonic_FSTP, op1, fp0),inst);
                    }
                    if (op2->isPlacedIn(OpndKind_XMMReg)) {
                        RegName reg2 = op2->getRegName();
                        unsigned regNum2 = (unsigned)reg2 & 0xF;
                        assert((*xmmMemOpsPtr)[regNum2]);
                        
                        //load value from XMM-memory slot onto FPU stack
                        node->prependInst(irManager->newInst(Mnemonic_FLD, fp0, (*xmmMemOpsPtr)[regNum2]),inst);
                    } else {
                        //load value from memory onto FPU stack
                        node->prependInst(irManager->newInst(Mnemonic_FLD, fp0, op2),inst);
                    }
                    break;
                }
                case Mnemonic_CVTTSS2SI:
                case Mnemonic_CVTTSD2SI:
                {

                    //replace: CVTTSS2SI ebx, xmm2  ->  FNSTCW  [fpucw]
                    //                                  FNSTCW  [tmpcw]
                    //                                  OR      [fpucw], ROUND_FLAG
                    //                                  FLDCW   [fpucw]
                    //                                  FLD     fp0, [xmm2_Mem]
                    //                                  FISTP   [cMemOp], fp0
                    //                                  MOV     ebx, [cMemOp]
                    //                                  FLDCW   [tmpcw]
                    
                    
                    //set rounding mode to "round to zero" before operation and and restore old value after operation
                    Opnd * fpucw = irManager->newMemOpnd(irManager->getTypeManager().getInt16Type(), 
                                                            MemOpndKind_StackAutoLayout,  irManager->getRegOpnd(STACK_REG), 0);
                    Opnd * tmpcw = irManager->newMemOpnd(irManager->getTypeManager().getInt16Type(), 
                                                            MemOpndKind_StackAutoLayout,  irManager->getRegOpnd(STACK_REG), 0);
                    node->prependInst(irManager->newInst(Mnemonic_FNSTCW, fpucw),inst);
                    node->prependInst(irManager->newInst(Mnemonic_FNSTCW, tmpcw),inst);
                    node->prependInst(irManager->newInst(Mnemonic_OR, fpucw, irManager->newImmOpnd(fpucw->getType(), ROUND_FLAG)),inst);
                    node->prependInst(irManager->newInst(Mnemonic_FLDCW, fpucw),inst);
                    node->appendInst(irManager->newInst(Mnemonic_FLDCW, tmpcw),inst);

                    //create the memory slot
                    Opnd * cMemOp = irManager->newMemOpnd(op1->getType(), MemOpndKind_StackAutoLayout, irManager->getRegOpnd(STACK_REG), 0);

                    //move result from the memory slot to destination
                    node->appendInst(irManager->newInst(Mnemonic_MOV, op1, cMemOp),inst);
                    
                    //store the value with truncation to the memory slot and pop FPU stack
                    node->appendInst(irManager->newInst(Mnemonic_FISTP, cMemOp, fp0),inst);
                    
                    if (op2->isPlacedIn(OpndKind_XMMReg)) {
                        RegName reg2 = op2->getRegName();
                        unsigned regNum2 = (unsigned)reg2 & 0xF;
                        assert((*xmmMemOpsPtr)[regNum2]);

                        //load value from XMM-memory slot onto FPU stack
                        node->prependInst(irManager->newInst(Mnemonic_FLD, fp0, (*xmmMemOpsPtr)[regNum2]),inst);
                    } else {
                        //load value from memory onto FPU stack
                        node->prependInst(irManager->newInst(Mnemonic_FLD, fp0, op2),inst);
                    }
                    break;
                }
                case Mnemonic_UCOMISD:
                case Mnemonic_UCOMISS:
                {
                    //replace:  UCOMISD xmm0, xmm2  ->  PUSH    eax
                    //          JP      L1              FLD     fp0, [xmm2_Mem]
                    //          JNA     L2              FLD     fp0, [xmm0_Mem]
                    //                                  FUCOMPP fp0, fp1
                    //                                  FNSTSW  eax
                    //                                  MOV     [cMemOp], eax
                    //                                  POP     eax
                    //                                  AND     [cMemOp], FPU_FLAGS
                    //                                  CMP     [cMemOp], FPU_FLAGS
                    //                                  JZ      L1
                    //                                  AND     [cMemOp], FPU_FLAGS
                    //                                  CMP     [cMemOp], 0
                    //                                  JNZ      L2
                    Inst * nextInst = inst->getNextInst();
                    //find conditional instruction
                    for (; nextInst != NULL ; nextInst = nextInst->getNextInst()) {
                        Mnemonic bm = getBaseConditionMnemonic(nextInst->getMnemonic());
                        if (bm != Mnemonic_Null)
                            break;
                    }
                    assert(nextInst);

                    //save and restore EAX register
                    Opnd * eax = irManager->getRegOpnd(RegName_EAX);
                    node->prependInst(irManager->newInst(Mnemonic_PUSH, eax),inst);
                    node->appendInst(irManager->newInst(Mnemonic_POP, eax),inst);
                    
                    //generate x87 comparison
                    if (op2->isPlacedIn(OpndKind_XMMReg)) {
                        RegName reg2 = op2->getRegName();
                        unsigned regNum2 = (unsigned)reg2 & 0xF;
                        assert((*xmmMemOpsPtr)[regNum2]);
                        node->prependInst(irManager->newInst(Mnemonic_FLD, fp0, (*xmmMemOpsPtr)[regNum2]),inst);
                    } else {
                        node->prependInst(irManager->newInst(Mnemonic_FLD, fp0, op2),inst);
                    }
                    if (op1->isPlacedIn(OpndKind_XMMReg)) {
                        RegName reg1 = op1->getRegName();
                        regNum1 = (unsigned)reg1 & 0xF;
                        assert((*xmmMemOpsPtr)[regNum1]);
                        node->prependInst(irManager->newInst(Mnemonic_FLD, fp0, (*xmmMemOpsPtr)[regNum1]),inst);
                    } else {
                        assert(0);
                    }
                    node->prependInst(irManager->newInst(Mnemonic_FUCOMPP, fp0, fp1),inst);
                    
                    node->prependInst(irManager->newInst(Mnemonic_FNSTSW, eax),inst);
                    
                    //save result from eax to memory
                    Opnd * cMemOp = irManager->newMemOpnd(eax->getType(), MemOpndKind_StackAutoLayout, irManager->getRegOpnd(STACK_REG), 0);
                    node->appendInst(irManager->newInst(Mnemonic_MOV, cMemOp, eax),inst);

                    Inst * parityInst = NULL;
                    Node * trueTarget, * falseTarget;

                    if (nextInst->getKind() == Inst::Kind_BranchInst) {
                        Mnemonic mn = nextInst->getMnemonic();
                        //if the instruction is a check for NaN
                        if (mn == Mnemonic_JP || mn == Mnemonic_JNP) {
                            parityInst = nextInst;
                            trueTarget = ((BranchInst *)parityInst)->getTrueTarget();
                            falseTarget = ((BranchInst *)parityInst)->getFalseTarget();

                            //if NaN then all FPU flags are set
                            node->prependInst(irManager->newInst(Mnemonic_AND, cMemOp, irManager->newImmOpnd(cMemOp->getType(),FPU_FLAGS)),nextInst);
                            node->prependInst(irManager->newInst(Mnemonic_CMP, cMemOp, irManager->newImmOpnd(cMemOp->getType(),FPU_FLAGS)),nextInst);

                            if (mn == Mnemonic_JP) {
                                node->prependInst(irManager->newBranchInst(Mnemonic_JZ, trueTarget, falseTarget),nextInst);
                                nextInst = (Inst *)falseTarget->getLastInst();
                            } else {
                                node->prependInst(irManager->newBranchInst(Mnemonic_JNZ, trueTarget, falseTarget),nextInst);
                                nextInst = (Inst *)trueTarget->getLastInst();
                            }
                            parityInst->unlink();
                            mn = nextInst->getMnemonic();
                        } 
                        trueTarget = ((BranchInst *)nextInst)->getTrueTarget();
                        falseTarget = ((BranchInst *)nextInst)->getFalseTarget();
                        Node * nextNode = nextInst->getNode();

                        //check particular flags and do the corresponding conditional jump
                        if (mn == Mnemonic_JA || mn == Mnemonic_JNA) {
                            nextNode->prependInst(irManager->newInst(Mnemonic_AND, cMemOp, irManager->newImmOpnd(cMemOp->getType(),FPU_FLAGS)),nextInst);
                            nextNode->prependInst(irManager->newInst(Mnemonic_CMP, cMemOp, irManager->newImmOpnd(cMemOp->getType(),0)),nextInst);
                            nextNode->prependInst(irManager->newBranchInst(mn==Mnemonic_JA ? Mnemonic_JZ : Mnemonic_JNZ, trueTarget, falseTarget),nextInst);
                        } else if (mn == Mnemonic_JAE || mn == Mnemonic_JNAE) {
                            nextNode->prependInst(irManager->newInst(Mnemonic_AND, cMemOp, irManager->newImmOpnd(cMemOp->getType(),FPU_FLAG_C0)),nextInst);
                            nextNode->prependInst(irManager->newBranchInst(mn==Mnemonic_JAE? Mnemonic_JZ : Mnemonic_JNZ, trueTarget, falseTarget),nextInst);
                        } else if (mn == Mnemonic_JE || mn == Mnemonic_JNE) {
                            nextNode->prependInst(irManager->newInst(Mnemonic_AND, cMemOp, irManager->newImmOpnd(cMemOp->getType(),FPU_FLAG_C3)),nextInst);
                            nextNode->prependInst(irManager->newBranchInst(mn==Mnemonic_JE? Mnemonic_JNZ : Mnemonic_JZ, trueTarget, falseTarget),nextInst);
                        }
                        nextInst->unlink();
                    } else {
                        nextInst = nextInst->getNextInst();
                        Mnemonic mn = nextInst->getMnemonic();
                        Inst::Opnds sopnds(nextInst, Inst::OpndRole_Explicit|Inst::OpndRole_UseDef);
                        Inst::Opnds::iterator oit = sopnds.begin();
                        Opnd * sop = inst->getOpnd(oit);
                        
                        //check particular flags and do the corresponding conditional set
                        if (mn == Mnemonic_SETA || mn == Mnemonic_SETNA) {
                            node->prependInst(irManager->newInst(Mnemonic_CMP, cMemOp, irManager->newImmOpnd(cMemOp->getType(),0)),nextInst);
                            node->prependInst(irManager->newInst(mn==Mnemonic_SETA ? Mnemonic_SETZ : Mnemonic_SETNZ, sop),nextInst);
                        } else if (mn == Mnemonic_SETAE || mn == Mnemonic_SETNAE) {
                            node->prependInst(irManager->newInst(Mnemonic_AND, cMemOp, irManager->newImmOpnd(cMemOp->getType(),FPU_FLAGS)),nextInst);
                            node->prependInst(irManager->newInst(Mnemonic_CMP, cMemOp, irManager->newImmOpnd(cMemOp->getType(),FPU_FLAG_C0)),nextInst);
                            node->prependInst(irManager->newInst(mn==Mnemonic_SETAE? Mnemonic_SETNZ : Mnemonic_SETZ, sop),nextInst);
                        } else if (mn == Mnemonic_SETE || mn == Mnemonic_SETNE) {
                            node->prependInst(irManager->newInst(Mnemonic_AND, cMemOp, irManager->newImmOpnd(cMemOp->getType(),FPU_FLAGS)),nextInst);
                            node->prependInst(irManager->newInst(Mnemonic_CMP, cMemOp, irManager->newImmOpnd(cMemOp->getType(),FPU_FLAGS)),nextInst);
                            node->prependInst(irManager->newInst(mn==Mnemonic_SETE? Mnemonic_SETZ : Mnemonic_SETNZ, sop),nextInst);
                        }
                        parityInst = nextInst->getNextInst();
                        nextInst->unlink();
                        if (parityInst) {
                            nextInst = parityInst;
                            mn = parityInst->getMnemonic();
                            
                            //if the instruction is a check for NaN
                            if (mn == Mnemonic_CMOVP || mn == Mnemonic_CMOVNP) {
                                node->prependInst(irManager->newInst(Mnemonic_AND, cMemOp, irManager->newImmOpnd(cMemOp->getType(),FPU_FLAGS)),nextInst);
                                node->prependInst(irManager->newInst(Mnemonic_CMP, cMemOp, irManager->newImmOpnd(cMemOp->getType(),FPU_FLAGS)),nextInst);
                                Opnd * cop0 = NULL;
                                Opnd * cop1 = NULL;
                                Inst::Opnds copnds(nextInst, Inst::OpndRole_Explicit|Inst::OpndRole_UseDef);
                                Inst::Opnds::iterator oit = copnds.begin();
                                cop0 = inst->getOpnd(oit);
                                cop1 = inst->getOpnd(copnds.next(oit));
                                if (mn ==Mnemonic_CMOVP) {
                                    node->prependInst(irManager->newInst((Mnemonic_CMOVNZ), cop0, cop1),nextInst);
                                    nextInst = nextInst->getNextInst();
                                } else {
                                    node->prependInst(irManager->newInst((Mnemonic_CMOVZ), cop0, cop1),nextInst);
                                }
                                nextInst->unlink();
                            }
                        }
                    }
                    break;
                }
                default:
                {
                    assert(0);
                    break;
                }
            }
            
            inst->unlink();
            inst = (Inst * )node->getFirstInst();
        }
    }
    
    StlVector<Inst *> cmovs(irManager->getMemoryManager());
    StlVector<Inst *> sets(irManager->getMemoryManager());

    for (Nodes::const_iterator cit = nodes.begin(); cit != nodes.end(); ++cit) {
        Node* node = *cit;
        if (!node->isBlockNode()) {
            continue;
        }
        for(Inst * inst = (Inst *)node->getFirstInst(); inst != NULL; inst = inst->getNextInst()) {
            Mnemonic mn = inst->getMnemonic();
            Mnemonic bm = getBaseConditionMnemonic(mn);
            if(bm == Mnemonic_CMOVcc) {
                cmovs.push_back(inst);
            } else if (bm == Mnemonic_SETcc) {
                sets.push_back(inst);
            }
        }
    }
    
    for(unsigned i = 0 ; i<cmovs.size(); i++) {
        //replace: CMOVcc op1, op2  ->      Jcc L1
        //                                  JMP L2
        //                              L1: MOV op1, op2
        //                              L2: ...
        Inst * inst = cmovs[i];
        Mnemonic mn = inst->getMnemonic();
        Mnemonic bm = getBaseConditionMnemonic(mn);
        Inst::Opnds opnds(inst, Inst::OpndRole_Explicit|Inst::OpndRole_UseDef);
        Opnd * op1 = inst->getOpnd(opnds.begin());
        Opnd * op2 = inst->getOpnd(opnds.begin()+1);
        ControlFlowGraph* subCFG = irManager->createSubCFG(true, false);
        Node* bbJmp = subCFG->getEntryNode();
        Node* bbTrue  = subCFG->createBlockNode();
        Node* bbRet = subCFG->getReturnNode();

        Inst * jmp = irManager->newBranchInst((Mnemonic)(Mnemonic_Jcc+(mn-bm)),bbTrue, bbRet);
        bbJmp->appendInst(jmp);

        Inst * trueMov = irManager->newInst(Mnemonic_MOV, op1, op2);
        bbTrue->appendInst(trueMov);

        subCFG->addEdge(bbJmp, bbTrue, 0.5);
        subCFG->addEdge(bbJmp, bbRet, 0.5);
        subCFG->addEdge(bbTrue, bbRet, 1);
        irManager->getFlowGraph()->spliceFlowGraphInline(inst, *subCFG);
        inst->unlink();
    }
    
    for(unsigned i = 0 ; i<sets.size(); i++) {
        //replace: SETcc op1  ->     Jcc L1
        //                           MOV op1, 0
        //                           JMP L2
        //                       L1: MOV op1, 1
        //                       L2: ...
        Inst * inst = sets[i];
        Mnemonic mn = inst->getMnemonic();
        Mnemonic bm = getBaseConditionMnemonic(mn);
        Inst::Opnds opnds(inst, Inst::OpndRole_Explicit|Inst::OpndRole_UseDef);
        Opnd * op1 = inst->getOpnd(opnds.begin());
        ControlFlowGraph* subCFG = irManager->createSubCFG(true, false);
        Node* bbJmp = subCFG->getEntryNode();
        Node* bbTrue  = subCFG->createBlockNode();
        Node* bbFalse = subCFG->createBlockNode();
        Node* bbRet = subCFG->getReturnNode();

        Inst * jmp = irManager->newBranchInst((Mnemonic)(Mnemonic_Jcc+(mn-bm)),bbTrue, bbFalse);
        bbJmp->appendInst(jmp);

        Inst * trueMov = irManager->newInst(Mnemonic_MOV, op1, irManager->newImmOpnd(op1->getType(), 1));
        bbTrue->appendInst(trueMov);

        Inst * falseMov = irManager->newInst(Mnemonic_MOV, op1, irManager->newImmOpnd(op1->getType(), 0));
        bbFalse->appendInst(falseMov);

        subCFG->addEdge(bbJmp, bbTrue, 0.5);
        subCFG->addEdge(bbJmp, bbFalse, 0.5);
        subCFG->addEdge(bbTrue, bbRet, 1);
        subCFG->addEdge(bbFalse, bbRet, 1);
        irManager->getFlowGraph()->spliceFlowGraphInline(inst, *subCFG);
        inst->unlink();
    }
    
    irManager->getFlowGraph()->purgeEmptyNodes();
    irManager->getFlowGraph()->purgeUnreachableNodes();
    irManager->fixEdgeProfile();
    
}

}}; //namespace Ia32

