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

#define LOG_DOMAIN "ncai.thread"
#include "cxxlog.h"

#include "open/ncai_thread.h"

#include "suspend_checker.h"
#include "ncai_utils.h"
#include "jthread.h"
#include "Class.h"
#include "jni_utils.h"
#include "jvmti_internal.h"
#include "ncai_internal.h"
#include "ncai_direct.h"


static bool ncai_thread_is_alive(hythread_t hythread);


//////////////////////////////////////////////////////////////////////////////
//  Interface functions
//////////////////////////////////////////////////////////////////////////////

ncaiError JNICALL
ncaiGetAllThreads(ncaiEnv *env, jint *count_ptr, ncaiThread **threads_ptr)
{
    TRACE2("ncai.thread", "GetAllThreads called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (threads_ptr == NULL || count_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    hythread_group_t* groups;
    int group_count;
    IDATA status;
    int i, res_count = 0;

    status = hythread_group_get_list(&groups, &group_count);

    if (status != TM_ERROR_NONE)
        return NCAI_ERROR_INTERNAL;

    for (i = 0; i < group_count; i++)
    {
        hythread_t cur_thread;
        hythread_iterator_t iterator = hythread_iterator_create(groups[i]);

        while (iterator && (cur_thread = hythread_iterator_next(&iterator)))
        {
            if (ncai_thread_is_alive(cur_thread))
                res_count++;
        }

        hythread_iterator_release(&iterator);
    }

    ncaiThread* res_threads =
        (ncaiThread*)ncai_alloc(sizeof(void*)*res_count);

    if (res_threads == NULL)
        return NCAI_ERROR_OUT_OF_MEMORY;

    int index = 0;

    for (i = 0; i < group_count; i++)
    {
        hythread_t cur_thread;
        hythread_iterator_t iterator = hythread_iterator_create(groups[i]);

        while (iterator && (cur_thread = hythread_iterator_next(&iterator)))
        {
            if (index >= res_count)
                break; // some threads were created between two cycles

            if (!ncai_thread_is_alive(cur_thread))
                continue;

            res_threads[index] = (ncaiThread)cur_thread;
            ++index;
        }

        hythread_iterator_release(&iterator);
    }

    *threads_ptr = res_threads;
    *count_ptr = index;
    return NCAI_ERROR_NONE;
}

ncaiError JNICALL
ncaiGetThreadInfo(ncaiEnv *env, ncaiThread thread, ncaiThreadInfo *info_ptr)
{
    TRACE2("ncai.thread", "ncaiGetThreadsInfo called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (info_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    if (thread == NULL)
        return NCAI_ERROR_INVALID_THREAD;

    hythread_t hythread = reinterpret_cast<hythread_t>(thread);

    if (!ncai_thread_is_alive(hythread))
        return NCAI_ERROR_THREAD_NOT_ALIVE;

    jthread java_thread = jthread_get_java_thread(hythread);

    if (java_thread != NULL)
    {
        JNIEnv* jni_env = jthread_get_vm_thread(hythread)->jni_env;
        jclass cl = GetObjectClass(jni_env, java_thread);
        jmethodID id = jni_env->GetMethodID(cl, "getName","()Ljava/lang/String;");
        jstring  name = jni_env->CallObjectMethod(java_thread, id);
        info_ptr->name = (char *)jni_env->GetStringUTFChars(name, NULL);

        info_ptr->kind = NCAI_THREAD_JAVA;
        return NCAI_ERROR_NONE;
    }

    info_ptr->kind = NCAI_THREAD_VM_INTERNAL;

    const char* name_int = "native_0x";
    size_t name_len = strlen(name_int) + 4 + 1;
    info_ptr->name = (char*)ncai_alloc(name_len);
    assert(info_ptr->name);
    sprintf(info_ptr->name, "%s%04X", name_int, hythread_get_id(hythread));

    return NCAI_ERROR_NONE;
}

ncaiError JNICALL
ncaiGetThreadHandle(ncaiEnv *env, jthread thread, ncaiThread *thread_ptr)
{
    TRACE2("ncai.thread", "GetThreadHandle called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (thread_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    if (thread == NULL)
        return NCAI_ERROR_INVALID_THREAD;

    hythread_t hythread = jthread_get_native_thread(thread);

    if (hythread == NULL)
        return NCAI_ERROR_INVALID_THREAD;

    *thread_ptr = reinterpret_cast<ncaiThread>(hythread);

    return NCAI_ERROR_NONE;
}

ncaiError JNICALL
ncaiGetThreadObject(ncaiEnv *env, ncaiThread thread, jthread *thread_ptr)
{
    TRACE2("ncai.thread", "GetThreadObject called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (thread_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    if (thread == NULL)
        return NCAI_ERROR_INVALID_THREAD;

    hythread_t hythread = reinterpret_cast<hythread_t>(thread);

    if (!ncai_thread_is_alive(hythread))
        return NCAI_ERROR_THREAD_NOT_ALIVE;

    jthread java_thread = jthread_get_java_thread(hythread);

    *thread_ptr = java_thread;

    return NCAI_ERROR_NONE;
}

ncaiError JNICALL
ncaiSuspendThread(ncaiEnv *env, ncaiThread thread)
{
    TRACE2("ncai.thread", "SuspendThread called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (thread == NULL)
        return NCAI_ERROR_INVALID_THREAD;

    hythread_t hythread = reinterpret_cast<hythread_t>(thread);

    if (!ncai_thread_is_alive(hythread))
        return NCAI_ERROR_THREAD_NOT_ALIVE;

    IDATA status = hythread_suspend_thread_native(hythread);

    if (status != TM_ERROR_NONE)
        return NCAI_ERROR_INTERNAL;

    return NCAI_ERROR_NONE;
}

ncaiError JNICALL
ncaiResumeThread(ncaiEnv *env, ncaiThread thread)
{
    TRACE2("ncai.thread", "ResumeThread called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (thread == NULL)
        return NCAI_ERROR_INVALID_THREAD;

    hythread_t hythread = reinterpret_cast<hythread_t>(thread);

    if (!ncai_thread_is_alive(hythread))
        return NCAI_ERROR_THREAD_NOT_ALIVE;

    IDATA status = hythread_resume_thread_native(hythread);

    if (status != TM_ERROR_NONE)
        return NCAI_ERROR_INTERNAL;

    return NCAI_ERROR_NONE;
}

ncaiError JNICALL
ncaiTerminateThread(ncaiEnv *env, ncaiThread thread)
{
    TRACE2("ncai.thread", "TerminateThread called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (thread == NULL)
        return NCAI_ERROR_INVALID_THREAD;

    hythread_t hythread = reinterpret_cast<hythread_t>(thread);

    hythread_t self = hythread_self();

    if (hythread == self)
        return NCAI_ERROR_INVALID_THREAD;

    assert(thread);

    // grab hythread global lock
    hythread_global_lock();

    if (!ncai_thread_is_alive(hythread))
    {
        hythread_global_unlock();
        return NCAI_ERROR_THREAD_NOT_ALIVE;
    }

    IDATA UNUSED status = jthread_vm_detach(jthread_get_vm_thread(hythread));
    assert(status == TM_ERROR_NONE);

    hythread_cancel(hythread);

    // release hythread global lock
    hythread_global_unlock();

    return NCAI_ERROR_NONE;
}


//////////////////////////////////////////////////////////////////////////////
// Helper functions
//////////////////////////////////////////////////////////////////////////////

static bool ncai_thread_is_alive(hythread_t hythread)
{
    if (!hythread)
        return false;

    vm_thread_t vm_thread = jthread_get_vm_thread(hythread);

    if (!vm_thread || !vm_thread->java_thread)
        return false;

    return hythread_is_alive(hythread);
}
