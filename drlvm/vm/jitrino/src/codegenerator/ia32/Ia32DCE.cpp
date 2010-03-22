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
 * @author Vyacheslav P. Shakin
 */

#include "Ia32IRManager.h"
#include "Ia32CodeGenerator.h"

namespace Jitrino
{
namespace Ia32{

/**
    class DCE performs Dead code elimination
*/
class DCE : public SessionAction {
public:
    void runImpl();
    U_32 getSideEffects() const {return 0;}
    U_32 getNeedInfo()const {return 0;}
};

static ActionFactory<DCE> _dce("cg_dce");


//========================================================================================
// class DCE
//========================================================================================

//_________________________________________________________________________________________________
void DCE::runImpl()
{   
    bool early = false;
    getArg("early", early);
    if (early && !irManager->getCGFlags()->earlyDCEOn) {
        return;
    }

    irManager->updateLivenessInfo();
    irManager->calculateOpndStatistics();
    BitSet ls(irManager->getMemoryManager(), irManager->getOpndCount());
    const Nodes& nodes = irManager->getFlowGraph()->getNodesPostOrder();
    for (Nodes::const_iterator it = nodes.begin(),end = nodes.end();it!=end; ++it) {
        Node* node = *it;
        if (node->isBlockNode()){
            //Here we'll try to remove redundant branches that could appear after 
            //branch translations. All such branches are supposed to be conditional.
            Inst * inst = (Inst *)node->getLastInst();
            if(inst && node->getOutEdges().size() > 1) {
                Edges edges = node->getOutEdges();
                for (Edges::const_iterator ite1 = ++edges.begin(), end = edges.end(); ite1 != end; ++ite1) {
                    for (Edges::const_iterator ite2 = edges.begin(); ite1 != ite2; ++ite2) {
                        Edge *edge1 = *ite1;
                        Edge *edge2 = *ite2;
                        assert(edge1 != edge2);

                        //If this condition is satisfied then there are at least two branches with 
                        //the same destination
                        if (edge1->getTargetNode() == edge2->getTargetNode()) {
                            //Check that edges are conditional and the last instruction is branch, 
                            //the other situations are not permitted at the moment
                            assert(inst->hasKind(Inst::Kind_BranchInst));
                            assert(edge1->getKind() == Edge::Kind_True || 
                                        edge1->getKind() == Edge::Kind_False);
                            assert(edge2->getKind() == Edge::Kind_True || 
                                        edge2->getKind() == Edge::Kind_False);
                            
                            //Remove last instruction if it is a branch
                            inst->unlink();
                            irManager->getFlowGraph()->removeEdge(edge2);
                        }
                    
                    }
                }
           }

            irManager->getLiveAtExit(node, ls);
            for (Inst * inst=(Inst*)node->getLastInst(), * prevInst=NULL; inst!=NULL; inst=prevInst){
                prevInst=inst->getPrevInst();
                // Prevent debug traps or instructions with side effects
                // like (MOVS) from being removed.
                bool deadInst=!inst->hasSideEffect() && (inst->getMnemonic() != Mnemonic_INT3);
                if (deadInst){
                    if (inst->hasKind(Inst::Kind_CopyPseudoInst)){
                        Opnd * opnd=inst->getOpnd(1);
                        if (opnd->getType()->isFP() && opnd->getDefiningInst()!=NULL && opnd->getDefiningInst()->getMnemonic()==Mnemonic_CALL){
                            deadInst=false;
                        }
                    }
                    if (deadInst){
                        Inst::Opnds opnds(inst, Inst::OpndRole_All);
                        for (Inst::Opnds::iterator ito = opnds.begin(); ito != opnds.end(); ito = opnds.next(ito)){
                            Opnd * opnd = inst->getOpnd(ito);
                            if ((ls.getBit(opnd->getId()) && (inst->getOpndRoles(ito) & Inst::OpndRole_Def)) ||
                                (((opnd->getMemOpndKind()&(MemOpndKind_Heap|MemOpndKind_StackManualLayout))!=0) && (inst->getMnemonic() != Mnemonic_LEA))) {
                                deadInst=false;
                                break;
                            }
                        }
                    }
                }
                if (deadInst) {
                    inst->unlink();
                } else {
                    irManager->updateLiveness(inst, ls);
                }
            }
            irManager->getLiveAtEntry(node)->copyFrom(ls);
        }
    }

    irManager->eliminateSameOpndMoves();

    irManager->getFlowGraph()->purgeEmptyNodes();
    irManager->getFlowGraph()->mergeAdjacentNodes(true, false);
    irManager->getFlowGraph()->purgeUnreachableNodes();

    irManager->packOpnds();
    irManager->invalidateLivenessInfo();
}

}}; //namespace Ia32


