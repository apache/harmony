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

//#include <ctype.h>

#include "open/vm_properties.h"
#include "port_dso.h"
#include "port_modules.h"
#include "port_crash_handler.h"
#include "port_frame_info.h"
#include "m2n.h"
#include "stack_iterator.h"
#include "environment.h"
#include "vm_threads.h"
#include "exceptions.h"
#include "natives_support.h"
#include "stack_trace.h"
#include "signals.h"
#include "interpreter.h"
#include "compile.h"
#include "cci.h"
#include "jit_intf_cpp.h"
#include "native_stack.h"
#include "crash_dump.h"


#ifdef PLATFORM_POSIX
#include <strings.h>
#define strcmp_case strcasecmp
#else /* Windows */
#include <string.h>
#define strcmp_case _stricmp
#endif


static void cd_init_crash_sequence();
static void cd_cleanup_crash_sequence();
static void cd_fill_java_method_info(Method* m, void* ip, bool is_ip_past,
                                     int inl_depth, port_stack_frame_info* sfi);
static void cd_print_module_info(Registers* regs);
static void cd_print_threads_info();


struct st_int_uwinfo
{
    bool filled;
    FrameHandle* frame;
    void* prev_sp;
};

struct st_jit_uwinfo
{
    bool filled;
    CodeChunkInfo* cci;
    int inline_index;
    M2nFrame* lm2n;
    bool is_first; // For setting is_ip_past
};

static U_32 cci_get_inlined_depth(CodeChunkInfo* cci, U_32 offset)
{
    ASSERT_NO_INTERPRETER

    if (!cci || !cci->has_inline_info())
        return 0;

    return cci->get_jit()->get_inline_depth(cci->get_inline_info(), offset);
}

static Method* get_jit_method(CodeChunkInfo* cci, void* ip, U_32 index, bool is_first)
{
    Method* method = cci->get_method();

    if (index == 0)
        return method;

    U_32 offset = (U_32)((POINTER_SIZE_INT)ip -
        (POINTER_SIZE_INT)cci->get_code_block_addr());
    U_32 inlined_depth = cci_get_inlined_depth(cci, offset);
    bool is_ip_past = !is_first; // || (index != inlined_depth);

    if (index > inlined_depth)
        return method; // Error

    Method* inl_method =
        cci->get_jit()->get_inlined_method(cci->get_inline_info(), offset, index);

    return inl_method;
}


static void cd_unwind_from_java(vm_thread_t vmthread,
                CodeChunkInfo* cci, Registers* regs, bool is_ip_past, M2nFrame* m2n)
{
    StackIterator* si = (StackIterator*)STD_ALLOCA(si_size());

    M2nFrame* last_m2n = m2n ? m2n : m2n_get_last_frame(vmthread);
    si_fill_from_registers(si, regs, is_ip_past, last_m2n);

    while (!si_is_past_end(si))
    {
        CodeChunkInfo* curcci = si_get_code_chunk_info(si);

        if (curcci && curcci == cci &&
            si_get_ip(si) == regs->get_ip())
            break; // Method is found in stack iterator

        si_goto_previous(si);
    }

    if (si_is_past_end(si)) // Error: method not found
        return;

    si_goto_previous(si);
    si_copy_to_registers(si, regs);
}

static void cd_unwind_from_stub(M2nFrame* lm2n, Registers* regs)
{
    StackIterator* si = (StackIterator*)STD_ALLOCA(si_size());
    si_fill_from_registers(si, regs, true, lm2n);
    si_goto_previous(si);
    si_copy_to_registers(si, regs);
}


static int unwind_compiled_frame(Registers *regs, port_stack_frame_info *sfi)
{
    // Suppose this callback is called by PORT for crash reasons only
    cd_init_crash_sequence();

    if (!sfi->iteration_state)
        return (int)(interpreter_enabled() ? sizeof(st_int_uwinfo)
                                           : sizeof(st_jit_uwinfo));

    void* cur_ip = regs->get_ip();
    void* cur_sp = regs->get_sp();

    // For interpreter - only return additional info
    if (interpreter_enabled())
    {
        vm_thread_t vmthread = get_thread_ptr();
        st_int_uwinfo* uwinfo = (st_int_uwinfo*)sfi->iteration_state;

        if (!vmthread)
            return -1;

        if (!uwinfo->filled)
        {
            uwinfo->frame = interpreter.interpreter_get_last_frame(vmthread);
            uwinfo->prev_sp = cur_sp;
            uwinfo->filled = true;
        }

        bool is_java = interpreter.is_frame_in_native_frame(uwinfo->frame, uwinfo->prev_sp, cur_sp);
        if (is_java)
        {
            Method* method = (Method*)interpreter.interpreter_get_frame_method(uwinfo->frame);
            U_8* bc_ptr = interpreter.interpreter_get_frame_bytecode_ptr(uwinfo->frame);
            cd_fill_java_method_info(method, (void*)bc_ptr, false, -1, sfi);
            uwinfo->frame = interpreter.interpreter_get_prev_frame(uwinfo->frame);
        }

        uwinfo->prev_sp = cur_sp;
        return -1;
    }

    // JIT-frames
    vm_thread_t vmthread = get_thread_ptr();
    st_jit_uwinfo* uwinfo = (st_jit_uwinfo*)sfi->iteration_state;

    if (!vmthread)
        return -1;

    if (!uwinfo->filled)
    {
        uwinfo->cci = NULL;
        uwinfo->inline_index = -1;
        uwinfo->is_first = true;
        uwinfo->filled = true;
        uwinfo->lm2n = NULL;
    }

    Global_Env* env = VM_Global_State::loader_env;
    bool ip_past = !uwinfo->is_first;
    uwinfo->is_first = false; // For the next iterations

    // Stubs - return stub name as additional info
    if (native_is_ip_stub(cur_ip))
    {
        sfi->method_class_name = "stub";
        sfi->method_name = native_get_stub_name_nocpy(cur_ip);

        if (uwinfo->lm2n == NULL) // Initialize
            uwinfo->lm2n = m2n_get_last_frame(vmthread);

        cd_unwind_from_stub(uwinfo->lm2n, regs);
        uwinfo->lm2n = m2n_get_previous_frame(uwinfo->lm2n);

        return 0; // Regs now contain a context for previous Java frame
    }

    CodeChunkInfo* cci = NULL;
    Method_Handle mh = env->em_interface->LookupCodeChunk(cur_ip,
                                        ip_past, NULL, NULL, (void**)&cci);

    if (!mh)
    {
        //assert(uwinfo->inline_index == 0);// we should not miss first JITted frame
        uwinfo->cci = NULL;
        return -1;
    }

    if (cci == uwinfo->cci) // Continue reporting inlined methods
    {
        if (uwinfo->inline_index <= 0)
            return -1; // Error: should be unwound earlier

        --uwinfo->inline_index;
        Method* m = get_jit_method(cci, cur_ip, uwinfo->inline_index, ip_past);
        cd_fill_java_method_info(m, cur_ip, ip_past, uwinfo->inline_index, sfi);

        if (uwinfo->inline_index > 0) // Simply return info from inlined method
            return 0;

        // Need to unwind JITted Java frame and update registers
        cd_unwind_from_java(vmthread, cci, regs, ip_past, uwinfo->lm2n);
        return 0;
    }

    // New cci
    U_32 offset = (U_32)((POINTER_SIZE_INT)cur_ip -
                        (POINTER_SIZE_INT)cci->get_code_block_addr());
    U_32 inlined_depth = cci_get_inlined_depth(cci, offset);

    uwinfo->cci = cci;
    uwinfo->inline_index = (int)inlined_depth;

    Method* m = get_jit_method(cci, cur_ip, inlined_depth, ip_past);
    cd_fill_java_method_info(m, cur_ip, ip_past, inlined_depth, sfi);

    if (inlined_depth == 0) // Unwind right now if not inlined
        cd_unwind_from_java(vmthread, cci, regs, ip_past, uwinfo->lm2n);

    return 0;
}


static void crash_action(port_sigtype UNREF signum, Registers* regs, void* UNREF fault_addr)
{
    if (!regs) // No regs info - do not print anything
        return;

    // For the case when unwind callback was not called before
    cd_init_crash_sequence();

    // Print crashed modile info
    cd_print_module_info(regs);
    // Print threads info
    cd_print_threads_info();

        fflush(stderr);
    cd_cleanup_crash_sequence();
}


static port_signal_handler_registration registrations[] =
{
    {PORT_SIGNAL_GPF,             null_reference_handler},
    {PORT_SIGNAL_STACK_OVERFLOW,  stack_overflow_handler},
    {PORT_SIGNAL_ABORT,           abort_handler},
    {PORT_SIGNAL_QUIT,            ctrl_backslash_handler},
    {PORT_SIGNAL_CTRL_BREAK,      ctrl_break_handler},
    {PORT_SIGNAL_CTRL_C,          ctrl_c_handler},
    {PORT_SIGNAL_BREAKPOINT,      native_breakpoint_handler},
    {PORT_SIGNAL_ARITHMETIC,      arithmetic_handler}
};

int vm_initialize_signals()
{
    Boolean result = port_init_crash_handler(
                        registrations,
                        sizeof(registrations)/sizeof(registrations[0]),
                        unwind_compiled_frame);

    if (!result)
        return -1;

    result = port_crash_handler_add_action(crash_action);

    if (!result)
        return -1;

    unsigned flags = port_crash_handler_get_capabilities();

#ifdef PLATFORM_POSIX
    bool call_dbg = (VM_Global_State::loader_env == NULL) ||
            vm_property_get_boolean("vm.crash_handler", FALSE, VM_PROPERTIES);
#else // WIN
    bool call_dbg = (VM_Global_State::loader_env == NULL) ||
            vm_property_get_boolean("vm.assert_dialog", TRUE, VM_PROPERTIES);
#endif
    if (!call_dbg)
        flags &= ~PORT_CRASH_CALL_DEBUGGER;

    port_crash_handler_set_flags(flags);

    return 0;
}


int vm_shutdown_signals()
{
    Boolean result = port_shutdown_crash_handler();
    if (!result)
        return -1;

    return 0;
}



// Crash sequence is single-threaded, it's provided by the PORT
static bool g_crash_initialized = false;
static int g_disable_count;
static bool g_unwindable;
static native_module_t* g_modules = NULL;

static void cd_init_crash_sequence()
{
    if (g_crash_initialized)
        return;

    VM_thread* thread = get_thread_ptr(); // Can be NULL for pure native thread

    if (thread)
    {
        // Enable suspend to allow working with threads
        g_disable_count = hythread_reset_suspend_disable();
        // Acquire global lock to print threads list
//        hythread_global_lock();
        g_unwindable = set_unwindable(false); // To call Java code
    }

    g_crash_initialized = true;
}

static void cd_cleanup_crash_sequence()
{
    if (!g_crash_initialized)
        return;

    VM_thread* thread = get_thread_ptr(); // Can be NULL for pure native thread

    if (thread)
    {
        set_unwindable(g_unwindable);
//        hythread_global_unlock();
        hythread_set_suspend_disable(g_disable_count);
    }

    g_crash_initialized = false;
}


static void cd_fill_java_method_info(Method* m, void* ip, bool is_ip_past,
                                     int inl_depth, port_stack_frame_info* sfi)
{
    if (!m || !sfi)
        return;

    sfi->method_class_name = m->get_class()->get_name()->bytes;
    sfi->method_name = m->get_name()->bytes;
    sfi->method_signature = m->get_descriptor()->bytes;
    sfi->source_file_name = NULL;
    sfi->source_line_number = -1;

    if (!ip)
        return;

    const char* fname = NULL;
    int line = -1;

    if (inl_depth == 0)
        inl_depth = -1; // should pass -1 for non-inlined methods

    get_file_and_line(m, ip, is_ip_past, inl_depth, &fname, &line);

    if (fname)
    {
        sfi->source_file_name = fname;
        sfi->source_line_number = line;
    }
}


const char* cd_get_module_type(const char* short_name)
{
    char name[256];

    if (strlen(short_name) > 255)
        return "Too long short name";

    strcpy(name, short_name);
    char* dot = strchr(name, '.');

    // Strip suffix/extension
    if (dot)
        *dot = 0;

    // Strip prefix
    char* nameptr = name;

    if (!memcmp(short_name, PORT_DSO_PREFIX, strlen(PORT_DSO_PREFIX)))
        nameptr += strlen(PORT_DSO_PREFIX);

    const char* vm_modules[] = {"java", "em", "encoder", "gc_gen", "gc_gen_uncomp", "gc_cc",
        "harmonyvm", "hythr", "interpreter", "jitrino", "vmi"};

    for (size_t i = 0; i < sizeof(vm_modules)/sizeof(vm_modules[0]); i++)
    {
        if (!strcmp_case(name, vm_modules[i]))
            return "VM native code";
    }

    if (natives_is_library_loaded_slow(short_name))
        return "JNI native library";

    return "Unknown/system native module";
}

static void cd_fill_modules()
{
    if (g_modules)
        return;

    int count;
    bool res = port_get_all_modules(&g_modules, &count);
    assert(res && g_modules && count);
}

static void cd_print_module_info(Registers* regs)
{
    cd_fill_modules();

    native_module_t* module = port_find_module(g_modules, (void*)regs->get_ip());
    cd_parse_module_info(module, regs->get_ip());
}


static void cd_print_threads_info()
{
    VM_thread* cur_thread = get_thread_ptr();

    if (!cur_thread)
        fprintf(stderr, "\nCurrent thread is not attached to VM, ID: %d\n", port_gettid());

    fprintf(stderr, "\nVM attached threads:\n\n");

    hythread_iterator_t it = hythread_iterator_create(NULL);
    int count = (int)hythread_iterator_size(it);

    for (int i = 0; i < count; i++)
    {
        hythread_t thread = hythread_iterator_next(&it);
        VM_thread* vm_thread = jthread_get_vm_thread(thread);

        if (!vm_thread)
            continue;

        jthread java_thread = jthread_get_java_thread(thread);
        JNIEnv* jni_env = vm_thread->jni_env;

        if (cur_thread && java_thread)
        {
            jclass cl = GetObjectClass(jni_env, java_thread);
            jmethodID id = jni_env->GetMethodID(cl, "getName","()Ljava/lang/String;");
            jstring name = jni_env->CallObjectMethod(java_thread, id);
            char* java_name = (char*)jni_env->GetStringUTFChars(name, NULL);

            fprintf(stderr, "%s[%p]  '%s'\n",
                    (cur_thread && vm_thread == cur_thread) ? "--->" : "    ",
                    thread->os_handle, java_name);

            jni_env->ReleaseStringUTFChars(name, java_name);
        }
        else
        {
            fprintf(stderr, "%s[%p]\n",
                    (cur_thread && vm_thread == cur_thread) ? "--->" : "    ",
                    thread->os_handle);
        }
    }

    hythread_iterator_release(&it);
}
