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

#include <open/vm_class_info.h>
#include <jit_import.h>

/**
 * @file
 * @brief CodeGen routines for PUSH/POP, LDC and LD/ST.
 */

namespace Jitrino {
namespace Jet {

void CodeGen::gen_ldc(void)
{
    jtype jtyp = to_jtype(class_cp_get_const_type(
                                m_klass, (unsigned short)m_curr_inst->op0));
    if (jtyp != jobj) { // if not loading String
        const void * p = class_cp_get_const_addr(m_klass, m_curr_inst->op0);
        assert(p);
        if (jtyp == dbl64 || jtyp == flt32) {
            gen_push(jtyp, p);
        }
        else if(jtyp == i64) {
            gen_push(*(jlong*)p);
        }
        else if(jtyp == i32) {
            gen_push(*(int*)p);
        }
        else if(jtyp == u16) {
            gen_push(*(unsigned short*)p);
        }
        else if(jtyp == i16) {
            gen_push(*(short*)p);
        }
        else {
            assert(jtyp == i8);
            gen_push(*(char*)p);
        }
        return;
    }
    assert(m_curr_inst->opcode != OPCODE_LDC2_W);
    gen_call_vm(ci_helper_oi, rt_helper_ldc_string, 0, m_klass, m_curr_inst->op0);
    gen_save_ret(ci_helper_oi);
    vstack(0).set(VA_NZ);
    m_bbstate->seen_gcpt = true;
}

void CodeGen::gen_push(int ival)
{
    vpush(Val(ival));
}

void CodeGen::gen_push(jlong lval)
{
    if (is_big(i64)) {
        vpush2(Val((jlong)lo32(lval)), Val((jlong)hi32(lval)));
    }
    else {
        vpush(Val(lval));
    }
}

void CodeGen::gen_push(jtype jt, const void *p)
{
    if (jt == dbl64) {
        vpush(Val(*(double*)p, p));
    }
    else if (jt == flt32) {
        vpush(Val(*(float*)p, p));
    }
    else {
        assert(jt==jobj);
        // This is for brand new 1.5's 'LDC object'.
        // The object loaded is marked as constant which can cross a call 
        // site, however must check with VM/GC guys whether this is indeed
        // true.
        // XXX: So, are the 'LDC object'-s pinned ?
        // For now, using conservative approach.
        vpush(Val(jobj, p)); // vpush(Val(jobj, p).long_live());
    }
}

void CodeGen::gen_pop(jtype jt)
{
    vpop();
}

void CodeGen::gen_pop2(void)
{
    jtype jt = m_jframe->top();
    if (is_wide(jt)) {
        vpop();
    }
    else {
        vpop();
        assert(!is_wide(m_jframe->top()));
        vpop();
    }
}

void CodeGen::gen_dup(JavaByteCodes opc)
{
    jtype jtop = m_jframe->top();

// Only need to take a special action if 's' is a stack item resides in 
// the stack memory area. If it points to 
// local/static/const/field/register/whatever then can simply duplicate it. 
#define COPY_SLOT(dst, src) \
    { \
    const Val& ssrc = vstack(src); \
    if (vis_stack(ssrc) && !ssrc.is_dummy()) { \
        vstack(src, true); \
    } \
    Val& sdst = vstack(dst); \
    rfree(sdst); \
    sdst = ssrc; \
    rref(sdst); \
    }
    
    if (opc == OPCODE_SWAP) {
        // stack: .. val1, val2 => val2, val1
        Val& val2 = vstack(0, vis_stack(0));
        rlock(val2);
        Val& val1 = vstack(1, vis_stack(1));
        runlock(val2);
        Val tmp = val1;
        val1 = val2;
        val2 = tmp;
        return;
    }
    
    if (opc == OPCODE_DUP) {
        // [.. val] => [.. val, val]
        vpush(jtop);
        COPY_SLOT(0, 1);
        return;
    }
    
    if (opc==OPCODE_DUP2) {
        // [.. val64] => [.. val64, val64]
        vpush(jtop);
        if (!is_wide(jtop)) {
            vpush(jtop);
        }
        COPY_SLOT(0, 2);
        COPY_SLOT(1, 3);
        return;
    }
    
    if (opc == OPCODE_DUP_X1) {
        // [..., value2, value1] => [.. value1, value2, value1]
        // 1. push(jtop)
        // result: val2, val1, typeof(val1)
        // 2. depth(0) = depth(1)
        // result: val2, val1, val1
        // 3. depth(1) = depth(2)
        // result: val2, val2, val1
        // 4. depth(2) = depth(0)
        // result: val1, val2, val1
        vpush(jtop);
        COPY_SLOT(0, 1);
        COPY_SLOT(1, 2);
        COPY_SLOT(2, 0);
        return;
    }
    
    // DUP_X2
    if (opc == OPCODE_DUP_X2) {
        // Form 1: .. val3, val2, val1 => val1, val3, val2, val1
        // Form 2: .. val2.hi, val2.lo, val1.32 =>  
        //                              ..val1, val2.hi, val2.lo, val1
        // form1:
        // 1. push(typeof(depth(0)))
        // result: val3, val2, val1, typeof(val1)
        // 2. depth(0) = depth(1)
        // result: val3, val2, val1, val1
        // 3. depth(1) = depth(2)
        // result: val3, val2, val2, val1
        // 4. depth(2) = depth(3)
        // result: val3, val3, val2, val1
        // 5. depth(3) = depth(0)
        // result: val1, val3, val2, val1
        vpush(jtop);
        COPY_SLOT(0, 1);
        COPY_SLOT(1, 2);
        COPY_SLOT(2, 3);
        COPY_SLOT(3, 0);
        return;
    }

    if (opc == OPCODE_DUP2_X1) {
        // Form 1: val3, val2, val1  => val2, val1, val3, val2, val1
        // Form 2: [..., val2.32, val1.64] => [.. val1, val2, val1]
        // A strategy:
        // 1. push(typeof(v1))
        // result: .. val2, val1.64, val1.64(unknown location)
        // 2. depth(0) = depth(2) ; depth(1) = depth(3)
        // result: .. val2, val1.64, val1.64
        // 3. depth(2) = depth(4) ; 
        // result: .. val2, zzz, val2, val1.64 
        //  zzz is former high part of val1
        // 4. depth(3) = depth(0)
        // result: .. val2, val1.lo, val2, val1.64 
        // 5. depth(4) = depth(1)
        // result: .. val1.hi, val1.lo, val2, val1.64 
        vpush(jtop);
        if (!is_wide(jtop)) {
            vpush(jtop);
        }
        COPY_SLOT(0, 2); COPY_SLOT(1, 3);
        COPY_SLOT(2, 4);
        COPY_SLOT(3, 0);
        COPY_SLOT(4, 1);
        return;
    }
    if (opc == OPCODE_DUP2_X2) {
        // [.. val2.64, val1.64] =>  [.. val1.64, val2.64, val1.64]
        //f1 .., val4, val3, val2, val1 => val2, val1, val4, val3, val2, val1
        //f2 .., val3, val2, val1 ..., val1, val3, val2, val1
        //f3 .., val3, val2, val1 ..., val2, val1, val3, val2, val1
        //f4 .., val2, val1 ..., val1, val2, val1
        // 
        // A strategy:
        // 1. push(any-wide-type)
        // result: val4, val3, val2, val1, xx, xx
        // 2. depth(0) = depth(2)
        // 3. depth(1) = depth(3)
        // result: val4, val3, val2, val1, val2, val1
        // 4. depth(2) = depth(4)
        // 5. depth(3) = depth(5)
        // result: val4, val3, val4, val3, val2, val1
        // 6. depth(4) = depth(0)
        // 7. depth(5) = depth(1)
        // result: val2, val1, val2, val1, val2, val1
        vpush(i64); // the type does not really matter
        // 2, 3
        COPY_SLOT(0, 2);
        COPY_SLOT(1, 3);
        // 4, 5
        COPY_SLOT(2, 4);
        COPY_SLOT(3, 5);
        // 6, 7
        COPY_SLOT(4, 0);
        COPY_SLOT(5, 1);
        return;
    }
    assert(false);
}


void CodeGen::gen_ld(jtype jt, unsigned idx)
{
    if (is_big(jt)) {
        Val vlo = vlocal(jt, idx, false);
        rlock(vlo);
        Val vhi = vlocal(jt, idx+1, false);
        vpush2(vlo, vhi);
        runlock(vlo);
    }
    else {
        Val& v = vlocal(jt, idx, false);
        vpush(v);
    }
}

void CodeGen::gen_st(jtype jt, unsigned idx)
{
    vvar_def(jt, idx);
    gen_gc_mark_local(jt, idx);
    //
    // v|s
    // -|-|-------------------------
    // r|m| move
    // r|i| move
    // r|r| move
    // m|m| allocate reg, then move
    // m|r| move
    // m|i| move
    //
    Val s0 = vstack(0);
    rlock(s0);
    Val& var = vlocal(jt, idx, true);
    runlock(s0);
    vunref(jt, var);
    if (var.is_mem() && s0.is_reg() && rrefs(s0.reg()) == 1) {
        // The local is on memory, though the stack item is on register
        vunref(jt, s0, 0);
        vassign(jt, idx, s0);
    }
    else {
        do_mov(var, s0);
    }
    var.attrs(s0.attrs());
    
    if (is_big(jt)) {
        //
        ++idx;
        //
        Val s0 = vstack(1);
        rlock(s0);
        Val& var = vlocal(jt, idx, true);
        runlock(s0);
        vunref(jt, var);
        if (var.is_mem() && s0.is_reg() &&  rrefs(s0.reg()) == 1) {
            // The local is on memory, though the stack item is on register
            vunref(jt, s0, 1);
            vassign(jt, idx, s0);
        }
        else {
            do_mov(var, s0);
        }
        var.attrs(s0.attrs());
    }
    vpop();
}



}}; // ~namespace Jitrino::Jet
