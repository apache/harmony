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
#ifndef _VM_EE_H_
#define _VM_EE_H_

/**
 * @file
 * 
 *
 */
#include "open/types.h"
#include "open/rt_types.h"
#include "open/em.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
* Acquires lock associated with method
*/
DECLARE_OPEN(void, method_lock, (Method_Handle mh));

/*
* Releases lock associated with method
*/
DECLARE_OPEN(void, method_unlock, (Method_Handle mh));

/**
* Looks for a method in native libraries of a class loader.
*
* @param[in] method - a searching native-method structure
* @return The pointer to found a native function.
* @note The function raises <code>UnsatisfiedLinkError</code> with a method name
*       in an exception message, if the specified method is not found.*/
DECLARE_OPEN(void *, method_get_native_func_addr, (Method_Handle method));

/**
* Address of the memory location containing the address of the code.
* Used for static and special methods which have been resolved but not jitted. (FIXME??)
* The call would be:
*      call dword ptr [addr]
*
* @return The address where the code pointer for a given method is.
* 
* A simple JIT that doesn't support recompilation (see e.g. 
* <code>vm_register_jit_recompiled_method_callback</code>) can only 
* generate code with indirect branches through the address provided 
* by method_get_indirect_address().
*/
DECLARE_OPEN(void *, method_get_indirect_address, (Method_Handle method));

/**
* @return The offset in bytes from the start of the vtable to the entry for
*         a given method.
*/
DECLARE_OPEN(size_t, method_get_vtable_offset, (Method_Handle method));

/**
* Allocate the "read-write" data block for this method. This memory block
* cannot be retrieved later. The intention is to use the data block for data
* that may be needed during the program execution (e.g. tables for
* switch statements).
*
* Separation of data allocated by method_allocate_data_block() and
* method_allocate_info_block() may help improve locality of 
* references to data accessed during execution of compiled code and data 
* accessed during stack uwinding.
*
* @sa method_allocate_info_block
*/
DECLARE_OPEN(U_8*, method_allocate_data_block, (Method_Handle method,
                                          JIT_Handle j,
                                          size_t size,
                                          size_t alignment));

/**
* Allocated a "read-only" data block.
*
* (? 20030314) This function is deprecated. In all new code, use
* method_allocate_data_block() only. At some point, we 
* will revisit this interface to have more control over the layout 
* of various memory blocks allocated by the VM.
*/

DECLARE_OPEN(U_8*, method_allocate_jit_data_block, (Method_Handle method,
                                              JIT_Handle j,
                                              size_t size,
                                              size_t alignment));


/**
* This function allows allocation of multiple chunks of code with different
* heat values. The JIT is responsible for specifying ids that are unique
* within the same method.
* The first instruction of the chunk with <code>id=0</code> is the entry point 
* of the method.
* 
* Deprecated.
*
* @return If the <code>CAA_Allocate</code> argument is specified, memory is 
*         allocated and a pointer to it is returned. If the 
*         <code>CAA_Simulate</code> argument is specified, no memory is
*         allocated - the same as pass parameter size = 0 - function returns 
*         only current address for allocation in pool but no memory is allocated.  
*/
DECLARE_OPEN(U_8*,
method_allocate_code_block, (Method_Handle m,
                           JIT_Handle j,
                           size_t size,
                           size_t alignment,
                           CodeBlockHeat heat,
                           int id,
                           Code_Allocation_Action action));


/**
* Allocate an info block for this method. An info block can be later
* retrieved by the JIT. The JIT may for instance store GC maps for
* root set enumeration and stack unwinding in the onfo block.
*
* @sa method_allocate_data_block
*/
DECLARE_OPEN(U_8*, method_allocate_info_block, (Method_Handle method,
                                          JIT_Handle j,
                                          size_t size));


/**
* Retrieve the memory block allocated earlier by 
* method_allocate_code_block().
* A pair <code><method, jit></code> uniquely identifies a code block.
*/
DECLARE_OPEN(U_8*, method_get_code_block_addr_jit, (Method_Handle method,
                                              JIT_Handle j));

/**
* Get the size of the memory block allocated earlier by
* method_allocate_code_block().
*/
DECLARE_OPEN(unsigned, method_get_code_block_size_jit, (Method_Handle method,
                                                 JIT_Handle j));

/**
* Retrieve the memory block allocated earlier by
* method_allocate_code_block().
* A triple <code><method, jit, id></code> uniquely identifies a 
* code block.
*/
DECLARE_OPEN(U_8*, method_get_code_block_addr_jit_new, (Method_Handle method,
                                                  JIT_Handle j,
                                                  int id));

/**
* Get the size of the memory block allocated earlier by
* method_allocate_code_block().
* A triple <code><method, jit, id></code> uniquely identifies a 
* code block.
*/
DECLARE_OPEN(unsigned, method_get_code_block_size_jit_new, (Method_Handle method,
                                                     JIT_Handle j,
                                                     int id));

/**
* Retrieve the memory block allocated earlier by 
* method_allocate_info_block().
* A pair <code><method, jit></code> uniquely identifies a JIT info block.
*/ 
DECLARE_OPEN(U_8*, method_get_info_block_jit, (Method_Handle method,
                                         JIT_Handle j));

/**
* Get the size of the memory block allocated earlier by
* method_allocate_info_block().
*/

DECLARE_OPEN(unsigned, method_get_info_block_size_jit, (Method_Handle method,
                                                 JIT_Handle j));

/**
 * Called by a JIT in order to be notified whenever the vtable entries for the 
 * given method are changed. This could happen, e.g., when a method is first 
 * compiled, or when it is recompiled. The <code>callback_data</code> pointer 
 * will be passed back to the JIT during the callback. The callback method is 
 * <code>JIT_recompiled_method_callback</code>.
 */
DECLARE_OPEN(void, vm_register_jit_recompiled_method_callback, (JIT_Handle jit, 
                                Method_Handle method, 
                                Method_Handle caller, 
                                void *callback_data));

/**
 * Called by a JIT to have the VM replace a section of executable code in a 
 * thread-safe fashion. This function does not synchronize the I- or D-caches. 
 * It may be a lot cheaper to batch up the patch requests, so we may need to 
 * extend this interface.
 */
DECLARE_OPEN(void, vm_patch_code_block, (U_8* code_block, U_8* new_code, size_t size));

/** 
 * Called by a JIT to have VM synchronously (in the same thread) compile a method
 * It is a requirement that JIT calls this routine only during compilation of 
 * other method, not during run-time.
 */
DECLARE_OPEN(JIT_Result, vm_compile_method, (JIT_Handle jit, Method_Handle method));


/**
* Adds information about inlined method.
* @param[in] method - method which is inlined
* @param[in] codeSize - size of inlined code block
* @param[in] codeAddr - size of inlined code block
* @param[in] mapLength - number of AddrLocation elements in addrLocationMap
* @param[in] addrLocationMap - native addresses to bytecode locations
*       correspondence table
* @param[in] compileInfo - VM specific information.
* @param[in] outer_method - target method to which inlining was made
*/
DECLARE_OPEN(void, vm_compiled_method_load, (Method_Handle method, U_32 codeSize, 
                                            void* codeAddr, U_32 mapLength, 
                                            AddrLocation* addrLocationMap, 
                                            void* compileInfo, Method_Handle outer_method));


DECLARE_OPEN(Method_Side_Effects, method_get_side_effects, (Method_Handle mh));
DECLARE_OPEN(void, method_set_side_effects, (Method_Handle mh, Method_Side_Effects mse));

//Has side effect: initiates classloading of classes annotating the method, if any.
DECLARE_OPEN(BOOLEAN, method_has_annotation, (Method_Handle target, Class_Handle antn_type));
/**
* Set the number of exception handlers in the code generated by the JIT 
* <code>j</code> for a given method. The JIT must then 
* call method_set_target_handler_info()
* for each of the num_handlers exception handlers.
*/
DECLARE_OPEN(void, method_set_num_target_handlers, (Method_Handle method,
                                             JIT_Handle j,
                                             unsigned num_handlers));

/**
* Set the information about an exception handler in the code generated by
* the JIT.
*/
DECLARE_OPEN(void, method_set_target_handler_info, (Method_Handle method,
                                             JIT_Handle j,
                                             unsigned      eh_number,
                                             void         *start_ip,
                                             void         *end_ip,
                                             void         *handler_ip,
                                             Class_Handle  catch_cl,
                                             Boolean       exc_obj_is_dead));

//-----------------------------------------------------------------------------
// Constant pool resolution
//-----------------------------------------------------------------------------
//
// The following byte codes reference constant pool entries:
//
//  field
//      getstatic           static field
//      putstatic           static field
//      getfield            non-static field
//      putfield            non-static field
//
//  method
//      invokevirtual       virtual method
//      invokespecial       special method
//      invokestatic        static method
//      invokeinterface     interface method
//
//  class
//      new                 class
//      anewarray           class
//      checkcast           class
//      instanceof          class
//      multianewarray      class
//

//
// For the method invocation byte codes, certain linkage exceptions are thrown 
// at run-time:
//
//  (1) invocation of a native methods throws the UnsatisfiedLinkError if the 
//      code that implements the method cannot be loaded or linked.
//  (2) invocation of an interface method throws 
//          - IncompatibleClassChangeError if the object does not implement 
//            the called method, or the method is implemented as static
//          - IllegalAccessError if the implemented method is not public
//          - AbstractMethodError if the implemented method is abstract
//

/** 
* @name Resolution-related functions
*/
//@{

/**
* Resolve a reference to a non-static field.
* The <code>idx</code> parameter is interpreted as a constant pool index for JVM.
* Used for getfield and putfield in JVM.
*/
DECLARE_OPEN(Field_Handle, 
resolve_nonstatic_field, (Compile_Handle h, Class_Handle ch, unsigned idx, unsigned putfield));

/**
* Resolve constant pool reference to a static field.
* The <code>idx</code> parameter is interpreted as a constant pool index for JVM.
* Used for getstatic and putstatic in JVM.
*/
DECLARE_OPEN(Field_Handle,
resolve_static_field, (Compile_Handle h, Class_Handle ch, unsigned idx, unsigned putfield));

/**
* Resolve a method.
* The <code>idx</code> parameter is interpreted as a constant pool index for JVM.
*/ 
DECLARE_OPEN(Method_Handle, 
resolve_method, (Compile_Handle h, Class_Handle ch, unsigned idx));


/**
* Resolve a method. Same as resolve_method() but the VM checks 
* that the method can be used for a virtual dispatch.
* The <code>idx</code> parameter is interpreted as a constant pool index for JVM.
*/
DECLARE_OPEN(Method_Handle,
resolve_virtual_method, (Compile_Handle h, Class_Handle c, unsigned index));

/**
* Resolve a method. Same as resolve_method() but the VM checks 
* that the method is static (i.e. it is not an instance method).
* The <code>idx</code> parameter is interpreted as a constant pool index for 
* JVM.
*/
DECLARE_OPEN(Method_Handle, 
resolve_static_method, (Compile_Handle h, Class_Handle c, unsigned index));

/** 
* Resolve a method. Same as resolve_method() but the VM checks 
* that the method is declared in an interface type.
* The <code>idx</code> parameter is interpreted as a constant pool index for JVM.
*/
DECLARE_OPEN(Method_Handle, 
resolve_interface_method, (Compile_Handle h, Class_Handle c, unsigned index));

//
// resolve constant pool reference to a virtual method
// used for invokespecial
//
DECLARE_OPEN(Method_Handle, 
resolve_special_method, (Compile_Handle h, Class_Handle c, unsigned index));

//@}

//
// resolve constant pool reference to a class
// used for
//      (1) new 
//              - InstantiationError exception if resolved class is abstract
//      (2) anewarray
//      (3) multianewarray
//
// resolve_class_new is used for resolving references to class entries by the
// the new byte code.
//
/**
* Resolve a class and provide error checking if the class cannot have an
* instance, i.e. it is abstract (or is an interface class).
* The <code>idx</code> parameter is interpreted as a constant pool index for JVM.
*/
DECLARE_OPEN(Class_Handle, resolve_class_new, (Compile_Handle h, Class_Handle c, unsigned index));

//
// resolve_class is used by all the other byte codes that reference classes,
// as well as exception handlers.
//
DECLARE_OPEN(Class_Handle, resolve_class, (Compile_Handle h, Class_Handle c, unsigned index));

DECLARE_OPEN(U_32, class_get_depth, (Class_Handle cl));
/// Check if fast_instanceof is applicable for the class
DECLARE_OPEN(BOOLEAN, class_is_support_fast_instanceof, (Class_Handle cl));

/**
* @return The offset to the vtable pointer in an object.
*/ 
DECLARE_OPEN(size_t, object_get_vtable_offset, ());

/**
* @return The vtable handle of the given class.
*/
DECLARE_OPEN(VTable_Handle, class_get_vtable, (Class_Handle ch));

/**
* @return Class handle given object's <code>VTable_Handle</code>.
*/ 
DECLARE_OPEN(Class_Handle, vtable_get_class, (VTable_Handle vh));


/**
* @return The allocation handle to be used for the object allocation
*         routines, given a class handle.
*/
DECLARE_OPEN(Allocation_Handle, class_get_allocation_handle, (Class_Handle ch));

/**
* @return The class handle corresponding to a given allocation handle.
*/
DECLARE_OPEN(Class_Handle, allocation_handle_get_class, (Allocation_Handle ah));


/**
* Returns the address of the global flag that specifies whether
* MethodEntry event is enabled. JIT should call this function in case
* a method is compiled with exe_notify_method_entry flag set.
*/
DECLARE_OPEN(char *, get_method_entry_flag_address, ());

/**
* Returns the address of the global flag that specifies whether
* MethodExit event is enabled. JIT should call this function in case
* a method is compiled with exe_notify_method_exit flag set.
*/
DECLARE_OPEN(char *, get_method_exit_flag_address, ());


/**
* @return The address and bit mask, for the flag which determine whether field
*         access event should be sent. JIT may use the following expression to
*         determine if specified field access should be tracked:
*         ( **address & *mask != 0 )
*
* @param field         - handle of the field
* @param[out] address  - pointer to the address of the byte which contains the flag
* @param[out] mask     - pointer to the bit mask of the flag
*/
DECLARE_OPEN(void,
field_get_track_access_flag, (Field_Handle field, char** address, char* mask));

/**
* @return the address and bit mask, for the flag which determine whether field
*         modification event should be sent. JIT may use the following expression to
*         determine if specified field modification should be tracked:
*         ( **address & *mask != 0 )
*
* @param field         - handle of the field
* @param[out] address  - pointer to the address of the byte which contains the flag
* @param[out] mask     - pointer to the bit mask of the flag
*/
DECLARE_OPEN(void,
field_get_track_modification_flag, (Field_Handle field, char** address,
                                  char* mask));

#ifdef __cplusplus
}
#endif


#endif
