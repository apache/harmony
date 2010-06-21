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

bool OpndUtils::isReg(const Opnd* op, RegName what)
{
    if (!op->hasAssignedPhysicalLocation()) {
        return false;
    }
    if (!op->isPlacedIn(OpndKind_Reg)) {
        return false;
    }
    if (what == RegName_Null) {
        return true;
    }
    return op->getRegName() == what;
}

bool OpndUtils::isXmmReg(const Opnd* op, RegName what)
{
    if (!isReg(op)) {
        return false;
    }
    const RegName regName = op->getRegName();
    if (getRegKind(regName) != OpndKind_XMMReg) {
        return false;
    }
    if (what == RegName_Null) {
        return true; 
    }
    return regName == what;
}

bool OpndUtils::isImm(const Opnd* op)
{
    return op->isPlacedIn(OpndKind_Imm) && (op->getRuntimeInfo() == NULL);
}

bool OpndUtils::isImm(const Opnd* op, int iVal)
{
    return isImm(op) && (op->getImmValue() == iVal);
}

bool OpndUtils::isImm8(const Opnd* op)
{
    return isImm(op) && op->getSize() == OpndSize_8;
}

bool OpndUtils::isImm32(const Opnd* op)
{
    return isImm(op) && op->getSize() == OpndSize_32;
}

bool OpndUtils::fitsImm8(const Opnd* op)
{
    return isImm(op) && 
           (CHAR_MIN <= op->getImmValue() && op->getImmValue() <= CHAR_MAX);
}

bool OpndUtils::isMem(const Opnd* op)
{
    return op->isPlacedIn(OpndKind_Mem);
}

bool OpndUtils::isZeroImm(const Opnd* op)
{
    return isImm(op, 0);
}

bool OpndUtils::isSingleDef(const Opnd* opnd)
{
    return isImm(opnd) || (opnd->getDefiningInst() != NULL);
}


const void* OpndUtils::extractAddrOfConst(const Opnd* op)
{
    if (op->getMemOpndKind() != MemOpndKind_ConstantArea) {
        return NULL;
    }
    // Actually, it's currently only works for IA-32 - I expect 
    // the address of constant completely in the displacement.
    // On Intel64, the address already get loaded into a register, 
    // so more complicated analysis needed to find the proper constant
    Opnd* disp = op->getMemOpndSubOpnd(MemOpndSubOpndKind_Displacement);
    if (disp == NULL) {
        // Perhaps, it's IA-32?
        return NULL;
    }
    
    Opnd::RuntimeInfo* rtInfo = disp->getRuntimeInfo();
    assert(rtInfo != NULL);
    assert(rtInfo->getKind() == Opnd::RuntimeInfo::Kind_ConstantAreaItem);
    ConstantAreaItem* item = (ConstantAreaItem*)rtInfo->getValue(0);
    // At this point we must have the address...
    assert(item->getValue()!= NULL);
    return item->getValue();
}

bool OpndUtils::isConstAreaItem(const Opnd* op)
{
    return extractAddrOfConst(op) != NULL;
}

bool OpndUtils::isFPConst(const Opnd* op, double dVal)
{
    const void* addr = extractAddrOfConst(op);
    return (addr == NULL) ? false : (dVal == *(const double*)addr);
}

bool OpndUtils::isFPConst(const Opnd* op, float fVal)
{
    const void* addr = extractAddrOfConst(op);
    return (addr == NULL) ? false : (fVal == *(const float*)addr);
}

int OpndUtils::extractIntConst(const Opnd* op)
{
    assert(isConstAreaItem(op));
    const void* addr = extractAddrOfConst(op);
    return *(const int*)addr;
}

double OpndUtils::extractDoubleConst(const Opnd* op)
{
    assert(isConstAreaItem(op));
    const void* addr = extractAddrOfConst(op);
    return *(const double*)addr;
}

float OpndUtils::extractFloatConst(const Opnd* op)
{
    assert(isConstAreaItem(op));
    const void* addr = extractAddrOfConst(op);
    return *(const float*)addr;
}

bool OpndUtils::equals(const Opnd* a, const Opnd* b)
{
    //TODO:
    return a == b;
}

Opnd* OpndUtils::convertImmToImm8(Opnd* imm)
{
    if (isImm8(imm)) {
        return imm;
    }
    assert(fitsImm8(imm));
    TypeManager& typeMan = m_irManager->getTypeManager();
    Type* int8type = typeMan.getInt8Type();
    Opnd* imm8 = m_irManager->newImmOpnd(int8type, imm->getImmValue());
    return imm8;
}

Opnd* OpndUtils::convertToXmmReg64(Opnd* xmmReg)
{
    assert(isXmmReg(xmmReg));
    RegName regName = xmmReg->getRegName();
    OpndSize size = getRegSize(regName);
    if (size == OpndSize_64) {
        return xmmReg;
    }
    TypeManager& typeMan = m_irManager->getTypeManager();
    Type* doubleType = typeMan.getDoubleType();
    unsigned regIndex = getRegIndex(regName);
    RegName regName64 = getRegName(OpndKind_XMMReg, OpndSize_64, regIndex);
    Opnd* xmm64 = m_irManager->newRegOpnd(doubleType, regName64);
    return xmm64;
}

Opnd* OpndUtils::getZeroConst(Type* type)
{
    if (type->isDouble()) {
        return getDoubleZeroConst();
    }
    if (type->isSingle()) {
        return getFloatZeroConst();
    }
    if (type->isInt4()) {
        return getIntZeroConst();
    }
    return m_irManager->newImmOpnd(type, 0);
}

Opnd* OpndUtils::getIntZeroConst(void)
{
    if (m_opndIntZero == NULL) {
        Type* type = m_irManager->getTypeFromTag(Type::Int32);
        m_opndIntZero = m_irManager->newImmOpnd(type, 0);
    }
    return m_opndIntZero;
}
Opnd* OpndUtils::getDoubleZeroConst(void)
{
    if (m_opndDoubleZero == NULL) {
        m_opndDoubleZero = m_irManager->newFPConstantMemOpnd((double)0);
    }
    return m_opndDoubleZero;
}

Opnd* OpndUtils::getFloatZeroConst(void)
{
    if (m_opndFloatZero == NULL) {
        m_opndFloatZero = m_irManager->newFPConstantMemOpnd((float)0);
    }
    return m_opndFloatZero;
}

Opnd* OpndUtils::findImmediateSource(Opnd* opnd) 
{
    Opnd* res = opnd;
    while (!res->isPlacedIn(OpndKind_Imm)) {
        Inst* defInst = res->getDefiningInst();
        if (!defInst || defInst->getMnemonic()!=Mnemonic_MOV) {
            return NULL;
        }
        res = defInst->getOpnd(1);
    }
    return res;
}

//All CALL insts except some special helpers that never cause stacktrace printing
bool InstUtils::instMustHaveBCMapping(Inst* inst) {
    if (!inst->hasKind(Inst::Kind_CallInst)) {
        return false;
    }
    CallInst* callInst = (CallInst*)inst;
    Opnd * targetOpnd=callInst->getOpnd(callInst->getTargetOpndIndex());
    Opnd* immOpnd = OpndUtils::findImmediateSource(targetOpnd);
    Opnd::RuntimeInfo * ri = immOpnd ? immOpnd->getRuntimeInfo() : NULL;
    if(!ri) {
        return true;
    } else if (ri->getKind() == Opnd::RuntimeInfo::Kind_InternalHelperAddress) { 
        return false;
    } else if (ri->getKind() == Opnd::RuntimeInfo::Kind_HelperAddress) { 
        VM_RT_SUPPORT helperId = (VM_RT_SUPPORT)(POINTER_SIZE_INT)ri->getValue(0);
        switch (helperId) {
            case VM_RT_GC_GET_TLS_BASE:
            case VM_RT_GC_SAFE_POINT:
                return false;
            default:
                break;
        }
    }
    return true;
}


void InstUtils::replaceInst(Inst* toBeReplaced, Inst* brandNewInst)
{
    BasicBlock* bb = toBeReplaced->getBasicBlock();
    bb->appendInst(brandNewInst, toBeReplaced);
    toBeReplaced->unlink();
}

void InstUtils::replaceOpnd(Inst* inst, unsigned index, Opnd* newOpnd)
{
    Opnd* oldOpnd = inst->getOpnd(index);
    // to be *replaced*, an operand must exist first
    assert(oldOpnd != NULL);
    if (oldOpnd != newOpnd) {
        inst->replaceOpnd(oldOpnd, newOpnd);
    }
}


ControlFlowGraph* SubCfgBuilderUtils::newSubGFG(bool withReturn, bool withUnwind)
{
    m_subCFG = m_irManager->createSubCFG(withReturn, withUnwind);
    return m_subCFG;
}

Node* SubCfgBuilderUtils::getSubCfgEntryNode(void)
{
    return m_subCFG->getEntryNode();
}

Node* SubCfgBuilderUtils::getSubCfgReturnNode(void)
{
    return m_subCFG->getReturnNode();
}

BasicBlock* SubCfgBuilderUtils::newBB(void)
{
    BasicBlock* bb = (BasicBlock*)m_subCFG->createBlockNode();
    setCurrentNode(bb);
    return bb;
}

Node* SubCfgBuilderUtils::setCurrentNode(Node* node)
{
    Node* old = m_currNode;
    m_currNode = node;
    return old;
}

Node* SubCfgBuilderUtils::getCurrentNode(void) const
{
    return m_currNode;
}

Inst* SubCfgBuilderUtils::newInst(
    Mnemonic mn, 
    unsigned defsCount, 
    Opnd* op0, Opnd* op1, Opnd* op2, 
    Opnd* op3, Opnd* op4, Opnd* op5)
{
    Inst* inst = NULL;
    if (mn == Mnemonic_MOV) {
        assert(op0 != NULL && op1 != NULL);
        assert(op2==NULL && op3 == NULL);
        assert(op4 == NULL && op5 == NULL);
        inst = m_irManager->newCopyPseudoInst(mn, op0, op1);
    }
    else {
        inst = m_irManager->newInstEx(mn, defsCount, op0, op1, op2, op3, op4, op5);
    }
    m_currNode->appendInst(inst);
    return inst;
}

Inst* SubCfgBuilderUtils::newInst(
    Mnemonic mn, Opnd* op0, Opnd* op1, Opnd*op2)
{
    if (mn == Mnemonic_MOV) {
        // special handling in another newInst()
        return newInst(mn, 0, op0, op1, op2);
        
    }
    Inst* inst = m_irManager->newInst(mn, op0, op1, op2);
    m_currNode->appendInst(inst);
    return inst;
}

Inst* SubCfgBuilderUtils::newBranch(
    Mnemonic mn, 
    Node* trueTarget, Node* falseTarget,
    double trueProbability, double falseProbability)
{
    Inst* branch = m_irManager->newBranchInst(mn, trueTarget, falseTarget);
    m_currNode->appendInst(branch);
    m_subCFG->addEdge(m_currNode, trueTarget, trueProbability);
    m_subCFG->addEdge(m_currNode, falseTarget, falseProbability);
    return branch;
}

void SubCfgBuilderUtils::connectNodes(Node* from, Node* to)
{
    m_subCFG->addEdge(from, to);
}

void SubCfgBuilderUtils::connectNodeTo(Node* to)
{
    connectNodes(m_currNode, to);
}

ControlFlowGraph* SubCfgBuilderUtils::getSubCFG(void)
{
    return m_subCFG;
}

ControlFlowGraph* SubCfgBuilderUtils::setSubCFG(ControlFlowGraph* subCFG)
{
    ControlFlowGraph* old = m_subCFG;
    m_subCFG = subCFG;
    return old;
}

void SubCfgBuilderUtils::propagateSubCFG(Inst* inst, bool purgeEmptyNodes)
{
    ControlFlowGraph* mainCFG = m_irManager->getFlowGraph();
    mainCFG->spliceFlowGraphInline(inst, *m_subCFG);
    inst->unlink();
    if (purgeEmptyNodes) {
        mainCFG->purgeEmptyNodes(true, false);
    }
    m_subCFG = NULL;
}

}}; // ~namespace Jitrino::Ia32
