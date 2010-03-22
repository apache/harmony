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
 * Import interfaces used by the interpreter.
 *
 * The current DRLVM implementation describes the following import interfaces:
 * interpreter import locking, exceptions handling, JVMTI and JNI functionality.*/

#ifndef _INTERPRETER_IMPORTS_H_
#define _INTERPRETER_IMPORTS_H_

#include "open/types.h"
#include "vm_core_types.h"
#include "jvmti.h"

struct ManagedObject;
typedef struct ManagedObject ManagedObject;

/**
 * Gains ownership over a monitor.
 * The current thread blocks, if the specified monitor is owned by another thread.
 *
 * @param[in] obj - the monitor object where the monitor is located*/
VMEXPORT void vm_monitor_enter_wrapper(ManagedObject *obj);

/**
 * Releases ownership over a monitor.
 *
 * @param[in] obj - the monitor object where the monitor is located*/
VMEXPORT void vm_monitor_exit_wrapper(ManagedObject *obj);

/**
 * Calls the <code>class_throw_linking_error</code> function that throws
 * a linking error.
 *
 * @param[in] ch       - the class handle
 * @param[in] cp_index - the index in the constant pool
 * @param[in] opcode   - the opcode of bytecodes*/
VMEXPORT void class_throw_linking_error_for_interpreter(Class_Handle ch,
        unsigned cp_index, unsigned opcode);

/**
 * Returns the JNI environment.
 *
 * @return The JNI environment associated with this thread.*/
VMEXPORT JNIEnv * get_jni_native_intf();

/**
 * A callback function for interpreter breakpoint processing.
 *
 * @param[in] method - the method ID
 * @param[in] loc    - the location*/
VMEXPORT jbyte jvmti_process_interpreter_breakpoint_event(jmethodID method, jlocation loc);

/**
 * Enables single-step event processing.
 *
 * @param[in] method   - the method ID
 * @param[in] location - the location*/
VMEXPORT void jvmti_process_single_step_event(jmethodID method, jlocation location);

/**
 * Enables frame-pop event processing.
 *
 * @param[in] env                     - the jvmti environment
 * @param[in] method                  - the method ID
 * @param[in] was_popped_by_exception - if the frame was popped by exception*/
VMEXPORT void jvmti_process_frame_pop_event(jvmtiEnv *env,
        jmethodID method, jboolean was_popped_by_exception);

/**
 * Looks for a method in native libraries of a class loader.
 *
 * @param[in] method - a searching native-method structure
 * @return The pointer to found a native function.
 * @note The function raises <code>UnsatisfiedLinkError</code> with a method name
 *       in an exception message, if the specified method is not found.*/
VMEXPORT GenericFunctionPointer classloader_find_native(const Method_Handle method);

#endif /* _INTERPRETER_IMPORTS_H_ */
