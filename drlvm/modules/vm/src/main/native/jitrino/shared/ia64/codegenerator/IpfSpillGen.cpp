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

#include "IpfSpillGen.h"
#include "IpfIrPrinter.h"
#include "IpfOpndManager.h"

namespace Jitrino {
namespace IPF {

//========================================================================================//
// SpillGen
//========================================================================================//

SpillGen::SpillGen(Cfg &cfg) : 
    mm(cfg.getMM()), 
    cfg(cfg),
    fillCode(mm),
    spillCode(mm) {

    opndManager = cfg.getOpndManager();
    p0          = opndManager->getP0();
    sp          = opndManager->getR12();
    stackAddr   = opndManager->newRegOpnd(OPND_G_REG, DATA_I64, SPILL_REG1);

    outOffset = 0;
    locOffset = 0;
    psfOffset = 0;
    inOffset  = 0; 
    maxOffset = 0;
}

//----------------------------------------------------------------------------------------//

void SpillGen::genSpillCode() {

    // Iterate through CFG nodes
    NodeVector &nodes = cfg.search(SEARCH_POST_ORDER);
    for(uint16 i=0; i<nodes.size(); i++) {

        if(nodes[i]->getNodeKind() != NODE_BB) continue;          // ignore not BB node
        InstVector &insts = ((BbNode *)nodes[i])->getInsts();     // get node's insts
        for(uint16 j=0; j<insts.size(); j++) {                    // iterate through the insts

            if (LOG_ON && containsStackOpnd(insts[j])==true) {    
                IPF_LOG << endl << "      instruction: " << endl;
                IPF_LOG << "        " << IrPrinter::toString(insts[j]) << endl;
            }

            spillInst(insts[j]);                                  // gen spill and fill code for the inst

            InstIterator it = insts.begin() + j;                  // get iterator on current inst
            insts.insert(it, fillCode.begin(), fillCode.end());   // insert fill code before curr inst
            j += fillCode.size();                                 // increment counter

            it = insts.begin() + j + 1;                           // get iterator on current inst
            insts.insert(it, spillCode.begin(), spillCode.end()); // insert spill code after curr inst
            j += spillCode.size();                                // increment counter
            
            if (LOG_ON) printResult(insts, j);
        }
    }
}
 
//----------------------------------------------------------------------------------------//

void SpillGen::spillInst(Inst *inst) {

    OpndVector &opnds  = inst->getOpnds();                  // get inst opnds
    uint16     numDst  = inst->getNumDst();                 // get num of dst opnds
    uint16     numOpnd = inst->getNumOpnd();                // get num of all opnds

    // Generate fill code. Result in fillCode vector. It goes before current instruction
    fillCode.clear();                                       // clear fill code buffer
    resetSpillRegMasks();                                   // make all spill regs available
    for(uint16 i=numDst+1; i<numOpnd; i++) {
        if(opnds[i]->isMem() == false) continue;            // ignore non stack opnds

        IPF_LOG << "      opnd to fill: " << IrPrinter::toString(opnds[i]) << endl;
        opnds[i] = spillOpnd((RegOpnd *)opnds[i], false);   // gen fill code
    }

    // Generate spill code. Result in fillCode vector. It goes after current instruction
    spillCode.clear();                                      // clear spill code buffer
    resetSpillRegMasks();                                   // make all spill regs available
    for(uint16 i=0; i<numDst+1; i++) {
        if(opnds[i]->isMem() == false) continue;            // ignore non stack opnds

        IPF_LOG << "      opnd to spill: " << IrPrinter::toString(opnds[i]) << endl;
        opnds[i] = spillOpnd((RegOpnd *)opnds[i], true);    // gen spill code
    }
}

//----------------------------------------------------------------------------------------//

RegOpnd *SpillGen::spillOpnd(RegOpnd *stackOpnd, bool spillFlag) {

    // Create scratchOpnd
    OpndKind opndKind     = stackOpnd->getOpndKind();
    DataKind dataKind     = stackOpnd->getDataKind();
    I_32    scratchReg   = getAvailableSpillReg(opndKind);
    RegOpnd  *scratchOpnd = opndManager->newRegOpnd(opndKind, dataKind, scratchReg);
    
    // calculate absolute offset and set it as location on stack opnd
    opndManager->calculateOffset(stackOpnd);

    // Create instruction calculating stack address to store to
    Opnd *offset = opndManager->newImm(stackOpnd->getValue());
    Inst *adds   = new(mm) Inst(mm, INST_ADDS, p0, stackAddr, offset, sp);
    spillFlag ? spillCode.push_back(adds) : fillCode.push_back(adds);

    // Create instruction storing value from scratchOpnd on stack
    switch(scratchOpnd->getOpndKind()) {
        case OPND_G_REG: spillFlag ? spillGr(scratchOpnd) : fillGr(scratchOpnd); break;
        case OPND_F_REG: spillFlag ? spillFr(scratchOpnd) : fillFr(scratchOpnd); break;
        case OPND_B_REG: spillFlag ? spillBr(scratchOpnd) : fillBr(scratchOpnd); break;
        case OPND_P_REG: spillFlag ? spillPr(scratchOpnd) : fillPr(scratchOpnd); break;
        default: IPF_ERR << "invalid OpndKind " << IrPrinter::toString(scratchOpnd) << endl;
    }

    return scratchOpnd;
}    

//----------------------------------------------------------------------------------------//

void SpillGen::resetSpillRegMasks() {
    
    spillGrMask = opndManager->spillGrMask;
    spillFrMask = opndManager->spillFrMask;
    spillPrMask = opndManager->spillPrMask;
    spillBrMask = opndManager->spillBrMask;
}

//----------------------------------------------------------------------------------------//

I_32 SpillGen::getAvailableSpillReg(OpndKind opndKind) {
    
    switch(opndKind) {
        case OPND_G_REG: return getAvailableReg(spillGrMask, NUM_G_REG);
        case OPND_F_REG: return getAvailableReg(spillFrMask, NUM_F_REG);
        case OPND_P_REG: return getAvailableReg(spillPrMask, NUM_P_REG);
        case OPND_B_REG: return getAvailableReg(spillBrMask, NUM_B_REG);
        default        : return LOCATION_INVALID;
    }
}
    
//----------------------------------------------------------------------------------------//

I_32 SpillGen::getAvailableReg(RegBitSet &regMask, int16 maskSize) {
    
    for(int16 i=0; i<maskSize; i++) if(regMask[i] == 1) { regMask[i]=0; return i; }
    IPF_ERR << " No available spill reg" << endl;
    return LOCATION_INVALID;
    
}

//----------------------------------------------------------------------------------------//
//      add  stackOpnd = 1, r34
//
//      add  scratchOpnd = 1, r34
//      adds stackAddr   = offset, r12
//      st   [stackAddr] = scratchOpnd

void SpillGen::spillGr(RegOpnd *scratchOpnd) {
    
    Completer completer = CMPLT_INVALID;
    switch(scratchOpnd->getSize()) {
        case 1 : completer = CMPLT_SZ_1;   break;
        case 2 : completer = CMPLT_SZ_2;   break;
        case 4 : completer = CMPLT_SZ_4;   break;
        case 8 : completer = CMPLT_SZ_8;   break;
        default        : IPF_ERR << "invalid size " << IrPrinter::toString(scratchOpnd) << endl;
    }

    Inst *st = new(mm) Inst(mm, INST_ST, completer, p0, stackAddr, scratchOpnd);
    spillCode.push_back(st);
}

//----------------------------------------------------------------------------------------//
//      fadd stackOpnd = f30, f31
//
//      fadd scratchOpnd = r30, f31
//      adds stackAddr   = offset, r12
//      stf  [stackAddr] = scratchOpnd

void SpillGen::spillFr(RegOpnd *scratchOpnd) {
    
    Completer completer = CMPLT_INVALID;
    switch(scratchOpnd->getDataKind()) {
        case DATA_S   : completer = CMPLT_FSZ_S; break;
        case DATA_D   : completer = CMPLT_FSZ_D; break;
        case DATA_F   : completer = CMPLT_FSZ_E; break;
        default: IPF_ERR << "invalid DataKind " << IrPrinter::toString(scratchOpnd) << endl;
    }

    Inst *st = new(mm) Inst(mm, INST_STF, completer, p0, stackAddr, scratchOpnd);
    spillCode.push_back(st);
}

//----------------------------------------------------------------------------------------//
//      mov  stackOpnd = r34
//
//      mov  scratchOpnd = r34
//      adds stackAddr   = offset, r12
//      mov  bufOpnd     = scratchOpnd
//      st   [stackAddr] = bufOpnd

void SpillGen::spillBr(RegOpnd *scratchOpnd) {
    
    I_32 bufReg   = getAvailableSpillReg(OPND_G_REG);
    Opnd  *bufOpnd = opndManager->newRegOpnd(OPND_G_REG, DATA_B, bufReg);
    Inst  *mov     = new(mm) Inst(mm, INST_MOV, p0, bufOpnd, scratchOpnd);
    Inst  *st      = new(mm) Inst(mm, INST_ST, CMPLT_SZ_8, p0, stackAddr, bufOpnd);

    spillCode.push_back(mov);
    spillCode.push_back(st);
}

//----------------------------------------------------------------------------------------//
//               cmp  stackOpnd, p0 = r0, r0
//
//               cmp  scratchOpnd, p0 = r0, r0
//               adds stackAddr   = offset, r12
//               mov  bufOpnd     = r0
// (scratchOpnd) mov  bufOpnd     = 1
//               st   [stackAddr] = bufOpnd

void SpillGen::spillPr(RegOpnd *scratchOpnd) {
    
    I_32 bufReg  = getAvailableSpillReg(OPND_G_REG);
    Opnd *bufOpnd = opndManager->newRegOpnd(OPND_G_REG, DATA_P, bufReg);
    Opnd *imm1    = opndManager->newImm(1);
    Inst *mov1    = new(mm) Inst(mm, INST_MOV, p0, bufOpnd, opndManager->getR0());
    Inst *mov2    = new(mm) Inst(mm, INST_MOV, scratchOpnd, bufOpnd, imm1);
    Inst *st      = new(mm) Inst(mm, INST_ST, CMPLT_SZ_1, p0, stackAddr, bufOpnd);

    spillCode.push_back(mov1);
    spillCode.push_back(mov2);
    spillCode.push_back(st);
}

//----------------------------------------------------------------------------------------//
//      add  r34 = stackOpnd, 1
//
//      adds stackAddr   = offset, r12
//      ld   scratchOpnd = [stackAddr]
//      add  r34 = scratchOpnd, 1

void SpillGen::fillGr(RegOpnd *scratchOpnd) {
    
    Completer completer = CMPLT_INVALID;
    switch(scratchOpnd->getSize()) {
        case 1 : completer = CMPLT_SZ_1;   break;
        case 2 : completer = CMPLT_SZ_2;   break;
        case 4 : completer = CMPLT_SZ_4;   break;
        case 8 : completer = CMPLT_SZ_8;   break;
        default        : IPF_ERR << "invalid size " << IrPrinter::toString(scratchOpnd) << endl;
    }

    Inst *ld = new(mm) Inst(mm, INST_LD, completer, p0, scratchOpnd, stackAddr);
    fillCode.push_back(ld);

    // Create sxt instruction for I_32 data type
    if(scratchOpnd->getDataKind() == DATA_I32) {
        Inst *sxt = new(mm) Inst(mm, INST_SXT, CMPLT_XSZ_4, p0, scratchOpnd, scratchOpnd);
        fillCode.push_back(sxt);
    }
}

//----------------------------------------------------------------------------------------//
//      fadd f30         = stackOpnd, f0
//
//      adds stackAddr   = offset, r12
//      ldf  scratchOpnd = [stackAddr]
//      add  f30         = scratchOpnd, f0

void SpillGen::fillFr(RegOpnd *scratchOpnd) {
    
    Completer completer = CMPLT_INVALID;
    switch(scratchOpnd->getDataKind()) {
        case DATA_S   : completer = CMPLT_FSZ_S; break;
        case DATA_D   : completer = CMPLT_FSZ_D; break;
        case DATA_F   : completer = CMPLT_FSZ_E; break;
        default: IPF_ERR << "invalid DataKind " << IrPrinter::toString(scratchOpnd) << endl;
    }

    Inst *ld = new(mm) Inst(mm, INST_LDF, completer, p0, scratchOpnd, stackAddr);
    fillCode.push_back(ld);
}

//----------------------------------------------------------------------------------------//
//      br   stackOpnd
//
//      adds stackAddr   = offset, r12
//      ld   bufOpnd     = [stackAddr]
//      mov  scratchOpnd = bufOpnd
//      br   scratchOpnd

void SpillGen::fillBr(RegOpnd *scratchOpnd) {
    
    I_32 bufReg  = getAvailableSpillReg(OPND_G_REG);
    Opnd *bufOpnd = opndManager->newRegOpnd(OPND_G_REG, DATA_I64, bufReg);
    Inst *ld      = new(mm) Inst(mm, INST_LD, CMPLT_SZ_8, p0, bufOpnd, stackAddr);
    Inst *mov     = new(mm) Inst(mm, INST_MOV, p0, scratchOpnd, bufOpnd);

    fillCode.push_back(ld);
    fillCode.push_back(mov);
}

//----------------------------------------------------------------------------------------//
// (stackOpnd)   mov r34 = r0
//
//               adds stackAddr = offset, r12
//               ld   bufOpnd   = [stackAddr]
//               cmp.ne scratchOpnd, p0 = bufOpnd, r0
// (scratchOpnd) mov r34 = r0

void SpillGen::fillPr(RegOpnd *scratchOpnd) {
    
    I_32 bufReg   = getAvailableSpillReg(OPND_G_REG);
    Opnd  *bufOpnd = opndManager->newRegOpnd(OPND_G_REG, DATA_P, bufReg);
    Inst  *ld      = new(mm) Inst(mm, INST_LD, CMPLT_SZ_1, p0, bufOpnd, stackAddr);
    Inst  *cmp     = new(mm) Inst(mm, INST_CMP, CMPLT_CMP_CREL_NE, p0, scratchOpnd, p0, bufOpnd);
    cmp->addOpnd(opndManager->getR0());

    fillCode.push_back(ld);
    fillCode.push_back(cmp);
}

//----------------------------------------------------------------------------------------//

bool SpillGen::containsStackOpnd(Inst *inst) {
    
    OpndVector &opnds  = inst->getOpnds();
    uint16     numOpnd = inst->getNumOpnd();

    for(uint16 i=0; i<numOpnd; i++) {
        if(opnds[i]->isMem() == false) continue;  // ignore non stack opnds
        return true;
    }
    return false;
}

//----------------------------------------------------------------------------------------//
// print spill/fill code for current inst

void SpillGen::printResult(InstVector &insts, uint16 last) {

    if (fillCode.size()+spillCode.size() == 0) return;
    uint16 first = last - fillCode.size() - spillCode.size();

    for (uint16 i=first; i<=last; i++) {
        IPF_LOG << "        " << IrPrinter::toString(insts[i]) << endl;
    }
}

} // IPF
} // Jitrino
