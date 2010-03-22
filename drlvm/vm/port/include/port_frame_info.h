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

#ifndef _PORT_FRAME_INFO_
#define _PORT_FRAME_INFO_

#include "open/platform_types.h"

/**
 * @file Interface to stack iteration from inside of port library
 * subcomponents
 */

/**
 * Structure describing stack frame information for stack iteration
 */
typedef struct {
    /**
     * Method class name executed at specified stack frame
     */
    const char *method_class_name;
    /**
     * Method name executed at specified stack frame
     */
    const char *method_name;
    /**
     * Method signature executed at specified stack frame
     */
    const char *method_signature;
    /**
     * Method source file executed at specified stack frame
     */
    const char *source_file_name;
    /**
     * Source line number
     */
    int source_line_number;
    /**
     * Stack iteration internal state. It should be initialized with
     * NULL when structure is created. In case
     * port_unwind_compiled_frame returns value greater than zero, it
     * should be initialized with a pointer to a buffer of memory of
     * this value size, and this buffer should be filled with
     * zeroes. When iteration goes on, this buffer contains iteration
     * internal data, like information about inlined methods depth.
     */
    void *iteration_state;
} port_stack_frame_info;

/**
 * Callback function type for unwinding stack frames. Function is
 * specific to the stack type and can unwind only the stack types of
 * its component.
 *
 * @param regs - Register context of the current stack frame, function
 * updates it to a new stack frame if unwinding succeeds.
 * @param frame_info - Pointer to stack frame information structure,
 * it is filled in with information for the current (not unwound!) stack
 * frame.
 * @return Positive number in case stack iterator buffer has to be
 * allocated in frame_info->iteration_state. Zero if unwinding is
 * successful and -1 if unwinding is not successful.
 */
typedef int (*port_unwind_compiled_frame)(Registers *regs,
    port_stack_frame_info *frame_info);

#endif /* _PORT_FRAME_INFO_ */

