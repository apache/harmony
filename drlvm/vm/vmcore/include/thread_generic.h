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
 * @author Andrey Chernyshev
 */  


#ifndef THREAD_GENERIC_HEADER
#define THREAD_GENERIC_HEADER

#include "vm_threads.h"

#ifdef __cplusplus
extern "C" {
#endif

VMEXPORT vm_thread_t jthread_get_vm_thread_ptr_safe(jobject thread_obj);

/////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////


#ifdef __cplusplus
}
#endif


#endif //THREAD_GENERIC_HEADER
