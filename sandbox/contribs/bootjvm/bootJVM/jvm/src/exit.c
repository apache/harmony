/*!
 * @file exit.c
 *
 * @brief Abort strategy functions for the JVM.
 *
 * This implementation uses two invocations of
 * @c @b setjmp(3)/longjmp(3) to perform non-local returns
 * from error conditions.
 *
 * Normal exit conditions are more likely to use a simple
 * @c @b return() instead of the error mechanism, but are
 * not inhibited from it except that returning the normal
 * @link #EXIT_MAIN_OKAY EXIT_MAIN_OKAY@endlink cannot occur due to the
 * design of @c @b longjmp(3), which will force
 * @link #EXIT_LONGJMP_ARGERROR EXIT_LONGJMP_ARGERROR@endlink instead.
 *
 * @link #EXIT_INIT() EXIT_INIT()@endlink must be invoked at a
 * higher level than where @link #pjvm pjvm@endlink is used to
 * access anything, namely, @c @b pjvm->xxx since the
 * main JVM structure cannot be initialized at the same time
 * it is being protected by a @b jmp_buf hook stored within it.
 * It typically will be armed at the very entry to the JVM and
 * will never be re-armed since it is global in its scope of coverage.
 *
 * @link #EXIT_EXCEPTION_SETUP() EXIT_EXCEPTION_SETUP()@endlink
 * has similar requirements.  However, since it is involved more
 * closely with @link #jvm_init() jvm_init()@endlink, it is typically
 * invoked at the beginning of that function.  Once initialization
 * proceeds and more and more facilities become available, it should
 * be re-armed to a new handler to reflect increased capability.  Once
 * the virtual execution engine is ready, it should be re-armed to
 * manually run @link #JVMCLASS_JAVA_LANG_LINKAGEERROR
 * JVMCLASS_JAVA_LANG_LINKAGEERROR@endlink subclasses through
 * the virtual execution engine before shutting down the JVM.
 *
 * @link #OPCODE_END_THREAD_SETUP() OPCODE_END_THREAD_SETUP()@endlink
 * is not so much an error handler as a simplification of the JVM
 * inner loop execution in @link #opcode_run() opcode_run()@endlink
 * that eliminates the need for two of the tests needed for continuing
 * to run Java virtual instruction on this thread.  It is invoked
 * only once, and that before the inner look @c @b while statement.
 *  When a member of the Java @b return instruction group is
 * executed, then if the thread termination conditions have been
 * met, the @c @b longjmp(3) exits the loop non-locally.
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
ARCH_SOURCE_COPYRIGHT_APACHE(exit, c,
"$URL$",
"$Id$");


#define PORTABLE_JMP_BUF_VISIBLE
#include "jvmcfg.h"
#include "classfile.h"
#include "jvm.h"

/*!
 * @brief Permit localized usage of @b EXIT_xxx symbols.
 */
#define I_AM_EXIT_C
#include "exit.h"

/*!
 * @brief Return a descriptive name string for each exit code.
 *
 *
 * @param  code   Exit code @link #exit_code_enum enumeration@endlink
 *
 *
 * @returns Null-terminated string describing the exit code.
 *
 */
rchar *exit_get_name(exit_code_enum code)
{
    ARCH_FUNCTION_NAME(exit_get_name);

    switch(code)
    {
        case EXIT_MAIN_OKAY:        return(EXIT_MAIN_OKAY_DESC);
        case EXIT_LONGJMP_ARGERROR: return(EXIT_LONGJMP_ARGERROR_DESC);
        case EXIT_ARGV_HELP:        return(EXIT_ARGV_HELP_DESC);
        case EXIT_ARGV_VERSION:     return(EXIT_ARGV_VERSION_DESC);
        case EXIT_ARGV_COPYRIGHT:   return(EXIT_ARGV_COPYRIGHT_DESC);
        case EXIT_ARGV_LICENSE:     return(EXIT_ARGV_LICENSE_DESC);
        case EXIT_ARGV_ENVIRONMENT: return(EXIT_ARGV_ENVIRONMENT_DESC);
        case EXIT_JVM_THREAD:       return(EXIT_JVM_THREAD_DESC);
        case EXIT_JVM_CLASS:        return(EXIT_JVM_CLASS_DESC);
        case EXIT_JVM_OBJECT:       return(EXIT_JVM_OBJECT_DESC);
        case EXIT_JVM_METHOD:       return(EXIT_JVM_METHOD_DESC);
        case EXIT_JVM_FIELD:        return(EXIT_JVM_FIELD_DESC);
        case EXIT_JVM_ATTRIBUTE:    return(EXIT_JVM_ATTRIBUTE_DESC);
        case EXIT_JVM_THROWABLE:    return(EXIT_JVM_THROWABLE_DESC);
        case EXIT_JVM_SIGNAL:       return(EXIT_JVM_SIGNAL_DESC);
        case EXIT_JVM_BYTECODE:     return(EXIT_JVM_BYTECODE_DESC);
        case EXIT_JVM_GC:           return(EXIT_JVM_GC_DESC);
        case EXIT_JVM_INTERNAL:     return(EXIT_JVM_INTERNAL_DESC);
        case EXIT_HEAP_ALLOC:       return(EXIT_HEAP_ALLOC_DESC);
        case EXIT_GC_ALLOC:         return(EXIT_GC_ALLOC_DESC);
        case EXIT_THREAD_STACK:     return(EXIT_THREAD_STACK_DESC);
        case EXIT_TIMESLICE_START:  return(EXIT_TIMESLICE_START_DESC);
        case EXIT_TMPAREA_MKDIR:    return(EXIT_TMPAREA_MKDIR_DESC);
        case EXIT_TMPAREA_RMDIR:    return(EXIT_TMPAREA_RMDIR_DESC);
        case EXIT_CLASSPATH_JAR:    return(EXIT_CLASSPATH_JAR_DESC);
        case EXIT_MANIFEST_JAR:     return(EXIT_MANIFEST_JAR_DESC);
        default:                    return("unknown");
    }
} /* END of exit_get_name() */


/* Moved to portability library:  static jmp_buf exit_general_failure;*/

/* Moved to portability library:  static jmp_buf exit_LinkageError; */


/*!
 * Class to run on non-local return.  Give global visibility for use
 * by users of
 * @link #exit_throw_exception() exit_throw_exception()@endlink.
 */
rchar *exit_LinkageError_subclass;


/*!
 * Thread where error occurred.  Give global visibility for use by
 * users of
 * @link #exit_throw_exception() exit_throw_exception()@endlink.
 */
jvm_thread_index exit_LinkageError_thridx;


/*!
 * @brief Global handler setup (part 1) for fatal
 * @link jvm_init() jvm_init()@endlink errors and other
 * @c @b java.lang.Throwable events-- implements
 * @c @b setjmp(3).
 *
 *
 * This function @e must be used in conjunction with
 * @link EXIT_EXCEPTION_SETUP() EXIT_EXCEPTION_SETUP()@endlink
 * to properly arm handler for throwing @c @b java.lang.Error and
 * @c @b java.lang.Exception throwable events.
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */

rvoid exit_exception_setup(rvoid)
{
    ARCH_FUNCTION_NAME(exit_exception_setup);

    exit_LinkageError_subclass = (rchar *) rnull;
    exit_LinkageError_thridx   = jvm_thread_index_null;

    return;

} /* END of exit_exception_setup() */


/*!
 * @brief Global handler for initialization linkage errors,
 * per spec section 2.17.x  -- implements @c @b longjmp(3).
 *
 * Use this function to throw @c @b java.lang.Error and
 * @c @b java.lang.Exception throwable events.
 *
 * This is a global handler invocation first for @link #jvm_init()
   jvm_init()@endlink during startup and then runtime events.
 * A @e wide variety of runtime conditions may be expressed in
 * the combination of @b rc and @b preason.  Judicious combinations
 * of exit codes and error classes will greatly limit the need
 * for expanding on the number of values for either parameter,
 * yet can express many different nuances of errors.
 *
 * @param  rcenum  Return code to pass back out of failed routine, which
 *                 must be an
 *                 @link #exit_code_enum exit code enumeration@endlink
 *                 other than
 *                 @link #EXIT_MAIN_OKAY EXIT_MAIN_OKAY@endlink, which
 *                 will get translated into @link
                   #EXIT_LONGJMP_ARGERROR EXIT_LONGJMP_ARGERROR@endlink.
 *
 * @param  preason Error class, which must be a subclass of
 *                   @c @b java.lang.LinkageError , namely:
 *
 * <ul> <li> @link #JVMCLASS_JAVA_LANG_LINKAGEERROR
             JVMCLASS_JAVA_LANG_LINKAGEERROR@endlink </li>
 *      <li> @link #JVMCLASS_JAVA_LANG_CLASSCIRCULARITYERROR
            JVMCLASS_JAVA_LANG_CLASSCIRCULARITYERROR@endlink </li>
 *      <li> @link #JVMCLASS_JAVA_LANG_CLASSFORMATERROR
             JVMCLASS_JAVA_LANG_CLASSFORMATERROR@endlink </li>
 *      <li> @link #JVMCLASS_JAVA_LANG_EXCEPTIONININITIALIZERERROR
            JVMCLASS_JAVA_LANG_EXCEPTIONININITIALIZERERROR@endlink </li>
 *      <li> @link #JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR
           JVMCLASS_JAVA_LANG_INCOMPATIBLECLASSCHANGEERROR@endlink </li>
 *      <li> @link #JVMCLASS_JAVA_LANG_NOCLASSDEFFOUNDERROR
             JVMCLASS_JAVA_LANG_NOCLASSDEFFOUNDERROR@endlink </li>
 *      <li> @link #JVMCLASS_JAVA_LANG_UNSATISFIEDLINKERROR
             JVMCLASS_JAVA_LANG_UNSATISFIEDLINKERROR@endlink </li>
 *      <li> @link #JVMCLASS_JAVA_LANG_VERIFYERROR
             JVMCLASS_JAVA_LANG_VERIFYERROR@endlink </li>
 * </ul>
 *
 *
 * @returns non-local state restoration from setup via @c @b setjmp(3)
 *          as stored in @link #portable_exit_LinkageError
            portable_exit_LinkageError@endlink
 *          buffer by @link #exit_init() exit_init()@endlink
 *          in @link #jvm_init() jvm_init()@endlink before any of
 *          these errors could occur. All code invoking this
 *          function should use the standard @c @b lint(1) comment for
 *          "code not reached" as shown after the @c @b longjmp(3)
 *          function call in the source code of this function:
 *          <b>/</b><b>*NOTREACHED*</b><b>/</b>
 *
 */

rvoid exit_throw_exception(exit_code_enum rcenum, rchar *preason)
{
    ARCH_FUNCTION_NAME(exit_throw_exception);

    /* Report error class to handler */
    exit_LinkageError_subclass = preason;
    exit_LinkageError_thridx   = CURRENT_THREAD;

    /* Returns to @c @b setjmp(3) */
    int rc = (int) rcenum;
    PORTABLE_LONGJMP(&portable_exit_LinkageError, rc);
/*NOTREACHED*/

} /* END of exit_throw_exception() */


/*!
 * @brief Global handler invocation for fatal JVM errors-- implements
 * @c @b longjmp(3)
 *
 * @param  rcenum  Return code to pass back out of JVM.
 *
 *
 * @returns non-local state restoration from setup via @c @b setjmp(3)
 *          above as stored in @link #portable_exit_general_failure
            portable_exit_general_failure@endlink.
 *          All code invoking this function should use the
 *          standard @c @b lint(1) comment for "code not reached" as
 *          shown after the @c @b longjmp(3) function call in
 *          the source code of this function:
 *          <b>/</b><b>*NOTREACHED*</b><b>/</b>
 *
 */

rvoid exit_jvm(exit_code_enum rcenum)
{
    ARCH_FUNCTION_NAME(exit_jvm);

    /* Returns to @c @b setjmp(3) as declared in exit_jvm() */
    int rc = (int) rcenum;
    PORTABLE_LONGJMP(&portable_exit_general_failure, rc);
/*NOTREACHED*/

} /* END of exit_jvm() */


/* EOF */
