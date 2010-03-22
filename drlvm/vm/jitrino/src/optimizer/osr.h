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

#ifndef _OSR_H
#define _OSR_H

#include "LoopTree.h"
#include <iostream>
#include "Opcode.h"
#include "ControlFlowGraph.h"
#include "Stl.h"
#include "Dominator.h"
#include "CSEHash.h"
#include "optpass.h"
#include "Opnd.h"
#include "irmanager.h"
#include "constantfolder.h"
#include <utility>

namespace Jitrino {

class IRManager;
class MemoryManager;
class InequalityGraph;
class Node;
class DominatorNode;
class Dominator;
class CFGNode;
class Opnd;
class CFGInst;
class CSEHashTable;
class Type;
class LoopTree;
class DominatorTree;

enum iv_detection_flag {
    CHOOSE_MAX_IN_BRANCH,
    IGNORE_BRANCH
};

/* Class for holding info on operands.
 * Similar to what loop_unroll uses internally.
 * */

class OSROpndInfo {
public:
    enum OpndType {
        DEF_OUT_OF_LOOP,
        LD_CONST,
        COUNTER,
        UNDEF
    };

    OSROpndInfo() {
        type = DEF_OUT_OF_LOOP;
        val = 0;
        phiSplit = false;
        header = 0;
        header_found = false;
        curOpnd = 0;
    } 
    OpndType getType() const {return type;} 

    void setType(OpndType newType) {
        assert(type != newType);
        type = newType;
        val = 0;
    }

    bool isCounter() const { return type == COUNTER;} 
    bool isLDConst() const {return type == LD_CONST;} 
    bool isDOL() const {return type == DEF_OUT_OF_LOOP;} 
    bool isUndefined() const {return type == UNDEF;} 

    U_32 getConst() const {
        assert(getType() == LD_CONST);
        return val;
    }
    void setConst(U_32 v) {
        assert(getType() == LD_CONST);
        val = v;
    }

    int getIncrement() const {
        assert(getType() == COUNTER);
        return val;
    }
    void setIncrement(int v) {
        assert(getType() == COUNTER);
        val = v;
    }
    bool isPhiSplit() const { return phiSplit;} 
    void markPhiSplit() {
        assert(isCounter());
        phiSplit = true;
    }
    Opnd *getHeader() { return header;}
    void setHeader(Opnd * h) {header = h;}
    bool isHeaderFound() { return header_found;}
    void setHeaderFound() {header_found = true;}
    void setOpnd(Opnd * opnd) {curOpnd = opnd;}
    Opnd *getOpnd() {return curOpnd;}

    void print(std::ostream & out) {
        out << "Pruint32ing status for: ";
        curOpnd->print(Log::out());
        out << "\n";
        if (type == DEF_OUT_OF_LOOP)
            out << "DOL";
        else if (type == LD_CONST)
            out << "LDC:" << getConst();
        else if (type == COUNTER)
            out << "CNT:" << getIncrement() << (phiSplit ? " splt" : "");
        else
            out << "UNDEF";
        if (isHeaderFound()) {
            out << "\n Header: ";
            if (header != 0) {
                header->print(Log::out());
            } else {
                Log::out() << "NULL";
            }
            out << "\n";
        }
    }

private:
    friend class OSRInductionDetector;
    OpndType type;
    int val;
    bool phiSplit;
    Opnd *header;
    bool header_found;
    Opnd *curOpnd;
};

class OSRInductionDetector {
public:
    typedef StlVector < Inst* >InstStack;
    static OSROpndInfo processOpnd(LoopTree * tree,
                   LoopNode* loopHead,
                   InstStack& defStack,
                   const Opnd* opnd,
                   iv_detection_flag flag = IGNORE_BRANCH);
    static bool inLoop(LoopTree * tree, Opnd * opnd);
    static bool inExactLoop(LoopTree * tree, Opnd * opnd,
                LoopNode * curnode);
    static void writeHeaderToResult(OSROpndInfo & result,
                    LoopTree * tree, OSROpndInfo info1,
                    OSROpndInfo info2);
};

class OSR {
public:
    OSR(IRManager & irManager0, MemoryManager & memManager,
    LoopTree * loop_Tree, DominatorTree * dtree);
    void runPass();

private:
    SsaOpnd * iv;
    SsaOpnd *rc;

    struct OldInst {
        Type* type;
        VarOpnd* var;
        SsaOpnd* ssa;

        bool operator<(OldInst other) const {
            return ((POINTER_SIZE_INT) type + (POINTER_SIZE_SINT) var + (POINTER_SIZE_SINT) ssa) <
            ((POINTER_SIZE_INT) other.type + (POINTER_SIZE_SINT) other.var + (POINTER_SIZE_SINT) other.ssa);
        }
    };

    typedef std::pair < OldInst, Operation > Entry;
    IRManager& irManager;
    MemoryManager& mm;
    LoopTree* loopTree;
    CSEHashTable* hashTable;
    DominatorTree* dominators;
    StlHashMap < SsaOpnd*, SsaOpnd* >leading_operands;

    struct EntryComparison {
        bool operator() (Entry x1, Entry x2) const {
            return std::less < OldInst > () (x1.first, x2.first);
        }
    };

    struct reduceCand {
        reduceCand(SsaOpnd * dst, SsaOpnd * iv, SsaOpnd * rc) {
            this->dst = dst;
            this->iv = iv;
            this->rc = rc;
        } SsaOpnd *iv;
        SsaOpnd *rc;
        SsaOpnd *dst;
    };

    StlMap < Entry, VarOpnd*, EntryComparison > addedOpnds;
    StlVector < reduceCand > cands;

    typedef std::pair < Operation, SsaOpnd* >OperationSsaOpnd;
    typedef std::pair < OperationSsaOpnd, SsaOpnd* >LFTREntry;
    StlHashMap < SsaOpnd *, LFTREntry > LFTRHashMap;

    void writeLeadingOperand(SsaOpnd* opnd, SsaOpnd* header);
    SsaOpnd *getLeadingOperand(SsaOpnd* opnd);
    void replace(SsaOpnd* opnd, SsaOpnd* iv, SsaOpnd* rc);
    SsaOpnd *reduce(Type* type, Opcode opcode, Operation op,
            SsaOpnd* iv, SsaOpnd* rc);
    Inst *lookupReducedDefinition(Operation op, SsaOpnd* iv,
                  SsaOpnd* rc);
    Inst *copyInst(Type* type, Inst* oldDef, SsaOpnd* rc, Operation op);
    void insertInst(Inst * toinsert, Inst * where);
    SsaOpnd *apply(Type* type, Opcode opcode, Operation op,
           SsaOpnd* opnd1, SsaOpnd * opnd2);
    SsaVarOpnd *makeVar(SsaOpnd* opnd, SsaVarOpnd* var, Inst* place);
    SsaTmpOpnd *makeTmp(SsaOpnd* opnd, Inst* place);
    VarOpnd *createOperand(Type* newType, VarOpnd* var, SsaOpnd* rc,
               Operation op);
    bool isLoopInvariant(SsaOpnd * name);
    void writeLFTR(SsaOpnd* iv, Operation op, SsaOpnd*  loop_inv,
           SsaOpnd* result);
    void replaceLinearFuncTest(StlVector < Node* >&postOrderNodes);
    void performLFTR(Inst* inst);
    SsaOpnd *followEdges(SsaOpnd* iv);
    SsaOpnd *applyEdges(SsaOpnd* iv, SsaOpnd* bound);
    void writeInst(Inst* inst);
    Inst *insertNewDef(Type* type, SsaOpnd* iv, SsaOpnd* rc);
    void replaceOperands(Type* type, Inst* newDef, SsaOpnd* iv,
             SsaOpnd* rc, Opcode opcode, Operation op);
    void findLeadingOpnd(Inst* newDef, SsaOpnd* opnd);
    void replaceOperand(U_32 num, Inst* newDef, SsaOpnd* o,
            Opnd* lead, Node* leadBlock, Type* type,
            Opcode opcode, SsaOpnd* rc, Operation op);
    Inst *findInsertionPlace(SsaOpnd* opnd2, SsaOpnd* opnd1);
    void recordIVRC(Inst* inst);
    Inst *chooseLocationForConvert(Inst* inst, Inst* place);
    Inst *createNewVarInst(SsaOpnd* oldDst, Type* type,
               Inst* oldDef, SsaOpnd* rc, Operation op);
    bool isNoArrayInLoop(LoopNode* lnode, SsaOpnd* iv);
};

}
#endif
