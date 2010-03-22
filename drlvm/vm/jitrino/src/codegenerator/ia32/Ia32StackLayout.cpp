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
 * @author Intel, Nikolay A. Sidelnikov
 */

#include "Ia32IRManager.h"
#include "Ia32StackInfo.h"

namespace Jitrino
{
namespace Ia32 {

//========================================================================================
// class StackLayouter
//========================================================================================
/**
 *  Class StackLayouter performs forming of stack memory for a method, e.g.
 *  allocates memory for stack variables and input arguments, inserts 
 *  saving/restoring callee-save registers. Also it fills StackInfo object
 *  for runtime access to information about stack layout
 *
 *  This transformer ensures that
 *
 *  1)  All input argument operands and stack memory operands have appropriate
 *      displacements from stack pointer
 *  
 *  2)  There are save/restore instructions for all callee-save registers
 *
 *  3)  There are save/restore instructions for all caller-save registers for
 *      all calls in method
 *
 *  4)  ESP has appropriate value throughout whole method
 *
 *  Stack layout illustration:
 *
 *  +-------------------------------+   inargEnd
 *  |                               |
 *  |                               |
 *  |                               |
 *  +-------------------------------+   inargBase, eipEnd
 *  |           eip                 |
 *  +-------------------------------+   eipBase,icalleeEnd      <--- "virtual" ESP
 *  |           EBX                 |
 *  |           EBP                 |
 *  |           ESI                 |
 *  |           EDI                 |
 *  +-------------------------------+   icalleeBase, fcalleeEnd
 *  |                               |
 *  |                               |
 *  |                               |
 *  +-------------------------------+   fcalleeBase, acalleeEnd
 *  |                               |
 *  |                               |
 *  |                               |
 *  +-------------------------------+   acalleeBase, localEnd
 *  |                               |
 *  |                               |
 *  |                               |
 *  +-------------------------------+   localBase    
 *  |      alignment padding        |
 *  |-------------------------------+   <--- "real" ESP
 *  |           EAX                 |
 *  |           ECX                 |
 *  |           EDX                 |
 *  +-------------------------------+   base of caller-save regs
 *  
*/

class StackLayouter : public SessionAction {
public:
    StackLayouter ();
    StackInfo * getStackInfo() { return stackInfo; }
    void runImpl();

    U_32 getNeedInfo()const{ return 0; }
    U_32 getSideEffects()const{ return 0; }

protected:
    void checkUnassignedOpnds();

    /** Computes all offsets for stack areas and stack operands,
        inserts pushs for callee-save registers
    */
    void createProlog();

    /** Restores stack pointer if needed,
        inserts pops for callee-save registers
    */
    void createEpilog();

    I_32 getLocalBase(){ return  localBase; }
    I_32 getLocalEnd(){ return  localEnd; }
    I_32 getApplCalleeBase(){ return  acalleeBase; }
    I_32 getApplCalleeEnd(){ return  acalleeEnd; }
    I_32 getFloatCalleeBase(){ return  fcalleeBase; }
    I_32 getFloatCalleeEnd(){ return  fcalleeEnd; }
    I_32 getIntCalleeBase(){ return  icalleeBase; }
    I_32 getIntCalleeEnd(){ return  icalleeEnd; }
    I_32 getRetEIPBase(){ return  retEIPBase; } 
    I_32 getRetEIPEnd(){ return  retEIPEnd; }
    I_32 getInArgBase(){ return  inargBase; } 
    I_32 getInArgEnd(){ return  inargEnd; }
    I_32 getFrameSize(){ return  frameSize; }
    U_32 getOutArgSize(){ return  outArgSize; }

    I_32 localBase;
    I_32 localEnd;
    I_32 fcalleeBase;
    I_32 fcalleeEnd;
    I_32 acalleeBase;
    I_32 acalleeEnd;
    I_32 icalleeBase;
    I_32 icalleeEnd;

    I_32 retEIPBase;
    I_32 retEIPEnd;
    I_32 inargBase;
    I_32 inargEnd;
    I_32 frameSize;
    U_32 outArgSize;
    StackInfo * stackInfo;

    MemoryManager memoryManager;
    
    static const int alignmentSequenceSize = 4; 
    static Opnd::MemOpndAlignment const alignmentSequence[4];
};

static ActionFactory<StackLayouter> _stack("stack");
Opnd::MemOpndAlignment const StackLayouter::alignmentSequence[] =
    { Opnd::MemOpndAlignment_16, Opnd::MemOpndAlignment_8,
      Opnd::MemOpndAlignment_4, Opnd::MemOpndAlignment_Any
     };



StackLayouter::StackLayouter ()
   :localBase(0),
    localEnd(0),
    fcalleeBase(0),
    fcalleeEnd(0),
    acalleeBase(0),
    acalleeEnd(0),
    icalleeBase(0),
    icalleeEnd(0),
    retEIPBase(0),
    retEIPEnd(0),
    inargBase(0),
    inargEnd(0),
    frameSize(0),
    outArgSize(0),
    memoryManager("StackLayouter")
{
};


static bool isSOEHandler(ObjectType* type) {
    static const char* soeHandlers[] = {"java/lang/Object", "java/lang/Throwable", "java/lang/Error", "java/lang/StackOverflowError", NULL};
    const char* typeName = type->getName();
    for (size_t i=0;soeHandlers[i]!=NULL; i++) {
        if (!strcmp(typeName, soeHandlers[i])) {
            return true;
        }
    }
    return false;
}

//checks if SOE can be caught in this method
static bool hasSOEHandlers(IRManager& irm) {
    //A contract with VM: check extra page for synchronized methods or methods with SOE handlers.
    if (irm.getMethodDesc().isSynchronized()) {
        return true;
    }

    const Nodes& nodes= irm.getFlowGraph()->getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
       Node* node = *it;
        if (node->isCatchBlock()) {
            const Edges& edges = node->getInEdges();
            for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
                Edge* e = *ite;
                CatchEdge* catchEdge = (CatchEdge*)e;
                ObjectType* catchType = catchEdge->getType()->asObjectType();
                assert(catchType!=NULL);
                if (isSOEHandler(catchType)) {
                    return true;
                }
            }
        }
    }
    return false;
}

#define MAX_STACK_FOR_SOE_HANDLERS 0x2000

static void insertSOECheck(IRManager& irm, U_32 maxStackUsedByMethod) {
#ifdef _EM64T_
    //SOE checking is not finished on EM64T
    //TODO: work on stack alignment & SOE checkers
    if (true) return; 
#endif
    U_32 stackToCheck = maxStackUsedByMethod + (hasSOEHandlers(irm) ? MAX_STACK_FOR_SOE_HANDLERS : 0);
    if (stackToCheck == 0) {
        return;
    }
    static const U_32 PAGE_SIZE=0x1000;
    
    U_32 nPagesToCheck = stackToCheck / PAGE_SIZE;
    Inst* prevInst = irm.getEntryPointInst();
    for(U_32 i=0;i<=nPagesToCheck; i++) {
        U_32 offset = i < nPagesToCheck ? PAGE_SIZE * (i+1) : stackToCheck;
        Opnd* guardedMemOpnd = irm.newMemOpnd(irm.getTypeFromTag(Type::IntPtr), MemOpndKind_Heap, irm.getRegOpnd(STACK_REG), -(int)offset);
        Inst* guardInst = irm.newInst(Mnemonic_TEST, guardedMemOpnd, irm.getRegOpnd(STACK_REG));
        guardInst->insertAfter(prevInst);
        guardInst->setBCOffset(0);
        prevInst = guardInst;
    }
}

void StackLayouter::runImpl()
{
    stackInfo = new(irManager->getMemoryManager()) StackInfo(irManager->getMemoryManager());
    irManager->setInfo(STACK_INFO_KEY, stackInfo);

    irManager->calculateOpndStatistics();
#ifdef _DEBUG
    checkUnassignedOpnds();
#endif
    irManager->calculateTotalRegUsage(OpndKind_GPReg);
    createProlog();
    createEpilog();
    U_32 maxStackDepth = irManager->calculateStackDepth();
    insertSOECheck(*irManager, maxStackDepth);
    irManager->layoutAliasOpnds();

    //fill StackInfo object
    stackInfo->frameSize = getFrameSize();

    stackInfo->icalleeMask = irManager->getCallingConvention()->getCalleeSavedRegs(OpndKind_GPReg).getMask() & irManager->getTotalRegUsage(OpndKind_GPReg);
    stackInfo->icalleeOffset = getIntCalleeBase();
    stackInfo->fcallee = irManager->getCallingConvention()->getCalleeSavedRegs(OpndKind_FPReg).getMask();
    stackInfo->foffset = getFloatCalleeBase();
    stackInfo->acallee = 0; //VSH: TODO - get rid off appl regs irManager->getCallingConvention()->getCalleeSavedRegs(OpndKind_ApplicationReg);
    stackInfo->aoffset = getApplCalleeBase();
    stackInfo->localOffset = getLocalBase();
    stackInfo->eipOffset = getRetEIPBase();
}

void StackLayouter::checkUnassignedOpnds()
{
    for (U_32 i=0, n=irManager->getOpndCount(); i<n; i++) {
#ifdef _DEBUG
        Opnd * opnd = irManager->getOpnd(i);
        assert(!opnd->getRefCount() || opnd->hasAssignedPhysicalLocation());
#endif
    }
}

void StackLayouter::createProlog()
{
    const U_32 slotSize = sizeof(POINTER_SIZE_INT); 
    EntryPointPseudoInst* entryPointInst = NULL;
    const CallingConventionClient* cClient = NULL;
    const CallingConvention* cConvention = NULL;
    U_32 stackSizeAlignment = 0; 
    int offset = 0;
    
    entryPointInst = irManager->getEntryPointInst();
    assert(entryPointInst->getNode() == irManager->getFlowGraph()->getEntryNode());
    cClient = &((const EntryPointPseudoInst*)entryPointInst)->getCallingConventionClient();
    cConvention = cClient->getCallingConvention();
    // Overall size of stack frame should preserve alignment available on method enter. 
    stackSizeAlignment = (cConvention->getStackAlignment() == STACK_ALIGN_HALF16)
        ? STACK_ALIGN16 : cConvention->getStackAlignment();

    // Create or reset displacements for stack memory operands.
    for (U_32 i = 0; i < irManager->getOpndCount(); i++) {
        Opnd * opnd = irManager->getOpnd(i);
        if (opnd->getRefCount() && opnd->getMemOpndKind() == MemOpndKind_StackAutoLayout) {
            Opnd * dispOpnd=opnd->getMemOpndSubOpnd(MemOpndSubOpndKind_Displacement);
            if (dispOpnd == NULL){
                dispOpnd = irManager->newImmOpnd(irManager->getTypeManager().getInt32Type(), 0);
                opnd->setMemOpndSubOpnd(MemOpndSubOpndKind_Displacement, dispOpnd);
            }
            dispOpnd->assignImmValue(0);
        }
    }

    // Return EIP area.
    retEIPBase = offset;
    offset += sizeof(POINTER_SIZE_INT);
    retEIPEnd = inargBase = offset;


    // Assign displacements for input operands.
    if (entryPointInst) {
        const StlVector<CallingConventionClient::StackOpndInfo>& stackOpndInfos = 
            cClient->getStackOpndInfos(Inst::OpndRole_Def);

        for (U_32 i = 0, n = (U_32)stackOpndInfos.size(); i < n; i++) {
            uint64 argOffset = stackOpndInfos[i].offset;
            Opnd * opnd = entryPointInst->getOpnd(stackOpndInfos[i].opndIndex);
            Opnd * disp = opnd->getMemOpndSubOpnd(MemOpndSubOpndKind_Displacement);
            disp->assignImmValue(offset + argOffset);
        }
    }
    inargEnd = offset;
    icalleeEnd = offset = 0;

    U_32 calleeSavedRegs = cConvention->getCalleeSavedRegs(OpndKind_GPReg).getMask();
    U_32 usageRegMask = irManager->getTotalRegUsage(OpndKind_GPReg);
    Inst * lastPush = NULL;
    
    // Push callee-save registers onto stack.
#ifdef _EM64T_
    for (U_32 reg = RegName_R15; reg >= RegName_RAX; reg--) {
#else
    for (U_32 reg = RegName_EDI; reg >= RegName_EAX; reg--) {
#endif
        U_32 mask = getRegMask((RegName)reg);
        if ((mask & calleeSavedRegs) && (usageRegMask & mask)) {
            Inst * inst = irManager->newInst(Mnemonic_PUSH, irManager->getRegOpnd((RegName)reg));
            if (!lastPush) {
                lastPush = inst;
            }
            inst->insertAfter(entryPointInst);
            offset -= slotSize;
        }
    }
    icalleeBase = fcalleeEnd = fcalleeBase = acalleeEnd = acalleeBase = localEnd = offset;
    
    // Align callee save area on maximum possible value:
    //   - for STACK_ALIGN16 & STACK_ALIGN_HALF16 align on 16-bytes  
    //   - for STACK_ALIGN4 align on 4-bytes
    offset &= ~(stackSizeAlignment - 1);
    
    if (cConvention->getStackAlignment() == STACK_ALIGN_HALF16 &&
        (offset & ~(STACK_ALIGN16 - 1)) == 0) {
        // Need to align size of callee save area on half of 16-bytes
        // thus resulting stack pointer will be 16-bytes aligned.
        offset -= STACK_ALIGN_HALF16; 
    }

    // Retrieve relations not earlier than all memory locations are assigned.
    IRManager::AliasRelation * relations = new(irManager->getMemoryManager()) IRManager::AliasRelation[irManager->getOpndCount()];
    irManager->getAliasRelations(relations);

    // Assign displacements for local variable operands.
    for (int j = 0; j <= alignmentSequenceSize; j++) {
        for (U_32 i = 0; i < irManager->getOpndCount(); i++) {
            Opnd * opnd = irManager->getOpnd(i);
            Opnd::MemOpndAlignment currentAlignment = alignmentSequence[j];
            if(opnd->getRefCount() != 0 
                    && opnd->getMemOpndKind() == MemOpndKind_StackAutoLayout
                    && opnd->getMemOpndAlignment() == currentAlignment) {
                Opnd * dispOpnd = opnd->getMemOpndSubOpnd(MemOpndSubOpndKind_Displacement);
                if (dispOpnd->getImmValue() == 0) {
                    if (relations[opnd->getId()].outerOpnd == NULL) {
                        if (currentAlignment == Opnd::MemOpndAlignment_Any) {
                            U_32 cb = getByteSize(opnd->getSize());
                            cb = (cb + (slotSize - 1)) & ~(slotSize - 1);
                            offset -= cb;
                        } else {
                            // Make sure 
                            assert((stackSizeAlignment % currentAlignment) == 0);
                            // It just doesn't make sense to align on less than operand size.
                            assert((U_32)currentAlignment >= getByteSize(opnd->getSize()));
                            offset -= currentAlignment;
                        }
                        dispOpnd->assignImmValue(offset);
                    }
                }
            }
        }
    }

    // Align stack pointer. Local area should preserve alignment available on function enter.
    offset &= ~(stackSizeAlignment - 1);

    // Assert local area is properly aligned.
    assert((offset & (STACK_ALIGNMENT - 1)) == 0);    
    
    localBase = offset;

    if (localEnd>localBase) {
        Inst* newIns = irManager->newInst(Mnemonic_SUB, irManager->getRegOpnd(STACK_REG), irManager->newImmOpnd(irManager->getTypeManager().getInt32Type(), localEnd - localBase));
        newIns->insertAfter(lastPush ? lastPush : entryPointInst);
    }

    frameSize = icalleeEnd -localBase;
}       

void StackLayouter::createEpilog()
{ // Predeccessors of en and irManager->isEpilog(en->pred)
    U_32 calleeSavedRegs = irManager->getCallingConvention()->getCalleeSavedRegs(OpndKind_GPReg).getMask();
    const Edges& inEdges = irManager->getFlowGraph()->getExitNode()->getInEdges();
    U_32 usageRegMask = irManager->getTotalRegUsage(OpndKind_GPReg);
    for (Edges::const_iterator ite = inEdges.begin(), ende = inEdges.end(); ite!=ende; ++ite) {
        Edge* edge = *ite;
        if (irManager->isEpilog(edge->getSourceNode())) {
            Node * epilog = edge->getSourceNode();
            Inst * retInst = (Inst*)epilog->getLastInst();
            assert(retInst->hasKind(Inst::Kind_RetInst));
            if (localEnd > localBase) {
                // Restore stack pointer.
                Inst* newIns = irManager->newInst(Mnemonic_ADD, irManager->getRegOpnd(STACK_REG), irManager->newImmOpnd(irManager->getTypeManager().getInt32Type(), localEnd - localBase));
                newIns->insertBefore(retInst);
            }
#ifdef _EM64T_
            for (U_32 reg = RegName_R15; reg >= RegName_RAX ; reg--) {//pop callee-save registers
#else
            for (U_32 reg = RegName_EDI; reg >= RegName_EAX ; reg--) {//pop callee-save registers
#endif
                U_32 mask = getRegMask((RegName)reg);
                if ((mask & calleeSavedRegs) &&  (usageRegMask & mask)) {
                    Inst* newIns = irManager->newInst(Mnemonic_POP, irManager->getRegOpnd((RegName)reg));
                    newIns->insertBefore(retInst);
                }
            }
        }
    }
}

}} //namespace Ia32

