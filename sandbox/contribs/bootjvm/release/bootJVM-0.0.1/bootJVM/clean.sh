#!/bin/sh
#
#!
# @file /home/dlydick/harmony/bootjvm/bootJVM/clean.sh
#
# @brief Remove build of Boot JVM.
#
# Remove a build of a component of the project.  This either means to
# remove compiled object code and either remove a static library
# archive file or a linked binary, or to remove generated documentation.
#
# The main body of logic for this script is found in
# @link common.sh common.sh@endlink.
#
# @see @link ./build.sh ./build.sh@endlink
#
# @see @link ./common.sh ./common.sh@endlink
#
# @see @link jvm/clean.sh jvm/clean.sh@endlink
#
# @see @link libjvm/clean.sh libjvm/clean.sh@endlink
#
# @see @link main/clean.sh main/clean.sh@endlink
#
# @see @link test/clean.sh test/clean.sh@endlink
#
# @see @link jni/src/harmony/generic/0.0/clean.sh
#            jni/src/harmony/generic/0.0/clean.sh@endlink
#
#
# @todo  HARMONY-6-clean.sh-1 A Windows .BAT version of this script
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
