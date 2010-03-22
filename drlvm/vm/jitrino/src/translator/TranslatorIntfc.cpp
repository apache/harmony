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
 * @author Intel, George A. Timoshenko
 *
 */

#include <assert.h>

#include "MemoryManager.h"
#include "TranslatorIntfc.h"
#include "JavaTranslator.h"
#include "irmanager.h"
#include "IRBuilder.h"
#include "simplifier.h"
#include "Log.h"
#include "methodtable.h"
#include "CompilationContext.h"
#include "FlowGraph.h"
#include "Jitrino.h"

namespace Jitrino {


void TranslatorSession::run () {
    TranslatorAction* action = (TranslatorAction*)getAction();
    flags = action->getFlags();
#ifdef _DEBUG
/*
    TODO: to avoid recursive compilation with OPT we need to finish this task
    1) Fix lazy exceptions opt
    2) Ia32CodeEmmitter forces creation of interned strings during compilation -> fix it.
    if (flags.assertOnRecursion) {
        int rec = Jitrino::getCompilationRecursionLevel();
        assert( rec == 1);
    }
*/
#endif
    translate();
    postTranslatorCleanup();
}

// this is the regular routine to be used to generate IR for a method
void TranslatorSession::translate() {
    CompilationContext* cc = getCompilationContext();
    IRManager* irm = cc->getHIRManager();
    assert(irm);
    irm->getTypeManager().setLazyResolutionMode(flags.lazyResolution);
    MethodDesc& methodDesc = irm->getMethodDesc();
    //create IRBuilder
    MemoryManager& mm = cc->getCompilationLevelMemoryManager();
    TranslatorAction* myAction = (TranslatorAction*)getAction();
    IRBuilder* irb = (IRBuilder*)myAction->getIRBuilderAction()->createSession(mm);
    irb->setCompilationContext(cc);
    MemoryManager tmpMM("IRBuilder::tmpMM");
    irb->init(irm, &flags, tmpMM);
    JavaTranslator::translateMethod(*cc->getVMCompilationInterface(), methodDesc, *irb);
}


void TranslatorSession::postTranslatorCleanup() {
    IRManager* irm = getCompilationContext()->getHIRManager();
    ControlFlowGraph& flowGraph = irm->getFlowGraph();
    MethodDesc& methodDesc = irm->getMethodDesc();
    if (Log::isEnabled()) {
        Log::out() << "PRINTING LOG: After Translator" << std::endl;
        FlowGraph::printHIR(Log::out(), flowGraph, methodDesc);
    }
    FlowGraph::doTranslatorCleanupPhase(*irm);
    if (Log::isEnabled()) {
        Log::out() << "PRINTING LOG: After Cleanup" << std::endl;
        FlowGraph::printHIR(Log::out(), flowGraph,  methodDesc);
    }
}


static const char* help = \
    "  propValues[={ON|off}]    -  propagate values during translation\n"\
    "  balancedSync[={on|OFF}] - treat all synchronization as balanced\n"\
    "  ignoreSync[={on|OFF}]   - do not generate synchronization\n"\
    "  syncAsEnterFence[={on|OFF}] - implement synchronization as monitor enter fence\n"\
    "  genMinMaxAbs[={on|OFF}]   - use special opcodes for Min/Max/Abs\n"\
    "  genFMinMaxAbs[={on|OFF}]  - also use opcodes for float Min/Max/Abs\n";




static ActionFactory<TranslatorSession, TranslatorAction> _translator("translator", help);

void TranslatorAction::init() {
    readFlags();
    MemoryManager& mm = getJITInstanceContext().getGlobalMemoryManager();
    irbAction = (IRBuilderAction*)createAuxAction(mm, IRBUILDER_ACTION_NAME, "irbuilder");
    irbAction->init();
}    

void TranslatorAction::readFlags() {
    flags.propValues = getBoolArg("propValues", true);
#if defined(_IPF_)
    flags.optArrayInit = getBoolArg("optArrayInit", false);
#else
    flags.optArrayInit = getBoolArg("optArrayInit", true);
#endif
    flags.onlyBalancedSync = getBoolArg("balancedSync", false);

    flags.ignoreSync       = getBoolArg("ignoreSync",false);
    flags.syncAsEnterFence = getBoolArg("syncAsEnterFence",false);
    
    flags.genMinMaxAbs = getBoolArg("genMinMaxAbs", false);
    flags.genFMinMaxAbs = getBoolArg("genFMinMaxAbs", false);

#ifndef _IPF_ 
    bool defaultIsLazy = true;
#else
    bool defaultIsLazy = false;
#endif
    flags.lazyResolution = getBoolArg("lazyResolution", defaultIsLazy);
    flags.assertOnRecursion = getBoolArg("assertOnRecursion", flags.lazyResolution);
}


OpndStack::OpndStack(MemoryManager& memManager,U_32 slots) 
    : maxSlots(slots) 
{
    opnds = new (memManager) Opnd*[maxSlots];
    tos = 0;
}


enum {
    IsNonNull      = 0x02,
    IsExactType    = 0x04,
    StackOpndAlive = 0x10,  // to get rid of phi nodes in the translator
    StackOpndSaved = 0x20   // to get rid of phi nodes in the translator
};
static bool isNonNull(U_32 flags)   {
    return (flags & IsNonNull) != 0; 
}
static bool isExactType(U_32 flags) {
    return (flags & IsExactType) != 0; 
}
static U_32 setNonNull(U_32 flags,bool val) { 
    return (val ? (flags | IsNonNull) : (flags & ~IsNonNull));
}
static U_32 setExactType(U_32 flags,bool val){ 
    return (val ? (flags | IsExactType) : (flags & ~IsExactType));
}
static bool isStackOpndAlive(U_32 flags) {
    return (flags & StackOpndAlive) != 0;
}
static bool isStackOpndSaved(U_32 flags) {
    return (flags & StackOpndSaved) != 0;
}
static U_32 setStackOpndAlive(U_32 flags,bool val) {
    return (val ? (flags | StackOpndAlive) : (flags & ~StackOpndAlive));
}

static U_32 setStackOpndSaved(U_32 flags,bool val) {
    return (val ? (flags | StackOpndSaved) : (flags & ~StackOpndSaved));
}


//
// utility methods to allow refactoring of Opnd.h
bool 
isNonNullOpnd(Opnd* opnd) {
    if (opnd->isVarOpnd()) {
        //
        // use the properties in Opnd
        //
        return isNonNull(opnd->getProperties());
    }
    return (Simplifier::isNonNullObject(opnd) ||
            Simplifier::isNonNullParameter(opnd));
}

bool 
isExactTypeOpnd(Opnd* opnd) {
    if (opnd->isVarOpnd()) {
        //
        // use the properties in Opnd
        //
        return isExactType(opnd->getProperties());
    }
    return Simplifier::isExactType(opnd);
}

bool
isStackOpndAliveOpnd(Opnd* opnd) {
    return isStackOpndAlive(opnd->getProperties());
}

bool
isStackOpndSavedOpnd(Opnd* opnd) {
    return isStackOpndSaved(opnd->getProperties());
}

void
setNonNullOpnd(Opnd* opnd,bool val) {
    if (opnd->isVarOpnd()) {
        //
        // use the properties in Opnd
        //
        U_32 props = opnd->getProperties();
        opnd->setProperties(setNonNull(props,val));
        return;
    }
}

void
setExactTypeOpnd(Opnd* opnd,bool val) {
    if (opnd->isVarOpnd()) {
        //
        // use the properties in Opnd
        //
        U_32 props = opnd->getProperties();
        opnd->setProperties(setExactType(props,val));
        return;
    }
}

void
setStackOpndAliveOpnd(Opnd* opnd,bool val) {
    //
    // use the properties in Opnd
    //
    U_32 props = opnd->getProperties();
    opnd->setProperties(setStackOpndAlive(props,val));
    return;
}

void
setStackOpndSavedOpnd(Opnd* opnd,bool val) {
    //
    // use the properties in Opnd
    //
    U_32 props = opnd->getProperties();
    opnd->setProperties(setStackOpndSaved(props,val));
    return;
}

} //namespace Jitrino 
