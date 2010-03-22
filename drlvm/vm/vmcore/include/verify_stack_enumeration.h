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
 * @author Salikh Zakirov
 *
 * @file 
 * Verify stack enumeration code by conservatively scanning the stack
 */
#ifndef _VERIFY_STACK_ENUMERATION_H
#define _VERIFY_STACK_ENUMERATION_H

#ifdef _DEBUG

/* 
 * BEWARE! This code is used in _DEBUG configuration only 
 */

#include "vm_threads.h"

extern int verify_stack_enumeration_period;
extern int verify_stack_enumeration_counter;
extern bool verify_stack_enumeration_flag;

void initialize_verify_stack_enumeration();
void verify_stack_enumeration();

inline void debug_stack_enumeration()
{
    // We verify stack enumeration only when the thread
    // is about to enable suspend, or just disabled it
    if (hythread_is_suspend_enabled()) return;

    // NB: safepoints in suspend enabled mode are ignored
    // such safepoints are used in suspend.cpp to avoid deadlocks
    // during thread suspension

    if (verify_stack_enumeration_flag) {
        --verify_stack_enumeration_counter;
        if (verify_stack_enumeration_counter <= 0) {
            verify_stack_enumeration();
            verify_stack_enumeration_counter = verify_stack_enumeration_period;
        }
    }
}

#define MARK_STACK_END          \
    {                           \
        void* local_variable;   \
        p_TLS_vmthread->stack_end = &local_variable; \
    }

#else // !_DEBUG

#define MARK_STACK_END

inline void debug_stack_enumeration() {}
inline void initialize_verify_stack_enumeration() {}
inline void verify_stack_enumeration() {}

#endif // !_DEBUG
#endif // _VERIFY_STACK_ENUMERATION_H
