/*!
 * @file argv.c
 *
 * @brief Process command line arguments passed in to JVM program.
 *
 * Read @link #jvm() command line parameters@endlink from,
 * @code main(int argc, char **argv, char **envp) @endcode
 *
 * These parameters typically are passed from the
 * @link #main() actual 'C' program entry point@endlink.
 * Process these parameters and use a subset to construct the Java
 * command line parameters to pass in to the main Java entry point,
 * @code public static void main(String[] args) @endcode
 *
 * Part of this process is to read environment settings for
 * @b JAVA_HOME, @b CLASSPATH, and @b BOOTCLASSPATH, which may be
 * overridden from the command line.
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
ARCH_SOURCE_COPYRIGHT_APACHE(argv, c,
"$URL$",
"$Id$");


/* #include <stdlib.h> */
/* #include <string.h> */

#include "jvmcfg.h" 
#include "classfile.h" 
#include "classpath.h" 
#include "exit.h" 
#include "heap.h" 
#include "jvm.h" 
#include "util.h" 


/*!
 * @brief Parse command line
 *
 * Retrieve parameters from the program entry point
 * @code main(int argc, char **argv, char **envp) @endcode
 * and parse them into internal form.  Since this program
 * is delivered primarily as a library, these will have
 * to be reported as parameters to
 * @link jvm(int argc, char **argv, char **envp)@endlink
 * from the invoking program.
 *
 * The syntax for the command line is:
 *
 * @code
 * main_pgm_name [{-Xjh    | -Xjavahome | -Xjava_home}  dir_name]
 *               [{-cp     | -classpath}                dir_name]
 *               [{-Xbcp   | -Xbootclasspath}           dir_name]
 *               [{-Xdebug | -Xdebuglevel | -Xdebug_level} level]
 *
 *               [-jar any/path/name/jarfilename]
 *               start.class.name
 *
 *               [java_arg1 [java_arg2 [...]]]
 *
 *
 * main_pgm_name -show
 *               -version
 *               -copyright
 *               -license
 *               -help
 * @endcode
 *
 * If more than one '-\<token\>' or '-X\<token\>' option entry is
 * found on the command line for a given token type, the last one
 * wins If one of these tokens is the last parameter on a command
 * line, it is ignored.  After the option entries are reviewed, the
 * starting class name is specified, preceded by an optional '-jar'
 * modifier which, if present, indicates that the specified jar file
 * should be read to locate the starting class.  Any parameters after
 * the starting class name are passed to the JVM as '(String) args[]'.
 *
 *
 * @param argc    Number of arguments on command line
 *
 * @param argv    Argument vector from the command line
 *
 * @param envp    Environment pointer from command line environ
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid argv_init(int argc, char **argv, char **envp)
{
    ARCH_FUNCTION_NAME(argv_init);

    rint     argclocal = (rint)     argc;
    rchar ** argvlocal = (rchar **) argv;
    rchar ** envplocal = (rchar **) envp;

    rchar *chkjh;  chkjh =  (rchar *) rnull;
    rchar *chkcp;  chkcp =  (rchar *) rnull;
    rchar *chkbcp; chkbcp = (rchar *) rnull;

    pjvm->startjar   = (rchar *) rnull;
    pjvm->startclass = (rchar *) rnull;

    /* Save off startup parameters, incl. pgm name (argv0) */
    pjvm->argc = argclocal;
    pjvm->argv = argvlocal;
    pjvm->envp = envplocal;

    /* Extract program name from path, defaulting to whole invocation */
    pjvm->argv0 = argvlocal[0];
    pjvm->argv0name = portable_strrchr(pjvm->argv0,
                                       JVMCFG_PATHNAME_DELIMITER_CHAR);
#ifdef CONFIG_CYGWIN
    if (rnull == pjvm->argv0name)
    {
        pjvm->argv0name = portable_strrchr(pjvm->argv0,
                                    JVMCFG_PATHNAME_ALT_DELIMITER_CHAR);
    }
#endif

    if (rnull != pjvm->argv0name)
    {
        pjvm->argv0name++;
    }
    else
    {
        pjvm->argv0name = pjvm->argv0;
    }

    /*
     * Initially extract @b JAVA_HOME, @b CLASSPATH, and
     * @p @b BOOTCLASSPATH from environment (could do this with
     * @b envp, but @c @b getenv(3) is easier).
     */
    chkjh  = portable_getenv(JVMCFG_ENVIRONMENT_VARIABLE_JAVA_HOME);
    chkcp  = portable_getenv(JVMCFG_ENVIRONMENT_VARIABLE_CLASSPATH);
    chkbcp = portable_getenv(JVMCFG_ENVIRONMENT_VARIABLE_BOOTCLASSPATH);

    /*
     * Look in command line, scan WHOLE command line for
     * '-X<clspthtoken> classpath', last entry wins.
     * Ignore trailing token without a following parameter.
     */
    rint  i;
    rint show_flag = rfalse;

    for (i = 1; i < argclocal; i++)
    {
        /*
         * Options that terminate program execution
         */
        if (0 == portable_strcmp(JVMCFG_COMMAND_LINE_HELP_PARM,
                                 argvlocal[i]))
        {
            argv_helpmsg();
            exit_jvm(EXIT_ARGV_HELP);
/*NOTREACHED*/
        }
        else
        if (0 == portable_strcmp(JVMCFG_COMMAND_LINE_LICENSE_PARM,
                                 argvlocal[i]))
        {
            argv_licensemsg();
            exit_jvm(EXIT_ARGV_LICENSE);
/*NOTREACHED*/
        }
        else
        if (0 == portable_strcmp(JVMCFG_COMMAND_LINE_VERSION_PARM,
                                 argvlocal[i]))
        {
            argv_versionmsg();
            exit_jvm(EXIT_ARGV_VERSION);
/*NOTREACHED*/
        }
        else
        if (0 == portable_strcmp(JVMCFG_COMMAND_LINE_COPYRIGHT_PARM,
                                 argvlocal[i]))
        {
            argv_copyrightmsg();
            exit_jvm(EXIT_ARGV_COPYRIGHT);
/*NOTREACHED*/
        }
        else
        /*
         * Reporting options
         */
        if (0 == portable_strcmp(JVMCFG_COMMAND_LINE_SHOW_PARM,
                                 argvlocal[i]))
        {
            /* Display command line parms after parsing */
            show_flag = rtrue;
            continue;
        }
        else
        /*
         * Options that affect program execution,
         * not including JVM arguments (below).
         */
        if ((0 == portable_strcmp(JVMCFG_JAVA_HOME_ABBREV_PARM,
                                                       argvlocal[i])) ||
            (0 == portable_strcmp(JVMCFG_JAVA_HOME_MID_PARM,
                                                       argvlocal[i])) ||
            (0 == portable_strcmp(JVMCFG_JAVA_HOME_FULL_PARM,
                                                       argvlocal[i])))
        {
            if (argclocal - 1 > i)
            {
                chkjh = argvlocal[i + 1];
                i++;
            }
        }
        else
        if ((0 == portable_strcmp(JVMCFG_CLASSPATH_ABBREV_PARM,
                                  argvlocal[i])) ||
            (0 == portable_strcmp(JVMCFG_CLASSPATH_FULL_PARM,
                                  argvlocal[i])))
        {
            if (argclocal - 1 > i)
            {
                chkcp = argvlocal[i + 1];
                i++;
            }
        }
        else
        if ((0 == portable_strcmp(JVMCFG_BOOTCLASSPATH_ABBREV_PARM,
                                  argvlocal[i])) ||
            (0 == portable_strcmp(JVMCFG_BOOTCLASSPATH_FULL_PARM,
                                  argvlocal[i])))
        {
            if (argclocal - 1 > i)
            {
                chkbcp = argvlocal[i + 1];
                i++;
            }
        }
        else
        if ((0 == portable_strcmp(JVMCFG_DEBUGMSGLEVEL_ABBREV_PARM,
                                                       argvlocal[i])) ||
            (0 == portable_strcmp(JVMCFG_DEBUGMSGLEVEL_MID_PARM,
                                                       argvlocal[i])) ||
            (0 == portable_strcmp(JVMCFG_DEBUGMSGLEVEL_FULL_PARM,
                                                       argvlocal[i])))
        {
            if (argclocal - 1 > i)
            {
                rint chkdml = portable_atol(argvlocal[i + 1]);

                if ((DMLOFF == chkdml) ||
                    ((DMLMIN <= chkdml) && (DMLMAX >= chkdml)))
                {
                    jvmutil_set_dml(chkdml);

                 /* sysDbgMsg(DMLMIN, */
                    sysErrMsg(arch_function_name,
                              "debug message level %d",
                              chkdml);
                }
                else
                {
                    sysErrMsg(arch_function_name,
                           "invalid debug message level %d.  Ignored",
                              chkdml);
                }

                i++;
            }
        }
        else
        /*
         * Java class to run (terminates option scanning),
         * either JAR or class file.
         */
        if (0 == portable_strcmp(JVMCFG_JARFILE_STARTCLASS_PARM,
                                 argvlocal[i]))
        {
            if (argclocal - 1 > i)
            {
                pjvm->startjar = argvlocal[i + 1];
                i++;
                i++;
            }

            break; /* End of token parsing.  Quit for() loop */
        }
        else
        {
            /* If not a -xxxxx token, it must be the startup class */
            pjvm->startclass = argvlocal[i];
            i++;

            break; /* End of token parsing.  Quit for() loop */
        }

    } /* for i */

    /*
     * Now get JVM Java @c @b args[] list from rest
     * of @b argv parms
     */

    /* Add 1 NUL byte at the end, initialize to zeroes (NUL) */
    pjvm->argcj = 0;
    pjvm->argvj = HEAP_GET_DATA(sizeof(rchar *) * ( 1 + argclocal - i),
                                rtrue);

    /*
     * If any @b argv parms left, load them into
     * @c @b pjvm->argvj
     */
    if (i < argclocal)
    {
        /*
         * Keep scanning @c @b argv[] , only initialization
         * for @c @b pjvm->argvj
         */

        /* @warning  NON-STANDARD TERMINATION CONDITION <= VERSUS < */
        rint j;
        for (j = 0; i <= argclocal; i++, j++)
        {
            /* Done when all @b argv is scanned and NUL byte added */
            if (i == argclocal)
            {
                pjvm->argvj[j] = (rchar *) rnull;
                break;
            }

            pjvm->argcj++;
            pjvm->argvj[j] = argvlocal[i];
        }
    }

    /* Try to get a valid @b JAVA_HOME even if not found elsewhere */
    chkjh = (rchar *) ((rnull == chkjh)
                       ? JVMCFG_JAVA_HOME_DEFAULT
                       : chkjh);

    pjvm->java_home = (rchar *) rnull;
    if (rnull != chkjh)
    {
        /* Copy '\0' byte also*/
        pjvm->java_home =
          HEAP_GET_DATA(sizeof(rchar) + portable_strlen(chkjh), rfalse);

        portable_memcpy(pjvm->java_home,
                        chkjh,
                        sizeof(rchar) + portable_strlen(chkjh));
    }

    /* Try to get a valid @b CLASSPATH even if not found elsewhere */
    chkcp = (rchar *) ((rnull == chkcp)
                         ? JVMCFG_CLASSPATH_DEFAULT
                         : (const rchar *) chkcp);

    pjvm->classpath = (rchar *) rnull;
    if (rnull != chkcp)
    {
        /* Copy '\0' byte also */
        pjvm->classpath =
            HEAP_GET_DATA(sizeof(rchar) + portable_strlen(chkcp),
                          rfalse);

        portable_memcpy(pjvm->classpath,
                        chkcp, 
                        sizeof(rchar) + portable_strlen(chkcp));
    }
#ifdef JVMCFG_HARDCODED_TEST_CLASSPATH
    pjvm->classpath = JVMCFG_HARDCODED_TEST_CLASSPATH;
#endif

    /* Try to get a valid @b BOOTCLASSPATH even if not found elsewhere*/
    chkbcp = (rchar *) ((rnull == chkbcp)
                          ?
#ifdef CONFIG_HACKED_BOOTCLASSPATH
                            JVMCFG_CLASSPATH_DEFAULT
#else
                            JVMCFG_BOOTCLASSPATH_DEFAULT
#endif
                          : (const rchar *) chkbcp);

    pjvm->bootclasspath = (rchar *) rnull;
    if (rnull != chkbcp)
    {
        /* Copy '\0' byte also */
        pjvm->bootclasspath =
          HEAP_GET_DATA(sizeof(rchar) + portable_strlen(chkbcp),rfalse);

        portable_memcpy(pjvm->bootclasspath,
                        chkbcp,
                        sizeof(rchar) + portable_strlen(chkbcp));
    }

    /* Show summary of what command line resoved into */
    if (rtrue == show_flag)
    {
        argv_showmsg();
    }

    /* Display copyright msg and finish */
    argv_copyrightmsg();

    /* Declare this module initialized */
    jvm_argv_initialized = rtrue;

    return;

} /* END of argv_init() */


/*!
 * @brief Show program version message to standard output.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid argv_versionmsg(rvoid)
{
    ARCH_FUNCTION_NAME(argv_versionmsg);

    fprintfLocalStdout("%s\n", CONFIG_RELEASE_LEVEL);

    return;

} /* END of argv_versionmsg() */


/*!
 * @brief Show program copyright message to standard output.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid argv_copyrightmsg(rvoid)
{
    ARCH_FUNCTION_NAME(argv_copyrightmsg);

    fprintfLocalStdout("\n%s:  %s, version %s\n%s\n\n",
                       CONFIG_PROGRAM_NAME,
                       CONFIG_PROGRAM_DESCRIPTION,
                       CONFIG_RELEASE_LEVEL,
                       ARCH_COPYRIGHT_TEXT_APACHE);

    return;

} /* END of argv_copyrightmsg() */


/*!
 * @brief Show program software license message to standard output.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid argv_licensemsg(rvoid)
{
    ARCH_FUNCTION_NAME(argv_licensemsg);

    argv_copyrightmsg();
    fprintfLocalStdout("%s\n\n", ARCH_LICENSE_TEXT_APACHE);

    return;

} /* END of argv_licensemsg() */


#define LEADING_SPACES "                  "

/*!
 * @brief Show program syntax and usage message to standard output.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid argv_helpmsg(rvoid)
{
    ARCH_FUNCTION_NAME(argv_helpmsg);

    argv_copyrightmsg();

    fprintfLocalStdout(
"%s:  version %s\n\n",
                       pjvm->argv0name,
                       CONFIG_RELEASE_LEVEL);
    fprintfLocalStdout(
"Invoke a class: %s: [options] start.class.name [args]\n",
                       pjvm->argv0name);
    fprintfLocalStdout(
"Invoke a jar:   %s: [options] -jar filename    [args]\n\n",
                       pjvm->argv0name);

    fprintfLocalStdout(
"Where:\n");

    fprintfLocalStdout(
"    %s filename %s",
                       JVMCFG_JARFILE_STARTCLASS_PARM,
            "Invoke class from JAR file whose main() appears in the\n");
    fprintfLocalStdout(
"%senclosed '%s' file on the '%s'\n",
                       LEADING_SPACES,
                       JVMCFG_JARFILE_MANIFEST_FILENAME,
                       JVMCFG_JARFILE_MANIFEST_MAIN_CLASS);
    fprintfLocalStdout(
"%sattribute line.  This name may not cause the line to be\n",
                       LEADING_SPACES);
    fprintfLocalStdout(
"%smore than %d characters wide.  Line continuations are\n",
                       LEADING_SPACES,
                       JVMCFG_JARFILE_MANIFEST_LINE_MAX);
    fprintfLocalStdout(
"%snot supported for locating this class name.\n\n",
                       LEADING_SPACES);

    fprintfLocalStdout("    %s         %s",
                       JVMCFG_COMMAND_LINE_SHOW_PARM,
   "Show how command line and environment resolves after parsing.\n");
    fprintfLocalStdout("    %s      %s",
                       JVMCFG_COMMAND_LINE_VERSION_PARM,
   "Display the program release level.\n");

    fprintfLocalStdout("    %s    %s",
                       JVMCFG_COMMAND_LINE_COPYRIGHT_PARM,
   "Display the program copyright.\n");

    fprintfLocalStdout("    %s      %s",
                       JVMCFG_COMMAND_LINE_LICENSE_PARM,
   "Display the program software license.\n");

    fprintfLocalStdout("    %s         %s",
                       JVMCFG_COMMAND_LINE_HELP_PARM,
   "Display this help message.\n\n");


    fprintfLocalStdout("    %s pathname\n", JVMCFG_JAVA_HOME_FULL_PARM);
    fprintfLocalStdout("    %s  pathname\n", JVMCFG_JAVA_HOME_MID_PARM);
    fprintfLocalStdout(
"    %s        pathname\n",
                       JVMCFG_JAVA_HOME_ABBREV_PARM);
    fprintfLocalStdout(
"%sOverride JAVA_HOME environment variable, if any.\n",
                       LEADING_SPACES);
    fprintfLocalStdout(
"%sIf none found, this option @e must be present.\n",
                       LEADING_SPACES);
    fprintfLocalStdout(
"%sIt points to the Java home directory for this invocation.\n\n",
                       LEADING_SPACES);

    fprintfLocalStdout(
"    %s path1%cpath2%c...pathN\n",
                       JVMCFG_CLASSPATH_FULL_PARM,
                       CLASSPATH_ITEM_DELIMITER_CHAR,
                       CLASSPATH_ITEM_DELIMITER_CHAR);
    fprintfLocalStdout(
"    %s        path1%cpath2%c...pathN\n",
                       JVMCFG_CLASSPATH_ABBREV_PARM,
                       CLASSPATH_ITEM_DELIMITER_CHAR,
                       CLASSPATH_ITEM_DELIMITER_CHAR);
    fprintfLocalStdout(
"%sOverride CLASSPATH environment variable, if any.\n",
                       LEADING_SPACES);
    fprintfLocalStdout(
"%sIf none found, this option @e must be present.\n",
                       LEADING_SPACES);
    fprintfLocalStdout(
"%sContains a search paths of directories and/or jar files.\n",
                       LEADING_SPACES);
    fprintfLocalStdout(
"%sto use for locating classfiles.  Each entry is\n",
                       LEADING_SPACES);
    fprintfLocalStdout(
"%san /absolute/path/name and is separated by a '%c' character.\n\n",
                       LEADING_SPACES,
                       CLASSPATH_ITEM_DELIMITER_CHAR);

    fprintfLocalStdout(
"    %s path1%cpath2%c...pathN\n",
                       JVMCFG_BOOTCLASSPATH_FULL_PARM,
                       CLASSPATH_ITEM_DELIMITER_CHAR,
                       CLASSPATH_ITEM_DELIMITER_CHAR);
    fprintfLocalStdout(
"    %s           path1%cpath2%c...pathN\n",
                       JVMCFG_BOOTCLASSPATH_ABBREV_PARM,
                       CLASSPATH_ITEM_DELIMITER_CHAR,
                       CLASSPATH_ITEM_DELIMITER_CHAR);
    fprintfLocalStdout(
"%sOverride BOOTCLASSPATH environment variable, if any.\n",
                       LEADING_SPACES);
    fprintfLocalStdout(
"%sPerforms similar function to CLASSPATH, but used for\n",
                       LEADING_SPACES);
    fprintfLocalStdout("%ssystem startup purposes.\n\n",
                       LEADING_SPACES);
    fprintfLocalStdout(
"    args          Optional arguments to pass to JVM, if any.\n\n");


    return;

} /* END of argv_helpmsg() */


/*!
 * @brief Show resolution of command line to standard output.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid argv_showmsg(rvoid)
{
    ARCH_FUNCTION_NAME(argv_showmsg);

    fprintfLocalStdout("JAVA_HOME=%s\n", pjvm->java_home);

    fprintfLocalStdout("CLASSPATH=%s\n", pjvm->classpath);

    fprintfLocalStdout("BOOTCLASSPATH=%s\n", pjvm->bootclasspath);

    if (rnull != pjvm->startjar)
    {
        fprintfLocalStdout("entry=%s\n", pjvm->startjar);
    }
    else
    if (rnull != pjvm->startclass)
    {
        fprintfLocalStdout("entry=%s\n", pjvm->startclass);
    }
    else
    {
        fprintfLocalStdout("entry=UNKNOWN\n");
    }

    rint argc;
    for (argc = 0; rnull != pjvm->argvj[argc]; argc++)
    {
        ; /* Simply count number of args, terminated by rnull slot */
    }

    fprintfLocalStdout("argc=%d\n", argc);

    fprintfLocalStdout("args=");

    rint i;
    for (i = 0; i < argc; i++)
    {
        fprintfLocalStdout("%s ", pjvm->argvj[i]);

    }
    
    fprintfLocalStdout("\n\n");


    return;

} /* END of argv_showmsg() */


/*!
 * @brief Clean up from argv[] setup after JVM execution.
 *
 *
 * @b Parameters: @link #rvoid rvoid@endlink
 *
 *
 * @returns @link #rvoid rvoid@endlink
 *
 */
rvoid argv_shutdown(rvoid)
{
    ARCH_FUNCTION_NAME(argv_shutdown);

    HEAP_FREE_DATA(pjvm->bootclasspath);
    pjvm->bootclasspath = (rchar *) rnull;

    HEAP_FREE_DATA(pjvm->classpath);
    pjvm->classpath = (rchar *) rnull;

    HEAP_FREE_DATA(pjvm->java_home);
    pjvm->java_home = (rchar *) rnull;

    rint argidx;
    for (argidx = 0; argidx < pjvm->argcj; argidx++)
    {
        HEAP_FREE_DATA(pjvm->argvj[argidx]);
    }
    HEAP_FREE_DATA(pjvm->argvj);
    pjvm->argvj = (rchar **) rnull;

    /* Declare this module uninitialized */
    jvm_argv_initialized = rfalse;

    return;

} /* END of argv_shutdown() */


/* EOF */
