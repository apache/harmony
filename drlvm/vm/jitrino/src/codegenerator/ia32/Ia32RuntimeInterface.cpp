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
#include "Ia32RuntimeInterface.h"
#include "Ia32StackInfo.h"
#include "Ia32GCMap.h"
#include "Ia32BCMap.h"

namespace Jitrino
{
namespace Ia32{

void RuntimeInterface::unwindStack(MethodDesc* methodDesc, JitFrameContext* context, bool isFirst) {
    StackInfo stackInfo;
#ifdef _EM64T_
    stackInfo.read(methodDesc, *context->p_rip, isFirst);
#else
    stackInfo.read(methodDesc, *context->p_eip, isFirst);
#endif
    stackInfo.unwind(methodDesc, context, isFirst);
}

bool RuntimeInterface::isSOEArea(MethodDesc* methodDesc, const ::JitFrameContext* context, bool isFirst) {
#ifdef _EM64T_
    POINTER_SIZE_INT eip = *context->p_rip;
#else
    POINTER_SIZE_INT eip = *context->p_eip;
#endif
    StackInfo stackInfo;
    stackInfo.read(methodDesc, eip, isFirst);
    assert(eip >= (POINTER_SIZE_INT)methodDesc->getCodeBlockAddress(0));
    POINTER_SIZE_INT eipOffset = eip - (POINTER_SIZE_INT)methodDesc->getCodeBlockAddress(0);
    assert(fit32(eipOffset));
    return eipOffset<=stackInfo.getSOECheckAreaOffset();
}

void* RuntimeInterface::getAddressOfThis(MethodDesc * methodDesc, const JitFrameContext* context, bool isFirst) {
    assert(!methodDesc->isStatic());
    if (!methodDesc->isSynchronized() &&  !methodDesc->isParentClassIsLikelyExceptionType()) {
        static const uint64 default_this=0;
        return (void*)&default_this;
    }
    assert(context);
    StackInfo stackInfo;
#ifdef _EM64T_
    stackInfo.read(methodDesc, *context->p_rip, isFirst);
    assert(isFirst || (POINTER_SIZE_INT)context->p_rip+8 == context->rsp);
    return (void *)(context->rsp + stackInfo.getStackDepth() + (int)stackInfo.getOffsetOfThis());
#else
    stackInfo.read(methodDesc, *context->p_eip, isFirst);
    assert(isFirst || (U_32)context->p_eip+4 == context->esp);
    //assert(stackInfo.getStackDepth()==0 || !isFirst);
    return (void *)(context->esp + stackInfo.getStackDepth() + stackInfo.getOffsetOfThis());
#endif
}

void  RuntimeInterface::fixHandlerContext(MethodDesc* methodDesc, JitFrameContext* context, bool isFirst)
{
    StackInfo stackInfo;
#ifdef _EM64T_
    stackInfo.read(methodDesc, *context->p_rip, isFirst);
#else
    stackInfo.read(methodDesc, *context->p_eip, isFirst);
#endif
    stackInfo.fixHandlerContext(context);
}

bool RuntimeInterface::getBcLocationForNative(MethodDesc* method, POINTER_SIZE_INT native_pc, uint16 *bc_pc)
{
    StackInfo stackInfo;

    U_8* infoBlock = method->getInfoBlock();
    if (infoBlock == NULL) {
        assert(0 && "missing info block for method");
        return false;
    }
    POINTER_SIZE_INT stackInfoSize = stackInfo.readByteSize(infoBlock);
    POINTER_SIZE_INT gcMapSize = GCMap::readByteSize(infoBlock + stackInfoSize);

    POINTER_SIZE_INT nativeOffset = (POINTER_SIZE_INT)native_pc - (POINTER_SIZE_INT)method->getCodeBlockAddress(0);
    assert(nativeOffset<=1024*1024*1024);//extra check: we do not generate such a large methods..
    uint16 bcOffset = BcMap::get_bc_offset_for_native_offset((U_32)nativeOffset, infoBlock + stackInfoSize + gcMapSize);
    if (bcOffset != ILLEGAL_BC_MAPPING_VALUE) {
        *bc_pc = bcOffset;
        return true;
    } 
    if (Log::isLogEnabled(LogStream::RT)) {
        Log::log(LogStream::RT) << "Bytecode location for method: "<<method->getName()<<" IP = " << native_pc << " not found " << std::endl;
    }
    return false;
}
bool RuntimeInterface::getNativeLocationForBc(MethodDesc* method, uint16 bc_pc, POINTER_SIZE_INT *native_pc) {
    StackInfo stackInfo;

    U_8* infoBlock = method->getInfoBlock();
    POINTER_SIZE_INT stackInfoSize = stackInfo.readByteSize(infoBlock);
    POINTER_SIZE_INT gcMapSize = GCMap::readByteSize(infoBlock + stackInfoSize);

    U_32 nativeOffset = BcMap::get_native_offset_for_bc_offset(bc_pc, infoBlock + stackInfoSize + gcMapSize);
    if (nativeOffset!= MAX_UINT32) {
        *native_pc =  (POINTER_SIZE_INT)(method->getCodeBlockAddress(0) + nativeOffset);
        return true;
    } 
    if (Log::isLogEnabled(LogStream::RT)) {
        Log::log(LogStream::RT) << "Native code for method: "<<method->getName()<<" BC = " << bc_pc << " not found " << std::endl;
    }
    return false;
}

U_32  RuntimeInterface::getInlineDepth(InlineInfoPtr ptr, U_32 offset) {
    const InlineInfoMap::Entry* e = InlineInfoMap::getEntryWithMaxDepth(ptr, offset);
    // real instructions are recorded at an extra nested level to enclosing method
    // but we need to count method marker entries only
    return e == NULL ? 0 : e->getInlineDepth() - 1;
}

Method_Handle   RuntimeInterface::getInlinedMethod(InlineInfoPtr ptr, U_32 offset, U_32 inline_depth) {
    const InlineInfoMap::Entry* e = InlineInfoMap::getEntry(ptr, offset, inline_depth);
    return e == NULL  ? 0 : e->method;
}

uint16 RuntimeInterface::getInlinedBc(InlineInfoPtr ptr, U_32 offset, U_32 inline_depth) {
    const InlineInfoMap::Entry* e = InlineInfoMap::getEntryWithMaxDepth(ptr, offset);
    assert(inline_depth);

    // Real instructions are recorded at a nested level to enclosing method
    // and may happen on topmost entry only;
    // otherwise we have a chain of inlined methods
    // and each entry holds bcOffset of a call inst in parent method.
    // In both cases needed bcOffset is stored in child entry
    const InlineInfoMap::Entry* childCallee = e;
    while (e) {
        U_32 depth = e->getInlineDepth();
        if (depth == inline_depth)
        {
            return childCallee->bcOffset;
        }
        childCallee = e;
        e = e->parentEntry;
    }

    return 0;
}


}}; //namespace Ia32



