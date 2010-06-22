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
// class BranchTranslator
//========================================================================================
/**
 *    class BranchTranslator is implementation of replacing branching for a 
 *    single loading of an operand with a conditional CMOVcc or SETcc 
 *    instruction
 *    The algorithm takes one-pass over CFG.
 *
 *    This transformer allows to reduce count of branches
 *
 *    This transformer is recommended to be inserted before all optimizations
 *    because it unites basic blocks
 *
 *    The algorithm works as follows:    
 *        
 *    1)    Finds branch instruction which performs branch to basic blocks with
 *        only instructions MOV with the same def-operand.
 *
 *    2)    If each of thus blocks has only one predecessor they and branch 
 *        instruction is replaced with conditional instruction
 *
 *    The implementation of this transformer is located Ia32BranchTrans.cpp.
 */
class BranchTranslator : public SessionAction {
protected:
    void runImpl();
    void removeConstCompare();
    void eliminateSignCheck();
    void insertCMOVs();
        
};

static ActionFactory<BranchTranslator> _btr("btr");

static Inst* findDefInstWithMove(Inst* currentInst, Opnd* opnd) {
    if (opnd->getDefScope() != Opnd::DefScope_Temporary) {
        //For variables with semi-temporary or global def scope process only moves in current block
        for (Inst* inst= currentInst->getPrevInst(); inst!=NULL; inst = inst->getPrevInst()) {
            Inst::Opnds defs(inst,Inst::OpndRole_Def|Inst::OpndRole_Explicit|Inst::OpndRole_Auxilary);
            if (defs.begin()!= defs.end()) {
                Opnd* tmpOpnd =  inst->getOpnd(defs.begin()); 
                if (tmpOpnd == opnd) {
                    if (inst->getMnemonic()!=Mnemonic_MOV) {
                        //op is not supporter by BTR 
                        return NULL;
                    }
                    return inst;
                }
            }
            
        }
        return NULL; //no more defs for this var/semitemporal in the block
    } else {
        Inst* inst = opnd->getDefiningInst();
        if (!inst || inst->getMnemonic()!=Mnemonic_MOV) {
            return NULL;
        }
        return inst;
    }
}

static Opnd * getMOVsChainSource(Inst* inst, Opnd * opnd) {
    Inst * instUp = findDefInstWithMove(inst, opnd);
    Opnd * resOpnd = opnd;
    while(instUp!=NULL) {
        assert(instUp->getMnemonic() == Mnemonic_MOV);
        resOpnd = instUp->getOpnd(1);
        instUp = findDefInstWithMove(instUp, resOpnd);
    }
    return resOpnd;
}

static bool branchDirection (int64 v1, int64 v2, OpndSize sz,ConditionMnemonic mn) {
    switch (sz) {
        case OpndSize_8:
            v1 = int64(I_8(v1));
            v2 = int64(I_8(v2));
            break;
        case OpndSize_16:
            v1 = int64(int16(v1));
            v2 = int64(int16(v2));
            break;
        case OpndSize_32:
            v1 = int64(I_32(v1));
            v2 = int64(I_32(v2));
            break;
        default:
            break;
    }

    bool branchDirection = false;
    switch (mn) {
        case ConditionMnemonic_E:
            branchDirection = v1 == v2;
            break;
        case ConditionMnemonic_NE:
            branchDirection = v1 != v2;
            break;
        case ConditionMnemonic_G:
            branchDirection = v1 > v2;
            break;
        case ConditionMnemonic_GE:
            branchDirection = v1>= v2;
            break;
        case ConditionMnemonic_L:
            branchDirection = v1 < v2;
            break;
        case ConditionMnemonic_LE:
            branchDirection = v1 <= v2;
            break;
        case ConditionMnemonic_AE:
            branchDirection = (uint64)v1 >= (uint64)v2;
            break;
        case ConditionMnemonic_A:
            branchDirection = (uint64)v1 > (uint64)v2;
            break;
        case ConditionMnemonic_BE:
            branchDirection = (uint64)v1<= (uint64)v2;
            break;
        case ConditionMnemonic_B:
            branchDirection = (uint64)v1 < (uint64)v2;
            break;
        default:
            assert(0);
            break;
    }
    return branchDirection;
}

static void mapDefsPerEdge(StlMap<Edge *, Opnd *>& defsPerEdge, Node* node, Opnd* opnd) {
    assert(opnd->getDefScope() == Opnd::DefScope_Variable);
    const Edges& inEdges = node->getInEdges();
    for (Edges::const_iterator ite = inEdges.begin(), ende = inEdges.end(); ite!=ende; ++ite) {
        Edge* edge = *ite;
        Node* prevNode = edge->getSourceNode();
        Inst* lastInst = (Inst*)prevNode->getLastInst();
        if (lastInst == NULL) {
            defsPerEdge[edge] = NULL;
            continue;
        }
        Opnd* opndDef = getMOVsChainSource(lastInst, opnd);
        defsPerEdge[edge] = opndDef;    
    }
}

void
BranchTranslator::runImpl() 
{
    irManager->calculateOpndStatistics();

    bool consts = getBoolArg("removeConstCompare", false);
    if (consts) {
        removeConstCompare();
    }

	bool signcheck = getBoolArg("eliminateSignCheck", false);
    if (signcheck) {
        eliminateSignCheck();
    }

	bool cmovs = getBoolArg("insertCMOVs", false);
	if (cmovs) {
        insertCMOVs();
    }
    
    irManager->getFlowGraph()->purgeEmptyNodes();
    irManager->getFlowGraph()->purgeUnreachableNodes();
}


void BranchTranslator::removeConstCompare() {
    MemoryManager tmpMM("Ia32BranchTransl::removeConstCompare");
    StlMap<Node *, bool> loopHeaders(tmpMM);
    LoopTree * lt = irManager->getFlowGraph()->getLoopTree();

    const Nodes& nodes = irManager->getFlowGraph()->getNodesPostOrder();
    for (Nodes::const_reverse_iterator it = nodes.rbegin(),end = nodes.rend();it!=end; ++it) {
        Node* bb = *it;
        if (lt->isLoopHeader(bb))
            loopHeaders[bb] = true;
        else
            loopHeaders[bb] = false;
    }

    for (Nodes::const_reverse_iterator it = nodes.rbegin(),end = nodes.rend();it!=end; ++it) {
        Node* bb = *it;
        if (!bb->isBlockNode() || bb->isEmpty() || bb->getOutDegree() == 1) {
            continue;
        }
        Inst * branchInst = (Inst *)bb->getLastInst();
        //check is last instruction in basic block is a conditional branch instruction
        if (branchInst==NULL || !branchInst->hasKind(Inst::Kind_BranchInst)) {
            continue;
        }
        Node * trueBB = bb->getTrueEdge()->getTargetNode();
        Node * falseBB = bb->getFalseEdge()->getTargetNode();

        ConditionMnemonic condMnem = ConditionMnemonic(branchInst->getMnemonic() - getBaseConditionMnemonic(branchInst->getMnemonic()));

        //****start check for constants comparison****

        Inst * cmpInst = branchInst->getPrevInst();
        if (cmpInst && cmpInst->getMnemonic() == Mnemonic_CMP) {
            Inst::Opnds uses(cmpInst,Inst::OpndRole_Use|Inst::OpndRole_Explicit|Inst::OpndRole_Auxilary);
            Opnd * cmpOp1 = cmpInst->getOpnd(uses.begin());
            Opnd * cmpOp2 = cmpInst->getOpnd(uses.begin()+1);

            Opnd * propogatedOp1 = getMOVsChainSource(cmpInst, cmpOp1);
            Opnd * propogatedOp2 = getMOVsChainSource(cmpInst, cmpOp2);

            if (propogatedOp1->isPlacedIn(OpndKind_Imm) && propogatedOp2->isPlacedIn(OpndKind_Imm)) {
                //If both operands are constants we can just remove the branch
                irManager->resolveRuntimeInfo(propogatedOp1);
                irManager->resolveRuntimeInfo(propogatedOp2);
                //remove "dead" edges
                if (branchDirection(propogatedOp1->getImmValue(), 
                    propogatedOp2->getImmValue(), propogatedOp1->getSize(),condMnem)) {
                        irManager->getFlowGraph()->removeEdge(bb->getFalseEdge());
                    } else {
                        irManager->getFlowGraph()->removeEdge(bb->getTrueEdge());
                    }
                    //remove CMP and Jcc instructions
                    branchInst->unlink();
                    cmpInst->unlink();
                    continue;
            } else if (cmpOp1->getDefScope() != Opnd::DefScope_Temporary) {
                //The following optimizations works correctly only if variables 
                //propagation was done within single basic block (i.e. only for variables 
                //with semi-temporary or global def scope).
                if (propogatedOp1->getDefScope() == Opnd::DefScope_Variable 
                    && cmpOp2->isPlacedIn(OpndKind_Imm) && cmpInst->getPrevInst()== NULL) 
                {
                    //If first operand is variable and second is const and no side effects are 
                    //done in the current basic block we still can try to make some optimizations.
                    //All the assignments to first operands in previous block(s) are propagated
                    //and in case if it was assigned to constant blocks are merged and conditional 
                    //jump removed with single branch.
                    if(loopHeaders[bb])
                        continue;

                    assert(cmpOp1->getDefScope() != Opnd::DefScope_Temporary);
                    StlMap<Edge *, Opnd *> defInsts(irManager->getMemoryManager());
                    mapDefsPerEdge(defInsts, bb, propogatedOp1);
                    for (StlMap<Edge *, Opnd *>::iterator eit = defInsts.begin(); eit != defInsts.end(); eit++) {
                        Edge * edge = eit->first;
                        Opnd * opnd = eit->second;
                        if (opnd == NULL || !opnd->isPlacedIn(OpndKind_Imm)) {
                            continue; //can't retarget this edge -> var is not a const
                        }
                        if (branchDirection(opnd->getImmValue(), cmpOp2->getImmValue(),cmpOp1->getSize(),condMnem)) {
                            irManager->getFlowGraph()->replaceEdgeTarget(edge, trueBB, true);
                        } else {
                            irManager->getFlowGraph()->replaceEdgeTarget(edge, falseBB, true);
                        }
                        for(Inst * copy = (Inst *)bb->getFirstInst();copy!=NULL; copy=copy->getNextInst()) {
                            if (copy != branchInst && copy !=cmpInst) {
                                Node * sourceBB = edge->getSourceNode();
                                Inst * lastInst = (Inst*)sourceBB->getLastInst();
                                Inst * newInst = copy->getKind() == Inst::Kind_I8PseudoInst?
                                    irManager->newI8PseudoInst(Mnemonic_MOV,1,copy->getOpnd(0),copy->getOpnd(1)):
                                irManager->newCopyPseudoInst(Mnemonic_MOV,copy->getOpnd(0),copy->getOpnd(1));
                                if (lastInst->getKind()== Inst::Kind_BranchInst) {
                                    //WAS: sourceBB->prependInst(newInst, lastInst);
                                    //create new block instead of prepending to branchInst
                                    //some algorithms like I8Lowerer are very sensitive to CMP/JCC pattern
                                    //and fails if any inst is inserted between CMP and JCC
                                    irManager->getFlowGraph()->spliceBlockOnEdge(edge, newInst, true);
                                } else {
                                    sourceBB->appendInst(newInst);
                                }
                            }
                        }
                    }
                } else if (propogatedOp1->getDefScope() == Opnd::DefScope_SemiTemporary) {
                    //TODO: merge DefScope_SemiTemporary & DefScope_Variable if-branches

                    assert(cmpOp1->getDefScope() != Opnd::DefScope_Temporary);
                    //try to reduce ObjMonitorEnter pattern
                    Inst * defInst = cmpInst;
                    bool stopSearch = false;
                    //look for Mnemonic_SETcc def for cmpOp1 in the current block (it has SemiTemporary kind)
                    while (1) {
                        defInst = defInst->getPrevInst();
                        if (defInst == NULL) {
                            break;
                        }
                        Inst::Opnds defs(defInst,Inst::OpndRole_Def|Inst::OpndRole_Explicit|Inst::OpndRole_Auxilary);
                        for (Inst::Opnds::iterator ito = defs.begin(); ito != defs.end(); ito = defs.next(ito)){
                            Opnd * opnd = defInst->getOpnd(ito);
                            if (opnd == cmpOp1) {
                                Mnemonic mnem = getBaseConditionMnemonic(defInst->getMnemonic());
                                ConditionMnemonic cm = ConditionMnemonic(defInst->getMnemonic()-mnem);
                                if (mnem == Mnemonic_SETcc && cmpOp2->isPlacedIn(OpndKind_Imm) && cmpOp2->getImmValue() == 0) {
                                    if(cm == condMnem) {
                                        defInst->unlink();
                                        cmpInst->unlink();
                                        branchInst->unlink();
                                        bb->appendInst(irManager->newBranchInst((Mnemonic)(Mnemonic_Jcc+reverseConditionMnemonic(cm)),((BranchInst*)branchInst)->getTrueTarget(),((BranchInst*)branchInst)->getFalseTarget()));
                                        stopSearch = true;
                                        break;
                                    } 
                                } else {
                                    stopSearch = true;
                                    break;
                                }
                            }
                        }
                        Inst::Opnds flags(defInst,Inst::OpndRole_Def|Inst::OpndRole_Implicit);
                        if (stopSearch || ((flags.begin() != flags.end()) && defInst->getOpnd(flags.begin())->getRegName() == RegName_EFLAGS)) {
                            break;
                        }
                    }
                    continue;
                }
            }
            //****end check for constants comparison****
        }
    }
}

void BranchTranslator::eliminateSignCheck() {
    const Nodes& nodes = irManager->getFlowGraph()->getNodesPostOrder();
    for (Nodes::const_reverse_iterator it = nodes.rbegin(),end = nodes.rend();it!=end; ++it) {
        Node* bb = *it;
        if (!bb->isBlockNode() || bb->isEmpty() || bb->getOutDegree()== 1){
            continue;
        }
        Inst * brachInst = (Inst *)bb->getLastInst();
        //check is last instruction in basic block is a conditional branch instruction
        if(brachInst == NULL || !brachInst->hasKind(Inst::Kind_BranchInst)) {
            continue;
        }
        //get successors of bb

        Edge * trueEdge = bb->getTrueEdge();
        Edge * falseEdge = bb->getFalseEdge();
        Node * trueBB = trueEdge->getTargetNode();
        Node * falseBB = falseEdge->getTargetNode();

        Inst * falseInst = (Inst *)falseBB->getFirstInst();
        Inst * cmpInst = brachInst->getPrevInst();

        if (cmpInst && falseInst) {
            U_32 prevDefCount = cmpInst->getOpndCount(Inst::OpndRole_InstLevel|Inst::OpndRole_Def);
            U_32 falseDefCount = falseInst->getOpndCount(Inst::OpndRole_InstLevel|Inst::OpndRole_Def);
            Inst * nextFalse = falseInst->getNextInst();

            // check if we have following in current BB:
            // cmp x, 0
            // jge trueBB
            if ( cmpInst->getMnemonic() == Mnemonic_CMP && brachInst->getMnemonic() == Mnemonic_JGE &&
                cmpInst->getOpnd(prevDefCount+1)->isPlacedIn(OpndKind_Imm) && cmpInst->getOpnd(prevDefCount+1)->getImmValue() == 0) 
            {

                // check if we have following in false BB:
                // w = add x, y
                if ( falseInst->getMnemonic() == Mnemonic_ADD &&
                    falseInst->getOpnd(falseDefCount) == cmpInst->getOpnd(prevDefCount)) 
                {

                    // check if that was the only instruction in false BB or there was also
                    // mov v, w
                    if ( falseBB->getInstCount() == 1 || (falseBB->getInstCount() == 2 && 
                        nextFalse->getMnemonic() == Mnemonic_MOV && 
                        nextFalse->getOpnd(1) == falseInst->getOpnd(0)) ) 
                    {

                        //check if trueBB is successor of falseBB
                        bool canBeRemoved = true;
                        Node * nextBB = trueBB;
                        if (falseBB->getOutEdges().front()->getTargetNode() != nextBB)
                            canBeRemoved = false;

                        //check if bb is the only predecessor of falseBB
                        const Edges& fEdges  = falseBB->getInEdges();
                        for (Edges::const_iterator  edge = fEdges.begin(); edge != fEdges.end(); ++edge) {
                            Edge * e = *edge;
                            if (e->getSourceNode() != bb)
                                canBeRemoved = false;
                        }
                        if (!canBeRemoved)
                            continue;

                        // eliminating branch

                        if (nextFalse)
                            nextFalse->unlink();

                        Opnd* x = cmpInst->getOpnd(prevDefCount);
                        Opnd* tmp = irManager->newOpnd(x->getType());
                        // mov tmp, x
                        bb->appendInst(irManager->newCopyPseudoInst(Mnemonic_MOV, tmp, x));
                        // sar tmp, 31 (spreading sign for the whole register)
                        bb->appendInst(irManager->newInstEx(Mnemonic_SAR, 1, tmp, tmp, irManager->newImmOpnd(irManager->getTypeManager().getInt8Type(), 31)));
                        // and tmp, y
                        bb->appendInst(irManager->newInstEx(Mnemonic_AND, 1, tmp, tmp, falseInst->getOpnd(falseDefCount+1)));
                        // add w, tmp
                        bb->appendInst(irManager->newInstEx(Mnemonic_ADD, 1, falseInst->getOpnd(0), x, tmp));
                        if (nextFalse)
                            bb->appendInst(nextFalse);

                        irManager->getFlowGraph()->removeEdge(trueEdge);
                        irManager->getFlowGraph()->removeEdge(falseEdge);
                        irManager->getFlowGraph()->addEdge(bb, nextBB);
                        irManager->getFlowGraph()->removeNode(falseBB);
                        cmpInst->unlink();
                        brachInst->unlink();
                    }
                }

            }//end if BasicBlock
        }//end for() by Nodes
    }
}


void BranchTranslator::insertCMOVs() {
    const Nodes& nodes = irManager->getFlowGraph()->getNodesPostOrder();
    for (Nodes::const_reverse_iterator it = nodes.rbegin(),end = nodes.rend();it!=end; ++it) {
        Node* bb = *it;
        if (bb->isBlockNode()){
            if(bb->isEmpty())
                continue;

            Inst * inst = (Inst *)bb->getLastInst();
            //check is last instruction in basic block is a conditional branch instruction
            if(inst && inst->hasKind(Inst::Kind_BranchInst)) {
                //get successors of bb
                if(bb->getOutEdges().size() == 1)
                    continue;

                Edge * trueEdge = bb->getTrueEdge();
                Edge * falseEdge = bb->getFalseEdge();
                Node * trueBB = trueEdge->getTargetNode();
                Node * falseBB = falseEdge->getTargetNode();

                ConditionMnemonic condMnem = ConditionMnemonic(inst->getMnemonic() - getBaseConditionMnemonic(inst->getMnemonic()));

                Inst * trueInst = (Inst *)trueBB->getFirstInst();
                Inst * falseInst = (Inst *)falseBB->getFirstInst();
                if(trueBB && falseInst && trueBB->getInstCount() == 1 && falseBB->getInstCount() == 1 && trueInst->getMnemonic() == Mnemonic_MOV && falseInst->getMnemonic() == Mnemonic_MOV && trueInst->getOpnd(0) == falseInst->getOpnd(0) && trueInst->getOpnd(0)->getMemOpndKind() == MemOpndKind_Null) {
                    //check is bb is only predecessor for trueBB and falseBB
                    bool canBeRemoved = true;
                    Node * nextBB = trueBB->getOutEdges().front()->getTargetNode();
                    if (falseBB->getOutEdges().front()->getTargetNode() != nextBB)
                        canBeRemoved = false;

                    const Edges& tEdges  = trueBB->getInEdges();
                    for (Edges::const_iterator  edge = tEdges.begin(); edge != tEdges.end(); ++edge) {
                        Edge * e = *edge;
                        if (e->getSourceNode() != bb)
                            canBeRemoved = false;
                    }
                    const Edges& fEdges  = falseBB->getInEdges();
                    for (Edges::const_iterator  edge = fEdges.begin(); edge != fEdges.end(); ++edge) {
                        Edge * e = *edge;
                        if (e->getSourceNode() != bb)
                            canBeRemoved = false;
                    }
                    if (!canBeRemoved)
                        continue;

                    Opnd * tfOp= trueInst->getOpnd(0);
                    Opnd * tsOp= trueInst->getOpnd(1);
                    Opnd * fsOp= falseInst->getOpnd(1);
                    int64 v1 = tsOp->getImmValue();
                    int64 v2 = fsOp->getImmValue();
                    if (tsOp->isPlacedIn(OpndKind_Imm) && 
                        fsOp->isPlacedIn(OpndKind_Imm) && 
                        ((v1==0 && v2==1)|| (v1==1 && v2==0))) 
                    {
                        bb->prependInst(irManager->newCopyPseudoInst(Mnemonic_MOV, tfOp, v1?fsOp:tsOp), inst);
                        bb->prependInst(irManager->newInstEx(Mnemonic(Mnemonic_SETcc+(v1?condMnem:reverseConditionMnemonic(condMnem))), 1, tfOp,tfOp),inst);
                    } else {
                        //insert loading of initial value for operand
                        bb->prependInst(irManager->newCopyPseudoInst(Mnemonic_MOV, tfOp, fsOp), inst);
                        if (tsOp->isPlacedIn(OpndKind_Imm)) {
                            Opnd * tempOpnd = irManager->newOpnd(tsOp->getType());
                            Inst * tempInst = irManager->newCopyPseudoInst(Mnemonic_MOV, tempOpnd, tsOp);
                            bb->prependInst(tempInst, inst);
                            tsOp = tempOpnd;
                        }
                        //insert conditional CMOVcc instruction 
                        bb->prependInst(irManager->newInstEx(Mnemonic(Mnemonic_CMOVcc+condMnem), 1, tfOp,tfOp,tsOp),inst);
                    }
                    //link bb with successor of trueBB and falseBB
                    irManager->getFlowGraph()->replaceEdgeTarget(bb->getFalseEdge(), nextBB, true);
                    irManager->getFlowGraph()->removeEdge(bb->getTrueEdge());
                    inst->unlink();
                    irManager->getFlowGraph()->removeNode(falseBB);
                    irManager->getFlowGraph()->removeNode(trueBB);
                }
            } 
        }//end if BasicBlock
    }//end for() by Nodes

}


} //end namespace Ia32
}
