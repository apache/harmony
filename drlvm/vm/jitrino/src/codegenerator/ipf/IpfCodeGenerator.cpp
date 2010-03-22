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

#include "Jitrino.h"
#include "CodeSelectors.h"
#include "IpfCodeGenerator.h"
#include "IpfCodeSelector.h"
#include "IpfCodeLayouter.h"
#include "IpfLiveAnalyzer.h"
#include "IpfRegisterAllocator.h"
#include "IpfSpillGen.h"
#include "IpfIrPrinter.h"
#include "IpfEmitter.h"
#include "IpfPrologEpilogGenerator.h"
#include "IpfRuntimeSupport.h"
#include "IpfCfgVerifier.h"
#include "IpfInstrumentator.h"

namespace Jitrino {
namespace IPF {

static const char* help = "  Ipf CodeGen\n";
static ActionFactory<CodeGenerator> _ipf_codegen("ipf_codegen", help);

//========================================================================================//
// IpfCodeGenerator
//========================================================================================//

CodeGenerator::CodeGenerator() {

    compilationInterface = NULL;
    cfg                  = NULL;
    methodDesc           = NULL;
}

//----------------------------------------------------------------------------------------//

void CodeGenerator::run() {
    
    MemoryManager mm("IpfCodeGenerator");

    CompilationContext   *cc          = CompilationContext::getCurrentContext();
    IRManager            &irManager   = *cc->getHIRManager();
    CompilationInterface *ci          = cc->getVMCompilationInterface();
    MethodDesc           *methodDesc  = ci->getMethodToCompile();
    ::Jitrino::OpndManager &opndManager = irManager.getOpndManager();
    const OptimizerFlags &optFlags    = irManager.getOptimizerFlags();
    VarOpnd              *varOpnds    = opndManager.getVarOpnds();

    MethodCodeSelector *methodCodeSelector = new(mm) _MethodCodeSelector(irManager, methodDesc,
        varOpnds, &irManager.getFlowGraph(), opndManager, optFlags.sink_constants, optFlags.sink_constants1);
    
    compilationInterface = CompilationContext::getCurrentContext()->getVMCompilationInterface();
    cfg                  = new(mm) Cfg(mm, *compilationInterface);
    methodDesc           = compilationInterface->getMethodToCompile();
    IrPrinter irPrinter(*cfg);

    IPF_LOG << endl << IrPrinter::toString(methodDesc) << endl;
    IPF_LOG << endl << "=========== Stage: Code Selector =============================" << endl;
    IpfMethodCodeSelector ipfMethodCodeSelector(*cfg, *compilationInterface);
    methodCodeSelector->selectCode(ipfMethodCodeSelector);

    methodDesc = ipfMethodCodeSelector.getMethodDesc();
    cfg->getOpndManager()->insertProlog(*cfg);
    cfg->getOpndManager()->saveThisArg();
    if(LOG_ON) irPrinter.printCfgDot("cs.dot");

//    IPF_LOG << endl << "=========== Stage: Code Instrumentation ======================" << endl;
//    Instrumentator instrumentator(*cfg);
//    instrumentator.instrument();

    IPF_LOG << endl << "=========== Stage: Code Layouter =============================" << endl;
    CodeLayouter codeLayouter(*cfg);
    codeLayouter.layout();
    if(LOG_ON) irPrinter.printCfgDot("cl.dot");
    if(LOG_ON) irPrinter.printAsm(LOG_OUT);

    IPF_LOG << endl << "=========== Stage: Liveness analysis =========================" << endl;
    LiveAnalyzer liveAnalyzer(*cfg);
    liveAnalyzer.analyze();
    liveAnalyzer.dce();

    IPF_LOG << endl << "=========== Stage: Build GC Root Set =========================" << endl;
    RuntimeSupport runtimeSupport(*cfg, *compilationInterface);
    runtimeSupport.buildRootSet();

    IPF_LOG << endl << "=========== Stage: Register Allocator ========================" << endl;
    RegisterAllocator registerAllocator(*cfg);
    registerAllocator.allocate();
    if(LOG_ON) irPrinter.printAsm(LOG_OUT);

    IPF_LOG << endl << "=========== Stage: Prolog and Epilog Generator ===============" << endl;
    PrologEpilogGenerator prologEpilogGenerator(*cfg);
    prologEpilogGenerator.genPrologEpilog();
    
    IPF_LOG << endl << "=========== Stage: Spill Generator ===========================" << endl;
    SpillGen spillGen(*cfg);
    spillGen.genSpillCode();
    if(LOG_ON) irPrinter.printAsm(LOG_OUT);

    IPF_LOG << endl << "=========== Stage: Code Emitter ==============================" << endl;
    Emitter emitter(*cfg, *compilationInterface, false);
    bool ret = emitter.emit();

    IPF_LOG << endl << "=========== Stage: Make Runtime Info =========================" << endl;
    runtimeSupport.makeRuntimeInfo();
    
    if(ret) IPF_LOG << endl << "=========== Compilation Successful ===========================" << endl;
    else    IPF_LOG << endl << "=========== Compilation Failed ===============================" << endl;
}

} // IPF
} // Jitrino 

