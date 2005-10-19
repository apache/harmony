#!/bin/sh
#
#!
# @file jni/src/harmony/generic/0.0/build.sh
#
# @brief Build Sample JNI subset, showing how to reference
#              the local native methods, but using the full
#              JNI mechanism.
#
# Compile Java source files into class files, but do @e not create
# a JAR file.  Then compile 'C' source files into object files and
# link into a target binary file.
#
# This binary really should be a shared object .so/.dll file, but that
# is a task for the project team.  This is only a quick sample.
#
# @see @link jni/src/harmony/generic/0.0/clean.sh
#            jni/src/harmony/generic/0.0/clean.sh@endlink
#
# @see @link jni/src/harmony/generic/0.0/common.sh
#            jni/src/harmony/generic/0.0/common.sh@endlink
#
# @see @link ./build.sh ./build.sh@endlink
#
# @see @link ./clean.sh ./clean.sh@endlink
#
# @see @link ./common.sh ./common.sh@endlink
#
#
# @todo  HARMONY-6-jni-build.sh-1 The linked binary should be
#        converted to a shared object .so/.dll file instead.
#
# @todo  HARMONY-6-jni-build.sh-1 A Windows .BAT version of this
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
. common.sh

###################################################################
#
# Construct output area.  Since Eclipse uses 'bin/src' for its
# output area, there should not be a conflict unless Eclipse does
# a 'make clean', which removes and rebuilds 'bin'.
#
if test ! -d $TARGET_DIRECTORY
then
    mkdir $TARGET_DIRECTORY
fi
if test ! -d $TARGET_DIRECTORY
then
    echo "$PGMNAME:  Cannot create '$TARGET_DIRECTORY' directory"
    exit 1
fi

rm -f `find $TARGET_DIRECTORY -name \*.class`
rm -f $TARGET_DIRECTORY/*.o $TMP_LIBRARY $TARGET_LIBRARY $TARGET_BINARY

###################################################################
#
# Build Java class files from source code (do @e not create JAR file)
#
javac -g -sourcepath src -classpath bin -d bin $SOURCES_JAVA
rc=$?

if test 0 -eq $rc
then
    jar cf $TMP_LIBRARY -C bin .
    rc1=$?
    mv $TMP_LIBRARY $TARGET_LIBRARY # Avoid partial JAR file inside self
    if test 0 -eq $rc
    then
        rc=$rc1
    fi
fi

###################################################################
#
# Build 'C' object files from source code
#

OBJECTS=""
for f in $SOURCES_C
do
    OBJECT=$TARGET_DIRECTORY/`expr "/${f:-.}" : \
                                   '\(.*[^/]\)/*$' : \
                                   '.*/\(..*\)' : \
                                   "\\(.*\\)c\$" `o

    OBJECTS="$OBJECTS ${OBJECT}"
    case $f in
        jvm/src/stdio.c)
            gcc $ALWAYS_OPTIONS \
                -o ${OBJECT} \
                -c $f
            ;;

        *)  gcc $ALWAYS_OPTIONS \
                $USUALLY_OPTIONS \
                -o ${OBJECT} \
                -c $f
            ;;
    esac
done

###################################################################
#
# Link final binary file from object code
#

gcc `cat ../../../../../config/config_opts_always.gccld` \
    $OBJECTS \
    -L ../../../../../libjvm/lib \
    -ljvm \
    -o $TARGET_BINARY
rc1=$?

if test 0 -eq $rc1
then
    rc=$rc1
fi

###################################################################
#
# Done.
#
exit $rc

#
# EOF
