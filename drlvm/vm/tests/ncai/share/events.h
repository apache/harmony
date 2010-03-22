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
 * @author Valentin Al. Sitnick, Petr Ivanov
 *
 */


#ifndef _EVENTS_H_
#define _EVENTS_H_

#ifndef LINUX
#include <windows.h>
#endif

#include "jvmti.h"
#include <string.h>

/* *********************************************************************** */
/*
 * Macros with parameters for events callbacks
 */

#define   prms_AGENT_ONLOAD     JavaVM *vm, char *options, void *reserved

#define   prms_SINGLE_STEP      jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jmethodID method,          \
                                jlocation location

#define   prms_BRKPOINT         jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jmethodID method,          \
                                jlocation location

#define   prms_FLD_ACCESS       jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jmethodID method,          \
                                jlocation location, jclass field_klass,    \
                                jobject object, jfieldID field

#define   prms_FLD_MODIF        jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jmethodID method,          \
                                jlocation location, jclass field_klass,    \
                                jobject object, jfieldID field,            \
                                char, jvalue

#define   prms_FRM_POP          jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jmethodID method,          \
                                jboolean was_popped_by_exception

#define   prms_METHOD_ENTRY     jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jmethodID method

#define   prms_METHOD_EXIT      jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jmethodID method,          \
                                jboolean was_popped_by_exception,          \
                                jvalue

#define   prms_METHOD_BIND      jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jmethodID method,          \
                                void* address, void** new_address_ptr

#define   prms_EXCPT            jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jmethodID method,          \
                                jlocation location, jobject exception,     \
                                jmethodID catch_method,                    \
                                jlocation catch_location

#define   prms_EXCPT_CATCH      jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jmethodID method,          \
                                jlocation location, jobject exception

#define   prms_THRD_START       jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread

#define   prms_THRD_END         jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread

#define   prms_CLS_LD           jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jclass klass

#define   prms_CLS_PRPR         jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jclass klass

#define   prms_CLS_FL_LD_HOOK   jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jclass class_being_redefined,              \
                                jobject loader, const char* name,          \
                                jobject protection_domain,                 \
                                jint class_data_len,                       \
                                const unsigned char* class_data,           \
                                jint* new_class_data_len,                  \
                                unsigned char** new_class_data

#define   prms_VMSTART          jvmtiEnv *jvmti_env, JNIEnv* jni_env

#define   prms_VMINIT           jvmtiEnv* jvmti_env, JNIEnv* jni_env,      \
                                jthread thread

#define   prms_VMDEATH          jvmtiEnv *jvmti_env, JNIEnv* jni_env

#define   prms_COMPL_MET_LD     jvmtiEnv *jvmti_env, jmethodID method,     \
                                jint code_size, const void* code_addr,     \
                                jint map_length,                           \
                                const jvmtiAddrLocationMap* map,           \
                                const void* compile_info

#define   prms_COMPL_MET_ULD    jvmtiEnv *jvmti_env, jmethodID method,     \
                                const void* code_addr

#define   prms_DYN_CODE_GEN     jvmtiEnv *jvmti_env, const char* name,     \
                                const void* address, jint length

#define   prms_DATA_DUMP        jvmtiEnv *jvmti_env

#define   prms_MON_ENTER        jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jobject object

#define   prms_MON_ENTERED      jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jobject object

#define   prms_MON_WAIT         jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jobject object,            \
                                jlong timeout

#define   prms_MON_WAITED       jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jobject object,            \
                                jboolean timed_out

#define   prms_VMOBJ_ALLOC      jvmtiEnv *jvmti_env, JNIEnv* jni_env,      \
                                jthread thread, jobject object,            \
                                jclass object_klass, jlong size

#define   prms_OBJ_FREE         jvmtiEnv *jvmti_env, jlong tag

#define   prms_GC_START         jvmtiEnv *jvmti_env

#define   prms_GC_FIN           jvmtiEnv *jvmti_env

#define   prms_GC           jvmtiEnv *jvmti_env


/* *********************************************************************** */
/*
 * Macros with parameters for events callbacks
 */

#define   check_AGENT_ONLOAD                                               \
if (vm == NULL)                                                            \
    fprintf(stderr,                                                        \
            "\tcheck: Agent_OnLoad was called with jni_env = NULL\n");     \
if (options == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: Agent_OnLoad was called with options = NULL\n");     \
if (reserved == NULL)                                                      \
    fprintf(stderr,                                                        \
            "\tcheck: Agent_OnLoad was called with reserved = NULL\n");    \

#define   check_SINGLE_STEP                                                \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: SingleStep was called with jvmti_env = NULL\n");     \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: SingleStep was called with jni_env = NULL\n");       \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: SingleStep was called with thread = NULL\n");        \
if (method == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: SingleStep was called with method = NULL\n");        \
if (location == 0)                                                         \
    fprintf(stderr,                                                        \
            "\tcheck: SingleStep was called with location = NULL\n");      \

#define   check_BRKPOINT                                                   \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: Breakpoint was called with jvmti_env = NULL\n");     \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: Breakpoint was called with jni_env = NULL\n");       \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: Breakpoint was called with thread = NULL\n");        \
if (method == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: Breakpoint was called with method = NULL\n");        \
if (location == 0)                                                         \
    fprintf(stderr,                                                        \
            "\tcheck: Breakpoint was called with location = NULL\n");      \

#define   check_FLD_ACCESS                                                 \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: FieldAccess was called with jvmti_env = NULL\n");    \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: FieldAccess was called with jni_env = NULL\n");      \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: FieldAccess was called with thread = NULL\n");       \
if (method == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: FieldAccess was called with method = NULL\n");       \
if (location == 0)                                                         \
    fprintf(stderr,                                                        \
            "\tcheck: FieldAccess was called with location = NULL\n");     \
if (field_klass == NULL)                                                   \
    fprintf(stderr,                                                        \
            "\tcheck: FieldAccess was called with thread = NULL\n");       \
if (object == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: FieldAccess was called with object = NULL\n");       \
if (field == NULL)                                                         \
    fprintf(stderr,                                                        \
            "\tcheck: FieldAccess was called with field = NULL\n");        \

#define   check_FLD_MODIF                                                  \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: FieldModific was called with jvmti_env = NULL\n");   \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: FieldModific was called with jni_env = NULL\n");     \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: FieldModific was called with thread = NULL\n");      \
if (method == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: FieldModific was called with method = NULL\n");      \
if (location == 0)                                                         \
    fprintf(stderr,                                                        \
            "\tcheck: FieldModific was called with location = NULL\n");    \
if (field_klass == NULL)                                                   \
    fprintf(stderr,                                                        \
            "\tcheck: FieldModific was called with field_klass = NULL\n"); \
if (object == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: FieldModific was called with object = NULL\n");      \
if (field == NULL)                                                         \
    fprintf(stderr,                                                        \
            "\tcheck: FieldModific was called with field = NULL\n");

#define   check_FRM_POP                                                    \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: FramePop was called with jvmti_env = NULL\n");       \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: FramePop was called with jni_env = NULL\n");         \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: FramePop was called with thread = NULL\n");          \
if (method == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: FramePop was called with method = NULL\n");          \
if (was_popped_by_exception == 0)                                          \
    fprintf(stderr,                                                        \
            "\tcheck: FramePop was called with popped_by_exc = NULL\n");   \

#define   check_METHOD_ENTRY                                               \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: MethodEntry was called with jvmti_env = NULL\n");    \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: MethodEntry was called with jni_env = NULL\n");      \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MethodEntry was called with thread = NULL\n");       \
if (method == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MethodEntry was called with method = NULL\n");       \

#define   check_METHOD_EXIT                                                \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: MethodExit was called with jvmti_env = NULL\n");     \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: MethodExit was called with jni_env = NULL\n");       \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MethodExit was called with thread = NULL\n");        \
if (method == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MethodExit was called with method = NULL\n");        \
if (was_popped_by_exception == 0)                                          \
    fprintf(stderr,                                                        \
            "\tcheck: MethodExit was called with popped_by_exc = 0\n");

#define   check_METHOD_BIND                                                \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: MethodBind was called with jvmti_env = NULL\n");     \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: MethodBind was called with jni_env = NULL\n");       \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MethodBind was called with thread = NULL\n");        \
if (method == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MethodBind was called with method = NULL\n");        \
if (address == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: MethodBind was called with address = NULL\n");       \
if (new_address_ptr == NULL)                                               \
    fprintf(stderr,                                                        \
            "\tcheck: MethodBind was called with new_addr_ptr = NULL\n");  \

#define   check_EXCPT                                                      \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: Exception was called with jvmti_env = NULL\n");      \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: Exception was called with jni_env = NULL\n");        \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: Exception was called with thread = NULL\n");         \
if (method == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: Exception was called with method = NULL\n");         \
if (location == 0)                                                         \
    fprintf(stderr,                                                        \
            "\tcheck: Exception was called with location = NULL\n");       \
if (exception == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: Exception was called with exception = NULL\n");      \
if (catch_method == NULL)                                                  \
    fprintf(stderr,                                                        \
            "\tcheck: Exception was called with catch_method = NULL\n");   \
if (catch_location == 0)                                                   \
    fprintf(stderr,                                                        \
            "\tcheck: Exception was called with catch_location = NULL\n"); \

#define   check_EXCPT_CATCH                                                \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: ExceptionCatch was called with jvmti_env = NULL\n"); \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: ExceptionCatch was called with jni_env = NULL\n");   \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: ExceptionCatch was called with thread = NULL\n");    \
if (method == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: ExceptionCatch was called with method = NULL\n");    \
if (location == 0)                                                         \
    fprintf(stderr,                                                        \
            "\tcheck: ExceptionCatch was called with location = NULL\n");  \
if (exception == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: ExceptionCatch was called with exception = NULL\n"); \

#define   check_THRD_START                                                 \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: ThreadStart was called with jvmti_env = NULL\n");    \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: ThreadStart was called with jni_env = NULL\n");      \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: ThreadStart was called with thread = NULL\n");       \

#define   check_THRD_END                                                   \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: ThreadEnd was called with jvmti_env = NULL\n");      \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: ThreadEnd was called with jni_env = NULL\n");        \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: ThreadEnd was called with thread = NULL\n");         \

#define   check_CLS_LD                                                     \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: ClassLoad was called with jvmti_env = NULL\n");      \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: ClassLoad was called with jni_env = NULL\n");        \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: ClassLoad was called with thread = NULL\n");         \
if (klass == NULL)                                                         \
    fprintf(stderr,                                                        \
            "\tcheck: ClassLoad was called with klass = NULL\n");          \

#define   check_CLS_PRPR                                                   \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: ClassPrepare was called with jvmti_env = NULL\n");   \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: ClassPrepare was called with jni_env = NULL\n");     \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: ClassPrepare was called with thread = NULL\n");      \
if (klass == NULL)                                                         \
    fprintf(stderr,                                                        \
            "\tcheck: ClassPrepare was called with klass = NULL\n");       \

#define   check_CLS_FL_LD_HOOK                                             \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: LoadHook was called with jvmti_env = NULL\n");       \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: LoadHook was called with jni_env = NULL\n");         \
if (class_being_redefined == NULL)                                         \
    fprintf(stderr,                                                        \
            "\tcheck: LoadHook was called with class_redef = NULL\n");     \
if (loader == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: LoadHook was called with loader = NULL\n");          \
if (name == NULL)                                                          \
    fprintf(stderr,                                                        \
            "\tcheck: LoadHook was called with name = NULL\n");            \
if (protection_domain == NULL)                                             \
    fprintf(stderr,                                                        \
            "\tcheck: LoadHook was called with protect_domain = NULL\n");  \
if (class_data_len == 0)                                                   \
    fprintf(stderr,                                                        \
            "\tcheck: LoadHook was called with class_data_len = NULL\n");  \
if (class_data == NULL)                                                    \
    fprintf(stderr,                                                        \
            "\tcheck: LoadHook was called with class_data = NULL\n");      \
if (new_class_data_len == 0)                                               \
    fprintf(stderr,                                                        \
            "\tcheck: LoadHook was called with new_data_len = NULL\n");    \
if (new_class_data == NULL)                                                \
    fprintf(stderr,                                                        \
            "\tcheck: LoadHook was called with new_class_data = NULL\n");  \

#define   check_VMSTART                                                    \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: VMStart was called with jvmti_env = NULL\n");        \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: VMStart was called with jni_env = NULL\n");          \

#define   check_VMINIT                                                     \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: VMInit was called with jvmti_env = NULL\n");         \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: VMInit was called with jni_env = NULL\n");           \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: VMInit was called with thread = NULL\n");            \

#define   check_VMDEATH                                                    \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: VMDeath was called with jvmti_env = NULL\n");        \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: VMDeath was called with jni_env = NULL\n");          \

#define   check_COMPL_MET_LD                                               \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: MethodLoad was called with jvmti_env = NULL\n");     \
if (method == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MethodLoad was called with method = NULL\n");        \
if (code_size == 0)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MethodLoad was called with code_size = NULL\n");     \
if (code_addr == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: MethodLoad was called with code_addr = NULL\n");     \
if (map_length == 0)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: MethodLoad was called with map_length = NULL\n");    \
if (map == NULL)                                                           \
    fprintf(stderr,                                                        \
            "\tcheck: MethodLoad was called with map = NULL\n");           \
if (compile_info == NULL)                                                  \
    fprintf(stderr,                                                        \
            "\tcheck: MethodLoad was called with compile_info = NULL\n");  \

#define   check_COMPL_MET_ULD                                              \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: MethodUnLoad was called with jvmti_env = NULL\n");   \
if (method == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MethodUnLoad was called with method = NULL\n");      \
if (code_addr == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: MethodUnLoad was called with code_addr = NULL\n");   \

#define   check_DYN_CODE_GEN                                               \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: DynamicCode was called with jvmti_env = NULL\n");    \
if (name == NULL)                                                          \
    fprintf(stderr,                                                        \
            "\tcheck: DynamicCode was called with name = NULL\n");         \
if (address == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: DynamicCode was called with address = NULL\n");      \
if (length == 0)                                                           \
    fprintf(stderr,                                                        \
            "\tcheck: DynamicCode was called with length = NULL\n");       \

#define   check_DATA_DUMP                                                  \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: DataDump was called with jvmti_env = NULL\n");       \

#define   check_MON_ENTER                                                  \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorEnter was called with jvmti_env = NULL\n");   \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorEnter was called with jni_env = NULL\n");     \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorEnter was called with thread = NULL\n");      \
if (object == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorEnter was called with object = NULL\n");      \

#define   check_MON_ENTERED                                                \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorEntered was called with jvmti_env = NULL\n"); \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorEntered was called with jni_env = NULL\n");   \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorEntered was called with thread = NULL\n");    \
if (object == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorEntered was called with object = NULL\n");    \

#define   check_MON_WAIT                                                   \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorWait was called with jvmti_env = NULL\n");    \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorWait was called with jni_env = NULL\n");      \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorWait was called with thread = NULL\n");       \
if (object == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorWait was called with object = NULL\n");       \
if (timeout == 0)                                                          \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorWait was called with timeout = NULL\n");      \

#define   check_MON_WAITED                                                 \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorWaited was called with jvmti_env = NULL\n");  \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorWaited was called with jni_env = NULL\n");    \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorWaited was called with thread = NULL\n");     \
if (object == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorWaited was called with object = NULL\n");     \
if (timed_out == 0)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: MonitorWaited was called with timed_out = NULL\n");  \

#define   check_VMOBJ_ALLOC                                                \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: VMObjAlloc was called with jvmti_env = NULL\n");     \
if (jni_env == NULL)                                                       \
    fprintf(stderr,                                                        \
            "\tcheck: VMObjAlloc was called with tag = NULL\n");           \
if (thread == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: VMObjAlloc was called with thread = NULL\n");        \
if (object == NULL)                                                        \
    fprintf(stderr,                                                        \
            "\tcheck: VMObjAlloc was called with object = NULL\n");        \
if (object_klass == NULL)                                                  \
    fprintf(stderr,                                                        \
            "\tcheck: VMObjAlloc was called with object_klass = NULL\n");  \
if (size == NULL)                                                          \
    fprintf(stderr,                                                        \
            "\tcheck: VMObjAlloc was called with size = NULL\n");          \

#define   check_OBJ_FREE                                                   \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: ObjectFree was called with jvmti_env = NULL\n");     \
if (tag == 0)                                                              \
    fprintf(stderr,                                                        \
            "\tcheck: ObjectFree was called with tag = NULL\n");           \

#define   check_GC_START                                                   \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: GCStart was called with jvmti_env = NULL\n");        \

#define   check_GC_FIN                                                     \
if (jvmti_env == NULL)                                                     \
    fprintf(stderr,                                                        \
            "\tcheck: GCFinish was called with jvmti_env = NULL\n");       \

/***************************************************************************/

void JNICALL callbackSingleStep(prms_SINGLE_STEP);

void JNICALL callbackBreakpoint(prms_BRKPOINT);

void JNICALL callbackFieldAccess(prms_FLD_ACCESS);

void JNICALL callbackFieldModification(prms_FLD_MODIF);

void JNICALL callbackFramePop(prms_FRM_POP);

void JNICALL callbackMethodEntry(prms_METHOD_ENTRY);

void JNICALL callbackMethodExit(prms_METHOD_EXIT);

void JNICALL callbackNativeMethodBind(prms_METHOD_BIND);

void JNICALL callbackException(prms_EXCPT);

void JNICALL callbackExceptionCatch(prms_EXCPT_CATCH);

void JNICALL callbackThreadStart(prms_THRD_START);

void JNICALL callbackThreadEnd(prms_THRD_END);

void JNICALL callbackClassLoad(prms_CLS_LD);

void JNICALL callbackClassPrepare(prms_CLS_PRPR);

void JNICALL callbackClassFileLoadHook(prms_CLS_FL_LD_HOOK);

void JNICALL callbackVMStart(prms_VMSTART);

void JNICALL callbackVMInit(prms_VMINIT);

void JNICALL callbackVMDeath(prms_VMDEATH);

void JNICALL callbackCompiledMethodLoad(prms_COMPL_MET_LD);

void JNICALL callbackCompiledMethodUnload(prms_COMPL_MET_ULD);

void JNICALL callbackDynamicCodeGenerated(prms_DYN_CODE_GEN);

void JNICALL callbackDataDumpRequest(prms_DATA_DUMP);

void JNICALL callbackMonitorContendedEnter(prms_MON_ENTER);

void JNICALL callbackMonitorContendedEntered(prms_MON_ENTERED);

void JNICALL callbackMonitorWait(prms_MON_WAIT);

void JNICALL callbackMonitorWaited(prms_MON_WAITED);

void JNICALL callbackVMObjectAlloc(prms_VMOBJ_ALLOC);

void JNICALL callbackObjectFree(prms_OBJ_FREE);

void JNICALL callbackGarbageCollectionStart(prms_GC_START);

void JNICALL callbackGarbageCollectionFinish(prms_GC_FIN);

/* *********************************************************************** */

#define AGENT_FOR_EVENTS_TESTS_PART_I                                      \
                                                                           \
    jvmtiEnv* jvmti;                                                       \
    jvmtiError results[70];                                                \
    jvmtiCapabilities pc;                                                  \
    jvmtiEventCallbacks cb = {NULL};                                       \
                                                                           \
    if (options == NULL)                                                   \
    {                                                                      \
        /* do nothing */                                                   \
    }                                                                      \
                                                                           \
    if (reserved == NULL)                                                  \
    {                                                                      \
        /* do nothing */                                                   \
    }                                                                      \
                                                                           \
    {                                                                      \
        jint res = JNI_OK;                                                 \
        res = vm->GetEnv((void**)&jvmti, JVMTI_VERSION_1_0);               \
                                                                           \
        if (res != JNI_OK) {                                               \
           printf ("Failure of initialization of JVMTI environment: %d\n", \
                   (int)res);                                              \
           return res;                                                     \
        }                                                                  \
    }                                                                      \
                                                                           \
    results[1] = jvmti->GetPotentialCapabilities(&pc);                     \
    results[2] = jvmti->AddCapabilities(&pc);                              \
                                                                           \
                                                                           \
    if  ((results[1] != JVMTI_ERROR_NONE) ||                               \
         (results[2] != JVMTI_ERROR_NONE))                                 \
    {                                                                      \
         printf("Failure of adding capabilities : pot=%d add=%d \n",       \
                    results[1], results[2]);                               \
         return JNI_ERR;                                                   \
    }                                                                      \
                                                                           \
    memset(&cb, 0, sizeof(cb));                                            \
                                                                           \
    cb.SingleStep              = CB.cbSingleStep;                          \
    cb.Breakpoint              = CB.cbBreakpoint;                          \
    cb.FieldAccess             = CB.cbFieldAccess;                         \
    cb.FieldModification       = CB.cbFieldModification;                   \
    cb.FramePop                = CB.cbFramePop;                            \
    cb.MethodEntry             = CB.cbMethodEntry;                         \
    cb.MethodExit              = CB.cbMethodExit;                          \
    cb.NativeMethodBind        = CB.cbNativeMethodBind;                    \
    cb.Exception               = CB.cbException;                           \
    cb.ExceptionCatch          = CB.cbExceptionCatch;                      \
    cb.ThreadStart             = CB.cbThreadStart;                         \
    cb.ThreadEnd               = CB.cbThreadEnd;                           \
    cb.ClassLoad               = CB.cbClassLoad;                           \
    cb.ClassPrepare            = CB.cbClassPrepare;                        \
    cb.ClassFileLoadHook       = CB.cbClassFileLoadHook;                   \
    cb.VMStart                 = CB.cbVMStart;                             \
    cb.VMInit                  = CB.cbVMInit;                              \
    cb.VMDeath                 = CB.cbVMDeath;                             \
    cb.CompiledMethodLoad      = CB.cbCompiledMethodLoad;                  \
    cb.CompiledMethodUnload    = CB.cbCompiledMethodUnload;                \
    cb.DynamicCodeGenerated    = CB.cbDynamicCodeGenerated;                \
    cb.DataDumpRequest         = CB.cbDataDumpRequest;                     \
    cb.MonitorContendedEnter   = CB.cbMonitorContendedEnter;               \
    cb.MonitorContendedEntered = CB.cbMonitorContendedEntered;             \
    cb.MonitorWait             = CB.cbMonitorWait;                         \
    cb.MonitorWaited           = CB.cbMonitorWaited;                       \
    cb.VMObjectAlloc           = CB.cbVMObjectAlloc;                       \
    cb.ObjectFree              = CB.cbObjectFree;                          \
    cb.GarbageCollectionStart  = CB.cbGarbageCollectionStart;              \
    cb.GarbageCollectionFinish = CB.cbGarbageCollectionFinish;             \


#define AGENT_FOR_EVENTS_TESTS_PART_II                                     \
                                                                           \
    int size  = (sizeof(events) / 4);                                      \
                                                                           \
    for ( int i = 0 ; i < size; i++ )                                      \
    {                                                                      \
        results[3 + i] =                                                   \
            jvmti->SetEventNotificationMode(JVMTI_ENABLE,                  \
                    events[ i ], NULL);                                    \
                                                                           \
        if (results[3 + i] != JVMTI_ERROR_NONE)                            \
        {                                                                  \
            printf("Failure of enabling event: %d\n", (int)results[3 + i]);\
                                                                           \
            return JNI_ERR;                                                \
        }                                                                  \
    }                                                                      \
                                                                           \
    /* set event callbacks */                                              \
    {                                                                      \
        results[64] = jvmti->SetEventCallbacks( &cb, sizeof(cb));          \
                                                                           \
        if (results[64] != JVMTI_ERROR_NONE)                               \
        {                                                                  \
            printf("Failure of setting event callbacks: %d\n",             \
                    (int)results[64]);                                     \
                                                                           \
            return JNI_ERR;                                                \
        }                                                                  \
    }

/* *********************************************************************** */

#define cb_exc        CB.cbException = (jvmtiEventException)callbackException;
#define cb_exccatch   CB.cbExceptionCatch = (jvmtiEventExceptionCatch)callbackExceptionCatch;
#define cb_obj        CB.cbVMObjectAlloc = (jvmtiEventVMObjectAlloc)callbackVMObjectAlloc;
#define cb_death      CB.cbVMDeath = (jvmtiEventVMDeath)callbackVMDeath;
#define cb_start      CB.cbVMStart = (jvmtiEventVMStart)callbackVMStart;
#define cb_clprep     CB.cbClassPrepare = (jvmtiEventClassPrepare)callbackClassPrepare;
#define cb_clld       CB.cbClassLoad = (jvmtiEventClassLoad)callbackClassLoad;
#define cb_tstart     CB.cbThreadStart = (jvmtiEventThreadStart)callbackThreadStart;
#define cb_tend       CB.cbThreadEnd = (jvmtiEventThreadEnd)callbackThreadEnd;
#define cb_init       CB.cbVMInit = (jvmtiEventVMInit)callbackVMInit;
#define cb_frpop      CB.cbFramePop = (jvmtiEventFramePop)callbackFramePop;
#define cb_brk        CB.cbBreakpoint = (jvmtiEventBreakpoint)callbackBreakpoint;
#define cb_acc        CB.cbFieldAccess = (jvmtiEventFieldAccess)callbackFieldAccess;
#define cb_mod        CB.cbFieldModification = (jvmtiEventFieldModification)callbackFieldModification;
#define cb_gcstart    CB.cbGarbageCollectionStart = (jvmtiEventGarbageCollectionFinish)callbackGarbageCollectionStart;
#define cb_gcfin      CB.cbGarbageCollectionFinish = (jvmtiEventGarbageCollectionFinish)callbackGarbageCollectionFinish;
#define cb_step       CB.cbSingleStep = (jvmtiEventSingleStep)callbackSingleStep;
#define cb_objfree    CB.cbObjectFree = (jvmtiEventObjectFree)callbackObjectFree;
#define cb_mentry     CB.cbMethodEntry = (jvmtiEventMethodEntry)callbackMethodEntry;
#define cb_mexit      CB.cbMethodExit = (jvmtiEventMethodExit)callbackMethodExit;
#define cb_faccess    CB.cbFieldAccess = (jvmtiEventFieldAccess)callbackFieldAccess;
#define cb_fmodif     CB.cbFieldModification = (jvmtiEventFieldModification)callbackFieldModification;
#define cb_loadhook   CB.cbClassFileLoadHook = (jvmtiEventClassFileLoadHook)callbackClassFileLoadHook;
#define cb_cmload     CB.cbCompiledMethodLoad = (jvmtiEventCompiledMethodLoad)callbackCompiledMethodLoad;
#define cb_cmunload   CB.cbCompiledMethodUnload = (jvmtiEventCompiledMethodUnload)callbackCompiledMethodUnload;
#define cb_dump       CB.cbDataDumpRequest = (jvmtiEventDataDumpRequest)callbackDataDumpRequest;
#define cb_codegen    CB.cbDynamicCodeGenerated = (jvmtiEventDynamicCodeGenerated)callbackDynamicCodeGenerated;
#define cb_mcenter    CB.cbMonitorContendedEnter = (jvmtiEventMonitorContendedEnter)callbackMonitorContendedEnter;
#define cb_mcentered  CB.cbMonitorContendedEntered = (jvmtiEventMonitorContendedEntered)callbackMonitorContendedEntered;
#define cb_mwait      CB.cbMonitorWait = (jvmtiEventMonitorWait)callbackMonitorWait;
#define cb_mwaited    CB.cbMonitorWaited = (jvmtiEventMonitorWaited)callbackMonitorWaited;
#define cb_native     CB.cbNativeMethodBind = (jvmtiEventNativeMethodBind)callbackNativeMethodBind;

/* *********************************************************************** */

class Callbacks {
public:
    Callbacks();

    jvmtiEventSingleStep cbSingleStep;
    jvmtiEventBreakpoint cbBreakpoint;
    jvmtiEventFieldAccess cbFieldAccess;
    jvmtiEventFieldModification cbFieldModification;
    jvmtiEventFramePop cbFramePop;
    jvmtiEventMethodEntry cbMethodEntry;
    jvmtiEventMethodExit cbMethodExit;
    jvmtiEventNativeMethodBind cbNativeMethodBind;
    jvmtiEventException cbException;
    jvmtiEventExceptionCatch cbExceptionCatch;
    jvmtiEventThreadStart cbThreadStart;
    jvmtiEventThreadEnd cbThreadEnd;
    jvmtiEventClassLoad cbClassLoad;
    jvmtiEventClassPrepare cbClassPrepare;
    jvmtiEventClassFileLoadHook cbClassFileLoadHook;
    jvmtiEventVMStart cbVMStart;
    jvmtiEventVMInit cbVMInit;
    jvmtiEventVMDeath cbVMDeath;
    jvmtiEventCompiledMethodLoad cbCompiledMethodLoad;
    jvmtiEventCompiledMethodUnload cbCompiledMethodUnload;
    jvmtiEventDynamicCodeGenerated cbDynamicCodeGenerated;
    jvmtiEventDataDumpRequest cbDataDumpRequest;
    jvmtiEventMonitorContendedEnter cbMonitorContendedEnter;
    jvmtiEventMonitorContendedEntered cbMonitorContendedEntered;
    jvmtiEventMonitorWait cbMonitorWait;
    jvmtiEventMonitorWaited cbMonitorWaited;
    jvmtiEventVMObjectAlloc cbVMObjectAlloc;
    jvmtiEventObjectFree cbObjectFree;
    jvmtiEventGarbageCollectionFinish cbGarbageCollectionStart;
    jvmtiEventGarbageCollectionFinish cbGarbageCollectionFinish;

private:

    int i;
};


    void SingleStep
    (jvmtiEnv*, JNIEnv*, jthread, jmethodID, jlocation);

    void Breakpoint
    (jvmtiEnv*, JNIEnv*, jthread, jmethodID, jlocation);

    void FieldAccess
    (jvmtiEnv*, JNIEnv*, jthread, jmethodID, jlocation, jclass, jobject, jfieldID);

    void FieldModification
    (jvmtiEnv*, JNIEnv*, jthread, jmethodID, jlocation, jclass, jobject, jfieldID, char, jvalue);

    void FramePop
    (jvmtiEnv*, JNIEnv*, jthread, jmethodID, jboolean);

    void MethodEntry
    (jvmtiEnv*, JNIEnv*, jthread, jmethodID);

    void MethodExit
    (jvmtiEnv*, JNIEnv*, jthread, jmethodID, jboolean, jvalue);

    void NativeMethodBind
    (jvmtiEnv*, JNIEnv*, jthread, jmethodID, void*, void**);

    void Exception
    (jvmtiEnv*, JNIEnv*, jthread, jmethodID, jlocation, jobject, jmethodID, jlocation);

    void ExceptionCatch
    (jvmtiEnv*, JNIEnv*, jthread, jmethodID, jlocation, jobject);

    void JNICALL ThreadStart
    (jvmtiEnv*, JNIEnv*, jthread);

    void ThreadEnd
    (jvmtiEnv*, JNIEnv*, jthread);

    void ClassLoad
    (jvmtiEnv*, JNIEnv*, jthread, jclass);

    void ClassPrepare
    (jvmtiEnv*, JNIEnv*, jthread, jclass);

    void ClassFileLoadHook
    (jvmtiEnv*, JNIEnv*, jclass, jobject, const char*, jobject, jint,
     const unsigned char*, jint*, unsigned char**);

    void VMStart
    (jvmtiEnv*, JNIEnv*);

    void VMInit
    (jvmtiEnv*, JNIEnv*, jthread);

    void VMDeath
    (jvmtiEnv*, JNIEnv*);

    void CompiledMethodLoad
    (jvmtiEnv*, jmethodID, jint, const void*, jint,
     const jvmtiAddrLocationMap*, const void*);

    void CompiledMethodUnload
    (jvmtiEnv *, jmethodID, const void*);

    void DynamicCodeGenerated
    (jvmtiEnv*, const char*, const void*, jint);

    void DataDumpRequest
    (jvmtiEnv*);

    void MonitorContendedEnter
    (jvmtiEnv*, JNIEnv*, jthread, jobject);

    void MonitorContendedEntered
    (jvmtiEnv*, JNIEnv*, jthread, jobject);

    void MonitorWait
    (jvmtiEnv*, JNIEnv*, jthread, jobject, jlong);

    void MonitorWaited
    (jvmtiEnv*, JNIEnv*, jthread, jobject, jboolean);

    void VMObjectAlloc
    (jvmtiEnv*, JNIEnv*, jthread, jobject, jclass, jlong);

    void ObjectFree
    (jvmtiEnv*, jlong);

    void GarbageCollectionStart
    (jvmtiEnv*);

    void GarbageCollectionFinish
    (jvmtiEnv*);

/* *********************************************************************** */

#endif /* _EVENTS_H_ */


