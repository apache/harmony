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
* @author Intel, Nikolay A. Sidelnikov
*
*/

#include "escapeanalyzer.h"
#include "Log.h"
#include "Inst.h"
#include "Dominator.h"
#include "globalopndanalyzer.h"
#include "optimizer.h"
#include "FlowGraph.h"
#include "LoopTree.h"

#include <algorithm>

namespace Jitrino {

    struct LoopEdges 
    {
        Edge * outEdge;
        Edge * inEdge;
        Edge * backEdge;

        LoopEdges() : outEdge(NULL), inEdge(NULL), backEdge(NULL) {} ;
    };

DEFINE_SESSION_ACTION(FastArrayFillPass, fastArrayFill, "Fast Array Filling")

void
FastArrayFillPass::_run(IRManager& irManager) 
{
    /*
    This pass tries to find the array filling code pattern and adds 
    new code under a branch. The new code will be executed in the case of 
    array filling goes from first to last index of the array.

    Example (C++ like):

    Old code:

        char[] a = new char[100];
        char c = 1;
        int num = 100;
        for (int i = 0; i < num; i++) {
            a[i] = c;
        }

    New code:

        char[] a = new char[100];
        int num = 100;
        if (num == lengthof(a)) {
            int * b = (int *)a;
            int cc = 1 | (1 << 16)
            for (int i = 0; i < 100; i += 2) {
                b[i] = cc;
            }
        } else {
            char c = 1;
            int num = 100;
            for (int i = 0; i < num; i++) {
                a[i] = c;
            }
        }
    
    The pattern depends on loop peeling, de-SSA pass and ABCD pass.
    Also it needs cg_fastArrayFill pass in code generator.
    
    Seems like this pass could be moved into the translator phase with 
    significant reducing of the pattern and removing of dependencies 
    from HLO optimizations.
    */

    const double FAIL_PROB = 10e-6; 
        
    LoopTree * info = irManager.getLoopTree();
    if (!info->isValid()) {
        info->rebuild(false);
    }
    if (!info->hasLoops())  {
        return;
    }

    MemoryManager tmm("FastArrayInitPass::insertFastArrayInit");
    Edges loopEdges(tmm);
    StlMap<Node *, LoopEdges> loopInfo(tmm);
        
    //collect necessary information about loops
    const Nodes& nodes = irManager.getFlowGraph().getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (!info->isLoopHeader(node)) {
            continue;
        }
        //compute number of nodes of the loop
        Nodes loopNodes = info->getLoopNode(node,false)->getNodesInLoop();
        size_t sz = loopNodes.size();
        if (sz!=3) {
            continue;
        }

        if (info->isBackEdge(node->getInEdges().front())) {
            loopInfo[node].backEdge = node->getInEdges().front();
            loopInfo[node].inEdge = node->getInEdges().back();
        } else {
            loopInfo[node].inEdge = node->getInEdges().front();
            loopInfo[node].backEdge = node->getInEdges().back();
        }
        loopInfo[node].outEdge = info->isLoopExit(node->getOutEdges().front())? node->getOutEdges().front() : node->getOutEdges().back();
    }

    //check found loops for pattern
    /* Code pattern:

    label .node1
    arraylen  arrayRef ((tau1,tau3)) -) arrayBound:I_32
    chkub 0 .lt. arrayBound -) tau2:tau
    GOTO .node2

    label .node2
    ldbase    arrayRef -) arrayBase:ref:char
    stind.unc:chr constValue ((tau1,tau2,tau3)) -) [arrayBase]
    stvar     startIndex -) index:I_32
    GOTO .loopNode1

    label .loopNode1
    ldvar     index -) tmpIndex:I_32
    if cge.i4  tmpIndex, fillBound goto .loopExit
    GOTO .node2

    label .loopNode2
    chkub tmpIndex .lt. arrayBound -) tau2:tau
    GOTO .node3

    label .loopNode3
    addindex  arrayBase, tmpIndex -) address:ref:char
    stind.unc:chr constValue ((tau1,tau2,tau3)) -) [address]
    add   tmpIndex, addOp -) inc:I_32
    stvar     inc -) index:I_32
    GOTO .node1
    
    */

    for(StlMap<Node *, LoopEdges>::const_iterator it = loopInfo.begin(); it != loopInfo.end(); ++it) {

        Edge * backEdge = it->second.backEdge;
        Edge * inEdge = it->second.inEdge;
        Edge * outEdge = it->second.outEdge;
    
        Node * startNode = backEdge->getSourceNode();
        Opnd * index = NULL; 
        Opnd * tmpIndex = NULL;
        Opnd * constValue = NULL;
        Opnd * address = NULL;
        Opnd * arrayBase = NULL;
        Opnd * arrayRef = NULL;
        Opnd* inc = NULL;
        Opnd * arrayBound = NULL;
        Opnd * fillBound = NULL;
        Inst * inst = ((Inst *)startNode->getLastInst());
        bool found = false;


        //check StVar
        if (inst->getOpcode() == Op_StVar) {
            index = inst->getDst();
            inc = inst->getSrc(0);
            inst = inst->getPrevInst();
            //check Add
            if (inst->getOpcode() == Op_Add) {
                Opnd* addOp = inst->getSrc(1);
                tmpIndex = inst->getSrc(0);
                //check Add operands
                if (inst->getDst() == inc && 
                    addOp->getInst()->getOpcode() == Op_LdConstant && 
                    ((ConstInst *)addOp->getInst())->getValue().i4 == 1) 
                {
                    inst = inst->getPrevInst();
                    //check StInd
                    if (inst->getOpcode() == Op_TauStInd) {
                        constValue = inst->getSrc(0);
                        address = inst->getSrc(1);
                        inst = inst->getPrevInst();
                        //check AddIndex
                        if (inst->getOpcode() == Op_AddScaledIndex && inst->getDst() == address &&
                            inst->getSrc(1) == tmpIndex) 
                        {
                            arrayBase = inst->getSrc(0);
                            Inst* tmpInst = arrayBase->getInst();
                            if (tmpInst->getOpcode() == Op_LdArrayBaseAddr) {
                                arrayRef = tmpInst->getSrc(0); 
                                inst = inst->getPrevInst();
                                //check Label aka beginning of BB
                                if (inst->getOpcode() == Op_Label) {
                                    found = true;
                                }
                            }
                        } 
                    } 
                } 
            } 
        }
        if (!found) {
            continue;
        }
        startNode = startNode->getInEdges().front()->getSourceNode();
        inst = ((Inst *)startNode->getLastInst());

        //check CheckUpperBound
        Opcode cbOpcode = inst->getOpcode();
        if (cbOpcode == Op_TauCheckUpperBound && inst->getSrc(0) == tmpIndex && inst->getPrevInst()->getOpcode() == Op_Label) {
            arrayBound = inst->getSrc(1);
        } else if (cbOpcode == Op_TauCheckBounds && inst->getSrc(1) == tmpIndex && inst->getPrevInst()->getOpcode() == Op_Label) {
            arrayBound = inst->getSrc(0);
        } else {
            continue;
        }

        startNode = startNode->getInEdges().front()->getSourceNode();
        inst = ((Inst *)startNode->getLastInst());
        found = false;
        //check Branch
        if (inst->getOpcode() == Op_Branch && inst->getSrc(0) == tmpIndex) {
            fillBound = inst->getSrc(1);
            inst = inst->getPrevInst();
            //check LdVar and Label
            if (inst->getOpcode() == Op_LdVar && inst->getSrc(0) == index && inst->getDst() == tmpIndex && inst->getPrevInst()->getOpcode() == Op_Label) {
                found = true;
            }
        }
        if (!found) {
            continue;
        }

        startNode = inEdge->getSourceNode();
        startNode = startNode->getInEdges().front()->getSourceNode();
        inst = ((Inst *)startNode->getLastInst());
        found = false;

        //check CheckUpperBound
        ConstInst* cInst = 0;
        cbOpcode = inst->getOpcode();
        if (cbOpcode == Op_TauCheckUpperBound) {
            cInst = (ConstInst *)inst->getSrc(0)->getInst();
        } else if (cbOpcode == Op_TauCheckBounds) {
            cInst = (ConstInst *)inst->getSrc(1)->getInst();
        }
        if (cInst && cInst->getValue().i4 == 0) {
            inst = inst->getPrevInst();
            //check ArrayLength and Label
            if (inst->getOpcode() == Op_TauArrayLen && inst->getSrc(0) == arrayRef && inst->getDst() == arrayBound && inst->getPrevInst()->getOpcode() == Op_Label) {
                found = true;
            }
        }
        if (!found) {
            continue;
        }

        //now we found our pattern

        inEdge = startNode->getInEdges().front();

        //get a new constant
#ifdef _EM64T_
        int64 val = (int64)((ConstInst*)constValue->getInst())->getValue().i8;
#else
        I_32 val = (I_32)((ConstInst*)constValue->getInst())->getValue().i4;
#endif
        switch (((Type*)arrayRef->getType()->asArrayType()->getElementType())->tag) {
            case Type::Int8:
            case Type::Boolean:
            case Type::UInt8:
                val |= (val << 8);
                val |= (val << 16);
#ifdef _EM64T_
                val |= (val << 32);
#endif
                break;
            case Type::Int16:
            case Type::UInt16:
            case Type::Char:
                val |= (val << 16);
#ifdef _EM64T_
                val |= (val << 32);
#endif
                break;
            case Type::Int32:
            case Type::UInt32:
#ifdef _EM64T_
                val |= (val << 32);
                break;
#endif
            case Type::UIntPtr:
            case Type::IntPtr:
#ifdef _EM64T_
            case Type::UInt64:
            case Type::Int64:
#endif
                break;
            default:
                continue;
        }

        //split node1 (see code pattern) after arraylen instruction 
        //and insert a check whether fillBound is equal to arrayBound.
        //if not equal then go to the regular loop
        ControlFlowGraph& fg = irManager.getFlowGraph();
        Node * preheader = fg.splitNodeAtInstruction(inst, true, false, irManager.getInstFactory().makeLabel());
        Inst * cmp = irManager.getInstFactory().makeBranch(Cmp_NE_Un, arrayBound->getType()->tag, arrayBound, fillBound, (LabelInst *)preheader->getFirstInst());
        startNode->findTargetEdge(preheader)->setEdgeProb(FAIL_PROB);
        startNode->appendInst(cmp);

        //create a node with some instructions to prepare variables for loop
        Node * prepNode = fg.createBlockNode(irManager.getInstFactory().makeLabel());
        fg.addEdge(startNode,prepNode, 1.0 - FAIL_PROB);
        
        OpndManager& opndManager = irManager.getOpndManager();

        Opnd * copyOp = opndManager.createArgOpnd(irManager.getTypeManager().getIntPtrType());
        Inst * copyInst  = irManager.getInstFactory().makeLdConst(copyOp,val);
        prepNode->appendInst(copyInst);

        //insert ldbase instruction
        Opnd *baseOp = opndManager.createArgOpnd(irManager.getTypeManager().getIntPtrType());
        Inst * ldBaseInst = irManager.getInstFactory().makeLdArrayBaseAddr(arrayRef->getType()->asArrayType()->getElementType(),baseOp, arrayRef);
        prepNode->appendInst(ldBaseInst);

        Opnd* args[4] = {copyOp, arrayRef, arrayBound, baseOp};

        // insert the helper.
        // this helper should be expanded in the code generator phase
        Inst* initInst = irManager.getInstFactory().makeJitHelperCall(
            OpndManager::getNullOpnd(), FillArrayWithConst, NULL, NULL, 4, args);
        prepNode->appendInst(initInst);

        fg.addEdge(prepNode, outEdge->getTargetNode());
                    
    }
}
}






