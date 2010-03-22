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
 *
 */

#ifndef _CODEGENINTFC_H_
#define _CODEGENINTFC_H_

#include "open/types.h"
#include "Type.h"
#include "Jitrino.h"
#include "VMInterface.h"
#include "Stl.h"



namespace Jitrino
{

    // struct ::JitFrameContext;    
    class CG_OpndHandle {
    };


// "_Ovf" types are used to support CLI's overflow semantics.  CLI supports 
// overflow arithmetic.  If an overflow type is used below, generated code must
// test for and throw an exception if an overflow occurs.

class ArithmeticOp {
public:
    enum Types {
        I4, I4_Ovf, U4_Ovf,
        I8, I8_Ovf, U8_Ovf,
        I,  I_Ovf,  U_Ovf,
        F,  S,        D
    };
};

class RefArithmeticOp {
public:
    enum Types {
        I4, I, U4_Ovf, U_Ovf
    };
};

class IntegerOp {
public:
    enum Types {
        I4,
        I8,
        I
    };
};

class DivOp {
public:
    enum Types {
        I4,    U4,
        I8,    U8,
        I,     U,
        F,  S,  D
    };
};

class MulHiOp {
public:
    enum Types {
        I4, U4, I8, U8, I, U
    };
};

class NegOp {
public:
    enum Types {
        I4,
        I8,
        I,
        F, S, D
    };
};

class CompareOp {
public:
    enum Types {
        I4,
        I8,
        I,
        F, S, D,
        Ref,
        CompRef
    };
    enum Operators {
        Eq,             // equal        
        Ne,             // int: not equal; 
                        // fp: not equal or unordered
        Gt,             // greater than
        Gtu,            // int: greater than unsigned; 
                        // fp: greater than or unordered
        Ge,             // greater than or equal
        Geu             // int: greater than or equal unsigned; 
        // fp: greater than or equal or unordered
    };
};

class CompareZeroOp {
public:
    enum Types {
        I4,
        I8,
        I,
        Ref,
        CompRef
    };
};

class ConvertToIntOp {
public:
    enum Types {
        I1, I2, I4, I8, I
    };
    enum OverflowMod {
        NoOvf,
        SignedOvf,
        UnsignedOvf
    };
};

class ConvertToFpOp {
public:
    enum Types {
        Single,
        Double,
        FloatFromUnsigned
    };
};

class JitHelperCallOp {
public:
    enum Id {
        Prefetch,
        Memset0,
        InitializeArray,
        FillArrayWithConst,
        SaveThisState,
        ReadThisState,
        LockedCompareAndExchange,
        AddValueProfileValue,
        ArrayCopyDirect,
        ArrayCopyReverse,
        StringCompareTo,
        StringRegionMatches,
        StringIndexOf
    };
};

class InstructionCallback {
public:
    virtual ~InstructionCallback() {}
    virtual void            opndMaybeGlobal(CG_OpndHandle* opnd) = 0;

    // tau generating instructions
    virtual CG_OpndHandle*  tauPoint() = 0; // depends on all preceding branches
    virtual CG_OpndHandle*  tauEdge() = 0; // tied to incoming edge
    virtual CG_OpndHandle*  tauAnd(U_32 numArgs, CG_OpndHandle** args) = 0;
    virtual CG_OpndHandle*  tauUnsafe() = 0; // have lost dependence info, don't move uses
    virtual CG_OpndHandle*  tauSafe() = 0;   // operation is always safe

    virtual CG_OpndHandle*  add(ArithmeticOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2) = 0;
    virtual CG_OpndHandle*  addRef(RefArithmeticOp::Types,CG_OpndHandle* refSrc,CG_OpndHandle* intSrc) = 0;
    virtual CG_OpndHandle*  sub(ArithmeticOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2) = 0;
    virtual CG_OpndHandle*  subRef(RefArithmeticOp::Types,CG_OpndHandle* refSrc, CG_OpndHandle* intSrc) = 0;
    virtual CG_OpndHandle*  diffRef(bool ovf, CG_OpndHandle* ref1,CG_OpndHandle* ref2) = 0;
    virtual CG_OpndHandle*  mul(ArithmeticOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2) = 0;
    virtual CG_OpndHandle*  tau_div(DivOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2,
                                    CG_OpndHandle *tauSrc1NonZero) = 0;
    virtual CG_OpndHandle*  tau_rem(DivOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2,
                                    CG_OpndHandle *tauSrc2NonZero) = 0;
    virtual CG_OpndHandle*  neg(NegOp::Types,CG_OpndHandle* src) = 0;
    virtual CG_OpndHandle*  mulhi(MulHiOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2) = 0;
    virtual CG_OpndHandle*  min_op(NegOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2) = 0;
    virtual CG_OpndHandle*  max_op(NegOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2) = 0;
    virtual CG_OpndHandle*  abs_op(NegOp::Types,CG_OpndHandle* src1) = 0;
    virtual CG_OpndHandle*  tau_ckfinite(CG_OpndHandle* src) = 0;
    virtual CG_OpndHandle*  and_(IntegerOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2) = 0;
    virtual CG_OpndHandle*  or_(IntegerOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2) = 0;
    virtual CG_OpndHandle*  xor_(IntegerOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2) = 0;
    virtual CG_OpndHandle*  not_(IntegerOp::Types,CG_OpndHandle* src) = 0;
    virtual CG_OpndHandle*  shladd(IntegerOp::Types,CG_OpndHandle* value,
                                   U_32 shiftamount,
                                   CG_OpndHandle* addto) = 0;
    virtual CG_OpndHandle*  shl(IntegerOp::Types,CG_OpndHandle* value,CG_OpndHandle* shiftAmount) = 0;
    virtual CG_OpndHandle*  shr(IntegerOp::Types,CG_OpndHandle* value,CG_OpndHandle* shiftAmount) = 0;
    virtual CG_OpndHandle*  shru(IntegerOp::Types,CG_OpndHandle* value,CG_OpndHandle* shiftAmount) = 0;
    virtual CG_OpndHandle*  select(CompareOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2,
                                   CG_OpndHandle* src3) = 0;
    // BEGIN PRED DEPRECATED
    virtual CG_OpndHandle*  cmp(CompareOp::Operators,CompareOp::Types, CG_OpndHandle* src1,CG_OpndHandle* src2,int ifNaNResult=0) = 0;
    virtual CG_OpndHandle*  cmp3(CompareOp::Operators,CompareOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2) { return 0; };
    virtual CG_OpndHandle*  czero(CompareZeroOp::Types,CG_OpndHandle* src) = 0;
    virtual CG_OpndHandle*  cnzero(CompareZeroOp::Types,CG_OpndHandle* src) = 0;
    // END PRED DEPRECATED

    // result is a predicate
    virtual CG_OpndHandle*  pred_czero(CompareZeroOp::Types,CG_OpndHandle* src) = 0;
    virtual CG_OpndHandle*  pred_cnzero(CompareZeroOp::Types,CG_OpndHandle* src) = 0;

    // BEGIN PRED DEPRECATED
    virtual void            branch(CompareOp::Operators,CompareOp::Types,CG_OpndHandle* src1,CG_OpndHandle* src2) = 0;
    virtual void            bzero(CompareZeroOp::Types,CG_OpndHandle* src) = 0;
    virtual void            bnzero(CompareZeroOp::Types,CG_OpndHandle* src) = 0;
    // END PRED DEPRECATED

    virtual void            jump() = 0;
    virtual void            tableSwitch(CG_OpndHandle* src, U_32 nTargets) = 0;       
    virtual void            throwException(CG_OpndHandle* exceptionObj, bool createStackTrace) = 0;
    virtual void            throwSystemException(CompilationInterface::SystemExceptionId) = 0;
    virtual void            throwLinkingException(Class_Handle encClass, U_32 cp_ndx, U_32 opcode) = 0;

    /// convert unmanaged pointer to object. Boxing
    virtual CG_OpndHandle*  convUPtrToObject(ObjectType * dstType, CG_OpndHandle* val) = 0;
    /// convert object or integer to unmanaged pointer. Unboxing
    virtual CG_OpndHandle*  convToUPtr(PtrType * dstType, CG_OpndHandle* op) = 0;

    virtual CG_OpndHandle*  convToInt(ConvertToIntOp::Types, bool isSigned, bool isZeroExtend, 
                                      ConvertToIntOp::OverflowMod,
                                      Type* dstType, CG_OpndHandle* src) = 0;
    virtual CG_OpndHandle*  convToFp(ConvertToFpOp::Types, Type* dstType, CG_OpndHandle* src) = 0;

    virtual CG_OpndHandle*  ldFunAddr(Type* dstType, MethodDesc *desc) = 0; 
    virtual CG_OpndHandle*  tau_ldVirtFunAddr(Type* dstType, CG_OpndHandle* vtableAddr, 
                                              MethodDesc *desc,
                                              CG_OpndHandle *tauVtableHasDesc) = 0;
    virtual CG_OpndHandle*  tau_ldVTableAddr(Type *dstType, CG_OpndHandle* base,
                                             CG_OpndHandle *tauBaseNonNull) = 0;
    virtual CG_OpndHandle*  getVTableAddr(Type *dstType, ObjectType *base) = 0;
    virtual CG_OpndHandle*  getClassObj(Type *dstType, ObjectType *base) = 0;
    virtual CG_OpndHandle*  tau_ldIntfTableAddr(Type *dstType, CG_OpndHandle* base, 
                                                NamedType* vtableType) = 0;
    virtual CG_OpndHandle*  call(U_32 numArgs, CG_OpndHandle** args, Type* retType,
                                 MethodDesc *desc) = 0;
    virtual CG_OpndHandle*  tau_call(U_32 numArgs, CG_OpndHandle** args, Type* retType,
                                     MethodDesc *desc,
                                     CG_OpndHandle *tauNullChecked,
                                     CG_OpndHandle *tauTypesChecked) = 0;
    // for callvirt this reference is in args[0]
    virtual CG_OpndHandle*  tau_callvirt(U_32 numArgs, CG_OpndHandle** args, Type* retType,
                                         MethodDesc *desc, CG_OpndHandle* tauNullChecked,
                                         CG_OpndHandle* tauTypesChecked) = 0;
    virtual CG_OpndHandle*  tau_calli(U_32 numArgs,CG_OpndHandle** args, Type* retType,
                                      CG_OpndHandle* methodPtr, 
                                      CG_OpndHandle* tauNullChecked,
                                      CG_OpndHandle* tauTypesChecked) = 0;
    virtual CG_OpndHandle*  callhelper(U_32 numArgs, CG_OpndHandle** args, Type* retType,
                                       JitHelperCallOp::Id callId) = 0;
    virtual CG_OpndHandle*  callvmhelper(U_32 numArgs, CG_OpndHandle** args, Type* retType,
                                       VM_RT_SUPPORT callId) = 0;

    virtual CG_OpndHandle*  ldc_i4(I_32 val) = 0;
    virtual CG_OpndHandle*  ldc_i8(int64 val) = 0;
    virtual CG_OpndHandle*  ldc_s(float val) = 0;
    virtual CG_OpndHandle*  ldc_d(double val) = 0;
    virtual CG_OpndHandle*  ldnull(bool compressed) = 0;

    // result of each of these is now a tau
    // if a negative result is needed, use a tau_point() on the exception handler
    virtual CG_OpndHandle*  tau_checkNull(CG_OpndHandle* base, bool checksThisForInlinedMethod) = 0;
    virtual CG_OpndHandle*  tau_checkBounds(CG_OpndHandle* arrayLen, CG_OpndHandle *index) = 0;
    virtual CG_OpndHandle*  tau_checkLowerBound(CG_OpndHandle* a, CG_OpndHandle *b) = 0; // throw if (a > b), unsigned if pointer
    virtual CG_OpndHandle*  tau_checkUpperBound(CG_OpndHandle* a, CG_OpndHandle *b) = 0; // throw if (a >= b)
    virtual CG_OpndHandle*  tau_checkElemType(CG_OpndHandle* array, CG_OpndHandle *src,
                                              CG_OpndHandle* tauNullChecked,
                                              CG_OpndHandle* tauIsArray) = 0;
    virtual CG_OpndHandle*  tau_checkZero(CG_OpndHandle* src) = 0;
    virtual CG_OpndHandle*  tau_checkDivOpnds(CG_OpndHandle* src1, CG_OpndHandle *src2) = 0;

    virtual CG_OpndHandle*  tau_checkCast(ObjectType *toType, CG_OpndHandle* obj,
                                          CG_OpndHandle* tauCheckedNull) = 0; // for lowered cast

    // *** begin 32-bit pointers:
    virtual CG_OpndHandle*  uncompressRef(CG_OpndHandle *compref) = 0;
    virtual CG_OpndHandle*  compressRef(CG_OpndHandle *ref) = 0;

    // yields an Offset
    virtual CG_OpndHandle*  ldFieldOffset(FieldDesc *desc) = 0;
    // yields an OffsetPlusHeapbase
    virtual CG_OpndHandle*  ldFieldOffsetPlusHeapbase(FieldDesc *desc) = 0;

    // yields an Offset
    virtual CG_OpndHandle*  ldArrayBaseOffset(Type *elemType) = 0;
    // yields an Offset
    virtual CG_OpndHandle*  ldArrayLenOffset(Type *elemType) = 0;
    // yields an OffsetPlusHeapbase
    virtual CG_OpndHandle*  ldArrayBaseOffsetPlusHeapbase(Type *elemType) = 0;
    // yields an OffsetPlusHeapbase
    virtual CG_OpndHandle*  ldArrayLenOffsetPlusHeapbase(Type *elemType) = 0;

    // takes an uncompressed reference ref and an Offset fieldOffset,
    // yields a managed pointer of type fieldRefType:
    virtual CG_OpndHandle*  addOffset(Type *pointerType, CG_OpndHandle* ref, 
                                      CG_OpndHandle* fieldOffset) = 0;
    // takes a compressed reference compRef and an OffsetPlusHeapbase, 
    // yields a managed pointer of type fieldRefType:
    virtual CG_OpndHandle*  addOffsetPlusHeapbase(Type *pointerType, 
                                                  CG_OpndHandle* compRef, 
                                                  CG_OpndHandle* fieldOffsetPlusHeapbase) = 0;

    // *** end 32-bit pointers

    // COMPRESSED_PTR note: if we are using compressed references, and
    // field is a reference, then result type should be
    // Ptr<CompressedRef>; otherwise, it is Ptr<Ref>.
    virtual CG_OpndHandle*  ldFieldAddr(Type* fieldRefType,CG_OpndHandle* base,FieldDesc *desc) = 0;
    virtual CG_OpndHandle*  ldStaticAddr(Type* fieldRefType,FieldDesc *desc) = 0;
    virtual CG_OpndHandle*  ldElemBaseAddr(CG_OpndHandle* array) = 0;
    virtual CG_OpndHandle*  addElemIndex(Type*, CG_OpndHandle *elemBase,CG_OpndHandle* index) = 0;
    virtual CG_OpndHandle*  addElemIndexWithLEA(Type*, CG_OpndHandle *elemBase,CG_OpndHandle* index) = 0;
    virtual CG_OpndHandle*  ldElemAddr(CG_OpndHandle* array,CG_OpndHandle* index) = 0;
    // COMPRESSED_PTR note: 
    // if we are using compressed references, and ptr is Ptr<CompressedRef>, then
    // if autoUncompressRef, then result is uncompressed by CG and is of type Ref
    // otherwise, result is of type CompressedRef and optimizer will handle it
    // TAU note:
    //   tau_addressInRange = (for array reference) tau_isarray && tau_inbounds
    //                        (for field reference) tau_hastype
    //                        (for static reference) special AlwaysTrueTau
    //                        (for speculative reference) special AlwaysFalseTau
    //                        (for array length) chknull tau
    virtual CG_OpndHandle*  tau_ldInd(Type* dstType, CG_OpndHandle* ptr, Type::Tag memType,
                                      bool autoUncompressRef, bool speculate,
                                      CG_OpndHandle* tauBaseNonNull,
                                      CG_OpndHandle* tauaddressInRange) = 0;
    // COMPRESSED_PTR note: similar here, except field type determines behavior
    virtual CG_OpndHandle*  ldStatic(Type *dstType, FieldDesc *desc, Type::Tag fieldType,
                                     bool autoUncompressRef) = 0;
    virtual CG_OpndHandle*  tau_ldField(Type *dstType, CG_OpndHandle* base, Type::Tag fieldType,
                                        FieldDesc *desc, bool autoUncompressRef,
                                        CG_OpndHandle* tauBaseNonNull,
                                        CG_OpndHandle* tauBaseTypeHasField) = 0;
    virtual CG_OpndHandle*  tau_ldElem(Type *dstType, CG_OpndHandle* array, CG_OpndHandle* index,
                                       bool autoUncompressRef,
                                       CG_OpndHandle* tauBaseNonNull,
                                       CG_OpndHandle* tauIdxIsInBounds) = 0;
    // COMPRESSED_PTR note: var is already uncompressed, so compression doesn't affect these:
    virtual CG_OpndHandle*  ldVarAddr(U_32 varId) = 0;
    virtual CG_OpndHandle*  ldVar(Type* dstType, U_32 varId) = 0;
    virtual CG_OpndHandle*  tau_arrayLen(Type* dstType, ArrayType* arrayType, Type* lenType,
                                         CG_OpndHandle* array,
                                         CG_OpndHandle* tauArrayNonNull,
                                         CG_OpndHandle* tauIsArray) = 0;

    // COMPRESSED_PTR note: If we are using compressed references, and
    // ptr is Ptr<Compressed Ref>, then
    //   (1) if autoCompressRef, then src should be Ref and CG will compress 
    //       it on store; type is uncompressedRef type
    //   (2) if !autoCompressRef, then src should be CompressedRef; type is 
    //       compressedRef type
    virtual void            tau_stInd(CG_OpndHandle* src, CG_OpndHandle* ptr, Type::Tag memType,
                                      bool autoCompressRef,
                                      CG_OpndHandle* tauBaseNonNull,
                                      CG_OpndHandle* tauAddressInRange,
                                      CG_OpndHandle* tauElemTypeChecked) = 0;
    // COMPRESSED_PTR note: similar here
    virtual void            tau_stStatic(CG_OpndHandle* src, FieldDesc *desc, Type::Tag fieldType,
                                         bool autoCompressRef,
                                         CG_OpndHandle* tauFieldTypeChecked) = 0; 
    virtual void            tau_stField(CG_OpndHandle* src, CG_OpndHandle* base, Type::Tag fieldType,
                                        FieldDesc *desc, bool autoCompress,
                                        CG_OpndHandle* tauBaseNonNull,
                                        CG_OpndHandle* tauBaseTypeHasField,
                                        CG_OpndHandle* tauFieldTypeChecked) = 0;
    virtual void            tau_stElem(CG_OpndHandle* src, CG_OpndHandle* array, 
                                       CG_OpndHandle* index,
                                       bool autoCompress,
                                       CG_OpndHandle* tauBaseNonNull,
                                       CG_OpndHandle* tauAddressInRange,
                                       CG_OpndHandle* tauElemTypeChecked) = 0;
    virtual void            tau_stRef(CG_OpndHandle* src, CG_OpndHandle* ptr, CG_OpndHandle* base, Type::Tag memType,
                                       bool autoCompressRef, 
                                       CG_OpndHandle* tauBaseNonNull,
                                       CG_OpndHandle* tauAddressInRange, 
                                       CG_OpndHandle* tauElemTypeChecked) = 0;
    // COMPRESSED_PTR note: var is already uncompressed, so compression doesn't affect it
    virtual void            stVar(CG_OpndHandle* src, U_32 varId) = 0;

    virtual CG_OpndHandle*  newObj(ObjectType* objType) = 0; 
    virtual CG_OpndHandle*  newArray(ArrayType* arrayType, CG_OpndHandle* numElems) = 0;
    virtual CG_OpndHandle*  newMultiArray(ArrayType* arrayType, U_32 numDims, CG_OpndHandle** dims) = 0;
    virtual CG_OpndHandle*  ldRef(Type* type,MethodDesc* enclosingMethod,U_32 stringToken, bool autouncompress) = 0;
    virtual void            incCounter(Type *counterType,U_32 counter) = 0;

    virtual void            ret() = 0;
    virtual void            ret(CG_OpndHandle* returnValue) = 0;
    virtual CG_OpndHandle*  defArg(U_32 position,Type *type) = 0;

    virtual void            tau_monitorEnter(CG_OpndHandle* obj, 
                                             CG_OpndHandle* tauIsNonNull) = 0;
    virtual void            tau_monitorExit(CG_OpndHandle* obj,
                                            CG_OpndHandle* tauIsNonNull) = 0;
    virtual CG_OpndHandle*  ldLockAddr(CG_OpndHandle* obj) = 0;
    virtual CG_OpndHandle*  tau_balancedMonitorEnter(CG_OpndHandle* obj, CG_OpndHandle* lockAddr,
                                                     CG_OpndHandle* tauIsNonNull) = 0;
    // null-check dependence is threaded through oldLock from tau_balancedMonitorEnter
    virtual void            balancedMonitorExit(CG_OpndHandle* obj, CG_OpndHandle* lockAddr, 
                                                CG_OpndHandle* oldLoc) = 0;
    virtual CG_OpndHandle*  tau_optimisticBalancedMonitorEnter(CG_OpndHandle* obj, 
                                                               CG_OpndHandle* lockAddr,
                                                               CG_OpndHandle* tauIsNonNull) = 0;
    // null-check dependence is threaded through oldLock from tau_optimisticBalancedMonitorEnter
    virtual void            optimisticBalancedMonitorExit(CG_OpndHandle* obj, 
                                                          CG_OpndHandle* lockAddr, 
                                                          CG_OpndHandle* oldLoc) = 0;
    // null-check dependence is threaded through oldLock from tau_optimisticBalancedMonitorEnter
    virtual void            incRecursionCount(CG_OpndHandle* obj, CG_OpndHandle* oldLock) = 0;
    virtual void            monitorEnterFence(CG_OpndHandle* obj) = 0;
    virtual void            monitorExitFence(CG_OpndHandle* obj) = 0;
    virtual void            typeMonitorEnter(NamedType *type) = 0;
    virtual void            typeMonitorExit(NamedType *type) = 0;
    
    virtual CG_OpndHandle*  tau_staticCast(ObjectType *toType, CG_OpndHandle* obj,
                                           CG_OpndHandle* tauIsType) = 0;
    virtual CG_OpndHandle*  tau_cast(ObjectType *toType, CG_OpndHandle* obj,
                                     CG_OpndHandle* tauCheckedNull) = 0;
    virtual CG_OpndHandle*  tau_asType(ObjectType* type, CG_OpndHandle* obj,
                                       CG_OpndHandle* tauCheckedNull) = 0;
    virtual CG_OpndHandle*  tau_instanceOf(ObjectType *type, CG_OpndHandle* obj,
                                           CG_OpndHandle* tauCheckedNull) = 0;
    virtual void            initType(Type* type) = 0;
    virtual CG_OpndHandle*  box(ObjectType * dstType, CG_OpndHandle* val) = 0;
    virtual CG_OpndHandle*  unbox(Type * dstType, CG_OpndHandle* obj) = 0;
    virtual CG_OpndHandle*  ldValueObj(Type* objType, CG_OpndHandle *srcAddr) = 0;
    virtual void            stValueObj(CG_OpndHandle *dstAddr, CG_OpndHandle *src) = 0;
    virtual void            initValueObj(Type* objType, CG_OpndHandle *objAddr) = 0;
    virtual void            copyValueObj(Type* objType, CG_OpndHandle *dstAddr, CG_OpndHandle *srcAddr) = 0;
    virtual CG_OpndHandle*  copy(CG_OpndHandle *src) = 0;
    virtual CG_OpndHandle*  catchException(Type * exceptionType) = 0;
    virtual void prefetch(CG_OpndHandle *addr) = 0;

    virtual void pseudoInst() = 0;

    virtual void methodEntry(MethodDesc* mDesc) = 0;
    virtual void methodEnd(MethodDesc* mDesc, CG_OpndHandle* retVallue = NULL) = 0;

    // Set the current persistent instruction id associated with any subsequently generated instructions.
    virtual void            setCurrentPersistentId(PersistentInstructionId persistentId) = 0;

    // Clear the current persistent instruction id.  
    // Any subsequently generated instructions have no associated ID.
    virtual void            clearCurrentPersistentId() = 0;
    // Set current HIR instruction bytecode offset
    virtual void   setCurrentHIRInstBCOffset(uint16 val) = 0; 
    virtual uint16 getCurrentHIRInstBCOffset() const = 0; 
private:
};

//
// interface for generating code for a variable
//
class VarCodeSelector {
public:
    virtual ~VarCodeSelector() {}
    class Callback {
    public:
        virtual ~Callback() {}
        virtual U_32 defVar(Type* varType,bool isAddressTaken,bool isPinned) = 0;
        virtual void setManagedPointerBase(U_32 managedPtrVarNum, U_32 baseVarNum) = 0;
    };
    virtual void genCode(Callback&) = 0;
};

//
// interface for generating code for a basic block
//
class BlockCodeSelector {
public:
    virtual ~BlockCodeSelector() {}
    virtual void genCode(InstructionCallback&) = 0;
};

//
// interface for generating code for a control-flow graph
//
class CFGCodeSelector {
public:
    virtual ~CFGCodeSelector() {}
    class Callback {
    public:
        enum BlockKind {Prolog, InnerBlock, Epilog};
        virtual ~Callback() {}
        virtual U_32  genDispatchNode(U_32 numInEdges,U_32 numOutEdges, const StlVector<MethodDesc*>& inlineEndMarkers, double cnt) = 0;
        virtual U_32  genBlock(U_32 numInEdges,U_32 numOutEdges, BlockKind blockKind,
                                 BlockCodeSelector&, double cnt) = 0;
        virtual U_32  genUnwindNode(U_32 numInEdges, U_32 numOutEdges,double cnt) = 0;
        virtual U_32  genExitNode(U_32 numInEdges, double cnt) = 0;
        virtual void    genUnconditionalEdge(U_32 tailNodeId,U_32 headNodeId,double prob) = 0;
        virtual void    genTrueEdge(U_32 tailNodeId,U_32 headNodeId,double prob) = 0;
        virtual void    genFalseEdge(U_32 tailNodeId,U_32 headNodeId, double prob) = 0;
        virtual void    genSwitchEdges(U_32 tailNodeId, U_32 numTargets, 
                                       U_32 *targets, double *probs, U_32 defaultTarget) = 0;
        virtual void    genExceptionEdge(U_32 tailNodeId, U_32 headNodeId, double prob) = 0;
        virtual void    genCatchEdge(U_32 tailNodeId,U_32 headNodeId,
                                     U_32 priority,Type* exceptionType, double prob) = 0;
        
        // Set the persistent block ID for a given block.
        virtual void    setPersistentId(U_32 nodeId, U_32 persistentId) = 0;
    };
    virtual void genCode(Callback&) = 0;
};
//
// interface for generating code for a method
//
class MethodCodeSelector {
public:
    MethodCodeSelector() {}
    virtual ~MethodCodeSelector() {}
    class Callback {
    public:
        virtual void    genVars(U_32 numLocals,VarCodeSelector&) = 0;
        virtual void    setMethodDesc(MethodDesc * desc) = 0;
        virtual void    genCFG(U_32 numNodes,CFGCodeSelector&,bool useProfile) = 0;
        virtual ~Callback() {}
    };
    virtual void selectCode(Callback&) = 0;
};

class SessionAction;
class CodeGenerator {
public:
    virtual ~CodeGenerator() {}
    virtual void genCode(SessionAction* sa, MethodCodeSelector&) = 0;
};

class CodeGeneratorFactory {
public:
    virtual ~CodeGeneratorFactory() {}
    virtual CodeGenerator* getCodeGenerator(MemoryManager &mm, 
                                            CompilationInterface& compInterface) = 0;
};

}
#endif // _CODEGENINTFC_H_
