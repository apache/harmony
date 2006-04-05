/*!
 * @file java_lang_String.c
 *
 * @brief Sample subset of @c @b java.lang.String native
 * methods
 *
 * This file contains a stub sample implementation this class.
 *
 * The full implementation of this source file should contain each and
 * every native method that is declared by the implementation and it
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
ARCH_SOURCE_COPYRIGHT_APACHE(java_lang_String, c,
"$URL$",
"$Id$");


#include <jni.h>
#include "java_lang_String.h"
#include "jlString.h"

/*!
 * @internal Force agreement and alignment of fields
 *
 */
#if 0 != JLSTRING_CRITICAL_FIELD_VALUE
#error Critical java.lang.String 'value' field number mismatch
#endif

#if 1 != JLSTRING_CRITICAL_FIELD_LENGTH
#error Critical java.lang.String 'length' field number mismatch
#endif

#if 2 != JLSTRING_CRITICAL_NUM_FIELDS
#error Critical java.lang.String number of fields mismatch
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*!
 * @brief Native implementation
 * of @c @b java.lang.String.registerNatives()
 *
 * @verbatim
   Class:     java_lang_String
   Method:    registerNatives
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_String_registerNatives(JNIEnv *env, jclass jc)
{
    ARCH_FUNCTION_NAME(Java_java_lang_String_registerNatives);

   /* Contents to be determined */
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.String.unregisterNatives()
 *
 * @verbatim
   Class:     java_lang_String
   Method:    unregisterNatives
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_String_unregisterNatives(JNIEnv *env, jclass jc)
{
    ARCH_FUNCTION_NAME(Java_java_lang_String_unregisterNatives);

   /* Contents to be determined */
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.String.intern()
 *
 * @verbatim
   Class:     java_lang_String
   Method:    intern
   Signature: ()Ljava/lang/String;
   @endverbatim
 *
 */
JNIEXPORT jstring JNICALL
    Java_java_lang_String_intern(JNIEnv *env, jobject jo)
{
    ARCH_FUNCTION_NAME(Java_java_lang_String_intern);

    return(jlString_intern(jo));
}


#ifdef __cplusplus
}
#endif


/* EOF */
