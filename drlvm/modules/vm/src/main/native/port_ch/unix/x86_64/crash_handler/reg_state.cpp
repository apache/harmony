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

#include <stdio.h>
#include "open/platform_types.h"
#include "stack_dump.h"


void print_reg_state(Registers* regs)
{
    fprintf(stderr, "Registers:\n");
    fprintf(stderr, "    RAX: 0x%"W_PI_FMT", RBX: 0x%"W_PI_FMT"\n",
        regs->rax, regs->rbx);
    fprintf(stderr, "    RCX: 0x%"W_PI_FMT", RDX: 0x%"W_PI_FMT"\n",
        regs->rcx, regs->rdx);
    fprintf(stderr, "    RSI: 0x%"W_PI_FMT", RDI: 0x%"W_PI_FMT"\n",
        regs->rsi, regs->rdi);
    fprintf(stderr, "    RSP: 0x%"W_PI_FMT", RBP: 0x%"W_PI_FMT"\n",
        regs->rsp, regs->rbp);
    fprintf(stderr, "    R8 : 0x%"W_PI_FMT", R9 : 0x%"W_PI_FMT"\n",
        regs->r8,  regs->r9);
    fprintf(stderr, "    R10: 0x%"W_PI_FMT", R11: 0x%"W_PI_FMT"\n",
        regs->r10, regs->r11);
    fprintf(stderr, "    R12: 0x%"W_PI_FMT", R13: 0x%"W_PI_FMT"\n",
        regs->r12, regs->r13);
    fprintf(stderr, "    R14: 0x%"W_PI_FMT", R15: 0x%"W_PI_FMT"\n",
        regs->r14, regs->r15);
    fprintf(stderr, "    RIP: 0x%"W_PI_FMT"\n", regs->rip);
}
