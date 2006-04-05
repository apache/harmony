/*!
 * @file Class.java
 *
 * @brief Sample subset of @c @b java.lang.Class native
 * methods
 *
 * This file contains a stub sample implementation this class.
 *
 * The full implementation of this source file should contain each and
 * every native method that is declared by the implementation and it
 * should be stored in a shared archive along with the other classes
 * of this Java package's native methods.
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
 * @brief Java class definition of @c @b java.lang.Class, a
 * class that describes the class features of any Java object.
 *
 * The class @c @b java.lang.Class contains methods that
 * describe all objects.  As a class that contains @c @b native
 * calls into the JVM, this stub sample implementation is intended
 * to be filled out into the complete class definition.
 *
 */
public class Class
{
    /* Please see 'jvm/include/arch.h' for corresponding 'C' defns */
    private static final String copyright =
 "\0$URL$ " +
 "$Id$ " +
        org.apache.harmony.Copyright.copyrightText;

    /*!
     * @brief Native definition
     * for @c @b java.lang.Class.registerNatives()
     *
     * @verbatim
       Class:     java.lang.Class
       Method:    registerNatives
       Signature: ()V
       @endverbatim
     *
     */
    native private static void registerNatives();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Class.unregisterNatives()
     *
     * @verbatim
       Class:     java.lang.Class
       Method:    unregisterNatives
       Signature: ()V
       @endverbatim
     *
     */
    native private static void unregisterNatives();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Class.isArray()
     *
     * @verbatim
       Class:     java.lang.Class
       Method:    isArray
       Signature: ()Z
       @endverbatim
     *
     */
    native public boolean isArray();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Class.isPrimative()
     *
     * @verbatim
       Class:     java.lang.Class
       Method:    isPrimitive
       Signature: ()Z
       @endverbatim
     *
     */
    native public boolean isPrimative();

} /* END of java.lang.Class */


/* EOF */
