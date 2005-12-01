/* Copyright 2004, 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
/* Header for class com_ibm_platform_OSFileSystem */

#if !defined(_Included_com_ibm_platform_OSFileSystem)
#define _Included_com_ibm_platform_OSFileSystem
#if defined(__cplusplus)
extern "C"
{
#endif
/*
 * Class:     com_ibm_platform_OSFileSystem
 * Method:    lockImpl
 * Signature: (JJJIZ)I
 */
  JNIEXPORT jint JNICALL Java_com_ibm_platform_OSFileSystem_lockImpl
    (JNIEnv *, jobject, jlong, jlong, jlong, jint, jboolean);
/*
 * Class:     com_ibm_platform_OSFileSystem
 * Method:    unlockImpl
 * Signature: (JJJ)I
 */
  JNIEXPORT jint JNICALL Java_com_ibm_platform_OSFileSystem_unlockImpl
    (JNIEnv *, jobject, jlong, jlong, jlong);
/*
 * Class:     com_ibm_platform_OSFileSystem
 * Method:    fflushImpl
 * Signature: (JZ)I
 */
  JNIEXPORT jint JNICALL Java_com_ibm_platform_OSFileSystem_fflushImpl
    (JNIEnv *, jobject, jlong, jboolean);
/*
 * Class:     com_ibm_platform_OSFileSystem
 * Method:    seekImpl
 * Signature: (JJI)J
 */
  JNIEXPORT jlong JNICALL Java_com_ibm_platform_OSFileSystem_seekImpl
    (JNIEnv *, jobject, jlong, jlong, jint);
/*
 * Class:     com_ibm_platform_OSFileSystem
 * Method:    readDirectImpl
 * Signature: (JJI)J
 */
  JNIEXPORT jlong JNICALL Java_com_ibm_platform_OSFileSystem_readDirectImpl
    (JNIEnv *, jobject, jlong, jlong, jint);
/*
 * Class:     com_ibm_platform_OSFileSystem
 * Method:    writeDirectImpl
 * Signature: (JJI)J
 */
  JNIEXPORT jlong JNICALL Java_com_ibm_platform_OSFileSystem_writeDirectImpl
    (JNIEnv *, jobject, jlong, jlong, jint);
/*
 * Class:     com_ibm_platform_OSFileSystem
 * Method:    readImpl
 * Signature: (J[BII)J
 */
  JNIEXPORT jlong JNICALL Java_com_ibm_platform_OSFileSystem_readImpl
    (JNIEnv *, jobject, jlong, jbyteArray, jint, jint);
/*
 * Class:     com_ibm_platform_OSFileSystem
 * Method:    mmapImpl
 * Signature: (JJJI)J
 */
  JNIEXPORT jlong JNICALL Java_com_ibm_platform_OSFileSystem_mmapImpl
    (JNIEnv *, jobject, jlong, jlong, jlong, jint);
/*
 * Class:     com_ibm_platform_OSFileSystem
 * Method:    closeImpl
 * Signature: (J)I
 */
  JNIEXPORT jint JNICALL Java_com_ibm_platform_OSFileSystem_closeImpl
    (JNIEnv *, jobject, jlong);
#if defined(__cplusplus)
}
#endif
#endif
