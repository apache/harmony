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
 * @author Intel, George A. Timoshenko
 *
 */

#ifndef _JAVAFLOWGRAPHBUILDER_
#define _JAVAFLOWGRAPHBUILDER_

#include "MemoryManager.h"
#include "ControlFlowGraph.h"

namespace Jitrino {

class JavaFlowGraphBuilder {
public:
    // regular version
    JavaFlowGraphBuilder(MemoryManager&, IRBuilder &);
    // version for IR inlining
    JavaFlowGraphBuilder(MemoryManager&, IRBuilder &, Node *, ControlFlowGraph *);
    Node*     genBlock(LabelInst* blockLabel);
    Node*     genBlockAfterCurrent(LabelInst *label);
    void         build();
    void         buildIRinline(Opnd *ret, ControlFlowGraph*, Inst *callsite); // version for IR inlining
    ControlFlowGraph*   getCFG() { return fg; }
    Node* createDispatchNode();
private:
    void         createCFGEdges();
    Node*        edgesForBlock(Node* block);
    void         edgesForHandler(Node* entry);
    void         edgeForFallthrough(Node* block);
    
    Node* createBlockNodeOrdered(LabelInst* label);
    Node* createBlockNodeAfter(Node* node, LabelInst* label);
    void addEdge(Node* source, Node* target);

    //
    // private fields
    //
    MemoryManager&  memManager;
    Node*        currentBlock;
    ControlFlowGraph*      fg;
    IRBuilder&      irBuilder;
typedef StlList<Node*> NodeList;
    NodeList fallThruNodes;
    void*    methodHandle;
};


} //namespace Jitrino 

#endif // _JAVAFLOWGRAPHBUILDER_
