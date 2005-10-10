/*!
 * @file classpath.c
 *
 * @brief Extract @b CLASSPATH runtime variables from the environment
 * and/or the command line or other appropriate sources.
 *
 * The HEAP_INIT() function must have been called before using
 * these functions so the environment variables can be stored
 * into it and not depend on the argument or environment pointers
 * to always be unchanged.
 *
 * The tmparea_init() function must have been called before these
 * functions so the internal @b CLASSPATH can be set up properly.
 *
 *
 * @section Control
 *
 * \$URL: https://svn.apache.org/path/name/classpath.c $ \$Id: classpath.c 0 09/28/2005 dlydick $
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
ARCH_COPYRIGHT_APACHE(classpath, c, "$URL: https://svn.apache.org/path/name/classpath.c $ $Id: classpath.c 0 09/28/2005 dlydick $");


#include <string.h>
#include <stdlib.h>
#include <sys/stat.h>

#include "jvmcfg.h" 
#include "cfmacros.h" 
#include "classfile.h" 
#include "classpath.h" 
#include "exit.h" 
#include "heap.h" 
#include "linkage.h" 
#include "jvm.h" 
#include "nts.h" 
#include "utf.h" 
#include "util.h" 

/*!
 *
 * @brief Initialize @b CLASSPATH search area of the JVM model.
 * 
 * Break @b CLASSPATH apart into its constituent paths.  Run once
 * during startup to parse @b CLASSPATH.  Heap management must be
 * started before calling this function via HEAP_INIT().  The
 * command line must also have been scanned via argv_init().
 *
 * The tmparea_init() function must have been called before these
 * functions so the internal @b CLASSPATH can be set up properly.
 *
 *
 * @param  argc    Number of arguments on command line
 *
 * @param  argv    Argument vector from the command line
 *
 * @param  envp    Environment pointer from command line environ
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 *
 * @todo  Add proper searching for @c @b rt.jar file
 *        and @c @b Xbootclasspath .
 *        For the moment, they are defined in
 *        @link  config.h config.h@endlink as the
 *        @link #CONFIG_HACKED_RTJARFILE CONFIG_HACKED_RTJARFILE@endlink
 *        and
  @link #CONFIG_HACKED_BOOTCLASSPATH CONFIG_HACKED_BOOTCLASSPATH@endlink
 *        pre-processor symbols and are commented in
 *        @link jvm/src/jvmcfg.h jvmcfg.h@endlink after this
 *        fashion.
 *
 */
static rchar **classpath_list    = CHEAT_AND_USE_NULL_TO_INITIALIZE;
static rint   classpath_list_len = 0;

rvoid classpath_init()
{
    /* Initialize only once */
    if (rnull != classpath_list)
    {
        return;
    }

    rint i;
    rint pathcount;

    /*
     * Prepare to concatenate the temp directory area with actual
     * @b CLASSPATH, followed by potential
     * CONFIG_HACKED_xxx definitions.
     */
    rchar *tmpclasspath;                    /* @b CLASSPATH dlm */
    rint  tmpcplen = strlen(tmparea_get())   + sizeof(rchar)   +
                     strlen(pjvm->classpath) + sizeof(rchar);

    /*
     * Compensate for CONFIG_HACKED_xxx definitions, handy development
     * hooks for when @b CLASSPATH is in question.
     */
#ifdef CONFIG_HACKED_BOOTCLASSPATH
                                                   /* @b CLASSPATH dlm*/
    tmpcplen += strlen(CONFIG_HACKED_BOOTCLASSPATH) + sizeof(rchar);
#endif
#ifdef CONFIG_HACKED_RTJARFILE

    /* For $JAVA_HOME/$CONFIG_HACKED_RTJARFILE */
    tmpcplen += strlen(pjvm->java_home);
    tmpcplen += sizeof(rchar);
    tmpcplen += strlen(CONFIG_HACKED_RTJARFILE)     + sizeof(rchar);
#endif
    tmpcplen += sizeof(rchar);/* NUL byte, possibly 1 more than needed*/

    /*
     * Allocate space for classpath image with temp area and
     * possible CONFIG_HACKED_xxx adjustments
     */
    tmpclasspath = HEAP_GET_DATA(tmpcplen, rfalse);

    /*
     * Generate concatenation of
     *
     * pjvm->TMPAREA:classpath:
@link #CONFIG_HACKED_BOOTCLASSPATH CONFIG_HACKED_BOOTCLASSPATH@endlink:
     * @link #CONFIG_HACKED_RTJARFILE CONFIG_HACKED_RTJARFILE@endlink
     */
    strcpy(tmpclasspath, tmparea_get());
    i = strlen(tmpclasspath);
    tmpclasspath[i] = CLASSPATH_ITEM_DELIMITER_CHAR;
    tmpclasspath[i + 1] = '\0';

    strcat(tmpclasspath, pjvm->classpath);

#ifdef CONFIG_HACKED_BOOTCLASSPATH
     i = strlen(tmpclasspath);
    tmpclasspath[i] = CLASSPATH_ITEM_DELIMITER_CHAR;
    tmpclasspath[i + 1] = '\0';
    strcat(tmpclasspath, CONFIG_HACKED_BOOTCLASSPATH);
#endif
#ifdef CONFIG_HACKED_RTJARFILE
     i = strlen(tmpclasspath);
    tmpclasspath[i] = CLASSPATH_ITEM_DELIMITER_CHAR;
    tmpclasspath[i + 1] = '\0';
    strcat(tmpclasspath, pjvm->java_home);

     i = strlen(tmpclasspath);
    tmpclasspath[i] = CLASSPATH_ITEM_DELIMITER_CHAR;
    tmpclasspath[i + 1] = '\0';
    strcat(tmpclasspath, CONFIG_HACKED_RTJARFILE);
#endif
    HEAP_FREE_DATA(pjvm->classpath); /* May or may not be on heap */
    pjvm->classpath = tmpclasspath;  /* Keep for duration of pgm run */
    

    /* WARNING!  NON-STANDARD TERMINATION CONDITION <= VERSUS < */
    for (i = 0, pathcount = 0; i <= strlen(tmpclasspath); i++)
    {
        if ((CLASSPATH_ITEM_DELIMITER_CHAR == tmpclasspath[i]) ||
            (i == strlen(tmpclasspath)))
        {
            pathcount++;
        }
    }


    /* Allocate space for list of @b CLASSPATH entries */
    classpath_list = HEAP_GET_DATA(pathcount * sizeof(rchar *), rtrue);

    rchar *nextpath;
    rint  thislen;
    classpath_list_len = 0;

    /* WARNING!  NON-STANDARD TERMINATION CONDITION <= VERSUS < */
    for (i = 0, nextpath = tmpclasspath;
         i <= strlen(tmpclasspath);
         i++)
    {

        /* If found item delimiter OR END OF STRING (SEE ABOVE) */
        if ((CLASSPATH_ITEM_DELIMITER_CHAR == tmpclasspath[i]) ||
            (i == strlen(tmpclasspath)))
        {
            /* calculate length of this @b CLASSPATH entry */
            thislen = (&tmpclasspath[i]) - nextpath;

            /*
             * Ignore double-delimiter cases.
             * It does not hurt any thing for @b classpath_list
             * to be longer than the pointers slots allocated
             * in it because @b classpath_list_len limits the
             * range of usage to those validly allocated.
             */
            if (0 == thislen)
            {
                /* Pretend it was valid */
                nextpath = &tmpclasspath[i + 1];
                continue;
            }

            /*
             * Allocate enough space for item, plus final '\0'.
             * Since we are scanning for a delimiter or EOS,
             * the current length calculation includes the "1 + x"
             * for the '\0'.  The string[x] location is set to '\0'.
             */
            classpath_list[classpath_list_len] =
                HEAP_GET_DATA(thislen + sizeof(rchar), rfalse);

            /* Store current @b CLASSPATH item, including final '\0' */
            memcpy(classpath_list[classpath_list_len],
                   nextpath, 
                   thislen);
            classpath_list[classpath_list_len][thislen] = '\0';
            classpath_list_len++;

            /* Start looking at next @b CLASSPATH item */
            nextpath = &tmpclasspath[i + 1];

        } /* if tmpclasspath[i] */

    } /* for i */

    /* Declare this module initialized */
    jvm_classpath_initialized = rtrue;

    return;

} /* END of classpath_init() */


/*!
 * @brief Determine whether or not a @b CLASSPATH entry is a JAR file
 * instead of being a directory name.
 *
 * A JAR file will be named @c @b /path/name/filename.jar, while a
 * file name in a directory will be named
 * @c @b /path/name/ClassName.class .
 *
 *
 * @param  pclasspath  String from @b CLASSPATH list
 *
 * @returns @link #rtrue rtrue@endlink if string ends with
 *          @c @b .jar, @link #rfalse rfalse@endlink otherwise.
 *
 */
rboolean classpath_isjar(rchar *pclasspath)
{
    rint len, jarlen;

    /* Lengths of test string and of JAR extension (w/ name.ext dlm)*/
    len = strlen(pclasspath);
    jarlen = strlen(JVMCFG_EXTENSION_DELIMITER_STRING) +
             strlen(CLASSFILE_EXTENSION_JAR);

    /* For VERY short @b CLASSPATH entries, it cannot be a JAR file */
    if (jarlen >= len)
    {
        return(rfalse);
    }

    /* Check if name.ext delimiter present in test string */
    if (JVMCFG_EXTENSION_DELIMITER_CHAR != pclasspath[len - jarlen])
    {
        return(rfalse);
    }

    /* Now go test JAR extension since delimiter is present */
    jarlen--;
    if (0 == strncmp(&pclasspath[len - jarlen],
                     CLASSFILE_EXTENSION_JAR,
                     jarlen))
    {
        return(rtrue);
    }
    else
    {
        return(rfalse);
    }

} /* END of classpath_isjar() */


/*!
 * @brief Convert class name format external to internal form.
 
 * The external format is @c @b class.name.format , while the
 * internal format is @c @b class/name/format .
 * Result is unchanged if it is already in internal format.
 * When finished with result, call HEAP_FREE_DATA().
 *
 *
 * @param  clsname   Null-terminated string containing class name.
 *
 *
 * @returns Null terminated string in internal format.
 *          Call HEAP_FREE_DATA() when finished with buffer.
 *
 */

rchar *classpath_external2internal_classname(rchar *clsname)
{
    rint len = strlen(clsname);
    rchar *rc = HEAP_GET_DATA(1 + len, rfalse); /* 1 + for NUL byte */

    memcpy(rc, clsname, 1 + len); /* 1 + for NUL byte */

    return(classpath_external2internal_classname_inplace(rc));

} /* END of classpath_external2internal_classname() */


/*
 * In-place version of classpath_externam2internal_classname():
 * Takes an existing buffer and performs the conversion on it
 * @e without heap allocation.  Return the input buffer.
 *
 *
 * @param[in,out] inoutbfr  Existing buffer containing text to be
 *                          translated, also receive output
 *
 *
 * @returns buffer address @b inoutbfr
 *
 */
rchar *classpath_external2internal_classname_inplace(rchar *inoutbfr)
{
    rint i;
    int len = strlen(inoutbfr);

    for (i = 0; i < len; i++)
    {
        /*
         * Substitute internal/external delimiter character
         * in @b inoutbfr where needed.
         */
        if (CLASSNAME_EXTERNAL_DELIMITER_CHAR == inoutbfr[i])
        {
            inoutbfr[i] = CLASSNAME_INTERNAL_DELIMITER_CHAR;
        }
     }

    return(inoutbfr);

} /* END of classpath_external2internal_classname_inplace() */


/*!
 * @brief Search @b CLASSPATH for a given class name using a prchar.
 *
 * Return heap pointer to a buffer containing its location.
 * If not found, return @link #rnull rnull@endlink.
 * If a class by this name is stored in more than one location, only
 * the first location is returned.  When done with result, call
 * HEAP_FREE_DATA(result) to return buffer to heap area.
 *
 * All CLASSNAME_EXTERNAL_DELIMITER (ASCII period) characters
 * found in the input class name will be unconditionally replaced
 * with CLASSNAME_INTERNAL_DELIMITER (ASCII slash) characters.
 * Therefore, the class file extension CLASSFILE_EXTENSION_DEFAULT
 * may not be appended to the class name.  This constraint permits
 * both internal and external class names to use the same function
 * to search for classes.
 *
 *
 * @param  clsname  Name of class, without @c @b .class
 *                  extension, as either @c @b some.class.name
 *                  or @c @b some/class/name , that is,
 *                  the internal form of the class name.  The string
 *                  may or may not contain class formatting of the
 *                  form @c @b [[[Lsome/class/name;
 *
 *
 * @returns Heap pointer into @b CLASSPATH of directory or JAR file
 *          containing class (for a regular .class file).
 *          For a JAR file, report the name of the .jar file
 *          as for a .class file, but also call classpath_isjar()
 *          to distinguish between them.  Thus the usage is,
 *          Return @link #rnull rnull@endlink if no match.
 *
 *         @todo VM Spec section 5.3.1:  Throw 'NoClassDeffoundError'
 *               if no match.
 *
 *         Notice that @b clsname must be specified with package
 *         designations using INTERNAL (slash) delimiter form of
 *         the path.  This is what is natively found in the class
 *         files.  Of course, no package name means the simple
 *         default package, that is, an unpackaged class having
 *         no <b><code>package some.package.name</code></b> statement
 *         in source.
 *
 * @verbatim
               rchar *p = classpath_get_from_prchar(
                                     "some/package/name/SomeClassName");
  
               if (rnull != p)
               {
             
                   if (rtrue == classpath_isjar(p))
                   {
                       ** Extract class from JAR file **
                   }
                   else
                   {
                       ** Read class file directly **
                   }
               }
   @endverbatim
 *
 */

rchar *classpath_get_from_prchar(rchar *clsname)
{
    rint i;
    struct stat statbfr;
    rchar *name;
    int baselen;

    rchar *class_location = HEAP_GET_DATA(JVMCFG_PATH_MAX, rfalse);

    if (rtrue == nts_prchar_isclassformatted(clsname))
    {
        /*
         * Convert @c @b [[[Lpath/name/ClassName; into
         * @c @b path/name/ClassName
         */
        jvm_array_dim arraydims = nts_get_prchar_arraydims(clsname);
        name = &clsname[1 + arraydims];

        /* Calc position of end-of-class delimiter */
        rchar *pdlm = strchr(name, BASETYPE_CHAR_L_TERM);
        baselen = strlen(name);

        /* Should @e always be @link #rtrue rtrue@endlink */
        if (rnull != pdlm)
        {
            baselen = pdlm - name;
        }
    }
    else
    {
        /*
         * If this class name string contained no formatting,
         * fake the adjustment above to a formatted class name.
         * Notice that without formatting, there cannot be any
         * array dimensions.
         */
        name = clsname;
        baselen = strlen(name);
    }

    rchar *jarscript = HEAP_GET_DATA(JVMCFG_SCRIPT_MAX, rfalse);

    /*
     * Search through each entry in @b CLASSPATH for a file by
     * the proper name.
     */

    for (i = 0; i < classpath_list_len; i++)
    {
        int clen;

        /* Test for JAR files in @b CLASSPATH */
        if (rtrue == classpath_isjar(classpath_list[i]))
        {
            /* Convert input parm to internal form, append suffix */
            strcpy(class_location, name);
            clen = strlen(class_location);
            (rvoid) classpath_external2internal_classname_inplace(
                                                        class_location);
            class_location[clen] = JVMCFG_EXTENSION_DELIMITER_CHAR;
            class_location[clen + 1] = '\0';
            strcat(class_location, CLASSFILE_EXTENSION_DEFAULT);

            /*!
             * @internal Build up JAR command using internal class name
             * with suffix.  Make @e sure all files are writeable
             * for final <b><code>rm -rf</code></b>.
             */
             
            
            sprintfLocal(jarscript,
                         JVMCFG_JARFILE_DATA_EXTRACT_SCRIPT,
                         tmparea_get(),
                         pjvm->java_home,
                         pjvm->java_home,
                         JVMCFG_PATHNAME_DELIMITER_CHAR,
                         classpath_list[i],
                         class_location);

#ifdef CONFIG_WINDOWS

           /*
            *   gmj : awful hack - need to escape out every \ in paths
            *   or it doesn't seem to get across into the batch file correctly
            */
		   rchar *fixscript = HEAP_GET_DATA(JVMCFG_SCRIPT_MAX, rfalse);

           char *buffptr = fixscript;
		   char *jarscriptPtr = jarscript;
		              
		   char c;
           while((c = *jarscriptPtr++)) {
           		*buffptr++ = c;
           		
	             if (c == '\\') 
	             {
	           	    *buffptr++ = '\\';
	             } 
           }
           *buffptr = '\0';
           
           strcpy(jarscript, fixscript);
           HEAP_FREE_DATA(fixscript);
                        
#endif

           int rc = system(jarscript);

            if (0 != rc)
            {
                sysErrMsg("classpath_get_from_prchar",
                          "Cannot extract '%s' from JAR file %s",
                          class_location,
                          classpath_list[i]);
                exit_jvm(EXIT_CLASSPATH_JAR);
/*NOTREACHED*/
            }

            /* Location of extracted file */
            sprintfLocal(jarscript,
                         "%s%c%s",
                         tmparea_get(),
                         JVMCFG_PATHNAME_DELIMITER_CHAR,
                         class_location);

            rc = stat(jarscript, &statbfr);

            /*
             * If file was extracted, report result
             * in heap-allocated bfr
             */
            if (0 == rc)
            {
                HEAP_FREE_DATA(class_location);

                return(jarscript);
            }

            HEAP_FREE_DATA(jarscript);
        }
        else
        {
            /* Convert input parm to internal form */
            sprintfLocal(class_location,
                         "%s%c\0",
                         classpath_list[i],
                         JVMCFG_PATHNAME_DELIMITER_CHAR);

            clen = strlen(class_location);

            strcat(class_location, name);

            /*
             * Convert input parm to internal format and append
             * class suffix, but convert @e only the @b name part
             * just appended, and not if if it is a JAR file.
             */
            if (rfalse == classpath_isjar(class_location))
            {
                (rvoid) classpath_external2internal_classname_inplace(
                                                 &class_location[clen]);


                class_location[clen + baselen] =
                                        JVMCFG_EXTENSION_DELIMITER_CHAR;
                class_location[clen + baselen + 1] = '\0';

                strcat(class_location, CLASSFILE_EXTENSION_DEFAULT);
            }

            /* Test for existence of valid class file */
            int rc = stat(class_location, &statbfr);

            /* If match found, report result in heap-allocated bfr */
            if (0 == rc)
            {
                return(class_location);
            }
        }
    } /* for i */

    /* Class not found in @b CLASSPATH */
    HEAP_FREE_DATA(class_location);
    return((rchar *) rnull);

} /* END of classpath_get_from_prchar() */


/*!
 * @brief Search @b CLASSPATH for a given class name using
 * a CONSTANT_Utf8_info.
 *
 * Invoke @link #classpath_get_from_prchar()
   classpath_get_from_prchar@endlink after converting @b clsname
 * from CONSTANT_Utf8_info to prchar.
 *
 * For more information, see @link #classpath_get_from_prchar()
   classpath_get_from_prchar@endlink.
 *
 *
 * @param  clsname  Name of class, without @c @b .class
 *                  extension, as either @c @b some.class.name
 *                  or @c @b some/class/name , that is,
 *                  the internal form of the class name.  The string
 *                  may or may not contain class formatting of the
 *                  form @c @b [[[Lsome/class/name;
 *
 *
 * @returns Heap pointer into @b CLASSPATH of directory or JAR file
 *          containing class (for a regular .class file).
 *
 */

rchar *classpath_get_from_cp_entry_utf(cp_info_dup *clsname)
{
    rchar *prchar_clsname = utf_utf2prchar(PTR_THIS_CP_Utf8(clsname));

    rchar *rc = classpath_get_from_prchar(prchar_clsname);

    HEAP_FREE_DATA(prchar_clsname);

    return(rc);

} /* END of classpath_get_from_cp_entry_utf() */


/*!
 * @brief Shut down the @b CLASSPATH search area of the JVM model after
 * JVM execution.
 * 
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 *       @returns @link #rvoid rvoid@endlink
 *
 */
rvoid classpath_shutdown()
{
    rint i;

    for (i = 0; i < classpath_list_len; i++)
    {
        HEAP_FREE_DATA(classpath_list[i]);
    }
    HEAP_FREE_DATA(classpath_list);

    classpath_list = (rchar **) rnull;
    classpath_list_len = 0;

    /* Declare this module uninitialized */
    jvm_classpath_initialized = rfalse;

    return;

} /* END of classpath_shutdown() */


/* EOF */
