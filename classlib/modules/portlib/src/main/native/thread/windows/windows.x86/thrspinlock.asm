;  Licensed to the Apache Software Foundation (ASF) under one or more
;  contributor license agreements.  See the NOTICE file distributed with
;  this work for additional information regarding copyright ownership.
;  The ASF licenses this file to You under the Apache License, Version 2.0
;  (the "License"); you may not use this file except in compliance with
;  the License.  You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
;  Unless required by applicable law or agreed to in writing, software
;  distributed under the License is distributed on an "AS IS" BASIS,
;  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;  See the License for the specific language governing permissions and
;  limitations under the License.

	.586p
       	assume cs:flat,ds:flat,ss:flat
       	.xmm
eq_HyThreadAbstractMonitor_spinCount1 equ 48
eq_HyThreadAbstractMonitor_spinCount2 equ 52
eq_HyThreadAbstractMonitor_spinCount3 equ 56
eq_HyThreadAbstractMonitor_spinlockState equ 40
eq_pointer_size equ 4
eqS_hythread_spinlock_acquire equ 18
eqS_hythread_spinlock_swapState equ 16
eqSR_hythread_spinlock_acquire equ 3
eqSR_hythread_spinlock_swapState equ 2
eqSRS_hythread_spinlock_acquire equ 12
eqSRS_hythread_spinlock_swapState equ 8
eqSS_hythread_spinlock_acquire equ 72
eqSS_hythread_spinlock_swapState equ 64
HYTHREAD_MONITOR_SPINLOCK_OWNED equ 1
HYTHREAD_MONITOR_SPINLOCK_UNOWNED equ 0
       	CONST SEGMENT DWORD USE32 PUBLIC 'CONST'
       	CONST ends
       	_TEXT SEGMENT PARA USE32 PUBLIC 'CODE'
        extrn hythread_yield:near
        public hythread_spinlock_acquire
        public hythread_spinlock_swapState
; Prototype: IDATA hythread_spinlock_acquire(hythread_t self, hythread_monitor_t monitor);
; Defined in: #THREAD Args: 2
        align 16
hythread_spinlock_acquire      	PROC NEAR
        ;  localStackUse = 18
        push EBP
        mov EBP,ESP
        push EBX
        push ESI
        sub ESP,72
        mov EDX,dword ptr (eqSS_hythread_spinlock_acquire+0+eqSRS_hythread_spinlock_acquire+8)[ESP]
        mov ECX,dword ptr eq_HyThreadAbstractMonitor_spinCount3[EDX]
L2:
        mov EBX,dword ptr eq_HyThreadAbstractMonitor_spinCount2[EDX]
L3:
; Try to cmpxchg 0 into the target field (-1 indicates free)
        cmp dword ptr eq_HyThreadAbstractMonitor_spinlockState[EDX],HYTHREAD_MONITOR_SPINLOCK_UNOWNED ; setFlags: true
        jne short L10
        xor EAX,EAX
        mov ESI,HYTHREAD_MONITOR_SPINLOCK_OWNED
lockFixup12:
CONST$_LOCKFIXUPS_B SEGMENT DWORD USE32 PUBLIC 'CONST'
        dd offset flat:lockFixup12
CONST$_LOCKFIXUPS_B ends
        lock cmpxchg dword ptr eq_HyThreadAbstractMonitor_spinlockState[EDX],ESI
        test EAX,EAX                             ; setFlags: true
        jnz short L10
        xor EBX,EBX
        jmp short L1
L10:
        dw 37107                                 ; PAUSE
; begin tight loop
        mov EAX,dword ptr eq_HyThreadAbstractMonitor_spinCount1[EDX]
L11:
; inside tight loop
        dec EAX                                  ; setFlags: true(Converted subtract 1 to dec)
        jnz short L11
; end tight loop
        dec EBX                                  ; setFlags: true(Converted subtract 1 to dec)
        jnz short L3
        mov dword ptr 64[ESP],ECX                ; save VMtemp3_1_3_(HyThreadAbstractMonitor->spinCount3)
        mov dword ptr 68[ESP],EDX                ; save VMtemp3_1_2_(struct HyThreadAbstractMonitor*) in_HyVMThreadSpinlocks>>#hythread_spinlock_acquire
        call hythread_yield
        mov ECX,dword ptr 64[ESP]                ; load VMtemp3_1_3_(HyThreadAbstractMonitor->spinCount3)
        dec ECX                                  ; setFlags: true(Converted subtract 1 to dec)
        mov EDX,dword ptr 68[ESP]                ; load VMtemp3_1_2_(struct HyThreadAbstractMonitor*) in_HyVMThreadSpinlocks>>#hythread_spinlock_acquire
        jnz short L2
        mov EBX,-1
L1:
        mov EAX,EBX
        add ESP,72
        pop ESI
        pop EBX
        pop EBP
        ret
hythread_spinlock_acquire        ENDP
; Prototype: UDATA hythread_spinlock_swapState(hythread_monitor_t monitor, UDATA newState);
; Defined in: #THREAD Args: 2
        align 16
hythread_spinlock_swapState    	PROC NEAR
        ;  localStackUse = 16
        push EBP
        mov EBP,ESP
        push EBX
        sub ESP,64
        mov EBX,dword ptr (eqSS_hythread_spinlock_swapState+0+eqSRS_hythread_spinlock_swapState+4)[ESP]
        mov ECX,dword ptr (eqSS_hythread_spinlock_swapState+0+eqSRS_hythread_spinlock_swapState+8)[ESP]
; If we are writing in UNOWNED, we are exiting the critical section, therefore
; have to finish up any writes
        test ECX,ECX                             ; setFlags: true
        ; memory barrier (no code necessary for write barriers)
        xchg dword ptr eq_HyThreadAbstractMonitor_spinlockState[EBX],ECX
; if we entered the critical section, (i.e. we swapped out UNOWNED) then
; we have to issue a readBarrier
        test ECX,ECX                             ; setFlags: true
        mov EAX,ECX
        add ESP,64
        pop EBX
        pop EBP
        ret
hythread_spinlock_swapState        ENDP
       	_TEXT ends
       	end
