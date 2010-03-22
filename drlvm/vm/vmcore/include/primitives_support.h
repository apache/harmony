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
* @author Intel, Alexey V. Varlamov
*/  

#ifndef _PRIMITIVES_SUPPORT_H_
#define _PRIMITIVES_SUPPORT_H_

#include "jni_types.h"

bool widen_primitive_jvalue(jvalue* val, char from_type, char to_type);
char is_wrapper_class(const char*);
jobject wrap_primitive(JNIEnv *env, jvalue value, char sig);
jvalue  unwrap_primitive(JNIEnv *env, jobject wobj, char sig);

#endif // !_PRIMITIVES_SUPPORT_H_
