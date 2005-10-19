/*!
 * @file String.java
 *
 * @brief Sample subset of @c @b java.lang.String native
 * methods
 *
 * The full implementation of this source file should contain each and
 * every native method that is declared by the implmentation and it
 * should be stored in a shared archive along with the other classes
 * of this Java package's native methods.
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

package java.lang;

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
