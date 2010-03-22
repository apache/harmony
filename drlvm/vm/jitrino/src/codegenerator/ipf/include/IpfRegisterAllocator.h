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

#ifndef IPFREGISTERALLOCATOR_H_
#define IPFREGISTERALLOCATOR_H_

#include "IpfCfg.h"
#include "IpfLiveManager.h"

namespace Jitrino {
namespace IPF {

//========================================================================================//
// RegisterAllocator
//========================================================================================//

class RegisterAllocator {
public:
                       RegisterAllocator(Cfg&);
    void               allocate();

protected:
    void               buildInterferenceMatrix();
    void               removeSelfDep();
    void               assignLocations();
    
    void               assignLocation(RegOpnd*);
    void               updateAllocSet(Opnd*, U_32, QpMask);
    void               checkCallSite(Inst*, QpMask);
    
    void               checkCoalescing(U_32, Inst*);
    void               removeSameRegMoves();

    MemoryManager      &mm;
    Cfg                &cfg;
    OpndManager        *opndManager;
    LiveManager        liveManager;
    RegOpndSet         allocSet;       // set of all opnds that need allocation
    RegOpndSet         &liveSet;       // set of opnds alive in current node
};

} // IPF
} // Jitrino

#endif /*IPFREGISTERALLOCATOR_H_*/
