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

#include "Ia32InternalTrace.h"
#include "Log.h"
#include "Ia32Printer.h"
#include "Ia32GCMap.h"

namespace Jitrino
{
namespace Ia32{

//========================================================================================
// class InternalTrace
//========================================================================================

static ActionFactory<InternalTrace> _itrace("itrace");

//_________________________________________________________________________________________________
static inline void m_assert(bool cond)  {
#ifdef _DEBUG
        assert(cond);
#else
    #ifdef _WIN32 // any windows
        if(!cond) {
            DebugBreak();
        }
    #endif
#endif    
    }

static MemoryManager mm("printRuntimeOpndInternalHelper");
static TypeManager *tm = NULL;
void __stdcall methodEntry(const char * methodName, U_32 argInfoCount, CallingConvention::OpndInfo * argInfos)
{

    JitFrameContext context;
#ifdef _EM64T_
    context.rsp=(POINTER_SIZE_INT)(&methodName+sizeof(POINTER_SIZE_INT)); // must point to the beginning of incoming stack args
#else
    context.esp=(POINTER_SIZE_INT)(&methodName+sizeof(POINTER_SIZE_INT)); // must point to the beginning of incoming stack args
#endif
    ::std::ostream & os=Log::cat_rt()->out(); 
    os<<"__METHOD_ENTRY__:"<<methodName<<::std::endl;
    os<<"\t(";
    printRuntimeArgs(os, argInfoCount, argInfos, &context);
    os<<")"<<::std::endl;

    if (tm == NULL) {
        tm = new (mm) TypeManager(mm); tm->init();
    }
    for (U_32 i=0; i<argInfoCount; i++){
        CallingConvention::OpndInfo & info=argInfos[i];
        U_32 cb=0;
        U_8 arg[4*sizeof(U_32)]; 
        for (U_32 j=0; j<info.slotCount; j++){
            if (!info.isReg){
#ifdef _EM64T_
                *(POINTER_SIZE_INT*)(arg+cb)=((POINTER_SIZE_INT*)context.rsp)[info.slots[j]];
#else
                *(U_32*)(arg+cb)=((U_32*)context.esp)[info.slots[j]];
#endif
                cb+=sizeof(U_32);
            }else{
                m_assert(info.isReg);
                m_assert(0);                    
            }   
        }
        if (Type::isObject((Type::Tag)info.typeTag)){
            GCMap::checkObject(*tm, *(const void**)(const void*)arg);
        }
    }
}

//____________________________ _____________________________________________________________________
void __stdcall methodExit(const char * methodName)
{
    ::std::ostream & os=Log::cat_rt()->out();
    os<<"__METHOD_EXIT__:"<<methodName<<::std::endl;
}

//_________________________________________________________________________________________________
void __stdcall fieldWrite(const void * address)
{   
    ::std::ostream & os=Log::cat_rt()->out();
    os<<"__FIELD_WRITE__:"<<address<<" at "<<*(void**)(&address-1)<<::std::endl;
    void * heapBase = VMInterface::getHeapBase();
    void * heapCeiling = VMInterface::getHeapCeiling();
    if (address<heapBase || address>=heapCeiling){
        os<<"PROBABLY STATIC OR INVALID ADDRESS. DYNAMIC ADDRESSES MUST BE IN ["<<heapBase<<","<<heapCeiling<<")"<<::std::endl;
    }
}   

//_________________________________________________________________________________________________
void InternalTrace::runImpl()
{
    MemoryManager mm("InternalTrace");

    irManager->registerInternalHelperInfo("itrace_method_entry", IRManager::InternalHelperInfo((void*)&methodEntry, &CallingConvention_STDCALL));
    irManager->registerInternalHelperInfo("itrace_method_exit", IRManager::InternalHelperInfo((void*)&methodExit, &CallingConvention_STDCALL));
    irManager->registerInternalHelperInfo("itrace_field_write", IRManager::InternalHelperInfo((void*)&fieldWrite, &CallingConvention_STDCALL));

    char methodFullName[0x1000]="";
    MethodDesc & md=irManager->getMethodDesc();
    strcat(methodFullName, md.getParentType()->getName());
    strcat(methodFullName, ".");
    strcat(methodFullName, md.getName());
    strcat(methodFullName, " ");
    strcat(methodFullName, md.getSignatureString());

    Opnd * methodNameOpnd=irManager->newInternalStringConstantImmOpnd(methodFullName);
    irManager->setInfo("itraceMethodExitString", methodNameOpnd->getRuntimeInfo()->getValue(0));

    Node* prolog=irManager->getFlowGraph()->getEntryNode();

    Inst * inst=(Inst*)prolog->getFirstInst();
    if (inst->hasKind(Inst::Kind_EntryPointPseudoInst)){
        EntryPointPseudoInst * entryPointPseudoInst = (EntryPointPseudoInst *)inst;
        entryPointPseudoInst->getCallingConventionClient().finalizeInfos(Inst::OpndRole_Def, CallingConvention::ArgKind_InArg);
        const StlVector<CallingConvention::OpndInfo> & infos=((const EntryPointPseudoInst *)entryPointPseudoInst)->getCallingConventionClient().getInfos(Inst::OpndRole_Def);
        Opnd * argInfoOpnd=irManager->newBinaryConstantImmOpnd((U_32)infos.size()*sizeof(CallingConvention::OpndInfo), &infos.front());
        Opnd * args[3]={ methodNameOpnd, 
            irManager->newImmOpnd(irManager->getTypeManager().getInt32Type(), infos.size()), 
            argInfoOpnd,
        };
        Inst * internalTraceInst=irManager->newInternalRuntimeHelperCallInst("itrace_method_entry", 3, args, NULL);
        internalTraceInst->insertBefore(inst);
    }else{
        Opnd * args[3]={ methodNameOpnd, 
            irManager->newImmOpnd(irManager->getTypeManager().getInt32Type(), 0), 
            irManager->newImmOpnd(irManager->getTypeManager().getUnmanagedPtrType(irManager->getTypeManager().getIntPtrType()), 0),
        };
        Inst * internalTraceInst=irManager->newInternalRuntimeHelperCallInst("itrace_method_entry", 3, args, NULL);
        prolog->prependInst(internalTraceInst);
    }

    const Edges& inEdges = irManager->getFlowGraph()->getExitNode()->getInEdges();
    for (Edges::const_iterator ite = inEdges.begin(), ende = inEdges.end(); ite!=ende; ++ite) {
        Edge* edge = *ite;
        if (irManager->isEpilog(edge->getSourceNode())) {
            Node* epilog = edge->getSourceNode();
            Inst * retInst=(Inst*)epilog->getLastInst();
            assert(retInst->hasKind(Inst::Kind_RetInst));
            Opnd * args[1]={ methodNameOpnd };
            Inst * internalTraceInst=irManager->newInternalRuntimeHelperCallInst("itrace_method_exit", 1, args, NULL);
            internalTraceInst->insertBefore(retInst);
        }
    }

#ifdef IA32_EXTENDED_TRACE
    const Nodes& nodes = irManager->getFlowGraph()->getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (node->isBlockNode()){
            for (Inst * inst=(Inst*)node->getFirstInst(); inst!=NULL; inst=inst->getNextInst()){
                if (inst->getMnemonic()==Mnemonic_MOV){
                    Opnd * dest=inst->getOpnd(0);
                    if (dest->isPlacedIn(OpndKind_Memory) && dest->getMemOpndKind()==MemOpndKind_Heap){
                        Opnd * opnd=irManager->newOpnd(Type::UnmanagedPtr);
                        Inst* newIns = irManager->newInst(Mnemonic_LEA, opnd, dest);
                        newIns->insertBefore(inst);
                        Opnd * args[1]={ opnd };
                        newIns = irManager->newInternalRuntimeHelperCallInst("itrace_field_write", 1, args, NULL);
                        newIns->insertBefore(inst);
                    }
                }
            }
        }
    }
#endif
}

}}; //namespace Ia32


