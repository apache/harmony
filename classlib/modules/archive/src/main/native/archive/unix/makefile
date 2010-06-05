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
# Makefile for module 'archive'
#

HY_BIN=$(HY_TARGET)/build/classlib/archive/native/archive/
include $(HY_TARGET)/hdk/build/make/defines.mk
ZLIB_DIST = $(HY_TARGET)/depends/native/zlib_dist/
INCLUDES += -I$(SHARED)common -I$(ZLIB_DIST) -I../../include/shared

BUILDFILES = archive_copyright.o jclcrc32.o zip.o adler32.o inflater.o \
  jarfile.o deflater.o archiveglob.o

ifneq ($(HY_ZIP_API),true)
MDLLIBFILES +=	$(LIBPATH)libhyzip.a
endif

ifeq ($(HY_LOCAL_ZLIB),true)
DEFINES += -DHY_LOCAL_ZLIB
OSLIBS += -lz
else
MDLLIBZLIB += $(DLLPATH)libhyzlib$(HY_LINKLIB_SUFFIX)
endif

MDLLIBFILES += \
	$(MDLLIBZLIB) $(LIBPATH)libvmi$(HY_LINKLIB_SUFFIX)

DLLNAME=$(DLLPATH)libhyarchive$(HY_SHLIB_SUFFIX)
EXPNAME=HYARCHIVE_0.1

include $(HY_TARGET)/hdk/build/make/rules.mk
