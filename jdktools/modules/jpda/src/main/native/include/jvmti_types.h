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
#ifndef _JVMTI_TYPES_H_
#define _JVMTI_TYPES_H_

#include "jni_types.h"

#ifdef __cplusplus
extern "C"
{
#endif

    /**
     * Basic types
     */
    struct ti_interface;
    struct jvmtiEnv_struct;

#ifdef __cplusplus
    typedef jvmtiEnv_struct jvmtiEnv;
#else
    typedef const struct ti_interface *jvmtiEnv;
#endif

    typedef jobject jthread;
    typedef jlong jlocation;
    typedef jobject jthreadGroup;
    struct _jrawMonitorID;
    typedef struct _jrawMonitorID *jrawMonitorID;
    typedef struct JNINativeInterface_ jniNativeInterface;


    /**
     * Pointer to a function which could be launched
     * as a separate system thread.
     */
    typedef void (JNICALL * jvmtiStartFunction)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, void *arg);
        
    /**
     * Heap visit control flags
     */
    enum {
        JVMTI_VISIT_OBJECTS = 0x100, 
        JVMTI_VISIT_ABORT = 0x8000 
    };
     
    /**
     * Heap reference enumeration 
     */
    typedef enum {
        JVMTI_HEAP_REFERENCE_CLASS = 1,
        JVMTI_HEAP_REFERENCE_FIELD = 2, 
        JVMTI_HEAP_REFERENCE_ARRAY_ELEMENT = 3, 
        JVMTI_HEAP_REFERENCE_CLASS_LOADER = 4,  
        JVMTI_HEAP_REFERENCE_SIGNERS = 5,  
        JVMTI_HEAP_REFERENCE_PROTECTION_DOMAIN = 6,
        JVMTI_HEAP_REFERENCE_INTERFACE = 7, 
        JVMTI_HEAP_REFERENCE_STATIC_FIELD = 8, 
        JVMTI_HEAP_REFERENCE_CONSTANT_POOL = 9,
        JVMTI_HEAP_REFERENCE_SUPERCLASS = 10, 
        JVMTI_HEAP_REFERENCE_JNI_GLOBAL = 21, 
        JVMTI_HEAP_REFERENCE_SYSTEM_CLASS = 22,  
        JVMTI_HEAP_REFERENCE_MONITOR = 23,
        JVMTI_HEAP_REFERENCE_STACK_LOCAL = 24,  
        JVMTI_HEAP_REFERENCE_JNI_LOCAL = 25, 
        JVMTI_HEAP_REFERENCE_THREAD = 26,  
        JVMTI_HEAP_REFERENCE_OTHER = 27   
    } jvmtiHeapReferenceKind;
    
    /**
     * Single character type descriptors of primitive types. 
     */
    typedef enum {
        JVMTI_PRIMITIVE_TYPE_BOOLEAN = 90, 
        JVMTI_PRIMITIVE_TYPE_BYTE = 66, 
        JVMTI_PRIMITIVE_TYPE_CHAR = 67, 
        JVMTI_PRIMITIVE_TYPE_SHORT = 83,
        JVMTI_PRIMITIVE_TYPE_INT = 73, 
        JVMTI_PRIMITIVE_TYPE_LONG = 74, 
        JVMTI_PRIMITIVE_TYPE_FLOAT = 70, 
        JVMTI_PRIMITIVE_TYPE_DOUBLE = 68
    } jvmtiPrimitiveType;
    
    /**
     * Reference information structure for Field references
     */
    typedef struct {
        jint index;
    } jvmtiHeapReferenceInfoField;
		
    /**
     * Reference information structure for Array references
     */
    typedef struct {
        jint index;
    } jvmtiHeapReferenceInfoArray;
		
    /**
     * Reference information structure for Constant Pool references
     */
    typedef struct {
        jint index;
    } jvmtiHeapReferenceInfoConstantPool;
		
    /**
     * Reference information structure for Local Variable references
     */
    typedef struct {
        jlong thread_tag;
        jlong thread_id;
        jint depth;
        jmethodID method;
        jlocation location;
        jint slot;
    } jvmtiHeapReferenceInfoStackLocal;
    
    /**
     * Reference information structure for JNI local references
     */
    typedef struct {
        jlong thread_tag;
        jlong thread_id;
        jint depth;
        jmethodID method;
    } jvmtiHeapReferenceInfoJniLocal;
    
    /**
     * Reference information structure for Other references
     */
    typedef struct {
        jlong reserved1;
        jlong reserved2;
        jlong reserved3;
        jlong reserved4;
        jlong reserved5;
        jlong reserved6;
        jlong reserved7;
        jlong reserved8;
    } jvmtiHeapReferenceInfoReserved;
    
    /**
     * Represented as a union of the various kinds of reference information. 
     */
     typedef union {
        jvmtiHeapReferenceInfoField field;
        jvmtiHeapReferenceInfoArray array;
        jvmtiHeapReferenceInfoConstantPool constant_pool;
        jvmtiHeapReferenceInfoStackLocal stack_local;
        jvmtiHeapReferenceInfoJniLocal jni_local;
        jvmtiHeapReferenceInfoReserved other;
     } jvmtiHeapReferenceInfo;
     
    /**
     *  Describes (but does not pass in) an object in the heap. 
     */
     typedef jint (JNICALL *jvmtiHeapIterationCallback)
        (jlong class_tag, 
         jlong size, 
         jlong* tag_ptr, 
         jint length, 
         void* user_data);
         
    /**
     * Describes a reference from an object or the VM (the referrer) 
     * to another object (the referree) or a heap root to a referree. 
     */
     typedef jint (JNICALL *jvmtiHeapReferenceCallback)
        (jvmtiHeapReferenceKind reference_kind, 
         const jvmtiHeapReferenceInfo* reference_info, 
         jlong class_tag, 
         jlong referrer_class_tag, 
         jlong size, 
         jlong* tag_ptr, 
         jlong* referrer_tag_ptr, 
         jint length, 
         void* user_data);

    /**
     * This callback will describe a static field if the object is a class, 
     * and otherwise will describe an instance field. 
     */
     typedef jint (JNICALL *jvmtiPrimitiveFieldCallback)
        (jvmtiHeapReferenceKind kind, 
         const jvmtiHeapReferenceInfo* info, 
         jlong object_class_tag, 
         jlong* object_tag_ptr, 
         jvalue value, 
         jvmtiPrimitiveType value_type, 
         void* user_data);
     
    /**
     * Describes the values in an array of a primitive type.
     */
     typedef jint (JNICALL *jvmtiArrayPrimitiveValueCallback)
        (jlong class_tag, 
         jlong size, 
         jlong* tag_ptr, 
         jint element_count, 
         jvmtiPrimitiveType element_type, 
         const void* elements, 
         void* user_data);
     
    /**
     * Describes the value of a java.lang.String. 
     */ 
     typedef jint (JNICALL *jvmtiStringPrimitiveValueCallback)
        (jlong class_tag, 
         jlong size, 
         jlong* tag_ptr, 
         const jchar* value, 
         jint value_length, 
         void* user_data);
         
    /**
     * Reserved for future use. 
     */
     typedef jint (JNICALL *jvmtiReservedCallback)
        ();
     
    /**
     * Error codes.
     */
    typedef enum
    {
        JVMTI_ERROR_NONE = 0,
        JVMTI_ERROR_NULL_POINTER = 100,
        JVMTI_ERROR_OUT_OF_MEMORY = 110,
        JVMTI_ERROR_ACCESS_DENIED = 111,
        JVMTI_ERROR_UNATTACHED_THREAD = 115,
        JVMTI_ERROR_INVALID_ENVIRONMENT = 116,
        JVMTI_ERROR_WRONG_PHASE = 112,
        JVMTI_ERROR_INTERNAL = 113,
        JVMTI_ERROR_INVALID_PRIORITY = 12,
        JVMTI_ERROR_THREAD_NOT_SUSPENDED = 13,
        JVMTI_ERROR_THREAD_SUSPENDED = 14,
        JVMTI_ERROR_THREAD_NOT_ALIVE = 15,
        JVMTI_ERROR_CLASS_NOT_PREPARED = 22,
        JVMTI_ERROR_NO_MORE_FRAMES = 31,
        JVMTI_ERROR_OPAQUE_FRAME = 32,
        JVMTI_ERROR_DUPLICATE = 40,
        JVMTI_ERROR_NOT_FOUND = 41,
        JVMTI_ERROR_NOT_MONITOR_OWNER = 51,
        JVMTI_ERROR_INTERRUPT = 52,
        JVMTI_ERROR_UNMODIFIABLE_CLASS = 79,
        JVMTI_ERROR_NOT_AVAILABLE = 98,
        JVMTI_ERROR_ABSENT_INFORMATION = 101,
        JVMTI_ERROR_INVALID_EVENT_TYPE = 102,
        JVMTI_ERROR_NATIVE_METHOD = 104,
        JVMTI_ERROR_INVALID_THREAD = 10,
        JVMTI_ERROR_INVALID_FIELDID = 25,
        JVMTI_ERROR_INVALID_METHODID = 23,
        JVMTI_ERROR_INVALID_LOCATION = 24,
        JVMTI_ERROR_INVALID_OBJECT = 20,
        JVMTI_ERROR_INVALID_CLASS = 21,
        JVMTI_ERROR_TYPE_MISMATCH = 34,
        JVMTI_ERROR_INVALID_SLOT = 35,
        JVMTI_ERROR_MUST_POSSESS_CAPABILITY = 99,
        JVMTI_ERROR_INVALID_THREAD_GROUP = 11,
        JVMTI_ERROR_INVALID_MONITOR = 50,
        JVMTI_ERROR_ILLEGAL_ARGUMENT = 103,
        JVMTI_ERROR_INVALID_TYPESTATE = 65,
        JVMTI_ERROR_UNSUPPORTED_VERSION = 68,
        JVMTI_ERROR_INVALID_CLASS_FORMAT = 60,
        JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION = 61,
        JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED = 63,
        JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED = 64,
        JVMTI_ERROR_FAILS_VERIFICATION = 62,
        JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED = 66,
        JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED = 67,
        JVMTI_ERROR_NAMES_DONT_MATCH = 69,
        JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED = 70,
        JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED = 71,
        JVMTI_NYI = 666
    } jvmtiError;

    /**
     * Class status flags (from spec)
     */
    enum
    {
        /*
         * Class bytecodes have been verified
         */
        JVMTI_CLASS_STATUS_VERIFIED = 1,
        /*
         * Class preparation is complete
         */
        JVMTI_CLASS_STATUS_PREPARED = 2,
        /*
         * Class initialization is complete. Static initializer has been run.
         */
        JVMTI_CLASS_STATUS_INITIALIZED = 4,
        /*
         * Error during initialization makes class unusable
         */
        JVMTI_CLASS_STATUS_ERROR = 8,
        /*
         * Class is an array. If set, all other bits are zero.
         */
        JVMTI_CLASS_STATUS_ARRAY = 16,
        /*
         * Class is a primitive class (for example, java.lang.Integer.TYPE).
         * If set, all other bits are zero.
         */
        JVMTI_CLASS_STATUS_PRIMITIVE = 32
    };

    /**
     * Thread states (from spec).
     */
    enum
    {
        JVMTI_THREAD_STATE_ALIVE = 0x0001,
        JVMTI_THREAD_STATE_TERMINATED = 0x0002,
        JVMTI_THREAD_STATE_RUNNABLE = 0x0004,
        JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER = 0x0400,
        JVMTI_THREAD_STATE_WAITING = 0x0080,
        JVMTI_THREAD_STATE_WAITING_INDEFINITELY = 0x0010,
        JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT = 0x0020,
        JVMTI_THREAD_STATE_SLEEPING = 0x0040,
        JVMTI_THREAD_STATE_IN_OBJECT_WAIT = 0x0100,
        JVMTI_THREAD_STATE_PARKED = 0x0200,
        JVMTI_THREAD_STATE_SUSPENDED = 0x100000,
        JVMTI_THREAD_STATE_INTERRUPTED = 0x200000,
        JVMTI_THREAD_STATE_IN_NATIVE = 0x400000,
        JVMTI_THREAD_STATE_VENDOR_1 = 0x10000000,
        JVMTI_THREAD_STATE_VENDOR_2 = 0x20000000,
        JVMTI_THREAD_STATE_VENDOR_3 = 0x40000000
    };

    enum
    {
        JVMTI_JAVA_LANG_THREAD_STATE_MASK =
        JVMTI_THREAD_STATE_TERMINATED |
        JVMTI_THREAD_STATE_ALIVE |
        JVMTI_THREAD_STATE_RUNNABLE |
        JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER |
        JVMTI_THREAD_STATE_WAITING |
        JVMTI_THREAD_STATE_WAITING_INDEFINITELY |
        JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT,
        JVMTI_JAVA_LANG_THREAD_STATE_NEW = 0,
        JVMTI_JAVA_LANG_THREAD_STATE_TERMINATED = JVMTI_THREAD_STATE_TERMINATED,
        JVMTI_JAVA_LANG_THREAD_STATE_RUNNABLE =
        JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_RUNNABLE,
        JVMTI_JAVA_LANG_THREAD_STATE_BLOCKED =
        JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER,
        JVMTI_JAVA_LANG_THREAD_STATE_WAITING =
        JVMTI_THREAD_STATE_ALIVE |
        JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_WAITING_INDEFINITELY,
        JVMTI_JAVA_LANG_THREAD_STATE_TIMED_WAITING =
        JVMTI_THREAD_STATE_ALIVE |
        JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT
    };

    /**
     * Thread priorities (from spec).
     */
    enum
    {
        JVMTI_THREAD_MIN_PRIORITY = 1,
        JVMTI_THREAD_NORM_PRIORITY = 5,
        JVMTI_THREAD_MAX_PRIORITY = 10
    };

    /**
     * Thread data (from spec).
     */
    typedef struct
    {
        char *name;
        jint priority;
        jboolean is_daemon;
        jthreadGroup thread_group;
        jobject context_class_loader;
    } jvmtiThreadInfo;

    /**
     * Thread group data (from spec).
     */
    typedef struct
    {
        jthreadGroup parent;
        char *name;
        jint max_priority;
        jboolean is_daemon;
    } jvmtiThreadGroupInfo;

    /**
     * Stack frame data (from spec).
     */
    typedef struct
    {
        jmethodID method;
        jlocation location;
    } jvmtiFrameInfo;

    /**
     * Heap callback function structure
     */
    typedef struct {
        jvmtiHeapIterationCallback heap_iteration_callback;
        jvmtiHeapReferenceCallback heap_reference_callback;
        jvmtiPrimitiveFieldCallback primitive_field_callback;
        jvmtiArrayPrimitiveValueCallback array_primitive_value_callback;
        jvmtiStringPrimitiveValueCallback string_primitive_value_callback;
        jvmtiReservedCallback reserved5;
        jvmtiReservedCallback reserved6;
        jvmtiReservedCallback reserved7;
        jvmtiReservedCallback reserved8;
        jvmtiReservedCallback reserved9;
        jvmtiReservedCallback reserved10;
        jvmtiReservedCallback reserved11;
        jvmtiReservedCallback reserved12;
        jvmtiReservedCallback reserved13;
        jvmtiReservedCallback reserved14;
        jvmtiReservedCallback reserved15;
    } jvmtiHeapCallbacks;
    
    /**
     * Thread stack data (from spec).
     */
    typedef struct
    {
        jthread thread;
        jint state;
        jvmtiFrameInfo *frame_buffer;
        jint frame_count;
    } jvmtiStackInfo;

    /**
     * Event numbers (from spec). Custom events could be added
     * after JVMTI_MAX_EVENT_TYPE_VAL.
     */
    typedef enum
    {
        JVMTI_MIN_EVENT_TYPE_VAL = 50,
        JVMTI_EVENT_VM_INIT = 50,
        JVMTI_EVENT_VM_DEATH = 51,
        JVMTI_EVENT_THREAD_START = 52,
        JVMTI_EVENT_THREAD_END = 53,
        JVMTI_EVENT_CLASS_FILE_LOAD_HOOK = 54,
        JVMTI_EVENT_CLASS_LOAD = 55,
        JVMTI_EVENT_CLASS_PREPARE = 56,
        JVMTI_EVENT_VM_START = 57,
        JVMTI_EVENT_EXCEPTION = 58,
        JVMTI_EVENT_EXCEPTION_CATCH = 59,
        JVMTI_EVENT_SINGLE_STEP = 60,
        JVMTI_EVENT_FRAME_POP = 61,
        JVMTI_EVENT_BREAKPOINT = 62,
        JVMTI_EVENT_FIELD_ACCESS = 63,
        JVMTI_EVENT_FIELD_MODIFICATION = 64,
        JVMTI_EVENT_METHOD_ENTRY = 65,
        JVMTI_EVENT_METHOD_EXIT = 66,
        JVMTI_EVENT_NATIVE_METHOD_BIND = 67,
        JVMTI_EVENT_COMPILED_METHOD_LOAD = 68,
        JVMTI_EVENT_COMPILED_METHOD_UNLOAD = 69,
        JVMTI_EVENT_DYNAMIC_CODE_GENERATED = 70,
        JVMTI_EVENT_DATA_DUMP_REQUEST = 71,
        JVMTI_EVENT_DATA_RESET_REQUEST = 72,
        JVMTI_EVENT_MONITOR_WAIT = 73,
        JVMTI_EVENT_MONITOR_WAITED = 74,
        JVMTI_EVENT_MONITOR_CONTENDED_ENTER = 75,
        JVMTI_EVENT_MONITOR_CONTENDED_ENTERED = 76,
        JVMTI_EVENT_GARBAGE_COLLECTION_START = 81,
        JVMTI_EVENT_GARBAGE_COLLECTION_FINISH = 82,
        JVMTI_EVENT_OBJECT_FREE = 83,
        JVMTI_EVENT_VM_OBJECT_ALLOC = 84,
        JVMTI_MAX_EVENT_TYPE_VAL = 84
    } jvmtiEvent;

#define TOTAL_EVENT_TYPE_NUM (JVMTI_MAX_EVENT_TYPE_VAL - JVMTI_MIN_EVENT_TYPE_VAL + 1)

    /**
     * Root types.
     */
    typedef enum
    {
        JVMTI_HEAP_ROOT_JNI_GLOBAL = 1,
        JVMTI_HEAP_ROOT_SYSTEM_CLASS = 2,
        JVMTI_HEAP_ROOT_MONITOR = 3,
        JVMTI_HEAP_ROOT_STACK_LOCAL = 4,
        JVMTI_HEAP_ROOT_JNI_LOCAL = 5,
        JVMTI_HEAP_ROOT_THREAD = 6,
        JVMTI_HEAP_ROOT_OTHER = 7
    } jvmtiHeapRootKind;

    /**
     * Generic iteration control.
     */
    typedef enum
    {
        JVMTI_ITERATION_CONTINUE = 1,
        JVMTI_ITERATION_IGNORE = 2,
        JVMTI_ITERATION_ABORT = 0
    } jvmtiIterationControl;

    /**
     * Describes enumerated references.
     */
    typedef enum
    {
        JVMTI_REFERENCE_CLASS = 1,
        JVMTI_REFERENCE_FIELD = 2,
        JVMTI_REFERENCE_ARRAY_ELEMENT = 3,
        JVMTI_REFERENCE_CLASS_LOADER = 4,
        JVMTI_REFERENCE_SIGNERS = 5,
        JVMTI_REFERENCE_PROTECTION_DOMAIN = 6,
        JVMTI_REFERENCE_INTERFACE = 7,
        JVMTI_REFERENCE_STATIC_FIELD = 8,
        JVMTI_REFERENCE_CONSTANT_POOL = 9
    } jvmtiObjectReferenceKind;

    /**
     * Mostly tag support resides on VM side, but GC
     * should be aware of it.
     */
    typedef enum
    {
        JVMTI_HEAP_OBJECT_TAGGED = 1,
        JVMTI_HEAP_OBJECT_UNTAGGED = 2,
        JVMTI_HEAP_OBJECT_EITHER = 3
    } jvmtiHeapObjectFilter;

    typedef struct
    {
        jthread owner;
        jint entry_count;
        jint waiter_count;
        jthread *waiters;
        jint notify_waiter_count;
        jthread *notify_waiters;
    } jvmtiMonitorUsage;

    typedef struct
    {
        jlocation start_location;
        jint line_number;
    } jvmtiLineNumberEntry;

    typedef struct
    {
        jlocation start_location;
        jint length;
        char *name;
        char *signature;
        char *generic_signature;
        jint slot;
    } jvmtiLocalVariableEntry;

    /* ******************************************************
     * Event management is exposed to other OPEN components.
     */

    typedef void (JNICALL * jvmtiEventVMInit)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread);

    typedef void (JNICALL * jvmtiEventSingleStep)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread,
        jmethodID method, jlocation location);

    typedef void (JNICALL * jvmtiEventBreakpoint)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread,
        jmethodID method, jlocation location);

    typedef void (JNICALL * jvmtiEventFieldAccess)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread,
        jmethodID method, jlocation location, jclass field_clazz,
        jobject object, jfieldID field);

    typedef void (JNICALL * jvmtiEventFieldModification)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread,
        jmethodID method, jlocation location, jclass field_clazz,
        jobject object, jfieldID field, char signature_type, jvalue new_value);

    typedef void (JNICALL * jvmtiEventFramePop)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread,
        jmethodID method, jboolean was_popped_by_exception);

    typedef void (JNICALL * jvmtiEventMethodEntry)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread,
        jmethodID method);

    typedef void (JNICALL * jvmtiEventMethodExit)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread,
        jmethodID method, jboolean was_popped_by_exception, jvalue return_value);

    typedef void (JNICALL * jvmtiEventNativeMethodBind)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread,
        jmethodID method, void *address, void **new_address_ptr);

    typedef void (JNICALL * jvmtiEventException)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread,
        jmethodID method, jlocation location, jobject exception,
        jmethodID catch_method, jlocation catch_location);

    typedef void (JNICALL * jvmtiEventExceptionCatch)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env,
        jthread thread, jmethodID method, jlocation location, jobject exception);

    typedef void (JNICALL * jvmtiEventThreadStart)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread);

    typedef void (JNICALL * jvmtiEventThreadEnd)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread);

    typedef void (JNICALL * jvmtiEventClassLoad)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread, jclass clazz);

    typedef void (JNICALL * jvmtiEventClassPrepare)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread, jclass clazz);

    typedef void (JNICALL * jvmtiEventClassFileLoadHook)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env,
        jclass class_being_redefined, jobject loader,
        const char *name, jobject protection_domain,
        jint class_data_len, const unsigned char *class_data,
        jint * new_class_data_len, unsigned char **new_class_data);

    typedef void (JNICALL * jvmtiEventVMStart)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env);

    typedef void (JNICALL * jvmtiEventVMDeath)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env);

    typedef struct
    {
        const void *start_address;
        jlocation location;
    } jvmtiAddrLocationMap;

    typedef void (JNICALL * jvmtiEventCompiledMethodLoad)
        (jvmtiEnv * jvmti_env, jmethodID method, jint code_size,
        const void *code_addr, jint map_length,
        const jvmtiAddrLocationMap * almap, const void *compile_info);

    typedef void (JNICALL * jvmtiEventCompiledMethodUnload)
        (jvmtiEnv * jvmti_env, jmethodID method, const void *code_addr);

    typedef void (JNICALL * jvmtiEventDynamicCodeGenerated)
        (jvmtiEnv * jvmti_env,
        const char *name, const void *address, jint length);

    typedef void (JNICALL * jvmtiEventDataDumpRequest) (jvmtiEnv * jvmti_env);

    typedef void (JNICALL * jvmtiEventDataResetRequest) (jvmtiEnv * jvmti_env);

    typedef void (JNICALL * jvmtiEventMonitorContendedEnter)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread, jobject object);

    typedef void (JNICALL * jvmtiEventMonitorContendedEntered)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread, jobject object);

    typedef void (JNICALL * jvmtiEventMonitorWait)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread,
        jobject object, jlong timeout);

    typedef void (JNICALL * jvmtiEventMonitorWaited)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread,
        jobject object, jboolean timed_out);

    typedef void (JNICALL * jvmtiEventVMObjectAlloc)
        (jvmtiEnv * jvmti_env, JNIEnv * jni_env, jthread thread,
        jobject object, jclass object_clazz, jlong size);

    typedef void (JNICALL * jvmtiEventObjectFree)
        (jvmtiEnv * jvmti_env, jlong tag);

    typedef void (JNICALL * jvmtiEventGarbageCollectionStart)
        (jvmtiEnv * jvmti_env);

    typedef void (JNICALL * jvmtiEventGarbageCollectionFinish)
        (jvmtiEnv * jvmti_env);

    typedef void *jvmtiEventReserved;

    typedef struct
    {
        jvmtiEventVMInit VMInit;
        jvmtiEventVMDeath VMDeath;
        jvmtiEventThreadStart ThreadStart;
        jvmtiEventThreadEnd ThreadEnd;
        jvmtiEventClassFileLoadHook ClassFileLoadHook;
        jvmtiEventClassLoad ClassLoad;
        jvmtiEventClassPrepare ClassPrepare;
        jvmtiEventVMStart VMStart;
        jvmtiEventException Exception;
        jvmtiEventExceptionCatch ExceptionCatch;
        jvmtiEventSingleStep SingleStep;
        jvmtiEventFramePop FramePop;
        jvmtiEventBreakpoint Breakpoint;
        jvmtiEventFieldAccess FieldAccess;
        jvmtiEventFieldModification FieldModification;
        jvmtiEventMethodEntry MethodEntry;
        jvmtiEventMethodExit MethodExit;
        jvmtiEventNativeMethodBind NativeMethodBind;
        jvmtiEventCompiledMethodLoad CompiledMethodLoad;
        jvmtiEventCompiledMethodUnload CompiledMethodUnload;
        jvmtiEventDynamicCodeGenerated DynamicCodeGenerated;
        jvmtiEventDataDumpRequest DataDumpRequest;
        jvmtiEventDataResetRequest DataResetRequest;
        jvmtiEventMonitorWait MonitorWait;
        jvmtiEventMonitorWaited MonitorWaited;
        jvmtiEventMonitorContendedEnter MonitorContendedEnter;
        jvmtiEventMonitorContendedEntered MonitorContendedEntered;
        jvmtiEventReserved reserved77;
        jvmtiEventReserved reserved78;
        jvmtiEventReserved reserved79;
        jvmtiEventReserved reserved80;
        jvmtiEventGarbageCollectionStart GarbageCollectionStart;
        jvmtiEventGarbageCollectionFinish GarbageCollectionFinish;
        jvmtiEventObjectFree ObjectFree;
        jvmtiEventVMObjectAlloc VMObjectAlloc;
    } jvmtiEventCallbacks;


    typedef enum
    {
        JVMTI_KIND_IN = 91,
        JVMTI_KIND_IN_PTR = 92,
        JVMTI_KIND_IN_BUF = 93,
        JVMTI_KIND_ALLOC_BUF = 94,
        JVMTI_KIND_ALLOC_ALLOC_BUF = 95,
        JVMTI_KIND_OUT = 96,
        JVMTI_KIND_OUT_BUF = 97
    } jvmtiParamKind;

    typedef enum
    {
        JVMTI_TYPE_JBYTE = 101,
        JVMTI_TYPE_JCHAR = 102,
        JVMTI_TYPE_JSHORT = 103,
        JVMTI_TYPE_JINT = 104,
        JVMTI_TYPE_JLONG = 105,
        JVMTI_TYPE_JFLOAT = 106,
        JVMTI_TYPE_JDOUBLE = 107,
        JVMTI_TYPE_JBOOLEAN = 108,
        JVMTI_TYPE_JOBJECT = 109,
        JVMTI_TYPE_JTHREAD = 110,
        JVMTI_TYPE_JCLASS = 111,
        JVMTI_TYPE_JVALUE = 112,
        JVMTI_TYPE_JFIELDID = 113,
        JVMTI_TYPE_JMETHODID = 114,
        JVMTI_TYPE_CCHAR = 115,
        JVMTI_TYPE_CVOID = 116,
        JVMTI_TYPE_JNIENV = 117
    } jvmtiParamTypes;

    typedef struct
    {
        char *name;
        jvmtiParamKind kind;
        jvmtiParamTypes base_type;
        jboolean null_ok;
    } jvmtiParamInfo;

    typedef jvmtiError
        (JNICALL * jvmtiExtensionFunction) (jvmtiEnv * jvmti_env, ...);

    typedef enum
    {
        JVMTI_ENABLE = 1,
        JVMTI_DISABLE = 0
    } jvmtiEventMode;

    typedef struct
    {
        jvmtiExtensionFunction func;
        char *id;
        char *short_description;
        jint param_count;
        jvmtiParamInfo *params;
        jint error_count;
        jvmtiError *errors;
    } jvmtiExtensionFunctionInfo;

    typedef struct
    {
        jint extension_event_index;
        char *id;
        char *short_description;
        jint param_count;
        jvmtiParamInfo *params;
    } jvmtiExtensionEventInfo;

    /**
     * OPEN components should be aware of event management
     * capabilities.
     */
    typedef struct
    {
        unsigned int can_tag_objects:1;
        unsigned int can_generate_field_modification_events:1;
        unsigned int can_generate_field_access_events:1;
        unsigned int can_get_bytecodes:1;
        unsigned int can_get_synthetic_attribute:1;
        unsigned int can_get_owned_monitor_info:1;
        unsigned int can_get_current_contended_monitor:1;
        unsigned int can_get_monitor_info:1;
        unsigned int can_pop_frame:1;
        unsigned int can_redefine_classes:1;
        unsigned int can_signal_thread:1;
        unsigned int can_get_source_file_name:1;
        unsigned int can_get_line_numbers:1;
        unsigned int can_get_source_debug_extension:1;
        unsigned int can_access_local_variables:1;
        unsigned int can_maintain_original_method_order:1;
        unsigned int can_generate_single_step_events:1;
        unsigned int can_generate_exception_events:1;
        unsigned int can_generate_frame_pop_events:1;
        unsigned int can_generate_breakpoint_events:1;
        unsigned int can_suspend:1;
        unsigned int can_redefine_any_class:1;
        unsigned int can_get_current_thread_cpu_time:1;
        unsigned int can_get_thread_cpu_time:1;
        unsigned int can_generate_method_entry_events:1;
        unsigned int can_generate_method_exit_events:1;
        unsigned int can_generate_all_class_hook_events:1;
        unsigned int can_generate_compiled_method_load_events:1;
        unsigned int can_generate_monitor_events:1;
        unsigned int can_generate_vm_object_alloc_events:1;
        unsigned int can_generate_native_method_bind_events:1;
        unsigned int can_generate_garbage_collection_events:1;
        unsigned int can_generate_object_free_events:1;
        unsigned int can_force_early_return : 1;
        unsigned int can_get_owned_monitor_stack_depth_info : 1;
        unsigned int can_get_constant_pool : 1;
        unsigned int can_set_native_method_prefix : 1;
        unsigned int can_retransform_classes : 1;
        unsigned int can_retransform_any_class : 1;
        unsigned int can_generate_resource_exhaustion_heap_events : 1;
        unsigned int can_generate_resource_exhaustion_threads_events : 1;

        unsigned int:7;
        unsigned int:16;
        unsigned int:16;
        unsigned int:16;
        unsigned int:16;
        unsigned int:16;
    } jvmtiCapabilities;

    typedef enum
    {
        JVMTI_TIMER_USER_CPU = 30,
        JVMTI_TIMER_TOTAL_CPU = 31,
        JVMTI_TIMER_ELAPSED = 32
    } jvmtiTimerKind;

    typedef struct
    {
        jlong max_value;
        jboolean may_skip_forward;
        jboolean may_skip_backward;
        jvmtiTimerKind kind;
        jlong reserved1;
        jlong reserved2;
    } jvmtiTimerInfo;

    typedef struct
    {
        jobject monitor;
        jint stack_depth;
    } jvmtiMonitorStackDepthInfo;


    typedef enum
    {
        JVMTI_PHASE_ONLOAD = 1,
        JVMTI_PHASE_PRIMORDIAL = 2,
        JVMTI_PHASE_START = 6,
        JVMTI_PHASE_LIVE = 4,
        JVMTI_PHASE_DEAD = 8
    } jvmtiPhase;

    typedef enum
    {
        JVMTI_VERBOSE_OTHER = 0,
        JVMTI_VERBOSE_GC = 1,
        JVMTI_VERBOSE_CLASS = 2,
        JVMTI_VERBOSE_JNI = 4
    } jvmtiVerboseFlag;

    typedef enum
    {
        JVMTI_JLOCATION_JVMBCI = 1,
        JVMTI_JLOCATION_MACHINEPC = 2,
        JVMTI_JLOCATION_OTHER = 0
    } jvmtiJlocationFormat;

    typedef jvmtiIterationControl
        (JNICALL * jvmtiHeapObjectCallback)
        (jlong class_tag, jlong size, jlong * tag_ptr, void *user_data);

    typedef jvmtiIterationControl
        (JNICALL * jvmtiHeapRootCallback)
        (jvmtiHeapRootKind root_kind, jlong class_tag, jlong size,
        jlong * tag_ptr, void *user_data);

    typedef jvmtiIterationControl
        (JNICALL * jvmtiStackReferenceCallback)
        (jvmtiHeapRootKind root_kind, jlong class_tag, jlong size,
        jlong * tag_ptr, jlong thread_tag, jint depth, jmethodID method,
        jint slot, void *user_data);

    typedef jvmtiIterationControl
        (JNICALL * jvmtiObjectReferenceCallback)
        (jvmtiObjectReferenceKind reference_kind, jlong class_tag, jlong size,
        jlong * tag_ptr, jlong referrer_tag, jint referrer_index,
        void *user_data);

    typedef struct
    {
        jclass klass;
        jint class_byte_count;
        const unsigned char *class_bytes;
    } jvmtiClassDefinition;

    typedef void (JNICALL * jvmtiExtensionEvent) (jvmtiEnv * jvmti_env, ...);

#ifdef __cplusplus
}               /* extern "C" { */
#endif


#endif /* _JVMTI_TYPES_H_ */
