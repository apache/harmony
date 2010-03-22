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

#include <assert.h>
#include <iostream>
#include <algorithm>
#include "Stl.h"
#include "Log.h"
#include "open/types.h"
#include "Inst.h"
#include "irmanager.h"
#include "reassociate.h"
#include "CSEHash.h"
#include "Opcode.h"
#include "constantfolder.h"
#include "simplifier.h"
#include "FlowGraph.h"

namespace Jitrino {

DEFINE_SESSION_ACTION(ReassociationPass, reassoc, "Reassociation")

void
ReassociationPass::_run(IRManager& irm) {
    Reassociate pass(irm, irm.getNestedMemoryManager());
    pass.runPass(false);
}

DEFINE_SESSION_ACTION(DepthReassociationPass, reassocdepth, "Reassociation with Minimum Depth")

void
DepthReassociationPass::_run(IRManager& irm) {
    Reassociate pass(irm, irm.getNestedMemoryManager());
    pass.runPass(true);
}

DEFINE_SESSION_ACTION(LateDepthReassociationPass, latereassocdepth, "Reassociation with Minimum Depth + Constant Mul/Div Optimization")

void
LateDepthReassociationPass::_run(IRManager& irm) {
    Reassociate pass(irm, irm.getNestedMemoryManager());
    pass.runPass(true, true);
}

inline U_32 computePriority(U_32 op_cost, U_32 priority0) {
    if (priority0 == 0) return 0;
    else return op_cost + priority0;
}
inline U_32 computePriority(U_32 op_cost, U_32 priority0, U_32 priority1) {
    if (priority0 == 0) return 0;
    else return op_cost + ::std::max(priority0, priority1);
}
inline U_32 computePriority(U_32 op_cost, U_32 priority0, U_32 priority1, U_32 priority2) {
    if ((priority0 == 0) && (priority1 == 0) && (priority2 == 0)) return 0;
    else return op_cost + ::std::max(::std::max(priority0, priority1), priority2);
}

inline bool operator < (const OpndWithPriority &a, const OpndWithPriority &b)
{
    if (a.priority < b.priority) return true;
    if (a.priority == b.priority) {
        if (b.negate && !a.negate) return true;
    }
    return false;
}

Reassociate::Reassociate(IRManager &irManager0, 
             MemoryManager& memManager)
    : irManager(irManager0), 
      mm(memManager),
      cfgRpoNum(memManager),
      priority(memManager),
      minDepth(false), 
      maxReassocDepth(64)
{
}

Reassociate::~Reassociate()
{
}


U_32 Reassociate::getPriority(Opnd *opnd)
{
    if (priority.has(opnd)) return priority[opnd];
    Inst *inst = opnd->getInst();
    U_32 opriority = 0;
    Operation operation = inst->getOperation();
    Opcode opc = operation.getOpcode();
    switch (opc) {
    case Op_LdConstant:
    opriority = 0; break;
    case Op_Copy:
    opriority = getPriority(inst->getSrc(0)); break;
    case Op_LdVar:
    {
        Opnd *var = inst->getSrc(0);
        SsaVarOpnd *ssaVarOpnd = var->asSsaVarOpnd();
        assert(ssaVarOpnd);
        opriority = getPriority(ssaVarOpnd);
    }
    break;
    case Op_Add:
        opriority = computePriority(costOfAdd, getPriority(inst->getSrc(0)), 
                                    getPriority(inst->getSrc(1)));
    break;
    case Op_Sub:
    opriority = computePriority(costOfSub, getPriority(inst->getSrc(0)), 
                                    getPriority(inst->getSrc(1)));
    break;
    case Op_Neg:
    opriority = computePriority(costOfNeg, getPriority(inst->getSrc(0)));
    break;
    case Op_Mul: 
    opriority = computePriority(costOfMul, getPriority(inst->getSrc(0)), 
                                    getPriority(inst->getSrc(1)));
    break;
    case Op_Conv: 
    opriority = computePriority(costOfConv, getPriority(inst->getSrc(0)));
    break;
    case Op_And:
    case Op_Or:
    case Op_Xor:
    opriority = computePriority(costOfBoolOp, getPriority(inst->getSrc(0)), 
                                    getPriority(inst->getSrc(1)));
    break;
    case Op_Not:
    opriority = computePriority(costOfNot, getPriority(inst->getSrc(0)));
    break;
    case Op_Shladd:
    opriority = computePriority(costOfShladd,getPriority(inst->getSrc(0)), 
                                    getPriority(inst->getSrc(1)),
                                    getPriority(inst->getSrc(2)));
    break;
    case Op_Shr:
    case Op_Shl:
    opriority = computePriority(costOfShift, getPriority(inst->getSrc(0)), 
                                    getPriority(inst->getSrc(1)));
    break;
    default:
    if (operation.isMovable()) {
        U_32 numOpnds = inst->getNumSrcOperands();
        opriority = 0;

        for (U_32 i = 0; i<numOpnds; i++) {
                Opnd *opndi = inst->getSrc(i);
                U_32 argpriority = getPriority(opndi);
                opriority = ::std::max(opriority, argpriority);
        }
        opriority = computePriority(costOfMisc, opriority);
    } else {
        Inst *previ = inst->getPrevInst();
        U_32 ipriority = 1;
        while (!previ->isLabel()) {
        ++ipriority;
        previ = previ->getPrevInst();
        }
        Node *block = previ->asLabelInst()->getNode();
        U_32 bpriority = cfgRpoNum[block];
        opriority = ((bpriority * priorityFactorOfBlock)
                         + ipriority)*priorityFactorOfPosition;
    }
    break;
    }
    if (Log::isEnabled()) {
        Log::out() << "Priority of ";
        opnd->print(Log::out());
        Log::out() << " computed is " << (int) opriority << ::std::endl;
    }
    priority[opnd] = opriority;
    return opriority;
}

void Reassociate::runPass(bool minimizeDepth, bool isLate)
{
    minDepth = minimizeDepth;
    if (minimizeDepth) {
    costOfAdd = 1;
    costOfSub = 1;
    costOfNeg = 1;
    costOfMul = 15;
    costOfConv = 1;
    costOfBoolOp = 1;
    costOfNot = 1;
    costOfShladd = 1;
    costOfShift = 1;
    costOfMisc = 1; 
    priorityFactorOfBlock = 64;
    priorityFactorOfPosition = 2;
    } else {
    costOfAdd = 0;
    costOfSub = 0;
    costOfNeg = 0;
    costOfMul = 0;
    costOfConv = 0;
    costOfBoolOp = 0;
    costOfNot = 0;
    costOfShladd = 0;
    costOfShift = 0;
    costOfMisc = 0; 
    priorityFactorOfBlock = 64;
    priorityFactorOfPosition = 1;
    }

    if (Log::isEnabled()) {
        Log::out() << "IR before Reassociation pass" << ::std::endl;
        FlowGraph::printHIR(Log::out(),  irManager.getFlowGraph(), irManager.getMethodDesc());
        FlowGraph::printDotFile(irManager.getFlowGraph(), irManager.getMethodDesc(), "beforereassoc");
    }

    // first, assign Reverse-PostOrder numbers to flowGraph nodes
    StlVector<Node *> postOrderNodes(mm);
    ControlFlowGraph &fg = irManager.getFlowGraph();
    fg.getNodesPostOrder(postOrderNodes);
    StlVector<Node *>::reverse_iterator 
    riter = postOrderNodes.rbegin(),
    rend = postOrderNodes.rend();
    U_32 i = 0;
    while (riter != rend) {
    Node *node = *riter;
    if (Log::isEnabled()) {
        Log::out() << "RPO[";
        FlowGraph::printLabel(Log::out(), node);
        Log::out() << "] = " << (int) i;
        Log::out() << ::std::endl;
    }
    cfgRpoNum[node] = i++;
    ++riter;
    }
    numBlocks = i;
    
    SimplifierWithInstFactory simplifier(irManager, isLate, this);
    theSimp = &simplifier;
    simplifier.simplifyControlFlowGraph();
    
    if (Log::isEnabled()) {
        Log::out() << "IR after Reassociation pass" << ::std::endl;
        FlowGraph::printHIR(Log::out(),  irManager.getFlowGraph(), irManager.getMethodDesc());
        FlowGraph::printDotFile(irManager.getFlowGraph(), irManager.getMethodDesc(), "afterreassoc");
    }
}

void
Reassociate::addAddAssoc(StlDeque<OpndWithPriority> &opnds, 
                         bool negated,
             Type* type, Opnd* opnd)
{
    Inst *inst = opnd->getInst();
    if (inst->getType() == type->tag && opnds.size() < maxReassocDepth) {
    switch(inst->getOpcode()) {
    case Op_Add:
        if (inst->getOverflowModifier() != Overflow_None) break;
            if (Log::isEnabled()) {
                Log::out() << "addAddAssoc of operand ";
                opnd->print(Log::out());
                Log::out() << " is Add(";
                inst->getSrc(0)->print(Log::out());
                Log::out() << ", ";
                inst->getSrc(1)->print(Log::out());
                Log::out() << "), checking opnds" << ::std::endl;
            }
        addAddAssoc(opnds, negated, type, inst->getSrc(0));
        addAddAssoc(opnds, negated, type, inst->getSrc(1));
        return;
    case Op_Neg:
            if (Log::isEnabled()) {
                Log::out() << "addAddAssoc of operand ";
                opnd->print(Log::out());
                Log::out() << " is Neg(";
                inst->getSrc(0)->print(Log::out());
                Log::out() << "), checking opnd" << ::std::endl;
            }
        addAddAssoc(opnds, !negated, type, inst->getSrc(0));
        return;
    case Op_Sub:
        if (inst->getOverflowModifier() != Overflow_None) break;
            if (Log::isEnabled()) {
                Log::out() << "addAddAssoc of operand ";
                opnd->print(Log::out());
                Log::out() << " is Sub(";
                inst->getSrc(0)->print(Log::out());
                Log::out() << ", ";
                inst->getSrc(1)->print(Log::out());
                Log::out() << "), checking opnds" << ::std::endl;
            }
        addAddAssoc(opnds, negated, type, inst->getSrc(0));
        addAddAssoc(opnds, !negated, type, inst->getSrc(1));
        return;
        case Op_Copy:
            if (Log::isEnabled()) {
                Log::out() << "addAddAssoc of operand ";
                opnd->print(Log::out());
                Log::out() << " is Copy(";
                inst->getSrc(0)->print(Log::out());
                Log::out() << "), checking opnd" << ::std::endl;
            }
        addAddAssoc(opnds, negated, type, inst->getSrc(0));
        return;
    default:
        break;
    }
    }
    if (Log::isEnabled()) {
        Log::out() << "addAddAssoc of operand ";
        opnd->print(Log::out());
        Log::out() << " is terminal, adding" << ::std::endl;
    }
    opnds.push_back(OpndWithPriority(opnd, getPriority(opnd), negated));
}

void
Reassociate::addMulAssoc(StlDeque<OpndWithPriority> &opnds, 
                         bool negated,
             Type* type, Opnd* opnd)
{
    Inst *inst = opnd->getInst();
    if (inst->getType() == type->tag  && opnds.size() < maxReassocDepth) {
    switch(inst->getOpcode()) {
    case Op_Mul:
        if (inst->getOverflowModifier() != Overflow_None) break;
        addMulAssoc(opnds, negated, type, inst->getSrc(0));
        addMulAssoc(opnds, negated, type, inst->getSrc(1));
        return;
        case Op_Copy:
            addAddAssoc(opnds, negated, type, inst->getSrc(0));
        return;
    default:
        break;
    }
    }
    opnds.push_back(OpndWithPriority(opnd, getPriority(opnd), negated));
}

void
Reassociate::addAddOffsetAssoc(StlDeque<OpndWithPriority> &opnds, 
                               bool compressed,
                               Type* type, Opnd* opnd)
{
    Inst *inst = opnd->getInst();
    if (inst->getType() == type->tag  && opnds.size() < maxReassocDepth) {
    switch(inst->getOpcode()) {
    case Op_AddOffset:
        addAddOffsetAssoc(opnds, compressed, type, inst->getSrc(0));
        addAddOffsetAssoc(opnds, compressed, type, inst->getSrc(1));
        return;
    case Op_AddOffsetPlusHeapbase:
        addAddOffsetAssoc(opnds, compressed, type, inst->getSrc(0));
        addAddOffsetAssoc(opnds, compressed, type, inst->getSrc(1));
        return;
        case Op_UncompressRef:
        case Op_CompressRef:
        case Op_Copy:
        case Op_LdFieldOffset:
        case Op_LdFieldOffsetPlusHeapbase:
        case Op_LdArrayBaseOffsetPlusHeapbase:
        case Op_LdArrayLenOffset:
        case Op_LdArrayLenOffsetPlusHeapbase:
        case Op_AddScaledIndex:
        case Op_TauArrayLen:
    default:
        break;
    }
    }
    opnds.push_back(OpndWithPriority(opnd, getPriority(opnd), compressed));
}


// maybe this stuff should be in simplifier.
Opnd*
Reassociate::simplifyReassociatedAdd(Type *type, 
                     StlDeque<OpndWithPriority> &opnds)
{
    typedef StlDeque<OpndWithPriority> VectType;

    ::std::sort(opnds.begin(), opnds.end());

    if (Log::isEnabled()) {
    VectType::iterator 
        iter = opnds.begin(),
        end = opnds.end();
    Log::out() << "BEGIN simplifyReassociatedAdd: ";
    for ( ; iter != end ; iter++) {
        OpndWithPriority &owd = *iter;
        Log::out() << (owd.negate ? "-" : "+") << "(";
        owd.opnd->print(Log::out());
        Log::out() << ", " << (int)owd.priority << ") ";
    }
    Log::out() << ::std::endl;
    }

    // algorithm:
    //   first, if all opnds are negated, wantToNegate=true
    //   while (opnds.size > 1)
    //      if (opnds[0].negate != opnds[1].negate) {
    //        if (opnds[0].negate)
    //           newop = Op_Sub(opnds[1], opnds[0]);
    //        else
    //           newop = Op_Sub(opnds[0], opnds[1]);
    //      else
    //        if (wantToNegate && (computePriority(costofNeg, opnds[0].priority) < opnds[1].priority)
    //            newop = Op_Sub(Op_Neg(opnds[0]), opnds[1])
    //            wantToNegate = false
    //        else
    //            newop = Op_Add(opnds[0], opnds[1]);

    VectType::size_type size = opnds.size();

    if (size < 2) { return 0; } 

    bool wantToNegate = true;
    VectType::iterator 
        iter = opnds.begin(),
        end = opnds.end();
    for ( ; iter != end; ++iter) {
        if (!(*iter).negate) wantToNegate=false;
    }

    if ((!wantToNegate) && (size < 3)) { return 0; } 

    while (size > 1) {
        OpndWithPriority owd0 = opnds[0]; 
        OpndWithPriority owd1 = opnds[1];
        opnds.pop_front();
        opnds.pop_front();
        size -= 2;
        Opnd *opnd0 = owd0.opnd;
        Opnd *opnd1 = owd1.opnd;
        Opnd *newOpnd = 0;
        bool newNegated = 0;
        U_32 priority0 = owd0.priority;
        U_32 priority1 = owd1.priority;
        const char *debug_op = "";
        bool debug_swapped = false;
        U_32 newPriority = 0;
        if (owd0.negate != owd1.negate) {
            if (owd0.negate) {
                newOpnd = theSimp->genSub(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                          opnd1, opnd0)->getDst();
                newPriority = computePriority(costOfSub, priority1, priority0);
                newNegated = false;
                debug_swapped = true;
            } else {
                newOpnd = theSimp->genSub(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                                opnd0, opnd1)->getDst();
                newPriority = computePriority(costOfSub, priority0, priority1);
                newNegated = false;
            }
            debug_op = "sub";
        } else {
            if (wantToNegate && (computePriority(costOfNeg, priority0) < priority1)) {
                // we can effort to negate here, negate deeper argument
                Opnd *negated = theSimp->genNeg(type, opnd0)->getDst();
                newOpnd = theSimp->genSub(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                          negated, opnd1)->getDst();
                newNegated = false;
                wantToNegate = false;
                newPriority = computePriority(costOfSub, computePriority(costOfNeg, priority0), priority1);
                debug_op = "negsub";
            } else {
                newNegated = owd0.negate;
                newOpnd = theSimp->genAdd(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                          opnd0, opnd1)->getDst();
                newPriority = computePriority(costOfAdd, priority0, priority1);
                debug_op = "add";
            }
        }
        OpndWithPriority newelt(newOpnd, newPriority, newNegated);
        opnds.push_front(newelt);
        size += 1;
        if (Log::isEnabled()) {
            Log::out() << "    new " << (newNegated ? "-" : "+") << "(";
            newOpnd->print(Log::out());
            Log::out() << ", " << (int) newPriority << ") = " << debug_op << "(";
            if (debug_swapped) {
                opnd1->print(Log::out());
            } else {
                opnd0->print(Log::out());
            }
            Log::out() << ", ";
            if (debug_swapped) {
                opnd0->print(Log::out());
            } else {
                opnd1->print(Log::out());
            }
            Log::out() << ")" << ::std::endl;
        }
    }

    assert(size == 1);
    Opnd *res = opnds[0].opnd;
    if (wantToNegate) {
        assert(opnds[0].negate);
        // need to neg result
        Opnd *newres = theSimp->genNeg(type, res)->getDst();
        if (Log::isEnabled()) {
            Log::out() << "    negated: ";
            newres->print(Log::out());
            Log::out() << " = neg(";
            res->print(Log::out());
            Log::out() << ")" << ::std::endl;
        }
    res = newres;
    } else {
        assert(!opnds[0].negate);
    }
    if (Log::isEnabled()) {
    Log::out() << "FINISHED simplifyReassociatedAdd: ";
    res->print(Log::out());
    Log::out() << ::std::endl;
    }
    return res;
}

Opnd*
Reassociate::simplifyReassociatedMul(Type *type, 
                     StlDeque<OpndWithPriority> &opnds)
{
    typedef StlDeque<OpndWithPriority> VectType;
    if (Log::isEnabled()) {
    VectType::iterator 
        iter = opnds.begin(),
        end = opnds.end();
    Log::out() << "BEGIN simplifyReassociatedMul: ";
    for ( ; iter != end ; iter++) {
        OpndWithPriority &owd = *iter;
        Log::out() << "(";
        owd.opnd->print(Log::out());
        Log::out() << ", " << (int)owd.priority << ") ";
    }
    Log::out() << ::std::endl;
    }

    ::std::sort(opnds.begin(), opnds.end());
    
    // general algorithm: for priority reduction, we just repeatedly reduce
    //   2 highest-priority operands

    VectType::size_type opndsize = opnds.size();
    while (opndsize > 1) {
        OpndWithPriority owd0 = opnds[0]; 
        OpndWithPriority owd1 = opnds[1];
        Opnd *opnd0 = owd0.opnd;
        U_32 priority0 = owd0.priority;
        bool neg0 = owd0.negate;
        Opnd *opnd1 = owd1.opnd;
        U_32 priority1 = owd1.priority;
        bool neg1 = owd1.negate;

        if ((opnd0 == opnd1) && (opndsize > 3)
            && (opnds[2].opnd == opnds[3].opnd)) {
            // we have (a * a * b * b)
            // convert to (a*b)*(a*b)

            OpndWithPriority owd2 = opnds[2]; 
            OpndWithPriority owd3 = opnds[3];
            Opnd *opnd2 = owd2.opnd;
            U_32 priority2 = owd2.priority;

            Opnd *newOpnd0 = theSimp->genMul(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                             opnd0, opnd2)->getDst();
            Opnd *newOpnd1 = theSimp->genMul(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                             newOpnd0, newOpnd0)->getDst();
            opnds.pop_front();
            opnds.pop_front();
            opnds.pop_front();
            U_32 priorityNew0 = computePriority(costOfMul, priority0,
                                                  priority2);
            U_32 priorityNew1 = computePriority(costOfMul, 
                                                  priorityNew0,
                                                  priorityNew0);
            OpndWithPriority newelt(newOpnd1, priorityNew1, 
                                    false // negs negate each other
                                    ); 
            
            if (Log::isEnabled()) {
                Log::out() << "    new (";
                newOpnd1->print(Log::out());
                Log::out() << ", " << (int) priorityNew1 << ") = sqr(mul(";
                opnd0->print(Log::out());
                Log::out() << ", " << (int) priority0 << "), (";
                opnd2->print(Log::out());
                Log::out() << ", " << (int) priority2 << "))" << ::std::endl;
            }
            
            opnds[0] = newelt;
            opndsize -= 3;
            U_32 i = 1;
            while (i < opndsize) {
                if (opnds[i].priority < priorityNew1) {
                    opnds[i-1] = opnds[i];
                    opnds[i] = newelt;
                } else {
                    break;
                }
            }
        } else {

            Opnd *newOpnd = theSimp->genMul(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                            opnd0, opnd1)->getDst();
            opnds.pop_front();
            U_32 priority = computePriority(costOfMul, priority0, priority1);
            OpndWithPriority newelt(newOpnd, priority, neg0 ^ neg1);
            
            if (Log::isEnabled()) {
                Log::out() << "    new (";
                newOpnd->print(Log::out());
                Log::out() << ", " << (int) priority << ") = mul((";
                opnd0->print(Log::out());
                Log::out() << ", " << (int) priority0 << "), (";
                opnd1->print(Log::out());
                Log::out() << ", " << (int) priority1 << "))" << ::std::endl;
            }
            
            opnds[0] = newelt;
            opndsize -= 1;
            U_32 i = 1;
            while (i < opndsize) {
                if (opnds[i].priority < priority) {
                    opnds[i-1] = opnds[i];
                    opnds[i] = newelt;
                } else {
                    break;
                }
            }
    }
    }
    if (Log::isEnabled()) {
    VectType::iterator 
        iter = opnds.begin(),
        end = opnds.end();
    Log::out() << "FINISHED simplifyReassociatedMul: ";
    opnds[0].opnd->print(Log::out());
    Log::out() << ::std::endl;
    }
    return opnds[0].opnd;
}

Opnd*
Reassociate::simplifyReassociatedAddOffset(Type *type, 
                                           StlDeque<OpndWithPriority> &opnds)
{
    typedef StlDeque<OpndWithPriority> VectType;

    ::std::sort(opnds.begin(), opnds.end());

    if (Log::isEnabled()) {
    VectType::iterator 
        iter = opnds.begin(),
        end = opnds.end();
    Log::out() << "BEGIN simplifyReassociatedAddOffset: ";
    for ( ; iter != end ; iter++) {
        OpndWithPriority &owd = *iter;
        Log::out() << (owd.negate ? "-" : "+") << "(";
        owd.opnd->print(Log::out());
        Log::out() << ", " << (int)owd.priority << ") ";
    }
    Log::out() << ::std::endl;
    }

    assert(0);

    // algorithm:
    //   first, if all opnds are negated, wantToNegate=true
    //   while (opnds.size > 1)
    //      if (opnds[0].negate != opnds[1].negate) {
    //        if (opnds[0].negate)
    //           newop = Op_Sub(opnds[1], opnds[0]);
    //        else
    //           newop = Op_Sub(opnds[0], opnds[1]);
    //      else
    //        if (wantToNegate && (computePriority(costofNeg, opnds[0].priority) < opnds[1].priority)
    //            newop = Op_Sub(Op_Neg(opnds[0]), opnds[1])
    //            wantToNegate = false
    //        else
    //            newop = Op_Add(opnds[0], opnds[1]);

    VectType::size_type size = opnds.size();

    if (size < 2) { return 0; } // don't rewrite, no point

    bool wantToNegate = true;
    VectType::iterator 
        iter = opnds.begin(),
        end = opnds.end();
    for ( ; iter != end; ++iter) {
        if (!(*iter).negate) wantToNegate=false;
    }

    if ((!wantToNegate) && (size < 3)) { return 0; } // don't bother

    while (size > 1) {
        OpndWithPriority owd0 = opnds[0]; 
        OpndWithPriority owd1 = opnds[1];
        opnds.pop_front();
        opnds.pop_front();
        size -= 2;
        Opnd *opnd0 = owd0.opnd;
        Opnd *opnd1 = owd1.opnd;
        Opnd *newOpnd = 0;
        bool newNegated = 0;
        U_32 priority0 = owd0.priority;
        U_32 priority1 = owd1.priority;
        const char *debug_op = "";
        bool debug_swapped = false;
        U_32 newPriority = 0;
        if (owd0.negate != owd1.negate) {
            if (owd0.negate) {
                newOpnd = theSimp->genSub(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                          opnd1, opnd0)->getDst();
                newPriority = computePriority(costOfSub, priority1, priority0);
                newNegated = false;
                debug_swapped = true;
            } else {
                newOpnd = theSimp->genSub(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                                opnd0, opnd1)->getDst();
                newPriority = computePriority(costOfSub, priority0, priority1);
                newNegated = false;
            }
            debug_op = "sub";
        } else {
            if (wantToNegate && (computePriority(costOfNeg, priority0) < priority1)) {
                // we can effort to negate here, negate deeper arguments
                Opnd *negated = theSimp->genNeg(type, opnd0)->getDst();
                newOpnd = theSimp->genSub(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                          negated, opnd1)->getDst();
                newNegated = false;
                wantToNegate = false;
                newPriority = computePriority(costOfSub, computePriority(costOfNeg, priority0), priority1);
                debug_op = "negsub";
            } else {
                newNegated = owd0.negate;
                newOpnd = theSimp->genAdd(type, Modifier(Overflow_None)|Modifier(Exception_Never)|Modifier(Strict_No),
                                          opnd0, opnd1)->getDst();
                newPriority = computePriority(costOfAdd, priority0, priority1);
                debug_op = "add";
            }
        }
        OpndWithPriority newelt(newOpnd, newPriority, newNegated);
        opnds.push_front(newelt);
        size += 1;
        if (Log::isEnabled()) {
            Log::out() << "    new " << (newNegated ? "-" : "+") << "(";
            newOpnd->print(Log::out());
            Log::out() << ", " << (int) newPriority << ") = " << debug_op << "(";
            if (debug_swapped) {
                opnd1->print(Log::out());
            } else {
                opnd0->print(Log::out());
            }
            Log::out() << ", ";
            if (debug_swapped) {
                opnd0->print(Log::out());
            } else {
                opnd1->print(Log::out());
            }
            Log::out() << ")" << ::std::endl;
        }
    }

    assert(size == 1);
    Opnd *res = opnds[0].opnd;
    if (wantToNegate) {
        assert(opnds[0].negate);
        // need to neg result
        Opnd *newres = theSimp->genNeg(type, res)->getDst();
        if (Log::isEnabled()) {
            Log::out() << "    negated: ";
            newres->print(Log::out());
            Log::out() << " = neg(";
            res->print(Log::out());
            Log::out() << ")" << ::std::endl;
        }
    res = newres;
    } else {
        assert(!opnds[0].negate);
    }
    if (Log::isEnabled()) {
    Log::out() << "FINISHED simplifyReassociatedAdd: ";
    res->print(Log::out());
    Log::out() << ::std::endl;
    }
    return res;
}


// checks for and performs possible re-associations of add
// makes no assumptions about types
Opnd*
Simplifier::simplifyAddViaReassociation2(Type* type, Opnd* src1, Opnd* src2) {
    assert(theReassociate);
    StlDeque<OpndWithPriority> opnds(theReassociate->mm);

    if (Log::isEnabled()) {
        Log::out() << "simplifyAddViaReassociation2 Add(";
        src1->print(Log::out());
        Log::out() << ", ";
        src2->print(Log::out());
        Log::out() << "): " << ::std::endl;
    }

    theReassociate->addAddAssoc(opnds, false, type, src1);
    theReassociate->addAddAssoc(opnds, false, type, src2);
    return theReassociate->simplifyReassociatedAdd(type, opnds);
}

// checks for and performs possible re-associations of neg
// makes no assumptions about types
Opnd*
Simplifier::simplifyNegViaReassociation2(Type* type, Opnd* src1) {
    assert(theReassociate);
    StlDeque<OpndWithPriority> opnds(theReassociate->mm);

    if (Log::isEnabled()) {
        Log::out() << "simplifyNegViaReassociation2 Neg(";
        src1->print(Log::out());
        Log::out() << "): " << ::std::endl;
    }

    theReassociate->addAddAssoc(opnds, true, type, src1);
    return theReassociate->simplifyReassociatedAdd(type, opnds);
}

// checks for and performs possible re-associations of neg
// makes no assumptions about types
Opnd*
Simplifier::simplifySubViaReassociation2(Type* type, Opnd* src1, Opnd *src2) {
    assert(theReassociate);
    StlDeque<OpndWithPriority> opnds(theReassociate->mm);

    if (Log::isEnabled()) {
        Log::out() << "simplifySubViaReassociation2 Sub(";
        src1->print(Log::out());
        Log::out() << ", ";
        src2->print(Log::out());
        Log::out() << "): " << ::std::endl;
    }

    theReassociate->addAddAssoc(opnds, false, type, src1);
    theReassociate->addAddAssoc(opnds, true, type, src2);
    return theReassociate->simplifyReassociatedAdd(type, opnds);
}

// checks for and performs possible re-associations of add
// makes no assumptions about types
Opnd*
Simplifier::simplifyMulViaReassociation2(Type* type, Opnd* src1, Opnd* src2) {
    assert(theReassociate);
    StlDeque<OpndWithPriority> opnds(theReassociate->mm);

    if (Log::isEnabled()) {
        Log::out() << "simplifyMulViaReassociation2 Mul(";
        src1->print(Log::out());
        Log::out() << ", ";
        src2->print(Log::out());
        Log::out() << "): " << ::std::endl;
    }

    theReassociate->addMulAssoc(opnds, false, type, src1);
    theReassociate->addMulAssoc(opnds, false, type, src2);

    return theReassociate->simplifyReassociatedMul(type, opnds);
}

// checks for and performs possible re-associations of addOffset
Opnd*
Simplifier::simplifyAddOffsetViaReassociation(Opnd* uncompBase, 
                                              Opnd* offset) {
    assert(theReassociate);
    StlDeque<OpndWithPriority> opnds(theReassociate->mm);

    if (Log::isEnabled()) {
        Log::out() << "simplifyAddOffsetViaReassociation AddOffset(";
        uncompBase->print(Log::out());
        Log::out() << ", ";
        offset->print(Log::out());
        Log::out() << "): " << ::std::endl;
    }

    theReassociate->addAddOffsetAssoc(opnds, false, uncompBase->getType(), uncompBase);
    theReassociate->addAddOffsetAssoc(opnds, false, uncompBase->getType(), offset);
    return theReassociate->simplifyReassociatedAddOffset(uncompBase->getType(), opnds);
}

// checks for and performs possible re-associations of addOffsetPlusHeapbase
Opnd*
Simplifier::simplifyAddOffsetPlusHeapbaseViaReassociation(Opnd* compBase, 
                                                          Opnd* offsetPlusHeapbase) {
    assert(theReassociate);
    StlDeque<OpndWithPriority> opnds(theReassociate->mm);

    if (Log::isEnabled()) {
        Log::out() << "simplifyAddViaReassociation2 Add(";
        compBase->print(Log::out());
        Log::out() << ", ";
        offsetPlusHeapbase->print(Log::out());
        Log::out() << "): " << ::std::endl;
    }

    Type *type = irManager.getTypeManager().uncompressType(compBase->getType());
    theReassociate->addAddOffsetAssoc(opnds, false,  type, compBase);
    theReassociate->addAddOffsetAssoc(opnds, false, type, offsetPlusHeapbase);
    return theReassociate->simplifyReassociatedAddOffset(type, opnds);
}


} //namespace Jitrino 
