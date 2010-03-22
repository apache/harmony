# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Makefile for module 'zlib'
#

# We don't need to debug into zlib
HY_CFG=release

include $(HY_HDK)/build/make/defines.mk

HYRELEASECFLAGS = -O2

# Further performance improvement on x86 and x86_64
ifneq (,$(findstring x86,$(HY_ARCH)))
HYRELEASECFLAGS += -DUNALIGNED_OK
endif

ZLIB_DIST=../../zlib_dist/# Path to zlib

BUILDFILES = \
	$(SHAREDSUB)zlib_copyright.o $(ZLIB_DIST)adler32.o $(ZLIB_DIST)compress.o \
	$(ZLIB_DIST)crc32.o $(ZLIB_DIST)deflate.o $(ZLIB_DIST)infback.o $(ZLIB_DIST)inffast.o \
	$(ZLIB_DIST)inflate.o $(ZLIB_DIST)inftrees.o $(ZLIB_DIST)trees.o $(ZLIB_DIST)uncompr.o $(ZLIB_DIST)zutil.o

DLLNAME = $(DLLPATH)libhyzlib$(HY_SHLIB_SUFFIX)
EXPNAME = HYZLIB_0.1

ifeq ($(DLL_LD),$(CC))
ifneq ($(HY_OS),zos)
ifneq ($(HY_OS),macosx)
LDFLAGS += -Wl,-Bsymbolic
endif
endif
endif

ifeq ($(HY_OS),zos)
OPT += -DSTDC
endif

include $(HY_HDK)/build/make/rules.mk
