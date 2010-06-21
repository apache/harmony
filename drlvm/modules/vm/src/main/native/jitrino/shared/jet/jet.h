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
 * @author Alexander Astapchuk
 */

/**
 * @file
 * @brief Declaration of main interfaces provided by Jitirno.JET.
 */

#if !defined(__JET_H_INCLUDED__)
#define __JET_H_INCLUDED__

#include <open/types.h>
#include <jit_export.h>
#include <jit_export_jpda.h>

namespace Jitrino {
namespace Jet {

/**
 * @defgroup JITRINO_JET_RUNTIME_SUPPORT Runtime support
 * 
 * The 'rt_' prefix for functions in this file stands for 'runtime' -  the 
 * methods with this prefix are intended for the support of the compiled 
 * method during runtime.
 * @{
 * @}
 */

/**
 * @ingroup JITRINO_JET_RUNTIME_SUPPORT
 * @defgroup JITRINO_JET_RUNTIME_GENERAL General execution support
 * 
 * @note Most of 'rt_' functions here (except of #rt_check_method) expect 
 *       that the method passed as argument was indeed compiled by the 
 *       Jitrino.JET. If this condition is not met, behaviour is 
 *       unpredictable.
 * @{
 */

/**
 * @brief Checks whether a given method was compiled by the Jitrino.JET.
 * @param jit - JIT handle
 * @param method - a handle of a method to be checked
 * @return \b true - if the method was compiled by Jitrino.JET, 
 *         \b false otherwise.
 */
bool rt_check_method(JIT_Handle jit, Method_Handle method);

/**
 * @brief 'Unwinds' stack for a given method.
 * 
 * Speaking loosely, the 'unwinding' means 'to perform a return' from the 
 * given method. The rt_unwind function updates JitFrameContext:
 *  - 'restores' registers (if necessary)
 *  - sets IP pointer to point to the return addres
 *  - sets SP to 'pop out' IP from stack
 *
 * @param jit - JIT handle
 * @param method - method handle
 * @param context - a pointer to the context to be updated
 */
void rt_unwind(JIT_Handle jit, Method_Handle method, 
               JitFrameContext * context);


/**
 * @brief Enumerates root set for a given method.
 *
 * Root set enumeration procedure lists all the objects currently in use
 * by the given method.
 *
 * @param jit - JIT handle
 * @param method - method handle
 * @param henum - garbage collector's enumeration handle
 * @param context - a pointer to context to be updated
 */
void rt_enum(JIT_Handle jit, Method_Handle method, 
             GC_Enumeration_Handle henum, JitFrameContext * context);

/**
 * @brief 'Fixes' a catch handler context to prepare for the control to be 
 * transferred to the handler.
 *
 * 'fix handler context' for a given IP means to correct stack pointer, so 
 * a control can be transferred from the IP to appropriate handler <b>in 
 * the same method</b>.
 *
 * @param jit - JIT handle
 * @param method - method handle
 * @param context - a pointer to context to be updated
 */
void rt_fix_handler_context(JIT_Handle jit, Method_Handle method, 
                            JitFrameContext * context);

/**
* @brief Returns 'TRUE' if EIP referenced by context points to SOE checking area of the method
*/
Boolean rt_is_soe_area(JIT_Handle jit, Method_Handle method, const JitFrameContext * context);

/**
 * @brief Returns address of 'this' argument for the given method.
 *
 * Returns an address where the 'this' pointer is stored. This is normally 
 * used by VM for unwinding from the synchronized instance method, so VM 
 * can correctly perform MONITOREXIT * on the instance.
 *
 * @param jit - JIT handle
 * @param method - method handle
 * @param context - pointer to context
 * @returns address of where 'this' is stored.
 */
void * rt_get_address_of_this(JIT_Handle jit, Method_Handle method, 
                              const JitFrameContext * context);

/**
 * @brief Finds PC for a given IP.
 *
 * The function finds Java byte code's program counter (PC) for a given 
 * address of a native instruction (IP).
 * @note If the given IP does not belong to the method, the behavior is 
 *       not specified.
 *
 * @param jit - JIT handle
 * @param method - method handle
 * @param ip - address of native instruction to find PC for
 * @param pbc_pc - where to store the found PC
 */
void rt_native2bc(JIT_Handle jit, Method_Handle method, const void * ip,
                  unsigned short * pbc_pc);

/**
 * @brief Finds IP for a given PC
 *
 * The function finds and returns address of the first native instruction 
 * (IP) for a given Java byte code's program counter (PC).
 *
 * @param jit - JIT handle
 * @param method - method handle
 * @param bc_pc - byte code program counter
 * @param pip - to store the found IP
 */
void rt_bc2native(JIT_Handle jit, Method_Handle method, 
                  unsigned short bc_pc, void ** pip);

/// @} // ~ JITRINO_JET_RUNTIME_GENERAL


/**
 * @ingroup JITRINO_JET_RUNTIME_SUPPORT
 * @defgroup JITRINO_JET_JVMTI_SUPPORT JVMTI support
 * @{
 */

/**
 * @brief Gets the given local variable with specified index and type.
 * @returns EXE_ERROR_NONE if the access was performed, or corresponding 
 *          error code (i.e. method was not compiled by the Jitrino.Jet).
 */
::OpenExeJpdaError rt_get_local_var(JIT_Handle jit, Method_Handle method,
                                    const ::JitFrameContext *context,
                                    unsigned  var_num, 
                                    VM_Data_Type var_type,
                                    void *value_ptr);
/**
* @brief Sets the given local variable with specified index and type.
* @returns EXE_ERROR_NONE if the access was performed, or corresponding 
* error code (i.e. method was not compiled by the Jitrino.Jet).
*/
::OpenExeJpdaError rt_set_local_var(JIT_Handle jit, Method_Handle method,
                                    const ::JitFrameContext *context,
                                    unsigned var_num,
                                    VM_Data_Type var_type,
                                    void *value_ptr);

/// @} // ~ JITRINO_JET_JVMTI_SUPPORT

/**
 * @brief Initialization routine, normally called from the JIT_init().
 *
 * @param hjit - JIT handle passed from VM to JIT_init()
 * @param name - name assigned to JIT
 */
void setup(JIT_Handle hjit, const char* name);

/**
 * @brief Cleanup routine, normally called from the JIT_deinit().
 */
void cleanup(void);

/**
 * @brief Command line processing routine, normally called from 
 *        JIT_next_command_line_argument().
 * @param jit - JIT handle
 * @param name - name of the command line parameter
 * @param arg - value of the parameter
 */
void cmd_line_arg(JIT_Handle jit, const char * name, const char * arg);

/**
 * @brief Returns true if Jitrino.JET supports compressed references on 
 *        the current platform.
 */
bool supports_compresed_refs(void);

/**
 * @brief Performs compilation of the method.
 * 
 * Compiles the method with taking into account additional parameters 
 * (supposed to be use for JPDA purposes). 
 * @returns \b true if the method was compiled, \b false otherwise
 */
JIT_Result compile_with_params(JIT_Handle jh, Compile_Handle compile,
                               Method_Handle method, 
                               OpenMethodExecutionParams params);

/**
 * @brief Returns compilation capabilities.
 *
 * The function returns features that are supported by the Jitrino.JET 
 * compiler.
 * @return Supported features
 */
OpenMethodExecutionParams get_exe_capabilities();

/**
* @brief Notifies JET that profile is collected and counters could be removed 
* now.
*/
void rt_profile_notification_callback(JIT_Handle jit, PC_Handle pc, Method_Handle mh);

}}; // ~namespace Jitrino::Jet

#endif  // ~__JET_H_INCLUDED__
