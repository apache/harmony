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

#ifndef THREAD_MANAGER_HEADER
#define THREAD_MANAGER_HEADER

#include "open/hythread.h"

#include "jthread.h"
#include "exceptions_type.h"

#define GC_BYTES_IN_THREAD_LOCAL (20 * sizeof(void *))
#define CONVERT_ERROR(stat)	(stat)
#define TM_JVMTI_MAX_BUFFER_SIZE 500
#define TM_INITIAL_OWNED_MONITOR_SIZE 32

#ifdef __cplusplus
extern "C"
{
#endif

struct jvmti_frame_pop_listener;
struct JVMTISingleStepState;
struct NCAISingleStepState;
struct ClassLoader;
struct Registers;

/**
 * Java-specific context that is attached to tm_thread control structure by Java layer
 */
struct JVMTIThread
{
    /**
     * Blocked on monitor times count
     */
    jlong blocked_count;

    /**
     * Blocked on monitor time in nanoseconds
     */
    jlong blocked_time;

    /**
     * Waited on monitor times count
     */
    jlong waited_count;

    /**
     * Waited on monitor time in nanoseconds
     */
    jlong waited_time;

    /**
     * JVM TI local storage
     */
    JVMTILocalStorage jvmti_local_storage;

    /**
     * Monitor this thread is blocked on.
     */
    jobject contended_monitor;

    /**
     * Monitor this thread waits on.
     */
    jobject wait_monitor;

    /**
     * Monitors for which this thread is owner.
     */
    jobject *owned_monitors;

    /**
     * owned monitors count.
     */
    int owned_monitors_nmb;

    /**
     * owned monitors array size.
     */
     int owned_monitors_size;

    /**
     * For support of JVMTI events: EXCEPTION, EXCEPTION_CATCH
     * If p_exception_object is set and p_exception_object_ti is not
     *    - EXCEPTION event should be generated
     * If p_exception_object_ti is set and p_exception_object is not
     *     - EXCEPTION_CATCH even should be generated
     */
    volatile struct ManagedObject *p_exception_object_ti;

    /**
     * Buffer used to create instructions instead of original instruction
     * to transfer execution control back to the code after breakpoint
     * has been processed
     */
    jbyte *jvmti_jit_breakpoints_handling_buffer;

    struct jvmti_frame_pop_listener *frame_pop_listener;
    struct JVMTISingleStepState *ss_state;
    struct Registers *jvmti_saved_exception_registers;

    // Flag and restart address for memory access violation detection
    int                               violation_flag;
    void*                             violation_restart_address;

    // Storage for NCAI Single Step data
    struct NCAISingleStepState* ncai_ss;
    // Is set when current thread is in NCAI handler
    jboolean flag_ncai_handler;
};

struct VM_thread
{
    /**
     * Native thread which is associated with <code>VM_thread</code>
     * The fields of <code>HyThread</code> sub-structure should not be used in VM directly
     * An address of <code>hy_thread</code> field should be used instead
     * as an argument for <code>hythread*</code> functions.
     */
    struct HyThread hy_thread;

    /**
     * Thread reference object to corresponding java.lang.ThreadWeakRef instance
     */
    jobject weak_ref;

    /**
     * Java thread object to corresponding java.lang.Thread instance
     */
    jobject java_thread;

    /**
     * Exception that has to be thrown in stopped thread
     */
    jthrowable stop_exception;

    /**
     * Memory pool where this structure is allocated.
     * This pool should be used by current thread for memory allocations.
     */
    apr_pool_t *pool;

    /**
     * JNI environment associated with this thread.
     */
    JNIEnv *jni_env;

    /**
     * Class loader which loads native library and calls to its JNI_OnLoad
     */
    struct ClassLoader *onload_caller;

    /**
     * Flag to detect if a class is not found on bootclasspath,
     * as opposed to linkage errors.
     * Used for implementing default delegation model.
     */
    unsigned char class_not_found;

    /**
     * Flag to detect if a thread is suspend.
     * Used for serialization Java suspend.
     */
    unsigned char suspend_flag;

    // In case exception is thrown, Exception object is put here
    volatile struct Exception thread_exception;

    // flag which indicate that guard page on the stack should be restored
    unsigned char restore_guard_page;

    // thread stack address
    void *stack_addr;

    // thread stack size
    UDATA stack_size;

    int finalize_thread_flags;

    // CPU registers.
    struct Registers *regs;

    // This field is private the to M2nFrame module, init code should set it to NULL
    // Informational frame - created when native is called from Java,
    // used to store local handles (jobjects) + registers.
    // =0 if there is no m2n frame.
    void *last_m2n_frame;

    // GC Information
    unsigned char _gc_private_information[GC_BYTES_IN_THREAD_LOCAL];
    void *native_handles;
    void *gc_frames;

#if defined(PLATFORM_POSIX) && defined(_IPF_)
    // Linux/IPF
    hysem_t suspend_self;   // To suspend current thread for signal handler
    uint64 suspended_state; // Flag to indicate how the one thread is suspended
                            // Possible values:
                            // NOT_SUSPENDED, 
                            // SUSPENDED_IN_SIGNAL_HANDLER,
                            // SUSPENDED_IN_DISABLE_GC_FOR_THREAD
    uint64 t[2];    // t[0] <= rnat, t[1] <= bspstore for current thread context
                    // t[0] <= rnat, t[1] <= bsp      for other   thread context
#endif

    void *lastFrame;
    void *firstFrame;
    int interpreter_state;

    /**
     * The upper boundary of the stack to scan when verifying stack enumeration
     */
    void **stack_end;
    
    /**
     * Is this thread daemon?
     */
    IDATA daemon;

    /**
     * JVMTI support in thread structure
     */
    struct JVMTIThread jvmti_thread;
};

/**
 * Java thread creation attributes.
 */
struct jthread_start_proc_data
{
    /**
     * Native thread
     */
    hythread_t native_thread;

    /**
     * Pointer to Java VM.
     */
    JavaVM *java_vm;

    /**
     * Thread scheduling priority.
     */
    jint priority;

    /**
     * Thread stack size.
     */
    UDATA stacksize;

    /**
     * Denotes whether Java thread is daemon.  
     * JVM exits when the only threads running are daemon threads.
     */
    jboolean daemon;

    /**
     * JVMTI environment.
     */
    jvmtiEnv *jvmti_env;

    /**
     * JVMTI start function to be executed in this thread.
     */
    jvmtiStartFunction proc;

    /**
     * Start function argument to the start function. Is passed as an array.
     */
    const void *arg;
};

/**
 * Registrates current thread in VM, so it could execute Java.
 *
 * @param[in] java_vm    - current thread will be attached to the specified VM
 * @param[out] p_jni_env - will point to JNI environment assocciated with the thread
 */
VMEXPORT jint vm_attach(JavaVM * java_vm, JNIEnv ** p_jni_env);

/**
 * Frees java related resources before thread exit.
 */
VMEXPORT jint vm_detach(jobject java_thread);

/**
 * Stores a pointer to TM-specific data in the <code>java.lang.Thread</code> object.
 *
 * A typical implementation may store a pointer within a private
 * non-static field of Thread.
 *
 * @param[in] thread    - a <code>java.lang.Thread</code> object those private field
 *                        is going to be used for data storage
 * @param[in] data_ptr  - a pointer to data to be stored
 */
VMEXPORT void jthread_set_tm_data(jobject thread, void *data_ptr);

/**
 * Retrieves TM-specific data from the <code>java.lang.Thread</code> object.
 *
 * @param[in] thread - a thread
 *
 * @return TM-specific data previously stored, or <code>NULL</code>,
 *         if there are none.
 */
VMEXPORT void* jthread_get_tm_data(jobject thread);

/**
 * <code>vm_objects_are_equal<br>
 * obj1 jobject<br>
 * obj2 jobject</code>
 *
 * @return <code>int</code>
 */
VMEXPORT int vm_objects_are_equal(jobject obj1, jobject obj2);

/**
 * Gets VM_thread from native thread
 */
hy_inline vm_thread_t jthread_self_vm_thread()
{
    register hythread_t self = hythread_self();
    return (self && self->java_status == TM_STATUS_INITIALIZED)
            ? ((vm_thread_t)self) : NULL;
} // jthread_self_vm_thread

/**
 * Gets VM_thread from a given native thread
 */
hy_inline vm_thread_t jthread_get_vm_thread(hythread_t native)
{
    return (native && native->java_status == TM_STATUS_INITIALIZED)
            ? ((vm_thread_t)native) : NULL;
} // jthread_get_vm_thread

/**
 * Gets unsafe VM_thread from native thread.
 * VM_thread could be not initialized.
 */
hy_inline vm_thread_t jthread_self_vm_thread_unsafe()
{
    register hythread_t self = hythread_self();
    return (self && self->java_status != TM_STATUS_WITHOUT_JAVA)
            ? ((vm_thread_t)self) : NULL;
} // jthread_self_vm_thread_unsafe

/**
 * Gets unsafe VM_thread from a given native thread.
 * VM_thread could be not initialized.
 */
hy_inline vm_thread_t jthread_get_vm_thread_unsafe(hythread_t native)
{
    return (native && native->java_status != TM_STATUS_WITHOUT_JAVA)
            ? ((vm_thread_t)native) : NULL;
} // jthread_get_vm_thread_unsafe

/**
 * Gets native thread associated with a given Java thread.
 * @return native thread
 */
hy_inline hythread_t jthread_get_native_thread(jobject java_thread)
{
    assert(java_thread);
    return (hythread_t)jthread_get_tm_data(java_thread);
} // jthread_get_native_thread

/**
 * Gets VM_thread associated with a given Java thread.
 * @return pointer to VM_thread or NULL
 */
hy_inline vm_thread_t jthread_get_vm_thread_from_java(jobject java_thread)
{
    vm_thread_t vm_thread;
    assert(java_thread);
    vm_thread = (vm_thread_t)jthread_get_tm_data(java_thread);
    return (vm_thread && ((hythread_t)vm_thread)->java_status == TM_STATUS_INITIALIZED)
            ? vm_thread : NULL;
} // jthread_get_vm_thread_from_java

#ifdef __cplusplus
}
#endif

#endif // THREAD_MANAGER_HEADER
