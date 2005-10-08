#!/bin/sh
#
#!
# @file /home/dlydick/harmony/bootJVM/common.sh
#
# @brief Common code for @link ./build.sh build.sh@endlink and
# @link ./clean.sh clean.sh@endlink and
# @link ./dist-src.sh dist-src.sh@endlink and
# @link ./dist-bin.sh dist-bin.sh@endlink and
# @link ./dist-doc.sh dist-doc.sh@endlink.
#
# This script is common to @link ./build.sh build.sh@endlink and
# @link ./clean.sh clean.sh@endlink and is not designed to do
# anything on its own.
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
# Each of these directories contains a @b build.sh script.  In addition
# to these, an Eclipse project file is available in each for use with
# the Eclipse C/C++ plugin, so this entire directory tree may be
# imported wholesale into an Eclipse workspace and used without
# changes.  (Eclipse 3.0.2 generated these files.)  Notice that
# the Eclipse setup does not build the documentation set.  This must
# be done manually with the top-level 'build.sh dox'.
#
# @attention For Eclipse uses, be aware that the configuration
# options stored by @link config.sh config.sh@endlink into
# <b><code>config/config_*.gcc*</code></b> are @e not directly
# available to Eclipse and @e must be manually entered there
# after they are established by @link ./config.sh config.sh@endlink.
# They should be entered in the project build parameters for
# C/C++ in the miscellaneous parameters section.  For example,
# @b -m32 and @b -m64 .  For a command line GCC invocation,
# the following is a convenient way to incorporate the options:
#
# @verbatim
#
#   $ gcc `cat ../config/config_opts_always.gcc` \
#         `cat ../config/config_opts_usually.gcc` -c filename.c ...
#
# @endverbatim
#
# Notice that this script may be run instead of or as well as an
# Eclipse build.  There is only a slight difference as to
# where the compiled object files are stored, but the binaries
# and library archives are stored in the same place for both
# methods.
#
#
# @see @link ./build.sh ./build.sh@endlink
#
# @see @link ./clean.sh ./clean.sh@endlink
#
# @see @link jvm/build.sh jvm/build.sh@endlink
#
# @see @link libjvm/build.sh libjvm/build.sh@endlink
#
# @see @link main/build.sh main/build.sh@endlink
#
# @see @link test/build.sh test/build.sh@endlink
#
# @see @link jni/src/harmony/generic/0.0/build.sh
#            jni/src/harmony/generic/0.0/build.sh@endlink
#
# @see @link jvm/clean.sh jvm/clean.sh@endlink
#
# @see @link libjvm/clean.sh libjvm/clean.sh@endlink
#
# @see @link main/clean.sh main/clean.sh@endlink
#
# @see @link test/clean.sh test/clean.sh@endlink
#
# @see @link jni/src/harmony/generic/0.0/clean.sh
#            jni/src/harmony/generic/0.0/clean.sh@endlink
#
# @see @link jvm/common.sh jvm/common.sh@endlink
#
# @see @link libjvm/common.sh libjvm/common.sh@endlink
#
# @see @link main/common.sh main/common.sh@endlink
#
# @see @link test/common.sh test/common.sh@endlink
#
# @see @link jni/src/harmony/generic/0.0/common.sh
#            jni/src/harmony/generic/0.0/common.sh@endlink
#
#
# @todo  A Windows .BAT version of this script needs to be written
#
#
# @section Control
#
# \$URL: https://svn.apache.org/path/name/build.sh $ \$Id: build.sh 0 09/28/2005 dlydick $
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
# @version \$LastChangedRevision: 0 $
#
# @date \$LastChangedDate: 09/28/2005 $
#
# @author \$LastChangedBy: dlydick $
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
# Check compilation options
#
Usage ()
{
    (
   OPTLIST="{cfg | all | jvm | libjvm | main | test | jni | dox | help}"
        echo "Usage:  $0 $OPTLIST"
        echo ""
echo "where  cfg   Build what was configured by the 'config.sh' script"
        echo "       all    Build/clean everything"
        echo "       jvm    Build/clean main JVM development area"
        echo "       libjvm Build/clean static JVM library"
        echo "       main   Build/clean JVM binary"
       echo "       test   Build/clean test classes for JVM development"
        echo "       jni    Build/clean JNI library and sample binary"
        echo "       dox    Build/clean documentation"
        echo "       help   Display this message"
        echo ""
    ) 1>&2
    exit 10
}

BUILD_ALL=0
BUILD_JVM=0
BUILD_LIB=0
BUILD_MAIN=0
BUILD_TEST=0
BUILD_JNI=0
BUILD_DOX=0

BUILD_DIST=0

case $1 in
    cfg)    if test 1 -eq $CONFIG_BUILD_ALLCODE;  then BUILD_ALL=1;  fi
            if test 1 -eq $CONFIG_BUILD_JVM;      then BUILD_JVM=1;  fi
            if test 1 -eq $CONFIG_BUILD_LIB;      then BUILD_LIB=1;  fi
            if test 1 -eq $CONFIG_BUILD_MAIN;     then BUILD_MAIN=1; fi
            if test 1 -eq $CONFIG_BUILD_TEST;     then BUILD_TEST=1; fi
            if test 1 -eq $CONFIG_BUILD_JNI;      then BUILD_JNI=1;  fi
            if test 1 -eq $CONFIG_BUILD_DOX;      then BUILD_DOX=1;  fi
            ;;
    all)    BUILD_ALL=1
            BUILD_JVM=1
            BUILD_LIB=1
            BUILD_MAIN=1
            BUILD_TEST=1
            BUILD_JNI=1
            BUILD_DOX=1
            ;;
    jvm)    BUILD_JVM=1;;
    libjvm) BUILD_LIB=1;;
    main)   BUILD_MAIN=1;;
    test)   BUILD_TEST=1;;
    jni)    BUILD_JNI=1;;
    dox)    BUILD_DOX=1;;

    *)      # These scripts ave their own Usage tests
            case $PGMNAME in
                dist-src.sh | dist-bin.sh | dist-doc.sh)
                    # CONFIG_RELEASE_LEVEL contains release level,
                    # and no other parameterization is needed
                    BUILD_DIST=1
                    ;;
                *)  if test -z "$1"
                    then
                        $0 cfg
                        exit $?
                    fi
                    Usage
                    ;;
            esac
            ;;
esac

###################################################################
#
# Build binary from source code
#
rc=0

# All scripts except distribution scripts use this logic:
if test 0 -eq $BUILD_DIST
then
    if test 1 -eq $BUILD_JVM
    then
        echo "$PGMNAME jvm"
        cd jvm
        $PGMNAME
        rc=$?
        cd ..
    fi

    if test 1 -eq $BUILD_LIB
    then
        echo "$PGMNAME libjvm"
        cd libjvm
        $PGMNAME
        rc1=$?
        if test 0 -eq $rc; then rc=$rc1; fi
        cd ..
    fi

    if test 1 -eq $BUILD_MAIN
    then
        echo "$PGMNAME main"
        cd main
        $PGMNAME
        rc1=$?
        if test 0 -eq $rc; then rc=$rc1; fi
        cd ..
    fi

    if test 1 -eq $BUILD_TEST
    then
        echo "$PGMNAME test"
        cd test
        $PGMNAME
        rc1=$?
        if test 0 -eq $rc; then rc=$rc1; fi
        cd ..
    fi

    if test 1 -eq $BUILD_JNI
    then
        echo "$PGMNAME jni/src/harmony/generic/0.0"
        cd jni/src/harmony/generic/0.0
        $PGMNAME
        rc1=$?
        if test 0 -eq $rc; then rc=$rc1; fi
        cd ../../../../..
    fi

    if test 1 -eq $BUILD_DOX
    then
        echo "$PGMNAME dox"
        case $PGMNAME in
            build.sh) dox.sh;;
            clean.sh) undox.sh;;
        esac
        rc1=$?
        if test 0 -eq $rc; then rc=$rc1; fi
    fi

    exit $rc
fi

###################################################################
#
# Preparation for 'dist-src.sh' and 'dist-bin.sh' and 'dist-doc.sh'
#
# Strip out _all_ output, rebuild documentation in _all_ formats,
# archive it, and then delete it.  Package the result in final
# 'tar' file.
#

# Names of documentation tar file and source distribution tar file
PREFMTDOCSTAR="bootJVM-docs.tar"

# Can't add release level unless 'config.sh' can locate release level:
#PREFMTDOCSTAR="bootJVM-docs-$CONFIG_RELEASE_LEVEL.tar"

DISTBINTAR="bootJVM-bin-$CONFIG_RELEASE_LEVEL.tar"
DISTDOCTAR="bootJVM-doc-$CONFIG_RELEASE_LEVEL.tar"
DISTSRCTAR="bootJVM-src-$CONFIG_RELEASE_LEVEL.tar"

# Transient storage for keeping existing old configuration
PGMTMP=../tmp.$$.$PGMNAME


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
        chmod +w doc.ORIG
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
    echo ""
    echo "$PGMNAME: Creating documentation set in _all_ formats"

    # Keep old doc cfg file, generate all-inclusive temporary one
    rm -f ${CDSD}.ORIG
    mv $CDSD ${CDSD}.ORIG
    (
        echo "GENERATE_HTML=YES"
        echo "GENERATE_LATEX=YES"
        echo "GENERATE_RTF=YES"
        echo "GENERATE_MAN=YES"
        echo "GENERATE_XML=YES"
    ) > $CDSD
}


DistTargetBuild ()
{
    # Make SURE the output area is clean
    SUPPRESS_DOXYGEN_VERYCLEAN=
    export SUPPRESS_DOXYGEN_VERYCLEAN

    # Set up for patchable 'doxygen.css' file
    CONFIG_BUILD_HTML_ADJUST_NETSCAPE47X=YES
    export CONFIG_BUILD_HTML_ADJUST_NETSCAPE47X

    ./build.sh $1

    # Set initial state to unpatched
    ./doxunpatch.sh
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
    ./clean.sh dox
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
