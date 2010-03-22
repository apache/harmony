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
 * @author Intel, Konstantin M. Anisimov, Igor V. Chebykin
 *
 */

#include "IpfRuntimeInterface.h"
#include "IpfEncoder.h"
#include "IpfType.h"
#include "IpfOpndManager.h"

using namespace std;

namespace Jitrino {
namespace IPF {

//========================================================================================//
// RuntimeInterface
//========================================================================================//

void RuntimeInterface::unwindStack(MethodDesc      *methodDesc, 
                                   JitFrameContext *jitFrameContext, 
                                   bool            isFirst) {

    // cout << "IPF::RuntimeInterface::unwindStack "  << methodDesc->getName() << endl;

    U_8*      infoBlock = methodDesc->getInfoBlock();    // get method infoBlock
    StackInfo stackInfo  = *((StackInfo*) infoBlock);     // read StackInfo structure

    uint64 sp            = jitFrameContext->sp;           // get current frame sp 
    uint64 addr          = sp + stackInfo.savedBase;      // mem stack offset of first saved gr
    U_32 savedGrMask   = stackInfo.savedGrMask;         // mask of preserved grs saved on stack
    U_32 savedFrMask   = stackInfo.savedFrMask;         // mask of preserved frs saved on stack
    U_32 savedBrMask   = stackInfo.savedBrMask;         // mask of preserved frs saved on stack

    // Restore preserved general registers
    if(savedGrMask != 0) {
        for(U_32 i=0; i<32; i++) {
            if((savedGrMask & 1) == 1) {
                jitFrameContext->p_gr[i] = (uint64 *)addr;
                addr += 8;
            }
            savedGrMask >>= 1;
        }
    }
    
    // Restore preserved floating registers
    if(savedFrMask != 0) {
        for(U_32 i=0; i<32; i++) {
            if((savedFrMask & 1) == 1) {
                jitFrameContext->p_fp[i] = (uint64 *)addr;
                addr += 16;
            }
            savedFrMask >>= 1;
        }
    }

    // Restore preserved branch registers
    if(savedBrMask != 0) {
        for(U_32 i=0; i<8; i++) {
            if((savedBrMask & 1) == 1) {
                jitFrameContext->p_br[i] = (uint64 *)addr;
                addr += 8;
            }
            savedBrMask >>= 1;
        }
    }

    // restore sp
    jitFrameContext->sp += stackInfo.memStackSize;

    // Restore return pointer
    I_32 rpBak = stackInfo.rpBak;
    if(rpBak != LOCATION_INVALID) {
        if(rpBak >= S_BASE) jitFrameContext->p_eip = (uint64 *)(sp + rpBak - S_BASE);
        else                jitFrameContext->p_eip = jitFrameContext->p_gr[rpBak];
    }

    // Restore prs
    I_32 prBak = stackInfo.prBak;
    if(prBak != LOCATION_INVALID) {
        if(prBak >= S_BASE) jitFrameContext->preds = *((uint64 *)(sp + prBak - S_BASE));
        else                jitFrameContext->preds = *(jitFrameContext->p_gr[prBak]);
    }

    // Restore pfs
    I_32 pfsBak = stackInfo.pfsBak;
    if(pfsBak != LOCATION_INVALID) {
        if(pfsBak >= S_BASE) jitFrameContext->p_ar_pfs = (uint64 *)(sp + pfsBak - S_BASE);
        else                 jitFrameContext->p_ar_pfs = jitFrameContext->p_gr[pfsBak];
    }

    // Restore unat
    I_32 unatBak = stackInfo.unatBak;
    if(unatBak != LOCATION_INVALID) {
        if(unatBak >= S_BASE) jitFrameContext->ar_unat = *((uint64 *)(sp + unatBak - S_BASE));
        else                  jitFrameContext->ar_unat = *(jitFrameContext->p_gr[unatBak]);
    }

//    cout << "  sp " << hex << jitFrameContext->sp << dec << " frame size " << stackInfo.memStackSize << endl;
//    if(savedGrMask != 0) cout << "  savedGrMask " << hex << savedGrMask << dec << endl;
//    if(savedFrMask != 0) cout << "  savedFrMask " << hex << savedFrMask << dec << endl;
//    if(savedBrMask != 0) cout << "  savedBrMask " << hex << savedBrMask << dec << endl;
//    if(pfsBak != LOCATION_INVALID) cout << "  pfsBak " << pfsBak << " " << hex << *(jitFrameContext->p_ar_pfs) << dec << endl;
//    if(rpBak  != LOCATION_INVALID) cout << "  rpBak  " << rpBak << " " << hex << *(jitFrameContext->p_eip) << dec << endl;
//    if(prBak  != LOCATION_INVALID) cout << "  prBak  " << prBak << " " << hex << jitFrameContext->preds << dec<< endl;
}

//----------------------------------------------------------------------------------------//

void RuntimeInterface::getGCRootSet(MethodDesc            *methodDesc, 
                                    GCInterface           *gcInterface_, 
                                    const JitFrameContext *context_, 
                                    bool                  isFirst) {

    // cout << "IPF::RuntimeInterface::getGCRootSet" << endl;

    gcInterface      = gcInterface_;
    context          = context_;
    U_8*   infoBlock = methodDesc->getInfoBlock();
    U_8*   gcInfo    = infoBlock + sizeof(StackInfo);
    uint64 currIp    = *context->p_eip;
    U_32 gcSize    = *((U_32 *)gcInfo);

//    cout << "getGCRootSet for ip " << hex << currIp << dec << " method " << methodDesc->getName() << endl;
    U_8* safePoint = findSafePoint(gcInfo, gcSize, currIp);
    enumerateRootSet(gcInterface, context, safePoint);
}

//----------------------------------------------------------------------------------------//

U_32 RuntimeInterface::getInlineDepth(InlineInfoPtr ptr, U_32 offset) {

    std::cout << "FIXME: IPF::RuntimeInterface::getInlineDepth" << endl;
    return 0;
}

//----------------------------------------------------------------------------------------//

Method_Handle RuntimeInterface::getInlinedMethod(InlineInfoPtr ptr, 
                                                 U_32        offset, 
                                                 U_32        inline_depth) {

    std::cout << "FIXME: IPF::RuntimeInterface::getInlinedMethod" << endl;
    return NULL;
}

//----------------------------------------------------------------------------------------//

void RuntimeInterface::fixHandlerContext(MethodDesc *methodDesc, JitFrameContext *context, bool isFirst) {
   //std::cout << "FIXME: IPF::RuntimeInterface::fixHandlerContext " << methodDesc->getName() << endl;
}

//----------------------------------------------------------------------------------------//

void* RuntimeInterface::getAddressOfThis(MethodDesc              *methodDesc, 
                                         const ::JitFrameContext *jitFrameContext, 
                                         bool                    isFirst) {

//    cout << "IPF::RuntimeInterface::getAddressOfThis" << endl;
    assert(!methodDesc->isStatic());
    return jitFrameContext->p_gr[G_INARG_BASE];
}

//----------------------------------------------------------------------------------------//

bool RuntimeInterface::recompiledMethodEvent(MethodDesc               *methodDesc, 
                                             void                     *data) {
    //std::cout << "IPF::RuntimeInterface::recompiledMethodEvent " << methodDesc->getName() << endl;

    char *callAddr      = (char *)(~(((uint64)0x4cafe) << 32) & (uint64)data);
    char **indirectAddr = (char **)methodDesc->getIndirectAddress();
    char *methodAddr    = *indirectAddr;

    return Encoder::patchCallAddr(callAddr, methodAddr);
}

//----------------------------------------------------------------------------------------//

bool RuntimeInterface::getBcLocationForNative(MethodDesc *methodDesc, POINTER_SIZE_INT native_pc, uint16 *bc_pc) {

    std::cout << "FIXME: IPF::RuntimeInterface::getBcLocationForNative " << methodDesc->getName() << endl;
//    assert(0);
//    return false;
    return true;
}

//----------------------------------------------------------------------------------------//

bool RuntimeInterface::getNativeLocationForBc(MethodDesc *methodDesc,  uint16 bc_pc, POINTER_SIZE_INT *native_pc) {

    std::cout << "FIXME: IPF::RuntimeInterface::getNativeLocationForBc " << methodDesc->getName() << endl;
    assert(0);
    return false;
}

//----------------------------------------------------------------------------------------//
// GC Root Set 
//----------------------------------------------------------------------------------------//

U_8* RuntimeInterface::findSafePoint(U_8* info, U_32 size, uint64 currIp)
{
    U_32 offset    = ROOT_SET_HEADER_SIZE;
    U_8*   safePoint = NULL;
    U_32 spSize    = 0;
    uint64 spAddress = 0;

    while (offset < size) {
        safePoint = info + offset;
        spSize    = *((U_32 *)safePoint);
        spAddress = *((uint64 *)(safePoint + sizeof(U_32)));

        if (spAddress == currIp) return safePoint;
        offset += spSize;
    }

    IPF_ERR << " No safe point found";
    return NULL;
}

//----------------------------------------------------------------------------------------//
// 

void RuntimeInterface::enumerateRootSet(GCInterface*           gcInterface,
                                        const JitFrameContext* context,
                                        U_8*                   safePoint)
{
    U_32 size   = *((U_32 *)(safePoint));
    I_32* ptr    = (I_32 *)(safePoint + SAFE_POINT_HEADER_SIZE);
    I_32* maxPtr = (I_32 *)(safePoint + size);

    while (ptr < maxPtr) {
        if (isMptr(*ptr)) reportMptr(*(ptr++), *(ptr++));
        else              reportBase(*(ptr++));
    }
}

//----------------------------------------------------------------------------------------//

void** RuntimeInterface::getContextValue(I_32 location) {
    
    if (location < 0) {
        location = - (location + 1);              // if location refers mptr - restore it
    }

    void** ptr = NULL;
    if (location >= NUM_G_REG) {                  // this location points in memory stack
        I_32 offset = location - NUM_G_REG;      // calc memory stack offset
        ptr = (void **)(context->sp + offset);    // get pointer on stack value
    } else {                                      // general register
        ptr = (void **)(context->p_gr[location]); // get pointer on reg value
    }
    return ptr;                                   // return value
}

//----------------------------------------------------------------------------------------//

void RuntimeInterface::reportMptr(I_32 mptr, I_32 base) {

    void **mptrPtr = getContextValue(mptr);
    void **basePtr = getContextValue(base);

    uint64 *u1    = (uint64 *)basePtr;
    uint64 *u2    = (uint64 *)mptrPtr;
    assert(*u2 >= *u1);
    size_t offset = *u2 - *u1;
//    cout << "  report mptr: " << - (mptr+1) << flush; 
//    cout << " " << u2 << flush;
//    cout << " " << hex << *u2 << dec << flush;
//    cout << " base: " << base << flush;
//    cout << " " << u1 << flush;
//    cout << " " << hex << *u1  << dec; // << endl;
//    cout << " offset " << offset << endl;
    
    gcInterface->enumerateRootManagedReference(mptrPtr, offset);
}

//----------------------------------------------------------------------------------------//

void RuntimeInterface::reportBase(I_32 base) {

    void** basePtr = getContextValue(base);   
//    uint64 *u1 = (uint64 *)basePtr;
//    cout << "  report base: " << base << flush;
//    cout << " " << u1 << flush;
//    cout << " " << hex << *u1  << dec << endl;

    gcInterface->enumerateRootReference(basePtr);
}

//----------------------------------------------------------------------------------------//

bool RuntimeInterface::isMptr(I_32 ptr) { return ptr < 0; }

} // IPF
} // Jitrino
