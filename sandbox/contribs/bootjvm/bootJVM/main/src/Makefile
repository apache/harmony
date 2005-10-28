#
#!
# @file ./main/src/Makefile
#
# @brief Build JVM using main program entry point and library archive
#
# (Please refer to @link ./roster.sh roster.sh@endlink as to why
# the <b>@@file</b> directive is constructed the way that it is.)
#
# @see @link ./Makefile ./Makefile@endlink
#
# @see @link ./MakeSetup ./MakeSetup@endlink
#
# @see @link ./MakeRules ./MakeRules@endlink
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
# Create the 'bootjvm' binary
#

# Set up directory knowledge and basic structure
TOPDIR=../..
include $(TOPDIR)/MakeSetup

# Roster of C source files and C header files
C_SOURCES=main.c
BIN_OBJECTS=$(BINDIR)/main.o

# Target to build
BIN_TARGET=$(BINDIR)/bootjvm$(DOT_BIN_EXTENSION)

# Additional include paths, before main list
PRE_INCLUDE_PATHS:=-I$(TOPDIR)/jvm/src

# Pick up JVM library
LLFLAGS:=-ljvm

# Build rules
include $(TOPDIR)/MakeRules

# Default rule
all:: $(BIN_TARGET)

###################################################################
#
# EOF
