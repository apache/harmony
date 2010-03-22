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
 * @author Intel, George A. Timoshenko
 *
 */

#include "Log.h"
#include "Opcode.h"
#include "Type.h"
#include "Stack.h"
#include "IRBuilder.h"
#include "ExceptionInfo.h"
#include "JavaFlowGraphBuilder.h"
#include "TranslatorIntfc.h"
#include "irmanager.h"
#include "FlowGraph.h"

#include <stdlib.h>
#include <string.h>

namespace Jitrino {


Node* JavaFlowGraphBuilder::genBlock(LabelInst* label) {
    if (currentBlock == NULL) {
        currentBlock = createBlockNodeOrdered(label);
        assert(fg->getEntryNode()==NULL);
        fg->setEntryNode(currentBlock);
    } else {
        currentBlock = createBlockNodeOrdered(label);
    }
    return currentBlock;
}

Node* JavaFlowGraphBuilder::genBlockAfterCurrent(LabelInst *label) {
    // hidden exception info
    void *exceptionInfo = ((LabelInst*)currentBlock->getFirstInst())->getState();
    currentBlock = createBlockNodeAfter(currentBlock, label);
    label->setState(exceptionInfo);
    return currentBlock;
}



void JavaFlowGraphBuilder::edgeForFallthrough(Node* block) {
    //
    // find fall through basic block; skip handler nodes
    //
    NodeList::const_iterator niter = std::find(fallThruNodes.begin(), fallThruNodes.end(), block);
    assert(niter != fallThruNodes.end());
    for(++niter; niter != fallThruNodes.end(); ++niter) {
        Node* node = *niter;
        if (node->isBlockNode() && !((LabelInst*)node->getFirstInst())->isCatchLabel()) {
            break;
        }
    }
    assert(niter != fallThruNodes.end());
    Node* fallthrough = *niter;
    addEdge(block,fallthrough);
}

Node * JavaFlowGraphBuilder::edgesForBlock(Node* block) {
    //
    // find if this block has any region that could catch the exception
    //
    Node *dispatch = NULL;
    ExceptionInfo *exceptionInfo = (CatchBlock*)((LabelInst*)block->getFirstInst())->getState();
    if (exceptionInfo != NULL) {
        dispatch = exceptionInfo->getLabelInst()->getNode();
    }else{
        dispatch = fg->getUnwindNode();
    }
    assert(dispatch->isDispatchNode());

    //
    // split the block so that 
    //      each potentially-exceptional instruction ends a block
    //
    Inst* first = (Inst*)block->getFirstInst();
    Inst* last = (Inst*)block->getLastInst();
    Inst* lastExceptionalInstSeen = NULL;
    for (Inst* inst = first->getNextInst(); inst != NULL; inst = inst->getNextInst()) {
        if (lastExceptionalInstSeen != NULL) {
            // start a new basic block
            LabelInst* label = irBuilder.getInstFactory()->makeLabel();
            Node *newblock = createBlockNodeAfter(block, label); 
            uint16 bcOffset = ILLEGAL_BC_MAPPING_VALUE;
            for (Inst *ins = lastExceptionalInstSeen->getNextInst(), *nextIns = NULL; ins!=NULL; ins = nextIns) {
                nextIns = ins->getNextInst();
                ins->unlink();
                newblock->appendInst(ins);
                if (bcOffset == ILLEGAL_BC_MAPPING_VALUE) {
                    bcOffset = ins->getBCOffset();
                }
            }
            label->setBCOffset(bcOffset);

            // now fix up the CFG, duplicating edges
            if (!lastExceptionalInstSeen->isThrow())
                fg->addEdge(block,newblock);
            //
            // add an edge to handler entry node
            //
            assert(!block->findTargetEdge(dispatch));
            fg->addEdge(block,dispatch);
            block = newblock;
            lastExceptionalInstSeen = NULL;
        } 
        if (inst->getOperation().canThrow()) {
            lastExceptionalInstSeen = inst;
        }
    }

    // 
    // examine the last instruction and create appropriate CFG edges
    //
    switch(last->getOpcode()) {
    case Op_Jump:
        {
        fg->addEdge(block,((BranchInst*)last)->getTargetLabel()->getNode());
        last->unlink();
        }
        break;
    case Op_Branch:
    case Op_JSR:
        addEdge(block, ((BranchInst*)last)->getTargetLabel()->getNode());
        edgeForFallthrough(block);
        break;
    case Op_Throw:
    case Op_ThrowSystemException:
    case Op_ThrowLinkingException:
        // throw/rethrow creates an edge to a handler that catches the exception
        assert(dispatch != NULL);
        assert(lastExceptionalInstSeen == last);
        break;
    case Op_Return:
        addEdge(block, fg->getReturnNode());
        break;
    case Op_Ret:
        break; // do  not do anything
    case Op_Switch:
        {
            SwitchInst *sw = (SwitchInst*)last;
            U_32 num = sw->getNumTargets();
            for (U_32 i = 0; i < num; i++) {
                Node* target = sw->getTarget(i)->getNode();
                // two switch values may go to the same block
                if (!block->findTargetEdge(target)) {
                    fg->addEdge(block,target);
                }
            }
            Node* target = sw->getDefaultTarget()->getNode();
            if (!block->findTargetEdge(target)) {
                fg->addEdge(block,target);
            }
        }
        break;
    default:;
        if (block != fg->getReturnNode()) { // a fallthrough edge is needed
           // if the basic block does not have any outgoing edge, add one fall through edge
           if (block->getOutEdges().empty())
               edgeForFallthrough(block);
        }
    }
    //
    // add an edge to handler entry node
    //
    if (lastExceptionalInstSeen != NULL)
        addEdge(block,dispatch);
    return block;
}

void JavaFlowGraphBuilder::edgesForHandler(Node* entry) {
    CatchBlock *catchBlock = (CatchBlock*)((LabelInst*)entry->getFirstInst())->getState();
    if(entry == fg->getUnwindNode())
        // No local handlers for unwind.
        return;
    //
    // create edges between handler entry and each catch
    //
    CatchHandler * handlers = catchBlock->getHandlers();
    for (;handlers != NULL; handlers = handlers->getNextHandler())  {
        fg->addEdge(entry,handlers->getLabelInst()->getNode());
    }
    // edges for uncaught exception
    ExceptionInfo *nextBlock = NULL;
    for (nextBlock = catchBlock->getNextExceptionInfoAtOffset();
        nextBlock != NULL; nextBlock = nextBlock->getNextExceptionInfoAtOffset()) 
    {
        if (nextBlock->isCatchBlock()) {
            Node *next = nextBlock->getLabelInst()->getNode();
            fg->addEdge(entry,next);
            break;
        }
    }
    if(nextBlock == NULL) {
        fg->addEdge(entry,fg->getUnwindNode());
    }
}

void JavaFlowGraphBuilder::createCFGEdges() {
    for (NodeList::iterator it = fallThruNodes.begin(); it!=fallThruNodes.end(); ++it) {
        Node* node = *it;
        if (node->isBlockNode())
            node = edgesForBlock(node);
        else if (node->isDispatchNode())
            edgesForHandler(node);
        else
            assert(node == fg->getExitNode());
    }
}

Node* JavaFlowGraphBuilder::createBlockNodeOrdered(LabelInst* label) {
    assert(label != NULL);
    Node* node = fg->createBlockNode(label);
    fallThruNodes.push_back(node);
    return node;
}

Node* JavaFlowGraphBuilder::createBlockNodeAfter(Node* node, LabelInst* label) {
    NodeList::iterator it = std::find(fallThruNodes.begin(), fallThruNodes.end(), node);
    assert(it!=fallThruNodes.end());
    assert(label != NULL);
    label->setState(((LabelInst*)node->getFirstInst())->getState());
    Node* newNode = fg->createBlockNode(label);
    fallThruNodes.insert(++it, newNode);
    return newNode;
}

Node* JavaFlowGraphBuilder::createDispatchNode() {
    Node* node = fg->createDispatchNode(irBuilder.getInstFactory()->makeLabel());
    fallThruNodes.push_back(node);
    return node;
}

void JavaFlowGraphBuilder::addEdge(Node* source, Node* target) {
    if  (source->findTargetEdge(target) == NULL) {
        fg->addEdge(source, target);
    }
}

//
// construct flow graph
//
void JavaFlowGraphBuilder::build() {
    //
    // create epilog, unwind, and exit
    //
    InstFactory* instFactory = irBuilder.getInstFactory();
    fg->setReturnNode(createBlockNodeOrdered(instFactory->makeLabel()));
    fg->setUnwindNode(createDispatchNode());
    fg->setExitNode(fg->createNode(Node::Kind_Exit, instFactory->makeLabel()));
    fg->addEdge(fg->getReturnNode(), fg->getExitNode());
    fg->addEdge(fg->getUnwindNode(), fg->getExitNode());
    //
    // second phase: construct edges
    //
    createCFGEdges();
}


JavaFlowGraphBuilder::JavaFlowGraphBuilder(MemoryManager& mm, IRBuilder &irB) : 
    memManager(mm), currentBlock(NULL), irBuilder(irB), fallThruNodes(mm)
{
    fg = irBuilder.getFlowGraph();
    methodHandle = irBuilder.getIRManager()->getMethodDesc().getMethodHandle();
}

} //namespace Jitrino 
