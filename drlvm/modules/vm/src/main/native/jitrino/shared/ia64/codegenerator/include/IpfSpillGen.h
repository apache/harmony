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

#ifndef IPFSPILLGEN_H_
#define IPFSPILLGEN_H_

#include "IpfCfg.h"

namespace Jitrino {
namespace IPF {

//========================================================================================//
// SpillGen
//========================================================================================//

class SpillGen {
public:
                  SpillGen(Cfg&);
    void          genSpillCode();

protected:
    void          spillInst(Inst* inst);
    RegOpnd       *spillOpnd(RegOpnd*, bool);
    void          resetSpillRegMasks();
    I_32         getAvailableSpillReg(OpndKind);
    I_32         getAvailableReg(RegBitSet&, int16);
    bool          containsStackOpnd(Inst*);
    void          printResult(InstVector&, uint16);

    void          spillGr(RegOpnd*);
    void          spillFr(RegOpnd*);
    void          spillBr(RegOpnd*);
    void          spillPr(RegOpnd*);

    void          fillGr(RegOpnd*);
    void          fillFr(RegOpnd*);
    void          fillBr(RegOpnd*);
    void          fillPr(RegOpnd*);

    MemoryManager &mm;
    Cfg           &cfg;
    OpndManager   *opndManager;
    
    Opnd          *p0;            // opnd representing p0
    Opnd          *sp;            // opnd representing stack pointer
    Opnd          *stackAddr;     // opnd used to calculate stack address (always r14)
    InstVector    fillCode;       // buffer for fill insts to be inserted in node inst list
    InstVector    spillCode;      // buffer for spill insts to be inserted in node inst list
    
    I_32         outOffset;      // offset of the first mem out arg (bytes)
    I_32         locOffset;      // offset of the first mem local (bytes)
    I_32         psfOffset;      // offset of previous stack frame (bytes)
    I_32         inOffset;       // offset of the first mem in arg (bytes)
    I_32         maxOffset;      // first unavailable in current frame offset (bytes)
    
    RegBitSet     spillGrMask;    // gr available for spilling (true - reg is available)
    RegBitSet     spillFrMask;    // fr available for spilling (true - reg is available)
    RegBitSet     spillPrMask;    // pr available for spilling (true - reg is available)
    RegBitSet     spillBrMask;    // br available for spilling (true - reg is available)
};

} // IPF
} // Jitrino

#endif /*IPFSPILLGEN_H_*/
