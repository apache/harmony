/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyrighashTable ownership.
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


/*
 * NOTE: The code below is based on ideas from the paper "Operator Strength
 * Reduction" by Keith D. Cooper, L.Taylor Simpson, Christopher Vick. However
 * there are some differences between original alogorithm and the
 * implementation: 
 *
 * 1. We do not traverse SSA graph, but rely on "lazy" detection of induction
 * variables 
 *
 * 2. The first step is not an SCC-detection, but CFG traversal that finds
 * instruction of the following type: mul(iv, rc) or mul(rc, iv), (where iv -
 * induction variable, rc - region constant, which is understood as loop
 * invariant) and stores them uint32o cands vector. 
 *
 * 3. Useful step that prevents degradation: check if the induction variable is
 * used as array subscript in the loop.  If such a check returns true, we do not
 * perform an optimization. The reasoning behind is the following: we will not
 * be able to remove an old inductio variable in a case when it is used as an
 * array subcsript. Even though our tranformation will reduce mul, it will
 * increase register pres * sure and lengthen codepath. It's better to avoid
 * such effects 
 *
 * 4. Then we perform apply/reduce process to the instruction stored in cands
 * vector.
 *
 * On complexity: OSR performs depth first search on each step - generally the
 * complexity of this algorithm is exponential. However the SSA graphs are
 * usually sparse, thus the results are no harder than quadratic complexity so,
 * this otimization does not slow down the compilation in any noticable rate.   
 */

#include "osr.h"
#include "deadcodeeliminator.h"

namespace Jitrino {

DEFINE_SESSION_ACTION(OSRPass, osr, "Operator Strength Reduction")

static U_32 signof(int v){ return (v == 0) ? 0 : (v < 0 ? -1 : 1);}


/* The code below is based on loop_unroll processOpnd function. However it
 * gathers some additional OSR-specific information which is obsolele for
 * loop_unroll. 
 *
 * TODO: create flexible mechanism for gathering info on the operands which
 * would help to avoid code duplication from the one hand, and be customizable
 * from another hand 
 */
OSROpndInfo OSRInductionDetector::processOpnd(LoopTree* tree,
                                      LoopNode* loopHead,
                                      InstStack& defStack,
                                      const Opnd* opnd,
                                      iv_detection_flag flag){
    if (Log::isEnabled()) {
        Log::out() << "Processing opnd: ";
        opnd->print(Log::out());
        Log::out() << "\n";
    }

    OSROpndInfo result;
    Inst* defInst = opnd->getInst();

    if (std::find(defStack.begin(), defStack.end(), defInst) !=
        defStack.end()) {
        result.setType(OSROpndInfo::COUNTER);
        result.setIncrement(0);
        result.setOpnd((Opnd*) opnd);
        return result;
    }

    Opcode opcode = defInst->getOpcode();

    if (opcode == Op_LdConstant) {
        result.setType(OSROpndInfo::LD_CONST);
        result.setConst(defInst->asConstInst()->getValue().i4);
        result.setOpnd((Opnd*) opnd);
        result.setHeader((Opnd*) opnd);
        result.setHeaderFound();
        return result;
    }

    if (!inExactLoop(tree, (Opnd*) opnd, loopHead)) {
        result.setOpnd((Opnd*) opnd);
        result.setHeader((Opnd*) opnd);
        result.setHeaderFound();
        return result;
    }

    defStack.push_back(defInst);

    if (opcode == Op_Phi) {
        OSROpndInfo info1 =
            processOpnd(tree, loopHead, defStack, defInst->getSrc(0));

        if (defInst->getNumSrcOperands() > 1) {
            OSROpndInfo info2 =
                processOpnd(tree, loopHead, defStack, defInst->getSrc(1));
            if ( ((info1.isCounter() && !info1.isPhiSplit())
                   && (info2.isDOL() || info2.isLDConst()))
                 || ((info2.isCounter() && !info2.isPhiSplit())
                   && (info1.isDOL() || info1.isLDConst())) ) {

                result.setType(OSROpndInfo::COUNTER);
                result.setIncrement(info1.isCounter()? info1.
                                    getIncrement() : info2.getIncrement());
                result.markPhiSplit();
                result.setHeader((Opnd*) opnd);
                result.setHeaderFound();
            } else if ((flag == CHOOSE_MAX_IN_BRANCH) && info1.isCounter()
                       && info2.isCounter()
                       && signof(info1.getIncrement()) ==
                       signof(info2.getIncrement())) {

                result.setType(OSROpndInfo::COUNTER);
                result.setIncrement(std::abs(info1.getIncrement()) >
                                    std::abs(info2.getIncrement())? info1.
                                    getIncrement() : info2.getIncrement());
                result.markPhiSplit();
                result.setHeader((Opnd*) opnd);
                result.setHeaderFound();
            } else {
                result.setType(OSROpndInfo::UNDEF);
            }
        }
    } else if (opcode == Op_Add || opcode == Op_Sub) {
        Opnd* opnd1 = defInst->getSrc(0);
        Opnd* opnd2 = defInst->getSrc(1);
        OSROpndInfo info1 = processOpnd(tree, loopHead, defStack, opnd1);
        OSROpndInfo info2 = processOpnd(tree, loopHead, defStack, opnd2);

        if ((info1.isLDConst() || info1.isDOL())
            && (info2.isLDConst() || info2.isDOL())) {
            if (info1.isLDConst() && info2.isLDConst()
                && info1.getConst() == info2.getConst()) {
                result.setType(OSROpndInfo::LD_CONST);
                result.setConst(info1.getConst());
                writeHeaderToResult(result, tree, info1, info2);
            }
        } else if ((info1.isCounter() && info2.isLDConst())
                   || (info2.isCounter() && info1.isLDConst())) {
            U_32 increment = info1.isCounter() ?
                info1.getIncrement() : info2.getIncrement();
            U_32 diff = info1.isLDConst()? info1.getConst() : info2.getConst();
            bool monotonousFlag = increment == 0 || diff == 0
                || (opcode == Op_Add && signof(diff) == signof(increment))
                || (opcode == Op_Sub && signof(diff) != signof(increment));
            if (monotonousFlag) {
                result.setType(OSROpndInfo::COUNTER);
                if ((info1.isCounter() && info1.isPhiSplit()) ||
                    (info2.isCounter() && info2.isPhiSplit())) {
                    result.markPhiSplit();
                    writeHeaderToResult(result, tree, info1, info2);
                }
                if (opcode == Op_Add) {
                    result.setIncrement(increment + diff);
                    writeHeaderToResult(result, tree, info1, info2);
                } else {
                    result.setIncrement(increment - diff);
                    writeHeaderToResult(result, tree, info1, info2);
                }
            } else {
                result.setType(OSROpndInfo::UNDEF);
            }
        } else {
            result.setType(OSROpndInfo::UNDEF);
        }
    } else if (opcode == Op_StVar || opcode == Op_LdVar) {
        Opnd* newOpnd = defInst->getSrc(0);
        result = processOpnd(tree, loopHead, defStack, newOpnd);
    } else if (opcode == Op_TauArrayLen) {
        Opnd* arrayOpnd = defInst->getSrc(0);
        result = processOpnd(tree, loopHead, defStack, arrayOpnd);
    } else {
        result.setType(OSROpndInfo::UNDEF);
    }

    defStack.pop_back();
    result.setOpnd((Opnd*) opnd);
    return result;
}

bool OSRInductionDetector::inExactLoop(LoopTree* tree, Opnd* opnd,
                                       LoopNode* curnode){
    LoopNode* lnode = tree->getLoopNode(opnd->getInst()->getNode(), false);
    if (lnode == 0) {
        return false;
    } else if (lnode == curnode) {
        return true;
    }
    return false;
}

bool OSRInductionDetector::inLoop(LoopTree* tree, Opnd* opnd){
    LoopNode* lnode = tree->getLoopNode(opnd->getInst()->getNode(), false);
    if (lnode == 0) {
        return false;
    }
    return true;
}

void OSRInductionDetector::writeHeaderToResult(OSROpndInfo& result,
                                               LoopTree* tree,
                                               OSROpndInfo info1,
                                               OSROpndInfo info2){
    if (result.isCounter()) {
        bool opnd1_in_loop = inLoop(tree, info1.getOpnd());
        bool opnd2_in_loop = inLoop(tree, info2.getOpnd());
        if (opnd1_in_loop) {
            result.setHeader(info1.getHeader());
        } else if (opnd2_in_loop) {
            result.setHeader(info2.getHeader());
        }
        result.setHeaderFound();

    } else {
        if (info2.isHeaderFound()) {
            result.setHeader(info2.getHeader());
            result.setHeaderFound();
        } else if (info1.isHeaderFound()) {
            result.setHeader(info1.getHeader());
            result.setHeaderFound();
        }
    }
}

OSR::OSR(IRManager & irManager0, 
         MemoryManager & memManager, 
         LoopTree* loop_Tree, 
         DominatorTree* dtree): iv(0),
                                 rc(0),
                                 irManager(irManager0),
                                 mm(memManager),
                                 loopTree(loop_Tree),
                                 hashTable(new CSEHashTable(memManager)),
                                 dominators(dtree),
                                 leading_operands(memManager),
                                 addedOpnds(memManager), 
                                 cands(memManager),
                                 LFTRHashMap(memManager){}

void OSRPass::_run(IRManager & irm){
    splitCriticalEdges(irm);
    computeDominatorsAndLoops(irm);
    LoopTree* loopTree = irm.getLoopTree();
    DominatorTree* dominatorTree = irm.getDominatorTree();
    OSR osr(irm, irm.getMemoryManager(), loopTree, dominatorTree);
    osr.runPass();
}

void OSR::recordIVRC(Inst* inst){

    if (Log::isEnabled()) {
        Log::out() << "Processing: ";
        inst->print(Log::out());
        Log::out() << "\n";
    }

    Opnd* opnd1 = inst->getSrc(0);
    Opnd* opnd2 = inst->getSrc(1);

    LoopNode* lnode = loopTree->getLoopNode(inst->getNode(), false);
    if (lnode == 0)
        return;

    OSRInductionDetector::InstStack defstack(mm);
    OSROpndInfo info1 = OSRInductionDetector::processOpnd(loopTree, lnode, defstack, opnd1);
    OSROpndInfo info2 = OSRInductionDetector::processOpnd(loopTree, lnode, defstack,opnd2);

    if (Log::isEnabled()) {
        info1.print(Log::out());
        info2.print(Log::out());

    }
    if (info1.isCounter() && (info2.isLDConst() || info2.isDOL())) {
        iv = (SsaOpnd*) info1.getOpnd();
        rc = (SsaOpnd*) info2.getOpnd();
        bool no_array = isNoArrayInLoop(lnode, iv);
        if (no_array) {
            writeLeadingOperand((SsaOpnd*) opnd1,
                                (SsaOpnd*) info1.getHeader());
        } else {
            iv = 0;
            rc = 0;
        }
    } else if (info2.isCounter() && (info1.isLDConst() || info1.isDOL())) {
        iv = (SsaOpnd*) info2.getOpnd();
        rc = (SsaOpnd*) info1.getOpnd();

        bool no_array = isNoArrayInLoop(lnode, iv);

        if (no_array) {
            writeLeadingOperand((SsaOpnd*) opnd2,
                                (SsaOpnd*) info2.getHeader());
        } else {
            iv = 0;
            rc = 0;
        }
    }
}

bool OSR::isNoArrayInLoop(LoopNode* lnode, SsaOpnd* iv){
    Nodes nodes = lnode->getNodesInLoop();
    StlVector < Node* >::iterator iter = nodes.begin(), end = nodes.end();

    for (; iter != end; iter++) {
        Node* node = *iter;
        Inst* last_inst = (Inst*) node->getLastInst();

        for (Inst* iter1 = (Inst*) node->getFirstInst();
             iter1 != last_inst; iter1 = iter1->getNextInst()) {
            Inst inst = *iter1;
            if (inst.getOpcode() == Op_AddScaledIndex) {
                Opnd* opnd = inst.getSrc(1);
                findLeadingOpnd(&inst, (SsaOpnd*) opnd);
                SsaOpnd* lop = getLeadingOperand((SsaOpnd*) opnd);
                if (lop != 0) {
                    SsaOpnd* ivlop = getLeadingOperand(iv);
                    if (lop == ivlop) {
                        return false;
                    }

                }

            }
        }
    }
    return true;
}

void OSR::writeInst(Inst* inst){
    Opcode opcode = inst->getOpcode();
    if (opcode == Op_Mul) {
        recordIVRC(inst);
        if (iv != 0 && rc != 0) {
            reduceCand red_c((SsaOpnd*) inst->getDst(), iv, rc);
            cands.push_back(red_c);
        }
        iv = 0;
        rc = 0;
    }
}

void OSR::runPass(){
    StlVector < Node* >nodes(mm);
    ControlFlowGraph& flowgraph = irManager.getFlowGraph();
    flowgraph.getNodes(nodes);

    StlVector < Node* >::iterator iter = nodes.begin(), end = nodes.end();

    for (; iter != end; iter++) {
        Node* node = *iter;
        Inst* last_inst = (Inst*) node->getLastInst();

        for (Inst* iter1 = (Inst*) node->getFirstInst();
             iter1 != last_inst; iter1 = iter1->getNextInst()) {
            writeInst(iter1);
        }
    }

    StlVector < reduceCand >::iterator viter = cands.begin();
    StlVector < reduceCand >::iterator vend = cands.end();

    for (; viter != vend; viter++) {
        reduceCand rcand = *viter;
        replace(rcand.dst, rcand.iv, rcand.rc);
    }

    StlVector < Node* >nnodes(mm);
    ControlFlowGraph& ffg = irManager.getFlowGraph();
    ffg.getNodes(nnodes);
    replaceLinearFuncTest(nnodes);

}

void OSR::writeLeadingOperand(SsaOpnd* opnd, SsaOpnd* header){
    leading_operands[opnd] = header;
    if (Log::isEnabled()) {
        Log::out() << "Header of: " << std::endl;
        opnd->print(Log::out());
        Log::out() << "is" << std::endl;
        if (header) {
            header->print(Log::out());
            Log::out() << std::endl;
        } else {
            Log::out() << "Null" << std::endl;
        }
    }
}

SsaOpnd* OSR::getLeadingOperand(SsaOpnd* opnd){
    if (opnd == 0) {
        return 0;
    }
    SsaOpnd* result = 0;
    if (leading_operands.has(opnd)) {
        result = leading_operands[opnd];
    }
    return result;
}

void OSR::replace(SsaOpnd* opnd, SsaOpnd* iv, SsaOpnd* rc){
    Inst* inst = opnd->getInst();
    SsaOpnd* dstInst = reduce(inst->getDst()->getType(), inst->getOpcode(),
                              inst->getOperation(), iv, rc);
    Inst* copyInst = irManager.getInstFactory().makeCopy(opnd, dstInst);
    copyInst->insertBefore(inst);
    inst->unlink();
    writeLeadingOperand(opnd, getLeadingOperand(iv));

}

Inst* OSR::insertNewDef(Type* type, SsaOpnd* iv, SsaOpnd* rc){
    Inst* inst = copyInst(type, iv->getInst(), rc,
                          Operation(iv->getInst()->getOpcode()));
    insertInst(inst, iv->getInst());
    return inst;
}

void OSR::findLeadingOpnd(Inst* newDef, SsaOpnd* opnd){
    Node* node = newDef->getNode();
    LoopNode* lnode = this->loopTree->getLoopNode(node, false);
    OSRInductionDetector::InstStack newstack(this->mm);
    OSROpndInfo oinfo = OSRInductionDetector::processOpnd(loopTree, lnode, newstack, opnd);
    if (oinfo.isHeaderFound()) {
        writeLeadingOperand(opnd, (SsaOpnd*) oinfo.getHeader());
    }
}

void OSR::replaceOperand(U_32 num, Inst* inst, SsaOpnd* opnd,
                         Opnd* iv_lead, Node* iv_lead_node,
                         Type* type, Opcode opcode, SsaOpnd* rc,
                         Operation op){
    findLeadingOpnd(inst, opnd);
    SsaOpnd* opnd_header = getLeadingOperand(opnd);
    Node* opnd_headerNode = 0;

    if (opnd_header != 0) {
        opnd_headerNode = opnd_header->getInst()->getNode();
    }

    if ((iv_lead != 0) && (opnd_headerNode == iv_lead_node)) {
        inst->setSrc(num, reduce(type, opcode, op, opnd, rc));
    } else if ((opcode == Op_Mul) || (inst->getOpcode() == Op_Phi)) {
        SsaOpnd* newOpnd = apply(type, opcode, op, opnd, rc);
        if (opnd->isSsaVarOpnd()) {
            newOpnd = makeVar(newOpnd, inst->getDst()->asSsaVarOpnd(),
                              opnd->getInst());
        } else {
            newOpnd = makeTmp(newOpnd->asSsaOpnd(), opnd->getInst());
        }
        inst->setSrc(num, newOpnd);
    }
}

void OSR::replaceOperands(Type* type, Inst* inst, SsaOpnd* iv,
                          SsaOpnd* rc, Opcode opcode, Operation op){

    if (Log::isEnabled()) {
        Log::out() << "Replacing operands" << std::endl;
        Log::out() << "RC" << std::endl;
        rc->print(Log::out());
        Log::out() << "IV" << std::endl;
        iv->print(Log::out());
        Log::out() << std::endl;
    }

    U_32 numOpnds = inst->getNumSrcOperands();
    SsaOpnd* iv_lead = getLeadingOperand(iv);
    Node* iv_lead_node = iv_lead->getInst()->getNode();
    for (U_32 num = 0; num < numOpnds; num++) {
        SsaOpnd* opnd = inst->getSrc(num)->asSsaOpnd();
        replaceOperand(num, inst, opnd, iv_lead, iv_lead_node, type,
                       opcode, rc, op);
    }

}

SsaOpnd* OSR::reduce(Type* type, Opcode opcode, Operation op,
                     SsaOpnd* iv, SsaOpnd* rc){
    if (Log::isEnabled()) {
        Log::out() << "Reducing: ";
        iv->print(Log::out());
        Log::out() << std::endl;
        rc->print(Log::out());
        Log::out() << std::endl;
    }
    Inst* newinst = hashTable->lookup(op.encodeForHashing(), iv->getId(), rc->getId());
    if (!newinst) {
        Inst* newDef = insertNewDef(type, iv, rc);
        if (Log::isEnabled()) {
            Log::out() << "NewDef" << std::endl;
            newDef->print(Log::out());
            Log::out() << std::endl;
        }

        SsaOpnd* result = newDef->getDst()->asSsaOpnd();
        writeLeadingOperand(result, getLeadingOperand(iv));
        hashTable->insert(op.encodeForHashing(), iv->getId(),
                          rc->getId(), newDef);
        writeLFTR(iv, op, rc, newDef->getDst()->asSsaOpnd());
        replaceOperands(type, newDef, iv, rc, opcode, op);
        return result;

    } else {
        SsaOpnd* result = newinst->getDst()->asSsaOpnd();
        return result;
    }
}

Inst* OSR::copyInst(Type* type, Inst* oldDef, SsaOpnd* rc, Operation op){
    Inst* newDef = 0;
    SsaOpnd* old = oldDef->getDst()->asSsaOpnd();
    if (old->isSsaVarOpnd()) {
        newDef = createNewVarInst(old, type, oldDef, rc, op);
    } else {
        InstFactory& instFactory = irManager.getInstFactory();
        OpndManager& opndManager = irManager.getOpndManager();
        OpndRenameTable opndRenameTable(mm);
        newDef = instFactory.clone(oldDef, opndManager, &opndRenameTable);
        newDef->getDst()->setType(type);
        newDef->setType(type->tag);
    }
    return newDef;
}

Inst* OSR::createNewVarInst(SsaOpnd* old, Type* type,
                            Inst* old_inst, SsaOpnd* rc, Operation op){
    InstFactory& instFactory = irManager.getInstFactory();
    OpndManager& opndManager = irManager.getOpndManager();

    VarOpnd* oldVar = old->asSsaVarOpnd()->getVar();
    VarOpnd* newVar = createOperand(type, oldVar, rc, op);
    SsaVarOpnd* newSsaVar = opndManager.createSsaVarOpnd(newVar);
    if (old_inst->getOpcode() == Op_StVar) {
        return instFactory.makeStVar(newSsaVar,
                                     old_inst->getSrc(0)->asSsaOpnd());
    } else {
        U_32 num = old_inst->getNumSrcOperands();
        Opnd** newOpnds = new(mm) Opnd* [num];
        for (U_32 i = 0; i < num; i++) {
            newOpnds[i] = old_inst->getSrc(i)->asSsaOpnd();
        }
        return (PhiInst*)instFactory.makePhi(newSsaVar, num, newOpnds);
    }
}

void OSR::insertInst(Inst* inst, Inst* place){
    if (inst->getOpcode() == Op_Phi || place->getOpcode() != Op_Phi) {
        inst->insertAfter(place);
    } else {
        do {
            place = (Inst*) place->next();
        }
        while ((place != 0) && (place->getOpcode() == Op_Phi));
        if (place != 0) {
            inst->insertBefore(place);
        }
    }
}

SsaOpnd* OSR::apply(Type* type, Opcode opcode, Operation op,
                    SsaOpnd* opnd1, SsaOpnd* opnd2){
    SsaOpnd* opnd2Leader = getLeadingOperand(opnd2);
    if (opnd2Leader!= 0) {
        opnd2 = opnd2Leader;
    }

    opnd1 = (SsaOpnd*) DeadCodeEliminator::copyPropagate(opnd1);

    if (Log::isEnabled()) {
        Log::out() << "Applying: ";
        opnd1->print(Log::out());
        Log::out() << std::endl;
        opnd2->print(Log::out());
        Log::out() << std::endl;
    }

    Inst* newinst =
        hashTable->lookup(op.encodeForHashing(), opnd1->getId(),
                          opnd2->getId());
    SsaOpnd* result = 0;
    if (newinst) {
        result = newinst->getDst()->asSsaOpnd();
    } else {
        if (getLeadingOperand(opnd1) && isLoopInvariant(opnd2)) {
            result = reduce(type, opcode, op, opnd1, opnd2);
        } else if (getLeadingOperand(opnd2) && isLoopInvariant(opnd1)) {
            result = reduce(type, opcode, op, opnd2, opnd1);
        } else {
            OpndManager & opndManager = irManager.getOpndManager();
            result = opndManager.createSsaTmpOpnd(type);
            InstFactory & instFactory = irManager.getInstFactory();
            opnd1 = makeTmp(opnd1, opnd1->getInst());
            opnd2 = makeTmp(opnd2, opnd2->getInst());

            Inst* newInst = instFactory.makeInst(opcode, op.getModifier(), type->tag,
                                                 result, opnd1, opnd2);
            Inst* instLoc = findInsertionPlace(opnd1, opnd2);
            insertInst(newInst, instLoc);
            writeLeadingOperand(result, 0);
            hashTable->insert(op.encodeForHashing(), opnd1->getId(),
                              opnd2->getId(), newInst);
            writeLFTR(opnd1, op, opnd2, newInst->getDst()->asSsaOpnd());

        }
    }
    return result;
}
Inst* OSR::findInsertionPlace(SsaOpnd* opnd1, SsaOpnd* opnd2){

    findLeadingOpnd(opnd2->getInst(), opnd2);
    SsaOpnd* opnd2Leader = getLeadingOperand(opnd2);
    if (opnd2Leader!= 0) {
      opnd2 = opnd2Leader;
    }

    Inst* opnd1inst = opnd1->getInst();
    Inst* opnd2inst = opnd2->getInst();
    Node* block1 = opnd1inst->getNode();
    Node* block2 = opnd2inst->getNode();
    Inst* place = 0;
    if (block1 == block2) {
        Inst* i = opnd1inst;
        place = opnd2inst;
        while (!i->isLabel()) {
            if (i == opnd2inst) {
                place = opnd1inst;
                break;
            }
            i = i->getPrevInst();
        }
    } else if ((block1 != 0) && (dominators->dominates(block1, block2))) {
        place = opnd2inst;
    } else {
        place = opnd1inst;
    }
    return place;
}

SsaTmpOpnd* OSR::makeTmp(SsaOpnd* inOpnd, Inst* place){
    if (inOpnd->isSsaVarOpnd()) {
        SsaVarOpnd* inSsaVarOpnd = inOpnd->asSsaVarOpnd();
        Inst* inst = inSsaVarOpnd->getInst();
        if (inst->getOpcode() == Op_StVar) {
            SsaTmpOpnd* res = inst->getSrc(0)->asSsaTmpOpnd();
            return res;
        } else {
            OpndManager& opndManager = irManager.getOpndManager();
            InstFactory& instFactory = irManager.getInstFactory();

            SsaTmpOpnd* res = opndManager.createSsaTmpOpnd(inOpnd->getType());
            Inst* ldVarInst = instFactory.makeLdVar(res, inSsaVarOpnd);
            Inst* where = chooseLocationForConvert(inst, place);
            insertInst(ldVarInst, where);
            writeLeadingOperand(res, getLeadingOperand(inOpnd));
            return res;
        }
    } else {
        return inOpnd->asSsaTmpOpnd();
    }
}

SsaVarOpnd* OSR::makeVar(SsaOpnd* inOpnd, SsaVarOpnd* var, Inst* place){

    if (inOpnd->isSsaTmpOpnd()) {
        Inst* inst = inOpnd->getInst();
        if (inst->getOpcode() == Op_LdVar) {
            SsaVarOpnd* res = inst->getSrc(0)->asSsaVarOpnd();
            return res;
        } else {
            OpndManager& opndManager = irManager.getOpndManager();
            InstFactory& instFactory = irManager.getInstFactory();

            SsaVarOpnd* res = opndManager.createSsaVarOpnd(var->getVar());
            Inst* stVarInst = instFactory.makeStVar(res, inOpnd);

            Inst* where = chooseLocationForConvert(inst, place);
            insertInst(stVarInst, where);
            writeLeadingOperand(res, getLeadingOperand(inOpnd));
            return res;
        }
    } else {
        return inOpnd->asSsaVarOpnd();
    }
}

Inst* OSR::chooseLocationForConvert(Inst* inst, Inst* place){
    Node* instNode = inst->getNode();
    Node* placeNode = place->getNode();
    Inst* where = 0;
    if (instNode == placeNode) {
        where = inst;
    } else {
        if (dominators->dominates(instNode, placeNode)) {
            where = place;
        } else {
            where = inst;
        }
    }
    return where;

}

VarOpnd* OSR::createOperand(Type* newType, VarOpnd * var, SsaOpnd * rc,
                            Operation op){
    OldInst g;
    g.type = newType;
    g.var = var;
    g.ssa = rc;
    Entry key(g, op);

    VarOpnd* newVar = addedOpnds[key];

    if (newVar == 0) {
        OpndManager & opndManager = irManager.getOpndManager();
        newVar = opndManager.createVarOpnd(newType, false);
        addedOpnds[key] = newVar;
    }
    return newVar;
}

bool OSR::isLoopInvariant(SsaOpnd* name){

    OSRInductionDetector::InstStack stack(this->mm);
    LoopNode* mynode = this->loopTree->getLoopNode(name->getInst()->getNode(), false);
    if (mynode == 0) {
        return false;
    }
    OSROpndInfo nameinfo = OSRInductionDetector::processOpnd(loopTree, mynode, stack, name);
    if (nameinfo.isHeaderFound()) {
        writeLeadingOperand(name, (SsaOpnd*) nameinfo.getHeader());
    }
    if (nameinfo.isDOL() || nameinfo.isLDConst()) {
        return true;
    } else {
        return false;
    }

}

void OSR::writeLFTR(SsaOpnd* iv, Operation op, SsaOpnd* loop_inv,
                    SsaOpnd* result){
    OperationSsaOpnd tmp1(op, loop_inv);
    LFTREntry tmp2(tmp1, result);
    LFTRHashMap[iv] = tmp2;
}

void OSR::replaceLinearFuncTest(StlVector < Node* >&postOrderNodes){
    StlVector < Node* >::reverse_iterator riter = postOrderNodes.rbegin(),
        rend = postOrderNodes.rend();
    for (; riter != rend; ++riter) {
        Node* node = *riter;
        Inst* labelInst = (Inst*) node->getLabelInst();

        for (Inst* iter = (Inst*) labelInst->next();
             (iter != 0 && iter != labelInst);
             iter = (Inst*) iter->next()) {
            if ((iter->getOpcode() == Op_Cmp)
                || (iter->getOpcode() == Op_Branch))
                performLFTR(iter);

        }
    }
}

void OSR::performLFTR(Inst* inst){
    Opcode opcode = inst->getOpcode();
    if (opcode == Op_Cmp || opcode == Op_Branch) {
        U_32 num = inst->getNumSrcOperands();
        if (2 == num) {
            iv = 0;
            rc = 0;
            recordIVRC(inst);
            if (iv && rc) {
                SsaOpnd* reduced = 0;
                SsaOpnd* newbound = 0;
                SsaOpnd* src = getLeadingOperand(iv);
                if (src == 0)
                    return;
                reduced = followEdges(iv);
                newbound = applyEdges(iv, rc);
                if ((reduced != src) && (newbound != rc)) {
                    reduced = makeTmp(reduced, iv->getInst());
                    newbound = makeTmp(newbound, rc->getInst());
                    inst->setType(reduced->getType()->tag);

                    if ((inst->getOpcode() == Op_Cmp
                         || inst->getOpcode() == Op_Branch)
                        && inst->getComparisonModifier() == Cmp_GT) {
                        inst->setSrc(1, reduced);
                        inst->setSrc(0, newbound);
                    } else
                        if ((inst->getOpcode() == Op_Cmp
                             || inst->getOpcode() == Op_Branch)
                            && inst->getComparisonModifier() == Cmp_GTE) {
                        inst->setSrc(0, reduced);
                        inst->setSrc(1, newbound);

                    } else {
                        inst->setSrc(1, reduced);
                        inst->setSrc(0, newbound);
                    }
                }
            }
            iv = 0;
            rc = 0;
        }
    }
}

SsaOpnd* OSR::followEdges(SsaOpnd* iv){
    if (!LFTRHashMap.has(iv)) {
        return iv;
    } else {
        LFTREntry & tmp2 = LFTRHashMap[iv];
        SsaOpnd* reduced = tmp2.second;
        return followEdges(reduced);
    }
}

SsaOpnd* OSR::applyEdges(SsaOpnd* iv, SsaOpnd* bound){
    SsaOpnd* res = 0;
    if (!LFTRHashMap.has(iv)) {
        res = bound;
    } else {
        LFTREntry & tmp2 = LFTRHashMap[iv];
        Operation op = tmp2.first.first;
        SsaOpnd* rc = tmp2.first.second;
        SsaOpnd* reduced = tmp2.second;
        Opcode opcode = op.getOpcode();
        SsaOpnd* newRC = apply(reduced->getType(), opcode, op, bound, rc);
        res = applyEdges(reduced, newRC);
    }
    return res;
}
}
