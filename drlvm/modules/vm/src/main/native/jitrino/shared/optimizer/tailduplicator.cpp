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
 * @author Intel, Pavel A. Ozhdikhin
 *
 */

#include "Log.h"
#include "tailduplicator.h"
#include "irmanager.h"
#include "Dominator.h"
#include "Loop.h"
#include "escapeanalyzer.h"
#include "FlowGraph.h"

namespace Jitrino {

DEFINE_SESSION_ACTION(RedundantBranchMergingPass, taildup, "Redundant Branch Merging/Factoring")

void
RedundantBranchMergingPass::_run(IRManager& irm) {
    computeDominators(irm);
    DominatorTree* dominatorTree = irm.getDominatorTree();
    assert(dominatorTree->isValid());
    TailDuplicator tm(irm, dominatorTree);
    tm.doTailDuplication();
}

DEFINE_SESSION_ACTION(HotPathSplittingPass, hotpath, "Profile Guided Hot Path Splitting")

void
HotPathSplittingPass::_run(IRManager& irm) {
    computeDominatorsAndLoops(irm);
    DominatorTree* dominatorTree = irm.getDominatorTree();
    LoopTree* loopTree = irm.getLoopTree();
    assert(dominatorTree->isValid() && loopTree->isValid());
    TailDuplicator tm(irm, dominatorTree);
    tm.doProfileGuidedTailDuplication(loopTree);
}

bool
TailDuplicator::isMatchingBranch(BranchInst* br1, BranchInst* br2) {
    if(br1->getComparisonModifier() != br2->getComparisonModifier())
        return false;
    if(br1->getNumSrcOperands() == 2 && br2->getNumSrcOperands() == 2) {
        if(br1->getSrc(0) == br2->getSrc(0) && br1->getSrc(1) == br2->getSrc(1))
            return true;
    }
    return false;
}

void
TailDuplicator::process(DefUseBuilder& defUses, DominatorNode* dnode) {
    // Process children.
    DominatorNode* child;
    for(child = dnode->getChild(); child != NULL; child = child->getSiblings()) {
        process(defUses, child);
    }

    Node* node = dnode->getNode();
    BranchInst* branch = ((Inst*)node->getLastInst())->asBranchInst();
    if(branch == NULL)
        return;

    // Tail duplicate redundant branches in dominated nodes.
    for(child = dnode->getChild(); child != NULL; child = child->getSiblings()) {
        Node* cnode = child->getNode();
        BranchInst* branch2 = ((Inst*)cnode->getLastInst())->asBranchInst();
        if(branch2 != NULL && node->findTargetEdge(cnode) == NULL && isMatchingBranch(branch, branch2))
            tailDuplicate(defUses, node, cnode);
    }
}

void
TailDuplicator::tailDuplicate(DefUseBuilder& defUses, Node* idom, Node* tail) 
{
    if(tail->getInDegree() != 2)
        return;
    Node* t1 =  idom->getTrueEdge()->getTargetNode();
    Node* f1 =  idom->getFalseEdge()->getTargetNode();
    
    Node* p1 =  tail->getInEdges().front()->getSourceNode();
    Node* p2 =  tail->getInEdges().back()->getSourceNode();

    Node* t2;

    if(_dtree->dominates(t1, p1) && _dtree->dominates(f1, p2)) {
        t2 = p1;
    } else if(_dtree->dominates(t1, p2) && _dtree->dominates(f1, p1)) {
        t2 = p2;
    } else {
        return;
    }

    if(Log::isEnabled()) {
        Log::out() << "Tail duplicate ";
        FlowGraph::printLabel(Log::out(), tail);
        Log::out() << ::std::endl;
    }

    ControlFlowGraph& fg = _irm.getFlowGraph();
    Node* copy = FlowGraph::tailDuplicate(_irm, t2, tail, defUses);
    FlowGraph::foldBranch(fg, ((Inst*)copy->getLastInst())->asBranchInst(), true);
    FlowGraph::foldBranch(fg, ((Inst*)tail->getLastInst())->asBranchInst(), false);
}

void
TailDuplicator::doTailDuplication() {
    MemoryManager mm("TailDuplicator::doTailDuplication.mm");
    ControlFlowGraph& fg = _irm.getFlowGraph();

    DefUseBuilder defUses(mm);
    defUses.initialize(fg);

    DominatorNode* dnode = _dtree->getDominatorRoot();

    process(defUses, dnode);
}

void
TailDuplicator::profileGuidedTailDuplicate(LoopTree* ltree, DefUseBuilder& defUses, Node* node) {
    ControlFlowGraph& fg = _irm.getFlowGraph();
    
    if(node->getInDegree() < 2)
        // Nothing to duplicate
        return;

    double heatThreshold = _irm.getHeatThreshold();
    
    double nodeCount = node->getExecCount();
    Log::out() << "Try nodeCount = " << nodeCount << ::std::endl;
    if(nodeCount < 0.90 * heatThreshold)
        return;
    
    const Edges& inEdges = node->getInEdges();

    Node* hotPred = NULL;
    Edge* hotEdge = NULL;
    Edges::const_iterator j;
    for(j = inEdges.begin(); j != inEdges.end(); ++j) {
        Edge* edge = *j;
        Node* pred = edge->getSourceNode();
        if(!pred->isBlockNode()) {
            hotPred = NULL;
            break;
        }
        if(pred->getExecCount() > 0.90 * nodeCount) {
            hotPred = pred;
            hotEdge = edge;
        }
    }

    if(hotPred != NULL) {
        Log::out() << "Duplicate " << ::std::endl;
        double hotProb = hotEdge->getEdgeProb();
        double hotFreq = hotPred->getExecCount() * hotProb;
        if(Log::isEnabled()) {
            Log::out() << "Hot Pred = ";
            FlowGraph::printLabel(Log::out(), hotPred);
            Log::out() << ::std::endl;
            Log::out() << "HotPredProb = " << hotProb << ::std::endl;
            Log::out() << "HotPredFreq = " << hotPred->getExecCount() << ::std::endl;
            Log::out() << "HotFreq = " << hotFreq << ::std::endl;
        }
        if(hotFreq > 1.10 * nodeCount)
            assert(0);
        if(hotFreq > nodeCount)
            hotFreq = nodeCount;
        else if(hotFreq < 0.75 * nodeCount)
            return;
        Node* newNode = FlowGraph::tailDuplicate(_irm, hotPred, node, defUses);
        if(Log::isEnabled()) {
            Log::out() << "New node = ";
            FlowGraph::printLabel(Log::out(), newNode);
            Log::out() << ::std::endl;
        }
        newNode->setExecCount(hotFreq);
        Edge* stBlockEdge = newNode->getUnconditionalEdge();
        if(stBlockEdge != NULL) {
            Node* stBlock =  stBlockEdge->getTargetNode();
            if(stBlock->getId() > newNode->getId())
                stBlock->setExecCount(newNode->getExecCount());
        }
        node->setExecCount(nodeCount-hotFreq);
        hotPred->findTargetEdge(newNode)->setEdgeProb(hotProb);

        // Test if node is source of back edge.
        Node* loopHeader = NULL;
        const Edges& outEdges = node->getOutEdges();
        for(j = outEdges.begin(); j != outEdges.end(); ++j) {
            Edge* edge = *j;
            Node* succ = edge->getTargetNode();
            if(succ->getDfNum() != MAX_UINT32 && ltree->isLoopHeader(succ) && succ->getDfNum() < node->getDfNum()) {
                loopHeader = succ;
            }
        }
        
        if(loopHeader != NULL) {
            // Create new loop header:
            Node* newHeader = fg.createBlockNode(_irm.getInstFactory().makeLabel());

            const Edges& loopInEdges = loopHeader->getInEdges();
            for(j = loopInEdges.begin(); j != loopInEdges.end();) {
                Edge* edge = *j;
                ++j;
                Node* pred = edge->getSourceNode();
                if(pred != newNode) {
                    fg.replaceEdgeTarget(edge, newHeader);
                    newHeader->setExecCount(newHeader->getExecCount()+pred->getExecCount()*edge->getEdgeProb());
                }
            }
            fg.addEdge(newHeader, loopHeader);            
        }
    }
}

void 
TailDuplicator::doProfileGuidedTailDuplication(LoopTree* ltree) {
    MemoryManager mm("TailDuplicator::doProfileGuidedTailDuplication.mm");
    ControlFlowGraph& fg = _irm.getFlowGraph();
    if(!fg.hasEdgeProfile())
        return;

    DefUseBuilder defUses(mm);
    defUses.initialize(fg);

    Nodes nodes(mm);
    fg.getNodesPostOrder(nodes);
    Nodes::reverse_iterator i;
    for(i = nodes.rbegin(); i != nodes.rend(); ++i) {
        Node* node = *i;
        if(Log::isEnabled()) {
            Log::out() << "Consider ";
            FlowGraph::printLabel(Log::out(), node);
            Log::out() << ::std::endl;
        }
        if(!node->isBlockNode() || node == fg.getReturnNode())
            continue;

        if(ltree->isLoopHeader(node))
            continue;

        profileGuidedTailDuplicate(ltree, defUses, node);
    }
}


} //namespace Jitrino 
