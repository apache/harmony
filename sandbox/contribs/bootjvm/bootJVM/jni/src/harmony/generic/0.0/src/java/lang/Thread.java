/*!
 * @file Thread.java
 *
 * @brief Sample subset of @c @b java.lang.Thread native
 * methods
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
 * @brief Java class definition of @c @b java.lang.Thread,
 * the JVM thread model implementation class.
 *
 * The class @c @b java.lang.Thread contains fields methods that
 * implement the threading model for the JVM.  As a class that contains
 * @c @b native calls into the JVM, this stub sample
 * implementation is intended to be filled out into the complete
 * class definition.
 *
 */
public class Thread
{
    /* Please see 'jvm/include/arch.h' for corresponding 'C' defns */
    private static final String copyright =
"\0$URL$ " +
"$Id$ " +
        org.apache.harmony.Copyright.copyrightText;

    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.registerNatives()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    registerNatives
       Signature: ()V
       @endverbatim
     *
     */
    native private static void registerNatives();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.unregisterNatives()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    unregisterNatives
       Signature: ()V
       @endverbatim
     *
     */
    native private static void unregisterNatives();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.currentThread()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    currentThread
       Signature: ()Ljava/lang/Thread;
       @endverbatim
     *
     */
    native public static Thread currentThread();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.yield()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    yield
       Signature: ()V
       @endverbatim
     *
     */
    native public static void yield();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.interrupt()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    interrupt
       Signature: ()V
       @endverbatim
     *
     */
    native public void interrupt();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.interrupted()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    interrupted
       Signature: ()Z
       @endverbatim
     *
     */
    native public static boolean interrupted();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.isInterrupted()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    isInterrupted
       Signature: ()Z
       @endverbatim
     *
     */
    native public boolean isInterrupted();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.sleep(long)
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    sleep
       Signature: (J)V
       @endverbatim
     *
     */
    native public static void sleep(long milliseconds);


    /*!
     * @brief Native definition
     * for <b><code>java.lang.Thread.sleep(long, int)</code></b>
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    sleep
       Signature: (JI)V
       @endverbatim
     *
     */
    native public static void sleep(long milliseconds, int nanoseconds);


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.join()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    join
       Signature: ()V
       @endverbatim
     *
     */
    native public void join();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.join(long)
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    join
       Signature: (J)V
       @endverbatim
     *
     */
    native public void join(long milliseconds);


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.join()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    join
       Signature: (JI)V
       @endverbatim
     *
     */
    native public void join(long milliseconds, int nanoseconds);


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.isAlive()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    isAlive
       Signature: ()Z
       @endverbatim
     *
     */
    native public boolean isAlive();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.start()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    start
       Signature: ()V
       @endverbatim
     *
     */
    native public void start();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.countStackFrames()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    countStackFrames
       Signature: ()I
       @endverbatim
     *
     */
    native public int countStackFrames();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.holdsLock()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    holdsLock
       Signature: (Ljava/lang/Object;)Z
       @endverbatim
     *
     */
    native public static boolean holdsLock(Object object);


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.setPriority()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    setPriority
       Signature: (I)V
       @endverbatim
     *
     */
    native public void setPriority(int priority);


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.getPriority()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    getPriority
       Signature: ()I
       @endverbatim
     *
     */
    native public int getPriority();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.destroy()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    destroy
       Signature: ()V
       @endverbatim
     *
     */
    native public void destroy();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.checkAccess()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    checkAccess
       Signature: ()V
       @endverbatim
     *
     */
    native public void checkAccess();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.setDaemon()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    setDaemon
       Signature: (Z)V
       @endverbatim
     *
     */
    native public void setDaemon(boolean isDaemon);


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.isDaemon()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    isDaemon
       Signature: ()Z
       @endverbatim
     *
     */
    native public boolean isDaemon();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.stop()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    stop
       Signature: ()V
       @endverbatim
     *
     */
    native public void stop();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.suspend()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    suspend
       Signature: ()V
       @endverbatim
     *
     */
    native public void suspend();


    /*!
     * @brief Native definition
     * for @c @b java.lang.Thread.resume()
     *
     * @verbatim
       Class:     java.lang.Thread
       Method:    resume
       Signature: ()V
       @endverbatim
     *
     */
    native public void resume();


} /* END of java.lang.Thread */


/* EOF */
