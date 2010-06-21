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

#include "Log.h"
#include "gcmanagedpointeranalyzer.h"
#include "irmanager.h"
#include "deadcodeeliminator.h"
#include "FlowGraph.h"

namespace Jitrino {

DEFINE_SESSION_ACTION(GCManagedPointerAnalysisPass, opt_gcmap, "GC Managed Pointer to Base Map Construction")

void
GCManagedPointerAnalysisPass::_run(IRManager& irm) {
    GCManagedPointerAnalyzer analyzer(irm.getNestedMemoryManager(), irm);
    analyzer.analyzeManagedPointers();
}

GCManagedPointerAnalyzer::GCManagedPointerAnalyzer(MemoryManager& memoryManager, IRManager& irManager)
: _memoryManager(memoryManager),
_irManager(irManager),
_pBaseMap(new (memoryManager) BaseMap(memoryManager)),
_baseMap(*_pBaseMap),
_pVarMap(&irManager.getGCBasePointerMap()),
_varMap(*_pVarMap),
_mapsComputed(false),
_rematerializeMode(!irManager.getOptimizerFlags().gc_build_var_map)
{
}


SsaVarOpnd*
GCManagedPointerAnalyzer::createVarMapping(Type* baseType, SsaVarOpnd* ptr) 
{
    VarOpnd* ptrVar = ptr->getVar();
    VarOpnd* baseVar;
    if(_varMap.find(ptrVar) != _varMap.end()) {
        baseVar = _varMap[ptrVar];
        assert(baseVar->getType() == baseType);
    } else {
        baseVar = _irManager.getOpndManager().createVarOpnd(baseType, false);
        _varMap[ptrVar] = baseVar;
    }
    
    SsaVarOpnd* base = _irManager.getOpndManager().createSsaVarOpnd(baseVar);
    _baseMap[ptr] = base;
    return base;
}

void
GCManagedPointerAnalyzer::computeBaseMaps()
{
    assert(_mapsComputed == false);
    ControlFlowGraph& fg = _irManager.getFlowGraph();
    MemoryManager mm("GCManagedPointerAnalyzer::computeBaseMaps.mm");
    
    // List of nodes in RPO
    StlVector<Node*> nodes(mm);
    fg.getNodesPostOrder(nodes);
    StlVector<Node*>::reverse_iterator niter;
    
    // List of known static (i.e., non-heap) managed pointers.  We can ignore these. 
    StlHashSet<SsaOpnd*> _staticMap(mm);
    
#ifndef NDEBUG
    U_32 iterCount = 0;
#endif
    bool done = false;
    while(!done) {
        assert(++iterCount <= 2);
        done = true;
        for(niter = nodes.rbegin(); niter != nodes.rend(); ++niter) {
            Node* node = *niter;
            if(Log::isEnabled()) {
                Log::out() << "Consider block ";
                FlowGraph::printLabel(Log::out(), node);
                Log::out() << ::std::endl;
            }
            Inst* first = (Inst*)node->getFirstInst();
            for(Inst* inst = first->getNextInst(); inst != NULL; inst = inst->getNextInst()) {
                Opnd* dst_ = inst->getDst();
                if(!dst_->isNull() && dst_->getType()->isManagedPtr()) {
                    //
                    // Keep track of base
                    //
                    SsaOpnd* dst = dst_->asSsaOpnd();
                    assert(dst != NULL);
                    switch(inst->getOpcode()) {
                    case Op_LdStaticAddr:
                        // Static field - no GC support needed.
                        _staticMap.insert(dst);
                        break;
                    case Op_LdFieldAddr:
                    case Op_LdElemAddr: 
                    case Op_LdArrayBaseAddr:
                    case Op_LdLockAddr:
                    case Op_AddOffset:
                    case Op_AddOffsetPlusHeapbase:
                        {
                            // Base is directly available.
                            SsaTmpOpnd* base = inst->getSrc(0)->asSsaTmpOpnd();
                            assert(base != NULL && base->getType()->isObject());
                            _baseMap[dst] = base;
                            break;
                        }
                    case Op_AddScaledIndex:
                    case Op_StVar:
                    case Op_LdVar:
                        {
                            // Propagate base from src operand to dst operand.
                            SsaOpnd* src = inst->getSrc(0)->asSsaOpnd();
                            assert(src != NULL && src->getType()->isManagedPtr());
                            if(_baseMap.find(src) != _baseMap.end()) {
                                _baseMap[dst] = _baseMap[src];
                            } else if(_staticMap.find(src) != _staticMap.end()) {
                                _staticMap.insert(dst);
                            } else {
                                assert(0);
                            }
                            break;
                        }
                    case Op_Phi:
                        {
                            if(Log::isEnabled()) {
                                Log::out() << "Consider phi ";
                                inst->print(Log::out()); 
                                Log::out() << ::std::endl;
                            }
                            
                            PhiInst* phi = inst->asPhiInst();
                            SsaOpnd* base = NULL;
                            bool isStatic = false;
                            bool isAmbiguous = false;
                            U_32 nSrcs = phi->getNumSrcOperands();
                            for(U_32 i = 0; i < nSrcs; ++i) {
                                SsaVarOpnd* src = phi->getSrc(i)->asSsaVarOpnd();
                                assert(src != NULL);
                                if(_baseMap.find(src) != _baseMap.end()) {
                                    assert(!isStatic);
                                    SsaOpnd* candidate = _baseMap[src];
                                    if(base == NULL) {
                                        base = candidate;
                                    }
                                    else if(base != candidate) {
                                        assert(base->getType() == candidate->getType());
                                        isAmbiguous = true;
                                    }
                                } else if(_staticMap.find(src) != _staticMap.end()) {
                                    assert(base == NULL && isAmbiguous == false);
                                    isStatic = true;
                                    _staticMap.insert(dst);
                                } else {
                                    // Not recorded - uninitialized input - need another iteration
                                    done = false;
                                    if(Log::isEnabled()) {
                                        Log::out() << "Undefined arg ";
                                        src->print(Log::out()); 
                                        Log::out() << " - redo" << ::std::endl;
                                    }
                                }
                            }
                            assert(isStatic || base != NULL);
                            
                            if(isAmbiguous) {
                                SsaVarOpnd* dstSsaVar = dst->asSsaVarOpnd();
                                assert(dstSsaVar != NULL);
                                createVarMapping(base->getType(), dstSsaVar);
                            } else if(!isStatic) {
                                assert(base != NULL);
                                _baseMap[dst] = base;
                            }
                        } // case Op_Phi
            break;
            default:
            break;
                    } // switch(inst->getOpcode()) {
                } // if(!dst_->isNull() && dst_->getType()->isManagedPtr())
            }
        }
    }
    
    _mapsComputed = true;
}

void
GCManagedPointerAnalyzer::addBaseVarDefs()
{
    assert(_mapsComputed == true);
    
    
    BaseMap::iterator i;
    for(i = _baseMap.begin(); i != _baseMap.end(); ++i) {
        if(Log::isEnabled()) {
            Log::out() << "Add def for ";
            i->first->print(Log::out());
            Log::out() << ::std::endl;
        }
        SsaVarOpnd* ptr = i->first->asSsaVarOpnd();
        if(ptr != NULL && _varMap.find(ptr->getVar()) != _varMap.end())
            insertVarDef(ptr);
    }
}

SsaVarOpnd*
GCManagedPointerAnalyzer::insertVarDef(SsaVarOpnd* ptr)
{
    if(Log::isEnabled()) {
        Log::out() << "Insert var def for ";
        ptr->print(Log::out());
        Log::out() << ::std::endl;
    }
    assert(ptr->getType()->isManagedPtr());
    SsaOpnd* base = _baseMap[ptr];
    VarOpnd* ptrVar = ptr->getVar();
    assert(_varMap.find(ptrVar) != _varMap.end());
    VarOpnd* baseVar = _varMap[ptrVar];
    
    
    SsaVarOpnd* baseSsa = NULL;
    if(base->isSsaVarOpnd() && base->asSsaVarOpnd()->getVar() == baseVar) {
        baseSsa = base->asSsaVarOpnd();
        if(base->getInst() != NULL) {
            // Already defined!
            return baseSsa;
        } else {
            if(ptr->getInst()->getOpcode() == Op_StVar)
                // Nothing to be done.  baseSsa will be defined at a phi.
                return baseSsa;
        }
    }
    else {
        baseSsa = createVarMapping(baseVar->getType(), ptr);
    }
    assert(baseSsa != NULL && baseSsa->getVar() == baseVar && baseSsa->getInst() == NULL);
    
    Inst* ptrDef = ptr->getInst();
    Opcode ptrOp = ptrDef->getOpcode();
    InstFactory& instFactory = _irManager.getInstFactory();
    if(ptrOp == Op_Phi) {
        PhiInst* ptrPhi = ptrDef->asPhiInst();
        U_32 numOpnds = ptrPhi->getNumSrcOperands();
        Opnd** newOpnds = new (_irManager.getMemoryManager()) Opnd*[numOpnds];
        Inst* basePhi = instFactory.makePhi(baseSsa, numOpnds, newOpnds);
        for(U_32 i = 0; i < numOpnds; ++i)
            basePhi->setSrc(i, insertVarDef(ptrPhi->getSrc(i)->asSsaVarOpnd()));
        basePhi->insertBefore(ptrPhi);
    } else {
        assert(ptrOp == Op_StVar);
        VarAccessInst* ptrStVar = ptrDef->asVarAccessInst();
        SsaTmpOpnd* tmp = base->asSsaTmpOpnd();
        if(tmp == NULL) {
            assert(base->isSsaVarOpnd() && base->asSsaVarOpnd()->getVar() != baseSsa->getVar());
            tmp = _irManager.getOpndManager().createSsaTmpOpnd(base->getType());
            Inst* ldVar = instFactory.makeLdVar(tmp, base->asSsaVarOpnd());
            ldVar->insertBefore(ptrStVar);
        }
        Inst* baseStVar = instFactory.makeStVar(baseSsa, tmp);
        baseStVar->insertBefore(ptrStVar);
    }
    assert(baseSsa->getInst() != NULL);
    return baseSsa;
}

void
GCManagedPointerAnalyzer::analyzeManagedPointers() 
{
    if(!_rematerializeMode) {
        computeBaseMaps();
        addBaseVarDefs();
    } else {
        ControlFlowGraph& fg = _irManager.getFlowGraph();
        MemoryManager mm("GCManagedPointerAnalyzer::analyzeManagedPointers.mm");
        
        StlVector<Node*> nodes(mm);
        fg.getNodesPostOrder(nodes);
        
        StlHashSet<SsaOpnd*> _staticMap(mm);
        
        StlVector<Node*>::reverse_iterator niter;
        
#ifndef NDEBUG
        U_32 k = 0;
#endif
        bool done = false;
        while(!done) {
            assert((++k) <= 3);
            done = true;
            for(niter = nodes.rbegin(); niter != nodes.rend(); ++niter) {
                Node* node = *niter;
                if(Log::isEnabled()) {
                    Log::out() << "Consider block ";
                    FlowGraph::printLabel(Log::out(), node);
                    Log::out() << ::std::endl;
                }
                Inst* first = (Inst*)node->getFirstInst();
                for(Inst* inst = first->getNextInst(); inst != NULL; inst = inst->getNextInst()) {
                    Opnd* dst_ = inst->getDst();
                    if(!dst_->isNull() && dst_->getType()->isManagedPtr()) {
                        //
                        // Keep track of base
                        //
                        SsaOpnd* dst = dst_->asSsaOpnd();
                        assert(dst != NULL);
                        switch(inst->getOpcode()) {
                        case Op_LdStaticAddr:
                            // Static field - no GC support needed.
                            _staticMap.insert(dst);
                            break;
                        case Op_LdFieldAddr:
                        case Op_LdElemAddr: 
                        case Op_LdArrayBaseAddr:
                        case Op_LdLockAddr:
                            {
                                SsaTmpOpnd* base = inst->getSrc(0)->asSsaTmpOpnd();
                                assert(base != NULL && base->getType()->isObject());
                                _baseMap[dst] = base;
                                break;
                            }
                        case Op_AddScaledIndex:
                        case Op_StVar:
                        case Op_LdVar:
                            {
                                SsaOpnd* src = inst->getSrc(0)->asSsaOpnd();
                                assert(src != NULL && src->getType()->isManagedPtr());
                                if(_baseMap.find(src) != _baseMap.end()) {
                                    _baseMap[dst] = _baseMap[src];
                                } else if(_staticMap.find(src) != _staticMap.end()) {
                                    _staticMap.insert(dst);
                                } else {
                                    assert(0);
                                    if(inst->getOpcode() == Op_AddScaledIndex) {
                                        FlowGraph::printDotFile(fg, _irManager.getMethodDesc(), "error");
                                    }
                                    assert(inst->getOpcode() != Op_AddScaledIndex);
                                }
                                break;
                            }
                        case Op_Phi:
                            {
                                if(Log::isEnabled()) {
                                    Log::out() << "Consider phi ";
                                    inst->print(Log::out()); 
                                    Log::out() << ::std::endl;
                                }
                                
                                bool rematerialize = false;
                                PhiInst* phi = inst->asPhiInst();
                                SsaOpnd* base = NULL;
                                U_32 nSrcs = phi->getNumSrcOperands();
                                for(U_32 i = 0; i < nSrcs; ++i) {
                                    SsaVarOpnd* src = phi->getSrc(i)->asSsaVarOpnd();
                                    assert(src != NULL);
                                    if(_baseMap.find(src) != _baseMap.end()) {
                                        SsaOpnd* candidate = _baseMap[src];
                                        if(base == NULL)
                                            base = candidate;
                                        else if(base != candidate) 
                                            rematerialize = true;
                                    } else {
                                        if(_staticMap.find(src) == _staticMap.end()) {
                                            // Not recorded as a static
                                            if(Log::isEnabled()) {
                                                Log::out() << "Undefined arg ";
                                                src->print(Log::out()); 
                                                Log::out() << " - redo" << ::std::endl;
                                            }
                                            done = false;
                                        } else {
                                            _staticMap.insert(dst);
                                        }
                                    }
                                }
                                if(!rematerialize) {
                                    if(base != NULL)
                                        _baseMap[dst] = base;
                                } else {
                                    if(Log::isEnabled()) {
                                        Log::out() << "Converting " << ::std::endl;
                                        FlowGraph::print(Log::out(), node);
                                        Log::out() << "Rematerialize for " << ::std::endl;
                                        phi->print(Log::out());
                                        Log::out() << std::endl;
                                    }
                                    Opcode opcode = NumOpcodes;
                                    FieldDesc* fieldDesc = NULL;
                                    Type* elementType = NULL;
                                    U_32 numArgs = 0;
                                    StlVector<SsaTmpOpnd*> args(mm);
                                    StlVector<Type*> argTypes(mm);
                                    StlVector<SsaTmpOpnd*> defs(mm);
                                    for(U_32 i = 0; i < nSrcs; ++i) {
                                        SsaVarOpnd* src = phi->getSrc(i)->asSsaVarOpnd();
                                        assert(src != NULL);
                                        Inst* stVar = src->getInst();
                                        assert(stVar->getOpcode() == Op_StVar);
                                        DeadCodeEliminator::copyPropagate(stVar);
                                        SsaTmpOpnd* def = stVar->getSrc(0)->asSsaTmpOpnd();
                                        assert(def != NULL);
                                        assert(defs.size() == i);
                                        defs.push_back(def);
                                        Inst* defInst = def->getInst();
                                        if(Log::isEnabled()) {
                                            Log::out() << "Process input " << ::std::endl;
                                            defInst->print(Log::out());
                                            Log::out() << ::std::endl;
                                        }
                                        if(opcode == NumOpcodes) {
                                            opcode = defInst->getOpcode();
                                            numArgs = defInst->getNumSrcOperands();
                                            switch(opcode) {
                                            case Op_LdFieldAddr:
                                                fieldDesc = defInst->asFieldAccessInst()->getFieldDesc();
                                                break;
                                            case Op_LdElemAddr: 
                                            case Op_LdArrayBaseAddr:
                                                elementType = defInst->asTypeInst()->getTypeInfo();
                                                break;
                                            default:
                                                break;
                                            }
                                            for(U_32 j = 0; j < numArgs; ++j) {
                                                SsaTmpOpnd* arg = defInst->getSrc(j)->asSsaTmpOpnd();
                                                assert(arg != NULL);
                                                if(Log::isEnabled()) {
                                                    Log::out() << "Initializing ";
                                                    arg->print(Log::out());
                                                    Log::out() << ::std::endl;
                                                }
                                                args.push_back(arg);
                                                argTypes.push_back(arg->getType());
                                            }
                                        } else {
                                            Opcode newOpcode = defInst->getOpcode();
                                            if( !(opcode == newOpcode) ) assert(0);
                                            assert(numArgs == defInst->getNumSrcOperands());
                                            switch(opcode) {
                                            case Op_LdFieldAddr:
                                                assert(fieldDesc == defInst->asFieldAccessInst()->getFieldDesc());
                                                break;
                                            case Op_LdElemAddr: 
                                            case Op_LdArrayBaseAddr:
                                                assert(elementType == defInst->asTypeInst()->getTypeInfo());
                                                break;
                                            default:
                                                break;
                                            }
                                            for(U_32 j = 0; j < numArgs; ++j) {
                                                SsaTmpOpnd* arg = defInst->getSrc(j)->asSsaTmpOpnd();
                                                if(Log::isEnabled()) {
                                                    Log::out() << "Recording ";
                                                    arg->print(Log::out());
                                                    Log::out() << ::std::endl;
                                                }
                                                if(args[j] != arg) {
                                                    Log::out() << "Must create phi for arg " << (int) j << ::std::endl;
                                                    args[j] = NULL;
                                                    argTypes[j] = _irManager.getTypeManager().getCommonType(argTypes[j], arg->getType());
                                                }
                                            }
                                        }
                                    }
                                    OpndManager& opndManager = _irManager.getOpndManager();
                                    InstFactory& instFactory = _irManager.getInstFactory();
                                    Inst* last = inst;
                                    for(U_32 j = 0; j < numArgs; ++j) {
                                        if(args[j] == NULL) {
                                            VarOpnd* var = opndManager.createVarOpnd(argTypes[j], false);
                                            if(Log::isEnabled()) {
                                                Log::out() << "Creating ";
                                                var->print(Log::out());
                                                Log::out() << " for arg " << (int) j << ::std::endl;
                                            }
                                            SsaTmpOpnd* tmp = opndManager.createSsaTmpOpnd(argTypes[j]);
                                            Opnd** phiArgs = new (_irManager.getMemoryManager()) Opnd*[nSrcs];
                                            for(U_32 i = 0; i < nSrcs; ++i) {
                                                SsaTmpOpnd* def = defs[i];
                                                Inst* defInst = def->getInst();
                                                SsaVarOpnd* ssaVar = opndManager.createSsaVarOpnd(var);
                                                phiArgs[i] = ssaVar;
                                                SsaTmpOpnd* src = defInst->getSrc(j)->asSsaTmpOpnd();
                                                Inst* stVar = instFactory.makeStVar(ssaVar, src);
                                                stVar->insertBefore(defInst);
                                                if(src->getType()->isManagedPtr()) {
                                                    if(_baseMap.find(src) != _baseMap.end()) {
                                                        _baseMap[ssaVar] = _baseMap[src];
                                                    }
                                                    else {
                                                        if(Log::isEnabled()) {
                                                            Log::out() << "Can't find base for ";
                                                            src->print(Log::out()); 
                                                            Log::out() << " - redo" << ::std::endl;
                                                        }
                                                        done = false;
                                                    }
                                                }
                                            }
                                            SsaVarOpnd* ssaDst = opndManager.createSsaVarOpnd(var);
                                            Inst* phiInst = instFactory.makePhi(ssaDst, nSrcs, phiArgs);
                                            phiInst->insertAfter(last);
                                            last = phiInst;
                                            Inst* ldVar = instFactory.makeLdVar(tmp, ssaDst);
                                            ldVar->insertAfter(last);
                                            last = ldVar;
                                            args[j] = tmp;
                                        }
                                    }
                                    SsaVarOpnd* dst = phi->getDst()->asSsaVarOpnd();
                                    SsaTmpOpnd* tmp = opndManager.createSsaTmpOpnd(dst->getType());
                                    Inst* redo = NULL;
                                    switch (opcode) {
                                    case Op_LdFieldAddr:
                                        assert(fieldDesc != NULL);
                                        redo = instFactory.makeLdFieldAddr(tmp, args[0], fieldDesc);
                                        break;
                                    case Op_LdElemAddr: 
                                        assert(elementType != NULL);
                                        redo = instFactory.makeLdElemAddr(elementType, tmp, args[0], args[1]);
                                        break;
                                    case Op_LdArrayBaseAddr:
                                        assert(elementType != NULL);
                                        redo = instFactory.makeLdArrayBaseAddr(elementType, tmp, args[0]);
                                        break;
                                    case Op_AddScaledIndex:
                                        redo = instFactory.makeAddScaledIndex(tmp, args[0], args[1]);
                                        break;
                                    case Op_LdLockAddr:
                                        // this might work if this happens:
                                        redo = instFactory.makeLdLockAddr(tmp, args[0]);
                                        break;
                                    default:
                                        assert(0);
                                    }
                                    redo->insertAfter(last);
                                    last = redo;
                                    Inst* stVar = instFactory.makeStVar(dst, tmp);
                                    stVar->insertAfter(last);
                                    Inst* prev = inst->getPrevInst();
                                    inst->unlink();
                                    inst = prev;
                                    if(Log::isEnabled()) {
                                        Log::out() << "to " << ::std::endl;
                                        FlowGraph::print(Log::out(), node);
                                    }
                                }
                                break;
                            }
                        default:
                            assert(0);
                        }
                    }
                }
            }
        }
    }
}

} //namespace Jitrino 
