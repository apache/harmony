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
# @see @link support/MakeSetup support/MakeSetup@endlink
#
# @see @link support/MakeRules support/MakeRules@endlink
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
# @todo HARMONY-6-Makefile-1 Get $(DOCDIR) dependencies working so
#       that if no source files have changed (are newer than either
#       $(DOCDIR) or $(MAKEDOX) subdirectories), then do not rebuild
#       documentation.
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
# Makefile setup
#

# Targets that are command line parms, but not file or directory names:
.PHONY:: cfg all clean veryclean jvm libjvm main jni test dox undox help

#
# The default rule for the top level is 'cfg' instead of 'all'
# (that all other Makefiles use as the default rule).
#
cfg::

TOPDIR=.
include $(TOPDIR)/support/MakeSetup

#
# Disable subdirectory warnings if desired
#
NPD=--no-print-directory
#NPD=

###################################################################
#
# Target specifications.
#
OPTLIST1=cfg

ifeq (1,$(CONFIG_BUILD_JVM))
cfg::
	$(MAKE) $(NPD) -C jvm/src BOOTJVM=defined all
clean::
	$(MAKE) $(NPD) -C jvm/src BOOTJVM=defined clean
endif
ifeq (1,$(CONFIG_BUILD_LIB))
cfg::
	$(MAKE) $(NPD) -C jvm/src LIBJVM=defined all
clean::
	$(MAKE) $(NPD) -C jvm/src LIBJVM=defined clean
endif
ifeq (1,$(CONFIG_BUILD_MAIN))
cfg::
	$(MAKE) $(NPD) -C main/src all
clean::
	$(MAKE) $(NPD) -C main/src clean
endif
ifeq (1,$(CONFIG_BUILD_JNI))
cfg::
	$(MAKE) $(NPD) -C jni/src/harmony/generic/0.0/src all
clean::
	$(MAKE) $(NPD) -C jni/src/harmony/generic/0.0/src clean
endif
ifeq (1,$(CONFIG_BUILD_TEST))
cfg::
	$(MAKE) $(NPD) -C test/src all
clean::
	$(MAKE) $(NPD) -C test/src clean
endif
ifeq (1,$(CONFIG_BUILD_DOX))
cfg::
	$(MAKE) $(NPD) dox
clean::
	$(MAKE) $(NPD) undox
endif

OPTLIST2=all | clean | veryclean
all veryclean::
	-$(MAKE) $(NPD) -C jvm/src BOOTJVM=defined LIBJVM=defined $@
	-$(MAKE) $(NPD) -C main/src $@
	-$(MAKE) $(NPD) -C jni/src/harmony/generic/0.0/src $@
	-$(MAKE) $(NPD) -C test/src $@

all::
	-$(MAKE) $(NPD) dox

veryclean::
	-$(MAKE) $(NPD) undox


OPTLIST3=jvm | libjvm | main | jni | test

jvm::
	$(MAKE) $(NPD) -C jvm/src BOOTJVM=defined all
libjvm::
	$(MAKE) $(NPD) -C jvm/src LIBJVM=defined all
main::
	$(MAKE) $(NPD) -C main/src all
jni::
	$(MAKE) $(NPD) -C jni/src/harmony/generic/0.0/src all
test::
	$(MAKE) $(NPD) -C test/src all



OPTLIST4=dox | undox | help

include config/config_dox_setup.mak

# Build documentation only if at least one format is configured
MAKEDOX:=
ifeq (YES,$(GENERATE_HTML))
MAKEDOX:=$(MAKEDOX) $(DOCDIR)/html
endif
ifeq (YES,$(GENERATE_LATEX))
MAKEDOX:=$(MAKEDOX) $(DOCDIR)/latex
endif
ifeq (YES,$(GENERATE_RTF))
MAKEDOX:=$(MAKEDOX) $(DOCDIR)/rtf
endif
ifeq (YES,$(GENERATE_MAN))
MAKEDOX:=$(MAKEDOX) $(DOCDIR)/man
endif
ifeq (YES,$(GENERATE_XML))
MAKEDOX:=$(MAKEDOX) $(DOCDIR)/xml
endif

ifeq (,$(MAKEDOX))

# Nothing to do for documentation
dox::

else # MAKEDOX

# List all sources (run 'make roster' if update is needed)
DOX_SOURCES:=
-include config/config_roster_c.mak
DOX_SOURCES:=$(DOX_SOURCES) $(addprefix jvm/src/,$(C_SOURCES))
-include config/config_roster_h.mak
DOX_SOURCES:=$(DOX_SOURCES) $(addprefix jvm/src/,$(C_HEADERS))
-include config/config_roster_jni_c.mak
DOX_SOURCES:=$(DOX_SOURCES) $(addprefix jni/src/harmony/generic/0.0/src/,$(C_SOURCES))
-include config/config_roster_jni_h.mak
DOX_SOURCES:=$(DOX_SOURCES) $(addprefix jni/src/harmony/generic/0.0/src/,$(C_HEADERS))
-include config/config_roster_jni_java.mak
DOX_SOURCES:=$(DOX_SOURCES) $(addprefix jni/src/harmony/generic/0.0/src/,$(JAVA_SOURCES))
-include config/config_roster_test_java.mak
DOX_SOURCES:=$(DOX_SOURCES) $(addprefix jvm/src/,$(JAVA_SOURCES))

# Clear out variables from include files
C_SOURCES:=
C_HEADERS:=
JAVA_SOURCES:=

#
# To unconditionally build documentation, simply
# run 'dox.sh' by itself or run 'make veryclean' first.
#
# Otherwise, a simple 'make' will _only_ build documentation
# if it was configured by 'config.sh', and then only the
# output formats configured.  (The output formats will
# _still_ have to be configured when running the
# stand-alone script.)
#

.PHONY:: $(DOX_SOURCES)

dox:: $(DOCDIR)

$(DOCDIR):: $(DOX_SOURCES)
	dox.sh

endif # MAKEDOX

undox::
	undox.sh

roster::
	roster.sh

help::
	@echo ""
	@echo "Usage:  $(MAKE) $(OPTLIST2) |"
	@echo "             $(OPTLIST1) | $(OPTLIST3) |"
	@echo "             $(OPTLIST4)"
	@echo ""
	@echo "where  all       Build everything (default rule)"
	@echo "       dox       Build documentation (only)"
	@echo "       clean     Smart cleanup"
	@echo "       veryclean Unconditional cleanup"
	@echo ""
	@echo "(*)(+) cfg       Build what was configured by 'config.sh'"
	@echo "   (+) jvm       Build JVM main development area"
	@echo "   (+) libjvm    Build JVM static library"
	@echo "   (+) main      Build binary from main() and static library"
	@echo "   (+) test      Build test classes for JVM development"
	@echo "   (+) jni       Build JNI classes and shared object library"
	@echo "                   (but currently the same binary as 'main')"
	@echo ""
	@echo "       undox     Clean documentation (only)"
	@echo "   (-) depend    Build dependencies (done by 'all' also)"
	@echo "   (-) nodepend  Clean dependencies (done by 'clean' also)"
	@echo "   (-) tags      Build vi(1) editor tag file"
	@echo "       roster    Update the file roster to represent the"
	@echo "                   current list of source and header files"
	@echo "       help      Display this message"
	@echo ""
	@echo " Top level:  (+) only (-) never  (*) Default rule"

###################################################################
#
# EOF
