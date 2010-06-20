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


#ifndef __NATIVE_UNWIND_H__
#define __NATIVE_UNWIND_H__

#include "port_modules.h"
#include "port_unwind.h"


#ifdef __cplusplus
extern "C" {
#endif

//////////////////////////////////////////////////////////////////////////////
// Interchange between platform-dependent and general functions

bool native_unwind_stack_frame(UnwindContext* context, Registers* regs);
bool native_get_stack_range(UnwindContext* context, Registers* regs, native_segment_t* seg);
bool native_is_frame_exists(UnwindContext* context, Registers* regs);
bool native_unwind_special(UnwindContext* context, Registers* regs);
bool native_is_in_code(UnwindContext* context, void* ip);
bool native_is_in_stack(UnwindContext* context, void* sp);


#ifdef __cplusplus
}
#endif

#endif /* !__NATIVE_UNWIND_H__ */
