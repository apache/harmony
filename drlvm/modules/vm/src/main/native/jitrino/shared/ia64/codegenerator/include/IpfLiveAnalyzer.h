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

#ifndef IPFLIVEANALYZER_H_
#define IPFLIVEANALYZER_H_

#include "IpfCfg.h"
#include "IpfLiveManager.h"

using namespace std;

namespace Jitrino {
namespace IPF {

//========================================================================================//
// LiveAnalyzer
//========================================================================================//

class LiveAnalyzer {
public:
                  LiveAnalyzer(Cfg&);
    void          analyze();
    void          dce();
    void          verify();

protected:
    bool          analyzeNode(Node*);
    void          pushPreds(Node*);
    bool          isInstDead(Inst*);

    Cfg           &cfg;
    MemoryManager &mm;
    NodeVector    workSet;      // nodes to be iterated during liveSets calculation or dce
    LiveManager   liveManager;  // Live Manager evaluates current live set from inst to inst
    RegOpndSet    &liveSet;     // reference on current live set in liveManager
    bool          dceFlag;      // make dce if flag is on
};

} // IPF
} // Jitrino

#endif /*IPFLIVEANALYZER_H_*/
