/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
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
 * @file
 * Export interfaces provided by the interpreter.
 *
 * The current DRLVM implementation describes how the interpreter exports its
 * enumeration, stack trace generation and JVMTI support functions via a single
 * method table, to create the Interpreter interface.*/

#ifndef _INTERPRETER_EXPORTS_H_
#define _INTERPRETER_EXPORTS_H_

#include "open/types.h"
#include "jvmti.h"

typedef struct FrameHandle FrameHandle;

/**
 * @ingroup
 * Interpreter table
 */

typedef struct {

/**
 * Fills the stack trace frame at the given depth for the current thread.
 *
 * @param[in] target_depth - the zero-based depth of the frame or inlined method,
 *                           information about which will be stored at the given
 *                           stack trace frame, <i>stf</i>
 * @param[out] stf         - the pointer to the <code>StackTraceFrame</code>
 *                           structure that needs to be filled with the data on
 *                           the frame or inlined method corresponding to the
 *                           given depth
 * @return <code>TRUE</code> on success, <code>FALSE</code> if the depth is greater than
 *          or equal to the current thread stack trace length.*/
    bool (*interpreter_st_get_frame) (unsigned target_depth, struct StackTraceFrame* stf);

/**
 * Fills the stack trace frames for the specified number of frames of the specified thread.
 *
 * @param[in] thread     - the pointer to the thread
 * @param[in] res_depth  - the number of frames including inlined methods,
 *                         information about which should be stored
 * @param[out] stfs      - the pointer to the array of stack trace frames
 *                         created by this function and returned via this pointer
 * @note The caller is responsible for freeing the memory.*/
    void (*interpreter_st_get_trace) (struct VM_thread *thread, unsigned* res_depth, struct StackTraceFrame** stfs);

/**
 * Enumerates references associated with the thread.
 *
 * @param thread - the pointer to the thread*/
    void (*interpreter_enumerate_thread) (struct VM_thread *thread);

/**
 * Returns the last frame.
 *
 * @param thread - the pointer to the thread
 * @return The pointer to the last frame.*/
    FrameHandle* (*interpreter_get_last_frame) (struct VM_thread *thread);

/** Returns the previous frame.
 *
 * @param frame - the pointer to the frame
 * @return The pointer to the previous frame.*/
    FrameHandle* (*interpreter_get_prev_frame) (FrameHandle* frame);

/**
 * Returns the frame method.
 *
 * @param frame - the pointer to the frame
 * @return The pointer to the method.*/
    Method_Handle (*interpreter_get_frame_method) (FrameHandle* frame);

/**
 * Returns the pointer to the bytecode.
 *
 * @param frame - the pointer to the frame
 * @return The pointer to the bytecode.*/
    U_8* (*interpreter_get_frame_bytecode_ptr) (FrameHandle* frame);
    // 'end' is not inclusive

/**
 * Returns <code>TRUE</code> if the frame is native.
 *
 * @param frame - the pointer to the frame
 * @param begin - the pointer to the register
 * @param end   - the pointer to the register
 * @return <code>TRUE</code> on success.*/
    bool (*is_frame_in_native_frame) (struct FrameHandle* frame, void* begin, void* end);

/**
 * Enumerates references associated with the thread.
 *
 * @param thread   - the pointer to the thread
 * @param jvmtiEnv - the pointer to the jvmti environment*/
    void (*interpreter_ti_enumerate_thread) (jvmtiEnv*, struct VM_thread *thread);

#ifdef _IPF_
/**
 * Returns the stacked register address.
 *
 * @param bsp - the pointer to the register
 * @param reg - the register
 * @return The stacked register address.*/
    uint64* (*interpreter_get_stacked_register_address) (uint64* bsp, unsigned reg);
#endif

/**
 * Returns the frame location.
 *
 * @param jvmtiEnv   - the pointer to the jvmti environment
 * @param thread     - the pointer to the thread
 * @param depth      - the pointer to the depth
 * @param _jmethodID - the pointer to the method
 * @param jlocation  - the pointer to the location
 * @return <code>JVMTI_ERROR_NONE</code> - a successfully added notification<br>
 *         <code>JVMTI_ERROR_NO_MORE_FRAMES</code> - depth is too large*/
    jvmtiError (*interpreter_ti_getFrameLocation) ( jvmtiEnv*, struct VM_thread*,
            int, struct _jmethodID * *, int64 *);

/**
 * Returns the value of the 32 bit local variable.
 *
 * @param jvmtiEnv  - the pointer to the jvmti environment
 * @param thread    - the pointer to the thread
 * @param depth     - the pointer to the depth
 * @param slot      - the pointer to the slot
 * @param value_ptr - the pointer to the value
 * @return <code>JVMTI_ERROR_NONE</code>           - a successfully added notification<br>
 *         <code>JVMTI_ERROR_NO_MORE_FRAMES</code> - depth is too large<br>
 *         <code>JVMTI_ERROR_OPAQUE_FRAME</code>   - no frame<br>
 *         <code>JVMTI_ERROR_INVALID_SLOT</code>   - a bad slot<br>
 *         <code>JVMTI_ERROR_TYPE_MISMATCH</code>  - an invalid variable type*/
    jvmtiError (*interpreter_ti_getLocal32) ( jvmtiEnv*, struct VM_thread*, int, int, int *);

/**
 * Returns the value of 64 bit local variable.
 *
 * @param jvmtiEnv  - the pointer to the jvmti environment
 * @param thread    - the pointer to the thread
 * @param depth     - the pointer to the depth
 * @param slot      - the pointer to the slot
 * @param value_ptr - the pointer to the value
 * @return <code>JVMTI_ERROR_NONE</code>           - a successfully added notification<br>
 *         <code>JVMTI_ERROR_NO_MORE_FRAMES</code> - depth is too large<br>
 *         <code>JVMTI_ERROR_OPAQUE_FRAME</code>   - no frame<br>
 *         <code>JVMTI_ERROR_INVALID_SLOT</code>   - a bad slot<br>
 *         <code>JVMTI_ERROR_TYPE_MISMATCH</code>  - an invalid variable type*/
    jvmtiError (*interpreter_ti_getLocal64) ( jvmtiEnv*, struct VM_thread*, int, int, int64 *);

/**
 * Returns the value of the <code>Object</code> type local variable.
 *
 * @param jvmtiEnv  - the pointer to the jvmti environment
 * @param thread    - the pointer to the thread
 * @param depth     - the pointer to the depth
 * @param slot      - the pointer to the slot
 * @param value_ptr - the pointer to the value
 * @return <code>JVMTI_ERROR_NONE</code>           - a successfully added notification<br>
 *         <code>JVMTI_ERROR_NO_MORE_FRAMES</code> - depth is too large<br>
 *         <code>JVMTI_ERROR_OPAQUE_FRAME</code>   - no frame<br>
 *         <code>JVMTI_ERROR_INVALID_SLOT</code>   - a bad slot<br>
 *         <code>JVMTI_ERROR_TYPE_MISMATCH</code>  - an invalid variable type*/
    jvmtiError (*interpreter_ti_getObject) ( jvmtiEnv*, struct VM_thread*, int, int, struct _jobject * *);

/**
 * Returns stack trace data.
 *
 * @param jvmtiEnv        - the pointer to the jvmti environment
 * @param thread          - the pointer to the thread
 * @param start_depth     - the pointer to the depth
 * @param max_frame_count - the pointer to <code>max_frame_count</code>
 * @param frame_buffer    - the pointer to <code>frame_buffer</code>
 * @param count_ptr       - the pointer to the count
 * @return <code>JVMTI_ERROR_NONE</code>             - a successfully added notification<br>
 *         <code>JVMTI_ERROR_ILLEGAL_ARGUMENT</code> - bad arguments*/
    jvmtiError (*interpreter_ti_getStackTrace) (jvmtiEnv*, struct VM_thread*, int, int, jvmtiFrameInfo*, int *);

/**
 * Returns frame count.
 *
 * @param jvmtiEnv       - the pointer to the jvmti environment
 * @param thread         - the pointer to the thread
 * @param count_ptr[out] - the pointer to the count
 * @return <code>JVMTI_ERROR_NONE</code> - a successfully added notification*/
    jvmtiError (*interpreter_ti_get_frame_count) ( jvmtiEnv*, struct VM_thread*, int *);

/**
 * Sets the value of 32 bit local variable.
 *
 * @param jvmtiEnv  - the pointer to the jvmti environment
 * @param thread    - the pointer to the thread
 * @param depth     - the pointer to the depth
 * @param slot      - the pointer to the slot
 * @param value_ptr - the pointer to the value
 * @return <code>JVMTI_ERROR_NONE</code>           - a successfully added notification<br>
 *         <code>JVMTI_ERROR_NO_MORE_FRAMES</code> - depth is too large<br>
 *         <code>JVMTI_ERROR_OPAQUE_FRAME</code>   - no frame<br>
 *         <code>JVMTI_ERROR_INVALID_SLOT</code>   - a bad slot<br>
 *         <code>JVMTI_ERROR_TYPE_MISMATCH</code>  - an invalid variable type*/
    jvmtiError (*interpreter_ti_setLocal32) ( jvmtiEnv*, struct VM_thread*, int, int, int);

/**
 * Sets the value of 64 bit local variable.
 *
 * @param jvmtiEnv  - the pointer to the jvmti environment
 * @param thread    - the pointer to the thread
 * @param depth     - the pointer to the depth
 * @param slot      - the pointer to the slot
 * @param value_ptr - the pointer to the value
 * @return <code>JVMTI_ERROR_NONE</code>           - a successfully added notification<br>
 *         <code>JVMTI_ERROR_NO_MORE_FRAMES</code> - depth is too large<br>
 *         <code>JVMTI_ERROR_OPAQUE_FRAME</code>   - no frame<br>
 *         <code>JVMTI_ERROR_INVALID_SLOT</code>   - a bad slot<br>
 *         <code>JVMTI_ERROR_TYPE_MISMATCH</code>  - an invalid variable type<br>*/
    jvmtiError (*interpreter_ti_setLocal64) ( jvmtiEnv*, struct VM_thread*, int, int, int64);

/**
 * Sets the value of the <code>Object</code> type local variable.
 *
 * @param jvmtiEnv  - the pointer to the jvmti environment
 * @param thread    - the pointer to the thread
 * @param depth     - the pointer to the depth
 * @param slot      - the pointer to the slot
 * @param value_ptr - the pointer to the value
 * @return <code>JVMTI_ERROR_NONE</code>           - a successfully added notification<br>
 *         <code>JVMTI_ERROR_NO_MORE_FRAMES</code> - depth is too large<br>
 *         <code>JVMTI_ERROR_OPAQUE_FRAME</code>   - no frame<br>
 *         <code>JVMTI_ERROR_INVALID_SLOT</code>   - a bad slot<br>
 *         <code>JVMTI_ERROR_TYPE_MISMATCH</code>  - an invalid variable type*/
    jvmtiError (*interpreter_ti_setObject) ( jvmtiEnv*, struct VM_thread*, int, int, struct _jobject *);

/**
 * Returns the interrupted method native bit.
 *
 * @param thread - the pointer to the thread
 * @return The interrupted method native bit.*/
    unsigned int (*interpreter_st_get_interrupted_method_native_bit) (struct VM_thread *);

/** @defgroup open_interfaces Open Interfaces
 * Open interfaces.*/
/*@{*/

/**
 * The function is called when the global TI event state is changed. This means
 * that at least one of jvmtiEnv's enabled the event or the event was
 * disabled in all environments.
 *
 * @param event_type - jvmti to enable/disable
 * @param enable     - enable or disable the events in exe*/
    void (*interpreter_ti_set_notification_mode)(jvmtiEvent event_type, bool enable);

/**
 * Sets the breakpoint in the place identified by the method and location.
 * No more than one breakpoint will be set at any specific place. Handling
 * for multiple jvmti environments is done by the jvmti framework.
 *
 * @return The bytecode has been replaced by instrumentation.*/
    jbyte (*interpreter_ti_set_breakpoint)(jmethodID method, jlocation location);

/**
 * Clears the breakpoint in the place identified by the method and location.
 * Replaced the bytecode, returned by <code>interpreter_ti_set_breakpoint(..)</code>,
 * is also passed as a parameter.*/
    void (*interpreter_ti_clear_breakpoint)(jmethodID method, jlocation location, jbyte saved);

/**
 * Sets a callback to notify JVMTI about the frame-pop event.
 *
 * @return <code>JVMTI_ERROR_NONE</code>           - a successfully added notification<br>
 *         <code>JVMTI_ERROR_OPAQUE_FRAME</code>   - the frame is native<br>
 *         <code>JVMTI_ERROR_NO_MORE_FRAMES</code> - depth is too large*/
    jvmtiError (*interpreter_ti_notify_frame_pop) (jvmtiEnv*,
                                                   VM_thread *thread,
                                                   int depth);

/**
 * Pops the frame.
 *
 * @param jvmtiEnv - the pointer to the jvmti environment
 * @param thread   - the pointer to the thread
 * @return <code>JVMTI_ERROR_NONE</code>         - a successfully added notification<br>
 *         <code>JVMTI_ERROR_OPAQUE_FRAME</code> - no frame*/
    jvmtiError (*interpreter_ti_pop_frame) (jvmtiEnv*, VM_thread *thread);

/**
 * Dumps the stack.
 *
 * @param file - File to dump stack to
 * @param thread - the pointer to the thread*/
   void (*stack_dump) (int, VM_thread*);

} Interpreter;

/*@}*/

/**
 * Returns the interpreter table.
 *
 * @return The interpreter table.*/
VMEXPORT Interpreter *interpreter_table();

#ifdef BUILDING_VM
extern Interpreter interpreter;

/**
 * Returns <code>TRUE</code> if interpreter table.
 *
 * @return <code>TRUE</code> on success.*/
extern bool interpreter_enabled();
#endif

#endif /* _INTERPRETER_EXPORTS_H_ */
