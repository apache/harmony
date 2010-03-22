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
 * @author Intel, Konstantin M. Anisimov, Igor V. Chebykin
 *
 */

#include "IpfRuntimeSupport.h"
#include "IpfIrPrinter.h"
#include "IpfLiveAnalyzer.h"

namespace Jitrino {
namespace IPF {

//----------------------------------------------------------------------------------------//

bool greaterPriority(Edge *edge1, Edge *edge2) {
    
    EdgeKind kind1 = edge1->getEdgeKind();
    EdgeKind kind2 = edge2->getEdgeKind();
    if (kind1 != EDGE_EXCEPTION || kind2 != EDGE_EXCEPTION) return false;
        
    return ((ExceptionEdge *)edge1)->getPriority() < ((ExceptionEdge *)edge2)->getPriority();
}

//========================================================================================//
// TryRegion
//========================================================================================//

TryRegion::TryRegion(U_8*        startAddr,
                     U_8*        endAddr,
                     U_8*        handlerAddr,
                     ObjectType* exceptionType,
                     bool        isExceptionObjDead) :
    startAddr(startAddr),
    endAddr(endAddr),
    handlerAddr(handlerAddr),
    exceptionType(exceptionType),
    isExceptionObjDead(isExceptionObjDead) {
}

//========================================================================================//
// RuntimeSupport
//========================================================================================//

RuntimeSupport::RuntimeSupport(Cfg &cfg, CompilationInterface &compilationInterface) :
    mm(cfg.getMM()), 
    cfg(cfg),
    compilationInterface(compilationInterface),
    tryRegions(mm),
    safePoints(mm),
    mptr2def(mm) {
        
    opndManager = cfg.getOpndManager();
}

//----------------------------------------------------------------------------------------//
// Runtime info
//   Stack info
//   Root Set info
//
// Stack info structure
//   see struct StackInfo in IpfRuntimeInterface.h
//
// Root set info structure
//                +---------------+
//   header       |  4 bytes      |  block size in bytes   
//                +---------------+  
//   safe point n |  4 bytes      |  safe poin info size in bytes
//                |  8 bytes      |  safe point address
//                |  4 or 8 bytes |  base or mptr+base
//                +---------------+

void RuntimeSupport::makeRuntimeInfo() {

    IPF_LOG << endl << "  Register Exception Handlers" << dec << endl;
    registerExceptionHandlers();

    IPF_LOG << endl << "  Make Stack Info" << endl;
    StackInfo *stackInfo = makeStackInfo();
    U_32 stackInfoSize = sizeof(StackInfo);
    IPF_LOG << "    stack info size (bytes): " << stackInfoSize << endl;

    IPF_LOG << endl << "  Make Root Seet Info" << endl;
    Uint32Vector rootSetInfo(mm);
    makeRootSetInfo(rootSetInfo);
    U_32 rootSetInfoSize = ROOT_SET_HEADER_SIZE + rootSetInfo.size() * sizeof(U_32);
    IPF_LOG << "    GC root set info size (bytes): " << rootSetInfoSize << endl;

    // create info block
    U_32 infoBlockSize = stackInfoSize + rootSetInfoSize;
    U_8*   infoBlock     = compilationInterface.allocateInfoBlock(infoBlockSize);

    // write stack info
    *((StackInfo *)infoBlock) = *stackInfo;

    // write root set info
    U_32 *gcInfo = (U_32 *)(infoBlock + sizeof(StackInfo));
    U_32 j       = ROOT_SET_HEADER_SIZE / sizeof(U_32); 
    gcInfo[0] = rootSetInfoSize;
    for (U_32 i=0; i<rootSetInfo.size(); i++, j++) {
        gcInfo[j] = rootSetInfo[i];
    }
}

//----------------------------------------------------------------------------------------//
// Exception registration
//----------------------------------------------------------------------------------------//

void RuntimeSupport::registerExceptionHandlers() {
    
    NodeVector &nodes        = cfg.search(SEARCH_LAYOUT_ORDER);
    BbNode     *regionStart  = (BbNode *)nodes[0];
    Node       *dispatchNode = regionStart->getDispatchNode();
    
    inserFakeLastNode(nodes);
    for (uint16 i=1; i<nodes.size(); i++) {
        if (nodes[i]->getNodeKind() != NODE_BB) continue; // ignore non BB node
        BbNode *node = (BbNode *)nodes[i];

        if (dispatchNode != node->getDispatchNode()) {    // if current node protected by another dispatch 
            makeRegion(regionStart, node, dispatchNode);  // create new try region
            regionStart  = node;                          // current node is first node of new region (if any)
            dispatchNode = node->getDispatchNode();       // current dispatch protects new region
        }
    }

    makeRegion(regionStart, (BbNode *)nodes.back(), dispatchNode);  // create new try region

    MethodDesc* md = compilationInterface.getMethodToCompile();
    md->setNumExceptionHandler(tryRegions.size());
    IPF_LOG << endl << "    region registration:" << endl;
    if (tryRegions.size() == 0) {
        IPF_LOG << "      no catch handlers detected" << endl;
        return;
    }

    IPF_LOG << "      start            end              handler          exception                           objIsDead" << endl;
    for (uint16 i=0; i<tryRegions.size(); i++) {
        md->setExceptionHandlerInfo(i, 
            tryRegions[i]->startAddr, 
            tryRegions[i]->endAddr,
            tryRegions[i]->handlerAddr,
            tryRegions[i]->exceptionType,
            tryRegions[i]->isExceptionObjDead);

        IPF_LOG << "      " << hex << (uint64)tryRegions[i]->startAddr;
        IPF_LOG << " " << (uint64)tryRegions[i]->endAddr;
        IPF_LOG << " " << (uint64)tryRegions[i]->handlerAddr;
        IPF_LOG << " " << setw(35) << tryRegions[i]->exceptionType->getName();
        IPF_LOG << " " << boolalpha << tryRegions[i]->isExceptionObjDead << dec << endl;
    }    
}

//----------------------------------------------------------------------------------------//

void RuntimeSupport::makeRegion(BbNode *regionStart, BbNode *regionEnd, Node *dispatchNode) {

    if (dispatchNode == NULL) return;   // it is not a try region
    
    IPF_LOG << "    try region detected: node" << regionStart->getId() << " - node" << regionEnd->getId();

    if (dispatchNode->getNodeKind() != NODE_DISPATCH) { 
        IPF_LOG << ", there is no catch block" << endl;
        return; 
    }

    IPF_LOG << ", dispatch: node" << dispatchNode->getId();

    U_8*        startAddr = (U_8*)regionStart->getAddress();
    U_8*        endAddr   = (U_8*)regionEnd->getAddress();
    EdgeVector &outEdges  = dispatchNode->getOutEdges();     // get out edges   
    sort(outEdges.begin(), outEdges.end(), greaterPriority); // sort them by Priority

    for (uint16 i=0; i<outEdges.size(); i++) {
        Edge *edge = outEdges[i];
        if (edge->getEdgeKind() == EDGE_EXCEPTION) {

            BbNode*     handlerNode   = (BbNode*)edge->getTarget();
            U_8*        handlerAddr   = (U_8*)handlerNode->getAddress();
            ObjectType* exceptionType = (ObjectType *)((ExceptionEdge *)edge)->getExceptionType();
            TryRegion*  region = new(mm) TryRegion(startAddr, endAddr, handlerAddr, exceptionType, false);

            tryRegions.push_back(region);
            IPF_LOG << ", handler: node" << handlerNode->getId();
            IPF_LOG << ", priority: " << ((ExceptionEdge *)edge)->getPriority();
            IPF_LOG << ", " << exceptionType->getName() << endl;
        }
        
        if (edge->getEdgeKind() == EDGE_DISPATCH) { 
            makeRegion(regionStart, regionEnd, edge->getTarget());
        }
    }
}

//----------------------------------------------------------------------------------------//

void RuntimeSupport::inserFakeLastNode(NodeVector &nodes) {
    
    BbNode     *lastNode = (BbNode *)nodes.back();
    InstVector &insts    = lastNode->getInsts();
    uint64     address   = 0;
    
    if (insts.size() < 1) address = lastNode->getAddress();
    else                  address = lastNode->getInstAddr(insts.back());

    uint64 mask = 0xfffffffffffffff0;
    address = (address & mask) + 0x10;

    BbNode *node = new(mm) BbNode(mm, 0, 0);
    node->setAddress(address);
    nodes.push_back(node);
}

//----------------------------------------------------------------------------------------//
// Make info block which will be used in stack unwind routine
//----------------------------------------------------------------------------------------//

StackInfo* RuntimeSupport::makeStackInfo() {
    
    I_32  rpBak          = opndManager->rpBak;
    I_32  prBak          = opndManager->prBak;
    I_32  pfsBak         = opndManager->pfsBak;
    I_32  unatBak        = opndManager->unatBak;
    U_32 savedGrMask    = opndManager->savedGrMask;
    U_32 savedFrMask    = opndManager->savedFrMask;
    U_32 savedBrMask    = opndManager->savedBrMask;
    U_32 memStackSize   = opndManager->memStackSize;
    I_32  savedBase      = opndManager->savedBase;

    if(LOG_ON) {
        if (rpBak!=LOCATION_INVALID   && rpBak>=S_OUTARG_BASE)      { IPF_ERR << " rpBak = " << rpBak << endl; }
        if (prBak!=LOCATION_INVALID   && prBak >= S_OUTARG_BASE)    { IPF_ERR << " prBak = " << prBak << endl; }
        if (pfsBak!=LOCATION_INVALID  && pfsBak >= S_OUTARG_BASE)   { IPF_ERR << " pfsBak = " << pfsBak << endl; }
        if (unatBak!=LOCATION_INVALID && unatBak >= S_OUTARG_BASE)  { IPF_ERR << " unatBak = " << unatBak << endl; }
        
        if (rpBak==LOCATION_INVALID)   { IPF_LOG << "    return pointer is not saved" << endl; }
        else if (rpBak >= S_BASE)      { IPF_LOG << "    return pointer saved on stack offset   " << rpBak-S_BASE << endl; }
        else                           { IPF_LOG << "    return pointer saved on reg            " << rpBak << endl; }

        if (prBak==LOCATION_INVALID)   { IPF_LOG << "    pred registers are not saved" << endl; }
        else if (prBak >= S_BASE)      { IPF_LOG << "    pred registers saved on stack offset   " << prBak-S_BASE << endl; }
        else                           { IPF_LOG << "    pred registers saved on reg            " << prBak << endl; }

        if (pfsBak==LOCATION_INVALID)  { IPF_LOG << "    pfs register is not saved  " << endl; }
        else if (pfsBak >= S_BASE)     { IPF_LOG << "    pfs register saved on stack offset     " << pfsBak-S_BASE << endl; }
        else                           { IPF_LOG << "    pfs register saved on reg              " << pfsBak << endl; }

        if (unatBak==LOCATION_INVALID) { IPF_LOG << "    unat register is not saved  " << endl; }
        else if (unatBak >= S_BASE)    { IPF_LOG << "    unat register saved on stack offset    " << unatBak-S_BASE << endl; }
        else                           { IPF_LOG << "    unat register saved on reg             " << unatBak << endl; }

        IPF_LOG << "    memory stack offset for preserved regs " << savedBase << endl;
        IPF_LOG << "      saved general registers  (hex mask)  " << hex << savedGrMask << endl;
        IPF_LOG << "      saved floating registers (hex mask)  " << hex << savedFrMask << endl;
        IPF_LOG << "      saved branch registers   (hex mask)  " << hex << savedBrMask << endl;
        IPF_LOG << "    stack size                             " << dec << memStackSize << endl;
    }
    
    return (StackInfo *)opndManager;
}

//----------------------------------------------------------------------------------------//
// Build GC Root Set
//----------------------------------------------------------------------------------------//
// - Build set of alive mptrs and bases for each safe point (call instruction)
// - Build mptr->base dependencies for all mptrs (mptr = base + const)
// - Extend base live ranges. For each mptr alive on safe point we must report base. Thus, the base 
//   must be alive on the safe point. 

void RuntimeSupport::buildRootSet() {
    
    LiveManager   liveManager(cfg);
    RegOpndSet    &liveSet = liveManager.getLiveSet();
    NodeVector    &nodes   = cfg.search(SEARCH_POST_ORDER);

    for (uint16 i=0; i<nodes.size(); i++) {              // iterate through CFG nodes

        if (nodes[i]->isBb() == false) continue;         // non BB node - ignore

        BbNode *node = (BbNode *)nodes[i];
        liveManager.init(node);

        InstVector   &insts    = node->getInsts();
        InstIterator currInst  = insts.end()-1;
        InstIterator firstInst = insts.begin()-1;
        
        for (; currInst>firstInst; currInst--) {

            Inst *inst = *currInst;
            liveManager.def(inst);                       // update liveSet for currInst
            if (inst->isCall()) {                        // if inst is "call" (only safe point we have)
                RegOpnd *qp  = (RegOpnd *)inst->getOpnd(0);
                QpMask  mask = liveManager.getLiveMask(qp);
                newSafePoint(node, inst, liveSet, mask); // insert (safe point->liveSet) pair in sp2LiveSet
            }                                            //
            liveManager.use(inst);                       // update liveSet for currInst
            defMptr(node, inst);                         // build mptr->base dependency for currInst
        }
    }
    
    if (LOG_ON) {
        IPF_LOG << endl << "  Build mptr to base map" << endl;
        for (MptrDefMapIterator it=mptr2def.begin(); it!=mptr2def.end(); it++) {
            IPF_LOG << "      " << IrPrinter::toString(it->first) << "->";
            IPF_LOG << IrPrinter::toString(it->second.base) << endl;
        }
    }

    IPF_LOG << endl << "  Safe point list" << endl;
    // set mptr->base relations (vector SafePoint.alivePtrs will contain base after each mptr)
    // and extend bases live ranges
    for (uint16 i=0; i<safePoints.size(); i++) {
        SafePoint& sp = safePoints[i];
        insertBases(sp.inst, sp.alivePtrs);  
    }
}

//----------------------------------------------------------------------------------------//
// - Add new record in rootSet map (sp position -> alive mptrs&bases)
// - extend base live ranges
 
void RuntimeSupport::newSafePoint(BbNode *node, Inst *spInst, RegOpndSet &liveSet, QpMask mask) {
    
    safePoints.push_back(SafePoint(mm, node, spInst));  // create record for current safe point
    RegOpndVector &ptrs = safePoints.back().alivePtrs;  // get vector for mptrs and bases alive on the safe point
    
    for (RegOpndSetIterator it=liveSet.begin(); it!=liveSet.end(); it++) {
        RegOpnd *opnd = *it;
        if (opnd->isAlive(mask) == false) continue;
        if (opnd->getDataKind() == DATA_MPTR) {
            ptrs.push_back(opnd);
            ptrs.push_back(NULL);
        }
        if (opnd->getDataKind() == DATA_BASE) {
            ptrs.push_back(opnd);
        }
    }
}

//----------------------------------------------------------------------------------------//
// If inst defs mptr and uses base - add new record in mptr2base map

void RuntimeSupport::defMptr(BbNode *node, Inst *inst) {
    
    OpndVector &opnds  = inst->getOpnds();
    uint16     numDst  = inst->getNumDst();
    uint16     numOpnd = inst->getNumOpnd();
    RegOpnd    *mptr   = NULL;
    RegOpnd    *base   = NULL;

    // check if the inst defines mptr
    for (uint16 i=1; i<=numDst; i++) {  
        if (opnds[i]->getDataKind() == DATA_MPTR) mptr = (RegOpnd *)opnds[i];
    }
    if (mptr == NULL) return;                             // there is no mptr among dst opnds of the inst
    
    // check if the inst uses base
    for (uint16 i=numDst+1; i<numOpnd; i++) {
        if (opnds[i]->getDataKind() == DATA_BASE) base = (RegOpnd *)opnds[i];
    }
    if (base == NULL) return;                             // there is no base among src opnds of the inst

    // add new record or merge with existing one in mptr2def map
    if (mptr2def.count(mptr) > 0) {                       // if there is record for this mptr (mptr has been already defined)
        MptrDef &md = mptr2def[mptr];
        if (md.inst != NULL) {                            // if record base has not been merged - merge it
            RegOpnd *commonBase = opndManager->newRegOpnd(OPND_G_REG, DATA_BASE); // create common base for this mptr
            mergeBase(md.node, md.inst, md.base, commonBase);                     // merge record base with new common base
            md.base = commonBase;                         // record base is common base now
            md.inst = NULL;
            md.node = NULL;
        }
        mergeBase(node, inst, base, md.base);             // merge current base with common base 
    } else {
        mptr2def[mptr] = MptrDef(node, inst, base);       // add new record (new mptr definition found)
    }
}

//----------------------------------------------------------------------------------------//
// From:
//      add mptr       = 5, oldBase
// To:
//      mov commonBase = oldBase
//      add mptr       = 5, commonBase

void RuntimeSupport::mergeBase(BbNode *node, Inst *inst, RegOpnd *oldBase, RegOpnd *commonBase) {

    if (oldBase == commonBase) return;              // nothing to do
    replaceBase(inst, oldBase, commonBase);         // replace old base opnd with new one in the current instruction
    insertMovInst(node, inst, oldBase, commonBase); // insert inst "mov commonBase, oldBase" before inst defining the mptr
}

//----------------------------------------------------------------------------------------//
// - Find base opnd among inst srcs
// - Replace it with commonBase opnd
// - Return old base

void RuntimeSupport::replaceBase(Inst *inst, Opnd *base, Opnd *commonBase) {
    
    OpndVector &opnds  = inst->getOpnds();
    uint16     numDst  = inst->getNumDst();
    uint16     numOpnd = inst->getNumOpnd();
    
    for (uint16 i=numDst+1; i<numOpnd; i++) {
        if (opnds[i] == base) {
            opnds[i] = commonBase;
            return;
        }
    }
    IPF_ERR << endl;
}

//----------------------------------------------------------------------------------------//

void RuntimeSupport::insertMovInst(BbNode *node, Inst *inst, Opnd *oldBase, Opnd *commonBase) {
    
    InstVector   &insts   = node->getInsts();
    Inst         *newInst = new(mm) Inst(mm, INST_MOV, commonBase, oldBase);
    InstIterator pos      = find(insts.begin(), insts.end(), inst);
    
    if (LOG_ON) if (pos == insts.end()) IPF_ERR << endl;
    
    insts.insert(pos, newInst);
}
    
//----------------------------------------------------------------------------------------//
// Find base corresponding to mptr and 
// - insert it in vector of alive on safe point opnds
// - insert it in safe point instruction arg list (extend live range)
 
void RuntimeSupport::insertBases(Inst *inst, RegOpndVector &ptrs) {
    
    IPF_LOG << "    alive pointers:";
    for (uint16 i=0; i<ptrs.size(); i++) {
        IPF_LOG << " " << IrPrinter::toString(ptrs[i]);
        if (ptrs[i]->getDataKind() == DATA_MPTR) {
            RegOpnd *mptr = (RegOpnd *)ptrs[i];
            if (LOG_ON && mptr2def.count(mptr) == 0) IPF_ERR << IrPrinter::toString(mptr) << endl;
            RegOpnd *base = mptr2def[mptr].base;
            if (find(ptrs.begin(), ptrs.end(), base) == ptrs.end()) {
                ptrs.push_back(base);
            }
            ptrs[++i] = base;                    // next to mptr opnd is the mptr's base
            inst->addOpnd(base);                 // extend base live range till the inst
            base->setCrossCallSite(true);        // base must live across call site
            IPF_LOG << "->" << IrPrinter::toString(base);
        }
    }
    IPF_LOG << "  safe point: " << IrPrinter::toString(inst) << endl;
}

//----------------------------------------------------------------------------------------//
// Write GC Root Set info block
//----------------------------------------------------------------------------------------//
// Create info block which is used in GC root set enumeration routine (RuntimeInterface::getGCRootSet)

void RuntimeSupport::makeRootSetInfo(Uint32Vector &info) {

    IPF_LOG << "  Safe points list:" << endl;
    for (uint16 i=0; i<safePoints.size(); i++) {

        BbNode *node   = safePoints[i].node;
        Inst   *inst   = safePoints[i].inst;
        uint64 spAddr = node->getInstAddr(inst);            // get address of safe point instruction 
        spAddr = (spAddr & 0xfffffffffffffff0) + 0x10;      // zero low part and increment
        writeSpInfo(info, spAddr, safePoints[i].alivePtrs);
    }
}

//----------------------------------------------------------------------------------------//

void RuntimeSupport::writeSpInfo(Uint32Vector &info, uint64 spAddr, RegOpndVector &ptrs) {

    U_32 spAddrHight = spAddr;                                // hight part of spAddr
    U_32 spAddrLow   = spAddr >> 32;                          // low   part of spAddr
    uint16 sizePos     = info.size();

    info.push_back(0);                                          // push safe point info size (placeholder)
    info.push_back(spAddrHight);                                // push safe point addr (hight)
    info.push_back(spAddrLow);                                  // push safe point addr (low)

    IPF_LOG << "      address: " << hex << spAddr << dec << "  alive pointers locations:";
    for (uint16 i=0; i<ptrs.size(); i++) {
        I_32 location = toInt32(ptrs[i]);                 // get mptr location
        info.push_back(location);                       // push base location
        IPF_LOG << " " << location;
    }

    info[sizePos] = (info.size() - sizePos) * sizeof(U_32);
    IPF_LOG << "  size: " << info[sizePos] << endl;
}

//----------------------------------------------------------------------------------------//
// returns opnd location in form to store in info block

I_32 RuntimeSupport::toInt32(RegOpnd *opnd) {
    
    I_32 location = opnd->getValue();
    if (LOG_ON && location >= S_OUTARG_BASE) IPF_ERR << " location = " << location << endl;
    if (opnd->isMem() == true)            location += NUM_G_REG;
    if (opnd->getDataKind() == DATA_MPTR) location = - (location + 1);
    return location;
}
    
} // IPF
} // Jitrino
