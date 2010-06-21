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

#ifndef _INSERTPI_H
#define _INSERTPI_H

#include <iostream>

#include "abcdbounds.h"
#include "FlowGraph.h"
#include "Opcode.h"
#include "open/types.h"
#include "opndmap.h"

namespace Jitrino {

class InsertPi {
public:
    InsertPi(MemoryManager& mm, DominatorTree& dom_tree, IRManager& irm, 
             bool use_aliases) :
        _mm(mm), 
        _domTree(dom_tree), 
        _useAliases(use_aliases),
        _irManager(irm),
        _blockTauEdge(0),
        _lastTauEdgeBlock(0),
        _piMap(0)
    {}

    // Add a Pi node in the node if it is after each test 
    // which tells something about a variable
    void insertPi();

    void removePi();

    void setUseAliases(bool use = true) { _useAliases = use; }
    bool useAliases() const { return _useAliases; }
private:
    friend class InsertPiWalker;
    friend class RenamePiWalker;
    friend class RemovePiWalker;

    // add Pi in the node iff after a test which tells something about the var
    void insertPiToNode(Node* block);

    // definition: PEI=Potentially Excepting Instruction
    void insertPiForUnexceptionalPEI(Node *block, Inst *lasti);

    SsaTmpOpnd* getBlockTauEdge(Node *block);

    void insertPiForBranch(Node* block, 
                           BranchInst* branchi, 
                           Edge::Kind kind);

    void insertPiForComparison(Node* block,
                               ComparisonModifier mod,
                               const PiCondition& bounds,
                               Opnd* op,
                               bool swap_operands,
                               bool negate_comparison);

    void insertPiForOpnd(Node* block, 
                         Opnd* org, 
                         const PiCondition &cond,
                         Opnd* tauOpnd);

    // checks for aliases of opnd, inserts them.
    void insertPiForOpndAndAliases(Node* block, 
                                   Opnd* org, 
                                   const PiCondition& cond,
                                   Opnd* tauOpnd);

    bool getAliases(Opnd *opnd, AbcdAliases *aliases, int64 addend);

    Opnd* getConstantOpnd(Opnd *opnd);

    // Renames variables for which we have Pi nodes.
    void renamePiVariables();

    void renamePiVariablesInNode(Node *block);

    void renamePiVariablesInDomNode(DominatorNode *block);

    void removePiOnInst(Node* block, Inst *inst);

    MemoryManager& _mm;
    DominatorTree& _domTree;
    bool _useAliases;
    IRManager& _irManager;

    SsaTmpOpnd* _blockTauEdge;
    Node* _lastTauEdgeBlock;
    SparseOpndMap *_piMap;
};

} //namespace Jitrino 

#endif /* _INSERTPI_H */
