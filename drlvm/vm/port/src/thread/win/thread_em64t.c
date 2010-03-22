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

#include "port_thread.h"


void port_thread_context_to_regs(Registers* regs, PCONTEXT pcontext)
{
    regs->rsp = pcontext->Rsp;
    regs->rbp = pcontext->Rbp;
    regs->rip = pcontext->Rip;

    regs->rbx = pcontext->Rbx;
    regs->r12 = pcontext->R12;
    regs->r13 = pcontext->R13;
    regs->r14 = pcontext->R14;
    regs->r15 = pcontext->R15;

    regs->rax = pcontext->Rax;
    regs->rcx = pcontext->Rcx;
    regs->rdx = pcontext->Rdx;
    regs->rsi = pcontext->Rsi;
    regs->rdi = pcontext->Rdi;
    regs->r8  = pcontext->R8;
    regs->r9  = pcontext->R9;
    regs->r10 = pcontext->R10;
    regs->r11 = pcontext->R11;

    regs->eflags = pcontext->EFlags;
}

void port_thread_regs_to_context(PCONTEXT pcontext, Registers* regs)
{
    pcontext->Rsp = regs->rsp;
    pcontext->Rbp = regs->rbp;
    pcontext->Rip = regs->rip;

    pcontext->Rbx = regs->rbx;
    pcontext->R12 = regs->r12;
    pcontext->R13 = regs->r13;
    pcontext->R14 = regs->r14;
    pcontext->R15 = regs->r15;

    pcontext->Rax = regs->rax;
    pcontext->Rcx = regs->rcx;
    pcontext->Rdx = regs->rdx;
    pcontext->Rsi = regs->rsi;
    pcontext->Rdi = regs->rdi;
    pcontext->R8  = regs->r8;
    pcontext->R9  = regs->r9;
    pcontext->R10 = regs->r10;
    pcontext->R11 = regs->r11;

    pcontext->EFlags = regs->eflags;
}
