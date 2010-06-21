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
 * @author Intel, Konstantin M. Anisimov, Igor V. Chebykin
 *
 */

#include "IpfLiveAnalyzer.h"
#include "IpfIrPrinter.h"
#include "IpfOpndManager.h"

namespace Jitrino {
namespace IPF {

//========================================================================================//
// LiveAnalyzer
//========================================================================================//

LiveAnalyzer::LiveAnalyzer(Cfg &cfg) : 
    cfg(cfg),
    mm(cfg.getMM()),
    workSet(mm),
    liveManager(cfg),
    liveSet(liveManager.getLiveSet()),
    dceFlag(false) {
}

//----------------------------------------------------------------------------------------//

void LiveAnalyzer::analyze() {
    
    NodeVector &nodes = cfg.search(SEARCH_POST_ORDER);  // get postordered node list
    for (int16 i=nodes.size()-1; i>=0; i--) {           // iterate nodes
        Node *node = nodes[i];                          //
        node->setVisited(false);                        // mark node unvisited
        if (dceFlag == false) node->clearLiveSet();     // clear node's live set
        workSet.push_back(node);                        // put node in workSet (reverse order)
    }

    while (workSet.size() > 0) {                        // while there is node to analyze
        Node *node = workSet.back();                    // get last workSet node
        node->setVisited(true);                         // mark the node visited
        workSet.pop_back();                             // remove the node from workSet
        if (analyzeNode(node) == false) {               // node's live set has changed - live sets of predecessor need evaluation
            pushPreds(node);                            // put predecessors in workSet
        }
    }
    
    if (VERIFY_ON) verify();
}

//----------------------------------------------------------------------------------------//

void LiveAnalyzer::dce() {

    IPF_LOG << endl << "  Dead code:" << endl;
    dceFlag = true;
    analyze();
    dceFlag = false;
}
    
//----------------------------------------------------------------------------------------//
// check if node's liveSets coincide with calculated ones
// check if liveSet of Enter node is empty (except inArgs including implicit arg b0)

void LiveAnalyzer::verify() {
    
    NodeVector &nodes = cfg.search(SEARCH_POST_ORDER);              // get nodes postorder
    
    for (uint16 i=0; i<nodes.size(); i++) {                         // iterate it
        if (analyzeNode(nodes[i]) == false) {                       // if node's liveSet does not coincide with calculated one
            IPF_ERR << " node" << nodes[i]->getId() << endl;        // throw error message
        }
    }
    
    RegOpndSet    enterLiveSet = cfg.getEnterNode()->getLiveSet();  // get liveSet of Enter node
    RegOpndVector &inArgs      = cfg.getOpndManager()->getInArgs(); // get inArgs
    for (uint16 i=0; i<inArgs.size(); i++) {                        //
        enterLiveSet.erase(inArgs[i]);                              // remove inArgs from enterLiveSet
    }                                                               // 
    
    enterLiveSet.erase(cfg.getOpndManager()->getB0());              // remove b0 from enterLiveSet
    if (enterLiveSet.size() == 0) return;                           // if enterLiveSet is empty - Ok

    IPF_ERR << " size " << enterLiveSet.size() << endl;
    for (RegOpndSet::iterator it=enterLiveSet.begin(); it!=enterLiveSet.end(); it++) {
        IPF_ERR << " alive opnd on method enter " << IrPrinter::toString(*it)
            << " " << IrPrinter::toString(cfg.getMethodDesc()) << endl;
    }
}

//----------------------------------------------------------------------------------------//
// recalculate live set for the node
// if it has not changed - return true, else - return false

bool LiveAnalyzer::analyzeNode(Node *node) {

    liveManager.init(node);                      // init LiveManager with current node
    if (LOG_ON) {
        IPF_LOG << endl << "  Qp Tree for node" << node->getId() << endl;
        liveManager.printQpTree();
    }
    
    RegOpndSet &oldLiveSet = node->getLiveSet(); // get current node live set (on node enter)
    if (node->getNodeKind() != NODE_BB) {        // liveSet is not going to change 
        if (oldLiveSet == liveSet) return true;  // if live set has not changed - nothing to do
        node->setLiveSet(liveSet);               // set liveSet for the current node
        return false;                            // live set of the node has changed
    }

    InstVector &insts = ((BbNode *)node)->getInsts();
    for(int16 i=insts.size()-1; i>=0; i--) {     // iterate through node insts postorder
        Inst *inst = insts[i];                   //
        if (dceFlag && isInstDead(inst)) {       // if dce is activated and inst is dead
            IPF_LOG << "    node" << setw(3) << left << node->getId();
            IPF_LOG << IrPrinter::toString(inst) << endl;
            insts.erase(insts.begin()+i);        // remove inst from InstVector of current node
            liveManager.def(inst);               // change liveSet according with inst def
        } else {                                 //
            liveManager.def(inst);               // change liveSet according with inst def
            liveManager.use(inst);               // change liveSet according with inst use
        }
    }
    
    if (oldLiveSet == liveSet) return true;      // if live set has not changed - nothing to do
    node->setLiveSet(liveSet);                   // set liveSet for the current node
    return false;                                // live set of the node has changed
}

//----------------------------------------------------------------------------------------//
// push all predecessors of the node in workSet (ignore preds which are in workSet already)

void LiveAnalyzer::pushPreds(Node *node) {

    EdgeVector &edges = node->getInEdges();       // get in edges
    for (uint16 i=0; i<edges.size(); i++) {       // iterate
        Node *pred = edges[i]->getSource();       // get predecessor
        if (pred->isVisited() == false) continue; // if predecessor is in workSet - ignore
        workSet.push_back(pred);                  // push predecessor in workSet
        pred->setVisited(false);                  // mark it unvisited
    }
}

//----------------------------------------------------------------------------------------//
// Check if instruction can be removed from inst vector. 
// Do not remove instruction having "side effects" (like "call")

bool LiveAnalyzer::isInstDead(Inst *inst) {
    
    if (inst->isCall())                    return false; // "call" inst is never dead
    if (inst->getInstCode() == INST_ALLOC) return false; // "alloc" inst is never dead

    uint16 numDst = inst->getNumDst();                   // get num of dst opnds
    if (numDst == 0) return false;                       // if there is no dst opnds - ignore

    OpndVector &opnds = inst->getOpnds();                // get inst opnds
    RegOpnd    *qp    = (RegOpnd *)opnds[0];             // get qp of the inst
    QpMask     mask   = liveManager.getLiveMask(qp);     // get mask for this qp space
    for (uint16 i=1; i<numDst+1; i++) {                  // iterate dst opnds
        RegOpnd *dst = (RegOpnd *)opnds[i];              // 
        if (dst->isAlive(mask)) return false;            // if dst is alive - inst is alive
    }
    return true;                                         // there is no alive dst opnd - inst is dead
}

} // IPF
} // Jitrino
