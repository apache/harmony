# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#  
#      http://www.apache.org/licenses/LICENSE-2.0
#  
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

#
# Configuration Makefile
#

CXX = $(CC)
CPP = $(CC) -E
AS = as
AR = ar
RANLIB=echo
ARFLAGS = rcv
DLL_LD = $(CC)
DLL_LDFLAGS = -shared -Wl,-soname=$(@F) -Wl,--version-script,$(EXPFILE)
CXX_DLL_LD = $(CXX)
STDCLIBS = -lstdc++
OSLIBS = -lc -lm
XLIBS = -L/usr/X11R6/lib -lX11 -lXft -lXext -lXtst
MDLLIBPREFIX = -Xlinker --start-group
MDLLIBSUFFIX = -Xlinker --end-group
EXELDFLAGS = $(LDFLAGS)
EXERPATHPREFIX = -Xlinker -z -Xlinker origin -Xlinker -rpath \
	-Xlinker \$$ORIGIN/ -Xlinker -rpath-link \
	-Xlinker
WARNFLAGS=-Werror

PLATFORM = -fpic
HYDEBUGCFLAGS = -ggdb -O0
HYRELEASECFLAGS = -O1 -DNDEBUG

LIBPATH=$(HY_HDK)/lib/
DLLPATH=$(HY_HDK)/jdk/jre/bin/
EXEPATH=$(HY_HDK)/jdk/jre/bin/
DBGPATH=$(HY_HDK)/jdk/lib/
SHAREDSUB=../shared/
INCLUDES += -I$(HY_HDK)/include -I$(HY_HDK)/jdk/include -I. -I$(SHAREDSUB)

include $(HY_HDK)/build/make/platform/$(HY_PLATFORM).mk

DEFINES += -D_REENTRANT

ifeq ($(HY_CFG),release)
OPT += $(HYRELEASECFLAGS)
else
OPT += $(HYDEBUGCFLAGS)
endif

MDLLIBFILES = $(LIBPATH)libhycommon.a
ifeq ($(HY_OS),zos)
# On z/OS we need to link every module against the ascii2ebcdic library
MDLLIBFILES += $(LIBPATH)libhya2e.x
endif

ifeq ($(HY_NO_THR),false)
ifeq ($(HY_THR_NO_DEPLOY), true)
MDLLIBFILES += $(HY_HDK)/../modules/portlib/src/main/native/thread/libhythr$(HY_LINKLIB_SUFFIX)
else
MDLLIBFILES += $(DLLPATH)libhythr$(HY_LINKLIB_SUFFIX)
endif
else
DEFINES += -DHY_NO_THR
endif

ifeq ($(HY_ZIP_API),true)
DEFINES += -DHY_ZIP_API
endif

ifeq ($(HY_LOCAL_ZLIB),true)
DEFINES += -DHY_LOCAL_ZLIB
OSLIBS += -lz
MDLLIBZLIB =
else
MDLLIBZLIB += $(DLLPATH)libhyzlib$(HY_LINKLIB_SUFFIX)
endif
