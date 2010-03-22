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
 * @author Intel, Pavel Afremov
 */  

/**
 *@file
 * Mechanism for iterating over stack frames
 * of Java and native code.
 *
 * This stack iterator handle is a black box.
 *
 * The iteractor supports iterating over:
 * <ul>
 * <li>Managed code frames corresponding to Java
 * code
 * <li>Managed-to-native frames (M2N) for transferring
 * data and control between managed and native frames
 *
 * Native frames iteration is not currently supported. To iterate over
 * native frames, use OS-provided tools.
 * </ul>
 * This iterator uses the order from the most recently pushed frame
 * to the first frame pushed.
 * With the stack iterator, you can also resume a frame in the current thread and
 * transfer execution and control to this frame.
 */

#ifndef _STACK_ITERATOR_H_
#define _STACK_ITERATOR_H_

#include "jit_export.h"
#include "open/types.h"
#include "vm_core_types.h"

struct StackIterator;

/**
 * Creates a new stack iterator for the current thread.
 *
 * @note The function assumes that the thread is currently in native code.
 */
StackIterator* si_create_from_native();

/**
 * Filles a stack iterator structure for the current thread.
 *
 * @note The function assumes that the thread is currently in native code.
 */
void si_fill_from_native(StackIterator* si);

/**
 * Creates a new stack iterator for the given thread.
 *
 * The thread can run concurrently with the stack iterator,
 * but it must not pop (return past) the most recent M2N frame when the iterator is called.
 *
 * Creation is not atomic with respect to pushing/popping of M2N frames.
 * The client code must ensure that such operations are serialized.
 *
 * @param[in] thread - the pointer to the thread, the stack of which must be enumerated
 *
 * @note The function assumes that the given thread is currently in native code.
 */
StackIterator* si_create_from_native(VM_thread* thread);

/**
 * Filles a stack iterator structure for the  given thread.
 *
 * The thread can run concurrently with the stack iterator,
 * but it must not pop (return past) the most recent M2N frame when the iterator is called.
 *
 * Creation is not atomic with respect to pushing/popping of M2N frames.
 * The client code must ensure that such operations are serialized.
 *
 * @param[in] thread - the pointer to the thread, the stack of which must be enumerated
 *
 * @note The function assumes that the given thread is currently in native code.
 */
void si_fill_from_native(StackIterator* si, VM_thread* thread);

/**
 * Creates a new stack iterator for the suspended thread.
 *
 * The thread can run concurrently with the stack iterator,
 * but it must not pop (return past) the most recent M2N frame when the iterator is called.
 *
 * Creation is not atomic with respect to pushing/popping of M2N frames.
 * The client code must ensure that such operations are serialized.
 *
 * @param[in] regs        -  values of the registers at the point of suspension
 * @param[in] is_ip_past  -  indicate is ip past or not
 * @param[in] m2nf        -  the pointer to the M2N frame that must be the one immediately
 *                           prior to the suspended frame
 *
 * @note The function assumes that iterated thread is currently suspended from managed code.
 */
StackIterator* si_create_from_registers(Registers* regs, bool is_ip_past, M2nFrame* m2nf);

/**
 * Filles a stack iterator structure for the suspended thread.
 *
 * The thread can run concurrently with the stack iterator,
 * but it must not pop (return past) the most recent M2N frame when the iterator is called.
 *
 * Creation is not atomic with respect to pushing/popping of M2N frames.
 * The client code must ensure that such operations are serialized.
 *
 * @param[in] regs        -  values of the registers at the point of suspension
 * @param[in] is_ip_past  -  indicate is ip past or not
 * @param[in] m2nf        -  the pointer to the M2N frame that must be the one immediately
 *                           prior to the suspended frame
 *
 * @note The function assumes that iterated thread is currently suspended from managed code.
 */
void si_fill_from_registers(StackIterator* si, Registers* regs, bool is_ip_past, M2nFrame* lm2nf);

/**
 * Returns suze of stack iterator structure in bytes.
 */
size_t si_size();

/**
 * Makes a copy of the given stack iterator.
 *
 * @param[in] si -  the pointer to the stack iterator to be copied
 */
StackIterator* si_dup(StackIterator* si);

/**
 * Frees the stack iterator.
 *
 * @param[in] si -  the pointer to the stack iterator to be freed
 */
void si_free(StackIterator* si);

/**
 * Ensures that all preserved registers are transferred from the M2N frame
 * to the iterator.
 *
 * Depending on the platform, the implementation of stack iterators and M2N frames
 * may not track all preserved registers required for resuming a frame, but may instead track
 * enough for root set enumeration and stack walking.
 *
 * This function and the corresponding additional stub generator for M2N frames
 * allow all registers to be tracked for exception propagation.
 *
 * @param[in] si -  the poiter to the stack iterator that will contain all preserved
 *                  registers
 *
 * @note Only call the function when the iterator is at an M2N frame
 *       that has all preserved registers saved.
 */
void si_transfer_all_preserved_registers(StackIterator* si);

/**
 * Checks whether the stack iterator has passed all the frames.
 *
 * @param[in] si -  the poiter to a StackIterator which should be tested is past all
 *                  the frames or not.
 * @return <code>TRUE</code> if the transferred stack iterator has passed all the frames;
 *         otherwise, <code>FALSE</code>.
 */
bool si_is_past_end(StackIterator* si );

/**
 * Goes to the frame previous to the current one.
 *
 * @param[in] si -  the pointer to the stack iterator that will be iterated to the previous
 *                  frame
 * @param[in] over_popped - take into account the intermediate result of pop frame operation.
 */
void si_goto_previous(StackIterator* si, bool over_popped = true);

/**
 * Gets the pointer to the top of the stack.
 *
 * @param[in] si -  the pointer to the stack iterator indicating stack
 *
 * @return The pointer to the top of the stack.
 */
void* si_get_sp(StackIterator* si);

/**
 * Gets the instruction pointer for the current frame.
 *
 * @param[in] si -  the pointer to the stack iterator indicating the current frame
 *
 * @return The instruction pointer for the current frame.
 */
NativeCodePtr si_get_ip(StackIterator* si);

/**
 * Sets the instruction pointer for the current frame.
 *
 * @param[in] si                        -  the pointer to the stack iterator indicating
 *                                         the current frame
 * @param[in] ip                        -  the instruction pointer for the current frame
 * @param[in] also_update_stack_itself  -  the flag indicating whether the function must update
 *                                         data on the stack or only in the iterator
 *
 * @return If <i>also_update_stack_itself</i> is <code>TRUE</code>,
 *         updates the instruction pointer in the stack; otherwise, the new
 *         value stored in the stack iterator only.
 */
void si_set_ip(StackIterator* si, NativeCodePtr ip,
               bool also_update_stack_itself = false);

/**
 * Sets the code chunk for the current frame of the stack indicated by the iterator.
 *
 * @param[in] si  -  the pointer to the stack iterator indicating the current frame
 * @param[in] cci -  the pointer to CodeChunkInfo to be set for the current frame
 *
 * @note The function assumes that the thread is iterated in a managed frame.
 */
void si_set_code_chunk_info(StackIterator* si, CodeChunkInfo* cci);

/**
 * Gets the code chunk information for the current frame.
 *
 * @param[in] si  -  the pointer to the stack iterator indicating the current frame
 *
 * @return The pointer to the code chunk information for managed frames and <code>NULL</code>
 * for M2N frames.
 */
CodeChunkInfo* si_get_code_chunk_info(StackIterator* si);

/**
 * Gets the JIT frame context for the current frame.
 *
 * @param[in] si -  the pointer to the stack iterator indicating the current frame
 *
 * @return The JIT frame context for the current frame.
 */
JitFrameContext* si_get_jit_context(StackIterator* si);

/**
 * Checks whether the current frame is an M2N frame.
 *
 * @param[in] si -  the pointer to the stack iterator indicating the current frame
 *
 * @return <code>TRUE</code> if the current thread is an M2N frame;
 *         otherwise, <code>FALSE</code>.
 */
bool si_is_native(StackIterator* si);

/**
 * Gets the pointer to the M2N frame if the current frame is M2N.
 *
 * @param[in] si -  the pointer to the stack iterator indicating the current frame
 *
 * @return The pointer to the the M2N frame if the current frame is M2N; otherwise,
 *         <code>NULL</code>.
 */
M2nFrame* si_get_m2n(StackIterator* si);

/**
 * Gets the pointer to the value of the return register.
 *
 * If transfer control is called, the resumed frame will see this value.
 *
 * @param[in] si           -  the pointer to the stack iterator indicating the current frame
 *
 * @return  the pointer to the pointer to the return value.
 */
void** si_get_return_pointer(StackIterator* si);

/**
 * Sets the pointer to the value of the return register.
 *
 * If the transfer control is subsequently called, the resumed frame has data on this change.
 *
 * @param[in] si           -  the pointer to the stack iterator indicating the current frame
 * @param[in] return_value -  the pointer to the pointer to the return value that will be set
 */
void si_set_return_pointer(StackIterator* si, void** return_value);

/**
 * Thransfers control and resumes execution in the current frame of the iterator.
 * Returns no values and frees the stack iterator.
 *
 * @param[in] si -  the pointer to the stack iterator indicating the current frame
 *
 * @note This function must only be called for the iterator on the current thread's frames.
 */
void si_transfer_control(StackIterator* si);

/**
 * Copies the value of the stack iterators' current frame into the given registers.
 *
 * This way, resuming these registers transfers control to the current frame.
 *
 * @param[in] si    -  the pointer to the stack iterator indicating the current frame
 * @param[out] regs -  the pointer to the registers where the registers' values
 *               from the stack iterator will be copied
 */
void si_copy_to_registers(StackIterator* si, Registers* regs);

/**
 * Reloads registers from the register stack.
 *
 *
 * @note On architectures with register stacks, ensure that the register stack of
 *       the current thread is consistent with its backing store. This is done because the backing
 *       store might have been modified by the stack walking code.
 */
void si_reload_registers();

/**
 * Gets the method handle for the frame iterated by the stack iterator.
 *
 * @param[in] si - the pointer to the stack iterator indicating the current frame
 *
 * @return The method handle corresponding to the given stack iterator.
 */
Method_Handle si_get_method(StackIterator* si);

/**
 * Gets the number of inlined methods corresponding to the current frame
 * iterated by stack iterator.
 *
 * @param[in] si - the pointer to the stack iterator indicating the current frame
 *
 * @return The number of inlined methods.
 */
U_32 si_get_inline_depth(StackIterator* si);

/**
 * Gets the method handle for the frame iterated by the stack iterator.
 *
 * @param[in] si - the pointer to the stack iterator indicating the frame,
 *                 from which control will be transfered
 * @param[in] callback - pointer to the native cose adress which should be
 *                       called, after transfer control
 */
void si_set_callback(StackIterator* si, NativeCodePtr* callback);

#endif //!_STACK_ITERATOR_H_
