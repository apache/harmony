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

#ifndef _HASHVALUENUMBER_H_
#define _HASHVALUENUMBER_H_

#include "irmanager.h"

namespace Jitrino {

class IRManager;
class Inst;
class DominatorTree;
class MemoryOpt;
class FlowGraph;


class HashValueNumberer {
public:
    HashValueNumberer(IRManager& irm, DominatorTree& dom,
                      bool useBranchConditions = true,
                      bool ignoreAllFlow = false) 
        : irManager(irm), dominators(dom),
          fg(irm.getFlowGraph()),
          useBranches(useBranchConditions)
    {
    }
    void doValueNumbering(MemoryOpt *mopt=0);
    void doGlobalValueNumbering(MemoryOpt *mopt=0);
private:
    IRManager&      irManager;
    DominatorTree&  dominators;
    ControlFlowGraph&  fg;
    bool  useBranches; // do we try to take account of in-edge conditions in a block?
};

} //namespace Jitrino 

#endif // _HASHVALUENUMBER_H_
