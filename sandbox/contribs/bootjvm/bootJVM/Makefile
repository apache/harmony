#
#!
# @file /home/dlydick/harmony/bootjvm/bootJVM/Makefile
#
# @brief Build entire code base
#
# Top level @c @b Makefile for compiling the whole code base.
#
# (Please refer to @link ./roster.sh roster.sh@endlink as to why
# the <b>@@file</b> directive is constructed the way that it is.)
#
# In order to support both the creation of the JVM static library
# archive and the Eclipse project files, some adjustments have been
# made to the way the @c @b jvm and @c @b libjvm trees are built.
# Since @c @b libjvm/src is a symbolic link to the @c @b jvm/src
# directory, @link jvm/src/Makefile jvm/src/Makefile@endlink has
# been set up to compile both types of targets, the linked binary
# of its original build script and the static library from
# @c @b libjvm/src directory.  (The former build scripts were
# placed above the @c @b src level and so could distinguish
# between them without any other mechanism.)  That
# @link jvm/src/Makefile jvm/src/Makefile@endlink may be used
# to compile for one or the other or both directories.
#
# This script replaces the former @c @b build.sh and @c @b clean.sh
# scripts, which were implemented merely as a convenience toward
# getting the initial code base written.
#
#
# @see @link ./MakeSetup ./MakeSetup@endlink
#
# @see @link ./MakeRules ./MakeRules@endlink
#
# @see @link jvm/src/Makefile jvm/src/Makefile@endlink
#
# @see @link main/src/Makefile main/src/Makefile@endlink
#
# @see @link test/src/Makefile test/src/Makefile@endlink
#
# @see @link jni/src/harmony/generic/0.0/src/Makefile jni/src/harmony/generic/0.0/src/Makefile@endlink
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
# Makefile setup
#

#
# The default rule for the top level is 'cfg' instead of 'all'
# (that all other Makefiles use as the default rule).
#
cfg::

TOPDIR=.
include $(TOPDIR)/MakeSetup

#
# Disable subdirectory warnings if desired
#
NPD=--no-print-directory
#NPD=

###################################################################
#
# Target specifications.
#
OPTLIST1=cfg | all | clean | veryclean

cfg::
ifeq (1,$(CONFIG_BUILD_JVM))
	$(MAKE) $(NPD) -C jvm/src BOOTJVM=non-null all
endif
ifeq (1,$(CONFIG_BUILD_LIB))
	$(MAKE) $(NPD) -C jvm/src LIBJVM=non-null all
endif
ifeq (1,$(CONFIG_BUILD_MAIN))
	$(MAKE) $(NPD) -C main/src all
endif
ifeq (1,$(CONFIG_BUILD_JNI))
	$(MAKE) $(NPD) -C jni/src/harmony/generic/0.0/src all
endif
ifeq (1,$(CONFIG_BUILD_TEST))
	$(MAKE) $(NPD) -C test/src all
endif
ifeq (1,$(CONFIG_BUILD_DOX))
	$(MAKE) $(NPD) dox
endif

all clean veryclean::
	-$(MAKE) $(NPD) -C jvm/src BOOTJVM=non-null LIBJVM=non-null $@
	-$(MAKE) $(NPD) -C main/src $@
	-$(MAKE) $(NPD) -C jni/src/harmony/generic/0.0/src $@
	-$(MAKE) $(NPD) -C test/src $@

all::
	-$(MAKE) $(NPD) dox

ifeq (1,$(CONFIG_BUILD_DOX))
clean::
	-$(MAKE) $(NPD) undox
endif

veryclean::
	-$(MAKE) $(NPD) undox



OPTLIST2=jvm | libjvm | main | jni | test

jvm::
	$(MAKE) $(NPD) -C jvm/src BOOTJVM=non-null all
libjvm::
	$(MAKE) $(NPD) -C jvm/src LIBJVM=non-null all
main::
	$(MAKE) $(NPD) -C main/src all
jni::
	$(MAKE) $(NPD) -C jni/src/harmony/generic/0.0/src all
test::
	$(MAKE) $(NPD) -C test/src all



OPTLIST3=dox | undox | help

include config/config_dox_setup.mak

MAKEDOX:=
ifeq (YES,$(GENERATE_HTML))
MAKEDOX:=$(MAKEDOX) doc/html
endif
ifeq (YES,$(GENERATE_LATEX))
MAKEDOX:=$(MAKEDOX) doc/latex
endif
ifeq (YES,$(GENERATE_RTF))
MAKEDOX:=$(MAKEDOX) doc/rtf
endif
ifeq (YES,$(GENERATE_MAN))
MAKEDOX:=$(MAKEDOX) doc/man
endif
ifeq (YES,$(GENERATE_XML))
MAKEDOX:=$(MAKEDOX) doc/xml
endif

.PHONY:: dox

ifeq (,$(MAKEDOX))

# Nothing to do for documentation
dox::

else
# To unconditionally build documentation, simply run 'dox.sh' by itself
# or run 'make clean' first.
#

dox:: ./doc

./doc:: $(MAKEDOX)
	dox.sh

$(MAKEDOX)::

endif

undox::
	undox.sh

roster::
	roster.sh

help::
	@echo ""
	@echo "Usage:  $(MAKE) $(OPTLIST1) |"
	@echo "             $(OPTLIST2) |"
	@echo "             $(OPTLIST3)"
	@echo ""
	@echo "where  cfg       Build what was configured by 'config.sh'"
	@echo "       all       Build everything (*)"
	@echo "       clean     Smart cleanup (*)"
	@echo "       veryclean Unconditional cleanup (*)"
	@echo ""
	@echo "       jvm       Build main JVM development area"
	@echo "       libjvm    Build static JVM library"
	@echo "       main      Build JVM binary from main() and static library"
	@echo "       test      Build test classes for JVM development"
	@echo "       jni       Build JNI classes and shared object library"
	@echo "                   (but currently the same binary as 'main')"
	@echo ""
	@echo "       dox       Build documentation (*)"
	@echo "       undox     Clean documentation (*)"
	@echo "       roster    Change the file roster (*)"
	@echo "       help      Display this message"
	@echo ""
	@echo " (*) Works at any level, not just here at the top level"
	@echo ""

###################################################################
#
# EOF
