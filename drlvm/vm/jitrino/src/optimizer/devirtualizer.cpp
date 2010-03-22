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

#include "devirtualizer.h"
#include "Log.h"
#include "Dominator.h"
#include "inliner.h"
#include "EMInterface.h"

namespace Jitrino {

DEFINE_SESSION_ACTION(GuardedDevirtualizationPass, devirt, "Guarded Devirtualization of Virtual Calls")

void
GuardedDevirtualizationPass::_run(IRManager& irm) {
    computeDominators(irm);
    DominatorTree* dominatorTree = irm.getDominatorTree();
    Devirtualizer pass(irm, this);
    pass.guardCallsInRegion(irm, dominatorTree);
}

DEFINE_SESSION_ACTION(GuardRemovalPass, unguard, "Removal of Cold Guards")

void
GuardRemovalPass::_run(IRManager& irm) {
    Devirtualizer pass(irm, this);
    pass.unguardCallsInRegion(irm);
}

Devirtualizer::Devirtualizer(IRManager& irm, SessionAction* sa) 
: _hasProfileInfo(irm.getFlowGraph().hasEdgeProfile()), 
  _typeManager(irm.getTypeManager()), 
  _instFactory(irm.getInstFactory()),
  _opndManager (irm.getOpndManager()) {
    
    const OptimizerFlags& optFlags = irm.getOptimizerFlags();
    _doAggressiveGuardedDevirtualization = !_hasProfileInfo || optFlags.devirt_do_aggressive_guarded_devirtualization;
    _devirtSkipExceptionPath = optFlags.devirt_skip_exception_path;
    _devirtBlockHotnessMultiplier = optFlags.devirt_block_hotness_multiplier;
    _devirtSkipJLObjectMethods = optFlags.devirt_skip_object_methods;
    _devirtInterfaceCalls = sa ? sa->getBoolArg("devirt_intf_calls", false) : optFlags.devirt_intf_calls;
    _devirtVirtualCalls = sa ? sa->getBoolArg("devirt_virtual_calls", true) : true;
    _devirtAbstractCalls = sa ? sa->getBoolArg("devirt_abstract_calls", false) : false;
    _devirtUsingProfile = sa ? sa->getBoolArg("devirt_using_profile", false) : false;

    _directCallPercent = optFlags.unguard_dcall_percent;
    _directCallPercientOfEntry = optFlags.unguard_dcall_percent_of_entry;
}

bool
Devirtualizer::isGuardableVirtualCall(Inst* inst, MethodInst*& methodInst, Opnd*& base, Opnd*& tauNullChecked, Opnd *&tauTypesChecked, U_32 &argOffset, bool &isIntfCall)
{
    //
    // Returns true if this call site may be considered for guarded devirtualization
    //
    Opcode opcode = inst->getOpcode();
    if(opcode == Op_TauVirtualCall) { 
        // Virtual function call
        // We don't use this instruction any more - indirect calls are used instead
        assert(0);
        assert(inst->getNumSrcOperands() >= 3);
        MethodCallInst *methodCallInst = inst->asMethodCallInst();
        assert(methodCallInst != NULL);

        methodInst = methodCallInst;
        base = methodCallInst->getSrc(2);
        tauNullChecked = methodCallInst->getSrc(0);
        tauTypesChecked = methodCallInst->getSrc(1);
        argOffset = 2;

        assert(tauNullChecked->getType()->tag == Type::Tau);
        assert(tauTypesChecked->getType()->tag == Type::Tau);
    } else if(opcode == Op_IndirectMemoryCall) {
        // Indirect call - attempt to determine original virtual call
        CallInst* call = inst->asCallInst();
        assert(call != NULL);
        Opnd* funPtr = call->getFunPtr(); // = Src(0)
        methodInst = funPtr->getInst()->asMethodInst();
        if(methodInst == NULL)
            // Cannot resolve methodDesc.
            return false;

        assert(inst->getNumSrcOperands() >= 4);

        // LdFunAddr should have already converted to a direct call (Op_DirectCall)
        // by the simplifier.
        base = call->getSrc(3);
        tauNullChecked = call->getSrc(1);
        tauTypesChecked = call->getSrc(2);
        argOffset = 3;

        assert(methodInst != NULL && methodInst->getOpcode() == Op_TauLdVirtFunAddrSlot);
        assert(tauNullChecked->getType()->tag == Type::Tau);
        assert(tauTypesChecked->getType()->tag == Type::Tau);
 
        // Assert that this base matches the one from ldFunInst.
        Opcode methodOpcode = methodInst->getOpcode();
        Opnd *methodSrc0 = methodInst->getSrc(0);
        if ((methodOpcode == Op_TauLdVirtFunAddrSlot) ||
            (methodOpcode == Op_TauLdVirtFunAddr)) {
            Inst *vtableInst = methodSrc0->getInst();
            Opcode vtableInstOpcode = vtableInst->getOpcode();
            if (vtableInstOpcode == Op_TauLdVTableAddr) {
                isIntfCall = false;
            } else if (vtableInstOpcode == Op_TauLdIntfcVTableAddr) {
                isIntfCall = true;
            } else {
                // Need a real example when this assertion fires
                // This can be handled with copy propagation
                assert(0);
            }
        } else {
            assert(base == methodInst->getSrc(0));
        }
    } else {
        return false;
    }

    return true;
}

void
Devirtualizer::guardCallsInRegion(IRManager& regionIRM, DominatorTree* dtree) {
    // 
    // Perform guarded de-virtualization on calls in region
    //
    
    Log::out() << "Devirt params: " << std::endl;
    Log::out() << "  _doAggressiveGuardedDevirtualization: " << _doAggressiveGuardedDevirtualization << std::endl;
    Log::out() << "  _devirtSkipJLObjectMethods: " << _devirtSkipJLObjectMethods << std::endl;
    Log::out() << "  _devirtInterfaceCalls: " << _devirtInterfaceCalls << std::endl;
    Log::out() << "  _devirtVirtualCalls: " << _devirtVirtualCalls << std::endl;
    Log::out() << "  _devirtAbstractCalls: " << _devirtAbstractCalls << std::endl;
    Log::out() << "  _devirtUsingProfile: " << _devirtUsingProfile << std::endl;

    assert(dtree->isValid());
    StlDeque<DominatorNode *> dom_stack(regionIRM.getMemoryManager());

    dom_stack.push_back(dtree->getDominatorRoot());
    Node* node = NULL;
    DominatorNode *domNode = NULL;
    while (!dom_stack.empty()) {

        domNode = dom_stack.back();
        
        if (domNode) {
            
            node = domNode->getNode();
            guardCallsInBlock(regionIRM, node);
            
            // move to next sibling;
            dom_stack.back() = domNode->getSiblings();
            
            // but first deal with this node's children
            //
            // Skip exception control flow unless directed
            //
            // (because of this if we can't use walkers from walkers.h)
            if(node->isBlockNode() || !_devirtSkipExceptionPath)
                dom_stack.push_back(domNode->getChild()); 
        } else {
            dom_stack.pop_back(); // no children left, exit scope
        }
    }
}


void
Devirtualizer::genGuardedDirectCall(IRManager &regionIRM, Node* node, Inst* call, MethodDesc* methodDesc, ObjectType* objectType, Opnd *tauNullChecked, Opnd *tauTypesChecked, U_32 argOffset) {
    ControlFlowGraph &regionFG = regionIRM.getFlowGraph();
    assert(!methodDesc->isStatic());
    assert(call == node->getLastInst());
    uint16 bcOffset = call->getBCOffset();

    Log::out() << "Generating guarded direct call to " << objectType->getName()
        << "." << methodDesc->getName() << ::std::endl;
    Log::out() << "Guarded call bytecode size=" << (int) methodDesc->getByteCodeSize() << ::std::endl;

    //
    // Compute successors of call site
    //
    Node* next =  node->getUnconditionalEdge()->getTargetNode();
    Node* dispatch =  node->getExceptionEdge()->getTargetNode();

    //
    // Disconnect edges from call.
    //
    regionFG.removeEdge(node, next);
    regionFG.removeEdge(node, dispatch);
    call->unlink();

    //
    // Create nodes for guard, direct call, virtual call, and merge.
    //
    Node* guard = node;
    Node* directCallBlock = regionFG.createBlockNode(_instFactory.makeLabel());
    Node* virtualCallBlock = regionFG.createBlockNode(_instFactory.makeLabel());
    directCallBlock->getFirstInst()->setBCOffset(bcOffset);
    virtualCallBlock->getFirstInst()->setBCOffset(bcOffset);

    Node* merge = next;

    //
    // Reconnect graph with new nodes.  Connect to merge point later.
    //
    regionFG.addEdge(guard, directCallBlock)->setEdgeProb(1.0);
    regionFG.addEdge(guard, virtualCallBlock);
    regionFG.addEdge(directCallBlock, dispatch);
    regionFG.addEdge(virtualCallBlock, dispatch);
    directCallBlock->setExecCount(guard->getExecCount());

    //
    // Create direct call instruction
    //
    Opnd* dst = call->getDst(); 
    U_32 numArgs = call->getNumSrcOperands()-argOffset;
    U_32 i = 0;
    U_32 j = argOffset; // skip taus
    Opnd** args = new (regionIRM.getMemoryManager()) Opnd*[numArgs];
    for(; i < numArgs; ++i, ++j)
        args[i] = call->getSrc(j); // skip taus
    Inst* directCall = _instFactory.makeDirectCall(dst, 
                                                   tauNullChecked,
                                                   tauTypesChecked,
                                                   numArgs, args, methodDesc);
    directCall->setBCOffset(call->getBCOffset());
    directCallBlock->appendInst(directCall);

    //
    // Create virtual call
    //
    Inst* virtualCall = call;
    CallInst* icall = virtualCall->asCallInst();
    if(icall != NULL) {
        // Duplicate function pointer calculation in virtual call block.
        // Either the original call will be eliminated as dead code or 
        // this call will be eliminated by CSE.
        Opnd* funPtr = icall->getFunPtr();
        MethodInst* ldSlot = funPtr->getInst()->asMethodInst();
        assert(ldSlot != NULL && ldSlot->getOpcode() == Op_TauLdVirtFunAddrSlot);
        assert(ldSlot->getNumSrcOperands() == 2);
        Opnd* vtable = ldSlot->getSrc(0);
        Opnd* tauVtableHasDesc = ldSlot->getSrc(1);
        Opnd* funPtr2 = _opndManager.createSsaTmpOpnd(funPtr->getType());
        Inst* ldSlot2 = _instFactory.makeTauLdVirtFunAddrSlot(funPtr2, vtable,
                                                             tauVtableHasDesc,
                                                             ldSlot->getMethodDesc());
        virtualCallBlock->appendInst(ldSlot2);
        icall->setSrc(0, funPtr2);
        // Duplicate ldInterfaceVTable in virtual call block
        Inst* ldVTable = vtable->getInst();
        if (ldVTable->getOpcode() == Op_TauLdIntfcVTableAddr) {
            Opnd* slot = ldVTable->getSrc(0);
            Type* type = ldVTable->asTypeInst()->getTypeInfo();
            Opnd* vtable2 = _opndManager.createSsaTmpOpnd(vtable->getType());
            Inst* ldVTable2 = _instFactory.makeTauLdIntfcVTableAddr(vtable2, slot, type);
            ldVTable2->setBCOffset(ldVTable->getBCOffset());
            ldSlot2->setSrc(0, vtable2);
            ldVTable2->insertBefore(ldSlot2);
        }
    }
    virtualCallBlock->appendInst(virtualCall);

    //
    // Promote a return operand (if one exists) of the call from a ssa temp to a var.
    //
    if (!dst->isNull()) {
        Opnd* ssaTmp1 = _opndManager.createSsaTmpOpnd(dst->getType());  // returns value of direct call
        directCall->setDst(ssaTmp1);
        Opnd* ssaTmp2 = _opndManager.createSsaTmpOpnd(dst->getType());  // returns value of direct call
        virtualCall->setDst(ssaTmp2);
        
        VarOpnd* returnVar = _opndManager.createVarOpnd(dst->getType(), false);
        
        Node* directStVarBlock = regionFG.createBlockNode(_instFactory.makeLabel());
        directStVarBlock->getFirstInst()->setBCOffset(bcOffset);
        directStVarBlock->setExecCount(directCallBlock->getExecCount());
        regionFG.addEdge(directCallBlock, directStVarBlock)->setEdgeProb(1.0);
        Inst* stVar1 = _instFactory.makeStVar(returnVar, ssaTmp1);
        directStVarBlock->appendInst(stVar1);
        
        Node* virtualStVarBlock = regionFG.createBlockNode(_instFactory.makeLabel());
        virtualStVarBlock->getFirstInst()->setBCOffset(bcOffset);
        regionFG.addEdge(virtualCallBlock, virtualStVarBlock);
        Inst* stVar2 = _instFactory.makeStVar(returnVar, ssaTmp2);
        virtualStVarBlock->appendInst(stVar2);
        
        Inst* ldVar = _instFactory.makeLdVar(dst, returnVar);
        Inst* phi = NULL;

        if(regionIRM.getInSsa()) {
            Opnd* returnSsaVars[2];
            returnSsaVars[0] = _opndManager.createSsaVarOpnd(returnVar);  // returns value of direct call
            returnSsaVars[1] = _opndManager.createSsaVarOpnd(returnVar);  // returns value of virtual call
            Opnd* phiSsaVar = _opndManager.createSsaVarOpnd(returnVar);  // phi merge of the two above
            stVar1->setDst(returnSsaVars[0]);
            stVar2->setDst(returnSsaVars[1]);
            ldVar->setSrc(0, phiSsaVar);
            phi = _instFactory.makePhi(phiSsaVar, 2, returnSsaVars);
        }
        
        //
        // If successor has more than 1 in-edge, then we need a block for phi
        //
        bool needPhiBlock = (merge->getInDegree() > 0);
        if (needPhiBlock) {
            Node* phiBlock = regionFG.createBlockNode(_instFactory.makeLabel());
            phiBlock->getFirstInst()->setBCOffset(bcOffset);
            regionFG.addEdge(phiBlock, merge);
            regionFG.addEdge(directStVarBlock, phiBlock)->setEdgeProb(1.0);
            regionFG.addEdge(virtualStVarBlock, phiBlock);
            merge = phiBlock;
        } else {
            regionFG.addEdge(directStVarBlock, merge)->setEdgeProb(1.0);
            regionFG.addEdge(virtualStVarBlock, merge);
        }

        merge->prependInst(ldVar);
        if(phi != NULL) 
            merge->prependInst(phi);
    } else{
        // Connect calls directly to merge.
        regionFG.addEdge(directCallBlock, merge)->setEdgeProb(1.0);
        regionFG.addEdge(virtualCallBlock, merge);
    }

    //
    // Add the vtable compare (i.e., the type test) and branch in guard node
    //
    Opnd* base = directCall->getSrc(2); // skip over taus
    assert(base->getType()->isObject());
    assert(base->getType()->isInterface() || base->getType()->isAbstract() || (((ObjectType*) base->getType()) == objectType) || _devirtUsingProfile);
    Opnd* dynamicVTableAddr = _opndManager.createSsaTmpOpnd(_typeManager.getVTablePtrType(objectType));
    Opnd* staticVTableAddr = _opndManager.createSsaTmpOpnd(_typeManager.getVTablePtrType(objectType));
    guard->appendInst(_instFactory.makeTauLdVTableAddr(dynamicVTableAddr, base, tauNullChecked));
    guard->appendInst(_instFactory.makeGetVTableAddr(staticVTableAddr, objectType));
    guard->appendInst(_instFactory.makeBranch(Cmp_EQ, Type::VTablePtr, dynamicVTableAddr, staticVTableAddr, (LabelInst*)directCallBlock->getFirstInst()));
 }

bool
Devirtualizer::doGuard(IRManager& irm, Node* node, MethodDesc& methodDesc) {
    //
    // Determine if a call site should be guarded
    //

    if (_devirtSkipJLObjectMethods) {
        const char* className = methodDesc.getParentType()->getName();
        if (!strcmp(className, "java/lang/Object")) {
            Log::out() << "  Don't guard calls to java/lang/Object methods"<< std::endl;
            return false;
        }
    }

    if (_doAggressiveGuardedDevirtualization) {
        // 
        // In this mode, always guard in the first pass
        //
        Log::out() << "  Guarding calls to all methods aggressively"<< std::endl;
        return true;
    }

    //
    // Only devirtualize if there's profile information for this
    // node and the apparent target.
    //
    if(!_hasProfileInfo) {
        Log::out() << "  The node doesn't have profile info - don't guard"<< std::endl;
        return false;
    }

    double methodCount = irm.getFlowGraph().getEntryNode()->getExecCount();
    double blockCount = node->getExecCount();
    if (blockCount < (methodCount / _devirtBlockHotnessMultiplier)) {
        Log::out() << "  Too small block count - don't guard"<< std::endl;
        Log::out() << "  methodCount: " << methodCount << ", blockCount = " << blockCount
            << ", hotness factor: " << _devirtBlockHotnessMultiplier << std::endl;
        return false;
    }

    return true;
}

ObjectType *
Devirtualizer::getTopProfiledCalleeType(IRManager& regionIRM, MethodDesc *origMethodDesc, Inst *call) {
    assert(regionIRM.getCompilationInterface().isBCMapInfoRequired());
    CompilationContext* cc = regionIRM.getCompilationContext();
    MethodDesc& methDesc = regionIRM.getMethodDesc();

    ProfilingInterface* pi = cc->getProfilingInterface();
    // Don't devirtualize if there is no value profile
    if (!pi->hasMethodProfile(ProfileType_Value, methDesc)) return 0;
    ValueMethodProfile* mp = pi->getValueMethodProfile(regionIRM.getMemoryManager(), methDesc);
    if (Log::isLogEnabled(LogStream::DBG)) {
        mp->dumpValues(Log::out());
    }

    // Get bytecode offset of the call
    uint16 bcOffset = call->getBCOffset();
    assert(bcOffset != 0);
    assert(bcOffset != ILLEGAL_BC_MAPPING_VALUE);
    Log::out() << "Call instruction bcOffset = " << (I_32)bcOffset << std::endl;

    // Get profiled vtable value
    POINTER_SIZE_INT vtHandle = mp->getTopValue(bcOffset);
    if (vtHandle == 0) {
        // Do not devirtualize - there were no real calls here
        return 0;
    }

    // get desired MethodDesc object
    assert(vtHandle != 0);
    ObjectType* clssObjectType = _typeManager.getObjectType(VMInterface::getTypeHandleFromVTable((void*)vtHandle));
    return clssObjectType;
}

void
Devirtualizer::guardCallsInBlock(IRManager& regionIRM, Node* node) {

    //
    // Search node for guardian call
    //
    if(node->isBlockNode()) {
        Inst* last = (Inst*)node->getLastInst();
        MethodInst* methodInst = 0;
        Opnd* base = 0;
        Opnd* tauNullChecked = 0;
        Opnd* tauTypesChecked = 0;
        U_32 argOffset = 0;
        bool isIntfCall = false;
        if(isGuardableVirtualCall(last, methodInst, base, tauNullChecked, tauTypesChecked, argOffset, isIntfCall)) {
            assert(methodInst && base && tauNullChecked && tauTypesChecked && argOffset);
            Type* type = base->getType();
            if (type->isNullObject()) { //NullObject type is not ObjectType instance, but Type instance
                return;
            }
            if (type->isUnresolvedType()) {
                return;
            }
            ObjectType* baseType = type->asObjectType();
            assert(baseType!=NULL);
            
            
            ObjectType* devirtType = NULL;
            if (! ((_devirtInterfaceCalls && isIntfCall) || (_devirtVirtualCalls && !isIntfCall) ||
                   (baseType->isAbstract() && _devirtAbstractCalls))) {
                return;
            }
            // If base type is concrete, consider an explicit guarded test against it
            if((_devirtInterfaceCalls && isIntfCall) || !baseType->isAbstract() || baseType->isArray() || (baseType->isAbstract() && _devirtAbstractCalls)) {
                MethodDesc* origMethodDesc = methodInst->getMethodDesc();
                MethodDesc* candidateMeth = NULL;

                if (_devirtUsingProfile || baseType->isInterface() || baseType->isAbstract()) {

                    MethodDesc& methDesc = regionIRM.getMethodDesc();
                    Log::out() << std::endl << "Devirtualizing interface/abstract call in the method :" << std::endl << "\t";
                    methDesc.printFullName(Log::out());
                    Log::out() << std::endl << "call to the method: " << std::endl << "\t";
                    origMethodDesc->printFullName(Log::out());
                    Log::out() << std::endl << "from the CFG node: " << node->getId() <<
                        ", node exec count: " << node->getExecCount() << std::endl;

                    ObjectType* clssObjectType = getTopProfiledCalleeType(regionIRM, origMethodDesc, last);
                    if(clssObjectType == 0) {
                        return;
                    }
                    Log::out() << "Valued type: ";
                    clssObjectType->print(Log::out());
                    Log::out() << std::endl;
                    candidateMeth = regionIRM.getCompilationInterface().resolveMethod(clssObjectType, origMethodDesc->getName(), origMethodDesc->getSignatureString());
                    Log::out() << "candidateMeth: "<< std::endl;
                    candidateMeth->printFullName(Log::out());
                    Log::out() << std::endl;
 
                    devirtType = clssObjectType;

                } else {
                    NamedType* methodType = origMethodDesc->getParentType();
                    if (_typeManager.isSubClassOf(baseType, methodType)) {
                        candidateMeth = regionIRM.getCompilationInterface().getOverridingMethod(baseType, origMethodDesc);
                        if (candidateMeth) {
                            jitrino_assert(origMethodDesc->getParentType()->isClass());
                            methodInst->setMethodDesc(candidateMeth);
                            devirtType = baseType;
                        }
                   }
                }
                if (candidateMeth) {
                    //
                    // Try to guard this call
                    //
                    assert(devirtType);
                    if(doGuard(regionIRM, node, *candidateMeth )) {
                        Log::out() << "Guard call to " << baseType->getName() << "::" << candidateMeth->getName() << std::endl;
                        genGuardedDirectCall(regionIRM, node, last, candidateMeth, devirtType, tauNullChecked, tauTypesChecked, argOffset);
                        Log::out() << "Done guarding call to " << baseType->getName() << "::" << candidateMeth->getName() << std::endl;
                    } else {
                        Log::out() << "Don't guard call to " << baseType->getName() << "::" << origMethodDesc->getName() << std::endl;
                    }
                }
            }
        }
    }
}

void
Devirtualizer::unguardCallsInRegion(IRManager& regionIRM) {
    ControlFlowGraph &regionFG = regionIRM.getFlowGraph();
    if(!regionFG.hasEdgeProfile()) {
        if (Log::isEnabled()) {
            Log::out()<<"No edge profile, skipping unguard pass"<<std::endl;
        }
        return;
    }

    //
    // Search for previously guarded virtual calls
    //
    MemoryManager mm("Devirtualizer::unguardCallsInRegion.mm");
    Nodes nodes(mm);
    regionFG.getNodesPostOrder(nodes);
    StlVector<Node*>::reverse_iterator i;
    for(i = nodes.rbegin(); i != nodes.rend(); ++i) {
        Node* node = *i;
        Inst* last = (Inst*)node->getLastInst();
        if(last->isBranch()) {
            //
            // Check if branch is a guard
            //
            assert(last->getOpcode() == Op_Branch);
            BranchInst* branch = last->asBranchInst();
            Node* dCallNode =  node->getTrueEdge()->getTargetNode(); 
            Node* vCallNode =  node->getFalseEdge()->getTargetNode(); 

            if(branch->getComparisonModifier() != Cmp_EQ)
                continue;
            if(branch->getNumSrcOperands() != 2)
                continue;
            Opnd* src0 = branch->getSrc(0);
            Opnd* src1 = branch->getSrc(1);
            if(!src0->getType()->isVTablePtr() || !src1->getType()->isVTablePtr())
                continue;
            if(src0->getInst()->getOpcode() != Op_TauLdVTableAddr || src1->getInst()->getOpcode() != Op_GetVTableAddr)
                continue;

            Inst* ldvfnslot = (Inst*)vCallNode->getSecondInst();
            Inst* callimem = ldvfnslot->getNextInst();
            if(ldvfnslot->getOpcode() != Op_TauLdVirtFunAddrSlot || callimem->getOpcode() != Op_IndirectMemoryCall)
                continue;

            //
            // A guard - fold based on profile results.
            //
            
            bool fold = dCallNode->getExecCount() < (node->getExecCount() * _directCallPercent / 100);
            fold = fold || (dCallNode->getExecCount()  < (regionFG.getEntryNode()->getExecCount() * _directCallPercientOfEntry / 100));
            
            if(fold) {
                //
                // A compile time is compared.  Later simplification will fold branch appropriately.
                //
                if (Log::isEnabled()) {
                    Log::out()<<"Unguarding: instId="<<last->getId()<<std::endl;
                }
               
                branch->setSrc(1, src0);
                branch->setComparisonModifier(Cmp_NE_Un);

                //regionFG.removeEdge(node->getTrueEdge());
               // branch->unlink();
            }
        }
    }
}

} //namespace Jitrino 
