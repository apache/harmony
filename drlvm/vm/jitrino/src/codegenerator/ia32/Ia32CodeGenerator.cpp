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
 * @author Vyacheslav P. Shakin
 */

#define IRTRANSFORMER_REGISTRATION_ON 1

#include <fstream>
#include "Stl.h"
#include "Ia32CodeGenerator.h"
#include "Ia32CodeSelector.h"
#include "Log.h"
#include "Ia32IRManager.h"
#include "Ia32Printer.h"

#ifdef PLATFORM_POSIX
    #include <stdarg.h>
    #include <sys/stat.h>
    #include <sys/types.h>
#endif //PLATFORM_POSIX


namespace Jitrino
{
namespace Ia32
{

//___________________________________________________________________________________________________
void _cdecl die(U_32 retCode, const char * message, ...)
{
    ::std::cerr<<"---------- die called (ret code = "<<retCode<<") --------------------------------------"<<std::endl;
    if (message!=NULL){
        va_list args;
        va_start(args, message);
        char str[0x10000];
        vsprintf(str, message, args);
        ::std::cerr<<str<<std::endl;
    }
    exit(retCode);
}


//___________________________________________________________________________________________________
class InstructionFormTranslator : public SessionAction {
    void runImpl(){ irManager->translateToNativeForm(); }
    U_32 getNeedInfo()const{ return 0; }
    U_32 getSideEffects()const{ return 0; }
};

static ActionFactory<InstructionFormTranslator> _native("native");

//___________________________________________________________________________________________________
class UserRequestedDie : public SessionAction {
    void runImpl(){ die(10, getArg("msg")); }
    U_32 getNeedInfo()const{ return 0; }
    U_32 getSideEffects()const{ return 0; }
    bool isIRDumpEnabled(){ return false; }
};

static ActionFactory<UserRequestedDie> _die("die");

//___________________________________________________________________________________________________
class UserRequestedBreakPoint : public SessionAction {
    void runImpl(){ 
        irManager->getFlowGraph()->getEntryNode()->prependInst(irManager->newInst(Mnemonic_INT3));
    }
    U_32 getNeedInfo()const{ return 0; }
    U_32 getSideEffects()const{ return 0; }
    bool isIRDumpEnabled(){ return false; }
};

static ActionFactory<UserRequestedBreakPoint> _break("break");


//================================================================================
//  class CodeGenerator
//================================================================================

//___________________________________________________________________________________________________

void CodeGenerator::genCode(::Jitrino::SessionAction* sa, ::Jitrino::MethodCodeSelector& inputProvider) {
    LogStream& irdump  = Log::log(LogStream::IRDUMP);
    LogStream& dotdump = Log::log(LogStream::DOTDUMP);
    U_32 stageId = Log::getStageId();
    const char* stageName = sa->getName();
    if (irdump.isEnabled()) {
        Log::printStageBegin(irdump.out(), stageId, "IA32", stageName, stageName);
    }


    CompilationContext* cc = sa->getCompilationContext();
    CompilationInterface* ci = cc->getVMCompilationInterface();
    MemoryManager& mm = cc->getCompilationLevelMemoryManager();
    IRManager* irManager = new (mm) IRManager(mm,ci->getTypeManager(),*ci->getMethodToCompile(), *ci);
#ifdef _DEBUG
    irManager->setVerificationLevel(1);
#else
    irManager->setVerificationLevel(0);
#endif     
    cc->setLIRManager(irManager);
    
    MemoryManager  codeSelectorMemManager("CodeGenerator::selectCode.codeSelectorMemManager");
    MethodCodeSelector    codeSelector(sa, *ci, mm, codeSelectorMemManager, *irManager);

    inputProvider.selectCode(codeSelector);

    
    const char* logKind = "after";

    if (irdump.isEnabled()) {
        irManager->updateLoopInfo();
        irManager->updateLivenessInfo();
        Ia32::dumpIR(irManager, stageId, "IA32 LIR CFG after ", stageName, stageName, logKind, "opnds");
        Ia32::dumpIR(irManager, stageId, "IA32 LIR CFG after ", stageName, stageName, logKind);
        Log::printStageEnd(irdump.out(), stageId, "IA32", stageName, stageName);
    }

    if (dotdump.isEnabled()) {
        irManager->updateLoopInfo();
        irManager->updateLivenessInfo();
        Ia32::printDot(irManager, stageId, "IA32 LIR CFG after ", stageName, stageName,  logKind);
        Ia32::printDot(irManager, stageId, "IA32 LIR CFG after ", stageName, stageName, logKind, "liveness");
    }
}

}}; // namespace Ia32


