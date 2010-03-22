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

#include <open/hythread_ext.h>
#include <thread_helpers.h>

#include "platform_lowlevel.h"
#include "open/vm_util.h"
#include "encoder.h"
#include "nogc.h"
#include "compile.h"

#include "exceptions_jit.h"
#include "lil.h"
#include "lil_code_generator.h"
#include "../m2n_ia32_internal.h"
#include "object_handles.h"
#include "Class.h"
#include "jit_runtime_support.h"
#include "internal_jit_intf.h"

#include "dump.h"
#include "vm_stats.h"


char * gen_convert_managed_to_unmanaged_null_ia32(char * ss, 
                                                  unsigned stack_pointer_offset);

#define INPUT_ARG_OFFSET 4

/*
 * Helper for monenter intstruction
 */
static char * gen_restore_monitor_enter(char *ss, char *patch_addr_null_arg)
{
    
    // Obtain lockword offset for the given object
    const unsigned header_offset = ManagedObject::header_offset();
    signed offset;
    assert(header_offset);
#ifdef VM_STATS
    ss = inc(ss,  M_Opnd((unsigned)&(VM_Statistics::get_vm_stats().num_monitor_enter)));
#endif
    ss = mov(ss,  ecx_opnd,  M_Base_Opnd(esp_reg, INPUT_ARG_OFFSET));
    
#ifdef _DEBUG_CHECK_NULL_ //_DEBUG    
    //npe check
    ss = test(ss,  ecx_opnd,   ecx_opnd);
    ss = branch8(ss, Condition_Z,  Imm_Opnd(size_8, 0));
    char *backpatch_address__null_pointer = ((char *)ss) - 1;
#endif

    // skip fast path if can_generate_monitor_events capability
    // was requested, so all TI events will be generated
    if (!VM_Global_State::loader_env->TI->get_global_capability(
                DebugUtilsTI::TI_GC_ENABLE_MONITOR_EVENTS)) {
        ss = alu(ss, add_opc, ecx_opnd, Imm_Opnd(header_offset)); // pop parameters
        ss = gen_monitorenter_fast_path_helper(ss, ecx_opnd);
        ss = test(ss,  eax_opnd,   eax_opnd);
        ss = branch8(ss, Condition_NZ,  Imm_Opnd(size_8, 0));
        char *backpatch_address__fast_monitor_failed = ((char *)ss) - 1;
        ss = ret(ss,  Imm_Opnd(4));

        offset = (signed)ss - (signed)backpatch_address__fast_monitor_failed - 1;
        *backpatch_address__fast_monitor_failed = (char)offset;
    }

    // Slow path: happens when the monitor is busy (contention case)
    ss = gen_setup_j2n_frame(ss);
    ss = mov(ss, eax_opnd,  M_Base_Opnd(esp_reg, m2n_sizeof_m2n_frame));
    ss = gen_monitorenter_slow_path_helper(ss, eax_opnd);
    ss = gen_pop_j2n_frame(ss);

    ss = ret(ss,  Imm_Opnd(4));

#ifdef _DEBUG_CHECK_NULL_//_DEBUG    
    // Handle NPE here
    signed npe_offset = (signed)ss - (signed)backpatch_address__null_pointer - 1;
    *backpatch_address__null_pointer = (char)npe_offset;
    if (patch_addr_null_arg != NULL) {
        npe_offset = (signed)ss - (signed)patch_addr_null_arg - 1;
        *patch_addr_null_arg = (char)npe_offset;
    }
    // Object is null so throw a null pointer exception
    ss = jump(ss, (char*)exn_get_rth_throw_null_pointer());
#endif
    return ss;
} //gen_restore_monitor_enter

static char * gen_restore_monitor_exit(char *ss, char *patch_addr_null_arg)
{

    const unsigned header_offset = ManagedObject::header_offset();
#ifdef VM_STATS
    ss = inc(ss,  M_Opnd((unsigned)&(VM_Statistics::get_vm_stats().num_monitor_enter)));
#endif

    ss = mov(ss,  ecx_opnd,  M_Base_Opnd(esp_reg, INPUT_ARG_OFFSET));

#ifdef _DEBUG_CHECK_NULL_//_DEBUG    
    //check npe
    ss = test(ss,  ecx_opnd,   ecx_opnd);
    ss = branch8(ss, Condition_Z,  Imm_Opnd(size_8, 0));
    char *backpatch_address__null_pointer = ((char *)ss) - 1;
#endif

    // skip fast path if can_generate_monitor_events capability
    // was requested, so all TI events will be generated
    if (!VM_Global_State::loader_env->TI->get_global_capability(
                DebugUtilsTI::TI_GC_ENABLE_MONITOR_EVENTS)) {
        ss = alu(ss, add_opc, ecx_opnd, Imm_Opnd(header_offset));
        ss = gen_monitor_exit_helper(ss, ecx_opnd);
    } else {
        ss = gen_setup_j2n_frame(ss);
        ss = push(ss,  M_Base_Opnd(esp_reg, m2n_sizeof_m2n_frame));

        ss = call(ss, (char *)oh_convert_to_local_handle);
        ss = alu(ss, add_opc, esp_opnd, Imm_Opnd(4)); // pop parameters
        ss = gen_monitorexit_slow_path_helper(ss, eax_opnd);
        ss = gen_pop_j2n_frame(ss);
    }
    ss = test(ss,  eax_opnd,   eax_opnd);
    ss = branch8(ss, Condition_NZ,  Imm_Opnd(size_8, 0));
    char *backpatch_address__fast_monitor_failed = ((char *)ss) - 1;
    ss = ret(ss,  Imm_Opnd(4));

    signed offset = (signed)ss - (signed)backpatch_address__fast_monitor_failed - 1;
    *backpatch_address__fast_monitor_failed = (char)offset;
    //  illegal state happend
    ss = jump(ss, (char*)exn_get_rth_throw_illegal_state_exception());

#ifdef _DEBUG_CHECK_NULL_//_DEBUG    
    //NPE
    offset = (signed)ss - (signed)backpatch_address__null_pointer - 1;
    *backpatch_address__null_pointer = (char)offset;
    if (patch_addr_null_arg != NULL) {
        offset = (signed)ss - (signed)patch_addr_null_arg - 1;
        *patch_addr_null_arg = (char)offset;
    }
    // Object is null so throw a null pointer exception
    ss = jump(ss, (char*)exn_get_rth_throw_null_pointer());
#endif     
    return ss;
  
} //gen_restore_monitor_exit


void * getaddress__vm_monitor_enter_naked()
{
    static void *addr = NULL;
    if (addr != NULL) {
        return addr;
    }

    const int stub_size = 226;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_MAX/2, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;

#ifdef VM_STATS
    int * value = VM_Statistics::get_vm_stats().rt_function_calls.lookup_or_add((void*)VM_RT_MONITOR_ENTER, 0, NULL);
    ss = inc(ss,  M_Opnd((unsigned)value));
#endif

    ss = gen_restore_monitor_enter(ss, /*patch_addr_null_arg*/ NULL);

    addr = stub;
    assert((ss - stub) < stub_size);

    compile_add_dynamic_generated_code_chunk("vm_monitor_enter_naked", false, stub, stub_size);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("vm_monitor_enter_naked", stub, stub_size);
    }

    DUMP_STUB(stub, "getaddress__vm_monitor_enter_naked", ss - stub);

    return addr;
}


void * getaddress__vm_monitor_exit_naked()
{
    static void *addr = NULL;
    if (addr != NULL) {
        return addr;
    }

    const int stub_size = /*126*/210;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_MAX/2, CAA_Allocate);
    char *ss = stub;

#ifdef VM_STATS
    int * value = VM_Statistics::get_vm_stats().rt_function_calls.lookup_or_add((void*)VM_RT_MONITOR_EXIT, 0, NULL);
    ss = inc(ss,  M_Opnd((unsigned)value));
#endif

    ss = gen_convert_managed_to_unmanaged_null_ia32((Emitter_Handle)ss, /*stack_pointer_offset*/ INPUT_ARG_OFFSET);
    ss = gen_restore_monitor_exit(ss, /*patch_addr_null_arg*/ NULL);

    addr = stub;
    assert((ss - stub) < stub_size);

    compile_add_dynamic_generated_code_chunk("vm_monitor_exit_naked", false, stub, stub_size);

    if (jvmti_should_report_event(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        jvmti_send_dynamic_code_generated_event("vm_monitor_exit_naked", stub, stub_size);
    }

    DUMP_STUB(stub, "getaddress__vm_monitor_exit_naked", ss - stub);

    return addr;
} //getaddress__vm_monitor_exit_naked

