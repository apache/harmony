#!/bin/sh
#
#!
# @file ./config.sh
#
# @brief Configure Boot JVM.
#
# Shell script to configure which features are in the compiled code.
# This build happens in several phases:
#
# <ul>
# <li>
# <b>(1)</b> Pre-formatted documentation
# </li>
# <li>
# <b>(2)</b> Java Setup
# </li>
# <li>
# <b>(3)</b> C Compilation and Document Compilation Setup
# </li>
# <li>
# <b>(4)</b> Startup Library (bootclasspath) Java classes
# </li>
# <li>
# <b>(5)</b> Source code build-- Invokes 'make' which may be
#                         used for all further compilations.
# </li>
# </ul>
#
# Remember that if you add a Java source file, a C source or header
# file, or a shell script, you should run this script again so that
# the @b config directory roster locates the update.  This way,
# 'doxygen' will automatically incorporate it into the documentation
# suite.
#
# @todo HARMONY-6-config.sh-1 Support the following input parameter
#       format:
#
# Input:   [cputype [wordwidth [osname]]]
#
# These optional positional parameters contain specific token values,
# which may change from one release to the next.  Initially, they are
# as follows.  For current actual supported platform values, please
# refer to parsing of positional parms $1 and $2 and $3 below.
#
# <ul>
#         <li>cputype      Keyword for supported hardware platform.
#                          Initially @b sparc and @b intel
#
#         </li>
#         <li>wordwidth    Keyword for hardware word size.
#                          Initialiy either @b 32 and @b 64
#         </li>
#         <li>osname       Keyword for operating system name.
#                          Initially @b solaris and @b linux and
#                          @b windows and @b cygwin.
#         </li>
# </ul>
#
# These may evolve over time, and not every combination of these
# is valid.  The initial valid combinations are:
#
# (cputype, wordwidth, osname) ::=   one of these combinations:
#
# <ul>
#     <li>(sparc, 32, solaris)
#     </li>
#     <li>(sparc, 64, solaris)
#     </li>
#     <li>(sparc, 32, linux)
#     </li>
#     <li>(sparc, 64, linux)
#     </li>
#     <li>(intel, 32, solaris)
#     </li>
#     <li>(intel, 32, linux)
#     </li>
#     <li>(intel, 64, solaris)
#     </li>
#     <li>(intel, 64, linux)
#     </li>
#     <li>(intel, 32, windows)
#     </li>
#     <li>(intel, 32, cygwin)
#     </li>
#     <li>(amd,   64, windows)... JDK for AMD64 coming soon from Sun.
#     </li>
# </ul>
#
#
# @todo  HARMONY-6-config.sh-2 A Windows .BAT version of this
#        script needs to be written
#
#
# @section Control
#
# \$URL$
#
# \$Id$
#
# Copyright 2005 The Apache Software Foundation
# or its licensors, as applicable.
#
# Licensed under the Apache License, Version 2.0 ("the License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
# either express or implied.
#
# See the License for the specific language governing permissions
# and limitations under the License.
#
# @version \$LastChangedRevision$
#
# @date \$LastChangedDate$
#
# @author \$LastChangedBy$
#
#         Original code contributed by Daniel Lydick on 09/28/2005.
#
# @section Reference
#
#/ /* 
# (Use  #! and #/ with dox_filter.sh to fool Doxygen into
# parsing this non-source text file for the documentation set.
# Use the above open comment to force termination of parsing
# since it is not a Doxygen-style 'C' comment.)
#
#
###################################################################
#
# Script setup
#
chmod -w $0 ./echotest.sh

. ./echotest.sh

# Read release level, if found
if test -f RELEASE_LEVEL
then
    RELEASE_LEVEL=`cat RELEASE_LEVEL`
else
    RELEASE_LEVEL=""
fi

# `dirname $0` for shells without that utility
PGMDIR=`expr "${0:-.}/" : '\(/\)/*[^/]*//*$'  \| \
             "${0:-.}/" : '\(.*[^/]\)//*[^/][^/]*//*$' \| .`
PGMDIR=`cd $PGMDIR; pwd`

# `basename $0` for shells without that utility
PGMNAME=`expr "/${0:-.}" : '\(.*[^/]\)/*$' : '.*/\(..*\)' \|\
              "/${0:-.}" : '\(.*[^/]\)/*$' : '.*/\(..*\)' \| \
              "/${0:-.}" : '.*/\(..*\)'`

###################################################################
#
# Constant values
#
PROGRAM_NAME="BootJVM"
PROGRAM_DESCRIPTION="Apache Harmony Bootstrap JVM"

###################################################################
#
# Introduction
#
tput clear
echo ""
echo "Welcome to the Apache Harmony Bootstrap JVM configurator!"
echo "========================================================="
echo ""
echo "You may run this utility as often as needed to configure or"
echo "reconfigure various parts of your installation of this project."
echo "There are several areas that will be configured with this tool:"
echo ""
echo "  (1) Pre-formatted documentation"
echo "  (2) Java compilation setup"
echo "  (3) C compilation and document compilation setup"
echo "  (4) Startup library (bootclasspath) Java classes"
echo "  (5) Project build"
echo ""
echo "Phases (2) and (3) will be configured every time, where"
echo "phases (1) and (4) need only be configured once, unless"
echo "there is a need to change something.  Phase (5) may be"
echo "run each time or not, as desired.  The most important"
echo "information from phases (2) and (3) is stored into the"
CFGH="config/config.h"
echo "configuration file '$CFGH' and are the ones"
echo "most likely to need adjustment from time to time."
echo ""
$echon "Do you wish to read the introductory notes? [y,n] $echoc"
read readintro
echo ""

case $readintro in
    y|ye|yes|Y|YE|YES) READINTRO=1;;
    *)                 READINTRO=0;;
esac

if test 1 -eq $READINTRO
then
# Do not indent so that more text may be put on each line:

tput clear
echo ""
echo "Introductory Configuration Notes"
echo "--------------------------------"
echo ""
echo "The documentation for this project is created from information"
echo "stored literally within each source file.  Documentation tags are"
echo "used to frame each portion, which is extracted and formatted"
echo "with the C/C++ documentation tool called Doxygen."
echo ""
echo "Over the course of the development cycle, you may add or delete"
echo "source files:  Java source, C source, C headers, perhaps shell"
echo "scripts.  When this is done, the 'make' process will autmatically"
echo "pick up the changes after this script is run.  The changes"
echo "will be made available to the 'doxygen' and 'gcc' as"
echo "coordinated by the 'config/config_roster*' files, and"
echo "without assistance required from users."
echo ""
echo "The 'README' file in this directory contains much useful"
echo "information about which source files perform which function."
echo ""
$echon "more... $echoc"
read dummy
echo ""
echo "This project was originally developed on a Solaris 9 platform."
echo "All C code was originally compiled there with the GCC C compiler."
echo "Your GCC compiler is located in:"
echo ""
echo "\$ which gcc"
which gcc
echo ""
echo "Please use version 3.3.2 or newer."
echo ""
$echon "ready... $echoc"
read dummy
echo ""
echo "The project is built with GNU 'make'.  It is found in:"
echo ""
echo "\$ which make"
which make
echo ""
echo "Please use version 3.80 or newer.  If one or both of these"
echo "programs is not installed, the source may be located at"
echo "www.gnu.org.  There are also compiled binary editions of"
echo "the GNU programs 'gcc' and 'make'  available from numerous"
echo "hardware vendors and/or their user groups."
echo ""
$echon "ready... $echoc"
read dummy
echo ""
echo "The 'C' source code for the project may have its dependencies"
echo "checked at compilation time by the common utility 'makedepend."
echo ""
echo "\$ which makedepend"
which makedepend
echo ""
echo "This is commonly available on many platforms and is available"
echo "in several versions, all of which accomplish the same task."
echo "Its absence will not interfere with correct compilation, but"
echo "a message may appear warning that is is missing."
echo ""
$echon "ready... $echoc"
read dummy
echo ""
echo "The 'make dox' facility extracts the documentation from the"
echo "source code using 'doxygen'.  On your system, it is located in:"
echo ""
echo "\$ which doxygen"
which doxygen
echo ""
echo "If not found, binary distributions for numerous platforms are"
echo "located at: www.doxygen.org.  Please use version 1.4.4 or newer."
echo ""
echo ""
echo "... End of introductory notes ..."
echo ""
$echon "ready... $echoc"
read dummy
fi

###################################################################
#
# Set up phase 1:  Pre-formatted documentation
#
tput clear
echo ""
echo "Phase 1:  Pre-formatted documentation"
echo "-------------------------------------"
echo ""
echo ""
echo "The project documentation comes pre-formatted by 'doxygen'."
echo "It provides output in HTML format for a web browser, in Unix"
echo "man and Latex info formats, as well as RTF, and XML formats"
echo "for other access methods."
echo ""
echo "This documentation may also be derived from the source code at"
echo "any time by invoking the top-level project build option"
echo "'make dox' or 'make all'."
echo ""
echo "Developers working only on documentation may wish to _not_"
echo "install it, while most others may wish to do so for a starting"
echo "point for their work.  (Remember that documentation of source"
echo "files, functions, data types, and data structures is all part"
echo "of the development process.  Running 'make dox' will refresh"
echo "the documentation suite with your current work.)"
echo ""
$echon "more... $echoc"
read dummy
echo ""
echo "Once installed in the 'doc' directory, this information will"
echo "promptly be moved to a 'doc.ORIG' directory as a read-only"
echo "reference that is not changed during 'make' operations."
echo "All documentation builds write their output to the 'doc'"
echo "directory, leaving 'doc.ORIG' untouched.  If you will be"
echo "regularly working with documentation changes, or if you do"
echo "not need a reference copy of the documentation, you may"
echo "safely remove this version."
echo ""
$echon "more... $echoc"
read dummy
echo ""
echo "It is recommended that a typical user install the documentation"
echo "suite in manner described here and used it in this way until"
echo "accustomed to the use of the various tools."
echo ""
echo "If it has already been installed on a previous instance of"
echo "running '$PGMNAME', then it does not need to be installed again."
echo ""
$echon "Do you wish to install pre-formatted documentation? [y,n] $echoc"
read prefmtdocs

###################################################################
#
# Set up and run phase 2:  Java compilation setup
#
tput clear
echo ""
echo "Phase 2:  Java compilation setup"
echo "--------------------------------"
echo ""
echo "This project uses your current Java J2SE compilation tools."
echo "Any JDK will do, as long as its compiler is called 'javac'"
echo "and it has an archiver utility called 'jar'.  It also needs"
echo "to have a class library of some description from which this"
echo "Java runtime will initially draw its library classes such as"
echo "java.lang.Object and other fundamental components.  These will,"
echo "over time, be replaced with classes native to the project."
echo ""
echo "$PGMNAME:  Testing JAVA_HOME..."

if test "" = "$JAVA_HOME"
then
    echo "$PGMNAME:  STOP!  Need to have JAVA_HOME defined first"
    exit 1
fi
echo "$PGMNAME:  JAVA_HOME okay: $JAVA_HOME"

echo "$PGMNAME:  Testing $JAVA_HOME..."

if test ! -d $JAVA_HOME/.
then
    echo "$PGMNAME:  JAVA_HOME directory does not exist"
fi

CRITICAL_PROGRAMS="jar javac" # javah java ... add as needed

echo "$PGMNAME:  Testing $JAVA_HOME/bin..."

rc=0
for binary in $CRITICAL_PROGRAMS
do
    if test ! -x $JAVA_HOME/bin/$binary
    then
     echo "$PGMNAME:  Critical program $JAVA_HOME/bin/$binary not found"
        rc=1
    fi
done
if test 0 -ne $rc
then
    exit 2
fi

echo "$PGMNAME:  Testing $JAVA_HOME/include..."

if test ! -d $JAVA_HOME/include
then
    echo "$PGMNAME:  Missing JDK include directory $JAVA_HOME/include"
    exit 3
fi

echo "$PGMNAME:  Testing $PGMDIR..."

if test ! -w .
then
 echo "$PGMNAME:  Cannot write to $PGMDIR.  Please change to read-write"
    exit 4
fi

echo "$PGMNAME:  Testing Java version..."

$JAVA_HOME/bin/java -version

echo  ""
$echon "Is this version of Java and JAVA_HOME satisfactory? [y,n] $echoc"
read libsetup
echo ""

case $libsetup in
    y|ye|yes|Y|YE|YES) ;;
    *)     echo ""
           echo "Remedy:  Change JAVA_HOME to desired JDK and try again"
           echo ""
           exit 5;;
esac

for dir in jvm libjvm main test jni jni/src/harmony/generic/0.0
do
    echo "$PGMNAME:  Testing Source Area $dir..."

    if test ! -d $dir/src
    then
        echo "$PGMNAME:  Missing '$PGMDIR/$dir/src' directory"
        exit 6
    fi
done

for dir in jvm jni/src/harmony/generic/0.0
do
    echo "$PGMNAME:  Testing Include Area $dir..."

    if test ! -d $dir/include
    then
        echo "$PGMNAME:  Missing '$PGMDIR/$dir/include' directory"
        exit 7
    fi
done

echo "$PGMNAME:  Testing Object Area..."
echo "$PGMNAME:  Setting up output area..."

echo ""
echo "$PGMNAME:  Enter name of JRE class library archive file,"
echo "              relative to JAVA_HOME ($JAVA_HOME)."
echo "              Use this format:  relative/pathname/filename.jar"

while true
do
    echo ""
    echo "           If the default is acceptable, enter an empty line:"
    echo ""
    $echon "JRE class library archive: [jre/lib/rt.jar] $echoc"
    read rtjarfile

    if test -z "$rtjarfile"
    then
        rtjarfile="jre/lib/rt.jar"
    fi

   if test ! -f $JAVA_HOME/$rtjarfile
   then
       echo ""
       echo "File not found: $JAVA_HOME/$rtjarfile"
       echo ""
       continue
   fi

   if test ! -r $JAVA_HOME/$rtjarfile
   then
       echo ""
       echo "Permission denied: $JAVA_HOME/$rtjarfile"
       echo ""
       continue
   fi

   (
       mkdir ${TMPDIR:-/tmp}/tmp.config.$$
       cd ${TMPDIR:-/tmp}/tmp.config.$$
       $JAVA_HOME/bin/jar xf $JAVA_HOME/$rtjarfile \
                             java/lang/Object.class
   )
   if test ! -r ${TMPDIR:-/tmp}/tmp.config.$$/java/lang/Object.class
   then
       rm -rf ${TMPDIR:-/tmp}/tmp.config.$$
       echo ""
       echo "Archive is missing java.lang.Object: $JAVA_HOME/$rtjarfile"
       echo ""
       continue
   fi
   rm -rf ${TMPDIR:-/tmp}/tmp.config.$$

   # JRE file correct
   break
done
RTJARFILE=$rtjarfile
CONFIG_RTJARFILE=$JAVA_HOME/$rtjarfile

###################################################################
#
# Set up phase 3:  C Compilation and Document Compilation Setup
#
tput clear
echo ""
echo "Phase 3:  C Compilation and Document Compilation Setup"
echo "------------------------------------------------------"
echo ""
echo "The following is a set of questions that tells the compiler"
echo "the release level of the project and which compile options"
echo "to use and tells the source code about certain features of"
echo "the CPU.  Further questions inform the compiler and linker"
echo "about which options to use for selected modular features"
echo "such as heap allocation and garbage collection."
echo ""
$echon "more... $echoc"
read dummy

echo ""
echo "The release level is configured as:  MAJOR.MINOR.PATCHLEVEL"
echo "where each part of the tuple is"
echo "a numeric value of up to four digits."
echo "It is important when a release or"
echo "distribution is being made, at which"
echo "time the tuple assigned by the project"
echo "management committee must be used."
echo "It is recommended to use the pre-defined"
echo "value unless you are currently performing"
echo "this activity."
echo ""
echo "The default release level is defined in"
echo "the './RELEASE_LEVEL' file."
echo ""

if test -z "$RELEASE_LEVEL"
then
    echo "  This file does not exist at this time."
else
    echo \
"The current 'MAJOR.MINOR.PATCHLEVEL' value is:   '$RELEASE_LEVEL'"
    echo ""
    $echon "Do you wish to use this value?  [y,n] $echoc"
    read usedfltrellvl

    case $usedfltrellvl in
        y|ye|yes|Y|YE|YES) ;;
        *)  while true
            do

                echo ""
                $echon \
                 "  Enter the numeric MAJOR revision level:      $echoc"
                read RELEASE_MAJOR

                case $RELEASE_MAJOR in
                    [0-9] | [0-9][0-9] | [0-9][0-9][0-9] | \
                    [0-9][0-9][0-9][0-9] ) ;;

                    *)     echo ""
                           echo \
                        "Please use a numeric value for the MAJOR field"
                           echo ""
                           continue;;
                esac

                # Strip leading zeroes
                RELEASE_MAJOR=`expr 0 + $RELEASE_MAJOR`

                echo ""
                $echon \
                 "  Enter the numeric MINOR revision level:      $echoc"
                read RELEASE_MINOR

                case $RELEASE_MINOR in
                    [0-9] | [0-9][0-9] | [0-9][0-9][0-9] | \
                    [0-9][0-9][0-9][0-9] ) ;;

                    *)     echo ""
                           echo \
                        "Please use a numeric value for the MINOR field"
                           echo ""
                           continue;;
                esac

                # Strip leading zeroes
                RELEASE_MINOR=`expr 0 + $RELEASE_MINOR`

                echo ""
                $echon \
                 "  Enter the numeric PATCHLEVEL revision level: $echoc"
                read RELEASE_PATCHLEVEL

                case $RELEASE_PATCHLEVEL in
                    [0-9] | [0-9][0-9] | [0-9][0-9][0-9] | \
                    [0-9][0-9][0-9][0-9] ) ;;

                    *)     echo ""
                           echo \
                   "Please use a numeric value for the PATCHLEVEL field"
                           echo ""
                           continue;;
                esac

                # Strip leading zeroes
                RELEASE_PATCHLEVEL=`expr 0 + $RELEASE_PATCHLEVEL`

                # Valid tuple of three numbers entered
       RELEASE_LEVEL="$RELEASE_MAJOR.$RELEASE_MINOR.$RELEASE_PATCHLEVEL"
                break
            done
            ;;
    esac
fi

echo "---"
echo "Valid release level: $RELEASE_LEVEL"


while true
do
    echo ""
    echo "  Enter hardware configuration of this platform:"
    echo ""
    HWLIST="Sun Sparc series, Intel x86 series[, add others here]"
    echo "  Hardware vendor name: $HWLIST"
    echo ""
    $echon "  Select [sparc,intel] $echoc"
    read cputype

    case $cputype in
        sparc) CPUTYPE=SPARC;;
        intel) CPUTYPE=INTEL;;
        *)     echo ""
               echo "Hardware vendor '$cputype' invalid"
               echo ""
               continue;;
    esac

    echo ""
    echo "  Architecture word width model:  32-bit word, 64-bit word"
    echo ""
    $echon "  Select [32,64] $echoc"
    read wordwidth

    case $wordwidth in
        32 | 64 ) WORDWIDTH=$wordwidth;;
        *)        echo ""
                  echo "Word width '$wordwidth' invalid"
                  echo ""
                  continue;;
    esac

    echo ""
    echo "  Operating system:  Solaris, Linux, Windows, CygWin"
    echo ""
    $echon "  Select [solaris,linux,windows,cygwin] $echoc"
    read osname

    case $osname in
        solaris) OSNAME=SOLARIS;;
        linux)   OSNAME=LINUX;;
        windows) OSNAME=WINDOWS;;
        cygwin)  OSNAME=CYGWIN;;
        *)       echo ""
                 echo "Operating system '$osname' invalid"
                 echo ""
                 continue;;
    esac

    # Check invalid combinations
    echo ""
    case $cputype in
        sparc)  if test "$osname" = "windows" -o "$osname" = "cygwin"
                then
                    echo "Combination invalid:  $cputype/$osname"
                    echo ""
                    continue
                fi;;

        intel)  if test "$wordwidth" = "64"
                then
                    echo "Combination unsupported: $cputype/$wordwidth"
                    echo ""
                    continue
                fi;;
    esac

    break # All other combinations valid

done

echo "---"
echo \
"Valid architecture:  CPU=$cputype  Word=$wordwidth bits OS=$osname"


# Verify platform-specific JDK header file directory
    case $osname in
        solaris) OSNAME=SOLARIS;;
        linux)   OSNAME=LINUX;;
        windows) OSNAME=WINDOWS;;
        cygwin)  OSNAME=CYGWIN;;
        *)       echo ""
                 echo "Operating system '$osname' invalid"
                 echo ""
                 continue;;
    esac


echo ""
echo ""
echo "  Operating system-specific JDK header file sub-directory."
echo ""
echo "  This will be found under $JAVA_HOME/include"
echo ""
JDKlist=`list=""; \
         (cd $JAVA_HOME/include; \
         for dir in *; do if test -d $dir; then echo $dir; fi; done) | \
         (while read dir; do list="$list $dir"; done; echo $list)`
while true
do
    echo ""
    $echon "  Select [$JDKlist] $echoc"
    read osJDKdir

    if test -n "$osJDKdir" -a -d $JAVA_HOME/include/$osJDKdir
    then
        break
    fi

    echo "Directory not found: $JAVA_HOME/include/$osJDKdir"
    echo ""
    continue
done

echo "---"
echo \
"Valid operating system-specific JDK header file directory:  $osJDKdir"

echo ""
echo "$PGMNAME:  Choose a compiler"
echo ""
echo "            gcc--   Uses GNU 'C' compiler"
echo "            other-- Use anything not on this list, default 'gcc'"

while true
do
    echo ""
    $echon "C compiler: [gcc,other] $echoc"
    read ccompiler

    case $ccompiler in
        gcc)     CCOMPILER="GCC";;
        other)   CCOMPILER="OTHER";;

        *)       echo ""
                 echo "C compiler '$ccompiler' invalid"
                 echo ""
                 continue;;
    esac
    break
done


echo ""
echo "$PGMNAME:  Choose a heap allocation method:"
echo ""
echo "            simple-- Uses malloc(3) and free(3) only"
echo "            bimodal-- Augments 'simple' with large storage area"
echo "            other-- Roll your own, generates unresolved externals"

while true
do
    echo ""
    $echon "Heap allocation method: [simple,bimodal,other] $echoc"
    read heapalloc

    case $heapalloc in
        simple)  HEAPALLOC="HEAP_TYPE_SIMPLE";;
        bimodal) HEAPALLOC="HEAP_TYPE_SIMPLE";;
        other)   HEAPALLOC="HEAP_TYPE_OTHER";;

        *)       echo ""
                 echo "Heap allocation method '$heapalloc' invalid"
                 echo ""
                 continue;;
    esac
    break
done


echo ""
echo "$PGMNAME:  Choose a garbage collection method:"
echo ""
echo "            stub--  Only the API calls, no content"
echo "            other-- Roll your own, generates unresolved externals"

while true
do
    echo ""
    $echon "Garbage collection method: [stub,other] $echoc"
    read gcmethod

    case $gcmethod in
        stub)    GCMETHOD="GC_TYPE_STUB";;
        other)   GCMETHOD="GC_TYPE_OTHER";;

        *)       echo ""
                 echo "Garbage collection method '$gcmethod' invalid"
                 echo ""
                 continue;;
    esac
    break
done

###################################################################
#
# Set up phase 4: Extract startup classes for system bootstrap
#
# Extract selected classes from '$CONFIG_RTJARFILE' for use
# during system startup.  This is a convenient workaround for
# when the JAR extraction logic is not yet available.
#
# These classes are required for proper initialization of the
# JVM.  Any others, which are listed in 'src/jvmclass.h' may
# be included also.  The minimum set is:
#
#     #define JVM_STARTCLASS_JAVA_LANG_OBJECT "java/lang/Object"
#     #define JVM_STARTCLASS_JAVA_LANG_VOID   "java/lang/Void"
#     #define JVM_STARTCLASS_JAVA_LANG_STRING "java/lang/String"
#     #define JVM_STARTCLASS_JAVA_LANG_STRING "java/lang/Thread"
#
tput clear
echo ""
echo "Phase 4: Extract startup classes"
echo "--------------------------------"
echo ""
echo "The environment BOOTCLASSPATH is used to make selected classes"
echo "more easily accessible during runtime startup.  When installed,"
echo "a BOOTCLASSPATH directory decreases startup time significantly"
echo "by having fundamental classes ready to be loaded without having"
echo "to extract them from the class library.  A minimum set is needed"
echo "and a larger set is available if desired, although they are not"
echo "vital to normal startup scenarios.  This environment variable"
echo "may also be overridden on the 'bootjvm' command line."
echo ""

# Squeeze msg onto 80-columns (just a matter of text standards and form)
MSG80="Do you wish to set up the BOOTCLASSPATH library?"
echo  ""
$echon "$MSG80 [y,n] $echoc"
read libsetup

biglib=0
case $libsetup in
    y|ye|yes|Y|YE|YES)
        dosetupboot=1

        echo  ""
        echo "How much disk buffering do you wish"
     $echon "for the BOOTCLASSPATH directory to use? [less,more] $echoc"
        read libsize

        case $libsize in
            m|mo|mor|more|M|MO|MOR|MORE) biglib=1;;
            *)                           biglib=0;; # Redundant
        esac
        ;;

    *)  dosetupboot=0;;
esac

###################################################################
#
# Set up phase 5:  Build binary and documentation from source code
#
tput clear
echo ""
echo "Phase 5:  Build binary and documentation from source code"
echo "---------------------------------------------------------"
echo ""
echo "The code may be partitioned into several components and built"
echo "either all at once and/or by its various parts.  The top-level"
echo "build script has these same options, and using 'make cfg'"
echo "will build what is requested here in addition to individual"
echo "selections.  The default option is 'make cfg'.  Choosing"
echo "'all' declares that all components are to be built by the"
echo "default build option 'make cfg'."
echo ""
$echon "more... $echoc"
read dummy
echo ""
echo \
"The several options for building the code (answer 'yes' or 'no') are:"
echo ""
echo "    all--   Build the entire code tree, namely:"
echo ""
echo "    jvm--   Build the main development area"
echo "    libjvm--Build the static JVM library"
echo "    main--  Build the sample main() program (links 'libjvm.a')"
echo "    test--  Build the Java test code area"
echo "    jni--   Build the sample JNI code area (links 'libjvm.a')"
echo "    dox--   Build the documentation (using doxygen)"
echo ""
echo "A suggested combination for beginning users is 'jvm' and 'dox' to"
echo "build everything in one place and generate documentation changes"
echo "when appropriate."
echo ""
echo "A suggested combination for integrators is 'lib' and 'main' for"
echo "creating the JVM in a library and testing it out with 'main'."
echo ""
echo "A suggested combination for test case writers is 'jvm' and 'test'"
echo "for creating the JVM in a binary and adding test cases."
echo ""

SHOULDBUILD="Should 'make' construct"
MSG80ALL="all:  $SHOULDBUILD the entire code tree?"
MSG80JVM="jvm:  $SHOULDBUILD the main develoment area?"
MSG80LIB="lib:  $SHOULDBUILD the static JVM library?"
MSG80MAIN="main: $SHOULDBUILD the sample main() program?"
MSG80TEST="test: $SHOULDBUILD the Java test code area?"
MSG80JNI="jni:  $SHOULDBUILD the sample JNI code area?"
MSG80DOX="dox:  $SHOULDBUILD the documentation area?"

BUILD_JVM=1
BUILD_LIB=1
BUILD_MAIN=1
BUILD_TEST=1
BUILD_JNI=1
BUILD_DOX=1

while true
do
    echo  ""
    $echon "$MSG80ALL [y,n] $echoc"
    read buildall

    case $buildall in
        y|ye|yes|Y|YE|YES)
           # All 'BUILD_xxx' vars are set to 1 above
           ;;

        n|no|N|NO)
           echo  ""
           $echon "$MSG80JVM [y,n] $echoc"
           read buildjvm
           case $buildjvm in
               y|ye|yes|Y|YE|YES) BUILD_JVM=1;;
               *)                 BUILD_JVM=0;;
           esac

           echo  ""
           $echon "$MSG80LIB [y,n] $echoc"
           read buildlib
           case $buildlib in
               y|ye|yes|Y|YE|YES) BUILD_LIB=1;;
               *)                 BUILD_LIB=0;;
           esac

           echo  ""
           $echon "$MSG80MAIN [y,n] $echoc"
           read buildmain
           case $buildmain in
               y|ye|yes|Y|YE|YES) BUILD_MAIN=1;;
               *)                 BUILD_MAIN=0;;
           esac

           echo  ""
           $echon "$MSG80TEST [y,n] $echoc"
           read buildmain
           case $buildmain in
               y|ye|yes|Y|YE|YES) BUILD_TEST=1;;
               *)                 BUILD_TEST=0;;
           esac

           echo  ""
           $echon "$MSG80JNI [y,n] $echoc"
           read buildjni
           case $buildjni in
               y|ye|yes|Y|YE|YES) BUILD_JNI=1;;
               *)                 BUILD_JNI=0;;
           esac

           echo  ""
           $echon "$MSG80DOX [y,n] $echoc"
           read builddox
           case $builddox in
               y|ye|yes|Y|YE|YES) BUILD_DOX=1;;
               *)                 BUILD_DOX=0;;
           esac
           ;;

        *) continue;;
    esac

    break
done
echo ""
echo ""
echo "The documentation creation process is independent of the"
echo "pre-formatted documentation installed into 'doc.ORIG'.  When"
echo "generated, it gets stored into the 'doc' directory without regard"
echo "for previous contents.  It may be generated either through the"
echo "pre-configured build process (per above question) or by direct"
echo "action from the top-level build command 'make dox'.  When"
echo "time comes to generate documentation, there are several formats"
echo "available.  Most options may be used in combination to yield only"
echo "the desired formats.  Choosing 'all' configures every format."
echo ""
$echon "more... $echoc"
read dummy
echo ""
echo "The several options for building various documentation formats"
echo "(answer 'yes' or 'no') are:"
echo ""
echo "            all--  Build documentation in every format, namely:"
echo ""
echo "            html-- Build HTML format docs (doc/html)"
echo "            latex--Build Latex info format docs (doc/latex)"
echo "            rtf--  Build RTF docs (doc/rtf)"
echo "            man--  Build man section 3 docs (doc/man/man3)"
echo "            xml--  Build XML format docs (doc/xml)"
echo ""
echo "(Note:  Choosing 'rtf' may cause doxygen to generate spurious"
echo "        messages, 'QGDict::hashAsciiKey: Invalid null key' on"
echo "        otherwise perfectly formatted documentation.  Reasons"
echo "        are not yet known.)"
echo ""
echo "A suggested combination for Unix users might be HTML and"
echo "either man page or latex formats."
echo ""
echo "A suggested combination for Windows users might be HTML and"
echo "RTF formats."
echo ""
$echon "more... $echoc"
read dummy
echo ""
echo "Older versions of the NetScape HTML browser can do odd things to"
echo "newer versions of HTML code.  In particular, the persistent"
echo "versions 4.7X that are still widely used on many Unix systems"
echo "may experience difficulties.  The output of Doxygen may require"
echo "certain adjustments, particularly on presentation of code"
echo "fragments and other so-called 'verbatim' fragments."
echo ""

MSG80HTML="Do you need these adjustments done for your HTML browser?"
while true
do
    echo  ""
    $echon "$MSG80HTML [y,n] $echoc"
    read adjnetscape

    case $adjnetscape in
        y|ye|yes|Y|YE|YES) BUILD_HTML_ADJ_NETSCAPE47X=YES
                           break;;

        n|no|N|NO)         BUILD_HTML_ADJ_NETSCAPE47X=NO
                           break;;

        *)                 continue;;
    esac
done



SHOULDBUILD="Should 'make dox' build"
MSG80ALL="all:    $SHOULDBUILD docs in every format?"
MSG80HTML="html:   $SHOULDBUILD HTML format docs (doc/html) ?"
MSG80LATEX="latex:  $SHOULDBUILD Latex info format docs (doc/latex) ?"
MSG80RTF="rtf:    $SHOULDBUILD RTF docs (doc/rtf) ?"
MSG80MAN="man:    $SHOULDBUILD man section 3 docs (doc/man/man3) ?"
MSG80XML="xml:    $SHOULDBUILD XML format docs (doc/xml) ?"

BUILD_HTML=YES
BUILD_LATEX=YES
BUILD_RTF=YES
BUILD_MAN=YES
BUILD_XML=YES

while true
do
    echo  ""
    $echon "$MSG80ALL [y,n] $echoc"
    read buildall

    case $buildall in
        y|ye|yes|Y|YE|YES)
           BUILD_ALLDOX=YES
           ;;

        n|no|N|NO)
           BUILD_ALLDOX=NO

           echo  ""
           $echon "$MSG80HTML [y,n] $echoc"
           read buildhtml
           case $buildhtml in
               y|ye|yes|Y|YE|YES) BUILD_HTML=YES;;
               *)                 BUILD_HTML=NO;;
           esac

           echo  ""
           $echon "$MSG80LATEX [y,n] $echoc"
           read buildlatex
           case $buildlatex in
               y|ye|yes|Y|YE|YES) BUILD_LATEX=YES;;
               *)                 BUILD_LATEX=NO;;
           esac

           echo  ""
           $echon "$MSG80RTF [y,n] $echoc"
           read buildrtf
           case $buildrtf in
               y|ye|yes|Y|YE|YES) BUILD_RTF=YES;;
               *)                 BUILD_RTF=NO;;
           esac

           echo  ""
           $echon "$MSG80MAN [y,n] $echoc"
           read buildman
           case $buildman in
               y|ye|yes|Y|YE|YES) BUILD_MAN=YES;;
               *)                 BUILD_MAN=NO;;
           esac

           echo  ""
           $echon "$MSG80XML [y,n] $echoc"
           read buildxml
           case $buildxml in
               y|ye|yes|Y|YE|YES) BUILD_XML=YES;;
               *)                 BUILD_XML=NO;;
           esac
           ;;

        *) continue;;
    esac

    break
done
echo ""

echo ""
MSG80="the configured components after configuration"
$echon "Do you wish to build ${MSG80}? [y,n] $echoc"
read buildnow
echo ""
echo  ""
$echon "$PGMNAME: Ready to configure and build... $echoc"
read doit
echo  ""

###################################################################
#
# Run phase 1:  Pre-formatted documentation
#
echo  ""
$echon "$PGMNAME: Starting to configure... $echoc"
sleep 3
echo  ""
echo  ""
echo  "$PGMNAME:  configuring project"
echo  ""
case $prefmtdocs in
    y|ye|yes|Y|YE|YES)
        PREFMTDOCSTAR="$PGMDIR/bootJVM-docs.tar"
        echo "$PGMNAME:  Testing $PREFMTDOCSTAR.gz"

        if test ! -r $PREFMTDOCSTAR.gz
        then
  echo "$PGMNAME:  Pre-formatted documents not found: $PREFMTDOCSTAR.gz"
            echo ""
            echo "This probably means that you installed from a"
            echo "distribution containing only source, namely from SVN"
            echo "or from a 'bootJVM-src-x.y.z.tar.gz' file. The"
            echo "documentation may be found in both the"
            echo "'bootJVM-doc-x.y.z.tar.gz'" and in the
            echo "'bootJVM-srcdoc-x.y.z.tar.gz' distributions."
            echo "This latter also contains the source distribution."
            echo ""
            echo "The documentation can be generated from the source"
            echo "itself using 'make dox' at any time."
            echo ""
            $echon "ready... $echoc"
            read dummy
            echo ""
            echo ""
            echo "Configuration will continue..."
            echo ""
            sleep 3
            # exit 8

        else
            chmod 0444 $PREFMTDOCSTAR.gz

          echo "$PGMNAME:  Removing previous documentation installation"
            if test -d doc/.; then chmod -R +w doc; fi
            if test -d doc.ORIG/.; then chmod -R +w doc.ORIG; fi

            rm -rf doc doc.ORIG
            if test -d doc/.
            then
              echo "$PGMNAME:  Could not remove directory '$PGMDIR/doc'"
                exit 9
            fi
            if test -d doc.ORIG/.
            then
         echo "$PGMNAME:  Could not remove directory '$PGMDIR/doc.ORIG'"
                exit 10
            fi

            echo "$PGMNAME:  Installing pre-formatted documentation"
            cat $PREFMTDOCSTAR.gz | gunzip | tar xf - doc 

            echo "$PGMNAME:  Verifying documentation install"
            if test 0 -ne $?
            then
     echo "$PGMNAME:  Cannot perform tar extract from $PREFMTDOCSTAR.gz"
                exit 11
            fi
            if test ! -d doc/.
            then
                echo "$PGMNAME:  Cannot create '$PGMDIR/doc' directory"
                exit 12
            fi

      echo "$PGMNAME:  Moving pre-formatted documentation to 'doc.ORIG'"
            mv doc doc.ORIG
            chmod -R -w doc.ORIG

            if test ! -d doc.ORIG/.
            then
            echo "$PGMNAME:  Cannot create '$PGMDIR/doc.ORIG' directory"
                exit 13
            fi
        fi
        ;;

    *)  ;;
esac

###################################################################
#
# Run phase 3:  C Compilation and Document Compilation Setup
#
# (Phase 2 did setup and run all above.)
#
if test -d config/.
then
    chmod -R +w config/.
fi
rm -rf config
if test -d config/.
then
    echo "$PGMNAME:  Could not remove directory '$PGMDIR/config'"
    exit 14
fi

mkdir config
if test ! -d config
then
    echo "$PGMNAME:  Cannot create '$PGMDIR/config' directory"
    exit 15
fi

CRME="config/README"
THISDATE=`date`
(
    echo ""
    echo "Boot JVM build configuration files."
    echo ""
    echo "Auto-generated by $PGMNAME"
    echo "on $THISDATE:"
    echo "DO NOT MODIFY!"
    echo ""
    echo "Instead, run $PGMNAME to change _anything_ at all!"
    echo "(The same goes for the '*.gcc' and '*.gccld' files in this"
    echo "directory, but notice that they cannot contain any comments.)"
    echo ""

) > $CRME

chmod -w $CRME

(
    # Include basic doxygen tokens for documentation purposes
    echo "/*!"
    echo " * @file config.h"
    echo " *"
    echo " * @brief Boot JVM build configuration."
    echo " *"
    echo " * Top-level configuration declarations."
 echo " * Auto-generated by @link ./config.sh $PGMNAME@endlink"
    echo " * on $THISDATE:"
    echo " * <b>@verbatim"
    echo ""
    echo "   DO NOT MODIFY!"
    echo ""
    echo "   @endverbatim</b>"
    echo " *"
    echo " * @section Control"
    echo " *"
    echo " * Id:  Auto-generated by @c @b $PGMNAME"
    echo " *"
    echo " * Copyright `date +%Y` The Apache Software Foundation"
    echo " * or its licensors, as applicable."
    echo " *"
echo " * Licensed under the Apache License, Version 2.0 (\"the License\");"
echo " * you may not use this file except in compliance with the License."
    echo " * You may obtain a copy of the License at"
    echo " *"
    echo " *     http://www.apache.org/licenses/LICENSE-2.0"
    echo " *"
    echo " * Unless required by applicable law or agreed to in writing,"
   echo " * software distributed under the License is distributed on an"
  echo " * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,"
    echo " * either express or implied."
    echo " *"
echo " * See the License for the specific language governing permissions"
    echo " * and limitations under the License."
    echo " *"
    echo " * @version N/A"
    echo " *"
    echo " * @date $THISDATE"
    echo " *"
    echo " * @author $USER"
    echo " *"
    echo " * @section Reference"
    echo " *"
    echo " */"

    echo ""
    echo "/*!"
    echo " * @def CONFIG_PROGRAM_NAME"
    echo " * @brief Short project name string"
    echo " *"
    echo " * The program name string is used in both the source"
    echo " * and in the documentation title.  It is known between"
    echo " * this header and the Doxygen setup, where it is"
    echo " * called @c @b PROJECT_NAME in that parlance.  The"
    echo " * definition of @link #CONFIG_PROGRAM_DESCRIPTION"
    echo "   CONFIG_PROGRAM_DESCRIPTION@endlink is appended to"
    echo " * @c @b CONFIG_PROJECT_NAME to form the"
    echo " * text 'CONFIG_PROGRAM_NAME: CONFIG_PROGRAM_DESCRIPTION'"
    echo " * in that location."
    echo " *"
    echo " */"
    echo "#define CONFIG_PROGRAM_NAME \"$PROGRAM_NAME\""

    echo ""
    echo "/*!"
    echo " * @def CONFIG_PROGRAM_DESCRIPTION"
    echo " * @brief Short description string of project function"
    echo " *"
    echo " * The program description is a short string describing"
    echo " * the functionality of the program.  It is known commonly"
    echo " * between this header and the Doxygen setup, where it is"
    echo " * called @c @b PROJECT_NAME in that parlance.  The"
    echo " * definition of @c @b CONFIG_PROGRAM_DESCRIPTION is appended"
    echo " * to @link #CONFIG_PROGRAM_NAME CONFIG_PROGRAM_NAME@endlink"
    echo " * to form the"
    echo " * text 'CONFIG_PROGRAM_NAME: CONFIG_PROGRAM_DESCRIPTION'"
    echo " * in that location."
    echo " *"
    echo " */"
    echo "#define CONFIG_PROGRAM_DESCRIPTION \"$PROGRAM_DESCRIPTION\""

    echo ""
    echo "/*!"
    echo " * @def CONFIG_RELEASE_LEVEL"
    echo " * @brief Project release as major, minor, and patchlevel"
    echo " *"
    echo " * The release number is stored in a three-field tuple"
    echo " * as 'major.minor.patchlevel'.  It is known commonly"
    echo " * between this header and the Doxygen setup, where it"
    echo " * is called @c @b PROJECT_NUMBER in that parlance."
    echo " *"
    echo " */"
    echo "#define CONFIG_RELEASE_LEVEL \"$RELEASE_LEVEL\""

    echo ""
    echo "/*!"
    echo " * @def CONFIG_WORDWIDTH"
    echo " * @brief Null-terminated string number of bits in real"
    echo " * machine integer word."
    echo " *"
    echo " * This value may be either @b 32 or @b 64."
    echo " *"
    echo " * @ see CONFIG_WORDWIDTH$WORDWIDTH"
    echo " *"
    echo " */"
    echo "#define CONFIG_WORDWIDTH \"$wordwidth\""

    echo ""
    echo "/*!"
    echo " * @def CONFIG_WORDWIDTH$WORDWIDTH"
    echo " * @brief Number of bits in real machine integer word."
    echo " *"
    echo " * This value may be either @b 32 or @b 64."
    echo " *"
    echo " */"
    echo "#define CONFIG_WORDWIDTH$WORDWIDTH"

    echo ""
    echo "/*!"
    echo " * @def CONFIG_CPUTYPE"
    echo " * @brief Null-terminated string name or type of CPU."
    echo " *"
    echo " * Useful for diagnostic strings that report CPU information."
    echo " *"
    echo " * This value typically implies a specific CPU architecture"
    echo " * rather than a manufacturer of a number of them."
    echo " *"
    echo " * @see CONFIG_$CPUTYPE"
    echo " *"
    echo " */"
    echo "#define CONFIG_CPUTYPE \"$cputype\""

    echo ""
    echo "/*!"
    echo " * @def CONFIG_$CPUTYPE"
    echo " * @brief Explicitly named type of CPU."
    echo " *"
    echo " * Another way at specifying CPU architecture, but with"
    echo " * an explicit token rather than a string in a token."
    echo " *"
    echo " * @b sparc means Sun's SPARC CPU architecture."
    echo " *"
    echo " * @b intel means Intel's x86 CPU architecture."
    echo " *"
    echo " * @see CONFIG_CPUTYPE"
    echo " *"
    echo " */"
    echo "#define CONFIG_$CPUTYPE"

    echo ""
    echo "/*!"
    echo " * @def CONFIG_$CPUTYPE$WORDWIDTH"
    echo \
" * @brief Combination of CONFIG_$CPUTYPE and CONFIG_WORDWIDTH$WORDWIDTH."
    echo " *"
    echo " */"
    echo "#define CONFIG_$CPUTYPE$WORDWIDTH"

    echo ""
    echo "/*!"
    echo " * @def CONFIG_OSNAME"
    echo " * @brief Null-terminated string name of operating system."
    echo " *"
    echo " * Useful for diagnostic strings that report OS information."
    echo " *"
    echo " *"
    echo " * @see CONFIG_$OSNAME"
    echo " *"
    echo " */"
    echo "#define CONFIG_OSNAME \"$osname\""

    echo ""
    echo "/*!"
    echo " * @def CONFIG_$OSNAME"
    echo " * @brief Operating system name."
    echo " *"
  echo " * This value may be @b solaris or @b linux or @b windows or @b cygwin."
    echo " *"
    OSTXTABBRV="Unix architecture operating system" # Keep short lines
    echo " * @b solaris means Sun's premier $OSTXTABBRV"
    echo " *"
    echo " * @b linix means the open-source $OSTXTABBRV"
    echo " *"
    echo " * @b windows means Microsoft's proprietary operating system."
    echo " *"
    echo " * @b cygwin means the open-source Unix work-alike Windows utility."
    echo " *"
    echo " * Notice that, with the exception of @b windows and @b cygwin,"
    echo " * various CPU architectures may run various operating systems."
    echo " *"
    echo " * @see CONFIG_OSNAME"
    echo " *"
    echo " */"
    echo "#define CONFIG_$OSNAME"

    echo ""
    echo "/*!"
    echo " * @def CONFIG_$OSNAME$WORDWIDTH"
    echo \
" * @brief Combination of CONFIG_$OSNAME and CONFIG_WORDWIDTH$WORDWIDTH."
    echo " *"
    echo " */"
    echo "#define CONFIG_$OSNAME$WORDWIDTH"

    echo ""
    echo "/*!"
    echo " * @def CONFIG_HACKED_RTJARFILE"
    echo " * @brief Location of run-time Java class library archive."
    echo " *"
    echo " * This JAR file name may be appended to @b BOOTCLASSPATH"
    echo " * as a development hack to provide a default startup class"
    echo " * library until the project has something better.  This"
    echo " * file name points to the JRE run-time class library archive"
    echo " * file name as requested from user input by" 
    echo " * @c @b $PGMNAME at configuration time."
    echo " *"
    echo " * This symbol may be removed via @c @b \#undef in"
    echo \
" * @link jvm/src/jvmcfg.h jvmcfg.h @endlink if desired without"
    echo " * changing its definition here in this file."
    echo " *"
    echo " * @todo  HARMONY-6-config.sh-3 Put fuller definition for"
    echo " *        this symbol and its usage into the code in"
    echo " *        @link jvm/src/classpath.c classpath.c @endlink."
    echo " *"
    echo " * @todo  HARMONY-6-config.sh-4 Need to find some other way"
    echo " *        to locate the class library archive in the JDK so"
    echo " *        that @e any JDK may be considered (initially)."
    echo " *        Need to ultimately change out the logic that uses"
    echo " *        it over time to begin looking for natively"
    echo " *        constructed class library archive instead of" 
    echo " *        leaning on outside work."
    echo " *"
    echo " */"
    echo "#define CONFIG_HACKED_RTJARFILE \"$RTJARFILE\""

    echo ""
    echo "/*!"
    echo " * @def CONFIG_HACKED_BOOTCLASSPATH"
    echo \
      " * @brief Location of provisional run-time Java startup classes."
    echo " *"
    echo " * Internally append this name onto the end of @b CLASSPATH"
    echo " * as a development hack to provide a default startup class"
    echo " * library until the project has something better.  This"
    echo " * directory name points to the @b bootclasspath directory"
    echo " * as created by @c @b $PGMNAME at configuration time."
    echo " *"
    echo " * This symbol may be removed via @c @b \#undef in"
    echo \
" * @link jvm/src/jvmcfg.h jvmcfg.h @endlink if desired without"
    echo " * changing its definition here in this file."
    echo " *"
    echo " * @todo  HARMONY-6-config.sh-5 Put fuller definition for"
    echo " *        this symbol and its usage into the code in"
    echo " *        @link jvm/src/classpath.c classpath.c @endlink"
    echo " *        and @link jvm/src/argv.c argv.c @endlink."
    echo " *"
    echo " * @todo  HARMONY-6-config.sh-6 Remove compiled absolute path"
    echo " *        name in favor or either a relative path name or"
    echo " *        removal of this symbol from the logic entirely."
    echo " *"
    echo " */"
    echo "#define CONFIG_HACKED_BOOTCLASSPATH \"$PGMDIR/bootclasspath\""
    echo ""

  if test -z "$CCOMPILER"
  then
    echo ""
    echo "/*!"
    echo " * @internal There is no C compiler configured"
    echo " *"
    echo " */"
  else
    echo ""
    echo "/*!"
    echo " * @def CONFIG_CCOMPILER_$CCOMPILER"
    echo " * @brief C Compiler"
    echo " *"
    echo " * This value may be @b gcc or @b other."
    echo " *"
    echo " * @b gcc means use GNU C compiler"
    echo " *"
    echo " * @b other means use the default as specified in"
    echo " *          @link ./MakeRules ./MakeRules@endlink"
    echo " *"
    echo " */"
    echo "#define CONFIG_CCOMPILER_$CCOMPILER"
  fi

  if test -z "$HEAPALLOC"
  then
    echo ""
    echo "/*!"
    echo " * @internal There is no heap allocation method configured"
    echo " *"
    echo " */"
  else
    echo ""
    echo "/*!"
    echo " * @def CONFIG_$HEAPALLOC"
    echo " * @brief Heap allocation method"
    echo " *"
    echo " * This value may be @b simple or @b bimodal or @b other."
    echo " *"
    echo " * @b simple means @c @b malloc(3)/free(3) only"
    echo " *"
  echo " * @b bimodal means @c @b malloc(3)/free(3) plus a large buffer"
    echo " *"
    echo " * @b other means roll your own-- generates unresolved"
    echo " *          external symbols."
    echo " *"
    echo " * Refer to"
    echo " * @link jvm/src/heap_bimodal.c heap_bimodal.c@endlink"
    echo " * for a good example as to how to implement the heap API and"
    echo " * incorporate it into the suite of heap allocation options."
    echo " * Remember also to add it to"
    echo " * @link ./$PGMNAME $PGMNAME@endlink"
    echo " * so others may configure and use it."
    echo " *"
    echo " */"
    echo "#define CONFIG_$HEAPALLOC"
  fi

  if test -z "$GCMETHOD"
  then
    echo ""
    echo "/*!"
    echo " * @internal There is no garbage collection method configured"
    echo " *"
    echo " */"
  else
    echo ""
    echo "/*!"
    echo " * @def CONFIG_$GCMETHOD"
    echo " * @brief Garbage collection method"
    echo " *"
    echo " * This value may be @b stub or @b other."
    echo " *"
    echo " * @b stub means API only, no content"
    echo " *"
    echo " * @b other means roll your own-- generates unresolved"
    echo " *          external symbols."
    echo " *"
    echo " * Refer to"
    echo " * @link jvm/src/heap_bimodal.c heap_bimodal.c@endlink"
    echo " * for a good example as to how to implement the heap API and"
    echo " * incorporate it into the suite of heap allocation options."
    echo " * The garbage collection API is similarly implemented."
    echo " * Remember also to add it to"
    echo " * @link ./$PGMNAME $PGMNAME@endlink"
    echo " * so others may configure and use it."
    echo " *"
    echo " */"
    echo "#define CONFIG_$GCMETHOD"
  fi

    echo "/* EOF */"

) > $CFGH

chmod -w $CFGH

# Set up include path for the current tree, the configuration,
# the JVM tree, and the JDK tree.
JAVA_INCLUDE_PATHS="-I$(JAVA_HOME)/include \
  -I$(JAVA_HOME)/include/$osJDKdir"

USEDOX="for 'dox.sh' and 'gmake dox'"
USEBLDCLN="for 'gmake all' and 'gmake clean'"

CBSD="config/config_build_steps.sh"
CBSM="config/config_build_steps.mak"
(
    echo "#"
    echo "# Code build steps configured by user"
    echo "# $USEBLDCLN"
    echo "#"
    echo "# Auto-generated by $PGMNAME"
    echo "# on $THISDATE:"
    echo "# DO NOT MODIFY!"
    echo "#"
) | tee $CBSD > $CBSM
(
    echo "CONFIG_REPOSITORY=\"https://svn.apache.org/repos/asf/incubator/harmony/enhanced/trunk/sandbox/contribs/bootjvm/\""
    echo "export CONFIG_REPOSITORY"
    echo "#"
    echo "CONFIG_WORDWIDTH=\"$wordwidth\""
    echo "export CONFIG_WORDWIDTH"
    echo "#"
    echo "CONFIG_CPUTYPE=\"$cputype\""
    echo "export CONFIG_CPUTYPE"
    echo "#"
    echo "CONFIG_OSNAME=\"$osname\""
    echo "export CONFIG_OSNAME"
    echo "#"
    echo "CONFIG_RELEASE_LEVEL=\"$RELEASE_LEVEL\""
    echo "export CONFIG_RELEASE_LEVEL"
    echo "#"
    echo \
      "CONFIG_BUILD_HTML_ADJUST_NETSCAPE47X=$BUILD_HTML_ADJ_NETSCAPE47X"
    echo "export CONFIG_BUILD_HTML_ADJUST_NETSCAPE47X"
    echo "#"
) >> $CBSD
(
    echo "CONFIG_WORDWIDTH=$wordwidth"
    echo "#"
    echo "JAVA_INCLUDE_PATHS:=$JAVA_INCLUDE_PATHS"
    echo "#"
) >> $CBSM
(
    echo "CONFIG_BUILD_JVM=$BUILD_JVM"
    echo "CONFIG_BUILD_LIB=$BUILD_LIB"
    echo "CONFIG_BUILD_MAIN=$BUILD_MAIN"
    echo "CONFIG_BUILD_TEST=$BUILD_TEST"
    echo "CONFIG_BUILD_JNI=$BUILD_JNI"
    echo "CONFIG_BUILD_DOX=$BUILD_DOX"
    echo "#"
    echo "# EOF"
) | tee -a $CBSD >> $CBSM

chmod -w $CBSD $CBSM

CDSD="config/config_dox_setup.dox"
CDSM="config/config_dox_setup.mak"
(
    echo "#"
    echo "# documentation build steps configured by user $USEDOX"
    echo "#"
    echo "# Auto-generated by $PGMNAME"
    echo "# on $THISDATE:"
    echo "# DO NOT MODIFY!"
    echo "#"
    echo "PROJECT_NAME=\"$PROGRAM_NAME: $PROGRAM_DESCRIPTION\""
    echo "PROJECT_NUMBER=\"$RELEASE_LEVEL\""
    echo "GENERATE_HTML=$BUILD_HTML"
    echo "GENERATE_LATEX=$BUILD_LATEX"
    echo "GENERATE_RTF=$BUILD_RTF"
    echo "GENERATE_MAN=$BUILD_MAN"
    echo "GENERATE_XML=$BUILD_XML"
    echo ""
    echo "# EOF"

) | tee $CDSD > $CDSM

chmod -w $CDSD $CDSM

(
    roster.sh
    echo "$PGMNAME:  Compile configuration:         $CFGH"
    echo "$PGMNAME:  Code build steps:              $CBSD"
    echo "                                           $CBSM"
    echo "$PGMNAME:  Documentation build steps:     $CDSD"
    echo "                                           $CDSM"
    echo ""
) | more


#############################
#
# END set up 'config' directory.
#

###################################################################
#
# Run phase 4: Extract startup classes for system bootstrap
#
if test 1 -eq $dosetupboot
then

    ###
    echo "$PGMNAME:  Setting up boot class library class area..."

    rm -rf bootclasspath
    if test -d bootclasspath
    then
    echo "$PGMNAME:  Could not remove directory '$PGMDIR/bootclasspath'"
        exit 16
    fi

    mkdir bootclasspath
    if test ! -d bootclasspath
    then
       echo "$PGMNAME:  Cannot create '$PGMDIR/bootclasspath' directory"
        exit 17
    fi

    echo ""
    echo "$PGMNAME:  Extracting classes for $PGMDIR/bootclasspath"
    ###

    # Normally ('true') just extract those that are always referenced
    # at startup, but if desired ('false'), extract a whole list of
    # useful and interesting classes, especially those that are
    # used during JVM initialization.
    if test 0 -eq $biglib
    then
        JAVA_LANG_CLASS_LIST="Object Class String Thread"
        JAVA_LANG_REF_CLASS_LIST=""
        JAVA_LANG_REFLECT_CLASS_LIST=""
        JAVA_UTIL_JAR_CLASS_LIST=""
        JAVA_UTIL_ZIP_CLASS_LIST=""
        JAVA_IO_CLASS_LIST=""
    else
        JAVA_LANG_CLASS_LIST="Object Class String Thread \
\
        ThreadGroup Void Runtime System \
\
        SecurityManager ClassLoader \
\
        Throwable StackTraceElement \
\
        Exception \
        ClassNotFoundException \
        CloneNotSupportedException \
\
        RuntimeException \
        ArithmeticException \
        ArrayIndexOutOfBoundsException \
        ArrayNegativeSizeException \
        ArrayStoreSizeException \
        IllegalArgumentException \
        IllegalMonitorStateException \
        IllegalThreadStateException \
        InterruptedException \
        IndexOutOfBoundsException \
        IndexOutOfBoundsException \
        NullPointerException \
        SecurityException \
\
        Error \
        ClassFormatError \
        ClassCircularityError \
        ExceptionInitializationError \
        IllegalAccessError \
        IncompatibleClassChangeError \
        InstantiationError \
        InternalError \
        LinkageError \
        NoClassDefFoundError \
        NoSuchFieldError \
        NoSuchMethodError \
        OutOfMemoryError \
        StackOverflowError \
        UnknownError \
        UnsatisfiedLinkError \
        UnsupportedClassVersionError \
        VerifyError \
        VirtualMachineError"

        JAVA_LANG_REF_CLASS_LIST="Finalizer"

        JAVA_LANG_REFLECT_CLASS_LIST="Array Constructor Method Field"

        JAVA_UTIL_JAR_CLASS_LIST="JarFile JarEntry Manifest"

        JAVA_UTIL_ZIP_CLASS_LIST="ZipFile ZipEntry"

        JAVA_IO_CLASS_LIST="InputStream IOException \
        FileNotFoundException \
\
        OutputStream FilterOutputStream PrintStream \
\
        Serializable"
    fi

    cd bootclasspath
    rc=0
    if test -n "$JAVA_LANG_CLASS_LIST"
    then
        for class in $JAVA_LANG_CLASS_LIST
        do
            echo "java.lang.$class"
            $JAVA_HOME/bin/jar xf $CONFIG_RTJARFILE \
                                  java/lang/$class.class

            if test 0 -ne $rc
            then
                rc=1
            fi
        done
    fi
    if test -n "$JAVA_LANG_REFCLASS_LIST"
    then
        for class in $JAVA_LANG_REF_CLASS_LIST
        do
            echo "java.lang.ref.$class"
            $JAVA_HOME/bin/jar xf $CONFIG_RTJARFILE \
                                  java/lang/ref/$class.class

            if test 0 -ne $rc
            then
                rc=1
            fi
        done
    fi
    if test -n "$JAVA_LANG_REFLECT_CLASS_LIST"
    then
        for class in $JAVA_LANG_REFLECT_CLASS_LIST
        do
            echo "java.lang.reflect.$class"
            $JAVA_HOME/bin/jar xf $CONFIG_RTJARFILE \
                                  java/lang/reflect/$class.class

            if test 0 -ne $rc
            then
                rc=1
            fi
        done
    fi
    if test -n "$JAVA_UTIL_JAR_CLASS_LIST"
    then
        for class in $JAVA_UTIL_JAR_CLASS_LIST
        do
            echo "java.util.jar.$class"
            $JAVA_HOME/bin/jar xf $CONFIG_RTJARFILE \
                                  java/util/jar/$class.class
    
        if test 0 -ne $rc
            then
                rc=1
            fi
        done
    fi
    if test -n "$JAVA_UTIL_ZIP_CLASS_LIST"
    then
        for class in $JAVA_UTIL_ZIP_CLASS_LIST
        do
            echo "java.util.zip.$class"
                $JAVA_HOME/bin/jar xf $CONFIG_RTJARFILE \
                                   java/util/zip/$class.class

            if test 0 -ne $rc
            then
                rc=1
            fi
        done
    fi
    if test -n "$JAVA_IO_CLASS_LIST"
    then
        for class in $JAVA_IO_CLASS_LIST
        do
            echo "java.io.$class"
            $JAVA_HOME/bin/jar xf $CONFIG_RTJARFILE \
                                   java/io/$class.class

            if test 0 -ne $rc
            then
                rc=1
            fi
        done
    fi
    if test 0 -ne $rc
    then
        echo "$PGMNAME:  Could not extract all startup library classes"
        exit 18
    fi

    cd ..

fi

#############################
#
# END set up 'bootclasspath' directory.
#
echo ""
$echon "$PGMNAME: Starting to build... $echoc"
sleep 3
echo ""
echo ""

###################################################################
#
# Run phase 5:  Build binary from source code
#
echo  ""
echo  "$PGMNAME:  Building configured components"
echo ""
echo "$PGMNAME:  Cleaning out entire tree of anything left over"
echo ""
make clean
rc=$?


case $buildnow in
    y|ye|yes|Y|YE|YES)
        echo ""
        echo "$PGMNAME:  Building configured components"
        echo ""
        make cfg
        rc=$?
        ;;
    *)  ;;
esac

###################################################################
#
# Done.  Return with exit code from build script.
#
echo ""
echo "$PGMNAME:  Return from build with exit code $rc"
echo ""

exit $rc
#
# EOF
