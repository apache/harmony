#!/bin/sh
#!
# @file ./dox-patch.sh
#
# @brief Install CSS patch in HTML documentation that supports
# NetScape 4.7x HTML browser.
#
# A patch is needed to work around the cascading style sheet issue in
#
#     <b>&lt;pre class="fragment"&gt;</b>
#
# directives in Doxygen HTML output so that old NetScape 4.7x browsers
# do not parse this to eliminate newline characters in @b @@verbatim
# blocks, @b &lt;code&gt; blocks, source file listings, etc.
#
# This only is effective if @b CONFIG_BUILD_HTML_ADJUST_NETSCAPE47X
# was configured by @link ./config.sh config.sh@endlink.  Otherwise
# it is meaningless and the request is silently ignored.
#
# When configured, the cascading style sheet @c @b doc/html/doxygen.css
# has two copies made when @link ./dox.sh dox.sh@endlink is run.
# A symbolic link by the original name points to the patched copy
# originally. @link ./dox-unpatch.sh dox-unpatch@endlink can use it
# to revert back to the unpatched copy.  This script moves the symbolic
# link to the patched copy that suppresses processing that causes
# the problem.  The down side of this approach is that other formatting
# may not be quite right, but at least the blocks in question are
# readable.
#
# @see dox-unpatch.sh
#
# @todo  HARMONY-6-dox-patch.sh-1 A Windows .BAT version of this
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
# (Use  #! and #/ with dox-filter.sh to fool Doxygen into
# parsing this non-source text file for the documentation set.
# Use the above open comment to force termination of parsing
# since it is not a Doxygen-style 'C' comment.)
#
#
########################################################################
#
# Script setup
#
. support/dox-common.sh
. config/config_build_steps.sh

########################################################################
#
# Check if configured and move symbolic link if so.
#
if test "YES" != "$CONFIG_BUILD_HTML_ADJUST_NETSCAPE47X"
then
    exit 0 # Silently ignore the request
else
    # Only meaningful if "GENERATE_HTML=YES" in directive file

    if test -d $OUTPUT_DIRECTORY/$HTML_OUTPUT
    then
        rm -f $CSS_FILE

        ln -s $CSS_FILE_PATCHED_NAME $CSS_FILE
    fi
fi


########################################################################
#
# EOF
