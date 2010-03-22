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
 * @author Intel, Pavel Pervov
 */
#ifndef _VM_METHOD_ACCESS_H
#define _VM_METHOD_ACCESS_H

#include "common.h"
#include "platform_types.h"
#include "types.h"
#include "rt_types.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @file
 * Part of Class Support interface related to retrieving different
 * properties of methods contained in class.
 */

/**
 * Returns the method name.
 *
 * @param method - the method handle
 *
 * @return Method name bytes.
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code>.
 */
DECLARE_OPEN(const char*, method_get_name, (Method_Handle method));

/**
 * Returns the method descriptor.
 *
 * @param method - the method handle
 *
 * @return Method descriptor bytes.
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code>.
 */
DECLARE_OPEN(const char*, method_get_descriptor, (Method_Handle method));

/**
 * Returns the signature that can be used to iterate over the agruments of the given method
 * and to query the type of the method result.
 *
 * @param method - the method handle
 *
 * @return The method signature.
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code>. 
 */
DECLARE_OPEN(Method_Signature_Handle, method_get_signature, (Method_Handle method));

/**
 * Returns the class where the given method is declared.
 *
 * @param method - the method handle
 *
 * @return The name of the class where the method is declared.
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code>. 
 */
DECLARE_OPEN(Class_Handle, method_get_class, (Method_Handle method));

/**
 * Acquires lock associated with a method
 *
 * @param method - the method handle
*/
DECLARE_OPEN(void, method_lock, (Method_Handle method));

/**
 * Releases lock associated with a method
 *
 * @param method - the method handle
*/
DECLARE_OPEN(void, method_unlock, (Method_Handle method));

/**
 *  Checks whether the given method is private.
 *
 * @param method - the method handle
 *
 * @return <code>TRUE</code> if the given method is private; otherwise, <code>FALSE</code>.
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code>. 
 * @ingroup Extended 
 */
DECLARE_OPEN(BOOLEAN, method_is_private, (Method_Handle method));

/**
 *  Checks whether the given method is protected.
 *
 * @param method - the method handle
 *
 * @return <code>TRUE</code> if the given method is protected; otherwise, <code>FALSE</code>.
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code>. 
 * @ingroup Extended 
 */
DECLARE_OPEN(BOOLEAN, method_is_protected, (Method_Handle method));

/**
 *  Checks whether the given method is static.
 *
 * @param method - the method handle
 *
 * @return <code>TRUE</code> if the given method is static; otherwise, <code>FALSE</code>.
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code>.
 * @ingroup Extended
 */
DECLARE_OPEN(BOOLEAN, method_is_static, (Method_Handle method));

/**
 *  Checks whether the given method is native.
 *
 * @param method - the method handle
 *
 * @return <code>TRUE</code> if the given method is native; otherwise, <code>FALSE</code>.
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code>. 
* @ingroup Extended 
 */
DECLARE_OPEN(BOOLEAN, method_is_native, (Method_Handle method));

/**
 *  Checks whether the given method is synchronized.
 *
 * @param method - the method handle
 *
 * @return <code>TRUE</code> if the given method is synchronized; otherwise, <code>FALSE</code>.
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code>. 
* @ingroup Extended 
 */
DECLARE_OPEN(BOOLEAN, method_is_synchronized, (Method_Handle method));

/**
 *  Checks whether the given method is final.
 *
 * @param method - the method handle
 *
 * @return <code>TRUE</code> if the given method is final; otherwise, <code>FALSE</code>.
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code>. 
* @ingroup Extended 
 */
DECLARE_OPEN(BOOLEAN, method_is_final, (Method_Handle method));

/**
 *  Checks whether the given method is abstract.
 *
 * @param method - the method handle
 *
 * @return <code>TRUE</code> if the given method is abstract; otherwise, <code>FALSE</code>.
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code>. 
* @ingroup Extended 
 */
DECLARE_OPEN(BOOLEAN, method_is_abstract, (Method_Handle method));

/**
 *  Checks whether the given method is strict.
 *
 * Java* methods can have a flag set to indicate that floating point operations
 * must be performed in the strict mode.
 *
 * @param method - the method handle
 *
 * @return <code>TRUE</code> if the <code>ACC_STRICT</code> flag is set for 
 *                the Java* method and <code>FALSE</code> if otherwise.
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code>. 
 * @ingroup Extended
 */
DECLARE_OPEN(BOOLEAN, method_is_strict, (Method_Handle method));

/**
 *  Checks whether the given method has been overridden in a subclass.
 *
 * @param method - the method handle
 *
 * @return <code>TRUE</code> if the method has been overriden and <code>FALSE</code> otherwise.
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code>. 
 *
 * @note If this function returns <code>FALSE</code>, loading the subclass later 
 *             in the execution of the program may invalidate this condition.
 *              If a JIT compiler uses this function to perform  unconditional inlining, 
 *              the compiler must be prepared to patch the code later. 
 *
 * @see vm_register_jit_overridden_method_callback
 */
DECLARE_OPEN(BOOLEAN, method_is_overridden, (Method_Handle method));

/**
 * Checks whether the JIT compiler is allowed to in-line the method.
 * 
 * The compiler can be set not to in-line a method for various reasons, for example, 
 * the JIT must not in-line native methods and Java* methods
 * loaded by a different class loader than the caller.
 *
 * @param method - the method handle
 *
 * @return <code>TRUE</code> if the JIT must not in-line the method; otherwise, <code>FALSE</code>.
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code>. 
 * @note Always <code>FALSE</code> for Java*.
 */
DECLARE_OPEN(BOOLEAN, method_is_no_inlining, (Method_Handle method));

/**
 * FIXME: NOCOMMENT
 */
DECLARE_OPEN(BOOLEAN, method_has_annotation, (Method_Handle mh, Class_Handle antn_type));

/**
 * Retrieves potential side effects of the given method. 
 *
 * @param method - handle for the method, for which side effects are retrieved
 *
 * @return Enumeration value which corresponds to side effects method may have.
 * One of:
 *      MSE_Unknown         - it is not known whether this method has side effects
 *      MSE_True            - method has side effects
 *      MSE_False           - method does not have side effects
 *      MSE_True_Null_Param - 
 */
DECLARE_OPEN(Method_Side_Effects, method_get_side_effects, (Method_Handle method));

/**
 * Sets potential side effects of the given method. 
 *
 * @param m - method handle for which side effects are set
 * @param mse - side effect of this method (see method_get_side_effects for details)
 */
DECLARE_OPEN(void, method_set_side_effects, (Method_Handle m, Method_Side_Effects mse));

/**
 * Sets the number of exception handlers in the code generated by the JIT compiler 
 * <i>jit</i> for the given method. 
 *
 * @param method        - the method handle
 * @param jit           - the JIT handle
 * @param num_handlers  - the number of exception handlers
 *
 * @note The JIT compiler must then call the <i>method_set_target_handler_info</i> function
 *             for each of the <i>num_handlers</i> exception handlers.
 */
DECLARE_OPEN(void, method_set_num_target_handlers,
    (Method_Handle method, JIT_Handle jit, unsigned num_handlers));

/**
 * Set the information about an exception handler in the code generated by
 * the JIT.
 */
DECLARE_OPEN(void, method_set_target_handler_info,
    (Method_Handle method, JIT_Handle j, unsigned eh_number,
     void* start_ip, void* end_ip, void* handler_ip,
     Class_Handle catch_cl, Boolean exc_obj_is_dead));

/**
 * Returns the handle for an accessible method overriding <i>method</i> in <i>klass</i> 
 * or in its closest superclass that overrides <i>method</i>.
 *
 * @param klass     - the class handle
 * @param method    - the method handle
 *
 * @return The method handle for an accessible method overriding <i>method</i>; otherwise, <code>FALSE</code>.
 */
DECLARE_OPEN(Method_Handle, method_get_overriding_method,
    (Class_Handle klass, Method_Handle method));

/**
 * Returns the number of arguments defined for the method.
 * This number automatically includes the pointer to this (if present).
 *
 * @param method_signature  - method signature handle
 *
 * @return The number of arguments defined for the method.
 */
DECLARE_OPEN(unsigned char, method_args_get_number, (Method_Signature_Handle method_signature));

/**
 * Returns the number of method exception handlers.
 *
 * @param method    - the method handle
 *
 * @return The number of method exception handlers.
 *
 * @note An assertion is raised if method equals to <code>NULL</code>. 
 * @note Replaces method_get_num_handlers function.
 */
DECLARE_OPEN(uint16, method_get_exc_handler_number, (Method_Handle method));

/**
 * Obtains the method exception handle information.
 *
 * @param method     - the method handle
 * @param index      - the exception handle index number
 * @param start_pc   - the resulting pointer to the exception handle start program count
 * @param end_pc     - the resulting pointer to the exception handle end program count
 * @param handler_pc - the resulting pointer to the exception handle program count
 * @param catch_type - the resulting pointer to the constant pool entry index
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code> or
 *             if the exception handle index is out of range or if any pointer equals to <code>NULL</code>. 
 * @note Replaces the method_get_handler_info function.
 */
DECLARE_OPEN(void, method_get_exc_handler_info,
    (Method_Handle method, uint16 index,
     uint16* start_pc, uint16* end_pc, uint16* handler_pc, uint16* catch_type));

/**
 * Returns type information for the return value.
 *
 * @param method_signature  - the method signature handle
 *
 * @return The type information for the return value.
 */
DECLARE_OPEN(Type_Info_Handle, method_ret_type_get_type_info,
    (Method_Signature_Handle method_signature));

/**
 * Returns type information for the given argument.
 *
 * @param method_signature  - the method signature handle
 * @param index             - the argument index
 *
 * @return Type information for the argument.
 */
DECLARE_OPEN(Type_Info_Handle, method_args_get_type_info,
    (Method_Signature_Handle method_signature, unsigned index));

/**
 * Returns the method bytecode size.
 *
 * @param method - the method handle
 *
 * @return The method bytecode size.
 */
DECLARE_OPEN(U_32, method_get_bytecode_length, (Method_Handle method));

/**
 * Returns the method bytecode array.
 *
 * @param method - the method handle
 *
 * @return The method bytecode array.
 *
 * @note An assertion is raised if the method equals to <code>NULL</code>. 
 */
DECLARE_OPEN(const U_8*, method_get_bytecode, (Method_Handle method));

/**
 * Returns StackMapTable attribute.
 * Parameter <i>hmethod</i> must not be equal to <code>NULL</code>.
 * If parameter <i>index</i> is out of range, returns <code>NULL</code>.
 *
 * @param hmethod  - method handle
 *
 * @return StackMapTable bytes
 */
DECLARE_OPEN(U_8*, method_get_stackmaptable, (Method_Handle hmethod));

/**
 * Returns the maximum number of local variables for the given method.
 *
 * @param method - the method handle
 *
 * @return The maximum number of local variables for the method.
 *
 * @note An assertion is raised if the method equals to <code>NULL</code>. 
 * @note Replaces functions method_get_max_local and method_vars_get_number.
 */
DECLARE_OPEN(U_32, method_get_max_locals, (Method_Handle method));

/**
 * Returns the maximum stack depth for the given method.
 *
 * @param method - the method handle
 *
 * @return The maximum stack depth for the method.
 *
 * @note An assertion is raised if <i>method</i> equals to <code>NULL</code>. 
 */
DECLARE_OPEN(uint16, method_get_max_stack, (Method_Handle method));

/**
 * Returns the offset from the start of the vtable to the entry for the given method
 *
 * @param method - the method handle
 *
 * @return The offset from the start of the vtable to the entry for the method, in bytes. 
 */
DECLARE_OPEN(unsigned, method_get_offset, (Method_Handle method));

/**
 * Gets the address of the code pointer for the given method.
 * 
 * A straight-forward JIT compiler that does not support recompilation can only generate
 * code with indirect branches through the address provided by this function.
 *
 * @param method - the method handle
 *
 * @return the address where the code pointer for a given method is.
 *
 *  @see vm_register_jit_recompiled_method_callback
 */
DECLARE_OPEN(void*, method_get_indirect_address, (Method_Handle method));

/**
* Looks for a method in native libraries of a class loader.
*
* @param[in] method - a searching native-method structure
* @return The pointer to found a native function.
* @note The function raises <code>UnsatisfiedLinkError</code> with a method name
*       in an exception message, if the specified method is not found.*/
DECLARE_OPEN(void*, method_get_native_func_addr, (Method_Handle method));

/**
 * Allocates an information block for the given method.
 * 
 * An <i>info block</i> can be later retrieved by the JIT compiler for various operations. 
 * For example, the JIT can store GC maps for root set enumeration and stack
 * unwinding in the onfo block.
 *
 * @param method    - the method handle
 * @param jit       - the JIT handle
 * @param size      - the size of the allocated code block
 *
 * @return The pointer to the allocated info block.
 *
 * @see method_allocate_data_block
 */
DECLARE_OPEN(U_8*, method_allocate_info_block,
    (Method_Handle method, JIT_Handle jit, size_t size));

/**
 * Allocates the "read-write" data block for the given method.
 *
 * This block is intended for data that may be required during program execution, 
 * for example, tables for switch statements. This block cannot be retrieved later. 
 *
 * Separation of data allocated by <i>method_allocate_data_block</i> and
 * <i>method_allocate_info_block</i> may help improve locality of references
 * to data accessed during execution of compiled code and data accessed during
 * stack unwinding.
 *
 * @param method    - the method handle
 * @param jit       - the JIT handle
 * @param size      - the size of the allocated code block
 * @param alignment - the memory block aligment
 *
 * @return The pointer to the allocated data block.
 *
 * @note FIXME This has to go to the compilation infrastructure interface.
 *
 * @see method_allocate_info_block
 */
DECLARE_OPEN(U_8*, method_allocate_data_block,
    (Method_Handle method, JIT_Handle jit, size_t size, size_t alignment));

/**
 * Allocated a "read-only" data block.
 *
 * This function is deprecated. In all new code, use
 * method_allocate_data_block() only.  At some point, we will revisit
 * this interface to have more control over the layout of various
 * memory blocks allocated by the VM.
 */
DECLARE_OPEN(U_8*, method_allocate_jit_data_block,
    (Method_Handle method, JIT_Handle jit, size_t size, size_t alignment));

/**
 * Enables allocation of multiple chunks of code with different heat values. 
 * 
 * The JIT compiler is responsible for specifying  unique IDs of code chunks within one method.
 * The first instruction of the chunk with id=0 is the entry point of the method.
 *
 * @param method    - the method handle
 * @param jit       - the JIT handle
 * @param size      - the size of the allocated code block
 * @param alignment - the memory block aligment
 * @param heat      - ?
 * @param id        - code chunk id
 * @param action    - the resulting return action
 *
 * @return The pointer to the allocated code block with the following specifics: 
 *                 <ul>
 *                     <li>If the CAA_Allocate argument is specified, memory is allocated and the function returns the pointer
 *                          to this memory.
 *                     <li>If the CAA_Simulate argument is specified, no memory is allocated and the function returns the address 
 *                         that would have been allocated if CAA_Allocate was specified and all the other arguments were the same. 
 *                 </ul>
 *                 The function may also return <code>NULL</code> when CAA_Simulate is specified. For example, this may happen
 *                 when multiple heat values are mapped to the same code pool or when the specified size require a new code pool.
 *
 * @note FIXME This has to go to the compilation infrastructure interface. 
 */
DECLARE_OPEN(U_8*, method_allocate_code_block,
    (Method_Handle method, JIT_Handle jit, size_t size, size_t alignment,
     CodeBlockHeat heat, int id, Code_Allocation_Action action));

/**
 * Retrieve the memory block allocated earlier by
 * method_allocate_code_block().
 * A triple <method, jit, id> uniquely identifies a code block.
 *
 * @param method - the method handle
 * @param j - the JIT handle
 * @param id - code block ID
 *
 * @return address of the requested code block
 */
DECLARE_OPEN(U_8*, method_get_code_block_jit,
    (Method_Handle method, JIT_Handle j));/*, int id));*/

/**
 * Get the size of the memory block allocated earlier by
 * method_allocate_code_block().
 * A triple <method, jit, id> uniquely identifies a code block.
 *
 * @param method - the method handle
 * @param j - the JIT handle
 * @param id - code block ID
 *
 * @return size of the requested code block
 */
DECLARE_OPEN(unsigned, method_get_code_block_size_jit,
    (Method_Handle method, JIT_Handle j));/*, int id));*/


/**
 * Retrieves the memory block allocated earlier by the
 * <i>method_allocate_info_block</i> function.
 * The pair of parameters <method, jit> uniquely identifies the JIT compiler information block.
 *
 * @param method    - the method handle
 * @param jit       - the JIT handle
 *
 * @return The pointer to the allocated info block.
 */
DECLARE_OPEN(U_8*, method_get_info_block_jit, (Method_Handle method, JIT_Handle jit));

/**
 * Get the size of the memory block allocated earlier by
 * method_allocate_info_block().
 *
 * FIXME: NOCOMMENT
 */
DECLARE_OPEN(unsigned, method_get_info_block_size_jit, (Method_Handle method, JIT_Handle jit));

}

#endif // _VM_METHOD_ACCESS_H
