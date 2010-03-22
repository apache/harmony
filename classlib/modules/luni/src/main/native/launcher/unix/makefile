#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

#
# Makefile for 'launcher'
#

include $(HY_HDK)/build/make/defines.mk

BUILDFILES = $(SHAREDSUB)main.o $(SHAREDSUB)cmain.o \
	$(SHAREDSUB)launcher_copyright.o $(SHAREDSUB)strbuf.o \
	$(SHAREDSUB)libhlp.o
ifeq ($(HY_NO_THR),false)
MDLLIBFILES += $(DLLPATH)libhyprt$(HY_LINKLIB_SUFFIX)
else
BUILDFILES += main_hlp.o
endif
EXENAME = $(EXEPATH)java

ifeq ($(HY_OS),zos)
# On z/OS statically link the a2e lib so it can be used before LIBPATH is set
MDLLIBFILES = $(LIBPATH)libhya2e.a $(LIBPATH)libhycommon.a
endif

include $(HY_HDK)/build/make/rules.mk
