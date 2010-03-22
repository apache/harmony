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
*
*/

#include "LoopTree.h"
#include "Dominator.h"

namespace Jitrino {

LoopTree::LoopTree(MemoryManager& _mm, ControlFlowGraph* f, EdgeCoalescerCallback* _coalesceCallback) 
: mm(_mm), fg(f), traversalNum(0), headerMap(_mm), headers(_mm), coalesceCallback(_coalesceCallback), normalized(false)
{
    // create a root loop containing all loops within the method
    root = new (mm) LoopNode(mm, this, NULL); 
}

// 
// we find loop headers and then find loop edges because coalesceLoops 
// introduces new blocks and edges into the flow graph.  dom does not
// dominator information for the newly created blocks.
//
void LoopTree::findLoopHeaders(Nodes& headers) { // out
    DominatorTree* dom = fg->getDominatorTree();
    const Nodes& nodes = fg->getNodes();
    Nodes::const_iterator niter;
    for(niter = nodes.begin(); niter != nodes.end(); ++niter) {
        Node* n = *niter;
        // if n dominates its predecessor, then the edge is an loop back edge
        const Edges& edges = n->getInEdges();
        Edges::const_iterator eiter;
        for(eiter = edges.begin(); eiter != edges.end(); ++eiter) {
            Node* pred = (*eiter)->getSourceNode();
            if (dom->dominates(n, pred)) {
                assert(n->isBlockNode());
                headers.push_back(n);
                break;
            }
        }
    }
}

//
// check each header to see if multiple loops share the head
// if found, coalesce those loop edges.
void LoopTree::findAndCoalesceLoopEdges(const Nodes& headers) { 
    DominatorTree* dom = fg->getDominatorTree();
    assert(dom->isValid());
    MemoryManager mm("LoopTree::findAndCoalesceEdges");
    Edges entryEdges(mm);
    Edges loopEdges(mm);
    for (Nodes::const_iterator it = headers.begin(), end = headers.end(); it!=end; ++it) {
        Node* head = *it;
        entryEdges.clear();
        loopEdges.clear();
        
        // if n dominates its predecessor, then the edge is an loop back edge
        const Edges& inEdges = head->getInEdges();
        for(Edges::const_iterator eiter = inEdges.begin(); eiter != inEdges.end(); ++eiter) {
            Node* pred = (*eiter)->getSourceNode();
            if (dom->dominates(head, pred)) {
                loopEdges.push_back(*eiter);
            } else {
                entryEdges.push_back(*eiter);
            }
        }
        assert(!entryEdges.empty() && !loopEdges.empty()); 
    
        if (head->isBlockNode()) {
            // Create unique preheader if 
            // a) more than one entry edge or
            // b) the one entry edge is a critical edge 
            if ((entryEdges.size() > 1) || (entryEdges.front()->getSourceNode()->getOutDegree() > 1)) {
                coalesceEdges(entryEdges);
            }

            // Create unique tail if
            // a) more than one back edge or
            // b) the one back edge is a critical edge
            if ((loopEdges.size() > 1) ||  loopEdges.front()->getSourceNode()->getOutDegree() > 1) {
                Edge* backEdge = coalesceEdges(loopEdges);
                loopEdges.push_back(backEdge);
            }
        }
    }
}


//
// header map is ready to use for parent loops
//
LoopNode* LoopTree::findEnclosingLoop(Node* header) {
    LoopNode* loop = headerMap[header->getDfNum()];
    return loop != NULL ? loop : (LoopNode*)root;
}


class CompareDFN {
public:
    bool operator() (Node* h1, Node* h2) { return h1->getDfNum() < h2->getDfNum(); }
};

//
// If loop A is contained in loop B, then B's header must dominate 
// A's header.  We sort loop edges based on headers' dfn so as to
// construct loops from the outermost to the innermost
//
void LoopTree::formLoopHierarchy(Nodes& headers) {

    // sort headers based on their depth first number
    std::sort(headers.begin(), headers.end(), CompareDFN());

    assert(fg->hasValidOrdering()); 

    // create loops in df num order
    for (Nodes::const_iterator it = headers.begin(), end = headers.end(); it!=end; ++it) {
        Node* header = *it;
        createLoop(header);
    }
}

void LoopNode::clear() {
     assert(this == loopTree->root);
     child = NULL;
}

void LoopNode::backwardMarkNode(Node* node, DominatorTree* dom) {
    if (node->getTraversalNum() > loopTree->fg->getTraversalNum()) {
        return;
    }

    assert(node->getTraversalNum() == loopTree->fg->getTraversalNum());
    loopTree->headerMap[node->getDfNum()] = this;
    nodesInLoop.push_back(node);
    node->setTraversalNum(node->getTraversalNum()+1);

    const Edges& inEdges = node->getInEdges();
    for(Edges::const_iterator eiter = inEdges.begin(); eiter != inEdges.end(); ++eiter) {
        Edge* e = *eiter;
        Node* srcNode = e->getSourceNode();
        if (dom->dominates(header, srcNode)) {
            backwardMarkNode(srcNode, dom);
        }
    }
}

// 
// traverse graph backward starting from tail until header is reached
//
void LoopNode::markNodesOfLoop() {
    DominatorTree* dom = loopTree->fg->getDominatorTree();
    assert(dom->isValid());

    assert(header->getTraversalNum() == loopTree->fg->getTraversalNum()); 
    backwardMarkNode(header, dom);
    
    //restore traversal nums
    U_32 fgTraversalNum = loopTree->fg->getTraversalNum();
    for (Nodes::const_iterator it = nodesInLoop.begin(), end = nodesInLoop.end(); it!=end; ++it) {
        Node* node = *it;
        assert(node->getTraversalNum() == fgTraversalNum + 1);
        node->setTraversalNum(fgTraversalNum);
    }
}


void LoopTree::createLoop(Node* header) {
    // walk up the hierarchy to find which loop contains it
    LoopNode* loop = new (mm) LoopNode(mm, this, header);
    LoopNode* parent = findEnclosingLoop(header);
    parent->addChild(loop);
    loop->markNodesOfLoop();
}



// create a new <block> that sinks all edges 
// create a new edge <block>-><header>.
// return the newly created block.
//
Edge* LoopTree::coalesceEdges(Edges& edges) {
    Node* header = edges.back()->getTargetNode();

    // create a new block and re-target all loop edges to the block
    Node* block = fg->createNode(header->getKind());
    
    if (coalesceCallback!=NULL) {
        coalesceCallback->coalesce(header, block, (U_32)edges.size());
    }
    //retarget all edges
    for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
        Edge* e = *ite;
        assert(e->getTargetNode() == header);  // must share the same header

        fg->replaceEdgeTarget(e, block, true);
        block->setExecCount(block->getExecCount()+e->getSourceNode()->getExecCount()*e->getEdgeProb());
    }

    // create factored edge <block,header>
    Edge* edge = fg->addEdge(block,header, 1.0);
    return edge;
}



void LoopTree::rebuild(bool doNormalization, bool ignoreUnreachable) {
    if (isValid() && (!doNormalization || normalized)) {
        return;
    }
    assert(!(ignoreUnreachable && doNormalization)); //incompatible modes.

    normalized = false;

    DominatorTree* dom = fg->getDominatorTree();
    if ( dom==NULL || !dom->isValid()) {
        DominatorBuilder db;
        dom = db.computeDominators(mm, fg, false, ignoreUnreachable);
        fg->setDominatorTree(dom);
    }

    headers.clear();
    findLoopHeaders(headers);

    if (doNormalization) {
        findAndCoalesceLoopEdges(headers);
        normalized = true;
        if (!dom->isValid()) {
            DominatorBuilder db;
            dom = db.computeDominators(fg->getMemoryManager(), fg);
            fg->setDominatorTree(dom);
        }
    }

    if (isValid()) {
        return; // double check after normalization -> if already normalized and valid -> return
    }

    //cleanup
    headerMap.clear();
    ((LoopNode*)root)->clear();

    U_32 numBlocks = fg->getNodeCount();
    headerMap.resize(numBlocks); 

    formLoopHierarchy(headers);

    computeOrder(); // compute pre/post nums for loop nodes

    traversalNum = fg->getTraversalNum();
}

bool LoopTree::isLoopHeader(const Node* node) const {
    assert(isValid()); 
    LoopNode* loop = headerMap[node->getDfNum()];
    return loop!=NULL && loop->header == node;
}


LoopNode* LoopTree::getLoopNode(const Node* node, bool containingLoop) const {
    assert(isValid());
    U_32 df = node->getDfNum();
    if (df >= headerMap.size()) {
        return NULL; //invalid DF can be caused by unreachable node
    }
    LoopNode* loop = headerMap[df];
    if (loop == NULL) {
        return NULL;
    }
    LoopNode* result = containingLoop && node == loop->getHeader() ? (LoopNode*)loop->getParent() : loop;
    return result;
}


Node* LoopTree::getLoopHeader(const Node* node, bool containingLoop) const {
    LoopNode* headerLoop = getLoopNode(node, containingLoop);
    Node* header = headerLoop == NULL ? NULL : headerLoop->getHeader();
    return header;
}

U_32 LoopTree::getLoopDepth(const Node* node) const {
    LoopNode* header = getLoopNode(node, false);
    return header == 0 ? 0 : header->getDepth();
}

bool LoopTree::isLoopExit(const Edge* edge) const {
    LoopNode* srcLoop = getLoopNode(edge->getSourceNode(), false);
    if (srcLoop == NULL) { //source not in loop
        return false;
    }
    LoopNode* dstLoop = getLoopNode(edge->getTargetNode(), false);
    return dstLoop == NULL || (dstLoop!=srcLoop && dstLoop->getDepth() <= srcLoop->getDepth());
}

bool LoopTree::isBackEdge(const Edge* edge) const {
    assert(isValid());
    return fg->getDominatorTree()->dominates(edge->getTargetNode(), edge->getSourceNode());
}


bool LoopNode::inLoop(const Node* node) const  {
    assert(this!=loopTree->getRoot());
    assert(loopTree->isValid()); 
    LoopNode* nodeLoop = loopTree->headerMap[node->getDfNum()];
    bool result = nodeLoop!=NULL && (nodeLoop == this || loopTree->isAncestor(this, nodeLoop));
    return result;
}

Node* LoopNode::getHeader() const { 
    assert(loopTree->isValid());
    return header; 
}

const Nodes& LoopNode::getNodesInLoop() const {
    assert(loopTree->isValid()); 
    return nodesInLoop;
}

bool LoopNode::isLoopExit(const Edge* e) const {
    return inLoop(e->getSourceNode()) && !inLoop(e->getTargetNode());
}

bool LoopNode::isBackEdge(const Edge* e) const {
    assert(loopTree->isValid());
    return e->getTargetNode() == header && loopTree->isBackEdge(e);
}

}//namespace
