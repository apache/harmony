#ifndef _util_h_defined_
#define _util_h_defined_

/*!
 * @file util.h
 *
 * @brief Miscellaneous utility macros and function prototypes.
 *
 * Source files whose function prototypes are defined
 * herein include:
 *
 *     @link jvm/src/argv.c argv.c@endlink
 *
 *     @link jvm/src/bytegames.c bytegames.c@endlink
 *
 *     @link jvm/src/classpath.c classpath.c@endlink
 *
 *     @link jvm/src/jvmutil.c jvm.c@endlink
 *
 *     @link jvm/src/manifest.c manifest.c@endlink
 *
 *     @link jvm/src/stdio.c stdio.c@endlink
 *
 *     @link jvm/src/timeslice.c timeslice.c@endlink
 *
 *     @link jvm/src/tmparea.c tmparea.c@endlink
 *
 * In order to separate the structure packing demands of
 * <b><code>typedef struct {} ClassFile</code></b>, the
 * following files will @e not have their prototypes
 * defined here, even though they are utility functions:
 * 
 *     @link jvm/src/nts.c nts.c@endlink
 *
 *     @link jvm/src/portable_libc.c portable_libc.c@endlink
 *
 *     @link jvm/src/portable_libm.c portable_libm.c@endlink
 *
 *     @link jvm/src/portable_jmp_buf.c portable_jmp_buf.c@endlink
 *
 *     @link jvm/src/unicode.c unicode.c@endlink
 *
 *     @link jvm/src/utf.c utf.c@endlink
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
 *     @link jvm/src/unicode.c unicode.c@endlink
 *
 *     @link jvm/src/utf.c utf.c@endlink
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

ARCH_HEADER_COPYRIGHT_APACHE(util, h,
"$URL$",
"$Id$");


/* Prototypes for functions in 'bytegames.c' */
extern rushort bytegames_getrs2(rushort    *ptr2);
extern rushort bytegames_getrs2_le(rushort *ptr2);

extern ruint   bytegames_getri4(ruint      *ptr4);
extern ruint   bytegames_getri4_le(ruint   *ptr4);

extern rulong  bytegames_getrl8(rulong    *ptr8);

extern rvoid   bytegames_putrs2(rushort *ptr2, rushort val2);
extern rvoid   bytegames_putri4(ruint   *ptr4, ruint   val4);
extern rvoid   bytegames_putrl8(rulong  *ptr8, rulong  val8);

extern rushort bytegames_swap2(rushort val);
extern ruint   bytegames_swap4(ruint   val);

extern rulong  bytegames_swap8(rulong  val);
extern rulong  bytegames_mix8(rulong   val);

extern jlong   bytegames_combine_jlong(jint msword, jint lsword);
extern jdouble bytegames_combine_jdouble(jint msword, jint lsword);

extern rvoid bytegames_split_jlong(jlong splitlong,
                                   jint *msword,
                                   jint *lsword);
extern rvoid bytegames_split_jdouble(jdouble  splitdouble,
                                     jint    *msword,
                                     jint    *lsword);
/*!
 * @todo  HARMONY-6-jvm-util.h-1 Make sure GETRI4() works
 *        with -m64 compilations (64-bit ptrs)
 *
 * @todo  HARMONY-6-jvm-util.h-2 Make sure GETRL8() works
 *        with -m64 compilations (64-bit ptrs)
 *
 *
 * @internal See @link jvm/include/arch.h arch.h@endlink for the origin
 *           of architecture-specific @c @b \#define's as used below:
 *
 */
#ifdef ARCH_ODD_ADDRESS_SIGSEGV
#define GETRS2(ptr) bytegames_getrs2(ptr)
#define GETRI4(ptr) bytegames_getri4(ptr)
#define GETRL8(ptr) bytegames_getrl8(ptr)
#else
#ifdef ARCH_BIG_ENDIAN
#define GETRS2(ptr) (*(rushort *) (ptr))
#define GETRI4(ptr) (*(ruint   *) (ptr))
#define GETRL8(ptr) (*(rulong  *) (ptr))
#else
#define GETRS2(ptr) bytegames_getrs2(ptr)
#define GETRI4(ptr) bytegames_getri4(ptr)
#define GETRL8(ptr) bytegames_getrl8(ptr)
#endif
#endif


#ifdef ARCH_LITTLE_ENDIAN

#define MACHINE_JSHORT_SWAP(rsval) rsval = bytegames_swap2(rsval)
#define MACHINE_JINT_SWAP(rival)   rival = bytegames_swap4(rival)
#define MACHINE_JLONG_SWAP(rlval)  rlval = bytegames_swap8(rlval)
#define MACHINE_JLONG_MIX(rlval)   rlval = bytegames_mix8(rlval)

#define MACHINE_JSHORT_SWAP_PTR(prsval) *prsval = \
                                             bytegames_swap2(*prsval)
#define MACHINE_JINT_SWAP_PTR(prival)   *prival = \
                                             bytegames_swap4(*prival)
#define MACHINE_JLONG_SWAP_PTR(prlval)  *prlval = \
                                             bytegames_swap8(*prlval)
#define MACHINE_JLONG_MIX_PTR(prlval)   *prlval = \
                                             bytegames_mix8(*prlval)

#else

/*!
 * @internal Big endian architectures do not need to swap anything
 *           since the JVM spec declares its (short) and (int)
 *           structures as big endian.
 *
 */

#define MACHINE_JSHORT_SWAP(p1)
#define MACHINE_JINT_SWAP(p1)
#define MACHINE_JLONG_SWAP(p1)
#define MACHINE_JLONG_MIX(p1)

#define MACHINE_JSHORT_SWAP_PTR(p1)
#define MACHINE_JLONG_SWAP_PTR(p1)
#define MACHINE_JLONG_MIX_PTR(p1)

#endif


/* Prototypes for functions in 'stdio.c' */

#ifndef I_AM_STDIO_C
extern rvoid sysDbgMsg(jvm_debug_level_enum dml,
                       const rchar *fn,
                       rchar *fmt,
                       ...);
extern rvoid sysErrMsg(const rchar *fn, rchar *fmt, ...);
extern rvoid sysErrMsgBfrFormat(rchar *bfr, rchar *fn, rchar *fmt, ...);
extern rvoid sprintfLocal(rchar *bfr, rchar *fmt, ...);
extern rvoid fprintfLocalStderr(rchar *fmt, ...);
extern rvoid fprintfLocalStdout(rchar *fmt, ...);
#endif

/*!
 * @internal <b>DO NOT</b> (!) define prototypes for
 *           _printfLocal() or _fprintfLocal() or _sprintfLocal().
 *           They are meant to promote use of printfLocal() and
 *           fprintfLocal() and sprintfLocal() instead, so let
 *           compile warnings help.
 *
 */


/* Prototypes for functions in 'argv.c' */

extern rvoid argv_init(int argc, char **argv, char **envp);

extern rvoid argv_shutdown(rvoid);

extern rvoid argv_versionmsg(rvoid);

extern rvoid argv_copyrightmsg(rvoid);

extern rvoid argv_licensemsg(rvoid);

extern rvoid argv_helpmsg(rvoid);

extern rvoid argv_showmsg(rvoid);


/* Prototypes for small utility functions in 'jvmutil.c' */

#define DBGMSG_PRINT(dml) (jvmutil_get_dml() >= dml)

extern rvoid                jvmutil_set_dml(jvm_debug_level_enum level);
extern jvm_debug_level_enum jvmutil_get_dml(rvoid);

extern rvoid jvmutil_print_stack(jvm_thread_index  thridx,
                                 rchar            *pheader);
extern rvoid jvmutil_print_stack_details(jvm_thread_index  thridx,
                                         rchar            *pheader);
extern rvoid jvmutil_print_stack_locals(jvm_thread_index  thridx,
                                        rchar            *pheader);
extern rvoid jvmutil_print_error_stack(jvm_thread_index thridx);
extern rvoid jvmutil_print_exception_stack(jvm_thread_index thridx);


/* Prototypes for functions in 'manifest.c' */

extern rchar *manifest_get_main(rchar *mnfname);


/* Prototypes for functions in 'timeslice.c' */

extern rvoid timeslice_shutdown(rvoid);
extern jlong timeslice_get_thread_sleeptime(jvm_thread_index thridx);
extern void *timeslice_run(void *dummy);
extern rvoid timeslice_init(rvoid);


/* Prototypes for functions in 'tmparea.c' */

extern rvoid tmparea_init(char **argv);
extern const rchar *tmparea_get(rvoid);
extern rvoid tmparea_shutdown(rvoid);

#endif /* _util_h_defined_ */


/* EOF */
