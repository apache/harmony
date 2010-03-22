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

#include "codelowerer.h"
#include "irmanager.h"
#include "globalopndanalyzer.h"
#include "constantfolder.h"
#include "optimizer.h"
#include "Log.h"
#include "FlowGraph.h"
#include "PMFAction.h"

namespace Jitrino {

DEFINE_SESSION_ACTION(CodeLoweringPass, lower, "Code Lowering / Fast Path Inlining");

void CodeLoweringPass::_run(IRManager& irm){
    CodeLowerer lowerer(irm);
    lowerer.doLower();
}

void
CodeLowerer::doLower() {
    _preserveSsa = _irm.getInSsa();

    GlobalOpndAnalyzer globalOpndAnalyzer(_irm);
    globalOpndAnalyzer.doAnalysis();

    MemoryManager mm("CodeLowerer.doLower");

    Nodes nodes(mm);
    _irm.getFlowGraph().getNodesPostOrder(nodes);
    
    for(Nodes::reverse_iterator i = nodes.rbegin(); i != nodes.rend(); ++i) {
        Node *_currentBlock = *i;
        if(_currentBlock->isBlockNode())
            lowerBlock(_currentBlock);
    }
}

void
CodeLowerer::lowerBlock(Node *block) {
    assert(block->isBlockNode());
    for(Inst* inst = (Inst*)block->getFirstInst(); inst != NULL; inst = inst->getNextInst()) {
        inst = optimizeInst(inst);
        if (inst == NULL) {
            break;
        }
    }
}

Inst*
CodeLowerer::caseTauCast(TypeInst* inst)
{
    Node* block = inst->getNode();
    //
    // Perform null and vtable checks to avoid an expensive vm call.
    // The vtable check often succeeds in practice.
    //

    //
    // Skip if non-concrete type.
    //
    assert(inst != NULL);
    Type* type = inst->getTypeInfo();

    //
    // dst = cast src, castType, tauNullChecked
    //
    ObjectType* castType = (ObjectType*) type;
    Opnd* src = inst->getSrc(0);
    Opnd* tauNullChecked = inst->getSrc(1);
    assert(tauNullChecked->getType()->tag == Type::Tau);
    Opnd* dst = inst->getDst();
    
    if (tauNullChecked->getInst()->getOpcode() != Op_TauUnsafe) {
        // we must have a preceding null check which would eliminate any succeeding one
        assert(0); // should have been lowered
        return 0;
    }

    //
    // Target is _succ_ if cast succeeds and _fail_ if cast fails.
    // 
    Node* succ = block->getUnconditionalEdge()->getTargetNode();
    Node* fail = block->getExceptionEdge()->getTargetNode();
    assert(succ->isBlockNode() && fail->isDispatchNode());

    ControlFlowGraph& fg = _irm.getFlowGraph();
    InstFactory& instFactory = _irm.getInstFactory();
    OpndManager& opndManager = _irm.getOpndManager();
    TypeManager& typeManager = _irm.getTypeManager();

    Inst* succInst = (Inst*)succ->getSecondInst();
    if(succInst->getOpcode() == Op_TauCheckNull) {
        //
        // We have
        //    dst = cast src, castType   ---> fail
        //  succ:
        //    tauDst2 = chknull dst          ---> fail2
        //  succ2:
        // Transform it to
        //    tauDst2 = chknull src          ---> fail2
        //  succ:
        //    dst = cast src, castType, tauDst2   ---> fail
        //  succ2:
        assert(succInst == succ->getLastInst());
        Node* fail2 =  succ->getExceptionEdge()->getTargetNode();
        Opnd* src2 = succInst->getSrc(0);
        if(succ->getInDegree() == 1 && fail2 == fail && src2 == dst) {
            // Reorder checknull and checkcast.
            Opnd* tauDst2 = succInst->getDst();

            assert(inst->getSrc(1)->getType()->tag == Type::Tau);
            inst->setSrc(1, tauDst2);

            inst->unlink();
            succInst->unlink();

            block->appendInst(succInst);
            succ->appendInst(inst);

            succInst->setSrc(0, src);
            return succInst;
        }
    }

    // note that a test above guarantees that tauNullChecked is tauUnsafe here

    //
    // Perform the following lowering:
    //
    // Replace:
    //   block:
    //     ...
    //     dst = cast src, (castType), tauNullChecked ---> fail
    //   succ: 
    //     ...
    //
    // with
    //   block:
    //     ...
    //     if cz src goto b3b
    //   b1:
    //     tauNullChecked = tauEdge
    //     vt1 = ldVTableAddr src, tauNullChecked
    //     vt2 = getVTableAddr castType 
    //     if ceq  vt1, vt2 goto b3c
    //   b2:
    //     tauCheckedCast = checkcast src, (castType) ---> fail
    //   b3a:
    //     stvar tauvar1, tauCheckedCast
    //     goto b3
    //   b3b:
    //     tau3 = tauEdge
    //     stvar tauvar2, tau3
    //     goto b3
    //   b3c:
    //     tau4 = tauEdge
    //     stvar tauvar3, tau4
    //     goto b3
    //   b3:
    //     tauvar4 = phi(tauvar1, tauvar2, tauvar3)
    //     tau2 = ldvar tauvar4
    //     dst = staticcast src, tau2, (castType)
    //   succ:
    //     ...

    Node* b1 = fg.createBlockNode(instFactory.makeLabel());
    Node* b2 = fg.createBlockNode(instFactory.makeLabel());
    Node* b3 = fg.createBlockNode(instFactory.makeLabel());
    Node* b3a = fg.createBlockNode(instFactory.makeLabel());
    Node* b3b = fg.createBlockNode(instFactory.makeLabel());
    Node* b3c = 0;

    //
    // Add null check to _block_.
    //
    fg.removeEdge(block, fail);
    fg.removeEdge(block, succ);
    inst->unlink();    
    Inst* test1 = instFactory.makeBranch(Cmp_Zero, src->getType()->tag,
                                         src,
                                         (LabelInst*)b3b->getFirstInst());
    block->appendInst(test1);
    fg.addEdge(block, b1);
    fg.addEdge(block, b3b);
    
    // create a new tau for null-checked
    tauNullChecked = opndManager.createSsaTmpOpnd(typeManager.getTauType());
    b1->appendInst(instFactory.makeTauEdge(tauNullChecked));

    //
    // Test direct vtable equality in _b1_ if cast target type is concrete.
    //
    if(!((type->isAbstract() && !type->isArray()) || type->isInterface())) {
        assert(type->isObject());
        assert(src->getType()->isObject());
        ObjectType* srcType = (ObjectType*) src->getType();
        Opnd* vt1 = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(srcType));
        Opnd* vt2 = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(castType));
        b1->appendInst(instFactory.makeTauLdVTableAddr(vt1, src, tauNullChecked));
        b1->appendInst(instFactory.makeGetVTableAddr(vt2, castType));
        b3c = fg.createBlockNode(instFactory.makeLabel());
        b1->appendInst(instFactory.makeBranch(Cmp_EQ, Type::VTablePtr, vt1, vt2, (LabelInst*)b3c->getFirstInst()));
        fg.addEdge(b1, b3c);
    }
    fg.addEdge(b1, b2);

    //
    // Fall throw to cast call in _b2_.
    //
    Opnd* tauCheckedCast = opndManager.createSsaTmpOpnd(typeManager.getTauType());
    b2->appendInst(instFactory.makeTauCheckCast(tauCheckedCast, src,
                                            tauNullChecked,
                                            type));
    fg.addEdge(b2, b3a);
    fg.addEdge(b2, fail);

    Opnd* tau3 = opndManager.createSsaTmpOpnd(typeManager.getTauType());
    Opnd* tau4 = opndManager.createSsaTmpOpnd(typeManager.getTauType());
    Opnd* tau2 = opndManager.createSsaTmpOpnd(typeManager.getTauType());

    if (_preserveSsa) {
        // using SSA
        VarOpnd* tauHasTypeVar = opndManager.createVarOpnd(typeManager.getTauType(), false);
        SsaVarOpnd *tauvar1 = opndManager.createSsaVarOpnd(tauHasTypeVar);
        SsaVarOpnd *tauvar2 = opndManager.createSsaVarOpnd(tauHasTypeVar);
        SsaVarOpnd *tauvar3 = opndManager.createSsaVarOpnd(tauHasTypeVar);
        SsaVarOpnd *tauvar4 = opndManager.createSsaVarOpnd(tauHasTypeVar);
        
        // b3a
        b3a->appendInst(instFactory.makeStVar(tauvar1, tauCheckedCast));
        
        // b3b
        b3b->appendInst(instFactory.makeTauEdge(tau3));
        b3b->appendInst(instFactory.makeStVar(tauvar2, tau3));
        
        if (b3c) {
            // b3c
            b3c->appendInst(instFactory.makeTauEdge(tau4));
            b3c->appendInst(instFactory.makeStVar(tauvar3, tau4));
        
            // b3
            Opnd* phiOpnds[3] = { tauvar1, tauvar2, tauvar3 };
            b3->appendInst(instFactory.makePhi(tauvar4, 3, phiOpnds));
            b3->appendInst(instFactory.makeLdVar(tau2, tauvar4));
        } else {
            // b3
            Opnd* phiOpnds[3] = { tauvar1, tauvar2 };
            b3->appendInst(instFactory.makePhi(tauvar4, 2, phiOpnds));
            b3->appendInst(instFactory.makeLdVar(tau2, tauvar4));
        }
    } else {
        // not SSA

        VarOpnd* tauHasTypeVar = opndManager.createVarOpnd(typeManager.getTauType(), false);
        VarOpnd *tauvar1 = tauHasTypeVar;
        VarOpnd *tauvar2 = tauHasTypeVar;
        VarOpnd *tauvar4 = tauHasTypeVar;
        
        // b3a
        b3a->appendInst(instFactory.makeStVar(tauvar1, tauCheckedCast));
        
        // b3b
        b3b->appendInst(instFactory.makeTauEdge(tau3));
        b3b->appendInst(instFactory.makeStVar(tauvar2, tau3));
        
        if (b3c) {
            // b3c
            b3c->appendInst(instFactory.makeTauEdge(tau4));
            b3c->appendInst(instFactory.makeStVar(tauvar4, tau4));
        }

        // b3
        b3->appendInst(instFactory.makeLdVar(tau2, tauvar4));
    }

    fg.addEdge(b3a, b3);
    fg.addEdge(b3b, b3);
    if (b3c)
        fg.addEdge(b3c, b3);
    b3->appendInst(instFactory.makeTauStaticCast(dst, src, tau2, type));
    fg.addEdge(b3, succ);

    return test1;
}

Inst*
CodeLowerer::caseTauCheckCast(TypeInst* inst)
{
    Node* block = inst->getNode();
    //
    // Perform null and vtable checks to avoid an expensive VM call.
    // The vtable check often succeeds in practice.
    //

    assert(inst != NULL);
    Type* type = inst->getTypeInfo();

    //
    // dstTau = checkCast src, castType, tauNullChecked
    //
    ObjectType* castType = (ObjectType*) type;
    Opnd* src = inst->getSrc(0);
    Opnd* tauNullChecked = inst->getSrc(1);
    assert(tauNullChecked->getType()->tag == Type::Tau);
    Opnd* dst = inst->getDst();
    
    bool nullWasChecked = (tauNullChecked->getInst()->getOpcode() != Op_TauUnsafe);

    //
    // Target is _succ_ if cast succeeds and _fail_ if cast fails.
    // 
    Node* succ =  block->getUnconditionalEdge()->getTargetNode();
    Node* fail =  block->getExceptionEdge()->getTargetNode();
    assert(succ->isBlockNode() && fail->isDispatchNode());

    ControlFlowGraph& fg = _irm.getFlowGraph();
    InstFactory& instFactory = _irm.getInstFactory();
    OpndManager& opndManager = _irm.getOpndManager();
    TypeManager& typeManager = _irm.getTypeManager();

    Inst* succInst = (Inst*)succ->getSecondInst();
    Inst* succInst2 = 0;
    Opnd* checkNullSrc = 0;
    if (succInst!=NULL && (succInst->getOpcode() == Op_TauStaticCast) && (succInst->getSrc(0) == src)) {
        // if succInst is a static cast, find following inst
        checkNullSrc = succInst->getDst();
        succInst2 = succInst;
        succInst = succInst->getNextInst();
    }
    if(succInst!=NULL && succInst->getOpcode() == Op_TauCheckNull) {
        assert(succInst == succ->getLastInst());
        Node* fail2 =  succ->getExceptionEdgeTarget();
        if(succ->getInDegree() == 1 && fail2 == fail && 
           ((succInst->getSrc(0) == src) || 
            (succInst->getSrc(0) == checkNullSrc))) {
            if (nullWasChecked) {
                //
                // We have
                //    dsttau = checkcast src, castType, tauNullChecked ---> fail
                //  succ:
                //    [optional staticcast]
                //    tauDst2 = chknull src          ---> fail2
                //  succ2:
                // Transform it to
                //    dsttau = checkcastsrc, castType, tauNullChecked ---> fail
                //  succ:
                //    [optional staticcast]
                //    tauDst2 = copy tauNullChecked
                //  succ2:

                Opnd* tauDst2 = succInst->getDst();
                succInst->setDst(OpndManager::getNullOpnd());

                Inst *copyInst = instFactory.makeCopy(tauDst2, tauNullChecked);
                copyInst->insertBefore(succInst);

                FlowGraph::eliminateCheck(fg, succ, succInst, false);
            } else {
                //
                // We have
                //    dsttau = checkcast src, castType, tauUnsafe ---> fail
                //  succ:
                //    [optional staticcast]
                //    tauDst2 = chknull src          ---> fail2
                //  succ2:
                // Transform it to
                //    tauDst2 = chknull src          ---> fail2
                //  succ:
                //    dst = checkcast src, castType, tauDst2   ---> fail
                //  succ2:
                //    [optional staticcast]
                
                // Reorder checknull and checkcast.
                Opnd* tauDst2 = succInst->getDst();
                Node* succ2 =  succ->getUnconditionalEdge()->getTargetNode();
                
                assert(inst->getSrc(1)->getType()->tag == Type::Tau);
                inst->setSrc(1, tauDst2);
                
                inst->unlink();
                succInst->unlink();
                
                block->appendInst(succInst);
                succ->appendInst(inst);
                
                succInst->setSrc(0, src); // if it had a static cast

                if (succInst2) {
                    // we had a static cast, insert it in succ2
                    succInst2->unlink();
                    Inst *succ2HeadInst = (Inst*)succ2->getFirstInst();
                    Inst *insertBeforeInst = succ2HeadInst->getNextInst();
                    while (insertBeforeInst!=NULL 
                        && ( (insertBeforeInst->getOpcode() == Op_Phi) 
                             || (insertBeforeInst->getOpcode() == Op_TauPi) ) ) 
                    {
                        insertBeforeInst = insertBeforeInst->getNextInst();
                    }
                    succInst2->insertBefore(insertBeforeInst);
                }

                // update our local state so we can apply the next rule
                block = succ;
                succ =  block->getUnconditionalEdge()->getTargetNode();
                fail =  block->getExceptionEdge()->getTargetNode();
                tauNullChecked = tauDst2;
                nullWasChecked = true;
            }
        }
    }

    // we probably have nullWasChecked here, but maybe not.

    if (!nullWasChecked) {
        //
        // Perform the following lowering:
        //
        // Replace:
        //   block:
        //     ...
        //     dsttau = checkcast src, (castType), tauNullChecked ---> fail
        //   succ: 
        //     ...
        //
        // with
        //   block:
        //     ...
        //     if cz src goto b3b
        //   b1:
        //     tauNullChecked = tauEdge
        //     vt1 = ldVTableAddr src, tauNullChecked
        //     vt2 = getVTableAddr castType 
        //     if ceq  vt1, vt2 goto b3c
        //   b2:
        //     tauCheckedCast = checkcast src, (castType), tauNullChecked ---> fail
        //   b3a:
        //     stvar tauvar1, tauCheckedCast
        //     goto b3
        //   b3b:
        //     tau3 = tauEdge
        //     stvar tauvar2, tau3
        //     goto b3
        //   b3c:
        //     tau4 = tauEdge
        //     stvar tauvar3, tau4
        //     goto b3
        //   b3:
        //     tauvar4 = phi(tauvar1, tauvar2, tauvar3)
        //     dsttau = ldvar tauvar4
        //   succ:
        //     ...
        
        Node* b1 = fg.createBlockNode(instFactory.makeLabel());
        Node* b2 = fg.createBlockNode(instFactory.makeLabel());
        Node* b3 = fg.createBlockNode(instFactory.makeLabel());
        Node* b3a = fg.createBlockNode(instFactory.makeLabel());
        Node* b3b = fg.createBlockNode(instFactory.makeLabel());
        Node* b3c = 0;
        
        //
        // Add null check to _block_.
        //
        fg.removeEdge(block, fail);
        fg.removeEdge(block, succ);
        inst->unlink();    
        Inst* test1 = instFactory.makeBranch(Cmp_Zero, src->getType()->tag,
                                             src,
                                             (LabelInst*)b3b->getFirstInst());
        block->appendInst(test1);
        fg.addEdge(block, b1);
        fg.addEdge(block, b3b);
        
        // create a new tau for nullchecked
        tauNullChecked = 
            opndManager.createSsaTmpOpnd(typeManager.getTauType());
        b1->appendInst(instFactory.makeTauEdge(tauNullChecked));
        
        //
        // Test direct vtable equality in _b1_ if cast target type is concrete.
        //
        if(!((type->isAbstract() && !type->isArray()) || type->isInterface())) {
            assert(type->isObject());
            assert(src->getType()->isObject());
            ObjectType* srcType = (ObjectType*) src->getType();
            Opnd* vt1 = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(srcType));
            Opnd* vt2 = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(castType));
            b1->appendInst(instFactory.makeTauLdVTableAddr(vt1, src, tauNullChecked));
            b1->appendInst(instFactory.makeGetVTableAddr(vt2, castType));
            b3c = fg.createBlockNode(instFactory.makeLabel());
            b1->appendInst(instFactory.makeBranch(Cmp_EQ, Type::VTablePtr, vt1, vt2, (LabelInst*)b3c->getFirstInst()));
            fg.addEdge(b1, b3c);
        }
        fg.addEdge(b1, b2);
        
        //
        // Fall throw to cast call in _b2_.
        //
        Opnd* tauCheckedCast = opndManager.createSsaTmpOpnd(typeManager.getTauType());
        // reuse inst
        inst->setDst(tauCheckedCast);
        inst->setSrc(1, tauNullChecked);
        b2->appendInst(inst);
        fg.addEdge(b2, b3a);
        fg.addEdge(b2, fail);
        
        Opnd* tau3 = opndManager.createSsaTmpOpnd(typeManager.getTauType());
        Opnd* tau4 = opndManager.createSsaTmpOpnd(typeManager.getTauType());
        Opnd* tau2 = dst; // reuse old dst tau
        
        if (_preserveSsa) {
            // using SSA
            VarOpnd* tauHasTypeVar = opndManager.createVarOpnd(typeManager.getTauType(), false);
            SsaVarOpnd *tauvar1 = opndManager.createSsaVarOpnd(tauHasTypeVar);
            SsaVarOpnd *tauvar2 = opndManager.createSsaVarOpnd(tauHasTypeVar);
            SsaVarOpnd *tauvar3 = opndManager.createSsaVarOpnd(tauHasTypeVar);
            SsaVarOpnd *tauvar4 = opndManager.createSsaVarOpnd(tauHasTypeVar);
            
            // b3a
            b3a->appendInst(instFactory.makeStVar(tauvar1, tauCheckedCast));
            
            // b3b
            b3b->appendInst(instFactory.makeTauEdge(tau3));
            b3b->appendInst(instFactory.makeStVar(tauvar2, tau3));
            
            if (b3c) {
                // b3c
                b3c->appendInst(instFactory.makeTauEdge(tau4));
                b3c->appendInst(instFactory.makeStVar(tauvar3, tau4));
                
                // b3
                Opnd* phiOpnds[3] = { tauvar1, tauvar2, tauvar3 };
                b3->appendInst(instFactory.makePhi(tauvar4, 3, phiOpnds));
                b3->appendInst(instFactory.makeLdVar(tau2, tauvar4));
            } else {
                // b3
                Opnd* phiOpnds[3] = { tauvar1, tauvar2 };
                b3->appendInst(instFactory.makePhi(tauvar4, 2, phiOpnds));
                b3->appendInst(instFactory.makeLdVar(tau2, tauvar4));
            }
        } else {
            // not SSA
            
            VarOpnd* tauHasTypeVar = opndManager.createVarOpnd(typeManager.getTauType(), false);
            VarOpnd *tauvar1 = tauHasTypeVar;
            VarOpnd *tauvar2 = tauHasTypeVar;
            VarOpnd *tauvar4 = tauHasTypeVar;
            
            // b3a
            b3a->appendInst(instFactory.makeStVar(tauvar1, tauCheckedCast));
            
            // b3b
            b3b->appendInst(instFactory.makeTauEdge(tau3));
            b3b->appendInst(instFactory.makeStVar(tauvar2, tau3));
            
            if (b3c) {
                // b3c
                b3c->appendInst(instFactory.makeTauEdge(tau4));
                b3c->appendInst(instFactory.makeStVar(tauvar4, tau4));
            }
            
            // b3
            b3->appendInst(instFactory.makeLdVar(tau2, tauvar4));
        }
        
        fg.addEdge(b3a, b3);
        fg.addEdge(b3b, b3);
        if (b3c)
            fg.addEdge(b3c, b3);
        fg.addEdge(b3, succ);
        return 0;
    } else {
        //
        // We already have a null check, perform the following lowering:
        //
        // Replace:
        //   block:
        //     ...
        //     dsttau = checkcast src, (castType), tauNullChecked ---> fail
        //   succ: 
        //     ...
        //
        // with
        //   block:
        //     ...
        //     vt1 = ldVTableAddr src, tauNullChecked
        //     vt2 = getVTableAddr castType 
        //     if ceq  vt1, vt2 goto b3c
        //   b2:
        //     tauCheckedCast = checkcast src, (castType), tauNullChecked ---> fail
        //   b3a:
        //     stvar tauvar1, tauCheckedCast
        //     goto b3
        //   b3c:
        //     tau4 = tauEdge
        //     stvar tauvar3, tau4
        //     goto b3
        //   b3:
        //     tauvar4 = phi(tauvar1, tauvar3)
        //     dsttau = ldvar tauvar4
        //   succ:
        //     ...
        
        // this only makes sense if the type is concrete, so return if not.
        if (((type->isAbstract() && !type->isArray()) || type->isInterface())) {
            return 0;
        }

        Node* b1 = block;
        Node* b2 = fg.createBlockNode(instFactory.makeLabel());
        Node* b3 = fg.createBlockNode(instFactory.makeLabel());
        Node* b3a = fg.createBlockNode(instFactory.makeLabel());
        Node* b3c = fg.createBlockNode(instFactory.makeLabel());
        
        // remove test at the end of block
        inst->unlink();    
        fg.removeEdge(block, fail);
        fg.removeEdge(block, succ);
        
        //
        // Test direct vtable equality in _b1_
        //
        assert(type->isObject());
        assert(src->getType()->isObject());
        ObjectType* srcType = (ObjectType*) src->getType();
        Opnd* vt1 = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(srcType));
        Opnd* vt2 = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(castType));
        Inst* nextInst = instFactory.makeTauLdVTableAddr(vt1, src, tauNullChecked);
        b1->appendInst(nextInst);
        b1->appendInst(instFactory.makeGetVTableAddr(vt2, castType));
        b1->appendInst(instFactory.makeBranch(Cmp_EQ, Type::VTablePtr, vt1, vt2, (LabelInst*)b3c->getFirstInst()));
        fg.addEdge(b1, b3c);
        fg.addEdge(b1, b2);
        
        //
        // Fall throw to cast call in _b2_.
        //
        Opnd* tauCheckedCast = opndManager.createSsaTmpOpnd(typeManager.getTauType());
        // reuse inst
        inst->setDst(tauCheckedCast);
        inst->setSrc(1, tauNullChecked);
        b2->appendInst(inst);
        fg.addEdge(b2, b3a);
        fg.addEdge(b2, fail);
        
        opndManager.createSsaTmpOpnd(typeManager.getTauType());
        Opnd* tau4 = opndManager.createSsaTmpOpnd(typeManager.getTauType());
        Opnd* tau2 = dst; // reuse old dst tau
        
        if (_preserveSsa) {
            // using SSA
            VarOpnd* tauHasTypeVar = opndManager.createVarOpnd(typeManager.getTauType(), false);
            SsaVarOpnd *tauvar1 = opndManager.createSsaVarOpnd(tauHasTypeVar);
            SsaVarOpnd *tauvar3 = opndManager.createSsaVarOpnd(tauHasTypeVar);
            SsaVarOpnd *tauvar4 = opndManager.createSsaVarOpnd(tauHasTypeVar);
            
            // b3a
            b3a->appendInst(instFactory.makeStVar(tauvar1, tauCheckedCast));
            
            // b3c
            b3c->appendInst(instFactory.makeTauEdge(tau4));
            b3c->appendInst(instFactory.makeStVar(tauvar3, tau4));
            
            // b3
            Opnd* phiOpnds[2] = { tauvar1, tauvar3 };
            b3->appendInst(instFactory.makePhi(tauvar4, 2, phiOpnds));
            b3->appendInst(instFactory.makeLdVar(tau2, tauvar4));
        } else {
            // not SSA
            
            VarOpnd* tauHasTypeVar = opndManager.createVarOpnd(typeManager.getTauType(), false);
            VarOpnd *tauvar1 = tauHasTypeVar;
            VarOpnd *tauvar4 = tauHasTypeVar;
            
            // b3a
            b3a->appendInst(instFactory.makeStVar(tauvar1, tauCheckedCast));
            
            // b3c
            b3c->appendInst(instFactory.makeTauEdge(tau4));
            b3c->appendInst(instFactory.makeStVar(tauvar4, tau4));
            
            // b3
            b3->appendInst(instFactory.makeLdVar(tau2, tauvar4));
        }
        
        fg.addEdge(b3a, b3);
        fg.addEdge(b3c, b3);
        fg.addEdge(b3, succ);
        return 0;
    }
}

Inst*
CodeLowerer::caseTauAsType(TypeInst* inst)
{
    ControlFlowGraph& fg = _irm.getFlowGraph();
    Node* block = inst->getNode();
    Node* succ = fg.splitNodeAtInstruction(inst, true, false, _irm.getInstFactory().makeLabel());

    // Replace:
    //   block:
    //     ...
    //     dst = asType src, castType, tauNullChecked
    //   succ:
    //
    // with
    //   block:
    //     ...
    // #if tauNullChecked == TauUnsafe
    //     if cz src goto b2
    //   b0:
    //     tauNullChecked2 = tauedge
    // #else
    // #define tauNullChecked2 =  tauNullChecked
    // #endif
    //     t1 = instanceOf src, tauNullChecked2, castType
    //     if cz t1 goto b2
    //   b1:
    //     tau1 = tauedge
    //     t2 = staticcast src, tau1, castType
    //     v1.1 = stvar t2
    //     goto succ
    //   b2:
    //     t3 = null
    //     v1.2 = t3
    //   succ:
    //     v1.3 = phi(v1.1, v1.2)
    //     dst = ldVar v1.3
    //     

    InstFactory& instFactory = _irm.getInstFactory();
    OpndManager& opndManager = _irm.getOpndManager();
    TypeManager& typeManager = _irm.getTypeManager();

    fg.removeEdge(block, succ);
    inst->unlink();
    Opnd* src = inst->getSrc(0);
    Opnd* tauNullChecked = inst->getSrc(1);
    assert(tauNullChecked->getType()->tag == Type::Tau);
    Opnd* dst = inst->getDst();
    Type* castType = inst->getTypeInfo();

    Node* b1 = fg.createBlockNode(instFactory.makeLabel());
    Node* b2 = fg.createBlockNode(instFactory.makeLabel());
    VarOpnd* var = opndManager.createVarOpnd(castType, false);

    if (tauNullChecked->getInst()->getOpcode() == Op_TauUnsafe) {
        Node* b0 = fg.createBlockNode(instFactory.makeLabel());
        // Block
        block->appendInst(instFactory.makeBranch(Cmp_Zero, src->getType()->tag, src, (LabelInst*)b2->getFirstInst()));
        fg.addEdge(block, b0);
        fg.addEdge(block, b2);
        
        // b0
        tauNullChecked = opndManager.createSsaTmpOpnd(typeManager.getTauType());
        b0->appendInst(instFactory.makeTauEdge(tauNullChecked));

        block = b0;
    }
    SsaTmpOpnd* t1 = opndManager.createSsaTmpOpnd(typeManager.getBooleanType());
    Inst *instanceOf = instFactory.makeTauInstanceOf(t1, src, tauNullChecked, 
                                                     castType);
    block->appendInst(instanceOf);
    block->appendInst(instFactory.makeBranch(Cmp_Zero, t1->getType()->tag, t1, (LabelInst*)b2->getFirstInst()));
    fg.addEdge(block, b2);
    fg.addEdge(block, b1);
    
    if (_preserveSsa) {
        // B1
        SsaTmpOpnd* t2 = opndManager.createSsaTmpOpnd(castType);
        SsaTmpOpnd* tau1 = opndManager.createSsaTmpOpnd(typeManager.getTauType());
        SsaVarOpnd *var1 = opndManager.createSsaVarOpnd(var);
        b1->appendInst(instFactory.makeTauEdge(tau1));
        b1->appendInst(instFactory.makeTauStaticCast(t2, src, tau1, castType));
        b1->appendInst(instFactory.makeStVar(var1, t2));
        
        // B2
        SsaTmpOpnd* t3 = opndManager.createSsaTmpOpnd(castType);
        SsaVarOpnd *var2 = opndManager.createSsaVarOpnd(var);
        b2->appendInst(instFactory.makeLdNull(t3));
        b2->appendInst(instFactory.makeStVar(var2, t3));
        
        // succ
        SsaVarOpnd *var3 = opndManager.createSsaVarOpnd(var);
        Opnd* phiOpnds[2] = { var1, var2 };
        Inst* phiInst = instFactory.makePhi(var3, 2, phiOpnds);
        Inst* ldVar = instFactory.makeLdVar(dst, var);
        succ->prependInst(ldVar);
        succ->prependInst(phiInst);
    } else {
        // B1
        SsaTmpOpnd* t2 = opndManager.createSsaTmpOpnd(castType);
        SsaTmpOpnd* tau1 = opndManager.createSsaTmpOpnd(typeManager.getTauType());
        b1->appendInst(instFactory.makeTauEdge(tau1));
        b1->appendInst(instFactory.makeTauStaticCast(t2, src, tau1, castType));
        b1->appendInst(instFactory.makeStVar(var, t2));
        
        // B2
        SsaTmpOpnd* t3 = opndManager.createSsaTmpOpnd(castType);
        b2->appendInst(instFactory.makeLdNull(t3));
        b2->appendInst(instFactory.makeStVar(var, t3));
        
        // succ
        Inst* ldVar = instFactory.makeLdVar(dst, var);
        succ->prependInst(ldVar);
    }

    fg.addEdge(b1, succ);
    fg.addEdge(b2, succ);

    lowerBlock(succ);
    return instanceOf;
}

Inst*
CodeLowerer::caseTauInstanceOf(TypeInst* inst)
{
    Node* block = inst->getNode();
    //
    // Perform null and vtable checks to avoid an expensive vm call.
    // The vtable check often succeeds in practice.
    //

    //
    // Skip if non-concrete type.
    //
    assert(inst != NULL);
    Type* type = inst->getTypeInfo();
    if((type->isAbstract() && !type->isArray()) || type->isInterface())
        return inst;
    assert(type->isObject());

    //
    // dst = instanceOf src, tauCheckedNull, castType 
    //
    ObjectType* castType = (ObjectType*) type;    
    Opnd* src = inst->getSrc(0);
    Opnd* tauCheckedNull = inst->getSrc(1);
    assert(tauCheckedNull->getType()->tag == Type::Tau);
    Opnd* dst = inst->getDst();

    ControlFlowGraph& fg = _irm.getFlowGraph();
    InstFactory& instFactory = _irm.getInstFactory();
    OpndManager& opndManager = _irm.getOpndManager();
    TypeManager& typeManager = _irm.getTypeManager();

    if (!dst->isGlobal()) {
        // check for
        //     dst = instanceOf src, tauCheckedNull, castType
        //     zero = ldconst 0
        //     if ceq dst, zero goto fail
        // which can be simplified more

        //
        // Ensure that dst is only used within a following branch.  If so,
        // dst can be folded.  Else, give up.
        //
        Inst* i;
        for(i  = (Inst*)block->getFirstInst(); i != NULL; i = i->getNextInst()) {
            if (i->getNumSrcOperands() > 0)
                break;
        }

        if ((i->getOpcode() == Op_Branch) &&
            (i->getSrc(0)->getType()->isInteger())) {            

            BranchInst* branch = i->asBranchInst();
            assert(branch != NULL);

            //
            // Determine whether branch is testing dst == 0 in some form.
            //
            Opnd* src0 = branch->getSrc(0);
            Opnd* src1 = NULL;

            bool eq = true;
            switch (branch->getComparisonModifier()) {
            case Cmp_NE_Un:
                eq = false;
            case Cmp_EQ:
                src1 = branch->getSrc(1);
                if(!ConstantFolder::isConstantZero(src1)) {
                    if(!ConstantFolder::isConstantZero(src0))
                        src0 = 0; // not analyzable, this will cause test below to fail;
                    else
                        src0 = src1;
                }
                break;
            case Cmp_NonZero:
                eq = false;
            case Cmp_Zero:
                break;
            case Cmp_GT:
            case Cmp_GT_Un:
            case Cmp_GTE:
            case Cmp_GTE_Un:
                src0 = 0; // not analyzable, this will cause test below to fail;
                break;
            default:
                assert(0);
                break;
            }

            if(src0 == dst) {
                Node* taken =  block->getTrueEdge()->getTargetNode();
                Node* nottaken =  block->getFalseEdge()->getTargetNode();
            
                //
                // Determine the targets when instanceOf is true (succ) and false (fail).
                //
                Node* succ = eq ? nottaken : taken;
                Node* fail = eq ? taken : nottaken;

                // Lower when we can fold into the following branch and eliminate dst.
                //
                // Replace:
                //   block:
                //     ...
                //     dst = instanceOf src, tauUnsafe, castType
                //     zero = ldconst 0
                //     if ceq dst, zero goto fail
                //   succ:
                //     ... 
                //   fail:
                //     ...
                //with
                //   block:
                //     ...
                //[if tauCheckedNull==tauUnsafe
                //     if cz src goto fail
                //   b1:
                //     tau1 = tauedge
                // else
                //     tau1 = tauCheckedNull
                //endif]
                //     vt1 = ldVTableAddr src, tau1
                //     vt2 = getVTableAddr castType 
                //     if ceq  vt1, vt2 goto succ
                //   b2:
                //     dst = instanceOf src, tau1, castType
                //     zero = ldconst 0
                //     if ceq dst, zero goto fail
                //   succ:
                //     ...
                //   fail:
                //     ...
                
                //
                // Remove the instanceOf and branch from block.
                //
                inst->unlink();
                branch->unlink();
                fg.removeEdge(block, succ);
                
                Node* b1 = 0;
                Node* b2 = fg.createBlockNode(instFactory.makeLabel());

                Opnd *tau1 = 0;
                if (tauCheckedNull->getInst()->getOpcode() == Op_TauUnsafe) {
                    b1 = fg.createBlockNode(instFactory.makeLabel());

                    // block: ... if src == NULL
                    Inst* test1 = instFactory.makeBranch(Cmp_Zero, src->getType()->tag, src, (LabelInst*)fail->getFirstInst());
                    block->appendInst(test1);
                    fg.addEdge(block, b1);
                    tau1 = opndManager.createSsaTmpOpnd(typeManager.getTauType());
                    b1->appendInst(instFactory.makeTauEdge(tau1));
                    inst->setSrc(1, tau1);
                } else {
                    // remove the other edge; we add new ones
                    fg.removeEdge(block, fail);
                    // just use block as b1, and tauCheckedNull as tau1
                    b1 = block;
                    tau1 = tauCheckedNull;
                }
                
                // b1: if src.vtable == type.vtable
                assert(src->getType()->isObject());
                ObjectType* srcType = (ObjectType*) src->getType();
                Opnd* dynamicVTableAddr = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(srcType));
                Opnd* staticVTableAddr = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(castType));
                b1->appendInst(instFactory.makeTauLdVTableAddr(dynamicVTableAddr, src, tau1));
                b1->appendInst(instFactory.makeGetVTableAddr(staticVTableAddr, castType));
                b1->appendInst(instFactory.makeBranch(Cmp_EQ, Type::VTablePtr, dynamicVTableAddr, staticVTableAddr, (LabelInst*)succ->getFirstInst()));
                fg.addEdge(b1, succ);
                fg.addEdge(b1, b2);
                
                // b2: var = src instanceOf type
                b2->appendInst(inst);
                b2->appendInst(branch);
                fg.addEdge(b2, succ);
                fg.addEdge(b2, fail);
                
                return NULL;
            }
        }
    }
    
    // otherwise, just lower instanceOf, although we don't have a branch
    // to fold it into.
    { 
        Node* succ = fg.splitNodeAtInstruction(inst, true, false, instFactory.makeLabel());

        fg.removeEdge(block, succ);
        inst->unlink();

        if (tauCheckedNull->getInst()->getOpcode() != Op_TauUnsafe) {
            // we must have already checked src for null
            
            // Replace:
            //   block:
            //     ...
            //     dst = instanceOf src, tauCheckedNull, castType
            //   succ:
            // with
            //   block:
            //     ...
            //     vt1 = ldVTableAddr src, tauCheckedNull
            //     vt2 = getVTableAddr castType
            //     if ceq  vt1, vt2 goto b3
            //   b2:
            //     newDst = instanceOf src, tauCheckedNull, castType
            //     v1.1 = stvar newDst
            //     goto succ
            //   b3:
            //     v1.2 = ldconst 1
            //     goto succ
            //   succ:
            //     v1.3 = phi(v1.1, v1.2)
            //     dst = ldvar v1.3
            
            Node* b2 = fg.createBlockNode(instFactory.makeLabel());
            Node* b3 = fg.createBlockNode(instFactory.makeLabel());
            
            // block: if src.vtable == type.vtable
            assert(src->getType()->isObject());
            ObjectType* srcType = (ObjectType*) src->getType();
            Opnd* dynamicVTableAddr = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(srcType));
            Opnd* staticVTableAddr = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(castType));
            Inst * ldVTableAddrInst = 
                instFactory.makeTauLdVTableAddr(dynamicVTableAddr, src, tauCheckedNull);
            block->appendInst(ldVTableAddrInst);
            block->appendInst(instFactory.makeGetVTableAddr(staticVTableAddr, castType));
            block->appendInst(instFactory.makeBranch(Cmp_EQ, Type::VTablePtr, dynamicVTableAddr, staticVTableAddr, (LabelInst*)b3->getFirstInst()));
            fg.addEdge(block, b3);
            fg.addEdge(block, b2);
            
            // b2: newDst = instanceOf src, tauCheckedNull, type
            Opnd* newDst = opndManager.createSsaTmpOpnd(dst->getType());
            inst->setDst(newDst);
            b2->appendInst(inst);
            fg.addEdge(b2, succ);
            
            // b3: var = 1
            Opnd* one = opndManager.createSsaTmpOpnd(dst->getType());
            b3->appendInst(instFactory.makeLdConst(one, (I_32) 1));
            fg.addEdge(b3, succ);
            
            VarOpnd* dstVar = opndManager.createVarOpnd(dst->getType(), false);
            if (_preserveSsa) {
                // use SSA
                SsaVarOpnd* var1 = opndManager.createSsaVarOpnd(dstVar);  // dst value of instanceof;
                SsaVarOpnd* var2 = opndManager.createSsaVarOpnd(dstVar);  // dst value of one
                SsaVarOpnd* phiSsaVar = opndManager.createSsaVarOpnd(dstVar);  // phi result
                
                Opnd* dstSsaVars[3] =  { var1, var2 };
                
                // Patch b2
                b2->appendInst(instFactory.makeStVar(var1, newDst));
                
                // Patch b3
                b3->appendInst(instFactory.makeStVar(var2, one));
                
                // succ: dst = var ...
                succ->prependInst(instFactory.makeLdVar(dst, phiSsaVar));
                succ->prependInst(instFactory.makePhi(phiSsaVar, 2, dstSsaVars));
            } else {
                // no SSA
                
                // Patch b2
                b2->appendInst(instFactory.makeStVar(dstVar, newDst));
                
                // Patch b3
                b3->appendInst(instFactory.makeStVar(dstVar, one));
                
                // succ: dst = var ...
                succ->prependInst(instFactory.makeLdVar(dst, dstVar));
            }            
            
            lowerBlock(succ);
            return ldVTableAddrInst;
        } else {
            // src may be null, need to add a check

            //
            // Replace:
            //   block:
            //     ...
            //     dst = instanceOf src, tauUnsafe, castType
            //   succ:
            //with
            //   block:
            //     ...
            //     if cz src goto b4
            //   b1:
            //     tau1 = tauedge
            //     vt1 = ldVTableAddr src, tau1
            //     vt2 = getVTableAddr castType 
            //     if ceq  vt1, vt2 goto b3
            //   b2:
            //     newDst = instanceOf src, tau1, castType
            //     stvar v1.1, newDst
            //     goto succ
            //   b3:
            //     one = ldconst 1
            //     stvar v1.2, one
            //     goto succ
            //   b4:
            //     zero = ldconst 0
            //     stvar v1.3, zero
            //     goto succ
            //   succ:
            //     v1.4 = phi(v1.1, v1.2, v1.3)
            //     dst = ldvar v1.4
            //     ...
            
            Node* b1 = fg.createBlockNode(instFactory.makeLabel());
            Node* b2 = fg.createBlockNode(instFactory.makeLabel());
            Node* b3 = fg.createBlockNode(instFactory.makeLabel());
            Node* b4 = fg.createBlockNode(instFactory.makeLabel());
            
            // block: ... if src == NULL
            Inst* test1 = instFactory.makeBranch(Cmp_Zero, src->getType()->tag, src, (LabelInst*)b4->getFirstInst());
            block->appendInst(test1);
            fg.addEdge(block, b4);
            fg.addEdge(block, b1);
            
            // b1: if src.vtable == type.vtable
            assert(src->getType()->isObject());
            ObjectType* srcType = (ObjectType*) src->getType();
            Opnd* dynamicVTableAddr = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(srcType));
            Opnd* staticVTableAddr = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(castType));
            Opnd* tauNonNull = opndManager.createSsaTmpOpnd(typeManager.getTauType());
            b1->appendInst(instFactory.makeTauEdge(tauNonNull));
            b1->appendInst(instFactory.makeTauLdVTableAddr(dynamicVTableAddr, src, tauNonNull));
            b1->appendInst(instFactory.makeGetVTableAddr(staticVTableAddr, castType));
            b1->appendInst(instFactory.makeBranch(Cmp_EQ, Type::VTablePtr, dynamicVTableAddr, staticVTableAddr, (LabelInst*)b3->getFirstInst()));
            fg.addEdge(b1, b3);
            fg.addEdge(b1, b2);
            
            // b2: var = src instanceOf type
            Opnd* newDst = opndManager.createSsaTmpOpnd(dst->getType());
            inst->setDst(newDst);
            b2->appendInst(inst);
            fg.addEdge(b2, succ);
            
            // b3: var = 1
            Opnd* one = opndManager.createSsaTmpOpnd(dst->getType());
            b3->appendInst(instFactory.makeLdConst(one, (I_32) 1));
            fg.addEdge(b3, succ);
            
            // b4: var = 0
            Opnd* zero = opndManager.createSsaTmpOpnd(dst->getType());
            b4->appendInst(instFactory.makeLdConst(zero, (I_32) 0));
            fg.addEdge(b4, succ);
            
            VarOpnd* dstVar = opndManager.createVarOpnd(dst->getType(), false);
            if (_preserveSsa) {
                // use SSA
                SsaVarOpnd* var1 = opndManager.createSsaVarOpnd(dstVar);  // dst value of instanceof in b2;
                SsaVarOpnd* var2 = opndManager.createSsaVarOpnd(dstVar);  // dst value of one in b3
                SsaVarOpnd* var3 = opndManager.createSsaVarOpnd(dstVar);  // dst value of zero in b4
                SsaVarOpnd* var4 = opndManager.createSsaVarOpnd(dstVar);  // phi merge of the two above, in succ
                
                Opnd* dstSsaVars[3] =  { var1, var2, var3 };
                
                // Patch b2
                b2->appendInst(instFactory.makeStVar(var1, newDst));
                
                // Patch b3
                b3->appendInst(instFactory.makeStVar(var2, one));
                
                // Patch b4
                b4->appendInst(instFactory.makeStVar(var3, zero));
                
                // succ: dst = var ...
                succ->prependInst(instFactory.makeLdVar(dst, var4));
                succ->prependInst(instFactory.makePhi(var4, 3, dstSsaVars));
            } else {
                // no SSA
                
                // Patch b2
                b2->appendInst(instFactory.makeStVar(dstVar, newDst));
                
                // Patch b3
                b3->appendInst(instFactory.makeStVar(dstVar, one));
                
                // Patch b4
                b4->appendInst(instFactory.makeStVar(dstVar, zero));
                
                // succ: dst = var ...
                succ->prependInst(instFactory.makeLdVar(dst, dstVar));
            }
            
            lowerBlock(succ);
            return test1;
        }
    }
}

Inst*
CodeLowerer::caseTauCheckElemType(Inst* inst)
{
    Node* block = inst->getNode();
    //
    // Perform null and vtable checks to avoid an expensive vm call.
    // The vtable check often succeeds in practice.
    //

    // Skip if non-concrete type.
    //
    assert(inst != NULL);

    //
    // checkElemType array, src, tauCheckedNull, tauIsArray
    //
    assert(inst->getNumSrcOperands() == 4);
    Opnd* array = inst->getSrc(0);
    Opnd* src = inst->getSrc(1);
    Opnd* tauCheckedNull = inst->getSrc(2);
    Opnd* tauIsArray = inst->getSrc(3);
    assert(tauCheckedNull->getType()->tag == Type::Tau);
    Opnd* tauResult = inst->getDst();
    assert(tauResult->getType()->tag == Type::Tau);
    
    //
    // Target is _succ_ if check succeeds and _fail_ if cast fails.
    // 
    Node* succ =  block->getUnconditionalEdge()->getTargetNode();
    Node* fail =  block->getExceptionEdge()->getTargetNode();
    assert(succ->isBlockNode() && fail->isDispatchNode());

    if (tauCheckedNull->getInst()->getOpcode() == Op_TauUnsafe) {
        assert(0);
        return 0;
    }
    if (tauIsArray->getInst()->getOpcode() == Op_TauUnsafe) {
        assert(0);
        return 0;
    }

    if (Log::isEnabled()) {
        Log::out() << "Reducing checkelemtype: ";
        inst->print(Log::out());
        Log::out() << "block has id " << (int) block->getId() << ::std::endl;
        Log::out() << "succ has id " << (int) succ->getId() << ::std::endl;
        Log::out() << "fail has id " << (int) fail->getId() << ::std::endl;
        Log::out() << ::std::endl;
    }

    // now, lower checkElemType:
    // replace
    //    block:
    //      ...
    //      tauResult = checkElemType array, src, tauCheckedNull, TauIsArray
    //    succ:
    // 
    // with
    //    block:
    //      ...

    //      if cz src goto b3 // null, done
    //      goto b1 // if array could be array of Object
    //    b1:
    //      t1 = ldvtable array, tauCheckedNull
    //      t2 = getvtable ArrayOfObject
    //      if t1 == t2 goto b1a  // array of Object, all references fit
    //      goto b2
    //    b1a:
    //      tau1 = tauEdge
    //      tauvar1 = stvar tau1
    //      goto b0
    //    b2: // do the check
    //      tau2 = checkElemType array, src, tauCheckedNull, TauUnsafe
    //      goto b2a
    //    b2a: // check succeeded
    //      tauvar2 = stvar tau2
    //      goto b0
    //    b3: // src is null
    //      tau3 = tauEdge
    //      tauvar3 = stvar tau3
    //      goto b0
    //    b0:
    //      tauvar0 = phi(tauvar1, tauvar2, tauvar3)
    //      tauResult = ldvar tauvar
    //    succ:

    ControlFlowGraph& fg = _irm.getFlowGraph();
    InstFactory& instFactory = _irm.getInstFactory();
    OpndManager& opndManager = _irm.getOpndManager();
    TypeManager& typeManager = _irm.getTypeManager();
    
    Node* b1 = 0;
    Node* b1a = 0;
    Node* b2 = fg.createBlockNode(instFactory.makeLabel());
    Node* b2a = fg.createBlockNode(instFactory.makeLabel());
    Node* b3 = fg.createBlockNode(instFactory.makeLabel());
    Node* b0 = fg.createBlockNode(instFactory.makeLabel());

    fg.removeEdge(block, fail);
    fg.removeEdge(block, succ);

    fg.addEdge(b2, fail);
    fg.addEdge(b2, b2a);
    fg.addEdge(b2a, b0);

    fg.addEdge(block, b3);
    fg.addEdge(b3, b0);

    fg.addEdge(b0, succ);

    //
    // Add null check to _block_.
    //
    inst->unlink();    
    Inst* test1 = instFactory.makeBranch(Cmp_Zero, src->getType()->tag,
                                         src,
                                         (LabelInst*)b3->getFirstInst());
    block->appendInst(test1);

    assert(array->getType()->isObject());
    ObjectType* arrayType = (ObjectType*) array->getType();
    ObjectType* systemObjectType = typeManager.getSystemObjectType();
    ObjectType* arrayOfObjectType = typeManager.getArrayType(systemObjectType);
    if(arrayType == arrayOfObjectType) {
        // Check if it's an exact match at runtime.  If so, we can avoid the more expensive elemType check.
        b1 = fg.createBlockNode(instFactory.makeLabel());
        b1a = fg.createBlockNode(instFactory.makeLabel());
        
        opndManager.createSsaTmpOpnd(typeManager.getTauType());
        Opnd* dynamicVTableAddr = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(arrayType));
        Opnd* staticVTableAddr = opndManager.createSsaTmpOpnd(typeManager.getVTablePtrType(arrayOfObjectType));
        b1->appendInst(instFactory.makeTauLdVTableAddr(dynamicVTableAddr, array, tauCheckedNull));
        b1->appendInst(instFactory.makeGetVTableAddr(staticVTableAddr, arrayOfObjectType));
        b1->appendInst(instFactory.makeBranch(Cmp_EQ, Type::VTablePtr, dynamicVTableAddr, staticVTableAddr, (LabelInst*)b1a->getFirstInst()));

        fg.addEdge(block, b1);
        fg.addEdge(b1, b1a);
        fg.addEdge(b1a, b0);

        fg.addEdge(b1, b2);
    } else {
        // skip to b2
        fg.addEdge(block, b2);
    }
        
    //
    // b2: Otherwise, fall throw to checkElemType
    //
    Opnd *tau2 = opndManager.createSsaTmpOpnd(typeManager.getTauType());
    inst->setDst(tau2);
    b2->appendInst(inst);
    
    VarOpnd* tauHasTypeVar = opndManager.createVarOpnd(typeManager.getTauType(), false);

    if (_preserveSsa) {
        SsaVarOpnd *tauvar0 = opndManager.createSsaVarOpnd(tauHasTypeVar);
        SsaVarOpnd *tauvar2 = opndManager.createSsaVarOpnd(tauHasTypeVar);
        SsaVarOpnd *tauvar3 = opndManager.createSsaVarOpnd(tauHasTypeVar);
            
        // b2a
        b2a->appendInst(instFactory.makeStVar(tauvar2, tau2));
        
        // b3
        Opnd *tau3 = opndManager.createSsaTmpOpnd(typeManager.getTauType());
        b3->appendInst(instFactory.makeTauEdge(tau3));
        b3->appendInst(instFactory.makeStVar(tauvar3, tau3));
            
        if (b1a) {
            // b1a
            SsaVarOpnd *tauvar1 = opndManager.createSsaVarOpnd(tauHasTypeVar);
            Opnd *tau1 = opndManager.createSsaTmpOpnd(typeManager.getTauType());
            b1a->appendInst(instFactory.makeTauEdge(tau1));
            b1a->appendInst(instFactory.makeStVar(tauvar1, tau1));
            
            // b0
            Opnd* phiOpnds[3] = { tauvar1, tauvar2, tauvar3 };
            b0->prependInst(instFactory.makeLdVar(tauResult, tauvar0));
            b0->prependInst(instFactory.makePhi(tauvar0, 3, phiOpnds));
        } else {
            // b0
            Opnd* phiOpnds[2] = { tauvar2, tauvar3 };
            b0->prependInst(instFactory.makeLdVar(tauResult, tauvar0));
            b0->prependInst(instFactory.makePhi(tauvar0, 2, phiOpnds));
        }
    } else {
        // b2a
        b2a->appendInst(instFactory.makeStVar(tauHasTypeVar, tau2));
        
        // b3
        Opnd *tau3 = opndManager.createSsaTmpOpnd(typeManager.getTauType());
        b3->appendInst(instFactory.makeTauEdge(tau3));
        b3->appendInst(instFactory.makeStVar(tauHasTypeVar, tau3));
        
        if (b1a) {
            // b1a
            Opnd *tau1 = opndManager.createSsaTmpOpnd(typeManager.getTauType());
            b1a->appendInst(instFactory.makeTauEdge(tau1));
            b1a->appendInst(instFactory.makeStVar(tauHasTypeVar, tau1));
        }
        
        // b0
        b0->prependInst(instFactory.makeLdVar(tauResult, tauHasTypeVar));
    }
    
    if (Log::isEnabled()) {
        Log::out() << "Reducing checkelemtype: ";
        inst->print(Log::out());
        if (b1) {
            Log::out() << "b1 is has id " << (int) b1->getId() << ::std::endl;
            Log::out() << "b1a is has id " << (int) b1a->getId() << ::std::endl;
        }
        Log::out() << "b2 has id " << (int) b2->getId() << ::std::endl;
        Log::out() << "b2a has id " << (int) b2a->getId() << ::std::endl;
        Log::out() << "b3 has id " << (int) b3->getId() << ::std::endl;
        Log::out() << "b0 has id " << (int) b0->getId() << ::std::endl;
        Log::out() << ::std::endl;
    }
    return test1;
}

Inst*
CodeLowerer::caseLdStatic(FieldAccessInst *inst)
{
    assert(0); // shouldn't show up in lowering phase.
    return inst;
}

Inst*
CodeLowerer::caseTauLdField(FieldAccessInst *inst)
{
    assert(0); // shouldn't show up in lowering phase.
    return inst;
}

Inst*
CodeLowerer::caseTauLdElem(TypeInst *inst)
{
    assert(0); // shouldn't show up in lowering phase.
    return inst;
}

Inst*
CodeLowerer::caseLdStaticAddr(FieldAccessInst *inst)
{
    // we don't do anything for this
    return inst;
}

Inst*
CodeLowerer::caseLdFieldAddr(FieldAccessInst *inst)
{
    const OptimizerFlags& optimizerFlags = _irm.getOptimizerFlags();
    if (optimizerFlags.reduce_compref) {
        // reduce this to addoffset(base, ldfieldoffset)

        Opnd *dst = inst->getDst();
        Opnd *base = inst->getSrc(0);
        FieldDesc *desc = inst->getFieldDesc();
#ifndef NDEBUG
        Type::Tag tag = inst->getType();
        assert(Type::isReference(tag) && !Type::isCompressedReference(tag));
#endif
        InstFactory& instFactory = _irm.getInstFactory();
        OpndManager& opndManager = _irm.getOpndManager();
        TypeManager& typeManager = _irm.getTypeManager();

        SsaTmpOpnd* t1 = opndManager.createSsaTmpOpnd(typeManager.getOffsetType());
        Inst *ldOffsetInst = instFactory.makeLdFieldOffset(t1, desc);
        Inst *addOffsetInst = instFactory.makeAddOffset(dst, base, t1);
        ldOffsetInst->insertAfter(inst);
        addOffsetInst->insertAfter(ldOffsetInst);
        inst->unlink();

        return addOffsetInst;
    }
    return inst;
}

Inst*
CodeLowerer::caseLdElemAddr(TypeInst *inst)
{
    const OptimizerFlags& optimizerFlags = _irm.getOptimizerFlags();
    if (optimizerFlags.reduce_compref) {
        // reduce this to addoffset(base, ldelemoffset)
        TypeInst *tinst = inst->asTypeInst();
        assert(tinst);

        Opnd *dst = inst->getDst();
        Opnd *array = inst->getSrc(0);
        Opnd *index = inst->getSrc(1);
        Type *elemType = tinst->getTypeInfo();

        InstFactory& instFactory = _irm.getInstFactory();
        OpndManager& opndManager = _irm.getOpndManager();
        TypeManager& typeManager = _irm.getTypeManager();

        SsaTmpOpnd* baseOffset = opndManager.createSsaTmpOpnd(typeManager.getOffsetType());
        SsaTmpOpnd* baseAddr = opndManager.createSsaTmpOpnd(dst->getType());

        Inst *baseOffsetInst = instFactory.makeLdArrayBaseOffset(baseOffset,
                                                                 elemType);
        Inst *baseAddrInst = instFactory.makeAddOffset(baseAddr, array,
                                                       baseOffset);
        Inst *elemAddrInst = instFactory.makeAddScaledIndex(dst, baseAddr,
                                                            index);
        baseOffsetInst->insertAfter(inst);
        baseAddrInst->insertAfter(baseOffsetInst);
        elemAddrInst->insertAfter(baseAddrInst);
        inst->unlink();
        return elemAddrInst;
    }
    return inst;
}

Inst*
CodeLowerer::caseLdArrayBaseAddr(Inst *inst)
{
    const OptimizerFlags& optimizerFlags = _irm.getOptimizerFlags();
    if (optimizerFlags.reduce_compref) {
        // reduce this to addoffset(base, ldelemoffset)
        TypeInst *tinst = inst->asTypeInst();
        assert(tinst);

        Opnd *dst = inst->getDst();
        Opnd *array = inst->getSrc(0);
        Type *elemType = tinst->getTypeInfo();

        InstFactory& instFactory = _irm.getInstFactory();
        OpndManager& opndManager = _irm.getOpndManager();
        TypeManager& typeManager = _irm.getTypeManager();

        SsaTmpOpnd* baseOffset = opndManager.createSsaTmpOpnd(typeManager.getOffsetType());

        Inst *baseOffsetInst = instFactory.makeLdArrayBaseOffset(baseOffset,
                                                                 elemType);
        Inst *baseAddrInst = instFactory.makeAddOffset(dst, array,
                                                       baseOffset);
        baseOffsetInst->insertAfter(inst);
        baseAddrInst->insertAfter(baseOffsetInst);
        inst->unlink();
        return baseAddrInst;
    }
    return inst;
}


Inst*
CodeLowerer::caseTauArrayLen(Inst *inst)
{
    const OptimizerFlags& optimizerFlags = _irm.getOptimizerFlags(); 
    if (optimizerFlags.reduce_compref) {
        // reduce this to addoffset(base, ldelemoffset)

        Opnd *dst = inst->getDst();
        Opnd *array = inst->getSrc(0);
        Opnd *tauNonNull = inst->getSrc(1);
        Opnd *tauIsArray = inst->getSrc(2);

        Type *arrayType = array->getType();
        assert(arrayType->isArrayType());
        ArrayType *aType = (ArrayType*)arrayType;
        Type *elemType = aType->getElementType();

        InstFactory& instFactory = _irm.getInstFactory();
        OpndManager& opndManager = _irm.getOpndManager();
        TypeManager& typeManager = _irm.getTypeManager();

        SsaTmpOpnd* lenOffset = opndManager.createSsaTmpOpnd(typeManager.getOffsetType());
        SsaTmpOpnd* lenAddr = opndManager.createSsaTmpOpnd(typeManager.getManagedPtrType(typeManager.getInt32Type()));
        
        Inst *lenOffsetInst = instFactory.makeLdArrayLenOffset(lenOffset,
                                                                elemType);
        Inst *lenAddrInst = instFactory.makeAddOffset(lenAddr, array,
                                                      lenOffset);

        Inst *ldLenInst = instFactory.makeTauLdInd(AutoCompress_No,
                                                   Type::Int32,
                                                   dst, lenAddr,
                                                   tauNonNull, tauIsArray);
        lenOffsetInst->insertAfter(inst);
        lenAddrInst->insertAfter(lenOffsetInst);
        ldLenInst->insertAfter(lenAddrInst);
        inst->unlink();
        return ldLenInst;
    }
    return inst;
}

Inst*
CodeLowerer::caseTauLdInd(Inst *inst)
{
    // handled in simplifier for now.
    return inst;
}

Inst*
CodeLowerer::caseLdRef(TokenInst *inst)
{
    // handled in simplifier for now.
    return inst;
}

Inst*
CodeLowerer::caseLdNull(ConstInst *inst)
{
    assert(0);
    return inst;
}

} //namespace Jitrino 
