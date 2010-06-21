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

#ifndef IPFINSTRUMENTATOR_H_
#define IPFINSTRUMENTATOR_H_

#include "IpfCfg.h"
#include "IpfOpndManager.h"

namespace Jitrino {
namespace IPF {

//=======================================================================================//
// Instrumentator
//========================================================================================//

class Instrumentator {
public:
                   Instrumentator(Cfg&);
    void           instrument();
    static void    methodStart(Method_Handle);
    static void    methodEnd(Method_Handle);

protected:
    void           instrumentStart();
    void           instrumentEnd();
    void           genNativeCall(uint64, InstVector&);

    MemoryManager  &mm;
    OpndManager    *opndManager;
    Cfg            &cfg;
};

} // IPF
} // Jitrino

#endif /*IPFINSTRUMENTATOR_H_*/
