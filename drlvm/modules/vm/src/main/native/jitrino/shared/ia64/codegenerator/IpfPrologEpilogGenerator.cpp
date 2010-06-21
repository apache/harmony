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

#include "IpfPrologEpilogGenerator.h"
#include "IpfIrPrinter.h"

namespace Jitrino {
namespace IPF {

//========================================================================================//
// PrologEpilogGenerator
//========================================================================================//

PrologEpilogGenerator::PrologEpilogGenerator(Cfg &cfg) :
    mm(cfg.getMM()),
    cfg(cfg), 
    outRegArgs(mm),
    prologInsts(mm),
    epilogInsts(mm),
    allocInsts(mm),
    saveSpInsts(mm),
    savePfsInsts(mm),
    saveUnatInsts(mm),
    saveGrsInsts(mm),
    saveFrsInsts(mm),
    saveBrsInsts(mm),
    savePrsInsts(mm),
    saveRpInsts(mm),
    restRpInsts(mm),
    restPrsInsts(mm),
    restBrsInsts(mm),
    restFrsInsts(mm),
    restGrsInsts(mm),
    restUnatInsts(mm),
    restPfsInsts(mm),
    restSpInsts(mm),
    epilogNodes(mm) {
    
    opndManager = cfg.getOpndManager();
    p0          = opndManager->getP0();
    sp          = opndManager->getR12();
    stackAddr   = opndManager->newRegOpnd(OPND_G_REG, DATA_I64, SPILL_REG1);
}

//----------------------------------------------------------------------------------------//
// Generate prolog and epilog
//   save/restore callee saved regs
//     build mask of regs not used in method
//   generate "alloc"
//     calculate number of local grs
//   save/restore PFS, Stack pointer and Return pointer
//     calculate stack size
//   reassign out reg args. Now they are located inverse order in the end of reg 
//   stack (first in 127, second in 126 ...)

void PrologEpilogGenerator::genPrologEpilog() {
    
    buildSets();

    IPF_LOG << endl << "  Generate Code" << endl;
    genCode();

    IPF_LOG << "  Reassign OutRegArgs" << endl;
    reassignOutRegArgs();

    IPF_LOG << endl; opndManager->printStackInfo(); 
    IPF_LOG << endl; printRegMasks(); IPF_LOG << endl; 
}

//----------------------------------------------------------------------------------------//
// Build free reg masks and outArgs vector

void PrologEpilogGenerator::buildSets() {

    // Build set of RegOpnd used in method. Buld epilog nodes vector
    NodeVector &nodes = cfg.search(SEARCH_POST_ORDER);          // get nodes
    for(uint16 i=0; i<nodes.size(); i++) {                      // iterate through CFG nodes

        if(nodes[i]->getNodeKind() != NODE_BB) continue;        // ignore non BB node
        
        InstVector &insts = ((BbNode *)nodes[i])->getInsts();   // get insts
        if(insts.size() == 0) continue;                         // if there are no insts in node - ignore


        // check if node is epilog and insert it in epilogNodes list
        CompVector &comps = insts.back()->getComps();           // get last inst completers
        if(comps.size()>0 && comps[0]==CMPLT_BTYPE_RET) {       // if the inst is br.ret - node is epilog
            epilogNodes.push_back(nodes[i]);                    // put it in epilogNodes list
        }

        // build mask of used registers and vector of outArgs 
        for(uint16 j=0; j<insts.size(); j++) {                  // iterate through instructions
            OpndVector &opnds = insts[j]->getOpnds();           // get opnds
            for(uint16 k=0; k<insts[j]->getNumOpnd(); k++) {    // iterate through opnds
                if(opnds[k]->isReg() == false) continue;        // ignore non register opnd
                RegOpnd *opnd = (RegOpnd *)opnds[k];

                setRegUsage(opnd, true);                        // mark reg as used
                if (opndManager->isOutReg(opnd)) {              // if opnd is out arg
                    outRegArgs.insert(opnd);                    // place it in ouRegArgs vector
                }
            }
        }
    }
}
    
//----------------------------------------------------------------------------------------//
// At this point out reg args have temporary locations. First one assigned to gr127, second to 126 ...
// Now we know number of in args and locals. Thus we can assign real locations for out args.

void PrologEpilogGenerator::reassignOutRegArgs() {
    
    // First out arg will have this location 
    I_32 outArgBase = G_INARG_BASE + opndManager->getLocRegSize();
    
    for(RegOpndSetIterator it=outRegArgs.begin(); it!=outRegArgs.end(); it++) {  
        setRegUsage(*it, false);                                     // mark old reg as free
    }

    for(RegOpndSetIterator it=outRegArgs.begin(); it!=outRegArgs.end(); it++) {  

        RegOpnd *arg = *it;
        IPF_LOG << "      " << IrPrinter::toString(arg) << " reassigned on ";
        I_32 outArgNum = G_OUTARG_BASE - arg->getValue();           // calculate real out arg number
        arg->setLocation(outArgBase + outArgNum);                    // calculate and assign new location
        setRegUsage(arg, true);                                      // mark new reg as used
        IPF_LOG << IrPrinter::toString(arg) << endl;  
    }
}

//----------------------------------------------------------------------------------------//
// Generate prolog and epilog code and place it in Enter and Epilog nodes
// Prolog:
//   alloc     pfsBak      = 1, 93, 2, 0      # gen alloc (pfsBak can not be preserved gr)
//   adds      r12         = -stackSize, r12  # save SP
//   adds      stackAddr   = offset, r12      # if pfsBak is stack opnd - spill pfs
//   st8       [stackAddr] = pfsBak           #
//   mov       scratch     = unat             # if we use preserved grs - spill unat
//   adds      stackAddr   = offset, r12      #
//   st8       [stackAddr] = scratch          #
//   adds      stackAddr   = offset, r12      # spill preserved grs
//   st8.spill [stackAddr] = preservedGr      #
//   adds      stackAddr   = offset, r12      # spill preserved frs
//   stf.spill [stackAddr] = preservedFr      #
//   mov       brBak       = preservedBr      # save preserved brs
//   mov       prBak       = pr               # save preserved prs
//   mov       rpBak       = b0               # save return poiner
//
// Epilog:
//   mov       b0          = prBak            # restore return poiner
//   mov       pr          = prBak            # restore preserved prs
//   mov       preservedBr = brBak            # restore preserved brs
//   adds      stackAddr   = offset, r12      # fill preserved frs
//   ldf.fill  preservedFr = [stackAddr]      #
//   adds      stackAddr   = offset1, r12     # fill preserved grs
//   ld8.fill  preservedGr = [stackAddr]      #
//   adds      stackAddr   = offset, r12      # if we use preserved grs - fill unat
//   ld8       scratch     = [stackAddr]      #
//   mov       unat        = scratch          #
//   mov.i     AR.PFS      = pfsBak           # restore AR.PFS (if pfsBak is stack opnd - fill pfs)
//   adds      r12         = stackSize, r12   # restore SP

void PrologEpilogGenerator::genCode() {

    containCall = opndManager->getContainCall();

    saveRestoreRp();              // affects: mem stack, reg stack  use: 
    saveRestorePr();              // affects: mem stack, reg stack  use: 
    genAlloc();                   // affects: mem stack, reg stack  use: reg stack
    saveRestoreUnat();            // affects: mem stack             use: 
    saveRestorePreservedGr();     // affects: mem stack             use: 
    saveRestorePreservedFr();     // affects: mem stack             use:
    saveRestorePreservedBr();     // affects: mem stack             use: 
    saveRestoreSp();              // affects:                       use: mem stack

    prologInsts.splice(prologInsts.end(), allocInsts);
    prologInsts.splice(prologInsts.end(), saveSpInsts);
    prologInsts.splice(prologInsts.end(), savePfsInsts);
    prologInsts.splice(prologInsts.end(), saveUnatInsts);
    prologInsts.splice(prologInsts.end(), saveGrsInsts);
    prologInsts.splice(prologInsts.end(), saveFrsInsts);
    prologInsts.splice(prologInsts.end(), saveBrsInsts);
    prologInsts.splice(prologInsts.end(), savePrsInsts);
    prologInsts.splice(prologInsts.end(), saveRpInsts);

    epilogInsts.splice(epilogInsts.end(), restRpInsts);
    epilogInsts.splice(epilogInsts.end(), restPrsInsts);
    epilogInsts.splice(epilogInsts.end(), restBrsInsts);
    epilogInsts.splice(epilogInsts.end(), restFrsInsts);
    epilogInsts.splice(epilogInsts.end(), restGrsInsts);
    epilogInsts.splice(epilogInsts.end(), restUnatInsts);
    epilogInsts.splice(epilogInsts.end(), restPfsInsts);
    epilogInsts.splice(epilogInsts.end(), restSpInsts);

    // Print Prolog and Epilog insts
    IPF_LOG << "    Prolog:" << endl << IrPrinter::toString(prologInsts);
    IPF_LOG << "    Epilog:" << endl << IrPrinter::toString(epilogInsts) << endl;

    // Insert prolog instructions in begining of enter node
    BbNode     *enterNode  = (BbNode *)cfg.getEnterNode();
    InstVector &enterInsts = enterNode->getInsts();
    enterInsts.insert(enterInsts.begin(), prologInsts.begin(), prologInsts.end());

    // Insert epilog instructions in each epilog node (all nodes which end with "br.ret")
    for(uint16 i=0; i<epilogNodes.size(); i++) {
        InstVector &exitInsts = ((BbNode *)epilogNodes[i])->getInsts();
        exitInsts.insert(exitInsts.end()-1, epilogInsts.begin(), epilogInsts.end());
    }
}

//----------------------------------------------------------------------------------------//
// Generate saving/restoring return pointer
//   mov   rpBak = b0

void PrologEpilogGenerator::saveRestoreRp() {

    if(containCall == false) return;               // method does not contain "call" - nothing to do
    
    RegOpnd *b0    = opndManager->getB0();         // return pointer to be saved
    RegOpnd *rpBak = newStorage(DATA_B, SITE_REG); // opnd to store return pointer
    opndManager->rpBak = rpBak->getLocation();     // set rpBak as storage of return pointer

    if (rpBak->isMem()) {
        Opnd  *offset   = opndManager->newImm(rpBak->getValue());
        Opnd  *scratch  = opndManager->newRegOpnd(OPND_G_REG, DATA_B, SPILL_REG2);

        saveRpInsts.push_back(new(mm) Inst(mm, INST_MOV, p0, scratch, b0));
        saveRpInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, stackAddr, offset, sp));
        saveRpInsts.push_back(new(mm) Inst(mm, INST_ST, CMPLT_SZ_8, p0, stackAddr, scratch));
    
        restRpInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, stackAddr, offset, sp));
        restRpInsts.push_back(new(mm) Inst(mm, INST_LD, CMPLT_SZ_8, p0, scratch, stackAddr));
        restRpInsts.push_back(new(mm) Inst(mm, INST_MOV, p0, b0, scratch));
    } else {
        saveRpInsts.push_back(new(mm) Inst(mm, INST_MOV, p0, rpBak, b0));
        restRpInsts.push_back(new(mm) Inst(mm, INST_MOV, p0, b0, rpBak));
    }
}

//----------------------------------------------------------------------------------------//
// If we use preserved prs - generate saving/restoring predicate registers

void PrologEpilogGenerator::saveRestorePr() {

    RegBitSet regMask = usedPrMask & opndManager->preservPrMask;
    if(regMask.any() == false) return;               // method does not use preserved pr

    RegOpnd *prBak = newStorage(DATA_U64, SITE_REG); // opnd to store prs
    opndManager->prBak = prBak->getLocation();       // set prBak as storage of prs

    if (prBak->isMem()) {
        Opnd  *offset   = opndManager->newImm(prBak->getValue());
        Opnd  *scratch  = opndManager->newRegOpnd(OPND_G_REG, DATA_B, SPILL_REG2);

        savePrsInsts.push_back(new(mm) Inst(mm, INST_MOV, p0, scratch, p0));
        savePrsInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, stackAddr, offset, sp));
        savePrsInsts.push_back(new(mm) Inst(mm, INST_ST, CMPLT_SZ_8, p0, stackAddr, scratch));
    
        restPrsInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, stackAddr, offset, sp));
        restPrsInsts.push_back(new(mm) Inst(mm, INST_LD, CMPLT_SZ_8, p0, scratch, stackAddr));
        restPrsInsts.push_back(new(mm) Inst(mm, INST_MOV, p0, p0, scratch));
    } else {
        savePrsInsts.push_back(new(mm) Inst(mm, INST_MOV, p0, prBak, p0));
        restPrsInsts.push_back(new(mm) Inst(mm, INST_MOV, p0, p0, prBak));
    }
}

//----------------------------------------------------------------------------------------//
// Generate "alloc" and AR.PFS restoring instructions

void PrologEpilogGenerator::genAlloc() {

    I_32 locRegSize = calculateLocRegSize();                  // actual reg usage in local area
    I_32 inRegSize  = opndManager->getInRegSize();            // in regs number
    I_32 outRegSize = opndManager->getOutRegSize();           // out regs number

    if (containCall==false && locRegSize<=inRegSize) return;   // method does not need "alloc" inst

    Opnd *pfsBak    = saveRestorePfs();                        // opnd to store AR.PFS
         locRegSize = calculateLocRegSize();                   // saveRestorePfs can allocate local gr
    Opnd *iSize     = opndManager->newImm(0);                  // always 0
    Opnd *lSize     = opndManager->newImm(locRegSize);         // number of in+local  gr
    Opnd *oSize     = opndManager->newImm(outRegSize);         // number of output gr
    Opnd *rSize     = opndManager->newImm(0);                  // number of rotate gr

    allocInsts.push_back(new(mm) Inst(mm, INST_ALLOC, p0, pfsBak, iSize, lSize, oSize, rSize));
}

//----------------------------------------------------------------------------------------//
// Generate instructions for saving/restoring AR.PFS

Opnd* PrologEpilogGenerator::saveRestorePfs() {

    if (containCall == false) {                  // method does not contain "call" - do not save AR.PFS
        return opndManager->newRegOpnd(OPND_G_REG, DATA_U64, SPILL_REG2);
    }
    
    RegBitSet regMask = usedGrMask;             
    for(uint16 i=4; i<8; i++) regMask[i] = 1;    // do not use preserved gr for saving AR.PFS

    Opnd    *pfs    = opndManager->newRegOpnd(OPND_A_REG, DATA_U64, AR_PFS_NUM);
    RegOpnd *pfsBak = newStorage(DATA_U64, SITE_REG);
    opndManager->pfsBak = pfsBak->getLocation(); // set the location as storage of AR.PFS

    if (pfsBak->isMem()) {
        Opnd  *offset  = opndManager->newImm(pfsBak->getValue());
        Opnd  *scratch = opndManager->newRegOpnd(OPND_G_REG, DATA_B, SPILL_REG2);

        savePfsInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, stackAddr, offset, sp));
        savePfsInsts.push_back(new(mm) Inst(mm, INST_ST, CMPLT_SZ_8, p0, stackAddr, scratch));
    
        restPfsInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, stackAddr, offset, sp));
        restPfsInsts.push_back(new(mm) Inst(mm, INST_LD, CMPLT_SZ_8, p0, scratch, stackAddr));
        restPfsInsts.push_back(new(mm) Inst(mm, INST_MOV_I, p0, pfs, scratch));
        
        return scratch;
    } else {
        restPfsInsts.push_back(new(mm) Inst(mm, INST_MOV_I, p0, pfs, pfsBak));
        return pfsBak;
    }
}

//----------------------------------------------------------------------------------------//
// Generate instructions for saving/restoring AR.UNAT

void PrologEpilogGenerator::saveRestoreUnat() {

    RegBitSet preservGrMask(string("11110000"));   // work with r4-r7 only (not with automatic regs r32-r127)
    RegBitSet regMask = usedGrMask & preservGrMask; 
    if(regMask.any() == false) return;

    RegOpnd *storage = newStorage(DATA_U64, SITE_STACK);
    Opnd    *offset  = opndManager->newImm(storage->getValue());
    Opnd    *unat    = opndManager->newRegOpnd(OPND_A_REG, DATA_U64, AR_UNAT_NUM);
    Opnd    *scratch = opndManager->newRegOpnd(OPND_G_REG, DATA_U64, SPILL_REG2);

    opndManager->unatBak = storage->getLocation();

    saveUnatInsts.push_back(new(mm) Inst(mm, INST_MOV_M, p0, scratch, unat));
    saveUnatInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, stackAddr, offset, sp));
    saveUnatInsts.push_back(new(mm) Inst(mm, INST_ST, CMPLT_SZ_8, p0, stackAddr, scratch));

    restUnatInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, stackAddr, offset, sp));
    restUnatInsts.push_back(new(mm) Inst(mm, INST_LD, CMPLT_SZ_8, p0, scratch, stackAddr));
    restUnatInsts.push_back(new(mm) Inst(mm, INST_MOV_M, p0, unat, scratch));
}

//----------------------------------------------------------------------------------------//
// Generate instructions for saving/restoring preserved grs
//   adds      r14   = offset, r12
//   st8.spill [r14] = preserved gr

void PrologEpilogGenerator::saveRestorePreservedGr() {

    opndManager->initSavedBase();                // after this point mem local stack must contain preserved regs only
    IPF_LOG << "    Preserved register saved in memory stack with offset: " << opndManager->savedBase << endl;

    RegBitSet preservGrMask(string("11110000")); // work with r4-r7 only (not with automatic regs r32-r127)
    RegBitSet regMask = usedGrMask & preservGrMask;
    if(regMask.any() == false) return;

    IPF_LOG << "    Preserved grs:";
    for(uint16 i=4; i<8; i++) {
        if(regMask[i] == false) continue;

        Opnd *storage = newStorage(DATA_U64, SITE_STACK);
        Opnd *offset  = opndManager->newImm(storage->getValue());
        Opnd *preserv = opndManager->newRegOpnd(OPND_G_REG, DATA_U64, i);

        saveGrsInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, stackAddr, offset, sp));
        saveGrsInsts.push_back(new(mm) Inst(mm, INST_ST8_SPILL, p0, stackAddr, preserv));
        restGrsInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, stackAddr, offset, sp));
        restGrsInsts.push_back(new(mm) Inst(mm, INST_LD8_FILL, p0, preserv, stackAddr));

        IPF_LOG << " " << IrPrinter::toString(preserv);
    }
    IPF_LOG << endl;
    opndManager->savedGrMask = regMask.to_ulong();
}

//----------------------------------------------------------------------------------------//
// Generate instructions for saving/restoring preserved frs
//   adds      r14   = offset, r12
//   stf.spill [r14] = preserved fr

void PrologEpilogGenerator::saveRestorePreservedFr() {

    RegBitSet regMask = usedFrMask & opndManager->preservFrMask; 
    if(regMask.any() == false) return;

    IPF_LOG << "    Preserved frs:";
    for(uint16 i=2; i<32; i++) {
        if(regMask[i] == false) continue;

        Opnd *storage = newStorage(DATA_F, SITE_STACK);
        Opnd *offset  = opndManager->newImm(storage->getValue());
        Opnd *preserv = opndManager->newRegOpnd(OPND_F_REG, DATA_F, i);
        
        saveFrsInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, stackAddr, offset, sp));
        saveFrsInsts.push_back(new(mm) Inst(mm, INST_STF_SPILL, p0, stackAddr, preserv));
        restFrsInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, stackAddr, offset, sp));
        restFrsInsts.push_back(new(mm) Inst(mm, INST_LDF_FILL, p0, preserv, stackAddr));

        IPF_LOG << " " << IrPrinter::toString(preserv);
    }
    IPF_LOG << endl;
    opndManager->savedFrMask = regMask.to_ulong();
}

//----------------------------------------------------------------------------------------//
// Generate instructions for saving/restoring preserved brs

void PrologEpilogGenerator::saveRestorePreservedBr() {

    RegBitSet regMask = usedBrMask & opndManager->preservBrMask;
    if(regMask.any() == false) return;

    IPF_LOG << "    Preserved brs:";
    for(uint16 i=1; i<6; i++) {
        if(regMask[i] == false) continue;

        Opnd *storage  = newStorage(DATA_B, SITE_STACK);
        Opnd *offset   = opndManager->newImm(storage->getValue());
        Opnd *scratch  = opndManager->newRegOpnd(OPND_G_REG, DATA_B, SPILL_REG2);
        Opnd *preserv  = opndManager->newRegOpnd(OPND_B_REG, DATA_B, i);
        
        saveBrsInsts.push_back(new(mm) Inst(mm, INST_MOV, p0, scratch, preserv));
        saveBrsInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, stackAddr, offset, sp));
        saveBrsInsts.push_back(new(mm) Inst(mm, INST_ST, CMPLT_SZ_8, p0, stackAddr, scratch));

        restBrsInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, stackAddr, offset, sp));
        restBrsInsts.push_back(new(mm) Inst(mm, INST_LD, CMPLT_SZ_8, p0, scratch, stackAddr));
        restBrsInsts.push_back(new(mm) Inst(mm, INST_MOV, p0, preserv, scratch));
        IPF_LOG << " " << IrPrinter::toString(preserv);
    }
    IPF_LOG << endl;
    opndManager->savedBrMask = regMask.to_ulong();
}

//----------------------------------------------------------------------------------------//
// Generate instructions for saving/restoring stack pointer. Instructions are inserted after "alloc"

void PrologEpilogGenerator::saveRestoreSp() {
    
    opndManager->initMemStackSize();
    I_32 memStackSize = opndManager->memStackSize;
    if (memStackSize <= S_SCRATCH_SIZE) return;    // method does not need to save SP
    
    Opnd *memStackSizeOpndNeg = opndManager->newImm(-memStackSize);
    Opnd *memStackSizeOpndPos = opndManager->newImm(memStackSize);
    saveSpInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, sp, memStackSizeOpndNeg, sp));
    restSpInsts.push_back(new(mm) Inst(mm, INST_ADDS, p0, sp, memStackSizeOpndPos, sp));
}
    
//----------------------------------------------------------------------------------------//

RegOpnd* PrologEpilogGenerator::newStorage(DataKind dataKind, uint16 site) {
    
    I_32 location = LOCATION_INVALID;
    if (site==SITE_REG) location = opndManager->newLocation(OPND_G_REG, dataKind, usedGrMask, containCall);
    else                location = opndManager->newLocSlot(dataKind);
    
    RegOpnd *storage = opndManager->newRegOpnd(OPND_G_REG, dataKind, location); // new storage

    if (storage->isMem()) opndManager->calculateOffset(storage);                // calc real offset
    else                  setRegUsage(storage, true);                           // mark reg as used
    
    return storage;
}

//----------------------------------------------------------------------------------------//

void PrologEpilogGenerator::setRegUsage(RegOpnd *opnd, bool flag) {
    
    if(opnd->isMem() == true) return;
    uint16 regNum = opnd->getValue();

    switch(opnd->getOpndKind()) {
        case OPND_G_REG: usedGrMask[regNum] = flag; break;
        case OPND_F_REG: usedFrMask[regNum] = flag; break;
        case OPND_P_REG: usedPrMask[regNum] = flag; break;
        case OPND_B_REG: usedBrMask[regNum] = flag; break;
        default: IPF_ERR << " " << opnd->getOpndKind() << endl;
    }
}
    
//----------------------------------------------------------------------------------------//

I_32 PrologEpilogGenerator::calculateLocRegSize() {
    
    uint16 first      = G_INARG_BASE;                             // first possible loc reg opnd location
    uint16 last       = NUM_G_REG - opndManager->getOutRegSize(); // last possible loc reg opnd location
    I_32  locRegSize = 0; 
    
    // find last used gr in local area
    for(uint16 i=first; i<last; i++) {
        if (usedGrMask[i] == true) locRegSize = i;
    }

    if (locRegSize > 0) locRegSize = locRegSize - first + 1;
    opndManager->setLocRegSize(locRegSize);                       // set new local register area size 
    return locRegSize;
}
   
//----------------------------------------------------------------------------------------//

void PrologEpilogGenerator::printRegMasks() {

    IPF_LOG << noboolalpha << "    Used grs: ";
    for(uint16 i=0; i<NUM_G_REG; i++) { if(i%8==0) IPF_LOG << " " << i << " "; IPF_LOG << usedGrMask[i]; }

    IPF_LOG << endl << "    Used frs: ";
    for(uint16 i=0; i<NUM_F_REG; i++) { if(i%8==0) IPF_LOG << " " << i << " "; IPF_LOG << usedFrMask[i]; }

    IPF_LOG << endl << "    Used prs: ";
    for(uint16 i=0; i<NUM_P_REG; i++) { if(i%8==0) IPF_LOG << " " << i << " "; IPF_LOG << usedPrMask[i]; }

    IPF_LOG << endl << "    Used brs: ";
    for(uint16 i=0; i<NUM_B_REG; i++) { if(i%8==0) IPF_LOG << " " << i << " "; IPF_LOG << usedBrMask[i]; }
}

} // IPF
} // Jitrino
