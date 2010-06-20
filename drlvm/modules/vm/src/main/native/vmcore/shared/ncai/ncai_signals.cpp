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

#define LOG_DOMAIN "ncai.signal"
#include "cxxlog.h"
#include "suspend_checker.h"
#include "jvmti_internal.h"
#include "environment.h"
#include "port_crash_handler.h"

#include "ncai_utils.h"
#include "ncai_direct.h"
#include "ncai_internal.h"


struct st_signal_info
{
    jint        signal;
    const char* name;
    size_t      name_size;
};

static size_t ncai_get_signal_count();
static st_signal_info* find_signal(jint sig);
static jint ncai_get_min_signal();
static jint ncai_get_max_signal();
static const char* ncai_get_signal_name(jint signal);
static size_t ncai_get_signal_name_size(jint signal);
bool ncai_is_signal_in_range(jint signal);


ncaiError JNICALL
ncaiGetSignalCount(ncaiEnv *env, jint* count_ptr)
{
    TRACE2("ncai.signal", "GetSignalCount called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (count_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    *count_ptr = (jint)ncai_get_signal_count();
    return NCAI_ERROR_NONE;
}

ncaiError JNICALL
ncaiGetSignalInfo(ncaiEnv *env, jint signal, ncaiSignalInfo* info_ptr)
{
    TRACE2("ncai.signal", "GetSignalInfo called");
    SuspendEnabledChecker sec;

    if (env == NULL)
        return NCAI_ERROR_INVALID_ENVIRONMENT;

    if (info_ptr == NULL)
        return NCAI_ERROR_NULL_POINTER;

    if (!ncai_is_signal_in_range(signal))
        return NCAI_ERROR_ILLEGAL_ARGUMENT;

    const char* name = ncai_get_signal_name(signal);
    if (name == NULL)
        return NCAI_ERROR_ILLEGAL_ARGUMENT;

    size_t name_size = ncai_get_signal_name_size(signal);
    char* out_name = (char*)ncai_alloc(name_size);

    if (out_name == NULL)
        return NCAI_ERROR_OUT_OF_MEMORY;

    memcpy(out_name, name, name_size);
    info_ptr->name = out_name;
    return NCAI_ERROR_NONE;
}

void ncai_process_signal_event(NativeCodePtr addr,
        jint code, bool is_internal, bool* p_handled)
{
    if (!GlobalNCAI::isEnabled())
        return;

    DebugUtilsTI* ti = VM_Global_State::loader_env->TI;

    hythread_t hythread = hythread_self();
    ncaiThread thread = reinterpret_cast<ncaiThread>(hythread);
    bool skip_handler = false;

    TIEnv* next_env;
    for (TIEnv* env = ti->getEnvironments(); env; env = next_env)
    {
        next_env = env->next;
        NCAIEnv* ncai_env = env->ncai_env;

        if (NULL == ncai_env)
            continue;

        ncaiSignal func =
            (ncaiSignal)ncai_env->get_event_callback(NCAI_EVENT_SIGNAL);

        if (NULL != func)
        {
            if (ncai_env->global_events[NCAI_EVENT_SIGNAL - NCAI_MIN_EVENT_TYPE_VAL])
            {
                TRACE2("ncai.signal", "Calling global Signal callback, address = "
                    << addr << ", code = " << (void*)(size_t)code);

                jboolean is_h = *p_handled;
                func((ncaiEnv*)ncai_env, thread, (void*)addr, code, is_internal, &is_h);

                if (!(*p_handled) && is_h)
                    skip_handler = true;

                TRACE2("ncai.signal", "Finished global Signal callback, address = "
                    << addr << ", code = " << (void*)(size_t)code);

                continue;
            }

            ncaiEventThread* next_et;
            ncaiEventThread* first_et =
                ncai_env->event_threads[NCAI_EVENT_SIGNAL - NCAI_MIN_EVENT_TYPE_VAL];

            for (ncaiEventThread* et = first_et; NULL != et; et = next_et)
            {
                next_et = et->next;

                if (et->thread == thread)
                {
                    TRACE2("ncai.signal", "Calling local Signal callback, address = "
                        << addr << ", code = " << (void*)(size_t)code);

                    jboolean is_h = *p_handled;
                    func((ncaiEnv*)ncai_env, thread, (void*)addr, code, is_internal, &is_h);

                    if (!(*p_handled) && is_h)
                        skip_handler = true;

                    TRACE2("ncai.signal", "Finished local Signal callback, address = "
                        << addr << ", code = " << (void*)(size_t)code);
                }

                et = next_et;
            }
        }
    }
}



#define STR_AND_SIZE(_x_) _x_, (strlen(_x_) + 1)

static st_signal_info sig_table[] = {
    {PORT_SIGNAL_GPF,           STR_AND_SIZE("PORT_SIGNAL_GPF")},
    {PORT_SIGNAL_STACK_OVERFLOW,STR_AND_SIZE("PORT_SIGNAL_STACK_OVERFLOW")},
    {PORT_SIGNAL_ABORT,         STR_AND_SIZE("PORT_SIGNAL_ABORT")},
    {PORT_SIGNAL_QUIT,          STR_AND_SIZE("PORT_SIGNAL_QUIT")},
    {PORT_SIGNAL_CTRL_BREAK,    STR_AND_SIZE("PORT_SIGNAL_CTRL_BREAK")},
    {PORT_SIGNAL_CTRL_C,        STR_AND_SIZE("PORT_SIGNAL_CTRL_C")},
    {PORT_SIGNAL_BREAKPOINT,    STR_AND_SIZE("PORT_SIGNAL_BREAKPOINT")},
    {PORT_SIGNAL_ARITHMETIC,    STR_AND_SIZE("PORT_SIGNAL_ARITHMETIC")},
};

static size_t ncai_get_signal_count()
{
    return sizeof(sig_table)/sizeof(sig_table[0]);
}

static st_signal_info* find_signal(jint sig)
{
    for (size_t i = 0; i < ncai_get_signal_count(); i++)
    {
        if (sig_table[i].signal == sig)
            return &sig_table[i];
    }

    return NULL;
}

static jint ncai_get_min_signal()
{
    static int min_sig_value = sig_table[1].signal;

    if (min_sig_value != sig_table[1].signal)
        return min_sig_value;

    min_sig_value = sig_table[0].signal;

    for (size_t i = 1; i < ncai_get_signal_count(); i++)
    {
        if (sig_table[i].signal < min_sig_value)
            min_sig_value = sig_table[i].signal;
    }

    return min_sig_value;
}

static jint ncai_get_max_signal()
{
    static int max_sig_value = -1;

    if (max_sig_value != -1)
        return max_sig_value;

    max_sig_value = sig_table[0].signal;

    for (size_t i = 1; i < ncai_get_signal_count(); i++)
    {
        if (sig_table[i].signal > max_sig_value)
            max_sig_value = sig_table[i].signal;
    }

    return max_sig_value;
}

static const char* ncai_get_signal_name(jint signal)
{
    st_signal_info* psig = find_signal(signal);
    return psig ? psig->name : NULL;
}

static size_t ncai_get_signal_name_size(jint signal)
{
    st_signal_info* psig = find_signal(signal);
    return psig ? psig->name_size : 0;
}

bool ncai_is_signal_in_range(jint signal)
{
    return (signal >= ncai_get_min_signal() ||
            signal <= ncai_get_max_signal());
}
