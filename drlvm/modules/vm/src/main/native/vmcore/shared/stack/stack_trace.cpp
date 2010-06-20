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

#define LOG_DOMAIN "vm.stack"
#include "cxxlog.h"
#include "vm_log.h"
#include "open/vm_class_info.h"

#include "open/vm_method_access.h"
#include "m2n.h"
#include "stack_iterator.h"
#include "stack_trace.h"
#include "interpreter.h"
#include "jit_intf_cpp.h"
#include "environment.h"
#include "cci.h"
#include "class_member.h"
#include "open/hythread.h"

void get_file_and_line(Method_Handle mh, void *ip, bool is_ip_past,
                        int depth, const char **file, int *line) {
    Method *method = (Method*)mh;
    *file = class_get_source_file_name(method_get_class(method));

    if (method_is_native(method)) {
        *line = -2;
        return;
    }

    *line = -1;
    if (interpreter_enabled()) {
        int bc = (int)((U_8*)ip - (U_8*)method->get_byte_code_addr());
        *line = method->get_line_number((uint16)bc);
        return;
    }

    if (ip == NULL) {
        return;
    }


#if !defined(_IPF_) // appropriate callLength should be put here when IPF bc maping will be implemented
    uint16 bcOffset;
    POINTER_SIZE_INT callLength = 5;

    Global_Env * vm_env = VM_Global_State::loader_env;

    CodeChunkInfo* cci;
    Method_Handle m = vm_env->em_interface->LookupCodeChunk(ip, is_ip_past,
        NULL, NULL, reinterpret_cast<void **>(&cci));
    assert(NULL != m);
    assert(cci);

    POINTER_SIZE_INT eff_ip = (POINTER_SIZE_INT)ip -
                                (is_ip_past ? callLength : 0);

    U_32 offset = 0;
    if (depth < 0) // Not inlined method
    {
        if (cci->get_jit()->get_bc_location_for_native(
            method, (NativeCodePtr)eff_ip, &bcOffset) != EXE_ERROR_NONE)
            return;
    }
    else // Inlined method
    {
        InlineInfoPtr inl_info = cci->get_inline_info();

        if (inl_info)
        {
            offset = (U_32) ((POINTER_SIZE_INT)ip -
                (POINTER_SIZE_INT)cci->get_code_block_addr());
            bcOffset = cci->get_jit()->get_inlined_bc(inl_info, offset, depth);
        }
    }

    *line = method->get_line_number(bcOffset);
    TRACE("Location of " << method << " at idepth=" << depth << " noff=" << offset
        << " bc=" << bcOffset << " line=" << *line);
#endif        
}

unsigned st_get_depth(VM_thread *p_vmthread)
{
    ASSERT_NO_INTERPRETER
    StackIterator* si = (StackIterator*) STD_ALLOCA(si_size());
    si_fill_from_native(si, p_vmthread);

    unsigned depth = 0;
    while (!si_is_past_end(si)) {
        if (si_get_method(si)) {
            depth += 1 + si_get_inline_depth(si);
        }
        si_goto_previous(si);
    }
    return depth;
}

bool st_get_frame(unsigned target_depth, StackTraceFrame* stf)
{
    if (interpreter_enabled()) {
        return interpreter.interpreter_st_get_frame(target_depth, stf);
    }

    TRACE("looking for frame: "<<target_depth);
    StackIterator* si = si_create_from_native();
    unsigned depth = 0;
    while (!si_is_past_end(si)) {
        stf->method = si_get_method(si);
        stf->depth = -1;
        if (stf->method) {
            U_32 inlined_depth = si_get_inline_depth(si);
            if ( (target_depth >= depth) && 
                 (target_depth <= depth + inlined_depth) ) {
                stf->ip = si_get_ip(si);

                if (target_depth != depth + inlined_depth) {
                    assert(inlined_depth);
                    CodeChunkInfo* cci = si_get_code_chunk_info(si);
                    // FIXME64: no support for large methods
                    // with compiled code size greater than 4GB
                    U_32 offset = (U_32)((POINTER_SIZE_INT)stf->ip - (POINTER_SIZE_INT)cci->get_code_block_addr());
                    stf->depth = inlined_depth - (target_depth - depth);
                    stf->method = cci->get_jit()->get_inlined_method(
                            cci->get_inline_info(), offset, stf->depth);
                    TRACE("found inlined frame: "<<stf->method << " at depth="<<stf->depth << " of "<< inlined_depth);
                }
                else {
                    TRACE("found frame: "<<stf->method);
                }

                si_free(si);
                return true;
            }
            depth += inlined_depth + 1;
        }
        si_goto_previous(si);
    }
    si_free(si);
    return false;
}

VMEXPORT StackTraceFrame* 
st_alloc_frames(int num) {
    return (StackTraceFrame*) STD_MALLOC(sizeof(StackTraceFrame)*num);
}

static inline void *get_this(JIT *jit, Method *method, StackIterator *si) {
    if (method->is_static()) return 0;
    void **address_of_this = (void**)jit->get_address_of_this(method, si_get_jit_context(si));
    if (address_of_this) {
        return *address_of_this;
    }
    return NULL;
}

void st_get_trace(VM_thread *p_vmthread, unsigned* res_depth, StackTraceFrame** stfs)
{
    tmn_suspend_disable();
    if (interpreter_enabled()) {
        interpreter.interpreter_st_get_trace(p_vmthread, res_depth, stfs);
        tmn_suspend_enable();
        return;
    }

    unsigned depth = st_get_depth(p_vmthread);
    StackTraceFrame* stf = st_alloc_frames(depth);

    if (stf == NULL) {
        *res_depth = depth;
        *stfs = NULL;
        tmn_suspend_enable();
        return;
    }

    assert(stf);
    *res_depth = depth;
    *stfs = stf;
    StackIterator* si = si_create_from_native(p_vmthread);

    depth = 0;
    while (!si_is_past_end(si)) {
        Method_Handle method = si_get_method(si);

        if (method) {
            NativeCodePtr ip = si_get_ip(si);
            CodeChunkInfo* cci = si_get_code_chunk_info(si);
            if ( cci == NULL ) {
                stf->outdated_this = 0;
            } else {
                JIT *jit = cci->get_jit();
                if (cci->has_inline_info()) {
                    // FIXME64: no support for large methods
                    // with compiled code greater than 4GB
                    U_32 offset = (U_32)((POINTER_SIZE_INT)ip - (POINTER_SIZE_INT)cci->get_code_block_addr());
                    U_32 inlined_depth = jit->get_inline_depth(
                        cci->get_inline_info(), offset);
                    if (inlined_depth) {
                        for (U_32 i = inlined_depth; i > 0; i--) {
                            stf->method = jit->get_inlined_method(cci->get_inline_info(), offset, i);
                            stf->ip = ip;
                            stf->depth = i;
                            TRACE("tracing inlined frame: "<<stf->method << " at depth="<<stf->depth);
                            stf->outdated_this = get_this(jit, method, si);
                            stf++;
                            depth++;
                        }
                    }
                }
                stf->outdated_this = get_this(jit, method, si);
            }
            stf->method = method;
            TRACE("tracing frame: "<<stf->method);
            stf->ip = ip;
            stf->depth = -1;
            stf++;
            depth++;
        }
        si_goto_previous(si);
    }
    assert(depth==*res_depth);
    si_free(si);
    tmn_suspend_enable();
}

void st_print_frame(ExpandableMemBlock* buf, StackTraceFrame* stf)
{
    const char* cname = stf->method->get_class()->get_name()->bytes;
    const char* mname = stf->method->get_name()->bytes;
    const char* dname = stf->method->get_descriptor()->bytes;
    buf->AppendFormatBlock("\tat %s.%s%s", cname, mname, dname);
    const char *file;
    int line;
    get_file_and_line(stf->method, stf->ip, false, stf->depth, &file, &line);

    if (line==-2)
        // Native method
        buf->AppendBlock(" (Native Method)");
    else if (file)
        if (line==-1)
            buf->AppendFormatBlock(" (%s)", file);
        else
            buf->AppendFormatBlock(" (%s:%d)", file, line);
    else if (stf->ip)
        buf->AppendFormatBlock(" (ip=%p)", stf->ip);
    buf->AppendBlock("\n");
}

void st_print_all(FILE* f) {
    hythread_t native_thread;
    hythread_iterator_t  iterator;

    assert(hythread_is_suspend_enabled());

    hythread_suspend_all(&iterator, NULL);
    while(native_thread = hythread_iterator_next(&iterator)) {
        st_print(f, native_thread);
    }
    hythread_resume_all(NULL);
}

void st_print(FILE* f, hythread_t thread)
{
    assert(hythread_is_suspend_enabled());

    vm_thread_t vm_thread = jthread_get_vm_thread(thread);
    
    if (vm_thread == NULL) {
        // Do not print native stack.
        return;
    }
    if (vm_thread != p_TLS_vmthread) {
        hythread_suspend_other(thread);
    }
    
    // TODO: print real java name
    fprintf(f, "The stack trace of the %p java thread:\n", vm_thread);

    if (interpreter_enabled()) {
        int fd;
#ifdef PLATFORM_NT
        fd = _fileno(f);
#else
        fd = fileno(f);
#endif
        interpreter.stack_dump(fd, vm_thread);
        fflush(f);
        return;
    }

    StackIterator* si = si_create_from_native(vm_thread);
    unsigned depth = 0;
    while (!si_is_past_end(si)) {
        Method_Handle m = si_get_method(si);

        if (m) {
            fprintf(f, "  [%p] %p(%c): ", vm_thread, si_get_ip(si), (si_is_native(si) ? 'n' : 'm'));
            CodeChunkInfo* cci = si_get_code_chunk_info(si);
            if (cci != NULL && cci->has_inline_info()) {
                // FIXME64: no support for large methods
                // with compiled code size greater than 4GB
                U_32 offset = (U_32)((POINTER_SIZE_INT)si_get_ip(si) - (POINTER_SIZE_INT)cci->get_code_block_addr());
                U_32 inlined_depth = cci->get_jit()->get_inline_depth(
                    cci->get_inline_info(), offset);
                if (inlined_depth) {
                    for (U_32 i = inlined_depth; i > 0; i--) {
                        Method *real_method = cci->get_jit()->get_inlined_method(cci->get_inline_info(), offset, i);
                        fprintf(f, "%s.%s%s\n", real_method->get_class()->get_name()->bytes, 
                            real_method->get_name()->bytes, real_method->get_descriptor()->bytes);
                        depth++;
                    }
                }
            }
            fprintf(f, "%s.%s%s\n", m->get_class()->get_name()->bytes,
                m->get_name()->bytes, m->get_descriptor()->bytes);
        }
        depth++;
        si_goto_previous(si);
    }
    si_free(si);

    if (vm_thread != p_TLS_vmthread) {
        hythread_resume(thread);
    }
    fprintf(f, "\n");
    fflush(f);
}

void st_print() {
    st_print(stderr, hythread_self());
}
