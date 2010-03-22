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

#ifndef _FLOWGRAPH_
#define _FLOWGRAPH_

#include "TranslatorIntfc.h"

#include "Inst.h"
#include "ControlFlowGraph.h"
#include "Dominator.h"
#include "PrintDotFile.h"
#include "Log.h"

#include "Stl.h"
#include "BitSet.h"
#include "List.h"

#include <iomanip>

namespace Jitrino {

class NodeRenameTable;
class Opnd;
class DefUseBuilder;
class DominatorNode;

typedef StlMultiMap<Node*, Node*> NodeMap;

class FlowGraph  {
private:
    FlowGraph(){}
public:

    // Folds the branch at the end of block.  If isTaken, the true edge is
    // converted to an unconditional edge, and the false edge is deleted.
    // If !isTaken, then the false edge is converted, and the true edge
    // is deleted.  In either case, the branch instruction br is removed
    // from block.
    static void  foldBranch(ControlFlowGraph& fg, BranchInst* br, bool isTaken);

    static void  foldSwitch(ControlFlowGraph& fg, SwitchInst* sw, U_32 target);

    // Eliminates the check at the end of block and the associated exception
    // edge.  If (alwaysThrows), then eliminates the non-exception edge instead;
    // we should have already inserted throw instruction before the check.
    static void  eliminateCheck(ControlFlowGraph& fg, Node* block, Inst* check, bool alwaysThrows);

    static Node* tailDuplicate(IRManager& irm, Node* pred, Node* tail, DefUseBuilder& defUses);

    static Node* duplicateRegion(IRManager& irm, Node* entry, StlBitVector& nodesInRegion, DefUseBuilder& defUses, double newEntryFreq=0.0);
    static Node* duplicateRegion(IRManager& irm, Node* entry, StlBitVector& nodesInRegion, DefUseBuilder& defUses, NodeRenameTable& nodeRenameTable, OpndRenameTable& opndRenameTable, double newEntryFreq = 0.0);
    

    static void  renameOperandsInNode(Node *node, OpndRenameTable *renameTable);

    static void doTranslatorCleanupPhase(IRManager& irm);
    

    static void printHIR(std::ostream& cout, ControlFlowGraph& fg, MethodDesc& methodDesc);
    static void print(std::ostream& cout, Node* node);
    static void printLabel(std::ostream& cout, Node* node);
    static void printLabel(std::ostream& cout, DominatorNode* dNode) {printLabel(cout, dNode->getNode()); }
    static void printInsts(std::ostream& cout, Node* node, U_32 indent);

    static void printDotFile(ControlFlowGraph& cfg, MethodDesc& methodDesc,const char *suffix);

private:
    static Node* duplicateNode(IRManager& irm, Node *source, Node *before, OpndRenameTable *renameTable);
    static Node* duplicateNode(IRManager& irm, Node *node, StlBitVector* nodesInRegion, 
                               DefUseBuilder* defUses, OpndRenameTable* opndRenameTable,
                               NodeRenameTable* reverseNodeRenameTable); 
    static Node* _duplicateRegion(IRManager& irm, Node* node, Node* entry,
                                  StlBitVector& nodesInRegion,
                                  DefUseBuilder* defUses,
                                  NodeRenameTable* nodeRenameTable,
                                  NodeRenameTable* reverseNodeRenameTable,
                                  OpndRenameTable* opndRenameTable);

    static Inst* insertPhi(IRManager& irm, StlBitVector* nodesInRegion,
                           NodeRenameTable* reverseNodeRenameTable,
                           StlBitVector& visitedNodes,
                           Node* useNode, DefUseBuilder* defUses,
                           SsaVarOpnd* srcVar1, SsaVarOpnd* srcVar2);
    static Inst* findPhi(Node* node, Opnd** opnds, int opndsCount);
    static bool _inlineFinally(IRManager& irm, Node *from, Node *to, Node *retTarget,
                               NodeRenameTable *nodeRenameTable,
                               OpndRenameTable *opndRenameTable);
    static bool inlineFinally(IRManager& irm, Node *block);

};

class NodeRenameTable : public HashTable<Node,Node> {
public:
    typedef HashTableIter<Node, Node> Iter;

    NodeRenameTable(MemoryManager& mm,U_32 size):HashTable<Node,Node>(mm,size) {}
    Node *getMapping(Node *node) { return (Node*)lookup(node);  }
    void     setMapping(Node *node, Node *to) { insert(node,to); }

protected:
    virtual bool keyEquals(Node* key1,Node* key2) const { return key1 == key2; }
    
    // return hash of address bits
    virtual U_32 getKeyHashCode(Node* key) const { return ((U_32)(((POINTER_SIZE_INT)key) >> sizeof(void*))); }
};

} //namespace Jitrino 

#endif // _FLOWGRAPH_
