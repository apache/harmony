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
 * @author Evgueni Brevnov
 *
 */

#include <stdlib.h>

#include "dabce.h"
#include "optpass.h"
#include "FlowGraph.h"

namespace Jitrino {

DEFINE_SESSION_ACTION(DynamicABCEPass, dabce, "Dynamic elimination of array bound checks")

void DynamicABCEPass::_run(IRManager& irm) {
    
    DynamicABCE dynamicABCE(irm, this);
    dynamicABCE.run();
}

DynamicABCE::DynamicABCE(IRManager& irm, SessionAction* sa) :
    INC_SEQ_PROB(0.8), IN_BOUNDS_PROB(0.9), 
   irManager(irm), memoryManager(irm.getMemoryManager()),
   flowGraph(irm.getFlowGraph()), typeManager(irm.getTypeManager()),
   instFactory(irm.getInstFactory()), opndManager (irm.getOpndManager()),   
   arrayAccesses(memoryManager), inductionInfo(memoryManager),
   eliminationInfo(memoryManager), monotonicityInfo(memoryManager),
   loopsToOptimize(memoryManager), flags(*irm.getOptimizerFlags().dabceFlags) {
    
    // Build dominators.
    OptPass::computeDominatorsAndLoops(irManager, true);
    dce = new (memoryManager) DeadCodeEliminator(irManager);
    sortByHotness = new (memoryManager) HotnessSorter;
    loopTree = irm.getLoopTree();
}

void DynamicABCE::readFlags(Action* argSource, DynamicABCEFlags* flags) {
    flags->sizeThreshold = argSource->getIntArg("dabce.sizeThreshold", 1);
    flags->hotnessThreshold = argSource->getIntArg("dabce.hotnessThreshold", 1);       
}

void DynamicABCE::showFlags(std::ostream& os) {
    os << "  Dynamic ABCE flags:" << std::endl;
    os << "    dabce.sizeThreshold     - maximum number of nodes in loop" << std::endl;
    os << "    dabce.hotnessThreshold  - minimum hotness of loop header " << std::endl;
}

void DynamicABCE::run() {
    assert(loopTree->isValid());

    // No loops, nothing to optimize.
    if (!loopTree->hasLoops()) return;

    LoopNode* topLevelLoop = (LoopNode*)loopTree->getRoot();
    
    // Find all loops.
    findLoopsToOptimize(topLevelLoop);
    
    // Sort loops by hotness.
    std::sort(loopsToOptimize.begin(), loopsToOptimize.end(), *sortByHotness);
    
    // Optimize found loops.
    optimizeLoops();
}

void DynamicABCE::findLoopsToOptimize(LoopNode* topLevelLoop) {
    LoopNode* innerLoop;
    
    if (topLevelLoop->getHeader() != NULL) {
        loopsToOptimize.push_back(topLevelLoop->getHeader());
    }
    for (innerLoop = topLevelLoop->getChild(); innerLoop != NULL;
            innerLoop = innerLoop->getSiblings()) {
        findLoopsToOptimize(innerLoop);
    }
}

void DynamicABCE::optimizeLoops() {
    InductionDetector* inductionDetector = NULL;
    Node* loopHeader = NULL;
    StlVector<Node*>::iterator loopIt;
    ArrayAccessInfo::iterator  accessIt;
    
    // Apply transformation from top level to inner most loops.
    while (!loopsToOptimize.empty()) {     
        clearAll();

        loopIt = loopsToOptimize.begin();
        loopHeader = *loopIt;
        loopsToOptimize.erase(loopIt);
        

        optimizedLoop = loopTree->getLoopNode(loopHeader, false);
        assert(optimizedLoop != NULL);

        if (Log::isEnabled()) {
            Log::out() << "Optimizing loop " << loopHeader->getId() << std::endl;
    
        }
        
        double relativeHotness = loopHeader->getExecCount() / flowGraph.getEntryNode()->getExecCount();
        
        if (relativeHotness < flags.hotnessThreshold) {
            if (Log::isEnabled()) {
                Log::out() << "FAILED: Loop is too cold.\n";
            }
            // No need to traverse other loops. They are even colder.
            break;
        }

        if (optimizedLoop->getNodesInLoop().size() > relativeHotness * flags.sizeThreshold) {
            if (Log::isEnabled()) {
                Log::out() << "FAILED: Loop is too large.\n";
            }
            continue;
        }
               
        // Find candidates for elimintation.
        findArrayAccesses();
        
        // Check if we found at least one candidate for elimination.
        if (Log::isEnabled() && arrayAccesses.empty()) {
            Log::out() << "FAILED: No bound checks found.\n";
        }
        
        inductionDetector = InductionDetector::create(memoryManager, optimizedLoop);

        while (!arrayAccesses.empty()) {
            accessIt = arrayAccesses.begin();            
            const ArrayAccessTemplate& arrayAccess = **accessIt;
            arrayAccesses.erase(accessIt);
            
            eliminateBoundsCheck(inductionDetector, arrayAccess);
            
            // Loop tree can be updated so reload related info.
            if (optimizedLoop != loopTree->getLoopNode(loopHeader, false)) {
                optimizedLoop = loopTree->getLoopNode(loopHeader, false);
                inductionDetector = InductionDetector::create(memoryManager, optimizedLoop);
                findArrayAccesses();
            }
        }
    }    
}

bool DynamicABCE::eliminateBoundsCheck(InductionDetector* inductionDetector,
                                       const ArrayAccessTemplate& arrayAccess) {
    InductiveOpndLoopInfo* inductionInfo = NULL;
    InductiveOpndLoopInfo* base = NULL;
    InvariantOpndLoopInfo* scale = NULL;
    InvariantOpndLoopInfo* stride = NULL;
    InvariantOpndLoopInfo* start = NULL;
    InvariantOpndLoopInfo* end = NULL;
    IndexEliminationInfo* indexElimintaionInfo = NULL;
    bool canReachBound = false;
    bool result = false;
    
    indexElimintaionInfo = eliminationInfo[arrayAccess.array];
    
    if (indexElimintaionInfo == NULL) {
        indexElimintaionInfo = new (memoryManager) IndexEliminationInfo(memoryManager);
        eliminationInfo[arrayAccess.array] = indexElimintaionInfo;  
    } else {
        if (indexElimintaionInfo->has(arrayAccess.index)) {
            // Constraints already generated. Just remove bounds check.
            instFactory.makeTauSafe(arrayAccess.checkBoundsInst->getDst())->
                insertAfter(arrayAccess.checkBoundsInst);
            arrayAccess.checkBoundsInst->unlink();
            result = true;
            
            if (Log::isEnabled()) {
                Log::out() << "SUCCEED: Array bound check ";
                arrayAccess.checkBoundsInst->print(Log::out());
                Log::out() << " was removed.\n";
            }
        }
        return result;
    }
    
    // Check that array reference and array lengh defined out of the loop.
    if (isDefInLoop(arrayAccess.array) || isDefInLoop(arrayAccess.arrayLen)) {
        if (Log::isEnabled()) {
            Log::out() << "FAILED: Array ";
            arrayAccess.array->print(Log::out());
            Log::out() << " is not loop invariant.\n";
        }
        goto done;
    }
    
    // Create initial subgraph structure.
    subGraph = new (memoryManager) SubGraph(memoryManager);
    subGraph->setEntryNode(subGraph->createBlockNode(instFactory.makeLabel()));
    subGraph->setExitNode(subGraph->createBlockNode(instFactory.makeLabel()));
    subGraph->setSpecialExitNode(subGraph->createBlockNode(instFactory.makeLabel()));
    subGraph->addEdge(subGraph->getEntryNode(), subGraph->getExitNode());
    
    
    inductionInfo = getSimplifiedInduction(inductionDetector, arrayAccess);
    
    if (inductionInfo == NULL) {
        if (Log::isEnabled()) {
            Log::out() << "FAILED: Index ";
            arrayAccess.index->print(Log::out());
            Log::out() << " is not loop inductive variable.\n";
        }
        goto done;
    }
    
    base = inductionInfo->getBase();
    assert(base->getOpnd() == base->getBase()->getOpnd());
    start = base->getStartValue();          
    end = base->getEndValue();        
    
    // start and end values should be detected as loop invariants...
    if (start == NULL || end == NULL) {
        if (Log::isEnabled()) {
            Log::out() << "FAILED: start/end value is not loop invariant.\n";
        }
        goto done;
    }

    // ... and should be defined out of the loop.
    if (isDefInLoop(start->getOpnd()) || isDefInLoop(end->getOpnd())) {
        if (Log::isEnabled()) {
            Log::out() << "FAILED: start/end value has loop scope.\n";
        }
        goto done;        
    }

    // Promote start/end value from variable to temporary.
    start = promoteVarToTmp(start, inductionDetector);
    end   = promoteVarToTmp(end, inductionDetector);
    base->setStartValue(start);
    base->setEndValue(end);
    
    if (!isProperMonotonicity(base, start, end)) {
        if (Log::isEnabled()) {
            Log::out() << "FAILED: Wrong monotonicity.\n";
        }
        goto done;        
    }
    
    if (inductionInfo != base) {
        scale = inductionInfo->getScale();
        stride = inductionInfo->getStride(); 
    }
    
    canReachBound = base->getBoundType() == LOW_EQ_BOUND ||
        base->getBoundType() == UPPER_EQ_BOUND; 
    // Generate constraint for initial value.
    genBoundConstraint(scale, start, stride, arrayAccess.arrayLen, true);
    // Generate constraint for final value.
    genBoundConstraint(scale, end, stride, arrayAccess.arrayLen, canReachBound);
    // Link subGraph node.
    linkSubGraph();
    // Remove bounds check.    
    instFactory.makeTauSafe(arrayAccess.checkBoundsInst->getDst())->
        insertAfter(arrayAccess.checkBoundsInst);
    arrayAccess.checkBoundsInst->unlink();

    if (Log::isEnabled()) {
        Log::out() << "SUCCEED: Array bound check ";
        arrayAccess.checkBoundsInst->print(Log::out());
        Log::out() << " was removed.\n";
    }
    
    result = true;
done:
    (*indexElimintaionInfo)[arrayAccess.index] = result;
    return result;
}

InvariantOpndLoopInfo*
DynamicABCE::promoteVarToTmp(InvariantOpndLoopInfo* invOpnd,
                             InductionDetector* inductionDetector) {
    SsaOpnd* tmpOpnd = NULL;
    SsaVarOpnd* varOpnd = NULL;
    
    if (!invOpnd->getOpnd()->isSsaVarOpnd()) {
        return invOpnd;        
    }
    
    varOpnd = invOpnd->getOpnd()->asSsaVarOpnd();
    
    if (varOpnd->getInst()->isStVar()) {
        tmpOpnd = varOpnd->getInst()->getSrc(0)->asSsaTmpOpnd();
    } else {
        tmpOpnd = opndManager.createSsaTmpOpnd(varOpnd->getType());
        subGraph->getEntryNode()->appendInst(instFactory.makeLdVar(tmpOpnd, varOpnd));        
    }
    
    return inductionDetector->createInvariantOpnd(tmpOpnd);
}

InductiveOpndLoopInfo*
DynamicABCE::getSimplifiedInduction(InductionDetector* inductionDetector,                                    
                                    const ArrayAccessTemplate& arrayAccess) {
    SsaOpnd* complexScale = NULL;
    SsaOpnd* complexStride = NULL;
    Modifier mulMod = Modifier(Overflow_None) | Modifier(Exception_Never);
    Modifier addMod = mulMod | Modifier(Strict_No);
    Node* subGraphEnter = NULL;
    OpndLoopInfo* opndLoopInfo = NULL;
    InvariantOpndLoopInfo* scale = NULL;
    InductiveOpndLoopInfo* base = NULL;
    InvariantOpndLoopInfo* stride = NULL;
    InductiveOpndLoopInfo* indexInfo = NULL;
    InductiveOpndLoopInfo* simplifiedInductionInfo = NULL;
    

    if (inductionInfo.has(arrayAccess.index)) {
        return inductionInfo[arrayAccess.index];
    }
    
    opndLoopInfo = inductionDetector->getOpndInfo(arrayAccess.index);    
    
    if (opndLoopInfo == NULL || !opndLoopInfo->isInductive()) {
        goto done;
    }

    indexInfo = opndLoopInfo->asInductive();
    scale = indexInfo->getScale();
    stride = indexInfo->getStride();
    subGraphEnter = subGraph->getEntryNode();    

    // Scale and sride must be defined out of the loop.
    if (isDefInLoop(scale->getOpnd()) || isDefInLoop(stride->getOpnd())) {
        goto done;        
    }
    
    // Check if its already simple induction.
    if (indexInfo == indexInfo->getBase()) {
        simplifiedInductionInfo = indexInfo;
        goto done;
    }

    do {
        SsaOpnd* tmpOpnd1 = NULL;
        SsaOpnd* tmpOpnd2 = NULL;

        scale = indexInfo->getScale();
        base = indexInfo->getBase();
        stride = indexInfo->getStride();
        
        assert(scale != NULL && base != NULL && stride != NULL);        

        if (isDefInLoop(scale->getOpnd()) || isDefInLoop(stride->getOpnd())) {
            goto done;        
        }

        if (complexStride == NULL) {
            complexStride = stride->getOpnd();
        } else if (!stride->isConstant() || stride->asConstant()->getValue() != 0) {
            if (complexScale != NULL) {
                tmpOpnd1 = opndManager.createSsaTmpOpnd(complexScale->getType());
                subGraphEnter->appendInst(instFactory.makeMul(mulMod, tmpOpnd1, complexScale, stride->getOpnd()));
            } else {
                tmpOpnd1 = stride->getOpnd();
            }
            tmpOpnd2 = opndManager.createSsaTmpOpnd(complexStride->getType());
            subGraphEnter->appendInst(instFactory.makeAdd(addMod, tmpOpnd2, complexStride, tmpOpnd1));
            complexStride = tmpOpnd2;
        }

        if (complexScale == NULL) {
            complexScale = scale->getOpnd();                
        } else if (!scale->isConstant() || scale->asConstant()->getValue() != 1) {
            tmpOpnd1 = opndManager.createSsaTmpOpnd(complexScale->getType());
            subGraphEnter->appendInst(instFactory.makeMul(mulMod, tmpOpnd1, complexScale, scale->getOpnd()));
            complexScale = tmpOpnd1;            
        }
        
    } while (indexInfo != base &&
        (indexInfo = inductionDetector->getOpndInfo(base->getOpnd())->asInductive()) != NULL);
    
    scale = complexScale ? inductionDetector->createInvariantOpnd(complexScale) : NULL;
    stride = complexStride ? inductionDetector->createInvariantOpnd(complexStride) : NULL;
    simplifiedInductionInfo =
        inductionDetector->createInductiveOpnd(arrayAccess.index, scale, base, stride);
 done:
    inductionInfo[arrayAccess.index] = simplifiedInductionInfo;
    return simplifiedInductionInfo;
}

bool DynamicABCE::isProperMonotonicity(InductiveOpndLoopInfo* base,
                                       InvariantOpndLoopInfo* startValue,
                                       InvariantOpndLoopInfo* endValue) {
    Modifier aluMod;
    Type* int32Type;
    VarOpnd* fk;
    Node* startNode = NULL;
    Node* subGraphExit = NULL;
    Node* increasingNode = NULL;
    Node* decreasingNode = NULL;
    InvariantOpndLoopInfo* scale;
    InvariantOpndLoopInfo* stride;
    bool isScaleEqualOne = false;
    bool isStrideEqualZero = false;
    bool result = false;
    
    assert(base->getOpnd() == base->getBase()->getOpnd());

    if (monotonicityInfo.has(base->getOpnd())) {
        return monotonicityInfo[base->getOpnd()];
    }
    
    aluMod = Modifier(Overflow_None) | Modifier(Exception_Never) | Modifier(Strict_No);
    int32Type = typeManager.getInt32Type();
    subGraphExit = subGraph->getExitNode();

    // Generate bound values check.
    switch (base->getBoundType()) {
    case LOW_BOUND:
    case LOW_EQ_BOUND:
        increasingNode = subGraph->getSpecialExitNode();
        if (startValue->isConstant() && endValue->isConstant() &&
            startValue->asConstant()->getValue() >= endValue->asConstant()->getValue()) {
            decreasingNode = subGraphExit;
        } else {
            decreasingNode = subGraph->createBlockNode(instFactory.makeLabel());
            subGraph->addEdge(decreasingNode, subGraph->getSpecialExitNode(), 0.1);
            subGraph->addEdge(decreasingNode, subGraphExit, 0.9);
            decreasingNode->appendInst(instFactory.makeBranch(Cmp_GTE,
                int32Type->tag, startValue->getOpnd(), endValue->getOpnd(),
                ((Inst*)subGraphExit->getLabelInst())->asLabelInst()));
        }
        break;
    case UPPER_BOUND:
    case UPPER_EQ_BOUND:
        decreasingNode = subGraph->getSpecialExitNode();
        if (startValue->isConstant() && endValue->isConstant() &&
            startValue->asConstant()->getValue() <= endValue->asConstant()->getValue()) {
            increasingNode = subGraphExit;
        } else {
            increasingNode = subGraph->createBlockNode(instFactory.makeLabel());
            subGraph->addEdge(increasingNode, subGraph->getSpecialExitNode(), 0.1);
            subGraph->addEdge(increasingNode, subGraphExit, 0.9);
            increasingNode->appendInst(instFactory.makeBranch(Cmp_GTE,
                int32Type->tag, endValue->getOpnd(), startValue->getOpnd(),
                ((Inst*)subGraphExit->getLabelInst())->asLabelInst()));
        }
        break;
    default:
        goto done;
    }
    
    // Generate monotonicity check.
    fk = opndManager.createVarOpnd(int32Type, false);
    scale = base->getScale();
    stride = base->getStride();
    
    isScaleEqualOne = (scale->isConstant() && scale->asConstant()->getValue() == 1); 
    // If scale is equal to one it doesn't affect direction of monotonicity. 
    if (!isScaleEqualOne) {
        startNode = subGraph->createBlockNode(instFactory.makeLabel());
        Node* monCheckNode = flowGraph.createBlockNode(instFactory.makeLabel());
        SsaOpnd* one = opndManager.createSsaTmpOpnd(int32Type);        
        SsaOpnd* tk = opndManager.createSsaTmpOpnd(int32Type);
        startNode->appendInst(instFactory.makeLdConst(one, 1));
        startNode->appendInst(instFactory.makeSub(aluMod, tk, scale->getOpnd(), one));
        
        Node* trueNode = subGraph->createBlockNode(instFactory.makeLabel());
        Node* falseNode = subGraph->createBlockNode(instFactory.makeLabel());

        subGraph->addEdge(startNode, trueNode, 0.5);
        subGraph->addEdge(trueNode, monCheckNode, 1.0);
        subGraph->addEdge(startNode, falseNode, 0.5);
        
        startNode->appendInst(instFactory.makeBranch(Cmp_Zero, int32Type->tag,
            tk, ((Inst*)trueNode->getLabelInst())->asLabelInst()));

        // Generate true node instructions.
        SsaOpnd* strideOpnd = stride->getOpnd();
        if (strideOpnd == NULL) {
            strideOpnd = opndManager.createSsaTmpOpnd(int32Type);
            trueNode->appendInst(instFactory.makeLdConst(strideOpnd,
                stride->asConstant()->getValue()));
        }
        trueNode->appendInst(instFactory.makeStVar(fk, strideOpnd));            
        
        // Generate false node instructions.
        isStrideEqualZero = (stride->isConstant()&&
            stride->asConstant()->getValue() == 0); 
        SsaTmpOpnd* rk =  opndManager.createSsaTmpOpnd(int32Type);
        if (!isStrideEqualZero) {
            SsaTmpOpnd* tauSafe = opndManager.createSsaTmpOpnd(typeManager.getTauType());
            SsaTmpOpnd* div = opndManager.createSsaTmpOpnd(typeManager.getFloatType());
            falseNode->appendInst(instFactory.makeTauSafe(tauSafe));
            falseNode->appendInst(instFactory.makeTauDiv(aluMod, div,
                stride->getOpnd(), tk, tauSafe));
            falseNode->appendInst(instFactory.makeAdd(aluMod, rk,
                startValue->getOpnd(), div));
        } else {
            falseNode->appendInst(instFactory.makeCopy(rk, startValue->getOpnd()));
        }

        Node* subTrueNode = subGraph->createBlockNode(instFactory.makeLabel());
        Node* subFalseNode = subGraph->createBlockNode(instFactory.makeLabel());        

        subGraph->addEdge(falseNode, subTrueNode, 0.5);
        subGraph->addEdge(falseNode, subFalseNode, 0.5);
        subGraph->addEdge(subTrueNode, monCheckNode, 1.0);
        subGraph->addEdge(subFalseNode, monCheckNode, 1.0);

        falseNode->appendInst(instFactory.makeBranch(Cmp_GT, int32Type->tag,
            tk, ((Inst*)subTrueNode->getLabelInst())->asLabelInst()));
        
        // Generate true subnode instructions.
        trueNode->appendInst(instFactory.makeStVar(fk, rk));
        
        // Generate false subnode instructions.
        SsaTmpOpnd* negrk =  opndManager.createSsaTmpOpnd(int32Type);
        falseNode->appendInst(instFactory.makeNeg(negrk, rk));
        falseNode->appendInst(instFactory.makeStVar(fk, negrk));
        
        // Generate final check.
        SsaTmpOpnd* checkOpnd = opndManager.createSsaTmpOpnd(int32Type);
        monCheckNode->appendInst(instFactory.makeLdVar(checkOpnd, fk));
        monCheckNode->appendInst(instFactory.makeBranch(Cmp_GTE,
            int32Type->tag, checkOpnd, ((Inst*)increasingNode->getLabelInst())->asLabelInst()));
        // Link monotonicity check to bounds check nodes.
        subGraph->addEdge(monCheckNode, increasingNode, INC_SEQ_PROB);
        subGraph->addEdge(monCheckNode, decreasingNode, 1 - INC_SEQ_PROB);
    } else {        
        if (!stride->isConstant()) {
            // Generate runtime check.
            startNode = flowGraph.createBlockNode(instFactory.makeLabel());
            startNode->appendInst(instFactory.makeBranch(Cmp_GTE, int32Type->tag,
                stride->getOpnd(), ((Inst*)increasingNode->getLabelInst())->asLabelInst()));
            subGraph->addEdge(startNode, increasingNode, INC_SEQ_PROB);
            subGraph->addEdge(startNode, decreasingNode, 1 - INC_SEQ_PROB);
        } else {
            // Scale is equal to 1 here => only stride determines direction of monotonicity.
            if (stride->asConstant()->getValue() >= 0) {
                // That's not an inductive variable/
                assert(stride->asConstant()->getValue() != 0);
                // Increasing sequence.
                if (base->getBoundType() == UPPER_BOUND || base->getBoundType() == UPPER_EQ_BOUND) {
                    startNode = increasingNode;
                } else {
                    goto done;
                }                
            } else {
                assert(stride->asConstant()->getValue() < 0);
                // Decreasing sequence.
                if (base->getBoundType() == LOW_BOUND || base->getBoundType() == LOW_EQ_BOUND) {
                    startNode = decreasingNode;
                } else {
                    goto done;
                }                
            }
        }        
    }    
    // Link monotonicity check.
    if (startNode != NULL) {
        // Remove link between enter & exit nodes.
        Node* subGraphEnter = subGraph->getEntryNode();
        Edge* edge = subGraphEnter->findEdge(true, subGraphExit); 
        if (edge != NULL) {
            subGraph->removeEdge(edge);
        }
        subGraph->addEdge(subGraphEnter, startNode);
    }
    result = true;
done:
    monotonicityInfo[base->getOpnd()] = result;
    return result;
}

void DynamicABCE::genBoundConstraint(InvariantOpndLoopInfo* scale,
                                     InvariantOpndLoopInfo* baseValue,
                                     InvariantOpndLoopInfo* stride,
                                     SsaOpnd* arrayLength,
                                     bool canReachBound) {
    Modifier aluMod;
    ComparisonModifier comMod;
    Type* int32Type = NULL;
    SsaOpnd* mulRes = NULL;
    SsaOpnd* addRes = NULL;
    Node* newNode = NULL;
    Node* subGraphExit = NULL;
    
    aluMod = Modifier(Overflow_None) | Modifier(Exception_Never) | Modifier(Strict_No);
    int32Type = typeManager.getInt32Type();
    subGraphExit = subGraph->getExitNode();
    
    newNode = subGraph->createBlockNode(instFactory.makeLabel());    

    if (scale == NULL || (scale->isConstant() && scale->asConstant()->getValue() == 1)) {
        mulRes = baseValue->getOpnd();
    } else {
        mulRes = opndManager.createSsaTmpOpnd(int32Type);
        newNode->appendInst(instFactory.makeMul(aluMod, mulRes,
            scale->getOpnd(), baseValue->getOpnd()));        
    }
    
    if (stride == NULL || (stride->isConstant() && stride->asConstant()->getValue() == 0)) {
        addRes = mulRes;
    } else {
        addRes = opndManager.createSsaTmpOpnd(int32Type);
        newNode->appendInst(instFactory.makeAdd(aluMod, addRes,
            mulRes, stride->getOpnd()));                
    }
    
    comMod = canReachBound ? Cmp_GTE_Un : Cmp_GT_Un; 
    newNode->appendInst(instFactory.makeBranch(comMod, int32Type->tag,
        addRes, arrayLength, ((Inst*)subGraph->getSpecialExitNode()->getLabelInst())->asLabelInst()));

    // Insert new node.
    Edge* inEdge = findUnconditionalInEdge(subGraph->getExitNode());
    subGraph->replaceEdgeTarget(inEdge, newNode, false);

    subGraph->addEdge(newNode, subGraphExit, IN_BOUNDS_PROB);
    subGraph->addEdge(newNode, subGraph->getSpecialExitNode(), 1 - IN_BOUNDS_PROB);
}

Node* DynamicABCE::getClonedLoop() {

    if (clonedLoop != NULL) return clonedLoop;
    
    if (Log::isEnabled()) {
        Log::out() << "Duplicating original loop...\n";
    }
    
    U_32 maxNodeId = flowGraph.getMaxNodeId();
    StlBitVector nodesInLoop(memoryManager, maxNodeId);
    const Nodes& loopNodes = optimizedLoop->getNodesInLoop();
    
    for (Nodes::const_iterator it = loopNodes.begin(), end = loopNodes.end(); it != end; it++) {
        Node* node = *it;
        nodesInLoop.setBit(node->getId());
    }

    DefUseBuilder defUseBuilder(memoryManager);    
    NodeRenameTable nodeRenameTable(memoryManager, loopNodes.size());
    OpndRenameTable opndRenameTable(memoryManager, loopNodes.size(), true);
    
    defUseBuilder.initialize(flowGraph);
    
    clonedLoop = FlowGraph::duplicateRegion(irManager,
        optimizedLoop->getHeader(), nodesInLoop, defUseBuilder, nodeRenameTable, opndRenameTable);
     
     return clonedLoop;    
}

void DynamicABCE::linkSubGraph() {
    Node* tagetNode = NULL;
    Edge* inEdge = NULL;
    
    if (Log::isEnabled()) {
        Log::out() << "Inserting condition subgraph:\n";
        FlowGraph::printHIR(Log::out(), *subGraph, irManager.getMethodDesc());
    }
    
    inEdge = findUnconditionalInEdge(optimizedLoop->getHeader());    
    tagetNode = inEdge->getTargetNode();
    
    // Insert subgraph.
    flowGraph.spliceFlowGraphInline(inEdge, *subGraph);
    
    // Replace subgraph exit.
    assert(subGraph->getExitNode()->hasOnlyOnePredEdge());
    inEdge = subGraph->getExitNode()->getInEdges().front();
    flowGraph.replaceEdgeTarget(inEdge, tagetNode, true);

    // Cleanup phase.
    dce->eliminateDeadCode(true);
    dce->eliminateUnreachableCode();
    OptPass::computeDominatorsAndLoops(irManager, true);

    // Retarget all edges from specialExitNode to clonedLoop.
    while (subGraph->getSpecialExitNode()->getInDegree() != 0) {
        inEdge = subGraph->getSpecialExitNode()->getInEdges().front();
        flowGraph.replaceEdgeTarget(inEdge, getClonedLoop(), true);
    }
    
    /*
    if (!irManager.isSsaUpdated()) {
        // TODO: This would not be required if FlowGraph::duplicateRegion or
        // OptPass::fixupSsa worked better.
        OptPass::dessa(irManager);
        OptPass::ssa(irManager);        
    }
    */
    irManager.setSsaUpdated();
    // Cleanup phase.
    dce->eliminateDeadCode(true);
    dce->eliminateUnreachableCode();
    OptPass::computeDominatorsAndLoops(irManager, true);
}

Edge* DynamicABCE::findUnconditionalInEdge(Node* targetNode) {
    Edge* inEdge = NULL;
    const Edges& inEdges = targetNode->getInEdges();
    for (Edges::const_iterator it = inEdges.begin(), end = inEdges.end(); it != end; it++) {
        if (!optimizedLoop->isBackEdge(*it)) {
            inEdge = *it;
            break;
        }
    }
    assert(inEdge != NULL);
    return inEdge;
}

void DynamicABCE::findArrayAccesses() {
    ArrayAccessTemplate* arrayAccess = NULL;
    // Find all array accesses.
    arrayAccesses.clear();
    const Nodes& loopNodes = optimizedLoop->getNodesInLoop();
    for (Nodes::const_iterator it = loopNodes.begin(), end = loopNodes.end(); it != end; ++it) {
        Node* node = *it;
        for (Inst* inst = (Inst*)node->getFirstInst(); inst != NULL; inst = inst->getNextInst()) {
            switch (inst->getOpcode()) {
            case Op_TauCheckBounds:
            case Op_TauCheckLowerBound:
            case Op_TauCheckUpperBound: {
                arrayAccess = new (memoryManager) ArrayAccessTemplate();
                fillTemplate(arrayAccess, inst);
                
                // Filter out not fully recognized patterns.
                if (arrayAccess->array != NULL && arrayAccess->index != NULL) {
                    arrayAccesses.push_back(arrayAccess);
                } else {
                    if (Log::isEnabled()) {
                        Log::out() << "Skip not fully recognized pattern: ";
                        arrayAccess->checkBoundsInst->print(Log::out());
                        Log::out() << std::endl;
                    }
                }
               break;
            }
            default:;
            };
        }
    }
}

void DynamicABCE::fillTemplate(ArrayAccessTemplate* arrayAccess, Inst* checkInst) {
    Inst* ldBaseInst = NULL;

    assert(checkInst->getOpcode() == Op_TauCheckBounds);
    
    arrayAccess->checkBoundsInst = checkInst;
    arrayAccess->index = checkInst->getSrc(1)->asSsaOpnd();
    
    SsaOpnd* lenOpnd = checkInst->getSrc(0)->asSsaOpnd();
    Inst* lenInst = lenOpnd->getInst();
    
    if (lenInst->getOpcode() == Op_TauArrayLen) {
        arrayAccess->array = lenInst->getSrc(0)->asSsaOpnd();
    }    
    arrayAccess->arrayLen = lenInst->getDst()->asSsaOpnd();

    Node* node = checkInst->getNode()->getUnconditionalEdgeTarget();
    for (Inst* inst = (Inst*)node->getFirstInst(); inst != NULL; inst = inst->getNextInst()) {
        Opcode opcode = inst->getOpcode();
        if (opcode == Op_LdArrayBaseAddr) {
            if (arrayAccess->array == inst->getSrc(0)) {                
                ldBaseInst = inst;
            }
        } else if (opcode == Op_AddScaledIndex &&
            arrayAccess->index == inst->getSrc(1) && arrayAccess->array == NULL) {
            assert(ldBaseInst == NULL);
            ldBaseInst = inst->getSrc(0)->asSsaOpnd()->getInst();
            assert((ldBaseInst->getOpcode() == Op_LdArrayBaseAddr)||(ldBaseInst->getOpcode() == Op_LdVar));
            arrayAccess->array = ldBaseInst->getSrc(0)->asSsaOpnd();
            break;
        }
    }
}

bool DynamicABCE::isDefInLoop(SsaOpnd* opnd) {
    return opnd != NULL && optimizedLoop->inLoop(opnd->getInst()->getNode());   
}


void DynamicABCE::clearAll() {
    arrayAccesses.clear();
    eliminationInfo.clear();
    inductionInfo.clear();
    optimizedLoop = NULL;
    clonedLoop = NULL;
    subGraph = NULL;
}

} // namespace Jitrino
