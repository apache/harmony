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
 * @author Pavel Afremov
 */  
#ifndef _STACK_TRACE_H
#define _STACK_TRACE_H

/**
 *@file
 * Mechanism for tracing the stack of Java and native code.
 *
 * This module provides stack traces. A <b>stack trace</b> is a sequence
 * of frames counting from the top to the bottom of the stack.
 *
 * The stack trace inlcudes one frame for each managed
 * stack frame and one frame for each M2N frame that has an associated
 * method. For each frame, the method and the IP are provided and, optionally,
 * the file and line number.
 *
 * The current implementation supports the following types of frames:
 *
 * <ul>
 * <li>Managed code frames corresponding to Java code
 * <li>Managed-to-native frames (M2N) for transferring
 *      data and control between managed and native frames
 *
 * Native frames iteration is not currently supported. To iterate over
 * native frames, use OS-provided tools.
 * </ul>
 */

#include <stdio.h>
#include "open/hythread.h"
#include "open/types.h"
#include "ExpandableMemBlock.h"
#include "vm_threads.h"

// Defines the StackTraceFrame structure
#ifdef __cplusplus
extern "C" {
#endif

struct StackTraceFrame {
    Method_Handle method;
    NativeCodePtr ip;
    int depth; // Inlined depth for inlined methods, or -1 otherwise
    void *outdated_this;
};

/**
 * Gets the depth of the stack trace for the specified thread.
 *
 * The <b>depth</b> is the number of supported stack frames in the current
 * thread stack, from most recently pushed to the first pushed. The depth
 * also includes the number of inlined methods.
 *
 * @param[in]  p_vmthread - pointer to the thread
 *
 * @return The number of frames above the current one including the current frame and
 *         inlined methods.
 */
VMEXPORT unsigned st_get_depth(VM_thread *p_vmthread);

/**
 * Fills the stack trace frame at the given relative depth for the current thread.
 *
 * @param[in]  depth - relative depth of a frame or inlined method on the stack,
 *                     topmost frame has zero depth.
 * @param[out] stf   - the pointer to the <code>StackTraceFrame</code> structure that needs
 *                     to be filled with the data on the frame or inlined method
 *                     corresponding to the given depth
 *
 * @return <code>TRUE</code> on success, <code>FALSE</code> if the depth is greater than
 *          or equal to thecurrent thread's stack trace length.
 */
VMEXPORT bool st_get_frame(unsigned depth, StackTraceFrame* stf);

/**
 * Allocates memory required for the given number of stack trace frames.
 *
 * Used internaly by the interpreter to avoid memory allocation/de-allocation conflicts
 * in the VM and DLLs on Windows / IA-32 systems.
 *
 * @param[in] num - required number of stack trace frames
 *
 * @return The pointer to the allocated array of stack trace frames.
 *
 * @note The caller is responsible for freeing the memory.
 */
VMEXPORT StackTraceFrame* st_alloc_frames(int num);

/**
 * Fills the stack trace frames for the specified number of frames of the specified thread.
 *
 * @param[in] p_vmthread - pointer to the thread
 * @param[in] depth      - the number of frames including inlined methods,
 *                         information about which should be stored
 * @param[out] stfs      - the pointer to the array of stack trace frames
 *                         created by this function and returned via this
 *                         pointer.
 *
 * @note The caller is responsible for freeing the memory.
 */
VMEXPORT void st_get_trace(VM_thread *p_vmthread, unsigned* depth,
        StackTraceFrame** stfs);

/**
 * Fills the given pointer to a source file and line number by using the given method and IP.
 *
 * @param[in]  method - the handle of the method information to identify the source file
 * @param[in]  ip     - the instruction pointer to identify the JIT and using the JIT line number
 * @param[in]  depth  - the inlined depth for inlined methods;
 *                      (-1) for native methods and methods which were not inlined
 * @param[out] file   - the pointer to the file reference to be filled by this function
 * @param[out] line   - the pointer to the line number to be filled by this function
 */
VMEXPORT void get_file_and_line(Method_Handle method, void *ip, bool is_ip_past,
                                int depth, const char **file, int *line);

#ifdef __cplusplus
}
#endif

/**
 * Appends data about the given frame to the expandable buffer in a human-readable form.
 *
 * @param[in] buf -  the pointer to <code>ExpandableMemBlock</code>, where data will be added
 * @param[in] stf -  the pointer to the stack trace frame, data from which will be
 *                   printed into buffer
 */
void st_print_frame(ExpandableMemBlock* buf, StackTraceFrame* stf);

/**
 * Prints stack trace of all java threads.
 *
 * This function supports and prints all Java frames.
 *
 * @param[in] f - the pointer to the file, where the stack trace will be printed
 *
 * @note Intended for debugging purposes.
 */
void st_print_all(FILE* f);

/**
 * Prints the current java thread stack trace into specified stream.
 *
 * This function supports and prints all Java frames.
 *
 * @param[in] f - the pointer to the file, where the stack trace will be printed
 * @param[in] thread - the thread wich stack should be printed
 *
 * @note Intended for debugging purposes.
 */
void st_print(FILE* f, hythread_t thread);

/**
 * Prints the current java thread stack trace into the <code>stderr</code> stream.
 *
 * @note Intended for debugging purposes.
 */
void st_print();

#endif /* _STACK_TRACE_H */
