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
#ifndef _NATIVE_UTILS
#define _NATIVE_UTILS

#include "Class.h"
#include "jni.h"
struct Class;

extern char* JavaStringToCharArray (JNIEnv*, jstring, jint*);
/// toss this, a jni interface does the same thing ------> extern jstring CharArrayToJavaString (JNIEnv *, const char*, jsize);



extern Field *LookupDeclaredField (Class*, const char*);
extern Method* LookupDeclaredMethod (Class*, const char*, const char*);


void VerifyArray (JNIEnv* env, jarray array);
char GetComponentSignature (JNIEnv *env, jarray array);

extern jboolean IsNullRef(jobject jobj);
#endif /* _NATIVE_UTILS */
