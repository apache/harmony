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
    fprintf(stderr, "Registers:");

    for (int gn = 0; gn < 8; gn++)
    {
        fprintf(stderr, "\n    GR %2d-%2d:", gn*4, (gn+1)*4 - 1);
        for (int gc = 0; gc < 4; gc++)
             fprintf(stderr, " 0x%"W_PI_FMT, regs->gr[gn*4 + gc]);
    }

    for (int bn = 0; bn < 2; bn++)
    {
        fprintf(stderr, "\n    BR %2d-%2d:", bn*4, (bn+1)*4 - 1);
        for (int bc = 0; bc < 4; bc++)
             fprintf(stderr, " 0x%"W_PI_FMT, regs->br[bn*4 + bc]);
    }
        
    fprintf(stderr, "\n");
    fprintf(stderr, "    preds: 0x%"W_PI_FMT"\n", regs->preds);
    fprintf(stderr, "     nats: 0x%"W_PI_FMT"\n", regs->nats);
    fprintf(stderr, "      pfs: 0x%"W_PI_FMT"\n", regs->pfs);
    fprintf(stderr, "      bsp: 0x%"W_PI_FMT"\n", (POINTER_SIZE_INT)regs->bsp);
    fprintf(stderr, "     *bsp: 0x%"W_PI_FMT"\n", regs->bsp ? (*regs->bsp) : 0);
    fprintf(stderr, "       ip: 0x%"W_PI_FMT"\n", regs->ip);
}
