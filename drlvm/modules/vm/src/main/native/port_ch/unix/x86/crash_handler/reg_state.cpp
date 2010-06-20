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
    fprintf(stderr, "    EAX: 0x%"W_PI_FMT", EBX: 0x%"W_PI_FMT", ECX: 0x%"W_PI_FMT", EDX: 0x%"W_PI_FMT"\n",
        regs->eax, regs->ebx, regs->ecx, regs->edx);
    fprintf(stderr, "    ESI: 0x%"W_PI_FMT", EDI: 0x%"W_PI_FMT", ESP: 0x%"W_PI_FMT", EBP: 0x%"W_PI_FMT"\n",
        regs->esi, regs->edi, regs->esp, regs->ebp);
    fprintf(stderr, "    EIP: 0x%"W_PI_FMT"\n", regs->eip);
}
