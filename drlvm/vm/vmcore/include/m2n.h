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
 * @author Intel, Evgueni Brevnov, Ivan Volosyuk
 */  

#ifndef _M2N_H_
#define _M2N_H_

#include "object_handles.h"
#include "open/types.h"
#include "vm_core_types.h"

// This module is for the "managed to native frame" abstraction, M2nFrame.
// An M2nFrame sits on the activation stack immediately above a managed
// frame and immediately below a native frame. All managed to native code
// transitions should involve pushing an M2nFrame with the following exceptions:
//     Code that does not enable GC (note that calling managed code enables GC),
//     throw exceptions, or inspects the stack does not need to push an M2nFrame.
//     All other code must.
// M2nFrames are linked together in the order they are pushed onto the
// activation stack, and each thread has a pointer to the most recent M2nFrame.
// Each M2nFrame contains a list of local object handles, can identify the
// location of the return address into the managed frame that preceeds it,
// and is optionally associated with a native method.

struct M2nFrame;

typedef U_32 frame_type;

extern const U_32 FRAME_UNKNOWN;
extern const U_32 FRAME_NON_UNWINDABLE;
extern const U_32 FRAME_JNI;
extern const U_32 FRAME_COMPILATION;
extern const U_32 FRAME_UNPOPABLE;
extern const U_32 FRAME_POPABLE;
extern const U_32 FRAME_POP_NOW;
extern const U_32 FRAME_POP_DONE;
extern const U_32 FRAME_POP_MASK;
extern const U_32 FRAME_SAFE_POINT;
extern const U_32 FRAME_MODIFIED_STACK;

// The pushing and popping of native frames is done only by stubs that
// implement the managed to native transitions. These stubs use code that is
// necessarily architecture specific and some facilities that are not needed
// by other clients of this code, so this part of the interface is described
// elsewhere.

// fill m2n frame as empty
void m2n_null_init(M2nFrame *);

// Get the most recent M2nFrame of the current thread
VMEXPORT // temporary solution for interpreter unplug
M2nFrame* m2n_get_last_frame();

// Set the most recent M2nFrame of the current thread
// The caller must ensure the frame is on the current thread's activation stack
VMEXPORT // temporary solution for interpreter unplug
void m2n_set_last_frame(M2nFrame *);

// Set the most recent M2nFrame of given thread
// The caller must ensure the frame is on the thread's activation stack
VMEXPORT
void m2n_set_last_frame(VM_thread *, M2nFrame *);

// Get the most recent M2nFrame of the given thread
VMEXPORT // temporary solution for interpreter unplug
M2nFrame* m2n_get_last_frame(VM_thread *);

// Get the previous M2nFrame to the given one
VMEXPORT // temporary solution for interpreter unplug
M2nFrame* m2n_get_previous_frame(M2nFrame *);

// Get the local object handles for the M2nFrame
VMEXPORT // temporary solution for interpreter unplug
ObjectHandles* m2n_get_local_handles(M2nFrame *);

// Set the local object handles
// Note that this just updates the list, it does not free the old list
VMEXPORT // temporary solution for interpreter unplug
void m2n_set_local_handles(M2nFrame *, ObjectHandles *);

// Get the return address into the preceding managed frame
NativeCodePtr m2n_get_ip(M2nFrame *);

// ? 20040708 New function - needs proper implementation.
// Set the return address into the preceding managed frame
void m2n_set_ip(M2nFrame *, NativeCodePtr);

// Get the native method associated with the transition to native code
// or NULL if no such association.
Method_Handle m2n_get_method(M2nFrame *);

// Returns type of noted m2n frame
frame_type m2n_get_frame_type(M2nFrame *);

// Sets type of noted m2n frame
void m2n_set_frame_type(M2nFrame *, frame_type);

// Returns size of m2n frame
size_t m2n_get_size();

// Push a special M2nFrame for managed code suspended by the OS in say
// a signal handler or exception filter.
// The frame can be popped by setting the last frame to a prior frame
void m2n_push_suspended_frame(M2nFrame* m2nf, Registers* regs);

// Push a special M2nFrame for managed code suspended by the OS in say
// a signal handler or exception filter.
// The frame can be popped by setting the last frame to a prior frame
void m2n_push_suspended_frame(VM_thread* thread, M2nFrame* m2nf, Registers* regs);

// Push a special M2nFrame for managed code suspended by the OS in say
// a signal handler or exception filter. The frame is allocated in the heap.
// It can be popped by setting the last frame to a prior frame
// and then calling free on the frame pointer.
M2nFrame* m2n_push_suspended_frame(Registers *);

// Push a special M2nFrame for managed code suspended by the OS in
// signal handler or exception filter. The frame is allocated in the heap.
// The frame can be popped by setting the last frame to a prior frame
// and then calling free on the frame.
M2nFrame* m2n_push_suspended_frame(VM_thread *, Registers *);

// answers true if passed in m2n frame represents suspended frame
bool m2n_is_suspended_frame(M2nFrame *);

// returns pointer to the registers used for jvmti PopFrame
Registers* get_pop_frame_registers(M2nFrame* );

// sets pointer to the registers used for jvmti PopFrame
void set_pop_frame_registers(M2nFrame* , Registers*);

#endif //!_M2N_H_
