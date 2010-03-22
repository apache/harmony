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
#include "Type.h"
#include "FlowGraph.h"
#include "Dominator.h"
#include "Stack.h"
#include "IRBuilder.h"
#include "irmanager.h"
#include "Log.h"
#include "XTimer.h"
#include "StaticProfiler.h"
#include "escapeanalyzer.h"
#include "deadcodeeliminator.h"
#include "TranslatorIntfc.h"
#include "LoopTree.h"
#include "inliner.h"

#include <stdlib.h>
#include <string.h>

namespace Jitrino {

void         
FlowGraph::foldBranch(ControlFlowGraph& fg, BranchInst* br, bool isTaken)
{
	Node* block = br->getNode();
	assert(block->getOutDegree() == 2);
	
    fg.removeEdge(block->getOutEdge(isTaken ? Edge::Kind_False : Edge::Kind_True));
    br->unlink();
}

void         
FlowGraph::foldSwitch(ControlFlowGraph& fg, SwitchInst* sw, U_32 index)
{
    Node* block = sw->getNode();
    assert(sw == block->getLastInst());
    LabelInst* target;
    if(index < sw->getNumTargets())
        target = sw->getTarget(index);
    else
        target = sw->getDefaultTarget();

    while (!block->getOutEdges().empty()) {
        fg.removeEdge(block->getOutEdges().back());
    }
    sw->unlink();
    fg.addEdge(block, target->getNode(), 1.0);
}


void         
FlowGraph::eliminateCheck(ControlFlowGraph& fg, Node* block, Inst* check, bool alwaysThrows)
{
    assert(check == (Inst*)block->getLastInst() &&  check->getOperation().canThrow());
    Edge* edge = NULL;
    if (alwaysThrows) {
#ifndef NDEBUG
        Inst *prevInst = check->getPrevInst();
        assert(prevInst->isThrow());
#endif
        edge = block->getUnconditionalEdge();
    } else {
        edge = block->getExceptionEdge();
    }
    assert(edge != NULL);
    fg.removeEdge(edge);
    check->unlink();
}


Node* 
FlowGraph::tailDuplicate(IRManager& irm, Node* pred, Node* tail, DefUseBuilder& defUses) {
    MemoryManager mm("FlowGraph::tailDuplicate.mm");
    ControlFlowGraph& fg = irm.getFlowGraph();

    // Set region containing only node.
    StlBitVector region(mm, fg.getMaxNodeId()*2);
    region.setBit(tail->getId());

    // Make copy.
    Node* copy = duplicateRegion(irm, tail, region, defUses);

    // Specialize for pred.
    Edge* edge = fg.replaceEdgeTarget(pred->findTargetEdge(tail), copy, true);
    copy->setExecCount(pred->getExecCount()*edge->getEdgeProb());
    if(copy->getExecCount() < tail->getExecCount())  {
        tail->setExecCount(tail->getExecCount() - copy->getExecCount());
    } else {
        tail->setExecCount(0);
    }
    return copy;
}

Node* FlowGraph::duplicateNode(IRManager& irm, Node *source, Node *before, OpndRenameTable *renameTable) {
    Node *newblock;
    LabelInst *first = (LabelInst*)source->getFirstInst();
    if(Log::isEnabled()) {
        first->print(Log::out()); Log::out() << std::endl;
    }
    InstFactory& instFactory = irm.getInstFactory();
    OpndManager& opndManager = irm.getOpndManager();
    ControlFlowGraph& fg = irm.getFlowGraph();
    LabelInst* l = (LabelInst*)instFactory.clone(first,opndManager,renameTable);
    newblock = fg.createNode(source->getKind(), l);
    l->setState(first->getState());

    // go throw the 'entry' instructions and duplicate them (renaming whenever possible)
    for (Inst *inst = first->getNextInst(); inst != NULL; inst = inst->getNextInst()) {
        if(Log::isEnabled()) {
            Log::out() << "DUPLICATE ";
            Log::out().flush();
            inst->print(Log::out()); Log::out() << "\n";
            Log::out().flush();
        }

        Inst* newInst = instFactory.clone(inst,opndManager,renameTable);
        newblock->appendInst(newInst);
        if(Log::isEnabled()) {
            Log::out() << "          ";
            Log::out() << (I_32)inst->getNumSrcOperands();
            Log::out() << " " << (I_32)inst->getOpcode() << " ";
            newInst->print(Log::out());
            Log::out() << "\n";
            Log::out().flush();
        }
    }
    return newblock;
}


Node* FlowGraph::duplicateNode(IRManager& irm, Node *node, StlBitVector* nodesInRegion, 
                           DefUseBuilder* defUses, OpndRenameTable* opndRenameTable,
                           NodeRenameTable* reverseNodeRenameTable) 
{
    if(Log::isEnabled()) {
        Log::out() << "DUPLICATE NODE " << std::endl;
        FlowGraph::print(Log::out(), node);
    }

    InstFactory& instFactory = irm.getInstFactory();
    OpndManager& opndManager = irm.getOpndManager();
    ControlFlowGraph& fg = irm.getFlowGraph();

    Node *newNode;
    LabelInst *first = (LabelInst*)node->getFirstInst();
    LabelInst* l = (LabelInst*)instFactory.clone(first,opndManager,opndRenameTable);
    newNode  = fg.createNode(node->getKind(), l);
    l->setState(first->getState());
    reverseNodeRenameTable->setMapping(newNode, node);

    // Copy edges
    const Edges& outEdges = node->getOutEdges();
    Edges::const_iterator eiter;
    for(eiter = outEdges.begin(); eiter != outEdges.end(); ++eiter) {
        Edge* edge = *eiter;
        Node* succ = edge->getTargetNode();
        Edge* newEdge = fg.addEdge(newNode, succ);
        newEdge->setEdgeProb(edge->getEdgeProb());
    }    

    Node* stBlock = NULL;

    // go throw the 'entry' instructions and duplicate them (renaming whenever possible)
    for (Inst *inst = first->getNextInst(); inst != NULL; inst = inst->getNextInst()) {
        if(Log::isEnabled()) {
            Log::out() << "DUPLICATE ";
            Log::out().flush();
            inst->print(Log::out()); Log::out() << "\n";
            Log::out().flush();
        }

        // Create clone inst, add to new node, and update defuse links.
        Inst* newInst = instFactory.clone(inst,opndManager,opndRenameTable);
        
        // Special care required for SSA operands.
        Opnd* dst = inst->getDst();
        if(dst->isSsaOpnd()) {
            // Determine if dst should be promoted to a var.
            Opnd* var = NULL;
            SsaVarOpnd* srcVar1 = NULL;
            SsaVarOpnd* srcVar2 = NULL;
            DefUseLink* duLink = NULL;

            for(duLink = defUses->getDefUseLinks(inst); duLink != NULL; duLink = duLink->getNext()) {
                int index = duLink->getSrcIndex();
                Inst* useInst = duLink->getUseInst();
                Node* useNode = useInst->getNode();
                assert(useNode != NULL);
                
                if(Log::isEnabled()) {
                    Log::out() << "Examine use: ";
                    useInst->print(Log::out());
                    Log::out() << std::endl;
                }
                
                // No special actions for nodes to be clonned.
                if (nodesInRegion->getBit(useNode->getId())) continue;

                // Handle variables.
                if (dst->isSsaVarOpnd()) {
                    defUses->removeDefUse(inst, useInst, index);
                    // Handle cyclic dependence.
                    if (reverseNodeRenameTable->getMapping(useNode) != NULL) {
                        // There is already phi insturction.
                        // Just update source operand.
                        useInst->setSrc(index, newInst->getDst());
                        defUses->addDefUse(newInst, useInst, index);
                        continue;
                    }
                    // Insert phi instruction.
                    srcVar1 = inst->getDst()->asSsaVarOpnd();
                    srcVar2 = newInst->getDst()->asSsaVarOpnd();
                    StlBitVector visitedNodes = StlBitVector(irm.getMemoryManager(), fg.getMaxNodeId(), false); 
                    Inst* phiInst = insertPhi(irm, nodesInRegion, reverseNodeRenameTable,
                        visitedNodes, useNode, defUses, srcVar1, srcVar2);
                    useInst->setSrc(index, phiInst->getDst());
                    defUses->addDefUse(phiInst, useInst, index);                    
                } else {
                    assert(!dst->isVarOpnd() && !dst->isSsaVarOpnd());
                    // Need to promote dst to variable.
                    if(Log::isEnabled()) {
                        Log::out() << "Patch use: ";
                        useInst->print(Log::out());
                        Log::out() << std::endl;
                    }
                    Inst* patchdef = NULL;
                    ConstInst* ci = inst->asConstInst();
                    if(ci != NULL) {
                        MemoryManager mm("FlowGraph::duplicateNode.mm");
                        OpndRenameTable table(mm,1);
                        patchdef = instFactory.clone(ci, opndManager, &table)->asConstInst();
                        assert(patchdef != NULL);
                    } else {
                        if(var == NULL) {
                            var = opndManager.createVarOpnd(dst->getType(), false);
                            if (irm.getInSsa()) {
                                var = opndManager.createSsaVarOpnd(var->asVarOpnd());
                            }

                            Inst* stVar = irm.getInSsa() ? instFactory.makeStVar(var->asSsaVarOpnd(), dst)
                                : instFactory.makeStVar(var->asVarOpnd(), dst);
                            if(inst->getOperation().canThrow()) {
                                LabelInst* lblInst = instFactory.makeLabel();
                                assert(inst->getBCOffset()!=ILLEGAL_BC_MAPPING_VALUE);
                                lblInst->setBCOffset(inst->getBCOffset());
                                stBlock = fg.createBlockNode(lblInst);
                                stBlock->appendInst(stVar);
                                Node* succ =  node->getUnconditionalEdge()->getTargetNode();
                                Edge* succEdge = node->findTargetEdge(succ);
                                stBlock->setExecCount(node->getExecCount()*succEdge->getEdgeProb());
                                fg.replaceEdgeTarget(succEdge, stBlock, true);
                                fg.replaceEdgeTarget(newNode->findTargetEdge(succ), stBlock, true);
                                fg.addEdge(stBlock, succ)->setEdgeProb(1.0);
                                defUses->addUses(stVar);
                                nodesInRegion->resize(stBlock->getId()+1);
                                nodesInRegion->setBit(stBlock->getId());
                            } else {
                                stVar->insertAfter(inst);
                                defUses->addUses(stVar);
                            }
                        }

                        Opnd* newUse = opndManager.createSsaTmpOpnd(dst->getType());
                        patchdef = irm.getInSsa() ?  instFactory.makeLdVar(newUse, var->asSsaVarOpnd())
                            : instFactory.makeLdVar(newUse, var->asVarOpnd());
                    }

                    //
                    // Patch useInst to load from var.
                    //
                    patchdef->insertBefore(useInst);
                    defUses->addUses(patchdef);
                    useInst->setSrc(index, patchdef->getDst());

                    //
                    // Update def-use chains.
                    //
                    defUses->removeDefUse(inst, useInst, index);
                    defUses->addDefUse(patchdef, useInst, index);
                }
            }
        }
        
        newNode->appendInst(newInst);        
        defUses->addUses(newInst);
    }
    if(Log::isEnabled()) {
        FlowGraph::print(Log::out(), newNode);
        Log::out() << "---------------" << std::endl;
    }
    return newNode;
}

Inst* FlowGraph::insertPhi(IRManager& irm, StlBitVector* nodesInRegion,
                           NodeRenameTable* reverseNodeRenameTable,
                           StlBitVector& visitedNodes,
                           Node* useNode, DefUseBuilder* defUses,
                           SsaVarOpnd* srcVar1, SsaVarOpnd* srcVar2) {
    Inst* phiInst = NULL;
    std::set<SsaVarOpnd*> phiOpnds;
    
    // Check if we already visited this node.
    if (visitedNodes.getBit(useNode->getId())) {
        return NULL;
    }
    
    // Mark node as visited.
    visitedNodes.setBit(useNode->getId(), true);
    
    // Find in values throug all eges.
    const Edges& inEdges = useNode->getInEdges();
    for (Edges::const_iterator it = inEdges.begin(), end = inEdges.end(); it != end; it++) {
        Edge* inEdge = *it;
        if (nodesInRegion->getBit(inEdge->getSourceNode()->getId())) {
            phiOpnds.insert(phiOpnds.end(), srcVar1);
            phiOpnds.insert(phiOpnds.end(), srcVar2);            
        } else if (!reverseNodeRenameTable->getMapping(inEdge->getSourceNode())){
            Inst* phi = insertPhi(irm, nodesInRegion, reverseNodeRenameTable,
                visitedNodes, inEdge->getSourceNode(), defUses, srcVar1, srcVar2);
            if (phi != NULL) {
                phiOpnds.insert(phiOpnds.end(), phi->getDst()->asSsaVarOpnd());
            }
        }
    }
    
    // Merge input values.
    if (phiOpnds.size() == 1) {
        phiInst = (*phiOpnds.begin())->getInst();
    } else if (phiOpnds.size() > 1) {
        Opnd** opnds = (Opnd**)irm.getMemoryManager().alloc(sizeof(Opnd*) * phiOpnds.size()); 
        std::copy(phiOpnds.begin(), phiOpnds.end(), opnds);
        phiInst = findPhi(useNode, opnds, phiOpnds.size());
        if (phiInst == NULL) {
            SsaVarOpnd* dst = irm.getOpndManager().createSsaVarOpnd(srcVar1->getVar());
            phiInst = irm.getInstFactory().makePhi(dst, phiOpnds.size(), opnds);
            useNode->prependInst(phiInst);
            defUses->addUses(phiInst);
        }
    }
    return phiInst;
}

Inst* FlowGraph::findPhi(Node* node, Opnd** opnds, int opndsCount) {
    
    for (Inst* inst = (Inst*)node->getFirstInst(); inst != NULL; inst = inst->getNextInst()) {        
        if (inst->getOpcode() == Op_Phi && inst->getNumSrcOperands() == (U_32)opndsCount) {
            for (int i = 0; i < opndsCount; i++) {
                if (inst->getSrc(i) != opnds[i]) {
                    break;
                }
                return inst;
            }
        }
    }
    return NULL;
}

Node* FlowGraph::_duplicateRegion(IRManager& irm, Node* node, Node* entry,
                              StlBitVector& nodesInRegion,
                              DefUseBuilder* defUses,
                              NodeRenameTable* nodeRenameTable,
                              NodeRenameTable* reverseNodeRenameTable,
                              OpndRenameTable* opndRenameTable) {
    assert(nodesInRegion.getBit(node->getId()));
    Node* newNode = nodeRenameTable->getMapping(node);
    if(newNode != NULL)
        return newNode;

    newNode = duplicateNode(irm, node, &nodesInRegion, defUses, opndRenameTable, reverseNodeRenameTable);
    nodeRenameTable->setMapping(node, newNode);

    ControlFlowGraph& fg = irm.getFlowGraph();
    const Edges& outEdges = node->getOutEdges();
    Edges::const_iterator eiter;
    for(eiter = outEdges.begin(); eiter != outEdges.end(); ++eiter) {
        Edge* edge = *eiter;
        Node* succ = edge->getTargetNode();
        if(nodesInRegion.getBit(succ->getId())) {
            Node* newSucc = nodeRenameTable->getMapping(succ); 
            if (newSucc == NULL) {
                newSucc = _duplicateRegion(irm, succ, entry, nodesInRegion,
                    defUses, nodeRenameTable, reverseNodeRenameTable, opndRenameTable);
                assert(newSucc != NULL);
            }
            fg.replaceEdgeTarget(newNode->findTargetEdge(succ), newSucc, true);
        }
    }    

    return newNode;
}


Node* FlowGraph::duplicateRegion(IRManager& irm, Node* entry,
                                 StlBitVector& nodesInRegion,
                                 DefUseBuilder& defUses,
                                 NodeRenameTable& nodeRenameTable,                                 
                                 OpndRenameTable& opndRenameTable,
                                 double newEntryFreq) {
    NodeRenameTable* reverseNodeRenameTable = new (irm.getMemoryManager()) 
        NodeRenameTable(irm.getMemoryManager(), nodesInRegion.size());     
    Node* newEntry = _duplicateRegion(irm, entry, entry, nodesInRegion,
        &defUses, &nodeRenameTable, reverseNodeRenameTable, &opndRenameTable);
    if(newEntryFreq == 0) {
        return newEntry;
    }
    double scale = newEntryFreq / entry->getExecCount();
    assert(scale >=0 && scale <= 1);
    NodeRenameTable::Iter iter(&nodeRenameTable);
    Node* oldNode = NULL;
    Node* newNode = NULL;
    while(iter.getNextElem(oldNode, newNode)) {
        newNode->setExecCount(oldNode->getExecCount()*scale);
        oldNode->setExecCount(oldNode->getExecCount()*(1-scale));
    }
    return newEntry;
}


Node* FlowGraph::duplicateRegion(IRManager& irm, Node* entry, StlBitVector& nodesInRegion, DefUseBuilder& defUses, double newEntryFreq) {
    MemoryManager dupMemManager("FlowGraph::duplicateRegion.dupMemManager");
    // prepare the hashtable for the operand rename translation
    OpndRenameTable    *opndRenameTable = new (dupMemManager) OpndRenameTable(dupMemManager, nodesInRegion.size());
    NodeRenameTable *nodeRenameTable = new (dupMemManager) NodeRenameTable(dupMemManager, nodesInRegion.size());
    return duplicateRegion(irm, entry, nodesInRegion, defUses, *nodeRenameTable, *opndRenameTable, newEntryFreq);
}

void FlowGraph::renameOperandsInNode(Node *node, OpndRenameTable *renameTable) {
    Inst *first = (Inst*)node->getFirstInst();
    for (Inst *inst = first->getNextInst(); inst != NULL; inst = inst->getNextInst()) {
        U_32 n = inst->getNumSrcOperands();
        for(U_32 i = 0; i < n; ++i) {
            Opnd* src = inst->getSrc(i);
            Opnd* newsrc = renameTable->getMapping(src);
            if(newsrc != NULL)
                inst->setSrc(i, newsrc);
        }
    }
}

//
// used to find 'saveret' that starts the node
//     'labelinst' and 'stvar' are skipped
//
static Inst* findSaveRet(Node* node) {
    assert(node);
    Inst* first = (Inst*)node->getFirstInst();
    assert(first->getOpcode() == Op_Label);
    Inst* i;
    for ( i = first->getNextInst(); i != NULL; i = i->getNextInst() ) {
        Opcode opc = i->getOpcode();
        if ( opc == Op_SaveRet ) {
            return i;
        }else if ( opc != Op_LdVar ) {
            return NULL;
        }
    }
    return NULL;
}

void static findNodesInRegion(Node* entry, Node* end, StlBitVector& nodesInRegion) {
    if(Log::isEnabled()) {
        Log::out() << "Find nodes in region (entry = ";
        FlowGraph::printLabel(Log::out(), entry);
        Log::out() << ", exit = ";
        FlowGraph::printLabel(Log::out(), end);
        Log::out() << ")" << std::endl;
    }

    // Return if visited and mark.
    if(nodesInRegion.setBit(end->getId()))
        return;

    if(end != entry) {
        const Edges& inEdges = end->getInEdges();
        // Must not be method entry.
        assert(!inEdges.empty());
        Edges::const_iterator eiter;
        for(eiter = inEdges.begin(); eiter != inEdges.end(); ++eiter) {
            Edge* edge = *eiter;
            findNodesInRegion(entry, edge->getSourceNode(), nodesInRegion);
        }
    }
    assert(nodesInRegion.getBit(entry->getId()));
}


static bool inlineJSR(IRManager* irManager, Node *block, DefUseBuilder& defUses, JsrEntryInstToRetInstMap& entryMap) {
    // Inline the JSR call at the end of block.
    Inst* last = (Inst*)block->getLastInst();
    assert(last->isJSR());
    BranchInst* jsrInst   = (BranchInst*) last;

    //
    // Process entry of JSR.
    //
    Node* entryJSR  = jsrInst->getTargetLabel()->getNode();
    Inst *saveReturn = findSaveRet(entryJSR);
    assert(saveReturn);
    //
    // if JSR 'returns' via at least one RET
    // then
    //     'stvar' follows 'saveret' and stores return info into the local var
    //     retVar (the stored operand) is non-NULL
    // else
    //     no 'stvar' can follow 'saveret' (because it is unused)
    //     retVar is NULL
    Inst *stVar = saveReturn->getNextInst();
    Opnd *retVar = NULL;
    if ( stVar && 
         stVar->isStVar() && 
         (stVar->getSrc(0) == saveReturn->getDst()) ) {
        retVar = stVar->getDst();
    }

    //
    // Find return node for this invocation.
    //
    Node* retTarget = NULL;
    const Edges& outEdges = block->getOutEdges();
    Edges::const_iterator eiter;
    for(eiter = outEdges.begin(); eiter != outEdges.end(); ++eiter) {
        Edge* edge = *eiter;
        Node *node = edge->getTargetNode(); 
        if (node != entryJSR && !node->isDispatchNode()) {
            retTarget = node;
            break;
        }
    }

    if(Log::isEnabled()) {
        Log::out() << "INLINE JSR into ";
        FlowGraph::printLabel(Log::out(), block);
        Log::out() << ":" << std::endl;
        FlowGraph::print(Log::out(), entryJSR);
    }

    ControlFlowGraph& fg = irManager->getFlowGraph();

    //
    // entryMap is a mapping [stVar -> ret inst]
    //
    // In some cases retNode exists, but is unreachable
    // Let's check this
    // if retNodeIsUnrichable==true
    // then JSR never returns.  Convert to jmp
    bool retNodeIsUnreachable = true;
    if (entryMap.find(stVar) != entryMap.end()) {
        JsrEntryCIterRange jsr_range = entryMap.equal_range(stVar);
        JsrEntryInstToRetInstMap::const_iterator i;
        for(i = jsr_range.first; i != jsr_range.second; ++i) {
            assert(i->first->getNode() == entryJSR);
            Node* retNode = i->second->getNode();
            UNUSED Inst* ret = (Inst*)retNode->getLastInst();
            assert(ret->isRet());
            assert(ret->getSrc(0) == retVar);

            const Edges& inEdges = retNode->getInEdges();
            if (!inEdges.empty()) {
                retNodeIsUnreachable = false;
                break;
            }
        }
    }

    if(retNodeIsUnreachable == true || retVar == NULL || (entryMap.find(stVar) == entryMap.end())) {
        //
        // JSR never returns.  Convert to jmp.
        //
        saveReturn->unlink();
        if(retVar != NULL) {
            irManager->getOpndManager().deleteVar((VarOpnd*)retVar);
        }

        const Edges& inEdges = entryJSR->getInEdges();
        for(eiter = inEdges.begin(); eiter != inEdges.end();) {
            Edge* edge = *eiter;
            ++eiter;
            Node *node = edge->getSourceNode();
            Inst* last = (Inst*)node->getLastInst();
            if (last->isJSR()) {
	           last->unlink();
            }
            if (node->getOutDegree() == 2) {
	            Node* t1 = node->getOutEdges().front()->getTargetNode();
	            Node* t2 = node->getOutEdges().back()->getTargetNode();
	            if(t1 == entryJSR) {
	                fg.removeEdge(node, t2);
	            } else {
	                assert(t2 == entryJSR);
	                fg.removeEdge(node, t1);
	            }
            }
        }
    } else if (entryJSR->hasOnlyOnePredEdge()) {
        // 
        // Inline the JSR in place - no duplication needed.
        //
        fg.removeEdge(block,retTarget);
        jsrInst->unlink();

        JsrEntryCIterRange jsr_range = entryMap.equal_range(stVar);
        JsrEntryInstToRetInstMap::const_iterator i;
        for(i = jsr_range.first; i != jsr_range.second; ++i) {
            assert(i->first->getNode() == entryJSR);
            Node* retNode = i->second->getNode();
            Inst* ret = (Inst*)retNode->getLastInst();
            assert(ret->isRet());
            assert(ret->getSrc(0) == retVar);
            ret->unlink();
            fg.addEdge(retNode, retTarget);
        }
    } else {
        //
        // Duplicate and inline the JSR.
        //
        MemoryManager inlineManager("FlowGraph::inlineJSR.inlineManager"); 

        // Find the nodes in the JSR.
        StlBitVector nodesInJSR(inlineManager, fg.getMaxNodeId());

        JsrEntryCIterRange jsr_range = entryMap.equal_range(stVar);
        JsrEntryInstToRetInstMap::const_iterator i;
        for(i = jsr_range.first; i != jsr_range.second; ++i) {
            assert((*i).first->getNode() == entryJSR);
            findNodesInRegion(entryJSR, (*i).second->getNode(), nodesInJSR);
        }

        // prepare the hash tables for rename translation
        OpndRenameTable    *opndRenameTable = new (inlineManager) OpndRenameTable(inlineManager,10);
        NodeRenameTable *nodeRenameTable = new (inlineManager) NodeRenameTable(inlineManager,10);

        Node* newEntry = FlowGraph::duplicateRegion(*irManager, entryJSR, nodesInJSR, defUses, *nodeRenameTable, *opndRenameTable, 0);

        fg.removeEdge(block,retTarget);
        fg.removeEdge(block,entryJSR);
        jsrInst->unlink();
        fg.addEdge(block, newEntry);

        //
        // Add edge from inline ret locations to return target.
        //
        for(i = jsr_range.first; i != jsr_range.second; ++i) {
            Node* inlinedRetNode = nodeRenameTable->getMapping((*i).second->getNode());
            Inst* ret = (Inst*)inlinedRetNode->getLastInst();
            assert(ret->isRet());
            assert(ret->getSrc(0) == retVar);
            ret->unlink();
            fg.addEdge(inlinedRetNode, retTarget);
        }

        //
        // delete the first two instructions of the inlined node
        // 
        assert(entryJSR != newEntry);
        saveReturn = findSaveRet(newEntry);
        assert(saveReturn);
        stVar = saveReturn->getNextInst();
        assert(stVar->isStVar() && (stVar->getSrc(0) == saveReturn->getDst()));
        retVar = stVar->getDst();

    }
    if(retVar != NULL) {
        ((VarOpnd *)retVar)->setDeadFlag(true); 
        stVar->unlink();
    }
    saveReturn->unlink();
    return TRUE;
}


static void inlineJSRs(IRManager* irManager) {
    MemoryManager jsrMemoryManager("FlowGraph::inlineJSRs.jsrMemoryManager");
    static CountTime inlineJSRTimer("ptra::fg::inlineJSRs");
    AutoTimer tm(inlineJSRTimer);

    JsrEntryInstToRetInstMap* jsr_entry_map = irManager->getJsrEntryMap();
    assert(jsr_entry_map); // must be set by translator

    if(Log::isEnabled()) {
        Log::out() << "JSR entry map:";
        JsrEntryInstToRetInstMap::const_iterator jsr_entry_it, jsr_entry_end;
        for ( jsr_entry_it = jsr_entry_map->begin(), 
            jsr_entry_end = jsr_entry_map->end(); 
            jsr_entry_it != jsr_entry_end; ++jsr_entry_it) {
                Log::out() << "--> [entry->ret]: ";
                jsr_entry_it->first->print(Log::out());
                Log::out() << " || ";
                jsr_entry_it->second->print(Log::out());
                Log::out() << std::endl;
            }
    }

    DefUseBuilder defUses(jsrMemoryManager);
    {
        static CountTime findJSRTimer("ptra::fg::inlineJSRs::defuse");
        AutoTimer tm(findJSRTimer);
        defUses.initialize(irManager->getFlowGraph());
    }

    ControlFlowGraph& fg = irManager->getFlowGraph();
    const Nodes& nodes = fg.getNodes();
    //Nodes nodes(jsrMemoryManager);
    //fg.getNodesPostOrder(nodes);
    //std::reverse(nodes.begin(), nodes.end());
    //WARN: new nodes created during the iteration 
    //we use the fact that new nodes added to the end of the collection here.
    for (U_32 idx = 0; idx < nodes.size(); ++idx) {
        Node* node = nodes[idx];
        Inst* last = (Inst*)node->getLastInst();
        if(last->isJSR()) {
            inlineJSR(irManager, node, defUses, *jsr_entry_map);
        }
    }
#ifdef _DEBUG
    const Nodes& nnodes = fg.getNodes();
    for (U_32 idx = 0; idx < nnodes.size(); ++idx) {
        Node* node = nnodes[idx];
        Inst* last = (Inst*)node->getLastInst();
        assert(!last->isJSR());
    }
#endif
}

// Finally inlining
static void _fixFinally(ControlFlowGraph& fg, Node *node, Node *retTarget) {
    // if the node has been visited, return
    if (node->getTraversalNum() >= fg.getTraversalNum()) {
        return;
    }
    node->setTraversalNum(fg.getTraversalNum());
    const Edges& outEdges = node->getOutEdges();
    Edges::const_iterator eiter;
    for(eiter = outEdges.begin(); eiter != outEdges.end(); ++eiter) {
        Edge* edge = *eiter;
        _fixFinally(fg, edge->getTargetNode(),retTarget);
    }

}


static void _fixBranchTargets(Inst *newInst, NodeRenameTable *nodeRenameTable) {
    // fix targets of instructions
    switch(newInst->getOpcode()) {
    case Op_Branch:
    case Op_JSR:
    case Op_Jump:
        {
            BranchInst *newBranch = (BranchInst*)newInst;
            Node *tar = newBranch->getTargetLabel()->getNode();
            if(nodeRenameTable->getMapping(tar) != NULL) {
                LabelInst *label = (LabelInst*)nodeRenameTable->getMapping(tar)->getFirstInst();
                newBranch->replaceTargetLabel(label);
            }
        }
        break;
    case Op_Switch:
        assert(0);
    default:
        break;
    }
}

// Finally inlining
bool FlowGraph::_inlineFinally(IRManager& irm, Node *from, Node *to, Node *retTarget,
                            NodeRenameTable *nodeRenameTable,
                            OpndRenameTable *opndRenameTable) {
    // if the node has been visited, return
    if (nodeRenameTable->getMapping(from) != NULL) return true;

    ControlFlowGraph& fg = irm.getFlowGraph();
    // if the node is epilogue, add and edge to it
    if (to->isExitNode()) {
       fg.addEdge(from,to);
       return true;
    }
    Node *newto = nodeRenameTable->getMapping(to);
    if (newto != NULL) {
        fg.addEdge(from,newto);
        return true;
    }
    // start following the control flow graph, duplicating basic blocks as needed
    newto = duplicateNode(irm, to, to, opndRenameTable);
    fg.addEdge(from,newto);
    nodeRenameTable->setMapping(to,newto);

    const Edges& outEdges = to->getOutEdges();
    Edges::const_iterator eiter;
    for(eiter = outEdges.begin(); eiter != outEdges.end(); ++eiter) {
        Edge* edge = *eiter;
        _inlineFinally(irm, newto,edge->getTargetNode(),retTarget,nodeRenameTable,opndRenameTable);
    }
    _fixBranchTargets((Inst*)newto->getLastInst(),nodeRenameTable);
    return true;
}


bool FlowGraph::inlineFinally(IRManager& irm, Node *block) {
    ControlFlowGraph& fg = irm.getFlowGraph();
    BranchInst* leaveInst  = (BranchInst*)block->getLastInst();
    Node *   retTarget  = leaveInst->getTargetLabel()->getNode();
    // record input operand
    Node *   entryFinally = NULL;
    const Edges& outEdges = block->getOutEdges();
    Edges::const_iterator eiter;
    for(eiter = outEdges.begin(); eiter != outEdges.end(); ++eiter) {
        Edge* edge = *eiter;
        Node *node = edge->getTargetNode();
        if (node != retTarget && !node->isDispatchNode()) {
            entryFinally = node;
            break;
        }
    }
    if (Log::isEnabled()) {
        Log::out() << "INLINE FINALLY\n";
        FlowGraph::printLabel(Log::out(), entryFinally);
        FlowGraph::printLabel(Log::out(), retTarget);
        Log::out() << std::endl;
        Log::out().flush();
    }
    if (entryFinally->hasOnlyOnePredEdge()) {
        fg.setTraversalNum(fg.getTraversalNum()+1);
        fg.removeEdge(block,retTarget);
        _fixFinally(fg, entryFinally,retTarget);
    } else {
        MemoryManager inlineManager("FlowGraph::inlineFinally.inlineManager");
        // prepare the hashtable for the operand rename translation
        OpndRenameTable    *opndRenameTable =
            new (inlineManager) OpndRenameTable(inlineManager,10);
        NodeRenameTable *nodeRenameTable =new (inlineManager) NodeRenameTable(inlineManager,10);
        fg.removeEdge(block,retTarget);
        fg.removeEdge(block,entryFinally);
        _inlineFinally(irm, block,entryFinally,retTarget, nodeRenameTable,opndRenameTable );
    }
    leaveInst->unlink();
    return true;
}


static void checkBCMapping(IRManager& irm) {
#ifdef _DEBUG
    ControlFlowGraph& fg = irm.getFlowGraph();
    const Nodes& nodes=  fg.getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
    	Node* node = *it;
        if (!node->isEmpty()) { //allow empty nodes (like dispatches, exit, return nodes) do not have bc-mapping
            for (Inst* inst = (Inst*)node->getFirstInst(); inst!=NULL; inst = inst->getNextInst()) {
                if (inst->getOperation().canThrow() || inst->isLabel()) {
                    assert(inst->getBCOffset()!=ILLEGAL_BC_MAPPING_VALUE);
                }
            }
        }
    }
#endif
}

void FlowGraph::doTranslatorCleanupPhase(IRManager& irm) {
    U_32 id = irm.getCompilationContext()->getCurrentSessionNum();
    const char* stage = "trans_cleanup";
    if (Log::isLogEnabled(LogStream::IRDUMP)) {
        LogStream& irdump = Log::log(LogStream::IRDUMP);
        Log::printStageBegin(irdump.out(), id, "TRANS", stage, stage);
        irdump << OptPass::indent(irm) << "Trans:   Running " << "cleanup" << ::std::endl;
        Log::printIRDumpBegin(irdump.out(), id, stage, "before");
        printHIR(irdump.out(), irm.getFlowGraph(), irm.getMethodDesc());
        Log::printIRDumpEnd(irdump.out(), id, stage, "before");
    }

    ControlFlowGraph& fg = irm.getFlowGraph();
    InstFactory& instFactory = irm.getInstFactory();
    OpndManager& opndManager = irm.getOpndManager();


    static CountTime cleanupPhaseTimer("ptra::fg::cleanupPhase");
    AutoTimer tm(cleanupPhaseTimer);
    fg.purgeUnreachableNodes();

#ifdef _DEBUG
    //check if loop structure is valid after translator
    OptPass::computeLoops(irm, false);
#endif


    inlineJSRs(&irm);
    fg.purgeUnreachableNodes();

    checkBCMapping(irm);

    {
        static CountTime cleanupPhaseInternalTimer("ptra::fg::cleanupPhase::in");
        AutoTimer tm(cleanupPhaseInternalTimer);
        // Cleanup optimizations
        for (Nodes::const_iterator niter = fg.getNodes().begin(), end = fg.getNodes().end(); niter!=end; ++niter) {
            Node* node = *niter;
            if (node->isDispatchNode() || node == fg.getReturnNode() || node == fg.getExitNode()) {
                continue;
            }
            Inst *last = (Inst*)node->getLastInst();
            if (last->isBranch()) {
                if (last->isConditionalBranch()) { // loop peeling for conditional branches
                    Node *target  = ((BranchInst*)last)->getTargetLabel()->getNode();
                    if (((LabelInst*)target->getFirstInst())->getLabelId() > ((LabelInst*)node->getFirstInst())->getLabelId()) 
                    {
                        LabelInst *targetLabel = ((BranchInst*)last)->getTargetLabel();
                        Node *fthru = NULL, *taken = NULL;
                        const Edges& outEdges = node->getOutEdges();
                        Edges::const_iterator eIter;
                        for(eIter = outEdges.begin(); eIter != outEdges.end(); ++eIter) {
                            Edge* edge = *eIter;
                            Node * tar = edge->getTargetNode();
                            if (!tar->isDispatchNode()) {
                                if (tar->getFirstInst() == targetLabel) {
                                    taken = tar;
                                } else {
                                    fthru = tar; 
                                }
                            }
                        }
                        Inst *takenLdConst = (Inst*)taken->getSecondInst();
                        Inst *fthruLdConst = (Inst*)fthru->getSecondInst();
                        Inst *takenStVar   = (takenLdConst!=NULL) ? takenLdConst->getNextInst() : NULL;
                        Inst *fthruStVar   = (fthruLdConst!=NULL) ? fthruLdConst->getNextInst() : NULL;
                        if (takenStVar!=NULL && fthruStVar!=NULL                    &&
                            taken->hasOnlyOneSuccEdge()                             &&
                            taken->hasOnlyOnePredEdge()                             &&
                            fthru->hasOnlyOneSuccEdge()                             &&
                            fthru->hasOnlyOnePredEdge()                             &&
                            (taken->getOutEdges().front()->getTargetNode() ==
                            fthru->getOutEdges().front()->getTargetNode())         &&
                            takenLdConst->getOpcode()       == Op_LdConstant        &&
                            fthruLdConst->getOpcode()       == Op_LdConstant        &&
                            takenLdConst->getType()         == Type::Int32          &&
                            fthruLdConst->getType()         == Type::Int32          &&
                            takenStVar->getOpcode()         == Op_StVar             &&
                            fthruStVar->getOpcode()         == Op_StVar             &&
                            takenStVar->getDst()            == fthruStVar->getDst() &&
                            takenStVar->getNextInst()       == taken->getLastInst() &&
                            fthruStVar->getNextInst()       == fthru->getLastInst())
                        {

                                int takenint32 = ((ConstInst*)takenLdConst)->getValue().i4;
                                int fthruint32 = ((ConstInst*)fthruLdConst)->getValue().i4;
                                Node *meet = taken->getOutEdges().front()->getTargetNode();
                                // find the ldVar for the variable, if any
                                Inst *meetLdVar = (Inst*)meet->getSecondInst();
                                while (meetLdVar->getOpcode()==Op_LdVar) {
                                    if (meetLdVar->getSrc(0) == takenStVar->getDst())
                                        break;
                                    meetLdVar = meetLdVar->getNextInst();
                                }
                                if ((((takenint32==0) && (fthruint32==1)) ||
                                    ((takenint32==1) && (fthruint32==0))) &&
                                    meetLdVar->getOpcode()==Op_LdVar &&
                                    last->getNumSrcOperands()==2) {
                                        // change the instruction to reflect the compare instruction
                                        fg.removeEdge(node,taken);
                                        fg.removeEdge(node,fthru);
                                        fg.addEdge(node,meet);
                                        Opnd* dst = opndManager.createSsaTmpOpnd(meetLdVar->getDst()->getType());
                                        BranchInst *lastBranch = (BranchInst*)last;
                                        if (takenint32==0) 
                                            lastBranch->swapTargets(NULL);
                                        Inst *cmp = instFactory.makeCmp(
                                            lastBranch->getComparisonModifier(),
                                            lastBranch->getType(), dst, 
                                            lastBranch->getSrc(0),
                                            lastBranch->getSrc(1));
                                        cmp->insertBefore(lastBranch);
                                        Inst* newStVar = instFactory.makeStVar(meetLdVar->getSrc(0)->asVarOpnd(),dst);
                                        newStVar->insertBefore(lastBranch);
                                        lastBranch->unlink();
                                    }
                            }
                    }
                }
            }
            // remove trivial basic blocks
            if (node->hasOnlyOnePredEdge()) {
                Node *pred = node->getInEdges().front()->getSourceNode();
                if (!pred->isDispatchNode() &&  !((Inst*)pred->getLastInst())->isSwitch() &&
                    (pred->hasOnlyOneSuccEdge() ||  (node->isEmpty() && node->hasOnlyOneSuccEdge()))) 
                {                

                    // don't merge if the node has an exception edge 
                    Edge *edge = NULL;
                    Edges::const_iterator eiter;
                    for(eiter = node->getOutEdges().begin(); eiter != node->getOutEdges().end(); ++eiter) {
                        edge = *eiter;
                        if ((edge->getTargetNode())->isDispatchNode()) {
                            break;
                        }
                    }
                    // If the node has an exception edge, then merging is potentially illegal, so
                    // skip.
                    if (edge == NULL) {
                        if (Log::isEnabled()) {
                            Log::out()<<" MERGE ";FlowGraph::printLabel(Log::out(), pred);FlowGraph::printLabel(Log::out(), node);Log::out()<<"\n";
                            Log::out().flush();
                        }
                        BranchInst *branch = NULL;
                        if (!pred->hasOnlyOneSuccEdge() && node->isEmpty()) {
                            Inst* lastPred = (Inst*)pred->getLastInst();
                            Node* nodeSucc = node->getOutEdges().front()->getTargetNode();
                            if (lastPred->isBranch()) {
                                branch = (BranchInst*)lastPred;
                                if (branch->getTargetLabel() == node->getFirstInst()) {
                                    branch->replaceTargetLabel((LabelInst*)nodeSucc->getFirstInst());
                                }
                            }
                        }
                        fg.mergeBlocks(pred,node);

                        // remove useless branches
                        if (branch != NULL) {
                            Node *target = NULL;
                            for(eiter=pred->getOutEdges().begin(); eiter!=pred->getOutEdges().end();) {
                                Edge* edge = *eiter;
                                ++eiter;
                                Node *succ = edge->getTargetNode();
                                if (! succ->isDispatchNode()) {
                                    if (target == succ) {
                                        fg.removeEdge(pred,succ);
                                        branch->unlink();
                                        break;
                                    }
                                    target = succ;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Remove extra PseudoThrow insts
    DeadCodeEliminator dce(irm);
    dce.removeExtraPseudoThrow();

    //process all methods marked with @Inline pragma
    if (Log::isLogEnabled(LogStream::IRDUMP)) {
        LogStream& irdump = Log::log(LogStream::IRDUMP);
        Log::printIRDumpBegin(irdump.out(), id, stage, "before_pragma_inline");
        printHIR(irdump.out(), irm.getFlowGraph(), irm.getMethodDesc());
        Log::printIRDumpEnd(irdump.out(), id, stage, "before_pragma_inline");
    }
    Inliner::processInlinePragmas(irm);


    //
    // a quick cleanup of unreachable and empty basic blocks
    //
    fg.purgeUnreachableNodes();
    fg.purgeEmptyNodes(false);

    if (Log::isLogEnabled(LogStream::IRDUMP)) {
        LogStream& irdump = Log::log(LogStream::IRDUMP);
        Log::printStageEnd(irdump.out(), id, "TRANS", stage, stage);
    }

}

void FlowGraph::printHIR(std::ostream& os, ControlFlowGraph& fg, MethodDesc& methodDesc) {
    const char* methodName = methodDesc.getName();
    os << std::endl << "--------  irDump: " << methodDesc.getParentType()->getName() << "::" << methodName << "  --------" << std::endl;

    MemoryManager mm("ControlFlowGraph::print.mm");
    Nodes nodes(mm);
    fg.getNodesPostOrder(nodes);
    Nodes::reverse_iterator iter = nodes.rbegin(), end = nodes.rend();
    for(; iter != end; iter++) {
        Node* node = *iter;
        print(os, node);
        os << std::endl;
    }
}

void FlowGraph::print(std::ostream& os, Node* node) {
    os << "Block ";
    printLabel(os, node);
    os << ":  ";
    os << std::endl;

    // Print predecessors
    os << "  Predecessors:";
    const Edges& inEdges = node->getInEdges();
    for (Edges::const_iterator ite = inEdges.begin(), ende = inEdges.end(); ite!=ende; ++ite) {
        Edge* edge = *ite;
        os << " ";
        printLabel(os, edge->getSourceNode());
    }
    os << std::endl;

    // Print successors
    os << "  Successors:";
    const Edges& outEdges = node->getOutEdges();
    for (Edges::const_iterator ite = outEdges.begin(), ende = outEdges.end(); ite!=ende; ++ite) {
        Edge* edge = *ite;
        os << " ";
        printLabel(os, edge->getTargetNode());
    }
    os << std::endl;

    // Print instructions
    printInsts(os, node, 2);

}

void FlowGraph::printLabel(std::ostream& cout, Node* node) {
    Inst *first = (Inst*)node->getFirstInst();
    assert(first->isLabel());
    if (node->isBlockNode()) {
        if(node->getInDegree() == 0) {
            cout << "ENTRY_";
            ((LabelInst*)first)->printId(cout);
        } else if(node->getOutDegree() == 1 && node->getOutEdges().front()->getTargetNode()->isExitNode()) {
            cout << "RETURN";
        } else {
            ((LabelInst*)first)->printId(cout);
        }
    } else if (node->isDispatchNode()) {
        if(node->getOutDegree() == 1 && node->getOutEdges().front()->getTargetNode()->isExitNode())
            cout << "UNWIND";
        else
            cout << "D" << (I_32)((LabelInst*)first)->getLabelId();
    } else {
        cout << "EXIT";
    }
}

void FlowGraph::printInsts(std::ostream& cout, Node* node, U_32 indent){
    std::string indentstr(indent, ' ');
    Inst* inst = (Inst*)node->getFirstInst();
    while (inst!=NULL) {
        cout << indentstr.c_str();
        inst->print(cout); 
        cout << std::endl;
        inst = inst->getNextInst();
    }

    // The IR does not contain explicit GOTOs, so for IR printing purposes
    // we should print a GOTO if required, extracted from the CFG.
    inst = (Inst*)node->getLastInst();
    if (!inst->isReturn() && !inst->isThrow()  && !inst->isRet()) {
            Node *target = NULL;
            if (inst->isConditionalBranch()) {
                target = ((BranchInst*)inst)->getTakenEdge(false)->getTargetNode();
            } else {
                Edges::const_iterator j = node->getOutEdges().begin(), jend = node->getOutEdges().end();
                for (; j != jend; ++j) {
                    Edge* edge = *j;
                    Node *node = edge->getTargetNode();
                    if (!node->isDispatchNode()) {
                        target = node;
                        break;
                    }
                }
            }
            if (target != NULL) {
                cout << indentstr.c_str() << "GOTO ";
                printLabel(cout, target);
                cout << std::endl;
            }
        }
}

class HIRDotPrinter : public PrintDotFile {
public:
    HIRDotPrinter(ControlFlowGraph& _fg) : fg(_fg), loopTree(NULL), dom(NULL){}
    void printDotBody();
    void printDotNode(Node* node);
    void printDotEdge(Edge* edge);
private:
    ControlFlowGraph& fg;
    LoopTree* loopTree;
    DominatorTree* dom;
};

void FlowGraph::printDotFile(ControlFlowGraph& fg, MethodDesc& methodDesc,const char *suffix) {
    HIRDotPrinter printer(fg);
    printer.printDotFile(methodDesc, suffix);
}

void HIRDotPrinter::printDotBody() {
    const Nodes& nodes = fg.getNodes();
    dom = fg.getDominatorTree();
    if (dom != NULL && !dom->isValid()) {
        dom = NULL;
    }
    loopTree = fg.getLoopTree();
    if (loopTree!=NULL && !loopTree->isValid()) {
        loopTree = NULL;
    }
    for(Nodes::const_iterator niter = nodes.begin(); niter != nodes.end(); ++niter) {
        Node* node = *niter;
        printDotNode(node);
    }
    for(Nodes::const_iterator niter = nodes.begin(); niter != nodes.end(); ++niter) {
        Node* node = *niter;
        const Edges& edges = node->getOutEdges();
        for(Edges::const_iterator eiter = edges.begin(); eiter != edges.end(); ++eiter) {
            Edge* edge = *eiter;
            printDotEdge(edge);
        }
    }
}

void HIRDotPrinter::printDotNode(Node* node) {
    std::ostream& out = *os;
    FlowGraph::printLabel(out, node);
    out << " [label=\"";
    if (!node->isEmpty()) {
        out << "{";
    }
    ((Inst*)node->getFirstInst())->print(out);
    out << "tn: " << (int) node->getTraversalNum() << " pre:" << (int)node->getPreNum() << " post:" << (int)node->getPostNum() << " ";
    out << "id: " << (int) node->getId() << " ";
    if(fg.hasEdgeProfile()) {
        out << " execCount:" << node->getExecCount() << " ";
    }
    Node* idom = dom==NULL ? NULL: dom->getIdom(node);
    if (idom!=NULL) {
        out << "idom("; FlowGraph::printLabel(out, idom); out << ") ";
    }
    if (loopTree!=NULL) {
        Node* loopHeader = loopTree->getLoopHeader(node);
        bool isLoopHeader = loopTree->isLoopHeader(node);
        out << "loop(";
        if (!isLoopHeader) {
            out<<"!";
        }
        out<<"hdr";
        if (loopHeader!=NULL) {
            out<<" head:";FlowGraph::printLabel(out, loopHeader); 
        }
        out << ") ";
    }

    if (!node->isEmpty()) {
        out << "\\l|\\" << std::endl;
        Inst* first = (Inst*)node->getFirstInst();
        for (Inst* i = first->getNextInst(); i != NULL; i=i->getNextInst()) { // skip label inst
            i->print(out); 
            out << "\\l\\" << std::endl;
        }
        out << "}";
    }
    out << "\"";
    if (!node->isExitNode() && fg.hasEdgeProfile()) {
        double freq = node->getExecCount();
        double methodFreq = fg.getEntryNode()->getExecCount();
        if(freq > methodFreq*10) {
            out << ",color=red";
        } else if(freq > 0 && freq >= 0.85*methodFreq) {
            out << ",color=orange";
        }
    }
    if (node->isDispatchNode()) {
        out << ",shape=diamond,color=blue";
    } else if (node->isExitNode()) {
        out << ",shape=ellipse,color=green";
    } 
    
    out << "]" << std::endl;
}

void HIRDotPrinter::printDotEdge(Edge* edge) {
    std::ostream& out = *os;
    Node *from = edge->getSourceNode();
    Node *to   = edge->getTargetNode();
    FlowGraph::printLabel(out, from);
    out << " -> ";
    FlowGraph::printLabel(out, to);
    out<<" [";
    out << "taillabel=\"" ;
        if (loopTree) {
        if (loopTree->isBackEdge(edge)) {
            out<<"(backedge)";
        } 
        if (loopTree->isLoopExit(edge)) {
            out<<"(loopexit)";
        }
    }
    if (fg.hasEdgeProfile()) {
        out<<"p:" << edge->getEdgeProb();
    }
    out <<"\"";
    if (to->isDispatchNode()) {
        out << ",style=dotted,color=blue";
    }
    out << "];" << std::endl;
}

} //namespace Jitrino 
