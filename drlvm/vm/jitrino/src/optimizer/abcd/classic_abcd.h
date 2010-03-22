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

#ifndef _CLASSIC_ABCD_H
#define _CLASSIC_ABCD_H

#include <iostream>
#include "open/types.h"
#include "Opcode.h"
#include "FlowGraph.h"
#include "optpass.h"
#include "classic_abcd_solver.h"
#include "insertpi.h"

namespace Jitrino {

class ClassicAbcd {
public:    
    ClassicAbcd(SessionAction* arg_source, IRManager &ir_manager, 
                MemoryManager& mem_manager, DominatorTree& dom0);
    void runPass();
private:
    friend class BuildInequalityGraphWalker;

    enum RedundancyType {
        rtNONE_MASK = 0x0,
        rtLOWER_MASK = 0x1,
        rtUPPER_MASK = 0x2,
        rtFULL_MASK  = 0x3
    };

    typedef StlMap<Inst *, RedundancyType> InstRedundancyMap;

    // utility-method to update a value for markRedundantInstructions(...)
    void updateOrInitValue
         (InstRedundancyMap& map, Inst* inst, RedundancyType type);

    // marks upper-redundant or lower-redundant operations and stores output in
    // _redundantChecks, if an instruction was marked as redundant, the value is
    // correctly updated with the new redundancy info
    void markRedundantInstructions
         (bool upper_problem, InequalityGraph& igraph, ControlFlowGraph& cfg);

    IRManager& _irManager;
    MemoryManager& _mm;
    DominatorTree& _domTree;
    InstRedundancyMap _redundantChecks;
    IOpnd* _zeroIOp;

    bool _runTests;
    bool _useAliases;
    bool _dump_abcd_stats;
};

} //namespace Jitrino 

#endif /* _CLASSIC_ABCD_H */
