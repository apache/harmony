#!/bin/sh
#
#!
# @file ./svnstat.sh
#
# @brief Per doxygen @b config.html recommendation for
# FILE_VERSION_FILTER
#
# @todo HARMONY-6-svnstat.sh-1 The @c @b sed(1) command as documented
#       is not valid.  See also @@bug entry HARMONY-6-svnstat.sh-1001.
#
# @verbatim
#
# svn stat -v $1 | \\
#     sed -n 's/^[ A-Z?\*|!]\{1,15\}/r/;s/ \{1,15\}/\/r/;s/ ./p'
#
# @endverbatim
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
# @bug HARMONY-6-svnstat.sh-1001 The @c @b sed(1) string recommended in
#      the Doxygen narrative needs fixing.  It is reported here
#      verbatim, but it is possible that the number of backslash
#      characters is not correct.
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
# Run recommended SVN command (buggy, needs fixing)
#
svn stat -v $1 | \
    sed -n 's/^[ A-Z?\*|!]\{1,15\}/r/;s/ \{1,15\}/\/r/;s/ ./p'
#
# EOF
