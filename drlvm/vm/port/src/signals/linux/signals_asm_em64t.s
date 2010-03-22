//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

	.text
	.align 16

// struct Registers {
// uint64 rsp;   ; 00h
// uint64 rbp;   ; 08h
// uint64 rip;   ; 10h
// // callee-saved
// uint64 rbx;   ; 18h
// uint64 r12;   ; 20h
// uint64 r13;   ; 28h
// uint64 r14;   ; 30h
// uint64 r15;   ; 38h
// // scratched
// uint64 rax;   ; 40h
// uint64 rcx;   ; 48h
// uint64 rdx;   ; 50h
// uint64 rsi;   ; 58h
// uint64 rdi;   ; 60h
// uint64 r8;    ; 68h
// uint64 r9;    ; 70h
// uint64 r10;   ; 78h
// uint64 r11;   ; 80h
//
// uint32 eflags;; 88h
// };
//
// void port_transfer_to_regs_asm(Registers* regs)

.globl port_transfer_to_regs_asm
       .type   port_transfer_to_regs_asm, @function
port_transfer_to_regs_asm:
    movq    %rdi, %rdx // regs pointer (1st param - RDI) -> RDX

    movq    0x08(%rdx), %rbp // RBP field
    movq    0x18(%rdx), %rbx // RBX field
    movq    0x20(%rdx), %r12 // R12 field
    movq    0x28(%rdx), %r13 // R13 field
    movq    0x30(%rdx), %r14 // R14 field
    movq    0x38(%rdx), %r15 // R15 field
    movq    0x58(%rdx), %rsi // RSI field
    movq    0x60(%rdx), %rdi // RDI field
    movq    0x68(%rdx), %r8  // R8 field
    movq    0x70(%rdx), %r9  // R9 field
    movq    0x78(%rdx), %r10 // R10 field
    movq    0x80(%rdx), %r11 // R11 field

    movq    0x00(%rdx), %rax // (new RSP) -> RAX
    movq    %rax, (%rsp)     // (new RSP) -> [RSP] for future use
    movq    0x10(%rdx), %rcx // (new RIP) -> RCX
    movq    %rcx, -0x88(%rax)// (new RIP) -> [(new RSP) - 128 - 8]
    movq    0x40(%rdx), %rax // RAX field

    movzwq  0x88(%rdx), %rcx // (word)EFLAGS -> RCX
    test    %rcx, %rcx
    je      __skipefl__
    pushfq
    andl    $0x003F7202, (%rsp) // Clear OF, DF, TF, SF, ZF, AF, PF, CF
    andl    $0x00000CD5, %ecx   // Clear all except OF, DF, SF, ZF, AF, PF, CF
    orl     %ecx, (%rsp)
    popfq                    // restore RFLAGS
__skipefl__:

    movq    0x48(%rdx), %rcx // RCX field
    movq    0x50(%rdx), %rdx // RDX field

    movq    (%rsp), %rsp     // load new RSP
    jmpq    * -0x88(%rsp)    // JMP to new RIP


// void port_longjump_stub(void)
//
// after returning from the called function, RSP points to the 2 argument
// slots in the stack. Saved Registers structure pointer is (RSP + 48)
//
// | interrupted |
// |  program    | <- RSP where the program was interrupted by signal
// |-------------|
// | 0x80 bytes  | <- 'red zone' - we will not change it
// |-------------|
// | return addr |
// | from stub   | <- for using in port_transfer_to_regs as [(new RSP) - 128 - 8]
// |-------------|
// |    saved    |
// |  Registers  | <- to restore register context
// |-------------|
// | [alignment] | <- align Regs pointer to 16-bytes boundary
// |-------------|
// |  pointer to |
// |  saved Regs | <- (RSP + 128)
//                // |-------------|
//                // | 0x80 bytes  | <- 'red zone'
// |-------------|
// | return addr |
// |  from 'fn'  | <- address to return to the port_longjump_stub
// |-------------|

.globl port_longjump_stub
	.type	port_longjump_stub, @function
port_longjump_stub:
//    movq    128(%rsp), %rdi // load RDI with the address of saved Registers
    movq    (%rsp), %rdi // load RDI with the address of saved Registers
    callq   port_transfer_to_regs_asm // restore context
    ret                             // dummy RET - unreachable
