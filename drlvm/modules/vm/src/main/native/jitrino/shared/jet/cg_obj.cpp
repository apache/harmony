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
 * @author Alexander Astapchuk
 */
#include "compiler.h"
#include "trace.h"

#ifdef WIN32
#include <malloc.h>
#endif
#include <memory.h>
#include <assert.h>
#include <stdlib.h>

#include "open/vm_class_info.h"
#include "open/vm_class_loading.h"
#include "open/vm_ee.h"
#include "open/vm.h"

/**
 * @file
 * @brief Object creation, monitors and cast checks.
 */

namespace Jitrino {
namespace Jet {

void CodeGen::gen_new_array(Class_Handle enclClass, unsigned cpIndex) 
{
    bool lazy = m_lazy_resolution;
    bool resolve = !lazy || class_cp_is_entry_resolved(enclClass, cpIndex);
    if (resolve) {
        Allocation_Handle ah = 0;
        Class_Handle klass = resolve_class(m_compileHandle, enclClass,  cpIndex);
        if (klass != NULL) {
            klass = class_get_array_of_class(klass);
        }
        if (klass != NULL) {
            ah = class_get_allocation_handle(klass);
        }
        gen_new_array(ah);
        return;
    } 
    assert(lazy);
    vpark();
    SYNC_FIRST(static const CallSig cs_newarray_withresolve(CCONV_HELPERS, jobj, iplatf, i32, i32));
    rlock(cs_newarray_withresolve);
    Val sizeVal = vstack(0);
    // setup constant parameters first,
    Val vclass(iplatf, enclClass);
    Val vcpIdx((int)cpIndex);
    gen_args(cs_newarray_withresolve, 0, &vclass, &vcpIdx, &sizeVal);
    gen_call_vm(cs_newarray_withresolve, rt_helper_new_array_withresolve, 3);
    runlock(cs_newarray_withresolve);
    vpop();// pop array size
    gen_save_ret(cs_newarray_withresolve);

    // the returned can not be null, marking as such.
    vstack(0).set(VA_NZ);
    // allocation assumes GC invocation
    m_bbstate->seen_gcpt = true;
}

void CodeGen::gen_new_array(Allocation_Handle ah) {
    const JInst& jinst = *m_curr_inst;
    assert(jinst.opcode == OPCODE_NEWARRAY ||  jinst.opcode == OPCODE_ANEWARRAY);
        
    if (ah == 0) {
        assert(!m_lazy_resolution);
        // it's unexpected that that something failed for a primitive type
        assert(jinst.opcode != OPCODE_NEWARRAY);
        gen_call_throw(ci_helper_linkerr, rt_helper_throw_linking_exc, 0,
                       m_klass, jinst.op0, jinst.opcode);
    }
    SYNC_FIRST(static const CallSig cs_new_arr(CCONV_HELPERS, jobj, i32, jobj));
    unsigned stackFix = gen_stack_to_args(true, cs_new_arr, 0, 1);
    gen_call_vm(cs_new_arr, rt_helper_new_array, 1, ah);
    runlock(cs_new_arr);
    
    if (stackFix != 0) {
        alu(alu_sub, sp, stackFix);
    }
    gen_save_ret(cs_new_arr);
    // the returned can not be null, marking as such.
    vstack(0).set(VA_NZ);
    // allocation assumes GC invocation
    m_bbstate->seen_gcpt = true;
}

void CodeGen::gen_multianewarray(Class_Handle enclClass, unsigned short cpIndex, unsigned num_dims)
{
    // stack: [..., count1, [...count N] ]
    // args: (klassHandle, num_dims, count_n, ... count_1)
    // native stack to be established (ia32):  
    //          .., count_1, .. count_n, num_dims, klassHandle
    
    vector<jtype> args;
    for (unsigned i = 0; i<num_dims; i++) {
        args.push_back(i32);
    }
    args.push_back(i32);
    args.push_back(jobj);
        
    Class_Handle klass = NULL;
    Val klassVal;

    bool lazy = m_lazy_resolution;
    bool resolve = !lazy || class_cp_is_entry_resolved(enclClass, cpIndex);
    if(!resolve) {
        assert(lazy);
        SYNC_FIRST(static const CallSig ci_get_class_withresolve(CCONV_HELPERS, jobj, iplatf, i32));
        gen_call_vm(ci_get_class_withresolve, rt_helper_get_class_withresolve, 0, enclClass, cpIndex);
        AR gr_ret = ci_get_class_withresolve.ret_reg(0);
        klassVal = Val(jobj, gr_ret);
    }  else {
        klass = resolve_class(m_compileHandle, enclClass, cpIndex);
        klassVal = Val(jobj, klass);
    }
    rlock(klassVal); // to protect gr_ret while setting up helper args

    // note: need to restore the stack - the cdecl-like function
    CallSig ci(CCONV_CDECL|CCONV_MEM|CCONV_L2R|CCONV_RETURN_FP_THROUGH_FPU, jobj, args);
    unsigned stackFix = gen_stack_to_args(true, ci, 0, num_dims);

    runlock(klassVal);

    if (klass == NULL && !lazy) {
        gen_call_throw(ci_helper_linkerr, rt_helper_throw_linking_exc, 0,
                       m_klass, cpIndex, OPCODE_MULTIANEWARRAY);
    }
    Val vnum_dims = Opnd(num_dims);
    gen_args(ci, num_dims, &vnum_dims, &klassVal);

    gen_call_vm(ci, rt_helper_multinewarray, 2+num_dims);
    runlock(ci);
    if (stackFix != 0) {
        alu(alu_sub, sp, stackFix);
    }
    gen_save_ret(ci);
    // the returned can not be null, marking as such.
    vstack(0).set(VA_NZ);
    // allocation assumes GC invocation
    m_bbstate->seen_gcpt = true;
}


void CodeGen::gen_new(Class_Handle enclClass, unsigned short cpIndex)
{
    bool lazy = m_lazy_resolution;
    bool resolve = !lazy || class_cp_is_entry_resolved(enclClass, cpIndex);
    const CallSig* ci = NULL; 
    if (resolve) {
        SYNC_FIRST(static const CallSig ci_new(CCONV_HELPERS, jobj, i32, jobj));
        ci = &ci_new;

        Class_Handle klass = resolve_class_new(m_compileHandle, enclClass, cpIndex);
        if (klass == NULL) {
            gen_call_throw(ci_helper_linkerr, rt_helper_throw_linking_exc, 0, enclClass, cpIndex, OPCODE_NEW);
        } else {
            if ( klass!=enclClass && !class_is_initialized(klass)) {
                gen_call_vm(ci_helper_o, rt_helper_init_class, 0, klass);
            }
            unsigned size = class_get_object_size(klass);
            Allocation_Handle ah = class_get_allocation_handle(klass);
            gen_call_vm(ci_new, rt_helper_new, 0, size, ah);
        }
    } else {
        assert(lazy);
        SYNC_FIRST(static const CallSig ci_new_with_resolve(CCONV_HELPERS, jobj, iplatf, i32));
        ci = &ci_new_with_resolve;
        gen_call_vm(ci_new_with_resolve, rt_helper_new_withresolve, 0, enclClass, cpIndex);
    }
    gen_save_ret(*ci);
    vstack(0).set(VA_NZ);// the returned can not be null, marking as such.
    m_bbstate->seen_gcpt = true;// allocation assumes GC invocation

}

void CodeGen::gen_instanceof_cast(JavaByteCodes opcode, Class_Handle enclClass, unsigned short cpIdx)
{
    assert (opcode == OPCODE_INSTANCEOF || opcode == OPCODE_CHECKCAST);
    bool lazy = m_lazy_resolution;
    bool resolve  = !lazy || class_cp_is_entry_resolved(enclClass, cpIdx);
    if (resolve) {
        Class_Handle klass = resolve_class(m_compileHandle, enclClass, cpIdx);
        if (klass == NULL) {
            // resolution has failed
            gen_call_throw(ci_helper_linkerr, rt_helper_throw_linking_exc, 0, enclClass, cpIdx, opcode);
        }
        SYNC_FIRST(static const CallSig cs_checkcast(CCONV_HELPERS, jobj, jobj, jobj));
        SYNC_FIRST(static const CallSig cs_instanceof(CCONV_HELPERS, i32, jobj, jobj));
        const CallSig& cs = (opcode == OPCODE_CHECKCAST) ? cs_checkcast : cs_instanceof;   
        char * helper = (opcode == OPCODE_CHECKCAST) ? rt_helper_checkcast : rt_helper_instanceof;
        unsigned stackFix = gen_stack_to_args(true, cs, 0, 1);
        gen_call_vm(cs, helper, 1, klass);
        if (stackFix != 0) {
            alu(alu_sub, sp, stackFix);
        }
        runlock(cs);
        gen_save_ret(cs);
    } else {
        assert(lazy);
        vpark();
        SYNC_FIRST(static const CallSig cs_checkcast_with_resolve(CCONV_HELPERS, jobj, iplatf, i32, jobj));
        SYNC_FIRST(static const CallSig cs_instanceof_with_resolve(CCONV_HELPERS, i32, iplatf, i32, jobj));
        const CallSig& cs_with_resolve = (opcode == OPCODE_CHECKCAST) ? cs_checkcast_with_resolve : cs_instanceof_with_resolve;
        rlock(cs_with_resolve);
        char * helper = (opcode == OPCODE_CHECKCAST) ? rt_helper_checkcast_withresolve : rt_helper_instanceof_withresolve;
        Val tos = vstack(0);
        // setup constant parameters first,
        Val vclass(iplatf, enclClass);
        Val vcpIdx(cpIdx);
        gen_args(cs_with_resolve, 0, &vclass, &vcpIdx, &tos);
        gen_call_vm(cs_with_resolve, helper, 3);
        runlock(cs_with_resolve);
        vpop();//pop obj
        gen_save_ret(cs_with_resolve);
    }
}

void CodeGen::gen_monitor_ee(void)
{
    const JInst& jinst = *m_curr_inst;
    gen_check_null(0);
    SYNC_FIRST(static const CallSig cs_mon(CCONV_HELPERS, jvoid, jobj));
    unsigned stackFix = gen_stack_to_args(true, cs_mon, 0);
    gen_call_vm(cs_mon,
            jinst.opcode == OPCODE_MONITORENTER ? 
                rt_helper_monitor_enter : rt_helper_monitor_exit, 1);
    runlock(cs_mon);
    if (stackFix != 0) {
        alu(alu_sub, sp, stackFix);
    }
}

void CodeGen::gen_athrow(void)
{
    SYNC_FIRST(static const CallSig cs_throw(CCONV_HELPERS, jvoid, jobj));
    unsigned stackFix = gen_stack_to_args(true, cs_throw, 0);
    gen_call_vm(cs_throw, rt_helper_throw, 1);
    runlock(cs_throw);
    if (stackFix != 0) {
        alu(alu_sub, sp, stackFix);
    }
}





}}; // ~namespace Jitrino::Jet
