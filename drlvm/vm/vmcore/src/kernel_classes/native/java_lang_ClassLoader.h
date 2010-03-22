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

#include <jni.h>


/* Header for class java.lang.ClassLoader */

#ifndef _JAVA_LANG_CLASSLOADER_H
#define _JAVA_LANG_CLASSLOADER_H

#ifdef __cplusplus
extern "C" {
#endif


/* Native methods */

/*
 * Method: java.lang.ClassLoader.findLoadedClass(Ljava/lang/String;)Ljava/lang/Class;
 */
JNIEXPORT jclass JNICALL
Java_java_lang_ClassLoader_findLoadedClass(JNIEnv *, jobject, 
    jstring);

/*
 * Method: java.lang.ClassLoader.findLoadedClass(Ljava/lang/String;)Ljava/lang/Class;
 */
JNIEXPORT void JNICALL
Java_java_lang_ClassLoader_registerInitiatedClass(JNIEnv *, jobject, jclass);

/*                                          
 * Method: java.lang.ClassLoader.defineClass0(Ljava/lang/String;[BII)Ljava/lang/Class;
 * Throws: java.lang.ClassFormatError
 */
JNIEXPORT jclass JNICALL
Java_java_lang_ClassLoader_defineClass0(JNIEnv *, jobject, 
    jstring, jbyteArray, jint, jint);


#ifdef __cplusplus
}
#endif

#endif /* _JAVA_LANG_CLASSLOADER_H */
