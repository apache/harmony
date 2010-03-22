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
 * @author Intel, Evgueni Brevnov, Ivan Volosyuk
 */  





#ifndef _JIT_RUNTIME_SUPPORT_COMMON_H_
#define _JIT_RUNTIME_SUPPORT_COMMON_H_

#include "lil.h"
#include "platform_lowlevel.h"
#include "heap.h"

/**
 * When entering managed function, obey the (sp)%%4 == 0 rule.
 */
#define STACK_ALIGN4         (0x00000004)

/**
 * When entering managed function, obey the (sp)%%8 == 0 rule.
 */
#define STACK_ALIGN_HALF16   (0x00000008)

/**
 * When entering managed function, obey the (sp)%%16 == 0 rule.
 */
#define STACK_ALIGN16         (0x00000010)


#ifdef _EM64T_
    #define MANAGED_STACK_ALIGNMENT STACK_ALIGN_HALF16
#else
    // Use 16-byte stack pointer alignment to avoid perf penalty on SSE usage in JITted code. 
    #define MANAGED_STACK_ALIGNMENT STACK_ALIGN16
#endif

VMEXPORT // temporary solution for interpreter unplug
int __stdcall vm_instanceof(ManagedObject *obj, Class *c);

// Implements VM_RT_AASTORE
void * __stdcall
vm_rt_aastore(Vector_Handle array, int idx, ManagedObject *elem);

// Implements VM_RT_AASTORE_TEST
int __stdcall
vm_aastore_test(ManagedObject *elem, Vector_Handle array);


void *vm_get_interface_vtable(ManagedObject *obj, Class *iid);

void vm_instanceof_update_stats(ManagedObject *obj, Class *super);
void vm_checkcast_update_stats(ManagedObject *obj, Class *super);
void vm_aastore_test_update_stats(ManagedObject *elem, Vector_Handle array);

void vm_rt_class_initialize(Class *clss);
void vm_rt_class_throw_linking_error(Class_Handle ch, unsigned index, unsigned opcode);

ManagedObject* vm_rt_class_alloc_new_object(Class *c);
Vector_Handle vm_rt_new_vector(Class *vector_class, int length);
Vector_Handle vm_rt_new_vector_using_vtable_and_thread_pointer(
        int length, Allocation_Handle vector_handle, void *tp);

/** 
 * Creates a LIL code stub for checkcast or instance of
 * can be used by both IA32 and IPF code
 */
LilCodeStub *gen_lil_typecheck_stub(bool is_checkcast);
 
#endif
