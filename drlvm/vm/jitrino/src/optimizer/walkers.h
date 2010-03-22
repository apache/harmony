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

#ifndef _WALKERS_H
#define _WALKERS_H

#include <assert.h>
#include <iostream>
#include <algorithm>
#include "Stl.h"

#include "MemoryManager.h"

#include "Inst.h"
#include "irmanager.h"
#include "Dominator.h"

namespace Jitrino {

class DominatorNode;
class Node;
class DominatorTree;

/* example DomWalker
class DomWalker {
public:
    void applyToDominatorNode(DominatorNode *domNode) {};
    void enterScope() {}; // is called before a node and its children are processed
    void exitScope() {}; // is called after node and children are processed
};
*/

// either pre-order or post-order
template <bool preorder, class DomWalker>
void DomTreeWalk(DominatorTree &dTree, DomWalker &walker, MemoryManager &mm)
{
    assert(dTree.isValid());
    StlDeque<DominatorNode *> dom_stack(mm);

    walker.enterScope();
    dom_stack.push_back(dTree.getDominatorRoot());
    while (!dom_stack.empty()) {
        DominatorNode *domNode = dom_stack.back();
        
        if (domNode) {
            walker.enterScope(); 
            // scope is associated with this node and its children
            
            if (preorder) {
                walker.applyToDominatorNode(domNode); // process node
            }
            
            // move to next sibling;
            dom_stack.back() = domNode->getSiblings();
            
            // but first deal with this node's children
            dom_stack.push_back(domNode); // push parent
            dom_stack.push_back(domNode->getChild()); 
                    // scope will be exited if no children left
        } else {
            dom_stack.pop_back(); // no children left, exit scope
            if (!dom_stack.empty()) {
                domNode = dom_stack.back(); dom_stack.pop_back(); // get parent
                if (!preorder) {
                    walker.applyToDominatorNode(domNode);
                }
            }
            walker.exitScope();
        }
    }
}

/* example InstWalker
class InstWalker {
private:
   void applyToInst(Inst *i);
};
*/

// walker can modify/insert/unlink anything before i->next() (if forward)
// or anything after i->prev() (if !forward)
template <bool forward, class InstWalker>
void WalkInstsInBlock(Node *node, InstWalker &walker)
{
    Inst* inst  =  (Inst*)(forward ? node->getFirstInst() : node->getLastInst());
    while(inst!=NULL) {
        Inst *next = forward ? inst->getNextInst() : inst->getPrevInst();
        walker.applyToInst(inst);
        inst = next;
    }
}

/*
  example 
  class DomNodeInstWalker {
      void startNode(DominatorNode *node);
      void applyToInst(Inst *i);
      void finishNode(DominatorNode *node);
  };

 */


// adapter from DomNodeInst to DomWalker
template <bool forward, class DomNodeInstWalker>
class DomNodeInst2DomWalker {
    DomNodeInstWalker &instWalker;
public:
    void applyToDominatorNode(DominatorNode *domNode) {
        instWalker.startNode(domNode);
        WalkInstsInBlock<forward, DomNodeInstWalker>(domNode->getNode(), instWalker);
        instWalker.finishNode(domNode);
    }
    void enterScope() {}; // is called before a node and its children are processed
    void exitScope() {}; // is called after node and children are processed
    DomNodeInst2DomWalker(DomNodeInstWalker &instWalker0) :
        instWalker(instWalker0) {};
};

/*
  example 
  class ScopedDomNodeInstWalker {
      void startNode(DominatorNode *node);
      void applyToInst(Inst *i) {};
      void finishNode(DominatorNode *node);
      void enterScope();
      void exitScope();
  };

 */

// adapter from DomNodeInst to DomWalker
template <bool forward, class ScopedDomNodeInstWalker>
class ScopedDomNodeInst2DomWalker {
    ScopedDomNodeInstWalker &instWalker;
public:
    void applyToDominatorNode(DominatorNode *domNode) {
        instWalker.startNode(domNode);
        WalkInstsInBlock<forward, ScopedDomNodeInstWalker>(domNode->getNode(), 
                                                           instWalker);
        instWalker.finishNode(domNode);
    }
    void enterScope() { instWalker.enterScope(); };
    void exitScope() { instWalker.exitScope(); };
    ScopedDomNodeInst2DomWalker(ScopedDomNodeInstWalker &instWalker0) :
        instWalker(instWalker0) {};
};

/* example NodeWalker
class NodeWalker {
    void applyToCFGNode(Node *node);
};
class NodeInstWalker  {
    void applyToInst(Inst *inst);
    void startNode(CFGNOde *node);
    void finishNode(Node *node);
};
*/

template <bool forward, class NodeInstWalker>
class NodeInst2NodeWalker {
    NodeInstWalker &instWalker;
public:
    void applyToCFGNode(Node *node) {
        instWalker.startNode(node);
        WalkInstsInBlock<forward, NodeInstWalker>(node, instWalker);
        instWalker.finishNode(node);
    }
    NodeInst2NodeWalker(NodeInstWalker &instWalker0) :
        instWalker(instWalker0) {};
};

template <bool forward, class InstWalker>
class Inst2NodeWalker {
    InstWalker &instWalker;
public:
    void applyToCFGNode(Node *node) {
        WalkInstsInBlock<forward, InstWalker>(node, instWalker);
    }
    Inst2NodeWalker(InstWalker &instWalker0) :
        instWalker(instWalker0) {};
};

template <class NodeWalker>
struct NodeWalkDelegate : public ::std::unary_function<Node *, void > {
    NodeWalker &realWalker;
    void operator()(Node *n) {
        realWalker.applyToCFGNode(n);
    };
    NodeWalkDelegate(NodeWalker &w) : realWalker(w) {};
};

// walk over flowgraph nodes in any order
template <class NodeWalker>
void NodeWalk(ControlFlowGraph &fg, NodeWalker &walker)
{
    NodeWalkDelegate<NodeWalker> delegate(walker);
    const Nodes &nodes = fg.getNodes();
    ::std::for_each(nodes.begin(), nodes.end(), delegate);
}

template <class inst_fun_type>
struct Inst2NodeFun : public ::std::unary_function<Node *, void>
{
    inst_fun_type underlying_fun;
    Inst2NodeFun(const inst_fun_type &theFun) : underlying_fun(theFun) {};
    void operator()(Node *theNode) {
        Inst *first = theNode->getFirstInst();
        Inst *thisinst = first;
        assert(thisinst);
        do {
            underlying_fun(thisinst);
            thisinst = thisinst->next();
        } while (thisinst != first);
    }
};

template <class node_fun_type>
struct Node2FlowgraphFun : public ::std::unary_function<ControlFlowGraph &, void>
{
    node_fun_type underlying_fun;
    Node2FlowgraphFun(const node_fun_type &theFun) : underlying_fun(theFun) {};
    void operator()(ControlFlowGraph &fg) {
        const Nodes &nodes = fg.getNodes();
        ::std::for_each(nodes.begin(), nodes.end(), underlying_fun);
    }
};


} //namespace Jitrino 

#endif
