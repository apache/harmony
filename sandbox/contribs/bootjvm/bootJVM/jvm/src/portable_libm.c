/*!
 * @file portable_libm.c
 *
 * @brief Portable version of @b system(2) calls and @b library(3)
 * utility functions.
 *
 * Isolate all section 3 math library functions from the normal
 * compilation environment so as to eliminate all questions about
 * @b libm compilation modes, especially use of structure packing.
 * For GCC, this means how the compile option <b>-fpack-struct</b>
 * was or was not used.
 *
 * For other comments, please refer to
 * @link jvm/src/portable_libc.c portable_libc.c@endlink.
 *
 * @see @link jvm/src/portable_libc.c portable_libc.c@endlink
 *
 * @see @link jvm/src/portable_jmp_buf.c portable_jmp_buf.c@endlink
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

#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include "arch.h"
ARCH_SOURCE_COPYRIGHT_APACHE(portable_libm, c,
"$URL$",
"$Id$");

 
#define I_AM_PORTABLE_C /* Suppress function name remapping */
#include "jvmcfg.h"


/*!
 * @name fmod() function for both jfloat and jdouble operands.
 *
 *
 * @param x  First of two operands, the divisor
 *
 * @param y  Second of two operands, the dividend
 *
 *
 * @returns The remainder of dividing @c @b x by @c @b y
 *
 */

/*@{ */ /* Begin grouped definitions */

/*!
 * @brief Portable replacement for @c @b fmod(3) library function for
 * @link #jfloat jfloat@endlink operands.
 *
 * Cast @link #jfloat jfloat@endlink operands as native
 * machine @c @b float and invoke @c @b fmod(3) .  Cast the
 * @c @b double result back into a @link #jfloat jfloat@endlink
 * for return.
 *
 */
jfloat portable_jfloat_fmod(jfloat x, jfloat y)
{
    ARCH_FUNCTION_NAME(portable_jfloat_fmod);

    double xlocal = (double) x;
    double ylocal = (double) y;

    double rc = fmod(xlocal, ylocal);

    return((jfloat) rc);

} /* END of portable_jfloat_fmod() */


/*!
 * @brief Portable replacement for @c @b fmod(3) library function for
 * @link #jdouble jdouble@endlink operands.
 *
 *
 * Cast @link #jdouble jdouble@endlink operands as native
 * machine @c @b double and invoke @c @b fmod(3) .  Cast the
 * @c @b double result back into a @link #jdouble jdouble@endlink
 * for return.
 *
 */
jfloat portable_jdouble_fmod(jdouble x, jdouble y)
{
    ARCH_FUNCTION_NAME(portable_jdouble_fmod);

    double xlocal = (double) x;
    double ylocal = (double) y;

    double rc = fmod(xlocal, ylocal);

    return((jdouble) rc);

} /* END of portable_jdouble_fmod() */

/*@} */ /* End of grouped definitions */


/* EOF */
