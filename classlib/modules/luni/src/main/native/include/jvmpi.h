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

#if !defined(jvmpi_h)
#define jvmpi_h

#include "jni.h"
/* JVMPI Constants */
#define JVMPI_VERSION_1 ((jint)0x10000001)
#define JVMPI_VERSION_1_1 ((jint)0x10000002)
#define JVMPI_VERSION_1_2 ((jint)0x10000003)
#define JVMPI_VERSION_HOTSPOT ((jint)0x10000002)
/* for compatability with older specs */
#define JVMPI_EVENT_LOAD_COMPILED_METHOD JVMPI_EVENT_COMPILED_METHOD_LOAD
#define JVMPI_EVENT_UNLOAD_COMPILED_METHOD JVMPI_EVENT_COMPILED_METHOD_UNLOAD
#define JVMPI_EVENT_METHOD_ENTRY    ((jint)1)
#define JVMPI_EVENT_METHOD_ENTRY2   ((jint)2)
#define JVMPI_EVENT_METHOD_EXIT   ((jint)3)
#define JVMPI_EVENT_OBJECT_ALLOC    ((jint)4)
#define JVMPI_EVENT_OBJECT_FREE   ((jint)5)
#define JVMPI_EVENT_OBJECT_MOVE   ((jint)6)
#define JVMPI_EVENT_COMPILED_METHOD_LOAD    ((jint)7)
#define JVMPI_EVENT_COMPILED_METHOD_UNLOAD    ((jint)8)
#define JVMPI_EVENT_INSTRUCTION_START   ((jint)9)       /* added in JVMPI_VERSION_1_2 */
#define JVMPI_EVENT_UNUSED_10   ((jint)10)
#define JVMPI_EVENT_UNUSED_11   ((jint)11)
#define JVMPI_EVENT_UNUSED_12   ((jint)12)
#define JVMPI_EVENT_UNUSED_13   ((jint)13)
#define JVMPI_EVENT_UNUSED_14   ((jint)14)
#define JVMPI_EVENT_UNUSED_15   ((jint)15)
#define JVMPI_EVENT_UNUSED_16   ((jint)16)
#define JVMPI_EVENT_UNUSED_17   ((jint)17)
#define JVMPI_EVENT_UNUSED_18   ((jint)18)
#define JVMPI_EVENT_UNUSED_19   ((jint)19)
#define JVMPI_EVENT_UNUSED_20   ((jint)20)
#define JVMPI_EVENT_UNUSED_21   ((jint)21)
#define JVMPI_EVENT_UNUSED_22   ((jint)22)
#define JVMPI_EVENT_UNUSED_23   ((jint)23)
#define JVMPI_EVENT_UNUSED_24   ((jint)24)
#define JVMPI_EVENT_UNUSED_25   ((jint)25)
#define JVMPI_EVENT_UNUSED_26   ((jint)26)
#define JVMPI_EVENT_UNUSED_27   ((jint)27)
#define JVMPI_EVENT_UNUSED_28   ((jint)28)
#define JVMPI_EVENT_UNUSED_29   ((jint)29)
#define JVMPI_EVENT_UNUSED_30   ((jint)30)
#define JVMPI_EVENT_UNUSED_31   ((jint)31)
#define JVMPI_EVENT_UNUSED_32   ((jint)32)
#define JVMPI_EVENT_THREAD_START    ((jint)33)
#define JVMPI_EVENT_THREAD_END    ((jint)34)
#define JVMPI_EVENT_CLASS_LOAD_HOOK   ((jint)35)
#define JVMPI_EVENT_UNUSED_36   ((jint)36)
#define JVMPI_EVENT_HEAP_DUMP   ((jint)37)
#define JVMPI_EVENT_JNI_GLOBALREF_ALLOC   ((jint)38)
#define JVMPI_EVENT_JNI_GLOBALREF_FREE    ((jint)39)
#define JVMPI_EVENT_JNI_WEAK_GLOBALREF_ALLOC    ((jint)40)
#define JVMPI_EVENT_JNI_WEAK_GLOBALREF_FREE   ((jint)41)
#define JVMPI_EVENT_CLASS_LOAD    ((jint)42)
#define JVMPI_EVENT_CLASS_UNLOAD    ((jint)43)
#define JVMPI_EVENT_DATA_DUMP_REQUEST   ((jint)44)
#define JVMPI_EVENT_DATA_RESET_REQUEST    ((jint)45)
#define JVMPI_EVENT_JVM_INIT_DONE   ((jint)46)
#define JVMPI_EVENT_JVM_SHUT_DOWN   ((jint)47)
#define JVMPI_EVENT_ARENA_NEW   ((jint)48)
#define JVMPI_EVENT_ARENA_DELETE    ((jint)49)
#define JVMPI_EVENT_OBJECT_DUMP   ((jint)50)
#define JVMPI_EVENT_RAW_MONITOR_CONTENDED_ENTER   ((jint)51)
#define JVMPI_EVENT_RAW_MONITOR_CONTENDED_ENTERED   ((jint)52)
#define JVMPI_EVENT_RAW_MONITOR_CONTENDED_EXIT    ((jint)53)
#define JVMPI_EVENT_MONITOR_CONTENDED_ENTER   ((jint)54)
#define JVMPI_EVENT_MONITOR_CONTENDED_ENTERED   ((jint)55)
#define JVMPI_EVENT_MONITOR_CONTENDED_EXIT    ((jint)56)
#define JVMPI_EVENT_MONITOR_WAIT    ((jint)57)
#define JVMPI_EVENT_MONITOR_WAITED    ((jint)58)
#define JVMPI_EVENT_MONITOR_DUMP    ((jint)59)
#define JVMPI_EVENT_GC_START    ((jint)60)
#define JVMPI_EVENT_GC_FINISH   ((jint)61)
#define JVMPI_MAX_EVENT_TYPE_VAL    ((jint)61)
/* IBM-Specific Profiling Events */
#define JVMPI_EVENT_DISABLE_COMPATIBILITY         ((jint )2000)
#define JVMPI_EVENT_GEN_COMPILED_METHOD           ((jint )2001)
#define JVMPI_EVENT_GEN_INLINE_METHOD             ((jint )2002)
#define JVMPI_EVENT_GEN_BUILTIN_METHOD            ((jint )2003)
#define JVMPI_EVENT_COMPILED_METHOD_ENTRY         ((jint )2004)
#define JVMPI_EVENT_COMPILED_METHOD_ENTRY2        ((jint )2005)
#define JVMPI_EVENT_COMPILED_METHOD_EXIT          ((jint )2006)
#define JVMPI_EVENT_INLINE_METHOD_ENTRY           ((jint )2007)
#define JVMPI_EVENT_INLINE_METHOD_ENTRY2          ((jint )2008)
#define JVMPI_EVENT_BUILTIN_METHOD_ENTRY          ((jint )2009)
#define JVMPI_EVENT_BUILTIN_METHOD_ENTRY2         ((jint )2010)
#define JVMPI_EVENT_NATIVE_METHOD_ENTRY           ((jint )2011)
#define JVMPI_EVENT_NATIVE_METHOD_ENTRY2          ((jint )2012)
#define JVMPI_EVENT_COMPILING_START               ((jint )2013)
#define JVMPI_EVENT_COMPILING_END                 ((jint )2014)
#define JVMPI_EVENT_COMPILER_GC_START             ((jint )2015)
#define JVMPI_EVENT_COMPILER_GC_END               ((jint )2016)
#define JVMPI_EVENT_OBJ_ALLOC_FAILURE             ((jint )2017)
#define JVMPI_EVENT_COMPILED_METHOD_LOAD2         ((jint )2018)
#define JVMPI_EVENT_JLM                           ((jint )2019)
#define JVMPI_EVENT_JLMTS                         ((jint )2020)
#define JVMPI_EVENT_MONITOR_JLM_DUMP              ((jint )2021)
#define JVMPI_EVENT_TRANSFER                      ((jint )2022)
#define JVMPI_EVENT_SEGMENT                       ((jint )2023)
#define JVMPI_MIN_IBM_INTERNAL_OPTION_VAL ((jint) 2000)
#define JVMPI_MAX_IBM_INTERNAL_OPTION_VAL ((jint) 2023)
#define JVMPI_REQUESTED_EVENT   ((jint)0x10000000)
#define JVMPI_SUCCESS   ((jint)0)
#define JVMPI_NOT_AVAILABLE   ((jint)1)
#define JVMPI_FAIL    ((jint)-1)

enum
{
  JVMPI_THREAD_RUNNABLE = 1,
  JVMPI_THREAD_MONITOR_WAIT,
  JVMPI_THREAD_CONDVAR_WAIT
};

#define JVMPI_THREAD_SUSPENDED    0x8000
#define JVMPI_THREAD_INTERRUPTED    0x4000
#define JVMPI_MINIMUM_PRIORITY    1
#define JVMPI_MAXIMUM_PRIORITY    10
#define JVMPI_NORMAL_PRIORITY   5
#define JVMPI_NORMAL_OBJECT   ((jint)0)
#define JVMPI_CLASS           ((jint)2)
#define JVMPI_BOOLEAN         ((jint)4)
#define JVMPI_CHAR            ((jint)5)
#define JVMPI_FLOAT           ((jint)6)
#define JVMPI_DOUBLE          ((jint)7)
#define JVMPI_BYTE            ((jint)8)
#define JVMPI_SHORT           ((jint)9)
#define JVMPI_INT             ((jint)10)
#define JVMPI_LONG            ((jint)11)
#define JVMPI_MONITOR_JAVA    0x01
#define JVMPI_MONITOR_RAW   0x02
#define JVMPI_GC_ROOT_UNKNOWN   0xff
#define JVMPI_GC_ROOT_JNI_GLOBAL    0x01
#define JVMPI_GC_ROOT_JNI_LOCAL   0x02
#define JVMPI_GC_ROOT_JAVA_FRAME    0x03
#define JVMPI_GC_ROOT_NATIVE_STACK    0x04
#define JVMPI_GC_ROOT_STICKY_CLASS    0x05
#define JVMPI_GC_ROOT_THREAD_BLOCK    0x06
#define JVMPI_GC_ROOT_MONITOR_USED    0x07
#define JVMPI_GC_ROOT_THREAD_OBJ    0x08
#define JVMPI_GC_CLASS_DUMP   0x20
#define JVMPI_GC_INSTANCE_DUMP    0x21
#define JVMPI_GC_OBJ_ARRAY_DUMP   0x22
#define JVMPI_GC_PRIM_ARRAY_DUMP    0x23
#define JVMPI_DUMP_LEVEL_0    ((jint)0)
#define JVMPI_DUMP_LEVEL_1    ((jint)1)
#define JVMPI_DUMP_LEVEL_2    ((jint)2)
/* JLM monitor dump */
#define JVMPI_DUMP_LEVEL_3    ((jint)3)
/* generic_event.flags */
#define JVMPI_GENERIC_FLAG_DISABLE_GC ((jint)1)
#define JVMPI_GENERIC_FLAG_CHECK_EVENT  ((jint)2)
/* generic_compiled_method_load.ld_ind */
#define JVMPI_LOAD      ((jint)1)
#define JVMPI_UNLOAD      ((jint)2)
/* generic_compiled_method_load.edesc */
#define JVMPI_NAME_FORMAT   ((jint)1)
/* generic_transfer_event.transfer_type */
#define JVMPI_TRANSFER_ITOJ   ((jint)1)
/* generic_transfer_event.transfer_status */
#define JVMPI_TRANSFER_OK   ((jint)1)
#define JVMPI_TRANSFER_FAIL   ((jint)2)
/* generic_segment_event.seg_type */
#define JVMPI_JITTED_SEGMENT    ((jint)1)
#define JVMPI_MMI_SEGMENT   ((jint)2)
/* generic_segment_event.alloc_ind */
#define JVMPI_SEGMENT_ALLOCATE    ((jint)1)
#define JVMPI_SEGMENT_FREE    ((jint)2)
/* jobjectID */
struct _jobjectID;
typedef struct _jobjectID *jobjectID;
/* JVMPI_RawMonitor */
struct _JVMPI_RawMonitor;
typedef struct _JVMPI_RawMonitor *JVMPI_RawMonitor;

/* JVMPI_CallFrame */
typedef struct
{
  jint lineno;
  jmethodID method_id;
} JVMPI_CallFrame;

/* JVMPI_CallTrace */
typedef struct
{
  JNIEnv *env_id;
  jint num_frames;
  JVMPI_CallFrame *frames;
} JVMPI_CallTrace;

/* JVMPI_Field */
typedef struct
{
  char *field_name;
  char *field_signature;
} JVMPI_Field;

/* JVMPI_HeapDumpArg */
typedef struct
{
  jint heap_dump_level;
} JVMPI_HeapDumpArg;

/* JVMPI_Lineno */
typedef struct
{
  jint offset;
  jint lineno;
} JVMPI_Lineno;

/* JVMPI_Method */
typedef struct
{
  char *method_name;
  char *method_signature;
  jint start_lineno;
  jint end_lineno;
  jmethodID method_id;
} JVMPI_Method;

/* JVMPI Event */
typedef struct
{
  jint event_type;
  JNIEnv *env_id;
  union
  {
    struct
    {
      jint arena_id;
    } delete_arena;
    struct
    {
      jint arena_id;
      char *arena_name;
    } new_arena;

    struct
    {
      char *class_name;
      char *source_name;
      jint num_interfaces;
      jint num_methods;
      JVMPI_Method *methods;
      jint num_static_fields;
      JVMPI_Field *statics;
      jint num_instance_fields;
      JVMPI_Field *instances;
      jobjectID class_id;
    } class_load;
    struct
    {
      unsigned char *class_data;
      jint class_data_len;
      unsigned char *new_class_data;
      jint new_class_data_len;
      void *(*malloc_f) (unsigned int);
    } class_load_hook;
    struct
    {
      jobjectID class_id;
    } class_unload;
    struct
    {
      jmethodID method_id;
      void *code_addr;
      jint code_size;
      jint lineno_table_size;
      JVMPI_Lineno *lineno_table;
    } compiled_method_load;
    struct
    {
      jmethodID method_id;
    } compiled_method_unload;
    struct
    {
      jlong used_objects;
      jlong used_object_space;
      jlong total_object_space;
    } gc_info;
    struct
    {
      int dump_level;
      char *begin;
      char *end;
      jint num_traces;
      JVMPI_CallTrace *traces;
    } heap_dump;
    struct
    {
      jobjectID obj_id;
      jobject ref_id;
    } jni_globalref_alloc;
    struct
    {
      jobject ref_id;
    } jni_globalref_free;
    struct
    {
      jmethodID method_id;
    } method;
    struct
    {
      jmethodID method_id;
      jobjectID obj_id;
    } method_entry2;
    struct
    {
      jobjectID object;
    } monitor;
    struct
    {
      char *begin;
      char *end;
      jint num_traces;
      JVMPI_CallTrace *traces;
      jint *threads_status;
    } monitor_dump;
    struct
    {
      jobjectID object;
      jlong timeout;
    } monitor_wait;
    struct
    {
      jint arena_id;
      jobjectID class_id;
      jint is_array;
      jint size;
      jobjectID obj_id;
    } obj_alloc;
    struct
    {
      jint data_len;
      char *data;
    } object_dump;
    struct
    {
      jobjectID obj_id;
    } obj_free;
    struct
    {
      jint arena_id;
      jobjectID obj_id;
      jint new_arena_id;
      jobjectID new_obj_id;
    } obj_move;
    struct
    {
      char *name;
      JVMPI_RawMonitor id;
    } raw_monitor;
    struct
    {
      char *thread_name;
      char *group_name;
      char *parent_name;
      jobjectID thread_id;
      JNIEnv *thread_env_id;
    } thread_start;
    struct
    {
      jmethodID method_id;
      jobjectID obj_id;
      jsize flags;
      jint reserv1;
      jint reserv2;
      jint reserv3;
      jint reserv4;
      jint reserv5;
      jint reserv6;
      jint reserv7;
      jint reserv8;
    } generic_event;
    struct
    {
      jmethodID method_id;
      jobjectID obj_id;
      jsize flags;
      char *code_name;
      jint edesc;
      jint ld_ind;
      void *code_addr;
      jint code_size;
      jint lineno_table_size;
      JVMPI_Lineno *lineno_table;
    } generic_compiled_method_load;
    struct
    {
      jmethodID method_id;
      jobjectID obj_id;
      jsize flags;
      jint transfer_type;
      jint transfer_status;
      jint reserv3;
      jint reserv4;
      jint reserv5;
      jint reserv6;
      jint reserv7;
      jint reserv8;
    } generic_transfer_event;
    struct
    {
      jmethodID method_id;
      jobjectID obj_id;
      jsize flags;
      char *seg_name;
      void *seg_addr;
      jsize seg_size;
      jint seg_type;
      jint alloc_ind;
      void *old_seg_addr;
      jint reserv7;
      jint reserv8;
    } generic_segment_event;
  } u;
} JVMPI_Event;

typedef struct
{
  jint version;
  void (*NotifyEvent) (JVMPI_Event * event);
  jint (*EnableEvent) (jint event_type, void *arg);
  jint (*DisableEvent) (jint event_type, void *arg);
  jint (*RequestEvent) (jint event_type, void *arg);
  void (*GetCallTrace) (JVMPI_CallTrace * trace, jint depth);
  void (*ProfilerExit) (jint);
  JVMPI_RawMonitor (*RawMonitorCreate) (char *lock_name);
  void (*RawMonitorEnter) (JVMPI_RawMonitor lock_id);
  void (*RawMonitorExit) (JVMPI_RawMonitor lock_id);
  void (*RawMonitorWait) (JVMPI_RawMonitor lock_id, jlong ms);
  void (*RawMonitorNotifyAll) (JVMPI_RawMonitor lock_id);
  void (*RawMonitorDestroy) (JVMPI_RawMonitor lock_id);
  jlong (*GetCurrentThreadCpuTime) (void);
  void (*SuspendThread) (JNIEnv * env);
  void (*ResumeThread) (JNIEnv * env);
  jint (*GetThreadStatus) (JNIEnv * env);
  jboolean (*ThreadHasRun) (JNIEnv * env);
  jint (*CreateSystemThread) (char *name, jint priority,
                              void (*f) (void *));
  void (*SetThreadLocalStorage) (JNIEnv * env_id, void *ptr);
  void *(*GetThreadLocalStorage) (JNIEnv * env_id);
  void (*DisableGC) (void);
  void (*EnableGC) (void);
  void (*RunGC) (void);
  jobjectID (*GetThreadObject) (JNIEnv * env);
  jobjectID (*GetMethodClass) (jmethodID mid);
  /* JVMPI_VERSION_1_1 additions */
  jobject (*jobjectID2jobject) (jobjectID jid);
  jobjectID (*jobject2jobjectID) (jobject j);
  /* JVMPI_VERSION_1_2 additions */
  void (*SuspendThreadList) (jint reqCount, JNIEnv ** reqList,
                            jint * results);
  void (*ResumeThreadList) (jint reqCount, JNIEnv ** reqList, jint * results);
} JVMPI_Interface;

#endif /* jvmpi_h */
