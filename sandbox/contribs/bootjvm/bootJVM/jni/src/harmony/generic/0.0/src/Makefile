#
#!
# @file ./jni/src/harmony/generic/0.0/src/Makefile
#
# @brief Build Harmony generic JNI area
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
# (Use  #! and #/ with dox_filter.sh to fool Doxygen into
# parsing this non-source text file for the documentation set.
# Use the above open comment to force termination of parsing
# since it is not a Doxygen-style 'C' comment.)
#
#
###################################################################
#
# Create the 'bootjvm' binary and 'bootjvm.jar' class archive
#

# Set up directory knowledge and basic structure
TOPDIR=../../../../../..
include $(TOPDIR)/support/MakeSetup

# Roster of C source files and C header files
include $(CFGDIR)/config_roster_jni_c.mak
include $(CFGDIR)/config_roster_jni_h.mak

# Roster of Java source files
include $(CFGDIR)/config_roster_jni_java.mak

# Pick up JVM library
LLFLAGS:=-ljvm

# Targets to build
JAR_TARGET=$(BINDIR)/bootjvm.$(JAVA_ARCHIVE)
BIN_TARGET=$(BINDIR)/bootjvm$(DOT_BIN_EXTENSION)

# Build rules
include $(TOPDIR)/support/MakeRules

# Default rule
all:: $(BIN_TARGET) $(JAR_TARGET)

###################################################################
#
# EOF
