#ifndef _jlObject_h_included_
#define _jlObject_h_included_

/*!
 * @file jlObject.h
 *
 * @brief Public interface to native implementation of
 * @c @b java.lang.Object
 *
 * Two parallel sets of definitions are used here, one for internal
 * implementation purposes, the other for the JNI interface.  The first
 * uses internal data types (via @link #JLOBJECT_LOCAL_DEFINED
   \#ifdef JLOBJECT_LOCAL_DEFINED@endlink) where the second does not.
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
   #include <solaris/jni_md.h>   ... or appropriate platform-specifics
   #include "java_lang_Object.h" ... JNI definitions
   #include "jlObject.h"         ... this file
  
   JNIEXPORT jint JNICALL
       Java_java_lang_Object_hashCode(JNIEnv  *env, jobject thisobj)
   {
       jint i;

       i = jlClass_hashCode(thisobj); ... call native implementation

       return(i);
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
#ifdef JLOBJECT_LOCAL_DEFINED

ARCH_HEADER_COPYRIGHT_APACHE(jlObject, h,
"$URL$",
"$Id$");

/**********************************************************************/
#else /* JLOBJECT_LOCAL_DEFINED */

/*!
 * @name Reserved native local method ordinal numbers
 *
 * @brief These ordinal values are reserved for use by the
 * local native method interface as implemented in
 * @link jvm/src/native.c native.c@endlink
 *
 */
/*@{*/
/*!
 * @brief Empty table index for code internals.
 *
 * See also parallel definition in
 * @link #JVMCFG_JLOBJECT_NMO_NULL jvmcfg.h@endlink
 */
#define JLOBJECT_NMO_NULL 0
 

/*!
 * @brief Reserved table index for JNI method registration.
 *
 * See also parallel definition in
 * @link #JVMCFG_JLOBJECT_NMO_REGISTER jvmcfg.h@endlink
 */
#define JLOBJECT_NMO_REGISTER 1


/*!
 * @brief Reserved table index for JNI method <em>de</em>-registration.
 *
 * See also parallel definition in
 * @link #JVMCFG_JLOBJECT_NMO_UNREGISTER jvmcfg.h@endlink
 */
#define JLOBJECT_NMO_UNREGISTER 2

/*}@*/

/*!
 * @name JNI parallel type definitions.
 *
 * @brief Implementation type definitions, but redefined from
 * @c @b \<jni.h\> so no implementation header files are needed by
 * JNI source code.
 *
 * See also respective parallel definition for each one as found 
 * particularly in @link jvm/src/jvmcfg.h jvmcfg.h@endlink
 *
 */

/*@{*/

/*!
 * @brief Implementation type definition for @c @b jobject
 *
 * This symbol is defined for use in the core JVM code in
 * @link jvm/src/jvmcfg.h jvmcfg.h@endlink. 
 *
 * @attention  See comments about @c @b jobject and @c @b jclass in
 *             the local definition of @b jvm_class_index.
 *
 * @todo HARMONY-6-jvm-jlObject.h-1 In the JVM spec, an object reference
 *       seems to look like an arbitrary integer token.  However,
 *       looking at the JNI header file for the Solaris JDK 1.4.2_06
 *       implementation, it appears to be a pointer to an empty
 *       structure.  The apparent purpose of this definition is to
 *       have a @e completely generic definition of use by any
 *       implementation.  @b HOWEVER, there is a problem with this:
 *       What happens when the implementation is ported to a 64-bit
 *       hardware runtime environment?  All pointers change from
         32 bits (namely, from @c @b sizeof(jint)) to 64 bits (namely,
 *       @c @b sizeof(jlong)), taking a second 32-bit word.
 *       This can have @e significant implications for code, and not
 *       only for the JNI interface.  This needs some detailed scrutiny
 *       so that the JNI interface and the implementation as a whole
 *       properly compensates for this situtation or declares it a
 *       non-issue.
 *
 * @note As an aside, and in addition to the above @@todo item, the
 *       consider that many GC implementations use a variation
 *       of "copying garbage collection" (sometimes with adjectives
 *       in front, such as "generational copying garbage collection,"
 *       etc.).  These tend to be more efficient than the old
 *       mark-and-sweep, even though they usually add an extra
 *       layer of indirection.  For example, every pointer might
 *       actually be an index into a table of pointers or perhaps
 *       a pointer to a pointer.  The idea is that the GC algorithm
 *       can relocate/copy the object, knowing it needs to update
 *       only one pointer and all current accesses to the object
 *       at its old location will now be able to access it at its new
 *       location without missing a beat.  In this case, the 32-bit
 *       unsigned int might be an index into a table of pointers and
 *       the pointers might be 64-bits or 32-bits or anything else.
 *
 */
typedef jobject jvm_object_hash;

/*!
 * @brief Implementation type definition for @c @b jclass
 *
 * This symbol is defined for use in the core JVM code in
 * @link jvm/src/jvmcfg.h jvmcfg.h@endlink. 
 *
 * @attention As long as this type definition is the same width or
 *            narrower than the local definition of @b jvm_object_hash,
 *            all code will connect the JNI and JVM environments
 *            properly.  Some Java implementations may consider
 *            @c @b jobject to be an ordinal, some may consider it to
 *            be an array index, some may consider it to be a pointer.
 *            If @c @b jclass is compatible with such definition,
 *            then everything should work fine.  The compiler will
 *            provide the necessary width adjustments without loss of
 *            any significant digits. @e Therefore, notice that this
 *            data type @e cannot be defined as being any @e wider than
 *            @c @b jobject and @b jvm_object_hash.
 */
typedef jclass  jvm_class_index;

/*!
 * @brief Definition of Java @c @b void as used in
 * this implementation.
 *
 * This type definition is @e not typically part
 * of @c @b \<jni.h\> but used extensively here.
 *
 * This symbol is defined for use in the core JVM code in
 * @link jvm/src/jrtypes.h jrtypes.h@endlink. 
 */
typedef void jvoid;

/*@}*/

#endif /* JLOBJECT_LOCAL_DEFINED */
/**********************************************************************/

/*!
 * @name Unified set of prototypes for functions
 * in @link jvm/src/jlObject.c jlObject.c@endlink
 *
 * @brief JNI table index and external reference to
 * each function that locally implements a JNI native method.
 *
 * The JVM native interface ordinal definition base for this class
 * is 10.  An enumeration is used so the compiler can help the use
 * to not choose duplicate values.
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef enum
{

    JLOBJECT_NMO_GETCLASS = 10, /**< Ordinal for
                        @link #jlObject_getClass() getClass()@endlink */

    JLOBJECT_NMO_HASHCODE = 11, /**< Ordinal for
                        @link #jlObject_hashCode() hashCode()@endlink */

    JLOBJECT_NMO_WAIT4EVER = 12, /**< Ordinal for
                      @link #jlObject_wait4ever() wait4ever()@endlink */

    JLOBJECT_NMO_WAITTIMED = 13  /**< Ordinal for
                      @link #jlObject_waittimed() waittimed()@endlink */

} jlObject_nmo_enum;

/*
 * Add one function prototype below
 * for each local native method enumeration above:
 */

/*!
 * @brief JNI hook to @link #jlObject_getClass() getClass()@endlink
 */
jvm_object_hash jlObject_getClass(jvm_object_hash objhash);

/*!
 * @brief JNI hook to @link #jlObject_hashCode() hashCode()@endlink
 */
jint jlObject_hashCode(jvm_object_hash objhash);

/*!
 * @brief JNI hook to @link #jlObject_wait4ever() wait4ever()@endlink
 */
extern jvoid jlObject_wait4ever(jvm_object_hash objhashcurr);

/*!
 * @brief JNI hook to @link #jlObject_waittimed() waittimed()@endlink
 */
extern jvoid jlObject_waittimed(jvm_object_hash objhashcurr,
                                jlong           sleeptime);

/*@} */ /* End grouped definitions */


/*!
 * @name Connection to local native method tables.
 *
 * @brief Manifest constants code fragments.
 *
 * These code fragments are designed to be
 * inserted directly into locations in
 * @link jvm/src/native.c native.c@endlink without any other
 * modification to that file except a @e single entry to actually
 * invoke the method.
 *
 */
/*@{ */ /* Begin grouped definitions */


/*!
 * @brief Complete list of local native method ordinals
 * for @c @b java.lang.Object
 *
 */
#define NATIVE_TABLE_JLOBJECT     \
    case JLOBJECT_NMO_GETCLASS:   \
    case JLOBJECT_NMO_HASHCODE:   \
    case JLOBJECT_NMO_WAIT4EVER:  \
    case JLOBJECT_NMO_WAITTIMED:

/*!
 * @brief Table of local native methods and their descriptors
 * for @c @b java.lang.Object
 *
 */
#define NATIVE_TABLE_JLOBJECT_ORDINALS                                \
    {                                                                 \
        { JLOBJECT_NMO_GETCLASS, "getClass", "()Ljava/lang/Class"  }, \
        { JLOBJECT_NMO_HASHCODE, "hashCode", "()I"  },                \
        { JLOBJECT_NMO_WAIT4EVER, "wait",    "()V"  },                \
        { JLOBJECT_NMO_WAIT4EVER, "wait",    "(J)V" },                \
                                                                      \
        /* Add other method entries here */                           \
                                                                      \
                                                                      \
        /* End of table marker */                                     \
        { JVMCFG_JLOBJECT_NMO_NULL,                                   \
          CHEAT_AND_USE_NULL_TO_INITIALIZE,                           \
          CHEAT_AND_USE_NULL_TO_INITIALIZE }                          \
    }

/*!
 * @brief @c @b (jvoid) local native method ordinal table
 * for @c @b java.lang.Object
 *
 */
#define NATIVE_TABLE_JLOBJECT_JVOID     \
    case JLOBJECT_NMO_WAIT4EVER:        \
    case JLOBJECT_NMO_WAITTIMED:

/*!
 * @brief @c @b (jobject) local native method ordinal table
 * for @c @b java.lang.Object
 *
 */
#define NATIVE_TABLE_JLOBJECT_JOBJECT \
    case JLOBJECT_NMO_GETCLASS:

/*!
 * @brief @c @b (jint) local native method ordinal table
 * for @c @b java.lang.Object
 *
 */
#define NATIVE_TABLE_JLOBJECT_JINT \
    case JLOBJECT_NMO_HASHCODE:

#define NATIVE_TABLE_JLOBJECT_JFLOAT  /**< No @c @b (jfloat) methods */
#define NATIVE_TABLE_JLOBJECT_JLONG   /**< No @c @b (jlong) methods */
#define NATIVE_TABLE_JLOBJECT_JDOUBLE /**< No @c @b (jdouble) methods*/

/*@} */ /* End grouped definitions */


#endif /* _jlObject_h_included_ */


/* EOF */
