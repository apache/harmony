/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
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
 * @file
 * Defines native-related macros and functions used by the interpreter.
 */

#include "vm_core_types.h"

#ifdef _IPF_
#include "../m2n_ipf_internal.h"
#elif defined _EM64T_
#include "../m2n_em64t_internal.h"
#else
#include "../m2n_ia32_internal.h"
#endif

/**
 * Enumerates thread-assocciated references that are not stored on the
 * thread's stack.
 *
 * @param[in] thread - the thread pointer
 */
extern void
vm_enumerate_root_set_single_thread_not_on_stack(VM_thread * thread);

/**
 * Frees and deletes all local object handles.
 *
 * @param[in] head - the object handles pointer
 */
extern VMEXPORT void free_local_object_handles2(ObjectHandles * head);

/** Allocates memory.*/
#define M2N_ALLOC_MACRO                                     \
    assert(!hythread_is_suspend_enabled());                      \
    M2nFrame m2n;                                           \
    memset((void*)&m2n, 0, sizeof(M2nFrame));               \
    m2n.prev_m2nf = m2n_get_last_frame();                   \
    m2n_set_last_frame(&m2n);                               \
                                                            \
    union {                                                 \
        ObjectHandlesOld old;                               \
        ObjectHandlesNew nw;                                \
    } handles;                                              \
    handles.nw.capacity = 0;                                \
    handles.nw.size = 0;                                    \
    handles.nw.next = 0;                                    \
    m2n_set_local_handles(&m2n, (ObjectHandles*)&handles)

/** Frees memory.*/
#define M2N_FREE_MACRO                                      \
    assert(!hythread_is_suspend_enabled());                      \
    free_local_object_handles2(m2n_get_local_handles(&m2n));\
    m2n_set_last_frame(m2n_get_previous_frame(&m2n))

/**
 * Looks up the implementation for the native method.
 *
 * @param[in] method - the searching native-method structure
 * @return The pointer to find the native function.
 */
GenericFunctionPointer interpreterGetNativeMethodAddr(Method*);



