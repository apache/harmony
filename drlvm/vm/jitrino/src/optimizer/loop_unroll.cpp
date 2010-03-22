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
*/

#include "escapeanalyzer.h"
#include "Log.h"
#include "Inst.h"
#include "Dominator.h"
#include "irmanager.h"
#include "LoopTree.h"
#include "FlowGraph.h"
#include "ssa/SSA.h"
#include "Opnd.h"

namespace Jitrino {

//TODOs:
// 1) support parents loop unrolling -> easy
// 2) support complete loop elimination if limit is const -> easy  (or do not unroll at all)
// 3) support more branch types (Cmp_Eq, Cmp_NotEq, Cmp_Zero..)
// 4) support loop unrolling for counters with long, float and double types

class UnrollFlags {
public:
    UnrollFlags() 
        : smallLoopSize(0), smallLoopHotness(0), smallLoopUnrollCount(0),
        mediumLoopSize(0), mediumLoopHotness(0), mediumLoopUnrollCount(0),
        largeLoopSize(0), largeLoopHotness(0), largeLoopUnrollCount(0)
    {}

    int  smallLoopSize;
    int  smallLoopHotness;
    int  smallLoopUnrollCount;

    int  mediumLoopSize;
    int  mediumLoopHotness;
    int  mediumLoopUnrollCount;

    int  largeLoopSize;
    int  largeLoopHotness;
    int  largeLoopUnrollCount;

    bool unrollParentLoops;
};

class LoopUnrollAction : public Action {
public:
    void init();
    const UnrollFlags& getFlags() {return flags;}
protected:
    UnrollFlags flags;
};

DEFINE_SESSION_ACTION_WITH_ACTION(LoopUnrollPass, LoopUnrollAction, unroll, "Loop Unrolling")


//loop structure is
//loop {
// BodyA
// check
// BodyB
//}
// all paths from bodyA to bodyB must go through the 'check'
//
class LoopUnrollInfo {
public:
    LoopUnrollInfo() 
        : header(NULL), branchInst(NULL), branchTargetIsExit(false),
        branchLimitOpndPos(0), increment(0), unrollCount(0), doUnroll(false) 
    {}

    Node* header;
    BranchInst* branchInst;
    bool  branchTargetIsExit;
    int   branchLimitOpndPos;
    
    int increment; 
    int unrollCount;
    bool   doUnroll;

    
    Opnd* getLimitOpnd() const {return branchLimitOpndPos==0?branchInst->getSrc(0) : branchInst->getSrc(1);}
    Opnd* getIdxOpnd() const {return branchLimitOpndPos==0?branchInst->getSrc(1) : branchInst->getSrc(0);}
    
    void print(std::ostream& out) const {
        out<<"LoopHead=";FlowGraph::printLabel(out, header);
        out<<" inst=I"<<branchInst->getId();
        out<<" idx-opndId="<<getIdxOpnd()->getId();
        out<<" limit-opndId="<<getLimitOpnd()->getId();
        out<<" increment="<<increment;
        out<<" unroll_count="<<unrollCount;
        out<<" do_unroll="<<doUnroll;
    }
};

class OpndLoopInfo {
public:
    enum OpndType {DEF_OUT_OF_LOOP, LD_CONST, COUNTER, UNDEF};
    OpndLoopInfo() {type=DEF_OUT_OF_LOOP; val=0; phiSplit= false;}

    OpndType getType() const {return type;}
    void setType(OpndType newType) {assert(type!=newType);type = newType; val=0;}
    bool isCounter() const {return type == COUNTER;}
    bool isLDConst() const {return type == LD_CONST;}
    bool isDOL() const {return type == DEF_OUT_OF_LOOP;}
    bool isUndefined() const {return type == UNDEF;}
    int getConst() const {assert(getType()==LD_CONST); return val;}
    void setConst(I_32 v) {assert(getType()==LD_CONST); val=v;}
    int getIncrement() const {assert(getType()==COUNTER); return val;}
    void setIncrement(I_32 v) {assert(getType()==COUNTER); val=v;}
    bool isPhiSplit() const {return phiSplit;}
    void markPhiSplit() {assert(isCounter()); phiSplit = true;}
    
    
    void print(std::ostream& out) {
        if (type == DEF_OUT_OF_LOOP) out << "DOL"; 
        else if (type == LD_CONST)   out << "LDC:"<<getConst();
        else if (type == COUNTER)    out << "CNT:"<<getIncrement() << (phiSplit?" splt":"");
        else                         out << "UNDEF";
    }
private:
    OpndType type;
    int      val;
    bool     phiSplit; //only 1 phi split with DOL is allowed for counters.
};

typedef StlVector<Inst*> InstStack;
typedef StlVector<LoopUnrollInfo*> UnrollInfos;

static void log_ident(size_t n)  {for (size_t i=0;i<n;++i) Log::out()<<"  "; }
static int signof(int v) { return (v == 0) ? 0 : ( v < 0 ? -1 : 1); }



static void findLoopsToUnroll(MemoryManager& mm, IRManager& irm, UnrollInfos& result, const UnrollFlags& flags);
static void doUnroll(MemoryManager& mm, IRManager& irm, const LoopUnrollInfo* info, const UnrollFlags& flags);
static void calculateReachableNodesInLoop(LoopNode* loop, Node* node, Node* stopNode, BitSet& flags);


void LoopUnrollAction::init() {
    flags.smallLoopSize = getIntArg("small_loop_max_size", 5);
    flags.smallLoopHotness = getIntArg("small_loop_hotness_percent", 300);
    flags.smallLoopUnrollCount = getIntArg("small_loop_unroll_count", 8);

    flags.mediumLoopSize = getIntArg("medium_loop_max_size", 10);
    flags.mediumLoopHotness = getIntArg("medium_loop_hotness_percent", 300);
    flags.mediumLoopUnrollCount = getIntArg("medium_loop_unroll_count", 4);

    flags.largeLoopSize = getIntArg("large_loop_max_size", 20);
    flags.largeLoopHotness = getIntArg("large_loop_hotness_percent", 300);
    flags.largeLoopUnrollCount = getIntArg("large_loop_unroll_count", 2);

//TODO: support unrolling of parent loops
    flags.unrollParentLoops = false;//getBoolArg("unroll_parent_loops", false);
}

void LoopUnrollPass::_run(IRManager& irm) {
    const UnrollFlags& flags = ((LoopUnrollAction*)getAction())->getFlags();
    
    OptPass::computeDominatorsAndLoops(irm);
    ControlFlowGraph& cfg = irm.getFlowGraph();
    LoopTree* lt = cfg.getLoopTree();
    if (!lt->hasLoops()) {
        return;
    }
    
    MemoryManager mm("loopUnrollMM");
    UnrollInfos loopsToUnroll(mm);
    findLoopsToUnroll(mm, irm, loopsToUnroll, flags);
    if (loopsToUnroll.empty()) {
        if (Log::isEnabled()) Log::out() << "No candidates found to unroll"<<std::endl;
        return;
    }
    if (Log::isEnabled()) {
        Log::out()<<"Loops to unroll before filtering:"<<std::endl;
        for (UnrollInfos::const_iterator it = loopsToUnroll.begin(), end = loopsToUnroll.end();it!=end; ++it) {
            const LoopUnrollInfo* info = *it;
            info->print(Log::out()); Log::out()<<std::endl;
        }
    }
    bool hasProfile =  cfg.hasEdgeProfile();
    //filter out that can't be unrolled, calculate BodyA and BodyB
    BitSet bodyANodes(mm, cfg.getMaxNodeId()), bodyBNodes(mm, cfg.getMaxNodeId());
    for (UnrollInfos::iterator it = loopsToUnroll.begin(), end = loopsToUnroll.end();it!=end; ++it) {
        LoopUnrollInfo* info = *it;
        if (info == NULL) {
            continue;
        }
        if (!info->doUnroll) {
            *it=NULL;
            continue;
        }
        Node* header=info->header;
        LoopNode* loopHeader = lt->getLoopNode(header, false);
        assert(loopHeader->getHeader() == header);

        Node* checkNode = info->branchInst->getNode();
        bodyANodes.clear();
        bodyBNodes.clear();
        calculateReachableNodesInLoop(loopHeader, loopHeader->getHeader(), checkNode, bodyANodes);
        calculateReachableNodesInLoop(loopHeader, checkNode, NULL, bodyBNodes);
        bodyANodes.intersectWith(bodyBNodes);
        bool checkNodeIsJunctionPoint = bodyANodes.isEmpty();
        if (!checkNodeIsJunctionPoint) {
            if (Log::isEnabled()) {
                Log::out()<<"Check node is not a junction point -> removing from the list: branch inst id=I"<<info->branchInst->getId()<<std::endl;
            }
            *it=NULL;
            continue;
        }
        //check if branch semantic is OK
        ComparisonModifier cmpMod = info->branchInst->getModifier().getComparisonModifier();
        if (cmpMod!=Cmp_GT && cmpMod!=Cmp_GTE && cmpMod!=Cmp_GT_Un && cmpMod!=Cmp_GTE_Un) {
            if (Log::isEnabled()) {
                Log::out()<<"Branch is not a range comparison -> removing from the list: branch inst id=I"<<info->branchInst->getId()<<std::endl;
            }
            *it=NULL;
            continue;
        }

        //check config settings
        bool failed = false;
        int nodesInLoop = (int)loopHeader->getNodesInLoop().size();
        const char* reason = "unknown";
        if (nodesInLoop > flags.largeLoopSize) {
            reason = "loop is too large";
            failed = true;
        } else if (hasProfile) {
            int headHotness = (int)(header->getExecCount()*100.0  / cfg.getEntryNode()->getExecCount());
            int minHeaderHotness= nodesInLoop <= flags.smallLoopSize ? flags.smallLoopHotness :
                nodesInLoop <= flags.mediumLoopSize ? flags.mediumLoopHotness : flags.largeLoopHotness;
            info->unrollCount = nodesInLoop <= flags.smallLoopSize ? flags.smallLoopUnrollCount :
                nodesInLoop <= flags.mediumLoopSize? flags.mediumLoopUnrollCount: flags.largeLoopUnrollCount;
            failed = headHotness < minHeaderHotness || info->unrollCount < 1;
            if (failed) {
                reason = "loop is too cold";
            }
        }
        if (failed) {
            if (Log::isEnabled()) {
                Log::out()<<"Loop does not match unroll configuration ("<<reason<<") -> removing from the list: branch inst id=I"<<info->branchInst->getId()<<std::endl;
            }
            *it=NULL;
        }
    }    
    //filter out loops with multiple exits 
    for (UnrollInfos::iterator it1 = loopsToUnroll.begin(), end = loopsToUnroll.end();it1!=end; ++it1) {
        const LoopUnrollInfo* info1 = *it1;
        if (info1== NULL) {
            continue;
        }
        Node* header=info1->header;
        for (UnrollInfos::iterator it2 = it1+1; it2!=end; ++it2) {
            const LoopUnrollInfo* info2 = *it2;
            if (info2!=NULL && header==info2->header) {
                if (Log::isEnabled()) {
                    Log::out() << "Found multiple exits:"; FlowGraph::printLabel(Log::out(), header);Log::out()<<std::endl;
                }
                if (hasProfile)  {
                    Node* check1 = info1->branchInst->getNode();
                    Node* check2 = info2->branchInst->getNode();
                    if (check1->getExecCount() > check2->getExecCount()) {
                        *it2 = NULL;
                    } else {
                        *it1 = NULL;
                    }
                } else { // random selection
                    *it2=NULL;
                }
            }
        }
    }    
    loopsToUnroll.erase(std::remove(loopsToUnroll.begin(), loopsToUnroll.end(), (LoopUnrollInfo*)NULL), loopsToUnroll.end());
    if (loopsToUnroll.empty()) {
        if (Log::isEnabled()) Log::out() << "--------No candidates to unroll left after filtering"<<std::endl;
        return;
    }
    
    //dessa CFG before unrolling -> need to duplicate regions and we can do it on dessa form only today
    {
        SSABuilder::deconvertSSA(&cfg, irm.getOpndManager());
        irm.setInSsa(false);
    }

    if (Log::isEnabled()) {
        Log::out()<<"--------Loops to unroll after filtering : n="<<loopsToUnroll.size()<<std::endl;
            for (UnrollInfos::const_iterator it = loopsToUnroll.begin(), end = loopsToUnroll.end();it!=end; ++it) {
                const LoopUnrollInfo* info = *it;
                info->print(Log::out()); Log::out()<<std::endl;
            }
    }

    for (UnrollInfos::const_iterator it = loopsToUnroll.begin(), end = loopsToUnroll.end();it!=end; ++it) {
        const LoopUnrollInfo* info = *it;
        doUnroll(mm, irm, info, flags);
    }
};

static void calculateReachableNodesInLoop(LoopNode* loop, Node* node, Node* stopNode, BitSet& flags) {
    int id = node->getId();
    if (!loop->inLoop(node) || node == stopNode || flags.getBit(id)) {
        return;
    }
    flags.setBit(id, true);
    const Edges& edges = node->getOutEdges();
    for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
        Edge* e = *ite;
        Node* nextNode = e->getTargetNode();
        if (nextNode != loop->getHeader()) {
            calculateReachableNodesInLoop(loop, nextNode, stopNode, flags);
        }
    }
}


static OpndLoopInfo processOpnd(LoopNode* loopHead, LoopTree* lt, InstStack& defStack, Opnd* opnd) {
    OpndLoopInfo result;
    Inst* defInst = opnd->getInst();
    if (Log::isEnabled()) {
        log_ident(defStack.size()); defInst->print(Log::out()); Log::out()<<"]"<<std::endl;
    }
    if (std::find(defStack.begin(), defStack.end(), defInst)!=defStack.end()) {
        result.setType(OpndLoopInfo::COUNTER);
        result.setIncrement(0);
        if (Log::isEnabled()) {
            log_ident(defStack.size()); 
            Log::out()<<"Found duplicate in def stack -> stopping recursion. ";result.print(Log::out()); Log::out()<<std::endl;
        }
        return result;
    }
    Node* defNode = defInst->getNode();
    Opcode opcode = defInst->getOpcode();

    if (opcode == Op_LdConstant) {
        result.setType(OpndLoopInfo::LD_CONST);
        result.setConst(defInst->asConstInst()->getValue().i4);
        if (Log::isEnabled()) {
            log_ident(defStack.size()); 
            Log::out()<<"assigning to const -> stopping recursion. ";result.print(Log::out());Log::out()<<std::endl;
        }
        return result;
    }
    if (!loopHead->inLoop(defNode)) {
        if (Log::isEnabled()) {
            log_ident(defStack.size());
            Log::out()<<"Inst out of the loop -> stopping recursion. ";result.print(Log::out()); Log::out()<<std::endl;
        }
        return result;
    }

    defStack.push_back(defInst);
    if (opcode == Op_Phi) {
        OpndLoopInfo info1 = processOpnd(loopHead, lt, defStack, defInst->getSrc(0));
        OpndLoopInfo info2 = processOpnd(loopHead, lt, defStack, defInst->getSrc(1));
        if (Log::isEnabled()) {
            log_ident(defStack.size());
            Log::out()<<"PHI(";info1.print(Log::out());Log::out()<<",";info2.print(Log::out());Log::out()<<")"<<std::endl;
        }
        if ( ((info1.isCounter() && !info1.isPhiSplit()) && (info2.isDOL() || info2.isLDConst()))
             || ((info2.isCounter() && !info2.isPhiSplit()) && (info1.isDOL() || info1.isLDConst())) )
        {
            result.setType(OpndLoopInfo::COUNTER);
            result.setIncrement(info1.isCounter() ? info1.getIncrement() : info2.getIncrement());
            result.markPhiSplit();
        } else {
            result.setType(OpndLoopInfo::UNDEF);
        }
    } else if (opcode == Op_Add || opcode == Op_Sub) { //todo: LADD 
        Opnd *op1 = defInst->getSrc(0);
        Opnd *op2 = defInst->getSrc(1);
        OpndLoopInfo info1 = processOpnd(loopHead, lt, defStack, op1);
        OpndLoopInfo info2 = processOpnd(loopHead, lt, defStack, op2);
        if ((info1.isLDConst() || info1.isDOL()) && (info2.isLDConst() || info2.isDOL())) {
            if (info1.isLDConst() && info2.isLDConst() && info1.getConst() == info2.getConst()) {
                result.setType(OpndLoopInfo::LD_CONST);
                result.setConst(info1.getConst());
            } else {
                //result is DOL (default type)
            }
        } else if ((info1.isCounter() && info2.isLDConst()) || (info2.isCounter() && info1.isLDConst())) {
            int increment = info1.isCounter()? info1.getIncrement(): info2.getIncrement();
            int diff = info1.isLDConst()? info1.getConst(): info2.getConst(); 

            //we use SSA form to analyze how opnd changes in loop and we do not analyze actual control flow, 
            // so we can unroll loops with monotonically changing 'counters' only.
            //Example: when 'counter' changes not monotonically and we can't unroll:
            //idx=0; loop {idx+=100; if(idx>=100) break; idx-=99;} ->'increment'=1 but not monotonicaly.
            bool monotonousFlag = increment == 0 || diff == 0 
                || (opcode == Op_Add && signof(diff) == signof(increment)) 
                || (opcode == Op_Sub && signof(diff) != signof(increment));
            if (monotonousFlag) {
                result.setType(OpndLoopInfo::COUNTER);
                if ((info1.isCounter() && info1.isPhiSplit()) || (info2.isCounter() && info2.isPhiSplit())) {
                    result.markPhiSplit();
                }
                //TO IMPROVE: for loops like: for (; length-1>=0;length--){...}
                //we have 2 SUBs by -1 => "-2", but real counter is changed by "-1".
                //Loop unroll will use "-2". It's ok, because this value is used in a guard inst
                //and ABS(increment_in_unroll) >= ABS(real_increment). This work only for monotonous loops.
                //To make increment_in_unroll == real_increment we must track modifications (SUB,ADD) that affects vars only.
                if (opcode == Op_Add) {
                    result.setIncrement(increment + diff);
                } else {
                    result.setIncrement(increment - diff);
                }
            } else {
                result.setType(OpndLoopInfo::UNDEF);
            }
        } else {
            result.setType(OpndLoopInfo::UNDEF);
        }
    } else if (opcode == Op_StVar  || opcode == Op_LdVar) {
        Opnd* newOpnd = defInst->getSrc(0);
        result  = processOpnd(loopHead, lt, defStack, newOpnd);
    } else if (opcode == Op_TauArrayLen) {
        Opnd* arrayOpnd  = defInst->getSrc(0);
        result  = processOpnd(loopHead, lt, defStack, arrayOpnd);
    } else { //unsupported op
        result.setType(OpndLoopInfo::UNDEF);
        if (Log::isEnabled()) {
            log_ident(defStack.size()); Log::out()<<"unknown op -> stopping recursion. ";
        }
    }
    defStack.pop_back();
    if (Log::isEnabled()) {
        log_ident(defStack.size());
        result.print(Log::out());Log::out()<<std::endl;
    }
    return result;
}

static LoopUnrollInfo* prepareUnrollInfo(MemoryManager& mm, LoopTree* lt, BranchInst* branchInst) {

    if (Log::isEnabled()) {
        Log::out()<<"==Checking loop exit:"; branchInst->print(Log::out()); Log::out()<<std::endl;
    }

    //traverse loop and track all modifications
    Node* node = branchInst->getNode();
    LoopNode* loopHeader = lt->getLoopNode(node, false);
    Opnd* opnd1 = branchInst->getSrc(0);
    Opnd* opnd2 = branchInst->getNumSrcOperands()==1?NULL:branchInst->getSrc(1);
    
    if (opnd2==NULL) {
        assert(branchInst->getComparisonModifier() == Cmp_Zero || branchInst->getComparisonModifier() == Cmp_NonZero);
        assert(opnd1->getType()->isObject());
        if (Log::isEnabled()) {
            Log::out()<<"----Unsupported comparison modifier."<<std::endl;
        }
        return NULL;
    }
    
    if (!opnd1->getType()->isInteger() || opnd1->getType()->isInt8()
        || !opnd2->getType()->isInteger() || opnd2->getType()->isInt8()) 
    {
        if (Log::isEnabled()) {
            Log::out()<<"----Unsupported opnd types."<<std::endl;
        }
        return NULL;        //IMPROVE: longs and floating types are not supported
    }

    InstStack defStack(mm);

    Log::out()<<"----Analyzing opnd1 id="<<opnd1->getId()<<std::endl;
    OpndLoopInfo opndInfo1 = processOpnd(loopHeader, lt, defStack, opnd1);
    assert(defStack.empty());
    
    Log::out()<<"----Analyzing opnd2 id="<<opnd2->getId()<<std::endl;
    OpndLoopInfo opndInfo2 = processOpnd(loopHeader, lt, defStack, opnd2);
    assert(defStack.empty());

    if(Log::isEnabled()) {
        Log::out()<<"----Result: opndId1="<<opnd1->getId()<<" type=";opndInfo1.print(Log::out());
        Log::out()<<", opndId2="<<opnd2->getId()<<" type=";opndInfo2.print(Log::out());Log::out()<<std::endl;
    }

    //default values -> this item will not be unrolled unless all constraints are OK
    LoopUnrollInfo* info = new (mm) LoopUnrollInfo();
    info->header = loopHeader->getHeader();
    info->branchInst = branchInst;
    info->branchTargetIsExit = !loopHeader->inLoop(branchInst->getTargetLabel()->getNode());
    info->doUnroll = false;

    if (opndInfo1.isCounter() && (opndInfo2.isDOL() || opndInfo2.isLDConst())) {
        info->doUnroll = true;
        info->branchLimitOpndPos=1;
        info->increment = opndInfo1.getIncrement();
    } else  if (opndInfo2.isCounter() && (opndInfo1.isDOL() || opndInfo1.isLDConst())) {
        info->doUnroll = true;
        info->branchLimitOpndPos=0;
        info->increment = opndInfo2.getIncrement();
    } 
    return info;
}

void findLoopsToUnroll(MemoryManager& tmpMM, IRManager& irm, UnrollInfos& result, const UnrollFlags& flags) {

    ControlFlowGraph& fg = irm.getFlowGraph();
    LoopTree* lt = fg.getLoopTree();

    //find all loop exits
    Edges loopExits(tmpMM);
    const Nodes& nodes = fg.getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
       Node* node = *it;
        LoopNode* loopNode = lt->getLoopNode(node, false);
        if (loopNode == NULL) {
            continue; //node not in a loop
        }
        if (!flags.unrollParentLoops && loopNode->getChild()!=NULL) {
            continue; //skip parent loops
        }
        const Edges& edges = node->getOutEdges();
        for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
           Edge* edge = *ite;
            if (lt->isLoopExit(edge)) {
                loopExits.push_back(edge);
            }
        }
    }
   
    //filter out all edges except branches
    for (Edges::iterator ite = loopExits.begin(), ende = loopExits.end(); ite!=ende; ++ite) {
       Edge* edge = *ite;
        if (edge->isDispatchEdge() || edge->isUnconditionalEdge() || edge->isCatchEdge()) {
            *ite = NULL;
            continue;
        }
        Inst* lastInst = (Inst*)edge->getSourceNode()->getLastInst();
        if (lastInst->isSwitch()) {
            *ite = NULL;
            continue;
        }
        assert(lastInst->isBranch());
        assert(edge->isFalseEdge() || edge->isTrueEdge());
    }
    loopExits.erase(std::remove(loopExits.begin(), loopExits.end(), (Edge*)NULL), loopExits.end());

    // analyze every loop exit and prepare unroll info
    for (Edges::const_iterator ite = loopExits.begin(), ende = loopExits.end(); ite!=ende; ++ite) {
        Edge* edge = *ite;
        Node* sourceNode = edge->getSourceNode();
        Inst* lastInst = (Inst*)sourceNode->getLastInst();
        assert(lastInst->isBranch());
        LoopUnrollInfo* info = prepareUnrollInfo(tmpMM, lt, lastInst->asBranchInst());
        if (info == NULL) {
            continue;
        }
        if (Log::isEnabled()) {
            info->print(Log::out());
            Log::out()<<std::endl;
        }
        result.push_back(info);
    }
}

static void doUnroll(MemoryManager& mm, IRManager& irm, const LoopUnrollInfo* info, const UnrollFlags& flags) {
    //unroll algorithm does the following
    //before:
    // loopOrig {
    //    bodyA
    //    check(idxOpnd,limitOpnd)
    //    bodyB
    // }
    //after:
    // unrolledIncOpnd = unrollCount * idx->increment
    // unrolledLimitOpnd = limitOpnd-unrolledIncOpnd;
    // bodyA 
    // loopUnrolled {
    //     check(idxOpnd,unrolledLimitOpnd)
    //     bodyB
    //     bodyA
    //     bodyB
    //     ...
    //     bodyA
    // }
    // loopEpilogue {
    //    check(idxOpnd,limitOpnd)
    //    bodyB
    //    bodyA
    // }
    //
    //where:
    // bodyA - all nodes of the same loop accessible from checkNode via incoming edges
    // bodyB - all nodes except bodyA and checkNode

    ControlFlowGraph& cfg = irm.getFlowGraph();
    LoopTree* lt = cfg.getLoopTree();
    InstFactory& instFactory = irm.getInstFactory();
    OpndManager& opndManager = irm.getOpndManager();
    Type* opType = info->getLimitOpnd()->getType();
    
 //   printf("UNROLL\n");

    //STEP 0: cache all data needed
    assert(info->unrollCount >= 1);
    Node* origHeader = info->header;
    assert(origHeader->getInDegree() == 2); //loop is normalized

    OptPass::computeLoops(irm);//recompute loop info if needed
    LoopNode* loopNode = lt->getLoopNode(origHeader, false); 
    
    Edge* entryEdge = origHeader->getInEdges().front();
    if (lt->isBackEdge(entryEdge)) {
        entryEdge = origHeader->getInEdges().back();
    }
    Node* origCheckNode = info->branchInst->getNode();
    Edge* origLoopExitEdge = info->branchTargetIsExit ? origCheckNode->getTrueEdge() : origCheckNode->getFalseEdge();
    
    U_32 maxNodeId = cfg.getMaxNodeId()+1; //+1 for a split check node
    StlBitVector nodesInLoop(mm, maxNodeId);
    {
        const Nodes& loopNodes = loopNode->getNodesInLoop();
        for (Nodes::const_iterator it = loopNodes.begin(), end = loopNodes.end(); it!=end; ++it) {
            Node* node = *it;
            nodesInLoop.setBit(node->getId());
        }
    }
    
    
    //STEP 1: calculate bodyA nodes
    BitSet aFlags(mm, maxNodeId);
    calculateReachableNodesInLoop(loopNode, origHeader, origCheckNode, aFlags);
    StlBitVector bodyANodes(mm, maxNodeId);
    for (U_32 i=0;i<maxNodeId;i++) bodyANodes.setBit(i, aFlags.getBit(i));
    
    //STEP 2: make checkNode a separate node, prepare loop region
    bodyANodes.setBit(origCheckNode->getId(), true);
    Node* checkNode = cfg.splitNodeAtInstruction(info->branchInst->prev(), true, false, instFactory.makeLabel());
    nodesInLoop.setBit(checkNode->getId(), true);
    Node* preCheckNode = origCheckNode;
    bodyANodes.setBit(preCheckNode->getId(), true);
    
    //STEP 3: rotate original loop
    // before: {bodyA1, check , bodyB}
    // after:  bodyA2 {check, bodyB, bodyA1}
    Edge* bodyA2ToCheckEdge = NULL;
    Opnd* limitOpndInBodyA2 = NULL;
    {
        //WARN: info->limitOpnd and info->indexOpnd can be replaced after code duplication if promoted to vars
        Opnd* limitOpndBefore = info->getLimitOpnd();

        assert(preCheckNode->getOutDegree()==1 && preCheckNode->getUnconditionalEdgeTarget() == checkNode);
        DefUseBuilder defUses(mm);
        defUses.initialize(cfg);
        OpndRenameTable opndRenameTable(mm, maxNodeId); //todo: maxNodeId is overkill estimate here
        NodeRenameTable nodeRenameTable(mm, maxNodeId);
        Node* bodyA2 = FlowGraph::duplicateRegion(irm, origHeader, bodyANodes, defUses, nodeRenameTable, opndRenameTable);
        cfg.replaceEdgeTarget(entryEdge, bodyA2, true);
        
        // while duplicating a region new nodes could be created and 'nodesInRegion' bitvector param is updated. 
        // BodyA is part of the loop -> if new nodes were created in the loop we must track them.
        nodesInLoop.resize(bodyANodes.size());
        for (U_32 i=0;i<bodyANodes.size();i++) nodesInLoop.setBit(i, bodyANodes.getBit(i) || nodesInLoop.getBit(i));

        Node* bodyA2PreCheckNode = nodeRenameTable.getMapping(preCheckNode);
        assert(bodyA2PreCheckNode->getOutDegree()==1 && bodyA2PreCheckNode->getUnconditionalEdgeTarget() == checkNode);
        bodyA2ToCheckEdge = bodyA2PreCheckNode->getUnconditionalEdge();
        limitOpndInBodyA2 = limitOpndBefore;
        if (nodeRenameTable.getMapping(limitOpndBefore->getInst()->getNode())!=NULL) {
            limitOpndInBodyA2 = opndRenameTable.getMapping(limitOpndBefore);
        }
        assert(limitOpndInBodyA2!=NULL);
    }

    //STEP 4: prepare epilogue loop: {check, bodyB, bodyA}
    Node* epilogueLoopHead = NULL;
    {
        DefUseBuilder defUses(mm);
        defUses.initialize(cfg);
        OpndRenameTable opndRenameTable(mm, maxNodeId); //todo: maxNodeId is overkill estimate here
        NodeRenameTable nodeRenameTable(mm, maxNodeId);
        epilogueLoopHead = FlowGraph::duplicateRegion(irm, checkNode, nodesInLoop, defUses, nodeRenameTable, opndRenameTable);
        cfg.replaceEdgeTarget(origLoopExitEdge, epilogueLoopHead, true);
    }

    //STEP 5: prepare unrolledLimitOpnd and replace it in original loop's check
    {
        Node* unrolledPreheader = cfg.spliceBlockOnEdge(bodyA2ToCheckEdge, instFactory.makeLabel());
        Opnd* unrolledIncOpnd = opndManager.createSsaTmpOpnd(opType);
        unrolledPreheader->appendInst(instFactory.makeLdConst(unrolledIncOpnd, info->increment * info->unrollCount));
        Opnd* unrolledLimitOpnd = opndManager.createSsaTmpOpnd(opType);
        Modifier mod = Modifier(SignedOp)|Modifier(Strict_No)|Modifier(Overflow_None)|Modifier(Exception_Never);
        unrolledPreheader->appendInst(instFactory.makeSub(mod, unrolledLimitOpnd, limitOpndInBodyA2, unrolledIncOpnd));
        info->branchInst->setSrc(info->branchLimitOpndPos, unrolledLimitOpnd);
    }

    DefUseBuilder defUses(mm);
    defUses.initialize(cfg);
    //STEP 6: unroll original loop and remove all checks in duplicated bodies
    {
        Edge* backedge = preCheckNode->getUnconditionalEdge();
        for (int i=1;i<info->unrollCount;i++) {
            OpndRenameTable opndRenameTable(mm, maxNodeId);
            NodeRenameTable nodeRenameTable(mm, maxNodeId);

            Node* unrolledRegionHeader = FlowGraph::duplicateRegion(irm, checkNode, nodesInLoop, defUses, nodeRenameTable, opndRenameTable);
            cfg.replaceEdgeTarget(backedge, unrolledRegionHeader, true); 

            Node* newTail = nodeRenameTable.getMapping(preCheckNode);
            assert(newTail->getOutDegree()==1 );
            backedge = newTail->getUnconditionalEdge();
            cfg.replaceEdgeTarget(backedge, checkNode, true);
            
            //remove check from duplicated code
            Node* duplicateCheckNode = nodeRenameTable.getMapping(checkNode);
            assert(duplicateCheckNode->getOutDegree()==2);
            Edge* exitEdge = info->branchTargetIsExit ? duplicateCheckNode->getTrueEdge() : duplicateCheckNode->getFalseEdge();
            duplicateCheckNode->getLastInst()->unlink();
            cfg.removeEdge(exitEdge);
        }
    }
    
    //STEP 7: make old loop colder
    if (cfg.hasEdgeProfile()) {
        Edge* epilogueExit = info->branchTargetIsExit ? epilogueLoopHead->getTrueEdge() : epilogueLoopHead->getFalseEdge();
        epilogueExit->setEdgeProb(epilogueExit->getEdgeProb() * 5);
    }
}   


}//namespace


