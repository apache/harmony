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
 * @author Intel, Konstantin M. Anisimov, Igor V. Chebykin
 *
 */

#ifndef IPFCODELAYOUTER_H_
#define IPFCODELAYOUTER_H_

#include "IpfCfg.h"

namespace Jitrino {
namespace IPF {

typedef StlMap <uint64, BbNode*> Long2Node;

//========================================================================================//
// CodeLayouter
//========================================================================================//

class CodeLayouter {
public:
                  CodeLayouter(Cfg&);
    void          layout();

protected:
    void          transformPredicatedCalls();
    void          transformPredicatedCall(BbNode*, Long2Node&);
    Edge*         getUnwindEdge(Node*);

    // merge sequential nodes
    void          mergeNodes();
    BbNode*       getCandidate(BbNode*);
    bool          isMergable(BbNode*, BbNode*);
    void          merge(BbNode*, BbNode*);
    void          checkUnwind();

    // layout nodes
    void          makeChains();
    void          inChainList(Edge*);
    void          pushBack(Chain*, Node*);
    void          pushFront(Chain*, Node*);
    void          layoutNodes();
    U_32        calculateChainWeight(Chain*);

    // set branch targets
    void          setBranchTargets();
    void          fixConditionalBranch(BbNode*);
    void          fixSwitch(BbNode*);
    void          fixUnconditionalBranch(BbNode*);
    
    MemoryManager &mm;
    Cfg           &cfg;
    OpndManager   *opndManager;
    ChainList     chains;
    NodeSet       visitedNodes;
};

} // IPF
} // Jitrino
#endif /*IPFCODELAYOUTER_H_*/
