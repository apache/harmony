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

#ifndef _GLOBAL_CODE_MOTION_H
#define _GLOBAL_CODE_MOTION_H

#include <iostream>
#include "open/types.h"
#include "Opcode.h"
#include "Stl.h"
#include <utility>

namespace Jitrino {

class IRManager;
class FlowGraph;
class methodDesc;
class MemoryManager;
class InequalityGraph;
class DominatorNode;
class Dominator;
class Node;
class Opnd;
class CSEHashTable;
class Type;
class LoopTree;

struct GcmFlags {
    bool dry_run;
    bool sink_stvars;
    bool min_cut;
    bool sink_constants;
};


/*
 * Implementation of Global Code Motion,
 * [C.Click. Global Code Motion, Global Value Numbering. PLDI 1995]
 */
 
class GlobalCodeMotion {
    IRManager& irManager;
    ControlFlowGraph& fg;
    MethodDesc& methodDesc;
    MemoryManager &mm;
    DominatorTree& dominators;
    LoopTree *loopTree;
private:
    GcmFlags& flags;
public:    
    static void readFlags(Action* argSource, GcmFlags* flags);
    static void showFlags(std::ostream& os);

    GlobalCodeMotion(IRManager &irManager0, 
                     MemoryManager& memManager,
                     DominatorTree& dom0,
                     LoopTree *loopTree);
    
    ~GlobalCodeMotion();

    void runPass();

private:
    bool isPinned(Inst *i);
    StlHashMap<Inst*, DominatorNode *> earliest; // block for earliest placement
    StlHashMap<Inst*, DominatorNode *> latest;
    typedef StlHashSet<Inst *> VisitedSet;
    VisitedSet visited;
    typedef StlHashSet<Inst *> UsesSet;
    typedef StlHashMap<Inst *, UsesSet*> UsesMap;
    UsesMap uses;

    void scheduleAllEarly();
    void scheduleBlockEarly(Node *n);
    void scheduleEarly(DominatorNode *domNode, Inst *i);

    void scheduleAllLate();
    void scheduleLate(DominatorNode *domNode, Inst *basei, Inst *i);
    void sinkAllConstants();
    void sinkConstants(Inst *i);

    // utils
    DominatorNode *leastCommonAncestor(DominatorNode *a, DominatorNode *b);

    void markAsVisited(Inst *i);
    bool alreadyVisited(Inst *i);
    void clearVisited();

    friend class GcmScheduleEarlyWalker;
    friend class GcmScheduleLateWalker;
    friend class GcmSinkConstantsWalker;
};

} //namespace Jitrino 

#endif // _GLOBAL_CODE_MOTION_H
