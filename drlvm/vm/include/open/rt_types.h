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

#ifndef _RUNTIME_TYPES_H_
#define _RUNTIME_TYPES_H_

#include "open/platform_types.h"

/**
* One entry of correspondence table between native addresses and bytecode
* locations.
*/
typedef struct AddrLocation {
    /** Native code address. */
    void* start_addr;

    /** Bytecode location. */
    uint16 location;
} AddrLocation;


/**
* Elements of this struct correspond to certain requirements
* of how a managed method is executed (what it additionally does
* during execution). Most of them correspond to requirements to
* call a certain VM helpers at certain places in the code. For JIT,
* in particular, this means that it will have to generate additional
* code which will perform these calls.
* <p>
* Each of the requirement is associated with a corresponding ability of
* the EE to satisfy this requirement. So, elements of the struct should also
* be used to denote EE capabilities related to method execution.
* <p>
* If an element corresponds to a certain VM helper, concrete contract
* of calling this helper (arguments, etc.) can be found at the place of
* definition of this helper (or its ID) within present OPEN specification.
*/
typedef struct OpenMethodExecutionParams {
    /** call corresponding VM helper upon entry to the managed method */
    Boolean  exe_notify_method_entry : 1;

    /** call corresponding VM helper upon exit from the managed method */
    Boolean  exe_notify_method_exit : 1;

    /** call corresponding VM helper upon reading a value of a field which has <field access mask> set */
    Boolean  exe_notify_field_access  : 1;

    /** call corresponding VM helper upon setting a value of a field which has <field modification mask> set */
    Boolean  exe_notify_field_modification : 1;

    /**
    * call corresponding VM helper upon exception throw,
    * if by default the throw code does not enter any VM helper
    * (for example, in case of JIT optimizations)
    */
    Boolean  exe_notify_exception_throw : 1;

    /**
    * call corresponding VM helper upon exception catch,
    * if by default the exception propagation code does not enter any VM helper
    * (for example, in case of JIT optimizations)
    */
    Boolean  exe_notify_exception_catch : 1;

    /**
    * call corresponding VM helper upon entering a monitor,
    * if by default the monitor enter code does not enter any VM helper
    * (for example, in case of JIT optimizations)
    */
    Boolean  exe_notify_monitor_enter : 1;

    /**
    * call corresponding VM helper upon exiting a monitor,
    * if by default the monitor exit code does not enter any VM helper
    * (for example, in case of JIT optimizations)
    */
    Boolean  exe_notify_monitor_exit : 1;

    /**
    * call corresponding VM helper upon entering a contended monitor,
    * if by default the contended monitor enter code does not enter any VM helper
    * (for example, in case of JIT optimizations)
    */
    Boolean  exe_notify_contended_monitor_enter : 1;

    /**
    * call corresponding VM helper upon exiting a contended monitor,
    * if by default the contended monitor exit code does not enter any VM helper
    * (for example, in case of JIT optimizations)
    */
    Boolean  exe_notify_contended_monitor_exit : 1;

    /** perform method in-lining during compilation (JIT-specific) */
    Boolean  exe_do_method_inlining : 1;

    /**
    * Keep correspondence between bytecode offsets and native instruction IPs (JIT-specific).
    * For a JIT this, in particular, means that it should not do any optimizations which
    * may hinder this mapping. It should also store the map after method compilation so that
    * later VM could use appropriate ExeJPDA interfaces to retrieve the mapping.
    */
    Boolean  exe_do_code_mapping : 1;

    /**
    * Keep correspondence between bytecode local variables and locations of the
    * native operands (JIT-specific) in relevant locations within method code.
    * For a JIT this, in particular, means that it should not do any optimizations
    * which may hinder this mapping. It should also store the map after method compilation
    * so that later VM could use appropriate ExeJPDA interfaces to retrieve the mapping.
    */
    Boolean  exe_do_local_var_mapping : 1;

    /** call corresponding VM helper upon setting a value of any field of reference type */
    Boolean  exe_insert_write_barriers : 1;

    /**
    * Provide possibility to obtain reference to the current 'this' object by
    * means of get_address_of_this method. Used for JVMTI debug support.
    */
    Boolean  exe_provide_access_to_this : 1;

    /**
    * Provide restoring of arguments in the stack after the call
    * of the unwind_frame method so that method could be called again
    * with the same arguments. Used for JVMTI debug support.
    */
    Boolean  exe_restore_context_after_unwind : 1;

    /**
    * Sent CompileMethodLoad event when a method is compiled and loaded into memory 
    */
    Boolean  exe_notify_compiled_method_load : 1;

} OpenMethodExecutionParams;


    ///////////////////////////////////////////////////////
    // begin Frame Contexts for JITs

#ifdef _IPF_

    // Note that the code in transfer context is very depend upon the ordering of fields in this structure.
    // Be very careful in changing this structure.
    typedef
    struct JitFrameContext {
        uint64 *p_ar_pfs;
        uint64 *p_eip;
        uint64 sp;
        uint64 *p_gr[128];
        uint64 *p_fp[128];
        uint64 preds;
        uint64 *p_br[8];
        uint64 nats_lo;
        uint64 nats_hi;
        Boolean is_ip_past;
        uint64 ar_fpsr;
        uint64 ar_unat;
        uint64 ar_lc;
    } JitFrameContext; //JitFrameContext

#elif defined _EM64T_

    typedef
    struct JitFrameContext {
        uint64   rsp;
        uint64 * p_rbp;
        uint64 * p_rip;

        // Callee-saved registers
        uint64 * p_rbx;
        uint64 * p_r12;
        uint64 * p_r13;
        uint64 * p_r14;
        uint64 * p_r15;

        // The scratch registers are currently only valid during GC enumeration.
        uint64 * p_rax;
        uint64 * p_rcx;
        uint64 * p_rdx;
        uint64 * p_rsi;
        uint64 * p_rdi;
        uint64 * p_r8;
        uint64 * p_r9;
        uint64 * p_r10;
        uint64 * p_r11;

        // To restore processor flags during transfer
        U_32 eflags;

        Boolean is_ip_past;
    } JitFrameContext;

#else // "_IA32_"

    typedef
    struct JitFrameContext {
        U_32 esp;
        U_32 *p_ebp;
        U_32 *p_eip;

        // Callee-saved registers
        U_32 *p_edi;
        U_32 *p_esi;
        U_32 *p_ebx;

        // The scratch registers are currently only valid during GC enumeration.
        U_32 *p_eax;
        U_32 *p_ecx;
        U_32 *p_edx;

        // To restore processor flags during transfer
        U_32 eflags;

        Boolean is_ip_past;
    } JitFrameContext;

#endif // "_IA32_"

    // end Frame Contexts for JITs
    ///////////////////////////////////////////////////////

typedef void * InlineInfoPtr;
typedef void * Compile_Handle; //deprecated??
  /**
   * The handle to the JIT instance. 
   */
typedef void *JIT_Handle;


typedef
    enum Method_Side_Effects {
        MSE_Unknown,
        MSE_True,
        MSE_False,
        MSE_True_Null_Param
    } Method_Side_Effects;

/**
* @sa method_allocate_code_block
*/

typedef enum Code_Allocation_ActionEnum {
    CAA_Simulate,
    CAA_Allocate
}Code_Allocation_Action;

//
// Code block heat - used when a method is split into hot and cold parts
//
typedef enum {
    CodeBlockHeatMin,
    CodeBlockHeatDefault,
    CodeBlockHeatMax
} CodeBlockHeat;


#endif // !_RUNTIME_TYPES_H_
