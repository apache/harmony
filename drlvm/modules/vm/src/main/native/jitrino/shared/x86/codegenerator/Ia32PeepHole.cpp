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
 * @author Alexander Astapchuk
 */

#include "Ia32CgUtils.h"

namespace Jitrino {
namespace Ia32 {

class PeepHoleOpt;
static const char* help = 
"Performs simple local (per-BB) or per-Inst optimizations.\n"
"Some of them include:\n"
"\t Inlined F2I conversion\n"
"A better instructions selection:\n"
"\t Change 32bit immediate values to 8bit in ALU instructions\n"
"\t MOVSS/MOVSD replaced with MOVQ\n"
"\t MOVSS/MOVSD xmm, [memconst=0.] => PXOR xmm, xmm\n"
"It's recommended to have 2 passes of peephole: the first one before\n"
"a register allocator - to inline the conversions and provide more\n"
"opportunities for further optimization. And the second one - after\n"
"the register allocator to improve the instructions selection."
;

static ActionFactory<PeepHoleOpt> _staticAutoRegister("peephole", help);

class PeepHoleOpt : 
    public SessionAction, 
    protected OpndUtils, 
    protected InstUtils, 
    protected SubCfgBuilderUtils 
{
private:
    // Virtuals
    U_32  getSideEffects(void) const
    {
        return m_bHadAnyChange ? (U_32)-1 : 0;
    }
private:
    enum Changed { 
    /// Nothing was changed
    Changed_Nothing, 
    /**
     * One or more Opnds were changed/added/removed - might need to 
     * update liveness info.
     */
    Changed_Opnd, 
    /**
     * One or more Insts were changed/added/removed - might need to 
     * update Insts list.
     */
    Changed_Inst, 
    /**
     * One or more Nodes were changed/added/removed - might need to 
     * update Nodes list.
     */
    Changed_Node
    };
    //
    // General machinery. 
    // TODO: It's better to separate the general CFG-walking machinery into
    // a separate class.
    //
    void runImpl(void);
    Changed handleBasicBlock(Node* node);
    Changed handleInst(Inst* inst);
    //
    // 
    //
    Changed handleInst_MOV(Inst* inst);
    Changed handleInst_Call(Inst* inst);
    Changed handleInst_InternalHelperCall(Inst* inst, const  Opnd::RuntimeInfo* ri);
    Changed handleInst_Convert_F2I_D2I(Inst* inst);
    Changed handleInst_ALU(Inst* inst);
    Changed handleInst_MUL(Inst* inst);
    Changed handleInst_SSEMov(Inst* inst);
    Changed handleInst_SSEXor(Inst* inst);
    Changed handleInst_CMP(Inst* inst);
	Changed handleInst_SETcc(Inst* inst);

    //
    // Helpers
    //
    //
    bool m_bHadAnyChange;

    // For local propagation from new generated MOV
    StlHashMap<Opnd*, Opnd*> *copyMap;
    StlSet<Opnd*> *tempSet;
}; // ~PeepHoleOpt

void PeepHoleOpt::runImpl(void)
{
    MemoryManager mm("handleBasicBlock");
    copyMap = new(mm) StlHashMap<Opnd*, Opnd*>(mm);
    tempSet = new(mm) StlSet<Opnd*>(mm);
    setIRManager(irManager);
    irManager->calculateOpndStatistics();
    m_bHadAnyChange = false;
    // organize an infinity loop and keep spinning till we have any change.
    // thought have a safety counter to prevent a really infinity in case 
    // anything goes wrong in runtime
    bool keepGoing = true;
    unsigned safetyCounter = 0;
    do {
        keepGoing = false;
        const Nodes& nodes = irManager->getFlowGraph()->getNodes();
        for (Nodes::const_iterator citer = nodes.begin(); 
            citer != nodes.end(); ++citer) {
            Node* node = *citer;
            if (!node->isBlockNode()) {
                continue;
            }
            Changed whatChanged = handleBasicBlock(node);
            if (whatChanged != Changed_Nothing) {
                m_bHadAnyChange = true;
                keepGoing = true;
            }
            if (whatChanged == Changed_Node) {
                break;
            }
        }
        ++safetyCounter;
        if(safetyCounter > 100000) {
            // I hardly believe in a method that has more than 100K 
            // opportunities to fix in peephole. 
            // Most probably self bug - assert() in debug mode, stop trying
            // in release.
            assert(false);
            keepGoing = false;
        }
    } while(keepGoing);
}


PeepHoleOpt::Changed PeepHoleOpt::handleBasicBlock(Node* node)
{
    copyMap->clear();
    tempSet->clear();
    Inst* inst = (Inst*)node->getFirstInst();
    Changed changedInBB = Changed_Nothing;
    while (inst != NULL) {
        Inst* savePrev = inst->getPrevInst();
        Changed whatChanged = handleInst(inst);
        if (whatChanged == Changed_Node) {
            // Need to scan the CFG again.
            return Changed_Node;
        }
        Inst* next = NULL;
        if (whatChanged == Changed_Inst) {
            changedInBB = Changed_Inst;
            // Inst was replaced, or deleted, or new Inst was added - 
            // proceed with this new or updated instruction(s) again
            if (savePrev == NULL) {
                next = (Inst*)node->getFirstInst();
            }
            else {
                next = savePrev->getNextInst();
            }
        }
        else {
            assert(whatChanged == Changed_Nothing || whatChanged == Changed_Opnd);
            if (changedInBB != Changed_Nothing) {
                changedInBB = whatChanged;
            }
            next = inst->getNextInst();
        }
        inst = next;
    }
    return changedInBB;
}

static bool isTypeConversionAllowed(Opnd* fromOpnd, Opnd* toOpnd) {
    Type * fromType = fromOpnd->getType();
    Type * toType = toOpnd->getType();
    bool fromIsGCType = fromType->isObject() || fromType->isManagedPtr();
    bool toIsGCType = toType->isObject() || toType->isManagedPtr();
    return fromIsGCType == toIsGCType;
}

PeepHoleOpt::Changed PeepHoleOpt::handleInst(Inst* inst)
{
    PeepHoleOpt::Changed temp;

    // Local propagation
    Inst::Opnds opnds(inst, Inst::OpndRole_All);
    
    for (Inst::Opnds::iterator it=opnds.begin();it != opnds.end();it = opnds.next(it)) {
        Opnd * opnd=inst->getOpnd(it);
        U_32 roles=inst->getOpndRoles(it);
                
        if (roles & Inst::OpndRole_Use) {
            if ((roles & Inst::OpndRole_All & Inst::OpndRole_FromEncoder) 
                && (roles & Inst::OpndRole_All & Inst::OpndRole_ForIterator)
                && (roles & Inst::OpndRole_Changeable) && ((roles & Inst::OpndRole_Def) == 0)
                && copyMap->has(opnd)) {
                if (opnd->getType()->isUnmanagedPtr() && (*copyMap)[opnd]->getType()->isInteger())
                    (*copyMap)[opnd]->setType(opnd->getType());
                inst->setOpnd(it, (*copyMap)[opnd]);
            }
        }
    }

    for (Inst::Opnds::iterator it = opnds.begin();it != opnds.end();it = opnds.next(it)) {
        Opnd * opnd=inst->getOpnd(it);
        U_32 roles=inst->getOpndRoles(it);

        if (roles & Inst::OpndRole_Def) {
            if (copyMap->has(opnd)) {
            	if (Log::isEnabled()) Log::out()<<"copy relation DELETED: " << opnd->getFirstId() << "<=" << (*copyMap)[opnd]->getFirstId() <<std::endl;
                copyMap->erase(opnd);
            }

            tempSet->clear();
            for(StlHashMap<Opnd*, Opnd*>::iterator iter=copyMap->begin();
                iter!=copyMap->end();++iter)
                if (iter->second == opnd) {
                    if (Log::isEnabled()) Log::out()<<"copy relation DELETED: " << iter->first->getFirstId() << "<=" << iter->second->getFirstId() <<std::endl;
                    tempSet->insert(iter->first);
                }
            for(StlSet<Opnd*>::iterator iter=tempSet->begin();
                iter!=tempSet->end();++iter)
                copyMap->erase(*iter);
        }
    }

    if (inst->getMnemonic() == Mnemonic_MOV) {
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
            bool typeConvOk = src->getSize() == dst->getSize() && isTypeConversionAllowed(src, dst);
            if (typeConvOk && kindsAreOk && ! src->isPlacedIn(OpndKind_Reg)) {
                if (copyMap->has(src)) {
                    (*copyMap)[dst] = (*copyMap)[src];
                    if (Log::isEnabled()) Log::out()<<"copy relation INSERTED: " << dst->getFirstId() << "<=" << (*copyMap)[src]->getFirstId() <<std::endl;
                } else {
                    (*copyMap)[dst] = src;
                    if (Log::isEnabled()) Log::out()<<"copy relation INSERTED: " << dst->getFirstId() << "<=" << src->getFirstId() <<std::endl;
                }
            }
        }
    }
            
    if (inst->hasKind(Inst::Kind_PseudoInst) && inst->getKind() != Inst::Kind_CopyPseudoInst) {
        return Changed_Nothing;
    }

    Mnemonic mnemonic = inst->getMnemonic();
    switch(mnemonic) {
    case Mnemonic_MOV:
        return handleInst_MOV(inst);
    case Mnemonic_CALL:
        return handleInst_Call(inst);
    case Mnemonic_ADD:
    case Mnemonic_ADC:
    case Mnemonic_SUB:
    case Mnemonic_SBB:
    case Mnemonic_NOT:
    case Mnemonic_AND:
    case Mnemonic_OR:
    case Mnemonic_XOR:
    case Mnemonic_TEST:
        return handleInst_ALU(inst);
    case Mnemonic_CMP:
    temp = handleInst_CMP(inst);
    if ( temp == Changed_Nothing ) {
        return handleInst_ALU(inst); 
    } else {
        return temp;
    }
    case Mnemonic_SETG:
    case Mnemonic_SETE:
    case Mnemonic_SETNE:
    case Mnemonic_SETL:
		return handleInst_SETcc(inst);
    case Mnemonic_IMUL:
    case Mnemonic_MUL:
        return handleInst_MUL(inst);
    case Mnemonic_MOVSS:
    case Mnemonic_MOVSD:
        //return handleInst_SSEMov(inst);
    case Mnemonic_XORPS:
    case Mnemonic_XORPD:
        //return handleInst_SSEXor(inst);
    default:
        break;
    }
    return Changed_Nothing;
}

PeepHoleOpt::Changed PeepHoleOpt::handleInst_Call(Inst* inst)
{
    assert(inst->getMnemonic() == Mnemonic_CALL);
    CallInst* callInst = (CallInst*)inst;
    unsigned targetOpndIndex = callInst->getTargetOpndIndex();
    Opnd* targetOpnd = callInst->getOpnd(targetOpndIndex);
    Opnd::RuntimeInfo* ri = targetOpnd->getRuntimeInfo();
    Opnd::RuntimeInfo::Kind rt_kind = Opnd::RuntimeInfo::Kind_Null;
    if (ri != NULL) {
        rt_kind = ri->getKind();
    }

    if (Opnd::RuntimeInfo::Kind_InternalHelperAddress == rt_kind) {
        return handleInst_InternalHelperCall(inst, ri);
    }
    return Changed_Nothing;
}

PeepHoleOpt::Changed PeepHoleOpt::handleInst_InternalHelperCall(
    Inst* inst, 
    const Opnd::RuntimeInfo* ri)
{
    assert(Opnd::RuntimeInfo::Kind_InternalHelperAddress == ri->getKind());
    /** The value of the operand is irManager.getInternalHelperInfo((const char*)[0]).pfn */
    char* rt_data = (char*)(ri->getValue(0));

    if (strcmp(rt_data,"convF4I4") == 0) {
        return handleInst_Convert_F2I_D2I(inst);
    } else if (strcmp(rt_data,"convF8I4") == 0) {
        return handleInst_Convert_F2I_D2I(inst);
    }
    return Changed_Nothing;
}

PeepHoleOpt::Changed PeepHoleOpt::handleInst_Convert_F2I_D2I(Inst* inst)
{
    //
    // Inline 'int_value = (int)(float_value or double_value)'
    //
    Opnd* dst = inst->getOpnd(0);
    Opnd* src = inst->getOpnd(2);
    Type* srcType = src->getType();
    assert(srcType->isSingle() || srcType->isDouble());
    assert(dst->getType()->isInt4());
    const bool is_dbl = srcType->isDouble();
    // Here, we might have to deal with 3 cases with src (_value):
    // 1. Unassigned operand - act as if were operating with XMM
    // 2. Assigned to FPU - convert to FPU operations, to 
    //    avoid long FPU->mem->XMM chain
    // 3. Assigned to XMM - see #1
    const bool xmm_way = 
        !(src->hasAssignedPhysicalLocation() && src->isPlacedIn(OpndKind_FPReg));

    if (!xmm_way) {
        //TODO: will add FPU later if measurements show it worths trying
        return Changed_Nothing;
    }
    //
    //
    /*
        movss xmm0, val
        // presuming the corner cases (NaN, overflow) 
        // normally happen rare, do conversion first, 
        // and check for falls later
    -- convertNode
        cvttss2si eax, xmm0
    -- ovfTestNode
        // did overflow happen ?
        cmp eax, 0x80000000 
        jne _done               // no - go return result
    -- testAgainstZeroNode
        // test SRC against zero
        comiss xmm0, [fp_zero]
        // isNaN ? 
        jp _nan     // yes - go load 0
    -- testIfBelowNode
        // xmm < 0 ?
        jb _done    // yes - go load MIN_INT. EAX already has it - simply return.
    -- loadMaxIntNode 
        // ok. at this point, XMM is positive and > MAX_INT
        // must load MAX_INT which is 0x7fffffff.
        // As EAX has 0x80000000, then simply substract 1
        sub eax, 1
        jmp _done
    -- loadZeroNode
    _nan:
        xor eax, eax
    -- nodeNode
    _done:
        mov result, eax
    }
    */
    Opnd* fpZeroOpnd = getZeroConst(srcType);
    Type* int32type = irManager->getTypeManager().getInt32Type();
    Opnd* oneOpnd = irManager->newImmOpnd(int32type, 1);
    Opnd* intZeroOpnd = getIntZeroConst();

    // 0x8..0 here is not the INT_MIN, but comes from the COMISS 
    // opcode description instead.
    Opnd* minIntOpnd = irManager->newImmOpnd(int32type, 0x80000000);

    newSubGFG();
    Node* entryNode = getSubCfgEntryNode();

    Node* convertNode = newBB();
    Node* ovfTestNode = newBB();
    Node* testAgainstZeroNode = newBB();
    Node* testIfBelowNode = newBB();
    Node* loadMaxIntNode = newBB();
    Node* loadZeroNode = newBB();
    Node* doneNode = newBB();
    //
    // presuming the corner cases (NaN, overflow) 
    // normally happen rare, do conversion first, 
    // and check for falls later
    //
    connectNodes(entryNode, convertNode);
    //
    // convert
    //
    setCurrentNode(convertNode)    ;
    Mnemonic mn_cvt = is_dbl ? Mnemonic_CVTTSD2SI : Mnemonic_CVTTSS2SI;
    /*cvttss2si r32, xmm*/ newInst(mn_cvt, 1, dst, src);
    connectNodeTo(ovfTestNode);
    setCurrentNode(NULL);

    //
    // check whether overflow happened
    //
    setCurrentNode(ovfTestNode);
    /*cmp r32, MIN_INT*/ newInst(Mnemonic_CMP, dst, minIntOpnd);
    /*jne _done       */ newBranch(Mnemonic_JNE, doneNode, testAgainstZeroNode, 0.9, 0.1);
    //
    setCurrentNode(NULL);

    // test SRC against zero
    //
    setCurrentNode(testAgainstZeroNode);
    Mnemonic mn_cmp = is_dbl ? Mnemonic_UCOMISD : Mnemonic_UCOMISS;
    /*comiss src, 0.  */ newInst(mn_cmp, src, fpZeroOpnd);
    /*jp _nan:result=0*/ newBranch(Mnemonic_JP, loadZeroNode, testIfBelowNode);
    setCurrentNode(NULL);

    //
    // 
    //
    setCurrentNode(loadZeroNode);
    /*mov r32, 0*/      newInst(Mnemonic_MOV, dst, intZeroOpnd);
    /*jmp _done*/       connectNodeTo(doneNode);
    setCurrentNode(NULL);

    //
    // test if we have a huge negative in SRC
    //
    setCurrentNode(testIfBelowNode);
    /*jb _done:*/       newBranch(Mnemonic_JB, doneNode, loadMaxIntNode);
    setCurrentNode(NULL);
    //
    // 
    //
    setCurrentNode(loadMaxIntNode);
    /* sub dst, 1*/     newInst(Mnemonic_SUB, dst, oneOpnd);
    connectNodeTo(doneNode);
    setCurrentNode(NULL);
    //
    connectNodes(doneNode, getSubCfgReturnNode());
    //
    propagateSubCFG(inst);
    return Changed_Node;
}

static int getMinBit(U_32 val) {
    U_32 currentVal = val;
    int i=0;
    do {
        if ((currentVal & 1)) {
            return i;
        }
        currentVal = currentVal >> 1;
    } while (++i<32);
    return i;
}

static int getMaxBit(U_32 val) {
    U_32 currentVal = val;
    int i=31;
    do {
        if ((currentVal & (1<<31))) {
            return i;
        }
        currentVal = currentVal << 1;
    } while (--i>=0);
    return i;
}

PeepHoleOpt::Changed PeepHoleOpt::handleInst_MOV(Inst* inst)
{
    Node* node = inst->getNode();
    if (((BasicBlock*)node)->getLayoutSucc() == NULL)
    {
        Inst *next = inst->getNextInst();

        Node *currNode = node;
        bool methodMarkerOccur = false;
        MethodMarkerPseudoInst* methodMarker = NULL;
        // ignoring instructions that have no effect and saving method markers to correct them during optimizations
        while (next == NULL || next->getKind() == Inst::Kind_MethodEndPseudoInst || next->getMnemonic() == Mnemonic_JMP)
        {
            if (next == NULL)
            {
                currNode = currNode->getOutEdge(Edge::Kind_Unconditional)->getTargetNode();
                if (currNode->getKind() == Node::Kind_Exit)
                    return Changed_Nothing;
                next = (Inst*) currNode->getFirstInst();
            }
            else
            {
                if (next->getKind() == Inst::Kind_MethodEndPseudoInst)
                {
                    //max 1 saved method marker
                    if (methodMarkerOccur)
                    {
                        return Changed_Nothing;
                    }
                    methodMarker = (MethodMarkerPseudoInst*)next;
                    methodMarkerOccur = true;
                }
                next = next->getNextInst();
            }
        }

        Inst *jump = next->getNextInst();


        bool step1 = true;
        currNode = node;
        while (currNode != next->getNode())
        {
            currNode = currNode->getOutEdge(Edge::Kind_Unconditional)->getTargetNode();
            if (currNode->getInDegree()!=1)
            {
                step1 = false;
                break;
            }
        }

        // step1:
        // ---------------------------------------------
        // MOV opnd, opnd2             MOV opnd3, opnd2
        // MOV opnd3, opnd      ->
        // ---------------------------------------------
        // nb: applicable if opnd will not be used further
        if (step1 && next->getMnemonic() == Mnemonic_MOV)
        {
            Opnd *movopnd1, *movopnd2, *nextmovopnd1, *nextmovopnd2;
            bool isInstCopyPseudo = (inst->getKind() == Inst::Kind_CopyPseudoInst);
            if (isInstCopyPseudo)
            {
                movopnd1 = inst->getOpnd(0);
                movopnd2 = inst->getOpnd(1);
            }
            else
            {
                Inst::Opnds movuses(inst, Inst::OpndRole_Explicit|Inst::OpndRole_Use);
                Inst::Opnds movdefs(inst, Inst::OpndRole_Explicit|Inst::OpndRole_Def);
                movopnd1 = inst->getOpnd(movdefs.begin());
                movopnd2 = inst->getOpnd(movuses.begin());
            }
            bool isNextCopyPseudo = (next->getKind() == Inst::Kind_CopyPseudoInst);
            if (isNextCopyPseudo)
            {
                nextmovopnd1 = next->getOpnd(0);
                nextmovopnd2 = next->getOpnd(1);
            }
            else
            {
                Inst::Opnds nextmovuses(next, Inst::OpndRole_Explicit|Inst::OpndRole_Use);
                Inst::Opnds nextmovdefs(next, Inst::OpndRole_Explicit|Inst::OpndRole_Def);
                nextmovopnd1 = next->getOpnd(nextmovdefs.begin());
                nextmovopnd2 = next->getOpnd(nextmovuses.begin());
            }
            if (movopnd1->getId() == nextmovopnd2->getId() && 
                !isMem(movopnd2) && !isMem(nextmovopnd1) &&
                !isMem(movopnd1)
                )
            {
                BitSet ls(irManager->getMemoryManager(), irManager->getOpndCount());
                irManager->updateLivenessInfo();
                irManager->getLiveAtExit(next->getNode(), ls);
                for (Inst* i = (Inst*)next->getNode()->getLastInst(); i!=next; i = i->getPrevInst()) {
                    irManager->updateLiveness(i, ls);
                }
                bool dstNotUsed = !ls.getBit(movopnd1->getId());
                if (dstNotUsed)
                {
                    if (isInstCopyPseudo && isNextCopyPseudo)
                        irManager->newCopyPseudoInst(Mnemonic_MOV, nextmovopnd1, movopnd2)->insertAfter(inst);
                    else
                        irManager->newInst(Mnemonic_MOV, nextmovopnd1, movopnd2)->insertAfter(inst);
                    inst->unlink();
                    next->unlink();
                    return Changed_Node;
                }
            }
        }

        // step2:
        // --------------------------------------------------------------
        // MOV opnd, 0/1                Jmp smwh/BB1            Jmp smwh/BB1
        // CMP opnd, 0           ->     CMP opnd, 0     v
        // Jcc smwh                     Jcc smwh
        // BB1:                         BB1:
        // --------------------------------------------------------------
        // nb: applicable if opnd will not be used further
        if (next->getMnemonic() == Mnemonic_CMP && jump!= NULL && (jump->getMnemonic() == Mnemonic_JE ||
            jump->getMnemonic() == Mnemonic_JNE))
        {
            Opnd *movopnd1, *movopnd2;
            if (inst->getKind() == Inst::Kind_CopyPseudoInst)
            {
                movopnd1 = inst->getOpnd(0);
                movopnd2 = inst->getOpnd(1);
            }
            else
            {
                Inst::Opnds movuses(inst, Inst::OpndRole_Explicit|Inst::OpndRole_Use);
                Inst::Opnds movdefs(inst, Inst::OpndRole_Explicit|Inst::OpndRole_Def);
                movopnd1 = inst->getOpnd(movdefs.begin());
                movopnd2 = inst->getOpnd(movuses.begin());
            }
            Inst::Opnds cmpuses(next, Inst::OpndRole_Explicit|Inst::OpndRole_Use);
            Opnd* cmpopnd1 = next->getOpnd(cmpuses.begin());
            Opnd* cmpopnd2 = next->getOpnd(cmpuses.next(cmpuses.begin()));
            
            if (isImm(movopnd2) && (movopnd2->getImmValue() == 0 || movopnd2->getImmValue() == 1) &&
                movopnd1->getId() == cmpopnd1->getId() &&
                isImm(cmpopnd2) && cmpopnd2->getImmValue() == 0)
            {
                BitSet ls(irManager->getMemoryManager(), irManager->getOpndCount());
                irManager->updateLivenessInfo();
                irManager->getLiveAtExit(jump->getNode(), ls);
                bool opndNotUsed = !ls.getBit(movopnd1->getId());
                if (opndNotUsed)
                {
                    ControlFlowGraph* cfg = irManager->getFlowGraph();
                    Node* destination = ((BranchInst*)jump)->getTrueTarget();
                    if ((jump->getMnemonic() == Mnemonic_JNE || movopnd2->getImmValue() == 1) && !(jump->getMnemonic() == Mnemonic_JNE && movopnd2->getImmValue() == 1))
                    {
                        destination = ((BranchInst*)jump)->getFalseTarget();
                    }
                    if (node->getId() != next->getNode()->getId())
                    {
                        if (methodMarkerOccur)
                        {
                            inst->getNode()->appendInst(irManager->newMethodEndPseudoInst(methodMarker->getMethodDesc()));
                        }
                        inst->unlink();
                        Edge *outEdge = node->getOutEdge(Edge::Kind_Unconditional);
                        cfg->replaceEdgeTarget(outEdge, destination, true);
                        cfg->purgeUnreachableNodes(); // previous successor may become unreachable
                    }
                    else
                    {
                        cfg->removeEdge(node->getOutEdge(Edge::Kind_True));
                        cfg->removeEdge(node->getOutEdge(Edge::Kind_False));
                        cfg->addEdge(node, destination);
                        inst->unlink();
                        next->unlink();
                        jump->unlink();
                    }

                    return Changed_Node;
                }
            }
        }
    }
    return Changed_Nothing;
}

PeepHoleOpt::Changed PeepHoleOpt::handleInst_CMP(Inst* inst) {
    assert(inst->getMnemonic()==Mnemonic_CMP);
    
    Inst::Opnds uses(inst, Inst::OpndRole_Explicit | Inst::OpndRole_Use);
    Opnd* src1 = inst->getOpnd(uses.begin());
    Opnd* src2 = inst->getOpnd(uses.next(uses.begin()));
    assert(src1!=NULL && src2!=NULL);

    if (isImm(src1)) {
        Opnd* tmp = src1; src1 = src2; src2 = tmp;
    }

    if (isImm(src2) && isReg(src1) && (int)src2->getImmValue() == 0) {
            if (Log::isEnabled()) Log::out()<<"I"<<inst->getId()<<" -> CMP with 0"<<std::endl;
            irManager->newInst(Mnemonic_TEST, src1, src1)->insertAfter(inst);
            inst->unlink();
            return Changed_Inst;
    }
    return Changed_Nothing;
}



PeepHoleOpt::Changed PeepHoleOpt::handleInst_MUL(Inst* inst) {
    assert((inst->getMnemonic() == Mnemonic_IMUL) || (inst->getMnemonic() == Mnemonic_MUL));
    
    if (inst->getForm() == Inst::Form_Native) {
        return Changed_Nothing;
    }
    
    Inst::Opnds defs(inst, Inst::OpndRole_Explicit|Inst::OpndRole_Def);
    Opnd* dst1 = inst->getOpnd(defs.begin());
    Opnd* dst2 = NULL;
    if ((inst->getMnemonic() == Mnemonic_IMUL) && (defs.next(defs.begin()) != defs.end())){
        return Changed_Nothing;
    }
    else { //inst->getMnemonic() == Mnemonic_MUL
        dst2 = inst->getOpnd(defs.next(defs.begin()));
        if (defs.next(defs.next(defs.begin()))!=defs.end())
            return Changed_Nothing;
    }

    Inst::Opnds uses(inst, Inst::OpndRole_Explicit|Inst::OpndRole_Use);
    Opnd* src1= inst->getOpnd(uses.begin());
    Opnd* src2= inst->getOpnd(uses.next(uses.begin()));
    if (inst->getMnemonic() == Mnemonic_IMUL)
        assert(src1!=NULL && src2!=NULL && dst1!=NULL);
    else //inst->getMnemonic() == Mnemonic_MUL
        assert(src1!=NULL && src2!=NULL && dst1!=NULL && dst2!=NULL);

    if (isImm(src1)) {
        Opnd* tmp = src1; src1 = src2; src2 = tmp;
    }
    if (isImm(src2) && irManager->getTypeSize(src2->getType()) <=32) {
        int immVal = (int)src2->getImmValue();
        if (immVal == 0) {
            if (Log::isEnabled()) Log::out()<<"I"<<inst->getId()<<" -> MUL with 0"<<std::endl;
            if (inst->getMnemonic() == Mnemonic_IMUL) {
                irManager->newCopyPseudoInst(Mnemonic_MOV, dst1, src2)->insertAfter(inst);
            } else { //inst->getMnemonic() == Mnemonic_MUL
                irManager->newCopyPseudoInst(Mnemonic_MOV, dst1, src2)->insertAfter(inst);
                irManager->newCopyPseudoInst(Mnemonic_MOV, dst2, src2)->insertAfter(inst);
            }
            inst->unlink();
            return Changed_Inst;
        } else if (immVal == 1) {
            if (Log::isEnabled()) Log::out()<<"I"<<inst->getId()<<" -> MUL with 1"<<std::endl;
            if (inst->getMnemonic() == Mnemonic_IMUL) {
                irManager->newCopyPseudoInst(Mnemonic_MOV, dst1, src1)->insertAfter(inst);
            } else { //inst->getMnemonic() == Mnemonic_MUL
                Opnd* zero = irManager->newImmOpnd(dst1->getType(), 0);
                irManager->newCopyPseudoInst(Mnemonic_MOV, dst1, zero)->insertAfter(inst);
                irManager->newCopyPseudoInst(Mnemonic_MOV, dst2, src1)->insertAfter(inst);
            }
            inst->unlink();
            return Changed_Inst;
        } else if (immVal == 2) {
            if (inst->getMnemonic() == Mnemonic_IMUL) {
                if (Log::isEnabled()) Log::out()<<"I"<<inst->getId()<<" -> MUL with 2"<<std::endl;
                irManager->newInstEx(Mnemonic_ADD, 1, dst1, src1, src1)->insertAfter(inst);
                inst->unlink();
                return Changed_Inst;
            }
        } else {
            if (inst->getMnemonic() == Mnemonic_IMUL) {
                int minBit=getMinBit(immVal);   
                int maxBit=getMaxBit(immVal);
                if (minBit == maxBit) {
                     assert(minBit>=2);
                     if (Log::isEnabled()) Log::out()<<"I"<<inst->getId()<<" -> MUL with 2^"<<minBit<<std::endl;
                     Type* immType = irManager->getTypeManager().getUInt8Type();
                     irManager->newCopyPseudoInst(Mnemonic_MOV, dst1, src1)->insertBefore(inst);
                     irManager->newInst(Mnemonic_SHL, dst1, irManager->newImmOpnd(immType, minBit))->insertBefore(inst);
                     inst->unlink();
                     return Changed_Inst;
                 }
             }
          }
      }
      return Changed_Nothing;
}

PeepHoleOpt::Changed PeepHoleOpt::handleInst_ALU(Inst* inst)
{
    // The normal form is 'OPERATION left opnd, right operand'
    // except for NOT operation.
    const Mnemonic mnemonic = inst->getMnemonic();
    if (mnemonic == Mnemonic_NOT) {
        // No optimizations this time
        return Changed_Nothing;
    }
    
    // Only these mnemonics have the majestic name of ALUs.
    assert(mnemonic == Mnemonic_ADD || mnemonic == Mnemonic_SUB ||
           mnemonic == Mnemonic_ADC || mnemonic == Mnemonic_SBB ||
           mnemonic == Mnemonic_OR || mnemonic == Mnemonic_XOR ||
           mnemonic == Mnemonic_AND ||
           mnemonic == Mnemonic_CMP || mnemonic == Mnemonic_TEST);
    
    if (mnemonic == Mnemonic_AND)
    {
        Inst::Opnds defs(inst, Inst::OpndRole_Explicit|Inst::OpndRole_Def);
        Opnd* dst = inst->getOpnd(defs.begin());
        Inst::Opnds uses(inst, Inst::OpndRole_Explicit|Inst::OpndRole_Use);
        Opnd* src1= inst->getOpnd(uses.begin());
        Opnd* src2= inst->getOpnd(uses.next(uses.begin()));

        Opnd *newopnd2;
        // test can work only with operands having equal sizes
        if (isImm(src2) && src2->getSize() != src1->getSize())
            newopnd2 = irManager->newImmOpnd(src1->getType(), src2->getImmValue());
        else
            newopnd2 = src2;
        if (!isMem(dst) && !isMem(src1) && !isMem(src2))
        {
            BitSet ls(irManager->getMemoryManager(), irManager->getOpndCount());
            irManager->updateLivenessInfo();
            irManager->getLiveAtExit(inst->getNode(), ls);
            for (Inst* i = (Inst*)inst->getNode()->getLastInst(); i!=inst; i = i->getPrevInst()) {
                irManager->updateLiveness(i, ls);
            }
            bool dstNotUsed = !ls.getBit(dst->getId());
            if (dstNotUsed)
            {
                // what: AND opnd1, opnd2 => TEST opnd1, opnd2
                // nb: applicable if opnd1 will not be used further
                
                if (inst->getForm() == Inst::Form_Extended)
                    irManager->newInstEx(Mnemonic_TEST, 0, src1, newopnd2)->insertAfter(inst);
                else
                    irManager->newInst(Mnemonic_TEST, src1, newopnd2)->insertAfter(inst);
                inst->unlink();
                return Changed_Inst;
            }
        }
    } else if (mnemonic == Mnemonic_ADD) {
        /* Change "dst=src+0" to "MOV dst, src" if there is another ADD inst followed in the same BB. */
        Inst::Opnds defs(inst, Inst::OpndRole_Explicit|Inst::OpndRole_Def);
        Opnd* dst = inst->getOpnd(defs.begin());
        Inst::Opnds uses(inst, Inst::OpndRole_Explicit|Inst::OpndRole_Use);
        Opnd* src1= inst->getOpnd(uses.begin());
        Opnd* src2= inst->getOpnd(uses.next(uses.begin()));

        bool src1IsZero = false;
        bool src2IsZero = false;
        if (src1->isPlacedIn(OpndKind_Imm) && (src1->getImmValue() == 0))
            src1IsZero = true;
        if (src2->isPlacedIn(OpndKind_Imm) && (src2->getImmValue() == 0))
            src2IsZero = true;

        bool anotherADD = false;
        Inst *iter = inst->getNextInst();
        while (iter != NULL) {
            if (iter->getMnemonic() == Mnemonic_ADC)
                break;
            if (iter->getMnemonic() == Mnemonic_ADD) {
                anotherADD = true;
                break;
            }
            iter = iter->getNextInst();;
        }

        if (anotherADD) {
            if (src1IsZero) {
                irManager->newCopyPseudoInst(Mnemonic_MOV, dst, src2)->insertAfter(inst);
                inst->unlink();
                return Changed_Inst;
            } else if (src2IsZero) {
                irManager->newCopyPseudoInst(Mnemonic_MOV, dst, src1)->insertAfter(inst);
                inst->unlink();
                return Changed_Inst;
            }
        }
    }
    return Changed_Nothing;
}

PeepHoleOpt::Changed PeepHoleOpt::handleInst_SETcc(Inst* inst)
{
    if (((BasicBlock*)inst->getNode())->getLayoutSucc() == NULL)
    {
        Mnemonic mn = inst->getMnemonic();
        
        Inst* prev = inst->getPrevInst();
        Inst *next = inst->getNextInst();

        Node *currNode = inst->getNode();
        bool methodMarkerOccur = false;
        MethodMarkerPseudoInst* methodMarker = NULL;
        // ignoring instructions that have no effect and saving method markers to correct them during optimizations
        while (next == NULL || next->getKind() == Inst::Kind_MethodEndPseudoInst || next->getMnemonic() == Mnemonic_JMP)
        {
            if (next == NULL)
            {
                currNode = currNode->getOutEdge(Edge::Kind_Unconditional)->getTargetNode();
                if (currNode->getKind() == Node::Kind_Exit)
                    return Changed_Nothing;
                next = (Inst*) currNode->getFirstInst();
            }
            else
            {
                if (next->getKind() == Inst::Kind_MethodEndPseudoInst)
                {
                    //max 1 saved method marker
                    if (methodMarkerOccur)
                    {
                        return Changed_Nothing;
                    }
                    methodMarker = (MethodMarkerPseudoInst*)next;
                    methodMarkerOccur = true;
                }
                next = next->getNextInst();
            }
        }

        Inst *next2 = next->getNextInst();

        bool step1 = true;
        currNode = inst->getNode();
        while (currNode != next->getNode())
        {
            currNode = currNode->getOutEdge(Edge::Kind_Unconditional)->getTargetNode();
            if (currNode->getInDegree()!=1)
            {
                step1 = false;
                break;
            }
        }

        // step1:
        // ------------------------------------------
        // MOV opnd, 0                  MOV opnd2, 0
        // SETcc opnd           ->      SETcc opnd2
        // MOV opnd2, opnd
        // ------------------------------------------
        // nb: applicable if opnd will not be used further
        if (step1 && prev!= NULL && prev->getMnemonic() == Mnemonic_MOV &&
            next!= NULL && next->getMnemonic() == Mnemonic_MOV)
        {
            Opnd *prevopnd1, *prevopnd2, *nextopnd1, *nextopnd2, *setopnd;
            if (prev->getKind() == Inst::Kind_CopyPseudoInst)
            {
                prevopnd1 = prev->getOpnd(0);
                prevopnd2 = prev->getOpnd(1);
            }
            else
            {
                Inst::Opnds prevuses(prev, Inst::OpndRole_Explicit|Inst::OpndRole_Use);
                Inst::Opnds prevdefs(prev, Inst::OpndRole_Explicit|Inst::OpndRole_Def);
                prevopnd1 = prev->getOpnd(prevdefs.begin());
                prevopnd2 = prev->getOpnd(prevuses.begin());
            }
            if (next->getKind() == Inst::Kind_CopyPseudoInst)
            {
                nextopnd1 = next->getOpnd(0);
                nextopnd2 = next->getOpnd(1);
            }
            else
            {
                Inst::Opnds nextuses(next, Inst::OpndRole_Explicit|Inst::OpndRole_Use);
                Inst::Opnds nextdefs(next, Inst::OpndRole_Explicit|Inst::OpndRole_Def);
                nextopnd1 = next->getOpnd(nextdefs.begin());
                nextopnd2 = next->getOpnd(nextuses.begin());
            }
            Inst::Opnds setdefs(inst, Inst::OpndRole_Explicit|Inst::OpndRole_Def);
            setopnd = inst->getOpnd(setdefs.begin());

            if (isReg(nextopnd1) &&
                prevopnd1->getId() == setopnd->getId() &&
                setopnd->getId() == nextopnd2->getId() &&
                isImm(prevopnd2) && prevopnd2->getImmValue() == 0
                )
            {
                BitSet ls(irManager->getMemoryManager(), irManager->getOpndCount());
                irManager->updateLivenessInfo();
                irManager->getLiveAtExit(next->getNode(), ls);
                for (Inst* i = (Inst*)next->getNode()->getLastInst(); i!=next; i = i->getPrevInst()) {
                    irManager->updateLiveness(i, ls);
                }
                bool opndNotUsed = !ls.getBit(setopnd->getId());
                if (opndNotUsed)
                {
                    if (nextopnd1->getRegName() != RegName_Null &&
                        Constraint::getAliasRegName(nextopnd1->getRegName(), OpndSize_8) == RegName_Null)
                    {
                        nextopnd1->assignRegName(setopnd->getRegName());
                    }
                    irManager->newInst(Mnemonic_MOV, nextopnd1, prevopnd2)->insertBefore(inst);
                    irManager->newInst(mn, nextopnd1)->insertBefore(inst);
                    prev->unlink();
                    inst->unlink();
                    next->unlink();
                    return Changed_Node;
                }
            }
        }

        // step2:
        // --------------------------------------------------------------
        // MOV opnd, 0                  Jcc smwh                Jcc smwh
        // SETcc opnd           ->      BB1:            v       BB1:
        // CMP opnd, 0                  ...
        // Jcc smwh                     CMP opnd, 0
        // BB1:                         Jcc smwh
        // --------------------------------------------------------------
        // nb: applicable if opnd will not be used further
        // nb: conditions of new jumps are calculated from conditions of old jump and set instructions
	    if (prev!= NULL && prev->getMnemonic() == Mnemonic_MOV &&
            next!= NULL && (next->getMnemonic() == Mnemonic_CMP || next->getMnemonic() == Mnemonic_TEST) &&
            next2!= NULL && (next2->getMnemonic() == Mnemonic_JG || next2->getMnemonic() == Mnemonic_JE || next2->getMnemonic() == Mnemonic_JNE) )
	    {
            Opnd* movopnd1;
            Opnd* movopnd2;
            if (prev->getKind() == Inst::Kind_CopyPseudoInst)
            {
                movopnd1 = prev->getOpnd(0);
                movopnd2 = prev->getOpnd(1);
            }
            else
            {
                Inst::Opnds movuses(prev, Inst::OpndRole_Explicit|Inst::OpndRole_Use);
                Inst::Opnds movdefs(prev, Inst::OpndRole_Explicit|Inst::OpndRole_Def);
                movopnd1 = prev->getOpnd(movdefs.begin());
                movopnd2 = prev->getOpnd(movuses.begin());
            }
            Inst::Opnds cmpuses(next, Inst::OpndRole_Explicit|Inst::OpndRole_Use);
            Opnd* cmpopnd1 = next->getOpnd(cmpuses.begin());
            Opnd* cmpopnd2 = next->getOpnd(cmpuses.next(cmpuses.begin()));

            if (
                isImm(movopnd2) && movopnd2->getImmValue() == 0 &&
                movopnd1->getId() == cmpopnd1->getId() &&
                //case CMP:
                (next->getMnemonic() != Mnemonic_CMP || (isImm(cmpopnd2) && cmpopnd2->getImmValue() == 0)) &&
                //case TEST:
                (next->getMnemonic() != Mnemonic_TEST || cmpopnd1->getId() == cmpopnd2->getId())
                )
            {
                BitSet ls(irManager->getMemoryManager(), irManager->getOpndCount());
                irManager->updateLivenessInfo();
                irManager->getLiveAtExit(next2->getNode(), ls);
                bool opndNotUsed = !ls.getBit(movopnd1->getId());
                if (opndNotUsed)
                {
                    BranchInst* br = (BranchInst*) next2;

                    Mnemonic newjumpmn = Mnemonic_JZ;
                    if (next2->getMnemonic() == Mnemonic_JE)
                    {
                        switch (mn)
                        {
                        case Mnemonic_SETG:
                            newjumpmn = Mnemonic_JLE; break;
                        case Mnemonic_SETE:
                            newjumpmn = Mnemonic_JNE; break;
                        case Mnemonic_SETL:
                            newjumpmn = Mnemonic_JGE; break;
                        case Mnemonic_SETNE:
                            newjumpmn = Mnemonic_JE; break;
                        default:
                            assert(0); break;
                        }
                    }
                    else
                    {
                        switch (mn)
                        {
                        case Mnemonic_SETG:
                            newjumpmn = Mnemonic_JG; break;
                        case Mnemonic_SETE:
                            newjumpmn = Mnemonic_JE; break;
                        case Mnemonic_SETL:
                            newjumpmn = Mnemonic_JL; break;
                        case Mnemonic_SETNE:
                            newjumpmn = Mnemonic_JNE; break;
                        default:
                            assert(0); break;
                        }
                    }

                    if (inst->getNode()->getId() != next->getNode()->getId())
                    {
                        ControlFlowGraph* cfg = irManager->getFlowGraph();
                        cfg->removeEdge(inst->getNode()->getOutEdge(Edge::Kind_Unconditional));

                        double trueEdgeProb = next2->getNode()->getOutEdge(Edge::Kind_True)->getEdgeProb();
                        double falseEdgeProb = next2->getNode()->getOutEdge(Edge::Kind_False)->getEdgeProb();
                        cfg->addEdge(inst->getNode(), br->getTrueTarget(), trueEdgeProb);
                        cfg->addEdge(inst->getNode(), br->getFalseTarget(), falseEdgeProb);
                        irManager->newBranchInst(newjumpmn, br->getTrueTarget(), br->getFalseTarget())->insertAfter(inst);
                        if (methodMarkerOccur)
                        {
                            inst->getNode()->appendInst(irManager->newMethodEndPseudoInst(methodMarker->getMethodDesc()));
                        }
		                prev->unlink();
		                inst->unlink();
                        cfg->purgeUnreachableNodes();
                    }
                    else
                    {
                        irManager->newBranchInst(newjumpmn, br->getTrueTarget(), br->getFalseTarget())->insertAfter(next2);
                        prev->unlink();
		                inst->unlink();
		                next->unlink();
                        next2->unlink();
                    }
		            return Changed_Node;
                }// endif opndNotUsed
            }
        }
    }
	return Changed_Nothing;
}

PeepHoleOpt::Changed PeepHoleOpt::handleInst_SSEMov(Inst* inst)
{
    assert(inst->getMnemonic() == Mnemonic_MOVSS || 
           inst->getMnemonic() == Mnemonic_MOVSD);
           
    const bool isDouble = inst->getMnemonic() == Mnemonic_MOVSD;

    if (inst->getOpndCount() != 2) {
        // Expected only MOVSS/SD a, b
        assert(false);
        return Changed_Nothing;
    }
    Opnd* dst = inst->getOpnd(0);
    Opnd* src = inst->getOpnd(1);
    //
    //
    if (isReg(dst) && equals(src, dst)) {
        // what: same register moved around
        // why:  useless thing
        inst->unlink();
        return Changed_Inst;
    }
    
    //
    //
    if (isReg(dst) && isMem(src)) {
        /* what: MOVSS/MOVSD xmmreg, [zero constant from memory] => PXOR xmmreg, xmmreg
           why:  shorter instruction; no memory access => faster
           nb:   only works with 64 XMMs
        */
        bool isZeroConstant = false;
        if (isDouble) {
            isZeroConstant = isFPConst(src, (double)0);
        }
        else {
            isZeroConstant = isFPConst(src, (float)0);
        }
        
        if (isZeroConstant) {
            // PXOR only accepts double registers, convert dst
            dst = convertToXmmReg64(dst);
            Inst* ii = irManager->newInst(Mnemonic_PXOR, dst, dst);
            replaceInst(inst, ii);
            return Changed_Inst;
        }
        //
        // fall through to process more
        // ||
        // vv
        
    }   // ~ movss xmm, 0 => pxor xmm,xmm
    
    if (isReg(dst) && isReg(src)) {
        /*what: MOVSS/MOVSD reg, reg => MOVQ reg, reg
          why: MOVSD has latency=6, MOVSS has latency=4, MOVQ's latency=2
          nb: MOVQ only works with 64 xmms
        */
        dst = convertToXmmReg64(dst);
        src = convertToXmmReg64(src);
        Inst* ii = irManager->newInst(Mnemonic_MOVQ, dst, src);
        replaceInst(inst, ii);
        return Changed_Inst;
    }
    
    // We just handled 'both regs' case above, the only possible variant:
    assert((isReg(dst)&&isMem(src)) || (isReg(src)&&isMem(dst)));
    if (false && isDouble) {
        //FIXME: MOVQ with memory gets encoded badly - need to fix in encoder
        /*
        what: MOVSD => MOVQ
        why:  faster (? actually, I hope so. Need to double check)
        nb:   only for xmm64
        */
        Inst* ii = irManager->newInst(Mnemonic_MOVQ, dst, src);
        replaceInst(inst, ii);
        return Changed_Inst;
    }
    
    return Changed_Nothing;
}

PeepHoleOpt::Changed PeepHoleOpt::handleInst_SSEXor(Inst* inst)
{
    assert(inst->getMnemonic() == Mnemonic_XORPS || 
           inst->getMnemonic() == Mnemonic_XORPD);
           
    if (inst->getOpndCount() != 2) {
        // Expected only XORPS/PD a, b
        assert(false);
        return Changed_Nothing;
    }
    
    Opnd* dst = inst->getOpnd(0);
    Opnd* src = inst->getOpnd(1);
    
    if (isReg(dst) && isReg(src, dst->getRegName())) {
        /*what: XORPS/XORPD regN, regN => PXOR regN, regN
          why: XORPS/PD used for zero-ing register, but PXOR is faster 
               (2 ticks on PXOR vs 4 ticks for XORPS/XORPD)
        */
        // FIXME: replacing operands on 1 instruction only 
        // will fail liveness verification if their refcount > 1
        //dst = convertToXmmReg64(dst);
        //src = convertToXmmReg64(src);
        //Inst* ii = irManager->newInst(Mnemonic_PXOR, dst, src);
        //replaceInst(inst, ii);
        //return Changed_Inst;
    }
    return Changed_Nothing;
}

}}; // ~namespace Jitrino::Ia32

