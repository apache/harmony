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

#include "CodeSelectors.h"

namespace Jitrino {


class HIR2LIRSelectorSessionAction: public SessionAction {
public:
    virtual void run ();
};
static ActionFactory<HIR2LIRSelectorSessionAction> _hir2lir("hir2lir");

//
// code generator entry point
//
void HIR2LIRSelectorSessionAction::run() {

#if defined(_IPF_)
#else
    CompilationContext* cc = getCompilationContext();
    IRManager& irManager = *cc->getHIRManager();
    CompilationInterface* ci = cc->getVMCompilationInterface();
    MethodDesc* methodDesc  = ci->getMethodToCompile();
    OpndManager& opndManager = irManager.getOpndManager();
    const OptimizerFlags& optFlags = irManager.getOptimizerFlags();
    VarOpnd* varOpnds   = opndManager.getVarOpnds();
    MemoryManager& mm  = cc->getCompilationLevelMemoryManager();

    MethodCodeSelector* mcs = new (mm) _MethodCodeSelector(irManager,methodDesc,varOpnds,&irManager.getFlowGraph(),
        opndManager, optFlags.sink_constants, optFlags.sink_constants1);

    Ia32::CodeGenerator cg;
    cg.genCode(this, *mcs);
#endif
}

} //namespace Jitrino 
