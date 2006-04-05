#
#!
# @file ./test/src/Makefile
#
# @brief Build JVM test classes
#
# (Please refer to @link ./roster.sh roster.sh@endlink as to why
# the <b>@@file</b> directive is constructed the way that it is.)
#
# @see @link ./Makefile ./Makefile@endlink
#
# @see @link support/MakeSetup support/MakeSetup@endlink
#
# @see @link support/MakeRules support/MakeRules@endlink
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
# Create the 'boottest.jar' Java class library archive
#

# Set up directory knowledge and basic structure
TOPDIR=../..
include $(TOPDIR)/support/MakeSetup

# Roster of Java source files
include $(CFGDIR)/config_roster_test_java.mak

# Target to build
JAR_TARGET=$(BINDIR)/boottest.$(JAVA_ARCHIVE)

# Build rules
include $(TOPDIR)/support/MakeRules

# Default rule
all:: $(JAR_TARGET)

###################################################################
#
# EOF
