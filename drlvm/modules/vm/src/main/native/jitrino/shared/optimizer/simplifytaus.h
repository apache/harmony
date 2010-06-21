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

#ifndef _SIMPLIFY_TAUS_H_
#define _SIMPLIFY_TAUS_H_

#include "open/types.h"
#include "optpass.h"
#include "Stl.h"

namespace Jitrino {

class MemoryManager;
class IRManager;
class SsaOpnd;
class VarOpnd;
class SsaVarOpnd;
class Type;
class FlowGraph;
class Opnd;
class SsaTmpOpnd;


class SimplifyTaus {
public:
    SimplifyTaus(MemoryManager& memoryManager, IRManager& irManager);
    
    void runPass();
private:
    MemoryManager& memManager;
    IRManager&     irManager;
    SsaTmpOpnd    *tauSafeOpnd;
    
    SsaTmpOpnd    *findTauSafeOpnd();
};

#endif // _SIMPLIFY_TAUS_H_


} //namespace Jitrino 
