/*!
 * @file portable_jmp_buf.c
 *
 * @brief Portable version of @b jmp_buf structures.
 *
 * Isolate all @c @b jmp_buf declarations from the normal compilation
 * environment so as to eliminate all questions about @b libc
 * compilation modes, especially use of structure packing.  For GCC,
 * this means how the compile option <b>-fpack-struct</b> was or was
 * not used.
 *
 *
 * @attention A word about @c @b setjmp(3)/longjmp(3) and portability:
 *            The non-intuitive mechanism used by these functions to
 *            implement non-local returns is complemented on some
 *            systems by an equally non-intuitive definition of these
 *            functions and of the @c @b jmp_buf structure.  Since
 *            members of @c @b jmp_buf variables are @e never
 *            referenced at the API level, but @e only internally to
 *            these functions, and since some systems can generate
 *            really strange compile errors when attempting either,
 *            no attempt will be made to enforce the portability
 *            reminder mechanism on the lot.
 *
 * @attention A word about placement of @c @b setjmp(3):  Due to the
 *            non-local nature of @c @b longjmp(3), there can @e never
 *            be such as thing as a @c @b portable_setjmp() function
 *            that invokes @c @b setjmp(3) in a manner similar
 *            to more normal library calls such as those found in
 *            @link jvm/src/portable_libc.c portable_libc.c@endlink.
 *            The reason for this is non-intuitive, yet simple:
 *            Invocations of @c @b setjmp(3) use the stack state
 *            at the time of the @c @b setjmp(3) itself.  When that
 *            function returns from the initial setup, it passes
 *            back its return value, which in turn would hypothetically
 *            be passed back to the caller of the portable routine.
 *            This part works, fine, but when @c @b longjmp(3) tries
 *            to return to that stack state, it @e also will try to
 *            pass back @e its return code, which will @e also be to
 *            the portable routine.  The stack state will be @e exactly
 *            as the @c @b setjmp(3) call defined it, namely, to
 *            return into the portable routine, but
 *            <b>not to the @e caller of the portable routine!</b>
 *            So the portable routine returns from this condition,
 *            it will be popping potentially random garbage off of
 *            the stack from other stack usage following the
 *            @c @b setjmp(3).  Thus it is highly unlikely to pass
 *            the proper return code back through @c @b setjmp(3)
 *            into the invoking runtime environment.  It might even
 *            return to a wild pointer address.
 *
 *            The bottom line:  Although @c @b longjmp(3) is designed
 *            like any other function in how it may be invoked, its
 *            companion @c @b setjmp(3) must @e never be invoked from
 *            a subroutine.  It must be invoked @e directly by the
 *            runtime stack frame that is to be re-entered in the
 *            non-local location set up in the @c @b jmp_buf.
 *
 * For other comments on portability issues, please refer to
 * @link jvm/src/portable_libc.c portable_libc.c@endlink and
 * @link jvm/src/portable_libm.c portable_libm.c@endlink.
 *
 *
 *
 * @see @link jvm/src/portable_libc.c portable_libc.c@endlink
 *
 * @see @link jvm/src/portable_libm.c portable_libm.c@endlink
 *
 * @see @link jvm/src/portable.h portable.h@endlink
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
ARCH_SOURCE_COPYRIGHT_APACHE(portable_jmp_buf, c,
"$URL$",
"$Id$");

#define PORTABLE_JMP_BUF_VISIBLE
#include "jvmcfg.h"


/*!
 * @name static jmp_buf declarations gathered from around the code.
 *
 * All @c @b jmp_buf variables, typically defined previously as
 * @c @b static in their source files (thus file scope) are gathered
 * here into the portability library.
 *
 * References to these variables is typically through
 * @link #PORTABLE_SETJMP() PORTABLE_SETJMP()@endlink and
 * @link #PORTABLE_LONGJMP() PORTABLE_LONGJMP()@endlink.
 *
 */

/*@{ */ /* Begin grouped definitions */

portable_jmp_buf portable_exit_general_failure;

portable_jmp_buf portable_exit_LinkageError;

/*@} */ /* End of grouped definitions */


/*!
 * @brief Report the correct size of @c @b sizeof(jmp_buf) for use
 * by memory allocations of buffers not statically defined here in
 * this source file.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns number of bytes required by @c @b jmp_buf structures
 *
 */
rint portable_sizeof_portable_jmp_buf()
{
    ARCH_FUNCTION_NAME(portable_sizeof_portable_jmp_buf);

    return(sizeof(portable_jmp_buf));

} /* END of portable_sizeof_portable_jmp_buf() */


/* EOF */
