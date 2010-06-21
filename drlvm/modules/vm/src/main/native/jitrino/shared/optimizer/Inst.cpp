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

#include <stdio.h>

#include "Inst.h"
#include "Type.h"
#include "IRBuilder.h"


namespace Jitrino {

//-----------------------------------------------------------------------------
// Inst constructors
//-----------------------------------------------------------------------------

Inst::Inst(Opcode opcode, Modifier mod, Type::Tag type, Opnd* dst_)
    : operation(opcode, type, mod), numSrcs(0), dst(0)
{
    setDst(dst_);
}

Inst::Inst(Opcode opcode, Modifier mod, Type::Tag type, Opnd* dst_, Opnd* src)
    : operation(opcode, type, mod), numSrcs(1), dst(0)
{
    setDst(dst_);
    srcs[0] = src;
}

Inst::Inst(Opcode opcode, Modifier mod, Type::Tag type, Opnd* dst_,
           Opnd* src1, Opnd* src2)
    : operation(opcode, type, mod), numSrcs(2), dst(0)
{
    setDst(dst_);
    srcs[0] = src1;
    srcs[1] = src2;
}

Inst::Inst(Opcode opcode, Modifier mod, Type::Tag type, Opnd* dst_, U_32 nSrcs)
    : operation(opcode, type, mod), numSrcs(nSrcs), dst(0)
{
    setDst(dst_);
}

Opnd* Inst::getSrcExtended(U_32 srcIndex) const {
    assert(0);
    return NULL;
}

bool Inst::isThrow() const {
    return ((getOpcode() == Op_Throw) || (getOpcode() == Op_ThrowSystemException) || 
        (getOpcode() == Op_ThrowLinkingException) ||
        ( (getOpcode() == Op_VMHelperCall) ?
            (asVMHelperCallInst()->isThrowLazy()) : false ) );
}


Edge::Kind Inst::getEdgeKind(const Edge* edge) const {
#ifdef _DEBUG
    Node* node = edge->getSourceNode();
    Node* succ = edge->getTargetNode();
    assert(node->isBlockNode());
    assert(succ->isBlockNode());
    assert(node->getLastInst() == this);
    assert(node->getOutDegree() <= 2); //uncond + exception
#endif
    return Edge::Kind_Unconditional;// jump
}

void Inst::removeRedundantBranch() {
    if (getOpcode() == Op_Branch || isSwitch()) {
        unlink();
    }
}


//-----------------------------------------------------------------------------
// BranchInst utilities
//-----------------------------------------------------------------------------

void BranchInst::updateControlTransferInst(Node *oldTarget, Node* newTarget ) {
    assert(oldTarget->getKind() == newTarget->getKind());

    LabelInst*  oldLabel = (LabelInst*)oldTarget->getFirstInst();
    LabelInst*  newLabel = (LabelInst*)newTarget->getFirstInst();

    // Update source nodes branch instruction
    if(getTargetLabel() == oldLabel) {
        assert(newTarget->isBlockNode());
        replaceTargetLabel(newLabel);
    }
}

// the following utility for conditional branches will make the fallthrough the target and
// the target the fallthrough, changing the branch condition in the process

void  BranchInst::swapTargets(LabelInst *target) {
    ComparisonModifier modifier = getComparisonModifier();
    switch (modifier) {
    // binary boolean comparisons. NOTE: the operands are also swapped
    case Cmp_EQ:       modifier = Cmp_NE_Un;   break;
    case Cmp_NE_Un:    modifier = Cmp_EQ;      break;
    case Cmp_GT:       modifier = Cmp_GTE;     break;
    case Cmp_GT_Un:    modifier = Cmp_GTE_Un;  break;
    case Cmp_GTE:      modifier = Cmp_GT;      break;
    case Cmp_GTE_Un:   modifier = Cmp_GT_Un;   break;
    // unary boolean comparisons
    case Cmp_Zero:     modifier = Cmp_NonZero; break;
    case Cmp_NonZero:  modifier = Cmp_Zero;    break;
    default:
        assert(0);
    }
    setComparisonModifier(modifier);
    if (getNumSrcOperands()==2) {
        Opnd *src0 = getSrc(0);
        Opnd *src1 = getSrc(1);
        setSrc(0, src1);
        setSrc(1, src0);
    }
    replaceTargetLabel(target);
}

Edge::Kind BranchInst::getEdgeKind(const Edge* edge) const {
    Node* succ = edge->getTargetNode();
#ifdef _DEBUG
    Node* node = edge->getSourceNode();
    assert(node->isBlockNode());
    assert(succ->isBlockNode());
    assert(node->getLastInst() == this);
    assert(node->getOutDegree() == 2 || (node->getOutDegree() == 3 && node->getExceptionEdge()!=NULL));
#endif
    Edge::Kind kind =  getTargetLabel() == succ->getFirstInst() ? Edge::Kind_True : Edge::Kind_False;
    return kind;
}

// the following utility for conditional branches will return the taken edge based on the
// incoming condition. This will ignore exception edges

Edge* BranchInst::getTakenEdge(U_32 result) {
    // find the node for this branch instruction
    Node *node = getNode();
    assert(node->getFirstInst()->isLabel());
    LabelInst *targetLabel = getTargetLabel();
    Edge* edge = NULL;
    const Edges& edges = node->getOutEdges();
    for(Edges::const_iterator eiter = edges.begin(); eiter != edges.end(); ++eiter) {
        edge = *eiter;
        Node * tar = edge->getTargetNode();
        if ( (!tar->isDispatchNode() &&
              ((result == 0) && (tar->getFirstInst() != targetLabel))) ||
             ((result == 1) && (tar->getFirstInst() == targetLabel)) ) {
            break;
        }
    }
    assert(edge != NULL); 
        // was:
        // std::cout << "WARNING, useless branch\n";
        // if(node->getOutEdges().empty())
        //    return NULL;
        // else
        //     return node->getOutEdges().front();
    return edge;
}

//-----------------------------------------------------------------------------
// Inst printing methods
//-----------------------------------------------------------------------------

void Inst::print(::std::ostream& os) const {
    os << "I" << (int)id << ":";
    printFormatString(os, getOperation().getOpcodeFormatString());
}

void Inst::handlePrintEscape(::std::ostream& os, char code) const {
    switch (code) {
    case 's': // src operands
        {
            bool comma = false;
            for (U_32 i=0; i<numSrcs; i++) {
                if (comma)
                    os << ", ";
                getSrc(i)->print(os);
                comma = true;
            }
        }
        break;
    case 'a': // arguments to a virtual or indirect call
        {
            bool comma = false;
            for (U_32 i=3; i<numSrcs; i++) {
                if (comma)
                    os << ", ";
                getSrc(i)->print(os);
                comma = true;
            }
        }
        break;
    case 'b': // bytecode mapping info
        {
            if (getBCOffset() == ILLEGAL_BC_MAPPING_VALUE) {
                Node* node = getNode();
                if (node!=NULL && node->isBlockNode()) { // write 'unknown' only for block nodes
                    os<<"bcmap:unknown ";
                }
            } else {
                I_32 bcOffset = (I_32)getBCOffset();
                os<<"bcmap:"<<bcOffset<<" ";
            }
        }
        break;
    case 'p': // arguments to a more direct call
        {
            bool comma = false;
            for (U_32 i=2; i<numSrcs; i++) {
                if (comma)
                    os << ", ";
                getSrc(i)->print(os);
                comma = true;
            }
        }
        break;
    case 'l':
        {
            if (getDst()->isNull() != true)
                getDst()->printWithType(os);
        }
        break;
    case 'n': // new line
        os << ::std::endl;
        break;
    case 'm': // modifier
        os << getOperation().getModifierString();
        break;
    case 't': // instruction type
        os << Type::getPrintString(getType());
        break;
    case '0':
        getSrc(0)->print(os);
        break;
    case '1':
        getSrc(1)->print(os);
        break;
    case '2':
        getSrc(2)->print(os);
        break;
    case '3':
        getSrc(3)->print(os);
        break;
    case '4':
        getSrc(4)->print(os);
        break;
    case '5':
        getSrc(5)->print(os);
        break;
    default:
        os << '?' << code << '?';
        break;
    }
}

const char *messageStr(const char *string) {
    if (strcmp(string, "<init>")==0)
        return "_init_";
    if (strcmp(string, "<clinit>")==0)
        return "_clinit_";
    return string;
}

void MethodEntryInst::handlePrintEscape(::std::ostream& os, char code) const {
    switch (code) {
    case 'd':
        os  << methodDesc->getParentType()->getName()
            << "::" << messageStr(methodDesc->getName());
        break;
    default:
        Inst::handlePrintEscape(os, code);
        break;
    }
}

void MethodMarkerInst::handlePrintEscape(::std::ostream& os, char code) const {
    switch (code) {
    case 'd':
        os  << methodDesc->getParentType()->getName()
            << "::" << messageStr(methodDesc->getName());
        break;
    default:
        Inst::handlePrintEscape(os, code);
        break;
    }
}

void LabelInst::printId(::std::ostream&os) const {
    os << "L" << (int)getLabelId();
}

void LabelInst::handlePrintEscape(::std::ostream& os, char code) const {
    switch (code) {
    case 'l': // label
        os << "L" << (int)getLabelId();
        break;
    default:
        Inst::handlePrintEscape(os, code);
        break;
    }
}

void DispatchLabelInst::printId(::std::ostream&os) const {
    os << "D" << (int)getLabelId();
}

void DispatchLabelInst::handlePrintEscape(::std::ostream& os, char code) const {
    switch (code) {
    case 'l': // label
        os << "D" << (int)getLabelId();
        break;
    default:
        Inst::handlePrintEscape(os, code);
        break;
    }
}

void CatchLabelInst::printId(::std::ostream&os) const {
    os << "C" << (int)getLabelId();
}

void CatchLabelInst::handlePrintEscape(::std::ostream& os, char code) const {
    switch (code) {
    case 'l': // label
        os << "C" << (int)getLabelId() << " ord:" << (int)getOrder() << " ";
        getExceptionType()->print(os);
        break;
    default:
        Inst::handlePrintEscape(os, code);
        break;
    }
}

void BranchInst::handlePrintEscape(::std::ostream& os, char code) const {
    switch (code) {
    case 'l':  // label
        os << "L" << (int)targetLabel->getLabelId();
        break;
    default:
        Inst::handlePrintEscape(os, code);
        break;
    }
}

void ConstInst::handlePrintEscape(::std::ostream& os, char code) const {
    if (code != 'c') {
        Inst::handlePrintEscape(os, code);
        return;
    }
    // print constant value
    switch (getType()) {
    case Type::Int32:
        os << value.i4;
        break;
    case Type::Int64:
        os << (unsigned long)value.i8;
        break;
    case Type::IntPtr:
    case Type::UIntPtr:
    case Type::UnmanagedPtr:
        os << value.i;
        break;
    case Type::Single:
        os << value.s;
        break;
    case Type::Double:
        os << value.d;
        break;
    case Type::NullObject:
        os << "null";
        break;
    case Type::CompressedNullObject:
        os << "cnull";
        break;
    default: assert(0);
    }
}

void VarAccessInst::handlePrintEscape(::std::ostream& os, char code) const {
    Inst::handlePrintEscape(os, code);
}

void TokenInst::handlePrintEscape(::std::ostream& os, char code) const {
    switch (code) {
    case 'd':  // token
        os << (int)token;
        break;
    default:
        Inst::handlePrintEscape(os, code);
        break;
    }
}

void FieldAccessInst::handlePrintEscape(::std::ostream& os, char code) const {
    switch (code) {
    case 'd':
        os  << fieldDesc->getParentType()->getName()
            << "::" << fieldDesc->getName();
        break;
    default:
        Inst::handlePrintEscape(os, code);
        break;
    }
}

void MethodInst::handlePrintEscape(::std::ostream& os, char code) const {
    switch (code) {
    case 'd':
        os  << methodDesc->getParentType()->getName()
            << "::" << messageStr(methodDesc->getName());
        break;
    default:
        Inst::handlePrintEscape(os, code);
        break;
    }
}

void JitHelperCallInst::handlePrintEscape(::std::ostream& os, char code) const {
    switch(code) {
    case 'd':
        switch(jitHelperId) {
    case Prefetch:
        os << "Prefetch"; break;
    case Memset0:
        os << "Memset0"; break;
    case InitializeArray:
        os << "InitializeArray"; break;
    case SaveThisState:
        os << "SaveThisState"; break;
    case ReadThisState:
        os << "ReadThisState"; break;
    case LockedCompareAndExchange:
        os << "LockedCmpExchange"; break;
    case AddValueProfileValue:
        os << "AddValueProfileValue"; break;
    case FillArrayWithConst:
        os << "FillArrayWithConst"; break;
    case ArrayCopyDirect:
        os << "ArrayCopyDirect"; break;
    case ArrayCopyReverse:
        os << "ArrayCopyReverse"; break;
    case StringCompareTo:
        os << "StringCompareTo"; break;
    case StringIndexOf:
        os << "StringIndexOf"; break;
    case StringRegionMatches:
        os << "StringRegionMatches"; break;
    case ClassIsArray:
        os << "ClassIsArray"; break;
    case ClassGetAllocationHandle:
        os << "ClassGetAllocationHandle"; break;
    case ClassGetTypeSize:
        os << "ClassGetTypeSize"; break;
    case ClassGetArrayElemSize:
        os << "ClassGetArrayElemSize"; break;
    case ClassIsInterface:
        os << "ClassIsInterface"; break;
    case ClassIsFinal:
        os << "ClassIsFinal"; break;
    case ClassGetArrayClass:
        os << "ClassGetArrayClass"; break;
    case ClassGetFastCheckDepth:
        os << "ClassGetFastCheckDepth"; break;
    default:
        assert(0); break;
        }
        break;
    default:
        Inst::handlePrintEscape(os, code);
        break;
    }
}

void VMHelperCallInst::handlePrintEscape(::std::ostream& os, char code) const {
    switch(code) {
    case 'd': 
        {
            CompilationContext* cc = CompilationContext::getCurrentContext();
            const char* name = cc->getVMCompilationInterface()->getRuntimeHelperName(vmHelperId);
            os <<name<<" ";
        } 
        break;
    default:
        Inst::handlePrintEscape(os, code);
        break;
    }
}


void TypeInst::handlePrintEscape(::std::ostream& os, char code) const {
    switch (code) {
    case 'd':  // typeDesc
        type->print(os);
        break;
    default:
        Inst::handlePrintEscape(os, code);
        break;
    }
}

void SwitchInst::updateControlTransferInst(Node *oldTarget, Node* newTarget ) {
    assert(oldTarget->getKind() == newTarget->getKind());

    LabelInst*  oldLabel = (LabelInst*)oldTarget->getFirstInst();
    LabelInst*  newLabel = (LabelInst*)newTarget->getFirstInst();

    if (getDefaultTarget() == oldLabel) {
        assert(newTarget->isBlockNode());
        replaceDefaultTargetLabel(newLabel);
    }
    U_32 n = getNumTargets();
    for (U_32 i = 0; i < n; i++) {
        if (getTarget(i) == oldLabel) {
            assert(newTarget->isBlockNode());
            replaceTargetLabel(i, newLabel);
        }
    }
}

Edge::Kind SwitchInst::getEdgeKind(const Edge* edge) const {
    Node* succ = edge->getTargetNode();
#ifdef _DEBUG
    Node* node = edge->getSourceNode();
    assert(node->isBlockNode());
    assert(succ->isBlockNode());
    assert(node->getLastInst() == this);
#endif
    Edge::Kind kind = getDefaultTarget() == succ->getFirstInst()? Edge::Kind_False : Edge::Kind_True;
    return kind;
}

void SwitchInst::handlePrintEscape(::std::ostream& os, char code) const {
    switch (code) {
    case 'l':  // target labels
        {
            bool comma = false;
            for (U_32 i=0; i<numTargets; i++) {
                if (comma)
                    os << ", ";
                comma = true;
                os << "L" << (int)targetInsts[i]->getLabelId();
            }
            os << ", DEF:L" << (int)defaultTargetInst->getLabelId();
        }
        break;
    default:
        Inst::handlePrintEscape(os, code);
        break;
    }
}

extern void printPiCondition(const PiCondition* cond, ::std::ostream &os);

void TauPiInst::handlePrintEscape(::std::ostream& os, char code) const {
    switch (code) {
    case 'd':
        printPiCondition(cond, os);
        break;
    default:
        Inst::handlePrintEscape(os, code);
        break;
    }
}

void Inst::printFormatString(::std::ostream& os, const char* formatString) const {
    const char* str = formatString;
    for (char c = *str; c != '\0'; c = *++str) {
        if (c == '%') {
            // escape
            str++;
            handlePrintEscape(os, *str);
            continue;
        }
        os << c;
    }
}


//-----------------------------------------------------------------------------
// Inst duplication
//-----------------------------------------------------------------------------
class CloneVisitor : public InstFormatVisitor {
private:
    InstFactory&        instFactory;
    OpndManager&        opndManager;
    OpndRenameTable&    renameTable;
    Inst* clone;
public:
    CloneVisitor(InstFactory& ifactory, OpndManager& om, OpndRenameTable& rt)
        : instFactory(ifactory), opndManager(om), renameTable(rt), clone(NULL)
    {
    }
    Inst*   getClone()  {return clone;}
    void accept(Inst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(LabelInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(DispatchLabelInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(CatchLabelInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(JitHelperCallInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(VMHelperCallInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(MethodEntryInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(MethodMarkerInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(BranchInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(SwitchInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(ConstInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(TokenInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(LinkingExcInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(VarAccessInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(TypeInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(FieldAccessInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(MethodInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(MethodCallInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(CallInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(PhiInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(TauPiInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
    void accept(MultiSrcInst* inst) {
        clone = instFactory.makeClone(inst, opndManager, renameTable);
    }
};

Inst*
InstFactory::clone(Inst* inst, OpndManager& opndManager, OpndRenameTable *table) {
    CloneVisitor cloneVisitor(*this, opndManager, *table);
    inst->visit(cloneVisitor);
    Inst* newInst = cloneVisitor.getClone();
    newInst->setBCOffset(inst->getBCOffset());
    return newInst;
}

Inst*
InstFactory::makeClone(Inst* inst,
                       OpndManager& opndManager,
                       OpndRenameTable& table) {
    Inst *newInst = NULL;
    switch(inst->getNumSrcOperands()) {
    case 0:
        newInst = makeInst(inst->getOpcode(),
                           inst->getModifier(),
                           inst->getType(),
                           table.duplicate(opndManager, inst->getDst()));
        break;
    case 1:
        newInst = makeInst(inst->getOpcode(),
                           inst->getModifier(),
                           inst->getType(),
                           table.duplicate(opndManager, inst->getDst()),
                           table.rename(inst->getSrc(0)));
        break;
    case 2:
        newInst = makeInst(inst->getOpcode(),
                           inst->getModifier(),
                           inst->getType(),
                           table.duplicate(opndManager, inst->getDst()),
                           table.rename(inst->getSrc(0)),
                           table.rename(inst->getSrc(1)));
        break;
    default:
        assert(0);
    }
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

LabelInst*
InstFactory::makeClone(LabelInst* inst, OpndManager& opndManager, OpndRenameTable& table) {
    LabelInst *newInst = makeLabel();
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

DispatchLabelInst*
InstFactory::makeClone(DispatchLabelInst* inst, OpndManager& opndManager, OpndRenameTable& table) {
    assert(0);
    return NULL;
}

CatchLabelInst*
InstFactory::makeClone(CatchLabelInst* inst,
                       OpndManager& opndManager,
                       OpndRenameTable& table) {
    CatchLabelInst *newInst =makeCatchLabelInst(createLabelNumber(), 
                                                inst->getOrder(), inst->getExceptionType());
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

MethodEntryInst*
InstFactory::makeClone(MethodEntryInst* inst,
                       OpndManager& opndManager,
                       OpndRenameTable& table) {
    MethodEntryInst *newInst = makeMethodEntryInst(createLabelNumber(),
                                                   inst->methodDesc);
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

MethodMarkerInst*
InstFactory::makeClone(MethodMarkerInst* inst,
                       OpndManager& opndManager,
                       OpndRenameTable& table) {
    MethodMarkerInst *newInst = NULL;
    if (inst->getNumSrcOperands() > 0) {
        newInst = makeMethodMarkerInst(inst->kind, inst->methodDesc, 
                                       table.rename(inst->getSrc(0)));
    } else {
        newInst = makeMethodMarkerInst(inst->kind, inst->methodDesc);
    }
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

BranchInst*
InstFactory::makeClone(BranchInst* inst, OpndManager& opndManager, OpndRenameTable& table) {
    BranchInst *newInst = NULL;
    switch (inst->getNumSrcOperands()) {
    case 0:
        newInst = makeBranchInst(inst->getOpcode(), inst->getTargetLabel());
        break;
    case 1:
        newInst = makeBranchInst(inst->getOpcode(),
                                 inst->getComparisonModifier(),
                                 inst->getType(),
                                 table.rename(inst->getSrc(0)),
                                 inst->getTargetLabel());
        break;
    case 2:
        newInst = makeBranchInst(inst->getOpcode(),
                                 inst->getComparisonModifier(),
                                 inst->getType(),
                                 table.rename(inst->getSrc(0)),
                                 table.rename(inst->getSrc(1)),
                                 inst->getTargetLabel());
        break;
    default: assert(0);
    }
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

SwitchInst*
InstFactory::makeClone(SwitchInst* inst,
                       OpndManager& opndManager,
                       OpndRenameTable& table) {
    Opnd* src = table.rename(inst->getSrc(0));
    U_32 numTargets = inst->getNumTargets();
    LabelInst** targets = inst->getTargets();
    LabelInst** newTargets = new (memManager) LabelInst*[numTargets];
    for(U_32 i = 0; i < numTargets; ++i)
        newTargets[i] = targets[i];
    SwitchInst *newInst = makeSwitchInst(src, newTargets, numTargets, inst->getDefaultTarget());
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

ConstInst*
InstFactory::makeClone(ConstInst* inst,
                       OpndManager& opndManager,
                       OpndRenameTable& table) {
    ConstInst *newInst = NULL;
    switch(inst->getType()) {
    case Type::Int32:
        newInst = makeConstInst(table.duplicate(opndManager, inst->getDst()),
                                inst->getValue().i4);
        break;
    case Type::Int64:
        newInst = makeConstInst(table.duplicate(opndManager, inst->getDst()),
                                inst->getValue().i8);
        break;
    case Type::Single:
        newInst = makeConstInst(table.duplicate(opndManager, inst->getDst()),
                                inst->getValue().s);
        break;
    case Type::Double:
        newInst = makeConstInst(table.duplicate(opndManager, inst->getDst()),
                                inst->getValue().d);
        break;
    case Type::UnmanagedPtr:
        {
            ConstInst::ConstValue v;
            v.i8 = inst->getValue().i8;
            newInst = makeConstInst(table.duplicate(opndManager, inst->getDst()), v);
        }
        break;
    case Type::NullObject:
        newInst = makeConstInst(table.duplicate(opndManager, inst->getDst()));
        break;
    case Type::CompressedNullObject:
        newInst = makeConstInst(table.duplicate(opndManager, inst->getDst()));
        break;
    default: assert(0);
    }
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

TokenInst*
InstFactory::makeClone(TokenInst* inst,
                       OpndManager& opndManager,
                       OpndRenameTable& table) {
    TokenInst *newInst = makeTokenInst(inst->getOpcode(),
                                       inst->getModifier(),
                                       inst->getType(),
                                       table.duplicate(opndManager, inst->getDst()),
                                       inst->getToken(),
                                       inst->getEnclosingMethod());
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

LinkingExcInst*
InstFactory::makeClone(LinkingExcInst* inst,
                       OpndManager& opndManager,
                       OpndRenameTable& table) {
    LinkingExcInst *newInst = makeLinkingExcInst(inst->getOpcode(),
                                       inst->getModifier(),
                                       inst->getType(),
                                       table.duplicate(opndManager, inst->getDst()),
                                       inst->getEnclosingClass(),
                                       inst->getCPIndex(),
                                       inst->getOperation());
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

VarAccessInst*
InstFactory::makeClone(VarAccessInst* inst,
                       OpndManager& opndManager,
                       OpndRenameTable& table) {
    VarAccessInst *newInst = NULL;
    if (inst->getOpcode() == Op_StVar) {
        if (inst->getDst()->isVarOpnd()) {
            newInst = makeVarAccessInst(inst->getOpcode(),
                                        inst->getType(),
                                        table.duplicate(opndManager, inst->getDst())->asVarOpnd(),
                                        table.rename(inst->getSrc(0)));
        } else {
            newInst = makeVarAccessInst(inst->getOpcode(),
                                        inst->getType(),
                                        table.duplicate(opndManager, inst->getDst())->asSsaVarOpnd(),
                                        table.rename(inst->getSrc(0)));
        }
    } else {
        if (inst->getSrc(0)->isVarOpnd()) {
            newInst = makeVarAccessInst(inst->getOpcode(),
                                        inst->getType(),
                                        table.duplicate(opndManager, inst->getDst()),
                                        table.rename(inst->getSrc(0))->asVarOpnd());
        } else {
            newInst = makeVarAccessInst(inst->getOpcode(),
                                        inst->getType(),
                                        table.duplicate(opndManager, inst->getDst()),
                                        table.rename(inst->getSrc(0))->asSsaVarOpnd());
        }
    }
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

TypeInst*
InstFactory::makeClone(TypeInst* inst, OpndManager& opndManager, OpndRenameTable& table) {
    Opcode opc = inst->getOpcode();
    Modifier mod = inst->getModifier();
    Type::Tag tag = inst->getType();
    Opnd* dst = table.duplicate(opndManager, inst->getDst());
    U_32 numSrcs = inst->getNumSrcOperands();
    TypeInst *newInst = NULL;
    switch(inst->getNumSrcOperands()) {
    case 0:
        newInst = makeTypeInst(opc, mod, tag, dst, inst->getTypeInfo());
        break;
    case 1:
        newInst = makeTypeInst(opc, mod, tag, dst,table.rename(inst->getSrc(0)), inst->getTypeInfo());
        break;
    case 2:
        newInst = makeTypeInst(opc, mod, tag, dst, table.rename(inst->getSrc(0)), table.rename(inst->getSrc(1)),
                               inst->getTypeInfo());
        break;
    default:
        Opnd** opnds = new (memManager) Opnd*[numSrcs];
        for (U_32 i=0; i<numSrcs; i++)
            opnds[i] = table.rename(inst->getSrc(i));
        newInst = makeTypeInst(opc, mod, tag, dst, numSrcs, opnds, inst->getTypeInfo());
        break;
    }
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

FieldAccessInst*
InstFactory::makeClone(FieldAccessInst* inst, OpndManager& opndManager, OpndRenameTable& table) {
    Opcode opc = inst->getOpcode();
    Modifier mod = inst->getModifier();
    Type::Tag tag = inst->getType();
    Opnd* dst = table.duplicate(opndManager, inst->getDst());
    U_32 numSrcs = inst->getNumSrcOperands();
    FieldAccessInst *newInst = NULL;
    switch(inst->getNumSrcOperands()) {
    case 0:
        newInst = makeFieldAccessInst(opc, mod, tag, dst, inst->fieldDesc);
        break;
    case 1:
        newInst = makeFieldAccessInst(opc, mod, tag, dst, table.rename(inst->getSrc(0)),
                                      inst->fieldDesc);
        break;
    case 2:
        newInst = makeFieldAccessInst(opc, mod, tag, dst, table.rename(inst->getSrc(0)), 
                                      table.rename(inst->getSrc(1)), inst->fieldDesc);
        break;
    default:
        Opnd** opnds = new (memManager) Opnd*[numSrcs];
        for (U_32 i=0; i<numSrcs; i++)
            opnds[i] = table.rename(inst->getSrc(i));
        newInst = makeFieldAccessInst(opc, mod, tag, dst, numSrcs, opnds, inst->fieldDesc);
        break;
    }
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

MethodInst*
InstFactory::makeClone(MethodInst* inst,
                       OpndManager& opndManager,
                       OpndRenameTable& table) {
    Opcode opc = inst->getOpcode();
    Modifier mod = inst->getModifier();
    Type::Tag tag = inst->getType();
    Opnd* dst = table.duplicate(opndManager, inst->getDst());
    U_32 numSrcs = inst->getNumSrcOperands();
    MethodInst *newInst = NULL;
    switch(inst->getNumSrcOperands()) {
    case 0:
        newInst = makeMethodInst(opc, mod, tag, dst, inst->methodDesc);
        break;
    case 1:
        newInst = makeMethodInst(opc, mod, tag, dst, table.rename(inst->getSrc(0)),
                                 inst->methodDesc);
        break;
    default:
        Opnd** opnds = new (memManager) Opnd*[numSrcs];
        for (U_32 i=0; i<numSrcs; i++)
            opnds[i] = table.rename(inst->getSrc(i));
        newInst = makeMethodInst(opc, mod, tag, dst, numSrcs, opnds, inst->methodDesc);
        break;
    }
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

MethodCallInst*
InstFactory::makeClone(MethodCallInst* inst,
                       OpndManager& opndManager,
                       OpndRenameTable& table) {
    U_32 nsrcs = inst->getNumSrcOperands();
    Opnd** newArgs = new (memManager) Opnd*[nsrcs];
    for (U_32 i=0; i<nsrcs; i++)
        newArgs[i] = table.rename(inst->getSrc(i));
    MethodCallInst *newInst 
        = makeMethodCallInst(inst->getOpcode(),
                             inst->getModifier(),
                             inst->getType(),
                             table.duplicate(opndManager, inst->getDst()),
                             nsrcs,
                             newArgs,
                             inst->getMethodDesc());
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

CallInst*
InstFactory::makeClone(CallInst* inst,
                       OpndManager& opndManager,
                       OpndRenameTable& table) {
    U_32 numArgs = inst->getNumArgs();
    Opnd** newArgs = new (memManager) Opnd*[numArgs];
    for (U_32 i=0; i<numArgs; i++)
        newArgs[i] = table.rename(inst->getArg(i));

    CallInst *newInst = makeCallInst(inst->getOpcode(),
                        inst->getModifier(),
                        inst->getType(),
                        table.duplicate(opndManager, inst->getDst()),
                        table.rename(inst->getFunPtr()),
                        inst->getNumArgs(),
                        newArgs);
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

JitHelperCallInst*
InstFactory::makeClone(JitHelperCallInst* inst,
                       OpndManager& opndManager,
                       OpndRenameTable& table)
{
    U_32 nsrcs = inst->getNumSrcOperands();
    Opnd** newArgs = new (memManager) Opnd*[nsrcs];
    for (U_32 i=0; i<nsrcs; i++){
        newArgs[i] = table.rename(inst->getSrc(i));
    }
    JitHelperCallInst *newInst = makeJitHelperCallInst(inst->getOpcode(),
                                 inst->getModifier(),
                                 inst->getType(),
                                 table.duplicate(opndManager, inst->getDst()),
                                 nsrcs,
                                 newArgs,
                                 inst->getJitHelperId());
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

VMHelperCallInst*
InstFactory::makeClone(VMHelperCallInst* inst,
                       OpndManager& opndManager,
                       OpndRenameTable& table)
{
    U_32 nsrcs = inst->getNumSrcOperands();
    Opnd** newArgs = new (memManager) Opnd*[nsrcs];
    for (U_32 i=0; i<nsrcs; i++){
        newArgs[i] = table.rename(inst->getSrc(i));
    }
    VMHelperCallInst *newInst = makeVMHelperCallInst(inst->getOpcode(),
        inst->getModifier(),
        inst->getType(),
        table.duplicate(opndManager, inst->getDst()),
        nsrcs,
        newArgs,
        inst->getVMHelperId());
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

PhiInst*
InstFactory::makeClone(PhiInst* inst, OpndManager& opndManager, OpndRenameTable& table) {
    U_32 numArgs = inst->getNumSrcOperands();
    Opnd** newArgs = new (memManager) Opnd*[numArgs];
    for (U_32 i=0; i<numArgs; i++)
        newArgs[i] = table.rename(inst->getSrc(i));
    
    PhiInst *newInst = makePhiInst(inst->getType(),
                       table.duplicate(opndManager, inst->getDst()),
                       numArgs,
                       newArgs);
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

MultiSrcInst*
InstFactory::makeClone(MultiSrcInst* inst, OpndManager& opndManager, OpndRenameTable& table) {
    Opcode opc = inst->getOpcode();
    Modifier mod = inst->getModifier();
    Type::Tag tag = inst->getType();
    Opnd* dst = table.duplicate(opndManager, inst->getDst());
    U_32 numSrcs = inst->getNumSrcOperands();
    MultiSrcInst *newInst;
    switch(numSrcs) {
    case 0:
        newInst = makeMultiSrcInst(opc, mod, tag, dst);
        break;
    case 1:
        newInst = makeMultiSrcInst(opc, mod, tag, dst,table.rename(inst->getSrc(0)));
        break;
    case 2:
        newInst = makeMultiSrcInst(opc, mod, tag, dst, table.rename(inst->getSrc(0)), table.rename(inst->getSrc(1)));
        break;
    default:
        Opnd** opnds = new (memManager) Opnd*[numSrcs];
        for (U_32 i=0; i<numSrcs; i++)
            opnds[i] = table.rename(inst->getSrc(i));
        newInst = makeMultiSrcInst(opc, mod, tag, dst, numSrcs, opnds);
        break;
    }
    newInst->setPersistentInstructionId(inst->getPersistentInstructionId());
    return newInst;
}

//-----------------------------------------------------------------------------
// InstFactory methods
//-----------------------------------------------------------------------------
InstFactory::InstFactory(MemoryManager& mm, MethodDesc &md) : memManager(mm) {
    maxNumLabels = 0;
    numInsts = 0;
}

Opnd**
InstFactory::copyOpnds(Opnd** srcs, U_32 numSrcs) {
    Opnd** newSrcs = new (memManager) Opnd*[numSrcs];
    for (U_32 i=0; i<numSrcs; i++)
        newSrcs[i] = srcs[i];
    return newSrcs;
}

Opnd**
InstFactory::copyOpnds(Opnd* src1, Opnd** srcs, U_32 numSrcs) {
    Opnd** newSrcs = new (memManager) Opnd*[numSrcs+1];
    newSrcs[0] = src1;
    for (U_32 i=0; i<numSrcs; i++)
        newSrcs[i+1] = srcs[i];
    return newSrcs;
}

Opnd**
InstFactory::copyOpnds(Opnd* src1, Opnd* src2, Opnd** srcs, U_32 numSrcs) {
    Opnd** newSrcs = new (memManager) Opnd*[numSrcs+2];
    newSrcs[0] = src1;
    newSrcs[1] = src2;
    for (U_32 i=0; i<numSrcs; i++)
        newSrcs[i+2] = srcs[i];
    return newSrcs;
}

void
InstFactory::appendSrc(MultiSrcInst *inst, Opnd *opnd) {
    U_32 oldNumSrcs = inst->numSrcs;
    U_32 newNumSrcs = inst->numSrcs+1;
    if (newNumSrcs > MAX_INST_SRCS) {
        // an extended source
        U_32 oldExtendedSrcs = oldNumSrcs - MAX_INST_SRCS;
        U_32 newExtendedSrcs = newNumSrcs - MAX_INST_SRCS;
        if (newExtendedSrcs <= inst->extendedSrcSpace) {
            // fits in existing memory
            inst->extendedSrcs[oldExtendedSrcs] = opnd;
        } else {
            // need to allocate more memory
            U_32 newSpace = newExtendedSrcs * 2;
            Opnd** opnds = new (memManager) Opnd*[newSpace];
            Opnd** oldOpnds = inst->extendedSrcs;
            // copy existing opnds
            U_32 j;
            for (j = 0; j < oldExtendedSrcs; j++) 
                opnds[j] = oldOpnds[j];
            opnds[j++] = opnd;
            inst->extendedSrcs = opnds;
            inst->extendedSrcSpace = newSpace;
        }
        inst->numSrcs = newNumSrcs;
    } else {
        // fits in the normal sources
        inst->numSrcs = newNumSrcs;
        inst->setSrc(oldNumSrcs, opnd);
    }
}

//-----------------------------------------------------------------------------
// InstFactory factory methods for different instruction formats.  All Inst
// generation should use one of these factory methods.
//-----------------------------------------------------------------------------
Inst*
InstFactory::makeInst(Opcode op, Modifier mod, Type::Tag type, Opnd* dst) {
    Inst* inst = new (memManager) Inst(op, mod, type, dst);
    inst->id       = numInsts++;
    return inst;
}
Inst*
InstFactory::makeInst(Opcode op, Modifier mod, Type::Tag type, Opnd* dst, Opnd* src) {
    Inst* inst = new (memManager) Inst(op, mod, type, dst, src);
    inst->id       = numInsts++;
    return inst;
}
Inst*
InstFactory::makeInst(Opcode op,
                      Modifier mod,
                      Type::Tag type,
                      Opnd* dst,
                      Opnd* src1,
                      Opnd* src2) {
    Inst* inst = new (memManager) Inst(op, mod, type, dst, src1, src2);
    inst->id       = numInsts++;
    return inst;
}
LabelInst*
InstFactory::makeLabelInst(U_32 labelId) {
    LabelInst* inst = new (memManager) LabelInst(labelId);
    inst->id       = numInsts++;
    return inst;
}
LabelInst*
InstFactory::makeLabelInst(Opcode opc, U_32 labelId) {
    LabelInst* inst = new (memManager) LabelInst(opc, labelId);
    inst->id       = numInsts++;
    return inst;
}
DispatchLabelInst*
InstFactory::makeDispatchLabelInst(U_32 labelId) {
    DispatchLabelInst* inst = new (memManager) DispatchLabelInst(labelId);
    inst->id       = numInsts++;
    return inst;
}

CatchLabelInst *
InstFactory::makeCatchLabelInst(U_32 labelId,
                                   U_32 ord,
                                   Type *exceptionType) {
    CatchLabelInst* inst = new (memManager) CatchLabelInst(labelId, ord, exceptionType);
    inst->id       = numInsts++;
    return inst;
}

MethodEntryInst*
InstFactory::makeMethodEntryInst(U_32 labelId, MethodDesc* md)  {
    MethodEntryInst* inst = new (memManager) MethodEntryInst(labelId, md);
    inst->id       = numInsts++;
    return inst;
}

MethodMarkerInst*
InstFactory::makeMethodMarkerInst(MethodMarkerInst::Kind k, MethodDesc* md,
        Opnd *obj, Opnd *retOpnd) {
    assert(obj && !obj->isNull());
    MethodMarkerInst* inst = new (memManager) MethodMarkerInst(k, md, obj, retOpnd);
    inst->id = numInsts++;
    return inst;
}

MethodMarkerInst*
InstFactory::makeMethodMarkerInst(MethodMarkerInst::Kind k, MethodDesc* md, 
        Opnd *retOpnd) {
    MethodMarkerInst* inst = new (memManager) MethodMarkerInst(k, md, retOpnd);
    inst->id       = numInsts++;
    return inst;
}

MethodMarkerInst*
InstFactory::makeMethodMarkerInst(MethodMarkerInst::Kind k, MethodDesc* md) {
    MethodMarkerInst* inst = new (memManager) MethodMarkerInst(k, md);
    inst->id       = numInsts++;
    return inst;
}

BranchInst*
InstFactory::makeBranchInst(Opcode op, LabelInst* target) {
    BranchInst* inst = new (memManager) BranchInst(op, target);
    inst->id       = numInsts++;
    return inst;
}
BranchInst*
InstFactory::makeBranchInst(Opcode op,
                            Opnd* src,
                            LabelInst* target) {
    BranchInst* inst = new (memManager) BranchInst(op, src, target);
    inst->id       = numInsts++;
    return inst;
}
BranchInst*
InstFactory::makeBranchInst(Opcode op,
                            ComparisonModifier mod,
                            Type::Tag type,
                            Opnd* src,
                            LabelInst* target) {
    BranchInst* inst = new (memManager) BranchInst(op, mod, type, src, target);
    inst->id       = numInsts++;
    return inst;
}
BranchInst*
InstFactory::makeBranchInst(Opcode op,
                            ComparisonModifier mod,
                            Type::Tag type,
                            Opnd* src1,
                            Opnd* src2,
                            LabelInst* target) {
    BranchInst* inst = new (memManager) BranchInst(op, mod, type, src1, src2, target);
    inst->id       = numInsts++;
    return inst;
}

SwitchInst*
InstFactory::makeSwitchInst(Opnd* src, LabelInst** targets, U_32 nTargets, 
                            LabelInst* defTarget) {
    SwitchInst* inst = new (memManager) SwitchInst(src, targets, nTargets, defTarget);
    inst->id       = numInsts++;
    return inst;
}
ConstInst*
InstFactory::makeConstInst(Opnd* dst, I_32 i4) {
    ConstInst* inst = new (memManager) ConstInst(dst, i4);
    inst->id       = numInsts++;
    return inst;
}
ConstInst*
InstFactory::makeConstInst(Opnd* dst, int64 i8)  {
    ConstInst* inst = new (memManager) ConstInst(dst, i8);
    inst->id       = numInsts++;
    return inst;
}
ConstInst*
InstFactory::makeConstInst(Opnd* dst, float fs) {
    ConstInst* inst = new (memManager) ConstInst(dst, fs);
    inst->id       = numInsts++;
    return inst;
}
ConstInst*
InstFactory::makeConstInst(Opnd* dst, double fd)  {
    ConstInst* inst = new (memManager) ConstInst(dst, fd);
    inst->id       = numInsts++;
    return inst;
}
ConstInst*
InstFactory::makeConstInst(Opnd* dst, ConstInst::ConstValue val) {
    ConstInst* inst = new (memManager) ConstInst(dst, val);
    inst->id       = numInsts++;
    return inst;
}
ConstInst*
InstFactory::makeConstInst(Opnd* dst) {
    ConstInst* inst = new (memManager) ConstInst(dst);
    inst->id       = numInsts++;
    return inst;
}
TokenInst*
InstFactory::makeTokenInst(Opcode opc, Modifier mod, Type::Tag type, Opnd* dst, U_32 t, MethodDesc* encMethod) {
    TokenInst* inst = new (memManager) TokenInst(opc, mod, type, dst, t, encMethod);
    inst->id       = numInsts++;
    return inst;
}
LinkingExcInst*
InstFactory::makeLinkingExcInst(Opcode opc, Modifier mod, Type::Tag type, Opnd* dst,
                                Class_Handle encClass, U_32 CPIndex, U_32 operation) {
    LinkingExcInst* inst =
        new (memManager) LinkingExcInst(opc, mod, type, dst, encClass, CPIndex, operation);
    inst->id       = numInsts++;
    return inst;
}
VarAccessInst*
InstFactory::makeVarAccessInst(Opcode op, Type::Tag type, Opnd* dst, VarOpnd* var) {
    VarAccessInst* inst = new (memManager) VarAccessInst(op, type, dst, var);
    inst->id       = numInsts++;
    return inst;
}
VarAccessInst*
InstFactory::makeVarAccessInst(Opcode op, Type::Tag type, VarOpnd* var, Opnd* src) {
    VarAccessInst* inst = new (memManager) VarAccessInst(op, type, var, src);
    inst->id       = numInsts++;
    return inst;
}
VarAccessInst*
InstFactory::makeVarAccessInst(Opcode op, Type::Tag type, Opnd* dst, SsaVarOpnd* var) {
    VarAccessInst* inst = new (memManager) VarAccessInst(op, type, dst, var);
    inst->id       = numInsts++;
    return inst;
}
VarAccessInst*
InstFactory::makeVarAccessInst(Opcode op, Type::Tag type, SsaVarOpnd* var, Opnd* src) {
    VarAccessInst* inst = new (memManager) VarAccessInst(op, type, var, src);
    inst->id       = numInsts++;
    return inst;
}
TypeInst*
InstFactory::makeTypeInst(Opcode op, Modifier mod, Type::Tag ty, Opnd* dst, Type* td) {
    TypeInst* inst = new (memManager) TypeInst(op, mod, ty, dst, td);
    inst->id       = numInsts++;
    return inst;
}
TypeInst*
InstFactory::makeTypeInst(Opcode op, Modifier mod, Type::Tag ty, Opnd* dst,
    Opnd* src, Type* td) {
    TypeInst* inst = new (memManager) TypeInst(op, mod, ty, dst, src, td);
    inst->id       = numInsts++;
    return inst;
}
TypeInst*
InstFactory::makeTypeInst(Opcode op, Modifier mod, Type::Tag ty, Opnd* dst,
    Opnd* src1, Opnd* src2, Type* td) {
    TypeInst* inst = new (memManager) TypeInst(op, mod, ty, dst, src1, src2, td);
    inst->id       = numInsts++;
    return inst;
}
TypeInst*
InstFactory::makeTypeInst(Opcode op, Modifier mod, Type::Tag ty, Opnd* dst,
    Opnd* src1, Opnd* src2, Opnd* src3, Type* td) {
    Opnd* srcs_local[3] = { src1, src2, src3 };
    Opnd** srcs = copyOpnds(srcs_local, 3);
    TypeInst* inst = new (memManager) TypeInst(op, mod, ty, dst, 3, srcs, td);
    inst->id       = numInsts++;
    return inst;
}
TypeInst*
InstFactory::makeTypeInst(Opcode op, Modifier mod, Type::Tag ty, Opnd* dst,
    Opnd* src1, Opnd* src2, Opnd* src3, Opnd* src4, Type* td) {
    Opnd* srcs_local[4] = { src1, src2, src3, src4 };
    Opnd** srcs = copyOpnds(srcs_local, 4);
    TypeInst* inst = new (memManager) TypeInst(op, mod, ty, dst, 4, srcs, td);
    inst->id       = numInsts++;
    return inst;
}
TypeInst*
InstFactory::makeTypeInst(Opcode op, Modifier mod, Type::Tag ty, Opnd* dst,
    U_32 nArgs, Opnd** args_, Type* td) {
    TypeInst* inst = new (memManager) TypeInst(op, mod, ty, dst, nArgs, args_, td);
    inst->id       = numInsts++;
    return inst;
}
FieldAccessInst*
InstFactory::makeFieldAccessInst(Opcode op,
                Modifier mod,
                Type::Tag type,
                Opnd* dst,
                FieldDesc* fd) {
    FieldAccessInst* inst = new (memManager) FieldAccessInst(op, mod, type, dst, fd);
    inst->id       = numInsts++;
    return inst;
}
FieldAccessInst*
InstFactory::makeFieldAccessInst(Opcode op,
                Modifier mod,
                Type::Tag type,
                Opnd* dst,
                Opnd* src,
                FieldDesc* fd) {
    FieldAccessInst* inst = new (memManager) FieldAccessInst(op, mod, type, dst, src, fd);
    inst->id       = numInsts++;
    return inst;
}
FieldAccessInst*
InstFactory::makeFieldAccessInst(Opcode op,
                Modifier mod,
                Type::Tag type,
                Opnd* dst,
                Opnd* src1,
                Opnd* src2,
                FieldDesc* fd) {
    FieldAccessInst* inst = new (memManager) FieldAccessInst(op, mod, type, dst, src1, src2, fd);
    inst->id       = numInsts++;
    return inst;
}
FieldAccessInst*
InstFactory::makeFieldAccessInst(Opcode op,
                                 Modifier mod,
                                 Type::Tag type,
                                 Opnd* dst,
                                 U_32 numSrcs,
                                 Opnd** srcs,
                                 FieldDesc* fd) {
    srcs = copyOpnds(srcs, numSrcs);
    FieldAccessInst* inst = new (memManager) FieldAccessInst(op, mod, type, dst, numSrcs, srcs, fd);
    inst->id       = numInsts++;
    return inst;
}
MethodInst*
InstFactory::makeMethodInst(Opcode op, Modifier mod, Type::Tag type, Opnd* dst, MethodDesc* md) {
    MethodInst* inst = new (memManager) MethodInst(op, mod, type, dst, md);
    inst->id       = numInsts++;
    return inst;
}
MethodInst*
InstFactory::makeMethodInst(Opcode op, Modifier mod, Type::Tag type, Opnd* dst, Opnd* base, MethodDesc* md) {
    MethodInst* inst = new (memManager) MethodInst(op, mod, type, dst, base, md);
    inst->id       = numInsts++;
    return inst;
}
MethodInst*
InstFactory::makeMethodInst(Opcode op, Modifier mod, Type::Tag type, Opnd* dst, Opnd* base, 
                            Opnd* tauHasMethod, MethodDesc* md) {
    assert(tauHasMethod->getType()->tag == Type::Tau);
    MethodInst* inst = new (memManager) MethodInst(op, mod, type, dst, base, tauHasMethod, md);
    inst->id       = numInsts++;
    return inst;
}
MethodInst*
InstFactory::makeMethodInst(Opcode op,
                            Modifier mod,
                            Type::Tag type,
                            Opnd* dst,
                            U_32 nArgs,
                            MethodDesc* md) {
    MethodInst* inst = new (memManager) MethodInst(op, mod, type, dst, nArgs, 0, md);
    inst->id       = numInsts++;
    return inst;
}
MethodInst*
InstFactory::makeMethodInst(Opcode op,
                            Modifier mod,
                            Type::Tag type,
                            Opnd* dst,
                            U_32 numSrcs,
                            Opnd** srcs,
                            MethodDesc* md) {
    srcs = copyOpnds(srcs, numSrcs);
    MethodInst* inst = new (memManager) MethodInst(op, mod, type, dst, numSrcs, srcs, md);
    inst->id       = numInsts++;
    return inst;
}
MethodCallInst*
InstFactory::makeMethodCallInst(Opcode op, Modifier mod,
               Type::Tag type,
               Opnd* dst,
               U_32 numArgs,
               Opnd** args,
               MethodDesc* md) {
    MethodCallInst* inst = new (memManager) MethodCallInst(op, mod, type, dst, numArgs, args, md, memManager);
    inst->id       = numInsts++;
    return inst;
}

CallInst*
InstFactory::makeCallInst(Opcode op, Modifier mod,
                          Type::Tag type,
                          Opnd* dst,
                          Opnd* funptr,
                          U_32 numArgs,
                          Opnd** args) {
    CallInst* inst = new (memManager) CallInst(op, mod, type, dst, funptr, numArgs, args, memManager);
    inst->id       = numInsts++;
    return inst;
}

JitHelperCallInst* 
InstFactory::makeJitHelperCallInst(Opcode op,
                                    Modifier mod,
                                    Type::Tag type,
                                    Opnd* dst,
                                    U_32 nArgs,
                                    Opnd** args_,
                                    JitHelperCallId id) {
    JitHelperCallInst * inst = 
        new (memManager) JitHelperCallInst(op, mod, type, dst, nArgs, args_, id);
    inst->id       = numInsts++;
    return inst;
}

VMHelperCallInst* 
InstFactory::makeVMHelperCallInst(Opcode op,
                                    Modifier mod,
                                    Type::Tag type,
                                    Opnd* dst,
                                    U_32 nArgs,
                                    Opnd** args_,
                                    VM_RT_SUPPORT id) {
    VMHelperCallInst * inst = 
        new (memManager) VMHelperCallInst(op, mod, type, dst, nArgs, args_, id);
    inst->id       = numInsts++;
    return inst;
}

PhiInst*
InstFactory::makePhiInst(Type::Tag type, Opnd* dst, U_32 nArgs, Opnd** args_) {
#ifdef BRM_CHECK_TYPES
    for (U_32 i=0; i<nArgs; ++i) {
        assert(args_[i]->getType() == dst->getType());
    }
#endif
    PhiInst* inst = new (memManager) PhiInst(type, dst, nArgs, args_);
    inst->id       = numInsts++;
    return inst;
}

MultiSrcInst*
InstFactory::makeMultiSrcInst(Opcode opc,
                          Modifier mod,
                          Type::Tag ty,
                          Opnd* dst) {
    MultiSrcInst* inst = new (memManager) MultiSrcInst(opc, mod, ty, dst);
    inst->id       = numInsts++;
    return inst;
}
MultiSrcInst*
InstFactory::makeMultiSrcInst(Opcode opc,
                          Modifier mod,
                          Type::Tag ty,
                          Opnd* dst,
                          Opnd* src) {
    MultiSrcInst* inst = new (memManager) MultiSrcInst(opc, mod, ty, dst, src);
    inst->id       = numInsts++;
    return inst;
}
MultiSrcInst*
InstFactory::makeMultiSrcInst(Opcode opc,
                          Modifier mod,
                          Type::Tag ty,
                          Opnd* dst,
                          Opnd* src1,
                          Opnd* src2) {
    MultiSrcInst* inst = new (memManager) MultiSrcInst(opc, mod, ty, dst, src1, src2);
    inst->id       = numInsts++;
    return inst;
}
MultiSrcInst*
InstFactory::makeMultiSrcInst(Opcode opc,
                              Modifier mod,
                              Type::Tag ty,
                              Opnd* dst,
                              Opnd* src1,
                              Opnd* src2,
                              Opnd* src3) {
    
    Opnd* localSrcs[3] = {src1, src2, src3};
    Opnd** srcs = copyOpnds(localSrcs, 3);
    return makeMultiSrcInst(opc, mod, ty, dst, 3, srcs);
}
MultiSrcInst*
InstFactory::makeMultiSrcInst(Opcode opc,
                              Modifier mod,
                              Type::Tag ty,
                              Opnd* dst,
                              Opnd* src1,
                              Opnd* src2,
                              Opnd* src3,
                              Opnd* src4) {
    
    Opnd* localSrcs[4] = {src1, src2, src3, src4};
    Opnd** srcs = copyOpnds(localSrcs, 4);
    return makeMultiSrcInst(opc, mod, ty, dst, 4, srcs);
}
MultiSrcInst*
InstFactory::makeMultiSrcInst(Opcode opc,
                          Modifier mod,
                          Type::Tag ty,
                          Opnd* dst,
                          U_32 nSrcs,
                          Opnd** srcs) {
    MultiSrcInst* inst = new (memManager) MultiSrcInst(opc, mod, ty, dst, nSrcs, srcs);
    inst->id       = numInsts++;
    return inst;
}

//-----------------------------------------------------------------------------
// InstFactory factory methods for different opcodes
//-----------------------------------------------------------------------------

LabelInst*
InstFactory::makeMethodEntryLabel(MethodDesc* methodDesc) {
    return makeMethodEntryInst(createLabelNumber(), methodDesc);
}

Inst*
InstFactory::makeMethodMarker(MethodMarkerInst::Kind kind, MethodDesc* methodDesc,
        Opnd *obj, Opnd *retOpnd) {
    assert(obj && !obj->isNull());
    return makeMethodMarkerInst(kind, methodDesc, obj, 
            retOpnd != NULL ? retOpnd : OpndManager::getNullOpnd());
}

Inst*
InstFactory::makeMethodMarker(MethodMarkerInst::Kind kind, MethodDesc* methodDesc,
        Opnd *retOpnd) {
    return makeMethodMarkerInst(kind, methodDesc, 
            retOpnd != NULL ? retOpnd : OpndManager::getNullOpnd());
}

Inst*
InstFactory::makeMethodMarker(MethodMarkerInst::Kind kind, MethodDesc* methodDesc) {
    return makeMethodMarkerInst(kind, methodDesc);
}

// Numeric compute
Inst*
InstFactory::makeAdd(Modifier mod, Opnd* dst, Opnd* src1, Opnd* src2) {
    return makeInst(Op_Add, mod, dst->getType()->tag, dst, src1, src2);
}

Inst*
InstFactory::makeSub(Modifier mod, Opnd* dst, Opnd* src1, Opnd* src2) {
    return makeInst(Op_Sub, mod, dst->getType()->tag, dst, src1, src2);
}

Inst*
InstFactory::makeMul(Modifier mod, Opnd* dst, Opnd* src1, Opnd* src2) {
    return makeInst(Op_Mul, mod, dst->getType()->tag, dst, src1, src2);
}

Inst*
InstFactory::makeTauDiv(Modifier mod, Opnd* dst, Opnd* src1, Opnd* src2, Opnd *tauOpndsChecked) {
    assert(tauOpndsChecked->getType()->tag == Type::Tau);
    return makeMultiSrcInst(Op_TauDiv, mod, dst->getType()->tag, dst, src1, src2, tauOpndsChecked);
}

Inst*
InstFactory::makeTauRem(Modifier mod, Opnd* dst, Opnd* src1, Opnd* src2, Opnd *tauOpndsChecked) {
    assert(tauOpndsChecked->getType()->tag == Type::Tau);
    return makeMultiSrcInst(Op_TauRem, mod, dst->getType()->tag, dst, src1, src2, tauOpndsChecked);
}

Inst*
InstFactory::makeNeg(Opnd* dst, Opnd* src) {
    return makeInst(Op_Neg, Modifier(), dst->getType()->tag, dst, src);
}

Inst*
InstFactory::makeMulHi(Modifier mod, Opnd* dst, Opnd* src1, Opnd* src2) {
    return makeInst(Op_MulHi, mod, dst->getType()->tag, dst, src1, src2);
}

Inst*
InstFactory::makeMin(Opnd* dst, Opnd* src1, Opnd* src2) {
    return makeInst(Op_Min, Modifier(), dst->getType()->tag, dst, src1, src2);
}

Inst*
InstFactory::makeMax(Opnd* dst, Opnd* src1, Opnd* src2) {
    return makeInst(Op_Max, Modifier(), dst->getType()->tag, dst, src1, src2);
}

Inst*
InstFactory::makeAbs(Opnd* dst, Opnd* src) {
    return makeInst(Op_Abs, Modifier(), dst->getType()->tag, dst, src);
}

Inst* InstFactory::makeTauCheckFinite(Opnd* dst, Opnd* src) {
    return makeInst(Op_TauCheckFinite, Modifier(Exception_Sometimes), 
                    dst->getType()->tag, dst, src);
}

// Bitwise
Inst* InstFactory::makeAnd(Opnd* dst, Opnd* src1, Opnd* src2) {
    return makeInst(Op_And, Modifier(), dst->getType()->tag, dst, src1, src2);
}

Inst* InstFactory::makeOr(Opnd* dst, Opnd* src1, Opnd* src2) {
    return makeInst(Op_Or, Modifier(), dst->getType()->tag, dst, src1, src2);
}

Inst* InstFactory::makeXor(Opnd* dst, Opnd* src1, Opnd* src2) {
    return makeInst(Op_Xor, Modifier(), dst->getType()->tag, dst, src1, src2);
}

Inst* InstFactory::makeNot(Opnd* dst, Opnd* src) {
    return makeInst(Op_Not, Modifier(), dst->getType()->tag, dst, src);
}

// selection
Inst*
InstFactory::makeSelect(Opnd* dst, Opnd* src1, Opnd* src2, Opnd *src3) {
    Opnd* localSrcs[3] = {src1, src2, src3};
    Opnd** srcs = copyOpnds(localSrcs, 3);
    return makeMultiSrcInst(Op_Select, Modifier(), dst->getType()->tag, dst,
                            3, srcs);
}

// conversion
Inst* InstFactory::makeConv(Modifier mod, Type::Tag toType, Opnd* dst, Opnd* src) {
    //use convUnmanaged to convert between managed and unmanaged types
    assert (!(dst->getType()->isUnmanagedPtr() && src->getType()->isObject()) 
        && !(dst->getType()->isObject() && src->getType()->isUnmanagedPtr())); 

    return makeInst(Op_Conv, mod, toType, dst, src);
}

Inst* InstFactory::makeConvZE(Modifier mod, Type::Tag toType, Opnd* dst, Opnd* src) {
    assert((dst->getType()->isInteger() || dst->getType()->isUnmanagedPtr()) && src->getType()->isInteger());
    return makeInst(Op_ConvZE, mod, toType, dst, src);
}

Inst* InstFactory::makeConvUnmanaged(Modifier mod, Type::Tag toType, Opnd* dst, Opnd* src) {
    assert ((dst->getType()->isUnmanagedPtr() && (src->getType()->isObject() || src->getType()->isManagedPtr()))
        || (((dst->getType()->isObject() || dst->getType()->isManagedPtr())) && src->getType()->isUnmanagedPtr())); 
    return makeInst(Op_ConvUnmanaged, mod, toType, dst, src);
}

// shifts
Inst* InstFactory::makeShladd(Opnd* dst, Opnd* value, Opnd* shiftAmount, Opnd* addTo) {
    Opnd* localSrcs[3] = {value, shiftAmount, addTo};
    Opnd** srcs = copyOpnds(localSrcs, 3);
    return makeMultiSrcInst(Op_Shladd, Modifier(), dst->getType()->tag, dst,
                            3, srcs);
}

Inst* InstFactory::makeShl(Modifier mod, Opnd* dst, Opnd* value, Opnd* shiftAmount) {
    return makeInst(Op_Shl, mod, dst->getType()->tag, dst, value, shiftAmount);
}

Inst* InstFactory::makeShr(Modifier mod, Opnd* dst, Opnd* value, Opnd* shiftAmount) {
    return makeInst(Op_Shr, mod, dst->getType()->tag, dst, 
                    value, shiftAmount);
}

// comparison
Inst* InstFactory::makeCmp(ComparisonModifier mod, Type::Tag type, Opnd* dst, Opnd* src1, Opnd* src2) {
    return makeInst(Op_Cmp, mod, type, dst, src1, src2);
}

Inst* InstFactory::makeCmp3(ComparisonModifier mod, Type::Tag type, Opnd* dst, Opnd* src1, Opnd* src2) {
    return makeInst(Op_Cmp3, mod, type, dst, src1, src2);
}

Inst*
InstFactory::makeCatchLabel(U_32 labelId,
                            U_32 exceptionOrder,
                            Type*  handlerExceptionType) {
    return makeCatchLabelInst(labelId, exceptionOrder, handlerExceptionType);
}

CatchLabelInst*
InstFactory::makeCatchLabel(U_32 exceptionOrder,
                            Type*  handlerExceptionType) {
    return makeCatchLabelInst(createLabelNumber(), exceptionOrder, handlerExceptionType);
}

LabelInst*
InstFactory::makeLabel() {
    return makeLabelInst(createLabelNumber());
}

Inst*
InstFactory::makeBranch(ComparisonModifier mod, Type::Tag type, Opnd* src1, Opnd* src2, LabelInst* labelInst) {
    return makeBranchInst(Op_Branch, mod, type, src1, src2, labelInst);
;
}

Inst*
InstFactory::makeBranch(ComparisonModifier mod, Type::Tag type, Opnd* src, LabelInst* labelInst) {
    return makeBranchInst(Op_Branch, mod, type, src, labelInst);
}

Inst*
InstFactory::makeJump(LabelInst* labelInst) {
    return makeBranchInst(Op_Jump, labelInst);
}

Inst* InstFactory::makeJSR(LabelInst* labelInst) {
    return makeBranchInst(Op_JSR, labelInst);
}

Inst* InstFactory::makeSwitch(Opnd* src, U_32 nLabels, LabelInst** labelInsts, LabelInst* defaultLabel) {
    LabelInst** newLabelInsts = new (memManager) LabelInst*[nLabels];
    for (U_32 i=0; i<nLabels; i++)
        newLabelInsts[i] = labelInsts[i];
    return makeSwitchInst(src, newLabelInsts, nLabels, defaultLabel);
}

Inst* InstFactory::makeReturn(Opnd* src) {
    return makeInst(Op_Return, Modifier(), src->getType()->tag, OpndManager::getNullOpnd(), src);
}

// void return type
Inst* InstFactory::makeReturn() {
    return makeInst(Op_Return, Modifier(), Type::Void, OpndManager::getNullOpnd());
}

Inst* InstFactory::makeRet(Opnd* src) {
    return makeInst(Op_Ret, Modifier(), src->getType()->tag, OpndManager::getNullOpnd(), src);
}


Inst* InstFactory::makeThrow(ThrowModifier mod, Opnd* exceptionObj) {
    return makeInst(Op_Throw, Modifier(mod), Type::Void, OpndManager::getNullOpnd(), exceptionObj);
}

Inst* InstFactory::makePseudoThrow() {
    return makeInst(Op_PseudoThrow, Modifier(Exception_Sometimes), Type::Void, OpndManager::getNullOpnd());
}

Inst* InstFactory::makeThrowSystemException(CompilationInterface::SystemExceptionId exceptionId) {
    MethodDesc* enclosingMethod = 0;
    return makeTokenInst(Op_ThrowSystemException, Modifier(), Type::Void, 
                         OpndManager::getNullOpnd(), (U_32) exceptionId, enclosingMethod);
}

Inst* InstFactory::makeThrowLinkingException(Class_Handle encClass, U_32 CPIndex, U_32 operation) {
    return makeLinkingExcInst(Op_ThrowLinkingException, Modifier(), Type::Void, 
                              OpndManager::getNullOpnd(), encClass, CPIndex, operation);
}


Inst* InstFactory::makePrefetch(Opnd* addr) {
    return makeInst(Op_Prefetch, Modifier(), Type::Void, OpndManager::getNullOpnd(), addr);
}

Inst* InstFactory::makeIdentHC(Opnd* dst, Opnd* src) {
    return makeInst(Op_IdentHC, Modifier(), dst->getType()->tag, dst, src);
}

Inst*
InstFactory::makeDirectCall(Opnd* dst,
                            Opnd* tauNullChecked,
                            Opnd* tauTypesChecked,
                            U_32 numArgs,
                            Opnd** args,
                            MethodDesc* methodDesc) {
    assert(tauNullChecked->getType()->tag == Type::Tau);
    assert(tauTypesChecked->getType()->tag == Type::Tau);
    Type::Tag returnType = dst->isNull()? Type::Void : dst->getType()->tag;
    args = copyOpnds(tauNullChecked, tauTypesChecked, args, numArgs);
    return makeMethodCallInst(Op_DirectCall, Modifier(Exception_Sometimes), 
                              returnType, dst, numArgs+2, args, methodDesc);
}

Inst*
InstFactory::makeTauVirtualCall(Opnd* dst,
                                Opnd *tauNullChecked,
                                Opnd *tauTypesChecked,
                                U_32 numArgs,
                                Opnd** args,
                                MethodDesc* methodDesc) {
    assert(tauNullChecked->getType()->tag == Type::Tau);
    assert(tauTypesChecked->getType()->tag == Type::Tau);
    Type::Tag returnType = dst->isNull()? Type::Void : dst->getType()->tag;
    args = copyOpnds(tauNullChecked, tauTypesChecked, args, numArgs);
    return makeMethodCallInst(Op_TauVirtualCall, Modifier(Exception_Sometimes), 
                              returnType, dst, numArgs+2, args, methodDesc);
}

Inst*
InstFactory::makeIndirectCall(Opnd* dst,
                              Opnd* funAddr,
                              Opnd* tauNullCheckedFirstArg,
                              Opnd* tauTypesChecked,
                              U_32 numArgs,
                              Opnd** args) {
    assert(tauNullCheckedFirstArg->getType()->tag == Type::Tau);
    assert(tauTypesChecked->getType()->tag == Type::Tau);
    Type::Tag returnType = dst->isNull()? Type::Void : dst->getType()->tag;
    Opnd** newArgs = copyOpnds(tauNullCheckedFirstArg, tauTypesChecked, 
                               args, numArgs);
    Inst* inst = makeCallInst(Op_IndirectCall, Modifier(Exception_Sometimes), 
                              returnType, dst, funAddr, numArgs+2, newArgs);
    inst->id = numInsts++;
    return inst;
}

Inst*
InstFactory::makeIndirectMemoryCall(Opnd* dst,
                                    Opnd* funAddr,
                                    Opnd* tauNullCheckedFirstArg,
                                    Opnd* tauTypesChecked,
                                    U_32 numArgs,
                                    Opnd** args) {
    assert(tauNullCheckedFirstArg->getType()->tag == Type::Tau);
    assert(tauTypesChecked->getType()->tag == Type::Tau);
    Type::Tag returnType = dst->isNull()? Type::Void : dst->getType()->tag;
    Opnd** newArgs = copyOpnds(tauNullCheckedFirstArg, tauTypesChecked, 
                               args, numArgs);
    Inst* inst = makeCallInst(Op_IndirectMemoryCall, Modifier(Exception_Sometimes), 
                              returnType, dst, funAddr, numArgs+2, newArgs);
    inst->id = numInsts++;
    return inst;
}

Inst*
InstFactory::makeJitHelperCall(Opnd* dst, JitHelperCallId id,
                               Opnd* tauNullChecked, Opnd* tauTypesChecked,
                               U_32 numArgs, Opnd** args)
{
    Type::Tag returnType = dst->isNull()? Type::Void : dst->getType()->tag;
    if (id == ArrayCopyDirect || id == ArrayCopyReverse) // these three need taus
    {
        args = copyOpnds(tauNullChecked, tauTypesChecked, args, numArgs);
        numArgs = numArgs+2;
    } else {
        args = copyOpnds(args, numArgs);
    }
    Modifier mod;
    switch(id) {
        case StringCompareTo:
        case StringIndexOf:
        case StringRegionMatches:
            mod = Modifier(Exception_Never);
            break;
        default:
            mod = Modifier(Exception_Sometimes);
    }
    return makeJitHelperCallInst(Op_JitHelperCall, mod, returnType, dst, numArgs, args, id);
}

Inst*
InstFactory::makeVMHelperCall(Opnd* dst, VM_RT_SUPPORT id, U_32 numArgs, Opnd** args) {
    Type::Tag returnType = dst->isNull()? Type::Void : dst->getType()->tag;
    args = copyOpnds(args, numArgs);
    return makeVMHelperCallInst(Op_VMHelperCall, Modifier(Exception_Sometimes), returnType, dst, numArgs, args, id);
}


// load, store, & move
Inst*
InstFactory::makePhi(Opnd* dst, U_32 numOpnds, Opnd** opnds) {
#ifdef BRM_CHECK_TYPES
    for (U_32 i=0; i<numOpnds; ++i) {
        assert(opnds[i]->getType() == dst->getType());
    }
#endif
    Opnd** newOpnds = copyOpnds(opnds, numOpnds);
    return makePhiInst(dst->getType()->tag, dst, numOpnds, newOpnds);
}

Inst*
InstFactory::makeTauPi(Opnd* dst, Opnd* src, Opnd *tauDep, PiCondition *cond) {
#ifdef BRM_CHECK_TYPES
    assert(src->getType() == dst->getType());
#endif
    assert(tauDep->getType()->tag == Type::Tau);
    Type::Tag atype = src->getType()->tag;
    TauPiInst* inst = new (memManager) TauPiInst(atype, dst, src, tauDep, cond);
    inst->id       = numInsts++;
    return inst;
}

Inst*
InstFactory::makeCopy(Opnd* dst, Opnd* src) {
    return makeInst(Op_Copy, Modifier(), dst->getType()->tag, dst, src);
}

Inst*
InstFactory::makeDefArg(Modifier mod, Opnd* arg) {
    return makeInst(Op_DefArg, mod, arg->getType()->tag, arg);
}

Inst* InstFactory::makeCatch(Opnd* dst) {
    return makeInst(Op_Catch, Modifier(), dst->getType()->tag, dst);
}

Inst* InstFactory::makeSaveRet(Opnd* dst) {
    return makeInst(Op_SaveRet, Modifier(), dst->getType()->tag, dst);
}

Inst* InstFactory::makeLdStatic(Modifier mod, Type* type, Opnd* dst, FieldDesc* fieldDesc) {
    return makeFieldAccessInst(Op_LdStatic, mod, type->tag, dst, fieldDesc);
}

Inst* InstFactory::makeTauLdField(Modifier mod, Type* type, Opnd* dst, Opnd* base, 
                                  Opnd* tauNonNullBase, Opnd *tauObjectTypeChecked,
                                  FieldDesc* fieldDesc) {
    assert(tauNonNullBase->getType()->tag == Type::Tau);
    assert(tauObjectTypeChecked->getType()->tag == Type::Tau);
    Opnd* localSrcs[3] = {base, tauNonNullBase, tauObjectTypeChecked};
    Opnd** srcs = copyOpnds(localSrcs, 3);

    return makeFieldAccessInst(Op_TauLdField, mod, type->tag, dst, 3,
                               srcs, fieldDesc);
}

Inst* InstFactory::makeTauLdInd(Modifier mod, Type::Tag type, Opnd* dst, Opnd* ptr,
                                Opnd *tauBaseNonNull, Opnd *tauAddressInRange) {
    assert(tauBaseNonNull->getType()->tag == Type::Tau);
    Modifier m1 = mod;
    if (mod.getSpeculativeModifier() == 0) {
        m1 = mod | Speculative_No;
    }
    return makeMultiSrcInst(Op_TauLdInd, m1, type, dst, ptr, tauBaseNonNull, tauAddressInRange);
}

Inst* InstFactory::makeTauLdElem(Modifier mod, Type* type, Opnd* dst, Opnd* array, Opnd* index,
                                 Opnd *tauBaseNonNull, Opnd *tauAddressInRange) {
    assert(tauBaseNonNull->getType()->tag == Type::Tau);
    assert(tauAddressInRange->getType()->tag == Type::Tau);
    return makeTypeInst(Op_TauLdElem, mod, dst->getType()->tag, dst, array, 
                        index,
                        tauBaseNonNull, tauAddressInRange, type);
}

#ifdef BRM_CHECK_TYPES
static void doBRMCheck(Opnd* src, Opnd* dst) {
    Type *srcType = src->getType();
    Type *dstType = dst->getType();
    assert((srcType == dstType) || (srcType->isNullObject() && dstType->isObject()) 
        || (srcType->isObject() && dstType->isObject() 
            && (srcType->isUnresolvedType() || dstType->isUnresolvedType()
                || srcType->asObjectType()->isSubClassOf(dstType->asObjectType()))));
}
#endif

Inst* InstFactory::makeLdVar(Opnd* dst, VarOpnd* var) {
#ifdef BRM_CHECK_TYPES
    doBRMCheck(var, dst);
#endif
    return makeVarAccessInst(Op_LdVar, dst->getType()->tag, dst, var);
}

Inst* InstFactory::makeLdVar(Opnd* dst, SsaVarOpnd* var) {
#ifdef BRM_CHECK_TYPES
    doBRMCheck(var, dst);
#endif
    return makeVarAccessInst(Op_LdVar, dst->getType()->tag, dst, var);
}

Inst* InstFactory::makeLdStaticAddr(Opnd* dst, FieldDesc* fieldDesc) {
    return makeFieldAccessInst(Op_LdStaticAddr, Modifier(), dst->getType()->tag, dst, fieldDesc);
}

Inst* InstFactory::makeLdFieldAddr(Opnd* dst, Opnd* base, FieldDesc* fieldDesc) {
    return makeFieldAccessInst(Op_LdFieldAddr, Modifier(), dst->getType()->tag, dst, base, fieldDesc);
}

Inst* InstFactory::makeLdElemAddr(Type* type, Opnd* dst, Opnd* array, Opnd* index) {
    return makeTypeInst(Op_LdElemAddr, Modifier(), dst->getType()->tag, dst, array, index, type);
}

Inst* InstFactory::makeLdVarAddr(Opnd* dst, VarOpnd* var) {
    return makeVarAccessInst(Op_LdVarAddr, dst->getType()->tag, dst, var);
}

Inst* InstFactory::makeLdFunAddr(Opnd* dst, MethodDesc* methodDesc) {
    return makeMethodInst(Op_LdFunAddr, Modifier(), dst->getType()->tag, dst, methodDesc);
}

Inst* InstFactory::makeTauLdVirtFunAddr(Opnd* dst, Opnd* vtable,
                                        Opnd* tauVtableHasMethod, 
                                        MethodDesc* methodDesc) {
    assert(tauVtableHasMethod->getType()->tag == Type::Tau);
    return makeMethodInst(Op_TauLdVirtFunAddr, Modifier(), dst->getType()->tag, dst, 
                          vtable, tauVtableHasMethod, methodDesc);
}

Inst* InstFactory::makeLdFunAddrSlot(Opnd* dst, MethodDesc* methodDesc) {
    return makeMethodInst(Op_LdFunAddrSlot, Modifier(), dst->getType()->tag, dst, methodDesc);
}

Inst* InstFactory::makeTauLdVirtFunAddrSlot(Opnd* dst, Opnd* vtable, Opnd *tauVtableHasMethod,
                                         MethodDesc* methodDesc) {
    assert(tauVtableHasMethod->getType()->tag == Type::Tau);
    return makeMethodInst(Op_TauLdVirtFunAddrSlot, Modifier(), dst->getType()->tag, dst, vtable, 
                          tauVtableHasMethod, methodDesc);
}

Inst* InstFactory::makeTauLdIntfcVTableAddr(Opnd* dst, Opnd* base, 
                                            Type* vtableType) {
    return makeTypeInst(Op_TauLdIntfcVTableAddr, Modifier(), dst->getType()->tag, dst, base, 
        vtableType);
}

Inst* InstFactory::makeTauLdVTableAddr(Opnd* dst, Opnd* base, 
                                       Opnd *tauBaseNonNull) {
    assert(tauBaseNonNull);
    return makeInst(Op_TauLdVTableAddr, Modifier(), dst->getType()->tag, dst, base, 
                    tauBaseNonNull);
}

Inst* InstFactory::makeGetVTableAddr(Opnd* dst, ObjectType *type) {
    return makeTypeInst(Op_GetVTableAddr, Modifier(), dst->getType()->tag, dst, type);
}

Inst* InstFactory::makeGetClassObj(Opnd* dst, ObjectType *type) {
    //makeTypeInst(Op_TypeMonitorEnter, Modifier(), Type::Void, OpndManager::getNullOpnd(), type);
    return makeTypeInst(Op_GetClassObj, Modifier(), dst->getType()->tag, dst, type);
}

Inst* InstFactory::makeTauArrayLen(Opnd* dst, Type::Tag type, Opnd* base,
                                   Opnd *tauBaseIsNonNull,
                                   Opnd *tauBaseIsArray) {
    assert(tauBaseIsNonNull->getType()->tag == Type::Tau);
    assert(tauBaseIsArray->getType()->tag == Type::Tau);
    return makeMultiSrcInst(Op_TauArrayLen, Modifier(), type, dst, base, tauBaseIsNonNull,
                            tauBaseIsArray);
}

Inst* InstFactory::makeLdArrayBaseAddr(Type* type, Opnd* dst, Opnd* array) {
    return makeTypeInst(Op_LdArrayBaseAddr, Modifier(), dst->getType()->tag, dst, array, type);
}

//
Inst* InstFactory::makeAddScaledIndex(Opnd* dst, Opnd* ptr, Opnd* index) {
    return makeInst(Op_AddScaledIndex, Modifier(), dst->getType()->tag, 
                    dst, ptr, index);
}

//
Inst* InstFactory::makeUncompressRef(Opnd* dst, Opnd* compref)
{
    return makeInst(Op_UncompressRef, Modifier(), dst->getType()->tag, 
                    dst, compref);
}

Inst* InstFactory::makeCompressRef(Opnd* dst, Opnd* uncompref)
{
    return makeInst(Op_CompressRef, Modifier(), dst->getType()->tag, 
                    dst, uncompref);
}

Inst* InstFactory::makeLdFieldOffset(Opnd* dst, FieldDesc *fieldDesc)
{
    return makeFieldAccessInst(Op_LdFieldOffset, Modifier(), 
                               Type::Offset, dst, fieldDesc);
}

Inst* InstFactory::makeLdFieldOffsetPlusHeapbase(Opnd* dst, FieldDesc *fieldDesc)
{
    return makeFieldAccessInst(Op_LdFieldOffsetPlusHeapbase, Modifier(), 
                               Type::OffsetPlusHeapbase, dst, fieldDesc);
}

Inst* InstFactory::makeLdArrayBaseOffset(Opnd* dst, Type *elemType)
{
    return makeTypeInst(Op_LdArrayBaseOffset, Modifier(), Type::Offset, dst,
                        elemType);
}

Inst* InstFactory::makeLdArrayBaseOffsetPlusHeapbase(Opnd* dst, Type *elemType)
{
    return makeTypeInst(Op_LdArrayBaseOffsetPlusHeapbase, Modifier(), Type::Offset, dst,
                        elemType);
}

Inst* InstFactory::makeLdArrayLenOffset(Opnd* dst, Type *elemType)
{
    return makeTypeInst(Op_LdArrayLenOffset, Modifier(), Type::Offset, dst,
                        elemType);
}

Inst* InstFactory::makeLdArrayLenOffsetPlusHeapbase(Opnd* dst, Type *elemType)
{
    return makeTypeInst(Op_LdArrayLenOffsetPlusHeapbase, Modifier(), Type::Offset, dst,
                        elemType);
}

Inst* InstFactory::makeAddOffset(Opnd* dst, Opnd* ref, Opnd* offset)
{
    return makeInst(Op_AddOffset, Modifier(), dst->getType()->tag, 
                    dst, ref, offset);
}

Inst* InstFactory::makeAddOffsetPlusHeapbase(Opnd* dst, Opnd* ref, Opnd* offset)
{
    return makeInst(Op_AddOffsetPlusHeapbase, Modifier(), dst->getType()->tag, 
                    dst, ref, offset);
}

Inst* InstFactory::makeTauStStatic(Modifier mod, Type::Tag type, Opnd* src, 
                                   Opnd *tauFieldTypeChecked, FieldDesc* fieldDesc) {
    assert(tauFieldTypeChecked->getType()->tag == Type::Tau);
    return makeFieldAccessInst(Op_TauStStatic, mod,
                               type, OpndManager::getNullOpnd(), src, 
                               tauFieldTypeChecked, fieldDesc);
}

Inst* InstFactory::makeTauStField(Modifier mod, Type::Tag type, Opnd* src, Opnd* base,
                                  Opnd *tauBaseNonNull, Opnd *tauAddressInRange,
                                  Opnd *tauFieldTypeChecked, 
                                  FieldDesc* fieldDesc) {
    assert(tauBaseNonNull->getType()->tag == Type::Tau);
    assert(tauAddressInRange->getType()->tag == Type::Tau);
    assert(tauFieldTypeChecked->getType()->tag == Type::Tau);
    Opnd* localSrcs[4] = {src, base, tauBaseNonNull, tauAddressInRange};
    Opnd** srcs = copyOpnds(localSrcs, 4);
    return makeFieldAccessInst(Op_TauStField, mod, type, OpndManager::getNullOpnd(), 4, srcs,
                               fieldDesc);
}

Inst* InstFactory::makeTauStInd(Modifier mod, Type::Tag type, Opnd* src, Opnd* ptr, 
                                Opnd *tauBaseNonNull, Opnd *tauAddressInRange, Opnd *tauElemTypeChecked) {
    assert(tauBaseNonNull->getType()->tag == Type::Tau);
    assert(tauAddressInRange->getType()->tag == Type::Tau);
    assert(tauElemTypeChecked->getType()->tag == Type::Tau);
    Opnd* localSrcs[5] = {src, ptr, tauBaseNonNull, tauAddressInRange, tauElemTypeChecked};
    Opnd** srcs = copyOpnds(localSrcs, 5);
    return makeMultiSrcInst(Op_TauStInd, mod, type, OpndManager::getNullOpnd(), 5, srcs);
}

Inst* InstFactory::makeTauStRef(Modifier mod, Type::Tag type, Opnd *src, Opnd* pointer, 
                                Opnd* objbase, 
                                Opnd *tauBaseNonNull, Opnd *tauAddressInRange,
                                Opnd* tauElemTypeChecked) {
    assert(tauBaseNonNull->getType()->tag == Type::Tau);
    assert(tauAddressInRange->getType()->tag == Type::Tau);
    assert(tauElemTypeChecked->getType()->tag == Type::Tau);
    Opnd* localSrcs[6] = {src, pointer, objbase, 
                          tauBaseNonNull, tauAddressInRange, tauElemTypeChecked};
    Opnd** srcs = copyOpnds(localSrcs, 6);
    return makeMultiSrcInst(Op_TauStRef, mod, type, OpndManager::getNullOpnd(), 6, srcs);
}

Inst* InstFactory::makeTauStElem(Modifier mod, Type::Tag type, Opnd* src, 
                                 Opnd* array, Opnd* index, 
                                 Opnd *tauBaseNonNull, Opnd *tauAddressInRange,
                                 Opnd *tauElemTypeChecked) {
    assert(tauBaseNonNull->getType()->tag == Type::Tau);
    assert(tauAddressInRange->getType()->tag == Type::Tau);
    assert(tauElemTypeChecked->getType()->tag == Type::Tau);
    Opnd* localSrcs[6] = {src, array, index, tauBaseNonNull, tauAddressInRange, tauElemTypeChecked};
    Opnd** srcs = copyOpnds(localSrcs, 6);
    return makeMultiSrcInst(Op_TauStElem, mod, type, OpndManager::getNullOpnd(), 6, srcs);
}

Inst* InstFactory::makeStVar(VarOpnd* var, Opnd* src) {
#ifdef BRM_CHECK_TYPES
    doBRMCheck(src, var);
#endif
    return makeVarAccessInst(Op_StVar, src->getType()->tag, var, src);
}

Inst* InstFactory::makeStVar(SsaVarOpnd* var, Opnd* src) {
#ifdef BRM_CHECK_TYPES
    doBRMCheck(src, var);
#endif
    return makeVarAccessInst(Op_StVar, src->getType()->tag, var, src);
}

Inst* InstFactory::makeNewObj(Opnd* dst, Type* type) {
    return makeTypeInst(Op_NewObj, Modifier(Exception_Sometimes), 
                        type->tag, dst, type);
}

Inst* InstFactory::makeNewArray(Opnd* dst, Opnd* numElems, Type* elemType) {
    return makeTypeInst(Op_NewArray, Modifier(Exception_Sometimes), 
                        dst->getType()->tag, dst, numElems, elemType);
}

Inst*
InstFactory::makeNewMultiArray(Opnd* dst,
                               U_32 dimensions,
                               Opnd** numElems,
                               Type* elemType) {
    Opnd** newNumElems = copyOpnds(numElems, dimensions);
    return makeTypeInst(Op_NewMultiArray, Modifier(Exception_Sometimes), 
                        dst->getType()->tag, dst, dimensions, newNumElems, elemType);
}

Inst* InstFactory::makeTauMonitorEnter(Opnd* src, Opnd *tauNonNull) {
    assert(tauNonNull->getType()->tag == Type::Tau);
    return makeInst(Op_TauMonitorEnter, Modifier(), Type::Void, OpndManager::getNullOpnd(), src,
                    tauNonNull);
}

Inst* InstFactory::makeTauMonitorExit(Opnd* src, Opnd *tauNonNull) {
    assert(tauNonNull->getType()->tag == Type::Tau);
    return makeInst(Op_TauMonitorExit, Modifier(Exception_Sometimes), 
                    Type::Void, OpndManager::getNullOpnd(), src, tauNonNull);
}

Inst* InstFactory::makeTypeMonitorEnter(Type *type) {
    return makeTypeInst(Op_TypeMonitorEnter, Modifier(), Type::Void, OpndManager::getNullOpnd(), type);
}

Inst* InstFactory::makeTypeMonitorExit(Type *type) {
    return makeTypeInst(Op_TypeMonitorExit, Modifier(Exception_Sometimes), 
                        Type::Void, OpndManager::getNullOpnd(), type);
}

Inst* InstFactory::makeLdLockAddr(Opnd* dst, Opnd* obj) {
    return makeInst(Op_LdLockAddr, Modifier(), dst->getType()->tag, dst, obj);
}

Inst* InstFactory::makeIncRecCount(Opnd* obj, Opnd* oldLock) {
    return makeInst(Op_IncRecCount, Modifier(), Type::Void, OpndManager::getNullOpnd(), obj, oldLock);
}

Inst* InstFactory::makeTauBalancedMonitorEnter(Opnd *dst, Opnd* obj, Opnd *lockAddr,
                                               Opnd *tauNonNull) {
    assert(tauNonNull->getType()->tag == Type::Tau);
    Opnd* localSrcs[3] = {obj, lockAddr, tauNonNull};
    Opnd** srcs = copyOpnds(localSrcs, 3);
    return makeMultiSrcInst(Op_TauBalancedMonitorEnter, Modifier(), dst->getType()->tag, dst, 3, srcs);
}

Inst* InstFactory::makeBalancedMonitorExit(Opnd* obj, Opnd *lockAddr, Opnd *oldValue) {
    Opnd* localSrcs[3] = {obj, lockAddr, oldValue};
    Opnd** srcs = copyOpnds(localSrcs, 3);
    return makeMultiSrcInst(Op_BalancedMonitorExit, 
                            Modifier(), Type::Void, 
                            OpndManager::getNullOpnd(), 3, srcs);
}

Inst* InstFactory::makeTauOptimisticBalancedMonitorEnter(Opnd *dst, Opnd* obj, 
                                                         Opnd *lockAddr, Opnd *tauNonNull) {
    assert(tauNonNull->getType()->tag == Type::Tau);
    Opnd* localSrcs[3] = {obj, lockAddr, tauNonNull};
    Opnd** srcs = copyOpnds(localSrcs, 3);
    return makeMultiSrcInst(Op_TauOptimisticBalancedMonitorEnter, 
                            Modifier(), dst->getType()->tag, 
                            dst, 3, srcs);
}

Inst* InstFactory::makeOptimisticBalancedMonitorExit(Opnd* obj, Opnd *lockAddr,
                                                     Opnd *oldValue) {
    Opnd* localSrcs[3] = {obj, lockAddr, oldValue};
    Opnd** srcs = copyOpnds(localSrcs, 3);
    return makeMultiSrcInst(Op_OptimisticBalancedMonitorExit, 
                            Modifier(Exception_Sometimes), Type::Void, 
                            OpndManager::getNullOpnd(), 3, srcs);
}


Inst* InstFactory::makeMonitorEnterFence(Opnd* src) {
    return makeInst(Op_MonitorEnterFence, Modifier(), Type::Void, OpndManager::getNullOpnd(), src);
}

Inst* InstFactory::makeMonitorExitFence(Opnd* src) {
    return makeInst(Op_MonitorExitFence, Modifier(), Type::Void, OpndManager::getNullOpnd(), src);
}

Inst* InstFactory::makeLdRef(Modifier mod, Opnd* dst, MethodDesc* enclosingMethod, U_32 token) {
    return makeTokenInst(Op_LdRef, mod, dst->getType()->tag, dst, token, enclosingMethod);
}

// type checking
Inst* InstFactory::makeTauStaticCast(Opnd* dst, Opnd* src, Opnd *tauCastChecked, Type* type) {
    assert(tauCastChecked->getType()->tag == Type::Tau);
    return makeTypeInst(Op_TauStaticCast, Modifier(), dst->getType()->tag, dst, src, 
                        tauCastChecked, type);
}

Inst* InstFactory::makeTauCast(Opnd* dst, Opnd* src, Opnd *tauCheckedNull, Type* type) {
    assert(tauCheckedNull->getType()->tag == Type::Tau);
    return makeTypeInst(Op_TauCast, 
                        (Modifier(Exception_Sometimes)),
                        dst->getType()->tag, dst, src, tauCheckedNull, type);
}

Inst* InstFactory::makeTauAsType(Opnd* dst, Opnd* src, Opnd *tauNullChecked, Type* type) {
    assert(tauNullChecked->getType()->tag == Type::Tau);
    return makeTypeInst(Op_TauAsType, Modifier(), dst->getType()->tag, dst, src, tauNullChecked, type);
}

Inst* InstFactory::makeTauInstanceOf(Opnd* dst, Opnd* src, Opnd* tauNullChecked, 
                                     Type* type) {
    assert(tauNullChecked->getType()->tag == Type::Tau);
    return makeTypeInst(Op_TauInstanceOf, Modifier(), dst->getType()->tag, dst, src,
                        tauNullChecked, type);
}

Inst* InstFactory::makeInitType(Type* type) {
    return makeTypeInst(Op_InitType, Modifier(Exception_Sometimes), 
                        Type::Void, OpndManager::getNullOpnd(), type);
}

// lowered instructions
Inst* InstFactory::makeTauCheckBounds(Opnd *dst, Opnd* arrayLen, Opnd* index) {
    return makeInst(Op_TauCheckBounds, Modifier(Exception_Sometimes)|Modifier(Overflow_Unsigned), 
                    Type::Tau, dst, arrayLen, index);
}

Inst* InstFactory::makeTauCheckLowerBound(Opnd *dst, Opnd* lb, Opnd *idx) {
    return makeInst(Op_TauCheckLowerBound, 
                    Modifier(Exception_Sometimes)|Modifier(Overflow_Unsigned), 
                    Type::Tau, dst, lb, idx);
}

Inst* InstFactory::makeTauCheckUpperBound(Opnd *dst, Opnd* idx, Opnd *ub) {
    return makeInst(Op_TauCheckUpperBound, 
                    Modifier(Exception_Sometimes)|Modifier(Overflow_Unsigned), 
                    Type::Tau, dst, idx, ub);
}

Inst* InstFactory::makeTauCheckNull(Opnd* dst, Opnd* base) {
    assert(dst->getType()->tag == Type::Tau);
    return makeInst(Op_TauCheckNull, Modifier(Exception_Sometimes)|Modifier(DefArgNoModifier), 
                    Type::Tau, dst, base);
}

Inst* InstFactory::makeTauCheckZero(Opnd* dst, Opnd* src) {
    assert(dst->getType()->tag == Type::Tau);
    return makeInst(Op_TauCheckZero, Modifier(Exception_Sometimes), 
                    Type::Tau, dst, src);
}

Inst* InstFactory::makeTauCheckDivOpnds(Opnd *dst, Opnd* src1, Opnd* src2) {
    return makeInst(Op_TauCheckDivOpnds, Modifier(Exception_Sometimes), Type::Tau,
                    dst, src1, src2);
}

Inst* InstFactory::makeTauCheckElemType(Opnd *dst, Opnd* array, Opnd* src,
                                        Opnd *tauNullChecked,
                                        Opnd *tauIsArray) {
    assert(tauNullChecked->getType()->tag == Type::Tau);
    assert(tauIsArray->getType()->tag == Type::Tau);
    return makeMultiSrcInst(Op_TauCheckElemType, Modifier(Exception_Sometimes), 
                            Type::Tau, dst, array, src, tauNullChecked,
                            tauIsArray);
}

Inst* InstFactory::makeLdConst(Opnd* dst, I_32 val) {
    return makeConstInst(dst, val);
}

Inst* InstFactory::makeLdConst(Opnd* dst, int64 val) {
    return makeConstInst(dst, val);
}

Inst* InstFactory::makeLdConst(Opnd* dst, float val) {
    return makeConstInst(dst, val);
}

Inst* InstFactory::makeLdConst(Opnd* dst, double val) {
    return makeConstInst(dst, val);
}

Inst* InstFactory::makeLdConst(Opnd* dst, ConstInst::ConstValue val) {
    return makeConstInst(dst, val);
}

Inst* InstFactory::makeLdNull(Opnd* dst) {
    return makeConstInst(dst);
}

Inst* InstFactory::makeIncCounter(U_32 val) {
    return makeTokenInst(Op_IncCounter, Modifier(), Type::Void, OpndManager::getNullOpnd(), val, NULL);
}

Inst* InstFactory::makeTauPoint(Opnd *dst) {
    assert(dst->getType()->tag == Type::Tau);
    return makeInst(Op_TauPoint, Modifier(), Type::Tau, dst);
}

Inst* InstFactory::makeTauEdge(Opnd *dst) {
    assert(dst->getType()->tag == Type::Tau);
    return makeInst(Op_TauEdge, Modifier(), Type::Tau, dst);
}

Inst* InstFactory::makeTauAnd(Opnd *dst, U_32 numOpnds, Opnd** opnds) {
    assert(dst->getType()->tag == Type::Tau);
    assert(numOpnds > 0);

    Opnd** srcs = copyOpnds(opnds, numOpnds);
    return makeMultiSrcInst(Op_TauAnd, Modifier(), Type::Tau, dst, numOpnds, srcs);
}

Inst* InstFactory::makeTauUnsafe(Opnd *dst) {
    assert(dst->getType()->tag == Type::Tau);
    return makeInst(Op_TauUnsafe, Modifier(), Type::Tau, dst);
}

Inst* InstFactory::makeTauSafe(Opnd *dst) {
    assert(dst->getType()->tag == Type::Tau);
    return makeInst(Op_TauSafe, Modifier(), Type::Tau, dst);
}

Inst* InstFactory::makeTauCheckCast(Opnd* dst, Opnd* src, Opnd *tauCheckedNull, Type* type) {
    assert(tauCheckedNull->getType()->tag == Type::Tau);
    assert(dst->getType()->tag == Type::Tau);
    return makeTypeInst(Op_TauCheckCast, 
                        (Modifier(Exception_Sometimes)),
                        Type::Tau, dst, src, tauCheckedNull, type);
}

Inst* InstFactory::makeTauHasType(Opnd* dst, Opnd* src, Type* type) {
    assert(dst->getType()->tag == Type::Tau);
    return makeTypeInst(Op_TauHasType,
                        Modifier(),
                        Type::Tau, dst, src, type);
}

Inst* InstFactory::makeTauHasExactType(Opnd* dst, Opnd* src, Type* type) {
    assert(dst->getType()->tag == Type::Tau);
    return makeTypeInst(Op_TauHasExactType,
                        Modifier(),
                        Type::Tau, dst, src, type);
}

Inst* InstFactory::makeTauIsNonNull(Opnd* dst, Opnd* src) {
    assert(dst->getType()->tag == Type::Tau);
    return makeInst(Op_TauIsNonNull,
                    Modifier(),
                    Type::Tau, dst, src);
}


//-----------------------------------------------------------------------------
// InstOptimizer methods
//-----------------------------------------------------------------------------

Inst*
InstOptimizer::dispatch(Inst* inst) {
    switch (inst->getOpcode()) {
    case Op_Add:                return caseAdd(inst);
    case Op_Mul:                return caseMul(inst);
    case Op_Sub:                return caseSub(inst);
    case Op_TauDiv:             return caseTauDiv(inst);
    case Op_TauRem:             return caseTauRem(inst);
    case Op_Neg:                return caseNeg(inst);
    case Op_MulHi:              return caseMulHi(inst);
    case Op_Min:                return caseMin(inst);
    case Op_Max:                return caseMax(inst);
    case Op_Abs:                return caseAbs(inst);
    case Op_And:                return caseAnd(inst);
    case Op_Or:                 return caseOr(inst);
    case Op_Xor:                return caseXor(inst);
    case Op_Not:                return caseNot(inst);
    case Op_Select:             return caseSelect(inst);
    case Op_Conv:               return caseConv(inst);
    case Op_ConvZE:             return caseConvZE(inst);
    case Op_ConvUnmanaged:      return caseConvUnmanaged(inst);
    case Op_Shladd:             return caseShladd(inst);
    case Op_Shl:                return caseShl(inst);
    case Op_Shr:                return caseShr(inst);
    case Op_Cmp:                return caseCmp(inst);
    case Op_Cmp3:               return caseCmp3(inst);
    case Op_Branch:             return caseBranch(inst->asBranchInst());
    case Op_Jump:               return caseJump(inst->asBranchInst());
    case Op_Switch:             return caseSwitch(inst->asSwitchInst());
    case Op_DirectCall:         return caseDirectCall(inst->asMethodCallInst());
    case Op_TauVirtualCall:        return caseTauVirtualCall(inst->asMethodCallInst());
    case Op_IndirectCall:       return caseIndirectCall(inst->asCallInst());
    case Op_IndirectMemoryCall: return caseIndirectMemoryCall(inst->asCallInst());
    case Op_JitHelperCall:      return caseJitHelperCall(inst->asJitHelperCallInst());
    case Op_VMHelperCall:       return caseVMHelperCall(inst->asVMHelperCallInst());
    case Op_Return:             return caseReturn(inst);
    case Op_Catch:              return caseCatch(inst);
    case Op_Throw:              return caseThrow(inst);
    case Op_PseudoThrow:        return casePseudoThrow(inst);
    case Op_ThrowSystemException: return caseThrowSystemException(inst);
    case Op_ThrowLinkingException: return caseThrowLinkingException(inst);
    case Op_JSR:                return caseJSR(inst);
    case Op_Ret:                return caseRet(inst);
    case Op_SaveRet:                return caseSaveRet(inst);
    case Op_Copy:               return caseCopy(inst);
    case Op_DefArg:             return caseDefArg(inst);
    case Op_LdConstant:         return caseLdConstant(inst->asConstInst());
    case Op_LdRef:              return caseLdRef(inst->asTokenInst());
    case Op_LdVar:              return caseLdVar(inst);
    case Op_LdVarAddr:          return caseLdVarAddr(inst);
    case Op_TauLdInd:              return caseTauLdInd(inst);
    case Op_TauLdField:         return caseTauLdField(inst->asFieldAccessInst());
    case Op_LdStatic:           return caseLdStatic(inst->asFieldAccessInst());
    case Op_TauLdElem:             return caseTauLdElem(inst->asTypeInst());
    case Op_LdFieldAddr:        return caseLdFieldAddr(inst->asFieldAccessInst());
    case Op_LdStaticAddr:       return caseLdStaticAddr(inst->asFieldAccessInst());
    case Op_LdElemAddr:         return caseLdElemAddr(inst->asTypeInst());
    case Op_TauLdVTableAddr:       return caseTauLdVTableAddr(inst);
    case Op_TauLdIntfcVTableAddr:  return caseTauLdIntfcVTableAddr(inst->asTypeInst());
    case Op_TauLdVirtFunAddr:      return caseTauLdVirtFunAddr(inst->asMethodInst());
    case Op_TauLdVirtFunAddrSlot:  return caseTauLdVirtFunAddrSlot(inst->asMethodInst());
    case Op_LdFunAddr:          return caseLdFunAddr(inst->asMethodInst());
    case Op_LdFunAddrSlot:      return caseLdFunAddrSlot(inst->asMethodInst());
    case Op_GetVTableAddr:      return caseGetVTableAddr(inst->asTypeInst());
    case Op_GetClassObj:      return caseGetClassObj(inst->asTypeInst());
    case Op_TauArrayLen:           return caseTauArrayLen(inst);
    case Op_LdArrayBaseAddr:    return caseLdArrayBaseAddr(inst);
    case Op_AddScaledIndex:     return caseAddScaledIndex(inst);
    case Op_StVar:              return caseStVar(inst);
    case Op_TauStInd:              return caseTauStInd(inst);
    case Op_TauStField:            return caseTauStField(inst);
    case Op_TauStElem:             return caseTauStElem(inst);
    case Op_TauStStatic:           return caseTauStStatic(inst);
    case Op_TauStRef:              return caseTauStRef(inst);
    case Op_TauCheckBounds:        return caseTauCheckBounds(inst);
    case Op_TauCheckLowerBound:    return caseTauCheckLowerBound(inst);
    case Op_TauCheckUpperBound:    return caseTauCheckUpperBound(inst);
    case Op_TauCheckNull:          return caseTauCheckNull(inst);
    case Op_TauCheckZero:          return caseTauCheckZero(inst);
    case Op_TauCheckDivOpnds:      return caseTauCheckDivOpnds(inst);
    case Op_TauCheckElemType:      return caseTauCheckElemType(inst);
    case Op_TauCheckFinite:        return caseTauCheckFinite(inst);
    case Op_NewObj:             return caseNewObj(inst);
    case Op_NewArray:           return caseNewArray(inst);
    case Op_NewMultiArray:      return caseNewMultiArray(inst);
    case Op_TauMonitorEnter:       return caseTauMonitorEnter(inst);
    case Op_TauMonitorExit:        return caseTauMonitorExit(inst);
    case Op_TypeMonitorEnter:   return caseTypeMonitorEnter(inst);
    case Op_TypeMonitorExit:    return caseTypeMonitorExit(inst);
    case Op_LdLockAddr:         return caseLdLockAddr(inst);
    case Op_IncRecCount:        return caseIncRecCount(inst);
    case Op_TauBalancedMonitorEnter: return caseTauBalancedMonitorEnter(inst);
    case Op_BalancedMonitorExit:  return caseBalancedMonitorExit(inst);
    case Op_TauOptimisticBalancedMonitorEnter: return caseTauOptimisticBalancedMonitorEnter(inst);
    case Op_OptimisticBalancedMonitorExit:  return caseOptimisticBalancedMonitorExit(inst);
    case Op_MonitorEnterFence:  return caseMonitorEnterFence(inst);
    case Op_MonitorExitFence:   return caseMonitorExitFence(inst);
    case Op_TauStaticCast:         return caseTauStaticCast(inst->asTypeInst());
    case Op_TauCast:            return caseTauCast(inst->asTypeInst());
    case Op_TauAsType:          return caseTauAsType(inst->asTypeInst());
    case Op_TauInstanceOf:      return caseTauInstanceOf(inst->asTypeInst());
    case Op_InitType:           return caseInitType(inst->asTypeInst());
    case Op_Label:              return caseLabel(inst);
    case Op_MethodEntry:        return caseMethodEntry(inst);
    case Op_MethodEnd:          return caseMethodEnd(inst);
    case Op_Phi:                return casePhi(inst);
    case Op_TauPi:                 return caseTauPi(inst->asTauPiInst());
    case Op_IncCounter:         return caseIncCounter(inst);
    case Op_Prefetch:          return casePrefetch(inst);
    case Op_UncompressRef:      return caseUncompressRef(inst);
    case Op_CompressRef:        return caseCompressRef(inst);
    case Op_LdFieldOffset:      return caseLdFieldOffset(inst->asFieldAccessInst());
    case Op_LdFieldOffsetPlusHeapbase:   return caseLdFieldOffsetPlusHeapbase(inst->asFieldAccessInst());
    case Op_LdArrayBaseOffset:  return caseLdArrayBaseOffset(inst->asTypeInst());
    case Op_LdArrayBaseOffsetPlusHeapbase:  return caseLdArrayBaseOffsetPlusHeapbase(inst->asTypeInst());
    case Op_LdArrayLenOffset:   return caseLdArrayLenOffset(inst->asTypeInst());
    case Op_LdArrayLenOffsetPlusHeapbase:  return caseLdArrayLenOffsetPlusHeapbase(inst->asTypeInst());
    case Op_AddOffset:          return caseAddOffset(inst);
    case Op_AddOffsetPlusHeapbase: return caseAddOffsetPlusHeapbase(inst);
    case Op_TauPoint:           return caseTauPoint(inst);
    case Op_TauEdge:            return caseTauEdge(inst);
    case Op_TauAnd:             return caseTauAnd(inst);
    case Op_TauUnsafe:          return caseTauUnsafe(inst);
    case Op_TauSafe:            return caseTauSafe(inst);
    case Op_TauCheckCast:       return caseTauCheckCast(inst->asTypeInst());
    case Op_TauHasType:         return caseTauHasType(inst->asTypeInst());
    case Op_TauHasExactType:    return caseTauHasExactType(inst->asTypeInst());
    case Op_TauIsNonNull:       return caseTauIsNonNull(inst);
    case Op_IdentHC:            return caseIdentHC(inst);

    default:
        ::std::cerr << "Unknown opcode! " << inst->getOpcode() << " : "
             << inst->getOperation().getOpcodeString() << ::std::endl;
        assert(0);
    }
    return NULL;
}

} //namespace Jitrino 
