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

#include <assert.h>
#include <iostream>
#include <algorithm>
#include "Stl.h"
#include "Log.h"
#include "open/types.h"
#include "Inst.h"
#include "irmanager.h"
#include "Dominator.h"
#include "Loop.h"
#include "Opcode.h"
#include "walkers.h"
#include "optpass.h"
#include "opndmap.h"

#include "memoryopt.h"
#include "aliasanalyzer.h"
#include "memoryoptrep.h"
#include "./ssa/SSA.h"
#include "XTimer.h"
#include "hashvaluenumberer.h"

namespace Jitrino {

DEFINE_SESSION_ACTION(MemoryValueNumberingPass, memopt, "Redundant Ld-St Elimination")

void
MemoryValueNumberingPass::_run(IRManager& irm) { 
    computeDominators(irm);
    DominatorTree* dominatorTree = irm.getDominatorTree();
    assert(dominatorTree && dominatorTree->isValid());

    computeLoops(irm);
    LoopTree* loopTree = irm.getLoopTree();
    assert(loopTree && loopTree->isValid());

    ControlFlowGraph& flowGraph = irm.getFlowGraph();
    MemoryManager& memoryManager = irm.getNestedMemoryManager();

    DomFrontier frontier(memoryManager,*dominatorTree,&flowGraph);
    TypeAliasAnalyzer aliasAnalyzer;
    MemoryOpt mopt(irm, memoryManager, *dominatorTree, 
                   frontier, loopTree, &aliasAnalyzer);
    mopt.runPass();

    HashValueNumberer valueNumberer(irm, *dominatorTree);
    valueNumberer.doValueNumbering(&mopt);
    bool do_redstore = irm.getOptimizerFlags().memOptFlags->do_redstore;
    if (do_redstore) {
        mopt.eliminateRedStores();
    }
}

MemoryOpt::MemoryOpt(IRManager &irManager0, 
                     MemoryManager& memManager,
                     DominatorTree& dom0,
                     DomFrontier& df0,
                     LoopTree *loopTree0,
                     AliasAnalyzer *aa0)
    : irManager(irManager0), 
      fg(irManager0.getFlowGraph()),
      mm(memManager),
      dominators(dom0),
      df(df0),
      loopTree(loopTree0),
      aliasManager(new (memManager) AliasManager(memManager, aa0, &irManager0.getTypeManager())),
      instDoesWhat(new Inst2MemBehavior(memManager)),
      aliasDefSites(0),
      memPhiSites(0),
      renameMap(0),
      memUseDefs(memManager),
      memDefUses(memManager),
      flags(*irManager.getOptimizerFlags().memOptFlags)
{
    assert(aliasManager);

    U_32 numNodes = df0.getNumNodes();
    aliasDefSites = new (mm) AliasDefSites(mm, numNodes);
    memPhiSites = new (mm) MemPhiSites(mm, numNodes);
    renameMap = new (mm) AliasRenameMap(mm, aliasManager,
                                        2*numNodes,
                                        2*numNodes);
}

MemoryOpt::~MemoryOpt()
{
}

void 
MemoryOpt::readFlags(Action* argSource, MemoptFlags* flags)
{
    IAction::HPipeline p = NULL; //default pipeline for argSource

    const char *res = argSource->getStringArg(p, "memopt.mm", NULL);
    flags->model = Model_Default;
    if (res) {
        if (strncmp(res,"strict",7)==0) {
            flags->model = Model_Strict;
        } else if (strncmp(res,"reads_kill",11)==0) {
            flags->model = Model_ReadsKill;
        } else if (strncmp(res,"cse_final",9)==0) {
            flags->model = Model_CseFinal;
        }
    }
    res = argSource->getStringArg(p, "memopt.synch", NULL);
    flags->synch = Synch_Default;
    if (res) {
        if (strncmp(res,"fence", 6)==0) {
            flags->synch = Synch_Fence;
        } else if (strncmp(res,"moveable", 9)==0) {
            flags->synch = Synch_Moveable;
        } else if (strncmp(res,"only_lock", 10)==0) {
            flags->synch = Synch_OnlyLock;
        } else if (strncmp(res,"one_thread", 11)==0) {
            flags->synch = Synch_OneThread;
        }
    }
    flags->debug = argSource->getBoolArg(p, "memopt.debug", false);
    flags->verbose = argSource->getBoolArg(p, "memopt.verbose", false);
    flags->redstore = argSource->getBoolArg(p, "memopt.redstore", false);
    flags->syncopt = argSource->getBoolArg(p, "memopt.syncopt", false);
    flags->do_redstore = argSource->getBoolArg(p, "memopt.do_redstore", true);

}

void MemoryOpt::showFlags(std::ostream& os) {
    os << "  memopt flags:"<<std::endl;
    os << "    memopt.mm=strict           - no memory optimizations" << std::endl;
    os << "    memopt.mm=reads_kill       - reads kill" << std::endl;
    os << "    memopt.mm=cse_final        - allow final field CSEing, maybe hoisting" << std::endl;
    os << "    memopt.synch=fence         - no memory ops move past lock/unlock" << std::endl;
    os << "    memopt.synch=moveable      - load/store movement into locks allowed" << std::endl;
    os << "    memopt.synch=only_lock     - eliminable local objects not treated as fence" << std::endl;
    os << "    memopt.synch=one_thread    - locks and fences elided freely" << std::endl;
    os << "    memopt.debug[={ON,off}]    - debug memory opts" << std::endl;
    os << "    memopt.verbose[={ON,off}]  - verbose memory opt output" << std::endl;
    os << "    memopt.redstore[={on,OFF}] - try to eliminate redundant stores" << std::endl;
    os << "    memopt.syncopt[={on,OFF}]  - turn exit/enter into fences" << std::endl;
}

static CountTime memoptPhase1Timer("opt::mem::phase1"); // not thread-safe
static CountTime memoptPhase2Timer("opt::mem::phase2"); // not thread-safe
static CountTime memoptPhase3Timer("opt::mem::phase3"); // not thread-safe

void MemoryOpt::runPass()
{
    if (Log::isEnabled()) {
        Log::out() << "Starting MemoryOpt Pass" << std::endl;
    }
    // initialize iDef, iUse for each instr, build initial set of locations
    {
        AutoTimer t(memoptPhase1Timer); 
        initMemoryOperations(); 
    }

    // walk dominator tree in pre-order,
    //   building MustDef, MayDef, MustUse for each instruction
    //   (1) when we see iDef=loc at instruction i
    //      create loc1 = new SsaMemOpnd(loc), defined by i
    //      set renameTable(loc)=loc1
    //      set MustDef(i)=loc1
    //   (2) for each loc'!=loc defined in renameTable s.t. mayalias(loc',loc)
    //      create loc'' = AliasSet(loc,loc')
    //      create loc2 = new SsaMemOpnd(loc''), defined by i
    //      set renameTable(loc') = loc2
    //      set MayDef(i)=loc2
    //   (3) when we see iUse=loc at instruction i
    //      if lookup loc3=renameTable(loc) is defined
    //         set MayUse(i)=loc3
    //      if reads_kill, then also see (3)
    //   (4) when we enter a block, consider the iPhi functions on the block:
    //      for each loc=iPhi(loc1,loc2,...),
    //         
    // 
    if (flags.redstore) {
        if (Log::isEnabled()) {
            Log::out() << "Trying redundant store elimination" << std::endl;
        }
        AutoTimer t(memoptPhase2Timer); 
        eliminateRedStores();
    }
    if (Log::isEnabled()) {
        Log::out() << "Finished MemoryOpt Pass" << std::endl;
    }

    if (flags.syncopt) {
        AutoTimer t(memoptPhase3Timer); 
        if (Log::isEnabled()) {
            Log::out() << "Trying synchronization optimization" << std::endl;
        }
        doSyncOpt();
    }
    if (Log::isEnabled()) {
        Log::out() << "Finished synchronization optimization" << std::endl;
    }
}

// a NodeInstWalker, backwards over instructions
class MemoryOptInitWalker {
    MemoryOpt *thePass;
    Node *n;
public:
    MemoryOptInitWalker(MemoryOpt *thePass0) :
        thePass(thePass0), n(0)
    { };
    void startNode(Node *node) { n = node; };
    void applyToInst(Inst *inst);
    void finishNode(Node *node) {};
};

// Phase 1: for each memory access, get an description of the set of
// memory locations which must be considered with it.

static CountTime memoptPhase1bTimer("opt::mem::phase1b"); // not thread-safe
static CountTime memoptPhase1cTimer("opt::mem::phase1c"); // not thread-safe
static CountTime memoptPhase1dTimer("opt::mem::phase1d"); // not thread-safe

void MemoryOpt::initMemoryOperations()
{
    // initialize memory locations read/written by each instruction
    MemoryOptInitWalker initWalker(this);
    typedef NodeInst2NodeWalker<false, MemoryOptInitWalker> InitNodeWalker;
    InitNodeWalker initNodeWalker(initWalker);

    if (Log::isEnabled()) {
        Log::out() << std::endl << "Instruction Memory Effects:" << std::endl;
    }

    {
        AutoTimer t(memoptPhase1bTimer); 
    // have the first inst, define Any for renaming walk
    DominatorNode *domRoot = dominators.getDominatorRoot();
    Node *cfgRoot = domRoot->getNode();
    Inst *firstInst = (Inst*)cfgRoot->getFirstInst();
    assert(firstInst->isLabel());
    AliasRep escapingMem = aliasManager->getAny();
    renameMap->insert(escapingMem, firstInst);
    }

    {
        AutoTimer t(memoptPhase1cTimer); 
    // walk over all instructions, inserting memory effects
    NodeWalk<InitNodeWalker>(fg, initNodeWalker);
    if (Log::isEnabled()) {
        Log::out() << std::endl << "END of Instruction Memory Effects" << std::endl;
    }
    insertMemPhi();
    }    

    if (Log::isEnabled()) {
        aliasManager->dumpAliasReps(Log::out());
    }

    {
        AutoTimer t(memoptPhase1dTimer); 
    createUsesMap();
    }
}


/* 
   0. MemOpnd has 
      - ptr to address operand(s) read/written
      - ptr to defining instruction (normal opt) or block (for a phi)

   1. each memory-altering instr at node n
          generates a unique MemOpnd, 
             adds a memPhi node with new MemOpnd at each element of DF+(n)
          uses a uniqe MemOpnd
             ? adds a memSplit node with new input MemOpnd at each element
             ? of PDF+(n)
   2. walk dominator tree in pre-order, carrying set L of live MemOpnd
          (1) at each memory instruction i,
              (a) consider input MemOpnd mo_in, find narrowest mo_1 in L 
                  containing the mo_in
                  - union(mo_in, mo_1)
              (b) consider output MemOpnd mo_out
                  - remove from L any mo_l contained in mo_out
                  - add mo_out to L
          (2) at each outedge leading to a memPhi node for mo_in,
              (a) find the narrowest mo_l in L containing mo_in, add
                  mo_l as a parameter to the memPhi

          (3) for each inedge leading from a memSplit node with outedge
              mo_out, add mo_out to L


*/            

InstMemBehavior *
MemoryOpt::getInstEffect(Inst *i)
{
    Inst2MemBehavior::iterator
        found = instDoesWhat->find(i),
        end = instDoesWhat->end();
    if (found != end) {
        return (*found).second;
    } else {
        return 0;
    }
}

InstMemBehavior *
MemoryOpt::getOrCreateInstEffect(Inst *i)
{
    Inst2MemBehavior::iterator
        found = instDoesWhat->find(i),
        end = instDoesWhat->end();
    if (found != end) {
        return (*found).second;
    } else {
        InstMemBehavior *imb = new (mm) InstMemBehavior(mm);
        (*instDoesWhat)[i] = imb;
        return imb;
    }
}

// R/W anything that escapes or is global
void MemoryOpt::effectAnyGlobal(Node *n, Inst *i)
{
    AliasRep anyGlobal = aliasManager->getAnyGlobal();
    addUseToInstruction(n, i, anyGlobal);
    addDefToInstruction(n, i, anyGlobal);
}

// writes opnd.vptr
void MemoryOpt::effectWriteVtable(Node *n, Inst *i, Opnd *opnd)
{
    AliasRep vtblMemory = aliasManager->getVtableOf(opnd);
    addDefToInstruction(n, i, vtblMemory);
}

void MemoryOpt::effectReadVtable(Node *n, Inst *i, Opnd *opnd)
{
    AliasRep vtblMemory = aliasManager->getVtableOf(opnd);
    addUseToInstruction(n, i, vtblMemory);
}

void MemoryOpt::effectReadMethodPtr(Node *n, Inst *i, Opnd *obj, MethodDesc *desc)
{
    AliasRep methodPtrMem = aliasManager->getMethodPtr(obj, desc);
    addUseToInstruction(n, i, methodPtrMem);
}

void MemoryOpt::effectReadMethodPtr(Node *n, Inst *i, MethodDesc *desc)
{
    NamedType *theType = desc->getParentType();
    AliasRep typeVtable = aliasManager->getVtableOf(theType);
    addUseToInstruction(n, i, typeVtable);
}

void MemoryOpt::effectReadFunPtr(Node *n, Inst *i, Opnd *fnptr)
{

}

void MemoryOpt::effectExit(Node *n, Inst *i)
{
    AliasRep escapingMem = aliasManager->getAnyEscaping();
    addUseToInstruction(n, i, escapingMem);
}

void MemoryOpt::effectInit(Node *n, Inst *i)
{
    AliasRep escapingMem = aliasManager->getAny();
    addDefToInstruction(n, i, escapingMem);
}

void MemoryOpt::effectEntry(Node *n, Inst *i)
{
    AliasRep escapingMem = aliasManager->getAnyEscaping();
    addDefToInstruction(n, i, escapingMem);
}

void MemoryOpt::effectRead(Node *n, Inst *i, Opnd *addr)
{
    AliasRep thisMem = aliasManager->getReference(addr);
    addUseToInstruction(n, i, thisMem);
}

void MemoryOpt::effectWrite(Node *n, Inst *i, Opnd *addr)
{
    AliasRep thisMem = aliasManager->getReference(addr);
    addDefToInstruction(n, i, thisMem);
}

void MemoryOpt::effectReadClassVtable(Node *n, Inst *i, NamedType *t)
{
    AliasRep vtblMemory = aliasManager->getVtableOf(t);
    addDefToInstruction(n, i, vtblMemory);
}

void MemoryOpt::effectWriteArrayLength(Node *n, Inst *i, Opnd *opnd)
{
    AliasRep arrayLenMem = aliasManager->getArrayLen(opnd);
    addDefToInstruction(n, i, arrayLenMem);
}

void MemoryOpt::effectReadArrayLength(Node *n, Inst *i, Opnd *opnd)
{
    AliasRep arrayLenMem = aliasManager->getArrayLen(opnd);
    addUseToInstruction(n, i, arrayLenMem);
}

void MemoryOpt::effectWriteArrayElements(Node *n, Inst *i, Opnd *array,
                                         Opnd *offset, Opnd *length)
{
    AliasRep arrayElemMem = aliasManager->getArrayElements(array, offset, length);
    addDefToInstruction(n, i, arrayElemMem);
}

void MemoryOpt::effectReadArrayElements(Node *n, Inst *i, Opnd *array,
                                        Opnd *offset, Opnd *length)
{
    AliasRep arrayElemMem = aliasManager->getArrayElements(array, offset, length);
    addUseToInstruction(n, i, arrayElemMem);
}

// creates an object/array, returned in opnd:
//   writes array length, etc.
void MemoryOpt::effectNew(Node *n, Inst *i, Opnd *dstop)
{
    AliasRep objInitMem = aliasManager->getObjectNew(dstop);
    addDefToInstruction(n, i, objInitMem);
}

// make sure object's vtable are visible to others before publishing this
void MemoryOpt::effectReleaseObject(Node *n, Inst *i, Opnd *obj)
{
    addReleaseToInstruction(n, i);
}

// make sure object's vtable is available to all
void MemoryOpt::effectInitType(Node *n, Inst *i, NamedType *type)
{
    AliasRep typeInitMem = aliasManager->getTypeNew(type);
    addDefToInstruction(n, i, typeInitMem);
}

// make sure object's vtable is available to all
void MemoryOpt::effectFinishObject(Node *n, Inst *i, Opnd *obj)
{
    AliasRep objFinishMem = aliasManager->getFinishObject(obj);
    addDefToInstruction(n, i, objFinishMem);
}

// make sure object's vtable is available to all
void MemoryOpt::effectFinishType(Node *n, Inst *i, NamedType *type)
{
    AliasRep typeFinishMem = aliasManager->getFinishType(type);
    addDefToInstruction(n, i, typeFinishMem);
}

// can commute with any ops, but not be added/removed:
void MemoryOpt::effectIncCounter(Node *n, Inst *i)
{
}

// object may be 0 if lock has been removed
void MemoryOpt::effectIncRecCount(Node *n, Inst *i, Opnd *object)
{
    if (object) {
        AliasRep thisMem = aliasManager->getLock(object);

        addUseToInstruction(n, i, thisMem);
        addDefToInstruction(n, i, thisMem);
    }
}

// object may be 0 if lock has been removed
void MemoryOpt::effectMonitorEnter(Node *n, Inst *i, Opnd *object)
{
    if (object) {
        AliasRep thisMem = aliasManager->getLock(object);

        addUseToInstruction(n, i, thisMem);
        addDefToInstruction(n, i, thisMem);
    }
    addAcquireToInstruction(n, i);
}

void MemoryOpt::effectMonitorExit(Node *n, Inst *i, Opnd *object)
{
    if (object) {
        AliasRep thisMem = aliasManager->getLock(object);

        addUseToInstruction(n, i, thisMem);
        addDefToInstruction(n, i, thisMem);
    }
    addReleaseToInstruction(n, i);
}

// object may be 0 if lock has been removed
void MemoryOpt::effectTypeMonitorEnter(Node *n, Inst *i, Type *type)
{
    assert(type);

    AliasRep thisMem = aliasManager->getLock(type);

    addUseToInstruction(n, i, thisMem);
    addDefToInstruction(n, i, thisMem);
    addAcquireToInstruction(n, i);
}

void MemoryOpt::effectTypeMonitorExit(Node *n, Inst *i, Type *type)
{
    assert(type);

    AliasRep thisMem = aliasManager->getLock(type);
    addUseToInstruction(n, i, thisMem);
    addDefToInstruction(n, i, thisMem);
    addReleaseToInstruction(n, i);
}

void MemoryOpt::addDefToInstruction(Node *n, Inst *i, const AliasRep &thisMem)
{
    InstMemBehavior *b = getOrCreateInstEffect(i);
    b->defs.insert(thisMem);

    if (Log::isEnabled()) {
        Log::out() << "Inserting def for ";
        thisMem.print(Log::out());
        Log::out() << " at node ";
        FlowGraph::printLabel(Log::out(), n);
        Log::out() << std::endl;
    }

    // if there is already a definition of an ancestor of this one, then it 
    // hides this, so skip adding the def
    const AliasManager::AliasList *alist = aliasManager->getAncestors(thisMem);
    AliasManager::AliasList::const_iterator iter = alist->begin();
    AliasManager::AliasList::const_iterator endIter = alist->end();
    AliasDefSites::iterator end = aliasDefSites->end();
    for ( ; iter != endIter; ++iter) {
        AliasRep anc = *iter;
        if (aliasDefSites->hasAliasDefSite(anc, n)) {
            if (Log::isEnabled()) {
                Log::out() << "Not recording def for ";
                thisMem.print(Log::out());
                Log::out() << " at node ";
                FlowGraph::printLabel(Log::out(), n);
                Log::out() << " because ancestor ";
                anc.print(Log::out());
                Log::out() << " has def there" << std::endl;
            }
            return;
        }
    }
    // if there is no ancestor definition, add a definition for thisMem to the node.
    aliasDefSites->addAliasDefSite(thisMem, n);
}

void MemoryOpt::addUseToInstruction(Node *n, Inst *i, const AliasRep &thisMem)
{
    InstMemBehavior *b = getOrCreateInstEffect(i);
    b->uses.insert(thisMem);
}

void MemoryOpt::addReleaseToInstruction(Node *n, Inst *i)
{
    InstMemBehavior *b = getOrCreateInstEffect(i);
    b->release = true;
}

void MemoryOpt::addAcquireToInstruction(Node *n, Inst *i)
{
    InstMemBehavior *b = getOrCreateInstEffect(i);
    b->acquire = true;
}

void
MemoryOptInitWalker::applyToInst(Inst *i)
{
    switch (i->getOpcode()) {

    case Op_DirectCall: 
        thePass->effectAnyGlobal(n, i); break;
    case Op_TauVirtualCall: 
        {
            // Src(0) and Src(1) are taus
            // calls (i->getSrc(2)).method, if we ever get IPA
            MethodCallInst *calli = i->asMethodCallInst();
            
            thePass->effectReadVtable(n, i, i->getSrc(2));
            thePass->effectReadMethodPtr(n, i, i->getSrc(2), 
                                         calli->getMethodDesc());
            thePass->effectAnyGlobal(n, i);
        }
        break;
    case Op_IndirectCall:
    case Op_IndirectMemoryCall:
        {
            // calls i->getSrc(0), if we ever get IPA
            thePass->effectReadFunPtr(n, i, i->getSrc(0));
            thePass->effectAnyGlobal(n, i);
        }
        break;
    case Op_VMHelperCall:
        break;
    case Op_JitHelperCall:
        {
            JitHelperCallInst *jitcalli = i->asJitHelperCallInst();
            JitHelperCallId callId = jitcalli->getJitHelperId();
            switch (callId) {
            case Prefetch:
            case Memset0:
            case InitializeArray:
            case SaveThisState:
            case ReadThisState:
            case LockedCompareAndExchange:
            case AddValueProfileValue:
            case ArrayCopyDirect:
            case ArrayCopyReverse:
            case StringCompareTo:
            case StringIndexOf:
            case StringRegionMatches:
            case FillArrayWithConst: 
            case ClassIsArray:
            case ClassGetAllocationHandle:
            case ClassGetTypeSize:
            case ClassGetArrayElemSize:
            case ClassIsInterface:
            case ClassIsFinal:
            case ClassGetArrayClass:
            case ClassIsFinalizable:
            case ClassGetFastCheckDepth:
                break;
            default:
                assert(0);
                break;
            }
        }
        break;
    case Op_PseudoThrow:
        break;
    case Op_Return:
    case Op_Throw:
    case Op_ThrowSystemException:
    case Op_ThrowLinkingException:
        thePass->effectExit(n, i);
        break;
    case Op_Catch:
        thePass->effectEntry(n, i);
        break;

    case Op_Prefetch:
    case Op_Ret:
    case Op_SaveRet:
    case Op_JSR:
        break;

    case Op_TauLdInd:
        thePass->effectRead(n, i, i->getSrc(0));
        break;

    case Op_TauLdIntfcVTableAddr:
        {
            TypeInst *typeInst = i->asTypeInst();
            Type *vtableType = typeInst->getTypeInfo();
            assert(vtableType->isUserObject());
            NamedType *namedType = (NamedType*)vtableType;

            thePass->effectReadClassVtable(n, i, namedType);
            thePass->effectReadVtable(n, i, i->getSrc(0));
        }
        break;

    case Op_TauLdVirtFunAddr:
    case Op_TauLdVirtFunAddrSlot:
        {
            Opnd *vtableOpnd = i->getSrc(0);

            thePass->effectRead(n, i, vtableOpnd);
        }
        break;

    case Op_LdFunAddr:
    case Op_LdFunAddrSlot:
        {
            MethodInst *methodInst = i->asMethodInst();
            MethodDesc *methodDesc = methodInst->getMethodDesc();

            thePass->effectReadMethodPtr(n, i, methodDesc);
        }
        break;

    case Op_TauArrayLen:
        {
            thePass->effectReadArrayLength(n, i, i->getSrc(0));
        }
        break;

    case Op_TauStInd:
        {
            thePass->effectWrite(n, i, i->getSrc(1));
        }
        break;

    case Op_TauStRef:
        {
            thePass->effectWrite(n, i, i->getSrc(1));
        }
        break;

    case Op_TauCheckElemType:
        {
            thePass->effectReadVtable(n, i, i->getSrc(0)); // array
            thePass->effectReadVtable(n, i, i->getSrc(1)); // to store
        }
        break;

    case Op_NewObj:
        {
            thePass->effectNew(n, i, i->getDst());
            thePass->effectWriteVtable(n, i, i->getDst());
            thePass->effectReleaseObject(n, i, i->getDst());
        }
        break;

    case Op_NewMultiArray:
    case Op_NewArray:
        {
            thePass->effectNew(n, i, i->getDst());
            thePass->effectWriteVtable(n, i, i->getDst());
            thePass->effectWriteArrayLength(n, i, i->getDst());
            thePass->effectReleaseObject(n, i, i->getDst());
        }
        break;

    case Op_GetClassObj:
        {
            thePass->effectNew(n, i, i->getDst());
            thePass->effectWriteVtable(n, i, i->getDst());
            thePass->effectReleaseObject(n, i, i->getDst());
        }
        break;

    case Op_TypeMonitorEnter:
        {
            TypeInst *tinst = i->asTypeInst();
            Type *thetype = tinst->getTypeInfo();
            thePass->effectTypeMonitorEnter(n, i, thetype);
        }
        break;

    case Op_TypeMonitorExit:
        {
            TypeInst *tinst = i->asTypeInst();
            Type *thetype = tinst->getTypeInfo();
            thePass->effectTypeMonitorExit(n, i, thetype);
        }
        break;

    case Op_TauMonitorEnter:
        {
            thePass->effectMonitorEnter(n, i, i->getSrc(0));
        }
        break;

    case Op_TauMonitorExit:
        {
            thePass->effectMonitorExit(n, i, i->getSrc(0));
        }
        break;

    case Op_LdLockAddr:
        // just an address computation
        break;

    case Op_IncRecCount:
        // just an address computation
        break;

    case Op_TauBalancedMonitorEnter:
        {
            thePass->effectMonitorEnter(n, i, i->getSrc(0));
        }
        break;
    case Op_BalancedMonitorExit:
        {
            thePass->effectMonitorExit(n, i, i->getSrc(0));
        }
        break;
    case Op_TauOptimisticBalancedMonitorEnter:
        {
            thePass->effectMonitorEnter(n, i, i->getSrc(0));
        }
        break;
    case Op_OptimisticBalancedMonitorExit:
        {
            thePass->effectMonitorExit(n, i, i->getSrc(0));
        }
        break;
    case Op_MonitorEnterFence:
        {
            thePass->effectMonitorEnter(n, i, 0);
        }
        break;
    case Op_MonitorExitFence:
        {
            thePass->effectMonitorExit(n, i, 0);
        }
        break;

    case Op_TauCast:
    case Op_TauAsType:
    case Op_TauInstanceOf:
    case Op_TauCheckCast:
        {
            thePass->effectReadVtable(n, i, i->getSrc(0));
        }
        break;
    case Op_InitType:
        {
            TypeInst *typeInst = i->asTypeInst();
            thePass->effectAnyGlobal(n, i); // could modify unrelated globals
            thePass->effectInitType(n, i, (NamedType *)typeInst->getTypeInfo());
        }
        break;

    case Op_MethodEntry:
        // this is just a marker for an inlined method, don't use the following effect:
        // thePass->effectEntry(n, i);
        break;

    case Op_MethodEnd:
        {
            MethodMarkerInst *mmi = i->asMethodMarkerInst();
            assert(mmi);
            MethodDesc *md = mmi->getMethodDesc();
            if (md->isInstanceInitializer()) {
                if (mmi->getNumSrcOperands() == 1) {
                    Opnd *obj = mmi->getSrc(0);
                    thePass->effectFinishObject(n, i, obj);
                } else {
                    // static method, skip it
                }
            } else if (md->isClassInitializer()) {
                thePass->effectFinishType(n, i, md->getParentType());
            }
        }
        break;

    case Op_IncCounter:
        thePass->effectIncCounter(n, i);
        break;


        // loads vtable from object, depends on object initialization
    case Op_TauLdVTableAddr:

        // THE FOLLOWING ARE JUST ADDRESS COMPUTATIONS
    case Op_LdVarAddr:
        // mark Var as addr_taken
    case Op_LdFieldAddr:
    case Op_LdStaticAddr:
    case Op_LdElemAddr:
    case Op_GetVTableAddr:
    case Op_LdArrayBaseAddr:
    case Op_AddScaledIndex:


    case Op_UncompressRef:
    case Op_CompressRef:
    case Op_LdFieldOffset:
    case Op_LdFieldOffsetPlusHeapbase:
    case Op_LdArrayBaseOffset:
    case Op_LdArrayBaseOffsetPlusHeapbase:
    case Op_LdArrayLenOffset:
    case Op_LdArrayLenOffsetPlusHeapbase:
    case Op_AddOffset:
    case Op_AddOffsetPlusHeapbase:
        
        // the following are irrelevant, but cased so we
        // notice any additions:

    case Op_Add: case Op_Mul: case Op_Sub: case Op_TauDiv: case Op_TauRem: 
    case Op_Neg: case Op_MulHi: case Op_Min: case Op_Max: case Op_Abs:
    case Op_And: case Op_Or: case Op_Xor: 
    case Op_Not: case Op_Select: case Op_Conv: case Op_ConvZE: case Op_ConvUnmanaged: case Op_Shladd: case Op_Shl: 
    case Op_Shr: case Op_Cmp: case Op_Cmp3: 
    case Op_Branch: case Op_Jump: case Op_Switch:
    case Op_LdConstant: 
    case Op_LdRef: // helper call to load a constant, no memory effect.

    case Op_Copy:
    case Op_StVar:
    case Op_LdVar:
        // Reads a variable; may need to check for addr-taken
    case Op_DefArg:
        // Reads stack; may need to check for addr-taken

    case Op_TauCheckBounds: case Op_TauCheckLowerBound: case Op_TauCheckUpperBound:
    case Op_TauCheckNull: case Op_TauCheckZero: case Op_TauCheckDivOpnds:
    case Op_TauCheckFinite: 

    case Op_TauStaticCast: // just a compile-time assertion
    case Op_Label:
    case Op_Phi:
    case Op_TauPi:
    case Op_TauPoint:
    case Op_TauEdge:
    case Op_TauAnd:
    case Op_TauUnsafe:
    case Op_TauSafe:
    case Op_TauHasType:
    case Op_TauHasExactType:
    case Op_TauIsNonNull:
        break;

    //case Op_TauStRef:
    //    assert(0); 

    case Op_TauLdField:
    case Op_LdStatic:
    case Op_TauLdElem:

    case Op_TauStField:
    case Op_TauStElem:
    case Op_TauStStatic:
        assert(0);
        break;

    default:
        assert(0);
        break;
    }
    if (Log::isEnabled()) {
        Log::out() << "Inst "; i->print(Log::out());
        Log::out() << std::endl;
        InstMemBehavior *b = thePass->getInstEffect(i);
        if (b) {
            StlVectorSet<AliasRep>::iterator 
                iter1 = b->defs.begin(),
                end1 = b->defs.end();
            if (iter1 != end1) {
                Log::out() << "        def: ";
                for ( ; iter1 != end1; iter1++) {
                    const AliasRep &rep = *iter1;
                    rep.dump(Log::out());
                    Log::out() << " ";
                }
                Log::out() << std::endl;
            }
            StlVectorSet<AliasRep>::iterator 
                iter2 = b->uses.begin(),
                end2 = b->uses.end();
            if (iter2 != end2) {
                Log::out() << "        use: ";
                for ( ; iter2 != end2; iter2++) {
                    const AliasRep &rep = *iter2;
                    rep.dump(Log::out());
                    Log::out() << " ";
                }
                Log::out() << std::endl;
            }
        }
    }
}

bool AliasManager::mayAlias(const AliasRep &a, const AliasRep &b)
{
    if ((a.kind == AliasRep::NullKind) || (b.kind == AliasRep::NullKind)) return false;
    if ((a.kind == AliasRep::AnyKind) || (b.kind == AliasRep::AnyKind)) return true;
    if (a.kind == AliasRep::GlobalKind) {
        if ((b.kind == AliasRep::LocalKind) || 
            ((b.opnd != 0) && !analyzer->mayEscape(b.opnd))) {
            return false;
        } else {
            return true;
        }
    }
    if (b.kind == AliasRep::GlobalKind) {
        if ((a.kind == AliasRep::LocalKind) || 
            ((a.opnd != 0) && !analyzer->mayEscape(a.opnd))) {
            return false;
        }
        return true;
    }
    if (a.kind == AliasRep::LocalKind) {
        if ((b.kind == AliasRep::GlobalKind) || 
            ((b.opnd == 0) || analyzer->mayEscape(b.opnd))) {
            return false;
        }
        return true;
    }
    if (b.kind == AliasRep::LocalKind) {
        if ((a.kind == AliasRep::GlobalKind) || 
            ((a.opnd == 0) || analyzer->mayEscape(a.opnd))) {
            return false;
        }
        return true;
    }
    if (a.kind == AliasRep::FinishObjectKind) {
        return (b.isObjectFinal());
    }
    if (b.kind == AliasRep::FinishObjectKind) {
        return (a.isObjectFinal());
    }
    if (a.kind == AliasRep::FinishTypeKind) {
        return (b.isTypeFinal());
    }
    if (b.kind == AliasRep::FinishTypeKind) {
        return (a.isTypeFinal());
    }

    if (a.kind != b.kind) return false;
    if (a.type != b.type) return false;
    if (a.desc != b.desc) return false;
    if (a.opnd && b.opnd && !analyzer->mayAlias(a.opnd, b.opnd)) return false;
    return true;
}

void AliasRep::dump(::std::ostream &os) const
{
    switch (kind) {
    case NullKind: os << "Null"; break;
    case GlobalKind: os << "Global"; break;
    case LocalKind: os << "Local"; break;
    case AnyKind: os << "Any"; break;
        
    case UnknownKind: os << "Unknown"; break;
        
    case ObjectFieldKind: os << "ObjectField"; break;
    case UnresolvedObjectFieldKind: os << "UnresolvedObjectField"; break;
    case ArrayElementKind: os << "ArrayElement"; break;
        
    case StaticFieldKind: os << "StaticField"; break;
    case UnresolvedStaticFieldKind: os << "UnresolvedStaticField"; break;

    case ObjectVtableKind: os << "ObjectVtable"; break;
    case MethodPtrKind: os << "MethodPtr"; break;
    case FunPtrKind: os << "FunPtr"; break;
    case TypeVtableKind: os << "TypeVtable"; break;
    case ArrayLenKind: os << "ArrayLen"; break;
    case NewObjectKind: os << "NewObject"; break;
    case NewTypeKind: os << "NewType"; break;

    case FinishObjectKind: os << "Final"; break;
    case FinishTypeKind: os << "StaticFinal"; break;
    case LockKind: os << "Lock"; break;
    case TypeLockKind: os << "TypeLock"; break;
    default:
        assert(0);
    }
    if (type || desc || opnd) {
        os << "[";
        if (opnd) { opnd->print(os); if (type || desc) os << ","; };
        if (enclClass) {enclClass->print(os); os<<","; idx->print(os);}
        if (type) { type->print(os); if (desc) os << ","; };
        if (desc) desc->printFullName(os);
        os << "]";
    }
    if (id) {
        os << "(" << id << ")";
    }
}

void AliasRep::print(::std::ostream &os) const
{
    dump(os);
}

bool AliasRep::isObjectFinal() const {
    switch (kind) {
    case ObjectFieldKind:
        {
            FieldDesc *field = (FieldDesc *) desc;
            return field->isInitOnly();
        };

    case ObjectVtableKind:
    case ArrayLenKind:
    case NewObjectKind:
    case FinishObjectKind:
        return true;
    default:
        return false;
    }
}

Opnd *AliasRep::getFinalObject() const {
    assert(isObjectFinal());
    if (kind == FinishObjectKind) {
        return 0;
    }
    return opnd;
}

bool AliasRep::isTypeFinal() const {
    switch (kind) {
    case StaticFieldKind:
        {
            FieldDesc *field = (FieldDesc *) desc;
            return field->isInitOnly();
        };

    case TypeVtableKind:
    case FinishTypeKind:
        return true;
    default:
        return false;
    }
}

NamedType *AliasRep::getFinalType() const {
    assert(isTypeFinal());
    switch (kind) {
    case StaticFieldKind:
        return (NamedType *)type;
    case TypeVtableKind:
        return (NamedType *)type;
    case FinishTypeKind:
        return 0;
    default:
        assert(0);
        return 0;
    }
}

bool 
AliasManager::isDuplicate(const AliasRep &a, const AliasRep &b) {
    if (a.kind != b.kind) return false;
    if (a.type && b.type && !Type::mayAlias(typeManager, a.type, b.type)) return false;
    if (a.desc != b.desc) return false;
    if ((a.opnd==NULL) != (b.opnd == NULL)) {
        return false;
    }
    if ((a.opnd && b.opnd) &&
        (! analyzer->mayAlias(a.opnd, b.opnd))) return false;
    return true;
};

void 
AliasManager::sawDuplicate(const AliasRep &ar, const AliasRep &canon)
{
    if (Log::isEnabled()) {
        Log::out() << "Saw duplicate of ";
        canon.print(Log::out());
        Log::out() << " : ";
        ar.print(Log::out());
        Log::out() << std::endl;
    }
    StlVectorSet<AliasRep> *theset = canon2others[canon];
    if (!theset) {
        theset = new (mm) StlVectorSet<AliasRep>(mm);
        canon2others[canon] = theset;
    }
    theset->insert(ar);
}

AliasRep AliasManager::findOrInsertAlias(AliasRep rep)
{
    Alias2Alias::const_iterator
        found = other2canon.find(rep),
        notfound = other2canon.end();
    if (found != notfound) {
        return (*found).second;
    }
    AliasList::iterator
        iter = allAliasReps.begin(),
        end = allAliasReps.end();
    for (; iter != end; iter++) {
        AliasRep thisRep = *iter;
        if (isDuplicate(thisRep, rep)) {
            sawDuplicate(rep, thisRep);
            other2canon[rep] = thisRep;
            return thisRep;
        }
    }
    other2canon[rep] = rep;
    rep.id = ++numAliasReps;
    allAliasReps.push_back(rep);
    sawDuplicate(rep, rep);
    return rep;
}

AliasRep AliasManager::getReference(Opnd *addr)
{
    // examine the address
    Inst *addri = addr->getInst();
    Opcode opcode = addri->getOpcode();
    switch (opcode) {
    case Op_LdFieldAddr: 
        {
            FieldAccessInst *faInst = addri->asFieldAccessInst();
            assert(faInst);
            Opnd *obj = faInst->getSrc(0);
            FieldDesc *field = faInst->getFieldDesc();
            return findOrInsertAlias(getObjectField(obj, field));
        }
    case Op_LdStaticAddr:
        {
            FieldAccessInst *faInst = addri->asFieldAccessInst();
            assert(faInst);
            FieldDesc *field = faInst->getFieldDesc();
            return findOrInsertAlias(getStaticField(field));
        }
    case Op_LdElemAddr:
        {
            Opnd *theArray = addri->getSrc(0);
            Type *theType = theArray->getType();
            assert(theType->isArrayType());
            ArrayType *arrayType = (ArrayType *)theType;
            NamedType *eltType = arrayType->getElementType();
            return findOrInsertAlias(getArrayElementByType(eltType));
        }
    case Op_LdArrayBaseAddr:
        {
            Opnd *theArray = addri->getSrc(0);
            Type *theType = theArray->getType();
            assert(theType->isArrayType());
            ArrayType *arrayType = (ArrayType *)theType;
            NamedType *eltType = arrayType->getElementType();
            return findOrInsertAlias(getArrayElementByType(eltType));
        }
    case Op_AddScaledIndex:
        {
            Type *eltType = NULL;
            Opnd *thePtr = addri->getSrc(0);
            Type *theType = thePtr->getType();
            if (theType->isManagedPtr() || theType->isUnmanagedPtr()) {
                PtrType *thePtrType = (PtrType *) theType;
                eltType = thePtrType->getPointedToType();
            } else {
                assert(0);
            }
            return findOrInsertAlias(getArrayElementByType(eltType));
        }
    case Op_TauLdVTableAddr:
        {
            Opnd *theObject = addri->getSrc(0);
            return findOrInsertAlias(getVtableOf(theObject));
        }
    case Op_GetVTableAddr:
        {
            TypeInst *typeInst = addri->asTypeInst();
            Type *theType = typeInst->getTypeInfo();
            return findOrInsertAlias(getVtableOf((NamedType *)theType));
        }
    case Op_TauLdIntfcVTableAddr:
        {
            Opnd *theObject = addri->getSrc(0);
            return findOrInsertAlias(getVtableOf(theObject));
        }
    case Op_TauLdVirtFunAddr:
        {
            Opnd *theObject = addri->getSrc(0);
            return findOrInsertAlias(getVtableOf(theObject));
        }
    case Op_TauLdVirtFunAddrSlot:
        {
            Opnd *theObject = addri->getSrc(0);
            return findOrInsertAlias(getVtableOf(theObject));
        }
    case Op_LdFunAddr: // static member function address
        {            
            MethodInst *mi = addri->asMethodInst();
            MethodDesc *mdesc = mi->getMethodDesc();
            NamedType *theType = mdesc->getParentType();
            return findOrInsertAlias(getVtableOf(theType));
        }
    case Op_LdFunAddrSlot:
        {            
            MethodInst *mi = addri->asMethodInst();
            MethodDesc *mdesc = mi->getMethodDesc();
            NamedType *theType = mdesc->getParentType();
            return findOrInsertAlias(getVtableOf(theType));
        }

    case Op_StVar:
    case Op_LdVar:
    case Op_UncompressRef:
    case Op_CompressRef:
    case Op_Copy:
        {
            return getReference(addri->getSrc(0));
        }

    case Op_LdFieldOffset:
    case Op_LdFieldOffsetPlusHeapbase:
        {
            FieldAccessInst *faInst = addri->asFieldAccessInst();
            assert(faInst);
            FieldDesc *field = faInst->getFieldDesc();
            return findOrInsertAlias(getObjectField(0, field));
        }

    case Op_LdArrayBaseOffset:
    case Op_LdArrayBaseOffsetPlusHeapbase:
        {
            TypeInst *tinst = addri->asTypeInst();
            assert(tinst);
            Type *eltType = tinst->getTypeInfo();
            NamedType *namedEltType = (NamedType *)eltType;
            return findOrInsertAlias(getArrayElementByType(namedEltType));
        }

    case Op_LdArrayLenOffset:
    case Op_LdArrayLenOffsetPlusHeapbase:
        {
            return findOrInsertAlias(getArrayLen(0));
        }

    case Op_AddOffset:
    case Op_AddOffsetPlusHeapbase:
        {
            Opnd *baseOpnd = addri->getSrc(0);
            Opnd *offsetOpnd = addri->getSrc(1);

            AliasRep offsetRep = getReference(offsetOpnd);
            switch (offsetRep.kind) {
            case AliasRep::ArrayLenKind:
                return findOrInsertAlias(getArrayLen(baseOpnd));
            case AliasRep::ArrayElementKind:
                return offsetRep;
            case AliasRep::ObjectFieldKind:
                {
                    TypeMemberDesc *desc = offsetRep.desc;
                    assert(offsetRep.opnd == 0);
                    return findOrInsertAlias(getObjectField(baseOpnd, desc));
                }
            case AliasRep::UnresolvedObjectFieldKind:
                {
                    assert(offsetRep.opnd == 0);
                    Opnd* enclClass = offsetRep.enclClass;
                    Opnd* cpIdx = offsetRep.idx;
                    assert(enclClass!=NULL &&  cpIdx!=NULL);
                    return findOrInsertAlias(getUnresolvedObjectField(baseOpnd, enclClass, cpIdx));
                }
            case AliasRep::LockKind:
                assert(offsetRep.opnd == 0);
                return findOrInsertAlias(getLock(baseOpnd));
            default:
                assert(0);
            }
        }

    case Op_LdVarAddr:
    case Op_Phi:
    case Op_DefArg: //magic as method param
        break;
    case Op_Conv: //the result of a conversion
    case Op_ConvZE:
    case Op_ConvUnmanaged:
    case Op_TauLdInd: // the result of static field load
    case Op_LdConstant:
        break;
    case Op_VMHelperCall:
        {
            VMHelperCallInst* callInst = addri->asVMHelperCallInst();
            assert(callInst->getVMHelperId() == VM_RT_GET_NONSTATIC_FIELD_OFFSET_WITHRESOLVE
                || callInst->getVMHelperId() == VM_RT_GET_STATIC_FIELD_ADDR_WITHRESOLVE);
            Opnd* enclClass = callInst->getSrc(0);
            Opnd* cpIdx = callInst->getSrc(1);
            return findOrInsertAlias(getUnresolvedObjectField(0, enclClass, cpIdx));
        }
        // break; unreachable because of the return above
    default:
        assert(0);
        break;
    }
    AliasRep theAlias = getAny(); // (AliasRep::UnknownKind);
    return findOrInsertAlias(theAlias);
}

AliasRep AliasManager::getAnyGlobal() 
{
    return findOrInsertAlias(AliasRep(AliasRep::GlobalKind));
}

AliasRep AliasManager::getVtableOf(Opnd *opnd)
{
    AliasRep vtblAlias(AliasRep::ObjectVtableKind, opnd);
    return findOrInsertAlias(vtblAlias);
}

AliasRep AliasManager::getLock(Opnd *opnd)
{
    AliasRep lockAlias(AliasRep::LockKind, opnd);
    return findOrInsertAlias(lockAlias);
}

AliasRep AliasManager::getLock(Type *type)
{
    AliasRep lockAlias(AliasRep::TypeLockKind, type);
    return findOrInsertAlias(lockAlias);
}

AliasRep AliasManager::getMethodPtr(Opnd *opnd, MethodDesc *desc)
{
    AliasRep methodAlias(AliasRep::MethodPtrKind, opnd, desc);
    return findOrInsertAlias(methodAlias);
}

AliasRep AliasManager::getFunPtr(Opnd *opnd)
{
    AliasRep funAlias(AliasRep::FunPtrKind, opnd);
    return findOrInsertAlias(funAlias);
}

AliasRep AliasManager::getNoMemory()
{
    return findOrInsertAlias(AliasRep(AliasRep::NullKind));
}

AliasRep AliasManager::getAnyEscaping()
{
    return findOrInsertAlias(AliasRep(AliasRep::GlobalKind));
}

AliasRep AliasManager::getAny()
{
    return findOrInsertAlias(AliasRep(AliasRep::AnyKind));
}

AliasRep AliasManager::getAnyLocal()
{
    return findOrInsertAlias(AliasRep(AliasRep::LocalKind));
}

AliasRep AliasManager::getVtableOf(NamedType *type)
{
    AliasRep vtblAlias(AliasRep::TypeVtableKind, type);
    return findOrInsertAlias(vtblAlias);
}

AliasRep AliasManager::getArrayLen(Opnd *opnd)
{
    AliasRep theAlias(AliasRep::ArrayLenKind, opnd);
    return findOrInsertAlias(theAlias);
}

AliasRep AliasManager::getArrayElements(Opnd *array, Opnd *offset,
                                        Opnd *length)
{
    Type *theType = array->getType();
    assert(theType->isArrayType());
    ArrayType *arrayType = (ArrayType *)theType;
    NamedType *eltType = arrayType->getElementType();
    return findOrInsertAlias(getArrayElementByType(eltType));
}

AliasRep AliasManager::getObjectNew(Opnd *opnd)
{
    AliasRep theAlias(AliasRep::NewObjectKind, opnd);
    return findOrInsertAlias(theAlias);
}

AliasRep AliasManager::getTypeNew(NamedType *type)
{
    AliasRep theAlias(AliasRep::NewTypeKind, type);
    return findOrInsertAlias(theAlias);
}

AliasRep AliasManager::getFinishObject(Opnd *obj)
{
    AliasRep theAlias(AliasRep::FinishObjectKind, obj);
    return findOrInsertAlias(theAlias);
}

AliasRep AliasManager::getFinishType(NamedType *type)
{
    AliasRep theAlias(AliasRep::FinishTypeKind, type);
    return findOrInsertAlias(theAlias);
}

AliasRep AliasManager::getArrayElementByType(Type *elementType)
{
    AliasRep theAlias(AliasRep::ArrayElementKind, elementType);
    return findOrInsertAlias(theAlias);
}

AliasRep AliasManager::getStaticField(TypeMemberDesc *field)
{
    AliasRep theAlias(AliasRep::StaticFieldKind, field);
    return findOrInsertAlias(theAlias);
}

AliasRep AliasManager::getObjectField(Opnd *obj, TypeMemberDesc *field)
{
    AliasRep theAlias(AliasRep::ObjectFieldKind, obj, field);
    return findOrInsertAlias(theAlias);
}

AliasRep AliasManager::getUnresolvedObjectField(Opnd *obj, Opnd* enclClass, Opnd* cpIdx)
{
    AliasRep theAlias(AliasRep::UnresolvedObjectFieldKind, obj, enclClass, cpIdx);
    return findOrInsertAlias(theAlias);
}

AliasRep AliasManager::getUnresolvedStaticField(Opnd* enclClass, Opnd* cpIdx)
{
    AliasRep theAlias(AliasRep::UnresolvedStaticFieldKind, 0, enclClass, cpIdx);
    return findOrInsertAlias(theAlias);
}

void
AliasManager::dumpAliasReps(::std::ostream &os) const
{
    os << "Alias Sets:" << std::endl;
    AliasList::const_iterator 
        iter = allAliasReps.begin(),
        end = allAliasReps.end();
    for (; iter != end; iter++) {
        const AliasRep &as = *iter;
        os << "    ";
        as.dump(os);
        os << " : ";
        AliasRep2AliasReps::const_iterator
            found = canon2others.find(as),
            notfound = canon2others.end();
        if (found != notfound) {
            const StlVectorSet<AliasRep> *represents = (*found).second;
            assert(represents);
            StlVectorSet<AliasRep>::const_iterator 
                iter2 = represents->begin(),
                end2 = represents->end();
            for (; iter2 != end2; iter2++) {
                const AliasRep &as2 = *iter2;
                as2.dump(os);
                os << " ";
            }
        } else {
            os << "--null--";
        }
        os << std::endl;
    }
    os << "End of Alias Sets" << std::endl;
}

// yields a list of ancestors in some order
const AliasManager::AliasList *
AliasManager::getAncestors(const AliasRep &a)
{
    Ancestors::iterator
        found = ancestors.find(a),
        notfound = ancestors.end();
    if (found != notfound) {
        return (*found).second;
    } else {
        AliasList *r = new (mm) AliasList(mm);
        computeAncestors(a, r);
        ancestors[a] = r;
        return r;
    }
}

// adds parents to result, returning true if nonempty
// yields a list of ancestors in some order
void
AliasManager::computeAncestors(const AliasRep &a, 
                               AliasManager::AliasList *result)
{
    switch (a.kind) {
    case AliasRep::NullKind: return;
    case AliasRep::GlobalKind: result->push_back(getAny()); return;
    case AliasRep::LocalKind: result->push_back(getAny()); return;
    case AliasRep::AnyKind: return;
    case AliasRep::UnknownKind:
    case AliasRep::UnresolvedStaticFieldKind:
        result->push_back(getAnyGlobal());
        result->push_back(getAnyLocal());
        result->push_back(getAny());
        return;
    case AliasRep::ObjectFieldKind:
        {
            FieldDesc *field = (FieldDesc *) a.desc;
            if (a.opnd) {
                result->push_back(getObjectField(0, field));
            }
            if (field->isInitOnly()) {
                result->push_back(getFinishObject(a.opnd));
            } else {
                if ((a.opnd == 0) || analyzer->mayEscape(a.opnd)) {
                    result->push_back(getAnyGlobal());
                }
                if ((a.opnd == 0) || !analyzer->mayEscape(a.opnd)) {
                    result->push_back(getAnyLocal());
                }
            }
            result->push_back(getAny()); return;
        }
    case AliasRep::UnresolvedObjectFieldKind:
        {
            if ((a.opnd == 0) || analyzer->mayEscape(a.opnd)) {
                result->push_back(getAnyGlobal());
            }
            if ((a.opnd == 0) || !analyzer->mayEscape(a.opnd)) {
                result->push_back(getAnyLocal());
            }
            result->push_back(getAny()); return;
        }
    case AliasRep::ArrayElementKind:
        {
            if ((a.opnd == 0) || analyzer->mayEscape(a.opnd)) {
                result->push_back(getAnyGlobal());
            }
            if ((a.opnd == 0) || !analyzer->mayEscape(a.opnd)) {
                result->push_back(getAnyLocal());
            }
            result->push_back(getAny()); return;
        }
    case AliasRep::StaticFieldKind:
        {
            FieldDesc *field = (FieldDesc *) a.desc;
            if (field->isInitOnly()) {
                result->push_back(getFinishType(field->getParentType()));
            } else {
                result->push_back(getAnyGlobal());
            }
            result->push_back(getAny()); return;
        }
    case AliasRep::ObjectVtableKind:
        {
            result->push_back(getFinishObject(a.opnd));
            result->push_back(getAny()); return;
        }
    case AliasRep::MethodPtrKind:
        {
            result->push_back(getFinishObject(a.opnd));
            result->push_back(getAny()); return;
        }
    case AliasRep::FunPtrKind:
        {
            result->push_back(getFinishObject(a.opnd));
            result->push_back(getAny()); return;
        }
    case AliasRep::TypeVtableKind:
        {
            result->push_back(getFinishType((NamedType *)a.type));
            result->push_back(getAny()); return;
        }
    case AliasRep::ArrayLenKind:
        {
            result->push_back(getFinishObject(a.opnd));
            result->push_back(getAny()); return;
        }
    case AliasRep::NewObjectKind:
        {
            result->push_back(getFinishObject(a.opnd));
            result->push_back(getAny()); return;
        }
    case AliasRep::NewTypeKind:
        {
            result->push_back(getFinishType((NamedType *)a.type));
            result->push_back(getAny()); return;
        }
    case AliasRep::FinishObjectKind:
        {
            result->push_back(getAny()); return;
        }
    case AliasRep::FinishTypeKind:
        {
            result->push_back(getAny()); return;
        }
    case AliasRep::LockKind: result->push_back(getAny()); return;
    case AliasRep::TypeLockKind: result->push_back(getAny()); return;
    default:
        assert(0);
    };
}

// ancestors of theRep have already been done
void MemoryOpt::insertPhiFor(const AliasRep &theRep, VarDefSites* defSites,
                             StlList<VarDefSites *> &ancestorDefSites)
{
    StlList<VarDefSites *>::iterator ancEnd = ancestorDefSites.end();
    Node* node;
    while ((node = defSites->removeDefSite()) != NULL) {
        bool done = false;
        // if an ancestor has a def there, skip it
        StlList<VarDefSites *>::iterator ancIter = ancestorDefSites.begin();
        for ( ; ancIter != ancEnd; ++ancIter ) {
            VarDefSites *ancSites = *ancIter;
            if (ancSites->isDefSite(node)) {
                done = true;
                if (Log::isEnabled()) {
                    Log::out() << "Skipping node ";
                    FlowGraph::printLabel(Log::out(), node);
                    Log::out() << " because ancestor has def there" << std::endl;
                }
                break;
            }
        }
        if (done) continue;

        if (Log::isEnabled()) {
            Log::out() << "Consider def at node ";
            FlowGraph::printLabel(Log::out(), node);
            Log::out() << std::endl;
        }

        List<Node>* dflist = df.getFrontiersOf(node);
        for (; dflist != NULL; dflist = dflist->getNext()) {
            // block where phi inst is going to be inserted
            Node* insertedLoc = dflist->getElem();
            // if phi has been inserted, then skip
            // no need to insert phi inst in the epilog because
            // there is no var use in the epilog
            if (!defSites->beenInsertedPhi(insertedLoc) &&
                !insertedLoc->getOutEdges().empty())  {

                if (Log::isEnabled()) {
                    Log::out() << "Queuing DF node ";
                    FlowGraph::printLabel(Log::out(), insertedLoc);
                    Log::out() << std::endl;
                }
                // create a new phi instruction for varOpnd
                createMemPhiInst(theRep,insertedLoc);
                // record phi(var) inst is created in dflist->getElem() block 
                defSites->insertPhiSite(insertedLoc);
            }
        }
    }
}

void MemoryOpt::insertMemPhi() {
    AliasDefSites::iterator
        iter = aliasDefSites->begin(),
        end = aliasDefSites->end();
    
    for ( ; iter != end; ++iter ) {
        AliasRep aliasRep = (*iter).first;
        VarDefSites* defSites = (*iter).second;

        if (defSites) {
            if (Log::isEnabled()) {
                Log::out() << "BEGIN Inserting Phis for ";
                aliasRep.print(Log::out());
                Log::out() << std::endl;
            }

            const AliasManager::AliasList *alist = aliasManager->getAncestors(aliasRep);
            StlList<VarDefSites *> ancestorDefSites(mm);
            AliasManager::AliasList::const_iterator
                iter = alist->begin(),
                end = alist->end();
            AliasDefSites::iterator sitesEnd = aliasDefSites->end();
            for ( ; iter != end; ++iter) {
                AliasRep ancestor = *iter;
                AliasDefSites::iterator found = aliasDefSites->find(ancestor);
                if (found != sitesEnd) {
                    ancestorDefSites.push_back((*found).second);
                }
            }
            insertPhiFor(aliasRep, defSites, ancestorDefSites);

            if (Log::isEnabled()) {
                Log::out() << "DONE Inserting Phis for ";
                aliasRep.print(Log::out());
                Log::out() << std::endl;
            }
        } else {
            if (Log::isEnabled()) {
                Log::out() << "NO Phis for ";
                aliasRep.print(Log::out());
                Log::out() << " because it has no defs" << std::endl;
            }
        }
    }
}

void MemoryOpt::createMemPhiInst(const AliasRep &aliasRep, Node *node)
{
    if (Log::isEnabled()) {
        Log::out() << "Insert MemPhi for ";
        aliasRep.print(Log::out());
        Log::out() << " at node ";
        FlowGraph::printLabel(Log::out(), node);
        Log::out() << std::endl;
    }
    Inst *firstInst = (Inst*)node->getFirstInst();
    assert(firstInst->isLabel());

    InstMemBehavior *b = getOrCreateInstEffect(firstInst);
    b->uses.insert(aliasRep);
    b->defs.insert(aliasRep);

    memPhiSites->addMemPhi(node, aliasRep);
}    

// a ScopedDomNodeInstWalker, to be applied forwards/pre-order
class MemoryRenameWalker {
    MemoryOpt *thePass;
    U_32 timeCount;
public:
    MemoryRenameWalker(MemoryOpt *thePass0) :
        thePass(thePass0), timeCount(1)
    { 
    };
    void startNode(DominatorNode *domNode0) { };
    void applyToInst(Inst *inst);
    void finishNode(DominatorNode *domNode);
    void enterScope() { thePass->renameMap->enter_scope(); };
    void exitScope() { thePass->renameMap->exit_scope(); };
private:
    void addUse(Inst *inst, const AliasRep &rep);
    void addDef(Inst *inst, const AliasRep &rep);
};

void MemoryOpt::addMemUseDef(Inst *use, Inst *def)
{
    // update use->def map:
    DefsSet *defSet = memUseDefs[use];
    if (!defSet) {
        defSet = new (mm) DefsSet(mm);
        memUseDefs[use] = defSet;
    }
    ::std::pair<DefsSet::iterator, bool> res = defSet->insert(def);

    if (res.second) {
        // update def->use map as well:
        UsesSet *theSet = memDefUses[def];
        if (!theSet) {
            theSet = new (mm) UsesSet(mm);
            memDefUses[def] = theSet;
        }
        theSet->insert(use);
    }
}

void MemoryOpt::remMemInst(Inst *theInst)
{
    if (Log::isEnabled()) {
        Log::out() << "Eliminating redundant memory instruction: ";
        theInst->print(Log::out());
        Log::out() << std::endl;
    }

    // get defs used by theInst
    Use2DefsMap::iterator
        found = memUseDefs.find(theInst),
        end = memUseDefs.end();
    DefsSet *defsReachingInst = 0;
    if (found != end) {
        defsReachingInst = (*found).second;
        memUseDefs.erase(theInst);
    }

    // get insts using mem defined by theInst
    Def2UsesMap::iterator
        found2 = memDefUses.find(theInst),
        end2 = memDefUses.end();
    UsesSet *usersOfInst = 0;
    if (found2 != end2) {
        usersOfInst = (*found2).second;
        memDefUses.erase(theInst);
    }

    // set all users to point to this instance's reaching def
    if (usersOfInst) {
        UsesSet::iterator
            usersIter = usersOfInst->begin(),
            usersEnd = usersOfInst->end();
        for ( ; usersIter != usersEnd; ++usersIter) {
            Inst *user = *usersIter;

            DefsSet *defsOfThisUser = memUseDefs[user];
            assert(defsOfThisUser);
            assert(defsOfThisUser->has(theInst));
            defsOfThisUser->erase(theInst);
            if (defsReachingInst) {
                defsOfThisUser->insert(defsReachingInst->begin(),
                                       defsReachingInst->end());
            }
        }
    }

    if (defsReachingInst) {
        DefsSet::const_iterator 
            defsIter = defsReachingInst->begin(),
            defsEnd = defsReachingInst->end();
        for ( ; defsIter != defsEnd; ++defsIter) {
            Inst *defReachingInst = *defsIter;
            UsesSet *theSet = memDefUses[defReachingInst];
            assert(theSet);
            assert(theSet->has(theInst));
            // remove this use
            theSet->erase(theInst);
        
            if (usersOfInst) {
                // add new transitive uses
                theSet->insert(usersOfInst->begin(),
                               usersOfInst->end());
            }
        }
    }
    if (Log::isEnabled()) {
        Log::out() << "Finished eliminating redundant memory instruction: ";
        theInst->print(Log::out());
        Log::out() << std::endl;
    }
}

void MemoryOpt::replaceMemInst(Inst *oldInst, Inst *newInst)
{
    if (Log::isEnabled()) {
        Log::out() << "Replacing memory instruction ";
        oldInst->print(Log::out());
        Log::out() << " by ";
        newInst->print(Log::out());
        Log::out() << std::endl;
    }

    // get defs used by theInst
    assert(memUseDefs.find(newInst) == memUseDefs.end());

    DefsSet *defs = 0;
    Use2DefsMap::iterator 
        found = memUseDefs.find(oldInst),
        end = memUseDefs.end();
    if (found != end) {
        // point new inst to them
        defs = (*found).second;
        assert(defs);
        memUseDefs[oldInst] = defs;
        // and erase old inst from map
        memUseDefs.erase(oldInst);

        // update each reaching def to have newInst as a use instead of oldInst
        DefsSet::const_iterator
            iter = defs->begin(),
            end = defs->end();
        for ( ; iter != end; ++iter) {
            Inst *def = *iter;
            UsesSet *uses = memDefUses[def];
            assert(uses);
            uses->erase(oldInst);
            uses->insert(newInst);
        }
    }

    // get uses of theInst
    assert(memDefUses.find(newInst) == memDefUses.end());

    UsesSet *uses = 0;
    Def2UsesMap::iterator 
        foundUses = memDefUses.find(oldInst),
        endUses = memDefUses.end();
    if (foundUses != endUses) {
        // point new inst to them
        uses = (*foundUses).second;
        assert(uses);
        memDefUses[oldInst] = uses;
        // and erase old inst from map
        memDefUses.erase(oldInst);

        // update each user to have newInst as a def instead of oldInst
        UsesSet::const_iterator
            iter = uses->begin(),
            end = uses->end();
        for ( ; iter != end; ++iter) {
            Inst *use = *iter;
            DefsSet *defs = memUseDefs[use];
            assert(defs);
            defs->erase(oldInst);
            defs->insert(newInst);
        }
    }
}

bool MemoryOpt::hasSameReachingDefs(Inst *i1, Inst *i2)
{
    assert(i1 != i2);
    if (Log::isEnabled()) {
        Log::out() << "checking use2defs(" << (int)i1->getId() << ")==use2defs("
                   << (int)i2->getId() << "): ";
    }
    bool result;
    Use2DefsMap::iterator 
        found1 = memUseDefs.find(i1),
        found2 = memUseDefs.find(i2),
        end = memUseDefs.end();
    if ((found1 == end) || (found2 == end)) { 
        result = (found1 == found2);
    } else {
        DefsSet *defs1 = (*found1).second;
        DefsSet *defs2 = (*found2).second;
        assert(defs1 && defs2);
        result = (*defs1 == *defs2);
    }
    if (Log::isEnabled()) {
        Log::out() << (result ? " == > TRUE" : " == > FALSE") << std::endl;
    }
    return result;
}

bool MemoryOpt::hasDefReachesUse(Inst *def, Inst *use)
{
    Use2DefsMap::iterator 
        found = memUseDefs.find(use),
        end = memUseDefs.end();
    if (found == end) {
        return false;
    } else {
        DefsSet *defsSet = (*found).second;
        assert(defsSet);
        return (defsSet->has(def));
    }
}

void
MemoryRenameWalker::addDef(Inst *inst, const AliasRep &rep)
{
    addUse(inst, rep);


    // ok, now add a new binding
    thePass->renameMap->insert(rep, inst);
}

void
MemoryRenameWalker::addUse(Inst *inst, const AliasRep &rep)
{
    if (Log::isEnabled()) {
        Log::out() << "Adding Use of aliasRep ";
        rep.print(Log::out());
        Log::out() << " to inst ";
        inst->print(Log::out());
        Log::out() << std::endl;
    }

    AliasRenameMap::DefsSet defs(thePass->mm);
    thePass->renameMap->lookup(rep, defs);
    
    AliasRenameMap::DefsSet::iterator
        iter = defs.begin(),
        end = defs.end();
    for ( ; iter != end; ++iter) {
        Inst *defInst = *iter;
        thePass->addMemUseDef(inst, defInst);
    }
}

void
MemoryRenameWalker::applyToInst(Inst *inst)
{
    InstMemBehavior *b = thePass->getInstEffect(inst);

    if (Log::isEnabled()) {
        Log::out() << "applying renaming to Inst ";
        inst->print(Log::out());
        Log::out() << std::endl;
    }
    if (b) {
        // compute uses here
        if (b->acquire) {
            addUse(inst, thePass->aliasManager->getAny());
            addDef(inst, thePass->aliasManager->getAny());
            return;
        }
        if (b->release) {
            addUse(inst, thePass->aliasManager->getAny());
            addDef(inst, thePass->aliasManager->getAny());
            return;
        }
        {
            StlVectorSet<AliasRep>::const_iterator 
                iter = b->uses.begin(),
                end = b->uses.end();
            for ( ; iter != end; ++iter) {
                AliasRep rep = *iter;
                if (Log::isEnabled()) {
                    Log::out() << "  has use of ";
                    rep.print(Log::out());
                    Log::out() << std::endl;
                }
                addUse(inst, rep);
            }
        }

        // compute definitions here

        {        
            StlVectorSet<AliasRep>::const_iterator 
                iter = b->defs.begin(),
                end = b->defs.end();
            for ( ; iter != end; ++iter) {
                AliasRep rep = *iter;
                if (Log::isEnabled()) {
                    Log::out() << "  has def of ";
                    rep.print(Log::out());
                    Log::out() << std::endl;
                }
                addDef(inst, rep);
            }
        }
    }
}

void
MemoryRenameWalker::finishNode(DominatorNode *domNode)
{
    Node *node = domNode->getNode(); 

    if (Log::isEnabled()) {
        Log::out() << "finishing renaming for Node ";
        FlowGraph::printLabel(Log::out(), node);
        Log::out() << std::endl;
    }

    const Edges& edges = node->getOutEdges();
    Edges::const_iterator eiter;
    for(eiter = edges.begin(); eiter != edges.end(); ++eiter) {
        Edge* e = *eiter;
        Node* succ = e->getTargetNode();

        const StlVector<AliasRep> *memPhis = thePass->memPhiSites->getMemPhis(succ);
        if (memPhis) {
            Inst *succLabel = (Inst*)succ->getFirstInst();
            assert(succLabel->isLabel());
            
            StlVector<AliasRep>::const_iterator
                iter = memPhis->begin(),
                end = memPhis->end();
            
            for ( ; iter != end; ++iter) {
                const AliasRep &thisRep = *iter;
                addUse(succLabel, thisRep);
            }
        }
    }

    if (Log::isEnabled()) {
        Log::out() << "finished renaming for Node ";
        FlowGraph::printLabel(Log::out(), node);
        Log::out() << std::endl;
    }
}

// a NodeInstWalker, to be applied forwards
class MemoryDebugWalker {
    MemoryOpt *thePass;
public:
    MemoryDebugWalker(MemoryOpt *thePass0) :
        thePass(thePass0)
    { };
    void startNode(Node *node) { };
    void applyToInst(Inst *inst);
    void finishNode(Node *node) {};
};

void MemoryDebugWalker::applyToInst(Inst *inst)
{
    inst->print(Log::out());
    Log::out() << std::endl;
    MemoryOpt::Use2DefsMap::iterator 
        found = thePass->memUseDefs.find(inst),
        end = thePass->memUseDefs.end();
    if (found != end) {
        MemoryOpt::DefsSet *defs = (*found).second;
        assert(defs);
        Log::out() << " depends on instructions: ";
        MemoryOpt::DefsSet::iterator
            iter2 = defs->begin(),
            end2 = defs->end();
        for ( ; iter2 != end2; ++iter2) {
            Inst *thisDep = *iter2;
            thisDep->print(Log::out());
            Log::out() << " ";
        }
        Log::out() << std::endl;
    }
}

void MemoryOpt::createUsesMap() 
{
    MemoryRenameWalker renameWalker(this);

    // adapt the ScopedDomNodeInstWalker to a DomWalker
    typedef ScopedDomNodeInst2DomWalker<true, MemoryRenameWalker>
        MemoryRenameDomWalker;
    MemoryRenameDomWalker domRenameWalker(renameWalker);
    
    // do the walk, pre-order
    DomTreeWalk<true, MemoryRenameDomWalker>(dominators, domRenameWalker, 
                                             mm);

    if (Log::isEnabled()) {
        MemoryDebugWalker debugWalker(this);

        // adapt the forwards NodeInstWalker to a NodeWalker
        typedef NodeInst2NodeWalker<true, MemoryDebugWalker>
            MemoryDebugNodeWalker;
        MemoryDebugNodeWalker debugNodeWalker(debugWalker);
    
        // do the walk over nodes in arbitrary order
        NodeWalk<MemoryDebugNodeWalker>(fg, debugNodeWalker);
    }
}

void AliasRenameMap::enter_scope()
{
    ++depth;
    defMap.enter_scope();
    activeDescendents.enter_scope();
}

void AliasRenameMap::exit_scope()
{
    defMap.exit_scope();
    activeDescendents.exit_scope();
    --depth;
}

void AliasRenameMap::insert(const AliasRep &rep, Inst *defInst)
{
    U_32 timeNow = ++timeCount;
    AliasBinding newBinding(defInst, timeNow);

    if (Log::isEnabled()) {
        Log::out() << "  adding def of ";
        rep.print(Log::out());
        Log::out() << " at time " << (int) timeNow << " to inst " 
                   << (int) defInst->getId() << std::endl;
    }

    // first, set this value
    defMap.insert(rep, newBinding);

    // clear active descendants of rep; they are overwritten now,
    // so remove their definitions and clear it
    DescendentSet *descSet = activeDescendents.lookup(rep);
    if (descSet) {
        descSet->clear();
    }
    // Add this rep to the set of active descendants for
    // each ancestor.
    const AliasManager::AliasList *alist = 
        aliasManager->getAncestors(rep);
    AliasManager::AliasList::const_iterator
        iter = alist->begin(),
        end = alist->end();
    for ( ; iter != end; ++iter) {
        const AliasRep &thisAncestor = *iter;
        
        // get the set of descendants of the ancestor, or create it
        DescendentSet *descSet = activeDescendents.lookup(thisAncestor);
        if (!descSet) {
            descSet = new (mm) DescendentSet(mm, depth);
            activeDescendents.insert(thisAncestor, descSet);
        } else if (descSet->depth != depth) {
            // we need to make a copy of the set so we don't overwrite
            // the definition in the sibling nodes
            descSet = new (mm) DescendentSet(mm, descSet, depth);
            activeDescendents.insert(thisAncestor, descSet);
        }
        // add this rep to the descendant set
        descSet->insert(rep);
    }
}

void AliasRenameMap::lookup(const AliasRep &rep, DefsSet &defs)
{
    // first, find the appropriate binding to use, checking for most recent of
    // rep and its ancestors which has been set
    AliasBinding bestFound = defMap.lookup(rep);
    Inst *bestInst = bestFound.inst;
    U_32 bestWhen = bestInst ? bestFound.when : 0;
    {    
        
        if (!bestInst) {
            if (Log::isEnabled()) {
                Log::out() << "aliasRep ";
                rep.print(Log::out());
                Log::out() << " has no binding" << std::endl;
            }
        } else {
            if (Log::isEnabled()) {
                Log::out() << "aliasRep ";
                rep.print(Log::out());
                Log::out() << " has binding at time " << (int) bestWhen << std::endl;
            }
        }

        const AliasManager::AliasList *alist = 
            aliasManager->getAncestors(rep);
        AliasManager::AliasList::const_iterator
            iter = alist->begin(),
            end = alist->end();
        if (Log::isEnabled()) {
            if (iter != end) {
                Log::out() << "trying ancestors: " << std::endl;
            } else {
                Log::out() << "has no ancestors" << std::endl;
            }
        }
        for ( ; iter != end; ++iter) {
            AliasRep thisAncestor = *iter;
            if (Log::isEnabled()) {
                thisAncestor.print(Log::out());
                Log::out() << " ";
            }
            AliasBinding newFound = defMap.lookup(thisAncestor);
            Inst *thisInst = newFound.inst;
            if (thisInst) {
                U_32 thisWhen = newFound.when;
                if (Log::isEnabled()) {
                    Log::out() << "    ";
                    thisAncestor.print(Log::out());
                    Log::out() << " has binding at time " << (int) thisWhen << std::endl;
                }
                if (newFound.when > bestWhen) {
                    bestWhen = thisWhen;
                    bestInst = thisInst;
                    if (Log::isEnabled()) {
                        Log::out() << ", replaces bestInst";
                    }
                }
                if (Log::isEnabled()) {
                    Log::out() << std::endl;
                }
            } else {
                if (Log::isEnabled()) {
                    Log::out() << "    ";
                    thisAncestor.print(Log::out());
                    Log::out() << " has no binding " << std::endl;
                }
            }
        }
        if (Log::isEnabled()) {
            Log::out() << std::endl;
        }
    }        
    assert(bestInst);
    defs.insert(bestInst);

    // ok, now look for bindings of this rep's descendants which are 
    // more recent than the best one we found.
    DescendentSet *descSet = activeDescendents.lookup(rep);
    if (descSet) {
        // has descendants, add those, too;
        DescendentSet::iterator
            descIter = descSet->begin(),
            descEnd = descSet->end();
        for ( ; descIter != descEnd; ++descIter) {
            const AliasRep &desc = *descIter;
            const AliasBinding &descBinding = defMap.lookup(desc);
            Inst *desci = descBinding.inst;
            U_32 when = descBinding.when;
            if (when > bestWhen) {             // descendant is newer
                defs.insert(desci);
            }
        }
    }
}

// an InstWalker, to be applied forwards
class MemoryRedStoreWalker {
    MemoryOpt *thePass;
public:
    MemoryRedStoreWalker(MemoryOpt *thePass0) :
        thePass(thePass0)
    { };
    void applyToInst(Inst *inst);
};

static U_8
getBitWidth(Type::Tag tag)
{
    switch (tag) {
    case Type::Void: return 0;
    case Type::Boolean: return 1;
    case Type::Char: return 16;
    case Type::Int8: return 8;
    case Type::Int16: return 16;
    case Type::Int32: return 32;
    case Type::Int64: return 64;
    case Type::UInt8: return 8;
    case Type::UInt16: return 16;
    case Type::UInt32: return 32;
    case Type::UInt64: return 64;

    default: return 0xff; // uses all;
    }        
}

void
MemoryRedStoreWalker::applyToInst(Inst *inst)
{

    switch (inst->getOpcode()) {
    case Op_TauStInd:
    case Op_TauStField:
    case Op_TauStStatic:
    case Op_TauStElem:
    case Op_TauStRef:
        // maybe eliminate inst;
        break;
    default:
        return;
    }
    MemoryOpt::Def2UsesMap::iterator
        foundUses = thePass->memDefUses.find(inst),
        endDefUse = thePass->memDefUses.end();
    MemoryOpt::UsesSet *usesSet = ((foundUses != endDefUse)
                                   ? (*foundUses).second
                                   : 0);
    if ((!usesSet) || (usesSet->size() == 0)) {
    } else if (usesSet->size() == 1) {
        Inst *usingInst = (*usesSet)[0];
        if ((usingInst->getOpcode())  == (inst->getOpcode())) {
            switch (usingInst->getOpcode()) {
            case Op_TauStInd:
                {
                    Opnd *usingAddr = usingInst->getSrc(1);
                    Opnd *thisAddr = inst->getSrc(1);
                    if ((usingAddr == thisAddr) &&
                        (getBitWidth(usingInst->getType()) >=
                         getBitWidth(inst->getType()))) {
                        // eliminate inst;
                        thePass->remMemInst(inst);
                        inst->unlink();
                    }
                }
                break;

            case Op_TauStRef:
                {
                    Opnd *usingAddr = usingInst->getSrc(1);
                    Opnd *thisAddr = inst->getSrc(1);
                    if ((usingAddr == thisAddr) &&
                        (getBitWidth(usingInst->getType()) >=
                         getBitWidth(inst->getType()))) {
                         // eliminate inst;
                        thePass->remMemInst(inst);
                        inst->unlink();
                    }
                }
                break;

            case Op_TauStField:
                {
                    Opnd *usingBase = usingInst->getSrc(1);
                    Opnd *thisBase = inst->getSrc(1);
                    FieldAccessInst *finst = inst->asFieldAccessInst();
                    FieldAccessInst *fusing = usingInst->asFieldAccessInst();
                    FieldDesc *instDesc = finst->getFieldDesc();
                    FieldDesc *usingDesc = fusing->getFieldDesc();
                    
                    usingDesc = instDesc;
                    if ((usingBase == thisBase) &&
                        usingDesc &&
                        ( getBitWidth(usingInst->getType()) 
                            >= getBitWidth(inst->getType()) )) {
                        // eliminate inst;
                        thePass->remMemInst(inst);
                        inst->unlink();
                    }
                }
                break;
            case Op_TauStStatic:
                {
                    FieldAccessInst *finst = inst->asFieldAccessInst();
                    FieldAccessInst *fusing = usingInst->asFieldAccessInst();
                    FieldDesc *instDesc = finst->getFieldDesc();
                    FieldDesc *usingDesc = fusing->getFieldDesc();
                    
                    usingDesc = instDesc;
                    if (usingDesc &&
                        (getBitWidth(usingInst->getType()) 
                            >= getBitWidth(inst->getType()))) {
                        // eliminate inst;
                        thePass->remMemInst(inst);
                        inst->unlink();
                    }
                }
                break;
            case Op_TauStElem:
                {
                    Opnd *usingArray = usingInst->getSrc(1);
                    Opnd *thisArray = inst->getSrc(1);
                    
                    Opnd *usingIndex = usingInst->getSrc(2);
                    Opnd *thisIndex = inst->getSrc(2);
                    
                    if ((usingArray == thisArray) &&
                        (usingIndex == thisIndex) &&
                        (getBitWidth(usingInst->getType()) >=
                         getBitWidth(inst->getType()))) {
                        // eliminate inst;
                        thePass->remMemInst(inst);
                        inst->unlink();
                    }
                }
                break;
            default:
                break;
            }
        }
    }
}

void MemoryOpt::eliminateRedStores() 
{
    MemoryRedStoreWalker redStoreWalker(this);

    // adapt the forwards NodeInstWalker to a NodeWalker
    typedef Inst2NodeWalker<true, MemoryRedStoreWalker>
        MemoryRedStoreNodeWalker;
    MemoryRedStoreNodeWalker redStoreNodeWalker(redStoreWalker);
    
    // do the walk over nodes in arbitrary order
    NodeWalk<MemoryRedStoreNodeWalker>(fg, redStoreNodeWalker);

    if (Log::isEnabled()) {
        Log::out() << "After red store elimination: " << std::endl;

        MemoryDebugWalker debugWalker(this);

        // adapt the forwards InstWalker to a NodeWalker
        typedef Inst2NodeWalker<true, MemoryDebugWalker>
            MemoryDebugNodeWalker;
        MemoryDebugNodeWalker debugNodeWalker(debugWalker);
        
        // do the walk over nodes in arbitrary order
        NodeWalk<MemoryDebugNodeWalker>(fg, debugNodeWalker);
    }
}

// an InstWalker, to be applied forwards
class MemorySyncOptWalker {
    MemoryOpt *thePass;
public:
    MemorySyncOptWalker(MemoryOpt *thePass0) :
        thePass(thePass0)
    { };
    void applyToInst(Inst *inst);
};

void
MemorySyncOptWalker::applyToInst(Inst *inst)
{

    switch (inst->getOpcode()) {
    case Op_BalancedMonitorExit:
        return;
    case Op_TauBalancedMonitorEnter:
        // check whether this is balanced
        break;
    default:
        return;
    }

    StlVectorSet<Inst *> involved(thePass->mm);
    StlVectorSet<Inst *> newlyInvolved(thePass->mm);

    newlyInvolved.insert(inst);

    Opnd *obj = inst->getSrc(0);
    if (Log::isEnabled()) {
        Log::out() << "Checking instruction ";
        inst->print(Log::out());
        Log::out() << std::endl;
    }

    Opnd *enterLockAddrOpnd = 0;
    Opnd *enterLockValueOpnd = 0;
    Opnd *exitLockAddrOpnd = 0;
    Opnd *exitLockValueOpnd = 0;
    while (!newlyInvolved.empty()) {
        Inst *monitorInst = *(newlyInvolved.begin());
        newlyInvolved.erase(newlyInvolved.begin());
        
        if (involved.count(monitorInst) == 0) {
            // don't have it, insert it
            involved.insert(monitorInst);
            
            if (Log::isEnabled()) {
                Log::out() << "Is involved with instruction ";
                monitorInst->print(Log::out());
                Log::out() << std::endl;
            }
            if (monitorInst->isLabel()) {
                // acting as memory phi, match both ways

                MemoryOpt::Use2DefsMap::iterator
                    foundDefs = thePass->memUseDefs.find(monitorInst),
                    endDefs = thePass->memUseDefs.end();
                if (foundDefs != endDefs) {
                    MemoryOpt::DefsSet *defs = (*foundDefs).second;
                    MemoryOpt::DefsSet::iterator
                        iter2 = defs->begin(),
                        end2 = defs->end();
                    for ( ; iter2 != end2; ++iter2) {
                        Inst *def = *iter2;

                        if (!(def->isLabel() ||
                              (def->getOpcode() == Op_BalancedMonitorExit))) {
                            // failed
                            return;
                        }
                    }
                    newlyInvolved.insert(defs->begin(), defs->end());
                } else {
                    // failed to find match
                    return;
                }

                MemoryOpt::Def2UsesMap::iterator
                    foundMatches = thePass->memDefUses.find(monitorInst),
                    endMatches = thePass->memDefUses.end();
                if (foundMatches != endMatches) {
                    MemoryOpt::UsesSet *foundUses = (*foundMatches).second;
                    
                    MemoryOpt::UsesSet::iterator
                        usesIter = foundUses->begin(),
                        usesEnd = foundUses->end();
                    for ( ; usesIter != usesEnd; ++usesIter) {
                        Inst *use = *usesIter;
                        if (!(use->isLabel() ||
                              use->getOpcode() == Op_TauBalancedMonitorEnter)) {
                            // failed
                            return;
                        }
                    }
                    newlyInvolved.insert(foundUses->begin(), foundUses->end());
                }

            } else {
                switch (monitorInst->getOpcode()) {
                case Op_BalancedMonitorExit: {
                    // getMatch = monitorExit to following monitorEnter
                    // look at uses

                    if (obj != monitorInst->getSrc(0)) {
                        // failed
                        return;
                    }
                    
                    if (!exitLockAddrOpnd) {
                        exitLockAddrOpnd = monitorInst->getSrc(1);
                        exitLockValueOpnd = monitorInst->getSrc(2);
                    } else {
                        if ((exitLockAddrOpnd != monitorInst->getSrc(1))||
                            (exitLockValueOpnd != monitorInst->getSrc(2))) {
                            // failed
                            return;
                        }
                        
                    }

                    MemoryOpt::Def2UsesMap::iterator
                        foundMatches = thePass->memDefUses.find(monitorInst),
                        endMatches = thePass->memDefUses.end();
                    if (foundMatches != endMatches) {
                        MemoryOpt::UsesSet *foundUses = (*foundMatches).second;
                        
                        MemoryOpt::UsesSet::iterator
                            usesIter = foundUses->begin(),
                            usesEnd = foundUses->end();
                        for ( ; usesIter != usesEnd; ++usesIter) {
                            Inst *use = *usesIter;
                            if (!(use->isLabel() ||
                                  use->getOpcode() == Op_TauBalancedMonitorEnter)) {
                                // failed
                                return;
                            }
                        }
                        newlyInvolved.insert(foundUses->begin(),
                                             foundUses->end());
                    } else {
                        // failed
                        return;
                    }
                }
                    break;
                case Op_TauBalancedMonitorEnter: {
                    // monitorEnter to preceding monitorExit

                    if (obj != monitorInst->getSrc(0)) {
                        // failed
                        return;
                    }
                    
                    MemoryOpt::Use2DefsMap::iterator
                        foundMatches = thePass->memUseDefs.find(monitorInst),
                        endMatches = thePass->memUseDefs.end();
                    if (foundMatches != endMatches) {
                        MemoryOpt::DefsSet *foundDefs = (*foundMatches).second;
                        assert(foundDefs);
                        
                        MemoryOpt::DefsSet::iterator
                            iter2 = foundDefs->begin(),
                            end2 = foundDefs->end();
                        for ( ; iter2 != end2; ++iter2) {
                            Inst *foundDef = *iter2;
                        
                            if (!(foundDef->isLabel() ||
                                  (foundDef->getOpcode()==Op_BalancedMonitorExit))) {
                                // failed
                                return;
                            }
                        }
                        
                        if (!enterLockAddrOpnd) {
                            enterLockAddrOpnd = monitorInst->getSrc(1);
                            enterLockValueOpnd = monitorInst->getDst();
                        } else {
                            if ((enterLockAddrOpnd != monitorInst->getSrc(1))||
                                (enterLockValueOpnd != monitorInst->getDst())) {
                                // failed
                                return;
                            }
                                
                        }
                        newlyInvolved.insert(foundDefs->begin(),
                                             foundDefs->end());
                    } else {
                        // failed
                        return;
                    }
                }
                    break;
                default:
                    // failed
                    return;

                }
            }
        }
    }
    // involved contains all exit/enter which match (plus possible labels)

    // turn them into fences 

    if (Log::isEnabled()) {
        Log::out() << "Can convert to fence: " << std::endl;
    }

    InstFactory &instFactory = thePass->irManager.getInstFactory();
    StlVector<Inst *>::iterator 
        invIter = involved.begin(),
        invEnd = involved.end();
    for ( ; invIter != invEnd; ++invIter) {
        Inst *invInst = *invIter;
        Inst *newI = 0;
        if (invInst->isLabel())
            continue;
        
        switch (invInst->getOpcode()) {
        case Op_BalancedMonitorExit:
            newI = instFactory.makeMonitorExitFence(obj);
            break;
        case Op_TauBalancedMonitorEnter:
            if (enterLockAddrOpnd != exitLockAddrOpnd) {
                Inst *enterLockAddrInst = enterLockAddrOpnd->getInst();
                Inst *newI2= instFactory.makeCopy(enterLockAddrOpnd, exitLockAddrOpnd);
                newI2->insertBefore(enterLockAddrInst);
                enterLockAddrInst->unlink();
            }            
            {
                Inst *newI3 = instFactory.makeCopy(enterLockValueOpnd, 
                                                   exitLockValueOpnd);
                newI3->insertAfter(invInst);
            }
            newI = instFactory.makeMonitorEnterFence(obj); 
            break;
        default:
            assert(0);
        }
        if (Log::isEnabled()) {
            Log::out() << "Instruction " << std::endl << "    ";
            invInst->print(Log::out());
            Log::out() << std::endl << "  becomes " << std::endl << "    ";
            newI->print(Log::out());
            Log::out() << std::endl;
        }
        newI->insertAfter(invInst);
        invInst->unlink();
        thePass->replaceMemInst(invInst, newI);
    }
}

void MemoryOpt::doSyncOpt() 
{
    MemorySyncOptWalker memSyncOptWalker(this);

    // adapt the forwards NodeInstWalker to a NodeWalker
    typedef Inst2NodeWalker<true, MemorySyncOptWalker>
        MemorySyncOptNodeWalker;
    MemorySyncOptNodeWalker memSyncOptNodeWalker(memSyncOptWalker);
    
    // do the walk over nodes in arbitrary order
    NodeWalk<MemorySyncOptNodeWalker>(fg, memSyncOptNodeWalker);

    if (Log::isEnabled()) {
        Log::out() << "After mem syncopt: " << std::endl;

        MemoryDebugWalker debugWalker(this);

        // adapt the forwards InstWalker to a NodeWalker
        typedef Inst2NodeWalker<true, MemoryDebugWalker>
            MemoryDebugNodeWalker;
        MemoryDebugNodeWalker debugNodeWalker(debugWalker);
        
        // do the walk over nodes in arbitrary order
        NodeWalk<MemoryDebugNodeWalker>(fg, debugNodeWalker);
    }
}


} //namespace Jitrino 
