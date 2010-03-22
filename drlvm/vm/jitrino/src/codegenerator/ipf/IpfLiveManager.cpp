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
// QpNode
//========================================================================================//

QpNode::QpNode(QpNode *predNode, QpMask nodeMask) : 
    predNode(predNode), 
    compNode(NULL), 
    nodeMask(nodeMask), 
    compMask(0),
    liveMask(0) {

    QpNode *qpNode = predNode;
    while (qpNode != NULL) {            // iterate through all predecessors of the node
        qpNode->orNodeMask(nodeMask);   // add bits of this nodeMask in predecessor's nodeMasks
        qpNode = qpNode->getPredNode(); // get next predecessor
    }
}

//----------------------------------------------------------------------------------------//
// make mask of all predicate spaces interfering with current one. These are all spaces
// except complementing ones

void QpNode::initLiveMask() {
    
    QpNode *qpNode = this;
    liveMask = 0;
    while (qpNode != NULL) {
        QpMask mask = qpNode->getCompMask();        // get nodeMask of complemening to current space
        if (mask != MAX_QP_MASK) liveMask |= mask;  // if there is complementing space (is not MAX_QP_MASK) - add masks
        qpNode = qpNode->getPredNode();             // get next predecessor
    }                                               //
    liveMask = ~liveMask;                           // this qpNode interfere with all others except complementing ones
}

//----------------------------------------------------------------------------------------//
// if qpNode has complement node - get its nodeMask (just to speed up "getCompMask" method)
// else - set all ones in compMask

void QpNode::initCompMask() { 
    
    if (compNode == NULL) compMask = MAX_QP_MASK;
    else                  compMask = compNode->getNodeMask(); 
}

//========================================================================================//
// QpTree
//========================================================================================//

QpTree::QpTree(Cfg &cfg) : 
    cfg(cfg),
    mm(cfg.getMM()),
    qpMap(mm),
    slot(0),
    p0(cfg.getOpndManager()->getP0()) {

    qpMap.insert( make_pair(p0, new(mm) QpNode(NULL, MAX_QP_MASK)) );  // make root qpNode
}

//----------------------------------------------------------------------------------------//
// build qpMap determining qp->qpNode relations. If qp is defined several times - the tree 
// contains several entryes corresponding to the qp.

void QpTree::makeQpTree(InstVector &insts) {
    
    IPF_ASSERT(qpMap.size() == 1);
    
    slot = 1;                                                  // init slot (position in mask)
    for (InstVector::iterator it=insts.begin(); it!=insts.end(); it++) {
        Inst *inst = *it;                                      // iterate insts

        if (isDefOnePred(inst)) {                              // if inst defs one predicate opnd
            OpndVector &opnds = inst->getOpnds();              // get opnds of the inst
            QpNode     *qpNode = findQpNode(opnds[0]);         // inst qp is predecessor for predicates defined in the inst
            makeQpNode(qpNode, opnds[2]);                      // make qpNode for the predicate opnd (it is always second one)
            continue;
        }

        if (isDefTwoPreds(inst)) {                             // if inst defs two predicate opnds
            OpndVector &opnds = inst->getOpnds();              // get opnds of the inst
            QpNode     *qpNode = findQpNode(opnds[0]);         // inst qp is predecessor for predicates defined in the inst
            QpNode     *p1Node = makeQpNode(qpNode, opnds[1]); // make qpNode for first predicate opnd
            QpNode     *p2Node = makeQpNode(qpNode, opnds[2]); // make qpNode for second predicate opnd
            
            if (isDefComps(inst) == false) continue;           // inst does not define mutually complemen predicates - continue
            if (p1Node != NULL) p1Node->setCompNode(p2Node);   // p2Node complements p1Node
            if (p2Node != NULL) p2Node->setCompNode(p1Node);   // p1Node complements p2Node
        }
    }    

    for (QpMap::iterator it=qpMap.begin(); it!=qpMap.end(); it++) {
        QpNode *qpNode = it->second;                           // iterate all qpNodes in the tree
        qpNode->initCompMask();                                // set comp mask (to speed up getCompMask)
    }

    for (QpMap::iterator it=qpMap.begin(); it!=qpMap.end(); it++) {
        QpNode *qpNode = it->second;                           // iterate all qpNodes in the tree
        qpNode->initLiveMask();                                // set live masks (predicate spaces which do not complement)
    }
}

//----------------------------------------------------------------------------------------//
// find qpNode corresponding to "qp" from qpMap. There are can be several nodes 
// corresponding to one qp. We should return least recent inserted one

QpNode* QpTree::findQpNode(Opnd *qp) {
    
    QpMap::iterator it = qpMap.upper_bound(qp);
    return (--it)->second;
}

//----------------------------------------------------------------------------------------//
// remove qpNode corresponding to "qp" from qpMap. There are can be several nodes 
// corresponding to one qp. We should remove least recent inserted one

void QpTree::removeQpNode(Opnd *qp) {
    
    QpMap::iterator it = qpMap.upper_bound(qp);    // find in qpMap last node corresponding to "qp"
    qpMap.erase(--it);                             // remove the node from the tree
}

//----------------------------------------------------------------------------------------//

void QpTree::printQpTree() {
    
    QpMap::iterator it = qpMap.upper_bound(p0);
    printQpNode(--it, 0);
}

//----------------------------------------------------------------------------------------//

QpNode* QpTree::makeQpNode(QpNode *predNode, Opnd *qp) {
    
    if (qp == p0) return NULL;                       // node for p0 has been created in constructor
    slot <<= 1;                                      // get next empty slot (position in mask)
    QpNode *qpNode = new(mm) QpNode(predNode, slot); // create qpNode
    qpMap.insert( make_pair(qp, qpNode) );           // insert the qpNode in qpTree
    return qpNode;
}

//----------------------------------------------------------------------------------------//

void QpTree::printQpNode(QpMap::iterator nodeIt, uint16 level) {
    
    QpNode *predNode = nodeIt->second;
    Opnd   *qp       = nodeIt->first;
    IPF_LOG << "    " << IrPrinter::toString(predNode);
    for (uint16 i=0; i<level; i++) IPF_LOG << "  ";
    IPF_LOG << "    " << IrPrinter::toString(qp) << endl;
    
    for (QpMap::iterator it=qpMap.begin(); it!=qpMap.end(); it++) {
        QpNode *qpNode = it->second;
        if (qpNode->getPredNode() != predNode) continue;
        printQpNode(it, level+1);
    }
}

//----------------------------------------------------------------------------------------//
// return true if the inst defines one pred opnd

bool QpTree::isDefOnePred(Inst *inst) {
    
    switch (inst->getInstCode()) {
        case INST_FRCPA    : 
        case INST_FPRCPA   : 
        case INST_FRSQRTA  : 
        case INST_FPRSQRTA : return true;
        default            : return false;
    }
}

//----------------------------------------------------------------------------------------//
// return true if the inst defines two pred opnds

bool QpTree::isDefTwoPreds(Inst *inst) {
    
    switch (inst->getInstCode()) {
        case INST_CMP    : 
        case INST_CMP4   : 
        case INST_FCMP   : 
        case INST_TBIT   : 
        case INST_TNAT   : 
        case INST_FCLASS : return true;
        default          : return false;
    }
}

//----------------------------------------------------------------------------------------//
// return true if the inst defines two mutually complement preds

bool QpTree::isDefComps(Inst *inst) {
    
    InstCode instCode = inst->getInstCode();
    if (instCode == INST_FCMP || instCode == INST_FCLASS) return true;
    if (instCode == INST_TBIT || instCode == INST_TNAT)   return true;
    
    CompVector &comps = inst->getComps();
    if (comps.size() < 2)                 return true;
    if (comps[1] == CMPLT_CMP_CTYPE_NONE) return true;
    if (comps[1] == CMPLT_CMP_CTYPE_UNC)  return true;
    return false;
}

//========================================================================================//
// LiveManager
//========================================================================================//

LiveManager::LiveManager(Cfg &cfg) : 
    cfg(cfg),
    qpTree(cfg),
    liveSet(cfg.getMM()) {
}

//----------------------------------------------------------------------------------------//

void LiveManager::init(Node *node) {
    
    liveSet.clear();                                 // clear live set
    node->mergeOutLiveSets(liveSet);                 // all opnds alive in successors are alive in current live set

    for (RegOpndSet::iterator it=liveSet.begin(); it!=liveSet.end(); it++) {
        RegOpnd *opnd = *it;                         //
        opnd->orQpMask(MAX_QP_MASK);                 // initially opnds alive in all pred spaces
    }                                                //

    if (node->getNodeKind() != NODE_BB) return;      // if node is not BB - nothing to do
    qpTree.makeQpTree(((BbNode *)node)->getInsts()); // make qpTree for current BB
}

//----------------------------------------------------------------------------------------//
// add opnds used in the inst in liveSet. Or modify opnd's qpMask to reflect use under inst's qp

void LiveManager::use(Inst *inst) {

    OpndVector &opnds  = inst->getOpnds();      // get inst's opnds
    RegOpnd    *qp     = (RegOpnd *)opnds[0];   // fist opnd is instruction qp
    QpNode     *qpNode = qpTree.findQpNode(qp); // find qpNode corresponding to the qp

    if (qp->isWritable()) {
        qp->orQpMask(MAX_QP_MASK);              // qp is alive in all pred spaces
        liveSet.insert(qp);
    }

    for (uint16 i=inst->getNumDst()+1; i<opnds.size(); i++) {
        RegOpnd *opnd = (RegOpnd *)opnds[i];
        if (opnd->isWritable() == false) continue;
        useOpnd(qpNode, opnd);
        liveSet.insert(opnd);
    }
}
    
//----------------------------------------------------------------------------------------//
// remove opnds defined in the inst from liveSet. Or modify opnd's qpMask to reflect def 
// under inst's qp

void LiveManager::def(Inst *inst) {

    OpndVector &opnds  = inst->getOpnds();
    RegOpnd    *qp     = (RegOpnd *)opnds[0];   // fist opnd is instruction qp
    QpNode     *qpNode = qpTree.findQpNode(qp);

    for (uint16 i=1; i<=inst->getNumDst(); i++) {
        RegOpnd *opnd = (RegOpnd *)opnds[i];
        if (opnd->isWritable()    == false) continue;
        if (defOpnd(qpNode, opnd) == false) continue;
        liveSet.erase(opnd);
    }
}
    
//----------------------------------------------------------------------------------------//

QpMask LiveManager::getLiveMask(RegOpnd *opnd) {
    QpNode *qpNode = qpTree.findQpNode(opnd);
    return qpNode->getLiveMask();
}

//----------------------------------------------------------------------------------------//

void LiveManager::useOpnd(QpNode *qpNode, RegOpnd *opnd) {
    
    QpMask mask = qpNode->getNodeMask();         // get mask of predicates alive in space of the qpNode
    opnd->orQpMask(mask);                        // add mask to current opnd qpMask
}

//----------------------------------------------------------------------------------------//

bool LiveManager::defOpnd(QpNode *qpNode, RegOpnd *opnd) {
    
    if (opnd->isQp()) qpTree.removeQpNode(opnd); // if we def predicate opnd - remove corresponding qpNode from qpTree
    QpMask mask = opnd->getQpMask();             // get mask of pred spaces, the opnd alive in
    if (mask == 0) return true;                  // if mask is zero - opnd is dead
     
    while ((mask & qpNode->getCompMask())==0) {  // while opnd is dead in complement predicate space - propagate def up
        qpNode = qpNode->getPredNode();          // get predecessor in qpTree
    }                                            // 

    opnd->andQpMask(~(qpNode->getNodeMask()));   // null bits corresponding the successors in opnd's qp mask
    if (opnd->getQpMask()) return false;         // opnd is alive in some predicate spaces
    else                   return true;          // opnd is dead
}

} // IPF
} // Jitrino
