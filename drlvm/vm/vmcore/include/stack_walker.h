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
#ifndef _STACK_WALKER_H
#define _STACK_WALKER_H

/**
@file
\ref stack_walking

This module contains the interfaces to provide stack walking functionality.
Note, that this interfaces are common to all types of stack frames.
See specific modules for more on information on particular types
of stack frames, e.g. native_frame.h

*/

/**
@page stack_walking Stack Walking subsystem

    @section sw_introduction Introduction

The interface for stack walking is given in stack_walker.h.
Various types of the frames are discussed in \ref native_frame

    @section rationale Rationale

<UL>
<LI>each method invocation has an activation record
  (see programming tutorials on detailed explanation).
  We will refer activation records as "stack frames".

<LI>some of the stack frames are of interest to us,
  particularly those related to Java and having following 
  properties:
  <UL>
  <LI> enumerable
  <LI> unwindable
  <LI> security reportable (associated with java method)
  </UL>

<LI> there is a number of different types of stack frames,
  e.g. java method stack frames, JNI stack frames, VM
  internal stack frames. We want to treat them uniformly.  
  We particularly want to consider stack frames as
  entities, in order to avoid square growth of number
  of possible transition types.

<LI> different stack frames have different physical
  representation. For example, native frame is likely to
  be a C struct in memory, and jitted method frame may
  be represented as a just pair of values: stack pointer
  and instruction pointer. This non-uniformity of
  possible stack frames calls for a different ways of
  working with stack frames. The most natural way of
  representing the stack walking is the iterator.

<LI> as we want different stack frames to be represented
  uniformly, we opt for a iterator as a designated
  structure in memory. Thus we need to manage memory,
  which is used to store stack iterators. The currently
  used model is explicit memory management by
  si_create()/si_free() functions. This may be
  improved by using managed heap and handles.

<LI> a pointer to NativeFrame or InterpreterFrame
  is not a complete information about corresponding
  stack frame, as we need to know the owning execution
  engine as well. Thus, a designated stack iterator
  is useful for native and interpreted methods too.

<LI> Most of the information required to clients of stack walking
  is directly computable from IP, for example, method,
  owning execution engine, file name, line number.
  Other information, e.g. needed during unwinding
</UL>

    @section scenarios Usage scenarios.

<OL>
<LI> update_code_addresses_for_thread. (on-stack replacement?)

    In case a code block was moved, iterate over the thread stack frames
    and change the ip in each stack frame.

    \code
    StackIterator* iterator = si_create();
    while (!si_over(iterator)) {
        NativeCodePtr old_ip = si_get_ip(iterator);
        NativeCodePtr new_ip = update_address(old_ip);
        si_set_ip(iterator,new_ip);
        si_next(iterator);
    }
    si_free(iterator);
    \endcode

<LI> compile_rewrite_dcl{1,2} rewrite direct call

    \code
    StackIterator* si = si_create();
    si_next(si);
    assert(!si_over(si));
    Execution_Engine_Handle jit = si_get_owner(si);
    Method_Handle method = si_get_method(si);
    NativeCodePtr ret_ip = si_get_ip(si);
    si_free(si);

    jit->rewrite_direct_call(REWRITE_PATCH_CALLER, (Method_Handle)method, ret_ip, method->get_code_addr());
    \endcode

<LI> throw exception

    \code
    StackIterator* frame = si_create();
    while (!si_over(frame) && si_is_unwindable(frame)) {
        Execution_Engine_Handle ee = si_get_owner(frame);
        Pointer ip = si_get_ip(frame);
        for (handlers in ee_get_handlers(ee,frame)) { // XXX
            if (hanlder catches exception at ip) {
                Execution_Context* context = si_get_context(frame);
                ee_fix_context(ee,context,handler);
                si_free(frame);
                vm_transfer_control(context);
            }
        }
        si_unwind(frame);
    }

    if (si_over(frame)) {
        // uncaught exception
    } else if (!si_is_unwindable(frame)) {
        set_current_thread_exception(exception);
    }
    si_free(frame);
    \endcode

<LI> Stack trace construction (security or printStackTrace())

    \code
    StackIterator* frame = si_create();
    while (!si_over(frame)) {
        Method_Handle method = si_get_trace_method(frame);
        char filename[256];
        si_get_trace_file(frame,filename,sizeof(filename));
        int linenumber = si_get_trace_line(frame);

        // fill in some other structure
        si_next_trace(frame);
    }
    si_free(frame);
    \endcode

<LI> Root set enumeration

    \code
    StackIterator *frame = si_create();
    while (!si_over(frame)) {
        Execution_Engine_Handle ee = si_get_owner(frame);
        ee_enumerate(ee,frame);
        si_next(frame);
    }
    si_free(frame);
    \endcode

</OL>

    @section external_reqs External requirements

Only very rough list of required from EE functionality,
needs thorough work.

GC needs to
\li iterate over enumerable frames
\li enumerate each of them 

Security manager nees to
\li iterate over formal method activation records
\li get method info for each activation record

Stack trace constructor needs to
\li iterate over real or formal stack frames (depending on the context)
\li get method, file, line information for each frame

Unwinding needs to
\li unwind unwindable frame (with appropriate context propagation)
\li release locks associated with unwinded frame
\li modify and resume context of handler stack frame

The rough list of methods required from external components to accomplish
stack walking tasks follows.

\code
Method_Handle ee_get_method(Execution_Engine_Handle ee, NativeCodePtr ip);
Execution_Engine_Handle vm_get_owner(NativeCodePtr ip);
void ee_get_line_info(Execution_Engine_Handle ee, Method_Handle method, int *pline, char **pfile);
?? ee_get_arg_description(Execution_Engine_Handle ee, Method_Handle method, ??);
void ee_enumerate(Execution_Engine_Handle ee, Method_Handle method, void* info);
void ee_unwind(Execution_Engine_Handle ee, Method_Handle method, Execution_Context* context, void* info); //??
void ee_fix_context(Execution_Engine_Handle ee, Method_Handle method, Execution_Context* context, Exception e); //??
\endcode

    @section issues Issues

\li  use managed heap to avoid explicit si_free()
\li  should we expose the internal structure of StackIterator?
\li  what do we need si_transfer_all_preserved_registers() for?
\li  what do we need si_reload_registers() for?
\li  is_ip_past?
\li  CodeChunkInfo?
\li  jit->unwind and native frame interaction?
\li  formal and actual parameters access from stack frame?
\li  M2NFrame: ip, registers, method, object handles, link to next M2nFrame.
\li  what do we need si_dup() for: the only use in exn_propagate_exception()

    @section existing Existing interfaces

\code

struct StackIterator {
    CodeChunkInfo*    cci;
    JitFrameContext   c;
    M2nFrame*         m2nfl;
    U_32            ip;
};

StackIterator* si_create_from_native();
StackIterator* si_create_from_native(VM_thread* thread);
StackIterator* si_create_from_registers(Registers* regs, bool is_ip_past, M2nFrame* lm2nf);
void si_transfer_all_preserved_registers(StackIterator*);
bool si_is_past_end(StackIterator* si);
void si_goto_previous(StackIterator* si);
StackIterator* si_dup(StackIterator* si);
void si_free(StackIterator* si);
NativeCodePtr si_get_ip(StackIterator* si);
void si_set_ip(StackIterator* si, NativeCodePtr ip, bool also_update_stack_itself);
void si_set_code_chunk_info(StackIterator* si, CodeChunkInfo* cci);
CodeChunkInfo* si_get_code_chunk_info(StackIterator* si);
JitFrameContext* si_get_jit_context(StackIterator* si);
bool si_is_native(StackIterator* si);
M2nFrame* si_get_m2n(StackIterator* si);
void si_set_return_pointer(StackIterator* si, void** return_value);
void si_transfer_control(StackIterator* si);
void si_copy_to_registers(StackIterator* si, Registers* regs);
void si_reload_registers();
static void si_unwind_from_m2n(StackIterator* si);

// Stack trace

unsigned st_get_depth()
bool st_get_frame(unsigned target_depth, StackTraceFrame* stf)
void st_get_trace(unsigned* res_depth, StackTraceFrame** stfs)
void st_print_frame(ExpandableMemBlock* buf, StackTraceFrame* stf)
void st_print(FILE* f)

struct StackTraceFrame {
    Method_Handle method;
    NativeCodePtr ip;
    const char* file;
    int line;  // -2 for native methods, -1 for unknown line number
};

\endcode

*/

/**
 * Pointer to code.
 */
typedef unsigned* NativeCodePtr;

/**
 * Opaque pointer to execution engine.
 */
typedef struct ExecutionEngine* Execution_Engine_Handle;

/**
 * Machine-dependent thread context (fully or partly filled): register values.
 */
typedef struct Execution_Context Execution_Context;

/**
 * Return execution context of current thread.
 */
Execution_Context ec_get_current();

/**
 * Return execution context of suspended thread.
 */
Execution_Context ec_get_suspended(Thread_Handle thread);

/**
 * On architectures with register stacks, ensure that the register stack of the
 * current thread is consistent with its backing store, as the backing store
 * might have been modified by stack walking code.
 *
 * @note rename of the function si_reload_registers
 */
void ec_update_current();

/**
 * Kinds of values stored in executed context.
 * This clasification is machine dependent, but there are common part like IP and return IP.
 */
typedef enum {
    EC_CT_IP, EC_CT_RETIP
} ContextType;

/**
 * Returns value contained in specified executing context for specified register
 */
ContextValue ec_get(Execution_Context context, ContextType type);

/**
 * Sets value contained in specified executing context for specified register
 */
void ec_set(Execution_Context context, ContextType type, ContextValue value);

/**
 * Stack iterator structure. At any moment of time stack
 * iterator may point to a particular stack frame or be
 * past the end of iteration.
 *
 * @note This is an possible implementation note,
 *       and this will not be exposed in final interface.
 *      
 */
typedef struct StackIterator {

    /**
     * Handle of owner of current stack frame.
     * @note computable from ip.
     */
    Execution_Engine_Handle ee;

    /**
     * Depth of inlined method.
     */
    int depth;

    /** 
     * Execution engine-specific field.
     * May be used to store NativeFrame* or ebp value.
     */
    void *info;

    /**
     * Execution context (register values).
     * Fully or partly filled machine-dependent structure
     * @note rename of JitFrameContext
     */
    Execution_Context context;

} StackIterator;

///**
// * Create stack iterator from current thread context.
// * @note rename of si_create_from_native()
// */
//StackIterator* si_create_for_current();
//
///**
// * Create stack iterator for the specified suspended thread.
// * @note rename of si_create_from_native()
// */
//StackIterator* si_create_for_suspended(Thread_Handle thread);
//
///**
// * Create stack iterator for the specified execution context.
// * @note consolidation of si_create_from_registers() 
// *       and si_create_from_native(thread)
// */
//StackIterator* si_create_from_context(Execution_Context* context);

/**
 * Create stack iterator for the specified execution context.
 * @note consolidation of si_create_from_registers() 
 *       and si_create_from_native(thread)
 */
StackIterator* si_create(Execution_Context* context);

/**
 * Duplicate stack iterator.
 *
 * @note used only once in current VM. Probably may be eliminated.
 */
StackIterator* si_dup(StackIterator* iterator);

/**
 * Free memory used by stack iterator.
 */
void si_free(StackIterator* iterator);

/**
 * Kinds of stack frame.
 * This classification is intended for security manager to be able
 * to distinguish stack frames it does not need to take into account,
 * for example, VM stack frames.
 *
 * @note XXX <UL>In fact, two different orthogonal classifications exist:
 *       <LI>by the owner: interpreter, JIT, VM, JNI
 *       <LI>by the java spec: java, JNI, other
 *       </UL>
 *       Current vision is that owner classification is available
 *       through si_get_owner().
 */
typedef enum {
    SF_JAVA_REAL = 0x1,
    SF_JAVA_INLINED = 0x2,
    SF_JNI = 0x4,
    SF_VM = 0x8,
    SF_JAVA = SF_JAVA_REAL | SF_JAVA_INLINED,
    SF_OTHER
} SF_Kind;

/**
 * Returns the kind of the current stack frame.
 *
 * @see SF_Kind
 * @note How to compute the kind of the stack frame?
 */
SF_Kind si_get_kind(StackIterator* iterator);

/**
 * Iterate to next stack frame.
 *
 * @param iterator - the pointer to the stack frame
 *
 * @return true if iterator still points to a stack frame, 
 *         false if iteration is complete.
 * @note rename of si_goto_previous()
 */
bool si_next(StackIterator* iterator, SF_Kind kind);


/**
 * Returns true if iteration is over.
 * @note rename of si_is_past_end()
 *
 * @param iterator - the pointer to the stack frame
 *
 */
bool si_done(StackIterator* iterator);

/**
 * Returns true if current stack frame is unwindable.
 *
 * @param iterator - the pointer to the stack frame
 *
 */
bool si_is_unwindable(StackIterator* iterator);

/**
 * Unwind current stack frame with restoring the values
 * of saved registers. The synchronization objects
 * must be carefully released by this method.
 *
 * @param iterator - the pointer to the stack frame
 *
 */
void si_unwind(StackIterator* iterator);

/**
 * Resume current stack frame on current thread.
 * This function frees the iterator and does not return.
 *
 * @note Equivalent to transfer_control(si_get_context(iterator))
 *       and may be eliminated as such.
 *
 * @param iterator - the pointer to the stack frame
 *
 */
void si_transfer_control(StackIterator* iterator);

/**
 * Return pointer to context structure of the current stack
 * frame context.
 *
 * @param iterator - the pointer to the stack frame
 *
 */
Execution_Context* si_get_context(StackIterator* iterator);


/**
 * Get the pointer to the owner of the stack frame
 * (i.e. JIT or interpreter).
 *
 * @note We may want to have a dummy execution engine
 *       corresponding to the native C++ compiler in order to
 *       provide more functionality for VM code.
 *
 * @param iterator - the pointer to the stack frame
 *
 */
Execution_Engine_Handle si_get_owner(StackIterator* iterator);

/**
 * Enumerate live pointers in current stack frame.
 *
 * @note Real enumeration is delegated to the exucution
 *       engine owning the current stack frame.
 *       This method may be completely eliminated
 *       in favour of using EE interfaces directly.
 *
 * @param iterator - the pointer to the stack frame
 *
 */
void si_enumerate(StackIterator* iterator);

/**
 * Retrieves information associated with current stack frame.
 * For native or interpreter frames it is likely to return
 * pointer to the actual frame structure, for jitted frames
 * may return the value of base stack register.
 *
 * The returned value may be used as a parameter to functions
 * provided by the execution engine owning the stack frame.
 *
 * @param iterator - the pointer to the stack frame
 *
 */
void* si_get_info(StackIterator* iterator);

/**
 * Get the method handle of the current stack frame.
 *
 * @param iterator - the pointer to the stack frame
 *
 */
Method_Handle si_get_method(StackIterator* iterator);


/**
 * Retrieves the file name corresponding to the current stack frame.
 * If no information is available, an empty string is written to the
 * buffer.
 *
 * @param iterator - the pointer to the stack frame
 * @param buf - the buffer to be filled
 * @param length - the size of the buffer. If the size is not sufficient
 *        to hold the file name, then file name is truncated.
 *
 * @note the request is delegated to execution engine.
 */
void si_get_trace_file(StackIterator* iterator, char* buf, int length);

/**
 * Retrieves the line number corresponding to the current stack frame.
 * If the number is not available, returns 0.
 *
 * @param iterator - the pointer to the stack frame
 * @return the line number corresponding to the current stack frame, where routine is working now.
 * @note The request is delegated to execution engine.
 */
int si_get_trace_line(StackIterator* iterator);

/**
 * Retrieves debug information associated 
 * with the current stack frame.
 * The format of this information is implementation-dependent.
 *
 * @param iterator - the pointer to the stack frame
 *
 * @note the request is delegated to execution engine.
 */
void si_get_debug_info(StackIterator* iterator, char* buf, int length);


#endif // _STACK_WALKER_H
