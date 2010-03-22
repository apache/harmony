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


#ifndef __SIGNALS_H__
#define __SIGNALS_H__

#include <assert.h>
#include "open/platform_types.h"
#include "port_crash_handler.h"
#include "vm_threads.h"
#include "interpreter.h"
#include "exceptions_jit.h"
#include "compile.h"


int vm_initialize_signals();
int vm_shutdown_signals();

Boolean null_reference_handler(port_sigtype signum, Registers* regs, void* fault_addr);
Boolean stack_overflow_handler(port_sigtype signum, Registers* regs, void* fault_addr);
Boolean abort_handler(port_sigtype signum, Registers* regs, void* fault_addr);
Boolean ctrl_backslash_handler(port_sigtype signum, Registers* regs, void* fault_addr);
Boolean ctrl_break_handler(port_sigtype signum, Registers* regs, void* fault_addr);
Boolean ctrl_c_handler(port_sigtype signum, Registers* regs, void* fault_addr);
Boolean native_breakpoint_handler(port_sigtype signum, Registers* regs, void* fault_addr);
Boolean arithmetic_handler(port_sigtype signum, Registers* regs, void* fault_addr);


inline bool is_in_ti_handler(vm_thread_t vmthread, void* ip)
{
    if (!vmthread)
        return false;

    POINTER_SIZE_INT break_buf =
        (POINTER_SIZE_INT)(vmthread->jvmti_thread.jvmti_jit_breakpoints_handling_buffer);

    return ((POINTER_SIZE_INT)ip >= break_buf &&
            (POINTER_SIZE_INT)ip < break_buf + TM_JVMTI_MAX_BUFFER_SIZE);
}

inline bool is_in_java(Registers* regs)
{
    return (vm_identify_eip(regs->get_ip()) == VM_TYPE_JAVA);
}

inline void signal_throw_exception(Registers* regs, Class* exc_clss)
{
    vm_set_exception_registers(p_TLS_vmthread, *regs);
    exn_athrow_regs(regs, exc_clss, is_in_java(regs), false);
}

inline void signal_throw_java_exception(Registers* regs, Class* exc_clss)
{
    ASSERT_NO_INTERPRETER;
    assert(is_in_java(regs));

    vm_set_exception_registers(p_TLS_vmthread, *regs);
    exn_athrow_regs(regs, exc_clss, true, false);
}


#endif //!__SIGNALS_H__
