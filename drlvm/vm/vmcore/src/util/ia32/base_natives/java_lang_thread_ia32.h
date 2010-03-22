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
 * @author Intel, Evgueni Brevnov
 */  


#include "cxxlog.h"

#ifdef PLATFORM_POSIX

#define OS_HW_REGS_TO_VM_THREAD_REGS()    LDIE(44, "Not supported");
#define OS_VM_THREAD_REGS_TO_HW_REGS()    LDIE(44, "Not supported");

#else

#define OS_HW_REGS_TO_VM_THREAD_REGS()     \
    p_thr->regs.eax = nt_registers.Eax;     \
    p_thr->regs.ebx = nt_registers.Ebx;     \
    p_thr->regs.ecx = nt_registers.Ecx;     \
    p_thr->regs.edx = nt_registers.Edx;     \
    p_thr->regs.edi = nt_registers.Edi;     \
    p_thr->regs.esi = nt_registers.Esi;     \
    p_thr->regs.ebp = nt_registers.Ebp;     \
    p_thr->regs.esp = nt_registers.Esp;     \
    p_thr->regs.eip = nt_registers.Eip; 



#define OS_VM_THREAD_REGS_TO_HW_REGS()     \
    nt_registers.Eax = p_thr->regs.eax;     \
    nt_registers.Ebx = p_thr->regs.ebx;     \
    nt_registers.Ecx = p_thr->regs.ecx;     \
    nt_registers.Edx = p_thr->regs.edx;     \
    nt_registers.Edi = p_thr->regs.edi;     \
    nt_registers.Esi = p_thr->regs.esi;     \
    nt_registers.Ebp = p_thr->regs.ebp;



#endif
