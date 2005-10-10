/*!
 * @file tmparea.c
 *
 * @brief Create and manage temporary directory area for class files,
 * etc.
 *
 *
 * @section Control
 *
 * \$URL: https://svn.apache.org/path/name/tmparea.c $ \$Id: tmparea.c 0 09/28/2005 dlydick $
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
ARCH_COPYRIGHT_APACHE(tmparea, c, "$URL: https://svn.apache.org/path/name/tmparea.c $ $Id: tmparea.c 0 09/28/2005 dlydick $");

 
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>

#include "jvmcfg.h"
#include "exit.h"
#include "heap.h" 
#include "util.h"


/*!
 * @brief Initialize the class area of the JVM model
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 *       @returns @link #rvoid rvoid@endlink
 *
 */
static rchar *env_tmpdir = CHEAT_AND_USE_NULL_TO_INITIALIZE;

static rchar tmparea[100];

rvoid tmparea_init(char **argv)
{
    struct stat statbfr;
    char *argv0name = strrchr(argv[0], JVMCFG_PATHNAME_DELIMITER_CHAR);

    if (rnull != argv0name)
    {
        argv0name++;
    }
    else
    {
        argv0name = argv[0];
    }

    int pid = getpid();

    env_tmpdir = getenv("TMPDIR");

    if (rnull == env_tmpdir)
    {
        env_tmpdir = JVMCFG_TMPAREA_DEFAULT;
    }

    sprintfLocal(tmparea,
                 "%s%ctmp.%s.%d",
                 env_tmpdir,
                 JVMCFG_PATHNAME_DELIMITER_CHAR,
                 "bootJVM", /* @todo fix gmj : argv0name, */
                 pid);
                 
    int rc = mkdir(tmparea, 0755); /* Could use <sys/stat.h> constants*/

    /* Verify existence of directory */
    rc = stat(tmparea, &statbfr);

    if (0 != rc)
    {
        sysErrMsg("tmparea_init",
                  "Cannot create temp directory %s",
                  tmparea);
        exit_jvm(EXIT_TMPAREA_MKDIR);
/*NOTREACHED*/
    }
    /* Declare this module initialized */
    jvm_tmparea_initialized = rtrue;

    return;

} /* END of tmparea_init() */


/*!
 * Retrieve the temporary directory area path.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns Null-terminated string to temp area.
 *
 */
const rchar *tmparea_get()
{
    return((const rchar *) tmparea);

} /* END of tmparea_get() */


/*!
 * @brief Shut down the temporary directory area after JVM execution.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 *       @returns @link #rvoid rvoid@endlink
 *
 */
rvoid tmparea_shutdown(rvoid)
{

/* Normal method requires directory to be empty:
 *
 *  int rc = rmdir(tmparea);
 *
 *  if (0 != rc)
 *  {
 *      sysErrMsg("tmparea_shutdown",
 *                "Cannot remove temp directory %s",
 *                tmparea);
 *      exit_jvm(EXIT_TMPAREA_RMDIR);
**NOTREACHED**
 *  }
 *
 */

    struct stat statbfr;

/* Since there will be temp files here, cheat just a @e little bit: */


    rchar *rmscript =        /* format spec %s make strlen longer than
                                it needs to be, but it is benign */
        HEAP_GET_DATA(strlen(JVMCFG_TMPAREA_REMOVE_SCRIPT) +
                          strlen(tmparea) +
                          sizeof(rchar) /* NUL byte */,
                      rfalse);

    sprintfLocal(rmscript, JVMCFG_TMPAREA_REMOVE_SCRIPT, tmparea);

    int rc = system(rmscript);

    /* Verify missing directory */
    rc = stat(tmparea, &statbfr);

    if (0 == rc)
    {
        sysErrMsg("tmparea_shutdown",
                  "Cannot remove temp directory %s",
                  tmparea);
        exit_jvm(EXIT_TMPAREA_RMDIR);
/*NOTREACHED*/
    }

    /* Declare this module uninitialized */
    jvm_tmparea_initialized = rfalse;

    return;

} /* END of tmparea_shutdown() */


/* EOF */
