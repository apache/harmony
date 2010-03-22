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
 * @author Gregory Shimansky
 */

#ifndef _JVMTI_DIRECT_H_
#define _JVMTI_DIRECT_H_

#include "jni_direct.h"
#include "jvmti.h"
#include "open/hythread_ext.h"
#include "open/rt_types.h"
#include "vm_core_types.h"
#include "vm_threads.h"

struct TIEventThread
{
    hythread_t thread;
    TIEventThread *next;
};

struct Agent;
extern Agent *current_loading_agent;
class VMBreakInterface;

// declared privately in jvmti_tags.h
struct TITags;

// declared privately in jvmti_heap.h
struct TIIterationState;

struct NCAIEnv;

/*
 * Type that describes TI environment created by GetEnv function
 */
struct TIEnv
{
    const ti_interface *functions;

    /// Lock used to protect TIEnv instance
    osmutex_t environment_data_lock;

    JavaVM_Internal *vm;
    Agent *agent;
    void *user_storage;
    jvmtiEventCallbacks event_table;
    jvmtiExtensionEvent *extension_event_table;
    jvmtiCapabilities posessed_capabilities;
    VMBreakInterface *brpt_intf;
    TITags* tags;
    TIIterationState* iteration_state;
    TIEnv* next;
    NCAIEnv *ncai_env;

    bool global_events[TOTAL_EVENT_TYPE_NUM];
    TIEventThread *event_threads[TOTAL_EVENT_TYPE_NUM];

    /**
     * Returns pointer to a callback function that was set by SetEventCallbacks
     * If no callback was set, this function returns NULL, in this case
     * no event should be sent to environment.
     */
    void *get_event_callback(jvmtiEvent event_type)
    {
        return ((void **)&event_table)[event_type - JVMTI_MIN_EVENT_TYPE_VAL];
    }

    /**
     * Returns pointer to a callback function that was set by SetExtensionEventCallback
     * If no callback was set, this function returns NULL, in this case
     * no event should be sent to environment.
     */
    jvmtiExtensionEvent get_extension_event_callback(jint event_id)
    {
        return extension_event_table[event_id];
    }

    jvmtiError allocate_extension_event_callbacks_table();
};

jint JNICALL create_jvmti_environment(JavaVM *vm, void **env, jint version);
jint get_thread_stack_depth(VM_thread *thread);
void jvmti_get_compilation_flags(OpenMethodExecutionParams *flags);

// Marks topmost frame of the specified thead to be popped
jvmtiError jvmti_jit_pop_frame(jthread thread);
// On current thread perform popping of topmost frame
void jvmti_jit_prepare_pop_frame();
void jvmti_jit_complete_pop_frame();
void jvmti_jit_do_pop_frame();

/* Events functions */

#ifdef __cplusplus
extern "C" {
#endif

bool jvmti_should_report_event(jvmtiEvent event_type);
void jvmti_send_vm_start_event(Global_Env *env, JNIEnv *jni_env);
void jvmti_send_vm_init_event(Global_Env *env);
void jvmti_send_region_compiled_method_load_event(Method *method, U_32 codeSize, 
                                  const void* codeAddr, U_32 mapLength, 
                                  const AddrLocation* addrLocationMap, 
                                  const void* compileInfo);
void jvmti_send_chunks_compiled_method_load_event(Method *method);
void jvmti_send_inlined_compiled_method_load_event(Method *method);
void jvmti_send_dynamic_code_generated_event(const char *name, const void *address, jint length);
VMEXPORT void jvmti_send_contended_enter_or_entered_monitor_event(jobject obj, int isEnter);
VMEXPORT void jvmti_send_wait_monitor_event(jobject obj, jlong timeout);
VMEXPORT void jvmti_send_waited_monitor_event(jobject obj, jboolean is_timed_out);
void jvmti_send_exception_event(jthrowable exn_object,
    Method *method, jlocation location,
    Method *catch_method, jlocation catch_location);
void jvmti_send_class_load_event(const Global_Env* env, Class* clss);
void jvmti_send_class_file_load_hook_event(const Global_Env* env,
    ClassLoader* loader,
    const char* classname,
    int classlen, unsigned char* classbytes,
    int* newclasslen, unsigned char** newclass);
void jvmti_send_class_prepare_event(Class* clss);
VMEXPORT void jvmti_send_thread_start_end_event(vm_thread_t thread, int is_start);
void jvmti_send_vm_death_event();
void jvmti_send_gc_finish_event();
void jvmti_send_gc_start_event();
bool jvmti_jit_breakpoint_handler(Registers *regs);
VMEXPORT void jvmti_process_native_method_bind_event(jmethodID method, 
    NativeCodePtr address, NativeCodePtr* new_address_ptr);
void jvmti_clean_reclaimed_object_tags();
void jvmti_create_event_thread();
void jvmti_destroy_event_thread();
void jvmti_notify_data_dump_request();

#ifdef __cplusplus
}
#endif

/* External events functions */
VMEXPORT void jvmti_interpreter_exception_event_callback_call(
        ManagedObject *exc, Method *method, jlocation location,
        Method *catch_method, jlocation catch_location);
bool jvmti_is_exception_event_requested();
ManagedObject *jvmti_jit_exception_event_callback_call(ManagedObject *exn,
    JIT *jit, Method *method, NativeCodePtr native_location,
    JIT *catch_jit, Method *catch_method, NativeCodePtr native_catch_location);
VMEXPORT void jvmti_interpreter_exception_catch_event_callback_call(
    ManagedObject *exc, Method *catch_method, jlocation catch_location);
ManagedObject *jvmti_jit_exception_catch_event_callback_call(ManagedObject
    *exn_object, JIT *catch_jit, Method *catch_method,
    NativeCodePtr native_catch_location);
VMEXPORT void jvmti_process_method_entry_event(jmethodID method);
VMEXPORT void jvmti_process_method_exit_event(jmethodID method, jboolean exn_flag, jvalue ret_val);
VMEXPORT void jvmti_process_method_exception_exit_event(jmethodID method, jboolean exn_flag, jvalue ret_val, StackIterator* si);
VMEXPORT void jvmti_process_field_access_event(Field_Handle field,
    jmethodID method, jlocation location, ManagedObject* object);
VMEXPORT void jvmti_process_field_modification_event(Field_Handle field,
    jmethodID method, jlocation location, ManagedObject* object, jvalue new_value);
VMEXPORT Managed_Object_Handle vm_alloc_and_report_ti(unsigned size, 
    Allocation_Handle p_vtable, void *thread_pointer, Class* object_class);

#ifdef __cplusplus
extern "C" {
#endif

    extern struct JNINativeInterface_ jni_vtable;

    /*   2 : Set Event Notification Mode */
    VMEXPORT jvmtiError JNICALL jvmtiSetEventNotificationMode (jvmtiEnv* env,
        jvmtiEventMode mode,
        jvmtiEvent event_type,
        jthread event_thread,
        ...);

    /*   4 : Get All Threads */
    VMEXPORT jvmtiError JNICALL jvmtiGetAllThreads (jvmtiEnv* env,
        jint* threads_count_ptr,
        jthread** threads_ptr);

    /*   5 : Suspend Thread */
    VMEXPORT jvmtiError JNICALL jvmtiSuspendThread (jvmtiEnv* env,
        jthread thread);

    /*   6 : Resume Thread */
    VMEXPORT jvmtiError JNICALL jvmtiResumeThread (jvmtiEnv* env,
        jthread thread);

    /*   7 : Stop Thread */
    VMEXPORT jvmtiError JNICALL jvmtiStopThread (jvmtiEnv* env,
        jthread thread,
        jobject exception);

    /*   8 : Interrupt Thread */
    VMEXPORT jvmtiError JNICALL jvmtiInterruptThread (jvmtiEnv* env,
        jthread thread);

    /*   9 : Get Thread Info */
    VMEXPORT jvmtiError JNICALL jvmtiGetThreadInfo (jvmtiEnv* env,
        jthread thread,
        jvmtiThreadInfo* info_ptr);

    /*   10 : Get Owned Monitor Info */
    VMEXPORT jvmtiError JNICALL jvmtiGetOwnedMonitorInfo (jvmtiEnv* env,
        jthread thread,
        jint* owned_monitor_count_ptr,
        jobject** owned_monitors_ptr);

    /*   11 : Get Current Contended Monitor */
    VMEXPORT jvmtiError JNICALL jvmtiGetCurrentContendedMonitor (jvmtiEnv* env,
        jthread thread,
        jobject* monitor_ptr);

    /*   12 : Run Agent Thread */
    VMEXPORT jvmtiError JNICALL jvmtiRunAgentThread (jvmtiEnv* env,
        jthread thread,
        jvmtiStartFunction proc,
        const void* arg,
        jint priority);

    /*   13 : Get Top Thread Groups */
    VMEXPORT jvmtiError JNICALL jvmtiGetTopThreadGroups (jvmtiEnv* env,
        jint* group_count_ptr,
        jthreadGroup** groups_ptr);

    /*   14 : Get Thread Group Info */
    VMEXPORT jvmtiError JNICALL jvmtiGetThreadGroupInfo (jvmtiEnv* env,
        jthreadGroup group,
        jvmtiThreadGroupInfo* info_ptr);

    /*   15 : Get Thread Group Children */
    VMEXPORT jvmtiError JNICALL jvmtiGetThreadGroupChildren (jvmtiEnv* env,
        jthreadGroup group,
        jint* thread_count_ptr,
        jthread** threads_ptr,
        jint* group_count_ptr,
        jthreadGroup** groups_ptr);

    /*   16 : Get Frame Count */
    VMEXPORT jvmtiError JNICALL jvmtiGetFrameCount (jvmtiEnv* env,
        jthread thread,
        jint* count_ptr);

    /*   17 : Get Thread State */
    VMEXPORT jvmtiError JNICALL jvmtiGetThreadState (jvmtiEnv* env,
        jthread thread,
        jint* thread_state_ptr);

    /*   19 : Get Frame Location */
    VMEXPORT jvmtiError JNICALL jvmtiGetFrameLocation (jvmtiEnv* env,
        jthread thread,
        jint depth,
        jmethodID* method_ptr,
        jlocation* location_ptr);

    /*   20 : Notify Frame Pop */
    VMEXPORT jvmtiError JNICALL jvmtiNotifyFramePop (jvmtiEnv* env,
        jthread thread,
        jint depth);

    /*   21 : Get Local Variable - Object */
    VMEXPORT jvmtiError JNICALL jvmtiGetLocalObject (jvmtiEnv* env,
        jthread thread,
        jint depth,
        jint slot,
        jobject* value_ptr);

    /*   22 : Get Local Variable - Int */
    VMEXPORT jvmtiError JNICALL jvmtiGetLocalInt (jvmtiEnv* env,
        jthread thread,
        jint depth,
        jint slot,
        jint* value_ptr);

    /*   23 : Get Local Variable - Long */
    VMEXPORT jvmtiError JNICALL jvmtiGetLocalLong (jvmtiEnv* env,
        jthread thread,
        jint depth,
        jint slot,
        jlong* value_ptr);

    /*   24 : Get Local Variable - Float */
    VMEXPORT jvmtiError JNICALL jvmtiGetLocalFloat (jvmtiEnv* env,
        jthread thread,
        jint depth,
        jint slot,
        jfloat* value_ptr);

    /*   25 : Get Local Variable - Double */
    VMEXPORT jvmtiError JNICALL jvmtiGetLocalDouble (jvmtiEnv* env,
        jthread thread,
        jint depth,
        jint slot,
        jdouble* value_ptr);

    /*   26 : Set Local Variable - Object */
    VMEXPORT jvmtiError JNICALL jvmtiSetLocalObject (jvmtiEnv* env,
        jthread thread,
        jint depth,
        jint slot,
        jobject value);

    /*   27 : Set Local Variable - Int */
    VMEXPORT jvmtiError JNICALL jvmtiSetLocalInt (jvmtiEnv* env,
        jthread thread,
        jint depth,
        jint slot,
        jint value);

    /*   28 : Set Local Variable - Long */
    VMEXPORT jvmtiError JNICALL jvmtiSetLocalLong (jvmtiEnv* env,
        jthread thread,
        jint depth,
        jint slot,
        jlong value);

    /*   29 : Set Local Variable - Float */
    VMEXPORT jvmtiError JNICALL jvmtiSetLocalFloat (jvmtiEnv* env,
        jthread thread,
        jint depth,
        jint slot,
        jfloat value);

    /*   30 : Set Local Variable - Double */
    VMEXPORT jvmtiError JNICALL jvmtiSetLocalDouble (jvmtiEnv* env,
        jthread thread,
        jint depth,
        jint slot,
        jdouble value);

    /*   31 : Create Raw Monitor */
    VMEXPORT jvmtiError JNICALL jvmtiCreateRawMonitor (jvmtiEnv* env,
        const char* name,
        jrawMonitorID* monitor_ptr);

    /*   32 : Destroy Raw Monitor */
    VMEXPORT jvmtiError JNICALL jvmtiDestroyRawMonitor (jvmtiEnv* env,
        jrawMonitorID monitor);

    /*   33 : Raw Monitor Enter */
    VMEXPORT jvmtiError JNICALL jvmtiRawMonitorEnter (jvmtiEnv* env,
        jrawMonitorID monitor);

    /*   34 : Raw Monitor Exit */
    VMEXPORT jvmtiError JNICALL jvmtiRawMonitorExit (jvmtiEnv* env,
        jrawMonitorID monitor);

    /*   35 : Raw Monitor Wait */
    VMEXPORT jvmtiError JNICALL jvmtiRawMonitorWait (jvmtiEnv* env,
        jrawMonitorID monitor,
        jlong millis);

    /*   36 : Raw Monitor Notify */
    VMEXPORT jvmtiError JNICALL jvmtiRawMonitorNotify (jvmtiEnv* env,
        jrawMonitorID monitor);

    /*   37 : Raw Monitor Notify All */
    VMEXPORT jvmtiError JNICALL jvmtiRawMonitorNotifyAll (jvmtiEnv* env,
        jrawMonitorID monitor);

    /*   38 : Set Breakpoint */
    VMEXPORT jvmtiError JNICALL jvmtiSetBreakpoint (jvmtiEnv* env,
        jmethodID method,
        jlocation location);

    /*   39 : Clear Breakpoint */
    VMEXPORT jvmtiError JNICALL jvmtiClearBreakpoint (jvmtiEnv* env,
        jmethodID method,
        jlocation location);

    /*   41 : Set Field Access Watch */
    VMEXPORT jvmtiError JNICALL jvmtiSetFieldAccessWatch (jvmtiEnv* env,
        jclass klass,
        jfieldID field);

    /*   42 : Clear Field Access Watch */
    VMEXPORT jvmtiError JNICALL jvmtiClearFieldAccessWatch (jvmtiEnv* env,
        jclass klass,
        jfieldID field);

    /*   43 : Set Field Modification Watch */
    VMEXPORT jvmtiError JNICALL jvmtiSetFieldModificationWatch (jvmtiEnv* env,
        jclass klass,
        jfieldID field);

    /*   44 : Clear Field Modification Watch */
    VMEXPORT jvmtiError JNICALL jvmtiClearFieldModificationWatch (jvmtiEnv* env,
        jclass klass,
        jfieldID field);

    /*   46 : Allocate */
    VMEXPORT jvmtiError JNICALL jvmtiAllocate (jvmtiEnv* env,
        jlong size,
        unsigned char** mem_ptr);

    /*   47 : Deallocate */
    VMEXPORT jvmtiError JNICALL jvmtiDeallocate (jvmtiEnv* env,
        unsigned char* mem);

    /*   48 : Get Class Signature */
    VMEXPORT jvmtiError JNICALL jvmtiGetClassSignature (jvmtiEnv* env,
        jclass klass,
        char** signature_ptr,
        char** generic_ptr);

    /*   49 : Get Class Status */
    VMEXPORT jvmtiError JNICALL jvmtiGetClassStatus (jvmtiEnv* env,
        jclass klass,
        jint* status_ptr);

    /*   50 : Get Source File Name */
    VMEXPORT jvmtiError JNICALL jvmtiGetSourceFileName (jvmtiEnv* env,
        jclass klass,
        char** source_name_ptr);

    /*   51 : Get Class Modifiers */
    VMEXPORT jvmtiError JNICALL jvmtiGetClassModifiers (jvmtiEnv* env,
        jclass klass,
        jint* modifiers_ptr);

    /*   52 : Get Class Methods */
    VMEXPORT jvmtiError JNICALL jvmtiGetClassMethods (jvmtiEnv* env,
        jclass klass,
        jint* method_count_ptr,
        jmethodID** methods_ptr);

    /*   53 : Get Class Fields */
    VMEXPORT jvmtiError JNICALL jvmtiGetClassFields (jvmtiEnv* env,
        jclass klass,
        jint* field_count_ptr,
        jfieldID** fields_ptr);

    /*   54 : Get Implemented Interfaces */
    VMEXPORT jvmtiError JNICALL jvmtiGetImplementedInterfaces (jvmtiEnv* env,
        jclass klass,
        jint* interface_count_ptr,
        jclass** interfaces_ptr);

    /*   55 : Is Interface */
    VMEXPORT jvmtiError JNICALL jvmtiIsInterface (jvmtiEnv* env,
        jclass klass,
        jboolean* is_interface_ptr);

    /*   56 : Is Array Class */
    VMEXPORT jvmtiError JNICALL jvmtiIsArrayClass (jvmtiEnv* env,
        jclass klass,
        jboolean* is_array_class_ptr);

    /*   57 : Get Class Loader */
    VMEXPORT jvmtiError JNICALL jvmtiGetClassLoader (jvmtiEnv* env,
        jclass klass,
        jobject* classloader_ptr);

    /*   58 : Get Object Hash Code */
    VMEXPORT jvmtiError JNICALL jvmtiGetObjectHashCode (jvmtiEnv* env,
        jobject object,
        jint* hash_code_ptr);

    /*   59 : Get Object Monitor Usage */
    VMEXPORT jvmtiError JNICALL jvmtiGetObjectMonitorUsage (jvmtiEnv* env,
        jobject object,
        jvmtiMonitorUsage* info_ptr);

    /*   60 : Get Field Name (and Signature) */
    VMEXPORT jvmtiError JNICALL jvmtiGetFieldName (jvmtiEnv* env,
        jclass klass,
        jfieldID field,
        char** name_ptr,
        char** signature_ptr,
        char** generic_ptr);

    /*   61 : Get Field Declaring Class */
    VMEXPORT jvmtiError JNICALL jvmtiGetFieldDeclaringClass (jvmtiEnv* env,
        jclass klass,
        jfieldID field,
        jclass* declaring_class_ptr);

    /*   62 : Get Field Modifiers */
    VMEXPORT jvmtiError JNICALL jvmtiGetFieldModifiers (jvmtiEnv* env,
        jclass klass,
        jfieldID field,
        jint* modifiers_ptr);

    /*   63 : Is Field Synthetic */
    VMEXPORT jvmtiError JNICALL jvmtiIsFieldSynthetic (jvmtiEnv* env,
        jclass klass,
        jfieldID field,
        jboolean* is_synthetic_ptr);

    /*   64 : Get Method Name (and Signature) */
    VMEXPORT jvmtiError JNICALL jvmtiGetMethodName (jvmtiEnv* env,
        jmethodID method,
        char** name_ptr,
        char** signature_ptr,
        char** generic_ptr);

    /*   65 : Get Method Declaring Class */
    VMEXPORT jvmtiError JNICALL jvmtiGetMethodDeclaringClass (jvmtiEnv* env,
        jmethodID method,
        jclass* declaring_class_ptr);

    /*   66 : Get Method Modifiers */
    VMEXPORT jvmtiError JNICALL jvmtiGetMethodModifiers (jvmtiEnv* env,
        jmethodID method,
        jint* modifiers_ptr);

    /*   68 : Get Max Locals */
    VMEXPORT jvmtiError JNICALL jvmtiGetMaxLocals (jvmtiEnv* env,
        jmethodID method,
        jint* max_ptr);

    /*   69 : Get Arguments Size */
    VMEXPORT jvmtiError JNICALL jvmtiGetArgumentsSize (jvmtiEnv* env,
        jmethodID method,
        jint* size_ptr);

    /*   70 : Get Line Number Table */
    VMEXPORT jvmtiError JNICALL jvmtiGetLineNumberTable (jvmtiEnv* env,
        jmethodID method,
        jint* entry_count_ptr,
        jvmtiLineNumberEntry** table_ptr);

    /*   71 : Get Method Location */
    VMEXPORT jvmtiError JNICALL jvmtiGetMethodLocation (jvmtiEnv* env,
        jmethodID method,
        jlocation* start_location_ptr,
        jlocation* end_location_ptr);

    /*   72 : Get Local Variable Table */
    VMEXPORT jvmtiError JNICALL jvmtiGetLocalVariableTable (jvmtiEnv* env,
        jmethodID method,
        jint* entry_count_ptr,
        jvmtiLocalVariableEntry** table_ptr);

    /*   75 : Get Bytecodes */
    VMEXPORT jvmtiError JNICALL jvmtiGetBytecodes (jvmtiEnv* env,
        jmethodID method,
        jint* bytecode_count_ptr,
        unsigned char** bytecodes_ptr);

    /*   76 : Is Method Native */
    VMEXPORT jvmtiError JNICALL jvmtiIsMethodNative (jvmtiEnv* env,
        jmethodID method,
        jboolean* is_native_ptr);

    /*   77 : Is Method Synthetic */
    VMEXPORT jvmtiError JNICALL jvmtiIsMethodSynthetic (jvmtiEnv* env,
        jmethodID method,
        jboolean* is_synthetic_ptr);

    /*   78 : Get Loaded Classes */
    VMEXPORT jvmtiError JNICALL jvmtiGetLoadedClasses (jvmtiEnv* env,
        jint* class_count_ptr,
        jclass** classes_ptr);

    /*   79 : Get Classloader Classes */
    VMEXPORT jvmtiError JNICALL jvmtiGetClassLoaderClasses (jvmtiEnv* env,
        jobject initiating_loader,
        jint* class_count_ptr,
        jclass** classes_ptr);

    /*   80 : Pop Frame */
    VMEXPORT jvmtiError JNICALL jvmtiPopFrame (jvmtiEnv* env,
        jthread thread);

    /*   87 : Redefine Classes */
    VMEXPORT jvmtiError JNICALL jvmtiRedefineClasses (jvmtiEnv* env,
        jint class_count,
        const jvmtiClassDefinition* class_definitions);

    /*   88 : Get Version Number */
    VMEXPORT jvmtiError JNICALL jvmtiGetVersionNumber (jvmtiEnv* env,
        jint* version_ptr);

    /*   89 : Get Capabilities */
    VMEXPORT jvmtiError JNICALL jvmtiGetCapabilities (jvmtiEnv* env,
        jvmtiCapabilities* capabilities_ptr);

    /*   90 : Get Source Debug Extension */
    VMEXPORT jvmtiError JNICALL jvmtiGetSourceDebugExtension (jvmtiEnv* env,
        jclass klass,
        char** source_debug_extension_ptr);

    /*   91 : Is Method Obsolete */
    VMEXPORT jvmtiError JNICALL jvmtiIsMethodObsolete (jvmtiEnv* env,
        jmethodID method,
        jboolean* is_obsolete_ptr);

    /*   92 : Suspend Thread List */
    VMEXPORT jvmtiError JNICALL jvmtiSuspendThreadList (jvmtiEnv* env,
        jint request_count,
        const jthread* request_list,
        jvmtiError* results);

    /*   93 : Resume Thread List */
    VMEXPORT jvmtiError JNICALL jvmtiResumeThreadList (jvmtiEnv* env,
        jint request_count,
        const jthread* request_list,
        jvmtiError* results);

    /*   100 : Get All Stack Traces */
    VMEXPORT jvmtiError JNICALL jvmtiGetAllStackTraces (jvmtiEnv* env,
        jint max_frame_count,
        jvmtiStackInfo** stack_info_ptr,
        jint* thread_count_ptr);

    /*   101 : Get Thread List Stack Traces */
    VMEXPORT jvmtiError JNICALL jvmtiGetThreadListStackTraces (jvmtiEnv* env,
        jint thread_count,
        const jthread* thread_list,
        jint max_frame_count,
        jvmtiStackInfo** stack_info_ptr);

    /*   102 : Get Thread Local Storage */
    VMEXPORT jvmtiError JNICALL jvmtiGetThreadLocalStorage (jvmtiEnv* env,
        jthread thread,
        void** data_ptr);

    /*   103 : Set Thread Local Storage */
    VMEXPORT jvmtiError JNICALL jvmtiSetThreadLocalStorage (jvmtiEnv* env,
        jthread thread,
        const void* data);

    /*   104 : Get Stack Trace */
    VMEXPORT jvmtiError JNICALL jvmtiGetStackTrace (jvmtiEnv* env,
        jthread thread,
        jint start_depth,
        jint max_frame_count,
        jvmtiFrameInfo* frame_buffer,
        jint* count_ptr);

    /*   106 : Get Tag */
    VMEXPORT jvmtiError JNICALL jvmtiGetTag (jvmtiEnv* env,
        jobject object,
        jlong* tag_ptr);

    /*   107 : Set Tag */
    VMEXPORT jvmtiError JNICALL jvmtiSetTag (jvmtiEnv* env,
        jobject object,
        jlong tag);

    /*   108 : Force Garbage Collection */
    VMEXPORT jvmtiError JNICALL jvmtiForceGarbageCollection (jvmtiEnv* env);

    /*   109 : Iterate Over Objects Reachable From Object */
    VMEXPORT jvmtiError JNICALL jvmtiIterateOverObjectsReachableFromObject (jvmtiEnv* env,
        jobject object,
        jvmtiObjectReferenceCallback object_reference_callback,
        void* user_data);

    /*   110 : Iterate Over Reachable Objects */
    VMEXPORT jvmtiError JNICALL jvmtiIterateOverReachableObjects (jvmtiEnv* env,
        jvmtiHeapRootCallback heap_root_callback,
        jvmtiStackReferenceCallback stack_ref_callback,
        jvmtiObjectReferenceCallback object_ref_callback,
        void* user_data);

    /*   111 : Iterate Over Heap */
    VMEXPORT jvmtiError JNICALL jvmtiIterateOverHeap (jvmtiEnv* env,
        jvmtiHeapObjectFilter object_filter,
        jvmtiHeapObjectCallback heap_object_callback,
        void* user_data);

    /*   112 : Iterate Over Instances Of Class */
    VMEXPORT jvmtiError JNICALL jvmtiIterateOverInstancesOfClass (jvmtiEnv* env,
        jclass klass,
        jvmtiHeapObjectFilter object_filter,
        jvmtiHeapObjectCallback heap_object_callback,
        void* user_data);

    /*   114 : Get Objects With Tags */
    VMEXPORT jvmtiError JNICALL jvmtiGetObjectsWithTags (jvmtiEnv* env,
        jint tag_count,
        const jlong* tags,
        jint* count_ptr,
        jobject** object_result_ptr,
        jlong** tag_result_ptr);

    /*   120 : Set JNI Function Table */
    VMEXPORT jvmtiError JNICALL jvmtiSetJNIFunctionTable (jvmtiEnv* env,
        const jniNativeInterface* function_table);

    /*   121 : Get JNI Function Table */
    VMEXPORT jvmtiError JNICALL jvmtiGetJNIFunctionTable (jvmtiEnv* env,
        jniNativeInterface** function_table);

    /*   122 : Set Event Callbacks */
    VMEXPORT jvmtiError JNICALL jvmtiSetEventCallbacks (jvmtiEnv* env,
        const jvmtiEventCallbacks* callbacks,
        jint size_of_callbacks);

    /*   123 : Generate Events */
    VMEXPORT jvmtiError JNICALL jvmtiGenerateEvents (jvmtiEnv* env,
        jvmtiEvent event_type);

    /*   124 : Get Extension Functions */
    VMEXPORT jvmtiError JNICALL jvmtiGetExtensionFunctions (jvmtiEnv* env,
        jint* extension_count_ptr,
        jvmtiExtensionFunctionInfo** extensions);

    /*   125 : Get Extension Events */
    VMEXPORT jvmtiError JNICALL jvmtiGetExtensionEvents (jvmtiEnv* env,
        jint* extension_count_ptr,
        jvmtiExtensionEventInfo** extensions);

    /*   126 : Set Extension Event Callback */
    VMEXPORT jvmtiError JNICALL jvmtiSetExtensionEventCallback (jvmtiEnv* env,
        jint extension_event_index,
        jvmtiExtensionEvent callback);

    /*   127 : Dispose Environment */
    VMEXPORT jvmtiError JNICALL jvmtiDisposeEnvironment (jvmtiEnv* env);

    /*   128 : Get Error Name */
    VMEXPORT jvmtiError JNICALL jvmtiGetErrorName (jvmtiEnv* env,
        jvmtiError error,
        char** name_ptr);

    /*   129 : Get JLocation Format */
    VMEXPORT jvmtiError JNICALL jvmtiGetJLocationFormat (jvmtiEnv* env,
        jvmtiJlocationFormat* format_ptr);

    /*   130 : Get System Properties */
    VMEXPORT jvmtiError JNICALL jvmtiGetSystemProperties (jvmtiEnv* env,
        jint* count_ptr,
        char*** property_ptr);

    /*   131 : Get System Property */
    VMEXPORT jvmtiError JNICALL jvmtiGetSystemProperty (jvmtiEnv* env,
        const char* property,
        char** value_ptr);

    /*   132 : Set System Property */
    VMEXPORT jvmtiError JNICALL jvmtiSetSystemProperty (jvmtiEnv* env,
        const char* property,
        const char* value);

    /*   133 : Get Phase */
    VMEXPORT jvmtiError JNICALL jvmtiGetPhase (jvmtiEnv* env,
        jvmtiPhase* phase_ptr);

    /*   134 : Get Current Thread CPU Timer Information */
    VMEXPORT jvmtiError JNICALL jvmtiGetCurrentThreadCpuTimerInfo (jvmtiEnv* env,
        jvmtiTimerInfo* info_ptr);

    /*   135 : Get Current Thread CPU Time */
    VMEXPORT jvmtiError JNICALL jvmtiGetCurrentThreadCpuTime (jvmtiEnv* env,
        jlong* nanos_ptr);

    /*   136 : Get Thread CPU Timer Information */
    VMEXPORT jvmtiError JNICALL jvmtiGetThreadCpuTimerInfo (jvmtiEnv* env,
        jvmtiTimerInfo* info_ptr);

    /*   137 : Get Thread CPU Time */
    VMEXPORT jvmtiError JNICALL jvmtiGetThreadCpuTime (jvmtiEnv* env,
        jthread thread,
        jlong* nanos_ptr);

    /*   138 : Get Timer Information */
    VMEXPORT jvmtiError JNICALL jvmtiGetTimerInfo (jvmtiEnv* env,
        jvmtiTimerInfo* info_ptr);

    /*   139 : Get Time */
    VMEXPORT jvmtiError JNICALL jvmtiGetTime (jvmtiEnv* env,
        jlong* nanos_ptr);

    /*   140 : Get Potential Capabilities */
    VMEXPORT jvmtiError JNICALL jvmtiGetPotentialCapabilities (jvmtiEnv* env,
        jvmtiCapabilities* capabilities_ptr);

    /*   142 : Add Capabilities */
    VMEXPORT jvmtiError JNICALL jvmtiAddCapabilities (jvmtiEnv* env,
        const jvmtiCapabilities* capabilities_ptr);

    /*   143 : Relinquish Capabilities */
    VMEXPORT jvmtiError JNICALL jvmtiRelinquishCapabilities (jvmtiEnv* env,
        const jvmtiCapabilities* capabilities_ptr);

    /*   144 : Get Available Processors */
    VMEXPORT jvmtiError JNICALL jvmtiGetAvailableProcessors (jvmtiEnv* env,
        jint* processor_count_ptr);

    /*   147 : Get Environment Local Storage */
    VMEXPORT jvmtiError JNICALL jvmtiGetEnvironmentLocalStorage (jvmtiEnv* env,
        void** data_ptr);

    /*   148 : Set Environment Local Storage */
    VMEXPORT jvmtiError JNICALL jvmtiSetEnvironmentLocalStorage (jvmtiEnv* env,
        const void* data);

    /*   149 : Add To Bootstrap Class Loader Search */
    VMEXPORT jvmtiError JNICALL jvmtiAddToBootstrapClassLoaderSearch (jvmtiEnv* env,
        const char* segment);

    /*   150 : Set Verbose Flag */
    VMEXPORT jvmtiError JNICALL jvmtiSetVerboseFlag (jvmtiEnv* env,
        jvmtiVerboseFlag flag,
        jboolean value);

    /*   154 : Get Object Size */
    VMEXPORT jvmtiError JNICALL jvmtiGetObjectSize (jvmtiEnv* env,
        jobject object,
        jlong* size_ptr);

#ifdef __cplusplus
}
#endif
#endif
