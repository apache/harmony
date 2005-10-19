/*!
 * @file threadutil.c
 *
 * @brief Utilities for operating the JVM thread state model on this
 * real machine implementation.
 *
 * @todo HARMONY-6-jvm-threadutil.c-1 Timers for Thread.sleep() and Thread.wait() and Object.wait()
 *       that use millisecond timers @e are supported.  The variation
 *       that supports higher resolution of milliseconds and nanoseconds
 *       are @e not supported, but the millisecond version is used
 *       instead.
 *
 * @internal This file also serves the dual purpose as a catch-all for
 *           development experiments.  Due to the fact that the
 *           implementation of the Java thread and the supporting
 *           rthread structure is deeply embedded in the core of the
 *           development of this software, this file has contents that
 *           come and go during development.  Some functions get staged
 *           here before deciding where they @e really go; some are
 *           interim functions for debugging, some were glue that
 *           eventually went away.  Be careful to remove prototypes
 *           to such functions from the appropriate header file.
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

#include "arch.h"
ARCH_SOURCE_COPYRIGHT_APACHE(threadutil, c,
"$URL$",
"$Id$");


#include "jvmcfg.h"
#include "cfmacros.h"
#include "classfile.h"
#include "jvm.h"
#include "jvmclass.h"
#include "util.h"


/*!
 * @brief Update the interval timer for this thread from
 * @c @b java.lang.Thread.sleep() or from a timed
 * @c @b java.lang.Thread.wait() or @c @b java.lang.Thread.join().
 *
 * This function is designed to be invoked from the timeslice interrupt
 * handler and @e only from thence.  It DOES NOT handle
 * (millisec, nanosec) resolution, only millisecond resolution.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */

rvoid threadutil_update_sleeptime_interval(rvoid)
{
    ARCH_FUNCTION_NAME(threadutil_update_sleeptime_interval);

    jvm_thread_index thridx;

    /* Lock out the @e world during timer update */
    pthread_mutex_lock(&pjvm->sleeplock);

    for (thridx = jvm_thread_index_null;
         thridx < JVMCFG_MAX_THREADS;
         thridx++)
    {
        if ((THREAD_STATUS_INUSE    & THREAD(thridx).status)    &&
            ((THREAD_STATUS_SLEEP     |
              THREAD_STATUS_JOINTIMED |
              THREAD_STATUS_WAITTIMED  ) & THREAD(thridx).status))
        {
            /*
             * Perform next interval update.  Stop decrementing
             * when time reaches zero.
             */
            if (0 != THREAD(thridx).sleeptime)
            {
                THREAD(thridx).sleeptime--;
            }
        }
    }

    /* Unlock the @e world after timer update */
    pthread_mutex_unlock(&pjvm->sleeplock);

    return;

} /* END of threadutil_update_sleeptime_interval() */


/*!
 * @brief Complete the UNtimed Thread.join() request and allow threads
 * that have joined this one to resume execution.
 *
 * This function is typically called when a thread enters the
 * @b COMPLETE state after finishing its JVM execution.
 *
 * Review state of thread table, looking for the following conditions.
 * For those that meet them, move thread out of given state and
 * forward to next state.  Three functions are used, depending on
 * the current state, threadutil_update_blockingevent() and
 * threadutil_update_wait() and threadutil_update_lock():
 *
 * @verbatim

   Condition:         Current state: Next state:  threadutil_update_YYY:
   ----------         -------------- -----------  ----------------------
  
   Thread.join()      COMPLETE       N/C              _blockingevents()
   (forever, where
   current thread
   is COMPLETE, and
   target thread is
   BLOCKED, and is
   moved to UNBLOCKED)
  
   Thread.join(n)     COMPLETE       N/C              _blockingevents()
   (timed, where
   n has expired
   on current
   thread or it is
   COMPLETE, and
   target thread
   is BLOCKED, and
   is moved to
   UNBLOCKED)
  
   Thread.sleep(n)    BLOCKED        UNBLOCKED        _blockingevents()
   (n has expired on
   current thread)
  
   Interruptible I/O  BLOCKED        UNBLOCKED        _blockingevents()
   from class
   java.nio.channels
   .InterruptibleChannel
  
   Object.wait()      WAIT           NOTIFY           _wait()
   (forever on
   current thread,
   where target object
   lock was released)
  
   Object.wait(n)     WAIT           NOTIFY           _wait()
   (timed, where
   @c @b n
   has expired on
   current thread
   or target object
   lock was released)
  
   One of:            LOCK           ACQUIRE          _lock()
   Object.notify()
   Object.notifyAll()
   Thread.interrupt()
   synchronized(Object)
   ... put thread
   into LOCK state.
   Now it will
   negotiate to
   ACQUIRE its
   object's
   monitor lock.
   
  
   Thread.suspend()   ANY            BLOCKED        threadutil_suspend()
  
   Thread.resume()    BLOCKED        UNBLOCKED       threadutil_resume()
   moves a
   Thread.suspend()
   thread forward
   to UNBLOCKED
  
   @endverbatim
 *
 * With the exception of threadutil_suspend() and threadutil_resume(),
 * these functions is designed to be invoked from the JVM outer loop.
 * CAVEAT EMPTOR:  Those two functions are deprecated.  Use
 * at your own risk!
 *
 * @todo HARMONY-6-jvm-threadutil.c-2 Interruptible from class
 *       @c @b java.nio.channels.InterruptibleChannel
 *
 *
 * @param  thridxcurr   Thread for which to examine state changes
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */

rvoid threadutil_update_blockingevents(jvm_thread_index thridxcurr)
{
    ARCH_FUNCTION_NAME(threadutil_update_blockingevents);

    /* Only examine INUSE threads in selected states */
    if (!(THREAD_STATUS_INUSE & THREAD(thridxcurr).status))
    {
        return;
    }

    switch (THREAD(thridxcurr).this_state)
    {
        case THREAD_STATE_COMPLETE:  /* Untimed/untimed Thread.join() */

        case THREAD_STATE_BLOCKED:   /* Thread.sleep() and */
                                     /*! @todo
                                      *     HARMONY-6-jvm-threadutil.c-3
                                      *        interruptible I/O req's
                                      */

        default:                     /* Not meaningful for this logic */
            return;
    }

    /* sleep() -- time period expired for only this thread */
    if ((THREAD_STATE_BLOCKED == THREAD(thridxcurr).this_state) &&
       (THREAD_STATUS_SLEEP   &  THREAD(thridxcurr).status)     &&
       (0 == timeslice_get_thread_sleeptime(thridxcurr)))
    {
        /* Mark thread to continue now after sleep() */
        THREAD(thridxcurr).status &= ~THREAD_STATUS_SLEEP;
        threadstate_request_unblocked(thridxcurr);

        /*
         * Check if Thread.interrupt() was thrown
         * against this thread.
         *
         * Also need to throw the exception in JVM outer loop.
         */
        if (THREAD_STATUS_INTERRUPTED & THREAD(thridxcurr).status)
        {
            THREAD(thridxcurr).status &= ~THREAD_STATUS_INTERRUPTED;

            THREAD(thridxcurr).status |= THREAD_STATUS_THREW_EXCEPTION;

            THREAD(thridxcurr).pThrowableEvent =
                                JVMCLASS_JAVA_LANG_INTERRUPTEDEXCEPTION;
        }
    }
    else

    /*!
     * @todo  HARMONY-6-jvm-threadutil.c-4 First pass guess at
     *        interruptible I/O
     */
    if ((THREAD_STATE_BLOCKED == THREAD(thridxcurr).this_state) &&
        (THREAD_STATUS_INTERRUPTIBLEIO & THREAD(thridxcurr).status))
    {
        /*!
         * Check if Thread.interrupt() was thrown
         * against the interruptible I/O operation on this thread.
         *
         * @todo HARMONY-6-jvm-threadutil.c-5 Is this the correct way
         *       to handle interruptible I/O events?
         *       Also need to throw the exception in JVM outer loop.
         */
        if (THREAD_STATUS_INTERRUPTED & THREAD(thridxcurr).status)
        {
            /* Mark thread to continue now after Thread.interrupt() */
            THREAD(thridxcurr).status &= ~THREAD_STATUS_INTERRUPTIBLEIO;
            threadstate_request_unblocked(thridxcurr);

            THREAD(thridxcurr).status &= ~THREAD_STATUS_INTERRUPTED;

            THREAD(thridxcurr).status |= THREAD_STATUS_THREW_EXCEPTION;

            THREAD(thridxcurr).pThrowableEvent =
                JVMCLASS_JAVA_NIO_CHANNELS_CLOSEDBYINTERRUPTEXCEPTION;
        }
    }
    else

    /*
     * Examine Thread.join() conditions, both timed and untimed
     */
    if (THREAD_STATE_COMPLETE == THREAD(thridxcurr).this_state)
    {
        jvm_thread_index thridxjoin;

                  /* Skip JVMCFG_NULL_THREAD */
        for (thridxjoin = JVMCFG_SYSTEM_THREAD;
             thridxjoin < JVMCFG_MAX_THREADS;
             thridxjoin++)
        {
            /*
             * Skip myself
             */
            if (thridxcurr == thridxjoin)
            {
                continue;
            }

            /* Only process threads in the BLOCKED state */
            if (THREAD_STATE_BLOCKED != THREAD(thridxjoin).this_state)
            {
                continue; 
            }

            /*
             * If current COMPLETE thread is the target of a join,
             * check which type it is, timed or untimed.
             */
            if (thridxcurr == THREAD(thridxjoin).jointarget)
            {
                /* UNtimed join() -- where time period is zero/forever*/
                if ((THREAD_STATUS_JOIN4EVER &
                     THREAD(thridxjoin).status))
                {
                    /* Mark joined thread to be UNBLOCKED after join()*/
                    THREAD(thridxjoin).jointarget =
                                                  jvm_thread_index_null;
                    THREAD(thridxjoin).status &= 
                                               ~THREAD_STATUS_JOIN4EVER;
                    threadstate_request_unblocked(thridxjoin);

                    /*
                     * Check if Thread.interrupted() was thrown
                     * against this thread.
                     */
                    if (THREAD_STATUS_INTERRUPTED &
                        THREAD(thridxjoin).status)
                    {
                        THREAD(thridxjoin).status &=
                                             ~THREAD_STATUS_INTERRUPTED;

                        THREAD(thridxjoin).status |=
                                          THREAD_STATUS_THREW_EXCEPTION;

                        THREAD(thridxjoin).pThrowableEvent =
                            JVMCLASS_JAVA_LANG_INTERRUPTEDEXCEPTION;
                    }

                }
                else

                /* TIMED join() --time period is NON-zero,now expired */
                if ((THREAD_STATUS_JOINTIMED &
                                     THREAD(thridxjoin).status) &&
                    (0 == timeslice_get_thread_sleeptime(thridxjoin)))
                {
                    /* Mark joined thread to be UNBLOCKED after join()*/
                    THREAD(thridxjoin).jointarget =
                                                  jvm_thread_index_null;
                    THREAD(thridxjoin).status &=
                                               ~THREAD_STATUS_JOINTIMED;
                    threadstate_request_unblocked(thridxjoin);

                    /*
                     * Check if Thread.interrupted() was thrown
                     * against this thread.
                     */
                    if (THREAD_STATUS_INTERRUPTED &
                        THREAD(thridxjoin).status)
                    {
                        THREAD(thridxjoin).status &=
                                             ~THREAD_STATUS_INTERRUPTED;

                        THREAD(thridxjoin).status |=
                            THREAD_STATUS_THREW_EXCEPTION;

                        THREAD(thridxjoin).pThrowableEvent =
                            JVMCLASS_JAVA_LANG_INTERRUPTEDEXCEPTION;
                    }
                }
            }
        }
    }

    return;

} /* END of threadutil_update_blockingevents() */


rvoid threadutil_update_wait(jvm_thread_index thridxcurr)
{
    ARCH_FUNCTION_NAME(threadutil_update_wait);

    /* Only examine INUSE threads in WAIT state */
    if (!(THREAD_STATUS_INUSE & THREAD(thridxcurr).status))
    {
        return;
    }

    switch (THREAD(thridxcurr).this_state)
    {
        case THREAD_STATE_WAIT:      /* Timed/untimed Object.wait() */
            break;

        default:                     /* Not meaningful for this logic */
            return;
    }


    /* wait() -- Check for notify() or notifyAll() or interrupt() */
    if (THREAD_STATUS_WAIT4EVER & THREAD(thridxcurr).status)
    {
        /*
         * Check if Thread.interrupt() was thrown against this thread
         * or if Object.notify() against this object by this thread.
         *
         * (Also need to throw the exception in JVM outer loop.)
         */
        if (THREAD_STATUS_INTERRUPTED & THREAD(thridxcurr).status)
        {
            THREAD(thridxcurr).status &= ~THREAD_STATUS_INTERRUPTED;

            THREAD(thridxcurr).status |= THREAD_STATUS_THREW_EXCEPTION;

            THREAD(thridxcurr).pThrowableEvent =
                                JVMCLASS_JAVA_LANG_INTERRUPTEDEXCEPTION;

            /* Remove WAIT condition and move to NOTIFY state */
            THREAD(thridxcurr).status &= ~THREAD_STATUS_WAIT4EVER;
            threadstate_request_notify(thridxcurr);
        }
        else
        if (THREAD_STATUS_NOTIFIED & THREAD(thridxcurr).status)
        {
            THREAD(thridxcurr).status &= ~THREAD_STATUS_NOTIFIED;

            /* Remove WAIT condition and move to NOTIFY state */
            THREAD(thridxcurr).status &= ~THREAD_STATUS_WAIT4EVER;
            threadstate_request_notify(thridxcurr);
        }
    }
    else

    /* wait(n) -- TIMED, chk if monitor lock released on target object
                  or if timer expired */
    if ((THREAD_STATUS_WAITTIMED &  THREAD(thridxcurr).status)   &&
        (jvm_object_hash_null    != THREAD(thridxcurr).locktarget))
    {
        /* Give up if timer expired */
        if (0 == timeslice_get_thread_sleeptime(thridxcurr))
        {
            /* Remove @b WAIT condition and move to next state */
            THREAD(thridxcurr).status &= ~THREAD_STATUS_WAITTIMED;
            threadstate_request_notify(thridxcurr);
        }
        else
        {
            /*
             * Check if Thread.interrupt() was thrown against this
             * thread or if Object.notify() against this object by this
             * thread.
             *
             * (Also need to throw the exception in JVM outer loop.)
             */
            if (THREAD_STATUS_INTERRUPTED & THREAD(thridxcurr).status)
            {
                THREAD(thridxcurr).status &= ~THREAD_STATUS_INTERRUPTED;

                THREAD(thridxcurr).status |=
                                          THREAD_STATUS_THREW_EXCEPTION;

                THREAD(thridxcurr).pThrowableEvent =
                                JVMCLASS_JAVA_LANG_INTERRUPTEDEXCEPTION;

                /* Remove WAIT condition and move to NOTIFY state */
                THREAD(thridxcurr).status &= ~THREAD_STATUS_WAITTIMED;
                threadstate_request_notify(thridxcurr);
            }
            else
            if (THREAD_STATUS_NOTIFIED & THREAD(thridxcurr).status)
            {
                THREAD(thridxcurr).status &= ~THREAD_STATUS_NOTIFIED;

                /* Remove WAIT condition and move to NOTIFY state */
                THREAD(thridxcurr).status &= ~THREAD_STATUS_WAITTIMED;
                threadstate_request_notify(thridxcurr);
            }
        }
    }

    return;

} /* END of threadutil_update_wait() */


rvoid threadutil_update_lock(jvm_thread_index thridxcurr)
{
    ARCH_FUNCTION_NAME(threadutil_update_lock);

    /* Only examine INUSE threads in LOCK state */
    if (!(THREAD_STATUS_INUSE & THREAD(thridxcurr).status))
    {
        return;
    }

    switch (THREAD(thridxcurr).this_state)
    {
        case THREAD_STATE_LOCK:      /* Lock arbitration */
            break;

        default:                     /* Not meaningful for this logic */
            return;
    }


    /*
     * One of java.lang.Object.notify() or java.lang.Object.notifyAll()
     * or java.lang.Thread.interrupt() or a @c @b synchronize()
     * block got the thread here, now try to wake up from @b LOCK with
     * the target object's monitor lock acquired, if currently
     * available.
     *
     * To do so, check if previous thread unlocked this object.  If so,
     * go lock it for use by this thread.
     */
    if ((jvm_object_hash_null != THREAD(thridxcurr).locktarget) &&
        (!(OBJECT_STATUS_MLOCK & OBJECT(THREAD(thridxcurr).locktarget)
                                   .status)))
    {
        /* If fail, arbitrate here again next time around */
        if (rtrue ==
            objectutil_synchronize(THREAD(thridxcurr).locktarget,
                                   thridxcurr))
        {
            /* Move to @b ACQUIRE state */
            THREAD(thridxcurr).status &= ~THREAD_STATUS_WAIT4EVER;
            threadstate_request_acquire(thridxcurr);
        }
    }

    return;

} /* END of threadutil_update_lock() */


/*!
 * @brief Examine an object to see if a thread owns its monitor lock
 *
 *
 * @param thridx      Thread table index of thread to compare with.
 *
 * @param objhashlock Object table hash of object to examine.
 *
 *
 * @returns @link #rtrue rtrue@endlink if this thread owns
 *          this object's monitor lock.
 *
 */

rboolean threadutil_holds_lock(jvm_thread_index thridx,
                               jvm_object_hash  objhashlock)
{
    ARCH_FUNCTION_NAME(threadutil_holds_lock);

    /*
     * Make sure thread and object are both in use,
     * make sure object is locked, then do @b BIDIRECTIONAL check
     * to see if object and thread think each other is in sync
     * and that @e this thread indeed @b DOES hold the lock on @e this
     * object.
     */

    /* Prerequisite to this check */
    if ((!(OBJECT_STATUS_INUSE & OBJECT(objhashlock).status)) ||
        (OBJECT_STATUS_NULL    & OBJECT(objhashlock).status))
    {
        return(rfalse);
    }

    /* Prerequisite to this check */
    if ((!(THREAD_STATUS_INUSE & THREAD(thridx).status)) ||
        (THREAD_STATUS_NULL    & THREAD(thridx).status))
    {
        return(rfalse);
    }

    if ((OBJECT_STATUS_MLOCK & OBJECT(objhashlock).status) &&
        (thridx             == OBJECT(objhashlock).mlock_thridx)  &&
        ((THREAD_STATUS_WAIT4EVER |
          THREAD_STATUS_WAITTIMED ) & THREAD(thridx).status) &&
        (objhashlock        == THREAD(thridx).locktarget))
    {
        return(rtrue);
    }

    return(rfalse);

} /* END of threadutil_holds_lock() */


/* EOF */
