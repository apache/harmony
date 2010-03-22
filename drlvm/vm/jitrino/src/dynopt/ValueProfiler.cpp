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
/* COPYRIGHT_NOTICE */

/**
* @author Pavel Ozhdikhin
*/


#include "Jitrino.h"
#include "optpass.h"
#include "devirtualizer.h"
#include "irmanager.h"
#include "Inst.h"
#include "FlowGraph.h"
#include "EMInterface.h"

namespace Jitrino {

DEFINE_SESSION_ACTION(ValueProfilerInstrumentationPass, vp_instrument, "Perform value profiler instrumentation pass")

void ValueProfilerInstrumentationPass::_run(IRManager& irm)
{
    // Currently value profile is used by interface devirtualization only

    ControlFlowGraph& flowGraph = irm.getFlowGraph();
    MemoryManager mm("Value Profiler Instrumentation Pass");
    MethodDesc& md = irm.getMethodDesc();
    InstFactory& instFactory = irm.getInstFactory();
    OpndManager& opndManager = irm.getOpndManager();
    TypeManager& typeManager = irm.getTypeManager();
    StlVector<U_32> counterKeys(mm);
    bool debug = Log::isEnabled();
    bool profileAbstractCalls =  getBoolArg("profile_abstract", false);
    bool profileAllVirtualCalls =  getBoolArg("profile_all_virtual", false);

    U_32 key = 0;

    StlVector<Node*> nodes(mm);
    flowGraph.getNodesPostOrder(nodes); 
    for (StlVector<Node*>::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if(node->isBlockNode()) {
            Inst* lastInst = (Inst*)node->getLastInst();
            MethodInst* methodInst = NULL;
            Opnd* base = NULL;
            Opnd* tauNullChecked = NULL;
            Opnd* tauTypesChecked = NULL;
            U_32 argOffset = 0;
            bool isIntfCall = false;
            if(Devirtualizer::isGuardableVirtualCall(lastInst, methodInst, base, tauNullChecked, tauTypesChecked, argOffset, isIntfCall)) {
                assert(methodInst && base && tauNullChecked && tauTypesChecked && argOffset);
                assert(base->getType()->isObject());

                CallInst* call = lastInst->asCallInst();
                assert(call != NULL);
                
                if (debug) {
                    Log::out() << "Indirect call detected. \n\tNode: ";
                    FlowGraph::printLabel(Log::out(), node);
                    Log::out() << "\n\tCall inst: ";
                    call->print(Log::out());
                    Log::out() << std::endl;
                }

                Inst* vtableInst = methodInst->getSrc(0)->getInst();
                if (isIntfCall) {
                    // Need to generate VTable loading
                    Opnd* vTable = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(typeManager.getSystemObjectType()));
                    Inst* ldVtableInst = instFactory.makeTauLdVTableAddr(vTable, base, tauNullChecked);
                    ((CFGInst *)ldVtableInst)->insertBefore(vtableInst);
                    vtableInst = ldVtableInst;
                    if (debug) {
                        Log::out() << "\tInserted ldVTable instruction to instrument: ";
                        ldVtableInst->print(Log::out());
                        Log::out() << std::endl;
                    }
                } else {
                    // Profile abstract calls, or all virtual calls
                    ObjectType* baseType = (ObjectType*) base->getType();
                    if(!((baseType->isAbstract() && profileAbstractCalls) || profileAllVirtualCalls)) {
                        continue;
                    }
                }
                key = (U_32)lastInst->getBCOffset();
                assert(key != ILLEGAL_BC_MAPPING_VALUE);
                assert(key != 0);
                if (debug) {
                    Log::out() << "Use call instruction bcOffset = " << (I_32)key << std::endl;
                }

                Opnd* indexOpnd = opndManager.createSsaTmpOpnd(typeManager.getInt32Type());
                Inst* loadIndexInst = instFactory.makeLdConst(indexOpnd, (I_32)key);
                counterKeys.push_back(key);
                Opnd* valueOpnd = vtableInst->getDst();
                const U_32 numArgs = 2;
                Opnd* args[numArgs] = {indexOpnd, valueOpnd};
                Inst* addValueInst = instFactory.makeJitHelperCall(opndManager.getNullOpnd(), AddValueProfileValue, NULL, NULL, numArgs, args);
                addValueInst->setBCOffset(call->getBCOffset());
                ((CFGInst *)addValueInst)->insertBefore(call);
                ((CFGInst *)loadIndexInst)->insertBefore(addValueInst);
            }
        }
    }

    U_32 cc_size = (U_32)counterKeys.size();
    if (cc_size == 0) return;

    irm.getCompilationInterface().lockMethodData();
    
    ProfilingInterface* pi = irm.getProfilingInterface();
    if (!pi->hasMethodProfile(ProfileType_Value, md, JITProfilingRole_GEN)) {
        pi->createValueMethodProfile(mm , md,  cc_size,  (U_32*)&counterKeys.front());
    }

    irm.getCompilationInterface().unlockMethodData();

    if (debug) {
        Log::out() << std::endl << "ValuePC:: instrumented, nCounters = " << cc_size << std::endl;
    }

}

} //namespace
