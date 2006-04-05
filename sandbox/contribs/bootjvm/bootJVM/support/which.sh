#!/bin/sh
#
#!
# @file support/which.sh
#
# @brief Generic replacement for <b><code>which</code></b>
#
# This script include file contains the logic needed
# to derive proper functionality of shell command
# <b><code>which</code></b> for systems that do not have it.
# This is useful in shell scripts that need to locate where
# an executable program of script is found in the PATH environment
# variable, an operation typically performed by either the shell
# built-in function, the executable shell script, or the binaray
# program <b><code>which</code></b>.  It should work on any flavor
# of BASH, Korn, and Bourne shells.  A stand-alone invocation does
# nothing, but is benign.
#
# Loaded where needed as:  <b><code>. support/which.sh</code></b>
#
# Invoke from a shell script as:
#<b><code>VARNAME=`which pgmname`</code></b>
#
#
# @section Control
#
# \$URL: https://svn.apache.org/repos/asf/incubator/harmony/enhanced/trunk/sandbox/contribs/bootjvm/bootJVM/support/which.sh $
#
# \$Id: which.sh 330881 2005-11-04 20:16:42Z dlydick $
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
# @version \$LastChangedRevision: 330881 $
#
# @date \$LastChangedDate: 2005-11-04 14:16:42 -0600 (Fri, 04 Nov 2005) $
#
# @author \$LastChangedBy: dlydick $
#
# @section Reference
#
#/ /* 
# (Use  #! and #/ with dox-filter.sh to fool Doxygen into
# parsing this non-source text file for the documentation set.
# Use the above open comment to force termination of parsing
# since it is not a Doxygen-style 'C' comment.)
#
#
###################################################################
#
# Inquire at which place in $PATH environment variable parm $1 is found
#
#
which()
{
(
set +xv
    # Ignore empty path or no parm 1 (which should _never_ happen)
    if test -n "$PATH" -a -n "$1"
    then
        WHICHPATH=`echo $PATH | sed 's/:/ /g'`

        for wp in $WHICHPATH
        do
            # If located, report where parm 1 is found, then quit
            if test -x $wp/$1
            then
                echo "$wp/$1"
                exit 0;
            fi
        done
    fi
    echo "$1:  Command not found." 1>&2
)
}

###################################################################
#
# Done.
#
# EOF
