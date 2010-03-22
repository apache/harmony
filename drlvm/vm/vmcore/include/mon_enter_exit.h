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


#ifndef MON_ENTER_EXIT_HEADER
#define MON_ENTER_EXIT_HEADER

#include "object_layout.h"
#include "vm_threads.h"

#include "open/vm_gc.h"

#ifdef __cplusplus
extern "C" {
#endif

#define P_HASH_CONTENTION_BYTE(x) ( (U_8 *)(x->get_obj_info_addr()) )
#define P_HASH_CONTENTION(x)      ((POINTER_SIZE_INT)P_HASH_CONTENTION_BYTE(x))

/**
 * This is called once at startup, before any classes are loaded,
 * and after arguments are parsed. It should set function pointers
 * to the appropriate values.
 */
 void vm_monitor_init();

/** 
 * Monitor exit from synchronized method
 */
 struct StackIterator;
void vm_monitor_exit_synchronized_method(StackIterator *si);

/**
 * Does a monitorexit operation.
 */
 extern void (*vm_monitor_exit)(ManagedObject *p_obj);
extern void (*vm_monitor_enter)(ManagedObject *p_obj);
extern U_32 (*vm_monitor_try_enter)(ManagedObject *p_obj);
extern U_32 (*vm_monitor_try_exit)(ManagedObject *p_obj);

#define HASH_MASK 0x7e

#ifdef __cplusplus
}
#endif

#endif // MON_ENTER_EXIT_HEADER
