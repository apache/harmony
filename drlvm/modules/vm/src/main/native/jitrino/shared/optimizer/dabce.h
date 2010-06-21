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
 * @author Evgueni Brevnov
 *
 */

#ifndef _DYNAMICABCE_H_
#define _DYNAMICABCE_H_

#include "Stl.h"
#include "VMInterface.h"
#include "irmanager.h"
#include "PMFAction.h"
#include "Opnd.h"
#include "Loop.h"
#include "LoopTree.h"
#include "LoopUtils.h"
#include "deadcodeeliminator.h"
#include "escapeanalyzer.h"

namespace Jitrino {

struct DynamicABCEFlags {
    unsigned int    sizeThreshold;
    double          hotnessThreshold;
};

// TODO: Index bounds check for nested loop sould be moved outside of the top level loop if possible.
class DynamicABCE {

    struct ArrayAccessTemplate;
    struct ConstraintInfo;
    
    typedef StlList<ArrayAccessTemplate*> ArrayAccessInfo;
    // Association bewteen array index and its bounds check info.   
    typedef StlHashMap<SsaOpnd*, bool> IndexEliminationInfo;
    // Association between array reference and its index info.
    typedef StlHashMap<SsaOpnd*, IndexEliminationInfo*> ArrayEliminationInfo;
    // Association between array index and its induction info.
    typedef StlHashMap<SsaOpnd*, InductiveOpndLoopInfo*> IndexInductiveInfo;
    // Association bewteen induction variable and its monotonicity.   
    typedef StlHashMap<SsaOpnd*, bool> MonotonicityInfo;
    
    struct ArrayAccessTemplate {
        
        ArrayAccessTemplate(): array(NULL), arrayLen(NULL), index(NULL), checkBoundsInst(NULL) {}
        SsaOpnd* array;
        SsaOpnd* arrayLen;
        SsaOpnd* index;
        Inst* checkBoundsInst;
    };
    
    struct HotnessSorter {
         bool operator()(Node* const& lhs, Node* const& rhs) {
              return lhs->getExecCount() >= rhs->getExecCount();
         }
    };
    
    class SubGraph: public ControlFlowGraph {
    public:
        SubGraph(MemoryManager& memoryManager): ControlFlowGraph(memoryManager) {}
        Node* getSpecialExitNode() { return specialExit; }
        void  setSpecialExitNode(Node* node) { specialExit = node; } 
    private:
        Node* specialExit;        
    };
    
public:
    DynamicABCE(IRManager& irm, SessionAction* sa = NULL);
    
    static void readFlags(Action* argSource, DynamicABCEFlags* flags);
    static void showFlags(std::ostream& os);
    
    void run();

private:
    void optimizeLoops();
    void findLoopsToOptimize(LoopNode* topLevelLoop);
    bool eliminateBoundsCheck(InductionDetector* inductionDetector,
                              const ArrayAccessTemplate& arrayAccess);    
    InvariantOpndLoopInfo*
    promoteVarToTmp(InvariantOpndLoopInfo* invOpnd,
                    InductionDetector* inductionDetector);
    
    InductiveOpndLoopInfo*
    getSimplifiedInduction(InductionDetector* inductionDetector,
                           const ArrayAccessTemplate& arrayAccess);
    bool isProperMonotonicity(InductiveOpndLoopInfo* base,
                              InvariantOpndLoopInfo* startValue,
                              InvariantOpndLoopInfo* endValue);
    void genBoundConstraint(InvariantOpndLoopInfo* scale,
                            InvariantOpndLoopInfo* baseValue,
                            InvariantOpndLoopInfo* stride,
                            SsaOpnd* arrayLength,
                            bool canReachBound);
    void fillTemplate(ArrayAccessTemplate* arrayAccesses, Inst* checkInst);    
    Edge* findUnconditionalInEdge(Node* targetNode);
    Node* getClonedLoop();
    void findArrayAccesses();
    void linkSubGraph();
    bool isDefInLoop(SsaOpnd* opnd);
    void clearAll();

    const double INC_SEQ_PROB;
    const double IN_BOUNDS_PROB;
    
    IRManager&          irManager;
    MemoryManager&      memoryManager;
    ControlFlowGraph&   flowGraph;
    TypeManager&        typeManager;
    InstFactory&        instFactory;
    OpndManager&        opndManager;
    
    ArrayAccessInfo         arrayAccesses;
    IndexInductiveInfo      inductionInfo;        
    ArrayEliminationInfo    eliminationInfo;
    MonotonicityInfo        monotonicityInfo;
    StlVector<Node*>        loopsToOptimize;
    DynamicABCEFlags&       flags;
    
    DeadCodeEliminator* dce;
    HotnessSorter*      sortByHotness;        
    LoopTree*           loopTree;
    SubGraph*           subGraph;
    // Original loop which get transformed.
    LoopNode*           optimizedLoop;
    // Unmodified clone of the original loop.
    Node*               clonedLoop;
};

} // namespace Jitrino

#endif /*_DYNAMICABCE_H_*/
