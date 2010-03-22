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

#include "IpfCodeSelector.h"
#include "IpfOpndManager.h"
#include "IpfIrPrinter.h"

namespace Jitrino {
namespace IPF {

//========================================================================================//
// IpfMethodCodeSelector
//========================================================================================//

IpfMethodCodeSelector::IpfMethodCodeSelector(Cfg                  &cfg,
                                             CompilationInterface &compilationInterface) : 
    mm(cfg.getMM()),
    cfg(cfg), 
    compilationInterface(compilationInterface),
    methodDesc(NULL),
    opnds(mm),
    nodes(mm) {
}

//----------------------------------------------------------------------------------------//

void IpfMethodCodeSelector::genVars(U_32 numVars, VarCodeSelector &varCodeSelector) {

    IPF_LOG << endl << "  Select variables" << endl;
    IpfVarCodeSelector ipfVarCodeSelector(cfg, opnds);
    varCodeSelector.genCode(ipfVarCodeSelector);
}

//----------------------------------------------------------------------------------------//

void IpfMethodCodeSelector::setMethodDesc(MethodDesc *methodDesc_) {
    methodDesc = methodDesc_;
}

//----------------------------------------------------------------------------------------//

void IpfMethodCodeSelector::genCFG(U_32           numNodes, 
                                   CFGCodeSelector &codeSelector, 
                                   bool             useProfile) {

    IPF_LOG << endl << "  Select CFG" << endl;
    IpfCfgCodeSelector cfgCodeSelector(cfg, nodes, opnds, compilationInterface);
    codeSelector.genCode(cfgCodeSelector);
}

//----------------------------------------------------------------------------------------//

MethodDesc *IpfMethodCodeSelector::getMethodDesc() { return methodDesc; }

//========================================================================================//
// IpfVarCodeSelector
//========================================================================================//

IpfVarCodeSelector::IpfVarCodeSelector(Cfg &cfg, OpndVector &opnds) :
    mm(cfg.getMM()),
    cfg(cfg),
    opnds(opnds) {

    opndManager = cfg.getOpndManager();
}

//----------------------------------------------------------------------------------------//

U_32 IpfVarCodeSelector::defVar(Type *varType, bool isAddressTaken, bool isPinned) {

    OpndKind  opndKind = IpfInstCodeSelector::toOpndKind(varType->tag);
    DataKind  dataKind = IpfInstCodeSelector::toDataKind(varType->tag);
    RegOpnd  *var      = NULL;
    
    if (Type::isTau(varType->tag)) var = opndManager->getTau();
    else                           var = opndManager->newRegOpnd(opndKind, dataKind);

    opnds.push_back(var);

    IPF_LOG << "    Define local variable" 
        << "; varType=" << Type::tag2str(varType->tag)
        << ", isPinned=" << isPinned
        << "; var='" << IrPrinter::toString(var) << "'" << endl;

    return opnds.size()-1;      // It is going to be varId
}

//----------------------------------------------------------------------------------------//

void IpfVarCodeSelector::setManagedPointerBase(U_32 managedPtrVarNum, U_32 baseVarNum) {}

//========================================================================================//
// IpfCfgCodeSelector
//========================================================================================//

IpfCfgCodeSelector::IpfCfgCodeSelector(Cfg                  &cfg, 
                                       NodeVector           &nodes, 
                                       OpndVector           &opnds,
                                       CompilationInterface &compilationInterface) : 
    mm(cfg.getMM()),
    cfg(cfg),
    nodes(nodes), 
    opnds(opnds),
    compilationInterface(compilationInterface),
    opndManager(cfg.getOpndManager()) {
}

//----------------------------------------------------------------------------------------//

U_32 IpfCfgCodeSelector::genDispatchNode(U_32 numInEdges, 
                                           U_32 numOutEdges, 
                                           const StlVector<MethodDesc*>& inlineInfo,
                                           double cnt) {

    Node *node = new(mm) Node(mm, opndManager->getNextNodeId(), (U_32) cnt, NODE_DISPATCH);
    nodes.push_back(node);

    IPF_LOG << endl << "    Generate Dispatch node" << node->getId() << endl;
    return nodes.size()-1;
}

//----------------------------------------------------------------------------------------//

U_32 IpfCfgCodeSelector::genBlock(U_32            numInEdges, 
                                    U_32            numOutEdges, 
                                    BlockKind         blockKind, 
                                    BlockCodeSelector &codeSelector, 
                                    double            cnt) {

    BbNode *node = new(mm) BbNode(mm, opndManager->getNextNodeId(), (U_32)cnt);

    nodes.push_back(node);
    if(blockKind == Prolog) cfg.setEnterNode(node);

    IPF_LOG << endl << "    Generate BB node" << node->getId() << endl;
    IpfInstCodeSelector ipfInstCodeSelector(cfg, *node, opnds, compilationInterface);
    codeSelector.genCode(ipfInstCodeSelector);

    return nodes.size()-1;
}

//----------------------------------------------------------------------------------------//

U_32  IpfCfgCodeSelector::genUnwindNode(U_32 numInEdges, 
                                          U_32 numOutEdges, 
                                          double cnt) {

    Node *node = new(mm) Node(mm, opndManager->getNextNodeId(), (U_32) cnt, NODE_UNWIND);
    nodes.push_back(node);

    IPF_LOG << endl << "    Generate Unwind node" << node->getId() << endl;
    return nodes.size()-1;
}

//----------------------------------------------------------------------------------------//

U_32 IpfCfgCodeSelector::genExitNode(U_32 numInEdges, double cnt) {

    BbNode *node = new(mm) BbNode(mm, opndManager->getNextNodeId(), (U_32) cnt);
    nodes.push_back(node);
    cfg.setExitNode(node);

    IPF_LOG << endl << "    Generate Exit node" << node->getId() << endl << endl;
    return nodes.size()-1;
}

//----------------------------------------------------------------------------------------//

void IpfCfgCodeSelector::genUnconditionalEdge(U_32 tailNodeId,
                                              U_32 headNodeId, 
                                              double prob) {

    IPF_LOG << "    Generate Unconditional edge node" << nodes[tailNodeId]->getId();
    IPF_LOG << " -> node" << nodes[headNodeId]->getId() << endl;

    Edge *edge = new(mm) Edge(nodes[tailNodeId], nodes[headNodeId], prob, EDGE_THROUGH);
    edge->insert();
}

//----------------------------------------------------------------------------------------//

void IpfCfgCodeSelector::genTrueEdge(U_32 tailNodeId, U_32 headNodeId, double prob) {

    IPF_LOG << "    Generate True          edge node" << nodes[tailNodeId]->getId();
    IPF_LOG << " -> node" << nodes[headNodeId]->getId() << endl;

    Edge *edge = new(mm) Edge(nodes[tailNodeId], nodes[headNodeId], prob, EDGE_BRANCH);
    edge->insert();
}

//----------------------------------------------------------------------------------------//

void IpfCfgCodeSelector::genFalseEdge(U_32 tailNodeId, U_32 headNodeId, double prob) {

    IPF_LOG << "    Generate False         edge node" << nodes[tailNodeId]->getId();
    IPF_LOG << " -> node" << nodes[headNodeId]->getId() << endl;

    Edge *edge = new(mm) Edge(nodes[tailNodeId], nodes[headNodeId], prob, EDGE_THROUGH);
    edge->insert();
}

//----------------------------------------------------------------------------------------//

void IpfCfgCodeSelector::genSwitchEdges(U_32  tailNodeId, 
                                        U_32  numTargets, 
                                        U_32 *targets, 
                                        double *probs, 
                                        U_32  defaultTarget) {

    BbNode         *tailNode       = (BbNode *)nodes[tailNodeId];
    InstVector     &insts          = tailNode->getInsts();
    Inst           *switchInst     = insts.back();
    Opnd           *defTargetImm   =                   switchInst->getOpnd(POS_SWITCH_DEFAULT);
    ConstantRef    *constantRef    = (ConstantRef *)   switchInst->getOpnd(POS_SWITCH_TABLE);
    SwitchConstant *switchConstant = (SwitchConstant *)constantRef->getConstant();
    Edge           *defedge        = NULL;
    Edge           *edge           = NULL;

    bool   defadded = false;
    U_32 i        = 0;

    IPF_LOG << "    Generate Switch     tailNodeId=" << tailNodeId 
        << "; defaultTarget=" << defaultTarget << endl;

    defedge = new(mm) Edge(nodes[tailNodeId], nodes[defaultTarget], probs[defaultTarget], EDGE_BRANCH);
    defedge->insert();

    for(i=0; i<numTargets; i++) {
        if(targets[i] == defaultTarget) {
            defTargetImm->setValue(i);
            switchConstant->addEdge(defedge);
            defadded = true;
            IPF_LOG << "        default: " << i << endl;
            continue;
        }
        IPF_LOG << "        case " << i << ": " << targets[i] << endl;
        edge = new(mm) Edge(nodes[tailNodeId], nodes[targets[i]], probs[i], EDGE_BRANCH);
        edge->insert();
        switchConstant->addEdge(edge);
    }

    if (!defadded) {
        defTargetImm->setValue(i);
        switchConstant->addEdge(defedge);
        defadded = true;
        IPF_LOG << "        default: " << i << endl;
    }
}

//--------------------------------------------------------------------------------//
// edge from try region to dispatch node 

void IpfCfgCodeSelector::genExceptionEdge(U_32 tailNodeId, 
                                          U_32 headNodeId, 
                                          double prob) {

    Node *tailNode = nodes[tailNodeId];
    Node *headNode = nodes[headNodeId];

    IPF_LOG << "    Generate Exception     edge node" << tailNode->getId();
    IPF_LOG << " -> node" << headNode->getId() << endl;
    Edge *edge = new(mm) Edge(tailNode, headNode, prob, EDGE_DISPATCH);
    edge->insert();
}

//----------------------------------------------------------------------------------------//
// edge from dispatch node to exception handler 

void IpfCfgCodeSelector::genCatchEdge(U_32  tailNodeId, 
                                      U_32  headNodeId, 
                                      U_32  priority, 
                                      Type   *exceptionType, 
                                      double  prob) {

    Node *tailNode = nodes[tailNodeId];
    Node *headNode = nodes[headNodeId];

    IPF_LOG << "    Generate Catch         edge node" << tailNode->getId();
    IPF_LOG << " -> node" << headNode->getId() << endl;

    ExceptionEdge *edge = new(mm) ExceptionEdge(tailNode, headNode, prob, exceptionType, priority);
    edge->insert();
}

//----------------------------------------------------------------------------------------//

void IpfCfgCodeSelector::genExitEdge(U_32 tailNodeId,
                                     U_32 headNodeId,
                                     double prob) {

    Node *tailNode=nodes[tailNodeId];
    Node *headNode=nodes[headNodeId];

    IPF_LOG << "    Generate Exit          edge node" << tailNode->getId();
    IPF_LOG << " -> node" << headNode->getId() << endl;

    Edge *edge = new(mm) Edge(tailNode, headNode, prob, EDGE_THROUGH);
    edge->insert();
}

//----------------------------------------------------------------------------------------//

void IpfCfgCodeSelector::setLoopInfo(U_32 nodeId, 
                                     bool   isLoopHeader, 
                                     bool   hasContainingLoopHeader, 
                                     U_32 headerId) {

    if (hasContainingLoopHeader == false) return;
    
    Node *node   = nodes[nodeId];
    Node *header = nodes[headerId];

    node->setLoopHeader(header);
}

//----------------------------------------------------------------------------------------//

void IpfCfgCodeSelector::setPersistentId(U_32 nodeId, U_32 persistentId) {}

} //namespace IPF
} //namespace Jitrino 
