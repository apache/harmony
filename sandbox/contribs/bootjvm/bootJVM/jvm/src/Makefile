#
#!
# @file ./jvm/src/Makefile
#
# @brief Build main JVM development area and/or JVM library archive
#
# (Please refer to @link ./roster.sh roster.sh@endlink as to why
# the <b>@@file</b> directive is constructed the way that it is.)
#
# Default to building the main JVM development area, but also build
# the JVM library archive, typically controlled by
# @link ./Makefile ./Makefile@endlink .  The default may be changed
# here in this file to build one or the other or both or neither
# by default.
#
# <ul>
# <li><b>make BOOTJVM=any-non-null-string</b> (Default) Build main
#                                             JVM development area (in
#                                             @e this area,
#                                             @c @b jvm/bin ).
#
# </li>
# <li><b>make LIBJVM=any-non-null-string</b>  Build JVM static library
#                                             archive file (in the
#                                             @c @b libjvm/lib area,
#                                             just like old
#                                             @c @b build.sh did and
#                                             how Eclipse still does).
# </li>
# </ul>
#
# For notes on why @c @b jvm and @c @b libjvm are now built with
# a single build script, please refer to
# @link ./Makefile ./Makefile@endlink .
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
# Decide which target(s) to build
#

MAKEBOOTJVM=0
MAKELIBJVM=0

ifdef BOOTJVM
MAKEBOOTJVM=1
endif

ifdef LIBJVM
MAKELIBJVM=1
endif

# Default to building main development area
# (May be set to build one or the other or both or neither by default.)
ifeq (0,$(MAKEBOOTJVM))
ifeq (0,$(MAKELIBJVM))
MAKEBOOTJVM=1
endif
endif

###################################################################
#
# Create the 'bootjvm' binary and/or the 'libjvm.a' library archive
#

# Set up directory knowledge and basic structure
TOPDIR=../..
include $(TOPDIR)/MakeSetup

# Roster of C source files and C header files
include $(CFGDIR)/config_roster_c.mak
include $(CFGDIR)/config_roster_h.mak

# Target to build, either main development area or library archive
ifeq (1,$(MAKEBOOTJVM))
BIN_TARGET=$(BINDIR)/bootjvm$(DOT_BIN_EXTENSION)
endif

ifeq (1,$(MAKELIBJVM))
LIB_TARGET=$(LIBDIR)/libjvm.$(AR_ARCHIVE)
LIBDIR=$(LIBJVM_LIBDIR)
endif

# Build rules
include $(TOPDIR)/MakeRules

# Default rule (one or the other sources may be an empty string here)
all:: $(BIN_TARGET) $(LIB_TARGET)

###################################################################
#
# EOF
