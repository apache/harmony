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
 * @author Intel, Mikhail Y. Fursov
 */

#include "Ia32CodeLayout.h"
#include "Ia32IRManager.h"
#include "Log.h"
#include "Ia32Printer.h"
#include "Ia32CodeLayoutTopDown.h"
#include "Ia32CodeLayoutBottomUp.h"
namespace Jitrino
{
namespace Ia32 {

static ActionFactory <Layouter> _layout("layout");

#define ACCEPTABLE_DOUBLE_PRECISION_LOSS  0.000000001 

Linearizer::Linearizer(IRManager* irMgr) : irManager(irMgr) {
}



static Edge* findEdgeToAddJump(BasicBlock* block) {
    Inst* lastInst = (Inst*)block->getLastInst();
    BasicBlock* layoutSuccessor = block->getLayoutSucc();
    if (lastInst && lastInst->hasKind(Inst::Kind_BranchInst)) {
        BranchInst* br = (BranchInst*)lastInst;
        if (br->isDirect() && br->getFalseTarget() != layoutSuccessor) {
            return block->getFalseEdge();
        }
        return NULL;
    }
    if (lastInst && lastInst->hasKind(Inst::Kind_ControlTransferInst) && !lastInst->hasKind(Inst::Kind_CallInst)) { 
        //unconditional jumps (jump inst, ret, switch)
        return NULL;
    }
    
    if (block->getOutDegree() == 1 && block->getExceptionEdge()!=NULL) {
        return NULL; //throw
    }

    Edge* uncondEdge = block->getUnconditionalEdge();
    assert(uncondEdge);
    return uncondEdge->getTargetNode() == layoutSuccessor ? NULL : uncondEdge;
}


/**  Fix branches to work with the code layout */
void Linearizer::fixBranches() {
    MemoryManager tmpMM("Linearizer::fixBranches");
    Nodes nodes(tmpMM);
    irManager->getFlowGraph()->getNodes(nodes);
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it)  {
        Node* node = *it;
        if (node->isBlockNode()) {
            BasicBlock* block = (BasicBlock*)node;
            BasicBlock* layoutSuccessor = block->getLayoutSucc();
            
            // If block ends with true branch to its layout successor reverse
            // the branch to fallthrough to the layout successor
            Inst * lastInst = (Inst*)block->getLastInst();
            if (lastInst!=NULL && lastInst->hasKind(Inst::Kind_BranchInst)) {
                BranchInst* bInst =  (BranchInst *)lastInst;
                if (bInst->isDirect()) {
                    Node * target = bInst->getTrueTarget();
                    if (target == layoutSuccessor) {
                        reverseBranchIfPossible(block);
                    }
                }
            }
            
            //check for every out edge that additional jump block is needed
            Edge* edge = findEdgeToAddJump(block);
            if (edge!=NULL) {
                BasicBlock * jmpBlk = addJumpBlock(edge);
                //  Put jump block after the block in code layout
                jmpBlk->setLayoutSucc(layoutSuccessor);
                block->setLayoutSucc(jmpBlk);
            }   
        }
    }
}


/**  Reverse branch predicate. 
     We assume that branch is the last instruction in the node.
  */
bool Linearizer::reverseBranchIfPossible(Node * bb) {
    //  Last instruction of the basic block should be predicated branch:
    //      (p1) br  N
    //  Find p2 in (p1,p2) or (p2,p1) produced by the compare.
    assert(bb->isBlockNode() && !bb->isEmpty() && ((Inst*)bb->getLastInst())->hasKind(Inst::Kind_BranchInst));
    BranchInst * branch = (BranchInst *)bb->getLastInst();
    assert(branch->isDirect());
    Edge* edge = bb->getTrueEdge();
    if (canEdgeBeMadeToFallThrough(edge)) {
        branch->reverse(irManager);
    }
    return true;
}


 /**  Add block containing jump instruction to the fallthrough successor
  *  after this block
  */
BasicBlock* Linearizer::addJumpBlock(Edge * fallEdge) {
    irManager->invalidateLivenessInfo();
    Inst* jumpInst = irManager->newJumpInst();
    Node* jumpBlock = irManager->getFlowGraph()->spliceBlockOnEdge(fallEdge, jumpInst);
    return (BasicBlock*)jumpBlock;
}



// Returns true if edge can be converted to a fall-through edge (i.e. an edge
// not requiring a branch) assuming the edge's head block is laid out after the tail block. 
bool Linearizer::canEdgeBeMadeToFallThrough(Edge *edge) {
    Inst *inst = (Inst*)edge->getSourceNode()->getLastInst();
    if (edge->isUnconditionalEdge()) {
        return inst == NULL || !inst->hasKind(Inst::Kind_SwitchInst);
    }
    assert(edge->isTrueEdge());
    assert(inst->hasKind(Inst::Kind_BranchInst));
    return ((BranchInst*)inst)->canReverse();
}

bool Linearizer::isBlockLayoutDone() {
    bool lastBlockFound = false;
    const Nodes& nodes = irManager->getFlowGraph()->getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it)  {
        Node* node = *it;
        if (!node->isBlockNode()) {
            continue;
        }
        BasicBlock* block  = (BasicBlock*)node;
        if (block->getLayoutSucc()==NULL) {
           if (lastBlockFound) {
               return false; //two blocks without layout successor found
           }
           lastBlockFound = true;
        }
    }
    return lastBlockFound;
}

void Linearizer::linearizeCfg() {
    assert(!irManager->isLaidOut());
#ifdef _DEBUG
    bool livenessIsOkOnStart = irManager->hasLivenessInfo();
#endif
    
    linearizeCfgImpl();

    fixBranches();
    
    assert(isBlockLayoutDone());
    irManager->setLaidOut(true);
#ifdef _DEBUG
    checkLayout(irManager);
    if (livenessIsOkOnStart) {
        assert(irManager->ensureLivenessInfoIsValid());
    }
#endif
}


void Linearizer::doLayout(LinearizerType t, IRManager* irManager) {
    if (t == TOPDOWN) {
        TopDownLayout linearizer(irManager);
        linearizer.linearizeCfg();
    } else if (t == BOTTOM_UP) {
        BottomUpLayout linearizer(irManager);
        linearizer.linearizeCfg();
    } else {
        assert (t == TOPOLOGICAL);
        TopologicalLayout linearizer(irManager);
        linearizer.linearizeCfg();
    }
}

void Linearizer::checkLayout(IRManager* irm) {
#ifdef _DEBUG
    U_32 maxNodes = irm->getFlowGraph()->getMaxNodeId();
    StlVector<U_32> numVisits(irm->getMemoryManager(), maxNodes, 0); 
    StlVector<bool> isBB(irm->getMemoryManager(), maxNodes, false);
    
    // Find basic blocks
    bool wasLast = false;
    const Nodes& nodes = irm->getFlowGraph()->getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it)  {
        Node *node = *it;
        if (node->isBlockNode()) {
            isBB[node->getId()] = true;
            BasicBlock* layoutSucc =((BasicBlock*)node)->getLayoutSucc();
            if (layoutSucc==NULL )  {
                assert(!wasLast);
                wasLast = true;
            } else {
                U_32 id = layoutSucc->getId();
                numVisits[id]++;
                assert(numVisits[id] == 1);
            }
        }
    }

    
    // Check that every basic block has been visited once
    bool wasFirst = false;
    for (U_32 i = 0; i < maxNodes; i++) {
        U_32 correctNumVisits = isBB[i] ? 1 : 0;
        if (numVisits[i] != correctNumVisits) {
            if (isBB[i] && numVisits[i] == 0 && !wasFirst) {
                wasFirst = true;
            } else {
                assert(0);
            }
        }
    }

    //check fallthru successors
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it)  {
        Node *node = *it;
        if (node->isBlockNode()) {
            BasicBlock* block = (BasicBlock*)node;
            BasicBlock* layoutSucc = block->getLayoutSucc();
            Edge* fallEdge = layoutSucc==NULL ? NULL : block->findEdgeTo(true, layoutSucc);
            if (fallEdge == NULL) {
                Inst *inst = (Inst*)block->getLastInst();
                assert(!inst->hasKind(Inst::Kind_BranchInst)); //false edge must be fallthru
                bool ok = inst->hasKind(Inst::Kind_ControlTransferInst) && !inst->hasKind(Inst::Kind_CallInst);
                ok = ok || (block->getOutDegree() == 1 && block->getExceptionEdge() != NULL);
                assert(ok);
            } else {
                assert(fallEdge->isUnconditionalEdge() || fallEdge->isFalseEdge());
            }
        }
    }

#endif 
}




/////////////////////////////////////////////////////////////////////
/////////////////// TOPOLOGICAL LAYOUT //////////////////////////////

void TopologicalLayout::linearizeCfgImpl() {
    BasicBlock* prev = NULL;
    const Nodes& postOrderedNodes = irManager->getFlowGraph()->getNodesPostOrder();
    for (Nodes::const_reverse_iterator it = postOrderedNodes.rbegin(), end = postOrderedNodes.rend(); it!=end; ++it) {
        Node* node = *it;    
        if (!node->isBlockNode()) {
            continue;
        }
        BasicBlock* block = (BasicBlock*)node;
        if (prev!= NULL) {
            prev->setLayoutSucc(block);
        } else {
            assert(block == irManager->getFlowGraph()->getEntryNode());
        }
        prev=block; 
    }
}

//////////////////////////////////////////////////////////////////////////
///////////////////IRTransformer impl ////////////////////////////////////
void Layouter::runImpl() {
    const char* params = getArg("type");
    ControlFlowGraph* fg = irManager->getFlowGraph();
    bool hasEdgeProfile = fg->hasEdgeProfile();
    bool isClinit = irManager->getMethodDesc().isClassInitializer();
    Linearizer::LinearizerType type = hasEdgeProfile && !isClinit ? Linearizer::BOTTOM_UP : Linearizer::TOPOLOGICAL;
    if (params != NULL) {
        if (!strcmp(params, "bottomup")) {
            type = Linearizer::BOTTOM_UP;
        } else if (!strcmp(params, "topdown")) {
            type = Linearizer::TOPDOWN;
        } else if (!strcmp(params, "mixed")) {
            LoopTree* lt = fg->getLoopTree();
            type = lt->hasLoops() ? Linearizer::BOTTOM_UP  : Linearizer::TOPDOWN;
        } else if (!strcmp(params, "topological")) {
            type = Linearizer::TOPOLOGICAL;
        } else {
            if (Log::isEnabled()) {
                Log::out() << "Layout: unsupported layout type: '"<<params<<"' using default\n";;
            }
        }
    }
    if (type != Linearizer::TOPOLOGICAL && !hasEdgeProfile) {
        type = Linearizer::TOPOLOGICAL;
        if (Log::isEnabled()) {
            Log::out() << "Layout: not edge profile found: '"<<params<<"' using topological layout\n";;
        }
    }
    
    fg->purgeEmptyNodes();
    fg->getLoopTree()->rebuild(false);
    irManager->fixEdgeProfile();

    Linearizer::doLayout(type, &getIRManager());
}

}}//namespace
