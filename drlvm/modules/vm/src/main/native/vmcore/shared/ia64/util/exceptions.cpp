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
 * @author Intel, Pavel Afremov
 */  

//MVM
#include <iostream>

using namespace std;

#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include <float.h>

#include "open/types.h"
#include "jit_runtime_support.h"
#include "Class.h"
#include "environment.h"
#include "m2n.h"
#include "../m2n_ipf_internal.h"
#include "object_layout.h"
#include "open/vm_util.h"
#include "vm_threads.h"
#include "jit_intf.h"
#include "jit_intf_cpp.h"
#include "compile.h"
#include "ini.h"
#include "vm_stats.h"
#include "vm_ipf.h"
#include "Code_Emitter.h"
#include "stub_code_utils.h"
#include "nogc.h"

#include "exceptions.h"
#include "exceptions_jit.h"

void gen_convert_managed_to_unmanaged_null_ipf(Emitter_Handle emitter,
                                               unsigned reg);

extern "C" void vm_rt_athrow(volatile ManagedObject *exc_obj,
                              uint64 *bsp_arg,
                              Class *clss)
{
    exn_athrow((ManagedObject*)exc_obj, clss);
} //vm_rt_athrow



void gen_vm_rt_athrow_internal_compactor(Merced_Code_Emitter &emitter)
{
    const int out_arg0 = m2n_gen_push_m2n(&emitter, NULL, FRAME_UNKNOWN, false, 0, 0, 4);
    m2n_gen_save_extra_preserved_registers(&emitter);
    emitter.ipf_mov(out_arg0+0,  IN_REG0);
    emitter.ipf_mov(out_arg0+1,  IN_REG1);
    emitter.ipf_mov(out_arg0+2,  0);
    emitter.ipf_mov(out_arg0+3,  0);
    emit_call_with_gp(emitter, (void **)exn_athrow, true, 5);
} //gen_vm_rt_athrow_internal_compactor



void *get_vm_rt_athrow_naked_compactor()
{
    static void *addr = 0;
    if (addr) {
        return addr;
    }
    tl::MemoryPool mem_pool;
    Merced_Code_Emitter emitter(mem_pool, 2, 0);
    emitter.disallow_instruction_exchange();
    emitter.memory_type_is_unknown();

    // If compressing references, convert the first argument reference, if null, from a managed null (represented by heap_base)
    // to an unmanaged one (NULL/0). 
    gen_convert_managed_to_unmanaged_null_ipf((Emitter_Handle)&emitter, IN_REG0);

    // Set the second argument passed to vm_rt_athrow() to NULL. This would be 
    // the exception class used for lazy exceptions, but we already have an exception object.
    emitter.ipf_alloc(SCRATCH_GENERAL_REG, 2, 0, 0, 0);
    emitter.ipf_mov(IN_REG1, 0);
    
    // Control drops through to common code that does a procedure call to vm_rt_athrow().
    gen_vm_rt_athrow_internal_compactor(emitter);
    
    emitter.flush_buffer();
    size_t stub_size = emitter.get_size();
    void *stub = (void *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_COLD, CAA_Allocate);
    emitter.copy((char *)stub);
    flush_hw_cache((U_8*)stub, stub_size);
    sync_i_cache();
    addr = stub;
    return stub;
} //get_vm_rt_athrow_naked_compactor



static void *gen_vm_rt_exception_throw_compactor(Class *exc, char *stub_name)
{
    tl::MemoryPool mem_pool;
    Merced_Code_Emitter emitter(mem_pool, 2, 0);
    emitter.disallow_instruction_exchange();
    emitter.memory_type_is_unknown();

    // args to vm_rt_athrow: NULL and exc class
    emitter.ipf_alloc(SCRATCH_GENERAL_REG, 2, 0, 0, 0);
    emitter.ipf_mov(IN_REG0, 0);
    emit_movl_compactor(emitter, IN_REG1, (uint64)exc);

    // Control drops through to common code that does a procedure call to vm_rt_athrow().
    gen_vm_rt_athrow_internal_compactor(emitter);

    emitter.flush_buffer();
    size_t stub_size = emitter.get_size();
    void *stub = (void *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_COLD, CAA_Allocate);
    emitter.copy((char *)stub);
    flush_hw_cache((U_8*)stub, stub_size);
    sync_i_cache();

    return stub;
} //gen_vm_rt_exception_throw_compactor



static void *gen_vm_rt_exception_throw(Class *exc, char *stub_name)
{
    void *stub = NULL;
    stub = gen_vm_rt_exception_throw_compactor(exc, stub_name);
    assert(stub);
    return stub;
} //gen_vm_rt_exception_throw

void *get_vm_rt_null_ptr_exception_address()
{
    static void *addr = 0;
    if(addr) {
        return addr;
    }
    Class * exc_clss = VM_Global_State::loader_env->java_lang_NullPointerException_Class;

    addr = gen_vm_rt_exception_throw(exc_clss, "rt_null_ptr_exception");

    return addr;
} //get_vm_rt_null_ptr_exception_address
