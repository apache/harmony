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
#define ANSI
#include <stdarg.h>


void port_thread_context_to_regs(Registers* regs, PCONTEXT context)
{
    regs->eax = context->Eax;
    regs->ecx = context->Ecx;
    regs->edx = context->Edx;
    regs->edi = context->Edi;
    regs->esi = context->Esi;
    regs->ebx = context->Ebx;
    regs->ebp = context->Ebp;
    regs->eip = context->Eip;
    regs->esp = context->Esp;
    regs->eflags = context->EFlags;
}

void port_thread_regs_to_context(PCONTEXT context, Registers* regs)
{
    context->Esp = regs->esp;
    context->Eip = regs->eip;
    context->Ebp = regs->ebp;
    context->Ebx = regs->ebx;
    context->Esi = regs->esi;
    context->Edi = regs->edi;
    context->Eax = regs->eax;
    context->Ecx = regs->ecx;
    context->Edx = regs->edx;
    context->EFlags = regs->eflags;
}
