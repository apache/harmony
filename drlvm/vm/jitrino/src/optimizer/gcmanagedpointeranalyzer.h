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

#ifndef _GC_MANAGED_POINTER_ANALYZER_H_
#define _GC_MANAGED_POINTER_ANALYZER_H_

#include "open/types.h"
#include "Stl.h"

namespace Jitrino {

class MemoryManager;
class IRManager;
class FlowGraph;
class SsaOpnd;
class VarOpnd;
class SsaVarOpnd;
class Type;


class GCManagedPointerAnalyzer {
public:
    GCManagedPointerAnalyzer(MemoryManager& memoryManager, 
                             IRManager& irManager);

    void analyzeManagedPointers();

private:

    // 
    // Compute _baseMap and _varMap are create base vars when necessary.
    //
    void computeBaseMaps();
    SsaVarOpnd* createVarMapping(Type* baseType, SsaVarOpnd* ptr);

    // 
    // Add definitions for newly created base vars into the flowgraph.
    // 
    void addBaseVarDefs();
    SsaVarOpnd* insertVarDef(SsaVarOpnd* ptr);

    MemoryManager& _memoryManager;
    IRManager& _irManager;

    // Map from each managed pointer ssa opnd to a base ssa opnd.
    // Value is:
    //  - not present = uninitialized (bottom)
    //  - SsaTmpOpnd = known precisely (level 1)
    //  - SsaVarOpnd = created on the fly for ambiguous base (level 2)
    //  - NULL = CLI-only
    typedef StlHashMap<SsaOpnd*, SsaOpnd*> BaseMap;
    BaseMap* _pBaseMap;
    BaseMap& _baseMap;
    
    // Map from each managed pointer var opnd with ambiguous base to base var opnd.
    // This map must be kept consistent with the one above.  All ssa defs 
    // of the managed pointer var must map to ssa defs of this same base var.
    typedef StlHashMap<VarOpnd*, VarOpnd*> VarMap;
    VarMap* _pVarMap;
    VarMap& _varMap;

    bool _mapsComputed;

    bool _rematerializeMode;
};

} //namespace Jitrino 

#endif
