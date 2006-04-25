#ifndef _portable_h_defined_
#define _portable_h_defined_

/*!
 * @file portable.h
 *
 * @brief External definitions for portable version of
 * @b system(2) calls and @b library(3) utility functions for
 * @link jvm/src/portable_libc.c portable_libc.c@endlink,
 * @link jvm/src/portable_libm.c portable_libm.c@endlink, and
 * @link jvm/src/portable_jmp_buf.c portable_jmp_buf.c@endlink.
 *
 * @attention  See note in @link jvm/src/portable_jmp_buf.c
               portable_jmp_buf.c@endlink about use and redefinition
 *             of @c @b setjmp(3)/longjmp(3) and why there is no
 *             function remapping done here.
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
ARCH_HEADER_COPYRIGHT_APACHE(portable, h,
"$URL$",
"$Id$");

/*!
 * @name Remap portable calls to generate error
 *
 * By disallowing the old calls, any time a normal invocation is
 * made, the compiler will inform of the need to use the portability
 * library version.  This will help compensate for oversights.
 *
 */

/*@{ */ /* Begin grouped definitions */

#ifndef I_AM_PORTABLE_C

/* remappings for portable_libc.c */
#define mkdir PLEASE_USE_portable_mkdir /**< @c @b mkdir(2) has been
                                           remapped into the
                                           portability library. */

#define rmdir PLEASE_USE_portable_rmdir /**< @c @b rmdir(2) has been
                                           remapped into the
                                           portability library. */

#define stat PLEASE_USE_portable_stat   /**< @c @b stat(2) has been
                                           remapped into the
                                           portability library. */

#define open PLEASE_USE_portable_open   /**< @c @b open(2) has been
                                           remapped into the
                                           portability library. */

#define read PLEASE_USE_portable_read   /**< @c @b read(2) has been
                                           remapped into the
                                           portability library. */

#define close PLEASE_USE_portable_close /**< @c @b close(2) has been
                                           remapped into the
                                           portability library. */

#define fopen PLEASE_USE_portable_fopen /**< @c @b fopen(3) has been
                                           remapped into the
                                           portability library. */

#define fgets PLEASE_USE_portable_fgets /**< @c @b fgets(3) has been
                                           remapped into the
                                           portability library. */

#define fclose PLEASE_USE_portable_fclose /**< @c @b fclose(3) has been
                                           remapped into the
                                           portability library. */

#define getwd  PLEASE_USE_portable_getwd /**< @c @b getwd(3) has been
                                           remapped into the
                                           portability library. */

#define getenv PLEASE_USE_portable_getenv /**< @c @b getenv(3) has been
                                           remapped into the
                                           portability library. */

#define getpid PLEASE_USE_portable_getpid /**< @c @b getpid(3) has been
                                           remapped into the
                                           portability library. */

#define sleep PLEASE_USE_portable_sleep /**< @c @b sleep(3) has been
                                           remapped into the
                                           portability library. */

#define system PLEASE_USE_portable_system /**< @c @b system(3) has been
                                           remapped into the
                                           portability library. */

#define strchr PLEASE_USE_portable_strchr /**< @c @b strchr(3) has
                                           been remapped into the
                                           portability library. */

#define strrchr PLEASE_USE_portable_strrchr /**< @c @b strrchr(3) has
                                           been remapped into the
                                           portability library. */

#define strcmp PLEASE_USE_portable_strcmp /**< @c @b strcmp(3) has
                                           been remapped into the
                                           portability library. */

#define strncmp PLEASE_USE_portable_strncmp /**< @c @b strncmp(3) has
                                           been remapped into the
                                           portability library. */

#define strlen PLEASE_USE_portable_strlen /**< @c @b strlen(3) has
                                           been remapped into the
                                           portability library. */

#define strcat PLEASE_USE_portable_strcat /**< @c @b strcat(3) has
                                           been remapped into the
                                           portability library. */

#define strcpy PLEASE_USE_portable_strcpy /**< @c @b strcpy(3) has
                                           been remapped into the
                                           portability library. */

#define memcpy PLEASE_USE_portable_memcpy /**< @c @b memcpy(3) has
                                           been remapped into the
                                           portability library. */

#define atol PLEASE_USE_portable_atol   /**< @c @b atol(3) has been
                                           remapped into the
                                           portability library. */

#define isspace PLEASE_USE_portable_isspace /**< @c @b isspace(3) has
                                           been remapped into the
                                           portability library. */

#define malloc PLEASE_USE_portable_malloc /**< @c @b malloc(3) has been
                                           remapped into the
                                           portability library. */

#define free PLEASE_USE_portable_free   /**< @c @b free(3) has been
                                           remapped into the
                                           portability library. */

/* remappings for portable_libm.c */
#define fmod PLEASE_USE_portable_jPRECISION_fmod
                                        /**< @c @b fmod(3) has been
                                           remapped into the
                                           portability library.  It
                                           has a version for both
                                           single- and double-precision
                                           values */
#endif /* I_AM_PORTABLE_C */

/*@} */ /* End of grouped definitions */


/* remappings for portable_libc.c */
 
/* Level 1 file access */
extern rint   portable_mkdir(const rchar *path, rint oflag);
extern rint   portable_rmdir(const rchar *path);
extern rvoid *portable_stat(const rchar *path
                            /* struct stat *  impl w/in */);
extern rlong  portable_stat_get_st_size(rvoid *statbfr);
extern rint   portable_open(const rchar *path,
                            rint oflag /* , ... not impl/not needed */);
extern rlong  portable_read(rint fildes, rvoid *buf, rlong nbyte);
extern rint   portable_close(rint fildes);

/* Level 2 file access */
extern rvoid *portable_fopen(const rchar *filename, const rchar *mode);
extern rchar *portable_fgets(rchar *s, rint n, rvoid *stream);
extern rint   portable_fclose(rvoid *stream);

/* Shell and process control */
extern rchar *portable_getwd(rchar *path_name);
extern rchar *portable_getenv(const rchar *name);
extern rlong  portable_getpid(rvoid);
extern rint   portable_system(const rchar *string);
extern ruint  portable_sleep(ruint seconds);

/* String manipulation */
extern rchar *portable_strchr(const rchar *s, rint c);
extern rchar *portable_strrchr(const rchar *s, rint c);
extern rint   portable_strcmp(const rchar *s1, const rchar *s2);
extern rint   portable_strncmp(const rchar *s1,const rchar *s2,rlong n);
extern rlong  portable_strlen(const rchar *s);
extern rchar *portable_strcat(rchar *s1, const rchar *s2);
extern rchar *portable_strcpy(rchar *s1, const rchar *s2);

/* Memory manipulation */
extern rvoid *portable_memcpy(rvoid *s1, const rvoid *s2, rlong n);

/* C type library */
extern rlong  portable_atol(const rchar *str);
extern rint   portable_isspace(rint c);

/* Memory allocation */
extern rvoid *portable_malloc(rlong size);
extern rvoid  portable_free(rvoid *ptr);


#ifdef PORTABLE_JMP_BUF_VISIBLE

/* Include this file only in the source files that need it: */
#include <setjmp.h>

/*!
 * @brief Portable edition of @c @b jmp_buf structure.
 *
 * @attention Early on in the project, two @c @b jmp_buf structures were
 *            part of a source file along with other global and
 *            @c @b static variables.  Sometimes the @c @b longjmp(3)
 *            would return the correct return code, but sometimes, for
 *            reasons unknown then, it would return zero as if
 *            @c @b setjmp(3) had been called instead!  It is
 *            speculated that there was a structure packing issue
 *            at hand, where one of the last members was the
 *            @c @b longjmp(3) return code that got stomped on
 *            because the source file declaring the @c @b jmp_buf
 *            had a shorter idea of the structure size than did the
 *            library function such that the return code got set to
 *            zero by the declaring file using structure packing
 *            when it thought it was updating an adjacent variable.
 *            When the library routine @c @b longjmp(3) ran, using
 *            an unpacked structure, it read what should have been
 *            the return code and returned zero instead of what had
 *            originally been there.  Or something along those lines.
 *            The solution?  Use the same structure packing everywhere,
 *            which cannot be guaranteed.  The workaround?  Declare
 *            some padding following the @c @b jmp_buf to protect the
 *            end of the structure from corruption.  How much storage?
 *            Who knows?  Therefore, simply guess and add a duplicate
 *            @c @b jmp_buf adjacent to the first, which @e should
 *            supply more than enough padding to do the job.
 *
 * @internal This structure is @e horrible.  It is @e ugly.
 *           But it @e guarantees that absolutely no structure
 *           packing or lack thereof will compromise the integrity
 *           of an initialized @c @b jmp_buf.  There really is
 *           no other way to do it except add a third member here
 *           for more padding (which is probably not necessary),
 *           or to perform heap allocation, as is done in
 *           @link jvm/src/thread.c thread.c@endlink.  However,
 *           due to the non-local return nature of the structure
 *           any way, and since there are other non-local handlers
 *           active at the same time for other conditions, it is
 *           better to be ugly and correct than simple and pretty,
 *           yet vulnerable to failures.
 *
 */
#pragma pack(1)

typedef struct
{
    jmp_buf real_member1; /**< Main part of definition */
    jmp_buf pad_member2;  /**< Add a subsequent one to force padding */
 /* jmp_buf pad_member3;  If more padding is needed, add another one */

} portable_jmp_buf;

extern portable_jmp_buf portable_exit_general_failure;
extern portable_jmp_buf portable_exit_LinkageError;

extern rint portable_sizeof_portable_jmp_buf();

/*!
 * @brief Portable macro version of @c @b setjmp(3)
 *
 * @c @b setjmp(3) cannot be called from a subroutine from the
 * protected context, but it @e can be embedded in a macro.
 * This expression of portability makes use of
 * @link #portable_jmp_buf portable_jmp_buf@endlink, but otherwise
 * looks just like a @c @b setjmp(3) call.
 *
 *
 * @param penv   Pointer to portable edition of @c @b jmp_buf structure.
 *
 *
 * @returns 0 from setup, else return coded passed to @c @b longjmp(3)
 *          for this buffer.
 *
 */
#define PORTABLE_SETJMP(penv) setjmp((penv)->real_member1)


/*!
 * @brief Portable macro version of @c @b longjmp(3)
 *
 * @c @b longjmp(3) @e can be called from a subroutine from the
 * protected context, in fact typically often is, even though
 * @c @b setjmp(3) @e cannot be called from such a subroutine.
 * This expression of portability makes use of
 * @link #portable_jmp_buf portable_jmp_buf@endlink, but otherwise
 * looks just like a @c @b longjmp(3) call.
 *
 *
 * @param penv   Pointer to portable edition of @c @b jmp_buf structure.
 *
 * @param val    Return code to pass back to protected context.
 *
 *
 * @returns @c @b val parameter, via @c @b setjmp(3), into protected
 *          context.  There is no local return.
 *
 */
#define PORTABLE_LONGJMP(penv, val) longjmp((penv)->real_member1, \
                                            (int) (val))

#endif /* PORTABLE_JMP_BUF_VISIBLE */


/* remappings for portable_libm.c */
extern jfloat portable_jfloat_fmod(jfloat x, jfloat y);
extern jfloat portable_jdouble_fmod(jdouble x, jdouble y);

#endif /* _portable_h_defined_ */


/* EOF */
