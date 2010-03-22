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
 * @author Intel, Natalya V. Golovleva
 *
 */

#include "lazyexceptionopt.h"
#include "FlowGraph.h"
#include "irmanager.h"
#include "Opnd.h"
#include "Inst.h"
#include "Stl.h"
#include "Log.h"
#include "Dominator.h"
#include "optimizer.h"

namespace Jitrino {

DEFINE_SESSION_ACTION(LazyExceptionOptPass, lazyexc, "Lazy Exception Throwing Optimization")

void LazyExceptionOptPass::_run(IRManager& irm) {
    LazyExceptionOpt le(irm);
    le.doLazyExceptionOpt();
}

int LazyExceptionOpt::level=0;

LazyExceptionOpt::LazyExceptionOpt(IRManager &ir_manager) :
    irManager(ir_manager), 
    leMemManager("LazyExceptionOpt::doLazyExceptionOpt"),
    compInterface(ir_manager.getCompilationInterface())
{
}

/**
 * Executes lazy exception optimization pass.
 */
void 
LazyExceptionOpt::doLazyExceptionOpt() {
    MethodDesc &md = irManager.getMethodDesc();
    BitSet excOpnds(leMemManager,irManager.getOpndManager().getNumSsaOpnds());
    StlDeque<Inst*> candidateSet(leMemManager);
    optCandidates = new (leMemManager) OptCandidates(leMemManager);
    Method_Side_Effects m_sideEff = md.getSideEffect(); 

    const Nodes& nodes = irManager.getFlowGraph().getNodes();
    Nodes::const_iterator niter;

#ifdef _DEBUG
    mtdDesc=&md;
#endif

#ifdef _DEBUG
    if (Log::isEnabled()) {
        Log::out() << std::endl;
        for (int i=0; i<level; i++) Log::out() << " "; 
        Log::out() << "doLE "; md.printFullName(Log::out()); 
        Log::out() << " SideEff " << (int)m_sideEff << std::endl; 
    }
#endif

    level++;
    U_32 opndId = 0;
    isArgCheckNull = false;
    isExceptionInit = md.isInstanceInitializer() && 
            md.getParentType()->isLikelyExceptionType();
//  core api exception init
    if (m_sideEff == MSE_Unknown && isExceptionInit 
            && strncmp(md.getParentType()->getName(),"java/lang/",10) == 0) {
        m_sideEff = MSE_False;
        md.setSideEffect(m_sideEff);
#ifdef _DEBUG
        if (Log::isEnabled()) {
            Log::out() << "      core api exc "; md.printFullName(Log::out()); 
            Log::out() << " SideEff " << (int)m_sideEff << std::endl;
        }
#endif
    }

    for(niter = nodes.begin(); niter != nodes.end(); ++niter) {
        Node* node = *niter;
        Inst *headInst = (Inst*)node->getFirstInst();
        for (Inst* inst=headInst->getNextInst();inst!=NULL;inst=inst->getNextInst()) {
#ifdef _DEBUG
            if (inst->getOpcode()==Op_DefArg && isExceptionInit) {
                if (Log::isEnabled()) {
                    Log::out() << "    defarg: "; 
                    inst->print(Log::out()); Log::out()  << std::endl;
                    Log::out() << "            "; 
                    Log::out() << (int)(inst->getDefArgModifier()) << " " <<
                    (inst->getDefArgModifier()==DefArgNoModifier) << " " <<
                    (inst->getDefArgModifier()==NonNullThisArg) << " " <<
                    (inst->getDefArgModifier()==SpecializedToExactType) << " " <<
                    (inst->getDefArgModifier()==DefArgBothModifiers) << std::endl;
                }
            }
#endif
            if (inst->getOpcode()==Op_Throw) {
                if (inst->getSrc(0)->getInst()->getOpcode()==Op_NewObj) {
                    excOpnds.setBit(opndId=inst->getSrc(0)->getId(),true);
                    if (!addOptCandidates(opndId,inst))
                        excOpnds.setBit(opndId,false); // different exc. edges
#ifdef _DEBUG
                    if (excOpnds.getBit(opndId)==1) {
                        if (Log::isEnabled()) {
                            Log::out() << "      add opnd: "; 
                            inst->print(Log::out()); Log::out() << std::endl; 
                            Log::out() << "      add  obj: "; 
                            inst->getSrc(0)->getInst()->print(Log::out()); Log::out() << std::endl; 
                        }
                    }
#endif
                }
            }
            if (m_sideEff == MSE_Unknown)
                if (instHasSideEffect(inst)) {
                    m_sideEff = MSE_True;
#ifdef _DEBUG
                    if (Log::isEnabled()) {
                        Log::out() << "~~~~~~inst sideEff "; 
                        inst->print(Log::out()); Log::out() << std::endl; 
                    }
#endif
                }
        }
    }
    if (md.getSideEffect() == MSE_Unknown) {
        if (m_sideEff == MSE_Unknown) {
            if (isExceptionInit && isArgCheckNull) {
#ifdef _DEBUG
                if (Log::isEnabled()) {
                    Log::out() << "~~~~~~init sideEff reset: " << m_sideEff << " 3 "; 
                    md.printFullName(Log::out()); Log::out() << std::endl; 
                }
#endif
                m_sideEff = MSE_True_Null_Param;
            } else
                m_sideEff = MSE_False;
        }
        md.setSideEffect(m_sideEff);
    } 

    for(niter = nodes.begin(); niter != nodes.end(); ++niter) {
        Node* node = *niter;
        Inst *headInst = (Inst*)node->getFirstInst();
        Opnd* opnd;
        for (Inst* inst=headInst->getNextInst();inst!=NULL;inst=inst->getNextInst()) {
            U_32 nsrc = inst->getNumSrcOperands();
            for (U_32 i=0; i<nsrc; i++) {
                if (!(opnd=inst->getSrc(i))->isSsaOpnd())  // check ssa operands
                    continue;
                if (excOpnds.getBit(opndId=opnd->getId())==0) 
                    continue;
                if (inst->getOpcode()==Op_DirectCall) {
                    MethodDesc* md = inst->asMethodInst()->getMethodDesc();
                    if (md->isInstanceInitializer() &&
                        md->getParentType()->isLikelyExceptionType()) {
                        if (!addOptCandidates(opndId,inst)) {
                            excOpnds.setBit(opndId,false);
#ifdef _DEBUG
                            if (Log::isEnabled()) {
                                Log::out() << "    - rem opnd " << opnd->getId() << " "; 
                                inst->print(Log::out()); Log::out() << std::endl; 
                            }
#endif
                        } 
                    } else {
                        excOpnds.setBit(opndId,false);
#ifdef _DEBUG
                        if (Log::isEnabled()) {
                            Log::out() << "   -- rem opnd " << opnd->getId() << " "; 
                            inst->print(Log::out()); Log::out() << std::endl; 
                        }
#endif
                    }
                } else {
                    if (inst->getOpcode()!=Op_Throw) {
                        excOpnds.setBit(opndId,false);
#ifdef _DEBUG
                        if (Log::isEnabled()) {
                            Log::out() << "      rem opnd " << opnd->getId() << " "; 
                            inst->print(Log::out()); Log::out() << std::endl; 
                        }
#endif
                    }
                }
            }
        }
    }
    if (!excOpnds.isEmpty()) {
#ifdef _DEBUG
        if (Log::isEnabled()) {
            Log::out() << "------LE: "; 
            md.printFullName(Log::out()); Log::out() << std::endl;
        }
#endif
        fixOptCandidates(&excOpnds);
    }

level--;
#ifdef _DEBUG
    if (Log::isEnabled()) {
        for (int i=0; i<level; i++) Log::out() << " "; 
        Log::out() << "done "; md.printFullName(Log::out()); 
        Log::out() << " SideEff " << (int)m_sideEff << std::endl; 
    }
#endif
};

/**
 * Adds information to optCandidates list for specified exception object.
 * @param id - an exception object operand Id
 * @param inst - call, or throw instructions operating with this exception object
 * @return <code>true</code> if an information is added; 
 *         <code>false<code> if an exception object cannot be optimized.
 */
bool 
LazyExceptionOpt::addOptCandidates(U_32 id, Inst* inst) {
    OptCandidate* oc = NULL;
    ThrowInsts* thrinst = NULL;
    OptCandidates::iterator it;
    if (optCandidates == NULL)
        optCandidates = new (leMemManager) OptCandidates(leMemManager);
    for (it = optCandidates->begin( ); it != optCandidates->end( ); it++ ) {
        if ((*it)->opndId==id) {
            oc = *it;
            break;
        }
    }
#ifdef _DEBUG
    if (Log::isEnabled()) {
        Log::out() << "    addOptCandidates: "; 
        inst->print(Log::out()); Log::out()  << std::endl;
    }
#endif
    if (oc == NULL) {
        if (inst->getOpcode()==Op_Throw) {
            bool hasFinalize =
                ((NamedType*)inst->getSrc(0)->getType())->isFinalizable();
            if (hasFinalize) {
#ifdef _DEBUG
                Log::out() << "    isFinalizable: "
                 << hasFinalize << std::endl;
#endif
                return false;
            }
        }
        oc = new (leMemManager) OptCandidate;
        oc->opndId = id;
        oc->objInst = inst->getSrc(0)->getInst();
        oc->initInst=NULL;
        thrinst = new (leMemManager) ThrowInsts(leMemManager);
        thrinst->push_back(inst);
        oc->throwInsts = thrinst;
        optCandidates->push_back(oc);
        if (!isEqualExceptionNodes(oc->objInst,inst)) {
            return false;
        }
    } else {
        if (inst->getOpcode()==Op_Throw) {
            oc->throwInsts->push_back(inst);
            return true;
        } else {
            assert(inst->getOpcode()==Op_DirectCall);
            assert(oc->initInst==NULL);
            oc->initInst = inst;
#ifdef _DEBUG
            if (Log::isEnabled()) {
                Log::out() << "    addOptCandidates: call checkMC "; 
                inst->print(Log::out()); Log::out()  << std::endl;
            }
#endif
            U_32 nii_id=((Inst*)(inst->getNode()->getUnconditionalEdgeTarget()->getFirstInst()))->getId();
            ThrowInsts::iterator it1;
            for (it1 = oc->throwInsts->begin(); it1 !=oc->throwInsts->end(); it1++) {
                if ((*it1)->getId() != nii_id) {
#ifdef _DEBUG
                    if (Log::isEnabled()) {
                        Log::out() << "??  addOptCandidates: throw "; 
                        (*it1)->print(Log::out()); Log::out()  << std::endl;
                    }
#endif
                    if (checkInSideEff((*it1),inst))
                        return false;
                }
            }
            if (methodCallHasSideEffect(inst)) {
                return false;
            }
        }
    }
    return true;
};

/**
 * Checks if there is a side effect between throw_inst and init_inst instructions.
 * @param throw_inst - the exception object throw instruction
 * @param init_inst - the exception object constructor call instruction
 * @return <code>true</code> if there is side effect;
 *         <code>false<code> if there is no side effect.
 */
bool 
LazyExceptionOpt::checkInSideEff(Inst* throw_inst, Inst* init_inst) {
    Node* node = throw_inst->getNode();
    Inst* instfirst = (Inst*)node->getFirstInst();;
    Inst* instlast = throw_inst;
    Inst* inst;
    bool dofind = true;
    bool inSE = false;
    if (throw_inst!=instfirst)
        instlast=throw_inst->getPrevInst();
    else {
        node = node->getInEdges().front()->getSourceNode();
        instlast = (Inst*)node->getLastInst();    
    } 
    while (dofind && node!=NULL) {
        instfirst = (Inst*)node->getFirstInst();
        for (inst = instlast; inst!=instfirst; inst=inst->getPrevInst()) {
#ifdef _DEBUG
            if (Log::isEnabled()) {
                Log::out() << "      checkInSE: see "; 
                inst->print(Log::out()); Log::out() << std::endl; 
            }
#endif
            if (inst==init_inst) {
                dofind=false;
                break;
            }
            if (!inSE) {
                if (instHasSideEffect(inst)) {
                    inSE=true;
#ifdef _DEBUG
                    if (Log::isEnabled()) {
                        Log::out() << "      checkInSE: sideEff "; 
                        inst->print(Log::out()); Log::out() << std::endl; 
                    }
#endif
                    break;
                }
            }
        }
        if (dofind){
            node = node->getInEdges().front()->getSourceNode();
            instlast = (Inst*)node->getLastInst();
        }
    }
    if (dofind)
        return true; // call init wasn't found
    return inSE;
}

/**
 * Checks that exception edges are equal for newobj instruction node and
 * throw instruction node.
 * @param oi - newobj instruction
 * @param ti - throw instruction
 * @return <code>true</code> if exception edges are equal;
 *         <code>false<code> otherwise.
 */
bool
LazyExceptionOpt::isEqualExceptionNodes(Inst* oi, Inst* ti) {
    Edge* oedge = oi->getNode()->getExceptionEdge();
    Edge* tedge = ti->getNode()->getExceptionEdge();
    if (oedge->getTargetNode()!=tedge->getTargetNode()) {
#ifdef _DEBUG
        if (Log::isEnabled()) {
            Log::out() << "    addOptCandidates: diff.exc.edges for obj&throw "; 
            Log::out() << oedge->getTargetNode()->getId() << "  ";
            Log::out() << tedge->getTargetNode()->getId() << std::endl;
        }
#endif
        return false;
    }
    return true;
}

/**
 * Prints information about optimization candidates.
 * @param os - output stream
 */
void 
LazyExceptionOpt::printOptCandidates(::std::ostream& os) {
    OptCandidates::iterator it;
    Inst* oinst;
    Inst* iinst;
    Inst* tinst;

    if (optCandidates == NULL) {
        return;
    }
    for (it = optCandidates->begin( ); it != optCandidates->end( ); it++ ) {
        os << "~~  opndId " << (*it)->opndId << std::endl;
        oinst = (*it)->objInst; 
        os << "  obj       ";
        if (oinst != NULL)
            oinst->print(os);
        else
            os << "newobj NULL";
        os << std::endl;
        iinst = (*it)->initInst; 
        os << "  init      ";
        if (iinst != NULL)
            iinst->print(os);
        else
            os << "call init NULL";
        os << std::endl;
        if ((*it)->throwInsts == NULL) {
            os << "  thr        throw NULL";
            os << std::endl;
            continue;
        }
        ThrowInsts::iterator it1;
        for (it1 = (*it)->throwInsts->begin(); it1 !=(*it)->throwInsts->end(); it1++) {
            tinst = *it1;
            assert(tinst != NULL);
            os << "  thr       ";
            tinst->print(os);
            os << std::endl;
        }
    }
    os << "end~~" << std::endl;
}

/**
 * Checks that exception edges are equal for newobj instruction node and
 * throw instruction node.
 * @param bs - bit set of operands that may be optimized
 */
void 
LazyExceptionOpt::fixOptCandidates(BitSet* bs) {
    OptCandidates::iterator it;
    Inst* oinst;
    MethodCallInst* iinst;
    Inst* tinst;
    Inst* tlinst;
    U_32 opcount;
    Opnd **opnds = NULL;

    if (optCandidates == NULL) {
        return;
    }
    for (it = optCandidates->begin( ); it != optCandidates->end( ); it++ ) {
        if (bs->getBit((*it)->opndId)) {
            oinst = (*it)->objInst; 
            assert(oinst != NULL);
#ifdef _DEBUG
            if (Log::isEnabled()) {
                Log::out() << "  to remove ";
                oinst->print(Log::out());
                Log::out() << std::endl;
            }
#endif
            if ((*it)->initInst == NULL) {
#ifdef _DEBUG
                if (Log::isEnabled()) {
                    Log::out() << "  init inst is null ";
                    Log::out() << std::endl;
                }
#endif
                continue;
            }
            iinst = (*it)->initInst->asMethodCallInst(); 
            // inline info from constructor should be propagated to lazy
            // exception if any
#ifdef _DEBUG
            if (Log::isEnabled()) {
                Log::out() << "  to remove ";
                iinst->print(Log::out());
                Log::out() << std::endl;
            }
#endif
            assert((*it)->throwInsts != NULL);
            assert(iinst->getNumSrcOperands() >= 3);
            if (!removeInsts(oinst,iinst))
                continue;   // to null bitset?
            TypeManager& tm = irManager.getTypeManager();
            Opnd* mpt = irManager.getOpndManager().createSsaTmpOpnd(
                        tm.getMethodPtrType(iinst->getMethodDesc()));
            opcount = iinst->getNumSrcOperands()-2;  //numSrc-3+1 
            if (opcount >0) {
                opnds = new (leMemManager) Opnd*[opcount];   //local mem should be used
                opnds[0] = mpt;
                for (U_32 i = 0; i < opcount-1; i++)
                    opnds[i+1] = iinst->getSrc(i+3);
            }
            Inst* mptinst = irManager.getInstFactory().makeLdFunAddr(mpt,iinst->getMethodDesc());
#ifdef _DEBUG
            if (Log::isEnabled()) {
                Log::out() << "  1st      ";
                mptinst->print(Log::out());
                Log::out() << std::endl;
            }
#endif
            ThrowInsts::iterator it1;
            for (it1 = (*it)->throwInsts->begin(); it1 !=(*it)->throwInsts->end(); it1++) {
                tinst = *it1;
                assert(tinst != NULL);
                tlinst=irManager.getInstFactory().makeVMHelperCall(  
                        OpndManager::getNullOpnd(), VM_RT_THROW_LAZY, opcount, opnds);
#ifdef _DEBUG
                if (Log::isEnabled()) {
                    Log::out() << "  2nd      ";
                    tlinst->print(Log::out());
                    Log::out() << std::endl;
                }
                if (Log::isEnabled()) {
                    Log::out() << "  to change ";
                    tinst->print(Log::out());
                    Log::out() << std::endl;
                }
#endif
                mptinst->insertBefore(tinst); 
                tlinst->insertBefore(tinst);
                tinst->unlink();

                uint16 bcOffset = iinst->getBCOffset();
                mptinst->setBCOffset(bcOffset);
                tlinst->setBCOffset(bcOffset);
            }
        }
    }
}

/**
 * Removes specified instructions if they have the same exception node.
 * @param oinst - exception creating instruction
 * @param iinst - constructor call instruction
 * @return <code>true</code> if instruction were removed;
 *         <code>false<code> otherwise.
 */
bool
LazyExceptionOpt::removeInsts(Inst* oinst,Inst* iinst) {
    ControlFlowGraph& fg = irManager.getFlowGraph();
    Edge* oedge = oinst->getNode()->getExceptionEdge();
    Edge* iedge = iinst->getNode()->getExceptionEdge();
    Node* otn = oedge->getTargetNode();
    Node* itn = iedge->getTargetNode();

    if (otn!=itn) {
#ifdef _DEBUG
        if (Log::isEnabled()) {
            Log::out() << "    removeInsts: diff.exc.edges for obj&init "; 
            Log::out() << otn->getId() << "  "  << itn->getId() << std::endl;
            Log::out() << "   "; oinst->print(Log::out()); 
            Log::out() << std::endl;
            Log::out() << "   "; iinst->print(Log::out()); 
            Log::out() << std::endl;
        }
#endif
        return false;
    }
    oinst->unlink();
    iinst->unlink();
    if (otn->getInEdges().size() > 1) {
        fg.removeEdge(oedge);
    } else
        removeNode(otn);
    if (itn->getInEdges().size() > 1) {
        fg.removeEdge(iedge);
    } else
        removeNode(itn);
    return true;
}

/**
 * Removes node from compiled method flow graph.
 * @param node - removed node
 */
void 
LazyExceptionOpt::removeNode(Node* node) {
    const Edges &out_edges = node->getOutEdges();
    Edges::const_iterator eit;
    Node* n; 
    for (eit = out_edges.begin(); eit != out_edges.end(); ++eit) {
        n = (*eit)->getTargetNode();
        if (n->getInEdges().size() == 1)
            removeNode(n);
    }
    irManager.getFlowGraph().removeNode(node);
}

void 
LazyExceptionOpt::printInst1(::std::ostream& os, Inst* inst, std::string txt) {
    U_32 nsrc = inst->getNumSrcOperands();
    os << txt;
    inst->print(os);
    os << std::endl;
    for (U_32 i=0; i<nsrc; i++) {
        printInst1(os, inst->getSrc(i)->getInst(),txt+"  ");
    }

}

/**
 * Checks a callee method side effect.
 * @param inst - method call instruction
 * @return <code>true</code> if method has side effect;
 *         <code>false<code> if method has no side effect.
 */
bool 
LazyExceptionOpt::methodCallHasSideEffect(Inst* inst) {
    U_32 opcode = inst->getOpcode();
    MethodDesc* cmd = NULL;
    Method_Side_Effects mse = MSE_Unknown;

    if (opcode==Op_DirectCall || opcode==Op_TauVirtualCall) {
        cmd = inst->asMethodCallInst()->getMethodDesc();
    } else {
        if (opcode==Op_IndirectCall || opcode==Op_IndirectMemoryCall) {
            Type* type = inst->asCallInst()->getFunPtr()->getType();
            if (type->isUnresolvedType()) {
                return true;
            }
            cmd = type->asMethodPtrType()->getMethodDesc();
        } else {
#ifdef _DEBUG
            if (Log::isEnabled()) {
                Log::out() << "    checkMC: no check "; 
                inst->print(Log::out()); Log::out()  << std::endl;
            }
#endif
            return true;
        }
    }
#ifdef _DEBUG
    if (Log::isEnabled()) {
        Log::out() << "    checkMC: "; 
        cmd->printFullName(Log::out()); Log::out() << std::endl;
    }
#endif
    
    mse = cmd->getSideEffect();
#ifdef _DEBUG
    if (mse != MSE_Unknown) {
        if (Log::isEnabled()) {
            Log::out() << "    checkMC: prev.set sideEff " << mse << "  "; 
            inst->print(Log::out()); Log::out() << std::endl;
        }
    }
#endif
    if (mse == MSE_True) {
        return true;
    }
    if (mse == MSE_False) {
        return false;
    }
//  core api exception init
    if (cmd->isInstanceInitializer() && cmd->getParentType()->isLikelyExceptionType()
            && strncmp(cmd->getParentType()->getName(),"java/lang/",10) == 0) {
        cmd->setSideEffect(MSE_False);
#ifdef _DEBUG
        if (Log::isEnabled()) {
            Log::out() << "    checkMC: core api exc "; 
            inst->print(Log::out()); Log::out() << std::endl;
        }
#endif
        return false;
    }

    if ( opcode!=Op_DirectCall && !cmd->isFinal() ) {
#ifdef _DEBUG
        if (Log::isEnabled()) {
            Log::out() << "    checkMC: not DirCall not final "; 
            inst->print(Log::out()); Log::out() << std::endl;
        }
#endif
        return true;
    }

    if (!isExceptionInit && 
        !(cmd->isInstanceInitializer()&&cmd->getParentType()->isLikelyExceptionType())) {
#ifdef _DEBUG
        if (Log::isEnabled()) {
            Log::out() << "    checkMC: no init "; 
            Log::out() << isExceptionInit << " ";
            Log::out() << cmd->isInstanceInitializer() << " ";
            Log::out() << cmd->getParentType()->isLikelyExceptionType() << " ";
            inst->print(Log::out()); Log::out() << std::endl;
        }
#endif
        return true;
    }

/*
    if (cmd->getParentType()->needsInitialization()) {
#ifdef _DEBUG
        if (Log::isEnabled()) {
            Log::out() << "    checkMC: need cinit "; 
            inst->print(Log::out()); Log::out() << std::endl;
        }
#endif
        return true;  // cannot compile <init> before <clinit> (to fix vm)
    }
*/

    if (mse == MSE_Unknown) {  // try to compile method
        //TODO: avoid compilation here. Use translator to perform analysis needed
        bool allowRecursion = !compInterface.getTypeManager().isLazyResolutionMode();
        if (!allowRecursion) {
            return MSE_True;
        }
        if (!compInterface.compileMethod(cmd)) {
#ifdef _DEBUG
            if (Log::isEnabled()) {
                Log::out() << "    checkMC: method was not compiled " << std::endl;
            }
#endif
            return true;
        } else {
             mse = cmd->getSideEffect();
#ifdef _DEBUG
            if (Log::isEnabled()) {
                Log::out() << "    checkMC: method was compiled, sideEff " 
                    << mse << std::endl;
            }
#endif
            if (mse == MSE_True)
                return true;
            if (mse == MSE_False) {
                return false;
            }
       }
    }

    if (mse == MSE_True_Null_Param) {
        U_32 nsrc=inst->getNumSrcOperands();
        bool mayBeNull;
        if (nsrc>3) {
#ifdef _DEBUG
            if (Log::isEnabled()) {
                Log::out() << "    checkMC: exc.init "; 
                inst->print(Log::out()); Log::out() << std::endl;
            }
#endif
            mayBeNull=false;
            for (U_32 i=3; i<nsrc; i++) {
                if (inst->getSrc(i)->getType()->isReference()) {
                    if (mayBeNullArg(inst,i))
                        mayBeNull=true;
                }
            }
            if (!mayBeNull)
                return false;
#ifdef _DEBUG
            for (U_32 i=0; i<nsrc; i++) {
                if (Log::isEnabled()) {
                    Log::out() << "        "<<i<<" isRef: "<<
                    inst->getSrc(i)->getType()->isReference()<<" "; 
                    inst->getSrc(i)->getInst()->print(Log::out()); 
                    Log::out() << std::endl;
                }
            }
#endif
            return true;
        } 
#ifdef _DEBUG
        else {
            if (Log::isEnabled()) {
                Log::out() << " ?????? MSE_NULL_PARAM & nsrc "<<
                nsrc << std::endl;
            }
        }
#endif
    }
    return true;
}

/**
 * Checks if a callee method agrument may be null.
 * @param call_inst - method call instruction
 * @param arg_n - callee method argument number
 * @return <code>true</code> if a callee method argument may be null
 *         <code>false<code> if a callee method argument is not null
 */
bool 
LazyExceptionOpt::mayBeNullArg(Inst* call_inst, U_32 arg_n) {
    Opnd* arg_opnd = call_inst->getSrc(arg_n);
    Inst* inst=arg_opnd->getInst();

    while ((inst=inst->getNextInst())!=NULL) {
        if (inst->getOpcode()==Op_TauCheckNull && inst->getSrc(0)==arg_opnd) {
#ifdef _DEBUG
            if (Log::isEnabled()) {
                Log::out() << "   check node: " << inst->getNode()->getId() << " ";
                inst->print(Log::out()); Log::out() << std::endl; 
            }
#endif 
            return false; // may not be null
        }
    }
#ifdef _DEBUG
    if (Log::isEnabled()) {
        Log::out() << "   chknull wasn,t found in node: "
            << arg_opnd->getInst()->getId()<< std::endl; 
    }
#endif 
    return true;  // may be null
}

/**
 * Checks if Op_TauStInd (stind) instruction has a side effect.
 * @param inst - checked instruction
 * @return <code>true</code> if an instruction has side effect;
 *         <code>false<code> if an instruction has no side effect.
 */
bool
LazyExceptionOpt::fieldUsageHasSideEffect(Inst* inst) {
    Opnd* insOp = inst->getSrc(0);
    Inst* instDef = insOp->getInst();
    if (instDef->getOpcode() == Op_DefArg) {
#ifdef _DEBUG
        if (Log::isEnabled()) {
            Log::out() << "    fieldUsageHasSideEffect: "; 
            inst->print(Log::out()); Log::out()  << std::endl;
            Log::out() << "    fieldUsageHasSideEffect: "; 
            instDef->print(Log::out()); Log::out()  << std::endl;
            Log::out() << "    fieldUsageHasSideEffect: "; 
            Log::out() << (int)(instDef->getDefArgModifier()) << " " <<
            (instDef->getDefArgModifier()==DefArgNoModifier) << " " <<
            (instDef->getDefArgModifier()==NonNullThisArg) << " " <<
            (instDef->getDefArgModifier()==DefArgBothModifiers) << std::endl;
        }
#endif
        if (instDef->getDefArgModifier()==NonNullThisArg && isExceptionInit)
            return false;
    }
    return true;
}

/**
 * Checks if there is a side effect between throw_inst and init_inst instructions.
 * @param inst - checked instruction
 * @return <code>true</code> if an instruction has side effect;
 *         <code>false<code> if an instruction has no side effect.
 */
bool 
LazyExceptionOpt::instHasSideEffect(Inst* inst) {
    switch (inst->getOpcode()) {
        case Op_Add:
        case Op_Mul:
        case Op_Sub:
        case Op_TauDiv:
        case Op_TauRem:
        case Op_Neg:
        case Op_MulHi:
        case Op_Min:
        case Op_Max:
        case Op_Abs:
        case Op_And:
        case Op_Or:
        case Op_Xor:
        case Op_Not:
        case Op_Select:
            return false;
        case Op_Conv:
        case Op_ConvZE:
        case Op_ConvUnmanaged:
            return true;
        case Op_Shladd:
        case Op_Shl:
        case Op_Shr:
        case Op_Cmp:
        case Op_Cmp3:
        case Op_Branch:
        case Op_Jump:
        case Op_Switch:
            return false;
        case Op_DirectCall:
        case Op_TauVirtualCall:
        case Op_IndirectCall:
        case Op_IndirectMemoryCall:
#ifdef _DEBUG
            if (Log::isEnabled()) {
                Log::out() << "    instHasSideEffect: call checkMC "; 
                inst->print(Log::out()); Log::out()  << std::endl;
            }
#endif
            return methodCallHasSideEffect(inst);  
        case Op_JitHelperCall:
        case Op_VMHelperCall:
            return true;
        case Op_Return:
        case Op_Catch:
            return false;
        case Op_PseudoThrow:
            return false;
        case Op_Throw:
        case Op_ThrowSystemException:
        case Op_ThrowLinkingException:
            return true;
        case Op_JSR:              // deleted
        case Op_Ret:
        case Op_SaveRet:
            return true;
        case Op_Copy:
            return true;
        case Op_DefArg:
        case Op_LdConstant:
        case Op_LdRef:
        case Op_LdVar:    
        case Op_LdVarAddr:
        case Op_TauLdInd:
            return false;
        case Op_TauLdField:
            return true; 
        case Op_LdStatic:
            return true;
        case Op_TauLdElem:
             return false;
        case Op_LdFieldAddr: 
             return false;
        case Op_LdStaticAddr:
            return true;
        case Op_LdElemAddr:
            return false; //
        case Op_TauLdVTableAddr:
        case Op_TauLdIntfcVTableAddr:
        case Op_TauLdVirtFunAddr:
        case Op_TauLdVirtFunAddrSlot:
        case Op_LdFunAddr:
        case Op_LdFunAddrSlot:
        case Op_GetVTableAddr:
        case Op_GetClassObj:
            return false;
        case Op_TauArrayLen:
        case Op_LdArrayBaseAddr:
        case Op_AddScaledIndex:
            return true;
        case Op_StVar:
            return false;
        case Op_TauStInd:
            {
                Inst* inst_src1 = inst->getSrc(1)->getInst();
#ifdef _DEBUG
                if (Log::isEnabled()) {
                    Log::out() << "    stind: "; 
                    inst->print(Log::out()); Log::out()  << std::endl;
                    Log::out() << "           "; 
                    inst_src1->print(Log::out()); Log::out()  << std::endl;
                }
#endif
                if (inst_src1->getOpcode()==Op_LdFieldAddr ) 
                    return fieldUsageHasSideEffect(inst_src1);
            }
            return true;
        case Op_TauStRef:
            {
                Inst* inst_src1 = inst->getSrc(1)->getInst();
#ifdef _DEBUG
                if (Log::isEnabled()) {
                    Log::out() << "    stref: "; 
                    inst->print(Log::out()); Log::out()  << std::endl;
                    Log::out() << "           "; 
                    inst_src1->print(Log::out()); Log::out()  << std::endl;
                }
#endif
                if (inst_src1->getOpcode()==Op_LdFieldAddr ) 
                    return fieldUsageHasSideEffect(inst_src1);
            }
            return true;
        case Op_TauStField:
            return true; // 
        case Op_TauStElem:
        case Op_TauStStatic:
        case Op_TauCheckBounds:
        case Op_TauCheckLowerBound:
        case Op_TauCheckUpperBound:
            return true;
        case Op_TauCheckNull:
            {
                Inst* inst_src = inst->getSrc(0)->getInst();
#ifdef _DEBUG
                if (Log::isEnabled()) {
                    Log::out() << "    chknull: "; 
                    inst->print(Log::out()); Log::out()  << std::endl;
                    Log::out() << "               "; 
                    inst_src->print(Log::out()); Log::out()  << std::endl;
                }
#endif
                if (inst_src->getOpcode()==Op_DefArg && isExceptionInit) {
                    isArgCheckNull = true;
                    return false;
                }
            }
            return true; // 
        case Op_TauCheckZero:
        case Op_TauCheckDivOpnds:
        case Op_TauCheckElemType:
        case Op_TauCheckFinite:
            return true;
        case Op_NewObj:
// core api
            {
                NamedType* nt = inst->getDst()->getType()->asNamedType();
                if (strncmp(nt->getName(),"java/lang/",10)==0 && nt->isLikelyExceptionType()) {
#ifdef _DEBUG
                    if (Log::isEnabled()) {
                        Log::out() << "====newobj "; 
                        inst->print(Log::out()); Log::out()  << std::endl;
                        Log::out() << "core api exc " << nt->getName() << " "
                            << strncmp(nt->getName(),"java/lang/",10)
                            << " excType: " << nt->isLikelyExceptionType() << std::endl; 
                    }
#endif
                    return false;
                }
            }
            return true;
        case Op_NewArray:
        case Op_NewMultiArray:
        case Op_TauMonitorEnter:
        case Op_TauMonitorExit:
        case Op_TypeMonitorEnter:
        case Op_TypeMonitorExit:
        case Op_LdLockAddr:
        case Op_IncRecCount:
        case Op_TauBalancedMonitorEnter:
        case Op_BalancedMonitorExit:
        case Op_TauOptimisticBalancedMonitorEnter:
        case Op_OptimisticBalancedMonitorExit:
        case Op_MonitorEnterFence:
        case Op_MonitorExitFence:
            return true;
        case Op_TauStaticCast:
        case Op_TauCast:
        case Op_TauAsType:
        case Op_TauInstanceOf:
        case Op_InitType:
            return true;
        case Op_Label:
        case Op_MethodEntry:
        case Op_MethodEnd:
            return false;                
        case Op_Phi:
        case Op_TauPi:
            return false;
        case Op_IncCounter:
        case Op_Prefetch:
            return false;
        case Op_UncompressRef:
        case Op_CompressRef:
        case Op_LdFieldOffset:
        case Op_LdFieldOffsetPlusHeapbase:
        case Op_LdArrayBaseOffset:
        case Op_LdArrayBaseOffsetPlusHeapbase:
        case Op_LdArrayLenOffset:
        case Op_LdArrayLenOffsetPlusHeapbase:
        case Op_AddOffset:
        case Op_AddOffsetPlusHeapbase:
            return true;
        case Op_TauPoint:
        case Op_TauEdge:
        case Op_TauAnd:
        case Op_TauUnsafe:
        case Op_TauSafe:
            return false;
        case Op_TauCheckCast:
            return true;
        case Op_TauHasType:
        case Op_TauHasExactType:
        case Op_TauIsNonNull:
            return false;
        default:
            return true;
    }
}

} //namespace Jitrino






