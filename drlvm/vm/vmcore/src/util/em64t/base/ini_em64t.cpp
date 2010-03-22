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
#define LOG_DOMAIN "vm.helpers"
#include "cxxlog.h"

#include "jni_types.h"
#include "jit_intf.h"
#include "open/types.h"
#include "open/em.h"

#include "environment.h"
#include "exceptions.h"
#include "Class.h"
#include "object_handles.h"
#include "nogc.h"

#include "port_malloc.h"
#include "tl/memory_pool.h"
#include "encoder.h"
#include "ini.h"
#include "lil_code_generator_utils.h"
#include "jit_runtime_support_common.h"

#include "dump.h"

#if defined(WIN32) || defined(_WIN64)

// four fake parameters should be passed over GR
#define FAKE_PARAMETERS void *, void *, void *, void *
#define FAKE_ARGUMENTS  NULL,   NULL,   NULL,   NULL

#else // defined(WIN32) || defined(_WIN64)

// six fake parameters should be passed over GR
#define FAKE_PARAMETERS void *, void *, void *, void *, void *, void *
#define FAKE_ARGUMENTS  NULL,   NULL,   NULL,   NULL,   NULL,   NULL

#endif // defined(WIN32) || defined(_WIN64)


typedef int64 ( * invoke_managed_func_int_t) (
    FAKE_PARAMETERS,
    const void * const method_entry_point,
    int64 gr_nargs, int64 fr_nargs, int64 stack_nargs,
    uint64 gr_args[], double fr_args[], uint64 stack_args[]);

typedef double ( * invoke_managed_func_double_t)(
    FAKE_PARAMETERS,
    const void * const method_entry_point,
    int64 gr_nargs, int64 fr_nargs, int64 stack_nargs,
    uint64 gr_args[], double fr_args[], uint64 stack_args[]);

static invoke_managed_func_int_t gen_invoke_managed_func() {
    static invoke_managed_func_int_t func = NULL;
    if (func) {
        return func;
    }

    const char * MOVE_STACK_ARGS_BEGIN = "move_stack_args_begin";
    const char * MOVE_STACK_ARGS_END = "move_stack_args_end";
    const char * MOVE_FR_ARGS_END = "move_fr_args_end";
    const char * COMPUTE_ADDRESS = "compute_address";

    // [rbp + 16] - method_entry_point
    // [rbp + 24] - gr_nargs
    // [rbp + 32] - fr_nargs
    // [rbp + 40] - stack_nargs
    // [rbp + 48] - gr_args
    // [rbp + 56] - fr_args
    // [rbp + 64] - stack_args
    const I_32 METHOD_ENTRY_POINT_OFFSET = 16 + SHADOW;
    const I_32 UNUSED GR_NARGS_OFFSET = 24 + SHADOW;
    const I_32 FR_NARGS_OFFSET = 32 + SHADOW;
    const I_32 STACK_NARGS_OFFSET = 40 + SHADOW;
    const I_32 GR_ARGS_OFFSET = 48 + SHADOW;
    const I_32 FR_ARGS_OFFSET = 56 + SHADOW;
    const I_32 STACK_ARGS_OFFSET = 64 + SHADOW;
    
    const int STUB_SIZE = 200;
    char * stub = (char *) malloc_fixed_code_for_jit(STUB_SIZE,
        DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_DEFAULT, CAA_Allocate);
#ifdef _DEBUG
    memset(stub, 0xcc /*int 3*/, STUB_SIZE);
#endif
    
    tl::MemoryPool pool;
    LilCguLabelAddresses labels(&pool, stub);
    
    func = (invoke_managed_func_int_t) stub;
    stub = push(stub, rbp_opnd);
    stub = mov(stub, rbp_opnd, rsp_opnd);

    assert(MANAGED_STACK_ALIGNMENT == STACK_ALIGN_HALF16);

    // align stack pointer if required (rsp % 16 == 0)
    stub = alu(stub, and_opc, rsp_opnd, Imm_Opnd(0xfffffff0));

    // 1) move stacked arguments in reverse (right-to-left) order
    stub  = mov(stub, rcx_opnd, M_Base_Opnd(rbp_reg, STACK_NARGS_OFFSET));
    stub = alu(stub, or_opc, rcx_opnd, rcx_opnd);
    stub = branch8(stub, Condition_Z, Imm_Opnd(size_8, 0));
    labels.add_patch_to_label(MOVE_STACK_ARGS_END, stub - 1, LPT_Rel8);
    
    // align stack arguments if required (rsp % 16 == 0)
    stub = test(stub, rcx_opnd, Imm_Opnd(size_32, 1));
    stub = branch8(stub, Condition_Z, Imm_Opnd(size_8, 0));
    labels.add_patch_to_label(COMPUTE_ADDRESS, stub - 1, LPT_Rel8);
    stub = push(stub, rax_opnd);
    
    // compute effective address of the last stacked argument
    labels.define_label(COMPUTE_ADDRESS, stub, false);
    
    stub = mov(stub, r10_opnd, M_Base_Opnd(rbp_reg, STACK_ARGS_OFFSET));
    stub = lea(stub, r10_opnd, M_Index_Opnd(r10_reg, rcx_reg, -8, 8));
    stub = alu(stub, sub_opc, r10_opnd, rsp_opnd);
    
    // iterate through the arguments
    labels.define_label(MOVE_STACK_ARGS_BEGIN, stub, false);
    
    stub = push(stub, M_Index_Opnd(rsp_reg, r10_reg, 0, 1));
    stub = loop(stub, Imm_Opnd(size_8, 0));
    labels.add_patch_to_label(MOVE_STACK_ARGS_BEGIN, stub - 1, LPT_Rel8);
    
    labels.define_label(MOVE_STACK_ARGS_END, stub, false);

#ifdef _WIN64
    // 1a) WIN64, reserve 4 words of shadow space
    //stub = alu(stub, sub_opc, rsp_opnd, Imm_Opnd(SHADOW));
    // but jit doesn't suppoert it now
#endif

    // 2) move from fr_args to registers
    stub = mov(stub, rcx_opnd, M_Base_Opnd(rbp_reg, FR_NARGS_OFFSET));
    stub = alu(stub, or_opc, rcx_opnd, rcx_opnd);
    stub = branch8(stub, Condition_Z, Imm_Opnd(size_8, 0));
    labels.add_patch_to_label(MOVE_FR_ARGS_END, stub - 1, LPT_Rel8);

    stub = mov(stub, r10_opnd, M_Base_Opnd(rbp_reg, FR_ARGS_OFFSET));
    stub = movq(stub, xmm0_opnd, M_Base_Opnd(r10_reg, 0 * FR_STACK_SIZE));
    stub = movq(stub, xmm1_opnd, M_Base_Opnd(r10_reg, 1 * FR_STACK_SIZE));
    stub = movq(stub, xmm2_opnd, M_Base_Opnd(r10_reg, 2 * FR_STACK_SIZE));
    stub = movq(stub, xmm3_opnd, M_Base_Opnd(r10_reg, 3 * FR_STACK_SIZE));
#ifndef _WIN64
    stub = movq(stub, xmm4_opnd, M_Base_Opnd(r10_reg, 4 * FR_STACK_SIZE));
    stub = movq(stub, xmm5_opnd, M_Base_Opnd(r10_reg, 5 * FR_STACK_SIZE));
    stub = movq(stub, xmm6_opnd, M_Base_Opnd(r10_reg, 6 * FR_STACK_SIZE));
    stub = movq(stub, xmm7_opnd, M_Base_Opnd(r10_reg, 7 * FR_STACK_SIZE));
#endif

    labels.define_label(MOVE_FR_ARGS_END, stub, false);
    
    // 3) unconditionally move from gr_args to registers
#ifdef _WIN64
    stub = mov(stub, r10_opnd, M_Base_Opnd(rbp_reg, GR_ARGS_OFFSET));
    stub = mov(stub, rcx_opnd, M_Base_Opnd(r10_reg, 0 * GR_STACK_SIZE));
    stub = mov(stub, rdx_opnd, M_Base_Opnd(r10_reg, 1 * GR_STACK_SIZE));
    stub = mov(stub, r8_opnd, M_Base_Opnd(r10_reg, 2 * GR_STACK_SIZE));
    stub = mov(stub, r9_opnd, M_Base_Opnd(r10_reg, 3 * GR_STACK_SIZE));
#else
    stub = mov(stub, r10_opnd, M_Base_Opnd(rbp_reg, GR_ARGS_OFFSET));
    stub = mov(stub, rdi_opnd, M_Base_Opnd(r10_reg, 0 * GR_STACK_SIZE));
    stub = mov(stub, rsi_opnd, M_Base_Opnd(r10_reg, 1 * GR_STACK_SIZE));
    stub = mov(stub, rdx_opnd, M_Base_Opnd(r10_reg, 2 * GR_STACK_SIZE));
    stub = mov(stub, rcx_opnd, M_Base_Opnd(r10_reg, 3 * GR_STACK_SIZE));
    stub = mov(stub, r8_opnd, M_Base_Opnd(r10_reg, 4 * GR_STACK_SIZE));
    stub = mov(stub, r9_opnd, M_Base_Opnd(r10_reg, 5 * GR_STACK_SIZE));
#endif

    // 4) transfer control
    stub = mov(stub, r10_opnd, M_Base_Opnd(rbp_reg, METHOD_ENTRY_POINT_OFFSET));
    stub = call(stub, r10_opnd);

    // 5) leave current frame
    stub = mov(stub, rsp_opnd, rbp_opnd);
    stub = pop(stub, rbp_opnd);
    stub = ret(stub);

    assert(stub - (char *)func <= STUB_SIZE);

    DUMP_STUB(func, "invoke_managed_func", stub - (char *)func);

    return func;
}

void JIT_execute_method_default(JIT_Handle jh, jmethodID methodID,
                                jvalue * result, jvalue * args) {
    
    assert(!hythread_is_suspend_enabled());
    ASSERT_RAISE_AREA;

    static const invoke_managed_func_int_t invoke_managed_func = 
        (invoke_managed_func_int_t) gen_invoke_managed_func();

    // holds arguments that should be placed in GR's
    uint64 gr_args[MAX_GR];
    // holds arguments that should be placed in FR's
    double fr_args[MAX_FR];
    // gen_invoke_managed_func assumes such size
    assert(sizeof(double) == 8);

    Method * const method = (Method *)methodID;
    const void * const method_entry_point = method->get_code_addr();
    Arg_List_Iterator iter = method->get_argument_list();

    // hold arguments that should be placed on the memory stack
    uint64 * const stack_args = (uint64 *) STD_MALLOC(sizeof(uint64) * method->get_num_args());
    
    int64 gr_nargs = 0;
#ifdef _WIN64
#  define fr_nargs gr_nargs
#else
     int64 fr_nargs = 0;
#endif
    int64 stack_nargs = 0;
    int64 arg_num = 0;

    TRACE2("invoke", "enter method "
        << method->get_class()->get_name()->bytes << " "
        << method->get_name()->bytes << " "
        << method->get_descriptor());


    if(!method->is_static()) {
        ObjectHandle handle = (ObjectHandle) args[arg_num++].l;
        assert(handle);
        // convert from native to managed NULL
        gr_args[gr_nargs++] = (handle->object != NULL)
            ? (uint64) handle->object : (uint64) VM_Global_State::loader_env->managed_null;
    }

    Java_Type type;
    while((type = curr_arg(iter)) != JAVA_TYPE_END) {
        assert(gr_nargs <= MAX_GR);
        assert(fr_nargs <= MAX_FR);
        switch (type) {
        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY: {
            ObjectHandle handle = (ObjectHandle) args[arg_num++].l;
            uint64 ref = handle ? (uint64) handle->object : 0;
            // convert from native to managed NULL
            ref = ref ? ref : (uint64) VM_Global_State::loader_env->managed_null;
            if (gr_nargs < MAX_GR) {
                gr_args[gr_nargs++] = ref;
            } else {
                stack_args[stack_nargs++] = ref;
            }
            break;
        }
        case JAVA_TYPE_INT:
            // sign extension
            if (gr_nargs < MAX_GR) {
                gr_args[gr_nargs++] = (int64) args[arg_num++].i;
            } else {
                stack_args[stack_nargs++] = (int64) args[arg_num++].i;
            }
            break;
        case JAVA_TYPE_LONG:
            // sign extension
            if (gr_nargs < MAX_GR) {
                gr_args[gr_nargs++] = (int64) args[arg_num++].j;
            } else {
                stack_args[stack_nargs++] = (int64) args[arg_num++].j;
            }
            break;
        case JAVA_TYPE_SHORT:
            // sign extension
            if (gr_nargs < MAX_GR) {
                gr_args[gr_nargs++] = (int64) args[arg_num++].s;
            } else {
                stack_args[stack_nargs++] = (int64) args[arg_num++].s;
            }
            break;
        case JAVA_TYPE_CHAR:
            // zero extension
            if (gr_nargs < MAX_GR) {
                gr_args[gr_nargs++] = (uint64) args[arg_num++].c;
            } else {
                stack_args[stack_nargs++] = (uint64) args[arg_num++].c;
            }
            break;
        case JAVA_TYPE_BYTE:
            // sign extension
            if (gr_nargs < MAX_GR) {
                gr_args[gr_nargs++] = (int64) args[arg_num++].b;
            } else {
                stack_args[stack_nargs++] = (int64) args[arg_num++].b;
            }
            break;
        case JAVA_TYPE_BOOLEAN:
            // sign extension
            if (gr_nargs < MAX_GR) {
                gr_args[gr_nargs++] = (int64) args[arg_num++].z;
            } else {
                stack_args[stack_nargs++] = (int64) args[arg_num++].z;
            }
            break;
        case JAVA_TYPE_DOUBLE:
            if (fr_nargs < MAX_FR) {
                fr_args[fr_nargs++] = args[arg_num++].d;
            } else {
                *(double *)(stack_args + stack_nargs) = args[arg_num++].d;
                ++stack_nargs;
            }
            break;
        case JAVA_TYPE_FLOAT:
            if (fr_nargs < MAX_FR) {
                *(float *)&fr_args[fr_nargs++] = args[arg_num++].f;
            } else {
                *(float *)(stack_args + stack_nargs) = args[arg_num++].f;
                ++stack_nargs;
            }
            break;
        default:
            LDIE(31, "INTERNAL ERROR: Unexpected type of the argument: {0}" << type);
        }
        iter = advance_arg_iterator(iter);
    }

    // Save the result
    type = method->get_return_java_type();
    switch(type) {
    case JAVA_TYPE_VOID:
        invoke_managed_func(FAKE_ARGUMENTS,
            method_entry_point,
            gr_nargs,  fr_nargs, stack_nargs,
            gr_args, fr_args, stack_args);
        break;
    case JAVA_TYPE_ARRAY:
    case JAVA_TYPE_CLASS: {
        ObjectHandle handle = NULL;
        uint64 ref = invoke_managed_func(FAKE_ARGUMENTS,
            method_entry_point,
            gr_nargs,  fr_nargs, stack_nargs,
            gr_args, fr_args, stack_args);
        // convert from managed to native NULL
        ref = (ref != (uint64) VM_Global_State::loader_env->managed_null) ? ref : (uint64) NULL;
        if (ref) {
            handle = oh_allocate_local_handle();
            handle->object = (ManagedObject*) ref;
        }
        result->l = handle;
        break;
    }
    case JAVA_TYPE_LONG:
    case JAVA_TYPE_INT:
    case JAVA_TYPE_SHORT:
    case JAVA_TYPE_CHAR:
    case JAVA_TYPE_BYTE:
    case JAVA_TYPE_BOOLEAN:
        result->j = invoke_managed_func(FAKE_ARGUMENTS,
            method_entry_point,
            gr_nargs,  fr_nargs, stack_nargs,
            gr_args, fr_args, stack_args);
        break;
    case JAVA_TYPE_DOUBLE:
    case JAVA_TYPE_FLOAT:
        result->d = (invoke_managed_func_double_t(invoke_managed_func))(
            FAKE_ARGUMENTS,
            method_entry_point,
            gr_nargs,  fr_nargs, stack_nargs,
            gr_args, fr_args, stack_args);
        break;
    default:
        LDIE(32, "INTERNAL ERROR: Unexpected return type: {0}" << type);
    }

    STD_FREE(stack_args);

    TRACE2("invoke", "exit method "
        << method->get_class()->get_name()->bytes << " "
        << method->get_name()->bytes << " "
        << method->get_descriptor());
}

