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
 * @author Intel, Vyacheslav P. Shakin
 */

#ifndef _IA32_INST_SELECTOR_H
#define _IA32_INST_SELECTOR_H

#include "CodeGenIntfc.h"
#include "Ia32CodeSelector.h"
namespace Jitrino
{
namespace Ia32{

//================================================================================================
//  class InstCodeSelector
//================================================================================================
/** 
class VarGenerator is the Ia32 CG implementation of the 
::InstructionCallback interface 

InstCodeSelector is an instruction code selector which selects Ia32 LIR instructions 
for HIR instructions.

The class contains callbacks for conversion of each HIR instructions as well as 
a set of auxilary functions
*/
class InstCodeSelector : public InstructionCallback {
public:
    virtual void throwLinkingException(Class_Handle encClass, U_32 cp_ndx, U_32 opcode);

    static void onCFGInit(IRManager& irManager);

    InstCodeSelector(CompilationInterface&          compIntfc,
                        CfgCodeSelector&            codeSel,
                        IRManager&                  irM,
                        Node *                      currentBasicBlock
                        ); 
    
    virtual ~InstCodeSelector () {}

    //
    // Instructiosn that operate only on taus
    //
    virtual CG_OpndHandle*  tauPoint(); // tied to control flow reaching this point
    virtual CG_OpndHandle*  tauEdge(); // tied to previous branch
    virtual CG_OpndHandle*  tauAnd(U_32 numArgs, CG_OpndHandle** args);
    virtual CG_OpndHandle*  tauUnsafe(); // no dependence info; operation may depend on any branch
    virtual CG_OpndHandle*  tauSafe();   // operation is always safe

    // result is a predicate
    virtual CG_OpndHandle*  pred_czero(CompareZeroOp::Types,CG_OpndHandle* src);
    virtual CG_OpndHandle*  pred_cnzero(CompareZeroOp::Types,CG_OpndHandle* src);
    // END new tau instructions

    //
    // InstructionCallback methods
    //
    void   opndMaybeGlobal(CG_OpndHandle* opnd); 
    CG_OpndHandle* add(ArithmeticOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2);
    CG_OpndHandle* addRef(RefArithmeticOp::Types,CG_OpndHandle* refSrc,CG_OpndHandle* intSrc); 
    CG_OpndHandle* sub(ArithmeticOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2);
    CG_OpndHandle* subRef(RefArithmeticOp::Types,CG_OpndHandle* refSrc, CG_OpndHandle* intSrc);
    CG_OpndHandle* diffRef(bool ovf, CG_OpndHandle* ref1,CG_OpndHandle* ref2);
    CG_OpndHandle* mul(ArithmeticOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2);
    CG_OpndHandle* tau_div(DivOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2,CG_OpndHandle *tauSrc1NonZero);
    CG_OpndHandle* tau_rem(DivOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2,CG_OpndHandle *tauSrc2NonZero);
    CG_OpndHandle* neg(NegOp::Types,CG_OpndHandle* src);
    CG_OpndHandle* mulhi(MulHiOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2);
    CG_OpndHandle* min_op(NegOp::Types,CG_OpndHandle* src1, CG_OpndHandle* src2);
    CG_OpndHandle* max_op(NegOp::Types,CG_OpndHandle* src1, CG_OpndHandle* src2);
    CG_OpndHandle* abs_op(NegOp::Types,CG_OpndHandle* src);
    CG_OpndHandle* tau_ckfinite(CG_OpndHandle* src);
    CG_OpndHandle* and_(IntegerOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2);
    CG_OpndHandle* or_(IntegerOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2);
    CG_OpndHandle* xor_(IntegerOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2);
    CG_OpndHandle* not_(IntegerOp::Types,CG_OpndHandle* src);
    CG_OpndHandle* shladd(IntegerOp::Types,CG_OpndHandle* value,
                          U_32 shiftAmount, CG_OpndHandle* addto);
    CG_OpndHandle* shl(IntegerOp::Types,CG_OpndHandle* value,CG_OpndHandle* shiftAmount);
    CG_OpndHandle* shr(IntegerOp::Types,CG_OpndHandle* value,CG_OpndHandle* shiftAmount);
    CG_OpndHandle* shru(IntegerOp::Types,CG_OpndHandle* value,CG_OpndHandle* shiftAmount);
    CG_OpndHandle* select(CompareOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2,CG_OpndHandle* src3);
    CG_OpndHandle* cmp(CompareOp::Operators,CompareOp::Types, CG_OpndHandle* src1,CG_OpndHandle* src2, int ifNaNResult);
    CG_OpndHandle* czero(CompareZeroOp::Types opType,CG_OpndHandle* src);
    CG_OpndHandle* cnzero(CompareZeroOp::Types opType,CG_OpndHandle* src);
    void           branch(CompareOp::Operators,CompareOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2);
    void           bzero(CompareZeroOp::Types,CG_OpndHandle* src);
    void           bnzero(CompareZeroOp::Types,CG_OpndHandle* src);
    void           jump() { // do nothing
    }

    void           tableSwitch(CG_OpndHandle* src, U_32 nTargets);       
    void           throwException(CG_OpndHandle* exceptionObj, bool createStackTrace);
    void            throwSystemException(CompilationInterface::SystemExceptionId);
    CG_OpndHandle* convToInt(ConvertToIntOp::Types,bool isSigned, bool isZeroExtend,
                              ConvertToIntOp::OverflowMod,Type* dstType, CG_OpndHandle* src);
    CG_OpndHandle* convToFp(ConvertToFpOp::Types, Type* dstType, CG_OpndHandle* src);

    /// convert unmanaged pointer to object. Boxing
    CG_OpndHandle*  convUPtrToObject(ObjectType * dstType, CG_OpndHandle* val);
    /// convert object or integer to unmanaged pointer.
    CG_OpndHandle*  convToUPtr(PtrType * dstType, CG_OpndHandle* src);


    CG_OpndHandle* ldc_i4(I_32 val);
    CG_OpndHandle* ldc_i8(int64 val);
    CG_OpndHandle* ldc_s(float val);
    CG_OpndHandle* ldc_d(double val);
    CG_OpndHandle* ldnull(bool compressed);

    CG_OpndHandle* tau_checkNull(CG_OpndHandle* base, bool checksThisForInlinedMethod);
    CG_OpndHandle* tau_checkBounds(CG_OpndHandle* arrayLen, CG_OpndHandle *index);
    CG_OpndHandle* tau_checkLowerBound(CG_OpndHandle* a, CG_OpndHandle *b);
    CG_OpndHandle* tau_checkUpperBound(CG_OpndHandle* a, CG_OpndHandle *b);
    CG_OpndHandle* tau_checkElemType(CG_OpndHandle* array, CG_OpndHandle *src,
                                     CG_OpndHandle* tauNullChecked, CG_OpndHandle* tauIsArray);
    CG_OpndHandle* tau_checkZero(CG_OpndHandle* src);
    CG_OpndHandle* tau_checkDivOpnds(CG_OpndHandle* src1, CG_OpndHandle *src2);

    CG_OpndHandle* uncompressRef(CG_OpndHandle *compref);
    CG_OpndHandle* compressRef(CG_OpndHandle *ref);
    CG_OpndHandle* ldFieldOffset(FieldDesc *desc);
    CG_OpndHandle* ldFieldOffsetPlusHeapbase(FieldDesc *desc);
    CG_OpndHandle* ldArrayBaseOffset(Type *elemType);
    CG_OpndHandle* ldArrayLenOffset(Type *elemType);
    CG_OpndHandle* ldArrayBaseOffsetPlusHeapbase(Type *elemType);
    CG_OpndHandle* ldArrayLenOffsetPlusHeapbase(Type *elemType);
    CG_OpndHandle* addOffset(Type *pointerType, CG_OpndHandle* ref, 
                              CG_OpndHandle* fieldOffset);
    CG_OpndHandle* addOffsetPlusHeapbase(Type *pointerType, 
                                          CG_OpndHandle* compRef, 
                                          CG_OpndHandle* fieldOffsetPlusHeapbase);

    CG_OpndHandle* ldFieldAddr(Type* fieldRefType,CG_OpndHandle* base,FieldDesc *desc);
    CG_OpndHandle* ldStaticAddr(Type* fieldRefType,FieldDesc *desc); 
    CG_OpndHandle* ldElemBaseAddr(CG_OpndHandle *array);
    CG_OpndHandle* addElemIndex(Type*, CG_OpndHandle *elemBase,CG_OpndHandle* index);
    CG_OpndHandle* addElemIndexWithLEA(Type*, CG_OpndHandle *elemBase,CG_OpndHandle* index);
    CG_OpndHandle* ldElemAddr(CG_OpndHandle* array,CG_OpndHandle* index) {
        return addElemIndex(NULL,ldElemBaseAddr(array),index);
    }
    CG_OpndHandle* tau_ldInd(Type* dstType, CG_OpndHandle* ptr, Type::Tag memType, 
                         bool autoUncompressRef,bool speculative,
                         CG_OpndHandle* tauBaseNonNull,
                         CG_OpndHandle* tauAddressInRange);
    CG_OpndHandle* ldStatic(Type *dstType, FieldDesc *desc, Type::Tag fieldType, bool autoUncompressRef);
    CG_OpndHandle* tau_ldField(Type *dstType,CG_OpndHandle* base, Type::Tag fieldType,
                               FieldDesc *desc, bool autoUncompressRef, 
                               CG_OpndHandle* tauBaseNonNull, CG_OpndHandle* tauBaseTypeHasField);
    CG_OpndHandle* tau_ldElem(Type *dstType, CG_OpndHandle* array, CG_OpndHandle* index, 
                          bool autoUncompressRef, 
                          CG_OpndHandle* tauBaseNonNull, CG_OpndHandle* tauIdxIsInBounds);
    CG_OpndHandle* ldVarAddr(U_32 varId);
    CG_OpndHandle* ldVar(Type* dstType, U_32 varId);
    CG_OpndHandle* tau_arrayLen(Type* dstType,ArrayType* arrayType, Type* lenType, CG_OpndHandle* base,
                            CG_OpndHandle* tauArrayNonNull, CG_OpndHandle* tauIsArray);
    void           tau_stInd(CG_OpndHandle* src, CG_OpndHandle* ptr, Type::Tag memType, 
                             bool autoCompressRef, CG_OpndHandle* tauBaseNonNull,
                             CG_OpndHandle* tauAddressInRange, CG_OpndHandle* tauElemTypeChecked);
    void           tau_stStatic(CG_OpndHandle* src, FieldDesc *desc, Type::Tag fieldType, 
                                bool autoCompressRef, CG_OpndHandle* tauFieldTypeChecked);
    void           tau_stField(CG_OpndHandle*src,CG_OpndHandle* base, Type::Tag fieldType,
                               FieldDesc *desc, bool autoCompressRef,                                        
                               CG_OpndHandle* tauBaseNonNull, CG_OpndHandle* tauBaseTypeHasField,
                               CG_OpndHandle* tauFieldHasType);
    void           tau_stElem(CG_OpndHandle* src,CG_OpndHandle* array, CG_OpndHandle* index,
                          bool autoCompressRef, CG_OpndHandle* tauBaseNonNull,
                          CG_OpndHandle* tauAddressInRange, CG_OpndHandle* tauElemTypeChecked);
    void           tau_stRef(CG_OpndHandle* src, CG_OpndHandle* ptr, CG_OpndHandle* base, Type::Tag memType, 
                             bool autoCompressRef, CG_OpndHandle* tauBaseNonNull,
                             CG_OpndHandle* tauAddressInRange, CG_OpndHandle* tauElemTypeChecked);

    void           stVar(CG_OpndHandle* src, U_32 varId);
    CG_OpndHandle* newObj(ObjectType* objType);     
    CG_OpndHandle* newArray(ArrayType* arrayType, CG_OpndHandle* numElems);
    CG_OpndHandle* newMultiArray(ArrayType* arrayType, U_32 numDims, CG_OpndHandle** dims);
    CG_OpndHandle* ldRef(Type *dstType, MethodDesc* enclosingMethod,U_32 stringToken, bool uncompress);
    
    void           incCounter(Type *counterType,U_32 counter);
    void           ret();
    void           ret(CG_OpndHandle* returnValue);

    CG_OpndHandle* defArg(U_32 position, Type *type);
    CG_OpndHandle* ldFunAddr(Type* dstType, MethodDesc *desc);
    CG_OpndHandle* tau_ldVirtFunAddr(Type* dstType, CG_OpndHandle* vtableAddr, 
                                MethodDesc *desc, CG_OpndHandle *tauVtableHasDesc);
    CG_OpndHandle* tau_ldVTableAddr(Type *dstType, CG_OpndHandle* base, CG_OpndHandle *tauBaseNonNull);
    CG_OpndHandle* getVTableAddr(Type *dstType, ObjectType *base);
    CG_OpndHandle* getClassObj(Type *dstType, ObjectType *base);
    CG_OpndHandle* tau_ldIntfTableAddr(Type *dstType, CG_OpndHandle* base,NamedType* vtableTypeDesc);
    CG_OpndHandle* calli(U_32 numArgs,CG_OpndHandle** args, Type* retType,
                         CG_OpndHandle* methodPtr);
    CG_OpndHandle* tau_calli(U_32 numArgs,CG_OpndHandle** args, Type* retType,
                             CG_OpndHandle* methodPtr, CG_OpndHandle* nonNullFirstArgTau,
                             CG_OpndHandle* tauTypesChecked);
    CG_OpndHandle* call(U_32 numArgs, CG_OpndHandle** args, Type* retType, MethodDesc *desc);
    CG_OpndHandle* tau_call(U_32 numArgs, CG_OpndHandle** args, Type* retType,
                            MethodDesc *desc, CG_OpndHandle *nonNullFirstArgTau,
                            CG_OpndHandle *tauTypesChecked);
    CG_OpndHandle* tau_callvirt(U_32 numArgs,CG_OpndHandle** args, Type* retType, MethodDesc *desc,             CG_OpndHandle* tauNullChecked, CG_OpndHandle* tauTypesChecked);
    CG_OpndHandle* callhelper(U_32 numArgs, CG_OpndHandle** args, Type* retType,JitHelperCallOp::Id callId);
    CG_OpndHandle* callvmhelper(U_32 numArgs, CG_OpndHandle** args, Type* retType,
                                VM_RT_SUPPORT callId);
    
    CG_OpndHandle* box(ObjectType * boxedType, CG_OpndHandle* val);
    CG_OpndHandle* unbox(Type * dstType, CG_OpndHandle* objHandle);
    CG_OpndHandle* ldValueObj(Type* objType, CG_OpndHandle *srcAddr);
    void           stValueObj(CG_OpndHandle *dstAddr, CG_OpndHandle *src);
    void           initValueObj(Type* objType, CG_OpndHandle *objAddr);
    void           copyValueObj(Type* objType, CG_OpndHandle *dstAddr, CG_OpndHandle *srcAddr);
    void           tau_monitorEnter(CG_OpndHandle* obj, CG_OpndHandle* tauIsNonNull);
    void           tau_monitorExit(CG_OpndHandle* obj, CG_OpndHandle* tauIsNonNull);
    CG_OpndHandle* ldLockAddr(CG_OpndHandle* obj);
    CG_OpndHandle* tau_balancedMonitorEnter(CG_OpndHandle* obj, CG_OpndHandle* lockAddr,
                                        CG_OpndHandle* tauIsNonNull);
    void           balancedMonitorExit(CG_OpndHandle* obj, CG_OpndHandle* lockAddr, 
                                       CG_OpndHandle* oldLock);
    CG_OpndHandle* tau_optimisticBalancedMonitorEnter(CG_OpndHandle* obj, CG_OpndHandle* lockAddr,
                                                  CG_OpndHandle* tauIsNonNull);
    void           optimisticBalancedMonitorExit(CG_OpndHandle* obj, CG_OpndHandle* lockAddr, 
                                                 CG_OpndHandle* oldLock);
    void           incRecursionCount(CG_OpndHandle* obj, CG_OpndHandle* oldLock) {
        assert(0);
    }
    void           monitorEnterFence(CG_OpndHandle* obj);
    void           monitorExitFence(CG_OpndHandle* obj);
    void           typeMonitorEnter(NamedType *type);
    void           typeMonitorExit(NamedType *type);
    CG_OpndHandle* tau_staticCast(ObjectType *toType, CG_OpndHandle* obj,
                                  CG_OpndHandle* tauIsType);
    CG_OpndHandle* tau_cast(ObjectType *toType, CG_OpndHandle* obj,
                            CG_OpndHandle* tauCheckedNull);
    CG_OpndHandle* tau_checkCast(ObjectType *toType, CG_OpndHandle* obj,
                                 CG_OpndHandle* tauCheckedNull);
    CG_OpndHandle* tau_asType(ObjectType *type, CG_OpndHandle* obj,
                              CG_OpndHandle* tauCheckedNull);
    CG_OpndHandle* tau_instanceOf(ObjectType *type, CG_OpndHandle* obj,
                                  CG_OpndHandle* tauCheckedNull);
    void           initType(Type* type);
    CG_OpndHandle* catchException(Type * exceptionType);
    CG_OpndHandle* copy(CG_OpndHandle *src);
    void prefetch(CG_OpndHandle *addr);

    void pseudoInst();

    void methodEntry(MethodDesc* mDesc);
    void methodEnd(MethodDesc* mDesc, CG_OpndHandle* retOpnd = NULL);

    //
    //  Set/clear current persistent id to be assigned to the generated instructions
    //
    void           setCurrentPersistentId(PersistentInstructionId persistentId) {
        currPersistentId = persistentId;
    }
    void           clearCurrentPersistentId() {
        currPersistentId = PersistentInstructionId();
    }
    //
    //  Additional instruction selection methods
    //
    void   genSwitchDispatch(U_32 numTargets,Opnd *switchSrc);
    void   genReturn();
    CG_OpndHandle* runtimeHelperCall(U_32 numArgs, CG_OpndHandle** args,
                      Type *retType, 
                      VM_RT_SUPPORT helper,
                      Type *typeArgument,
                      Opnd * nonNullFirstArgTau);
    void genExitHelper(Opnd* retOpnd, MethodDesc* meth);

    //
    //  Block information instructions
    //
    bool             endsWithReturn() {return seenReturn;}
    bool             endsWithSwitch() {return switchSrcOpnd != NULL;}
    Opnd *           getSwitchSrc() {
        assert(switchSrcOpnd != NULL);
        return switchSrcOpnd;
    }
    U_32           getSwitchNumTargets() {
        assert(switchNumTargets > 0);
        return switchNumTargets;
    }
    //
    // Set current HIR instruction in order to allow Code Generator propagate bc offset info
    //
    virtual void   setCurrentHIRInstBCOffset(uint16 val) { currentHIRInstBCOffset =  val; }
    virtual uint16 getCurrentHIRInstBCOffset() const { return currentHIRInstBCOffset; }
private: 
    //
    // info about current HIR instruction bytecode offset 
    //
    uint16 currentHIRInstBCOffset;

    Opnd *  convertIntToInt(Opnd * srcOpnd, Type * dstType, Opnd * dstOpnd=NULL, bool isZeroExtend=false);
    Opnd *  convertIntToFp(Opnd * srcOpnd, Type * dstType, Opnd * dstOpnd=NULL);
    Opnd *  convertFpToInt(Opnd * srcOpnd, Type * dstType, Opnd * dstOpnd=NULL);
    Opnd *  convertFpToFp(Opnd * srcOpnd, Type * dstType, Opnd * dstOpnd=NULL);
    Opnd*   convertUnmanagedPtr(Opnd * srcOpnd, Type * dstType, Opnd * dstOpnd=NULL);
    Opnd*   convertToUnmanagedPtr(Opnd * srcOpnd, Type * dstType, Opnd * dstOpnd=NULL, bool isZeroExtend=false);
    
    bool    isIntegerType(Type * type)
    { return type->isInteger()||type->isBoolean()||type->isChar(); }
    void    copyOpnd(Opnd *dst, Opnd *src, bool doZeroExtension=false);
    void    copyOpndTrivialOrTruncatingConversion(Opnd *dst, Opnd *src);

    Opnd * convert(CG_OpndHandle * oph, Type * dstType, Opnd * dstOpnd=NULL, bool isZeroExtend=false);

    Opnd * simpleOp_I8(Mnemonic mn, Type * dstType, Opnd * src1, Opnd * src2);

    Opnd * simpleOp_I4(Mnemonic mn, Type * dstType, Opnd * src1, Opnd * src2);
    
    Opnd * fpOp(Mnemonic mn, Type * dstType, Opnd * src1, Opnd * src2); 

    Opnd * createResultOpnd(Type * dstType);

    Opnd * divOp(DivOp::Types   op, bool rem, Opnd * src1, Opnd * src2);

    Opnd * minMaxOp(NegOp::Types   opType, bool max, Opnd * src1, Opnd * src2);

    Opnd * shiftOp(IntegerOp::Types opType, Mnemonic mn, Opnd * value, Opnd * shiftAmount);

    bool cmpToEflags(CompareOp::Operators cmpOp, CompareOp::Types opType,
                                    Opnd * src1, Opnd * src2
                                    );
    // zero or HeapBase depending on compression mode
    Opnd* zeroForComparison(Opnd* target);
    // immediate or general opnd with heapBase value
    Opnd* heapBaseOpnd(Type* type, POINTER_SIZE_INT heapBase);

    //
    // Enums
    //
    enum XmulKind {XmulKind_Low, XmulKind_High, XmulKind_HighUnsigned};
    //
    // Methods
    //
    Inst *           appendInsts(Inst *inst);

    MethodDesc *     getMethodDesc() { return codeSelector.methodCodeSelector.getMethodDesc();  }

    MemoryManager& getCodeSelectorMemoryManager(){ return codeSelector.methodCodeSelector.codeSelectorMemManager; }
    
    Type * getMethodReturnType() { return getMethodDesc()->getReturnType(); }

    Opnd *              sxtInt32(Opnd *opnd);
    Opnd *              zxtInt32(Opnd *opnd);
    void                sxtOneInt32(Opnd*& opnd1, Opnd*& opnd2);
    void                zxtOneInt32(Opnd*& opnd1, Opnd*& opnd2);
    bool                opndIsFoldableImm(Opnd * opnd, U_32  nbits,
                                          I_32& imm);
    Opnd *          doSetf(Opnd * opnd);
    Type *              divOpType(DivOp::Types op);
    Type *              integerOpType(IntegerOp::Types opType);
    Opnd *          addOffset(Opnd * addr, Opnd * base, U_32 offset);
    Opnd*           buildOffset(U_32 offset, MemoryAttribute::Context context);
    Opnd*           buildOffsetPlusHeapbase(U_32 offset, MemoryAttribute::Context context);
    Opnd*           addVtableBaseAndOffset(Opnd *addr, CG_OpndHandle* compPtr, U_32 offset);
    void                simpleMemCopy(Opnd *dst, Opnd *src, OpndSize size,
                                      Opnd *dstBaseTau, Opnd *dstOffsetTau,
                                      Opnd *srcBaseTau, Opnd *srcOffsetTau);
    void                copyContext(Opnd * dst, Opnd * src);
    void                copyBase(Opnd * dst, Opnd * src);
    void                decompressOpnd(Opnd *dst, Opnd *src);
    void                compressOpnd(Opnd *dst, Opnd *src);
    void                makeComparable(Opnd*& srcOpnd1, Opnd*& srcOpnd2);
    CG_OpndHandle*      simpleLdInd(Type * dstType, Opnd *addr, Type::Tag memType,
                                    Opnd *baseTau, Opnd *offsetTau);
    void                simpleStInd(Opnd *addr, Opnd *src, Type::Tag memType, 
                                    bool autoCompressRef, Opnd *baseTau, Opnd *offsetAndTypeTau);
    Type *              getFieldRefType(Type *dstType, Type::Tag memType);
    void                simplifyTypeTag(Type::Tag& tag,Type *ptr);
    Opnd **         createReturnArray(Type * retType, U_32& numRegRet);
    void                assignCallArgs(CallInst *call, U_32 numArgs, CG_OpndHandle **args, 
                                       Type *retType, bool isHelperCall);
    CG_OpndHandle*      assignCallRet(CallInst *call, Type *retType, bool isHelperCall);
    Opnd *           doInt32Divide(Opnd * fA, Opnd * fB);
    Opnd *           doSingleDivide(Opnd * fA, Opnd * fB);
    Opnd *           doDoubleDivide(Opnd * fA, Opnd * fB);
    Opnd *           doExtendedDivide(Opnd * fA, Opnd * fB, bool isIntDivide);
    void                checkConvOverflow(bool srcIsSigned,
                                          bool dstIsSigned,
                                          ConvertToIntOp::Types opType,
                                          CG_OpndHandle* src);
    CG_OpndHandle *            genTauSplit(BranchInst * br);

    Type *              getRuntimeIdType() {return typeManager.getUnmanagedPtrType(typeManager.getIntPtrType());}

    //  Check if we should generate tau instructions
    bool                suppressTauInsts(){ return true; }; 
    //
    //  Synchronization fence flag
    //
    enum SyncFenceFlag {
        SyncFenceFlag_EnforceEnterFence = 0x1,
        SyncFenceFlag_EnforceExitFence  = 0x2
    };
    U_32       getSyncFenceFlag();
    //
    //  Set/clear current predicate operand to be assigned to the generated instructions
    //
    void           setCurrentPredOpnd(Opnd * p) {currPredOpnd = p;}
    void           clearCurrentPredOpnd() {currPredOpnd = NULL;}
    //
    //  Operand factory
    //
    Opnd *         createIntReg(Type * type);
    Opnd *         createFloatReg(Type * type);

    CG_OpndHandle*    getTauUnsafe()const
    {   return (CG_OpndHandle*)&_tauUnsafe; }

    //
    // Fields
    //
    CompilationInterface&           compilationInterface;
    CfgCodeSelector&                codeSelector;
    IRManager&                      irManager;
    TypeManager&                    typeManager;
    MemoryManager                   memManager; // for local data
    Node*                           currentBasicBlock;
    Opnd **                         vars;
    U_32                          inArgPos;
    int                             persistentId;
#ifdef _DEBUG
    U_32                          nextInArg;
#endif
    bool                            seenReturn;
    Opnd *                          switchSrcOpnd;
    U_32                          switchNumTargets;
    PersistentInstructionId         currPersistentId;
    Opnd *                          currPredOpnd;
    static VM_RT_SUPPORT
                                 divOpHelperIds[],
                                 remOpHelperIds[];
    static U_32 _tauUnsafe;
};


}}; // namespace Ia32

#endif // _IA32_INST_SELECTOR_h
