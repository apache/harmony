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
# Makefile for module 'luni'
#

HY_BIN=$(HY_TARGET)/build/classlib/luni/native/luni/
include $(HY_TARGET)/hdk/build/make/defines.mk

# CFLAGS += -fpic -Wall -Werror 
ifneq ($(HY_OS),zos)
ifneq ($(HY_OS),aix)
CFLAGS += -Wall
endif
endif

BUILDFILES = \
  luni_copyright.o file.o procimpl.o nethelp.o floatbits.o helpers.o math.o \
  luniglob.o proxy.o cbigint.o dblparse.o fltparse.o netif.o process.o \
  bigint.o osc.o socket.o inetadds.o filedesc.o timezone.o OSFileSystem.o \
  OSFileSystemLinux32.o OSMemory.o OSMemoryLinux32.o OSNetworkSystem.o \
  OSNetworkSystemLinux.o hyenv.o consoleimpl.o

MDLLIBFILES += $(LIBPATH)libhyfdlibm.a $(LIBPATH)libvmi$(HY_LINKLIB_SUFFIX)

OSLIBS += -lm

DLLNAME = $(DLLPATH)libhyluni$(HY_SHLIB_SUFFIX)
EXPNAME = HYLUNI_0.1

include $(HY_TARGET)/hdk/build/make/rules.mk
