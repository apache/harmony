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
 /**
  * @file
  * @brief Concentrates all the codegen's knowledge about the TLS and
  * related things.
  */


#include "Ia32Tls.h"
#include "VMInterface.h"

namespace Jitrino {
namespace Ia32 {

static Opnd* createTlsBaseLoadGeneric(IRManager& ir, Node* ctrlNode, Type* tlsBaseType);

#ifdef _WIN32
    static Opnd* createTlsBaseLoadWin(IRManager& ir, Node* ctrlNode, Type* tlsBaseType);
#else
    static Opnd* createTlsBaseLoadLin(IRManager& ir, Node* ctrlNode, Type* tlsBaseType);
#endif

Opnd* createTlsBaseLoadSequence(IRManager& irManager, Node* ctrlNode)
{
    Type* int8type = irManager.getTypeManager().getInt8Type();
    Type* tlsBaseType = irManager.getTypeManager().getUnmanagedPtrType(int8type);
    return createTlsBaseLoadSequence(irManager, ctrlNode, tlsBaseType);
}

Opnd* createTlsBaseLoadSequence(IRManager& irManager, Node* ctrlNode, Type* tlsBaseType)
{
    Opnd* tlsBase;
#if defined(_WIN32) && defined(_EM64T_)
    // Currently do it in a looong way - via the helper, but need to change
    // it to FS:[0x14] - TODO
    tlsBase = createTlsBaseLoadGeneric(irManager, ctrlNode, tlsBaseType);
#elif defined(_WIN32)
    // Fast access through FS:[0x14]
    tlsBase = createTlsBaseLoadWin(irManager, ctrlNode, tlsBaseType);
#elif defined(__linux__) || defined(FREEBSD)
    // On Linux, try the fast way
    tlsBase = createTlsBaseLoadLin(irManager, ctrlNode, tlsBaseType);
#else
    // Some other unknown platform - slow, but platform-independent way through the helper
    tlsBase = createTlsBaseLoadGeneric(irManager, ctrlNode, tlsBaseType);
#endif
    return tlsBase;
}

Opnd* createTlsBaseLoadGeneric(IRManager& irManager, Node* ctrlNode, Type* tlsBaseType)
{
    // Dunno where the HYTHR keeps its thread structure - request through the helper
    Opnd* tlsBase = irManager.newOpnd(tlsBaseType);
    Inst* callInst = irManager.newRuntimeHelperCallInst(VM_RT_GC_GET_TLS_BASE, 0, NULL, tlsBase);
    ctrlNode->appendInst(callInst);
    return tlsBase;
}

#ifdef _WIN32

Opnd* createTlsBaseLoadWin(IRManager& irManager, Node* ctrlNode, Type* tlsBaseType)
{
    if (!VMInterface::useFastTLSAccess()) {
        return createTlsBaseLoadGeneric(irManager, ctrlNode, tlsBaseType);
    }
    // HYTHR's structure is stored in TIB's free offset aka [fs:0x14]
    Opnd* tlsBase = irManager.newMemOpnd(tlsBaseType, MemOpndKind_Any, NULL, 0x14, RegName_FS);
    return tlsBase;
}

#else   // ifdef _WIN32

Opnd* createTlsBaseLoadLin(IRManager& irManager, Node* ctrlNode, Type* tlsBaseType)
{
    if (!VMInterface::useFastTLSAccess()) {
        return createTlsBaseLoadGeneric(irManager, ctrlNode, tlsBaseType);
    }

    // HYTHR's structure is stored in global TLS
    /*
        MOV tib, fs:[0]
        MOV pHythr, [tib + threadOffset]
        MOV hythread_t, *pHythr
        tlsBase = hythread_t
    */

    int threadOffset = VMInterface::getTLSBaseOffset();
    Opnd* pTib;
#if defined(_EM64T_)
    pTib = irManager.newMemOpnd(tlsBaseType, MemOpndKind_Any, NULL, 0, RegName_FS);
#else
    pTib = irManager.newMemOpnd(tlsBaseType, MemOpndKind_Any, NULL, 0, RegName_GS);
#endif
    Opnd* tib = irManager.newOpnd(tlsBaseType);
    Inst* ii;
    ii = irManager.newCopyPseudoInst(Mnemonic_MOV, tib, pTib);
    ctrlNode->appendInst(ii);

    Opnd* pHyThread = irManager.newMemOpnd(tlsBaseType, MemOpndKind_Any, tib, threadOffset);
    Opnd* _hythread_t = irManager.newOpnd(tlsBaseType);

    ii= irManager.newCopyPseudoInst(Mnemonic_MOV, _hythread_t, pHyThread);
    ctrlNode->appendInst(ii);
    Opnd* tlsBase = _hythread_t;
    return tlsBase;
}

#endif   // ifdef _WIN32


}};  // ~Jitrino::Ia32

