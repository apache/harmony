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
#include "Dominator.h"
#include "Loop.h"
#include "globalcodemotion.h"
#include "CSEHash.h"
#include "Opcode.h"
#include "constantfolder.h"
#include "walkers.h"
#include "optimizer.h"
#include "hashvaluenumberer.h"
#include "aliasanalyzer.h"
#include "memoryopt.h"
#include "FlowGraph.h"
#include "optpass.h"

namespace Jitrino {

    static void printLabel(std::ostream& os, DominatorNode* dNode) {
        FlowGraph::printLabel(os, dNode->getNode());
    }

DEFINE_SESSION_ACTION(GlobalCodeMotionPass, gcm, "Global Code Motion")

void GlobalCodeMotionPass::_run(IRManager& irm) {
    OptPass::splitCriticalEdges(irm);
    OptPass::computeDominatorsAndLoops(irm);
    MemoryManager& memoryManager = irm.getNestedMemoryManager();
    DominatorTree* dominatorTree = irm.getDominatorTree();
    LoopTree* loopTree = irm.getLoopTree();
    assert(dominatorTree->isValid() && loopTree->isValid());
    GlobalCodeMotion gcm(irm, memoryManager, *dominatorTree, loopTree);
    gcm.runPass();
}

DEFINE_SESSION_ACTION(GlobalValueNumberingPass, gvn, "Global Value Numbering")

void GlobalValueNumberingPass::_run(IRManager& irm) {
    OptPass::computeDominatorsAndLoops(irm);
    DominatorTree* dominatorTree = irm.getDominatorTree();
    assert(dominatorTree && dominatorTree->isValid());
    LoopTree* loopTree = irm.getLoopTree();
    assert(loopTree && loopTree->isValid());
    MemoryManager& memoryManager = irm.getNestedMemoryManager();
    ControlFlowGraph& flowGraph = irm.getFlowGraph();

    DomFrontier frontier(memoryManager,*dominatorTree,&flowGraph);
    TypeAliasAnalyzer aliasAnalyzer;
    MemoryOpt mopt(irm, memoryManager, *dominatorTree,
        frontier, loopTree, &aliasAnalyzer);
    mopt.runPass();
    HashValueNumberer valueNumberer(irm, *dominatorTree);
    valueNumberer.doGlobalValueNumbering(&mopt);
}



GlobalCodeMotion::GlobalCodeMotion(IRManager &irManager0, 
                                   MemoryManager& memManager,
                                   DominatorTree& dom0,
                                   LoopTree *loopTree0)
    : irManager(irManager0), 
      fg(irManager0.getFlowGraph()),
      methodDesc(irManager0.getMethodDesc()),
      mm(memManager),
      dominators(dom0),
      loopTree(loopTree0),
      flags(*irManager.getOptimizerFlags().gcmFlags),
      earliest(memManager),
      latest(memManager),
      visited(memManager),
      uses(memManager)
{
}

GlobalCodeMotion::~GlobalCodeMotion()
{
}

void 
GlobalCodeMotion::readFlags(Action* argSource, GcmFlags* flags) {
    IAction::HPipeline p = NULL; //default pipeline for argSource
    flags->dry_run = argSource->getBoolArg(p, "gcm.dry_run", false);
    flags->sink_stvars = argSource->getBoolArg(p, "gcm.sink_stvars", false);
    flags->min_cut = argSource->getBoolArg(p, "gcm.min_cut", false);
    flags->sink_constants = argSource->getBoolArg(p, "gcm.sink_constants", false);
}

void GlobalCodeMotion::showFlags(std::ostream& os) {
    os << "  global code motion flags:"<<std::endl;
    os << "    gcm.dry_run[={on|OFF}]        - don't really move anything" << std::endl;
    os << "    gcm.sink_stvars[={on|OFF}]    - raise ldvar, sink stvar to Phi nodes"  << std::endl;
    os << "    gcm.min_cut[={on|OFF}]        - duplicate placement on mincut between def and uses" << std::endl;
    os << "    gcm.sink_constants[={ON|off}] - sink constants after placement" << std::endl;
}

void GlobalCodeMotion::runPass() {
    if (flags.dry_run && !flags.sink_constants) return;

    if (Log::isEnabled()) {
        Log::out() << "IR before GCM pass" << std::endl;
        FlowGraph::printHIR(Log::out(), irManager.getFlowGraph(), methodDesc);
        FlowGraph::printDotFile(irManager.getFlowGraph(), methodDesc, "beforegcm");
        dominators.printDotFile(methodDesc, "beforegcm.dom");
        dominators.printIndentedTree(Log::out(), "beforegcm.dom");
        loopTree->printDotFile(methodDesc, "beforegcm.loop");
    }

    if (!flags.dry_run) {
        // schedule instructions early
        scheduleAllEarly();
        // now earliest[i] = domNode for earliest placement.

        clearVisited();

        scheduleAllLate();
    }

    // sink loads of constants, since IPF back-end doesn't re-materialize
    if (flags.sink_constants)
        sinkAllConstants();
    // patch up SSA form in case live ranges overlap now
    // for now, we don't move Phis, Ldvars, or Stvars, so Ssa form is unaffected
    // fixSsaForm();

    if (Log::isEnabled() ) {
        Log::out() << "IR after GCM pass" << std::endl;
        FlowGraph::printHIR(Log::out(),irManager.getFlowGraph(), methodDesc);
        FlowGraph::printDotFile( irManager.getFlowGraph(), methodDesc, "aftergcm");
        dominators.printDotFile(methodDesc, "aftergcm.dom");
    }
}

// EARLY SCHEDULING

// a DomNodeInstWalker, forwards/preorder
class GcmScheduleEarlyWalker {
    GlobalCodeMotion *thePass;
    Node *block;
    DominatorNode *domNode;
public:
    void startNode(DominatorNode *domNode0) { 
        domNode = domNode0;
        block = domNode->getNode();
        if (Log::isEnabled() ) {
            Log::out() << "Begin early scheduling of block ";
            FlowGraph::print(Log::out(), block);
            Log::out() << std::endl;
        }    
    };
    void applyToInst(Inst *i) { thePass->scheduleEarly(domNode, i);  };
    void finishNode(DominatorNode *domNode) { 
        Node *block1 = domNode->getNode();
        if( !(block == block1) ) assert(0);
        if (Log::isEnabled() ) {
            Log::out() << "Done early scheduling of block ";
            FlowGraph::print(Log::out(), block);
            Log::out() << std::endl;
        }    
    };
    
    GcmScheduleEarlyWalker(GlobalCodeMotion *thePass0)
        : thePass(thePass0), block(0), domNode(0)
    {
    };
};

void GlobalCodeMotion::scheduleAllEarly()
{
    GcmScheduleEarlyWalker scheduleEarlyWalker(this);

    // adapt the DomNodeInstWalker to a forwards DomWalker
    typedef DomNodeInst2DomWalker<true, GcmScheduleEarlyWalker> EarlyDomInstWalker;
    EarlyDomInstWalker domInstWalker(scheduleEarlyWalker);
    
    // do the walk, pre-order
    DomTreeWalk<true, EarlyDomInstWalker>(dominators, domInstWalker, mm);
}

// since it will usually visit in the right order, we used dominator tree order
// BUT: since GVN may leave instructions out of place, we can't count on
// operands having been visited before the instruction.
// THOUGH: we guarantee the head of each cycle (when placed legally) will
// be a Phi function, and thus pinned.  This means that for non-Phi instrs,
// we can just place all operands first, then this instruction.
// For Phi instructions, we must just leave them in place (pinned) and leave
// the operands for later visiting.

// domNode should be 0 or i's DominatorNode, if we happen to know it.
void GlobalCodeMotion::scheduleEarly(DominatorNode *domNode, Inst *i)
{
    if (i->isLabel()) return;
    if (alreadyVisited(i)) {
        /* if (Log::isEnabled() ) {
        Log::out() << "= Inst is already scheduled early: "; i->print(Log::out());
        Log::out() << std::endl;
        }
        */
        return;
    }

    Node *cfgNode = i->getNode();
    if (domNode==NULL) {
        domNode = dominators.getDominatorNode(cfgNode);
    }
    bool pinned = isPinned(i);
    markAsVisited(i);

    if (Log::isEnabled() ) {
        Log::out() << "< Beginning early scheduling of " << (pinned?"":"not");
        Log::out() << " pinned inst ";  i->print(Log::out());
        Log::out() << std::endl;
    }

    if (pinned) {       // leave it here
        // We place operands before placing instruction, to get ordering right
        // in the block; in normal code, operands should appear first and 
        // have been already placed, but after GVN we don't have that guarantee.
        if (i->getOpcode() != Op_Phi) { // phi operands must also be pinned, skip them.
            if (Log::isEnabled() ) {
                Log::out() << " Inst is not a Phi, processing operands" << std::endl;
            }
            // for others, record uses in this instruction
            U_32 numSrcs = i->getNumSrcOperands();
            for (U_32 srcNum=0; srcNum < numSrcs; ++srcNum) {
                Opnd *srcOpnd = i->getSrc(srcNum);
                Inst *srcInst = srcOpnd->getInst();
                scheduleEarly(0, srcInst);
                if (Log::isEnabled() ) {
                    Log::out() << " Recording that "; srcInst->print(Log::out());
                    Log::out() << " is used by "; i->print(Log::out());
                    Log::out() << std::endl;
                }
                UsesMap::iterator it = uses.find(srcInst);
                UsesSet* theSet = NULL; 
                if (it == uses.end()) {
                    theSet = new (mm) UsesSet(mm);
                    it = uses.insert(::std::make_pair(srcInst, theSet)).first;
                }
                theSet = it->second;
                theSet->insert(i);
            }
        }

        // now place instruction
        earliest[i] = domNode;

        if (i->getOperation().canThrow()) {
            // as a special case, mark it as being in the normal successor node;
            // this will cause dependent instructions to be placed correctly.
            Node *peiNode = earliest[i]->getNode();

            const Edges &outedges = peiNode->getOutEdges();
            typedef Edges::const_iterator EdgeIter;
            EdgeIter eLast = outedges.end();
            DominatorNode *newDom = 0;
            for (EdgeIter eIter = outedges.begin(); eIter != eLast; eIter++) {
                Edge *outEdge = *eIter;
                Edge::Kind kind = outEdge->getKind();
                if (kind != Edge::Kind_Dispatch) {
                    Node *succBlock = outEdge->getTargetNode();
                    newDom = dominators.getDominatorNode(succBlock);
                    break;
                }
            }        
            if (newDom) {
                earliest[i] = newDom;
            }
        }

    } else {
        // not pinned, figure out earliest placement
        DominatorNode *currentEarliest = dominators.getDominatorRoot();
        DominatorNode *srcInstEarliest = NULL;
        U_32 numSrcs = i->getNumSrcOperands();
        for (U_32 srcNum=0; srcNum < numSrcs; ++srcNum) {
            Opnd *srcOpnd = i->getSrc(srcNum);
            Inst *srcInst = srcOpnd->getInst();
            scheduleEarly(0, srcInst);
            srcInstEarliest = earliest[srcInst];
            // Note that both currentEarliest and srcInstEarliest dominate this instruction.
            // Thus one of them must dominate the other.
            // The dominated one is the earliest place we can put this.
            if (currentEarliest->isAncestorOf(srcInstEarliest)) {
                // must move currentEarliest down to have srcOpnd available
                currentEarliest = srcInstEarliest;
            }
            if (Log::isEnabled() ) {
                Log::out() << " Inst uses ";    srcInst->print(Log::out());
                Log::out() << ", which has early placement in "; FlowGraph::printLabel(Log::out(), srcInstEarliest);
                Log::out() << std::endl;
            }
            // record the use while we're iterating.
            UsesMap::iterator it = uses.find(srcInst);
            UsesSet* theSet = NULL; 
            if (it == uses.end()) {
                theSet = new (mm) UsesSet(mm);
                it = uses.insert(::std::make_pair(srcInst, theSet)).first;
            }
            theSet = it->second;
            theSet->insert(i);
        }
        srcInstEarliest = currentEarliest;  // now is the latest placement of src insts
        // moving above catch inst may cause problems in code emitter
        // check that we do not cross catch boundaries
        // find earliest intermediate basic block,  starting from the block containing i
        DominatorNode *instEarliest = domNode;
        for (currentEarliest = domNode; ; currentEarliest = currentEarliest->getParent()) {
            if(currentEarliest==0){
	    	break;
	    }
	
	    Node *candidateNode = currentEarliest->getNode();
	    
	    if (Log::isEnabled() ) {
                Log::out() << "  trying node "; FlowGraph::printLabel(Log::out(), candidateNode);
            }
            // intermediate basic blocks with catch labels are
            // inappropriate to place instructions in  
            if (candidateNode->isDispatchNode()) {
                if (Log::isEnabled() ) {
                    Log::out() << "  ...  isDispatchNode";      Log::out() << std::endl;
                }
                break;
            }
            LabelInst *first = (LabelInst*)(candidateNode->getFirstInst());
            Inst* next=first->getNextInst();
            if ((next !=NULL) && (next->getOpcode() == Op_Catch)) {
                if (Log::isEnabled() ) {
                    Log::out() << "  ...  getOpcode() == Op_Catch";     Log::out() << std::endl;
                }
                instEarliest=currentEarliest;
                break;
            }
            if (first->isCatchLabel()) {
                if (Log::isEnabled() ) {
                    Log::out() << "  ...  isCatchLabel";        Log::out() << std::endl;
                }
                break;
            }
            if (Log::isEnabled() ) {
                Log::out() << "  ...  OK";      Log::out() << std::endl;
            }
            instEarliest=currentEarliest;
            if (currentEarliest==srcInstEarliest) {
                break;
            }
        } // end for
        earliest[i] = instEarliest;
    }

    if (Log::isEnabled() ) {
        Log::out() << "> Done earliest placement of "; i->print(Log::out());
        Log::out() << " is in block ";  FlowGraph::printLabel(Log::out(), earliest[i]);            
        Log::out() << std::endl;
    }
}

// LATE SCHEDULING

// a domNodeInstWalker, backwards/postorder
class GcmScheduleLateWalker {
    GlobalCodeMotion *thePass;
    Node *block;
    DominatorNode *domNode;
public:
    void startNode(DominatorNode *domNode0) { 
        domNode = domNode0;
        block = domNode->getNode();
        if (Log::isEnabled() ) {
            Log::out() << "Begin late scheduling of block ";
            FlowGraph::print(Log::out(), block);
            Log::out() << std::endl;
        }    
    };
    void applyToInst(Inst *i) { thePass->scheduleLate(domNode, 0, i); };
    void finishNode(DominatorNode *domNode) { 
        Node *block1 = domNode->getNode();
        if( !(block == block1) ) assert(0);;
        if (Log::isEnabled() ) {
            Log::out() << "Done late scheduling of block ";
            FlowGraph::print(Log::out(), block);
            Log::out() << std::endl;
        }    
    };
    GcmScheduleLateWalker(GlobalCodeMotion *thePass0)
        : thePass(thePass0), block(0), domNode(0)
    {
    };
};

void GlobalCodeMotion::scheduleAllLate()
{
    GcmScheduleLateWalker scheduleLateWalker(this);

    // adapt the DomNodeInstWalker to a forwards DomWalker
    typedef DomNodeInst2DomWalker<false, GcmScheduleLateWalker> LateDomInstWalker;
    LateDomInstWalker domInstWalker(scheduleLateWalker);

    DomTreeWalk<false, LateDomInstWalker>(dominators, domInstWalker, mm);
}

// Note that we are visiting instructions in a block in reverse order.
// It is safe to modify anything below the current instruction.
// as long as no using* instruction precedes this instruction in the same block, we
// are safe in recursively placing use.

// basei, if non-0, is the instruction where we are current iterating, and domNode is
// its node.  If i is where we are iterating, then domNode is its node and basei is 0.
void GlobalCodeMotion::scheduleLate(DominatorNode *domNode, Inst *basei, Inst *i)
{
    if (i->isLabel()) return;
    if (alreadyVisited(i)) return;

    if (domNode==NULL) {
        domNode = dominators.getDominatorNode(i->getNode());
    }

    markAsVisited(i);

    if (Log::isEnabled() ) {
        Log::out() << "< Beginning late scheduling of inst ";
        i->print(Log::out());
        Log::out() << std::endl;
    }

    bool pinned = isPinned(i);
    // figure out where to put it, first scheduling users along the way

    // schedule users
    DominatorNode *lca = 0;
    UsesMap::iterator it = uses.find(i);
    UsesSet* users = NULL;

    if (it == uses.end()) {
        users = new (mm) UsesSet(mm);
        it = uses.insert(::std::make_pair(i, users)).first;
    }

    users = it->second;
    UsesSet::iterator uiter = users->begin();
    UsesSet::iterator uend = users->end();

    for ( ; uiter != uend; ++uiter) {
        Inst *useri = *uiter;

        // schedule use
        scheduleLate(domNode, basei, useri);

        if (Log::isEnabled() ) {
            Log::out() << "Inst ";  i->print(Log::out());
            Log::out() << " is used by inst ";  useri->print(Log::out());
            Log::out() << " which is in "; FlowGraph::printLabel(Log::out(), latest[useri]);
            Log::out() << std::endl;
        }

        if (pinned) continue;
        DominatorNode *usingBlock = latest[useri];
        lca = leastCommonAncestor(lca, usingBlock);
        while (lca->getNode()->isDispatchNode()) {
            lca = lca->getParent(); // code can't be placed in dispatch nodes
        }
    }

    if (pinned) {
        // if following instruction is not already scheduled,
        // then we are scheduling this pinned instruction as a use of some
        // other instruction.  Before scheduling it, we must schedule any
        // following pinned instruction.
        Inst *nextInst = i->getNextInst();
        while ((nextInst!=NULL) && !alreadyVisited(nextInst)) {
            if (isPinned(nextInst)) {
                if (Log::isEnabled() ) {
                    Log::out() << "Pinned Inst "; i->print(Log::out());
                    Log::out() << " is followed by unscheduled pinned inst ";
                    nextInst->print(Log::out());
                    Log::out() << std::endl;
                }
                scheduleLate(domNode, basei, nextInst);
                // scheduling nextInst should result in scheduling
                // any following pinned inst, so break out of loop.
                break; 
            } else {
                if (Log::isEnabled() ) {
                    Log::out() << "Pinned Inst "; i->print(Log::out());
                    Log::out() << " is followed by unscheduled unpinned inst ";
                    nextInst->print(Log::out());
                    Log::out() << std::endl;
                }
                nextInst = nextInst->getNextInst();
            }
        }
        if (Log::isEnabled() ) {
            Log::out() << "Inst is pinned, left in place: "; i->print(Log::out());
            Log::out() << std::endl;
        }
        latest[i] = dominators.getDominatorNode(i->getNode()); // leave it here.
    } else if (lca == 0) {
        // if LCA==0, then there are no uses; i is dead.
        if (Log::isEnabled() ) {
            Log::out() << "Inst has no uses, left in place: "; i->print(Log::out());
            Log::out() << std::endl;
        }
        latest[i] = dominators.getDominatorNode(i->getNode()); // leave it here.
    } else {
        // now LCA is leastCommonAncestor of all uses
        if (i->isLdVar()) {
            // moving ldVar late is dangerous: can jump over stVar for the same var
            if (domNode->isAncestorOf(lca)) {
                lca=domNode;
            }
        }
        Node* lcaNode = lca->getNode();
        DominatorNode *best = lca;
        Node* bestNode = best->getNode();
        DominatorNode *earliestBlock = earliest[i];
        // code can't be placed in dispatch nodes:
        assert(!earliestBlock->getNode()->isDispatchNode());
        if (Log::isEnabled() ) {
            Log::out() << " LCA is "; FlowGraph::printLabel(Log::out(), lca);
            Log::out() << ", best is "; printLabel(Log::out(), best);
            Log::out() << " earliestBlock is "; printLabel(Log::out(), earliestBlock);
            Log::out() << std::endl;
        }

        while (lca != earliestBlock) {
            lca = lca->getParent(); // go up dom tree
            lcaNode = lca->getNode();
            assert(lca && lcaNode);
            if (lcaNode->isDispatchNode()) {
                continue; // code can't be placed in dispatch nodes
            }
            if (Log::isEnabled() ) {
                Log::out() << " LCA is "; printLabel(Log::out(), lca);                          Log::out() << " with depth " << (int)loopTree->getLoopDepth(lcaNode);
                Log::out() << ", best is ";     printLabel(Log::out(), best);
                Log::out() << " with depth " << (int)loopTree->getLoopDepth(bestNode);
                Log::out() << std::endl;
            }

            if (loopTree->getLoopDepth(lcaNode) < loopTree->getLoopDepth(bestNode)) {
                // find deepest block at shallowest nest
                best = lca;
                bestNode = lcaNode;
                if (Log::isEnabled() ) {
                    Log::out() << " LCA replaces best" << std::endl;
                }
            }
        };

        if (Log::isEnabled() ) {
            Log::out() << " Finally, best is "; printLabel(Log::out(), best);
            Log::out() << std::endl;
        }
        latest[i] = best;
    }

    if (Log::isEnabled() ) {
        Log::out() << " Placing inst "; i->print(Log::out());
        Log::out() << " in block ";     printLabel(Log::out(), latest[i]);            
        Log::out() << std::endl;
    }

    // now, to actually move the instruction:

    // we always move instructions at the start of the block.
    // this looks confusing if we are inserting into the same block
    // we are iterating over: we may see the instruction again, but
    // this is safe since the instruction is already marked as placed.

    // we do this even for pinned instructions; they will be visited in
    // reverse order, so they will be replaced in the block starting at
    // the top, so we are ok.

    Node *node = latest[i]->getNode();
    Inst* headInst = (Inst*)node->getFirstInst();
    assert(headInst->isLabel());

    i->unlink();
    if (i->getOperation().mustEndBlock()) {
        // but an instruction which must be at the end must stay there.
        // this includes branches.
        if (Log::isEnabled()) {
            Log::out() << "> Placing inst before headinst ";
            headInst->print(Log::out());
            Log::out() << std::endl;
        }
        node->appendInst(i);
    } else {
        if (Log::isEnabled()) {
            Log::out() << "> Placing inst after headinst ";
            headInst->print(Log::out());
            Log::out() << std::endl;
        }
        node->prependInst(i);
    }
}

DominatorNode *GlobalCodeMotion::leastCommonAncestor(DominatorNode *a, DominatorNode *b)
{
    if (a == 0) return b;
    assert(b != 0);
    while (a != b) {
        if (dominators.isAncestor(a, b)) {
            b = a;
        } else if (dominators.isAncestor(b, a)) {
            a = b;
        } else {
            a = a->getParent();
            b = b->getParent();
        }
    }
    return a;
}

// a NodeInstWalker, forwards/preorder
class GcmSinkConstantsWalker {
    GlobalCodeMotion *thePass;
    Node *block;
public:
    void startNode(Node *cfgNode0) { 
        block = cfgNode0;
        if (Log::isEnabled()) {
            Log::out() << "Begin sinking constants in block";
            FlowGraph::print(Log::out(), block);
            Log::out() << std::endl;
        }    
    };
    void applyToInst(Inst *i) { thePass->sinkConstants(i); };
    void finishNode(Node *cfgNode0) { 
        if (Log::isEnabled() ) {
            Log::out() << "Done sinking constants in block";
            FlowGraph::print(Log::out(), block);
            Log::out() << std::endl;
        }    
    };
    
    GcmSinkConstantsWalker(GlobalCodeMotion *thePass0)
        : thePass(thePass0), block(0)
    {
    };
};


void GlobalCodeMotion::sinkAllConstants()
{
    GcmSinkConstantsWalker gcmSinkConstantsWalker(this);

    // adapt the NodeInstWalker to a NodeWalker

    typedef NodeInst2NodeWalker<true, GcmSinkConstantsWalker> 
        SinkConstantsNodeWalker;
    SinkConstantsNodeWalker sinkConstantsNodeWalker(gcmSinkConstantsWalker);
    
    // do the walk, postorder
    NodeWalk<SinkConstantsNodeWalker>(fg, sinkConstantsNodeWalker);
}

void GlobalCodeMotion::sinkConstants(Inst *inst)
{
    for (U_32 i = 0; i < inst->getNumSrcOperands(); i++) {
        Opnd *opnd = inst->getSrc(i);
        Inst *opndInst = opnd->getInst();
        if (opndInst->isConst()) {
            ConstInst* cinst = opndInst->asConstInst();
            ConstInst::ConstValue cv = cinst->getValue();
            InstFactory &ifactory = irManager.getInstFactory();
            OpndManager &opndManager = irManager.getOpndManager();
            Opnd *newOp = opndManager.createSsaTmpOpnd(opnd->getType());
            Inst *newInst = ifactory.makeLdConst(newOp, cv);
            newInst->insertBefore(inst);
        }
    }
}

// MISC STUFF

bool GlobalCodeMotion::isPinned(Inst *i)
{
    Opcode opcode = i->getOpcode();
    // be explicit about SSA ops
//  if ((opcode == Op_LdVar) || (opcode == Op_StVar) || (opcode == Op_Phi))
    if ((opcode == Op_StVar) || (opcode == Op_Phi))
        return true;
    if (i->getOperation().isMovable()) {
        return false;
    }
    const OptimizerFlags& optimizerFlags = irManager.getOptimizerFlags(); 
    if (0 && optimizerFlags.cse_final) {
        switch (i->getOpcode()) {
        case Op_TauLdInd: 
            {
                Inst *srcInst = i->getSrc(0)->getInst();
                if ((srcInst->getOpcode() == Op_LdFieldAddr) ||
                    (srcInst->getOpcode() == Op_LdStaticAddr)) {
                    FieldAccessInst *faInst = srcInst->asFieldAccessInst();
                    FieldDesc *fd = faInst->getFieldDesc();
                    if (fd->isInitOnly()) {
                        return false;
                    }
                }
            } 
            break;
        case Op_LdStatic:
            {
                FieldAccessInst *inst = i->asFieldAccessInst();
                assert(inst);
                FieldDesc* fd = inst->getFieldDesc();
                if (fd->isInitOnly())
                    return false;
            }
            break;
        case Op_TauLdField:
            {
                FieldAccessInst *inst = i->asFieldAccessInst();
                assert(inst);
                FieldDesc* fd = inst->getFieldDesc();
                if (fd->isInitOnly())
                    return false;
            }
            break;
        default:
            break;
        };
    }
    return true;
}

void GlobalCodeMotion::markAsVisited(Inst *i)
{
    visited.insert(i);
}

bool GlobalCodeMotion::alreadyVisited(Inst *i)
{
    VisitedSet::const_iterator it = visited.find(i);
    return (it != visited.end());
}

void GlobalCodeMotion::clearVisited()
{
    visited.clear();
}

} //namespace Jitrino 
