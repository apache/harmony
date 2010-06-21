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
/**
 * @file
 * @brief Implementation of debugging and tracing CodeGen routines.
 */
 
#include "compiler.h"
#include "trace.h"

#ifdef _WIN32
    #include <crtdbg.h>
#endif

namespace Jitrino {
namespace Jet {

const CallSig CodeGen::cs_trace_arg(CCONV_STDCALL, jvoid, jobj, i32, i32);


void CodeGen::dbg_check_mem(void)
{
#if defined(_DEBUG) && defined(_WIN32)
    static bool done = false;
    if (done) {
        _CrtSetReportMode(_CRT_ASSERT,  _CRTDBG_MODE_WNDW);
        _CrtSetReportMode(_CRT_ERROR,   _CRTDBG_MODE_WNDW);
        _CrtSetReportMode(_CRT_WARN,  _CRTDBG_MODE_WNDW);
        _set_error_mode(_OUT_TO_MSGBOX);
        done = true;
    }
    assert(_CrtCheckMemory());
#endif
}

void __stdcall CodeGen::dbg_trace_arg(void * val, int idx, jtype jt)
{
    assert(i8<=jt && jt<num_jtypes);
    char str_value[100];
    if (jt<=i32) {
        int v = *(int*)&val;
        snprintf(str_value, sizeof(str_value)-1, 
                 "%d (0x%x,'%c')", v, v, (v<32 || v>127 ? ' ' : (char)v));
    }
    else if (jt==flt32) {
        snprintf(str_value, sizeof(str_value)-1, "%f", *(float*)&val);
    }
    else if (jt==dbl64) {
        snprintf(str_value, sizeof(str_value)-1, "%f", *(double*)&val);
    }
    else if (jt==i64) {
        snprintf(str_value, sizeof(str_value)-1, "%lld", *(jlong*)&val);
    }
    else {
        snprintf(str_value, sizeof(str_value)-1, "%p", val);
    }
    
    if (idx == -1) {
        dbg_rt("\tret=(%s)%s", jtypes[jt].name, str_value);
    }
    else {
        dbg_rt("\targ#%d=(%s)%s", idx, jtypes[jt].name, str_value);
    }
}

void CodeGen::gen_dbg_check_stack(bool start)
{
    if (m_infoBlock.get_bc_size() == 1 && m_bc[0] == OPCODE_RETURN) {
        return; // empty method, nothing to do
    }
    if (start) {
        // We store SP before a code to be checked ...
        st(jobj, sp, m_base, voff(m_stack.dbg_scratch()));
        return;
    }
    // ... and check right after
    AR gr = valloc(jobj);
    ld(jobj, gr, m_base, voff(m_stack.dbg_scratch()));
    alu(jobj, alu_cmp, gr, sp);
    unsigned br_off = br(eq, 0, hint_none);
    gen_dbg_rt(false, "Corrupted stack @ %s @ PC=%d", meth_fname(), m_pc);
    trap();
    patch(br_off, ip());
}

void Compiler::gen_dbg_check_bb_stack(void)
{
    if (m_infoBlock.get_bc_size() == 1 && m_bc[0] == OPCODE_RETURN) {
        return; // empty method, nothing to do
    }
    // With the current code generation scheme, the depth of native stack
    // at the beginning of a basic block is exactly the same and is equal
    // to the stack depth right after the method prolog.
    // So, this check is enforced
    
    alu(alu_sub, m_base, m_stack.size());
    alu(jobj, alu_cmp, m_base, sp);
    unsigned br_off = br(eq, 0, hint_none);
    gen_dbg_rt(false, "Corrupted stack @ %s @ BB=%d", meth_fname(), m_pc);
    gen_brk();
    patch(br_off, ip(br_off), ip());
    alu(alu_add, m_base, m_stack.size());
}

void CodeGen::gen_dbg_rt(bool save_regs, const char * frmt, ...)
{
    char tmp_buf[1024*5], id_buf[20];
    va_list valist;
    va_start(valist, frmt);
    int len = vsprintf(tmp_buf, frmt, valist);
    
    len += snprintf(id_buf, sizeof(id_buf)-1, 
                    "| meth_id=%u@%u", m_methID, m_pc);
    
    // yes, there is a kind of leak here but it's intentional: 
    // this is debugging only feature and methods live during the whole 
    // VM's life so the pointer could not be freed anyway. 
    char *lost = new char[len + 1];
    strcpy(lost, tmp_buf);
    strcat(lost, id_buf);
    if (save_regs) { push_all(); }
    SYNC_FIRST(static const CallSig cs(CCONV_STDCALL, jvoid, jobj));
    call(is_set(DBG_CHECK_STACK), gr0, (void*)&dbg_rt_out, cs, 0, lost);
    if (save_regs) { pop_all(); }
}


}}; // ~namespace Jitrino::Jet
