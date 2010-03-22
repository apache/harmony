/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#ifndef USING_VMI
#define USING_VMI
#endif

#include "ExceptionManager.h"

using namespace jdwp;

static inline ThreadId_t _GetCurrentThreadId(JavaVM *jvm)
{
    ThreadId_t tid;
#ifdef HY_NO_THR
    THREAD_ACCESS_FROM_JAVAVM(jvm);
#endif /* HY_NO_THR */
    hythread_attach(&tid);
    return tid;
} // GetCurrentThreadId()


static inline bool ThreadId_equal(ThreadId_t treadId1, ThreadId_t treadId2)
{
    return (treadId1 == treadId2);
} // ThreadId_equal()

void ExceptionManager::Init(JNIEnv *jni) {
    jni->GetJavaVM(&m_jvm);
    m_monitor = new AgentMonitor("_jdwp_exception_monitor");
}

ExceptionManager::ExceptionManager() {
    m_context = 0;
}

void ExceptionManager::Clean() {
    exception_context* context = m_context;
    while (context != 0) {
        m_context = context->next_context;
        free(context);
        context = m_context;
    }
}

exception_context* ExceptionManager::GetCurrentContext(ThreadId_t tid) {
    
    exception_context* context = m_context;
    exception_context* pre_context = 0;
 
    while (context != 0) {
        // remove context contains no exception
        while (context->lastException == NULL) {
            if (pre_context == 0) {
                m_context = context->next_context;
                free(context);

                context = m_context;
            } else {
                pre_context->next_context = context->next_context;
                free(context);
                context = pre_context->next_context;
            }

            if (context == 0) {
                return 0;
            }
        }

        if (ThreadId_equal(tid, context->id)) {
            return context;
        }

        pre_context = context;
        context = context->next_context;
    }

    return 0;
}

exception_context* ExceptionManager::AddNewContext(ThreadId_t tid) {
    exception_context* context = (exception_context*) malloc(sizeof(exception_context));
    context->id = tid;
    context->next_context = m_context;
    context->lastException = NULL;
    m_context = context;
    return m_context;
}

AgentException* ExceptionManager::Clone(AgentException* ex) {
    AgentException* e = new AgentException(*ex);
    return e;
}


void ExceptionManager::SetException(AgentException& ex) {
    ThreadId_t tid = _GetCurrentThreadId(m_jvm);
    MonitorAutoLock lock(m_monitor JDWP_FILE_LINE);

    exception_context* context = GetCurrentContext(tid);
    if (context == 0) {
        context = AddNewContext(tid);
    }

    if (!context->lastException) {
        context->lastException = Clone(&ex);
    }
}

AgentException ExceptionManager::GetLastException() {
    ThreadId_t tid = _GetCurrentThreadId(m_jvm);
    MonitorAutoLock lock(m_monitor JDWP_FILE_LINE);

    exception_context* context = GetCurrentContext(tid);
    AgentException* aex;
    if (context == 0 || context->lastException == 0) {
        aex = new AgentException(JDWP_ERROR_NONE);
    } else {    
        aex = context->lastException;
        context->lastException = NULL;
    }
    return *aex;
}

jdwpError ExceptionManager::ReadLastErrorCode() {
    ThreadId_t tid = _GetCurrentThreadId(m_jvm);
    MonitorAutoLock lock(m_monitor JDWP_FILE_LINE);
    exception_context* context = GetCurrentContext(tid);
    return (context == 0 || context->lastException == 0) ?
        JDWP_ERROR_NONE : context->lastException->ErrCode();
}
