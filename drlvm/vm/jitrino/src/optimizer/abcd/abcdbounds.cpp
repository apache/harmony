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

#include <ostream>

#include "abcdbounds.h"
#include "constantfolder.h"
#include "opndmap.h"
#include "simplifier.h"

namespace Jitrino {

namespace {
bool hasTypeBounds(Type::Tag srcTag, int64 &lb, int64 &ub);
int numbitsInType(Type::Tag typetag);
}  // namespace

void AbcdReasons::print(::std::ostream &os) const
{
    assert(facts.size() < 100);
    StlSet<SsaTmpOpnd *>::const_iterator
        iter = facts.begin(),
        end = facts.end();
    if (iter != end) {
        os << "{";
        {
            SsaTmpOpnd *opnd = *iter;
            opnd->print(os);
        }
        for (++iter ; iter != end; ++iter) {
            SsaTmpOpnd *opnd = *iter;
            os << " ";
            opnd->print(os);
        }
        os << "}";
    } else {
        os << "NoReasons";
    }
}

PiBound PiBound::add(const PiBound &other, bool is_signed) const {
    assert(typetag == other.typetag);
    if (isUndefined()) return *this;
    if (other.isUndefined()) return other;
    if ((!isUnknown()) && (!other.isUnknown())) {
        if (var_part.isEmpty() || other.var_part.isEmpty()) {
            // if one or no variables, can add
            ConstInst::ConstValue v1;
            ConstInst::ConstValue v2;
            ConstInst::ConstValue res;
            v1.i8 = const_part;
            v2.i8 = other.const_part;
            if (ConstantFolder::foldConstant(typetag, Op_Add, v1, v2, res, is_signed)) {
                int64 c = (numbitsInType(typetag)==64) ? res.i8 : ((int64)res.i4);
                if (!var_part.isEmpty()) {
                    return PiBound(typetag, var_multiple, var_part, c);
                } else if (!other.var_part.isEmpty()) {
                    return PiBound(typetag, other.var_multiple, other.var_part, c);
                } else
                    return PiBound(typetag, c);
            }
        } else if ((!var_part.isEmpty()) && (!other.var_part.isEmpty())
                   && (var_part == other.var_part)) {
            // if variables are the same, can add
            ConstInst::ConstValue v1;
            ConstInst::ConstValue v2;
            ConstInst::ConstValue res;
            v1.i8 = const_part;
            v2.i8 = other.const_part;
            if (ConstantFolder::foldConstant(typetag, Op_Add, v1, v2, res, is_signed)) {
                int64 c = (numbitsInType(typetag)==64) ? res.i8 : ((int64)res.i4);
                v1.i8 = var_multiple;
                v2.i8 = other.var_multiple;
                if (ConstantFolder::foldConstant(typetag, Op_Add, v1, v2, res, is_signed)){
                    int64 mult = ((numbitsInType(typetag)==64)
                                  ? res.i8
                                  : ((int64)res.i4));
                    return PiBound(typetag, mult, var_part, c);
                }
            }
        }
    }
    return getUnknown();
}

PiBound PiBound::neg () const {
    if (isUnknown() || isUndefined()) return *this;
    ConstInst::ConstValue v1;
    ConstInst::ConstValue res;
    v1.i8 = const_part;
    if (ConstantFolder::foldConstant(typetag, Op_Neg, v1, res)) {
        int64 c = (numbitsInType(typetag)==64) ? res.i8 : ((int64)res.i4);
        v1.i8 = var_multiple;
        if (ConstantFolder::foldConstant(typetag, Op_Neg, v1, res)) {
            int64 mult = ((numbitsInType(typetag)==64)
                          ? res.i8
                          : ((int64)res.i4));
            return PiBound(typetag, mult, var_part, c);
        }
    }
    return getUnknown();
}

PiBound PiBound::abs () const {
    if (isUnknown() || isUndefined()) return *this;
    if (var_multiple != 0) return getUnknown();
    if (const_part < 0) {
        int64 newconst_part = -const_part;
        if (newconst_part == const_part) { //overflow
            return getUnknown();
        }
        return PiBound(typetag, 0, 0, newconst_part);
    }
    return *this;
}

PiBound PiBound::mul(const PiBound &other, bool is_signed) const {
    assert(typetag == other.typetag);
    if (isUndefined()) return other;
    else if (other.isUndefined()) return *this;
    else if ((!isUnknown()) && (!other.isUnknown())) {
        if (var_part.isEmpty() && other.var_part.isEmpty()) {
            // if no variables, can multiply
            ConstInst::ConstValue v1;
            ConstInst::ConstValue v2;
            ConstInst::ConstValue res;
            v1.i8 = const_part;
            v2.i8 = other.const_part;
            if (ConstantFolder::foldConstant(typetag, Op_Mul, v1, v2, res, is_signed)) {
                int64 c = (numbitsInType(typetag)==64) ? res.i8 : ((int64)res.i4);
                return PiBound(typetag, c);
            }
        } else if (var_part.isEmpty() != other.var_part.isEmpty()) {
            // if only 1 variable, can multiply
            ConstInst::ConstValue v1;
            ConstInst::ConstValue v2;
            ConstInst::ConstValue res;
            v1.i8 = const_part;
            v2.i8 = other.const_part;
            if (ConstantFolder::foldConstant(typetag, Op_Mul, v1, v2, res, is_signed)) {
                int64 c = (numbitsInType(typetag)==64) ? res.i8 : ((int64)res.i4);
                v1.i8 = var_part.isEmpty() ? other.var_multiple : var_multiple;
                v2.i8 = var_part.isEmpty() ? const_part : other.const_part;
                if (ConstantFolder::foldConstant(typetag, Op_Mul, v1, v2, res, is_signed)){
                    int64 mult = ((numbitsInType(typetag)==64)
                                  ? res.i8
                                  : ((int64)res.i4));
                    return PiBound(typetag, mult, var_part, c);
                }
            }
        }
    }
    return getUnknown();
}

PiBound
PiBound::cast(Type::Tag newtype, bool isLb) const {
    if (isUnknown()) return PiBound(newtype, true);
    else if (isUndefined()) return PiBound(newtype, false);
    else if (var_multiple == 0) {
        int64 lb, ub;
        if (hasTypeBounds(newtype, lb, ub)) {
            if (isLb) {
                return PiBound(newtype, var_multiple, var_part,
                               ::std::max(const_part, lb));
            } else {
                return PiBound(newtype, var_multiple, var_part,
                               ::std::min(const_part, ub));
            }
        }
    }
    return *this;
}

bool
PiBound::isConstant() const
{
    if (isUndefined()) return true;
    if (isUnknown()) return false;
    if (var_multiple != 0) return false;
    return true;
}

bool
PiBound::isNonNegativeConstant() const
{
    if (!isConstant()) return false;
    return (const_part >= 0);
}

bool
PiBound::isNegativeConstant() const
{
    if (!isConstant()) return false;
    return (const_part < 0);
}


PiCondition
PiCondition::add(int64 constant) const
{
    return PiCondition(lb.add(PiBound(lb.getType(), constant), true),
                       ub.add(PiBound(ub.getType(), constant), true));
}

PiCondition
PiCondition::neg() const
{
    return PiCondition(ub.neg(),lb.neg());
}

PiCondition
PiCondition::mul(int64 constant) const
{
    if (constant < 0) {
        return PiCondition(ub.mul(PiBound(ub.getType(), constant), true),
                           lb.mul(PiBound(lb.getType(), constant), true));
    } else {
        return PiCondition(lb.mul(PiBound(lb.getType(), constant), true),
                           ub.mul(PiBound(ub.getType(), constant), true));
    }
}

PiCondition
PiCondition::cast(Type::Tag newtype) const
{
    return PiCondition(lb.cast(newtype, true),ub.cast(newtype, false));
}

// Print routines gathered here

::std::ostream &operator <<(::std::ostream &os, int64 val)
{
    if (((int64)(int)val) == val) {
        os << (int) val;
    } else if (val == -val) {
        os << "-9223372036854775808";
    } else {
        char buf[100];
        int idx = 0;
        bool sign = (val < (int64) 0);
        if (sign) val = -val;
        while (val > 0) {
            buf[idx++] = '0' + ((char)(val%10));
            val = val / 10;
        }
        if (sign) os << "-";
        while (idx > 0) {
            os << buf[--idx];
        }
    }
    return os;
}

void
VarBound::print(::std::ostream &os) const
{
    if (isEmpty()) {
        os << "NULL";
    } else {
        the_var->print(os);
    }
}

void
ConstBound::print(::std::ostream& os) const
{
    switch (flag) {
    case Const_PlusInfinity:
        os << "+inf"; break;
    case Const_MinusInfinity:
        os << "-inf"; break;
    case Const_IsConst:
        os << (long) the_const; break;
    case Const_IsNull:
        assert(0);
        os << "<<NULLCONST>>"; break;
    }
}


void
PiBound::print(::std::ostream &os) const
{
    if (isUnknown()) {
        os << "unknown";
    } else if (isUndefined()) {
        os << "undef";
    } else {
        if (var_multiple) {
            if (var_multiple != int64(1)) {
                os << (long) var_multiple;
                os << "*";
            }
            var_part.print(os);
            if (const_part > 0)
                os << "+";
        }
        if (const_part || !var_multiple) {
            os << (long) const_part;
        }
    }
}

void
PiCondition::print(::std::ostream &os) const
{
    os << "[";
    lb.print(os);
    os << ",";
    ub.print(os);
    os << "]";
}

PiCondition PiCondition::typeBounds(Type::Tag dstTag, Type::Tag srcTag)
{
    int64 lb, ub;
    if (hasTypeBounds(srcTag, lb, ub)) {
        return PiCondition(PiBound(dstTag, lb), PiBound(dstTag, ub));
    } else {
        return PiCondition(PiBound(dstTag, true), PiBound(dstTag, true));
    }
}

PiCondition PiCondition::convBounds(VarBound convVar)
{
    assert(convVar.the_var != 0);
    Inst *defInst = convVar.the_var->getInst();
    assert(defInst && (defInst->getOpcode() == Op_Conv));

    Opnd *srci = defInst->getSrc(0); // the source operand
    Type::Tag srcType = srci->getType()->tag;
    Type::Tag instrType = defInst->getType();
    PiCondition bounds = PiCondition::typeBounds(instrType, srcType);
    PiCondition bounds2 = bounds.cast(srcType);
    return bounds2;
}

void printPiCondition(const PiCondition *cond, ::std::ostream &os)
{
    cond->print(os);
}

PiBoundIter
VarBound::getPredecessors(bool boundingBelow, bool useReasons,
                          MemoryManager &mm) const
{
    Inst *defInst = the_var ? (the_var->getInst()) : 0;
    return PiBoundIter(defInst, boundingBelow, useReasons, mm);
}

bool
VarBound::isPhiVar() const {
    Inst *defInst = the_var->getInst();
    return (defInst && (defInst->getOpcode() == Op_Phi));
}

bool
VarBound::isMinMax(bool isMin) const {
    Inst *defInst = the_var->getInst();
    if (defInst){
        if (isMin)
            return (defInst->getOpcode() == Op_Min);
        else
            return (defInst->getOpcode() == Op_Max);
    }
    return false;
}

bool
VarBound::isConvVar() const {
    Inst *defInst = the_var ? the_var->getInst() : 0;
    return (defInst && (defInst->getOpcode() == Op_Conv));
}

VarBound
VarBound::getConvSource() const {
    assert(isConvVar());
    Inst *defInst = the_var->getInst();
    return defInst->getSrc(0);
}

// true if type1 includes all values from type2
bool
VarBound::typeIncludes(Type::Tag type1, Type::Tag type2)
{
    if (type1 == type2) return true;
    int64 lb1, lb2, ub1, ub2;
    bool hasBounds1, hasBounds2;
    hasBounds1 = hasTypeBounds(type1, lb1, ub1);
    hasBounds2 = hasTypeBounds(type2, lb2, ub2);
    if ((!hasBounds1) || (!hasBounds2)) {
        return false;
    } else {
        if ((lb1 <= lb2) && (ub2 <= ub1)) {
            return true;
        } else {
            return false;
        }
    }
}

bool
VarBound::convPassesSource() const {
    assert(isConvVar());
    Inst *instr = the_var->getInst();
    Opnd *srci = instr->getSrc(0); // the source operand
    Type::Tag srcType = srci->getType()->tag;
    Type::Tag dstType = the_var->getType()->tag;
    Type::Tag instrType = instr->getType();
    if (typeIncludes(dstType, instrType) &&
        typeIncludes(instrType, srcType)) {
        return true;
    } else {
        return false;
    }
}

bool
VarBound::isConvexFunction() const {
    if (the_var == 0) return false;
    Inst *the_inst = the_var->getInst();
    if(Op_Shr == the_inst->getOpcode()) {
        Modifier mod = the_inst->getModifier();
        assert(mod.hasSignedModifier());
        if ((mod.getSignedModifier() == SignedOp) && ConstantFolder::isConstant(the_inst->getSrc(1)) ) {
                return true;
    }
    }
    return false;
}

ConstBound
VarBound::getConvexInputBound(bool isLB, ConstBound outputBound,
                              VarBound &inputVar) const
{
    if (outputBound.isKnown()) {
        Inst *the_inst = the_var->getInst();
        switch (the_inst->getOpcode()) {
        case Op_Shr: {
            assert(the_inst->getOperation().isSignedModifierSigned());
            Opnd *inputOpnd = the_inst->getSrc(0);
            Opnd *shiftByOp = the_inst->getSrc(1);
            Inst *shiftByInst = shiftByOp->getInst();
            ConstInst *shiftByConstInst = shiftByInst->asConstInst();
            if (shiftByConstInst != 0) {
                U_32 shiftBy;
                if (shiftByConstInst->getType() == Type::Int32) {
                    shiftBy = (U_32)shiftByConstInst->getValue().i4;
                } else if (shiftByConstInst->getType() == Type::Int64) {
                    shiftBy = (U_32)shiftByConstInst->getValue().i8;
                } else {
                    return ConstBound();
                }

                if (outputBound.isKnown()) {
                    Type::Tag typetag = the_inst->getType();
                    // must be signed
                    switch (typetag) {
                    case Type::Int32:
                        {
                            I_32 bound = outputBound.getInt32();
                            if (the_inst->getShiftMaskModifier() == ShiftMask_Masked)
                                shiftBy = shiftBy & 31;
                            I_32 input = bound << shiftBy;
                            if (!isLB) {
                                if (shiftBy < 31) {
                                    input = input + ((I_32(1) << shiftBy) - 1);
                                } else {
                                    input = 0xffffffff;
                                }
                            }
                            if (bound < 0) {
                                input = input | 0x80000000;
                            } else {
                                input = input & 0x7fffffff;
                            }
                            inputVar = VarBound(inputOpnd);
                            return ConstBound(input);
                        }

                    case Type::Int64:
                        {
                            int64 bound = outputBound.getInt64();
                            if (the_inst->getShiftMaskModifier() == ShiftMask_Masked)
                                shiftBy = shiftBy & 63;
                            int64 input = bound << shiftBy;
                            if (!isLB) {
                                if (shiftBy < 64) {
                                    input = input + ((int64(1) << shiftBy) - 1);
                                } else {
                                    input = __INT64_C(0xffffffffffffffff);
                                }
                            }
                            if (bound < 0) {
                                input = input | __INT64_C(0x8000000000000000);
                            } else {
                                input = input & __INT64_C(0x7fffffffffffffff);
                            }
                            inputVar = VarBound(inputOpnd);
                            return ConstBound(input);
                        }

                    default:
                        break;
                    }
                }
            }
            return ConstBound();
        }
        default:
            break;
        }
        assert(0);
        return ConstBound();
    } else {
        return ConstBound();
    }
}

void PiBoundIter::print(::std::ostream& outs) const
{
    if (!instr) {
        outs << "[]";
    } else {
        PiBoundIter acopy(*this);
        outs << "[";
        instr->print(outs);
        outs << " : ";
        current.print(outs);
        while (++acopy) {
            outs << ", ";
            acopy.current.print(outs);
        }
        outs << "]";
    }
}

bool PiBound::extractConstant(Type::Tag newtype, ConstInst *cinst0, PiBound &res)
{
    assert(cinst0);
    ConstInst::ConstValue cv = cinst0->getValue();
    switch (cinst0->getType()) {
    case Type::Int8: case Type::Int16: case Type::Int32:
        {
            I_32 c = cv.i4;
            res = PiBound(newtype, int64(c));
            return true;
        }
    case Type::Int64:
        {
            int64 c = cv.i8;
            res = PiBound(newtype, int64(c));
            return true;
        }
    case Type::UInt8: case Type::UInt16: case Type::UInt32:
        {
            U_32 c = (U_32) cv.i4;
            res = PiBound(newtype, int64(uint64(c)));
            return true;
        }
    case Type::UInt64:
        {
            uint64 c = cv.i8;
            if (c < uint64(__INT64_C(0x8000000000000000))) {
                res = PiBound(newtype, int64(c));
                return true;
            }
            break;
        }
    default:
        break;
    }
    return false;
}

Opnd *PiBoundIter::getConstantOpnd(Opnd *opnd)
{
    if (ConstantFolder::isConstant(opnd)) {
        return opnd;
    } else {
        return 0;
    }
}

PiBound PiBoundIter::getBound(AbcdReasons *why0) {
    if (why0) {
        assert(why);
        why0->addReasons(*why);
    }
    return current;
}

void PiBoundIter::init(bool useReasons)
{
    assert(!why);
    why = useReasons ? new AbcdReasons(mm) : 0;
}

void PiBoundIter::setCurrent()
{
    if (isEmpty()) return;
    if (!instr) return;
    switch (instr->getOpcode()) {
    case Op_Phi:
        {
            if (idx >= instr->getNumSrcOperands()) {
                instr = 0;
                if (why) why->clear();
            } else {
                Opnd *srci = instr->getSrc(idx);
                VarBound vb(srci);
                current = PiBound(instr->getType(), vb);
                if (why) why->clear();
            }
        }
        break;
    case Op_TauPi:
        {
            // first, try to insert condition
            TauPiInst *pInst = instr->asTauPiInst();
            const PiCondition *cond = pInst->getCond();
            Opnd *tauDepOpnd = pInst->getSrc(1);
            SsaTmpOpnd *tauDep = tauDepOpnd->asSsaTmpOpnd();
            assert(tauDep);
            if (isLb) {
                current = cond->getLb();
            } else {
                current = cond->getUb();
            }
            assert(!current.isUnknown());
            if (current.isUndefined() && (idx >= 1)) {
                instr = 0;
            } else if ((!current.isUndefined()) && (idx == 0)) {
                // use the Pi condition
                if (why) why->addReason(tauDep);
            } else if (idx <= 1) {
                // effect of assignment
                Opnd *srci = instr->getSrc(0);
                VarBound vb(srci);
                current = PiBound(instr->getType(), vb);
            } else {
                instr = 0;
            }
        }
        break;
    case Op_Add:
        if (idx > 0) instr = 0;
        else {
            Opnd *op0 = instr->getSrc(0);
            Opnd *op1 = instr->getSrc(1);
            Opnd *constOpnd0 = getConstantOpnd(op0);
            Opnd *constOpnd1 = getConstantOpnd(op1);
            if ((constOpnd0 || constOpnd1)
                && (instr->getType() == Type::Int32)) {
                int64 c;
                // I assume we've done folding first
                assert(!(constOpnd0 && constOpnd1));

                if (constOpnd1) {
                    // swap the operands;
                    constOpnd0 = constOpnd1;
                    op1 = op0;
                }
                // now constOpnd0 is the constant opnd
                // op1 is the non-constant opnd

                Inst *inst0 = constOpnd0->getInst();
                assert(inst0);
                ConstInst *cinst0 = inst0->asConstInst();
                assert(cinst0);
                ConstInst::ConstValue cv = cinst0->getValue();
                c = cv.i4;

                VarBound vb(op1);
                current = PiBound(instr->getType(), 1, vb, c);
            } else
                instr = 0;
        }
        break;
    case Op_Sub:
        if (idx > 0) instr = 0;
        else {
            Opnd *constOpnd = getConstantOpnd(instr->getSrc(1));
            if (constOpnd
                && (instr->getType() == Type::Int32)) {

#ifndef NDEBUG
                Opnd *op0 = instr->getSrc(0);
#endif
                Opnd *op1 = constOpnd;
                // now op1 should be constant
                // I assume we've done folding first
                assert(!getConstantOpnd(op0));

                Inst *inst1 = op1->getInst();
                assert(inst1);
                ConstInst *cinst1 = inst1->asConstInst();
                assert(cinst1);
                ConstInst::ConstValue cv = cinst1->getValue();
                I_32 c = cv.i4;
                I_32 negc = -c;
                if (neg_overflowed(negc,c)) { // overflowed
                    instr = 0;
                } else {
                    VarBound vb(op1);
                    current = PiBound(instr->getType(), 1, vb, negc);
                }
            } else
                instr = 0;
        }
        break;
    case Op_Min:
        if ((idx == 0) || (idx == 1)) {
            Opnd *srci = instr->getSrc(idx); // the source operand
            VarBound vb(srci);
            current = PiBound(instr->getType(), vb);
        } else {
            instr = 0;
        }
        break;
    case Op_Max:
        if ((idx == 0) || (idx == 1)) {
            Opnd *srci = instr->getSrc(idx); // the source operand
            VarBound vb(srci);
            current = PiBound(instr->getType(), vb);
        } else {
            instr = 0;
        }
        break;
    case Op_Abs:
        if (idx == 0) {
            ConstBound cb((int64)0);
            current = PiBound(instr->getType(), (int64) 0);
        } else {
            instr = 0;
        }
        break;
    case Op_TauCheckBounds:
        assert(0);
        switch (idx) {
        case 0:
            {
                Opnd *srci = instr->getSrc(1); // the index in;
                VarBound vb(srci);
                current = PiBound(srci->getType()->tag, vb); // result bounded by input index
            }
            break;
        case 1:
            if (isLb) {
                current = PiBound(instr->getType(), int64(0)); // and 0
            } else {
                // the array length
                Opnd *srci = instr->getSrc(0); // the array length
                VarBound vb(srci);
                current = PiBound(srci->getType()->tag, int64(1), vb, int64(-1)); // len - 1
            }
            break;
        default:
            instr = 0;
            break;
        };
        break;
    case Op_Copy:
        if (idx == 0) {
            Opnd *srci = instr->getSrc(0); // the source operand
            VarBound vb(srci);
            current = PiBound(instr->getType(), vb);
        } else {
            instr = 0;
        }
        break;
    case Op_LdVar:
        if (idx == 0) {
            assert(instr->isVarAccess());
            VarAccessInst *varInst = (VarAccessInst *)instr;
            Opnd *srci = varInst->getSrc(0);
            VarBound vb(srci);
            current = PiBound(instr->getType(), vb);
        } else {
            instr = 0;
        }
        break;
    case Op_StVar:
        if (idx == 0) {
            assert(instr->isVarAccess());
            Opnd *srci = instr->getSrc(0);
            VarBound vb(srci);
            current = PiBound(instr->getType(), vb);
        } else {
            instr = 0;
        }
        break;
    case Op_TauArrayLen:
        if (idx == 0) {
            if (isLb) {
                // 0
                current = PiBound(instr->getType(), int64(0));
            } else {
                current = PiBound(instr->getType(), int64(0x7fffffff)); // bounded by max int
            }
        } else {
            instr = 0;
        }
        break;
    case Op_LdConstant:
        if (idx == 0) {
            ConstInst *cinst0 = instr->asConstInst();
            if (!PiBound::extractConstant(instr->getType(), cinst0, current)) {
                instr = 0;
            }
        } else {
            instr = 0;
        }
        break;
        // could maybe do something with these:
    case Op_TauRem:
        if (idx == 0) {
            Opnd *denom = instr->getSrc(1);
            if (getConstantOpnd(denom)) {
                Inst *consti1 = denom->getInst();
                ConstInst *cinst1 = consti1->asConstInst();
                assert(cinst1);
                PiBound tmp(instr->getType(), true);
                if (PiBound::extractConstant(instr->getType(), cinst1, current)) {
                    if (isLb) {
                        current = tmp.abs().neg();
                    } else {
                        current = tmp.abs();
                    }
                }
            } else {
                instr = 0;
            }
        } else {
            instr = 0;
        }
        break;
    case Op_Conv:
        if (idx == 0) {
            Opnd *srci = instr->getSrc(0); // the source operand
            Type::Tag srcType = srci->getType()->tag;
            Type::Tag dstType = instr->getDst()->getType()->tag;
            Type::Tag instrType = instr->getType();
            PiCondition bounds = PiCondition::typeBounds(dstType, srcType);
            PiCondition bounds2 = bounds.cast(instrType);
            PiCondition bounds3 = bounds.cast(dstType);
            if (isLb) {
                current = bounds3.getLb();
            } else {
                current = bounds3.getUb();
            }
        } else {
            instr = 0;
        }
        break;
    case Op_Cmp:
        if (idx == 0) {
            current = PiBound(instr->getType(), int64(isLb ? 0 : 1));
        } else {
            instr = 0;
        }
        break;
    case Op_Cmp3:
        if (idx == 0) {
            current = PiBound(instr->getType(), int64(isLb ? -1 : 1));
        } else {
            instr = 0;
        }
        break;

    case Op_TauCheckNull:
    case Op_TauCheckZero:
    case Op_TauCheckCast:
    case Op_TauHasType:
    case Op_TauHasExactType:
    case Op_TauIsNonNull:
        assert(0);
        break;

    case Op_Or:
    case Op_Not:
    case Op_Neg:
    case Op_DefArg:
    case Op_NewArray:
    case Op_TauCast:
    case Op_TauAsType:

    default:
        instr = 0 ;
        break;
    }
}

namespace {
bool hasTypeBounds(Type::Tag srcTag, int64 &lb, int64 &ub)
{
    switch (srcTag) {
    case Type::Int8:   lb = -int64(0x80); ub = 0x7f; return true;
    case Type::Int16:  lb = -int64(0x8000); ub = 0x7fff; return true;
    case Type::Int32:  lb = -int64(0x80000000); ub = 0x7fffffff; return true;
    case Type::Int64:
        lb = __INT64_C(0x8000000000000000);
        ub = __INT64_C(0x7fffffffffffffff); return true;
    case Type::UInt8:  lb = 0; ub = 0x100; return true;
    case Type::UInt16: lb = 0; ub = 0x10000; return true;
    case Type::UInt32: lb = 0; ub = __INT64_C(0x100000000); return true;
    default:
        return false;
    }
}

int numbitsInType(Type::Tag typetag) {
    if ((typetag == Type::Int64) || (typetag == Type::UInt64) ||
        ((typetag == Type::IntPtr) && (sizeof(POINTER_SIZE_INT) == 8))) {
        return 64;
    } else {
        return 32;
    }
}
}  // namespace

}  // namespace Jitrino
