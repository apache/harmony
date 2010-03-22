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

//
// These are the functions that a JIT built as a DLL must export.
// Some functions may be optional and are marked as such.
//

#ifndef _JIT_EXPORT_H
#define _JIT_EXPORT_H


#include "open/types.h"
#include "open/ee_em_intf.h"
#include "open/rt_types.h"

#ifdef __cplusplus
extern "C" {
#endif // __cplusplus




////////////////////////////////////////////////////////
// Optional functions that don't have to be provided.
////////////////////////////////////////////////////////

JITEXPORT void JIT_next_command_line_argument(JIT_Handle jit, const char *name, const char *arg);


////////////////////////////////////////////////////////
// Required functions.
////////////////////////////////////////////////////////



/** 
    * Performs compilation of given method.
    *
    * @param method_handle      - handle of the method to be compiled
    * @param compilation_params - compilation parameters. If NULL, default compilation parameters
    *     should be used by the JIT (passed in the initialize function). If either of parameters is
    *     not supported by the JIT, the function should return compilation failure.
    * @return compilation status
    */
JITEXPORT JIT_Result JIT_compile_method_with_params(
    JIT_Handle jit,
    Compile_Handle     compile_handle, 
    Method_Handle       method_handle,
    OpenMethodExecutionParams compilation_params
    );


/**
    * Retrieves method execution-related capabilities supported by the EE.
    *
    * @return the set of supported capabilities
    */
JITEXPORT OpenMethodExecutionParams JIT_get_exe_capabilities (JIT_Handle jit);


#ifdef __cplusplus
}
#endif // __cplusplus


#endif /* _JIT_EXPORT_H */
