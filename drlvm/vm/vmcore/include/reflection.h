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
 * @author Alexey V. Varlamov
 */  

// This interface provides a set of common reflection mechanisms for Java class libraries.

#ifndef _REFLECTION_H_
#define _REFLECTION_H_

#include "jni_types.h"
#include "vm_core_types.h"

jobjectArray reflection_get_class_interfaces(JNIEnv*, jclass);
jobjectArray reflection_get_class_fields(JNIEnv*, jclass);
jobjectArray reflection_get_class_constructors(JNIEnv*, jclass);
jobjectArray reflection_get_class_methods(JNIEnv* jenv, jclass clazz);
jobjectArray reflection_get_parameter_types(JNIEnv *jenv, Method* method);
jobject reflection_get_enum_value(JNIEnv *jenv, Class* enum_type, String* name);
bool jobjectarray_to_jvaluearray(JNIEnv* jenv, jvalue** output, Method* method, jobjectArray input);
jclass descriptor_to_jclass(Type_Info_Handle desc);

#endif // !_REFLECTION_H_
