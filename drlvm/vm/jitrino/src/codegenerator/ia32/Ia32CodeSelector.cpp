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
 * @author Intel, Vyacheslav P. Shakin
 */


#include <stdlib.h>
#include "Ia32CodeGenerator.h"
#include "Ia32CodeSelector.h"
#include "Ia32CFG.h"
#include "Ia32InstCodeSelector.h"
#include "EMInterface.h"
#include "XTimer.h"

namespace Jitrino
{
namespace Ia32{

CountTime     selectionTimer("ia32::selector::selection");
CountTime     blockMergingTimer("ia32::selector::blockMerging");
CountTime     fixNodeInfoTimer("ia32::selector::fixNodeInfo");


//_______________________________________________________________________________________________________________
// FP conversion internal helpers (temp solution to be optimized)

//========================================================================================================
//                     class CfgCodeSelector
//========================================================================================================
//_______________________________________________________________________________________________
/**  Construct CFG builder */

CfgCodeSelector::CfgCodeSelector(::Jitrino::SessionAction* sa, 
                                 CompilationInterface&      compIntfc,
                                 MethodCodeSelector& methodCodeSel,
                                 MemoryManager&          codeSelectorMM, 
                                 U_32                  nNodes, 
                                 IRManager&          irM
                                 )
    : numNodes(nNodes), nextNodeId(0), compilationInterface(compIntfc), methodCodeSelector(methodCodeSel),
      irMemManager(irM.getMemoryManager()), 
      codeSelectorMemManager(codeSelectorMM),  irManager(irM),
      hasDispatchNodes(false), currBlock(NULL), returnOperand(0) 
{
    nextNodeId = 0;
    nodes = new (codeSelectorMemManager) Node*[numNodes];
    U_32 i;
    for (i = 0; i < numNodes; i++) 
        nodes[i] = NULL;

    InstCodeSelector::onCFGInit(irManager);
    flags.useInternalHelpersForInteger2FloatConv = sa->getBoolArg("useInternalHelpersForInteger2FloatConv", false);
    flags.slowLdString = sa->getBoolArg("SlowLdString", false); 
}

//_______________________________________________________________________________________________
/**  Create an exception handling (dispatching) node */

U_32 CfgCodeSelector::genDispatchNode(U_32 numInEdges,U_32 numOutEdges, const StlVector<MethodDesc*>& inlineEndMarkers, double cnt) 
{
    assert(nextNodeId < numNodes);
    U_32 nodeId = nextNodeId++;
    Node* node = irManager.getFlowGraph()->createDispatchNode();
    node->setExecCount(cnt);
    nodes[nodeId] = node;
    hasDispatchNodes = true;
    for (StlVector<MethodDesc*>::const_iterator it = inlineEndMarkers.begin(), end = inlineEndMarkers.end(); it!=end; ++it) {
        MethodDesc*  desc = *it;
        node->appendInst(irManager.newMethodEndPseudoInst(desc));
    }
    return nodeId;
}

//_______________________________________________________________________________________________
/**  Create a basic block */

U_32 CfgCodeSelector::genBlock(U_32              numInEdges,
                                    U_32              numOutEdges,
                                    BlockKind           blockKind,
                                    BlockCodeSelector&  codeSelector,
                                    double              cnt) 
{
    assert(nextNodeId < numNodes);
    U_32 nodeId = nextNodeId++;
    Node* bb = irManager.getFlowGraph()->createBlockNode();
    bb->setExecCount(cnt);
    nodes[nodeId] = bb;
    InstCodeSelector instCodeSelector(compilationInterface, *this, irManager, bb);
    currBlock = bb;
    { 
        codeSelector.genCode(instCodeSelector);
    }

    currBlock = NULL;
    //  Set prolog or epilogue node
    switch (blockKind) {
    case Prolog:
        {
        //  Copy execution count into IA32 CFG prolog node and
        //  create an edge from IA32 CFG prolog node to optimizer's prolog node
        Node* prolog = irManager.getFlowGraph()->getEntryNode();
        prolog->setExecCount(cnt);
        irManager.getFlowGraph()->addEdge(prolog, bb, 1.0);
        break;
        }
    case Epilog:
        {
        assert(bb->isEmpty());
        break;
        }
    case InnerBlock:
        break;  // nothing to do
    }

    if (instCodeSelector.endsWithSwitch()) {
        // Generate an additional node that contains switch dispatch
        U_32      numTargets = instCodeSelector.getSwitchNumTargets(); 
        Opnd * switchSrc = instCodeSelector.getSwitchSrc();
        genSwitchBlock(bb, numTargets, switchSrc);
    }

    return nodeId;
}

//_______________________________________________________________________________________________
/**
    Create unwind node.
    This is a temporary node that exists only during code selection.
    We create it using code selector memory manager and insert it into its own CFG.
*/

U_32  CfgCodeSelector::genUnwindNode(U_32 numInEdges, 
                                          U_32 numOutEdges,
                                          double cnt) 
{
    assert(nextNodeId < numNodes);
    U_32 nodeId = nextNodeId++;
    ControlFlowGraph* fg = irManager.getFlowGraph();
    Node* unwindNode = fg->createDispatchNode();
    fg->setUnwindNode(unwindNode);
    unwindNode->setExecCount(cnt);
    nodes[nodeId] = unwindNode;
    return nodeId;
}

//_______________________________________________________________________________________________
/**  Create exit node */

U_32 CfgCodeSelector::genExitNode(U_32 numInEdges, double cnt) 
{
    assert(nextNodeId < numNodes);
    U_32 nodeId = nextNodeId++;
    ControlFlowGraph* fg = irManager.getFlowGraph();
    Node* exitNode = fg->createExitNode();
    exitNode->setExecCount(cnt);
    fg->setExitNode(exitNode);
    nodes[nodeId] = exitNode;
    return nodeId;
}

//_______________________________________________________________________________________________
/**  Create a block for a switch statement */

void CfgCodeSelector::genSwitchBlock(Node *originalBlock,
                                        U_32         numTargets, 
                                        Opnd *      switchSrc) 
{
    Node *bb = irManager.getFlowGraph()->createBlockNode();
    bb->setExecCount(originalBlock->getExecCount());
    InstCodeSelector instSelector(compilationInterface, *this, irManager, bb);
    { 
        instSelector.genSwitchDispatch(numTargets,switchSrc);
    }
    // Create an edge from the original block to bb
    genFalseEdge(originalBlock, bb, 1.0);
}

//_______________________________________________________________________________________________
/**  Create true edge (i.e., edge that corresponds to a taken conditional branch) */

void CfgCodeSelector::genTrueEdge(U_32 tailNodeId,U_32 headNodeId, double prob) 
{
    Node* tailNode= nodes[tailNodeId];
    Node * headNode = nodes[headNodeId];
    genTrueEdge(tailNode, headNode, prob);
}

void CfgCodeSelector::genTrueEdge(Node* tailNode, Node* headNode, double prob) {
    assert(tailNode->isBlockNode() && headNode->isBlockNode());

    Inst* inst = (Inst*)tailNode->getLastInst();
    assert(inst!=NULL && inst->hasKind(Inst::Kind_BranchInst));
    BranchInst* br = (BranchInst*)inst;
    br->setTrueTarget(headNode);

    irManager.getFlowGraph()->addEdge(tailNode, headNode, prob);
}

//_______________________________________________________________________________________________
/**  Create false edge (i.e., edge that corresponds to a fallthrough after untaken conditional branch) */

void CfgCodeSelector::genFalseEdge(U_32 tailNodeId,U_32 headNodeId, double prob) 
{
    Node* tailNode = nodes[tailNodeId];
    Node* headNode = nodes[headNodeId];
    genFalseEdge(tailNode, headNode, prob);
}    

void CfgCodeSelector::genFalseEdge(Node* tailNode,Node* headNode, double prob) {
    assert(tailNode->isBlockNode() && headNode->isBlockNode());

    Inst* inst = (Inst*)tailNode->getLastInst();
    assert(inst!=NULL && inst->hasKind(Inst::Kind_BranchInst));
    BranchInst* br = (BranchInst*)inst;
    br->setFalseTarget(headNode);

    irManager.getFlowGraph()->addEdge(tailNode, headNode, prob);
}

//_______________________________________________________________________________________________
/**  Create unconditional edge (i.e., edge that corresponds to fallthrough) */

void CfgCodeSelector::genUnconditionalEdge(U_32 tailNodeId,U_32 headNodeId, double prob) 
{
    Node * tailNode = nodes[tailNodeId];
    Node * headNode = nodes[headNodeId];
    assert(tailNode->isBlockNode());
    assert(headNode->isBlockNode() || headNode == irManager.getFlowGraph()->getExitNode());
    Inst* lastInst = (Inst*)tailNode->getLastInst();
    if (lastInst!=NULL && lastInst->hasKind(Inst::Kind_BranchInst)) {
        BranchInst* br = (BranchInst*)lastInst;
        assert(br->getTrueTarget() != NULL);
        assert(br->getFalseTarget() == NULL);
        br->setFalseTarget(headNode);
    }
    irManager.getFlowGraph()->addEdge(tailNode, headNode, prob);
}

//_______________________________________________________________________________________________
/**  Create switch edges */

void CfgCodeSelector::genSwitchEdges(U_32 tailNodeId, U_32 numTargets, 
                                        U_32 *targets, double *probs, 
                                        U_32 defaultTarget) 
{
    // 
    //  Switch structure:
    //                              
    //      origBlock                                       switchBlock
    //     ===========                  Fallthrough        =============          
    //        ....                       =======>            .......
    //      if (switchVar >= numTargets)                    swTarget= jmp [switchVar + swTableBase]  
    //         jmp defaultTarget                            
    //  
    Node * origBlock = nodes[tailNodeId];
    const Edges& outEdges=origBlock->getOutEdges();
    assert(outEdges.size() == 1);
    Node * switchBlock= outEdges.front()->getTargetNode();
    assert(switchBlock->isBlockNode());
    assert(((Inst*)switchBlock->getLastInst())->hasKind(Inst::Kind_SwitchInst));
    SwitchInst * swInst = (SwitchInst *)switchBlock->getLastInst();

    double    defaultEdgeProb = 1.0;
    defaultEdgeProb = 1.0;
    for (U_32 i = 0; i < numTargets; i++) {
        U_32 targetId = targets[i];
        if ( targetId == defaultTarget) {
            defaultEdgeProb = probs[i];
            break;
        }
        if (std::find(targets, targets+i, targetId)!=targets+i) {
            continue; //repeated target
        }
        if (probs[i] < 0) {
            defaultEdgeProb = 0;
            break;
        } 
        defaultEdgeProb -= 1.0/(numTargets+1);
    }

    genTrueEdge(tailNodeId, defaultTarget, defaultEdgeProb);

    //  Fix probability of fallthrough edge
    if (defaultEdgeProb!=0) {
        origBlock->getOutEdges().front()->setEdgeProb(1.0 - defaultEdgeProb);
    }
    //  Generate edges from switchBlock to switch targets
    for (U_32 i = 0; i < numTargets; i++) {
        Node * targetNode = nodes[targets[i]];
        // Avoid generating duplicate edges. Jump table however needs all entries
        if (! switchBlock->isConnectedTo(true, targetNode)) {
            irManager.getFlowGraph()->addEdge(switchBlock, targetNode, probs[i]);
        }
        swInst->setTarget(i, targetNode);
    }
}

//_______________________________________________________________________________________________
/**  Create an edge to the exception dispatch node or unwind node  */

void CfgCodeSelector::genExceptionEdge(U_32 tailNodeId, U_32 headNodeId, double prob) 
{
    Node * headNode = nodes[headNodeId];
    Node * tailNode = nodes[tailNodeId];
    assert(headNode->isDispatchNode() || headNode->isExitNode()); 
    irManager.getFlowGraph()->addEdge(tailNode, headNode, prob);
}

//_______________________________________________________________________________________________
/**  Create catch edge */

void CfgCodeSelector::genCatchEdge(U_32 tailNodeId, 
                                      U_32 headNodeId,
                                      U_32 priority,
                                      Type*  exceptionType, 
                                      double prob) 
{
    Node * headNode = nodes[headNodeId];
    Node * tailNode = nodes[tailNodeId];
    assert(tailNode->isDispatchNode());
    assert(headNode->isBlockNode());
    CatchEdge* edge = (CatchEdge*)irManager.getFlowGraph()->addEdge(tailNode, headNode, prob);
    edge->setType(exceptionType);
    edge->setPriority(priority);
}


//_______________________________________________________________________________________________
/**  Cfg code selector is notified that method contains calls */
void CfgCodeSelector::methodHasCalls(bool nonExceptionCall) 
{
    irManager.setHasCalls();
    if (nonExceptionCall)
        irManager.setHasNonExceptionCalls();
}


///////////////////////////////////////////////////////////////////////////////////
//
//                     class VarGenerator
//
///////////////////////////////////////////////////////////////////////////////////

//_______________________________________________________________________________________________
U_32 VarGenerator::defVar(Type* varType, bool isAddressTaken, bool isPinned) 
{
    Opnd * opnd=irManager.newOpnd(varType);
    return opnd->getId(); 
}

//_______________________________________________________________________________________________
void VarGenerator::setManagedPointerBase(U_32 managedPtrVarNum, U_32 baseVarNum) 
{
}


///////////////////////////////////////////////////////////////////////////////////
//
//                     class MethodCodeSelector
//
///////////////////////////////////////////////////////////////////////////////////

//_______________________________________________________________________________________________
/**  Generate variable operands */

void MethodCodeSelector::genVars(U_32           numVars, VarCodeSelector& varCodeSelector) 
{
    numVarOpnds = numVars;
    VarGenerator varCodeSelectorCallback(irManager,*this);
    varCodeSelector.genCode(varCodeSelectorCallback);
}

//_______________________________________________________________________________________________
/** Update register usage */

void MethodCodeSelector::updateRegUsage() 
{
}

//_______________________________________________________________________________________________
/** Set persistent ids, and others for nodes that exist only in the code generator CFG */

void CfgCodeSelector::fixNodeInfo() 
{
    MemoryManager tmpMM("Ia32CS:fixNodeInfoMM");
    ControlFlowGraph* fg = irManager.getFlowGraph();
    Nodes nodes(tmpMM);
    fg->getNodes(nodes); //copy nodes -> loop creates new ones, so we can't use reference to cfg->getNodes()
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        // connect throw nodes added during inst code selection to corresponding dispatch or unwind nodes
        if (node->isBlockNode()){
            Inst * lastInst = (Inst*)node->getLastInst();
            if (lastInst) {
                Inst * prevInst = lastInst->getPrevInst();
                if(prevInst && prevInst->getKind() == Inst::Kind_BranchInst) {
                    Edge * ftEdge = node->getFalseEdge();
                    Edge * dbEdge = node->getTrueEdge();
                    assert(ftEdge && dbEdge);

                    Node* newBB =  fg->createBlockNode();
                    Node* nextFT =  ftEdge->getTargetNode();
                    Node* nextDB = dbEdge->getTargetNode();

                    fg->removeEdge(ftEdge);
                    fg->removeEdge(dbEdge);

                    newBB->appendInst(irManager.newBranchInst(lastInst->getMnemonic(), nextDB, nextFT));
                    lastInst->unlink();

                    //now fix prev branch successors
                    BranchInst* prevBranch = (BranchInst*)prevInst;
                    assert(prevBranch->getTrueTarget() == NULL && prevBranch->getFalseTarget() == NULL);
                    prevBranch->setTrueTarget(lastInst->getMnemonic() == Mnemonic_JZ? nextFT : nextDB);
                    prevBranch->setFalseTarget(newBB);
              
                    
                    fg->addEdge(node, lastInst->getMnemonic() == Mnemonic_JZ? nextFT : nextDB, 0);
                    fg->addEdge(node, newBB, 0);
                    fg->addEdge(newBB, nextDB, 0); 
                    fg->addEdge(newBB, nextFT, 0);
                }
            }
            if (node->getOutDegree() == 0){ // throw node
                assert(node->getInDegree()==1);
                Node* bbIn = node->getInEdges().front()->getSourceNode();
                assert(bbIn!=NULL);
                Node * target=bbIn->getExceptionEdgeTarget();
                assert(target!=NULL);
                fg->addEdge(node, target, 1.0);
            }
            // fixup empty catch blocks otherwise respective catchEdges will be lost
            // There is no [catchBlock]-->[catchHandler] edge. Catch block will be removed
            // as an empty one and exception handling will be incorrect
            if (node->isCatchBlock() && node->isEmpty()) {
                assert(node->getInDegree()==1);
                Edge* catchEdge = node->getInEdges().front();
                assert(catchEdge->getSourceNode()->isDispatchNode());
                assert(node->getOutDegree()==1);
                Node* succ = node->getUnconditionalEdgeTarget();
                while( succ->isEmpty() && (succ->getOutDegree() == 1) ) {
                    succ = succ->getUnconditionalEdgeTarget();
                }
                assert(succ && ((Inst*)succ->getFirstInst())->hasKind(Inst::Kind_CatchPseudoInst));
                fg->replaceEdgeTarget(catchEdge,succ,true/*keepOldBody*/);
            }
        }
    }
}

//_______________________________________________________________________________________________
/**  Generate heap base initialization */

void MethodCodeSelector::genHeapBase() 
{
}

//_______________________________________________________________________________________________
/** Generate control flow graph */

MethodCodeSelector::MethodCodeSelector(
                    ::Jitrino::SessionAction* _sa, 
                    CompilationInterface& compIntfc,
                   MemoryManager&          irMM,
                   MemoryManager&          codeSelectorMM,
                   IRManager&              irM)
: sa(_sa), compilationInterface(compIntfc),
irMemManager(irMM), codeSelectorMemManager(codeSelectorMM),
irManager(irM),
methodDesc(NULL),
edgeProfile(NULL)
{  
    ProfilingInterface* pi = irManager.getProfilingInterface();
    if (pi!=NULL && pi->isProfilingEnabled(ProfileType_Edge, JITProfilingRole_GEN)) {
        edgeProfile = pi->getEdgeMethodProfile(irMM, irM.getMethodDesc(), JITProfilingRole_GEN);
    }

}


void MethodCodeSelector::genCFG(U_32 numNodes, CFGCodeSelector& codeSelector, 
                                   bool useEdgeProfile) 
{
    ControlFlowGraph* fg = irManager.getFlowGraph();
    fg->setEdgeProfile(useEdgeProfile);

    CfgCodeSelector cfgCodeSelector(sa, compilationInterface, *this,
                        codeSelectorMemManager,numNodes, irManager);
    { 
        AutoTimer tm(selectionTimer); 
        if( NULL == irManager.getEntryPointInst() ) {
            irManager.newEntryPointPseudoInst( irManager.getDefaultManagedCallingConvention() );
        }
        codeSelector.genCode(cfgCodeSelector);
    }
    {
        AutoTimer tm(fixNodeInfoTimer); 
        irManager.expandSystemExceptions(0);
        cfgCodeSelector.fixNodeInfo();
    }
    {
        AutoTimer tm(blockMergingTimer); 
        fg->purgeEmptyNodes(false, true);
        fg->mergeAdjacentNodes(true, false);
        fg->purgeUnreachableNodes();
    }
}

//_______________________________________________________________________________________________



}; // namespace Ia32
};
