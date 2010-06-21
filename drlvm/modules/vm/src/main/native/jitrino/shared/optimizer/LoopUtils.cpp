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
 * @author Intel
 *
 */

#include "LoopUtils.h"
#include "Inst.h"

namespace Jitrino {

InvariantOpndLoopInfo* InductiveOpndLoopInfo::getStartValue() {
    return startValue;
}

InvariantOpndLoopInfo* InductiveOpndLoopInfo::getEndValue() {
    if (boundType != UNKNOWN_BOUND) {
        return endValue;
    }
    findBoundInfo();
    return endValue;
}

void InductiveOpndLoopInfo::setStartValue(InvariantOpndLoopInfo* start) {
    startValue = start;
}

void InductiveOpndLoopInfo::setEndValue(InvariantOpndLoopInfo* end) {
    endValue = end;
}

BoundType InductiveOpndLoopInfo::getBoundType() {
    if (boundType != UNKNOWN_BOUND) {
        return boundType;
    }
    findBoundInfo();
    return boundType;
}

void InductiveOpndLoopInfo::findBoundInfo() {
    BranchInst* branchInst = NULL;
    OpndLoopInfo* opndInfo = NULL;
    
    const Nodes& loopNodes =  inductionDetector->loopNode->getNodesInLoop();
    for (Nodes::const_iterator it = loopNodes.begin(), end = loopNodes.end(); it != end; it++) {
        Node* node = *it;
        branchInst = ((Inst*)node->getLastInst())->asBranchInst();
        
        if (branchInst == NULL) continue;
        
        SsaOpnd* leftOpnd = branchInst->getSrc(0)->asSsaOpnd();
        SsaOpnd* rightOpnd = (branchInst->getNumSrcOperands() == 2)
            ? branchInst->getSrc(1)->asSsaOpnd() : NULL;
        Edge* trueEdge = node->getTrueEdge();
        Edge* falseEdge = node->getFalseEdge();
        bool isTrueExit = inductionDetector->loopNode->isLoopExit(trueEdge);
        bool isFalseExit = inductionDetector->loopNode->isLoopExit(falseEdge);

        // Check if branch matches.
        if ((isTrueExit || isFalseExit) && (leftOpnd == mainOpnd || rightOpnd == mainOpnd)) {

            ComparisonModifier compMod = branchInst->getComparisonModifier();
            switch (compMod) {
            case Cmp_Zero:
                boundType = isTrueExit ? NE_BOUND : EQ_BOUND;
                endValue = inductionDetector->zero;
                break;
            case Cmp_NonZero:
                boundType = isTrueExit ? EQ_BOUND : NE_BOUND;
                endValue = inductionDetector->zero;
                break;
            case Cmp_EQ: {
                boundType = isTrueExit ? NE_BOUND : EQ_BOUND;
                opndInfo = (leftOpnd == mainOpnd)
                    ? inductionDetector->getOpndInfo(rightOpnd)
                    : inductionDetector->getOpndInfo(leftOpnd);
                endValue = (opndInfo != NULL) ? opndInfo->asInvarinat() : NULL;
                break;
            }
            case Cmp_NE_Un:
                boundType = isTrueExit ? EQ_BOUND : NE_BOUND;
                opndInfo = (leftOpnd == mainOpnd)
                    ? inductionDetector->getOpndInfo(rightOpnd)
                    : inductionDetector->getOpndInfo(leftOpnd);
                endValue = (opndInfo != NULL) ? opndInfo->asInvarinat() : NULL;
                break;
            case Cmp_GT:
            case Cmp_GT_Un:
                if (leftOpnd == mainOpnd) {
                    // indVar >= invVar
                    boundType = isTrueExit ? UPPER_EQ_BOUND : LOW_BOUND;
                    opndInfo = inductionDetector->getOpndInfo(rightOpnd);
                } else {
                    // invVar >= indVar
                    boundType = isTrueExit ? LOW_EQ_BOUND : UPPER_BOUND;
                    opndInfo = inductionDetector->getOpndInfo(leftOpnd);
                }
                endValue = (opndInfo != NULL) ? opndInfo->asInvarinat() : NULL;
                break;
            case Cmp_GTE:
            case Cmp_GTE_Un:
                if (leftOpnd == mainOpnd) {
                    // indVar >= invVar
                    boundType = isTrueExit ? UPPER_BOUND : LOW_EQ_BOUND;
                    opndInfo = inductionDetector->getOpndInfo(rightOpnd);
                } else {
                    // invVar >= indVar
                    boundType = isTrueExit ? LOW_BOUND : UPPER_EQ_BOUND;
                    opndInfo = inductionDetector->getOpndInfo(leftOpnd);
                }
                endValue = (opndInfo != NULL) ? opndInfo->asInvarinat() : NULL;
                break;
            default:;
            };
            return;
        }
    }    
}

InductionDetector::InductionDetector(MemoryManager& mm, LoopNode* loop):
    memoryManager(mm), loopNode(loop), defStack(mm) {

    zero = createConstOpnd(NULL, 0);
    one = createConstOpnd(NULL, 1);
};

OpndLoopInfo* InductionDetector::processOpnd(SsaOpnd * opnd) {
    OpndLoopInfo* resultOpnd = NULL;
    Inst* defInst = opnd->getInst();

    if (std::find(defStack.begin(), defStack.end(), defInst) != defStack.end()) {
        return createInductiveOpnd(opnd, one, NULL, zero);
    }

    Node* defNode = defInst->getNode();
    Opcode opcode = defInst->getOpcode();

    if (opcode == Op_LdConstant) {
        return createConstOpnd(opnd, defInst->asConstInst()->getValue().i4);
    }

    if (!loopNode->inLoop(defNode)) {
        return createInvariantOpnd(opnd);
    }

    defStack.push_back(defInst);

    switch (opcode) {
    case Op_Phi: {
        OpndLoopInfo* info1 = processOpnd(defInst->getSrc(0)->asSsaOpnd());
        OpndLoopInfo* info2 = (info1 != NULL && defInst->getNumSrcOperands() == 2)
            ? processOpnd(defInst->getSrc(1)->asSsaOpnd()) : NULL;
        if (info2 != NULL) {
            InductiveOpndLoopInfo* indOpnd = (info1->isInductive() && !info1->asInductive()->isPhiSplit) ?
                info1->asInductive() : ((info2->isInductive() && !info2->asInductive()->isPhiSplit) ?
                info2->asInductive() : NULL);
            InvariantOpndLoopInfo* invOpnd = info1->isInvariant() ? info1->asInvarinat() :
                (info2->isInvariant() ? info2->asInvarinat() : NULL); 
            if (indOpnd != NULL && invOpnd != NULL) {
                InductiveOpndLoopInfo* resOpnd = createInductiveOpnd(opnd, one,
                    NULL, indOpnd->getStride());
                resOpnd->startValue = invOpnd;
                resOpnd->header = opnd;                
                resOpnd->isPhiSplit = true;
                resultOpnd = resOpnd;
            }
        }
        break;
    } 
    case Op_Add:
    case Op_Sub:
    case Op_Mul: {
        SsaOpnd *op1 = defInst->getSrc(0)->asSsaOpnd();
        SsaOpnd *op2 = defInst->getSrc(1)->asSsaOpnd();
        OpndLoopInfo* info1 = processOpnd(op1);
        OpndLoopInfo* info2 = (info1 != NULL) ? processOpnd(op2) : NULL;

        if (info2 != NULL) {
            if (info1->isInvariant() && info2->isInvariant()) {
                InvariantOpndLoopInfo* invOpnd1 = info1->asInvarinat();
                InvariantOpndLoopInfo* invOpnd2 = info2->asInvarinat();
                if (invOpnd1->isConstant() && invOpnd2->isConstant()) {
                    int val1 = invOpnd1->asConstant()->getValue();
                    int val2 = invOpnd2->asConstant()->getValue();
                    if (opcode == Op_Add) {
                        resultOpnd = createConstOpnd(opnd, val1 + val2);                        
                    } else if (opcode == Op_Sub) {
                        resultOpnd = createConstOpnd(opnd, val1 - val2);
                    } else {
                        assert(opcode == Op_Mul);
                        resultOpnd = createConstOpnd(opnd, val1 * val2);
                    }
                } else {
                    resultOpnd = createInvariantOpnd(opnd);
                }
            } else {
                InductiveOpndLoopInfo* indOpnd = (info1->isInductive() && !info1->asInductive()->isPhiSplit) ?
                    info1->asInductive() : ((info2->isInductive() && !info2->asInductive()->isPhiSplit) ?
                    info2->asInductive() : NULL);
                InvariantOpndLoopInfo* invOpnd = info1->isInvariant() ? info1->asInvarinat() :
                    (info2->isInvariant() ? info2->asInvarinat() : NULL); 
                
                if (indOpnd != NULL && invOpnd != NULL) {
                    InductiveOpndLoopInfo* resOpnd = NULL;
                    if (opcode == Op_Add || opcode == Op_Sub) {
                        resOpnd = createInductiveOpnd(opnd, one, indOpnd, invOpnd);
                    } else {
                        assert(opcode == Op_Mul);
                        resOpnd = createInductiveOpnd(opnd, invOpnd, indOpnd, zero);
                    }
                    resOpnd->isPhiSplit = indOpnd->isPhiSplit;
                    resOpnd->header = indOpnd->header;
                    resultOpnd = resOpnd;
                }
            }
        }
        break;
    }
    case Op_StVar:
    case Op_LdVar: {
        resultOpnd = processOpnd(defInst->getSrc(0)->asSsaOpnd());
        if (resultOpnd != NULL) {
            resultOpnd->mainOpnd = opnd;
        }
        break;
    }
    case Op_TauArrayLen: {
        resultOpnd = processOpnd(defInst->getSrc(0)->asSsaOpnd());
        if (resultOpnd != NULL) {
            resultOpnd->mainOpnd = opnd;
        }
        break;
    }
    default:;
    };

    defStack.pop_back();
    return resultOpnd;
}

OpndLoopInfo* InductionDetector::getOpndInfo(SsaOpnd * opnd,
                                             IVDetectionMode mode) {
    // TODO: Current implementation doesn't support other modes.
    assert(mode == IGNORE_BRANCH);
    defStack.clear();
    ivMode = mode;
    return processOpnd(opnd);
}

} // namespace Jitrino
