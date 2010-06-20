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
 * @author Ilya Berezhniuk
 */

#include "lock_manager.h"
#include "m2n.h"
#include "compile.h"
#include "interpreter.h"
#include "interpreter_exports.h"
#include "environment.h"
#include "port_modules.h"
#include "port_unwind.h"
#include "native_stack.h"


//////////////////////////////////////////////////////////////////////////////
/// Helper functions

static DynamicCode* native_find_stub(void* ip)
{
    for (DynamicCode *dcList = compile_get_dynamic_code_list();
         NULL != dcList; dcList = dcList->next)
    {
        if (ip >= dcList->address &&
            ip < (void*)((POINTER_SIZE_INT)dcList->address + dcList->length))
            return dcList;
    }

    return NULL;
}

char* native_get_stub_name(void* ip, char* buf, size_t buflen)
{
    // Synchronizing access to dynamic code list
    LMAutoUnlock dcll(VM_Global_State::loader_env->p_dclist_lock);

    if (!buf || buflen == 0)
        return NULL;

    DynamicCode* code = native_find_stub(ip);

    if (!code || !code->name)
        return NULL;

    strncpy(buf, code->name, buflen);
    buf[buflen - 1] = '\0';

    return buf;
}

const char* native_get_stub_name_nocpy(void* ip)
{
    // Synchronizing access to dynamic code list
    LMAutoUnlock dcll(VM_Global_State::loader_env->p_dclist_lock);

    DynamicCode* code = native_find_stub(ip);

    if (!code)
        return NULL;

    return code->name;
}

bool native_is_ip_stub(void* ip)
{
    // Synchronizing access to dynamic code list
    LMAutoUnlock dcll(VM_Global_State::loader_env->p_dclist_lock);

    return (native_find_stub(ip) != NULL);
}

static void native_fill_frame_info(Registers* UNREF regs, native_frame_t* UNREF frame, jint UNREF jdepth)
{
    frame->java_depth = jdepth;

    if (!regs)
        return;

#if defined(_IPF_)
    // Nothing
#elif defined(_EM64T_)
    frame->ip = (void*)regs->rip;
    frame->frame = (void*)regs->rbp;
    frame->stack = (void*)regs->rsp;
#else // IA-32
    frame->ip = (void*)regs->eip;
    frame->frame = (void*)regs->ebp;
    frame->stack = (void*)regs->esp;
#endif
}

static void native_get_regs_from_jit_context(JitFrameContext* jfc, Registers* regs)
{
#if defined(_IPF_)
    // Nothing
#elif defined(_EM64T_)
    regs->rsp = jfc->rsp;
    regs->rip = *jfc->p_rip;
    regs->rbp = (jfc->p_rbp) ? *jfc->p_rbp : regs->rbp;
#else // IA-32
    regs->esp = jfc->esp;
    regs->eip = *jfc->p_eip;
    regs->ebp = (jfc->p_ebp) ? *jfc->p_ebp : regs->ebp;
#endif
}

/*
Now the technique for calling C handler from a signal/exception context
guarantees that all needed return addresses are present in stack, so
there is no need in special processing
static bool native_is_ip_in_breakpoint_handler(void* ip)
{
    return (ip >= &process_native_breakpoint_event &&
            ip < &jvmti_jit_breakpoint_handler);
}*/
/// Helper functions
//////////////////////////////////////////////////////////////////////////////


//////////////////////////////////////////////////////////////////////////////
//


static int walk_native_stack_jit(
    UnwindContext* context,
    Registers* pregs, VM_thread* pthread,
    int max_depth, native_frame_t* frame_array);
static int walk_native_stack_pure(
    UnwindContext* context, Registers* pregs,
    int max_depth, native_frame_t* frame_array);
static int walk_native_stack_interpreter(
    UnwindContext* context,
    Registers* pregs, VM_thread* pthread,
    int max_depth, native_frame_t* frame_array);

int walk_native_stack_registers(UnwindContext* context, Registers* pregs,
    VM_thread* pthread, int max_depth, native_frame_t* frame_array)
{
    if (pthread == NULL) // Pure native thread
        return walk_native_stack_pure(context, pregs, max_depth, frame_array);

    if (interpreter_enabled())
        return walk_native_stack_interpreter(context, pregs,
                                            pthread, max_depth, frame_array);

    return walk_native_stack_jit(context, pregs, pthread, max_depth, frame_array);
    return 0;
}


static int walk_native_stack_jit(
    UnwindContext* context,
    Registers* pregs, VM_thread* pthread,
    int max_depth, native_frame_t* frame_array)
{
    // Register context for current frame
    Registers regs = *pregs;

    int frame_count = 0;
    
    // Search for method containing corresponding address
    VM_Code_Type code_type = vm_identify_eip(regs.get_ip());
    bool flag_dummy_frame = false;
    jint java_depth = 0;
    StackIterator* si = NULL;
    bool is_java = false;

    if (code_type == VM_TYPE_JAVA)
    { // We must add dummy M2N frame to start SI iteration
        if (!pthread)
            return 0;

        is_java = true;

        M2nFrame* lm2n = m2n_get_last_frame(pthread);

        if (!m2n_is_suspended_frame(lm2n) || m2n_get_ip(lm2n) != regs.get_ip())
        { // We should not push frame if it was pushed by breakpoint handler
            m2n_push_suspended_frame(pthread, &regs);
            flag_dummy_frame = true;
        }
    }

    si = si_create_from_native(pthread);

    if (is_java ||
        // Frame was pushed already by breakpoint handler
        (si_is_native(si) && si_get_m2n(si) && m2n_is_suspended_frame(si_get_m2n(si))))
    {
        si_goto_previous(si);
    }

    jint inline_index = -1;
    jint inline_count;
    CodeChunkInfo* cci = NULL;
    bool is_stub = false;
    int special_count = 0;
    bool flag_breakpoint = false;

    while (1)
    {
        if (frame_array != NULL && frame_count >= max_depth)
            break;

        if (frame_array)
        { // If frames requested, store current frame
            native_fill_frame_info(&regs, &frame_array[frame_count],
                is_java ? java_depth : -1);
        }

        ++frame_count;

/////////////////////////
// vv Java vv
        if (is_java) //code_type == VM_TYPE_JAVA
        { // Go to previous frame using StackIterator
            cci = si_get_code_chunk_info(si);
            if (!cci) // Java method should contain cci
                break;

            // if (inline_index < 0) we have new si
            // (inline_index < inline_count) we must process inline
            // (inline_index == inline_count) we must process si itself
            if (inline_index < 0)
            {
                inline_count = si_get_inline_depth(si);
                inline_index = 0;
            }

            ++java_depth;

            if (inline_index < inline_count)
            { // Process inlined method
                // We don't need to update context,
                // because context is equal for
                // all inlined methods
                ++inline_index;
            }
            else
            {
                inline_index = -1;
                // Go to previous stack frame from StackIterator
                si_goto_previous(si);
                native_get_regs_from_jit_context(si_get_jit_context(si), &regs);
            }

            code_type = vm_identify_eip(regs.get_ip());
            is_java = (code_type == VM_TYPE_JAVA);
            continue;
        }
// ^^ Java ^^
/////////////////////////
// vv Native vv
        is_stub = native_is_ip_stub(regs.get_ip());

        if (is_stub)
        { // Native stub, previous frame is Java frame
            if (!si_is_native(si))
                break;

            if (si_get_method(si)) // Frame represents JNI frame
            {
                // Mark first frame (JNI stub) as Java frame
                if (frame_array && frame_count == 1)
                    frame_array[frame_count - 1].java_depth = java_depth;

                if (frame_array && frame_count > 1)
                    frame_array[frame_count - 2].java_depth = java_depth;

                ++java_depth;
            }

            // Ge to previous stack frame from StackIterator
            si_goto_previous(si);
            // Let's get context from si
            native_get_regs_from_jit_context(si_get_jit_context(si), &regs);
        }
        else
        {
            Registers tmp_regs = regs;

            if (!port_unwind_frame(context, &tmp_regs))
                    break;
/*
            VMBreakPoints* vm_breaks = VM_Global_State::loader_env->TI->vm_brpt;
            vm_breaks->lock();

Now the technique for calling C handler from a signal/exception context
guarantees that all needed return addresses are present in stack, so
there is no need in special processing
            if (native_is_ip_in_breakpoint_handler(tmp_regs.get_ip()))
            {
                regs = *pthread->jvmti_thread.jvmti_saved_exception_registers;
                flag_breakpoint = true;
            }
            else*/
                regs = tmp_regs;

//            vm_breaks->unlock();
        }
            
        code_type = vm_identify_eip(regs.get_ip());
        is_java = (code_type == VM_TYPE_JAVA);

        // If we've reached Java without native stub (or breakpoint handler frame)
        if (is_java && !is_stub && !flag_breakpoint)
            break; // then stop processing
        flag_breakpoint = false;
// ^^ Native ^^
/////////////////////////
    }

    if (flag_dummy_frame)
    { // Delete previously added dummy frame
        M2nFrame* plm2n = m2n_get_last_frame(pthread);
        m2n_set_last_frame(pthread, m2n_get_previous_frame(plm2n));
        STD_FREE(plm2n);
    }

    si_free(si);

    return frame_count;
}


static int walk_native_stack_pure(
    UnwindContext* context, Registers* pregs,
    int max_depth, native_frame_t* frame_array)
{
    // Register context for current frame
    Registers regs = *pregs;
    if (vm_identify_eip(regs.get_ip()) == VM_TYPE_JAVA)
        return 0;

    int frame_count = 0;

    while (1)
    {
        if (frame_array != NULL && frame_count >= max_depth)
            break;

        if (frame_array)
        { // If frames requested, store current frame
            native_fill_frame_info(&regs, &frame_array[frame_count], -1);
        }

        ++frame_count;

        if (port_unwind_frame(context, &regs))
                break;
    }

    return frame_count;
}


static int walk_native_stack_interpreter(
    UnwindContext* context,
    Registers* pregs, VM_thread* pthread,
    int max_depth, native_frame_t* frame_array)
{
    // Register context for current frame
    Registers regs = *pregs;

    assert(pthread);
    FrameHandle* last_frame = interpreter.interpreter_get_last_frame(pthread);
    FrameHandle* frame = last_frame;

    int frame_count = 0;
    jint java_depth = 0;

    while (1)
    {
        if (frame_array != NULL && frame_count >= max_depth)
            break;

        if (frame_array)
        { // If frames requested, store current frame
            native_fill_frame_info(&regs, &frame_array[frame_count], -1);
        }

        ++frame_count;

        // Store previous value to identify frame range later
        void* prev_sp = regs.get_sp();
        Registers tmp_regs = regs;

        if (port_unwind_frame(context, &tmp_regs))
                break;
/*
        VMBreakPoints* vm_breaks = VM_Global_State::loader_env->TI->vm_brpt;
        vm_breaks->lock();

Now the technique for calling C handler from a signal/exception context
guarantees that all needed return addresses are present in stack, so
there is no need in special processing
        if (native_is_ip_in_breakpoint_handler(tmp_regs.get_ip()))
            regs = *pthread->jvmti_thread.jvmti_saved_exception_registers;
        else*/
            regs = tmp_regs;

//        vm_breaks->unlock();

        bool is_java = interpreter.is_frame_in_native_frame(frame, prev_sp, regs.get_sp());

        if (is_java)
        {
            // Set Java frame number
            if (frame_array && frame_count > 1)
                frame_array[frame_count - 2].java_depth = java_depth++;

            // Go to previous frame
            frame = interpreter.interpreter_get_prev_frame(frame);
        }
    }

    return frame_count;
}
