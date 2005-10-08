#!/bin/sh
#
#!
# @file libjvm/build.sh
#
# @brief Build JVM code as a static library
#
# @see @link libjvm/clean.sh libjvm/clean.sh@endlink
#
# @see @link libjvm/common.sh libjvm/common.sh@endlink
#
# @see @link ./build.sh ./build.sh@endlink
#
# @see @link ./clean.sh ./clean.sh@endlink
#
# @see @link ./common.sh ./common.sh@endlink
#
#
# @verbatim
#
#   $ gcc `cat ../config/config_opts_always.gcc` \
#         `cat ../config/config_opts_usually.gcc` -c filename.c ...
#
# @endverbatim
#
# Notice that this script may be run instead of or as well as an
# Eclipse build.  The only difference is where the object files
# are stored.
#
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
chmod -w $0

. common.sh

###################################################################
#
# Construct output area.  Since Eclipse uses 'bin/src' for its
# output area, there should not be a conflict unless Eclipse does
# a 'make clean', which removes and rebuilds 'lib'.
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
rm -f $TARGET_DIRECTORY/*.o $TARGET_LIBRARY

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

    if test "src/main.c" = "$f"
    then
        : # Skip main() since a library archive is the target
    else
        OBJECTS="$OBJECTS ${OBJECT}"
    fi

    case $f in
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

###################################################################
#
# Build static library archive from objects
#

ar r $TARGET_LIBRARY $OBJECTS 
rc=$?


###################################################################
#
# Done.
#
exit $rc

#
# EOF
