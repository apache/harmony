#!/bin/sh
#
#!
# @file main/build.sh
#
# @brief Build sample @link main/src/main.c main()@endlink program
#        from source and link with the JVM static library.
#
#
# @see @link main/clean.sh main/clean.sh@endlink
#
# @see @link main/common.sh main/common.sh@endlink
#
# @see @link ./build.sh ./build.sh@endlink
#
# @see @link ./clean.sh ./clean.sh@endlink
#
# @see @link ./common.sh ./common.sh@endlink
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
# Build objects from source code
#

OBJECTS=""
for f in $SOURCES
do
    OBJECT=$TARGET_DIRECTORY/`expr "/${f:-.}" : \
                                   '\(.*[^/]\)/*$' : \
                                   '.*/\(..*\)' : \
                                   "\\(.*\\)c\$" `o


    case $f in
        # This item is actually 'jvm/src/main.c', which is where
        # the symbolic link 'main/src/main.c' actually points.
        # The latter entry is actually listed in the header file
        # roster, 'config/config_roster_h.dox' so it gets reported
        # properly for documentation purposes, but that is its only
        # purpose in being there.  Since the higher level directory
        # of 'jvm/' gets stripped off, the symbolic link provided
        # an easy workaround to avoid path name ambiguity and confusion.

        src/main.c)
            # Cheat on include path and use 'jvm' directory'
            OBJECTS="$OBJECTS ${OBJECT}"
            gcc $ALWAYS_OPTIONS \
                $USUALLY_OPTIONS \
                -I../jvm/src \
                -o ${OBJECT} \
                -c $f
            ;;

        *)  ;;  # Only building one sample object
    esac
done

###################################################################
#
# Build binary from objects
#
gcc `cat ../config/config_opts_always.gccld` \
    $OBJECTS \
    -L ../libjvm/lib \
    -ljvm \
    -o $TARGET_BINARY
rc=$?


###################################################################
#
# Done.
#
exit $rc

#
# EOF
