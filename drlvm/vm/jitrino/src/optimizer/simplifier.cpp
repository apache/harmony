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


#include "Opcode.h"
#include "Opnd.h"
#include "Type.h"
#include "Inst.h"
#include "BitSet.h"
#include "Log.h"
#include "optimizer.h"
#include "simplifier.h"
#include "constantfolder.h"
#include "deadcodeeliminator.h"
#include "optarithmetic.h"
#include "reassociate.h"
#include "irmanager.h"
#include "CompilationContext.h"

#include "FlowGraph.h"

#include <float.h>
#include <math.h>
#include "PlatformDependant.h"

namespace Jitrino {
/*
 * Implemented algorithms are similar to the ones described in 
 * [T.Granlund and P.L.Montgomery. Division by Invariant Integers using 
 * Multiplication. PLDI, 1994], [S.Muchnick. Advanced Compiler Design and 
 * Implementation. Morgan Kaufmann, San Francisco, CA, 1997].
 */


DEFINE_SESSION_ACTION(SimplificationPass, simplify, "Perform simplification pass");
void
SimplificationPass::_run(IRManager& irm) {
    SimplifierWithInstFactory simplifier(irm, false);
    simplifier.simplifyControlFlowGraph();
}

DEFINE_SESSION_ACTION(LateSimplificationPass, latesimplify, "Simplification + Constant Mul/Div Optimization");

void
LateSimplificationPass::_run(IRManager& irm) {
    SimplifierWithInstFactory simplifier(irm, true);
    simplifier.simplifyControlFlowGraph();
}

//-----------------------------------------------------------------------------
// Utilities for querying properties of opnds
//-----------------------------------------------------------------------------
static inline bool isNeg(Inst* inst) {
    return inst->getOpcode() == Op_Neg;
}
static inline bool isNeg(Opnd* opnd) {
    return isNeg(opnd->getInst());
}
static inline bool isAdd(Inst* inst) {
    return inst->getOpcode() == Op_Add;
}
static inline bool isAdd(Opnd* opnd) {
    return isAdd(opnd->getInst());
}
static inline bool isSub(Inst* inst) {
    return inst->getOpcode() == Op_Sub;
}
static inline bool isSub(Opnd* opnd) {
    return isSub(opnd->getInst());
}

static inline bool isAddWithConstant(Inst* inst) {
    return (inst->getOpcode() == Op_Add && ConstantFolder::hasConstant(inst));
}

static inline bool isSubWithConstant(Inst* inst) {
    return (inst->getOpcode() == Op_Sub && ConstantFolder::hasConstant(inst));
}

static inline bool isMultiplyByConstant(Inst* inst) {
    return (inst->getOpcode() == Op_Mul && ConstantFolder::hasConstant(inst));
}

static inline bool isTauDivByConstant(Inst* inst) {
    return (inst->getOpcode() == Op_TauDiv &&
            ConstantFolder::isConstant(inst->getSrc(1)));
}

static inline bool isCopy(Inst* inst) {
    return (inst->getOpcode() == Op_Copy);
}

static Opnd*
getArraySize(Opnd* arrayBase) {
    if (arrayBase->getType()->isArrayType() == false)
        return NULL;
    Inst* inst = arrayBase->getInst();
    if (inst->getOpcode() == Op_NewArray) {
        return inst->getSrc(0);
    }
    return NULL;
}

bool
Simplifier::isNonNullObject(Opnd* opnd) {
    Inst* inst = opnd->getInst();
    switch (inst->getOpcode()) {
    case Op_NewObj:    case Op_NewArray:    case Op_NewMultiArray:
    case Op_LdRef:    case Op_Catch:
        return true;
    default:
    return false;
    }
}

bool
Simplifier::isNonNullParameter(Opnd* opnd) {
    Inst* inst = opnd->getInst();
    if (inst->getOpcode() == Op_DefArg) {
        return (inst->getDefArgModifier() == NonNullThisArg);
    }
    return false;
}

bool
Simplifier::isNullObject(Opnd* opnd) {
    return (opnd->getType()->isObject() &&
            opnd->getInst()->getOpcode() == Op_LdConstant);
}

bool
Simplifier::isExactType(Opnd* opnd) {
    Type* type = opnd->getType();
    if (type->isObject() && ((ObjectType*)type)->isFinalClass())
        return true;
    Inst* inst = opnd->getInst();
    switch (inst->getOpcode()) {
    case Op_NewObj:    case Op_NewArray:    case Op_NewMultiArray:
    case Op_LdRef:
        return true;
    case Op_DefArg:
        return (inst->getDefArgModifier() == SpecializedToExactType);
    default:
    return false;
    }
}
//-----------------------------------------------------------------------------
// Simplifier class
//-----------------------------------------------------------------------------
Simplifier::Simplifier(IRManager& irm, bool latePass, 
               Reassociate *reassociate0)
    : irManager(irm), flowGraph(irm.getFlowGraph()),
      isLate(latePass), theReassociate(reassociate0)
{
}

Inst*
Simplifier::optimizeInst(Inst* inst) {
    // first copy propagate all sources of the instruction
    DeadCodeEliminator::copyPropagate(inst);
    // then simplify
    return dispatch(inst);
}

// fold unary operation
// returns NULL if no fold possible
// should always succeed on Int32/Int64 values
Opnd*
Simplifier::fold(Opcode op, Type* type, ConstInst *srcInst, bool is_signed) {
    ConstInst::ConstValue res;
    if (ConstantFolder::foldConstant(type->tag, op, srcInst->getValue(), res)) {
        if (type->tag == Type::Int32) {
            return genLdConstant(res.i4)->getDst();
        } else if (type->tag == Type::Int64) {
            return genLdConstant(res.i8)->getDst();
        } else if (type->tag == Type::Single) {
            return genLdConstant((float)res.s)->getDst();
        } else if (type->tag == Type::Double) {
            return genLdConstant((double)res.d)->getDst();
        } else {
            assert(0);
        }
    }
    return NULL;
}

// fold binary operation
// should always succeed on add/sub/mul of Int32/Int64 values
// returns NULL if no fold possible
Opnd*
Simplifier::fold(Opcode op, Type* type, ConstInst *srcInst1, ConstInst *srcInst2, 
                 bool is_signed) {
    ConstInst::ConstValue res;
    if (ConstantFolder::foldConstant(type->tag, 
                                     op, 
                                     srcInst1->getValue(), 
                                     srcInst2->getValue(), 
                                     res, 
                                     is_signed)) {
        if (type->tag == Type::Int32) {
            return genLdConstant(res.i4)->getDst();
        } else if (type->tag == Type::Int64) {
            return genLdConstant(res.i8)->getDst();
        } else if (type->tag == Type::Single) {
            return genLdConstant((float)res.s)->getDst();
        } else if (type->tag == Type::Double) {
            return genLdConstant((double)res.d)->getDst();
        } else {
            assert(0);
        }
    }
    return NULL;
}

// fold comparison operation
// returns NULL if no fold possible
// apparently, destination will always be Int32 (or acceptable to represent as)
Opnd*
Simplifier::foldComparison(ComparisonModifier mod, 
                           Type::Tag cmpType,
                           ConstInst* srcInst1, 
                           ConstInst* srcInst2) {
    ConstInst::ConstValue res;
    if (ConstantFolder::foldCmp(cmpType, mod, 
                                srcInst1->getValue(), srcInst2->getValue(), 
                                res)) {
        return genLdConstant(res.i4)->getDst();
    }
    return NULL;
}

// c1 + (c2 + s) -> (c1+c2) + s
// addInst must be add with constant operand,
// and same type as given.
Opnd*
Simplifier::foldConstByAddingToAddWithConstant(Type* type,
                                               ConstInst* constInst,
                                               Inst* addInst) {
    // Note: types of the 2 ops must be the same
    assert(type->tag == addInst->getType());

    ConstInst* otherConstInst;
    Opnd* nonConstSrc;
    if (ConstantFolder::isConstant(addInst->getSrc(0))) {
        // C + s
        otherConstInst = addInst->getSrc(0)->getInst()->asConstInst();
        nonConstSrc = addInst->getSrc(1);
    } else {
        // s + C
        otherConstInst = addInst->getSrc(1)->getInst()->asConstInst();
        nonConstSrc = addInst->getSrc(0);
    }
    // c1+c2
    Opnd* constSrc = fold(Op_Add, type, constInst, otherConstInst, true);
    // (c1+c2) + s
    return genAdd(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), constSrc, nonConstSrc)->getDst();
}

// c1 + (c2 - s) -> (c1+c2) - s
// c1 + (s - c2) -> (c1-c2) + s
// subInst must be subtract with constant operand,
// and type same as given.
Opnd*
Simplifier::foldConstByAddingToSubWithConstant(Type* type,
                                               ConstInst* constInst,
                                               Inst* subInst) {
    // Note: types of the 2 ops must be the same
    assert(type->tag == subInst->getType());

    ConstInst* otherConstInst;
    Opnd* nonConstSrc;
    if (ConstantFolder::isConstant(subInst->getSrc(0))) {
        // C2 - s
        otherConstInst = subInst->getSrc(0)->getInst()->asConstInst();
        nonConstSrc = subInst->getSrc(1);
        // (C1+C2)
        Opnd* foldedConst = fold(Op_Add, type, constInst, otherConstInst, true);
        // (C1+C2) - s
        return genSub(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), foldedConst, nonConstSrc)->getDst();
    } else {
        // s - C2
        nonConstSrc = subInst->getSrc(0);
        otherConstInst = subInst->getSrc(1)->getInst()->asConstInst();
        // (C1 - C2)
        Opnd* foldedConst = fold(Op_Sub, type, constInst, otherConstInst, true);
        // s + (C1 - C2)
        return genAdd(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), nonConstSrc, foldedConst)->getDst();
    }
}

// c1 * (c2 + s) -> c1*c2 + (c1 * s)
// addInst must be an add with constant operand,
// and type same as is given.
// this is only signed multiply.
Opnd*
Simplifier::foldConstMultiplyByAddWithConstant(Type* type,
                                               ConstInst* constInst,
                                               Inst* addInst) {
    // Note: types of the 2 ops must be the same
    assert(type->tag == addInst->getType());
    ConstInst* otherConstInst;

    Opnd* nonConstSrc;
    if (ConstantFolder::isConstant(addInst->getSrc(0))) {
        // c2 + s
        otherConstInst = addInst->getSrc(0)->getInst()->asConstInst();
        nonConstSrc = addInst->getSrc(1);
    } else {
        // s + c2
        otherConstInst = addInst->getSrc(1)->getInst()->asConstInst();
        nonConstSrc = addInst->getSrc(0);
    }
    // c1 * c2
    Opnd* constSrc = fold(Op_Mul, type, constInst, otherConstInst, true);
    // c1 * s
    Opnd* finalMulByConst = genMul(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), constInst->getDst(),
                                   nonConstSrc)->getDst();
    return genAdd(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), constSrc, finalMulByConst)->getDst();
}

// c1 * (c2 - s) -> (c1*c2) - (c1*s)
// c1 * (s - c2) -> (c1*s) + -(c1*c2)
// subInst must be a subtract with constant src,
// and same type as is given.
// should probably only be used with int type.
Opnd*
Simplifier::foldConstMultiplyBySubWithConstant(Type* type,
                                               ConstInst* constInst, // c1
                                               Inst* subInst) {
    // Note: types of the 2 ops must be the same
    assert(type->tag == subInst->getType());

    if (ConstantFolder::isConstant(subInst->getSrc(0))) {
        // c2 - s
        ConstInst* otherConstInst = 
            subInst->getSrc(0)->getInst()->asConstInst(); // c2
        Opnd *nonConstSrc = subInst->getSrc(1);  // s
        // (c1 * c2)
        Opnd* foldedConst = fold(Op_Mul, type, constInst,
                                 otherConstInst, true);
        // -c1
        Opnd* negC1 = fold(Op_Neg, type, constInst, true);
        // (-c1) * s
        Opnd* finalMulByConst = genMul(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), negC1,
                                       nonConstSrc)->getDst();
        // (c1*c2) + (-c1)*s
        return genAdd(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                      foldedConst, finalMulByConst)->getDst();
    } else {
        // s - c2
        Opnd *nonConstSrc = subInst->getSrc(0); // s
        ConstInst* otherConstInst = 
            subInst->getSrc(1)->getInst()->asConstInst(); // c2
        Opnd* negFoldedConst; // -(c1 * c2)
        { // avoid creating a useless instr for c1*c2
            ConstInst::ConstValue res;  // c1*c2
#ifndef NDEBUG
            bool foldres = 
#endif
                ConstantFolder::foldConstant(type->tag, Op_Mul, 
                                        constInst->getValue(), // c1
                                        otherConstInst->getValue(), // c2
                                        res,
                                        true);
            assert(foldres);
            ConstInst::ConstValue negRes; // -(c1*c2)
#ifndef NDEBUG
            foldres = 
#endif
                ConstantFolder::foldConstant(type->tag, Op_Neg, res, negRes);
            assert(foldres);
            if (type->tag == Type::Int32) {
                negFoldedConst = genLdConstant(negRes.i4)->getDst();
            } else if (type->tag == Type::Int64) {
                negFoldedConst = genLdConstant(negRes.i8)->getDst();
            } else {
                negFoldedConst = 0;
                assert(0);
            }
        }
        // c1*s
        Opnd* finalMulByConst = genMul(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                       constInst->getDst(),
                                       nonConstSrc)->getDst();
        // (-(c1*c2)) + c1*s
        return genAdd(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                      negFoldedConst,
                      finalMulByConst)->getDst();
    }
}

// c1*(c2*s) -> s * (c1*c2)
// mulInst must be a multiply by constant, with getType()==type
// should probably only be used with int type.
Opnd*
Simplifier::foldConstMultiplyByMulWithConstant(Type* type,
                                               ConstInst* constInst,
                                               Inst* mulInst) {
    // Note: types of the 2 ops must be the same
    assert(type->tag == mulInst->getType());

    ConstInst::ConstValue v1 = constInst->getValue();
    ConstInst* otherConstInst;
    Opnd* nonConstSrc;
    if (ConstantFolder::isConstant(mulInst->getSrc(0))) {
        // C2 * s
        otherConstInst = mulInst->getSrc(0)->getInst()->asConstInst();
        nonConstSrc = mulInst->getSrc(1);
    } else {
        // s * C2
        otherConstInst = mulInst->getSrc(1)->getInst()->asConstInst();
        nonConstSrc = mulInst->getSrc(0);
    }
    Opnd* constSrc = fold(Op_Mul, type, constInst, otherConstInst, true);
    return genMul(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), constSrc, nonConstSrc)->getDst();
}

// mulInst must be a multiply by constant, with getType()==type
Opnd*
Simplifier::foldNegOfMultiplyByConstant(Type* type,
                                        Inst* mulInst) {
    // Note: types of the 2 ops must be the same
    assert(type->tag == mulInst->getType());

    ConstInst* theConstInst;
    Opnd* nonConstSrc;
    if (ConstantFolder::isConstant(mulInst->getSrc(0))) {
        // C * s
        theConstInst = mulInst->getSrc(0)->getInst()->asConstInst();
        nonConstSrc = mulInst->getSrc(1);
    } else {
        // s * C
        theConstInst = mulInst->getSrc(1)->getInst()->asConstInst();
        nonConstSrc = mulInst->getSrc(0);
    }
    // (-C)
    Opnd* negatedConst = fold(Op_Neg, type, theConstInst, true);
    // (-C) * s
    return genMul(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), negatedConst, nonConstSrc)->getDst();
}

// checks for and performs a possible s1 + (-s3) -> s1 - s3
// makes no assumptions about types.
// works with or without overflow.
Opnd*
Simplifier::simplifyAddWithNeg(Modifier modifier,
                               Type* type,
                               Opnd* src1,
                               Opnd* src2) {
    //
    // s1 + (s2 = neg s3) -> s1 - s3
    //
    Inst *inst2 = src2->getInst();
    if (isNeg(inst2) && (inst2->getType() == type->tag)) {
        return genSub(type, modifier, src1, inst2->getSrc(0))->getDst();
    }
    return NULL;
}

// checks for and performs a possible s1 - (-s3) -> s1 + s3
// makes no assumptions about types
Opnd*
Simplifier::simplifySubWithNeg(Modifier modifier,
                               Type* type,
                               Opnd* src1,
                               Opnd* src2) {
    //
    // s1 - (s2 = neg s3) -> s1 + s3
    //
    Inst *inst2 = src2->getInst();
    if (isNeg(src2) && (inst2->getType() == type->tag)) {
        return genAdd(type, modifier, src1, inst2->getSrc(0))->getDst();
    }
    //
    // (s1 = neg s3) - s2 -> neg (s3 + s2)
    //
    Inst *inst1 = src1->getInst();
    if (isNeg(src1) && (inst1->getType() == type->tag)) {
        Opnd* addOpnd =
            genAdd(type, modifier, inst1->getSrc(0), src2)->getDst();
        return genNeg(type, addOpnd)->getDst();
    }
    return NULL;
}

// checks for and performs possible re-associations of sub
// makes no assumptions about types
// assumes no overflow, no exceptions, no strict
Opnd*
Simplifier::simplifySubViaReassociation(Type* type, Opnd* src1, Opnd* src2) {
    if (ConstantFolder::isConstant(src2)) {
        ConstInst* constInst2 = src2->getInst()->asConstInst();  // c2
        ConstInst::ConstValue value = constInst2->getValue();    // c2 value
        Inst *srcInst1 = src1->getInst();
        if (srcInst1->getType() != type->tag)
            return NULL;   // don't bother if types differ
        if (isSubWithConstant(srcInst1)) {
            // (C1 - s1) - C2 -> (C1 - C2) - s1
            // (s1 - C1) - C2 -> s1 + (-(C1 + C2))
            ConstInst* constInst1;
            Opnd* nonConstSrc;
            if (ConstantFolder::isConstant(srcInst1->getSrc(0))) {
                // (C1 - s1) - C2
                constInst1 = srcInst1->getSrc(0)->getInst()->asConstInst();
                nonConstSrc = srcInst1->getSrc(1);
                // C1-C2
                Opnd* constSrc = fold(Op_Sub, type, constInst1, constInst2, true);
                // (C1-C2) - s1
                return genSub(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                              constSrc, nonConstSrc)->getDst();
            } else {
                // (s1 - C1) - C2
                nonConstSrc = srcInst1->getSrc(0);
                constInst1 = srcInst1->getSrc(1)->getInst()->asConstInst();
                // C1 + C2
                Opnd* constSrc = fold(Op_Add, type, constInst2, constInst1, true);
                // -(C1+C2)
                Opnd* constSrcNeg = fold(Op_Neg, type, 
                                         constSrc->getInst()->asConstInst(), true);
                // s1 + (-(C1+C2))
                return genAdd(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                              nonConstSrc, constSrcNeg)->getDst();
            }
        }
        if (isAddWithConstant(srcInst1)) {
            // (s + C1) - C2 -> s + (C1 - C2)
            // (C1 + s) - C2 -> s + (C1 - C2)

            ConstInst* otherConstInst;  // c1
            Opnd* nonConstSrc;          // s
            if (ConstantFolder::isConstant(srcInst1->getSrc(0))) {
                // c1 + s
                otherConstInst = srcInst1->getSrc(0)->getInst()->asConstInst();
                nonConstSrc = srcInst1->getSrc(1);
            } else {
                // s + C
                otherConstInst = srcInst1->getSrc(1)->getInst()->asConstInst();
                nonConstSrc = srcInst1->getSrc(0);
            }
            // c1-c2
            Opnd* constSrc = fold(Op_Sub, type, otherConstInst, constInst2, true);
            // s + (c1+c2)
            return genAdd(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), nonConstSrc, constSrc)->getDst();
        }
    }
    if (ConstantFolder::isConstant(src1)) {
        // disabled to address a compiler warning of unreferenced local variable
        Inst *srcInst1 = src1->getInst();
        ConstInst* constInst1 = srcInst1->asConstInst();        // c1
        ConstInst::ConstValue value = constInst1->getValue();   // c1 value
        Inst* nonConstInst = src2->getInst();
        if (nonConstInst->getType() != type->tag) {
            return NULL;    // types differ, don't re-associate
        }
        if (isSubWithConstant(nonConstInst)) {
            // C1 - (C2 - s) -> (C1-C2) + s
            // C1 - (s - C2) -> (C1+C2) - s

            if (ConstantFolder::isConstant(nonConstInst->getSrc(0))) {
                // (C2 - s)
                ConstInst *constInst2 = nonConstInst->getSrc(0)->getInst()->asConstInst();
                Opnd *nonConstSrc = nonConstInst->getSrc(1);
                // C1-C2
                Opnd* constSrc = fold(Op_Sub, type, constInst1, constInst2, true);
                // s+(C1-C2)
                return genAdd(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                    constSrc, nonConstSrc)->getDst();
            } else {
                // (s - C2)
                Opnd *nonConstSrc = nonConstInst->getSrc(0);
                ConstInst *constInst2 = nonConstInst->getSrc(1)->getInst()->asConstInst();
                // C1+C2
                Opnd* constSrc = fold(Op_Add, type, constInst1, constInst2, true);
                // (C1+C2)-s
                return genSub(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                    constSrc, nonConstSrc)->getDst();
            }
        }
        if (isAddWithConstant(nonConstInst)) {
            // C1 - (s + C2) -> (C1-C2) - s
            // C1 - (C2 + s) -> (C1-C2) - s

            ConstInst *constInst2;
            Opnd *nonConstSrc;

            if (ConstantFolder::isConstant(nonConstInst->getSrc(0))) {
                // (C2 + s)
                constInst2 = nonConstInst->getSrc(0)->getInst()->asConstInst();
                nonConstSrc = nonConstInst->getSrc(1);
            } else {
                // (s + C2)
                constInst2 = nonConstInst->getSrc(1)->getInst()->asConstInst();
                nonConstSrc = nonConstInst->getSrc(0);
            }
            // C1-C2
            Opnd* constSrc = fold(Op_Sub, type, constInst1, constInst2, true);
            // (C1-C2) - s
            return genSub(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                          constSrc, nonConstSrc)->getDst();
        }
    }
    return NULL;
}

// checks for and performs possible re-associations of add
// makes no assumptions about types
Opnd*
Simplifier::simplifyAddViaReassociation(Type* type, Opnd* src1, Opnd* src2) {
    if (ConstantFolder::isConstant(src1)) {
        ConstInst* constInst = src1->getInst()->asConstInst();
        Inst* srcInst2 = src2->getInst();
        if (srcInst2->getType() != type->tag)
            return NULL; // don't bother if types differ
        if (isAddWithConstant(srcInst2)) {
            // c1 + (c2 + s3) -> (c1+c2) + s3
            // c1 + (s2 + c3) -> (c1+c3) + s2
            return foldConstByAddingToAddWithConstant(type, constInst, srcInst2);
        }
        if (isSubWithConstant(srcInst2)) {
            // c1 + (c2 - s3) -> (c1+c2) - s3
            // c1 + (s2 - c3) -> (c1-c3) + s2
            return foldConstByAddingToSubWithConstant(type, constInst, srcInst2);
        }
    }
    return NULL;
}

// pulls neg out of mul
// makes no assumptions about types
Opnd*
Simplifier::simplifyMulWithNeg(Type* type,
                               Opnd* src1,
                               Opnd* src2) {
    //
    // s1 * (s2 = neg s3) -> neg (s1 * s3)
    //
    if (isNeg(src2) && (src2->getInst()->getType() == type->tag)) {
        Opnd *src3 = src2->getInst()->getSrc(0);
        Opnd *mulRes = genMul(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), src1, src3)->getDst();
        return genNeg(type, mulRes)->getDst();
    }
    return NULL;
}

Opnd*
Simplifier::simplifyTauDivOfMul(Modifier mod,
                                Type* type,
                                Opnd* src1,
                                Opnd* src2,
                                Opnd* tauCheckedOpnds) {
    //
    // if (c1%c2==0), then
    //   (c1 * s1) / c2 -> (c1/c2) * s1
    //
    return NULL;

}

//-----------------------------------------------------------------------------
// Arithmetic & logical simplifications
//-----------------------------------------------------------------------------

//
// returns an Opnd if the add can be simplified,
// null if the operation cannot be simplified
//
Opnd*
Simplifier::simplifyAdd(Type* type,
                        Modifier modifier,
                        Opnd* src1,
                        Opnd* src2) {
    //
    // Algebraic identity
    //
    // 0 + s2 -> s2
    //
    if (ConstantFolder::isConstantZero(src1))
        return src2;
    //
    // s1 + 0 -> s1
    //
    if (ConstantFolder::isConstantZero(src2))
        return src1;

    //
    // we do not simplify add with overflow any further
    //
    if (modifier.getOverflowModifier() != Overflow_None)
        return NULL;
    //
    // Fold constants
    //
    // (s1 = ldconst c1) + (s2 = ldconst c2) -> ldconst fold(Op_Add, c1, c2)
    //
    if (ConstantFolder::isConstant(src1) && ConstantFolder::isConstant(src2))
        return fold(Op_Add, type,
                    src1->getInst()->asConstInst(),
                    src2->getInst()->asConstInst(),
                    true);
    //
    // don't simplify floating point further
    //
    switch (type->tag) {
    case  Type::Int32:  case Type::Int64:
    case Type::UInt32:  case Type::UInt64: break;
    default:
        return NULL;
    }
    //
    // Inversion
    //
    Opnd* opnd;
    opnd = simplifyAddWithNeg(modifier, type, src1, src2);
    if (opnd != NULL)
        return opnd;
    //
    // Commute and try again
    //
    opnd = simplifyAddWithNeg(modifier, type, src2, src1);
    if (opnd != NULL)
        return opnd;
    //
    // Reassociate
    //
    opnd = simplifyAddViaReassociation(type, src1, src2);
    if (opnd != NULL)
        return opnd;
    //
    // Commute and try again
    //
    opnd = simplifyAddViaReassociation(type, src2, src1);
    if (opnd != NULL)
        return opnd;
    //
    if (theReassociate) {
    opnd = simplifyAddViaReassociation2(type, src1, src2);
    if (opnd != NULL)
        return opnd;
    }
    //
    return NULL;
}

Opnd*
Simplifier::simplifySub(Type* type, Modifier modifier,
                        Opnd* src1, Opnd* src2) {
    //
    // Algebraic identity
    //
    // s1 - 0 -> s1
    //
    if (ConstantFolder::isConstantZero(src2))
        return src1;

    //
    // Fold constants
    //
    // (s1 = ldconst c1) - (s2 = ldconst c2) -> ldconst fold(Op_Sub, c1, c2)
    //
    if (ConstantFolder::isConstant(src1) && ConstantFolder::isConstant(src2))
        return fold(Op_Sub, type,
                    src1->getInst()->asConstInst(),
                    src2->getInst()->asConstInst(),
                    true);

    //
    // don't simplify floating point further
    //
    switch (type->tag) {
    case Type::Int32: case Type::Int64:
    case Type::UInt32: case Type::UInt64:
        break;
    default:
        return NULL;
    }
    //
    // 0 - s2 -> neg s2
    //
    if (ConstantFolder::isConstantZero(src1))
        return genNeg(type, src2)->getDst();

    // x - x -> 0
    if (src1 == src2) {
        ConstInst::ConstValue zeroval;
        switch(type->tag) {
        case Type::Int32: return genLdConstant((I_32)0)->getDst();
        case Type::Int64: return genLdConstant((int64)0)->getDst();
        default: assert(0); return NULL;
        }
    }
    //
    // we do not simplify sub with overflow any further
    // we could do folding and check for overflow; for now we punt
    //
    if (modifier.getOverflowModifier() != Overflow_None)
        return NULL;

    //
    // Inversion
    //
    Opnd* opnd;
    opnd = simplifySubWithNeg(modifier, type, src1, src2);
    if (opnd != NULL)
        return opnd;

    if (theReassociate) {
    opnd = simplifySubViaReassociation2(type, src1, src2);
    if (opnd != NULL)
        return opnd;
    }

    opnd = simplifySubViaReassociation(type, src1, src2);
    if (opnd != NULL)
    return opnd;


    return NULL;
}

Opnd*
Simplifier::simplifyNeg(Type* type, Opnd* src) {

    //
    // -C
    //
    if (ConstantFolder::isConstant(src)) {
        ConstInst* constInst = src->getInst()->asConstInst();
        return fold(Op_Neg, type, constInst, true);
    }

    //
    // double negation
    //
    if (isNeg(src))
        return src->getInst()->getSrc(0);

    //
    // don't simplify floating point further
    //
    switch (type->tag) {
    case Type::Int32: case Type::Int64:
    case Type::UInt32: case Type::UInt64:
        break;
    default:
        return NULL;
    }

    if (theReassociate) {
        Opnd *opnd = simplifyNegViaReassociation2(type, src);
        if (opnd != NULL)
            return opnd;
    }

    return NULL;
}

Opnd*
Simplifier::simplifyAnd(Type* theType, Opnd* src1, Opnd* src2) {
    //
    // Algebraic identity
    //
    // x & x -> x
    //
    if (src1 == src2)
        return src1;
    //
    // 0 & s2 -> 0
    //
    if (ConstantFolder::isConstantZero(src1))
        return src1;
    //
    // s2 & 0 -> 0
    //
    if (ConstantFolder::isConstantZero(src2))
        return src2;
    //
    // 0xf..f & s2 -> s2
    //
    if (ConstantFolder::isConstantAllOnes(src1))
        return src2;
    //
    // s1 & 0xf..f -> s1
    //
    if (ConstantFolder::isConstantAllOnes(src2))
      return src1;
    
    //
    // Fold constants
    //
    if (ConstantFolder::isConstant(src1) && ConstantFolder::isConstant(src2))
        return fold(Op_And, theType,
                    src1->getInst()->asConstInst(),
                    src2->getInst()->asConstInst(),
                    true);

    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();
    // check for And of 0xff with zxt/sxt:i1, etc.
    if (optimizerFlags.do_sxt && 
    (ConstantFolder::isConstant(src1) || ConstantFolder::isConstant(src2))) {
        Inst *inst2 = src2->getInst();
        Type::Tag typeTag2 = inst2->getType();
        Inst *inst1 = src1->getInst();
        Type::Tag typeTag1 = inst1->getType();
        int64 val64;
        I_32 val32;
        if (ConstantFolder::isConstant(src1->getInst(), val64) &&
            (inst2->getOpcode() == Op_Conv) &&
            (((typeTag2 == Type::Int8) && (((uint64)val64) <= 0xff)) ||
             ((typeTag2 == Type::Int16) && (((uint64)val64) <= 0xffff)) ||
             ((typeTag2 == Type::Int32) && (((uint64)val64) <= 0xffffffff)))) {
        Opnd *src2opnd = inst2->getSrc(0);
        if (src2opnd->getType() == src2->getType()) {
        return genAnd(theType, src1, src2opnd)->getDst();
        }
    } else if (ConstantFolder::isConstant(src2->getInst(), val64) &&
           (inst1->getOpcode() == Op_Conv) &&
           (((typeTag1 == Type::Int8) && (((uint64)val64) <= 0xff)) ||
            ((typeTag1 == Type::Int16) && (((uint64)val64) <= 0xffff)) ||
            ((typeTag1 == Type::Int32) && (((uint64)val64) <= 0xffffffff)))) {
        Opnd *src1opnd = inst1->getSrc(0);
        if (src1opnd->getType() == src1->getType()) {
        return genAnd(theType, src1opnd, src2)->getDst();
        }
    } else if (ConstantFolder::isConstant(src1->getInst(), val32) &&
           (inst2->getOpcode() == Op_Conv) &&
           (((typeTag2 == Type::Int8) && (((U_32)val32) <= 0xff)) ||
            ((typeTag2 == Type::Int16) && (((U_32)val32) <= 0xffff)) ||
            ((typeTag2 == Type::Int32) && (((U_32)val32) <= 0xffffffff)))) {
        Opnd *src2opnd = inst2->getSrc(0);
        if (src2opnd->getType() == src2->getType()) {
        return genAnd(theType, src1, src2opnd)->getDst();
        }
    } else if (ConstantFolder::isConstant(src2->getInst(), val32) &&
           (inst1->getOpcode() == Op_Conv) &&
           (((typeTag1 == Type::Int8) && (((U_32)val32) <= 0xff)) ||
            ((typeTag1 == Type::Int16) && (((U_32)val32) <= 0xffff)) ||
            ((typeTag1 == Type::Int32) && (((U_32)val32) <= 0xffffffff)))) {
        Opnd *src1opnd = inst1->getSrc(0);
        if (src1opnd->getType() == src1->getType()) {
        return genAnd(theType, src1opnd, src2)->getDst();
        }
    }

    // check for And of 0xff with ld.u1, etc.
        if (ConstantFolder::isConstant(src1->getInst(), val64) &&
            (inst2->getOpcode() == Op_TauLdInd) &&
            (((typeTag2 == Type::UInt8) && (((uint64)val64) == 0xff)) ||
             ((typeTag2 == Type::UInt16) && (((uint64)val64) == 0xffff)) ||
             ((typeTag2 == Type::UInt32) && (((uint64)val64) == 0xffffffff)))) {
        if (theType == src2->getType()) {
        return src2;
        }
    } else if (ConstantFolder::isConstant(src2->getInst(), val64) &&
           (inst1->getOpcode() == Op_TauLdInd) &&
           (((typeTag1 == Type::UInt8) && (((uint64)val64) == 0xff)) ||
            ((typeTag1 == Type::UInt16) && (((uint64)val64) == 0xffff)) ||
            ((typeTag1 == Type::UInt32) && (((uint64)val64) == 0xffffffff)))) {
        if (theType == src1->getType()) {
        return src1;
        }
    } else if (ConstantFolder::isConstant(src1->getInst(), val32) &&
           (inst2->getOpcode() == Op_TauLdInd) &&
           (((typeTag2 == Type::UInt8) && (((U_32)val32) == 0xff)) ||
            ((typeTag2 == Type::UInt16) && (((U_32)val32) == 0xffff)) ||
            ((typeTag2 == Type::UInt32) && (((U_32)val32) == 0xffffffff)))) {
        if (theType == src2->getType()) {
        return src2;
        }
    } else if (ConstantFolder::isConstant(src2->getInst(), val32) &&
           (inst1->getOpcode() == Op_TauLdInd) &&
           (((typeTag1 == Type::UInt8) && (((U_32)val32) == 0xff)) ||
            ((typeTag1 == Type::UInt16) && (((U_32)val32) == 0xffff)) ||
            ((typeTag1 == Type::UInt32) && (((U_32)val32) == 0xffffffff)))) {
        if (theType == src1->getType()) {
        return src1;
        }
    }
    }
    return NULL;
}

Opnd*
Simplifier::simplifyOr(Type* theType, Opnd* src1, Opnd* src2) {
    //
    // Algebraic identity
    //
    // x || x -> x
    //
    if (src1 == src2)
        return src1;
    //
    // 0 | s2 -> s2
    //
    if (ConstantFolder::isConstantZero(src1))
        return src2;
    //
    // s1 | 0 -> s1
    //
    if (ConstantFolder::isConstantZero(src2))
        return src1;
    //
    // 0xf..f | s2 -> 0xf..f
    //
    if (ConstantFolder::isConstantAllOnes(src1))
        return src1;
    //
    // s1 | 0xf..f -> 0xf..f
    //
    if (ConstantFolder::isConstantAllOnes(src2))
        return src2;
    
    //
    // Fold constants
    //
    if (ConstantFolder::isConstant(src1) && ConstantFolder::isConstant(src2))
        return fold(Op_Or, theType,
                    src1->getInst()->asConstInst(),
                    src2->getInst()->asConstInst(),
                    true);
    return NULL;
}

Opnd*
Simplifier::simplifyXor(Type* theType, Opnd* src1, Opnd* src2) {
    //
    // Algebraic identity
    //
    // 0 ^ s2 -> s2
    //
    if (ConstantFolder::isConstantZero(src1))
        return src2;
    //
    // s1 ^ 0 -> s1
    //
    if (ConstantFolder::isConstantZero(src2))
        return src1;
    //
    // 0xf..f ^ s2 -> not(s2)
    //
    if (ConstantFolder::isConstantAllOnes(src1))
        return simplifyNot(theType, src2);
    //
    // s1 ^ 0xf..f -> not(s1)
    //
    if (ConstantFolder::isConstantAllOnes(src2))
        return simplifyNot(theType, src1);
    
    //
    // Fold constants
    //
    if (ConstantFolder::isConstant(src1) && ConstantFolder::isConstant(src2))
        return fold(Op_Xor, theType,
                    src1->getInst()->asConstInst(),
                    src2->getInst()->asConstInst(),
                    true);
    return NULL;
}
    
Opnd*
Simplifier::simplifyNot(Type* theType, Opnd* src1) {
    //
    // Fold constants
    //
    if (ConstantFolder::isConstant(src1))
        return fold(Op_Not, theType,
                    src1->getInst()->asConstInst(),
                    true);
    return NULL;
}

Opnd*
Simplifier::simplifyMulViaReassociation(Type* type, Opnd* src1, Opnd* src2) {
    if (ConstantFolder::isConstant(src1)) {
        ConstInst* constInst = src1->getInst()->asConstInst();
        Inst* srcInst2 = src2->getInst();
        if (isAddWithConstant(srcInst2)) {
            //
            // c1 * (c2 + s3) -> (c1*c2) + c1*s3
            // c1 * (s2 + c3) -> (c1*c3) + c1*s2
            //
            return foldConstMultiplyByAddWithConstant(type, constInst, srcInst2);
        }
        if (isMultiplyByConstant(srcInst2)) {
            //
            // c1 * (c2 * s3) -> (c1*c2) * s3
            // c1 * (s2 * c3) -> (c1*c3) * s2
            //
            return foldConstMultiplyByMulWithConstant(type, constInst, srcInst2);
        }
        if (isSubWithConstant(srcInst2)) {
            //
            // c1 * (c2 - s3) -> (c1*c2) - c1*s3
            // c1 * (s2 - c3) -> (c1*(-c3)) + c1*s2
            //
            return foldConstMultiplyBySubWithConstant(type, constInst, srcInst2);
        }
    }
    return NULL;
}


Opnd*
Simplifier::simplifyMul(Type* type,
                        Modifier modifier,
                        Opnd* src1,
                        Opnd* src2) {
    

    bool src1isConstant = ConstantFolder::isConstant(src1);
    bool src2isConstant = ConstantFolder::isConstant(src2);
    //
    // Algebraic identity
    //
    // 1 * s2 -> s2
    //
    if (src1isConstant && ConstantFolder::isConstantOne(src1))
        return src2;
    //
    // s1 * 1 -> s1
    //
    if (src2isConstant && ConstantFolder::isConstantOne(src2))
        return src1;

    if (!type->isFP()) {

        //
        // Fold to 0
        //
        // 0 * s2 -> 0
        //
        if (src1isConstant && ConstantFolder::isConstantZero(src1))
            return src1;
        //
        // s1 * 0 -> 0
        //
        if (src2isConstant && ConstantFolder::isConstantZero(src2))
            return src2;
    }        

    //
    // we do not simplify mul with overflow any further
    // we could do folding and check for overflow; for now we punt
    //
    if (modifier.getOverflowModifier() != Overflow_None)
        return NULL;

    //
    // Fold constants
    //
    // (s1 = ldconst c1) * (s2 = ldconst c2) -> ldconst fold(Op_Mul, c1, c2)
    //
    if (src1isConstant && src2isConstant)
        return fold(Op_Mul, type,
                    src1->getInst()->asConstInst(),
                    src2->getInst()->asConstInst(),
                    true);
    
    //
    // limit the types we consider further
    //
    switch (type->tag) {
    case  Type::Int32:  case Type::Int64:       break;
    case  Type::UInt32:  case Type::UInt64:       break;
    default:
        return NULL;
    }

    // reduce multiply-by-constant
    if (src1isConstant || src2isConstant) {
        switch (type->tag) {
            case Type::Int32:
            case Type::UInt32:
                if (src1isConstant) {
                    I_32 multiplier;
                    bool t = ConstantFolder::isConstant(src1->getInst(), 
                                                        multiplier);
                    if( !t) assert(0);
                    Opnd *product = planMul32(multiplier, src2);
                    return product;
                } else { // src2isConstant
                    I_32 multiplier;
                    bool t = ConstantFolder::isConstant(src2->getInst(), 
                                                        multiplier);
                    if( !t) assert(0);
                    Opnd *product = planMul32(multiplier, src1);
                    return product;
                }        

            case Type::Int64:
            case Type::UInt64:
                if (src1isConstant) {
                    int64 multiplier;
                    bool t = ConstantFolder::isConstant(src1->getInst(),
                                                        multiplier);
                    if( !t) assert(0);
                    Opnd *product = planMul64(multiplier, src2);
                    return product;
                } else { // src2isConstant
                    int64 multiplier;
                    bool t = ConstantFolder::isConstant(src2->getInst(), 
                                                        multiplier);
                    if( !t) assert(0);
                    Opnd *product = planMul64(multiplier, src1);
                    return product;
                }     
                   
            default:
                break;
        }
    }


    //
    // Inversion
    //

    // a * (- b) -> - (a * b)
    Opnd* opnd;
    opnd = simplifyMulWithNeg(type, src1, src2);
    if (opnd != NULL)
        return opnd;

    //
    // Reassociate
    //
    opnd = simplifyMulViaReassociation(type, src1, src2);
    if (opnd != NULL)
        return opnd;
    //
    // Commute and try again
    //
    opnd = simplifyMulViaReassociation(type, src2, src1);
    if (opnd != NULL)
        return opnd;
    //
    if (theReassociate) {
    opnd = simplifyMulViaReassociation2(type, src1, src2);
    if (opnd != NULL)
        return opnd;
    }
    //
    return NULL;
}

Opnd*
Simplifier::simplifyMulHi(Type* type,
                          Modifier modifier,
                          Opnd* src1,
                          Opnd* src2) {

    //
    // Fold constants
    //
    // (s1 = ldconst c1) * (s2 = ldconst c2) -> ldconst fold(Op_Mul, c1, c2)
    //
    bool src1isConstant = ConstantFolder::isConstant(src1);
    bool src2isConstant = ConstantFolder::isConstant(src2);
    if (src1isConstant && src2isConstant)
        return fold(Op_MulHi, type,
                    src1->getInst()->asConstInst(),
                    src2->getInst()->asConstInst(),
                    Type::isSignedInteger(type->tag));

    //
    // Algebraic identity
    //
    // 1 * s2 -> s2
    //
    if ((src1isConstant && ConstantFolder::isConstantOne(src1)) ||
        (src2isConstant && ConstantFolder::isConstantOne(src2))) {
        if (modifier.isSigned()) {
            return NULL;
        } else {
            switch (type->tag) {
            case Type::Int32:
            case Type::UInt32:
                return genLdConstant((I_32) 0)->getDst();
            case Type::Int64:
            case Type::UInt64:
                return genLdConstant((int64) 0)->getDst();
            default:
                break;
            }
            return NULL;
        }
    }

    assert(!type->isFP());

    //
    // Fold to 0
    //
    // 0 * s2 -> 0
    //
    if (src1isConstant && ConstantFolder::isConstantZero(src1))
        return src1;
    //
    // s1 * 0 -> 0
    //
    if (src2isConstant && ConstantFolder::isConstantZero(src2))
        return src2;

    // reduce multiply-by-constant
    if (src1isConstant || src2isConstant) {
        bool isSigned = false;
        switch (type->tag) {
            case Type::Int32: isSigned = true;
            case Type::UInt32:
                if (src1isConstant) {
                    TypeManager &tm = irManager.getTypeManager();
                    I_32 multiplier;
                    bool t = ConstantFolder::isConstant(src1->getInst(), 
                                                        multiplier);
                    if( !t) assert(0);
                    Type *dstType64 = tm.getInt64Type();
                    Opnd *src2_64 = genConv(dstType64, 
                                            (isSigned 
                                             ? Type::Int64 : Type::UInt64), 
                                            Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                            src2)->getDst();
                    Opnd *product = planMul32(multiplier, src2_64);
                    Opnd *thirtytwo = genLdConstant((I_32)(32))->getDst();
                    Opnd *shifted = genShr(dstType64, 
                                           Modifier(UnsignedOp)|Modifier(ShiftMask_None),
                                           product, thirtytwo)->getDst();
                    Opnd *res = genConv(src1->getType(), Type::Int32, 
                                        Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                        shifted)->getDst();
                    return res;
                } else { // src2isConstant
                    I_32 multiplier;
                    bool t = ConstantFolder::isConstant(src2->getInst(), 
                                                        multiplier);
                    if( !t) assert(0);
                    TypeManager &tm = irManager.getTypeManager();
                    Type *dstType64 = tm.getInt64Type();
                    Opnd *src1_64 = genConv(dstType64, 
                                            (isSigned ? Type::Int64 : Type::UInt64), 
                                            Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                            src1)->getDst();
                    Opnd *product = planMul32(multiplier, src1_64);
                    Opnd *thirtytwo = genLdConstant((I_32)(32))->getDst();
                    Opnd *shifted = genShr(dstType64, 
                                           Modifier(UnsignedOp)|Modifier(ShiftMask_None),
                                           product, thirtytwo)->getDst();
                    Opnd *res = genConv(src1->getType(), Type::Int32, 
                                            Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                        shifted)->getDst();
                    return res;
                }        

            default:
                break;
        }
    }
    
    return NULL;
}

Opnd*
Simplifier::simplifyMin(Type* dstType,
                        Opnd* src1,
                        Opnd* src2) {
    if (ConstantFolder::isConstant(src1)) {
        if (ConstantFolder::isConstant(src2)) {
            return fold(Op_Min, dstType, 
                        src1->getInst()->asConstInst(),
                        src2->getInst()->asConstInst(),
                        true);
        }
    }
    if (src1 == src2) {
        return src1;
    }
    return NULL;
}

Opnd*
Simplifier::simplifyMax(Type* dstType,
                        Opnd* src1,
                        Opnd* src2) {
    if (ConstantFolder::isConstant(src1)) {
        if (ConstantFolder::isConstant(src2)) {
            return fold(Op_Max, dstType, 
                        src1->getInst()->asConstInst(),
                        src2->getInst()->asConstInst(),
                        true);
        }
    }
    if (src1 == src2) {
        return src1;
    }
    return NULL;
}

Opnd*
Simplifier::simplifyAbs(Type* dstType,
                        Opnd* src1) {
    if (ConstantFolder::isConstant(src1)) {
        return fold(Op_Abs, dstType, 
                    src1->getInst()->asConstInst(),
                    true);
    }
    return NULL;
}

Opnd*
Simplifier::simplifyTauDiv(Type* dstType,
                           Modifier mod,
                           Opnd* src1,
                           Opnd* src2,
                           Opnd* tauOpndsChecked) {
    //
    // constant-fold anything; check for 0 occurs inside fold()
    // if (c2 != 0)
    //   (s1 = ldconst c1) / (s2 = ldconst c2) -> ldconst fold(Op_Mul, c1, c2)
    //
    if (ConstantFolder::isConstant(src1) && 
        ConstantFolder::isConstant(src2))
        return fold(Op_TauDiv, dstType,
                    src1->getInst()->asConstInst(),
                    src2->getInst()->asConstInst(),
                    mod.isSigned());
    

    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();
    //
    // further ops only for integers
    //
    switch (dstType->tag) {
    case  Type::Int32:  case Type::Int64:       break;
    default:
        return NULL;
    }
    // Algebraic identity

    //
    // Div by constant might simplify
    //
    ConstInst::ConstValue value2;
    if (ConstantFolder::isConstant(src2->getInst(), value2)) {
        //
        // s1 / 1 -> s1
        //
        if ((dstType->tag == Type::Int32) 
            ? (value2.i4 == 1) 
            : (value2.i8 == 1))
            return src1;
        
        // shouldn't happen
        if ((dstType->tag == Type::Int32) 
            ? (value2.i4 == 0) 
            : (value2.i8 == 0))
            return src1;
        
        //
        // if (c1%c2 == 0), then
        //   (s1 * c1) / c2 -> s1 * (c1/c2)
        //
        Opnd* opnd = simplifyTauDivOfMul(mod, dstType, src1, src2, tauOpndsChecked);
        if (opnd != NULL)
            return opnd;
        
        // convert other cases to MulHi and shift if lower_divconst set
        if (optimizerFlags.lower_divconst && mod.isSigned()) {
            if (dstType->tag == Type::Int32) {
                I_32 denom = value2.i4;
                // note that 0 and 1 were handled above
                if (denom == -1) {
                    // convert to neg
                    Opnd *res = genNeg(dstType, src1)->getDst();
                    return res;
                } else if (isPowerOf2<I_32>(denom)) {
                    // convert to shift and such
                    I_32 absdenom = (denom < 0) ? -denom : denom;
                    int k = whichPowerOf2<I_32,32>(absdenom);
                    Opnd *kminus1 = genLdConstant((I_32)(k - 1))->getDst();
                    // make k-1 copies of the sign bit
                    Opnd *shiftTheSign = genShr(dstType, 
                                                Modifier(SignedOp)|Modifier(ShiftMask_None),
                                                src1, kminus1)->getDst();
                    // we 32-k zeros in on left to put copies of sign on right
                    Opnd *t32minusk = genLdConstant((I_32)(32-k))->getDst();
                    // if (n<0), this is 2^k-1, else 0
                    Opnd *kminus1ones = genShr(dstType, 
                                               Modifier(UnsignedOp)|Modifier(ShiftMask_None),
                                               shiftTheSign, t32minusk)->getDst();
                    Opnd *added = genAdd(dstType, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                         src1, kminus1ones)->getDst();
                    Opnd *kOpnd = genLdConstant((I_32)k)->getDst();
                    Opnd *res = genShr(dstType, Modifier(SignedOp)|Modifier(ShiftMask_None),
                                       added, kOpnd)->getDst();
                    if (denom != absdenom) { // ((denom < 0) && (k < 31))
                        res = genNeg(dstType, res)->getDst();
                    }
                    return res;
                } else {
                    // convert to MulHi and such
                    I_32 magicNum, shiftBy;
                    getMagic<I_32, U_32, 32>(denom, &magicNum, &shiftBy);
                
                    Opnd *mulRes;
                    if (optimizerFlags.use_mulhi) {
                        Opnd *magicOpnd = genLdConstant(magicNum)->getDst();
                        mulRes = genMulHi(dstType, SignedOp, magicOpnd,
                                          src1)->getDst();
                    } else {
                        Opnd *magicOpnd = genLdConstant((int64)magicNum)->getDst();
                        TypeManager &tm = irManager.getTypeManager();
                        Type *dstType64 = tm.getInt64Type();
                        Opnd *src64 = genConv(dstType64, Type::Int64, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                              src1)->getDst();
                        Opnd *mulRes64 = genMul(dstType64, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), magicOpnd,
                                                src64)->getDst();
                        Opnd *constant32 = genLdConstant((I_32)32)->getDst();
                        Opnd *mulRes64h = genShr(dstType64, 
                                                 Modifier(SignedOp)|Modifier(ShiftMask_None),
                                                 mulRes64,
                                                 constant32)->getDst();
                        mulRes = genConv(dstType, Type::Int32, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                         mulRes64h)->getDst();
                    }
                    // need to adjust for overflow in magicNum
                    // this is indicated by sign which differs from
                    // the denom.
                    if ((denom > 0) && (magicNum < 0)) {
                        mulRes = genAdd(dstType, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), 
                                        mulRes, src1)->getDst();
                    } else if ((denom < 0) && (magicNum > 0)) {
                        mulRes = genSub(dstType, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), 
                                        mulRes, src1)->getDst();
                    }
                    Opnd *shiftByOpnd = genLdConstant(shiftBy)->getDst();
                    mulRes = genShr(dstType, Modifier(SignedOp)|Modifier(ShiftMask_None),
                                    mulRes, shiftByOpnd)->getDst();
                    Opnd *thirtyOne = genLdConstant((I_32)31)->getDst();
                    Opnd *oneIfNegative = genShr(dstType,
                                                 Modifier(UnsignedOp)|Modifier(ShiftMask_None),
                                                 ((denom < 0) 
                                                  ? mulRes
                                                  : src1),
                                                 thirtyOne)->getDst();
                    Opnd *res = genAdd(dstType, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), mulRes, oneIfNegative)->getDst();
                    return res;
                }
            } else if (dstType->tag == Type::Int64) {
                int64 denom = value2.i8;
                // note that 0 and 1 were handled above
                if (denom == -1) {
                    // convert to neg
                    Opnd *res = genNeg(dstType, src1)->getDst();
                    return res;
                } else if (isPowerOf2<int64>(denom)) {
                    // convert to shift and such
                    int64 absdenom = (denom < 0) ? -denom : denom;
                    int k = whichPowerOf2<int64,64>(absdenom);
                    Opnd *kminus1 = genLdConstant((int64)(k - 1))->getDst();
                    // make k-1 copies of the sign bit
                    Opnd *shiftTheSign = genShr(dstType, 
                                                Modifier(SignedOp)|Modifier(ShiftMask_None),
                                                src1, kminus1)->getDst();
                    // we 64-k zeros in on left to put copies of sign on right
                    Opnd *t64minusk = genLdConstant((int64)(64-k))->getDst();
                    // if (n<0), this is 2^k-1, else 0
                    Opnd *kminus1ones = genShr(dstType, 
                                               Modifier(UnsignedOp)|Modifier(ShiftMask_None),
                                               shiftTheSign, t64minusk)->getDst();
                    Opnd *added = genAdd(dstType, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                         src1, kminus1ones)->getDst();
                    Opnd *kOpnd = genLdConstant((int64)k)->getDst();
                    Opnd *res = genShr(dstType, Modifier(SignedOp)|Modifier(ShiftMask_None),
                                       added, kOpnd)->getDst();
                    if (denom != absdenom) { // ((denom < 0) && (k < 63))
                        res = genNeg(dstType, res)->getDst();
                    }
                    return res;
                }
            }
        }
    }

    return NULL;
}

Opnd*
Simplifier::simplifyTauRem(Type* dstType,
                           Modifier mod,
                           Opnd* src1,
                           Opnd* src2,
                           Opnd* tauOpndsChecked) {
    //
    // Fold constants
    //
    // if (c2 != 0)
    //   (s1 = ldconst c1) / (s2 = ldconst c2) -> ldconst fold(Op_Rem, c1, c2)
    //
    // check for 0 occurs inside fold()
    if (ConstantFolder::isConstant(src1) &&
        ConstantFolder::isConstant(src2))
        return fold(Op_TauRem, dstType,
                    src1->getInst()->asConstInst(),
                    src2->getInst()->asConstInst(),
                    mod.isSigned());

    //
    // don't simplify floating point further
    //
    switch (dstType->tag) {
    case  Type::Int32:  case Type::Int64:       break;
    case  Type::UInt32:  case Type::UInt64:       break;
    default:
        return NULL;
    }

    // Algebraic identity

    //
    // Rem by constant might simplify
    //
    if (ConstantFolder::isConstant(src2)) {
        if (ConstantFolder::isConstantOne(src2)) {
            ConstInst::ConstValue zeroval;
            switch(dstType->tag) {
            case Type::Int32: return genLdConstant((I_32)0)->getDst();
            case Type::Int64: return genLdConstant((int64)0)->getDst();
            default: assert(0); return NULL;
            }
        }

        if (ConstantFolder::isConstantZero(src2))
            return NULL;

        // convert rem by powers of 2 into bitwise "and" operations
        // also handle 1 and -1 here.
        Inst *src2inst = src2->getInst();
        ConstInst::ConstValue cv;
        if (ConstantFolder::isConstant(src2inst, cv.i4)) {
            I_32 denom = cv.i4;
            if ((denom == -1) || (denom == 1)) {
                //
                // s1 % +-1 -> 0 
                //
                return genLdConstant((I_32)0)->getDst();
            }
        } else if (ConstantFolder::isConstant(src2inst, cv.i8)) {
            int64 denom = cv.i8;
            if ((denom == -1) || (denom == 1)) {
                //
                // s1 % +-1 -> 0 
                //
                return genLdConstant((int64)0)->getDst();
            }
        }

        // convert other cases to MulHi and shift, and Mul
        // just use div
        Opnd *tryDiv = simplifyTauDiv(dstType, mod, src1, src2, tauOpndsChecked);
        if (tryDiv) {
            // div worked, use it
            Opnd *divProduct = genMul(dstType, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                      tryDiv, src2)->getDst();
            Opnd *remOpnd = genSub(dstType, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                   src1, divProduct)->getDst();
            return remOpnd;
        }
    }

    return NULL;
}
//-----------------------------------------------------------------------------
// Conversion, shift and comparison simplifications
//-----------------------------------------------------------------------------
Opnd*
Simplifier::simplifyConv(Type* dstType,
                         Type::Tag toType,
                         Modifier ovfmod,
                         Opnd* src) {
    // get rid of redundant conversion
    Inst *opndInst = src->getInst();
    if (toType == opndInst->getType() && (dstType == src->getType())) {
        return src;
    }
    ConstInst *cinst = opndInst->asConstInst();
    if (cinst) {
        ConstInst::ConstValue srcval = cinst->getValue();
        ConstInst::ConstValue res;
        Type::Tag fromType = opndInst->getType();
        if (ConstantFolder::foldConv(fromType, toType, ovfmod, srcval, res)) {
            if (dstType->tag == toType) 
                return genLdConstant(dstType, res)->getDst();
        }
    }
    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();
    if (optimizerFlags.do_sxt && (opndInst->getOpcode() == Op_And)) {
        assert(opndInst->getNumSrcOperands() == 2);
        Opnd *src0 = opndInst->getSrc(0);
        Opnd *src1 = opndInst->getSrc(1);
    int64 val64;
    // and with 0xff makes Conv(u8) redundant, etc..
    if (ConstantFolder::isConstant(src0->getInst(), val64) &&
        (((toType == Type::UInt8)
          && (((uint64)val64) <= 0xff)) ||
             ((toType == Type::Int8)
              && (((uint64)val64) <= 0x7f)) ||
         ((toType == Type::UInt16)
          && (((uint64)val64) <= 0x7fff)) ||
         ((toType == Type::Int16)
          && (((uint64)val64) <= 0xffff)) ||
         ((toType == Type::UInt32)
          && (((uint64)val64) <= 0xffffffff)) ||
         ((toType == Type::Int32)
          && (((uint64)val64) <= 0x7fffffff)))) {
        if (dstType == src->getType())
        return src;
    } else if (ConstantFolder::isConstant(src1->getInst(), val64) &&
                   (((toType == Type::UInt8)
                     && (((uint64)val64) <= 0xff)) ||
                    ((toType == Type::Int8) 
                     && (((uint64)val64) <= 0x7f)) ||
                    ((toType == Type::UInt16)
                     && (((uint64)val64) <= 0xffff)) ||
                    ((toType == Type::Int16) 
                     && (((uint64)val64) <= 0x7fff)) ||
                    ((toType == Type::UInt32) 
                     && (((uint64)val64) <= 0xffffffff)) ||
                    ((toType == Type::Int32) 
                     && (((uint64)val64) <= 0x7fffffff)))) {
        if (dstType == src->getType())
        return src;
    }
    I_32 val32;
    if (ConstantFolder::isConstant(src0->getInst(), val32) &&
        (((toType == Type::UInt8)
          && (((U_32)val32) <= 0xff)) ||
             ((toType == Type::Int8) 
          && (((U_32)val32) <= 0x7f)) ||
         ((toType == Type::UInt16)
          && (((U_32)val32) <= 0xffff)) ||
         ((toType == Type::Int16) 
          && (((U_32)val32) <= 0x7fff)) ||
         ((toType == Type::UInt32)
          && (((U_32)val32) <= 0xffffffff)) ||
         ((toType == Type::Int32) 
          && (((U_32)val32) <= 0x7fffffff)))) {
        if (dstType == src->getType())
        return src;
    } else if (ConstantFolder::isConstant(src1->getInst(), val32) &&
        (((toType == Type::UInt8)
          && (((U_32)val32) <= 0xff)) ||
             ((toType == Type::Int8)
              && (((U_32)val32) <= 0x7f)) ||
         ((toType == Type::UInt16)
          && (((U_32)val32) <= 0xffff)) ||
         ((toType == Type::Int16) 
          && (((U_32)val32) <= 0x7fff)) ||
         ((toType == Type::UInt32)
          && (((U_32)val32) <= 0xffffffff)) ||
         ((toType == Type::Int32) 
          && (((U_32)val32) <= 0x7fffffff)))) {
        if (dstType == src->getType())
        return src;
    }
    }



    return NULL;
}

Opnd*
Simplifier::simplifyConvZE(Type* dstType,
                         Type::Tag toType,
                         Modifier ovfmod,
                         Opnd* src) 
{
    // get rid of redundant conversion
    Inst *opndInst = src->getInst();
    if (toType == opndInst->getType() && (dstType == src->getType())) {
        return src;
    }
    ConstInst *cinst = opndInst->asConstInst();
    if (cinst) {
        ConstInst::ConstValue srcval = cinst->getValue();
        ConstInst::ConstValue res;
        Type::Tag fromType = opndInst->getType();
        if (ConstantFolder::foldConv(fromType, toType, ovfmod, srcval, res)) {
            if (dstType->tag == toType) 
                return genLdConstant(dstType, res)->getDst();
        }
    }
    return NULL;
}

Opnd*
Simplifier::simplifyConvUnmanaged(Type* dstType,
                         Type::Tag toType,
                         Modifier ovfmod,
                         Opnd* src) 
{
    // get rid of redundant conversion
    Inst *opndInst = src->getInst();
    if (toType == opndInst->getType() && (dstType == src->getType())) {
        return src;
    }
    ConstInst *cinst = opndInst->asConstInst();
    if (cinst) {
        ConstInst::ConstValue srcval = cinst->getValue();
        ConstInst::ConstValue res;
        Type::Tag fromType = opndInst->getType();
        if (ConstantFolder::foldConv(fromType, toType, ovfmod, srcval, res)) {
            if (dstType->tag == toType) 
                return genLdConstant(dstType, res)->getDst();
        }
    }
    return NULL;
}


Opnd*
Simplifier::simplifyShladd(Type* dstType,
                           Opnd* value,
                           Opnd* shiftAmount,
                           Opnd* addto) {
    
    if (ConstantFolder::isConstantZero(shiftAmount))
        return genAdd(dstType, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), value, addto)->getDst();
    
    return NULL;
}

Opnd*
Simplifier::simplifyShl(Type* dstType,
                        Modifier mod,
                        Opnd* value,
                        Opnd* shiftAmount) {

    if (ConstantFolder::isConstantZero(shiftAmount))
        return value;

    return NULL;
}

Opnd*
Simplifier::simplifyShr(Type* dstType,
                        Modifier mod1,
                        Opnd* value,
                        Opnd* shiftAmount) {
    if (ConstantFolder::isConstantZero(shiftAmount))
        return value;

    return NULL;
}

Opnd*
Simplifier::simplifySelect(Type* dstType,
                           Opnd* src1,
                           Opnd* src2,
                           Opnd* src3) {
    if (ConstantFolder::isConstant(src1)) {
        if (ConstantFolder::isConstantZero(src1)) {
            return src3;
        } else {
            return src2;
        }
    }
    if (src2 == src3) {
        return src2;
    }
    if (src1->getInst()->getOpcode() == Op_Cmp) {
        if (ConstantFolder::isConstantOne(src2) &&
            ConstantFolder::isConstantZero(src3)) {
            return src1;
        }
    }
    return NULL;
}

Opnd*
Simplifier::simplifyCmp(Type* dstType,
                        Type::Tag instType, // source type
                        ComparisonModifier mod,
                        Opnd* src1,
                        Opnd* src2) {
    ConstInst::ConstValue res;
    if (ConstantFolder::isConstant(src1)) {
        if (ConstantFolder::isConstant(src2)) {
            return foldComparison(mod, 
                                  instType, 
                                  src1->getInst()->asConstInst(), 
                                  src2->getInst()->asConstInst());
        } else {
            // can fold if (src1 == 0) and comparison is Cmp_Gt_Un
            if (ConstantFolder::isConstantZero(src1) && 
                ((mod & Cmp_Mask)==Cmp_GT_Un)) {
                switch (dstType->tag) {
                case Type::Int32: return genLdConstant((I_32)0)->getDst();
                default:
                    break;
                }
            }
        }
    } else if (ConstantFolder::isConstantZero(src2) &&
               ((mod & Cmp_Mask) == Cmp_GTE_Un)) {
        switch (dstType->tag) {
        case Type::Int32: return genLdConstant((I_32)1)->getDst();
        default:
            break;
        }
    }
    // if operands are identical and not FP, then we can simplify it now
    if ((src1 == src2) && !Type::isFloatingPoint(instType)) {
        switch (mod & Cmp_Mask) {
        case Cmp_EQ: case Cmp_GTE: case Cmp_GTE_Un:
            return genLdConstant((I_32)1)->getDst();
        case Cmp_NE_Un: case Cmp_GT: case Cmp_GT_Un:
            return genLdConstant((I_32)0)->getDst();
        default:
            assert(0);
        }
    }
    
    // try to simplify to a new comparison
    { 
        ComparisonModifier newMod;
        Type::Tag newInstType;
        Opnd *newSrc1;
        Opnd *newSrc2;
        if (simplifyCmpToCmp(instType, mod, src1, src2,
                             newInstType, newMod, newSrc1, newSrc2)) {
            // can simplify it.
            Inst *res = genCmp(dstType, newInstType, newMod, 
                               newSrc1, (newSrc2 
                                         ? newSrc2
                                         : OpndManager::getNullOpnd()));
            return res->getDst();
        }
    }
    return NULL;
}

Opnd*
Simplifier::simplifyCmp3(Type* dstType,
                         Type::Tag instType,
                         ComparisonModifier mod,
                         Opnd* src1,
                         Opnd* src2) {

    ConstInst::ConstValue res;
    if (ConstantFolder::isConstant(src1)) {
        if (ConstantFolder::isConstant(src2)) {
            ConstInst *srcInst1 = src1->getInst()->asConstInst();
            ConstInst *srcInst2 = src2->getInst()->asConstInst();
            ConstInst::ConstValue res;
            if (ConstantFolder::foldCmp(instType, mod, 
                                        srcInst1->getValue(), 
                                        srcInst2->getValue(), res)) {
                if (res.i4) {
                    return genLdConstant((I_32)1)->getDst();
                } else {
                    ComparisonModifier mod2 = mod;
                    if (Type::isFloatingPoint(instType)) {
                        switch (mod) {
                        case Cmp_GT: mod2 = Cmp_GT_Un; break;
                        case Cmp_GT_Un: mod2 = Cmp_GT; break;
                        case Cmp_GTE: mod2 = Cmp_GTE_Un; break;
                        case Cmp_GTE_Un: mod2 = Cmp_GTE; break;
                        default: break;
                        }
                    }
                    if (ConstantFolder::foldCmp(instType, mod2,
                                                srcInst2->getValue(), 
                                                srcInst1->getValue(), res)) {
                        if ((U_32)res.i4) {
                            return genLdConstant((I_32)-1)->getDst();
                        } else {
                            return genLdConstant((I_32)0)->getDst();
                        }
                    }                    
                }
            }
        }
    }

    // if operands are identical and not FP, then we can simplify it now
    if ((src1 == src2) && !Type::isFloatingPoint(instType)) {
        switch (mod & Cmp_Mask) {
        case Cmp_EQ: case Cmp_GTE: case Cmp_GTE_Un:
            return genLdConstant((I_32)1)->getDst();
        case Cmp_NE_Un: case Cmp_GT: case Cmp_GT_Un:
            return genLdConstant((I_32)0)->getDst();
        default:
            assert(0);
        }
    }

    return NULL;
}
//-----------------------------------------------------------------------------
// Branch simplifications
//-----------------------------------------------------------------------------

// Returns true if the cmp3 can be simplified to a Cmp,
//    given that it can yield a result of just 1 or 2 of {-1,0,1};
//    if so, fills in the new comparison's modifier, src1, and src2.
// Interface is indirect to allow us to use this for Branches, too.
bool 
// Note that exactly 1 or 2 of the booleans can be true
Simplifier::simplifyCmp3ByResult(Opnd *cmp3Opnd,
                                 bool canbe_1, // can be -1
                                 bool canbe0,
                                 bool canbe1,
                                 Type::Tag &newInstType,
                                 ComparisonModifier &newmod,
                                 Opnd* &newSrc1,
                                 Opnd* &newSrc2)
{
    Inst *cmp3Inst = cmp3Opnd->getInst();
    assert(cmp3Inst->getOpcode() == Op_Cmp3);
    ComparisonModifier orgMod = cmp3Inst->getComparisonModifier();
    Type::Tag instType = cmp3Inst->getType();
    Opnd *src1 = cmp3Inst->getSrc(0);
    Opnd *src2 = cmp3Inst->getSrc(1);
    // simplify code below by initializing with original values:
    newInstType = instType;
    newmod = orgMod;
    newSrc1 = src1;
    newSrc2 = src2;
    if (!canbe_1) {
        if (!canbe0) { 
            // 0 0 1 - just the first test yields true
            // just convert it to a Cmp2 with same parameters
            return true;
        } else if (!canbe1) {
            // 0 1 0 - both tests yield false
            switch (orgMod) {
            case Cmp_EQ: // invert it
                { 
                    newmod = Cmp_NE_Un; return true;
                }
            case Cmp_GT: case Cmp_GT_Un: // must be equal.
                // recall that Nan is caught by one of the two tests
                {
                    newmod = Cmp_EQ; return true;
                }
            case Cmp_GTE: case Cmp_GTE_Un: 
                // recall that Nan is caught by one of the two tests
                // can't happen, make it a foldable comparison
                newmod = Cmp_NE_Un; newSrc2 = src1; return true;
            default:
                return false;
            }
        } else {
            // 0 1 1 - just the 2nd test is false
            switch (orgMod) {
            case Cmp_EQ: 
                // should fold to always true;
                newSrc2 = src1; return true;
            case Cmp_NE_Un:
                // can't happen, make it a foldable comparison
                newmod = Cmp_NE_Un; newSrc2 = src1; return true;
            case Cmp_GT:
                newmod = Cmp_GTE; return true;
            case Cmp_GT_Un:
                newmod = Cmp_GTE_Un; return true;
            case Cmp_GTE:
                newmod = orgMod; return true;
            case Cmp_GTE_Un:
                newmod = orgMod; return true;
            default:
                return false;
            }
        }
    } else { // canbe_1
        // 1 x x
        if (!canbe0 && !canbe1) {
            // 1 0 0 - just second test yields true
            // second test succeeds
            if (orgMod == Cmp_EQ) { // can happen only if one is NaN; leave it.
                return false;
            } else if (orgMod == Cmp_NE_Un) {
                // can't happen, make it easily foldable to false
                newSrc2 = src1; return true;
            } else {
                // just swap args
                newSrc1 = src2; newSrc2 = src1;
                if (Type::isFloatingPoint(instType)) {
                    // for floats, must also invert NaN handling:
                    switch (orgMod) {
                    case Cmp_GT: newmod = Cmp_GT_Un; break;
                    case Cmp_GT_Un: newmod = Cmp_GT; break;
                    case Cmp_GTE: newmod = Cmp_GTE_Un; break;
                    case Cmp_GTE_Un: newmod = Cmp_GTE_Un; break;
                    default: assert(0); break;
                    }
                }
                return true;
            }
        } else if (canbe1) {
            // 1 0 1 - one of first two tests succeeds
            switch (orgMod) {
            case Cmp_GT: case Cmp_GT_Un: newmod = Cmp_NE_Un; return true;
            case Cmp_GTE: case Cmp_GTE_Un: // shouldn't be possible
                newSrc2 = src1; newmod = Cmp_NE_Un; return true; // false
            case Cmp_NE_Un:
            case Cmp_EQ: return true; // same test.
            default:
                assert(0); return false;
            }
        } else {
            assert(canbe0);
            // 1 1 0 - first test fails, second is irrelevant
            newSrc1 = src2;
            newSrc2 = src1;
            switch (orgMod) {
            case Cmp_EQ: newmod = Cmp_NE_Un; break;
            case Cmp_NE_Un: newmod = Cmp_EQ; break;
            case Cmp_GT:
                newmod = ((Type::isFloatingPoint(instType))
                          ? Cmp_GTE_Un
                          : Cmp_GTE);
                
                break;
            case Cmp_GT_Un:
                newmod = ((Type::isFloatingPoint(instType))
                          ? Cmp_GTE
                          : Cmp_GTE_Un);
                break;
            case Cmp_GTE:
                newmod = ((Type::isFloatingPoint(instType))
                          ? Cmp_GT_Un
                          : Cmp_GT);
                break;
            case Cmp_GTE_Un:
                newmod = ((Type::isFloatingPoint(instType))
                          ? Cmp_GT
                          : Cmp_GT_Un);
                break;
            default:
                assert(0);
            }
            return true;
        }             
    }
}
                                 
void cloneComparison(bool negateIt,
                     Opnd *orgCmp,
                     Type::Tag &newInstType,
                     ComparisonModifier &newmod,
                     Opnd* &newSrc1,
                     Opnd* &newSrc2)
{
    Inst *orgInst = orgCmp->getInst();
    ComparisonModifier orgMod = orgInst->getComparisonModifier();
    Opnd *src1 = orgInst->getSrc(0);
    Opnd *src2 = orgInst->getSrc(1);
    if (negateIt) {
        // negate the comparison, by swapping parameters and adjusting test
        newInstType = orgInst->getType();
        newSrc1 = src2;
        newSrc2 = src1;
        if (Type::isFloatingPoint(newInstType)) {
            // for floats, invert NaN handling
            switch (orgMod) {
            case Cmp_GT: newmod = Cmp_GTE_Un; break;
            case Cmp_GT_Un: newmod = Cmp_GTE; break;
            case Cmp_GTE: newmod = Cmp_GT_Un; break;
            case Cmp_GTE_Un: newmod = Cmp_GT; break;
            case Cmp_EQ: newmod = Cmp_NE_Un; break;
            case Cmp_NE_Un: newmod = Cmp_EQ; break;
            default: assert(0); break;
            }
        } else {
            // for integers, don't modify signed/unsigned comparison
            switch (orgMod) {
            case Cmp_GT: newmod = Cmp_GTE; break;
            case Cmp_GT_Un: newmod = Cmp_GTE_Un; break;
            case Cmp_GTE: newmod = Cmp_GT; break;
            case Cmp_GTE_Un: newmod = Cmp_GT_Un; break;
            case Cmp_EQ: newmod = Cmp_NE_Un; break;
            case Cmp_NE_Un: newmod = Cmp_EQ; break;
            default: assert(0); break;
            }
        }
    } else {
        newInstType = orgInst->getType();
        newmod = orgMod;
        newSrc1 = src1;
        newSrc2 = src2;
    }
}

bool haveSameSignedTypes(Opnd* op1, Opnd *op2, Opnd *op3, Opnd *op4,
                         Modifier addMod,
                         ComparisonModifier cmpMod)
{
    Type *type1 = op1->getType();
    if (!Type::isSignedInteger(type1->tag)) return false;
    if (!addMod.isSigned()) return false;
    if (!isComparisonModifierSigned(cmpMod)) return false;
    return ((type1 == op2->getType()) &&
            (type1 == op3->getType()));
}

// check for the cases of cmp(cmp3(a,b), const)
bool
Simplifier::simplifyCmpOfCmp3(Type::Tag instType,
                              ComparisonModifier mod,
                              Opnd* src1,
                              Opnd* src2,
                              Type::Tag &newInstType,
                              ComparisonModifier &newmod,
                              Opnd* &newSrc1,
                              Opnd* &newSrc2)
{
    ConstInst::ConstValue valC;
    if (src1->getInst()->getOpcode() == Op_Cmp3) {
        if (ConstantFolder::isConstant(src2->getInst(), valC)) {
            I_32 val = valC.i4;
            if (((val == 0) && (mod == Cmp_GT)) ||
                ((val == 1) && ((mod == Cmp_GTE) || (mod == Cmp_EQ)))) {
                // test == 1
                // test > 0
                // test >= 1
                return simplifyCmp3ByResult(src1, false, false, true,
                                            newInstType, newmod, newSrc1, newSrc2);
            } else if ((val == 0) && (mod == Cmp_EQ)) {
                // test == 0
                return simplifyCmp3ByResult(src1, false, true, false,
                                            newInstType, newmod, newSrc1, newSrc2);
            } else if (((val == 0) && (mod == Cmp_GTE)) ||
                       ((val == -1) && (mod == Cmp_GT))) {
                // test >= 0
                // test > -1
                return simplifyCmp3ByResult(src1, false, true, true,
                                            newInstType, newmod, newSrc1, newSrc2);
            } else if (((val == -1) && (mod == Cmp_EQ)) ||
                       ((mod == Cmp_GTE_Un) &&
                        ((val < -1) || (val >= 2))) ||
                       ((mod == Cmp_GT_Un) &&
                        ((val < -2) || (val >= 1)))) {
                // test == -1
                // test >=u 2..maxint
                // test >=u minint..-1
                // test >u 1..maxint
                // test >u minint..-2
                return simplifyCmp3ByResult(src1, true, false, false,
                                            newInstType, newmod, newSrc1, newSrc2);
            } else if ((val == 0) && ((mod == Cmp_GT_Un) ||
                                      (mod == Cmp_NE_Un))) {
                // test != 0
                // test >u 0
                return simplifyCmp3ByResult(src1, true, false, true,
                                            newInstType, newmod, newSrc1, newSrc2);
            } else if ((val == 1) && (mod == Cmp_NE_Un)) {
                return simplifyCmp3ByResult(src1, true, true, false,
                                            newInstType, newmod, newSrc1, newSrc2);
            } else if ((val == -1) && (mod == Cmp_NE_Un)) {
                return simplifyCmp3ByResult(src1, false, true, true,
                                            newInstType, newmod, newSrc1, newSrc2);
            }
        }            
    } else if (src2->getInst()->getOpcode() == Op_Cmp3) {
        if (ConstantFolder::isConstant(src1->getInst(), valC)) {
            I_32 val = valC.i4;
            // val cmp Cmp3
            if ((val == 1) && (mod == Cmp_EQ)) {
                // 1 == test
                return simplifyCmp3ByResult(src2, false, false, true,
                                            newInstType, newmod, newSrc1, newSrc2);
            } else if (((val == 0) && (mod == Cmp_EQ)) ||
                       ((val == 1) && (mod == Cmp_GT_Un))) {
                // 0 == test
                // 1 >u test
                return simplifyCmp3ByResult(src2, false, true, false,
                                            newInstType, newmod, newSrc1, newSrc2);
            } else if (((mod == Cmp_GT_Un) && ((val<=-1)||(val >= 1)))&& 
                       ((mod == Cmp_GTE_Un)&& ((val<-1) || (val > 1)))&& 
                       ((val == -1) && (mod == Cmp_NE_Un))) {
                // -1 != test
                // minint..-2 >u test
                // 1..maxint >u test
                // minint..-1 >=u test
                // 2..maxint >=u test
                return simplifyCmp3ByResult(src2, false, true, true,
                                            newInstType, newmod, newSrc1, newSrc2);
            } else if (((val == 0) && (mod == Cmp_GT)) ||
                       ((val == -1) && ((mod == Cmp_GTE) ||
                                        (mod == Cmp_EQ)))) {
                // -1 == test
                // 0 > test
                // -1 >= test
                return simplifyCmp3ByResult(src2, true, false, false,
                                            newInstType, newmod, newSrc1, newSrc2);
            } else if ((val == 0) && (mod == Cmp_NE_Un)) {
                // 0 != test
                return simplifyCmp3ByResult(src2, true, false, true,
                                            newInstType, newmod, newSrc1, newSrc2);
            } else if (((val == 1) && (mod == Cmp_NE_Un)) ||
                       ((val == 0) && (mod == Cmp_GTE))) {
                // 1 != test
                // 0 >= test
                return simplifyCmp3ByResult(src2, true, true, false,
                                            newInstType, newmod, newSrc1, newSrc2);
            } 
        }
    }
    return false;
}

// simplify some cases of cmp(cmp(), const)
bool 
Simplifier::simplifyCmpOfCmp(Type::Tag instType,
                             ComparisonModifier mod,
                             Opnd* src1,
                             Opnd* src2,
                             Type::Tag &newInstType,
                             ComparisonModifier &newmod,
                             Opnd* &newSrc1,
                             Opnd* &newSrc2)
{
    ConstInst::ConstValue valC;
    if (ConstantFolder::isConstant(src1->getInst(), valC)) {
        I_32 val = valC.i4;
        if (((val == 0) && (mod == Cmp_NE_Un)) ||
            ((val == 1) && (mod == Cmp_EQ))) {
            // same test as src1;
            cloneComparison(false, // not negated
                            src2,
                            newInstType, newmod, newSrc1, newSrc2);
            return true;
        } else if (((val == 0) && ((mod == Cmp_EQ) ||
                                   (mod == Cmp_GTE) ||
                                   (mod == Cmp_GTE_Un))) ||
                   ((val == 1) && ((mod == Cmp_NE_Un) ||
                                   (mod == Cmp_GT) ||
                                   (mod == Cmp_GT_Un)))) {
            cloneComparison(true, // negated
                            src2,
                            newInstType, newmod, newSrc1, newSrc2);
            return true;
        }
    } else if (ConstantFolder::isConstant(src2->getInst(), valC)) {
        I_32 val = valC.i4;
        if (((val == 0) && ((mod == Cmp_GT) || (mod == Cmp_NE_Un)
                            || (mod == Cmp_GT_Un))) ||
            ((val == 1) && ((mod == Cmp_GTE) || (mod == Cmp_EQ)
                            || (mod == Cmp_GTE_Un)))) {
            // same test as src1;
            cloneComparison(false, // not negated
                            src1,
                            newInstType, newmod, newSrc1, newSrc2);
            return true;
        } else if (((val == 0) && (mod == Cmp_EQ)) ||
                   ((val == 1) && (mod == Cmp_NE_Un))) {
            // negate src1, by swapping parameters and adjusting test
            cloneComparison(true, // negated
                            src1,
                            newInstType, newmod, newSrc1, newSrc2);
            return true;
        }
    }
    return false;
}
    
bool 
Simplifier::simplifyCmpOfAddC(Type::Tag instType,
                              ComparisonModifier mod,
                              Opnd* addOpnd,
                              Opnd* otherOpnd,
                              Type::Tag &newInstType,
                              ComparisonModifier &newmod,
                              Opnd* &newAddSrc,
                              Opnd* &newOtherSrc,
                              bool swapped)
{
    Inst *addInst = addOpnd->getInst();
    assert(addInst->getNumSrcOperands() == 2);
    Opnd *addOp1 = addInst->getSrc(0);
    Opnd *addOp2 = addInst->getSrc(1);
    if (haveSameSignedTypes(addOpnd, otherOpnd, addOp1, addOp2,
                            addInst->getOverflowModifier(),
                            mod)) {
        Opnd *constAddOpnd;
        Opnd *otherAddOpnd;
        if (ConstantFolder::isConstant(addOp1)) {
            constAddOpnd = addOp1;
            otherAddOpnd = addOp2;
        } else {
            constAddOpnd = addOp2;
            otherAddOpnd = addOp1;
        }
        // turn ((x+c1) <> c2) into (x <> (c2-c1))
        if (ConstantFolder::isConstant(otherOpnd)) {
            Type *type2 = addOpnd->getType();
            Opnd *newOpnd =
                fold(Op_Sub, type2, 
                     otherOpnd->getInst()->asConstInst(), 
                     constAddOpnd->getInst()->asConstInst(),
                     true);
            newInstType = instType;
            newmod = mod;
            newAddSrc = otherAddOpnd;
            newOtherSrc = newOpnd;
            return true;
        }
        // turn (x >= (y+1)) into (x > y)
        if (ConstantFolder::isConstantOne(constAddOpnd)) {
            // turn ((x+1) > y) into (x >= y)
            if ((mod == Cmp_GT) && !swapped) {
                newInstType = instType;
                newmod = Cmp_GTE;
                newAddSrc = otherAddOpnd;
                newOtherSrc = otherOpnd;
                return true;
            }
            // turn (x >= (y+1)) into (x > y)
            if ((mod == Cmp_GTE) && swapped) {
                newInstType = instType;
                newmod = Cmp_GT;
                newAddSrc = otherAddOpnd;
                newOtherSrc = otherOpnd;
                return true;
            }
        } else if (ConstantFolder::isConstantAllOnes(constAddOpnd)) {
            // turn ((x-1) >= y) into (x > y)
            if ((mod == Cmp_GTE) && !swapped) {
                newInstType = instType;
                newmod = Cmp_GT;
                newAddSrc = otherAddOpnd;
                newOtherSrc = otherOpnd;
                return true;
            }
            // turn (x > (y-1)) into (x >= y)
            if ((mod == Cmp_GT) && swapped) {
                newInstType = instType;
                newmod = Cmp_GTE;
                newAddSrc = otherAddOpnd;
                newOtherSrc = otherOpnd;
                return true;
            }
        }
    }
    return false;
}

bool 
Simplifier::simplifyCmpOfSubC(Type::Tag instType,
                              ComparisonModifier mod,
                              Opnd* subOpnd,
                              Opnd* otherOpnd,
                              Type::Tag &newInstType,
                              ComparisonModifier &newmod,
                              Opnd* &newSubSrc,
                              Opnd* &newOtherSrc,
                              bool swapped)
{
    Inst *subInst = subOpnd->getInst();
    assert(subInst->getNumSrcOperands() == 2);
    Opnd *subOp1 = subInst->getSrc(0);
    Opnd *subOp2 = subInst->getSrc(1);
    if (haveSameSignedTypes(subOpnd, otherOpnd, subOp1, subOp2,
                            subInst->getOverflowModifier(),
                            mod)) {
        if (!ConstantFolder::isConstant(subOp2))
            return false;

        Opnd *constSubOpnd = subOp2;
        Opnd *otherSubOpnd = subOp1;

        // turn ((x-c1) <> c2) into (x <> (c2+c1))
        if (ConstantFolder::isConstant(otherOpnd)) {
            Type *type2 = subOpnd->getType();
            Opnd *newOpnd =
                fold(Op_Add, type2, 
                     otherOpnd->getInst()->asConstInst(), 
                     constSubOpnd->getInst()->asConstInst(),
                     true);
            newInstType = instType;
            newmod = mod;
            newSubSrc = otherSubOpnd;
            newOtherSrc = newOpnd;
            return true;
        }
        // turn (x > (y-1)) into (x >= y)
        if (ConstantFolder::isConstantOne(constSubOpnd)) {
            // turn ((x-1) >= y) into (x > y)
            if ((mod == Cmp_GTE) && !swapped) {
                newInstType = instType;
                newmod = Cmp_GT;
                newSubSrc = otherSubOpnd;
                newOtherSrc = otherOpnd;
                return true;
            }
            // turn (x > (y-1)) into (x >= y)
            if ((mod == Cmp_GT) && swapped) {
                newInstType = instType;
                newmod = Cmp_GTE;
                newSubSrc = otherSubOpnd;
                newOtherSrc = otherOpnd;
                return true;
            }
        } else if (ConstantFolder::isConstantAllOnes(constSubOpnd)) {
            // turn ((x+1) > y) into (x >= y)
            if ((mod == Cmp_GT) && !swapped) {
                newInstType = instType;
                newmod = Cmp_GTE;
                newSubSrc = otherSubOpnd;
                newOtherSrc = otherOpnd;
                return true;
            }
            // turn (x >= (y+1)) into (x > y)
            if ((mod == Cmp_GTE) && swapped) {
                newInstType = instType;
                newmod = Cmp_GT;
                newSubSrc = otherSubOpnd;
                newOtherSrc = otherOpnd;
                return true;
            }
        }
    }
    return false;
}

bool 
Simplifier::simplifyCmpOfAddOrSubC(Type::Tag instType,
                                   ComparisonModifier mod,
                                   Opnd* src1,
                                   Opnd* src2,
                                   Type::Tag &newInstType,
                                   ComparisonModifier &newmod,
                                   Opnd* &newSrc1,
                                   Opnd* &newSrc2)
{
    
    Inst *src1Inst = src1->getInst();
    if (isAddWithConstant(src1Inst)) {
        if (simplifyCmpOfAddC(instType, mod, src1, src2,
                              newInstType, newmod, newSrc1, newSrc2, false)) {
            return true;
        }
    } else if (isSubWithConstant(src1Inst)) {
        if (simplifyCmpOfSubC(instType, mod, src1, src2,
                              newInstType, newmod, newSrc1, newSrc2, false)) {
            return true;
        }
    }
    Inst *src2Inst = src2->getInst();
    if (isAddWithConstant(src2Inst)) {
        if (simplifyCmpOfAddC(instType, mod, src2, src1,
                              newInstType, newmod, newSrc2, newSrc1, true)) {
            return true;
        }
    } else if (isSubWithConstant(src2Inst)) {
        if (simplifyCmpOfSubC(instType, mod, src2, src1,
                              newInstType, newmod, newSrc2, newSrc1, true)) {
            return true;
        }
    }
    return false;
}

// Returns true if the described comparison can be simplified to a comparison;
//    if so, fills in the new comparison's modifier, src1, and src2.
// Interface is indirect to allow us to use this for Branches, too.
// 
// This is currently used to simplify a comparison of a Cmp3 with a constant.
bool 
Simplifier::simplifyCmpToCmp(Type::Tag instType,
                             ComparisonModifier mod,
                             Opnd* src1,
                             Opnd* src2,
                             Type::Tag &newInstType,
                             ComparisonModifier &newmod,
                             Opnd* &newSrc1,
                             Opnd* &newSrc2)
{
    ConstInst::ConstValue valC;
    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();
    if (optimizerFlags.elim_cmp3 && (instType == Type::Int32) &&
        simplifyCmpOfCmp3(instType, mod, src1, src2, newInstType, newmod,
                          newSrc1, newSrc2)) {
        return true;
    }
    if ((src1->getInst()->getOpcode() == Op_Cmp) ||
        (src2->getInst()->getOpcode() == Op_Cmp)) {
        if (simplifyCmpOfCmp(instType, mod, src1, src2, newInstType, newmod,
                             newSrc1, newSrc2))
            return true;
    }
    if (0) { // was: optimizerFlags.brm_debug
      if (ConstantFolder::isConstantZero(src1)) {
        newInstType = instType;
        newSrc1 = src2;
        newSrc2 = 0;
        switch (mod) {
        case Cmp_EQ: newmod = Cmp_Zero; return true;
        case Cmp_NE_Un: newmod = Cmp_NonZero; return true;
        case Cmp_GT: break;
        case Cmp_GT_Un: newmod = Cmp_NonZero; newSrc1 = src1; return true;
        case Cmp_GTE: break;
        case Cmp_GTE_Un: newmod = Cmp_Zero; newSrc1 = src2; return true;
        default: assert(0); break;
        }
      } else if (ConstantFolder::isConstantZero(src2)) {
        newInstType = instType;
        newSrc1 = src1;
        newSrc2 = 0;
        switch (mod) {
        case Cmp_EQ: newmod = Cmp_Zero; return true;
        case Cmp_NE_Un: newmod = Cmp_NonZero; return true;
        case Cmp_GT_Un: break;
        case Cmp_GT: break;
        case Cmp_GTE: break;
        case Cmp_GTE_Un: newmod = Cmp_Zero; newSrc1 = src2; return true;
        default: assert(0); break;
        }
      }
    }
    
    return false;
}

bool 
Simplifier::simplifyCmpToCmp(Type::Tag instType,
                             ComparisonModifier mod,
                             Opnd* src1,
                             Type::Tag &newInstType,
                             ComparisonModifier &newmod,
                             Opnd* &newSrc1)
{
    Inst *srci = src1->getInst();
    if (srci->getOpcode() == Op_TauStaticCast) {
        newInstType = instType;
        newSrc1 = srci->getSrc(0);
        newmod = mod;
        return true;
    }
    return false;
}

bool
Simplifier::canFoldBranch(Type::Tag instType,
                          ComparisonModifier mod,
                          Opnd* src1,
                          Opnd* src2,
                          bool& isTaken) {
    ConstInst::ConstValue val1, val2;
    if (ConstantFolder::isConstant(src1->getInst(), val1) && 
        ConstantFolder::isConstant(src2->getInst(), val2)) {
        // this branch can be folded
        ConstInst::ConstValue result;
        if (ConstantFolder::foldCmp(instType, mod, val1, val2, result)) {
            if (result.i4 == 0) {
                // branch not taken
                isTaken = false;
            } else {
                // branch taken
                isTaken = true;
            }
            return true;
        }
    } else if(src1 == src2) {
        if(src1->getType()->isFloatingPoint())
            // We can't assume x == x.  E.g., Nan != Nan
            return false;
        switch (mod) {
            case Cmp_EQ:    
            case Cmp_GTE:   
            case Cmp_GTE_Un:
                isTaken = true;
                return true;
            case Cmp_NE_Un: 
            case Cmp_GT:    
            case Cmp_GT_Un: 
                isTaken = false;
                return true;
            default:
                break;
        }
    } else if (Type::isVTablePtr(instType)) {
        Inst* src1DefInst = propagateCopy(src1)->getInst();
        Inst* src2DefInst = propagateCopy(src2)->getInst();
        if (src1DefInst->getOpcode() == Op_GetVTableAddr && src2DefInst->getOpcode() == Op_GetVTableAddr) {
            Type* src1ObjType = src1DefInst->asTypeInst()->getTypeInfo();
            Type* src2ObjType = src2DefInst->asTypeInst()->getTypeInfo();
            if (!src1ObjType->isUnresolvedType() && !src2ObjType->isUnresolvedType()) {
                isTaken = (src1ObjType == src2ObjType && mod == Cmp_EQ) 
                        || (src1ObjType != src2ObjType && mod == Cmp_NE_Un);
                return true;
            }
        }
    }
    return false;
}

bool
Simplifier::simplifyBranch(Type::Tag instType,
                           ComparisonModifier mod,
                           LabelInst* label,
                           Opnd* src1,
                           Opnd* src2)
{
    // try to simplify to a simpler comparison
    ComparisonModifier newMod;
    Type::Tag newInstType;
    Opnd *newSrc1;
    Opnd *newSrc2;
    if (simplifyCmpToCmp(instType, mod, src1, src2,
                         newInstType, newMod, newSrc1, newSrc2)) {
        // can simplify it.
        if (newSrc2) {
            genBranch(newInstType, newMod, label, newSrc1, newSrc2);
        } else {
            genBranch(newInstType, newMod, label, newSrc1);
        }
        return true;
    }
    return false;
}

bool
Simplifier::simplifyBranch(Type::Tag instType,
                           ComparisonModifier mod,
                           LabelInst* label,
                           Opnd* src1)
{
    // try to simplify to a simpler comparison
    ComparisonModifier newMod;
    Type::Tag newInstType;
    Opnd *newSrc1;
    if (simplifyCmpToCmp(instType, mod, src1, 
                         newInstType, newMod, newSrc1)) {
        // can simplify it.
        genBranch(newInstType, newMod, label, newSrc1);
        return true;
    }
    return false;
}


bool
Simplifier::canFoldBranch(Type::Tag instType,
                          ComparisonModifier mod,
                          Opnd* src,
                          bool& isTaken) {
    ConstInst::ConstValue val;
    if (ConstantFolder::isConstant(src->getInst(), val)) {
        ConstInst::ConstValue result;
        if (ConstantFolder::foldCmp(instType, mod, val, result)) {
            if (result.i4 == 0) {
                // branch not taken
                isTaken = false;
            } else {
                // branch taken
                isTaken = true;
            }
            return true;
        }
    } else if (isNonNullObject(src)) {
        if(mod == Cmp_Zero) {
                // branch not taken
            isTaken = false;
        } else {
            // branch taken
            isTaken = true;
        }
        return true;
    }
    return false;
}

bool
Simplifier::simplifySwitch(U_32 numLabels,
                           LabelInst* label[],
                           LabelInst* defaultLabel,
                           Opnd* src) {
    assert(numLabels > 0);
    // check for just 1 target
    LabelInst* label1 = label[0];
    U_32 i;
    for (i = 1; i<numLabels; i++) {
        if (label[i] != label1) return false; // we have >1 distinct labels
    }
    // have just 1 target;
    Opnd* numTargets = genLdConstant((I_32) numLabels)->getDst();
    genBranch(Type::Int32, Cmp_GT_Un, label1, numTargets, src);
    return true;
}
//-----------------------------------------------------------------------------
// Runtime check simplifications
//-----------------------------------------------------------------------------

    // These all return a tau normally.  If we can tell that they will always pass, 
    // then returns the destination of a tauSafe() operation.  If we can tell that they
    // will always fail, these methods return an original dstOpnd.  (This can be
    // used eventually to insert a throw.)  Otherwise, returns NULL.

Opnd*
Simplifier::simplifyTauCheckNull(Opnd* opnd, bool &alwaysThrows) {
    if (isNonNullObject(opnd))
        return genTauSafe()->getDst(); // is safe by construction
    if (isNonNullParameter(opnd)) {
        return genTauIsNonNull(opnd)->getDst(); // is safe, but only in method
    }
    if (isNullObject(opnd)) {
        if (Log::isEnabled()) {
            Log::out() << "CheckNull of src ";
            opnd->print(Log::out());
            Log::out() << " always throws" << ::std::endl;
        }
        genThrowSystemException(CompilationInterface::Exception_NullPointer);
        alwaysThrows = true;
        return genTauUnsafe()->getDst();
    }
    return NULL;
}

Opnd*
Simplifier::simplifyTauCheckBounds(Opnd* arrayLen, Opnd* index, bool &alwaysThrows) {
    // check if array was created with constant length
    // e.g., newarray with a constant length parameter
    ConstInst* constArrayLen = arrayLen->getInst()->asConstInst();
    ConstInst* constIndex = index->getInst()->asConstInst();
    if (constArrayLen && constIndex) {
        // compare the constant size and index
        I_32 result = 0;
        I_32 lenv = constArrayLen->getValue().i4;
        I_32 idxv = constIndex->getValue().i4;
        if (ConstantFolder::foldCmp32(Cmp_GT_Un,
                      lenv, idxv, result)) {
            if (result == 1) {
                // index is smaller than size, remove check
                return genTauSafe()->getDst(); // is safe by construction
            } else {
                // fold to a throwSystemId
                if (Log::isEnabled()) {
                    Log::out() << "Checkbounds of arrayLen ";
                    arrayLen->print(Log::out());
                    Log::out() << " and index ";
                    index->print(Log::out());
                    Log::out() << " equivalent to testing ("
                               << (int)lenv << " GTU " << (int)idxv
                               << ") = " << (int) result
                               << ", and always throws" << ::std::endl;
                }
                genThrowSystemException(CompilationInterface::Exception_ArrayIndexOutOfBounds);
                alwaysThrows = true;
                return genTauUnsafe()->getDst();
            }
        }
    }
    return NULL;
}

Opnd*
Simplifier::simplifyTauCheckLowerBound(Opnd* lb, Opnd *idx, bool &alwaysThrows) {
    ConstInst* constLB = lb->getInst()->asConstInst();
    ConstInst* constIndex = idx->getInst()->asConstInst();
    if (constLB != NULL && constIndex != NULL) {
        // compare the constant size and index
        I_32 result = 0;
        I_32 lbv = constLB->getValue().i4;
        I_32 idxv = constIndex->getValue().i4;
        if (ConstantFolder::foldCmp32(Cmp_GT,
                                      lbv,
                                      idxv,
                                      result)) {
            if (result == 1) {
                // fold to a throwSystemId
                if (Log::isEnabled()) {
                    Log::out() << "CheckLowerBound of lb ";
                    lb->print(Log::out());
                    Log::out() << " and index ";
                    idx->print(Log::out());
                    Log::out() << " equivalent to testing ("
                               << (int)lbv << " GT " << (int)idxv
                               << ") = " << (int) result
                               << ", and always throws" << ::std::endl;
                }
                genThrowSystemException(CompilationInterface::Exception_ArrayIndexOutOfBounds);
                alwaysThrows = true;
                return genTauUnsafe()->getDst();
            } else {
                // index is smaller than size, remove check
                return genTauSafe()->getDst(); // is safe by construction
            }
        }
    }
    return NULL;
}


Opnd*
Simplifier::simplifyTauCheckUpperBound(Opnd* idx, Opnd* ub, bool &alwaysThrows) {
    // check if both happen to be constant
    // which is pretty unlikely, since only ABCD inserts this instruction,
    // and it's pretty smart.
    ConstInst* constUB = ub->getInst()->asConstInst();
    ConstInst* constIndex = idx->getInst()->asConstInst();
    if (constUB != NULL && constIndex != NULL) {
        // compare the constant size and index
        I_32 result = 0;
        I_32 idxv = constIndex->getValue().i4;
        I_32 ubv = constUB->getValue().i4;
        if (ConstantFolder::foldCmp32(Cmp_GT,
                                      ubv,
                                      idxv,
                                      result)) {
            if (result == 1) {
                // index is smaller than size, remove check
                return genTauSafe()->getDst(); // check is safe by construction
            } else {
                // fold to a throwSystemId
                if (Log::isEnabled()) {
                    Log::out() << "CheckUpperBound of idx ";
                    idx->print(Log::out());
                    Log::out() << " and ub ";
                    ub->print(Log::out());
                    Log::out() << " equivalent to testing NOT("
                               << (int)ubv << " GT " << (int)idxv
                               << ") = " << (int) result
                               << ", and always throws" << ::std::endl;
                }
                genThrowSystemException(CompilationInterface::Exception_ArrayIndexOutOfBounds);
                alwaysThrows = true;
                return genTauUnsafe()->getDst();
            }
        }
    }
    return NULL;
}


//
// looks for a load of a non-zero constant
//
Opnd*
Simplifier::simplifyTauCheckZero(Opnd* opnd, bool &alwaysThrows) {
    I_32 value;
    if (ConstantFolder::isConstant(opnd->getInst(), value)) {
        if (value != 0)
            return genTauSafe()->getDst(); // check is safe by construction
        else {
            if (Log::isEnabled()) {
                Log::out() << "CheckZero of opnd ";
                opnd->print(Log::out());
                Log::out()<< " always throws" << ::std::endl;
            }
            genThrowSystemException(CompilationInterface::Exception_DivideByZero);
            alwaysThrows = true;
            return genTauUnsafe()->getDst();
        }
    }
    return NULL;
}

//
// simplify check if either operand is known
//
Opnd*
Simplifier::simplifyTauCheckDivOpnds(Opnd* src1, Opnd* src2, bool &alwaysThrows) {
    // look for anything other than src1=MAXNEGINT, src2=-1
    I_32 value;
    bool elim = false;
    if (ConstantFolder::isConstant(src1->getInst(), value)) {
        if ((U_32)value != 0x80000000) {
            // overflow can't happen
            elim = true;
        } else if (ConstantFolder::isConstant(src2->getInst(), value) &&
                   (value == -1)) {
            // overflow will happen for sure
            alwaysThrows = true;
            return genTauUnsafe()->getDst();
        }
    } else if (ConstantFolder::isConstant(src2->getInst(), value) 
               && (value != -1)) {
        elim = true;
    }
    if (!elim) {
        // above constant folders return false if opnd is not I_32
        // try int64 instead
        int64 value64;
        if (ConstantFolder::isConstant(src1->getInst(), value64)) {
            if ((uint64)value64 != __INT64_C(0x8000000000000000)) {
                // overflow can't happen
                elim = true;
            } else if (ConstantFolder::isConstant(src2->getInst(), value64) 
                       && (value64 == __INT64_C(-1))) {
                // overflow will happen for sure
                alwaysThrows = true;
                return genTauUnsafe()->getDst();
            }
        } else if (ConstantFolder::isConstant(src2->getInst(), value64) 
                   && (value64 != __INT64_C(-1)))
            elim = true;
    }
    if (elim) {
        return genTauSafe()->getDst(); // check is safe by construction
    }
    return NULL;
}

//
// looks for a load of a constant which is definitely finite
//    (not NaN, +infinity, or -infinity)
//
Opnd*
Simplifier::simplifyTauCheckFinite(Opnd* opnd, bool &alwaysThrows) {
    Type *opType = opnd->getType();
    ConstInst::ConstValue value;
    if (ConstantFolder::isConstant(opnd->getInst(), value)) {
        switch (opType->tag) {
        case Type::Single: // single
            {
                float s = value.s;
                if (!finite((double)s)) {
                    alwaysThrows = true;
                    return genTauUnsafe()->getDst();
                }
                break;
            }
        case Type::Double:
            {
                double d = value.d;
                if (!finite(d)) {
                    alwaysThrows = true;
                    return genTauUnsafe()->getDst();
                }
                break;
            }
        case Type::Float:
        default:
            return NULL;
        }
        return genTauSafe()->getDst(); // safe by construction
    } else
        return NULL;
}

//-----------------------------------------------------------------------------
// Type checking simplifications
//-----------------------------------------------------------------------------
Opnd*
Simplifier::simplifyTauCast(Opnd* src, Opnd *tauCheckedNull, Type* castType) {
    Type *opndType = src->getType();
    if (isNullObject(src) || 
        irManager.getTypeManager().isResolvedAndSubClassOf(opndType, castType)) {
        return genTauStaticCast(src, tauCheckedNull, castType)->getDst();
    }
    return NULL;
}

Opnd*
Simplifier::simplifyTauAsType(Opnd* src, Opnd* tauCheckedNull, Type* type) {
    Type* srcType = src->getType();
    if (isNullObject(src) || 
        irManager.getTypeManager().isResolvedAndSubClassOf(srcType, type))
        return src;
    if (isExactType(src) &&
        !irManager.getTypeManager().isResolvedAndSubClassOf(srcType, type)) {
        ConstInst::ConstValue val;
        val.i = NULL;
        return genLdConstant(irManager.getTypeManager().getNullObjectType(),
                             val)->getDst();
    }
    return NULL;
}

Opnd*
Simplifier::simplifyTauInstanceOf(Opnd* src, Opnd* tauCheckedNull, Type* type) {
    Type *srcType = src->getType();

    bool srcIsNonNull = (tauCheckedNull->getInst()->getOpcode() != Op_TauUnsafe);
    if (srcIsNonNull &&
        irManager.getTypeManager().isResolvedAndSubClassOf(srcType, type)) {
        return genLdConstant((I_32)1)->getDst();
    } 

    // If src is definitely a null object, then the result is 0.
    if ((!srcIsNonNull) && isNullObject(src)) {
        return genLdConstant((I_32)0)->getDst();
    }

    if (isExactType(src) &&
        !irManager.getTypeManager().isResolvedAndSubClassOf(srcType, type))
        return genLdConstant((I_32)0)->getDst();

    return NULL;
}

Opnd*
Simplifier::simplifyTauCheckElemType(Opnd* arrayBase, Opnd* src, bool &alwaysThrows) {
    if (isNullObject(src))
        return genTauSafe()->getDst(); // always safe to store null
    assert(arrayBase->getType()->isArrayType());
    Type *arrayElemType = ((ArrayType*)arrayBase->getType())->getElementType();
    if (isExactType(arrayBase) &&
        irManager.getTypeManager().isResolvedAndSubClassOf(src->getType(), arrayElemType)) {
        return genTauHasExactType(arrayBase,arrayBase->getType())->getDst();
    }
    // check to see if src was loaded from arrayBase
    if (src->getInst()->getOpcode() == Op_TauLdInd) {
        // src was loaded from memory
        Opnd* ptr = src->getInst()->getSrc(0);
        while (ptr->getInst()->getOpcode() == Op_AddScaledIndex) {
            // ptr is a ptr into an array
            ptr = ptr->getInst()->getSrc(0);
        }
        if (ptr->getInst()->getOpcode() == Op_LdArrayBaseAddr) {
            // ptr is an array base address
            Opnd* srcBase = ptr->getInst()->getSrc(0);
            if (srcBase == arrayBase) {
                return genTauSafe()->getDst(); // is safe, was loaded from same array
            }
        }
    }
    return NULL;
}

//-----------------------------------------------------------------------------
// Array access simplifications
//-----------------------------------------------------------------------------
Opnd*
Simplifier::simplifyTauArrayLen(Type* dstType, Type::Tag type, Opnd* base) {
    return getArraySize(base);
}

Opnd*
Simplifier::simplifyTauArrayLen(Type* dstType, Type::Tag type, Opnd* base,
                                Opnd* tauNullChecked, Opnd *tauTypeChecked) {
    return getArraySize(base);
}

Opnd*
Simplifier::simplifyAddScaledIndex(Opnd* base, Opnd* index) {
    ConstInst* constIndexInst = index->getInst()->asConstInst();
    if ( constIndexInst != NULL ) {
        assert(index->getType()->isInteger());
        if (constIndexInst->getValue().i8 == 0)
            return base;
    }
    return NULL;
}

//
// Compressed reference operations
//

Opnd*
Simplifier::simplifyUncompressRef(Opnd* opnd) {
    Inst *opndInst = opnd->getInst();
    if (opndInst->getOpcode() == Op_CompressRef) {
        Opnd *uncompRef = opndInst->getSrc(0);
        return uncompRef;
    }
    if (opndInst->getOpcode() == Op_LdConstant) {
        Type::Tag opndType = opndInst->getType();
        if (opndType == Type::CompressedNullObject) {
            // create an uncompressed null instead
            return genLdConstant(irManager.getTypeManager().getNullObjectType(), 
                                 ConstInst::ConstValue())->getDst();
        }
    }
    return NULL;
}

Opnd*
Simplifier::simplifyCompressRef(Opnd* opnd) {
    Inst *opndInst = opnd->getInst();
    if (opndInst->getOpcode() == Op_UncompressRef) {
        Opnd *compRef = opndInst->getSrc(0);
        return compRef;
    }
    if (opndInst->getOpcode() == Op_LdConstant) {
        Type::Tag opndType = opndInst->getType();
        if (opndType == Type::NullObject) {
            // create a compressed null instead
            return genLdConstant(irManager.getTypeManager().getCompressedNullObjectType(),
                                 ConstInst::ConstValue())->getDst();
        }
    }
    return NULL;
}

Opnd*
Simplifier::simplifyAddOffset(Type *ptrType, Opnd* uncompBase, Opnd *offset) {
    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();
    if (optimizerFlags.reduce_compref) {
        Inst *uncompBaseInst = uncompBase->getInst();
        if (uncompBaseInst->getOpcode() == Op_UncompressRef) {
            // try to convert to addOffsetPlusHeapbase
            Inst *offsetInst = offset->getInst();
            Inst *offsetPlusHeapbaseInst = 0;
            switch (offsetInst->getOpcode()) {
            case Op_LdFieldOffset: 
                {
                    FieldAccessInst *fainst = offsetInst->asFieldAccessInst();
                    FieldDesc *fd = fainst->getFieldDesc();
                    offsetPlusHeapbaseInst = genLdFieldOffsetPlusHeapbase(fd);
                    break;
                }
            case Op_LdArrayBaseOffset:
                {
                    TypeInst *tinst = offsetInst->asTypeInst();
                    Type *elemType = tinst->getTypeInfo();
                    offsetPlusHeapbaseInst = genLdArrayBaseOffsetPlusHeapbase(elemType);
                    break;
                }
            case Op_LdArrayLenOffset:  
                {
                    TypeInst *tinst = offsetInst->asTypeInst();
                    Type *elemType = tinst->getTypeInfo();
                    offsetPlusHeapbaseInst = genLdArrayLenOffsetPlusHeapbase(elemType);
                    break;
                }
            default:
                break;
            }
            if (offsetPlusHeapbaseInst) {
                Opnd *offsetPlusHeapbaseOpnd = offsetPlusHeapbaseInst->getDst();
                Opnd *compBase = uncompBaseInst->getSrc(0);
                Inst *addOffsetPlusHeapbaseInst 
                    = genAddOffsetPlusHeapbase(ptrType,
                                               compBase,
                                               offsetPlusHeapbaseOpnd);
                Opnd *result = addOffsetPlusHeapbaseInst->getDst();
                return result;
            }
        }
        if (0 && theReassociate) {
            Opnd *opnd = simplifyAddOffsetViaReassociation(uncompBase, offset);
            return opnd;
        }
    }
    return NULL;
}

Opnd*
Simplifier::simplifyAddOffsetPlusHeapbase(Type *ptrType,
                                          Opnd* compBase, 
                                          Opnd *offsetPlusHeapbase) {
    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();
    if (0 && optimizerFlags.reduce_compref && theReassociate) {
        Opnd *opnd = simplifyAddOffsetPlusHeapbaseViaReassociation(compBase,
                                                                   offsetPlusHeapbase); 
        return opnd;
    }
    return NULL;
}

//
// Store simplifications
//

// if compressRef, then this is a store into the heap, so if it's a reference,
// it should be compressed
Opnd *
SimplifierWithInstFactory::simplifyStoreSrc(Opnd* src, Type::Tag &typetag, 
                                            Modifier &mod,
                                            bool compressRef)
{
    Inst *srci = src->getInst();
    if ((srci->getOpcode() == Op_Conv) &&
        (typetag == srci->getType())) {
        Opnd *srcisrc = srci->getSrc(0);
        if (srcisrc->getType()->tag == src->getType()->tag)
            return srcisrc;
    }

    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();

    if (compressRef &&
        optimizerFlags.reduce_compref && 
        (mod.getAutoCompressModifier() == AutoCompress_Yes)) {

        Type *srcType = src->getType();
        assert(srcType->isReference());
        assert(!srcType->isCompressedReference());

        Type *newSrcType = typeManager.compressType(srcType);
        Modifier newMod = (Modifier(mod.getStoreModifier()) | 
                           Modifier(AutoCompress_No));
        Opnd *compressedSrc = genCompressRef(src)->getDst();

        typetag = newSrcType->tag;
        mod = newMod;
        return compressedSrc;
    }

    return 0;
}

void
Simplifier::simplifyTauStStatic(Inst *inst)
{
    if (Log::isEnabled()) {
        Log::out() << "Trying to simplify TauStStatic: ";
        inst->print(Log::out());
        Log::out() << ::std::endl;
    }
    FieldAccessInst *fieldInst = inst->asFieldAccessInst();
    assert(fieldInst);
    Type::Tag typetag = fieldInst->getType();
    Modifier mod = fieldInst->getModifier();
    assert(inst->getNumSrcOperands() == 2);
    Opnd *src = fieldInst->getSrc(0);

    Opnd *newSrc = simplifyStoreSrc(src, typetag, mod, true);
    if (newSrc) {
        inst->setSrc(0, newSrc);
        inst->setType(typetag);
        inst->setModifier(mod);
    }
}

void
Simplifier::simplifyTauStField(Inst *inst)
{
    if (Log::isEnabled()) {
        Log::out() << "Trying to simplify StField: ";
        inst->print(Log::out());
        Log::out() << ::std::endl;
    }
    FieldAccessInst *fieldInst = inst->asFieldAccessInst();
    assert(fieldInst);
    Type::Tag typetag = fieldInst->getType();
    Modifier mod = fieldInst->getModifier();
    assert(inst->getNumSrcOperands() == 4);
    Opnd *src = fieldInst->getSrc(0); // src

    Opnd *newSrc = simplifyStoreSrc(src, typetag, mod, true);
    if (newSrc) {
        inst->setSrc(0, newSrc);
        inst->setType(typetag);
        inst->setModifier(mod);
    }
}

void
Simplifier::simplifyTauStInd(Inst *inst)
{

    Modifier mod = inst->getModifier();
    Type::Tag typetag = inst->getType();
    assert(inst->getNumSrcOperands() == 5);
    Opnd *src = inst->getSrc(0);
    Opnd *ptr = inst->getSrc(1);
    
    Type *ptrType = ptr->getType();
    assert(ptrType->isPtr());
    Type *fieldType = ((PtrType *)ptrType)->getPointedToType();

    Opnd *newSrc = simplifyStoreSrc(src, typetag, mod, 
                                    fieldType->isCompressedReference());
    if (newSrc) {
        inst->setSrc(0, newSrc);
        inst->setType(typetag);
        inst->setModifier(mod);
    }
}


void
Simplifier::simplifyTauStElem(Inst *inst)
{
    Modifier mod = inst->getModifier();
    Type::Tag typetag = inst->getType();
    assert(inst->getNumSrcOperands() == 6);
    Opnd *src = inst->getSrc(0);

    Opnd *newSrc = simplifyStoreSrc(src, typetag, mod, true);
    if (newSrc) {
        inst->setSrc(0, newSrc);
        inst->setType(typetag);
        inst->setModifier(mod);
    }
}

void
Simplifier::simplifyTauStRef(Inst *inst)
{
    Type::Tag typetag = inst->getType();
    Modifier mod = inst->getModifier();
    assert(inst->getNumSrcOperands() == 6);
    Opnd *src = inst->getSrc(0);
    Opnd *pointer = inst->getSrc(1);

    Type *ptrType = pointer->getType();
    assert(ptrType->isPtr());
    Type *fieldType = ((PtrType *)ptrType)->getPointedToType();

    Opnd *newSrc = simplifyStoreSrc(src, typetag, mod, 
                                    fieldType->isCompressedReference());
    if (newSrc) {
        inst->setSrc(0, newSrc);
        inst->setType(typetag);
        inst->setModifier(mod);
    }
}

Opnd *
Simplifier::simplifyTauLdInd(Modifier mod, Type* dstType, Type::Tag type, Opnd *ptr,
                             Opnd *tauBaseNonNull, Opnd *tauAddressInRange)
{
    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();
    if (optimizerFlags.do_sxt && !optimizerFlags.ia32_code_gen) {
        // simplify signed loads to unsigned
        Opnd *newLd = 0;
        switch (type) {
        case Type::Int8:
            newLd = genTauLdInd(mod, dstType, Type::UInt8, ptr,
                                    tauBaseNonNull, tauAddressInRange)->getDst();
            break;
        case Type::Int16:
            newLd = genTauLdInd(mod, dstType, Type::UInt16, ptr,
                                    tauBaseNonNull, tauAddressInRange)->getDst();
            break;
        case Type::Int32:
            newLd = genTauLdInd(mod, dstType, Type::UInt32, ptr,
                                    tauBaseNonNull, tauAddressInRange)->getDst();
            break;
        default:
            break;
        }
        if (newLd) {
            Opnd *extOpnd = genConv(dstType, type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No), newLd)->getDst();
            return extOpnd;
        }
    }
    if (optimizerFlags.reduce_compref && 
        (mod.getAutoCompressModifier() == AutoCompress_Yes)) {
        
        assert(dstType->isReference());
        assert(!dstType->isCompressedReference());
        Type *compressedType = irManager.getTypeManager().compressType(dstType);
        Type *ptrType = ptr->getType();
        if( !(ptrType->isPtr()) ) assert(0);
        Type *fieldType = ((PtrType *)ptrType)->getPointedToType();
        if( !(fieldType->isCompressedReference()) ) assert(0);
        
        Opnd *newLd = genTauLdInd(AutoCompress_No, compressedType, 
                                  compressedType->tag, ptr, 
                                  tauBaseNonNull, tauAddressInRange)->getDst();
        Opnd *uncOpnd = genUncompressRef(newLd)->getDst();
        return uncOpnd;
    }
    return 0;
}


Opnd *
Simplifier::simplifyLdRef(Modifier mod, Type* dstType,
                          U_32 token, MethodDesc* enclosingMethod)
{
    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();
    if (optimizerFlags.reduce_compref && 
        (mod.getAutoCompressModifier() == AutoCompress_Yes)) {
        
        assert(dstType->isReference());
        assert(!dstType->isCompressedReference());
        Type *compressedType = irManager.getTypeManager().compressType(dstType);
        Opnd *newLdRef = genLdRef(AutoCompress_No, compressedType,
                                  token,
                                  enclosingMethod)->getDst();
        Opnd *uncOpnd = genUncompressRef(newLdRef)->getDst();
        return uncOpnd;
    }
    return 0;
}


//-----------------------------------------------------------------------------
// Method call related simplifications
//-----------------------------------------------------------------------------
Opnd*
Simplifier::simplifyTauLdVirtFunAddr(Opnd* vtable, Opnd *tauVtableHasMethod,
                                     MethodDesc* methodDesc) {
    Inst* ldVTableInst = vtable->getInst();
    if (ldVTableInst->getOpcode() == Op_TauLdVTableAddr ||
        ldVTableInst->getOpcode() == Op_TauLdIntfcVTableAddr) {
        Opnd* baseRef = ldVTableInst->getSrc(0);
        if (isExactType(baseRef)) {
        }
    } else {
        assert(0);
    }
    return NULL;
}

Opnd*
Simplifier::simplifyTauLdVirtFunAddrSlot(Opnd* vtable, Opnd *tauVtableHasMethod,
                                         MethodDesc* methodDesc) {
    // if vtable class is exact or final then change this
    // to a ldvirtfunaddrslot
    // if vtable is an interface vtable and the base of that
    // interface vtable is exact or final then change this
    // to a ldvirtfunaddrslot
    // if vtable is of exact type (loaded by getvtable) then
    // change this to a ldvirtfunaddrslot
    Inst* ldVTableInst = vtable->getInst();
    Type* baseType = NULL;
    if (ldVTableInst->getOpcode() == Op_TauLdVTableAddr || 
        ldVTableInst->getOpcode() == Op_TauLdIntfcVTableAddr) {
        Opnd* baseRef = ldVTableInst->getSrc(0);
        if (isExactType(baseRef)) {
            // base has exact type
            baseType = baseRef->getType();
        }
    } else if (ldVTableInst->getOpcode() == Op_GetVTableAddr) {
        TypeInst* typeInst = ldVTableInst->asTypeInst();
        assert(typeInst != NULL);
        baseType = typeInst->getTypeInfo();
    }
    if (baseType != NULL) {
        MethodDesc* newMethodDesc =
            irManager.getCompilationInterface().getOverridingMethod((NamedType*)baseType, methodDesc);
        if (newMethodDesc) {
            // change to ldvirtfunaddrslot of newMethodDesc
            return genLdFunAddrSlot(newMethodDesc)->getDst();
        }

    }
    return NULL;
}

Opnd*
Simplifier::simplifyTauLdIntfcVTableAddr(Opnd* base, Type* vtableType) {
    // Can't really simplify load of an interface vtable
    return NULL;
}

Opnd*
Simplifier::simplifyTauLdVTableAddr(Opnd* base, Opnd *tauBaseNonNull) {
    if(isExactType(base)) {
        Type* type = base->getType();
        assert((type->isClass() && !type->isAbstract()) || type->isArray());
        return genGetVTableAddr((ObjectType*) type)->getDst();
    }
    return NULL;
}

Opnd*
Simplifier::simplifyTauVirtualCall(MethodDesc* methodDesc,
                                   Type* returnType,
                                   Opnd* tauNullCheckedFirstArg,
                                   Opnd* tauTypesChecked,
                                   U_32 numArgs,
                                   Opnd* args[])
{
    //
    // change to a direct call if the type of the this pointer is exactly known
    // or if the method is final
    //
    if (isExactType(args[0]) || methodDesc->isFinal() || methodDesc->isPrivate()) {
        if(isExactType(args[0]) && !args[0]->getType()->isInterface()) {
            methodDesc = irManager.getCompilationInterface().getOverridingMethod(
                    (NamedType*) args[0]->getType(), methodDesc);
        }
        if (methodDesc == NULL || methodDesc->getParentType()->isValue()) {
            return NULL;
        }
        return genDirectCall(methodDesc, returnType,
                             tauNullCheckedFirstArg, tauTypesChecked,
                             numArgs, args)->getDst();
    }
    return NULL;
}
Inst*
Simplifier::simplifyIndirectCallInst(    Opnd* funPtr,
                                         Type* returnType,
                                         Opnd* tauNullCheckedFirstArg,
                                         Opnd* tauTypesChecked,
                                        U_32 numArgs,
                                        Opnd** args)
{
    return simplifyIndirectMemoryCallInst(funPtr, returnType, 
                                          tauNullCheckedFirstArg, tauTypesChecked,
                                          numArgs, args);
}

Inst*
Simplifier::simplifyIndirectMemoryCallInst(Opnd* funPtr,
                                           Type* returnType,
                                           Opnd* tauNullCheckedFirstArg,
                                           Opnd* tauTypesChecked,
                                          U_32 numArgs,
                                          Opnd** args)
{
    // if funptr is a load of a fun slot that does not go through a vtable, then
    // simplify this to a direct call if direct calls are enabled
    MethodInst* ldFunInst = funPtr->getInst()->asMethodInst();
    if (ldFunInst == NULL) {
        return NULL;
    }
    if (ldFunInst->getOpcode() == Op_LdFunAddrSlot) {
       return genDirectCall(ldFunInst->getMethodDesc(), returnType,
                             tauNullCheckedFirstArg, tauTypesChecked,
                             numArgs, args);
    }
    return NULL;    
}

Opnd*
Simplifier::simplifyTauAnd(MultiSrcInst *inst)
{
    U_32 nsrcs = inst->getNumSrcOperands();
    U_32 nsrcs0 = nsrcs;
    {
        for (U_32 i = 0; i < nsrcs; ++i) {
            Opnd *srci = inst->getSrc(i);
            Opcode opcode = srci->getInst()->getOpcode();
            if (opcode == Op_TauUnsafe) {
                return srci;
            } else if (opcode == Op_TauSafe) {
                // swap with last and decrease number
                nsrcs -= 1;
                if (i < nsrcs) {
                    Opnd *lastOpnd = inst->getSrc(nsrcs);
                    inst->setSrc(nsrcs, srci);
                    inst->setSrc(i, lastOpnd);
                    i -= 1; // re-examine that opnd
                }
            }
        }
    }
    if (nsrcs != nsrcs0) {
        inst->setNumSrcs(nsrcs);
    }
    switch (nsrcs) {
    case 0: // convert to TauSafe;
        return genTauSafe()->getDst(); // TauAnd of nothing is safe

    case 2:
        {
            if (inst->getSrc(0) != inst->getSrc(1)) {
                break; // don't simplify it
            }
        }
        // fall through and convert to copy;
    case 1: // convert to copy;
        return inst->getSrc(0);

    default:
        break;
    }
    return NULL;
}

Inst*
Simplifier::caseBranch(BranchInst* inst) {
    bool isTaken;
    Type::Tag type = inst->getType();
    ComparisonModifier mod = inst->getComparisonModifier();
    switch (inst->getNumSrcOperands()) {
    case 1:
        if(canFoldBranch(type,
            mod,
            inst->getSrc(0),
            isTaken)) { 
            foldBranch(inst, isTaken);
            return NULL;
        }
        else {
            if(simplifyBranch(type, mod, inst->getTargetLabel(), inst->getSrc(0)))
                return NULL;
        }
        break;
    case 2:
        if(canFoldBranch(type,
            mod,
            inst->getSrc(0),
            inst->getSrc(1),
            isTaken)) {
            foldBranch(inst, isTaken);
            return NULL;
        }
        else {
            if(simplifyBranch(type, mod, inst->getTargetLabel(), inst->getSrc(0), inst->getSrc(1)))
                return NULL;
        }
        break;
    }

    return inst;
}

Inst*
Simplifier::caseSwitch(SwitchInst* inst) {
    Opnd* index = inst->getSrc(0);
    I_32 value;
    if(ConstantFolder::isConstant(index->getInst(), value)) {
        foldSwitch(inst, value);
        return NULL;
    } else {
        U_32 numTarget = inst->getNumTargets();
        LabelInst** targets = inst->getTargets();
        LabelInst* defaultTarget = inst->getDefaultTarget();
        if(simplifySwitch(numTarget, targets, defaultTarget, index)) {
            return NULL;
        }
    }
    return inst;
}

Inst*
Simplifier::caseIndirectCall(CallInst* inst) {
    Opnd* dst = inst->getDst();
    Type* returnType = dst->isNull()? irManager.getTypeManager().getVoidType() : dst->getType();
    Opnd** args = inst->getArgs();
    U_32 numArgs = inst->getNumArgs();
    assert(numArgs >= 2);
    Opnd* tauNullCheckedFirstArg = args[0];
    Opnd* tauTypesChecked = args[1];
    Inst* newInst = simplifyIndirectCallInst(inst->getFunPtr(),
                                             returnType,
                                             tauNullCheckedFirstArg,
                                             tauTypesChecked,
                                             numArgs-2, // skip taus
                                             args+2);
    if (newInst != NULL) {
        return newInst;
    }
    return inst;
}

Inst*
Simplifier::caseIndirectMemoryCall(CallInst* inst) {
    Opnd* dst = inst->getDst();
    Type* returnType = dst->isNull()? irManager.getTypeManager().getVoidType() : dst->getType();
    Opnd** args = inst->getArgs();
    U_32 numArgs = inst->getNumArgs();
    assert(numArgs >= 2);
    Opnd* tauNullCheckedFirstArg = args[0];
    Opnd* tauTypesChecked = args[1];
    Inst* newInst = simplifyIndirectMemoryCallInst(inst->getFunPtr(),
                                                   returnType,
                                                   tauNullCheckedFirstArg,
                                                   tauTypesChecked,
                                                   numArgs-2,
                                                   args+2);
    if (newInst != NULL) {
        return newInst;
    }
    return inst;
}

Opnd*
Simplifier::propagateCopy(Opnd* opnd) {
    assert(opnd);
    Inst* inst = opnd->getInst();
    if (isCopy(inst)) {
        return inst->getSrc(0);
    }
    return opnd;
}

//-----------------------------------------------------------------------------
// Simplifier methods that generate instructions
//-----------------------------------------------------------------------------
SimplifierWithInstFactory::SimplifierWithInstFactory(IRManager& irm,
                             bool isLate, 
                             Reassociate *reassociate0)
    : Simplifier(irm, isLate, reassociate0),
      nextInst(NULL),
      currentCfgNode(NULL),
      instFactory(irm.getInstFactory()),
      opndManager(irm.getOpndManager()),
      typeManager(irm.getTypeManager()),
      tauSafeOpnd(NULL),
      tauMethodSafeOpnd(NULL),
      tauUnsafeOpnd(NULL)
{
}

void  
SimplifierWithInstFactory::foldBranch(BranchInst* br, bool isTaken) {
    FlowGraph::foldBranch(flowGraph, br,isTaken);
}

void  
SimplifierWithInstFactory::foldSwitch(SwitchInst* switchInst, U_32 index) {
    FlowGraph::foldSwitch(flowGraph, switchInst,index);
}


void  
SimplifierWithInstFactory::eliminateCheck(Inst* checkInst, bool alwaysThrows) {
    FlowGraph::eliminateCheck(flowGraph, currentCfgNode,checkInst,alwaysThrows);
}

U_32
SimplifierWithInstFactory::simplifyControlFlowGraph() {
    if (Log::isEnabled()) {
        Log::out() << "Starting simplifyControlFlowGraph" << ::std::endl;
    }

    U_32 numInstOptimized = 0;
    MemoryManager memManager("SimplifierWithInstFactory::simplifyControlFlowGraph");
    BitSet* reachableNodes = new (memManager) BitSet(memManager,flowGraph.getMaxNodeId());
    BitSet* unreachableInsts = 
        new (memManager) BitSet(memManager,irManager.getInstFactory().getNumInsts());
    StlVector<Node*> nodes(memManager);
    nodes.reserve(flowGraph.getMaxNodeId());
    //
    // Compute postorder list.
    //
    flowGraph.getNodesPostOrder(nodes);
    // Use reverse iterator to generate nodes in reverse postorder.
    StlVector<Node*>::reverse_iterator niter = nodes.rbegin();
    // mark first node as reachable
    reachableNodes->setBit((*niter)->getId(),true);
    for (niter = nodes.rbegin(); niter != nodes.rend(); ++niter) {
        currentCfgNode = *niter;
        Inst* headInst = (Inst*)currentCfgNode->getFirstInst();
        if (reachableNodes->getBit(currentCfgNode->getId()) == false) {
            // unreachable block
            // mark block's instructions as unreachable
            for (Inst* inst = headInst->getNextInst();inst!=NULL;inst=inst->getNextInst()) {
                unreachableInsts->setBit(inst->getId(),true);
            }
            // skip over unreachable block
            continue;
        }
        for (Inst* inst = headInst->getNextInst();inst!=NULL;) {
            Inst* nextInst = inst->getNextInst();
            if (Log::isEnabled()) {
                Log::out() << "Trying to simplify Instruction: ";
                inst->print(Log::out());
                Log::out() << ::std::endl;
            }
            Inst* optimizedInst = optimizeInst(inst);
            if (optimizedInst != inst) {
                if (Log::isEnabled()) {
                    Log::out() << "was simplified" << ::std::endl;
                }
                // simplification occurred
                numInstOptimized++;
                if (optimizedInst != NULL) {
                    if (Log::isEnabled()) {
                        Log::out() << "replacing with new instruction ";
                        optimizedInst->print(Log::out());
                        Log::out() << ::std::endl;
                    }
                    // instruction was not deleted by optimization
                    // replace with a copy from newInst.dst
                    Opnd* dstOpnd = inst->getDst();
                    //
                    // some operations, e.g., InitType, don't produce a
                    // value in a destination opnd
                    //
                    if (dstOpnd->isNull() == false) {
                        Opnd* srcOpnd = optimizedInst->getDst();
                        //
                        // Note, that sometimes dstOpnd could be a null operand 
                        // because of instructions that do not define a new 
                        // value (e.g., checkelemtype) but are simplified to 
                        // instructions that do (e.g., newobj); so we check if
                        // dstOpnd is null first.
                        //
                        Inst* copy = irManager.getInstFactory().makeCopy(dstOpnd, srcOpnd);
                        if (nextInst) {
                            assert(nextInst->getNode());
                            copy->insertBefore(nextInst);
                        } else {
                            currentCfgNode->appendInst(copy);
                        }
                        if (Log::isEnabled()) {
                            Log::out() << "inserting copy instruction ";
                            copy->print(Log::out());
                            Log::out() << ::std::endl;
                        }
                    }
                }
                inst->unlink();
            }
            inst = nextInst;
        }
        // mark successor blocks as reachable
        Edges::const_iterator
            i = currentCfgNode->getOutEdges().begin(),
            iend = currentCfgNode->getOutEdges().end();
        for (; i != iend; i++) {
            Node* succ = (*i)->getTargetNode();
            reachableNodes->setBit(succ->getId(),true);
        }
    }
    if (Log::isEnabled()) {
        Log::out() << "Done simplifyControlFlowGraph" << ::std::endl;
    }
    return numInstOptimized;
}

Inst*
SimplifierWithInstFactory::optimizeInst(Inst* inst) {
    nextInst = inst;
    return Simplifier::optimizeInst(inst);
}

void
SimplifierWithInstFactory::insertInst(Inst* inst) {
    inst->insertBefore(nextInst);
    inst->setBCOffset(nextInst->getBCOffset());
}

void
SimplifierWithInstFactory::insertInstInHeader(Inst* inst) {
    Node *head = flowGraph.getEntryNode();
    Inst *entryLabel = (Inst*)head->getFirstInst();
    // first search for one already there
    Inst *where = entryLabel->getNextInst();
    while (where != NULL) {
        if (where->getOpcode() != Op_DefArg) {
            break;
        }
        where = where->getNextInst();
    }
    // insert before where
    if (where!=NULL) {
        inst->insertBefore(where);
    } else {
       head->appendInst(inst);
    }
}

Inst*
SimplifierWithInstFactory::genAdd(Type* type, Modifier mod, Opnd* src1, Opnd* src2) {
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeAdd(mod, dst, src1, src2);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genSub(Type* type, Modifier mod, Opnd* src1, Opnd* src2) {
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeSub(mod, dst, src1, src2);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genNeg(Type* type, Opnd* src) {
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeNeg(dst, src);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genMul(Type* type, Modifier mod, Opnd* src1, Opnd* src2) {
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeMul(mod, dst, src1, src2);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genMulHi(Type* type, Modifier mod, Opnd* src1, Opnd* src2) {
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeMulHi(mod, dst, src1, src2);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genMin(Type* type, Opnd* src1, Opnd* src2) {
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeMin(dst, src1, src2);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genMax(Type* type, Opnd* src1, Opnd* src2) {
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeMax(dst, src1, src2);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genAbs(Type* type, Opnd* src1) {
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeAbs(dst, src1);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genAnd(Type* type, Opnd* src1, Opnd* src2) {
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeAnd(dst, src1, src2);
    insertInst(inst);
    return inst;
}
Inst*
SimplifierWithInstFactory::genOr(Type* type, Opnd* src1, Opnd* src2) {
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeOr(dst, src1, src2);
    insertInst(inst);
    return inst;
}
Inst*
SimplifierWithInstFactory::genXor(Type* type, Opnd* src1, Opnd* src2) {
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeXor(dst, src1, src2);
    insertInst(inst);
    return inst;
}
Inst*
SimplifierWithInstFactory::genNot(Type* type, Opnd* src1) {
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeNot(dst, src1);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genSelect(Type* type,
                                     Opnd* src1, Opnd* src2, Opnd* src3)
{
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeSelect(dst, src1, src2, src3);
    insertInst(inst);
    return inst;
}

Inst* 
SimplifierWithInstFactory::genConv(Type* type, Type::Tag toType, 
                                   Modifier ovfMod, 
                                   Opnd* src)
{
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeConv(ovfMod, toType, dst, src);
    insertInst(inst);
    return inst;
}

Inst* 
SimplifierWithInstFactory::genConvZE(Type* type, Type::Tag toType, 
                                   Modifier ovfMod, 
                                   Opnd* src)
{
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeConvZE(ovfMod, toType, dst, src);
    insertInst(inst);
    return inst;
}
Inst* 
SimplifierWithInstFactory::genConvUnmanaged(Type* type, Type::Tag toType, 
                                   Modifier ovfMod, 
                                   Opnd* src)
{
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeConvUnmanaged(ovfMod, toType, dst, src);
    insertInst(inst);
    return inst;
}


Inst*
SimplifierWithInstFactory::genShladd(Type* type,
                                     Opnd* value, Opnd* shiftAmount, 
                                     Opnd* addTo)
{
    OpndManager &opndManager = irManager.getOpndManager();
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeShladd(dst, value, shiftAmount, addTo);
    insertInst(inst);
    return inst;
}
Inst*
SimplifierWithInstFactory::genShl(Type* type,
                                  Modifier smmod,
                                  Opnd* src1, Opnd* src2) {
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeShl(smmod, dst, src1, src2);
    insertInst(inst);
    return inst;
}
Inst*
SimplifierWithInstFactory::genShr(Type* type, Modifier mods,
                                  Opnd* src1, Opnd* src2) {
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeShr(mods, dst, src1, src2);
    insertInst(inst);
    return inst;
}
Inst*
SimplifierWithInstFactory::genCmp(Type* type, Type::Tag insttype,
                                  ComparisonModifier mod, 
                                  Opnd* src1, Opnd* src2)
{
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeCmp(mod, insttype, dst, src1, src2);
    insertInst(inst);
    return inst;
}
void
SimplifierWithInstFactory::genBranch(Type::Tag insttype,
                                     ComparisonModifier mod, 
                                     LabelInst* label, 
                                     Opnd* src1, Opnd* src2)
{
    Inst* inst = instFactory.makeBranch(mod, insttype, src1, src2, label);
    insertInst(inst);
}

void
SimplifierWithInstFactory::genJump(LabelInst* label)
{
    Inst* inst = instFactory.makeJump(label);
    insertInst(inst);
}

void
SimplifierWithInstFactory::genBranch(Type::Tag insttype,
                                     ComparisonModifier mod, 
                                     LabelInst* label, 
                                     Opnd* src1)
{
    Inst* inst = instFactory.makeBranch(mod, insttype, src1, label);
    insertInst(inst);
}

Inst*
SimplifierWithInstFactory::genDirectCall(
                                MethodDesc* methodDesc,
                                      Type* returnType,
                                      Opnd* tauNullCheckedFirstArg,
                                      Opnd* tauTypesChecked,
                                     U_32 numArgs,
                                      Opnd* args[])
{
    Opnd* dst;
    if (returnType->tag == Type::Void) {
        dst = OpndManager::getNullOpnd();
    } else {
        dst = opndManager.createSsaTmpOpnd(returnType);
    }
    Inst* inst = instFactory.makeDirectCall(dst, 
                                            tauNullCheckedFirstArg, tauTypesChecked,
                                            numArgs, args, 
                                            methodDesc);
    insertInst(inst);
    return inst;
}
Inst*
SimplifierWithInstFactory::genLdConstant(I_32 val) {
    Opnd* dst = opndManager.createSsaTmpOpnd(typeManager.getInt32Type());
    Inst* inst = instFactory.makeLdConst(dst, val);
    insertInst(inst);
    return inst;
}
Inst*
SimplifierWithInstFactory::genLdConstant(int64 val) {
    Opnd* dst = opndManager.createSsaTmpOpnd(typeManager.getInt64Type());
    Inst* inst = instFactory.makeLdConst(dst, val);
    insertInst(inst);
    return inst;
}
Inst*
SimplifierWithInstFactory::genLdConstant(float val) {
    Opnd* dst = opndManager.createSsaTmpOpnd(typeManager.getSingleType());
    Inst* inst = instFactory.makeLdConst(dst, val);
    insertInst(inst);
    return inst;
}
Inst*
SimplifierWithInstFactory::genLdConstant(double val) {
    Opnd* dst = opndManager.createSsaTmpOpnd(typeManager.getDoubleType());
    Inst* inst = instFactory.makeLdConst(dst, val);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genLdConstant(Type* type, ConstInst::ConstValue val) {
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeLdConst(dst, val);
    insertInst(inst);
    return inst;
}

Inst* 
SimplifierWithInstFactory::genTauLdInd(Modifier mod, Type* type, Type::Tag ldType, 
                                       Opnd* ptr, Opnd *tauNonNullBase, 
                                       Opnd *tauAddressInRange)
{
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeTauLdInd(mod, ldType, dst, ptr, tauNonNullBase,
                                          tauAddressInRange);
    insertInst(inst);
    return inst;
}

Inst* 
SimplifierWithInstFactory::genLdRef(Modifier mod, Type* type, 
                                    U_32 token,
                                    MethodDesc *methodDesc)
{
    Opnd* dst = opndManager.createSsaTmpOpnd(type);
    Inst* inst = instFactory.makeLdRef(mod, dst, methodDesc, token);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genLdFunAddrSlot(MethodDesc* methodDesc) {
    Opnd* dst = opndManager.createSsaTmpOpnd(typeManager.getMethodPtrType(methodDesc));
    Inst* inst = instFactory.makeLdFunAddrSlot(dst, methodDesc);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genGetVTableAddr(ObjectType* type) {
    Opnd* dst = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(type));
    Inst* inst = instFactory.makeGetVTableAddr(dst, type);
    insertInst(inst);
    return inst;
}

Inst* 
SimplifierWithInstFactory::genCompressRef(Opnd* uncompref)
{
    Type* uncompRefType = uncompref->getType();
    Type* compRefType = typeManager.compressType(uncompRefType);
    Opnd* dst = opndManager.createSsaTmpOpnd(compRefType);
    Inst* inst = instFactory.makeCompressRef(dst, uncompref);
    insertInst(inst);
    return inst;
}

Inst* 
SimplifierWithInstFactory::genUncompressRef(Opnd* compref)
{
    Type* compRefType = compref->getType();
    Type* uncompRefType = typeManager.uncompressType(compRefType);
    Opnd* dst = opndManager.createSsaTmpOpnd(uncompRefType);
    Inst* inst = instFactory.makeUncompressRef(dst, compref);
    insertInst(inst);
    return inst;
}


Inst*
SimplifierWithInstFactory::genLdFieldOffsetPlusHeapbase(FieldDesc* fd) {
    Opnd* dst = opndManager.createSsaTmpOpnd(typeManager.getOffsetPlusHeapbaseType());
    Inst* inst = instFactory.makeLdFieldOffsetPlusHeapbase(dst, fd);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genLdArrayBaseOffsetPlusHeapbase(Type* elemType) {
    Opnd* dst = opndManager.createSsaTmpOpnd(typeManager.getOffsetPlusHeapbaseType());
    Inst* inst = instFactory.makeLdArrayBaseOffsetPlusHeapbase(dst, elemType);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genLdArrayLenOffsetPlusHeapbase(Type* elemType) {
    Opnd* dst = opndManager.createSsaTmpOpnd(typeManager.getOffsetPlusHeapbaseType());
    Inst* inst = instFactory.makeLdArrayLenOffsetPlusHeapbase(dst, elemType);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genAddOffsetPlusHeapbase(Type *ptrType,
                                                    Opnd *compRef,
                                                    Opnd *offsetPlusHeapbase) {
    Opnd* dst = opndManager.createSsaTmpOpnd(ptrType);
    Inst* inst = instFactory.makeAddOffsetPlusHeapbase(dst, compRef,
                                                       offsetPlusHeapbase);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genTauSafe() {
    if (tauSafeOpnd) {
        return tauSafeOpnd->getInst();
    }
    Opnd* dst = opndManager.createSsaTmpOpnd(typeManager.getTauType());
    Inst* inst = instFactory.makeTauSafe(dst);
    insertInstInHeader(inst);
    tauSafeOpnd = dst;
    return inst;
}

Inst*
SimplifierWithInstFactory::genTauMethodSafe() {
    if (tauMethodSafeOpnd) {
        return tauMethodSafeOpnd->getInst();
    }
    Opnd* dst = opndManager.createSsaTmpOpnd(typeManager.getTauType());
    Inst* inst = instFactory.makeTauPoint(dst);
    insertInstInHeader(inst);
    tauMethodSafeOpnd = dst;
    return inst;
}

Inst*
SimplifierWithInstFactory::genTauUnsafe() {
    if (tauUnsafeOpnd) {
        return tauUnsafeOpnd->getInst();
    }
    Opnd* dst = opndManager.createSsaTmpOpnd(typeManager.getTauType());
    Inst* inst = instFactory.makeTauUnsafe(dst);
    insertInstInHeader(inst);
    tauUnsafeOpnd = dst;
    return inst;
}

Inst*
SimplifierWithInstFactory::genTauStaticCast(Opnd *src, Opnd *tauCheckedCast, Type *castType)
{
    Opnd* dst = opndManager.createSsaTmpOpnd(castType);
    Inst* inst = instFactory.makeTauStaticCast(dst, src, tauCheckedCast, castType);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genTauHasType(Opnd *src, Type *castType)
{
    Opnd* dst = opndManager.createSsaTmpOpnd(typeManager.getTauType());
    Inst* inst = instFactory.makeTauHasType(dst, src, castType);
    insertInst(inst);
    return inst;
}

Inst*
SimplifierWithInstFactory::genTauHasExactType(Opnd *src, Type *castType)
{
    Opnd* dst = opndManager.createSsaTmpOpnd(typeManager.getTauType());
    Inst* inst = instFactory.makeTauHasExactType(dst, src, castType);
    insertInst(inst);
    return inst;
}


Inst*
SimplifierWithInstFactory::genTauIsNonNull(Opnd *src)
{
    Opnd* dst = opndManager.createSsaTmpOpnd(typeManager.getTauType());
    Inst* inst = instFactory.makeTauIsNonNull(dst, src);
    insertInst(inst);
    return inst;
}


Opnd* 
Simplifier::simplifyTauCheckCast(Opnd* src, Opnd* tauCheckedNull, Type* castType,
                                 bool &alwaysThrows)
{
    Type *opndType = src->getType();
    if (isNullObject(src)) {
        return genTauSafe()->getDst();
    } else if (opndType->isUnresolvedType()) {
        return NULL;
    } else if (irManager.getTypeManager().isResolvedAndSubClassOf(opndType, castType)) {
        return genTauHasType(src, castType)->getDst();
    } else if (!irManager.getTypeManager().isResolvedAndSubClassOf(castType, opndType)) {
        if (Log::isEnabled()) {
            Log::out() << "in simplifyTauCheckCast: castToType ";
            castType->print(Log::out());
            Log::out() << " not subtype of source ";
            src->print(Log::out());
            Log::out() << " type ";
            opndType->print(Log::out()); 
            Log::out() << ::std::endl;
        }
        return NULL;
    }
    return NULL;
}

Opnd* 
Simplifier::simplifyTauHasType(Opnd* src, Type* castType)
{
    // all references have type java/lang/Object
    if ((castType == irManager.getTypeManager().getSystemObjectType()) ||
            (castType == irManager.getTypeManager().getCompressedSystemObjectType())) {
        return genTauSafe()->getDst();
    }
    // otherwise, check for constants or casts
#ifndef NDEBUG
    Type *opndType = src->getType();
#endif
    Inst *srcInst = src->getInst();
    Opcode opc = srcInst->getOpcode();
    switch (opc) {
    case Op_NewObj:
    case Op_NewArray:
    case Op_NewMultiArray:
    case Op_TauLdInd:
    case Op_TauLdField:
    case Op_TauLdElem:
    case Op_LdStatic:
    case Op_LdConstant: // must be ldNull; we have no other object constants
        return genTauSafe()->getDst();
    case Op_TauStaticCast:
        {
            TypeInst *staticCastInst = srcInst->asTypeInst();
            Type *typeInfo = staticCastInst->getTypeInfo();
            assert(typeInfo == opndType);
            Opnd *tauCastChecked = staticCastInst->getSrc(1);
            
            // if we have an exact type match, use src
            if (typeInfo == castType) {
                return tauCastChecked;
            }
            // otherwise, first check whether we can be more precise than this cast
        DeadCodeEliminator::copyPropagate(staticCastInst);
            Opnd *staticCastSrc = staticCastInst->getSrc(0);
            tauCastChecked = staticCastInst->getSrc(1);
        
            Opnd *foundRecurse = simplifyTauHasType(staticCastSrc, castType);
            if (foundRecurse) {
                return foundRecurse;
            }
            // couldn't be more precise, just use this one.
            if (irManager.getTypeManager().isResolvedAndSubClassOf(typeInfo, castType)) {
                return tauCastChecked;
            }
        }
        break;
    case Op_Copy:
        assert(0); // should have been copy-propagated
    case Op_LdVar:
    default:
        break;
    }
    return NULL;
}

Opnd* 
Simplifier::simplifyTauHasExactType(Opnd* src, Type* castType)
{
    Inst *srcInst = src->getInst();
    Opcode opc = srcInst->getOpcode();
    if ((opc == Op_NewObj) || (opc == Op_NewArray) || (opc == Op_NewMultiArray)) {
        return genTauSafe()->getDst();
    }
    return NULL;
}

Opnd* 
Simplifier::simplifyTauIsNonNull(Opnd* src)
{
    Inst *srcInst = src->getInst();
    Opcode opc = srcInst->getOpcode();
    if ((opc == Op_NewObj) || (opc == Op_NewArray) || (opc == Op_NewMultiArray)) {
        return genTauSafe()->getDst();
    }
    return NULL;
}

Opnd*
Simplifier::simplifyTauStaticCast(Opnd *src, Opnd *tauCheckedCast, Type *castType)
{
    return NULL;
}


void
SimplifierWithInstFactory::genThrowSystemException(CompilationInterface::SystemExceptionId exceptionId)
{
    Inst* inst = instFactory.makeThrowSystemException(exceptionId);
    insertInst(inst);
}


static Class_Handle getClassHandle(Opnd* opnd) {
    //class handle can be: unmanaged ptr (from magics) or pointer_size_int const (I_32 or int64) if loaded as a const
    //assert(opnd->getType()->isUnmanagedPtr() || opnd->getType()->isInt4() || opnd->getType()->isInt8());
    assert(opnd->getType()->isUnmanagedPtr());
    Inst* inst = opnd->getInst();
    Class_Handle ch = NULL;
    if (inst->asConstInst() != NULL) {
        ch = (Class_Handle)(POINTER_SIZE_INT)inst->asConstInst()->getValue().i8;
    }
    return ch;
}

Inst* Simplifier::simplifyJitHelperCall(JitHelperCallInst* inst) {
    Inst* res = inst;
    Class_Handle ch = NULL;
    TypeManager& tm = irManager.getTypeManager();
    switch(inst->getJitHelperId()) {
        case ClassIsArray:
            ch = getClassHandle(inst->getSrc(0));
            if (ch) {
                res = genLdConstant((I_32)VMInterface::isArrayType(ch));
            }
            break;
        case ClassGetAllocationHandle:
            ch = getClassHandle(inst->getSrc(0));
            if (ch) {
                ConstInst::ConstValue v;
                v.i8 = (POINTER_SIZE_SINT)VMInterface::getAllocationHandle(ch);
                res = genLdConstant(tm.getInt32Type(), v);
                assert((sizeof(void*) == 4) && "TODO fix allocation helper on 64 bit");
            }
            break;
        case ClassGetTypeSize:
            ch = getClassHandle(inst->getSrc(0));
            if (ch) {
                res = genLdConstant((I_32)VMInterface::getObjectSize(ch));
            }
            break;
        case ClassGetArrayElemSize:
            ch = getClassHandle(inst->getSrc(0));
            if (ch) {
                res = genLdConstant((I_32)VMInterface::getArrayElemSize(ch));
            }
            break;
        case ClassIsInterface:
            ch = getClassHandle(inst->getSrc(0));
            if (ch) {
                res = genLdConstant((I_32)VMInterface::isInterfaceType(ch));
            }
            break;
        case ClassIsFinal:
            ch = getClassHandle(inst->getSrc(0));
            if (ch) {
                res = genLdConstant((I_32)VMInterface::isFinalType(ch));
            }
            break;
        case ClassGetArrayClass:
            ch = getClassHandle(inst->getSrc(0));
            if (ch) {
                ConstInst::ConstValue v;
                v.i8 = (POINTER_SIZE_SINT)VMInterface::getArrayVMTypeHandle(ch, false);
                res = genLdConstant(tm.getUnmanagedPtrType(tm.getInt8Type()), v);
            }
            break;
        case ClassIsFinalizable:
            ch = getClassHandle(inst->getSrc(0));
            if (ch) {
                res = genLdConstant((I_32)VMInterface::isFinalizable(ch));
            }
            break;
        case ClassGetFastCheckDepth:
            ch = getClassHandle(inst->getSrc(0));
            if (ch) {
                int depth = 0;
                if (VMInterface::getClassFastInstanceOfFlag(ch)) {
                    depth = (I_32)VMInterface::getClassDepth(ch);
                }
                res = genLdConstant(depth);
            }
            break;
        default: break;
    }
    return res;
}

} //namespace Jitrino 
