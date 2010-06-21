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

#include "Dominator.h"
#include <string.h>

namespace Jitrino {

// BOTTOM : no idom.  idom[entry] = BOTTOM
#define NONE MAX_UINT32
// TOP : no information computed yet
#define UNDEFINED 0 

// Cooper / Harvey / Kennedy variation on simple iterative dominator algorithm.
// They claim that this is faster than Lengauer / Tarjan in practice.  
DominatorTree*
DominatorBuilder::computeDominators(MemoryManager& mm, ControlFlowGraph* flowgraph, bool isPost, bool ignoreUnreach) {
    // Temp memory manager
    MemoryManager tmm("DominatorBuilder::computeDominators");
    Nodes nodes(tmm);

    // Get the nodes of the flowgraph in postorder.
    flowgraph->getNodesPostOrder(nodes, !isPost);
    U_32 numNodes = (U_32) nodes.size();
#ifdef _DEBUG
    // Start (entry/exit) node should be last.
    Node* start = nodes.back();
    assert((start == (isPost ? flowgraph->getExitNode() : flowgraph->getEntryNode()) 
            && start->getPostNum() == numNodes-1));
#endif         
    // Initialize the idom array to UNDEFINED.  Idiom maps the postorder
    // number of a node to the postorder number of its idom.
    StlVector<U_32> idom(tmm);
    idom.insert(idom.end(), (unsigned int) numNodes-1, UNDEFINED);
    // Initialize idom[entry] = NONE.
    idom.insert(idom.end(), NONE);

    // Find maximal fixed point
    bool changed = true;
    while(changed) {
        changed = false;
        // Process all nodes except entry in reverse postorder.
        Nodes::reverse_iterator i;
        for(i = nodes.rbegin()+1; i != nodes.rend(); ++i) {
            Node* node = *i;
            U_32 nodeNum = node->getPostNum();

            // Compute updated idom from predecessors
            U_32 newIdom = UNDEFINED;
            Edges::const_iterator j;
            for(j = node->getEdges(isPost).begin(); j != node->getEdges(isPost).end(); ++j) {
                Node* pred = (*j)->getNode(isPost);
                // Assert that pred is reachable.
                if (!ignoreUnreach)
                    assert(pred->getTraversalNum() == flowgraph->getTraversalNum());
                if (!ignoreUnreach || (ignoreUnreach && (pred->getTraversalNum() == flowgraph->getTraversalNum()))) {
                    U_32 predNum = pred->getPostNum();
                    // Skip unprocessed preds (only happens on first iteration).
                    if(idom[predNum] != UNDEFINED)
                        // Intersect dominator sets.
                        newIdom = (newIdom == UNDEFINED) ? predNum : intersect(idom, newIdom, predNum);
                } 
            }
            assert(newIdom != UNDEFINED);

            // Update if changed
            if(newIdom != idom[nodeNum]) {
                // Assert that we are converging.
                assert(newIdom > idom[nodeNum]);
                idom[nodeNum] = newIdom;
                changed = true;
            }
        }
    }

    // Build dominator tree and return.
    return new (mm) DominatorTree(mm, flowgraph, nodes, idom, isPost);
}

U_32 
DominatorBuilder::intersect(StlVector<U_32>& idom, U_32 finger1, U_32 finger2) {
    // Intersect the dominator sets represented by finger1 and finger2.
    while(finger1 != finger2) {
        while(finger1 < finger2)
            finger1 = idom[finger1];
        while(finger2 < finger1)
            finger2 = idom[finger2];
    }
    // Set should at least contain root/entry.
    assert(finger1 != NONE);
    return finger1;
}

DominatorTree::DominatorTree(MemoryManager& mm,
                             ControlFlowGraph* fg,
                             Nodes& nodes,
                             StlVector<U_32>& idom,
                             bool isPostDominator)     
                             : flowgraph(fg), traversalNum(fg->getTraversalNum()), 
                               numNodes(fg->getMaxNodeId()), 
                               _isPostDominator(isPostDominator), tree(mm, numNodes, NULL) {
    // Create the dominator tree nodes..
    U_32 postNum, id=MAX_UINT32;
    for(postNum = 0; postNum < nodes.size(); ++postNum) {
        Node* node = nodes[postNum];
        id = node->getId();
        tree[id] = new (mm) DominatorNode(node);
    }

    // Create tree itself.  Note that the children of each
    // dominator parent node will be in sorted in
    // reverse postorder.  It's assumed that nodes is 
    // in postorder.
    for(postNum = 0; postNum < nodes.size(); ++postNum) {
        Node* node = nodes[postNum];
        id = node->getId();
        // Assert post-ordering of nodes.
        assert((U_32) postNum == node->getPostNum());
        if(idom[postNum] != NONE) {
            // Assert that idoms are acyclic.
            assert(idom[postNum] > postNum);
            Node* parent = nodes[idom[postNum]];
            U_32 parentId = parent->getId();
            assert(tree[parentId] != NULL);
            // Assert that new child (added to beginning) has highest postorder number.
            assert(tree[parentId]->getChild() == NULL || tree[parentId]->getChild()->getNode()->getPostNum() < postNum);
            tree[parentId]->addChild(tree[id]);
        }
    }

    // Last node is root.
    root = tree[id];
    computeOrder();
}

bool DominatorTree::dominates(Node* a, Node* b) {
    assert(a != NULL && b != NULL);
    assert(a->getId() < numNodes && b->getId() < numNodes);
    DominatorNode* na = tree[a->getId()];
    DominatorNode* nb = tree[b->getId()];
    if (na == NULL || nb == NULL) {
        return false;
    }
    return a==b || isAncestor(na, nb);
}


DomFrontier::DomFrontier(MemoryManager& mm, 
                         DominatorTree& d,
                         ControlFlowGraph*     fg) 
: memManager(mm), dom(d), isPostDominator(d.isPostDominator()) {
    // To Do: may want to keep number of nodes in flow graph instead of recomputing every time
    numNodes = (U_32) fg->getNodeCount();

    // if flow graph has been modified since dominator was computed, then
    // dominator information needs to be recomputed
    if (!dom.isValid()) {
        DominatorBuilder db;
        dom = *db.computeDominators(memManager,fg);
    }

    beenComputed = new (memManager) BitSet(memManager, numNodes);
    DF = (List<Node>**) memManager.alloc(sizeof(List<Node>*)*numNodes);
    memset(DF, 0, numNodes*sizeof(List<Node>*));  // initialized with NULL
}

void DomFrontier::addDF(U_32 entry, Node* n) {
    assert(entry < numNodes);
    // make sure no duplicate entry
    for (List<Node>* l = DF[entry]; l != NULL; l = l->getNext())
        if (l->getElem() == n)  
            return;
    DF[entry] = new (memManager) List<Node>(n,DF[entry]);
}

void DomFrontier::computeDomFrontier(Node* node) {
    U_32 dfnum = node->getDfNum();
    // if dom frontiers are not computed yet
    if (beenComputed->getBit(dfnum))
        return;
    beenComputed->setBit(dfnum,true);

    // If this is a post-dominance frontier, use the reverse graph.
    bool forward = !isPostDominator;

    // compute DF local: the successors of node that are not strictly
    // dominated by node
    const Edges& edges = node->getEdges(forward);
    Edges::const_iterator eiter;
    for(eiter = edges.begin(); eiter != edges.end(); ++eiter) {
        Edge* e = *eiter;
        Node *succ =  e->getNode(forward);
        if (dom.getIdom(succ) != node) {
            addDF(dfnum,succ);
        }
    }

    // compute DF up: Nodes in the dom frontier of node that are dominated
    // by node's immediate dominator
    for (DominatorNode* c = dom.getDominatorNode(node)->getChild(); c != NULL; c = c->getSiblings()) {
        Node* child = c->getNode();
        computeDomFrontier(child);
        
        // go over each frontier of child
        for (List<Node>* f = DF[child->getDfNum()];f != NULL; f = f->getNext()) {
            Node *elem = f->getElem();
            // if node does not strictly dominates elem
            if (elem == node || !dom.dominates(node,elem)) {
                addDF(dfnum,elem);
            }
        }
    }
}


} //namespace Jitrino 
