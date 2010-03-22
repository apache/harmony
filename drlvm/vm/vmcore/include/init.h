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

#ifndef _INIT_H
#define _INIT_H

#include "environment.h"

jint vm_attach_internal(JNIEnv ** p_jni_env, jthread * java_thread,
                        JavaVM * java_vm, jobject group,
                        const char * name, jboolean daemon);
jint vm_init1(JavaVM_Internal * java_vm, JavaVMInitArgs * vm_arguments);
jint vm_init2(JNIEnv * jni_env);
/**
 * The method is called from both paths of VM shutdown, namely during <code>DestroyJavaVM</code> and
 * <code>System.exit</code> right after <code>java.lang.System.execShutdownSequence</code> completion.
 * @see java.lang.System.execShutdownSequence
 */
void exec_native_shutdown_sequence();
jint vm_destroy(JavaVM_Internal * java_vm, jthread java_thread);
void vm_interrupt_handler();
void vm_dump_handler();

void initialize_vm_cmd_state(Global_Env *p_env, JavaVMInitArgs* arguments);
void set_log_levels_from_cmd(JavaVMInitArgs* vm_arguments);
/**
 * Parses string pool size required for environment initialization.
 */
void parse_vm_arguments1(JavaVMInitArgs *vm_args, size_t *p_string_pool_size,
                         jboolean *p_is_class_data_shared, apr_pool_t* pool);
/**
 * Collects all arguments in VM properties.
 */
void parse_vm_arguments2(Global_Env *p_env);
void* get_portlib_for_logger(Global_Env *p_env);
void parse_jit_arguments(JavaVMInitArgs* vm_arguments);
void print_generic_help();
jint initialize_properties(Global_Env *p_env);
jint helper_magic_init(Global_Env * vm_env);

#endif //_INIT_H
