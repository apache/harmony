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

#ifndef _FLOWGRAPHDOM_H_
#define _FLOWGRAPHDOM_H_

#include "MemoryManager.h"
#include "Stl.h"
#include "BitSet.h"
#include "Tree.h"
#include "List.h"
#include "ControlFlowGraph.h"

namespace Jitrino {


/**
 * A DominatorNode represents (post)dominance information computed for a 
 * ControlFlowNode.  A DominatorNode may be use to navigate the 
 * DominatorTree (see below).
 **/
class DominatorNode : public TreeNode {
public:
    DominatorNode(Node* n) : node(n) {}
    DominatorNode* getChild()    {return (DominatorNode*)child;}
    DominatorNode* getSiblings() {return (DominatorNode*)siblings;}
    DominatorNode* getParent()   {return (DominatorNode*)parent;}
    Node* getNode()     {return node;}
private:
    Node* node;
};


/**
 * A DominatorTree represents (post)dominance information computed for a
 * ControlFlowGraph.  It may be used to query or navigate (post)dominance
 * relationships.  The root of the tree is the DominatorNode of the 
 * entry/exit, and the parent of any node (other than the root) is the 
 * immediate dominator/post-dominator.  
 *
 * Note that a DominatorTree is invalidated when the underlying CFG
 * is modified.  The DominatorTree can still be queried/navigated,
 * but may no longer reflect the CFG.
 **/
class DominatorTree: public Tree {
    friend class DominatorBuilder;
public:
    // Returns true if a == b or a strictly (post)dominates b.
    bool dominates(Node* a, Node* b);

    // Returns the immediate (post)dominator of n.  Return NULL if no nodes (post)dominate n.
    Node* getIdom(Node* n)    {
        DominatorNode* ndom = getDominatorNode(n);
        if (ndom == NULL) { //unreachable node -> have no dominator info
            return NULL;
        }
        DominatorNode* pdom = ndom->getParent();
        return (pdom == NULL) ? NULL : pdom->getNode();
    }

    // Returns the dominator tree node associated with this CFG node.
    // Use the tree node to traverse the (post)dominator tree.
    DominatorNode* getDominatorNode(Node* n) {return tree[n->getId()];}

    // Returns the root tree node of the (post)dominator tree.  If this is a
    // dominator tree, this is the tree node of the entry.  If this is a 
    // post-dominator tree, this is the tree node of the exit.
    DominatorNode* getDominatorRoot() {
        return (DominatorNode*) root;
    }

    // Returns the number of nodes in the tree.
    U_32 getNumNodes() const { return numNodes; }

    // True if this is a post-dominator tree. 
    bool isPostDominator() const { return _isPostDominator; }

    // True if the graph was not modified since the dominator was computed.
    bool isValid() { 
        return traversalNum > flowgraph->getModificationTraversalNum(); 
    } 
    U_32 getTraversalNum() { return traversalNum; }

    // True if node has dominator information.  Note, if the dominator tree
    // is not valid overall, it is up to the caller to ensure that an
    // individual nodes information is still valid.
    bool hasDomInfo(Node *n) {
        return (n->getId() < numNodes && tree[n->getId()] != NULL); 
    }

private:
    DominatorTree(MemoryManager& mm, ControlFlowGraph* fg, StlVector<Node*>& nodes, 
        StlVector<U_32>& idom, bool isPostDominator);
 
    ControlFlowGraph*    flowgraph;
    U_32               traversalNum;
    U_32               numNodes;
    bool                   _isPostDominator;
    StlVector<DominatorNode*> tree;
};


/**
 * A DominatorBuilder is used to compute (post)dominator information given
 * a CFG.
 **/
class DominatorBuilder {
public:
    DominatorBuilder() {}
    DominatorTree* computeDominators(MemoryManager& mm, ControlFlowGraph* flowgraph, bool isPost=false, bool ignoreUnreach = false);
    DominatorTree* computePostdominators(MemoryManager& mm, ControlFlowGraph* flowgraph, bool ignoreUnreach) { return computeDominators(mm, flowgraph, true, ignoreUnreach); }
private:
    U_32 intersect(StlVector<U_32>& idom, U_32 num1, U_32 num2);
};

/**
 * DomFrontier provides dominance frontier information given
 * a dominator tree.  Note, if passed a post-dominator tree,
 * the post-dominance frontier is, by definition, the control
 * dependence set.
 **/
class DomFrontier {
public:
    DomFrontier(MemoryManager& mm, DominatorTree& d, ControlFlowGraph* fg);
    void   printDomFrontier(::std::ostream&, Node* n);
    U_32 getNumNodes() {return numNodes;}
    List<Node>* getFrontiersOf(Node* n) {
        assert(n->getDfNum() < numNodes);
        computeDomFrontier(n);
        return DF[n->getDfNum()];
    }
    DominatorTree& getDominator() {return dom;}
private:
    void   computeDomFrontier(Node* n);
    void   addDF(U_32 entry, Node* n);


    List<Node>** DF;
    U_32          numNodes;
    BitSet*         beenComputed;
    MemoryManager&  memManager;
    DominatorTree&  dom;
    bool isPostDominator;
};

} //namespace Jitrino 

#endif // _FLOWGRAPHDOM_H_
