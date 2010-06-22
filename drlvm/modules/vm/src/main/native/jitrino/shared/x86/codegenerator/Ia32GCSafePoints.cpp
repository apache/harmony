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
* @author Mikhail Y. Fursov
*/

#include "Ia32Inst.h"
#include "Ia32GCSafePoints.h"
#include "XTimer.h"
#include "BitSet.h"
#include "Ia32CgUtils.h"


namespace Jitrino
{

#define LIVENESS_FILTER_INST_INTERVAL 128
#define LIVENESS_FILTER_OBJ_INST_INTERVAL 64

static CountTime phase0Timer("ia32::gcpointsinfo::phase0"), 
                 phase1Timer("ia32::gcpointsinfo::phase1"), 
                 phase1Checker("ia32::gcpointsinfo::phase1Checker"), 
                 phase2Timer("ia32::gcpointsinfo::phase2"), 
                 phase2Checker("ia32::gcpointsinfo::phase2Checker"), 
                 saveResultsTimer("ia32::gcpoints::saveResults");

namespace Ia32 {

static ActionFactory<GCPointsBaseLiveRangeFixer> _gcpoints ("gcpoints");

// does nothing except allows to debugger to not loose local stack if method called is last in code
static void dbg_point() {}

// Helper function, removes element idx from vector in O(1) time
// Moves last element in vector to idx and reduce size by 1
// WARN: elements order could be changed!
static void removeElementAt(GCSafePointPairs& pairs, U_32 idx) {
    assert(!pairs.empty());
    U_32 newSize = (U_32)pairs.size() - 1;
    assert(idx<=newSize);
    if (idx < newSize) {
        pairs[idx] = pairs[newSize];
    }
    pairs.resize(newSize);
}


void GCSafePointsInfo::removePairByMPtrOpnd(GCSafePointPairs& pairs, const Opnd* mptr) const {
#ifdef _DEBUG_
    U_32 nPairs = 0;
#endif 
    for (int i = (int)pairs.size(); --i>=0;) {
        MPtrPair& p = pairs[i];
        if (p.mptr == mptr) {
            removeElementAt(pairs, i);
#ifdef _DEBUG_
            nPairs++;
#else
            break; //only one pair in one point for mptr is allowed !
#endif
        }
    }
#ifdef _DEBUG_
    assert(nPairs <= 1);
#endif
}

MPtrPair* GCSafePointsInfo::findPairByMPtrOpnd(GCSafePointPairs& pairs, const Opnd* mptr) {
    for (GCSafePointPairs::iterator it = pairs.begin(), end = pairs.end(); it!=end; ++it) {
        MPtrPair& p = *it;
        if(p.getMptr() == mptr) {
            return &p;
        }
    }
    return NULL;
}

GCSafePointsInfo::GCSafePointsInfo(MemoryManager& _mm, IRManager& _irm, Mode _mode) :
mm(_mm), irm(_irm), pairsByNode(_mm), pairsByGCSafePointInstId(mm), 
livenessFilter(mm), ambiguityFilters(mm), staticMptrs(mm), allowMerging(false), opndsAdded(0), instsAdded(0), mode(_mode)
{
    _calculate();
}


bool GCSafePointsInfo::graphHasSafePoints(const IRManager& irm){
    const Nodes& nodes = irm.getFlowGraph()->getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (node->isBlockNode()) {
            if (blockHasSafePoints((BasicBlock*)node)) {
                return true;
            }
        }
    }
    return false;
}

bool GCSafePointsInfo::blockHasSafePoints(const Node* b) {
    for (Inst * inst=(Inst*)b->getFirstInst(); inst!=NULL; inst=inst->getNextInst()){
        if (isGCSafePoint(inst)) {
            return true;
        }
    }
    return false;
}


U_32 GCSafePointsInfo::getNumSafePointsInBlock(const Node* b, const GCSafePointPairsMap* pairsMap ) {
    U_32 n = 0;
    for (Inst * inst=(Inst*)b->getFirstInst(); inst!=NULL; inst=inst->getNextInst()){
        if (isGCSafePoint(inst)) {
            if (pairsMap!=NULL) { // if pairs!=NULL counts only gcpoints with non-empty pairs
                GCSafePointPairsMap::const_iterator it = pairsMap->find(inst->getId());
                assert(it!=pairsMap->end());
                GCSafePointPairs* pairs = it->second;
                if (pairs->empty()) {
                    continue;
                }
            }
            n++;
        }
    }
    return n;
}


void GCSafePointsInfo::_calculate() {
    assert(pairsByNode.empty());
    assert(pairsByGCSafePointInstId.empty());

    //check if there are no safepoints in cfg at all
    if (!graphHasSafePoints(irm)) {
        return;
    } 
    irm.updateLivenessInfo();
    irm.updateLoopInfo(); //to use _hasLoops property

    insertLivenessFilters();
    allowMerging = true;
    calculateMPtrs();
    if (mode == MODE_2_CALC_OFFSETS) { 
        assert(opndsAdded == 0);
        return;
    }
    if ( opndsAdded > 0 ) {
        irm.invalidateLivenessInfo();
        irm.updateLivenessInfo();
    }
    allowMerging  = false;
    filterLiveMPtrsOnGCSafePoints();
}


void GCSafePointsInfo::insertLivenessFilters() {
    AutoTimer tm(phase0Timer);
    const Nodes& nodes = irm.getFlowGraph()->getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (node->isBlockNode()) {
            U_32 numInsts = node->getInstCount();
            if (numInsts < LIVENESS_FILTER_INST_INTERVAL) {
                continue;
            }
            U_32 objOps  = 0;
            for (Inst* inst = (Inst*)node->getLastInst(); inst!=NULL; inst = inst->getPrevInst()) {
                if ( (inst->getOpndCount() == 0 && ((inst->getKind() == Inst::Kind_MethodEndPseudoInst) 
                        || (inst->getKind() == Inst::Kind_MethodEntryPseudoInst)))
                     || (inst->getMnemonic() == Mnemonic_NOP
                        || inst->getKind() == Inst::Kind_EmptyPseudoInst) )
                {
                    continue; 
                }
                Opnd* opnd = inst->getOpnd(0); // VSH: 0 - ???? 
                if (opnd->getType()->isObject() || opnd->getType()->isManagedPtr()) {
                    objOps++;
                    if (objOps == LIVENESS_FILTER_OBJ_INST_INTERVAL) {
                        break;
                    }
                }
            }
            if (objOps < LIVENESS_FILTER_OBJ_INST_INTERVAL) {
                continue;
            }
             // insert liveness filter for every n-th inst in block
            int nFilters = numInsts / LIVENESS_FILTER_INST_INTERVAL;
            BitSet* ls = new (mm) BitSet(mm, irm.getOpndCount());
            irm.getLiveAtExit(node, *ls);
            int nFiltersLeft = nFilters;
            int instsInInterval = 0;
            for (Inst* inst = (Inst*)node->getLastInst(); nFiltersLeft>0; inst = inst->getPrevInst()) {
                assert(inst!=NULL);
                irm.updateLiveness(inst, *ls);
                instsInInterval++;
                if (instsInInterval == LIVENESS_FILTER_INST_INTERVAL) {
                    nFiltersLeft--;
                    instsInInterval = 0;
                    setLivenessFilter(inst, ls);
                    if (nFiltersLeft != 0) {
                        ls = new (mm) BitSet(*ls);
                    }
                }
            }
        }
    }
}

void GCSafePointsInfo::setLivenessFilter(const Inst* inst, const BitSet* ls) {
    livenessFilter[inst] = ls;
}

const BitSet* GCSafePointsInfo::findLivenessFilter(const Inst* inst) const {
    StlMap<const Inst*, const BitSet*>::const_iterator it = livenessFilter.find(inst);
    if (it == livenessFilter.end()) {
        return NULL;
    }
    return it->second;
}

void GCSafePointsInfo::calculateMPtrs() {
    AutoTimer tm(phase1Timer);
    assert(pairsByNode.empty());
    ControlFlowGraph* fg = irm.getFlowGraph();
    LoopTree* lt =fg->getLoopTree();
    
    const Nodes& postOrderedNodes = fg->getNodesPostOrder();
    
    //prepare structures
    pairsByNode.resize(fg->getNodeCount());
    for (Nodes::const_iterator it = postOrderedNodes.begin(), end = postOrderedNodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (node!=fg->getExitNode() && node!=fg->getUnwindNode()) {
            pairsByNode[node->getDfNum()] = new (mm) GCSafePointPairs(mm);
        }
    }
        
    GCSafePointPairs tmpPairs(mm);
    U_32 nIterations = lt->getMaxLoopDepth() + 1;
    bool changed = true;
    bool restart = false;

#ifdef _DEBUG 
    nIterations++; //one more extra iteration to check on debug that nothing changed
#endif 
    for (U_32 iteration = 0; iteration < nIterations ; ) {
        changed = false;
        for (Nodes::const_reverse_iterator it = postOrderedNodes.rbegin(), end = postOrderedNodes.rend(); it!=end; ++it) {
            Node* node = *it;
            if (node == fg->getExitNode() || node == fg->getUnwindNode()) {
                continue;
            }
            tmpPairs.clear();
            U_32 instsBefore = instsAdded;
            derivePairsOnEntry(node, tmpPairs);
            if (instsBefore!=instsAdded) {
                restart = true;
                break;
            }
            GCSafePointPairs& nodePairs = *pairsByNode[node->getDfNum()];
            if (node->isBlockNode()) {
                for (Inst * inst=(Inst*)node->getFirstInst(); inst!=NULL; inst=inst->getNextInst()){
                    updatePairsOnInst(inst, tmpPairs);
                    if (isGCSafePoint(inst)) { //saving results on safepoint
                        GCSafePointPairs* instPairs = NULL;
                        instPairs = pairsByGCSafePointInstId[inst->getId()];
                        if (instPairs == NULL) {
                            instPairs = new (mm) GCSafePointPairs(mm);
                            pairsByGCSafePointInstId[inst->getId()] = instPairs;
                        }
                        instPairs->clear();
                        instPairs->insert(instPairs->end(), tmpPairs.begin(), tmpPairs.end());
                    }
                }
                if (iteration == 0 || !hasEqualElements(nodePairs, tmpPairs)) {
                    changed = true;
                    nodePairs.swap(tmpPairs);
                }
            } else { 
                nodePairs.swap(tmpPairs);
            }
        }
        if (restart) {  //clear all cached pairs and restart all iterations
            for (Nodes::const_iterator it2 = postOrderedNodes.begin(), end2 = postOrderedNodes.end(); it2!=end2; ++it2) {
                Node* node = *it2;
                GCSafePointPairs* pairs = pairsByNode[node->getDfNum()];
                assert(pairs!=NULL || node->isExitNode() || node == fg->getUnwindNode());
                if (pairs!=NULL) {
                    pairs->clear();
                } 
            }
            iteration = 0;
            restart = false;
            continue;
        }
        if (!changed) {
            break;
        }
        iteration++;
    }
#ifdef _DEBUG
    assert(!changed);
    checkPairsOnNodeExits();
#endif
}

static inline int adjustOffsets(I_32 offsetBefore, I_32 dOffset) {
    if (offsetBefore == MPTR_OFFSET_UNKNOWN || dOffset == MPTR_OFFSET_UNKNOWN) {
        return MPTR_OFFSET_UNKNOWN;
    }
    int res = offsetBefore + dOffset;
    assert(res>=0);
    return res;
}

static bool isHeapBase(Opnd* immOpnd) {
    assert(immOpnd->isPlacedIn(OpndKind_Imm));
#ifndef _EM64T_
    return false;
#else 
    int64 heapBase = (int64)VMInterface::getHeapBase();
    return immOpnd->getImmValue() == heapBase;
#endif
}

I_32 GCSafePointsInfo::getOffsetFromImmediate(Opnd* offsetOpnd) const {
    if (offsetOpnd->isPlacedIn(OpndKind_Immediate)) {
        if (offsetOpnd->getImmValue() == 0 && offsetOpnd->getRuntimeInfo()!=NULL) {
            irm.resolveRuntimeInfo(offsetOpnd);
        }
        assert(!isHeapBase(offsetOpnd));
        return (I_32)offsetOpnd->getImmValue();
    }
    return MPTR_OFFSET_UNKNOWN;
}


void GCSafePointsInfo::runLivenessFilter(Inst* inst, GCSafePointPairs& pairs) const {
    if (!pairs.empty()) {
        const BitSet* livenessFilter = findLivenessFilter(inst);
        if (livenessFilter!=NULL) {
            for (int i = (int)pairs.size(); --i>=0;) {
                MPtrPair& p = pairs[i];
                if (!livenessFilter->getBit(p.mptr->getId())) {
                    removeElementAt(pairs, i);
                }
            }
        }
    }
}

Opnd* GCSafePointsInfo::getBaseAccordingMode(Opnd* opnd) const {
    //MODE_2_CALC_OFFSETS does not have valid base info, because it does not resolves ambiguity!
    return mode == MODE_1_FIX_BASES ? opnd : NULL;
}    


void GCSafePointsInfo::updateMptrInfoInPairs(GCSafePointPairs& res, Opnd* newMptr, Opnd* fromOpnd, int offset, bool fromOpndIsBase) {
   assert(!isStaticFieldMptr(newMptr));
    if (fromOpndIsBase) { //from is base
        removePairByMPtrOpnd(res, newMptr);
        MPtrPair p(newMptr, getBaseAccordingMode(fromOpnd), offset);
        res.push_back(p);
    } else { //fromOpnd is mptr
       MPtrPair* fromPair = findPairByMPtrOpnd(res, fromOpnd);
       assert(fromPair != NULL);
       int newOffset = adjustOffsets(fromPair->offset, offset);
       //remove old pair, add new
       MPtrPair* pair = newMptr == fromOpnd ? fromPair : findPairByMPtrOpnd(res, newMptr);
       if (pair != NULL) { //reuse old pair
           pair->offset = newOffset;
           pair->base = fromPair->base;
           pair->mptr = newMptr;
       } else { // new mptr, was not used before
           MPtrPair toPair(newMptr, fromPair->base, newOffset);
           res.push_back(toPair);
       }
   }
}

static void add_static(Opnd* opnd, StlSet<Opnd*>& set, Opnd* cause) {
    set.insert(opnd);
    if (Log::isEnabled()) {
        Log::out()<<"Registering as static opnd, firstId="<<opnd->getFirstId()<<" reason-opndid:"<<(cause?cause->getFirstId() : (U_32)-1)<<std::endl;
    }
}

void GCSafePointsInfo::filterStaticMptrs(Inst* inst) {
    if (inst->hasKind(Inst::Kind_CallInst)) {
        CallInst* callInst = (CallInst*)inst;
        Opnd::RuntimeInfo * rt = callInst->getRuntimeInfo();
        if (rt && rt->getKind() == Opnd::RuntimeInfo::Kind_HelperAddress
            && ((VM_RT_SUPPORT)(POINTER_SIZE_INT)rt->getValue(0) == VM_RT_GET_STATIC_FIELD_ADDR_WITHRESOLVE))
        {
            Opnd* callRes = callInst->getOpnd(0);
            add_static(callRes, staticMptrs, NULL);
        }
    } else if (inst->getMnemonic() == Mnemonic_MOV) {
        Opnd* toOpnd = inst->getOpnd(0);
        Opnd* fromOpnd = inst->getOpnd(1);
        if (!isManaged(fromOpnd) && !isManaged(toOpnd)) {
            return;
         }
        if (fromOpnd->isPlacedIn(OpndKind_Mem) && fromOpnd->getMemOpndKind() == MemOpndKind_Heap) {
            //loading value from memory -> can be object only, not is a mptr to a static field
            return;
        }
        if (staticMptrs.find(fromOpnd)!=staticMptrs.end()) {
            add_static(toOpnd, staticMptrs, fromOpnd);
        } else if (fromOpnd->isPlacedIn(OpndKind_Imm)) { //imms are compile-time known values -> can only be static
            Opnd::RuntimeInfo* info = fromOpnd->getRuntimeInfo();
            bool isStaticFieldMptr = info!=NULL && info->getKind() == Opnd::RuntimeInfo::Kind_StaticFieldAddress;
            if (isStaticFieldMptr) {
                add_static(fromOpnd, staticMptrs, NULL);
                add_static(toOpnd, staticMptrs, fromOpnd);
            }
        }
   }
}


void GCSafePointsInfo::updatePairsOnInst(Inst* inst, GCSafePointPairs& res) {
    runLivenessFilter(inst, res);//filter pairs with dead mptrs from list

    Inst::Opnds opnds(inst, Inst::OpndRole_Explicit | Inst::OpndRole_Auxilary |Inst::OpndRole_UseDef);
    U_32 defIndex = opnds.begin();
    U_32 useIndex1 = opnds.next(defIndex);

    if (defIndex >= opnds.end() || useIndex1 >= opnds.end() 
        || (inst->getOpndRoles(defIndex) & Inst::OpndRole_Def) == 0 
        || (inst->getOpndRoles(useIndex1) & Inst::OpndRole_Use) == 0 )
    {
        return;
    }
    Opnd* opnd = inst->getOpnd(defIndex);
    Type* opndType = opnd->getType();
    if (!opndType->isObject() && !opndType->isManagedPtr()) {
        return;
    }
   filterStaticMptrs(inst);
   if (isStaticFieldMptr(opnd)) {
        removePairByMPtrOpnd(res, opnd);
       return; // no more analysis required
   }
   if (mode == MODE_1_FIX_BASES) { //3 addr form
        if (opndType->isObject()) { // MODE_1_FIX_BASES -> obj & mptr types are not coalesced
            StlMap<Inst*, Opnd*>::const_iterator ait;
            if (!ambiguityFilters.empty() && ((ait = ambiguityFilters.find(inst))!=ambiguityFilters.end())) {
                //mptr ambiguity filter -> replace old base with new(opnd) in pairs
                const Opnd* mptr = ait->second;
                MPtrPair* pair =  findPairByMPtrOpnd(res, mptr);
#ifdef _DEBUG
                Opnd* oldBase = inst->getOpnd(useIndex1); //inst is: newBase = oldBase
                assert(pair!=NULL);
                assert(pair->getBase() == oldBase || pair->getBase() == opnd); // ==opnd-> already resolved in dominant block
#endif

                pair->base = opnd;
            }
        } else { //def of mptr
            // detect which operand is base
            Opnd* fromOpnd = inst->getOpnd(useIndex1);
            U_32 useIndex2 = opnds.next(useIndex1);
            Opnd* fromOpnd2 = useIndex2!=opnds.end() ? inst->getOpnd(useIndex2) : NULL;
            if (!(fromOpnd->getType()->isObject() || fromOpnd->getType()->isManagedPtr())) {
                assert(fromOpnd2!=NULL);
                Opnd* tmp = fromOpnd; fromOpnd = fromOpnd2; fromOpnd2 = tmp;
            }
            assert(fromOpnd->getType()->isObject() || fromOpnd->getType()->isManagedPtr());
            I_32 offset = MPTR_OFFSET_UNKNOWN;
            if (inst->getMnemonic() == Mnemonic_LEA) {
                assert(fromOpnd->isPlacedIn(OpndKind_Memory));
                Opnd* scaleOpnd = fromOpnd->getMemOpndSubOpnd(MemOpndSubOpndKind_Scale);
                if (scaleOpnd == NULL) {
                    Opnd* displOpnd = fromOpnd->getMemOpndSubOpnd(MemOpndSubOpndKind_Displacement);
                    assert(displOpnd!=NULL);
                    offset = (I_32)displOpnd->getImmValue();
                }
                fromOpnd = fromOpnd->getMemOpndSubOpnd(MemOpndSubOpndKind_Base);
            } else if (fromOpnd2!=NULL) {
                Opnd* offsetOpnd = fromOpnd2; 
                offset = getOffsetFromImmediate(offsetOpnd);
            }
            updateMptrInfoInPairs(res, opnd, fromOpnd, offset, fromOpnd->getType()->isObject());
            dbg_point();
        }
    } else { // mode == MODE_2_CALC_OFFSETS   - 2 addr form
        //we can't rely on base/mptr type info here.
        //algorithm:
        if (inst->hasKind(Inst::Kind_ControlTransferInst) ||
            inst->getMnemonic() == Mnemonic_MOVS8         ||
            inst->getMnemonic() == Mnemonic_MOVS16        ||
            inst->getMnemonic() == Mnemonic_MOVS32        ||
            inst->getMnemonic() == Mnemonic_MOVS64        ||
            inst->getMnemonic() == Mnemonic_CMPSB         ||
            inst->getMnemonic() == Mnemonic_CMPSW         ||
            inst->getMnemonic() == Mnemonic_CMPSD         ) {
            //Do nothing, calls return only bases
        } else {
            Opnd* fromOpnd = NULL;
            I_32 offset = 0;
            Mnemonic mn = inst->getMnemonic();

            Mnemonic conditionalMnem = getBaseConditionMnemonic(mn);
            if (conditionalMnem != Mnemonic_NULL)
                mn = conditionalMnem;

            switch (mn) {
                case Mnemonic_XOR:
                case Mnemonic_MOV: 
                case Mnemonic_CMOVcc:
                    assert(mn != Mnemonic_XOR || inst->getOpnd(useIndex1)==opnd);
                    fromOpnd = inst->getOpnd(useIndex1);
                    break;                
                case Mnemonic_ADD: { 
                case Mnemonic_SUB:
                        fromOpnd = opnd;
                        Opnd* offsetOpnd = inst->getOpnd(useIndex1);
                        Opnd* immOffset = OpndUtils::findImmediateSource(offsetOpnd);
                        if (immOffset) {
                           if (isHeapBase(immOffset)) {
                               offset = 0;
                           } else {
                               offset = getOffsetFromImmediate(immOffset);
                           }
                        } else {
                           offset = MPTR_OFFSET_UNKNOWN;
                        }
                    }
                    break;
                case Mnemonic_LEA: {
                        fromOpnd = inst->getOpnd(useIndex1);
                        assert(fromOpnd->isPlacedIn(OpndKind_Memory));
                        Opnd* scaleOpnd = fromOpnd->getMemOpndSubOpnd(MemOpndSubOpndKind_Scale);
                        if (scaleOpnd == NULL) {
                            Opnd* displOpnd = fromOpnd->getMemOpndSubOpnd(MemOpndSubOpndKind_Displacement);
                            assert(displOpnd!=NULL);
                            offset = (I_32)displOpnd->getImmValue();
                        } else {
                            offset = MPTR_OFFSET_UNKNOWN;
                        }
                        fromOpnd = fromOpnd->getMemOpndSubOpnd(MemOpndSubOpndKind_Base);
                    }
                    break;
                default: assert(0);
            }
            bool fromIsMptrOrObj = fromOpnd->getType()->isObject() || fromOpnd->getType()->isManagedPtr();
            MPtrPair* fromPair = fromIsMptrOrObj ? findPairByMPtrOpnd(res, fromOpnd) : NULL;

            if (fromPair != NULL || offset!=0)  {//opnd is mptr -> update pairs
                updateMptrInfoInPairs(res, opnd, fromOpnd, offset,  fromPair == NULL);
            } else {
                //new def of base -> we must remove all pairs where opnd acts as base or mptr 
                //the problem is that in MODE2 we do not save info about bases (no ambiguity resolution is done) 
                //so we can't match pairs where opnd is a base. 
                //Solution: let pairs derived from this base to live, due to the fact that 
                //such pairs must have static offset and base redefinition will not corrupt info. 
                //For pairs with unknown offset there is a live base -> MODE1 cares about it
                removePairByMPtrOpnd(res, opnd);
            }
        }
    } // else mode2
}



void GCSafePointsInfo::derivePairsOnEntry(const Node* node, GCSafePointPairs& res) {
    assert(res.empty());
    // optimization: filter out those mptrs that are not live at entry.
    // this situation is possible because of updatePairsOnInst() function does not track liveness
    // and allows dead operands in pairs for block end;
    const BitSet* ls = irm.getLiveAtEntry(node); 
    const Edges& edges=node->getInEdges();
    
    assert(irm.getFlowGraph()->hasValidOrdering());

    //step1: add all live pairs from pred edges into res
    for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
        Edge* edge = *ite;
        Node * predNode =edge->getSourceNode();
        const GCSafePointPairs& predPairs = *pairsByNode[predNode->getDfNum()];
        res.reserve(res.size() + predPairs.size());
        for (GCSafePointPairs::const_iterator it = predPairs.begin(), end = predPairs.end(); it!=end; ++it) {
            const MPtrPair& predPair = *it;
            if (ls->getBit(predPair.mptr->getId())) {
                res.push_back(predPair);//by value
            }
        }
    }

    //step 2: merge pairs with the same (mptr, base) and process ambiguos pairs
    bool needToFilterDeadPairs = false;
    U_32 instsAddedBefore = instsAdded;
    std::sort(res.begin(), res.end());
    for (U_32 i=0, n = (U_32)res.size(); i < n; i++) {
        MPtrPair& p1 = res[i];
        Opnd* newBase = NULL; //new base opnd to merge ambiguos mptrs
        while (i+1 < n) {
            const MPtrPair& p2 = res[i+1];
            if (p1.mptr == p2.mptr) {
                if (p1.base == p2.base) { //same base & mptr -> merge offset
                    if (p1.offset != p2.offset) { 
                        p1.offset = MPTR_OFFSET_UNKNOWN;
                    }
                } else { //equal mptrs with a different bases -> add var=baseX to prevBlocks and replace pairs with new single pair
                    assert(mode == MODE_1_FIX_BASES);
                    assert(allowMerging);
                    if (newBase == NULL) { //first pass for this ambiguous mptrs fix them all
                        newBase = irm.newOpnd(p1.base->getType());
                        opndsAdded++;
                        U_32 dInsts = 0;
                        for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
                            Edge* edge = *ite;
                            Node * predNode =edge->getSourceNode();
                            GCSafePointPairs& predPairs = *pairsByNode[predNode->getDfNum()];
                            for (GCSafePointPairs::iterator it = predPairs.begin(), end = predPairs.end(); it!=end; ++it) {
                                MPtrPair& predPair = *it;
                                if (predPair.mptr == p1.mptr) { //ok remove this pair, add vardef, add new pair
                                    Opnd* oldBase = predPair.base;
                                    assert(predNode->isBlockNode());
                                    BasicBlock* predBlock = (BasicBlock*)predNode;
                                    //add var def inst to predBlock
                                    Inst* baseDefInst = irm.newCopyPseudoInst(Mnemonic_MOV, newBase, oldBase);
                                    Inst* lastInst = (Inst*)predBlock->getLastInst();
                                    if (lastInst!=NULL && lastInst->hasKind(Inst::Kind_ControlTransferInst)) {
                                        baseDefInst->insertBefore(lastInst);
                                    } else {
                                        predBlock->appendInst(baseDefInst);
                                    }
                                    if (predPair.offset!=p1.offset) {
                                        p1.offset = MPTR_OFFSET_UNKNOWN;
                                    }
                                    dInsts++;
                                    ambiguityFilters[baseDefInst] = p1.mptr;
                                    //and now we should remove oldBase from next nodes in topological ordering
                                    //oldBase could be cached and merging process could be started for oldBase and newBase in inner loops heads
                                    //we will do it one time after the all predPairs is processed for the current node
                                    break;//go to next node processing
                                }
                            }
                        }
                        p1.base = newBase;
                        assert(dInsts > 1);
                        instsAdded+=dInsts;
                    }
                }
                needToFilterDeadPairs = true;
                i++;
            } else {
                break;
            }
        } //while
    }

    //step3:
    if (instsAdded==instsAddedBefore && needToFilterDeadPairs) { //if nstsAdded!=instsAddedBefore -> recalculation restarts
        //step 3: leave only p1(see step2) pairs
        //unique -> remove all pairs with equal mptr but the first
        GCSafePointPairs::iterator newEnd = std::unique(res.begin(), res.end(), &MPtrPair::equalMptrs); 
        res.resize(newEnd - res.begin());
    }


}

void GCSafePointsInfo::filterLiveMPtrsOnGCSafePoints() {
    AutoTimer tm(phase2Timer);
    assert(!pairsByNode.empty());
    //unoptimized impl -> analyzing all blocks -> could be moved to GCMap
    const Nodes& nodes = irm.getFlowGraph()->getNodes();
    BitSet ls(mm, irm.getOpndCount());
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (!node->isBlockNode()) {
            continue;
        }
        U_32 nSafePoints = getNumSafePointsInBlock((BasicBlock*)node, &pairsByGCSafePointInstId);
        if (nSafePoints == 0) {
            continue;
        }
        // ok this basic block has safepoints        
        U_32 nSafePointsTmp = nSafePoints;
        // remove pairs with dead mptr, use liveness to do it;
        ls.clear();
        irm.getLiveAtExit(node, ls);
        nSafePointsTmp = nSafePoints;
        for (Inst * inst=(Inst*)node->getLastInst();;inst=inst->getPrevInst()) {
            assert(inst!=NULL);
            if (isGCSafePoint(inst)) {
                GCSafePointPairs& pairs  = *pairsByGCSafePointInstId[inst->getId()];
                if (!pairs.empty()) {
                    nSafePointsTmp--;
                    for (int i = (int)pairs.size(); --i>=0;) {
                        MPtrPair& p = pairs[i];
                        if (!ls.getBit(p.mptr->getId())) {
                            removeElementAt(pairs, i);
                        }
                    }
                }
                if (nSafePointsTmp == 0) {
                    break;
                }
            }
            irm.updateLiveness(inst, ls); 
        }
    }
#ifdef _DEBUG
    checkPairsOnGCSafePoints();
#endif
}

/// checks that sets of mptrs that come to any node from any edge are equal
void GCSafePointsInfo::checkPairsOnNodeExits() const {
    AutoTimer tm(phase1Checker);
    assert(!pairsByNode.empty());
    ControlFlowGraph* fg = irm.getFlowGraph();
    const Nodes& nodes = fg->getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it ) {
        Node* node = *it;
        if (node == fg->getUnwindNode() || node==fg->getExitNode()) {
            continue;
        }
        const BitSet* ls = irm.getLiveAtEntry(node);
        if(node->hasTwoOrMorePredEdges()) {
            const Edges& edges=node->getInEdges();
            Edge* edge1 = edges.front();
            Node * predNode1 =edge1->getSourceNode();
            const GCSafePointPairs& pairs1 = *pairsByNode[predNode1->getDfNum()];
            //ensure that first edge mptr set is equal to edge2..N set
            for (Edges::const_iterator ite = edges.begin() + 1, ende = edges.end(); ite!=ende; ++ite) {
                Edge* edge2 = *ite;
                Node * predNode2 =edge2->getSourceNode();
                const GCSafePointPairs& pairs2 = *pairsByNode[predNode2->getDfNum()];
                //now check that for every mptr in pairs1 there is a pair in pairs2 with the same mptr
                for (U_32 i1 = 0, n1 = (U_32)pairs1.size();i1<n1; i1++) {
                    const MPtrPair& p1 = pairs1[i1];
                    if (ls->getBit(p1.mptr->getId())) {
                        for (U_32 i2 = 0; ; i2++) {
                            assert(i2 < pairs2.size());
                            const MPtrPair& p2 = pairs2[i2];
                            if (p1.mptr == p2.mptr) {
                                break;
                            }
                        }
                    }
                }
                // and vice versa
                for (U_32 i2 = 0, n2 = (U_32)pairs2.size();i2<n2; i2++) {
                    const MPtrPair& p2 = pairs2[i2];
                    if (ls->getBit(p2.mptr->getId())) {
                        for (U_32 i1 = 0; ; i1++) {
                            assert(i1 < pairs1.size());
                            const MPtrPair& p1 = pairs1[i1];
                            if (p1.mptr == p2.mptr) {
                                break;
                            }
                        }
                    }
                }
            } //for edge2...edge[N]
        }
    }
}

// checks that we've collected pairs for every live mptr on safe point
void GCSafePointsInfo::checkPairsOnGCSafePoints() const {
    if (mode == MODE_2_CALC_OFFSETS) { // check is done using type info. Type info is not available for mode2
        return;
    }
    AutoTimer tm(phase2Checker);
    const Nodes& nodes = irm.getFlowGraph()->getNodes();
    U_32 nOpnds = irm.getOpndCount();
    BitSet ls(mm, nOpnds);
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (!node->isBlockNode()) {
            continue;
        }
        U_32 nSafePoints = getNumSafePointsInBlock((BasicBlock*)node);
        if (nSafePoints== 0) {
            continue;
        }
        U_32 nSafePointsTmp = nSafePoints;
        irm.getLiveAtExit(node, ls);
        for (Inst * inst=(Inst*)node->getLastInst(); nSafePointsTmp > 0; inst=inst->getPrevInst()) {
            assert(inst!=NULL);
            if (isGCSafePoint(inst)) {
                nSafePointsTmp--;
                GCSafePointPairsMap::const_iterator mit = pairsByGCSafePointInstId.find(inst->getId());
                assert(mit!=pairsByGCSafePointInstId.end());
                const GCSafePointPairs& pairs = *mit->second; 
                BitSet::IterB liveOpnds(ls);
                for (int i = liveOpnds.getNext(); i != -1; i = liveOpnds.getNext()) {
                    Opnd* opnd = irm.getOpnd(i);
                    if (opnd->getType()->isManagedPtr()) {
                        if (staticMptrs.find(opnd)!= staticMptrs.end()) {
                            continue;
                        }
                        for (U_32 j=0; ; j++) {
                            assert(j<pairs.size());
                            const MPtrPair&  pair = pairs[j];
                            if (pair.mptr == opnd) {
                                break;
                            }
                        }
                    }
                } //for opnds
            }
            irm.updateLiveness(inst, ls);
        }
    }
}

static U_32 select_1st(const std::pair<U_32, GCSafePointPairs*>& p) {return p.first;}


void GCSafePointsInfo::dump(const char* stage) const {
    Log::out()<<"========================================================================"<<std::endl;
    Log::out()<<"__IR_DUMP_BEGIN__: pairs dump"<<std::endl;
    Log::out()<<"========================================================================"<<std::endl;
    //sort by inst id
    const GCSafePointPairsMap& map = pairsByGCSafePointInstId;
    StlVector<U_32> insts(mm, map.size());
    std::transform(map.begin(), map.end(), insts.begin(), select_1st);
    std::sort(insts.begin(), insts.end());
    
    //for every inst sort by mptr id and dump
    for (size_t i = 0; i<insts.size(); i++) {
        U_32 id = insts[i];
        const GCSafePointPairs* pairs = map.find(id)->second;
        Log::out()<<"inst="<<id<<" num_pairs="<<pairs->size()<<std::endl;
        GCSafePointPairs cloned = *pairs;
        std::sort(cloned.begin(), cloned.end());
        for(size_t j=0; j<cloned.size(); j++) {
            MPtrPair& p = cloned[j];
            Log::out()<<"    mptr="<< p.mptr->getFirstId()
                <<" base="<<(p.base!=NULL?(int)p.base->getFirstId():-1)
                <<" offset="<<p.offset<<std::endl;
        }
    }
    Log::out()<<"========================================================================"<<std::endl;
    Log::out()<<"__IR_DUMP_END__: pairs dump"<<std::endl;
    Log::out()<<"========================================================================"<<std::endl;

}

#define MAX(a,b) (((a) > (b)) ? (a) : (b))

void GCPointsBaseLiveRangeFixer::runImpl() {
    bool disableStaticOffsets = false;
    getArg("disable_static_offsets", disableStaticOffsets);
    MemoryManager mm("GCSafePointsMarker");
    GCSafePointsInfo info(mm, *irManager, GCSafePointsInfo::MODE_1_FIX_BASES);
    
    if (Log::isEnabled()) {
        info.dump(getTagName());
    }

    if(!info.hasPairs()) {
        return;
    }
    AutoTimer tm(saveResultsTimer);
    const Nodes& nodes = irManager->getFlowGraph()->getNodes();
    StlVector<Opnd*> basesAndMptrs(mm); 
    StlVector<I_32> offsets(mm);
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node *node = *it;
        if (!node->isBlockNode()) {
            continue;
        }
        for (Inst * inst=(Inst*)node->getFirstInst(); inst!=NULL; inst=inst->getNextInst()) {
            if (IRManager::isGCSafePoint(inst)) {
                const GCSafePointPairs& pairs = info.getGCSafePointPairs(inst);
                if (pairs.empty()) {
                    continue;
                }
                sideEffect = SideEffect_InvalidatesLivenessInfo;
                basesAndMptrs.clear(); 
                offsets.clear();
                if (!disableStaticOffsets) {
                    //bases to adjust liveness info. No bases from pairs with valid static offsets get into this set
                    for (GCSafePointPairs::const_iterator it = pairs.begin(), end = pairs.end(); it!=end; ++it) {
                        const MPtrPair& p = *it;
                        if (p.getOffset() == MPTR_OFFSET_UNKNOWN) { // adjust base live range
                            Opnd* base = p.getBase();
                            if (std::find(basesAndMptrs.begin(), basesAndMptrs.end(), base) == basesAndMptrs.end()) {
                                basesAndMptrs.push_back(p.getBase());
                                offsets.push_back(0);
                            }
                        }
                        
                    }
                } else { //ignore static offsets info, adjust live range for all bases
                    std::transform(pairs.begin(), pairs.end(), basesAndMptrs.begin(), std::mem_fun_ref(&MPtrPair::getBase));
                    std::sort(basesAndMptrs.begin(), basesAndMptrs.end());
                    StlVector<Opnd*>::iterator newEnd = std::unique(basesAndMptrs.begin(), basesAndMptrs.end());
                    basesAndMptrs.resize(newEnd - basesAndMptrs.begin());
                    std::fill(offsets.begin(), offsets.end(), 0);
                }                
                if (!basesAndMptrs.empty()) {
                    GCInfoPseudoInst* gcInst = irManager->newGCInfoPseudoInst(basesAndMptrs);
                    gcInst->desc = getTagName();
                    gcInst->offsets.resize(offsets.size());
                    std::copy(offsets.begin(), offsets.end(), gcInst->offsets.begin());
                    gcInst->insertAfter(inst);
                }
            } //if inst is gc safe point
        } //for insts
    }
}

}} //namespace

