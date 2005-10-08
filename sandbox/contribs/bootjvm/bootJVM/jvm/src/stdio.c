/*!
 * @file stdio.c
 *
 * @brief Standard Output and Standard Error print functions that are
 * isolated from certain compile requirements of the other code.
 *
 * USE THESE FUNCTIONS IN PLACE OF fprintf() AND sprintf() IN ALL
 * CODE DUE TO STRUCTURE PACKING OPTIONS CAUSING @b SIGSEGV IN
 * SOME ARCHITECTURES (specifically Solaris 32-bit) WITH 'GCC'
 *
 * THIS FILE MUST BE COMPILED WITH STRUCTURE PACKING OFF
 * (at least for the default GCC stdio library) OR IN THE
 * SAME MANNER IN WHICH YOUR COMPILER'S RUNTIME LIBRARY
 * HAS IT COMPILED.  (You may get unexplainable @b SIGSEGV
 * errors otherwise.)
 *
 *
 * @section Control
 *
 * \$URL: https://svn.apache.org/path/name/stdio.c $ \$Id: stdio.c 0 09/28/2005 dlydick $
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

#include "arch.h"
ARCH_COPYRIGHT_APACHE(stdio, c, "$URL: https://svn.apache.org/path/name/stdio.c $ $Id: stdio.c 0 09/28/2005 dlydick $");


#include <stdio.h>
#include <stdarg.h>

#define SUPPRESS_STDIO_REDIRECTION /* All other files redirect here */
#include "jvmcfg.h"

              /* Defeat compiler complaints about @c @b extern */
#define I_AM_STDIO_C

#include "util.h"

/*!
 * @name printf() style printing to standard error and
 * buffer formatting.
 *
 * There are three versions of this routine.  One conditionally
 * prints a message to stderr based on a debug level.
 * The others perform unconditional actions.
 *
 * Create a variable-argument message to standard error or a buffer
 * in a fashion with a tag string on the front telling what type
 * of error it is.
 *
 *
 * @param  dml  Debug msg level @link #DML0 DML0@endlink through
 *              @link #DML10 DML10@endlink
 *
 * @param  bfr  Buffer to format
 *
 * @param  fn   Function name of caller or other unique "string"
 *
 * @param  fmt  printf() style format string
 *
 * _param  ...  printf() style argument list, zero or more
 *
 *
 * @returns number of characters emitted or formatted
 *
 */

/*@{ */ /* Begin grouped definitions */

int sysDbgMsg(rint dml, rchar *fn, rchar *fmt, ...)
{
    /* Display @e nothing if debug messages are disabled */
    if (rfalse == JVMCFG_DEBUG_MESSAGE_ENABLE)
    {
        return(0);
    }

    if (jvmutil_get_dml() < dml)
    {
        return(0);
    }

    /* If debug levels are satisfied, proceed to print message */

    rchar extfmt[JVMCFG_STDIO_BFR]; /* WARNING!  On the stack! */

    /* Load up format string w/ user specification */

    va_list ap;
    va_start(ap, fmt);

    sprintf(extfmt, "%s: %s\n", fn, fmt);
    int rc = vfprintf(stderr, extfmt, ap);

    va_end(ap);

    fflush(stderr);
    JVMCFG_DEBUG_ECLIPSE_FLUSH_STDIO_BETTER;
    return(rc);

} /* END of sysDbgMsg() */


int sysErrMsg(rchar *fn, rchar *fmt, ...)
{
    rchar extfmt[JVMCFG_STDIO_BFR]; /* WARNING!  On the stack! */

    /* Load up format string w/ user specification */

    va_list ap;
    va_start(ap, fmt);

    sprintf(extfmt, "%s: %s\n", fn, fmt);
    int rc = vfprintf(stderr, extfmt, ap);

    va_end(ap);

    fflush(stderr);
    JVMCFG_DEBUG_ECLIPSE_FLUSH_STDIO_BETTER;
    return(rc);

} /* END of sysErrMsg() */


int sysErrMsgBfrFormat(rchar *bfr, rchar *fn, rchar *fmt, ...)
{
    rchar extfmt[JVMCFG_STDIO_BFR]; /* WARNING!  On the stack! */

    /* Load up format string w/ user specification */

    va_list ap;
    va_start(ap, fmt);

    sprintf(extfmt, "%s: %s\n", fn, fmt);
    int rc = vsprintf(bfr, extfmt, ap);

    va_end(ap);

    return(rc);

} /* END of sysErrMsgBfrFormat() */

/*@} */ /* End of grouped definitions */


/*!
 * @name Macro support for local standard output utilities
 *
 */

/*@{ */ /* Begin grouped definitions */

#define LOCAL_FPRINTF(FP)                    \
    va_list ap;                              \
    va_start(ap, fmt);                       \
                                             \
    int rc = vfprintf(FP, fmt, ap);          \
                                             \
    va_end(ap);                              \
    fflush(FP);                              \
    JVMCFG_DEBUG_ECLIPSE_FLUSH_STDIO_BETTER; \
    return(rc)

#define LOCAL_SPRINTF                \
    va_list ap;                      \
    va_start(ap, fmt);               \
                                     \
    int rc = vsprintf(bfr, fmt, ap); \
                                     \
    va_end(ap);                      \
    return(rc)

/*@} */ /* End of grouped definitions */

/*!
 * @name Local version of standard output routines.
 *
 * Local implementations of @c @b sprintf(3) and @c @b fprintf(3)
 * are provided for both stdout and stderr.  These functions are
 * intended to bypass the structure packing mismatch between the
 * requirements of parts of this application and the manner in
 * which any compiler's runtime library was compiled.
 *
 * In the same manner as the printf() style printing functions
 * above, there are three versions of this routine.  In this
 * case, one writes to standard output, another to standard error,
 * and the third formats a buffer.
 *
 * @param  fmt  printf() style format string
 *
 * _param  ...  printf() style argument list, zero or more
 *
 *
 * @returns number of characters emitted or formatted
 *
 */

/*@{ */ /* Begin grouped definitions */

int fprintfLocalStdout(rchar *fmt, ...)
{
    LOCAL_FPRINTF(stdout);

} /* END of fprintfLocalStdout() */


int fprintfLocalStderr(rchar *fmt, ...)
{
    LOCAL_FPRINTF(stderr);

} /* END of fprintfLocalStderr() */

int sprintfLocal(rchar *bfr, rchar *fmt, ...)
{
    LOCAL_SPRINTF;

} /* END of sprintfLocal() */

/*@} */ /* End of grouped definitions */


/*!
 * @name Magic redirection of selected standard I/O functions.
 *
 * In @link jvm/src/jvmcfg.h jvmcfg.h@endlink, there are
 * several @c @b \#define statements that redirect @c @b printf()
 * and @c @b fprintf() and @c @b sprintf() from the
 * @c @b \<stdio.h\> linkages to local linkages.
 *
 * This magic redirection used the compile-time definition
 * @c @b SUPPRESS_STDIO_REDIRECTION to co-opt these standard I/O
 * calls and redirect them to local version so as to avoid @b SIGSEGV
 * problems with certain configurations. * These routines are meant
 * to encourage use of proper functions above.
 *
 * @param  fp   @c @b FILE handle to standard I/O file handle.
 *
 * @param  bfr  Buffer to format
 *
 * @param  fmt  printf() style format string
 *
 * _param  ...  printf() style argument list, zero or more
 *
 *
 * @returns number of characters emitted or formatted
 */

/*@{ */ /* Begin grouped definitions */

int _printfLocal(const rchar *fmt, ...)
{
    return(sysErrMsg("_printfLocal",
                     "Please use 'printfLocal' function instead"));

} /* END of _printfLocal() */
    
int _fprintfLocal(FILE *fp,  const rchar *fmt, ...)
{
    return(sysErrMsg("_fprintfLocal",
                     "Please use 'fprintfLocal' function instead"));

} /* END of _fprintfLocal() */

int _sprintfLocal(rchar *bfr, const rchar *fmt, ...)
{
    return(sysErrMsg("_sprintfLocal",
                     "Please use 'sprintfLocal' function instead"));

} /* END of _sprintfLocal() */

/*@} */ /* End of grouped definitions */


/* EOF */
