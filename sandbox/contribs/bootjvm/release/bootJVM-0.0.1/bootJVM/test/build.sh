#!/bin/sh
#
#!
# @file test/build.sh
#
# @brief Build Java test programs for exercising various parts
# of the JVM
#
# Compile Java source files into class files and create a JAR file.
#
#
# @see @link test/clean.sh test/clean.sh@endlink
#
# @see @link test/common.sh test/common.sh@endlink
#
# @see @link ./build.sh ./build.sh@endlink
#
# @see @link ./clean.sh ./clean.sh@endlink
#
# @see @link ./common.sh ./common.sh@endlink
#
#
# @todo  HARMONY-6-test-build.sh-1 A Windows .BAT version of this
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

###################################################################
#
# Build Java class files from source code and create JAR file
#

if test -d bin
then
    chmod -R +w bin
fi

javac -g -sourcepath src -d bin $SOURCES
rc=$?

if test 0 -eq $rc
then
    if test -f $TMP_LIBRARY
    then
        chmod +w $TMP_LIBRARY
        rm -f $TMP_LIBRARY
    fi

    jar cf $TMP_LIBRARY -C bin .
    rc=$?

    if test -f $TARGET_LIBRARY
    then
        chmod +w $TARGET_LIBRARY
        rm -f $TARGET_LIBRARY
    fi
    mv $TMP_LIBRARY $TARGET_LIBRARY # Avoid partial JAR file inside self
fi

###################################################################
#
# Done.
#
exit $rc

#
# EOF
