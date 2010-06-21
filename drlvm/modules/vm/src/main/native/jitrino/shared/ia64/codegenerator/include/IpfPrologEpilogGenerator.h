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

#ifndef IPFPROLOGEPILOGGENERATOR_H_
#define IPFPROLOGEPILOGGENERATOR_H_

#include "IpfCfg.h"
#include "IpfOpndManager.h"

namespace Jitrino {
namespace IPF {

//========================================================================================//
// Defines
//========================================================================================//

#define SITE_REG   1
#define SITE_STACK 2

//========================================================================================//
// PrologEpilogGenerator
//========================================================================================//

class PrologEpilogGenerator {
public:
                  PrologEpilogGenerator(Cfg&);
    void          genPrologEpilog();

private:
    void          buildSets();
    void          reassignOutRegArgs();
    void          genCode();

    void          saveRestoreRp();
    void          saveRestorePr();
    void          genAlloc();
    Opnd*         saveRestorePfs();
    void          saveRestoreUnat();
    void          saveRestorePreservedBr();
    void          saveRestorePreservedGr();
    void          saveRestorePreservedFr();
    void          saveRestoreSp();
    
    RegOpnd*      newStorage(DataKind, uint16);
    void          setRegUsage(RegOpnd*, bool);
    I_32         calculateLocRegSize();
    void          printRegMasks();

    MemoryManager &mm;
    Cfg           &cfg;
    OpndManager   *opndManager;
    
    RegOpndSet    outRegArgs;       // list of out arg registers
    bool          containCall;      // method contains call
    Opnd          *p0;              // p0 (true predicate)
    Opnd          *sp;              // gr12 (stack pointer)
    Opnd          *stackAddr;       // scratch opnd to hold stack address during saving/restoring

    InstList      prologInsts;      // prolog instructions
    InstList      epilogInsts;      // epilog instructions

    InstList      allocInsts;       // "alloc" instruction
    InstList      saveSpInsts;      // instructions to save stack pointer
    InstList      savePfsInsts;     // instructions to save AR.PFS
    InstList      saveUnatInsts;    // instructions to save AR.UNAT
    InstList      saveGrsInsts;     // instructions to save preserved general registers
    InstList      saveFrsInsts;     // instructions to save preserved floating registers
    InstList      saveBrsInsts;     // instructions to save preserved branch registers
    InstList      savePrsInsts;     // instructions to save preserved predicate registers
    InstList      saveRpInsts;      // instructions to save return pointer
    InstList      restRpInsts;      // instructions to restore return pointer
    InstList      restPrsInsts;     // instructions to restore preserved predicate registers
    InstList      restBrsInsts;     // instructions to restore preserved branch registers
    InstList      restFrsInsts;     // instructions to restore preserved floating registers
    InstList      restGrsInsts;     // instructions to restore preserved general registers
    InstList      restUnatInsts;    // instructions to restore AR.UNAT
    InstList      restPfsInsts;     // instructions to restore AR.PFS
    InstList      restSpInsts;      // instructions to restore stack pointer

    RegBitSet     usedGrMask;       // grs used in current method (1 means reg is free)
    RegBitSet     usedFrMask;       // frs used in current method (1 means reg is used)
    RegBitSet     usedPrMask;       // prs used in current method (1 means reg is free)
    RegBitSet     usedBrMask;       // brs used in current method (1 means reg is free)

    NodeVector    epilogNodes;      // epilog nodes list (nodes which contain "br.ret")
};

} // IPF
} // Jitrino

#endif /*IPFPROLOGEPILOGGENERATOR_H_*/
