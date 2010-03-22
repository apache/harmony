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
eq_hy_null equ 0
eq_HyThreadMonitor_pinCount equ 28
eq_pointer_size equ 4
eqS_current_stack_depth equ 16
eqS_hythread_monitor_pin equ 16
eqS_hythread_monitor_unpin equ 16
eqSR_current_stack_depth equ 2
eqSR_hythread_monitor_pin equ 2
eqSR_hythread_monitor_unpin equ 2
eqSRS_current_stack_depth equ 8
eqSRS_hythread_monitor_pin equ 8
eqSRS_hythread_monitor_unpin equ 8
eqSS_current_stack_depth equ 64
eqSS_hythread_monitor_pin equ 64
eqSS_hythread_monitor_unpin equ 64
       	CONST SEGMENT DWORD USE32 PUBLIC 'CONST'
       	CONST ends
       	_TEXT SEGMENT PARA USE32 PUBLIC 'CODE'
        public hythread_monitor_pin
        public current_stack_depth
        public hythread_monitor_unpin
        align 16
current_stack_depth    	PROC NEAR
        ;  localStackUse = 16
        push EBP
        mov EBP,ESP
        push EBX
        sub ESP,64
        mov EBX,EBP
        jmp short L2
L1:
        mov EBX,ECX
L2:
        mov ECX,dword ptr [EBX]
        test ECX,ECX                             ; setFlags: true
        jnz short L1
        sub EBX,EBP
        mov ECX,EBX
        mov EAX,EBX                              ; RegReg opt
        add ESP,64
        pop EBX
        pop EBP
        ret
current_stack_depth        ENDP
; Prototype: void hythread_monitor_pin( hythread_monitor_t monitor, hythread_t osThread);
; Defined in: #THREAD Args: 2
        align 16
hythread_monitor_pin   	PROC NEAR
        ;  localStackUse = 16
        push EBP
        mov EBP,ESP
        push EBX
        sub ESP,64
        mov EBX,dword ptr (eqSRS_hythread_monitor_pin+0+8+eqSS_hythread_monitor_pin)[ESP]
        mov EBX,dword ptr (eqSRS_hythread_monitor_pin+0+4+eqSS_hythread_monitor_pin)[ESP]
lockFixup3:
CONST$_LOCKFIXUPS_B SEGMENT DWORD USE32 PUBLIC 'CONST'
        dd offset flat:lockFixup3
CONST$_LOCKFIXUPS_B ends
        lock inc  dword ptr eq_HyThreadMonitor_pinCount[EBX] ;  (Converted add 1 to inc)
        add ESP,64
        pop EBX
        pop EBP
        ret
hythread_monitor_pin        ENDP
; Prototype: void hythread_monitor_unpin( hythread_monitor_t monitor, hythread_t osThread);
; Defined in: #THREAD Args: 2
        align 16
hythread_monitor_unpin 	PROC NEAR
        ;  localStackUse = 16
        push EBP
        mov EBP,ESP
        push EBX
        sub ESP,64
        mov EBX,dword ptr (eqSS_hythread_monitor_unpin+0+8+eqSRS_hythread_monitor_unpin)[ESP]
        mov EBX,dword ptr (eqSS_hythread_monitor_unpin+0+eqSRS_hythread_monitor_unpin+4)[ESP]
lockFixup4:
CONST$_LOCKFIXUPS_B SEGMENT DWORD USE32 PUBLIC 'CONST'
        dd offset flat:lockFixup4
CONST$_LOCKFIXUPS_B ends
        lock dec dword ptr eq_HyThreadMonitor_pinCount[EBX] ;  (Converted subtract 1 to dec)
        add ESP,64
        pop EBX
        pop EBP
        ret
hythread_monitor_unpin        ENDP
       	_TEXT ends
       	end
