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
ARCH_SOURCE_COPYRIGHT_APACHE(classpath, c,
"$URL$",
"$Id$");


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
 * @todo  HARMONY-6-jvm-classpath.c-1 Add proper searching
 *        for @c @b rt.jar file and @c @b Xbootclasspath .
 *        For the moment, they are defined in
 *        @link  config.h config.h@endlink as the
 *        @link #CONFIG_HACKED_RTJARFILE CONFIG_HACKED_RTJARFILE@endlink
 *        and @link #CONFIG_HACKED_BOOTCLASSPATH
          CONFIG_HACKED_BOOTCLASSPATH@endlink
 *        pre-processor symbols and are commented in
 *        @link jvm/src/jvmcfg.h jvmcfg.h@endlink after this
 *        fashion.
 *
 */
static rchar **classpath_list    = CHEAT_AND_USE_NULL_TO_INITIALIZE;
static rint   classpath_list_len = 0;

rvoid classpath_init()
{
    ARCH_FUNCTION_NAME(classpath_init);

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
    rchar *tmpclasspath;             /* @b CLASSPATH dlm */
    rint tmpcplen =
        portable_strlen(tmparea_get())   + sizeof(rchar)   +
        portable_strlen(pjvm->classpath) + sizeof(rchar);

    /*
     * Compensate for CONFIG_HACKED_xxx definitions, handy development
     * hooks for when @b CLASSPATH is in question.
     */
#ifdef CONFIG_HACKED_BOOTCLASSPATH
                                                 /* @b CLASSPATH dlm */
    tmpcplen +=
        portable_strlen(CONFIG_HACKED_BOOTCLASSPATH) + sizeof(rchar);
#endif
#ifdef CONFIG_HACKED_RTJARFILE

    /* For $JAVA_HOME/$CONFIG_HACKED_RTJARFILE */
    tmpcplen += portable_strlen(pjvm->java_home);
    tmpcplen += sizeof(rchar);
    tmpcplen += portable_strlen(CONFIG_HACKED_RTJARFILE) +sizeof(rchar);
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
    portable_strcpy(tmpclasspath, tmparea_get());
    i = portable_strlen(tmpclasspath);
    tmpclasspath[i] = CLASSPATH_ITEM_DELIMITER_CHAR;
    tmpclasspath[i + 1] = '\0';

    portable_strcat(tmpclasspath, pjvm->classpath);

#ifdef CONFIG_HACKED_BOOTCLASSPATH
     i = portable_strlen(tmpclasspath);
    tmpclasspath[i] = CLASSPATH_ITEM_DELIMITER_CHAR;
    tmpclasspath[i + 1] = '\0';
    portable_strcat(tmpclasspath, CONFIG_HACKED_BOOTCLASSPATH);
#endif
#ifdef CONFIG_HACKED_RTJARFILE
     i = portable_strlen(tmpclasspath);
    tmpclasspath[i] = CLASSPATH_ITEM_DELIMITER_CHAR;
    tmpclasspath[i + 1] = '\0';
    portable_strcat(tmpclasspath, pjvm->java_home);

     i = portable_strlen(tmpclasspath);
    tmpclasspath[i] = CLASSPATH_ITEM_DELIMITER_CHAR;
    tmpclasspath[i + 1] = '\0';
    portable_strcat(tmpclasspath, CONFIG_HACKED_RTJARFILE);
#endif
    HEAP_FREE_DATA(pjvm->classpath); /* May or may not be on heap */
    pjvm->classpath = tmpclasspath;  /* Keep for duration of pgm run */
    

    /* @warning  NON-STANDARD TERMINATION CONDITION <= VERSUS < */
    for (i = 0, pathcount = 0; i <= portable_strlen(tmpclasspath); i++)
    {
        if ((CLASSPATH_ITEM_DELIMITER_CHAR == tmpclasspath[i]) ||
            (i == portable_strlen(tmpclasspath)))
        {
            pathcount++;
        }
    }


    /* Allocate space for list of @b CLASSPATH entries */
    classpath_list = HEAP_GET_DATA(pathcount * sizeof(rchar *), rtrue);

    rchar *nextpath;
    rint  thislen;
    classpath_list_len = 0;

    /* @warning  NON-STANDARD TERMINATION CONDITION <= VERSUS < */
    for (i = 0, nextpath = tmpclasspath;
         i <= portable_strlen(tmpclasspath);
         i++)
    {

        /* If found item delimiter OR END OF STRING (SEE ABOVE) */
        if ((CLASSPATH_ITEM_DELIMITER_CHAR == tmpclasspath[i]) ||
            (i == portable_strlen(tmpclasspath)))
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
            portable_memcpy(classpath_list[classpath_list_len],
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
 *
 * @returns @link #rtrue rtrue@endlink if string ends with
 *          @c @b .jar, @link #rfalse rfalse@endlink otherwise.
 *
 */
rboolean classpath_isjar(rchar *pclasspath)
{
    ARCH_FUNCTION_NAME(classpath_isjar);

    rint len, jarlen;

    /* Lengths of test string and of JAR extension (w/ name.ext dlm) */
    len = portable_strlen(pclasspath);
    jarlen = portable_strlen(JVMCFG_EXTENSION_DELIMITER_STRING) +
             portable_strlen(CLASSFILE_EXTENSION_JAR);

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
    if (0 == portable_strncmp(&pclasspath[len - jarlen],
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
 *
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
    ARCH_FUNCTION_NAME(classpath_external2internal_classname);

    rint len = portable_strlen(clsname);
    rchar *rc = HEAP_GET_DATA(1 + len, rfalse); /* 1 + for NUL byte */

    portable_memcpy(rc, clsname, 1 + len); /* 1 + for NUL byte */

    return(classpath_external2internal_classname_inplace(rc));

} /* END of classpath_external2internal_classname() */


/*!
 * @brief Convert <em>in place</em> class name format external
 * to internal form.
 *
 * In-place version of classpath_external2internal_classname():
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
    ARCH_FUNCTION_NAME(classpath_external2internal_classname_inplace);

    rint i;
    int len = portable_strlen(inoutbfr);

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
 * @brief Adjust class name string for shell expansion artifacts.
 *
 * Insert a backslash character in front of the dollar sign of any
 * inner class name so that shell expansion does not see the dollar
 * sign as a special character representing a shell variable.  Unless
 * this step is taken, such a shell variable, most likely non-existent,
 * will effectively truncate the string before the dollar sign.
 *
 *
 * @param pclass_location  Null-terminated string containing a fully
 *                         qualified class name, typically a path into a
 *                         JAR file.
 *
 *
 * @returns buffer address @b outbfr.  When finished with this buffer,
 *          return it to the heap with
 *          @link #HEAP_FREE_DATA() HEAP_FREE_DATA@endlink.  Notice
 *          that if a buffer has no dollar signs, this function
 *          is equivalent to strcpy(3).
 *
 */
rchar *classpath_inner_class_adjust(rchar *pclass_location)
{
    ARCH_FUNCTION_NAME(classpath_inner_class_adjust);

    int len = portable_strlen(pclass_location);

    rint cllen = (rint) len;
    rint i, j;
    rint numdollar = 0;

#ifndef CONFIG_WINDOWS
    for (i = 0; i < cllen; i++)
    {
        if (CLASSNAME_INNERCLASS_MARKER_CHAR == pclass_location[i])
        {
            numdollar++;
        }
    }
#endif

    rchar *outbfr = HEAP_GET_DATA(cllen + numdollar, rfalse);

    /*
     * @warning  NON-STANDARD TERMINATION CONDITION <= VERSUS <
     *           (so that NUL byte gets copied without any
     *           special consideration).
     */
    for (i = 0, j = 0; i <= cllen; /* Done in body of loop: i++ */ )
    {
        /* Windows platforms don't need these escapes */
#ifndef CONFIG_WINDOWS
        /* Insert escape character before dollar sign where found */
        if (CLASSNAME_INNERCLASS_MARKER_CHAR == pclass_location[i])
        {
            outbfr[j++] = CLASSNAME_INNERCLASS_ESCAPE_CHAR;
        }
#endif

        /* Copy next input character to output buffer */
        outbfr[j++] = pclass_location[i++];
    }

    /* Return buffer expanded with escapes where needed */
    return(outbfr);

} /* END of classpath_inner_class_adjust() */


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
 * @todo HARMONY-6-jvm-classpath.c-2 VM Spec section 5.3.1:  Throw
 *       @b NoClassDeffoundError if no match.
 *
 *       Notice that @b clsname must be specified with package
 *       designations using INTERNAL (slash) delimiter form of
 *       the path.  This is what is natively found in the class
 *       files.  Of course, no package name means the simple
 *       default package, that is, an unpackaged class having
 *       no <b><code>package some.package.name</code></b> statement
 *       in source.
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
    ARCH_FUNCTION_NAME(classpath_get_from_prchar);

    rvoid *statbfr; /* Portability library does (struct stat) part */

    rint i;
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
        rchar *pdlm = portable_strchr(name, BASETYPE_CHAR_L_TERM);
        baselen = portable_strlen(name);

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
        baselen = portable_strlen(name);
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
            portable_strcpy(class_location, name);
            clen = portable_strlen(class_location);
            (rvoid) classpath_external2internal_classname_inplace(
                                                        class_location);
            class_location[clen] = JVMCFG_EXTENSION_DELIMITER_CHAR;
            class_location[clen + 1] = '\0';
            portable_strcat(class_location,
                            CLASSFILE_EXTENSION_DEFAULT);

            rchar *inner_class_location =
                classpath_inner_class_adjust(class_location);

            /*!
             * @internal Build up JAR command using internal class name
             *           with suffix.  Make @e sure all files are
             *           writeable for final <b><code>rm -rf</code></b>.
             */
            sprintfLocal(jarscript,
                         JVMCFG_JARFILE_DATA_EXTRACT_SCRIPT,
                         tmparea_get(),
                         pjvm->java_home,
                         pjvm->java_home,
                         JVMCFG_PATHNAME_DELIMITER_CHAR,
                         classpath_list[i],
                         inner_class_location);

#if defined(CONFIG_WINDOWS) || defined(CONFIG_CYGWIN)

            /*
             * @todo HARMONY-6-jvm-classpath.c-3
             *   gmj : awful hack - need to escape out every \ in paths
             *   or it doesn't seem to get across into the batch file
             *   correctly
             */
            rchar *fixscript = HEAP_GET_DATA(JVMCFG_SCRIPT_MAX, rfalse);

            char *buffptr = fixscript;
            char *jarscriptPtr = jarscript;

            char c;
            while((c = *jarscriptPtr++))
            {
                *buffptr++ = c;

#if defined(CONFIG_WINDOWS)
                if (c == JVMCFG_PATHNAME_DELIMITER_CHAR)
                {
                    *buffptr++ = JVMCFG_PATHNAME_DELIMITER_CHAR;
                }
#endif
#if defined(CONFIG_CYGWIN)
                /*
                 * CygWin can have both--
                 *    such as C:\\path\\name/more/path/name
                 */
                if (c == JVMCFG_PATHNAME_ALT_DELIMITER_CHAR)
                {
                    *buffptr++ = JVMCFG_PATHNAME_ALT_DELIMITER_CHAR;
                }
#endif
            }
            *buffptr = '\0';

            portable_strcpy(jarscript, fixscript);
            HEAP_FREE_DATA(fixscript);

#endif

            HEAP_FREE_DATA(inner_class_location);

            int rc = portable_system(jarscript);

            if (0 != rc)
            {
                sysErrMsg(arch_function_name,
                          "Cannot extract '%s' from JAR file %s",
                          inner_class_location,
                          classpath_list[i]);

                HEAP_FREE_DATA(class_location);
                HEAP_FREE_DATA(jarscript);

                exit_jvm(EXIT_CLASSPATH_JAR);
/*NOTREACHED*/
            }

            /*
             * @todo  HARMONY-6-jvm-classpath.c-4 Make sure that this
             *        sprintf/stat works with both CONFIG_WINDOWS and
             *        CONFIG_CYGWIN.
             *
             */

            /* Location of extracted file */
            sprintfLocal(jarscript,
                         "%s%c%s",
                         tmparea_get(),
                         JVMCFG_PATHNAME_DELIMITER_CHAR,
                         class_location);

            statbfr = portable_stat(jarscript);
            HEAP_FREE_DATA(statbfr);

            /*
             * If file was extracted, report result
             * in heap-allocated bfr
             */
            if (rnull != statbfr)
            {
                HEAP_FREE_DATA(class_location);

                return(jarscript);
            }

            HEAP_FREE_DATA(jarscript);
        }
        else
        {
            /*
             * @todo  HARMONY-6-jvm-classpath.c-4 Make sure that this
             *        sprintf/stat works with both CONFIG_WINDOWS and
             *        CONFIG_CYGWIN.
             *
             */

            /* Convert input parm to internal form */
            sprintfLocal(class_location,
                         "%s%c\0",
                         classpath_list[i],
                         JVMCFG_PATHNAME_DELIMITER_CHAR);

            clen = portable_strlen(class_location);

            portable_strcat(class_location, name);

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

                portable_strcat(class_location,
                                CLASSFILE_EXTENSION_DEFAULT);
            }

            /* Test for existence of valid class file */
            statbfr = portable_stat(class_location);
            HEAP_FREE_DATA(statbfr);

            /* If match found, report result in heap-allocated bfr */
            if (rnull != statbfr)
            {
                HEAP_FREE_DATA(jarscript);

                return(class_location);
            }
        }
    } /* for i */

    /* Class not found in @b CLASSPATH */
    HEAP_FREE_DATA(class_location);
    HEAP_FREE_DATA(jarscript);

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

rchar *classpath_get_from_cp_entry_utf(cp_info_mem_align *clsname)
{
    ARCH_FUNCTION_NAME(classpath_get_from_cp_entry_utf);

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
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid classpath_shutdown()
{
    ARCH_FUNCTION_NAME(classpath_shutdown);

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
