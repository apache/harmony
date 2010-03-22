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
#define LOG_DOMAIN "compile"
#include "cxxlog.h"
#include "vm_log.h"

#include "open/gc.h"
#include "open/vm_ee.h"
#include "open/vm_type_access.h"
#include "open/vm_method_access.h"
#include "open/vm_class_manipulation.h"

#include "apr_strings.h"
#include "lock_manager.h"
#include "classloader.h"
#include "exceptions.h"
#include "native_overrides.h"
#include "jit_intf_cpp.h"
#include "em_intf.h"
#include "heap.h"
#include "vm_strings.h"
#include "compile.h"
#include "jit_runtime_support.h"
#include "lil_code_generator.h"
#include "stack_iterator.h"
#include "interpreter.h"
#include "jvmti_internal.h"
#include "jvmti_break_intf.h"
#include "cci.h"

#include "vm_stats.h"
#include "dump.h"
#include "port_threadunsafe.h"

extern bool parallel_jit;

#define METHOD_NAME_BUF_SIZE 512

Global_Env* compile_handle_to_environment(Compile_Handle h)
{
    return ((Compilation_Handle*)h)->env;
}


int get_index_of_jit(JIT *jit)
{
    int idx;
    JIT **j;
    for (j = jit_compilers, idx = 0;  *j;  j++, idx++) {
        if (*j == jit) {
            return idx;
        }
    }
    return -999;
} //get_index_of_jit


////////////////////////////////////////////////////////////////////////
// begin CodeChunkInfo


CodeChunkInfo::CodeChunkInfo()
{
    _jit    = 0;
    _method = 0;
    _id     = 0;
    _relocatable = TRUE;
    _num_target_exception_handlers = 0;
    _target_exception_handlers     = NULL;
    _heat        = 0;
    _code_block     = NULL;
    _jit_info_block = NULL;
    _code_block_size      = 0;
    _jit_info_block_size  = 0;
    _code_block_alignment = 0;
    _data_blocks = NULL;
    _next        = NULL;
#ifdef VM_STATS
    num_throws  = 0;
    num_catches = 0;
    num_unwind_java_frames_gc = 0;
    num_unwind_java_frames_non_gc = 0;
#endif
} //CodeChunkInfo::CodeChunkInfo

int CodeChunkInfo::get_jit_index() const
{
    return get_index_of_jit(_jit);
}

unsigned CodeChunkInfo::get_num_target_exception_handlers() const
{
    if (_id==0) {
        return _num_target_exception_handlers;
    } else {
        return _method->get_num_target_exception_handlers(_jit);
    }
}

Target_Exception_Handler_Ptr CodeChunkInfo::get_target_exception_handler_info(unsigned eh_num) const
{
    if (_id==0) {
        return _target_exception_handlers[eh_num];
    } else {
        return _method->get_target_exception_handler_info(_jit, eh_num);
    }
}


void CodeChunkInfo::print_name() const
{
    assert(_method);
    const char* c = _method->get_class()->get_name()->bytes;
    const char* m = _method->get_name()->bytes;
    const char* d = _method->get_descriptor()->bytes;
    printf("%d:%d:%s.%s%s", get_jit_index(), get_id(), c, m, d);
} //CodeChunkInfo::print_name


void CodeChunkInfo::print_name(FILE *file) const
{
    assert(_method);
    const char* c = _method->get_class()->get_name()->bytes;
    const char* m = _method->get_name()->bytes;
    const char* d = _method->get_descriptor()->bytes; 
    fprintf(file, "%d:%d:%s.%s%s", get_jit_index(), get_id(), c, m, d);
} //CodeChunkInfo::print_name


void CodeChunkInfo::print_info(bool print_ellipses) const
{
    size_t code_size = get_code_block_size();
    print_name();
    printf(", %d bytes%s\n", (unsigned)code_size, (print_ellipses? "..." : ""));
} //CodeChunkInfo::print_info

// end CodeChunkInfo
////////////////////////////////////////////////////////////////////////



////////////////////////////////////////////////////////////////////////
// begin JIT management


JIT *jit_compilers[] = {0, 0, 0, 0, 0, 0, 0};

void vm_add_jit(JIT *jit)
{
    int max_jit_num = sizeof(jit_compilers) / sizeof(JIT *) - 2;
    if(jit_compilers[max_jit_num]) {
        LDIE(64, "Can't add new JIT");
        return;
    }

    // Shift the jits
    for(int i = max_jit_num; i > 0; i--) {
        jit_compilers[i] = jit_compilers[i - 1];
    }

    jit_compilers[0] = jit;
    assert(jit_compilers[max_jit_num + 1] == 0);
} //vm_add_jit


void vm_delete_all_jits()
{
    JIT **jit;
    for(jit = jit_compilers; *jit; jit++) {
        delete (*jit);
        *jit = 0;
    }

} //vm_delete_all_jits


// end JIT management
////////////////////////////////////////////////////////////////////////

//////////////////////////////////////////////////////////////////////////
// JNI Stubs

static bool is_reference(Type_Info_Handle tih)
{
    return type_info_is_reference(tih) || type_info_is_vector(tih);
} //is_reference

// Implementation note: don't use l2 (use l3, l4 instead if required) since its
// space can be used in case of 64-bit return value.
NativeCodePtr compile_create_lil_jni_stub(Method_Handle method, void* func, NativeStubOverride nso)
{
    ASSERT_NO_INTERPRETER;
    const Class_Handle clss = method->get_class();
    bool is_static = method->is_static();
    bool is_synchronised = method->is_synchronized();
    Method_Signature_Handle msh = method_get_signature(method);
    unsigned num_args = method->get_num_args();
    Type_Info_Handle ret_tih = method_ret_type_get_type_info(msh);
    VM_Data_Type ret_type = type_info_get_type(ret_tih);
    unsigned i;

    unsigned num_ref_args = 0; // among original args, does not include jclass for static methods
    for(i=0; i<num_args; i++)
        if (is_reference(method_args_get_type_info(msh, i))) num_ref_args++;

    //***** Part 1: Entry, Stats, Override, push m2n, allocate space for handles
    LilCodeStub* cs = lil_parse_code_stub("entry 0:managed:%0m;",
                                          method);
    assert(cs);

    // Increment stats (total number of calls)
#ifdef VM_STATS
    cs = lil_parse_onto_end(cs,
                            "inc [%0i:pint];",
                            &((Method*)method)->num_accesses);
    assert(cs);
#endif //VM_STATS

    // Do stub override here
    if (nso) cs = nso(cs, method);
    assert(cs);

    // Increment stats (number of nonoverridden calls)
#ifdef VM_STATS
    cs = lil_parse_onto_end(cs,
                            "inc [%0i:pint];",
                            &((Method*)method)->num_slow_accesses);
    assert(cs);
#endif

    // Push M2nFrame
    cs = lil_parse_onto_end(cs, "push_m2n %0i, %1i, handles; locals 3;",
                            method, (POINTER_SIZE_INT)FRAME_JNI);
    assert(cs);

    // Allocate space for handles
    unsigned number_of_object_handles = num_ref_args + (is_static ? 1 : 0);
    cs = oh_gen_allocate_handles(cs, number_of_object_handles, "l0", "l1");
    assert(cs);

    //***** Part 2: Initialize object handles

    if (is_static) {
        void *jlc = clss->get_class_handle();
        cs = lil_parse_onto_end(cs,
                                //"ld l1,[%0i:pint];"
                                "ld l1,[%0i:ref];",
                                jlc);
        assert(cs);
        cs = oh_gen_init_handle(cs, "l0", 0, "l1", false);
        assert(cs);
    } else {
        cs = oh_gen_init_handle(cs, "l0", 0, "i0", true);
    }

    // The remaining handles are for the proper arguments (not including this)
    // Loop over the arguments, skipping 0th argument for instance methods. If argument is a reference, generate code
    unsigned hn = 1;
    for(i=(is_static?0:1); i<num_args; i++) {
        if (is_reference(method_args_get_type_info(msh, i))) {
            char buf[20];
            sprintf(buf, "i%d", i);
            cs = oh_gen_init_handle(cs, "l0", hn, buf, true);
            assert(cs);
            hn++;
        }
    }

    //***** Part 3: Synchronize
    if (is_synchronised) {
        if (is_static) {
            cs = lil_parse_onto_end(cs,
                                    "out stdcall:pint:pint;"
                                    "o0=%0i;"
                                    "call %1i;"
                                    "out stdcall:pint:void;"
                                    "o0=r;"
                                    "call %2i;",
                                    clss,
                                    lil_npc_to_fp(vm_helper_get_addr(VM_RT_CLASS_2_JLC)),
                                    lil_npc_to_fp(vm_helper_get_addr(VM_RT_MONITOR_ENTER)));
            assert(cs);
        } else {
            cs = lil_parse_onto_end(cs,
                                    "out stdcall:ref:void;"
                                    "o0=i0;"
                                    "call %0i;",
                                    lil_npc_to_fp(vm_helper_get_addr(VM_RT_MONITOR_ENTER)));
            assert(cs);
        }
    }

    //***** Call JVMTI MethodEntry
    DebugUtilsTI* ti = VM_Global_State::loader_env->TI;
    if (ti->isEnabled() &&
        ti->get_global_capability(DebugUtilsTI::TI_GC_ENABLE_METHOD_ENTRY))
    {
        cs = lil_parse_onto_end(cs,
                                "out platform:pint:void;"
                                "o0=%0i:pint;"
                                "call %1i;",
                                (jmethodID)method,
                                jvmti_process_method_entry_event);
        assert(cs);
    }

    //***** Part 4: Enable GC
    cs = lil_parse_onto_end(cs,
                            "out platform::void;"
                            "call %0i;",
                            hythread_suspend_enable);
    assert(cs);

    //***** Part 5: Set up arguments

    // Setup outputs, set JNIEnv, set class/this handle
    cs = lil_parse_onto_end(cs,
                            "out jni:%0j;"
                            "l1=ts;"
                            "ld o0,[l1 + %1i:pint];"
                            "o1=l0+%2i;",
                            method,
                            (POINTER_SIZE_INT)APR_OFFSETOF(VM_thread, jni_env),
                            oh_get_handle_offset(0));
    assert(cs);

    // Loop over arguments proper, setting rest of outputs
    unsigned int arg_base = 1 + (is_static ? 1 : 0);
    hn = 1;
    for(i=(is_static?0:1); i<num_args; i++) {
        if (is_reference(method_args_get_type_info(msh, i))) {
            POINTER_SIZE_INT handle_offset = oh_get_handle_offset(hn);
            REFS_RUNTIME_SWITCH_IF
#ifdef REFS_RUNTIME_OR_COMPRESSED
                cs = lil_parse_onto_end(cs,
                                        "jc i%0i=%1i:ref,%n;"
                                        "o%2i=l0+%3i;"
                                        "j %o;"
                                        ":%g;"
                                        "o%4i=0;"
                                        ":%g;",
                                        i,
                                        VM_Global_State::loader_env->managed_null,
                                        arg_base+i, handle_offset, arg_base+i);
#endif // REFS_RUNTIME_OR_COMPRESSED
            REFS_RUNTIME_SWITCH_ELSE
#ifdef REFS_RUNTIME_OR_UNCOMPRESSED
                cs = lil_parse_onto_end(cs,
                                        "jc i%0i=0:ref,%n;"
                                        "o%1i=l0+%2i;"
                                        "j %o;"
                                        ":%g;"
                                        "o%3i=0;"
                                        ":%g;",
                                        i,
                                        arg_base+i, handle_offset,
                                        arg_base+i);
#endif // REFS_RUNTIME_OR_UNCOMPRESSED
            REFS_RUNTIME_SWITCH_ENDIF
            hn++;
        } else {
            cs = lil_parse_onto_end(cs, "o%0i=i%1i;", arg_base+i, i);
        }
        assert(cs);
    }

    //***** Part 6: Call
    cs = lil_parse_onto_end(cs,
                            "call %0i;",
                            func);
    assert(cs);

    //***** Part 7: Save return, widening if necessary
    switch (ret_type) {
    case VM_DATA_TYPE_VOID:
        break;
    case VM_DATA_TYPE_INT32:
        cs = lil_parse_onto_end(cs, "l1=r;");
        break;
    case VM_DATA_TYPE_BOOLEAN:
        cs = lil_parse_onto_end(cs, "l1=zx1 r;");
        break;
    case VM_DATA_TYPE_INT16:
        cs = lil_parse_onto_end(cs, "l1=sx2 r;");
        break;
    case VM_DATA_TYPE_INT8:
        cs = lil_parse_onto_end(cs, "l1=sx1 r;");
        break;
    case VM_DATA_TYPE_CHAR:
        cs = lil_parse_onto_end(cs, "l1=zx2 r;");
        break;
    default:
        cs = lil_parse_onto_end(cs, "l1=r;");
        break;
    }
    assert(cs);

    //***** Part 8: Disable GC
    cs = lil_parse_onto_end(cs,
                            "out platform::void;"
                            "call %0i;",
                            hythread_suspend_disable);
    assert(cs);

    // Exception offsets
    POINTER_SIZE_INT eoo = (POINTER_SIZE_INT)&((VM_thread*)0)->thread_exception.exc_object;
    POINTER_SIZE_INT eco = (POINTER_SIZE_INT)&((VM_thread*)0)->thread_exception.exc_class;

    //***** Call JVMTI MethodExit
    if (ti->isEnabled() &&
        ti->get_global_capability(DebugUtilsTI::TI_GC_ENABLE_METHOD_EXIT))
    {
        cs = lil_parse_onto_end(cs,
                                "out platform:pint,g1,g8:void;"
                                "l2=ts;"
                                "ld l2,[l2+%0i:ref];"
                                "jc l2!=0,_mex_exn_raised;"
                                "l2=ts;"
                                "ld l2,[l2+%1i:ref];"
                                "jc l2!=0,_mex_exn_raised;"
                                "o1=%2i:g1;"
                                "o2=l1:g8;"
                                "j _mex_exn_cont;"
                                ":_mex_exn_raised;"
                                "o1=%3i:g1;"
                                "o2=0:g8;"
                                ":_mex_exn_cont;"
                                "o0=%4i:pint;"
                                "call %5i;",
                                eoo,
                                eco,
                                (POINTER_SIZE_INT)JNI_FALSE,
                                (POINTER_SIZE_INT)JNI_TRUE,
                                (jmethodID)method,
                                jvmti_process_method_exit_event);
        assert(cs);
    }

    //***** Part 9: Synchronize
    if (is_synchronised) {
        if (is_static) {
            cs = lil_parse_onto_end(cs,
                "out stdcall:pint:pint;"
                "o0=%0i;"
                "call %1i;"
                "out stdcall:pint:void;"
                "o0=r;"
                "call %2i;",
                clss,
                lil_npc_to_fp(vm_helper_get_addr(VM_RT_CLASS_2_JLC)),
                lil_npc_to_fp(vm_helper_get_addr(VM_RT_MONITOR_EXIT)));
        } else {
            cs = lil_parse_onto_end(cs,
                "ld l0,[l0+%0i:ref];"
                "out stdcall:ref:void; o0=l0; call %1i;",
                oh_get_handle_offset(0),
                lil_npc_to_fp(vm_helper_get_addr(VM_RT_MONITOR_EXIT)));
        }
        assert(cs);
    }

    //***** Part 10: Unhandle the return if it is a reference
    if (is_reference(ret_tih)) {
        cs = lil_parse_onto_end(cs,
                                "jc l1=0,ret_done;"
                                "ld l1,[l1+0:ref];"
                                ":ret_done;");
#ifdef REFS_RUNTIME_OR_COMPRESSED
        REFS_RUNTIME_SWITCH_IF
            cs = lil_parse_onto_end(cs,
                                    "jc l1!=0,done_translating_ret;"
                                    "l1=%0i:ref;"
                                    ":done_translating_ret;",
                                    VM_Global_State::loader_env->managed_null);
        REFS_RUNTIME_SWITCH_ENDIF
#endif // REFS_RUNTIME_OR_UNCOMPRESSED
        assert(cs);
    }

    //***** Part 11: Rethrow exception
    cs = lil_parse_onto_end(cs,
                            "l0=ts;"
                            "ld l2,[l0+%0i:ref];"
                            "jc l2!=0,_exn_raised;"
                            "ld l2,[l0+%1i:ref];"
                            "jc l2=0,_no_exn;"
                            ":_exn_raised;"
                            "m2n_save_all;"
                            "out platform::void;"
                            "call.noret %2i;"
                            ":_no_exn;",
                            eoo, eco, exn_rethrow);
    assert(cs);

    //***** Part 12: Restore return variable, pop_m2n, return
    if (ret_type != VM_DATA_TYPE_VOID) {
        cs = lil_parse_onto_end(cs, "r=l1;");
        assert(cs);
    }
    cs = lil_parse_onto_end(cs,
                            "pop_m2n;"
                            "ret;");
    assert(cs);

    //***** Now generate code

    assert(lil_is_valid(cs));
    NativeCodePtr addr = LilCodeGenerator::get_platform()->compile(cs, clss->get_class_loader()->GetCodePool());

#ifndef NDEBUG
    char buf[100];
    apr_snprintf(buf, sizeof(buf)-1, "jni_stub.%s::%s", clss->get_name()->bytes,
        method->get_name()->bytes);
    DUMP_STUB(addr, buf, lil_cs_get_code_size(cs));
#endif

#ifdef VM_STATS
    VM_Statistics::get_vm_stats().jni_stub_bytes += lil_cs_get_code_size(cs);
#endif

    lil_free_code_stub(cs);
    return addr;
} // compile_create_lil_jni_stub


//////////////////////////////////////////////////////////////////////////
// PInvoke Stubs



/////////////////////////////////////////////////////////////////
// begin Support for stub override code sequences 


static int get_override_index(Method* method)
{
    const char* clss_name = method->get_class()->get_name()->bytes;
    const char* meth_name = method->get_name()->bytes;
    const char* meth_desc = method->get_descriptor()->bytes;
    for (int i = 0;  i < sizeof_stub_override_entries;  i++) {
        if ((strcmp(clss_name, stub_override_entries[i].class_name) == 0)
            && (strcmp(meth_name, stub_override_entries[i].method_name) == 0)
            && (strcmp(meth_desc, stub_override_entries[i].descriptor) == 0))
        {
            return i;
        }
    }
    return -1;
}

bool needs_override(Method *method) {
    int idx = get_override_index(method);
    if (idx < 0)
        return false;
    Override_Generator *override_generator = stub_override_entries[idx].override_generator;
    return (override_generator != NULL);
}    

// end Support for stub override code sequences
/////////////////////////////////////////////////////////////////


//////////////////////////////////////////////////////////////////////////
// Direct Call Support
// 20040113: This is deprecated and will go away soon

#define REWRITE_INVALID_TYPE                        0  
#define REWRITE_PATCH_COMPILE_ME_STUB               1
#define REWRITE_PATCH_CALLER                        2
#define REWRITE_PATCH_OLD_CODE_TO_REWRITER          3

//////////////////////////////////////////////////////////////////////////
// Compilation of Methods

static NativeCodePtr compile_create_jni_stub(Method_Handle method, GenericFunctionPointer func, NativeStubOverride nso)
{
    return compile_create_lil_jni_stub(method, (void*)func, nso);
} //compile_create_jni_stub

static JIT_Result compile_prepare_native_method(Method* method)
{
    TRACE("compile_prepare_native_method (" << method->get_name()->bytes << ")");
#ifdef VM_STATS
    VM_Statistics::get_vm_stats().num_native_methods++;
#endif
    assert(method->is_native());

    GenericFunctionPointer func = classloader_find_native(method);

    if (!func)
        return JIT_FAILURE;

    // Calling callback for NativeMethodBind event
    jvmti_process_native_method_bind_event( (jmethodID) method, (NativeCodePtr)func, (NativeCodePtr*)&func);

    Class* cl = method->get_class();
    NativeStubOverride nso = nso_find_method_override(VM_Global_State::loader_env,
                                                    cl->get_name(), method->get_name(),
                                                    method->get_descriptor());

    NativeCodePtr stub = compile_create_jni_stub(method, func, nso);
    if (!stub)
        return JIT_FAILURE;

    method->lock();
    method->set_code_addr(stub);
    method->unlock();

    return JIT_SUCCESS;
} // compile_prepare_native_method


JIT_Result compile_do_compilation_jit(Method* method, JIT* jit)
{
    // Time stamp for counting the total compilation time
    apr_time_t start;

    Global_Env * vm_env = VM_Global_State::loader_env;

    assert(method);
    assert(jit);

    if (!parallel_jit) {
        vm_env->p_jit_a_method_lock->_lock();
        // MikhailF reports that each JIT in recompilation chain has its own
        // JIT* pointer.
        // If in addition to recompilation chains one adds recompilation loops,
        // this check can be skipped, or main_code_chunk_id should be
        // modified.

        if (NULL != method->get_chunk_info_no_create_mt(jit, CodeChunkInfo::main_code_chunk_id)) {
            vm_env->p_jit_a_method_lock->_unlock();
            return JIT_SUCCESS;
        }
    }

    OpenMethodExecutionParams flags = {0}; 
    jvmti_get_compilation_flags(&flags);
    flags.exe_insert_write_barriers = gc_requires_barriers();

    Compilation_Handle ch;
    ch.env = VM_Global_State::loader_env;
    ch.jit = jit;

    start = apr_time_now();

    TRACE("compile_do_compilation_jit(): calling jit->compile_method_with_params() for method " << method );

    JIT_Result res = jit->compile_method_with_params(&ch, method, flags);

    TRACE("compile_do_compilation_jit(): returned from jit->compile_method_with_params() for method " << method );

    UNSAFE_REGION_START
    // Non-atomic increment of statistic counter
    // Conversion from microseconds to milliseconds
    vm_env->total_compilation_time += ((apr_time_now() - start)/1000);
    UNSAFE_REGION_END

    if (JIT_SUCCESS != res) {
        if (!parallel_jit) {
            vm_env->p_jit_a_method_lock->_unlock();
        }
        return res;
    }

    method->lock();
    for (CodeChunkInfo* cci = method->get_first_JIT_specific_info();  cci;  cci = cci->_next) {
        if (cci->get_jit() == jit) {
            compile_flush_generated_code_block((U_8*)cci->get_code_block_addr(), cci->get_code_block_size());
            // We assume the main chunk starts from entry point
            if (cci->get_id() == CodeChunkInfo::main_code_chunk_id) {
                method->set_code_addr(cci->get_code_block_addr());
            }
        }
    }

    // Commit the compilation by setting the method's code address
    method->set_state(Method::ST_Compiled);
    method->do_jit_recompiled_method_callbacks();
    method->apply_vtable_patches();
    method->unlock();
    if (!parallel_jit) {
        vm_env->p_jit_a_method_lock->_unlock();
    }

    // Find TI environment
    DebugUtilsTI *ti = vm_env->TI;

    // Call TI callbacks
    if (jvmti_should_report_event(JVMTI_EVENT_COMPILED_METHOD_LOAD)
        && ti->getPhase() == JVMTI_PHASE_LIVE)
    {
        jvmti_send_chunks_compiled_method_load_event(method);
    }
    return JIT_SUCCESS;
}

// Create an exception from a given type and a message.
// Set cause to the current thread exception.
static void compile_raise_exception(const char* name, const char* message, Method* method)
{
    assert(hythread_is_suspend_enabled());
    jthrowable old_exc = exn_get();
    exn_clear();

    const char* c = method->get_class()->get_name()->bytes;
    const char* m = method->get_name()->bytes;
    const char* d = method->get_descriptor()->bytes;
    size_t sz = 3 + // a space, a dot, and a terminator
        strlen(message) +
        method->get_class()->get_name()->len +
        method->get_name()->len +
        method->get_descriptor()->len;
    char* msg_raw = (char*)STD_MALLOC(sz);
    assert(msg_raw);
    sprintf(msg_raw, "%s%s.%s%s", message, c, m, d);
    assert(strlen(msg_raw) < sz);

    jthrowable new_exc = exn_create(name, msg_raw, old_exc);
    exn_raise_object(new_exc);
    STD_FREE(msg_raw);
}


static JIT_Result compile_do_compilation(Method* method)
{
    ASSERT_RAISE_AREA;
    assert(hythread_is_suspend_enabled());
    tmn_suspend_disable();
    class_initialize(method->get_class());
    tmn_suspend_enable();
   
    method->lock();
    if (exn_raised()) {
        method->unlock();
        return JIT_FAILURE;
    } else if (method->get_state() == Method::ST_Compiled) {
        method->unlock();
        return JIT_SUCCESS;
    } else if (method->get_state()==Method::ST_NotCompiled && exn_raised()) {
        method->unlock();
        return JIT_FAILURE;
    } else if(!check_available_stack_size(0x8000)) {
        method->unlock();
        return JIT_FAILURE;
    }

    if (method->is_native()) {
        JIT_Result res = compile_prepare_native_method(method);            
        if (res == JIT_SUCCESS) {
            compile_flush_generated_code();
            method->set_state(Method::ST_Compiled);
            method->do_jit_recompiled_method_callbacks();
            method->apply_vtable_patches();
        } else {
            method->set_state(Method::ST_NotCompiled);
            compile_raise_exception("java/lang/UnsatisfiedLinkError", "Cannot load native ", method);
        }
        method->unlock();
        return res;
    } else {
        // Call an execution manager to compile the method.
        // An execution manager is safe to call from multiple threads.
        method->unlock();
        return VM_Global_State::loader_env->em_interface->CompileMethod(method);
    }
}


NativeCodePtr compile_me(Method* method)
{
    ASSERT_RAISE_AREA;
    ASSERT_NO_INTERPRETER;
    TRACE("compile_me " << method);

    GcFrame gc;
    compile_protect_arguments(method, &gc);

    if (exn_raised()) {
        return NULL;
     }

    tmn_suspend_enable();
    if (method->is_abstract()) { 
        compile_raise_exception("java/lang/AbstractMethodError", "", method); 
        tmn_suspend_disable(); 
        return NULL; 
    }

    DebugUtilsTI *ti = VM_Global_State::loader_env->TI;
    JIT_Result res = compile_do_compilation(method);

    if (res != JIT_SUCCESS) {
        INFO2("compile", "Cannot compile " << method);
        if (!exn_raised()) {
            compile_raise_exception("java/lang/InternalError", "Cannot compile ", method);
        }
        tmn_suspend_disable();
        return NULL;
    }
    tmn_suspend_disable();

    NativeCodePtr entry_point = method->get_code_addr();
    INFO2("compile.code", "Compiled method " << method
            << ", entry " << entry_point);

    if (method->get_pending_breakpoints() != 0)
        jvmti_set_pending_breakpoints(method);
    if(ti->isEnabled() && ti->is_single_step_enabled()
        && !method->is_native())
    {
        jvmti_thread_t jvmti_thread = jthread_self_jvmti();
        assert(jvmti_thread);
        jvmti_set_single_step_breakpoints_for_method(ti, jvmti_thread, method);
    }

    return entry_point;
} // compile_me

// Adding dynamic generated code info to global list
// Is used in JVMTI and native frames interface
DynamicCode* compile_get_dynamic_code_list(void)
{
    return VM_Global_State::loader_env->dcList;
}

// Adding dynamic generated code info to global list
void compile_add_dynamic_generated_code_chunk(const char* name, bool free_name,
    const void* address, size_t length)
{
    DynamicCode *dc = (DynamicCode *)STD_MALLOC(sizeof(DynamicCode));
    assert(dc);
    dc->name = name;
    dc->free_name = free_name;
    dc->address = address;
    dc->length = length;

    // Synchronizing access to dynamic code list
    LMAutoUnlock dcll(VM_Global_State::loader_env->p_dclist_lock);

    DynamicCode** pdcList = &VM_Global_State::loader_env->dcList;
    dc->next = *pdcList;
    *pdcList = dc;
}

void compile_clear_dynamic_code_list(DynamicCode* list)
{
    while (list)
    {
        DynamicCode* next = list->next;
        if (list->free_name)
            STD_FREE((void *)list->name);
        STD_FREE(list);
        list = next;
    }
}

VMEXPORT void vm_compiled_method_load(Method_Handle method, U_32 codeSize, 
                                  void* codeAddr, U_32 mapLength, 
                                  AddrLocation* addrLocationMap, 
                                  void* compileInfo, Method_Handle outer_method) 
{
    assert(method);
    assert(outer_method);

    outer_method->add_inline_info_entry(method, codeSize, codeAddr, mapLength,
            addrLocationMap);

    // Find TI environment
    DebugUtilsTI *ti = VM_Global_State::loader_env->TI;

    // Call TI callbacks
    if (jvmti_should_report_event(JVMTI_EVENT_COMPILED_METHOD_LOAD)
        && ti->getPhase() == JVMTI_PHASE_LIVE)
    {
        jvmti_send_region_compiled_method_load_event(method, codeSize,
            codeAddr, mapLength, addrLocationMap, NULL);
    }
}
  


