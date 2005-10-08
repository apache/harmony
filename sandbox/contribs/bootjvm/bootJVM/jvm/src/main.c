/*!
 * @file jvm/src/main.c
 *
 * @brief Entry point for bootstrap Java Virtual Machine
 *
 * The normal invocation of the JVM is very simple: just pass
 * the main() parameters into the JVM entry point, then call
 * @c @b exit(2) with the return code from it.  That's all there
 * is to it from the top level.
 *
 *
 * @section Control
 *
 * \$URL: https://svn.apache.org/path/name/main.c $ \$Id: main.c 0 09/28/2005 dlydick $
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
ARCH_COPYRIGHT_APACHE(main, c, "$URL: https://svn.apache.org/path/name/main.c $ $Id: main.c 0 09/28/2005 dlydick $");


#include <stdlib.h>

#include "jvmcfg.h"
#include "classfile.h"
#include "jvm.h"


/*!
 * @brief Example of invoking the main JVM entry point
 *
 * The arguments to @link #jvm() jvm()@endlink are identical in
 * format and presentation to those used to enter a 'C' main program.
 *
 *
 * @param argc  Number of parameters being passed in @b argv
 *
 * @param argv  Array of null-terminated strings, @b argv[0] being the
 *              program name, @b argv[1] being the first argument, etc.
 *
 * @param envp  Environment pointer
 *
 *
 * @returns exit code from @link #jvm() jvm()@endlink
 *
 */
int main(int argc, char **argv, char **envp)
{
    /* Run the JVM and retrieve its return code */
    int rc = jvm(argc, argv, envp);

    /* Report the return code to the OS shell */
    exit(rc);

} /* END of main() */


/* EOF */
