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

#include "signals_internal.h"
#define ANSI
#include <stdarg.h>


LONG __declspec(naked) NTAPI vectored_exception_handler(LPEXCEPTION_POINTERS nt_exception)
{
    __asm {
    push    ebp
    mov     ebp,esp
    pushfd
    cld
    mov     eax, [ebp + 8]
    push    eax
    call    vectored_exception_handler_internal
    popfd
    pop     ebp
    ret     4
    }
}

void __declspec(naked) PORT_CDECL port_win_dbg_break()
{
__asm { int 3 }
}


extern "C" void port_longjump_stub(void);
#define DIR_FLAG ((U_32)0x00000400)

void port_set_longjump_regs(void* fn, Registers* regs, int num, ...)
{
    void** sp;
    va_list ap;
    int i;
    size_t rcount =
        (sizeof(Registers) + sizeof(void*) - 1) / sizeof(void*);

    if (!regs)
        return;

    sp = (void**)regs->esp - 1;
    *sp = (void*)regs->eip;
    sp = sp - rcount - 1;
    *((Registers*)(sp + 1)) = *regs;
    *sp = (void*)(sp + 1);
    regs->ebp = (U_32)sp;

    sp = sp - num - 1;

    va_start(ap, num);

    for (i = 1; i <= num; i = i + 1)
    {
        void* arg = va_arg(ap, void*);

        if (i == 1 && arg == regs)
            sp[i] = *((void**)regs->ebp); /* Replace 1st arg */
        else
            sp[i] = arg;
    }

    *sp = (void*)&port_longjump_stub;
    regs->esp = (U_32)sp;
    regs->eip = (U_32)fn;
    regs->eflags = regs->eflags & ~DIR_FLAG;
}

void port_transfer_to_function(void* fn, Registers* pregs, int num, ...)
{
    void** sp;
    va_list ap;
    int i;
    size_t rcount =
        (sizeof(Registers) + sizeof(void*) - 1) / sizeof(void*);
    Registers regs;

    if (!pregs)
        return;

    regs = *pregs;

    sp = (void**)regs.esp - 1;
    *sp = (void*)regs.eip;
    sp = sp - rcount - 1;
    *((Registers*)(sp + 1)) = regs;
    *sp = (void*)(sp + 1);
    regs.ebp = (U_32)sp;

    sp = sp - num - 1;

    va_start(ap, num);

    for (i = 1; i <= num; i = i + 1)
    {
        void* arg = va_arg(ap, void*);

        if (i == 1 && arg == pregs)
            sp[i] = *((void**)regs.ebp); /* Replace 1st arg */
        else
            sp[i] = arg;
    }

    *sp = (void*)&port_longjump_stub;
    regs.esp = (U_32)sp;
    regs.eip = (U_32)fn;
    regs.eflags = regs.eflags & ~DIR_FLAG;

    port_transfer_to_regs(&regs);
}


extern "C" void* __cdecl port_setstack_stub(Registers* regs);

// New stack's organization:
//
// | saved stack |
// |  address    | <- to store ESP of current stack
// |-------------|
// |    arg 5    | <- present even if not used
// |-------------|
// |     ...     |
// |-------------|
// |    arg 0    | <- present even if not used
// |-------------|
// | return addr | <- points to port_setstack_stub_end
// |-------------| <- regs->esp points to this cell

void* port_call_alt_stack(void* fn, void* stack_addr, int num, ...)
{
    void** sp;
    va_list ap;
    Registers regs = {};

    regs.esp = (U_32)stack_addr;
    regs.esp -= 4*(1 + 6 + 1);
    sp = (void**)regs.esp;

    va_start(ap, num);

    for (int i = 1; i <= num; i++)
    {
        sp[i] = va_arg(ap, void*);
    }

    regs.eip = (U_32)fn;

    return port_setstack_stub(&regs);
}
