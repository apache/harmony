/*!
 * @file manifest.c
 *
 * @brief Read JAR manifest file.
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
ARCH_SOURCE_COPYRIGHT_APACHE(manifest, c,
"$URL$",
"$Id$");


#include "jvmcfg.h"
#include "heap.h" 
#include "exit.h"
#include "util.h"

/*!
 * @brief Search a JAR manifest file for a main class attribute.
 *
 * This attribute MUST be all on one line and without any
 * line continuations breaking up the class name.  Look for
 * a line of text like this, where @b | indicates that the
 * following character is the first one on the line:
 *
 * @verbatim
      |
      |Main-Class: name.of.start.class\n
      |
  
   WITHOUT any line continuations,
  
      |
      |Main-Class: name.of.sta\n
      | rt.class\n
      |
  
   @endverbatim
 *
 * @todo  HARMONY-6-jvm-manifest.c-1 Although such a continuation
 *        is supported by the JAR file, this implementation does
 *        not support it (yet).  With this restriction, the
 *        if only one single space separates the name of the
 *        attribute and the class name, then since a line may be
 *        up to 72 characters long (that is
 *        @link #JVMCFG_JARFILE_MANIFEST_LINE_MAX
          JVMCFG_JARFILE_MANIFEST_LINE_MAX@endlink characters), then
 *        the class name may be 61 characters long.
 *
 *
 * @param  mnfname   JAR manifest file name, /absolute/path/name
 *         
 *
 * @returns Heap pointer to null-terminated class name, or
 *          @link #rnull rnull@endlink if not found.
 *          Call HEAP_FREE_DATA() when done.
 *
 */
rchar *manifest_get_main(rchar *mnfname)
{
    ARCH_FUNCTION_NAME(manifest_get_main);

    rvoid *mf;  /* Portability library does (FILE) part */

    mf = portable_fopen(mnfname, "r");

    if (rnull == mf)
    {
        return((rchar *) rnull);
    }

    rchar *mnfdata = HEAP_GET_DATA(JVMCFG_STDIO_BFR, rfalse);

    int mclen =  portable_strlen(JVMCFG_JARFILE_MANIFEST_MAIN_CLASS);

    /* Read until end of file or match located */
    while (mnfdata ==
           (rchar *) portable_fgets(mnfdata, JVMCFG_STDIO_BFR, mf))
    {
        /*
         * Scan for ^Main-Class: attribute name (text starting
         * at beginnin of line)
         */
        if (0 != portable_strncmp(mnfdata,
                                  JVMCFG_JARFILE_MANIFEST_MAIN_CLASS,
                                  mclen))
        {
            continue;
        }

        /*
         * Attribute name found.
         *
         * Scan for first non-white character after attribute name.
         * This will be the start of the class name.
         */
        rint i;
        for (i = mclen; i < portable_strlen(mnfdata); i++)
        {
            /* if <b>white space</b> is rfalse */
            if (0 == portable_isspace((int) mnfdata[i]))
            {
                break; /* Found first non-white-space character */
            }
        }
        /* If nothing else, the \n at end of line is white space */

        /*
         * Class name found.
         *
         * Scan for first white character after class name.
         * This will be the end of the class name.
         */
        rint j;
        for (j = i; j < portable_strlen(mnfdata); j++)
        {
            /* if <b>white space</b> is @link #rtrue rtrue@endlink */
            if (0 != portable_isspace((int) mnfdata[j]))
            {
                break;  /* Found first white-space character */
            }
        }
        /* If nothing else, the \n at end of line is white space */

        portable_fclose(mf);

        /* Allocate space for non-empty class name plus NUL byte */
        rchar *mnfresult;
        if (i != j)
        { 
            mnfresult = HEAP_GET_DATA(j - i + sizeof(rchar), rfalse);

            /* If failure above, HEAP_FREE_DATA(mnfdata) is never run */
        }
        else
        {
            /* Do not process empty class name, declare error instead */
            HEAP_FREE_DATA(mnfdata);

            sysErrMsg(arch_function_name,
                      "Missing class name in manifest file %s",
                      mnfname);
            exit_jvm(EXIT_MANIFEST_JAR);
/*NOTREACHED*/
        }

        portable_memcpy(mnfresult, &mnfdata[i], j - i);
        mnfresult[j - i] = '\0';

        HEAP_FREE_DATA(mnfdata);
        return(mnfresult);
    }

    portable_fclose(mf);

    HEAP_FREE_DATA(mnfdata);

    return((rchar *) rnull);

} /* END of manifest_get_main() */


/* EOF */
