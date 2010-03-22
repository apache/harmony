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
 * @author Xiaoming GU
 */

#include <fstream>
#include "Stl.h"
#include "Log.h"
#include "Ia32IRManager.h"
#include "Ia32Printer.h"

namespace Jitrino
{
namespace Ia32
{

//========================================================================================
// class GlobalPropagation
//========================================================================================
/**
 * This file is about LIR global constant/copy propagation for non-MEM operands. It is 
 * augmented from "early propagation" which only works for the operands defined just once.
 *  
 *   0. Data Structures
 *         A dst-src pair: [dst, src] which comes from a copy inst - MOV dst,src
 *         Eache BB: IN, OUT, KILL, GEN 
 *           IN: the set of dst-src pairs which are valid at the beginning of a BB (StlHashMap)
 *           OUT: the set of dst-src pairs which are valid at the end of a BB (StlHashMap)
 *           KILL: the set of variables which are killed in a BB (StlSet)
 *           GEN: the local set of dst-src pairs which are valid at the end of a BB (StlHashMap)
 *           VISITED: the flag indicating whether a BB is processed before (bool) 
 *         A list BBInfoHashMap (StlHashMap) containing BBInfo for each BB
 *   1. Initialization
 *       a) Local copy propagation
 *       b) Do local analysis for all BBs to compute KILL, GEN (OUT) and set IN as null.
 *   2. Iterative Data-flow Analysis
 *       While any IN changes, traverse all BBs. (Order doesn't matter on correctness 
 *       but preorder is better - less iterations.)
 *           For each BB b
 *               a) IN(b) = intersect(OUT of all pred(b))
 *                   + Do lattice computation on the srcs for the multiple pairs with the same dst.
 *               b) OUT(b) = (IN(b)-KILL(b)) union GEN(b) 
 *                   + If no change happens in IN, continue to next BB.
 *                   + Delete the pairs related to KILL.
 *                   + Add the pairs from GEN to OUT.
 *   3. Local propagation for the pairs in IN
 */

//#define DEBUG_GLOBAL_PROP
class GlobalPropagation : public SessionAction {
    void runImpl();
    U_32 getNeedInfo()const{ return 0; }
};

static ActionFactory<GlobalPropagation> _global_prop("global_prop");

static bool isTypeConversionAllowed(Opnd* fromOpnd, Opnd* toOpnd) {
    Type * fromType = fromOpnd->getType();
    Type * toType = toOpnd->getType();
    bool fromIsGCType = fromType->isObject() || fromType->isManagedPtr();
    bool toIsGCType = toType->isObject() || toType->isManagedPtr();
    return fromIsGCType == toIsGCType;
}

struct BBInfo {
    StlHashMap<Opnd*, Opnd*> *in;
    StlHashMap<Opnd*, Opnd*> *out;
    StlSet<Opnd*> *kill;
    StlHashMap<Opnd*, Opnd*> *gen;
    bool visited; //for loop
    BBInfo()
            :in(NULL), out(NULL), kill(NULL), gen(NULL) {}
};

#define BOTTOM NULL

void outputBBInfo(Node* node, BBInfo *bbInfo) {
    std::cout << "************BB #" << node->getId() << "************" << std::endl;
    std::cout << "\t\tPred: ";
    const Edges& edges = node->getInEdges();
    Edges::const_iterator ite=edges.begin();
    Edges::const_iterator ende=edges.end();
    while(ite != ende) {
        std::cout << (*ite)->getSourceNode()->getId() << "\t";
        ++ite;
    }
    std::cout << std::endl;

    std::cout << "\tIN: " << std::endl;
    for(StlHashMap<Opnd*, Opnd*>::iterator iter=bbInfo->in->begin();
        iter!=bbInfo->in->end();++iter) {
        std::cout << "\t\tdst: " << iter->first->getFirstId();
        if (iter->second != NULL)
            std::cout << "\tsrc: " << iter->second->getFirstId() << std::endl;
        else
            std::cout << "\tsrc: BOTTOM" << std::endl;
    }

    std::cout << "\tOUT: " << std::endl;
    for(StlHashMap<Opnd*, Opnd*>::iterator iter=bbInfo->out->begin();
        iter!=bbInfo->out->end();++iter) {
        std::cout << "\t\tdst: " << iter->first->getFirstId();
        if (iter->second != NULL)
            std::cout << "\tsrc: " << iter->second->getFirstId() << std::endl;
        else
            std::cout << "\tsrc: BOTTOM" << std::endl;
    }

    std::cout << "\tKILL: " << std::endl;
    StlSet<Opnd*>::iterator iterr;
    for(iterr=bbInfo->kill->begin();iterr!=bbInfo->kill->end();++iterr) {
        std::cout << "\t\topnd: " << (*iterr)->getFirstId() << std::endl;
    }

    std::cout << "\tGEN: " << std::endl;
    for(StlHashMap<Opnd*, Opnd*>::iterator iter=bbInfo->gen->begin();
        iter!=bbInfo->gen->end();++iter) {
        std::cout << "\t\tdst: " << iter->first->getFirstId() << "\tsrc: " << iter->second->getFirstId() << std::endl;
    }

    std::cout << "\tvisited: " << bbInfo->visited << std::endl;
}

//___________________________________________________________________________________________________
void GlobalPropagation::runImpl()
{  
    irManager->updateLoopInfo();

    MemoryManager mm("global_prop");
    StlHashMap<Node*, BBInfo*> BBInfoList(mm);
    StlSet<Opnd*> temp(mm);
    StlHashMap<Opnd*, Opnd*> newIn(mm);
    
    /* Local analysis and propagation */
    const Nodes& postOrdered = irManager->getFlowGraph()->getNodesPostOrder();
    for (Nodes::const_reverse_iterator it=postOrdered.rbegin(), end=postOrdered.rend();it!=end;++it) {
        Node * node=*it;

        BBInfo *bbInfo = new(mm) BBInfo;
        BBInfoList[node] = bbInfo;
        bbInfo->in = new(mm) StlHashMap<Opnd*, Opnd*>(mm);
        bbInfo->out = new(mm) StlHashMap<Opnd*, Opnd*>(mm);
        bbInfo->kill = new(mm) StlSet<Opnd*>(mm);
        bbInfo->gen = new(mm) StlHashMap<Opnd*, Opnd*>(mm);
        bbInfo->visited = false;
        
        /*
         * Traverse all INST in a BB in preorder
         *    For all INST - several DEF, several USE 
         *        + Replace a USE with [USE, src] in GEN
         *        + Delete [DEF, ...] and [..., DEF] from GEN 
         *        + Add DEF to KILL
         */
        for (Inst * inst=(Inst*)node->getFirstInst();inst != NULL;inst=inst->getNextInst()) {
            const bool isCopy = inst->getMnemonic() == Mnemonic_MOV;
            Inst::Opnds opnds(inst, Inst::OpndRole_All);
  
            for (Inst::Opnds::iterator it=opnds.begin();it != opnds.end();it = opnds.next(it)) {
                Opnd * opnd=inst->getOpnd(it);
                U_32 roles=inst->getOpndRoles(it);
                
                if (roles & Inst::OpndRole_Use) {
                    if ((roles & Inst::OpndRole_All & Inst::OpndRole_FromEncoder) 
                        && (roles & Inst::OpndRole_All & Inst::OpndRole_ForIterator)
                        && (roles & Inst::OpndRole_Changeable) && ((roles & Inst::OpndRole_Def) == 0)
                        && bbInfo->gen->has(opnd)) {
                        if (opnd->getType()->isUnmanagedPtr() && (*(bbInfo->gen))[opnd]->getType()->isInteger())
                            (*(bbInfo->gen))[opnd]->setType(opnd->getType());
                        inst->setOpnd(it, (*(bbInfo->gen))[opnd]);
                   }
                }
            }
            
            for (Inst::Opnds::iterator it = opnds.begin();it != opnds.end();it = opnds.next(it)) {
                Opnd * opnd=inst->getOpnd(it);
                U_32 roles=inst->getOpndRoles(it);

                if (roles & Inst::OpndRole_Def) {
                    if (bbInfo->gen->has(opnd)) {
                        bbInfo->gen->erase(opnd);
                    }

                    temp.clear();
                    for(StlHashMap<Opnd*, Opnd*>::iterator iter=bbInfo->gen->begin();
                        iter!=bbInfo->gen->end();++iter) {
                        if (iter->second == opnd) {
                            temp.insert(iter->first);
                        }
                    }
                    for(StlSet<Opnd*>::iterator iter=temp.begin();
                        iter!=temp.end();++iter) {
                        bbInfo->gen->erase(*iter);
                    }

                    if (!bbInfo->kill->has(opnd))
                        bbInfo->kill->insert(opnd);

                }
            }

            /*
             * For MOV inst which could update the map - one DEF, one USE 
             *        + Add [DEF, USE] to GEN
             *           - If [USE, s_old] exists in OUT then add [DEF, s_old] instead.
             *           - Otherwise add [DEF, USE] itself.
             */
            if (isCopy) {
                Inst::Opnds opnds(inst, Inst::OpndRole_All);
                Opnd * dst = NULL;
                Opnd * src = NULL;
                U_32 counterDef = 0;
                U_32 counterUse = 0;
                for (Inst::Opnds::iterator it=opnds.begin();it!=opnds.end();it=opnds.next(it)) {
                    Opnd * opnd = inst->getOpnd(it);
                    U_32 roles = inst->getOpndRoles(it);
                    
                    if (roles & Inst::OpndRole_Def) {
                        counterDef++;
                        dst = opnd;
                    } else if (roles & Inst::OpndRole_Use) {
                        counterUse++;
                        src = opnd;
                    }
                }

                if ((counterDef == 1) && (counterUse == 1) && (!dst->hasAssignedPhysicalLocation())) {
                    bool kindsAreOk = true;
                    if(src->canBePlacedIn(OpndKind_FPReg) || dst->canBePlacedIn(OpndKind_FPReg)) {
                        Constraint srcConstr = src->getConstraint(Opnd::ConstraintKind_Calculated);
                        Constraint dstConstr = dst->getConstraint(Opnd::ConstraintKind_Calculated);
                        kindsAreOk = ! (srcConstr&dstConstr).isNull();
                    }
                    bool typeConvOk = isTypeConversionAllowed(src, dst);
                    if (typeConvOk && kindsAreOk && ! src->isPlacedIn(OpndKind_Reg)) {
                        if (bbInfo->gen->has(src)) {
                            /* The oldest source is selected heuristically. Some chances maybe missed afterward. */
                            (*(bbInfo->gen))[dst] = (*(bbInfo->gen))[src];
                        }
                        else {
                            (*(bbInfo->gen))[dst] = src;
                        }
                    }
                }
            }
        }

        bbInfo->out->insert(bbInfo->gen->begin(), bbInfo->gen->end());
    }

#ifdef DEBUG_GLOBAL_PROP
    for (Nodes::const_reverse_iterator it=postOrdered.rbegin(),end=postOrdered.rend();it!=end;++it) {
        Node * node=*it;
        if (isMain)
            outputBBInfo(node, BBInfoList[node]);
    }
#endif

    /* Iterative data-flow analysis */
    bool changed = true;
    while (changed) {
        changed = false;
        
        for (Nodes::const_reverse_iterator it=postOrdered.rbegin(),end = postOrdered.rend();it!=end;++it) {
            Node * node = *it;
            BBInfo *bbInfo = BBInfoList[node];
            if (!bbInfo->visited)
                bbInfo->visited = true;

            /* Set IN of a BB as the Intersection of all the OUT of its visited predecessors */
            const Edges& edges = node->getInEdges();
            Edges::const_iterator ite=edges.begin();
            Edges::const_iterator ende=edges.end();
            Edge* edge = NULL;
            Node * pred = NULL;
            BBInfo *predBBInfo = NULL;

            newIn.clear();
            /* Find the first valid predecessor */
            while (ite != ende) {
                edge = *ite;
                pred = edge->getSourceNode();
                predBBInfo = BBInfoList[pred];
                if (predBBInfo->visited)
                    break;
                pred = NULL;
                predBBInfo = NULL;
                ++ite;
            }
            if (predBBInfo) {
                for(StlHashMap<Opnd*, Opnd*>::iterator iter=predBBInfo->out->begin();
                    iter!=predBBInfo->out->end();++iter) {
                    Opnd* dst = iter->first;
                    Opnd* src = iter->second;
                    newIn[dst] = src;
                }
                ++ite;

                for (;ite!=ende;++ite) {
                    edge = *ite;
                    pred = edge->getSourceNode();
                    predBBInfo = BBInfoList[pred];
                    if (!predBBInfo->visited)
                        continue;
                    
                    /* Do intersection */
                    temp.clear();
                    for(StlHashMap<Opnd*, Opnd*>::iterator iter=newIn.begin();
                        iter!=newIn.end();++iter) {
                        Opnd* dst = iter->first;
                        if (!predBBInfo->out->has(dst)) {
                            temp.insert(dst);
                        }
                        else {
                            Opnd* src = (*(predBBInfo->out))[dst];
                            Opnd* exSrc = iter->second;
                            if (src != BOTTOM) {
                                if (exSrc == BOTTOM)
                                    src = BOTTOM;
                                else if ((exSrc != src) && (!(exSrc->isPlacedIn(OpndKind_Imm) 
                                           && src->isPlacedIn(OpndKind_Imm) && (exSrc->getImmValue() == src->getImmValue()))))
                                           src = BOTTOM;
                            }

                            if (src == BOTTOM) {
                                temp.insert(dst);
                            }
                        }
                    }
                    for(StlSet<Opnd*>::iterator iter=temp.begin();
                        iter!=temp.end();++iter) {
                        newIn.erase(*iter);
                    }
                }
            }

            bool equal = true;
            for (StlHashMap<Opnd*, Opnd*>::iterator iter=newIn.begin();
                iter!=newIn.end();++iter) {
                if (!((bbInfo->in->has(iter->first)) && ((*(bbInfo->in))[iter->first] == iter->second))) {
                    equal = false;
                    break;
                }
            }
            if (equal) {
                for (StlHashMap<Opnd*, Opnd*>::iterator iter=bbInfo->in->begin();
                    iter!=bbInfo->in->end();++iter) {
                    if (!((newIn.has(iter->first)) && (newIn[iter->first] == iter->second))) {
                        equal = false;
                        break;
                    }
                }
            }
            if (!equal) {
                bbInfo->in->clear();
                bbInfo->in->insert(newIn.begin(), newIn.end());
                if (!changed)
                    changed = true;
            } else {
                continue;
            }
            
            /* Compute OUT from current IN and invariant KILL, GEN */
            bbInfo->out->clear();
            for(StlHashMap<Opnd*, Opnd*>::iterator iter=bbInfo->in->begin();
                iter!=bbInfo->in->end();++iter) {
                Opnd* dst = iter->first;
                Opnd* src = iter->second;
                if (!(bbInfo->kill->has(dst) || bbInfo->kill->has(src)))
                    (*(bbInfo->out))[dst] = src;
            }

            /*
             *  Try to add [d_new, s_new] from GEN to OUT
             *     - If [s_new, s_old] exists in OUT then add [d_new, s_old] instead.
             *     - Otherwise add [d_new, s_new] itself.
             */
            for(StlHashMap<Opnd*, Opnd*>::iterator iterr=bbInfo->gen->begin();
                iterr!=bbInfo->gen->end();++iterr) {
                Opnd* dst = iterr->first;
                Opnd* src = iterr->second;
                assert(!bbInfo->out->has(dst));
                if (bbInfo->out->has(src))
                    (*(bbInfo->out))[dst] = (*(bbInfo->out))[src];
                else
                    (*(bbInfo->out))[dst] = src;
            }
            	
        }

#ifdef DEBUG_GLOBAL_PROP
        for (Nodes::const_reverse_iterator it=postOrdered.rbegin(),end=postOrdered.rend();it!=end;++it) {
            Node * node=*it;
            if (isMain)
                outputBBInfo(node, BBInfoList[node]);
        }
#endif
    }

#ifdef DEBUG_GLOBAL_PROP
    for (Nodes::const_reverse_iterator it=postOrdered.rbegin(),end=postOrdered.rend();it!=end;++it) {
        Node * node=*it;
        if (isMain)
            outputBBInfo(node, BBInfoList[node]);
    }
#endif

    /* Local propagation for the pairs in IN */
    for (Nodes::const_reverse_iterator it=postOrdered.rbegin(), end=postOrdered.rend();it!=end;++it) {
        Node * node=*it;
        if (!node->isBlockNode()) {
            continue;
        }
        BBInfo *bbInfo = BBInfoList[node];

        /*
         * Traverse all INST in a BB in preorder
         *    For all INST - several DEF, several USE 
         *        + Replace a USE with [USE, src] in IN
         *        + Delete [DEF, ...] and [..., DEF] from IN 
         *        + Add DEF to KILL
         */
        for (Inst * inst=(Inst*)node->getFirstInst();inst != NULL;inst=inst->getNextInst()) {
            const bool isCopy = inst->getMnemonic() == Mnemonic_MOV;
            Inst::Opnds opnds(inst, Inst::OpndRole_All);
  
            for (Inst::Opnds::iterator it=opnds.begin();it != opnds.end();it = opnds.next(it)) {
                Opnd * opnd=inst->getOpnd(it);
                U_32 roles=inst->getOpndRoles(it);
                
                if (roles & Inst::OpndRole_Use) {
                    if ((roles & Inst::OpndRole_All & Inst::OpndRole_FromEncoder) 
                        && (roles & Inst::OpndRole_All & Inst::OpndRole_ForIterator)
                        && (roles & Inst::OpndRole_Changeable) && ((roles & Inst::OpndRole_Def) == 0)
                        && bbInfo->in->has(opnd) && ((*(bbInfo->in))[opnd] != BOTTOM)) {
                        assert((*(bbInfo->in))[opnd] != BOTTOM);
                        if (opnd->getType()->isUnmanagedPtr() && (*(bbInfo->in))[opnd]->getType()->isInteger())
                            (*(bbInfo->in))[opnd]->setType(opnd->getType());
                        inst->setOpnd(it, (*(bbInfo->in))[opnd]);
                    }
                }
            }
            
            for (Inst::Opnds::iterator it = opnds.begin();it != opnds.end();it = opnds.next(it)) {
                Opnd * opnd=inst->getOpnd(it);
                U_32 roles=inst->getOpndRoles(it);

                if (roles & Inst::OpndRole_Def) {
                    if (bbInfo->in->has(opnd)) {
                        bbInfo->in->erase(opnd);
                    }

                    temp.clear();
                    for(StlHashMap<Opnd*, Opnd*>::iterator iter=bbInfo->in->begin();
                        iter!=bbInfo->in->end();++iter) {
                        if (iter->second == opnd) {
                            temp.insert(iter->first);
                        }
                    }
                    for(StlSet<Opnd*>::iterator iter=temp.begin();
                        iter!=temp.end();++iter) {
                        bbInfo->in->erase(*iter);
                    }
 
                    if (!bbInfo->kill->has(opnd))
                        bbInfo->kill->insert(opnd);

                }
            }

            /*
             * For MOV inst which could update the map - one DEF, one USE 
             *        + Try to add [DEF, USE] to IN
             *           - If [USE, s_old] exists in IN then add [DEF, s_old] too.
             *           - Otherwise add [DEF, USE] itself.
             */
            if (isCopy) {
                Inst::Opnds opnds(inst, Inst::OpndRole_All);
                Opnd * dst = NULL;
                Opnd * src = NULL;
                U_32 counterDef = 0;
                U_32 counterUse = 0;
                for (Inst::Opnds::iterator it=opnds.begin();it!=opnds.end();it=opnds.next(it)) {
                    Opnd * opnd = inst->getOpnd(it);
                    U_32 roles = inst->getOpndRoles(it);
                    
                    if (roles & Inst::OpndRole_Def) {
                        counterDef++;
                        dst = opnd;
                    } else if (roles & Inst::OpndRole_Use) {
                        counterUse++;
                        src = opnd;
                    }
                }

                if ((counterDef == 1) && (counterUse == 1) && (!dst->hasAssignedPhysicalLocation())) {
                    bool kindsAreOk = true;
                    if(src->canBePlacedIn(OpndKind_FPReg) || dst->canBePlacedIn(OpndKind_FPReg)) {
                        Constraint srcConstr = src->getConstraint(Opnd::ConstraintKind_Calculated);
                        Constraint dstConstr = dst->getConstraint(Opnd::ConstraintKind_Calculated);
                        kindsAreOk = ! (srcConstr&dstConstr).isNull();
                    }
                    bool typeConvOk = isTypeConversionAllowed(src, dst);
                    if (typeConvOk && kindsAreOk && ! src->isPlacedIn(OpndKind_Reg)) {
                        if (bbInfo->in->has(src)) {
                            /* The oldest source is selected heuristically. Some chances maybe missed afterward. */
                            (*(bbInfo->in))[dst] = (*(bbInfo->in))[src];
                        }
                        else {
                            (*(bbInfo->in))[dst] = src;
                        }
                    }
                }
            }
        }
    }
}


}}

