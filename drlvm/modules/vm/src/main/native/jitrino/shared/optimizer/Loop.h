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

#ifndef _LOOP_H_
#define _LOOP_H_

#include "BitSet.h"
#include "Stl.h"
#include "Tree.h"
#include "irmanager.h"
#include "LoopTree.h"

namespace Jitrino {


struct LoopBuilderFlags {
    bool hoist_loads;
    bool invert;
    bool peel;
    bool insideout_peeling;
    bool old_static_peeling;
    bool aggressive_peeling;
    bool peel_upto_branch;
    U_32 peeling_threshold; 
    bool fullpeel;
    U_32 fullpeel_max_inst;
    bool peel_upto_branch_no_instanceof;
};

class LoopBuilder { 
    

public:    
    static void showFlags(std::ostream& os);
    static void readFlags(Action* argSource, LoopBuilderFlags* flags);

public:
    LoopBuilder(MemoryManager& mm, IRManager& irm, DominatorTree& d, bool useProfile); 

    void computeLoops(bool normalize);
    bool needSsaFixup() { return needsSsaFixup; };
    void didSsaFixup() { needsSsaFixup = false; };
    void peelLoops();
private:
    
    class IdentifyByID {
    public:
        U_32 operator() (Node* node) { return node->getId(); }
    };
    class IdentifyByDFN {
    public:
        U_32 operator() (Node* node) { return node->getDfNum(); }
    };

    void        peelLoops(StlVector<Edge*>& backEdges);
    void        hoistHeaderInvariants(Node* preheader, Node* header, StlVector<Inst*>& invariantInsts);
    bool        isVariantInst(Inst* inst, StlHashSet<Opnd*>& variantOpnds);
    bool        isVariantOperation(Operation operation);
    bool        isInversionCandidate(Node* currentHeader, Node* proposedHeader, StlBitVector& nodesInLoop, Node*& next, Node*& exit);
    U_32      markNodesOfLoop(StlBitVector& nodesInLoop,Node* header,Node* tail);
    LoopNode*   findEnclosingLoop(LoopNode* loop, Node* header);


    MemoryManager&  loopMemManager; // for creating loop hierarchy
    DominatorTree&  dom;           // dominator information
    IRManager&      irManager;
    InstFactory&    instFactory;  // create new label inst for blocks
    ControlFlowGraph& fg;
    LoopTree*       info;
    LoopNode*       root;
    bool            useProfile;
    bool            canHoistLoads;
    bool            needsSsaFixup;
    bool            invert;
    const LoopBuilderFlags& flags;
};

} //namespace Jitrino 

#endif // _LOOP_H_
