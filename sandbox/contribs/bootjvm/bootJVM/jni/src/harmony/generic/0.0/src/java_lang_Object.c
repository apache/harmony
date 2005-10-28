/*!
 * @file java_lang_Object.c
 *
 * @brief Sample subset of @c @b java.lang.Object native
 * methods
 *
 * This file contains a stub sample implementation this class.
 *
 * The full implementation of this source file should contain each and
 * every native method that is declared by the implmentation and it
 * should be stored in a shared archive along with the other classes
 * of this Java package's native methods.
 *
 * In this stub sample, the parameter <b><code>(JNIEnv *)</code></b>
 * is @e not considered.  Obviously, this is required for proper
 * linkage in a real implementation.
 *
 *
 * @section Control
 *
 * \$URL$
 *
 * \$Id$
 *
 * Copyright 2005 The Apache Software Foundation
 * or its licensors, as applicable.
 *
 * Licensed under the Apache License, Version 2.0 ("the License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * @version \$LastChangedRevision$
 *
 * @date \$LastChangedDate$
 *
 * @author \$LastChangedBy$
 *
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

#include "arch.h"
ARCH_SOURCE_COPYRIGHT_APACHE(java_lang_Object, c,
"$URL$",
"$Id$");


#include <jni.h>
#include "java_lang_Object.h"
#include "jlObject.h"

#ifdef __cplusplus
extern "C" {
#endif

/*!
 * @brief Native implementation
 * of @c @b java.lang.Object.registerNatives()
 *
 * @verbatim
   Class:     java_lang_Object
   Method:    registerNatives
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Object_registerNatives(JNIEnv *env, jclass jc)
{
    ARCH_FUNCTION_NAME(Java_java_lang_Object_registerNatives);

   /* Contents to be determined */
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Object.unregisterNatives()
 *
 * @verbatim
   Class:     java_lang_Object
   Method:    unregisterNatives
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Object_unregisterNatives(JNIEnv *env, jclass jc)
{
    ARCH_FUNCTION_NAME(Java_java_lang_Object_unregisterNatives);

   /* Contents to be determined */
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Object.getClass()
 *
 * @verbatim
   Class:     java_lang_Object
   Method:    getClass
   Signature: ()Ljava/lang/Class;
   @endverbatim
 *
 */
JNIEXPORT jobject JNICALL
    Java_java_lang_Object_getClass(JNIEnv *env, jobject jo)
{
    ARCH_FUNCTION_NAME(Java_java_lang_Object_getClass);

    return(jlObject_getClass(jo));
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Object.hashCode()
 *
 * @verbatim
   Class:     java_lang_Object
   Method:    hashCode
   Signature: ()I
   @endverbatim
 *
 */
JNIEXPORT jint JNICALL
    Java_java_lang_Object_hashCode(JNIEnv *env, jobject jo)
{
    ARCH_FUNCTION_NAME(Java_java_lang_Object_hashCode);

    return(jlObject_hashCode(jo));
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Object.wait()
 *
 * @verbatim
   Class:     java_lang_Object
   Method:    wait
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Object_wait(JNIEnv *env, jobject jo)
{
    ARCH_FUNCTION_NAME(Java_java_lang_Object_wait);

    jlObject_wait4ever(jo);

    return;
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Object.wait(long)
 *
 * @verbatim
   Class:     java_lang_Object
   Method:    wait
   Signature: (J)V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Object_wait__J(JNIEnv *env, jobject jo, jlong jl)
{
    ARCH_FUNCTION_NAME(Java_java_lang_Object_wait__J);

    jlObject_waittimed(jo, jl);

    return;
}


#ifdef __cplusplus
}
#endif


/* EOF */
