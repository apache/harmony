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

!include <$(HY_HDK)\build\make\defines.mak>

LIBBASE=hyprt
DLLNAME=$(DLLPATH)$(LIBBASE).dll
LIBNAME=$(LIBPATH)$(LIBBASE).lib

HYCFLAGS = $(HYCFLAGS) -DHYPORT_LIBRARY_DEFINE /I$(SHAREDSUB)
HYLDFLAGS = $(HYLDFLAGS) -def:$(LIBBASE).def

BUILDFILES = \
  $(SHAREDSUB)port_copyright.obj hycpu.obj $(SHAREDSUB)hyerror.obj \
  hyerrorhelpers.obj $(SHAREDSUB)hyexit.obj hyfile.obj hyfiletext.obj \
  $(SHAREDSUB)hygp.obj hyipcmutex.obj hymem.obj hymmap.obj \
  $(SHAREDSUB)hynls.obj hynlshelpers.obj hyosdump.obj $(SHAREDSUB)hyport.obj \
  $(SHAREDSUB)hyportcontrol.obj hyportptb.obj hyshmem.obj hyshsem.obj \
  hysl.obj hysock.obj $(SHAREDSUB)hystr.obj \
  $(SHAREDSUB)hystrftime.obj $(SHAREDSUB)hystsl.obj hysysinfo.obj hytime.obj \
  $(SHAREDSUB)hytlshelpers.obj hytty.obj hyvmem.obj

MDLLIBFILES = $(MDLLIBFILES) $(LIBPATH)hythr$(HY_LINKLIB_SUFFIX)
VIRTFILES = hyprt.res

SYSLIBFILES = \
	Delayimp.lib -delayload:shell32.dll -delayload:Iphlpapi.dll \
	ws2_32.lib Iphlpapi.lib shell32.lib

DLLBASE=0x11100000

!include <$(HY_HDK)\build\make\rules.mak>
