#ifndef _thread_h_included_
#define _thread_h_included_

/*!
 * @file thread.h
 *
 * @brief Definition of the @c @b java.lang.Thread structure in
 * this real machine implementation.
 *
 * There is no notion of a thread group natively in this JVM.  Thread
 * groups are supported at the class library level.
 *
 * @section Control
 *
 * \$URL: https://svn.apache.org/path/name/thread.h $ \$Id: thread.h 0 09/28/2005 dlydick $
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

ARCH_COPYRIGHT_APACHE(thread, h, "$URL: https://svn.apache.org/path/name/thread.h $ $Id: thread.h 0 09/28/2005 dlydick $");


#include <setjmp.h>  /* For jmp_buf structure for setjmp(3)/longjmp(3)*/

#include "jvmreg.h"

/*!
 * @name Macros for addressing threads
 *
 *
 * @param thridx  Thread table index into the global
 * @link #rjvm.thread rjvm.thread[]@endlink array (via
 * @link #pjvm pjvm->thread[]@endlink).
 * 
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @def THREAD
 * @brief Access structures of thread table at certain index.
 *
 * The thread table, being an array of slots, provides space for
 * one thread instance per slot.  This macro references one of
 * them using the @p @b thridx index.
 *
 * @returns pointer to a thread slot
 * 
 */
#define THREAD(thridx) pjvm->thread[thridx]

#define CURRENT_THREAD pjvm->current_thread /**< Access structures of
                                             * the thread now running
                                             * in the JVM.
                                             */

#define PREV_STATE(thridx) THREAD(thridx).prev_state /**<
                                             * Previous actual thread
                                             * state.
                                             */

#define THIS_STATE(thridx) THREAD(thridx).this_state /**<
                                             * This current thread
                                             * state.
                                             */

#define NEXT_STATE(thridx) THREAD(thridx).next_state /**<
                                             * Next requested thread
                                             * state.
                                             */

#define REQ_NEXT_STATE(state, thridx) \
    threadstate_request_##state(thridx)     /**< Place a thread into
                                             * requested thread state,
                                             * see @link
                                              #threadstate_request_new()
                                       threadstate_request_XXX()@endlink
                                             * for specifics.
                                             */

#define CURRENT_THREAD_REQUEST_NEXT_STATE(state) \
    REQ_NEXT_STATE(state, CURRENT_THREAD)   /**< Request that the
                                             * current thread move into
                                             * a certain thread state,
                                             * see @link
                                              #threadstate_request_new()
                                       threadstate_request_XXX()@endlink
                                             * for specifics.
                                             */

#define THREAD_REQUEST_NEXT_STATE(state, thridx) \
    REQ_NEXT_STATE(state, thridx)           /**< Request that an
                                             * arbitrary thread move
                                             * into a certain thread
                                             * state,
                                             * see @link
                                              #threadstate_request_new()
                                       threadstate_request_XXX()@endlink
                                             * for specifics.
                                             */

#define CURRENT_THREAD_ACTIVATE_THIS_STATE(state) \
    threadstate_activate_##state(CURRENT_THREAD) /**<
                                             * Activate current thread
                                             * state, actually moving
                                             * it from a prior state
                                             * into a requested state,
                                             * see @link
                                             #threadstate_activate_new()
                                      threadstate_activate_XXX()@endlink
                                             * for specifics.
                                             */

#define THREAD_ACTIVATE_THIS_STATE(state, thridx) \
    threadstate_activate_##state(thridx)    /**<
                                             * Activate arbitrary thread
                                             * state, actually moving it
                                             * from a prior state into
                                             * a requested state, see
                                             * @link
                                             #threadstate_activate_new()
                                      threadstate_activate_XXX()@endlink
                                             * for specifics.
                                             */

#define CURRENT_THREAD_PROCESS_THIS_STATE(state) \
    threadstate_process_##state(CURRENT_THREAD) /**<
                                             * Process activities for
                                             * current thread in its
                                             * current state, see
                                             * @link
                                              #threadstate_process_new()
                                       threadstate_process_XXX()@endlink
                                             * for specifics.
                                             */

#define THREAD_PROCESS_THIS_STATE(state, thridx) \
    threadstate_process_##state(thridx)     /**<
                                             * Process activities for
                                             * an arbitrary thread in
                                             * its current state, see
                                             * @link
                                              #threadstate_process_new()
                                       threadstate_process_XXX()@endlink
                                             * for specifics.
                                             */

/*@} */ /* End of grouped definitions */


/*!
 * @name Thread state machine state definitions.
 *
 * @brief Thread state machine state names, including min and max
 * states.
 *
 */
/*@{ */ /* Begin grouped definitions */

typedef enum
{
    THREAD_STATE_NEW           = 0, /**< Newly allocated thread */
    THREAD_STATE_START         = 1, /**< @b (transient state) Thread has
                                         been started */
    THREAD_STATE_RUNNABLE      = 2, /**< Thread may run JVM code */
    THREAD_STATE_RUNNING       = 3, /**< Thread is running JVM code */
    THREAD_STATE_COMPLETE      = 4, /**< @b (transient state) Thread has
                                         finished running JVM code */
    THREAD_STATE_BLOCKINGEVENT = 5, /**< @b (transient state) Desist
                                         from @b RUNING JVM code due to
                                         I/O request or control
                                         request such as
                                         @c @b sleep() or
                                         @c @b join() */
    THREAD_STATE_BLOCKED       = 6, /**< Wait for completion of I/O 
                                         request or control request
                                         such as @c @b sleep()
                                         or @c @b join() */
    THREAD_STATE_UNBLOCKED     = 7, /**< @b (transient state) I/O or
                                         control request completed, now
                                         can be @b RUNNABLE again. */
    THREAD_STATE_SYNCHRONIZED  = 8, /**< @b (transient state) Entered a
                                         @c @b synchronized
                                         block */
    THREAD_STATE_RELEASE       = 9, /**< @b (transient state) A
                                         @c @b wait() request
                                         released its lock. */
    THREAD_STATE_WAIT          = 10, /**< @c @b wait() for a
                                          @c @b notify(),
                                          @c @b notifyAll()
                                          or @c @b interrupt()
                                          event. */
    THREAD_STATE_NOTIFY        = 11, /**< Received a
                                          @c @b notify(),
                                          @c @b notifyAll()
                                          or @c @b interrupt()
                                          event. */
    THREAD_STATE_LOCK          = 12, /**< Negotiate to @b ACQUIRE the
                                          object's monitor lock again.*/
    THREAD_STATE_ACQUIRE       = 13, /**< @b (transient state)
                                          Re-acquired object's monitor
                                          lock, may be @b RUNNABLE
                                          again. */
    THREAD_STATE_DEAD          = 14, /**< All work complete, ready to
                                          deallocate thread slot. */
    THREAD_STATE_BADLOGIC      = 15 /**< Not in spec, code convenience
                                          for dealing with illegal
                                          operational conditions. */
} thread_state_enum;


#define THREAD_STATE_MIN_STATE    THREAD_STATE_NEW /**< Lowest possible
                                                        state number */
#define THREAD_STATE_MAX_STATE    THREAD_STATE_BADLOGIC /**< Highest
                                                        possible state
                                                        number */
/*@} */ /* End of grouped definitions */

/*!
 * @name JVM state priority definitions.
 *
 */

/*@{ */ /* Begin grouped definitions */

typedef enum
{
    THREAD_PRIORITY_BAD   = 0, /**< Not in spec, but useful in code */
    THREAD_PRIORITY_MIN   = 1, /**< Minimum thread priority */
    THREAD_PRIORITY_NORM  = 5, /**< Normal thread priority */
    THREAD_PRIORITY_MAX   = 10 /**< Maximum thread priority */

} thread_priority_enum;

/*@} */ /* End of grouped definitions */


/*!
 * @brief Real machine thread model implementation.
 */
typedef struct
{

/*********** Thread definitions ****************************/

    jvm_thread_index id;         /**< Self-same ID of this thread */

    rchar            *name;      /**< Name of thread
                                  *  @todo not impl.
                                  */
#define THREAD_NAME_MAX_LEN 64   /**< Arbitrary max thread name length*/

    jvm_object_hash thread_objhash;/**< Object hash for associated
                                        java.lang.Thread object */

    thread_priority_enum priority; /**< Runtime THREAD_PRIORITY_xxx */

    rushort          status;    /**< Runtime status of thread, bitwise*/

/****** First 2 bits same for class, object, and thread ***********/
#define THREAD_STATUS_EMPTY 0x0000 /**< This slot is available for use.
                                    *   <b>DO NOT CHANGE</b> since this
                                    *   is also the normal
                                    *   @c @b setjmp(3) return code
                                    *   from @link
                                        #thread_exception_setup()
                                        thread_exception_setup()@endlink
                                    */
#define THREAD_STATUS_INUSE 0x0001 /**< This slot contains a thread */
#define THREAD_STATUS_NULL  0x0002 /**< Null thread (only 1 exists in
                                    * normal use, any else besides the
                                    * JVMCFG_NULL_THREAD is a thread
                                    * slot that is being initialized.)*/
/******************************************************************/

/****** The remaining bits are unique to thread *******************/
#define THREAD_STATUS_ISDAEMON  0x0004 /**< Daemon thread state, vs user
                                       thread, per Thread.isdaemon() */

#define THREAD_STATUS_SLEEP     0x0008 /**< thread is sleeping */

#define THREAD_STATUS_JOIN4EVER 0x0010 /**< thread has unconditionally
                                            joined another */

#define THREAD_STATUS_JOINTIMED 0x0020 /**< thread has joined another,
                                     but is waiting for a finite time */

#define THREAD_STATUS_WAIT4EVER 0x0040 /**< thread is unconditionally
                                            waiting */

#define THREAD_STATUS_WAITTIMED 0x0080 /**< thread is waiting, but
                                            finite time */

#define THREAD_STATUS_SUSPEND   0x0100 /**< thread is suspended, can
                                            be resumed */

#define THREAD_STATUS_INTERRUPTIBLEIO 0x0200 /**< thread is waiting on
                               an interruptable I/O channel in class
                               java.nio.channels.InterruptibleChannel */

#define THREAD_STATUS_NOTIFIED  0x0400 /**< thread has been notified */


#define THREAD_STATUS_INTERRUPTED 0x0800 /**< thread has been
                                              interrupted */

#define THREAD_STATUS_THREW_EXCEPTION 0x1000 /**< thread threw a
                                  * @c @b java.lang.Exception
                                  * (but NOT a
                                  * @c @b java.lang.Error).  The
                                  * object type is found in @link
                                    #rthread.pThrowableEvent
                                    pThrowableEvent@endlink and is not
                                  * @link #rnull rnull@endlink.
                                  */

#define THREAD_STATUS_THREW_ERROR     0x2000 /**< thread threw a
                                  * @c @b java.lang.Error
                                  * (but NOT a
                                  * @c @b java.lang.Exception).
                                  * The object type is found in @link
                                    #rthread.pThrowableEvent
                                    pThrowableEvent@endlink and is not
                                  * @link #rnull rnull@endlink.
                                  */

#define THREAD_STATUS_THREW_THROWABLE 0x4000 /**< thread threw a
                                  * @c @b java.lang.Throwable
                                  * of unknowable type.  The
                                  * object type is found in @link
                                    #rthread.pThrowableEvent
                                    pThrowableEvent@endlink and is not
                                  * @link #rnull rnull@endlink.
                                  */

#define THREAD_STATUS_THREW_UNCAUGHT 0x8000 /**< A
                                  * @c @b java.lang.Throwable
                                  * of some type was thrown, but not
                                  * handled by the Java code itself.
                                  *  Instead, it will be handled
                                  * by the default
                           @c @b java.lang.ThreadGroup.uncaughtException
                                  * method.  The value of @link
                                    #rthread.pThrowableEvent
                                            pThrowableEvent@endlink
                                  * will be @link #rnull rnull@endlink.
                                  */

/******************************************************************/

    rchar *pThrowableEvent;      /**< Exception, Error, or Throwable
                                  * that was thrown by this thread.
                                  * @link #rnull rnull@endlink
                                  * if nothing was thrown.
                                  */

    jmp_buf *pnonlocal_ThrowableEvent; /**< Non-local return from
                                  * Exception, Error, and Throwable
                                  * conditions.
                                  *
                                  * @note Play pointer games to persuade
                                  * runtime package to accept a jmp_buf
                                  * that is within a structure.  Whether
                                  * this is a "feature" of the Solaris
                                  * version of the library remains to be
                                  * seen.
                                  *
                                  * Refer to opcode_run() for more
                                  * information.
                                  */


/*********** State and blocking definitions ****************/

    thread_state_enum prev_state; /**< Previous @link #THREAD_STATE_NEW
                                       THREAD_STATE_xxx@endlink
                                       (for diagnostics) */

    thread_state_enum this_state; /**< Current @link #THREAD_STATE_NEW
                                       THREAD_STATE_xxx@endlink */

    thread_state_enum next_state; /**< Next scheduled
                                       @link #THREAD_STATE_NEW
                                       THREAD_STATE_xxx@endlink */

    jvm_thread_index jointarget; /**< Doing @c @b Thread.join()
                                      onto this thread */

    volatile jlong   sleeptime;  /**< Milliseconds left on either
                                  * @c @b Thread.sleep()
                                  * or on a timed
                                  * @c @b Thread.wait()
                                  * or a timed
                                  * @c @b Thread.join().
                                  * See also comments in
                                    @link jvm/src/jvm.h jvm.h@endlink
                                  * and in @link jvm/src/timeslice.c
                                    timeslice.c@endlink.
                                  */

    jvm_object_hash  locktarget; /**< Doing @c @b Object.wait()
                                  * on this object.
                                  *
                                  * @todo  This implementation can only
                                  * handle ONE SINGLE monitor lock.
                                  * Does it need to be able to
                                  * handle arbitrary number of them at
                                  * once?
                                  */


/*********** Program counter definitions *******************/

    jvm_pc pc;                   /**< Program counter of this thread */

    ruint  pass_instruction_count;   /**< Number of JVM instructions
                                      * that have been run during this
                                      * pass.
                                      */

    rulong thread_instruction_count; /**< Total number of JVM
                                      * instructionsthat have been
                                      * run by this thread.
                                      */


/*********** Stack pointer definitions *********************/

    jint   *stack;               /**< Stack area for this thread */

    jvm_sp sp;                   /**< Stack pointer of this thread */

    jvm_sp fp;                   /**< Frame pointer in stack
                                      to this stack frame.  */

    jvm_sp fp_end_program;       /**< Final frame pointer in stack of
                                  * place to stop execution, starting
                                  * at beginning of program, but could
                                  * be changed to a throwable event
                                  * or some other arbitrary stack
                                  * frame.  Once the inner loop
                                  * detects that the next stack frame
                                  * would move beyond this boundary,
                                  * JVM inner loop execution halts.
                                  */

} rthread;


/*!
 * @name Thread state name strings
 *
 */

/*@{ */ /* Begin grouped definitions */

extern const rchar *thread_state_name;

#define THREAD_STATE_NEW_DESC           "new"
#define THREAD_STATE_START_DESC         "started"
#define THREAD_STATE_RUNNABLE_DESC      "runnable"
#define THREAD_STATE_RUNNING_DESC       "running"
#define THREAD_STATE_COMPLETE_DESC      "complete"
#define THREAD_STATE_BLOCKINGEVENT_DESC "blockingevent"
#define THREAD_STATE_BLOCKED_DESC       "blocked"
#define THREAD_STATE_UNBLOCKED_DESC     "unblocked"
#define THREAD_STATE_SYNCHRONIZED_DESC  "synchronized"
#define THREAD_STATE_RELEASE_DESC       "release"
#define THREAD_STATE_WAIT_DESC          "wait"
#define THREAD_STATE_NOTIFY_DESC        "notify"
#define THREAD_STATE_LOCK_DESC          "lock"
#define THREAD_STATE_ACQUIRE_DESC       "acquire"
#define THREAD_STATE_DEAD_DESC          "dead"
#define THREAD_STATE_BADLOGIC_DESC      "bad_logic"
#define THREAD_STATE_ILLEGAL_DESC       "illegal"

/*@} */ /* End of grouped definitions */


/* Prototypes for functions in 'thread.c' */

extern const rchar *thread_state_get_name(rushort state);

extern jvm_thread_index thread_class_load(rchar        *clsname,
                                          rchar        *mthname,
                                          rchar        *mthdesc,
                                          rint          priority,
                                          rboolean      isdaemon,
                                          rboolean      usesystemthread,
                                         rboolean find_registerNatives);

extern rvoid thread_init(rvoid);

extern rboolean thread_die(jvm_thread_index thridx);

extern rvoid thread_shutdown(rvoid);

extern int thread_exception_setup(jvm_thread_index thridx);
 
extern rvoid thread_throw_exception(jvm_thread_index  thridx,
                                    rushort  thread_status_bits,
                                    rchar   *exception_name);

/* Prototypes for functions in 'threadstate.c' */

/*!
 * @name Thread state phases.
 *
 * @brief Each thread state has three phases, each of which is handled
 * by a dedicated function.
 *
 * @b Requesting a state checks to see if a transition from the current
 * state into the requested state is valid.  If so, the @b NEXT
 * state is set to the requested one.  If not, the request is
 * ignored.
 *
 * @b Activating a state moves the thread into the requested @b NEXT
 * state so its @b THIS state changes.  Its @b PREV state then reports
 * the former state.
 *
 * @b Processing a state performs the activities for that state.
 *
 *
 * @param thridx Thread table index of thread to process.
 *
 *
 * @returns @link #rtrue rtrue@endlink if activities transpired
 *          normally, otherwise @link #rfalse rfalse@endlink.
 *
 */

/*@{ */ /* Begin grouped definitions */

extern rboolean threadstate_request_new(jvm_thread_index thridx);
extern rboolean threadstate_request_start(jvm_thread_index thridx);
extern rboolean threadstate_request_runnable(jvm_thread_index thridx);
extern rboolean threadstate_request_running(jvm_thread_index thridx);
extern rboolean threadstate_request_complete(jvm_thread_index thridx);
extern rboolean threadstate_request_blockingevent(jvm_thread_index
                                                                thridx);
extern rboolean threadstate_request_blocked(jvm_thread_index thridx);
extern rboolean threadstate_request_unblocked(jvm_thread_index thridx);
extern rboolean threadstate_request_synchronized(jvm_thread_index
                                                                thridx);
extern rboolean threadstate_request_release(jvm_thread_index thridx);
extern rboolean threadstate_request_wait(jvm_thread_index thridx);
extern rboolean threadstate_request_notify(jvm_thread_index thridx);
extern rboolean threadstate_request_lock(jvm_thread_index thridx);
extern rboolean threadstate_request_acquire(jvm_thread_index thridx);
extern rboolean threadstate_request_dead(jvm_thread_index thridx);
extern rboolean threadstate_request_badlogic(jvm_thread_index thridx);

extern rboolean threadstate_activate_new(jvm_thread_index thridx);
extern rboolean threadstate_activate_start(jvm_thread_index thridx);
extern rboolean threadstate_activate_runnable(jvm_thread_index thridx);
extern rboolean threadstate_activate_running(jvm_thread_index thridx);
extern rboolean threadstate_activate_complete(jvm_thread_index thridx);
extern rboolean threadstate_activate_blockingevent(jvm_thread_index
                                                                thridx);
extern rboolean threadstate_activate_blocked(jvm_thread_index thridx);
extern rboolean threadstate_activate_unblocked(jvm_thread_index thridx);
extern rboolean threadstate_activate_synchronized(jvm_thread_index
                                                                thridx);
extern rboolean threadstate_activate_release(jvm_thread_index thridx);
extern rboolean threadstate_activate_wait(jvm_thread_index thridx);
extern rboolean threadstate_activate_notify(jvm_thread_index thridx);
extern rboolean threadstate_activate_lock(jvm_thread_index thridx);
extern rboolean threadstate_activate_acquire(jvm_thread_index thridx);
extern rboolean threadstate_activate_dead(jvm_thread_index thridx);
extern rboolean threadstate_activate_badlogic(jvm_thread_index thridx);

extern rboolean threadstate_process_new(jvm_thread_index thridx);
extern rboolean threadstate_process_start(jvm_thread_index thridx);
extern rboolean threadstate_process_runnable(jvm_thread_index thridx);
extern rboolean threadstate_process_running(jvm_thread_index thridx);
extern rboolean threadstate_process_complete(jvm_thread_index thridx);
extern rboolean threadstate_process_blockingevent(jvm_thread_index
                                                                thridx);
extern rboolean threadstate_process_blocked(jvm_thread_index thridx);
extern rboolean threadstate_process_unblocked(jvm_thread_index thridx);
extern rboolean threadstate_process_synchronized(jvm_thread_index
                                                                thridx);
extern rboolean threadstate_process_release(jvm_thread_index thridx);
extern rboolean threadstate_process_wait(jvm_thread_index thridx);
extern rboolean threadstate_process_notify(jvm_thread_index thridx);
extern rboolean threadstate_process_lock(jvm_thread_index thridx);
extern rboolean threadstate_process_acquire(jvm_thread_index thridx);
extern rboolean threadstate_process_dead(jvm_thread_index thridx);
extern rboolean threadstate_process_badlogic(jvm_thread_index thridx);

/*@} */ /* End of grouped definitions */


/* Prototypes for functions in 'threadutil.c' */
extern rvoid threadutil_update_sleeptime_interval(rvoid);

extern rvoid threadutil_update_blockingevents(jvm_thread_index
                                                            thridxcurr);

extern rvoid threadutil_update_wait(jvm_thread_index thridxcurr);

extern rvoid threadutil_update_lock(jvm_thread_index thridxcurr);

extern rboolean threadutil_holds_lock(jvm_thread_index thridx,
                                      jvm_object_hash  objhashlock);

#endif /* _thread_h_included_ */


/* EOF */
