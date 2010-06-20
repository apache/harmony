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
 * @author Roman S. Bushmanov
 */

#include "java_lang_System.h"

JNIEXPORT void JNICALL Java_java_lang_System_setErrUnsecure
(JNIEnv *env, jclass clazz, jobject err){
    jfieldID field_id = env->GetStaticFieldID(clazz, "err", "Ljava/io/PrintStream;");
    env->SetStaticObjectField(clazz, field_id, err);
}

JNIEXPORT void JNICALL Java_java_lang_System_setInUnsecure
(JNIEnv *env, jclass clazz, jobject in){
    jfieldID field_id = env->GetStaticFieldID(clazz, "in", "Ljava/io/InputStream;");
    env->SetStaticObjectField(clazz, field_id, in);
}

JNIEXPORT void JNICALL Java_java_lang_System_setOutUnsecure
(JNIEnv *env, jclass clazz, jobject out){
    jfieldID field_id = env->GetStaticFieldID(clazz, "out", "Ljava/io/PrintStream;");
    env->SetStaticObjectField(clazz, field_id, out);
}

JNIEXPORT void JNICALL Java_java_lang_System_rethrow
  (JNIEnv *env, jclass clazz, jthrowable thr){
    env->Throw(thr);
}
