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

/*    MONITOR ENTER RUNTIME SUPPORT    */

#define LOG_DOMAIN "vm.helpers"
#include "cxxlog.h"

#include "environment.h"

#include "open/hythread_ext.h"
#include "jthread.h"
#include "thread_helpers.h"

//#include "open/vm_util.h"
#include "encoder.h"
#include "nogc.h"
#include "compile.h"

#include "exceptions_jit.h"
#include "lil.h"
#include "lil_code_generator.h"
#include "../m2n_em64t_internal.h"
#include "object_handles.h"
#include "Class.h"
#include "jit_runtime_support.h"
#include "internal_jit_intf.h"

#include "mon_enter_exit.h"
#include "exceptions.h"
#include "exceptions_jit.h"

#include "dump.h"
#include "vm_stats.h"

// Linix x86-64 fast helpers
char * gen_convert_managed_to_unmanaged_null_em64t(char * ss,
                                                  const R_Opnd & input_param1);
#define INPUT_ARG_OFFSET 8

// Helper for monenter intstruction
static char * gen_restore_monitor_enter(char *ss, char *patch_addr_null_arg)
{
    // Obtain lockword offset for the given object
    const unsigned header_offset = ManagedObject::header_offset();
    signed offset;
    assert(header_offset);
#ifdef VM_STATS
//    uint64* incr = &(VM_Statistics::get_vm_stats().num_monitor_enter);
//    ss = inc(ss,  M_Opnd((int64)incr));
#endif

#ifdef _DEBUG_CHECK_NULL_//_DEBUG
    // check on the null
    ss = test(ss,  rdi_opnd,   rdi_opnd);
    ss = branch8(ss, Condition_Z,  Imm_Opnd(size_8, 0));
    char *backpatch_address__null_pointer = ((char *)ss) - 1;
#endif

    // skip fast path if can_generate_monitor_events capability
    // was requested, so all TI events will be generated
    if (!VM_Global_State::loader_env->TI->get_global_capability(
                DebugUtilsTI::TI_GC_ENABLE_MONITOR_EVENTS)) {
        // Fast path
        ss = push(ss, rdi_opnd);
        ss = alu(ss, add_opc, rdi_opnd, Imm_Opnd(header_offset)); // pop parameters
        ss = gen_monitorenter_fast_path_helper(ss, rdi_opnd);
        ss = pop(ss, rdi_opnd);

        ss = test(ss,  rax_opnd,   rax_opnd);
        ss = branch8(ss, Condition_NZ,  Imm_Opnd(size_8, 0));
        char *backpatch_address__fast_monitor_failed = ((char *)ss) - 1;
        ss = ret(ss);

        offset = (int64)ss - (int64)backpatch_address__fast_monitor_failed - 1;
        *backpatch_address__fast_monitor_failed = (char)offset;
    }

    // Slow path: happens when the monitor is busy (contention case)
    ss = gen_setup_j2n_frame(ss);
    ss = gen_monitorenter_slow_path_helper(ss, rdi_opnd);
    ss = gen_pop_j2n_frame(ss);

    ss = ret(ss);

#ifdef _DEBUG_CHECK_NULL_//_DEBUG
    // Handle NPE here
    int64 npe_offset = (int64)ss - (int64)backpatch_address__null_pointer - 1;
    *backpatch_address__null_pointer = (char)npe_offset;
    if (patch_addr_null_arg != NULL) {
        npe_offset = (int64)ss - (int64)patch_addr_null_arg - 1;
        *patch_addr_null_arg = (char)npe_offset;
    }

    // Object is null so throw a null pointer exception
    ss = jump(ss, (char*)exn_get_rth_throw_null_pointer());
#endif
    return ss;
} //gen_restore_monitor_enter

void * getaddress__vm_monitor_enter_naked()
{
    static void *addr = NULL;
    if (addr != NULL) {
        return addr;
    }

    const int stub_size = 192;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_MAX/2, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, stub_size);
#endif
    char *ss = stub;

#ifdef VM_STATS
    int * value = VM_Statistics::get_vm_stats().rt_function_calls.lookup_or_add((void*)VM_RT_MONITOR_ENTER, 0, NULL);
    ss = mov(ss,  rax_opnd,  Imm_Opnd(size_64, (int64)value));
    ss = inc(ss,  M_Base_Opnd(rax_reg, 0));
#endif

    ss = gen_restore_monitor_enter(ss, /*patch_addr_null_arg*/ NULL);

    addr = stub;
    assert((ss - stub) < stub_size);

    compile_add_dynamic_generated_code_chunk("vm_monitor_enter_naked", false, stub, stub_size);

    // Put TI support here
    DUMP_STUB(stub, "getaddress__vm_monitor_enter_naked", ss - stub);

    return addr;
}


static char * gen_restore_monitor_exit(char *ss, char *patch_addr_null_arg)
{

    const unsigned header_offset = ManagedObject::header_offset();
#ifdef VM_STATS
//    uint64* incr = &(VM_Statistics::get_vm_stats().num_monitor_exit);
//    ss = inc(ss,  M_Opnd((int64)incr));
#endif

#ifdef _DEBUG_CHECK_NULL_//_DEBUG
    // check on teh null
    ss = test(ss, rdi_opnd, rdi_opnd);
    ss = branch8(ss, Condition_Z,  Imm_Opnd(size_8, 0));
    char *backpatch_address__null_pointer = ((char *)ss) - 1;
#endif

    // skip fast path if can_generate_monitor_events capability
    // was requested, so all TI events will be generated
    if (!VM_Global_State::loader_env->TI->get_global_capability(
                DebugUtilsTI::TI_GC_ENABLE_MONITOR_EVENTS)) {
        // Fast path
        ss = alu(ss, add_opc, rdi_opnd, Imm_Opnd(header_offset));
        ss = gen_monitor_exit_helper(ss, rdi_opnd);
    } else {
        // Slow path
        ss = gen_setup_j2n_frame(ss);

        ss = call(ss, (char *)oh_convert_to_local_handle);
        ss = gen_monitorexit_slow_path_helper(ss, rax_opnd);

        ss = gen_pop_j2n_frame(ss);
    }
    ss = test(ss,  rax_opnd,   rax_opnd);
    ss = branch8(ss, Condition_NZ,  Imm_Opnd(size_8, 0));
    char *backpatch_address__fast_monitor_failed = ((char *)ss) - 1;
    ss = ret(ss);

    POINTER_SIZE_SINT offset = (POINTER_SIZE_SINT)ss - (POINTER_SIZE_SINT)backpatch_address__fast_monitor_failed - 1;
    *backpatch_address__fast_monitor_failed = (char)offset;

    //  Monitor illegal state happend
    ss = jump(ss, (char*)exn_get_rth_throw_illegal_state_exception());

#ifdef _DEBUG_CHECK_NULL_//_DEBUG
    //NPE
    offset = (POINTER_SIZE_SINT)ss - (POINTER_SIZE_SINT)backpatch_address__null_pointer - 1;
    *backpatch_address__null_pointer = (char)offset;
    if (patch_addr_null_arg != NULL) {
        offset = (POINTER_SIZE_SINT)ss - (POINTER_SIZE_SINT)patch_addr_null_arg - 1;
        *patch_addr_null_arg = (char)offset;
    }

    // Object is null so throw a null pointer exception
    ss = jump(ss, (char*)exn_get_rth_throw_null_pointer());
#endif
    return ss;
} //gen_restore_monitor_exit

void * getaddress__vm_monitor_exit_naked()
{
    static void *addr = NULL;
    if (addr != NULL) {
        return addr;
    }

    const int stub_size = 144;
    char *stub = (char *)malloc_fixed_code_for_jit(stub_size, DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_MAX/2, CAA_Allocate);
    char *ss = stub;

#ifdef VM_STATS
    int * value = VM_Statistics::get_vm_stats().rt_function_calls.lookup_or_add((void*)VM_RT_MONITOR_EXIT, 0, NULL);
    ss = mov(ss,  rax_opnd,  Imm_Opnd(size_64, (int64)value));
    ss = inc(ss,  M_Base_Opnd(rax_reg, 0));
#endif

    ss = gen_convert_managed_to_unmanaged_null_em64t((Emitter_Handle)ss, rdi_opnd);
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

// Windows x86-64 helpers
static LilCodeStub * rth_get_lil_monitor_enter_generic(LilCodeStub * cs) {
    if(VM_Global_State::loader_env->TI->isEnabled() &&
            VM_Global_State::loader_env->TI->get_global_capability(
                            DebugUtilsTI::TI_GC_ENABLE_MONITOR_EVENTS) ) {
        return lil_parse_onto_end(cs,
            "push_m2n 0, 0;"
            "out platform:ref:void;"
            "o0 = l0;"
            "call %0i;"
            "pop_m2n;"
            "ret;",
            vm_monitor_enter);
    } else {
        return lil_parse_onto_end(cs,
            "out platform:ref:g4;"
            "o0 = l0;"
            "call %0i;"
            "jc r!=%1i,slow_path;"
            "ret;"
            ":slow_path;"
            "push_m2n 0, 0;"
            "out platform:ref:void;"
            "o0 = l0;"
            "call %2i;"
            "pop_m2n;"
            "ret;",
            vm_monitor_try_enter,
            (POINTER_SIZE_INT)TM_ERROR_NONE,
            vm_monitor_enter);
    }
}

NativeCodePtr rth_get_lil_monitor_enter() {
    static NativeCodePtr addr = NULL;
    
    if (addr != NULL) {
        return addr;
    }    

    LilCodeStub * cs = lil_parse_code_stub("entry 0:stdcall:ref:void;");

#ifdef VM_STATS
//    int * value = VM_Statistics::get_vm_stats().rt_function_calls.lookup_or_add((void*)VM_RT_MONITOR_ENTER, 0, NULL);
//    cs = lil_parse_onto_end(cs, "inc [%0i:pint];", value);
//    assert(cs);
#endif

#ifdef _DEBUG_CHECK_NULL_//_DEBUG
    // check if object is null
    cs = lil_parse_onto_end(cs,
        "jc i0 = %0i:ref, throw_null_pointer;",
        (ManagedObject *) VM_Global_State::loader_env->managed_null
    );
    assert(cs);
#endif

    cs = lil_parse_onto_end(cs,
        "locals 1;"
        "l0 = i0;"
    );
    assert(cs);

    // append generic monitor enter code
    cs = rth_get_lil_monitor_enter_generic(cs);
    assert(cs);

#ifdef _DEBUG_CHECK_NULL_//_DEBUG    
    // throw NullPointerException
    cs = lil_parse_onto_end(cs,
        ":throw_null_pointer;"
        "out stdcall::void;"
        "call.noret %0i;",
        lil_npc_to_fp(exn_get_rth_throw_null_pointer())
    );
    assert(cs && lil_is_valid(cs));
#endif

    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB((char *)addr, "monitor_enter", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);
    return addr;
}

/*    MONITOR EXIT RUNTIME SUPPORT    */

static LilCodeStub * rth_get_lil_monitor_exit_generic(LilCodeStub * cs) {
    if(VM_Global_State::loader_env->TI->isEnabled() &&
            VM_Global_State::loader_env->TI->get_global_capability(
                            DebugUtilsTI::TI_GC_ENABLE_MONITOR_EVENTS) ) {
        return lil_parse_onto_end(cs,
            "locals 1;"
            "l0 = o0;"
            "push_m2n 0, %0i;"
            "out platform:ref:void;"
            "o0 = l0;"
            "call %1i;"
            "pop_m2n;"
            "ret;",
            (POINTER_SIZE_INT)FRAME_NON_UNWINDABLE,
            vm_monitor_exit);
    } else {
        return lil_parse_onto_end(cs,
            "call %0i;"
            "jc r!=%1i, illegal_monitor;"
            "ret;"
            ":illegal_monitor;"
            "out stdcall::void;"
            "call.noret %2i;",
            vm_monitor_try_exit,
            (POINTER_SIZE_INT)TM_ERROR_NONE,
            lil_npc_to_fp(exn_get_rth_throw_illegal_monitor_state()));
    }
}


NativeCodePtr rth_get_lil_monitor_exit() {
    static NativeCodePtr addr = NULL;
    
    if (addr != NULL) {
        return addr;
    }    

    LilCodeStub * cs = lil_parse_code_stub("entry 0:stdcall:ref:void;");

#ifdef VM_STATS
//    int * value = VM_Statistics::get_vm_stats().rt_function_calls.lookup_or_add((void*)VM_RT_MONITOR_EXIT, 0, NULL);
//    cs = lil_parse_onto_end(cs, "inc [%0i:pint];", value);
//    assert(cs);
#endif

#ifdef _DEBUG_CHECK_NULL_//_DEBUG    
    // check if object is null
    cs = lil_parse_onto_end(cs,
        "jc i0 = %0i:ref, throw_null_pointer;",
        (ManagedObject *) VM_Global_State::loader_env->managed_null
    );
    assert(cs);
#endif

    cs = lil_parse_onto_end(cs,
        "in2out platform:g4;"
    );
    assert(cs);
    
    // append generic monitor enter code
    cs = rth_get_lil_monitor_exit_generic(cs);
    assert(cs);

#ifdef _DEBUG_CHECK_NULL_//_DEBUG    
    // throw NullPointerException
    cs = lil_parse_onto_end(cs,
        ":throw_null_pointer;"
        "out stdcall::void;"
        "call.noret %0i;",
        lil_npc_to_fp(exn_get_rth_throw_null_pointer())
    );
    assert(cs && lil_is_valid(cs));
#endif

    addr = LilCodeGenerator::get_platform()->compile(cs);

    DUMP_STUB(addr, "monitor_exit", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);
    return addr;
}

