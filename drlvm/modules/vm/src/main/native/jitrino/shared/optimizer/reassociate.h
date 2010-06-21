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

#ifndef _REASSOCIATE_H
#define _REASSOCIATE_H

#include <iostream>
#include "open/types.h"
#include "Opcode.h"
#include "Stl.h"
#include "optpass.h"
#include <utility>

#include "ControlFlowGraph.h"

namespace Jitrino {

class IRManager;
class MemoryManager;
class InequalityGraph;
class DominatorNode;
class Dominator;
class Node;
class Opnd;
class CSEHashTable;
class Type;
class LoopTree;
class Simplifier;

struct OpndWithPriority {
    OpndWithPriority(Opnd *op, U_32 priority0,
                     bool negate0) 
        : opnd(op), priority(priority0), negate(negate0) {};
    Opnd *opnd;
    U_32 priority;
    bool negate;
};
bool operator < (const OpndWithPriority &a, const OpndWithPriority &b);

//
// Try to re-associate expressions to either
//    - reduce expression height
//    - pull loop-invariant subexpressions out
//
class Reassociate {
    IRManager& irManager;
    MemoryManager &mm;
public:
    
private:
public:    
 
    Reassociate(IRManager &irManager0, 
        MemoryManager& memManager);

    ~Reassociate();

    void runPass(bool minimizeDepth, bool isLate=false);
private:
    friend class Simplifier;
    Simplifier *theSimp;

    Opnd* simplifySubViaReassociation(Type* type, Opnd* src1, Opnd* src2);
    Opnd* simplifyNegViaReassociation2(Type* type, Opnd* src1);
    Opnd* simplifySubViaReassociation2(Type* type, Opnd* src1, Opnd *src2);

    void addAddAssoc(StlDeque<OpndWithPriority> &opnds, 
                     bool negated,
             Type* type, Opnd* opnd);
    void addMulAssoc(StlDeque<OpndWithPriority> &opnds, 
                     bool negated,
             Type* type, Opnd* opnd);
    void addAddOffsetAssoc(StlDeque<OpndWithPriority> &opnds, 
                           bool compressed,
                           Type* type, Opnd* opnd);
    
    Opnd* simplifyReassociatedAdd(Type *type, 
                  StlDeque<OpndWithPriority> &opnds);
    Opnd* simplifyReassociatedMul(Type *type, 
                  StlDeque<OpndWithPriority> &opnds);

    Opnd* simplifyMulViaReassociation2(Type* type, Opnd* src1, Opnd* src2);

    Opnd* simplifyAddOffsetViaReassociation(Opnd* src1, 
                                             Opnd* src2);
    Opnd* simplifyAddOffsetPlusHeapbaseViaReassociation(Opnd* src1, 
                                                         Opnd* src2);
    Opnd* simplifyReassociatedAddOffset(Type *type, StlDeque<OpndWithPriority> &opnds);
    
private:
    // we compute Reverse-Postorder numbers for CFGnodes:
    StlHashMap<Node *, U_32> cfgRpoNum;

    StlHashMap<Opnd *, U_32> priority;
    U_32 getPriority(Opnd *opnd); // computes if not in the hash map

    // build a LdConstant(k), put it in header of FlowGraph
    SsaTmpOpnd *makeLdConst(Type *type, ConstInst::ConstValue k, SsaOpnd *dstOp);

    U_32 costOfAdd, costOfSub, 
        costOfNeg, costOfMul, costOfConv, costOfBoolOp,
    costOfNot, costOfShladd, costOfShift, costOfMisc, 
    priorityFactorOfBlock, priorityFactorOfPosition;
    U_32 numBlocks;
    
    bool minDepth;
    U_32 maxReassocDepth;
};

} //namespace Jitrino 

#endif // _REASSOCIATE_H
