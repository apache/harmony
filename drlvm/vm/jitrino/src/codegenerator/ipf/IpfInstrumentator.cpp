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

#include "IpfInstrumentator.h"
#include "IpfIrPrinter.h"
#include "VMInterface.h"
#include <fstream>

namespace Jitrino {
namespace IPF {

//========================================================================================//
// IrPrinter
//========================================================================================//

Instrumentator::Instrumentator(Cfg &cfg) : 
    mm(cfg.getMM()), 
    opndManager(cfg.getOpndManager()), 
    cfg(cfg) {
}

//----------------------------------------------------------------------------------------//

void Instrumentator::instrument() {

    instrumentStart();
    instrumentEnd();
    opndManager->setContainCall(true);
}

//----------------------------------------------------------------------------------------//

void Instrumentator::instrumentStart() {

    InstVector instCode(mm);
    uint64 funcDescPtr = (uint64) Instrumentator::methodStart;   // get function descriptor address

    genNativeCall(funcDescPtr, instCode);

    BbNode     *enterNode  = cfg.getEnterNode();
    InstVector &insts      = enterNode->getInsts();
    insts.insert(insts.end(), instCode.begin(), instCode.end());
    IPF_LOG << endl << "  Method start:" << endl << IrPrinter::toString(instCode);
}

//----------------------------------------------------------------------------------------//

void Instrumentator::instrumentEnd() {

    InstVector instCode(mm);
    uint64 funcDescPtr = (uint64) Instrumentator::methodEnd;        // get function descriptor address
    genNativeCall(funcDescPtr, instCode);                           // gen call of "methodEnd"

    NodeVector &nodes = cfg.search(SEARCH_POST_ORDER);              // get nodes
    for(uint16 i=0; i<nodes.size(); i++) {                          // iterate through CFG nodes

        if (nodes[i]->getNodeKind() != NODE_BB)          continue;  // ignore non BB node

        BbNode *node = (BbNode *)nodes[i];
        InstVector &insts = node->getInsts();                       // get node's insts
        if (insts.size() == 0)                           continue;  //
        CompVector &comps = insts.back()->getComps();               // get last inst completers
        if (comps.size()<1 || comps[0]!=CMPLT_BTYPE_RET) continue;  // if the inst is not "ret" - ignore

//        MethodDesc *methodDesc = cfg.getMethodDesc();
//        Type *type = methodDesc->getMethodSig()->getReturnType();
//        
//        if (type->isVoid()) {
            insts.insert(insts.end()-1, instCode.begin(), instCode.end());
//        } else {
//            insts.insert(insts.end()-2, instCode.begin(), instCode.end());
//        }
    }
        
    IPF_LOG << endl << "  Method end:" << endl << IrPrinter::toString(instCode);
}

//----------------------------------------------------------------------------------------//

void Instrumentator::genNativeCall(uint64 funcDescPtr, InstVector &insts) {

/*
    DrlVMMethodDesc *methodDesc  = (DrlVMMethodDesc *) cfg.getMethodDesc();
    Method_Handle   methodHandle = methodDesc->getDrlVMMethod();

    Opnd *methodDescAddr = opndManager->newImm((uint64) methodHandle);
    Opnd *p0             = opndManager->getP0();
    Opnd *r0             = opndManager->getR0();
    Opnd *b0             = opndManager->getB0();
    Opnd *r1             = opndManager->newRegOpnd(OPND_G_REG, DATA_U64, 1);
    Opnd *funDescAddr    = opndManager->newImm(funcDescPtr);
    Opnd *globalPtrAddr  = opndManager->newImm(funcDescPtr + 8);
    Opnd *entryPointer   = opndManager->newRegOpnd(OPND_G_REG, DATA_U64);
    Opnd *globalPtrBak   = opndManager->newRegOpnd(OPND_G_REG, DATA_U64);
    Opnd *globalPtr      = opndManager->newRegOpnd(OPND_G_REG, DATA_U64);
    Opnd *callTgt        = opndManager->newRegOpnd(OPND_B_REG, DATA_U64);
    Opnd *callArg        = opndManager->newRegOpnd(OPND_G_REG, DATA_U64, opndManager->newOutReg(0));

    // load entry pointer
    insts.push_back(new(mm) Inst(mm, INST_MOVL, p0, entryPointer, funDescAddr));
    insts.push_back(new(mm) Inst(mm, INST_LD, CMPLT_SZ_8, p0, entryPointer, entryPointer));

    // save global pointer
    insts.push_back(new(mm) Inst(mm, INST_MOV, p0, globalPtrBak, r1));
    
    // load new global pointer
    insts.push_back(new(mm) Inst(mm, INST_MOVL, p0, globalPtr, globalPtrAddr));
    insts.push_back(new(mm) Inst(mm, INST_LD, CMPLT_SZ_8, p0, r1, globalPtr));

    // make call
    insts.push_back(new(mm) Inst(mm, INST_MOVL, p0, callArg, methodDescAddr));
    insts.push_back(new(mm) Inst(mm, INST_MOV, p0, callTgt, entryPointer));
    insts.push_back(new(mm) Inst(mm, INST_BR13, CMPLT_BTYPE_CALL, p0, r0, b0, callTgt, callArg, r1));

    // restore global pointer
    insts.push_back(new(mm) Inst(mm, INST_MOV, p0, r1, globalPtrBak));
    insts.push_back(new(mm) Inst(mm, INST_USE, p0, r1));
    */
}

//----------------------------------------------------------------------------------------//

void Instrumentator::methodStart(Method_Handle methodHandle) {
    ofstream rtLog;
    rtLog.open("rt.log", ios::out | ios::app);

    rtLog << "Start " << methodHandle << endl;
    rtLog.close();
}

//----------------------------------------------------------------------------------------//

void Instrumentator::methodEnd(Method_Handle methodHandle) {
    ofstream rtLog;
    rtLog.open("rt.log", ios::out | ios::app);
    
    rtLog << "End   " << methodHandle << endl;
}

} // IPF
} // Jitrino
