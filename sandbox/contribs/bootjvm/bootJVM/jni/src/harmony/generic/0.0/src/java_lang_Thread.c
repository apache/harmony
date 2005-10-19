/*!
 * @file java_lang_Thread.c
 *
 * @brief Sample subset of @c @b java.lang.Thread native
 * methods
 *
 * This file contains a stub sample implementation this class.
 *
 * The full implementation of this source file should contain each and
 * every native method that is declared by the implmentation and it
 * should be stored in a shared archive along with the other classes
 * of this Java package's native methods.
 *
 * In this stub sample, the parameter <b><code>(JNIEnv *)</code></b>
 * is @e not considered.  Obviously, this is required for proper
 * linkage in a real implementation.
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
#include "java_lang_Thread.h"
#include "jlThread.h"

#ifdef __cplusplus
extern "C" {
#endif

/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.registerNatives()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    registerNatives
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_registerNatives(JNIEnv *env, jclass jc)
{
   /* Contents to be determined */
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.unregisterNatives()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    unregisterNatives
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_unregisterNatives(JNIEnv *env, jclass jc)
{
   /* Contents to be determined */
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.currentThread()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    currentThread
   Signature: ()Ljava/lang/Thread;
   @endverbatim
 *
 */
JNIEXPORT jobject JNICALL
    Java_java_lang_Thread_currentThread(JNIEnv *env, jclass jc)
{
    return(jlThread_currentThread(jc));
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.yield()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    yield
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_yield(JNIEnv *env, jclass jc)
{
    return(jlThread_yield(jc));
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.interrupt()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    interrupt
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_interrupt(JNIEnv *env, jobject jo)
{
    return(jlThread_interrupt(jo));
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.interrupted()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    interrupted
   Signature: ()Z
   @endverbatim
 *
 */
JNIEXPORT jboolean JNICALL
    Java_java_lang_Thread_interrupted(JNIEnv *env, jclass jc)
{
    return(jlThread_interrupted(jc));
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.isInterrupted()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    isInterrupted
   Signature: ()Z
   @endverbatim
 *
 */
JNIEXPORT jboolean JNICALL
    Java_java_lang_Thread_isInterrupted(JNIEnv *env, jobject jo)
{
    return(jlThread_isInterrupted(jo));
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.sleep(long)
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    sleep
   Signature: (J)V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_sleep__J(JNIEnv *env, jclass jc, jlong jl)
{
    jlThread_sleep(jc, jl);

    return;
}


/*!
 * @brief Native implementation
 * of <b><code>java.lang.Thread.sleep(long, int)</code></b>
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    sleep
   Signature: (JI)V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_sleep__JI(JNIEnv *env,
                                    jclass jc,
                                    jlong jl,
                                    jint ji)
{
    jlThread_sleep_nanos(jc, jl, ji);

    return;
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.join()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    join
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL Java_java_lang_Thread_join(JNIEnv *env,
                                                  jobject jo)
{
    jlThread_join4ever(jo);

    return;
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.join(long)
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    join
   Signature: (J)V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_join__J(JNIEnv *env, jobject jo, jlong jl)
{
    jlThread_jointimed(jo, jl);

    return;
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.join()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    join
   Signature: (JI)V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_join__JI(JNIEnv *env,
                                   jobject jo,
                                   jlong jl,
                                   jint ji)
{
    jlThread_jointimed_nanos(jo, jl, ji);

    return;
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.isAlive()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    isAlive
   Signature: ()Z
   @endverbatim
 *
 */
JNIEXPORT jboolean JNICALL
    Java_java_lang_Thread_isAlive(JNIEnv *env, jobject jo)
{
    return(jlThread_isAlive(jo));
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.start()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    start
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL Java_java_lang_Thread_start(JNIEnv *env,
                                                   jobject jo)
{
    jlThread_start(jo);

    return;
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.countStackFrames()
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
    Java_java_lang_Thread_countStackFrames(JNIEnv *env, jobject jo)
{
    return(jlThread_countStackFrames(jo));
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.holdsLock()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    holdsLock
   Signature: (Ljava/lang/Object;)Z
   @endverbatim
 *
 */
JNIEXPORT jboolean JNICALL
    Java_java_lang_Thread_holdsLock(JNIEnv *env, jclass jc, jobject jo)
{
    return(jlThread_holdsLock(jc, jo));
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.setPriority()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    setPriority
   Signature: (I)V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_setPriority(JNIEnv *env, jobject jo, jint ji)
{
    jlThread_setPriority(jo, ji);

    return;
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.getPriority()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    getPriority
   Signature: ()I
   @endverbatim
 *
 */
JNIEXPORT jint JNICALL
    Java_java_lang_Thread_getPriority(JNIEnv *env, jobject jo)
{
    return(jlThread_getPriority(jo));
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.destroy()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    destroy
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_destroy(JNIEnv *env, jobject jo)
{
    jlThread_destroy(jo);

    return;
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.checkAccess()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    checkAccess
   Signature: ()V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_checkAccess(JNIEnv *env, jobject jo)
{
    jlThread_checkAccess(jo);

    return;
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.setDaemon()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    setDaemon
   Signature: (Z)V
   @endverbatim
 *
 */
JNIEXPORT void JNICALL
    Java_java_lang_Thread_setDaemon(JNIEnv *env,
                                    jobject jo,
                                    jboolean jb)
{
    jlThread_setDaemon(jo, jb);

    return;
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.isDaemon()
 *
 * @verbatim
   Class:     java_lang_Thread
   Method:    isDaemon
   Signature: ()Z
   @endverbatim
 *
 */
JNIEXPORT jboolean JNICALL
    Java_java_lang_Thread_isDaemon(JNIEnv *env, jobject jo)
{
    return(jlThread_isDaemon(jo));
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.stop()
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
    Java_java_lang_Thread_stop(JNIEnv *env, jobject jo)
{
    return(jlThread_stop(jo));
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.suspend()
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
    Java_java_lang_Thread_suspend(JNIEnv *env, jobject jo)
{
    return(jlThread_suspend(jo));
}


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.resume()
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
    Java_java_lang_Thread_resume(JNIEnv *env, jobject jo)
{
    return(jlThread_resume(jo));
}


#ifdef __cplusplus
}
#endif


/* EOF */
