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
 * @author Intel, Vyacheslav P. Shakin, Nikolay A. Sidelnikov
 */

#include "Log.h"
#include "Ia32InstCodeSelector.h"
#include "Ia32CodeGenerator.h"
#include "Ia32Printer.h"
#include "EMInterface.h"
#include "VMInterface.h"
#include "Opcode.h"
#include "Ia32Tls.h"
#include "Ia32CgUtils.h"
#include "Algorithms.h"

#include <float.h>
#include <math.h>

namespace Jitrino
{
namespace Ia32{

#ifdef _DEBUG
#define ICS_ASSERT(a) assert(a)
#else
#define ICS_ASSERT(a) 
#endif

//For operating with EFLAGS
#define ZF 0x4000
#define UNORD_FLAGS_MASK 0x4500


ConditionMnemonic getConditionMnemonicFromCompareOperator(CompareOp::Operators cmpOp, CompareOp::Types opType)
{
    switch(cmpOp){
        case CompareOp::Eq:             // equal        
            return ConditionMnemonic_E;
        case CompareOp::Ne:             // int: not equal;                         // fp: not equal or unordered
            return ConditionMnemonic_NE;
        case CompareOp::Gt:             // greater than
            return opType==CompareOp::F||opType==CompareOp::D||opType==CompareOp::S?ConditionMnemonic_A:ConditionMnemonic_G;
        case CompareOp::Gtu:            // int: greater than unsigned;                         // fp: greater than or unordered
            return ConditionMnemonic_A;
        case CompareOp::Ge:             // greater than or equal
            return opType==CompareOp::F||opType==CompareOp::D||opType==CompareOp::S?ConditionMnemonic_AE:ConditionMnemonic_GE;
        case CompareOp::Geu:             // int: greater than or equal unsigned; 
            return ConditionMnemonic_AE;
        default:
            assert(0);

    }
    return ConditionMnemonic_E;
}

CompareOp::Types getCompareOpTypesFromCompareZeroOpTypes(CompareZeroOp::Types t)
{
    switch (t){
        case CompareZeroOp::I4:         return CompareOp::I4;
        case CompareZeroOp::I8:         return CompareOp::I8;
        case CompareZeroOp::I:          return CompareOp::I;
        case CompareZeroOp::Ref:        return CompareOp::Ref;
        case CompareZeroOp::CompRef:    return CompareOp::CompRef;
    }
    assert(0);
    return CompareOp::I4;
}

bool swapIfLastIs(Opnd *& opnd0, Opnd *& opnd1, Constraint c=OpndKind_Imm)
{
    if (opnd1!=NULL && opnd0->isPlacedIn(c)){
        Opnd * opnd=opnd0;
        opnd0=opnd1; opnd1=opnd;
        return true;
    }
    return false;
}

///////////////////////////////////////////////////////////////////////////////////
//
//                     class InstCodeSelector
//
///////////////////////////////////////////////////////////////////////////////////


//_______________________________________________________________________________________________________________
// FP conversion internal helpers (temp solution to be optimized)
unsigned long dNaNMask[2]={0xffffffff, 0x7fffffff};
double dnan = *( double* )dNaNMask;

unsigned long fNaNMask[1]={0x7fffffff};
float fnan = *( float* )dNaNMask;

double  __stdcall   convF4F8    (float v) stdcall__;
double  __stdcall   convF4F8    (float v) { return (double)v;   }

float   __stdcall   convF8F4    (double v) stdcall__;
float   __stdcall   convF8F4    (double v) {    return (float)v;    }

I_32   __stdcall   convF4I4    (float v) stdcall__;
I_32   __stdcall   convF4I4    (float v) { 
#ifdef PLATFORM_POSIX
    if (isnan(v))
#else
    if (_isnan(v))
#endif
        return 0;
    if (v>(double)(I_32)0x7fffffff)
        return (I_32)0x7fffffff;     // maxint
    if (v<(double)(I_32)0x80000000)
        return (I_32)0x80000000;     // minint
    return (I_32)v;
}

int64   __stdcall   convF4I8    (float v) stdcall__;
int64   __stdcall   convF4I8    (float v) { 
#ifdef PLATFORM_POSIX
    if (isnan(v))
#else
    if (_isnan(v))
#endif
        return 0;
    if (v >= (double)(int64)(__INT64_C(0x7fffffffffffffff)))
        return (int64)
        __INT64_C(0x7fffffffffffffff);      // maxint
    else if (v < (double)(int64)__INT64_C(0x8000000000000000))
            return (int64)__INT64_C(0x8000000000000000);     // minint
    return (int64)v;
}

I_32   __stdcall   convF8I4    (double v) stdcall__;
I_32   __stdcall   convF8I4    (double v) {
#ifdef PLATFORM_POSIX
    if (isnan(v))
#else
    if (_isnan(v))
#endif
        return 0;
    if (v>(double)(I_32)0x7fffffff)
        return (I_32)0x7fffffff;     // maxint
    if (v<(double)(I_32)0x80000000)
        return (I_32)0x80000000;     // minint
    return (I_32)v;
}

int64   __stdcall   convF8I8    (double v) stdcall__;
int64   __stdcall   convF8I8    (double v) {
#ifdef PLATFORM_POSIX
    if (isnan(v))
#else
    if (_isnan(v))
#endif
        return 0;
    if (v >= (double)(int64)(__INT64_C(0x7fffffffffffffff)))
        return (int64)__INT64_C(0x7fffffffffffffff);      // maxint
    else if (v < (double)(int64)__INT64_C(0x8000000000000000))
            return (int64)__INT64_C(0x8000000000000000);     // minint
    return (int64)v;
}

double  __stdcall   convI4F8    (U_32 v) stdcall__;
double  __stdcall   convI4F8    (U_32 v) {
    return (double)(I_32)v;    }

float   __stdcall   convI4F4    (U_32 v) stdcall__;
float   __stdcall   convI4F4    (U_32 v) {    return (float)(I_32)v; }

double  __stdcall   convI8F8    (uint64 v) stdcall__;
double  __stdcall   convI8F8    (uint64 v) {    return (double)(int64)v;    }

float   __stdcall   convI8F4    (uint64 v) stdcall__;
float   __stdcall   convI8F4    (uint64 v) {    return (float)(int64)v; } 

// FP remainder internal helpers (temp solution to be optimized)
float   __stdcall   remF4   (float v0, float v1)stdcall__;
float   __stdcall   remF4   (float v0, float v1)   { 
    return (float)fmod((double)v0,(double)v1);
}

double  __stdcall   remF8   (double v0, double v1)stdcall__;
double  __stdcall   remF8   (double v0, double v1)  {
    return jitrino_ieee754_fmod_double(v0,v1);
} 

void __stdcall initialize_array(U_8* array, U_32 elems_offset, U_8* data, U_32 num_elems) stdcall__;
void __stdcall initialize_array(U_8* array, U_32 elems_offset, U_8* data, U_32 num_elems) {
    U_8* array_data = array + elems_offset;
    for (U_32 i = 0; i < num_elems; i++) {
        array_data[i] = data[i];
    }
}

void __stdcall add_value_profile_value(EM_ProfileAccessInterface* profileAccessInterface, Method_Profile_Handle mpHandle, U_32 index, POINTER_SIZE_INT value) stdcall__;
void __stdcall add_value_profile_value(EM_ProfileAccessInterface* profileAccessInterface, Method_Profile_Handle mpHandle, U_32 index, POINTER_SIZE_INT value) {
    profileAccessInterface->value_profiler_add_value(mpHandle, index, value);
}

void __stdcall fill_array_with_const(U_32 copyOp, U_32 arrayRef, U_32 arrayBound, U_32 baseOp) {
    Jitrino::crash("Illegal internal helper was called.\n Please enable cg_fastArrayFill optimization in the code generator path");
    assert(0);
    return;
}

//_______________________________________________________________________________________________________________
U_32 InstCodeSelector::_tauUnsafe;

//_______________________________________________________________________________________________________________
void InstCodeSelector::onCFGInit(IRManager& irManager)
{
// FP conversion internal helpers (temp solution to be optimized)
    irManager.registerInternalHelperInfo("convF4F8", IRManager::InternalHelperInfo((void*)&convF4F8,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("convF8F4", IRManager::InternalHelperInfo((void*)&convF8F4,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("convF4I4", IRManager::InternalHelperInfo((void*)&convF4I4,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("convF4I8", IRManager::InternalHelperInfo((void*)&convF4I8,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("convF8I4", IRManager::InternalHelperInfo((void*)&convF8I4,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("convF8I8", IRManager::InternalHelperInfo((void*)&convF8I8,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("convI4F8", IRManager::InternalHelperInfo((void*)&convI4F8,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("convI4F4", IRManager::InternalHelperInfo((void*)&convI4F4,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("convI8F8", IRManager::InternalHelperInfo((void*)&convI8F8,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("convI8F4", IRManager::InternalHelperInfo((void*)&convI8F4,&CallingConvention_STDCALL));

// FP remainder internal helpers (temp solution to be optimized)
    irManager.registerInternalHelperInfo("remF8", IRManager::InternalHelperInfo((void*)&remF8,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("remF4", IRManager::InternalHelperInfo((void*)&remF4,&CallingConvention_STDCALL));

    irManager.registerInternalHelperInfo("initialize_array", IRManager::InternalHelperInfo((void*)&initialize_array,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("add_value_profile_value", IRManager::InternalHelperInfo((void*)&add_value_profile_value,&CallingConvention_STDCALL));

    irManager.registerInternalHelperInfo("fill_array_with_const", IRManager::InternalHelperInfo((void*)&fill_array_with_const,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("memory_copy_direct", IRManager::InternalHelperInfo(NULL,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("memory_copy_reverse", IRManager::InternalHelperInfo(NULL,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("String_compareTo", IRManager::InternalHelperInfo(NULL,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("String_regionMatches", IRManager::InternalHelperInfo(NULL,&CallingConvention_STDCALL));
    irManager.registerInternalHelperInfo("String_indexOf", IRManager::InternalHelperInfo(NULL,&CallingConvention_STDCALL));
}

//_______________________________________________________________________________________________________________
//  Constructor

InstCodeSelector::
InstCodeSelector(CompilationInterface&          compIntfc,
                    CfgCodeSelector&            codeSel,
                    IRManager&                  irM,
                    Node *                      currBasicBlock
                    ) 
: compilationInterface(compIntfc), codeSelector(codeSel),   
  irManager(irM), typeManager(irM.getTypeManager()), 
  memManager("InstCodeSelector"),
  currentBasicBlock(currBasicBlock),
  inArgPos(0),
  seenReturn(false),               switchSrcOpnd(NULL), 
  switchNumTargets(0), currPersistentId(),
  currPredOpnd(NULL)
{
}

//_______________________________________________________________________________________________________________
Inst * InstCodeSelector::appendInsts(Inst *inst)
{
    assert(currentBasicBlock);
    inst->setBCOffset(currentHIRInstBCOffset);
    assert(currentHIRInstBCOffset!=ILLEGAL_BC_MAPPING_VALUE || !InstUtils::instMustHaveBCMapping(inst));
    if (Log::isEnabled()){
        Inst * i=inst; 
        do {
            Log::out()<<"\tIA32 inst: ";
            IRPrinter::printInst(Log::out(), i); 
            Log::out()<<::std::endl;
            i=i->getNext();
        }while (i!=inst);
    }

    currentBasicBlock->appendInst(inst);

    return inst;
}

//_______________________________________________________________________________________________________________
//    Mark operand as global temporary

void InstCodeSelector::opndMaybeGlobal(CG_OpndHandle* opnd) 
{
}

//_______________________________________________________________________________________________________________
void InstCodeSelector::copyOpndTrivialOrTruncatingConversion(Opnd *dst, Opnd *src) 
{ 
    assert(dst->getSize()<=src->getSize());
#ifndef _EM64T_
    if (src->getType()->isInteger()&&src->getSize()==OpndSize_64)
        appendInsts(irManager.newI8PseudoInst(Mnemonic_MOV, 1, dst, src));
    else
#endif
        appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, dst, src));
}

//_______________________________________________________________________________________________________________
//    Copy any kind of the operand
void InstCodeSelector::copyOpnd(Opnd *dst, Opnd *src, bool doZeroExtension) 
{ 
    convert(src, dst->getType(), dst, doZeroExtension);
}


//_______________________________________________________________________________________________________________
void InstCodeSelector::throwLinkingException(Class_Handle encClass, U_32 cp_ndx, U_32 opcode)
{  
    Opnd* encClassArg = irManager.newImmOpnd(getRuntimeIdType(), (POINTER_SIZE_INT)encClass);
    Opnd* cpIndexArg = irManager.newImmOpnd(typeManager.getUInt32Type(),cp_ndx);
    Opnd* opcodeArg = irManager.newImmOpnd(typeManager.getUInt32Type(), opcode);

    Opnd* args[] = {encClassArg, cpIndexArg, opcodeArg};
    appendInsts(irManager.newRuntimeHelperCallInst(VM_RT_THROW_LINKING_EXCEPTION, lengthof(args), args, NULL));
}
//_______________________________________________________________________________________________________________
Opnd * InstCodeSelector::convertIntToInt(Opnd * srcOpnd, Type * dstType, Opnd * dstOpnd, bool isZeroExtend)
{
    Type * srcType=srcOpnd->getType();
    assert(isIntegerType(srcType) && (isIntegerType(dstType) || dstType->isPtr()));

    OpndSize srcSize=irManager.getTypeSize(srcType);
    OpndSize dstSize=irManager.getTypeSize(dstType);
    
    if (dstSize==srcSize){ // trivial conversion
        if (dstOpnd==NULL && dstType == srcType) {
            dstOpnd=srcOpnd;
        } else {
            if (dstOpnd == NULL) {
                dstOpnd = irManager.newOpnd(dstType);
            }
            copyOpndTrivialOrTruncatingConversion(dstOpnd, srcOpnd);
        }
    }else if (dstSize<=srcSize){ // truncating conversion 
        if (dstOpnd==NULL)
            dstOpnd=irManager.newOpnd(dstType);
        copyOpndTrivialOrTruncatingConversion(dstOpnd, srcOpnd);
    }else{
        if (dstOpnd==NULL)
            dstOpnd=irManager.newOpnd(dstType);
#ifdef _EM64T_
            appendInsts(irManager.newInstEx(srcType->isSignedInteger() & !isZeroExtend ? Mnemonic_MOVSX:Mnemonic_MOVZX, 1, dstOpnd, srcOpnd));
#else
        if (dstSize<OpndSize_64){
            appendInsts(irManager.newInstEx(srcType->isSignedInteger() && !isZeroExtend ? Mnemonic_MOVSX:Mnemonic_MOVZX, 1, dstOpnd, srcOpnd));
        }else{
            appendInsts(irManager.newI8PseudoInst(srcType->isSignedInteger()?Mnemonic_MOVSX:Mnemonic_MOVZX, 1, dstOpnd, srcOpnd));
        }
#endif
    }
    return dstOpnd;
}

//_______________________________________________________________________________________________________________
Opnd * InstCodeSelector::convertIntToFp(Opnd * srcOpnd, Type * dstType, Opnd * dstOpnd)
{
    assert(srcOpnd->getType()->isInteger() && dstType->isFP());
    if (dstOpnd==NULL) {
        dstOpnd=irManager.newOpnd(dstType);
    }
    OpndSize srcSize = srcOpnd->getSize();
    OpndSize dstSize = dstOpnd->getSize();

    if (srcSize<OpndSize_32) {
        srcOpnd=convert(srcOpnd, typeManager.getInt32Type());
        srcSize = srcOpnd->getSize();
    }
    assert(srcSize == OpndSize_32 || srcSize == OpndSize_64);

    bool useHelpers = codeSelector.getFlags().useInternalHelpersForInteger2FloatConv; 
    if (!useHelpers) { //use SSE
        if (srcSize == OpndSize_32) {
            Inst* convInst = irManager.newInst(dstSize == OpndSize_32 ? Mnemonic_CVTSI2SS : Mnemonic_CVTSI2SD, dstOpnd, srcOpnd);            
            appendInsts(convInst);
        } else { 
            //use FPU to convert long to float/double on IA32 platform
#ifdef _EM64T_
            Opnd* int64Opnd = srcOpnd;
#else  //32 bit mode - use memory aliasing for 64bit opnd
            //copy i8 to stack first
            Opnd* i8onStackOpnd = irManager.newMemOpnd(srcOpnd->getType(), MemOpndKind_StackAutoLayout, irManager.getRegOpnd(STACK_REG));
            Inst* copyInst = irManager.newI8PseudoInst(Mnemonic_MOV, 1, i8onStackOpnd, srcOpnd);
            appendInsts(copyInst);

            //Alias i8onStackOpnd memory location with new opnd that is not affected by i8lowerer and keeps int64 type until emitter
            Opnd* int64Opnd = irManager.newMemOpnd(srcOpnd->getType(), MemOpndKind_StackAutoLayout, irManager.getRegOpnd(STACK_REG));
            Opnd * args[] = {i8onStackOpnd};
            Inst* memAliasInst = irManager.newAliasPseudoInst(int64Opnd, 1, args);
            appendInsts(memAliasInst);
#endif

            //load float value to FP0
            Opnd* fpResOpnd = irManager.newOpnd(irManager.getTypeManager().getDoubleType());
            Inst* convInst = irManager.newInst(Mnemonic_FILD, fpResOpnd, int64Opnd);            
            appendInsts(convInst);

            //copy from FP0 to SSE
            if (dstType->isDouble()) {
                Inst* copy2SSEInst = irManager.newCopySequence(Mnemonic_MOV, dstOpnd, fpResOpnd);
                appendInsts(copy2SSEInst);
            } else {
                assert(dstSize == OpndSize_32);
                convertFpToFp(fpResOpnd, dstType, dstOpnd);
            }
        }
    }

    if (useHelpers) {
        const char * helperName;
        if (srcSize == OpndSize_32) {
            helperName=dstType->isSingle()?"convI4F4":"convI4F8";
        } else {
            helperName=dstType->isSingle()?"convI8F4":"convI8F8";
        }
        Opnd * args[] = {srcOpnd};
        appendInsts(irManager.newInternalRuntimeHelperCallInst(helperName, 1, args, dstOpnd));
    }
    return dstOpnd;
}

//_______________________________________________________________________________________________________________
Opnd * InstCodeSelector::convertFpToInt(Opnd * srcOpnd, Type * dstType, Opnd * dstOpnd)
{
    assert(srcOpnd->getType()->isFP() && dstType->isInteger());
    const char * helperName;
    OpndSize dstSize=irManager.getTypeSize(dstType);
    if (dstSize<=OpndSize_32){
        if (dstOpnd==NULL)
            dstOpnd=irManager.newOpnd(typeManager.getInt32Type());
        helperName=srcOpnd->getType()->isSingle()?"convF4I4":"convF8I4";
    }else{
        assert(dstSize==OpndSize_64);
        if (dstOpnd==NULL)
            dstOpnd=irManager.newOpnd(dstType);
        helperName=srcOpnd->getType()->isSingle()?"convF4I8":"convF8I8";
    }

    Opnd * args[] = {srcOpnd};
    appendInsts(irManager.newInternalRuntimeHelperCallInst(helperName, 1, args, dstOpnd));

    if (dstSize<OpndSize_32)
        dstOpnd=convert(dstOpnd, dstType);

    return dstOpnd;
}

//_______________________________________________________________________________________________________________
Opnd * InstCodeSelector::convertFpToFp(Opnd * srcOpnd, Type * dstType, Opnd * dstOpnd)
{
    Type * srcType=srcOpnd->getType();
    assert(srcType->isFP() && dstType->isFP());
    if ((srcType->isSingle() && dstType->isSingle())||!(srcType->isSingle() || dstType->isSingle())){
        if (!dstOpnd)
            return srcOpnd;
        copyOpndTrivialOrTruncatingConversion(dstOpnd, srcOpnd);
        return dstOpnd;
    }
    if (dstOpnd==NULL)
        dstOpnd=irManager.newOpnd(dstType);
    const char * helperName=dstType->isSingle()?"convF8F4":"convF4F8";
    Opnd * args[] = {srcOpnd};
    appendInsts(irManager.newInternalRuntimeHelperCallInst(helperName, 1, args, dstOpnd));
    return dstOpnd;
}

//_______________________________________________________________________________________________________________
Opnd * InstCodeSelector::convertToUnmanagedPtr(Opnd * srcOpnd, Type * dstType, Opnd * dstOpnd, bool isZeroExtend) {
    assert(dstType->isUnmanagedPtr());
    Type* srcType = srcOpnd->getType();
    if (dstOpnd == NULL) {
        dstOpnd = irManager.newOpnd(dstType);
    }
    if (srcType->isObject()) {
        OpndSize srcSize=irManager.getTypeSize(srcType);
        OpndSize dstSize=irManager.getTypeSize(dstType);
        if (srcSize == dstSize) {
            appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, dstOpnd, srcOpnd));
        } else {
#ifdef _EM64T_
            if (srcType->isInteger()) {
                appendInsts(irManager.newInstEx(isZeroExtend?Mnemonic_MOVZX:Mnemonic_MOVSX, 1, dstOpnd, srcOpnd)); 
            }       
#else
            assert(0);
#endif
        }
    } else if (srcType->isInteger()) {
        convertIntToInt(srcOpnd, dstType, dstOpnd);
    } else {
        assert(0);
    }
    return dstOpnd;
}


//_______________________________________________________________________________________________________________
Opnd * InstCodeSelector::convertUnmanagedPtr(Opnd * srcOpnd, Type * dstType, Opnd * dstOpnd) {
    Type * srcType=srcOpnd->getType();
    assert(srcType->isUnmanagedPtr());

    if (dstOpnd == NULL) {
        dstOpnd = irManager.newOpnd(dstType);
    }

    if (dstType->isObject()) {
        appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, dstOpnd, srcOpnd));
    } else {
        assert(dstType->isInteger() || dstType->isVTablePtr());
        OpndSize srcSize=irManager.getTypeSize(srcType);
        OpndSize dstSize=irManager.getTypeSize(dstType);
        if (dstSize<=srcSize) {
            copyOpndTrivialOrTruncatingConversion(dstOpnd, srcOpnd);
        } else {
            assert(dstSize==OpndSize_64);
#ifdef _EM64T_
            appendInsts(irManager.newInstEx(srcType->isSignedInteger()?Mnemonic_MOVSX:Mnemonic_MOVZX, 1, dstOpnd, srcOpnd));
#else
            appendInsts(irManager.newI8PseudoInst(srcType->isSignedInteger()?Mnemonic_MOVSX:Mnemonic_MOVZX, 1, dstOpnd, srcOpnd));
#endif
        }
    }
    return dstOpnd;
}

//_______________________________________________________________________________________________________________
Opnd * InstCodeSelector::convert(CG_OpndHandle * oph, Type * dstType, Opnd * dstOpnd, bool isZeroExtend)
{
    if (!oph)
        return NULL;
    Opnd * srcOpnd=(Opnd*)oph;

    Type * srcType=srcOpnd->getType();
    bool converted=false;
    if (isIntegerType(srcType)){
        if (isIntegerType(dstType)){
            dstOpnd=convertIntToInt(srcOpnd, dstType, dstOpnd, isZeroExtend);
            converted=true;
        } else if (dstType->isFP()){
            dstOpnd=convertIntToFp(srcOpnd, dstType, dstOpnd);
            converted=true;
        } else if (dstType->isUnmanagedPtr()) {
            dstOpnd = convertToUnmanagedPtr(srcOpnd, dstType, dstOpnd, isZeroExtend);
            converted = true;
        }
    }else if (srcType->isFP()){
        if (dstType->isInteger()){
            dstOpnd=convertFpToInt(srcOpnd, dstType, dstOpnd);
            converted=true;
        }else if (dstType->isFP()){
            dstOpnd=convertFpToFp(srcOpnd, dstType, dstOpnd);
            converted=true;
        }   
    } else if (srcType->isUnmanagedPtr() && !dstType->isUnmanagedPtr()) {
        dstOpnd = convertUnmanagedPtr(srcOpnd, dstType, dstOpnd);
        converted = true;
    } else  if (srcType->isObject() && dstType->isUnmanagedPtr()) {
        dstOpnd = convertToUnmanagedPtr(srcOpnd, dstType, dstOpnd);
        converted = true;
    }
    if (!converted){
        assert(irManager.getTypeSize(dstType)==srcOpnd->getSize());
        if (dstOpnd==NULL)
            dstOpnd=srcOpnd;
        else
            copyOpndTrivialOrTruncatingConversion(dstOpnd, srcOpnd);
    }
    return dstOpnd;
}


//_______________________________________________________________________________________________________________
//  Convert to integer

CG_OpndHandle* InstCodeSelector::convToInt(ConvertToIntOp::Types       opType,
                                           bool                        isSigned,
                                           bool                        isZeroExtend,
                                           ConvertToIntOp::OverflowMod ovfMod,
                                           Type*                       dstType, 
                                           CG_OpndHandle*              src) 
{
    Type * sizeType=NULL;
    switch (opType){
        case ConvertToIntOp::I1:
            sizeType=isSigned?typeManager.getInt8Type():typeManager.getUInt8Type();
            break;
        case ConvertToIntOp::I2:
            sizeType=isSigned?typeManager.getInt16Type():typeManager.getUInt16Type();
            break;

#ifdef _IA32_
        case ConvertToIntOp::I:
#endif
        case ConvertToIntOp::I4:
            sizeType=isSigned?typeManager.getInt32Type():typeManager.getUInt32Type();
            break;

#ifdef _EM64T_
        case ConvertToIntOp::I:
#endif
        case ConvertToIntOp::I8:
            sizeType=isSigned?typeManager.getInt64Type():typeManager.getUInt64Type();
            break;
        default: assert(0);
    }
    Opnd * tmpOpnd = convert(src, sizeType, NULL, isZeroExtend); 
    CG_OpndHandle* res =  convert(tmpOpnd, dstType, NULL, isZeroExtend);
    return res;
}

//_______________________________________________________________________________________________________________
//  Convert to floating-point

CG_OpndHandle*  InstCodeSelector::convToFp(ConvertToFpOp::Types opType, 
                                                Type*                 dstType, 
                                                CG_OpndHandle*        src) 
{
    return convert(src, dstType);
}

//_______________________________________________________________________________________________________________
//  Convert to objects to unmanaged pointers and via versa

/// convert unmanaged pointer to object. Boxing
CG_OpndHandle*  InstCodeSelector::convUPtrToObject(ObjectType * dstType, CG_OpndHandle* val) {
    return convert(val, dstType);
}

/// convert object or integer to unmanaged pointer.
CG_OpndHandle*  InstCodeSelector::convToUPtr(PtrType * dstType, CG_OpndHandle* src) {
    return  convert(src, dstType);
}

//_______________________________________________________________________________________________________________
Opnd * InstCodeSelector::createResultOpnd(Type * dstType)
{
    if (dstType==NULL)
        return NULL;
    return irManager.newOpnd(dstType);
}

//_______________________________________________________________________________________________________________
Opnd * InstCodeSelector::simpleOp_I8(Mnemonic mn, Type * dstType, Opnd * src1, Opnd * src2)
{
    src1=convert(src1, dstType); 

    if (src2!=NULL){
        src2=convert(src2, dstType);
    }
    Opnd * dst = irManager.newOpnd(dstType);

    switch(mn){
        case Mnemonic_ADD:
        case Mnemonic_SUB:
        case Mnemonic_AND:
        case Mnemonic_OR:
        case Mnemonic_XOR:
        case Mnemonic_NOT:
#ifndef _EM64T_
            appendInsts(irManager.newI8PseudoInst(mn, 1, dst, src1, src2));
#else
            appendInsts(irManager.newInstEx(mn, 1, dst, src1, src2));
#endif
            break;
        default:
            assert(0);
            break;
    }

    return dst;
}

//_______________________________________________________________________________________________________________
Opnd * InstCodeSelector::fpOp(Mnemonic mn, Type * dstType, Opnd * src1, Opnd * src2)
{
    Opnd * dst=irManager.newOpnd(dstType);
    Opnd * srcOpnd1=(Opnd*)convert(src1, dstType), * srcOpnd2=(Opnd*)convert(src2, dstType);
    appendInsts(irManager.newInstEx(mn, 1, dst, srcOpnd1, srcOpnd2));
    return dst;
}

//_______________________________________________________________________________________________________________
Opnd * InstCodeSelector::simpleOp_I4(Mnemonic mn, Type * dstType, Opnd * src1, Opnd * src2)
{
    Opnd * dst=irManager.newOpnd(dstType);
    Opnd * srcOpnd1=(Opnd*)convert(src1, dstType);
    Opnd * srcOpnd2=src2==NULL?NULL:(Opnd*)convert(src2, dstType);
    if (Encoder::getMnemonicProperties(mn)&Inst::Properties_Symmetric)
        swapIfLastIs(srcOpnd1, srcOpnd2);
    appendInsts(irManager.newInstEx(mn, 1, dst, srcOpnd1, srcOpnd2));
    return dst;
}

//_______________________________________________________________________________________________________________
//  Add numeric values

CG_OpndHandle* InstCodeSelector::add(ArithmeticOp::Types opType, 
                                        CG_OpndHandle*      src1,
                                        CG_OpndHandle*      src2) 
{
    switch(opType){
        case ArithmeticOp::I4:
        case ArithmeticOp::I:{
            Type * dstType=opType==ArithmeticOp::I?irManager.getTypeFromTag(Type::IntPtr):irManager.getTypeFromTag(Type::Int32);
            return simpleOp_I4(Mnemonic_ADD, dstType, (Opnd*)src1, (Opnd*)src2);
        }
        case ArithmeticOp::I8:
            return simpleOp_I8(Mnemonic_ADD, irManager.getTypeFromTag(Type::Int64), (Opnd*)src1, (Opnd*)src2);
        case ArithmeticOp::D:
        case ArithmeticOp::F:
            return fpOp(Mnemonic_ADDSD, irManager.getTypeFromTag(Type::Double), (Opnd*)src1, (Opnd*)src2);
        case ArithmeticOp::S:
            return fpOp(Mnemonic_ADDSS, irManager.getTypeFromTag(Type::Single), (Opnd*)src1, (Opnd*)src2);
        default:
            ICS_ASSERT(0);
    }
    return NULL;
}

//_______________________________________________________________________________________________________________
//  Sub numeric values

CG_OpndHandle* InstCodeSelector::sub(ArithmeticOp::Types opType, 
                                        CG_OpndHandle*      src1,
                                        CG_OpndHandle*      src2) 
{
    switch(opType){
        case ArithmeticOp::I4:
        case ArithmeticOp::I:{
            Type * dstType=opType==ArithmeticOp::I?irManager.getTypeFromTag(Type::IntPtr):irManager.getTypeFromTag(Type::Int32);
            return simpleOp_I4(Mnemonic_SUB, dstType, (Opnd*)src1, (Opnd*)src2);
        }
        case ArithmeticOp::I8:
            return simpleOp_I8(Mnemonic_SUB, irManager.getTypeFromTag(Type::Int64), (Opnd*)src1, (Opnd*)src2);
        case ArithmeticOp::D:
        case ArithmeticOp::F:
            return fpOp(Mnemonic_SUBSD, irManager.getTypeFromTag(Type::Double), (Opnd*)src1, (Opnd*)src2);
        case ArithmeticOp::S:
            return fpOp(Mnemonic_SUBSS, irManager.getTypeFromTag(Type::Single), (Opnd*)src1, (Opnd*)src2);
        default:
            ICS_ASSERT(0);
    }
    return NULL;
}


//_______________________________________________________________________________________________________________
//  Add integer to a reference

CG_OpndHandle* InstCodeSelector::addRef(RefArithmeticOp::Types opType,
                                           CG_OpndHandle*         refSrc,
                                           CG_OpndHandle*         intSrc) 
{
    return add(opType==RefArithmeticOp::I?ArithmeticOp::I:ArithmeticOp::I4, refSrc, intSrc);
}

//_______________________________________________________________________________________________________________
//  Subtract integer from a reference

CG_OpndHandle* InstCodeSelector::subRef(RefArithmeticOp::Types opType,
                                           CG_OpndHandle*         refSrc,
                                           CG_OpndHandle*         intSrc) 
{
    return sub(opType==RefArithmeticOp::I?ArithmeticOp::I:ArithmeticOp::I4, refSrc, intSrc);
}

//_______________________________________________________________________________________________________________
//  Subtract reference from reference

CG_OpndHandle*    InstCodeSelector::diffRef(bool           ovf, 
                                                 CG_OpndHandle* ref1,
                                                 CG_OpndHandle* ref2) 
{
    return sub(ArithmeticOp::I4, ref1, ref2);
}   

 
//_______________________________________________________________________________________________________________
//  Multiply numeric values

CG_OpndHandle* InstCodeSelector::mul(ArithmeticOp::Types opType, 
                                        CG_OpndHandle*      src1,
                                        CG_OpndHandle*      src2) 
{
    Type * dstType;
    Opnd * dst, * srcOpnd1, * srcOpnd2;
    switch(opType){
        case ArithmeticOp::I4:
        case ArithmeticOp::I:
        {
            dstType=irManager.getTypeFromTag(Type::Int32);
            dst=irManager.newOpnd(dstType);
            srcOpnd1=(Opnd*)convert(src1, dstType);
            srcOpnd2=(Opnd*)convert(src2, dstType);
            swapIfLastIs(srcOpnd1, srcOpnd2);
            appendInsts(irManager.newInstEx(Mnemonic_IMUL, 1, dst, srcOpnd1, srcOpnd2));
            return dst;
        }
        case ArithmeticOp::I8:
            dstType=irManager.getTypeFromTag(Type::Int64);
            dst=irManager.newOpnd(dstType);
            srcOpnd1=(Opnd*)convert(src1, dstType);
            srcOpnd2=(Opnd*)convert(src2, dstType);
            swapIfLastIs(srcOpnd1, srcOpnd2);
#ifndef _EM64T_
            appendInsts(irManager.newI8PseudoInst(Mnemonic_IMUL,1,dst,srcOpnd1,srcOpnd2));
#else
            appendInsts(irManager.newInstEx(Mnemonic_IMUL,1,dst,srcOpnd1,srcOpnd2));
#endif
            return dst;
        case ArithmeticOp::D:
        case ArithmeticOp::F:
            return fpOp(Mnemonic_MULSD, irManager.getTypeFromTag(Type::Double), (Opnd*)src1, (Opnd*)src2);
        case ArithmeticOp::S:
            return fpOp(Mnemonic_MULSS, irManager.getTypeFromTag(Type::Single), (Opnd*)src1, (Opnd*)src2);
        default:
            ICS_ASSERT(0);
    }

    return NULL;
}

//_______________________________________________________________________________________________________________
//  Get high part of multiply

CG_OpndHandle* InstCodeSelector::mulhi(MulHiOp::Types op,
                                          CG_OpndHandle* src1,
                                          CG_OpndHandle* src2) 
{
    ICS_ASSERT(0);
    return 0;
}


//_______________________________________________________________________________________________________________
Opnd * InstCodeSelector::divOp(DivOp::Types   opType, bool rem, Opnd * src1, Opnd * src2)
{
    Opnd * dst=NULL;
    Type * dstType;
    Opnd * srcOpnd1, * srcOpnd2;
    switch(opType){
        case DivOp::I:
        case DivOp::I4:
        {
            dstType=irManager.getTypeFromTag(Type::Int32);
            Opnd * dstOpnd0=irManager.newOpnd(dstType);
            Opnd * dstOpnd1=irManager.newOpnd(dstType);
            Opnd * srcOpnd1=(Opnd*)convert(src1, dstType);
            appendInsts(irManager.newInstEx(Mnemonic_CDQ, 1, dstOpnd1, srcOpnd1));
            appendInsts(irManager.newInstEx(Mnemonic_IDIV, 2, dstOpnd1, dstOpnd0, dstOpnd1, srcOpnd1, (Opnd*)convert(src2, dstType)));
            dst=rem?dstOpnd1:dstOpnd0;
            break;
        }
        case DivOp::I8:
        {
            dstType=irManager.getTypeFromTag(Type::Int64);
            dst=irManager.newOpnd(dstType);
            srcOpnd1=(Opnd*)convert(src1, dstType);
            srcOpnd2=(Opnd*)convert(src2, dstType);

#ifndef _EM64T_
            //
            // NOTE: as we don't have IREM mnemonic, then generate I8Inst 
            // with IDIV mnemonic. The 4th fake non-zero argument means 
            // what we really need REM, not DIV.
            // This is handled specially in I8Lowerer. 
            // The scheme with the fake arg looks ugly, might need to 
            // reconsider.
            //
            Opnd* fakeReminderFlag = NULL;
            if (rem) {
                Type* int32type = irManager.getTypeFromTag(Type::Int32);
                fakeReminderFlag = irManager.newImmOpnd(int32type, 12345678);
            }
            Inst* ii = irManager.newI8PseudoInst(Mnemonic_IDIV, 1, dst, srcOpnd1, srcOpnd2, fakeReminderFlag);
            appendInsts(ii);
            
#else
            Opnd * dstOpnd0=irManager.newOpnd(dstType);
            Opnd * dstOpnd1=irManager.newOpnd(dstType);
            appendInsts(irManager.newInstEx(Mnemonic_CDQ, 1, dstOpnd1, srcOpnd1));
            appendInsts(irManager.newInstEx(Mnemonic_IDIV, 2, dstOpnd1, dstOpnd0, dstOpnd1, srcOpnd1, (Opnd*)convert(src2, dstType)));
            dst=rem?dstOpnd1:dstOpnd0;
#endif
            break;
        }
        case DivOp::D:
        case DivOp::F:
            if(rem) {
                dstType=irManager.getTypeFromTag(Type::Double);
                dst=irManager.newOpnd(dstType);
                Opnd * args[]={ (Opnd*)convert(src1, dstType), (Opnd*)convert(src2, dstType) };
                CallInst * callInst=irManager.newInternalRuntimeHelperCallInst("remF8", 2, args, dst);
                appendInsts(callInst);
            } else {
                return fpOp(Mnemonic_DIVSD, irManager.getTypeFromTag(Type::Double), src1, src2);
            }
            break;

        case DivOp::S:
            if(rem) {
                dstType=irManager.getTypeFromTag(Type::Single);
                dst=irManager.newOpnd(dstType);
                Opnd * args[]={ (Opnd*)convert(src1, dstType), (Opnd*)convert(src2, dstType) };
                CallInst * callInst=irManager.newInternalRuntimeHelperCallInst("remF4", 2, args, dst);
                appendInsts(callInst);
            } else {
                return fpOp(Mnemonic_DIVSS, irManager.getTypeFromTag(Type::Single), src1, src2);
            }
            break;
        default:
            ICS_ASSERT(0);
    }
    return dst;
}

//_______________________________________________________________________________________________________________
//  Divide two numeric values. 

CG_OpndHandle * InstCodeSelector::tau_div(DivOp::Types   op,
                                            CG_OpndHandle* src1,
                                            CG_OpndHandle* src2,
                                            CG_OpndHandle* tau_src1NonZero) 
{
    return divOp(op, false, (Opnd *)src1, (Opnd *)src2);
}

//_______________________________________________________________________________________________________________
//  Get remainder from the division of two numeric values

CG_OpndHandle* InstCodeSelector::tau_rem(DivOp::Types   op,
                                            CG_OpndHandle* src1,
                                            CG_OpndHandle* src2,
                                            CG_OpndHandle* tau_src2NonZero) 
{
    return divOp(op, true, (Opnd *)src1, (Opnd *)src2);
}

//_______________________________________________________________________________________________________________
//  Negate numeric value

CG_OpndHandle* InstCodeSelector::neg(NegOp::Types   opType,
                                          CG_OpndHandle* src) 
{
    Opnd * dst=NULL;
    switch(opType){
        case NegOp::I:
        case NegOp::I4:
        {
            Type * dstType=irManager.getTypeFromTag(Type::Int32);
            dst=irManager.newOpnd(dstType);
            appendInsts(irManager.newInstEx(Mnemonic_NEG, 1, dst, (Opnd*)convert(src, dstType)));
            break;
        }
        case NegOp::I8:
        {
            Type * dstType=irManager.getTypeFromTag(Type::Int64);
            Opnd * dstMOV = irManager.newOpnd(dstType);
            Opnd * src_null = irManager.newImmOpnd(dstType, 0);
#ifndef _EM64T_
            appendInsts(irManager.newI8PseudoInst(Mnemonic_MOV,1,dstMOV,src_null));
#else
            appendInsts(irManager.newInstEx(Mnemonic_MOV,1,dstMOV,src_null));
#endif
            dst = (Opnd *)sub(ArithmeticOp::I8, dstMOV, src);
            break;
        }
        case NegOp::D:
        case NegOp::F:
        {
            Type * dstType=irManager.getTypeFromTag(Type::Double);
            dst = irManager.newOpnd(dstType);
            appendInsts(irManager.newInstEx(Mnemonic_FCHS,1,dst,(Opnd*)src));
            break;
        }
        case NegOp::S:
        {
            Type * dstType=irManager.getTypeFromTag(Type::Single);
            dst = irManager.newOpnd(dstType);
            appendInsts(irManager.newInstEx(Mnemonic_FCHS,1,dst,(Opnd*)src));
            break;
        }
        default:
            ICS_ASSERT(0);
    }
    return dst;
}

//_______________________________________________________________________________________________________________
Opnd * InstCodeSelector::minMaxOp(NegOp::Types   opType, bool max, Opnd * src1, Opnd * src2)
{
    Opnd * dst=NULL;
    switch(opType){
        case NegOp::I:
        case NegOp::I4:
        {
            Type * dstType=irManager.getTypeFromTag(Type::Int32);
            src1=convert(src1, dstType);
            src2=convert(src2, dstType);
            dst=irManager.newOpnd(dstType);
            appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, dst, (Opnd*)src2));
            appendInsts(irManager.newInst(Mnemonic_CMP, (Opnd*)src1, dst));
            appendInsts(irManager.newInst(max?Mnemonic_CMOVG:Mnemonic_CMOVL, dst, (Opnd*)src1));
            break;
        }
        case NegOp::I8:
        case NegOp::D:
        case NegOp::F:
        case NegOp::S:
            ICS_ASSERT(0);      
        default:
            ICS_ASSERT(0);
    }
    return dst;
}

//_______________________________________________________________________________________________________________
//  Min

CG_OpndHandle* InstCodeSelector::min_op(NegOp::Types   opType,
                                           CG_OpndHandle* src1,
                                           CG_OpndHandle* src2) 
{
    return minMaxOp(opType, false, (Opnd*)src1, (Opnd*)src2);
}

//_______________________________________________________________________________________________________________
//  Max

CG_OpndHandle* InstCodeSelector::max_op(NegOp::Types   opType,
                                           CG_OpndHandle* src1,
                                           CG_OpndHandle* src2) 
{
    return minMaxOp(opType, true, (Opnd*)src1, (Opnd*)src2);
}

//_______________________________________________________________________________________________________________
//    Abs

CG_OpndHandle* InstCodeSelector::abs_op(NegOp::Types   opType, 
                                           CG_OpndHandle* src) 
{
    Opnd * dst=NULL;
    switch(opType){
        case NegOp::I:
        case NegOp::I4:
        {
            Type * dstType=irManager.getTypeFromTag(Type::Int32);
            dst=irManager.newOpnd(dstType);
            Opnd * srcOpnd=convert(src, dstType);
            appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, dst, (Opnd*)srcOpnd));
            appendInsts(irManager.newInst(Mnemonic_NEG, dst));
            appendInsts(irManager.newInstEx(Mnemonic_CMOVL, 1, dst, dst, (Opnd*)srcOpnd));
            break;
        }
        case NegOp::I8:
        case NegOp::D:
        case NegOp::F:
        case NegOp::S:
            ICS_ASSERT(0);      
        default:
            ICS_ASSERT(0);
    }
    return dst;
}


//_______________________________________________________________________________________________________________
//    Check if value is finite

CG_OpndHandle*    InstCodeSelector::tau_ckfinite(CG_OpndHandle* src) 
{
    return getTauUnsafe();
}

//_______________________________________________________________________________________________________________
//  Logical and

CG_OpndHandle* InstCodeSelector::and_(IntegerOp::Types opType,
                                           CG_OpndHandle*   src1,
                                           CG_OpndHandle*   src2) 
{
    switch(opType){
        case IntegerOp::I4:
        case IntegerOp::I:{
            Type * dstType=opType==IntegerOp::I?irManager.getTypeFromTag(Type::IntPtr):irManager.getTypeFromTag(Type::Int32);
            return simpleOp_I4(Mnemonic_AND, dstType, (Opnd*)src1, (Opnd*)src2);
        }
        case IntegerOp::I8:
            return simpleOp_I8(Mnemonic_AND, irManager.getTypeFromTag(Type::Int64), (Opnd*)src1, (Opnd*)src2);
        default:
            ICS_ASSERT(0);
    }
    return NULL;
}

//_______________________________________________________________________________________________________________
//  Logical or

CG_OpndHandle* InstCodeSelector::or_(IntegerOp::Types opType,
                                          CG_OpndHandle*   src1,
                                          CG_OpndHandle*   src2) 
{
    switch(opType){
        case IntegerOp::I4:
        case IntegerOp::I:{
            Type * dstType=opType==IntegerOp::I?irManager.getTypeFromTag(Type::IntPtr):irManager.getTypeFromTag(Type::Int32);
            return simpleOp_I4(Mnemonic_OR, dstType, (Opnd*)src1, (Opnd*)src2);
        }
        case IntegerOp::I8:
            return simpleOp_I8(Mnemonic_OR, irManager.getTypeFromTag(Type::Int64), (Opnd*)src1, (Opnd*)src2);
        default:
            ICS_ASSERT(0);
    }
    return NULL;
}

//_______________________________________________________________________________________________________________
//  Logical xor

CG_OpndHandle*    InstCodeSelector::xor_(IntegerOp::Types opType,
                                              CG_OpndHandle* src1,
                                              CG_OpndHandle* src2) 
{
    switch(opType){
        case IntegerOp::I4:
        case IntegerOp::I:{
            Type * dstType=opType==IntegerOp::I?irManager.getTypeFromTag(Type::IntPtr):irManager.getTypeFromTag(Type::Int32);
            return simpleOp_I4(Mnemonic_XOR, dstType, (Opnd*)src1, (Opnd*)src2);
        }
        case IntegerOp::I8:
            return simpleOp_I8(Mnemonic_XOR, irManager.getTypeFromTag(Type::Int64), (Opnd*)src1, (Opnd*)src2);
        default:
            ICS_ASSERT(0);
    }
    return NULL;
}

//_______________________________________________________________________________________________________________
//  Logical not

CG_OpndHandle*    InstCodeSelector::not_(IntegerOp::Types opType, 
                                              CG_OpndHandle*   src) 
{
    switch(opType){
        case IntegerOp::I4:
        case IntegerOp::I:{
            Type * dstType=opType==IntegerOp::I?irManager.getTypeFromTag(Type::IntPtr):irManager.getTypeFromTag(Type::Int32);
            return simpleOp_I4(Mnemonic_NOT, dstType, (Opnd*)src, 0);
        }
        case IntegerOp::I8:
            return simpleOp_I8(Mnemonic_NOT, irManager.getTypeFromTag(Type::Int64), (Opnd*)src, 0);
        default:
            ICS_ASSERT(0);
    }
    return NULL;
}

//_______________________________________________________________________________________________________________
Opnd * InstCodeSelector::shiftOp(IntegerOp::Types opType, Mnemonic mn, Opnd * value, Opnd * shiftAmount)
{
    Opnd * dst=NULL;
    Type * dstType;
    switch(opType){
        case IntegerOp::I:
        case IntegerOp::I4:
        {
            dstType=irManager.getTypeFromTag(Type::Int32);
            dst = irManager.newOpnd(dstType);
            appendInsts(irManager.newInstEx(mn, 1, dst, (Opnd*)convert(value, dstType), (Opnd*)convert(shiftAmount, typeManager.getInt8Type())));
            break;
        }
        case IntegerOp::I8:
            dstType=irManager.getTypeFromTag(Type::Int64);
            dst=irManager.newOpnd(dstType);
#ifndef _EM64T_
            appendInsts(irManager.newI8PseudoInst(mn,1,dst,(Opnd*)convert(value, dstType),(Opnd*)convert(shiftAmount, typeManager.getInt32Type())));
#else
            appendInsts(irManager.newInstEx(mn,1,dst,(Opnd*)convert(value, dstType),(Opnd*)convert(shiftAmount, typeManager.getInt32Type())));
#endif
            return dst;
        default:
            ICS_ASSERT(0);
    }
    return dst;
}

//_______________________________________________________________________________________________________________
//  Shift left and add
#define LEA_EXP_LIMIT 3
CG_OpndHandle*    InstCodeSelector::shladd(IntegerOp::Types opType, 
                                              CG_OpndHandle*   value, 
                                              U_32           imm, 
                                              CG_OpndHandle*   addto)  
{ 
    if ((opType == IntegerOp::I4) && (imm <= LEA_EXP_LIMIT)) {
        Type * dstType = irManager.getTypeFromTag(Type::Int32);
        bool addtoIsImm = false;
        if (((Opnd*)addto)->isPlacedIn(OpndKind_Imm))
            addtoIsImm = true;
        Opnd *res;
        if (addtoIsImm) {
            res = irManager.newMemOpnd(dstType, NULL, (Opnd*)value, 
                irManager.newImmOpnd(typeManager.getInt32Type(), 1<<imm), (Opnd*)addto);
        } else {
            res = irManager.newMemOpnd(dstType, (Opnd*)addto, (Opnd*)value, 
                irManager.newImmOpnd(typeManager.getInt32Type(), 1<<imm), NULL);
        }
        res->setMemOpndKind(MemOpndKind_LEA);
        Opnd *dst = irManager.newOpnd(dstType);
        Inst *newInst = irManager.newInstEx(Mnemonic_LEA, 1, dst, res);
        appendInsts(newInst);
        return dst;
    }

    Opnd * shiftDest = (Opnd *)shl(opType, value, irManager.newImmOpnd(typeManager.getUInt8Type(), imm));
    ArithmeticOp::Types atype;
    switch (opType) {
        case IntegerOp::I : atype = ArithmeticOp::I; break;
        case IntegerOp::I8 : atype = ArithmeticOp::I8; break;
        default : atype = ArithmeticOp::I4; break;
    }
    return add(atype, addto, shiftDest);
} 


//_______________________________________________________________________________________________________________
//  Shift left

CG_OpndHandle*    InstCodeSelector::shl(IntegerOp::Types opType, 
                                             CG_OpndHandle*   value,
                                             CG_OpndHandle*   shiftAmount) 
{
    return shiftOp(opType, Mnemonic_SHL, (Opnd*)value, (Opnd*)shiftAmount);
}

//_______________________________________________________________________________________________________________
//  Shift right

CG_OpndHandle* InstCodeSelector::shr(IntegerOp::Types opType,
                                        CG_OpndHandle*   value,
                                        CG_OpndHandle*   shiftAmount) 
{
    return shiftOp(opType, Mnemonic_SAR, (Opnd*)value, (Opnd*)shiftAmount);
}

//_______________________________________________________________________________________________________________
//  Shift right unsigned

CG_OpndHandle*    InstCodeSelector::shru(IntegerOp::Types opType,
                                            CG_OpndHandle* value,
                                            CG_OpndHandle* shiftAmount) 
{
    return shiftOp(opType, Mnemonic_SHR, (Opnd*)value, (Opnd*)shiftAmount);
}

//_______________________________________________________________________________________________________________
//  Select a value: (src1 ? src2 : src3)

CG_OpndHandle*  InstCodeSelector::select(CompareOp::Types     opType,
                                            CG_OpndHandle*       src1,
                                            CG_OpndHandle*       src2,
                                            CG_OpndHandle*       src3) 
{
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
//  Compare two values. Result is an integer.

CG_OpndHandle*  InstCodeSelector::cmp(CompareOp::Operators cmpOp,
                                         CompareOp::Types     opType,
                                         CG_OpndHandle*       src1,
                                         CG_OpndHandle*       src2, int ifNaNResult) 
{
    Opnd * dst=irManager.newOpnd(typeManager.getInt32Type());
    bool swapped=cmpToEflags(cmpOp, opType, (Opnd*)src1, (Opnd*)src2);
    ConditionMnemonic cm=getConditionMnemonicFromCompareOperator(cmpOp, opType);
    if (swapped) 
        cm=swapConditionMnemonic(cm);
    appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, dst, irManager.newImmOpnd(typeManager.getInt32Type(), 0)));
    appendInsts(irManager.newInstEx(getMnemonic(Mnemonic_SETcc, cm), 1, dst,dst));
    if (ifNaNResult == 1 || (ifNaNResult ==0 && ((opType==CompareOp::F) ||(opType==CompareOp::S) ||(opType==CompareOp::D)) && ((cmpOp == CompareOp::Geu) || (cmpOp == CompareOp::Gtu) || (cmpOp == CompareOp::Ne) || (cmpOp == CompareOp::Eq)))) {
        appendInsts(irManager.newInstEx(Mnemonic_CMOVP,1,dst,dst,irManager.newImmOpnd(typeManager.getInt32Type(), ifNaNResult)));
    }
    return dst;
}

//_______________________________________________________________________________________________________________
//    Check if operand is equal to zero. Result is integer.

CG_OpndHandle*    InstCodeSelector::czero(CompareZeroOp::Types opType,
                                             CG_OpndHandle*       src) 
{
    Opnd * dst=irManager.newOpnd(typeManager.getInt32Type());

    cmpToEflags(CompareOp::Eq, getCompareOpTypesFromCompareZeroOpTypes(opType), (Opnd*)src, NULL);
    appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, dst, irManager.newImmOpnd(typeManager.getInt32Type(), 0)));
    appendInsts(irManager.newInstEx(Mnemonic_CMOVZ,  1, dst, dst, irManager.newImmOpnd(typeManager.getInt32Type(), 1)));
    return dst;
}

//_______________________________________________________________________________________________________________
//    Check if operand is not equal to zero. Result is integer.

CG_OpndHandle*    InstCodeSelector::cnzero(CompareZeroOp::Types opType,
                                                CG_OpndHandle*       src) 
{
    Opnd * dst=irManager.newOpnd(typeManager.getInt32Type());
    cmpToEflags(CompareOp::Eq, getCompareOpTypesFromCompareZeroOpTypes(opType), (Opnd*)src, NULL);
    appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, dst, irManager.newImmOpnd(typeManager.getInt32Type(), 0)));
    appendInsts(irManager.newInstEx(Mnemonic_CMOVNZ,     1, dst, dst,irManager.newImmOpnd(typeManager.getInt32Type(), 1)));
    return dst;
}

//_______________________________________________________________________________________________________________
bool InstCodeSelector::cmpToEflags(CompareOp::Operators cmpOp, CompareOp::Types opType,
                                    Opnd * src1, Opnd * src2
                                    )
{
    bool swapped=false;
    switch(opType){
        case CompareOp::I4:
#ifndef _EM64T_
        case CompareOp::I:
        case CompareOp::Ref:
#endif
        {
            Type * srcType=irManager.getTypeFromTag(Type::Int32);
            Opnd * srcOpnd1=(Opnd*)convert(src1, srcType);
            if (!src2){
                appendInsts(irManager.newInst(Mnemonic_TEST, srcOpnd1, srcOpnd1));
            }else{
                Opnd * srcOpnd2=(Opnd*)convert(src2, srcType);
                swapped=swapIfLastIs(srcOpnd1, srcOpnd2);
                appendInsts(irManager.newInst(Mnemonic_CMP, srcOpnd1, srcOpnd2));
            }
            break;
        }
#ifdef _EM64T_
        case CompareOp::I:
        case CompareOp::Ref:
#endif
        case CompareOp::I8:
        {
            Type * srcType=irManager.getTypeFromTag(Type::Int64);
            Opnd * srcOpnd1=(Opnd*)convert(src1, srcType), * srcOpnd2;
            if (!src2) {
                srcOpnd2=irManager.newImmOpnd(srcType, 0);              
            } else {
                srcOpnd2=(Opnd*)convert(src2, srcType);
            }
            swapped=swapIfLastIs(srcOpnd1, srcOpnd2);
#ifndef _EM64T_
            Opnd * dst = irManager.getRegOpnd(RegName_EFLAGS);
            appendInsts(irManager.newI8PseudoInst(Mnemonic_CMP,1,dst,srcOpnd1,srcOpnd2));
#else
            appendInsts(irManager.newInst(Mnemonic_CMP,srcOpnd1,srcOpnd2));
#endif
            irManager.getCGFlags()->earlyDCEOn = true;
            break;
        }
        case CompareOp::D:
        case CompareOp::F:
        {
            Type * srcType=irManager.getTypeFromTag(Type::Double);
            Opnd * srcOpnd1=(Opnd*)convert(src1, srcType);
#ifdef _EM64T_
            Opnd * baseOpnd = irManager.newOpnd(typeManager.getUnmanagedPtrType(srcType));
            Opnd * srcOpnd2=src2?(Opnd*)convert(src2, srcType):irManager.newFPConstantMemOpnd((double)0.0, baseOpnd, (BasicBlock*)currentBasicBlock);
#else
            Opnd * srcOpnd2=src2?(Opnd*)convert(src2, srcType):irManager.newFPConstantMemOpnd((double)0.0);
#endif
            appendInsts(irManager.newInst(Mnemonic_UCOMISD, srcOpnd1, srcOpnd2));
            break;
        }
        case CompareOp::S:
        {
            Type * srcType=irManager.getTypeFromTag(Type::Single);
            Opnd * srcOpnd1=(Opnd*)convert(src1, srcType);
#ifdef _EM64T_
            Opnd * baseOpnd = irManager.newOpnd(typeManager.getUnmanagedPtrType(srcType));
            Opnd * srcOpnd2=src2?(Opnd*)convert(src2, srcType):irManager.newFPConstantMemOpnd((float)0.0, baseOpnd, (BasicBlock*)currentBasicBlock);
#else
            Opnd * srcOpnd2=src2?(Opnd*)convert(src2, srcType):irManager.newFPConstantMemOpnd((float)0.0);
#endif
            appendInsts(irManager.newInst(Mnemonic_UCOMISS, srcOpnd1, srcOpnd2));
            break;
        }
        default:
            ICS_ASSERT(0);
    }
    return swapped;
}

//_______________________________________________________________________________________________________________
//  Branch 

void InstCodeSelector::branch(CompareOp::Operators cmpOp,
                                   CompareOp::Types    opType,
                                   CG_OpndHandle* src1,
                                   CG_OpndHandle* src2) 
{
    bool swapped=cmpToEflags(cmpOp, opType, (Opnd*)src1, (Opnd*)src2);
    ConditionMnemonic cm=getConditionMnemonicFromCompareOperator(cmpOp, opType);
    if (((opType==CompareOp::F) ||(opType==CompareOp::S) ||(opType==CompareOp::D)) && ((cmpOp == CompareOp::Geu) || (cmpOp == CompareOp::Gtu)|| (cmpOp == CompareOp::Ne) || (cmpOp == CompareOp::Eq))) {
        //! branch true&false edges & new block for this branch will be added in CodeSelector::fixNodeInfo
        appendInsts(irManager.newBranchInst(Mnemonic_JP, NULL, NULL)); 
    }
    //! branch true&false edges are added during genTrue|FalseEdge
    appendInsts(irManager.newBranchInst(getMnemonic(Mnemonic_Jcc, swapped?swapConditionMnemonic(cm):cm), NULL, NULL));
}

Opnd*
InstCodeSelector::zeroForComparison(Opnd* target)
{
    POINTER_SIZE_INT zero = 0;
    Type* type = target->getType();
    if(irManager.refsAreCompressed() && type->isReference()) {
        zero = (POINTER_SIZE_INT)VMInterface::getHeapBase();
    }
    if(zero != 0) {
        return heapBaseOpnd(type,zero);
    }
    return NULL;
}

Opnd*
InstCodeSelector::heapBaseOpnd(Type* type, POINTER_SIZE_INT heapBase) {
#ifndef _EM64T_
    assert(0); // not supposed to be used on ia32
#endif
    Opnd* heapBaseOpnd = NULL;
    if((POINTER_SIZE_INT)heapBase == (U_32)heapBase) { // heap base fits into 32 bits
        heapBaseOpnd = irManager.newImmOpnd(type, heapBase);
    } else { // heapBase can not be an immediate at comparison
        heapBaseOpnd = irManager.newOpnd(type);
        // be careful here. if type == Int64 the immediate opnd is returned from copyOpnd.
        // Make sure it is not a problem if you are removing the assert.
        assert(type != typeManager.getInt64Type());
        copyOpnd(heapBaseOpnd,irManager.newImmOpnd(typeManager.getInt64Type(), heapBase));
    }
    return heapBaseOpnd;
}
//_______________________________________________________________________________________________________________
//  Branch if src is zero

void InstCodeSelector::bzero(CompareZeroOp::Types opType, CG_OpndHandle* src) 
{
    Opnd* zeroOp = zeroForComparison((Opnd*)src);
    cmpToEflags(CompareOp::Eq, getCompareOpTypesFromCompareZeroOpTypes(opType), (Opnd*)src, zeroOp);
    //! branch true&false edges are added during genTrue|False edge calls
    appendInsts(irManager.newBranchInst(Mnemonic_JZ, NULL, NULL));
}

//_______________________________________________________________________________________________________________
//  Branch if src is not zero

void InstCodeSelector::bnzero(CompareZeroOp::Types opType, CG_OpndHandle* src) 
{
    Opnd* zeroOp = zeroForComparison((Opnd*)src);
    cmpToEflags(CompareOp::Eq, getCompareOpTypesFromCompareZeroOpTypes(opType), (Opnd*)src, zeroOp);
    //! branch true&false edges are added during genTrue|FalseEdge
    appendInsts(irManager.newBranchInst(Mnemonic_JNZ, NULL, NULL));
}

//_______________________________________________________________________________________________________________
//  Switch: generate compare and branch to the default case.

void InstCodeSelector::tableSwitch(CG_OpndHandle* src, U_32 nTargets) 
{
    switchSrcOpnd=(Opnd*)src;
    switchNumTargets=nTargets;
    branch(CompareOp::Geu, CompareOp::I4, src, irManager.newImmOpnd(typeManager.getUInt32Type(), nTargets));
}

//_______________________________________________________________________________________________________________
//  Generate instructions for switch dispatch
//  SwitchTableInst should be the first inst in the block.

void InstCodeSelector::genSwitchDispatch(U_32 numTargets, Opnd *switchSrc)
{
#ifdef _EM64T_
    Opnd * opnd = irManager.newOpnd(typeManager.getUInt64Type());
    copyOpnd(opnd, switchSrc);
    appendInsts(irManager.newSwitchInst(numTargets, opnd));
#else
    appendInsts(irManager.newSwitchInst(numTargets, switchSrc));
#endif
}

//_______________________________________________________________________________________________________________
//  Throw an exception

void InstCodeSelector::throwException(CG_OpndHandle* exceptionObj, bool createStackTrace) 
{
    Opnd * args[]={ (Opnd*)exceptionObj };
    CallInst * callInst=irManager.newRuntimeHelperCallInst(
        createStackTrace ? 
            VM_RT_THROW_SET_STACK_TRACE : 
            VM_RT_THROW, 
        lengthof(args), args, NULL
    );
    appendInsts(callInst);
}

//_______________________________________________________________________________________________________________
//  Throw system exception

void InstCodeSelector::throwSystemException(CompilationInterface::SystemExceptionId id) 
{
#ifdef _EM64T_
    bool lazy = false;
#else
    bool lazy = true;
#endif
    CallInst * callInst = NULL;
    ObjectType* excType = NULL; 

    switch (id) {
        case CompilationInterface::Exception_NullPointer:
            {
            excType = compilationInterface.findClassUsingBootstrapClassloader(NULL_POINTER_EXCEPTION);
            //callInst=irManager.newRuntimeHelperCallInst(
            //    VM_RT_NULL_PTR_EXCEPTION, 0, NULL, NULL);
            };
            break;
        case CompilationInterface::Exception_ArrayIndexOutOfBounds:
            excType = compilationInterface.findClassUsingBootstrapClassloader(INDEX_OUT_OF_BOUNDS);
            //callInst=irManager.newRuntimeHelperCallInst(
            //    VM_RT_IDX_OUT_OF_BOUNDS, 0, NULL, NULL);
            break;
        case CompilationInterface::Exception_ArrayTypeMismatch:
            excType = compilationInterface.findClassUsingBootstrapClassloader(ARRAY_STORE_EXCEPTION);
            //callInst=irManager.newRuntimeHelperCallInst(
            //    VM_RT_ARRAY_STORE_EXCEPTION, 0, NULL, NULL);
            break;
        case CompilationInterface::Exception_DivideByZero:
            excType = compilationInterface.findClassUsingBootstrapClassloader(DIVIDE_BY_ZERO_EXCEPTION);
            //callInst=irManager.newRuntimeHelperCallInst(
            //    VM_RT_DIVIDE_BY_ZERO_EXCEPTION, 0, NULL, NULL);
            break;
        default:
            assert(0);
    }

    assert(excType);

    if (lazy){
        Opnd * helperOpnds[] = {
            // first parameter exception class
            irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_TypeRuntimeId, excType),
            // second is constructor method handle, 0 - means default constructor
            irManager.newImmOpnd(getRuntimeIdType(), 0)
        };
        callInst=irManager.newRuntimeHelperCallInst(
            VM_RT_THROW_LAZY, lengthof(helperOpnds), helperOpnds, NULL);
        appendInsts(callInst);
    } else {
        Opnd * helperOpnds1[] = {
            irManager.newImmOpnd(typeManager.getInt32Type(), Opnd::RuntimeInfo::Kind_Size, excType),
            irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_AllocationHandle, excType)
        };
        Opnd * retOpnd=irManager.newOpnd(excType);
        callInst=irManager.newRuntimeHelperCallInst(
            VM_RT_NEW_RESOLVED_USING_VTABLE_AND_SIZE,
            lengthof(helperOpnds1), helperOpnds1, retOpnd);
        appendInsts(callInst);

        MethodDesc* md = compilationInterface.resolveMethod( excType, 
                DEFAUlT_COSTRUCTOR_NAME, DEFAUlT_COSTRUCTOR_DESCRIPTOR);
        
        Opnd * target=irManager.newImmOpnd(typeManager.getInt32Type(), Opnd::RuntimeInfo::Kind_MethodDirectAddr, md);
        Opnd * helperOpnds2[] = { (Opnd*)retOpnd };
        callInst=irManager.newCallInst(target, irManager.getDefaultManagedCallingConvention(), 
            lengthof(helperOpnds2), helperOpnds2, NULL);
        appendInsts(callInst);

        Opnd * helperOpnds3[] = { (Opnd*)retOpnd };
        callInst=irManager.newRuntimeHelperCallInst(
            VM_RT_THROW, 
            lengthof(helperOpnds3), helperOpnds3, NULL);
        appendInsts(callInst);
    }
}

//_______________________________________________________________________________________________________________
//  Load 32-bit integer constant

CG_OpndHandle*    InstCodeSelector::ldc_i4(I_32 val) 
{
    return irManager.newImmOpnd(typeManager.getInt32Type(), val);
}

//_______________________________________________________________________________________________________________
//  Load 64-bit integer constant

CG_OpndHandle*    InstCodeSelector::ldc_i8(int64 val) 
{ 
#ifndef _EM64T_ 
    return irManager.newImmOpnd(typeManager.getInt64Type(), val);
#else
    Opnd * tmp = irManager.newOpnd(typeManager.getInt64Type());
    copyOpnd(tmp, irManager.newImmOpnd(typeManager.getInt64Type(), val));
    return tmp;
#endif
}   

//_______________________________________________________________________________________________________________
//  get vtable constant (a constant pointer)

CG_OpndHandle* InstCodeSelector::getVTableAddr(Type *       dstType, ObjectType * base) 
{
#ifndef _EM64T_
    return irManager.newImmOpnd(dstType, Opnd::RuntimeInfo::Kind_VTableConstantAddr, base);
#else
    Opnd * acc = irManager.newOpnd(dstType);
    copyOpnd(acc, irManager.newImmOpnd(dstType, Opnd::RuntimeInfo::Kind_VTableConstantAddr, base));
    return acc;
#endif
}

//_______________________________________________________________________________________________________________
//  get java.lang.Object

CG_OpndHandle* InstCodeSelector::getClassObj(Type *       dstType, ObjectType * base) 
{
    Opnd * helperOpnds[]={irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_TypeRuntimeId, base)};
    Opnd * retOpnd=irManager.newOpnd(dstType);
    CallInst * callInst=irManager.newRuntimeHelperCallInst(VM_RT_CLASS_2_JLC,
        1, helperOpnds, retOpnd);
    appendInsts(callInst);
    return retOpnd;
}

//_______________________________________________________________________________________________________________
//  Load double FP constant (unoptimized straightforward version)

CG_OpndHandle* InstCodeSelector::ldc_s(float val) 
{
#ifndef _EM64T_
    return irManager.newFPConstantMemOpnd(val);
#else
    Opnd * baseOpnd = irManager.newOpnd(typeManager.getUnmanagedPtrType(typeManager.getSingleType()));
    Opnd * dst = irManager.newOpnd(typeManager.getSingleType());
    copyOpnd(dst, irManager.newFPConstantMemOpnd(val, baseOpnd, (BasicBlock*)currentBasicBlock));
    return dst;
#endif
}

//_______________________________________________________________________________________________________________
//  Load double FP constant (unoptimized straightforward version)

CG_OpndHandle*    InstCodeSelector::ldc_d(double val) 
{
#ifndef _EM64T_
    return irManager.newFPConstantMemOpnd(val);
#else
    Opnd * baseOpnd = irManager.newOpnd(typeManager.getUnmanagedPtrType(typeManager.getDoubleType()));
    Opnd * dst = irManager.newOpnd(typeManager.getDoubleType());
    copyOpnd(dst, irManager.newFPConstantMemOpnd(val, baseOpnd, (BasicBlock*)currentBasicBlock));
    return dst;
#endif
}

//_______________________________________________________________________________________________________________
//  Load Null

CG_OpndHandle*    InstCodeSelector::ldnull(bool compressed) {
    if (compressed) {
        return irManager.newImmOpnd(typeManager.getCompressedNullObjectType(), 0);
    } else {
        if (irManager.refsAreCompressed()) {
            return irManager.newImmOpnd(typeManager.getNullObjectType(), (POINTER_SIZE_INT)VMInterface::getHeapBase());
        } else {
            return irManager.newImmOpnd(typeManager.getNullObjectType(), 0);
        }
    }
}

//_______________________________________________________________________________________________________________
//  Throw null pointer exception if base is NULL.

CG_OpndHandle* InstCodeSelector::tau_checkNull(CG_OpndHandle* base, bool checksThisForInlinedMethod) 
{
    appendInsts(irManager.newSystemExceptionCheckPseudoInst(CompilationInterface::Exception_NullPointer, (Opnd*)base, 0, checksThisForInlinedMethod));
    return getTauUnsafe();
}

//_______________________________________________________________________________________________________________
//  Throw index out of range exception if index is larger than array length

CG_OpndHandle* InstCodeSelector::tau_checkBounds(CG_OpndHandle* arrayLen, 
                                                    CG_OpndHandle* index) 
{
    Type * type1 = ((Opnd*)arrayLen)->getType();
    Type * type2 = ((Opnd*)index)->getType();

    CompareOp::Types cmpType;
    if (type1->isInt4() && type2->isInt4()) 
        cmpType = CompareOp::I4;
    else if (type1->isInt8()) {
        assert(type2->isInt8());
        cmpType = CompareOp::I8;
    } else 
        cmpType = CompareOp::I;

    Node* throwBasicBlock = irManager.getFlowGraph()->createBlockNode();

    ObjectType* excType = compilationInterface.findClassUsingBootstrapClassloader(INDEX_OUT_OF_BOUNDS);
    irManager.throwException(excType, currentHIRInstBCOffset, throwBasicBlock);

    //Inst* throwInst = irManager.newRuntimeHelperCallInst(VM_RT_IDX_OUT_OF_BOUNDS, 0, NULL, NULL);
    //throwInst->setBCOffset(currentHIRInstBCOffset);
    //throwBasicBlock->appendInst(throwInst);

    branch(CompareOp::Geu, cmpType, (Opnd*)index, (Opnd*)arrayLen);
    codeSelector.genTrueEdge(currentBasicBlock, throwBasicBlock, 0);
    return getTauUnsafe();
}

//_______________________________________________________________________________________________________________
//  Throw index out of range exception if (a > b), unsigned if pointer
//  Ignore tau argument.

CG_OpndHandle* InstCodeSelector::tau_checkLowerBound(CG_OpndHandle* a,
                                                        CG_OpndHandle* b) 
{
    Type * type1 = ((Opnd*)a)->getType();
    Type * type2 = ((Opnd*)b)->getType();

    CompareOp::Types cmpType;
    CompareOp::Operators cmpOp; 
    if (type1->isInt4() && type2->isInt4()) {
        cmpType = CompareOp::I4;
        cmpOp = CompareOp::Gt;
    }else if (type1->isInt8()) {
        assert(type2->isInt8());
        cmpType = CompareOp::I8;
        cmpOp = CompareOp::Gt;
    } else {
        cmpType = CompareOp::I;
        cmpOp = CompareOp::Gtu;
    }
    Node* throwBasicBlock = irManager.getFlowGraph()->createBlockNode();
    ObjectType* excType = compilationInterface.findClassUsingBootstrapClassloader(INDEX_OUT_OF_BOUNDS);
    irManager.throwException(excType, currentHIRInstBCOffset, throwBasicBlock);
    
    //Inst* throwInst = irManager.newRuntimeHelperCallInst(VM_RT_IDX_OUT_OF_BOUNDS, 0, NULL, NULL);
    //throwInst->setBCOffset(currentHIRInstBCOffset);
    //throwBasicBlock->appendInst(throwInst);
    
    branch(cmpOp, cmpType, (Opnd*)a, (Opnd*)b);
    codeSelector.genTrueEdge(currentBasicBlock, throwBasicBlock, 0);
    return getTauUnsafe();
}

//_______________________________________________________________________________________________________________
//  Throw index out of range exception if (index >=arrayLen (upper bound))
//  Ignore tau argument

CG_OpndHandle* InstCodeSelector::tau_checkUpperBound(CG_OpndHandle* index,
                                                        CG_OpndHandle* arrayLen) 
{
    return tau_checkBounds(arrayLen, index);
}

//_______________________________________________________________________________________________________________
//  Throw array type mismatch exception if src's type is not subclass of
//  array's element type

CG_OpndHandle* InstCodeSelector::tau_checkElemType(CG_OpndHandle* array, 
                                                      CG_OpndHandle* src,
                                                      CG_OpndHandle* tauNullChecked, 
                                                      CG_OpndHandle* tauIsArray) 
{
    Node* throwBasicBlock = irManager.getFlowGraph()->createBlockNode();
    assert(currentHIRInstBCOffset!=ILLEGAL_BC_MAPPING_VALUE);
    ObjectType* excType = compilationInterface.findClassUsingBootstrapClassloader(ARRAY_STORE_EXCEPTION);
    irManager.throwException(excType, currentHIRInstBCOffset, throwBasicBlock);
    //Inst* throwInst = irManager.newRuntimeHelperCallInst(VM_RT_ARRAY_STORE_EXCEPTION, 0, NULL, NULL);
    //throwInst->setBCOffset(currentHIRInstBCOffset);
    //throwBasicBlock->appendInst(throwInst);

    Opnd * args[] = { (Opnd*)src, (Opnd*)array };
    Opnd * flag = irManager.newOpnd(typeManager.getInt32Type());

    appendInsts(irManager.newRuntimeHelperCallInst(
                VM_RT_AASTORE_TEST, lengthof(args), args, flag));

    // the helper returns boolean value
    bzero(CompareZeroOp::I4, flag);
    codeSelector.genTrueEdge(currentBasicBlock, throwBasicBlock, 0);

    return getTauUnsafe();
}

//_______________________________________________________________________________________________________________
//  Throw DivideByZeroException if checkZero's argument is 0

CG_OpndHandle*  InstCodeSelector::tau_checkZero(CG_OpndHandle* src) 
{
    Node* throwBasicBlock = irManager.getFlowGraph()->createBlockNode();
    assert(currentHIRInstBCOffset!=ILLEGAL_BC_MAPPING_VALUE);
    ObjectType* excType = compilationInterface.findClassUsingBootstrapClassloader(DIVIDE_BY_ZERO_EXCEPTION);
    irManager.throwException(excType, currentHIRInstBCOffset, throwBasicBlock);
    //Inst* throwInst = irManager.newRuntimeHelperCallInst(VM_RT_DIVIDE_BY_ZERO_EXCEPTION, 0, NULL, NULL);
    //throwInst->setBCOffset(currentHIRInstBCOffset);
    //throwBasicBlock->appendInst(throwInst);
    Opnd * srcOpnd = (Opnd*)src;
    Type * type = srcOpnd->getType();
    CompareZeroOp::Types opType = CompareZeroOp::I;
    switch (type->tag) {
        case Type::Int32:
            opType = CompareZeroOp::I4; break;
        case Type::Int64:
            opType = CompareZeroOp::I8; break;
        case Type::IntPtr:
            opType = CompareZeroOp::I;  break;
        default:
            assert(0);
    }
    bzero(opType, src);
    codeSelector.genTrueEdge(currentBasicBlock, throwBasicBlock, 0);
    return getTauUnsafe();
}

//_______________________________________________________________________________________________________________
//  Throw an exception if the result of integer division
//    cannot be represented in the result type, that is, if
//    src1 is minint and src2 is -1

CG_OpndHandle* InstCodeSelector::tau_checkDivOpnds(CG_OpndHandle* src1, 
                                                      CG_OpndHandle* src2) 
{
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
//  Add immediate offset to an operand

Opnd* InstCodeSelector::addOffset(Opnd *   addr, 
                                        Opnd *   base, 
                                        U_32      offset) 
{
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
CG_OpndHandle* InstCodeSelector::uncompressRef(CG_OpndHandle *compref) 
{ 
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
CG_OpndHandle* InstCodeSelector::compressRef(CG_OpndHandle *ref)
{ 
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
CG_OpndHandle * InstCodeSelector::ldFieldOffset(FieldDesc *fieldDesc)
{
    return irManager.newImmOpnd(typeManager.getIntPtrType(), Opnd::RuntimeInfo::Kind_FieldOffset, fieldDesc);
}

//_______________________________________________________________________________________________________________
CG_OpndHandle * InstCodeSelector::ldFieldOffsetPlusHeapbase(FieldDesc *fieldDesc)
{ 
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
CG_OpndHandle * InstCodeSelector::ldArrayBaseOffset(Type *elemType)
{ 
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
CG_OpndHandle * InstCodeSelector::ldArrayBaseOffsetPlusHeapbase(Type *elemType)
{ 
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
CG_OpndHandle * InstCodeSelector::ldArrayLenOffset(Type *elemType)
{ 
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
CG_OpndHandle * InstCodeSelector::ldArrayLenOffsetPlusHeapbase(Type *elemType)
{ 
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
CG_OpndHandle * InstCodeSelector::addOffset(Type *pointerType, CG_OpndHandle* refHandle,
                               CG_OpndHandle* offsetHandle)
{ 
    Opnd* base = (Opnd*)refHandle;
    Opnd* offsetOpnd = (Opnd*)offsetHandle;
    Type* fieldRefType = pointerType;
    assert(base->getType()->isObject());
    assert(offsetOpnd->getType()->isInteger());
    assert(fieldRefType->isManagedPtr());

#ifdef _EM64T_
    return simpleOp_I8(Mnemonic_ADD, fieldRefType, (Opnd*)base, offsetOpnd);
#else
    return simpleOp_I4(Mnemonic_ADD, fieldRefType, (Opnd*)base, offsetOpnd);
#endif
}

//_______________________________________________________________________________________________________________
CG_OpndHandle * InstCodeSelector::addOffsetPlusHeapbase(Type *pointerType, 
                                           CG_OpndHandle* compRefHandle, 
                                           CG_OpndHandle* offsetPlusHeapbaseHandle)
{ 
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
//  Load address of the field at "base + offset"

CG_OpndHandle* InstCodeSelector::ldFieldAddr(Type*          fieldRefType,
                                                CG_OpndHandle* base, 
                                                FieldDesc *    fieldDesc) 
{
    Opnd * offsetOpnd=(Opnd*)ldFieldOffset(fieldDesc);
#ifdef _EM64T_
    return simpleOp_I8(Mnemonic_ADD, fieldRefType, (Opnd*)base, offsetOpnd);
#else
    return simpleOp_I4(Mnemonic_ADD, fieldRefType, (Opnd*)base, offsetOpnd);
#endif
}

//_______________________________________________________________________________________________________________
//  Load static field address

CG_OpndHandle*    InstCodeSelector::ldStaticAddr(Type* fieldRefType, FieldDesc *fieldDesc) 
{
#ifndef _EM64T_
    Opnd * addr=irManager.newImmOpnd(fieldRefType, Opnd::RuntimeInfo::Kind_StaticFieldAddress, fieldDesc);
#else
    Opnd* immOp = irManager.newImmOpnd(fieldRefType, Opnd::RuntimeInfo::Kind_StaticFieldAddress, fieldDesc);
    Opnd* addr =  irManager.newOpnd(typeManager.getManagedPtrType(fieldRefType));
    appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, addr, immOp));
#endif
    return addr;
}

//_______________________________________________________________________________________________________________
//  Compute address of the first array element

CG_OpndHandle*  InstCodeSelector::ldElemBaseAddr(CG_OpndHandle* array) 
{
    ArrayType * arrayType=((Opnd*)array)->getType()->asArrayType();
    Type * elemType=arrayType->getElementType();
#ifdef _EM64T_
    if (elemType->isReference()
        && Type::isCompressedReference(elemType->tag, compilationInterface) 
        && !elemType->isCompressedReference()) {
        elemType = typeManager.compressType(elemType);
    }
    Type * offType = typeManager.getInt64Type();
#else
    Type * offType = typeManager.getInt32Type();
#endif
    Type * dstType=irManager.getManagedPtrType(elemType);
    Opnd * dst=irManager.newOpnd(dstType);
    appendInsts(irManager.newInstEx(Mnemonic_ADD, 1, dst, (Opnd*)array, irManager.newImmOpnd(offType, arrayType->getArrayElemOffset())));
    return dst;
}

//_______________________________________________________________________________________________________________
//  Compute address of the array element given 
//  address of the first element and index
//  using 'LEA' instruction

CG_OpndHandle*  InstCodeSelector::addElemIndexWithLEA(Type          * eType,
                                                      CG_OpndHandle * array,
                                                      CG_OpndHandle * index) 
{
    ArrayType * arrayType=((Opnd*)array)->getType()->asArrayType();
    Type * elemType=arrayType->getElementType();
    Type * dstType=irManager.getManagedPtrType(elemType);

#ifdef _EM64T_
    Type * indexType = typeManager.getInt64Type();
    Type * offType = typeManager.getInt64Type();
#else
    Type * indexType = typeManager.getInt32Type();
    Type * offType = typeManager.getInt32Type();
#endif
        
    U_32 elemSize = 0;
    if (elemType->isReference()
        && Type::isCompressedReference(elemType->tag, compilationInterface)
        && !elemType->isCompressedReference()) {
        elemSize = 4;
    } else {
        elemSize = getByteSize(irManager.getTypeSize(elemType));
    }
    Opnd * elemSizeOpnd  = irManager.newImmOpnd(indexType, elemSize);
    
    Opnd * indexOpnd = (Opnd *)index;
    indexOpnd = convert(indexOpnd, indexType);

    if ( indexOpnd->isPlacedIn(OpndKind_Imm) ) {
        // we need to put index operand on a register to satisfy LEA constraint
        int64 immValue = indexOpnd->getImmValue();
        if (immValue == 0) {
            indexOpnd = NULL;
            elemSizeOpnd = NULL;
        } else {
            Opnd * indexReg = irManager.newOpnd(indexType);
            copyOpnd(indexReg,indexOpnd);
            indexOpnd = indexReg;
        }
    }    
    Opnd * addr = irManager.newMemOpnd(dstType,(Opnd*)array, indexOpnd, elemSizeOpnd,
                                       irManager.newImmOpnd(offType, arrayType->getArrayElemOffset())
                                       );
    Opnd * dst = irManager.newOpnd(dstType);
    appendInsts(irManager.newInstEx(Mnemonic_LEA, 1, dst, addr));
    return dst;
}

//_______________________________________________________________________________________________________________
//  Compute address of the array element given 
//  address of the first element and index
//  using 'ADD' instruction

CG_OpndHandle*  InstCodeSelector::addElemIndex(Type          * eType,
                                               CG_OpndHandle * elemBase,
                                               CG_OpndHandle * index) 
{
    Type* type = ((Opnd*)elemBase)->getType();
    PtrType * ptrType=type->asPtrType();
    Type * elemType = ptrType->getPointedToType();

    U_32 elemSize=getByteSize(irManager.getTypeSize(elemType));

    Type * indexType = 
#ifdef _EM64T_
        typeManager.getInt64Type();
#else
        typeManager.getInt32Type();
#endif
        
    Opnd * indexOpnd=(Opnd *)index;
    indexOpnd=convert(indexOpnd, indexType);
    
    Opnd * scaledIndexOpnd=NULL;
    if (indexOpnd->isPlacedIn(OpndKind_Imm)) {
        scaledIndexOpnd=irManager.newImmOpnd(indexType, indexOpnd->getImmValue() * elemSize);
    //} else if (elemSize == 1 && ptrType->isUnmanagedPtr()) {
    //    scaledIndexOpnd = indexOpnd;
    } else {
        scaledIndexOpnd=irManager.newOpnd(indexType);
        appendInsts(irManager.newInstEx(Mnemonic_IMUL, 1, scaledIndexOpnd, indexOpnd, irManager.newImmOpnd(indexType, elemSize)));
    }
    Opnd * dst=irManager.newOpnd(ptrType);
    appendInsts(irManager.newInstEx(Mnemonic_ADD, 1, dst, (Opnd*)elemBase, scaledIndexOpnd));
    return dst;
}

//_______________________________________________________________________________________________________________
//  Load array length

CG_OpndHandle*    InstCodeSelector::tau_arrayLen(Type *         dstType, 
                                                ArrayType *    arrayType,
                                                Type *         lenType,
                                                CG_OpndHandle* base,
                                                CG_OpndHandle* tauArrayNonNull, 
                                                CG_OpndHandle* tauIsArray) 
{
    Opnd * offset=irManager.newImmOpnd(typeManager.getInt32Type(), arrayType->getArrayLengthOffset());
    Opnd * addr=irManager.newMemOpnd(lenType, (Opnd*)base, 0, 0, offset);
    Opnd * dst=irManager.newOpnd(typeManager.getInt32Type());
    appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, dst, addr));
    return dst;
}

//_______________________________________________________________________________________________________________
//  Simple load indirect -- load primitive value

CG_OpndHandle* InstCodeSelector::simpleLdInd(Type * dstType, Opnd * addr,
                                                Type::Tag memType,
                                                Opnd * baseTau,
                                                Opnd * offsetTau) 
{
#ifdef _EM64T_
    if(irManager.refsAreCompressed() && memType > Type::Float && memType!=Type::UnmanagedPtr) {
        Opnd * opnd = irManager.newMemOpndAutoKind(typeManager.getInt32Type(), addr);
        Opnd * dst = irManager.newOpnd(typeManager.getInt64Type());
        // loading compressed 32-bit managed address, ensure zero-extention
        copyOpnd(dst, opnd, true);
        // uncompress
        Type* unmanagedPtrType = typeManager.getUnmanagedPtrType(typeManager.getInt8Type());
        Opnd* heapBase = heapBaseOpnd(unmanagedPtrType, (POINTER_SIZE_INT)VMInterface::getHeapBase());
        dst = simpleOp_I8(Mnemonic_ADD, dstType, dst, heapBase);
        return dst;
    } 
    else 
#endif
    {
        Opnd * opnd = irManager.newMemOpndAutoKind(irManager.getTypeFromTag(memType), addr);
        Opnd * dst = irManager.newOpnd(dstType);
        copyOpnd(dst, opnd);
        return dst;
    }
}

//_______________________________________________________________________________________________________________
//  Simple store indirect -- store primitive value

void InstCodeSelector::simpleStInd(Opnd * addr, 
                                      Opnd * src,
                                      Type::Tag memType,
                                      bool      autoCompressRef,
                                      Opnd * baseTau,
                                      Opnd * offsetAndTypeTau) 
{
#ifdef _EM64T_
    // unmanaged pointers are never being compressed
    // Actually, there is only one possible case caused by magics:
    // unmanaged pointer to Int8
    if(irManager.refsAreCompressed() && memType > Type::Float && !src->getType()->isUnmanagedPtr()) {
        Type * unmanagedPtrType = typeManager.getUnmanagedPtrType(typeManager.getInt8Type());
        Opnd * heap_base = heapBaseOpnd(unmanagedPtrType, (POINTER_SIZE_INT)VMInterface::getHeapBase());
        Opnd * compressed_src = irManager.newOpnd(typeManager.compressType(src->getType()));
        Opnd * opnd = irManager.newMemOpndAutoKind(typeManager.compressType(src->getType()), addr);
        appendInsts(irManager.newInstEx(Mnemonic_SUB, 1, compressed_src, src, heap_base));
        appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, opnd, compressed_src));
    } else
#endif
    {
        assert( !src->getType()->isUnmanagedPtr() || src->getType()->asPtrType()->getPointedToType()->isInt1());
        Opnd * dst = irManager.newMemOpndAutoKind(irManager.getTypeFromTag(memType), addr);
        copyOpnd(dst, src);
    }
} 

//_______________________________________________________________________________________________________________
//  Load static field

CG_OpndHandle* InstCodeSelector::ldStatic(Type *     dstType, 
                                             FieldDesc* fieldDesc, 
                                             Type::Tag  fieldType,
                                             bool       autoUncompressRef) 
{
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
//  Load field

CG_OpndHandle* InstCodeSelector::tau_ldField(Type *         dstType,
                                            CG_OpndHandle* base, 
                                            Type::Tag      fieldType, 
                                            FieldDesc*     fieldDesc,
                                            bool           autoUncompressRef,
                                            CG_OpndHandle* tauBaseNonNull,
                                            CG_OpndHandle* tauBaseTypeHasField) 
{
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
//  Load array element

CG_OpndHandle*  InstCodeSelector::tau_ldElem(Type *         dstType,
                                             CG_OpndHandle* array,
                                             CG_OpndHandle* index,
                                             bool autoUncompressRef,
                                             CG_OpndHandle* tauBaseNonNull, 
                                             CG_OpndHandle* tauIdxIsInBounds) 
{
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
//  Store static field
//  tauFieldTypeChecked is a redundant argument and should go away

void InstCodeSelector::tau_stStatic(CG_OpndHandle*   src,  
                                   FieldDesc*       fieldDesc,
                                   Type::Tag        fieldType,
                                   bool             autoCompressRef,
                                   CG_OpndHandle*   tauFieldTypeChecked) 
{
    ICS_ASSERT(0);
}

//_______________________________________________________________________________________________________________
//  Store field

void InstCodeSelector::tau_stField(CG_OpndHandle*src, 
                                  CG_OpndHandle* base, 
                                  Type::Tag fieldType,
                                  FieldDesc *fieldDesc,
                                  bool autoCompressRef,
                                  CG_OpndHandle* tauBaseNonNull, 
                                  CG_OpndHandle* tauBaseTypeHasField,
                                  CG_OpndHandle* tauFieldHasType) 
{
    ICS_ASSERT(0);
}

//_______________________________________________________________________________________________________________
//  Store array element

void InstCodeSelector::tau_stElem(CG_OpndHandle* src,
                                 CG_OpndHandle* array, 
                                 CG_OpndHandle* index,
                                 bool autoCompressRef,
                                 CG_OpndHandle* tauBaseNonNull,
                                 CG_OpndHandle* tauAddressInRange, 
                                 CG_OpndHandle* tauElemTypeChecked) 
{
    ICS_ASSERT(0);
}

//_______________________________________________________________________________________________________________
//  Load indirect

CG_OpndHandle* InstCodeSelector::tau_ldInd(Type* dstType, CG_OpndHandle* ptr, 
                                          Type::Tag memType,
                                          bool autoUncompressRef,
                                          bool speculateLoad,
                                          CG_OpndHandle* tauBaseNonNull,
                                          CG_OpndHandle* tauAddressInRange) 
{
    return simpleLdInd(dstType, (Opnd*)ptr, memType, (Opnd*)tauBaseNonNull, (Opnd*)tauAddressInRange);
}

//_______________________________________________________________________________________________________________
//  Store indirect

void InstCodeSelector::tau_stInd(CG_OpndHandle* src, 
                                    CG_OpndHandle* ptr, 
                                    Type::Tag      memType, 
                                    bool           autoCompressRef,
                                    CG_OpndHandle* tauBaseNonNull,
                                    CG_OpndHandle* tauAddressInRange, 
                                    CG_OpndHandle* tauElemTypeChecked) 
{
    return simpleStInd((Opnd*)ptr, (Opnd*)src, memType, autoCompressRef, (Opnd*)tauBaseNonNull, (Opnd*)tauElemTypeChecked);
}

void InstCodeSelector::tau_stRef(CG_OpndHandle* src,
                                    CG_OpndHandle* ptr, 
                                    CG_OpndHandle* base,
                                    Type::Tag      memType, 
                                    bool           autoCompressRef,
                                    CG_OpndHandle* tauBaseNonNull,
                                    CG_OpndHandle* tauAddressInRange, 
                                    CG_OpndHandle* tauElemTypeChecked)
{
    Opnd* helperOpnds[] = {(Opnd*)base,(Opnd*)ptr,(Opnd*)src};
    CallInst * callInst = irManager.newRuntimeHelperCallInst(
                                        VM_RT_GC_HEAP_WRITE_REF,
                                        3,helperOpnds,NULL);
    appendInsts(callInst);
}

//_______________________________________________________________________________________________________________
//  Load variable address

CG_OpndHandle* InstCodeSelector::ldVarAddr(U_32 varId) 
{
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
//  Load variable

CG_OpndHandle * InstCodeSelector::ldVar(Type* dstType, U_32 varId) 
{
    if (dstType->tag==Type::Tau)
        return getTauUnsafe();
    Opnd * dst=irManager.newOpnd(dstType);
    copyOpnd(dst, irManager.getOpnd(varId));    
    return dst;
}

//_______________________________________________________________________________________________________________
//  Store variable

void InstCodeSelector::stVar(CG_OpndHandle* src, U_32 varId) 
{
    if (src==getTauUnsafe())
        return;
    copyOpnd(irManager.getOpnd(varId), (Opnd*)src); 
}

//_______________________________________________________________________________________________________________
//  Create new object

CG_OpndHandle* InstCodeSelector::newObj(ObjectType* objType) 
{
    Opnd * helperOpnds[] = {
        irManager.newImmOpnd(typeManager.getInt32Type(), Opnd::RuntimeInfo::Kind_Size, objType),
        irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_AllocationHandle, objType)
    };
    Opnd * retOpnd=irManager.newOpnd(objType);

    CallInst * callInst=irManager.newRuntimeHelperCallInst(
        VM_RT_NEW_RESOLVED_USING_VTABLE_AND_SIZE,
        2, helperOpnds, retOpnd);
    appendInsts(callInst);
    return retOpnd;
}

//_______________________________________________________________________________________________________________
//  Create new array
//    Call Helper_NewArray(allocationHandle, arrayLength)

CG_OpndHandle* InstCodeSelector::newArray(ArrayType*      arrayType,
                                             CG_OpndHandle* numElems) 
{
    Opnd * helperOpnds[] = {
        (Opnd*)numElems,
        irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_AllocationHandle, arrayType)
    };
    Opnd * retOpnd=irManager.newOpnd(arrayType);
    CallInst * callInst=irManager.newRuntimeHelperCallInst(
        VM_RT_NEW_VECTOR_USING_VTABLE,
        2, helperOpnds, retOpnd);
    appendInsts(callInst);
    return retOpnd;
}

//_______________________________________________________________________________________________________________
//    Create multidimentional new array
//    Call Helper_NewMultiArray(arrayTypeRuntimeId, numDims, U_32 dimN, ...,U_32 dim1)    

CG_OpndHandle*  InstCodeSelector::newMultiArray(ArrayType*      arrayType, 
                                                     U_32          numDims, 
                                                     CG_OpndHandle** dims) 
{
    Opnd ** helperOpnds = new (memManager) Opnd * [2+numDims];
    helperOpnds[0]=irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_TypeRuntimeId, arrayType);
    helperOpnds[1]=irManager.newImmOpnd(typeManager.getInt32Type(), numDims);
    for (U_32 i = 0; i < numDims; i++) 
        helperOpnds[i + 2] = (Opnd*)dims[numDims - 1 - i];
    Opnd * retOpnd=irManager.newOpnd(arrayType);
    CallInst * callInst=irManager.newRuntimeHelperCallInst(
        VM_RT_MULTIANEWARRAY_RESOLVED,
        2+numDims, helperOpnds, retOpnd);
    appendInsts(callInst);
    return retOpnd;
}

//_______________________________________________________________________________________________________________
//  Load reference (can also be a string)

CG_OpndHandle* InstCodeSelector::ldRef(Type *dstType,
                                       MethodDesc* enclosingMethod,
                                       U_32 refToken,
                                       bool uncompress) 
{
    assert(dstType->isSystemString() || dstType->isSystemClass());
    Opnd * retOpnd=irManager.newOpnd(dstType);

    if (codeSelector.getFlags().slowLdString || dstType->isSystemClass() ||
        *((POINTER_SIZE_INT *) compilationInterface.getStringInternAddr(enclosingMethod, refToken)) == 0) {
        NamedType * parentType=enclosingMethod->getParentType();
    #ifdef _EM64T_
        Opnd * tp = irManager.getRegOpnd(RegName_RDI);
        appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, tp,irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_TypeRuntimeId, parentType)));
        Opnd * st = irManager.getRegOpnd(RegName_RSI);
        appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, st, irManager.newImmOpnd(typeManager.getInt64Type(), refToken)));
        Opnd * helperOpnds[] = {
            tp,
            st
        };
    #else
        Opnd * helperOpnds[] = {
            irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_TypeRuntimeId, parentType),
            irManager.newImmOpnd(typeManager.getInt32Type(), refToken)
        };
    #endif

        CallInst * callInst = irManager.newRuntimeHelperCallInst(VM_RT_LDC_STRING, 2, helperOpnds, retOpnd);
        appendInsts(callInst);
    } else {
        // this optimized version is based on determining item address at compile time.
        // Respective compile time helper (class_get_const_string_intern_addr) is available for strings.
        // Similar function for literal constants is not ready.
        // TODO: rewrite this as soon as the helper for loading ref addr at compile time is ready.

        Type* objectType = irManager.getTypeFromTag(Type::Object);
        if(irManager.refsAreCompressed()) {
            Opnd * base = irManager.newOpnd(objectType);
            copyOpnd(base, irManager.newImmOpnd(objectType, (POINTER_SIZE_INT)VMInterface::getHeapBase()));
            Opnd * tmp = irManager.newImmOpnd(irManager.getTypeFromTag(Type::UInt64),
                                              Opnd::RuntimeInfo::Kind_StringAddress,
                                              enclosingMethod, (void*)(POINTER_SIZE_INT)refToken);
            Opnd * ptr;
            if  (uncompress) {
                ptr = irManager.newOpnd(objectType);
                copyOpnd(ptr,tmp);
            } else {
                ptr = simpleOp_I8(Mnemonic_ADD, objectType,base,tmp);
            }
            
            Opnd* memOpnd = irManager.newMemOpnd(typeManager.getSystemStringType(), MemOpndKind_Heap,
                                                 ptr, NULL, NULL, NULL); 
            retOpnd = simpleOp_I8(Mnemonic_ADD, memOpnd->getType(), memOpnd, base);
        } else {
#ifdef _EM64T_ // in uncompressed mode the ptr can be greater than MAX_INT32 so it can not be an immediate
            Opnd * tmp = irManager.newImmOpnd(irManager.getTypeFromTag(Type::UInt64),
                                              Opnd::RuntimeInfo::Kind_StringAddress,
                                              enclosingMethod, (void*)(POINTER_SIZE_INT)refToken);
            Opnd* ptr = irManager.newOpnd(objectType);
    //        Opnd* ptr = irManager.newOpnd(typeManager.getUnmanagedPtrType(typeManager.getSystemStringType()));
            copyOpnd(ptr,tmp);
            Opnd* memOpnd = irManager.newMemOpnd(typeManager.getSystemStringType(), MemOpndKind_Heap,
                                                 ptr, NULL, NULL,  NULL); 
#else
            Opnd* ptr = irManager.newImmOpnd(objectType, Opnd::RuntimeInfo::Kind_StringAddress,
                                             enclosingMethod, (void*)(POINTER_SIZE_INT)refToken);
            Opnd* memOpnd = irManager.newMemOpnd(typeManager.getSystemStringType(), MemOpndKind_Heap,
                                                 NULL, NULL, NULL,  ptr); 
#endif
            copyOpnd(retOpnd, memOpnd);
        }
    }

    return retOpnd;
}

//_______________________________________________________________________________________________________________
//  Increment counter for the program instrumentation

void InstCodeSelector::incCounter(Type *counterType,U_32 key) 
{
    assert( counterType->isUInt4() );
    EdgeMethodProfile* edgeProfile = this->codeSelector.methodCodeSelector.edgeProfile;
    assert(edgeProfile!=NULL);
    U_32* ptr = key == 0 ? edgeProfile->getEntryCounter() : edgeProfile->getCounter(key );
    assert( ptr != NULL /*&& *ptr == 0 if compilation is locked*/);
    Opnd* baseOpnd = irManager.newImmOpnd(typeManager.getUnmanagedPtrType(typeManager.getUIntPtrType()), (POINTER_SIZE_INT)ptr);
    Opnd* memOpnd = irManager.newMemOpnd(typeManager.getUIntPtrType(), MemOpndKind_Heap, baseOpnd, NULL, NULL, NULL);
    const Mnemonic mn = Mnemonic_ADD;
    Inst* inst = irManager.newInst(mn, memOpnd,  irManager.newImmOpnd(typeManager.getUInt32Type(), 1));
    appendInsts(inst);
}

//_______________________________________________________________________________________________________________
//  Return with a value

void InstCodeSelector::ret(CG_OpndHandle* retValue) 
{
    Opnd* ret_val = (Opnd*)retValue;
    MethodDesc* mDesc = irManager.getCompilationInterface().getMethodToCompile();

    if (irManager.getCompilationInterface().getCompilationParams().exe_notify_method_exit) {
        if (ret_val == NULL) {
            ret_val = irManager.newImmOpnd(typeManager.getInt32Type(), 0);
        }
        genExitHelper(ret_val, mDesc);
    }

    appendInsts(irManager.newRetInst((Opnd*)retValue));
    seenReturn = true;
}

//_______________________________________________________________________________________________________________
//  Return without value

void InstCodeSelector::ret() 
{
    MethodDesc* mDesc = irManager.getCompilationInterface().getMethodToCompile();

    if (irManager.getCompilationInterface().getCompilationParams().exe_notify_method_exit) {
        Opnd* ret_val = irManager.newImmOpnd(typeManager.getInt32Type(), 0);
        genExitHelper(ret_val, mDesc);
    }
    appendInsts(irManager.newRetInst(0));
    seenReturn = true;
} 

//_______________________________________________________________________________________________________________
//    Generate return instruction

void InstCodeSelector::genReturn() 
{
    if (!seenReturn){
        MethodDesc* mDesc = irManager.getCompilationInterface().getMethodToCompile();

        if (irManager.getCompilationInterface().getCompilationParams().exe_notify_method_exit) {
            Opnd* ret_val = irManager.newImmOpnd(typeManager.getInt32Type(), 0);
            genExitHelper(ret_val, mDesc);
        }
        appendInsts(irManager.newRetInst(0));
    }
}

//_______________________________________________________________________________________________________________
//  Define an argument

CG_OpndHandle*    InstCodeSelector::defArg(U_32 position, Type *type) 
{
    return irManager.defArg(type, position);
}

//_______________________________________________________________________________________________________________
// Load address of the memory location that contains function address

CG_OpndHandle* InstCodeSelector::ldFunAddr(Type*        dstType, 
                                              MethodDesc * methodDesc) 
{
    Opnd * addrOpnd=irManager.newImmOpnd(dstType, Opnd::RuntimeInfo::Kind_MethodIndirectAddr, methodDesc);
    return addrOpnd;
}
    
//_______________________________________________________________________________________________________________
//  Load address of the virtual/interface table slot that contains function address
//    The tau operand is irrelevant because we compute the address of the slot not
//    address of the function.
//

CG_OpndHandle* InstCodeSelector::tau_ldVirtFunAddr(Type*        dstType, 
                                                  CG_OpndHandle*   vtableAddr, 
                                                  MethodDesc*      methodDesc,
                                                  CG_OpndHandle *  tauVtableHasDesc) 
{
#ifdef _EM64T_
    Opnd * offsetOpnd=irManager.newImmOpnd(typeManager.getIntPtrType(), Opnd::RuntimeInfo::Kind_MethodVtableSlotOffset, methodDesc);
    Opnd * acc = irManager.newOpnd(dstType);
    copyOpnd(acc, (Opnd*)vtableAddr);
    return simpleOp_I8(Mnemonic_ADD, dstType, acc, offsetOpnd);
#else
    Opnd * offsetOpnd=irManager.newImmOpnd(typeManager.getInt32Type(), Opnd::RuntimeInfo::Kind_MethodVtableSlotOffset, methodDesc);
    return simpleOp_I4(Mnemonic_ADD, dstType, (Opnd*)vtableAddr, offsetOpnd);
#endif
}

//_______________________________________________________________________________________________________________
//  Load virtual table address

CG_OpndHandle* InstCodeSelector::tau_ldVTableAddr(Type* dstType, 
                                                     CG_OpndHandle* base,
                                                     CG_OpndHandle *tauBaseNonNull) 
{
#ifndef _EM64T_
    Opnd * vtableAddr=irManager.newOpnd(dstType);
    Opnd * sourceVTableAddr=irManager.newMemOpnd(dstType, (Opnd*)base, 0, 0, 
            irManager.newImmOpnd(typeManager.getInt32Type(), Opnd::RuntimeInfo::Kind_VTableAddrOffset)
        );
    copyOpnd(vtableAddr, sourceVTableAddr);
    return vtableAddr;
#else
    Opnd * vtableAddr=irManager.newOpnd(dstType);

    int64 heapBase = (int64) VMInterface::getVTableBase();
    Opnd * acc =  simpleOp_I8(Mnemonic_ADD, dstType, (Opnd *)base, irManager.newImmOpnd(dstType, Opnd::RuntimeInfo::Kind_VTableAddrOffset));
    Opnd * sourceVTableAddr=irManager.newMemOpnd(typeManager.getInt32Type(), acc, 0, 0, irManager.newImmOpnd(typeManager.getUInt32Type(), 0));
    acc=irManager.newOpnd(dstType);
    appendInsts(irManager.newInstEx(Mnemonic_MOVZX, 1, acc, sourceVTableAddr));
    vtableAddr =  simpleOp_I8(Mnemonic_ADD, dstType, acc,  irManager.newImmOpnd(dstType, heapBase));
    return vtableAddr;
#endif
}

//_______________________________________________________________________________________________________________
//  Load interface table address
//    The tau parameter is irrelevant because VM helper does not expect JIT
//    to verify that the object type has given interface

CG_OpndHandle*  InstCodeSelector::tau_ldIntfTableAddr(Type *         dstType, 
                                                     CG_OpndHandle* base, 
                                                     NamedType*     vtableType)
{
    Opnd * helperOpnds[] = {
        (Opnd*)base,
        irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_TypeRuntimeId, vtableType)
    };

    Opnd * retOpnd=irManager.newOpnd(dstType);

    CallInst * callInst=irManager.newRuntimeHelperCallInst(
        VM_RT_GET_INTERFACE_VTABLE_VER0,
        2, helperOpnds, retOpnd);
    appendInsts(callInst);
    return retOpnd;
}

//_______________________________________________________________________________________________________________
//  Indirect call w/o a tau that indicates that first argument is non-null

CG_OpndHandle* InstCodeSelector::calli(U_32            numArgs, 
                                          CG_OpndHandle**   args, 
                                          Type*             retType, 
                                          CG_OpndHandle*    methodPtr)
{
    return tau_calli(numArgs, args, retType, methodPtr, getTauUnsafe(), getTauUnsafe());
}
                                        
//_______________________________________________________________________________________________________________
//  Indirect call with the tau that indicates that first argument is non-null

CG_OpndHandle* InstCodeSelector::tau_calli(U_32        numArgs, 
                                              CG_OpndHandle**   args, 
                                              Type*             retType, 
                                              CG_OpndHandle*    methodPtr,
                                              CG_OpndHandle*    nonNullFirstArgTau,
                                              CG_OpndHandle*    tauTypesChecked)
{
    Opnd * target=irManager.newMemOpnd(typeManager.getUnmanagedPtrType(typeManager.getUIntPtrType()), (Opnd*)methodPtr);
    Opnd * retOpnd=createResultOpnd(retType);
    CallInst * callInst=irManager.newCallInst(target, irManager.getDefaultManagedCallingConvention(), numArgs, (Opnd **)args, retOpnd);
    appendInsts(callInst);
    return retOpnd;
}

//_______________________________________________________________________________________________________________
//  Direct call to the method. Depending on the code generator flags we
//  either expand direct call into loading method address into a register and
//  indirect call or true direct call that will be patched if method is recompiled.

CG_OpndHandle* InstCodeSelector::call(U_32          numArgs, 
                                         CG_OpndHandle** args, 
                                         Type*           retType,
                                         MethodDesc *    desc)
{    
    return tau_call(numArgs, args, retType, desc, getTauUnsafe(), getTauUnsafe());
}

//_______________________________________________________________________________________________________________
//  Direct call to the method. Depending on the code generator flags we
//  either expand direct call into loading method address into a register and
//  indirect call or true direct call that will be patched if method is recompiled.

CG_OpndHandle* InstCodeSelector::tau_call(U_32          numArgs, 
                                             CG_OpndHandle** args, 
                                             Type*           retType,
                                             MethodDesc *    desc,
                                             CG_OpndHandle* tauNullCheckedFirstArg,
                                             CG_OpndHandle* tauTypesChecked)
{
    // target address here has Int32 type. On EM64T platform it can be larger, but this situations are managed
    // in Ia32CodeEmitter::postPass method (transforming direct calls into register form if target offset
    // does not fit into 32 bits). If on EM64T we set IntPtrType for target address here constraint resolver work makes all
    // calls a register-form ones. (Even for those with a short offset). But immediate calls are faster and takes
    // less space. We should keep them when it is possible.

    Opnd * target=irManager.newImmOpnd(typeManager.getInt32Type(), Opnd::RuntimeInfo::Kind_MethodDirectAddr, desc);
    Opnd * retOpnd=createResultOpnd(retType);
    CallInst * callInst=irManager.newCallInst(target, irManager.getDefaultManagedCallingConvention(), 
        numArgs, (Opnd **)args, retOpnd);
    appendInsts(callInst);
    return retOpnd;
}

//_______________________________________________________________________________________________________________
//  Virtual call.
//  'this' reference is in args[0]

CG_OpndHandle* InstCodeSelector::tau_callvirt(U_32          numArgs,
                                                 CG_OpndHandle** args, 
                                                 Type*           retType,
                                                 MethodDesc *    desc,
                                                 CG_OpndHandle * tauNullChecked,
                                                 CG_OpndHandle * tauTypesChecked)
{
    CG_OpndHandle * vtableAddr = NULL;
    NamedType* vtableType = desc->getParentType();
    Type* vtableAddrType = typeManager.getVTablePtrType(vtableType);
    CG_OpndHandle * tauUnsafe = getTauUnsafe();
    if (vtableType->isInterface()) {
        vtableAddr = tau_ldIntfTableAddr(vtableAddrType, args[0], vtableType);
    } else {
        vtableAddr = tau_ldVTableAddr(vtableAddrType, args[0], tauNullChecked);
    }
    CG_OpndHandle * virtFunAddr = tau_ldVirtFunAddr(typeManager.getUnmanagedPtrType(typeManager.getUIntPtrType()),
                                                vtableAddr, desc, tauUnsafe);
    return tau_calli(numArgs,args,retType,virtFunAddr,tauNullChecked, tauTypesChecked);
}

//_______________________________________________________________________________________________________________
//  JIT helper call

CG_OpndHandle* InstCodeSelector::callhelper(U_32              numArgs, 
                                            CG_OpndHandle**     args, 
                                            Type*               retType,
                                            JitHelperCallOp::Id callId) 
{
    Opnd * dstOpnd=createResultOpnd(retType);
    switch(callId) {
    case Prefetch:
    {
        assert (numArgs == 3);
        Opnd* address = (Opnd*) args[0];
        Opnd* distance = (Opnd*) args[1];    
        Opnd* stride = (Opnd*) args[2];

        assert (distance->isPlacedIn(OpndKind_Imm) && stride->isPlacedIn(OpndKind_Imm));
                assert(fit32(distance->getImmValue()));
                assert(fit32(stride->getImmValue()));
                int dist = (int)distance->getImmValue();
        int strd = (int)stride->getImmValue();
    
        for (int i=0; i< dist; i+=strd)
        {
            Opnd* prefAddress = irManager.newMemOpnd(typeManager.getInt8Type(), address, 0, 0, irManager.newImmOpnd(typeManager.getInt32Type(), i));
            Inst* inst = irManager.newInst(Mnemonic_PREFETCH, prefAddress);
            appendInsts(inst);
        }
        break;
    }

    case Memset0:
    {
        assert(numArgs == 2);
        Opnd** opnds = (Opnd**)args;    
        Opnd *addr = opnds[0];
        Opnd *size = opnds[1];

        RegName counterRegName = RegName_ECX;   
        RegName dstRegName = RegName_EDI;
        RegName zeroRegName = RegName_EAX;
        
        // prepare counter
        Type* int32Type = typeManager.getInt32Type();
        Opnd* counter = irManager.newRegOpnd(int32Type,counterRegName);
        copyOpnd(counter,size);
        appendInsts(irManager.newInst(Mnemonic_SHR, counter, irManager.newImmOpnd(int32Type,2)));

        // prepare dst  
        Opnd* dst = irManager.newRegOpnd(int32Type,dstRegName);
        copyOpnd(dst, addr);
        
        // prepare zero
        Opnd* eax = irManager.newRegOpnd(int32Type,zeroRegName);
        //appendInsts(irManager.newInst(Mnemonic_XOR, eax, eax));
        Opnd* zero = irManager.newImmOpnd(int32Type,0);
        appendInsts(irManager.newInst(Mnemonic_MOV, eax, zero));
        
        Inst* storeInst = irManager.newInst(Mnemonic_STOS, dst, counter, eax);
        storeInst->setPrefix(InstPrefix_REP);
        appendInsts(storeInst);
          break;
    }
    case InitializeArray:
    {
        assert(numArgs == 4);
        appendInsts(irManager.newInternalRuntimeHelperCallInst("initialize_array", numArgs, (Opnd**)args, dstOpnd));
          break;
    }
    case SaveThisState:
    {
        assert(numArgs == 1);
        Type* int32type = typeManager.getInt32Type();
        Type* unmanPtr = typeManager.getUnmanagedPtrType(int32type);
        Opnd * tlsBase = createTlsBaseLoadSequence(irManager, currentBasicBlock, unmanPtr);
        U_32 offsetInTLS = VMInterface::flagTLSThreadStateOffset();
        Opnd* pStateFlag = irManager.newMemOpnd(int32type, MemOpndKind_Any, tlsBase, offsetInTLS);
        Inst* ii = irManager.newCopyPseudoInst(Mnemonic_MOV, pStateFlag, (Opnd*)args[0]);
        appendInsts(ii);
        break;
    }
    case ReadThisState:
    {
        assert(numArgs == 0);
        Type* int32type = typeManager.getInt32Type();
        Type* unmanPtr = typeManager.getUnmanagedPtrType(int32type);

        Opnd * tlsBase = createTlsBaseLoadSequence(irManager, currentBasicBlock, unmanPtr);
        U_32 offsetInTLS = VMInterface::flagTLSThreadStateOffset();
        Opnd* pStateFlag = irManager.newMemOpnd(int32type, MemOpndKind_Any, tlsBase, offsetInTLS);
        Inst* ii = irManager.newCopyPseudoInst(Mnemonic_MOV, dstOpnd, pStateFlag);
        appendInsts(ii);
        break;
    }
    case LockedCompareAndExchange:
    {
        assert(numArgs == 3);
        Opnd** opnds = (Opnd**)args;

        //deal with constraints       
#ifdef _EM64T_
        Type* opnd1Type = opnds[1]->getType();
        assert(irManager.getTypeSize(opnd1Type)==OpndSize_32 || irManager.getTypeSize(opnd1Type)==OpndSize_64);
        assert(irManager.getTypeSize(opnds[1]->getType())==irManager.getTypeSize(opnds[2]->getType()));
        bool is64bit = irManager.getTypeSize(opnd1Type) == OpndSize_64;
//        Type* opType = is64bit ? typeManager.getInt64Type():typeManager.getInt32Type();
        Constraint ceax(OpndKind_GPReg, is64bit?OpndSize_64:OpndSize_32, 1<<getRegIndex(is64bit?RegName_RAX:RegName_EAX));
        Constraint cecx(OpndKind_GPReg, is64bit?OpndSize_64:OpndSize_32, 1<<getRegIndex(is64bit?RegName_RCX:RegName_ECX));
        Opnd* eaxOpnd = irManager.newOpnd(opnds[1]->getType(), ceax);
        Opnd* ecxOpnd = irManager.newOpnd(opnds[2]->getType(), cecx);
#else
        Opnd* eaxOpnd = irManager.getRegOpnd(RegName_EAX);
        Opnd* ecxOpnd = irManager.newOpnd(typeManager.getUInt32Type(), Constraint(OpndKind_GPReg));
#endif
        Opnd* memOpnd = irManager.newMemOpnd(opnds[1]->getType(), opnds[0]);//use opnd1 type for memopnd
        appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, eaxOpnd, opnds[1]));
        appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, ecxOpnd, opnds[2]));
        
        Inst*  inst = irManager.newInst(Mnemonic_CMPXCHG, memOpnd, ecxOpnd, eaxOpnd);
        inst->setPrefix(InstPrefix_LOCK);
        appendInsts(inst);

        //save the result
        appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, dstOpnd, irManager.newImmOpnd(typeManager.getInt32Type(), 0)));
        appendInsts(irManager.newInst(Mnemonic_SETZ, dstOpnd));
        break;
    }
    case AddValueProfileValue:
    {
        assert(numArgs == 2);
        Opnd* empiOpnd = irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_EM_ProfileAccessInterface);
        Opnd* mphOpnd = irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_Method_Value_Profile_Handle);
        const U_32 nArgs = 4;
        Opnd* newArgs[4] = {empiOpnd, mphOpnd, ((Opnd **)args)[0], ((Opnd **)args)[1]};
        appendInsts(irManager.newInternalRuntimeHelperCallInst("add_value_profile_value", nArgs, newArgs, dstOpnd));
        break;
    }
    case FillArrayWithConst:
    {
        assert(numArgs == 4);
        Opnd * newArgs[4] = {(Opnd *)args[0], (Opnd *)args[1], (Opnd *)args[2], (Opnd *)args[3]};
        appendInsts(irManager.newInternalRuntimeHelperCallInst("fill_array_with_const", numArgs, newArgs, dstOpnd));
        break;
    }
    case ArrayCopyDirect:
    {
        assert(numArgs == 5);
        Opnd * newArgs[5] = {(Opnd *)args[0], (Opnd *)args[1], (Opnd *)args[2], (Opnd *)args[3], (Opnd *)args[4]};
        appendInsts(irManager.newInternalRuntimeHelperCallInst("memory_copy_direct", numArgs, newArgs, dstOpnd));
        break;
    }
    case ArrayCopyReverse:
    {
        assert(numArgs == 5);
        Opnd * newArgs[5] = {(Opnd *)args[0], (Opnd *)args[1], (Opnd *)args[2], (Opnd *)args[3], (Opnd *)args[4]};
        appendInsts(irManager.newInternalRuntimeHelperCallInst("memory_copy_reverse", numArgs, newArgs, dstOpnd));
        break;
    }
    case StringCompareTo:
    {
        assert(numArgs == 7);
        Opnd * newArgs[7] = {(Opnd *)args[0], (Opnd *)args[1], (Opnd *)args[2], (Opnd *)args[3], (Opnd *)args[4], (Opnd *)args[5], (Opnd *)args[6]};
        appendInsts(irManager.newInternalRuntimeHelperCallInst("String_compareTo", numArgs, newArgs, dstOpnd));
        break;
    }
    case StringRegionMatches:
    {
        //printf("%d \n", callId);
        assert(numArgs == 5);
        Opnd * newArgs[5] = {(Opnd *)args[0], (Opnd *)args[1], (Opnd *)args[2], (Opnd *)args[3], (Opnd *)args[4]};
        appendInsts(irManager.newInternalRuntimeHelperCallInst("String_regionMatches", numArgs, newArgs, dstOpnd));
        break;
    }
    case StringIndexOf:
    {
        assert(numArgs == 7);
        Opnd * newArgs[7] = {(Opnd *)args[0], (Opnd *)args[1], (Opnd *)args[2], (Opnd *)args[3], (Opnd *)args[4], (Opnd *)args[5], (Opnd *)args[6]};
        appendInsts(irManager.newInternalRuntimeHelperCallInst("String_indexOf", numArgs, newArgs, dstOpnd));
        break;
    }

    default:
    {
        assert(0);
        break;
    }
    }
    return dstOpnd;
}

//_______________________________________________________________________________________________________________
//  VM helper call

CG_OpndHandle* InstCodeSelector::callvmhelper(U_32              numArgs, 
                                              CG_OpndHandle**     args, 
                                              Type*               retType,
                                              VM_RT_SUPPORT  callId) 
{
    Opnd* dstOpnd=NULL;
    switch(callId) {
    case VM_RT_THROW_LAZY:
    {

        Opnd **hlpArgs = new (memManager) Opnd* [numArgs+1];
        MethodDesc* md = ((Opnd*)args[0])->getType()->asMethodPtrType()->getMethodDesc();

        hlpArgs[0] = irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_TypeRuntimeId, 
                                          md->getParentType());
        for (U_32 i = 0; i < numArgs-1; i++)
            hlpArgs[i+1] = (Opnd*)args[i+1];
        hlpArgs[numArgs] = irManager.newImmOpnd(getRuntimeIdType(), 
                                               Opnd::RuntimeInfo::Kind_MethodRuntimeId, md);
        appendInsts(irManager.newRuntimeHelperCallInst(VM_RT_THROW_LAZY,
                                                       numArgs+1, (Opnd**)hlpArgs, dstOpnd));
        break;
    }
    case VM_RT_GC_GET_TLS_BASE:
    {
        assert(numArgs == 0);
        Type* tlsBaseType = typeManager.getUnmanagedPtrType(typeManager.getInt8Type());
        Opnd * tlsBase = createTlsBaseLoadSequence(irManager, currentBasicBlock, tlsBaseType);
        dstOpnd  = tlsBase;
        break;
    }
    //vmhelper slow paths
    case VM_RT_NEW_RESOLVED_USING_VTABLE_AND_SIZE:
    case VM_RT_NEW_VECTOR_USING_VTABLE:
    case VM_RT_MONITOR_ENTER:
    case VM_RT_MONITOR_EXIT:
    case VM_RT_GC_HEAP_WRITE_REF:
    case VM_RT_GET_INTERFACE_VTABLE_VER0:
    case VM_RT_CHECKCAST:
    case VM_RT_INSTANCEOF:
    
    //helpers used by JIT in lazy resolution mode
    case VM_RT_NEWOBJ_WITHRESOLVE:
    case VM_RT_NEWARRAY_WITHRESOLVE:
    case VM_RT_GET_NONSTATIC_FIELD_OFFSET_WITHRESOLVE:
    case VM_RT_GET_STATIC_FIELD_ADDR_WITHRESOLVE:
    case VM_RT_CHECKCAST_WITHRESOLVE:
    case VM_RT_INSTANCEOF_WITHRESOLVE:
    case VM_RT_GET_INVOKESTATIC_ADDR_WITHRESOLVE:
    case VM_RT_GET_INVOKEINTERFACE_ADDR_WITHRESOLVE:
    case VM_RT_GET_INVOKEVIRTUAL_ADDR_WITHRESOLVE:
    case VM_RT_GET_INVOKE_SPECIAL_ADDR_WITHRESOLVE:
    case VM_RT_INITIALIZE_CLASS_WITHRESOLVE:
    case VM_RT_MULTIANEWARRAY_RESOLVED:
        
    case VM_RT_GET_IDENTITY_HASHCODE:
{
        dstOpnd = retType==NULL ? NULL: irManager.newOpnd(retType);
        CallInst * callInst=irManager.newRuntimeHelperCallInst(callId, numArgs, (Opnd**)args, dstOpnd);
        appendInsts(callInst);
        break;
    }
    default:
        assert(0);
    }
    return dstOpnd;
}

//_______________________________________________________________________________________________________________
//  Box a value

CG_OpndHandle* InstCodeSelector::box(ObjectType    * boxedType, 
                                        CG_OpndHandle * val) 
{
    ICS_ASSERT(0);
    return 0;
}
 
//_______________________________________________________________________________________________________________
//  Unbox object

CG_OpndHandle* InstCodeSelector::unbox(Type * dstType, CG_OpndHandle* objHandle) 
{
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
//  Load value object

CG_OpndHandle* InstCodeSelector::ldValueObj(Type* objType, CG_OpndHandle *srcAddr) 
{
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
//  Store value object

void InstCodeSelector::stValueObj(CG_OpndHandle *dstAddr, CG_OpndHandle *src) 
{
    ICS_ASSERT(0);
}

//_______________________________________________________________________________________________________________
//  Initialize value object

void InstCodeSelector::initValueObj(Type* objType, CG_OpndHandle *objAddr) 
{
    ICS_ASSERT(0);
}

//_______________________________________________________________________________________________________________
//  Copy value object

void  InstCodeSelector::copyValueObj(Type* objType, CG_OpndHandle *dstAddr, CG_OpndHandle *srcAddr) 
{
    ICS_ASSERT(0);
}

//_______________________________________________________________________________________________________________
//  Acquire monitor for an object

void InstCodeSelector::tau_monitorEnter(CG_OpndHandle* obj, CG_OpndHandle* tauIsNonNull) 
{
    Opnd * helperOpnds[] = { (Opnd*)obj };
    CallInst * callInst=irManager.newRuntimeHelperCallInst(VM_RT_MONITOR_ENTER,
        1, helperOpnds, NULL);
    appendInsts(callInst);
}

//_______________________________________________________________________________________________________________
//    Release monitor for an object

void InstCodeSelector::tau_monitorExit(CG_OpndHandle* obj,
                                          CG_OpndHandle* tauIsNonNull) 
{
    Opnd * helperOpnds[] = { (Opnd*)obj };
    CallInst * callInst=irManager.newRuntimeHelperCallInst(VM_RT_MONITOR_EXIT,
        1, helperOpnds, NULL);
    appendInsts(callInst);
}

//_______________________________________________________________________________________________________________
//  Load address of the object lock 
// (aka lock owner field in the synchronization header)

CG_OpndHandle* InstCodeSelector::ldLockAddr(CG_OpndHandle* obj) 
{
    ICS_ASSERT(0);
    return 0;
} 

//_______________________________________________________________________________________________________________
CG_OpndHandle* InstCodeSelector::tau_balancedMonitorEnter(CG_OpndHandle* obj, CG_OpndHandle* lockAddr,
                                        CG_OpndHandle* tauIsNonNull)
{
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
void           InstCodeSelector::balancedMonitorExit(CG_OpndHandle* obj, CG_OpndHandle* lockAddr, 
                                       CG_OpndHandle* oldLock)
{
    ICS_ASSERT(0);
}

//_______________________________________________________________________________________________________________
CG_OpndHandle* InstCodeSelector::tau_optimisticBalancedMonitorEnter(CG_OpndHandle* obj, CG_OpndHandle* lockAddr,
                                                  CG_OpndHandle* tauIsNonNull)
{
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
void           InstCodeSelector::optimisticBalancedMonitorExit(CG_OpndHandle* obj, CG_OpndHandle* lockAddr, 
                                                 CG_OpndHandle* oldLock)
{
    ICS_ASSERT(0);
}

//_______________________________________________________________________________________________________________
//  Monitor enter fence

void InstCodeSelector::monitorEnterFence(CG_OpndHandle* obj) 
{
}

//_______________________________________________________________________________________________________________
//  Monitor exit fence

void InstCodeSelector::monitorExitFence(CG_OpndHandle* obj) 
{
}

//_______________________________________________________________________________________________________________
//      Acquire monitor for a class
//      Helper_TypeMonitorEnter(typeRuntimeId)

void InstCodeSelector::typeMonitorEnter(NamedType *type) 
{
    Opnd * helperOpnds1[]={irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_TypeRuntimeId, type)};
    Opnd * retOpnd=irManager.newOpnd(type);
    CallInst * callInst1=irManager.newRuntimeHelperCallInst(VM_RT_CLASS_2_JLC,
        1, helperOpnds1, retOpnd);
    appendInsts(callInst1);

    Opnd * helperOpnds2[]={retOpnd};
    CallInst * callInst2=irManager.newRuntimeHelperCallInst(VM_RT_MONITOR_ENTER,
        1, helperOpnds2, NULL);
    appendInsts(callInst2);
}

//_______________________________________________________________________________________________________________
//      Release monitor for a class
//      Helper_TypeMonitorExit(typeRuntimeId)

void InstCodeSelector::typeMonitorExit(NamedType *type) 
{
    Opnd * helperOpnds1[]={irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_TypeRuntimeId, type)};
    Opnd * retOpnd=irManager.newOpnd(type);
    CallInst * callInst1=irManager.newRuntimeHelperCallInst(VM_RT_CLASS_2_JLC,
        1, helperOpnds1, retOpnd);
    appendInsts(callInst1);

    Opnd * helperOpnds2[]={retOpnd};
    CallInst * callInst2=irManager.newRuntimeHelperCallInst(VM_RT_MONITOR_EXIT,
        1, helperOpnds2, NULL);
    appendInsts(callInst2);
}

//_______________________________________________________________________________________________________________
//  Statically cast object to type.
//  This cast requires no runtime check.  It's a compiler assertion. 

CG_OpndHandle* InstCodeSelector::tau_staticCast(ObjectType *   toType, 
                                                   CG_OpndHandle* obj,
                                                   CG_OpndHandle* tauIsType) 
{
    Opnd * dst=irManager.newOpnd(toType);
    copyOpnd(dst, (Opnd*)obj);
    return (Opnd*)dst;
}

//_______________________________________________________________________________________________________________
//      Cast object to type and return the object if casting is legal,
//      otherwise throw an exception. 
//      The cast operation does not produce a tau.

CG_OpndHandle* InstCodeSelector::tau_cast(ObjectType *toType, 
                                         CG_OpndHandle* obj,
                                         CG_OpndHandle* tauCheckedNull) 
{
    Opnd * dst=irManager.newOpnd(toType);
    Opnd * args[]={ (Opnd*)obj, irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_TypeRuntimeId, toType) };
    CallInst * callInst=irManager.newRuntimeHelperCallInst(
        VM_RT_CHECKCAST,
        lengthof(args), args, dst
    );
    appendInsts(callInst);
    return dst;
}

//_______________________________________________________________________________________________________________
//  Cast object to type and return the tau tied to this cast if casting is legal,
//  otherwise throw an exception. 
//  checkCast is different from cast - checkCast returns the tau while cast returns
//  the casted object (which is the same as the argument object)
//

CG_OpndHandle* InstCodeSelector::tau_checkCast(ObjectType *   toType, 
                                                  CG_OpndHandle* obj,
                                                  CG_OpndHandle* tauCheckedNull) 
{
    Opnd * dst=irManager.newOpnd(toType);
    Opnd * args[]={ (Opnd*)obj, irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_TypeRuntimeId, toType) };
    CallInst * callInst=irManager.newRuntimeHelperCallInst(
        VM_RT_CHECKCAST,
        lengthof(args), args, dst
    );
    appendInsts(callInst);
    return getTauUnsafe();
}

//_______________________________________________________________________________________________________________
//  Check if object has given type.
//  If it is return the object, else return NULL.

CG_OpndHandle* InstCodeSelector::tau_asType(ObjectType* type,
                                           CG_OpndHandle* obj,
                                           CG_OpndHandle* tauCheckedNull) 
{
    Opnd * instanceOfResult=(Opnd*)tau_instanceOf(type, obj, tauCheckedNull);
    Opnd * dst=irManager.newOpnd(type);
    cmpToEflags(CompareOp::Eq, CompareOp::Ref, (Opnd*)instanceOfResult, NULL);
    appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, dst, irManager.newImmOpnd(typeManager.getInt32Type(), 0))); 
    appendInsts(irManager.newInstEx(Mnemonic_CMOVZ,  1, dst, dst, (Opnd*)obj));
    return dst;
}

//_______________________________________________________________________________________________________________
//    Check if an object has given type.
//    If it is return 1 else return 0.

CG_OpndHandle* InstCodeSelector::tau_instanceOf(ObjectType *type, 
                                                 CG_OpndHandle* obj,
                                                 CG_OpndHandle* tauCheckedNull) 
{
#ifdef _EM64T_
    Opnd * dst=irManager.newOpnd(typeManager.getInt64Type());
#else
    Opnd * dst=irManager.newOpnd(typeManager.getInt32Type());
#endif
    Opnd * args[]={ (Opnd*)obj, irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_TypeRuntimeId, type) };
    CallInst * callInst=irManager.newRuntimeHelperCallInst(
        VM_RT_INSTANCEOF,
        lengthof(args), args, dst
    );
    appendInsts(callInst);
    return dst;
}

//_______________________________________________________________________________________________________________
//    Initialize type

void InstCodeSelector::initType(Type* type) 
{
    assert(type->isObject());
    NamedType * namedType = type->asNamedType();
    Opnd * args[]={ irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_TypeRuntimeId, namedType) };
    CallInst * callInst=irManager.newRuntimeHelperCallInst(
        VM_RT_INITIALIZE_CLASS,
        lengthof(args), args, NULL
    );
    appendInsts(callInst);
}

//_______________________________________________________________________________________________________________
//    Catch an exception object

CG_OpndHandle*  InstCodeSelector::catchException(Type * exceptionType) 
{
    Opnd * exceptionObj = irManager.newOpnd(exceptionType);
    appendInsts(irManager.newCatchPseudoInst(exceptionObj));
    return exceptionObj;
}


//_______________________________________________________________________________________________________________
//
//  Copy operand
//
CG_OpndHandle * InstCodeSelector::copy(CG_OpndHandle *src) 
{
    if (src==getTauUnsafe())
        return src;
    Opnd * dst=irManager.newOpnd(((Opnd*)src)->getType());
    copyOpnd(dst, (Opnd*)src);
    return dst;
}


//_______________________________________________________________________________________________________________
//  Prefetch a cache line from [addr]

void InstCodeSelector::prefetch(CG_OpndHandle *addr) 
{
    Opnd *mem = irManager.newMemOpnd(typeManager.getInt8Type(), (Opnd*) addr, 0, 0, 0);
    Inst* inst = irManager.newInst(Mnemonic_PREFETCH, mem);
    appendInsts(inst);
}

//_______________________________________________________________________________________________________________
//    Create a tau point definition indicating that control flow reached the current point

CG_OpndHandle*  InstCodeSelector::tauPoint() 
{
    return getTauUnsafe();
}

//_______________________________________________________________________________________________________________
//    Create a tau edge definition tied to the previous split in the control flow

CG_OpndHandle*  InstCodeSelector::tauEdge() 
{
    return getTauUnsafe();
}

//_______________________________________________________________________________________________________________
//    And several tau operands

CG_OpndHandle*  InstCodeSelector::tauAnd(U_32 numArgs, CG_OpndHandle** args) 
{
    return getTauUnsafe();
}

//_______________________________________________________________________________________________________________
//    Return tauUnsafe operand that indicates that it is unsafe
//    to move operation above any control flow split

CG_OpndHandle*  InstCodeSelector::tauUnsafe() 
{
    return getTauUnsafe();
}

//_______________________________________________________________________________________________________________
//    Return tauSafe operand that indicates that it is always safe to move the instruction

CG_OpndHandle*  InstCodeSelector::tauSafe() 
{
    return getTauUnsafe();
}


//_______________________________________________________________________________________________________________
CG_OpndHandle*  InstCodeSelector::pred_czero(CompareZeroOp::Types,CG_OpndHandle* src)
{
    ICS_ASSERT(0);
    return 0;
}

//_______________________________________________________________________________________________________________
CG_OpndHandle*  InstCodeSelector::pred_cnzero(CompareZeroOp::Types,CG_OpndHandle* src)
{
    ICS_ASSERT(0);
    return 0;
}


// END new tau instructions

//____________________________________________________________________________________________________
// Pseudo instruction

void InstCodeSelector::pseudoInst() {
    appendInsts(irManager.newEmptyPseudoInst());
}
//____________________________________________________________________________________________________
// Method entry/end marker instructions

void InstCodeSelector::methodEntry(MethodDesc* mDesc) {
    appendInsts(irManager.newMethodEntryPseudoInst(mDesc));
    Opnd* dstOpnd = NULL;
    if (irManager.getCompilationInterface().getCompilationParams().exe_notify_method_entry) {
        Opnd **hlpArgs = new (memManager) Opnd* [1];
        hlpArgs[0] = irManager.newImmOpnd(getRuntimeIdType(), 
                                               Opnd::RuntimeInfo::Kind_MethodRuntimeId, mDesc);
        appendInsts(irManager.newRuntimeHelperCallInst(VM_RT_JVMTI_METHOD_ENTER_CALLBACK,
                                                       1, (Opnd**)hlpArgs, dstOpnd));
    }
}
void InstCodeSelector::methodEnd(MethodDesc* mDesc, CG_OpndHandle* retOpnd) {
    Opnd* ret_val = (Opnd*)retOpnd;
    if (ret_val == NULL) {
        ret_val = irManager.newImmOpnd(typeManager.getInt32Type(), 0);
    }
    if (irManager.getCompilationInterface().getCompilationParams().exe_notify_method_exit) {
        genExitHelper(ret_val, mDesc);
    }
    appendInsts(irManager.newMethodEndPseudoInst(mDesc));
}

void InstCodeSelector::genExitHelper(Opnd* ret_val, MethodDesc* mDesc) {
    Opnd* bufOpnd = irManager.newOpnd(typeManager.getIntPtrType());

    if ((ret_val->getType()->isInt8())) {
        Opnd* longOpnd = (Opnd*)NULL;
        
        longOpnd = irManager.newMemOpnd(ret_val->getType(), MemOpndKind_StackAutoLayout,             
                irManager.getRegOpnd(STACK_REG),   0);

        appendInsts(irManager.newI8PseudoInst(Mnemonic_MOV, 1, longOpnd, ret_val));
        Opnd* zOpnd =  irManager.newMemOpnd(ret_val->getType(), MemOpndKind_StackAutoLayout, 
            irManager.getRegOpnd(STACK_REG),   0);
        Opnd* longArr[] = {longOpnd};
        appendInsts(irManager.newAliasPseudoInst(zOpnd, 1, longArr));
        appendInsts(irManager.newInst(Mnemonic_LEA, bufOpnd, zOpnd));
    } else {
        Opnd* stackLayOutedOpnd = irManager.newMemOpnd(ret_val->getType(), 
                                MemOpndKind_StackAutoLayout, 
                                irManager.getRegOpnd(STACK_REG), 
                                0);
        appendInsts(irManager.newCopyPseudoInst(Mnemonic_MOV, stackLayOutedOpnd, ret_val));
        appendInsts(irManager.newInst(Mnemonic_LEA, bufOpnd, stackLayOutedOpnd));

    }
    Opnd* hlpArgs[] = { irManager.newImmOpnd(getRuntimeIdType(), Opnd::RuntimeInfo::Kind_MethodRuntimeId, mDesc),
            bufOpnd };

    appendInsts(irManager.newRuntimeHelperCallInst(VM_RT_JVMTI_METHOD_EXIT_CALLBACK,
                                                    2, (Opnd**)hlpArgs, NULL));
}

//____________________________________________________________________________________________________

}}; // namespace Ia32
