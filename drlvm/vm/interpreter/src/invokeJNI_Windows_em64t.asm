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


PUBLIC  invokeJNI

_TEXT   SEGMENT

invokeJNI PROC

;    int64 invokeJNI(uint64 *args, uword n_fps, uword n_stacks, GenericFunctionPointer f);
;   rcx - memory
;   rdx - n fp args
;   r8  - n mem args
;   r9  - function ptr

    push    rbp
    mov rbp, rsp
    mov rax, rcx ; mem
    mov rcx, r8 ; n mem args

; cycle to fill all fp args
    movsd xmm0, qword ptr [rax+8]
    movsd xmm1, qword ptr [rax+16]
    movsd xmm2, qword ptr [rax+24]
    movsd xmm3, qword ptr [rax+32]

    mov r10, r9 ; func ptr
; check for memory args
    cmp rcx, 0
    jz cycle_end

    mov rdx, rsp  ; Check stack alignment on 16 bytes
    and rdx, 8    ; This code may be removed after we make sure that
    jz  no_abort  ; compiler always calls us with aligned stack
    int 3
no_abort:
    mov rdx, rcx  ; Align stack on 16 bytes before pushing stack
    and rdx, 1    ; arguments in case we have odd number of stack
    shl rdx, 3    ; arguments
    sub rsp, rdx

; store memory args
    lea r9, qword ptr [rax + rcx * 8 + 64]
    sub r9, rsp ; offset
cycle:
    push qword ptr [rsp+r9]
    loop cycle
cycle_end:
    mov rcx, [rax + 40]
    mov rdx, [rax + 48]
    mov r8,  [rax + 56]
    mov r9,  [rax + 64]

    sub rsp, 32 ; shadow space

    call    r10
    leave
    ret

invokeJNI ENDP


_TEXT   ENDS

END

