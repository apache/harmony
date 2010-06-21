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

#ifndef _CODELOWERER_H_
#define _CODELOWERER_H_

#include "Inst.h"
#include "optpass.h"

namespace Jitrino {
class IRManager;
class FlowGraph;
class Node;
class Inst;
class TypeInst;


class CodeLowerer : public InstOptimizer {
public:
    CodeLowerer(IRManager& irm) : _irm(irm) {}

    void doLower();

private:
    void lowerBlock(Node *node);

    Inst* caseDefault(Inst* inst) {return inst;}

    Inst* caseTauCast(TypeInst* inst);
    Inst* caseTauCheckCast(TypeInst* inst);
    Inst* caseTauInstanceOf(TypeInst* inst);
    Inst* caseTauCheckElemType(Inst* inst);
    Inst* caseTauAsType(TypeInst* inst);

    // lower autocompress/decompress operations
    Inst* caseLdStatic(FieldAccessInst *inst);
    Inst* caseTauLdField(FieldAccessInst *inst);
    Inst* caseTauLdElem(TypeInst *inst);

    Inst* caseLdStaticAddr(FieldAccessInst *inst);
    Inst* caseLdFieldAddr(FieldAccessInst *inst);
    Inst* caseLdElemAddr(TypeInst *inst);
    Inst* caseLdArrayBaseAddr(Inst *inst);
    Inst* caseTauArrayLen(Inst *inst);

    Inst* caseTauLdInd(Inst *inst);
    Inst* caseLdRef(TokenInst *inst);
    Inst* caseLdNull(ConstInst* inst);

    // Numeric compute
    Inst* caseAdd(Inst* inst) {return caseDefault(inst);}

    Inst* caseMul(Inst* inst) {return caseDefault(inst);}

    Inst* caseSub(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauDiv(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauRem(Inst* inst) {return caseDefault(inst);}

    Inst* caseNeg(Inst* inst) {return caseDefault(inst);}

    Inst* caseMulHi(Inst* inst) {return caseDefault(inst);}

    Inst* caseMin(Inst* inst) {return caseDefault(inst);}

    Inst* caseMax(Inst* inst) {return caseDefault(inst);}

    Inst* caseAbs(Inst* inst) {return caseDefault(inst);}

    // Bitwise
    Inst* caseAnd(Inst* inst) {return caseDefault(inst);}

    Inst* caseOr(Inst* inst) {return caseDefault(inst);}

    Inst* caseXor(Inst* inst) {return caseDefault(inst);}

    Inst* caseNot(Inst* inst) {return caseDefault(inst);}

    // selection
    Inst* caseSelect(Inst* inst) {return caseDefault(inst);}

    // conversion
    Inst* caseConv(Inst* inst) {return caseDefault(inst);}

    // conversion
    Inst* caseConvZE(Inst* inst) {return caseDefault(inst);}

    // conversion
    Inst* caseConvUnmanaged(Inst* inst) {return caseDefault(inst);}

    // shifts
    Inst* caseShladd(Inst* inst) {return caseDefault(inst);}

    Inst* caseShl(Inst* inst) {return caseDefault(inst);}

    Inst* caseShr(Inst* inst) {return caseDefault(inst);}

    // comparison
    Inst* caseCmp(Inst* inst) {return caseDefault(inst);}

    Inst* caseCmp3(Inst* inst) {return caseDefault(inst);}

    // Control flow
    Inst* caseBranch(BranchInst* inst) {return caseDefault(inst);}

    Inst* caseJump(BranchInst* inst) {return caseDefault(inst);}

    Inst* caseSwitch(SwitchInst* inst) {return caseDefault(inst);}

    Inst* caseDirectCall(MethodCallInst* inst) {return caseDefault(inst);}

    Inst* caseTauVirtualCall(MethodCallInst* inst) {return caseDefault(inst);}

    Inst* caseIndirectCall(CallInst* inst) {return caseDefault(inst);}

    Inst* caseIndirectMemoryCall(CallInst* inst) {return caseDefault(inst);}

    Inst* caseJitHelperCall(JitHelperCallInst* inst) {return caseDefault(inst);}

    Inst* caseVMHelperCall(VMHelperCallInst* inst) {return caseDefault(inst);}

    Inst* caseReturn(Inst* inst) {return caseDefault(inst);}

    Inst* caseCatch(Inst* inst) {return caseDefault(inst);}

    Inst* caseThrow(Inst* inst) {return caseDefault(inst);}

    Inst* casePseudoThrow(Inst* inst) {return caseDefault(inst);}

    Inst* caseThrowSystemException(Inst* inst) {return caseDefault(inst);}

    Inst* caseThrowLinkingException(Inst* inst) {return caseDefault(inst);}

    Inst* caseRethrow(Inst* inst) {return caseDefault(inst);}

    Inst* caseLeave(Inst* inst) {return caseDefault(inst);}

    Inst* caseJSR(Inst* inst) {return caseDefault(inst);}

    Inst* caseRet(Inst* inst) {return caseDefault(inst);}

    Inst* caseSaveRet(Inst* inst) {return caseDefault(inst);}

    // load, store & mov
    Inst* caseCopy(Inst* inst) {return caseDefault(inst);}

    Inst* caseDefArg(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdConstant(ConstInst* inst) {return caseDefault(inst);};

    Inst* caseLdVar(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdVarAddr(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauLdVTableAddr(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauLdIntfcVTableAddr(TypeInst* inst) {return caseDefault(inst);}

    Inst* caseTauLdVirtFunAddr(MethodInst* inst) {return caseDefault(inst);}

    Inst* caseTauLdVirtFunAddrSlot(MethodInst* inst) {return caseDefault(inst);}

    Inst* caseLdFunAddr(MethodInst* inst) {return caseDefault(inst);}

    Inst* caseLdFunAddrSlot(MethodInst* inst) {return caseDefault(inst);}

    Inst* caseGetVTableAddr(TypeInst* inst) {return caseDefault(inst);}

    Inst* caseGetClassObj(TypeInst* inst) {return caseDefault(inst);}

    Inst* caseAddScaledIndex(Inst* inst) {return caseDefault(inst);}

    Inst* caseStVar(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauStInd(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauStField(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauStElem(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauStStatic(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauStRef(Inst* inst) {return caseDefault(inst);}

    // checks
    Inst* caseTauCheckBounds(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauCheckLowerBound(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauCheckUpperBound(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauCheckNull(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauCheckZero(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauCheckDivOpnds(Inst* inst) {return caseDefault(inst);}
    
    Inst* caseTauCheckFinite(Inst* inst) {return caseDefault(inst);}

    // alloc

    Inst* caseNewObj(Inst* inst) {return caseDefault(inst);}

    Inst* caseNewArray(Inst* inst) {return caseDefault(inst);}

    Inst* caseNewMultiArray(Inst* inst) {return caseDefault(inst);}

    // sync

    Inst* caseTauMonitorEnter(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauMonitorExit(Inst* inst) {return caseDefault(inst);}

    Inst* caseTypeMonitorEnter(Inst* inst) {return caseDefault(inst);}

    Inst* caseTypeMonitorExit(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdLockAddr(Inst* inst) {return caseDefault(inst);}

    Inst* caseIncRecCount(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauBalancedMonitorEnter(Inst* inst) {return caseDefault(inst);}

    Inst* caseBalancedMonitorExit(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauOptimisticBalancedMonitorEnter(Inst* inst) {return caseDefault(inst);}

    Inst* caseOptimisticBalancedMonitorExit(Inst* inst) {return caseDefault(inst);}

    Inst* caseMonitorEnterFence(Inst* inst) {return caseDefault(inst);}

    Inst* caseMonitorExitFence(Inst* inst) {return caseDefault(inst);}

    // type checking
    Inst* caseTauStaticCast(TypeInst* inst) {return caseDefault(inst);}

    Inst* caseSizeof(TypeInst* inst) {return caseDefault(inst);}

    Inst* caseInitType(TypeInst* inst) {return caseDefault(inst);}

    // labels

    Inst* caseLabel(Inst* inst) {return caseDefault(inst);}

    Inst* caseCatchLabelInst(Inst* inst) {return caseDefault(inst);}

    // method entry/exit
    Inst* caseMethodEntryLabel(Inst* inst) {return caseDefault(inst);}

    Inst* caseMethodEntry(Inst* inst) {return caseDefault(inst);}

    Inst* caseMethodEnd(Inst* inst) {return caseDefault(inst);}

    // source markers
    Inst* caseMethodMarker(Inst* inst) {return caseDefault(inst);}

    Inst* casePhi(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauPi(TauPiInst* inst) {return caseDefault(inst);}

    Inst* caseIncCounter(Inst* inst) {return caseDefault(inst);}

    Inst* casePrefetch(Inst* inst) {return caseDefault(inst);}

    // compressed references

    Inst* caseUncompressRef(Inst* inst) {return caseDefault(inst);}

    Inst* caseCompressRef(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdFieldOffset(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdFieldOffsetPlusHeapbase(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdArrayBaseOffset(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdArrayBaseOffsetPlusHeapbase(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdArrayLenOffset(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdArrayLenOffsetPlusHeapbase(Inst* inst) {return caseDefault(inst);}

    Inst* caseAddOffset(Inst* inst) {return caseDefault(inst);}

    Inst* caseAddOffsetPlusHeapbase(Inst* inst) {return caseDefault(inst);}

    // new tau methods
    Inst* caseTauPoint(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauEdge(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauAnd(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauSafe(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauUnsafe(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauHasType(TypeInst* inst) {return caseDefault(inst);}

    Inst* caseTauHasExactType(TypeInst* inst) {return caseDefault(inst);}

    Inst* caseTauIsNonNull(Inst* inst) {return caseDefault(inst);}

    Inst* caseIdentHC(Inst* inst) { return caseDefault(inst);  }
    
    IRManager& _irm;
    bool _preserveSsa;
};

} //namespace Jitrino 

#endif //_CODELOWERER_H_
