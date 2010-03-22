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
// This describes the core VM interface to generic object functionality

#ifndef _INTERFACE_OBJECT_H
#define _INTERFACE_OBJECT_H

#include "jni_types.h"

#ifdef __cplusplus
extern "C" {
#endif

// Return the generic hashcode for an object
jint object_get_generic_hashcode(JNIEnv*, jobject);
jint default_hashcode(Managed_Object_Handle obj);

jobject object_clone(JNIEnv*, jobject);

#ifdef __cplusplus
}
#endif
#endif /* _INTERFACE_OBJECT_H */
