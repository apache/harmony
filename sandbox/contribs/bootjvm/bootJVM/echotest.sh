#!/bin/sh
#
#!
# @file ./echotest.sh
#
# @brief Generic replacement for <b><code>echo -n</code></b>
#
# This script include file contains the keyboard support
# needed to derive proper functionality of shell command
# <b><code>echo -n</code></b> for systems that do not have it.
# This is useful in shell scripts when requesting user input
# by way of a <b><code>read varname</code></b> command.
# It should work on any flavor of BASH, Korn, and Bourne shells.
# A stand-alone invocation does nothing, but is benign.
#
# Loaded where needed as:  <b><code>. ./echotest.sh</code></b>
#
# Invoke as:
#<b><code>$echon "Print a line without a final newline$echoc"</code></b>
#
#
# @section Control
#
# \$URL: https://svn.apache.org/path/name/echotest.sh $ \$Id: echotest.sh 0 09/28/2005 dlydick $
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
# Script setup:  Determine style of echo command w/o newline
#

rm -f /tmp/echotest1.$$.tmp

touch /tmp/echotest1.$$.tmp

echo -n "" > /tmp/echotest2.$$.tmp

cmp /tmp/echotest1.$$.tmp /tmp/echotest2.$$.tmp 2> /dev/null

if test 0 -eq $?
then
    echon="echo -n"
    echoc=""
else
    echo "\c" > /tmp/echotest3.$$.tmp

    cmp /tmp/echotest1.$$.tmp /tmp/echotest3.$$.tmp 2> /dev/null

    if test 0 -eq $?
    then
        echon="echo"
        echoc="\\c"
    else
        echon="echo"
        echoc=""
    fi
fi
rm -f /tmp/echotest?.$$.tmp
# echo "echon: $echon"
# echo "echoc: \\${echoc}"

###################################################################
#
# Done.  Use non-terminated $echon "text$echoc" from now on in
# main script...

#
# EOF
