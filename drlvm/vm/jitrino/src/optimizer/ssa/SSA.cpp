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

/*
 * the following file contains utilities to convert/deconvert to/from SSA form
 * for comments on the de-converter, see the second part of this file
 */



#include "Dominator.h"
#include "SSA.h"
#include "Inst.h"
#include "opndmap.h"
#include "walkers.h"
#include "Log.h"
#include "unionfind.h"
#include "BitSet.h"
#include "optimizer.h"
#include "CompilationContext.h"

namespace Jitrino {

DEFINE_SESSION_ACTION(FixupVarsPass, fixupvars, "Fixup SSA Vars")

void
FixupVarsPass::_run(IRManager& irm) {
    OptPass::computeDominators(irm);
    DominatorTree* dominatorTree = irm.getDominatorTree();
    ControlFlowGraph& flowGraph = irm.getFlowGraph();
    
    DomFrontier frontier(irm.getNestedMemoryManager(),*dominatorTree,&flowGraph);
    SSABuilder ssaBuilder(irm.getOpndManager(),irm.getInstFactory(),frontier,&flowGraph, irm.getOptimizerFlags());
    bool phiInserted = ssaBuilder.fixupVars(&irm.getFlowGraph(), irm.getMethodDesc());
    irm.setInSsa(true);
    if (phiInserted)
        irm.setSsaUpdated();
}

DEFINE_SESSION_ACTION(SSAPass, ssa, "SSA Construction")

void
SSAPass::_run(IRManager& irm) {
    OptPass::computeDominators(irm);
    DominatorTree* dominatorTree = irm.getDominatorTree();
    ControlFlowGraph& flowGraph = irm.getFlowGraph();
   
    DomFrontier frontier(irm.getNestedMemoryManager(),*dominatorTree,&flowGraph);
    SSABuilder ssaBuilder(irm.getOpndManager(),irm.getInstFactory(),frontier,&flowGraph, irm.getOptimizerFlags());
    ssaBuilder.convertSSA(irm.getMethodDesc());
    irm.setInSsa(true);
    irm.setSsaUpdated();
}

DEFINE_SESSION_ACTION(DeSSAPass, dessa, "SSA Deconstruction")

void
DeSSAPass::_run(IRManager& irm) {
    SSABuilder::deconvertSSA(&irm.getFlowGraph(),irm.getOpndManager());
    irm.setInSsa(false);
}

DEFINE_SESSION_ACTION(SplitSSAPass, splitssa, "SSA Variable Web Splitting") 

void
SplitSSAPass::_run(IRManager& irm) {
    SSABuilder::splitSsaWebs(&irm.getFlowGraph(), irm.getOpndManager());
}


//#define DEBUG_SSA

class RenameStack : public SparseScopedMap<Opnd *, SsaVarOpnd *> {
public:
    typedef SparseScopedMap<Opnd *, SsaVarOpnd *> BaseMap;
    RenameStack(MemoryManager& mm, U_32 n, const OptimizerFlags& optimizerFlags)
        : BaseMap(n, mm,
                  optimizerFlags.hash_init_factor,
                  optimizerFlags.hash_resize_factor,
                  optimizerFlags.hash_resize_to)
    {}
    ~RenameStack()
    {};
};


// Checks that phi insts can start from the second or third position only
// and goes in a row
// The first instruction is usually a LabelInst.
// The second one is Phi (if any). But in catch blocks the second inst
// is a CatchInst, and phi may be shifted to the third position.

bool SSABuilder::phiInstsOnRightPositionsInBB(Node* node) {
    Inst* inst = (Inst*)node->getSecondInst();
    if(inst && !inst->isPhi()) {
        // try the next one (third)
        inst = inst->getNextInst();
    }
    // skip all phis
    while ( inst!=NULL && inst->isPhi() ) {
        inst = inst->getNextInst();
    }
    // 'true' only if there is no any other phis in the node
    while ( inst!=NULL ) {
        if(inst->isPhi()) {
            return false;
        }
        inst = inst->getNextInst();
    }
    return true;
} 

//
// find def sites (blocks) of var operand
//
void SSABuilder::findDefSites(DefSites& allDefSites) {
    const Nodes& nodes = fg->getNodes();
    Nodes::const_iterator niter;
    for(niter = nodes.begin(); niter != nodes.end(); ++niter) {
        Node* node = *niter;
        if (!node->isBlockNode()) continue;

        // go over each instruction to find var definition
        Inst* first = (Inst*)node->getFirstInst();
        for (Inst* inst = first->getNextInst(); inst != NULL; inst = inst->getNextInst()) {
            // look for var definitions
            if (!inst->isStVar()) 
                continue;

            assert(inst->isVarAccess());
            // if inst->getVar() return NULL, then inst is accessing SSAOpnd.
            // Hence, there is no need to do SSA transformation (addVarDefSite() 
            // immediately returns.
            allDefSites.addVarDefSite(((VarAccessInst*)inst)->getVar(),node);
        }
    }
}

void SSABuilder::addPhiSrc(PhiInst* i, 
                           SsaVarOpnd* src) {
    assert(i->isPhi());
    U_32 numOpnds = i->getNumSrcOperands();
    for (U_32 j = 0; j < numOpnds; j++) {
        // no duplicate SSA opnd
        if (i->getSrc(j) == src) 
            return;
    }
    instFactory.appendSrc(i, src);
}

// a DomWalker, to be used pre-order
class SsaRenameWalker {
    MemoryManager &localMemManager;
    SSABuilder *ssaBuilder;
    RenameStack *rs;
    U_32 n;
    const StlVectorSet<VarOpnd *> *whatVars;
    const OptimizerFlags& optimizerFlags;
public:
    SsaRenameWalker(SSABuilder *builder0,
                    MemoryManager &localMM,
                    U_32 num, const OptimizerFlags& _optimizerFlags)
        : localMemManager(localMM),
          ssaBuilder(builder0),
          rs(0),
          n(num),
          whatVars(0),
          optimizerFlags(_optimizerFlags)
    {};

    void setChangedVarsSet(const StlVectorSet<VarOpnd *> *whatVars0) {
        whatVars = whatVars0;
    }

    void applyToDominatorNode(DominatorNode *domNode) {
        ssaBuilder->renameNode(rs, domNode, whatVars);
    }
    void enterScope() {
        if (!rs)
            rs = new (localMemManager) 
                RenameStack(localMemManager, n, optimizerFlags);
        rs->enter_scope();
    }
    void exitScope() {
        rs->exit_scope();
    };
};

//
// traverse dominator tree and rename variables
//
void SSABuilder::renameNode(RenameStack *renameStack, DominatorNode* dt,
                            const StlVectorSet<VarOpnd *> *whatVars)
{
    if (dt == NULL) return;
    Node* node = dt->getNode();

    Inst* head = (Inst*)node->getFirstInst();
#ifdef DEBUG_SSA
    std::ostream &cout = Log::out();
    if (Log::isEnabled()) {
        cout << "renameNode "; FlowGraph::printLabel(cout, node); cout << std::endl;
    }
#endif
    for (Inst* i = head->getNextInst(); i != NULL; i = i->getNextInst()) {
        if (!i->isPhi()) {
            // replace src with ssa opnd
            U_32 nSrcs = i->getNumSrcOperands();
            for (U_32 j = 0; j < nSrcs; j++) {
                Opnd *srcj = i->getSrc(j);
                VarOpnd *srcjVar = (srcj->isSsaVarOpnd()
                                    ? srcj->asSsaVarOpnd()->getVar()
                                    : srcj->asVarOpnd());
                if (!(srcjVar && !srcjVar->isAddrTaken()))
                    continue;
                if (whatVars && !whatVars->has(srcjVar)) continue;
                SsaVarOpnd* ssa = renameStack->lookup(srcjVar);
                assert(ssa);
                i->setSrc(j,ssa);
            }
        }

        // for both Phi and non-Phi
        // we replace any non-ssa dst with a new ssa opnd
        // and record it in the RenameStack map
        Opnd* dst = i->getDst();
        VarOpnd *theVar = dst->asVarOpnd();

        if (theVar && (!theVar->isAddrTaken())
            && !(whatVars && !whatVars->has(theVar))) {

            SsaVarOpnd* ssaDst = opndManager.createSsaVarOpnd((VarOpnd*)dst);
#ifdef DEBUG_SSA
            if (Log::isEnabled()) {
                cout << "SSA "; ssaDst->print(cout); cout << ::std::endl;
            }
#endif
            renameStack->insert((VarOpnd*)dst, ssaDst);
            i->setDst(ssaDst);
#ifdef DEBUG_SSA
            if (Log::isEnabled()) {
                i->print(cout); cout << ::std::endl;
            }
#endif
            // record stVar inst
        } else if (dst->isSsaVarOpnd()) {
            SsaVarOpnd* ssaDst = dst->asSsaVarOpnd();
            theVar = ssaDst->getVar();
            if (whatVars && !whatVars->has(theVar))
                continue;
#ifdef DEBUG_SSA
            if (Log::isEnabled()) {
                cout << "SSA "; ssaDst->print(cout); cout << ::std::endl;
            }
#endif
            renameStack->insert(ssaDst->getVar(), ssaDst);
#ifdef DEBUG_SSA
            if (Log::isEnabled()) {
                i->print(cout); cout << ::std::endl;
            }
#endif
        }
    }

    // add var sources to following phi instructions
    const Edges& edges = node->getOutEdges();
    Edges::const_iterator 
        eiter = edges.begin(),
        eend = edges.end();
    for(eiter = edges.begin(); eiter != eend; ++eiter) {
        Edge* e = *eiter;
        Node* succ = e->getTargetNode();
        // Phi insts are inserted to the beginning of the block
        // if succ does not have phi insts, then we can skip  it
        Inst *phi = (Inst*)succ->getSecondInst();
        if (phi==NULL || !phi->isPhi()) continue;

        // node is jth predecessor for succ
        
        
        // replace jth var of phi insts
        Inst* nextphi = phi->getNextInst();
        for (;phi!=NULL && phi->isPhi(); phi = nextphi) {
            nextphi = phi->getNextInst();
            // get var 
            Opnd *theopnd = phi->getDst();
            VarOpnd *thevar = theopnd->asVarOpnd();

            if (!thevar) {
                SsaVarOpnd *theSsaVar = theopnd->asSsaVarOpnd();
                assert(theSsaVar);

#ifdef DEBUG_SSA
                if (Log::isEnabled()) {
                    Log::out() << "case 2" << ::std::endl;
                }
#endif
                thevar = theSsaVar->getVar();
            }

            if (whatVars && !whatVars->has(thevar)) continue;

            SsaVarOpnd* ssa = renameStack->lookup((VarOpnd*)thevar);

            if (ssa != NULL) {
#ifdef DEBUG_SSA
                if (Log::isEnabled()) {
                    cout << "redge";
//                    cout << (I_32)j;
                    cout << " with ssa "; ssa->print(cout); cout << ::std::endl;
                }
#endif
                addPhiSrc((PhiInst*)phi,ssa);
            } else {
#ifdef DEBUG_SSA
                if (Log::isEnabled()) {
                    cout << "no source for phi of var ";
                    thevar->print(cout);
                    cout << ::std::endl;
                }
#endif
                // if ssa is NULL, then the phi must be a dead phi inst
                // (no more use of var afterwards). it will be removed.
            }
        }
    }
}

void SSABuilder::insertPhi(DefSites& allDefSites) {
    HashTableIter<void,VarDefSites> tableIter(allDefSites.getVarDefSites());
    void* var;
    VarDefSites* defSites;
    while (tableIter.getNextElem(var,defSites)) {
        VarOpnd* varOpnd = (VarOpnd*)var;
        //
        // skip var opnd whose address has been taken
        //
        if (varOpnd->isAddrTaken())
            continue;

        Node* node;
        Node *returnNode = fg->getReturnNode();
        Node *unwindNode = fg->getUnwindNode();
        Node *exitNode = fg->getExitNode();

        while ((node = defSites->removeDefSite()) != NULL) {
            List<Node>* df = frontier.getFrontiersOf(node);
            for (; df != NULL; df = df->getNext()) {
                // block where phi inst is going to be inserted
                Node* insertedLoc = df->getElem();
                // if phi has been inserted, then skip
                // no need to insert phi inst in the epilog,
                // return node, or unwind node because
                // there is no var use in these nodes
                
                if (!defSites->beenInsertedPhi(insertedLoc) &&
                    !((insertedLoc == unwindNode) ||
                      (insertedLoc == returnNode) ||
                      (insertedLoc == exitNode))) {
                    // create a new phi instruction for varOpnd
                    createPhiInst(varOpnd,insertedLoc);
                    // record phi(var) inst is created in df->getElem() block 
                    defSites->insertPhiSite(insertedLoc);
                }
            }
        }
    }
}

void SSABuilder::createPhiInst(VarOpnd* var, Node* insertedLoc) {
    // create phi instruction
    Inst* phi = instFactory.makePhi(var,0, 0);
    // insert to the beginning of insertedLoc
    insertedLoc->prependInst(phi);
    createPhi = true;
}

// this is the main function which returns true if a PHI node has been inserted
bool SSABuilder::convertSSA(MethodDesc&    methodDesc) {
    // create structure recording the blocks that define var opnds
    MemoryManager ssaMemManager("SSABuilder::convertSSA.ssaMemManager");
    DefSites allDefSites(ssaMemManager,frontier.getNumNodes());

    // find out where vars are defined
    findDefSites(allDefSites);

    // insert phi nodes
    insertPhi(allDefSites);

    // rename
    SsaRenameWalker renameWalker(this, ssaMemManager,
                                 optimizerFlags.hash_node_var_factor *
                                 (U_32) fg->getNodes().size(), optimizerFlags);

    DomTreeWalk<true, // pre-order
        SsaRenameWalker>(frontier.getDominator(), renameWalker, ssaMemManager);  

    return createPhi; 
}

// a NodeWalker
class ClearPhiSrcsWalker {
    SSABuilder *thePass;
    const StlVectorSet<VarOpnd *> *whatVars;
public:
    void applyToCFGNode(Node *node) { thePass->clearPhiSrcs(node,
                                                               whatVars); };
    
    ClearPhiSrcsWalker(SSABuilder *thePass0)
        : thePass(thePass0),
          whatVars(0)
    {
    };
    void setChangedVarsSet(const StlVectorSet<VarOpnd *> *changedVarsSet) {
        whatVars = changedVarsSet;
    }
};

// a NodeWalker
class ClearPhiSrcsWalker2 {
    SSABuilder *thePass;
    const StlVectorSet<VarOpnd *> *oldVars; // vars of interest
    StlVector<VarOpnd *> *newVars; // vars which we have changed
    const StlVectorSet<Opnd *> *removedVars; // vars defined by removed Phis
    StlVector<Node *> &tmpNodeList;
public:
    void applyToCFGNode(Node *node) { thePass->clearPhiSrcs2(node,
                                                                oldVars,
                                                                newVars,
                                                                removedVars,
                                                                tmpNodeList); };
    ClearPhiSrcsWalker2(SSABuilder *thePass0,
                        const StlVectorSet<VarOpnd *> *oldVars0,
                        StlVector<VarOpnd *> *newVars0,
                        const StlVectorSet<Opnd *> *removedVars0,
                        StlVector<Node *> &tmpNodeList0)
        : thePass(thePass0),
          oldVars(oldVars0), newVars(newVars0),
          removedVars(removedVars0),
          tmpNodeList(tmpNodeList0)
    {
    };
    void setOldVars(const StlVectorSet<VarOpnd *> *vars) {
        oldVars = vars;
    }
    void setNewVars(StlVector<VarOpnd *> *vars) {
        newVars = vars;
    }
    void setRemovedVars(const StlVectorSet<Opnd *> *vars) {
        removedVars = vars;
    }
};

void SSABuilder::clearPhiSrcs(Node *node, const StlVectorSet<VarOpnd *> *whatVars)
{
    Inst* phi = (Inst*)node->getSecondInst();
    if (whatVars) {
        for (;phi!=NULL && phi->isPhi(); phi = phi->getNextInst()) {
            Opnd *dstOp = phi->getDst();
            VarOpnd *varOpnd = dstOp->asVarOpnd();
            if (!varOpnd) {
                SsaVarOpnd *ssaOpnd = dstOp->asSsaVarOpnd();
                assert(ssaOpnd);
                varOpnd = ssaOpnd->getVar();
            }
            if (whatVars->has(varOpnd)) {
                PhiInst *phiInst = phi->asPhiInst();
                assert(phiInst);
                phiInst->setNumSrcs(0);
            }
        }
    } else {
        for (;phi!=NULL && phi->isPhi(); phi = phi->getNextInst()) {
            PhiInst *phiInst = phi->asPhiInst();
            assert(phiInst);
            phiInst->setNumSrcs(0);
        }
    }
}

void SSABuilder::clearPhiSrcs2(Node *node, 
                               const StlVectorSet<VarOpnd *> *whatVars,
                               StlVector<VarOpnd *> *changedVars,
                               const StlVectorSet<Opnd *> *removedVars,
                               StlVector<Node *> &scratchNodeList)
{
    bool needPreds = true;
    StlVector<Node *> &preds = scratchNodeList;

    Inst* inst = (Inst*)node->getSecondInst();
    for (;inst!=NULL && inst->isPhi(); inst = inst->getNextInst()) {
        Opnd *dstOp =inst->getDst();
        VarOpnd *varOpnd = 0;
        if (whatVars) {
            varOpnd = dstOp->asVarOpnd();
            if (!varOpnd) {
                SsaVarOpnd *ssaOpnd = dstOp->asSsaVarOpnd();
                assert(ssaOpnd);
                varOpnd = ssaOpnd->getVar();
            }
            if (!whatVars->has(varOpnd))
                continue;
        }
        bool changed=false;
        U_32 numSrcs = inst->getNumSrcOperands();
        for (U_32 i=0; i<numSrcs; ++i) {
            Opnd *thisOpnd = inst->getSrc(i);

            if (!(removedVars && removedVars->has(thisOpnd))) {
                // need to test whether need to remove
                if (needPreds) {
                    needPreds = false;
                    
                    const Edges& edges2 = node->getInEdges();
                    preds.clear();
                    preds.reserve(edges2.size());
                    Edges::const_iterator eiter2;
                    for(eiter2 = edges2.begin(); eiter2 != edges2.end(); ++eiter2){
                        preds.push_back((*eiter2)->getSourceNode());
                    }
                }
                DominatorTree &domTree = frontier.getDominator();
                Inst *thisOpndInst = thisOpnd->getInst();
                Node *thisOpndInstNode = thisOpndInst->getNode();
                if (thisOpndInstNode) {
                    // the operand's source instruction was not already dead.
                    StlVector<Node *>::const_iterator 
                        predIter = preds.begin(),
                        predEnd = preds.end();
                    bool foundDom = false;
                    for ( ; predIter != predEnd; ++predIter) {
                        Node *predNode = *predIter;
                        if (domTree.dominates(thisOpndInstNode, predNode)) {
                            // we found it, leave this operand alone.
                            foundDom = true;
                            break;
                        }
                    }
                    if (foundDom) continue; // leave operand alone.
                }
            }
            // remove this operand;
            if (i < numSrcs-1) {
                inst->setSrc(i, inst->getSrc(numSrcs-1));
                --i; // re-examine this operand, which is now the last
            }
            --numSrcs; // we deleted one operand
            PhiInst *phiInst = inst->asPhiInst();
            assert(phiInst);
            phiInst->setNumSrcs(numSrcs);
            changed = true;
        }
        if (changed) {
            // note the changed var;
            if (!varOpnd) {
                varOpnd = dstOp->asVarOpnd();
                if (!varOpnd) {
                    SsaVarOpnd *ssaOpnd = dstOp->asSsaVarOpnd();
                    assert(ssaOpnd);
                    varOpnd = ssaOpnd->getVar();
                }
            }
            changedVars->push_back(varOpnd);
        }
    }
}

// a NodeWalker
class CheckForTrivialPhisWalker {
    SSABuilder *thePass;
    MemoryManager &localMemManager;
    bool removedPhi;
    bool removedPhiRecently;
    StlVector<VarOpnd *> changedVars;
    StlVectorSet<VarOpnd *> changedVarsSet;
public:
    void applyToCFGNode(Node *node) { 
        if (thePass->checkForTrivialPhis(node, changedVars)) {
            removedPhi = true; 
            removedPhiRecently = true;
        }
    };
    
    CheckForTrivialPhisWalker(SSABuilder *thePass0,
                              MemoryManager &localMM)
        : thePass(thePass0), localMemManager(localMM),
          removedPhi(false), removedPhiRecently(false), changedVars(localMM),
          changedVarsSet(localMM)
    {
    };
    bool foundTrivial() { return removedPhi; }
    void resetFound() { removedPhi = false; changedVars.clear(); changedVarsSet.clear(); }
    const StlVectorSet<VarOpnd *> *getChangedVarsSet() { 
        if (changedVars.empty()) {
            return 0;
        } else {
            if (removedPhiRecently) {
                changedVarsSet.clear();
                changedVarsSet.insert(changedVars.begin(), changedVars.end());
                removedPhiRecently = false;
            }
            return &changedVarsSet;
        }
    };
};

bool SSABuilder::checkForTrivialPhis(Node *node, 
                                     StlVector<VarOpnd *> &changedVars)
{
    // Check that phi insts can start from the second or third position only
    // and goes in a row
    assert(phiInstsOnRightPositionsInBB(node));

    Inst* phi = (Inst*)node->getSecondInst();
    if(phi && !phi->isPhi()) {
        // try the next one (third)
        phi = phi->getNextInst();
    }

    bool removedPhi = false;
#ifdef DEBUG_SSA
    if (Log::isEnabled()) {
        Log::out() << "Checking node " << (int)node->getId() 
                   << " for trivial phis" << ::std::endl;
    }
#endif
    Inst* nextphi = NULL;            
    for (;phi!=NULL && phi->isPhi(); phi = nextphi) {
        nextphi = phi->getNextInst();
#ifdef _DEBUG
        PhiInst *phiInst = phi->asPhiInst();
        assert(phiInst);
#endif
        U_32 nSrcs = phi->getNumSrcOperands();
        if (nSrcs <= 1) {
            // phi must be trivial
#ifdef DEBUG_SSA
            ::std::ostream &cout = Log::out();
            if (Log::isEnabled()) {
                cout << "removing trivial instruction "; phi->print(cout); cout << ::std::endl;
            }
#endif
            Opnd *dstOp = phi->getDst();
            VarOpnd *varOp = dstOp->asVarOpnd();
            if (!varOp) {
                SsaVarOpnd *ssaOp = dstOp->asSsaVarOpnd();
                assert(ssaOp);
                varOp = ssaOp->getVar();
            }
            changedVars.push_back(varOp);
            removedPhi = true;
            phi->unlink();
        }
    }
    return removedPhi;
}

// a NodeWalker
class CheckForTrivialPhisWalker2 {
    SSABuilder *thePass;
    const StlVectorSet<VarOpnd *> *lookatVars; // vars to look at 
    StlVector<VarOpnd *> *changedVars; // note which vars are changed
    StlVector<Opnd *> *removedVars; // record dst of removed Phis
public:
    void applyToCFGNode(Node *node) { 
        thePass->checkForTrivialPhis2(node, lookatVars, changedVars,
                                      removedVars);
    };
    CheckForTrivialPhisWalker2(SSABuilder *thePass0,
                               StlVectorSet<VarOpnd *> *lookatVars0,
                               StlVector<VarOpnd *> *changedVars0,
                               StlVector<Opnd *> *removedVars0)
        : thePass(thePass0),
          lookatVars(lookatVars0), changedVars(changedVars0),
          removedVars(removedVars0)
    {
    };
    void setLookatVars(const StlVectorSet<VarOpnd *> *vars) { lookatVars = vars; }
    void setChangedVars(StlVector<VarOpnd *> *vars) { changedVars = vars; }
};

void SSABuilder::checkForTrivialPhis2(Node *node, 
                                      const StlVectorSet<VarOpnd *> *lookatVars,
                                      StlVector<VarOpnd *> *changedVars,
                                      StlVector<Opnd *> *removedVars)
{
    // Check that phi insts can start from the second or third position only
    // and goes in a row
    assert(phiInstsOnRightPositionsInBB(node));

    Inst* phi = (Inst*)node->getSecondInst();
    if(phi && !phi->isPhi()) {
        // try the next one (third)
        phi = phi->getNextInst();
    }

    Inst *nextphi = NULL;
#ifdef DEBUG_SSA
    if (Log::isEnabled()) {
        Log::out() << "Checking node " << (int)node->getId() 
                   << " for trivial phis2" << ::std::endl;
    }
#endif
    for (;phi->isPhi(); phi = nextphi) {
        nextphi = phi->getNextInst();
#ifdef _DEBUG
        PhiInst *phiInst = phi->asPhiInst();
        assert(phiInst);
#endif
        U_32 nSrcs = phi->getNumSrcOperands();
        if (nSrcs <= 1) {
            // phi must be trivial
#ifdef DEBUG_SSA
            ::std::ostream &cout = Log::out();
            if (Log::isEnabled()) {
                cout << "removing trivial2 instruction "; phi->print(cout); cout << ::std::endl;
            }
#endif
            Opnd *dstOp = phi->getDst();
            VarOpnd *varOp = dstOp->asVarOpnd();
            if (!varOp) {
                SsaVarOpnd *ssaOp = dstOp->asSsaVarOpnd();
                assert(ssaOp);
                varOp = ssaOp->getVar();
            }
            assert(!lookatVars || (lookatVars->has(varOp)));
            changedVars->push_back(varOp);
            removedVars->push_back(dstOp);
            phi->unlink();
        }
    }
}

bool SSABuilder::fixupSSA(MethodDesc& methodDesc, bool useBetter) {
    // clear out all Phi args
    MemoryManager localMM("SSABuilder::fixupSSA::memManager");

#ifdef DEBUG_SSA
    if (Log::isEnabled()) {
        Log::out() << "Starting fixupSSA" << ::std::endl;
        
        FlowGraph::printDotFile(*fg, methodDesc, "midfixup-0");
    }
#endif

    if (useBetter) {
        StlVector<VarOpnd *> newChangedVars(localMM);
        StlVector<Opnd *> removedVars(localMM);
        StlVector<Node *> tmpNodeList(localMM);

        // first cleanup trivial Phis to avoid mess later
        CheckForTrivialPhisWalker2 checkTrivialWalker(this,
                                                      0,
                                                      &newChangedVars,
                                                      &removedVars);
        NodeWalk<CheckForTrivialPhisWalker2>(*fg, checkTrivialWalker);

        StlVectorSet<Opnd *> removedVarsSet(localMM);
        removedVarsSet.insert(removedVars.begin(), removedVars.end());

        // checks for vars which don't dominate an in-edge of the phi
        ClearPhiSrcsWalker2 clearPhisWalker2(this, 
                                             0,
                                             &newChangedVars,
                                             &removedVarsSet,
                                             tmpNodeList);
        NodeWalk<ClearPhiSrcsWalker2>(*fg, clearPhisWalker2);
        if (newChangedVars.empty()) {
#ifdef DEBUG_SSA
            if (Log::isEnabled()) {
                Log::out() << "newChangedVars is empty" << ::std::endl;
            }
#endif
            return false; // nothing changed, should still be non-trivial
        }

        // do renaming to propagate vars to Phis which had vars removed
        SsaRenameWalker renameWalker(this, localMM,
                                     optimizerFlags.hash_node_var_factor *
                                     (U_32) fg->getNodes().size(), optimizerFlags);

        // oldChangedVarsSet will be the set for looking up Vars to consider
        StlVectorSet<VarOpnd *> oldChangedVarsSet(localMM);
        oldChangedVarsSet.insert(newChangedVars.begin(), newChangedVars.end());
        renameWalker.setChangedVarsSet(&oldChangedVarsSet);

        DomTreeWalk<true, // preorder
            SsaRenameWalker>(frontier.getDominator(), renameWalker, localMM);

        // check for Phis which are now trivial, but only need to look at oldChangedVarsSet
        newChangedVars.clear(); // note that the checkTrivialWalker updates newChangedVars
        checkTrivialWalker.setLookatVars(&oldChangedVarsSet);
        NodeWalk<CheckForTrivialPhisWalker2>(*fg, checkTrivialWalker);

        while (!newChangedVars.empty()) {
            // lather, rinse, repeat

#ifdef DEBUG_SSA
            if (Log::isEnabled()) {
                Log::out() << "beginning iteration" << ::std::endl;
            }
#endif
            oldChangedVarsSet.clear();
            oldChangedVarsSet.insert(newChangedVars.begin(), newChangedVars.end());
            newChangedVars.clear();

            // clear phis with unreaching parameters
            clearPhisWalker2.setOldVars(&oldChangedVarsSet);

            NodeWalk<ClearPhiSrcsWalker2>(*fg, clearPhisWalker2);
            if (newChangedVars.empty()) {
                return true;  // nothing changed, still non-trivial
            }

            // rename just newVars
            DomTreeWalk<true, // pre-order
                SsaRenameWalker>(frontier.getDominator(), renameWalker, 
                                 localMM);

            
            oldChangedVarsSet.clear();
            oldChangedVarsSet.insert(newChangedVars.begin(), newChangedVars.end());
            newChangedVars.clear();

            // check oldChangedVars for trivial Phis
            NodeWalk<CheckForTrivialPhisWalker2>(*fg, checkTrivialWalker);
        }
#ifdef DEBUG_SSA
        if (Log::isEnabled()) {
            Log::out() << "done iteration" << ::std::endl;
        }
#endif
    } else {        
        // old algorithm
        ClearPhiSrcsWalker clearPhisWalker(this);
        NodeWalk<ClearPhiSrcsWalker>(*fg, clearPhisWalker);
        
#ifdef DEBUG_SSA
        if (Log::isEnabled()) {
            FlowGraph::printDotFile(*fg, methodDesc, "midfixup-1");
        }
#endif
        
        SsaRenameWalker renameWalker(this, localMM,
                                     optimizerFlags.hash_node_var_factor *
                                     (I_32) fg->getNodes().size(), optimizerFlags);
        DomTreeWalk<true, // pre-order
            SsaRenameWalker>(frontier.getDominator(), renameWalker,
                             localMM);
        
#ifdef DEBUG_SSA
        if (Log::isEnabled()) {
            FlowGraph::printDotFile(*fg, methodDesc, "midfixup-2");
        }
#endif

        CheckForTrivialPhisWalker checkTrivialWalker(this, localMM);
        NodeWalk<CheckForTrivialPhisWalker>(*fg, checkTrivialWalker);

#ifdef DEBUG_SSA
        if (Log::isEnabled()) {
            Log::out() << "before iteration" << ::std::endl;
        }
#endif
        while (checkTrivialWalker.foundTrivial()) {
#ifdef DEBUG_SSA
            if (Log::isEnabled()) {
                Log::out() << "starting iteration" << ::std::endl;
            }
#endif

            // we deleted some Phi instruction(s)
#ifdef DEBUG_SSA
            if (Log::isEnabled()) {
                FlowGraph::printDotFile(*fg, methodDesc, "midfixup-3");
            }
#endif
            // clear all other Phis for that var(s)
            clearPhisWalker.setChangedVarsSet(checkTrivialWalker.getChangedVarsSet());
            NodeWalk<ClearPhiSrcsWalker>(*fg, clearPhisWalker);
            
            // do renaming for that var(s)
            renameWalker.setChangedVarsSet(checkTrivialWalker.getChangedVarsSet());
            DomTreeWalk<true, // pre-order
                SsaRenameWalker>(frontier.getDominator(), renameWalker,
                                 localMM);
            
            // check again
            checkTrivialWalker.resetFound();
            NodeWalk<CheckForTrivialPhisWalker>(*fg, checkTrivialWalker);
        }
#ifdef DEBUG_SSA
        if (Log::isEnabled()) {
            Log::out() << "done iteration" << ::std::endl;
        }
#endif
    }        
#ifdef DEBUG_SSA
    if (Log::isEnabled()) {
        FlowGraph::printDotFile(*fg, methodDesc, "midfixup-4");
        
        Log::out() << "Finished fixupSSA" << ::std::endl;
    }
#endif

    return true;
}

// an InstWalker, arbitrary order
class ScanSsaVarsWalker {
    SSABuilder *thePass;
    StlVectorSet<VarOpnd *> &usedOutOfSsa;

public:
    ScanSsaVarsWalker(SSABuilder *from,
                      StlVectorSet<VarOpnd *> &usedOutOfSsa0)
        : thePass(from),
          usedOutOfSsa(usedOutOfSsa0)
    {                         
    };
    
    void recordOpnd(Opnd *thisOpnd) {
        assert(thisOpnd);
        if (thisOpnd->isSsaVarOpnd()) {
            SsaVarOpnd *ssaOpnd = thisOpnd->asSsaVarOpnd();
            ssaOpnd->getVar();
        } else if (thisOpnd->isVarOpnd()) {
            VarOpnd *var = thisOpnd->asVarOpnd();
            usedOutOfSsa.insert(var);
        }
    };

    void applyToInst(Inst *inst) {
        U_32 numSrcs = inst->getNumSrcOperands();
        for (U_32 i=0; i<numSrcs; ++i) {
            Opnd *thisOpnd = inst->getSrc(i);
            recordOpnd(thisOpnd);
        }
        Opnd *dstOpnd = inst->getDst();
        recordOpnd(dstOpnd);
    }
};

// an InstWalker, arbitrary order
class DeSsaVarsWalker {
    SSABuilder *thePass;
    StlVectorSet<VarOpnd *> &usedOutOfSsa;

public:
    DeSsaVarsWalker(SSABuilder *from,
                    StlVectorSet<VarOpnd *> &usedOutOfSsa0)
        : thePass(from),
          usedOutOfSsa(usedOutOfSsa0)
    {
    };
    
    VarOpnd *needToDeSsaOpnd(Opnd *thisOpnd) {
        assert(thisOpnd);
        if (thisOpnd->isSsaVarOpnd()) {
            SsaVarOpnd *ssaOpnd = thisOpnd->asSsaVarOpnd();
            VarOpnd *var = ssaOpnd->getVar();
            if (usedOutOfSsa.has(var)) {
                return var;
            }
        }
        return 0;
    };

    void applyToInst(Inst *inst) {
        if (inst->getOpcode() == Op_Phi) {
            // we should just delete the whole thing.
            Opnd *dstOpnd = inst->getDst();
            
            if (dstOpnd->isSsaVarOpnd()) {
                SsaVarOpnd *ssaOpnd = dstOpnd->asSsaVarOpnd();
                VarOpnd *var = ssaOpnd->getVar();
                if (usedOutOfSsa.has(var)) {
                    // remove instruction
                    inst->unlink();
                    return;
                }
            } else if (dstOpnd->isVarOpnd()) {
                VarOpnd *var = dstOpnd->asVarOpnd();
                if (usedOutOfSsa.has(var)) {
                    // remove instruction
                    inst->unlink();
                    return;
                }
            }
        }
        U_32 numSrcs = inst->getNumSrcOperands();
        for (U_32 i=0; i<numSrcs; ++i) {
            Opnd *thisOpnd = inst->getSrc(i);
            VarOpnd *varOpnd = needToDeSsaOpnd(thisOpnd);
            if (varOpnd) {
                inst->setSrc(i, varOpnd);
            }
        }
        Opnd *dstOpnd = inst->getDst();
        VarOpnd *varDstOpnd = needToDeSsaOpnd(dstOpnd);
        if (varDstOpnd) {
            inst->setDst(varDstOpnd);
        }
    }
};


// check for new Vars which are not in SSA form; if there are any, fix up all occurrences of 
// that var, put into SSA form.
bool SSABuilder::fixupVars(ControlFlowGraph*fg, MethodDesc& methodDesc) {
    // clear out all Phi args
    MemoryManager localMM("SSABuilder::fixupVars::memManager");

#ifdef DEBUG_SSA
    if (Log::isEnabled()) {
        Log::out() << "Starting fixupVars" << ::std::endl;
        
        FlowGraph::printDotFile(*fg, methodDesc, "midfixupvars-0");
    }
#endif

    StlVectorSet<VarOpnd *> usedOutOfSsa(localMM);
    ScanSsaVarsWalker firstWalker(this, usedOutOfSsa);
    typedef Inst2NodeWalker<true, ScanSsaVarsWalker> ScanSsaVarsNodeWalker;
    ScanSsaVarsNodeWalker scanSsaVarsNodeWalker(firstWalker);
    NodeWalk<ScanSsaVarsNodeWalker>(*fg, scanSsaVarsNodeWalker);

    // now usedOutOfSsa has vars that need to be fixed up

    if (!usedOutOfSsa.empty()) {
        // cleanup in case there are any other uses still.
        DeSsaVarsWalker cleanupWalker(this, usedOutOfSsa);

        typedef Inst2NodeWalker<true, DeSsaVarsWalker> DeSsaVarsNodeWalker;
        DeSsaVarsNodeWalker cleanupNodeWalker(cleanupWalker);

        NodeWalk<DeSsaVarsNodeWalker>(*fg, cleanupNodeWalker);
        
        // now all vars in usedOutOfSsa are not used in SSA form, and have no Phis.
        
#ifdef DEBUG_SSA
        if (Log::isEnabled()) {
            FlowGraph::printDotFile(*fg,methodDesc, "midfixupvars-1");
        }
#endif

        // just run the conversion pass
        convertSSA(methodDesc);
    }

#ifdef DEBUG_SSA
    if (Log::isEnabled()) {
        FlowGraph::printDotFile(*fg, methodDesc, "midfixupvars-3");
        
        Log::out() << "Finished fixupVars" << ::std::endl;
    }
#endif

    return !usedOutOfSsa.empty();
}



/* END OF CONVERTER ********************************************************************************/


/* the second part of this file contains the de-converter.
 * CAUTION: the de-converter is currently very simple, assumes that the
 * live ranges of SSA variables never overlap, which could be caused
 * by optimizations such as copy propagation and code motion.  
 */


void SSABuilder::deconvertSSA(ControlFlowGraph* fg,OpndManager& opndManager) {
    const Nodes& nodes = fg->getNodes();
    Nodes::const_iterator niter;
    for(niter = nodes.begin(); niter != nodes.end(); ++niter) {
        Node* node = *niter;
        Inst *headInst = (Inst*)node->getFirstInst();
        for (Inst *inst = headInst->getNextInst(); inst != NULL; ) {
            Inst *nextInst = inst->getNextInst();
            if (inst->isPhi()) {
                inst->unlink();
            } else {
                for (U_32 i = 0; i < inst->getNumSrcOperands(); i++) {
                    Opnd *opnd = inst->getSrc(i);
                    if (opnd->isSsaVarOpnd()) {
                        SsaVarOpnd *ssa = (SsaVarOpnd *)opnd;
                        VarOpnd *var = ssa->getVar();
                        inst->setSrc(i,var);
                    } else if (opnd->isVarOpnd()) {
                    }
                }
                Opnd *dst = inst->getDst();
                if (dst->isSsaVarOpnd()) {
                    SsaVarOpnd *ssa = (SsaVarOpnd *)dst;
                    inst->setDst(ssa->getVar()); 
                } 
            }
            inst = nextInst;
        }
    }
}

struct SsaVarClique : private UnionFind {
    VarOpnd *var;
    SsaVarClique(VarOpnd *var0) : var(var0) {};
    SsaVarClique() : var(0) {};
    SsaVarClique *getRoot() { 
        UnionFind *root = find();
        return (SsaVarClique *) root;
    }
    void link(SsaVarClique *other) {
        UnionFind::link(other);
    }
};

// rename vars to make un-overlapping live ranges of a variable into
// different variables.
void SSABuilder::splitSsaWebs(ControlFlowGraph* fg,OpndManager& opndManager) {
    U_32 numSsaOpnds = opndManager.getNumSsaOpnds();
    MemoryManager localMM("SSABuilder::splitSsaWebs::memManager");
    SsaVarClique *cliques = new (localMM) SsaVarClique[numSsaOpnds];
    
    const Nodes& nodes = fg->getNodes();
    Nodes::const_iterator niter;
    for(niter = nodes.begin(); niter != nodes.end(); ++niter) {
        Node* node = *niter;
        Inst *headInst = (Inst*)node->getFirstInst();
        for (Inst *inst = headInst->getNextInst(); inst != NULL; ) {
            Inst *nextInst = inst->getNextInst();
            if (inst->isPhi()) {
                // do something
                VarOpnd *var0 = 0;
                SsaVarClique *clique = 0;
                for (U_32 i = 0; i < inst->getNumSrcOperands(); i++) {
                    Opnd *opnd = inst->getSrc(i);
                    if (opnd->isSsaVarOpnd()) {
                        SsaVarOpnd *ssa = (SsaVarOpnd *)opnd;
                        U_32 id = ssa->getId();
                        if (var0) {
                            assert(ssa->getVar()==var0);
                            cliques[id].link(clique);
                        } else {
                            var0 = ssa->getVar();
                            clique = &cliques[id];
                        }
                    }
                }
                Opnd *dst = inst->getDst();
                if (dst->isSsaVarOpnd()) {
                    SsaVarOpnd *ssa = (SsaVarOpnd *)dst;
                    ssa->getVar();
                    U_32 id = ssa->getId();
                    if (var0) {
                        assert(ssa->getVar()==var0);
                        cliques[id].link(clique);
                    } else {
                        var0 = ssa->getVar();
                        clique = &cliques[id];
                    }
                }
            }
            inst = nextInst;
        }
    }
    U_32 numvars = opndManager.getNumVarOpnds();
    bool *used = new (localMM) bool[numvars];
    for (U_32 i=0; i<numvars; i++) {
        used[i] = false;
    }

    for(niter = nodes.begin(); niter != nodes.end(); ++niter) {
        Node* node = *niter;
        Inst *headInst = (Inst*)node->getFirstInst();
        for (Inst *inst = headInst->getNextInst(); inst != NULL; ) {
            Inst *nextInst = inst->getNextInst();

            for (U_32 i = 0; i < inst->getNumSrcOperands(); i++) {
                Opnd *opnd = inst->getSrc(i);
                if (opnd->isSsaVarOpnd()) {
                    SsaVarOpnd *ssa = (SsaVarOpnd *)opnd;
                    VarOpnd *var = ssa->getVar();
                    U_32 id=ssa->getId();
                    SsaVarClique *clique = &cliques[id];
                    clique = clique->getRoot();
                    VarOpnd *cvar = clique->var;
                    if (cvar == 0) {
                        U_32 varId = var->getId();
                        if (used[varId]) {
                            cvar = opndManager.createVarOpnd(var->getType(),
                                                             var->isPinned());
                        } else {
                            cvar = var;
                            used[varId] = true;
                        }
                        clique->var = cvar;
                    }
                    if (cvar != var) {
                        ssa->setVar(cvar);
                    }
                }
            }
            Opnd *dst = inst->getDst();
            if (dst->isSsaVarOpnd()) {
                SsaVarOpnd *ssa = (SsaVarOpnd *)dst;
                VarOpnd *var = ssa->getVar();
                U_32 id=ssa->getId();

                SsaVarClique *clique = &cliques[id];
                clique = clique->getRoot();
                VarOpnd *cvar = clique->var;
                if (cvar == 0) {
                    U_32 varId = var->getId();
                    if (used[varId]) {
                        cvar = opndManager.createVarOpnd(var->getType(),
                                                         var->isPinned());
                    } else {
                        cvar = var;
                        used[varId] = true;
                    }
                    clique->var = cvar;
                }
                if (cvar != var) {
                    ssa->setVar(cvar);
                }
            } 
            
            inst = nextInst;
        }
    }
}

} //namespace Jitrino 
