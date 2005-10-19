/*!
 * @file classutil.c
 *
 * @brief Utility and glue functions for
 * @link jvm/src/class.c class.c@endlink
 * and @c @b java.lang.Class
 *
 *
 * @internal Due to the fact that the implementation of the Java class
 * and the supporting rclass structure is deeply embedded in the core
 * of the development of this software, this file has contents
 * that come and go during development.  Some functions get
 * staged here before deciding where they @e really go; some
 * are interim functions for debugging, some were glue that eventually
 * went away.  Be careful to remove prototypes to such functions
 * from the appropriate header file.
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
ARCH_COPYRIGHT_APACHE(classutil, c, "$URL$ $Id$");


#include "jvmcfg.h"
#include "classfile.h"
#include "jvm.h"


/* EOF */
