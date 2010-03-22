#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

eq_HyThreadAbstractMonitor_spinCount1 = 48
eq_HyThreadAbstractMonitor_spinCount2 = 52
eq_HyThreadAbstractMonitor_spinCount3 = 56
eq_HyThreadAbstractMonitor_spinlockState = 40
eq_pointer_size = 4
eqS_hythread_spinlock_acquire = 18
eqS_hythread_spinlock_swapState = 16
eqSR_hythread_spinlock_acquire = 4
eqSR_hythread_spinlock_swapState = 4
eqSRS_hythread_spinlock_acquire = 16
eqSRS_hythread_spinlock_swapState = 16
eqSS_hythread_spinlock_acquire = 72
eqSS_hythread_spinlock_swapState = 64
HYTHREAD_MONITOR_SPINLOCK_OWNED = 1
HYTHREAD_MONITOR_SPINLOCK_UNOWNED = 0
       	#CODE32 SEGMENT FLAT PUBLIC 'CODE'
       	#assume cs:flat,ds:flat,ss:flat
       	#CODE32 ends
       	#CODE32 SEGMENT FLAT PUBLIC 'CODE'
       	#assume cs:flat,ds:flat,ss:flat
        .globl hythread_yield # an extern
        .globl hythread_spinlock_acquire
        .type hythread_spinlock_acquire,@function
        .globl hythread_spinlock_swapState
        .type hythread_spinlock_swapState,@function
## Prototype: IDATA hythread_spinlock_acquire(hythread_t self, hythread_monitor_t monitor);
## Defined in: #THREAD Args: 2

        .text
        .align 8
hythread_spinlock_acquire:
        push %rbp
        mov %rsp, %rbp
        push %rsi
        push %rdi
        push %rbx
        sub $72, %rsp
        mov (eqSS_hythread_spinlock_acquire+0+eqSRS_hythread_spinlock_acquire+8)(%rsp), %rdx
        mov eq_HyThreadAbstractMonitor_spinCount3(%rdx), %rcx
.L2:
        mov eq_HyThreadAbstractMonitor_spinCount2(%rdx), %rbx
.L3:
## Try to cmpxchg 0 into the target field (-1 indicates free)
        cmpl $HYTHREAD_MONITOR_SPINLOCK_UNOWNED, eq_HyThreadAbstractMonitor_spinlockState(%rdx) ## setFlags: true
        jne .L10
        xor %rax, %rax
        mov $HYTHREAD_MONITOR_SPINLOCK_OWNED, %rsi
        lock 
        cmpxchg %rsi, eq_HyThreadAbstractMonitor_spinlockState(%rdx)
        test %rax, %rax                         ## setFlags: true
        jnz .L10
        xor %rbx, %rbx
        jmp .L1
.L10:
        .word 37107                              ## PAUSE
## begin tight loop
        mov eq_HyThreadAbstractMonitor_spinCount1(%rdx), %rax
.L11:
## inside tight loop
        dec %rax                                ## setFlags: true(Converted subtract 1 to dec)
        jnz .L11
## end tight loop
        dec %rbx                                ## setFlags: true(Converted subtract 1 to dec)
        jnz .L3
        mov %rcx, 64(%rsp)                      ## save VMtemp3_1_3_(HyThreadAbstractMonitor->spinCount3)
        mov %rdx, 68(%rsp)                      ## save VMtemp3_1_2_(struct HyThreadAbstractMonitor*) in_HyVMThreadSpinlocks>>#hythread_spinlock_acquire
        call hythread_yield@PLT
        mov 64(%rsp), %rcx                      ## load VMtemp3_1_3_(HyThreadAbstractMonitor->spinCount3)
        dec %rcx                                ## setFlags: true(Converted subtract 1 to dec)
        mov 68(%rsp), %rdx                      ## load VMtemp3_1_2_(struct HyThreadAbstractMonitor*) in_HyVMThreadSpinlocks>>#hythread_spinlock_acquire
        jnz .L2
        mov $-1, %rbx
.L1:
        mov %rbx, %rax
        add $72, %rsp
        pop %rbx
        pop %rdi
        pop %rsi
        pop %rbp
        ret
END_hythread_spinlock_acquire:
        .size hythread_spinlock_acquire,END_hythread_spinlock_acquire - hythread_spinlock_acquire

## Prototype: UDATA hythread_spinlock_swapState(hythread_monitor_t monitor, UDATA newState);
## Defined in: #THREAD Args: 2

        .text
        .align 8
hythread_spinlock_swapState:
        push %rbp
        mov %rsp, %rbp
        push %rsi
        push %rdi
        push %rbx
        sub $64, %rsp
        mov (eqSS_hythread_spinlock_swapState+0+eqSRS_hythread_spinlock_swapState+4)(%rsp), %rbx
        mov (eqSS_hythread_spinlock_swapState+0+eqSRS_hythread_spinlock_swapState+8)(%rsp), %rcx
## If we are writing in UNOWNED, we are exiting the critical section, therefore
## have to finish up any writes
        test %rcx, %rcx                         ## setFlags: true
        ## memory barrier (no code necessary for write barriers)
        xchg %rcx, eq_HyThreadAbstractMonitor_spinlockState(%rbx)
## if we entered the critical section, (i.e. we swapped out UNOWNED) then
## we have to issue a readBarrier
        test %rcx, %rcx                         ## setFlags: true
        mov %rcx, %rax
        add $64, %rsp
        pop %rbx
        pop %rdi
        pop %rsi
        pop %rbp
        ret
END_hythread_spinlock_swapState:
        .size hythread_spinlock_swapState,END_hythread_spinlock_swapState - hythread_spinlock_swapState

       	#CODE32 ends
        # end of file
