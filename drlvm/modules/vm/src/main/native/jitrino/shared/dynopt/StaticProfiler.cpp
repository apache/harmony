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
 * @author Intel, Mikhail Y. Fursov
 *
 */

#include "StaticProfiler.h"
#include "ControlFlowGraph.h"
#include "Dominator.h"
#include "Loop.h"
#include "constantfolder.h"
#include "mkernel.h"
#include "Stl.h"
#include "FlowGraph.h"

/** see article:  "Static Branch Frequency and Program Profile Analysis / Wu, Laurus, IEEE/ACM 1994" */

//TODO: avoid using postdom information. A lot of exceptions-paths make it useless.

namespace Jitrino{

DEFINE_SESSION_ACTION(StaticProfilerPass, statprof, "Perform edge annotation pass based on static heuristics");

class StaticProfilerContext;


#define MAX(a,b) (((a) > (b)) ? (a) : (b))
#define ABS(a) (((a) > (0)) ? (a) : -(a))

//probabilities of taken branch
#define PROB_ALL_EXCEPTIONS         0.0000001
#define PROB_HEURISTIC_FAIL         0.50
#define PROB_LOOP_EXIT              0.10
#define PROB_OPCODE_HEURISTIC       0.84
#define PROB_CALL_HEURISTIC         0.78
#define PROB_LOOP_HEADER_HEURISTIC  0.75
#define PROB_RET_HEURISTIC          0.72
#define PROB_DEVIRT_GUARD_HEURISTIC 0.62
#define PROB_REFERENCE              0.60
#define PROB_STORE_HEURISTIC        0.55
#define PROB_FALLTHRU_HEURISTIC     0.45

#define ACCEPTABLE_DOUBLE_PRECISION_LOSS  0.000000001 

#define DEFAULT_ENTRY_NODE_FREQUENCY 10000

/** returns edge1 probability 
Explicit parameters used here help to avoid code duplication in impl functions
*/
typedef double(*HeuristicsFn) (const StaticProfilerContext*);

static double callHeuristic(const StaticProfilerContext*);
static double returnHeuristic(const StaticProfilerContext*);
static double loopHeaderHeuristic(const StaticProfilerContext*);
static double opcodeHeuristic(const StaticProfilerContext*);
static double storeHeuristic(const StaticProfilerContext*);
static double referenceHeuristic(const StaticProfilerContext*);
static double devirtGuardHeuristic(const StaticProfilerContext*);
static double fallThruHeuristic(const StaticProfilerContext*);
static void setHeatThreshold(IRManager& irm);
typedef std::vector<std::string> StringList;
static void splitString(const char* string, StringList& result, char separator, bool notEmptyTokensOnly);

struct Heuristics {
    std::string name;
    HeuristicsFn fn;
    
    Heuristics(const std::string& _name, HeuristicsFn _fn) : name(_name), fn(_fn){}
    Heuristics(){name="", fn=NULL;}
};

Heuristics HEURISTICS[] =  {
    Heuristics("loop", loopHeaderHeuristic),
    Heuristics("call", callHeuristic), 
    Heuristics("ret", returnHeuristic), 
    Heuristics("opcode", opcodeHeuristic),
    Heuristics("store", storeHeuristic),
    Heuristics("ref", referenceHeuristic),
    Heuristics("guard", devirtGuardHeuristic),
    Heuristics("ft", fallThruHeuristic),
    Heuristics("", NULL)
};

static Heuristics* findHeuristicsByName(const std::string& name) {
    for (int i=0;;i++) {
        Heuristics* h = &HEURISTICS[i];
        if (h->name == name) {
            return h;
        }
        if (h->name.empty()) {
            break;
        }
    }
    return NULL;
}

class StaticProfilerContext {
public:
    IRManager& irm;
    ControlFlowGraph* fg;
    Node* node;
    Edge* edge1; //trueEdge
    Edge* edge2; //falseEdge
    DominatorTree* domTree;
    DominatorTree* postDomTree;
    LoopTree* loopTree;
    StlVector<Heuristics*> heuristics;
    bool doLoopHeuristicsOverride;
    
    StaticProfilerContext(IRManager& _irm) 
        : irm(_irm), fg (&_irm.getFlowGraph()), node(NULL), edge1(NULL), edge2(NULL), 
        domTree(NULL), postDomTree(NULL), loopTree(NULL), 
        heuristics(_irm.getMemoryManager()), doLoopHeuristicsOverride(true)
    { 
        initDefaultHeuristics();
        
        OptPass::computeDominatorsAndLoops(irm, false);
        
        loopTree = irm.getLoopTree();
        domTree = irm.getDominatorTree();
        postDomTree = DominatorBuilder().computePostdominators(irm.getNestedMemoryManager(), fg, true);
        fillActiveHeuristicsList();
    }

private:
    void fillActiveHeuristicsList() {
        const OptimizerFlags& optFlags = irm.getOptimizerFlags();
        doLoopHeuristicsOverride = optFlags.statprof_do_loop_heuristics_override;
        const char* heuristicsNamesList = optFlags.statprof_heuristics;
        if (heuristicsNamesList == NULL) {
            heuristics.resize(defaultHeuristics.size());
            std::copy(defaultHeuristics.begin(), defaultHeuristics.end(), heuristics.begin());
        } else {
            getHeuristicsByNamesList(heuristicsNamesList, heuristics);
        }

    }
  
    static void initDefaultHeuristics() {
        static Mutex initMutex;
        static bool defaultHeuristicsInited = false;
        if (!defaultHeuristicsInited) {
            initMutex.lock();
            if (!defaultHeuristicsInited) {
                getHeuristicsByNamesList("loop,ret,ft,guard", defaultHeuristics);
                defaultHeuristicsInited = true;
            }
            initMutex.unlock();
        }
    }
    
    template <class Container> 
    static void getHeuristicsByNamesList(const char* list, Container& result) {
        StringList nameList;
        splitString(list, nameList, ',', true);
        for (StringList::const_iterator it = nameList.begin(), end = nameList.end(); it!=end; ++it) {
            const std::string& name = *it;
            Heuristics* h = findHeuristicsByName(name);
            assert(h!=NULL);
            result.push_back(h);
        }
    }

private:
    static std::vector<Heuristics*> defaultHeuristics;
    
};

std::vector<Heuristics*> StaticProfilerContext::defaultHeuristics;


static void estimateNode(StaticProfilerContext* c);

/** looks for unconditional path to inner loop header.
 */
static Node* findLoopHeaderOnUncondPath(Node* parentLoopHeader, LoopTree* loopTree,  Edge* edge) {
    Node* node = edge->getTargetNode();
    if (!node->isBlockNode() || node == parentLoopHeader) {
        return NULL;
    }

    if (loopTree->isLoopHeader(node)) {
        return node;
    }
    Edge* uncondEdge = node->getUnconditionalEdge();
    if (uncondEdge==NULL) {
        return NULL;
    }
    return findLoopHeaderOnUncondPath(parentLoopHeader, loopTree, uncondEdge);
}

/** returns TRUE if
 *  1) edge is back-edge
 *  2) if lookForDirectPath == TRUE returns isBackedge(edge->targetNode->targetUncondOutEdge ) 
 *    if targetUncondOutEdge is the only unconditional edge of targetNode
 */
static inline bool isBackedge(LoopTree* loopTree, Edge* e, bool lookForDirectPath) {
    Node* loopHeader = loopTree->getLoopHeader(e->getSourceNode(), false);
    if (loopHeader == NULL) {
        return false;
    }
    Node* targetNode = e->getTargetNode();
    if (targetNode == loopHeader) {
        return true;
    }
    if (lookForDirectPath) {
        Edge* targetUncondOutEdge = targetNode->getUnconditionalEdge();
        if (targetUncondOutEdge !=NULL) {
            return isBackedge(loopTree, targetUncondOutEdge, lookForDirectPath);
        }
    }
    return false;
}

static bool isLoopHeuristicAcceptable(StaticProfilerContext* c) {
    bool edge1IsLoopExit = c->loopTree->isLoopExit(c->edge1);
    bool edge2IsLoopExit = c->loopTree->isLoopExit(c->edge2);
    assert (!(edge1IsLoopExit && edge2IsLoopExit)); //both edges could not be loop exits at the same time
    return edge1IsLoopExit || edge2IsLoopExit;
}

void StaticProfiler::estimateGraph( IRManager& irm, double entryFreq, bool cleanOldEstimations) {
    assert(entryFreq >= 0);
    if (entryFreq == 0) {
        entryFreq = DEFAULT_ENTRY_NODE_FREQUENCY;
    }
    if (irm.getFlowGraph().hasEdgeProfile() && !cleanOldEstimations) {
        fixEdgeProbs(irm);
    } else {
        StaticProfilerContext c(irm);
        const Nodes& nodes = c.fg->getNodes();
        for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
            Node* node = *it;
            c.node = node;
            c.edge1 = NULL;
            c.edge2 = NULL;
            estimateNode(&c);
        }
        c.fg->getEntryNode()->setExecCount(entryFreq);
        c.fg->setEdgeProfile(true);
    }
    irm.getFlowGraph().smoothEdgeProfile();

    if (irm.getHeatThreshold()<=0) {
        setHeatThreshold(irm);
    }
}

static void setHeatThreshold(IRManager& irm) {
    const OptimizerFlags& optimizerFlags = irm.getOptimizerFlags();
    ControlFlowGraph& flowGraph = irm.getFlowGraph();
    double profile_threshold = optimizerFlags.profile_threshold;
    if(optimizerFlags.use_average_threshold) {
        // Keep a running average of method counts.
        static U_32 count = 0;
        static double total = 0.0;
        count++;
        double methodFreq = flowGraph.getEntryNode()->getExecCount();
        assert(methodFreq > 0);
        total += methodFreq;
        irm.setHeatThreshold(total / count);
    } else if(optimizerFlags.use_minimum_threshold) {
        double methodFreq = flowGraph.getEntryNode()->getExecCount();
        if(methodFreq < profile_threshold) {
            methodFreq = profile_threshold;
        }
        irm.setHeatThreshold(methodFreq);
    } else if(optimizerFlags.use_fixed_threshold) {
        irm.setHeatThreshold(profile_threshold);
    } else {
        // Use the method entry
        irm.setHeatThreshold(flowGraph.getEntryNode()->getExecCount());
    }
    if (Log::isEnabled())  {
        Log::out() << "Heat threshold = " << irm.getHeatThreshold() << std::endl; 
    }
}


void StaticProfilerPass::_run(IRManager& irm) {
    OptPass::computeDominatorsAndLoops(irm, false);
    StaticProfiler::estimateGraph(irm, DEFAULT_ENTRY_NODE_FREQUENCY);
}

static void estimateNode(StaticProfilerContext* c) {
    const Edges& edges =  c->node->getOutEdges();
    if (edges.empty()) {
        return;
    } else if (edges.size() == 1) {
        (*edges.begin())->setEdgeProb(1.0);
        return;
    } else {
        for (Edges::const_iterator it = edges.begin(), itEnd = edges.end(); it!=itEnd; it++) {
            Edge* e = *it;
            e->setEdgeProb(0.0);
        }
    }
    Edge* falseEdge = c->node->getFalseEdge();
    Edge* trueEdge = c->node->getTrueEdge();
    double probLeft = 1.0;
    U_32 edgesLeft = (U_32)edges.size();
    if (falseEdge == NULL || trueEdge == NULL) { // can't apply general heuristics.
        Edge* uncondEdge = c->node->getUnconditionalEdge();
        if (uncondEdge) {
            uncondEdge->setEdgeProb(1 - PROB_ALL_EXCEPTIONS);
            probLeft-=uncondEdge->getEdgeProb();
            edgesLeft--;
        }
    } else if (((Inst*)c->node->getLastInst())->isSwitch()) {
        assert(c->node->getExceptionEdge() == 0);
        falseEdge->setEdgeProb(0.5);
        probLeft = 0.5;
        edgesLeft--;
    } else {
        assert(falseEdge->getTargetNode()!=trueEdge->getTargetNode());

        double prob = 0.5; // trueEdge prob

        // separate back-edge heuristic from others heuristics (the way it's done in article)
        c->edge1 = trueEdge;
        c->edge2 = falseEdge;
        if (c->doLoopHeuristicsOverride && isLoopHeuristicAcceptable(c)) {
            prob = c->loopTree->isLoopExit(trueEdge) ? PROB_LOOP_EXIT : 1 - PROB_LOOP_EXIT;
        } else if (c->node->getTraversalNum() == c->fg->getTraversalNum()) {//node is reachable
            // from this point we can apply general heuristics
            for (StlVector<Heuristics*>::const_iterator it = c->heuristics.begin(), end = c->heuristics.end(); it!=end; ++it) {
                Heuristics* h = *it;
                HeuristicsFn fn = h->fn;
                double dprob = (*fn)(c);
                assert(dprob>0 && dprob<1);
                if (dprob!=PROB_HEURISTIC_FAIL) {
                    double newprob = prob*dprob / (prob*dprob + (1-prob)*(1-dprob));
                    assert(newprob>0 && newprob<1);
                    prob = newprob;
                }
            }
        }
        // all other edges (exception, catch..) have lower probability
        double othersProb = edges.size() == 2 ? 0.0 : PROB_ALL_EXCEPTIONS;
        trueEdge->setEdgeProb(prob - othersProb/2);
        falseEdge->setEdgeProb(1 - prob - othersProb/2);
        assert(trueEdge->getEdgeProb()!=0);
        assert(falseEdge->getEdgeProb()!=0);
        probLeft = othersProb;
        edgesLeft-=2;
    }
    
    if (edgesLeft != 0) {
        double probPerEdge = probLeft / edgesLeft;
        for (Edges::const_iterator it = edges.begin(), itEnd = edges.end(); it!=itEnd; it++) {
            Edge* e = *it;
            if (e->getEdgeProb()==0.0) {
                e->setEdgeProb(probPerEdge);
            }
        }
    } 
    
#ifdef _DEBUG //debug check
    double probe = 0;
    for (Edges::const_iterator it = edges.begin(), itEnd = edges.end(); it!=itEnd; it++) {
        Edge* e = *it;
        double dprob = e->getEdgeProb();
        assert (dprob!=0.0);
        probe+=dprob;
    }
    assert(ABS(probe - 1.0) < ACCEPTABLE_DOUBLE_PRECISION_LOSS);
#endif
}


/** looks for Inst with opcode in specified range (both bounds are included) 
  * in specified node and it's unconditional successors.
  */
static inline Inst* findInst(Node* node, Opcode opcodeFrom, Opcode opcodeTo) {
    for (Inst *i = (Inst*)node->getFirstInst(); i!=NULL ; i = i->getNextInst()) {
        Opcode opcode = i->getOpcode();
        if (opcode>=opcodeFrom && opcode<=opcodeTo) {
            return i;
        }
    }
    
    Edge* uncondEdge = node->getUnconditionalEdge(); 
    if (uncondEdge!=NULL && uncondEdge->getTargetNode()->getInEdges().size() == 1) {
        return findInst(uncondEdge->getTargetNode(), opcodeFrom, opcodeTo);
    }
    return NULL;
}

static inline Inst* findInst(Node* node, Opcode opcode) {
    return findInst(node, opcode, opcode);
}


/** Call heuristic (CH). 
 *  Predict a successor that contains
 *  a call and does not post-dominate will not be taken.
 */
static double callHeuristic(const StaticProfilerContext* c) {
    Node* node1 = c->edge1->getTargetNode();
    Node* node2 = c->edge2->getTargetNode();
    bool node1HasCall = findInst(node1, Op_DirectCall, Op_VMHelperCall)!=NULL;
    bool node2HasCall = findInst(node2, Op_DirectCall, Op_VMHelperCall)!=NULL;
    
    if (!node1HasCall && !node2HasCall) {
        return PROB_HEURISTIC_FAIL;
    }

    // at least one node has call from this point
    bool node1Post = false;
    bool node2Post = false;
    
    if (node1HasCall) {
        node1Post = c->postDomTree->dominates(node1, c->node);
        if (!node1Post && !node2HasCall) {
            return 1 - PROB_CALL_HEURISTIC;
        }
    }
    if (node2HasCall) {
        node2Post = c->postDomTree->dominates(node2, c->node);
        if (!node2Post && !node1HasCall) {
            return PROB_CALL_HEURISTIC;
        }
    }
    
    if (node1HasCall != node2HasCall) {
        return PROB_HEURISTIC_FAIL;
    }
    
    // both nodes have call from this point
    return node1Post==node2Post ? PROB_HEURISTIC_FAIL: (!node1Post ? 1- PROB_CALL_HEURISTIC: PROB_CALL_HEURISTIC);
    
}


/** Return heuristic (RH). 
 *  Predict a successor that contains a return will not be taken.
 */
static double returnHeuristic(const StaticProfilerContext* c) {
    bool node1HasRet= findInst(c->edge1->getTargetNode(), Op_Return)!=NULL;
    bool node2HasRet= findInst(c->edge2->getTargetNode(), Op_Return)!=NULL;
    return node1HasRet == node2HasRet ? PROB_HEURISTIC_FAIL: (node1HasRet ? 1 - PROB_RET_HEURISTIC: PROB_RET_HEURISTIC);
}


/**  Loop header heuristic (LHH)
 *   Predict a successor that is a loop header or loop pre-header will be taken
 */
static double loopHeaderHeuristic(const StaticProfilerContext* c) {
    Node* parentLoopHeader = c->loopTree->getLoopHeader(c->node, true);
    
    Node* edge1Loop = findLoopHeaderOnUncondPath(parentLoopHeader, c->loopTree, c->edge1);
    Node* edge2Loop = findLoopHeaderOnUncondPath(parentLoopHeader, c->loopTree, c->edge2);
        
    if ( (edge1Loop!=NULL && edge2Loop!=NULL) || edge2Loop==edge2Loop) {
        return PROB_HEURISTIC_FAIL;
    }
    Node* edge1Target = c->edge1->getTargetNode();
    Node* edge2Target = c->edge2->getTargetNode();
    if ((edge1Loop != NULL && !c->domTree->dominates(edge1Target, edge1Loop))
        || (edge2Loop != NULL && !c->domTree->dominates(edge2Target, edge2Loop))) 
    {
        return PROB_HEURISTIC_FAIL;
    }
    return edge1Loop!=NULL? PROB_LOOP_HEADER_HEURISTIC : 1 - PROB_LOOP_HEADER_HEURISTIC;
}


/** Opcode heuristic (OH)
 *  Predict that a comparison of an integer for less than zero,
 *  less than or equal to zero or equal to a constant, will fail.
 */
static double opcodeHeuristic(const StaticProfilerContext* c) {
    Inst* inst = (Inst*)c->node->getLastInst();
    if (!(inst->getOpcode() == Op_Branch && inst->getSrc(0)->getType()->isInteger())) {
        return PROB_HEURISTIC_FAIL;
    }

    assert (inst->getNumSrcOperands() == 2);
    
    Opnd* src0 = inst->getSrc(0);
    Opnd* src1 = inst->getSrc(1);

    
    ConstInst::ConstValue v0, v1;
    bool src0Const = ConstantFolder::isConstant(src0->getInst(), v0);
    bool src1Const = ConstantFolder::isConstant(src1->getInst(), v1);

    if (!src0Const && !src1Const) {
        return PROB_HEURISTIC_FAIL;
    }

    Type::Tag tag = src0->getType()->tag;

    enum CMP_STATUS { CMP_UNDEF, CMP_OK, CMP_FAIL } rc = CMP_UNDEF;

    if (src0Const && src1Const) {
        rc = v0.dword1 == v1.dword1 && v0.dword2 == v1.dword2 ? CMP_OK : CMP_FAIL;
    } else { //!(src0Const && src1Const)
        ConstInst::ConstValue val =  src0Const ? v0: v1;
        bool constValLEzero = false;
        switch(tag) {
            case Type::IntPtr:  // ptr-sized integer
#ifdef POINTER64
                constValLEzero = val.i8 <= 0;
#else
                constValLEzero = val.i4 <= 0;
#endif
                break;
            case Type::Int8:  constValLEzero = ((I_8)val.i4) <= 0; break;
            case Type::Int16: constValLEzero = ((int16)val.i4) <= 0; break;
            case Type::Int32: constValLEzero = val.i4 <= 0; break;
            case Type::Int64: constValLEzero = val.i8 <= 0; break;
            
            case Type::UIntPtr: //ConstValue is union
            case Type::UInt8: 
            case Type::UInt16:
            case Type::UInt32:
            case Type::UInt64: constValLEzero = ((uint64)val.i8) == 0; break;
            default: assert(false);
        }

        ComparisonModifier cmpOp = inst->getComparisonModifier();
        switch (cmpOp) {
            case Cmp_EQ:     rc = CMP_FAIL; break;//comparison with const fails
            case Cmp_NE_Un:  rc = CMP_OK; break;
            case Cmp_GT:
            case Cmp_GT_Un:
            case Cmp_GTE:
            case Cmp_GTE_Un:
            if (constValLEzero) { //negative or zero branch is not taken
                rc = src0Const ? CMP_FAIL : CMP_OK;
            }
            break;
            case Cmp_Zero: rc = CMP_FAIL; break;//compare with const fails
            case Cmp_NonZero:  rc = CMP_OK;  break;//not equal to const -> OK
            default: break; //to avoid warning  that this enum value is not handled
        }
    }
    if (rc == CMP_UNDEF) {
        return PROB_HEURISTIC_FAIL;
    } else if (rc == CMP_OK) {
        return PROB_OPCODE_HEURISTIC;
    } 
    return 1 - PROB_OPCODE_HEURISTIC;
}


/** Store heuristic (SH). 
*  Predict a successor that contains a store instruction 
*  and does not post dominate will not be taken
*/
static double storeHeuristic(const StaticProfilerContext* c) {
    Node* node1 = c->edge1->getTargetNode();
    Node* node2 = c->edge2->getTargetNode();
    bool node1Accepted = (findInst(node1, Op_TauStInd)!=NULL||findInst(node1, Op_TauStRef)!=NULL);
    bool node2Accepted = (findInst(node2, Op_TauStInd)!=NULL||findInst(node2, Op_TauStRef)!=NULL);
    if (!node1Accepted && !node2Accepted) {
        return PROB_HEURISTIC_FAIL;
    }
    node1Accepted == node1Accepted && !c->postDomTree->dominates(node1, c->node);
    node2Accepted == node2Accepted && !c->postDomTree->dominates(node2, c->node);

    if (!node1Accepted && !node2Accepted) {
        return PROB_HEURISTIC_FAIL;
    }
    return node1Accepted ? 1 - PROB_STORE_HEURISTIC: PROB_STORE_HEURISTIC;
}
/**
 * Give more prob to fallthru branch
 */
static double fallThruHeuristic(const StaticProfilerContext * c) {
    Inst* inst = (Inst*)c->node->getLastInst();
    if (inst->getOpcode() != Op_Branch) {
        return PROB_HEURISTIC_FAIL;
    }
    return PROB_FALLTHRU_HEURISTIC;
}

/** Reference heuristic (RH)
 *  Same as Pointer heuristic (PH) in article
 *  Predict that a comparison of object reference
 *  against null or two references will fail
 */
static double referenceHeuristic(const StaticProfilerContext *c) {
    Inst* inst = (Inst*)c->node->getLastInst();
    if (inst->getOpcode() != Op_Branch || !inst->getSrc(0)->getType()->isObject()) {
        return PROB_HEURISTIC_FAIL;
    }
    enum CMP_STATUS { CMP_UNDEF, CMP_OK, CMP_FAIL } rc = CMP_UNDEF;
    ComparisonModifier cmpOp = inst->getComparisonModifier();
    switch (cmpOp) {
        case Cmp_EQ: rc = CMP_FAIL; break;
        case Cmp_NE_Un: rc = CMP_OK; break;
        case Cmp_Zero: rc = CMP_FAIL; break;
        case Cmp_NonZero: rc = CMP_OK; break;
        default: assert(false);
    }
    assert(rc!=CMP_UNDEF);
    return rc == CMP_OK ? PROB_REFERENCE : 1 - PROB_REFERENCE;
}

/** De-virtualization guard heuristic (DGH) 
  * Predict that comparison of VTablePtr will succeed
  */
static double devirtGuardHeuristic(const StaticProfilerContext *c) {
    Inst* inst = (Inst*)c->node->getLastInst();
    if (inst->getOpcode() != Op_Branch || !inst->getSrc(0)->getType()->isVTablePtr()) {
        return PROB_HEURISTIC_FAIL;
    }
    ComparisonModifier cmpOp = inst->getComparisonModifier();
    assert(cmpOp == Cmp_EQ || cmpOp == Cmp_NE_Un);
    return cmpOp == Cmp_EQ ? PROB_DEVIRT_GUARD_HEURISTIC : 1 - PROB_DEVIRT_GUARD_HEURISTIC;
}


void StaticProfiler::fixEdgeProbs(IRManager& irm) {
    assert(irm.getFlowGraph().hasEdgeProfile());

    double minProb = PROB_ALL_EXCEPTIONS;


    //fix edge-probs, try to reuse old probs as much as possible..    
    const Nodes& nodes = irm.getFlowGraph().getNodes();
    StaticProfilerContext* c = NULL;
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        double sumProb = 0;
        U_32 nNotEstimated = 0;
        const Edges& outEdges = node->getOutEdges();
        for(Edges::const_iterator eit = outEdges.begin(), eend = outEdges.end(); eit!=eend; ++eit) {
            Edge* e = *eit;
            double prob = e->getEdgeProb();
            sumProb+=prob;
            if (prob <= 0)  {
                nNotEstimated++;
            } 
        }
        if (nNotEstimated==0 && ABS(1 - sumProb) <= ACCEPTABLE_DOUBLE_PRECISION_LOSS) {
            continue; //ok, nothing to fix
        }   
        if (nNotEstimated == outEdges.size()) { //apply all active heuristics
            if (c == NULL) {
                c = new (irm.getMemoryManager()) StaticProfilerContext(irm);
            }
            c->node = node;
            c->edge1 = NULL;
            c->edge2 = NULL;
            estimateNode(c);
        } else { //assign min.possible prob for all not-estimated edges and scale probs to have sum == 1.
           double scale = 1;
            if (nNotEstimated == 0) { //do scaling only.
                scale = 1 / sumProb;        
            } else { //assign a min possible probs for all not estimated edges, calculate scale
                sumProb = 0;
                for(Edges::const_iterator eit = outEdges.begin(), eend = outEdges.end(); eit!=eend; ++eit) {
                    Edge* e = *eit;
                    double prob = e->getEdgeProb();
                    if (prob <= 0) {
                        prob = minProb;
                        e->setEdgeProb(prob);
                    }
                    sumProb+=prob;
                }
                scale = 1 / sumProb;
            }
            sumProb = 0;
            for(Edges::const_iterator eit = outEdges.begin(), eend = outEdges.end(); eit!=eend; ++eit) {
                Edge* e = *eit;
                double prob = e->getEdgeProb();
                prob = prob * scale;
                assert(prob > 0);
                e->setEdgeProb(prob);
#ifdef _DEBUG
                sumProb+=prob;
#endif          
            }
            assert(ABS(1-sumProb) < ACCEPTABLE_DOUBLE_PRECISION_LOSS);
        }
    }
    setHeatThreshold(irm);
}

static void splitString(const char* string, StringList& result, char separator, bool notEmptyTokensOnly) {
    std::string token;
    for (const char* suffix = string; *suffix!=0; ++suffix) {
        char c = *suffix;
        if (c == separator) {
            if (token.empty() && notEmptyTokensOnly) {
                continue;
            }
            result.push_back(token);
            token.clear();
        } else {
            token.push_back(c);
        }
    }
    if (!token.empty()) { //last value
        result.push_back(token);
    }
}

} //namespace
