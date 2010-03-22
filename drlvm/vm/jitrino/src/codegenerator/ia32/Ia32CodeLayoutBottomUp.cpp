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

#include "Ia32CodeLayoutBottomUp.h"
#include "Ia32IRManager.h"
#include "Log.h"
namespace Jitrino
{
namespace Ia32 {
/**
* Class to perform bottom-up block layout. 
* The layout algorithm is similar to the one described in the paper
* "Profile Guided Code Positioning" by Hansen & Pettis. 
*/

BottomUpLayout::BottomUpLayout(IRManager* irm) : 
Linearizer(irm), 
mm("Ia32::bottomUpLayout"), 
firstInChain(mm, irm->getFlowGraph()->getNodeCount(), false),
lastInChain(mm, irm->getFlowGraph()->getNodeCount(), false),
prevInLayoutBySuccessorId(mm, irm->getFlowGraph()->getNodeCount(), NULL)
{
}


struct edge_comparator {
    bool operator() (const Edge* e1, const Edge* e2) const { //true -> e1 is first
        double v1 = getEdgeExecCount(e1);
        double v2 = getEdgeExecCount(e2);
        return   v1 > v2;
    }
    static double getEdgeExecCount(const Edge* e) { 
        return e->getSourceNode()->getExecCount() * e->getEdgeProb();
    }

};

void BottomUpLayout::linearizeCfgImpl() {
    assert(irManager->getFlowGraph()->isEdgeProfileConsistent());
    StlVector<Edge*> sortedEdges(mm);
    const Nodes& nodes = irManager->getFlowGraph()->getNodes();
    sortedEdges.reserve(nodes.size() * 3);
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it)  {
        Node* node = *it;
        const Edges& edges = node->getOutEdges();
        sortedEdges.insert(sortedEdges.end(), edges.begin(), edges.end());
    }
    //GCC 3.4 passes NULLs to comparator if usual std::sort is used here..
    std::stable_sort(sortedEdges.begin(), sortedEdges.end(), edge_comparator());
    for(StlVector<Edge*>::const_iterator it = sortedEdges.begin(), itEnd = sortedEdges.end(); it!=itEnd; it++) {
        Edge* edge  = *it;
        layoutEdge(edge);
    }
    //create one-block-chains from blocks that was not laid out
    //these blocks are dispatch successors or are blocks connected by in/out edges to already laid out chains
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it)  {
        Node* node = *it;
        if (node->isBlockNode()) {
            BasicBlock* block = (BasicBlock*)node;
            if (block->getLayoutSucc() == NULL && !lastInChain[block->getDfNum()]) {
                firstInChain[block->getDfNum()] = true;
                lastInChain[block->getDfNum()] = true;
            }
        }
    }
    combineChains();
}

void  BottomUpLayout::layoutEdge(Edge *edge) {
    Node* tailNode = edge->getSourceNode();
    Node* headNode = edge->getTargetNode();
    if (!headNode->isBlockNode() || !tailNode->isBlockNode()) {
        return;
    }
    if (headNode == irManager->getFlowGraph()->getEntryNode()) { //prolog node should be first in layout
        return;
    }
    if (tailNode == headNode) {
        return; //backedge to self
    }
    BasicBlock* tailBlock = (BasicBlock*)tailNode;
    if (tailBlock->getLayoutSucc()!=NULL) {
        return;  //tailBlock is layout predecessor for another successor
    }
    BasicBlock* headBlock = (BasicBlock*)headNode;
    if (prevInLayoutBySuccessorId[headBlock->getDfNum()]!=NULL) {
        return; // head was already laid out (in other chain)
    }
    if (lastInChain[tailBlock->getDfNum()] && firstInChain[headBlock->getDfNum()]) {
        BasicBlock* tailOfHeadChain = headBlock;
        while (tailOfHeadChain->getLayoutSucc()!=NULL) {
            tailOfHeadChain = tailOfHeadChain->getLayoutSucc();
        }   
        if (tailOfHeadChain == tailBlock) {
            return; // layout must be acyclic
        }
    }

    tailBlock->setLayoutSucc(headBlock);
    prevInLayoutBySuccessorId[headBlock->getDfNum()] = tailBlock;

    BasicBlock* tailPred = prevInLayoutBySuccessorId[tailBlock->getDfNum()];
    if (tailPred) {
        assert(lastInChain[tailBlock->getDfNum()]);
        lastInChain[tailBlock->getDfNum()] = false;
    } else {
        firstInChain[tailBlock->getDfNum()] = true;
    }// here we have valid first
    firstInChain[headBlock->getDfNum()] = false;

    BasicBlock* newLast = headBlock;
    while (newLast->getLayoutSucc()!=NULL) {
        newLast = newLast->getLayoutSucc();
    }
    lastInChain[newLast->getDfNum()] = true;
}


struct chains_comparator{
    const StlVector<BasicBlock*>& prevInLayoutBySuccessorId;
    const BasicBlock* prolog;
    chains_comparator(const StlVector<BasicBlock*>& _prevInLayoutBySuccessorId, BasicBlock* _prolog) 
        : prevInLayoutBySuccessorId(_prevInLayoutBySuccessorId), prolog(_prolog) 
    {};
    
    
    bool operator() (const BasicBlock* c1, const BasicBlock* c2) const { 
        if (c1 == prolog) {
            return true;
        }
        if (c2 == prolog) {
            return false;
        }
        double fromC1ToC2 = calcEdgesWeight(c1, c2);
        double fromC2ToC1 = calcEdgesWeight(c2, c1);
        if (fromC1ToC2 > fromC2ToC1) {
            return true; //c1 is first in topological order
        } else if (fromC1ToC2 < fromC2ToC1) {
            return false; //c2 is first in topological order
        }
        return c1 > c2; //any stable order..
    }

    double calcEdgesWeight(const BasicBlock* c1, const BasicBlock* c2) const {
        double d = 0.0;
        //distance is sum of exec count of c1 blocks out edges c1 to c2;
        for (const BasicBlock* b = c1; b!=NULL; b = b->getLayoutSucc()) {
            const Edges& outEdges = b->getOutEdges();
            for (Edges::const_iterator ite = outEdges.begin(), ende = outEdges.end(); ite!=ende; ++ite) {
                Edge* e= *ite;
                Node* node = e->getTargetNode();
                if (node != b->getLayoutSucc() && node->isBlockNode()) {
                    const BasicBlock* targetBlock = (BasicBlock*)node;
                    //look if node is in c2 chain
                    const BasicBlock* targetChain = findChain(targetBlock);
                    if (targetChain == c2) {
                        double dd = b->getExecCount() * e->getEdgeProb();
                        d+=dd;
                    }
                }
            }
        }
        return d;
    }

    const BasicBlock* findChain(const BasicBlock* bb) const  {
        const BasicBlock* prev  = bb;
        while ((prev = prevInLayoutBySuccessorId[bb->getDfNum()])!=NULL) {
            bb = prev;
        }
        return bb;
    }
};



void BottomUpLayout::combineChains() {
    StlVector<BasicBlock*> chains(mm);
    const Nodes& nodes = irManager->getFlowGraph()->getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it)  {
        Node* node = *it;
        if (firstInChain[node->getDfNum()]) {
            assert(node->isBlockNode());
            chains.push_back((BasicBlock*)node);
        }
    }
    std::sort(chains.begin(), chains.end(), 
        chains_comparator(prevInLayoutBySuccessorId, (BasicBlock*)irManager->getFlowGraph()->getEntryNode()));
    assert(*chains.begin() ==irManager->getFlowGraph()->getEntryNode());

    assert(*chains.begin() == irManager->getFlowGraph()->getEntryNode());
    for (U_32 i = 0, n = (U_32)chains.size()-1; i<n;i++) {
        BasicBlock* firstChain = chains[i];
        BasicBlock* secondChain= chains[i+1];
        BasicBlock* lastInFirst = firstChain;
        while (lastInFirst->getLayoutSucc()!=NULL) {
            lastInFirst = lastInFirst->getLayoutSucc();
        }
        lastInFirst->setLayoutSucc(secondChain);
    }
}

}} //namespace
