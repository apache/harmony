#!/bin/sh
#
#!
# @file support/dist-common.sh
#
# @brief Common code for
# @link ./dist-src.sh dist-src.sh@endlink and
# @link ./dist-bin.sh dist-bin.sh@endlink and
# @link ./dist-doc.sh dist-doc.sh@endlink.
#
# This script is common to several scripts and is not designed to do
# anything on its own.
#
# @note Originally, the build scripts @c @b build.sh and @c @b clean.sh
#       were used to build this project.  This was a matter of
#       expediency, knowing full well that a need for @c @b Makefiles
#       would not be too far away.  Both of those scripts also used
#       this common inclusion, but their functions, being superceded
#       by @c @b Makefiles , have been removed, leaving only the
#       distribution functions.
#
# @todo HARMONY-6-support-dist-common.sh-1  Combine all distribution
#       functions into a single @c @b dist.sh script and move this
#       logic into that script, eliminating this file.
#
# All source code compiled according to selections from
# @link ./config.sh config.sh@endlink.  This may or may not include
# any or all of:
#
# <ul>
# <li>@b jvm:      The main JVM development area.  All source code
#                  is found here.  Building this area will compile
#                  source code and link it into a binary form in
#                  @b jvm/bin/bootjvm </li>
#
# <li>@b libjvm:   JVM code as a static library.  Compile the source
#                  code from @b jvm/src (less
#                  @link jvm/src/main.c main.c@endlink) and
#                  archive it into @b libjvm/lib/libjvm.a </li>
#                  (Note:  Due to the fact that @b libjvm/src is a
#                  symbolic link to @b jvm/src , this functionality
#                  is @e only performed here by the Eclipse project
#                  files.  When using @c @b make for normal building,
#                  @b jvm/src is the actual directory, but its
#                  @link jvm/src/Makefile Makefile@endlink is set up
#                  to compile both a library and a binary.  See
#                  @link ./Makefile ./Makefile@endlink for details.)
#
# <li>@b main:     Sample @link main/src/main.c main()@endlink
#                  program.  This program is currently not a unique
#                  program, but is a symbolic link to
#                  @link jvm/src/main.c jvm/src/main.c@endlink.
#                  This may change as necessary.  Compile @b main()
#                  that contains an invokation of the main JVM entry
#                  point @link jvm/src/jvm.c jvm()@endlink and
#                  link to @b libjvm/lib/libjvm.a .</li>
#
# <li>@b jni:      Sample JNI subset, showing how to reference
#                  the local native methods, but using the full
#                  JNI mechanism.  Compile code that meets JNI
#                  naming and prototype conventions that would
#                  normally be stored in a shared object @b .so
#                  or @b .dll file.  (Here it will be statically linked
#                  into a regular binary.)  Then statically link this
#                  code to @b libjvm/lib/libjvm.a .</li>
#
# <li>@b test:     Java test programs for exercising various parts
#                  of the JVM</li>
#
# <li>@b dox:      Documentation of source code in @b jvm directory
# </ul>
#
# Each of these directories contains a @c @b Makefile build script.
# In addition to these, an Eclipse project file is available in each
# for use with the Eclipse C/C++ plugin, so this entire directory tree
# may be imported wholesale into an Eclipse workspace and used without
# changes.  (Eclipse 3.0.2 generated these files.)  Notice that
# the Eclipse setup does not build the documentation set.  This must
# be done manually with the top-level 'make dox'.
#
# @attention For Eclipse uses, be aware that the <b>\$(LFLAGS)</b>
# compile options must be the same as found in
# @link support/MakeRules support/MakeRules@endlink.  These are
# unfortunately @e not directly available to Eclipse and @e must be
# manually verified there-- also check that the compiler <b>\$(CC)</b>
# and archiver <b>\$(AR)</b> and linker <b>$(LN)</b> are correctly
# specified in both placed.  They should be entered in the project
# build parameters for C/C++ in the miscellaneous parameters section.
#  For example,
# @b -m32 and @b -m64 .
#
# Notice that @c @b make may be run instead of or as well as an
# Eclipse build.  There is only a slight difference as to
# where the compiled object files are stored, but the binaries
# and library archives are stored in the same place for both
# methods.  However, @e never use Eclipse for release and
# distribution builds since @c @b make is the "official" release
# method, as it were.
#
#
# @see @link ./Makefile ./Makefile@endlink
#
# @see @link support/MakeSetup support/MakeSetup@endlink
#
# @see @link support/MakeRules support/MakeRules@endlink
#
#
# @todo  HARMONY-6-support-dist-common.sh-2 A Windows .BAT version
#        of this script needs to be written
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
. config/config_build_steps.sh

# `dirname $0` for shells without that utility
PGMDIR=`expr "${0:-.}/" : '\(/\)/*[^/]*//*$'  \| \
             "${0:-.}/" : '\(.*[^/]\)//*[^/][^/]*//*$' \| .`
PGMDIR=`cd $PGMDIR; pwd`

# `basename $0` for shells without that utility
PGMNAME=`expr "/${0:-.}" : '\(.*[^/]\)/*$' : '.*/\(..*\)'`

###################################################################
#
# Preparation for 'dist-src.sh' and 'dist-bin.sh' and 'dist-doc.sh'
#
# Strip out _all_ output, rebuild documentation in _all_ formats,
# archive it, and then delete it.  Package the result in final
# 'tar' file.
#

DistChkReleaseLevel()
{
RELSPEC1="$CONFIG_RELEASE_LEVEL"
RELSPEC2="$CONFIG_CPUTYPE-$CONFIG_OSNAME-$CONFIG_WORDWIDTH"
    echo "$PGMNAME:  Ready to distribute release '$RELSPEC1-$RELSPEC2'"
    echo ""
    $echon "    Is this the correct release level?  [y,n] $echoc"
    read readrelslvl
    echo ""

    case $readrelslvl in
        y|ye|yes|Y|YE|YES)
            ;;
        *)  echo ""
            echo \
"$PGMNAME:  Please run 'config.sh' and set correct release level."
            exit 1
    esac

}

# Names of documentation tar file and source distribution tar file
PREFMTDOCSTAR="bootJVM-docs.tar"

# Can't add release level unless 'config.sh' can locate release level:
#PREFMTDOCSTAR="bootJVM-docs-$CONFIG_RELEASE_LEVEL.tar"

DISTBINTAR="bootJVM-bin-$CONFIG_RELEASE_LEVEL-$CONFIG_CPUTYPE-$CONFIG_OSNAME-$CONFIG_WORDWIDTH.tar"
DISTDOCTAR="bootJVM-doc-$CONFIG_RELEASE_LEVEL.tar"
DISTSRCTAR="bootJVM-src-$CONFIG_RELEASE_LEVEL.tar"
DISTSRCDOCTAR="bootJVM-srcdoc-$CONFIG_RELEASE_LEVEL.tar"

# Transient storage for keeping existing old configuration
PGMTMP=../tmp.$$.$PGMNAME


DistChkTarget()
{
    TARGET_BASENAME="bootJVM-$CONFIG_RELEASE_LEVEL"
    TARGET_HOME="$TARGET_BASENAME"
    if test -h ../$TARGET_HOME -o \
            -d ../$TARGET_HOME -o \
            -f ../$TARGET_HOME
    then
        echo ""
        echo "$PGMNAME: Target ../$TARGET_HOME exists"
        exit 2
    fi
}

DistPrep()
{
    # Keep existing configuration in temp area for later restoration
    rm -rf $PGMTMP
    mkdir $PGMTMP
    if test ! -d $PGMTMP
    then
        echo "$PGMNAME: Cannot create temp area '$PGMTMP'"
        echo "$PGMNAME: Either non-writable parent or file system full."
        echo "$PGMNAME: Please make it writable/not full and try again."
        exit 3
    fi

    . config/config_build_steps.sh
    # # Don't need this constraint if only changing doxygen.css file:
    # if test "NO" != "$CONFIG_BUILD_HTML_ADJUST_NETSCAPE47X"
    # then
    #     echo ""
    #     echo "$PGMNAME: Documentation must be build without any"
    #     echo "adjustment for Netscape 4.7x.  Please run 'config.sh'"
    #     echo "again and do NOT permit this adjustment, then retry"
    #     echo "the distribution."
    #     echo ""
    #     exit 4
    # fi

    chmod +w .
    if test -f $PREFMTDOCSTAR.gz
    then
        mv $PREFMTDOCSTAR.gz $PGMTMP
    fi
    if test -d doc
    then
        mv doc $PGMTMP
    fi
    if test -d doc.ORIG
    then
        mv doc.ORIG $PGMTMP
    fi
    if test -d bootclasspath
    then
        mv bootclasspath $PGMTMP
    fi
}

CDSD="config/config_dox_setup.dox"
DistDocPrep ()
{
    # Keep old doc cfg file, generate all-inclusive temporary one
    # that overrides certain variable setings.
    chmod +w config $CDSD
    if test -f ${CDSD}.ORIG
    then
        chmod +w ${CDSD}.ORIG
    fi
    rm -f ${CDSD}.ORIG
    cp $CDSD ${CDSD}.ORIG
    (
        echo "GENERATE_HTML=YES"
        echo "GENERATE_LATEX=YES"
        echo "GENERATE_RTF=YES"
        echo "GENERATE_MAN=YES"
        echo "GENERATE_XML=YES"
    ) >> $CDSD
}


DistTargetBuild ()
{
    echo ""
    echo \
"$PGMNAME: Creating documentation in _all_ formats via 'make $1'"
    # Make SURE the output area is clean
    SUPPRESS_DOXYGEN_VERYCLEAN=
    export SUPPRESS_DOXYGEN_VERYCLEAN

    # Set up for patchable 'doxygen.css' file
    CONFIG_BUILD_HTML_ADJUST_NETSCAPE47X=YES
    export CONFIG_BUILD_HTML_ADJUST_NETSCAPE47X

    make $1

    # Set initial state to unpatched
    ./dox-unpatch.sh
}

DistDocTar ()
{
    echo ""
    echo "$PGMNAME: Creating documentation file '$PREFMTDOCSTAR.gz'"
    tar cf $PREFMTDOCSTAR doc
    rm -f $PREFMTDOCSTAR.gz
    gzip $PREFMTDOCSTAR
}

DistDocUnPrep ()
{
    echo ""
    echo "$PGMNAME: Removing temporary documentation set"
    make undox
    rm -f $CDSD
    mv ${CDSD}.ORIG $CDSD 
}

DistConfigPrep ()
{
    mv config $PGMTMP
}

DistConfigUnPrep ()
{
    mv $PGMTMP/config .
}

DistUnPrep ()
{
    echo ""
    echo "$PGMNAME: Restoring configuration from temp area"
    mv $PGMTMP/* .
    rmdir $PGMTMP
}

###################################################################
#
# EOF
