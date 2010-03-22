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
 * @author Ilya Berezhniuk
 */
/**
 * @file jvmti_break_intf.h
 * @brief JVMTI native breakpoints API
 */
#if !defined(__JVMTI_BREAK_INTF_H__)
#define __JVMTI_BREAK_INTF_H__

#include "open/types.h"
#include "jni.h"
#include "lock_manager.h"
#include "environment.h"


// Callbacks are called for interfaces according to its priority
typedef enum {
    PRIORITY_NCAI_STEP_BREAKPOINT = 0,
    PRIORITY_SINGLE_STEP_BREAKPOINT,
    PRIORITY_NCAI_BREAKPOINT,
    PRIORITY_SIMPLE_BREAKPOINT,
    PRIORITY_NUMBER
} jvmti_BreakPriority;

class VMBreakInterface;
class InstructionDisassembler;

struct VMBreakPoint
{
    InstructionDisassembler* disasm;
    VMBreakPoint*            next;
    NativeCodePtr            addr;
    jmethodID                method;
    jlocation                location;
    jbyte                    saved_byte;
    Registers                regs;
};

// Breakpoint reference
struct VMBreakPointRef
{
    VMBreakPoint*    bp;
    POINTER_SIZE_INT data;
    VMBreakPointRef* next;
};

struct VMLocalBreak
{
    VMBreakPoint *bp;
    VMBreakPoint *bp_next;
    VMBreakInterface *intf;
    VMLocalBreak* next;
    unsigned priority;
    VM_thread* vmthread;
    VMBreakPoint *local_bp;
};

// Pointer to interface callback function
typedef bool (*BPInterfaceCallBack)(TIEnv *env, const VMBreakPoint* bp, const POINTER_SIZE_INT data);
typedef bool (*BPInterfaceProcedure) (VMBreakPoint *bp);

class VMBreakPoints
{
public:
    VMBreakPoints() : m_break(NULL), m_local(NULL)
    {
        for(unsigned index = 0; index < PRIORITY_NUMBER; index++ ) {
            m_intf[index] = NULL;
        }
    }

    ~VMBreakPoints();

    // Class lock interface
    void lock() {m_lock._lock();}
    void unlock() {m_lock._unlock();}
    // Is used for LMAutoUnlock
    Lock_Manager* get_lock() {return &m_lock;}

    // Returns interface for breakpoint handling
    VMBreakInterface* new_intf(TIEnv *env, BPInterfaceCallBack callback,
        unsigned priority, bool is_interp);
    // Destroys interface and deletes all its breakpoints
    void release_intf(VMBreakInterface* intf);

    // Inserts breakpoint into global list and performs instrumentation
    bool insert_native_breakpoint(VMBreakPoint* bp);
    bool insert_interpreter_breakpoint(VMBreakPoint* bp);
    // Removes breakpoint from global list and restores instrumented area
    bool remove_native_breakpoint(VMBreakPoint* bp);
    bool remove_interpreter_breakpoint(VMBreakPoint* bp);

    // Search breakpoints operations
    VMBreakPoint* find_breakpoint(jmethodID method, jlocation location);
    VMBreakPoint* find_breakpoint(NativeCodePtr addr);
    VMBreakPoint* find_other_breakpoint_with_same_addr(VMBreakPoint* bp);
    VMBreakPoint* find_next_breakpoint(VMBreakPoint* prev, NativeCodePtr addr);
    VMBreakPoint* find_next_breakpoint(VMBreakPoint* prev, jmethodID method,
        jlocation location);

    // Search breakpoints for given method
    VMBreakPoint* find_method_breakpoint(jmethodID method);
    VMBreakPoint* find_next_method_breakpoint(VMBreakPoint* prev, jmethodID method);

    // Checks if given breakpoint is set by other interfaces
    VMBreakPointRef* find_other_reference(VMBreakInterface* intf,
        jmethodID method, jlocation location);
    VMBreakPointRef* find_other_reference(VMBreakInterface* intf,
        NativeCodePtr addr);
    VMBreakPointRef* find_other_reference(VMBreakInterface* intf,
        VMBreakPoint* bp);

    // Interfaces iterator
    VMBreakInterface* get_first_intf(unsigned priority) { return m_intf[priority]; }
    VMBreakInterface* get_next_intf(VMBreakInterface *intf);

    // Breakpoints iterator
    VMBreakPoint* get_first_breakpoint() { return m_break; }
    VMBreakPoint* get_next_breakpoint(VMBreakPoint* prev);

    // General callback functions
    void  process_native_breakpoint(Registers* regs);
    jbyte process_interpreter_breakpoint(jmethodID method, jlocation location);

    // Find thread-local breakpoint information
    VMLocalBreak* find_thread_local_break(VM_thread* vmthread);

private:
    // Checks breakpoint before inserting
    inline bool check_insert_breakpoint(VMBreakPoint* bp);
    void insert_breakpoint(VMBreakPoint* bp);
    void remove_breakpoint(VMBreakPoint* bp);

    // Set/remove thread processing breakpoints interfaces
    void set_thread_local_break(VMLocalBreak *local);
    void remove_thread_local_break(VMLocalBreak *local);

private:
    VMBreakInterface* m_intf[PRIORITY_NUMBER];
    VMBreakPoint*     m_break;
    VMLocalBreak*     m_local;
    Lock_Manager      m_lock;
};

class VMBreakInterface
{
    friend class VMBreakPoints;

public:
    int get_priority() const { return m_priority; }

    // Iteration
    VMBreakPointRef* get_reference() { return m_list; }
    VMBreakPointRef* get_next_reference(VMBreakPointRef* ref)
    {
        assert(ref);
        return ref->next;
    }

    // Basic operations

    VMBreakPointRef* add_reference(jmethodID method, jlocation location, POINTER_SIZE_INT data);
    // To specify address explicitly
    VMBreakPointRef* add_reference(jmethodID method, jlocation location,
                    NativeCodePtr addr, POINTER_SIZE_INT data);
    VMBreakPointRef* add_reference(NativeCodePtr addr, POINTER_SIZE_INT data);

    bool remove_reference(VMBreakPointRef* ref);
    void remove_all_reference()
    {
        while (m_list) {
            remove_reference(m_list);
        }
    }

    VMBreakPointRef* find_reference(jmethodID method, jlocation location);
    VMBreakPointRef* find_reference(NativeCodePtr addr);
    VMBreakPointRef* find_reference(VMBreakPoint* bp);

protected:
    VMBreakInterface(TIEnv *env,
                     BPInterfaceCallBack callback,
                     unsigned priority,
                     bool is_interp);
    ~VMBreakInterface() { remove_all_reference(); }
    TIEnv* get_env() { return m_env; }

private:
    inline VMBreakPointRef* add_reference_internal(VMBreakPoint *bp, POINTER_SIZE_INT data);

protected:
    VMBreakInterface*   m_next;

private:
    BPInterfaceCallBack  breakpoint_event_callback;
    BPInterfaceProcedure breakpoint_insert;
    BPInterfaceProcedure breakpoint_remove;
    VMBreakPointRef*     m_list;
    TIEnv               *m_env;
    unsigned             m_priority;
    Lock_Manager         m_lock;
};

// Address of this function is used for stack unwinding througn breakpoint
extern "C" void __cdecl process_native_breakpoint_event(Registers* regs);

// Callback function for native breakpoint processing
bool jvmti_jit_breakpoint_handler(Registers *regs);

// Callback function for interpreter breakpoint processing
VMEXPORT jbyte
jvmti_process_interpreter_breakpoint_event(jmethodID method, jlocation location);

// Callback for JIT method compile
void jvmti_set_pending_breakpoints(Method *method);

// debug function dump compiled method
void jvmti_dump_compiled_method(Method *method);


#endif  // __JVMTI_BREAK_INTF_H__
