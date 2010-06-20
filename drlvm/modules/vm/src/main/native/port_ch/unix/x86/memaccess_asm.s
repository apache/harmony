//
//  Licensed to the Apache Software Foundation (ASF) under one or more
//  contributor license agreements.  See the NOTICE file distributed with
//  this work for additional information regarding copyright ownership.
//  The ASF licenses this file to You under the Apache License, Version 2.0
//  (the "License"); you may not use this file except in compliance with
//  the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
    .text
    .align 4

// port_memcpy_asm(void* dst, const void* src, size_t size,
//                 void** prestart_addr, void* memcpyaddr);
// 1st arg (EBP +  8) - dest
// 2nd arg (EBP + 12) - src
// 3rd arg (EBP + 16) - len
// 4th arg (EBP + 20) - address of restart_address storage
// 5th arg (EBP + 24) - address of memcpy - not used on x86

.globl port_memcpy_asm
    .type    port_memcpy_asm, @function
port_memcpy_asm:
    pushl    %ebp
    movl     %esp, %ebp
    subl     $20, %esp

    movl    20(%ebp), %eax
    movl    $preret, (%eax)

    movl    8(%ebp), %eax
    movl    %eax, (%esp)
    movl    12(%ebp), %eax
    movl    %eax, 4(%esp)
    movl    16(%ebp), %eax
    movl    %eax, 8(%esp)
    call    memcpy

preret:
    addl    $20, %esp
    popl    %ebp
    ret
