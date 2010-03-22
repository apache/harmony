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
#ifndef _ROOT_SET_ENUM_INTERNAL_H_
#define _ROOT_SET_ENUM_INTERNAL_H_

#include "vm_threads.h"
#include "stack_iterator.h"

enum{ VM_JVMTI_THREAD_STATE_WAITING = 0x80000000 };

// Functions defined in root_set_enum_common.cpp.
VMEXPORT // temporary solution for interpreter unplug
void vm_enumerate_root_set_single_thread_not_on_stack(VM_thread *thread);
void vm_enumerate_root_set_single_thread_on_stack(StackIterator* si);
void vm_enumerate_root_set_global_refs();
void vm_enumerate_thread(VM_thread *thread);

void vm_enumerate_static_fields();
void vm_enumerate_references_to_enqueue();
void oh_enumerate_global_handles();
void vm_enumerate_interned_strings();
void vm_enumerate_root_set_mon_arrays();


#endif // _ROOT_SET_ENUM_INTERNAL_H_
