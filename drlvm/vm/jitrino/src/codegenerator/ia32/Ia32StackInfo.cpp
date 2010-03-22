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
#include "Stl.h"
#include "Ia32StackInfo.h"
#include "Ia32RuntimeInterface.h"
#include "Ia32InternalTrace.h"
#include <math.h>


namespace Jitrino
{
namespace Ia32 {

//========================================================================================
//  class StackInfoInstRegistrar
//========================================================================================
/**
 *  class StackInfoInstRegistrar performs calling of StackInfo method which
 *  performes saving information about call instructions for stack unwinding
 *
 *  This transformer ensures that it calls the proper method
 *
 *  This transformer must inserted after Code Emitter, because it needs
 *  EIP value for instructions.
 */

class StackInfoInstRegistrar : public SessionAction {
    void runImpl()
    { 
        StackInfo * stackInfo = (StackInfo*)irManager->getInfo(STACK_INFO_KEY);
        assert(stackInfo!=NULL);
        stackInfo->registerInsts(*irManager);
        stackInfo->setMethodExitString(*irManager);
    }
    U_32 getNeedInfo()const{ return 0; }
    U_32 getSideEffects()const{ return 0; }
    bool isIRDumpEnabled(){ return false; }
};


static ActionFactory<StackInfoInstRegistrar> _si_insts("si_insts");

#define CALL_MIN_SIZE 2
#define CALL_MAX_SIZE 6

POINTER_SIZE_INT hashFunc(POINTER_SIZE_INT key, U_32 hashTableSize) {
    return (key % hashTableSize);
}

    StackDepthInfo hashGet(DepthEntry** entries, POINTER_SIZE_INT key, U_32 hashTableSize) {
        DepthEntry * e = entries[hashFunc(key,hashTableSize)];
        assert(e);
        for(;e !=NULL;) {
            if(e->eip == key) 
                return e->info;
            else
                e = e->next;
            assert(e);
        }
        return e->info;
    }

    void hashSet(DepthEntry** entries, POINTER_SIZE_INT eip, U_32 hashTableSize,StackDepthInfo info, MemoryManager& mm) {
        POINTER_SIZE_INT key = hashFunc(eip,hashTableSize);
        DepthEntry * e = entries[key];
        if(!e) {
            entries[key] = new(mm) DepthEntry(eip, info, NULL);
        } else {
            for(;e->next!=NULL;e = e->next) ;
            e->next =  new(mm) DepthEntry(eip, info, NULL);
        }
    }

POINTER_SIZE_INT StackInfo::getByteSize() const     {
    return byteSize ? 
            byteSize : 
            (sizeof(StackInfo)+(stackDepthInfo?stackDepthInfo->size()*sizeof(DepthEntry):0))+hashTableSize*sizeof(POINTER_SIZE_INT);
}

POINTER_SIZE_INT StackInfo::getByteSize(MethodDesc* md) {
    U_8* data =  md->getInfoBlock();
    return *(POINTER_SIZE_INT*)data;
}

POINTER_SIZE_INT StackInfo::readByteSize(const U_8* bytes) const{
    POINTER_SIZE_INT* data = (POINTER_SIZE_INT *) bytes;
    POINTER_SIZE_INT sizeInBytes = *data;

    return sizeInBytes;
}

typedef DepthEntry * EntryPtr;

void StackInfo::write(U_8* bytes) {
    U_8* data = bytes;
    StackInfo* serializedInfo = (StackInfo*)data;
    *serializedInfo = *this;
    serializedInfo->byteSize = getByteSize();
    data+=sizeof(StackInfo);
    MemoryManager mm("DepthInfo");
    EntryPtr * entries = new(mm) EntryPtr[hashTableSize];
    for(U_32 i = 0; i< hashTableSize; i++)
        entries[i] = NULL;
    for(DepthMap::iterator dmit = stackDepthInfo->begin(); dmit != stackDepthInfo->end(); dmit++) {
        hashSet(entries, dmit->first, hashTableSize, dmit->second, mm);
    }
    U_8* next = data + hashTableSize * sizeof(POINTER_SIZE_INT);
    for(U_32 i = 0; i< hashTableSize; i++) {
        DepthEntry * e = entries[i];
        POINTER_SIZE_INT serializedEntryAddr = 0;
        if(entries[i]) {
            serializedEntryAddr = (POINTER_SIZE_INT)next;
            for(; e != NULL; e = e->next) {
                DepthEntry* serialized = (DepthEntry*)next;
                *serialized = *e;
                next+=sizeof(DepthEntry);
                serialized->next = e->next ? (DepthEntry*)next : NULL;
            }
        }
        *((POINTER_SIZE_INT*)data)= serializedEntryAddr;
        data+=sizeof(POINTER_SIZE_INT);
    }
    assert(getByteSize() == (POINTER_SIZE_INT) (((U_8*)next) - bytes));
}

DepthEntry * getHashEntry(U_8* data, POINTER_SIZE_INT eip, U_32 size) 
{
    if(!size)
        return NULL;
    POINTER_SIZE_INT key = hashFunc(eip,size);
    data += sizeof(StackInfo) + key * sizeof(POINTER_SIZE_INT);
    DepthEntry * entry = (DepthEntry *)*(POINTER_SIZE_INT*)data;
    for(;entry && entry->eip != eip; entry = entry->next) {
        assert(entry!=entry->next);
    }
    return entry;
}

void StackInfo::read(MethodDesc* pMethodDesc, POINTER_SIZE_INT eip, bool isFirst) {
    U_8* data = pMethodDesc->getInfoBlock();
    byteSize = ((StackInfo *)data)->byteSize;
    hashTableSize = ((StackInfo *)data)->hashTableSize;
    frameSize = ((StackInfo *)data)->frameSize;
    icalleeOffset = ((StackInfo *)data)->icalleeOffset;
    icalleeMask = ((StackInfo *)data)->icalleeMask;
    localOffset = ((StackInfo *)data)->localOffset;
    offsetOfThis = ((StackInfo *)data)->offsetOfThis;
    itraceMethodExitString = ((StackInfo *)data)->itraceMethodExitString;
    soeCheckAreaOffset = ((StackInfo *)data)->soeCheckAreaOffset;
    
    if (!isFirst){
        DepthEntry * entry = getHashEntry(data, eip, hashTableSize);
        assert(entry);
        if (!entry){
            if (Log::cat_rt()->isEnabled())
                Log::cat_rt()->out()<<"eip=0x"<<(void*)eip<<" not found during stack unwinding!"<<::std::endl;
            ::std::cerr<<"eip=0x"<<(void*)eip<<" not found during stack unwinding!"<<::std::endl;
        }
        calleeSaveRegsMask = entry->info.calleeSaveRegs;
        stackDepth = entry->info.stackDepth;
    }else{
        assert(eip >= (POINTER_SIZE_INT)pMethodDesc->getCodeBlockAddress(0));
        POINTER_SIZE_INT eipOffset = eip - (POINTER_SIZE_INT)pMethodDesc->getCodeBlockAddress(0);
        assert(fit32(eipOffset));
        if (eipOffset <= soeCheckAreaOffset) {
            stackDepth = 0; //0 depth -> stack overflow error
        } else {
            DepthEntry * entry = NULL;
            U_32 i = CALL_MIN_SIZE;
            for (; i<= CALL_MAX_SIZE; i++) {
                entry = getHashEntry(data, eip + i, hashTableSize);
                if(entry)
                    break;
            }
            if (entry && (entry->info.callSize == i)) {
                stackDepth = entry->info.stackDepth; //call site's depth
            } else {
                stackDepth = frameSize; //hardware NPE's depth
            }
        }

        if (Log::cat_rt()->isEnabled())
            Log::cat_rt()->out()<<"Hardware exception from eip=0x"<<(void*)eip<<::std::endl;
    }
}


void StackInfo::unwind(MethodDesc* pMethodDesc, JitFrameContext* context, bool isFirst) const {
   

    POINTER_SIZE_INT offset_step = sizeof(POINTER_SIZE_INT);
#ifdef _EM64T_
    if (itraceMethodExitString!=NULL){
        Log::cat_rt()->out()<<"__UNWIND__:"
            <<(itraceMethodExitString!=(const char*)1?itraceMethodExitString:"")
            <<"; unwound from EIP="<<(void*)*context->p_rip
            <<::std::endl;
    }

    context->rsp += stackDepth;
    assert((context->rsp & 0xf) == 0x8);

    POINTER_SIZE_INT offset = context->rsp;
    context->p_rip = (POINTER_SIZE_INT *) (offset);

    offset += icalleeOffset;

    if(stackDepth != 0) {
        if(getRegMask(RegName_R15) & icalleeMask) {
            context->p_r15 = (POINTER_SIZE_INT *)  offset;
            offset += offset_step;
        }
        if(getRegMask(RegName_R14) & icalleeMask) {
            context->p_r14 = (POINTER_SIZE_INT *)  offset;
            offset += offset_step;
        }
        if(getRegMask(RegName_R13) & icalleeMask) {
            context->p_r13 = (POINTER_SIZE_INT *)  offset;
            offset += offset_step;
        }
        if(getRegMask(RegName_R12) & icalleeMask) {
            context->p_r12 = (POINTER_SIZE_INT *)  offset;
            offset += offset_step;
        }
        if(getRegMask(RegName_RDI) & icalleeMask) {
            context->p_rdi = (POINTER_SIZE_INT *)  offset;
            offset += offset_step;
        }
        if(getRegMask(RegName_RSI) & icalleeMask) {
            context->p_rsi = (POINTER_SIZE_INT *)  offset;
            offset += offset_step;
        }
        if(getRegMask(RegName_RBP) & icalleeMask) {
            context->p_rbp = (POINTER_SIZE_INT *)  offset;
            offset += offset_step;
        }
        if(getRegMask(RegName_RBX) & icalleeMask) {
            context->p_rbx = (POINTER_SIZE_INT *)  offset;
        }
    }
    context->rsp += offset_step; //IP register size
#else
    if (itraceMethodExitString!=NULL){
        Log::cat_rt()->out()<<"__UNWIND__:"
            <<(itraceMethodExitString!=(const char*)1?itraceMethodExitString:"")
            <<"; unwound from EIP="<<(void*)*context->p_eip
            <<::std::endl;
    }

    context->esp += stackDepth;

    U_32 offset = context->esp;
    context->p_eip = (POINTER_SIZE_INT *) offset;

    offset += icalleeOffset;
    if(stackDepth != 0) {
        if(getRegMask(RegName_EDI) & icalleeMask) {
            context->p_edi = (POINTER_SIZE_INT *)  offset;
            offset += offset_step;
        }
        if(getRegMask(RegName_ESI) & icalleeMask) {
            context->p_esi = (POINTER_SIZE_INT *)  offset;
            offset += offset_step;
        }
        if(getRegMask(RegName_EBP) & icalleeMask) {
            context->p_ebp = (POINTER_SIZE_INT *)  offset;
            offset += offset_step;
        }
        if(getRegMask(RegName_EBX) & icalleeMask) {
            context->p_ebx = (POINTER_SIZE_INT *)  offset;
        }
    }
    context->esp += offset_step; //IP register size
#endif
}

POINTER_SIZE_INT* StackInfo::getRegOffset(const JitFrameContext* context, RegName reg) const
{
    assert(stackDepth >=0);
    if(calleeSaveRegsMask & getRegMask(reg)) { //callee save regs
        //regnames are hardcoded -> unwind() has hardcoded regs too.
        //return register offset for previous stack frame. 
        //MUST be called before unwind()
        switch(reg) {
#ifdef _EM64T_
            case RegName_R15:
                return context->p_r15;
            case RegName_R14:
                return context->p_r14;
            case RegName_R13:
                return context->p_r13;
            case RegName_R12:
                return context->p_r12;
            case RegName_RBP:
                return context->p_rbp;
            case RegName_RBX:
                return context->p_rbx;
#ifdef _WIN64
            case RegName_RSI:
                return context->p_rsi;
            case RegName_RDI:
                return context->p_rdi;
#endif
#else
            case RegName_ESI:
                return context->p_esi;
            case RegName_EDI:
                return context->p_edi;
            case RegName_EBP:
                return context->p_ebp;
            case RegName_EBX:
                return context->p_ebx;
#endif
        default:
        assert(0);
        return NULL;
        }
    } else { //caller save regs
        assert(0);
        VERIFY_OUT("Caller save register requested!");
        exit(1);
    }
}

void StackInfo::fixHandlerContext(JitFrameContext* context)
{
#ifdef _EM64T_
    if (itraceMethodExitString!=NULL){
        Log::cat_rt()->out()<<"__CATCH_HANDLER__:"
            <<(itraceMethodExitString!=(const char*)1?itraceMethodExitString:"")
            <<"; unwound from EIP="<<(void*)*context->p_rip
            <<::std::endl;
    }
    context->rsp -= frameSize - stackDepth;
#else
    if (itraceMethodExitString!=NULL){
        Log::cat_rt()->out()<<"__CATCH_HANDLER__:"
            <<(itraceMethodExitString!=(const char*)1?itraceMethodExitString:"")
            <<"; unwound from EIP="<<(void*)*context->p_eip
            <<::std::endl;
    }
    context->esp -= frameSize - stackDepth;
#endif
}

void StackInfo::registerInsts(IRManager& irm) 
{
    MethodDesc& md = irm.getMethodDesc();
    if (!md.isStatic()) {
#ifdef _EM64T_
        if ((md.isSynchronized() || md.isParentClassIsLikelyExceptionType())) {
            EntryPointPseudoInst * entryPointInst = irm.getEntryPointInst();
            offsetOfThis = (U_32)entryPointInst->thisOpnd->getMemOpndSubOpnd(MemOpndSubOpndKind_Displacement)->getImmValue();
        } else {
            offsetOfThis = 0;
        }
#else
        EntryPointPseudoInst * entryPointInst = irm.getEntryPointInst();
        offsetOfThis = (U_32)entryPointInst->getOpnd(0)->getMemOpndSubOpnd(MemOpndSubOpndKind_Displacement)->getImmValue();
#endif
    }
    const Nodes& nodes = irm.getFlowGraph()->getNodes();
    for (Nodes::const_iterator it = nodes.begin(), end = nodes.end(); it!=end; ++it) {
        Node* node = *it;
        if (node->isBlockNode()){
            for (Inst * inst=(Inst*)node->getFirstInst(); inst!=NULL; inst=inst->getNextInst()){
                if(inst->getMnemonic() == Mnemonic_CALL) {
                    (*stackDepthInfo)[(POINTER_SIZE_INT)inst->getCodeStartAddr()+inst->getCodeSize()]=
                        StackDepthInfo(((CallInst *)inst)->getCallingConventionClient().getCallingConvention()->getCalleeSavedRegs(OpndKind_GPReg).getMask(), 
                        inst->getStackDepth(),
                        inst->getCodeSize());
                } 
            }
        }
    }
    hashTableSize = (U_32)stackDepthInfo->size();
}

void StackInfo::setMethodExitString(IRManager& irm)
{
    ConstantAreaItem * cai=(ConstantAreaItem *)irm.getInfo("itraceMethodExitString");
    if (cai!=NULL){
        itraceMethodExitString=(const char*)cai->getAddress();
        if (itraceMethodExitString==NULL)
            itraceMethodExitString=(const char*)1;
    }
}


}} //namespace

