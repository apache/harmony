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
 * @author George A. Timoshenko
 */

#include "Ia32IRManager.h"
#include "VMInterface.h"
#include "Ia32Tls.h"
#include <open/hythread_ext.h>

namespace Jitrino
{
namespace Ia32{

const U_32 BBPollingMaxVersion = 6;

//========================================================================================
// class BBPolling
//========================================================================================
/**
    class BBPolling implements utilities for back branch polling pass 

*/
class BBPolling
{
typedef ::std::pair<U_32,U_32> targetIdDispatchIdPair;
typedef StlMap<targetIdDispatchIdPair,Node*> BBPControllersMap;
public:

    BBPolling(IRManager& ir,U_32 ver) :
        irManager(ir),
        version(ver),
        hasThreadInterruptablePoint(irManager.getMemoryManager(), irManager.getFlowGraph()->getMaxNodeId(), false),
        hasNativeInterruptablePoint(irManager.getMemoryManager(), irManager.getFlowGraph()->getMaxNodeId(), false),
        isOnThreadInterruptablePath(irManager.getMemoryManager(), irManager.getFlowGraph()->getMaxNodeId(), false),
        tlsBaseRegForLoopHeader(irManager.getMemoryManager(), irManager.getFlowGraph()->getMaxNodeId(), NULL),
        bbpCFGControllerForNode(irManager.getMemoryManager()),
        toppestLoopHeader(irManager.getMemoryManager(), irManager.getFlowGraph()->getMaxNodeId(), NULL),
        otherStartNdx(irManager.getMemoryManager(), irManager.getFlowGraph()->getMaxNodeId(), 0),
        loopHeaders(irManager.getMemoryManager()),
        otherEdges(irManager.getMemoryManager()),
        eligibleEdges(irManager.getMemoryManager()),
#ifdef _DEBUG
        interruptablePoints(0),
        pollingPoints(0),
#endif
        loopHeaderOfEdge(irManager.getMemoryManager())
    {
        gcFlagOffsetOffset = VMInterface::flagTLSSuspendRequestOffset();
        calculateInitialInterruptability(version == 5 || version == 6);
        if (version == 2 || version == 3)
            calculateInterruptablePathes();
        // no more calculations here, just collect the edges!
        switch (version) {
            case 1: collectEligibleEdges(); break;
            case 2: collectEligibleEdges2(); break;
            case 3: collectEligibleEdgesRecursive(); break;
            case 4: collectEligibleEdges(); break;
            case 5: collectEligibleEdges(); break;
            case 6: collectEligibleEdges(); break;
            default: assert(0);
        }
        if (Log::isEnabled()) {
            dumpEligibleEdges();
        }
#ifdef _DEBUG
        if (version == 2 || version == 3)
            verify();
#endif
    }

    U_32  numberOfAffectedEdges()     { return (U_32)eligibleEdges.size(); }
    Edge*   getAffectedEdge(U_32 i)   { assert(i < eligibleEdges.size()); return eligibleEdges[i]; }

    Opnd*   getOrCreateTLSBaseReg(Edge* e);

    static bool isThreadInterruptablePoint(const Inst* inst);

    static bool hasNativeInterruptablePoints(const Node* node);
    
    bool    hasAllThreadInterruptablePredecessors(const Node* node);

    Node*   getBBPSubCFGController(U_32 targetId, U_32 dispatchId);
    void    setBBPSubCFGController(U_32 targetId, U_32 dispatchId, Node* node);
    ControlFlowGraph*    createBBPSubCFG(IRManager& ir, Opnd* tlsBaseReg);

private:

    bool    isInterruptable(const BasicBlock* b) {
        return hasThreadInterruptablePoint[b->getId()];
    }
    // select all loopHeaders
    // identify all nodes which hasNativeInterruptablePoints
    // mark all successors of such nodes as hasThreadInterruptablePoint
    void    calculateInitialInterruptability(bool doPassingDown);
    // identify the nodes which are on the way from a loopHeader to a node which hasThreadInterruptablePoints
    void    calculateInterruptablePathes();
    bool    isOnInterruptablePath(Node* node);

    // collect the edges for substition by subCFG that checks the flag and call the helper if the flag.
    // These edges are those from a Node which isOnInterruptablePath to it's succ_node which is not.
    void    collectEligibleEdges();     // all backedges
    void    collectEligibleEdges2();    // all pairs [isOnThreadInterruptablePath]->[!isOnThreadInterruptablePath]
    void    collectEligibleEdgesRecursive(); // recursive selecting of 2
    void    collectEdgesDeeper(Node* node);

    void    dumpEligibleEdges();

#ifdef _DEBUG
    bool    isEligible(Edge* e);
    void    verify();
    void    verifyDeeper(Node* node, Node* enwind, Node* exit);
#endif // _DEBUG

    IRManager&  irManager;

    // version of BBPolling:
    //  0 - must be discarded in runImpl()
    //  1 - insert bbpCFG at all backedges except those going from initiallyInterruptable nodes
    //      (initialInterruptability is not being propagated to the successors. This is the feature of version 5)
    //  2 - path analysis based on searching of pairs [isOnThreadInterruptablePath]->[!isOnThreadInterruptablePath]
    //  3 - recursive version of "2"
    //  4 - "1" + suspension flag addr [TLS base + offset] is calculated before the loop header
    //  5 - like "1" but some backedges are not patched (if all paths through it are uninterpretable)
    //  6 - "4" + "5"
    //  7.. illegal
    U_32  version;
    // storage for ThreadInterruptablePoints information
    //     hasThreadInterruptablePoint[Node->getId()] == true means that the Node is a LoopHeader
    //     OR It has at least one instruction that isThreadInterruptablePoint(inst)
    //     OR all predecessors hasThreadInterruptablePoint or incoming edge is a loopExit
    StlVector<bool> hasThreadInterruptablePoint;
    StlVector<bool> hasNativeInterruptablePoint; // only those that hase at least one InterruptablePoint(inst)
    // storage for InterruptablePathes information
    //     isOnThreadInterruptablePath[Node->getId()] == true means that there is a way from the Node
    //     to another a_node which hasThreadInterruptablePoint[a_node->getId()]
    StlVector<bool> isOnThreadInterruptablePath;
    //     tlsBaseRegs pointers. One per each affected loopHeader
    StlVector<Opnd*>    tlsBaseRegForLoopHeader;
    //     pointers to already prepared bbpCFG. bbpCFG is placed "before" a node to collect all eligible edges.
    //     pair.first - targetNode id
    //     pair.second - sourceNode dispatch edge target id
    BBPControllersMap   bbpCFGControllerForNode;
    //     to get the toppest loop header of the given without calling getLoopHeader
    StlVector<Node*>    toppestLoopHeader;
    //     start index in otheredges collection for the topmost loop headers
    StlVector<U_32> otherStartNdx;

    // just a collection of loop headers of the method (Basic blocks only!)
    StlVector<Node*> loopHeaders;
    // edges which are not a back edge
    StlVector<Edge*> otherEdges;
    // edges for inserting BBPolling subCFG
    StlVector<Edge*> eligibleEdges;

    /// Offset of the suspension flag in the VM's structure stored in TLS.
    U_32 gcFlagOffsetOffset;

#ifdef _DEBUG
    U_32  interruptablePoints;
    U_32  pollingPoints;
#endif
    StlHashMap<Edge*, Node *> loopHeaderOfEdge;

}; // BBPolling class

//___________________________________________________________________________________________________

class BBPollingTransformer : public SessionAction {
public:
    BBPollingTransformer() : hasSideEffects(false){}

    bool hasSideEffects;

    void runImpl(){
        LoopTree* lt = irManager->getFlowGraph()->getLoopTree();
        if(!lt->hasLoops()) {
            return;
        }
        version = getIntArg("version", 6);
        if(version == 0) {
            return;
        }
        if(version > BBPollingMaxVersion) {
            assert(0);
            return;
        } 
        if (Log::isEnabled()) {
            Log::out() << "BBPolling transformer version="<< version <<" STARTED" << ::std::endl;
        }
        BBPolling bbp = BBPolling(*irManager, version);
        U_32 numOfAffectedEdges = bbp.numberOfAffectedEdges();
        hasSideEffects = numOfAffectedEdges != 0;
        ControlFlowGraph* fg = irManager->getFlowGraph();
        // Foreach eligible backedge create bbpCFG and inline it between the backedge's Tail and it's target
        for (U_32 j = 0; j < numOfAffectedEdges; j++) {
            Edge* edge = bbp.getAffectedEdge(j);

            // get or create and insert before the loopHeade a basic block for calculating TLS base
            Opnd* tlsBaseReg = bbp.getOrCreateTLSBaseReg(edge);

            U_32 originalTargetId = edge->getTargetNode()->getId();
            Edge*  srcDispatchEdge  = edge->getSourceNode()->getExceptionEdge();
            U_32 sourceDispatchId = srcDispatchEdge ? srcDispatchEdge->getTargetNode()->getId() : 0;
            // CFG for inlining
            Node* bbpCFGController = bbp.getBBPSubCFGController(originalTargetId,sourceDispatchId);
            if (bbpCFGController) { // just retarget the edge
                fg->replaceEdgeTarget(edge, bbpCFGController, true);
            } else { // we need a new bbpCFG
                ControlFlowGraph* bbpCFG = bbp.createBBPSubCFG(*irManager, tlsBaseReg);
            
                // Inlining bbpCFG at edge
                if (fg->getUnwindNode()==NULL) {//inlined cfg has dispatch flow -> add unwind to parent if needed
                    Node* unwind = fg->createDispatchNode();
                    fg->addEdge(unwind, fg->getExitNode());
                    fg->setUnwindNode(unwind);
                }
                fg->spliceFlowGraphInline(edge, *bbpCFG);
                bbp.setBBPSubCFGController(originalTargetId,sourceDispatchId,bbpCFG->getEntryNode());
            }
        }
        if (Log::isEnabled())
            Log::out() << "BBPolling transformer FINISHED" << ::std::endl;
    } //runImpl()
    U_32  getNeedInfo()const{ return NeedInfo_LoopInfo; }
    U_32  getSideEffects()const{ return hasSideEffects ? SideEffect_InvalidatesLoopInfo|SideEffect_InvalidatesLivenessInfo : 0; }
    bool    isIRDumpEnabled(){ return true; }
    U_32  version;
};

static ActionFactory<BBPollingTransformer> _bbp("bbp");

Opnd*
BBPolling::getOrCreateTLSBaseReg(Edge* e)
{
    // TLS base reg is one per each nested loop
    Node* loopHeader = toppestLoopHeader[loopHeaderOfEdge[e]->getId()];
    assert(loopHeader);

    const U_32 id = loopHeader->getId();

    Opnd* tlsBaseReg = tlsBaseRegForLoopHeader[id];
    if ( tlsBaseReg ) { // it is already created for this loop
        return tlsBaseReg;
    }

    Type* tlsBaseType = irManager.getTypeManager().getUnmanagedPtrType(irManager.getTypeManager().getIntPtrType());

#ifdef _EM64T_
    tlsBaseReg = irManager.newOpnd(tlsBaseType, Constraint(OpndKind_GPReg));
#else
    tlsBaseReg = irManager.newOpnd(tlsBaseType, Constraint(RegName_EAX)|
                                                            RegName_EBX |
                                                            RegName_ECX |
                                                            RegName_EDX |
                                                            RegName_EBP |
                                                            RegName_ESI |
                                                            RegName_EDI);
#endif
    // Basic Block for flag address calculating. (To be inserted before the loopHeaders)
    Node * bbpFlagAddrBlock = irManager.getFlowGraph()->createBlockNode();
    Opnd* tlsBase = createTlsBaseLoadSequence(irManager, bbpFlagAddrBlock);

    if (version == 4 || version == 6) {
        Opnd * offset = irManager.newImmOpnd(tlsBaseType, gcFlagOffsetOffset);
        bbpFlagAddrBlock->appendInst(irManager.newInstEx(Mnemonic_ADD, 1, tlsBaseReg, tlsBase, offset));
    } else {
        bbpFlagAddrBlock->appendInst(irManager.newInst(Mnemonic_MOV, tlsBaseReg, tlsBase));
    }

    // inserting bbpFlagAddrBlock before the given loopHeader
    U_32 startIndex = otherStartNdx[id];

    ControlFlowGraph* fg = irManager.getFlowGraph();
    for (U_32 otherIdx = startIndex; ; otherIdx++) {
        if (otherIdx == otherEdges.size())
            break;
        Edge* other = otherEdges[otherIdx];
        if (other->getTargetNode() != loopHeader)
            break;
        fg->replaceEdgeTarget(other, bbpFlagAddrBlock, true);
    }

    assert(loopHeader->isBlockNode());
    fg->addEdge(bbpFlagAddrBlock, loopHeader, 1);

    tlsBaseRegForLoopHeader[id] = tlsBaseReg;
    return tlsBaseReg;
}

Node*
BBPolling::getBBPSubCFGController(U_32 targetId, U_32 dispatchId)
{
    const BBPControllersMap::iterator iter = bbpCFGControllerForNode.find(::std::make_pair(targetId,dispatchId));
    if (iter == bbpCFGControllerForNode.end()) {
        return NULL;
    } else {
        return (*iter).second;
    }
}

void
BBPolling::setBBPSubCFGController(U_32 targetId, U_32 dispatchId, Node* node)
{
    assert(node);
    assert(!bbpCFGControllerForNode.has(::std::make_pair(targetId,dispatchId)));
    bbpCFGControllerForNode[::std::make_pair(targetId,dispatchId)] = node;
}

// This is a debug hack. It does not work with gcc (warning when casting to int64 below)
#if 0
static void callerStarter()
{
   ::std::cout << "Caller started!" << ::std::endl;
//    DebugBreak();
}

static void controllerStarter()
{
    ::std::cout << "Controller started!" << ::std::endl;
//    DebugBreak();
}
#endif

ControlFlowGraph*
BBPolling::createBBPSubCFG(IRManager& irManager, Opnd* tlsBaseReg)
{
    Type* typeInt32 = irManager.getTypeManager().getPrimitiveType(Type::Int32);
    // CFG for inlining
    ControlFlowGraph* bbpCFG = irManager.createSubCFG(true, true);
    Node* bbpBBController=bbpCFG->getEntryNode();
    Node* bbpBBHelpCaller=bbpCFG->createBlockNode();
    Node* bbpReturn =bbpCFG->getReturnNode();
    
    // Controller node

// This is a debug hack. It does not work with gcc (warning when casting to int64)
#if 0
    Opnd * target = irManager.newImmOpnd(   irManager.getTypeManager().getUnmanagedPtrType(irManager.getTypeManager().getIntPtrType()),
                                            (int64)controllerStarter);
    Inst* dbgCall = irManager.newCallInst(target, &CallingConvention_STDCALL, 0, NULL, NULL, NULL);
    bbpBBController->appendInsts(dbgCall);
#endif

    Opnd* gcFlag = NULL;
    if (version == 4 || version == 6) {
        gcFlag = irManager.newMemOpnd(typeInt32, MemOpndKind_Any, tlsBaseReg, 0);
    } else {
        gcFlag = irManager.newMemOpnd(typeInt32, MemOpndKind_Any, tlsBaseReg, gcFlagOffsetOffset);
    }

    Opnd* zero = irManager.newImmOpnd(typeInt32, 0);
    bbpBBController->appendInst(irManager.newInst(Mnemonic_CMP, gcFlag, zero));
    bbpBBController->appendInst(irManager.newBranchInst(Mnemonic_JNZ, bbpBBHelpCaller, bbpReturn));


    // Helper Caller node

// This is a debug hack. It does not work with gcc (warning when casting to int64)
#if 0
    target = irManager.newImmOpnd(  irManager.getTypeManager().getUnmanagedPtrType(irManager.getTypeManager().getIntPtrType()),
                                    (int64)callerStarter);
    dbgCall = irManager.newCallInst(target, &CallingConvention_STDCALL, 0, NULL, NULL, NULL);
    bbpBBHelpCaller->appendInsts(dbgCall);
#endif

    bbpBBHelpCaller->appendInst(irManager.newRuntimeHelperCallInst(
        VM_RT_GC_SAFE_POINT, 0, NULL, NULL)
        );
    
    bbpCFG->addEdge(bbpBBController,bbpBBHelpCaller, 0);
    bbpCFG->addEdge(bbpBBController, bbpReturn, 1);
    bbpCFG->addEdge(bbpBBHelpCaller, bbpReturn, 1);
    bbpCFG->addEdge(bbpBBHelpCaller, bbpCFG->getUnwindNode(), 0);

    return bbpCFG;
}

bool
BBPolling::isThreadInterruptablePoint(const Inst* inst) {
    if ( inst->getMnemonic() == Mnemonic_CALL ) {
        Opnd::RuntimeInfo* ri = inst->getOpnd(((ControlTransferInst*)inst)->getTargetOpndIndex())->getRuntimeInfo();
        if (!ri) {
            return false;
        } else {
            if ( ri->getKind() == Opnd::RuntimeInfo::Kind_MethodDirectAddr &&
                 ((MethodDesc*)ri->getValue(0))->isNative() )
            {
                return true;
            }
            CompilationInterface* ci = CompilationContext::getCurrentContext()->getVMCompilationInterface();
            if ( ri->getKind() == Opnd::RuntimeInfo::Kind_HelperAddress &&
                 ci->isInterruptible((VM_RT_SUPPORT)(POINTER_SIZE_INT)ri->getValue(0)) )
            {
                return true;
            }
        }
    }
    return false;
}

bool
BBPolling::hasNativeInterruptablePoints(const Node* node) {
    if (node->isBlockNode()) {
        // If current BB has an inst that is ThreadInterruptablePoint itself
        // the hasThreadInterruptablePoint becomes true for it
        for (Inst* inst = (Inst*)node->getFirstInst(); inst!=NULL; inst = inst->getNextInst()) {
            if (BBPolling::isThreadInterruptablePoint(inst)) {
                return true;
            }
        }
    }
    return false;
}

bool
BBPolling::hasAllThreadInterruptablePredecessors(const Node* node) {
    LoopTree* lt = irManager.getFlowGraph()->getLoopTree();
    assert(lt->isValid());
    const Edges& edges = node->getInEdges();
    
    if (edges.size()==0) return false;    

    // All the predecessors must be processed earlier!
    for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
        Edge* e = *ite;
        if (!hasThreadInterruptablePoint[e->getSourceNode()->getId()] && !lt->isLoopExit(e)) {
            return false;
        }
    }
    return true;
}

void
BBPolling::calculateInitialInterruptability(bool doPassingDown)
{
    LoopTree* lt = irManager.getFlowGraph()->getLoopTree();
    assert(lt->isValid());
    const Nodes& postOrder = irManager.getFlowGraph()->getNodesPostOrder();
    for (Nodes::const_reverse_iterator it = postOrder.rbegin(), end = postOrder.rend(); it!=end; ++it) {
        Node* node = *it;
        const U_32 id = node->getId();

        if (lt->isLoopHeader(node) && node->isBlockNode()) {
            loopHeaders.push_back(node);
            // here we calculate loopHeaders with depth == 0, and otherEdges for them
            // to create only one TLS base loader per each nested loop
            Node* loopHeader = node;
            LoopNode* loop = lt->getLoopNode(loopHeader, false);
            assert(loopHeader == loop->getHeader());
            while (loop->getDepth() > 1) {
                loop = loop->getParent();
            }
            loopHeader = loop->getHeader();
            if ( !toppestLoopHeader[id] ) {
                toppestLoopHeader[id] = loopHeader;
                // here we need to remember all otheredges and their start index for particular loopHeader
                otherStartNdx[loopHeader->getId()] = (U_32)otherEdges.size();
                const Edges& edges = loopHeader->getInEdges();
                for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
                    Edge* e = *ite;
                    if ( !lt->isBackEdge(e) ) {
                        otherEdges.push_back(e);
                    }
                }
            }
        }

        // If current BB has an inst that is ThreadInterruptablePoint itself
        // or it is a Dispatch Node
        // the hasThreadInterruptablePoint becomes true for it
        hasNativeInterruptablePoint[id] = BBPolling::hasNativeInterruptablePoints(node);
        if (hasNativeInterruptablePoint[id]) {
            hasThreadInterruptablePoint[id] = true;
            if (Log::isEnabled())
                Log::out() << "        hasNativeInterruptablePoint["<<id<<"] == true" << ::std::endl;
        }

        if (doPassingDown) {
            // if all the predecessors hasThreadInterruptablePoint or incoming edge is a loopExit
            // mark the node as it hasThreadInterruptablePoint (if it is not a loop header)
            if (!hasThreadInterruptablePoint[id] && !lt->isLoopHeader(node) ) {
                if ( hasThreadInterruptablePoint[id] = hasAllThreadInterruptablePredecessors(node) &&
                     Log::isEnabled() )
                {
                    Log::out() << "        (inherited) hasThreadInterruptablePoint["<<id<<"] == true" << ::std::endl;
                }
            }
        }
    }
} // calculateInitialInterruptability

void
BBPolling::calculateInterruptablePathes()
{
    assert(loopHeaders.size());
    LoopTree* lt = irManager.getFlowGraph()->getLoopTree();
    U_32 maxLoopDepth = lt->getMaxLoopDepth();

    // process the deepest firstly
    for (U_32 currDepth = maxLoopDepth; currDepth > 0; currDepth--)
    {
        for (U_32 i=0; i < loopHeaders.size(); i++)
        {
            Node* loopHeader = loopHeaders[i];
            LoopNode* loop = lt->getLoopNode(loopHeader, false);
            assert(loopHeader == loop->getHeader());
            const U_32 loopHeaderId = loopHeader->getId();
            if (loop->getDepth() == currDepth - 1) {
                if (hasNativeInterruptablePoint[loopHeaderId])  {
                    // fortunately we can skip this loop, because it is already interruptable ( it has respective
                    // instructions and it was calculated earlier in calculateInitialInterruptability() )
                    continue;
                }

                // Process all successors of the loopHeader
                const Edges& edges = loopHeader->getOutEdges();
                for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
                    Edge* e = *ite;
                    Node* succ = e->getTargetNode();
                    if( loop->inLoop(succ) && succ == loopHeader) {
                        isOnInterruptablePath(succ);
                    } else {
                        // this edge does not go into the loop. Skip it.
                        // OR our loop consist of only one BB (the loopHeader itself). Skip the backedge.
                    }
                }
                // After the loop is processed it gets hasThreadInterruptablePoint mark
                // to prvent the outer loop processing from going inside the current one
                // Also it gets isOnThreadInterruptablePath mark for further selection of eligibleEdges
                hasThreadInterruptablePoint[loopHeaderId] = true;
                isOnThreadInterruptablePath[loopHeaderId] = true;
                if (Log::isEnabled()) {
                    Log::out() << "    loopHeader:" << ::std::endl;
                    Log::out() << "        hasThreadInterruptablePoint["<<loopHeaderId<<"] == true" << ::std::endl;
                    Log::out() << "        isOnThreadInterruptablePath["<<loopHeaderId<<"] == true" << ::std::endl;
                }
            }
        }
    }
} // calculateInterruptablePathes

bool
BBPolling::isOnInterruptablePath(Node* node)
{
    const U_32 id = node->getId();
    // the order of these check is essential! (because in case of nested loops
    // if the node is a loopHeader of a nested loop we must return true)
    LoopTree* lt = irManager.getFlowGraph()->getLoopTree();
    if( hasThreadInterruptablePoint[id] ) {
        isOnThreadInterruptablePath[id] = true;
        if (Log::isEnabled())
            Log::out() << "        isOnThreadInterruptablePath["<<id<<"] == true" << ::std::endl;
        return true;
    } else if ( lt->isLoopHeader(node) ) {
        return false; // loopHeader also breaks the recursion
    }

    bool retValue = false;
    const Edges& edges = node->getOutEdges();
    for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
        Edge* e= *ite;
        Node* succ = e->getTargetNode();
        
        if( !lt->isLoopExit(e) && isOnInterruptablePath(succ) ) {
            if (Log::isEnabled())
                Log::out() << "        isOnThreadInterruptablePath["<<id<<"] == true" << ::std::endl;
            isOnThreadInterruptablePath[id] = true;
            // Must not break here.
            // All outgoing ways must be passed till the LoopHeader or hasThreadInterruptablePoint
            // The LoopHeader at the edge's head means
            //  - that the edge is a LoopExit
            //  - or it is a header of nested loop which must be processed earlier
            //    so it hasThreadInterruptablePoint
            retValue = true;
        }
    }
    return retValue;
} // isOnInterruptablePath

void
BBPolling::collectEligibleEdges()
{
    LoopTree* lt = irManager.getFlowGraph()->getLoopTree();
    for (U_32 i=0; i < loopHeaders.size(); i++) {
        Node* node = loopHeaders[i];
        const Edges& edges = node->getInEdges();
        for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
            Edge* e = *ite;
            if ( lt->isBackEdge(e) && !e->isCatchEdge() &&
                 !hasThreadInterruptablePoint[e->getSourceNode()->getId()]
               )
            {
                eligibleEdges.push_back(e);
                loopHeaderOfEdge[e] = node;
            }
        }
    }

} // collectEligibleEdges

void
BBPolling::collectEligibleEdges2()
{
    LoopTree* lt = irManager.getFlowGraph()->getLoopTree();
    const Nodes& nodes = irManager.getFlowGraph()->getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        const U_32 id = node->getId();
        if ( isOnThreadInterruptablePath[id] && ! hasNativeInterruptablePoint[id] ) {
            const Edges& edges = node->getOutEdges();
            LoopNode* nodeLoop = lt->getLoopNode(node, false);
            assert(nodeLoop!=NULL);
            for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
                Edge* e = *ite;
                Node* succ = e->getTargetNode();
                U_32 succId = succ->getId();
                if (nodeLoop->inLoop(succ)) {
                    if( !isOnThreadInterruptablePath[succId] ) {
                        eligibleEdges.push_back(e);
                        loopHeaderOfEdge[e] = nodeLoop->getHeader();
                    }
                } else {
                    continue; // the edge leaves our loop
                }
            }
        }
    }

} // collectEligibleEdges2

void
BBPolling::collectEligibleEdgesRecursive()
{
    for (U_32 i=0; i < loopHeaders.size(); i++)
    {
        collectEdgesDeeper(loopHeaders[i]);
    }
    std::sort(eligibleEdges.begin(), eligibleEdges.end());
    StlVector<Edge*>::iterator newEnd = std::unique(eligibleEdges.begin(), eligibleEdges.end());
    eligibleEdges.resize(newEnd - eligibleEdges.begin());

} // collectEligibleEdgesRecursive

void
BBPolling::collectEdgesDeeper(Node* node)
{
    U_32 id = node->getId();
    if (hasNativeInterruptablePoint[id]) {
        return;
    }
    LoopTree* lt = irManager.getFlowGraph()->getLoopTree();
    if ( isOnThreadInterruptablePath[id] ) {
        // if the node has an outgoing edge to the node which:
        //  - is not OnThreadInterruptablePath
        //  - OR this ougoing edge points to the node itself
        // add the edge to eligibleEdges if it is not a loopExit
        const Edges& edges = node->getOutEdges();
        for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
            Edge* e = *ite;
            Node* succ = e->getTargetNode();
            U_32 succId = succ->getId();
            if ( succ == node ) {
                eligibleEdges.push_back(e);
                loopHeaderOfEdge[e] = node;
            }
            LoopNode* nodeLoop= lt->getLoopNode(node, false);
            assert(nodeLoop);
            if ( nodeLoop->inLoop(succ) && succ != nodeLoop->getHeader()) {
                if(!isOnThreadInterruptablePath[succId])
                {
                    eligibleEdges.push_back(e);
                    loopHeaderOfEdge[e] = nodeLoop->getHeader();
                } else {
                    if (lt->isLoopHeader(succ)) {
                        continue; // there are no eligible edges deeper (nested loop)
                    } else {
                        collectEdgesDeeper(succ);
                    }
                }
            } else if (lt->isBackEdge(e)) {
                // get a backedge and have not met any interruptable points earlier
                assert(e->getTargetNode() == nodeLoop->getHeader());
                eligibleEdges.push_back(e);
                loopHeaderOfEdge[e] = nodeLoop->getHeader();
            } else {
                continue; // the edge leaves our loop
            }
        }
    } else {
        assert(0);
    }
} // collectEdgesDeeper

void
BBPolling::dumpEligibleEdges()
{
    assert(Log::isEnabled());
    Log::out() << "    EligibleEdges:" << ::std::endl;
    for (U_32 i = 0; eligibleEdges.size() > i; i++) {
        Edge* e = eligibleEdges[i];
        U_32 srcId  = e->getSourceNode()->getId();
        U_32 succId = e->getTargetNode()->getId();
        Log::out() << "        eligibleEdge ["<<srcId<<"]-->["<<succId<<"]" << ::std::endl;
    }
    Log::out() << "    EligibleEdges END! " << ::std::endl;
}

#ifdef _DEBUG

bool
BBPolling::isEligible(Edge* e)
{
    return ::std::find(eligibleEdges.begin(),eligibleEdges.end(),e) == eligibleEdges.end();
} // isEligible

void
BBPolling::verify()
{
    if (Log::isEnabled())
        Log::out() << "BBPolling verification started" << ::std::endl;
    interruptablePoints = 0;
    pollingPoints = 0;

    Node* unwind = irManager.getFlowGraph()->getUnwindNode();
    Node* exit = irManager.getFlowGraph()->getExitNode();

    for (U_32 i=0; i < loopHeaders.size(); i++)
    {
        assert(interruptablePoints == 0);
        assert(pollingPoints == 0);

        Node* loopHeader = loopHeaders[i];
        bool nativelyInterruptable = BBPolling::hasNativeInterruptablePoints(loopHeader);

        if(nativelyInterruptable)
            interruptablePoints++;
            
        if (Log::isEnabled())
            Log::out() << "   verification for loopHeader id=" << loopHeader->getId() << "STARTED" << ::std::endl;
        verifyDeeper(loopHeader,unwind,exit);

        if (Log::isEnabled())
            Log::out() << "   verification for loopHeader id=" << loopHeader->getId() << "FINISHED" << ::std::endl;
        if(nativelyInterruptable)
            interruptablePoints--;
    }

    assert(interruptablePoints == 0);
    assert(pollingPoints == 0);
    if (Log::isEnabled())
        Log::out() << "BBPolling verification successfully finished" << ::std::endl;
} // verify

void
BBPolling::verifyDeeper(Node* node, Node* unwind, Node* exit)
{
    const Edges& edges = node->getOutEdges();
    if (Log::isEnabled()) {
        Log::out() << "   verification:  NODE id=" << node->getId() << ::std::endl;
    }
    
    LoopTree* lt = irManager.getFlowGraph()->getLoopTree();

    for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
        Edge* e= *ite;
        bool eligible = isEligible(e);
        Node* succ = e->getTargetNode();

        if ( lt->isLoopExit(e) || succ == unwind ) {
            continue;
        }
        if (Log::isEnabled())
            Log::out() << "   verification:  succ id=" << succ->getId() << ::std::endl;
        if ( lt->isBackEdge(e)) {
            if (Log::isEnabled())
                Log::out() << "   verification BackEdge ["<<node->getId()<<"]-->["<<succ->getId()<<"]" << ::std::endl;
            if(pollingPoints == 0 && eligible) 
                continue;
            if(pollingPoints == 1 && !eligible)
                continue;
            if(pollingPoints == 0 && interruptablePoints > 0 && !eligible)
                continue;
            assert(0);
        }
        if (lt->isLoopHeader(succ)) { // nested loop
            if (Log::isEnabled()) {
                Log::out() << "   verification NestedLoop" << ::std::endl;
            }
            if(pollingPoints == 0 && !eligible) {
                continue;
            }
            assert(0);
        }

        bool nativelyInterruptable = BBPolling::hasNativeInterruptablePoints(succ);
        if (nativelyInterruptable)
            interruptablePoints++;
        if (eligible)
            pollingPoints++;

        verifyDeeper(succ,unwind,exit);

        if (nativelyInterruptable)
            interruptablePoints--;
        if (eligible)
            pollingPoints--;
    }
} // verifyDeeper

#endif // _DEBUG

}}; // namespace Ia32


