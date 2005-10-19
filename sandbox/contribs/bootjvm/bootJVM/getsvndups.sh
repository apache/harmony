#!/bin/sh
#!
# @file ./getsvndups.sh
#
# @brief Report list of all non-unique versions of all source files 
# compiled into a binary.
#
# Generate report of multiple versions of source files that have been
# compiled and are found in a compiled object file or a linked binary.
# A sorted, unique list of URL-based file names is displayed, showing
# @e only names of files that have multiple versions present in the
# binary.  This is perfectly acceptable for linked binaries, but
# is always problematic for static libraries and dynamic libraries.
#
# To report the versions of all source files represented in the
# binary, use @link ./getsvndata.sh getsvndata.sh@endlink.
#
# The macro @link #ARCH_COPYRIGHT_APACHE() ARCH_COPYRIGHT_APACHE@endlink
# generates the appropriate data from the expansion of SubVersion (SVN)
# keywords that are stored in a static string in each compiled object
# file.  This data is promoted at link time to be stored in the
# linked binary explicitly for use by this reporting script.
#
# @see getsvndata.sh
#
# @todo A Windows .BAT version of this script needs to be written
#
#
# @section Control
#
# \$URL$ \$Id$
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
########################################################################
#
# Check script syntax
#
if test 0 -eq $#
then
    echo "Usage:  $0 object_or_binary_filename [...]"
    exit 1
fi
 
########################################################################
#
# Scan requested file(s) and report SVN 
#
trap "" 1 2 3 15

DOLLAR='$'
SEARCHFOR="^\\\$URL: |^\\\$HeadURL: "

TMPFILE1=${TMPDIR:-/tmp}/tmp.1.$$
TMPFILE2=${TMPDIR:-/tmp}/tmp.2.$$
rm -f $TMPFILE1 $TMPFILE2

strings $* | egrep "$SEARCHFOR" | sort -u | cut -f2,6 -d' ' > $TMPFILE1

cut -f1 -d' ' $TMPFILE1 > $TMPFILE2

cut -f1 -d' ' $TMPFILE1 | sort -u | diff $TMPFILE2 - | cut -f2 -d' '

rm -f $TMPFILE1 $TMPFILE2

########################################################################
#
# EOF
