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
#include "simplifytaus.h"
#include "irmanager.h"
#include "Stl.h"
#include "walkers.h"

namespace Jitrino {

DEFINE_SESSION_ACTION(TauSimplificationPass, tausimp, "Tau Simplification")

void
TauSimplificationPass::_run(IRManager& irm) {
    SimplifyTaus pass(irm.getNestedMemoryManager(), irm);
    pass.runPass();
}

//
// Our goal is to remove uses of tauHasType
//   we can make use of these rules:
//     tauHasType(t1=defArg,T) -->  tauSafe
//     tauHasType(t1=call,T) --> tauSafe
//     tauHasType(t1=staticCast(t2,tau1,T1),T2) -> tau1  [requires T1 <= T2; assert otherwise]
//     tauHasType(t1=ldvar(v1),T) -> ldvar(map(v1,T))
//     tauHasType(t1=copy(t2),T) -> tauHasType(t2)
//     tauHasType(t1=ldInd(),T) -> tauSafe

//   if map(var,T) is used, we must construct a new tauVar=map(var1,T), and add:
//     foreach version var1 of var,
//        var1 = stvar t1               -->             tau1 = tauHasType(t1, T); tauvar1 = stvar tau1
//        var1 = phi(var2, ...)         -->             tauVar1 = phi(tauvar2, ...)

//   and then the new HasType operations must be removed

SimplifyTaus::SimplifyTaus(MemoryManager& memoryManager, IRManager& irManager0)
    : memManager(memoryManager),
      irManager(irManager0),
      tauSafeOpnd(0)
{
}

class TauHasTypesMap {
    typedef ::std::pair<Opnd *, Type *> OpndXType;
#ifdef PLATFORM_POSIX
    struct OpndXTypeHash : public __gnu_cxx::hash<OpndXType> {
#else
    #if !defined(__SGI_STL_PORT)
    struct OpndXTypeHash : public stdext::hash_compare<OpndXType> {
    #else
    struct OpndXTypeHash : public ext::hash_compare<OpndXType> {
    #endif
#endif
        size_t operator() (const OpndXType x) const {
            return ((((size_t) x.first) >> 3) ^
                    (((size_t) x.second) >> 3));
        }
    };
    typedef StlMap<OpndXType, Opnd * /*, OpndXTypeHash*/> TableType;
    TableType table;
public:
    TauHasTypesMap(MemoryManager &mm): table(mm) {};
    Opnd *lookup(Opnd *src, Type *type) {
        TableType::iterator 
            found = table.find(OpndXType(src, type)),
            end = table.end();
        if (found != end) {
            return (*found).second;
        } else {
            return 0;
        }
    };
    void insert(Opnd *mapTo, Opnd *src, Type *type) {
        Opnd *found = table[OpndXType(src, type)];
        if (found) {
            assert(found == mapTo);
        } else {
            table[OpndXType(src, type)] = mapTo;
        }
    }
};

class TauCopyMap {
    typedef StlHashMap<Opnd *, Opnd *> TableType;
    TableType table;
public:
    TauCopyMap(MemoryManager &mm) : table(mm) {};
    bool has(Opnd *from) {
        return (table.find(from) != table.end());
    };
    Opnd *&operator[](Opnd *from) {
        return table[from];
    }
    // returns 0 if not there
    Opnd *lookup(Opnd *from) {
        TableType::iterator 
            found = table.find(from),
            end = table.end();
        if (found != end) {
            return (*found).second;
        } else {
            return 0;
        }
    }
};

class TauSeenTypesMap {
public:
    typedef StlVectorSet<Type *> TypeSet;
    typedef StlHashMap<Opnd *, TypeSet *> TableType;
private:
    TableType table;
    MemoryManager &mm;
public:
    TauSeenTypesMap(MemoryManager &mm0): table(mm0), mm(mm0) {};
    TypeSet *lookup(Opnd *src) {
        TableType::iterator 
            found = table.find(src),
            end = table.end();
        if (found != end) {
            return (*found).second;
        } else {
            return 0;
        }
    };
    void insert(Opnd *src, Type *type) {
        TypeSet *found = table[src];
        if (!found) {
            found = new (mm) TypeSet(mm);
        }
        table[src] = found;
        found->insert(type);
    }
};


// base class shared by walkers
class TauWalkerState {
protected:
    bool handleCalls;
    MemoryManager &mm;
    IRManager &irManager;
    SsaTmpOpnd *tauSafeOpnd;
    TauHasTypesMap &map;
    TauHasTypesMap &exactMap;
    TauCopyMap &copyMap;
    StlVector<Inst *> &toRemove;
    TauSeenTypesMap &typesMap;
    TauSeenTypesMap &exactTypesMap;
    bool foundVarTau;
public:
    TauWalkerState(bool withcalls,
                   MemoryManager &mm0,
                   IRManager &irManager0,
                   SsaTmpOpnd *tauSafeOpnd0,
                   TauHasTypesMap &map0,
                   TauHasTypesMap &exactMap0,
                   TauCopyMap &copyMap0,
                   TauSeenTypesMap &typesMap0,
                   TauSeenTypesMap &exactTypesMap0,
                   StlVector<Inst *> &toRemove0)
        : handleCalls(withcalls), mm(mm0), irManager(irManager0),
          tauSafeOpnd(tauSafeOpnd0), map(map0), exactMap(map0),
          copyMap(copyMap0), toRemove(toRemove0),
          typesMap(typesMap0), exactTypesMap(exactTypesMap0),
          foundVarTau(false)
    {};

    void findReducibleTaus(Inst *i);
    void reduceVarTaus(Inst *i);
    void replaceReducibleTaus(Inst *i);

    bool foundNewVarTau() { return foundVarTau; };
    void resetNewVarTau() { foundVarTau = false; };

protected:
    // find taus walker methods
    void noteInstToRemove(Inst *tauHasTypeInst);
    Opnd *findReplacement(Opnd *src, Type *type, bool exactType); // returns true if found
    Opnd *lookupMapping(Opnd*, Type *, bool exactType);
    void recordMapping(Opnd *mapTo, Opnd *src, Type *type, bool exactType);
    Opnd *handleLdVar(Opnd *src, Type *type, Inst *ldVarInst, bool exactType);
    void recordOpndTypePair(Opnd *, Type *, bool exactType);

    void recordCopyMapping(Opnd *mapTo, Opnd *mapFrom);
    Opnd *lookupCopyMapping(Opnd *mapFrom);

    SsaTmpOpnd *genTauSafe() {
        return tauSafeOpnd;
    };

    // var walker methods
    StlVectorSet<Type *> *shouldReduceSsaOpnd(Opnd *opnd, bool exactType); // returns set of types for which var should be reduced
    VarOpnd *getReductionTauBaseVar(Opnd *opnd, Type *type, bool exactType); // returns tau base for (var,type)
    Opnd *reduceSsaOpnd(Opnd *opnd, VarOpnd *baseVar, Type *type, bool exactType);

    VarOpnd *getTauForVarOpnd(VarOpnd *varOpnd, Type *type, bool exactType);
    SsaVarOpnd *getTauForSsaVarOpnd(SsaVarOpnd *ssaVarOpnd, Type *type, bool exactType);

    // replace taus walker methods
};

// an Inst walker
class FindReducibleTausWalker {
    TauWalkerState &state;
public:
    FindReducibleTausWalker(TauWalkerState &state0):
        state(state0)
    {};

    void applyToInst(Inst *i) { state.findReducibleTaus(i); };
};

// an Inst walker
class ReduceVarTausWalker {
    TauWalkerState &state;
public:
    ReduceVarTausWalker(TauWalkerState &state0):
        state(state0)
    {};

    void applyToInst(Inst *i) { state.reduceVarTaus(i); };
};

// an Inst walker
class ReplaceReducibleTausWalker {
    TauWalkerState &state;
public:
    ReplaceReducibleTausWalker(TauWalkerState &state0):
        state(state0)
    {};

    void applyToInst(Inst *i) { state.replaceReducibleTaus(i); };
};

void
SimplifyTaus::runPass()
{
    ControlFlowGraph &fg = irManager.getFlowGraph();
    SsaTmpOpnd *tauSafeOpnd = findTauSafeOpnd();
    TauHasTypesMap map(memManager);
    TauHasTypesMap exactmap(memManager);
    StlVector<Inst *> toRemove(memManager);
    TauCopyMap copymap(memManager);
    TauSeenTypesMap typesmap(memManager);
    TauSeenTypesMap exacttypesmap(memManager);

    TauWalkerState state(true, // include calls
                         memManager, irManager, tauSafeOpnd, map, exactmap, copymap, 
                         typesmap, exacttypesmap, toRemove);


    do {
        // first walker examines each TauHasType operand,
        //   dereferences TauStaticCast and TauAsType to the source Tau
        //   handles LdVar by creating a new tauVar and tauSsaVar and recording
        //   mappings (var,type)->tauvar; (ssavar,type)->taussavar
        // in each case, the reduced tau is added to copymap; the code is not yet changed
        FindReducibleTausWalker instWalker(state);
        Inst2NodeWalker<true, FindReducibleTausWalker> nodeWalker(instWalker);
        NodeWalk(fg, nodeWalker);
        
        state.resetNewVarTau();
        
        // for any (var,type) mapped above, we add a parallel tau structure:
        //   for any stvar var=tmp
        //   we add  stvar tauvar=HasType(tmp,type)
        ReduceVarTausWalker instWalker2(state);
        Inst2NodeWalker<true, ReduceVarTausWalker> nodeWalker2(instWalker2);
        NodeWalk(fg, nodeWalker2);
    } while (state.foundNewVarTau());

    ReplaceReducibleTausWalker instWalker3(state);
    Inst2NodeWalker<true, ReplaceReducibleTausWalker> nodeWalker3(instWalker3);
    NodeWalk(fg, nodeWalker3);

    StlVector<Inst *>::iterator
        iter = toRemove.begin(),
        end = toRemove.end();
    for ( ; iter != end; ++iter) {
#ifdef _NDEBUG
        Inst *remInst = *iter;
        assert(remInst->next() == remInst);
        assert(remInst->prev() == remInst);
#endif
    }
}

Opnd *TauWalkerState::lookupMapping(Opnd *src, Type *type, bool exactType)
{
    Opnd *found = exactType ? exactMap.lookup(src, type) : map.lookup(src, type);
    if (Log::isEnabled()) {
        if (exactType)
            Log::out() << "found tau exacttype mapping (";
        else
            Log::out() << "found tau mapping (";
        src->print(Log::out());
        Log::out() << ", ";
        if (type)
            type->print(Log::out());
        else
            Log::out() << "NONNULL";
        Log::out() << ") -> ";
        if (found)
            found->print(Log::out());
        else
            Log::out() << "NULL";
        Log::out() << ::std::endl;
    }
    return found;
}

void TauWalkerState::recordMapping(Opnd *mapTo, Opnd *src, Type *type, bool exactType)
{
    if (Log::isEnabled()) {
        Log::out() << "recording tau mapping (";
        src->print(Log::out());
        Log::out() << ", ";
        if (type)
            type->print(Log::out());
        else
            Log::out() << "NONNULL";
        Log::out() << ") -> ";
        mapTo->print(Log::out());
        Log::out() << ::std::endl;
    }
    if (exactType) {
        exactMap.insert(mapTo, src, type);
    } else {
        map.insert(mapTo, src, type);
    }
    recordOpndTypePair(src, type, exactType);
}

void TauWalkerState::recordCopyMapping(Opnd *mapTo, Opnd *mapFrom)
{
    if (Log::isEnabled()) {
        Log::out() << "recording tau CopyMapping: ";
        mapFrom->print(Log::out());
        Log::out() << " -> ";
        mapTo->print(Log::out());
        Log::out() << ::std::endl;
    }
    copyMap[mapFrom] = mapTo;
}

Opnd *TauWalkerState::lookupCopyMapping(Opnd *mapFrom)
{
    Opnd *found = copyMap.lookup(mapFrom);
    if (Log::isEnabled()) {
        Log::out() << "found CopyMapping: ";
        mapFrom->print(Log::out());
        Log::out() << " -> ";
        if (found)
            found->print(Log::out());
        else
            Log::out() << "NULL";
        Log::out() << ::std::endl;
    }
    return found;
}

void TauWalkerState::noteInstToRemove(Inst *tauHasTypeInst)
{
    if (Log::isEnabled()) {
        Log::out() << "noting inst to remove: ";
        tauHasTypeInst->print(Log::out());
        Log::out() << ::std::endl;
    }
    toRemove.push_back(tauHasTypeInst);
}

void TauWalkerState::findReducibleTaus(Inst *i)
{
    Opcode opcode = i->getOpcode();
    if ((opcode == Op_TauHasType) || (opcode == Op_TauHasExactType)) {
        TypeInst *tInst = i->asTypeInst();
        assert(tInst);
        Opnd *tauDst = tInst->getDst();
        Opnd *src = tInst->getSrc(0);
        Type *type = tInst->getTypeInfo();

        bool exactType = (opcode == Op_TauHasExactType);
        Opnd *mapsTo = lookupMapping(src, type, exactType);
        
        if (mapsTo) {
            noteInstToRemove(i);
            recordCopyMapping(mapsTo, tauDst);
        } else {
            Opnd *found = findReplacement(src, type, exactType);
            if (found) {
                noteInstToRemove(i);
                recordMapping(found, src, type, exactType);
                recordCopyMapping(found, tauDst);
            }
        }
    } else if (opcode == Op_TauIsNonNull) {
        Opnd *tauDst = i->getDst();
        Opnd *src = i->getSrc(0);
        Type *type = 0; // use 0 type to denote non null

        Opnd *mapsTo = lookupMapping(src, type, false);
        
        if (mapsTo) {
            noteInstToRemove(i);
            recordCopyMapping(mapsTo, tauDst);
        } else {
            Opnd *found = findReplacement(src, type, false);
            if (found) {
                noteInstToRemove(i);
                recordMapping(found, src, type, false);
                recordCopyMapping(found, tauDst);
            }
        }
    }
}

Opnd *TauWalkerState::findReplacement(Opnd *src, Type *type, bool exactType)
{
    Inst *srcInst = src->getInst();
    Opcode opc = srcInst->getOpcode();
    switch (opc) {
    case Op_Add:
    case Op_Mul:
    case Op_Sub:
    case Op_TauDiv:
    case Op_TauRem:
    case Op_Neg:
    case Op_MulHi:
    case Op_Min:
    case Op_Max:
    case Op_Abs:
    case Op_And:
    case Op_Or:
    case Op_Xor:
    case Op_Not:
        assert(0); 
        
    case Op_Select:
        assert(0); 
    case Op_Conv:
        assert(0); 
        
    case Op_Shladd:
    case Op_Shl:
    case Op_Shr:
    case Op_Cmp:
    case Op_Cmp3:
        assert(0); 
    case Op_Branch:
    case Op_Jump:
    case Op_Switch:
        assert(0); 
        
    case Op_DirectCall:
    case Op_TauVirtualCall:
    case Op_IndirectCall:
    case Op_IndirectMemoryCall:
        if (handleCalls) {
            return genTauSafe();
        } else {
            return 0;
        }
        
    case Op_Return:
        assert(0); 
            
    case Op_Catch:
        // is kind of a call
        if (handleCalls) {
            return genTauSafe();
        } else {
            return 0;
        }
        
    case Op_Throw:
    case Op_PseudoThrow:
        assert(0); 
        
    case Op_JSR:
    case Op_Ret:
    case Op_SaveRet:
        assert(0); 
        
    case Op_Copy:
        assert(0); 
        
    case Op_DefArg:
        // is a call-related type
        if (handleCalls) {
            return genTauSafe();
        } else {
            return 0;
        }
        
    case Op_LdConstant:
    case Op_LdRef:
        {
            return genTauSafe();
        }
        
    case Op_LdVar:
        {
            return handleLdVar(src, type, srcInst, exactType); 
        }
        
    case Op_LdVarAddr:
    case Op_TauLdInd:
    case Op_TauLdField:
    case Op_LdStatic:
    case Op_TauLdElem:
    case Op_LdFieldAddr:
    case Op_LdStaticAddr:
    case Op_LdElemAddr:
        {
            return genTauSafe();
        }
        
    case Op_TauLdVTableAddr:
    case Op_TauLdIntfcVTableAddr:
    case Op_TauLdVirtFunAddr:
    case Op_TauLdVirtFunAddrSlot:
    case Op_LdFunAddr:
    case Op_LdFunAddrSlot:
    case Op_GetVTableAddr:
        assert(0);        // not an object type

    case Op_GetClassObj:
        {
            return genTauSafe();
        }

    case Op_TauArrayLen:
    case Op_LdArrayBaseAddr:
    case Op_AddScaledIndex:
        assert(0);        // not an object type
        
    case Op_StVar:
        assert(0); // shouldn't happen, not a tmp
        
    case Op_TauStInd:
    case Op_TauStField:
    case Op_TauStElem:
    case Op_TauStStatic:
    case Op_TauStRef:
        assert(0); // shouldn't happen, no dstOp
        
    case Op_TauCheckBounds:
    case Op_TauCheckLowerBound:
    case Op_TauCheckUpperBound:
    case Op_TauCheckNull:
    case Op_TauCheckZero:
    case Op_TauCheckDivOpnds:
    case Op_TauCheckElemType:
    case Op_TauCheckFinite:
        assert(0); // shouldn't happen, dst isn't object
        
    case Op_NewObj:
    case Op_NewArray:
    case Op_NewMultiArray:
        {
            return genTauSafe();
        }

    case Op_TauMonitorEnter:
    case Op_TauMonitorExit:
    case Op_TypeMonitorEnter:
    case Op_TypeMonitorExit:
    case Op_LdLockAddr:
    case Op_IncRecCount:
    case Op_TauBalancedMonitorEnter:
    case Op_BalancedMonitorExit:
    case Op_TauOptimisticBalancedMonitorEnter:
    case Op_OptimisticBalancedMonitorExit:
    case Op_MonitorEnterFence:
    case Op_MonitorExitFence:
        assert(0); // no object 
        
    case Op_TauStaticCast:
        {
            assert(type);

            TypeInst *srcTypeInst = srcInst->asTypeInst();
            assert(srcTypeInst);
            Type *castType = srcTypeInst->getTypeInfo();
            if (irManager.getTypeManager().isResolvedAndSubClassOf(castType, type)) {
                // is guaranteed by the cast
                Opnd *tauOpnd = srcInst->getSrc(1);
                assert(tauOpnd->getType()->tag == Type::Tau);
                return tauOpnd;
            } else {
                assert(0); 
                return 0;
            }
        }
            
    case Op_TauCast:
        assert(0);         
        break;
        
    case Op_TauAsType:
        {
            assert(!exactType);
            assert(type);

            TypeInst *srcTypeInst = srcInst->asTypeInst();
            assert(srcTypeInst);
            Type *castType = srcTypeInst->getTypeInfo();
            if (irManager.getTypeManager().isResolvedAndSubClassOf(castType, type)) {
                // is guaranteed
                return genTauSafe();
            } else {
                assert(0); 
                return 0;
            }
        }
        
    case Op_TauInstanceOf:
        assert(0); // not an object
        
    case Op_InitType:
    case Op_Label:
    case Op_MethodEntry:
    case Op_MethodEnd:
        assert(0); // no dstOpnd
        
    case Op_Phi:
        assert(0); // should be a ldvar instead
        break;
        
    case Op_TauPi:
        assert(0); 
        
    case Op_IncCounter:
    case Op_Prefetch:
        assert(0); // no object destopnd
        
    case Op_UncompressRef:
        {
            assert(type);
            Opnd *compressedSrc = srcInst->getSrc(0);
            Type *compressedType = irManager.getTypeManager().compressType(type);

            return findReplacement(compressedSrc, compressedType, exactType);
        }
        
    case Op_CompressRef:
        {
            assert(type);
            Opnd *uncompressedSrc = srcInst->getSrc(0);
            Type *uncompressedType = irManager.getTypeManager().uncompressType(type);

            return findReplacement(uncompressedSrc, uncompressedType, exactType);
        }
        
    case Op_LdFieldOffset:
    case Op_LdFieldOffsetPlusHeapbase:
    case Op_LdArrayBaseOffset:
    case Op_LdArrayBaseOffsetPlusHeapbase:
    case Op_LdArrayLenOffset:
    case Op_LdArrayLenOffsetPlusHeapbase:
        assert(0); // these are offsets, not objects
        
    case Op_AddOffset:
    case Op_AddOffsetPlusHeapbase:
        assert(0); // none of the above yield object references
        
    case Op_TauPoint:
    case Op_TauEdge:
    case Op_TauAnd:
    case Op_TauUnsafe:
    case Op_TauSafe:
    case Op_TauCheckCast:
    case Op_TauHasType:
    case Op_TauHasExactType:
    case Op_TauIsNonNull:
        assert(0); // none of the above yield objects
        
    default:
        assert(0);
    }
    assert(0);
    return 0;
}

SsaTmpOpnd *SimplifyTaus::findTauSafeOpnd()
{
    if (!tauSafeOpnd) {
        Node *head = irManager.getFlowGraph().getEntryNode();
        Inst *entryLabel = (Inst*)head->getFirstInst();
        // first search for one already there
        Inst *inst = entryLabel->getNextInst();
        while (inst != NULL) {
            if (inst->getOpcode() == Op_TauSafe) {
                tauSafeOpnd = inst->getDst()->asSsaTmpOpnd();
                Inst *prevInst = inst->getPrevInst(); // make sure it's before any possible uses
                if ((prevInst != entryLabel) &&
                    (prevInst->getOpcode() != Op_DefArg)) {
                    do {
                        prevInst = prevInst->getPrevInst();
                    } while ((prevInst != entryLabel) &&
                             (prevInst->getOpcode() != Op_DefArg));
                    inst->unlink();
                    inst->insertAfter(prevInst);
                }
                return tauSafeOpnd;
            }
            inst = inst->getNextInst();
        }
        // need to insert one
        TypeManager &tm = irManager.getTypeManager();
        tauSafeOpnd = irManager.getOpndManager().createSsaTmpOpnd(tm.getTauType());
        Inst *tauSafeInst = irManager.getInstFactory().makeTauSafe(tauSafeOpnd);
        tauSafeInst->insertAfter(entryLabel);
    }
    return tauSafeOpnd;
}

VarOpnd *TauWalkerState::getTauForVarOpnd(VarOpnd *varOpnd, Type *type, bool exactType)
{
    // we may already have one
    Opnd *tauOpnd = lookupMapping(varOpnd, type, exactType);
    if (!tauOpnd) {
        // we need to build a tau for the base var
        TypeManager &tm = irManager.getTypeManager();
        // create a tau var
        tauOpnd = irManager.getOpndManager().createVarOpnd(tm.getTauType(), false);
        // insert it as the tau for the baseVar
        recordMapping(tauOpnd, varOpnd, type, exactType);
        
        // record that we found a new var that is getting a tau
        foundVarTau = true;
    }
    VarOpnd *tauVarOpnd = tauOpnd->asVarOpnd();
    assert(tauVarOpnd);
    return tauVarOpnd;
}

SsaVarOpnd *TauWalkerState::getTauForSsaVarOpnd(SsaVarOpnd *ssaVarOpnd, Type *type, bool exactType)
{
    // we may have already constructed a tau for this ssavar
    Opnd *tauSsaVarOpnd = lookupMapping(ssaVarOpnd, type, exactType);
    SsaVarOpnd *tauSsaVar = 0;
    if (tauSsaVarOpnd) {
        tauSsaVar = tauSsaVarOpnd->asSsaVarOpnd();
        assert(tauSsaVar);
    } else {
        // no luck, so consider the base var of the ssavar
        VarOpnd *baseVar = ssaVarOpnd->getVar();
        VarOpnd *baseTauVar = getTauForVarOpnd(baseVar, type, exactType);

        // create a tau SsaVar
        tauSsaVar = irManager.getOpndManager().createSsaVarOpnd(baseTauVar);
        // insert it as the tau for the SsaVar
        recordMapping(tauSsaVar, ssaVarOpnd, type, exactType);
    }
    return tauSsaVar;
}

// we want to construct a parallel tau structure to represent tauDst==HasType(src, type)
// for a ldVarInst
//     src = ldvar ssaVar
// so, we build or find (if already built) tau opnds
//     tauSsaVar==HasType(ssaVar, type)
//     baseTauOpnd==HasType(basVar, type)
//     tauSsaTmp==HasType(ssaVar, type)
// the caller will record the mapping
//     tauDst -> tauSsaTmp
// we also construct a ldVar
//     tauSsaTmp = ldvar tauSsaVar
// and insert it just after ldVarInst; this should be inserted before the current
// walk point, but if it's not, it will be ignored so it's not a problem.
Opnd *TauWalkerState::handleLdVar(Opnd *src, Type *type, Inst *ldVarInst, bool exactType)
{
    assert(src->getInst()->getOpcode() == Op_LdVar);

    // consider the src of the ldVarInst
    Opnd *ldVarOpnd = ldVarInst->getSrc(0);

    // create an SsaTmpVar for our tau
    TypeManager &tm = irManager.getTypeManager();
    SsaTmpOpnd *tauSsaTmp = irManager.getOpndManager().createSsaTmpOpnd(tm.getTauType());
    Inst *tauLdVarInst = 0;

    // handle both SSA and non-SSA
    if (ldVarOpnd->isSsaVarOpnd()) {
        SsaVarOpnd *ssaVarOpnd = ldVarOpnd->asSsaVarOpnd();
        SsaVarOpnd *tauSsaVarOpnd = getTauForSsaVarOpnd(ssaVarOpnd, type, exactType);

        tauLdVarInst = irManager.getInstFactory().makeLdVar(tauSsaTmp, tauSsaVarOpnd);
    } else {
        assert(ldVarOpnd->isVarOpnd());
        VarOpnd *varOpnd = ldVarOpnd->asVarOpnd();
        VarOpnd *tauVarOpnd = getTauForVarOpnd(varOpnd, type, exactType);

        tauLdVarInst = irManager.getInstFactory().makeLdVar(tauSsaTmp, tauVarOpnd);
    }
    tauLdVarInst->insertAfter(ldVarInst);


    return tauSsaTmp;
}

void TauWalkerState::recordOpndTypePair(Opnd *opnd, Type *type, bool exactType)
{
    if (exactType) {
        exactTypesMap.insert(opnd, type);
    } else {
        typesMap.insert(opnd, type);
    }
}

// when doing the var walk, determines if (opnd, type) is in the map for any types;
// if so, we return the set of types for which it is.
StlVectorSet<Type *> *TauWalkerState::shouldReduceSsaOpnd(Opnd *opnd, bool exactType)
{
    Type *varType = opnd->getType();
    if (!varType->isReference()) return 0;

    SsaVarOpnd *ssaVarOpnd = opnd->asSsaVarOpnd();
    if (ssaVarOpnd) {
        VarOpnd *varOpnd = ssaVarOpnd->getVar();
        assert(varOpnd);

        if (exactType)
            return exactTypesMap.lookup(varOpnd);
        else
            return typesMap.lookup(varOpnd);

    } else {
        VarOpnd *varOpnd = opnd->asVarOpnd();
        assert(varOpnd);

        if (exactType)
            return typesMap.lookup(varOpnd);
        else
            return typesMap.lookup(varOpnd);
    }
}

// when doing the var walk, determines if (opnd, type) is in the map;
// opnd is always an ssa/VarOpnd, though caller need not cast it in case the
//   type (which we check first) is not an object.
// if it is in the map, then returns the tau for the base var
VarOpnd *TauWalkerState::getReductionTauBaseVar(Opnd *opnd, Type *type, bool exactType)
{
    assert(opnd->getType()->isObject());

    SsaVarOpnd *ssaVarOpnd = opnd->asSsaVarOpnd();
    if (ssaVarOpnd) {
        VarOpnd *varOpnd = ssaVarOpnd->getVar();
        assert(varOpnd);
        
        Opnd *baseTauVarOpnd = lookupMapping(varOpnd, type, exactType);
        if (baseTauVarOpnd) {
            VarOpnd *baseTauVar = baseTauVarOpnd->asVarOpnd();
            assert(baseTauVar);
            if (Log::isEnabled()) {
                if (exactType)
                    Log::out() << "should reduce exact ssaVarOpnd ";
                else
                    Log::out() << "should reduce ssaVarOpnd ";
                opnd->print(Log::out());
                Log::out() << " with var ";
                varOpnd->print(Log::out());
                Log::out() << " to baseTau ";
                baseTauVar->print(Log::out());
                Log::out() << ::std::endl;
            }
            return baseTauVar;
        } else {
            if (Log::isEnabled()) {
                if (exactType)
                    Log::out() << "should not reduce exact ssaVarOpnd ";
                else
                    Log::out() << "should not reduce ssaVarOpnd ";
                opnd->print(Log::out());
                Log::out() << " with var ";
                varOpnd->print(Log::out());
                Log::out() << ::std::endl;
            }
            return 0;
        }
    } else {
        VarOpnd *varOpnd = opnd->asVarOpnd();
        assert(varOpnd);
        Opnd *baseTauVarOpnd = lookupMapping(varOpnd, type, exactType);

        if (baseTauVarOpnd) {
            VarOpnd *baseTauVar = baseTauVarOpnd->asVarOpnd();
            assert(baseTauVar);

            if (Log::isEnabled()) {
                if (exactType)
                    Log::out() << "should reduce exact VarOpnd ";
                else
                    Log::out() << "should reduce VarOpnd ";
                varOpnd->print(Log::out());
                Log::out() << " to baseTauVar ";
                baseTauVar->print(Log::out());
                Log::out() << ::std::endl;
            }
            return baseTauVar;
        } else {
            if (Log::isEnabled()) {
                if (exactType)
                    Log::out() << "should not reduce exact VarOpnd ";
                else
                    Log::out() << "should not reduce VarOpnd ";
                varOpnd->print(Log::out());
                Log::out() << ::std::endl;
            }
            return 0;
        }
    }
}

// for opnd, which may be Ssa/VarOpnd or SsaTmpOpnd, with the given tau base var,
//   finds or creates a corresponding tau opnd and returns it.
// (baseVar is needed as the base if we must create an SsaVarOpnd)
// type is the type which is used.
Opnd *TauWalkerState::reduceSsaOpnd(Opnd *opnd, VarOpnd *baseVar, Type *type, bool exactType)
{
    Opnd *mappedOpnd = 0;
    if (opnd->isVarOpnd()) {
        mappedOpnd = baseVar;
    } else {
        mappedOpnd = lookupMapping(opnd, type, exactType);
        if (!mappedOpnd) {
            // create one and map it;
            OpndManager &om = irManager.getOpndManager();
            if (opnd->isSsaVarOpnd()) {
                if (Log::isEnabled()) {
                    Log::out() << "creating reducedSsaOpnd for baseVar=";
                    baseVar->print(Log::out());
                    Log::out() << ::std::endl;
                }                
                mappedOpnd = om.createSsaVarOpnd(baseVar);
            } else if (opnd->isVarOpnd()) {
                return baseVar;
            } else {
                assert(opnd->isSsaTmpOpnd());
                if (Log::isEnabled()) {
                    Log::out() << "creating new tmp for baseVar=";
                    baseVar->print(Log::out());
                    Log::out() << ::std::endl;
                }                
                mappedOpnd = om.createSsaTmpOpnd(irManager.getTypeManager().getTauType());
            }
            recordMapping(mappedOpnd, opnd, type, exactType);
        }
    }
    if (Log::isEnabled()) {
        Log::out() << "reduceSsaOpnd(";
        opnd->print(Log::out());
        Log::out() << ", ";
        baseVar->print(Log::out());
        Log::out() << ", ";
        if (type)
            type->print(Log::out());
        else
            Log::out() << "NONNULL";
        Log::out() << ") -> ";
        mappedOpnd->print(Log::out());
        Log::out() << ::std::endl;
    }
    return mappedOpnd;
}

void TauWalkerState::reduceVarTaus(Inst *inst)
{
    Opcode opcode = inst->getOpcode();
    switch (opcode) {
    case Op_LdVar:
        {
        }
        break;
    case Op_StVar:
        {
            Opnd *opnd = inst->getDst();

            for (U_32 exactTypeCount = 0; exactTypeCount < 2; ++exactTypeCount) {
                bool exactType = (exactTypeCount != 0);
            
                StlVectorSet<Type *> *reduceTypes = shouldReduceSsaOpnd(opnd, exactType);
                if (reduceTypes) {

                    if (Log::isEnabled()) {
                        Log::out() << "reducing StVar inst ";
                        inst->print(Log::out());
                        Log::out() << ::std::endl;
                    }
                    
                    StlVectorSet<Type *>::iterator 
                        iter = reduceTypes->begin(),
                        end = reduceTypes->end();
                    for ( ; iter != end; ++iter) {
                        Type *type = *iter;
                        VarOpnd *baseTauOpnd = getReductionTauBaseVar(opnd, type, exactType);
                        
                        if (Log::isEnabled()) {
                            Log::out() << "reducing StVar inst ";
                            inst->print(Log::out());
                            if (exactType)
                                Log::out() << " for exact type ";
                            else
                                Log::out() << " for type ";
                            if (type)
                                type->print(Log::out());
                            else
                                Log::out() << "NONNULL";
                            Log::out() << ::std::endl;
                        }
                        
                        Opnd *newDst = reduceSsaOpnd(inst->getDst(), baseTauOpnd, type, exactType);
                        SsaVarOpnd *newDstSsaVarOpnd = newDst->asSsaVarOpnd();
                        if (newDstSsaVarOpnd) {
                            if (!newDstSsaVarOpnd->getInst()) {
                                Opnd *newSrc = findReplacement(inst->getSrc(0), type, exactType);
                                Inst *newInst = irManager.getInstFactory().makeStVar(newDstSsaVarOpnd, 
                                                                                     newSrc);
                                assert(newInst->getDst()->getInst() == newInst);
                                assert(newDstSsaVarOpnd->getInst()->getDst() == newDstSsaVarOpnd);
                                newInst->insertAfter(inst);
                                if (Log::isEnabled()) {
                                    Log::out() << "reduced StVar inst ";
                                    inst->print(Log::out());
                                    Log::out() << " to inst ";
                                    newInst->print(Log::out());
                                    Log::out() << ::std::endl;
                                }
                            } else {
                                if (Log::isEnabled()) {
                                    Log::out() << "tau opnd ";
                                    newDstSsaVarOpnd->print(Log::out());
                                    Log::out() << " already has an inst ";
                                    newDstSsaVarOpnd->getInst()->print(Log::out());
                                    Log::out() << ::std::endl;
                                }
                            }
                        } else {
                            VarOpnd *newDstVarOpnd = newDst->asVarOpnd();
                            assert(newDstVarOpnd);
                            Opnd *newSrc = findReplacement(inst->getSrc(0), type, exactType);
                            Inst *newInst = irManager.getInstFactory().makeStVar(newDstVarOpnd, 
                                                                                 newSrc);
                            newInst->insertAfter(inst);
                            if (Log::isEnabled()) {
                                Log::out() << "reduced StVar inst ";
                                inst->print(Log::out());
                                Log::out() << " to inst ";
                                newInst->print(Log::out());
                                Log::out() << ::std::endl;
                            }
                        }
                    }
                }
            }
        }
        break;
    case Op_Phi:
        {
            Opnd *opnd = inst->getDst();

            for (U_32 exactTypeCount = 0; exactTypeCount < 2; ++exactTypeCount) {
                bool exactType = (exactTypeCount != 0);

                StlVectorSet<Type *> *reduceTypes = shouldReduceSsaOpnd(opnd, exactType);
                if (reduceTypes) {

                    if (Log::isEnabled()) {
                        Log::out() << "reducing Phi inst ";
                        inst->print(Log::out());
                        Log::out() << ::std::endl;
                    }
                    
                    StlVectorSet<Type *>::iterator 
                        iter = reduceTypes->begin(),
                        end = reduceTypes->end();
                    for ( ; iter != end; ++iter) {
                        Type *type = *iter;
                        VarOpnd *baseTauOpnd = getReductionTauBaseVar(opnd, type, exactType);
                        
                        if (Log::isEnabled()) {
                            Log::out() << "reducing Phi inst ";
                            inst->print(Log::out());
                            if (exactType)
                                Log::out() << " for exact type ";
                            else
                                Log::out() << " for type ";
                            if (type)
                                type->print(Log::out());
                            else
                                Log::out() << "NONNULL";
                            Log::out() << ::std::endl;
                        }
                        
                        Opnd *newDst = reduceSsaOpnd(inst->getDst(), baseTauOpnd, type, exactType);
                        if (!newDst->getInst()){
                            U_32 numOpnds = inst->getNumSrcOperands();
                            Opnd** newOpnds = new (mm) Opnd*[numOpnds];
                            for (U_32 i=0; i<numOpnds; ++i) {
                                Opnd *oldOpnd = inst->getSrc(i);
                                Opnd *newOpnd = reduceSsaOpnd(oldOpnd, baseTauOpnd, type, exactType);
                                newOpnds[i] = newOpnd;
                            }
                            Inst *newInst = irManager.getInstFactory().makePhi(newDst, numOpnds, newOpnds);
                            newInst->insertAfter(inst);

                            if (Log::isEnabled()) {
                                Log::out() << "reduced Phi inst ";
                                inst->print(Log::out());
                                Log::out() << " to inst ";
                                newInst->print(Log::out());
                                Log::out() << ::std::endl;
                            }
                        } else {
                            if (Log::isEnabled()) {
                                Log::out() << "tau opnd ";
                                newDst->print(Log::out());
                                Log::out() << " already has an inst ";
                                newDst->getInst()->print(Log::out());
                                Log::out() << ::std::endl;
                            }
                        }
                    }
                }
            }
        }
        break;
    default:
        break;
    }
}

void TauWalkerState::replaceReducibleTaus(Inst *inst)
{
    Opnd *dstOp = inst->getDst();
    if (dstOp && (!dstOp->isNull()) && lookupCopyMapping(inst->getDst())) {
        // inst should be removed
        inst->unlink();
        return;
    }
    U_32 numSrcs = inst->getNumSrcOperands();
    for (U_32 i=0; i<numSrcs; ++i) {
        Opnd *srci = inst->getSrc(i);
        Opnd *found = lookupCopyMapping(srci);
        if (found) {
            inst->setSrc(i, found);
        }
    }
}


} //namespace Jitrino 
