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

#ifndef _SIMPLIFIER_H_
#define _SIMPLIFIER_H_

#include "VMInterface.h"
#include "Opcode.h"
#include "Type.h"
#include "Inst.h"
#include "optpass.h"

namespace Jitrino {

class FlowGraph;
class IRManager;
class InstFactory;
class Reassociate;


class Simplifier : public InstOptimizer {
public:
    Simplifier(IRManager& irm, bool latepass=false,
           Reassociate *reassociate=0);
    //
    // returns an Opnd if the add can be simplified
    // null if the operation cannot be simplified
    //
    Opnd* simplifyAdd(Type*, Modifier, Opnd* src1, Opnd* src2);
    Opnd* simplifySub(Type*, Modifier, Opnd* src1, Opnd* src2);
    Opnd* simplifyMul(Type*, Modifier, Opnd* src1, Opnd* src2);
    Opnd* simplifyTauDiv(Type*, Modifier, Opnd* src1, Opnd* src2, Opnd *tauCheckedOpnds);
    Opnd* simplifyTauRem(Type*, Modifier, Opnd* src1, Opnd* src2, Opnd *tauCheckedOpnds);
    Opnd* simplifyNeg(Type*, Opnd* src);
    Opnd* simplifyMulHi(Type*, Modifier, Opnd* src1, Opnd* src2);
    Opnd* simplifyMin(Type*, Opnd* src1, Opnd* src2);
    Opnd* simplifyMax(Type*, Opnd* src1, Opnd* src2);
    Opnd* simplifyAbs(Type*, Opnd* src1);
    Opnd* simplifyAnd(Type*, Opnd* src1, Opnd* src2);
    Opnd* simplifyOr(Type*, Opnd* src1, Opnd* src2);
    Opnd* simplifyXor(Type*, Opnd* src1, Opnd* src2);
    Opnd* simplifyNot(Type*, Opnd* src1);
    Opnd* simplifySelect(Type*, Opnd* src1, Opnd* src2, Opnd* src3);
    Opnd* simplifyConv(Type*, Type::Tag toType, Modifier, Opnd* src);
    Opnd* simplifyConvZE(Type*, Type::Tag toType, Modifier, Opnd* src);
    Opnd* simplifyConvUnmanaged(Type*, Type::Tag toType, Modifier, Opnd* src);
    Opnd* simplifyShladd(Type* dstType, Opnd* value, Opnd* shiftAmount, Opnd *addto);
    Opnd* simplifyShl(Type* dstType, Modifier, Opnd* value, Opnd* shiftAmount);
    Opnd* simplifyShr(Type* dstType, Modifier, Opnd* value, Opnd* shiftAmount);
    // Comparison
    Opnd* simplifyCmp(Type* dstType, Type::Tag srcType, ComparisonModifier, Opnd* src1, Opnd* src2);
    // 3-way Java comparison
    Opnd* simplifyCmp3(Type* dstType, Type::Tag srcType, ComparisonModifier, Opnd* src1, Opnd* src2);
    // Control flow
    // If it can simplify the given branch, then makes a emits branch and
    // returns true.  Otherwise, returns false.  You should probably try
    // canFoldBranch() first.
    bool simplifyBranch(Type::Tag instType, ComparisonModifier mod,
                        LabelInst* label, Opnd* src1);
    bool simplifyBranch(Type::Tag instType, ComparisonModifier mod,
                        LabelInst* label, Opnd* src1, Opnd* src2);
    // tests to fold a branch away completely.  side-effects isTaken
    // and returns True if branch can be folded away.
    bool canFoldBranch(Type::Tag, ComparisonModifier, Opnd* src1, Opnd* src2,bool& isTaken);
    bool canFoldBranch(Type::Tag, ComparisonModifier, Opnd* src, bool& isTaken);
    bool simplifySwitch(U_32 numLabels, LabelInst* label[], LabelInst* defaultLabel, Opnd* src);

    Opnd* simplifyTauVirtualCall(MethodDesc* methodDesc,
                                 Type* returnType,
                                 Opnd* tauNullCheckedFirstArg,
                                 Opnd* tauTypesChecked,
                                 U_32 numArgs,
                                 Opnd* args[]);

    Inst* simplifyIndirectCallInst(Opnd* funPtr,
                                   Type* returnType,
                                   Opnd* tauNullCheckedFirstArg,
                                   Opnd* tauTypesChecked,
                                   U_32 numArgs,
                                   Opnd** args);

    Inst* simplifyIndirectMemoryCallInst(Opnd* funPtr,
                                         Type* returnType,
                                         Opnd* tauNullCheckedFirstArg,
                                         Opnd* tauTypesChecked,
                                         U_32 numArgs,
                                         Opnd** args);

    Inst* simplifyJitHelperCall(JitHelperCallInst* inst);

    // loads
    Opnd* simplifyLdRef(Modifier mod, Type *dstType, 
                        U_32 token,
                        MethodDesc* enclosingMethod);
    Opnd* simplifyTauLdInd(Modifier mod, Type *dstType, 
                           Type::Tag type,
                           Opnd* ptr,
                           Opnd* tauBaseNonNull,
                           Opnd* tauAddressInRange);
    Opnd* simplifyTauLdVTableAddr(Opnd* base, Opnd *tauBaseNonNull);
    Opnd* simplifyTauLdIntfcVTableAddr(Opnd* base, Type* vtableType);
    Opnd* simplifyTauLdVirtFunAddr(Opnd* vtable, Opnd *tauVtableHasDesc, MethodDesc*);
    Opnd* simplifyTauLdVirtFunAddrSlot(Opnd* vtable, Opnd *tauVtableHasDesc, MethodDesc*);

    // array operations
    Opnd* simplifyTauArrayLen(Type* dstType, Type::Tag type, Opnd* base);
    Opnd* simplifyTauArrayLen(Type* dstType, Type::Tag type, Opnd* base,
                              Opnd* tauNullChecked, Opnd *tauTypeChecked);
    Opnd* simplifyLdArrayBaseAddr(Type* elemType, Opnd* array) {
        return NULL;
    }
    Opnd* simplifyAddScaledIndex(Opnd* base, Opnd* index);
    // stores
    void simplifyTauStInd(Inst *inst); // modifies instr in place if possible
    void simplifyTauStField(Inst *inst); // modifies instr in place if possible
    void simplifyTauStElem(Inst *inst); // modifies instr in place if possible
    void simplifyTauStStatic(Inst *inst); // modifies instr in place if possible
    void simplifyTauStRef(Inst *inst); // modifies instr in place if possible

    // checks
    // These all return a tau normally.  If we have reason to believe that they
    // will always pass, we return the tau guaranteeing that.  If they would always
    // fail, returns the destOp of a TauUnsafe instruction.
    Opnd* simplifyTauCheckBounds(Opnd* arrayLen, Opnd* index, bool &alwaysThrows);
    Opnd* simplifyTauCheckLowerBound(Opnd* lb, Opnd* idx, bool &alwaysThrows);
    Opnd* simplifyTauCheckUpperBound(Opnd* idx, Opnd* ub, bool &alwaysThrows);
    Opnd* simplifyTauCheckNull(Opnd*, bool &alwaysThrows);
    Opnd* simplifyTauCheckZero(Opnd*, bool &alwaysThrows);
    Opnd* simplifyTauCheckDivOpnds(Opnd* src1, Opnd* src2, bool &alwaysThrows);
    Opnd* simplifyTauCheckElemType(Opnd* arrayBase, Opnd* src, bool &alwaysThrows);
    Opnd* simplifyTauCheckFinite(Opnd*, bool &alwaysThrows);
    Opnd* simplifyTauCheckCast(Opnd* src, Opnd* tauCheckedNull, Type *castType, 
                               bool &alwaysThrows);
    Opnd* simplifyTauHasType(Opnd* src, Type *castType);
    Opnd* simplifyTauHasExactType(Opnd* src, Type *castType);
    Opnd* simplifyTauIsNonNull(Opnd* src);

    // type checking
    Opnd* simplifyTauCast(Opnd* src, Opnd* tauCheckedNull, Type* castType);
    Opnd* simplifyTauStaticCast(Opnd* src, Opnd *tauNonNull, Type* castType);
    Opnd* simplifyTauAsType(Opnd* src, Opnd *tauCheckedNull, Type* type);
    Opnd* simplifyTauInstanceOf(Opnd* src, Opnd *tauCheckedNull, Type* type);

    // compressed references
    Opnd* simplifyUncompressRef(Opnd* compRef);
    Opnd* simplifyCompressRef(Opnd* uncompRef);
    Opnd* simplifyAddOffset(Type *ptrType, Opnd* uncompBase, Opnd *offset);
    Opnd* simplifyAddOffsetPlusHeapbase(Type *ptrType, Opnd* compBase, 
                                        Opnd *offsetPlusHeapbase);

    // tau operations
    Opnd* simplifyTauAnd(MultiSrcInst *tauAndInst);
    
    Opnd* propagateCopy(Opnd*);

    static bool isNonNullObject(Opnd*);
    static bool isNonNullParameter(Opnd*);
    static bool isNullObject(Opnd*);
    static bool isExactType(Opnd*);
protected:
    IRManager& irManager;
    ControlFlowGraph& flowGraph;

    // genOp routines create an instruction, may never return NULL
    // but may simplify it to some other instruction
    // numeric
    virtual Inst* genAdd(Type*, Modifier, Opnd* src1, Opnd* src2) = 0;
    virtual Inst* genSub(Type*, Modifier, Opnd* src1, Opnd* src2) = 0;
    virtual Inst* genNeg(Type*, Opnd* src) = 0;
    virtual Inst* genMul(Type*, Modifier, Opnd* src1, Opnd* src2) = 0;
    virtual Inst* genMulHi(Type*, Modifier, Opnd* src1, Opnd* src2) = 0;
    virtual Inst* genMin(Type*, Opnd* src1, Opnd* src2) = 0;
    virtual Inst* genMax(Type*, Opnd* src1, Opnd* src2) = 0;
    virtual Inst* genAbs(Type*, Opnd* src1) = 0;
    // bitwise
    virtual Inst* genAnd(Type*, Opnd* src1, Opnd* src2) = 0;
    virtual Inst* genOr(Type*, Opnd* src1, Opnd* src2) = 0;
    virtual Inst* genXor(Type*, Opnd* src1, Opnd* src2) = 0;
    virtual Inst* genNot(Type*, Opnd* src1) = 0;
    virtual Inst* genSelect(Type*,
                            Opnd* src1, Opnd* src2, Opnd* src3) = 0;
    // conversion
    virtual Inst* genConv(Type*, Type::Tag toType, Modifier, 
                          Opnd* src1) = 0;
    virtual Inst* genConvZE(Type*, Type::Tag toType, Modifier, 
                          Opnd* src1) = 0;
    virtual Inst* genConvUnmanaged(Type*, Type::Tag toType, Modifier, 
                          Opnd* src1) = 0;
    // shifts
    virtual Inst* genShladd(Type*, Opnd *value,
                            Opnd* shiftAmount, Opnd* addTo) = 0;
    virtual Inst* genShl(Type*, Modifier,
                         Opnd* src1, Opnd* src2) = 0;
    virtual Inst* genShr(Type*, Modifier,
                         Opnd* src1, Opnd* src2) = 0;
    // comparison
    virtual Inst* genCmp(Type*, Type::Tag insttype, 
                         ComparisonModifier, Opnd* src1, Opnd* src2) = 0;
    // control flow
    virtual void genJump(LabelInst* label) = 0;
    virtual void genBranch(Type::Tag instType, ComparisonModifier mod, 
                           LabelInst* label, Opnd* src1, Opnd* src2) = 0;
    virtual void genBranch(Type::Tag instType, ComparisonModifier mod, 
                           LabelInst* label, Opnd* src1) = 0;
    virtual Inst* genDirectCall(MethodDesc*,
                                Type* returnType,
                                Opnd* tauNullCheckedFirstArg,
                                Opnd* tauTypesChecked,
                                U_32 numArgs,
                                Opnd* args[]) = 0;
    // load, store & move
    virtual Inst* genLdConstant(I_32 val) = 0;
    virtual Inst* genLdConstant(int64 val) = 0;
    virtual Inst* genLdConstant(float val) = 0;
    virtual Inst* genLdConstant(double val) = 0;
    virtual Inst* genLdConstant(Type *type, ConstInst::ConstValue val) = 0;
    virtual Inst* genTauLdInd(Modifier mod, Type *dstType, Type::Tag type, Opnd *ptr, 
                              Opnd *tauNonNullBase, Opnd *tauAddressInRange) = 0;
    virtual Inst* genLdRef(Modifier mod, Type *dstType,
                           U_32 token, MethodDesc *enclosingMethod) = 0;
    virtual Inst* genLdFunAddrSlot(MethodDesc*) = 0;
    virtual Inst* genGetVTableAddr(ObjectType* type) = 0;
    // compressed references
    virtual Inst* genCompressRef(Opnd *uncompref) = 0;
    virtual Inst* genUncompressRef(Opnd *compref) = 0;
    virtual Inst* genLdFieldOffsetPlusHeapbase(FieldDesc*) = 0;
    virtual Inst* genLdArrayBaseOffsetPlusHeapbase(Type *elemType) = 0;
    virtual Inst* genLdArrayLenOffsetPlusHeapbase(Type *elemType) = 0;
    virtual Inst* genAddOffsetPlusHeapbase(Type *ptrType, Opnd *compRef, 
                                           Opnd *offsetPlusHeapbase) = 0;
    virtual Inst* genTauSafe() = 0;
    virtual Inst* genTauMethodSafe() = 0;
    virtual Inst* genTauUnsafe() = 0;
    virtual Inst* genTauStaticCast(Opnd *src, Opnd *tauCheckedCast, Type *castType) = 0;
    virtual Inst* genTauHasType(Opnd *src, Type *castType) = 0;
    virtual Inst* genTauHasExactType(Opnd *src, Type *castType) = 0;
    virtual Inst* genTauIsNonNull(Opnd *src) = 0;

    // helper for store simplification, builds/finds simpler src, possibly
    // modifies typetag or store modifier. 
    virtual Opnd *simplifyStoreSrc(Opnd *, Type::Tag &typetag, Modifier &mod,
                                   bool compressRef) = 0;

    virtual void genThrowSystemException(CompilationInterface::SystemExceptionId) = 0;

    virtual void  foldBranch(BranchInst* branchInst, bool isTaken) = 0;
    virtual void  foldSwitch(SwitchInst* switchInst, U_32 index) = 0;
    virtual void  eliminateCheck(Inst* checkInst, bool alwaysThrows) = 0;

    Opnd *planMul32(I_32 multiplier, Opnd *opnd);
    Opnd *planMul64(int64 multiplier, Opnd *opnd);
public:
    //-------------------------------------------------------------------------
    // InstOptimizer methods
    //-------------------------------------------------------------------------
    virtual Inst* optimizeInst(Inst* inst);
    Inst* caseAdd(Inst* inst) {
        Opnd* opnd = simplifyAdd(inst->getDst()->getType(),
                                 inst->getModifier(),
                                 inst->getSrc(0),
                                 inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseMul(Inst* inst) {
        Opnd* opnd = simplifyMul(inst->getDst()->getType(),
                                 inst->getModifier(),
                                 inst->getSrc(0),
                                 inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseSub(Inst* inst) {
        Opnd* opnd = simplifySub(inst->getDst()->getType(),
                                 inst->getModifier(),
                                 inst->getSrc(0),
                                 inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseTauDiv(Inst* inst) {
        Opnd* opnd = simplifyTauDiv(inst->getDst()->getType(),
                                    inst->getModifier(),
                                    inst->getSrc(0),
                                    inst->getSrc(1),
                                    inst->getSrc(2));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseTauRem(Inst* inst) {
        Opnd* opnd = simplifyTauRem(inst->getDst()->getType(),
                                    inst->getModifier(),
                                    inst->getSrc(0),
                                    inst->getSrc(1),
                                    inst->getSrc(2));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseNeg(Inst* inst) {
        Opnd* opnd = simplifyNeg(inst->getDst()->getType(),
                                 inst->getSrc(0));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseMulHi(Inst* inst) {
        Opnd* opnd = simplifyMulHi(inst->getDst()->getType(),
                                   inst->getModifier(),
                                   inst->getSrc(0),
                                   inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseMin(Inst* inst) {
        Opnd* opnd = simplifyMin(inst->getDst()->getType(),
                                 inst->getSrc(0),
                                 inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseMax(Inst* inst) {
        Opnd* opnd = simplifyMax(inst->getDst()->getType(),
                                 inst->getSrc(0),
                                 inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseAbs(Inst* inst) {
        Opnd* opnd = simplifyAbs(inst->getDst()->getType(),
                                 inst->getSrc(0));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    
    // Bitwise
    Inst* caseAnd(Inst* inst) {
        Opnd* opnd = simplifyAnd(inst->getDst()->getType(),
                                 inst->getSrc(0),
                                 inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseOr(Inst* inst) {
        Opnd* opnd = simplifyOr(inst->getDst()->getType(),
                                inst->getSrc(0),
                                inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseXor(Inst* inst) {
        Opnd* opnd = simplifyXor(inst->getDst()->getType(),
                                 inst->getSrc(0),
                                 inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseNot(Inst* inst) {
        Opnd* opnd = simplifyNot(inst->getDst()->getType(),
                                 inst->getSrc(0));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }

    // selection
    Inst* caseSelect(Inst* inst) {
        Opnd* opnd = simplifySelect(inst->getDst()->getType(),
                                    inst->getSrc(0),
                                    inst->getSrc(1),
                                    inst->getSrc(2));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    // conversion
    Inst* caseConv(Inst* inst) {
        Opnd* opnd = simplifyConv(inst->getDst()->getType(),
                                  inst->getType(),
                                  inst->getModifier(),
                                  inst->getSrc(0));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }

    // conversion
    Inst* caseConvUnmanaged(Inst* inst) {
        return inst;
    }
    
    Inst* caseConvZE(Inst* inst) {
        return inst;
    }


    // shifts
    Inst* caseShladd(Inst* inst) {
        Opnd* opnd = simplifyShladd(inst->getDst()->getType(),
                                    inst->getSrc(0),
                                    inst->getSrc(1),
                                    inst->getSrc(2));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseShl(Inst* inst) {
        Opnd* opnd = simplifyShl(inst->getDst()->getType(),
                                 inst->getModifier(),
                                 inst->getSrc(0),
                                 inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseShr(Inst* inst) {
        Opnd* opnd = simplifyShr(inst->getDst()->getType(),
                                 inst->getModifier(),
                                 inst->getSrc(0),
                                 inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }

    // comparison
    Inst* caseCmp(Inst* inst) {
        Opnd* opnd = simplifyCmp(inst->getDst()->getType(),
                                 inst->getType(),
                                 inst->getComparisonModifier(),
                                 inst->getSrc(0),
                                 inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }

    Inst* caseCmp3(Inst* inst) {
        Opnd* opnd = simplifyCmp3(inst->getDst()->getType(),
                                  inst->getType(),
                                  inst->getComparisonModifier(),
                                  inst->getSrc(0),
                                  inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }

    Inst* caseBranch(BranchInst* inst);

    Inst* caseJump(BranchInst* inst) { return caseDefault(inst); };

    Inst* caseSwitch(SwitchInst* inst);
    
    Inst* caseDirectCall(MethodCallInst* inst) {return caseDefault(inst);}

    Inst* caseTauVirtualCall(MethodCallInst* inst) {return caseDefault(inst);}

    Inst* caseIndirectCall(CallInst* inst);

    Inst* caseIndirectMemoryCall(CallInst* inst);

    Inst* caseJitHelperCall(JitHelperCallInst* inst) {return simplifyJitHelperCall(inst->asJitHelperCallInst());}

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

    Inst* caseCopy(Inst* inst) {return inst->getSrc(0)->getInst(); }

    Inst* caseDefArg(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdConstant(ConstInst* inst) {return caseDefault(inst);}

    Inst* caseLdNull(ConstInst* inst) {return caseDefault(inst);}

    Inst*
    caseLdRef(TokenInst* inst) {
    Opnd* opnd = simplifyLdRef(inst->getModifier(),
                               inst->getDst()->getType(),
                               inst->getToken(),
                               inst->getEnclosingMethod());
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }

    Inst* caseLdVar(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdVarAddr(Inst* inst) {return caseDefault(inst);}

    Inst*
    caseTauLdInd(Inst* inst)          {
    Opnd* opnd = simplifyTauLdInd(inst->getModifier(),
                                   inst->getDst()->getType(),
                                   inst->getType(),
                                   inst->getSrc(0),
                                   inst->getSrc(1),
                                   inst->getSrc(2));
    if (opnd != NULL)
        return opnd->getInst();
    return inst;
    }

    Inst* caseTauLdField(FieldAccessInst* inst) {return caseDefault(inst);}

    Inst* caseLdStatic(FieldAccessInst* inst) {return caseDefault(inst);}

    Inst* caseTauLdElem(TypeInst* inst) {return caseDefault(inst);}

    // address loads
    Inst* caseLdFieldAddr(FieldAccessInst* inst) {return caseDefault(inst);}

    Inst* caseLdStaticAddr(FieldAccessInst* inst) {return caseDefault(inst);}

    Inst* caseLdElemAddr(TypeInst* inst) {return caseDefault(inst);}

    Inst* caseTauLdVTableAddr(Inst* inst) {
        Opnd* opnd = simplifyTauLdVTableAddr(inst->getSrc(0),
                                             inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseTauLdIntfcVTableAddr(TypeInst* inst) {
        Opnd* opnd = simplifyTauLdIntfcVTableAddr(inst->getSrc(0),
                                                  inst->getTypeInfo());
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseTauLdVirtFunAddr(MethodInst* inst) {
        Opnd* opnd = simplifyTauLdVirtFunAddr(inst->getSrc(0),
                                              inst->getSrc(1),
                                              inst->getMethodDesc());
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseTauLdVirtFunAddrSlot(MethodInst* inst) {
        Opnd* opnd = simplifyTauLdVirtFunAddrSlot(inst->getSrc(0),
                                                  inst->getSrc(1),
                                                  inst->getMethodDesc());
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }

    Inst* caseLdFunAddr(MethodInst* inst) {return caseDefault(inst);}

    Inst* caseLdFunAddrSlot(MethodInst* inst) {return caseDefault(inst);}

    Inst* caseGetVTableAddr(TypeInst* inst) {return caseDefault(inst);}

    Inst* caseGetClassObj(TypeInst* inst) {return caseDefault(inst);}


    // array access
    Inst* caseTauArrayLen(Inst* inst) {
        Opnd* opnd = simplifyTauArrayLen(inst->getDst()->getType(),
                                         inst->getType(),
                                         inst->getSrc(0),
                                         inst->getSrc(1),
                                         inst->getSrc(2));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }

    Inst* caseLdArrayBaseAddr(Inst* inst) {return caseDefault(inst);}

    Inst* caseAddScaledIndex(Inst* inst) {
        Opnd* opnd = simplifyAddScaledIndex(inst->getSrc(0),
                                            inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    
    // stores
    Inst* caseStVar(Inst* inst) {return caseDefault(inst);}

    Inst* caseTauStInd(Inst* inst) {
        simplifyTauStInd(inst);
        return inst;
    }

    Inst* caseTauStField(Inst* inst) {
        simplifyTauStField(inst);
        return inst;
    }

    Inst* caseTauStElem(Inst* inst) {
        simplifyTauStElem(inst);
        return inst;
    }

    Inst* caseTauStStatic(Inst* inst) {
        simplifyTauStStatic(inst);
        return inst;
    }

    Inst* caseTauStRef(Inst* inst) {
        simplifyTauStRef(inst);
        return inst;
    }

    // checks
    Inst* caseTauCheckBounds(Inst* inst) {
        bool alwaysThrows = false;
        Opnd* opnd = simplifyTauCheckBounds(inst->getSrc(0), inst->getSrc(1),
                                            alwaysThrows);
        if (opnd) {
            eliminateCheck(inst, alwaysThrows);
            return opnd->getInst();
        }
        return inst;
    }
    Inst* caseTauCheckLowerBound(Inst* inst) {
        bool alwaysThrows = false;
        Opnd* opnd = simplifyTauCheckLowerBound(inst->getSrc(0), inst->getSrc(1),
                                                alwaysThrows);
        if (opnd) {
            eliminateCheck(inst, alwaysThrows);
            return opnd->getInst();
        }
        return inst;
    }
    Inst* caseTauCheckUpperBound(Inst* inst) {
        bool alwaysThrows = false;
        Opnd* opnd = simplifyTauCheckUpperBound(inst->getSrc(0), inst->getSrc(1),
                                                alwaysThrows);
        if (opnd) {
            eliminateCheck(inst, alwaysThrows);
            return opnd->getInst();
        }
        return inst;
    }
    Inst* caseTauCheckNull(Inst* inst) {
        bool alwaysThrows = false;
        Opnd* opnd = simplifyTauCheckNull(inst->getSrc(0), alwaysThrows);
        if (opnd) {
            eliminateCheck(inst, alwaysThrows);
            return opnd->getInst();
        }
        return inst;
    }
    Inst* caseTauCheckZero(Inst* inst) {
        bool alwaysThrows = false;
        Opnd* opnd = simplifyTauCheckZero(inst->getSrc(0), alwaysThrows);
        if (opnd) {
            eliminateCheck(inst, alwaysThrows);
            return opnd->getInst();
        }
        return inst;
    }
    Inst* caseTauCheckDivOpnds(Inst* inst) {
        bool alwaysThrows = false;
        Opnd* opnd = simplifyTauCheckDivOpnds(inst->getSrc(0), inst->getSrc(1),
                                              alwaysThrows);
        if (opnd) {
            eliminateCheck(inst, alwaysThrows);
            return opnd->getInst();
        }
        return inst;
    }
    Inst* caseTauCheckElemType(Inst* inst) {
        bool alwaysThrows = false;
        Opnd* opnd = simplifyTauCheckElemType(inst->getSrc(0), inst->getSrc(1),
                                              alwaysThrows);
        if (opnd) {
            eliminateCheck(inst, alwaysThrows);
            return opnd->getInst();
        }
        return inst;
    }
    Inst* caseTauCheckFinite(Inst* inst) {
        bool alwaysThrows = false;
        Opnd* opnd = simplifyTauCheckFinite(inst->getSrc(0), alwaysThrows);
        if (opnd) {
            eliminateCheck(inst, alwaysThrows);
            return opnd->getInst();
        }
        return inst;
    }

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
    Inst* caseTauStaticCast(TypeInst *inst) {
        Opnd* opnd = simplifyTauStaticCast(inst->getSrc(0), inst->getSrc(1),
                                           inst->getTypeInfo());
        if (opnd != NULL) {
            return opnd->getInst();
        }
        return inst;
    }
    Inst* caseTauCast(TypeInst* inst) {
        Opnd* opnd = simplifyTauCast(inst->getSrc(0), inst->getSrc(1), inst->getTypeInfo());
        if (opnd != NULL) {
            eliminateCheck(inst, false);
            return opnd->getInst();
        }
        return inst;
    }

    Inst* caseSizeof(TypeInst* inst) {return caseDefault(inst);}

    Inst* caseTauAsType(TypeInst* inst) {
        Opnd* opnd = simplifyTauAsType(inst->getSrc(0), inst->getSrc(1), inst->getTypeInfo());
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }

    Inst* caseTauInstanceOf(TypeInst* inst) {
        Opnd* opnd = simplifyTauInstanceOf(inst->getSrc(0),
                                           inst->getSrc(1),
                                           inst->getTypeInfo());
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }
    Inst* caseInitType(TypeInst* inst) {
        return inst;
    }

    // labels
    Inst* caseLabel(Inst* inst) {return caseDefault(inst);}

    Inst* caseCatchLabelInst(Inst* inst) {return caseDefault(inst);}

    // method entry/exit
    Inst* caseMethodEntryLabel(Inst* inst) {return caseDefault(inst);}

    Inst* caseMethodEntry(Inst* inst) {return caseDefault(inst);}

    Inst* caseMethodEnd(Inst* inst) {return caseDefault(inst);}
    
    // source markers
    Inst* caseMethodMarker(Inst* inst) {return caseDefault(inst);}

    Inst* casePhi(Inst* inst){return caseDefault(inst);}

    Inst* caseTauPi(TauPiInst* inst){return caseDefault(inst);}

    Inst* caseIncCounter(Inst* inst) {return caseDefault(inst);}

    Inst* casePrefetch(Inst* inst) {return caseDefault(inst);}

    // compressed reference stuff
    Inst* caseUncompressRef(Inst* inst)  {
        Opnd* opnd = simplifyUncompressRef(inst->getSrc(0));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }

    Inst* caseCompressRef(Inst* inst)  {
        Opnd* opnd = simplifyCompressRef(inst->getSrc(0));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }

    Inst* caseLdFieldOffset(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdFieldOffsetPlusHeapbase(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdArrayBaseOffset(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdArrayBaseOffsetPlusHeapbase(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdArrayLenOffset(Inst* inst) {return caseDefault(inst);}

    Inst* caseLdArrayLenOffsetPlusHeapbase(Inst* inst) {return caseDefault(inst);}

    Inst* caseAddOffset(Inst* inst)  {
        Opnd* opnd = simplifyAddOffset(inst->getDst()->getType(),
                                       inst->getSrc(0),
                                       inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }

    Inst* caseAddOffsetPlusHeapbase(Inst* inst)  {
        Opnd* opnd = simplifyAddOffsetPlusHeapbase(inst->getDst()->getType(),
                                                   inst->getSrc(0),
                                                   inst->getSrc(1));
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }

    Inst* caseTauPoint(Inst* inst) {
        return inst;
    }

    Inst* caseTauEdge(Inst* inst) {
        return inst;
    }

    Inst* caseTauAnd(Inst* inst) {
        Opnd* opnd = simplifyTauAnd(inst->asMultiSrcInst());
        if (opnd != NULL)
            return opnd->getInst();
        return inst;
    }

    Inst* caseTauUnsafe(Inst* inst) {
        return inst;
    }

    Inst* caseTauSafe(Inst* inst) {
        return inst;
    }

    Inst* caseTauCheckCast(TypeInst* inst) {
        bool alwaysThrows = false;
        Opnd* opnd = simplifyTauCheckCast(inst->getSrc(0),
                                          inst->getSrc(1),
                                          inst->getTypeInfo(),
                                          alwaysThrows);
        if (opnd) {
            eliminateCheck(inst, alwaysThrows);
            return opnd->getInst();
        }
        return inst;
    }

    Inst* caseTauHasType(TypeInst* inst) {
        Opnd* opnd = simplifyTauHasType(inst->getSrc(0),
                                        inst->getTypeInfo());
        if (opnd)
            return opnd->getInst();
        return inst;
    }

    Inst* caseTauHasExactType(TypeInst* inst) {
        Opnd* opnd = simplifyTauHasExactType(inst->getSrc(0),
                                             inst->getTypeInfo());
        if (opnd)
            return opnd->getInst();
        return inst;
    }

    Inst* caseTauIsNonNull(Inst* inst) {
        Opnd* opnd = simplifyTauIsNonNull(inst->getSrc(0));
        if (opnd)
            return opnd->getInst();
        return inst;
    }
    
    Inst* caseIdentHC(Inst* inst) {
        return caseDefault(inst);
    }

    // default
    Inst* caseDefault(Inst* inst)  {
        return inst;
    }

private:
    // fold routines always produce a result, except for div by 0, which yields NULL.
    // currently only applicable to Int32 or Int64 operands
    Opnd* fold(Opcode op, Type*, ConstInst* srcInst1, ConstInst* srcInst2, bool is_signed);
    Opnd* fold(Opcode op, Type*, ConstInst* srcInst, bool is_signed);
    Opnd* foldComparison(ComparisonModifier, 
                         Type::Tag cmpType,
                         ConstInst* srcInst1, 
                         ConstInst* srcInst2);

    Opnd* foldConstByAddingToAddWithConstant(Type*, ConstInst*, Inst* addInst);
    Opnd* foldConstByAddingToSubWithConstant(Type*, ConstInst*, Inst* subInst);
    Opnd* foldConstMultiplyByAddWithConstant(Type*, ConstInst*, Inst* subInst);
    Opnd* foldConstMultiplyBySubWithConstant(Type*, ConstInst*, Inst* subInst);
    Opnd* foldConstMultiplyByMulWithConstant(Type*, ConstInst*, Inst* subInst);
    Opnd* foldNegOfMultiplyByConstant(Type*, Inst*);

    // simplifyOp routines produce NULL or a simplified result
    Opnd* simplifyAddWithNeg(Modifier, Type*, Opnd* src1, Opnd* src2);
    Opnd* simplifyAddViaReassociation(Type*, Opnd* src1, Opnd* src2);
    Opnd* simplifySubWithNeg(Modifier, Type*, Opnd* src1, Opnd* src2);
    Opnd* simplifySubViaReassociation(Type*, Opnd* src1, Opnd* src2);
    Opnd* simplifyMulViaReassociation(Type*, Opnd* src1, Opnd* src2);
    Opnd* simplifyMulWithNeg(Type*, Opnd* src1, Opnd* src2);
    Opnd* simplifyTauDivOfMul(Modifier mod, Type*, Opnd* src1, Opnd* src2,
                              Opnd* tauOpndsChecked);
    
    // SimplifyCmp routines have different interface to allow use in
    // folding Cmps and Branches.  Returns true if can simplify, and
    // writes in ref parameters for new Branch or Cmp instruction.
    bool simplifyCmpToCmp(Type::Tag, ComparisonModifier, Opnd*, Opnd*,
                          Type::Tag &newInstType, ComparisonModifier &newmod,
                          Opnd* &newSrc1, Opnd* &newSrc2);
    bool simplifyCmpToCmp(Type::Tag, ComparisonModifier, Opnd*,
                          Type::Tag &newInstType, ComparisonModifier &newmod,
                          Opnd* &newSrc1);
    // subroutines of simplifyCmpToCmp:
    // to partition cmp simplification somewhat
    // check for cmp(x+c1, c2) and the like.
    bool simplifyCmpOfAddOrSubC(Type::Tag, ComparisonModifier, 
                                Opnd*, Opnd*,
                                Type::Tag &newInstType, 
                                ComparisonModifier &newmod,
                                Opnd* &newSrc1, Opnd* &newSrc2);
    bool simplifyCmpOfAddC(Type::Tag, ComparisonModifier, 
                           Opnd* addOpnd, Opnd* otherOpnd,
                           Type::Tag &newInstType, 
                           ComparisonModifier &newmod,
                           Opnd* &newAddSrc, Opnd* &newOtherSrc,
                           bool swapped);
    bool simplifyCmpOfSubC(Type::Tag, ComparisonModifier, 
                           Opnd* subOpnd, Opnd* otherOpnd,
                           Type::Tag &newInstType, 
                           ComparisonModifier &newmod,
                           Opnd* &newSubSrc, Opnd* &newOtherSrc,
                           bool swapped);
    // check for cmp(cmp(x,y),c) and the like
    bool simplifyCmpOfCmp(Type::Tag, ComparisonModifier, Opnd*, Opnd*,
                          Type::Tag &newInstType, ComparisonModifier &newmod,
                          Opnd* &newSrc1, Opnd* &newSrc2);
    // check for cmp(cmp3(x,y),c) and the like
    bool simplifyCmpOfCmp3(Type::Tag, ComparisonModifier, Opnd*, Opnd*,
                           Type::Tag &newInstType, ComparisonModifier &newmod,
                           Opnd* &newSrc1, Opnd* &newSrc2);
    // end subroutines of simplifyCmpToCmp

    // given possible results for the Cmp3, yield the simplified branch
    // or cmp parameters, returning false if it can't be done.
    bool simplifyCmp3ByResult(Opnd *cmp3Opnd, 
                              bool canbe_1, // can be -1
                              bool canbe0,
                              bool canbe1,
                              Type::Tag &newInstType, 
                              ComparisonModifier &newmod,
                              Opnd* &newSrc1, Opnd* &newSrc2);

    friend struct OpndDepthCompare;
    U_32 getDepth(Opnd *src);

protected:
    bool            isLate;
    bool            tryReassoc;
    enum AssocKind { AssocNone, AssocConst, AssocDepth, AssocLoop } assocKind;
    friend class MulMethod;

    // re-association machinery
    friend class Reassociate;
    Opnd* simplifyAddViaReassociation2(Type* type, Opnd* src1, Opnd* src2);
    Opnd* simplifyNegViaReassociation2(Type* type, Opnd* src1);
    Opnd* simplifySubViaReassociation2(Type* type, Opnd* src1, Opnd *src2);
    Opnd* simplifyMulViaReassociation2(Type* type, Opnd* src1, Opnd* src2);
    Opnd* simplifyAddOffsetViaReassociation(Opnd* uncompBase, Opnd *offset);
    Opnd* simplifyAddOffsetPlusHeapbaseViaReassociation(Opnd *compBase, Opnd *offsetPlusHeapbase);
    Reassociate *theReassociate;
};

class SimplifierWithInstFactory : public Simplifier {
public:
    SimplifierWithInstFactory(IRManager&,bool latePass=false,
                  Reassociate *reassociate0=0);
    virtual Inst* optimizeInst(Inst* inst);
    U_32 simplifyControlFlowGraph();
protected:
    void insertInst(Inst* inst);
    void insertInstInHeader(Inst* inst);

    virtual Inst* genAdd(Type*, Modifier, Opnd* src1, Opnd* src2);
    virtual Inst* genSub(Type*, Modifier, Opnd* src1, Opnd* src2);
    virtual Inst* genNeg(Type*, Opnd* src);
    virtual Inst* genMul(Type*, Modifier, Opnd* src1, Opnd* src2);
    virtual Inst* genMulHi(Type*, Modifier, Opnd* src1, Opnd* src2);
    virtual Inst* genMin(Type*, Opnd* src1, Opnd* src2);
    virtual Inst* genMax(Type*, Opnd* src1, Opnd* src2);
    virtual Inst* genAbs(Type*, Opnd* src1);

    virtual Inst* genAnd(Type*, Opnd* src1, Opnd* src2);
    virtual Inst* genOr(Type*, Opnd* src1, Opnd* src2);
    virtual Inst* genXor(Type*, Opnd* src1, Opnd* src2);
    virtual Inst* genNot(Type*, Opnd* src1);
    virtual Inst* genSelect(Type*,
                            Opnd* src1, Opnd* src2, Opnd* src3);

    virtual Inst* genConv(Type*, Type::Tag toType, Modifier, 
                          Opnd* src1);
    virtual Inst* genConvZE(Type*, Type::Tag toType, Modifier, 
                          Opnd* src1);
    virtual Inst* genConvUnmanaged(Type*, Type::Tag toType, Modifier, 
                          Opnd* src1);

    virtual Inst* genShladd(Type*, Opnd *value,
                            Opnd* shiftAmount, Opnd* addTo);
    virtual Inst* genShl(Type*, Modifier,
                         Opnd* src1, Opnd* src2);
    virtual Inst* genShr(Type*, Modifier,
                         Opnd* src1, Opnd* src2);

    virtual Inst* genCmp(Type*, Type::Tag insttype, 
                         ComparisonModifier, Opnd* src1, Opnd* src2);
    virtual void genJump(LabelInst* label);
    virtual void genBranch(Type::Tag instType, ComparisonModifier mod, 
                           LabelInst* label, Opnd* src1, Opnd* src2);
    virtual void genBranch(Type::Tag instType, ComparisonModifier mod, 
                           LabelInst* label, Opnd* src1);
    virtual Inst* genDirectCall(MethodDesc*,
                                Type* returnType,
                                Opnd* tauNullCheckedFirstArg,
                                Opnd* typesChecked,
                                U_32 numArgs,
                                Opnd* args[]);

    virtual Inst* genLdConstant(I_32 val);
    virtual Inst* genLdConstant(int64 val);
    virtual Inst* genLdConstant(float val);
    virtual Inst* genLdConstant(double val);
    virtual Inst* genLdConstant(Type* type, ConstInst::ConstValue val);

    virtual Inst* genTauLdInd(Modifier mod, Type *dstType, Type::Tag type, Opnd *ptr,
                              Opnd *tauNonNullBase, Opnd *tauAddressInRange);
    virtual Inst* genLdRef(Modifier mod, Type *dstType,
                           U_32 token, MethodDesc *enclosingMethod);

    virtual Inst* genLdFunAddrSlot(MethodDesc*);
    virtual Inst* genGetVTableAddr(ObjectType* type);

    virtual Inst* genCompressRef(Opnd *uncompref);
    virtual Inst* genUncompressRef(Opnd *compref);

    virtual Inst* genLdFieldOffsetPlusHeapbase(FieldDesc*);
    virtual Inst* genLdArrayBaseOffsetPlusHeapbase(Type *elemType);
    virtual Inst* genLdArrayLenOffsetPlusHeapbase(Type *elemType);
    virtual Inst* genAddOffsetPlusHeapbase(Type *ptrType, Opnd *compRef, 
                                           Opnd *offsetPlusHeapbase);
    virtual Inst* genTauSafe();
    virtual Inst* genTauMethodSafe();
    virtual Inst* genTauUnsafe();
    virtual Inst* genTauStaticCast(Opnd *src, Opnd *tauNonNull, Type *castType);
    virtual Inst* genTauHasType(Opnd *src, Type *castType);
    virtual Inst* genTauHasExactType(Opnd *src, Type *castType);
    virtual Inst* genTauIsNonNull(Opnd *src);

    Opnd *simplifyStoreSrc(Opnd *, Type::Tag &typetag, Modifier &mod,
                           bool compressRef);

    virtual void genThrowSystemException(CompilationInterface::SystemExceptionId);

    virtual void  foldBranch(BranchInst* br, bool isTaken);
    virtual void  foldSwitch(SwitchInst* switchInst, U_32 index);
    virtual void  eliminateCheck(Inst* checkInst, bool alwaysThrows);
private:
    Inst*           nextInst;
    Node*        currentCfgNode;
    InstFactory&    instFactory;
    OpndManager&    opndManager;
    TypeManager&    typeManager;
    Opnd*           tauSafeOpnd;
    Opnd*           tauMethodSafeOpnd;
    Opnd*           tauUnsafeOpnd;
};

} //namespace Jitrino 

#endif // _SIMPLIFIER_H_
