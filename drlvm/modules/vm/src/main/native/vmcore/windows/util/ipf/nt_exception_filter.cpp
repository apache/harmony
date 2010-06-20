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
#include "platform_lowlevel.h"

#include "Class.h"
#include "Environment.h"
#include "exceptions.h"
#include "method_lookup.h"
#include "vm_strings.h"
#include "vm_threads.h"
#include "open/vm_util.h"
#include "compile.h"
#include "../../../arch/ipf/include/vm_ipf.h"


// Afremov Pavel 20050117 
#include "../m2n_ipf_internal.h"

void nt_to_vm_context(PCONTEXT pcontext, Registers* regs)
{
    ABORT("The function is never called");
    // 20030402: This code is broken
    regs->pfs        = (uint64)(pcontext->RsPFS);
    regs->ip         = (uint64)(pcontext->StIIP);
    regs->bsp        = (uint64*) (pcontext->RsBSP);

    regs->gr[  1]   = (uint64)(pcontext->IntGp);
    regs->gr[  2]   = (uint64)(pcontext->IntT0);
    regs->gr[  3]   = (uint64)(pcontext->IntT1);
    regs->gr[  4]   = (uint64)(pcontext->IntS0);
    regs->gr[  5]   = (uint64)(pcontext->IntS1);
    regs->gr[  6]   = (uint64)(pcontext->IntS2);
    regs->gr[  7]   = (uint64)(pcontext->IntS3);
    regs->gr[  8]   = (uint64)(pcontext->IntV0);
    regs->gr[  9]   = (uint64)(pcontext->IntT2);
    regs->gr[ 10]   = (uint64)(pcontext->IntT3);
    regs->gr[ 11]   = (uint64)(pcontext->IntT4);
    regs->gr[ 12]   = (uint64)(pcontext->IntSp);
    regs->gr[ 13]   = (uint64)(pcontext->IntTeb);
    regs->gr[ 14]   = (uint64)(pcontext->IntT5);
    regs->gr[ 15]   = (uint64)(pcontext->IntT6);
    regs->gr[ 16]   = (uint64)(pcontext->IntT7);
    regs->gr[ 17]   = (uint64)(pcontext->IntT8);
    regs->gr[ 18]   = (uint64)(pcontext->IntT9);
    regs->gr[ 19]   = (uint64)(pcontext->IntT10);
    regs->gr[ 20]   = (uint64)(pcontext->IntT11);
    regs->gr[ 21]   = (uint64)(pcontext->IntT12);
    regs->gr[ 22]   = (uint64)(pcontext->IntT13);
    regs->gr[ 23]   = (uint64)(pcontext->IntT14);
    regs->gr[ 24]   = (uint64)(pcontext->IntT15);
    regs->gr[ 25]   = (uint64)(pcontext->IntT16);
    regs->gr[ 26]   = (uint64)(pcontext->IntT17);
    regs->gr[ 27]   = (uint64)(pcontext->IntT18);
    regs->gr[ 28]   = (uint64)(pcontext->IntT19);
    regs->gr[ 29]   = (uint64)(pcontext->IntT20);
    regs->gr[ 30]   = (uint64)(pcontext->IntT21);
    regs->gr[ 31]   = (uint64)(pcontext->IntT22);

    U_32 gr_cursor = 32;
    uint64 *bsp_cursor = regs->bsp;

    while (gr_cursor<128) {

        if(0x1f8 == (0x1f8 & (uint64)bsp_cursor)) {
            bsp_cursor++;
        }

        regs->gr[gr_cursor] = *bsp_cursor;

        bsp_cursor++;
        gr_cursor++;
    }
}


void vm_to_nt_context(Registers* regs, PCONTEXT pcontext)
{
    ABORT("The function is never called");
    // 20030402: This code is broken
    pcontext->RsPFS    = regs->pfs;
    pcontext->StIIP    = regs->ip;
    pcontextRsBSP    = (uint64)regs->bsp;
    pcontextIntGp    = regs->gr[  1];
    pcontextIntS0    = regs->gr[  4];
    pcontext->IntS1    = regs->gr[  5];
    pcontext->IntS2    = regs->gr[  6];
    pcontext->IntS3    = regs->gr[  7];
    pcontext->IntV0    = regs->gr[  8];
    pcontext->IntSp    = regs->gr[ 12];
    pcontext->IntTeb   = regs->gr[ 13];
}


int NT_exception_filter(LPEXCEPTION_POINTERS p_NT_exception) 
{

    // this filter catches _all_ null ptr exceptions including those caused by
    // VM internal code.  To elimate confusion over what caused the null ptr
    // exception, we first make sure the exception was thrown inside a Java
    // method else assert(0); <--- means it was thrown by VM C/C++ code.

    cout << "very top of NT_exception_filter()"  << endl;
    cout << "ExceptionCode = " << hex << (p_NT_exception->ExceptionRecord->ExceptionCode) << endl;
    cout << "StIIP = " << hex << (long)(p_NT_exception->ContextRecord->StIIP) << endl;
    cout << "IntSp = " << hex << (long)(p_NT_exception->ContextRecord->IntSp) << endl;

    Global_Env *env = VM_Global_State::loader_env;

    VM_Code_Type vmct =
      vm_identify_eip((void *)p_NT_exception->ContextRecord->StIIP);
    if(vmct != VM_TYPE_JAVA) {
        // For now.  What is the correct way of handling a null ptr exception
        // thrown from native code?
        cout << "error ---- not a java null reference" << endl;
        return EXCEPTION_CONTINUE_SEARCH;
    }

    Class *exc_clss = 0;
    switch(p_NT_exception->ExceptionRecord->ExceptionCode) {
    case STATUS_ACCESS_VIOLATION:
        // null pointer exception -- see ...\vc\include\winnt.h
        {
            // Lazy exception object creation
            exc_clss = env->java_lang_NullPointerException_Class;
        }
        break;

    case STATUS_INTEGER_DIVIDE_BY_ZERO:
        // divide by zero exception  -- see ...\vc\include\winnt.h
        {
            // Lazy exception object creation
            exc_clss = env->java_lang_ArithmeticException_Class;
        }
        break;

    case STATUS_PRIVILEGED_INSTRUCTION:
        {
            // privileged exception  -- see ...\vc\include\winnt.h

            // the jit/vm uses "outs" (opcode = 0x6e) as an "int 3"
            // to set breakpoints in jitted code (for stuff like enumerating
            // live references.)
            // save the breakpoint eip in the java thread block so that 
            // vm_at_a_jit_breakpoint() can restore it

            ABORT("Priveleged exception is caugth");
            return EXCEPTION_CONTINUE_EXECUTION;
        }
        break;

    case STATUS_DATATYPE_MISALIGNMENT:
        {
            cout << "got a STATUS_DATATYPE_MISALIGNMENT -- let the os handle this" << endl;
            return EXCEPTION_CONTINUE_EXECUTION;
        }
        break;

    default:
        ABORT("Unexpected ecxeption code");
        return EXCEPTION_CONTINUE_SEARCH;
    }

    Registers regs;
    nt_to_vm_context(p_NT_exception->ContextRecord, &regs);

    // The exception object of class exc_clss will be created by vm_null_ptr_throw.
    assert(exc_clss);
    exn_athrow_regs(&regs, exc_clss, true);

    vm_to_nt_context(&regs, p_NT_exception->ContextRecord);

    return EXCEPTION_CONTINUE_EXECUTION;
} //NT_exception_filter

void __cdecl call_the_run_method2( void * p_xx )
{
    LPEXCEPTION_POINTERS p_NT_exception;
    int NT_exception_filter(LPEXCEPTION_POINTERS p_NT_exception);

    // NT null pointer exception support
    __try {  
        call_the_run_method(p_xx);
    }
    __except ( p_NT_exception = GetExceptionInformation(), 
        NT_exception_filter(p_NT_exception) ) {

        ABORT("Uncaught exception");  // get here only if NT_null_ptr_filter() screws up

    }  // NT null pointer exception support
}
