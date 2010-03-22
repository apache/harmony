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
 * @author Intel, Salikh Zakirov
 */  






#ifndef _FINALIZE_H_
#define _FINALIZE_H_

#include "open/types.h"

#ifdef USE_GC_STATIC
extern int running_finalizers_deferred;
#endif

#ifndef USE_GC_STATIC
VMEXPORT
#endif
void vm_run_pending_finalizers();
int vm_do_finalization(int quantity);
int vm_get_finalizable_objects_quantity();
bool vm_finalization_is_enabled();
void vm_obtain_finalizer_fields();

#ifndef USE_GC_STATIC
VMEXPORT
#endif
void vm_enumerate_objects_to_be_finalized();
void vm_enumerate_references_to_enqueue();
int vm_get_references_quantity();

void vm_activate_ref_enqueue_thread();
void vm_enqueue_references();
void vm_ref_enqueue_func(void);   // added for NATIVE REFERENCE ENQUEUE THREAD

Boolean get_native_finalizer_thread_flag(); // added for NATIVE FINALIZER THREAD
Boolean get_native_ref_enqueue_thread_flag(); // added for NATIVE REF ENQUEUE THREAD
void wait_native_fin_threads_detached(void); // added for NATIVE FINALIZER THREAD
void wait_native_ref_thread_detached(void); // added for NATIVE REF ENQUEUE THREAD

#endif
