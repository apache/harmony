/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
  
#ifndef _JVMTI_H_
#define _JVMTI_H_

#include <stdlib.h>

#include "jni.h"
#include "jvmti_types.h"

/*
 * Supported JVMTI versions
 */
/** Constant which specifies JNI interface mask */
#define JVMTI_VERSION_INTERFACE_JNI 0x00000000
/** Constant which specifies JVMTI interface mask */
#define JVMTI_VERSION_INTERFACE_JVMTI 0x30000000
/** Constant which specifies VM externals interface mask */
#define JVMTI_VERSION_MASK_INTERFACE_TYPE 0x70000000
/** Constant which specifies major JVMTI version interface mask */
#define JVMTI_VERSION_MASK_MAJOR 0x0FFF0000
/** Constant which specifies minor JVMTI version interface mask */
#define JVMTI_VERSION_MASK_MINOR 0x0000FF00
/** Constant which specifies micro JVMTI version interface mask */
#define JVMTI_VERSION_MASK_MICRO 0x000000FF
/** Constant which specifies major JVMTI version left shit */
#define JVMTI_VERSION_SHIFT_MAJOR 16
/** Constant which specifies minor JVMTI version left shit */
#define JVMTI_VERSION_SHIFT_MINOR 8
/** Constant which specifies micro JVMTI version left shit */
#define JVMTI_VERSION_SHIFT_MICRO 0

/** JVMTI major version supported by VM */
#define JVMTI_VERSION_MAJOR 1
/** JVMTI minor version supported by VM */
#define JVMTI_VERSION_MINOR 0
/** JVMTI micro version supported by VM */
#define JVMTI_VERSION_MICRO 36

/** Constant which defines JVMTI version identifier for JVMTI version
 * 1.0.0 */
#define JVMTI_VERSION_1_0 \
    (JVMTI_VERSION_INTERFACE_JVMTI | \
    (JVMTI_VERSION_MAJOR << JVMTI_VERSION_SHIFT_MAJOR) | \
    (0 << JVMTI_VERSION_SHIFT_MINOR) | \
    (0 << JVMTI_VERSION_SHIFT_MICRO))

/** Constant which defines JVMTI version identifier for JVMTI version
 * supported by VM */
#define JVMTI_VERSION \
    (JVMTI_VERSION_INTERFACE_JVMTI | \
    (JVMTI_VERSION_MAJOR << JVMTI_VERSION_SHIFT_MAJOR) | \
    (JVMTI_VERSION_MINOR << JVMTI_VERSION_SHIFT_MINOR) | \
    (JVMTI_VERSION_MICRO << JVMTI_VERSION_SHIFT_MICRO))

#ifdef __cplusplus
extern "C"
{
#endif

    /**
     * Agent StartUp function prototype which should be exported by agent library
     *
     * See <a
     * href="http://java.sun.com/j2se/1.5.0/docs/guide/jvmti/jvmti.html#onload">specification</a>
     * for details.
     */
    JNIEXPORT jint JNICALL
        Agent_OnLoad(JavaVM * vm, char *options, void *reserved);

    /**
     * Agent Shutdown prototype which should be exported by agent library
     *
     * See <a
     * href="http://java.sun.com/j2se/1.5.0/docs/guide/jvmti/jvmti.html#onunload">specification</a>
     * for details.
     */
    JNIEXPORT void JNICALL Agent_OnUnload(JavaVM * vm);

#ifdef __cplusplus
}
#endif

/**
 * JVMTI interface functions table for use in C sources
 *
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/guide/jvmti/jvmti.html#FunctionSection">specification</a>
 * for details.
 */
struct ti_interface
{
    void *reserved1;

    jvmtiError (JNICALL * SetEventNotificationMode) (jvmtiEnv * env,
        jvmtiEventMode mode,
        jvmtiEvent event_type,
        jthread event_thread,
        ...);

    void *reserved3;

    jvmtiError (JNICALL * GetAllThreads) (jvmtiEnv * env,
        jint * threads_count_ptr,
        jthread ** threads_ptr);

    jvmtiError (JNICALL * SuspendThread) (jvmtiEnv * env, jthread thread);

    jvmtiError (JNICALL * ResumeThread) (jvmtiEnv * env, jthread thread);

    jvmtiError (JNICALL * StopThread) (jvmtiEnv * env,
        jthread thread, jobject exception);

    jvmtiError (JNICALL * InterruptThread) (jvmtiEnv * env, jthread thread);

    jvmtiError (JNICALL * GetThreadInfo) (jvmtiEnv * env,
        jthread thread,
        jvmtiThreadInfo * info_ptr);

    jvmtiError (JNICALL * GetOwnedMonitorInfo) (jvmtiEnv * env,
        jthread thread,
        jint *
        owned_monitor_count_ptr,
        jobject **
        owned_monitors_ptr);

    jvmtiError (JNICALL * GetCurrentContendedMonitor) (jvmtiEnv * env,
        jthread thread,
        jobject * monitor_ptr);

    jvmtiError (JNICALL * RunAgentThread) (jvmtiEnv * env,
        jthread thread,
        jvmtiStartFunction proc,
        const void *arg, jint priority);

    jvmtiError (JNICALL * GetTopThreadGroups) (jvmtiEnv * env,
        jint * group_count_ptr,
        jthreadGroup ** groups_ptr);

    jvmtiError (JNICALL * GetThreadGroupInfo) (jvmtiEnv * env,
        jthreadGroup group,
        jvmtiThreadGroupInfo *
        info_ptr);

    jvmtiError (JNICALL * GetThreadGroupChildren) (jvmtiEnv * env,
        jthreadGroup group,
        jint * thread_count_ptr,
        jthread ** threads_ptr,
        jint * group_count_ptr,
        jthreadGroup **
        groups_ptr);

    jvmtiError (JNICALL * GetFrameCount) (jvmtiEnv * env,
        jthread thread, jint * count_ptr);

    jvmtiError (JNICALL * GetThreadState) (jvmtiEnv * env,
        jthread thread,
        jint * thread_state_ptr);

    void *reserved18;

    jvmtiError (JNICALL * GetFrameLocation) (jvmtiEnv * env,
        jthread thread,
        jint depth,
        jmethodID * method_ptr,
        jlocation * location_ptr);

    jvmtiError (JNICALL * NotifyFramePop) (jvmtiEnv * env,
        jthread thread, jint depth);

    jvmtiError (JNICALL * GetLocalObject) (jvmtiEnv * env,
        jthread thread,
        jint depth,
        jint slot, jobject * value_ptr);

    jvmtiError (JNICALL * GetLocalInt) (jvmtiEnv * env,
        jthread thread,
        jint depth,
        jint slot, jint * value_ptr);

    jvmtiError (JNICALL * GetLocalLong) (jvmtiEnv * env,
        jthread thread,
        jint depth,
        jint slot, jlong * value_ptr);

    jvmtiError (JNICALL * GetLocalFloat) (jvmtiEnv * env,
        jthread thread,
        jint depth,
        jint slot, jfloat * value_ptr);

    jvmtiError (JNICALL * GetLocalDouble) (jvmtiEnv * env,
        jthread thread,
        jint depth,
        jint slot, jdouble * value_ptr);

    jvmtiError (JNICALL * SetLocalObject) (jvmtiEnv * env,
        jthread thread,
        jint depth,
        jint slot, jobject value);

    jvmtiError (JNICALL * SetLocalInt) (jvmtiEnv * env,
        jthread thread,
        jint depth, jint slot, jint value);

    jvmtiError (JNICALL * SetLocalLong) (jvmtiEnv * env,
        jthread thread,
        jint depth, jint slot, jlong value);

    jvmtiError (JNICALL * SetLocalFloat) (jvmtiEnv * env,
        jthread thread,
        jint depth,
        jint slot, jfloat value);

    jvmtiError (JNICALL * SetLocalDouble) (jvmtiEnv * env,
        jthread thread,
        jint depth,
        jint slot, jdouble value);

    jvmtiError (JNICALL * CreateRawMonitor) (jvmtiEnv * env,
        const char *name,
        jrawMonitorID * monitor_ptr);

    jvmtiError (JNICALL * DestroyRawMonitor) (jvmtiEnv * env,
        jrawMonitorID monitor);

    jvmtiError (JNICALL * RawMonitorEnter) (jvmtiEnv * env,
        jrawMonitorID monitor);

    jvmtiError (JNICALL * RawMonitorExit) (jvmtiEnv * env,
        jrawMonitorID monitor);

    jvmtiError (JNICALL * RawMonitorWait) (jvmtiEnv * env,
        jrawMonitorID monitor,
        jlong millis);

    jvmtiError (JNICALL * RawMonitorNotify) (jvmtiEnv * env,
        jrawMonitorID monitor);

    jvmtiError (JNICALL * RawMonitorNotifyAll) (jvmtiEnv * env,
        jrawMonitorID monitor);

    jvmtiError (JNICALL * SetBreakpoint) (jvmtiEnv * env,
        jmethodID method,
        jlocation location);

    jvmtiError (JNICALL * ClearBreakpoint) (jvmtiEnv * env,
        jmethodID method,
        jlocation location);

    void *reserved40;

    jvmtiError (JNICALL * SetFieldAccessWatch) (jvmtiEnv * env,
        jclass clazz, jfieldID field);

    jvmtiError (JNICALL * ClearFieldAccessWatch) (jvmtiEnv * env,
        jclass clazz,
        jfieldID field);

    jvmtiError (JNICALL * SetFieldModificationWatch) (jvmtiEnv * env,
        jclass clazz,
        jfieldID field);

    jvmtiError (JNICALL * ClearFieldModificationWatch) (jvmtiEnv * env,
        jclass clazz,
        jfieldID field);

    void *reserved45;

    jvmtiError (JNICALL * Allocate) (jvmtiEnv * env,
        jlong size, unsigned char **mem_ptr);

    jvmtiError (JNICALL * Deallocate) (jvmtiEnv * env, unsigned char *mem);

    jvmtiError (JNICALL * GetClassSignature) (jvmtiEnv * env,
        jclass clazz,
        char **signature_ptr,
        char **generic_ptr);

    jvmtiError (JNICALL * GetClassStatus) (jvmtiEnv * env,
        jclass clazz, jint * status_ptr);

    jvmtiError (JNICALL * GetSourceFileName) (jvmtiEnv * env,
        jclass clazz,
        char **source_name_ptr);

    jvmtiError (JNICALL * GetClassModifiers) (jvmtiEnv * env,
        jclass clazz,
        jint * modifiers_ptr);

    jvmtiError (JNICALL * GetClassMethods) (jvmtiEnv * env,
        jclass clazz,
        jint * method_count_ptr,
        jmethodID ** methods_ptr);

    jvmtiError (JNICALL * GetClassFields) (jvmtiEnv * env,
        jclass clazz,
        jint * field_count_ptr,
        jfieldID ** fields_ptr);

    jvmtiError (JNICALL * GetImplementedInterfaces) (jvmtiEnv * env,
        jclass clazz,
        jint *
        interface_count_ptr,
        jclass **
        interfaces_ptr);

    jvmtiError (JNICALL * IsInterface) (jvmtiEnv * env,
        jclass clazz,
        jboolean * is_interface_ptr);

    jvmtiError (JNICALL * IsArrayClass) (jvmtiEnv * env,
        jclass clazz,
        jboolean * is_array_class_ptr);

    jvmtiError (JNICALL * GetClassLoader) (jvmtiEnv * env,
        jclass clazz,
        jobject * classloader_ptr);

    jvmtiError (JNICALL * GetObjectHashCode) (jvmtiEnv * env,
        jobject object,
        jint * hash_code_ptr);

    jvmtiError (JNICALL * GetObjectMonitorUsage) (jvmtiEnv * env,
        jobject object,
        jvmtiMonitorUsage *
        info_ptr);

    jvmtiError (JNICALL * GetFieldName) (jvmtiEnv * env,
        jclass clazz,
        jfieldID field,
        char **name_ptr,
        char **signature_ptr,
        char **generic_ptr);

    jvmtiError (JNICALL * GetFieldDeclaringClass) (jvmtiEnv * env,
        jclass clazz,
        jfieldID field,
        jclass *
        declaring_class_ptr);

    jvmtiError (JNICALL * GetFieldModifiers) (jvmtiEnv * env,
        jclass clazz,
        jfieldID field,
        jint * modifiers_ptr);

    jvmtiError (JNICALL * IsFieldSynthetic) (jvmtiEnv * env,
        jclass clazz,
        jfieldID field,
        jboolean * is_synthetic_ptr);

    jvmtiError (JNICALL * GetMethodName) (jvmtiEnv * env,
        jmethodID method,
        char **name_ptr,
        char **signature_ptr,
        char **generic_ptr);

    jvmtiError (JNICALL * GetMethodDeclaringClass) (jvmtiEnv * env,
        jmethodID method,
        jclass *
        declaring_class_ptr);

    jvmtiError (JNICALL * GetMethodModifiers) (jvmtiEnv * env,
        jmethodID method,
        jint * modifiers_ptr);

    void *reserved67;

    jvmtiError (JNICALL * GetMaxLocals) (jvmtiEnv * env,
        jmethodID method, jint * max_ptr);

    jvmtiError (JNICALL * GetArgumentsSize) (jvmtiEnv * env,
        jmethodID method,
        jint * size_ptr);

    jvmtiError (JNICALL * GetLineNumberTable) (jvmtiEnv * env,
        jmethodID method,
        jint * entry_count_ptr,
        jvmtiLineNumberEntry **
        table_ptr);

    jvmtiError (JNICALL * GetMethodLocation) (jvmtiEnv * env,
        jmethodID method,
        jlocation * start_location_ptr,
        jlocation * end_location_ptr);

    jvmtiError (JNICALL * GetLocalVariableTable) (jvmtiEnv * env,
        jmethodID method,
        jint * entry_count_ptr,
        jvmtiLocalVariableEntry **
        table_ptr);

    void *reserved73;

    void *reserved74;

    jvmtiError (JNICALL * GetBytecodes) (jvmtiEnv * env,
        jmethodID method,
        jint * bytecode_count_ptr,
        unsigned char **bytecodes_ptr);

    jvmtiError (JNICALL * IsMethodNative) (jvmtiEnv * env,
        jmethodID method,
        jboolean * is_native_ptr);

    jvmtiError (JNICALL * IsMethodSynthetic) (jvmtiEnv * env,
        jmethodID method,
        jboolean * is_synthetic_ptr);

    jvmtiError (JNICALL * GetLoadedClasses) (jvmtiEnv * env,
        jint * class_count_ptr,
        jclass ** classes_ptr);

    jvmtiError (JNICALL * GetClassLoaderClasses) (jvmtiEnv * env,
        jobject initiating_loader,
        jint * class_count_ptr,
        jclass ** classes_ptr);

    jvmtiError (JNICALL * PopFrame) (jvmtiEnv * env, jthread thread);

    void *reserved81;

    void *reserved82;

    void *reserved83;

    void *reserved84;

    void *reserved85;

    void *reserved86;

    jvmtiError (JNICALL * RedefineClasses) (jvmtiEnv * env,
        jint class_count,
        const jvmtiClassDefinition *
        class_definitions);

    jvmtiError (JNICALL * GetVersionNumber) (jvmtiEnv * env,
        jint * version_ptr);

    jvmtiError (JNICALL * GetCapabilities) (jvmtiEnv * env,
        jvmtiCapabilities *
        capabilities_ptr);

    jvmtiError (JNICALL * GetSourceDebugExtension) (jvmtiEnv * env,
        jclass clazz,
        char
        **source_debug_extension_ptr);

    jvmtiError (JNICALL * IsMethodObsolete) (jvmtiEnv * env,
        jmethodID method,
        jboolean * is_obsolete_ptr);

    jvmtiError (JNICALL * SuspendThreadList) (jvmtiEnv * env,
        jint request_count,
        const jthread * request_list,
        jvmtiError * results);

    jvmtiError (JNICALL * ResumeThreadList) (jvmtiEnv * env,
        jint request_count,
        const jthread * request_list,
        jvmtiError * results);

    void *reserved94;

    void *reserved95;

    void *reserved96;

    void *reserved97;

    void *reserved98;

    void *reserved99;

    jvmtiError (JNICALL * GetAllStackTraces) (jvmtiEnv * env,
        jint max_frame_count,
        jvmtiStackInfo **
        stack_info_ptr,
        jint * thread_count_ptr);

    jvmtiError (JNICALL * GetThreadListStackTraces) (jvmtiEnv * env,
        jint thread_count,
        const jthread *
        thread_list,
        jint max_frame_count,
        jvmtiStackInfo **
        stack_info_ptr);

    jvmtiError (JNICALL * GetThreadLocalStorage) (jvmtiEnv * env,
        jthread thread,
        void **data_ptr);

    jvmtiError (JNICALL * SetThreadLocalStorage) (jvmtiEnv * env,
        jthread thread,
        const void *data);

    jvmtiError (JNICALL * GetStackTrace) (jvmtiEnv * env,
        jthread thread,
        jint start_depth,
        jint max_frame_count,
        jvmtiFrameInfo * frame_buffer,
        jint * count_ptr);

    void *reserved105;

    jvmtiError (JNICALL * GetTag) (jvmtiEnv * env,
        jobject object, jlong * tag_ptr);

    jvmtiError (JNICALL * SetTag) (jvmtiEnv * env, jobject object, jlong tag);

    jvmtiError (JNICALL * ForceGarbageCollection) (jvmtiEnv * env);

    jvmtiError (JNICALL * IterateOverObjectsReachableFromObject) (jvmtiEnv *
        env,
        jobject
        object,
        jvmtiObjectReferenceCallback
        object_reference_callback,
        void
        *user_data);

    jvmtiError (JNICALL * IterateOverReachableObjects) (jvmtiEnv * env,
        jvmtiHeapRootCallback
        heap_root_callback,
        jvmtiStackReferenceCallback
        stack_ref_callback,
        jvmtiObjectReferenceCallback
        object_ref_callback,
        void *user_data);

    jvmtiError (JNICALL * IterateOverHeap) (jvmtiEnv * env,
        jvmtiHeapObjectFilter
        object_filter,
        jvmtiHeapObjectCallback
        heap_object_callback,
        void *user_data);

    jvmtiError (JNICALL * IterateOverInstancesOfClass) (jvmtiEnv * env,
        jclass clazz,
        jvmtiHeapObjectFilter
        object_filter,
        jvmtiHeapObjectCallback
        heap_object_callback,
        void *user_data);

    void *reserved113;

    jvmtiError (JNICALL * GetObjectsWithTags) (jvmtiEnv * env,
        jint tag_count,
        const jlong * tags,
        jint * count_ptr,
        jobject ** object_result_ptr,
        jlong ** tag_result_ptr);

    void *reserved115;

    void *reserved116;

    void *reserved117;

    void *reserved118;

    void *reserved119;

    jvmtiError (JNICALL * SetJNIFunctionTable) (jvmtiEnv * env,
        const jniNativeInterface *
        function_table);

    jvmtiError (JNICALL * GetJNIFunctionTable) (jvmtiEnv * env,
        jniNativeInterface **
        function_table);

    jvmtiError (JNICALL * SetEventCallbacks) (jvmtiEnv * env,
        const jvmtiEventCallbacks *
        callbacks,
        jint size_of_callbacks);

    jvmtiError (JNICALL * GenerateEvents) (jvmtiEnv * env,
        jvmtiEvent event_type);

    jvmtiError (JNICALL * GetExtensionFunctions) (jvmtiEnv * env,
        jint * extension_count_ptr,
        jvmtiExtensionFunctionInfo
        ** extensions);

    jvmtiError (JNICALL * GetExtensionEvents) (jvmtiEnv * env,
        jint * extension_count_ptr,
        jvmtiExtensionEventInfo **
        extensions);

    jvmtiError (JNICALL * SetExtensionEventCallback) (jvmtiEnv * env,
        jint
        extension_event_index,
        jvmtiExtensionEvent
        callback);

    jvmtiError (JNICALL * DisposeEnvironment) (jvmtiEnv * env);

    jvmtiError (JNICALL * GetErrorName) (jvmtiEnv * env,
        jvmtiError error, char **name_ptr);

    jvmtiError (JNICALL * GetJLocationFormat) (jvmtiEnv * env,
        jvmtiJlocationFormat *
        format_ptr);

    jvmtiError (JNICALL * GetSystemProperties) (jvmtiEnv * env,
        jint * count_ptr,
        char ***property_ptr);

    jvmtiError (JNICALL * GetSystemProperty) (jvmtiEnv * env,
        const char *property,
        char **value_ptr);

    jvmtiError (JNICALL * SetSystemProperty) (jvmtiEnv * env,
        const char *property,
        const char *value);

    jvmtiError (JNICALL * GetPhase) (jvmtiEnv * env, jvmtiPhase * phase_ptr);

    jvmtiError (JNICALL * GetCurrentThreadCpuTimerInfo) (jvmtiEnv * env,
        jvmtiTimerInfo *
        info_ptr);

    jvmtiError (JNICALL * GetCurrentThreadCpuTime) (jvmtiEnv * env,
        jlong * nanos_ptr);

    jvmtiError (JNICALL * GetThreadCpuTimerInfo) (jvmtiEnv * env,
        jvmtiTimerInfo * info_ptr);

    jvmtiError (JNICALL * GetThreadCpuTime) (jvmtiEnv * env,
        jthread thread,
        jlong * nanos_ptr);

    jvmtiError (JNICALL * GetTimerInfo) (jvmtiEnv * env,
        jvmtiTimerInfo * info_ptr);

    jvmtiError (JNICALL * GetTime) (jvmtiEnv * env, jlong * nanos_ptr);


    jvmtiError (JNICALL * GetPotentialCapabilities) (jvmtiEnv * env,
        jvmtiCapabilities *
        capabilities_ptr);

    void *reserved141;

    jvmtiError (JNICALL * AddCapabilities) (jvmtiEnv * env,
        const jvmtiCapabilities *
        capabilities_ptr);

    jvmtiError (JNICALL * RelinquishCapabilities) (jvmtiEnv * env,
        const jvmtiCapabilities *
        capabilities_ptr);

    jvmtiError (JNICALL * GetAvailableProcessors) (jvmtiEnv * env,
        jint *
        processor_count_ptr);

    void *reserved145;

    void *reserved146;

    jvmtiError (JNICALL * GetEnvironmentLocalStorage) (jvmtiEnv * env,
        void **data_ptr);

    jvmtiError (JNICALL * SetEnvironmentLocalStorage) (jvmtiEnv * env,
        const void *data);

    jvmtiError (JNICALL * AddToBootstrapClassLoaderSearch) (jvmtiEnv * env,
        const char
        *segment);

    jvmtiError (JNICALL * SetVerboseFlag) (jvmtiEnv * env,
        jvmtiVerboseFlag flag,
        jboolean value);

    void *reserved151;

    void *reserved152;

    void *reserved153;

    jvmtiError (JNICALL * GetObjectSize) (jvmtiEnv * env,
        jobject object, jlong * size_ptr);
};


/**
 * JVMTI interface functions table for use in C++ sources
 *
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/guide/jvmti/jvmti.html#FunctionSection">specification</a>
 * for details.
 */
struct jvmtiEnv_struct
{
    const struct ti_interface *funcs;

#ifdef __cplusplus

    jvmtiError Allocate (jlong size, unsigned char **mem_ptr)
    {
        return funcs->Allocate (this, size, mem_ptr);
    }

    jvmtiError Deallocate (unsigned char *mem)
    {
        return funcs->Deallocate (this, mem);
    }

    jvmtiError GetThreadState (jthread thread, jint * thread_state_ptr)
    {
        return funcs->GetThreadState (this, thread, thread_state_ptr);
    }

    jvmtiError GetAllThreads (jint * threads_count_ptr, jthread ** threads_ptr)
    {
        return funcs->GetAllThreads (this, threads_count_ptr, threads_ptr);
    }

    jvmtiError SuspendThread (jthread thread)
    {
        return funcs->SuspendThread (this, thread);
    }

    jvmtiError SuspendThreadList (jint request_count,
        const jthread * request_list,
        jvmtiError * results)
    {
        return funcs->SuspendThreadList (this, request_count, request_list,
            results);
    }

    jvmtiError ResumeThread (jthread thread)
    {
        return funcs->ResumeThread (this, thread);
    }

    jvmtiError ResumeThreadList (jint request_count,
        const jthread * request_list,
        jvmtiError * results)
    {
        return funcs->ResumeThreadList (this, request_count, request_list,
            results);
    }

    jvmtiError StopThread (jthread thread, jobject exception)
    {
        return funcs->StopThread (this, thread, exception);
    }

    jvmtiError InterruptThread (jthread thread)
    {
        return funcs->InterruptThread (this, thread);
    }

    jvmtiError GetThreadInfo (jthread thread, jvmtiThreadInfo * info_ptr)
    {
        return funcs->GetThreadInfo (this, thread, info_ptr);
    }

    jvmtiError GetOwnedMonitorInfo (jthread thread,
        jint * owned_monitor_count_ptr,
        jobject ** owned_monitors_ptr)
    {
        return funcs->GetOwnedMonitorInfo (this, thread,
            owned_monitor_count_ptr,
            owned_monitors_ptr);
    }

    jvmtiError GetCurrentContendedMonitor (jthread thread,
        jobject * monitor_ptr)
    {
        return funcs->GetCurrentContendedMonitor (this, thread, monitor_ptr);
    }

    jvmtiError RunAgentThread (jthread thread,
        jvmtiStartFunction proc,
        const void *arg, jint priority)
    {
        return funcs->RunAgentThread (this, thread, proc, arg, priority);
    }

    jvmtiError SetThreadLocalStorage (jthread thread, const void *data)
    {
        return funcs->SetThreadLocalStorage (this, thread, data);
    }

    jvmtiError GetThreadLocalStorage (jthread thread, void **data_ptr)
    {
        return funcs->GetThreadLocalStorage (this, thread, data_ptr);
    }

    jvmtiError GetTopThreadGroups (jint * group_count_ptr,
        jthreadGroup ** groups_ptr)
    {
        return funcs->GetTopThreadGroups (this, group_count_ptr, groups_ptr);
    }

    jvmtiError GetThreadGroupInfo (jthreadGroup group,
        jvmtiThreadGroupInfo * info_ptr)
    {
        return funcs->GetThreadGroupInfo (this, group, info_ptr);
    }

    jvmtiError GetThreadGroupChildren (jthreadGroup group,
        jint * thread_count_ptr,
        jthread ** threads_ptr,
        jint * group_count_ptr,
        jthreadGroup ** groups_ptr)
    {
        return funcs->GetThreadGroupChildren (this, group, thread_count_ptr,
            threads_ptr, group_count_ptr,
            groups_ptr);
    }

    jvmtiError GetStackTrace (jthread thread,
        jint start_depth,
        jint max_frame_count,
        jvmtiFrameInfo * frame_buffer, jint * count_ptr)
    {
        return funcs->GetStackTrace (this, thread, start_depth,
            max_frame_count, frame_buffer, count_ptr);
    }

    jvmtiError GetAllStackTraces (jint max_frame_count,
        jvmtiStackInfo ** stack_info_ptr,
        jint * thread_count_ptr)
    {
        return funcs->GetAllStackTraces (this, max_frame_count,
            stack_info_ptr, thread_count_ptr);
    }

    jvmtiError GetThreadListStackTraces (jint thread_count,
        const jthread * thread_list,
        jint max_frame_count,
        jvmtiStackInfo ** stack_info_ptr)
    {
        return funcs->GetThreadListStackTraces (this, thread_count,
            thread_list, max_frame_count,
            stack_info_ptr);
    }

    jvmtiError GetFrameCount (jthread thread, jint * count_ptr)
    {
        return funcs->GetFrameCount (this, thread, count_ptr);
    }

    jvmtiError PopFrame (jthread thread)
    {
        return funcs->PopFrame (this, thread);
    }

    jvmtiError GetFrameLocation (jthread thread,
        jint depth,
        jmethodID * method_ptr,
        jlocation * location_ptr)
    {
        return funcs->GetFrameLocation (this, thread, depth, method_ptr,
            location_ptr);
    }

    jvmtiError NotifyFramePop (jthread thread, jint depth)
    {
        return funcs->NotifyFramePop (this, thread, depth);
    }

    jvmtiError GetTag (jobject object, jlong * tag_ptr)
    {
        return funcs->GetTag (this, object, tag_ptr);
    }

    jvmtiError SetTag (jobject object, jlong tag)
    {
        return funcs->SetTag (this, object, tag);
    }

    jvmtiError ForceGarbageCollection ()
    {
        return funcs->ForceGarbageCollection (this);
    }

    jvmtiError IterateOverObjectsReachableFromObject (jobject object,
        jvmtiObjectReferenceCallback
        object_reference_callback,
        void *user_data)
    {
        return funcs->IterateOverObjectsReachableFromObject (this, object,
            object_reference_callback,
            user_data);
    }

    jvmtiError IterateOverReachableObjects (jvmtiHeapRootCallback
        heap_root_callback,
        jvmtiStackReferenceCallback
        stack_ref_callback,
        jvmtiObjectReferenceCallback
        object_ref_callback,
        void *user_data)
    {
        return funcs->IterateOverReachableObjects (this, heap_root_callback,
            stack_ref_callback,
            object_ref_callback,
            user_data);
    }

    jvmtiError IterateOverHeap (jvmtiHeapObjectFilter object_filter,
        jvmtiHeapObjectCallback heap_object_callback,
        void *user_data)
    {
        return funcs->IterateOverHeap (this, object_filter,
            heap_object_callback, user_data);
    }

    jvmtiError IterateOverInstancesOfClass (jclass clazz,
        jvmtiHeapObjectFilter object_filter,
        jvmtiHeapObjectCallback
        heap_object_callback,
        void *user_data)
    {
        return funcs->IterateOverInstancesOfClass (this, clazz, object_filter,
            heap_object_callback,
            user_data);
    }

    jvmtiError GetObjectsWithTags (jint tag_count,
        const jlong * tags,
        jint * count_ptr,
        jobject ** object_result_ptr,
        jlong ** tag_result_ptr)
    {
        return funcs->GetObjectsWithTags (this, tag_count, tags, count_ptr,
            object_result_ptr, tag_result_ptr);
    }

    jvmtiError GetLocalObject (jthread thread,
        jint depth, jint slot, jobject * value_ptr)
    {
        return funcs->GetLocalObject (this, thread, depth, slot, value_ptr);
    }

    jvmtiError GetLocalInt (jthread thread,
        jint depth, jint slot, jint * value_ptr)
    {
        return funcs->GetLocalInt (this, thread, depth, slot, value_ptr);
    }

    jvmtiError GetLocalLong (jthread thread,
        jint depth, jint slot, jlong * value_ptr)
    {
        return funcs->GetLocalLong (this, thread, depth, slot, value_ptr);
    }

    jvmtiError GetLocalFloat (jthread thread,
        jint depth, jint slot, jfloat * value_ptr)
    {
        return funcs->GetLocalFloat (this, thread, depth, slot, value_ptr);
    }

    jvmtiError GetLocalDouble (jthread thread,
        jint depth, jint slot, jdouble * value_ptr)
    {
        return funcs->GetLocalDouble (this, thread, depth, slot, value_ptr);
    }

    jvmtiError SetLocalObject (jthread thread,
        jint depth, jint slot, jobject value)
    {
        return funcs->SetLocalObject (this, thread, depth, slot, value);
    }

    jvmtiError SetLocalInt (jthread thread, jint depth, jint slot, jint value)
    {
        return funcs->SetLocalInt (this, thread, depth, slot, value);
    }

    jvmtiError SetLocalLong (jthread thread, jint depth, jint slot, jlong value)
    {
        return funcs->SetLocalLong (this, thread, depth, slot, value);
    }

    jvmtiError SetLocalFloat (jthread thread,
        jint depth, jint slot, jfloat value)
    {
        return funcs->SetLocalFloat (this, thread, depth, slot, value);
    }

    jvmtiError SetLocalDouble (jthread thread,
        jint depth, jint slot, jdouble value)
    {
        return funcs->SetLocalDouble (this, thread, depth, slot, value);
    }

    jvmtiError SetBreakpoint (jmethodID method, jlocation location)
    {
        return funcs->SetBreakpoint (this, method, location);
    }

    jvmtiError ClearBreakpoint (jmethodID method, jlocation location)
    {
        return funcs->ClearBreakpoint (this, method, location);
    }

    jvmtiError SetFieldAccessWatch (jclass clazz, jfieldID field)
    {
        return funcs->SetFieldAccessWatch (this, clazz, field);
    }

    jvmtiError ClearFieldAccessWatch (jclass clazz, jfieldID field)
    {
        return funcs->ClearFieldAccessWatch (this, clazz, field);
    }

    jvmtiError SetFieldModificationWatch (jclass clazz, jfieldID field)
    {
        return funcs->SetFieldModificationWatch (this, clazz, field);
    }

    jvmtiError ClearFieldModificationWatch (jclass clazz, jfieldID field)
    {
        return funcs->ClearFieldModificationWatch (this, clazz, field);
    }

    jvmtiError GetLoadedClasses (jint * class_count_ptr, jclass ** classes_ptr)
    {
        return funcs->GetLoadedClasses (this, class_count_ptr, classes_ptr);
    }

    jvmtiError GetClassLoaderClasses (jobject initiating_loader,
        jint * class_count_ptr,
        jclass ** classes_ptr)
    {
        return funcs->GetClassLoaderClasses (this, initiating_loader,
            class_count_ptr, classes_ptr);
    }

    jvmtiError GetClassSignature (jclass clazz,
        char **signature_ptr, char **generic_ptr)
    {
        return funcs->GetClassSignature (this, clazz, signature_ptr, generic_ptr);
    }

    jvmtiError GetClassStatus (jclass clazz, jint * status_ptr)
    {
        return funcs->GetClassStatus (this, clazz, status_ptr);
    }

    jvmtiError GetSourceFileName (jclass clazz, char **source_name_ptr)
    {
        return funcs->GetSourceFileName (this, clazz, source_name_ptr);
    }

    jvmtiError GetClassModifiers (jclass clazz, jint * modifiers_ptr)
    {
        return funcs->GetClassModifiers (this, clazz, modifiers_ptr);
    }

    jvmtiError GetClassMethods (jclass clazz,
        jint * method_count_ptr,
        jmethodID ** methods_ptr)
    {
        return funcs->GetClassMethods (this, clazz, method_count_ptr,
            methods_ptr);
    }

    jvmtiError GetClassFields (jclass clazz,
        jint * field_count_ptr, jfieldID ** fields_ptr)
    {
        return funcs->GetClassFields (this, clazz, field_count_ptr, fields_ptr);
    }

    jvmtiError GetImplementedInterfaces (jclass clazz,
        jint * interface_count_ptr,
        jclass ** interfaces_ptr)
    {
        return funcs->GetImplementedInterfaces (this, clazz,
            interface_count_ptr,
            interfaces_ptr);
    }

    jvmtiError IsInterface (jclass clazz, jboolean * is_interface_ptr)
    {
        return funcs->IsInterface (this, clazz, is_interface_ptr);
    }

    jvmtiError IsArrayClass (jclass clazz, jboolean * is_array_class_ptr)
    {
        return funcs->IsArrayClass (this, clazz, is_array_class_ptr);
    }

    jvmtiError GetClassLoader (jclass clazz, jobject * classloader_ptr)
    {
        return funcs->GetClassLoader (this, clazz, classloader_ptr);
    }

    jvmtiError GetSourceDebugExtension (jclass clazz,
        char **source_debug_extension_ptr)
    {
        return funcs->GetSourceDebugExtension (this, clazz,
            source_debug_extension_ptr);
    }

    jvmtiError RedefineClasses (jint class_count,
        const jvmtiClassDefinition * class_definitions)
    {
        return funcs->RedefineClasses (this, class_count, class_definitions);
    }

    jvmtiError GetObjectSize (jobject object, jlong * size_ptr)
    {
        return funcs->GetObjectSize (this, object, size_ptr);
    }

    jvmtiError GetObjectHashCode (jobject object, jint * hash_code_ptr)
    {
        return funcs->GetObjectHashCode (this, object, hash_code_ptr);
    }

    jvmtiError GetObjectMonitorUsage (jobject object,
        jvmtiMonitorUsage * info_ptr)
    {
        return funcs->GetObjectMonitorUsage (this, object, info_ptr);
    }

    jvmtiError GetFieldName (jclass clazz,
        jfieldID field,
        char **name_ptr,
        char **signature_ptr, char **generic_ptr)
    {
        return funcs->GetFieldName (this, clazz, field, name_ptr,
            signature_ptr, generic_ptr);
    }

    jvmtiError GetFieldDeclaringClass (jclass clazz,
        jfieldID field,
        jclass * declaring_class_ptr)
    {
        return funcs->GetFieldDeclaringClass (this, clazz, field,
            declaring_class_ptr);
    }

    jvmtiError GetFieldModifiers (jclass clazz,
        jfieldID field, jint * modifiers_ptr)
    {
        return funcs->GetFieldModifiers (this, clazz, field, modifiers_ptr);
    }

    jvmtiError IsFieldSynthetic (jclass clazz,
        jfieldID field, jboolean * is_synthetic_ptr)
    {
        return funcs->IsFieldSynthetic (this, clazz, field, is_synthetic_ptr);
    }

    jvmtiError GetMethodName (jmethodID method,
        char **name_ptr,
        char **signature_ptr, char **generic_ptr)
    {
        return funcs->GetMethodName (this, method, name_ptr, signature_ptr,
            generic_ptr);
    }

    jvmtiError GetMethodDeclaringClass (jmethodID method,
        jclass * declaring_class_ptr)
    {
        return funcs->GetMethodDeclaringClass (this, method, declaring_class_ptr);
    }

    jvmtiError GetMethodModifiers (jmethodID method, jint * modifiers_ptr)
    {
        return funcs->GetMethodModifiers (this, method, modifiers_ptr);
    }

    jvmtiError GetMaxLocals (jmethodID method, jint * max_ptr)
    {
        return funcs->GetMaxLocals (this, method, max_ptr);
    }

    jvmtiError GetArgumentsSize (jmethodID method, jint * size_ptr)
    {
        return funcs->GetArgumentsSize (this, method, size_ptr);
    }

    jvmtiError GetLineNumberTable (jmethodID method,
        jint * entry_count_ptr,
        jvmtiLineNumberEntry ** table_ptr)
    {
        return funcs->GetLineNumberTable (this, method, entry_count_ptr,
            table_ptr);
    }

    jvmtiError GetMethodLocation (jmethodID method,
        jlocation * start_location_ptr,
        jlocation * end_location_ptr)
    {
        return funcs->GetMethodLocation (this, method, start_location_ptr,
            end_location_ptr);
    }

    jvmtiError GetLocalVariableTable (jmethodID method,
        jint * entry_count_ptr,
        jvmtiLocalVariableEntry ** table_ptr)
    {
        return funcs->GetLocalVariableTable (this, method, entry_count_ptr,
            table_ptr);
    }

    jvmtiError GetBytecodes (jmethodID method,
        jint * bytecode_count_ptr,
        unsigned char **bytecodes_ptr)
    {
        return funcs->GetBytecodes (this, method, bytecode_count_ptr,
            bytecodes_ptr);
    }

    jvmtiError IsMethodNative (jmethodID method, jboolean * is_native_ptr)
    {
        return funcs->IsMethodNative (this, method, is_native_ptr);
    }

    jvmtiError IsMethodSynthetic (jmethodID method, jboolean * is_synthetic_ptr)
    {
        return funcs->IsMethodSynthetic (this, method, is_synthetic_ptr);
    }

    jvmtiError IsMethodObsolete (jmethodID method, jboolean * is_obsolete_ptr)
    {
        return funcs->IsMethodObsolete (this, method, is_obsolete_ptr);
    }

    jvmtiError CreateRawMonitor (const char *name, jrawMonitorID * monitor_ptr)
    {
        return funcs->CreateRawMonitor (this, name, monitor_ptr);
    }

    jvmtiError DestroyRawMonitor (jrawMonitorID monitor)
    {
        return funcs->DestroyRawMonitor (this, monitor);
    }

    jvmtiError RawMonitorEnter (jrawMonitorID monitor)
    {
        return funcs->RawMonitorEnter (this, monitor);
    }

    jvmtiError RawMonitorExit (jrawMonitorID monitor)
    {
        return funcs->RawMonitorExit (this, monitor);
    }

    jvmtiError RawMonitorWait (jrawMonitorID monitor, jlong millis)
    {
        return funcs->RawMonitorWait (this, monitor, millis);
    }

    jvmtiError RawMonitorNotify (jrawMonitorID monitor)
    {
        return funcs->RawMonitorNotify (this, monitor);
    }

    jvmtiError RawMonitorNotifyAll (jrawMonitorID monitor)
    {
        return funcs->RawMonitorNotifyAll (this, monitor);
    }

    jvmtiError SetJNIFunctionTable (const jniNativeInterface * function_table)
    {
        return funcs->SetJNIFunctionTable (this, function_table);
    }

    jvmtiError GetJNIFunctionTable (jniNativeInterface ** function_table)
    {
        return funcs->GetJNIFunctionTable (this, function_table);
    }

    jvmtiError SetEventCallbacks (const jvmtiEventCallbacks * callbacks,
        jint size_of_callbacks)
    {
        return funcs->SetEventCallbacks (this, callbacks, size_of_callbacks);
    }

    jvmtiError SetEventNotificationMode (jvmtiEventMode mode,
        jvmtiEvent event_type,
        jthread event_thread, ...)
    {
        return funcs->SetEventNotificationMode (this, mode, event_type,
            event_thread);
    }

    jvmtiError GenerateEvents (jvmtiEvent event_type)
    {
        return funcs->GenerateEvents (this, event_type);
    }

    jvmtiError GetExtensionFunctions (jint * extension_count_ptr,
        jvmtiExtensionFunctionInfo ** extensions)
    {
        return funcs->GetExtensionFunctions (this, extension_count_ptr,
            extensions);
    }

    jvmtiError GetExtensionEvents (jint * extension_count_ptr,
        jvmtiExtensionEventInfo ** extensions)
    {
        return funcs->GetExtensionEvents (this, extension_count_ptr, extensions);
    }

    jvmtiError SetExtensionEventCallback (jint extension_event_index,
        jvmtiExtensionEvent callback)
    {
        return funcs->SetExtensionEventCallback (this, extension_event_index,
            callback);
    }

    jvmtiError GetPotentialCapabilities (jvmtiCapabilities * capabilities_ptr)
    {
        return funcs->GetPotentialCapabilities (this, capabilities_ptr);
    }

    jvmtiError AddCapabilities (const jvmtiCapabilities * capabilities_ptr)
    {
        return funcs->AddCapabilities (this, capabilities_ptr);
    }

    jvmtiError RelinquishCapabilities (const jvmtiCapabilities *
        capabilities_ptr)
    {
        return funcs->RelinquishCapabilities (this, capabilities_ptr);
    }

    jvmtiError GetCapabilities (jvmtiCapabilities * capabilities_ptr)
    {
        return funcs->GetCapabilities (this, capabilities_ptr);
    }

    jvmtiError GetCurrentThreadCpuTimerInfo (jvmtiTimerInfo * info_ptr)
    {
        return funcs->GetCurrentThreadCpuTimerInfo (this, info_ptr);
    }

    jvmtiError GetCurrentThreadCpuTime (jlong * nanos_ptr)
    {
        return funcs->GetCurrentThreadCpuTime (this, nanos_ptr);
    }

    jvmtiError GetThreadCpuTimerInfo (jvmtiTimerInfo * info_ptr)
    {
        return funcs->GetThreadCpuTimerInfo (this, info_ptr);
    }

    jvmtiError GetThreadCpuTime (jthread thread, jlong * nanos_ptr)
    {
        return funcs->GetThreadCpuTime (this, thread, nanos_ptr);
    }

    jvmtiError GetTimerInfo (jvmtiTimerInfo * info_ptr)
    {
        return funcs->GetTimerInfo (this, info_ptr);
    }

    jvmtiError GetTime (jlong * nanos_ptr)
    {
        return funcs->GetTime (this, nanos_ptr);
    }

    jvmtiError GetAvailableProcessors (jint * processor_count_ptr)
    {
        return funcs->GetAvailableProcessors (this, processor_count_ptr);
    }

    jvmtiError AddToBootstrapClassLoaderSearch (const char *segment)
    {
        return funcs->AddToBootstrapClassLoaderSearch (this, segment);
    }

    jvmtiError GetSystemProperties (jint * count_ptr, char ***property_ptr)
    {
        return funcs->GetSystemProperties (this, count_ptr, property_ptr);
    }

    jvmtiError GetSystemProperty (const char *property, char **value_ptr)
    {
        return funcs->GetSystemProperty (this, property, value_ptr);
    }

    jvmtiError SetSystemProperty (const char *property, const char *value)
    {
        return funcs->SetSystemProperty (this, property, value);
    }

    jvmtiError GetPhase (jvmtiPhase * phase_ptr)
    {
        return funcs->GetPhase (this, phase_ptr);
    }

    jvmtiError DisposeEnvironment ()
    {
        return funcs->DisposeEnvironment (this);
    }

    jvmtiError SetEnvironmentLocalStorage (const void *data)
    {
        return funcs->SetEnvironmentLocalStorage (this, data);
    }

    jvmtiError GetEnvironmentLocalStorage (void **data_ptr)
    {
        return funcs->GetEnvironmentLocalStorage (this, data_ptr);
    }

    jvmtiError GetVersionNumber (jint * version_ptr)
    {
        return funcs->GetVersionNumber (this, version_ptr);
    }

    jvmtiError GetErrorName (jvmtiError error, char **name_ptr)
    {
        return funcs->GetErrorName (this, error, name_ptr);
    }

    jvmtiError SetVerboseFlag (jvmtiVerboseFlag flag, jboolean value)
    {
        return funcs->SetVerboseFlag (this, flag, value);
    }

    jvmtiError GetJLocationFormat (jvmtiJlocationFormat * format_ptr)
    {
        return funcs->GetJLocationFormat (this, format_ptr);
    }

#endif

};

#endif /* _JVMTI_H_ */
