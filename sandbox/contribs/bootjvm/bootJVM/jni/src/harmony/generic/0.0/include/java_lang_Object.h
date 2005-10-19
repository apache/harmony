#ifndef _included_java_lang_Object_h_
#define _included_java_lang_Object_h_
/*!
 * @file java_lang_Object.h
 *
 * @brief Sample subset of @c @b java.lang.Object native methods
 *
 * The full implementation of this header file should contain each and
 * every native method that is declared by the implmentation.
 *
 *
 * @section Control
 *
 * \$URL$ \$Id$
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
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/*!
 * @brief Native definition
 * for @c @b java.lang.Object.registerNatives()
 *
 * @verbatim
   Class:     java_lang_Object
   Method:    registerNatives
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Object_registerNatives(JNIEnv *, jclass);


/*!
 * @brief Native definition
 * for @c @b java.lang.Object.unregisterNatives()
 *
 * @verbatim
   Class:     java_lang_Object
   Method:    unregisterNatives
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Object_unregisterNatives(JNIEnv *, jclass);


/*!
 * @brief Native definition
 * for @c @b java.lang.Object.getClass()
 *
 * @verbatim
   Class:     java_lang_Object
   Method:    getClass
   Signature: ()Ljava/lang/Class;
   @endverbatim
 *
 */
JNIEXPORT jobject JNICALL
    Java_java_lang_Object_getClass(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Object.hashCode()
 *
 * @verbatim
   Class:     java_lang_Object
   Method:    hashCode
   Signature: ()I
   @endverbatim
 *
 */
JNIEXPORT jint JNICALL
    Java_java_lang_Object_hashCode(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Object.wait()
 *
 * @verbatim
   Class:     java_lang_Object
   Method:    wait
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Object_wait(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Object.wait(long)
 *
 * @verbatim
   Class:     java_lang_Object
   Method:    wait
   Signature: (J)V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Object_wait__J(JNIEnv *, jobject, jlong);


#ifdef __cplusplus
}
#endif

#endif /* _included_java_lang_Object_h_ */

/* EOF */
