#ifndef _included_java_lang_Class_h_
#define _included_java_lang_Class_h_
/*!
 * @file java_lang_Class.h
 *
 * @brief Sample subset of @c @b java.lang.Class native
 * methods
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
 * for @c @b java.lang.Class.registerNatives()
 *
 * @verbatim
   Class:     java_lang_Class
   Method:    registerNatives
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Class_registerNatives(JNIEnv *, jclass);


/*!
 * @brief Native definition
 * for @c @b java.lang.Class.unregisterNatives()
 *
 * @verbatim
   Class:     java_lang_Class
   Method:    unregisterNatives
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Class_unregisterNatives(JNIEnv *, jclass);


/*!
 * @brief Native definition
 * for @c @b java.lang.Class.isArray()
 *
 * @verbatim
   Class:     java_lang_Class
   Method:    isArray
   Signature: ()Z
   @endverbatim
 *
 */
JNIEXPORT jboolean JNICALL
    Java_java_lang_Class_isArray(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Class.isPrimative()
 *
 * @verbatim
   Class:     java_lang_Class
   Method:    isPrimitive
   Signature: ()Z
   @endverbatim
 *
 */
JNIEXPORT jboolean JNICALL
    Java_java_lang_Class_isPrimitive(JNIEnv *, jobject);


#ifdef __cplusplus
}
#endif

#endif /* _included_java_lang_String_h_ */

/* EOF */
