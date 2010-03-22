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

#ifndef _EXCEPTION_MANAGER_H
#define _EXCEPTION_MANAGER_H

#include <setjmp.h>
#include <stdlib.h>
#include <stdio.h>

#include "AgentBase.h"
#include "AgentException.h"
#include "AgentMonitor.h"
#include "vmi.h"
#include "hythread.h"
#include "hyport.h"
#include "jni.h"
#include "jvmti.h"

#define JDWP_SET_EXCEPTION(ex) AgentBase::GetExceptionManager().SetException(ex)

#define JDWP_LAST_ERROR_CODE \
  AgentBase::GetExceptionManager().ReadLastErrorCode()

#define JDWP_CHECK_ERROR_CODE(var) var = JDWP_LAST_ERROR_CODE; \
                                   if (var != JDWP_ERROR_NONE) { \
                                     return var; \
                                   }

#define JDWP_HAS_EXCEPTION (JDWP_LAST_ERROR_CODE != JDWP_ERROR_NONE)

#define JDWP_CHECK_RETURN(value) if (value != JDWP_ERROR_NONE) { \
                                    return value; \
                                 }

#define JDWP_CHECK_NOT_NULL(value) if (value == NULL) {\
                                    return JDWP_LAST_ERROR_CODE; \
                                 }

namespace jdwp {

    typedef hythread_t ThreadId_t;

    struct exception_context {
        ThreadId_t id;
        AgentException* lastException;
        struct exception_context* next_context;
    };

    typedef struct exception_context exception_context;


    class ExceptionManager
    {
    public:
        ExceptionManager();

        /**
         * A destructor.
         */
        ~ExceptionManager();

        void Init(JNIEnv *jni);
        void Clean();

        void SetException(AgentException& ex);
        AgentException GetLastException();
        jdwpError ReadLastErrorCode();

        inline void* operator new(size_t size) {
            return malloc(size);
        }

        inline void operator delete(void* ptr) {
            free(ptr);
        }

    private:
        exception_context* GetCurrentContext(ThreadId_t tid);
        exception_context* AddNewContext(ThreadId_t tid);
        AgentException* Clone(AgentException* ex);
        exception_context* m_context;

        JavaVM *m_jvm;
        AgentMonitor* m_monitor;
    };
};
#endif  // _EXCEPTION_MANAGER_H
