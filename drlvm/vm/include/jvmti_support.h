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
#ifndef _JVMTI_SUPPORT_H_
#define _JVMTI_SUPPORT_H_

#include <open/types.h>
#include <jvmti_types.h>

/**
 * @file
 * This file describes inter-component interfaces needed to support
 * JVMTI in the VM.
 * 
 * For now, the functions for interpreter and jitted code
 * execution support are specified directly. This may be
 * reconsidered later to be a table function.
 */

/**
 * Enumerates one stack root to the VM.
 */
VMEXPORT void vm_ti_enumerate_stack_root(jvmtiEnv* env,
        void* root, 
        Managed_Object_Handle obj,
        jvmtiHeapRootKind root_kind,
        int depth,
        jmethodID method,
        int slot);

/**
 * Enumerates one heap root to the VM.
 */
VMEXPORT void vm_ti_enumerate_heap_root(jvmtiEnv* env,
        void* root,
        Managed_Object_Handle obj,
        jvmtiHeapRootKind root_kind);

/**
 * Requests interpreter to enumerate one thread for TI.
 */
void interpreter_ti_enumerate_thread(jvmtiEnv *env, struct VM_thread *thread);

/**
 * Requests JIT support to enumerate one thread for TI.
 */
void jitted_ti_enumerate_thread(jvmtiEnv *env, struct VM_thread *thread);


#endif // _JVMTI_SUPPORT_H_
