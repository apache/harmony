/*!
 * @file Object.java
 *
 * @brief Sample subset of @c @b java.lang.Object native
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

/*!
 * @brief Java class definition of @c @b java.lang.Object,
 * the class that is the root of the object hierarchy.
 *
 * The class @c @b java.lang.object is the root of the
 * object hierarchy.  As a class that contains @c @b native
 * calls into the JVM, this stub sample implementation is intended
 * to be filled out into the complete class definition.
 *
 */
public class Object
{
    /*!
     * @brief Native definition
     * for @c @b java.lang.Object.registerNatives()
     *
     * @verbatim
       Class:     java.lang.Object
       Method:    registerNatives
       Signature: ()V
       @endverbatim
     *
     */
    native private static void registerNatives();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Object.unregisterNatives()
     *
     * @verbatim
       Class:     java.lang.Object
       Method:    unregisterNatives
       Signature: ()V
       @endverbatim
     *
     */
    native private static void unregisterNatives();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Object.getClass()
     *
     * @verbatim
       Class:     java.lang.Object
       Method:    getClass
       Signature: ()Ljava/lang/Class;
       @endverbatim
     *
     */
    native public Class getClass();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Object.hashCode()
     *
     * @verbatim
       Class:     java.lang.Object
       Method:    hashCode
       Signature: ()I
       @endverbatim
     *
     */
    native public int hashCode();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Object.wait()
     *
     * @verbatim
       Class:     java.lang.Object
       Method:    wait
       Signature: ()V
       @endverbatim
     *
     */
    native public void wait();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Object.wait(long)
     *
     * @verbatim
       Class:     java.lang.Object
       Method:    wait
       Signature: (J)V
       @endverbatim
     *
     */
    native public long wait(long milliseconds);

} /* END of java.lang.Object */


/* EOF */
