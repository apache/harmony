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
 * @author Ilya Berezhniuk
 */

#ifndef _NATIVE_STACK_H_
#define _NATIVE_STACK_H_

#include "open/platform_types.h"
#include "port_unwind.h"
#include "jni.h"
#include "stack_iterator.h"
#include "vm_threads.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    jint    java_depth;
    void*   ip;
    void*   frame;
    void*   stack;
} native_frame_t;


// If frame_array is NULL, only returns real frame count
int walk_native_stack_registers(UnwindContext* context, Registers* pregs,
    VM_thread* pthread, int max_depth, native_frame_t* frame_array);

bool native_is_ip_stub(void* ip);
char* native_get_stub_name(void* ip, char* buf, size_t buflen);
const char* native_get_stub_name_nocpy(void* ip);

#ifdef __cplusplus
}
#endif

#endif // _NATIVE_STACK_H_
