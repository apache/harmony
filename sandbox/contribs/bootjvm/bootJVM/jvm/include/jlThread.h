#ifndef _jlThread_h_included_
#define _jlThread_h_included_

/*!
 * @file jlThread.h
 *
 * @brief Public interface to native implementation of
 * @c @b java.lang.Thread
 *
 * Two parallel sets of definitions are used here, one for internal
 * implementation purposes, the other for the JNI interface.  The first
 * uses internal data types (via @link #JLTHREAD_LOCAL_DEFINED
   \#ifdef JLTHREAD_LOCAL_DEFINED@endlink) where the second does not.
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
  
   #include "java_lang_Thread.h"  ... JNI definitions
   #include "jlThread.h"          ... this file
  
   JNIEXPORT jboolean JNICALL
       Java_java_lang_Thread_holdsLock(JNIEnv *env,
                                       jclass  thisclass,
                                       jobject thisobj)
   {
       jboolean b;

       b = jlThread_holdsLock(thisclass,
                              thisobj); ... call native implementation

       return(b);
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
 * \$URL: https://svn.apache.org/path/name/jlThread.h $ \$Id: jlThread.h 0 09/28/2005 dlydick $
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
 * @version \$LastChangedRevision: 0 $
 *
 * @date \$LastChangedDate: 09/28/2005 $
 *
 * @author \$LastChangedBy: dlydick $
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

/**********************************************************************/
#ifdef JLTHREAD_LOCAL_DEFINED

ARCH_COPYRIGHT_APACHE(jlThread, h, "$URL: https://svn.apache.org/path/name/jlThread.h $ $Id: jlThread.h 0 09/28/2005 dlydick $");

/**********************************************************************/
#else /* JLTHREAD_LOCAL_DEFINED */

#include "jlObject.h"

/* There is currently nothing else needed here */

#endif /* JLTHREAD_LOCAL_DEFINED */
/**********************************************************************/

/*!
 * @name Unified set of prototypes for functions
 * in @link jvm/src/jlThread.c jlThread.c@endlink
 *
 * @brief JNI table index and external reference to
 * each function that locally implements a JNI native method.
 *
 * The JVM native interface ordinal definition base for this class
 * is 40.  An enumeration is used so the compiler can help the use
 * to not choose duplicate values.
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef enum
{

    JLTHREAD_NMO_CURRENTTHREAD = 40, /**< Ordinal for
                      @link #jlThread_currentThread() yield()@endlink */

    JLTHREAD_NMO_YIELD = 41,        /**< Ordinal for
                              @link #jlThread_yield() yield()@endlink */

    JLTHREAD_NMO_INTERRUPT = 42,    /**< Ordinal for
                       @link #jlThread_interrupt() interrupt()@endlink*/

    JLTHREAD_NMO_INTERRUPTED = 43,  /**< Ordinal for
                  @link #jlThread_interrupted() interrupted()@endlink */

    JLTHREAD_NMO_ISINTERRUPTED = 44, /**< Ordinal for
              @link #jlThread_isInterrupted() isInterrupted()@endlink */

    JLTHREAD_NMO_SLEEP = 45,        /**< Ordinal for
                              @link #jlThread_sleep() sleep()@endlink */

    JLTHREAD_NMO_SLEEP_NANOS = 46,  /**< Ordinal for
                  @link #jlThread_sleep_nanos() sleep_nanos()@endlink */

    JLTHREAD_NMO_JOIN4EVER = 47,    /**< Ordinal for
                       @link #jlThread_join4ever() join4ever()@endlink*/

    JLTHREAD_NMO_JOINTIMED = 48,    /**< Ordinal for
                       @link #jlThread_jointimed() jointimed()@endlink*/

    JLTHREAD_NMO_JOINTIMED_NANOS = 49, /**< Ordinal for
          @link #jlThread_jointimed_nanos() jointimed_nanos()@endlink */

    JLTHREAD_NMO_ISALIVE = 50,        /**< Ordinal for
                       @link #jlThread_isAlive() isAlive()@endlink */

    JLTHREAD_NMO_START = 51,          /**< Ordinal for
                           @link #jlThread_start() holdsLock()@endlink*/

    JLTHREAD_NMO_COUNTSTACKFRAMES = 52, /**< Ordinal for
        @link #jlThread_countStackFrames() countStackFrames()@endlink */

    JLTHREAD_NMO_HOLDSLOCK = 53,      /**< Ordinal for
                       @link #jlThread_holdsLock() holdsLock()@endlink*/

    JLTHREAD_NMO_SETPRIORITY = 54,    /**< Ordinal for
                  @link #jlThread_setPriority() setPriority()@endlink */

    JLTHREAD_NMO_GETPRIORITY = 55,    /**< Ordinal for
                  @link #jlThread_getPriority() getPriority()@endlink */

    JLTHREAD_NMO_DESTROY = 56,        /**< Ordinal for
                          @link #jlThread_destroy() destroy()@endlink */

    JLTHREAD_NMO_CHECKACCESS = 57,    /**< Ordinal for
                  @link #jlThread_checkAccess() checkAccess()@endlink */

    JLTHREAD_NMO_SETDAEMON = 58,      /**< Ordinal for
                      @link #jlThread_setDaemon() setDaemon()@endlink */

    JLTHREAD_NMO_ISDAEMON = 59,       /**< Ordinal for
                        @link #jlThread_isDaemon() isDaemon()@endlink */

    JLTHREAD_NMO_STOP = 60,           /**< Ordinal for
                                @link #jlThread_stop() stop()@endlink */

    JLTHREAD_NMO_SUSPEND = 61,        /**< Ordinal for
                             @link #jlThread_suspend() stop()@endlink */

    JLTHREAD_NMO_RESUME = 62,         /**< Ordinal for
                             @link #jlThread_resume() stop()@endlink */

} jlThread_nmo_enum;

/*
 * Add one function prototype below
 * for each local native method enumeration above:
 */

/*!
 * @brief JNI hook to @link #jlThread_currentThread()
   currentThread()@endlink
 */
extern
    jvm_object_hash jlThread_currentThread(jvm_class_index clsidxTHR);

/*!
 * @brief JNI hook to @link #jlThread_yield() yield()@endlink
 */
extern jvoid jlThread_yield(jvm_class_index clsidxTHR);

/*!
 * @brief JNI hook to @link #jlThread_interrupt() interrupt()@endlink
 */
extern jvoid jlThread_interrupt(jvm_object_hash objhashthis);

/*!
 *@brief JNI hook to @link #jlThread_interrupted() interrupted()@endlink
 */
extern jboolean jlThread_interrupted(jvm_object_hash objhashTHR);

/*!
 * @brief JNI hook to
 * @link #jlThread_isInterrupted() isInterrupted()@endlink
 */
extern jboolean jlThread_isInterrupted(jvm_object_hash objhashthis);

/*!
 * @brief JNI hook to @link #jlThread_sleep() sleep()@endlink
 */
extern jboolean jlThread_sleep(jvm_class_index clsidxTHR,
                               jlong           sleeptime_milliseconds);

/*!
 * @brief JNI hook to
 * @link #jlThread_sleep_nanos() sleep_nanos()@endlink
 */
extern jboolean jlThread_sleep_nanos(jvm_class_index clsidxTHR,
                                     jlong       sleeptime_milliseconds,
                                     jint        sleeptime_nanoseconds);

/*!
 * @brief JNI hook to @link #jlThread_join4ever() join4ever()@endlink
 */
extern jvoid jlThread_join4ever(jvm_object_hash objhashthis);

/*!
 * @brief JNI hook to @link #jlThread_jointimed() jointimed()@endlink
 */
extern jvoid jlThread_jointimed(jvm_object_hash objhashthis,
                                jlong           sleeptime);

/*!
 * @brief JNI hook to
 * @link #jlThread_jointimed_nanos() jointimed_nanos()@endlink
 */
extern jvoid jlThread_jointimed_nanos(jvm_object_hash objhashthis,
                                      jlong           sleeptime,
                                      jint          sleeptime_nanos);

/*!
 * @brief JNI hook to @link #jlThread_jointimed() jointimed()@endlink
 */
extern jboolean jlThread_isAlive(jvm_object_hash objhashthis);

/*!
 * @brief JNI hook to @link #jlThread_start() start()@endlink
 */
extern jboolean jlThread_start(jvm_object_hash objhashthis);

/*!
 * @brief JNI hook to
 * @link #jlThread_countStackFrames() countStackFrames()@endlink
 */
extern jint jlThread_countStackFrames(jvm_object_hash objhashthis);

/*!
 * @brief JNI hook to @link #jlThread_holdsLock() holdsLock()@endlink
 */
extern jboolean jlThread_holdsLock(jvm_class_index clsidxTHR,
                                   jvm_object_hash objhashLOCK);

/*!
 * @brief JNI hook to
 * @link #jlThread_setPriority() setPriority()@endlink
 */
extern jboolean jlThread_setPriority(jvm_object_hash objhashthis,
                                     jint             priority);

/*!
 * @brief JNI hook to
 * @link #jlThread_getPriority() getPriority()@endlink
 */
extern jint jlThread_getPriority(jvm_object_hash objhashthis);

/*!
 * @brief JNI hook to @link #jlThread_destroy() destroy()@endlink
 */
extern jboolean jlThread_destroy(jvm_object_hash objhashthis);

/*!
 * @brief JNI hook to
 * @link #jlThread_checkAccess() checkAccess()@endlink
 */
extern jboolean jlThread_checkAccess(jvm_object_hash objhashthis);

/*!
 * @brief JNI hook to @link #jlThread_setDaemon() setDaemon()@endlink
 */
extern jvoid jlThread_setDaemon(jvm_object_hash objhashthis,
                                jboolean isdaemon);

/*!
 * @brief JNI hook to @link #jlThread_isDaemon() isDaemon()@endlink
 */
extern jboolean jlThread_isDaemon(jvm_object_hash objhashthis);

/*!
 * @brief JNI hook to @link #jlThread_stop() stop()@endlink
 */
extern jvoid jlThread_stop(jvm_object_hash objhashthis);

/*!
 * @brief JNI hook to @link #jlThread_suspend() suspend()@endlink
 */
extern jvoid jlThread_suspend(jvm_object_hash objhashthis);

/*!
 * @brief JNI hook to @link #jlThread_resume() resume()@endlink
 */
extern jvoid jlThread_resume(jvm_object_hash objhashthis);

/*@} */ /* End grouped definitions */


/**********************************************************************/

/*!
 * @name Connection to local native method tables.
 *
 * @brief These manifest constant code fragments are designed to be
 * inserted directly into locations in
 * @link jvm/src/native.c native.c@endlink without any other
 * modification to that file except a @e single entry to actually
 * invoke the method.
 *
 */
/*@{*/

/*!
 * @brief Complete list of local native method ordinals
 * for @c @b java.lang.Thread
 */
#define NATIVE_TABLE_JLTHREAD           \
    case JLTHREAD_NMO_CURRENTTHREAD:    \
    case JLTHREAD_NMO_YIELD:            \
    case JLTHREAD_NMO_INTERRUPT:        \
    case JLTHREAD_NMO_INTERRUPTED:      \
    case JLTHREAD_NMO_ISINTERRUPTED:    \
    case JLTHREAD_NMO_SLEEP:            \
    case JLTHREAD_NMO_SLEEP_NANOS:      \
    case JLTHREAD_NMO_JOIN4EVER:        \
    case JLTHREAD_NMO_JOINTIMED:        \
    case JLTHREAD_NMO_JOINTIMED_NANOS:  \
    case JLTHREAD_NMO_ISALIVE:          \
    case JLTHREAD_NMO_START:            \
    case JLTHREAD_NMO_COUNTSTACKFRAMES: \
    case JLTHREAD_NMO_HOLDSLOCK:        \
    case JLTHREAD_NMO_SETPRIORITY:      \
    case JLTHREAD_NMO_GETPRIORITY:      \
    case JLTHREAD_NMO_DESTROY:          \
    case JLTHREAD_NMO_CHECKACCESS:      \
    case JLTHREAD_NMO_SETDAEMON:        \
    case JLTHREAD_NMO_ISDAEMON:         \
    case JLTHREAD_NMO_STOP:

/*!
 * @brief Table of local native methods and their descriptors
 * for @c @b java.lang.Thread
 */
#define NATIVE_TABLE_JLTHREAD_ORDINALS                                 \
    {                                                                  \
/*static*/ { JLTHREAD_NMO_CURRENTTHREAD,"currentThread",               \
                                           "()Ljava/lang/Thread;"   }, \
/*static*/ { JLTHREAD_NMO_YIELD,        "yield",            "()V"   }, \
           { JLTHREAD_NMO_INTERRUPT,    "interrupt",        "()V"   }, \
/*static*/ { JLTHREAD_NMO_INTERRUPTED,  "interrupted",      "()Z"   }, \
           { JLTHREAD_NMO_ISINTERRUPTED,"isInterrupted",    "()Z"   }, \
/*static*/ { JLTHREAD_NMO_SLEEP,        "sleep",            "(J)V"  }, \
/*static*/ { JLTHREAD_NMO_SLEEP_NANOS,  "sleep",            "(JI)V" }, \
           { JLTHREAD_NMO_JOIN4EVER,    "join",             "()V"   }, \
           { JLTHREAD_NMO_JOINTIMED,    "join",             "(J)V"  }, \
           { JLTHREAD_NMO_JOINTIMED_NANOS,                             \
                                        "join",             "(JI)V" }, \
           { JLTHREAD_NMO_ISALIVE,      "isAlive",          "()Z"   }, \
           { JLTHREAD_NMO_START,        "start",            "()V"   }, \
           { JLTHREAD_NMO_COUNTSTACKFRAMES,                            \
                                        "countStackFrames", "()I"   }, \
/*static*/ { JLTHREAD_NMO_HOLDSLOCK,    "holdsLock",                   \
                                           "(Ljava/lang/Object;)Z" },  \
           { JLTHREAD_NMO_SETPRIORITY,  "setPriority",     "(I)V"  },  \
           { JLTHREAD_NMO_GETPRIORITY,  "getPriority",     "()I"   },  \
           { JLTHREAD_NMO_DESTROY,      "destroy",         "()V"   },  \
           { JLTHREAD_NMO_CHECKACCESS,  "checkAccess",     "()V"   },  \
           { JLTHREAD_NMO_SETDAEMON,    "setDaemon",       "(Z)V"  },  \
           { JLTHREAD_NMO_ISDAEMON,     "isDaemon",        "()Z"   },  \
           { JLTHREAD_NMO_STOP,         "stop",            "()V"   },  \
           { JLTHREAD_NMO_SUSPEND,      "suspend",         "()V"   },  \
           { JLTHREAD_NMO_RESUME,       "resume",          "()V"   },  \
                                                                       \
        /* Add other method entries here */                            \
                                                                       \
                                                                       \
        /* End of table marker, regardless of static array[size] */    \
        { JVMCFG_JLOBJECT_NMO_NULL,                                    \
          CHEAT_AND_USE_NULL_TO_INITIALIZE,                            \
          CHEAT_AND_USE_NULL_TO_INITIALIZE }                           \
    }

/*!
 * @brief @c @b (jvoid) local native method ordinal table
 * for @c @b java.lang.Thread
 */
#define NATIVE_TABLE_JLTHREAD_JVOID     \
    case JLTHREAD_NMO_YIELD:            \
    case JLTHREAD_NMO_INTERRUPT:        \
    case JLTHREAD_NMO_SLEEP:            \
    case JLTHREAD_NMO_SLEEP_NANOS:      \
    case JLTHREAD_NMO_JOIN4EVER:        \
    case JLTHREAD_NMO_JOINTIMED:        \
    case JLTHREAD_NMO_JOINTIMED_NANOS:  \
    case JLTHREAD_NMO_START:            \
    case JLTHREAD_NMO_SETPRIORITY:      \
    case JLTHREAD_NMO_DESTROY:          \
    case JLTHREAD_NMO_CHECKACCESS:      \
    case JLTHREAD_NMO_SETDAEMON:        \
    case JLTHREAD_NMO_STOP:             \
    case JLTHREAD_NMO_SUSPEND:          \
    case JLTHREAD_NMO_RESUME:

/*!
 * @brief @c @b (jobject) local native method ordinal table
 * for @c @b java.lang.Thread
 */
#define NATIVE_TABLE_JLTHREAD_JOBJECT   \
    case JLTHREAD_NMO_CURRENTTHREAD:

/*!
 * @brief @c @b (jint) local native method ordinal table
 * for @c @b java.lang.Thread
 */
#define NATIVE_TABLE_JLTHREAD_JINT      \
    case JLTHREAD_NMO_INTERRUPTED:      \
    case JLTHREAD_NMO_ISINTERRUPTED:    \
    case JLTHREAD_NMO_ISALIVE:          \
    case JLTHREAD_NMO_COUNTSTACKFRAMES: \
    case JLTHREAD_NMO_HOLDSLOCK:        \
    case JLTHREAD_NMO_GETPRIORITY:      \
    case JLTHREAD_NMO_ISDAEMON:

#define NATIVE_TABLE_JLTHREAD_JFLOAT  /**< No @c @b (jfloat) methods */
#define NATIVE_TABLE_JLTHREAD_JLONG   /**< No @c @b (jlong) methods */
#define NATIVE_TABLE_JLTHREAD_JDOUBLE /**< No @c @b (jdouble) methods*/

/*@}*/

#endif /* _jlThread_h_included_ */


/* EOF */
