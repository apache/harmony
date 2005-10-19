/*!
 * @file jrtypes.c
 *
 * @brief Java architecture types convenient for C/C++ source code.
 *
 * Full escriptions of all of the following variables
 * may be found in @link jvm/src/jrtypes.h jrtypes.h@endlink

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
ARCH_SOURCE_COPYRIGHT_APACHE(jrtypes, c,
"$URL$",
"$Id$");


/*!
 * @brief Permit use of @c @b TRUE, @c @b FALSE,
 * @c @b NEITHER_TRUE_NOR_FALSE
 * with @link jvm/src/jvmcfg.h jvmcfg.h@endlink
 */
#define I_AM_JRTYPES_C

#include "jvmcfg.h"

const jvoid    *jnull                   = ((jvoid *) NULL);

const jboolean jfalse                   = ((jboolean) JNI_FALSE);
const jboolean jtrue                    = ((jboolean) JNI_TRUE);




const void    *rnull                    = NULL;

const rboolean rfalse                   = FALSE;
const rboolean rtrue                    = TRUE;
const rboolean rneither_true_nor_false  = NEITHER_TRUE_NOR_FALSE;


/* EOF */
