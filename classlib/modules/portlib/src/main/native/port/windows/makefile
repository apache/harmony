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

HY_BIN=$(HY_TARGET)\build\classlib\portlib\native\port\# trailing slash
!include <$(HY_TARGET)\hdk\build\make\defines.mak>

LIBBASE=hyprt
DLLNAME=$(DLLPATH)$(LIBBASE).dll
LIBNAME=$(LIBPATH)$(LIBBASE).lib

HYCFLAGS = $(HYCFLAGS) -DHYPORT_LIBRARY_DEFINE /I$(SHAREDSUB)
HYLDFLAGS = $(HYLDFLAGS) -def:$(LIBBASE).def

BUILDFILES = \
  $(HY_BIN)port_copyright.obj $(HY_BIN)hycpu.obj $(HY_BIN)hyerror.obj \
  $(HY_BIN)hyerrorhelpers.obj $(HY_BIN)hyexit.obj $(HY_BIN)hyfile.obj \
  $(HY_BIN)hyfiletext.obj $(HY_BIN)hygp.obj $(HY_BIN)hyipcmutex.obj \
  $(HY_BIN)hymem.obj $(HY_BIN)hymmap.obj $(HY_BIN)hynls.obj \
  $(HY_BIN)hynlshelpers.obj $(HY_BIN)hyosdump.obj $(HY_BIN)hyport.obj \
  $(HY_BIN)hyportcontrol.obj $(HY_BIN)hyportptb.obj $(HY_BIN)hyshmem.obj \
  $(HY_BIN)hyshsem.obj $(HY_BIN)hysl.obj $(HY_BIN)hysock.obj \
  $(HY_BIN)hystr.obj $(HY_BIN)hystrftime.obj $(HY_BIN)hystsl.obj \
  $(HY_BIN)hysysinfo.obj $(HY_BIN)hytime.obj $(HY_BIN)hytlshelpers.obj \
  $(HY_BIN)hytty.obj $(HY_BIN)hyvmem.obj

MDLLIBFILES = $(MDLLIBFILES) $(LIBPATH)hythr$(HY_LINKLIB_SUFFIX)
VIRTFILES = hyprt.res

SYSLIBFILES = \
	Delayimp.lib -delayload:shell32.dll -delayload:Iphlpapi.dll \
	ws2_32.lib Iphlpapi.lib shell32.lib

DLLBASE=0x11100000

!include <$(HY_HDK)\build\make\rules.mak>
