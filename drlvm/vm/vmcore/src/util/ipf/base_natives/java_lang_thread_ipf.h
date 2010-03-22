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

#define OS_HW_REGS_TO_VM_THREAD_REGS()                 \
    p_thr->regs.gr[4] = nt_registers.IntS0;             \
    p_thr->regs.gr[5] = nt_registers.IntS1;             \
    p_thr->regs.gr[6] = nt_registers.IntS2;             \
    p_thr->regs.gr[7] = nt_registers.IntS3;             \
                                                        \
    p_thr->regs.gr[12] = nt_registers.IntSp;            \
                                                        \
    p_thr->regs.preds = nt_registers.Preds;             \
                                                        \
    p_thr->regs.pfs = nt_registers.RsPFS;               \
    p_thr->regs.bsp = (uint64 *)nt_registers.RsBSP;     \
    p_thr->regs.ip = nt_registers.StIIP;


#define OS_VM_THREAD_REGS_TO_HW_REGS()                 \
    nt_registers.IntS0 = p_thr->regs.gr[4];             \
    nt_registers.IntS1 = p_thr->regs.gr[5];             \
    nt_registers.IntS2 = p_thr->regs.gr[6];             \
    nt_registers.IntS3 = p_thr->regs.gr[7];
