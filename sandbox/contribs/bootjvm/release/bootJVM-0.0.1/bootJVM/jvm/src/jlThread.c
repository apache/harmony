/*!
 * @file jlThread.c
 *
 * @brief Native implementation of @c @b java.lang.Thread
 *
 * @todo  HARMONY-6-jvm-jlThread.c-1 Perform intelligent check on
 *        input parameter @b objhash range for all functions.
 *
 * @todo  HARMONY-6-jvm-jlThread.c-2 In real life, the @b objhashthis
 *        values and @b clsidxthis values will be valid or these
 *        functions could not be invoked since these data types
 *        are @e mandatory for referencing them.  This probably
 *        means that the parameter valididty checking could probably
 *        be relaxed.
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

#include "arch.h"
ARCH_SOURCE_COPYRIGHT_APACHE(jlThread, c,
"$URL$",
"$Id$");


#include "jvmcfg.h"
#include "classfile.h"
#include "jvm.h"
#include "jvmclass.h"
#include "linkage.h"


/*!
 * @name Native implementation of java.lang.Thread.sleep() functions.
 *
 * @brief Sleep based on millisecond timer ticks.
 *
 * Results are undefined if thread has the @b JOIN4EVER, @b JOINTIMED,
 * @b WAIT4EVER, @b WAITTIMED, or @b INTERRUPTIBLEIO status or if thread
 * has been @b NOTIFIED or @b INTERRUPTED.
 *
 * This will only succeed if thread is in @b RUNNING state.
 *
 * The <b><code>sleep(ms, ns)</code></b> version ignores the
 * nanoseconds parameter and works just like
 * <b><code>sleep(ms)</code></b>.
 *
 * The class index of the current class is always passed
 * as the first parameter.
 *
 *
 * @param  clsidxthis              Class table index of the class of
 *                                 @c @b this object, namely,
 *                                 @c @b java.lang.Thread .
 *
 * @param  sleeptime_milliseconds  Number of timer ticks (milliseconds)
 *                                 to sleep.
 *
 * @param  sleeptime_nanoseconds   Number of nanoseconds to sleep
 *                                 in addition to the milliseconds.
 *
 *
 * @returns @link #jvoid jvoid@endlink
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERRUPTEDEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_INTERRUPTEDEXCEPTION
           if another thread had interrupted this thread@endlink.
 *
 *
 * @note These @c @b java.lang.Thread methods are unusual in that
 *       they does not require a @c @b jobject (in parlance of this
 *       implementation, a
 *       @link #jvm_object_hash jvm_object_hash@endlink)
 *       to run because they are declared as @c @b static methods.  As
 *       implemented here, the usual @b objhashthis parameter is
 *       therefore replaced by @b clsidxthis.  The thread context is
 *       located in @link #CURRENT_THREAD CURRENT_THREAD@endlink.
 *
 *
 * @todo HARMONY-6-jvm-jlThread.c-3 Make sure thread interruption
 *       logic below here is working.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Native implementation of millisecond
 * sleep method @c @b java.lang.Thread.sleep(jlong)
 *
 */

jvoid jlThread_sleep(jvm_class_index clsidxthis,
                     jlong           sleeptime_milliseconds)
{
    ARCH_FUNCTION_NAME(jlThread_sleep);

    /* Current thread always assumed valid */
    jvm_thread_index thridx = CURRENT_THREAD;

    THREAD(thridx).status |= THREAD_STATUS_SLEEP;
    THREAD(thridx).sleeptime = sleeptime_milliseconds;
    (rvoid) threadstate_request_runnable(thridx);

    return;

} /* END of jlThread_sleep() */


/*!
 *
 * @brief Native implementation of millisecond and nanosecond
 * sleep method <b><code>java.lang.Thread.sleep(jlong, jint)</code></b>
 *
 * Ignore the @b sleeptime_nanoseconds parameter in this implementation.
 *
 */
jvoid jlThread_sleep_nanos(jvm_class_index clsidxthis,
                           jlong           sleeptime_milliseconds,
                           jint            sleeptime_nanoseconds)
{
    ARCH_FUNCTION_NAME(jlThread_sleep_nanos);

    /* Do nothing with @b sleeptime_nanoseconds */

    jlThread_sleep(clsidxthis, sleeptime_milliseconds);

} /* END of jlThread_sleep_nanos() */

/*@} */ /* End of grouped definitions */


/*!
 * @name Native implementation of java.lang.Thread.join() functions.
 *
 * @brief Join one thread onto another, timed and untimed.
 *
 * Results are undefined if thread has the @b JOIN4EVER, @b JOINTIMED,
 * @b WAIT4EVER, @b WAITTIMED, or @b INTERRUPTIBLEIO status or if thread
 * has been @b NOTIFIED or @b INTERRUPTED.
 *
 * This will only succeed if thread is in @b RUNNING state.
 *
 * The <b><code>join(ms, ns)</code></b> version ignores the
 * nanoseconds parameter and works just like @c @b join(ms).
 *
 * The object hash of @c @b this object is always passed
 * as the first parameter.
 *
 *
 * @param  objhashthis             Object table hash of
 *                                 @c @b this object.
 *
 * @param  sleeptime               Number of timer ticks (milliseconds)
 *                                 to sleep.
 *
 * @param  sleeptime_nanoseconds   Number of nanoseconds to wait on join
 *                                 in addition to the milliseconds.
 *
 *
 * @returns @link #jvoid jvoid@endlink
 *
 *
 * @throws JVMCLASS_JAVA_LANG_INTERRUPTEDEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_INTERRUPTEDEXCEPTION
           if another thread had interrupted this thread@endlink.
 *
 *
 * @todo HARMONY-6-jvm-jlThread.c-4 Make sure thread interruption
 *       logic below here is working.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Native implementation of @c @b java.lang.Thread.join()
 *
 */

jvoid jlThread_join4ever(jvm_object_hash objhashthis)
{
    ARCH_FUNCTION_NAME(jlThread_join4ever);

    /* Current thread always assumed valid */
    jvm_thread_index thridxthis = CURRENT_THREAD;

    jvm_thread_index thridxjoin =
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;

    THREAD(thridxthis).status |= THREAD_STATUS_JOIN4EVER;
    THREAD(thridxthis).jointarget = thridxjoin;
    (rvoid) threadstate_request_runnable(thridxthis);

    return;

} /* END of jlThread_join4ever() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.join(jlong)
 *
 */
jvoid jlThread_jointimed(jvm_object_hash objhashthis,
                         jlong           sleeptime)
{
    ARCH_FUNCTION_NAME(jlThread_jointimed);

    /* Current thread always assumed valid */
    jvm_thread_index thridxthis = CURRENT_THREAD;

    jvm_thread_index thridxjoin =
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;

    THREAD(thridxthis).status |= THREAD_STATUS_JOINTIMED;
    THREAD(thridxthis).jointarget = thridxjoin;
    THREAD(thridxthis).sleeptime = sleeptime;
    (rvoid) threadstate_request_runnable(thridxthis);

    return;

} /* END of jlThread_jointimed() */


/*!
 * @brief Native implementation
 * of <b><code>java.lang.Thread.join(jlong, jint)</code></b>
 *
 * Ignore the @b sleeptime_nanoseconds parameter in this implementation.
 *
 */
jvoid jlThread_jointimed_nanos(jvm_object_hash objhashthis,
                               jlong           sleeptime,
                               jint            sleeptime_nanoseconds)
{
    ARCH_FUNCTION_NAME(jlThread_jointimed_nanos);

    /* Do nothing with @b sleeptime_nanoseconds */

    jlThread_jointimed(objhashthis, sleeptime);

    return;

} /* END of jlThread_jointimed_nanos() */

/*@} */ /* End of grouped definitions */


/*!
 * @name Native implementation of class static functions.
 *
 * The class index of the current class is always passed
 * as the first parameter.
 *
 * @note These @c @b java.lang.Thread methods are unusual in that
 *       they does not require a @c @b jobject (in parlance of this
 *       implementation, a
 *       @link #jvm_object_hash jvm_object_hash@endlink)
 *       to run because they are declared as @c @b static methods.  As
 *       implemented here, the usual @b objhashthis parameter is
 *       therefore replaced by @b clsidxthis.  The thread context is
 *       located in @link #CURRENT_THREAD CURRENT_THREAD@endlink.
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.currentThread()
 *
 *
 * @param  clsidxthis  Class table index of the class of
 *                     @c @b this object, namely,
 *                     @c @b java.lang.Thread .
 *
 *
 * @returns @c @b java.lang.Thread
 *          of @link rjvm#current_thread pjvm->current_thread@endlink,
 *          also known as @link #CURRENT_THREAD CURRENT_THREAD@endlink
 *
 */

jvm_object_hash jlThread_currentThread(jvm_class_index clsidxthis)
{
    ARCH_FUNCTION_NAME(jlThread_currentThread);

    /* Current thread always assumed valid */
    return(THREAD(CURRENT_THREAD).thread_objhash);

} /* END of jlThread_currentThread() */


/*!
 * @brief Native implementation of @c @b java.lang.Thread.yield()
 *
 *
 * @param  clsidxthis  Class table index of the class of
 *                     @c @b this object, namely,
 *                     @c @b java.lang.Thread .
 *
 *
 * @returns @link #jtrue jtrue@endlink if thread could be modified,
 *          else @link #jfalse jfalse@endlink.
 *
 */
jboolean jlThread_yield(jvm_class_index clsidxthis)
{
    ARCH_FUNCTION_NAME(jlThread_yield);

    /* Current thread always assumed valid */
    jvm_thread_index thridx = CURRENT_THREAD;

    jboolean rc = threadstate_request_runnable(thridx);

    if (jfalse == rc)
    {
        threadstate_request_badlogic(thridx);
    }

    return(rc);

} /* END of jlThread_yield() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.interrupted()
 *
 * Status is @b CLEARED by this method after testing it.
 *
 * @note <b>This is a static method and has no need of a
 *       @c @b this object hash.  Therefore, the first
 *       parameter is @e not an object hash, but the first
 *       application parameter itself.</b>
 *
 *
 * @param  clsidxthis  Class table index of the class of
 *                     @c @b this object, namely,
 *                     @c @b java.lang.Thread .
 *
 *
 * @returns @link #jtrue jtrue@endlink if thread has been interrupted,
 *          else @link #jfalse jfalse@endlink.
 *
 */

jboolean jlThread_interrupted(jvm_class_index clsidxthis)
{
    ARCH_FUNCTION_NAME(jlThread_interrupted);

    /* Current thread always assumed valid */
    jvm_thread_index thridx = CURRENT_THREAD;

    /* Retrieve status */
    jboolean rc = (THREAD_STATUS_INTERRUPTED &
                   THREAD(thridx).status) ? jtrue : jfalse;

    /* Clear status */
    THREAD(thridx).status &= ~THREAD_STATUS_INTERRUPTED;

    /* Report result */
    return(rc);

} /* END of jlThread_interrupted() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.holdsLock()
 *
 *
 * @param  clsidxthis  Class table index of the class of
 *                     @c @b this object, namely,
 *                     @c @b java.lang.Thread .
 *
 * @param  objhashLOCK Object hash of object to query.
 *
 *
 * @returns @link #jtrue jtrue@endlink if this thread holds the
 *          object's monitor lock, else @link #jfalse jfalse@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_NULLPOINTEREXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_NULLPOINTEREXCEPTION
           if the object hash is a null object@endlink.
 *
 */

jboolean jlThread_holdsLock(jvm_class_index clsidxthis,
                            jvm_object_hash objhashLOCK)
{
    ARCH_FUNCTION_NAME(jlThread_holdsLock);

    if (jvm_object_hash_null == objhashLOCK)
    {
        /*
         * The @objhashLOCK is a
         * @link #jvm_object_hash_null jvm_object_hash_null@endlink
         * object
         */

        /* Current thread always assumed valid */
        thread_throw_exception(CURRENT_THREAD,
                               THREAD_STATUS_THREW_EXCEPTION,
                               JVMCLASS_JAVA_LANG_NULLPOINTEREXCEPTION);
/*NOTREACHED*/
    }

    /* Current thread always assumed valid */
    if (rtrue == threadutil_holds_lock(CURRENT_THREAD, objhashLOCK))
    { 
        return(jtrue);
    }

    return(jfalse);

} /* END of jlThread_holdsLock() */


/*@} */ /* End of grouped definitions */

/*!
 * @name Native implementation of object instance functions.
 *
 * The object hash of @c @b this object is always passed
 * as the first parameter.
 *
 */


/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.interrupt()
 *
 * The
 * @link #THREAD_STATUS_INTERRUPTED THREAD_STATUS_INTERRUPTED@endlink
 * bit is unconditionally set here.  The logic for clearing the bit
 * and throwing exceptions is performed when this bit is read by other
 * functions.
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns @link #jtrue jtrue@endlink if thread could be modified, else
 *          throw @b SecurityException.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_SECURITYEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_SECURITYEXCEPTION
           if thread cannot be interrupted@endlink.
 *
 */

jboolean jlThread_interrupt(jvm_object_hash objhashthis)
{
    ARCH_FUNCTION_NAME(jlThread_interrupt);

    if ((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
        (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        jvm_thread_index thridx =
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;

        THREAD(thridx).status |= THREAD_STATUS_INTERRUPTED;
        return(jtrue);
    }

    /* Could not interrupt this thread */
    /* Current thread always assumed valid */
    thread_throw_exception(CURRENT_THREAD,
                           THREAD_STATUS_THREW_EXCEPTION,
                           JVMCLASS_JAVA_LANG_SECURITYEXCEPTION);
/*NOTREACHED*/
    return(jfalse); /* Satisfy compiler */

} /* END of jlThread_interrupt() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.isInterrupted()
 *
 * Status is UNCHANGED by this method after testing it.
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns @link #jtrue jtrue@endlink if thread has been interrupted,
 *          else @link #jfalse jfalse@endlink.
 *
 */

jboolean jlThread_isInterrupted(jvm_object_hash objhashthis)
{
    ARCH_FUNCTION_NAME(jlThread_isInterrupted);

    if ((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
        (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        jvm_thread_index thridx =
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;


        /* Retrieve status */
        jboolean rc = (THREAD_STATUS_INTERRUPTED &
                   THREAD(thridx).status) ? jtrue : jfalse;

        /* Report result */
        return(rc);
    }

    return(jfalse);

} /* END of jlThread_isInterrupted() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.isAlive()
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns @link #jtrue jtrue@endlink if thread is in use and
 *          not @b NEW and neither @b COMPLETE (transient) nor @b DEAD,
 *          else @link #jfalse jfalse@endlink.
 *
 * @todo  HARMONY-6-jvm-jlThread.c-5 CAVEAT:  Should this thread
 *        eventually get reallocated as
 *        @link #rjvm.thread_new_last pjvm->thread_new_last@endlink
 *        wraps around after @link #JVMCFG_MAX_THREADS
          JVMCFG_MAX_THREADS@endlink more new threads, this function
 *        will return a stale result at the real machine level.  This
 *        is unlikely, however, because the allocation of
 *        @c @b java.lang.Thread objects will likely cover
 *        this concern at a higher level in the design.
 */

jboolean jlThread_isAlive(jvm_object_hash objhashthis)
{
    ARCH_FUNCTION_NAME(jlThread_isAlive);

    if ((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
        (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        jvm_thread_index thridx =
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;

        switch (THREAD(thridx).this_state)
        {
            case THREAD_STATE_NEW:
            case THREAD_STATE_COMPLETE:
            case THREAD_STATE_DEAD:
                return(jfalse);
            default:
                return(jtrue);
        }

    }

    return(jfalse);

} /* END of jlThread_isAlive() */


/*!
 * @brief Native implementation of @c @b java.lang.Thread.start()
 *
 * This will only succeed if thread is in @b NEW state.
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns @link #jtrue jtrue@endlink if thread could be started,
 *          else @link #jfalse jfalse@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_INTERRUPTEDEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_INTERRUPTEDEXCEPTION
           if another thread had interrupted this thread@endlink.
 *
 */

jboolean jlThread_start(jvm_object_hash objhashthis)
{
    ARCH_FUNCTION_NAME(jlThread_start);

    if ((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
        (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        jvm_thread_index thridx = 
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;

        return(threadstate_request_start(thridx));
    }

    return(jfalse);

} /* END of jlThread_start() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.countStackFrames() .
 *
 *
 * @deprecated <b>CAVEAT EMPTOR:</b>  This method has been deprecated
 *                                    in the JDK library API
 *                                    documentation.
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns number of frames
 *
 *
 * @throws JVMCLASS_JAVA_LANG_ILLEGALTHREADSTATEEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_ILLEGALTHREADSTATEEXCEPTION
           if another thread had interrupted this thread@endlink.
 *
 */

jint jlThread_countStackFrames(jvm_object_hash objhashthis)
{
    ARCH_FUNCTION_NAME(jlThread_countStackFrames);

    jint rc = 0;

    if ((rfalse == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) ||
        (rfalse == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        return(rc);
    }

    jvm_thread_index thridx = 
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;

    if (!(THREAD_STATUS_INTERRUPTED & THREAD(thridx).status))
    {
        /* This thread is not suspended at this time */
        thread_throw_exception(thridx,
                               THREAD_STATUS_THREW_EXCEPTION,
                        JVMCLASS_JAVA_LANG_ILLEGALTHREADSTATEEXCEPTION);
/*NOTREACHED*/
    }

    jvm_sp fptest = FIRST_STACK_FRAME(thridx);

    /* Examine stack frame until end of stack,where last FP points*/
    while (!(CHECK_FINAL_STACK_FRAME_GENERIC(thridx, fptest)))
    {
        fptest = NEXT_STACK_FRAME_GENERIC(thridx, fptest);

        rc++;
    }

    return(rc);

} /* END of jlThread_countStackFrames() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.setPriority()
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 * @param  priority new priority value
 *
 *
 * @returns If this thread is in use, result is
 *          @link #jtrue jtrue@endlink,
 *          else @link #jfalse jfalse@endlink.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_ILLEGALARGUMENTEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_ILLEGALARGUMENTEXCEPTION
           if the requested thread priorty is out of range@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_SECURITYEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_SECURITYEXCEPTION
           if this thread cannot have its priority modified@endlink.
 *
 * @todo HARMONY-6-jvm-jlThread.c-6 Add logic to detect
 *       @b SecurityException.
 *
 */

jboolean jlThread_setPriority(jvm_object_hash objhashthis,
                              jint             priority)
{
    ARCH_FUNCTION_NAME(jlThread_setPriority);

    if ((THREAD_PRIORITY_MIN > priority) ||
        (THREAD_PRIORITY_MAX < priority))
    {
        /* The priority is out of range */

        /* Current thread always assumed valid */
        thread_throw_exception(CURRENT_THREAD,
                               THREAD_STATUS_THREW_EXCEPTION,
                           JVMCLASS_JAVA_LANG_ILLEGALARGUMENTEXCEPTION);
/*NOTREACHED*/
    }

    if ((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
        (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        jvm_thread_index thridx =
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;

        THREAD(thridx).priority = priority;
        return(jtrue);
    }

    /* Need to detect @b SecurityException */
#if 1
    return(jfalse);
#else
    /* This thread cannot have its priority modified */
    /* Current thread always assumed valid */
    thread_throw_exception(CURRENT_THREAD,
                           THREAD_STATUS_THREW_EXCEPTION,
                           JVMCLASS_JAVA_LANG_SECURITYEXCEPTION);
/*NOTREACHED*/
    return(jfalse); /* Satisfy compiler */
#endif

} /* END of jlThread_setPriority() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.getPriority()
 *
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns Execution priority of this thread.  If not in use, result is
 *          @link #THREAD_PRIORITY_BAD THREAD_PRIORITY_BAD@endlink.
 *
 */

jint jlThread_getPriority(jvm_object_hash objhashthis)
{
    ARCH_FUNCTION_NAME(jlThread_getPriority);

    if ((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
        (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        jvm_thread_index thridx =
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;

        return(THREAD(thridx).priority);
    }

    /* Invalid value for invalid thread */
    return(THREAD_PRIORITY_BAD);

} /* END of jlThread_getPriority() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.destroy()
 *
 * Simply kill the thread without @e any cleanup.
 * <b>THIS IS A VERY BAD THING!</b>  (Perhaps this
 * is why most JDK's do not implement this method
 * any more!)
 *
 * There is typically no implementation done of
 * @c @b java.lang.Thread.destroy(Runnable) ,
 * but will initially be done here.
 *
 * @todo  HARMONY-6-jvm-jlThread.c-7 Should this be implemented?
 *        Some JDK's probably don't implement it any more.
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns @link #jtrue jtrue@endlink if thread was moved to
 *          @b COMPLETE state, else @link #jfalse jfalse@endlink.
 *
 */

jboolean jlThread_destroy(jvm_object_hash objhashthis)
{
    ARCH_FUNCTION_NAME(jlThread_destroy);

    if ((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
        (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        jvm_thread_index thridx =
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;

        /* GAG!  This will @e really break the state machine! */
        /* THREAD(thridxcurr).status &= ~THREAD_STATUS_INUSE; */

        /* So try to kill it quietly: */
        threadstate_request_badlogic(thridx);
        threadstate_activate_badlogic(thridx);
        threadstate_activate_badlogic(thridx);
        return(threadstate_request_complete(thridx));
    }

    return(jfalse);

} /* END of jlThread_destroy() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.checkAccess()
 *
 * This method will @e always give permission in this JVM.
 *
 * @todo  HARMONY-6-jvm-jlThread.c-8 A smart java.lang.SecurityManager
 *        will take care of this matter.
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns @link #jtrue jtrue@endlink unconditionally.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_SECURITYEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_SECURITYEXCEPTION
       if current thread is not permitted to modify this thread@endlink.
 *
 * @todo HARMONY-6-jvm-jlThread.c-9 Add logic to detect
 *       @b SecurityException beyond passing in
 *       an invalid @b objhashthis.
 *
 */
jboolean jlThread_checkAccess(jvm_object_hash objhashthis)
{
    ARCH_FUNCTION_NAME(jlThread_checkAccess);

    if ((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
        (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
/* unused
        jvm_thread_index thridx =
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;
*/
        return(jtrue);
    }

    /* Could not modify this thread */
    thread_throw_exception(CURRENT_THREAD,
                           THREAD_STATUS_THREW_EXCEPTION,
                           JVMCLASS_JAVA_LANG_SECURITYEXCEPTION);
/*NOTREACHED*/
    return(jfalse); /* Satisfy compiler */

} /* END of jlThread_checkAccess() */


/*!
 * @brief Native implementation
 * of <code>java.lang.Thread.setDaemon()<code>
 *
 * @todo  HARMONY-6-jvm-jlThread.c-10 See notes elsewhere about
 *        implementation of the ISDAEMON bit.  This concept must
 *        be implemented in the JVM structures so as to know
 *        when to quit (no non-daemon threads running, that is,
 *        no user threads running).  Currently, it is a status bit in
 *        the @link rthread#status rthread.status@endlink structure
 *        named
 *        @link #THREAD_STATUS_ISDAEMON THREAD_STATUS_ISDAEMON@endlink
 *        but is typically @e also found as a private class member
 *        of @c @b java.lang.Thread .  If this were @e always
 *        true, then the former could be eliminated.  Since this code
 *        actually @e implements this class' native methods, either one
 *        could be eliminated @e if none of the other (non-native) class
 *        methods referenced the private variable without going through
 *        @link #jlThread_isDaemon jlThread_isDaemon@endlink.  However,
 *        this question is why this action item is present.
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 * @param  isdaemon     @link #rtrue rtrue@endlink or
 *                      @link #rfalse rfalse@endlink,
 *                      depending on requested condition
 *
 *
 * @returns @link #jtrue jtrue@endlink if could make the change,
 *          else throw @b SecurityException.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_ILLEGALTHREADSTATEEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_ILLEGALTHREADSTATEEXCEPTION
           if thread is not in the @b NEW state when attempting
           to set this condition@endlink.
 *
 * @throws JVMCLASS_JAVA_LANG_SECURITYEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_SECURITYEXCEPTION
           if current thread cannot change this thread@endlink.
 *
 *
 * @todo HARMONY-6-jvm-jlThread.c-11 Review jvm_init() code for
 *       setting up threads before there is a @c @b setjmp(3) handler
 *       for @c @b setDaemon() exceptions.
 *
 * @todo HARMONY-6-jvm-jlThread.c-12 Add logic to detect
 *       @b SecurityException beyond passing in an invalid
 *       @b objhashthis.
 *
 */

jvoid jlThread_setDaemon(jvm_object_hash objhashthis,
                        jboolean        isdaemon)
{
    ARCH_FUNCTION_NAME(jlThread_setDaemon);

    if ((rfalse == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) ||
        (rfalse == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        /* The requested thread is not valid */
        /* Current thread always assumed valid */
        thread_throw_exception(CURRENT_THREAD,
                               THREAD_STATUS_THREW_EXCEPTION,
                               JVMCLASS_JAVA_LANG_SECURITYEXCEPTION);
/*NOTREACHED*/
    }

    jvm_thread_index thridx = 
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;

    if (THREAD_STATE_NEW == THREAD(thridx).this_state)
    {
        if (jtrue == isdaemon)
        {
            THREAD(thridx).status |= THREAD_STATUS_ISDAEMON;
        }
        else
        {
            THREAD(thridx).status &= ~THREAD_STATUS_ISDAEMON;
        }

        return;
    }

    /* This thread is in some state besides @b NEW */
    thread_throw_exception(thridx,
                           THREAD_STATUS_THREW_EXCEPTION,
                        JVMCLASS_JAVA_LANG_ILLEGALTHREADSTATEEXCEPTION);
/*NOTREACHED*/
    return; /* Satisfy compiler */

} /* END of jlThread_setDaemon() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.isDaemon()
 *
 * @todo  HARMONY-6-jvm-jlThread.c-13 See notes elsewhere about
 *        implementation of the @b ISDAEMON bit.
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns @link #jtrue jtrue@endlink or @link #jfalse jfalse@endlink,
 *          depending on value of @b ISDAEMON bit.
 *
 */

jboolean jlThread_isDaemon(jvm_object_hash objhashthis)
{
    ARCH_FUNCTION_NAME(jlThread_isDaemon);

    if ((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
        (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        jvm_thread_index thridx =
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;

        return((THREAD_STATUS_ISDAEMON & THREAD(thridx).status)
               ? jtrue
               : jfalse);
    }

    return(jfalse);

} /* END of jlThread_isDaemon() */


#if 0
/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.setName()
 *
 * @todo  HARMONY-6-jvm-jlThread.c-14 Needs work to convert
 *        java.lang.String into (rthread).name (written
 *        @e long before @b String code).
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *         name         Null-terminated string containing new
 *                      thread name
 *
 *
 * @returns @link #jvoid jvoid@endlink
 *
 */
jvoid jlThread_setName(jvm_object_hash  objhashthis,
                       rchar           *newname)
{
    ARCH_FUNCTION_NAME(jlThread_setName);

    if ((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
        (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        jvm_thread_index thridx =
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;


        SomeRenditionOf(THREAD(thridx).name) = SomeRenditionOf(newname);
    }

    return;

} /* END of jlThread_setName() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.getName()
 *
 * @todo  HARMONY-6-jvm-jlThread.c-15 Needs work to convert
 *        java.lang.String into (rthread).name (written
 *        @e long before @b String code).
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns Object hash to @c @b String containing thread name
 *
 */
jvm_object_hash jlThread_getName(jvm_object_hash objhashthis)
{
    ARCH_FUNCTION_NAME(jlThread_getName);

    if ((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
        (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        jvm_thread_index thridx =
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;


        return(SomeRenditionOf(THREAD(thridx).name));
    }

    ... now what?

} /* END of jlThread_setName() */
#endif


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.stop(jvoid)
 *
 * There is typically no native implementation of
 * @c @b java.lang.Thread.stop(Runnable) .
 *
 *
 * @deprecated <b>CAVEAT EMPTOR:</b>  This method has been deprecated
 *                                    in the JDK library API
 *                                    documentation.
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns @link #jtrue jtrue@endlink if thread could be modified,
 *          else throw @b SecurityException
 *
 *
 * @throws JVMCLASS_JAVA_LANG_SECURITYEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_SECURITYEXCEPTION
           if current thread cannot change this thread@endlink.
 *
 * @todo HARMONY-6-jvm-jlThread.c-16 Add logic to detect
 *       @b SecurityException beyond passing in
 *       an invalid @b objhashthis.
 *
 */

jvoid jlThread_stop(jvm_object_hash objhashthis)
{
    ARCH_FUNCTION_NAME(jlThread_stop);

    if ((rfalse == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) ||
        (rfalse == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        /* This thread cannot change the requested thread */
        /* Current thread always assumed valid */
        thread_throw_exception(CURRENT_THREAD,
                               THREAD_STATUS_THREW_EXCEPTION,
                               JVMCLASS_JAVA_LANG_SECURITYEXCEPTION);
/*NOTREACHED*/
    }

    jvm_thread_index thridx =
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;

    jvm_object_hash objhash;

    /* Remove all monitor locks */
    for (objhash = JVMCFG_FIRST_OBJECT;
         objhash < JVMCFG_MAX_OBJECTS;
         objhash++)
    {
        /* Check object in use and locked, and do BIDIRECTIONAL test
         * of this thread knowing about this object lock / vice versa */
        if ((OBJECT_STATUS_INUSE & OBJECT(objhash).status)   &&
            (OBJECT_STATUS_MLOCK & OBJECT(objhash).status)   &&
            (thridx         == OBJECT(objhash).mlock_thridx) &&
            (objhash        == THREAD(thridx).locktarget))
        {
            OBJECT(objhash).status       &= ~OBJECT_STATUS_MLOCK;
            OBJECT(objhash).mlock_count   = 0;
            OBJECT(objhash).mlock_thridx  = jvm_thread_index_null;
        }
    }

    /* GAG!  This will @e really break the state machine! */
    /* THREAD(thridx).status &= ~THREAD_STATUS_INUSE; */

    /* So try to kill it quietly: */
    threadstate_request_badlogic(thridx);
    threadstate_activate_badlogic(thridx);
    (rvoid) threadstate_request_complete(thridx);

    return;

} /* END of jlThread_stop() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.suspend()
 *
 * Results are undefined if thread has the @b SLEEP, @b WAIT4EVER,
 * @b WAITTIMED, or @b INTERRUPTIBLEIO status or if thread has
 * been @b NOTIFIED or @b INTERRUPTED.
 *
 * This will work if thread is in @e any state at all.
 *
 * @deprecated <b>CAVEAT EMPTOR:</b>  This method has been deprecated
 *                                    in the JDK library API
 *                                    documentation.
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns @link #jtrue jtrue@endlink if thread could be modified,
 *          else throw @b SecurityException.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_SECURITYEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_SECURITYEXCEPTION
           if could not suspend thread@endlink.
 *
 * @todo HARMONY-6-jvm-jlThread.c-17 Add logic to
 *       detect @b SecurityException beyond passing in
 *       an invalid @b objhashthis.
 *
 */

jvoid jlThread_suspend(jvm_object_hash objhashthis)
{
    ARCH_FUNCTION_NAME(jlThread_suspend);

    if ((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
        (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        jvm_thread_index thridx =
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;

        THREAD(thridx).status |= THREAD_STATUS_SUSPEND;

        /*
         * Move through BADLOGIC state and into BLOCKINGEVENT,
         * which will put it in line to be BLOCKED.
         */
        threadstate_request_badlogic(thridx);
        threadstate_activate_badlogic(thridx);
        (rvoid) threadstate_request_blockingevent(thridx);

        return;
    }

    /* Could not suspend this thread */
    thread_throw_exception(CURRENT_THREAD,
                           THREAD_STATUS_THREW_EXCEPTION,
                           JVMCLASS_JAVA_LANG_SECURITYEXCEPTION);
/*NOTREACHED*/
    return; /* Satisfy compiler */

} /* END of jlThread_suspend() */


/*!
 * @brief Native implementation
 * of @c @b java.lang.Thread.resume()
 *
 * Results are undefined if thread has the @b SLEEP, @b WAIT4EVER,
 * @b WAITTIMED, or @b INTERRUPTIBLEIO status or if thread has
 * been @b NOTIFIED or @b INTERRUPTED.
 *
 * This will work if thread is in @e any state at all.
 *
 *
 * @deprecated <b>CAVEAT EMPTOR:</b>  This method has been deprecated
 *                                    in the JDK library API
 *                                    documentation.
 *
 *
 * @param  objhashthis  Object table hash of @c @b this object.
 *
 *
 * @returns @link #jtrue jtrue@endlink if thread could be modified,
 *          else throw @b SecurityException.
 *
 *
 * @throws JVMCLASS_JAVA_LANG_SECURITYEXCEPTION
 *         @link #JVMCLASS_JAVA_LANG_SECURITYEXCEPTION
           if could not suspend thread@endlink.
 *
 * @todo HARMONY-6-jvm-jlThread.c-18 Add logic to
 *       detect @b SecurityException beyond passing in
 *       an invalid @b objhashthis.
 *
 */

jvoid jlThread_resume(jvm_object_hash objhashthis)
{
    ARCH_FUNCTION_NAME(jlThread_resume);

    if ((rtrue == VERIFY_OBJECT_THREAD_LINKAGE(objhashthis)) &&
        (rtrue == VERIFY_THREAD_LINKAGE(
                      OBJECT_THREAD_LINKAGE(objhashthis)->thridx)))
    {
        jvm_thread_index thridx =
                             OBJECT_THREAD_LINKAGE(objhashthis)->thridx;

        if (THREAD_STATUS_SUSPEND & THREAD(thridx).status)
        {
            /*
             * Move back out into @b UNBLOCKED state.  Don't care how
             * far into process the @c @b Thread.suspend() went,
             * since this implementation, using jlThread_suspend() only
             * puts in the first request (for @b BLOCKINGEVENT).
             * Wherever the state machine is in its paces, the thread
             * will be moved forward to requesting @b UNBLOCKED.
             */
            switch (THREAD(thridx).this_state)
            {
                case THREAD_STATE_BADLOGIC:
                    (rvoid) threadstate_request_blockingevent(thridx);
                    (rvoid) threadstate_activate_blockingevent(thridx);
                    (rvoid) threadstate_process_blockingevent(thridx);
                    /* ... continue with next 'case' */

                case THREAD_STATE_BLOCKINGEVENT:
                    (rvoid) threadstate_request_blocked(thridx);
                    (rvoid) threadstate_activate_blocked(thridx);
                    (rvoid) threadstate_process_blocked(thridx);
                    /* ... continue with next 'case' */

                case THREAD_STATE_BLOCKED:
                    (rvoid) threadstate_request_unblocked(thridx);

                    return;

                /* Anything else is invalid */
                default:
                    break; /* Continue w/ thread_throw_exception()... */
            }
        }
    }

    /* Could not resume this thread */
    thread_throw_exception(CURRENT_THREAD,
                           THREAD_STATUS_THREW_EXCEPTION,
                           JVMCLASS_JAVA_LANG_SECURITYEXCEPTION);
/*NOTREACHED*/
    return; /* Satisfy compiler */

} /* END of jlThread_resume() */


/*@} */ /* End of grouped definitions */

/* EOF */
