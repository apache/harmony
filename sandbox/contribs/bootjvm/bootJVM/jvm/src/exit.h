#ifndef _exit_h_defined_
#define _exit_h_defined_

/*!
 * @file exit.h
 *
 * @brief Exit codes for JVM diagnostics.
 *
 * Notice that if a Java program calls @c @b System.exit() ,
 * then the system call @c @b exit(2) will @e still be made, but
 * with that requested exit code, having nothing to do with
 * these codes.  However, Most exit situations are set up
 * with @c @b fprintf(stderr) immediately before the
 * @c @b exit(2) call itself, so console output should demonstrate
 * proper context if there is ever a question.
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

ARCH_COPYRIGHT_APACHE(exit, h, "$URL$ $Id$");


/*!
 * @name Exit code enumeration definitions and name strings.
 *
 * @brief Codes and null-terminated strings for constructing
 * @link #exit_code_enum exit_code_enum enum@endlink and
 * @link #exit_get_name exit_get_name() switch@endlink, respectively.
 *
 * @b NEVER change definition of
 * @link #EXIT_MAIN_OKAY EXIT_MAIN_OKAY@endlink!  It is
 * the normal return from @link #exit_init() exit_init()@endlink
 * function by virtue of @c @b setjmp(3) definition.
 *
 * @b NEVER change the definition of
 * @link #EXIT_LONGJMP_ARGERROR EXIT_LONGJMP_ARGERROR@endlink!
 * It is defined by @c @b longjmp(3) when an attempt is made to
 * use @link #EXIT_MAIN_OKAY EXIT_MAIN_OKAY@endlink as
 * its return code.
 *
 */

/*@{ */ /* Begin grouped definitions */

#ifdef I_AM_EXIT_C
#define EXIT_MAIN_OKAY_DESC        "no error"
#define EXIT_LONGJMP_ARGERROR_DESC "longjmp(3) argument error"

#define EXIT_ARGV_HELP_DESC        "help request"
#define EXIT_ARGV_VERSION_DESC     "version request"
#define EXIT_ARGV_COPYRIGHT_DESC   "copyright request"
#define EXIT_ARGV_LICENSE_DESC     "license request"
#define EXIT_ARGV_ENVIRONMENT_DESC "environment request"

#define EXIT_JVM_THREAD_DESC       "thread start error"
#define EXIT_JVM_CLASS_DESC        "class load error"
#define EXIT_JVM_OBJECT_DESC       "object load error"
#define EXIT_JVM_METHOD_DESC       "method invocation error"
#define EXIT_JVM_FIELD_DESC        "field access error"
#define EXIT_JVM_ATTRIBUTE_DESC    "attribute access error"
#define EXIT_JVM_THROWABLE_DESC    "throwable event abort"
#define EXIT_JVM_SIGNAL_DESC       "OS signal abort trapped"
#define EXIT_JVM_BYTECODE_DESC     "JVM byte code execution error"
#define EXIT_JVM_GC_DESC           "Garbage collection error"
#define EXIT_JVM_INTERNAL_DESC     "Internal JVM logic error"

#define EXIT_HEAP_ALLOC_DESC       "Heap allocation error"
#define EXIT_GC_ALLOC_DESC         "Garbage collection heap error"
#define EXIT_THREAD_STACK_DESC     "Stack overflow suppressed"
#define EXIT_TIMESLICE_START_DESC  "cannot start timer"
#define EXIT_TMPAREA_MKDIR_DESC    "cannot make temp directory"
#define EXIT_TMPAREA_RMDIR_DESC    "cannot remove temp directory"
#define EXIT_CLASSPATH_JAR_DESC    "cannot run jar command"
#define EXIT_MANIFEST_JAR_DESC     "cannot process jar manifest file"
#endif

typedef enum
{
    EXIT_MAIN_OKAY        = 0, /**< Everything went fine.
                                    No Java code called
                                   @c @b java.lang.System.exit() ,
                                    either. */
    EXIT_LONGJMP_ARGERROR = 1, /**< @c @b longjmp(3) called with 0 arg*/

/* Command line conditions and errors */
    EXIT_ARGV_HELP        = 10, /**< @b Help invoked on command line */
    EXIT_ARGV_VERSION     = 11,/**< @b Version invoked on command line*/
    EXIT_ARGV_COPYRIGHT   = 12, /**< @b Copyright invoked on cmd line */
    EXIT_ARGV_LICENSE     = 13, /**< @b Copyright invoked on cmd line */
    EXIT_ARGV_ENVIRONMENT = 14, /**< Environment variable error */

/* JVM fatal runtime errors */
    EXIT_JVM_THREAD       = 20, /**< Thread start error */
    EXIT_JVM_CLASS        = 21, /**< Class load error */
    EXIT_JVM_OBJECT       = 22, /**< Object load error */
    EXIT_JVM_METHOD       = 23, /**< Method invocation error */
    EXIT_JVM_FIELD        = 24, /**< Field access error */
    EXIT_JVM_ATTRIBUTE    = 25, /**< Attribute access error */
    EXIT_JVM_THROWABLE    = 26, /**< Throwable event abort */
    EXIT_JVM_SIGNAL       = 27, /**< OS signal abort trapped */
    EXIT_JVM_BYTECODE     = 28, /**< JVM byte code execution error */
    EXIT_JVM_GC           = 29, /**< Garbage collection error */
    EXIT_JVM_INTERNAL     = 30, /**< Internal JVM logic error */

/* Fatal runtime errors in other modules */
    EXIT_HEAP_ALLOC       = 31, /**< Heap allocation error */
    EXIT_GC_ALLOC         = 32, /**< Garbage collection heap error */
    EXIT_THREAD_STACK     = 33, /**< Stack overflow suppressed */
    EXIT_TIMESLICE_START  = 34, /**< Cannot start timer */
    EXIT_TMPAREA_MKDIR    = 35, /**< Cannot make temp directory */
    EXIT_TMPAREA_RMDIR    = 36, /**< Cannot remove temp directory */
    EXIT_CLASSPATH_JAR    = 37, /**< Cannot run jar command */
    EXIT_MANIFEST_JAR     = 38  /**< Cannot process jar manifest file */

} exit_code_enum;

/*@} */ /* End of grouped definitions */


/* Prototypes for functions in 'exit.c' */
extern int exit_init(rvoid);

extern rchar *exit_get_name(exit_code_enum code);

extern rchar *exit_LinkageError_subclass;
extern jvm_thread_index exit_LinkageError_thridx;
extern int exit_exception_setup(rvoid);

extern rvoid exit_throw_exception(exit_code_enum rc, rchar *preason);

extern int   exit_end_thread_setup(rvoid);
extern rvoid exit_end_thread_test(jvm_thread_index thridx);

extern rvoid exit_jvm(exit_code_enum rc);

#endif /* _exit_h_defined_ */


/* EOF */
