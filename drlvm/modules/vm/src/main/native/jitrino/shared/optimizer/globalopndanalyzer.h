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

#ifndef _GLOBALOPNDANALYZER_H_
#define _GLOBALOPNDANALYZER_H_

class IRManager;
class Dominator;
class Node;
class Inst;
class LoopTree;

#include "Stl.h"
#include "irmanager.h"

namespace Jitrino {

//
//  Global operand analyzer marks temporaries whose live range spans
//  a basic block boundary as globals
//
class GlobalOpndAnalyzer {
public:
    GlobalOpndAnalyzer(IRManager& irm, ControlFlowGraph* region=NULL) 
        : irManager(irm), flowGraph((region != NULL) ? *region : irm.getFlowGraph()), nodes(irm.getMemoryManager())
    {
    }
    void doAnalysis();
    virtual ~GlobalOpndAnalyzer() {};
protected:
    void resetGlobalBits();
    virtual void markGlobals();

    IRManager&            irManager;
    ControlFlowGraph&            flowGraph;
    Nodes nodes;
};

//
//  Advanced operand analyzer marks temporaries whose live range
//  spans a loop boundary as globals
//

class AdvancedGlobalOpndAnalyzer : public GlobalOpndAnalyzer {
public:
    AdvancedGlobalOpndAnalyzer(IRManager& irm, const LoopTree& loopInfo) 
        : GlobalOpndAnalyzer(irm), opndTable(NULL), loopInfo(loopInfo)
    {}
    virtual ~AdvancedGlobalOpndAnalyzer() {};
private:
    void analyzeInst(Inst* inst, U_32 loopHeader, U_32 timeStamp);
    void unmarkFalseGlobals();
    void markManagedPointerBases();
    virtual void markGlobals();

    struct OpndInfo;
    class  OpndTable;
    OpndTable *      opndTable;
    const LoopTree&  loopInfo;
};

} //namespace Jitrino 

#endif // _GLOBALOPNDANALYZER_H_
