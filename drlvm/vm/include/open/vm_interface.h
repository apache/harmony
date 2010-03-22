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

#ifndef _VM_INTERFACE_H
#define _VM_INTERFACE_H

#include "open/types.h"
//#include "open/rt_types.h"
//#include "open/rt_helpers.h"
//#include "open/em.h"

#define PROTOTYPE_WITH_NAME(return_type, func_name, prototype) \
    typedef return_type (*func_name##_t)prototype

#define GET_INTERFACE(get_adapter, func_name) \
    (func_name##_t)get_adapter(#func_name)

DECLARE_OPEN(int, vector_get_first_element_offset, (Class_Handle element_type)); //vector_first_element_offset_class_handle
DECLARE_OPEN(int, vector_get_length_offset, ()); //vector_length_offset


//Vm
PROTOTYPE_WITH_NAME(IDATA       , vm_tls_alloc, (UDATA* key)); //IDATA VMCALL hythread_tls_alloc(hythread_tls_key_t *handle) 
PROTOTYPE_WITH_NAME(UDATA       , vm_tls_get_offset, (UDATA key)); //UDATA VMCALL hythread_tls_get_offset(hythread_tls_key_t key)
PROTOTYPE_WITH_NAME(UDATA       , vm_tls_get_request_offset, ()); //DATA VMCALL hythread_tls_get_request_offset
PROTOTYPE_WITH_NAME(UDATA       , vm_tls_is_fast, (void));//UDATA VMCALL hythread_uses_fast_tls
PROTOTYPE_WITH_NAME(IDATA       , vm_get_tls_offset_in_segment, (void));//IDATA VMCALL hythread_get_hythread_offset_in_tls(void)

#endif // _VM_INTERFACE_H
