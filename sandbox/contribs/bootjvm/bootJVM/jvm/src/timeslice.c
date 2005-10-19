/*!
 * @file timeslice.c
 *
 * @brief JVM one millisecond time slice timer.
 *
 * @attention This file contains data structures and functionality
 *            found nowhere else in the project.  It is inherently
 *            platform-specific and porting to different platforms
 *            must be done carefully so that nothing breaks on the
 *            existing implementations.
 *
 * The @link rjvm.timeslice_expired pjvm->timeslice_expired@endlink
 * flag is set by the periodic @b SIGALRM herein and tested in the JVM
 * virtual instruction inner loop to decide when a thread has finished
 * using its time slice.
 *
 * @verbatim
   function              flag value             meaning
   --------              ----------             -------
  
   timeslice_init()      set rfalse             initial value
  
   timeslice_tick()      set rtrue              time slice finished
  
   jvm_run()             rfalse                 keep running this slice
                         rtrue                  time slice finished
   @endverbatim
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
ARCH_SOURCE_COPYRIGHT_APACHE(timeslice, c,
"$URL$",
"$Id$");


#include <unistd.h>
#include <signal.h>
#if defined(CONFIG_WINDOWS) || defined(CONFIG_CYGWIN)
#include <sys/time.h>
#endif 

#define _REENTRANT
#include <pthread.h>

#ifndef CONFIG_WINDOWS
#ifndef CONFIG_CYGWIN
#include <thread.h> /* WATCH OUT!  /usr/include, not application .h */
#endif
#endif

#include "jvmcfg.h"
#include "classfile.h"
#include "jvm.h"
#include "exit.h" 
#include "util.h"


/*!
 * @brief Thread control structure for use by
 * @c @b pthread_create(3), etc.
 */
static pthread_t posix_thread_id;

/*!
 * @brief Start the time slicing mechanism at JVM init time
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid timeslice_init()
{
    ARCH_FUNCTION_NAME(timeslice_init);

    /* Time slice has not expired */
    pjvm->timeslice_expired = rfalse;

    /*
     * Initialize the @link rthread#sleeptim rthread.sleeptime@endlink
     * lock w/ defult attributes
     */
    pthread_mutex_init(&pjvm->sleeplock, (void *) rnull);

    int rc = pthread_create(&posix_thread_id,
                            (void *) rnull,
                            timeslice_run,
                            (void *) rnull);

    if (0 != rc)
    {
        sysErrMsg(arch_function_name, "Cannot start timer");
        exit_jvm(EXIT_TIMESLICE_START);
/*NOTREACHED*/
    }

    /* Declare this module initialized */
    jvm_timeslice_initialized = rtrue;

    return;

} /* END of timeslice_init() */


/*!
 * @brief Retrieve a thread's @link rthread#sleeptime sleeptime@endlink
 * value @e safely during read of that variable on a given thread.
 *
 *
 * @param  thridx   Thread index of thread to read its 
 *                  @link rthread#sleeptime sleeptime@endlink value
 *
 *
 * @returns remaining sleep time, in timer ticks
 *
 */
jlong timeslice_get_thread_sleeptime(jvm_thread_index thridx)
{
    ARCH_FUNCTION_NAME(timeslice_get_thread_sleeptime);

    jlong rc;

    /* Lock out the @e world while retrieving any thread's sleep time */
    pthread_mutex_lock(&pjvm->sleeplock);

    rc = THREAD(thridx).sleeptime;

    /* Unlock the @e world after sleep time retrieved */
    pthread_mutex_unlock(&pjvm->sleeplock);

    /* Report sleep time value */
    return(rc);

} /* END of timeslice_get_thread_sleeptime() */


/*!
 * @brief Length of time slice interval as used by
 * @c @b setitimer(2).
 */
static struct itimerval timeslice_period;


/*!
 * @brief Interval timer handler for the @b signal(SIGALRM) event.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * @warning Eclipse users need to remember that setting a combination
 *          of the value of @link #JVMCFG_TIMESLICE_PERIOD_ENABLE
            JVMCFG_TIMESLICE_PERIOD_ENABLE@endlink
 *          to @link #rtrue rtrue@endlink, the value
 *          of @link #JVMCFG_TIMESLICE_PERIOD_SECONDS
            JVMCFG_TIMESLICE_PERIOD_SECONDS@endlink to zero (0), and
 *          the value of @link #JVMCFG_TIMESLICE_PERIOD_MICROSECONDS
            JVMCFG_TIMESLICE_PERIOD_MICROSECONDS@endlink to a low
 *          milliseconds value is @e certain to interfere with the
 *          proper operation of the GDB debug process due to the high
 *          frequency of thread context changes per second.
 *          The debug session @b will terminate without rhyme nor
 *          reason.  Setting the period to several seconds will
 *          elminate this problem at the expense of time slicing
 *          on a normal basis.  For unit testing, this is not a
 *          problem.  For integration testing, you are on your own....
 *
 */

static void timeslice_tick(/* void --GCC won't allow this declaration*/)
{
    ARCH_FUNCTION_NAME(timeslice_tick);

    /* Suppress SIGALRM until finished with this handler */
    signal(SIGALRM, SIG_IGN);

    /* Debug report of timer.  Use ONLY for SLOW INTERVALS! */
    if (JVMCFG_TIMESLICE_DEBUG_REPORT_MIN_SECONDS != 0) /* 0:= disable*/
    {
        if (JVMCFG_TIMESLICE_DEBUG_REPORT_MIN_SECONDS <=
            timeslice_period.it_interval.tv_sec)
        {
            sysDbgMsg(DML9, arch_function_name, "tick");
        }
    }

    /*
     * Process signal by telling JVM to go to next time slice.
     * Also decrement sleep interval timers for sleeping, joining,
     * and waiting threads.
     */
    pjvm->timeslice_expired = rtrue;
    threadutil_update_sleeptime_interval();

    /* Set next SIGALARM for next tick of the time slice timer */
    signal(SIGALRM, timeslice_tick);

} /* END of timeslice_tick() */


/*!
 * @brief Interval timer thread.
 *
 *
 * @param  dummy The @c @b setitimer(2) system call requires
 *               a <b><code>(void *)</code></b> that has no
 *               meaning here.
 *
 *
 * @returns The required <b><code>(void *)</code></b> is passed back,
 *          but this function never returns until the time slice
 *          thread is killed.
 *
 */
void *timeslice_run(void *dummy)
{
    ARCH_FUNCTION_NAME(timeslice_run);

    /* Start timer and make its first tick one period from now */
    timeslice_period.it_interval.tv_sec =
                                        JVMCFG_TIMESLICE_PERIOD_SECONDS;
    timeslice_period.it_interval.tv_usec =
                                   JVMCFG_TIMESLICE_PERIOD_MICROSECONDS;
    timeslice_period.it_value.tv_sec =
                                        JVMCFG_TIMESLICE_PERIOD_SECONDS;
    timeslice_period.it_value.tv_usec =
                                   JVMCFG_TIMESLICE_PERIOD_MICROSECONDS;

    /* If timer is configured to run, get it going */
    if (rtrue == JVMCFG_TIMESLICE_PERIOD_ENABLE)
    {

        /*
         * Set initial SIGALARM for timer_tick(),
         * arm timer to generate it.
         */
        signal(SIGALRM, timeslice_tick);

        int rc = setitimer(ITIMER_REAL,
                           &timeslice_period,
                           (void *) rnull);

        if (0 != rc)
        {
            sysErrMsg(arch_function_name,"Cannot start interval timer");
            exit_jvm(EXIT_TIMESLICE_START);
/*NOTREACHED*/
        }
    }

    /*
     * Do nothing except wait on timer ticks.
     * Eventually, the main process exits and
     * this thread will die along with it without
     * ever having left the while() loop.
     *
     * If timer is not configured to run, this loop
     * will never see SIGALRM and timer ticks.
     */

    while(rtrue)
    {
        /*!
         * @todo HARMONY-6-jvm-timeslice.c-1 gmj : I think that
         *       yield( is solaris only
         *
         */
#if defined(CONFIG_WINDOWS) || defined(CONFIG_CYGWIN)
        /* do something useful */
#else
        yield();
#endif
    }

/*NOTREACHED*/
    return((void *) rnull);

} /* END of timeslice_run() */


/*!
 * @brief Shut down the time slicing mechanism for JVM shutdown.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid timeslice_shutdown(rvoid)
{
    ARCH_FUNCTION_NAME(timeslice_shutdown);

    /* Suppress SIGALRM so future tick does not happen AT ALL */
    signal(SIGALRM, SIG_IGN);

    /* Ignore error */
    pthread_cancel(posix_thread_id);

    /*!
     * @todo HARMONY-6-jvm-timeslice.c-2 Is this necessary at
     *       JVM shutdown time?
     */
    /* pthread_mutex_destroy(&pjvm->sleeplock); */

    /* Declare this module uninitialized */
    jvm_timeslice_initialized = rfalse;

    return;

} /* END of timeslice_shutdown() */


/* EOF */
