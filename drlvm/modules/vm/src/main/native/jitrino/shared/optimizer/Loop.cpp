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


#include "escapeanalyzer.h"
#include "Log.h"
#include "Inst.h"
#include "Dominator.h"
#include "Loop.h"
#include "globalopndanalyzer.h"
#include "optimizer.h"
#include "FlowGraph.h"

#include <algorithm>

namespace Jitrino {

DEFINE_SESSION_ACTION(LoopPeelingPass, peel, "Loop Peeling")

void
LoopPeelingPass::_run(IRManager& irm) {
    computeDominators(irm);
    DominatorTree* dominatorTree = irm.getDominatorTree();
    assert(dominatorTree->isValid());
    LoopBuilder lb(irm.getNestedMemoryManager(), irm, *dominatorTree, irm.getFlowGraph().hasEdgeProfile());
    lb.computeLoops(true);
    lb.peelLoops();
}

LoopBuilder::LoopBuilder(MemoryManager& mm, IRManager& irm, 
                         DominatorTree& d, bool useProfile) 
    : loopMemManager(mm), dom(d), irManager(irm), 
      instFactory(irm.getInstFactory()), fg(irm.getFlowGraph()), info(NULL), root(NULL), 
      useProfile(useProfile), needsSsaFixup(false), flags(*irm.getOptimizerFlags().loopBuilderFlags)
{
}

void LoopBuilder::readFlags(Action* argSource, LoopBuilderFlags* flags) {
    IAction::HPipeline p = NULL; //default pipeline for argSource
    flags->hoist_loads = argSource->getBoolArg(p, "loop.hoist_loads", false);
    flags->invert = argSource->getBoolArg(p, "loop.invert", false);
    flags->peel = argSource->getBoolArg(p, "loop.peel", true);
    flags->insideout_peeling = argSource->getBoolArg(p, "loop.insideout_peeling", false);
    flags->old_static_peeling = argSource->getBoolArg(p, "loop.old_static_peeling", false);
    flags->aggressive_peeling = argSource->getBoolArg(p, "loop.aggressive_peeling", true);
    flags->peel_upto_branch = argSource->getBoolArg(p, "loop.peel_upto_branch", true);
    flags->peeling_threshold = argSource->getIntArg(p, "loop.peeling_threshold", 2);
    flags->fullpeel = argSource->getBoolArg(p, "loop.fullpeel", false);
    flags->fullpeel_max_inst = argSource->getIntArg(p, "loop.fullpeel_max_inst", 40);
    flags->peel_upto_branch_no_instanceof = argSource->getBoolArg(p, "loop.peel_upto_branch_no_instanceof", true);

}

void LoopBuilder::showFlags(std::ostream& os) {
    os << "  loop builder flags:"<<std::endl;
    os << "    loop.hoist_loads[={on|OFF}] - peel assuming load hoisting" << std::endl;
    os << "    loop.invert[={on|OFF}]      - try to invert loops to reduce branches" << std::endl;
    os << "    loop.peel[={ON|off}]        - do peeling" << std::endl;
    os << "    loop.insideout_peeling[={on|OFF}]" << std::endl;
    os << "    loop.aggressive_peeling[={ON|off}] - be aggressive about peeling" << std::endl;
    os << "    loop.peel_upto_branch[={on|OFF}]   - peel only up to a branch" << std::endl;
    os << "    loop.old_static_peeling[={on|OFF}] - use old-style peeling for static runs" << std::endl;
    os << "    loop.peeling_threshold[=int]  - (default 2)" << std::endl;
    os << "    loop.peel_upto_branch_no_instanceof[={on|OFF}] - with peel_upto_branch, peel only up to a branch or instanceof" << std::endl;
}

template <class IDFun>
class LoopMarker {
public:
    static U_32 markNodesOfLoop(StlBitVector& nodesInLoop, Node* header, Node* tail);
private:
    static U_32 backwardMarkNode(StlBitVector& nodesInLoop, Node* node, StlBitVector& visited);
    static U_32 countInsts(Node* node);
};

template <class IDFun>
U_32 LoopMarker<IDFun>::countInsts(Node* node) {
    U_32 count = 0;
    Inst* first = (Inst*)node->getFirstInst();
    if (first != NULL) {
        for(Inst* inst = first->getNextInst(); inst != NULL; inst = inst->getNextInst())
            ++count;
    }
    return count;
}

template <class IDFun>
U_32 LoopMarker<IDFun>::backwardMarkNode(StlBitVector& nodesInLoop, 
                                Node* node, 
                                StlBitVector& visited) {
    static IDFun getId;
    if(visited.setBit(node->getId()))
        return 0;

    U_32 count = countInsts(node);
    nodesInLoop.setBit(getId(node));

    const Edges& inEdges = node->getInEdges();
    Edges::const_iterator eiter;
    for(eiter = inEdges.begin(); eiter != inEdges.end(); ++eiter) {
        Edge* e = *eiter;
        count += backwardMarkNode(nodesInLoop, e->getSourceNode(), visited);
    }
    return count;
}

// 
// traverse graph backward starting from tail until header is reached
//
template <class IDFun>
U_32 LoopMarker<IDFun>::markNodesOfLoop(StlBitVector& nodesInLoop,
                               Node* header,
                               Node* tail) {
    static IDFun getId;
    U_32 maxSize = ::std::max(getId(header), getId(tail));
    MemoryManager tmm("LoopBuilder::markNodesOfLoop.tmm");
    StlBitVector visited(tmm, maxSize);

    // mark header is visited
    U_32 count = countInsts(header);
    nodesInLoop.setBit(getId(header));
    visited.setBit(header->getId());

    // starting backward traversal
    return count + backwardMarkNode(nodesInLoop, tail, visited);
}



//
// the current loop contains the header
// we traverse descendants to find who contains the header and return the 
// descendant loop
//
LoopNode* LoopBuilder::findEnclosingLoop(LoopNode* loop, Node* header) {
    for (LoopNode* l = loop->getChild(); l != NULL; l = l->getSiblings())
        if (l->inLoop(header))
            return findEnclosingLoop(l, header);

    // if no descendant contains the header then return loop
    return loop;
}



bool noStoreOrSynch(ControlFlowGraph& fg) {
    const Nodes& nodes = fg.getNodes();
    Nodes::const_iterator niter;
    for(niter = nodes.begin(); niter != nodes.end(); ++niter) {
        Node* n = *niter;
        Inst* first = (Inst*)n->getFirstInst();
        for(Inst* inst = first->getNextInst(); inst != NULL; inst = inst->getNextInst()) {
            if (inst->getOperation().isStoreOrSync()) {
                return false;
            }
        }
    }
    return true;
}

bool LoopBuilder::isVariantOperation(Operation operation) {
    if (operation.isCheck())
        return false;
    if (operation.canThrow()) {
        return true;
    }
    if (!flags.hoist_loads) {
        if (operation.isLoad())
            return true;
    }
    if (operation.isCSEable()) {
        return false;
    }
    return true;
}


bool LoopBuilder::isVariantInst(Inst* inst, StlHashSet<Opnd*>& variantOpnds) {
    Operation operation = inst->getOperation();
    Opcode op = operation.getOpcode();

    bool isfinalload = false;
    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags();
    if (optimizerFlags.cse_final) {
        switch (inst->getOpcode()) {
        case Op_TauLdInd: 
            {
                Inst *srcInst = inst->getSrc(0)->getInst();
                if ((srcInst->getOpcode() == Op_LdFieldAddr) ||
                    (srcInst->getOpcode() == Op_LdStaticAddr)) {
                    FieldAccessInst *faInst = srcInst->asFieldAccessInst();
                    FieldDesc *fd = faInst->getFieldDesc();
                    if (fd->isInitOnly()) {

                        // first check for System stream final fields which vary
                        NamedType *td = fd->getParentType();
                        if (strncmp(td->getName(),"java/lang/System",20)==0) {
                            const char *fdname = fd->getName();
                            if ((strncmp(fdname,"in",5)==0) ||
                                (strncmp(fdname,"out",5)==0) ||
                                (strncmp(fdname,"err",5)==0)) {
                                break;
                            }
                        }

                        isfinalload = true;
                    }
                }
            } 
            break;
        case Op_LdStatic:
            {
                FieldAccessInst *inst1 = inst->asFieldAccessInst();
                assert(inst1);
                FieldDesc* fd = inst1->getFieldDesc();
                if (fd->isInitOnly()) {

                    // first check for System stream final fields which vary
                    NamedType *td = fd->getParentType();
                    if (strncmp(td->getName(),"java/lang/System",20)==0) {
                        const char *fdname = fd->getName();
                        if ((strncmp(fdname,"in",5)==0) ||
                            (strncmp(fdname,"out",5)==0) ||
                            (strncmp(fdname,"err",5)==0)) {
                            break;
                        }
                    }

                    isfinalload = true;
                }
            }
            break;
        case Op_TauLdField:
            {
                FieldAccessInst *inst1 = inst->asFieldAccessInst();
                assert(inst1);
                FieldDesc* fd = inst1->getFieldDesc();
                if (fd->isInitOnly()) {

                    // first check for System stream final fields which vary
                    NamedType *td = fd->getParentType();
                    if (strncmp(td->getName(),"java/lang/System",20)==0) {
                        const char *fdname = fd->getName();
                        if ((strncmp(fdname,"in",5)==0) ||
                            (strncmp(fdname,"out",5)==0) ||
                            (strncmp(fdname,"err",5)==0)) {
                            break;
                        }
                    }

                    isfinalload = true;
                }
            }
            break;
        default:
            break;
        };
    }

    // Inst has potential side effect
    if (isVariantOperation(operation) && !isfinalload)
        return true;

    // Is merge point.  Variant depending on incoming path.
    if (op == Op_LdVar || op == Op_Phi)
        return true;

    // Check for variant src.
    U_32 n = inst->getNumSrcOperands();
    for(U_32 i = 0; i < n; ++i) {
        Opnd* src = inst->getSrc(i);
        if (variantOpnds.find(src) != variantOpnds.end())
            return true;
    }

    return false;
}

void LoopBuilder::hoistHeaderInvariants(Node* preheader, Node* header, StlVector<Inst*>& invariantInsts) {
    assert(preheader->getOutDegree() == 1 && preheader->getOutEdges().front()->getTargetNode() == header);

    StlVector<Inst*>::iterator iiter;
    for(iiter = invariantInsts.begin(); iiter != invariantInsts.end(); ++iiter) {
        Inst* inst = *iiter;
        if (inst == header->getLastInst() && header->getOutDegree() > 1) {
            // Don't hoist control flow instruction.
            break;
        }
        inst->unlink();
        preheader->appendInst(inst);
    }
}


bool LoopBuilder::isInversionCandidate(Node* originalHeader, Node* header, StlBitVector& nodesInLoop, Node*& next, Node*& exit) {
    if(Log::isEnabled()) {
        Log::out() << "Consider rotating ";
        FlowGraph::printLabel(Log::out(), header);
        Log::out() << "  df = (" << (int) header->getDfNum() << ")" << std::endl;
    }
    assert(nodesInLoop.getBit(header->getId()));

    if (flags.aggressive_peeling) {
        if (header->getOutDegree() == 1) {
            next = header->getOutEdges().front()->getTargetNode();
            if(next->getInDegree() > 1 && next != originalHeader) {
                // A pre-header for an inner loop - do not peel.
                if (Log::isEnabled()) {
                    Log::out() << "Stop peeling node at ";FlowGraph::printLabel(Log::out(), header); Log::out()<<": preheader for inner loop. " << std::endl;
                }
                return false;
            }
            exit = NULL;
            return true;
        }
    }

    if (header->getOutDegree() != 2) {
        if (Log::isEnabled()) {
            Log::out() << "Stop peeling node at L";  FlowGraph::printLabel(Log::out(), header); Log::out() << ": no exit. " << std::endl;
        }
        return false;
    }

    Node* succ1 = header->getOutEdges().front()->getTargetNode();
    Node* succ2 = header->getOutEdges().back()->getTargetNode();
    
    // Check if either succ is an exit.
    if(nodesInLoop.getBit(succ1->getId())) {
        if(!nodesInLoop.getBit(succ2->getId())) {
            next = succ1;
            exit = succ2;
        } else {
            // Both succ's are in loop, no inversion.
            if (Log::isEnabled()) {
                Log::out() << "Stop peeling node at L";  FlowGraph::printLabel(Log::out(), header); Log::out()<< ": no exit. " << std::endl;
            }
            return false;
        }
    } else {
        // At least one succ of a header must be in the loop.
        assert(nodesInLoop.getBit(succ2->getId()));
        next = succ2;
        exit = succ1;
    }

    if(next->getInDegree() > 1 && next != originalHeader ) {
        // If next has more than one in edge, it must be the header
        // of an inner loop.  Do not peel its preheader.
        if (Log::isEnabled()) {
            Log::out()<< "Stop peeling node at L" ; FlowGraph::printLabel(Log::out(), header); Log::out() << ": preheader for inner loop. " << std::endl;
        }
        return false;
    }
    
    return true;
}

// Perform loop inversion for each recorded loop.
void LoopBuilder::peelLoops(StlVector<Edge*>& loopEdgesIn) {
    // Mark the temporaries that are local to a given basic block
    GlobalOpndAnalyzer globalOpndAnalyzer(irManager);
    globalOpndAnalyzer.doAnalysis();

    // Set def-use chains
    MemoryManager peelmem("LoopBuilder::peelLoops.peelmem");

    OpndManager& opndManager = irManager.getOpndManager();
    InstFactory& instFactory = irManager.getInstFactory();

    // Threshold at which a block is considered hot
    double heatThreshold = irManager.getHeatThreshold();

    StlVector<Edge*> loopEdges(peelmem);
    if(flags.insideout_peeling) {
        StlVector<Edge*>::reverse_iterator i = loopEdgesIn.rbegin(); while (i != loopEdgesIn.rend()) {
            loopEdges.insert(loopEdges.end(), *i);
            i++;
        }
    } else {
        StlVector<Edge*>::iterator i = loopEdgesIn.begin();
        while (i != loopEdgesIn.end()) {
            loopEdges.insert(loopEdges.end(), *i);
            i++;
        }
    }

    StlVector<Edge*>::iterator i;
    for(i = loopEdges.begin(); i != loopEdges.end(); ++i) {
        // The current back-edge
        Edge* backEdge = *i;

        // The initial header
        Node* header = backEdge->getTargetNode();
        if (header->isDispatchNode()) {
            continue;
        }
        assert(header->getInDegree() == 2);
        Node* originalInvertedHeader = header;

        // The initial loop end
        Node* tail = backEdge->getSourceNode();

        // The initial preheader
        Node* preheader = header->getInEdges().front()->getSourceNode();
        if (preheader == tail)
            preheader = header->getInEdges().back()->getSourceNode();

        // Temporary memory for peeling
        U_32 maxSize = fg.getMaxNodeId();
        MemoryManager tmm("LoopBuilder::peelLoops.tmm");
        
        // Compute nodes in loop
        StlBitVector nodesInLoop(tmm, maxSize);
        U_32 loopSize = LoopMarker<IdentifyByID>::markNodesOfLoop(nodesInLoop, header, tail);

        // Operand renaming table for duplicated nodes
        OpndRenameTable* duplicateTable = new (tmm) OpndRenameTable(tmm);

        // Perform loop inversion until header has no exit condition or 
        // until one iteration is peeled.
        Node* next;
        Node* exit;

        if (useProfile || !flags.old_static_peeling) {
            //
            // Only peel hot loops
            //
            if(useProfile) {
                double preheaderFreq = preheader->getExecCount();
                if(header->getExecCount() < heatThreshold || header->getExecCount() < preheaderFreq*flags.peeling_threshold || header->getExecCount() == 0)
                    continue;
            }
            
            //
            // Rotate loop one more time if the tail is not a branch (off by default)
            //
            Opcode op1 = ((Inst*)tail->getLastInst())->getOpcode();
            Opcode op2 = ((Inst*)header->getLastInst())->getOpcode();
            if(flags.invert && (op1 != Op_Branch) && (op2 != Op_Branch)) {
                if(isInversionCandidate(originalInvertedHeader, header, nodesInLoop, next, exit)) {
                    DefUseBuilder defUses(peelmem);
                    defUses.initialize(fg);
                    preheader = FlowGraph::tailDuplicate(irManager, preheader, header, defUses); 
                    tail = header;
                    header = next;
                    assert(tail->findTargetEdge(header) != NULL && preheader->findTargetEdge(header) != NULL);
                    if(tail->findTargetEdge(header) == NULL) {
                        tail =  tail->getUnconditionalEdge()->getTargetNode();
                        assert(tail != NULL && tail->findTargetEdge(header) != NULL);
                    }
                    originalInvertedHeader = header;
                }
            }
            
            //
            // Compute blocks to peel
            //
            Node* newHeader = header;
            Node* newTail = NULL;
            StlBitVector nodesToPeel(tmm, maxSize);
            if(flags.fullpeel) {
                if(loopSize <= flags.fullpeel_max_inst) {
                    nodesToPeel = nodesInLoop;
                    newTail = tail;
                }
            } else {
                while(isInversionCandidate(originalInvertedHeader, newHeader, nodesInLoop, next, exit)) {
                    if(Log::isEnabled()) {
                        Log::out() << "Peel ";
                        FlowGraph::printLabel(Log::out(), newHeader);
                        Log::out() << std::endl;
                    }
                    newTail = newHeader;
                    nodesToPeel.setBit(newHeader->getId());
                    newHeader = next;
                    if(newHeader == originalInvertedHeader) {
                        Log::out() << "One iteration peeled" << std::endl;
                        break;
                    }
                }
            }
            if(flags.peel) {
                header = newHeader;
                if(flags.peel_upto_branch) {
                    //
                    // Break final header at branch to peel more instructions
                    //
                    if(header != originalInvertedHeader) {
                        Inst* last = (Inst*)header->getLastInst();
                        if(header->getOutDegree() > 1)
                            last = last->getPrevInst();
                        if (flags.peel_upto_branch_no_instanceof) {
                            Inst *first = (Inst*)header->getFirstInst();
                            while ((first != last) && (first->getOpcode() != Op_TauInstanceOf)) {
                                first = first->getNextInst();
                            }
                            last = first;
                        }
                        Node* sinkNode = fg.splitNodeAtInstruction(last, true, false, instFactory.makeLabel());
                        newTail = header;
                        nodesToPeel.resize(sinkNode->getId()+1);
                        nodesToPeel.setBit(header->getId());
                        if(Log::isEnabled()) {
                            Log::out() << "Sink Node = ";
                            FlowGraph::printLabel(Log::out(), sinkNode);
                            Log::out() << ", Header = ";
                            FlowGraph::printLabel(Log::out(), header);
                            Log::out() << ", Original Header = ";
                            FlowGraph::printLabel(Log::out(), originalInvertedHeader);
                            Log::out() << std::endl;
                        }
                        header = sinkNode;
                    }
                }
                
                //
                // Peel the nodes
                //
                if(newTail != NULL) {
                    DefUseBuilder defUses(peelmem);
                    defUses.initialize(fg);
                    Edge* entryEdge = preheader->findTargetEdge(originalInvertedHeader);
                    double peeledFreq = preheader->getExecCount() * entryEdge->getEdgeProb(); 
                    Node* peeled = FlowGraph::duplicateRegion(irManager, originalInvertedHeader, nodesToPeel, defUses, peeledFreq);
                    assert(peeled->getInDegree() <= 1);
                    // Break cycle and link to original loop.
                    if (peeled->getInDegree() == 1) {
                        fg.replaceEdgeTarget(peeled->getInEdges().front(), header);
                    }
                    // Link duplicated region.
                    fg.replaceEdgeTarget(entryEdge, peeled);
                    if(newTail->findTargetEdge(header) == NULL) {
                        //
                        // An intervening stVar block was added to promote a temp to a var 
                        // in the duplicated block.  The new tail should be the stVar block.
                        //
                        tail =  newTail->getUnconditionalEdge()->getTargetNode();
                        assert(tail != NULL && tail->findTargetEdge(header) != NULL);
                    } else {
                        tail = newTail;
                    }
                    assert(header->getInDegree() == 2);
                    preheader = header->getInEdges().front()->getSourceNode();
                    if(preheader == tail)
                        preheader = header->getInEdges().back()->getSourceNode();            
                }            
            }
            if(preheader->getOutDegree() > 1) {
                Edge* edge = preheader->findTargetEdge(header);
                assert(edge != NULL);
                preheader = fg.spliceBlockOnEdge(edge, instFactory.makeLabel());
            }

        } else {            
            while(isInversionCandidate(header, header, nodesInLoop, next, exit)) {
                if(Log::isEnabled()) {
                    Log::out() << "Consider peeling node at L"; FlowGraph::printLabel(Log::out(), header); Log::out() << std::endl;
                }
                assert(header->isBlockNode());
                assert(header->getInDegree() == 2);            
                assert(header->findSourceEdge(preheader) != NULL);
                assert(header->findSourceEdge(tail) != NULL);
                
                // Find variant instructions.  Invariant instructions need not be duplicated.
                StlVector<Inst*> variantInsts(tmm);
                StlHashSet<Opnd*> variantOpnds(tmm);
                StlVector<Inst*> invariantInsts(tmm);
                Inst* first = (Inst*)header->getFirstInst();
                Inst* inst;
                for(inst = first->getNextInst(); inst != NULL; inst = inst->getNextInst()) {
                    if (isVariantInst(inst, variantOpnds)) {
                        if (Log::isEnabled()) {
                            Log::out() << "Inst ";
                            inst->print(Log::out());
                            Log::out() << " is variant" << std::endl;
                        }
                        variantInsts.push_back(inst);
                        Opnd* dst = inst->getDst();
                        variantOpnds.insert(dst);
                    } else {
                        if (Log::isEnabled()) {
                            Log::out() << "Inst ";
                            inst->print(Log::out());
                            Log::out() << " is invariant" << std::endl;
                        }
                        invariantInsts.push_back(inst);
                    }
                }
                
                // Heuristic #0: If no invariant in header, don't peel.
                if (invariantInsts.empty()) {
                    Log::out() << "Peeling heuristic #0, no invariant" << std::endl;
                    break;
                }
                
                // Heuristic #1: If the last instruction is a variant check, stop peeling.
                Inst* last = (Inst*)header->getLastInst();
                if (last->getOperation().isCheck() && !variantInsts.empty() && last == variantInsts.back() &&
                   !(last->getDst()->isNull() 
                     || (last->getDst()->getType()->tag == Type::Tau))) {
                    hoistHeaderInvariants(preheader, header, invariantInsts);
                    Log::out() << "Peeling heuristic #1, last inst is variant check" << std::endl;
                    break;
                }
                // Heuristic #2: If a defined tmp may be live on exit, don't peel this node.
                // Promoting such tmps to vars is expensive, since we need to find all uses.
                StlVector<Inst*> globalInsts(tmm);
                Opnd* unhandledGlobal = NULL;
                bool hasExit = exit != NULL;
                bool exitIsUnwind = hasExit && (exit == fg.getUnwindNode());
                bool exitIsNotDominated = !hasExit ||
                    (dom.hasDomInfo(header) && dom.hasDomInfo(exit) && !dom.dominates(header, exit));
                StlVector<Inst*>::iterator iiter;
                for(iiter = variantInsts.begin(); iiter != variantInsts.end(); ++iiter) {
                    Inst* inst = *iiter;
                    Opnd* dst = inst->getDst();
                    if (dst->isGlobal() && !dst->isVarOpnd() && !dst->isNull()) {
                        // Defined global temp.
                        if(exitIsNotDominated || exitIsUnwind || (inst == last && exit->isDispatchNode())) {
                            globalInsts.push_back(inst);
                        } else {
                            unhandledGlobal = dst;
                            break;
                        }
                    }
                }
                if (unhandledGlobal != NULL) {
                    if (Log::isEnabled()) {
                        Log::out() << "Stop peeling node at " ; FlowGraph::printLabel(Log::out(), header); Log::out() << ": unhandled global operand. ";
                        unhandledGlobal->print(Log::out());
                        Log::out() << std::endl;
                    }
                    hoistHeaderInvariants(preheader, header, invariantInsts);
                    break;
                }
                
                // Duplicate instructions
                Node* peeled = fg.createBlockNode(instFactory.makeLabel());
                if (Log::isEnabled()) {
                    Log::out() << "Peeling node "; FlowGraph::printLabel(Log::out(), header); Log::out() << " as ";  FlowGraph::printLabel(Log::out(), peeled); Log::out() << std::endl;
                }
                Inst* nextInst;
                // Peel instructions to peeled block.  Leave clones of variant insts.
                for(inst = first->getNextInst(), iiter = variantInsts.begin(); inst != NULL; inst = nextInst) {
                    nextInst = inst->getNextInst();
                    if (iiter != variantInsts.end() && inst == *iiter) {
                        // Make clone in place.
                        ++iiter;
                        Inst* clone = instFactory.clone(inst, opndManager, duplicateTable);
                        clone->insertBefore(nextInst);
                        inst->unlink();
                        if (Log::isEnabled()) {
                            Log::out() << "Peeling: copying ";
                            inst->print(Log::out());
                            Log::out() << " to ";
                            clone->print(Log::out());
                            Log::out() << std::endl;
                        }
                    } else {
                        if(inst->getOperation().canThrow())
                            FlowGraph::eliminateCheck(fg, header, inst, false);
                        else
                            inst->unlink();
                        if (Log::isEnabled()) {
                            Log::out() << "Peeling: not copying ";
                            inst->print(Log::out());
                            Log::out() << std::endl;
                        }
                    }
                    peeled->appendInst(inst);
                }
                assert(iiter == variantInsts.end());
                
                fg.replaceEdgeTarget(preheader->findTargetEdge(header), peeled);
                if (hasExit) {
                    fg.addEdge(peeled, exit);
                }
                fg.addEdge(peeled, next);
                if (!globalInsts.empty()) {
                    OpndRenameTable* patchTable = new (tmm) OpndRenameTable(tmm);
                    
                    // Need to patch global defs.  Create store blocks and merge at next
                    if (Log::isEnabled()) {
                        Log::out() << "Merging global temps in node ";FlowGraph::printLabel(Log::out(), header); Log::out()<<" and ";FlowGraph::printLabel(Log::out(), peeled);Log::out()<<std::endl;
                    }
                    Node* newpreheaderSt = fg.createBlockNode(instFactory.makeLabel());
                    Node* newtailSt = fg.createBlockNode(instFactory.makeLabel());
                    fg.replaceEdgeTarget(peeled->findTargetEdge(next), newpreheaderSt);
                    fg.addEdge(newpreheaderSt, next);
                    fg.replaceEdgeTarget(header->findTargetEdge(next), newtailSt);
                    fg.addEdge(newtailSt, next);
                    
                    StlVector<Inst*>::iterator i;
                    for(i = globalInsts.begin(); i != globalInsts.end(); ++i) {
                        Inst* inst = *i;
                        Opnd* dst = inst->getDst();
                        Opnd* dstPreheader = opndManager.createSsaTmpOpnd(dst->getType());
                        Opnd* dstTail = duplicateTable->getMapping(dst);
                        inst->setDst(dstPreheader);
                        VarOpnd* dstVar;
                        
                        if ((inst->getOpcode() == Op_LdVar) && (variantOpnds.find(inst->getSrc(0)) == variantOpnds.end())) {
                            // If dst is generated from a LdVar, reuse that var instead of promoting to a new var.
                            dstVar = inst->getSrc(0)->asVarOpnd();
                            assert(dstVar != NULL);
                        } else {
                            // Create a new var and initialize it the original and duplicated blocks.
                            dstVar = opndManager.createVarOpnd(dst->getType(), false);
                            Inst* stPreheader = instFactory.makeStVar(dstVar, dstPreheader);
                            newpreheaderSt->appendInst(stPreheader);
                            Inst* stTail = instFactory.makeStVar(dstVar, dstTail);
                            newtailSt->appendInst(stTail);
                        }
                        
                        Inst* ldVar = instFactory.makeLdVar(dst, dstVar);
                        next->prependInst(ldVar);
                        
                        patchTable->setMapping(dst, dstPreheader);
                    }
                    FlowGraph::renameOperandsInNode(peeled, patchTable);
                    
                    // Rotate
                    preheader = newpreheaderSt;
                    tail = newtailSt;
                } else {
                    if (peeled->getOutDegree() > 1) {
                        // Remove critical edge
                        preheader = fg.createBlockNode(instFactory.makeLabel());
                        fg.replaceEdgeTarget(peeled->findTargetEdge(next), preheader);
                        fg.addEdge(preheader, next);
                    } else {
                        preheader = peeled;
                    }
                    tail = header;
                }
                
                // Prepare to peel next node.
                header = next;
                if (flags.aggressive_peeling) {
                    if (header == originalInvertedHeader) {
                        if (Log::isEnabled()) {
                            Log::out()<<"Stop peeling node at ";FlowGraph::printLabel(Log::out(), header); Log::out() << ": peeled one iteration" << std::endl;
                        }
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        assert(header->getInDegree() == 2);
        backEdge = header->findSourceEdge(tail);
        assert(backEdge != NULL);
        *i = backEdge;
    }
    loopEdgesIn.clear();

    if(flags.insideout_peeling) {
        StlVector<Edge*>::reverse_iterator i = loopEdges.rbegin();
        while (i != loopEdges.rend()) {
            loopEdgesIn.insert(loopEdgesIn.end(), *i);
            i++;
        }
    } else {
        StlVector<Edge*>::iterator i = loopEdges.begin();
        while (i != loopEdges.end()) {
            loopEdgesIn.insert(loopEdgesIn.end(), *i);
            i++;
        }
    }
}

class EdgeCoalescerCallbackImpl : public EdgeCoalescerCallback {
friend class LoopBuilder;
public:
    EdgeCoalescerCallbackImpl(IRManager& _irm) : ssaAffected(false), irm(_irm){}
    
    virtual void coalesce(Node* header, Node* newPreHeader, U_32 numEdges) {
        InstFactory& instFactory = irm.getInstFactory();
        OpndManager& opndManager = irm.getOpndManager();
        Inst* labelInst = (Inst*)header->getFirstInst();
        assert(labelInst->isLabel() && !labelInst->isCatchLabel());
        newPreHeader->appendInst(instFactory.makeLabel());
        newPreHeader->getFirstInst()->setBCOffset(labelInst->getBCOffset());
        if (numEdges > 1 ) {
            for (Inst* phi = labelInst->getNextInst();phi!=NULL && phi->isPhi(); phi = phi->getNextInst()) {
                Opnd *orgDst = phi->getDst();
                SsaVarOpnd *ssaOrgDst = orgDst->asSsaVarOpnd();
                assert(ssaOrgDst);
                VarOpnd *theVar = ssaOrgDst->getVar();
                assert(theVar);
                SsaVarOpnd* newDst = opndManager.createSsaVarOpnd(theVar);
                Inst *newInst = instFactory.makePhi(newDst, 0, 0);
                PhiInst *newPhiInst = newInst->asPhiInst();
                assert(newPhiInst);
                U_32 n = phi->getNumSrcOperands();
                for (U_32 i=0; i<n; i++) {
                    instFactory.appendSrc(newPhiInst, phi->getSrc(i));
                }
                PhiInst *phiInst = phi->asPhiInst();
                assert(phiInst);
                instFactory.appendSrc(phiInst, newDst);
                newPreHeader->appendInst(newInst);
                ssaAffected = true;
            }
        }
    }

private:
    bool ssaAffected;
    IRManager& irm;
};

void LoopBuilder::computeLoops(bool normalize) {
    // find all loop headers
    LoopTree* lt = irManager.getLoopTree();
    if (lt == NULL) {
        MemoryManager& mm = irManager.getMemoryManager();
        EdgeCoalescerCallbackImpl* c = new (mm) EdgeCoalescerCallbackImpl(irManager);
        lt = new LoopTree(irManager.getMemoryManager(), &irManager.getFlowGraph(),c);
        irManager.setLoopTree(lt);
    }
    EdgeCoalescerCallbackImpl* callback = (EdgeCoalescerCallbackImpl*)lt->getCoalesceCallback();
    callback->ssaAffected = false;
    lt->rebuild(normalize);
    needsSsaFixup = callback->ssaAffected;
}

void LoopBuilder::peelLoops() {
    LoopTree*lt = irManager.getLoopTree();
    if (!lt->hasLoops())  {
        return;
    }

    MemoryManager tmm("LoopBuilder::peelLoops");
    Edges loopEdges(tmm);
    const Nodes& nodes = irManager.getFlowGraph().getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        const Edges& edges = node->getOutEdges();
        for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
            Edge* edge = *ite;
            if (lt->isBackEdge(edge)) {
                loopEdges.push_back(edge);
            }
        }
    }
    peelLoops(loopEdges);
    if (Log::isEnabled()) 
        FlowGraph::printHIR(Log::out(), irManager.getFlowGraph(), irManager.getMethodDesc());


    if (needSsaFixup()) {
        OptPass::fixupSsa(irManager);
    }
    OptPass::smoothProfile(irManager);
}
}//namespace Jitrino 
