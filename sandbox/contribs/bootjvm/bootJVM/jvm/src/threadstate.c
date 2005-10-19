/*!
 * @file threadstate.c
 *
 * @brief Validate and perform state transitions, and process each
 * state of the JVM thread state machin.
 *
 * This implementation of the JVM state machine using both stable and
 * transient states.  The thread system operates as a state machine
 * based on the JVM spec requirements.  The states for this
 * implementation are shown below.  Those marked @b (+) are
 * transient states and will move forward to the next stable
 * state as soon as possible:
 *
 * <ul>
 * <li>
 * @b THREAD_STATE_NEW          Thread is brand new and ready to run.
 *                              Before now, the
 *                              @link #THREAD_STATUS_INUSE
                                THREAD_STATUS_INUSE@endlink bit was
 *                              clear.
 * </li>
 * <li>
 * @b THREAD_STATE_START @b (+)
 *                              The thread.start() operation was
 *                              requested to start execution of thread's
 *                              java.lang.Thread.run() method.
 * </li>
 * <li>
 * @b THREAD_STATE_RUNNABLE     The a started thread may run when its
 *                              turn arrives in the arbitration
 *                              sequence.
 * </li>
 * <li>
 * @b THREAD_STATE_RUNNING      This thread is currently running.
 * </li>
 * <li>
 * @b THREAD_STATE_BLOCKEVENT @b (+)
 *                              Blocking event occurred (such as sleep()
 *                              or I/O operation).
 * </li>
 * <li>
 * @b THREAD_STATE_BLOCKED      This thread is blocked on blocking
 *                              event.
 * </li>
 * <li>
 * @b THREAD_STATE_UNBLOCKED @b (+)
 *                             Blocking event has expired (eg, sleep()
 *                             was called) that put the thread into a
 *                             blocked state.
 * </li>
 * <li>
 * @b THREAD_STATE_SYNCHRONIZED @b (+)
 *                             Another thread has engaged an object's
 *                             monitor lock.
 * </li>
 * <li>
 * @b THREAD_STATE_RELEASE @b (+)
 *                             This thread DOES have an object's monitor
 *                             lock and has called wait() to disengage
 *                             it and prepare to wait.
 * </li>
 * <li>
 * @b THREAD_STATE_WAIT        Waiting on one of
 *                             notify()/notifyAll()/interrupt()
 * </li>
 * <li>
 * @b THREAD_STATE_NOTIFY @b (+)
 *                             This waiting thread has been awakened
 *                             by notify()/notifyAll() or interrupt().
 * </li>
 * <li>
 * @b THREAD_STATE_LOC         This thread is waiting on an object's
 *                             monitor lock to be released by another
 *                             thread.
 * </li>
 * <li>
 * @b THREAD_STATE_ACQUIRE @b (+)
 *                             This thread now has an object's monitor
 *                             lock after it was released by another
 *                             thread.
 * </li>
 * <li>
 * @b THREAD_STATE_COMPLETE @b (+)
 *                             This thread has completed its run()
 *                             method and is ready to terminate because
 *                             it has had some other event occur to
 *                             finish its execution.
 * </li>
 * <li>
 * @b THREAD_STATE_DEAD        This thread has been shut down and does
 *                             is ready to have its thread slot
 *                             deallocated by clearing the @link
                               #THREAD_STATUS_INUSE
                               THREAD_STATUS_INUSE@endlink bit.
 * </li>
 * <li>
 * @b THREAD_STATE_BADLOGIC (sometimes @b (+) )
 *                             Diagnostic state, sometimes transient,
 *                             sometimes stable, depending on what
 *                             it is being used for.  See state diagram
 *                             below for details.
 * </li>
 * </ul>
 *
 * The state transitions are:
 *
 * <ul>
 * <li>
 * <b>(a) Simple execution</b>:
 *
 *         not used -> NEW -> START @b (+) -> RUNNABLE -> RUNNING
 *         -> COMPLETE @b (+) -> DEAD -> not used
 *
 * </li>
 * <li>
 * <b>(b) java.lang.Thread.yield()</b>:
 *
 *         RUNNING -> RUNNABLE -> RUNNING
 *
 * </li>
 * <li>
 * <b>(c) java.lang.Thread.sleep(), java.lang.Thread.join(),
 * java.lang.Thread.interrupt(), interruptible I/O</b>:
 *
 *         RUNNING -> BLOCKINGEVENT @b (+) -> BLOCKED
 *         -> UNBLOCKED @b (+) -> RUNNABLE
 *
 * </li>
 * <li>
 *
 * <b>(d) synchronized(Object)</b>:
 *
 *         RUNNING -> SYNCHRONIZED @b (+) -> LOCK
 *         -> ACQUIRE @b (+) -> RUNNABLE
 *
 * </li>
 * <li>
 * <b>(e) Object.wait()</b>:
 *
 *         RUNNING -> RELEASE @b (+) -> WAIT -> NOTIFY @b (+)
 *         -> LOCK -> ACQUIRE @b (+) -> RUNNABLE 
 *
 * </li>
 * <li>
 * <b>(f) Stillborn thread</b>:
 *
 *         NEW -> BADLOGIC @b (+) -> COMPLETE @b (+) -> DEAD
 *
 * </li>
 * <li>
 * <b>(g) java.lang.Thread.destroy(), java.lang.Thread.stop()-- both
 * deprecated</b>:
 *
 *         anywhere -> BADLOGIC @b (+) -> COMPLETE @b (+) -> DEAD
 *
 * </li>
 * <li>
 * <b>(h) java.lang.Thread.suspend()-- deprecated</b>:
 *
 *         anywhere -> BADLOGIC -> BLOCKINGEVENT @b (+) -> BLOCKED
 *
 * </li>
 * <li>
 * <b>(i) java.lang.Thread.resume()-- deprecated</b>:
 *
 *         BLOCKED -> UNBLOCKED @b (+) -> RUNNABLE
 *
 * </li>
 * <li>
 * <b>(j) Somebody GOOFED!  Programmatic suspend</b>:
 *
 *         anywhere-> BADLOGIC
 *
 * </li>
 * </ul>
 *
 * The first line <b>(line a)</b> is the main flow of control.
 *
 * <b>(line b)</b> depend on various programmatic events such as
 * java.lang.Thread.yield() invocation and normal JVM thread
 * arbitration.
 *
 * <b>(line c)</b> involves scheduled programmatic events like
 * java.lang.Thread.sleep() and java.lang.Thread.join(),
 * java.lang.Thread.interrupt() events (line c),
 * or interruptible I/O operations.
 *
 * <b>(line d)</b> depicts @c @b synchronized() requests (line d)
 *
 * <b>(line e)</b> involives java.lang.Object.wait() requests (after
 * object lock acquired) via java.lang.Object.notify() or
 * java.lang.Object.notifyAll() or java.lang.Thread.interrupt()
 * events.
 *
 * <b>(line f)</b>, shows the so-called "stillborn" thread that dies
 * before it has a chance to java.lang.Thread.start().
 *
 * <b>(lines g, h, i)</b>, demonstrates how this implementation
 * gracefully minimizes the damage of the deprecated
 * java.lang.Thread.destroy(), java.lang.Thread.stop(), 
 * java.lang.Thread.suspend(), and java.lang.Thread.resume().
 *
 * <b>(line j)</b>, shows a state that has been added to handle errors,
 * typically from development and testing.  This provides a valid
 * state transition that may be done programmatically to sideline
 * any desired thread.
 *
 * Notice that there are more states here than in the classic model
 * for the JVM.  Part of the reason for this is that a finer granularity
 * of control provides the state model with an insertion point for
 * diagnostics of the state model, especially in the transient states
 * (namely @b START, @b RELEASE, @b SYNCHRONIZED, @b NOTIFY, @b ACQUIRE,
 * and @b COMPLETE).  This is complementary to the definition of each
 * state.  The implementation of this model includes three groups of
 * functions named after each of three phases of state change and
 * processing and after each state name:
 *
 * <ul>
 * <li>
 * @link #threadstate_request_new() threadstate_request_XXX()@endlink:
 * Request that a thread move from the current state to
 * the @b XXX state.
 * </li>
 *
 * <li>
 * @link #threadstate_activate_new() threadstate_activate_XXX()@endlink:
 * Move a thread from the current state to the @b XXX state after
 * it was requested by the related function
 * @link #threadstate_request_new() threadstate_request_XXX()@endlink.
 * </li>
 *
 * <li>
 * @link #threadstate_process_new() threadstate_process_XXX()@endlink:
 * Perform normal actions appropriate for this state.  If this is a
 * stable state like @b RUNNING, run the next time slice of virtual
 * instructions.  If it is a transient state like @b START, request
 * transition on to the next state (in this case, @b RUNNABLE would
 * be next).
 * </li>
 * </ul>
 *
 * These three stages in the JVM outer loop provide immediate
 * diagnostics in case of problems with state transitions, which is,
 * of course, useful during development, but can also provide
 * safeguards during normal runtime.  The processing overhead is
 * negligible since it is in the @e outer loop.
 * All groups of routines return a boolean describing whether or not
 * the activities for that phase of that state succeeded. For example,
 * thread_request_runnable() attempts to take a thread from its current
 * state into
 * @link #THREAD_STATE_RUNNABLE THREAD_STATE_RUNNABLE@endlink.
 * If it succeeds, it returns @link #rtrue rtrue@endlink, otherwise
 * @link #rfalse rfalse@endlink.
 *
 * All transition requests work @e exactly the same way.  The actual
 * performance of that change may require doing some work, depending
 * on the particular state being requested, but most require only a
 * simple state transition.  For example, the beginning of the world is
 * threadstate_request_start(), which is equivalent to the
 * java.lang.Thread method java.lang.Thread.start().
 * At various times during development, it had some work to perform
 * as the thread state model was integrated into the JVM, such as
 * loading the JVM's PC with the entry point of the correct method.
 * But in the case of threadstate_request_unblocked(), a state
 * transition will also be requested since it is a transitory state in
 * addition to any other work it may perform.
 *
 * All logic for each state change is found within its respective
 * @b threadstate_[@link #threadstate_request_new() request@endlink|
   @link #threadstate_activate_new() activate@endlink
   @link #threadstate_process_new() process@endlink]<b>_XXX()</b>
 * function below.
 *
 *
 * @attention: Only a @b NEW thread may be moved into the @b START
 *             state.  Only an @link #THREAD_STATUS_EMPTY
               THREAD_STATUS_EMPTY@endlink thread may be moved into
 *             @link #THREAD_STATUS_INUSE THREAD_STATUS_INUSE@endlink
 *             status and hence the @b NEW state.  Only the
 *             @link #JVMCFG_NULL_THREAD JVMCFG_NULL_THREAD@endlink
 *             may be permanently marked as being
 *             @link #THREAD_STATUS_NULL THREAD_STATUS_NULL@endlink.
 *             These two conditions must @e never be manipulated
 *             by thread_new() and thread_die().
 *
 *
 * @section Control
 *
 * \$URL$ \$Id$
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
 *         Original code contributed by Daniel Lydick on 09/28/2005.
 *
 * @section Reference
 *
 */

#include "arch.h"
ARCH_COPYRIGHT_APACHE(threadstate, c, "$URL$ $Id$");


#include "jvmcfg.h"
#include "classfile.h"
#include "jvm.h"
#include "opcode.h"


/*!
 * @name State phase macros
 *
 * @brief Macros to support the three phases of moving from one
 * thread state to another.
 *
 * See section on the JVM thread model state machine for an explanation
 * as to @e why these functions exist.  Concerning @e how they operate,
 * an example of their usage in the @b START state functions will
 * provide clear guidance of how to use them generally.  The first state
 * transition request is always from @b NEW to @b START.  The macro
 * expansion of the macros inside of @link #threadstate_request_start()
   threadstate_request_start()@endlink demonstrate syntax for how
 * this accomplihed.  Following that is the syntax for @link
    #threadstate_activate_start() threadstate_activate_start()@endlink
 * and @link #threadstate_process_start()
   threadstate_process_start()@endlink.  The declaration of these
 * three functions follows later.  For good form when using these
 * functions with these state phase macros, <em>be sure</em> to place
 * the whole of the second parameter expression of STATE_REQUEST()
 * in (parentheses).
 *
 * Transition is shown from @b NEW to @b START to show a more
 * normal example than from "not in use" to @b NEW.
 *
 * The expansion of the macros inside of the three @b START functions
 * produces the following code:
 *
 * @verbatim
   rboolean threadstate_request_start(jvm_thread_index thridx)
   {
       if (THREAD_STATE_NEW == THREAD(thridx).this_state)
       {
           THREAD(thridx).next_state = THREAD_STATE_START;
   
           ** use (rtrue) if nothing else needed **
           return((rtrue) ? rtrue : rfalse);
       }
       else
       {
           return(rfalse);
       };
   }
   
   rboolean threadstate_activate_start(jvm_thread_index thridx)
   {
       if (THREAD_STATE_START == THREAD(thridx).next_state)
       {
         ** Record previous state when changing to next one **
           THREAD(thridx).prev_state = THREAD(thridx).this_state;
           THREAD(thridx).this_state = THREAD(thridx).next_state;
       }
       if (THREAD_STATE_START == THREAD(thridx).this_state)
       {
           return((rtrue) ? rtrue : rfalse);
       }
       else
       {
           return(rfalse);
       };
   }
   
   rboolean threadstate_process_start(jvm_thread_index thridx)
   {
       if (THREAD_STATE_START == THREAD(thridx).this_state)
       {
    
          ** ... Process activities for this thread state here ... **
   
          ** use (rtrue) if nothing else needed **
          return((threadstate_request_runnable(thridx)) ? rtrue:rfalse);
       }
       else
       {
           return(rfalse);
       };
  }
  
   @endverbatim
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @def STATE_REQUEST()
 *
 * @brief Request a state change from this state to another state.
 *
 *
 * @param upper             New state name in UPPER CASE (for macro
 *                          expansion purposes)
 *
 * @param unique_state_test <em>BE SURE</em> to put put this parameter
 *                          in (parentheses) for proper macro expansion!
 *                          Permit state change into requested state
 *                          @e only if this expression evaluates to
 *                          @link #rtrue rtrue@endlink, which may be
 *                          explicitly stated if this state change is
 *                          unconditional.
 *
 * @returns @link #rtrue rtrue@endlink if state change was permitted,
 *          otherwise @link #rfalse rfalse@endlink.
 *
 */
#define STATE_REQUEST(upper, unique_state_test)               \
    if (unique_state_test)                                    \
    {                                                         \
        NEXT_STATE(thridx) = THREAD_STATE_##upper

/*  } ignore-- keeps text editors happy */

/*!
 * @def STATE_ACTIVATE()
 *
 * @brief Activate a state change that was validated by
 * @link #STATE_REQUEST() STATE_REQUEST()@endlink.
 *
 *
 * @param upper             New state name in UPPER CASE (for macro
 *                          expansion purposes)
 *
 *
 * @returns @link #rtrue rtrue@endlink if state change was permitted,
 *          otherwise @link #rfalse rfalse@endlink.
 *
 */
#define STATE_ACTIVATE(upper)                                 \
    if (THREAD_STATE_##upper == NEXT_STATE(thridx))           \
    {                                                         \
        /* Record previous state when changing to next one */ \
        PREV_STATE(thridx) = THIS_STATE(thridx);              \
        THIS_STATE(thridx) = NEXT_STATE(thridx);              \
    }                                                         \
                                                              \
    if (THREAD_STATE_##upper == THIS_STATE(thridx))           \
    {

/*  } ignore-- keeps text editors happy */

/*!
 * @def STATE_PROCESS()
 *
 * @brief Process a state change that was activated by
 * @link #STATE_ACTIVATE() STATE_ACTIVATE()@endlink.
 *
 *
 * @param upper             New state name in UPPER CASE (for macro
 *                          expansion purposes)
 *
 *
 * @returns @link #rtrue rtrue@endlink if state change was permitted,
 *          otherwise @link #rfalse rfalse@endlink.
 *
 */
#define STATE_PROCESS(upper)                                  \
    if (THREAD_STATE_##upper == THIS_STATE(thridx))           \
    {

/*  } ignore-- keeps text editors happy */

/*  { ignore-- keeps text editors happy */

/*!
 * @def STATE_END()
 *
 * @brief Terminate the code fragment initiated by
 * @link #STATE_REQUEST() STATE_REQUEST()@endlink or
 * @link #STATE_ACTIVATE() STATE_ACTIVATE()@endlink or
 * @link #STATE_PROCESS() STATE_PROCESS()@endlink.
 *
 * In between the @link #STATE_REQUEST() STATE_REQUEST()@endlink and
 * @link #STATE_ACTIVATE() STATE_ACTIVATE()@endlink and
 * @link #STATE_PROCESS() STATE_PROCESS()@endlink macro instance and 
 * this macro, any phase-specific, state-specific code may be inserted.
 * Although most states do not do so, <em>pay particular attention</em>
 * to those that do, also any comments that may be present in lieu of
 * code.
 *
 */
#define STATE_END(expr)                                       \
        /* use (rtrue) if nothing else needed */              \
        return((expr) ? rtrue : rfalse);                      \
    }                                                         \
    else                                                      \
    {                                                         \
        return(rfalse);                                       \
    }

/*@} */ /* End of grouped definitions */


/*!
 * @name JVM thread model state machine
 *
 * @brief Implement the JVM state machine using both stable and
 * transient states.
 *
 * See tables above for full description.
 *
 * 
 * @todo  Need to find a way (per spec section 5.3.5) to throw
 *        a java.lang.LinkageError and/or java.lang.ClassFormatError
 *        for bad classfile representations.  Also major/minor versions
 *        mismatch should throw
 *        java/class/UnsupportedClassVersion error.
 *
 *
 * @param thridx thread index of thread whose state is to be changed.
 *
 *
 * @returns @link #rtrue rtrue@endlink if all activities were
 * successful, else @link #rfalse rfalse@endlink.
 *
 */

/*@{ */ /* Begin grouped definitions */


/*****************************************************************/

/*!
 * @brief Request the @b NEW thread state.
 *
 * The @b NEW state starts the whole state machine for a thread.
 *
 */
rboolean threadstate_request_new(jvm_thread_index thridx)
{
    STATE_REQUEST(NEW, (rtrue));
    STATE_END(rtrue);
}


/*!
 * @brief Activate the @b NEW thread state for a new thread.
 *
 */
rboolean threadstate_activate_new(jvm_thread_index thridx)
{
    STATE_ACTIVATE(NEW);
    STATE_END(rtrue);
}


/*!
 * @brief Process the @b NEW thread state.
 *
 * The @b NEW state idles until the @b START state is requested.
 *
 */
rboolean threadstate_process_new(jvm_thread_index thridx)
{
    STATE_PROCESS(NEW);
    STATE_END(rtrue);
}

/*****************************************************************/

/*!
 * @brief Request the @b START state.
 *
 * The @b START state can only be entered from @b NEW.
 *
 */

rboolean threadstate_request_start(jvm_thread_index thridx)
{
    STATE_REQUEST(START, (THREAD_STATE_NEW == THIS_STATE(thridx)));
    STATE_END(rtrue);
}

/*!
 * @brief Activate the @b START thread state of a thread known
 * to be coming from a valid previous state.
 *
 */
rboolean threadstate_activate_start(jvm_thread_index thridx)
{
    STATE_ACTIVATE(START);
    STATE_END(rtrue);
}

/*!
 * @brief Process the @b START thread state.
 *
 * <em>THIS IS A TRANSIENT STATE!</em>  A state change
 * from @b START to @b RUNNABLE is requested here.
 *
 */
rboolean threadstate_process_start(jvm_thread_index thridx)
{
    STATE_PROCESS(START);

    /* ... Process activities for this thread state here ... */

    STATE_END(threadstate_request_runnable(thridx));
}

/*****************************************************************/

/*!
 * @brief Request the @b RUNNABLE state.
 *
 * The @b RUNNABLE state can be entered from @b START, @b RUNNING,
 * @b UNBLOCKED, or @b ACQUIRE.
 *
 */
rboolean threadstate_request_runnable(jvm_thread_index thridx)
{
    STATE_REQUEST(RUNNABLE, 
                  ((THREAD_STATE_START     == THIS_STATE(thridx)) ||
                   (THREAD_STATE_RUNNING   == THIS_STATE(thridx)) ||
                   (THREAD_STATE_UNBLOCKED == THIS_STATE(thridx)) ||
                   (THREAD_STATE_ACQUIRE   == THIS_STATE(thridx))    ));
    STATE_END(rtrue);
}

/*!
 * @brief Activate the @b RUNNABLE thread state of a thread known
 * to be coming from a valid previous state.
 *
 */
rboolean threadstate_activate_runnable(jvm_thread_index thridx)
{
    STATE_ACTIVATE(RUNNABLE);
    STATE_END(rtrue);
}

/*!
 * @brief Process the @b RUNNABLE thread state.
 *
 * Always try to keep @b RUNNABLE thread in the @b RUNNING state.
 * Although not formally a transient state, @b RUNNABLE indicates
 * that a thread is @e potentially one that could be @b RUNNING,
 * thus the attempt to keep it there.
 *
 */
rboolean threadstate_process_runnable(jvm_thread_index thridx)
{
    STATE_PROCESS(RUNNABLE);
    STATE_END(rtrue);
}


/*****************************************************************/

/*!
 * @brief Request the @b RUNNING state.
 *
 * The @b RUNNING state can only be entered from @b RUNNABLE.
 *
 * The JVM inner execution loop is called from
 * threadstate_process_running(), causing any thread in the
 * @b RUNNING state to normally keep executing its code
 * from where it left off last time.
 *
 */
rboolean threadstate_request_running(jvm_thread_index thridx)
{
    STATE_REQUEST(RUNNING,
                  (THREAD_STATE_RUNNABLE == THIS_STATE(thridx)));
    STATE_END(rtrue);
}

/*!
 * @brief Activate the @b RUNNING thread state of a thread known
 * to be coming from a valid previous state.
 *
 */
rboolean threadstate_activate_running(jvm_thread_index thridx)
{
    STATE_ACTIVATE(RUNNING);
    STATE_END(rtrue);
}

/*!
 * @brief Process the @b RUNNING thread state.
 *
 * Run the next virtual instruction engine on the
 * current thread from where it left off last time.
 *
 * @attention Notice that the normal way to invoke
 *            opcode_run() is in this location, as
 *            driven by the JVM outer loop in jvm_run().
 *
 */
rboolean threadstate_process_running(jvm_thread_index thridx)
{
    STATE_PROCESS(RUNNING);

   STATE_END(opcode_run(CURRENT_THREAD, rtrue));
}


/*****************************************************************/

/*!
 * @brief Request the @b COMPLETE state.
 *
 * The @b COMPLETE state can be entered from @b NEW or @b RUNNING
 * or @b BADLOGIC.
 *
 * When entered from @b NEW, it is a so-called "stillborn" thread that
 * was created but never used.
 *
 * When entered from @b BADLOGIC, java.lang.Thread.destroy() or
 * java.lang.Thread.stop() was invoked, both of which are
 * dangerous, deprecated methods.
 *
 */
rboolean threadstate_request_complete(jvm_thread_index thridx)
{
    STATE_REQUEST(COMPLETE,
                  ((THREAD_STATE_NEW      == THIS_STATE(thridx)) ||
                   (THREAD_STATE_RUNNING  == THIS_STATE(thridx)) ||
                   (THREAD_STATE_BADLOGIC == THIS_STATE(thridx))));

    STATE_END(rtrue);
}

/*!
 * @brief Activate the @b COMPLETE thread state of a thread known
 * to be coming from a valid previous state.
 *
 */
rboolean threadstate_activate_complete(jvm_thread_index thridx)
{
    STATE_ACTIVATE(COMPLETE);
    STATE_END(rtrue);
}

/*!
 * @brief Process the @b COMPLETE thread state.
 *
 * <em>THIS IS A TRANSIENT STATE!</em>  A state change
 * from @b COMPLETE to @b DEAD is requested here.
 *
 */
rboolean threadstate_process_complete(jvm_thread_index thridx)
{
    STATE_PROCESS(COMPLETE);
    STATE_END(threadstate_request_dead(thridx));
}


/*****************************************************************/

/*!
 * @brief Request the @b BLOCKINGEVENT state.
 *
 * The @b BLOCKINGEVENT state can be entered from the
 * @b RUNNING or @b BADLOGIC states.  This can be caused by
 * java.lang.Thread.sleep() and java.lang.Thread.join() and
 * interruptible I/O operations.  Also, deprecated
 * java.lang.Thread.suspend() can go through @b BADLOGIC
 * to get here.
 *
 */
rboolean threadstate_request_blockingevent(jvm_thread_index thridx)
{
    STATE_REQUEST(BLOCKINGEVENT,
                  ((THREAD_STATE_RUNNING  == THIS_STATE(thridx)) ||
                   (THREAD_STATE_BADLOGIC == THIS_STATE(thridx))));
    STATE_END(rtrue);
}

/*!
 * @brief Activate the @b BLOCKINGEVENT thread state of a thread known
 * to be coming from a valid previous state.
 *
 */
rboolean threadstate_activate_blockingevent(jvm_thread_index thridx)
{
    STATE_ACTIVATE(BLOCKINGEVENT);
    STATE_END(rtrue);
}

/*!
 * @brief Process the @b BLOCKINGEVENT thread state.
 *
 * <em>THIS IS A TRANSIENT STATE!</em>  A state change
 * from @b BLOCKINGEVENT to @b BLOCKED is requested here.
 *
 */
rboolean threadstate_process_blockingevent(jvm_thread_index thridx)
{
    STATE_PROCESS(BLOCKINGEVENT);

    STATE_END(threadstate_request_blocked(thridx));
}

/*****************************************************************/

/*!
 * @brief Request the @b BLOCKED state.
 *
 * The @b BLOCKED state can only be entered from @b BLOCKINGEVENT.
 *
 */
rboolean threadstate_request_blocked(jvm_thread_index thridx)
{
    STATE_REQUEST(BLOCKED,
                  (THREAD_STATE_BLOCKINGEVENT == THIS_STATE(thridx)));
    STATE_END(rtrue);
}

/*!
 * @brief Activate the @b BLOCKED thread state of a thread known
 * to be coming from a valid previous state.
 *
 */
rboolean threadstate_activate_blocked(jvm_thread_index thridx)
{
    STATE_ACTIVATE(BLOCKED);
    STATE_END(rtrue);
}

/*!
 * @brief Process the @b BLOCKED thread state.
 *
 * The @b BLOCKED state idles until the @b UNBLOCKED state
 * is requsted by threadutil_update_blockingevents().
 *
 */
rboolean threadstate_process_blocked(jvm_thread_index thridx)
{
    STATE_PROCESS(BLOCKED);
    STATE_END(rtrue);
}


/*****************************************************************/

/*!
 * @brief Request the @b UNBLOCKED state.
 *
 * The @b UNBLOCKED state can only be entered from @b BLOCKED.
 *
 */
rboolean threadstate_request_unblocked(jvm_thread_index thridx)
{
    STATE_REQUEST(UNBLOCKED,
                  (THREAD_STATE_UNBLOCKED == THIS_STATE(thridx)));
    STATE_END(rtrue);
}

/*!
 * @brief Activate the @b UNBLOCKED thread state of a thread known
 * to be coming from a valid previous state.
 *
 */
rboolean threadstate_activate_unblocked(jvm_thread_index thridx)
{
    STATE_ACTIVATE(UNBLOCKED);
    STATE_END(rtrue);
}

/*!
 * @brief Process the @b UNBLOCKED thread state.
 *
 * <em>THIS IS A TRANSIENT STATE!</em>  A state change
 * from @b UNBLOCKED to @b RUNNABLE is requested here.
 *
 */
rboolean threadstate_process_unblocked(jvm_thread_index thridx)
{
    STATE_PROCESS(UNBLOCKED);

    STATE_END(threadstate_request_runnable(thridx));
}


/*****************************************************************/

/*!
 * @brief Request the @b SYNCHRONIZED state.
 *
 * The @b SYNCHRONIZED state can only be entered from @b RUNNING.
 *
 */
rboolean threadstate_request_synchronized(jvm_thread_index thridx)
{
    STATE_REQUEST(SYNCHRONIZED,
                  (THREAD_STATE_RUNNING == THIS_STATE(thridx)));
    STATE_END(rtrue);
}

/*!
 * @brief Activate the @b SYNCHRONIZED thread state of a thread known
 * to be coming from a valid previous state.
 *
 */
rboolean threadstate_activate_synchronized(jvm_thread_index thridx)
{
    STATE_ACTIVATE(SYNCHRONIZED);
    STATE_END(rtrue);
}

/*!
 * @brief Process the @b SYNCHRONIZED thread state.
 *
 * <em>THIS IS A TRANSIENT STATE!</em>  A state change
 * from @b SYNCHRONIZED to @b LOCK is requested here.
 *
 */
rboolean threadstate_process_synchronized(jvm_thread_index thridx)
{
    STATE_PROCESS(SYNCHRONIZED);

    STATE_END(threadstate_request_lock(thridx));
}


/*****************************************************************/

/*!
 * @brief Request the @b RELEASE state.
 *
 * The @b RELEASE state can only be entered from @b RUNNING.
 *
 */
rboolean threadstate_request_release(jvm_thread_index thridx)
{
    STATE_REQUEST(RELEASE,
                  (THREAD_STATE_RUNNING == THIS_STATE(thridx)));
    STATE_END(rtrue);
}

/*!
 * @brief Activate the @b RELEASE thread state of a thread known
 * to be coming from a valid previous state.
 *
 */
rboolean threadstate_activate_release(jvm_thread_index thridx)
{
    STATE_ACTIVATE(RELEASE);

    STATE_END(rtrue);
}

/*!
 * @brief Process the @b RELEASE thread state.
 *
 * <em>THIS IS A TRANSIENT STATE!</em>  A state change
 * from @b RELEASE to @b WAIT is requested here.
 * However, this will @e only occur if this thread
 * holds the lock on the @link #rthread.locktarget
   rthread.locktarget@endlink object.
 *
 */
rboolean threadstate_process_release(jvm_thread_index thridx)
{
    STATE_PROCESS(RELEASE);

    jvm_object_hash locktarget = THREAD(thridx).locktarget;

    /*
     * Check if this thread holds an object monitor lock.
     * If not, clear JOIN status and abort.
     */
    if (rfalse == threadutil_holds_lock(thridx, locktarget))
    {
        THREAD(thridx).status &= ~(THREAD_STATUS_JOIN4EVER |
                                   THREAD_STATUS_JOINTIMED);
        return(rfalse);
    }

    /*
     * Release lock and go to @b WAIT.
     *
     * But first, clear the monitor lock on the object.
     * However, DO NOT wipe the @link #rthread.locktarget
       locktarget@endlink object hash in the thread.
     * It will be needed when in the @b LOCK state to
     * @b ACQUIRE the lock again.
     */
    (rvoid) objectutil_unsynchronize(locktarget, thridx);

    STATE_END(threadstate_request_wait(thridx));
}


/*****************************************************************/

/*!
 * @brief Request the @b WAIT state.
 *
 * The @b WAIT state can only be entered from @b RELEASE.
 *
 */
rboolean threadstate_request_wait(jvm_thread_index thridx)
{
    STATE_REQUEST(WAIT, (THREAD_STATE_RELEASE == THIS_STATE(thridx)));

    STATE_END(rtrue);
}

/*!
 * @brief Activate the @b WAIT thread state of a thread known
 * to be coming from a valid previous state.
 *
 */
rboolean threadstate_activate_wait(jvm_thread_index thridx)
{
    STATE_ACTIVATE(WAIT);

    STATE_END(rtrue);
}

/*!
 * @brief Process the @b WAIT thread state.
 *
 * The @b WAIT state idles until the @b NOTIFY state
 * is requsted by threadutil_update_wait().
 *
 */
rboolean threadstate_process_wait(jvm_thread_index thridx)
{
    STATE_PROCESS(WAIT);

    STATE_END(rtrue);
}


/*****************************************************************/

/*!
 * @brief Request the @b NOTIFY state.
 *
 * The @b NOTIFY state can only be entered from @b WAIT.
 *
 */
rboolean threadstate_request_notify(jvm_thread_index thridx)
{
    STATE_REQUEST(NOTIFY, (THREAD_STATE_WAIT == THIS_STATE(thridx)));

    STATE_END(rtrue);
}

/*!
 * @brief Activate the @b NOTIFY thread state of a thread known
 * to be coming from a valid previous state.
 *
 */
rboolean threadstate_activate_notify(jvm_thread_index thridx)
{
    STATE_ACTIVATE(NOTIFY);

    STATE_END(rtrue);
}

/*!
 * @brief Process the @b NOTIFY thread state.
 *
 * <em>THIS IS A TRANSIENT STATE!</em>  A state change
 * from @b NOTIFY to @b LOCK is requested here.
 *
 */
rboolean threadstate_process_notify(jvm_thread_index thridx)
{
    STATE_PROCESS(NOTIFY);

    STATE_END(threadstate_request_lock(thridx));
}


/*****************************************************************/

/*!
 * @brief Request the @b LOCK state.
 *
 * The @b LOCK state can be entered from @b SYNCHRONIZED or @b NOTIFY.
 *
 */
rboolean threadstate_request_lock(jvm_thread_index thridx)
{
    STATE_REQUEST(LOCK,
                  ((THREAD_STATE_SYNCHRONIZED == THIS_STATE(thridx)) ||
                   (THREAD_STATE_NOTIFY       == THIS_STATE(thridx))));
    STATE_END(rtrue);
}

/*!
 * @brief Activate the @b LOCK thread state of a thread known
 * to be coming from a valid previous state.
 *
 */
rboolean threadstate_activate_lock(jvm_thread_index thridx)
{
    STATE_ACTIVATE(LOCK);

    STATE_END(rtrue);
}

/*!
 * @brief Process the @b LOCK thread state.
 *
 * <em>THIS IS THE PLACE WHERE THREADS COMPETE FOR AN OBJECT
 * MONITORY LOCK</em>.  If a thread is able to successfully
 * run objectutil_synchronize(), then it acquires the MLOCK
 * for that object and moves from the @b LOCK state forward
 * to the @b ACQUIRE state.
 *
 * The @b LOCK state idles until the @b ACQUIRE state
 * is requsted by threadutil_update_lock().
 *
 */
rboolean threadstate_process_lock(jvm_thread_index thridx)
{
    STATE_PROCESS(LOCK);

    /*
     * Attempt to acquire object's monitor lock (MLOCK),
     * first come first served.  Go to @b ACQUIRE state
     * if ownership was achieved.  If not achieved, stay
     * here in the @b LOCK state and keep asking.
     *
     * The objectutil_synchronize() call will fail until the
     * MLOCK bit is clear on this object, at which time the
     * lock is acquired by this thread.  At that time, the
     * thread can move forward to the @b ACQUIRE state.
     */
    if (rtrue == objectutil_synchronize(THREAD(thridx).locktarget,
                                        thridx))
    {
        /*
         * Should succeed since now in @b LOCK state, so return code
         * should always be @link #rtrue rtrue@endlink.
         */
        (rvoid) threadstate_request_acquire(thridx);
    }

    STATE_END(rtrue);
}


/*****************************************************************/

/*!
 * @brief Request the @b ACQUIRE state.
 *
 * The @b ACQUIRE state can only be entered from @b LOCK.
 *
 */
rboolean threadstate_request_acquire(jvm_thread_index thridx)
{
    STATE_REQUEST(ACQUIRE, (THREAD_STATE_LOCK == THIS_STATE(thridx)));

    STATE_END(rtrue);
}

/*!
 * @brief Activate the @b ACQUIRE thread state of a thread known
 * to be coming from a valid previous state.
 *
 */
rboolean threadstate_activate_acquire(jvm_thread_index thridx)
{
    STATE_ACTIVATE(ACQUIRE);

    STATE_END(rtrue);
}

/*!
 * @brief Process the @b ACQUIRE thread state.
 *
 * <em>THIS IS A TRANSIENT STATE!</em>  A state change
 * from @b ACQUIRE to @b RUNABLE is requested here.
 *
 */
rboolean threadstate_process_acquire(jvm_thread_index thridx)
{
    STATE_PROCESS(ACQUIRE);

    STATE_END(threadstate_request_runnable(thridx));
}


/*****************************************************************/

/*!
 * @brief Request the @b DEAD state.
 *
 * The @b DEAD state can only be entered from @b COMPLETE.
 *
 */
rboolean threadstate_request_dead(jvm_thread_index thridx)
{
    STATE_REQUEST(DEAD, (THREAD_STATE_COMPLETE == THIS_STATE(thridx)));

    STATE_END(rtrue);
}

/*!
 * @brief Activate the @b DEAD thread state of a thread known
 * to be coming from a valid previous state.
 *
 */
rboolean threadstate_activate_dead(jvm_thread_index thridx)
{
    STATE_ACTIVATE(DEAD);

    STATE_END(rtrue);
}

/*!
 * @brief Process the @b DEAD thread state.
 *
 * Bogus, yet true:  <em>THIS IS A TRANSIENT STATE!</em>  the usual
 * message about this state applies, yet the target state is
 * the "not used" condition, so it may also be considered
 * inapplicable.  Here 'tis:
 *
 *     <em>THIS IS A TRANSIENT STATE!</em>  A state change
 *     from @b DEAD to <b>"not used"</b> is requested here.
 *
 * So how is this statement true?  Because thread_die()
 * is called once the thread enters this condition, removing
 * it from the array of active thread table entries.
 *
 */
rboolean threadstate_process_dead(jvm_thread_index thridx)
{
    STATE_PROCESS(DEAD);

    STATE_END(thread_die(thridx));
}


/*****************************************************************/

/*!
 * @brief Request the @b BADLOGIC state.
 *
 * The @b BADLOGIC state can be entered from @e ANY state.
 * It is primarily a diagnostic state, but may be used
 * to gracefully handle java.lang.Thread.destroy() and
 * java.lang.Thread.stop() and java.lang.Thread.suspend()
 * and java.lang.Thread.resume().
 *
 */
rboolean threadstate_request_badlogic(jvm_thread_index thridx)
{
    STATE_REQUEST(BADLOGIC, (rtrue));

    STATE_END(rtrue);
}

/*!
 * @brief Activate the @b BADLOGIC thread state of a thread known
 * to be coming from a valid previous state.
 *
 */
rboolean threadstate_activate_badlogic(jvm_thread_index thridx)
{
    STATE_ACTIVATE(BADLOGIC);

    STATE_END(rtrue);
}

/*!
 * @brief Process the @b BADLOGIC thread state.
 *
 * The @b BADLOGIC state idles until another state is requested.
 * An "Idle forever" error message might be printed by code that
 * invoked this state.  If desired, this state can be moved
 * forward to @b COMPLETE or @b BLOCKINGEVENT.  Part of the
 * request logic for these states is to permit entrance
 * from the @b BADLOGIC state.
 *
 */
rboolean threadstate_process_badlogic(jvm_thread_index thridx)
{
    STATE_PROCESS(BADLOGIC);

    STATE_END(rtrue);
}

/*@} */ /* End of grouped definitions */


/* EOF */
