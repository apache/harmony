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
#ifndef _JIT_EXPORT_JPDA_H
#define _JIT_EXPORT_JPDA_H

#include "open/types.h"
#include "open/rt_types.h"
#include "open/em.h"

#ifdef __cplusplus
extern "C" {
#endif // __cplusplus

/**
    * Elements of this enum denote various error conditions which can
    * arise when working with the OpenExeJPDA interface.
    */
typedef enum OpenExeJpdaError {
    EXE_ERROR_NONE,
    EXE_ERROR_INVALID_METHODID,
    EXE_ERROR_INVALID_LOCATION,
    EXE_ERROR_TYPE_MISMATCH,
    EXE_ERROR_INVALID_SLOT,
    EXE_ERROR_UNSUPPORTED
} OpenExeJpdaError;


/** 
    * Gets corresponding native code location for given bytecode location.
    * @param method    method whose bytecode contains the location
    * @param bc_pc     location within the bytecode
    * @param native_pc (out) upon successfull return points to corresponding native
    *     code location 
    * @return
    *     EXE_ERROR_NONE on success
    *     EXE_ERROR_INVALID_METHODID if the given method handle is invalid
    *     EXE_ERROR_INVALID_LOCATION if either the execution engine is unable to map
    *         the bytecode location or if the location is invalid (does not correspond to
    *         a valid offset of a bytecode instruction)
    *     EXE_ERROR_UNSUPPORTED if the execution engine does not support this functionality
    */ 
JITEXPORT OpenExeJpdaError get_native_location_for_bc(
        JIT_Handle jit,
        Method_Handle  method,
        uint16              bc_pc,
        NativeCodePtr  *native_pc
        );

/**
    * Gets corresponding bytecode location for given native location (absolute code address).
    * @param method    method whose (compiled native) code contains the location
    * @param native_pc location within the native code
    * @param bc_pc     (out) upon successfull return points to corresponding bytecode location 
    * @return
    *     EXE_ERROR_NONE on success
    *     EXE_ERROR_INVALID_METHODID if the given method handle is invalid
    *     EXE_ERROR_INVALID_LOCATION if either the execution engine is unable to map
    *         the native location or if the location is invalid (does not correspond to
    *         a valid offset of a processor instruction within the compiled code)
    *     EXE_ERROR_UNSUPPORTED if the execution engine does not support this functionality
    */ 
JITEXPORT OpenExeJpdaError get_bc_location_for_native(
        JIT_Handle jit,
        Method_Handle  method,
        NativeCodePtr   native_pc,
        uint16             *bc_pc
        );


/** 
    * Gets the value of the bytecode local variable in given method stack frame.
    * @param method    method in whose frame the variable is to be read
    * @param context   stack frame of the method describing its current execution state
    *     (at the point of thread suspension)
    * @param var_num    the variable's slot number
    * @param var_type  the variable's type
    * @param value_ptr address of the buffer to write variable value into. Caller is responsible for
    *     providing enough (OPEN_VM::get_vm_type_size(var_type) bytes) memory in the buffer.
    * @return
    *     EXE_ERROR_NONE on success
    *     EXE_ERROR_INVALID_METHODID if the given method handle is invalid
    *     EXE_ERROR_INVALID_LOCATION if the execution engine does not support local variable access
    *         at current execution point in the method, specified by the context
    *     EXE_ERROR_TYPE_MISMATCH if the variable with given slot is of different type,
    *     EXE_ERROR_INVALID_SLOT if the stack frame does not have variable with given slot number,
    *     EXE_ERROR_UNSUPPORTED if the execution engine does not support this functionality
    */ 
JITEXPORT OpenExeJpdaError get_local_var(
        JIT_Handle jit,
        Method_Handle              method,
        const JitFrameContext *context,
        uint16                           var_num,
        VM_Data_Type             var_type,
        void                           *value_ptr
        );


/** 
    * Sets the value of the bytecode local variable in given method stack frame.
    * @param method    method in whose frame the variable is to be changed
    * @param context   stack frame of the method describing its current execution state
    *     (at the point of thread suspension)
    * @param var_num    the variable's slot number
    * @param var_type  the variable's type
    * @param value_ptr address of the new value for the variable
    * @return
    *     EXE_ERROR_NONE on success
    *     EXE_ERROR_INVALID_METHODID if the given method handle is invalid
    *     EXE_ERROR_INVALID_LOCATION if the execution engine does not support local variable access
    *         at current execution point in the method, specified by the context
    *     EXE_ERROR_TYPE_MISMATCH if the variable with given slot is of different type,
    *     EXE_ERROR_INVALID_SLOT if the stack frame does not have variable with given slot number,
    *     EXE_ERROR_UNSUPPORTED if the execution engine does not support this functionality
    */ 
JITEXPORT OpenExeJpdaError set_local_var(
        JIT_Handle jit,
        Method_Handle              method,
        const JitFrameContext *context,
        uint16                          var_num,
        VM_Data_Type                    var_type,
        void                           *value_ptr
        );

#ifdef __cplusplus
    }
#endif

#endif /* _JIT_EXPORT_JPDA_H */
