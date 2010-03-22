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

#include "escapeanalyzer.h"
#include "irmanager.h"
#include "Inst.h"
#include "Stl.h"
#include "BitSet.h"

namespace Jitrino {

DEFINE_SESSION_ACTION(OldEscapeAnalysisPass, old_escape, "Old Escape Analysis")

void
OldEscapeAnalysisPass::_run(IRManager& irm) {   
    EscapeAnalyzer ea(irm);
    ea.doAnalysis();
}

static bool
isEscapeOptimizationCandidate(Inst* inst) {
    switch (inst->getOpcode()) {
    case Op_Catch:  
        return false;
    case Op_LdRef:   case Op_NewObj: case Op_NewArray:
        return true;
    case Op_NewMultiArray:
        return true;
    case Op_LdConstant:
        //
        // shouldn't really care about these because ld constants of 
        // references only load nulls.
        //
        return false;
        //return (inst->getDst()->getType()->isObject());
    case Op_DefArg:
        return false;
    default:
    return false;
    }
//    return false;
}

//
// Returns true if the instruction can cause one of it's srcs to escape 
// the method.
//
static bool
isPotentiallyEscapingInst(Inst* inst) {
    switch (inst->getOpcode()) {
    case Op_DirectCall:     case Op_TauVirtualCall:
    case Op_IndirectCall:   case Op_IndirectMemoryCall: 
    case Op_Return:         case Op_Throw:
    case Op_TauStInd:       case Op_TauStRef:  
    case Op_TauStField:     case Op_TauStElem:     case Op_TauStStatic:
        return true;
    // in the absence of ssa form, stores to vars can conservatively escape
    case Op_StVar:
        return true;
    default: return false;
    }
    //return false;
}

//
// Returns true if any of the instruction allows the indicated src
// to escape the method.  These are calls, stores, and returns.
// We also conservatively assume that unsafe conversions to unmanaged
// pointers also cause an escape from the method.  We can refine this later.
// Also, StVar instructions are assumed to escape until we have SSA form.
//
static bool
isEscapingSrcObject(Inst* inst,U_32 srcIndex) {
    // only care about escapes of object types
    Type* srcType = inst->getSrc(srcIndex)->getType();
    if (srcType->isObject() == false && srcType->isManagedPtr() == false)
        return false;

    switch (inst->getOpcode()) {
        //
        // calls
        //
    case Op_IndirectCall:    case Op_IndirectMemoryCall:
        // the first argument of indirect calls is a fun ptr
        if (srcIndex == 0)
            return false;
        break;
    case Op_DirectCall:    case Op_TauVirtualCall:
        break;
        //
        // return & throw
        //
    case Op_Return:    case Op_Throw:
        break;
        //
        // stores
        //
        // Note, that we are being conservative with stores.  We should
        // check that the store is to an object that is not local to the
        // method.
        //
    case Op_TauStInd:    case Op_TauStField:    case Op_TauStElem:
    case Op_TauStRef:
        // the first argument of a store is the value stored
        if (srcIndex != 0)
            return false;
        break;
        //
        // store to a static always escapes.
        //
    case Op_TauStStatic:
        break;
        //
        // conversion to unmanaged pointer is also an escape!
        //
    case Op_Conv:
    case Op_ConvZE:
    case Op_ConvUnmanaged:
        break;
        // in the absence of ssa form, stores to vars can conservatively escape
    case Op_StVar:
        break;
    default:
        break;
    }
    return true;
}

static void
markEscapingInst(Inst* inst,BitSet& escapingInsts) {
    if (escapingInsts.setBit(inst->getId(),true))
        // already known to escape
        return;
    switch (inst->getOpcode()) {
    case Op_Copy:       case Op_TauStaticCast:   case Op_TauCast:   case Op_TauAsType: 
    case Op_TauCheckNull:  case Op_StVar:
    case Op_UncompressRef: case Op_CompressRef:
        // mark source as escaping
        markEscapingInst(inst->getSrc(0)->getInst(),escapingInsts);
        break;
        //
        // in the absence of ssa form, loads of vars can conservatively escape
        // if we had SSA form, LdVar would follow its use-def chain to the 
        // StVar or phi instruction that defines its SSA variable.
        // 
    case Op_LdVar:
        break;
    case Op_Catch:  case Op_DefArg: case Op_LdRef:   case Op_LdConstant:
    case Op_NewObj: case Op_NewArray:
        // no src operands to mark
        break;
    case Op_NewMultiArray:
        break;
        //
        // sources of loads do not escape further
        //
    case Op_TauLdInd: case Op_TauLdField:    case Op_LdStatic:   case Op_TauLdElem:
        break;
        //
        // calls should already be marked as escaping
        //
    case Op_DirectCall:     case Op_TauVirtualCall:
    case Op_IndirectCall:   case Op_IndirectMemoryCall: 
        break;
        //
        // managed pointers that escape
        //
    case Op_LdArrayBaseAddr:
    case Op_AddScaledIndex:
    case Op_LdFieldAddr:    case Op_LdElemAddr:
    case Op_AddOffset:
        // base pointer escapes
        markEscapingInst(inst->getSrc(0)->getInst(),escapingInsts);
        break;
    case Op_LdStaticAddr:   case Op_LdVarAddr:
        break;
    default:
        ::std::cerr << "ERROR: unknown escaping ref opcode: "
             << inst->getOperation().getOpcodeString()
             << ::std::endl;
        assert(0);
        break;
    }
}

U_32
EscapeAnalyzer::doAnalysis() {
    MemoryManager memManager("EscapeAnalyzer::doAnalysis");
    StlDeque<Inst*> candidateSet(memManager);
    BitSet escapingInsts(memManager,irManager.getInstFactory().getNumInsts());

    const Nodes& nodes = irManager.getFlowGraph().getNodes();
    Nodes::const_iterator niter;
    //
    // Clear all marks on instructions
    // Collect instructions that are candidate for escape optimizations
    //
    for(niter = nodes.begin(); niter != nodes.end(); ++niter) {
        Node* node = *niter;
        Inst *headInst = (Inst*)node->getFirstInst();
        for (Inst* inst=headInst->getNextInst();inst!=NULL;inst=inst->getNextInst()) {
            if (isEscapeOptimizationCandidate(inst))
                candidateSet.push_back(inst);
        }
    }
    //
    // Iteratively mark instructions whose results escape the method
    //
    for(niter = nodes.begin(); niter != nodes.end(); ++niter) {
        Node* node = *niter;
        Inst *headInst = (Inst*)node->getFirstInst();
        for (Inst* inst=headInst->getNextInst();inst!=NULL;inst=inst->getNextInst()) {
            if (isPotentiallyEscapingInst(inst) == false)
                continue;
            escapingInsts.setBit(inst->getId(),true);
            for (U_32 i=0; i<inst->getNumSrcOperands(); i++) {
                if (isEscapingSrcObject(inst,i) == false)
                    continue;
                // src escapes
                markEscapingInst(inst->getSrc(i)->getInst(),escapingInsts);
            }
        }
    }
    //
    // Print out non-escaping instructions
    //
    U_32 numTrapped = 0;
    while (candidateSet.empty() == false) {
         Inst* inst = candidateSet.front();
         candidateSet.pop_front();
         if (escapingInsts.getBit(inst->getId()))
             continue;
         numTrapped++;
     }
     return numTrapped;
}

static bool
isRefOrPtrType(Opnd* opnd) {
    Type* type = opnd->getType();
    return (type->isObject() || type->isManagedPtr());
}

static void
initialize(Inst* inst,
           StlDeque<Inst*>& workList,
           DefUseBuilder&   defUseBuilder,
           Opnd* returnOpnd) {
    //
    // Build def-use chains for instructions of interest
    // which are any instructions that use a ptr or ref value
    //
    switch (inst->getOpcode()) {
    case Op_Copy:       case Op_TauCast:   case Op_TauAsType: 
    case Op_TauCheckNull:
    case Op_UncompressRef: case Op_CompressRef:
        // loads from refs
    case Op_TauLdField:  case Op_TauLdElem: case Op_TauLdInd:
    case Op_LdArrayBaseAddr:    case Op_AddScaledIndex:
    case Op_LdFieldAddr:        case Op_LdElemAddr:
    case Op_AddOffset:
        // create a def-use to base address computation
        defUseBuilder.addDefUse(inst->getSrc(0)->getInst(),inst,0);
        break;
    case Op_TauStInd:    case Op_TauStField:    case Op_TauStElem:
        // the second argument of stores are the base or ptr
        defUseBuilder.addDefUse(inst->getSrc(1)->getInst(),inst,1);
        break;
        // store with write barrier.  1st argument is base, 2nd is managed ptr
        // 3rd src is the stored value
    case Op_TauStRef:
        defUseBuilder.addDefUse(inst->getSrc(0)->getInst(),inst,0);
        defUseBuilder.addDefUse(inst->getSrc(1)->getInst(),inst,1);
        break;
    case Op_StVar:
        if (isRefOrPtrType(inst->getSrc(0))) {
            defUseBuilder.addDefUse(inst->getSrc(0)->getInst(),inst,0);
        }
        break;
    case Op_LdVar:
        //
        // In SSA form, ldVar would have a source that is a SSAVar source
        // connected to either a phi or a StVar
        //
        if (isRefOrPtrType(inst->getSrc(0))) {
        }
        break;
    case Op_Phi:    
        break;
    default:
    break;
    }
    //
    // initialize work list according to:
    //
    // (1) Ptrs & refs that are incoming args are free
    // (2) Ptrs & refs returned by calls are free
    // (3) Ptrs & refs returned by the method are free
    // (4) Refs that are thrown by the method are free
    // (5) Refs pass as args to calls are free (ptrs do not escape)
    // (6) Refs that are stored to static fields are free
    // (7) Ptrs to static fields are free
    // (8) inlined return opnd is free
    //
    if (returnOpnd && (inst->getDst() == returnOpnd)) {
        workList.push_back(inst);
    } else {
        switch (inst->getOpcode()) {
        case Op_DefArg:
            if (isRefOrPtrType(inst->getDst())) {
                // this instruction creates a free ptr/ref
                workList.push_back(inst);
            }
            break;
        case Op_DirectCall:   
        case Op_TauVirtualCall:
            {
                if (isRefOrPtrType(inst->getDst())) {
                    // this instruction creates a free ptr/ref
                    workList.push_back(inst);
                }
                for (U_32 i=0; i<inst->getNumSrcOperands(); i++) {
                    if (isRefOrPtrType(inst->getSrc(i))) {
                        workList.push_back(inst->getSrc(i)->getInst());
                    }
                }
            }
            break;
        case Op_IndirectCall:
        case Op_IndirectMemoryCall:
            {
                if (isRefOrPtrType(inst->getDst())) {
                    // this instruction creates a free ptr/ref
                    workList.push_back(inst);
                }
                for (U_32 i=1; i<inst->getNumSrcOperands(); i++) {
                    if (isRefOrPtrType(inst->getSrc(i))) {
                        workList.push_back(inst->getSrc(i)->getInst());
                    }
                }
            }
            break;
        case Op_Return:
            if (isRefOrPtrType(inst->getSrc(0))) {
                workList.push_back(inst->getSrc(0)->getInst());
            }
            break;
        case Op_Throw:
            workList.push_back(inst->getSrc(0)->getInst());
            break;
        case Op_TauStStatic:
            if (isRefOrPtrType(inst->getSrc(0))) {
                workList.push_back(inst->getSrc(0)->getInst());
            }
            break;
        case Op_LdStaticAddr:
            workList.push_back(inst);
            break;
        case Op_StVar:
            if (isRefOrPtrType(inst->getSrc(0))) {
                workList.push_back(inst->getSrc(0)->getInst());            
            }
            break;
    default:
        break;
        }
    }
}

U_32 
EscapeAnalyzer::doAggressiveAnalysis() {
    //
    // Initialization:
    //
    // (1) Ptrs & refs that are incoming args are free
    // (2) Ptrs & refs returned by calls are free
    // (3) Ptrs & refs returned by the method are free
    // (4) Refs that are thrown by the method are free
    // (5) Refs pass as args to calls are free (ptrs do not escape)
    // 
    // Iteration:
    //
    // (6) Refs that are stored through free ptrs or refs are free
    // (7) Refs loaded through free ptrs or refs are free
    //
    MemoryManager memManager("EscapeAnalyzer::doAggressiveAnalysis");
    //
    // work list of instructions that define free refs & ptrs
    //
    StlDeque<Inst*> freeWorkList(memManager);
    DefUseBuilder   defUseBuilder(memManager);
    //
    // Initialization step
    //
    const Nodes& nodes = irManager.getFlowGraph().getNodes();
    Nodes::const_iterator niter;
    Opnd *returnOpnd = irManager.getReturnOpnd();
    for(niter = nodes.begin(); niter != nodes.end(); ++niter) {
        Node* node = *niter;
        Inst *headInst = (Inst*)node->getFirstInst();
        for (Inst* inst=headInst->getNextInst();inst!=NULL; inst=inst->getNextInst()) {
            initialize(inst,freeWorkList,defUseBuilder,returnOpnd);
        }
    }
    //
    // Iteration step 
    // 
   while (freeWorkList.empty() == false) {
       freeWorkList.pop_front();
   }
   U_32 numTrapped = 0;
   return numTrapped;
}

void
DefUseBuilder::initialize(ControlFlowGraph& fg) {
    const Nodes& nodes = fg.getNodes();
    Nodes::const_iterator i;
    for(i = nodes.begin(); i != nodes.end(); ++i) {
        Node* node = *i;
        Inst* label = (Inst*)node->getFirstInst();
        for(Inst* inst = label->getNextInst(); inst != NULL; inst = inst->getNextInst())
            addUses(inst);
    }
}

void
DefUseBuilder::addDefUse(Inst* defInst,Inst* useInst,U_32 srcIndex) {
    DefUseLink* newLink = new (memoryManager) DefUseLink(useInst,srcIndex,NULL);
    newLink->next = defUseTable[defInst];
    defUseTable[defInst] = newLink;
}

void
DefUseBuilder::addUses(Inst* useInst) {
    for (U_32 i=0; i<useInst->getNumSrcOperands(); i++) {
        SsaOpnd* opnd = useInst->getSrc(i)->asSsaOpnd();
        if(opnd != NULL)
            addDefUse(useInst->getSrc(i)->getInst(),useInst,i);
    }
}

DefUseLink*
DefUseBuilder::getDefUse(Inst* defInst, Inst* useInst, U_32 srcIndex) {
    DefUseLink* duLink = defUseTable[defInst];

    while(duLink != NULL) {
        if((duLink->getUseInst() == useInst) && (duLink->getSrcIndex() == srcIndex)) {
            return duLink;
        }
        duLink = duLink->getNext();
    }
    return NULL;
}


void
DefUseBuilder::removeDef(Inst* defInst) {
    defUseTable.erase(defInst);
}

void
DefUseBuilder::removeDefUse(Inst* defInst, Inst* useInst, U_32 srcIndex) {
    DefUseLink* duLink = defUseTable[defInst];
    DefUseLink* prevLink = NULL;

    while(duLink != NULL) {
        if((duLink->getUseInst() == useInst) && (duLink->getSrcIndex() == srcIndex)) {
            if(prevLink == NULL)
                defUseTable[defInst] = duLink->getNext();
            else
                prevLink->next = duLink->getNext();
            return;
        }
        prevLink = duLink;
        duLink = duLink->getNext();
    }
}



} //namespace Jitrino 
