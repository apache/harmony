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
#define LOG_DOMAIN "init.ipf"
#include "cxxlog.h"

#include "open/types.h"
#include "open/vm_util.h"
#include "open/hythread_ext.h"

// Internal Native Interface
#include "environment.h"
#include "Class.h"
#include "exceptions.h"
#include "vm_threads.h"

#include "compile.h"

#include "merced.h"
#include "vm_stats.h"
#include "vm_ipf.h"
#include "compile.h"
#include "Code_Emitter.h"
#include "stub_code_utils.h"
#include "nogc.h"
#include "open/gc.h"
#include "interpreter.h"
#include "ini.h"
#include "vtable.h"


void *get_vm_execute_java_method()
{
    static void *addr_execute = 0;
    if(addr_execute) {
        return addr_execute;
    }
    
    const int entry_point        = IN_REG0;
    //const int nargs              = IN_REG1; //not used
    const int args               = IN_REG2;
    const int double_result_addr = IN_REG3;
    //const int double_nargs       = IN_REG4; //not used
    const int double_args        = IN_REG5;
    const int thread_pointer     = IN_REG6;
    const int thread_id          = IN_REG7;

    const int saved_tp = 40;
    const int saved_tid = 41;
    const int old_pfs = 42;
    const int return_link = 43;
    const int saved_heap_base = 44;

    const int out0 = 45;
    
    tl::MemoryPool mem_pool;
    Merced_Code_Emitter emitter(mem_pool, 2, 0);
    emitter.disallow_instruction_exchange();
    emitter.memory_type_is_unknown();

    const int num_loc_regs = out0 - IN_REG0;
    const int num_out_regs = 10;
    emitter.ipf_alloc(old_pfs, 0, num_loc_regs, num_out_regs, 0);
    emitter.ipf_mfbr(return_link, BRANCH_RETURN_LINK_REG);
    
    // Save preserved registers used by VM for thread-local values
    emitter.ipf_mov(saved_tp, THREAD_PTR_REG);
    emitter.ipf_mov(saved_tid, THREAD_ID_REG);
    emitter.ipf_mov(saved_heap_base, HEAP_BASE_REG);
    emitter.ipf_mov(THREAD_PTR_REG, thread_pointer);
    emitter.ipf_mov(THREAD_ID_REG, thread_id);
    emit_mov_imm_compactor(emitter, HEAP_BASE_REG, (uint64)vm_get_heap_base_address());

    /////// begin get args
    // First move int args to stacked registers
    // and fp args to f8..f15
    // Issue two loads per cycle
    for(int i = 0; i < 8; i++) {
        emitter.ipf_ldf_inc_imm(float_mem_size_d, mem_ld_none, mem_none, 8 + i, double_args, 8);
        emitter.ipf_ld_inc_imm(int_mem_size_8, mem_ld_none, mem_none, out0 + i, args, 8);
    }

    // We should also:
    //  - move arg9 and higher to the memory stack
    
    /////// end get args

    enforce_calling_conventions(&emitter);

    emitter.ipf_mtbr(SCRATCH_BRANCH_REG, entry_point);
    emitter.ipf_bricall(br_many, br_sptk, br_none, BRANCH_RETURN_LINK_REG, SCRATCH_BRANCH_REG);
    
    // Unconditionally save the fp result.
    // It is ignored if the signature doesn't expect an fp result
    emitter.ipf_stf(float_mem_size_d, mem_st_none, mem_none, double_result_addr, 8);
    
    emitter.ipf_mov(THREAD_PTR_REG, saved_tp);
    emitter.ipf_mov(THREAD_ID_REG, saved_tid);
    emitter.ipf_mov(HEAP_BASE_REG, saved_heap_base);
    emitter.ipf_mtap(AR_pfs, old_pfs);
    emitter.ipf_mtbr(BRANCH_RETURN_LINK_REG, return_link);
    emitter.ipf_brret(br_many, br_sptk, br_none, BRANCH_RETURN_LINK_REG);

    addr_execute = finalize_stub(emitter, "vm_execute_java");
    return addr_execute;
} // get_vm_execute_java_method

void
JIT_execute_method_default(JIT_Handle jh, 
                           jmethodID   methodID,
                           jvalue   *return_value,
                           jvalue   *args)
{
    //assert(("Doesn't compile", 0));
    //abort();
#if 1
    Method *meth = (Method*) methodID;
    assert(!hythread_is_suspend_enabled());
    void *entry_point = meth->get_code_addr();
    int nargs = meth->get_num_args();
    uint64 arg_words[255];
    int double_nargs = 0;
    double double_args[8];
    int num_arg_words = 0;
    int arg_num = 0;
    int num_ref_args = 0;
    Arg_List_Iterator iter = meth->get_argument_list();
    uint64 i64;
    Java_Type typ;
    char msg[300];
    if(!meth->is_static()) {
        ObjectHandle h = (ObjectHandle) args[arg_num++].l;
        // this pointer
        i64 = 0;
        if (h) i64 = (uint64) h->object;
        if (VM_Global_State::loader_env->compress_references) {
            // 20030318 We are in unmanaged code where null is represented by 0/NULL. Convert a null reference
            // to the representation of null in managed code (heap_base).
            if (i64 == 0) {
                i64 = (uint64)VM_Global_State::loader_env->heap_base;
            }
        }
        arg_words[num_arg_words++] = i64;
        num_ref_args++;
    }
    while((typ = curr_arg(iter)) != JAVA_TYPE_END) {
        ObjectHandle h;
        *msg = '\0';
        switch(typ) {
        case JAVA_TYPE_LONG:
            i64 = args[arg_num++].j;
            arg_words[num_arg_words++] = i64;
            break;
        case JAVA_TYPE_CLASS:
        case JAVA_TYPE_ARRAY:
            h = (ObjectHandle) args[arg_num++].l;
            i64 = 0;
            if (h) i64 = (uint64) h->object;
            if (VM_Global_State::loader_env->compress_references) {
                // 20030318 We are in unmanaged code where null is represented by 0/NULL. Convert a null reference
                // to the representation of null in managed code (heap_base).
                if (i64 == 0) {
                    i64 = (uint64)VM_Global_State::loader_env->heap_base;
                }
            }
            arg_words[num_arg_words++] = i64;
            num_ref_args++;
#ifdef _DEBUG
            {
                if (! VM_Global_State::loader_env->compress_references ||
                    i64 != (uint64)VM_Global_State::loader_env->heap_base) {
                    ManagedObject *object = (ManagedObject *)i64;
                    if(object) {
                        Class *clss = object->vt()->clss;
                        sprintf(msg, " of class '%s'", clss->get_name()->bytes);
                    }
                }
            }
#endif
            break;
        case JAVA_TYPE_SHORT:
            i64 = (uint64)args[arg_num++].s;
            arg_words[num_arg_words++] = i64;
            break;
        case JAVA_TYPE_CHAR:
            i64 = (uint64)args[arg_num++].c;
            arg_words[num_arg_words++] = i64;
            break;
        case JAVA_TYPE_BYTE:
            i64 = (uint64)args[arg_num++].b;
            arg_words[num_arg_words++] = i64;
            break;
        case JAVA_TYPE_BOOLEAN:
            i64 = (uint64)args[arg_num++].z;
            arg_words[num_arg_words++] = i64;
            break;
        case JAVA_TYPE_DOUBLE:
            double_args[double_nargs] = args[arg_num++].d;
            double_nargs++;
            break;
        case JAVA_TYPE_FLOAT:
            double_args[double_nargs] = (double)args[arg_num++].f;
            double_nargs++;
            break;
        default:
            i64 = (uint64)args[arg_num++].i;
            arg_words[num_arg_words++] = i64;
            break;
        }
        iter = advance_arg_iterator(iter);
    }

    // assert(nargs <= 8);
    double double_result;

    static void* addr_execute = get_vm_execute_java_method();
    struct{
        void* fun;
        void* gp;
    } fptr;
    fptr.fun = addr_execute;
    fptr.gp = get_vm_gp_value();
        // gashiman - changed _cdecl to __cdecl to work on linux
    uint64 (__cdecl *fpp_exec)(void *entry_point, int nargs, uint64 args[], 
        double *double_result_addr, int double_nargs, double double_args[], 
        void *thread_pointer, uint64 tid) = (uint64 (__cdecl * )(void *entry_point, int nargs, uint64 args[],
        double *double_result_addr, int double_nargs, double double_args[], 
        void *thread_pointer, uint64 tid))&fptr;
    IDATA id = hythread_get_self_id();
    uint64 int_result = (uint64)fpp_exec(entry_point, nargs, arg_words, &double_result,
        double_nargs, double_args, p_TLS_vmthread, id);

    // Save the result
    Java_Type ret_type = meth->get_return_java_type();
    switch(ret_type) {
    case JAVA_TYPE_VOID:
        break;
    case JAVA_TYPE_ARRAY:
    case JAVA_TYPE_CLASS:
        {
            ObjectHandle h = 0;
            if (VM_Global_State::loader_env->compress_references) {
                // 20030318 Convert a null reference in managed code (represented by heap_base)
                // to the representation of null in unmanaged code (0 or NULL).
                if ((uint64)int_result == (uint64)VM_Global_State::loader_env->heap_base) {
                    int_result = 0;
                } 
            }
            if (int_result) {
                h = oh_allocate_local_handle();
                h->object = (ManagedObject*) int_result;
            }
            return_value->l = h;
        }
        break;
    case JAVA_TYPE_LONG:
        return_value->j = int_result;
        break;
    case JAVA_TYPE_INT:
        *((I_32 *)return_value) = (I_32) int_result;
        break;
    case JAVA_TYPE_SHORT:
        *((int16 *)return_value) = (int16) int_result;
        break;
    case JAVA_TYPE_CHAR:
        *((uint16 *)return_value) = (uint16) int_result;
        break;
    case JAVA_TYPE_BYTE:
        *((I_8 *)return_value) = (I_8) int_result;
        break;
    case JAVA_TYPE_BOOLEAN:
        *((U_8 *)return_value) = (U_8) int_result;
        break;
    case JAVA_TYPE_DOUBLE:
        *((double *)return_value) = double_result;
        break;
    case JAVA_TYPE_FLOAT:
        *((float *)return_value) = (float) double_result;
        break;
    default:
#ifdef _DEBUG
        std::clog << "Returned to C from "
               << meth->get_class()->get_name()->bytes << "." << meth->get_name()->bytes
               << meth->get_descriptor()->bytes << "\n";
#endif
        //DIE("Return type ");// <<  (int)ret_type << " is not implemented\n");
        std::clog << "Return type " <<  (int)ret_type << " is not implemented\n";
    }
#endif

} //vm_execute_java_method_array
