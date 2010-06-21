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

#ifndef _IPF_CODE_SELECTOR_H_
#define _IPF_CODE_SELECTOR_H_

#include "CodeGenIntfc.h"
#include "VMInterface.h"
#include "IpfCfg.h"
#include "IpfOpndManager.h"

namespace Jitrino {
namespace IPF {

#define NOT_IMPLEMENTED_C(name) { IPF_ERR << name << " INSTRUCTION NOT IMPLEMENTED" << endl; return NULL; }
#define NOT_IMPLEMENTED_V(name) { IPF_ERR << name << " INSTRUCTION NOT IMPLEMENTED" << endl; }

//========================================================================================//
// IpfMethodCodeSelector
//========================================================================================//

class IpfMethodCodeSelector : public MethodCodeSelector::Callback {
public:
                         IpfMethodCodeSelector(Cfg&, CompilationInterface&);
    MethodDesc           *getMethodDesc();

    void                 genVars(U_32, VarCodeSelector&);
    void                 setMethodDesc(MethodDesc*);
    void                 genCFG(U_32, CFGCodeSelector&, bool);
//    void                 setProfileInfo(CodeProfiler*) {}
    virtual              ~IpfMethodCodeSelector() {}

protected:
    MemoryManager        &mm;
    Cfg                  &cfg;
    CompilationInterface &compilationInterface;
    MethodDesc           *methodDesc;
    OpndVector           opnds;
    NodeVector           nodes;
};

//========================================================================================//
// IpfVarCodeSelector
//========================================================================================//

class IpfVarCodeSelector : public VarCodeSelector::Callback {
public:
                  IpfVarCodeSelector(Cfg&, OpndVector&);
    U_32        defVar(Type*, bool, bool);
    void          setManagedPointerBase(U_32, U_32);

protected:
    MemoryManager &mm;
    Cfg           &cfg;
    OpndVector    &opnds;
    OpndManager   *opndManager;
};

//========================================================================================//
// IpfCfgCodeSelector
//========================================================================================//

class IpfCfgCodeSelector : public CFGCodeSelector::Callback {
public:
                         IpfCfgCodeSelector(Cfg&, NodeVector&, OpndVector&, CompilationInterface&);
    U_32               genDispatchNode(U_32, U_32, const StlVector<MethodDesc*>&, double);
    U_32               genBlock(U_32, U_32, BlockKind, BlockCodeSelector&, double);
    U_32               genUnwindNode(U_32, U_32, double);
    U_32               genExitNode(U_32, double);
    void                 genUnconditionalEdge(U_32, U_32, double);
    void                 genTrueEdge(U_32, U_32, double);
    void                 genFalseEdge(U_32, U_32, double);
    void                 genSwitchEdges(U_32, U_32, U_32*, double*, U_32);
    void                 genExceptionEdge(U_32, U_32, double);
    void                 genCatchEdge(U_32, U_32, U_32, Type*, double);
    void                 genExitEdge(U_32, U_32, double);
    void                 setLoopInfo(U_32, bool, bool, U_32);
    void                 setPersistentId(U_32, U_32);
    virtual              ~IpfCfgCodeSelector() {}

protected:
    MemoryManager        &mm;
    Cfg                  &cfg;
    NodeVector           &nodes;
    OpndVector           &opnds;
    CompilationInterface &compilationInterface;
    OpndManager          *opndManager;
};

//========================================================================================//
// IpfInstCodeSelector
//========================================================================================//

class IpfInstCodeSelector : public InstructionCallback {

public:
    IpfInstCodeSelector(Cfg&, BbNode&, OpndVector&, CompilationInterface&);

    //---------------------------------------------------------------------------//
    // Arithmetic
    //---------------------------------------------------------------------------//

    CG_OpndHandle *add(ArithmeticOp::Types, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *sub(ArithmeticOp::Types, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *mul(ArithmeticOp::Types, CG_OpndHandle*, CG_OpndHandle*);

    CG_OpndHandle *addRef(RefArithmeticOp::Types, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *subRef(RefArithmeticOp::Types, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *diffRef(bool, CG_OpndHandle*, CG_OpndHandle*);

    CG_OpndHandle *tau_div(DivOp::Types, CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *tau_rem(DivOp::Types, CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*);

    CG_OpndHandle *neg    (NegOp::Types, CG_OpndHandle*);
    CG_OpndHandle *min_op (NegOp::Types, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *max_op (NegOp::Types, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *abs_op (NegOp::Types, CG_OpndHandle*);

    CG_OpndHandle *and_  (IntegerOp::Types, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *or_   (IntegerOp::Types, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *xor_  (IntegerOp::Types, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *not_  (IntegerOp::Types, CG_OpndHandle*);
    CG_OpndHandle *shl   (IntegerOp::Types, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *shr   (IntegerOp::Types, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *shru  (IntegerOp::Types, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *shladd(IntegerOp::Types, CG_OpndHandle*, U_32, CG_OpndHandle*);

    CG_OpndHandle *convToInt(ConvertToIntOp::Types, bool, bool, ConvertToIntOp::OverflowMod, Type*, CG_OpndHandle*);
    CG_OpndHandle *convToFp(ConvertToFpOp::Types, Type*, CG_OpndHandle*);

    CG_OpndHandle *ldc_i4(I_32);
    CG_OpndHandle *ldc_i8(int64);
    CG_OpndHandle *ldc_s(float);
    CG_OpndHandle *ldc_d(double);
    CG_OpndHandle *ldnull(bool);
    CG_OpndHandle *ldVar(Type*, U_32);
    void          stVar(CG_OpndHandle*, U_32);
    CG_OpndHandle *defArg(U_32, Type*);

    CG_OpndHandle *cmp   (CompareOp::Operators, CompareOp::Types, CG_OpndHandle*, CG_OpndHandle*, int);
    CG_OpndHandle *czero (CompareZeroOp::Types, CG_OpndHandle*);
    CG_OpndHandle *cnzero(CompareZeroOp::Types, CG_OpndHandle*);

    CG_OpndHandle *copy(CG_OpndHandle*);
    CG_OpndHandle *tau_staticCast(ObjectType*, CG_OpndHandle*, CG_OpndHandle*);

    //---------------------------------------------------------------------------//
    // Branch & Call
    //---------------------------------------------------------------------------//

    void          branch(CompareOp::Operators, CompareOp::Types, CG_OpndHandle*, CG_OpndHandle*);
    void          bzero (CompareZeroOp::Types, CG_OpndHandle*);
    void          bnzero(CompareZeroOp::Types, CG_OpndHandle*);
    void          tableSwitch(CG_OpndHandle*, U_32);       

    CG_OpndHandle *call(U_32, CG_OpndHandle**, Type*, MethodDesc*);
    CG_OpndHandle *tau_call(U_32, CG_OpndHandle**, Type*, MethodDesc*, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *tau_calli(U_32,CG_OpndHandle**, Type*, CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*);
    void          ret();
    void          ret(CG_OpndHandle*);

    //---------------------------------------------------------------------------//
    // Exceptions & checks
    //---------------------------------------------------------------------------//

    void          throwException(CG_OpndHandle*, bool);
    void          throwException(ObjectType* excType);// generater code to throw noted type exception
    void          throwSystemException(CompilationInterface::SystemExceptionId);
    void          throwLinkingException(Class_Handle, U_32, U_32);
    CG_OpndHandle *catchException(Type*);

    CG_OpndHandle *tau_checkNull(CG_OpndHandle *, bool);
    CG_OpndHandle *tau_checkBounds(CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *tau_checkLowerBound(CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *tau_checkUpperBound(CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *tau_checkElemType(CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *tau_checkZero(CG_OpndHandle*);
    CG_OpndHandle *tau_checkCast(ObjectType*, CG_OpndHandle*, CG_OpndHandle*);

    //---------------------------------------------------------------------------//
    // Memory operations
    //---------------------------------------------------------------------------//

    void          tau_stInd(CG_OpndHandle*, CG_OpndHandle*, Type::Tag, bool, CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *tau_ldInd(Type*, CG_OpndHandle*, Type::Tag, bool, bool, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *ldString(MethodDesc*, U_32, bool);
    CG_OpndHandle *ldLockAddr(CG_OpndHandle*);
    CG_OpndHandle *tau_ldVirtFunAddr(Type*, CG_OpndHandle*, MethodDesc*, CG_OpndHandle*);
    CG_OpndHandle *tau_ldVTableAddr(Type*, CG_OpndHandle*, CG_OpndHandle*);
    CG_OpndHandle *getVTableAddr(Type*, ObjectType*);
    CG_OpndHandle *getClassObj(Type*, ObjectType*);
    CG_OpndHandle *ldFieldAddr(Type*, CG_OpndHandle*, FieldDesc*);
    CG_OpndHandle *ldStaticAddr(Type*, FieldDesc*);

    CG_OpndHandle *tau_instanceOf(ObjectType*, CG_OpndHandle*, CG_OpndHandle*);
    void          initType(Type*);
    CG_OpndHandle *newObj(ObjectType*); 
    CG_OpndHandle *newArray(ArrayType*, CG_OpndHandle*);
    CG_OpndHandle *newMultiArray(ArrayType*, U_32, CG_OpndHandle**);
    CG_OpndHandle *ldElemBaseAddr(CG_OpndHandle*);
    CG_OpndHandle *addElemIndex(Type *, CG_OpndHandle *, CG_OpndHandle *);
    CG_OpndHandle *tau_arrayLen(Type*, ArrayType*, Type*, CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*);

    //---------------------------------------------------------------------------//
    // Monitors
    //---------------------------------------------------------------------------//

    void          tau_monitorEnter(CG_OpndHandle*, CG_OpndHandle*);
    void          tau_monitorExit (CG_OpndHandle*, CG_OpndHandle*);
    void          typeMonitorEnter(NamedType*);
    void          typeMonitorExit (NamedType*);
    CG_OpndHandle *tau_balancedMonitorEnter(CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*);
    void          balancedMonitorExit     (CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*);

    //---------------------------------------------------------------------------//
    // Methods that are not used (but implemented)
    //---------------------------------------------------------------------------//

    CG_OpndHandle *tauPoint()                      { return opndManager->getTau(); }
    CG_OpndHandle *tauEdge()                       { return opndManager->getTau(); }
    CG_OpndHandle *tauUnsafe()                     { return opndManager->getTau(); }
    CG_OpndHandle *tauSafe()                       { return opndManager->getTau(); }
    CG_OpndHandle *tauAnd(U_32, CG_OpndHandle**) { return opndManager->getTau(); }
    void          opndMaybeGlobal(CG_OpndHandle* opnd)            {}
    void          setCurrentPersistentId(PersistentInstructionId) {}
    void          clearCurrentPersistentId()                      {}
    void          setCurrentHIRInstBCOffset(uint16)               {}
    uint16        getCurrentHIRInstBCOffset() const         {return 0xFFFF;}

    //---------------------------------------------------------------------------//
    // Methods that are not going to be implemented
    //---------------------------------------------------------------------------//

    CG_OpndHandle *mulhi(MulHiOp::Types, CG_OpndHandle*, CG_OpndHandle*)   { NOT_IMPLEMENTED_C("mulhi"); }
    CG_OpndHandle *pred_czero (CompareZeroOp::Types, CG_OpndHandle*)       { NOT_IMPLEMENTED_C("pred_czero") }
    CG_OpndHandle *pred_cnzero(CompareZeroOp::Types, CG_OpndHandle*)       { NOT_IMPLEMENTED_C("pred_cnzero") }
    CG_OpndHandle *tau_checkDivOpnds(CG_OpndHandle*, CG_OpndHandle*)       { NOT_IMPLEMENTED_C("tau_checkDivOpnds") }
    CG_OpndHandle *ldFunAddr(Type*, MethodDesc*)                           { NOT_IMPLEMENTED_C("ldFunAddr") } 
    CG_OpndHandle *uncompressRef(CG_OpndHandle *compref)                   { NOT_IMPLEMENTED_C("uncompressRef") }
    CG_OpndHandle *compressRef(CG_OpndHandle *ref)                         { NOT_IMPLEMENTED_C("compressRef") }
    CG_OpndHandle *ldFieldOffset(FieldDesc*)                               { NOT_IMPLEMENTED_C("ldFieldOffset") }
    CG_OpndHandle *ldFieldOffsetPlusHeapbase(FieldDesc*)                   { NOT_IMPLEMENTED_C("ldFieldOffsetPlusHeapbase") }
    CG_OpndHandle *ldArrayBaseOffset(Type*)                                { NOT_IMPLEMENTED_C("ldArrayBaseOffset") }
    CG_OpndHandle *ldArrayLenOffset(Type*)                                 { NOT_IMPLEMENTED_C("ldArrayLenOffset") }
    CG_OpndHandle *ldArrayBaseOffsetPlusHeapbase(Type*)                    { NOT_IMPLEMENTED_C("ldArrayBaseOffsetPlusHeapbase") }
    CG_OpndHandle *ldArrayLenOffsetPlusHeapbase(Type*)                     { NOT_IMPLEMENTED_C("ldArrayLenOffsetPlusHeapbase") }
    CG_OpndHandle *addOffset(Type*, CG_OpndHandle*, CG_OpndHandle*)        { NOT_IMPLEMENTED_C("addOffset") }
    CG_OpndHandle *ldElemAddr(CG_OpndHandle*,CG_OpndHandle*)               { NOT_IMPLEMENTED_C("ldElemAddr") }
    CG_OpndHandle *ldStatic(Type*, FieldDesc*, Type::Tag, bool)            { NOT_IMPLEMENTED_C("ldStatic") }
    CG_OpndHandle *ldVarAddr(U_32)                                       { NOT_IMPLEMENTED_C("ldVarAddr") }
    CG_OpndHandle *ldToken(Type*, MethodDesc*, U_32)                     { NOT_IMPLEMENTED_C("ldToken") }
    CG_OpndHandle *tau_cast(ObjectType*, CG_OpndHandle*, CG_OpndHandle*)   { NOT_IMPLEMENTED_C("tau_cast") }
    CG_OpndHandle *tau_asType(ObjectType*, CG_OpndHandle*, CG_OpndHandle*) { NOT_IMPLEMENTED_C("tau_asType") }
    CG_OpndHandle *box(ObjectType*, CG_OpndHandle*)                        { NOT_IMPLEMENTED_C("box") }
    CG_OpndHandle *unbox(Type*, CG_OpndHandle*)                            { NOT_IMPLEMENTED_C("unbox") }
    CG_OpndHandle *ldValueObj(Type*, CG_OpndHandle*)                       { NOT_IMPLEMENTED_C("ldValueObj") }
    CG_OpndHandle *tau_ckfinite(CG_OpndHandle*)                            { NOT_IMPLEMENTED_C("tau_ckfinite") }
    CG_OpndHandle *callhelper(U_32, CG_OpndHandle**, Type*, JitHelperCallOp::Id) { NOT_IMPLEMENTED_C("callhelper") }
    CG_OpndHandle *tau_callvirt(U_32, CG_OpndHandle**, Type*, MethodDesc*, CG_OpndHandle*, CG_OpndHandle*)  { NOT_IMPLEMENTED_C("tau_callvirt") }
    CG_OpndHandle *select(CompareOp::Types, CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*) { NOT_IMPLEMENTED_C("select") }
    CG_OpndHandle *cmp3(CompareOp::Operators,CompareOp::Types, CG_OpndHandle*, CG_OpndHandle*) { NOT_IMPLEMENTED_C("cmp3") }
    CG_OpndHandle *tau_optimisticBalancedMonitorEnter(CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*) { NOT_IMPLEMENTED_C("tau_optimisticBalancedMonitorEnter") }
    CG_OpndHandle *addOffsetPlusHeapbase(Type*, CG_OpndHandle*, CG_OpndHandle*) { NOT_IMPLEMENTED_C("addOffsetPlusHeapbase") }
    CG_OpndHandle *tau_ldField(Type*, CG_OpndHandle*, Type::Tag, FieldDesc*, bool, CG_OpndHandle*, CG_OpndHandle*) { NOT_IMPLEMENTED_C("tau_ldField") }
    CG_OpndHandle *tau_ldElem(Type*, CG_OpndHandle*, CG_OpndHandle*, bool, CG_OpndHandle*, CG_OpndHandle*) { NOT_IMPLEMENTED_C("tau_ldElem") }
    void          incCounter(Type*, U_32)                               { NOT_IMPLEMENTED_V("incCounter") }
    void          incRecursionCount(CG_OpndHandle*, CG_OpndHandle*)       { NOT_IMPLEMENTED_V("incRecursionCount") }
    void          monitorEnterFence(CG_OpndHandle*)                       { NOT_IMPLEMENTED_V("monitorEnterFence") }
    void          monitorExitFence(CG_OpndHandle*)                        { NOT_IMPLEMENTED_V("monitorExitFence") }
    void          stValueObj(CG_OpndHandle*, CG_OpndHandle*)              { NOT_IMPLEMENTED_V("stValueObj") }
    void          initValueObj(Type*, CG_OpndHandle*)                     { NOT_IMPLEMENTED_V("initValueObj") }
    void          copyValueObj(Type*, CG_OpndHandle*, CG_OpndHandle*)     { NOT_IMPLEMENTED_V("copyValueObj") }
    void          prefetch(CG_OpndHandle*)                                { NOT_IMPLEMENTED_V("prefetch") }
    void          jump()                                                  { NOT_IMPLEMENTED_V("jump") }
    void          throwLazyException(U_32, CG_OpndHandle**, MethodDesc*) { NOT_IMPLEMENTED_V("throwLazyException") }
    void          tau_stStatic(CG_OpndHandle*, FieldDesc*, Type::Tag, bool, CG_OpndHandle*) { NOT_IMPLEMENTED_V("tau_stStatic") } 
    void          tau_stField(CG_OpndHandle*, CG_OpndHandle*, Type::Tag, FieldDesc*, bool, CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*) { NOT_IMPLEMENTED_V("tau_stField") }
    void          tau_stElem(CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*, bool, CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*) { NOT_IMPLEMENTED_V("tau_stElem") }
    void          optimisticBalancedMonitorExit(CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*) { NOT_IMPLEMENTED_V("optimisticBalancedMonitorExit") }

    //---------------------------------------------------------------------------//
    // TRANSITION
    //---------------------------------------------------------------------------//

    CG_OpndHandle* callvmhelper(U_32, CG_OpndHandle**, Type*
                        , VM_RT_SUPPORT) { NOT_IMPLEMENTED_C("unbox") }

    CG_OpndHandle* convUPtrToObject(ObjectType*, CG_OpndHandle*)              { NOT_IMPLEMENTED_C("convUPtrToObject") }
    CG_OpndHandle* convToUPtr(PtrType*, CG_OpndHandle*)                       { NOT_IMPLEMENTED_C("convToUPtr") }
    CG_OpndHandle *tau_ldIntfTableAddr(Type*, CG_OpndHandle*, NamedType*, CG_OpndHandle*) { NOT_IMPLEMENTED_C("tau_ldIntfTableAddr"); }
    CG_OpndHandle* tau_ldIntfTableAddr(Type*, CG_OpndHandle*, NamedType*);
    CG_OpndHandle* addElemIndexWithLEA(Type*, CG_OpndHandle*, CG_OpndHandle*) { NOT_IMPLEMENTED_C("addElemIndexWithLEA") }
    CG_OpndHandle* ldRef(Type*, MethodDesc*, unsigned int, bool); 
    void           pseudoInst()                                               {}
    void           methodEntry(MethodDesc*);
    void           methodEnd(MethodDesc*, CG_OpndHandle*);
    void           tau_stRef(CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*, Type::Tag, bool, CG_OpndHandle*, CG_OpndHandle*, CG_OpndHandle*) { NOT_IMPLEMENTED_V("tau_stRef") }

    //---------------------------------------------------------------------------//
    // convertors from HLO to IPF::CG types
    //---------------------------------------------------------------------------//

    static DataKind  toDataKind(IntegerOp::Types type);
    static DataKind  toDataKind(Type::Tag tag);                    
    static OpndKind  toOpndKind(Type::Tag tag);                    
    static DataKind  toDataKind(NegOp::Types type);
    static OpndKind  toOpndKind(NegOp::Types type);
    static DataKind  toDataKind(ArithmeticOp::Types type);
    static OpndKind  toOpndKind(ArithmeticOp::Types type);
    static DataKind  toDataKind(RefArithmeticOp::Types type);
    static DataKind  toDataKind(ConvertToIntOp::Types type, bool isSigned);
    static OpndKind  toOpndKind(DivOp::Types type);
    static DataKind  toDataKind(DivOp::Types type);
    static InstCode  toInstCmp(CompareOp::Types type);
    static InstCode  toInstCmp(CompareZeroOp::Types type);
    static Completer toCmpltCrel(CompareOp::Operators cmpOp, bool isFloating);
    static Completer toCmpltSz(DataKind dataKind);

protected:

    // Create new inst and add it in current node
    void      addInst(Inst* inst);
    Inst&     addNewInst(InstCode instCode_, 
                  CG_OpndHandle *op1=NULL, CG_OpndHandle *op2=NULL, CG_OpndHandle *op3=NULL, 
                  CG_OpndHandle *op4=NULL, CG_OpndHandle *op5=NULL, CG_OpndHandle *op6=NULL);
    Inst&     addNewInst(InstCode instCode_, Completer comp1,
                  CG_OpndHandle *op1=NULL, CG_OpndHandle *op2=NULL, CG_OpndHandle *op3=NULL, 
                  CG_OpndHandle *op4=NULL, CG_OpndHandle *op5=NULL, CG_OpndHandle *op6=NULL);
    Inst&     addNewInst(InstCode instCode_, Completer comp1, Completer comp2,
                  CG_OpndHandle *op1=NULL, CG_OpndHandle *op2=NULL, CG_OpndHandle *op3=NULL, 
                  CG_OpndHandle *op4=NULL, CG_OpndHandle *op5=NULL, CG_OpndHandle *op6=NULL);
    Inst&     addNewInst(InstCode instCode_, Completer comp1, Completer comp2, Completer comp3,
                  CG_OpndHandle *op1=NULL, CG_OpndHandle *op2=NULL, CG_OpndHandle *op3=NULL, 
                  CG_OpndHandle *op4=NULL, CG_OpndHandle *op5=NULL, CG_OpndHandle *op6=NULL);
        
    // CG helper methods
    void      directCall(U_32, Opnd**, RegOpnd*, Opnd*, RegOpnd*, Completer=CMPLT_WH_SPTK);
    void      indirectCall(U_32, Opnd**, RegOpnd*, RegOpnd*, RegOpnd*, Completer=CMPLT_WH_SPTK);
    void      makeCallArgs(U_32, Opnd**, Inst*, RegOpnd*);
    RegOpnd   *makeConvOpnd(RegOpnd*);
    void      makeRetVal(RegOpnd*, RegOpnd*, RegOpnd*);

    void      add(RegOpnd*, CG_OpndHandle*, CG_OpndHandle*);
    void      sub(RegOpnd*, CG_OpndHandle*, CG_OpndHandle*);
    void      cmp(InstCode, Completer, RegOpnd*, RegOpnd*, CG_OpndHandle*, CG_OpndHandle*);
    void      cmp(Completer, RegOpnd*, RegOpnd*, CG_OpndHandle*, CG_OpndHandle*);
    void      binOp(InstCode, RegOpnd*, CG_OpndHandle*, CG_OpndHandle*);
    void      shift(InstCode, RegOpnd*, CG_OpndHandle*, CG_OpndHandle*, int);
    void      minMax(RegOpnd*, CG_OpndHandle*, CG_OpndHandle*, bool);
    void      xma(InstCode, RegOpnd*, CG_OpndHandle*, CG_OpndHandle*);
    void      saturatingConv4(RegOpnd*, CG_OpndHandle*);
    void      saturatingConv8(RegOpnd*, CG_OpndHandle*);
    void      divDouble(RegOpnd*, CG_OpndHandle*, CG_OpndHandle*, bool rem = false);
    void      divFloat(RegOpnd*, CG_OpndHandle*, CG_OpndHandle*, bool rem = false);
    void      divInt(RegOpnd*, CG_OpndHandle*, CG_OpndHandle*, bool rem = false);
    void      divLong(RegOpnd*, CG_OpndHandle*, CG_OpndHandle*, bool rem = false);
    void      ldc(RegOpnd*, int64 val);
    void      sxt(CG_OpndHandle *src_, int16 refSize, Completer srcSize = CMPLT_INVALID);
    void      zxt(CG_OpndHandle *src_, int16 refSize, Completer srcSize = CMPLT_INVALID);
    RegOpnd   *toRegOpnd(CG_OpndHandle*);
    void      ipfBreakCounted(int);
    void      ipfBreakCounted(RegOpnd*, Completer, int);
    void      ipfBreakCounted(RegOpnd*);
    
    MemoryManager         &mm;
    Cfg                   &cfg;
    BbNode                &node;
    OpndVector            &opnds;
    CompilationInterface  &compilationInterface;
    OpndManager           *opndManager;
    
    RegOpnd               *p0;
};

} //namespace IPF
} //namespace Jitrino 

#endif // _IPF_CODE_SELECTOR_H_
