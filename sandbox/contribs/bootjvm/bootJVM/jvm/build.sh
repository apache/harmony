#!/bin/sh
#
#!
# @file ./jvm/build.sh
#
# @brief Build main JVM development area
#
# @see @link jvm/clean.sh jvm/clean.sh@endlink
#
# @see @link jvm/common.sh jvm/common.sh@endlink
#
# @see @link ./build.sh ./build.sh@endlink
#
# @see @link ./clean.sh ./clean.sh@endlink
#
# @see @link ./common.sh ./common.sh@endlink
#
# @internal Notice that 'jvm/*.sh' have the relative path prefix './'
# attached to the front of the @@file directive.  This is to avoid an
# interesting sensitivity in Doxygen that got confused between
# 'jvm/filename.sh' and 'libjvm/filename.sh' and failed to produce
# the "File List" entry for each 'jvm/*.sh' build script.  By marking
# them './jvm/build.sh' et al, this behavior went away.  This same
# comment may be found in @link ./config.sh config.sh@endlink
#
#
# @todo  HARMONY-6-jvm-build.sh-1 A Windows .BAT version of this
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
rm -f $TARGET_DIRECTORY/*.o $TARGET_BINARY

###################################################################
#
# Build binary from source code
#

OBJECTS=""
for f in $SOURCES
do
    OBJECT=$TARGET_DIRECTORY/`expr "/${f:-.}" : \
                                   '\(.*[^/]\)/*$' : \
                                   '.*/\(..*\)' : \
                                   "\\(.*\\)c\$" `o

    OBJECTS="$OBJECTS ${OBJECT}"

    case $f in
        src/portable_libc.c | \
        src/portable_libm.c | \
        src/portable_setjmp.c | \
        src/stdio.c)
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

gcc `cat ../config/config_opts_always.gccld` $OBJECTS -o $TARGET_BINARY
rc=$?


###################################################################
#
# Done.
#
exit $rc

#
# EOF
