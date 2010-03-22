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
 * @author Nikolay A. Sidelnikov
 */
#include "Ia32IRManager.h"

namespace Jitrino
{
namespace Ia32 {

class FastArrayFilling: public SessionAction {

    void runImpl();
};

static ActionFactory<FastArrayFilling> _faf("cg_fastArrayFill");

void
FastArrayFilling::runImpl() 
{
    /*
    find and replace particular internal helper (inserted by HLO path) 
    with a loop providing fast array filling with a constant.
    */

    const Nodes& nodes = irManager->getFlowGraph()->getNodesPostOrder();
    for (Nodes::const_reverse_iterator it = nodes.rbegin(),end = nodes.rend();it!=end; ++it) {
        Node* bb = *it;
        if (!bb->isBlockNode()) {
            continue;
        }
        if(bb->isEmpty()) {
            continue;
        }

        //find basic block with only instruction: internal helper fill_array_with_const
        Inst * inst = (Inst*)bb->getLastInst();
        if (inst->getMnemonic() != Mnemonic_CALL) {
            continue;
        }
        
        Opnd::RuntimeInfo * rt = inst->getOpnd(((ControlTransferInst*)inst)->getTargetOpndIndex())->getRuntimeInfo();

        if (!rt || rt->getKind() != Opnd::RuntimeInfo::Kind_InternalHelperAddress) {
            continue;
        }
        
        std::string str = std::string((const char*)rt->getValue(0));
        if (str != "fill_array_with_const") {
            continue;
        }

        //replace the internal helper with a sequence of instructions
        inst->unlink();

        ControlFlowGraph * fg = irManager->getFlowGraph();
        Edge * outEdge = bb->getOutEdges().front();
        Node * nextNode = outEdge->getTargetNode();

        //extract operands from the internal helper instruction
        Inst::Opnds opnds(inst, Inst::OpndRole_Use|Inst::OpndRole_Auxilary);
        Inst::Opnds::iterator ito = opnds.begin(); 

//      Opnd* args[4] = {valueOp, arrayRef, arrayBound, baseOp};
        Opnd * value = inst->getOpnd(ito++);
        Opnd * arrayRef = inst->getOpnd(ito++);
        Opnd * arrayBound = inst->getOpnd(ito++);
        Opnd * arrayBase = inst->getOpnd(ito++);

        //number of operands must be 5. First operand is the internal helper address
        assert(ito == opnds.end());

        //insert preparing instructions for filling loop
        //LEA instruction loads address of the end of an array 

        TypeManager& tm = irManager->getTypeManager();

        Type * int32Type = tm.getInt32Type();
        Type * intPtrType = tm.getIntPtrType();
        Type * elemType = arrayRef->getType()->asArrayType()->getElementType();
        //memory operand should contains only operand with managed pointer type
        Type * ptrToIntType = tm.getManagedPtrType(int32Type);

        Opnd * arrayEnd = irManager->newOpnd(intPtrType);
        Opnd * scale = irManager->newImmOpnd(int32Type, getByteSize(irManager->getTypeSize(elemType)));
        if (arrayEnd->getSize() > arrayBound->getSize()) {
            bb->appendInst(irManager->newInst(Mnemonic_MOVZX, arrayEnd, arrayBound));
        } else {
            bb->appendInst(irManager->newCopyPseudoInst(Mnemonic_MOV, arrayEnd, arrayBound));
        }
        bb->appendInst(irManager->newInst(Mnemonic_LEA, arrayEnd, irManager->newMemOpnd(arrayEnd->getType(), arrayBase, arrayEnd, scale)));

        //load an address of the first element 
        Opnd * index = irManager->newOpnd(ptrToIntType);
        bb->appendInst(irManager->newCopyPseudoInst(Mnemonic_MOV, index, arrayBase));

        //create increment
        Opnd * incOp = irManager->newOpnd(intPtrType);
        bb->appendInst(irManager->newCopyPseudoInst(Mnemonic_MOV, incOp, irManager->newImmOpnd(intPtrType,8)));

        Node * loopNode = fg->createNode(Node::Kind_Block);
        
        //insert filling instructions 
        Opnd * memOp1 = irManager->newMemOpndAutoKind(value->getType(), index);
        loopNode->appendInst(irManager->newCopyPseudoInst(Mnemonic_MOV, memOp1, value));
#ifndef _EM64T_
        Opnd * memOp2 = irManager->newMemOpndAutoKind(value->getType(), index,irManager->newImmOpnd(int32Type,4));
        loopNode->appendInst(irManager->newCopyPseudoInst(Mnemonic_MOV, memOp2, value));
#endif

        //increment the element address
        loopNode->appendInst(irManager->newInst(Mnemonic_ADD, index, incOp));
        
        //compare the element address with the end of the array
        loopNode->appendInst(irManager->newInst(Mnemonic_CMP, index, arrayEnd));

        fg->replaceEdgeTarget(outEdge, loopNode);

        loopNode->appendInst(irManager->newBranchInst(Mnemonic_JL, loopNode, nextNode));
        fg->addEdge(loopNode, loopNode, 0.95);
        fg->addEdge(loopNode, nextNode, 0.05);
    }
}

}}
