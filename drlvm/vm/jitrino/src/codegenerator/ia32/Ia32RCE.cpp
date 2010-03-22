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

namespace Jitrino
{
namespace Ia32 {

//========================================================================================
// class RCE
//========================================================================================
/**
 *  class RCE performs removing comparisons following instructions which
 *  affected flags in the same way as CMP. In some cases instructions can be
 *  reordered for resolving comparison as available for removing
 *
 *  The algorithm takes one-pass over CFG.
 *
 *  This transformer ensures that
 *
 *  1)  All conditional instructions get the same EFLAGS value as before 
 *      transformation
 *
 *  2)  All reordered instructions do the same effects as before transformation
 *
 *  For example:
 *  
 *  Original code piece:
 *      I29: t50.:I_32 (ID:v15(EFLGS):U_32) =AND .t28:I_32,t49(1):I_32 
 *      I30: (AD:v1:I_32) =CopyPseudoInst (AU:t48:I_32) 
 *      I31: (AD:v2:I_32) =CopyPseudoInst (AU:t25:I_32) 
 *      I32: (AD:v3:I_8[]) =CopyPseudoInst (AU:t38:I_8[]) 
 *      I33: (ID:v15(EFLGS):U_32) =CMP .t50:I_32,t51(0):I_32 
 *      I34: JNZ BB_12 t52(0):intptr (IU:v15(EFLGS):U_32) 
 *
 *  After optimization:
 *      I29: t50:I_32 (ID:v15(EFLGS):U_32) =AND .t28:I_32,t49(1):I_32 
 *      I30: (AD:v1:I_32) =CopyPseudoInst (AU:t48:I_32) 
 *      I31: (AD:v2:I_32) =CopyPseudoInst (AU:t25:I_32) 
 *      I32: (AD:v3:I_8[]) =CopyPseudoInst (AU:t38:I_8[]) 
 *      I34: JNZ BB_12 t52(0):intptr (IU:v15(EFLGS):U_32) 
 *
 *  The implementation of this transformer is located in Ia32RCE.cpp
 *
 */
    
class RCE : public SessionAction {
    void runImpl();
protected:

    // check if flags used by the conditional instruction (=condInst)
    // are affected by the instruction (=inst) in the same way as
    // a hypothetical CMP follower (with appropriate arguments) does
    bool instAffectsFlagsAsCmpInst(Inst * inst, Inst * condInst);

    //  check instruction inst for possibility of removing
    bool isSuitableToRemove(Inst * inst, Inst * condInst, Inst * cmpInst, Opnd * cmpOp);

};

static ActionFactory<RCE> _rce("rce");

/**
 *  The algorithm finds conditional instruction (=condInst) first, then
 *  corresponding CMP instruction (=cmpInst) and arithmetic instruction (=inst)
 *  which affects flags in the same way as CMP. Combination is considered as
 *  available to be reduced if there are no instructions between CMP and
 *  arithmetic instruction which influence to flags or CMP operands.
 *
 *  Also it transforms some conditional instruction to make them more suitable
 *  for optimizations
 */
void
RCE::runImpl() 
{
    Inst * inst, * cmpInst, *condInst;
    Opnd * cmpOp = NULL; 
    cmpInst = condInst = NULL;
    const Nodes& nodes = irManager->getFlowGraph()->getNodesPostOrder();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (node->isBlockNode()) {
            if(node->isEmpty()) {
                continue;
            }
            cmpInst = NULL;
            Inst* prevInst = NULL;
            for(inst = (Inst*)node->getLastInst(); inst != NULL; inst = prevInst) {
                prevInst = inst->getPrevInst();
                //find conditional instruction
                Mnemonic baseMnem = getBaseConditionMnemonic(inst->getMnemonic());
                if (baseMnem != Mnemonic_NULL) {
                    condInst = condInst ? NULL : inst;
                    cmpInst = NULL;
                } else if (condInst) {
                    //find CMP instruction corresponds to conditional instruction
                    if(inst->getMnemonic() == Mnemonic_CMP || inst->getMnemonic() == Mnemonic_UCOMISD || inst->getMnemonic() == Mnemonic_UCOMISS) {
                        if (cmpInst) {
                            //this comparison is redundant because of overrided by cmpInst
                            inst->unlink();
                            continue;
                        }
                        cmpInst = inst;
                        U_32 defCount = inst->getOpndCount(Inst::OpndRole_InstLevel|Inst::OpndRole_Def);
                        if(inst->getOpnd(defCount+1)->isPlacedIn(OpndKind_Imm)) {
                            //try to change conditional instruction to make combination available to optimize
                            cmpOp = inst->getOpnd(defCount);
                            Inst * newCondInst = NULL; 
                            Mnemonic mnem;
                            int64 val = inst->getOpnd(defCount+1)->getImmValue();
                            
                            if (val == 0) {
                                continue;
                            } else if (val == 1 && ConditionMnemonic(condInst->getMnemonic()-getBaseConditionMnemonic(condInst->getMnemonic())) == ConditionMnemonic_L){
                                mnem = Mnemonic((condInst->getMnemonic() - Mnemonic(ConditionMnemonic_L)) + Mnemonic(ConditionMnemonic_LE));
                            } else if (val == -1 && ConditionMnemonic(condInst->getMnemonic()-getBaseConditionMnemonic(condInst->getMnemonic())) == ConditionMnemonic_G) {
                                mnem = Mnemonic((condInst->getMnemonic() - Mnemonic(ConditionMnemonic_G)) + Mnemonic(ConditionMnemonic_GE));
                            } else if (val == -1 && ConditionMnemonic(condInst->getMnemonic()-getBaseConditionMnemonic(condInst->getMnemonic())) == ConditionMnemonic_B) {
                                mnem = Mnemonic((condInst->getMnemonic() - Mnemonic(ConditionMnemonic_B)) + Mnemonic(ConditionMnemonic_BE));
                            } else {
                                continue;
                            }
                            //replace old conditional instruction
                            if (condInst->hasKind(Inst::Kind_BranchInst)) {
                                BranchInst* br = (BranchInst*)condInst;
                                newCondInst = irManager->newBranchInst(mnem,br->getTrueTarget(), br->getFalseTarget(), condInst->getOpnd(0));
                            } else {
                                Mnemonic condMnem = getBaseConditionMnemonic(condInst->getMnemonic());
                                Inst::Opnds defs(condInst,Inst::OpndRole_Def|Inst::OpndRole_Explicit);
                                if (condMnem == Mnemonic_CMOVcc) {
                                    Inst::Opnds uses(condInst,Inst::OpndRole_Use|Inst::OpndRole_Explicit);
                                    newCondInst = irManager->newInst(mnem, condInst->getOpnd(defs.begin()), inst->getOpnd(uses.begin()));
                                } else if (condMnem == Mnemonic_SETcc) {
                                    newCondInst = irManager->newInst(mnem, condInst->getOpnd(defs.begin()));
                                } else {
                                    assert(0);
                                    continue;
                                }
                            }
                            newCondInst->insertAfter(condInst);
                            condInst->unlink();
                            condInst = newCondInst;
                            inst->setOpnd(defCount+1, irManager->newImmOpnd(inst->getOpnd(defCount+1)->getType(),0));
                        } 
                    //find flags affected instruction precedes cmpInst
                    } else if (instAffectsFlagsAsCmpInst(inst, condInst)) {
                        if (cmpInst) {
                            if (isSuitableToRemove(inst, condInst, cmpInst, cmpOp))
                            {
                                cmpInst->unlink();
                            } 
                        }
                        condInst = NULL; // do not optimize cmpInst any more in this block
                    } else {
                        if (inst->getOpndCount(Inst::OpndRole_Implicit|Inst::OpndRole_Def) || inst->getMnemonic() == Mnemonic_CALL) {
                            // instruction affects flags, skip optimizing cmpInst
                            condInst = NULL;
                        } else {
                            //check for moving cmpInst operands 
                            if ((inst->getMnemonic() == Mnemonic_MOV) && (inst->getOpnd(0) == cmpOp)) {
                                cmpOp = inst->getOpnd(1);
                            }
                        }
                    } 
                }//end if/else by condInst
            }//end for() by Insts
        }//end if BasicBlock
    }//end for() by Nodes
}

bool
RCE::instAffectsFlagsAsCmpInst(Inst * inst, Inst * condInst) 
{
    if (!inst->getOpndCount(Inst::OpndRole_Implicit|Inst::OpndRole_Def))
        //instruction doesn't change flags
        return false;

    ConditionMnemonic mn = ConditionMnemonic(condInst->getMnemonic()-getBaseConditionMnemonic(condInst->getMnemonic()));
    switch (inst->getMnemonic()) {
        case Mnemonic_SUB:
            // instruction changes all flags, but an overflow may affect the OF flag,
            // in that case jumping by JL, JLE, etc. is incorrect
            return (mn != ConditionMnemonic_L && mn != ConditionMnemonic_LE && mn != ConditionMnemonic_GE && mn != ConditionMnemonic_G);
        case Mnemonic_IDIV:
        case Mnemonic_CALL:
        case Mnemonic_IMUL:
        case Mnemonic_MUL:
        case Mnemonic_SBB: //SBB does not distinguish between signed and unsigned operands
            //instruction changes flags in the way doesn't correspond CMP
            return false;
        default:
            //instruction changes particular flags
            return ( mn == ConditionMnemonic_Z || mn == ConditionMnemonic_NZ) ? true : false;
    }
}

bool RCE::isSuitableToRemove(Inst * inst, Inst * condInst, Inst * cmpInst, Opnd * cmpOp)
{
    /*  cmpInst can be removed if inst defines the same operand which will be
     *  compared with zero by cmpInst
     *  Required: Native form of insts
     */
    U_32 cmpOpCount = cmpInst->getOpndCount(Inst::OpndRole_InstLevel|Inst::OpndRole_UseDef);
    if ((cmpOp == inst->getOpnd(0)) && cmpInst->getOpnd(cmpOpCount -1)->isPlacedIn(OpndKind_Imm) && (cmpInst->getOpnd(cmpOpCount -1)->getImmValue() == 0)) {
            return true;
    }
    return false;
}

}} //end namespace Ia32

