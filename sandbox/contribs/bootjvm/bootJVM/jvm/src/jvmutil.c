/*!
 * @file jvmutil.c
 *
 * @brief Utilities for operating the JVM on this
 * real machine implementation.
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
ARCH_COPYRIGHT_APACHE(jvmutil, c, "$URL$ $Id$");


#include "jvmcfg.h" 
#include "cfmacros.h" 
#include "classfile.h" 
#include "attribute.h" 
#include "jvm.h" 
#include "linkage.h" 
#include "util.h" 


/*!
 * @name Debug message verbosity utilities for sysDbgMsg().
 *
 * @brief Set and get an integer value that determines the number of 
 * debug messages that get displayed by sysDbgMsg().
 *
 * When sysDbgMsg() is inserted into the code of a function,
 * a verbosity level is its first parameter.  The higher the
 * number, the more verbose the debug output becomes, up to
 * level @link #DML10 DMLMAX@endlink.  The lower
 * the number, the less it is displayed at run time, down to
 * @link #DML1 DMLMIN@endlink.  At level @link #DML0 DMLOFF@endlink,
 * only @e vital diagnostic messages are displayed.  The rest are
 * suppressed.
 *
 * This value is heuristically applied by the developer as to the
 * importance of that information in various development and
 * testing situations.
 *
 * The importance of the situation defaults to
 * @link #DMLDEFAULT DMLDEFAULT@endlink at compile time and may
 * be changed at run time with the
 * @link #JVMCFG_DEBUGMSGLEVEL_FULL_PARM -Xdebug_level@endlink
 * command line parameter.
 *
 * @see jvm_debug_level_enum
 *
 * @see sysDbgMsg()
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Set current debug message level
 *
 *
 * @param  level     New level to set.
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */

rvoid jvmutil_set_dml(jvm_debug_level_enum level)
{
    pjvm->debug_message_level = level;

} /* END of jvmutil_set_dml() */


/*!
 * @brief Get current debug message level
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 *       @returns current debug message verbosity
 *
 */
jvm_debug_level_enum jvmutil_get_dml()
{
    return(pjvm->debug_message_level);

} /* END of jvmutil_get_dml() */

/*@} */ /* End of grouped definitions */


/*!
 * @name Stack dump utilities.
 *
 * @brief Print contents of a thread's stack to standard error.
 *
 * Several forms are available that provide various amounts of stack
 * frame detail.  The most verbose also shows local variables
 * in the stack frame.
 *
 * @attention These routines are @e not intended as a replacement
 * for the normal routine
 * <b><code>java.lang.Throwable.printStackTrace()</code></b> !!!
 *
 * @param  thridx      Thread table index of thread to show
 *
 * @param  pheader     Null-terminated header string.  If no header is
 *                     desired, pass a @link #rnull rnull@endlink
 *                     pointer here.
 *
 * @param  showdetails If @link #rtrue rtrue@endlink, show frame
 *                     details, else less verbose.
 *
 * @param  showlocals  If @link #rtrue rtrue@endlink, show local
 *                     variables also, but only if @b showdetails is
 *                     also @link #rtrue rtrue@endlink.
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * @todo  This function needs unit testing.
 *
 * @todo  Add line numbers to output.  Sample output might look like
 *        this (when line numbers are added):
 *
 * @verbatim
 * Exception in thread "main" java.lang.NullPointerException
        at Testit.sub1(Testit.java:9)
        at Testit.main(Testit.java:23)
   @endverbatim
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Common function to perform final output from all
 * stack print utilities.
 *
 */
static rvoid jvmutil_print_stack_common(jvm_thread_index  thridx,
                                        rchar            *pheader,
                                        rboolean          showdetails,
                                        rboolean          showlocals)
{
    /* Print header if one is passed in, else skip */
    if (rnull != pheader)
    {
        fprintfLocalStderr("%s\n", pheader);
    }

    /*
     * Read down through all frames until bottom of stack.
     * The very last stack frame holds a null FP.
     */

    jvm_sp fp = FIRST_STACK_FRAME(thridx);

    while (!CHECK_FINAL_STACK_FRAME_GENERIC(thridx, fp))
    {
        jvm_class_index clsidx =
            STACK(thridx,
                  GET_FP(thridx) + JVMREG_STACK_PC_CLSIDX_OFFSET);

        ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

        jvm_method_index mthidx =
            STACK(thridx,
                  GET_FP(thridx) + JVMREG_STACK_PC_MTHIDX_OFFSET);

        rint star_len_cls = CP1_NAME_STRLEN(CONSTANT_Class_info,
                                            pcfs,
                                            pcfs->this_class,
                                            name_index);

        rint star_len_mth = CP1_NAME_STRLEN(CONSTANT_Class_info,
                                            pcfs,
                                            mthidx,
                                            name_index);

        jvm_attribute_index atridx =
            attribute_find_in_class_by_enum(clsidx,
                                       LOCAL_SOURCEFILE_ATTRIBUTE);

        jvm_constant_pool_index cpidx;
        rint star_len_src;
        rchar *srcname;

        if (jvm_attribute_index_bad == atridx)
        {
            cpidx = CONSTANT_CP_DEFAULT_INDEX;

            star_len_src = 7; /* Length of "unknown" */

            srcname = "unknown";
        }
        else
        {
            cpidx = ((SourceFile_attribute *)
                       &pcfs->attributes[atridx]->ai)->sourcefile_index;

            star_len_src = CP_THIS_STRLEN(pcfs, cpidx);

            srcname = PTR_CP_THIS_STRNAME(pcfs, cpidx);
        }

        /* Least verbosity, called from jvmutil_print_stack() */
         fprintfLocalStderr("     at %*.*s%c%*.*s(%*.*s:%d)\n",

                 star_len_cls, star_len_cls,
                 PTR_CP1_NAME_STRNAME(CONSTANT_Class_info,
                                      pcfs,
                                      pcfs->this_class,
                                      name_index),

                 CLASSNAME_EXTERNAL_DELIMITER_CHAR,

                 star_len_mth, star_len_mth,
                 PTR_CP1_NAME_STRNAME(CONSTANT_Class_info,
                                      pcfs,
                                      mthidx,
                                      name_index),

                 star_len_src, star_len_src,
                 srcname,
                 0 /*! @todo  Get line numbers */);

        /*
         * Fill in frame details and local variables
         */
        if (rtrue == showdetails)
        {
            /*! @todo Show details of stack frame */

            if (rtrue == showlocals)
            {
                /*! @todo Show local variables in stack frame */
            }
        }

        /* Look at next stack frame */
        fp = NEXT_STACK_FRAME_GENERIC(thridx, fp);
    }

} /* END of jvmutil_print_stack_common() */


/*!
 * @brief Print basic stack frame summary only.
 *
 */
rvoid jvmutil_print_stack(jvm_thread_index thridx, rchar *pheader)
{
    jvmutil_print_stack_common(thridx, pheader, rfalse, rfalse);

} /* END of jvmutil_print_stack() */


/*!
 * @brief Print stack frame with some details.
 *
 */
rvoid jvmutil_print_stack_details(jvm_thread_index  thridx,
                                  rchar            *pheader)
{
    jvmutil_print_stack_common(thridx, pheader, rtrue, rfalse);

} /* END of jvmutil_print_stack_details() */


/*!
 * @brief Print stack frame with details and local variables.
 *
 */
rvoid jvmutil_print_stack_locals(jvm_thread_index  thridx,
                                 rchar            *pheader)
{
    jvmutil_print_stack_common(thridx, pheader, rtrue, rtrue);

} /* END of jvmutil_print_stack_locals() */


/*!
 * @brief Common print basic stack frame summary showing error type.
 *
 */
static rvoid jvmutil_print_errtype_stack(jvm_thread_index  thridx,
                                         rchar            *errtype)
{
    rchar *pheader = HEAP_GET_DATA(JVMCFG_STDIO_BFR, rfalse);

    jvm_class_index clsidx =
        STACK(thridx, GET_FP(thridx) + JVMREG_STACK_PC_CLSIDX_OFFSET);

    ClassFile *pcfs = CLASS_OBJECT_LINKAGE(clsidx)->pcfs;

    rint star_len = CP1_NAME_STRLEN(CONSTANT_Class_info,
                                    pcfs,
                                    pcfs->this_class,
                                    name_index);

    sprintfLocal(pheader,
                 "%s in thread \"s\" *.*%s",
                 errtype,
                 THREAD(thridx).name,
                 star_len, star_len,
                 PTR_CP1_NAME_STRNAME(CONSTANT_Class_info,
                                      pcfs,
                                      pcfs->this_class,
                                      name_index));

    jvmutil_print_stack(thridx, pheader);

    HEAP_FREE_DATA(pheader);

    return;

} /* END of jvmutil_print_errtype_stack() */


/*!
 * @brief Print basic stack frame summary reporting an error versus
 * an exception.
 *
 */
rvoid jvmutil_print_error_stack(jvm_thread_index thridx)
{
    jvmutil_print_errtype_stack(thridx, "Error");

} /* END of jvmutil_print_error_stack() */


/*!
 * @brief Print basic stack frame summary reporting an exception versus
 * an error.
 *
 */
rvoid jvmutil_print_exception_stack(jvm_thread_index thridx)
{
    jvmutil_print_errtype_stack(thridx, "Exception");

} /* END of jvmutil_print_exception_stack() */

/*@} */ /* End of grouped definitions */


/* EOF */
