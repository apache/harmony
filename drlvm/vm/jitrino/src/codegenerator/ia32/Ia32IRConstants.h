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
 * @author Vyacheslav P. Shakin
 */

#ifndef _IA32_IR_CONSTANTS_H_
#define _IA32_IR_CONSTANTS_H_

#include "open/types.h"
#include "Stl.h"
#include <fstream>
#include "enc_defs.h"

namespace Jitrino
{
namespace Ia32{

//=========================================================================================================
#define lengthof(arr) (sizeof(arr)/sizeof(arr[0]))

#define UNREFERENCED(p) p

#undef  offsetof
#define offsetof(cls, field)  ((POINTER_SIZE_INT)&(((cls*)4)->field)-4)

const U_32 EmptyUint32=((U_32)-1);
const U_32 UnknownId=EmptyUint32;

const U_32 IRMaxExtendedOpnds=8;
const U_32 IRMaxNativeOpnds=4;
const U_32 IRMaxInstOpnds=512;

const U_32 IRMaxOperandByteSize = 16;


//=========================================================================================================
const U_32 IRNumRegKinds=5;

const U_32 IRMaxRegKinds=OpndKind_Reg + 1;
const U_32 IRMaxRegNamesSameKind=16;
const U_32 IRMaxRegNames=IRMaxRegNamesSameKind*IRMaxRegKinds;
//=========================================================================================================
enum MemOpndKind
{
    MemOpndKind_Null=0, 
        MemOpndKind_StackAutoLayout=0xf,
        MemOpndKind_StackManualLayout=0x10,
    MemOpndKind_Stack=0x1f,
    MemOpndKind_Heap=0x20,
    MemOpndKind_ConstantArea=0x40,
    MemOpndKind_LEA=0x80,
    MemOpndKind_Any=0xff,
};

//=========================================================================================================
enum MemOpndSubOpndKind {
    MemOpndSubOpndKind_Base=0,
    MemOpndSubOpndKind_Index,
    MemOpndSubOpndKind_Scale,
    MemOpndSubOpndKind_Displacement,
    MemOpndSubOpndKind_Count
};
U_32              countOnes(U_32 mask);

//=========================================================================================================

inline U_32       getByteSize(OpndSize size)
{ return size <= OpndSize_64 ? size : size == OpndSize_128 ? 16 : size==OpndSize_80 ? 10 : 0; }

ConditionMnemonic   reverseConditionMnemonic(ConditionMnemonic cm);
ConditionMnemonic   swapConditionMnemonic(ConditionMnemonic cm);

/** returns base condition mnemonic like Jcc for JNZ, SETcc for SETZ, etc. */
Mnemonic            getBaseConditionMnemonic(Mnemonic mn);
inline Mnemonic     getMnemonic(Mnemonic mnBase, ConditionMnemonic cm){ return (Mnemonic)(mnBase+cm); }

}}; // namespace Ia32

#endif
