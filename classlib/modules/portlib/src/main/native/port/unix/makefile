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

include $(HY_HDK)/build/make/defines.mk

INCLUDES += -DHYPORT_LIBRARY_DEFINE -I$(HY_PLATFORM)

BUILDFILES = \
	$(SHAREDSUB)port_copyright.o $(SHAREDSUB)hyerror.o \
	hyerrorhelpers.o $(SHAREDSUB)hyexit.o hyfile.o hyfiletext.o \
	$(SHAREDSUB)hygp.o hymem.o \
	$(HY_OS)/hymmap.o $(SHAREDSUB)hynls.o hynlshelpers.o hyosdump.o \
	$(SHAREDSUB)hyport.o $(SHAREDSUB)hyportcontrol.o hyportptb.o \
	hysharedhelper.o $(HY_OS)/hysl.o hysock.o \
	$(SHAREDSUB)hystr.o $(SHAREDSUB)hystrftime.o $(SHAREDSUB)hystsl.o \
	hysysinfo.o hytime.o $(SHAREDSUB)hytlshelpers.o hytty.o

ifeq ($(HY_PORTLIB_STUBS),true)
BUILDFILES += stubs/hycpu.o stubs/hyipcmutex.o stubs/hyshmem.o \
              stubs/hyshsem.o stubs/hyvmem.o
else
BUILDFILES += hycpu.o hyipcmutex.o hyshmem.o hyshsem.o $(HY_OS)/hyvmem.o
endif

ifeq ($(HY_NO_THR),true)
MDLLIBFILES += $(LIBPATH)libhythr$(HY_LINKLIB_SUFFIX)
endif
DLLNAME = $(DLLPATH)libhyprt$(HY_SHLIB_SUFFIX)
EXPNAME = HYPRT_0.1

include $(HY_HDK)/build/make/rules.mk
