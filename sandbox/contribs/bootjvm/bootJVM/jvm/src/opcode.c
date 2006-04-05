/*!
 * @file opcode.c
 *
 * @brief Java Virtual Machine inner loop virtual instruction execution.
 *
 * A specific group of exceptions that may be thrown by JVM execution
 * (spec section 2.16.4 unless specified otherwise) are not typically
 * checked by the Java program and stop thread execution unless caught
 * by a Java @c @b catch block.
 *
 * <ul>
 * <li>
 * @b ArithmeticException    Integer divide by zero
 * </li><li>
 * @b ArrayStoreException    Storage compatibility error between
 *                           array type lvalue vs component rvalue
 * </li><li>
 * @b ClassCastException     Type narrowing loses significance or
 *                           casting of an object to a type that
 *                           is not valid.
 * </li><li>
 * @b IllegalMonitorStateException  Thread attempted to wait() or
 *                           notify()  on an object that it has
 *                           not locked.
 * </li><li>
 * @b IndexOutOfBoundsException An index or a subrange was outside the
 *                           limits \>=0 unto \< lenghth/size for
 *                           an array, string, or vector.
 * </li><li>
 * @b NegativeArraySizeException Attempted to create an array with a
 *                           negative number of elements.
 * 
 * </li><li>
 * @b NullPointerException   An object reference to a
 *                           @link #jvm_object_hash_null
                             jvm_object_hash_null@endlink object
 *                           was attempted instead of to a real
 *                           and valid object.
 *
 * </li><li>
 * @b SecurityException      Violation of security policy.
 * </li>
 * </ul>
 *
 *
 *
 * A suite of errors may also be thrown that Java programs normally
 * to not attempt to @c @b catch and which terminate JVM
 * execution:
 *
 * <ul>
 * <li>
 * @link #JVMCLASS_JAVA_LANG_LINKAGEERROR LinkageError@endlink Loading,
 *                    linking, or initialization error
 *                    (2.17.2, 2.17.3, 2.17.4).  May also be
 *                    thrown at run time.
 * </li>
 *
 * <li>
 * Loading Errors (2.17.2):
 * <ul>
 *     <li>
 *     @b ClassFormatError        Binary data is not a valid class file.
 *     </li><li>
 *     @b ClassCircularityError   Class hierarchy eventually references
 *                                itself.
 *     </li><li>
 *     @b NoClassDefFoundError    Class cannot be found by loader.
 *     </li><li>
 *     @b UnsupportedClassVersionError JVM does not support this version
 *                                of the class file specification.
 *     </li>
 * </ul>
 * </li>
 *
 * <li>
 * Linking Errors (2.17.3) (subclass of @link
   #JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR 
   IncompatibleClassChangeError@endlink):
 * <ul>
 *     <li>
 *     @b NosuchFieldError      Attempt to reference non-existent field
 *     </li><li>
 *     @b NoSuchMethodError     Attempt to reference non-existent method
 *     </li><li>
 *     @b InstantiationError    Attempt to instantiate abstract class
 *     </li><li>
 *     @b IllegalAccessError    Attempt to reference a field or method
 *                              that is not in scope (typically not
 *                              not @c @b public ).
 *     </li>
 * </ul>
 * </li>
 *
 * <li>
 * Verification Errors (2.17.3):
 * <ul>
 *     <li>
 *     @b VerifyError    Class fails integrity checks of the verifier.
 *     </li>
 * </ul>
 * </li>
 *
 * <li>
 * Initialization Errors (2.17.4):
 * <ul>
 *     <li>
 *     @b ExceptionInInitializerError   A static initializer of a static
 *                                   field initializer threw something
 *                                   that was neither a
 *                                   @c @b java.lang.Error
 *                                   or its subclass.
 *     </li>
 * </ul>
 * </li>
 *
 * <li>
 * Run-time instances of @link #JVMCLASS_JAVA_LANG_LINKAGEERROR
   LinkageError@endlink:
 * <ul>
 *     <li>
 *     @b AbstractMethodError    Invocation of an abstract method.
 *     </li><li>
 *     @b UnsatisfiedLinkError   Cannot load native method (shared obj).
 *     </li>
 * </ul>
 * </li>
 *
 * <li>
 * Resource limitations, via subclass of @link
   #JVMCLASS_JAVA_LANG_VIRTUALMACHINEERROR VirtualMachineError@endlink:
 * <ul>
 *     <li>
 *     @b InternalError         JVM software or host software/hardware
 *                              (may occur asynchronously, any time)
 *     </li><li>
 *     @b OutOfMemoryError      JVM cannot get enough memory for
 *                              request, even after GC/mmgmt.
 *     </li><li>
 *     @b StackOverflowError    Out of JVM stack space, typically due
 *                              to infinite recursion on a method.
 *     </li><li>
 *     @b UnknownError          JVM cannot determine the actual cause.
 *     </li>
 * </ul>
 * </li>
 * </ul>
 *
 *
 * @todo HARMONY-6-jvm-opcode.c-1 The code fragment macros used by
 *       the opcode switch in @link #opcode_run() opcode_run()@endlink
 *       need to have the local variables documented as to which as
 *       required upon macro startup and which are set for use at
 *       macro completion.
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
ARCH_SOURCE_COPYRIGHT_APACHE(opcode, c,
"$URL$",
"$Id$");

#define PORTABLE_JMP_BUF_VISIBLE
#include "jvmcfg.h"
#include "cfmacros.h"
#include "classfile.h"
#include "exit.h"
#include "gc.h" 
#include "jvm.h"
#include "jvmclass.h"
#include "linkage.h"
#include "method.h"
#include "native.h"
#define I_AM_OPCODE_C
#include "opcode.h"
#include "opmacros.h"
#include "utf.h"
#include "util.h"


/*!
 * @brief JVM inner loop setup for end of thread detection-- implements
 * @c @b setjmp(3) .
 *
 *
 * Use this function to arm handler for non-local exit from the
 * inner @c @b while() loop when a thread has finished
 * running.
 *
 * @param penv  Non-local return buffer for use by @c @b setjmp(3) call.
 *
 *
 * @returns From normal setup, integer
 *          @link #EXIT_MAIN_OKAY EXIT_MAIN_OKAY@endlink.
 *          Otherwise, return
 *          @link #exit_code_enum exit_enum code enumeration@endlink
 *          from @link #opcode_end_thread_test()
            opcode_end_thread_test()@endlink.
 *
 * @attention See comments in @link jvm/src/portable_jmp_buf.c
              portable_jmp_buf.c@endlink as to why this @e cannot
 *            be a function call.
 *
 */

#define OPCODE_END_THREAD_SETUP(penv) PORTABLE_SETJMP(penv)


/*!
 * @brief Detect end of thread when stack frame is empty at
 * @b return time and perform non-local return-- implements
 * @c @b longjmp(3) .
 *
 * Use this function to test for end of thread execution and to
 * perform a non-local return from the inner @c @b while()
 * loop when a thread has finished running.
 *
 * This function is invoked from within each of the Java
 * @b return family of opcodes to detect if the stack frame is
 * empty after a @link #POP_FRAME() POP_FRAME()@endlink.  If this
 * test passes, then the stack frame is empty and the thread has
 * nothing else to do.  It is therefore moved into the @b COMPLETE
 * state and a non-local return exits the while loop.
 *
 *
 * @param thridx  Thread index of thread to evaluate
 *
 * @param penv    Non-local return buffer for use by @c @b longjmp(3)
 *                call.
 *
 *
 * @returns @link #rvoid rvoid@endlink if end of thread test fails.
 *          If test passes, perform non-local state restoration from
 *          setup via @c @b setjmp(3) as stored in a local variable
 *          in @link #opcode_run() opcode_run()@endlink.
 *
 */

static rvoid opcode_end_thread_test(jvm_thread_index thridx,
                                    portable_jmp_buf *penv)
{
    ARCH_FUNCTION_NAME(opcode_end_thread_test);

    /* Check if thread has finished running */

    /*!
     * Check both current end of program FP and final FP in case
     * something like a @c @b \<clinit\> or @c @b \<init\> was loaded
     * on top of a running program.
     *
     * @todo HARMONY-6-jvm-opcode.c-2 Should FP condition be
     *       fp_end_program <= THREAD().fp instead of < condition? 
     *
     */
    if (THREAD(thridx).fp_end_program < THREAD(thridx).fp)
    {
        /* Thread is still running something, so continue */
        return;
    }

    /* Thread has finished running, so return to @c @b setjmp(3) */

    int rc = (int) EXIT_JVM_THREAD;

    PORTABLE_LONGJMP(penv, rc);
                                     
/*NOTREACHED*/

} /* END of opcode_end_thread_test() */


/*!
 * @brief Double-fault error state variable for throwable event.
 *
 * This boolean reports the the error-within-an-error state condition
 * within
 * @link #rvoid opcode_load_run_throwable()
   opcode_load_run_throwable()@endlink
 */
rboolean opcode_calling_java_lang_linkageerror =
    CHEAT_AND_USE_FALSE_TO_INITIALIZE;


/*!
 * @brief Load a @c @b java.lang.Throwable event, typically
 * an @b Error or @b Exception and run its @c @b \<clinit\> method
 * followed by its @c @b \<init\> method with default parameters.
 *
 * This function must @e not be called until
 * @c @b java.lang.Object , @c @b java.lang.Class ,
 * @c @b java.lang.String , @c @b java.lang.Throwable ,
 * and @c @b java.lang.Error have been loaded and initialized.
 *
 * There is @e no attempt to enforce which classes may be invoked
 * by this handler.  It is assumed that the caller will @e only
 * pass in subclasses of
 * @link #JVMCLASS_JAVA_LANG_ERROR JVMCLASS_JAVA_LANG_ERROR@endlink.
 * Anything else will produce undefined results.
 *
 * @warning <b><em>This handler is not a simple as it seems!</em></b>
 *          You absolutely @e must know what the non-local return
 *          mechanism @c @b setjmp(3)/longjmp(3) is before attempting
 *          to figure it out!!!
 *
 *          The strategy is a simple one:  Trap thrown errors by this
 *          handler and trap a failure in that error class by throwing
 *          a @link #JVMCLASS_JAVA_LANG_INTERNALERROR
            JVMCLASS_JAVA_LANG_INTERNALERROR@endlink.  If that fails,
 *          give up.
 *
 *          @b Details:  When this function is first called due to a
 *          thrown error, it attempts to load and run that error class.
 *          If all is well, that class runs and everyone lives happily
 *          ever after.  If that class throws an error, however, this
 *          handler, having been re-armed, is activated semi-recursively
 *          via @link #exit_throw_exception()
            exit_throw_exception()@endlink, (that is, not entering at
 *          the top of function, but at the return from
 *          @link #exit_throw_exception() exit_throw_exception()@endlink
 *          with a non-zero return value), entering @e this code at
 *          @link #exit_exception_setup exit_exception_setup()@endlink,
 *          choosing the conditional branch != @link #EXIT_MAIN_OKAY
            EXIT_MAIN_OKAY@endlink and attempts to recursively load and
 *          run @link #JVMCLASS_JAVA_LANG_LINKAGEERROR
            JVMCLASS_JAVA_LANG_LINKAGEERROR@endlink.  If this is
 *          successful, fine, call @link #exit_jvm() exit_jvm()@endlink
 *          and be done.  However, if even @e this fails and throws an
 *          error, the handler, having been rearmed @e again by the
 *          attempt to invoke @link #JVMCLASS_JAVA_LANG_LINKAGEERROR
            JVMCLASS_JAVA_LANG_LINKAGEERROR@endlink, it again
 *          semi-recursively is activated via
 *          @link #exit_throw_exception() exit_throw_exception()@endlink
 *          and again enters the code at @link #exit_exception_setup()
            exit_exception_setup()@endlink.  This time, the global
 *          @link #opcode_calling_java_lang_linkageerror
            opcode_calling_java_lang_linkageerror@endlink is
 *          @link #rtrue rtrue@endlink, so no more recursive invocations
 *          are performed.  Instead, @link #exit_jvm()
            exit_jvm()@endlink with the most recent
 *          @link #EXIT_MAIN_OKAY EXIT_xxx@endlink code from
 *          @link #exit_throw_exception() exit_throw_exception()@endlink
 *          and be done.
 *
 *
 * @param  pThrowableEvent  Null-terminated string name of
 *                          throwable class.
 *
 * @param  thridx           Thread table index of thread to load this
 *                          @c @b java.lang.Throwable sub-class
 *                          into.
 *
 *
 * @returns @link #rvoid rvoid@endlink.  Either the
 *          @c @b java.lang.Throwable class loads and
 *          runs, or it loads @c @b java.lang.LinkageError
 *          and runs it, then returns to caller, or it exits
 *          due to an error somewhere in this sequence of
 *          events.
 *
 */

rvoid opcode_load_run_throwable(rchar            *pThrowableEvent,
                                jvm_thread_index  thridx)
{
    ARCH_FUNCTION_NAME(opcode_load_run_throwable);

    /******* Re-arm java.lang.LinkageError handler ***/


    /*!
     * @internal This call to exit_exception_setup() and the following
     *           if (@link #EXIT_MAIN_OKAY EXIT_MAIN_OKAY@endlink)
     *           statement constitute the ugly part of this code as
     *           described above. See also other recursive calls and
     *           their control via @link
                 #opcode_calling_java_lang_linkageerror
                 opcode_calling_java_lang_linkageerror@endlink:
     *
     */
                      exit_exception_setup();
    int nonlocal_rc = EXIT_EXCEPTION_SETUP();

    if (EXIT_MAIN_OKAY != nonlocal_rc)
    {
        /*!
         * @todo  HARMONY-6-jvm-opcode.c-3 Make this load and run
         *        the error class @c @b \<clinit\> and default
         *        @c @b \<init\> method instead of/in addition to
         *        fprintf().  Other exit_throw_exception() handlers
         *        will have invoked this method, so it @e must be
         *        rearmed @e again at this point, lest an error
         *        that invokes it causes an infinite loop.
         */

        /* Should never be true via exit_throw_exception() */
        if (rnull == exit_LinkageError_subclass)
        {
            exit_LinkageError_subclass = "unknown";
        }

        fprintfLocalStderr(
            "opcode_load_run_throwable:  Recursive Error %d (%s): %s\n",
            nonlocal_rc,
            exit_get_name(nonlocal_rc),
            exit_LinkageError_subclass);

        jvmutil_print_stack(thridx, (rchar *) rnull);

        /*
         * WARNING!!! Recursive call, but will only go 1 level deep.
         */
        if (rfalse == opcode_calling_java_lang_linkageerror)
        {
            opcode_calling_java_lang_linkageerror = rtrue;

            opcode_load_run_throwable(JVMCLASS_JAVA_LANG_LINKAGEERROR,
                                      rtrue);

            opcode_calling_java_lang_linkageerror = rfalse;
        }

        exit_jvm(nonlocal_rc);
/*NOTREACHED*/
    }

    if (jvm_thread_index_null == thridx)
    {
        sysErrMsg(arch_function_name,
             "Invalid thread index %d for throwable class %s",
                thridx, pThrowableEvent);
        return;
    }

    /*!
     * @internal Load error class and run its @c @b \<clinit\> method.
     *
     * If an error is thrown by class_load_resolve_clinit(),
     * re-enter this error function recursively at
     * exit_exception_setup().
     */
    jvm_class_index clsidx =
        class_load_resolve_clinit(pThrowableEvent,
                                  thridx,
                                  rfalse, /* N/A due to valid thridx */
                                  rfalse);

    /*!
     * @internal Both mark (here) and unmark (below) class
     *           so it gets garbage collected.
     */
    (rvoid) GC_CLASS_MKREF_FROM_CLASS(jvm_class_index_null, clsidx);


    /*!
     * @internal Instantiate error class object and run its default
     *           @c @b \<init\> method with default parameters.
     *
     * If an error is thrown in object_instance_new(), re-enter this
     * error function recursively at exit_exception_setup().
     */
    jvm_object_hash objhash=
        object_instance_new(THREAD_STATUS_EMPTY,
                            CLASS_OBJECT_LINKAGE(clsidx)->pcfs,
                            clsidx,
                            LOCAL_CONSTANT_NO_ARRAY_DIMS,
                            (jint *) rnull,
                            rtrue,
                            thridx,
                            (CONSTANT_Utf8_info *) rnull);

    /*!
     * @internal Both mark and unmark object so it
     *           gets garbage collected
     */
    (rvoid) GC_OBJECT_MKREF_FROM_OBJECT(jvm_object_hash_null, objhash);
    (rvoid) GC_OBJECT_RMREF_FROM_OBJECT(jvm_object_hash_null, objhash);

    /*!
     * @internal Unmarked from above-- since JVM is going down,
     *           this may be irrelevant, but be consistent.
     */
    (rvoid) GC_CLASS_RMREF_FROM_CLASS(jvm_class_index_null, clsidx);

    return;

} /* END of opcode_load_run_throwable() */


/*!
 * @brief Inner loop of JVM virtual instruction execution engine.
 *
 * Only run the inner loop until:
 *
 * <ul>
 * <li> thread state changes </li>
 * <li> time slice expired </li>
 * <li> thread completes (when FP is not 0, that is,
 *      not @link #JVMCFG_NULL_SP JVMCFG_NULL_SP@endlink) </li>
 * </ul>
 *
 * Any remaining time on this time slice will go against
 * the next thread, which may only have a small amount
 * of time, even none at all.  This is a natural effect
 * of any time-slicing algorithm.
 *
 * Logic similar to the uncaught exception handler of this function
 * may be found in object_run_method() as far as initiating execution
 * of a JVM method.
 *
 * @todo  HARMONY-6-jvm-opcode.c-4 See if there is a better
 *        time-slicing algorithm that is just as easy to use
 *        and keeps good real clock time.
 * 
 * @todo:  HARMONY-6-jvm-opcode.c-5 having @c @b run_init_ (parm 6)
 *         for invocations of object_instance_new() to be
 *         @link #rfalse rfalse@endlink the right thing to
 *         do for array initialization, namely opcodes @b NEWARRAY
 *         and @b ANEWARRAY and @b MULTINEWARRAY ?  Initializing an
 *         array is really not a constructor type
 *         of operation, but the individual components
 *         (elements) of the array probably would be,
 *         and with default parameters.
 *
 *
 *
 * @param  thridx            Thread index of thread to run
 *
 * @param  check_timeslice   @link #rtrue rtrue@endlink if JVM time
 *                           slice preempts execution after maximum
 *                           time exceeded.
 *
 *
 * @returns @link #rtrue rtrue@endlink if this method and/or time slice
 *          ran correctly (whether or not the thread finished running),
 *          or @link #rfalse rfalse@endlink if an
 *          uncaught exception was thrown or if an
 *          Error, Exception, or Throwable was thrown, or if a
 *          thread state could not be properly changed.
 *
 */
rboolean opcode_run(jvm_thread_index thridx,
                    rboolean check_timeslice)
{
    ARCH_FUNCTION_NAME(opcode_run);

    /*!
     * @internal Handler linkage for end of thread detection.
     *           This structure is @e horrible.  It is @e ugly.  See
     *       @link jvm/src/portable_jmp_buf.c portable_jmp_buf.c@endlink
     *           for other hazards and rationale for workaround.
     *
     */
    portable_jmp_buf opcode_end_thread_nonlocal_return;


    /*
     * Arm handler for the three conditions
     * java.lang.Error, Exception, and Throwable.
     *
     * Any JVM virtual execution that throws one of
     * these that is @e not covered by an exception
     * handler in the class will issue:
     *
     *     <b><code>
PORTABLE_LONGJMP(THREAD(thridx).pportable_nonlocal_ThrowableEvent, rc)
           </b></code>
     *
     * by way of @link #thread_throw_exception() 
       thread_throw_exception@endlink, which will return to the
     * @c @b else branch of this @c @b if .  It will
     * contain a @link rthread#status rthread.status@endlink bit
     * @b THREAD_STATUS_xxx which may be examined there.  Notice that
     * @c @b int is wider than @c @b rushort and thus
     * will not lose any information in the implicit conversion.
     */

    /* Inner loop end of thread detection, init to unused value */
    int nonlocal_thread_return = EXIT_JVM_INTERNAL;

    /* Calls @c @b setjmp(3) to arm handler */
                      thread_exception_setup(thridx);
    int nonlocal_rc = THREAD_EXCEPTION_SETUP(thridx);

    /* Show error case first due to @e long switch() following */
    if (THREAD_STATUS_EMPTY != nonlocal_rc)
    {
        /*
         * Examine only the @c @b longjmp(3) conditions (should be
         * irrelevant due to filter in @link #thread_throw_exception()
           thread_throw_exception()@endlink)
         */
        nonlocal_rc &= (THREAD_STATUS_THREW_EXCEPTION |
                        THREAD_STATUS_THREW_ERROR |
                        THREAD_STATUS_THREW_THROWABLE |
                        THREAD_STATUS_THREW_UNCAUGHT);

        /*
         * Local copy of @link rthread#pThrowable pThrowable@endlink
         * for use below
         */
        rchar *pThrowableEvent = THREAD(thridx).pThrowableEvent;

        /*
         * Clear out the Throwable condition now that it
         * is being processed, in case of multiple exceptions.
         */
        THREAD(thridx).pThrowableEvent = (rchar *) rnull;
        THREAD(thridx).status &= ~(THREAD_STATUS_THREW_EXCEPTION |
                                   THREAD_STATUS_THREW_ERROR |
                                   THREAD_STATUS_THREW_THROWABLE |
                                   THREAD_STATUS_THREW_UNCAUGHT);

        /*
         * Process the specifics for each java.lang.Throwable
         * condition before doing the generic processing.
         */
        if (THREAD_STATUS_THREW_EXCEPTION & nonlocal_rc)
        {
            /*!
             * @todo  HARMONY-6-jvm-opcode.c-6 What needs to go here?
             */
        }
        else
        if (THREAD_STATUS_THREW_ERROR & nonlocal_rc)
        {
            /*!
             * @todo  HARMONY-6-jvm-opcode.c-7 What needs to go here?
             */
        }
        else
        if (THREAD_STATUS_THREW_THROWABLE & nonlocal_rc)
        {
            /*!
             * @todo  HARMONY-6-jvm-opcode.c-8 What needs to go here?
             */
        }
        else
        if (THREAD_STATUS_THREW_UNCAUGHT & nonlocal_rc)
        {
            /*
             * Handle an uncaught exception.  @b pThrowableEvent will
             * be @link #rnull rnull@endlink here, so there is nothing
             * to get from it.
             */

            jvm_class_index clsidx = class_find_by_prchar(
                                        JVMCLASS_JAVA_LANG_THREADGROUP);

            if (jvm_class_index_null == clsidx)
            {
                /* Problem creating error class, so quit */
                sysErrMsg(arch_function_name,
                     "Cannot find class %s",
                     JVMCLASS_JAVA_LANG_THREADGROUP);

                jvmutil_print_stack(thridx, (rchar *) rnull);

                exit_jvm(EXIT_JVM_CLASS);
/*NOTREACHED*/
            }

            /*!
             * @todo HARMONY-6-jvm-opcode.c-9 Get @c @b ThreadGroup
             *       logic working that figures out which
             *       @c @b java.lang.ThreadGroup this thread is
             *       a part of and invoke
             *       @c @b java.lang.ThreadGroup.uncaughtException()
             *       for that specific object instead of this general
             *       method. Probably the class library will gripe
             *       about not knowing which object to associate with
             *       the method call since
             *       @c @b java.lang.ThreadGroup.uncaughtException()
             *       is @e not a @c @b static method.
             */

            /*
             * Set FP lower boundary so Java @c @b RETURN
             * instruction does not keep going after handler, check if
             * @c @b java.lang.ThreadGroup.uncaughtException()
             * is there, and run it.
             */
            jvm_sp fp_save_end_program = THREAD(thridx).fp_end_program;

            /*
             * Make JVM stop once
             * @c @b java.lang.ThreadGroup.uncaughtException()
             * is done
             */
            THREAD(thridx).fp_end_program = THREAD(thridx).fp;

            /* Continue getting pieces for PUT_PC_IMMEDIATE() */
            jvm_method_index mthidx =
                method_find_by_prchar(clsidx,
                                      JVMCFG_UNCAUGHT_EXCEPTION_METHOD,
                                      JVMCFG_UNCAUGHT_EXCEPTION_PARMS);

            if (jvm_method_index_bad == mthidx)
            {
                exit_throw_exception(EXIT_JVM_METHOD,
                                  JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR);
/*NOTREACHED*/
            }

            /*
             * Load up entry point for Throwable call
             */
            ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

            method_info *pmthidx = pcfs->methods[mthidx];
            jvm_attribute_index codeatridx =
                pmthidx->LOCAL_method_binding.codeatridxJVM;

            if (jvm_attribute_index_bad == codeatridx)
            {
                exit_throw_exception(EXIT_JVM_ATTRIBUTE,
                                  JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR);
/*NOTREACHED*/
            }

            if (jvm_attribute_index_native == codeatridx)
            {
                /* Pass parameters for both local method and JNI call */
                native_run_method(thridx,
                                  clsidx,
                                  pmthidx->LOCAL_method_binding
                                            .nmordJVM,
                                  pmthidx->name_index,
                                  pmthidx->descriptor_index,
                                  pmthidx->access_flags,
                               method_implied_opcode_from_cp_entry_pcfs(
                                      pcfs,
                                      pmthidx->name_index,
                                      pmthidx->access_flags),
                                  IS_INIT_METHOD(pcfs,
                                                 pmthidx->name_index));
            }
            else
            {
                Code_attribute *pca = (Code_attribute *)
                    &pmthidx->attributes[codeatridx];
                PUSH_FRAME(thridx, pca->max_locals);
                PUT_PC_IMMEDIATE(thridx,
                                 clsidx,
                                 mthidx,
                                 pmthidx
                                   ->LOCAL_method_binding.codeatridxJVM,
                                 pmthidx
                                   ->LOCAL_method_binding.excpatridxJVM,
                                 CODE_CONSTRAINT_START_PC);

                if (rfalse == opcode_run(thridx, rfalse))
                {
                    /* Problem running error class, so quit */
                    sysErrMsg(arch_function_name,
                              "Cannot run exception method %s %s%s",
                              JVMCLASS_JAVA_LANG_THREADGROUP,
                              JVMCFG_UNCAUGHT_EXCEPTION_METHOD,
                              JVMCFG_UNCAUGHT_EXCEPTION_PARMS);

                    jvmutil_print_stack(thridx, (rchar *) rnull);

                    exit_jvm(EXIT_JVM_THREAD);
/*NOTREACHED*/
                }
            }

            /*
             * This would normally permit java.lang.Exception 
             * and java.lang.Throwable to continue by restoring
             * lower FP boundary, but unwind here anyway for
             * proper frame contents for later diagnostics.
             */
            THREAD(thridx).fp_end_program = fp_save_end_program;

            /* Attempt to shut down thread due to condition */
            if (rfalse == threadstate_request_complete(thridx))
            {
                sysErrMsg(arch_function_name,
                 "Unable to move completed thread %d to '%s' state",
                          thridx,
                          thread_state_get_name(THREAD_STATE_COMPLETE));
                THREAD_REQUEST_NEXT_STATE(badlogic, thridx);

                return(rfalse);
            }

            return(rfalse);

        } /* if THREAD_STATUS_THREW_UNCAUGHT */
        else
        {
            /*!
             * @todo  HARMONY-6-jvm-opcode.c-10 What needs to go here,
             *        if anything?
             */
        }

        /* Completely handled THREAD_STATUS_THREW_UNCAUGHT above */
        if (nonlocal_rc & (THREAD_STATUS_THREW_EXCEPTION |
                           THREAD_STATUS_THREW_ERROR |
                           THREAD_STATUS_THREW_THROWABLE))
        {
            /*
             * Utilizing the current contents of the @c @b longjmp(3)
             * condition found in @b pThrowableEvent, which will
             * have been set when one of the status bits was set--
             * see @link jvm/src/threadutil.c threadutil.c@endlink
             * for several examples.
             *
             * When loading the error class, process its
             * @c @b \<clinit\> on any available thread, but process
             * its @c @b \<init\> on @e this thread so thread will be
             * done running after it has been processed (due to FP
             * change).
             */
            opcode_load_run_throwable(pThrowableEvent, thridx);

            /*
             * All conditions except java.lang.Exception should kill
             * the thread.
             *
             */
            if (!(THREAD_STATUS_THREW_EXCEPTION & nonlocal_rc))
            {
                /* Attempt to shut down thread due to code completion */
                if (rfalse == threadstate_request_complete(thridx))
                {
                    sysErrMsg(arch_function_name,
                     "Unable to move aborted thread %d to '%s' state",
                              thridx,
                              thread_state_get_name(
                                                THREAD_STATE_COMPLETE));
                    THREAD_REQUEST_NEXT_STATE(badlogic, thridx);

                    return(rfalse);
                }
            }

        } /* if nonlocal_rc */

    } /* if nonlocal_rc */
    else
    {
        /**************************************************************/
        /**************************************************************/
        /**************************************************************/
        /**************************************************************/
        /**************************************************************/
        /* BEGIN GIANT SWITCH STATEMENT if(){}else{while(){switch()}} */
        /**************************************************************/
        /**************************************************************/
        /**************************************************************/
        /**************************************************************/
        /**************************************************************/


        /*
         * Run inner JVM execution loop.
         */

        /*
         * Scratch area for operating the inner loop
         * and its key associations
         */
        jvm_pc             *pc    = THIS_PC(thridx);

        /* Load new class file context, incl. method context for PC */
        ClassFile          *pcfs;
        jvm_virtual_opcode *pcode;
        LOAD_METHOD_CONTEXT;

        /* Scratch area for operand resolution */
        rboolean           iswide;
        iswide = rfalse;

        jvm_virtual_opcode opcode;
        u1                 op1u1;   /* Operand 1 as a (u1) */
        u2                 op1u2;   /* Operand 1 as a (u2) */
        u4                 op1u4;   /* Operand 1 as a (u4) */
        u1                 op2u1;   /* Operand 2 as a (u1) */
        u1                 op3u1;   /* Operand 3 as a (u1) */
        rboolean           rbool1;  /* Conditional instruction status */

        u4                *pu4;     /* Operand as a (u4 *) */

        jbyte              jbtmp;   /* Opcode (jbyte) scratch area */
        jchar              jctmp;   /* Opcode (jchar) scratch area */
        jshort             jstmp;   /* Opcode (jshort) scratch area */
        jint               jitmp1;  /* Opcode (jint) scratch area 1 */
        jint               jitmp2;  /* Opcode (jint) scratch area 2 */
        jint               jitmp3;  /* Opcode (jint) scratch area 3 */
        jint               jitmp4;  /* Opcode (jint) scratch area 4 */
        jint               jitmp5;  /* Opcode (jint) scratch area 5 */
        jint              *pjitmp6; /* Opcode (jint) scratch area 6 */
        jvm_pc_offset      jptmp;   /* Opcode (jvm_pc_offset) scratch */
        jlong              jltmp1;  /* Opcode (jlong) scratch area 1 */
        jlong              jltmp2;  /* Opcode (jlong) scratch area 2 */
        jfloat             jftmp1;  /* Opcode (jfloat) scratch area 1 */
        jfloat             jftmp2;  /* Opcode (jfloat) scratch area 2 */
        jdouble            jdtmp1;  /* Opcode (jdouble) scratch area 1*/
        jdouble            jdtmp2;  /* Opcode (jdouble) scratch area 2*/
        jvm_object_hash    jotmp1;  /* Opcode (jobject) scratch area 1*/
        jvm_object_hash    jotmp2;  /* Opcode (jobject) scratch area 2*/


        /* Scratch area for Fieldref and Methodref navigation */
        cp_info_dup               *pcpd;
        CONSTANT_Class_info       *pcpd_Class;
        CONSTANT_Fieldref_info    *pcpd_Fieldref;
        CONSTANT_Methodref_info   *pcpd_Methodref;
        CONSTANT_Utf8_info        *pcpd_Utf8;

        field_info              *pfld;
        method_info             *pmth;
        Code_attribute          *pca;

        ClassFile              *pcfsmisc;
        jvm_class_index         clsidxmisc;
        jvm_class_index         clsidxmisc2;
        jvm_method_index        mthidxmisc;
        jvm_object_hash         objhashmisc;
        jvm_field_lookup_index  fluidxmisc;
        rchar                  *prchar_clsname;
        rushort                 special_obj_misc;


        /* Calls @c @b setjmp(3) to arm handler */

        nonlocal_thread_return = OPCODE_END_THREAD_SETUP(
                                    &opcode_end_thread_nonlocal_return);

        /* Show error case first due to @e long switch() following */
        if (EXIT_MAIN_OKAY != nonlocal_thread_return)
        {
            ; /* Nothing to do since this is not an error. */
        }
        else
        {

            /*!
             * @internal For best runtime efficiency, place tests in
             *           order of most to least frequent occurrence.
             */

            while ( /* This thread is in the RUNNING state */
                   (THREAD_STATE_RUNNING == THREAD(thridx).this_state)&&

                   /* Time slice is still running or is N/A */
                   ((rfalse == check_timeslice) || /* or if true and */
                    (rfalse == pjvm->timeslice_expired)))
            {
                sysDbgMsg(DMLNORM - 1,
                          arch_function_name,
   "thr=%04.4x PC=%04.4x.%04.4x.%04.4x.%04.4x.%04.4x  opcode=%02.2x %s",
                          thridx,
                          pc->clsidx,
                          pc->mthidx,
                          pc->codeatridx,
                          pc->excpatridx,
                          pc->offset,
                          pcode[pc->offset],
                          opcode_names[pcode[pc->offset]]);

                /* Retrieve next virtual opcode */
                opcode = pcode[pc->offset++];

/*
 * Due to the significant complexity of this @c @b switch
 * statement, the indentation is being reset to permit wider lines
 * of code with out breaking up expressions with the intention of
 * creating better readability of the code.
 */

static void dummy1(void) { char *p, *dummy2; dummy2 = p; dummy1(); }
#define STUB { dummy1(); }

switch(opcode)
{
case OPCODE_00_NOP:         /* Do nothing */
    break;

case OPCODE_01_ACONST_NULL: /* Push NULL onto stack */
    PUSH(thridx, FORCE_JINT(jvm_object_hash_null));
    break;

case OPCODE_02_ICONST_M1:   /* Push constant -1, 0, 1, 2, 3, 4, 5 */
case OPCODE_03_ICONST_0:
case OPCODE_04_ICONST_1:
case OPCODE_05_ICONST_2:
case OPCODE_06_ICONST_3:
case OPCODE_07_ICONST_4:
case OPCODE_08_ICONST_5:
    PUSH(thridx, (((jint) opcode) - ((jint) OPCODE_03_ICONST_0)));
    break;

case OPCODE_09_LCONST_0:
case OPCODE_0A_LCONST_1:
    jltmp1 = (((jlong) opcode) - ((jlong) OPCODE_09_LCONST_0));

    bytegames_split_jlong(jltmp1, &jitmp1, &jitmp2);

    PUSH(thridx, jitmp1); /* ms word */
    PUSH(thridx, jitmp2); /* ls word */
    break;

case OPCODE_0B_FCONST_0:
case OPCODE_0C_FCONST_1:
case OPCODE_0D_FCONST_2:
    jftmp1 = (jfloat) (((jint) opcode) - ((jint) OPCODE_0B_FCONST_0));

    PUSH(thridx, FORCE_JINT(jftmp1));
    break;

case OPCODE_0E_DCONST_0:
case OPCODE_0F_DCONST_1:
    jdtmp1 = (jdouble) (((jint) opcode) - ((jint) OPCODE_0E_DCONST_0));

    bytegames_split_jdouble(jdtmp1, &jitmp1, &jitmp2);

    PUSH(thridx, jitmp1); /* ms word */
    PUSH(thridx, jitmp2); /* ls word */

    break;

case OPCODE_10_BIPUSH:
    GET_U1_OPERAND(op1u1);
    jbtmp  = (jbyte) op1u1; /* Play sign extension games */
    jitmp1 = (jint) jbtmp;

    PUSH(thridx, jitmp1);
    break;

case OPCODE_11_SIPUSH:
    GET_U2_OPERAND(op1u2);
    jstmp  = (jshort) op1u2; /* Play sign extension games */
    jitmp1 = (jint) jstmp;

    PUSH(thridx, jitmp1);
    break;

case OPCODE_12_LDC:
case OPCODE_13_LDC_W:
/*! @todo HARMONY-6-jvm-opcode.c-122 Needs unit testing with real data*/
    switch(opcode)
    {
        case OPCODE_12_LDC:
            GET_U1_OPERAND(op1u1);
            jitmp1 = (juint) op1u1; /* Play sign extension games */
            break;

        case OPCODE_13_LDC_W:
            GET_U2_OPERAND(op1u2);
            jitmp1 = (juint) op1u2; /* Play sign extension games */
            break;
    }

    /* Treat all three 32-bit types the same except for constant_pool */
    switch (CP_TAG(pcfs, jitmp1))
    {
        case CONSTANT_Integer:
            MAKE_PU4(pu4, &PTR_CP_ENTRY_INTEGER(pcfs, jitmp1)->bytes);
            jitmp2 = GETRI4(pu4);
            PUSH(thridx, jitmp2);
            break;

        case CONSTANT_Float:
            MAKE_PU4(pu4, &PTR_CP_ENTRY_FLOAT(pcfs, jitmp1)->bytes);
            jitmp2 = GETRI4(pu4); /* Don't need to do type conversions*/
            PUSH(thridx, jitmp2);
            break;

        case CONSTANT_String:
            objhashmisc =
                object_instance_new(
                    OBJECT_STATUS_STRING,
               CLASS_OBJECT_LINKAGE(pjvm->class_java_lang_String)->pcfs,
                    pjvm->class_java_lang_String,
                    LOCAL_CONSTANT_NO_ARRAY_DIMS,
                    (rvoid *) rnull,

                    /*
                     * Irrelevant for strings, relevant
                     * for its its superclass.
                     */
                    rtrue,

                    thridx,
                    PTR_CP_ENTRY_UTF8(pcfs,
                                      PTR_CP_ENTRY_STRING(pcfs,
                                                          jitmp1)
                      ->string_index));
            PUSH(thridx, (jint) objhashmisc);
            break;

        default:
            /* No other CP_TAG() is legal for this opcode */
            thread_throw_exception(thridx,
                                   THREAD_STATUS_THREW_ERROR,
                                   JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
    }

    break;

case OPCODE_14_LDC2_W:
/*! @todo HARMONY-6-jvm-opcode.c-123 Needs unit testing with real data*/
    GET_U2_OPERAND(op1u2);
    jitmp1 = (juint) op1u2; /* Play sign extension games */

    /* Treat all three 32-bit types the same except for constant_pool */
    switch (CP_TAG(pcfs, jitmp1))
    {
        case CONSTANT_Long:
            MAKE_PU4(pu4, &PTR_CP_ENTRY_LONG(pcfs, jitmp1)->high_bytes);
            jitmp2 = GETRI4(pu4);
            MAKE_PU4(pu4, &PTR_CP_ENTRY_LONG(pcfs, jitmp1)->low_bytes);
            jitmp3 = GETRI4(pu4);

            /* Just move words around, don't interpret w/ type cnv */
            PUSH(thridx, jitmp2);
            PUSH(thridx, jitmp3);
            break;

        case CONSTANT_Double:
            MAKE_PU4(pu4, &PTR_CP_ENTRY_DOUBLE(pcfs, 
                                               jitmp1)->high_bytes);
            jitmp2 = GETRI4(pu4);
            MAKE_PU4(pu4, &PTR_CP_ENTRY_DOUBLE(pcfs,
                                               jitmp1)->low_bytes);
            jitmp3 = GETRI4(pu4);

            /* Just move words around, don't interpret w/ type cnv */
            PUSH(thridx, jitmp2);
            PUSH(thridx, jitmp3);
            break;

        default:
            /* No other CP_TAG() is legal for this opcode */
            thread_throw_exception(thridx,
                                   THREAD_STATUS_THREW_ERROR,
                                   JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
    }

    break;

case OPCODE_15_ILOAD:
  case_opcode_17_fload:
  case_opcode_19_aload:

    GET_WIDE_OR_NORMAL_INDEX(jitmp1, 0);

    jitmp2 = GET_LOCAL_VAR(thridx, jitmp1);
    PUSH(thridx, jitmp2);
    break;

case OPCODE_16_LLOAD:
  case_opcode_18_dload:

    GET_WIDE_OR_NORMAL_INDEX(jitmp1, 1);

    jitmp2 = GET_LOCAL_VAR(thridx, jitmp1);
    jitmp3 = GET_LOCAL_VAR(thridx, jitmp1 + 1);
    PUSH(thridx, jitmp2);
    PUSH(thridx, jitmp3);
    break;

case OPCODE_17_FLOAD:
    /*!
     * @internal Instead of treating (jfloat) as a (jint), both
     *           being 1 word, one could treat it separately, even
     *           though there is more code required to do it.  This
     *           is more formally "correct" at the expense of code
     *           complexity, although probably not run time.
     *
     *       <b><code>GET_WIDE_OR_NORMAL_INDEX(jitmp1, 0);</code></b>
     *
     *       <b><code>jftmp1 = GET_LOCAL_VAR(thridx, jitmp1);</code></b>
     *
     *       <b><code>PUSH(thridx, FORCE_JINT(jftmp1));</code></b>
     *
     *           However, since all other types are simply moving
     *           (jint) words around, just follow suit in this case.
     */

    goto case_opcode_17_fload;  /* Don't like 'goto', but makes sense */

case OPCODE_18_DLOAD:
    goto case_opcode_18_dload;  /* Don't like 'goto', but makes sense */

case OPCODE_19_ALOAD:
    goto case_opcode_19_aload;  /* Don't like 'goto', but makes sense */

case OPCODE_1A_ILOAD_0:
case OPCODE_1B_ILOAD_1:
case OPCODE_1C_ILOAD_2:
case OPCODE_1D_ILOAD_3:

    jitmp1 = (((jint) opcode) - ((jint) OPCODE_1A_ILOAD_0));

    jitmp2 = GET_LOCAL_VAR(thridx, jitmp1);
    PUSH(thridx, jitmp2);
    break;

case OPCODE_1E_LLOAD_0:
case OPCODE_1F_LLOAD_1:
case OPCODE_20_LLOAD_2:
case OPCODE_21_LLOAD_3:
    jitmp1 = (((jint) opcode) - ((jint) OPCODE_1E_LLOAD_0));

    jitmp2 = GET_LOCAL_VAR(thridx, jitmp1);
    jitmp3 = GET_LOCAL_VAR(thridx, jitmp1 + 1);
    PUSH(thridx, jitmp2);
    PUSH(thridx, jitmp3);
    break;

case OPCODE_22_FLOAD_0:
case OPCODE_23_FLOAD_1:
case OPCODE_24_FLOAD_2:
case OPCODE_25_FLOAD_3:
    jitmp1 = (((jint) opcode) - ((jint) OPCODE_22_FLOAD_0));

    jitmp2 = GET_LOCAL_VAR(thridx, jitmp1);
    PUSH(thridx, jitmp2);
    break;

case OPCODE_26_DLOAD_0:
case OPCODE_27_DLOAD_1:
case OPCODE_28_DLOAD_2:
case OPCODE_29_DLOAD_3:
    jitmp1 = (((jint) opcode) - ((jint) OPCODE_26_DLOAD_0));

    jitmp2 = GET_LOCAL_VAR(thridx, jitmp1);
    jitmp3 = GET_LOCAL_VAR(thridx, jitmp1 + 1);
    PUSH(thridx, jitmp2);
    PUSH(thridx, jitmp3);
    break;

case OPCODE_2A_ALOAD_0:
case OPCODE_2B_ALOAD_1:
case OPCODE_2C_ALOAD_2:
case OPCODE_2D_ALOAD_3:
    jitmp1 = (((jint) opcode) - ((jint) OPCODE_2A_ALOAD_0));

    jitmp2 = GET_LOCAL_VAR(thridx, jitmp1);
    /*!
     * @todo HARMONY-6-jvm-opcode.c-138 Does there need to be a test
     *       to verify that this is actually an object hash?  Perhaps
     *       it is valid if null?  Perhaps evaluate other contents?
     */
    PUSH(thridx, jitmp2);
    break;

case OPCODE_2E_IALOAD:
 /*! @todo HARMONY-6-jvm-opcode.c-84 Needs unit testing with real data*/
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_I, jitmp1);

    jitmp2 = ((jint *) OBJECT(jotmp1).arraydata)[jitmp1];
    PUSH(thridx, jitmp2);
    break;

case OPCODE_2F_LALOAD:
 /*! @todo HARMONY-6-jvm-opcode.c-85 Needs unit testing with real data*/
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_J, jitmp1);

    jltmp1 = ((jlong *) OBJECT(jotmp1).arraydata)[jitmp1];

    bytegames_split_jlong(jltmp1, &jitmp1, &jitmp2);
    PUSH(thridx, jitmp1);
    PUSH(thridx, jitmp2);
    break;

case OPCODE_30_FALOAD:
 /*! @todo HARMONY-6-jvm-opcode.c-86 Needs unit testing with real data*/
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_F, jitmp1);

    jftmp1 = ((jfloat *) OBJECT(jotmp1).arraydata)[jitmp1];
    PUSH(thridx, FORCE_JINT(jftmp1));
    break;

case OPCODE_31_DALOAD:
 /*! @todo HARMONY-6-jvm-opcode.c-87 Needs unit testing with real data*/
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_D, jitmp1);

    jdtmp1 = ((jdouble *) OBJECT(jotmp1).arraydata)[jitmp1];

    bytegames_split_jdouble(jdtmp1, &jitmp1, &jitmp2);
    PUSH(thridx, jitmp1);
    PUSH(thridx, jitmp2);
    break;

case OPCODE_32_AALOAD:
 /*! @todo HARMONY-6-jvm-opcode.c-88 Needs unit testing with real data*/
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_L, jitmp1);

    /*!
     * @internal Careful!  Use of 'jotmp1' as both rvalue and lvalue!
     *
     */
    jotmp1 = ((jvm_object_hash *) OBJECT(jotmp1).arraydata)[jitmp1];
    jitmp1 = (jint) jotmp1;
    PUSH(thridx, jitmp1);
    break;

case OPCODE_33_BALOAD:
 /*! @todo HARMONY-6-jvm-opcode.c-89 Needs unit testing with real data*/
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_B, jitmp1);

    jbtmp  = ((jbyte *) OBJECT(jotmp1).arraydata)[jitmp1];
    jitmp2 = (jint) jbtmp; /* Play sign extension games */
    PUSH(thridx, jitmp2);
    break;

case OPCODE_34_CALOAD:
 /*! @todo HARMONY-6-jvm-opcode.c-90 Needs unit testing with real data*/
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_C, jitmp1);

    jctmp  = ((jchar *) OBJECT(jotmp1).arraydata)[jitmp1];
    jitmp2 = (jint) jctmp; /* Play sign extension games */
    PUSH(thridx, jitmp2);
    break;

case OPCODE_35_SALOAD:
 /*! @todo HARMONY-6-jvm-opcode.c-91 Needs unit testing with real data*/
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_S, jitmp1);

    jstmp  = ((jshort *) OBJECT(jotmp1).arraydata)[jitmp1];
    jitmp2 = (jint) jstmp; /* Play sign extension games */
    PUSH(thridx, jitmp2);
    break;

case OPCODE_36_ISTORE:
  case_opcode_38_fstore:
  case_opcode_3a_astore:

    GET_WIDE_OR_NORMAL_INDEX(jitmp1, 0);

    POP(thridx, jitmp2, jint);
    PUT_LOCAL_VAR(thridx, jitmp1, jitmp2);
    break;

case OPCODE_37_LSTORE:
  case_opcode_39_lstore:

    GET_WIDE_OR_NORMAL_INDEX(jitmp1, 1);

    POP(thridx, jitmp3, jint);
    POP(thridx, jitmp2, jint);
    PUT_LOCAL_VAR(thridx, jitmp1,     jitmp2);
    PUT_LOCAL_VAR(thridx, jitmp1 + 1, jitmp3);
    break;

case OPCODE_38_FSTORE:
    goto case_opcode_38_fstore; /* Don't like 'goto', but makes sense */

case OPCODE_39_DSTORE:
    goto case_opcode_39_lstore; /* Don't like 'goto', but makes sense */

case OPCODE_3A_ASTORE:
    goto case_opcode_3a_astore; /* Don't like 'goto', but makes sense */

case OPCODE_3B_ISTORE_0:
case OPCODE_3C_ISTORE_1:
case OPCODE_3D_ISTORE_2:
case OPCODE_3E_ISTORE_3:
    jitmp1 = (((jint) opcode) - ((jint) OPCODE_3B_ISTORE_0));

    POP(thridx, jitmp2, jint);
    PUT_LOCAL_VAR(thridx, jitmp1, jitmp2);
    break;

case OPCODE_3F_LSTORE_0:
case OPCODE_40_LSTORE_1:
case OPCODE_41_LSTORE_2:
case OPCODE_42_LSTORE_3:
    jitmp1 = (((jint) opcode) - ((jint) OPCODE_3F_LSTORE_0));

    POP(thridx, jitmp3, jint);
    POP(thridx, jitmp2, jint);
    PUT_LOCAL_VAR(thridx, jitmp1 + 1, jitmp3);
    PUT_LOCAL_VAR(thridx, jitmp1,     jitmp2);
    break;

case OPCODE_43_FSTORE_0:
case OPCODE_44_FSTORE_1:
case OPCODE_45_FSTORE_2:
case OPCODE_46_FSTORE_3:
    jitmp1 = (((jint) opcode) - ((jint) OPCODE_43_FSTORE_0));

    POP(thridx, jitmp2, jint);
    PUT_LOCAL_VAR(thridx, jitmp1, jitmp2);
    break;

case OPCODE_47_DSTORE_0:
case OPCODE_48_DSTORE_1:
case OPCODE_49_DSTORE_2:
case OPCODE_4A_DSTORE_3:
    jitmp1 = (((jint) opcode) - ((jint) OPCODE_47_DSTORE_0));

    POP(thridx, jitmp3, jint);
    POP(thridx, jitmp2, jint);
    PUT_LOCAL_VAR(thridx, jitmp1 + 1, jitmp3);
    PUT_LOCAL_VAR(thridx, jitmp1,     jitmp2);
    break;

case OPCODE_4B_ASTORE_0:
case OPCODE_4C_ASTORE_1:
case OPCODE_4D_ASTORE_2:
case OPCODE_4E_ASTORE_3:
    jitmp1 = (((jint) opcode) - ((jint) OPCODE_4B_ASTORE_0));

    POP(thridx, jitmp2, jint);
    PUT_LOCAL_VAR(thridx, jitmp1, jitmp2);
    break;

case OPCODE_4F_IASTORE:
 /*! @todo HARMONY-6-jvm-opcode.c-92 Needs unit testing with real data*/

    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_I, jitmp1);

    ((jint *) OBJECT(jotmp1).arraydata)[jitmp1] = jitmp2;
    break;

case OPCODE_50_LASTORE:
 /*! @todo HARMONY-6-jvm-opcode.c-93 Needs unit testing with real data*/

    POP(thridx, jitmp3, jint);
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_J, jitmp1);

    jltmp1 = bytegames_combine_jlong(jitmp2, jitmp3);
    ((jlong *) OBJECT(jotmp1).arraydata)[jitmp1] = jltmp1;
    break;

case OPCODE_51_FASTORE:
 /*! @todo HARMONY-6-jvm-opcode.c-94 Needs unit testing with real data*/

    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_F, jitmp1);

    jftmp1 = FORCE_JFLOAT(jitmp2);
    ((jfloat *) OBJECT(jotmp1).arraydata)[jitmp1] = jftmp1;
    break;

case OPCODE_52_DASTORE:
 /*! @todo HARMONY-6-jvm-opcode.c-95 Needs unit testing with real data*/

    POP(thridx, jitmp3, jint);
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_D, jitmp1);

    jdtmp1 = bytegames_combine_jdouble(jitmp2, jitmp3);
    ((jdouble *) OBJECT(jotmp1).arraydata)[jitmp1] = jdtmp1;
    break;

case OPCODE_53_AASTORE:
 /*! @todo HARMONY-6-jvm-opcode.c-96 Needs unit testing with real data*/

    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_L, jitmp1);

    ((jvm_object_hash *) OBJECT(jotmp1).arraydata)[jitmp1] = jitmp2;
    break;

case OPCODE_54_BASTORE:
 /*! @todo HARMONY-6-jvm-opcode.c-97 Needs unit testing with real data*/

    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_I, jitmp1);

    jbtmp = (jbyte) jitmp2;
    ((jbyte *) OBJECT(jotmp1).arraydata)[jitmp1] = jbtmp;
    break;

case OPCODE_55_CASTORE:
 /*! @todo HARMONY-6-jvm-opcode.c-98 Needs unit testing with real data*/

    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_C, jitmp1);

    jctmp = (jchar) jitmp2;
    ((jchar *) OBJECT(jotmp1).arraydata)[jitmp1] = jctmp;
    break;

case OPCODE_56_SASTORE:
 /*! @todo HARMONY-6-jvm-opcode.c-99 Needs unit testing with real data*/

    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);
    VERIFY_ARRAY_REFERENCE(jotmp1, BASETYPE_CHAR_C, jitmp1);

    jstmp = (jshort) jitmp2;
    ((jshort *) OBJECT(jotmp1).arraydata)[jitmp1] = jstmp;
    break;

case OPCODE_57_POP:
    POP(thridx, jitmp1, jint);
    break;

case OPCODE_58_POP2:
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    break;

case OPCODE_59_DUP:
    jitmp1 = GET_SP_WORD(thridx, 0, jint);
    PUSH(thridx, jitmp1);
    break;

case OPCODE_5A_DUP_X1:
    jitmp1 = GET_SP_WORD(thridx, 0, jint);
    jitmp2 = GET_SP_WORD(thridx, 1, jint);

    PUSH(thridx, jitmp1);
    PUT_SP_WORD(thridx, 1, jitmp2);
    PUT_SP_WORD(thridx, 2, jitmp1);
    break;

case OPCODE_5B_DUP_X2:
    jitmp1 = GET_SP_WORD(thridx, 0, jint);
    jitmp2 = GET_SP_WORD(thridx, 1, jint);
    jitmp3 = GET_SP_WORD(thridx, 2, jint);

    PUSH(thridx, jitmp1);
    PUT_SP_WORD(thridx, 1, jitmp2);
    PUT_SP_WORD(thridx, 2, jitmp3);
    PUT_SP_WORD(thridx, 3, jitmp1);
    break;

case OPCODE_5C_DUP2:
    jitmp1 = GET_SP_WORD(thridx, 0, jint);
    jitmp2 = GET_SP_WORD(thridx, 1, jint);

    PUSH(thridx, jitmp2);
    PUSH(thridx, jitmp1);
    break;

case OPCODE_5D_DUP2_X1:
    jitmp1 = GET_SP_WORD(thridx, 0, jint);
    jitmp2 = GET_SP_WORD(thridx, 1, jint);
    jitmp3 = GET_SP_WORD(thridx, 2, jint);

    PUSH(thridx, jitmp2);
    PUSH(thridx, jitmp1);
    PUT_SP_WORD(thridx, 2, jitmp3);
    PUT_SP_WORD(thridx, 3, jitmp1);
    PUT_SP_WORD(thridx, 4, jitmp2);
    break;

case OPCODE_5E_DUP2_X2:
    jitmp1 = GET_SP_WORD(thridx, 0, jint);
    jitmp2 = GET_SP_WORD(thridx, 1, jint);
    jitmp3 = GET_SP_WORD(thridx, 2, jint);
    jitmp4 = GET_SP_WORD(thridx, 3, jint);

    PUSH(thridx, jitmp2);
    PUSH(thridx, jitmp1);
    PUT_SP_WORD(thridx, 2, jitmp3);
    PUT_SP_WORD(thridx, 3, jitmp4);
    PUT_SP_WORD(thridx, 4, jitmp1);
    PUT_SP_WORD(thridx, 5, jitmp2);
    break;

case OPCODE_5F_SWAP:
    jitmp1 = GET_SP_WORD(thridx, 0, jint);
    jitmp2 = GET_SP_WORD(thridx, 1, jint);

    PUT_SP_WORD(thridx, 0, jitmp2);
    PUT_SP_WORD(thridx, 1, jitmp1);
    break;

case OPCODE_60_IADD:
    SINGLE_ARITHMETIC_BINARY(jitmp1, jitmp2, + , FORCE_NOTHING);
    break;

case OPCODE_61_LADD:
    DOUBLE_ARITHMETIC_BINARY(jltmp1, jltmp2, jitmp1, jitmp2, jlong, + );
    break;

case OPCODE_62_FADD:
    SINGLE_ARITHMETIC_BINARY(jftmp1, jftmp2, + , FORCE_JINT);
    break;

case OPCODE_63_DADD:
    DOUBLE_ARITHMETIC_BINARY(jdtmp1, jdtmp2, jitmp1,jitmp2,jdouble, + );
    break;

case OPCODE_64_ISUB:
    SINGLE_ARITHMETIC_BINARY(jitmp1, jitmp2, - , FORCE_NOTHING);
    break;

case OPCODE_65_LSUB:
    DOUBLE_ARITHMETIC_BINARY(jltmp1, jltmp2, jitmp1, jitmp2, jlong, - );
    break;

case OPCODE_66_FSUB:
    SINGLE_ARITHMETIC_BINARY(jftmp1, jftmp2, - , FORCE_JINT);
    break;

case OPCODE_67_DSUB:
    DOUBLE_ARITHMETIC_BINARY(jdtmp1, jdtmp2, jitmp1,jitmp2,jdouble, - );
    break;

case OPCODE_68_IMUL:
    SINGLE_ARITHMETIC_BINARY(jitmp1, jitmp2, * , FORCE_NOTHING);
    break;

case OPCODE_69_LMUL:
    DOUBLE_ARITHMETIC_BINARY(jltmp1, jltmp2, jitmp1, jitmp2, jlong, * );
    break;

case OPCODE_6A_FMUL:
    SINGLE_ARITHMETIC_BINARY(jftmp1, jftmp2, * , FORCE_JINT);
    break;

case OPCODE_6B_DMUL:
    DOUBLE_ARITHMETIC_BINARY(jdtmp1, jdtmp2, jitmp1,jitmp2,jdouble, * );
    break;

case OPCODE_6C_IDIV:
    SINGLE_ARITHMETIC_BINARY(jitmp1, jitmp2, / , FORCE_NOTHING);
    break;

case OPCODE_6D_LDIV:
    DOUBLE_ARITHMETIC_BINARY(jltmp1, jltmp2, jitmp1, jitmp2, jlong, / );
    break;

case OPCODE_6E_FDIV:
    SINGLE_ARITHMETIC_BINARY(jftmp1, jftmp2, / , FORCE_JINT);
    break;

case OPCODE_6F_DDIV:
    DOUBLE_ARITHMETIC_BINARY(jdtmp1, jdtmp2, jitmp1,jitmp2,jdouble, / );
    break;

case OPCODE_70_IREM:
    SINGLE_ARITHMETIC_BINARY(jitmp1, jitmp2, % , FORCE_NOTHING);
    break;

case OPCODE_71_LREM:
    DOUBLE_ARITHMETIC_BINARY(jltmp1, jltmp2, jitmp1, jitmp2, jlong, % );
    break;

case OPCODE_72_FREM:
    POP(thridx, jftmp1, jfloat);
    POP(thridx, jftmp2, jfloat);

    jftmp1 = portable_jfloat_fmod(jftmp1, jftmp2);

    PUSH(thridx, FORCE_JINT(jftmp1));
    break;

case OPCODE_73_DREM:
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jdtmp1 = bytegames_combine_jdouble(jitmp1, jitmp2);

    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jdtmp2 = bytegames_combine_jdouble(jitmp1, jitmp2);

    jdtmp1 = portable_jdouble_fmod(jdtmp1, jdtmp2);

    bytegames_split_jdouble(jdtmp1, &jitmp1, &jitmp2);
    PUSH(thridx, jitmp1);
    PUSH(thridx, jitmp2);
    break;

case OPCODE_74_INEG:
    POP(thridx, jitmp1, jint);
    jitmp1 = 0 - jitmp1;
    PUSH(thridx, jitmp1);
    break;

case OPCODE_75_LNEG:
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jltmp1 = bytegames_combine_jlong(jitmp1, jitmp2);

    jltmp1 = 0 - jltmp1;

    bytegames_split_jlong(jltmp1, &jitmp1, &jitmp2);
    PUSH(thridx, jitmp1);
    PUSH(thridx, jitmp2);
    break;

case OPCODE_76_FNEG:
    POP(thridx, jftmp1, jfloat);
    jftmp1 = 0.0 - jftmp1;
    PUSH(thridx, FORCE_JINT(jftmp1));
    break;

case OPCODE_77_DNEG:
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jdtmp1 = bytegames_combine_jdouble(jitmp1, jitmp2);

    jdtmp1 = 0.0 - jdtmp1;

    bytegames_split_jdouble(jdtmp1, &jitmp1, &jitmp2);
    PUSH(thridx, jitmp1);
    PUSH(thridx, jitmp2);
    break;

case OPCODE_78_ISHL:
    SINGLE_ARITHMETIC_BINARY(jitmp1, jitmp2, << , FORCE_NOTHING);
    break;

case OPCODE_79_LSHL:
    DOUBLE_ARITHMETIC_BINARY(jltmp1, jltmp2, jitmp1, jitmp2,jlong, << );
    break;

case OPCODE_7A_ISHR:
    SINGLE_ARITHMETIC_BINARY(jitmp1, jitmp2, >> , FORCE_NOTHING);
    break;

case OPCODE_7B_LSHR:
    DOUBLE_ARITHMETIC_BINARY(jltmp1, jltmp2, jitmp1, jitmp2,jlong, >> );
    break;

case OPCODE_7C_IUSHR:
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jitmp1 = ((juint) jitmp1) >> jitmp2 ;
    PUSH(thridx, jftmp1);
    break;

case OPCODE_7D_LUSHR:
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jltmp1 = bytegames_combine_jlong(jitmp1, jitmp2);

    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jltmp2 = bytegames_combine_jlong(jitmp1, jitmp2);

    jltmp1 = ((julong) jltmp1) >> jltmp2;

    bytegames_split_jlong(jltmp1, &jitmp1, &jitmp2);
    PUSH(thridx, jitmp1);
    PUSH(thridx, jitmp2);
    break;

case OPCODE_7E_IAND:
    SINGLE_ARITHMETIC_BINARY(jitmp1, jitmp2, & , FORCE_NOTHING);
    break;

case OPCODE_7F_LAND:
    DOUBLE_ARITHMETIC_BINARY(jltmp1, jltmp2, jitmp1, jitmp2, jlong, & );
    break;

case OPCODE_80_IOR:
    SINGLE_ARITHMETIC_BINARY(jitmp1, jitmp2, | , FORCE_NOTHING);
    break;

case OPCODE_81_LOR:
    DOUBLE_ARITHMETIC_BINARY(jltmp1, jltmp2, jitmp1, jitmp2, jlong, | );
    break;

case OPCODE_82_IXOR:
    SINGLE_ARITHMETIC_BINARY(jitmp1, jitmp2, ^ , FORCE_NOTHING);
    break;

case OPCODE_83_LXOR:
    DOUBLE_ARITHMETIC_BINARY(jltmp1, jltmp2, jitmp1, jitmp2, jlong, ^ );
    break;

case OPCODE_84_IINC:
    GET_U1_OPERAND(op1u1);
    jitmp1 = (jint) op1u1;

    GET_U1_OPERAND(op1u1);
    jbtmp  = (jbyte) op1u1; /* Play sign extension games */
    jitmp2 = (jint) jbtmp;

    jitmp3 = GET_LOCAL_VAR(thridx, jitmp1);
    jitmp3 += jitmp2;
    PUT_LOCAL_VAR(thridx, jitmp1, jitmp3);
    break;

case OPCODE_85_I2L:
    POP(thridx, jitmp1, jint);
    jltmp1 = (jlong) jitmp1;

    bytegames_split_jlong(jltmp1, &jitmp1, &jitmp2);
    PUSH(thridx, jitmp1);
    PUSH(thridx, jitmp2);
    break;

case OPCODE_86_I2F:
    POP(thridx, jitmp1, jint);
    jftmp1 = (jfloat) jitmp1;

    jitmp1 = FORCE_JINT(jftmp1);
    PUSH(thridx, jitmp1);
    break;

case OPCODE_87_I2D:
    POP(thridx, jitmp1, jint);
    jdtmp1 = (jdouble) jitmp1;

    bytegames_split_jdouble(jdtmp1, &jitmp1, &jitmp2);
    PUSH(thridx, jitmp1);
    PUSH(thridx, jitmp2);
    break;

case OPCODE_88_L2I:
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jltmp1 = bytegames_combine_jlong(jitmp1, jitmp2);

    jitmp3 = (jint) jitmp1;

    PUSH(thridx, jitmp3);
    break;

case OPCODE_89_L2F:
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jltmp1 = bytegames_combine_jlong(jitmp1, jitmp2);

    jftmp1 = (jfloat) jltmp1;

    jitmp1 = FORCE_JINT(jftmp1);
    PUSH(thridx, jitmp1);
    break;

case OPCODE_8A_L2D:
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jltmp1 = bytegames_combine_jlong(jitmp1, jitmp2);

    jdtmp1 = (jdouble) jltmp1;

    bytegames_split_jdouble(jdtmp1, &jitmp1, &jitmp2);
    PUSH(thridx, jitmp1);
    PUSH(thridx, jitmp2);
    break;

case OPCODE_8B_F2I:
/*! @todo HARMONY-6-jvm-opcode.c-100 Needs unit testing with real data*/
    POP(thridx, jftmp2, jfloat);

    IF_JFLOAT_SPECIAL_CASES(jftmp1,
                            jitmp2,
                            jitmp1,
                            0,
                            0,
                            JINT_LARGEST_POSITIVE,
                            0,
                            JINT_LARGEST_NEGATIVE)
    else
    {
        /*!
         * @todo HARMONY-6-jvm-opcode.c-101 This casting needs to
         *       perform proper rounding during conversion.  Verify
         *       that this happens.
         */
        jitmp1 = (jint) jftmp1;
    }

    PUSH(thridx, jitmp1);
    break;

case OPCODE_8C_F2L:
/*! @todo HARMONY-6-jvm-opcode.c-102 Needs unit testing with real data*/
    POP(thridx, jftmp1, jfloat);

    IF_JFLOAT_SPECIAL_CASES(jftmp1,
                            jitmp1,
                            jltmp1,
                            0,
                            0,
                            JLONG_LARGEST_POSITIVE,
                            0,
                            JLONG_LARGEST_NEGATIVE)
    else
    {
        /*!
         * @todo HARMONY-6-jvm-opcode.c-103 This casting needs to
         *       perform proper rounding during conversion.  Verify
         *       that this happens.
         */
        jltmp1 = (jlong) jftmp1;
    }

    bytegames_split_jlong(jltmp1, &jitmp1, &jitmp2);
    PUSH(thridx, jitmp1);
    PUSH(thridx, jitmp2);
    break;

case OPCODE_8D_F2D:
/*! @todo HARMONY-6-jvm-opcode.c-104 Needs unit testing with real data*/
    POP(thridx, jitmp1, jfloat);

    IF_JFLOAT_SPECIAL_CASES(jftmp1,
                            jitmp1,
                            jltmp1,
                            0.0,
                            JDOUBLE_POSITIVE_ZERO,
                            JLONG_LARGEST_POSITIVE,
                            JDOUBLE_NEGATIVE_ZERO,
                            JLONG_LARGEST_NEGATIVE)
    else
    {
        /*!
         * @todo HARMONY-6-jvm-opcode.c-105 This casting needs to
         *       perform proper rounding during conversion if FP-strict
         *       is enforced.  Verify that this happens.
         *
         * @todo HARMONY-6-jvm-opcode.c-106 This casting needs to
         *       perform extended value set conversion if FP-strict
         *       is not enforced.  Verify that this happens.
         *
         */
        jdtmp1 = (jdouble) jftmp1;
        jltmp1 = FORCE_JLONG(jdtmp1);
    }

    bytegames_split_jlong(jltmp1, &jitmp1, &jitmp2);
    PUSH(thridx, jitmp1);
    PUSH(thridx, jitmp2);
    break;

case OPCODE_8E_D2I:
/*! @todo HARMONY-6-jvm-opcode.c-107 Needs unit testing with real data*/
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jdtmp1 = bytegames_combine_jdouble(jitmp1, jitmp2);

    IF_JDOUBLE_SPECIAL_CASES(jdtmp1,
                             jltmp1,
                             jitmp1,
                             0,
                             0,
                             JINT_LARGEST_POSITIVE,
                             0,
                             JINT_LARGEST_NEGATIVE)
    else
    {
        /*!
         * @todo HARMONY-6-jvm-opcode.c-108 This casting needs to
         *       perform proper rounding during conversion.  Verify
         *       that this happens.
         */
        jitmp1 = (jint) jdtmp1;
    }

    PUSH(thridx, jitmp1);
    break;

case OPCODE_8F_D2L:
/*! @todo HARMONY-6-jvm-opcode.c-109 Needs unit testing with real data*/
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jdtmp1 = bytegames_combine_jdouble(jitmp1, jitmp2);

    IF_JDOUBLE_SPECIAL_CASES(jdtmp1,
                             jltmp1,
                             jltmp2,
                             0,
                             0,
                             JLONG_LARGEST_POSITIVE,
                             0,
                             JLONG_LARGEST_NEGATIVE)
    else
    {
        /*!
         * @todo HARMONY-6-jvm-opcode.c-110 This casting needs to
         *       perform proper rounding during conversion.  Verify
         *       that this happens.
         */
        jltmp2 = (jlong) jdtmp1;
    }

    bytegames_split_jlong(jltmp2, &jitmp1, &jitmp2);
    PUSH(thridx, jitmp1);
    PUSH(thridx, jitmp2);
    break;

case OPCODE_90_D2F:
/*! @todo HARMONY-6-jvm-opcode.c-111 Needs unit testing with real data*/
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jdtmp1 = bytegames_combine_jdouble(jitmp1, jitmp2);

    IF_JDOUBLE_SPECIAL_CASES(jdtmp1,
                             jltmp1,
                             jitmp1,
                             0.0,
                             JFLOAT_POSITIVE_ZERO,
                             JINT_LARGEST_POSITIVE,
                             JFLOAT_NEGATIVE_ZERO,
                             JINT_LARGEST_NEGATIVE)
    else
    {
        /*!
         * @todo HARMONY-6-jvm-opcode.c-112 This casting needs to
         *       perform proper rounding during conversion if FP-strict
         *       is enforced.  Verify that this happens.
         *
         * @todo HARMONY-6-jvm-opcode.c-113 This casting needs to
         *       perform extended value set conversion if FP-strict
         *       is not enforced.  Verify that this happens.
         *
         */
        jftmp1 = (jfloat) jdtmp1;
        jitmp1 = FORCE_JINT(jftmp1);
    }

    PUSH(thridx, jitmp1);
    break;

case OPCODE_91_I2B:
    POP(thridx, jitmp1, jint);
    jbtmp  = (jbyte) jitmp1;   /* Play sign extension games */
    jitmp1 = (jint)  jbtmp;

    PUSH(thridx, jitmp1);
    break;

case OPCODE_92_I2C:
    POP(thridx, jitmp1, jint);
    jctmp  = (jchar) jitmp1;   /* Play sign extension games */
    jitmp1 = (jint)  jctmp;

    PUSH(thridx, jitmp1);
    break;

case OPCODE_93_I2S:
    POP(thridx, jitmp1, jint);
    jstmp  = (jshort) jitmp1;   /* Play sign extension games */
    jitmp1 = (jint)   jstmp;

    PUSH(thridx, jitmp1);
    break;

case OPCODE_94_LCMP:
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jltmp2 = bytegames_combine_jlong(jitmp1, jitmp2);

    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jltmp1 = bytegames_combine_jlong(jitmp1, jitmp2);

    jltmp1 = jltmp2 - jltmp1;

    jitmp1 = (0 < jltmp1) ? ((jint) 1)
                          : (0 == jltmp1) ? ((jint) 0)
                                          : ((jint) -1);

    PUSH(thridx, jitmp1);
    break;

case OPCODE_95_FCMPL:
case OPCODE_96_FCMPG:
 /*! @todo HARMONY-6-jvm-opcode.c-117 Needs unit testing w/ real data */
    POP(thridx, jftmp2, jfloat);
    POP(thridx, jftmp1, jfloat);

    jitmp2 = FORCE_JINT(jftmp2);
    jitmp1 = FORCE_JINT(jftmp1);
    if (JFLOAT_IS_NAN(jitmp1) || JFLOAT_IS_NAN(jitmp2))
    {
        switch(opcode)
        {
            case OPCODE_95_FCMPL:
                jitmp1 = (jint) -1;
                break;

            case OPCODE_96_FCMPG:
                jitmp1 = (jint) 1;
                break;
        }
    }
    else
    {
        jftmp1 = jftmp1 - jftmp2;

        jitmp1 = (0.0 < jftmp1) ? ((jint) 1)
                                : (0.0 == jftmp1) ? ((jint) 0)
                                                  : ((jint) -1);
    }

    PUSH(thridx, jitmp1);
    break;

case OPCODE_97_DCMPL:
case OPCODE_98_DCMPG:
 /*! @todo HARMONY-6-jvm-opcode.c-118 Needs unit testing w/ real data */
    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jdtmp2 = bytegames_combine_jdouble(jitmp1, jitmp2);

    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);
    jdtmp1 = bytegames_combine_jdouble(jitmp1, jitmp2);

    jltmp2 = FORCE_JLONG(jdtmp2);
    jltmp1 = FORCE_JLONG(jdtmp1);
    if (JDOUBLE_IS_NAN(jltmp1) || JDOUBLE_IS_NAN(jltmp2))
    { 
        switch(opcode)
        {
            case OPCODE_97_DCMPL:
            jitmp1 = (jint) -1;
            break;

            case OPCODE_98_DCMPG:
            jitmp1 = (jint) 1;
            break;
        }
    }
    else
    {
        jdtmp1 = jdtmp1 - jdtmp2;

        jitmp1 = (0.0 < jdtmp1) ? ((jint) 1)
                                : (0.0 == jdtmp1) ? ((jint) 0)
                                                  : ((jint) -1);
    }

    PUSH(thridx, jitmp1);
    break;

case OPCODE_99_IFEQ:
case OPCODE_9A_IFNE:
case OPCODE_9B_IFLT:
case OPCODE_9C_IFGE:
case OPCODE_9D_IFGT:
case OPCODE_9E_IFLE:
 /*! @todo HARMONY-6-jvm-opcode.c-119 Needs unit testing w/ real data */
    GET_U2_OPERAND(op1u2);

    POP(thridx, jitmp1, jint);

    rbool1 = rfalse;
    switch(opcode)
    {
        case OPCODE_99_IFEQ:
            if (jitmp1 == 0) { rbool1 = rtrue; }  break;
        case OPCODE_9A_IFNE:
            if (jitmp1 != 0) { rbool1 = rtrue; }  break;
        case OPCODE_9B_IFLT:
            if (jitmp1 <  0) { rbool1 = rtrue; }  break;
        case OPCODE_9E_IFLE:
            if (jitmp1 <= 0) { rbool1 = rtrue; }  break;
        case OPCODE_9D_IFGT:
            if (jitmp1 >  0) { rbool1 = rtrue; }  break;
        case OPCODE_9C_IFGE:
            if (jitmp1 >= 0) { rbool1 = rtrue; }  break;
    }

    if (rtrue == rbool1)
    {
                             /* sizes of opcode + operand */
        LOAD_TARGET_PC_OFFSET(op1u2, sizeof(u1) + sizeof(u2));
    }

    break;

case OPCODE_9F_IF_ICMPEQ:
case OPCODE_A0_IF_ICMPNE:
case OPCODE_A1_IF_ICMPLT:
case OPCODE_A2_IF_ICMPGE:
case OPCODE_A3_IF_ICMPGT:
case OPCODE_A4_IF_ICMPLE:
 /*! @todo HARMONY-6-jvm-opcode.c-120 Needs unit testing w/ real data */
    GET_U2_OPERAND(op1u2);

    POP(thridx, jitmp2, jint);
    POP(thridx, jitmp1, jint);

    rbool1 = rfalse;
    switch(opcode)
    {
        case OPCODE_9F_IF_ICMPEQ:
            if (jitmp1 == jitmp2) { rbool1 = rtrue; }  break;
        case OPCODE_A0_IF_ICMPNE:
            if (jitmp1 != jitmp2) { rbool1 = rtrue; }  break;
        case OPCODE_A1_IF_ICMPLT:
            if (jitmp1 <  jitmp2) { rbool1 = rtrue; }  break;
        case OPCODE_A4_IF_ICMPLE:
            if (jitmp1 <= jitmp2) { rbool1 = rtrue; }  break;
        case OPCODE_A3_IF_ICMPGT:
            if (jitmp1 >  jitmp2) { rbool1 = rtrue; }  break;
        case OPCODE_A2_IF_ICMPGE:
            if (jitmp1 >= jitmp2) { rbool1 = rtrue; }  break;
    }

    if (rtrue == rbool1)
    {
                             /* sizes of opcode + operand */
        LOAD_TARGET_PC_OFFSET(op1u2, sizeof(u1) + sizeof(u2));
    }

    break;

case OPCODE_A5_IF_ACMPEQ:
case OPCODE_A6_IF_ACMPNE:
 /*! @todo HARMONY-6-jvm-opcode.c-121 Needs unit testing w/ real data */
    GET_U2_OPERAND(op1u2);

    POP(thridx, jotmp2, jvm_object_hash);
    POP(thridx, jotmp1, jvm_object_hash);

    switch(opcode)
    {
        case OPCODE_A5_IF_ACMPEQ:
            if (jotmp1 == jotmp2)
            {
                                    /* sizes of opcode  + operand */
                LOAD_TARGET_PC_OFFSET(op1u2, sizeof(u1) + sizeof(u2));
            }
            break;

        case OPCODE_A6_IF_ACMPNE:
            if (jotmp1 != jotmp2)
            {
                                    /* sizes of opcode  + operand */
                LOAD_TARGET_PC_OFFSET(op1u2, sizeof(u1) + sizeof(u2));
            }
            break;
    }

    break;

case OPCODE_A7_GOTO:
    GET_U2_OPERAND(op1u2);

                         /* sizes of opcode + operand */
    LOAD_TARGET_PC_OFFSET(op1u2, sizeof(u1) + sizeof(u2));
    break;

case OPCODE_A8_JSR:
    GET_U2_OPERAND(op1u2);

    /*!
     * @todo HARMONY-6-jvm-opcode.c-114 Need a better definition
     *       of type @c @b returnAddress than a simple
     *       @link #jint jint@endlink.
     *
     */
    jitmp1 = (jint) pc->offset;
    PUSH(thridx, jitmp1);

                          /* size of opcode */
    LOAD_TARGET_PC_OFFSET(op1u2, sizeof(u1));
    break;

case OPCODE_A9_RET:
    GET_WIDE_OR_NORMAL_INDEX(jitmp1, 0);

    /*!
     * @todo HARMONY-6-jvm-opcode.c-115 Need a better definition
     *       of type @c @b returnAddress than a simple
     *       @link #jint jint@endlink.
     *
     */
    pc->offset = (jvm_pc_offset) GET_LOCAL_VAR(thridx, jitmp1);
    break;

case OPCODE_AA_TABLESWITCH:
 /*! @todo HARMONY-6-jvm-opcode.c-124 Needs unit testing w/ real data */
    SWITCH_PAD_PC;

    /* Retrieve 'default' table switch value */
    MAKE_PU4(pu4, &pcode[pc->offset]);
    jitmp1 = GETRI4(pu4);
    pu4++;

    /* Retrieve 'low' table switch value */
    jitmp2 = GETRI4(pu4);
    pu4++;

    /* Retrieve 'high' table switch value */
    jitmp3 = GETRI4(pu4);
    pu4++;

    /* Retrieve switch table 'index' operand */
    POP(thridx, jitmp5, jint);

    /*
     * If operand out of range, select default offset
     * (currently in 'jitmp1'), else index the table
     * for target offset.
     */
    if ((jitmp5 >= jitmp2) && (jitmp5 <= jitmp3))
    {
        /* 'pu4' now points to 'jump offsets' area of opcode */
        jitmp1 = pu4[jitmp5 - jitmp2];
        MACHINE_JINT_SWAP(jitmp1);
    }
    pc->offset = jptmp + jitmp1;
    break;

case OPCODE_AB_LOOKUPSWITCH:
 /*! @todo HARMONY-6-jvm-opcode.c-125 Needs unit testing w/ real data */
    SWITCH_PAD_PC;

    /* Retrieve 'default' table switch value */
    MAKE_PU4(pu4, &pcode[pc->offset]);
    jitmp1 = GETRI4(pu4);
    pu4++;

    /* Retrieve 'npairs' table switch value */
    jitmp2 = GETRI4(pu4);
    pu4++;

    /* Retrieve lookup table 'key' operand */
    POP(thridx, jitmp5, jint);

    /* 'pu4' now points to 'match-offset pairs' area of opcode */

    /*!
     * @todo HARMONY-6-jvm-opcode.c-126 On the @b ASSUMPTION that
     *       the 'match-offset pairs' are stored with the 'match'
     *       as the @e first @link #u4 u4@endlink value and the
     *       'offset' as the @e second @link #u4 u4@endlink value,
     *       then the following @c @b for() loop should be correct.
     *       If these values are stored the other way around,
     *       then the @c @b pu4[] indices need to be swapped.
     *
     */
#define LOOKUPSWITCH_MATCH_INDEX  0
#define LOOKUPSWITCH_OFFSET_INDEX 1

    /* Scan lookup table (if 'npairs' is 0, no for() loop iterations) */
    rbool1 = rfalse;
    for (jitmp3 = 0; jitmp3 < jitmp2; jitmp3++)
    {
        /* Compare against 'match' value */

        /* 2 = 1 match (u4) + 1 offset (u4), 0+ means first word */
        jitmp4 = pu4[LOOKUPSWITCH_MATCH_INDEX + 2 * jitmp3];
        MACHINE_JINT_SWAP(jitmp4);

        /* Done if match found */
        if (jitmp4 == jitmp5)
        {
            /* 2 = 1 match (u4) + 1 offset (u4), 1+ means second word */
            jitmp4 = pu4[LOOKUPSWITCH_OFFSET_INDEX + 2 * jitmp3];
            MACHINE_JINT_SWAP(jitmp4);

            /* Match found, new PC offset loaded */
            pc->offset = jptmp + jitmp4;
            rbool1 = rtrue;
            break;
        }
    }

    /* Select default if match with 'key' operand not found */
    if (rfalse == rbool1)
    {
        pc->offset = jptmp + jitmp1;
    }
    break;

case OPCODE_AC_IRETURN:
case OPCODE_AD_LRETURN:
case OPCODE_AE_FRETURN:
case OPCODE_AF_DRETURN:
case OPCODE_B0_ARETURN:
case OPCODE_B1_RETURN:
 /*! @todo HARMONY-6-jvm-opcode.c-128 Needs unit testing w/ real data */

    /* Pop return code from this method's operand stack */
    switch(opcode)
    {
        case OPCODE_AC_IRETURN:
        case OPCODE_AE_FRETURN:
        case OPCODE_B0_ARETURN:
            POP(thridx, jitmp1, jint);
            break;

        case OPCODE_AD_LRETURN:
        case OPCODE_AF_DRETURN:
            POP(thridx, jitmp2, jint);
            POP(thridx, jitmp1, jint);
            break;

        case OPCODE_B1_RETURN:
            break;
    }

    clsidxmisc = GET_PC_FIELD_IMMEDIATE(thridx, clsidx);
    mthidxmisc = GET_PC_FIELD_IMMEDIATE(thridx, mthidx);
    pmth       = METHOD(clsidxmisc, mthidxmisc);

    /*
     * If synchronized method, release MLOCK.
     */
    if (ACC_SYNCHRONIZED & pmth->access_flags)
    {
        (rvoid) objectutil_unsynchronize(
                    CLASS(clsidxmisc).class_objhash,
                    thridx);
    }

    /*!
     * @todo  HARMONY-6-jvm-opcode.c-60 Implement test for same
     *        number of locks/unlocks per JVM spec section 8.13.
     *        This applies to @e all of the @c @b xRETURN opcodes.
     */

    POP_FRAME(thridx);

    /*!
     * @todo  HARMONY-6-jvm-opcode.c-127 Value set conversion is
     *        @e not performed on floating point values at return time.
     */

    /* Push return code onto calling method's operand stack */
    switch(opcode)
    {
        case OPCODE_AC_IRETURN:
        case OPCODE_AE_FRETURN:
        case OPCODE_B0_ARETURN:
            PUSH(thridx, jitmp1);
            break;

        case OPCODE_AD_LRETURN:
        case OPCODE_AF_DRETURN:
            PUSH(thridx, jitmp1);
            PUSH(thridx, jitmp2);
            break;

        case OPCODE_B1_RETURN:
            break;
    }

    opcode_end_thread_test(thridx, &opcode_end_thread_nonlocal_return);

    /* Restore class and method context of old method */
    LOAD_METHOD_CONTEXT;

    break;

case OPCODE_B2_GETSTATIC:
    case_opcode_b4_getfield:

    /* Retrieve the @c @b constant_pool (u2) operand */
    GET_U2_OPERAND(op1u2);

    /* Must reference a field */
    CHECK_CP_TAG(op1u2, CONSTANT_Fieldref);

    /* calc clsidxmisc and pcpd and pcpd_Fieldref */
    CALCULATE_FIELD_INFO_FROM_FIELD_REFERENCE(op1u2);

    /* Must be a valid reference to a field */
    CHECK_VALID_FIELDLOOKUPIDX(fluidxmisc);

    switch (opcode)
    {
        case OPCODE_B2_GETSTATIC:
            /* Must be a static field */
            CHECK_STATIC_FIELD;

            /*!
             * @todo HARMONY-6-jvm-opcode.c-131 Does this check for
             *       final field also apply to an object instance
             *       field, or is it for class static fields only?
             */

            /* If it is a final field, it must be in the current class*/
            CHECK_FINAL_FIELD_CURRENT_CLASS;

            /* Retrieve data from the class static field now */
            GETDATA(CLASS(pcpd_Fieldref
                            ->LOCAL_Fieldref_binding
                              .clsidxJVM)
                      .class_static_field_data[fluidxmisc]);
            break;

        case OPCODE_B4_GETFIELD:
            /*!
             * @todo HARMONY-6-jvm-opcode.c-132 Needs unit
             *       testing w/ real data
             */

            /* Must be an object instance field */
            CHECK_INSTANCE_FIELD;

            /* Retrieve data from the object instance field now */
            POP(thridx, jotmp1, jvm_object_hash);
            VERIFY_OBJECT_HASH(jotmp1);
            GETDATA(OBJECT(jotmp1)
                      .object_instance_field_data[fluidxmisc]);
            break;
    }
    break;

case OPCODE_B3_PUTSTATIC:
    case_opcode_b5_putfield:

    /* Retrieve the @c @b constant_pool (u2) operand */
    GET_U2_OPERAND(op1u2);

    /* Must reference a field */
    CHECK_CP_TAG(op1u2, CONSTANT_Fieldref);

    /* calc clsidxmisc and pcpd and pcpd_Fieldref */
    CALCULATE_FIELD_INFO_FROM_FIELD_REFERENCE(op1u2);

    /* Must be a valid reference to a field */
    CHECK_VALID_FIELDLOOKUPIDX(fluidxmisc);

    switch (opcode)
    {
        case OPCODE_B3_PUTSTATIC:
            /* Must be a static field */
            CHECK_STATIC_FIELD;

            /*!
             * @todo HARMONY-6-jvm-opcode.c-133 Does this check for
             *       final field also apply to an object instance
             *       field, or is it for class static fields only?
             */

            /* If it is a final field, it must be in the current class*/
            CHECK_FINAL_FIELD_CURRENT_CLASS;

            /* Store data into the static field now */
            PUTDATA(CLASS(pcpd_Fieldref
                            ->LOCAL_Fieldref_binding.clsidxJVM)
                    .class_static_field_data[fluidxmisc]);
            break;

        case OPCODE_B5_PUTFIELD:
            /*!
             * @todo HARMONY-6-jvm-opcode.c-134 Needs unit
             *       testing w/ real data
             */

            /* Must be an object instance field */
            CHECK_INSTANCE_FIELD;

            /* Store data into the object instance field now */
            POP(thridx, jotmp1, jvm_object_hash);
            VERIFY_OBJECT_HASH(jotmp1);
            PUTDATA(OBJECT(jotmp1)
                    .object_instance_field_data[fluidxmisc]);
            break;
    }
    break;

case OPCODE_B4_GETFIELD:
    goto case_opcode_b4_getfield;

case OPCODE_B5_PUTFIELD:
    goto case_opcode_b5_putfield;

case OPCODE_B6_INVOKEVIRTUAL:
    /* Retrieve the @c @b constant_pool (u2) operand */
    GET_U2_OPERAND(op1u2);

    goto case_opcode_b6_invokevirtual;

case OPCODE_B7_INVOKESPECIAL:
    /* Retrieve the @c @b constant_pool (u2) operand */
    GET_U2_OPERAND(op1u2);

    goto case_opcode_b7_invokespecial;

case OPCODE_B8_INVOKESTATIC:
    /* Retrieve the @c @b constant_pool (u2) operand */
    GET_U2_OPERAND(op1u2);

    case_opcode_b6_invokevirtual:
    case_opcode_b7_invokespecial:
    case_opcode_b9_invokeinterface:

    /* Must reference a method */
    CHECK_CP_TAG(op1u2, CONSTANT_Methodref);

    /*
     * Calc clsidxmisc and pcpd and pcpd_Methodref.  If class
     * has not yet been loaded, do so now.  The spec says to
     * do it following the check for an abstract method, but
     * in this implementation, the loading has to be done
     * before the testing in case the method is not in this
     * class but in a superclass or superinterface.
     */
    pcpd           = pcfs->constant_pool[op1u2];
    pcpd_Methodref = PTR_THIS_CP_Methodref(pcpd);
    clsidxmisc     = pcpd_Methodref->LOCAL_Methodref_binding.clsidxJVM;

    /*
     * Try to resolve this class before attempting to load.
     * It could be that it has been loaded but is not yet
     * resolved enough.
     */
    if (jvm_class_index_null == clsidxmisc)
    {
        (rvoid) linkage_resolve_class(GET_PC_FIELD_IMMEDIATE(thridx,
                                                             clsidx),
                                      rfalse);

        clsidxmisc = pcpd_Methodref->LOCAL_Methodref_binding.clsidxJVM;

        if (jvm_class_index_null == clsidxmisc)
        {
            /* If class is not loaded, retrieve it by UTF8 class name */
            LATE_CLASS_LOAD(pcpd_Methodref->class_index);

            /* Check if method exists in loaded class */
            clsidxmisc = pcpd_Methodref
                           ->LOCAL_Methodref_binding.clsidxJVM;
            if (jvm_class_index_null == clsidxmisc)
            {
                thread_throw_exception(thridx,
                                       THREAD_STATUS_THREW_ERROR,
                                JVMCLASS_JAVA_LANG_ABSTRACTMETHODERROR);
/*NOTREACHED*/
            }
        }
    }

    mthidxmisc = pcpd_Methodref->LOCAL_Methodref_binding.mthidxJVM;

    if (jvm_method_index_bad == mthidxmisc)
    {
        thread_throw_exception(thridx,
                               THREAD_STATUS_THREW_ERROR,
                                JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR);
/*NOTREACHED*/
    }

    pcfsmisc = CLASS_OBJECT_LINKAGE(clsidxmisc)->pcfs;
    pmth     = pcfsmisc->methods[mthidxmisc];

    rbool1 = IS_INIT_METHOD(pcfsmisc, pmth->name_index);

    /* Must be a valid reference to a method */
    if (jvm_attribute_index_bad == 
        pcpd_Methodref->LOCAL_Methodref_binding.codeatridxJVM)
    {
        thread_throw_exception(thridx,
                               THREAD_STATUS_THREW_ERROR,
                               JVMCLASS_JAVA_LANG_NOSUCHMETHODERROR);
/*NOTREACHED*/
    }

    switch(opcode)
    {
        case OPCODE_B6_INVOKEVIRTUAL:
            /* Must not be a class initialization method */
            CHECK_NOT_CLINIT_METHOD;
            CHECK_NOT_INIT_METHOD;

            /* Must be an instance method */
            CHECK_INSTANCE_METHOD;

            /* Must not be an abstract method */
            CHECK_NOT_ABSTRACT_METHOD;
            break;

        case OPCODE_B7_INVOKESPECIAL:
            /* Must be an instance method */
            CHECK_INSTANCE_METHOD;

            /* Must not be an abstract method */
            CHECK_NOT_ABSTRACT_METHOD;
            break;

        case OPCODE_B8_INVOKESTATIC:
            /* Must not be a class or instance initialization method */
            CHECK_NOT_CLINIT_METHOD;
            CHECK_NOT_INIT_METHOD;

            /* Must be a static method */
            CHECK_STATIC_METHOD;

            /* Must not be an abstract method */
            CHECK_NOT_ABSTRACT_METHOD;
            break;

        case OPCODE_B9_INVOKEINTERFACE:
            /* Must not be a class or instance initialization method */
            CHECK_NOT_CLINIT_METHOD;
            CHECK_NOT_INIT_METHOD;

            /* @c @b count must be non-zero */

            /*!
             * @todo HARMONY-6-jvm-opcode.c-145 What is the purpose of
             *       the 'count' operand besides taking space in the
             *       code area that must be parsed?  Ths spec says
             *       nothing beyond the fact that it is an unsigned
             *       byte that must not be zero.  There is something
             *       written out there somewhere about this, but what
             *       it is apparently is not that important.  (?)
             */
            if (0 == op2u1)
            {
                thread_throw_exception(thridx,
                                       THREAD_STATUS_THREW_ERROR,
                                       JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
            }

            /*!
             * @todo HARMONY-6-jvm-opcode.c-146 What is the purpose of
             *       the fourth unnamed operand besides taking space in
             *       the code area that must be parsed?  Ths spec says
             *       nothing beyond the fact that it is an unsigned
             *       byte that must be zero.
             */
            if (0 != op3u1)
            {
                thread_throw_exception(thridx,
                                       THREAD_STATUS_THREW_ERROR,
                                       JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
            }

            /* Must a public method */
            CHECK_PUBLIC_METHOD;
            break;
    }

    if (ACC_NATIVE & pmth->access_flags)
    {
        /*!
         * @todo HARMONY-6-jvm-opcode.c-136 Needs unit testing
         *       w/ real data
         */

        /* This macro conditionally uses 'break' to exit the switch() */
        SYNCHRONIZE_METHOD_INVOCATION;

        /* Return code, if any, is pushed in the JNI processing code */
        native_run_method(thridx,
                          clsidxmisc,
                          pcpd_Methodref
                            ->LOCAL_Methodref_binding
                              .nmordJVM,
                          pmth->name_index,
                          pmth->descriptor_index,
                          pmth->access_flags,
                          opcode,
                          rbool1);

        if (ACC_SYNCHRONIZED & pmth->access_flags)
        {
            /*!
             * @todo HARMONY-6-jvm-opcode.c-135 Notice that the
             *       spec for this instruction implies that the
             *       monitor exit happens @e before the return
             *       code is pushed onto the stack.  At this time,
             *       these actions are done the other way around.
             *       Does this need to change?
             *
             */
            (rvoid) objectutil_unsynchronize(
                        CLASS(clsidxmisc).class_objhash,
                        thridx);
        }
    }
    else
    {
        /*!
         * @internal Start up Java code.  Instead of using POP() to
         *           retrieve parameters and load them into the stack
         *           frame of the invoked method, simply reference
         *           the first parameter (bottom of the operand stack)
         *           as local variable 0 and build a stack frame above
         *           the pushed parameters.  (This will entail changing
         *           the size of the requested frame by this block size
         *           followed by a simple adjustment to the number of
         *           local variables that are reported in the stack
         *           frame.)
         *
         */
        jitmp1 = method_parm_size(clsidxmisc, 
                                  pmth->descriptor_index);

        jitmp2 = (ACC_STATIC & pmth->access_flags)
                 ? (((0 * sizeof(jvm_object_hash)) / sizeof(jint)))
                 : (((1 * sizeof(jvm_object_hash)) / sizeof(jint)));

        jvm_attribute_index codeatridx =
            pcfsmisc
              ->methods[mthidxmisc]
                ->LOCAL_method_binding.codeatridxJVM;

        pca = (Code_attribute *)
                &pcfsmisc->methods[mthidxmisc]->attributes[codeatridx];

        /* If less local storage than parameters, somebody goofed */
        if (pca->max_locals < jitmp1)
        {
            thread_throw_exception(thridx,
                                   THREAD_STATUS_THREW_ERROR,
                                   JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
        }

        /*!
         * @internal Build stack frame with parameters in place as
         *           local variables, but increase the length if the
         *           method is not a a @c @b static method to account
         *           for the object's @c @b this object hash as the
         *           first word on the stack for the method call.
         *           Thus the stack size must be adjusted by one (jint)
         *           word, the size of an object hash.
         */
        PUSH_FRAME(thridx, pca->max_locals - jitmp1);

        /* Load target program counter */
        PUT_PC_IMMEDIATE(thridx,
                         clsidxmisc,
                         mthidxmisc,
                         codeatridx,
                         pcfsmisc
                           ->methods[mthidxmisc]
                             ->LOCAL_method_binding.excpatridxJVM,
                         CODE_CONSTRAINT_START_PC);

        /* Store proper local var size now */
        PUT_FP_WORD(thridx,
                    JVMREG_STACK_LS_OFFSET,
                    pca->max_locals + jitmp2);

        /* Set class and method context to point to new method */
        LOAD_METHOD_CONTEXT;

        switch(opcode)
        {
            case OPCODE_B6_INVOKEVIRTUAL:
            case OPCODE_B7_INVOKESPECIAL:
                /*!
                 * Local variable 0 is the object reference
                 * for @c @b this object.
                 */
                objhashmisc = GET_LOCAL_VAR(thridx,0);

                /*!
                 * @internal The null reference and access_flags logic
                 *           is also implemented in the macro @link
                             #POP_THIS_OBJHASH POP_THIS_OBJHASH@endlink
                 *           in the file
                 *           @link jvm/src/native.c native.c@endlink
                 *           for native methods.
                 *
                 * @todo HARMONY-6-jvm-opcode.c-139 The spec is unclear
                 *       as to what happens in the case that the object
                 *       reference is not of the current class or one
                 *       of its subclasses.  It is @e assumed that
                 *       @b VerifyError should be thrown.  Is this
                 *       a valid assumption?
                 *
                 */
        
                /* Test for reasonable contents and non-null in object*/
                VERIFY_OBJECT_HASH(objhashmisc);

                if ((ACC_PROTECTED & pmth->access_flags) &&
                    (rtrue ==
                     classutil_class_is_a(
                         GET_PC_FIELD_IMMEDIATE(thridx, clsidx),
                         clsidxmisc)))
                {
                    if (rfalse ==
                        classutil_class_is_a(
                            OBJECT_CLASS_LINKAGE(objhashmisc)->clsidx,
                            GET_PC_FIELD_IMMEDIATE(thridx, clsidx)))
                    {
                        /* Somebody goofed */
                        thread_throw_exception(thridx,
                            THREAD_STATUS_THREW_ERROR,
                            JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
                    }
                }
                break;

            case OPCODE_B8_INVOKESTATIC:
                break;

            case OPCODE_B9_INVOKEINTERFACE:
                break;
        }

        CHECK_OBJECT_CLASS_STRUCTURE(clsidxmisc, objhashmisc, rbool1);

        /*!
         * @todo HARMONY-6-jvm-opcode.c-137 Needs unit testing
         *       w/ real data
         */

        /* This macro conditionally uses 'break' to exit the switch() */
        SYNCHRONIZE_METHOD_INVOCATION;
    }
    break;

case OPCODE_B9_INVOKEINTERFACE:
 /*! @todo HARMONY-6-jvm-opcode.c-147 Needs unit testing w/ real data */

    /*
     * Retrieve the @c @b constant_pool (u2) operand and a pair
     * of (u1) operands, the first being the @c @b count and
     * the second being a placeholder containing zero.
     */
    GET_U2_OPERAND(op1u2);
    GET_U1_OPERAND(op2u1);
    GET_U1_OPERAND(op3u1);

    goto case_opcode_b9_invokeinterface;

case OPCODE_BA_XXXUNUSEDXXX1:
                                /* Don't like 'goto', but makes sense */
    goto case_opcode_ba_xxxunusedxxx1;

case OPCODE_BB_NEW:

    /* Retrieve the @c @b constant_pool (u2) operand */
    GET_U2_OPERAND(op1u2);

    /* Must reference a normal class, not an array or interface class */
    CHECK_CP_TAG(op1u2, CONSTANT_Class);
    CHECK_NOT_ABSTRACT_CLASS;
    CHECK_NOT_ARRAY_OBJECT;
    CHECK_NOT_INTERFACE_CLASS;

    /* calc clsidxmisc and pcpd and pcpd_Class and pcfsmisc */
    CALCULATE_CLASS_INFO_FROM_CLASS_REFERENCE(op1u2);

    /* Create new object from this class */
    special_obj_misc = OBJECT_STATUS_EMPTY;

    if (0 == utf_prchar_classname_strcmp(JVMCLASS_JAVA_LANG_STRING,
                                         pcfsmisc,
                                         pcfsmisc->this_class))
    {
        special_obj_misc |= OBJECT_STATUS_STRING;

        /*
         * @internal Notice that 'utf8string' parameter will
         *           be unused here since the @c @b \<init\> method
         *           will be used to set them later.
         */
    }
    else
    if (0 == utf_prchar_classname_strcmp(JVMCLASS_JAVA_LANG_THREAD,
                                         pcfsmisc,
                                         pcfsmisc->this_class))
    {
        special_obj_misc |= OBJECT_STATUS_THREAD;
    }

    objhashmisc =
        object_instance_new(special_obj_misc,
                            pcfsmisc,
                            clsidxmisc,
                            LOCAL_CONSTANT_NO_ARRAY_DIMS,
                            (rvoid *) rnull,

                            /* Done by subsequent INVOKESPECIAL */
                            rfalse,

                            thridx,
                            (CONSTANT_Utf8_info *) rnull);

    /* Store result to stack */
    PUSH(thridx, (jint) objhashmisc);

    break;

case OPCODE_BC_NEWARRAY:
 /*! @todo HARMONY-6-jvm-opcode.c-140 Needs unit testing w/ real data */

    /* Retrieve the (u1) array type operand 'atype' */
    GET_U1_OPERAND(op1u1);

    switch(op1u1)
    {
        case CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_BOOLEAN:
            clsidxmisc = pjvm->class_primative_boolean;
            break;
        case CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_CHAR:
            clsidxmisc = pjvm->class_primative_char;
            break;
        case CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_FLOAT:
            clsidxmisc = pjvm->class_primative_float;
            break;
        case CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_DOUBLE:
            clsidxmisc = pjvm->class_primative_double;
            break;
        case CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_BYTE:
            clsidxmisc = pjvm->class_primative_byte;
            break;
        case CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_SHORT:
            clsidxmisc = pjvm->class_primative_short;
            break;
        case CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_INT:
            clsidxmisc = pjvm->class_primative_int;
            break;
        case CODE_CONSTRAINT_OP_NEWARRAY_TYPE_T_LONG:
            clsidxmisc = pjvm->class_primative_long;
            break;
        default:
            /* 'atype' field is not recognized */
            thread_throw_exception(thridx,
                                   THREAD_STATUS_THREW_ERROR,
                                   JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
    }

    /* Retrieve 'count' operand from TOS */
    POP(thridx, jitmp1, jint);

    /* Cannot have negative number of array elements (zero is okay) */
    if (0 > jitmp1)
    {
        thread_throw_exception(thridx,
                               THREAD_STATUS_THREW_EXCEPTION,
                         JVMCLASS_JAVA_LANG_NEGATIVEARRAYSIZEEXCEPTION);
/*NOTREACHED*/
    }

    pcfsmisc = CLASS_OBJECT_LINKAGE(clsidxmisc)->pcfs;

    /* Notice that @b NEWARRAY only handles a single array dimension */
    objhashmisc = object_instance_new(OBJECT_STATUS_ARRAY,
                                      pcfsmisc,
                                      clsidxmisc,
                                      1,
                                      &jitmp1,

                                      /* Irrelevant for primatives */
                                      rfalse,

                                      thridx,
                                      (CONSTANT_Utf8_info *) rnull);

    /* Store result to stack */
    PUSH(thridx, (jint) objhashmisc);

    break;

case OPCODE_BD_ANEWARRAY:

    /* Retrieve the @c @b constant_pool (u2) operand */
    GET_U2_OPERAND(op1u2);

    /*!
     * @todo HARMONY-6-jvm-opcode.c-67 Make sure that @e all of
     *       "class, array, or interface type" is supported by this
     *       test:
     */

    /* Must reference a class */
    CHECK_CP_TAG(op1u2, CONSTANT_Class);
    /* CHECK_CP_TAG2/3(op1u2, CONSTANT_Class,array? ,interface? ); */

    /* calc clsidxmisc and pcpd and pcpd_Class and pcfsmisc */
    CALCULATE_CLASS_INFO_FROM_CLASS_REFERENCE(op1u2);

    /* Retrieve 'count' operand from TOS */
    POP(thridx, jitmp1, jint);

    /* Cannot have negative number of array elements (zero is okay) */
    if (0 > jitmp1)
    {
        thread_throw_exception(thridx,
                               THREAD_STATUS_THREW_EXCEPTION,
                         JVMCLASS_JAVA_LANG_NEGATIVEARRAYSIZEEXCEPTION);
/*NOTREACHED*/
    }

    /* Create new object from this class, array, or interface */

    special_obj_misc = OBJECT_STATUS_ARRAY;

    /*!
     * @todo HARMONY-6-jvm-opcode.c-142 Is this meaningful, given
     *       that the final parameter to object_instance_new() is
     *       always null?  If so, uncomment this block.
     */
#if 0
    if (0 == utf_prchar_classname_strcmp(JVMCLASS_JAVA_LANG_STRING,
                                         pcfsmisc,
                                         pcfsmisc->this_class))
    {
        special_obj_misc |= OBJECT_STATUS_STRING;

        /*
         * @internal Notice that 'utf8string' parameter will
         *           be unused here since the @c @b \<init\> method
         *           will be used to set them later.
         */
    }
    else
#endif
    if (0 == utf_prchar_classname_strcmp(JVMCLASS_JAVA_LANG_THREAD,
                                         pcfsmisc,
                                         pcfsmisc->this_class))
    {
        special_obj_misc |= OBJECT_STATUS_THREAD;
    }

    /* Notice that @b ANEWARRAY only handles a single array dimension */
    objhashmisc =
        object_instance_new(special_obj_misc,
                            pcfsmisc,
                            clsidxmisc,

                            /*!
                             * @todo HARMONY-6-jvm-opcode.c-69 Should
                             *       this be simply '1' ?
                             */
                            ((OBJECT_STATUS_ARRAY & special_obj_misc)
                                 ? 1
                                 : 0),
                            &jitmp1,

                            /*
                             * Although an array has nothing to use
                             * an \<init\> method for, its superclasses
                             * very well may, so run their \<init\>
                             * methods.
                             */
                            rtrue,

                            thridx,
                            (CONSTANT_Utf8_info *) rnull);

    /* Store result to stack */
    PUSH(thridx, (jint) objhashmisc);

    break;

case OPCODE_BE_ARRAYLENGTH:
 /*! @todo HARMONY-6-jvm-opcode.c-141 Needs unit testing w/ real data */
    POP(thridx, jotmp1, jvm_object_hash);

    VERIFY_OBJECT_HASH(jotmp1);

    /*!
     * @todo HARMONY-6-jvm-opcode.c-148 The spec is ambiguous on what
     *       to do if an object reference is not an array or perhaps
     *       a valid array.  Which of these conditions, if any, should
     *       be checked?  Uncomment those that should be done.
     */
    if (!(OBJECT_STATUS_ARRAY & OBJECT(jotmp1).status))
    {
        /* Sorry, this is not a valid array object */
        thread_throw_exception(thridx,
                               THREAD_STATUS_THREW_ERROR,
                               JVMCLASS_JAVA_LANG_INTERNALERROR);
/*NOTREACHED*/
    }
#if 0
    else
    if (1 > OBJECT(jotmp1).arraydims)
    {
        /* Sorry, this is array object has a bad geometry */
        thread_throw_exception(thridx,
                               THREAD_STATUS_THREW_ERROR,
                               JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
    }
    else
    if (0 > OBJECT(jotmp1).arraylength)
    {
        /* Sorry, this is array object has a bad geometry */
        thread_throw_exception(thridx,
                               THREAD_STATUS_THREW_EXCEPTION,
                         JVMCLASS_JAVA_LANG_NEGATIVEARRAYSIZEEXCEPTION);
/*NOTREACHED*/
    }
#endif

    jitmp2 = OBJECT(jotmp1).arraylength;
    PUSH(thridx, jitmp2);
    break;

case OPCODE_BF_ATHROW:
    /*! @todo HARMONY-6-jvm-opcode.c-72 Write this opcode */
    STUB;
    break;

case OPCODE_C0_CHECKCAST:
case OPCODE_C1_INSTANCEOF:
 /*! @todo HARMONY-6-jvm-opcode.c-130 Needs unit testing w/ real data */

    /*
     * Retrieve the @c @b constant_pool (u2) operand.
     * The spec refers to this as an 'objectref' of type 'S'
     */
    GET_U2_OPERAND(op1u2);

    /* Must reference a normal class, an array, or an interface class */
    CHECK_CP_TAG(op1u2, CONSTANT_Class);

    /*
     * Locate or load class being requested.
     * The spec refers to this as type 'T'
     */
    clsidxmisc2 =
        class_load_from_cp_entry_utf(
            pcfs
              ->constant_pool
                [PTR_THIS_CP_Class(pcfs->constant_pool[op1u2])
                   ->name_index],
            rfalse,
            (jint *) rnull);

    /* Retrieve object reference to examine */
    POP(thridx, jotmp1, jvm_object_hash);

    /* Initialize result code, then go test INSTANCEOF conditions */
    jitmp2 = 0;

    if (jvm_object_hash_null == jotmp1)
    {

        switch(opcode)
        {
            case OPCODE_C0_CHECKCAST:

                jitmp2 = 1; /* No failure */
                break;

            case OPCODE_C1_INSTANCEOF:

                /* Nothing to do, just PUSH() result below*/
                break;
        }
    }
    else
    if (OBJECT(jotmp1).status &
        (OBJECT_STATUS_NULL | OBJECT_STATUS_GCREQ))
    {
        /* Sorry, this is not a valid object */
        thread_throw_exception(thridx,
                               THREAD_STATUS_THREW_ERROR,
                               JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
    }
    else
    {
        clsidxmisc = OBJECT_CLASS_LINKAGE(jotmp1)->clsidx;

        pcfsmisc   = OBJECT_CLASS_LINKAGE(jotmp1)->pcfs;

        /*
         * If S is a class representing the array type SC[], that is,
         * an array of components of type SC, then:
         */
        if (CLASS(clsidxmisc).status & CLASS_STATUS_ARRAY)
        {
            pcfsmisc = CLASS_OBJECT_LINKAGE(clsidxmisc2)->pcfs;

            /*
             * If T is an array type TC[], that is, an array of
             * components of type TC, then one of the following
             * must be true:
             */
            if (CLASS(clsidxmisc2).status & CLASS_STATUS_ARRAY)
            {
                /*
                 * TC and SC are the same primative type (sec 2.4.1).
                 */
                if ((CLASS(clsidxmisc).status & CLASS_STATUS_PRIMATIVE)
                    &&
                    (CLASS(clsidxmisc2).status & CLASS_STATUS_PRIMATIVE)
                    &&
                    (clsidxmisc2 == clsidxmisc))
                {
                    jitmp2 = 1;
                }
                else
                /*!
                 * @todo HARMONY-6-jvm-opcode.c-129 Resolve the
                 *       VM spec language differences and change
                 *       code for conformity if needed.
                 *
                 * @internal Slightly different spec statements here.
                 *           It is @e assumed that they mean the same
                 *           thing since @e all other algorithmic
                 *           comments are identical on these two
                 *           operation code:
                 *
                 * <ul><li>@b CHECKCAST opcode:
                 * TC and SC are reference types (sec 2.4.6), and
                 * type SC can be cast to TC by recursive application
                 * of these rules.
                 *
                 * </li>
                 *
                 * <li>@b INSTANCEOF opcode:
                 * TC and SC are reference types (sec 2.4.6), and
                 * type SC can be cast to TC by these runtime rules.
                 *
                 * </li></ul>
                 *
                 */
                if (
                  (!(CLASS(clsidxmisc).status & CLASS_STATUS_PRIMATIVE))
                    &&
                (!(CLASS(clsidxmisc2).status & CLASS_STATUS_PRIMATIVE)))
                {
                       /* class S is a T */
                   if ((rtrue ==
                        classutil_class_is_a(clsidxmisc,clsidxmisc2))
                       ||
                       /* class S implements interface T */
                       (rtrue ==
                        classutil_class_implements_interface(clsidxmisc,
                                                           clsidxmisc2))
                       ||
                       /* interface T is a superinterface of S */
                       (rtrue ==
                       classutil_class_is_superinterface_of(clsidxmisc2,
                                                           clsidxmisc)))
                    {
                        jitmp2 = 1;
                    }
                }
            }
            else
            /*
             * If T is an interface type, T must be one of the
             * interfaces implemented by arrays (sec 2.15)
             */
            if (pcfsmisc->access_flags & ACC_INTERFACE)
            {
                if (rtrue ==
                  classutil_interface_implemented_by_arrays(clsidxmisc))
                {
                    jitmp2 = 1;
                }
            }
            else
            /*
             * If T is a class type, then T must be java.lang.Object
             * (sec 2.4.7)
             */
            {
                if (clsidxmisc2 == pjvm->class_java_lang_Object)
                {
                    jitmp2 = 1;
                }
            }
        }
        else
        /*
         * If S is an interface type, then:
         */
        if (pcfsmisc->access_flags & ACC_INTERFACE)
        {
            pcfsmisc = CLASS_OBJECT_LINKAGE(clsidxmisc2)->pcfs;

            /*
             * If T is an interface type, then T must be the same
             * interface as S, or a super interface of S (sec 2.13.2).
             */
            if (pcfsmisc->access_flags & ACC_INTERFACE)
            {
                if (rtrue ==
                    classutil_class_is_superinterface_of(clsidxmisc2,
                                                         clsidxmisc))
                {
                    jitmp2 = 1;
                }
            }
            else
            /*
             * If T is a class type, then T must be java.lang.Object
             * (sec 2.4.7).
             */
            {
                if (clsidxmisc2 == pjvm->class_java_lang_Object)
                {
                    jitmp2 = 1;
                }
            }
        }
        else
        /*
         * If S is an ordinary (noarray) class, then:
         */
        {
            pcfsmisc = CLASS_OBJECT_LINKAGE(clsidxmisc2)->pcfs;

            /*
             * If T is an interface type, then S must implement
             * (sec 2.13) interface T.
             */
            if (pcfsmisc->access_flags & ACC_INTERFACE)
            {
                if (rtrue ==
                    classutil_class_implements_interface(clsidxmisc,
                                                         clsidxmisc2))
                {
                    jitmp2 = 1;
                }
            }
            else
            /*
             * If T is a class type, then S must be the same class
             * (sec 2.8.1) as T or a subclass of T.
             */
            {
                if (rtrue ==
                    classutil_class_is_a(clsidxmisc, clsidxmisc2))
                {
                    jitmp2 = 1;
                }
            }
        }
    }

    /* Process the result differently for each opcode */
    switch(opcode)
    {
        case OPCODE_C0_CHECKCAST:
            /* Throw exception if 'objectref' is not an instance of T */
            if (0 == jitmp2)
            {
                thread_throw_exception(thridx,
                                       THREAD_STATUS_THREW_EXCEPTION,
                                 JVMCLASS_JAVA_LANG_CLASSCASTEXCEPTION);
/*NOTREACHED*/
            }
            break;

        case OPCODE_C1_INSTANCEOF:
            /* Push result code onto stack  */
            PUSH(thridx, jitmp2);
            break;
    }
    break;

case OPCODE_C2_MONITORENTER:
    POP(thridx, jotmp1, jvm_object_hash);

    (rvoid) objectutil_synchronize(jotmp1, thridx);
    break;

case OPCODE_C3_MONITOREXIT:
    POP(thridx, jotmp1, jvm_object_hash);

    (rvoid) objectutil_unsynchronize(jotmp1, thridx);
    break;

case OPCODE_C4_WIDE:
    /*!
     * @todo HARMONY-6-jvm-opcode.c-77 Test this opcode and
     *       those that reference @c @b iswide for operand size.
     */
    iswide = rtrue;  /* Will be read then cleared by other opcodes */
    break;

case OPCODE_C5_MULTIANEWARRAY:
    /*
     * Retrieve the @c @b constant_pool (u2) operand
     * and the (u1) 'dimensions' operand.
     */
    GET_U2_OPERAND(op1u2);
    GET_U1_OPERAND(op2u1);

    /* Cannot have a zero-dimensional array or it is a scalar instead */
    if (1 > op2u1)
    {
        thread_throw_exception(thridx,
                               THREAD_STATUS_THREW_ERROR,
                               JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
    }

    /* Convert unsigned byte to more convenient signed integer */
    jitmp1 = ((jint) (juint) op2u1);

    /*!
     * @internal Not found in the spec, and should @e never happen,
     *           but if the number of dimensions is greater than
     *           the alleged current depth of the stack pointer,
     *           then the parameters were not stacked correctly.
     *           Is this something that should be checked?
     *           If so, uncomment this block.
     */
#if 0
    if (jitmp1 > GET_SP(thridx))
    {
        thread_throw_exception(thridx,
                               THREAD_STATUS_THREW_ERROR,
                               JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
    }
#endif

    /*!
     * @internal Can @e only use this method of referencing the
     *           array dimensions with a push-up stack.  They
     *           would be in reverse order on a push-down stack when
     *           using the expression <b><code>GET_SP(thridx)</code></b>
     *           and would be beyond the top of stack using the
     *           existing expression.
     */
    pjitmp6 = &STACK(thridx, GET_SP(thridx) + 1 - jitmp1);

    /* Verify that all dimensions are non-negative */
    for (jitmp2 = 0; jitmp2 < jitmp1; jitmp2++)
    {
        if (0 > pjitmp6[jitmp2])
        {
            thread_throw_exception(thridx,
                                   THREAD_STATUS_THREW_EXCEPTION,
                         JVMCLASS_JAVA_LANG_NEGATIVEARRAYSIZEEXCEPTION);
/*NOTREACHED*/
        }
    }

    /*!
     * @todo HARMONY-6-jvm-opcode.c-149 How is a multi-dimensional
     *       array of primatives supported?  Apparently by a
     *       constant pool table entry referencing an array, e.g. '[[I'
     *       for 2-dim (int) array, perhaps 'I' for integer, but is it
     *       valid to have a CP entry of primative class (int) instead
     *       of an array?  There must be @e some way to legitimately
     *       perform a multi-dimensional array allocation for primative
     *       types.  What is it?
     */

    /* Must reference a normal class, an array, or an interface class */
    CHECK_CP_TAG(op1u2, CONSTANT_Class);

    /*
     * Locate or load class being requested for array type.
     */
    clsidxmisc =
        class_load_from_cp_entry_utf(
            pcfs
              ->constant_pool
                [PTR_THIS_CP_Class(pcfs->constant_pool[op1u2])
                   ->name_index],
            rfalse,
            (jint *) rnull);

    /*
     * Verify that the current class may access the
     * requested array component type.
     */
    if (rfalse == classutil_class_is_accessible_to(clsidxmisc,
                                GET_PC_FIELD_IMMEDIATE(thridx, clsidx)))
    {
        /* Current class cannot access requested array type */
        thread_throw_exception(thridx,
                               THREAD_STATUS_THREW_ERROR,
                               JVMCLASS_JAVA_LANG_ILLEGALACCESSERROR);
/*NOTREACHED*/
    }

    pcfsmisc = CLASS_OBJECT_LINKAGE(clsidxmisc)->pcfs;

    special_obj_misc = OBJECT_STATUS_ARRAY;

    /*!
     * @todo HARMONY-6-jvm-opcode.c-143 Is this meaningful, given
     *       that the final parameter to object_instance_new() is
     *       always null?  If so, uncomment this block.
     */
#if 0
    if (0 == utf_prchar_classname_strcmp(JVMCLASS_JAVA_LANG_STRING,
                                         pcfsmisc,
                                         pcfsmisc->this_class))
    {
        special_obj_misc |= OBJECT_STATUS_STRING;

        /*
         * @internal Notice that 'utf8string' parameter will
         *           be unused here since the @c @b \<init\> method
         *           will be used to set them later.
         */
    }
    else
#endif
/*
    if (0 == utf_prchar_classname_strcmp(JVMCLASS_JAVA_LANG_THREAD,
                                         pcfsmisc,
                                         pcfsmisc->this_class))
    {
        special_obj_misc |= OBJECT_STATUS_THREAD;
    }
*/
    /*
     * In contrast to @b NEWARRAY and @b ANEWARRAY,
     * @b MULTINEWARRAY handles multiple dimensions.
     */
    objhashmisc =
        object_instance_new(special_obj_misc,
                            pcfsmisc,
                            clsidxmisc,
                            jitmp1,
                            pjitmp6,

                            /*
                             * Although an array has nothing to use
                             * an \<init\> method for, its superclasses
                             * very well may, so run their \<init\>
                             * methods.
                             */
                            rtrue,

                            thridx,
                            (CONSTANT_Utf8_info *) rnull);

    /*!
     * @todo HARMONY-6-jvm-opcode.c-144 What is the best error to
     *       throw in this situation?  @b InternalError ?  The spec
     *       mandates the effect, but not the error when it fails.
     */
    if (OBJECT(objhashmisc).arraydims < op2u1)
    {
        /* Result has fewer dimensions than requested geometry */
        thread_throw_exception(thridx,
                               THREAD_STATUS_THREW_ERROR,
                               JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
    }

    /* Pop all words of array geometry from stack */
    DEC_SP(thridx, jitmp1);

    /* Store result to stack */
    PUSH(thridx, (jint) objhashmisc);

    break;

case OPCODE_C6_IFNULL:
case OPCODE_C7_IFNONNULL:
    GET_U2_OPERAND(op1u2);

    POP(thridx, jotmp1, jvm_object_hash);

    if (OPCODE_C6_IFNULL)
    {
        if (jvm_object_hash_null == jotmp1)
        {
                                 /* sizes of opcode + operand */
            LOAD_TARGET_PC_OFFSET(op1u2, sizeof(u1) + sizeof(u2));
        }
    }
    else
    {
        if (jvm_object_hash_null != jotmp1)
        {
                                 /* sizes of opcode + operand */
            LOAD_TARGET_PC_OFFSET(op1u2, sizeof(u1) + sizeof(u2));
        }
    }

    break;

case OPCODE_C8_GOTO_W:
    GET_U4_OPERAND(op1u4);

                         /* sizes of opcode + operand */
    LOAD_TARGET_PC_OFFSET(op1u4, sizeof(u1) + sizeof(u4));
    break;

case OPCODE_C9_JSR_W:
    GET_U4_OPERAND(op1u4);

    /*!
     * @todo HARMONY-6-jvm-opcode.c-116 Need a better definition
     *       of type @c @b returnAddress than a simple
     *       @link #jint jint@endlink.
     *
     */
    jitmp1 = (jint) pc->offset;
    PUSH(thridx, jitmp1);

                          /* size of opcode */
    LOAD_TARGET_PC_OFFSET(op1u4, sizeof(u1));
    break;


/* Reserved opcodes: */
case OPCODE_CA_BREAKPOINT:
    /* This implementation is not currently using this opcode hook */
    thread_throw_exception(thridx,
                           THREAD_STATUS_THREW_ERROR,
                           JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
    break;


/* Undefined and unused opcodes, reserved */
case OPCODE_CB_UNUSED:
case OPCODE_CC_UNUSED:
case OPCODE_CD_UNUSED:
case OPCODE_CE_UNUSED:
case OPCODE_CF_UNUSED:

case OPCODE_D0_UNUSED:
case OPCODE_D1_UNUSED:
case OPCODE_D2_UNUSED:
case OPCODE_D3_UNUSED:
case OPCODE_D4_UNUSED:
case OPCODE_D5_UNUSED:
case OPCODE_D6_UNUSED:
case OPCODE_D7_UNUSED:
case OPCODE_D8_UNUSED:
case OPCODE_D9_UNUSED:
case OPCODE_DA_UNUSED:
case OPCODE_DB_UNUSED:
case OPCODE_DC_UNUSED:
case OPCODE_DD_UNUSED:
case OPCODE_DE_UNUSED:
case OPCODE_DF_UNUSED:

case OPCODE_E0_UNUSED:
case OPCODE_E1_UNUSED:
case OPCODE_E2_UNUSED:
case OPCODE_E3_UNUSED:
case OPCODE_E4_UNUSED:
case OPCODE_E5_UNUSED:
case OPCODE_E6_UNUSED:
case OPCODE_E7_UNUSED:
case OPCODE_E8_UNUSED:
case OPCODE_E9_UNUSED:
case OPCODE_EA_UNUSED:
case OPCODE_EB_UNUSED:
case OPCODE_EC_UNUSED:
case OPCODE_ED_UNUSED:
case OPCODE_EE_UNUSED:
case OPCODE_EF_UNUSED:

case OPCODE_F0_UNUSED:
case OPCODE_F1_UNUSED:
case OPCODE_F2_UNUSED:
case OPCODE_F3_UNUSED:
case OPCODE_F4_UNUSED:
case OPCODE_F5_UNUSED:
case OPCODE_F6_UNUSED:
case OPCODE_F7_UNUSED:
case OPCODE_F8_UNUSED:
case OPCODE_F9_UNUSED:
case OPCODE_FA_UNUSED:
case OPCODE_FB_UNUSED:
case OPCODE_FC_UNUSED:
case OPCODE_FD_UNUSED:

  case_opcode_ba_xxxunusedxxx1:

    /* These opcodes are not implemented by the JVM spec at this time */
    thread_throw_exception(thridx,
                           THREAD_STATUS_THREW_ERROR,
                           JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
    break;

/* Reserved opcodes: */
case OPCODE_FE_IMPDEP1:
    /* This implementation is not currently using this opcode hook */
    thread_throw_exception(thridx,
                           THREAD_STATUS_THREW_ERROR,
                           JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
    break;

case OPCODE_FF_IMPDEP2:
    /* This implementation is not currently using this opcode hook */
    thread_throw_exception(thridx,
                           THREAD_STATUS_THREW_ERROR,
                           JVMCLASS_JAVA_LANG_VERIFYERROR);
/*NOTREACHED*/
    break;

} /* switch(opcode) */

/* Renew indentation... */

            } /* while ... */

        } /* if nonlocal_thread_return else */

        /**************************************************************/
        /**************************************************************/
        /**************************************************************/
        /**************************************************************/
        /**************************************************************/
        /* END OF GIANT SWITCH STATEMENT if(){}else{while(){switch()}}*/
        /**************************************************************/
        /**************************************************************/
        /**************************************************************/
        /**************************************************************/
        /**************************************************************/

    } /* if nonlocal_rc else */

    /* If the timer ticked, clear flag and process next thread */
    if((rtrue == check_timeslice) && (rtrue == pjvm->timeslice_expired))
    {
        pjvm->timeslice_expired = rfalse;
    }

    /*
     * If frame is empty, thread is done running, but if
     * @link #rthread.fp_end_program fp_end_program@endlink is being
     * used to control, say, a \<clinit\> of a
     * @link #LATE_CLASS_LOAD() LATE_CLASS_LOAD@endlink, the thread is
     * still doing something even when the end-of-program indication
     * has occurred.  See also
     * @link #opcode_end_thread_test() opcode_end_thread_test()@endlink
     * for use of this same logic.
     *
     * Notice that this check does _not_ look for the fp-end-program
     * condition per @link #opcode_end_thread_test()
       opcode_end_thread_test()@endlink.  This means that the next
     * time this thread runs, execution will pick up with the next
     * instruction past a recent @c @b \<clinit\> or @c @b \<init\>
     * call.
     */
    if (CHECK_FINAL_STACK_FRAME_ULTIMATE(thridx) &&
        (THREAD_STATE_RUNNING == THREAD(thridx).this_state))
    {
        /* Attempt to shut down thread due to code completion */
        if (rfalse == threadstate_request_complete(thridx))
        {
            sysErrMsg(arch_function_name,
             "Unable to move completed thread %d to '%s' state",
                      thridx,
                      thread_state_get_name(THREAD_STATE_COMPLETE));
            THREAD_REQUEST_NEXT_STATE(badlogic, thridx);

            return(rfalse);
        }
    }

    /*!
     * If a thread completed running, and a proper request to the
     * @b COMPLETE state was issued, then it finished normally.
     *
     * @todo HARMONY-6-jvm-opcode.c-83 Should this @c @b if() statement
     *       be inside of the block requesting @b COMPLETE state?
     *       Should a simple @c @b return(rtrue) be there?  Should
     *       this @c @b if() statement be expanded to
     *       consider other conditions?  Etc.  Just needs review
     *       for other possibilities.
     */
    if (EXIT_JVM_THREAD == nonlocal_thread_return)
    {
        return(rtrue);
    }

    /*
     * Move unhandled condition (@c @b nonlocal_rc)
     * and "thread is finished running" (@c @b nonlocal_thread_return)
     * conditions to the @b COMPLETE state, otherwise everything
     * ran fine and thread is still in the @b RUNNING state.
     */
    if ((THREAD_STATUS_EMPTY == nonlocal_rc) &&
        (EXIT_MAIN_OKAY      == nonlocal_thread_return))
    {
        return(rtrue);
    }
    else
    {
        /* Attempt to shut down thread due to condition */
        if (rfalse == threadstate_request_complete(thridx))
        {
            sysErrMsg(arch_function_name,
             "Unable to move completed thread %d to '%s' state",
                      thridx,
                      thread_state_get_name(THREAD_STATE_COMPLETE));
            THREAD_REQUEST_NEXT_STATE(badlogic, thridx);

            return(rfalse);
        }
        /* Return @link #rtrue rtrue@endlink if end of thread detected,
         * otherwise @link #rfalse rfalse@endlink because of unhandled
         * condition.
         */
        return(rfalse);
    }

} /* END of opcode_run() */


/* EOF */
