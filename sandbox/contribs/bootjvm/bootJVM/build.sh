#!/bin/sh
#
#!
# @file /home/dlydick/harmony/bootjvm/bootJVM/build.sh
#
# @brief Build Boot JVM.
#
# Build a component of the project.  This either means to
# compile source code and either archive or link it, or
# to generate documentation.
#
# The main body of logic for this script is found in
# @link common.sh common.sh@endlink.
#
# @see @link ./clean.sh ./clean.sh@endlink
#
# @see @link ./common.sh ./common.sh@endlink
#
# @see @link jvm/build.sh jvm/build.sh@endlink
#
# @see @link libjvm/build.sh libjvm/build.sh@endlink
#
# @see @link main/build.sh main/build.sh@endlink
#
# @see @link test/build.sh test/build.sh@endlink
#
# @see @link jni/src/harmony/generic/0.0/build.sh
#            jni/src/harmony/generic/0.0/build.sh@endlink
#
#
# @todo  HARMONY-6-build.sh-1 The entire project should also have
#        'gmake' support.  It would be a simple thing to add/change the
#        'config/*' roster files with @link config.sh config.sh@endlink
#        to support this.
#
# @todo  HARMONY-6-build-sh-2  A Windows .BAT version of this script
#        needs to be written
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
# Invoke common code.
#
. common.sh

###################################################################
#
# Done.
#

#
# EOF
