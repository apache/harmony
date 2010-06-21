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

#include "ControlFlowGraph.h"
#include "LoopTree.h" //required for edge profile smoothing
#include <algorithm>

namespace Jitrino {

//////////////////////////////////////////////////////////////////////////
// ControlFlowGraphFactory methods
Node* ControlFlowGraphFactory::createNode(MemoryManager& mm, Node::Kind kind) {
    return new (mm) Node(mm, kind);
}    

Edge* ControlFlowGraphFactory::createEdge(MemoryManager& mm, Node::Kind srcKind, Node::Kind dstKind) {
    return new (mm) Edge();
}


//////////////////////////////////////////////////////////////////////////
// Edge methods

Edge::Kind Edge::getKind() const {
    Edge::Kind kind = Edge::Kind_Unconditional;
    if (source->isBlockNode()) {
        if (target->isBlockNode()) {
            if (source->getOutDegree() > 1) {
                CFGInst* inst = source->getLastInst();
                if (inst!=NULL) {
                    kind = inst->getEdgeKind(this);
                } else { //empty node with dispatch and unconditional edges. B->B edge is unconditional
#ifdef _DEBUG       //do some checks
                    assert(source->getOutDegree() == 2);
                    const Edges& outEdges = source->getOutEdges();
                    Edge* secondEdge  = outEdges.front() == this ? outEdges.back() : outEdges.front();
                    assert(secondEdge->getTargetNode()->isDispatchNode());
#endif
                    //edge kind stays unconditional
                }
            }
        } else if (target->isDispatchNode()) {
            kind = Edge::Kind_Dispatch;
        } else {
            assert(target->isExitNode());//kind is unconditional
        }
    } else {
        assert(source->isDispatchNode());
        if (target->isDispatchNode() || target->isExitNode()) {
            kind = Edge::Kind_Dispatch;
        } else {
            assert(target->isBlockNode());
            kind = Edge::Kind_Catch;
        }
    }
    return kind;
}

//////////////////////////////////////////////////////////////////////////
// Node methods

class HeadInst : public CFGInst {
public:
    HeadInst(Node* _node) {node = _node;}
    // must never be called on head inst
    virtual Edge::Kind getEdgeKind(const Edge* edge) const {assert(false); return Edge::Kind_Unconditional;}
};

Node::Node(MemoryManager& mm, Kind _kind)
: id(MAX_UINT32), dfNumber(0), preNumber(0), postNumber(0), traversalNumber(0), 
kind(_kind), inEdges(mm), outEdges(mm), instsHead(NULL), execCount(0)
{
    instsHead = new (mm) HeadInst(this);
}

Edge* Node::getOutEdge(Edge::Kind edgeKind) const {
    for (Edges::const_iterator ite = outEdges.begin(), ende = outEdges.end(); ite!=ende; ++ite) {
        Edge* edge = *ite;
        if (edge->getKind() == edgeKind) {
            return edge;
        }
    }
    return NULL;
}


Edge* Node::findEdge(bool isForward, const Node* node) const {
    const Edges& edges = getEdges(isForward);
    for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
        Edge* edge = *ite;
        if (edge->getNode(isForward) == node) {
            return edge;
        }
    }
    return NULL;
}


// appends inst to the end of node list.
void Node::appendInst(CFGInst* newInst, CFGInst* instAfter) {
    if (instAfter==NULL) {
        newInst->insertBefore(instsHead);
    } else {
        assert(instAfter->getNode() == this);
        newInst->insertAfter(instAfter);
    }
    
}

// appends inst to the beginning of node list. Checks "block header critical inst" property
void Node::prependInst(CFGInst* newInst, CFGInst* instBefore) {
    if (instBefore==NULL) {
        instBefore = instsHead;
        for (CFGInst* inst = getFirstInst(); inst!=NULL; inst = inst->next()) {
            if (inst->isLabel()) {
                continue;
            }
            if (newInst->isHeaderCriticalInst() || !inst->isHeaderCriticalInst()) {
                instBefore = inst;
                break;
            }
        }
    }
    assert(instBefore->getNode() == this);
    newInst->insertBefore(instBefore);
}

void Node::insertInst(CFGInst* prev, CFGInst* newInst) {
#ifdef _DEBUG
    assert(newInst->getNode() == NULL);
    if (newInst->isHeaderCriticalInst()) { //check prev inst is also critical
        if (prev != instsHead) { //check only if newInst will not be the first
            CFGInst* prevInst = (CFGInst*)prev;
            assert(prevInst->isHeaderCriticalInst());
            assert(!newInst->isLabel()); //label must be the first
        } 
    } else { //check next inst is not critical
        Dlink* nextLink = prev->getNext();
        if (nextLink!=instsHead) {//check only if newInst will not be the last
            CFGInst* nextInst = (CFGInst*)nextLink;
            assert(!nextInst->isHeaderCriticalInst());
        }
    }
#endif
    ((Dlink*)newInst)->insertAfter(prev);
    newInst->node = this;
}


U_32 Node::getInstCount(bool ignoreLabels) const  {
    U_32 count = 0;
    CFGInst* first = getFirstInst();
    while (ignoreLabels && first!=NULL && first->isLabel()) {
        first = first->next();
    }
    for (CFGInst* inst = first; inst!=NULL; inst = inst->next()) {
        count++;
    }
    return count;
}

Edge* Node::findEdgeTo(bool isForward, Node* node) const {
    const Edges& edges = getEdges(isForward);
    for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
        Edge* edge = *ite;
        if (edge->getNode(isForward) == node) {
            return edge;
        }
    }
    return NULL;

}

uint16 Node::getNodeStartBCOffset() const {
    if (!isBlockNode()) {
        return ILLEGAL_BC_MAPPING_VALUE;
    }
    CFGInst* inst = getFirstInst();
    while (inst!=NULL && inst->getBCOffset()==ILLEGAL_BC_MAPPING_VALUE) {
        inst = inst->next();
    }
    return inst==NULL ? ILLEGAL_BC_MAPPING_VALUE : inst->getBCOffset();
}

uint16 Node::getNodeEndBCOffset() const {
    if (!isBlockNode()) {
        return ILLEGAL_BC_MAPPING_VALUE;
    }
    CFGInst* inst = getLastInst();
    while (inst!=NULL && inst->getBCOffset()==ILLEGAL_BC_MAPPING_VALUE) {
        inst = inst->prev();
    }
    return inst==NULL ? ILLEGAL_BC_MAPPING_VALUE : inst->getBCOffset();
}



//////////////////////////////////////////////////////////////////////////
// CFG methods

ControlFlowGraph::ControlFlowGraph(MemoryManager& _mm, ControlFlowGraphFactory* _factory) 
: mm (_mm), factory(_factory), entryNode (NULL), returnNode(NULL), exitNode(NULL), unwindNode(NULL),
nodes(_mm), postOrderCache(_mm), nodeIDGenerator(0), edgeIDGenerator(0), nodeCount(0), 
traversalNumber(0), lastModifiedTraversalNumber(0), lastOrderingTraversalNumber(0),
lastEdgeRemovalTraversalNumber(0), lastProfileUpdateTraversalNumber(0), currentPreNumber(0), currentPostNumber(0),
annotatedWithEdgeProfile(false), domTree(NULL), postDomTree(NULL), loopTree(NULL)
{
    if (factory == NULL) { //use default factory
        factory = new (mm) ControlFlowGraphFactory();
    }
}

Node* ControlFlowGraph::createNode(Node::Kind kind, CFGInst* inst) {
    Node* node = factory->createNode(mm, kind);
    addNode(node);
    if (inst!=NULL) {
        node->appendInst(inst); 
    }
    return node;
}


void ControlFlowGraph::removeNode(Node* node) {
    removeNode(std::find(nodes.begin(), nodes.end(), node), true);
}

void ControlFlowGraph::removeNode(Nodes::iterator pos, bool erase) {
    Node* node = *pos;
    
	if (node == entryNode) {
		entryNode=NULL;
	}
    if (node == returnNode) {
        returnNode = NULL;
    } else if(node == unwindNode) {
        unwindNode = NULL;
    } else  if (node == exitNode) {
        exitNode = NULL;
    }
    
    // Remove incident edges
    while (!node->inEdges.empty()) {
        Edge* edge = node->inEdges.front();
        assert(edge->getTargetNode() == node);
        removeEdge(edge);
    }
    
    while (!node->outEdges.empty()) {
        Edge* edge = node->outEdges.front();
        assert(edge->getSourceNode() == node);
        removeEdge(edge);
    }

    // Erase the node itself
    if (erase) {
        *pos = *nodes.rbegin();
        nodes.pop_back();
    } else {
        *pos = NULL;
    }

    // Mark the graph modified.
    lastModifiedTraversalNumber = traversalNumber;
}

Edge* ControlFlowGraph::addEdge(Node* source, Node* target, double edgeProb) {
    assert(target!=entryNode);
    assert(source!=exitNode);
    assert (source->findTargetEdge(target) == NULL);
    Edge* edge = factory->createEdge(mm, source->getKind(), target->getKind());
    addEdge(source, target, edge, edgeProb);
    return edge;
}

void ControlFlowGraph::addEdge(Node* source, Node* target, Edge* edge, double edgeProb) {
    // Set ID
    edge->setId(edgeIDGenerator);
    edgeIDGenerator++;

    // Out edge for source node
    edge->source = source;
    source->outEdges.push_back(edge);

    // In edge for target node
    edge->target = target;
    target->inEdges.push_back(edge);

    edge->setEdgeProb(edgeProb);

    // Mark the graph modified.
    lastModifiedTraversalNumber = traversalNumber;
}


void ControlFlowGraph::removeEdge(Edge* edge) {
    // Remove out edge from source node
    Node* source = edge->getSourceNode();
    Edges& outEdges = source->outEdges;
    outEdges.erase(std::remove(outEdges.begin(), outEdges.end(), edge));

    // Remove in edge from target node
    Node* target = edge->getTargetNode();
    Edges& inEdges = target->inEdges;
    inEdges.erase(std::remove(inEdges.begin(), inEdges.end(), edge));

    // Mark the graph modified.
    lastModifiedTraversalNumber = traversalNumber;
    lastEdgeRemovalTraversalNumber = traversalNumber;
}

Edge* ControlFlowGraph::replaceEdgeTarget(Edge* edge, Node* newTarget, bool keepOldBody) {
    Node* source = edge->getSourceNode();
    Node* oldTarget = edge->getTargetNode();
    CFGInst* lastInst = source->getLastInst();

    Edge* newEdge = NULL;
    if (keepOldBody) {
        edge->target = newTarget;
        newTarget->inEdges.push_back(edge);
        Edges& oldInEdges = oldTarget->inEdges;
        oldInEdges.erase(std::remove(oldInEdges.begin(), oldInEdges.end(), edge));
        newEdge = edge;
        // Mark the graph modified.
        lastModifiedTraversalNumber = traversalNumber;
    } else {
        removeEdge(edge);
        newEdge = addEdge(source, newTarget);
    }
    if (lastInst!=NULL) {
        lastInst->updateControlTransferInst(oldTarget, newTarget);
    }
    return newEdge;
}


#define PROFILE_ERROR_ALLOWED   0.000000001
#define ABS(a) (((a) > (0)) ? (a) : -(a))


// Check the profile consistency of the CFG.
// Return true if the profile is consistent. Otherwise, return false.
bool ControlFlowGraph::isEdgeProfileConsistent(bool checkEdgeProbs, bool checkExecCounts, bool doAssertForClient) {
    assert(hasEdgeProfile());
    bool profileIsUptodate= lastProfileUpdateTraversalNumber > getModificationTraversalNum();
#ifndef _DEBUG
    if (profileIsUptodate) {
        return true;
    }
#else
    //do self test in debug version
    bool doAssert = profileIsUptodate || doAssertForClient;
#endif

    const Nodes& postOrdered = getNodesPostOrder();
    for (Nodes::const_iterator it = postOrdered.begin(), end = postOrdered.end(); it!=end; ++it) {
        Node* node = *it;
        if (checkEdgeProbs) {
            const Edges& outEdges = node->getOutEdges();
            if (!outEdges.empty()) {
                double probSum = 0;    
                for (Edges::const_iterator ite = outEdges.begin(), ende = outEdges.end(); ite!=ende; ++ite) {
                    Edge* edge = *ite;
                    double prob = edge->getEdgeProb();
                    if (prob <= 0 || prob > 1) {
                        assert(doAssert?false:true);
                        return false;
                    }
                    probSum+=prob;
                }
                if (ABS(probSum-1) > PROFILE_ERROR_ALLOWED) {
                    assert(doAssert?false:true);
                    return false;
                }
            }
        }
        if (checkExecCounts) {
            const Edges& inEdges = node->getInEdges();
            bool hasInfiniteLoops = false;
            if (!inEdges.empty()) {
                double freqSum = 0;
                for (Edges::const_iterator ite = inEdges.begin(), ende = inEdges.end(); ite!=ende; ++ite) {
                    Edge* edge = *ite;
                    Node* sourceNode = edge->getSourceNode();
                    double freq = edge->getEdgeProb() * sourceNode->getExecCount();
                    if (sourceNode->getTraversalNum()!=node->getTraversalNum()) {
                        freq = 0;
                    }
                    if (freq <=0) {
                        assert(doAssert?false:true);
                        return false;
                    }
                    freqSum+=freq;
                }
                if (freqSum <=0 ) {
                    assert(doAssert?false:true);
                    return false;
                }
                double nodeFreq = node->getExecCount();
                if (nodeFreq > 1/PROFILE_ERROR_ALLOWED) {
                    hasInfiniteLoops = true;
                } else if (ABS(nodeFreq / freqSum - 1) >= PROFILE_ERROR_ALLOWED) {
                    assert(doAssert?false:true);
                    return false;
                }
            }
            if (!hasInfiniteLoops && exitNode!=NULL) {
                double exitFreq = exitNode->getExecCount();
                double entryFreq = entryNode->getExecCount();
                bool ok = ABS(exitFreq - entryFreq) < PROFILE_ERROR_ALLOWED * entryFreq;
                if (!ok) {
                    assert(doAssert ? false : true);
                    return false;
                }
            }
        }
    }
    if (!profileIsUptodate && checkEdgeProbs && checkExecCounts) {
        lastProfileUpdateTraversalNumber = getTraversalNum();
    }
    return true;
}   

// this utility splits a node at a particular instruction, leaving the instruction in the
// same node and moving all insts (before inst : splitAfter=false/after inst : splitAfter=true 
// to the newly created node
// returns the new node created
Node* ControlFlowGraph::splitNodeAtInstruction(CFGInst *inst, bool splitAfter, bool keepDispatch, CFGInst* labelInst) {
    Node *node = inst->getNode();
    Node* dispatchNode = keepDispatch ? node->getExceptionEdgeTarget() : NULL;
    Node* newNode =  splitNode(node, true, labelInst);
    if (inst == node->getLastInst()) {
        assert(!splitAfter || (node->getOutDegree() == 1 && node->getUnconditionalEdge()!=NULL)
            || (node->getOutDegree()==2 && node->getExceptionEdgeTarget()!=NULL));
    }  
    
    //keeping dispatch node is useful for extended basis blocks
    if (keepDispatch && dispatchNode!=NULL) {
        assert(node->getExceptionEdgeTarget() == NULL);
        addEdge(node, dispatchNode, 0);
    }

    // now move the instructions
    for (CFGInst *ins = splitAfter ? inst->next() : inst; ins != NULL; ) {
        CFGInst *nextins = ins->next();
        ins->unlink();
        newNode->appendInst(ins);
        ins = nextins;
    } 
    newNode->setExecCount(node->getExecCount());
    node->findTargetEdge(newNode)->setEdgeProb(1.0);
    return newNode;
}

// Splice a new empty block on the CFG edge.
Node* ControlFlowGraph::spliceBlockOnEdge(Edge* edge, CFGInst* inst, bool keepOldEdge) {
    Node*    source = edge->getSourceNode();
    double edgeProb = edge->getEdgeProb();
    double edgeFreq = source->getExecCount()*edgeProb;

    // Split the edge
    Node* split =  splitEdge(edge, inst, keepOldEdge);
    split->setExecCount(edgeFreq);

    // Set the incoming edge probability
    assert(split->getInDegree() == 1);
    split->getInEdges().front()->setEdgeProb(edgeProb);

    // Set the outgoing edge probability
    assert(split->getOutDegree() == 1);
    split->getOutEdges().front()->setEdgeProb(1.0);

    return split;
}


void ControlFlowGraph::splitCriticalEdges(bool includeExceptionEdges, Nodes* newNodes) {
    // go backwards through collection so appending won't hurt us.
    Nodes::reverse_iterator iter = nodes.rbegin(), end = nodes.rend();
    for (int idx = (int)nodes.size(); --idx<=0;) {
        Node* target = nodes[idx];
        // exception edges go to a dispatch node.
        // only look at nodes with multiple in edges.
        if (target->isDispatchNode() || includeExceptionEdges) {
restart:
            if (target->getInDegree() > 1) {
                Edges& inEdges = target->inEdges;
                Edges::iterator iterEdge = inEdges.begin(), endEdge = inEdges.end();
                for (; iterEdge != endEdge; ++iterEdge) {
                    Edge* thisEdge = *iterEdge;
                    Node* source = thisEdge->getSourceNode();
                    if ((source->getOutDegree() > 1) && (includeExceptionEdges || !source->isDispatchNode())) {
                        // it's a critical edge, split it
                        Node* newNode = splitEdge(thisEdge, NULL, false); //TODO: keep old edge?
                        if (newNodes) {
                            newNodes->push_back(newNode);
                        }
                        goto restart;
                    }
                }
            }
        }
    }        
}


bool ControlFlowGraph::isBlockMergeAllowed(Node* source, Node* target, bool allowMergeDispatch) const {
    assert(source->isBlockNode() && target->isBlockNode());
    if (target == returnNode) {
        return false;
    }
    if (source->getUnconditionalEdgeTarget()!=target) {
        return false;
    }
    if (target->getInDegree()!=1 || source->getOutDegree() > 2) {
        return false;
    }

    Node* sourceDispatch = source->getExceptionEdgeTarget();
    Node* targetDispatch = target->getExceptionEdgeTarget();

    //check if there is no problem with header critical insts
    if (source->getLastInst()!=NULL && !source->getLastInst()->isHeaderCriticalInst()) {
        //we can't allow target to have critical insts except label
        CFGInst* inst = target->getFirstInst();
        inst = inst!=NULL?(inst->isLabel() ? inst->next() : inst) : NULL;
        if (inst!=NULL && inst->isHeaderCriticalInst()) {
            return false;
        }
    }

    if (source->getOutDegree() == 2 && sourceDispatch == 0) {
        return false; // 2 uncond edges, e.g. indirect jumps
    }

    if (!allowMergeDispatch) {
        return sourceDispatch==NULL && targetDispatch==NULL;
    }
    
    bool allowMergeByDispatch = sourceDispatch == targetDispatch 
        || (sourceDispatch == unwindNode && targetDispatch == NULL) 
        || (sourceDispatch == NULL && targetDispatch == unwindNode);

    return allowMergeByDispatch;
}


void ControlFlowGraph::mergeBlocks(Node* source, Node* target, bool keepFirst) {
    assert(isBlockMergeAllowed(source, target, true));

    Edge* edge = target->inEdges.front();
    assert(edge->getSourceNode() == source);
    removeEdge(edge);

    if(keepFirst) {
        // Merge second block into first
        moveInstructions(target, source, false);
        moveOutEdges(target, source);
        removeNode(target);
    } else {
        // Merge first block into second
        moveInstructions(source, target, true);
        moveInEdges(source, target);
        removeNode(source);
    }
}

void ControlFlowGraph::mergeAdjacentNodes(bool skipEntry, bool mergeByDispatch) {
    for(U_32 idx = 0; idx < nodes.size(); idx++) {
        Node* node = nodes[idx];
        if (node->isBlockNode() && (!skipEntry || node!=entryNode)) { 
            Node* succ = node->getUnconditionalEdgeTarget();
            if (succ!=NULL && succ->getInDegree() == 1 && !succ->isExitNode()) {
                if (isBlockMergeAllowed(node, succ, mergeByDispatch)){ 
                    mergeBlocks(node, succ);
                    idx--;
                }
            }
        } else if (node->isDispatchNode()) {
            if (node->getOutDegree() == 1 && node->isEmpty()) {
                Node* succ = node->getOutEdges().front()->getTargetNode();
                if (succ->isDispatchNode())  {
                    // This dispatch has no handlers.
                    // Forward edges to this dispatch node to the successor.
                    moveInstructions(node, succ, true);
                    moveInEdges(node, succ);
                    removeNode(nodes.begin() + idx, true);
                    idx--;
                }
            }
        }
    }
}


void ControlFlowGraph::purgeEmptyNodes(bool preserveCriticalEdges, bool removeEmptyCatchBlocks) {
    for(Nodes::iterator it = nodes.begin(),end = nodes.end(); it != end; ++it) {
        Node* node = *it;
        if(node->isBlockNode() && node->isEmpty()) {
            // Preserve entry node
            if(node == getEntryNode() || node == getReturnNode() || (!removeEmptyCatchBlocks  && node->isCatchBlock())) {
                continue;
            }
            // Node must have one unconditional successor if it is empty.
            assert(node->getOutDegree() == 1 || (node->getOutDegree()==2 && node->getExceptionEdge()!=NULL));
            Node* succ = node->getUnconditionalEdgeTarget();
            assert(succ->isBlockNode() || (succ->isExitNode() && getReturnNode() == NULL)); //cfg without special support for return node...
            if(succ == node) {
                // This is an empty infinite loop.
                continue;
            }

            // Preserve this node if it breaks a critical edge if requested.
            bool preserve = false;
            if(preserveCriticalEdges && succ->getInDegree() > 1) {
                // Preserve this block if any predecessors have multiple out edges.
                Edges::const_iterator eiter;
                for(eiter = node->getInEdges().begin(); !preserve && eiter != node->getInEdges().end(); ++eiter) {
                    if((*eiter)->getSourceNode()->getOutDegree() > 1)
                        // This node breaks a critical edge.
                        preserve = true;
                }
            }

            if(!preserve) {
                moveInEdges(node, succ);
                removeNode(it, false);
            }
        }
    }
    nodes.erase(std::remove(nodes.begin(), nodes.end(), (Node*)NULL), nodes.end());
}

void ControlFlowGraph::addNode(Node* node) {
    // Set ID
    node->setId(nodeIDGenerator);
    nodeIDGenerator++;

    // Add node
    nodes.push_back(node);

    // Reset node traversal number
    node->traversalNumber = 0;

    // Mark the graph modified.
    lastModifiedTraversalNumber = traversalNumber;
}


// Splice inlineFG into this CFG.
void ControlFlowGraph::spliceFlowGraphInline(Edge* edge, ControlFlowGraph& inlineFG) {
    assert(!edge->isDispatchEdge() && !edge->isCatchEdge());

    Node* nodeBeforeEntry = edge->getSourceNode();
    Node* nodeAfterReturn = edge->getTargetNode();
    Node* dispatchNode = nodeBeforeEntry->getExceptionEdgeTarget();
    
    Node* inlineEntryNode = inlineFG.getEntryNode();
    Node* inlineReturnNode = inlineFG.getReturnNode();
    Node* inlineUnwindNode = inlineFG.getUnwindNode();
    Node* inlineExitNode = inlineFG.getExitNode();

    bool returnSeen = false;
    bool unwindSeen = false;

   
    // Add nodes to CFG
    assert(&getMemoryManager() == &inlineFG.getMemoryManager());
    const Nodes& inlineNodes = inlineFG.getNodes();
    for(Nodes::const_iterator i = inlineNodes.begin(), end = inlineNodes.end(); i != end; ++i) {
        Node* n = *i;
        if (n != inlineExitNode) {
            addNode(n);
            const Edges& oEdges = n->getOutEdges();
            Edges::const_iterator e;
            for(e = oEdges.begin(); e != oEdges.end(); ++e) {
                setNewEdgeId(*e);
            }
            if (n == inlineReturnNode) {
                returnSeen = true;
            } else if (n == inlineUnwindNode) {
                unwindSeen = true;
            }
        }

    }
    
    // Connect nodes.
    replaceEdgeTarget(edge, inlineEntryNode);
    if(returnSeen) { 
        removeEdge(inlineReturnNode, inlineExitNode);
        addEdge(inlineReturnNode, nodeAfterReturn, 1); 
    }
    if(unwindSeen) {
        removeEdge(inlineUnwindNode, inlineExitNode);
        if (dispatchNode == NULL) {
            dispatchNode = getUnwindNode();
        }
        assert(dispatchNode != NULL);
        addEdge(inlineUnwindNode, dispatchNode, 1);
    }
}


void ControlFlowGraph::spliceFlowGraphInline(CFGInst* instAfter, ControlFlowGraph& inlineFG) {
    Node* node = instAfter->getNode();
    Edge* edgeToInlined = NULL;
    if (node->getLastInst() == instAfter) {
        edgeToInlined =  node->getUnconditionalEdge();
    } else {//WARN: no label inst created -> possible problems if method is used in HLO
        Node* newBlock = splitNodeAtInstruction(instAfter, true, false, NULL);
        assert(newBlock->getInDegree() == 1);
        edgeToInlined = newBlock->getInEdges().front();
    }
    assert(edgeToInlined!=NULL);
    spliceFlowGraphInline(edgeToInlined, inlineFG);
 }


void ControlFlowGraph::moveInEdges(Node* oldNode, Node* newNode) {
    assert(oldNode != newNode);
    assert(oldNode->getKind() == newNode->getKind() || (getReturnNode()==NULL && newNode->isExitNode()));
    Edges& oldIns = oldNode->inEdges;
    Edges& newIns = newNode->inEdges;
    newIns.insert(newIns.begin(), oldIns.begin(), oldIns.end());
    oldIns.clear();
    // Reset edge pointers and update branch instructions.
    resetInEdges(newNode);
    assert(oldNode->getInDegree() == 0);

    // Mark the graph modified.
    lastModifiedTraversalNumber = traversalNumber;
}

void
ControlFlowGraph::moveOutEdges(Node* oldNode, Node* newNode) {
    assert(oldNode->getKind() == newNode->getKind());
    Edges& oldOuts = oldNode->outEdges;
    Edges& newOuts = newNode->outEdges;
    newOuts.insert(newOuts.end(), oldOuts.begin(), oldOuts.end());
    oldOuts.clear();
    resetOutEdges(newNode);
    assert(oldNode->getOutDegree() == 0);

    // Mark the graph modified.
    lastModifiedTraversalNumber = traversalNumber;
}


void ControlFlowGraph::resetInEdges(Node* node) {
    Edges& edges = node->inEdges;
    for(int i=(int)edges.size(); --i>=0;) {
        Edge* edge = edges[i];
        // The old successor; node is the new successor
        Node* old = edge->getTargetNode();
        if(old != node) {
            Node* pred = edge->getSourceNode();
            // Check if another edge already goes to node.  .
            bool redundant = (pred->findTargetEdge(node) != NULL);
            // Set this edge to point to node.  Note, we do this even if the
            // edge is to be deleted.  removeEdge only works if the edge
            // is well-formed.
            edge->target = node;
            // Update control transfer instruction in source.
            CFGInst* lastInst = pred->getLastInst();
            if(pred->isBlockNode() && lastInst!=NULL) {
                lastInst->updateControlTransferInst(old, node);
            }
            if(redundant) {
                // Edge is redundant - eliminate.
                if(pred->getOutDegree() == 2 || (pred->getOutDegree()==3 && pred->getExceptionEdge()!=NULL)) {
                    // Pred already has edge to node and succ, and no other successors.  
                    // We can eliminate the branch and the edge to succ.
                    // Another case is branch+exception for extended form
                    if(pred->isBlockNode()) {
                        lastInst->removeRedundantBranch();
                    }
                }
                removeEdge(edge);
            }
        }
    }
}

void ControlFlowGraph::resetOutEdges(Node* node) {
    Edges& edges = node->outEdges;
    for(int i=(int)edges.size(); --i>=0;) {
        Edge* edge = edges[i];
        // The old predecessor; node is the new predecessor
        Node* old = edge->getSourceNode();
        if(old != node) {
            Node* succ = edge->getTargetNode();
            // Check if another edge already comes from node.
            bool redundant = (succ->findSourceEdge(node) != NULL);
            // Set this edge to point to node.  Note, we do this even if the
            // edge is to be deleted.  removeEdge only works if the edge
            // is well-formed.
            edge->source = node;
            if(redundant) {
                // Edge is redundant - eliminate.
                removeEdge(edge);
            }
        }
    }
}

Node* ControlFlowGraph::splitNode(Node* node, bool newBlockAtEnd, CFGInst* inst) {
    assert(node->isBlockNode());
    Node* newNode = createBlockNode(inst);
    if(newBlockAtEnd) {
        // move out edges
        moveOutEdges(node, newNode);
        addEdge(node, newNode);
    } else { // new block at beginning
        // move in edges
        moveInEdges(node, newNode);
        addEdge(newNode, node);
    }
    return newNode;
}

Node* ControlFlowGraph::splitEdge(Edge *edge, CFGInst* inst, bool keepOldEdge) {
    Node* target = edge->getTargetNode();
    Node* newNode = createBlockNode(inst);
    if (inst!=NULL && target->getFirstInst()!=NULL) {
        inst->setBCOffset(target->getFirstInst()->getBCOffset());
    }

    replaceEdgeTarget(edge, newNode, keepOldEdge);
    addEdge(newNode, target);

    return newNode;
}

void ControlFlowGraph::moveInstructions(Node* fromNode, Node* toNode, bool prepend) {
    CFGInst* to = prepend ? toNode->getFirstInst() : toNode->getLastInst();
    CFGInst *inst = fromNode->getFirstInst();
    if (inst!=NULL && inst->isLabel()) { //skip block label if exists
        inst = inst->next();
    }
    for (; inst != NULL;  ) {
        CFGInst *next = inst->next();
        inst->unlink();
        if (to == NULL) {
            if (prepend) {
                toNode->prependInst(inst);
            } else {
                toNode->appendInst(inst);
            }
        } else {
            inst->insertAfter(to);
        }
        to = inst;
        assert(prepend || to == toNode->getLastInst());
        inst = next;
    }
}

class CountFreqsAlgorithm {
public:
    CountFreqsAlgorithm(MemoryManager& _mm, ControlFlowGraph* _fg)  :
        mm(_mm), fg(_fg), loopTree(fg->getLoopTree()), 
        useCyclicFreqs(false), cyclicFreqs(NULL), exitEdges(mm)
    {
        if (!loopTree->isValid()) {
            loopTree->rebuild(false);
        }
        U_32 nNodes = fg->getNodeCount();
        cyclicFreqs = new (mm) double[nNodes];
        std::fill(cyclicFreqs, cyclicFreqs + nNodes, 1);
    }

    void estimate() {
        smoothLinearFrequencies();
        LoopNode* topLevelLoops = (LoopNode*)loopTree->getRoot();
        if (topLevelLoops->getChild()!=NULL) { //if there are loops in method
            useCyclicFreqs = true;
            for (LoopNode* loopHead = topLevelLoops->getChild(); loopHead!=NULL; loopHead = loopHead->getSiblings()) {
                estimateCyclicFrequencies(loopHead);
            }
            smoothLinearFrequencies();
        }
    }

    
    void smoothLinearFrequencies() {
        const Nodes& nodes = fg->getNodesPostOrder();
        for (Nodes::const_reverse_iterator it = nodes.rbegin(), end = nodes.rend(); it!=end; ++it) {
            Node* node = *it;
            const Edges& inEdges = node->getInEdges();
            if (inEdges.empty()) { 
                //node is entry, do nothing
                assert (node == fg->getEntryNode());
                continue;
            }
            double freq = 0.0;
            for(Edges::const_iterator it = inEdges.begin(), itEnd = inEdges.end(); it!=itEnd; it++) {
                Edge* edge = *it;
                if (loopTree->isBackEdge(edge)) { 
                    continue; //only linear freq estimation
                }
                Node* fromNode = edge->getSourceNode();
                if (fromNode->getTraversalNum()!=node->getTraversalNum()) {
                    continue;//unreachable source node
                }
                double fromFreq = fromNode->getExecCount();
                freq += fromFreq * edge->getEdgeProb();
            }
            if (useCyclicFreqs && loopTree->isLoopHeader(node)) {
                freq *= cyclicFreqs[node->getDfNum()];
            }
            node->setExecCount(freq);
        }
    }
    
    void estimateCyclicFrequencies(LoopNode* loopHead) {
        //process all child loops first
        bool hasChildLoop = loopHead->getChild()!=NULL;
        if (hasChildLoop) {
            for (LoopNode* childHead = loopHead->getChild(); childHead!=NULL; childHead = childHead->getSiblings()) {
                estimateCyclicFrequencies(childHead);
            }
        }
        findLoopExits(loopHead);
        if (hasChildLoop) {
            smoothLinearFrequencies();
        }
        Node* cfgLoopHead = loopHead->getHeader();
        double inFlow = cfgLoopHead->getExecCount(); //node has linear freq here
        double exitsFlow = 0;
        //sum all exits flow
        for (Edges::const_iterator it = exitEdges.begin(), end = exitEdges.end(); it!=end; ++it) {
            Edge* edge = *it;
            Node* fromNode = edge->getSourceNode();
            exitsFlow += edge->getEdgeProb() * fromNode->getExecCount();
        }
        // if loop will make multiple iteration exitsFlow becomes equals to inFlow
        double loopCycles = inFlow / exitsFlow;
        if (loopCycles < 1) {
            assert(ABS(inFlow-exitsFlow) < PROFILE_ERROR_ALLOWED);
            loopCycles = 1;
        }
        cyclicFreqs[cfgLoopHead->getDfNum()] = loopCycles;
    }

    void findLoopExits(LoopNode* loop) {
        exitEdges.clear();
        const Nodes& nodes = loop->getNodesInLoop();
        for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
            Node* node = *it;
            const Edges& outEdges = node->getOutEdges();
            for(Edges::const_iterator eit = outEdges.begin(), eend = outEdges.end(); eit!=eend; ++eit) {
                Edge* edge = *eit;
                if (loop->isLoopExit(edge)) {
                    exitEdges.push_back(edge);
                }
            }
        }
    }
private:
    MemoryManager& mm;
    ControlFlowGraph* fg;
    LoopTree* loopTree;
    bool useCyclicFreqs;
    double* cyclicFreqs;
    Edges exitEdges;
};

//count node exec count from probs
void ControlFlowGraph::smoothEdgeProfile() {
    assert(isEdgeProfileConsistent(true, false, true));
    if (isEdgeProfileConsistent(true, true)) {
        return;
    }
    assert(entryNode->getExecCount()!=0);
    CountFreqsAlgorithm c(mm, this);
    c.estimate();
    lastProfileUpdateTraversalNumber = traversalNumber;
    assert(isEdgeProfileConsistent(true, true, true));
}

//////////////////////////////////////////////////////////////////////////
/// CFGInst methods

CFGInst* CFGInst::next() const {
    CFGInst* next = (CFGInst*)getNext();
    return  (node!=NULL && next == node->instsHead) ? NULL : next;
}

CFGInst* CFGInst::prev() const {
    CFGInst* prev = (CFGInst*)getPrev(); 
    return (node!=NULL && prev == node->instsHead) ? NULL : prev;
}

void CFGInst::insertBefore(CFGInst* inst) {
    assert(node == NULL);
    CFGInst* nextInst = next();
    unlink();
    Node* newNode = inst->getNode();
    if (newNode!= NULL) {
        newNode->insertInst((CFGInst*)inst->getPrev(), this);
    } else {
        ((Dlink*)this)->insertBefore(inst);
    }
    if (nextInst!=this) {
        nextInst->insertBefore(inst);
    }
}

void CFGInst::insertAfter(CFGInst* inst) { 
    assert(node == NULL);
    CFGInst* nextInst = next();
    unlink();
    Node* newNode = inst->getNode();
    if (newNode!=NULL) {
        newNode->insertInst(inst, this);
    } else {
        ((Dlink*)this)->insertAfter(inst);
    }
    if (nextInst!=this) {
        nextInst->insertAfter(this);
    }
}

} //namespace
