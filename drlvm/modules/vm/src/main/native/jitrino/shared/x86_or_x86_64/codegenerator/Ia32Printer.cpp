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

#include "Ia32Printer.h"
#include "Log.h"
#include "PlatformDependant.h"

namespace Jitrino
{
namespace Ia32{


//========================================================================================
// class Printer
//========================================================================================

//_____________________________________________________________________________________________
Printer& Printer::setStream(::std::ostream& _os)
{
    assert(os==NULL);
    os=&_os;
    return *this;
}
//_____________________________________________________________________________________________
Printer& Printer::open(char* fname)
{
    logs.open(fname);
    setStream(logs.out());
    return *this;
}
//_____________________________________________________________________________________________
void Printer::close()
{
    logs.close();
}
//_____________________________________________________________________________________________
void Printer::print(U_32 indent)
{
    printHeader(indent);
    printBody(indent);
    printEnd(indent);
}

//_____________________________________________________________________________________________
void Printer::printHeader(U_32 indent)
{
    assert(irManager!=NULL);
    ::std::ostream& os = getStream();
    os << ::std::endl; printIndent(indent);
    os << "====================================================================" << ::std::endl; printIndent(indent);
    os << irManager->getMethodDesc().getParentType()->getName()<<"."<<irManager->getMethodDesc().getName()<<" " << (title?title:"") << ::std::endl; printIndent(indent);
    os << "====================================================================" << ::std::endl; printIndent(indent);
    os << ::std::endl; 
}

//_____________________________________________________________________________________________
void Printer::printEnd(U_32 indent)
{
    ::std::ostream& os = getStream();
    os << ::std::endl;
    os.flush();
}

//_____________________________________________________________________________________________
void Printer::printBody(U_32 indent)
{
    ::std::ostream& os = getStream();
    os << "Printer::printBody is stub implementation"<< ::std::endl;
    os.flush();
}

//========================================================================================
// class IRPrinter
//========================================================================================
//_____________________________________________________________________________________________
void IRPrinter::print(U_32 indent)
{
    Printer::print();
}

//_____________________________________________________________________________________________
void IRPrinter::printBody(U_32 indent)
{
    printCFG(indent);
}

//_____________________________________________________________________________________________
void IRPrinter::printCFG(U_32 indent)
{
    assert(irManager!=NULL);
    std::ostream& os = getStream();
    const Nodes& nodes = irManager->getFlowGraph()->getNodesPostOrder();
    //topological ordering
    for (Nodes::const_reverse_iterator it = nodes.rbegin(), end = nodes.rend(); it!=end; ++it) {
        Node* node = *it;
        printNode(node, indent);
        os<<std::endl;
    }
}

//_____________________________________________________________________________________________
void IRPrinter::printNodeName(const Node * node)
{
    ::std::ostream& os = getStream();
    if (!node){
        os<<"NULL";
        return;
    }
    
    if (node->isBlockNode()) {
        os << "BB_";
    } else if (node->isDispatchNode()) {
        if (node!=irManager->getFlowGraph()->getUnwindNode()) {
            os << "DN_";
        } else {
            os << "UN_";
        }
    } else {
        assert(node->isExitNode());
        os << "EN_";
    }
    os << node->getId();
    if (node == irManager->getFlowGraph()->getEntryNode()) {
        os << "_prolog";
    } else if (irManager->isEpilog(node)) {
        os << "_epilog";
    }
}

//_____________________________________________________________________________________________
void IRPrinter::printNodeHeader(const Node * node, U_32 indent)
{
    std::ostream& os = getStream();
    printIndent(indent);
    printNodeName(node);
    os<<::std::endl; printIndent(indent);

    if (((CGNode*)node)->getPersistentId()!=UnknownId){
        os << "  PersistentId = " << ((CGNode*)node)->getPersistentId() << std::endl;
        printIndent(indent);
    }
    if (node->getExecCount() >= 0.0) 
        os << "  ExecCnt = " << node->getExecCount() << std::endl;
    else 
        os << "  ExecCnt = Unknown" << std::endl;
    printIndent(indent);

    LoopTree* lt = irManager->getFlowGraph()->getLoopTree();
    if (lt->isValid()) {
        os << "  Loop: Depth=" << lt->getLoopDepth(node);
        if (lt->isLoopHeader(node)) {
            os << ", hdr, hdr= ";
        } else {
            os << ", !hdr, hdr=";
        }
        printNodeName(lt->getLoopHeader(node));
        os << std::endl; printIndent(indent);
    }
    {
        os << "  Predcessors: ";
        const Edges& es=node->getInEdges();
        for (Edges::const_iterator ite = es.begin(), ende = es.end(); ite!=ende; ++ite) {
            Edge* e= *ite;
            printNodeName(e->getSourceNode());
            os << " ";
        }
        os << std::endl; printIndent(indent);
    }
    {
        const Edges& es=node->getOutEdges();
        os << "  Successors:  ";
        for (Edges::const_iterator ite = es.begin(), ende = es.end(); ite!=ende; ++ite) {
            Edge* e= *ite;
            const Node * succ = e->getTargetNode();
            printNodeName(succ);
            os << " [Prob=" << (double)e->getEdgeProb() << "]";
            if (e->isCatchEdge()) {
                os << "(" << ((CatchEdge *)e)->getPriority() << ",";
                printType(((CatchEdge *)e)->getType());
                os << ")";
            }
            if (lt->isValid()) {
                if (lt->isBackEdge(e))  {
                    os << "(backedge)";
                }
                if (lt->isLoopExit(e)) {
                    os << "(loopexit)";
                }
            }
            if (e->isFalseEdge() || e->isTrueEdge()) {
                Inst* br = (Inst*)e->getSourceNode()->getLastInst();
                os << "(Br=I" << (int) br->getId() << ")";
            }
            os << " ";
    }
    }
    if (node->isBlockNode()) {
        const BasicBlock * bb=(const BasicBlock*)node;
        if (irManager->isLaidOut()){
            os << std::endl; printIndent(indent);
            os << "Layout Succ: ";
            printNodeName(bb->getLayoutSucc());
            if (irManager->getCodeStartAddr()!=NULL){
                os << std::endl; printIndent(indent);
                os << "Block code address: " << (void*)bb->getCodeStartAddr();
            }
        }
    }
}

//_____________________________________________________________________________________________
void IRPrinter::printNodeInstList(const Node* bb, U_32 indent)
{
    ::std::ostream& os = getStream();
    for (Inst * inst = (Inst*)bb->getFirstInst(); inst != NULL; inst = inst->getNextInst()) {
        Inst::Kind kind=inst->getKind();
        if ((kind & instFilter)==(U_32)kind){
            printIndent(indent+1); 
            if (irManager->getCodeStartAddr()!=NULL){
                os<<(void*)inst->getCodeStartAddr()<<' ';
            }
            printInst(inst); 
            os << std::endl;
        }
    }
}

//_____________________________________________________________________________________________
void IRPrinter::printNode(const Node * node, U_32 indent)
{
    std::ostream& os = getStream();
    printNodeHeader(node, indent);
    if (!node->isEmpty()) {
        os << std::endl;
        printNodeInstList((BasicBlock*)node, indent);
    }
    os << std::endl;
}

//_____________________________________________________________________________________________
void IRPrinter::printEdge(const Edge * edge)
{
}

//_____________________________________________________________________________________________
const char * IRPrinter::getPseudoInstPrintName(Inst::Kind k)
{
    switch(k){
        case Inst::Kind_PseudoInst: return "PseudoInst";
        case Inst::Kind_EntryPointPseudoInst: return "EntryPointPseudoInst";
        case Inst::Kind_AliasPseudoInst: return "AliasPseudoInst";
        case Inst::Kind_CatchPseudoInst: return "CatchPseudoInst";
        case Inst::Kind_CopyPseudoInst: return "CopyPseudoInst";
        case Inst::Kind_I8PseudoInst: return "I8PseudoInst";
        case Inst::Kind_GCInfoPseudoInst: return "GCInfoPseudoInst";
        case Inst::Kind_MethodEntryPseudoInst: return "MethodEntryPseudoInst";
        case Inst::Kind_MethodEndPseudoInst: return "MethodEndPseudoInst";
        case Inst::Kind_EmptyPseudoInst: return "EmptyPseudoInst";
        case Inst::Kind_CMPXCHG8BPseudoInst: return "CMPXCHG8BPseudoInst";
        default: return "";
    }
}

//_____________________________________________________________________________________________
U_32 IRPrinter::printInstOpnds(const Inst * inst, U_32 orf)
{
    ::std::ostream& os = getStream();
    if (!(orf&Inst::OpndRole_ForIterator))
        return 0;
    U_32 printedOpnds=0;
    bool explicitOnly=(orf&Inst::OpndRole_ForIterator)==Inst::OpndRole_Explicit;

    Inst::Opnds opnds(inst, orf);
    for (Inst::Opnds::iterator it = opnds.begin(); it != opnds.end(); it = opnds.next(it), printedOpnds++){
        if (printedOpnds)
            os<<",";
        else if (!explicitOnly){
            os<<"("; printOpndRoles(orf); os<<":";
        }
        printOpnd(inst, it, false, false);
    }
    if (printedOpnds && !explicitOnly)
        os<<")";
    return printedOpnds;
}

void IRPrinter::printInst(const Inst * inst)
{
    ::std::ostream& os = getStream();
    os<<"I"<<inst->getId()<<": ";

    if (opndRolesFilter & Inst::OpndRole_Def){
        U_32 printedOpndsTotal=0, printedOpnds=0;
        if (inst->getForm()==Inst::Form_Extended)
            printedOpnds=printInstOpnds(inst, (Inst::OpndRole_Def|Inst::OpndRole_Explicit)&opndRolesFilter);
        if (printedOpnds){ os<<" "; printedOpndsTotal+=printedOpnds; }
        printedOpnds=printInstOpnds(inst, (Inst::OpndRole_Def|Inst::OpndRole_Auxilary)&opndRolesFilter);
        if (printedOpnds){ os<<" "; printedOpndsTotal+=printedOpnds; }
        printedOpnds=printInstOpnds(inst, (Inst::OpndRole_Def|Inst::OpndRole_Implicit)&opndRolesFilter);
        if (printedOpnds){ os<<" "; printedOpndsTotal+=printedOpnds; }
        
        if (printedOpndsTotal)
            os<<"=";
    }

    if (inst->hasKind(Inst::Kind_PseudoInst)){
        os<<getPseudoInstPrintName(inst->getKind());
        if (inst->getMnemonic() != Mnemonic_Null) {
            os<< "/" << Encoder::getMnemonicString(inst->getMnemonic());
        }
    }else{
        if( inst->getMnemonic() != Mnemonic_Null )
            os<< Encoder::getMnemonicString( inst->getMnemonic() );
        if (inst->hasKind(Inst::Kind_BranchInst) && inst->getNode()!=NULL){
            Node* target=((BranchInst*)inst)->getTrueTarget();
            if (target){
                os<<" "; printNodeName(target);
            }
        }
    }
    if ((inst->getKind() == Inst::Kind_MethodEndPseudoInst || 
            inst->getKind() == Inst::Kind_MethodEntryPseudoInst)) {
        MethodMarkerPseudoInst* methMarkerInst = (MethodMarkerPseudoInst*)inst;
        os<<"[";
        os<<methMarkerInst->getMethodDesc()->getParentType()->getName();
        os<<"."<<methMarkerInst->getMethodDesc()->getName();
        os<<"]";
        if (inst->getKind() == Inst::Kind_MethodEndPseudoInst) os << "+++";
        else os << "---";
    }

    os<<" ";
    U_32 printedOpndsTotal=0, printedOpnds=0;
    printedOpnds=printInstOpnds(inst, ((inst->getForm()==Inst::Form_Extended?Inst::OpndRole_Use:Inst::OpndRole_UseDef)|Inst::OpndRole_Explicit)&opndRolesFilter);
    if (printedOpnds){ os<<" "; printedOpndsTotal+=printedOpnds; }
    printedOpnds=printInstOpnds(inst, (Inst::OpndRole_Use|Inst::OpndRole_Auxilary)&opndRolesFilter);
    if (printedOpnds){ os<<" "; printedOpndsTotal+=printedOpnds; }
    printedOpnds=printInstOpnds(inst, (Inst::OpndRole_Use|Inst::OpndRole_Implicit)&opndRolesFilter);
    if (printedOpnds){ os<<" "; printedOpndsTotal+=printedOpnds; }

    if (inst->hasKind(Inst::Kind_CallInst) || inst->hasKind(Inst::Kind_MethodEntryPseudoInst)) {
        os<<"[bcmap:";
        if (inst->getBCOffset()==ILLEGAL_BC_MAPPING_VALUE) {
            os<<"unknown";
        } else {
            os<<inst->getBCOffset();
        }
        os<<"] ";
    }

    if (inst->hasKind(Inst::Kind_GCInfoPseudoInst)) {
        const GCInfoPseudoInst* gcInst = (GCInfoPseudoInst*)inst;
        Opnd * const * uses = gcInst->getOpnds();
        const StlVector<I_32>& offsets = gcInst->offsets;
        os<<"[phase:"<<gcInst->desc<<"]";
        os<<"(";
        assert(!offsets.empty());
        for (U_32 i = 0, n = (U_32)offsets.size(); i<n; i++) {
                I_32 offset = offsets[i];
                Opnd* opnd = uses[i];
                if (i>0) {
                    os<<",";
                }
                os << "["; printOpndName(opnd); os <<"," <<offset <<"]";
        }
        os<<") ";
    }
}


//_________________________________________________________________________________________________
void IRPrinter::printOpndRoles(U_32 roles)
{
    ::std::ostream& os=getStream();
    if (roles&Inst::OpndRole_Explicit)          os<<"E"; 
    if (roles&Inst::OpndRole_Auxilary)          os<<"A"; 
    if (roles&Inst::OpndRole_Implicit)          os<<"I"; 
    if (roles&Inst::OpndRole_MemOpndSubOpnd)    os<<"M"; 
    OpcodeDescriptionPrinter::printOpndRoles(roles);
}

//_____________________________________________________________________________________________
void IRPrinter::printOpndName(const Opnd * opnd)
{
    ::std::ostream& os = getStream();
    Opnd::DefScope ds=opnd->getDefScope();
    os<<(
        ds==Opnd::DefScope_Variable?"v":
        ds==Opnd::DefScope_SemiTemporary?"s":
        ds==Opnd::DefScope_Temporary?"t":
        "o"
    )<<opnd->getFirstId();
}

//_________________________________________________________________________________________________
U_32 IRPrinter::getOpndNameLength(Opnd * opnd)
{
    U_32 id=opnd->getFirstId();
    U_32 idLength=id<10?1:id<100?2:id<1000?3:id<10000?4:5;
    return 1+idLength;
}

//_____________________________________________________________________________________________
void IRPrinter::printOpnd(const Inst * inst, U_32 idx, bool isLiveBefore, bool isLiveAfter)
{
    printOpnd(inst->getOpnd(idx), inst->getOpndRoles(idx), isLiveBefore, isLiveAfter);
}

//_____________________________________________________________________________________________
void IRPrinter::printRuntimeInfo(const Opnd::RuntimeInfo * info)
{
    ::std::ostream& os = getStream();
    switch(info->getKind()){
        case Opnd::RuntimeInfo::Kind_HelperAddress: 
            /** The value of the operand is compilationInterface->getRuntimeHelperAddress */
            {
            os<<"h:"<<
                irManager->getCompilationInterface().getRuntimeHelperName(
                    (VM_RT_SUPPORT)(POINTER_SIZE_INT)info->getValue(0)
                );
            }break;
        case Opnd::RuntimeInfo::Kind_InternalHelperAddress:
            /** The value of the operand is irManager.getInternalHelperInfo((const char*)[0]).pfn */
            {
            os<<"ih:"<<(const char*)info->getValue(0)<<":"<<(void*)irManager->getInternalHelperInfo((const char*)info->getValue(0))->pfn;
            }break;
        case Opnd::RuntimeInfo::Kind_TypeRuntimeId: 
            /*  The value of the operand is [0]->ObjectType::getRuntimeIdentifier() */
            {
            os<<"id:"; printType((NamedType*)info->getValue(0));
            }break;
        case Opnd::RuntimeInfo::Kind_AllocationHandle: 
            /* The value of the operand is [0]->ObjectType::getAllocationHandle() */
            {
            os<<"ah:"; printType((ObjectType*)info->getValue(0));
            }break;
        case Opnd::RuntimeInfo::Kind_StringDescription: 
            /* [0] - Type * - the containing class, [1] - string token */
            assert(0);
            break;
        case Opnd::RuntimeInfo::Kind_Size: 
            /* The value of the operand is [0]->ObjectType::getObjectSize() */
            {
            os<<"sz:"; printType((ObjectType*)info->getValue(0));
            }break;
        case Opnd::RuntimeInfo::Kind_StringAddress:
            /** The value of the operand is the address where the interned version of the string is stored*/
            {
            os<<"&str:";
            os<<(POINTER_SIZE_INT)info->getValue(1); // string token
            }break;
        case Opnd::RuntimeInfo::Kind_StaticFieldAddress:
            /** The value of the operand is [0]->FieldDesc::getAddress() */
            {
            FieldDesc * fd=(FieldDesc*)info->getValue(0);
            os<<"&f:"; 
            os<<fd->getParentType()->getName()<<"."<<fd->getName();
            }break;
        case Opnd::RuntimeInfo::Kind_FieldOffset:
            /** The value of the operand is [0]->FieldDesc::getOffset() */
            {
            FieldDesc * fd=(FieldDesc*)info->getValue(0);
            os<<"fo:"; 
            os<<fd->getParentType()->getName()<<"."<<fd->getName();
            }break;
        case Opnd::RuntimeInfo::Kind_VTableAddrOffset:
            /** The value of the operand is compilationInterface.getVTableOffset(), zero args */
            {
            os<<"vtao"; 
            }break;
        case Opnd::RuntimeInfo::Kind_VTableConstantAddr:
            /** The value of the operand is [0]->ObjectType::getVTable() */
            {
            os<<"vtca:"; printType((ObjectType*)info->getValue(0));
            }break;
        case Opnd::RuntimeInfo::Kind_MethodVtableSlotOffset:
            /** The value of the operand is [0]->MethodDesc::getOffset() */
            {
            MethodDesc * md=(MethodDesc*)info->getValue(0);
            os<<"vtso:"; 
            os<<md->getParentType()->getName()<<"."<<md->getName();
            }break;
        case Opnd::RuntimeInfo::Kind_MethodIndirectAddr:
            /** The value of the operand is [0]->MethodDesc::getIndirectAddress() */
            {
            MethodDesc * md=(MethodDesc*)info->getValue(0);
            os<<"&m:"; 
            os<<md->getParentType()->getName()<<"."<<md->getName();
            }break;
        case Opnd::RuntimeInfo::Kind_MethodDirectAddr:
            /** The value of the operand is *[0]->MethodDesc::getIndirectAddress() */
            {
            MethodDesc * md=(MethodDesc*)info->getValue(0);
            os<<"m:"; 
            os<<md->getParentType()->getName()<<"."<<md->getName();
            }break;
        case Opnd::RuntimeInfo::Kind_ConstantAreaItem:
            break;
        case Opnd::RuntimeInfo::Kind_MethodRuntimeId: 
            {
            MethodDesc * md=(MethodDesc*)info->getValue(0);
            os<<"mh:"; 
            os<<md->getParentType()->getName()<<"."<<md->getName();
            }break;
        case Opnd::RuntimeInfo::Kind_EM_ProfileAccessInterface:
            /** The value of the operand is a pointer to the EM_ProfileAccessInterface */
            {
                os<<"em_pi"; 
            }break;
        case Opnd::RuntimeInfo::Kind_Method_Value_Profile_Handle:
            /** The value of the operand is Method_Profile_Handle for the value profile of the compiled method */
            {
                os<<"mvph"; 
            }break;
        default:
            assert(0);
    }
    U_32 additionalOffset=info->getAdditionalOffset();
    if (additionalOffset>0)
        os<<"+"<<additionalOffset;
}

//_____________________________________________________________________________________________
void IRPrinter::printOpnd(const Opnd * opnd, U_32 roles, bool isLiveBefore, bool isLiveAfter)
{
    ::std::ostream& os = getStream();

    if (isLiveBefore) os<<".";
    printOpndName(opnd);
    if (isLiveAfter) os<<".";

    if (opndFlavor&OpndFlavor_Location){
        if (opnd->isPlacedIn(OpndKind_Reg)){
            os<<"("; printRegName(opnd->getRegName()); os<<")";
        }else if(opnd->isPlacedIn(OpndKind_Mem)){
            if (opnd->getSegReg() != RegName_Null) {
                os<<"(";printRegName(opnd->getSegReg());os<<":";
            }
            os<<"[";
            U_32 oldOpndFlavor=opndFlavor;
            opndFlavor&=~OpndFlavor_Type;
            bool append=false;
            for (U_32 i=0; i<MemOpndSubOpndKind_Count; i++){
                Opnd * subOpnd=opnd->getMemOpndSubOpnd((MemOpndSubOpndKind)i);
                if (subOpnd){
                    if (append){
                        if ((MemOpndSubOpndKind)i==MemOpndSubOpndKind_Scale)
                            os<<"*";
                        else
                            os<<"+";
                    }
                    printOpnd(subOpnd);
                    append=true;
                }
            }
            opndFlavor=oldOpndFlavor;
            os<<"]";
            if (opnd->getSegReg() != RegName_Null) {
                os<<")";
            }
        }else if(opnd->isPlacedIn(OpndKind_Imm)){
            os<<"("<<opnd->getImmValue();
            if (opndFlavor & OpndFlavor_RuntimeInfo){
                Opnd::RuntimeInfo * ri=opnd->getRuntimeInfo();
                if (ri!=NULL){
                    os<<":";
                    printRuntimeInfo(ri);
                }
            }
            os<<")";
        }
    }
    if (opndFlavor&OpndFlavor_Type){
        os<<":"; printType(opnd->getType());
    }

}

//_____________________________________________________________________________________________
void IRPrinter::printType(const Type * type)
{
    ::std::ostream& os = getStream();
    ((Type*)type)->print(os);
}

//========================================================================================
// class IRLivenessPrinter
//========================================================================================
//_____________________________________________________________________________________________
void IRLivenessPrinter::printNode(const Node * node, U_32 indent)
{
    assert(irManager!=NULL);
    ::std::ostream& os = getStream();
    printNodeHeader(node, indent);
    os << ::std::endl;

    BitSet * ls=irManager->getLiveAtEntry(node);
    os<<"Live at entry: "; printLiveSet(ls); os<<::std::endl;

    MemoryManager mm("IRLivenessPrinter::printNode");
    ls=new(mm) BitSet(mm, irManager->getOpndCount());
    irManager->getLiveAtExit(node, *ls);
    os<<"Live at exit: "; printLiveSet(ls); os<<::std::endl;

    os << ::std::endl;
}

//_____________________________________________________________________________________________
void IRLivenessPrinter::printLiveSet(const BitSet * ls)
{
    std::ostream& os = getStream();
    assert(irManager!=NULL);
    if (ls==NULL){
        os<<"Null";
        return;
    }
    for (U_32 i=0, n=irManager->getOpndCount(); i<n; i++){
        Opnd * opnd=irManager->getOpnd(i);
        if (ls->getBit(opnd->getId())){
            printOpndName(opnd); os<<"("<<opnd->getId()<<")"<<" ";
        }
    }
}

//========================================================================================
// class IROpndPrinter
//========================================================================================
//_____________________________________________________________________________________________
void IROpndPrinter::printBody(U_32 indent)
{
    assert(irManager!=NULL);
    ::std::ostream& os = getStream();
    for (U_32 i=0, n=irManager->getOpndCount(); i<n; i++){
        printIndent(indent);
        Opnd * opnd=irManager->getOpnd(i);
        printOpnd(opnd);
        os<<'\t';
        os<<"addr="<<opnd;
        os<<::std::endl;
        printIndent(indent+1);
        os<<"Initial constraint: ";
        printConstraint(opnd->getConstraint(Opnd::ConstraintKind_Initial));
        os<<::std::endl;
        printIndent(indent+1);
        os<<"Calculated constraint: ";
        printConstraint(opnd->getConstraint(Opnd::ConstraintKind_Calculated));
        os<<::std::endl;
        printIndent(indent+1);
        os<<"Location constraint: ";
        printConstraint(opnd->getConstraint(Opnd::ConstraintKind_Location));
        os<<::std::endl;
        printIndent(indent);
    }
}

//_____________________________________________________________________________________________
void IROpndPrinter::printHeader(U_32 indent)
{
    assert(irManager!=NULL);
    ::std::ostream& os = getStream();
    os << ::std::endl; printIndent(indent);
    os << "...................................................................." << ::std::endl; printIndent(indent);
    os << irManager->getMethodDesc().getParentType()->getName()<<"."<<irManager->getMethodDesc().getName()<<": Operands in " << (title?title:"???") << ::std::endl; printIndent(indent);
    os << "...................................................................." << ::std::endl; printIndent(indent);
    os << ::std::endl; 
}


//========================================================================================
// class IRInstConstraintPrinter
//========================================================================================

//_____________________________________________________________________________________________
void IRInstConstraintPrinter::printOpnd(const Inst * inst, U_32 idx, bool isLiveBefore, bool isLiveAfter)
{
    ::std::ostream& os = getStream();
    Opnd * opnd=inst->getOpnd(idx);
    printOpndName(opnd);
    os<<"(";        
    printConstraint(((Inst *)inst)->getConstraint(idx, 0, OpndSize_Null));
    os<<")";
}



//========================================================================================
// class OpcodeDescriptionPrinter
//========================================================================================
//_____________________________________________________________________________________________
void OpcodeDescriptionPrinter::printConstraint(Constraint c)
{
    ::std::ostream& os = getStream();
    if (c.isNull()){
        os<<"Null";
        return;
    }
    os<<getOpndSizeString(c.getSize())<<":";
    bool written=false;
    U_32 kind=c.getKind();
    if (kind & OpndKind_Imm){
        if (written) os<<"|";
        os << "Imm";
        written=true;
    }
    if (kind & OpndKind_Mem){
        if (written) os<<"|";
        os << "Mem";
        written=true;
    }

    if (kind & OpndKind_Reg){
        if (written) os<<"|";
        os << getOpndKindString((OpndKind)(kind & OpndKind_Reg));
        os <<"{";
        {
            bool written=false;
            U_32 mask=c.getMask();
            for (U_32 i=1, idx=0; i; i<<=1, idx++){
                if (mask&i){
                    const char * regName=getRegNameString(getRegName((OpndKind)(kind&OpndKind_Reg), c.getSize(), idx));
                    if (regName!=NULL){
                        if (written) os<<"|";
                        os << regName;
                        written=true;
                    }
                }
            }
        }
        os <<"}";
        written=true;
    }
}

//_____________________________________________________________________________________________
void OpcodeDescriptionPrinter::printRegName(const RegName regName)
{
    const char * regNameString=getRegNameString(regName);
    getStream()<<(regNameString?regNameString:"???");
}

//_________________________________________________________________________________________________
void OpcodeDescriptionPrinter::printOpndRoles(U_32 roles)
{
    ::std::ostream& os=getStream();
    if (roles&Inst::OpndRole_Def)   os<<"D";
    if (roles&Inst::OpndRole_Use)   os<<"U";
}

//_________________________________________________________________________________________________
void OpcodeDescriptionPrinter::printOpndRolesDescription(const Encoder::OpndRolesDescription * ord)
{
    ::std::ostream& os=getStream();
    os<<"count: "<<(U_32)ord->count<<" (D:"<<ord->defCount<<",U:"<<ord->useCount<<"); roles: "; 
    for (U_32 i=0; i<ord->count; i++){
        if (i>0)
            os<<',';
        printOpndRoles(Encoder::getOpndRoles(*ord, i));
    }
}

//_________________________________________________________________________________________________
void OpcodeDescriptionPrinter::printOpcodeDescription(const Encoder::OpcodeDescription * od, U_32 indent)
{
    assert( false );
}

//_________________________________________________________________________________________________
void OpcodeDescriptionPrinter::printOpcodeGroup(const Encoder::OpcodeGroup* ogd, U_32 indent)
{
    assert( false );
}

//_________________________________________________________________________________________________
void OpcodeDescriptionPrinter::print(U_32 indent)
{
    assert( false );
}


//========================================================================================
// class IRDotPrinter
//========================================================================================

void IRDotPrinter::printNode(const Node * node)
{
    std::ostream& out=getStream();
    printNodeName(node); 
    out << " [label=\"";

    BasicBlock * bb=node->isBlockNode()?(BasicBlock*)node:NULL;
    if (bb)out << "{";
    printNodeName(node);
    if (((CGNode*)node)->getPersistentId()!=UnknownId)
        out << " pid: " << ((CGNode*)node)->getPersistentId() << " ";
    if(node->getExecCount() > 0)
        out << " ec:" << node->getExecCount() << " ";

    LoopTree* lt = irManager->getFlowGraph()->getLoopTree();
    if (lt->isValid() && lt->getLoopHeader(node, false)!=NULL) {
        out << "  Loop: Depth=" << lt->getLoopDepth(node);
        if (lt->isLoopHeader(node)) 
            out << ", hdr, hdr= ";
        else
            out << ", !hdr, hdr=";
        printNodeName(lt->getLoopHeader(node));
    }

    if (bb!=NULL && irManager->getCodeStartAddr()!=NULL){
        out<<", code="<<(void*)bb->getCodeStartAddr();
    }

    if (!node->isEmpty()) {
        out << "\\l|\\" << std::endl;
        for (Inst * inst = (Inst*)node->getFirstInst(); inst != NULL; inst = inst->getNextInst()) {
            Inst::Kind kind=inst->getKind();
            if ((kind & instFilter)==(U_32)kind){
                printInst(inst);
                uint16 bcOffset = inst->getBCOffset();
                if (bcOffset != ILLEGAL_BC_MAPPING_VALUE) out<<" bcOff: "<< bcOffset << " ";
                out << "\\l\\" << ::std::endl;
            }
        }
        out << "}";
    }
    out << "\"";
    if (node->isDispatchNode()) {
        if (node!=irManager->getFlowGraph()->getUnwindNode()) {
            out << ",shape=diamond,color=blue";
        } else {
            out << ",shape=diamond,color=red";
        }
    } else if (node->isExitNode()) {
        out << ",shape=ellipse,color=green";
    }
    out << "]" << std::endl;
}

//_________________________________________________________________________________________________
void IRDotPrinter::printEdge(const Edge * edge)
{
    std::ostream& out=getStream();
    Node * from=edge->getSourceNode();
    Node * to=edge->getTargetNode();
    printNodeName(from);
    out<<" -> ";
    printNodeName(to);
    out<<" [taillabel=\"";
    if (edge->getEdgeProb()>=0.0)
        out<<"p: "<<edge->getEdgeProb();
    out<<"\"";

    Node* unwind = irManager->getFlowGraph()->getUnwindNode();
    if (edge->isFalseEdge()) {
        out<<",style=bold";
    } else if (edge->isTrueEdge()) {
        ;
    } else if (edge->isDispatchEdge()) {
        out<<",style=dotted,color=blue";
    } else if (to == unwind || from == unwind) {
        out<<",style=dotted,color=red";
    } else if (to->isExitNode()) {
        out<<",style=dotted,color=green";
    }
    
    LoopTree* lt = irManager->getFlowGraph()->getLoopTree();
    if (lt->isValid() && lt->isLoopExit(edge)) {
        out<<",arrowtail=inv";
    }

    if (edge->isCatchEdge()){
        out<<",color=blue,headlabel=\"Type: ";
        printType(((CatchEdge*)edge)->getType());
        out<<" pri:"<<((CatchEdge*)edge)->getPriority()<<"\"";
    }
    out<<"];"<<std::endl;
}

//_________________________________________________________________________________________________
void IRDotPrinter::printLayoutEdge(const BasicBlock * from, const BasicBlock * to)
{
    ::std::ostream& out=getStream();
    printNodeName(from);
    out<<" -> ";
    printNodeName(to);
    out<<" [";
    out<<"style=dotted,color=gray";
    out<<"]";
}

//_________________________________________________________________________________________________
void IRDotPrinter::printHeader(U_32 indent)
{
    assert(irManager!=NULL);
    getStream() << "digraph dotgraph {" << ::std::endl
        << "center=TRUE;" << ::std::endl
        << "margin=\".2,.2\";" << ::std::endl
        << "ranksep=\".25\";" << ::std::endl
        << "nodesep=\".20\";" << ::std::endl
        << "page=\"200,260\";" << ::std::endl
        << "ratio=auto;" << ::std::endl
        << "node [shape=record,fontname=\"Courier\",fontsize=9];" << ::std::endl
        << "edge [minlen=2];" << ::std::endl
        << "label=\""
        << irManager->getMethodDesc().getParentType()->getName()
        << "::"
        << irManager->getMethodDesc().getName()
        <<" - "<<title
        << "\";" << ::std::endl;

}

//_________________________________________________________________________________________________
void IRDotPrinter::printEnd(U_32 indent)
{
    getStream() << "}" << ::std::endl;
}

//_________________________________________________________________________________________________
void IRDotPrinter::printLiveness()
{
    ::std::ostream& out=getStream();

    out<<"subgraph cluster_liveness {"<<::std::endl;
    out<<"label=liveness"<<::std::endl;
    const Nodes& nodes = irManager->getFlowGraph()->getNodesPostOrder();
    for (Nodes::const_reverse_iterator it = nodes.rbegin(), end = nodes.rend(); it!=end; ++it) {
        Node* node = *it;
        const BitSet * ls=irManager->getLiveAtEntry(node);
        out<<"liveness_"; printNodeName(node); 
        out << " [label=\""; printNodeName(node); out<<":";
        if (ls){
            for (U_32 i = 0; i < ls->getSetSize(); i++) {
                if (ls->getBit(i))
                    out << " " << i;
            }
        }else
            out<<" UNKNOWN";
        out <<"\"]"; out<<::std::endl;
    }

    Node * lastNode=0;
    for (Nodes::const_reverse_iterator it = nodes.rbegin(), end = nodes.rend(); it!=end; ++it) {
        Node* node = *it;
        if (lastNode){
            out<<"liveness_"; printNodeName(lastNode);
            out<<" -> ";
            out<<"liveness_"; printNodeName(node); 
            out<<";"<<::std::endl;
        }
        lastNode=node;
    }

    out<<"}"<<std::endl;
}

//_________________________________________________________________________________________________
void IRDotPrinter::printTraversalOrder(CGNode::OrderType orderType)
{
    std::ostream& out=getStream();

    const char* prefix = NULL;
    Nodes nodes(irManager->getMemoryManager());
    switch(orderType) {
        case CGNode::OrderType_Topological: 
            {
                prefix = "topological";
                const Nodes& postOrder = irManager->getFlowGraph()->getNodesPostOrder();
                nodes.insert(nodes.end(), postOrder.rbegin(), postOrder.rend());
            }
            break;
        case CGNode::OrderType_Postorder:
            {
                prefix = "postorder";
                const Nodes& postOrder = irManager->getFlowGraph()->getNodesPostOrder();
                nodes.insert(nodes.end(), postOrder.begin(), postOrder.end());
            }
            break;
        case CGNode::OrderType_Layout:
            {
                assert(irManager->isLaidOut());
                prefix = "layout";
                for (BasicBlock* bb = (BasicBlock*)irManager->getFlowGraph()->getEntryNode(); bb!=NULL; bb = bb->getLayoutSucc()) {
                    nodes.push_back(bb);    
                }
            }
            break;
        default: 
            {
                assert(orderType == CGNode::OrderType_Arbitrary);
                prefix = "arbitrary";
                const Nodes& arbitrary = irManager->getFlowGraph()->getNodes();
                nodes.insert(nodes.end(), arbitrary.begin(), arbitrary.end());
            }
            break;
    }
    
    out<<"subgraph cluster_"<<prefix<<" {"<<std::endl;
    out<<"label="<<prefix<<std::endl;

    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        out<<prefix<<"_"; printNodeName(node); out<<"[label="; printNodeName(node); out<<"]"<<std::endl;
    }

    Node * prevNode=NULL;
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (prevNode){
            out<<prefix<<"_"; printNodeName(prevNode);
            out<<" -> ";
            out<<prefix<<"_"; printNodeName(node); 
            out<<";"<<std::endl;
        }
        prevNode=node;
    }
    out<<"}"<<std::endl;
}

//_________________________________________________________________________________________________
void IRDotPrinter::printBody(U_32 indent)
{
    assert(irManager!=NULL);

    printTraversalOrder(CGNode::OrderType_Topological);
    printTraversalOrder(CGNode::OrderType_Postorder);
    if (irManager->isLaidOut())
        printTraversalOrder(CGNode::OrderType_Layout);
    printCFG(0);
    printLiveness();

}

//_________________________________________________________________________________________________
void IRDotPrinter::printCFG(U_32 indent)
{
    assert(irManager!=NULL);
    const Nodes& nodes = irManager->getFlowGraph()->getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) { 
        Node* node = *it;
        printNode(node);
    }
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        const Edges& edges =node->getOutEdges();
        for (Edges::const_iterator ite = edges.begin(), ende = edges.end(); ite!=ende; ++ite) {
            Edge* e = *ite;
            printEdge(e);
        }
    }

    if (irManager->isLaidOut()){
        for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
            Node* node = *it;
            if (node->isBlockNode()) {
                BasicBlock * from=(BasicBlock*)node;
                BasicBlock * to=from->getLayoutSucc();
                if (to!=NULL) {
                    printLayoutEdge(from, to);
                }
            }
        }
    }

}

//_________________________________________________________________________________________________
void IRDotPrinter::print(U_32 indent)
{
    IRPrinter::print();
}

//_________________________________________________________________________________________________

//========================================================================================
// class IRLivenessDotPrinter
//========================================================================================

//_________________________________________________________________________________________________
void IRLivenessDotPrinter::printBody(U_32 indent)
{
    assert(irManager!=NULL);
    setOpndFlavor(OpndFlavor_Location);
    printCFG(0);
}

//_________________________________________________________________________________________________
char * IRLivenessDotPrinter::getRegString(char * str, Constraint c, StlVector<Opnd *> opnds)
{
    char * retStr=NULL;
    U_32 si=0;
    for (U_32 i=0, n=(U_32)opnds.size(); i<n; i++){
        Opnd * o=opnds[i];
        if (o->isPlacedIn(c)){
            retStr=str;
            RegName r=o->getRegName();
            str[si++]=(char)('0'+getRegIndex(r));
        }else
            str[si++]='_';
        for (U_32 j=0, l=getOpndNameLength(o)-1; j<l; j++) 
            str[si++]='_';
    }
    str[si++]=0;
    return retStr;
}

//_________________________________________________________________________________________________
void IRLivenessDotPrinter::printNode(const Node * node)
{
    ::std::ostream& out=getStream();
    MemoryManager mm("IRLivenessDotPrinter::printNode");
    printNodeName(node); 
    out << " [label=\"";

    BasicBlock * bb=node->isBlockNode()?(BasicBlock*)node:NULL;
    if (bb!=NULL) out << "{";
    printNodeName(node);
    if (((CGNode*)node)->getPersistentId()!=UnknownId)
        out << " pid: " << ((CGNode*)node)->getPersistentId() << " ";
    if(node->getExecCount() > 0)
        out << " ec:" << node->getExecCount() << " ";

    LoopTree* lt = irManager->getFlowGraph()->getLoopTree();
    if (lt->isValid() && lt->getLoopDepth(node)!=0) {
        out << "  Loop: Depth=" << lt->getLoopDepth(node);
        if (lt->isLoopHeader(node)) {
            out << ", hdr, hdr= ";
        }  else {
            out << ", !hdr, hdr=";
        }   
        printNodeName(lt->getLoopHeader(node));
    }

    if (bb!=NULL && irManager->getCodeStartAddr()!=NULL){
        out<<", code="<<(void*)bb->getCodeStartAddr();
    }

    if (bb!=NULL){
        if (irManager->hasLivenessInfo()){

            StlVector<BitSet *> liveSets(mm);
            StlVector<U_32> regUsages(mm);
            BitSet * lsAll=new (mm) BitSet(mm, irManager->getOpndCount());

            BitSet * lsCurrent=new (mm) BitSet(mm, irManager->getOpndCount());
            irManager->getLiveAtExit(node, *lsCurrent);
            lsAll->copyFrom(*lsCurrent);
            BitSet * ls=new (mm) BitSet(mm, irManager->getOpndCount());
            ls->copyFrom(*lsCurrent);
            liveSets.push_back(ls);

            U_32 regUsage=0, regUsageAll=0;
            irManager->getRegUsageAtExit(node, OpndKind_GPReg, regUsage);
            regUsageAll|=regUsage;
            regUsages.push_back(regUsage);

            for (Inst * inst = (Inst*)bb->getLastInst(); inst != NULL; inst = inst->getPrevInst()) {
                irManager->updateLiveness(inst, *lsCurrent);
                lsAll->unionWith(*lsCurrent);
                ls=new (mm) BitSet(mm, irManager->getOpndCount());
                ls->copyFrom(*lsCurrent);
                liveSets.push_back(ls);

                irManager->updateRegUsage(inst, OpndKind_GPReg, regUsage);
                regUsageAll|=regUsage;
                regUsages.push_back(regUsage);
            }

#ifdef _DEBUG
            BitSet* entrySet = irManager->getLiveAtEntry(node);
            if (!entrySet->isEqual(*lsCurrent)) {
                if (node == irManager->getFlowGraph()->getEntryNode() 
                    && ((POINTER_SIZE_INT*)irManager->getInfo(STACK_INFO_KEY))!=NULL) 
                {
                    //DETAILS: callee save regs has no defs. See IRManager.verifyLiveness for details.
                    //This is why we skip liveness verification for entry node here after stacklayouter pass.
                    //TODO: we can add pseudo-defs for callee save regs to EntryPointPseudoInst
                } else {
                    assert(false);
                }
            }
#endif

            StlVector<Opnd *> opndsAll(mm);

            for (U_32 i=0, n=irManager->getOpndCount(); i<n; i++){
                Opnd * opnd=irManager->getOpnd(i);
                if (lsAll->getBit(opnd->getId())) {
                    opndsAll.push_back(opnd);
                }
            }
            char * regStrings[IRMaxRegKinds]={ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
/* VSH: TODO - reg kinds are masks now, not indexes !           
*/

            out << "\\l|{\\" << ::std::endl;

            out<<"Operand Ids:\\l\\"<<::std::endl;

            U_32 regKindCount=0;
            for (U_32 i=0; i<IRMaxRegKinds; i++){
                if (regStrings[i]!=NULL){
                    regKindCount++;
                    out<<getOpndKindString((OpndKind)i) << "\\l\\" << ::std::endl;
                }
            }

            for (Inst * inst = (Inst*)bb->getFirstInst(); inst != NULL; inst = inst->getNextInst()) {
                printInst(inst); out<<"\\l\\"<<std::endl;
            }

            out << "|\\" << std::endl;
            for (U_32 i=0, n=(U_32)opndsAll.size(); i<n; i++){
                out<<opndsAll[i]->getFirstId()<<'_';
            }
            out << "\\l\\" << std::endl;

            for (U_32 i=0; i<IRMaxRegKinds; i++){
                if (regStrings[i]!=NULL)
                    out << regStrings[i] << "\\l\\" << ::std::endl;
            }

            U_32 idx=(U_32)liveSets.size()-1;

            for (Inst * inst = (Inst*)bb->getFirstInst(); inst != NULL; inst = inst->getNextInst(), idx--) {
                printLivenessForInst(opndsAll, liveSets[idx], liveSets[idx-1]); // output at entry
                out << "\\l\\" << ::std::endl;
            }

            if (regUsageAll!=0){
                out << "|\\" << ::std::endl;

                out << "01234567" << "\\l\\" << std::endl;
                for (U_32 i=1; i<regKindCount; i++)
                    out << "\\l\\" << std::endl;

                for (I_32 i=(I_32)regUsages.size()-1; i>=0; i--) {
                    U_32 regUsage=regUsages[i];
                    for (U_32 m=1; m!=0x100; m<<=1)
                        out << (m&regUsage?'.':'_');
                    out << "\\l\\" << std::endl;
                }
            }

            out << "}\\" << ::std::endl;
        
        }else{
            out<<"liveness info is outdated";
        }
    }
    if (bb!=NULL) out << "}";
    out << "\"";
    if (node->isDispatchNode()) {
        if (node!=irManager->getFlowGraph()->getUnwindNode()) {
            out << ",shape=diamond,color=blue";
        } else {
            out << ",shape=diamond,color=red";
        }
    } else if (node->isExitNode()) {
        out << ",shape=ellipse,color=green";
    }
    out << "]" << ::std::endl;
}

//_________________________________________________________________________________________________
void IRLivenessDotPrinter::printLivenessForInst(const StlVector<Opnd*> opnds, const BitSet * ls0, const BitSet * ls1)
{
    ::std::ostream& out=getStream();
    for (U_32 i=0, n=(U_32)opnds.size(); i<n; i++){
        Opnd * opnd=opnds[i];
        bool isLiveBefore=ls0!=NULL && ls0->getBit(opnd->getId());
        bool isLiveAfter=ls1!=NULL && ls1->getBit(opnd->getId());
        if (isLiveAfter && isLiveBefore)
            out<<'I';
        else if (isLiveAfter)
            out<<'.';
        else if (isLiveBefore)
            out<<'\'';
        else
            out<<'_';
        for (U_32 j=0, l=getOpndNameLength(opnd)-1; j<l; j++) 
            out<<'_';
    }
}



//_________________________________________________________________________________________________
void dumpIR(
            const IRManager * irManager,
            U_32 stageId,
            const char * readablePrefix,
            const char * readableStageName,
            const char * stageTagName,
            const char * subKind1, 
            const char * subKind2,
            U_32 instFilter, 
            U_32 opndFlavor,
            U_32 opndRolesFilter
            )
{
    std::ostream& out = Log::log(LogStream::IRDUMP).out();
    out << "-------------------------------------------------------------" << ::std::endl;

    char title[128];
    strcpy(title, readablePrefix);
    strcat(title, readableStageName);

    char subKind[128];
    assert(subKind1!=NULL);
    strcpy(subKind, subKind1);
    if (subKind2!=NULL && subKind2[0]!=0){
        strcat(subKind, ".");
        strcat(subKind, subKind2);
    }

    Log::printIRDumpBegin(out, stageId, readableStageName, subKind);
    if (subKind2!=0){
        if (stricmp(subKind2, "opnds")==0){
            IROpndPrinter(irManager, title)
                .setInstFilter(instFilter)
                .setStream(out).print();
        }else if (stricmp(subKind2, "inst_constraints")==0){
            IRInstConstraintPrinter(irManager, title)
                .setInstFilter(instFilter)
                .setStream(out).print();
        }else if (stricmp(subKind2, "liveness")==0){
            IRLivenessPrinter(irManager, title)
                .setInstFilter(instFilter)
                .setStream(out).print();
        }
    }else{
        IRPrinter(irManager, title)
            .setInstFilter(instFilter)
            .setStream(out).print();
    }
    Log::printIRDumpEnd(out, stageId, readableStageName, subKind);
}

//_________________________________________________________________________________________________
void printDot(
            const IRManager * irManager,
            U_32 stageId,
            const char * readablePrefix,
            const char * readableStageName,
            const char * stageTagName,
            const char * subKind1, 
            const char * subKind2,
            U_32 instFilter, 
            U_32 opndFlavor,
            U_32 opndRolesFilter
            )
{
    char title[128];
    strcpy(title, readablePrefix);
    strcat(title, readableStageName);

    char subKind[256]; 
    assert(subKind1!=NULL);
    sprintf(subKind, "%.2d.%s.%s", (int)stageId, stageTagName, subKind1);
    if (subKind2!=NULL && subKind2[0]!=0){
        strcat(subKind, ".");
        strcat(subKind, subKind2);
    }

    char* dotfilename = 0;

    if (subKind2!=0){
        if (stricmp(subKind2, "liveness")==0){
            dotfilename = Log::makeDotFileName(subKind);
            IRLivenessDotPrinter(irManager, title)
                .setInstFilter(instFilter)
                .open(dotfilename)
                .print();
        }
    }else{
        dotfilename = Log::makeDotFileName(subKind);
        IRDotPrinter(irManager, title)
            .setInstFilter(instFilter)
            .open(dotfilename)
            .print();
    }

    if (dotfilename != 0)
        delete [] dotfilename;
}

//_________________________________________________________________________________________________
void printRuntimeArgs(::std::ostream& os, U_32 opndCount, CallingConvention::OpndInfo * infos, JitFrameContext * context)
{
    MemoryManager mm("printRuntimeOpndInternalHelper");
    TypeManager tm(mm); tm.init();
    os<<opndCount<<" args: ";
    for (U_32 i=0; i<opndCount; i++){
        CallingConvention::OpndInfo & info=infos[i];
        U_32 cb=0;
        U_8 arg[4*sizeof(U_32)]; 
        for (U_32 j=0; j<info.slotCount; j++){
            if (!info.isReg){
#ifdef _EM64T_
                *(POINTER_SIZE_INT*)(arg+cb)=((POINTER_SIZE_INT*)context->rsp)[info.slots[j]];
#else
                *(U_32*)(arg+cb)=((U_32*)context->esp)[info.slots[j]];
#endif
                cb+=sizeof(U_32);
            }else{
                assert(info.isReg);
                //assert(0);                    
            }   
        }
        if (i>0)
            os<<", ";
        printRuntimeOpnd(os, tm, (Type::Tag)info.typeTag, (const void*)arg);
    }
}

//_________________________________________________________________________________________________
void printRuntimeOpnd(::std::ostream& os, TypeManager & tm, Type::Tag typeTag, const void * p)
{
    if (Type::isObject(typeTag)){
        printRuntimeObjectOpnd(os, tm, *(const void**)p);
    }else{
        tm.getPrimitiveType(typeTag)->print(os); 
        os<<" ";
        switch (typeTag){
            case Type::Int8:
                os<<*(I_8*)p;
                break;
            case Type::UInt8:
                os<<*(U_8*)p;
                break;
            case Type::Boolean:
                os<<(*(I_8*)p?true:false);
                break;
            case Type::Int16:   
                os<<*(int16*)p;
                break;
            case Type::UInt16:
                os<<*(uint16*)p;
                break;
            case Type::Char:
                os<<*(uint16*)p<<*(char*)p;
                break;
            case Type::Int32:
                os<<*(I_32*)p;
                break;
            case Type::UInt32:
                os<<*(U_32*)p;
                break;
            case Type::Int64:   
                os<<*(int64*)p;
                break;
            case Type::UInt64:
                os<<*(uint64*)p;
                break;
            case Type::Double:
            case Type::Float:
                os<<*(double*)p;
                break;
            case Type::Single:
                os<<*(float*)p;
                break;
            default:
                assert(0);
                break;
        }
    }
}

//_________________________________________________________________________________________________
void printRuntimeObjectOpnd(::std::ostream& os, TypeManager & tm, const void * p)
{
//  check for valid range for object pointer
    if (p==NULL){
        os<<"Ref Null";
        return;
    }
    os<<"Ref "<<p;
    if (p<(const void*)0x10000||p>(const void*)0x20000000){ 
        os<<"(INVALID PTR)";
        return;
    }
// check for valid alignment
    if ((((POINTER_SIZE_INT)p)&0x3)!=0){
        os<<"(INVALID PTR)";
        return;
    }
    POINTER_SIZE_INT vtableOffset = VMInterface::getVTableOffset();

    //  assume that vtable pointer in object head is allocation handle 
    void * allocationHandle=*(void**)(((U_8*)p)+vtableOffset);
        
    if (allocationHandle<(void*)0x10000||allocationHandle>(void*)0x20000000||(((POINTER_SIZE_INT)allocationHandle)&0x3)!=0){
        os<<"(INVALID VTABLE PTR: "<<allocationHandle<<")";
        return;
    }

    ObjectType * type=tm.getObjectTypeFromAllocationHandle(allocationHandle);
    if (type==NULL){
        os<<"(UNRECOGNIZED VTABLE PTR: "<<allocationHandle<<")";
        return;
    }
    os<<"(";
    printRuntimeObjectContent(os, tm, type, p);
    os<<":"<<type->getName();
    os<<")";
}

//_________________________________________________________________________________________________
void printRuntimeObjectContent_Array(::std::ostream& os, TypeManager & tm, Type * type, const void * p)
{
    ArrayType * arrayType=(ArrayType *)type;
    U_32 lengthOffset=arrayType->getArrayLengthOffset();
    U_32 length=*(U_32*)(((U_8*)p)+lengthOffset);
    os<<"{"<<length<<" elems: ";
    if (length>0){
        U_32 elemOffset=arrayType->getArrayElemOffset();
        printRuntimeOpnd(os, tm, arrayType->getElementType()->tag, (const void*)(((U_8*)p)+elemOffset));
        if (length>1)
            os<<", ...";
    }
    os<<"}";
}

//_________________________________________________________________________________________________
void printRuntimeObjectContent_String(::std::ostream& os, TypeManager & tm, Type * type, const void * p)
{
#ifndef KNOWN_STRING_FORMAT
    os<<"\"...\"";
    return;
#else
    U_32 stringLengthOffset=8;
    U_32 stringOffsetOffset=12;
    U_32 stringBufferOffset=16;
    U_32 bufferLengthOffset=8;
    U_32 bufferElemsOffset=12;
    
    U_8 * string=(U_8*)p;

    U_32  stringLength=*(U_32*)(string+stringLengthOffset);
    U_32  stringOffset=*(U_32*)(string+stringOffsetOffset);
    U_8 * buffer=*(U_8**)(string+stringBufferOffset);

    if (buffer==NULL){
        if (stringLength==0)
            os<<"\"\"";
        else
            os<<"INVALID STRING";
        return;
    }

    U_32 bufferLength=*(U_32*)(buffer+bufferLengthOffset);

    uint16 * bufferElems=(uint16*)(buffer+bufferElemsOffset);

    if (stringOffset>bufferLength || stringLength>bufferLength || stringLength>bufferLength-stringOffset){
        os<<"INVALID STRING";
        return;
    }

    os<<"\"";
    for (U_32 i=stringOffset, n=stringOffset+stringLength; i<n; i++)
        os<<(char)bufferElems[i];
    os<<"\"";
#endif
}

//_________________________________________________________________________________________________
void printRuntimeObjectContent(::std::ostream& os, TypeManager & tm, Type * type, const void * p)
{
    if (type->isArray()){
        os<<" ";
        printRuntimeObjectContent_Array(os, tm, type, p);       
    }else if (type->isSystemString()){
        os<<" ";
        printRuntimeObjectContent_String(os, tm, type, p);      
    }
}

//_________________________________________________________________________________________________
void __stdcall printRuntimeOpndInternalHelper(const void * p)
{
    MemoryManager mm("printRuntimeOpndInternalHelper");
    TypeManager tm(mm); tm.init();
    printRuntimeObjectOpnd(::std::cout, tm, p);
}


}}; //namespace Ia32


