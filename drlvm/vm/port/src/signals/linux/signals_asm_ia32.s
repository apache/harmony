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
	.align 4

// struct Registers {
// uint32 eax;    +00
// uint32 ebx;    +04
// uint32 ecx;    +08
// uint32 edx;    +0C
// uint32 edi;    +10
// uint32 esi;    +14
// uint32 ebp;    +18
// uint32 esp;    +1C
// uint32 eip;    +20
// uint32 eflags; +24
// };
//
// void port_transfer_to_regs(Registers* regs)

.globl port_transfer_to_regs
	.type	port_transfer_to_regs, @function
port_transfer_to_regs:
    movl    0x04(%esp), %edx // store regs pointer to EDX
    movl    0x20(%edx), %ebx // EIP field -> EBX
    movl    0x1C(%edx), %ecx // ESP field
    subl    $4, %ecx
    movl    %ecx, 0x04(%esp) // (new ESP - 4) -> [ESP + 4] (safe storage)
    movl    0x14(%edx), %esi // ESI field
    movl    0x10(%edx), %edi // EDI field
    movl    0x18(%edx), %ebp // EBP field
    movl    %ebx, (%ecx)     // new EIP -> (new ESP - 4) (as return address)
    movl    0x00(%edx), %eax // EAX field
    movl    0x04(%edx), %ebx // EBX field
    movzwl  0x24(%edx), %ecx // (word)EFLAGS -> ECX
    test    %ecx, %ecx
    je      _label_
    pushfl
    andl    $0x003F7202, (%esp)
    andl    $0x00000CD5, %ecx
    orl     %ecx, (%esp)
    popfl                    // restore EFLAGS
_label_:
    movl    0x08(%edx), %ecx // ECX field
    movl    0x0C(%edx), %edx // EDX field
    movl    0x04(%esp), %esp // ((new ESP - 4) -> ESP
    ret                      // JMP by RET


// void port_longjump_stub(void)
//
// after returning from the called function, EBP points to the pointer
// to saved Registers structure
//
// | interrupted |
// |  program    | <- ESP where the program was interrupted by signal
// |-------------|
// | return addr |
// | from stub   | <- for using in port_transfer_to_regs
// |-------------|
// |    saved    |
// |  Registers  | <- to restore register context
// |-------------|
// |  pointer to |
// |  saved Regs | <- EBP
// |-------------|
// |  arg 5      | <-
// |-------------|   |
// ...............    - arguments for 'fn'
// |-------------|   |
// |  arg 0      | <-
// |-------------|
// | return addr |
// |  from 'fn'  | <- address to return to the port_longjump_stub
// |-------------|

.globl port_longjump_stub
	.type	port_longjump_stub, @function
port_longjump_stub:
    movl    %ebp, %esp    // ESP now points to the address of saved Registers
    call    port_transfer_to_regs   // restore context
    ret                             // dummy RET - unreachable
