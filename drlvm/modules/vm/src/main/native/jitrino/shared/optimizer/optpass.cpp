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

#include "Log.h"
#include "optpass.h"
#include "irmanager.h"
#include "Dominator.h"
#include "Loop.h"
#include "./ssa/SSA.h"
#include "EMInterface.h"
#include "optimizer.h"
#include "FlowGraph.h"
#include "StaticProfiler.h"
#include "deadcodeeliminator.h"

#ifdef _WIN32
  #define snprintf _snprintf
#endif


namespace Jitrino {

void 
OptPass::run() 
{
    IRManager& irm = *getCompilationContext()->getHIRManager();
    id = Log::getStageId();

    LogStream& irdump  = log(LogStream::IRDUMP);
    LogStream& dotdump = log(LogStream::DOTDUMP);

    if (irdump.isEnabled()) {
        Log::printStageBegin(irdump.out(), id, "HLO", getName(), getTagName());
        irdump << indent(irm) << "Opt:   Running " << getName() << ::std::endl;
        printHIR(irm, "before");
    }

    if (dotdump.isEnabled()) {
        printDotFile(irm, id, getTagName(), "before");
    }

        _run(irm);

    if (dotdump.isEnabled()) {
        printDotFile(irm, id, getTagName(), "after");
    }

    if (irdump.isEnabled()) {
        printHIR(irm, "after");
        Log::printStageEnd(irdump.out(), id, "HLO", getName(), getTagName());
    }
}

void
OptPass::computeDominators(IRManager& irm) {
    DominatorTree* dominatorTree = irm.getDominatorTree();
    if(dominatorTree != NULL && dominatorTree->isValid()) {
        // Already valid.
        return;
    }
    Log::out() << indent(irm) << "Opt:   Compute Dominators" << ::std::endl;
    static CountTime computeDominatorsTimer("opt::helper::computeDominators");
    AutoTimer tm(computeDominatorsTimer);
    DominatorBuilder db;
    dominatorTree = db.computeDominators(irm.getNestedMemoryManager(), &(irm.getFlowGraph()),false,true);
    irm.setDominatorTree(dominatorTree);
}


void
OptPass::computeLoops(IRManager& irm, bool normalize) {
    LoopTree* lt = irm.getLoopTree();
    if (lt!=NULL && lt->isValid() && (lt->isNormalized() || !normalize)) {
        return;
    }
    Log::out() << indent(irm) << "Opt:   Compute Loop Tree" << ::std::endl;
    static CountTime computeLoopsTimer("opt::helper::computeLoops");
    AutoTimer tm(computeLoopsTimer);
    LoopBuilder lb(irm.getNestedMemoryManager(), irm, *(irm.getDominatorTree()),false);
    lb.computeLoops(normalize);
    if (lb.needSsaFixup()) {
        fixupSsa(irm);
    }
}

void
OptPass::computeDominatorsAndLoops(IRManager& irm, bool normalizeLoops) {
    computeDominators(irm);
    computeLoops(irm, normalizeLoops);
    computeDominators(irm);
}

void
OptPass::dce(IRManager& irm) {
    DeadCodeEliminator dce(irm);
    dce.eliminateDeadCode(false);
}

void
OptPass::uce(IRManager& irm, bool fixup_ssa) {
    DeadCodeEliminator dce(irm);
    dce.eliminateUnreachableCode();

    if (irm.getInSsa() && fixup_ssa) {
        OptPass::fixupSsa(irm);
    }    
}

void
OptPass::fixupSsa(IRManager& irm) {
    static CountTime fixupSsaTimer("opt::helper::fixupSsa");
    static U_32 globalSsaFixupCounter = 0;

    if(!irm.isSsaUpdated()) {
        AutoTimer tm(fixupSsaTimer);
        Log::out() << indent(irm) << "Opt:   SSA Fixup" << ::std::endl;
    
        computeDominators(irm);
        DominatorTree* dominatorTree = irm.getDominatorTree();
        ControlFlowGraph& flowGraph = irm.getFlowGraph();
        MemoryManager& memoryManager = irm.getNestedMemoryManager();
        DomFrontier frontier(memoryManager,*dominatorTree,&flowGraph);
        SSABuilder ssaBuilder(irm.getOpndManager(),irm.getInstFactory(),frontier,&flowGraph, irm.getOptimizerFlags());
        bool better_ssa_fixup = irm.getOptimizerFlags().better_ssa_fixup;
 
        ssaBuilder.fixupSSA(irm.getMethodDesc(), better_ssa_fixup);
        globalSsaFixupCounter += 1;
        irm.setSsaUpdated();
    }

}

void
OptPass::splitCriticalEdges(IRManager& irm) {
    if(!irm.areCriticalEdgesSplit()) {
        Nodes newNodes(irm.getMemoryManager());
        irm.getFlowGraph().splitCriticalEdges(false, &newNodes);
        for (Nodes::const_iterator it = newNodes.begin(), end = newNodes.end(); it!=end; ++it) {
            Node* node = *it;
            if(node->isEmpty(false)) {
                assert(node->isBlockNode() && !node->isCatchBlock());
                node->appendInst(irm.getInstFactory().makeLabel());
            }
        }
        irm.setCriticalEdgesSplit();
    }
}


void
OptPass::initialize() {
}


bool 
OptPass::isProfileConsistent(IRManager& irm) {
    return irm.getFlowGraph().isEdgeProfileConsistent();
}

void 
OptPass::smoothProfile(IRManager& irm) { 
    if (isProfileConsistent(irm) == false) {
        StaticProfiler::fixEdgeProbs(irm);
        irm.getFlowGraph().smoothEdgeProfile();
    }
}

void
OptPass::printHIR(IRManager& irm) {
    FlowGraph::printHIR(Log::log(LogStream::IRDUMP).out(), irm.getFlowGraph(), irm.getMethodDesc());
}

void
OptPass::printHIR(IRManager& irm, const char* when) {
    std::ostream& out = Log::log(LogStream::IRDUMP).out();
    Log::printIRDumpBegin(out, id, getName(), when);
    printHIR(irm);
    Log::printIRDumpEnd(out, id, getName(), when);
}

void
OptPass::printDotFile(IRManager& irm, int id, const char* name,  const char* suffix) {
    char temp[128];
    snprintf(temp, sizeof(temp), "%.2i.%s.%s", id, name, suffix);
    printDotFile(irm, temp);
}

void
OptPass::printDotFile(IRManager& irm, const char* name) {
    ControlFlowGraph& flowGraph = irm.getFlowGraph();
    FlowGraph::printDotFile(flowGraph, irm.getMethodDesc(), name);
}

const char*
OptPass::indent(IRManager& irm) { 
    return irm.getParent() == NULL ? "" : "    "; 
}

} //namespace Jitrino 
