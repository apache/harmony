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
# Makefile for module 'port'
#

HY_BIN=$(HY_TARGET)/build/classlib/portlib/native/port/
include $(HY_TARGET)/hdk/build/make/defines.mk

INCLUDES += -DHYPORT_LIBRARY_DEFINE -I$(HY_PLATFORM)

BUILDFILES = \
  port_copyright.o hyerror.o hyerrorhelpers.o hyexit.o hyfile.o hyfiletext.o \
  hygp.o hymem.o /hymmap.o hynls.o hynlshelpers.o hyosdump.o hyport.o \
  hyportcontrol.o hyportptb.o hysharedhelper.o hysl.o hysock.o hystr.o \
  hystrftime.o hystsl.o hysysinfo.o hytime.o hytlshelpers.o hytty.o hycpu.o \
  hyipcmutex.o hyshmem.o hyshsem.o hyvmem.o


ifeq ($(HY_NO_THR),true)
MDLLIBFILES += $(LIBPATH)libhythr$(HY_LINKLIB_SUFFIX)
endif
DLLNAME = $(DLLPATH)libhyprt$(HY_SHLIB_SUFFIX)
EXPNAME = HYPRT_0.1

include $(HY_TARGET)/hdk/build/make/rules.mk

ifeq ($(HY_PORTLIB_STUBS),true)
$(HY_BIN)%.o: $(SHAREDSUB)%.c
	$(CC) $(CFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<
endif
