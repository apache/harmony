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
 */

#include "CodeSelectors.h"

namespace Jitrino {

void _VarCodeSelector::genCode(Callback& callback) {
    VarOpnd * v0 = varOpnds;
    if (v0) {
        VarOpnd * v = v0;
        do {
            if (!(v->isDead())) {  // currently need to check, though dead items should be removed in future
                assert(!(v->isDead()));
                varIdMap[v->getId()] = 
                    callback.defVar(v->getType(),v->isAddrTaken(),
                    v->isPinned());
                if(Log::isEnabled()) {
                    Log::out() << "Opt var ";
                    v->print(Log::out());
                    Log::out() << " is CG var " << (int) varIdMap[v->getId()] << ::std::endl;
                }
            }
            v=v->getNextVarOpnd();
        } while (v != v0);
    }

    GCBasePointerMap::iterator i;
    for(i = gcMap.begin(); i != gcMap.end(); ++i) {
        if(Log::isEnabled()) {
            Log::out() << "Set GC base of ";
            i->first->print(Log::out());
            Log::out() << "(" << (int) varIdMap[i->first->getId()] << ") to ";
            i->second->print(Log::out());
            Log::out() << "(" << (int) varIdMap[i->second->getId()] << ")" << ::std::endl;
        }
        callback.setManagedPointerBase(varIdMap[i->first->getId()], varIdMap[i->second->getId()]);
    }
}

U_32 _VarCodeSelector::getNumVarOpnds() {
    VarOpnd* v0 = varOpnds;
    U_32 numVars = 0;
    if (v0) {
        VarOpnd* v = v0;
        do {
            if (!(v->isDead())) { // currently need to check, though in future dead items should be removed
                assert(!(v->isDead()));
                numVars++;
            }
            v = v->getNextVarOpnd();
        } while (v != v0);
    }
    return numVars;
}

//
// maps type and overflow modifier to a ArithmeticOp::Types
//
ArithmeticOp::Types _BlockCodeSelector::mapToArithmOpType(Inst* inst) {
    Type::Tag type = inst->getType();
    OverflowModifier modifier = inst->getOverflowModifier();
    ExceptionModifier excModifier = inst->getExceptionModifier();
    switch (type) {
        case Type::Int32:
            if ((modifier == Overflow_None) || (excModifier == Exception_Never))
                return ArithmeticOp::I4;
            if (modifier == Overflow_Signed)
                return ArithmeticOp::I4_Ovf;
            return ArithmeticOp::U4_Ovf;
        case Type::Int64:
            if ((modifier == Overflow_None) || (excModifier == Exception_Never))
                return ArithmeticOp::I8;
            if (modifier == Overflow_Signed)
                return ArithmeticOp::I8_Ovf;
            return ArithmeticOp::U8_Ovf;
        case Type::IntPtr:
        case Type::UIntPtr:
            if ((modifier == Overflow_None) || (excModifier == Exception_Never))
                return ArithmeticOp::I;
            if (modifier == Overflow_Signed)
                return ArithmeticOp::I_Ovf;
            return ArithmeticOp::U_Ovf;
        case Type::Float:
            return ArithmeticOp::F;
        case Type::Single:
            return ArithmeticOp::S;
        case Type::Double:
            return ArithmeticOp::D;
        default: assert(0);
    }
    assert(0);
    return ArithmeticOp::I;    // to keep the compiler quiet
}
 
//
//  maps type and overflow modifier to a RefArithmeticOp::Type
//
RefArithmeticOp::Types _BlockCodeSelector::mapToRefArithmOpType(Inst* inst, Opnd *src) {
    Type::Tag type = src->getType()->tag;
    OverflowModifier modifier = inst->getOverflowModifier();
    ExceptionModifier excModifier = inst->getExceptionModifier();
    assert(modifier != Overflow_Signed);
    switch (type) {
        case Type::Int32:
            if ((modifier == Overflow_None) || (excModifier == Exception_Never))
                return RefArithmeticOp::I4;
            return RefArithmeticOp::U4_Ovf;
        case Type::IntPtr:
            if ((modifier == Overflow_None) || (excModifier == Exception_Never))
                return RefArithmeticOp::I;
            return RefArithmeticOp::U_Ovf;
        default: assert(0);
    }
    assert(0);
    return RefArithmeticOp::I;    // to keep the compiler quiet
}

//
//  checks if instruction has an overflow modifier
//
bool _BlockCodeSelector::isOverflow(Inst* inst) {
    Modifier mod = inst->getModifier();
    if (mod.hasOverflowModifier()) {
        OverflowModifier modifier = mod.getOverflowModifier();
        return modifier != Overflow_None;
    } else
        return false;
}

//
//  checks if instruction has an exception modifier that can never except
//
bool _BlockCodeSelector::isExceptionNever(Inst* inst) {
    Modifier mod = inst->getModifier();
    if (mod.hasExceptionModifier()) {
        ExceptionModifier modifier = mod.getExceptionModifier();
        return modifier == Exception_Never;
    } else
        return false;
}

//
//  checks if instruction is unsigned
//
bool _BlockCodeSelector::isUnsigned(Inst *inst) {
    Modifier mod = inst->getModifier();
    if (mod.hasSignedModifier()) {
        SignedModifier modifier = mod.getSignedModifier();
        return modifier == UnsignedOp;
    } else {
        assert(0);
        return false;
    }
}

//
// checks if shift instruction needs shift mask
//
bool _BlockCodeSelector::isShiftMask(Inst *inst) {
    assert(inst->getModifier().hasShiftMaskModifier());
    ShiftMaskModifier modifier = inst->getModifier().getShiftMaskModifier();
    return modifier == ShiftMask_Masked;
}

DivOp::Types _BlockCodeSelector::mapToDivOpType(Inst* inst) {
    Type::Tag type = inst->getType();
    SignedModifier modifier = inst->getSignedModifier();
    bool unsignedDiv = (modifier == UnsignedOp);

    switch (type) {
        case Type::Int32: 
            return unsignedDiv ? DivOp::U4 : DivOp::I4;
        case Type::Int64:
            return unsignedDiv ? DivOp::U8 : DivOp::I8;
        case Type::IntPtr:
            return unsignedDiv ? DivOp::U  : DivOp::I;
        case Type::Float:
            return DivOp::F;
        case Type::Single:
            return DivOp::S;
        case Type::Double:
            return DivOp::D;
        default: assert(0);
    }
    assert(0);
    return DivOp::I; // to keep compiler quiet
}

MulHiOp::Types _BlockCodeSelector::mapToMulHiOpType(Inst * inst) {
    Type::Tag type = inst->getType();
    SignedModifier modifier = inst->getSignedModifier();
    bool unsignedMulhi = (modifier == UnsignedOp);

    switch (type) {
        case Type::Int32: 
            return unsignedMulhi ? MulHiOp::U4 : MulHiOp::I4;
        case Type::Int64:
            return unsignedMulhi ? MulHiOp::U8 : MulHiOp::I8;
        case Type::IntPtr:
            return unsignedMulhi ? MulHiOp::U  : MulHiOp::I;
        default: assert(0);
    }
    assert(0);
    return MulHiOp::I; // to keep compiler quiet
}
    
NegOp::Types _BlockCodeSelector::mapToNegOpType(Inst* inst) {
    Type::Tag type = inst->getType();
    switch(type) {
        case Type::Int32:
            return NegOp::I4;
        case Type::Int64:
            return NegOp::I8;
        case Type::IntPtr:
            return NegOp::I;
        case Type::Float:
            return NegOp::F;
        case Type::Single:
            return NegOp::S;
        case Type::Double:
            return NegOp::D;
        default: assert(0);
    }
    assert(0);
    return NegOp::I; // to keep compiler quiet
}

//
//  Maps instruction type to IntegerOp::Types
//
IntegerOp::Types _BlockCodeSelector::mapToIntegerOpType(Inst* inst) {
    Type::Tag type = inst->getType();
    switch (type) {
        case Type::Int32: 
            return IntegerOp::I4;
        case Type::Int64:
            return IntegerOp::I8;
        case Type::IntPtr:
        case Type::UIntPtr:
            return IntegerOp::I;
        default: assert(0);
    }
    assert(0);
    return IntegerOp::I; // to keep compiler quiet
}

//
//  maps type to CompareOp::Types
//
CompareOp::Types _BlockCodeSelector::mapToCompareOpType(Inst* inst) {
    Type::Tag type = inst->getType();
    switch (type) {
        case Type::Int32: 
            return CompareOp::I4;
        case Type::Int64:
            return CompareOp::I8;
        case Type::IntPtr:
        case Type::UIntPtr:
            return CompareOp::I;
        case Type::Float:
            return CompareOp::F;
        case Type::Single:
            return CompareOp::S;
        case Type::Double:
            return CompareOp::D;
        default: 

            assert(Type::isReference(type));
            return CompareOp::Ref;
    }
}

//
//  maps type to CompareZeroOp::Types
//
CompareZeroOp::Types _BlockCodeSelector::mapToCompareZeroOpType(Inst *inst) {
    Type::Tag type = inst->getType();
    switch (type) {
        case Type::Int32:
            return CompareZeroOp::I4;
        case Type::Int64:
            return CompareZeroOp::I8;
        case Type::IntPtr:
            return CompareZeroOp::I;
        default:
            assert(Type::isReference(type));
            return CompareZeroOp::Ref;
    }
}

//
//  Maps compare inst to the CompareOp::Operator
//
CompareOp::Operators _BlockCodeSelector::mapToComparisonOp(Inst* inst) {
    ComparisonModifier modifier = inst->getComparisonModifier();
    switch (modifier) {
        case    Cmp_EQ:     return CompareOp::Eq;
        case    Cmp_NE_Un:    return CompareOp::Ne;
        case    Cmp_GT:        return CompareOp::Gt;
        case    Cmp_GT_Un:    return CompareOp::Gtu;
        case    Cmp_GTE:    return CompareOp::Ge;
        case    Cmp_GTE_Un:    return CompareOp::Geu;
            // unary boolean comparisons
        case    Cmp_Zero:
        case    Cmp_NonZero:
        default:
            assert(0);
    }
    return CompareOp::Eq;    // to keep compiler quiet
}

//
//  Maps instruction to ConvertToFpOp::Types
//
ConvertToFpOp::Types _BlockCodeSelector::mapToFpConvertOpType(Inst *inst) {
    Type::Tag type = inst->getType();
    switch (type) {
        case Type::Single: return ConvertToFpOp::Single;
        case Type::Double: return ConvertToFpOp::Double;
        case Type::Float:  return ConvertToFpOp::FloatFromUnsigned;
        default: assert(0);
    }
    assert(0);
    return ConvertToFpOp::Single;   // to keep the compiler quiet
}

//
//  Maps instruction to ConvertToIntOp::Types
//
ConvertToIntOp::Types _BlockCodeSelector::mapToIntConvertOpType(Inst *inst) {
    Type::Tag type = inst->getType();
    switch (type) {
        case Type::Int8: 
        case Type::UInt8: 
            return ConvertToIntOp::I1;
        case Type::Int16: 
        case Type::UInt16: 
            return ConvertToIntOp::I2;
        case Type::Int32: 
        case Type::UInt32: 
            return ConvertToIntOp::I4;
        case Type::Int64: 
        case Type::UInt64: 
            return ConvertToIntOp::I8;
        case Type::IntPtr: 
        case Type::UIntPtr: 
            return ConvertToIntOp::I;
        default: assert(0);
    }
    assert(0);
    return ConvertToIntOp::I;    // to keep the compiler quiet
}

//
//  Maps instruction to ConvertToIntOp::OverflowMod
//  
ConvertToIntOp::OverflowMod _BlockCodeSelector::mapToIntConvertOvfMod(Inst *inst) {
    if (isExceptionNever(inst)) { return ConvertToIntOp::NoOvf; };
    OverflowModifier modifier = inst->getOverflowModifier();
    switch (modifier) {
    case Overflow_None:
        return ConvertToIntOp::NoOvf;
    case Overflow_Signed:
        return ConvertToIntOp::SignedOvf;
    case Overflow_Unsigned:
        return ConvertToIntOp::UnsignedOvf;
default: assert(0);
    }
    return ConvertToIntOp::NoOvf;    // to keep the compiler quiet
}

JitHelperCallOp::Id _BlockCodeSelector::convertJitHelperId(JitHelperCallId callId) {
    switch(callId) {
        case Prefetch:                  return JitHelperCallOp::Prefetch;
        case Memset0:                   return JitHelperCallOp::Memset0;
        case InitializeArray:           return JitHelperCallOp::InitializeArray;
        case SaveThisState:             return JitHelperCallOp::SaveThisState;
        case ReadThisState:             return JitHelperCallOp::ReadThisState;
        case LockedCompareAndExchange:  return JitHelperCallOp::LockedCompareAndExchange;
        case AddValueProfileValue:      return JitHelperCallOp::AddValueProfileValue;
        case FillArrayWithConst:        return JitHelperCallOp::FillArrayWithConst;
        case ArrayCopyDirect:           return JitHelperCallOp::ArrayCopyDirect;
        case ArrayCopyReverse:          return JitHelperCallOp::ArrayCopyReverse;
        case StringCompareTo:           return JitHelperCallOp::StringCompareTo;
        case StringRegionMatches:       return JitHelperCallOp::StringRegionMatches;
        case StringIndexOf:             return JitHelperCallOp::StringIndexOf;
        default: break;
    }
    crash("\n JIT helper in not supported in LIR : %d\n", callId);

    return JitHelperCallOp::InitializeArray; // to keep compiler quiet
}
    
CG_OpndHandle ** _BlockCodeSelector::genCallArgs(Inst * call, U_32 arg0Pos) {
    U_32 nSrc = call->getNumSrcOperands();
    CG_OpndHandle ** args = new(memManager) CG_OpndHandle*[nSrc - arg0Pos];
    for (U_32 i = arg0Pos; i < nSrc; i++)
        args[i - arg0Pos] = getCGInst(call->getSrc(i));
    return args;
}
    
CG_OpndHandle ** _BlockCodeSelector::genCallArgs(Opnd *extraArg, Inst * call, U_32 arg0Pos) {
    U_32 nSrc = call->getNumSrcOperands();
    CG_OpndHandle ** args = new(memManager) CG_OpndHandle*[nSrc - arg0Pos + 1];
    args[0] = getCGInst(extraArg);
    for (U_32 i = arg0Pos; i < nSrc; i++)
        args[i - arg0Pos + 1] = getCGInst(call->getSrc(i));
    return args;
}

void _BlockCodeSelector::genInstCode(InstructionCallback& instructionCallback, Inst *inst, bool genConsts) 
{
    uint16 bcOffset = inst->getBCOffset();
    instructionCallback.setCurrentHIRInstBCOffset(bcOffset);

    if(Log::isEnabled()) {
        Log::out() << "genInstCode ";
        inst->print(Log::out());
        if (genConsts) {
            Log::out() << "; genConsts=true" << ::std::endl;
        } else {
            Log::out() << "; genConsts=false" << ::std::endl;
        }
    }

    CG_OpndHandle* cgInst = NULL;
    Opnd *dst = inst->getDst();
    bool isConstant = false;
    switch(inst->getOpcode()) {
        case Op_Add:
            {
                assert(inst->getNumSrcOperands() == 2);
                Opnd* src1 = inst->getSrc(0);
                Opnd* src2 = inst->getSrc(1);
                CG_OpndHandle* src1CGInst = getCGInst(src1);
                CG_OpndHandle* src2CGInst = getCGInst(src2);
                if (src1->getType()->isReference()) {
                    cgInst = instructionCallback.addRef(mapToRefArithmOpType(inst,src2),
                        src1CGInst,src2CGInst);
                } else {
                    cgInst = instructionCallback.add(mapToArithmOpType(inst),
                        src1CGInst,src2CGInst);
                }
            }
            break;
        case Op_Mul:
            {
                assert(inst->getNumSrcOperands() == 2);
                cgInst = instructionCallback.mul(mapToArithmOpType(inst),
                    getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
            }
            break;
        case Op_Sub:
            {
                assert(inst->getNumSrcOperands() == 2);
                Opnd *src1 = inst->getSrc(0);
                Opnd *src2 = inst->getSrc(1);
                CG_OpndHandle* src1CGInst = getCGInst(src1);
                CG_OpndHandle* src2CGInst = getCGInst(src2);

                if (src1->getType()->isReference()) {
                    if (src2->getType()->isReference()) {
                        cgInst = instructionCallback.diffRef(isOverflow(inst) && !isExceptionNever(inst),
                            src1CGInst,
                            src2CGInst);
                    }
                    else {
                        cgInst = instructionCallback.subRef(mapToRefArithmOpType(inst,src2),
                            src1CGInst,
                            src2CGInst);
                    }
                }
                else {
                    cgInst = instructionCallback.sub(mapToArithmOpType(inst),
                        src1CGInst,
                        src2CGInst);
                }
            }
            break;
        case Op_TauDiv:
            {
                assert(inst->getNumSrcOperands() == 3);
                Opnd *tauOpnd = inst->getSrc(2);
                assert(tauOpnd->getType()->tag == Type::Tau);
                cgInst = instructionCallback.tau_div(mapToDivOpType(inst),
                    getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)),
                    getCGInst(tauOpnd));
            }
            break;
        case Op_TauRem:
            {
                assert(inst->getNumSrcOperands() == 3);
                Opnd *tauOpnd = inst->getSrc(2);
                assert(tauOpnd->getType()->tag == Type::Tau);

                cgInst = instructionCallback.tau_rem(mapToDivOpType(inst),
                    getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)),
                    getCGInst(tauOpnd));
            }
            break;
        case Op_Neg:
            {
                assert(inst->getNumSrcOperands() == 1);
                cgInst = instructionCallback.neg(mapToNegOpType(inst),
                    getCGInst(inst->getSrc(0)));
            }
            break;
        case Op_MulHi:
            {
                assert(inst->getNumSrcOperands() == 2);
                cgInst = instructionCallback.mulhi(mapToMulHiOpType(inst),
                    getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
            }
            break;
        case Op_Min:
            {
                assert(inst->getNumSrcOperands() == 2);
                cgInst = instructionCallback.min_op(mapToNegOpType(inst),
                    getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
            }
            break;
        case Op_Max:
            {
                assert(inst->getNumSrcOperands() == 2);
                cgInst = instructionCallback.max_op(mapToNegOpType(inst),
                    getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
            }
            break;
        case Op_Abs:
            {
                assert(inst->getNumSrcOperands() == 1);
                cgInst = instructionCallback.abs_op(mapToNegOpType(inst),
                    getCGInst(inst->getSrc(0)));
            }
            break;
        case Op_TauCheckFinite:
            {
                assert(inst->getNumSrcOperands() == 1);
                cgInst = instructionCallback.tau_ckfinite(getCGInst(inst->getSrc(0)));
            }
            break;
        case Op_And:
            {
                assert(inst->getNumSrcOperands() == 2);
                cgInst = instructionCallback.and_(mapToIntegerOpType(inst),
                    getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
            }
            break;
        case Op_Or:
            {
                assert(inst->getNumSrcOperands() == 2);
                cgInst = instructionCallback.or_(mapToIntegerOpType(inst),
                    getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
            }
            break;
        case Op_Xor:
            {
                assert(inst->getNumSrcOperands() == 2);
                cgInst = instructionCallback.xor_(mapToIntegerOpType(inst),
                    getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
            }
            break;
        case Op_Not:
            {
                assert(inst->getNumSrcOperands() == 1);
                cgInst = instructionCallback.not_(mapToIntegerOpType(inst),
                    getCGInst(inst->getSrc(0)));
            }
            break;
        case Op_Select:
            {
                assert(inst->getNumSrcOperands() == 3);
                cgInst = instructionCallback.select(mapToCompareOpType(inst),
                    getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)),
                    getCGInst(inst->getSrc(2)));
            }
            break;
        case Op_Conv:
        case Op_ConvZE:
        case Op_ConvUnmanaged:
            {
                assert(inst->getNumSrcOperands() == 1);
                Type * dstType = inst->getDst()->getType();
                if (dstType->isFP()) {
                    cgInst = instructionCallback.convToFp(mapToFpConvertOpType(inst),
                        dstType,
                        getCGInst(inst->getSrc(0)));
                } else if (dstType->isObject()){
                    cgInst = instructionCallback.convUPtrToObject(dstType->asObjectType(), getCGInst(inst->getSrc(0)));
                } else if (dstType->isUnmanagedPtr()) {
                    cgInst = instructionCallback.convToUPtr(dstType->asPtrType(), getCGInst(inst->getSrc(0)));
                } else {
                    bool isSigned = Type::isSignedInteger(inst->getType());
                    bool isZeroExtend = inst->getOpcode() == Op_ConvZE;
                    cgInst = instructionCallback.convToInt(mapToIntConvertOpType(inst),
                        isSigned, 
                        isZeroExtend,
                        mapToIntConvertOvfMod(inst), 
                        dstType,
                        getCGInst(inst->getSrc(0)));
                }
            }
            break;    
        case Op_Shladd:
            {
                assert(inst->getNumSrcOperands() == 3);
                Opnd *op1 = inst->getSrc(1);
                ConstInst *op1ci = op1->getInst()->asConstInst();
                assert(op1ci);
                assert(op1ci->getType() == Type::Int32);
                I_32 shiftby = op1ci->getValue().i4;
                cgInst = instructionCallback.shladd(mapToIntegerOpType(inst),
                    getCGInst(inst->getSrc(0)),
                    shiftby,
                    getCGInst(inst->getSrc(2)));
            }
            break;
        case Op_Shl:
            {
                assert(inst->getNumSrcOperands() == 2);
                cgInst = instructionCallback.shl(mapToIntegerOpType(inst),
                    getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
            }
            break;
        case Op_Shr:
            {
                assert(inst->getNumSrcOperands() == 2);
                IntegerOp::Types opType = mapToIntegerOpType(inst);
                CG_OpndHandle* src1CGInst = getCGInst(inst->getSrc(0));
                CG_OpndHandle* src2CGInst = getCGInst(inst->getSrc(1));

                if (isUnsigned(inst))
                    cgInst = instructionCallback.shru(opType,src1CGInst,src2CGInst);
                else
                    cgInst = instructionCallback.shr(opType,src1CGInst,src2CGInst); 
            }
            break;
        case Op_Cmp:
            {
                if (inst->getNumSrcOperands() == 2) {
                    // binary comparison
                    CompareOp::Operators cmpOp = mapToComparisonOp(inst);
                    CompareOp::Types opType = mapToCompareOpType(inst);
                    cgInst = instructionCallback.cmp(cmpOp, opType,
                        getCGInst(inst->getSrc(0)),
                        getCGInst(inst->getSrc(1)));
                } else {
                    assert(inst->getNumSrcOperands() == 1);
                    // unary comparison against zero
                    if (inst->getComparisonModifier() == Cmp_Zero) {
                        cgInst = instructionCallback.czero(mapToCompareZeroOpType(inst),
                            getCGInst(inst->getSrc(0)));
                    } else {
                        // Nonzero
                        cgInst = instructionCallback.cnzero(mapToCompareZeroOpType(inst),
                            getCGInst(inst->getSrc(0)));
                    }
                }
            }
            break;
        case Op_Cmp3:
            {
#ifndef BACKEND_HAS_CMP3                

                // always binary comparison
                CompareOp::Types opType = mapToCompareOpType(inst);
                CompareOp::Operators cmpOp = mapToComparisonOp(inst);
                CompareOp::Operators cmpOp2 = cmpOp;

                int ifNaNResult = -1;
                if (cmpOp == CompareOp::Gtu || cmpOp == CompareOp::Geu)
                    ifNaNResult = 1;

                CG_OpndHandle* cgInst1 = 
                    instructionCallback.cmp(cmpOp, opType,
                    getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)), ifNaNResult);

                // second operator has opposite NaN behavior for Floats
                if ((opType==CompareOp::F) ||
                    (opType==CompareOp::S) ||
                    (opType==CompareOp::D)) {
                        switch (cmpOp) {
                        case CompareOp::Gt: cmpOp2 = CompareOp::Gtu; ifNaNResult = 1; break;
                        case CompareOp::Gtu: cmpOp2 = CompareOp::Gt; ifNaNResult = -1; break;
                        case CompareOp::Ge: cmpOp2 = CompareOp::Geu; ifNaNResult = 1; break;
                        case CompareOp::Geu: cmpOp2 = CompareOp::Ge; ifNaNResult = -1; break;
                        default: break;
                        };
                    }

                    CG_OpndHandle* cgInst2 = 
                        instructionCallback.cmp(cmpOp2, opType, 
                        getCGInst(inst->getSrc(1)),
                        getCGInst(inst->getSrc(0)), ifNaNResult);
                    cgInst = 
                        instructionCallback.sub(ArithmeticOp::I4,
                        cgInst1,
                        cgInst2);
#else
                // always binary comparison
                CompareOp::Operators cmpOp = mapToComparisonOp(inst);
                CompareOp::Types opType = mapToCompareOpType(inst);
                cgInst = instructionCallback.cmp3(cmpOp, opType,
                    getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
#endif
            }
            break;
        case Op_Branch:
            {
                if (inst->getNumSrcOperands() == 2) {
                    // binary comparison
                    instructionCallback.branch(mapToComparisonOp(inst),
                        mapToCompareOpType(inst),
                        getCGInst(inst->getSrc(0)),
                        getCGInst(inst->getSrc(1)));
                } else {
                    assert(inst->getNumSrcOperands() == 1);
                    // unary comparison against zero
                    if (inst->getComparisonModifier() == Cmp_Zero) {
                        instructionCallback.bzero(mapToCompareZeroOpType(inst),
                            getCGInst(inst->getSrc(0)));
                    } else {
                        // Nonzero
                        instructionCallback.bnzero(mapToCompareZeroOpType(inst),
                            getCGInst(inst->getSrc(0)));
                    }
                }
            }
            break;
        case Op_Jump: 
            {    
                instructionCallback.jump();
            }
            break; 
        case Op_Switch: 
            {
                assert(inst->getNumSrcOperands() == 1);
                SwitchInst *swInst = (SwitchInst *)inst;
                instructionCallback.tableSwitch(getCGInst(inst->getSrc(0)),
                    swInst->getNumTargets());
            }
            break; 
        case Op_DirectCall:
            {
                assert(inst->getNumSrcOperands() >= 2);
                Opnd *tauNullChecked = inst->getSrc(0);
                assert(tauNullChecked->getType()->tag == Type::Tau);
                Opnd *tauTypesChecked = inst->getSrc(1);
                assert(tauTypesChecked->getType()->tag == Type::Tau);

                MethodCallInst * call = (MethodCallInst *)inst;
                MethodDesc * methodDesc = call->getMethodDesc();
                CG_OpndHandle ** args = genCallArgs(call,2); // omit tau operands
                U_32 numArgs = inst->getNumSrcOperands()-2; // also omit from count
                cgInst = 
                    instructionCallback.tau_call(numArgs,
                                                 args,
                                                 inst->getDst()->getType(),
                                                 methodDesc,
                                                 getCGInst(tauNullChecked),
                                                 getCGInst(tauTypesChecked));
            }
            break;
        case Op_TauVirtualCall:
            {
                assert(inst->getNumSrcOperands() >= 3);
                Opnd *tauNullChecked = inst->getSrc(0);
                Opnd *tauTypesChecked = inst->getSrc(1);
                assert(tauNullChecked->getType()->tag == Type::Tau);
                assert(tauTypesChecked->getType()->tag == Type::Tau);

                MethodCallInst * call = (MethodCallInst *)inst;
                MethodDesc * methodDesc = call->getMethodDesc();
                cgInst = 
                    instructionCallback.tau_callvirt(inst->getNumSrcOperands()-2, // omit taus
                                                     genCallArgs(call, 2), // omit taus
                                                     inst->getDst()->getType(),
                                                     methodDesc,
                                                     getCGInst(tauNullChecked),
                                                     getCGInst(tauTypesChecked));
            }
            break;
        case Op_IndirectCall:
            {
                assert(inst->getNumSrcOperands() >= 3);
                Opnd *fnAddr = inst->getSrc(0);
                Opnd *tauNullChecked = inst->getSrc(1);
                Opnd *tauTypesChecked = inst->getSrc(2);
                assert(tauNullChecked->getType()->tag == Type::Tau);
                assert(tauTypesChecked->getType()->tag == Type::Tau);
                assert(inst->isCall());
                cgInst = 
                    instructionCallback.tau_calli(inst->getNumSrcOperands() - 3, // omit taus and fnAddr
                                                  genCallArgs(inst, 3), // omit taus and fnAddr
                                                  inst->getDst()->getType(),
                                                  getCGInst(fnAddr),
                                                  getCGInst(tauNullChecked),
                                                  getCGInst(tauTypesChecked));
            }
            break;
        case Op_IndirectMemoryCall:
            {
                assert(inst->getNumSrcOperands() >= 3);
                Opnd *fnAddr = inst->getSrc(0);
                Opnd *tauNullChecked = inst->getSrc(1);
                Opnd *tauTypesChecked = inst->getSrc(2);
                assert(tauNullChecked->getType()->tag == Type::Tau);
                assert(tauTypesChecked->getType()->tag == Type::Tau);
                assert(inst->isCall());
                cgInst = 
                    instructionCallback.tau_calli(inst->getNumSrcOperands() - 3, // omit taus andfnAddr
                                                  genCallArgs(inst, 3), // omit taus and fnAddr
                                                  inst->getDst()->getType(),
                                                  getCGInst(fnAddr),
                                                  getCGInst(tauNullChecked),
                                                  getCGInst(tauTypesChecked));
            }
            break;
        case Op_JitHelperCall:
            {
                JitHelperCallInst* call = inst->asJitHelperCallInst();
                JitHelperCallId callId = call->getJitHelperId();
                if (callId == ArrayCopyDirect || callId == ArrayCopyReverse)                
                    cgInst = 
                    instructionCallback.callhelper(inst->getNumSrcOperands()-2, // omit taus
                                                   genCallArgs(call,2), // omit taus
                                                   inst->getDst()->getType(),
                                                   convertJitHelperId(callId));
                else                    
                    cgInst = 
                    instructionCallback.callhelper(inst->getNumSrcOperands(),
                                                   genCallArgs(call,0),
                                                   inst->getDst()->getType(),
                                                   convertJitHelperId(callId));
            }
            break;
        case Op_VMHelperCall:
            {
                VMHelperCallInst* call = inst->asVMHelperCallInst();
                VM_RT_SUPPORT callId = call->getVMHelperId();
                cgInst = 
                    instructionCallback.callvmhelper(inst->getNumSrcOperands(),
                                                     genCallArgs(call,0),
                                                     inst->getDst()->getType(),
                                                     callId);
            }
            break;
        case Op_Return:
            {
                if (inst->getType() == Type::Void) {
                    instructionCallback.ret();
                } else {
                    instructionCallback.ret(getCGInst(inst->getSrc(0)));
                }
            }
            break;
        case Op_Throw:
            {
                instructionCallback.throwException(getCGInst(inst->getSrc(0)), inst->getThrowModifier() == Throw_CreateStackTrace);
            }
            break;
        case Op_PseudoThrow:
            instructionCallback.pseudoInst();
            break;
        case Op_ThrowSystemException:
            {
                TokenInst *tokenInst = (TokenInst *)inst;
                U_32 token = tokenInst->getToken();
                CompilationInterface::SystemExceptionId id 
                    = (CompilationInterface::SystemExceptionId)token;
                instructionCallback.throwSystemException(id);
            }
            break;
        case Op_ThrowLinkingException:
            {
                LinkingExcInst *linkExcInst = (LinkingExcInst *)inst;
                Class_Handle encClass = linkExcInst->getEnclosingClass();
                U_32 constPoolIndex = linkExcInst->getCPIndex();
                U_32 opcode = linkExcInst->getOperation();
                instructionCallback.throwLinkingException(encClass, constPoolIndex, opcode);
            }
            break;
        case Op_Catch:
            cgInst = instructionCallback.catchException(inst->getDst()->getType());
            break;
        case Op_Copy:
            {
                cgInst = instructionCallback.copy(getCGInst(inst->getSrc(0)));
            }
            break;
        case Op_DefArg:
            {
                cgInst = instructionCallback.defArg(argCount,
                    inst->getDst()->getType());
                argCount += 1;
            }
            break;
        case Op_LdConstant:
            {
                if (!genConsts) break;
                ConstInst* constInst = (ConstInst*)inst;
                switch (inst->getType()) {
#ifdef _IA32_
        case Type::UIntPtr:
        case Type::IntPtr:
        case Type::UnmanagedPtr:
#endif
        case Type::Int32:
            cgInst = instructionCallback.ldc_i4(constInst->getValue().i4);
            break;
#ifdef _EM64T_
        case Type::UIntPtr:
        case Type::IntPtr:
        case Type::UnmanagedPtr:
#endif
        case Type::Int64:
            cgInst = instructionCallback.ldc_i8(constInst->getValue().i8);
            break;
        case Type::Single:
            cgInst = instructionCallback.ldc_s(constInst->getValue().s);
            break;
        case Type::Double:
            cgInst = instructionCallback.ldc_d(constInst->getValue().d);
            break;
        case Type::NullObject:
            cgInst = instructionCallback.ldnull(false);
            break;
        case Type::CompressedNullObject:
            cgInst = instructionCallback.ldnull(true);
            break;
        default: assert(0);
                }
                isConstant = true;
            }
            break;
        case Op_LdRef:
            {
                if (!genConsts) break;

                AutoCompressModifier acmod = inst->getAutoCompressModifier();

                TokenInst *tokenInst = (TokenInst *)inst;
                U_32 token = tokenInst->getToken();
                cgInst = instructionCallback.ldRef(inst->getDst()->getType(),
                    tokenInst->getEnclosingMethod(),
                    token, acmod==AutoCompress_Yes);
                isConstant = true;
            }
            break;
        case Op_LdVar:
            {
                VarAccessInst * varInst = (VarAccessInst *)inst;
                cgInst = instructionCallback.ldVar(inst->getDst()->getType(),
                    getVarHandle(varInst->getVar()));
            }
            break;
        case Op_LdVarAddr:
            {
                if (!genConsts) break;
                VarAccessInst * varInst = (VarAccessInst *)inst;
                cgInst = instructionCallback.ldVarAddr(getVarHandle(varInst->getVar()));
                isConstant = true;
            }
            break;
        case Op_TauLdInd:
            {
                assert(inst->getNumSrcOperands() == 3);
                AutoCompressModifier acmod = inst->getAutoCompressModifier();
                SpeculativeModifier   smod = inst->getSpeculativeModifier();

                instructionCallback.setCurrentPersistentId(inst->getPersistentInstructionId());
                Opnd *ptr = inst->getSrc(0);
                Opnd *tauNonNullBase = inst->getSrc(1);
                Opnd *tauAddressInRange = inst->getSrc(2);

                assert(tauNonNullBase->getType()->tag == Type::Tau);
                assert(tauAddressInRange->getType()->tag == Type::Tau);

                cgInst = instructionCallback.tau_ldInd(inst->getDst()->getType(),
                    getCGInst(ptr),
                    inst->getType(),
                    acmod == AutoCompress_Yes,
                    smod == Speculative_Yes,
                    getCGInst(tauNonNullBase),
                    getCGInst(tauAddressInRange));
                instructionCallback.clearCurrentPersistentId();
            }
            break;
        case Op_TauLdField:
            {
                assert(inst->getNumSrcOperands() == 3);
                AutoCompressModifier acmod = inst->getAutoCompressModifier();

                FieldAccessInst* fieldInst = (FieldAccessInst*)inst;
                FieldDesc* fieldDesc = fieldInst->getFieldDesc();
                Opnd *base = inst->getSrc(0);
                Opnd *tauNonNullBase = inst->getSrc(1);
                Opnd *tauObjectTypeChecked = inst->getSrc(2);

                assert(tauNonNullBase->getType()->tag == Type::Tau);
                assert(tauObjectTypeChecked->getType()->tag == Type::Tau);

                cgInst = instructionCallback.tau_ldField(inst->getDst()->getType(),
                    getCGInst(base),
                    inst->getType(),
                    fieldDesc,
                    acmod == AutoCompress_Yes,
                    getCGInst(tauNonNullBase),
                    getCGInst(tauObjectTypeChecked));
            }
            break;
        case Op_LdStatic:
            {
                AutoCompressModifier acmod = inst->getAutoCompressModifier();

                FieldAccessInst* fieldInst = (FieldAccessInst *)inst;
                FieldDesc* fieldDesc = fieldInst->getFieldDesc();
                cgInst = instructionCallback.ldStatic(inst->getDst()->getType(),
                    fieldDesc,
                    inst->getType(),
                    acmod == AutoCompress_Yes);
            }
            break;
        case Op_TauLdElem:
            {
                assert(inst->getNumSrcOperands() == 4);
                AutoCompressModifier acmod = inst->getAutoCompressModifier();

                Opnd *array = inst->getSrc(0);
                Opnd *index = inst->getSrc(1);
                Opnd *tauNonNullBase = inst->getSrc(2);
                Opnd *tauAddressInRange = inst->getSrc(3);

                assert(tauNonNullBase->getType()->tag == Type::Tau);
                assert(tauAddressInRange->getType()->tag == Type::Tau);

                cgInst = instructionCallback.tau_ldElem(inst->getDst()->getType(),
                    getCGInst(array),
                    getCGInst(index),
                    acmod == AutoCompress_Yes,
                    getCGInst(tauNonNullBase),
                    getCGInst(tauAddressInRange));
            }
            break;
        case Op_LdFieldAddr:
            {
                assert(inst->getNumSrcOperands() == 1);
                FieldAccessInst* fieldInst = (FieldAccessInst*)inst;
                FieldDesc* fieldDesc = fieldInst->getFieldDesc();
                cgInst = instructionCallback.ldFieldAddr(inst->getDst()->getType(),
                    getCGInst(inst->getSrc(0)),
                    fieldDesc);
            }
            break;
        case Op_LdStaticAddr:
            {
                assert(inst->getNumSrcOperands() == 0);
                FieldAccessInst* fieldInst = (FieldAccessInst *)inst;
                FieldDesc* fieldDesc = fieldInst->getFieldDesc();
                cgInst = instructionCallback.ldStaticAddr(inst->getDst()->getType(),
                    fieldDesc);
            }
            break;
        case Op_LdElemAddr:
            {
                assert(inst->getNumSrcOperands() == 2);
                cgInst = instructionCallback.ldElemAddr(getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
            }
            break;
        case Op_LdFunAddr:
            {
                assert(inst->getNumSrcOperands() == 0);
                MethodInst * methodInst = (MethodInst *)inst;
                MethodDesc * methodDesc = methodInst->getMethodDesc();
                cgInst = 
                    instructionCallback.ldFunAddr(inst->getDst()->getType(),
                    methodDesc);
            }
            break;
        case Op_TauLdVirtFunAddr:
            {
                assert(inst->getNumSrcOperands() == 2);
                MethodInst * methodInst = (MethodInst *)inst;
                MethodDesc * methodDesc = methodInst->getMethodDesc();
                Opnd *vtable = inst->getSrc(0);
                Opnd *tauBaseHasMethod = inst->getSrc(1);

                assert(tauBaseHasMethod->getType()->tag == Type::Tau);

                cgInst = 
                    instructionCallback.tau_ldVirtFunAddr(inst->getDst()->getType(),
                    getCGInst(vtable),
                    methodDesc,
                    getCGInst(tauBaseHasMethod));
            }
            break;
        case Op_LdFunAddrSlot:
            {
                assert(inst->getNumSrcOperands() == 0);
                MethodInst * methodInst = (MethodInst *)inst;
                MethodDesc * methodDesc = methodInst->getMethodDesc();
                cgInst = 
                    instructionCallback.ldFunAddr(inst->getDst()->getType(),
                    methodDesc);
            }
            break;
        case Op_TauLdVirtFunAddrSlot:
            {
                assert(inst->getNumSrcOperands() == 2);
                MethodInst * methodInst = (MethodInst *)inst;
                MethodDesc * methodDesc = methodInst->getMethodDesc();
                Opnd *vtable = inst->getSrc(0);
                Opnd *tauBaseHasMethod = inst->getSrc(1);

                assert(tauBaseHasMethod->getType()->tag == Type::Tau);

                cgInst = 
                    instructionCallback.tau_ldVirtFunAddr(inst->getDst()->getType(),
                    getCGInst(vtable),
                    methodDesc,
                    getCGInst(tauBaseHasMethod));
            }
            break;
        case Op_TauLdVTableAddr:
            {
                assert(inst->getNumSrcOperands() == 2);
                instructionCallback.setCurrentPersistentId(inst->getPersistentInstructionId());
                Type * dstType = inst->getDst()->getType();
                assert(dstType->isVTablePtr() || dstType->isVTablePtrObj());
                Opnd *base = inst->getSrc(0);
                Opnd *tauBaseNonNull = inst->getSrc(1);

                assert(tauBaseNonNull->getType()->tag == Type::Tau);

                cgInst = instructionCallback.tau_ldVTableAddr(dstType,
                    getCGInst(base),
                    getCGInst(tauBaseNonNull));
                instructionCallback.clearCurrentPersistentId();
            }
            break;
        case Op_GetVTableAddr:
            {
                assert(inst->getNumSrcOperands() == 0);
                if (!genConsts) break;
                Type *type = ((TypeInst*)inst)->getTypeInfo();
                assert(type->isObject());
                Type * dstType = inst->getDst()->getType();
                assert(dstType->isVTablePtr());
                cgInst = instructionCallback.getVTableAddr( dstType,(ObjectType*)type);
                isConstant = true;
            }
            break;
        case Op_GetClassObj:
            {
                assert(inst->getNumSrcOperands() == 0);
                Type *type = ((TypeInst*)inst)->getTypeInfo();
                assert(type->isObject() || type->isUserValue());
                Type * dstType = inst->getDst()->getType();
                assert(type->isObject());
                cgInst = instructionCallback.getClassObj( dstType,(ObjectType*)type);
            }
            break;
        case Op_TauLdIntfcVTableAddr:
            {
                assert(inst->getNumSrcOperands() == 1);
                TypeInst *typeInst = (TypeInst*)inst;
                Type * vtableType = typeInst->getTypeInfo();
                assert(vtableType->isUserObject());
                Opnd *base = inst->getSrc(0);
                cgInst = instructionCallback.tau_ldIntfTableAddr(inst->getDst()->getType(),
                    getCGInst(base),
                    (NamedType*)vtableType);
            }
            break;
        case Op_TauArrayLen:
            {
                assert(inst->getNumSrcOperands() == 3);
                Opnd *array = inst->getSrc(0);
                Opnd *tauNonNullArray = inst->getSrc(1);
                Opnd *tauIsArray = inst->getSrc(2);

                assert(tauNonNullArray->getType()->tag == Type::Tau);

                Type* dstType = inst->getDst()->getType();
                Type* arrayType = inst->getSrc(0)->getType();
                assert(arrayType->isArrayType());
                Type* arrayLenType = dstType;
                instructionCallback.setCurrentPersistentId(inst->getPersistentInstructionId());
                cgInst = instructionCallback.tau_arrayLen(dstType, 
                    (ArrayType*)arrayType,  
                    arrayLenType,
                    getCGInst(array),
                    getCGInst(tauNonNullArray),
                    getCGInst(tauIsArray));
                instructionCallback.clearCurrentPersistentId();
            }
            break;
            // load the base (zero element) address of array
        case Op_LdArrayBaseAddr:
            {
                assert(inst->getNumSrcOperands() == 1);
                cgInst = instructionCallback.ldElemBaseAddr(getCGInst(inst->getSrc(0)));
            }
            break;
            // Add a scaled index to an array element address
        case Op_AddScaledIndex:    
            {
                PtrType* arrType = (PtrType*) inst->getSrc(0)->getType();
                Type*    refType = arrType->getPointedToType();

                assert(inst->getNumSrcOperands() == 2);
                cgInst = instructionCallback.addElemIndex(refType,
                    getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
            }
            break;
        case Op_UncompressRef:
            {
                assert(inst->getNumSrcOperands() == 1);
                cgInst = instructionCallback.uncompressRef(getCGInst(inst->getSrc(0)));
            }
            break;
        case Op_CompressRef:
            {
                assert(inst->getNumSrcOperands() == 1);
                cgInst = instructionCallback.compressRef(getCGInst(inst->getSrc(0)));
            }
            break;
        case Op_LdFieldOffset:
            {
                assert(inst->getNumSrcOperands() == 0);
                if (!genConsts) break;
                FieldAccessInst* fieldInst = (FieldAccessInst*)inst;
                FieldDesc* fieldDesc = fieldInst->getFieldDesc();
                cgInst = instructionCallback.ldFieldOffset(fieldDesc);
                isConstant = true;
            }
            break;
        case Op_LdFieldOffsetPlusHeapbase:
            {
                assert(inst->getNumSrcOperands() == 0);
                if (!genConsts) break;
                FieldAccessInst* fieldInst = (FieldAccessInst*)inst;
                FieldDesc* fieldDesc = fieldInst->getFieldDesc();
                cgInst = instructionCallback.ldFieldOffsetPlusHeapbase(fieldDesc);
                isConstant = true;
            }
            break;
        case Op_LdArrayBaseOffset:
            {
                assert(inst->getNumSrcOperands() == 0);
                if (!genConsts) break;
                TypeInst *typeInst = (TypeInst*)inst;
                Type *elemType = typeInst->getTypeInfo();
                cgInst = instructionCallback.ldArrayBaseOffset(elemType);
                isConstant = true;
            }
            break;
        case Op_LdArrayBaseOffsetPlusHeapbase:
            {
                assert(inst->getNumSrcOperands() == 0);
                if (!genConsts) break;
                TypeInst *typeInst = (TypeInst*)inst;
                Type *elemType = typeInst->getTypeInfo();
                cgInst = instructionCallback.ldArrayBaseOffsetPlusHeapbase(elemType);
                isConstant = true;
            }
            break;
        case Op_LdArrayLenOffset:
            {
                assert(inst->getNumSrcOperands() == 0);
                if (!genConsts) break;
                TypeInst *typeInst = (TypeInst*)inst;
                Type *elemType = typeInst->getTypeInfo();
                cgInst = instructionCallback.ldArrayLenOffset(elemType);
                isConstant = true;
            }
            break;
        case Op_LdArrayLenOffsetPlusHeapbase:
            {
                assert(inst->getNumSrcOperands() == 0);
                if (!genConsts) break;
                TypeInst *typeInst = (TypeInst*)inst;
                Type *elemType = typeInst->getTypeInfo();
                cgInst = instructionCallback.ldArrayLenOffsetPlusHeapbase(elemType);
                isConstant = true;
            }
            break;
        case Op_AddOffset:
            {
                assert(inst->getNumSrcOperands() == 2);
                Opnd *dstop = inst->getDst();
                Opnd *ref = inst->getSrc(0);
                Opnd *offset = inst->getSrc(1);
                Type *dstType = dstop->getType();
                cgInst = instructionCallback.addOffset(dstType, getCGInst(ref), getCGInst(offset));
            }
            break;
        case Op_AddOffsetPlusHeapbase:
            {
                assert(inst->getNumSrcOperands() == 2);
                Opnd *dstop = inst->getDst();
                Opnd *ref = inst->getSrc(0);
                Opnd *offset = inst->getSrc(1);
                Type *dstType = dstop->getType();
                cgInst = instructionCallback.addOffsetPlusHeapbase(dstType, 
                    getCGInst(ref), 
                    getCGInst(offset));
            }
            break;
        case Op_StVar:
            {
                assert(inst->getNumSrcOperands() == 1);
                VarAccessInst * varInst = (VarAccessInst *)inst;
                instructionCallback.stVar(getCGInst(inst->getSrc(0)), 
                    getVarHandle(varInst->getVar()));  

            }
            break;
        case Op_TauStInd:
            {
                assert(inst->getNumSrcOperands() == 5);
                Opnd *src = inst->getSrc(0);
                Opnd *ptr = inst->getSrc(1);
                Opnd *tauNonNullBase = inst->getSrc(2);
                Opnd *tauAddressInRange = inst->getSrc(3);
                Opnd *tauElemTypeChecked = inst->getSrc(4);

                assert(tauNonNullBase->getType()->tag == Type::Tau);
                assert(tauAddressInRange->getType()->tag == Type::Tau);
                assert(tauElemTypeChecked->getType()->tag == Type::Tau);

                AutoCompressModifier acmod = inst->getAutoCompressModifier();
                bool autocompress = (acmod == AutoCompress_Yes);
                Type::Tag type = inst->getType();
                if (acmod == AutoCompress_Yes) {
                    assert(Type::isReference(type));
                    assert(!Type::isCompressedReference(type));
                }
                instructionCallback.tau_stInd(getCGInst(src),
                    getCGInst(ptr),
                    type,
                    autocompress,
                    getCGInst(tauNonNullBase),
                    getCGInst(tauAddressInRange),
                    getCGInst(tauElemTypeChecked));
            }
            break;
        case Op_TauStField:
            {
                assert(inst->getNumSrcOperands() == 5);
                Opnd *src = inst->getSrc(0);
                Opnd *base = inst->getSrc(1);
                Opnd *tauNonNullBase = inst->getSrc(2);
                Opnd *tauTypeHasField = inst->getSrc(3);
                Opnd *tauFieldTypeChecked = inst->getSrc(4);

                assert(tauNonNullBase->getType()->tag == Type::Tau);
                assert(tauTypeHasField->getType()->tag == Type::Tau);
                assert(tauFieldTypeChecked->getType()->tag == Type::Tau);

                AutoCompressModifier acmod = inst->getAutoCompressModifier();

                FieldAccessInst* fieldInst = (FieldAccessInst*)inst;
                FieldDesc* fieldDesc = fieldInst->getFieldDesc();

                bool autocompress = (acmod == AutoCompress_Yes);
                Type::Tag type = inst->getType();
                if (acmod == AutoCompress_Yes) {
                    assert(Type::isReference(type));
                    assert(!Type::isCompressedReference(type));
                }
                instructionCallback.tau_stField(getCGInst(src),
                    getCGInst(base),
                    type,
                    fieldDesc,
                    autocompress,
                    getCGInst(tauNonNullBase),
                    getCGInst(tauTypeHasField),
                    getCGInst(tauFieldTypeChecked));
            }
            break;

        case Op_TauStStatic: 
            {
                assert(inst->getNumSrcOperands() == 2);
                Opnd *src = inst->getSrc(0);
                Opnd *tauFieldTypeOk = inst->getSrc(1);

                AutoCompressModifier acmod = inst->getAutoCompressModifier();
                bool autocompress = (acmod == AutoCompress_Yes);
                Type::Tag type = inst->getType();
                if (acmod == AutoCompress_Yes) {
                    assert(Type::isReference(type));
                    assert(!Type::isCompressedReference(type));
                }
                FieldAccessInst* fieldInst = (FieldAccessInst *)inst;
                FieldDesc* fieldDesc = fieldInst->getFieldDesc();
                instructionCallback.tau_stStatic(getCGInst(src),
                    fieldDesc,
                    type,
                    autocompress,
                    getCGInst(tauFieldTypeOk));
            }
            break;
        case Op_TauStElem:
            {
                assert(inst->getNumSrcOperands() == 6);
                Opnd *src = inst->getSrc(0);
                Opnd *array = inst->getSrc(1);
                Opnd *index = inst->getSrc(2);
                AutoCompressModifier acmod = inst->getAutoCompressModifier();
                Opnd *tauNonNullBase = inst->getSrc(3);
                Opnd *tauAddressInRange = inst->getSrc(4);
                Opnd *tauElemTypeChecked = inst->getSrc(5);

                assert(tauNonNullBase->getType()->tag == Type::Tau);
                assert(tauAddressInRange->getType()->tag == Type::Tau);
                assert(tauElemTypeChecked->getType()->tag == Type::Tau);


                instructionCallback.tau_stElem(getCGInst(src),
                    getCGInst(array),
                    getCGInst(index),
                    acmod == AutoCompress_Yes,
                    getCGInst(tauNonNullBase),
                    getCGInst(tauAddressInRange),
                    getCGInst(tauElemTypeChecked));
            }
            break;
        case Op_TauStRef:
            {
                assert(inst->getNumSrcOperands() == 6);

                Opnd *src = inst->getSrc(0);
                Opnd *ptr = inst->getSrc(1);
                Opnd *base = inst->getSrc(2);

                Opnd *tauNonNullBase = inst->getSrc(3);
                Opnd *tauTypeHasField = inst->getSrc(4);
                Opnd *tauFieldTypeChecked = inst->getSrc(5);

                assert(tauNonNullBase->getType()->tag == Type::Tau);
                assert(tauTypeHasField->getType()->tag == Type::Tau);
                assert(tauFieldTypeChecked->getType()->tag == Type::Tau);

                AutoCompressModifier acmod = inst->getAutoCompressModifier();
                StoreModifier UNUSED stmod = inst->getStoreModifier();

                bool autocompress = (acmod == AutoCompress_Yes);
                Type::Tag type = inst->getType();
                if (acmod == AutoCompress_Yes) {
                    assert(Type::isReference(type));
                    assert(!Type::isCompressedReference(type));
                }
                assert(stmod == Store_WriteBarrier);
                instructionCallback.tau_stRef(getCGInst(src),
                    getCGInst(ptr),
                    getCGInst(base),
                    type,
                    autocompress,
                    getCGInst(tauNonNullBase),
                    getCGInst(tauTypeHasField),
                    getCGInst(tauFieldTypeChecked));
            }
            break;
        case Op_TauCheckBounds:
            {
                assert(inst->getNumSrcOperands() == 2);
                cgInst = instructionCallback.tau_checkBounds(getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
            }
            break;
        case Op_TauCheckLowerBound:
            {
                assert(inst->getNumSrcOperands() == 2);
                cgInst =
                    instructionCallback.tau_checkLowerBound(getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
            }
            break;
        case Op_TauCheckUpperBound:
            {
                assert(inst->getNumSrcOperands() == 2);
                cgInst =
                    instructionCallback.tau_checkUpperBound(getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
            }
            break;
        case Op_TauCheckNull:
            {
                assert(inst->getNumSrcOperands() == 1);
                if ( inst->getDefArgModifier() == NonNullThisArg ) {
                    if(Log::isEnabled()) {
                        Log::out() << " chknull_NonNullThisArg_check" << ::std::endl;
                    }
                    cgInst = instructionCallback.tau_checkNull(getCGInst(inst->getSrc(0)), true);
                }else{
                    cgInst = instructionCallback.tau_checkNull(getCGInst(inst->getSrc(0)), false);
                    assert(inst->getDefArgModifier() == DefArgNoModifier);
                }
            }
            break;
        case Op_TauCheckZero:
            {
                assert(inst->getNumSrcOperands() == 1);
                cgInst = instructionCallback.tau_checkZero(getCGInst(inst->getSrc(0)));
            }
            break;
        case Op_TauCheckDivOpnds:
            {
                assert(inst->getNumSrcOperands() == 2);
                cgInst = instructionCallback.tau_checkDivOpnds(getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
            }
            break;
        case Op_TauCheckElemType:
            {
                assert(inst->getNumSrcOperands() == 4);
                Opnd *array = inst->getSrc(0);
                Opnd *src = inst->getSrc(1);
                Opnd *tauCheckedNull = inst->getSrc(2);
                Opnd *tauIsArray = inst->getSrc(3);
                assert(tauCheckedNull->getType()->tag == Type::Tau);
                cgInst = instructionCallback.tau_checkElemType(getCGInst(array),
                    getCGInst(src),
                    getCGInst(tauCheckedNull),
                    getCGInst(tauIsArray));
            }
            break;
        case Op_NewObj:
            {
                assert(inst->getNumSrcOperands() == 0);
                TypeInst *typeInst = (TypeInst*)inst;
                Type * objType = typeInst->getTypeInfo();
                assert(objType->isObject());
                cgInst = instructionCallback.newObj((ObjectType*)objType);
            }
            break;
        case Op_NewArray:
            {
                assert(inst->getNumSrcOperands() == 1);
                Type * arrayType = inst->getDst()->getType();
                assert(arrayType->isArrayType());
                cgInst = instructionCallback.newArray((ArrayType *)arrayType,
                    getCGInst(inst->getSrc(0)));
            }
            break;
        case Op_NewMultiArray:
            {
                Type * arrayType = inst->getDst()->getType();
                assert(arrayType->isArrayType());
                U_32 numDims = inst->getNumSrcOperands();
                CG_OpndHandle ** dims = new(memManager) CG_OpndHandle*[numDims];
                for (U_32 i = 0; i < numDims; i++) 
                    dims[i] = getCGInst(inst->getSrc(i));
                cgInst = instructionCallback.newMultiArray((ArrayType*)arrayType,
                    numDims,
                    dims);
            }
            break;
        case Op_TauMonitorEnter:
            {
                assert(inst->getNumSrcOperands() == 2);
                Opnd *tauOpnd = inst->getSrc(1);
                instructionCallback.tau_monitorEnter(getCGInst(inst->getSrc(0)),
                    getCGInst(tauOpnd));
            }
            break;
        case Op_TauMonitorExit:
            {
                assert(inst->getNumSrcOperands() == 2);
                Opnd *tauOpnd = inst->getSrc(1);
                instructionCallback.tau_monitorExit(getCGInst(inst->getSrc(0)),
                    getCGInst(tauOpnd));
            }
            break;
        case Op_LdLockAddr:
            {
                assert(inst->getNumSrcOperands() == 1);
                cgInst = instructionCallback.ldLockAddr(getCGInst(inst->getSrc(0)));
            }
            break;
        case Op_IncRecCount:
            {
                assert(inst->getNumSrcOperands() == 2);
                instructionCallback.incRecursionCount(getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)));
            }
            break;
        case Op_TauBalancedMonitorEnter:
            {
                assert(inst->getNumSrcOperands() == 3);
                Opnd *tauOpnd = inst->getSrc(2);
                cgInst = instructionCallback.tau_balancedMonitorEnter(getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)),
                    getCGInst(tauOpnd));
            }
            break;
        case Op_BalancedMonitorExit:
            {
                assert(inst->getNumSrcOperands() == 3);
                instructionCallback.balancedMonitorExit(getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)),
                    getCGInst(inst->getSrc(2)));
            }
            break;
        case Op_TauOptimisticBalancedMonitorEnter:
            {
                assert(inst->getNumSrcOperands() == 3);
                Opnd *tauOpnd = inst->getSrc(2);
                cgInst = instructionCallback.tau_optimisticBalancedMonitorEnter(getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)),
                    getCGInst(tauOpnd));
            }
            break;
        case Op_OptimisticBalancedMonitorExit:
            {
                assert(inst->getNumSrcOperands() == 3);
                instructionCallback.optimisticBalancedMonitorExit(getCGInst(inst->getSrc(0)),
                    getCGInst(inst->getSrc(1)),
                    getCGInst(inst->getSrc(2)));
            }
            break;
        case Op_MonitorEnterFence:
            {
                assert(inst->getNumSrcOperands() == 1);
                instructionCallback.monitorEnterFence(getCGInst(inst->getSrc(0)));
            }
            break;
        case Op_MonitorExitFence:
            {
                assert(inst->getNumSrcOperands() == 1);
                instructionCallback.monitorExitFence(getCGInst(inst->getSrc(0)));
            }
            break;
        case Op_TypeMonitorEnter:
            {
                assert(inst->getNumSrcOperands() == 0);
                Type * type = ((TypeInst*)inst)->getTypeInfo();
                assert(type->isObject() || type->isUserValue());
                instructionCallback.typeMonitorEnter((NamedType *)type);
            }
            break;
        case Op_TypeMonitorExit:
            {
                assert(inst->getNumSrcOperands() == 0);
                Type * type = ((TypeInst*)inst)->getTypeInfo();
                assert(type->isObject() || type->isUserValue());
                instructionCallback.typeMonitorExit((NamedType *)type);
            }
            break;
        case Op_TauStaticCast:
            {
                assert(inst->getNumSrcOperands() == 2);
                Opnd *src = inst->getSrc(0);
                Opnd *tauCastChecked = inst->getSrc(1);
                assert(tauCastChecked->getType()->tag == Type::Tau);
                Type * toType = ((TypeInst*)inst)->getTypeInfo();
                assert(toType->isObject());
                cgInst = instructionCallback.tau_staticCast((ObjectType *)toType,
                    getCGInst(src),
                    getCGInst(tauCastChecked));
            }
            break;
        case Op_TauCast:
            {
                assert(inst->getNumSrcOperands() == 2);
                Type * toType = ((TypeInst*)inst)->getTypeInfo();
                assert(toType->isObject());
                Opnd *src = inst->getSrc(0);
                Opnd *tauCheckedNull = inst->getSrc(1);
                assert(tauCheckedNull->getType()->tag == Type::Tau);
                cgInst = instructionCallback.tau_cast((ObjectType *)toType,
                    getCGInst(src),
                    getCGInst(tauCheckedNull));
            }
            break;
        case Op_TauAsType:
            {
                assert(inst->getNumSrcOperands() == 2);
                Type * toType = ((TypeInst*)inst)->getTypeInfo();
                assert(toType->isObject());
                Opnd *src = inst->getSrc(0);
                Opnd *tauCheckedNull = inst->getSrc(1);
                assert(tauCheckedNull->getType()->tag == Type::Tau);
                cgInst = instructionCallback.tau_asType((ObjectType *)toType,
                    getCGInst(src),
                    getCGInst(tauCheckedNull));
            }
            break;
        case Op_TauInstanceOf:
            {
                assert(inst->getNumSrcOperands() == 2);
                Type * toType = ((TypeInst*)inst)->getTypeInfo();
                assert(toType->isObject());
                Opnd *src = inst->getSrc(0);
                Opnd *tauCheckedNull = inst->getSrc(1);
                assert(tauCheckedNull->getType()->tag == Type::Tau);
                cgInst = instructionCallback.tau_instanceOf((ObjectType *)toType,
                    getCGInst(src),
                    getCGInst(tauCheckedNull));
            }
            break;
        case Op_InitType:
            {
                assert(inst->getNumSrcOperands() == 0);
                TypeInst *typeInst = (TypeInst*)inst;
                instructionCallback.initType(typeInst->getTypeInfo());
            }
            break;
        case Op_Label:
            break;      // nothing to do
        case Op_MethodEntry:
            {  
                assert(inst->isMethodMarker());
                MethodMarkerInst* methEntryInst = inst->asMethodMarkerInst();
                instructionCallback.methodEntry(methEntryInst->getMethodDesc());
            }
            break;      // nothing to do
        case Op_MethodEnd:
            {  
                assert(inst->isMethodMarker());
                MethodMarkerInst* methEntryInst = inst->asMethodMarkerInst();
                // check that inst->getSrc(0) is really retOpnd and not thisOpnd
                CG_OpndHandle* ret_val = inst->getNumSrcOperands()==0 ? NULL : 
                getCGInst(inst->getSrc(0));
                instructionCallback.methodEnd(methEntryInst->getMethodDesc(), 
                    ret_val);
            }
            break;      // nothing to do
        case Op_Phi:
            {
                assert(0); // Phi nodes should be eliminated by deSSAing
            }
            break;
        case Op_TauPi:
            {
                assert(0);
            }
            break;
        case Op_IncCounter:
            {
                TokenInst *counterInst = (TokenInst *)inst;
                U_32 counter = counterInst->getToken();
                instructionCallback.incCounter(irmanager.getTypeManager().getUInt32Type(), counter);
            }
            break;
        case Op_Prefetch:
            {
                assert(inst->getNumSrcOperands() == 1);
                Opnd* addr= inst->getSrc(0);
                instructionCallback.prefetch(getCGInst(addr));
            }
            break;
        case Op_TauPoint:
            {
                assert(inst->getNumSrcOperands() == 0);
                cgInst = instructionCallback.tauPoint();
            }
            break;
        case Op_TauEdge:
            {
                assert(inst->getNumSrcOperands() == 0);
                cgInst = instructionCallback.tauEdge();
            }
            break;
        case Op_TauAnd:
            {
                U_32 numSrcs = inst->getNumSrcOperands();
                CG_OpndHandle **args = genCallArgs(inst, 0);
                cgInst = instructionCallback.tauAnd(numSrcs, args);
            }
            break;
        case Op_TauUnsafe:
            {
                assert(inst->getNumSrcOperands() == 0);
                cgInst = instructionCallback.tauUnsafe();
            }
            break;
        case Op_TauSafe:
            {
                assert(inst->getNumSrcOperands() == 0);
                cgInst = instructionCallback.tauSafe();
            }
            break;
        case Op_TauCheckCast:
            {
                assert(inst->getNumSrcOperands() == 2);
                Opnd *src = inst->getSrc(0);
                Opnd *tauCheckedNull = inst->getSrc(1);
                assert(tauCheckedNull->getType()->tag == Type::Tau);
                Type * toType = ((TypeInst*)inst)->getTypeInfo();
                assert(toType->isObject());
                cgInst = instructionCallback.tau_checkCast((ObjectType *)toType,
                    getCGInst(src),
                    getCGInst(tauCheckedNull));
            }
            break;
        case Op_TauHasType:
            {
                cgInst = instructionCallback.tauPoint();
            }
            break;
        case Op_TauHasExactType:
            {
                cgInst = instructionCallback.tauPoint();
            }
            break;
        case Op_TauIsNonNull:
            {
                cgInst = instructionCallback.tauPoint();
            }
            break;
        default:
            assert(0);
    } // end switch

    if (cgInst) { // record mapping from dst
        if(Log::isEnabled()) {
            Log::out() << "genInstCode ";
            inst->print(Log::out());
            if (genConsts) {
                Log::out() << "; genConsts=true";
            } else {
                Log::out() << "; genConsts=false";
            }
            Log::out() << " has cgInst"  << ::std::endl;
        }

        assert(dst);
        if (isConstant) {
            if(Log::isEnabled()) {
                Log::out() << " isConstant" << ::std::endl;
            }
            assert(genConsts);
            if (sinkConstants) {
                if(Log::isEnabled()) {
                    Log::out() << " sinkConstants=true" << ::std::endl;
                }
                setLocalCGInst(cgInst, dst);
            } else {
                if(Log::isEnabled()) {
                    Log::out() << " sinkConstants=false" << ::std::endl;
                }
                setCGInst(cgInst, dst);
            }
        } else {
            if(Log::isEnabled()) {
                Log::out() << " isConstant=false" << ::std::endl;
            }
            setCGInst(cgInst, dst);
            if (sinkConstants && sinkConstantsOne) {
                // we just finished a non-constant inst, clear the constant inst map
                clearLocalCGInsts();
            }
        }            
    } else {
        if(Log::isEnabled()) {
            Log::out() << "genInstCode ";
            inst->print(Log::out());
            if (genConsts) {
                Log::out() << "; genConsts=true";
            } else {
                Log::out() << "; genConsts=false";
            }
            Log::out() << " has NO cgInst"  << ::std::endl;
        }
    }
}

void _BlockCodeSelector::genCode(InstructionCallback& instructionCallback) {
    callback = &instructionCallback;
    //
    // go through instructions
    //
    Inst*    labelInst = (Inst*)block->getFirstInst();
    Inst*    inst = labelInst->getNextInst();
    while (inst != NULL) {
        if(Log::isEnabled()) {
            Log::out() << "Code select ";
            inst->print(Log::out());
            Log::out() << ::std::endl;
        }
        genInstCode(instructionCallback, inst, !sinkConstants);
        inst = inst->getNextInst();
    }
}

CG_OpndHandle* _BlockCodeSelector::getCGInst(Opnd* opnd) {
    CG_OpndHandle *res = opndToCGInstMap[opnd->getId()];
    if (!res) {
        assert(sinkConstants);
        res = localOpndToCGInstMap[opnd->getId()];
        if (!res) {
            uint16 savedBCOffset = callback->getCurrentHIRInstBCOffset();
            genInstCode(*callback, opnd->getInst(), true); // generate code for the constant
            res = localOpndToCGInstMap[opnd->getId()];
            assert(res);
            callback->setCurrentHIRInstBCOffset(savedBCOffset);
        }
    }
    return res;
}
    
void _BlockCodeSelector::setLocalCGInst(CG_OpndHandle* inst, Opnd* opnd) {
    assert(sinkConstants);
    localOpndToCGInstMap[opnd->getId()] = inst;
}
    
void _BlockCodeSelector::clearLocalCGInsts() {
    assert(sinkConstants && sinkConstantsOne);
    localOpndToCGInstMap.clear();
}

void _BlockCodeSelector::setCGInst(CG_OpndHandle* inst,Opnd* opnd) {
    opndToCGInstMap[opnd->getId()] = inst;
    if (opnd->isGlobal()) 
        callback->opndMaybeGlobal(inst);
}

U_32 _BlockCodeSelector::getVarHandle(VarOpnd *var) {
    return varIdMap[var->getId()];
}


void _CFGCodeSelector::genCode(Callback& callback) {
    bool    hasEdgeProfile = flowGraph->hasEdgeProfile();
    //
    // go through nodes in flow graph and call genDispatchNode for nodes
    // that are handlers (dispatchers) and genBlock for nodes that are blocks.
    // record the integer id that is returned and map it to the node.
    //

    //
    // this table maps from a node's depth-first number to the 
    // node id returned by the code selector
    //
    StlVector<MethodDesc*> inlineEndMarkers(memManager);
    U_32*    nodeMapTable = new (memManager) U_32[numNodes];
    
    // Compute postorder list to get only reachable nodes.
    const Nodes& nodes = flowGraph->getNodesPostOrder();

    assert(flowGraph->getExitNode()->getTraversalNum() == flowGraph->getTraversalNum());
    Node* unwind = flowGraph->getUnwindNode();
    Node* exit = flowGraph->getExitNode();
    // Use reverse iterator to generate nodes in reverse postorder.
    Nodes::const_reverse_iterator niter;
    for(niter = nodes.rbegin(); niter != nodes.rend(); ++niter) {
        Node* node = *niter;
        //
        // Count in and out edges
        //
        U_32 numOutEdges = node->getOutDegree();
        U_32 numInEdges = node->getInDegree();
        U_32 nodeId = MAX_UINT32;
        double cnt = (hasEdgeProfile? node->getExecCount() : -1.0);

        if (node == exit) {
            nodeId = callback.genExitNode(numInEdges,cnt);
        }
        else if (node == unwind) {
            nodeId = callback.genUnwindNode(numInEdges,numOutEdges,cnt);
        }
        else if (node->isBlockNode()) {
            _BlockCodeSelector    blockCodeSelector(memManager,
                irmanager,
                node,
                opndToCGInstMap,
                varIdMap,
                sinkConstants,
                sinkConstantsOne);


            //
            //  Derive the block kind (prolog, epilog, inner block)
            //
            CFGCodeSelector::Callback::BlockKind blockKind;
            if (node == flowGraph->getEntryNode())
                blockKind = CFGCodeSelector::Callback::Prolog;
            else if (node == flowGraph->getReturnNode())
                blockKind = CFGCodeSelector::Callback::Epilog;
            else
                blockKind = CFGCodeSelector::Callback::InnerBlock;
            nodeId = callback.genBlock(numInEdges,
                numOutEdges,
                blockKind,
                blockCodeSelector,
                cnt);

        } else if (node->isDispatchNode()) {
            if(node->getSecondInst()!=NULL) {
                for (Inst* inst = (Inst*)node->getSecondInst(); inst!=NULL; inst = inst->getNextInst()) {
                    assert(inst->isMethodMarker());
                    MethodMarkerInst* marker = inst->asMethodMarkerInst();
                    assert(marker->getOpcode() == Op_MethodEnd);
                    inlineEndMarkers.push_back(marker->getMethodDesc());
                }
            }
            nodeId = callback.genDispatchNode(numInEdges,numOutEdges,inlineEndMarkers, cnt);
            inlineEndMarkers.clear();
        }
        assert(nodeId < numNodes);
        callback.setPersistentId(nodeId, node->getId());
        nodeMapTable[node->getDfNum()] = nodeId;
    }
    //
    // go through edges in flow graph 
    //
    for(niter = nodes.rbegin(); niter != nodes.rend(); ++niter) {
        double    prob;
        Node* tailNode = *niter;
        U_32 tailNodeId = nodeMapTable[tailNode->getDfNum()];
        if (((Inst*)tailNode->getLastInst())->isSwitch()) { 
            //
            //  Generate switch edges
            //
            SwitchInst* sw = (SwitchInst *)tailNode->getLastInst();
            Node *defaultNode = sw->getDefaultTarget()->getNode();
            U_32 defaultNodeId = nodeMapTable[defaultNode->getDfNum()];
            U_32 numTargets = sw->getNumTargets();
            U_32 * targetNodeIds = new (memManager) U_32[numTargets];
            double * targetProbs = new (memManager) double[numTargets];
            U_32 i;
            for (i = 0; i < numTargets; i++) {
                Node *headNode = sw->getTarget(i)->getNode();
                Edge *edge = (Edge *) tailNode->findTargetEdge(headNode);
                targetNodeIds[i] = nodeMapTable[headNode->getDfNum()];
                targetProbs[i] = (hasEdgeProfile? edge->getEdgeProb() : -1.0);
            }
            callback.genSwitchEdges(tailNodeId,numTargets,targetNodeIds, 
                targetProbs, defaultNodeId);
            //
            //  Generate an exception edge if it exists
            //
            Edge* throwEdge = (Edge*) tailNode->getExceptionEdge();
            if(throwEdge != NULL) {
                assert(0);
                Node *succNode = throwEdge->getTargetNode();
                assert(succNode->isDispatchNode());
                U_32 headNodeId = nodeMapTable[succNode->getDfNum()];
                prob = (hasEdgeProfile? throwEdge->getEdgeProb() : -1.0);
                callback.genExceptionEdge(tailNodeId,headNodeId,prob);
            }
        }
        else { 
            //
            // Gen edges for a node that does not end with a switch
            //
            const Edges& edges = tailNode->getOutEdges();
            Edges::const_iterator eiter;
            for(eiter = edges.begin(); eiter != edges.end(); ++eiter) {
                Edge* edge = *eiter;
                prob = (hasEdgeProfile? edge->getEdgeProb() : -1.0);
                Node * headNode = edge->getTargetNode();
                U_32 headNodeId = nodeMapTable[headNode->getDfNum()];
                Edge::Kind edgeKind = edge->getKind();
                switch (edgeKind) {
                    case Edge::Kind_Unconditional:
                        callback.genUnconditionalEdge(tailNodeId,headNodeId,prob);
                        break;
                    case Edge::Kind_True:
                        callback.genTrueEdge(tailNodeId,headNodeId,prob);
                        break;
                    case Edge::Kind_False:
                        callback.genFalseEdge(tailNodeId,headNodeId,prob);
                        break;
                    case Edge::Kind_Dispatch:
                        callback.genExceptionEdge(tailNodeId,headNodeId,prob);
                        break;
                    case Edge::Kind_Catch:
                        {
                            Inst* first = (Inst*)headNode->getFirstInst();
                            assert(first->isLabel() && 
                                ((LabelInst*)first)->isCatchLabel());
                            CatchLabelInst * label = (CatchLabelInst *)first;
                            callback.genCatchEdge(tailNodeId,headNodeId,
                                label->getOrder(),
                                label->getExceptionType(),
                                prob);
                            break;
                        }
                    default:
                        assert(0);
                }
            }
        }
    }
}

void _MethodCodeSelector::selectCode(Callback& callback) {
    MemoryManager localMemManager("_MethodCodeSelector::genCode.localMemManager"); 

    callback.setMethodDesc(methodDesc);

    U_32 *varIdMap = new (localMemManager) U_32[numVars];
    U_32 i;
    for (i = 0; i < numVars; i++) {
        varIdMap[i] = 0;
    }
    _VarCodeSelector varCodeSelector(varOpnds,varIdMap,irmanager.getGCBasePointerMap());
    callback.genVars(varCodeSelector.getNumVarOpnds(),varCodeSelector);

    CG_OpndHandle** opndToCGInstMap = new (localMemManager) CG_OpndHandle*[numOpnds];
    for (i=0; i<numOpnds; i++) { 
        opndToCGInstMap[i] = NULL;
    }
    _CFGCodeSelector    cfgCodeSelector(localMemManager,irmanager,flowGraph,opndToCGInstMap,
        varIdMap, sinkConstants, sinkConstantsOne);

    bool    hasEdgeProfile = flowGraph->hasEdgeProfile();

    callback.genCFG(cfgCodeSelector.getNumNodes(),cfgCodeSelector, hasEdgeProfile);
}

} //namespace Jitrino 
