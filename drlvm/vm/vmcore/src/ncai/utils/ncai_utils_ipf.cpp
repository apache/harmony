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
 * @author Ilya Berezhniuk
 */

#include "open/types.h"
#include <open/ncai_thread.h>

#include "ncai_internal.h"


struct NcaiRegisters
{
    // General registers
    union
    {
        uint64  gr[32];
        struct
        {
            uint64  gr0;
            uint64  gr1;
            uint64  gr2;
            uint64  gr3;
            uint64  gr4;
            uint64  gr5;
            uint64  gr6;
            uint64  gr7;
            uint64  gr8;
            uint64  gr9;
            uint64  gr10;
            uint64  gr11;
            uint64  gr12;
            uint64  gr13;
            uint64  gr14;
            uint64  gr15;
            uint64  gr16;
            uint64  gr17;
            uint64  gr18;
            uint64  gr19;
            uint64  gr20;
            uint64  gr21;
            uint64  gr22;
            uint64  gr23;
            uint64  gr24;
            uint64  gr25;
            uint64  gr26;
            uint64  gr27;
            uint64  gr28;
            uint64  gr29;
            uint64  gr30;
            uint64  gr31;
        };
    };
    // Branch registers
    union
    {
        uint64  br[8];
        struct
        {
            uint64  br0;
            uint64  br1;
            uint64  br2;
            uint64  br3;
            uint64  br4;
            uint64  br5;
            uint64  br6;
            uint64  br7;
        };
    };
    // Application registers
    uint64  RSC;        // ar16
    uint64  BSP;        // ar17
//    uint64  BSPSTORE;   // ar18
    uint64  RNAT;       // ar19
//    uint64  FCR;        // ar21
//    uint64  EFLAGS;     // ar24
//    uint64  CSD;        // ar25
//    uint64  SSD;        // ar26
//    uint64  CFLG;       // ar27
//    uint64  FSR;        // ar28
//    uint64  FIR;        // ar29
//    uint64  FDR;        // ar30
    uint64  CCV;        // ar32
    uint64  UNAT;       // ar36
    uint64  FPSR;       // ar40
//    uint64  ITC;        // ar44
    uint64  PFS;        // ar64
    uint64  LC;         // ar65
//    uint64  EC;         // ar66

    uint64  IP;         // Instruction pointer
    uint64  CFM;        // Current frame marker (38 bits)
};

#define REGSIZE(_field_) ((jint)sizeof(((NcaiRegisters*)0)->_field_))
#define REGOFF(_field_) ((POINTER_SIZE_INT)&(((NcaiRegisters*)0)->_field_))

NcaiRegisterTableItem g_ncai_reg_table[] = {
    {"gr0",     REGSIZE(gr0),   REGOFF(gr0)   },
    {"gr1",     REGSIZE(gr1),   REGOFF(gr1)   },
    {"gr2",     REGSIZE(gr2),   REGOFF(gr2)   },
    {"gr3",     REGSIZE(gr3),   REGOFF(gr3)   },
    {"gr4",     REGSIZE(gr4),   REGOFF(gr4)   },
    {"gr5",     REGSIZE(gr5),   REGOFF(gr5)   },
    {"gr6",     REGSIZE(gr6),   REGOFF(gr6)   },
    {"gr7",     REGSIZE(gr7),   REGOFF(gr7)   },
    {"gr8",     REGSIZE(gr8),   REGOFF(gr8)   },
    {"gr9",     REGSIZE(gr9),   REGOFF(gr9)   },
    {"gr10",    REGSIZE(gr10),  REGOFF(gr10)  },
    {"gr11",    REGSIZE(gr11),  REGOFF(gr11)  },
    {"gr12",    REGSIZE(gr12),  REGOFF(gr12)  },
    {"gr13",    REGSIZE(gr13),  REGOFF(gr13)  },
    {"gr14",    REGSIZE(gr14),  REGOFF(gr14)  },
    {"gr15",    REGSIZE(gr15),  REGOFF(gr15)  },
    {"gr16",    REGSIZE(gr16),  REGOFF(gr16)  },
    {"gr17",    REGSIZE(gr17),  REGOFF(gr17)  },
    {"gr18",    REGSIZE(gr18),  REGOFF(gr18)  },
    {"gr19",    REGSIZE(gr19),  REGOFF(gr19)  },
    {"gr20",    REGSIZE(gr20),  REGOFF(gr20)  },
    {"gr21",    REGSIZE(gr21),  REGOFF(gr21)  },
    {"gr22",    REGSIZE(gr22),  REGOFF(gr22)  },
    {"gr23",    REGSIZE(gr23),  REGOFF(gr23)  },
    {"gr24",    REGSIZE(gr24),  REGOFF(gr24)  },
    {"gr25",    REGSIZE(gr25),  REGOFF(gr25)  },
    {"gr26",    REGSIZE(gr26),  REGOFF(gr26)  },
    {"gr27",    REGSIZE(gr27),  REGOFF(gr27)  },
    {"gr28",    REGSIZE(gr28),  REGOFF(gr28)  },
    {"gr29",    REGSIZE(gr29),  REGOFF(gr29)  },
    {"gr30",    REGSIZE(gr30),  REGOFF(gr30)  },
    {"gr31",    REGSIZE(gr31),  REGOFF(gr31)  },
    {"br0",     REGSIZE(br0),   REGOFF(br0)   },
    {"br1",     REGSIZE(br1),   REGOFF(br1)   },
    {"br2",     REGSIZE(br2),   REGOFF(br2)   },
    {"br3",     REGSIZE(br3),   REGOFF(br3)   },
    {"br4",     REGSIZE(br4),   REGOFF(br4)   },
    {"br5",     REGSIZE(br5),   REGOFF(br5)   },
    {"br6",     REGSIZE(br6),   REGOFF(br6)   },
    {"br7",     REGSIZE(br7),   REGOFF(br7)   },

    {"RSC",     REGSIZE(RSC),     REGOFF(RSC)     },
    {"BSP",     REGSIZE(BSP),     REGOFF(BSP)     },
//    {"BSPSTORE",REGSIZE(BSPSTORE),REGOFF(BSPSTORE)},
    {"RNAT",    REGSIZE(RNAT),    REGOFF(RNAT)    },
//    {"FCR",     REGSIZE(FCR),     REGOFF(FCR)     },
//    {"EFLAGS",  REGSIZE(EFLAGS),  REGOFF(EFLAGS)  },
//    {"CSD",     REGSIZE(CSD),     REGOFF(CSD)     },
//    {"SSD",     REGSIZE(SSD),     REGOFF(SSD)     },
//    {"CFLG",    REGSIZE(CFLG),    REGOFF(CFLG)    },
//    {"FSR",     REGSIZE(FSR),     REGOFF(FSR)     },
//    {"FIR",     REGSIZE(FIR),     REGOFF(FIR)     },
//    {"FDR",     REGSIZE(FDR),     REGOFF(FDR)     },
    {"CCV",     REGSIZE(CCV),     REGOFF(CCV)     },
    {"UNAT",    REGSIZE(UNAT),    REGOFF(UNAT)    },
    {"FPSR",    REGSIZE(FPSR),    REGOFF(FPSR)    },
//    {"ITC",     REGSIZE(ITC),     REGOFF(ITC)     },
    {"PFS",     REGSIZE(PFS),     REGOFF(PFS)     },
    {"LC",      REGSIZE(LC),      REGOFF(LC)      },
//    {"EC",      REGSIZE(EC),      REGOFF(EC)      },

    {"IP",      REGSIZE(IP),      REGOFF(IP)      },
    {"CFM",     REGSIZE(CFM),     REGOFF(CFM)     }
};

size_t ncai_get_reg_table_size()
{
    return sizeof(g_ncai_reg_table)/sizeof(g_ncai_reg_table[0]);
}


static void ncai_context_to_registers(ucontext_t* pcontext, NcaiRegisters* pregs)
{
    memcpy(&pregs->gr, &pcontext->uc_mcontext.sc_gr, sizeof(pregs->gr));
    memcpy(&pregs->br, &pcontext->uc_mcontext.sc_br, sizeof(pregs->br));

    pregs->RSC  = pcontext->uc_mcontext.sc_ar_rsc;
    pregs->BSP  = pcontext->uc_mcontext.sc_ar_bsp;
    pregs->RNAT = pcontext->uc_mcontext.sc_ar_rnat;
    pregs->CCV  = pcontext->uc_mcontext.sc_ar_ccv;
    pregs->UNAT = pcontext->uc_mcontext.sc_ar_unat;
    pregs->FPSR = pcontext->uc_mcontext.sc_ar_fpsr;
    pregs->PFS  = pcontext->uc_mcontext.sc_ar_pfs;
    pregs->LC   = pcontext->uc_mcontext.sc_ar_lc;
    pregs->IP   = pcontext->uc_mcontext.sc_ip;
    pregs->CFM  = pcontext->uc_mcontext.sc_cfm;
}

static void ncai_registers_to_context(NcaiRegisters* pregs, ucontext_t* pcontext)
{
    memcpy(&pcontext->uc_mcontext.sc_gr, &pregs->gr, sizeof(pcontext->uc_mcontext.sc_gr));
    memcpy(&pcontext->uc_mcontext.sc_br, &pregs->br, sizeof(pcontext->uc_mcontext.sc_br));

    pcontext->uc_mcontext.sc_ar_rsc  = pregs->RSC;
    pcontext->uc_mcontext.sc_ar_bsp  = pregs->BSP;
    pcontext->uc_mcontext.sc_ar_rnat = pregs->RNAT;
    pcontext->uc_mcontext.sc_ar_ccv  = pregs->CCV;
    pcontext->uc_mcontext.sc_ar_unat = pregs->UNAT;
    pcontext->uc_mcontext.sc_ar_fpsr = pregs->FPSR;
    pcontext->uc_mcontext.sc_ar_pfs  = pregs->PFS;
    pcontext->uc_mcontext.sc_ar_lc   = pregs->LC;
    pcontext->uc_mcontext.sc_ip      = pregs->IP;
    pcontext->uc_mcontext.sc_cfm     = pregs->CFM;
}

bool ncai_get_register_value(hythread_t thread, jint reg_number, void* buf_ptr)
{
    thread_context_t context;
    IDATA status = hythread_get_thread_context(thread, &context);

    if (status != TM_ERROR_NONE)
        return false;

    NcaiRegisters regs;
    ncai_context_to_registers(&context, &regs);

    memcpy(
        buf_ptr,
        ((U_8*)&regs) + g_ncai_reg_table[reg_number].offset,
        g_ncai_reg_table[reg_number].size);

    return true;
}

bool ncai_set_register_value(hythread_t thread, jint reg_number, void* buf_ptr)
{
    thread_context_t context;
    IDATA status = hythread_get_thread_context(thread, &context);

    if (status != TM_ERROR_NONE)
        return false;

    NcaiRegisters regs;
    ncai_context_to_registers(&context, &regs);

    memcpy(
        ((U_8*)&regs) + g_ncai_reg_table[reg_number].offset,
        buf_ptr,
        g_ncai_reg_table[reg_number].size);

    ncai_registers_to_context(&regs, &context);

    status = hythread_set_thread_context(thread, &context);

    return (status == TM_ERROR_NONE);
}

void* ncai_get_instruction_pointer(hythread_t thread)
{
    thread_context_t context;
    IDATA status = hythread_get_thread_context(thread, &context);

    if (status != TM_ERROR_NONE)
        return NULL;

    NcaiRegisters regs;
    ncai_context_to_registers(&context, &regs);

    return (void*)regs.IP;
}
