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

#include <assert.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/ucontext.h>
#include "port_thread.h"


void port_thread_context_to_regs(Registers* regs, ucontext_t* uc)
{
    memcpy(regs->gr, uc->uc_mcontext.sc_gr, sizeof(regs->gr));
    memcpy(regs->fp, uc->uc_mcontext.sc_fr, sizeof(regs->fp));
    memcpy(regs->br, uc->uc_mcontext.sc_br, sizeof(regs->br));

    regs->preds = uc->uc_mcontext.sc_pr;
    regs->nats  = uc->uc_mcontext.sc_ar_rnat;
    regs->pfs   = uc->uc_mcontext.sc_ar_pfs;
    regs->bsp   = (uint64*)uc->uc_mcontext.sc_ar_bsp;
    regs->ip    = uc->uc_mcontext.sc_ip;
}

void port_thread_regs_to_context(ucontext_t* uc, Registers* regs)
{
    memcpy(uc->uc_mcontext.sc_gr, regs->gr, sizeof(regs->gr));
    memcpy(uc->uc_mcontext.sc_fr, regs->fp, sizeof(regs->fp));
    memcpy(uc->uc_mcontext.sc_br, regs->br, sizeof(regs->br));

    uc->uc_mcontext.sc_pr      = regs->preds;
    uc->uc_mcontext.sc_ar_rnat = regs->nats;
    uc->uc_mcontext.sc_ar_pfs  = regs->pfs;
    uc->uc_mcontext.sc_ar_bsp  = (uint64)regs->bsp;
    uc->uc_mcontext.sc_ip      = regs->ip;
}
