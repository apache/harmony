#ifndef _jlString_h_included_
#define _jlString_h_included_

/*!
 * @file jlString.h
 *
 * @brief Public interface to native implementation of
 * @c @b java.lang.String
 *
 * Two parallel sets of definitions are used here, one for internal
 * implementation purposes, the other for the JNI interface.  The first
 * uses internal data types (via @link #JLSTRING_LOCAL_DEFINED
   \#ifdef JLSTRING_LOCAL_DEFINED@endlink) where the second does not.
 * Instead, it uses @c @b \<jni.h\> data types.  Those types @e must
 * match up for JNI to work, yet by keeping them
 * absolutely separate, application JNI code does @e not have
 * @b any dependencies on the core code of this JVM implementation.
 *
 * Even though there is only apparently @e one set of definitions,
 * the @c @b \#ifdef statement controls which set is used.
 *
 * This file must be included by JNI code along with the
 * @c @b java.lang.Class JNI header file.  The following example
 * shows how to call one of the @e local native methods of this class
 * from the JNI environment.  Notice that although this is not necessary
 * due to the local implementation shortcut defined in
 * @link jvm/src/native.c native.c@endlink, it is not only possible,
 * but sometimes quite desirable to do so.
 *
 * @verbatim
   #include <jni.h>
   #include <solaris/jni_md.h>    ... or appropriate platform-specifics
  
   #include "java_lang_String.h"  ... JNI definitions
   #include "jlString.h"          ... this file
  
   JNIEXPORT jstring JNICALL
       Java_java_lang_String_intern(JNIEnv  *env, jobject  thisobj)
   {
       jstring s;

        s = jlString_intern(thisobj); ... call native implementation

        return(s);
   }
   @endverbatim
 *
 * @attention This local native method implementation is defined
 *            in @link jvm/src/native.c native.c@endlink and
 *            does @e not make use of the @b JNIENV pointer in
 *            @e any manner.
 *
 * @attention Although @link #jvalue jvalue@endlink is indeed a part
 *            of both this implementation and the standard JNI interface
 *            through @c @b \<jni.h\> , it is @e not recommended to use
 *            it if at all possible.  Due to the fact that both
 *            definitions involve unions, along with the slightly
 *            differing contents between the two versions, it is almost
 *            certain that there will be compilation compatibility
 *            problems in the memory layouts from one platform to
 *            another, and possibly between the layouts between them on
 *            any given platform.  Since @link #jvalue jvalue@endlink
 *            is not specificaly a @e Java type, but instead a JNI
 *            construction, this may not be a problem, but this
 *            advisory is raised anyway in order to encourage reliable
 *            implementation of JNI.
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

/**********************************************************************/
#ifdef JLSTRING_LOCAL_DEFINED

ARCH_HEADER_COPYRIGHT_APACHE(jlString, h,
"$URL$",
"$Id$");

/**********************************************************************/
#else /* JLSTRING_LOCAL_DEFINED */

#include "jlObject.h"

/* There is currently nothing else needed here */

#endif /* JLSTRING_LOCAL_DEFINED */
/**********************************************************************/

/*!
 * @name Unified set of prototypes for functions
 * in @link jvm/src/jlString.c jlString.c@endlink
 *
 * @brief JNI table index and external reference to
 * each function that locally implements a JNI native method.
 *
 * The JVM native interface ordinal definition base for this class
 * is 30.  An enumeration is used so the compiler can help the use
 * to not choose duplicate values.
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef enum
{

    JLSTRING_NMO_INTERN = 30  /**< Ordinal for
                            @link #jlString_intern() intern()@endlink */

} jlString_nmo_enum;

/*
 * Add one function prototype below
 * for each local native method enumeration above:
 *
 */

/*!
 * @brief JNI hook to @link #jlString_intern() intern()@endlink
 *
 */
extern jvm_object_hash jlString_intern(jvm_object_hash objhash);

/*@} */ /* End grouped definitions */


/**********************************************************************/

/*!
 * @name Connection to local native method tables.
 *
 * @brief Manifest constant code fragments.
 *
 * These code fragments are designed to be
 * inserted directly into locations in
 * @link jvm/src/native.c native.c@endlink without any other
 * modification to that file except a @e single entry to actually
 * invoke the method.
 *
 */
/*@{*/

/*!
 * @brief Complete list of local native method ordinals
 * for @c @b java.lang.String
 *
 */
#define NATIVE_TABLE_JLSTRING \
    case JLSTRING_NMO_INTERN:

/*!
 * @brief Table of local native methods and their descriptors
 * for @c @b java.lang.String
 *
 */
#define NATIVE_TABLE_JLSTRING_ORDINALS                                 \
    {                                                                  \
        { JLSTRING_NMO_INTERN,     "intern",  "()Ljava/lang/String" }, \
                                                                       \
        /* Add other method entries here */                            \
                                                                       \
                                                                       \
        /* End of table marker */                                      \
        { JVMCFG_JLOBJECT_NMO_NULL,                                    \
         CHEAT_AND_USE_NULL_TO_INITIALIZE,                             \
         CHEAT_AND_USE_NULL_TO_INITIALIZE }                            \
    }

#define NATIVE_TABLE_JLSTRING_JVOID   /**< No @c @b (jvoid) methods */

/*!
 * @brief @c @b (jobject) local native method ordinal table
 * for @c @b java.lang.String
 *
 */
#define NATIVE_TABLE_JLSTRING_JOBJECT \
    case JLSTRING_NMO_INTERN:

#define NATIVE_TABLE_JLSTRING_JINT    /**< No @c @b (jint) methods */
#define NATIVE_TABLE_JLSTRING_JFLOAT  /**< No @c @b (jfloat) methods */
#define NATIVE_TABLE_JLSTRING_JLONG   /**< No @c @b (jlong) methods */
#define NATIVE_TABLE_JLSTRING_JDOUBLE /**< No @c @b (jdouble) methods*/

/*@}*/

#endif /* _jlString_h_included_ */


/* EOF */
