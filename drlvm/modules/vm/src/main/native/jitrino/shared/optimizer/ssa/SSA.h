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

#ifndef _SSA_H_
#define _SSA_H_

#include "MemoryManager.h"
#include "Stack.h"
#include "FlowGraph.h"
#include "HashTable.h"
#include "Dominator.h"
#include "optpass.h"

namespace Jitrino {

class DomFrontier;
class SparseOpndMap;
struct OptimizerFlags;


//
// There is a VarDefSites for each var that records all CFG nodes in which 
// the var is defined. Phi node also defines var. The two bit vectors are
// used to avoid adding/recording CFG nodes twice.
// 
class VarDefSites {
public:
    VarDefSites(MemoryManager& m, U_32 max) 
    : stack(m), alreadyRecorded(m, max), insertedPhi(m, max) {}
    void addDefSite(Node* node) {
        // avoid pushing the same node twice onto the stack
        if (alreadyRecorded.getBit(node->getDfNum()))
            return;
        alreadyRecorded.setBit(node->getDfNum(),true);
        stack.push(node);
    }
    Node* removeDefSite() {return stack.pop();}
    void insertPhiSite(Node* node) {
        insertedPhi.setBit(node->getDfNum(), true);
        addDefSite(node);
    }
    bool beenInsertedPhi(Node* node) {
        return insertedPhi.getBit(node->getDfNum());
    }
    bool isDefSite(Node* node) {
        return alreadyRecorded.getBit(node->getDfNum());
    }
private:
    Stack<Node> stack;
    BitSet         alreadyRecorded; // def sites (blocks) been recorded
    BitSet         insertedPhi;     // blocks that have been inserted phi 
};

//
// A hash table that keeps the mapping: var --> VarDefSites
//
class DefSites {
public:
    DefSites(MemoryManager& m, U_32 n) : table(m,32), mm(m), numNodes(n) {}
    void addVarDefSite(VarOpnd* var, Node* node) {
        if (var == NULL) return;

        VarDefSites* varSites = table.lookup(var);
        if (varSites == NULL) {
            varSites = new (mm) VarDefSites(mm, numNodes);
            table.insert(var,varSites);
        }
        varSites->addDefSite(node);
    }
    PtrHashTable<VarDefSites>* getVarDefSites() {return &table;}
private:
     PtrHashTable<VarDefSites> table;
     MemoryManager&  mm;
     U_32        numNodes;
};

class RenameStack;

class SSABuilder {
public:
    SSABuilder(OpndManager& om, InstFactory& factory, DomFrontier& df, ControlFlowGraph* f, const OptimizerFlags& optFlags) 
    : instFactory(factory), frontier(df), opndManager(om), fg(f), createPhi(false), optimizerFlags(optFlags) {}
    bool convertSSA(MethodDesc& methodDesc);
    bool fixupSSA(MethodDesc& methodDesc, bool useBetterAlg);
    bool fixupVars(ControlFlowGraph* fg, MethodDesc& methodDesc);
    static void deconvertSSA(ControlFlowGraph* fg,OpndManager& opndManager);
    static void splitSsaWebs(ControlFlowGraph* fg,OpndManager& opndManager);
    static bool phiInstsOnRightPositionsInBB(Node* node);
private:
    void findDefSites(DefSites& allDefSites);
    void insertPhi(DefSites& allDefSites);
    void createPhiInst(VarOpnd* var, Node* insertedLoc);
    void addPhiSrc(PhiInst* i, SsaVarOpnd* src);
    void renameNode(RenameStack *rs, DominatorNode* dt,
                    const StlVectorSet<VarOpnd *> *whatVars);
    void clearPhiSrcs(Node *, const StlVectorSet<VarOpnd *> *whatVars);
    void clearPhiSrcs2(Node *, 
                       const StlVectorSet<VarOpnd *> *whatVars,
                       StlVector<VarOpnd *> *changedVars,
                       const StlVectorSet<Opnd *> *removedVars,
                       StlVector<Node *> &scratchNodeList);
    bool checkForTrivialPhis(Node *, 
                             StlVector<VarOpnd *> &changedVars);
    void checkForTrivialPhis2(Node *node, 
                              const StlVectorSet<VarOpnd *> *lookatVars,
                              StlVector<VarOpnd *> *changedVars,
                              StlVector<Opnd *> *removedVars);

    InstFactory& instFactory;
    DomFrontier& frontier;
    OpndManager& opndManager;
    ControlFlowGraph*   fg;
    bool         createPhi;
    const OptimizerFlags& optimizerFlags;

    friend class ClearPhiSrcsWalker;
    friend class CheckForTrivialPhisWalker;
    friend class ClearPhiSrcsWalker2;
    friend class CheckForTrivialPhisWalker2;
    friend class SsaRenameWalker;
};

} //namespace Jitrino 

#endif // _SSA_H_
