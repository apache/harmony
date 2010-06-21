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

#ifndef IPFOPNDMANAGER_H_
#define IPFOPNDMANAGER_H_

#include "IpfType.h"
#include "MemoryManager.h"

namespace Jitrino {
namespace IPF {

//========================================================================================//
// Forward declaration
//========================================================================================//

class Opnd;
class RegOpnd;
class Cfg;

//========================================================================================//
// RegStack
//========================================================================================//

class RegStack {
public:
                 RegStack();

    I_32        newInReg(I_32);
    I_32        newOutReg(I_32);
    void         setLocRegSize(I_32 locRegSize_)     { locRegSize = locRegSize_; }
    I_32        getInRegSize()                       { return inRegSize; }
    I_32        getLocRegSize()                      { return locRegSize; }
    I_32        getOutRegSize()                      { return outRegSize; }
    bool         isOutReg(RegOpnd*);

    //----------------------------------------------------------------------------//
    // Reg masks
    //----------------------------------------------------------------------------//

    RegBitSet    scratchGrMask;        // grs that can be used as scratch registers
    RegBitSet    preservGrMask;        // grs that can be used as preserv registers
    RegBitSet    spillGrMask;          // grs that can be used for spilling/filling

    RegBitSet    scratchFrMask;        // frs that can be used as scratch registers
    RegBitSet    preservFrMask;        // frs that can be used as preserv registers
    RegBitSet    spillFrMask;          // frs that can be used for spilling/filling

    RegBitSet    scratchPrMask;        // prs that can be used as scratch registers
    RegBitSet    preservPrMask;        // prs that can be used as preserv registers
    RegBitSet    spillPrMask;          // prs that can be used for spilling/filling

    RegBitSet    scratchBrMask;        // brs that can be used as scratch registers
    RegBitSet    preservBrMask;        // brs that can be used as preserv registers
    RegBitSet    spillBrMask;          // brs that can be used for spilling/filling

protected:
    I_32        inRegSize;            // number of general registers used for in args passing
    I_32        locRegSize;           // number of general registers actually used in local area (including in args)
    I_32        outRegSize;           // number of general registers used for out args passing
};

//========================================================================================//
// MemStack
//========================================================================================//
//
//      +--------------------------------+
//      | ...                            |
//      | inarg 10            (8 bytes)  |
//      | inarg 9             (8 bytes)  |
//      +--------------------------------+ <--- inarg base
//      | scratch area        (16 bytes) |
//      +================================+ <--- previous stack pointer 
//      |                                |            
//      | fp callee-saved registers      |
//      |                                |
//      +--------------------------------+ <--- fp callee base
//      |                                |
//      | int callee-saved registers     |
//      |                                |
//      +--------------------------------+ <--- int callee base
//      |                                |
//      | spill area                     |
//      |                                |
//      +--------------------------------+ <--- local base
//      | ...                            |
//      | outarg 10           (8 bytes)  |
//      | outarg 9            (8 bytes)  |
//      +--------------------------------+ <--- outarg base
//      | scratch area        (16 bytes) | (if there are calls)
//      +================================+ <--- stack pointer

class MemStack {
public:
           MemStack();
    I_32  newInSlot(I_32);
    I_32  newLocSlot(DataKind);
    I_32  newOutSlot(I_32);

    void   calculateOffset(RegOpnd*);
    I_32  calculateOffset(I_32);
    I_32  getMemStackSize();
    I_32  getSavedBase();
    
protected:
    I_32  align(I_32, I_32);

    I_32  locMemSize;        // size of memory stack area used for opnd spilling (bytes)
    I_32  outMemSize;        // size of memory stack area used for out args passing (bytes)

    I_32  inBase;            // first memory inArg offset
    I_32  locBase;           // first local memory opnd offset
    I_32  outBase;           // first memory outArg offset
};

//========================================================================================//
// StackInfo
//========================================================================================//

class StackInfo {
public:
           StackInfo();
    I_32  rpBak;           // gr num or stack offset containing return pointer (e.g. r33)
    I_32  prBak;           // gr num or stack offset containing predicate registers (e.g. r34)
    I_32  pfsBak;          // gr num or stack offset containing AR.PFS (e.g. r35)
    I_32  unatBak;         // gr num or stack offset containing AR.UNAT (e.g. r36)
    I_32  savedBase;       // mem stack offset of first saved gr (bytes)
    U_32 savedGrMask;     // mask of preserved grs saved on stack
    U_32 savedFrMask;     // mask of preserved frs saved on stack
    U_32 savedBrMask;     // mask of preserved brs saved on stack
    U_32 memStackSize;    // mem stack frame size (bytes)
};

//========================================================================================//
// OpndManager
//========================================================================================//

class OpndManager : public RegStack, public MemStack, public StackInfo {
public:
                   OpndManager(MemoryManager&, CompilationInterface&);

    //----------------------------------------------------------------------------//
    // Opnd constructors    
    //----------------------------------------------------------------------------//

    Opnd           *newOpnd(OpndKind = OPND_INVALID);
    RegOpnd        *newRegOpnd(OpndKind, DataKind, I_32 = LOCATION_INVALID);
    Opnd           *newImm(int64 = 0);
    ConstantRef    *newConstantRef(Constant*, DataKind = DATA_CONST_REF);
    NodeRef        *newNodeRef(BbNode* = NULL);
    MethodRef      *newMethodRef(MethodDesc* = NULL);
    Opnd           *newInArg(OpndKind, DataKind, U_32);
    uint16         getFpArgsNum();

    //----------------------------------------------------------------------------//
    // set / get methods
    //----------------------------------------------------------------------------//
                
    RegOpnd        *getR0();
    RegOpnd        *getR0(RegOpnd*);
    RegOpnd        *getF0();
    RegOpnd        *getF1();
    
    RegOpnd        *getP0();
    RegOpnd        *getB0();
    RegOpnd        *getR12();
    RegOpnd        *getR8();
    RegOpnd        *getF8();
    RegOpnd        *getTau();

    void           setContainCall(bool containCall_)    { containCall = containCall_; }
    bool           getContainCall()                     { return containCall; }
    uint16         getNextNodeId()                      { return maxNodeId++; }
    BbNode         *getPrologNode()                     { return prologNode; }

    //----------------------------------------------------------------------------//
    // Reg allocation support
    //----------------------------------------------------------------------------//

    I_32          newLocation(OpndKind, DataKind, RegBitSet, bool);
    I_32          newScratchReg(OpndKind, RegBitSet&);
    I_32          newPreservReg(OpndKind, RegBitSet&);
    
    //----------------------------------------------------------------------------//
    // Compressed references
    //----------------------------------------------------------------------------//

    bool         areRefsCompressed()       { return refsCompressed; }
    bool         areVtablePtrsCompressed() { return vtablePtrsCompressed; }
    RegOpnd      *getHeapBase();
    Opnd         *getHeapBaseImm();
    RegOpnd      *getVtableBase();
    Opnd         *getVtableBaseImm();
    Opnd         *getVtableOffset();
    void         initCompBases(BbNode*);
    
    //----------------------------------------------------------------------------//
    // Misc
    //----------------------------------------------------------------------------//
    
    void           insertProlog(Cfg&);
    int64          getElemBaseOffset();
    void           printStackInfo();
    void           saveThisArg();
    void           initSavedBase();
    void           initMemStackSize();
    RegOpndVector& getInArgs()               { return inArgs; }

protected:
    MemoryManager        &mm;
    CompilationInterface &compilationInterface;
    
    uint16        maxOpndId;            // used for creating new opnds
    uint16        maxNodeId;            // used for creating new nodes
    RegOpndVector inArgs;               // input args vector
    BbNode        *prologNode;
    
    RegOpnd       *r0;                  // 0 (8 bytes unsigned)
    RegOpnd       *f0;                  // 0.0
    RegOpnd       *f1;                  // 1.0
    RegOpnd       *p0;                  // true
    RegOpnd       *b0;                  // return address
    RegOpnd       *r12;                 // stack pointer
    RegOpnd       *r8;                  // return value (general)
    RegOpnd       *f8;                  // return value (floating point)
    RegOpnd       *tau;                 // opnd ignored
    
    RegOpnd      *heapBase;            // Reg opnd containing base for references decompression
    Opnd         *heapBaseImm;         // Imm opnd containing base for references decompression
    RegOpnd      *vtableBase;          // Reg opnd containing base for vtable pointers decompression
    Opnd         *vtableBaseImm;       // Imm opnd containing base for vtable pointers decompression
    Opnd         *vtableOffset;        // opnd containing vtable offset inside class object

    bool         containCall;          // method contains call instruction
    bool         refsCompressed;       // references are compressed
    bool         vtablePtrsCompressed; // vtable pointers are compressed
};

} // IPF
} // Jitrino

#endif /*IPFOPNDMANAGER_H_*/
