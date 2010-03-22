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

#ifndef IPFLIVEMANAGER_H_
#define IPFLIVEMANAGER_H_

#include "IpfCfg.h"

using namespace std;

namespace Jitrino {
namespace IPF {

//========================================================================================//
// QpNode
//========================================================================================//

class QpNode {
public:
            QpNode(QpNode*, QpMask);
    void    initLiveMask();
    void    initCompMask();
    QpNode* getPredNode()             { return predNode; }
    QpMask  getNodeMask()             { return nodeMask; }
    QpMask  getCompMask()             { return compMask; }
    QpMask  getLiveMask()             { return liveMask; }
    void    setCompNode(QpNode *node) { compNode = node; }
    void    setNodeMask(QpMask mask)  { nodeMask = mask; }
    void    orNodeMask(QpMask mask)   { nodeMask |= mask; }
    
protected:
    QpNode  *predNode;   // predecessor in QpTree tree
    QpNode  *compNode;   // qpNode that complements current one (NULL if there is no complement)
    QpMask  nodeMask;    // predicates alive in current predicate space
    QpMask  compMask;    // mask of compNode (to speed up "getCompMask" method)
    QpMask  liveMask;    // mask of prdicate spaces interfering with current (all 
                         // predicates except complementing ones)
};

//========================================================================================//
// QpTree
//========================================================================================//

class QpTree {
public:
                  QpTree(Cfg&);
    void          makeQpTree(InstVector&);
    QpNode*       findQpNode(Opnd*);
    void          removeQpNode(Opnd*);
    void          printQpTree();

protected:
    QpNode*       makeQpNode(QpNode*, Opnd*);
    void          printQpNode(QpMap::iterator, uint16);
    bool          isDefOnePred(Inst*);
    bool          isDefTwoPreds(Inst*);
    bool          isDefComps(Inst*);
    
    Cfg           &cfg;
    MemoryManager &mm;
    QpMap         qpMap;       // map containing qpNodes for current node
    QpMask        slot;        // current available position in mask
    RegOpnd       *p0;         // opnd representing p0
};

//========================================================================================//
// LiveManager
//========================================================================================//

class LiveManager {
public:
                  LiveManager(Cfg&);
    void          init(Node*);
    void          def(Inst*);
    void          use(Inst*);
    QpMask        getLiveMask(RegOpnd*);
    void          printQpTree()         { qpTree.printQpTree(); }
    RegOpndSet&   getLiveSet()          { return liveSet; }

protected:
    void          useOpnd(QpNode*, RegOpnd*);
    bool          defOpnd(QpNode*, RegOpnd*);
    
    Cfg           &cfg;
    QpTree        qpTree;  // tree definind relations between qualifying predicates in node
    RegOpndSet    liveSet; // current liveSet used in liveness analysis (DCE, register allocator ...)
};

} // IPF
} // Jitrino

#endif /*IPFLIVEMANAGER_H_*/
