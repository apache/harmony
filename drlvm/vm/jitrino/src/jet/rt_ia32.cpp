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
#include "enc_ia32.h"
#include "open/rt_types.h"
/**
 * @file
 * @brief Implementation of runtime support routines specific for IA-32
 * Intel 64.
 */
 
namespace Jitrino {
namespace Jet {


void *** devirt(AR gr, const JitFrameContext * jfc)
{
#ifdef _EM64T_
    if (gr==gr_x)   return (void***)&jfc->p_rip;
    if (gr == sp)   return (void***)&jfc->rsp;
    if (gr == bp)   return (void***)&jfc->p_rbp;
    
    RegName reg = devirt(gr, jvoid);
    
    if (reg == RegName_RBX)    return (void***)&jfc->p_rbx;
    if (reg == RegName_R12)    return (void***)&jfc->p_r12;
    if (reg == RegName_R13)    return (void***)&jfc->p_r13;
    if (reg == RegName_R14)    return (void***)&jfc->p_r14;
    if (reg == RegName_R15)    return (void***)&jfc->p_r15;
    //
    if (reg == RegName_RAX)    return (void***)&jfc->p_rax;
    if (reg == RegName_RCX)    return (void***)&jfc->p_rcx;
    if (reg == RegName_RDX)    return (void***)&jfc->p_rdx;
    if (reg == RegName_RSI)    return (void***)&jfc->p_rsi;
    if (reg == RegName_RDI)    return (void***)&jfc->p_rdi;
    if (reg == RegName_R8)     return (void***)&jfc->p_r8;
    if (reg == RegName_R9)     return (void***)&jfc->p_r9;
    if (reg == RegName_R10)    return (void***)&jfc->p_r10;
    if (reg == RegName_R11)    return (void***)&jfc->p_r11;
#else
    if (gr == gr_x) { return (void***)&jfc->p_eip; }
    if (gr == sp)   { return (void***)&jfc->esp;   }
    if (gr == bp)   { return (void***)&jfc->p_ebp; }

    static AR eax = virt(RegName_EAX);
    static AR ebx = virt(RegName_EBX);
    static AR ecx = virt(RegName_ECX);
    static AR edx = virt(RegName_EDX);
    static AR esi = virt(RegName_ESI);
    static AR edi = virt(RegName_EDI);

    if (gr == ebx)    return (void***)&jfc->p_ebx;
    if (gr == esi)    return (void***)&jfc->p_esi;
    if (gr == edi)    return (void***)&jfc->p_edi;
    if (gr == eax)    return (void***)&jfc->p_eax;
    if (gr == ecx)    return (void***)&jfc->p_ecx;
    if (gr == edx)    return (void***)&jfc->p_edx;
#endif
    // May happen when XMM regs requested on IA-32/Intel64 - JitFrameContext
    // currently does not have such fields
    return NULL;
}


}};    // ~namespace Jitrino::Jet

