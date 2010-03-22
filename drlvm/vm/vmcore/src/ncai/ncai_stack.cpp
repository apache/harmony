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

#define LOG_DOMAIN "ncai.stack"
#include "cxxlog.h"

#include "suspend_checker.h"
#include "interpreter_exports.h"
#include "open/ncai_thread.h"

#include "native_stack.h"
#include "ncai_utils.h"
#include "ncai_direct.h"
#include "ncai_internal.h"


static ncaiError walk_native_stack(hythread_t thread,
    int* pcount, int max_depth, native_frame_t* frame_array);


ncaiError JNICALL
ncaiGetFrameCount(ncaiEnv *env, ncaiThread thread, jint *count_ptr)
{
    TRACE2("ncai.stack", "GetFrameCount called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (thread == NULL)
        return NCAI_ERROR_INVALID_THREAD;

    if (count_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    hythread_t hythread = reinterpret_cast<hythread_t>(thread);

    int count = 0;
    ncaiError err = walk_native_stack(hythread, &count, -1, NULL);

    if (err != NCAI_ERROR_NONE)
        return err;

    *count_ptr = count;
    return NCAI_ERROR_NONE;
}


ncaiError JNICALL
ncaiGetStackTrace(ncaiEnv *env, ncaiThread thread, jint depth,
    ncaiFrameInfo* frame_buffer, jint *count_ptr)
{
    TRACE2("ncai.stack", "GetStackTrace called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (thread == NULL)
        return NCAI_ERROR_INVALID_THREAD;

    if (count_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    if (depth < 0)
        return NCAI_ERROR_ILLEGAL_ARGUMENT;

    if (depth == 0)
    {
        *count_ptr = 0;
        return NCAI_ERROR_NONE;
    }

    if (frame_buffer == NULL)
        return NCAI_ERROR_NULL_POINTER;

    native_frame_t* frame_array =
        (native_frame_t*)ncai_alloc(sizeof(native_frame_t)*depth);

    if (!frame_array)
        return NCAI_ERROR_OUT_OF_MEMORY;

    hythread_t hythread = reinterpret_cast<hythread_t>(thread);

    int count = 0;
    ncaiError err = walk_native_stack(hythread, &count, depth, frame_array);
    assert(count >= 0);

    if (err != NCAI_ERROR_NONE)
    {
        ncai_free(frame_array);
        return err;
    }

    for (jint i = 0; i < count; i++)
    {
        frame_buffer[i].java_frame_depth = frame_array[i].java_depth;
        frame_buffer[i].pc_address = frame_array[i].ip;
        frame_buffer[i].frame_address = frame_array[i].frame;
        frame_buffer[i].stack_address = frame_array[i].stack;
        frame_buffer[i].return_address = NULL;

        if (i > 0)
            frame_buffer[i - 1].return_address = frame_array[i].ip;
    }

    ncai_free(frame_array);
    *count_ptr = count;
    return NCAI_ERROR_NONE;
}


static ncaiError walk_native_stack(hythread_t thread,
    int* pcount, int max_depth, native_frame_t* frame_array)
{
    if (!hythread_is_alive(thread))
                return NCAI_ERROR_THREAD_NOT_ALIVE;

    int suspend_count = hythread_get_suspend_count_native(thread);
    hythread_t hself = hythread_self();

    Registers regs;
    int count = 0;
    VM_thread* vm_thread;

    if (hself == thread) //Current thread is not suspended
    {
        assert(suspend_count <= 0);

        vm_thread = p_TLS_vmthread;

        if (vm_thread == NULL)
            return NCAI_ERROR_THREAD_NOT_ALIVE;

        if (!vm_thread->jvmti_thread.flag_ncai_handler)
            return NCAI_ERROR_THREAD_NOT_SUSPENDED;

        regs = *((Registers*)vm_thread->jvmti_thread.jvmti_saved_exception_registers);
    }
    else
    {
        if (suspend_count <= 0)
            return NCAI_ERROR_THREAD_NOT_SUSPENDED;

        // We know that the thread is suspended
        bool res = ncai_get_generic_registers(thread, &regs);

        if (!res)
            return NCAI_ERROR_INTERNAL;

        vm_thread = jthread_get_vm_thread(thread);
    }

    UnwindContext context;
    if (!port_init_unwind_context(&context, NULL, &regs))
        return NCAI_ERROR_INTERNAL;

    *pcount = walk_native_stack_registers(&context, &regs,
                    vm_thread, max_depth, frame_array);

    port_clean_unwind_context(&context);

    return NCAI_ERROR_NONE;
}
