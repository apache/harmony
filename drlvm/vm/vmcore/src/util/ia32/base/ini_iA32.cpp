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
#define LOG_DOMAIN "invoke"
#include "cxxlog.h"

#include "open/types.h"
#include "open/vm_util.h"
#include "Class.h"
#include "exceptions.h"
#include "vm_threads.h"
#include "jit_runtime_support_common.h"

#include "compile.h"
#include "nogc.h"
#include "encoder.h"
#include "ini.h"
#include "environment.h"
#include "lil.h"
#include "lil_code_generator.h"
#include "lil_code_generator_utils.h"

#include "interpreter.h"

#include "port_malloc.h"

#include "dump.h"

typedef ManagedObject* (*RefFuncPtr)(U_32* args, int args_size, void* func);
typedef I_32 (*IntFuncPtr)(U_32* args, int args_size, void* func);
typedef int64 (*LongFuncPtr)(U_32* args, int args_size, void* func);
typedef float (*FloatFuncPtr)(U_32* args, int args_size, void* func);
typedef double (*DoubleFuncPtr)(U_32* args, int args_size, void* func);

static char* gen_invoke_common_managed_func(char* stub) {
    // Defines stack alignment on managed function enter.
    const I_32 STACK_ALIGNMENT = MANAGED_STACK_ALIGNMENT;
    const I_32 STACK_ALIGNMENT_MASK = ~(STACK_ALIGNMENT - 1);
    const char * LOOP_BEGIN = "loop_begin";
    const char * LOOP_END = "loop_end";

    // [ebp + 8] - args
    // [ebp + 12] - size
    // [ebp + 16] - func
    const I_32 STACK_ARGS_OFFSET = 8;
    const I_32 STACK_NARGS_OFFSET = 12;
    const I_32 STACK_FUNC_OFFSET = 16;
    const I_32 STACK_CALLEE_SAVED_OFFSET = -12;
    
    tl::MemoryPool pool;
    LilCguLabelAddresses labels(&pool, stub);
    
    // Initialize ebp-based stack frame.
    stub = push(stub, ebp_opnd);
    stub = mov(stub, ebp_opnd, esp_opnd);
    
    // Preserve callee-saved registers.
    stub = push(stub, ebx_opnd);
    stub = push(stub, esi_opnd);
    stub = push(stub, edi_opnd);

    // Load an array of arguments ('args') and its size from the stack.
    stub = mov(stub, eax_opnd, M_Base_Opnd(ebp_reg, STACK_ARGS_OFFSET));
    stub = mov(stub, ecx_opnd, M_Base_Opnd(ebp_reg, STACK_NARGS_OFFSET));
    

    // Align memory stack.
    stub = lea(stub, ebx_opnd, M_Index_Opnd(n_reg, ecx_reg, 4, 4));
    stub = mov(stub, esi_opnd, ebx_opnd);
    stub = neg(stub, esi_opnd);
    stub = alu(stub, add_opc, esi_opnd, esp_opnd);
    stub = alu(stub, and_opc, esi_opnd, Imm_Opnd(size_32, STACK_ALIGNMENT_MASK));
    stub = alu(stub, add_opc, ebx_opnd, esi_opnd);
    stub = mov(stub, esp_opnd, ebx_opnd);
    
    // Load a pointer to the last argument of 'args' array.
    stub = lea(stub, eax_opnd, M_Index_Opnd(eax_reg, ecx_reg, -4, 4));
    stub = alu(stub, sub_opc, eax_opnd, esp_opnd);
    stub = alu(stub, or_opc, ecx_opnd, ecx_opnd);
    stub = branch8(stub, Condition_Z, Imm_Opnd(size_8, 0));
    labels.add_patch_to_label(LOOP_END, stub - 1, LPT_Rel8);
    
// LOOP_BEGIN:
    // Push inputs on the stack.
    labels.define_label(LOOP_BEGIN, stub, false);
    
    stub = push(stub, M_Index_Opnd(esp_reg, eax_reg, 0, 1));
    stub = loop(stub, Imm_Opnd(size_8, 0));
    labels.add_patch_to_label(LOOP_BEGIN, stub - 1, LPT_Rel8);

// LOOP_END:    
    labels.define_label(LOOP_END, stub, false);
    
    // Call target function.
    stub = mov(stub, eax_opnd, M_Base_Opnd(ebp_reg, STACK_FUNC_OFFSET));
    stub = call(stub, eax_opnd);
    
    // Restore callee-saved registers from the stack.
    stub = lea(stub, esp_opnd, M_Base_Opnd(ebp_reg, STACK_CALLEE_SAVED_OFFSET));
    stub = pop(stub, edi_opnd);
    stub = pop(stub, esi_opnd);
    stub = pop(stub, ebx_opnd);
    
    // Leave current frame.
    stub = pop(stub, ebp_opnd);

    return stub;
}

static IntFuncPtr gen_invoke_int_managed_func() {
    static IntFuncPtr func = NULL;
    
    if (func) {
        return func;
    }

    const int STUB_SIZE = 124;
    
    char * stub = (char *) malloc_fixed_code_for_jit(STUB_SIZE,
        DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_DEFAULT, CAA_Allocate);

    func = (IntFuncPtr) stub;
    stub = gen_invoke_common_managed_func(stub);
        
    stub = ret(stub);
    
    assert(stub - (char *)func <= STUB_SIZE);

    DUMP_STUB(func, "invoke_int_managed_func", stub - (char *)func);
    return func;
}

static FloatFuncPtr gen_invoke_float_managed_func() {
    static FloatFuncPtr func = NULL;
    
    if (func) {
        return func;
    }

    const int STUB_SIZE = 132;
    
    char * stub = (char *) malloc_fixed_code_for_jit(STUB_SIZE,
        DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_DEFAULT, CAA_Allocate);

    func = (FloatFuncPtr) stub;
    stub = gen_invoke_common_managed_func(stub);

    // Put return value on FPU stack.
    M_Opnd memloc(esp_reg, -4);
    stub = sse_mov(stub, memloc, xmm0_opnd, false);
    stub = fld(stub, memloc, false);
    stub = ret(stub);
    
    assert(stub - (char *)func <= STUB_SIZE);

    DUMP_STUB(func, "invoke_float_managed_func", stub - (char *)func);
    return func;    
}

static DoubleFuncPtr gen_invoke_double_managed_func() {
    static DoubleFuncPtr func = NULL;
    
    if (func) {
        return func;
    }

    const int STUB_SIZE = 132;
    
    char * stub = (char *) malloc_fixed_code_for_jit(STUB_SIZE,
        DEFAULT_CODE_ALIGNMENT, CODE_BLOCK_HEAT_DEFAULT, CAA_Allocate);

    func = (DoubleFuncPtr) stub;
    stub = gen_invoke_common_managed_func(stub);

    // Put return value on FPU stack.
    M_Opnd memloc(esp_reg, -8);
    stub = sse_mov(stub, memloc, xmm0_opnd, true);
    stub = fld(stub, memloc, true);
    stub = ret(stub);
    
    assert(stub - (char *)func <= STUB_SIZE);

    DUMP_STUB(func, "invoke_double_managed_func", stub - (char *)func);
    return func;
    
}

void
JIT_execute_method_default(JIT_Handle jit, jmethodID methodID, jvalue *return_value, jvalue *args) {

    // Detecting errors with object headears on stack when using destructive
    // unwinding.
    void *lastFrame = p_TLS_vmthread->lastFrame;
    p_TLS_vmthread->lastFrame = (void*)&lastFrame;
    //printf("execute: push: prev = 0x%p, curr=0x%p\n", lastFrame, &lastFrame);

//    fprintf(stderr, "Not implemented\n");

    Method *method = (Method*) methodID;
    TRACE("enter method "
            << method->get_class()->get_name()->bytes << " "
            << method->get_name()->bytes << " "
            << method->get_descriptor()->bytes);
    int sz = method->get_num_arg_slots();
    void *meth_addr = method->get_code_addr();
    U_32 *arg_words = (U_32*) STD_ALLOCA(sz * sizeof(U_32));

    int argId = sz;
    int pos = 0;

    assert(!hythread_is_suspend_enabled());
    if (!method->is_static()) {
        ObjectHandle handle = (ObjectHandle) args[pos++].l;
        assert(handle);
        arg_words[--argId] = (unsigned) handle->object;
    }

    const char *mtype = method->get_descriptor()->bytes + 1;
    assert(mtype != 0);

    for(; *mtype != ')'; mtype++) {
        switch(*mtype) {
            case JAVA_TYPE_CLASS:
            case JAVA_TYPE_ARRAY:
                {
                    ObjectHandle handle = (ObjectHandle) args[pos++].l;
                    arg_words[--argId] = (unsigned) (handle ? handle->object : 0);

                    while(*mtype == '[') mtype++;
                    if (*mtype == 'L')
                        while(*mtype != ';') mtype++;
                }
                break;

            case JAVA_TYPE_SHORT:
                // sign extend
                arg_words[--argId] = (U_32)(I_32) args[pos++].s;
                break;
            case JAVA_TYPE_BYTE:
                // sign extend
                arg_words[--argId] = (U_32)(I_32) args[pos++].b;
                break;
            case JAVA_TYPE_INT:
                // sign extend
                arg_words[--argId] = (U_32)(I_32) args[pos++].i;
                break;

            case JAVA_TYPE_FLOAT:
                arg_words[--argId] = (I_32) args[pos++].i;
                break;
            case JAVA_TYPE_BOOLEAN:
                arg_words[--argId] = (I_32) args[pos++].z;
                break;
            case JAVA_TYPE_CHAR:
                // zero extend
                arg_words[--argId] = (I_32) args[pos++].c;
                break;

            case JAVA_TYPE_LONG:
            case JAVA_TYPE_DOUBLE:
                *(jlong*)&arg_words[argId-2] = args[pos++].j;
                argId -= 2;
                break;
            default:
                LDIE(53, "Unexpected java type");
        }
    }
    assert(argId >= 0);

    jvalue *resultPtr = (jvalue*) return_value;
    Java_Type ret_type = method->get_return_java_type();

    arg_words += argId;
    argId = sz - argId;

    static const IntFuncPtr invoke_managed_func = gen_invoke_int_managed_func();
    static const FloatFuncPtr invoke_float_managed_func = gen_invoke_float_managed_func();
    static const DoubleFuncPtr invoke_double_managed_func = gen_invoke_double_managed_func();

    switch(ret_type) {
        case JAVA_TYPE_VOID:
            invoke_managed_func(arg_words, argId, meth_addr);
            break;
        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
        case JAVA_TYPE_STRING:
            {
                ManagedObject *ref = ((RefFuncPtr)invoke_managed_func)(arg_words, argId, meth_addr);
                ObjectHandle h = oh_allocate_local_handle();

                if (ref != NULL) {
                    h->object = ref;
                    resultPtr->l = h;
                } else {
                    resultPtr->l = NULL;
                }
            }
            break;

        case JAVA_TYPE_BOOLEAN:
        case JAVA_TYPE_BYTE:
        case JAVA_TYPE_CHAR:
        case JAVA_TYPE_SHORT:
        case JAVA_TYPE_INT:
            resultPtr->i = invoke_managed_func(arg_words, argId, meth_addr);
            break;

        case JAVA_TYPE_FLOAT:
            resultPtr->f = invoke_float_managed_func(arg_words, argId, meth_addr);
            break;

        case JAVA_TYPE_LONG:
            resultPtr->j = ((LongFuncPtr)invoke_managed_func)(arg_words, argId, meth_addr);
            break;

        case JAVA_TYPE_DOUBLE:
            resultPtr->d = invoke_double_managed_func(arg_words, argId, meth_addr);
            break;

        default:
            LDIE(53, "Unexpected java type");
    }

    if (exn_raised()) {
        TRACE("Exception occured: " << exn_get_name());
        if ((resultPtr != NULL) && (ret_type != JAVA_TYPE_VOID)) {   
            resultPtr->l = 0; //clear result
        }
    }
 
    TRACE("exit method "
            << method->get_class()->get_name()->bytes << " "
            << method->get_name()->bytes << " "
            << method->get_descriptor()->bytes);

    // Detecting errors with object headears on stack when using destructive
    // unwinding.
    //printf("execute:  pop: prev = 0x%p, curr=0x%p\n", &lastFrame, lastFrame);
    p_TLS_vmthread->lastFrame = lastFrame;
}





