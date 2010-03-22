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
* @author Intel, Mikhail Y. Fursov
*/

#include "PMFAction.h"
#include "optpass.h"
#include "inliner.h"
#include "LoopTree.h"
#include "Dominator.h"
#include "StlPriorityQueue.h"
#include "VMMagic.h"
#include "FlowGraph.h"

namespace Jitrino {

class HelperConfig {
public:
    HelperConfig(VM_RT_SUPPORT id) : helperId(id), doInlining(true), hotnessPercentToInline(0) {}
    VM_RT_SUPPORT helperId;
    bool          doInlining;
    U_32        hotnessPercentToInline;
};    

struct HelperInlinerFlags {
    HelperInlinerFlags(MemoryManager& mm) 
        : inlinerPipelineName(NULL), pragmaInlineType(NULL), opcodeToHelperMapping(mm), helperConfigs(mm){}

    const char* inlinerPipelineName;
    Type* pragmaInlineType;

    StlMap<Opcode, VM_RT_SUPPORT> opcodeToHelperMapping;
    StlMap<VM_RT_SUPPORT, HelperConfig*> helperConfigs;
};

class HelperInlinerAction: public Action {
public:
    HelperInlinerAction() : flags(Jitrino::getGlobalMM()) {}
    void init();
    void registerHelper(Opcode opcode, VM_RT_SUPPORT helperId);
    HelperInlinerFlags& getFlags() {return flags;}
protected:
    HelperInlinerFlags flags;
};

DEFINE_SESSION_ACTION_WITH_ACTION(HelperInlinerSession, HelperInlinerAction, inline_helpers, "VM helpers inlining");

void HelperInlinerAction::init() {
    flags.inlinerPipelineName = getStringArg("pipeline", "inliner_pipeline");
    
    registerHelper(Op_NewObj, VM_RT_NEW_RESOLVED_USING_VTABLE_AND_SIZE);
    registerHelper(Op_NewArray, VM_RT_NEW_VECTOR_USING_VTABLE);
    registerHelper(Op_TauMonitorEnter, VM_RT_MONITOR_ENTER);
    registerHelper(Op_TauMonitorExit, VM_RT_MONITOR_EXIT);
    registerHelper(Op_TauStRef, VM_RT_GC_HEAP_WRITE_REF);
    registerHelper(Op_TauLdIntfcVTableAddr, VM_RT_GET_INTERFACE_VTABLE_VER0);
    registerHelper(Op_TauCheckCast, VM_RT_CHECKCAST);
    registerHelper(Op_TauInstanceOf, VM_RT_INSTANCEOF);
    registerHelper(Op_IdentHC, VM_RT_GET_IDENTITY_HASHCODE);
}

void HelperInlinerAction::registerHelper(Opcode opcode, VM_RT_SUPPORT helperId) {
    MemoryManager& globalMM = getJITInstanceContext().getGlobalMemoryManager();
    assert(flags.opcodeToHelperMapping.find(opcode)==flags.opcodeToHelperMapping.end()); 
    flags.opcodeToHelperMapping[opcode] = helperId; 
    if (flags.helperConfigs.find(helperId)== flags.helperConfigs.end()) {
        std::string helperName = CompilationInterface::getRuntimeHelperName(helperId);
        HelperConfig* h = new (globalMM) HelperConfig(helperId);
        h->doInlining = getBoolArg(helperName.c_str(), false);
        h->hotnessPercentToInline = getIntArg((helperName + "_hotnessPercent").c_str(), 0);
        flags.helperConfigs[helperId] = h; 
    }
}

class HelperInliner {
public:
    HelperInliner(HelperInlinerSession* _sessionAction, MemoryManager& tmpMM, CompilationContext* _cc, Inst* _inst, 
        U_32 _hotness, MethodDesc* _helperMethod, VM_RT_SUPPORT _helperId)  
        : flags(((HelperInlinerAction*)_sessionAction->getAction())->getFlags()), localMM(tmpMM), 
        cc(_cc), inst(_inst), session(_sessionAction), method(_helperMethod),  helperId(_helperId)
    {
        hotness=_hotness;
        irm = cc->getHIRManager();
        instFactory = &irm->getInstFactory();
        opndManager = &irm->getOpndManager();
        typeManager = &irm->getTypeManager();
        cfg = &irm->getFlowGraph();
    }

    ~HelperInliner(){};

    void run();

    U_32 hotness;

    static MethodDesc* findHelperMethod(CompilationInterface* ci, VM_RT_SUPPORT helperId);

protected:
    void inlineVMHelper(MethodCallInst* call);
    void finalizeCall(MethodCallInst* call);
    void fixInlineInfo(MethodCallInst* callInst);

    HelperInlinerFlags& flags;
    MemoryManager& localMM;
    CompilationContext* cc;
    Inst* inst;
    HelperInlinerSession* session;
    MethodDesc*  method;
    VM_RT_SUPPORT helperId;

    //cache these values for convenience of use
    IRManager* irm;
    InstFactory* instFactory;
    OpndManager* opndManager;
    TypeManager* typeManager;
    ControlFlowGraph* cfg;
};

class HelperInlinerCompare {
public:
    bool operator()(const HelperInliner* hi1, const HelperInliner* hi2) { return hi1->hotness < hi2->hotness; }
};

void HelperInlinerSession::_run(IRManager& irm) {
    CompilationContext* cc = getCompilationContext();
    MemoryManager tmpMM("Inline VM helpers");
    HelperInlinerAction* action = (HelperInlinerAction*)getAction();
    HelperInlinerFlags& flags = action->getFlags();

    if (flags.pragmaInlineType== NULL) {
        // Avoid class resolution during compilation. VMMagic package should be loaded & resolved at start up.
        flags.pragmaInlineType= cc->getVMCompilationInterface()->findClassUsingBootstrapClassloader(PRAGMA_INLINE_TYPE_NAME);
        if (flags.pragmaInlineType == NULL) {
            Log::out()<<"Helpers inline pass failed! class not found: "<<PRAGMA_INLINE_TYPE_NAME<<std::endl;
            return;
        }
    }

    //finding all helper calls
    ControlFlowGraph& fg = irm.getFlowGraph();
    double entryExecCount = fg.hasEdgeProfile() ? fg.getEntryNode()->getExecCount(): 1;
    U_32 maxNodeCount = irm.getOptimizerFlags().hir_node_threshold;
    StlPriorityQueue<HelperInliner*, StlVector<HelperInliner*>, HelperInlinerCompare> *helperInlineCandidates = 
        new (tmpMM) StlPriorityQueue<HelperInliner*, StlVector<HelperInliner*>, HelperInlinerCompare>(tmpMM);

    const StlMap<Opcode, VM_RT_SUPPORT>& opcodeToHelper = flags.opcodeToHelperMapping;
    const StlMap<VM_RT_SUPPORT, HelperConfig*>& configs = flags.helperConfigs;

    const Nodes& nodes = fg.getNodesPostOrder();//process checking only reachable nodes.
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        double execCount = node->getExecCount();
        assert (execCount >= 0);
        U_32 nodePercent = fg.hasEdgeProfile() ? (U_32)(execCount*100/entryExecCount) : 0;
        if (node->isBlockNode()) {
            for (Inst* inst = (Inst*)node->getFirstInst(); inst!=NULL; inst = inst->getNextInst()) {
                Opcode opcode = inst->getOpcode();
                StlMap<Opcode, VM_RT_SUPPORT>::const_iterator o2h = opcodeToHelper.find(opcode);
                if (o2h == opcodeToHelper.end()) {
                    continue;
                }
                VM_RT_SUPPORT helperId = o2h->second;
                StlMap<VM_RT_SUPPORT, HelperConfig*>::const_iterator iconf = configs.find(helperId);
                if (iconf == configs.end()) {
                    continue;
                }
                HelperConfig* config = iconf->second;
                if (!config->doInlining || nodePercent < config->hotnessPercentToInline) {
                    continue;
                }
                MethodDesc* md = HelperInliner::findHelperMethod(cc->getVMCompilationInterface(), helperId);
                if (md == NULL) {
                    continue;
                }
                HelperInliner* inliner = new (tmpMM) HelperInliner(this, tmpMM, cc, inst, nodePercent, md, helperId);
                
                helperInlineCandidates->push(inliner);
            }
        }
    }

    //running all inliners
    while(!helperInlineCandidates->empty() && (fg.getNodeCount() < maxNodeCount)) {
        HelperInliner* inliner = helperInlineCandidates->top();
        inliner->run();
        helperInlineCandidates->pop();
    }
}

void HelperInliner::run()  {
    if (Log::isEnabled())  {
        Log::out() << "Processing inst:"; inst->print(Log::out()); Log::out()<<std::endl; 
    }
    assert(method);

    //Convert all inst params into helper params
    U_32 numHelperArgs = method->getNumParams();
    U_32 numInstArgs = inst->getNumSrcOperands();
    Opnd** helperArgs = new (irm->getMemoryManager()) Opnd*[numHelperArgs];
#ifdef _DEBUG
    std::fill(helperArgs, helperArgs + numHelperArgs, (Opnd*)NULL);
#endif
    U_32 currentHelperArg = 0;
    if (inst->isType()) {
        Type* type = inst->asTypeInst()->getTypeInfo();
        assert(type->isNamedType());
        Opnd* typeOpnd = opndManager->createSsaTmpOpnd(typeManager->getUnmanagedPtrType(typeManager->getInt8Type()));
        Inst* ldconst = instFactory->makeLdConst(typeOpnd, (POINTER_SIZE_SINT)type->asNamedType()->getVMTypeHandle());
        ldconst->insertBefore(inst);
        helperArgs[currentHelperArg] = typeOpnd;
        currentHelperArg++;
    } else if (inst->isToken()) {
        TokenInst* tokenInst = inst->asTokenInst();
        MethodDesc* methDesc = tokenInst->getEnclosingMethod();
        U_32 cpIndex = tokenInst->getToken();
        Opnd* classHandleOpnd = opndManager->createSsaTmpOpnd(typeManager->getUnmanagedPtrType(typeManager->getIntPtrType()));
        Opnd* cpIndexOpnd = opndManager->createSsaTmpOpnd(typeManager->getUInt32Type());
        Inst* ldMethDesc = instFactory->makeLdConst(classHandleOpnd, (POINTER_SIZE_SINT)methDesc->getParentHandle());
        Inst* ldCpIndex = instFactory->makeLdConst(cpIndexOpnd, (I_32)cpIndex);
        
        ldMethDesc->insertBefore(inst);
        helperArgs[currentHelperArg] = classHandleOpnd;
        currentHelperArg++;
        
        ldCpIndex->insertBefore(inst);
         helperArgs[currentHelperArg] = cpIndexOpnd;
        currentHelperArg++;                
    }
    
    for (U_32 i = 0; i < numInstArgs; i++) {
        Opnd* instArg = inst->getSrc(i);
        if (instArg->getType()->tag == Type::Tau) {
            continue;
        }
        assert(currentHelperArg < numHelperArgs);
        Type* helperArgType = method->getParamType(currentHelperArg);
        Type* instArgType = instArg->getType();
        assert(instArgType->isNumeric() == helperArgType->isNumeric());

        bool needObjToMagicConversion = (instArgType->isObject() || instArgType->isManagedPtr()) 
                && helperArgType->isNamedType() && VMMagicUtils::isVMMagicClass(helperArgType->asNamedType()->getName());

        Opnd* helperArg = instArg;
        if (needObjToMagicConversion) {
            Type* dstType = typeManager->getUnmanagedPtrType(typeManager->getInt8Type()); //TODO: use convertVMMagicType2HIR here from translator
            helperArg = opndManager->createSsaTmpOpnd(dstType);
            Modifier mod = Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No);
            instFactory->makeConvUnmanaged(mod, dstType->tag, helperArg, instArg)->insertBefore(inst);
        }
        helperArgs[currentHelperArg] = helperArg;
        currentHelperArg++;
    }
    assert(helperArgs[numHelperArgs-1]!=NULL);


    //Prepare res opnd
    Opnd* instResOpnd = inst->getDst();
    Type* helperRetType = method->getReturnType();
    Type* instResType = instResOpnd->getType();
    bool resIsTau = instResType && Type::isTau(instResType->tag);
    if (resIsTau) {
        assert(helperRetType->isVoid());
        instResType = NULL;
        instResOpnd = opndManager->getNullOpnd();
    }
    bool needResConv = instResType && instResType->isObject() && helperRetType->isObject() && VMMagicUtils::isVMMagicClass(helperRetType->asObjectType()->getName());
    Opnd* helperResOpnd = !needResConv ? instResOpnd : opndManager->createSsaTmpOpnd(typeManager->getUnmanagedPtrType(typeManager->getInt8Type()));
    
    //Make a call inst
    Opnd* tauSafeOpnd = opndManager->createSsaTmpOpnd(typeManager->getTauType());
    instFactory->makeTauSafe(tauSafeOpnd)->insertBefore(inst);
    MethodCallInst* call = instFactory->makeDirectCall(helperResOpnd, tauSafeOpnd, tauSafeOpnd, numHelperArgs, helperArgs, method)->asMethodCallInst();
    assert(inst->getBCOffset()!=ILLEGAL_BC_MAPPING_VALUE);
    call->setBCOffset(inst->getBCOffset());
    call->insertBefore(inst);
    inst->unlink();

    finalizeCall(call); //make call last inst in a block

    if (needResConv || resIsTau) {
        //convert address type to managed object type
        Edge* fallEdge = call->getNode()->getUnconditionalEdge();
        assert(fallEdge && fallEdge->getTargetNode()->isBlockNode());
        Node* fallNode = fallEdge->getTargetNode();
        if (fallNode->getInDegree()>1) {
            fallNode = irm->getFlowGraph().spliceBlockOnEdge(fallEdge, instFactory->makeLabel());
        }
        if (needResConv) {
            Modifier mod = Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No);
            fallNode->prependInst(instFactory->makeConvUnmanaged(mod, Type::Object, instResOpnd, helperResOpnd));
        } else {
            assert(resIsTau);
            Opnd* tauSafeOpnd2 = opndManager->createSsaTmpOpnd(typeManager->getTauType());
            Inst* makeTau = instFactory->makeTauSafe(tauSafeOpnd2);
            fallNode->prependInst(makeTau);
            instFactory->makeCopy(inst->getDst(), tauSafeOpnd2)->insertAfter(makeTau);
            
        }
    }

    //Inline the call
    inlineVMHelper(call);
}

MethodDesc* HelperInliner::findHelperMethod(CompilationInterface* ci, VM_RT_SUPPORT helperId) 
{
    MethodDesc* md = ci->getMagicHelper(helperId);
    if (md == NULL) {
        if (Log::isEnabled()) Log::out()<<"WARN: helper's method is not resolved:"<<CompilationInterface::getRuntimeHelperName(helperId)<<std::endl;
        return NULL;
    }
    
    Class_Handle ch = md->getParentHandle();
    if (!VMInterface::isInitialized(ch)) {
        if (Log::isEnabled()) Log::out()<<"WARN: class is not initialized:"<<VMInterface::getTypeName(ch)<<std::endl;
        return NULL;
    }
    
    return md;
}

void HelperInliner::inlineVMHelper(MethodCallInst* call) {
    if (Log::isEnabled()) {
        Log::out()<<"Inlining VMHelper:";call->print(Log::out());Log::out()<<std::endl;
    }

    Inliner inliner(session, localMM, *irm, false, false, flags.inlinerPipelineName);
    inliner.runInliner(call);
}

void HelperInliner::finalizeCall(MethodCallInst* callInst) {
    //if call is not last inst -> make it last inst
    if (callInst != callInst->getNode()->getLastInst()) {
        cfg->splitNodeAtInstruction(callInst, true, true, instFactory->makeLabel());
    }

    //every call must have exception edge -> add it
    if (callInst->getNode()->getExceptionEdge() == NULL) {
        Node* dispatchNode = cfg->getUnwindNode();
        if (dispatchNode==NULL) {
            dispatchNode = cfg->createDispatchNode(instFactory->makeLabel());
            cfg->setUnwindNode(dispatchNode);
            cfg->addEdge(dispatchNode, cfg->getExitNode());
        }
        cfg->addEdge(callInst->getNode(), dispatchNode);
        //fix inline info for this edge
        fixInlineInfo(callInst);
    }
}
typedef StlVector<MethodMarkerInst*> Markers;

static bool prepareMethodMarkersStack(Inst* stopInst, StlVector<bool>& flags, Markers& result, bool forward) {
    Node* node = stopInst->getNode();
    assert(flags[node->getId()]==false);
    flags[node->getId()] = true;
    const Edges& edges = node->getEdges(forward);
    bool res = forward ? node->isExitNode() : ((Inst*)node->getFirstInst())->isMethodEntry();
    for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
        Edge* e = *ite;
        Node* nextNode = e->getNode(forward);
        if (flags[nextNode->getId()]) {
            continue;
        }
        res = prepareMethodMarkersStack((Inst*)(forward?nextNode->getFirstInst():nextNode->getLastInst()), flags, result, forward);
        if (res) {
            break;
        }
    }
    if (!res) {
        return false;
    }
    if (Log::isEnabled()) {
        Log::out()<<"Path element :"; FlowGraph::printLabel(Log::out(), node); Log::out()<<std::endl;
    }

    //process insts in the current block: cache all non-paired markers, avoid caching paired insts
    //processing is done in reverse order for 'forward' flag -> from the last inst up to the first when forward='true'
    Inst* inst = (Inst*)(forward ? node->getLastInst() : node->getFirstInst());
    for (;;inst = forward?inst->getPrevInst():inst->getNextInst()) {
        if (inst->isMethodMarker()) {
            MethodMarkerInst* markerInst = inst->asMethodMarkerInst();
            MethodMarkerInst* pairInst = result.empty() ? (MethodMarkerInst*)NULL :*result.rbegin();
            MethodDesc* pairDesc = pairInst ? pairInst->getMethodDesc() : NULL;
            if (markerInst->getMethodDesc() == pairDesc && markerInst->isMethodEntryMarker() != pairInst->isMethodEntryMarker()) {
                if( Log::isEnabled()) {
                    Log::out()<<"POP: "; markerInst->print(Log::out()); Log::out()<<std::endl;
                }
                result.pop_back();
            } else { 
                if( Log::isEnabled()) {
                    Log::out()<<"PUSH: "; markerInst->print(Log::out());Log::out()<<std::endl;
                }
                assert((forward && markerInst->isMethodExitMarker()) || (!forward && markerInst->isMethodEntryMarker()));
                result.push_back(markerInst);
            }
        }
        if (inst == stopInst) {
            break;
        }
    }
    return true;
}

void HelperInliner::fixInlineInfo(MethodCallInst* callInst) {
    //the idea of algorithms: find all non-paired entry-markers on dispatch path
    // and create new dispatch node with a list of exit-markers

    if (Log::isEnabled()) {
        Log::out()<<"Fixing inline info for inst:"; inst->print(Log::out()); Log::out()<<std::endl;
    }
    Edge* dispatchEdge = callInst->getNode()->getExceptionEdge();
    assert(dispatchEdge);
    Node* dispatchNode = dispatchEdge->getTargetNode();

    Markers instsToEntry(localMM);
    Markers instsToExit(localMM);
    StlVector<bool> flags(localMM);
    flags.resize(cfg->getMaxNodeId());

    if (Log::isEnabled()) {
        Log::out()<<"fixInlineInfo: calculating path to entry:"<<std::endl;
    }
    std::fill(flags.begin(), flags.end(), false);
    prepareMethodMarkersStack(callInst, flags, instsToEntry, false);
    // here instsToEntry contains entry markers in topological order: 1, 2, 3

    if (Log::isEnabled()) {
        Log::out()<<"fixInlineInfo: calculating path to exit:"<<std::endl;
    }
    std::fill(flags.begin(), flags.end(), false);
    prepareMethodMarkersStack((Inst*)dispatchNode->getFirstInst(), flags, instsToExit, true);
    // here instsToExit contains exit markers in postorder order: 3, 2, 1
    std::reverse(instsToExit.begin(), instsToExit.end());
    // here instsToExit contains exit markers in postorder order: 1, 2, 3


    assert(instsToEntry.size() >= instsToExit.size());
    if (instsToEntry.size() > instsToExit.size()) {
        if (Log::isEnabled()) {
            Log::out()<<"Insts to entry:"<<instsToEntry.size()<<" insts to exit:"<<instsToExit.size()<<std::endl;
        }
        Node* newDispatchNode = cfg->createDispatchNode(instFactory->makeLabel());
        cfg->replaceEdgeTarget(dispatchEdge, newDispatchNode);
        cfg->addEdge(newDispatchNode, dispatchNode);

        size_t entrySize = instsToEntry.size();
        size_t exitSize = instsToExit.size();
        for(size_t i = 0; i < entrySize; i++) {
            if (i>=exitSize) {
                MethodMarkerInst* start = instsToEntry[i];
                assert(start->isMethodEntryMarker());
                MethodMarkerInst* end = (MethodMarkerInst*)instFactory->makeMethodMarker(MethodMarkerInst::Exit, start->getMethodDesc());    
                if (Log::isEnabled()) {
                    Log::out()<<"Creating pair for "; start->print(Log::out()); Log::out()<<" -> "; end->print(Log::out());Log::out()<<std::endl;
                }
                instsToExit.push_back(end);
                newDispatchNode->prependInst(end);
            }
        }
    } 
#ifdef _DEBUG
    assert(instsToEntry.size() == instsToExit.size());
    size_t size = instsToEntry.size();
    for(size_t i = 0; i < size; i++) {
        MethodMarkerInst* start = instsToEntry[i];
        MethodMarkerInst* end = instsToExit[i];
        assert(start->isMethodEntryMarker());
        assert(end->isMethodExitMarker());
        assert(start->getMethodDesc() == end->getMethodDesc());
    }
#endif
}

}//namespace
