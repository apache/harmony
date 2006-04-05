#ifndef _included_java_lang_Thread_h_
#define _included_java_lang_Thread_h_
/*!
 * @file java_lang_Thread.h
 *
 * @brief Sample subset of @c @b java.lang.Thread native
 * methods
 *
 * The full implementation of this header file should contain each and
 * every native method that is declared by the implementation.
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

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.registerNatives()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    registerNatives
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_registerNatives(JNIEnv *, jclass);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.unregisterNatives()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    unregisterNatives
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_unregisterNatives(JNIEnv *, jclass);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.currentThread()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    currentThread
   Signature: ()Ljava/lang/Thread;
   @endverbatim
 *
 */
JNIEXPORT jobject JNICALL
    Java_java_lang_Thread_currentThread(JNIEnv *, jclass);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.yield()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    yield
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_yield(JNIEnv *, jclass);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.interrupt()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    interrupt
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_interrupt(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.interrupted()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    interrupted
   Signature: ()Z
   @endverbatim
 *
 */
JNIEXPORT jboolean JNICALL
    Java_java_lang_Thread_interrupted(JNIEnv *, jclass);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.isInterrupted()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    isInterrupted
   Signature: ()Z
   @endverbatim
 *
 */
JNIEXPORT jboolean JNICALL
    Java_java_lang_Thread_isInterrupted(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.isInterrupted()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    sleep
   Signature: (J)V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_sleep__J(JNIEnv *, jclass, jlong);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.sleep(long, int)
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    sleep
   Signature: (JI)V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_sleep__JI(JNIEnv *, jclass, jlong, jint);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.join()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    join
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL Java_java_lang_Thread_join(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.join(long)
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    join
   Signature: (J)V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_join__J(JNIEnv *, jobject, jlong);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.join()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    join
   Signature: (JI)V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_join__JI(JNIEnv *, jobject, jlong, jint);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.isAlive()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    isAlive
   Signature: ()Z
   @endverbatim
 *
 */
JNIEXPORT jboolean JNICALL
    Java_java_lang_Thread_isAlive(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.start()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    start
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL Java_java_lang_Thread_start(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.countStackFrames()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    countStackFrames
   Signature: ()I
   @endverbatim
 *
 * @deprecated <b>CAVEAT EMPTOR:</b>  This method has been deprecated
 *                                    in the JDK library API
 *                                    documentation.
 *
 */
JNIEXPORT jint JNICALL
    Java_java_lang_Thread_countStackFrames(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.holdsLock()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    holdsLock
   Signature: (Ljava/lang/Object;)Z
   @endverbatim
 *
 */
JNIEXPORT jboolean JNICALL
    Java_java_lang_Thread_holdsLock(JNIEnv *, jclass, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.setPriority()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    setPriority
   Signature: (I)V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_setPriority(JNIEnv *, jobject, jint);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.getPriority()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    getPriority
   Signature: ()I
   @endverbatim
 *
 */
JNIEXPORT jint JNICALL
    Java_java_lang_Thread_getPriority(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.destroy()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    destroy
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_destroy(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.checkAccess()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    checkAccess
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_checkAccess(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.setDaemon()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    setDaemon
   Signature: (Z)V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_setDaemon(JNIEnv *, jobject, jboolean);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.isDaemon()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    isDaemon
   Signature: ()Z
   @endverbatim
 *
 */
JNIEXPORT jboolean JNICALL
    Java_java_lang_Thread_isDaemon(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.stop()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    stop
   Signature: ()V
   @endverbatim
 *
 * @deprecated <b>CAVEAT EMPTOR:</b>  This method has been deprecated
 *                                    in the JDK library API
 *                                    documentation.
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_stop(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.suspend()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    resume
   Signature: ()V
   @endverbatim
 *
 * @deprecated <b>CAVEAT EMPTOR:</b>  This method has been deprecated
 *                                    in the JDK library API
 *                                    documentation.
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_resume(JNIEnv *, jobject);


/*!
 * @brief Native definition
 * for @c @b java.lang.Thread.suspend()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    suspend
   Signature: ()V
   @endverbatim
 *
 * @deprecated <b>CAVEAT EMPTOR:</b>  This method has been deprecated
 *                                    in the JDK library API
 *                                    documentation.
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_suspend(JNIEnv *, jobject);


#ifdef __cplusplus
}
#endif

#endif /* _included_java_lang_Thread_h_ */

/* EOF */
