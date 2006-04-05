/*!
 * @file String.java
 *
 * @brief Sample subset of @c @b java.lang.String native
 * methods
 *
 * The full implementation of this source file should contain each and
 * every native method that is declared by the implementation and it
 * should be stored in a shared archive along with the other classes
 * of this Java package's native methods.
 *
 * An important item here is the storage location of the string's
 * data char[] array.  It is needed in order to complete
 * @link #object_utf8_string_lookup()
   object_utf8_string_lookup()@endlink and other string routines.
 *
 * @see @link jvm/src/object.c jvm/src/object.c@endlink
 *
 * @see @link jvm/include/jlString.h jvm/include/jlString.h@endlink
 *
 * @see @link jvm/src/native.c jvm/src/native.c@endlink
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

package java.lang;

import org.apache.harmony.Copyright.*;

/*!
 * @brief Java class definition of @c @b java.lang.String,
 * the Java pseudo-primative class for defining and manipulating
 * variable-length groups of characters.
 *
 * The class @c @b java.lang.String contains fields of multiple
 * Unicode (jchar) characters and methods for manipulating them.
 * As a class that contains @c @b native calls into the JVM,
 * this stub sample implementation is intended to be filled out into
 * the complete class definition.
 *
 */
public class String
{
/******** BEGIN:  DO NOT CHANGE THE ORDER OF THESE FIELD DEFINITIONS! */


    /* Please see 'jvm/include/arch.h' for corresponding 'C' defns */
    private static final String copyright =
"\0$URL$ " +
"$Id$ " +
        org.apache.harmony.Copyright.copyrightText;
        /**< Class static variable field index 0 (not critical) */

    /*!
     * @warning <b>DO NOT CHANGE THE POSITION OR ORDER OF THESE
     *          DECLARATIONS!</b>
     *
     *          This is @e required for proper operation of the native
     *          interface and of the core string contents loading in
     *          @link #object_instance_new()
                object_instance_new()@endlink as defined in
     *          @link jvm/include/jlString.h
                jvm/include/jlString.h@endlink and
     *          @link jvm/src/native.c jvm/src/native.c@endlink.
     *
     * @todo HARMONY-6-jni-String.java-1 Verify that this order is
     *       actually what is laid down in the compilation of this
     *       class.  @e Hopefully, it is not compiler-dependent.
     *
     */

    private char value[]; /**< Object instance variable field index 0 */

    private int length; /**< Object instance variable field index 1 */

/******** END: DO NOT CHANGE THE ORDER OF THESE FIELD DEFINITIONS! ****/

    /*!
     * @brief Native definition
     * for @c @b java.lang.String.registerNatives()
     *
     * @verbatim
       Class:     java.lang.String
       Method:    registerNatives
       Signature: ()V
       @endverbatim
     *
     */
    native private static void registerNatives();


    /*!
     * @brief Native definition
     * for @c @b java.lang.String.unregisterNatives()
     *
     * @verbatim
       Class:     java.lang.String
       Method:    unregisterNatives
       Signature: ()V
       @endverbatim
     *
     */
    native private static void unregisterNatives();


    /*!
     * @brief Native definition
     * for @c @b java.lang.String.intern()
     *
     * @verbatim
       Class:     java.lang.String
       Method:    intern
       Signature: ()Ljava/lang/String;
       @endverbatim
     *
     */
    native public String intern();

} /* END of java.lang.String */


/* EOF */
